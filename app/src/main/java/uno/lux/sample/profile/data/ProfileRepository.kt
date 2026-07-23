package uno.lux.sample.profile.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import uno.lux.sample.post.Post
import uno.lux.sample.post.PostId
import uno.lux.sample.post.PostKey
import uno.lux.sample.post.data.PostRepository
import uno.lux.sample.post.key
import uno.lux.sample.profile.Profile
import uno.lux.sample.user.UserId
import uno.lux.sample.user.data.UserRepository

/**
 * Source of truth for a user's profile metadata and the ordered IDs of their posts.
 *
 * [profile] streams the counts for a given user. [postIds] streams the ordered list of post IDs
 * authored by that user; callers resolve them via [PostRepository.entities] so mutations (likes,
 * bookmarks) are reflected without any involvement from this repository. Post mutations go
 * directly through [PostRepository] — not through here.
 *
 * [hasMorePosts] signals whether another page exists; [loadMorePosts] appends it into the flows
 * above.
 *
 * [bookmarkIds] and [likeIds] are the same arrangement for the posts a user saved and liked, with
 * two differences: each is loaded on demand ([refreshBookmarks] / [refreshLikes]) rather than by
 * [refresh] — a tab nobody opened costs no request, and the Saved one is private to its owner
 * besides — and each emits `null` until its first load lands, which is how a caller tells an empty
 * tab from an unopened one. Their authors are arbitrary users, so they are ingested into
 * [UserRepository] the way the feed's are.
 *
 * For the signed-in user those two lists are **derived from the flag, not echoed from the fetch**:
 * a list defined by `isBookmarked`/`isLiked` *is* the set of entities carrying that flag, ordered
 * by the server's own `(createdAt, id)` keyset. Membership therefore moves both ways and from
 * anywhere — liking a post in the Posts tab or the feed inserts it in the Likes tab in the right
 * place, unliking removes it — with no re-fetch and no dependence on whether the post happened to
 * be in a page the server already sent. Paging is the only limit: a post below the loaded window's
 * floor is held back until the page it belongs to arrives, so it can't jump the queue.
 *
 * None of that applies to *another* user's Likes tab, which is echoed exactly as fetched:
 * `isLiked`/`isBookmarked` are viewer-scoped — on their profile the flags describe you, not them —
 * so the fetched order is the only thing that says what belongs there. Hence [currentUserId].
 *
 * Where the data comes from — in-memory sample data vs. a live network call — is decided by
 * [ProfileDataSource].
 */
