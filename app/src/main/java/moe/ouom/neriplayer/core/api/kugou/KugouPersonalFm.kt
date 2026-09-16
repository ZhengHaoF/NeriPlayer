package moe.ouom.neriplayer.core.api.kugou

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
 * File: moe.ouom.neriplayer.core.api.kugou/KugouPersonalFm
 */

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem
import top.ghhccghk.multiplatform.kugouapi.model.FmAction
import top.ghhccghk.multiplatform.kugouapi.model.FmMode

private const val TAG = "KugouPersonalFm"

/** 私人FM 播放模式。 */
enum class KugouFmMode(val apiValue: String, val label: String) {
    NORMAL("normal", "红心"),
    SMALL("small", "小众"),
    PEAK("peak", "速览");

    fun toSdkMode(): FmMode = when (this) {
        NORMAL -> FmMode.NORMAL
        SMALL -> FmMode.SMALL
        PEAK -> FmMode.PEAK
    }

    companion object {
        fun fromApiValue(value: String?): KugouFmMode =
            entries.firstOrNull { it.apiValue == value } ?: NORMAL
    }
}

/** 私人FM 曲库。 */
enum class KugouFmSongPool(val apiValue: Int, val label: String) {
    TASTE(0, "口味"),
    STYLE(1, "风格"),
    EXPLORE(2, "探索");

    companion object {
        fun fromApiValue(value: Int?): KugouFmSongPool =
            entries.firstOrNull { it.apiValue == value } ?: TASTE
    }
}

/** 私人FM 请求参数。 */
data class KugouFmParams(
    val mode: KugouFmMode = KugouFmMode.NORMAL,
    val songPoolId: KugouFmSongPool = KugouFmSongPool.TASTE,
    val action: FmAction = FmAction.PLAY,
    val hash: String? = null,
    val songId: Long? = null,
    val playTime: Int? = null,
    val isOverplay: Boolean = false,
    val remainSongCnt: Int = 0
)

/**
 * 拉取私人FM推荐歌曲。
 * 需要登录态；未登录时 SDK 会返回空列表或错误。
 */
suspend fun KugouSession.fetchKugouPersonalFm(params: KugouFmParams = KugouFmParams()): List<SongItem> {
    if (!isLoggedIn) {
        NPLogger.w(TAG, "fetchKugouPersonalFm called without login")
        return emptyList()
    }
    val response = client.recommend.getPersonalFm(
        params.action,
        params.mode.toSdkMode(),
        params.songPoolId.apiValue,
        params.remainSongCnt,
        params.isOverplay,
        params.hash.orEmpty(),
        params.songId?.toString().orEmpty(),
        params.playTime?.toString().orEmpty(),
        "android"
    )
    if (response.body["status"]?.jsonPrimitive?.int != 1) {
        NPLogger.w(TAG, "fetchKugouPersonalFm status=${response.body["status"]}")
        return emptyList()
    }
    val data = response.body["data"]?.jsonObject ?: return emptyList()
    val songs = data["songs"]?.jsonArray?.takeIf { it.isNotEmpty() }
        ?: data["song_list"]?.jsonArray
        ?: return emptyList()
    NPLogger.d(TAG, "fetchKugouPersonalFm got ${songs.size} songs, mode=${params.mode}, pool=${params.songPoolId}")
    return songs.mapNotNull { item -> parseKugouAudioEntry(item.jsonObject) }
}
