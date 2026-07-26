# Code review — Mosaic (android-sample)

*Reviewed as an interviewer assessing a portfolio project. Scope: full source tree, build setup, docs, and a run of the JVM test suite and ktlint (both pass clean).*

## Overall impression

This is a strong portfolio piece. The architecture is coherent and — unusually — *enforced*: the Konsist `ArchitectureTest` derives its rules from the package shape instead of hardcoded lists, the navigation entry-identity problem (two pushes of the same screen sharing ViewModel state) is correctly diagnosed and solved, process death is handled deliberately on every screen, and the wire format is pinned with real Retrofit-over-MockWebServer tests rather than fakes. Hand-written fakes, no mocking framework, plain-JVM ViewModels — the testing philosophy in AGENTS.md is actually followed, which is rarer than having one.

The findings below are what I would raise in an interview. None of them undermine the architecture; several are the kind of production-hardening gaps that distinguish "demonstrates architecture" from "I would ship this".

---

## High — user-visible correctness

### 1. ~~A failed pull-to-refresh throws away a loaded feed~~

In `HomeViewModel` the error check wins over loaded content ([HomeViewModel.kt:69](app/src/main/java/uno/lux/sample/feed/ui/HomeViewModel.kt)): `if (loadError != null) return@combine HomeUiState.Error(loadError)` runs before the `FeedState.Loaded` branch. Pull to refresh in a tunnel and the feed you were reading is replaced by a full-screen error; tapping Retry then resets to `NotLoaded` and a spinner. `ProfileViewModel` resolves the same situation the other way — `state != null` wins over `loadError` ([ProfileViewModel.kt:147-159](app/src/main/java/uno/lux/sample/profile/ui/ProfileViewModel.kt)) — so a failed profile refresh keeps the content. The two screens disagree, and the feed's behavior is the wrong one. `HomeViewModelTest` covers the initial-load failure but not refresh-failure-over-loaded-content, which is how this slipped through. A refresh failure over existing content should surface as a transient (snackbar), with the full-screen error reserved for having nothing to show.

### 2. ~~`commentCount` goes stale the moment you comment~~

`PostDetailViewModel.addComment` prepends the new comment to its local list ([PostDetailViewModel.kt:241-244](app/src/main/java/uno/lux/sample/post/ui/PostDetailViewModel.kt)), but nothing updates `Post.commentCount` in the shared entity store. The count under the post — on its own detail page, on the feed card, on the profile — keeps showing the old number until a full re-fetch. The store propagates like counts on toggle; comments deserve the same treatment (a `PostRepository.commentAdded(postId)` bumping the entity would do it).

### 3. A failed page load leaves an infinite spinner with no way to retry

`loadMore` swallows its error (`ignoreErrors`, [HomeViewModel.kt:124-126](app/src/main/java/uno/lux/sample/feed/ui/HomeViewModel.kt)), `endReached` stays false, so `LoadingMoreFooter` keeps spinning. Worse, `LoadMoreEffect`'s trigger is `distinctUntilChanged` on a boolean that is still `true` ([Pagination.kt:36-44](app/src/main/java/uno/lux/sample/common/ui/Pagination.kt)), so it won't even re-fire until the user scrolls away from the end and back. Offline at page 2 means a spinner that never resolves. Pagination needs an error state: a "couldn't load more — tap to retry" footer.

### 4. Fire-and-forget mutations fail silently

Every mutation goes through `launchCatching`, which logs and discards ([StateFlows.kt:71-73](app/src/main/java/uno/lux/sample/app/util/StateFlows.kt)). For a like toggle that's defensible. But **delete** is in the same bucket ([PostDetailViewModel.kt:219-224](app/src/main/java/uno/lux/sample/post/ui/PostDetailViewModel.kt)): if the DELETE request fails, the confirmation dialog has closed, nothing pops, nothing appears — the user taps delete and the app just doesn't react. Follow toggles and reports behave the same. There is no snackbar/toast channel anywhere in the app for "that didn't go through". This is the single biggest UX robustness gap: at minimum, delete and follow should surface failure.

### 5. Like/bookmark toggles: no optimistic update, plus a stale write-back window

Two related issues in `PostRepository.toggleLike` ([PostRepository.kt:97-101](app/src/main/java/uno/lux/sample/post/data/PostRepository.kt)):

