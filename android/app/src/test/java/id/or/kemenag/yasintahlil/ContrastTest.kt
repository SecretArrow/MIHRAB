package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validasi rasio kontras WCAG 2.1 untuk pasangan warna tema yang benar-benar dipakai
 * sebagai teks/latar. Ambang: 4.5:1 untuk teks normal (AA), 3:1 untuk elemen besar/non-teks.
 */
class ContrastTest {

    /** Luminans relatif WCAG dari ARGB. */
    private fun lum(argb: Int): Double {
        fun ch(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        val r = ch((argb shr 16) and 0xFF)
        val g = ch((argb shr 8) and 0xFF)
        val b = ch(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun ratio(a: Int, b: Int): Double {
        val la = lum(a)
        val lb = lum(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private val putih = 0xFFFFFFFF.toInt()

    @Test
    fun temaTerangKontrasTeksCukup() {
        assertTrue("putih/Zamrud = ${ratio(putih, Palette.ZAMRUD)}",
            ratio(putih, Palette.ZAMRUD) >= 4.5)
        assertTrue("Zamrud/Gading = ${ratio(Palette.ZAMRUD, Palette.GADING)}",
            ratio(Palette.ZAMRUD, Palette.GADING) >= 4.5)
        // Emas = warna teks Latin di kartu (latar terang) — harus lolos ambang teks normal.
        assertTrue("Emas/Gading = ${ratio(Palette.EMAS, Palette.GADING)}",
            ratio(Palette.EMAS, Palette.GADING) >= 4.5)
    }

    @Test
    fun temaGelapKontrasTeksCukup() {
        assertTrue("ZamrudTerang/latarGelap = ${ratio(Palette.ZAMRUD_TERANG, Palette.DARK_BG)}",
            ratio(Palette.ZAMRUD_TERANG, Palette.DARK_BG) >= 4.5)
        assertTrue("onPrimary/ZamrudTerang = ${ratio(Palette.ON_ZAMRUD_TERANG, Palette.ZAMRUD_TERANG)}",
            ratio(Palette.ON_ZAMRUD_TERANG, Palette.ZAMRUD_TERANG) >= 4.5)
        assertTrue("EmasTerang/latarGelap = ${ratio(Palette.EMAS_TERANG, Palette.DARK_BG)}",
            ratio(Palette.EMAS_TERANG, Palette.DARK_BG) >= 4.5)
    }

    /**
     * "Tema halaman" (`plan.txt`) memakai kertas gading dan kertas gelap. Keduanya harus tetap
     * memenuhi ambang teks normal: tinta isi bacaan, aksen nomor/jumlah ulangan (primary), dan
     * warna teks Latin (secondary) — termasuk emas yang lebih gelap khusus kertas krem.
     */
    @Test
    fun temaHalamanKontrasTeksCukup() {
        assertTrue("TintaKrem/KertasKrem = ${ratio(Palette.TINTA_KREM, Palette.KERTAS_KREM)}",
            ratio(Palette.TINTA_KREM, Palette.KERTAS_KREM) >= 4.5)
        assertTrue("Zamrud/KertasKrem = ${ratio(Palette.ZAMRUD, Palette.KERTAS_KREM)}",
            ratio(Palette.ZAMRUD, Palette.KERTAS_KREM) >= 4.5)
        assertTrue("EmasKertas/KertasKrem = ${ratio(Palette.EMAS_KERTAS, Palette.KERTAS_KREM)}",
            ratio(Palette.EMAS_KERTAS, Palette.KERTAS_KREM) >= 4.5)

        assertTrue("TintaGelap/KertasGelap = ${ratio(Palette.TINTA_GELAP, Palette.KERTAS_GELAP)}",
            ratio(Palette.TINTA_GELAP, Palette.KERTAS_GELAP) >= 4.5)
        assertTrue("ZamrudTerang/KertasGelap = ${ratio(Palette.ZAMRUD_TERANG, Palette.KERTAS_GELAP)}",
            ratio(Palette.ZAMRUD_TERANG, Palette.KERTAS_GELAP) >= 4.5)
        assertTrue("EmasTerang/KertasGelap = ${ratio(Palette.EMAS_TERANG, Palette.KERTAS_GELAP)}",
            ratio(Palette.EMAS_TERANG, Palette.KERTAS_GELAP) >= 4.5)
    }
}
