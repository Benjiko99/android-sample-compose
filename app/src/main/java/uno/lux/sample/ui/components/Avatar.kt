package uno.lux.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uno.lux.sample.util.initials

/**
 * A circular monogram avatar. Colour is derived deterministically from [id] so a given
 * user always renders the same way, while avoiding any image-loading dependency.
 */
@Composable
fun Avatar(
    name: String,
    id: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (background: Color, foreground: Color) = when (id.hashCode().mod(3)) {
        0 -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        1 -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        else -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(name),
            color = foreground,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
