# AGENTS.md

Guidance for AI agents working in this repository.

## Project purpose

A sample Android app (`uno.lux.sample`, displayed as "Sample") built as a resume/portfolio piece demonstrating modern Android architecture. Code quality, idiomatic Compose and architectural clarity are the point, not just working features — prefer the current recommended Android way over expedient shortcuts.

## Testing philosophy — tests first

Tests are the **primary consumer** of this codebase; users come second.

- **Write the test first.** A change isn't done until tests cover it and they're green; new logic lands with its test in the same change.
- **Design for the test.** Logic lives in plain-JVM, constructor-injected units with no Android dependencies; pure functions take their inputs (e.g. `now`) as parameters instead of reading ambient state. Testability is *why* the architecture is shaped this way.
- **Same rule for UI.** Stateless composables take data + callbacks; ViewModels expose a `StateFlow` a test asserts against.

Shared test infrastructure lives in `app/src/test/java/uno/lux/sample/testing/`: `MainDispatcherRule` (swaps `Dispatchers.Main`), `ViewModelTest` (base class for ViewModel tests), `BackStacks` (`backStackOf`/`screens`, for driving the `Navigator`), `TestUtils`. Fakes are hand-written and live beside what they stand in for (`post/data/FakePostDataSource.kt`) — there is no mocking framework.

## Commands

The shell is Windows PowerShell; invoke the wrapper as `.\gradlew.bat`.

- Unit tests: `.\gradlew.bat testDebugUnitTest` (one class: `--tests "uno.lux.sample.post.data.PostRepositoryTest"`, append `.<method>` for one test)
- Lint: `.\gradlew.bat lintDebug` → `app/build/reports/lint-results-debug.html`; formatting: `.\gradlew.bat ktlintCheck`
- Build / install: `.\gradlew.bat assembleDebug` / `installDebug`
- Instrumented (needs a device): `.\gradlew.bat connectedDebugAndroidTest` (one class: `-Pandroid.testInstrumentationRunnerArguments.class=…`)

**CI** (`.github/workflows/ci.yml`) runs `ktlintCheck`, `lintDebug` and `testDebugUnitTest` on every push to main and every PR, uploading the reports as artifacts — so the three JVM-only checks are exactly what gates a change. **There is no emulator job**: the instrumented suite is run by nobody automatically, so a change to process-death or back-stack behaviour needs the user to run it.

**Do not touch a real device or emulator unless explicitly asked.** That covers `connectedDebugAndroidTest`, `installDebug`, `adb`, and driving the app by hand. Run the three JVM-only checks instead, and say plainly which parts of a change they cover and which would need a device. Write the instrumented test when the behaviour warrants one; leave *running* it to the user.

## Toolchain / build setup

- **Kotlin 2.4.10**, **AGP 9.3.1**, **Gradle 9.6.1**, Compose BOM **2026.06.01**. Java 11 source/target; Gradle daemon on JDK 21.
- `compileSdk` and `targetSdk` are both **37** (`release(37)` DSL), `minSdk = 26`. They are **two knobs bumped separately on purpose**: a new SDK lands on `compileSdk` first, and moves to `targetSdk` once its behaviour changes are reviewed. That review is cheap here — the app declares only `INTERNET`, picks media via `PickVisualMedia`, runs no foreground service, is already edge-to-edge, and ships no native code. Re-check those five when the next SDK lands.
- AGP 9's **built-in Kotlin** is in use (no `org.jetbrains.kotlin.android` plugin); annotation processing is **KSP** (kapt is incompatible), and Hilt rides on it.
- AGP bundles Kotlin 2.2.10; the root `build.gradle.kts` lifts the compiler to 2.4.10 via the **buildscript classpath** (the documented override). The Compose compiler, kotlinx-serialization and **Mappie** are version-locked to Kotlin — **a Kotlin bump is blocked until Mappie publishes a matching build**. KSP versions independently (`2.3.10`).
- Dependencies go in the **version catalog** (`gradle/libs.versions.toml`) and are referenced via `libs.*` — never hardcode a version in the build script.

## Backend

A **Rails** backend at `https://mosaic.tree-among-shrubs.com/api/` (`NetworkModule` holds the host as `BASE_URL` and builds `API_URL` from it); source in the `mosaic-server` repo under WSL at `\\wsl.localhost\ubuntu\home\benji\projects\mosaic-server`.

**There is no sign-in.** The signed-in user is seeded from `app/fixtures/SampleData.kt` and sent as an `X-User-Id` header; the server scopes viewer state (`isLiked`, `isBookmarked`) by it and enforces ownership with it — deleting someone else's post and reading someone else's bookmarks both 403 server-side, not merely in the client.

