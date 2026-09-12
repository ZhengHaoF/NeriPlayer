package moe.ouom.neriplayer.core.api.kugou

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem

/** 酷狗概念版榜单元数据。 */
data class KugouRankMeta(
    val rankId: String,
    val rankName: String,
    val coverUrl: String?,
    val playCount: Long,
    val intro: String?
)

/** 酷狗概念版歌单元数据（special_recommend 推荐歌单）。 */
data class KugouPlaylistMeta(
    val globalCollectionId: String,
    val name: String,
    val coverUrl: String?,
    val playCount: Long,
    val creator: String?
)

/** 酷狗概念版 tab 默认内容：榜单 + 热门歌单 + 每日推荐。 */
data class KugouChannelContent(
    val ranks: List<KugouRankMeta> = emptyList(),
    val playlists: List<KugouPlaylistMeta> = emptyList(),
    val dailyRecommend: List<SongItem> = emptyList()
)

/** 加载酷狗概念版 tab 默认内容（榜单列表 + 热门歌单 + 每日推荐）。 */
suspend fun KugouSession.loadKugouChannelContent(): KugouChannelContent {
    ensureDeviceRegistered()
    val ranks = runCatching { fetchKugouRankList() }.getOrElse { error ->
        NPLogger.w("KugouChannel", "load ranks failed: ${error.message}")
        emptyList()
    }
    val playlists = runCatching { fetchKugouPlaylistList() }.getOrElse { error ->
        NPLogger.w("KugouChannel", "load playlists failed: ${error.message}")
        emptyList()
    }
    val daily = runCatching { fetchKugouDailyRecommend() }.getOrElse { error ->
        NPLogger.w("KugouChannel", "load daily recommend failed: ${error.message}")
        emptyList()
    }
    return KugouChannelContent(ranks = ranks, playlists = playlists, dailyRecommend = daily)
}

