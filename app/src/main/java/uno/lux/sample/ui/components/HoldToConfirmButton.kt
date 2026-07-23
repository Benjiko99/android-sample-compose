package uno.lux.sample.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import uno.lux.sample.R
import uno.lux.sample.ui.theme.MosaicTheme

/** What the button is doing right now — both the label and the fill are read off it. */
internal enum class HoldPhase { IDLE, HOLDING, HINT }

/**
 * The press-and-hold gesture as a plain state machine, so the whole sequence — press, hold to
 * confirm, release early to hint, hint back to idle — is unit-testable without a frame clock or a
 * composition. The composable owns the animation; this owns *what phase the button is in* and
 * *when the action fires*.
 *
 * [press] deliberately outlives the finger: the hint keeps running after the release, which is why
 * a press is numbered and only the newest one may write [phase]. A user who taps, reads the hint
 * and immediately presses again would otherwise have the spent press's expiring hint yank the
 * button out of the new hold.
 */
@Stable
internal class HoldToConfirmState(
    private val holdMillis: Long,
    private val hintMillis: Long,
) {
    var phase: HoldPhase by mutableStateOf(HoldPhase.IDLE)
        private set

    private var presses = 0

    /**
     * Runs one press to its end. [awaitRelease] returns when the finger lifts; failing to return
     * within [holdMillis] *is* the confirmation, so [onConfirm] fires under the still-held finger
     * rather than on its release.
     */
    suspend fun press(awaitRelease: suspend () -> Unit, onConfirm: () -> Unit) {
        val press = ++presses
        moveTo(press, HoldPhase.HOLDING)

        try {
            val heldLongEnough = withTimeoutOrNull(holdMillis) { awaitRelease() } == null

            if (heldLongEnough) {
                onConfirm()
            } else {
                moveTo(press, HoldPhase.HINT)
                delay(hintMillis)
            }
        } finally {
            // Also the cancellation path: a gesture torn down mid-hold must not leave a stuck fill.
            moveTo(press, HoldPhase.IDLE)
        }
    }

    /** Writes [next] only while [press] is still the live one, leaving a superseded press mute. */
    private fun moveTo(press: Int, next: HoldPhase) {
        if (press == presses) phase = next
    }
}

/** Tuning for [HoldToConfirmButton], exposed so a caller can lengthen a weightier confirmation. */
object HoldToConfirmDefaults {
    /** How long the finger must stay down before the action fires. */
    const val HoldMillis = 1_500L

    /** How long the "press & hold" nudge replaces the label after too short a press. */
    const val HintMillis = 1_500L

    val MinHeight = 52.dp
}

/**
 * A filled button that commits only after the user has held it for [holdMillis] — the guard rail
 * for an action worth a moment's thought, like publishing a post. The fill sweeps left to right as
 * the hold progresses, so the remaining time is legible rather than a wait in the dark; letting go
 * early drains it and swaps the label for a nudge to press *and hold* before restoring [text].
 *
 * Holding is a motor-skill barrier, so an accessibility service activating the button confirms
 * immediately through the semantics [onClick] instead of being asked to sustain a gesture.
 */
@Composable
fun HoldToConfirmButton(
    text: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isBusy: Boolean = false,
    hintText: String = stringResource(R.string.hold_to_confirm_hint),
    holdMillis: Long = HoldToConfirmDefaults.HoldMillis,
    hintMillis: Long = HoldToConfirmDefaults.HintMillis,
) {
    val haptics = LocalHapticFeedback.current
    val colors = ButtonDefaults.buttonColors()
    val shape = ButtonDefaults.shape
    val active = enabled && !isBusy
    val containerColor = if (active) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (active) colors.contentColor else colors.disabledContentColor

    val state = remember(holdMillis, hintMillis) { HoldToConfirmState(holdMillis, hintMillis) }
    val progress = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    // Kept current so the gesture isn't torn down and rebuilt every time the caller recomposes.
    val currentConfirm by rememberUpdatedState(onConfirm)
    val confirm = remember {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            currentConfirm()
        }
    }

    LaunchedEffect(state.phase) {
        val holding = state.phase == HoldPhase.HOLDING

        launch { scale.animateTo(if (holding) PressedScale else 1f, tween(PressResponseMillis)) }

        if (holding) {
            // Matching the hold means the sweep lands exactly as the action fires.
            progress.animateTo(1f, tween(holdMillis.toInt(), easing = LinearEasing))
        } else {
            progress.animateTo(0f, tween(DrainMillis, easing = FastOutSlowInEasing))
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            // Both animations are read in the draw/layer phase, so a frame of the sweep costs no
            // recomposition — only the phase changes below recompose the label.
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = HoldToConfirmDefaults.MinHeight,
            )
            .clip(shape)
            .background(containerColor)
            .drawBehind {
                val fraction = progress.value
                if (fraction <= 0f) return@drawBehind

                val width = size.width * fraction

                drawRect(
                    color = contentColor.copy(alpha = FillAlpha),
                    size = Size(width = width, height = size.height),
                )

                // A bright leading edge, so the sweep reads as a moving front rather than a stain.
                drawRect(
                    color = contentColor.copy(alpha = LeadingEdgeAlpha),
                    topLeft = Offset(x = width - LeadingEdgeWidth.toPx(), y = 0f),
                    size = Size(width = LeadingEdgeWidth.toPx(), height = size.height),
                )
            }
            .pointerInput(active) {
                if (!active) return@pointerInput

                detectTapGestures(
                    onPress = { state.press(awaitRelease = { tryAwaitRelease() }, onConfirm = confirm) }
                )
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!active) disabled()
                onClick(label = text) {
                    if (active) currentConfirm()
                    active
                }
            }
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                strokeWidth = 2.5.dp,
                color = contentColor,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Crossfade(
                targetState = if (state.phase == HoldPhase.HINT) hintText else text,
                label = "hold-to-confirm-label",
            ) { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** How far the button sinks under the finger, the usual press feedback a filled button gives. */
private const val PressedScale = 0.97f
private const val PressResponseMillis = 120

/** Alphas over the container, so the sweep tracks the theme instead of pinning its own colour. */
private const val FillAlpha = 0.24f
private const val LeadingEdgeAlpha = 0.85f

private val LeadingEdgeWidth = 2.dp

/** Time the fill takes to drain once the finger lifts or the action fires. */
private const val DrainMillis = 220

@Preview(showBackground = true)
@Composable
private fun HoldToConfirmButtonPreview() {
    MosaicTheme {
        HoldToConfirmButton(
            text = "Publish",
            onConfirm = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Preview(name = "Busy", showBackground = true)
@Composable
private fun HoldToConfirmButtonBusyPreview() {
    MosaicTheme {
        HoldToConfirmButton(
            text = "Publish",
            onConfirm = {},
            isBusy = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}