**Client limits mirror server validations** (`CreatePostMaxImages`, `CreatePostMaxVideoBytes`, composer field lengths, `REPORT_DETAILS_MAX_LENGTH`, the `ReportReason` list) — changing one means changing both.

Reporting a post is fire-and-forget: `POST /api/posts/:id/report` validates and then logs-and-discards, answering 204. There is no moderation queue, and the "thank you" is optimistic — so a report the server would refuse must not be typeable in the first place. The reason's *wire* spelling lives in `post/data/network/ReportPostRequestDto.kt`, keeping the domain enum free of it; blank details are omitted from the body rather than sent empty.

## Package structure — where a new file goes

The top level is **slices, not layers**; layers are the shape *inside* a slice. There is no root `data/` or `ui/`, and deliberately no `activities/`, `viewmodels/`, `dialogs/`: **a file's home is decided by what it is *about*, never by what it extends.**

```
post/ user/ comment/ album/ video/           aggregates: the entity, its wire types, its store, its UI
feed/ profile/ composer/ settings/ shell/    features and read models — they own no entity

common/              what two or more concerns share — ui/ noun-free composables, data/ wire types and files
app/                 the machine — MainActivity, MosaicApplication, MosaicApp
app/di/              Hilt modules
app/navigation/      Navigator, Screen keys, BackStackEntry, page transitions
app/theme/           the Mosaic palette, type and gradients
app/ui/components/   the Mosaic design system — branded controls, no domain noun
app/util/            pure functions with zero project imports, plus composables that draw nothing
app/fixtures/        stand-in content for previews and DI seeding
```

Inside a concern there are four layers and no fifth:

```
<concern>/data/            the repository and its DataSource interface
<concern>/data/domain/     the models — pure Kotlin, no platform and no wire
<concern>/data/network/    the service, DTOs, mappers — the only place HTTP appears
<concern>/ui/              composables and ViewModels
```

**`app/` vs `common/`** — both hold noun-free composables, and the line is *whose vocabulary it is*. `app/ui/components/` is the branded design system (`HoldToConfirmButton`); `common/ui/` is noun-free UI that **two or more** concerns actually need (`FullScreenError`, `DiscardChangesDialog`). Noun-freeness alone does not earn a place in `common/` — a generic composable with one consumer belongs to that slice and moves the day a second needs it.

**Aggregates vs read models** keeps the graph directed. `post`/`user`/`comment`/`album`/`video` own an entity; `feed`/`profile` own none — they hold ordered *IDs* plus paging state and resolve them through the entity stores, which is why `ProfileRepository` *composes* `PostRepository`.

> Features may depend on aggregates. **Aggregates never depend on features.**

```
user  album  video  settings  -> (nothing)
post      -> album comment user video      comment  -> post user
feed      -> post settings user video      profile  -> post user video
composer  -> feed post                     shell    -> feed profile user
```

Every slice may depend on `app`; `app` wires everything, except `app/theme`, `app/util` and `app/ui/components`, which import no slice. The feature-to-feature edges are deliberate: `composer -> feed` (publishing prepends the new ID), `feed -> settings` (auto-play), `shell -> feed profile` (its tabs *are* those screens). `post <-> comment` is a real, tolerated cycle; folding `comment/` into `post/` is the fix if it ever bites.

**To file a new file, ask in order:**

1. **Does it mention a domain noun?** No → 2. Yes → 3.
2. Pixels → branded control → `app/ui/components`; colour/font → `app/theme`; needed by 2+ concerns → `common/ui`; one concern → that concern. Draws nothing → `app/util`, even when `@Composable`. Wires the app together → `app`. Neither → `common/data`.
3. Claimed by exactly one feature → that feature. By several → the noun's aggregate.

The moment a "helper" imports `Post`, it is post code — file it home.

**Two things that look like violations but are not:** cross-slice KDoc references are written fully qualified (`[uno.lux.sample.profile.data.ProfileRepository]`) since a `[Link]` needs an import; and a wire type shared by two features belongs to the aggregate it describes (`SideloadedUsers` → `user/data/network/`), or to `common/data/network/` when it belongs to no single aggregate (`LikeToggleDto`).

**The `DataSource` interface stays beside its consumer, not its implementation** — that is what lets a repository compose a network and a local source without either owning the contract, and why `data/network/` nests inside the slice.

**One Retrofit service per slice** (`FeedApi`, `PostApi`, `CommentApi`, `UserApi`, `ProfileApi`), all from the single `Retrofit` in `app/di/NetworkModule.kt`. Do not reintroduce an app-wide API interface — the previous one fit in no slice and its 338-line fake had to be implemented in full by every test needing one endpoint.

