package moe.ouom.neriplayer.ui.screen.host

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsHostScreenStateTest {

    @Test
    fun nextTowards_settingsToHomeSettings_jumpsDirectly() {
        // 回归：设置 → 首页展示排序 应直达 HomeSettings，而不是误入 DownloadManager
        assertEquals(
            SettingsScreenState.HomeSettings,
            SettingsScreenState.Settings.nextTowards(SettingsScreenState.HomeSettings)
        )
    }

    @Test
    fun nextTowards_settingsToDownloadProgress_keepsLinearChain() {
        assertEquals(
            SettingsScreenState.DownloadManager,
            SettingsScreenState.Settings.nextTowards(SettingsScreenState.DownloadProgress)
        )
        assertEquals(
            SettingsScreenState.DownloadProgress,
            SettingsScreenState.DownloadManager.nextTowards(SettingsScreenState.DownloadProgress)
        )
        assertEquals(
            SettingsScreenState.DownloadProgress,
            SettingsScreenState.DownloadProgress.nextTowards(SettingsScreenState.DownloadProgress)
        )
    }

    @Test
    fun nextTowards_homeSettingsToSettings_goesBack() {
        assertEquals(
            SettingsScreenState.Settings,
            SettingsScreenState.HomeSettings.nextTowards(SettingsScreenState.Settings)
        )
    }

    @Test
    fun nextTowards_sameState_stays() {
        assertEquals(
            SettingsScreenState.Settings,
            SettingsScreenState.Settings.nextTowards(SettingsScreenState.Settings)
        )
        assertEquals(
            SettingsScreenState.HomeSettings,
            SettingsScreenState.HomeSettings.nextTowards(SettingsScreenState.HomeSettings)
        )
    }
}
