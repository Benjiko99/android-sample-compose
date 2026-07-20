package uno.lux.sample.ui.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import uno.lux.sample.ui.components.debouncedClickable
import uno.lux.sample.ui.components.rememberDebounced
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uno.lux.sample.R
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.SampleUsers
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserId
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.profile.Profile
import uno.lux.sample.ui.components.Avatar
import uno.lux.sample.ui.components.FullScreenError
import uno.lux.sample.ui.components.FullScreenProgress
import uno.lux.sample.ui.components.LoadMoreEffect
import uno.lux.sample.ui.components.LoadingMoreFooter
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import uno.lux.sample.ui.post.PostCard
import uno.lux.sample.ui.post.PostCardData
import uno.lux.sample.ui.components.MosaicGradients
import uno.lux.sample.ui.components.ScrimIconButton
import uno.lux.sample.ui.format.asText
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.util.compactCount
import uno.lux.sample.util.createActionsProxy

/**
 * The profile's ViewModel-backed intents — liking / bookmarking the viewed user's posts, plus
 * navigation (opening a post, viewer or the editor, going back), which the ViewModel forwards
 * to the injected `Navigator` — as one [Stable] seam the stateless [ProfileScreen] depends on.
 * [ProfileViewModel] implements it, so the binder passes the ViewModel directly and a preview
 * passes a no-op [createActionsProxy].
 */
@Stable
interface ProfileActions {
    fun onToggleLike(postId: PostId)
    fun onToggleBookmark(postId: PostId)
    fun onDeletePost(postId: PostId)
    fun onToggleFollow()
    fun loadMorePosts()
    fun onSavedTabShown()
    fun loadMoreBookmarks()
    fun goBack()
    fun openEditProfile()
    fun openPost(postId: PostId)
    fun openProfile(userId: UserId)
    fun openVideo(video: Video)
    fun openAlbum(imageUrls: List<String>, initialIndex: Int)
    fun openAvatar(avatarUrl: String)
}

/**
 * Stateful entry point: binds a [ProfileViewModel] for [userId] and forwards state and intent
 * to the stateless overload below. [showBackButton] is false when the profile is shown as a
 * root tab (no up-affordance); true when it was pushed over the feed.
 */
