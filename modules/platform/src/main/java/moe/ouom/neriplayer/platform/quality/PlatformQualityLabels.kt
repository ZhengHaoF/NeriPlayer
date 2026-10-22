package moe.ouom.neriplayer.platform.quality

import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.model.playback.KUGOU_FREE_QUALITY
import moe.ouom.neriplayer.data.model.playback.KUGOU_HI_RES_QUALITY
import moe.ouom.neriplayer.data.model.playback.KUGOU_LOSSLESS_QUALITY
import moe.ouom.neriplayer.data.model.playback.KUGOU_QUALITY_FALLBACK_ORDER
import moe.ouom.neriplayer.data.model.playback.KUGOU_VIPER_TAPE_QUALITY
import moe.ouom.neriplayer.data.model.playback.KUGOU_VIP_QUALITY
import moe.ouom.neriplayer.data.model.playback.PlaybackQualityOption
import moe.ouom.neriplayer.data.model.playback.QQ_MUSIC_ANONYMOUS_FALLBACK_QUALITY
import moe.ouom.neriplayer.data.model.playback.QQ_MUSIC_FREE_QUALITY
import moe.ouom.neriplayer.data.model.playback.QQMUSIC_QUALITY_FALLBACK_ORDER

/**
 * 酷狗概念版 / QQ 音乐的档位展示名。
 *
 * 放在 :platform 是因为只有这一层同时看得到 :model 的档位类型和 :common 的字符串资源，
 * 而 app 的平台客户端与 :playback:runtime 的播放链路都要用同一份标签，
 * 任何一侧自己实现一份都会漂移。
 */
fun qualityLabelForKugou(key: String, getLocalizedString: (Int) -> String): String = when (key) {
    KUGOU_FREE_QUALITY -> getLocalizedString(CoreCommonR.string.quality_standard)
    KUGOU_VIP_QUALITY -> getLocalizedString(CoreCommonR.string.settings_audio_quality_high)
    KUGOU_LOSSLESS_QUALITY -> getLocalizedString(CoreCommonR.string.quality_lossless)
    KUGOU_HI_RES_QUALITY -> getLocalizedString(CoreCommonR.string.quality_hires)
    KUGOU_VIPER_TAPE_QUALITY -> getLocalizedString(CoreCommonR.string.quality_viper_tape)
    else -> key
}

fun buildKugouQualityOptions(getLocalizedString: (Int) -> String): List<PlaybackQualityOption> =
    KUGOU_QUALITY_FALLBACK_ORDER
        .asReversed()
        .map { PlaybackQualityOption(it, qualityLabelForKugou(it, getLocalizedString)) }

fun qqMusicQualityLabel(key: String, getLocalizedString: (Int) -> String): String =
    when (key.uppercase()) {
        QQ_MUSIC_FREE_QUALITY, QQ_MUSIC_ANONYMOUS_FALLBACK_QUALITY, "C200" ->
            getLocalizedString(CoreCommonR.string.quality_standard)
        "C600", "O600" -> getLocalizedString(CoreCommonR.string.settings_audio_quality_higher)
        "M800", "O800" -> getLocalizedString(CoreCommonR.string.quality_very_high)
        "F000", "AI00", "Q000" -> getLocalizedString(CoreCommonR.string.quality_lossless)
        "O400", "Q001", "Q003", "D004", "DT03", "TL01" -> getLocalizedString(CoreCommonR.string.quality_hires)
        else -> key
    }

fun buildQQMusicQualityOptions(getLocalizedString: (Int) -> String): List<PlaybackQualityOption> =
    QQMUSIC_QUALITY_FALLBACK_ORDER
        .asReversed()
        .map { PlaybackQualityOption(it, qqMusicQualityLabel(it, getLocalizedString)) }
