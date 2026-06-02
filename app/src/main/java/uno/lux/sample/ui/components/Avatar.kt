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
    val palette = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val (background: Color, foreground: Color) = palette[id.hashCode().floorMod(palette.size)]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.initials(),
            color = foreground,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

/** Non-negative modulo, so a negative [hashCode] still maps onto the palette. */
private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

/** Up to two uppercase initials drawn from the first words of a display name. */
private fun String.initials(): String =
    trim().split(' ')
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString(separator = "")