@Composable
fun ProfileScreen(
    userId: UserId,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    // The ViewModel store is per back-stack entry, so each opened profile page gets its own
    // ProfileViewModel, created for that entry's userId and cleared when the page pops.
    viewModel: ProfileViewModel = hiltViewModel<ProfileViewModel, ProfileViewModel.Factory>(
        creationCallback = { factory -> factory.create(userId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        actions = viewModel,
        modifier = modifier,
        onBack = if (showBackButton) viewModel::goBack else null,
    )
}

/**
 * Stateless profile screen — renders [uiState] and reports interactions through [actions].
 * Holding no ViewModel makes it directly previewable and testable. [onBack] carries the one
 * piece of navigation the interface can't: whether this instance shows an up-affordance at all
 * (null on the root tab).
 */
@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (uiState) {
            ProfileUiState.Loading -> {
                FullScreenProgress()
                if (onBack != null) PlainBackButton(onBack, Modifier.align(Alignment.TopStart))
            }

            is ProfileUiState.Error -> {
                FullScreenError(
                    message = uiState.error.asText(),
                    onRetry = onRetry,
                )
                if (onBack != null) PlainBackButton(onBack, Modifier.align(Alignment.TopStart))
            }

            ProfileUiState.NotFound -> {
                CenteredMessage(stringResource(R.string.profile_not_found))
                if (onBack != null) PlainBackButton(onBack, Modifier.align(Alignment.TopStart))
            }

            // The loaded state owns its own scroll-reactive TopAppBar over the cover.
            is ProfileUiState.Loaded -> ProfileContent(
                data = uiState.data,
                isCurrentUser = uiState.isCurrentUser,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                actions = actions,
                onBack = onBack,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    data: ProfileScreenData,
    isCurrentUser: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    actions: ProfileActions,
    onBack: (() -> Unit)?,
) {
    // Saved is the signed-in user's own private list, so it is not offered on anyone else's
    // profile. isCurrentUser is fixed for the life of this screen, so the visible set is too.
    val tabs = remember(isCurrentUser) {
        ProfileTab.entries.filter { isCurrentUser || !it.ownerOnly }
    }
    var selectedTab by rememberSaveable { mutableStateOf(ProfileTab.POSTS) }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // The cover bleeds to the very top, so we can't reserve the bar's height with content
    // padding. Instead, the sticky tab header grows a top inset as it nears the bar, derived
    // from the header item's own geometry — independent of the inset, so it can't oscillate.
    val density = LocalDensity.current
    val barBottomPx = WindowInsets.statusBars.getTop(density) +
            with(density) { ProfileBarHeight.toPx() }
    val tabInset by remember(barBottomPx) {
        derivedStateOf {
            val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "header" }
            val tabsTop = if (header != null) (header.offset + header.size).toFloat() else 0f
            with(density) { (barBottomPx - tabsTop).coerceIn(0f, barBottomPx).toDp() }
        }
    }

    // Both tabs page the one LazyColumn, so the effect follows whichever is showing.
    when (selectedTab) {
        ProfileTab.POSTS -> LoadMoreEffect(
            listState = listState,
            endReached = data.postsEndReached,
            onLoadMore = actions::loadMorePosts,
        )

        ProfileTab.SAVED -> {
            LaunchedEffect(Unit) { actions.onSavedTabShown() }

            LoadMoreEffect(
                listState = listState,
                endReached = data.bookmarks?.endReached ?: true,
                onLoadMore = actions::loadMoreBookmarks,
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item(key = "header") {
                    ProfileHeader(
                        user = data.user,
                        isCurrentUser = isCurrentUser,
                        onEditProfile = actions::openEditProfile,
                        onToggleFollow = actions::onToggleFollow,
                        onOpenAvatar = actions::openAvatar,
                    )
                }
                stickyHeader(key = "tabs") {
                    ProfileTabs(
                        tabs = tabs,
                        selected = selectedTab,
                        onSelect = { selectedTab = it },
                        topInset = tabInset,
                    )
                }
                when (selectedTab) {
                    ProfileTab.POSTS -> postItems(
                        screenData = data,
                        actions = actions,
                        isCurrentUser = isCurrentUser,
                    )

                    ProfileTab.SAVED -> bookmarkItems(
                        bookmarks = data.bookmarks,
                        actions = actions,
                    )
                }
                item(key = "bottom-inset") {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
        ProfileTopBar(
            scrollBehavior = scrollBehavior,
            userName = data.user.nickname,
            onBack = onBack,
        )
    }
}

// region Header

private val CoverHeight = 160.dp
private val AvatarRingSize = 96.dp
private val AvatarSize = 88.dp
private val AvatarOverlap = 44.dp // how far the avatar hangs below the cover
private val ProfileBarHeight = 64.dp // Material small TopAppBar content height (excl. status bar)

@Composable
private fun ProfileHeader(
    user: User,
    isCurrentUser: Boolean,
    onEditProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenAvatar: (avatarUrl: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Cover + overlapping avatar + actions, in a box tall enough to contain the hanging
        // avatar so nothing below paints over it (the avatar is the last child, so it wins
        // the cover's z-order cleanly).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CoverHeight + (AvatarRingSize - AvatarOverlap)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CoverHeight)
                    .background(MosaicGradients.mediaBrush(user.id)),
            )
            // Primary action, bottom-right across from the avatar (settings live in the app
            // bar): Edit profile on your own profile, Follow/Unfollow on anyone else's.
            val actionModifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
            if (isCurrentUser) {
                FilledTonalButton(
                    onClick = onEditProfile.rememberDebounced(),
                    modifier = actionModifier,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_edit))
                }
            } else {
                FollowButton(
                    isFollowing = user.isFollowing,
                    onToggleFollow = onToggleFollow,
                    modifier = actionModifier,
                )
            }
            AvatarRing(
                userId = user.id,
                name = user.nickname,
                avatarUrl = user.avatarUrl,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp)
                    .padding(top = CoverHeight - AvatarOverlap)
                    .debouncedClickable(enabled = !user.avatarUrl.isNullOrEmpty(), onClick = {
                        if (user.avatarUrl != null) onOpenAvatar(user.avatarUrl)
                    }),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = user.nickname,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = user.handle,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalMosaicColors.current.textTertiary,
            )
            Spacer(Modifier.height(12.dp))
            IdentityChips(user)
            user.bio?.let { bio ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            StatsRow(user)
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** A circular avatar wrapped in a surface-colored ring, so it reads against the cover. */
@Composable
private fun AvatarRing(
    userId: String,
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AvatarRingSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Avatar(
            userId = userId,
            name = name,
            size = AvatarSize,
            imageUrl = avatarUrl,
        )
    }
}

/**
 * Follow / Unfollow toggle for another user's profile. Filled while not yet following (the
 * inviting primary action), tonal once following (a calmer "you're following" affordance) —
 * both solid so the label stays legible over the cover gradient.
 */
@Composable
private fun FollowButton(
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = onToggleFollow.rememberDebounced()

    if (isFollowing) {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Text(stringResource(R.string.profile_following))
        }
    } else {
        Button(onClick = onClick, modifier = modifier) {
            Text(stringResource(R.string.profile_follow))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityChips(user: User) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        user.age?.let { age -> ProfileChip(text = age.toString()) }
        user.gender?.let { gender -> ProfileChip(text = gender) }
        user.location?.let { location ->
            ProfileChip(text = location, leadingIcon = R.drawable.ic_place)
        }
    }
}

@Composable
private fun ProfileChip(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(100.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = LocalMosaicColors.current.textTertiary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatsRow(user: User, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Stat(value = user.followerCount, label = stringResource(R.string.profile_stat_followers))
        Stat(value = user.followingCount, label = stringResource(R.string.profile_stat_following))
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = compactCount(value).asText(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMosaicColors.current.textTertiary,
        )
    }
}

// endregion

// region Tabs

/**
 * The profile's tab entries, in order. The row is generated by iterating [entries], so adding a
 * tab is an enum addition plus its branch in [ProfileContent]. [ownerOnly] marks a tab that only
 * belongs on the signed-in user's own profile — Saved holds what *they* bookmarked, which is
 * private, so it is filtered out of the row on anyone else's profile (and the server refuses the
 * list to a caller who isn't its owner regardless).
 */
private enum class ProfileTab(
    @get:StringRes val labelRes: Int,
    val ownerOnly: Boolean = false,
) {
    POSTS(R.string.profile_tab_posts),
    SAVED(R.string.profile_tab_saved, true),
}

@Composable
private fun ProfileTabs(
    tabs: List<ProfileTab>,
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    topInset: Dp,
) {
    // The surface-colored reserved strip grows as the tabs reach the app bar, so they pin just
    // below it and sit seamlessly under the then-filled bar (see tabInset in ProfileContent).
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        Spacer(Modifier.height(topInset))
        PrimaryTabRow(
            // The index into the *visible* tabs, which an ownerOnly entry makes narrower than
            // the enum — so this is not the selected tab's ordinal.
            selectedTabIndex = tabs.indexOf(selected),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = LocalMosaicColors.current.textTertiary,
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
            }
        }
    }
}

// endregion

// region Tab content

private fun LazyListScope.postItems(
    screenData: ProfileScreenData,
    actions: ProfileActions,
    isCurrentUser: Boolean,
) {
    val posts = screenData.posts
    val author = screenData.user

    if (posts.isEmpty()) {
        item(key = "posts-empty") { EmptyTab(R.string.profile_empty_posts) }
        return
    }
    items(posts, key = { it.id }) { post ->
        PostCard(
            // Every post here is by the profile's user, so "own post" is simply whose profile
            // this is — no per-post comparison needed.
            data = PostCardData(post, author, isOwn = isCurrentUser),
            onToggleLike = { actions.onToggleLike(post.id) },
            onToggleBookmark = { actions.onToggleBookmark(post.id) },
            // Already on this author's profile — tapping the header again is a no-op.
            onOpenProfile = {},
            onOpenVideo = actions::openVideo,
            onOpenAlbum = actions::openAlbum,
            onOpenPost = { actions.openPost(post.id) },
            onDelete = if (isCurrentUser) ({ actions.onDeletePost(post.id) }) else null,
        )
    }
    if (!screenData.postsEndReached) {
        item(key = "posts-loading-more") { LoadingMoreFooter() }
    }
}

/**
 * The Saved tab. A null [bookmarks] is the list before its first fetch lands — the tab loads on
 * demand, so unlike the posts above it has a loading state of its own.
 */
private fun LazyListScope.bookmarkItems(
    bookmarks: ProfileBookmarks?,
    actions: ProfileActions,
) {
    if (bookmarks == null) {
        item(key = "saved-loading") { LoadingMoreFooter() }
        return
    }

    if (bookmarks.posts.isEmpty()) {
        item(key = "saved-empty") { EmptyTab(R.string.profile_empty_saved) }
        return
    }

    items(bookmarks.posts, key = { it.post.id }) { data ->
        PostCard(
            data = data,
            onToggleLike = { actions.onToggleLike(data.post.id) },
            onToggleBookmark = { actions.onToggleBookmark(data.post.id) },
            // A saved post can be by anyone, so its header opens that author's profile —
            // the one place this screen pushes another profile over itself.
            onOpenProfile = { actions.openProfile(data.author.id) },
            onOpenVideo = actions::openVideo,
            onOpenAlbum = actions::openAlbum,
            onOpenPost = { actions.openPost(data.post.id) },
            onDelete = if (data.isOwn) ({ actions.onDeletePost(data.post.id) }) else null,
        )
    }

    if (!bookmarks.endReached) {
        item(key = "saved-loading-more") { LoadingMoreFooter() }
    }
}

@Composable
private fun EmptyTab(@StringRes messageRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = LocalMosaicColors.current.textTertiary,
        )
    }
}

