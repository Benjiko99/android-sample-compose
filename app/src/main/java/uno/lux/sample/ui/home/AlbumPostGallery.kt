package uno.lux.sample.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import uno.lux.sample.data.Album
import uno.lux.sample.ui.components.MosaicGradients

/**
 * An album post's media: its [Album.images] laid out in a horizontally scrolling row, each photo
 * loaded with Coil. While a photo loads (or if it fails) the deterministic Mosaic gradient shows
 * through behind it, so the row never flashes empty. The row scrolls edge to edge with a 16dp
 * inset, matching the feed's gutter.
 */
@Composable
internal fun AlbumPostGallery(
    album: Album,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(album.images) { index, url ->
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MosaicGradients.mediaBrush("${album.id}-$index")),
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
