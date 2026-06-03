package uno.lux.sample.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uno.lux.sample.R
import uno.lux.sample.data.ThemeMode
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.util.ActionsInvocationHandler

/**
 * The actions the settings screen can raise — change the theme, or navigate back. Bundled into
 * one [Stable] interface so the screen takes a single parameter and a preview can supply a no-op
 * proxy via [ActionsInvocationHandler.createActionsProxy].
 */
@Stable
interface SettingsActions {
    fun changeTheme(mode: ThemeMode)
    fun navigateBack()
}

/**
 * Stateful entry point: binds the [SettingsViewModel] and forwards state and intent to the
 * stateless overload below.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack) {
        object : SettingsActions {
            override fun changeTheme(mode: ThemeMode) = viewModel.setThemeMode(mode)
            override fun navigateBack() = onBack()
        }
    }
    SettingsScreen(
        themeMode = themeMode,
        actions = actions,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    themeMode: ThemeMode,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = actions::navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection(title = stringResource(R.string.settings_appearance)) {
                ThemeModeSelector(
                    selected = themeMode,
                    onSelected = actions::changeTheme,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
            ) {
                Text(stringResource(mode.labelRes()))
            }
        }
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MosaicTheme {
        SettingsScreen(
            themeMode = ThemeMode.SYSTEM,
            actions = ActionsInvocationHandler.createActionsProxy(),
        )
    }
}
