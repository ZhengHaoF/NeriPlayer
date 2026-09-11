package moe.ouom.neriplayer.core.api.kugou

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.ouom.neriplayer.core.api.search.MusicPlatform
import moe.ouom.neriplayer.core.api.search.SearchApi
import moe.ouom.neriplayer.core.api.search.SongDetails
import moe.ouom.neriplayer.core.api.search.SongSearchInfo
import moe.ouom.neriplayer.core.logging.NPLogger

/**
 * 酷狗搜索器：通过 [KugouSession] 走 SDK 的 complexsearch（免登录）。
 * 搜索结果映射为通用 [SongSearchInfo]（new_source = KUGOU）。
 */
class KugouSearchApi(private val session: KugouSession) : SearchApi {

    override suspend fun search(keyword: String, page: Int): List<SongSearchInfo> {
        return withContext(Dispatchers.IO) {
            val response = runCatching {
                session.ensureDeviceRegistered()
                session.client.search.search(keywords = keyword, page = page, pageSize = 30)
            }.getOrElse { error ->
                NPLogger.e(TAG, "kugou search failed: keyword=$keyword page=$page", error)
                return@withContext emptyList()
            }
            val bizStatus = response.body["status"]?.jsonPrimitive?.int
            if (bizStatus != 1) {
                NPLogger.w(TAG, "kugou search rejected: keyword=$keyword status=$bizStatus")
                return@withContext emptyList()
            }
            val lists = response.body["data"]?.jsonObject?.get("lists")?.jsonArray
                ?: return@withContext emptyList()
            lists.mapNotNull { item ->
                parseKugouSearchItem(item.jsonObject)?.let { song ->
                    SongSearchInfo(
                        id = song.hash,
                        songName = song.songName,
                        singer = song.singer,
                        duration = formatSearchDuration(song.durationMs),
                        source = MusicPlatform.KUGOU,
                        albumName = song.albumName,
                        coverUrl = song.coverUrl
                    )
                }
            }
        }
    }

    override suspend fun getSongInfo(id: String): SongDetails {
        // 老接口按 id(hash) 回填元数据；封面/歌词由播放与歌词链路自行取用，
        // 这里返回播放所需的电影信息，failure 时用占位兜底。
        return withContext(Dispatchers.IO) {
            SongDetails(
                id = id,
                songName = id,
                singer = "",
                album = "",
                coverUrl = null,
                lyric = null
            )
        }
    }

    private fun formatSearchDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format("%02d:%02d", minutes, seconds)
    }

    private companion object {
        const val TAG = "KugouSearchApi"
    }
}