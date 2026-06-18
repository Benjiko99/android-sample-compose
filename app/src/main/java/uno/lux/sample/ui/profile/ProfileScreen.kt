package uno.lux.sample.ui.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uno.lux.sample.R
import uno.lux.sample.data.Album
import uno.lux.sample.data.Profile
import uno.lux.sample.data.SampleAlbums
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.SampleUsers
import uno.lux.sample.data.SampleVideos
import uno.lux.sample.data.User
import uno.lux.sample.data.Video
import uno.lux.sample.ui.components.Avatar
import uno.lux.sample.ui.components.MosaicGradients
import uno.lux.sample.ui.format.asText
import uno.lux.sample.ui.home.PostCard
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.util.compactCount
import uno.lux.sample.util.createActionsProxy
import uno.lux.sample.util.formatVideoDuration

/**
 * The profile's ViewModel-backed intents — liking / bookmarking the viewed user's posts — as one
 * [Stable] seam the stateless [ProfileScreen] depends on. [ProfileViewModel] implements it, so the
 * binder passes the ViewModel directly and a preview passes a no-op [createActionsProxy].
 * Navigation (settings, back) is the host's concern, so it stays a separate lambda.
 */
@Stable
interface ProfileActions {
    fun onToggleLike(postId: String)
    fun onToggleBookmark(postId: String)
}

/**
 * Stateful entry point: binds a [ProfileViewModel] for [userId] and forwards state and intent
 * to the stateless overload below. [onBack] is null when the profile is shown as a root tab
 * (no up-affordance); non-null when it was pushed over the feed.
 */
@Composable
fun ProfileScreen(
    userId: String,
    onOpenSettings: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    // The ViewModel store is per back-stack entry, so each opened profile page gets its own
    // ProfileViewModel, created for that entry's userId and cleared when the page pops.
    viewModel: ProfileViewModel = hiltViewModel<ProfileViewModel, ProfileViewModel.Factory>(
        creationCallback = { factory -> factory.create(userId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        actions = viewModel,
        onOpenSettings = onOpenSettings,
        onOpenVideo = onOpenVideo,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Stateless profile screen — renders [uiState] and reports interactions through [actions].
 * Holding no ViewModel makes it directly previewable and testable.
 */
@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    actions: ProfileActions,
    onOpenSettings: () -> Unit,
    onOpenVideo: (Video) -> Unit,
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
                CenteredProgress()
                if (onBack != null) PlainBackButton(onBack, Modifier.align(Alignment.TopStart))
            }

            ProfileUiState.NotFound -> {
                CenteredMessage(stringResource(R.string.profile_not_found))
                if (onBack != null) PlainBackButton(onBack, Modifier.align(Alignment.TopStart))
            }

            // The loaded state owns its own scroll-reactive TopAppBar over the cover.
            is ProfileUiState.Loaded -> ProfileContent(
                profile = uiState.profile,
                isCurrentUser = uiState.isCurrentUser,
                actions = actions,
                onOpenSettings = onOpenSettings,
                onOpenVideo = onOpenVideo,
                onBack = onBack,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    profile: Profile,
    isCurrentUser: Boolean,
    actions: ProfileActions,
    onOpenSettings: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    onBack: (() -> Unit)?,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ProfileTab.POSTS) }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // The cover bleeds to the very top, so we can't reserve the bar's height with content
    // padding. Instead the sticky tab header grows a top inset as it nears the bar, derived
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "header") {
                ProfileHeader(user = profile.user, isCurrentUser = isCurrentUser)
            }
            stickyHeader(key = "tabs") {
                ProfileTabs(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    topInset = tabInset,
                )
            }
            when (selectedTab) {
                ProfileTab.POSTS -> postsTab(
                    profile = profile,
                    actions = actions,
                    onOpenVideo = onOpenVideo,
                )

                ProfileTab.ALBUMS -> albumsTab(profile.albums)
                ProfileTab.VIDEOS -> videosTab(profile.videos, onOpenVideo)
            }
            item(key = "bottom-inset") {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
        ProfileTopBar(
            scrollBehavior = scrollBehavior,
            onBack = onBack,
            onOpenSettings = onOpenSettings,
        )
    }
}

// region Header

private val CoverHeight = 150.dp
private val AvatarRingSize = 96.dp
private val AvatarSize = 88.dp
private val AvatarOverlap = 44.dp // how far the avatar hangs below the cover
private val ProfileBarHeight = 64.dp // Material small TopAppBar content height (excl. status bar)

@Composable
private fun ProfileHeader(
    user: User,
    isCurrentUser: Boolean,
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
            // Edit profile, bottom-right across from the avatar (settings live in the app bar).
            // Shown only on the signed-in user's own profile.
            if (isCurrentUser) {
                FilledTonalButton(
                    onClick = { /* Edit is a later iteration. */ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_edit))
                }
            }
            AvatarRing(
                name = user.nickname,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp)
                    .padding(top = CoverHeight - AvatarOverlap),
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
    name: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AvatarRingSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Avatar(name = name, size = AvatarSize)
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

private enum class ProfileTab(
    @get:StringRes val labelRes: Int,
) {
    POSTS(R.string.profile_tab_posts),
    ALBUMS(R.string.profile_tab_albums),
    VIDEOS(R.string.profile_tab_videos),
}