### ArchitectureTest

`app/src/test/java/uno/lux/sample/architecture/ArchitectureTest.kt` (Konsist) asserts these against the real source tree and fails the build on a violation. **Every rule is derived from the path, never from a list of names** — concerns are discovered as the top-level packages minus `app` and `common`, so adding a slice requires no edit.

1. **Every concern package follows the convention** — one of the four layers. The rest select files by those suffixes, so this rule is the guard that keeps them from silently matching nothing.
2. **The wire stays in `data/network`** — Retrofit/OkHttp nowhere else, `app/di` excepted for the one `Retrofit`.
3. **The domain layer is pure** — `data/domain` imports no platform, no wire, no screen; mappers go DTO → model only.
4. **`data` never depends on `ui`.**
5. **`common` knows no concern.**
6. **`app/theme`, `app/util` and `app/ui/components` know no concern** — the rest of `app` is exempt, since wiring is its job.
7. **A repository behind no interface stays plain-JVM** — a `*Repository` with no supertype carries no Android import. `DataStoreSettingsRepository` and `AppCompatLocaleRepository` may touch the platform *because* consumers can be handed a double.

The cross-concern graph direction is deliberately **not** checked — encoding which cycles are tolerated costs more than the code review that catches them. Adding a rule means adding a test; verify it by planting a violation and watching it fail. If a violation is *intended*, widen the rule and say why in its message rather than deleting it.

## One module, on purpose

**The app is a single `:app` module and stays one until a stated trigger fires.** Do not begin a split incidentally. The slices map onto module boundaries almost mechanically, so the split stays available.

What it would buy at ~150 files, and why it isn't worth it yet: `internal` would start meaning "private to the slice" (the real prize, and the least pressing); the cross-slice graph would be compiler-enforced (the strongest argument); build avoidance would likely be *negative*, since builds here are dominated by KSP/Hilt codegen and configuration, not by recompiling unrelated Kotlin. It would also cost the exhaustive `when` over the sealed `Screen`: feature modules can't see each other's screens, which pushes navigation toward runtime route registration.

**Revisit when:** boundaries are being violated in practice and review isn't catching it; build times become a real complaint measured with `--profile`; or a second contributor joins.

## Architecture

Single-activity, 100% Compose — no Fragments, no XML layouts (XML under `res/` is resources only).

### Entry point

`MainActivity` (`app/ui/MainActivity.kt`) is the only Activity and stays a thin shell: it collects `ThemeMode` from `MainViewModel`, resolves dark/light, applies it to `MosaicTheme` and hosts `MosaicApp()`. It extends `AppCompatActivity` **only** because AppCompat's delegate applies the per-app language below Android 13 (see *Localization*) — no AppCompat UI is used.

### Navigation — Navigation 3, driven by ViewModels

`MosaicApp()` owns a `rememberBackStack` of `@Serializable` `BackStackEntry` keys, each pairing a `Screen` with the identity its state is scoped to. `Screen.Shell` is the permanent root; `Screen.Profile(userId)` and `Screen.Settings` push over it. `NavDisplay` renders the top entry with the push/pop specs in `app/navigation/PageTransitions.kt`, predictive back included; back handling is `onBack` popping the stack — **no hand-rolled `BackHandler`s**. Entry decorators give each entry its own saveable state and `ViewModelStore`, so a pushed page's ViewModel is created on push and cleared on pop.

**Identity lives on the entry, not on the `Screen`.** Nav3 scopes everything per entry by `contentKey`, which is a function of the back-stack key *alone*. With a bare `Screen` as key, two pushes of `Screen.PostDetail("p1")` collapsed onto one scope — the second page inherited the first's ViewModel and half-typed comment. `BackStackEntry(screen, id)` fixes it; `Navigator.entryFor` is the only place an id is minted, so a new `Screen` cannot forget to take part. `Navigator` takes its id source as a constructor parameter (`nextId`, defaulting to a UUID) and **must not use a counter** — the `@ActivityRetainedScoped` instance restarts on process death while the restored stack still holds old ids.

**A screen opts back into sharing through `Screen.sharedId`** — `null` by default (fresh identity per push); a constant makes it one page wherever opened. Deriving it from arguments (`"post-$postId"`) shares per argument — reach for that only when a page genuinely *is* one thing per argument. `Screen.Shell` is the only screen pinning one today. Declare it as `get() = …`, never an initialized property: a backing field would be pulled into the serialized key.