- **Not optimistic.** The heart doesn't fill until the round trip completes. On a slow connection the app feels broken precisely on its most-tapped affordance. The architecture (single entity store, one writer) is ideally placed for optimistic-apply-then-reconcile; it's just not done.
- **Stale write-back.** The post is read *before* the request and the update is `post.copy(isLiked, likeCount)` from that pre-request snapshot ([NetworkPostDataSource.kt:45-48](app/src/main/java/uno/lux/sample/post/data/network/NetworkPostDataSource.kt)). If a refresh ingests a fresher entity while the toggle is in flight, the write-back resurrects the stale copy's other fields (comment count, edited body). Narrow window, but it's the kind of race a reviewer probes: the fix is to re-read the current entity at write time and apply only the toggled fields.

The wire protocol compounds this: `POST …/toggle` is non-idempotent, so a timeout-and-retry (or two rapid taps racing) can double-toggle. An idempotent `PUT like=true/false` is the safer contract, and it's your backend, so both halves are changeable.

---

## Medium — security and robustness

### 6. `usesCleartextTraffic="true"` in the release manifest

[AndroidManifest.xml:16](app/src/main/AndroidManifest.xml) sets the flag globally. The debug source set already has the right mechanism — a network security config permitting cleartext to `10.0.2.2` only ([network_security_config.xml](app/src/debug/res/xml/network_security_config.xml)) — and where an NSC is present it *overrides* the manifest flag. So the flag has no effect in debug and exactly one effect in release: allowing cleartext HTTP to **any** host in the build that talks to the HTTPS production backend. It should be deleted.

### 7. Backup rules are untouched template boilerplate

[backup_rules.xml](app/src/main/res/xml/backup_rules.xml) still contains the "Sample backup rules file; uncomment and customize" scaffold comment with `allowBackup="true"`. Harmless here (the only persisted data is theme/auto-play), but it reads as "never looked at" — for a portfolio, either configure it meaningfully or disable backup and say why.

### 8. All HTTP failures collapse to `AppError.Unknown`

`toAppError()` maps three connectivity exceptions and nothing else ([AppError.kt:20-24](app/src/main/java/uno/lux/sample/app/util/AppError.kt)) — `HttpException` isn't even mentioned, so a 422, 403 or 500 all render as the same generic message. The composer mirrors server validations client-side, which covers the happy path, but the day the mirrors drift (the stated risk in AGENTS.md), the server's structured 422 arrives and the user sees "something went wrong". An `AppError.Http(code)` case, and parsing the Rails error body for the composer, would close this. (Also: the KDoc references `uno.lux.sample.design.format.asText`, a package that no longer exists.)

### 9. Uploads buffer entire files in memory

`FileLoader.read` does `readBytes()` ([FileLoader.kt:38](app/src/main/java/uno/lux/sample/common/data/files/FileLoader.kt)) and `FileUpload.asPart` wraps the byte array ([MultipartParts.kt:22-26](app/src/main/java/uno/lux/sample/common/data/network/MultipartParts.kt)). Publishing ten photos plus — in the limit — a 25 MB video means tens of megabytes of heap held across the multipart write. The `MultipartParts` KDoc even names the fix ("streaming from the URI instead of a byte array, say"). A custom `RequestBody` writing from `ContentResolver.openInputStream` streams it in constant memory. The size check *before* reading is good; the read itself undoes the benefit.

### 10. ExoPlayer requests no audio focus

`ExoPlayer.Builder(appContext).build()` ([VideoPlayback.kt:127](app/src/main/java/uno/lux/sample/video/ui/VideoPlayback.kt)) never calls `setAudioAttributes(attrs, /* handleAudioFocus = */ true)` or `setHandleAudioBecomingNoisy(true)`. Feed videos will play over the user's music without pausing it, and keep blasting through the speaker when headphones unplug. For an app whose feed autoplays video, audio focus is table stakes.

### 11. Network client defaults

`OkHttpClient` uses default timeouts, no retry/backoff policy, and no cache. Fine for a demo, but worth being able to defend. Separately, Coil's `ImageLoader` builds its own OkHttp client rather than sharing the app's — shared connection pool and interceptors are one `.components`/`callFactory` line.

---

## Process and portfolio presentation

### 12. There is no CI

No `.github/workflows`, nothing. The project has a fast JVM suite, ktlint, Android Lint and an architecture test — everything a pipeline wants — and nothing runs them on push. For a portfolio repo this is the highest-leverage missing piece: a green Actions badge on the README is the first proof a reviewer sees that the test story is real. A single workflow running `testDebugUnitTest`, `ktlintCheck` and `lintDebug` would take under an hour to add.

### 13. README has drifted from the tree it describes

