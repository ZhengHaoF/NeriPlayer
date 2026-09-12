package moe.ouom.neriplayer.core.api.qqmusic

import java.io.IOException
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.util.network.awaitResponse
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** QQ音乐榜单元数据。 */
data class QQMusicRankMeta(
    val rankId: String,
    val rankName: String,
    val coverUrl: String?,
    val playCount: Long
)

/** QQ音乐歌单元数据（官方广场，匿名可读）。 */
data class QQMusicPlaylistMeta(
    val playlistId: String,
    val name: String,
    val coverUrl: String?,
    val playCount: Long,
    val creator: String?
)

/** QQ音乐 tab 默认内容：排行榜 + 热门歌单。 */
data class QQMusicChannelContent(
    val ranks: List<QQMusicRankMeta> = emptyList(),
    val playlists: List<QQMusicPlaylistMeta> = emptyList()
)

private const val TAG = "QQMusicChannel"
private const val QQMUSIC_QZONE_REFERER = "https://c.y.qq.com/"

/** org.json 的 [JSONArray] 不是 Iterable，用这个代替 Kotlin 的 mapNotNull。 */
private inline fun <T> JSONArray.mapEach(transform: (JSONObject) -> T?): List<T> {
    val result = ArrayList<T>(length())
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        transform(obj)?.let { result.add(it) }
    }
    return result
}

/**
 * QQ音乐 web 侧的匿名老接口都是 **GBK** 编码（即使 `format=json`），
 * 必须按 GB18030 解码，否则中文全部乱码。
 */
private suspend fun QQMusicSession.httpGetText(
    url: String,
    referer: String? = null,
    gbk: Boolean = false
): String = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(url)
        .header("User-Agent", QQMUSIC_USER_AGENT)
        .header("Referer", referer ?: QQMUSIC_REFERER)
        .apply {
            // qzone 老接口校验 referer 且要求携带 uin cookie，缺失时返回 invalid referer / check privacy error
            if (referer == QQMUSIC_QZONE_REFERER) header("Cookie", "uin=o0; uin=0")
        }
        .build()
    val charset: Charset = if (gbk) Charset.forName("GB18030") else Charsets.UTF_8
    client.newCall(request).awaitResponse { response ->
        if (!response.isSuccessful) throw IOException("QQ Music HTTP ${response.code}")
        String(response.body.bytes(), charset)
    }
}

/** 加载 QQ音乐 tab 默认内容（排行榜 + 热门歌单），各通道失败互不影响。 */
suspend fun QQMusicSession.loadQQMusicChannelContent(): QQMusicChannelContent {
    val ranks = runCatching { fetchQQMusicRankList() }.getOrElse { error ->
        NPLogger.w(TAG, "load ranks failed: ${error.message}")
        emptyList()
    }
    val playlists = runCatching { fetchQQMusicPlaylistList() }.getOrElse { error ->
        NPLogger.w(TAG, "load playlists failed: ${error.message}")
        emptyList()
    }
    return QQMusicChannelContent(ranks = ranks, playlists = playlists)
}

/** 排行榜列表：`fcg_myqq_toplist.fcg` → `data.topList[]`（匿名，UTF-8）。 */
suspend fun QQMusicSession.fetchQQMusicRankList(): List<QQMusicRankMeta> {
    val url = "https://c.y.qq.com/v8/fcg-bin/fcg_myqq_toplist.fcg?format=json&uin=0&needNewCode=1"
    val json = JSONObject(httpGetText(url))
    val topList = json.optJSONObject("data")?.optJSONArray("topList") ?: return emptyList()
    return topList.mapEach { obj ->
        val id = obj.optInt("id", -1)
        if (id < 0) {
            null
        } else {
            QQMusicRankMeta(
                rankId = id.toString(),
                rankName = obj.optString("topTitle"),
                coverUrl = obj.optString("picUrl").takeIf { it.startsWith("http", true) },
                playCount = obj.optLong("listenCount", 0L)
            )
        }
    }
}

