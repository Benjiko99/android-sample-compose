package uno.lux.sample.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import uno.lux.sample.R
import uno.lux.sample.ui.components.SettingsAction

/**
 * Stateful entry point: binds the [PlaceholderViewModel] (its sole job is opening settings
 * through the `Navigator`) and forwards to the stateless overload below.
 */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    viewModel: PlaceholderViewModel = hiltViewModel(),
) {
    PlaceholderScreen(
        titleRes = titleRes,
        onOpenSettings = viewModel::openSettings,
        modifier = modifier,
    )
}

/**
 * Temporary stand-in for destinations that aren't built yet, so the navigation shell stays
 * fully functional. Mirrors the real screens' chrome — a top app bar with a settings action.
 * Holding no ViewModel makes it directly previewable and testable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceholderScreen(
    @StringRes titleRes: Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                actions = { SettingsAction(onOpenSettings) },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.placeholder_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