@Composable
private fun ProfileTabs(
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    topInset: Dp,
) {
    // The surface-colored reserved strip grows as the tabs reach the app bar, so they pin just
    // below it and sit seamlessly under the then-filled bar (see tabInset in ProfileContent).
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        Spacer(Modifier.height(topInset))
        PrimaryTabRow(
            selectedTabIndex = selected.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ProfileTab.entries.forEach { tab ->
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

private fun LazyListScope.postsTab(
    profile: Profile,
    actions: ProfileActions,
    onOpenVideo: (Video) -> Unit,
) {
    if (profile.posts.isEmpty()) {
        item(key = "posts-empty") { EmptyTab(stringResource(R.string.profile_empty_posts)) }
        return
    }
    items(profile.posts, key = { it.id }) { post ->
        PostCard(
            post = post,
            onToggleLike = { actions.onToggleLike(post.id) },
            onToggleBookmark = { actions.onToggleBookmark(post.id) },
            // Already on this author's profile — tapping the header again is a no-op.
            onOpenProfile = {},
            onOpenVideo = onOpenVideo,
        )
    }
}

private fun LazyListScope.albumsTab(albums: List<Album>) {
    if (albums.isEmpty()) {
        item(key = "albums-empty") { EmptyTab(stringResource(R.string.profile_empty_albums)) }
        return
    }
    gridRows(albums, key = { it.id }) { album -> AlbumCell(album, Modifier.weight(1f)) }
}

private fun LazyListScope.videosTab(
    videos: List<Video>,
    onOpenVideo: (Video) -> Unit,
) {
    if (videos.isEmpty()) {
        item(key = "videos-empty") { EmptyTab(stringResource(R.string.profile_empty_videos)) }
        return
    }
    gridRows(videos, key = { it.id }) { video ->
        VideoCell(
            video = video,
            onClick = { onOpenVideo(video) },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Lays a list into a 2-column grid by emitting one LazyColumn item per row of two. We grid
 * inside the LazyColumn rather than swapping in a LazyVerticalGrid so the cover, identity and
 * the **sticky** tab header share one scroll container with the grid — `LazyVerticalGrid` has
 * no sticky-header support.
 */
private fun <T> LazyListScope.gridRows(
    items: List<T>,
    key: (T) -> Any,
    cell: @Composable RowScope.(T) -> Unit,
) {
    val rows = items.chunked(2)
    items(rows, key = { row -> "row-${key(row.first())}" }) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { item -> cell(item) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AlbumCell(album: Album, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MosaicGradients.mediaBrush(album.id)),
        ) {
            MediaBadge(
                text = album.itemCount.toString(),
                iconRes = R.drawable.ic_layers,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(
                R.plurals.profile_album_photos,
                album.itemCount,
                album.itemCount
            ),
            style = MaterialTheme.typography.bodySmall,
            color = LocalMosaicColors.current.textTertiary,
        )
    }
}

@Composable
private fun VideoCell(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MosaicGradients.mediaBrush(video.id))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.34f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    contentDescription = stringResource(R.string.profile_play),
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            MediaBadge(
                text = formatVideoDuration(video.durationSeconds),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(
                R.string.profile_video_views,
                compactCount(video.viewCount).asText()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = LocalMosaicColors.current.textTertiary,
        )
    }
}

/** A small dark-scrim pill (white text, optional leading icon) overlaid on a thumbnail corner. */
@Composable
private fun MediaBadge(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun EmptyTab(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
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
        onClick = onBack,
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
    onBack: (() -> Unit)?,
    onOpenSettings: () -> Unit,
) {
    val scrolled by remember {
        derivedStateOf { scrollBehavior.state.overlappedFraction > 0.01f }
    }
    val progress by animateFloatAsState(if (scrolled) 1f else 0f, label = "appBarProgress")

    TopAppBar(
        title = {},
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
        actions = {
            ScrimIconButton(
                iconRes = R.drawable.ic_settings,
                contentDescription = stringResource(R.string.nav_settings),
                progress = progress,
                onClick = onOpenSettings,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/**
 * An app-bar icon button whose gray circular scrim (and white tint) fade to a bare on-surface
 * icon as [progress] goes 0 → 1 — i.e. as the bar fills on scroll.
 */
@Composable
private fun ScrimIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    progress: Float,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.32f * (1f - progress))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = lerp(Color.White, MaterialTheme.colorScheme.onSurface, progress),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
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

private fun sampleProfile(): Profile {
    val user = SampleUsers.first()
    val posts = SamplePosts.filter { it.author.id == user.id }
    val albums = SampleAlbums.getValue(user.id)
    val videos = SampleVideos.getValue(user.id)

    return Profile(
        user = user,
        posts = posts,
        albums = albums,
        videos = videos,
        postsCount = posts.size,
        albumsCount = albums.size,
        videosCount = videos.size,
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    MosaicTheme {
        ProfileScreen(
            uiState = ProfileUiState.Loaded(sampleProfile(), isCurrentUser = true),
            actions = createActionsProxy(),
            onOpenSettings = {},
            onOpenVideo = {},
            onBack = {},
        )
    }
}

// endregion
