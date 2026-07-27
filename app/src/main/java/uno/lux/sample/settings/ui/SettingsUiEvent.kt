package uno.lux.sample.settings.ui

import uno.lux.sample.app.framework.UiEvent
import uno.lux.sample.settings.data.domain.AppLanguage
import uno.lux.sample.settings.data.domain.ThemeMode

sealed interface SettingsUiEvent : UiEvent {
    data class SetThemeMode(
        val mode: ThemeMode,
    ) : SettingsUiEvent

    data class SetAutoPlayVideos(
        val enabled: Boolean,
    ) : SettingsUiEvent

    data class SetLanguage(
        val language: AppLanguage,
    ) : SettingsUiEvent

    data object GoBack : SettingsUiEvent
}
