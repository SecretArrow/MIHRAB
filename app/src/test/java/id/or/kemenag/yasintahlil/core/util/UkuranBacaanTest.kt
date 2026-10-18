package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Bawaan ukuran bacaan dahulu ditulis ulang di banyak tempat: sebelas pembacaan `Store.ARAB_SIZE`
 * (enam memakai `22f`, lima `24f`), tiga nilai awal di ViewModel (22/24/24), dan angka `+ 8`/`44f`
 * untuk bacaan jarak jauh di empat layar. Akibatnya bacaan yang sama dirender 22 sp di layar Sholat
 * tetapi 24 sp di layar Yasin, dan angka di layar Pengaturan tidak cocok dengan yang benar-benar
 * tampil — padahal kuncinya satu. Uji ini menjaga agar bawaannya satu dan tetap di dalam rentangnya.
 */
class UkuranBacaanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun aturan(): String = File(
        root(), "src/main/java/id/or/kemenag/yasintahlil/core/util/UkuranBacaan.kt",
    ).readText()

    @Test
    fun tiapBawaanBeradaDiDalamRentangnya() {
        assertTrue(UkuranBacaan.ARAB_DEFAULT in UkuranBacaan.ARAB)
        assertTrue(UkuranBacaan.LATIN_DEFAULT in UkuranBacaan.LATIN)
        assertTrue(UkuranBacaan.JARAK_DEFAULT in UkuranBacaan.JARAK)
    }

    /** Rentang terbalik akan membuat slider diam di tempat, jadi urutannya ikut dijaga. */
    @Test
    fun rentangTerurut() {
        assertTrue(UkuranBacaan.ARAB.start < UkuranBacaan.ARAB.endInclusive)
        assertTrue(UkuranBacaan.LATIN.start < UkuranBacaan.LATIN.endInclusive)
        assertTrue(UkuranBacaan.JARAK.start < UkuranBacaan.JARAK.endInclusive)
    }

    @Test
    fun bacaJauhMenambahHanyaSaatAktif() {
        assertEquals(24f, UkuranBacaan.arabBacaJauh(24f, false), 0f)
        assertEquals(24f + UkuranBacaan.TAMBAHAN_BACA_JAUH, UkuranBacaan.arabBacaJauh(24f, true), 0f)
    }

    /** Batas atas harus ada: tanpa itu ukuran maksimum (40) melompat ke 48 dan melewati kartu. */
    @Test
    fun bacaJauhTidakMelewatiBatasAtas() {
        assertEquals(UkuranBacaan.MAKS_BACA_JAUH,
            UkuranBacaan.arabBacaJauh(UkuranBacaan.ARAB.endInclusive, true), 0f)
        assertTrue("batas atas lebih kecil dari ukuran maksimum yang sah",
            UkuranBacaan.MAKS_BACA_JAUH >= UkuranBacaan.ARAB.endInclusive)
    }

    /** Bawaan dan angka jarak jauh tak boleh ditulis ulang di luar aturan bersama ini. */
    @Test
    fun tidakAdaBawaanUkuranYangDitulisUlang() {
        val pelanggar = mutableListOf<String>()
        val pola = listOf(
            Regex("""Store\.ARAB_SIZE\s*,\s*[\d.]+f"""),
            Regex("""Store\.LATIN_SIZE\s*,\s*[\d.]+f"""),
            Regex("""Store\.LINE_HEIGHT\s*,\s*[\d.]+f"""),
            // Nilai awal `remember { mutableStateOf(...) }` dan nilai awal state ViewModel pun
            // bercabang sebelum ini (22/24/24): keduanya sama-sama terlihat pengguna.
            Regex("""(arabSize|arab|latin|latinSize|lineHeight|tinggi)\s*by remember \{ mutableStateOf\([\d.]+f\)"""),
            Regex("""val (arabSize|latinSize|lineHeight)\s*:\s*Float\s*=\s*[\d.]+f"""),
            Regex("""arabSize\s*\+\s*8"""),
            Regex("""coerceAtMost\(44f\)"""),
        )
        File(root(), "src/main/java/id/or/kemenag/yasintahlil").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { f ->
                val isi = f.readText()
                pola.forEach { p -> p.findAll(isi).forEach { pelanggar += "${f.name}: ${it.value}" } }
            }
        assertTrue("Ukuran bacaan ditulis ulang di luar UkuranBacaan: $pelanggar", pelanggar.isEmpty())
    }

    /** Aturan bersama harus tetap murni: tanpa Android, agar dapat diuji langsung. */
    @Test
    fun aturanUkuranTidakMenyentuhAndroid() {
        assertFalse("UkuranBacaan mengimpor Android sehingga tak dapat diuji di JVM",
            aturan().contains("import android."))
    }
}