class ProfileRepository(
    private val dataSource: ProfileDataSource,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val currentUserId: UserId,
) {
    private val _profiles = MutableStateFlow<Map<UserId, Profile>>(emptyMap())
    private val _userPostIds = MutableStateFlow<Map<UserId, List<PostId>>>(emptyMap())

    private data class PageState(val cursor: String?, val hasMore: Boolean)

    /**
     * One on-demand tab's loaded window for one user: the IDs the server sent, where to continue
     * from, and [oldestLoaded] — the key of the last post delivered, i.e. the floor of what has
     * been paged in. Absent until that tab's first load lands.
     */
    private data class TabState(
        val ids: List<PostId>,
        val cursor: String?,
        val hasMore: Boolean,
        val oldestLoaded: PostKey?,
    )

    private val _postPage = MutableStateFlow<Map<UserId, PageState>>(emptyMap())

    // The two on-demand tabs. They differ only in which endpoint fills them and which flag keeps
    // a post on the list — the ordering, paging and "not asked yet" bookkeeping is identical, so
    // it lives in one place.
    private val savedPosts = OnDemandPostIds(dataSource::bookmarks, Post::isBookmarked)
    private val likedPosts = OnDemandPostIds(dataSource::likes, Post::isLiked)

    fun profile(userId: UserId): Flow<Profile> =
        _profiles.map { it[userId] ?: emptyProfile(userId) }

    fun postIds(userId: UserId): Flow<List<PostId>> =
        _userPostIds.map { it[userId] ?: emptyList() }

    fun hasMorePosts(userId: UserId): Flow<Boolean> =
        _postPage.map { it[userId]?.hasMore ?: false }

    /** The saved post IDs in display order, or `null` before [refreshBookmarks] has run. */
    fun bookmarkIds(userId: UserId): Flow<List<PostId>?> = savedPosts.ids(userId)

    fun hasMoreBookmarks(userId: UserId): Flow<Boolean> = savedPosts.hasMore(userId)

    /** The liked post IDs in display order, or `null` before [refreshLikes] has run. */
    fun likeIds(userId: UserId): Flow<List<PostId>?> = likedPosts.ids(userId)

    fun hasMoreLikes(userId: UserId): Flow<Boolean> = likedPosts.hasMore(userId)

    suspend fun refresh(userId: UserId) {
        val data = dataSource.refresh(userId)

        postRepository.ingest(data.posts)

        _postPage.update { it + (userId to PageState(data.postCursor, data.postHasMore)) }

        _userPostIds.update { it + (userId to data.posts.map { p -> p.id }) }
        _profiles.update {
            it + (userId to Profile(userId = userId, postsCount = data.postsCount))
        }
    }

    suspend fun loadMorePosts(userId: UserId) {
        val page = _postPage.value[userId] ?: return
        if (!page.hasMore) return

        val result = dataSource.loadMorePosts(userId, page.cursor)
        postRepository.ingest(result.posts)

        _postPage.update { it + (userId to PageState(result.cursor, result.hasMore)) }
        _userPostIds.update { map ->
            val existing = map[userId] ?: emptyList()
            map + (userId to existing + result.posts.map { it.id })
        }
    }

    /** Whether [refreshBookmarks] has ever completed for [userId] — the caller's load-once guard. */
    fun hasLoadedBookmarks(userId: UserId): Boolean = savedPosts.hasLoaded(userId)

    suspend fun refreshBookmarks(userId: UserId) = savedPosts.refresh(userId)

    suspend fun loadMoreBookmarks(userId: UserId) = savedPosts.loadMore(userId)

    /** Whether [refreshLikes] has ever completed for [userId] — the caller's load-once guard. */
    fun hasLoadedLikes(userId: UserId): Boolean = likedPosts.hasLoaded(userId)

    suspend fun refreshLikes(userId: UserId) = likedPosts.refresh(userId)

    suspend fun loadMoreLikes(userId: UserId) = likedPosts.loadMore(userId)

    private fun emptyProfile(userId: UserId) = Profile(userId = userId, postsCount = 0)

    /**
     * One profile tab's worth of post IDs per user, filled on demand by [fetchPage] and paged
     * with its cursor. [ids] emits `null` until the first [refresh] lands, so a caller can tell
     * an empty tab from an unopened one; the posts and their authors go into the shared entity
     * stores on the way through, which is what lets a like toggled anywhere reach these lists.
     */
    private inner class OnDemandPostIds(
        private val fetchPage: suspend (UserId, String?) -> PostsWithAuthorsPage,
        private val stillBelongs: (Post) -> Boolean,
    ) {
        private val _state = MutableStateFlow<Map<UserId, TabState>>(emptyMap())

        /**
         * Which of the two shapes a tab takes is fixed by whose profile it is, so the choice is
         * made once here rather than on every emission — an echoed list has no reason to watch the
         * entity store at all.
         */
        fun ids(userId: UserId): Flow<List<PostId>?> =
            if (userId == currentUserId) derivedIds(userId) else echoedIds(userId)

        /**
         * Your own list is *derived*, not echoed: every post [stillBelongs] accepts, in the
         * server's own `(createdAt, id)` order. So liking a post anywhere — the Posts tab, the
         * feed, a post's detail screen — inserts it here in the right place, and unliking removes
         * it, with no re-fetch and no asymmetry between the two directions.
         *
         * The one thing paging costs: a post older than [TabState.oldestLoaded] belongs to a page
         * the server hasn't sent yet, so it is held back rather than jumped to the end of a partial
         * list. It arrives in order once that page is paged in. A fully-loaded list has no floor.
         */
        private fun derivedIds(userId: UserId): Flow<List<PostId>?> =
            combine(_state, postRepository.entities) { states, entities ->
                val state = states[userId] ?: return@combine null
                val floor = state.oldestLoaded.takeIf { state.hasMore }

                entities.values
                    .filter(stillBelongs)
                    .map { it.key }
                    .filter { floor == null || it >= floor }
                    .sortedDescending()
                    .map { it.id }
            }.distinctUntilChanged()

        /**
         * Someone else's list can only be echoed. [stillBelongs] reads a viewer-scoped flag, which
         * on their profile describes *you*, not them — it says nothing about what belongs there.
         */
        private fun echoedIds(userId: UserId): Flow<List<PostId>?> =
            _state.map { it[userId]?.ids }.distinctUntilChanged()

        fun hasMore(userId: UserId): Flow<Boolean> = _state.map { it[userId]?.hasMore ?: false }

        fun hasLoaded(userId: UserId): Boolean = _state.value.containsKey(userId)

        suspend fun refresh(userId: UserId) {
            val page = ingest(fetchPage(userId, null))

            _state.update {
                it + (userId to TabState(
                    ids = page.posts.map { post -> post.id },
                    cursor = page.cursor,
                    hasMore = page.hasMore,
                    oldestLoaded = page.posts.lastOrNull()?.key,
                ))
            }
        }

        suspend fun loadMore(userId: UserId) {
            val current = _state.value[userId] ?: return
            if (!current.hasMore) return

            val page = ingest(fetchPage(userId, current.cursor))

            _state.update {
                it + (userId to TabState(
                    ids = current.ids + page.posts.map { post -> post.id },
                    cursor = page.cursor,
                    hasMore = page.hasMore,
                    // An empty page moves the cursor but not the floor.
                    oldestLoaded = page.posts.lastOrNull()?.key ?: current.oldestLoaded,
                ))
            }
        }

        private fun ingest(page: PostsWithAuthorsPage): PostsWithAuthorsPage {
            postRepository.ingest(page.posts)
            userRepository.ingest(page.users)

            return page
        }
    }
}
