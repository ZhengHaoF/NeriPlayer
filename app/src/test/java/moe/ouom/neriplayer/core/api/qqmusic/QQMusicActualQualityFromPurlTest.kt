package moe.ouom.neriplayer.core.api.qqmusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QQMusicActualQualityFromPurlTest {

    @Test
    fun `parses matching quality prefix and extension`() {
        assertEquals("M500", actualQualityFromPurl("M500002tCAn43sbfuz.mp3?guid=1&vkey=abc"))
        assertEquals("C400", actualQualityFromPurl("C400002tCAn43sbfuz.m4a?guid=1&vkey=abc"))
        assertEquals("F000", actualQualityFromPurl("F000001abc.flac?guid=1"))
    }

    @Test
    fun `rejects mismatched extension`() {
        assertNull(actualQualityFromPurl("M500001abc.m4a?x=1"))
        assertNull(actualQualityFromPurl("C400001abc.mp3?x=1"))
    }

    @Test
    fun `rejects short or malformed names`() {
        assertNull(actualQualityFromPurl("M500.mp3"))
        assertNull(actualQualityFromPurl("mp3"))
        assertNull(actualQualityFromPurl(""))
    }
}
