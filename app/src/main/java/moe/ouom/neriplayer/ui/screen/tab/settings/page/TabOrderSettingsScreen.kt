package moe.ouom.neriplayer.ui.screen.tab.settings.page

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.screen.tab.settings.page/TabOrderSettingsScreen
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.platform.youtube.YouTubeFeatureGate
import moe.ouom.neriplayer.data.settings.SettingsRepository
import moe.ouom.neriplayer.ui.screen.tab.LibraryTab
import moe.ouom.neriplayer.ui.screen.tab.exploreSearchSourceDisplayOrder
import moe.ouom.neriplayer.ui.screen.tab.formatExploreTabOrder
import moe.ouom.neriplayer.ui.screen.tab.formatLibraryTabOrder
import moe.ouom.neriplayer.ui.screen.tab.libraryTabDisplayOrder
import moe.ouom.neriplayer.ui.screen.tab.parseExploreTabOrder
import moe.ouom.neriplayer.ui.screen.tab.parseLibraryTabOrder
import moe.ouom.neriplayer.ui.screen.tab.tabId
import moe.ouom.neriplayer.ui.viewmodel.tab.SearchSource

/**
 * 「设置 → 个性化 → 标签页排序」子页面。
 *
 * 分两块：探索页标签与媒体库标签。支持拖拽排序和开关隐藏；
 * 媒体库的「本地」固定第一位且不可隐藏。
 */
