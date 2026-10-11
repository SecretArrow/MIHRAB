package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentValidatorTest {

    @Test
    fun officialSourcesAccepted() {
        assertTrue(ContentValidator.isOfficialSource("https://quran.kemenag.go.id/"))
        assertTrue(ContentValidator.isOfficialSource("https://sihat.kemenag.dev/arah-kiblat"))
        assertTrue(ContentValidator.isOfficialSource("https://sunnah.com/bukhari:756"))
        assertTrue(ContentValidator.isOfficialSource("https://hilal.bmkg.go.id/"))
        assertTrue(ContentValidator.isOfficialSource("https://nu.or.id/"))
    }

    @Test
    fun unofficialRejected() {
        assertFalse(ContentValidator.isOfficialSource("https://random-blog.example/ayat"))
        assertFalse(ContentValidator.isOfficialSource(""))
        assertFalse(ContentValidator.isOfficialSource("bukan-url"))
    }

    /** Anti-spoofing: domain resmi sebagai substring di host asing harus ditolak. */
    @Test
    fun spoofedHostsRejected() {
        assertFalse(ContentValidator.isOfficialSource("https://evil-kemenag.go.id.attacker.com/"))
        assertFalse(ContentValidator.isOfficialSource("https://kemenag.go.id.attacker.com/"))
        assertFalse(ContentValidator.isOfficialSource("https://notsunnah.com/"))
        assertFalse(ContentValidator.isOfficialSource("https://sunnah.com.evil.net/"))
        assertFalse(ContentValidator.isOfficialSource("https://xkemenag.go.id/"))
    }

    @Test
    fun hostExtraction() {
        assertEquals("quran.kemenag.go.id", ContentValidator.hostOf("https://quran.kemenag.go.id/x?y=1"))
        assertNull(ContentValidator.hostOf("bukan-url"))
    }
}
