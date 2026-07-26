# Plan

Open work. Anything documented in [AGENTS.md](AGENTS.md) as existing behaviour has already landed —
check there before starting an item, since this file is the backlog and not a status report.

Ordered by severity. Each item names the files to start from, so it stands on its own.

## High — user-visible correctness

### A failed page load spins forever with no way to retry
`loadMore` swallows its error via `ignoreErrors` (`feed/ui/HomeViewModel.kt`) and `endReached` stays
false, so `LoadingMoreFooter` keeps spinning. `LoadMoreEffect`'s trigger is `distinctUntilChanged` on
a boolean that is still `true` (`common/ui/Pagination.kt`), so it won't re-fire until the user
scrolls away from the end and back. Offline at page 2 is an unresolvable spinner. Pagination needs an
error state and a "couldn't load more — tap to retry" footer.

### Mutations fail silently
Every mutation goes through `launchCatching`, which logs and discards (`app/util/StateFlows.kt`).
Defensible for a like toggle; wrong for **delete** (`post/ui/PostDetailViewModel.kt`) — the
confirmation dialog closes, nothing pops, nothing appears, and the user's tap simply does nothing.
Follow toggles and reports behave the same. There is no app-wide channel for "that didn't go
through"; at minimum delete and follow must surface failure.

### Like/bookmark: not optimistic, and a stale write-back window
In `PostRepository.toggleLike` (`post/data/PostRepository.kt`):
- **Not optimistic** — the heart doesn't fill until the round trip finishes, so the most-tapped
  affordance in the app feels broken on a slow connection. The single entity store with one writer is
  ideally placed for optimistic-apply-then-reconcile.
- **Stale write-back** — the post is read *before* the request and updated from that pre-request
  snapshot (`post/data/network/NetworkPostDataSource.kt`). A refresh landing mid-flight gets its
  fresher entity clobbered. Re-read the entity at write time and apply only the toggled fields.

`POST …/toggle` is also non-idempotent, so a timeout-retry or two rapid taps can double-toggle. An
idempotent `PUT like=true/false` is the safer contract — and the backend is ours to change.

## Medium — robustness

### All HTTP failures collapse to `AppError.Unknown`
`toAppError()` maps three connectivity exceptions and never mentions `HttpException`
(`app/util/AppError.kt`), so a 422, 403 and 500 all render the same generic message. The composer
mirrors server validations client-side, but the day those mirrors drift the server's structured 422
arrives and the user sees "something went wrong". Add an `AppError.Http(code)` case and parse the
Rails error body for the composer.

### Uploads buffer entire files in memory
`FileLoader.read` calls `readBytes()` (`common/data/files/FileLoader.kt`) and `FileUpload.asPart`
wraps the byte array (`common/data/network/MultipartParts.kt`), so ten photos plus a 25 MB video sit
on the heap across the whole multipart write. A custom `RequestBody` streaming from
`ContentResolver.openInputStream` holds constant memory. The size check *before* the read is right;
the read undoes it.

### ExoPlayer requests no audio focus
`ExoPlayer.Builder(appContext).build()` (`video/ui/VideoPlayback.kt`) never calls
`setAudioAttributes(attrs, handleAudioFocus = true)` or `setHandleAudioBecomingNoisy(true)`. Feed
videos play over the user's music without pausing it and keep playing out of the speaker when
headphones are unplugged. For an autoplaying feed this is table stakes.

### Network client defaults
`OkHttpClient` uses default timeouts with no retry/backoff and no cache. Defensible for a demo, but
worth a deliberate choice. Coil's `ImageLoader` (`app/MosaicApplication.kt`) also builds its own
OkHttp client instead of sharing the app's — a shared connection pool and interceptors are one
`callFactory` line.

### Backup rules are untouched boilerplate
`res/xml/backup_rules.xml` still carries the "Sample backup rules file; uncomment and customize"
scaffold with `allowBackup="true"`. Harmless (only theme and auto-play persist) but it reads as never
looked at — configure it meaningfully or disable backup and say why.

## Backend / API

### Comments have no pagination
`loadComments` fetches an entire thread; a post with the fixture's 612 comments pulls all of them in
one response.

## Code quality

- **`EditProfileViewModel` uses the array-overload `combine` with unchecked casts**
  (`args[0] as EditProfileForm?`, `user/ui/EditProfileViewModel.kt`) while `ProfileViewModel` solved
  the same arity problem with a typed pairing class (`LazyTabs`). The typed approach is the one worth
  showing off.
- **`Modifier.composed` in `debouncedClickable`** (`app/util/ClickDebounce.kt`) — the Compose team
  discourages `composed` because it defeats modifier skipping and reuse; `Modifier.Node` is the
  current answer.
- **`derivedIds` re-filters and re-sorts the whole entity store on every emission**
  (`profile/data/ProfileRepository.kt`) — O(N log N) per emission per subscribed tab. Fine at demo
  scale; worth knowing where the cliff is.

## Doc accuracy

- **Two KDocs describe things that don't exist.** `app/util/AppError.kt` links
  `uno.lux.sample.design.format.asText`, a package that is gone; `settings/data/DataStoreSettingsRepository.kt`
  describes "a one-time migration from the legacy SharedPreferences file" that appears nowhere — the
  store is created with no migrations (`app/di/DataModule.kt`). In a codebase whose comments are this
  load-bearing, a comment describing absent machinery is worse than none.
- **The README should state that `X-User-Id` is trust-the-client authentication.** It's a deliberate
  no-sign-in choice and fine for a sample, but impersonation is one header away and a reviewer should
  see that we know it.