/** 榜单列表：`rank.getList` → `data.info[]`。 */
suspend fun KugouSession.fetchKugouRankList(): List<KugouRankMeta> {
    val response = client.rank.getList(withSong = 0)
    if (response.body["status"]?.jsonPrimitive?.int != 1) return emptyList()
    return response.body["data"]?.jsonObject?.get("info")?.jsonArray.orEmpty()
        .mapNotNull { item ->
            val obj = item.jsonObject
            val rankId = obj["rankid"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            KugouRankMeta(
                rankId = rankId,
                rankName = obj["rankname"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                coverUrl = obj["img_9"]?.jsonPrimitive?.contentOrNull
                    ?.replace("{size}", "320")
                    ?.takeIf { it.startsWith("http", ignoreCase = true) },
                playCount = obj["play_times"]?.jsonPrimitive?.longOrNull ?: 0L,
                intro = obj["intro"]?.jsonPrimitive?.contentOrNull
            )
        }
}

/** 榜单歌曲：`rank.getAudio(rankId)` → `data.songlist[]`。 */
suspend fun KugouSession.fetchKugouRankSongs(rankId: String): List<SongItem> {
    val response = client.rank.getAudio(rankId = rankId, page = 1, pageSize = 50)
    if (response.body["status"]?.jsonPrimitive?.int != 1) return emptyList()
    return response.body["data"]?.jsonObject?.get("songlist")?.jsonArray.orEmpty()
        .mapNotNull { item -> parseKugouAudioEntry(item.jsonObject) }
}

/** 热门歌单：`top.getTopPlaylist` → `data.special_list[]`。 */
suspend fun KugouSession.fetchKugouPlaylistList(): List<KugouPlaylistMeta> {
    val response = client.top.getTopPlaylist(page = 1, pageSize = 20)
    if (response.body["status"]?.jsonPrimitive?.int != 1) return emptyList()
    return response.body["data"]?.jsonObject?.get("special_list")?.jsonArray.orEmpty()
        .mapNotNull { item ->
            val obj = item.jsonObject
            val globalId = obj["global_collection_id"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            KugouPlaylistMeta(
                globalCollectionId = globalId,
                name = obj["specialname"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                coverUrl = obj["imgurl"]?.jsonPrimitive?.contentOrNull
                    ?.replace("{size}", "320")
                    ?.takeIf { it.startsWith("http", ignoreCase = true) },
                playCount = obj["play_count"]?.jsonPrimitive?.longOrNull ?: 0L,
                creator = obj["nickname"]?.jsonPrimitive?.contentOrNull
            )
        }
}

/** 歌单歌曲：`playlist.getPlaylistTracks(globalCollectionId)` → `data.songs[]`。 */
suspend fun KugouSession.fetchKugouPlaylistSongs(globalCollectionId: String): List<SongItem> {
    val response = client.playlist.getPlaylistTracks(id = globalCollectionId, page = 1, pageSize = 50)
    if (response.body["status"]?.jsonPrimitive?.int != 1) return emptyList()
    val data = response.body["data"]?.jsonObject ?: return emptyList()
    val songs = data["songs"]?.jsonArray?.takeIf { it.isNotEmpty() }
        ?: data["list"]?.jsonArray
        ?: return emptyList()
    NPLogger.d("KugouChannel", "playlist songs raw head: ${songs.firstOrNull()?.toString()?.take(500)}")
    return songs.mapNotNull { item -> parseKugouAudioEntry(item.jsonObject) }
}

/** 每日推荐：`recommend.getDailyRecommend` → `data.song_list[]`。 */
suspend fun KugouSession.fetchKugouDailyRecommend(): List<SongItem> {
    val response = client.recommend.getDailyRecommend()
    if (response.body["status"]?.jsonPrimitive?.int != 1) return emptyList()
    return response.body["data"]?.jsonObject?.get("song_list")?.jsonArray.orEmpty()
        .mapNotNull { item ->
            val obj = item.jsonObject
            val hash = obj["hash"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            buildKugouSongItem(
                hash = hash,
                songName = obj["songname"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                singer = obj["author_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                albumName = obj["album_name"]?.jsonPrimitive?.contentOrNull,
                albumId = obj["album_id"]?.jsonPrimitive?.longOrNull ?: 0L,
                albumAudioId = 0L,
                coverUrl = resolveKugouCoverUrl(
                    obj["sizable_cover"],
                    obj["img"],
                    obj["trans_param"]?.jsonObject?.get("union_cover")
                ),
                durationMs = (obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L) * 1_000L
            )
        }
}

/**
 * 解析歌曲条目中的封面 URL（酷狗概念版不同接口封面字段名不同：
 * 每日推荐用 `sizable_cover`/`trans_param.union_cover`，搜索/榜单/歌单用 `img`/`Image` 等），
 * 按候选顺序取第一个非空值，统一替换 {size} 占位符并校验 http 前缀。
 */
internal fun resolveKugouCoverUrl(vararg candidates: JsonElement?): String? {
    return candidates.firstNotNullOfOrNull { it?.jsonPrimitive?.contentOrNull }
        ?.replace("{size}", "320")
        ?.takeIf { it.startsWith("http", ignoreCase = true) }
}

/**
 * 解析接口返回的歌曲条目（搜索/榜单/歌单通用，字段名做过容错）：
 * - hash：顶层 `hash` → `audio_info.hash_128/hash_320`（榜单音频哈希）→ `FileHash`
 * - 歌名：`songname` / `filename` / `ori_audio_name`（"歌手 - 歌名" 格式拆分）
 * - 歌手：`author_name` / `singername`
 * - album_audio_id：`audio_id` / `album_audio_id` / `mixsongid` / `MixSongID`
 * - 时长：`audio_info.duration_128`（毫秒）或顶层 `duration`（秒）
 */
internal fun parseKugouAudioEntry(obj: JsonObject): SongItem? {
    val audioInfo = obj["audio_info"]?.jsonObject
    val hash = obj["hash"]?.jsonPrimitive?.contentOrNull?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: audioInfo?.get("hash_128")?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: audioInfo?.get("hash_320")?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: obj["FileHash"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        ?: return null
    val filename = obj["filename"]?.jsonPrimitive?.contentOrNull.orEmpty()
    // 用户歌单（get_other_list_file_nofilt）等接口：歌名在 `name`（"歌手 - 歌名" 或纯歌名）
    val rawName = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val displayName = filename.ifBlank { rawName }
    val songName = obj["songname"]?.jsonPrimitive?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?: obj["ori_audio_name"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
        ?: displayName.substringAfter(" - ", displayName).takeIf { it.isNotBlank() }
            ?: rawName
    val singer = obj["author_name"]?.jsonPrimitive?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?: obj["singername"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
        ?: displayName.substringBefore(" - ", "").trim()
            .takeIf { it.isNotBlank() && displayName.contains(" - ") }
            ?: rawName.substringBefore(" - ", rawName).trim()
                .takeIf { rawName.isNotBlank() && rawName.contains(" - ") }
            ?: ""
    val albumAudioId = obj["album_audio_id"]?.jsonPrimitive?.longOrNull
        ?: obj["add_mixsongid"]?.jsonPrimitive?.longOrNull
        ?: obj["mixsongid"]?.jsonPrimitive?.longOrNull
        ?: obj["MixSongID"]?.jsonPrimitive?.longOrNull
        ?: obj["audio_id"]?.jsonPrimitive?.longOrNull
        ?: 0L
    val albumId = obj["album_id"]?.jsonPrimitive?.longOrNull
        ?: obj["album_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        ?: obj["AlbumID"]?.jsonPrimitive?.longOrNull
        ?: obj["AlbumID"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        ?: 0L
    val durationMs = audioInfo?.get("duration_128")?.jsonPrimitive?.longOrNull
        ?: audioInfo?.get("duration_320")?.jsonPrimitive?.longOrNull
        ?: (obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L) * 1_000L
    val coverUrl = resolveKugouCoverUrl(
        obj["cover"],
        obj["sizable_cover"],
        obj["img"],
        obj["Image"],
        obj["trans_param"]?.jsonObject?.get("union_cover"),
        obj["album_info"]?.jsonObject?.get("imgurl"),
        obj["album_info"]?.jsonObject?.get("img")
    )
    return buildKugouSongItem(
        hash = hash,
        songName = songName,
        singer = singer,
        albumName = obj["album_name"]?.jsonPrimitive?.contentOrNull,
        albumId = albumId,
        albumAudioId = albumAudioId,
        coverUrl = coverUrl,
        durationMs = durationMs
    )
}