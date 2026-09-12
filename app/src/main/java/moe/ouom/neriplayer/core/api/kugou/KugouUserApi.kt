package moe.ouom.neriplayer.core.api.kugou

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem
import org.json.JSONObject

/**
 * 酷狗概念版登录后内容（阶段 4）：听歌历史 / 收藏歌单 / 云盘。
 * 依赖登录态（token/userid），未登录时返回空列表；调用前需确保 [KugouSession.isLoggedIn]。
 */

/** 听歌历史：`user.getUserHistory` → 响应结构容错解析（song_list/list/info 皆可）。 */
suspend fun KugouSession.fetchKugouHistory(): List<SongItem> {
    ensureDeviceRegistered()
    val response = client.user.getUserHistory()
    val body = response.body
    if (body["status"]?.jsonPrimitive?.int != 1) {
        NPLogger.w("KugouUser", "fetch history rejected: ${body.toString().take(160)}")
        return emptyList()
    }
    val data = body["data"]?.jsonObject ?: return emptyList()
    val songs = data["song_list"]?.jsonArray
        ?: data["list"]?.jsonArray
        ?: data["info"]?.jsonArray
        ?: return emptyList()
    return songs.mapNotNull { item -> parseKugouAudioEntry(item.jsonObject) }
}

/** 收藏/创建的歌单：`user.getUserPlaylist` → `data.info[]`。 */
suspend fun KugouSession.fetchKugouUserPlaylists(): List<KugouPlaylistMeta> {
    ensureDeviceRegistered()
    val response = client.user.getUserPlaylist(page = 1, pageSize = 50)
    val body = response.body
    if (body["status"]?.jsonPrimitive?.int != 1) {
        NPLogger.w("KugouUser", "fetch user playlists rejected: ${body.toString().take(160)}")
        return emptyList()
    }
    val info = body["data"]?.jsonObject?.get("info")?.jsonArray.orEmpty()
    // 放宽截断长度：封面字段 pic 位于响应靠后位置，4000 字符会被截掉，排查封面问题时看不到
    NPLogger.d("KugouUser", "user playlists raw head: ${info.firstOrNull()?.toString()?.take(12_000)}")
    return info.mapNotNull { item ->
        val obj = item.jsonObject
        val id = obj["global_collection_id"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: obj["specialid"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        KugouPlaylistMeta(
            globalCollectionId = id,
            name = obj["specialname"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            // 该接口封面字段为 `pic`（EchoMusic mappers/playlist.ts:70 与 MoeKoe Library.vue 均取 pic），
            // imgurl / cover / img 仅作兜底；统一走 resolveKugouCoverUrl 做 {size} 替换与协议补全
            coverUrl = resolveKugouCoverUrl(
                obj["pic"],
                obj["imgurl"],
                obj["cover"],
                obj["img"],
            ),
            playCount = obj["play_count"]?.jsonPrimitive?.longOrNull
                ?: obj["playcount"]?.jsonPrimitive?.longOrNull
                ?: 0L,
            creator = obj["nickname"]?.jsonPrimitive?.contentOrNull
        )
    }
}

/**
 * 云盘歌曲：`user.getCloudList` → 响应为 AES/RSA 包裹，SDK 已解密并将 JSON 放回 body。
 * 兜底解析 data.list / raw 字段；未知结构时打印 head 便于真机排查。
 */
suspend fun KugouSession.fetchKugouCloudSongs(): List<SongItem> {
    ensureDeviceRegistered()
    val response = client.user.getCloudList(page = 1, pageSize = 50)
    val body = response.body
    val list = extractCloudList(body)
    if (list.isEmpty()) {
        NPLogger.w("KugouUser", "cloud list empty, bodyHead=${body.toString().take(200)}")
    } else {
        NPLogger.d("KugouUser", "cloud list size=${list.size}")
    }
    return list.mapNotNull { item ->
        val obj = item.jsonObject
        val hash = obj["hash"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: obj["FileHash"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val rawName = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val fileName = obj["fileName"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val songName = obj["songname"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: rawName.substringAfter(" - ", rawName).ifBlank { fileName.substringAfter(" - ", fileName) }
        val singer = obj["singername"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: rawName.substringBefore(" - ", "").trim()
            ?: fileName.substringBefore(" - ", "").trim()
        val albumAudioId = obj["album_audio_id"]?.jsonPrimitive?.longOrNull ?: 0L
        val audioId = obj["audio_id"]?.jsonPrimitive?.longOrNull ?: 0L
        val albumId = obj["album_id"]?.jsonPrimitive?.longOrNull ?: 0L
        val durationMs = (obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L) * 1_000L
        val coverUrl = obj["img"]?.jsonPrimitive?.contentOrNull
            ?.replace("{size}", "320")
            ?.takeIf { it.startsWith("http", ignoreCase = true) }
        buildCloudSongItem(
            hash = hash,
            songName = songName,
            singer = singer,
            albumId = albumId,
            albumAudioId = albumAudioId,
            audioId = audioId,
            coverUrl = coverUrl,
            durationMs = durationMs
        )
    }
}

/** 从云盘响应提取歌曲 JSON 数组（兼容 data.list / raw 字符串兜底）。 */
private fun extractCloudList(body: JsonObject): List<JsonObject> {
    if (body["status"]?.jsonPrimitive?.int != 1) return emptyList()
    val dataList: MutableList<JsonObject> = mutableListOf()
    body["data"]?.jsonObject?.let { data ->
        data["list"]?.jsonArray?.forEach { dataList.add(it.jsonObject) }
    }
    if (dataList.isNotEmpty()) return dataList

    val raw = body["raw"]?.jsonPrimitive?.contentOrNull ?: return emptyList()
    val parsed = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
    val nested = parsed.optJSONObject("data") ?: return emptyList()
    val list = nested.optJSONArray("list") ?: return emptyList()
    return (0 until list.length()).mapNotNull { i ->
        runCatching { kotlinx.serialization.json.Json.parseToJsonElement(list.optJSONObject(i).toString()).jsonObject }
            .getOrNull()
    }
}

/** 云盘歌曲构建（album 标记 `PlayerManagerCloudTag|hash|audioId`，播放分支据此走 getCloudUrl）。 */
internal fun buildCloudSongItem(
    hash: String,
    songName: String,
    singer: String,
    albumId: Long,
    albumAudioId: Long,
    audioId: Long,
    coverUrl: String?,
    durationMs: Long = 0L
): SongItem {
    val idPart = if (audioId > 0L) audioId else albumAudioId.takeIf { it > 0L } ?: 0L
    return SongItem(
        id = idPart.takeIf { it > 0L } ?: hash.hashCode().toLong(),
        name = songName,
        artist = singer,
        album = listOfNotNull(PlayerManagerCloudTag, hash, idPart.takeIf { it > 0L }?.toString())
            .joinToString(separator = "|"),
        albumId = albumId,
        durationMs = durationMs,
        coverUrl = coverUrl,
        channelId = KUGOU_CHANNEL_ID,
        audioId = hash,
        subAudioId = if (idPart > 0L) idPart.toString() else null
    )
}

/** 云盘歌曲的 album 前缀，播放分支据此走 `getCloudUrl`。 */
const val PlayerManagerCloudTag = "KugouCloud"