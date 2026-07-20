package uno.lux.sample.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
 * [bookmarkIds] is the same arrangement for the posts a user saved, with two differences: it is
 * loaded on demand by [refreshBookmarks] rather than by [refresh] — the Saved tab is private to
 * its owner, so nothing fetches it until they open it — and it emits `null` until that first load
 * lands, which is how a caller tells "no saved posts" from "not asked yet". Their authors are
 * arbitrary users, so they are ingested into [UserRepository] the way the feed's are.
 *
 * Where the data comes from — in-memory sample data vs. a live network call — is decided by
 * [ProfileDataSource].
 */
class ProfileRepository(
    private val dataSource: ProfileDataSource,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {
    private val _profiles = MutableStateFlow<Map<UserId, Profile>>(emptyMap())
    private val _userPostIds = MutableStateFlow<Map<UserId, List<PostId>>>(emptyMap())
    private val _bookmarkIds = MutableStateFlow<Map<UserId, List<PostId>>>(emptyMap())

    private data class PageState(val cursor: String?, val hasMore: Boolean)

    private val _postPage = MutableStateFlow<Map<UserId, PageState>>(emptyMap())
    private val _bookmarkPage = MutableStateFlow<Map<UserId, PageState>>(emptyMap())

    fun profile(userId: UserId): Flow<Profile> =
        _profiles.map { it[userId] ?: emptyProfile(userId) }

    fun postIds(userId: UserId): Flow<List<PostId>> =
        _userPostIds.map { it[userId] ?: emptyList() }

    fun hasMorePosts(userId: UserId): Flow<Boolean> =
        _postPage.map { it[userId]?.hasMore ?: false }

    /** The saved post IDs in display order, or `null` before [refreshBookmarks] has run. */
    fun bookmarkIds(userId: UserId): Flow<List<PostId>?> = _bookmarkIds.map { it[userId] }

    fun hasMoreBookmarks(userId: UserId): Flow<Boolean> =
        _bookmarkPage.map { it[userId]?.hasMore ?: false }

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
    fun hasLoadedBookmarks(userId: UserId): Boolean = _bookmarkIds.value.containsKey(userId)

    suspend fun refreshBookmarks(userId: UserId) {
        val page = dataSource.bookmarks(userId, cursor = null)

        ingestBookmarks(page)

        _bookmarkPage.update { it + (userId to PageState(page.cursor, page.hasMore)) }
        _bookmarkIds.update { it + (userId to page.posts.map { post -> post.id }) }
    }

    suspend fun loadMoreBookmarks(userId: UserId) {
        val page = _bookmarkPage.value[userId] ?: return
        if (!page.hasMore) return

        val result = dataSource.bookmarks(userId, page.cursor)
        ingestBookmarks(result)

        _bookmarkPage.update { it + (userId to PageState(result.cursor, result.hasMore)) }
        _bookmarkIds.update { map ->
            val existing = map[userId] ?: emptyList()
            map + (userId to existing + result.posts.map { it.id })
        }
    }

    private fun ingestBookmarks(page: BookmarksPage) {
        postRepository.ingest(page.posts)
        userRepository.ingest(page.users)
    }

    private fun emptyProfile(userId: UserId) = Profile(userId = userId, postsCount = 0)
}
