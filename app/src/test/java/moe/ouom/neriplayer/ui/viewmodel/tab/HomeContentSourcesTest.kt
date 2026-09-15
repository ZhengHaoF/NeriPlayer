package moe.ouom.neriplayer.ui.viewmodel.tab

import moe.ouom.neriplayer.core.api.kugou.KugouChannelContent
import moe.ouom.neriplayer.core.api.kugou.KugouRankMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeContentSourcesTest {

    // ---- parseHomeContentSourceOrder ----

    @Test
    fun parse_defaultRaw_yieldsNeteaseThenKugou() {
        assertEquals(
            listOf(HomeContentSource.NETEASE, HomeContentSource.KUGOU),
            parseHomeContentSourceOrder(DEFAULT_HOME_CONTENT_SOURCE_ORDER_RAW)
        )
    }

    @Test
    fun parse_unknownIdsAndWhitespace_areIgnored() {
        assertEquals(
            listOf(HomeContentSource.NETEASE, HomeContentSource.KUGOU),
            parseHomeContentSourceOrder("netease, kugou, unknown,,")
        )
    }

    @Test
    fun parse_reversedOrder_isKept() {
        assertEquals(
            listOf(HomeContentSource.KUGOU, HomeContentSource.NETEASE),
            parseHomeContentSourceOrder("kugou,netease")
        )
    }

    @Test
    fun parse_duplicates_areDeduplicated() {
        assertEquals(
            listOf(HomeContentSource.NETEASE, HomeContentSource.KUGOU),
            parseHomeContentSourceOrder("netease,kugou,netease")
        )
    }

    @Test
    fun parse_emptyOrInvalid_fallsBackToDefault() {
        assertEquals(DEFAULT_HOME_CONTENT_SOURCE_ORDER, parseHomeContentSourceOrder(""))
        assertEquals(DEFAULT_HOME_CONTENT_SOURCE_ORDER, parseHomeContentSourceOrder("   "))
        assertEquals(DEFAULT_HOME_CONTENT_SOURCE_ORDER, parseHomeContentSourceOrder("bogus"))
    }

    @Test
    fun parse_singleSource_isAllowed() {
        assertEquals(
            listOf(HomeContentSource.KUGOU),
            parseHomeContentSourceOrder("kugou")
        )
    }

    // ---- formatHomeContentSourceOrder ----

    @Test
    fun format_roundTripsWithParse() {
        val order = listOf(HomeContentSource.KUGOU, HomeContentSource.NETEASE)
        assertEquals(order, parseHomeContentSourceOrder(formatHomeContentSourceOrder(order)))
    }

    // ---- kugouContentHasData ----

    @Test
    fun kugouHasData_emptyContent_isFalse() {
        assertFalse(kugouContentHasData(KugouChannelContent()))
    }

    @Test
    fun kugouHasData_ranksOnly_isTrue() {
        val content = KugouChannelContent(
            ranks = listOf(
                KugouRankMeta(
                    rankId = "8888",
                    rankName = "飙升榜",
                    coverUrl = null,
                    playCount = 1L,
                    intro = null
                )
            )
        )
        assertTrue(kugouContentHasData(content))
    }

    @Test
    fun kugouHasData_emptyDailyRecommend_isFalse() {
        val content = KugouChannelContent(
            dailyRecommend = emptyList()
        )
        assertFalse(kugouContentHasData(content))
    }

    @Test
    fun kugouHasData_dailyRecommendOnly_isTrue() {
        val song = moe.ouom.neriplayer.data.model.SongItem(
            id = 1L,
            name = "测试",
            artist = "歌手",
            album = "",
            albumId = 0L,
            durationMs = 0L,
            coverUrl = null,
            channelId = "kugou"
        )
        val content = KugouChannelContent(
            dailyRecommend = listOf(song)
        )
        assertTrue(kugouContentHasData(content))
    }

    // ---- neteaseHomeHasData ----

    @Test
    fun neteaseHasData_emptyState_isFalse() {
        assertFalse(neteaseHomeHasData(HomeUiState()))
    }

    @Test
    fun neteaseHasData_radarFallbackPlaylists_isFalse() {
        // 硬编码雷达兜底不应视为有数据，否则永远不会降级到下一个源
        val fallback = HomeSectionState(
            items = NeteaseRadarPlaylistDefinitions.map { it.toPlaylistSummary() }
        )
        assertFalse(
            neteaseHomeHasData(
                HomeUiState(
                    radarPlaylists = fallback,
                    radarPlaylistsFromFallback = true
                )
            )
        )
    }

    @Test
    fun neteaseHasData_realRadarPlaylists_isTrue() {
        val real = HomeSectionState(
            items = listOf(
                PlaylistSummary(
                    id = 1L,
                    name = "时光雷达",
                    picUrl = "https://example.com/cover.jpg",
                    playCount = 10L,
                    trackCount = 10
                )
            )
        )
        assertTrue(
            neteaseHomeHasData(
                HomeUiState(
                    radarPlaylists = real,
                    radarPlaylistsFromFallback = false
                )
            )
        )
    }

    @Test
    fun neteaseHasData_playlistSectionWithItems_isTrue() {
        val state = HomeUiState(
            playlistSections = listOf(
                HomeNeteasePlaylistSectionState(
                    source = NeteaseHomePlaylistSource.PERSONALIZED,
                    section = HomeSectionState(
                        items = listOf(
                            PlaylistSummary(
                                id = 1L,
                                name = "测试歌单",
                                picUrl = "",
                                playCount = 0L,
                                trackCount = 0
                            )
                        )
                    )
                )
            )
        )
        assertTrue(neteaseHomeHasData(state))
    }

    @Test
    fun neteaseHasData_allSectionsEmptyButError_isFalse() {
        val state = HomeUiState(
            playlistSections = listOf(
                HomeNeteasePlaylistSectionState(
                    source = NeteaseHomePlaylistSource.PERSONALIZED,
                    section = HomeSectionState(error = "boom")
                )
            ),
            trendingSongSections = listOf(
                HomeNeteaseSongSectionState(
                    source = NeteaseHomeSongSource.TOP_HOT,
                    section = HomeSectionState(error = "boom")
                )
            ),
            radarSongSections = listOf(
                HomeNeteaseSongSectionState(
                    source = NeteaseHomeSongSource.PERSONAL_RADAR,
                    section = HomeSectionState(error = "boom")
                )
            )
        )
        assertFalse(neteaseHomeHasData(state))
    }
}
