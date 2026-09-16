package moe.ouom.neriplayer.ui.screen.tab

import moe.ouom.neriplayer.ui.viewmodel.tab.SearchSource
import org.junit.Assert.assertEquals
import org.junit.Test

class TabOrderingTest {

    // ---------- 探索页 ----------

    @Test
    fun `explore blank raw falls back to default order`() {
        val resolved = resolveExploreTabOrder(
            raw = "",
            isInternational = false,
            youtubeEnabled = true
        )

        assertEquals(
            exploreSearchSourceDisplayOrder(false, true),
            resolved
        )
    }

    @Test
    fun `explore custom order is respected`() {
        val resolved = resolveExploreTabOrder(
            raw = "qq_music,netease",
            isInternational = false,
            youtubeEnabled = false
        )

        assertEquals(
            listOf(SearchSource.QQ_MUSIC, SearchSource.NETEASE),
            resolved
        )
    }

    @Test
    fun `explore custom order filters disabled sources`() {
        val resolved = resolveExploreTabOrder(
            raw = "youtube_music,netease,kugou",
            isInternational = false,
            youtubeEnabled = false
        )

        assertEquals(
            listOf(SearchSource.NETEASE, SearchSource.KUGOU),
            resolved
        )
    }

    @Test
    fun `explore unknown ids are dropped and empty result falls back`() {
        assertEquals(
            listOf(SearchSource.NETEASE),
            resolveExploreTabOrder("netease", false, false)
        )
        assertEquals(
            exploreSearchSourceDisplayOrder(false, false),
            resolveExploreTabOrder("unknown_platform,,", false, false)
        )
    }

    @Test
    fun `explore order survives parse and format round trip`() {
        val order = listOf(SearchSource.KUGOU, SearchSource.LINK_RECOGNITION)
        val raw = formatExploreTabOrder(order)

        assertEquals(order, parseExploreTabOrder(raw))
    }

    // ---------- 媒体库 ----------

    @Test
    fun `library blank raw falls back to default order`() {
        val resolved = resolveLibraryTabOrder(
            raw = "",
            isInternational = false,
            youtubeEnabled = true
        )

        assertEquals(
            libraryTabDisplayOrder(false, true),
            resolved
        )
    }

    @Test
    fun `library local is always first and visible`() {
        val resolved = resolveLibraryTabOrder(
            raw = "kugou,qq_music",
            isInternational = false,
            youtubeEnabled = false
        )

        assertEquals(
            listOf(LibraryTab.LOCAL, LibraryTab.KUGOU, LibraryTab.QQMUSIC),
            resolved
        )
    }

    @Test
    fun `library local stored in middle is pulled back to front`() {
        val resolved = resolveLibraryTabOrder(
            raw = "netease,local,bilibili",
            isInternational = false,
            youtubeEnabled = false
        )

        assertEquals(LibraryTab.LOCAL, resolved.first())
        assertEquals(listOf(LibraryTab.NETEASE, LibraryTab.BILI), resolved.drop(1))
    }

    @Test
    fun `library all others hidden keeps local only`() {
        val resolved = resolveLibraryTabOrder(
            raw = "local",
            isInternational = true,
            youtubeEnabled = true
        )

        assertEquals(listOf(LibraryTab.LOCAL), resolved)
    }

    @Test
    fun `library custom order filters disabled sources`() {
        val resolved = resolveLibraryTabOrder(
            raw = "youtube_music,netease",
            isInternational = false,
            youtubeEnabled = false
        )

        assertEquals(
            listOf(LibraryTab.LOCAL, LibraryTab.NETEASE),
            resolved
        )
    }

    @Test
    fun `library order survives parse and format round trip`() {
        val order = listOf(LibraryTab.LOCAL, LibraryTab.FAVORITE, LibraryTab.KUGOU)
        val raw = formatLibraryTabOrder(order)

        assertEquals(order, parseLibraryTabOrder(raw))
    }
}
