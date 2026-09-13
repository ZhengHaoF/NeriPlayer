package moe.ouom.neriplayer.core.api.qqmusic

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.util.network.awaitResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "QQMusicUserApi"
private const val MUSICU_HOST = "https://u.y.qq.com/cgi-bin/musicu.fcg"
private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

/** QQ音乐用户歌单元数据（登录态「我的歌单」）。 */
data class QQMusicUserPlaylistMeta(
    val playlistId: String,
    val name: String,
    val coverUrl: String?,
    val songCount: Int,
    val playCount: Long,
    val creator: String?,
)

/**
 * 拉取当前登录账号创建的歌单列表。
 *
 * 对齐上游 QQMusicApi `user.get_created_songlist`：
 * `music.musicasset.PlaylistBaseRead` / `GetPlaylistByUin`，param 仅需 `uin`。
 * 响应在 `req_0.data.v_playlist[]`。走 Android comm（与登录/续期同链路）。
 *
 * 蓝本 QQMusicapi 的 `GetUserSonglist` 实测返回 500003/860100001，已弃用。
 */
suspend fun QQMusicSession.fetchQQMusicUserPlaylists(
    page: Int = 1,
    pageSize: Int = 50,
): List<QQMusicUserPlaylistMeta> = withContext(Dispatchers.IO) {
    val cred = credential
    if (!cred.isLoggedIn) return@withContext emptyList()

    try {
        device.ensureReady()
    } catch (error: Exception) {
        NPLogger.w(TAG, "device not ready for GetPlaylistByUin: ${error.message}")
        throw error
    }

    val payload = JSONObject()
        .put("comm", device.buildAndroidComm(cred))
        .put(
            "req_0",
            JSONObject()
                .put("module", "music.musicasset.PlaylistBaseRead")
                .put("method", "GetPlaylistByUin")
                .put(
                    "param",
                    JSONObject()
                        .put("uin", cred.musicid.toString())
                )
        )

    val request = Request.Builder()
        .url(MUSICU_HOST)
        .header("User-Agent", device.androidUserAgent())
        .header("Referer", QQMUSIC_REFERER)
        .header("Cookie", cred.toCookieHeader())
        .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    val responseText = client.newCall(request).awaitResponse { response ->
        if (!response.isSuccessful) throw IOException("GetPlaylistByUin HTTP ${response.code}")
        response.body.string()
    }

    val root = runCatching { JSONObject(responseText) }.getOrElse { error ->
        NPLogger.w(TAG, "GetPlaylistByUin not JSON: ${error.message} body=${responseText.take(160)}")
        throw IOException("GetPlaylistByUin invalid response")
    }
    val globalCode = root.optInt("code", 0)
    if (globalCode != 0) {
        NPLogger.w(TAG, "GetPlaylistByUin global code=$globalCode: ${responseText.take(200)}")
        throw IOException("GetPlaylistByUin global code=$globalCode")
    }
    val req0 = root.optJSONObject("req_0")
    val bizCode = req0?.optInt("code", -1) ?: -1
    if (bizCode != 0) {
        NPLogger.w(TAG, "GetPlaylistByUin biz code=$bizCode: ${responseText.take(240)}")
        throw IOException("GetPlaylistByUin biz code=$bizCode")
    }

    val data = req0.optJSONObject("data")
    NPLogger.i(TAG, "GetPlaylistByUin head=${responseText.take(800)}")
    val list = extractPlaylistArray(data)
    val parsed = parseCreatedPlaylists(list)
    NPLogger.i(
        TAG,
        "GetPlaylistByUin total=${data?.opt("total")} listLen=${list.length()} " +
            "parsed=${parsed.size} first=${parsed.firstOrNull()?.name}"
    )
    return@withContext parsed
}

/** 歌单数组：优先 `v_playlist`（上游模型 jsonpath），兼容 `list` / `playlists`。 */
private fun extractPlaylistArray(data: JSONObject?): JSONArray {
    if (data == null) return JSONArray()
    for (key in listOf("v_playlist", "list", "playlists", "playlist")) {
        val arr = data.optJSONArray(key)
        if (arr != null && arr.length() > 0) return arr
    }
    return JSONArray()
}

private fun parseCreatedPlaylists(list: JSONArray): List<QQMusicUserPlaylistMeta> {
    val result = ArrayList<QQMusicUserPlaylistMeta>(list.length())
    for (i in 0 until list.length()) {
        val obj = list.optJSONObject(i) ?: continue
        // 失效歌单跳过
        if (obj.optBoolean("invalid", false)) continue
        val id = firstNotBlank(obj, "id", "tid", "dissid", "diss_id") ?: continue
        val name = firstNotBlank(obj, "title", "dissname", "name", "dirName").orEmpty()
        if (name.isBlank()) continue
        val cover = firstHttpUrl(obj, "picurl", "picUrl", "cover", "logo", "bigpicUrl", "albumPicUrl")
        val songCount = firstPositiveInt(obj, "songnum", "songNum", "song_cnt")
        val playCount = firstPositiveLong(obj, "play_cnt", "playCnt", "listennum", "listen_count")
        val creator = firstNotBlank(obj, "nick", "nickname", "creator_name")
        result.add(
            QQMusicUserPlaylistMeta(
                playlistId = id,
                name = name,
                coverUrl = cover,
                songCount = songCount,
                playCount = playCount,
                creator = creator,
            )
        )
    }
    return result
}

private fun firstNotBlank(obj: JSONObject, vararg keys: String): String? {
    for (key in keys) {
        val value = obj.opt(key)?.toString()?.trim()
        if (!value.isNullOrBlank() && value != "null") return value
    }
    return null
}

private fun firstHttpUrl(obj: JSONObject, vararg keys: String): String? {
    val raw = firstNotBlank(obj, *keys) ?: return null
    return when {
        raw.startsWith("http", ignoreCase = true) -> raw
        raw.startsWith("//") -> "https:$raw"
        else -> null
    }
}

private fun firstPositiveInt(obj: JSONObject, vararg keys: String): Int {
    for (key in keys) {
        val value = obj.optInt(key, -1)
        if (value > 0) return value
    }
    return 0
}

private fun firstPositiveLong(obj: JSONObject, vararg keys: String): Long {
    for (key in keys) {
        val value = obj.optLong(key, -1L)
        if (value > 0L) return value
    }
    return 0L
}
