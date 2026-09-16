package moe.ouom.neriplayer.ui.screen.tab

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
 * File: moe.ouom.neriplayer.ui.screen.tab/TabOrdering
 */

import moe.ouom.neriplayer.ui.viewmodel.tab.SearchSource

/*
 * 探索页 / 媒体库标签排序
 *
 * 设置里保存的是「可见标签」的 id 顺序 (逗号分隔, 见 [formatExploreTabOrder])：
 * - 空串表示未自定义, 跟随内置默认顺序 (由国际化 / YouTube 开关决定)
 * - 缺失的标签视为被用户隐藏
 * - 非法/未知 id 直接丢弃, 结果为空时回落到默认顺序
 * - 媒体库的「本地」固定第一位且始终可见
 */

internal val SearchSource.tabId: String
    get() = when (this) {
        SearchSource.YOUTUBE_MUSIC -> "youtube_music"
        SearchSource.NETEASE -> "netease"
        SearchSource.BILIBILI -> "bilibili"
        SearchSource.KUGOU -> "kugou"
        SearchSource.QQ_MUSIC -> "qq_music"
        SearchSource.LINK_RECOGNITION -> "link_recognition"
    }

internal val LibraryTab.tabId: String
    get() = when (this) {
        LibraryTab.LOCAL -> "local"
        LibraryTab.FAVORITE -> "favorite"
        LibraryTab.YTMUSIC -> "youtube_music"
        LibraryTab.NETEASE -> "netease"
        LibraryTab.NETEASEALBUM -> "netease_album"
        LibraryTab.BILI -> "bilibili"
        LibraryTab.QQMUSIC -> "qq_music"
        LibraryTab.KUGOU -> "kugou"
    }

/** 解析设置存储的探索页标签顺序字符串；未知 id 丢弃并去重。 */
internal fun parseExploreTabOrder(raw: String): List<SearchSource> {
    return raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { id -> SearchSource.entries.firstOrNull { it.tabId == id } }
        .distinct()
}

/** 将探索页标签顺序序列化为设置存储格式。 */
internal fun formatExploreTabOrder(order: List<SearchSource>): String =
    order.joinToString(",") { it.tabId }

/**
 * 计算探索页实际展示的标签顺序：
 * 未自定义时跟随默认顺序；自定义后先按存储顺序排,
 * 再过滤掉当前不可用的源 (如 YouTube 被关闭), 全部无效时回落默认。
 */
internal fun resolveExploreTabOrder(
    raw: String,
    isInternational: Boolean,
    youtubeEnabled: Boolean
): List<SearchSource> {
    val allowed = exploreSearchSourceDisplayOrder(isInternational, youtubeEnabled)
    if (raw.isBlank()) return allowed
    val custom = parseExploreTabOrder(raw).filter { it in allowed }
    return custom.ifEmpty { allowed }
}

/** 解析设置存储的媒体库标签顺序字符串；未知 id 丢弃并去重。 */
internal fun parseLibraryTabOrder(raw: String): List<LibraryTab> {
    return raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { id -> LibraryTab.entries.firstOrNull { it.tabId == id } }
        .distinct()
}

/** 将媒体库标签顺序序列化为设置存储格式。 */
internal fun formatLibraryTabOrder(order: List<LibraryTab>): String =
    order.joinToString(",") { it.tabId }

/**
 * 计算媒体库实际展示的标签顺序：
 * 「本地」固定第一位且始终可见, 其余按用户自定义排序并过滤不可用源。
 */
internal fun resolveLibraryTabOrder(
    raw: String,
    isInternational: Boolean,
    youtubeEnabled: Boolean
): List<LibraryTab> {
    val allowed = libraryTabDisplayOrder(isInternational, youtubeEnabled)
    if (raw.isBlank()) return allowed
    val custom = parseLibraryTabOrder(raw).filter { it in allowed && it != LibraryTab.LOCAL }
    return listOf(LibraryTab.LOCAL) + custom
}
