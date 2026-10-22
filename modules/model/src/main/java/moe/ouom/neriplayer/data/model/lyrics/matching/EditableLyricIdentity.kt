package moe.ouom.neriplayer.data.model.lyrics.matching

/**
 * 歌曲在来源平台上的原生标识。
 *
 * [EditableLyricsMatcher] 默认按关键词搜索再打分匹配；当调用方已经握有平台 ID 时，
 * 各来源可以直接按身份取词，省掉一次搜索往返，也避免匹配到同名不同曲。
 *
 * 只覆盖已知平台 ID 的三个来源：AMLL / LrcLib / YouTube Music 没有可直取的原生
 * 曲目身份，因此这里不为它们提供分支。
 */
sealed interface EditableLyricIdentity {
    val source: EditableLyricMatchSource

    /** 身份是否可用；不可用时调用方应回退到关键词搜索。 */
    val isUsable: Boolean

    data class Kugou(
        val audioHash: String,
        val songId: String?
    ) : EditableLyricIdentity {
        override val source: EditableLyricMatchSource get() = EditableLyricMatchSource.KUGOU
        override val isUsable: Boolean get() = audioHash.isNotBlank()
    }

    data class QqMusic(
        val songMid: String
    ) : EditableLyricIdentity {
        override val source: EditableLyricMatchSource get() = EditableLyricMatchSource.QQ_MUSIC
        override val isUsable: Boolean get() = songMid.isNotBlank()
    }

    data class CloudMusic(
        val songId: Long
    ) : EditableLyricIdentity {
        override val source: EditableLyricMatchSource get() = EditableLyricMatchSource.CLOUD_MUSIC
        override val isUsable: Boolean get() = songId > 0L
    }
}
