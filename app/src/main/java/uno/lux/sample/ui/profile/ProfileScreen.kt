package uno.lux.sample.ui.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import uno.lux.sample.ui.theme.SampleTheme
import uno.lux.sample.util.compactCount
import uno.lux.sample.util.formatVideoDuration

/**
 * Stateful entry point: binds a [ProfileViewModel] for [userId] and forwards state and intent
 * to the stateless overload below. [onBack] is null when the profile is shown as a root tab
 * (no up-affordance); non-null when it was pushed over the feed.
 */
@Composable
fun ProfileScreen(
    userId: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    // Key the ViewModel by userId. Without a key, `viewModel()` caches one instance per class
    // in the (Activity-scoped) store and reuses it for every profile — so the factory's userId
    // would only ever take effect for the first user opened. The key gives each user their own.
    viewModel: ProfileViewModel = viewModel(
        key = "profile:$userId",
        factory = ProfileViewModel.provideFactory(userId),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onToggleLike = viewModel::onToggleLike,
        onToggleBookmark = viewModel::onToggleBookmark,
        modifier = modifier,
    )
}

/**
 * Stateless profile screen — renders [uiState] and reports interactions through the callbacks.
 * Holding no ViewModel makes it directly previewable and testable.
 */
@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onOpenSettings: () -> Unit,
    onToggleLike: (postId: String) -> Unit,
    onToggleBookmark: (postId: String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (uiState) {
            ProfileUiState.Loading -> CenteredProgress()
            ProfileUiState.NotFound -> CenteredMessage(stringResource(R.string.profile_not_found))
            is ProfileUiState.Loaded -> ProfileContent(
                profile = uiState.profile,
                onOpenSettings = onOpenSettings,
                onToggleLike = onToggleLike,
                onToggleBookmark = onToggleBookmark,
            )
        }
        if (onBack != null) {
            FloatingBackButton(
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileContent(
    profile: Profile,
    onOpenSettings: () -> Unit,
    onToggleLike: (postId: String) -> Unit,
    onToggleBookmark: (postId: String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ProfileTab.POSTS) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            ProfileHeader(user = profile.user, onOpenSettings = onOpenSettings)
        }
        stickyHeader(key = "tabs") {
            ProfileTabs(
                selected = selectedTab,
                profile = profile,
                onSelect = { selectedTab = it },
            )
        }
        when (selectedTab) {
            ProfileTab.POSTS -> postsTab(
                profile = profile,
                onToggleLike = onToggleLike,
                onToggleBookmark = onToggleBookmark,
            )

            ProfileTab.ALBUMS -> albumsTab(profile.albums)
            ProfileTab.VIDEOS -> videosTab(profile.videos)
        }
        item(key = "bottom-inset") {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// region Header

private val CoverHeight = 150.dp
private val AvatarRingSize = 96.dp
private val AvatarSize = 88.dp
private val AvatarOverlap = 44.dp // how far the avatar hangs below the cover

@Composable
private fun ProfileHeader(
    user: User,
    onOpenSettings: () -> Unit,
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
            // Edit profile + settings, bottom-right across from the avatar.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = { /* Edit is a later iteration. */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_edit))
                }
                FilledTonalIconButton(onClick = onOpenSettings) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.nav_settings),
                        modifier = Modifier.size(20.dp),
                    )
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
    val count: (Profile) -> Int,
) {
    POSTS(R.string.profile_tab_posts, Profile::postCount),
    ALBUMS(R.string.profile_tab_albums, Profile::albumCount),
    VIDEOS(R.string.profile_tab_videos, Profile::videoCount),
}

@Composable
private fun ProfileTabs(
    selected: ProfileTab,
    profile: Profile,
    onSelect: (ProfileTab) -> Unit,
) {
    // Surface fills behind the status-bar inset so, once pinned to the top, the bar icons keep
    // a solid backdrop. The inset reads as plain spacing while the tabs sit mid-screen.
    PrimaryTabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.statusBarsPadding(),
    ) {
        ProfileTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = LocalMosaicColors.current.textTertiary,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = compactCount(tab.count(profile)).asText(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

// endregion

// region Tab content

private fun androidx.compose.foundation.lazy.LazyListScope.postsTab(
    profile: Profile,
    onToggleLike: (postId: String) -> Unit,
    onToggleBookmark: (postId: String) -> Unit,
) {
    if (profile.posts.isEmpty()) {
        item(key = "posts-empty") { EmptyTab(stringResource(R.string.profile_empty_posts)) }
        return
    }
    items(profile.posts, key = { it.id }) { post ->
        PostCard(
            post = post,
            onToggleLike = { onToggleLike(post.id) },
            onToggleBookmark = { onToggleBookmark(post.id) },
            // Already on this author's profile — tapping the header again is a no-op.
            onOpenProfile = {},
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.albumsTab(albums: List<Album>) {
    if (albums.isEmpty()) {
        item(key = "albums-empty") { EmptyTab(stringResource(R.string.profile_empty_albums)) }
        return
    }
    gridRows(albums, key = { it.id }) { album -> AlbumCell(album, Modifier.weight(1f)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.videosTab(videos: List<Video>) {
    if (videos.isEmpty()) {
        item(key = "videos-empty") { EmptyTab(stringResource(R.string.profile_empty_videos)) }
        return
    }
    gridRows(videos, key = { it.id }) { video -> VideoCell(video, Modifier.weight(1f)) }
}

/**
 * Lays a list into a 2-column grid by emitting one LazyColumn item per row of two. We grid
 * inside the LazyColumn rather than swapping in a LazyVerticalGrid so the cover, identity and
 * the **sticky** tab header share one scroll container with the grid — `LazyVerticalGrid` has
 * no sticky-header support.
 */
private fun <T> androidx.compose.foundation.lazy.LazyListScope.gridRows(
    items: List<T>,
    key: (T) -> Any,
    cell: @Composable androidx.compose.foundation.layout.RowScope.(T) -> Unit,
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
            text = pluralStringResource(R.plurals.profile_album_photos, album.itemCount, album.itemCount),
            style = MaterialTheme.typography.bodySmall,
            color = LocalMosaicColors.current.textTertiary,
        )
    }
}

@Composable
private fun VideoCell(video: Video, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MosaicGradients.mediaBrush(video.id)),
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
            text = stringResource(R.string.profile_video_views, compactCount(video.viewCount).asText()),
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

@Composable
private fun FloatingBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 8.dp, top = 8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.32f)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.navigate_back),
                tint = Color.White,
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
    return Profile(
        user = user,
        posts = SamplePosts.filter { it.author.id == user.id },
        albums = SampleAlbums.getValue(user.id),
        videos = SampleVideos.getValue(user.id),
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    SampleTheme {
        ProfileScreen(
            uiState = ProfileUiState.Loaded(sampleProfile()),
            onOpenSettings = {},
            onToggleLike = {},
            onToggleBookmark = {},
            onBack = {},
        )
    }
}

// endregion