**Navigation intent flows through ViewModels**, not host lambdas: every screen has a ViewModel injecting the `Navigator` (`@ActivityRetainedScoped` in `app/di/NavigationModule.kt`) and calling `goTo`/`goBack`. `MosaicApp` therefore wires **no navigation lambdas at all** — it hands `NavDisplay` the shared `backStackEntryProvider` (where `contentKey = { it.id }` is stated once) and a private `ScreenContent` picks the page with one exhaustive `when` over the sealed `Screen`, so an unhandled page is a compile error. The `entryProvider { entry<T>() }` DSL is deliberately unused: it dispatches a `KClass`-keyed map (pointless with one key type) and its never-pruned metadata cache would retain every entry ever pushed.

Two push variants: `goTo` allows pushing a screen equal to the current top (an intentional re-open — the 500 ms click debounce on nav controls covers accidental double-taps), while `goToSingleTop` is a no-op when its screen is already on top, for pages that must never stack on themselves (Settings, the profile editor).

Unit tests drive the real `Navigator` via `testing/BackStacks.kt`'s `backStackOf(…)` and assert on `.screens()`, so assertions stay about pages rather than identities.

### The shell

`shell/` is a feature, not part of the machine — it owns no entity and hosts other features' screens. `ShellScreen` uses `DividedNavigationSuiteScaffold` (Material 3's `NavigationSuiteScaffold`) so navigation adapts to window size with no per-form-factor code. Destinations are data-driven from the `ShellDestinations` enum in `shell/ui/` (a `@StringRes` label + icon + optional `screen`) — it lives in `ui/` because a label resource and a drawable are presentation; add or change a tab by editing the enum.

- **Tab selection is plain `rememberSaveable`, not back-stack entries** — switching tabs is not a navigation event, so system back never walks through tabs.
- **CREATE is deliberately not a tab.** A destination carrying a `screen` is an *action*: selecting it pushes that page over the whole shell (`goToSingleTop`) instead of swapping the content area, so the tab underneath stays highlighted. Hence the unreachable `CREATE -> Unit` branch in the content `when`.
- Each screen owns its own `Scaffold`/`TopAppBar`. Tab switches cross-fade; page pushes slide.

### Data layer

`PostRepository` wraps a `PostDataSource` and holds a `StateFlow` entity cache, so like/bookmark/delete mutations reflect across every screen without a re-fetch — `FeedRepository`/`ProfileRepository` hold only ordered IDs and resolve them through that store, so a deleted post vanishes everywhere in one emission (the ordered lists are deliberately left untouched).

- **Deletion** is offered only on your own posts; the client gates on author ID and the server enforces (`DELETE /api/posts/:id`, 403 otherwise). It cascades to comments/likes/bookmarks and to an `Album`/`Video` once no post holds it.
- **Comment counts** ride the same propagation even though `CommentRepository` is stateless: `commentCount` is a field on the *post*, so `PostRepository.commentAdded` bumps the entity — but only **after** the server answers. Server-side the count is derived (no column; one grouped query per page), so the next read replaces the bump rather than compounding it.
- **A like is not restricted to other people's posts.** No owner gating on the heart, on either surface. `isOwn` gates *deletion* only.
- **Like and bookmark are optimistic**, unlike `commentAdded` above: the heart is the most-tapped control in the app, so it fills on the tap and reconciles when the answer lands, and a failure puts the fields back and rethrows. Having exactly one copy of each post to correct is what makes that safe.
  - **Every write re-reads the entity and touches only the fields it owns** (`PostRepository.updateEntity`). Reading the post once and writing that snapshot back afterwards was a real bug: a refresh landing mid-flight had its fresher post overwritten by a stale one. A repository mutation that writes a whole entity read before a suspension is the shape to avoid.
  - **An answer a newer tap has moved past is dropped** — `updateEntity`'s `stillOurs` predicate — so two taps in flight settle on the second, which is what the user last asked for.
  - **The endpoints are idempotent**: `PUT /posts/:id/like` and `PUT /posts/:id/bookmark` take `{"liked": true}` / `{"bookmarked": true}` rather than flipping what they find, so a retry after a timeout cannot move the like twice. They were bodiless `POST …/toggle`s, which could. Comment likes share the contract (`PUT …/comments/:id/like`), and `PostApiLikeTest` pins verb, path and body against MockWebServer. Following is still a toggle.
  - **A like answers with `LikeState`, not with the thing that was liked** (`common/data/`, mirroring `LikeStateDto`). Posts and comments both use it. Answering with a whole entity is what let a stale copy be written back; two fields cannot.
