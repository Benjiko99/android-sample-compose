package uno.lux.sample.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.UserId
import uno.lux.sample.data.user.UserRepository

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
 * Those two lists also *narrow* as the entities change: a list defined by a viewer-scoped flag has
 * to drop a post the moment the flag clears, or unsaving from the Saved tab would leave the row
 * sitting there. That derivation is the same [PostRepository.entities] join the ordered IDs already
 * go through — it is not a second copy of the state to keep in step, so un-saving and saving again
 * (the accidental tap) restores the row for free. It applies only to the signed-in user's own
 * lists: `isLiked`/`isBookmarked` describe *the viewer*, so on someone else's Likes tab they say
 * nothing about why a post is on that list.
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
        private val _ids = MutableStateFlow<Map<UserId, List<PostId>>>(emptyMap())
        private val _page = MutableStateFlow<Map<UserId, PageState>>(emptyMap())

        /**
         * The fetched order, narrowed to the posts [stillBelongs] accepts — which is what drops a
         * row the instant its like/bookmark is toggled off, from this tab or anywhere else. A post
         * not yet in the entity store is kept: the ID is all we know, and the caller resolving it
         * drops it anyway.
         */
        fun ids(userId: UserId): Flow<List<PostId>?> =
            combine(_ids, postRepository.entities) { idsByUser, entities ->
                val ids = idsByUser[userId] ?: return@combine null

                if (userId != currentUserId) ids
                else ids.filter { id -> entities[id]?.let(stillBelongs) ?: true }
            }.distinctUntilChanged()

        fun hasMore(userId: UserId): Flow<Boolean> = _page.map { it[userId]?.hasMore ?: false }

        fun hasLoaded(userId: UserId): Boolean = _ids.value.containsKey(userId)

        suspend fun refresh(userId: UserId) {
            val page = ingest(fetchPage(userId, null))

            _page.update { it + (userId to PageState(page.cursor, page.hasMore)) }
            _ids.update { it + (userId to page.posts.map { post -> post.id }) }
        }

        suspend fun loadMore(userId: UserId) {
            val current = _page.value[userId] ?: return
            if (!current.hasMore) return

            val page = ingest(fetchPage(userId, current.cursor))

            _page.update { it + (userId to PageState(page.cursor, page.hasMore)) }
            _ids.update { map ->
                val existing = map[userId] ?: emptyList()
                map + (userId to existing + page.posts.map { it.id })
            }
        }

        private fun ingest(page: PostsWithAuthorsPage): PostsWithAuthorsPage {
            postRepository.ingest(page.posts)
            userRepository.ingest(page.users)

            return page
        }
    }
}