@Composable
internal fun TabOrderSettingsScreen(
    onBack: () -> Unit,
    settingsRepo: SettingsRepository = AppContainer.settingsRepo
) {
    val exploreRaw by settingsRepo.exploreTabOrderFlow.collectAsState(initial = "")
    val libraryRaw by settingsRepo.libraryTabOrderFlow.collectAsState(initial = "")
    val isInternational by settingsRepo.internationalizationEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val youtubeEnabled by settingsRepo.youtubeEnabledFlow
        .collectAsStateWithLifecycle(initialValue = YouTubeFeatureGate.isEnabled())
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val topAppBarState = rememberTopAppBarState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keepOneTabText = stringResource(R.string.tab_order_keep_one_visible)

    val allowedExplore = remember(isInternational, youtubeEnabled) {
        exploreSearchSourceDisplayOrder(isInternational, youtubeEnabled)
    }
    val allowedLibrary = remember(isInternational, youtubeEnabled) {
        libraryTabDisplayOrder(isInternational, youtubeEnabled)
    }
    val storedExplore = remember(exploreRaw, allowedExplore) {
        parseExploreTabOrder(exploreRaw).filter { it in allowedExplore }
    }
    val storedLibrary = remember(libraryRaw, allowedLibrary) {
        listOf(LibraryTab.LOCAL) +
            parseLibraryTabOrder(libraryRaw)
                .filter { it != LibraryTab.LOCAL && it in allowedLibrary }
    }

    fun persistExplore(order: List<SearchSource>) {
        scope.launch {
            settingsRepo.setExploreTabOrder(
                if (order == allowedExplore) "" else formatExploreTabOrder(order)
            )
        }
    }

    fun persistLibrary(order: List<LibraryTab>) {
        scope.launch {
            settingsRepo.setLibraryTabOrder(
                if (order == allowedLibrary) "" else formatLibraryTabOrder(order)
            )
        }
    }

    fun toggleExplore(source: SearchSource, enabled: Boolean) {
        val current = storedExplore.toMutableList()
        if (enabled) {
            if (source in current) return
            current.add(source)
        } else {
            if (current.size <= 1) {
                scope.launch { snackbarHostState.showSnackbar(keepOneTabText) }
                return
            }
            current.remove(source)
        }
        persistExplore(current)
    }

    fun toggleLibrary(tab: LibraryTab, enabled: Boolean) {
        if (tab == LibraryTab.LOCAL) return
        val current = storedLibrary.filter { it != LibraryTab.LOCAL }.toMutableList()
        if (enabled) {
            if (tab in current) return
            current.add(tab)
        } else {
            current.remove(tab)
        }
        persistLibrary(listOf(LibraryTab.LOCAL) + current)
    }

    val exploreItems = remember(storedExplore, allowedExplore) {
        storedExplore.map { source ->
            TabOrderItem(
                key = "explore:${source.tabId}",
                labelRes = exploreTabLabelRes(source),
                descRes = if (source == SearchSource.LINK_RECOGNITION) {
                    R.string.tab_order_link_desc
                } else {
                    null
                },
                value = source,
                visible = true
            )
        } + allowedExplore.filter { it !in storedExplore }.map { source ->
            TabOrderItem(
                key = "explore:${source.tabId}",
                labelRes = exploreTabLabelRes(source),
                descRes = if (source == SearchSource.LINK_RECOGNITION) {
                    R.string.tab_order_link_desc
                } else {
                    null
                },
                value = source,
                visible = false
            )
        }
    }
    val libraryItems = remember(storedLibrary, allowedLibrary) {
        storedLibrary.map { tab ->
            TabOrderItem(
                key = "library:${tab.tabId}",
                labelRes = libraryTabLabelRes(tab),
                descRes = if (tab == LibraryTab.LOCAL) {
                    R.string.tab_order_local_pinned
                } else {
                    null
                },
                value = tab,
                visible = true,
                pinned = tab == LibraryTab.LOCAL
            )
        } + allowedLibrary.filter { it !in storedLibrary }.map { tab ->
            TabOrderItem(
                key = "library:${tab.tabId}",
                labelRes = libraryTabLabelRes(tab),
                descRes = null,
                value = tab,
                visible = false
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MiuixSettingsDetailScaffold(
            title = stringResource(R.string.tab_order_title),
            onBack = onBack,
            listState = listState,
            topAppBarState = topAppBarState
        ) {
            item(key = "explore_card") {
                TabOrderSectionCard(
                    title = stringResource(R.string.tab_order_explore_section),
                    description = stringResource(R.string.tab_order_explore_section_desc),
                    items = exploreItems,
                    onPersistOrder = ::persistExplore,
                    onToggle = ::toggleExplore,
                    onReset = { persistExplore(allowedExplore) }
                )
            }
            item(key = "library_card") {
                TabOrderSectionCard(
                    title = stringResource(R.string.tab_order_library_section),
                    description = stringResource(R.string.tab_order_explore_section_desc),
                    items = libraryItems,
                    onPersistOrder = { order ->
                        persistLibrary(
                            listOf(LibraryTab.LOCAL) + order.filter { it != LibraryTab.LOCAL }
                        )
                    },
                    onToggle = ::toggleLibrary,
                    onReset = { persistLibrary(allowedLibrary) }
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
    }
}

private fun exploreTabLabelRes(source: SearchSource): Int = when (source) {
    SearchSource.YOUTUBE_MUSIC -> R.string.explore_tab_youtube
    SearchSource.NETEASE -> R.string.platform_netease_short
    SearchSource.BILIBILI -> R.string.platform_bilibili
    SearchSource.KUGOU -> R.string.explore_tab_kugou
    SearchSource.QQ_MUSIC -> R.string.explore_tab_qqmusic
    SearchSource.LINK_RECOGNITION -> R.string.explore_tab_links
}

private fun libraryTabLabelRes(tab: LibraryTab): Int = when (tab) {
    LibraryTab.LOCAL -> R.string.library_tab_local
    LibraryTab.FAVORITE -> R.string.library_tab_favorite
    LibraryTab.YTMUSIC -> R.string.library_tab_youtube_music
    LibraryTab.NETEASE, LibraryTab.NETEASEALBUM -> R.string.library_tab_netease
    LibraryTab.BILI -> R.string.library_tab_bilibili
    LibraryTab.QQMUSIC -> R.string.library_tab_qqmusic
    LibraryTab.KUGOU -> R.string.library_tab_kugou
}
