package uno.lux.sample.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import uno.lux.sample.R

/** Top-app-bar action that opens the settings screen. */
@Composable
fun SettingsAction(onClick: () -> Unit) {
    IconButton(onClick = onClick.rememberDebounced()) {
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = stringResource(R.string.nav_settings),
        )
    }
}