// endregion

// region Shared

/** A plain back button for the loading / not-found states, which have no cover to scrim over. */
@Composable
private fun PlainBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack.rememberDebounced(),
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 4.dp, top = 4.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.navigate_back),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * A transparent app bar over the cover that fills to the surface color on scroll (via the
 * Material container/scrolledContainer colors). Its buttons carry a gray circular scrim with a
 * white icon over the cover, and fade that to a bare on-surface icon once the bar fills — both
 * driven by the same scroll [progress], so bar and buttons move in lockstep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    userName: String,
    onBack: (() -> Unit)?,
) {
    val scrolled by remember {
        derivedStateOf { scrollBehavior.state.overlappedFraction > 0.01f }
    }
    val progress by animateFloatAsState(if (scrolled) 1f else 0f, label = "appBarProgress")

    TopAppBar(
        modifier = Modifier.shadow(elevation = 4.dp * progress),
        title = {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = progress),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                ScrimIconButton(
                    iconRes = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.navigate_back),
                    progress = progress,
                    onClick = onBack,
                )
            }
        },
        actions = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun CenteredMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}


// endregion

// region Previews

private fun sampleProfileData(): ProfileScreenData {
    val user = SampleUsers.first()
    val posts = SamplePosts.filter { it.authorId == user.id }

    return ProfileScreenData(
        user = user,
        profile = Profile(userId = user.id, postsCount = posts.size),
        posts = posts,
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    MosaicTheme {
        ProfileScreen(
            uiState = ProfileUiState.Loaded(sampleProfileData(), isCurrentUser = true),
            isRefreshing = false,
            onRefresh = {},
            onRetry = {},
            actions = createActionsProxy(),
        )
    }
}

@Preview(showBackground = true, name = "Another user (Follow)")
@Composable
private fun ProfileScreenOtherUserPreview() {
    MosaicTheme {
        ProfileScreen(
            uiState = ProfileUiState.Loaded(sampleProfileData(), isCurrentUser = false),
            isRefreshing = false,
            onRefresh = {},
            onRetry = {},
            actions = createActionsProxy(),
        )
    }
}

// endregion
