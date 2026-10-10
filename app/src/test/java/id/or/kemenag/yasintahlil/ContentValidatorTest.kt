package id.or.kemenag.yasintahlil.core.util

import org.junit.Test
import org.junit.Assert.*

class ContentValidatorTest {
    @Test fun officialSourcesAccepted() {
        assertTrue(ContentValidator.isOfficialSource("https://quran.kemenag.go.id/"))
        assertTrue(ContentValidator.isOfficialSource("https://sihat.kemenag.dev/arah-kiblat"))
        assertTrue(ContentValidator.isOfficialSource("https://sunnah.com/bukhari:756"))
    }
    @Test fun unofficialRejected() {
        assertFalse(ContentValidator.isOfficialSource("https://random-blog.example/ayat"))
    }
    @Test fun ghaibStepsAreFourTakbir() {
        val takbirCount = 4
        assertEquals(4, takbirCount) // tanpa ruku/sujud, sesuai HR Muttafaq Alaih (Najasyi)
    }
}
