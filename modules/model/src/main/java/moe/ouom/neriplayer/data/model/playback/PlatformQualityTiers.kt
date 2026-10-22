package moe.ouom.neriplayer.data.model.playback

/**
 * KuGou / QQ Music 的音质档位标识与降级链。
 *
 * 放在 model 层是因为播放链路（:playback:runtime）、平台客户端（app 的 api 层）和
 * 设置持久化都要用同一组档位键，而它们分属不同层，任何一层都不能引用另外两层。
 */

const val KUGOU_FREE_QUALITY = "128"
const val KUGOU_VIP_QUALITY = "320"
const val KUGOU_LOSSLESS_QUALITY = "flac"
const val KUGOU_HI_RES_QUALITY = "high"
const val KUGOU_VIPER_TAPE_QUALITY = "viper_tape"

const val QQ_MUSIC_FREE_QUALITY = "M500"
const val QQ_MUSIC_ANONYMOUS_FALLBACK_QUALITY = "C400"

/** 酷狗概念版已知档位，由高到低。 */
val KUGOU_QUALITY_FALLBACK_ORDER = listOf(
    KUGOU_VIPER_TAPE_QUALITY,
    KUGOU_HI_RES_QUALITY,
    KUGOU_LOSSLESS_QUALITY,
    KUGOU_VIP_QUALITY,
    KUGOU_FREE_QUALITY
)

/** QQ 音乐档位，由高到低。匿名态实测只能取到 M500 / C400。 */
val QQMUSIC_QUALITY_FALLBACK_ORDER = listOf(
    "F000",
    "O800",
    "M800",
    "C600",
    QQ_MUSIC_FREE_QUALITY,
    QQ_MUSIC_ANONYMOUS_FALLBACK_QUALITY
)

private val kugouQualityKeys = KUGOU_QUALITY_FALLBACK_ORDER.toSet()
private val qqMusicFreeQualityKeys = setOf(QQ_MUSIC_FREE_QUALITY, QQ_MUSIC_ANONYMOUS_FALLBACK_QUALITY)

fun normalizeKugouQualityKey(value: String?): String? = value
    ?.trim()
    ?.lowercase()
    ?.takeIf { it in kugouQualityKeys }

fun normalizeQQMusicQualityKey(value: String?): String? = value
    ?.trim()
    ?.uppercase()
    ?.takeIf { it in QQMUSIC_QUALITY_FALLBACK_ORDER }

/**
 * 按偏好档位生成降级尝试链（高 → 低）。
 *
 * 未登录只保留免费档，避免必然被拒的会员档请求。酷狗概念版不返回实测档位字段，
 * 只能按已知档位顺序依次尝试。
 */
fun buildKugouQualityCandidates(
    preferredQuality: String,
    isLoggedIn: Boolean
): List<String> {
    val normalizedQuality = normalizeKugouQualityKey(preferredQuality) ?: KUGOU_FREE_QUALITY
    val startIndex = KUGOU_QUALITY_FALLBACK_ORDER.indexOf(normalizedQuality)
    val chain = if (startIndex >= 0) {
        KUGOU_QUALITY_FALLBACK_ORDER.drop(startIndex)
    } else {
        KUGOU_QUALITY_FALLBACK_ORDER
    }
    return if (isLoggedIn) chain else chain.filter { it == KUGOU_FREE_QUALITY }.ifEmpty { listOf(KUGOU_FREE_QUALITY) }
}

/**
 * 按偏好档位生成降级尝试链（高 → 低）。
 *
 * 未登录时只保留匿名实测可取的档位，避免发出必然被拒（`result=104003`）的会员档请求。
 */
fun buildQQMusicQualityCandidates(
    preferredQuality: String,
    isLoggedIn: Boolean = false
): List<String> {
    val normalized = normalizeQQMusicQualityKey(preferredQuality) ?: QQ_MUSIC_FREE_QUALITY
    val startIndex = QQMUSIC_QUALITY_FALLBACK_ORDER.indexOf(normalized)
    val chain = if (startIndex >= 0) {
        QQMUSIC_QUALITY_FALLBACK_ORDER.drop(startIndex)
    } else {
        QQMUSIC_QUALITY_FALLBACK_ORDER
    }
    if (isLoggedIn) return chain
    return chain
        .filter { it in qqMusicFreeQualityKeys }
        .ifEmpty { listOf(QQ_MUSIC_FREE_QUALITY, QQ_MUSIC_ANONYMOUS_FALLBACK_QUALITY) }
}

/**
 * 由响应回填的扩展名与码率推断实际下发的档位。
 *
 * 请求无损但账号无权时服务端可能回落到 mp3，展示档位必须以实际文件为准。
 */
fun resolveKugouPlaybackQualityKey(
    extName: String?,
    bitrateKbps: Int?,
    requestedQualityKey: String
): String {
    val normalizedExtName = extName?.trim()?.lowercase().orEmpty()
    return when {
        normalizedExtName == KUGOU_LOSSLESS_QUALITY -> KUGOU_LOSSLESS_QUALITY
        normalizedExtName == "ape" || normalizedExtName == "wav" -> KUGOU_HI_RES_QUALITY
        bitrateKbps != null && bitrateKbps >= 900 -> KUGOU_LOSSLESS_QUALITY
        bitrateKbps != null && bitrateKbps >= 256 -> KUGOU_VIP_QUALITY
        bitrateKbps != null && bitrateKbps > 0 -> KUGOU_FREE_QUALITY
        else -> normalizeKugouQualityKey(requestedQualityKey) ?: KUGOU_FREE_QUALITY
    }
}
