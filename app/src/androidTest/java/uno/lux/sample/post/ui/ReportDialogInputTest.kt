package uno.lux.sample.post.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uno.lux.sample.R
import uno.lux.sample.app.theme.MosaicTheme
import uno.lux.sample.common.data.ReportReason

/**
 * The details field is capped at what the server accepts.
 *
 * A report is acknowledged the moment Send is tapped — it travels fire-and-forget, so a 422 for
 * over-long details would be swallowed behind a "thank you". The cap is what keeps that honest,
 * and it belongs in an instrumented test because it is enforced by the text field itself rather
 * than by anything a JVM test could call.
 */
@RunWith(AndroidJUnit4::class)
class ReportDialogInputTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun capsTheDetailsAtWhatTheServerAccepts() {
        var submitted: String? = null
        composeRule.setContent {
            MosaicTheme {
                ReportPostDialog(onDismiss = {}, onSubmit = { _, details -> submitted = details })
            }
        }

        composeRule.onNodeWithText(string(R.string.report_reason_spam)).performClick()
        composeRule
            .onNodeWithText(string(R.string.report_details_hint))
            .performTextInput("x".repeat(REPORT_DETAILS_MAX_LENGTH + 50))
        composeRule.onNodeWithText(string(R.string.report_send)).performClick()

        assertEquals(REPORT_DETAILS_MAX_LENGTH, submitted?.length)
    }

    @Test
    fun handsTheChosenReasonAndTrimmedDetailsToTheHost() {
        var reported: Pair<ReportReason, String>? = null
        composeRule.setContent {
            MosaicTheme {
                ReportPostDialog(
                    onDismiss = {},
                    onSubmit = { reason, details -> reported = reason to details },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.report_reason_hate_speech)).performClick()
        composeRule
            .onNodeWithText(string(R.string.report_details_hint))
            .performTextInput("  Read the third paragraph  ")
        composeRule.onNodeWithText(string(R.string.report_send)).performClick()

        assertEquals(ReportReason.HATE_SPEECH to "Read the third paragraph", reported)
    }
}