/** 榜单歌曲：`fcg_v8_toplist_cp.fcg?topid=` → `songlist[]`（匿名，UTF-8）。 */
suspend fun QQMusicSession.fetchQQMusicRankSongs(topId: String): List<SongItem> {
    val url = "https://c.y.qq.com/v8/fcg-bin/fcg_v8_toplist_cp.fcg" +
        "?topid=$topId&type=top&format=json&song_begin=0&song_num=30"
    val json = JSONObject(httpGetText(url))
    val songlist = json.optJSONArray("songlist") ?: return emptyList()
    return songlist.mapEach { parseQQMusicLegacySongEntry(it) }
}

/** 热门歌单：`fcg_get_diss_by_tag.fcg` → `data.list[]`（匿名，GBK）。 */
suspend fun QQMusicSession.fetchQQMusicPlaylistList(): List<QQMusicPlaylistMeta> {
    val url = "https://c.y.qq.com/splcloud/fcgi-bin/fcg_get_diss_by_tag.fcg" +
        "?format=json&picmid=1&categoryId=10000000&sortId=5&sin=0&ein=29"
    val json = JSONObject(httpGetText(url, referer = QQMUSIC_QZONE_REFERER, gbk = true))
    val list = json.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
    return list.mapEach { obj ->
        val dissid = obj.optString("dissid").takeIf { it.isNotBlank() } ?: return@mapEach null
        QQMusicPlaylistMeta(
            playlistId = dissid,
            name = obj.optString("dissname"),
            coverUrl = obj.optString("imgurl").takeIf { it.startsWith("http", true) },
            playCount = obj.optLong("listennum", 0L),
            creator = obj.optJSONObject("creator")?.optString("name")?.takeIf { it.isNotBlank() }
        )
    }
}

/** 歌单歌曲：`fcg_ucc_getcdinfo_byids_cp.fcg` → `cdlist[0].songlist[]`（匿名，GBK，需 qzone referer + uin cookie）。 */
suspend fun QQMusicSession.fetchQQMusicPlaylistSongs(playlistId: String): List<SongItem> {
    val url = "https://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg" +
        "?format=json&type=1&uin=0&disstid=$playlistId&begin=0&song_num=30"
    val json = JSONObject(httpGetText(url, referer = QQMUSIC_QZONE_REFERER, gbk = true))
    val songlist = json.optJSONArray("cdlist")?.optJSONObject(0)?.optJSONArray("songlist")
        ?: return emptyList()
    return songlist.mapEach { parseQQMusicLegacySongEntry(it) }
}

/**
 * 解析 QQ音乐老版接口（榜单详情 / 歌单详情）的歌曲条目。
 * 两种包裹形态都容错：`{ data: {...} }`（榜单）或歌曲对象本身（歌单）。
 */
internal fun parseQQMusicLegacySongEntry(item: JSONObject?): SongItem? {
    if (item == null) return null
    val data = item.optJSONObject("data") ?: item
    val songMid = data.optString("songmid").takeIf { it.isNotBlank() }
        ?: return null
    val singers = data.optJSONArray("singer") ?: JSONArray()
    val singerNames = buildList {
        for (i in 0 until singers.length()) {
            val name = singers.optJSONObject(i)?.optString("name").orEmpty()
            if (name.isNotBlank()) add(name)
        }
    }
    return buildQQMusicSongItem(
        songMid = songMid,
        songName = data.optString("songname"),
        singer = singerNames.joinToString("/"),
        albumName = data.optString("albumname").takeIf { it.isNotBlank() },
        durationMs = data.optLong("interval", 0L) * 1_000L,
        coverUrl = buildQQMusicCoverUrl(data.optString("albummid").takeIf { it.isNotBlank() }),
        mediaMid = data.optString("strMediaMid").takeIf { it.isNotBlank() }
    )
}
