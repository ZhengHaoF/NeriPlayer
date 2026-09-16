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
 * File: moe.ouom.neriplayer.ui.screen.tab.settings.page/TabOrderSectionCard
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.ui.screen.tab.settings.component.settingsItemClickable
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsSwitch

/** 拖拽排序卡片中的一行条目。标签/描述用资源 ID, 在行内才解析为文字。 */
internal data class TabOrderItem<T>(
    val key: String,
    val labelRes: Int,
    val descRes: Int? = null,
    val value: T,
    val visible: Boolean,
    val pinned: Boolean = false
)

private val TabOrderRowHeight = 56.dp
private val TabOrderRowDivider = 1.dp

/**
 * 设置子页里通用的「拖拽排序 + 开关隐藏」分组卡片。
 *
 * [items] 中可见项排在前且有序, 隐藏项跟在后面；拖拽只在可见项之间生效,
 * [pinned] 项无拖拽手柄、开关锁定, 且其他项不能拖到它前面。
 * 拖拽过程中本地实时重排, 松手后通过 [onPersistOrder] 提交可见项顺序。
 */
@Composable
internal fun <T> TabOrderSectionCard(
    title: String,
    description: String,
    items: List<TabOrderItem<T>>,
    onPersistOrder: (List<T>) -> Unit,
    onToggle: (T, Boolean) -> Unit,
    onReset: () -> Unit
) {
    // 拖拽过程中的临时顺序覆盖; 拖拽结束后提交, 并在 items 变化时清除
    val itemsState = rememberUpdatedState(items)
    val onPersistOrderState = rememberUpdatedState(onPersistOrder)
    val dragOrderState = remember { mutableStateOf<List<T>?>(null) }
    LaunchedEffect(items) {
        dragOrderState.value = null
    }

    val displayVisible = dragOrderState.value
        ?: items.filter { it.visible }.map { it.value }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val currentItems = itemsState.value
            val visibleValues = dragOrderState.value
                ?: currentItems.filter { it.visible }.map { it.value }
            val fromValue = currentItems.firstOrNull { it.key == from.key }
                ?.takeIf { it.value in visibleValues && !it.pinned }?.value
                ?: return@rememberReorderableLazyListState
            val toValue = currentItems.firstOrNull { it.key == to.key }
                ?.takeIf { it.value in visibleValues && !it.pinned }?.value
                ?: return@rememberReorderableLazyListState
            val current = visibleValues.toMutableList()
            val fromIndex = current.indexOf(fromValue)
            val toIndex = current.indexOf(toValue)
            if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
                return@rememberReorderableLazyListState
            }
            current.removeAt(fromIndex)
            current.add(toIndex, fromValue)
            dragOrderState.value = current
        },
        canDragOver = { over, _ ->
            val currentItems = itemsState.value
            val overItem = currentItems.firstOrNull { it.key == over.key }
                ?: return@rememberReorderableLazyListState false
            val visibleValues = dragOrderState.value
                ?: currentItems.filter { it.visible }.map { it.value }
            overItem.value in visibleValues && !overItem.pinned
        },
        onDragEnd = { _, _ ->
            val override = dragOrderState.value ?: return@rememberReorderableLazyListState
            if (override.isNotEmpty()) {
                onPersistOrderState.value(override)
            }
        }
    )

    MiuixSettingsSectionCard {
        MiuixSettingsSectionIntro(
            title = title,
            description = description
        )
        val listHeight = TabOrderRowHeight * items.size + TabOrderRowDivider * (items.size - 1)
        LazyColumn(
            state = reorderState.listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(listHeight)
                .reorderable(reorderState)
        ) {
            items(items.size, key = { index -> items[index].key }) { index ->
                val item = items[index]
                ReorderableItem(state = reorderState, key = item.key) { isDragging ->
                    TabOrderRow(
                        item = item,
                        orderIndex = displayVisible.indexOf(item.value) + 1,
                        isDragging = isDragging,
                        showDivider = index < items.lastIndex,
                        reorderState = reorderState
                    ) { checked ->
                        onToggle(item.value, checked)
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        ListItem(
            modifier = Modifier.settingsItemClickable(onClick = onReset),
            headlineContent = {
                Text(stringResource(R.string.tab_order_reset_default))
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun <T> TabOrderRow(
    item: TabOrderItem<T>,
    orderIndex: Int,
    isDragging: Boolean,
    showDivider: Boolean,
    reorderState: ReorderableLazyListState,
    onToggle: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TabOrderRowHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (item.visible) "$orderIndex" else "·",
                modifier = Modifier.width(24.dp),
                style = MaterialTheme.typography.titleMedium,
                color = if (item.visible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.visible) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                item.descRes?.let { descRes ->
                    Text(
                        text = stringResource(descRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (item.visible && !item.pinned) {
                Box(
                    modifier = Modifier
                        .detectReorder(reorderState)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = null,
                        tint = if (isDragging) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }
            MiuixSettingsSwitch(
                checked = item.visible,
                enabled = !item.pinned,
                onCheckedChange = onToggle
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = TabOrderRowDivider,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}