- **A thread is paged like every other list**, and its window belongs to `PostDetailViewModel` rather than to the stateless `CommentRepository` — a cursor kept in the repository would outlive the list it points into. The ViewModel holds the comments, the cursor, the end flag and the load error as **one** `CommentThread` value, so a page landing moves them together and the `uiState` combine keeps a typed arity. **The cursor is the whole guard**: it is null before the first page lands and again once the server says that was the last, so `loadMoreComments` needs no flag of its own to know there is nothing to ask for. A page whose cursor no longer matches the thread's is *dropped* rather than appended — a reload landing mid-flight has started the thread over, and gluing the two together would show comments from two different reads (and duplicate keys in the `LazyColumn`). The header counts `post.commentCount`, never the loaded window.
- **Comment likes are optimistic too, and the code is deliberately not shared** with `PostRepository`'s. A comment's like lives in the list `PostDetailViewModel` owns rather than in an entity store, so `onToggleCommentLike`/`updateComment` repeat the *shape* — optimistic write, `stillOurs` reconcile, revert-and-rethrow — over a `List<Comment>` instead of a `Map<PostId, Post>`. Abstracting over "the container" costs more than the twenty lines it saves; keep them in step by reading both.
- `SettingsRepository` has `InMemorySettingsRepository` (test double) and `DataStoreSettingsRepository` (Preferences DataStore, injected as a constructor dependency so unit tests drive a real DataStore over a temp file). The persisted key `theme_mode` is the contract with the legacy SharedPreferences file — tests pin it.
- **An absent preference means "never chosen"**, and what that resolves to is named once in `DefaultAutoPlayVideos`: auto-play is **off** when unset, since a feed that plays by itself spends someone's data before they asked. Repository and `HomeViewModel` start from that same constant so a launch never flips playback mid-load. Tests seed the value *opposite* the default so a read that falls through fails rather than accidentally agreeing.
- Domain models, interfaces and every repository implementation carry no Android dependencies.

### Feature UI — the HOME feed sets the pattern

MVVM with unidirectional data flow. `HomeViewModel` exposes `StateFlow<HomeUiState>` (`Loading`/`Error`/`Feed`) and converts intent into repository mutations or `Navigator` pushes, both on the one `HomeActions` seam. Every screen splits into a **stateful binder** (collects state, injects the ViewModel) and an **internal stateless composable** (pure inputs + callbacks) so it previews and tests without a ViewModel.

**A loaded feed outranks a load error.** With nothing loaded a failure becomes `HomeUiState.Error` and owns the screen; over a loaded feed it rides along as `Feed.refreshError` and surfaces in a snackbar — a pull-to-refresh in a tunnel must not take away the posts being read. The transient error is **spent once shown** (`onRefreshErrorShown()` clears it), so a configuration change cannot re-announce it. `retry()` is the one path back to the full-screen error. Follow this on any screen that can fail while showing content.

**`profile/ui/`** — same split, parameterized by user through Hilt assisted injection, plus the `@CurrentUserId` signed-in user. A sticky tab row generated from the `ProfileTab` enum: Posts, Likes, Saved.

