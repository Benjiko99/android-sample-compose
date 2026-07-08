package uno.lux.sample.ui.post

import androidx.compose.foundation.background
import uno.lux.sample.ui.components.debouncedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import uno.lux.sample.R
import uno.lux.sample.data.post.Album
import uno.lux.sample.ui.components.MosaicGradients

/**
 * An album post's media: its [Album.images] laid out in a horizontally scrolling row, each photo
 * loaded with Coil. Tapping an image calls [onOpenImage] with its index (for the fullscreen
 * viewer). Each photo carries a position badge ("2 / 3") with an image icon in its top-end corner.
 * While a photo loads (or if it fails) the deterministic Mosaic gradient shows through behind it.
 */
@Composable
internal fun AlbumPostGallery(
    album: Album,
    onOpenImage: (imageIndex: Int) -> Unit,
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
                    .background(MosaicGradients.mediaBrush("${album.id}-$index"))
                    .debouncedClickable { onOpenImage(index) },
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "${index + 1}/${album.itemCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
