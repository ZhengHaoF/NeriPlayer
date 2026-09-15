package moe.ouom.neriplayer.ui.viewmodel.tab

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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab/HomeContentSources
 */

/**
 * 首页内容源。首页按 [parseHomeContentSourceOrder] 解析出的顺序尝试加载：
 * 当前源所有展示接口均无数据（失败或为空）时，自动降级到下一个源；
 * 每次刷新都会重新按序探测，主源恢复后自然回切。
 *
 * YouTube Music 与本地「继续播放」不参与此排序：
 * YouTube 由「国际化」开关控制，「继续播放」由播放历史控制。
 */
enum class HomeContentSource(val id: String) {
    NETEASE("netease"),
    KUGOU("kugou")
}

/** 默认排序：网易云音乐 → 酷狗音乐 */
val DEFAULT_HOME_CONTENT_SOURCE_ORDER: List<HomeContentSource> =
    listOf(HomeContentSource.NETEASE, HomeContentSource.KUGOU)

/** 设置存储格式：逗号分隔的源 id，如 "netease,kugou" */
const val DEFAULT_HOME_CONTENT_SOURCE_ORDER_RAW: String = "netease,kugou"

/**
 * 解析设置存储的源顺序字符串。非法/未知 id 会被丢弃并去重；
 * 解析结果为空时回落到默认排序，保证首页始终有内容源可用。
 */
fun parseHomeContentSourceOrder(raw: String): List<HomeContentSource> {
    val parsed = raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { id -> HomeContentSource.entries.firstOrNull { it.id == id } }
        .distinct()
    return if (parsed.isEmpty()) DEFAULT_HOME_CONTENT_SOURCE_ORDER else parsed
}

/** 将源顺序序列化为设置存储格式。 */
fun formatHomeContentSourceOrder(order: List<HomeContentSource>): String =
    order.joinToString(",") { it.id }