- **Saved is private, Likes is public.** `ProfileTab.ownerOnly` filters the row — which is why the selected index is `tabs.indexOf(selected)`, **not** the ordinal. Client gating is a courtesy; the server enforces (`/bookmarks` 403s unless `:id` is the caller, refusing before the lookup so an unknown id can't distinguish itself).
- **Both lists load lazily on first open** (`onSavedTabShown`/`onLikesTabShown`), not on `refresh` — a tab nobody opened costs no request. `bookmarkIds`/`likeIds` emit `null` until that first load, which is how a tab tells "empty" from "not asked yet"; refresh re-fetches only what was opened.
- **For the signed-in user each list is derived from its flag, not echoed from the fetch**: `OnDemandPostIds` takes `Post::isBookmarked`/`Post::isLiked` and `combine`s it with the entity store, so membership moves both directions from anywhere with no re-fetch. Deriving only *removal* is the trap — it made membership depend on whether the post happened to be in an already-fetched page. Order comes from the server's keyset, `(createdAt, id)` descending, which the client reproduces; `PageState.oldestLoaded` holds back a post below the loaded window rather than letting it jump ahead.
- None of that applies to **another** user's Likes tab, which is echoed exactly as fetched: `isLiked`/`isBookmarked` are viewer-scoped, so on someone else's profile they describe you. Saved/liked posts can be by anyone, so authors ride along in `included.users` into `UserRepository`.

**There is exactly one user projection, and that is load-bearing.** A sideloaded author is serialized identically to a fetched profile. A `minimal` projection was indistinguishable on the wire from a user who left those fields empty, and since `UserRepository.ingest` replaces entries wholesale it silently blanked a fully-loaded profile's bio and counts. The invariant on `User`: **a `null` optional field means the user left it empty, never "not loaded yet."** If a payload ever needs a trimmed user, give it its own type.

**`composer/ui/`** — `CreatePostViewModel` holds one plain `StateFlow<CreatePostUiState>` (form + in-flight flag + a single `CreatePostError`), not a sealed hierarchy, because a composer has nothing to load. One error field, not one per source: the screen has a single snackbar. Publishing goes through `FeedRepository.publish` (stores the entity, ingests the author, prepends the ID to the feed), then `Navigator.replaceTop`s with `Screen.PostDetail` so backing out lands on the shell, not a spent composer. A failed publish keeps the typed text; leaving with a part-written post raises the shared `DiscardChangesDialog`.

- **Media is exclusive — photos or one video, never both.** `CreatePostMedia` (and `NewPostMedia`) is a closed `None`/`Images`/`Video` hierarchy, so the illegal combination is *unrepresentable*; the server mirrors it with a `media_conflict` 422. The composer hides the other add-tile once one kind is chosen, making exclusivity self-evident without a dialog.
- Limits mirror the server: `CreatePostMaxImages` (10) and `CreatePostMaxVideoBytes` (25 MB), the latter checked via `FileLoader.sizeOf` **before** the file is read.
- Everything uploads as one multipart `POST /posts` — `images[]` (the brackets make Rack build an array) or `video`. Parts are built through `common/data/network/MultipartParts.kt` so it's one edit rather than one per data source.
- The form holds picked media as **content-URI strings, not bytes**; `FileLoader` reads them into `FileUpload`s once, at publish — which is why `CreatePostForm.toNewPost` takes the loaded media as a parameter. Re-picking is additive and de-duplicated.
- **Video duration is the server's to derive.** The upload carries the file alone. The composer's own thumbnail is the exception (nothing is uploaded yet), so `VideoMetadataReader` reads it locally for that badge only, degrading to 0 rather than blocking a post — hence `CreatePostMedia.Video` carries a duration and `NewPostMedia.Video` does not.
- `PostApiMultipartTest` drives the **real Retrofit stack over MockWebServer** to pin the wire format — part names and omitted optional parts are exactly where client and backend must agree.

### Dependency injection — Hilt

`MosaicApplication` is `@HiltAndroidApp` (not `MosaicApp`, the root *composable*); `MainActivity` is `@AndroidEntryPoint`. All bindings live in `app/di/{Data,Network,Navigation}Module.kt`. Repositories are `@Singleton` so the entity cache is shared across screens, and carry **no DI annotations themselves** (constructed in the module), keeping the data layer framework-free. ViewModels are `@HiltViewModel`, bound via `hiltViewModel()` from `androidx.hilt:hilt-lifecycle-viewmodel-compose` (the `hilt-navigation-compose` copy is deprecated); runtime args go through **assisted injection**. Tests never touch Hilt — they construct ViewModels directly with fakes, which is why ViewModels keep plain constructor parameters.

### Theming

`app/theme/` implements the **Mosaic** design system: a fixed brand palette (indigo accent, warm-neutral surfaces, coral like-state) with full light/dark tokens in `Color.kt`. **No dynamic color** — the brand stays consistent — and `surfaceTint` is transparent so cards keep their exact colour while still casting a shadow. `Type.kt` wires two bundled variable fonts (`res/font/`, OFL in `licenses/`): Bricolage Grotesque for the wordmark and post titles, Manrope for UI and body. Tokens with no Material role (`textTertiary`, `like`) ride on `MosaicColors` via `LocalMosaicColors`. **Wrap any new top-level Compose content and every `@Preview` in `MosaicTheme`.** Edge-to-edge bar styling is re-applied in a `DisposableEffect` keyed on the resolved theme.

## Surviving process death

**Rotation is the same problem, one notch weaker** — a configuration change wipes anything in a plain `remember`, and only ViewModels and repositories survive. The rule: **any state a composable owns outright is `rememberSaveable`, not `remember`** — including whether a sheet or dialog is open, so a rotation mid-report doesn't throw the report away. `remember` stays right for state that is meaningless after recreation (an `Animatable`, a transient gesture offset).

**The back stack comes back; nothing else does.** `rememberBackStack` saves the `@Serializable` entries and `rememberSaveableStateHolderNavEntryDecorator` restores each entry's saveable state under that same identity (which is why the id must be a stored property, not a regenerated default). Every repository, ViewModel and store is rebuilt from scratch — so **a screen must be able to rebuild everything it shows from its `Screen` key alone**, and the key is where anything unre-derivable goes (`AlbumViewer` carries its image URLs, `FullscreenVideo` its url and title).

For a page that fetches, that means a **cold-start load**: check whether the stores already hold what the key names, and fetch when they don't (`EditProfileViewModel.load`, `PostDetailViewModel.loadPost` via `PostRepository.load`). A page opened the ordinary way skips the request.

**An absent entity is three states, not one**, on every screen that resolves one by id — reading "the store has no such thing" as *not found* is only correct while the store is authoritative, which stops being true the moment the app can start on that page. `PostDetailViewModel.PostLoad` separates `Loading`, `NotFound` (a 404 — nothing to retry) and `Error` (retryable); `PostDataSource.fetch` returns `null` for the 404 via `notFoundAsNull` rather than throwing. `ProfileViewModel` gates `NotFound` behind `_hasLoaded` for the same reason.

**Deletion says so explicitly**, since absence is ambiguous: `PostRepository.delete` records the ID in `deletedIds` before dropping the entity, so a detail page whose post was deleted from another screen reads `NotFound` instead of spinning on a fetch nobody will make. `load` short-circuits on the same set, keeping "deleted" true offline. Feed and profile need nothing — they resolve IDs through `entities` and drop what vanishes.

Both endpoints answering with a *single* post embed the author, so `PostRepository` seeds `UserRepository` from `load` and `create` — a sideloaded user belongs in the user store wherever it arrives from.

**What the user typed is the one thing that must be *saved*, not re-derived.** `CreatePostViewModel` and `EditProfileViewModel` persist their form through the `SavedStateHandle`, scoped per back-stack entry. `app/util/SavedDraft.kt` is the seam: `saveDraft` registers *where to read the draft from* (a `() -> T`, not a `Flow<T>`) so the platform pulls it at most once per save and typing costs nothing. Serializing the form whole is what keeps `CreatePostMedia`'s variant — and with it the photos-or-video exclusivity — intact.

Only the *form* is saved; an in-flight publish and a dead process's errors are not, so a restored composer is idle. The editor re-reads its pristine snapshot from the server, so restored edits still read as unsaved.

**These tests are instrumented, deliberately** — a `SavedState` is a `Bundle`, so a JVM test could only exercise a stand-in. `BackStackRestorationTest` walks one of every `Screen` through `emulateSavedInstanceStateRestore()` (catching a key that quietly stops being serializable) and pins entry identity across it; `DraftRestorationTest` does the save → fresh handle → rebuild round trip. Cold-start loading is covered by ViewModel unit tests, since neither can emulate empty stores.

Video playback position is deliberately not restored.

## Compose performance

Strong skipping is on (built-in Compose compiler, Kotlin 2.4.10), which shapes what's worth doing:

- **Don't reflexively wrap lambdas in `remember` or chase `@Stable`/`@Immutable`.** Strong skipping auto-remembers lambdas and lets composables with unstable parameters skip via instance equality — feed rows skip fine despite `Post` carrying an `Instant` and a `List`.
- **Skipping is by instance, so a mutation must preserve identity for everything unchanged.** `PostRepository` stores a `Map<PostId, Post>` and mutates with `_entities.update { it + (postId to updated) }`, so a like recomposes exactly one `PostCard`. Mapping `copy()` over the whole collection would recompose every visible row — avoid it.
- **Read snapshot state in the smallest composable that uses it.** Scroll/pager state read at a screen's top level invalidates the whole scope; the album viewer keeps its page counter in a separate `PageIndicator` so swiping recomposes only the pill.
- **Coalesce continuous signals with `derivedStateOf`** (scroll offset → a boolean), so readers recompose only when the result flips. Not needed for a signal that's already coalesced — `HomeScreen` reads `listState.canScrollBackward` directly.
- **Don't key a long-lived effect on state you only read inside it.** The feed's autoplay collector reads posts through `rememberUpdatedState` and keys only on the stable `listState`/playback. The auto-play *setting* is a key on purpose: the restart re-reads the current layout, so switching it on plays the video already on screen.
- **Animate in the draw/layer phase where you can** — the like "pop" reads its `Animatable` inside `graphicsLayer`, so each frame re-runs the layer, not composition. Composition-scope animation is still right for short transitions of small subtrees.

## Code style

**ktlint does not enforce these, deliberately — this section is the only statement of them.** `.editorconfig` disables `function-signature` and `multiline-expression-wrapping` precisely because their wrapping modes fight the brace and parameter rules below; the rest have no ktlint rule at all. Do not delete this section on the assumption the formatter covers it.

**Separate logical sections within a function with a blank line** — declarations grouped, then a blank line before the work that uses them. Applies to plain functions and composables alike.

```kotlin
@Composable
private fun OverflowMenu(...) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    IconButton(...) { ... }

    if (showSheet) {
        PostOverflowSheet(...)
    }
}
```

**Break function parameters onto separate lines when there are three or more**, with a trailing comma.

```kotlin
// 2 params — inline is fine
fun FeedTopBar(elevated: Boolean, onOpenSettings: () -> Unit)

// 3+ params — one per line
fun HomeScreen(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    actions: HomeActions,
    onOpenSettings: () -> Unit,
)
```

**An expression body ending in a trailing lambda keeps the brace on the declaration line.** Never break after the `=` — break *inside* the lambda instead.

```kotlin
// Yes
override fun onToggleLike(postId: PostId) = launchCatching {
    postRepository.toggleLike(postId)
}

// No
override fun onToggleLike(postId: PostId) =
    launchCatching { postRepository.toggleLike(postId) }
```

A **block of work** — anything launched, a coroutine body, a test body — always takes its own lines, so a ViewModel's actions read as one shape. A **small value expression** stays inline until it no longer fits:

```kotlin
override fun onNicknameChange(value: String) = updateForm { it.copy(nickname = value) }

override fun onAgeChange(value: String) = updateForm { form ->
    form.copy(age = value.filter { it.isDigit() }.take(3))
}
```

**Use named arguments when the value doesn't self-document its role** — bare literals, same-typed arguments, anything opaque without context. Two cases always warrant them:

- *Lambda type parameters* — name them at the type declaration: `(Album, initialIndex: Int) -> Unit`, not `(Album, Int) -> Unit`.
- *Coordinate and geometry constructors* — `Offset(x = 0f, y = thickness / 2f)`, since `x`/`y` and `width`/`height` are easy to transpose.

Exception: enum constructor entries, where Kotlin doesn't allow named arguments.

**A file-level overview comment is a block comment (`/* … */`), not a KDoc** — ktlint's `no-consecutive-comments` fails the build otherwise.

## Localization

All user-facing text lives in `res/values/strings.xml` and is read with `stringResource(...)` — **never hardcode display strings in Kotlin.** Exception: strings with no words at all (`"$page / $total"`) are fine as plain interpolation. Navigation labels are `@StringRes` IDs on `ShellDestinations`. Post content in `SampleData` is stand-in data, not chrome, so it stays literal.

Computed text keeps its *logic* pure: `relativeTime()` and `compactCount()` in `app/util` return structured buckets with no display strings, and `common/Formatting.kt`'s `asText()` resolves a bucket to a localized resource. Anything counted needs `<plurals>` + `pluralStringResource(...)`, not `%d` — Czech buckets on `one`/`few`/`many`/`other`. Strings identical in every locale (the `Mosaic` wordmark, `English`/`Čeština`) are `translatable="false"`.

**Languages shipped: English (`values/`, default) and Czech (`values-cs/`).** Add one with a `values-<code>` folder plus an `AppLanguage` entry.

**The app is deliberately not listed under the system's per-app language settings** — no `android:localeConfig`, no `generateLocaleConfig`. The in-app picker is the only way in, which keeps `AppLocaleRepository` the single writer; its `StateFlow` mirrors the delegate and is only updated by its own `setLanguage`, so an external writer would leave it stale across the activity recreation a locale change triggers. **Do not re-enable `generateLocaleConfig`** without also making the repository re-read `AppCompatDelegate.getApplicationLocales()` on every activity create.

Switching uses the per-app language APIs, so the app persists nothing itself: `AppCompatLocaleRepository` calls `AppCompatDelegate.setApplicationLocales(…)`, which forwards to `LocaleManager` on 13+ and stores the choice itself below that. That backport is why `androidx.appcompat` is a dependency, why `MainActivity` extends `AppCompatActivity`, why `Theme.Mosaic` descends from `Theme.AppCompat`, and why the manifest declares `AppLocalesMetadataHolderService` with `autoStoreLocales=true`. `InMemoryAppLocaleRepository` is the test double.

**There is no "System" language option, deliberately** — with two languages shipped, a device set to a third would leave it silently meaning "English". Instead the device seeds the choice **once**: on first launch `MainActivity` calls `MainViewModel.resolveInitialLanguage()`, which pins the first device-preferred language we ship (`AppLanguage.fromLanguageTags`, ignoring region and Unicode-extension subtags) and falls back to English. It is a no-op once anything is stored, which is what keeps the app steady when the device's language later changes. The call stays in `MainActivity.onCreate` **after `super.onCreate`** — the locale APIs need the AppCompat delegate attached — rather than in a ViewModel `init`.
