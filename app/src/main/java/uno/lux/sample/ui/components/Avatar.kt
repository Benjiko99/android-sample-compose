package uno.lux.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uno.lux.sample.ui.theme.Manrope
import uno.lux.sample.util.initials

// Mosaic avatar gradients — duotones keyed by the name's hash so a user always renders the same.
private val AvatarGradients = listOf(
    Color(0xFF5B6CFF) to Color(0xFF9B4DFF),
    Color(0xFFFF7A59) to Color(0xFFFF4D8D),
    Color(0xFF1FB6A6) to Color(0xFF0E8F9C),
    Color(0xFFFFB547) to Color(0xFFFF7A59),
    Color(0xFF7C5CFF) to Color(0xFF4D8DFF),
    Color(0xFFE0407A) to Color(0xFFA83B8F),
)

/**
 * A circular gradient avatar with the user's initials, keyed deterministically by [name] —
 * no image-loading dependency.
 */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    val (start, end) = AvatarGradients[name.hashCode().mod(AvatarGradients.size)]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(start, end))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(name),
            color = Color.White,
            fontFamily = Manrope,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
        )
    }
}
