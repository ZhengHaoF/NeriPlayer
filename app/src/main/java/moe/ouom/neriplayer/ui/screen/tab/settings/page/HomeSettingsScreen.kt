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
 * File: moe.ouom.neriplayer.ui.screen.tab.settings.page/HomeSettingsScreen
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.settings.SettingsRepository
import moe.ouom.neriplayer.ui.viewmodel.tab.DEFAULT_HOME_CONTENT_SOURCE_ORDER
import moe.ouom.neriplayer.ui.viewmodel.tab.DEFAULT_HOME_CONTENT_SOURCE_ORDER_RAW
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeContentSource
import moe.ouom.neriplayer.ui.viewmodel.tab.formatHomeContentSourceOrder
import moe.ouom.neriplayer.ui.viewmodel.tab.parseHomeContentSourceOrder

/**
 * 「设置 → 个性化 → 首页展示排序」子页面。
 *
 * 按顺序展示首页内容源；每个源可开关、可拖拽排序。首页会按此顺序尝试加载，
 * 当前源所有展示接口均无数据时自动降级到下一个源。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSettingsScreen(
    onBack: () -> Unit,
    settingsRepo: SettingsRepository = AppContainer.settingsRepo
) {
    val rawOrder by settingsRepo.homeContentSourceOrderFlow
        .collectAsState(initial = DEFAULT_HOME_CONTENT_SOURCE_ORDER_RAW)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val topAppBarState = rememberTopAppBarState()
    val order = remember(rawOrder) { parseHomeContentSourceOrder(rawOrder) }
    val snackbarHostState = remember { SnackbarHostState() }
    val keepOneSourceText = stringResource(R.string.home_settings_keep_one_source)

    fun persist(newOrder: List<HomeContentSource>) {
        scope.launch {
            settingsRepo.setHomeContentSourceOrder(formatHomeContentSourceOrder(newOrder))
        }
    }

    fun toggle(source: HomeContentSource, enabled: Boolean) {
        val current = order.toMutableList()
        if (enabled) {
            if (source in current) return
            current.add(source)
        } else {
            if (current.size <= 1) {
                scope.launch { snackbarHostState.showSnackbar(keepOneSourceText) }
                return
            }
            current.remove(source)
        }
        persist(current)
    }

    val sourceItems = remember(order) {
        order.map { source ->
            TabOrderItem(
                key = "home:${source.id}",
                labelRes = homeSourceLabelRes(source),
                descRes = homeSourceDescRes(source),
                value = source,
                visible = true
            )
        } + HomeContentSource.entries.filter { it !in order }.map { source ->
            TabOrderItem(
                key = "home:${source.id}",
                labelRes = homeSourceLabelRes(source),
                descRes = homeSourceDescRes(source),
                value = source,
                visible = false
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MiuixSettingsDetailScaffold(
            title = stringResource(R.string.home_settings_title),
            onBack = onBack,
            listState = listState,
            topAppBarState = topAppBarState
        ) {
            item(key = "source_order") {
                TabOrderSectionCard(
                    title = stringResource(R.string.home_settings_source_order_section),
                    description = stringResource(R.string.home_settings_source_order_section_desc),
                    items = sourceItems,
                    onPersistOrder = ::persist,
                    onToggle = ::toggle,
                    onReset = { persist(DEFAULT_HOME_CONTENT_SOURCE_ORDER) }
                )
            }
            item(key = "notice") {
                Text(
                    text = stringResource(R.string.home_settings_youtube_notice),
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun homeSourceLabelRes(source: HomeContentSource): Int = when (source) {
    HomeContentSource.NETEASE -> R.string.home_settings_source_netease
    HomeContentSource.KUGOU -> R.string.home_settings_source_kugou
}

private fun homeSourceDescRes(source: HomeContentSource): Int = when (source) {
    HomeContentSource.NETEASE -> R.string.home_settings_source_netease_desc
    HomeContentSource.KUGOU -> R.string.home_settings_source_kugou_desc
}
