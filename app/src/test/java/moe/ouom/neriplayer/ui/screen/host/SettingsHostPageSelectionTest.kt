package moe.ouom.neriplayer.ui.screen.host

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsHostPageSelectionTest {
    @Test
    fun selectsThePageForEachNavigationState() {
        assertEquals(
            "settings",
            selectSettingsHostPage(
                SettingsScreenState.Settings,
                "settings", "manager", "progress", "home", "tabOrder"
            )
        )
        assertEquals(
            "manager",
            selectSettingsHostPage(
                SettingsScreenState.DownloadManager,
                "settings", "manager", "progress", "home", "tabOrder"
            )
        )
        assertEquals(
            "progress",
            selectSettingsHostPage(
                SettingsScreenState.DownloadProgress,
                "settings", "manager", "progress", "home", "tabOrder"
            )
        )
        assertEquals(
            "home",
            selectSettingsHostPage(
                SettingsScreenState.HomeSettings,
                "settings", "manager", "progress", "home", "tabOrder"
            )
        )
        assertEquals(
            "tabOrder",
            selectSettingsHostPage(
                SettingsScreenState.TabOrderSettings,
                "settings", "manager", "progress", "home", "tabOrder"
            )
        )
    }
}
