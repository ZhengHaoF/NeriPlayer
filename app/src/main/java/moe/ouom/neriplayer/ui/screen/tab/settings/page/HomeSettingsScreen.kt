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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.settings.SettingsRepository
import moe.ouom.neriplayer.ui.screen.tab.settings.component.settingsItemClickable
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsSwitch
import moe.ouom.neriplayer.ui.viewmodel.tab.DEFAULT_HOME_CONTENT_SOURCE_ORDER
import moe.ouom.neriplayer.ui.viewmodel.tab.DEFAULT_HOME_CONTENT_SOURCE_ORDER_RAW
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeContentSource
import moe.ouom.neriplayer.ui.viewmodel.tab.formatHomeContentSourceOrder
import moe.ouom.neriplayer.ui.viewmodel.tab.parseHomeContentSourceOrder

/**
 * 「设置 → 个性化 → 首页展示排序」子页面。
 *
 * 按顺序展示首页内容源；每个源可开关、可上移/下移。首页会按此顺序尝试加载，
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
        val current = parseHomeContentSourceOrder(rawOrder)
        val next = if (enabled) current + source else current - source
        if (next.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar(keepOneSourceText) }
            return
        }
        persist(next)
    }

    fun move(source: HomeContentSource, delta: Int) {
        val current = parseHomeContentSourceOrder(rawOrder).toMutableList()
        val index = current.indexOf(source)
        val target = index + delta
        if (index < 0 || target < 0 || target >= current.size) return
        current[index] = current[target].also { current[target] = current[index] }
        persist(current)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MiuixSettingsDetailScaffold(
            title = stringResource(R.string.home_settings_title),
            onBack = onBack,
            listState = listState,
            topAppBarState = topAppBarState
        ) {
            item(key = "source_order") {
                MiuixSettingsSectionCard {
                    MiuixSettingsSectionIntro(
                        title = stringResource(R.string.home_settings_source_order_section),
                        description = stringResource(R.string.home_settings_source_order_section_desc)
                    )
                    HomeContentSource.entries.forEach { source ->
                        val index = order.indexOf(source)
                        val enabled = index >= 0
                        HomeSourceRow(
                            source = source,
                            orderIndex = index,
                            enabled = enabled,
                            canMoveUp = enabled && index > 0,
                            canMoveDown = enabled && index < order.lastIndex,
                            onToggle = { toggle(source, it) },
                            onMoveUp = { move(source, -1) },
                            onMoveDown = { move(source, 1) }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    ListItem(
                        modifier = Modifier.settingsItemClickable {
                            persist(DEFAULT_HOME_CONTENT_SOURCE_ORDER)
                        },
                        headlineContent = {
                            Text(stringResource(R.string.home_settings_reset_default))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.home_settings_reset_default_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
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

@Composable
private fun HomeSourceRow(
    source: HomeContentSource,
    orderIndex: Int,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val titleRes = when (source) {
        HomeContentSource.NETEASE -> R.string.home_settings_source_netease
        HomeContentSource.KUGOU -> R.string.home_settings_source_kugou
    }
    val descRes = when (source) {
        HomeContentSource.NETEASE -> R.string.home_settings_source_netease_desc
        HomeContentSource.KUGOU -> R.string.home_settings_source_kugou_desc
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (enabled) "${orderIndex + 1}" else "·",
            modifier = Modifier.width(24.dp),
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (enabled) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.home_settings_move_up)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.home_settings_move_down)
                )
            }
        }
        MiuixSettingsSwitch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}
