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

    /**
     * Sumber tingkat berkas punya dua ejaan yang diterima skema. Pembacanya harus mengenal
     * keduanya: pemeriksa bundel (`BundelKonten`) dan gerbang CI memang mengenal `sumberUrl`,
     * sehingga bundel dengan ejaan itu lolos pemasangan dan dilaporkan *"terpasang"* — tetapi
     * `ContentRepo` dahulu hanya membaca `sumber_url`, menolak setiap berkasnya saat dibuka, dan
     * aplikasi diam-diam kembali ke konten bawaan. Pembaruan yang tampak berhasil tanpa mengubah
     * apa pun, tanpa satu pun pesan yang menyebut sebabnya.
     */
    @Test
    fun sumberBerkasMenerimaKeduaEjaan() {
        assertEquals("https://sunnah.com/muslim:591",
            ContentValidator.sumberBerkas(org.json.JSONObject("""{"sumber_url":"https://sunnah.com/muslim:591"}""")))
        assertEquals("https://sunnah.com/muslim:591",
            ContentValidator.sumberBerkas(org.json.JSONObject("""{"sumberUrl":"https://sunnah.com/muslim:591"}""")))
    }

    @Test
    fun sumberBerkasKosongBilaTidakMenyatakanSumber() {
        assertEquals("", ContentValidator.sumberBerkas(org.json.JSONObject("""{"judul":"x"}""")))
        assertEquals("", ContentValidator.sumberBerkas(org.json.JSONObject("""{"sumber_url":"   "}""")))
    }
}