The README's package diagram shows `app/common/`, `app/core/`, `app/format/` — none of which exist. `common/` is top-level now, `core/` is gone, `format/` became `common/Formatting.kt`; the worked examples repeat the stale paths (`FileLoader` → "`app/core/files/`" vs. the real `common/data/files/`). AGENTS.md was updated in the "docs drift" commit; the README — the file an interviewer actually reads first — was not. Also: "354 JVM unit tests" is a literal count that rots with every commit; "the CI badge above" ages better than a number.

Smaller drift in the same family: AGENTS.md calls Android Lint "the only linter configured" while the ktlint Gradle plugin is applied in both build scripts; AGENTS.md says `MosaicApp` supplies the Coil `ImageLoader` (it's `MosaicApplication`); and `DataStoreSettingsRepository`'s KDoc describes "a one-time migration from the legacy SharedPreferences file" that exists nowhere in the code ([DataStoreSettingsRepository.kt:17-19](app/src/main/java/uno/lux/sample/settings/data/DataStoreSettingsRepository.kt)) — the DataStore is created with no migrations ([DataModule.kt:110-115](app/src/main/java/uno/lux/sample/app/di/DataModule.kt)). In a codebase whose comments are this load-bearing, a comment describing machinery that isn't there is worse than no comment.

### 14. PLAN.md is stale

Two of its four items (pagination, configuration-change survival) are done and extensively tested; keeping them listed as open work undersells the project. Either delete the file or turn it into an honest "known limitations / next steps" section — which, given findings 1–5 above, could be genuinely impressive rather than apologetic.

### 15. Test-pyramid gaps

The JVM suite is excellent and the instrumented tests target exactly what JVM can't reach — good judgment. What's absent: **screenshot tests** (Paparazzi/Roborazzi would exercise the Mosaic design system, the thing this portfolio is selling visually, on the JVM at unit-test cost) and any **Compose UI behavior tests** runnable without a device (Robolectric). Both are cheap to add and conspicuous by absence in a "tests first" codebase.

---

## Minor / style

- **`EditProfileViewModel` uses the array-overload `combine` with unchecked casts** (`args[0] as EditProfileForm?`, [EditProfileViewModel.kt:59-72](app/src/main/java/uno/lux/sample/user/ui/EditProfileViewModel.kt)) while `ProfileViewModel` solved the same arity problem with a typed pairing class (`LazyTabs`). Be consistent; the typed approach is the one worth showing off.
- **`Modifier.composed` in `debouncedClickable`** ([ClickDebounce.kt:46](app/src/main/java/uno/lux/sample/app/util/ClickDebounce.kt)) — the Compose team discourages `composed` for performance (it defeats modifier skipping/reuse); `Modifier.Node` is the current answer. Low impact at one call site, but it's the kind of "is this current?" detail an interviewer probes.
- **`derivedIds` re-filters and re-sorts the entire entity store on every store emission** ([ProfileRepository.kt:177-188](app/src/main/java/uno/lux/sample/profile/data/ProfileRepository.kt)). Fine at demo scale and the comment defends the design, but it's O(N log N) per emission per subscribed tab; worth knowing where the cliff is.
- **Comments have no pagination** — `loadComments` fetches the whole thread; a post with the fixture's 612 comments would fetch all of them in one response.
- **No API versioning** (`/api/posts`, not `/api/v1/posts`) — PLAN.md already flags it; the client has no protection against a breaking backend change.
- **The `X-User-Id` header is trust-the-client authentication.** Documented as a deliberate no-sign-in choice and fine for a sample, but the README should say explicitly that impersonation is one header away, so a reviewer knows you know.

---

## What I'd ask in the interview

1. Walk me through what happens when the user taps delete on a post with airplane mode on. *(Finding 4 — checks whether the silent-failure design was conscious.)*
2. Why isn't the like toggle optimistic, given the entity store makes it easy? *(Finding 5.)*
3. Your README's tree and your source tree disagree — which one is the design? *(Finding 13.)*
4. The architecture test is impressive — what class of regression can it *not* catch? *(Expected answer: cross-slice dependency direction, which AGENTS.md acknowledges is reviewed by hand.)*

## Verdict

The hard problems — navigation identity, process death, a normalized store with instance-preserving updates, executable architecture — are solved well and explained better. What's missing is the production edge: error surfacing, offline behavior at the margins, CI, and doc/tree agreement. Fix findings 1–4, add a CI workflow, and re-sync the README, and this reads like the work of someone who has shipped, not just studied.
