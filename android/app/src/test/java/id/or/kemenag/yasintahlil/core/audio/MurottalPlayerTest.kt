package id.or.kemenag.yasintahlil.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uji JVM untuk pola URL murottal ([MurottalSumber]).
 *
 * [MurottalSumber] sengaja object murni — tanpa `Context` dan tanpa Media3 — sehingga polanya
 * teruji di JVM tanpa perangkat; kelas pemutarnya ([MurottalPlayer]) tidak bisa, karena
 * ExoPlayer hanya hidup di perangkat. Yang dipaku bukan sekadar isi string: nomor surah tiga
 * digit (`036`), padding tiga digit pada nomor ayat, dan digit Latin dari `Locale.US`.
 * Kesalahan kecil pada pola ini membuat setiap ayat menunjuk berkas yang tidak pernah ada di
 * server, dan kegagalannya baru terasa setelah aplikasi terpasang di tangan pengguna.
 */
class MurottalPlayerTest {

    @Test
    fun urlAyatPertama() {
        assertEquals("https://everyayah.com/data/Alafasy_128kbps/036001.mp3", MurottalSumber.url(1))
    }

    @Test
    fun urlAyatTerakhir() {
        assertEquals("https://everyayah.com/data/Alafasy_128kbps/036083.mp3", MurottalSumber.url(83))
    }

    /** Padding tiga digit: ayat 10 harus `036010`, bukan `03610` — nama berkas server berpola begitu. */
    @Test
    fun urlAyatSepuluhBerpaddingTigaDigit() {
        assertEquals("https://everyayah.com/data/Alafasy_128kbps/036010.mp3", MurottalSumber.url(10))
    }

    /**
     * `Locale.US`: seluruh URL ayat 1..83 hanya boleh memuat ASCII. Pada lokal lain `%d` dapat
     * menghasilkan digit non-ASCII (mis. digit Arab-Indik), dan URL berisi digit seperti itu
     * menunjuk berkas yang tidak pernah ada — tanpa satu pun galat yang menyebut sebabnya.
     */
    @Test
    fun urlSelaluDigitLatin() {
        (1..83).forEach { n ->
            val u = MurottalSumber.url(n)
            assertTrue("URL ayat $n memuat karakter non-ASCII: $u", u.all { it.code < 128 })
            assertTrue(
                "URL ayat $n tidak berakhiran 036 + nomor tiga digit: $u",
                Regex("036\\d{3}\\.mp3$").containsMatchIn(u),
            )
        }
    }
}
