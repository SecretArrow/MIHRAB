package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Ejaan angka waktu hidup di satu tempat — [FormatWaktu] — bukan ditulis ulang di tiap pemakainya.
 *
 * `"%02d:%02d"` dulu muncul tiga kali: di `PrayerCalc.compute` (menyusun waktu sholat), di
 * `JadwalHariIni.geser` (menggesernya), dan di layar Iqomah (menghitung mundur). Dua yang pertama
 * menghasilkan **jam** yang sama, jadi salinan yang berbeda berarti jam yang dihitung dan jam yang
 * digeser disebut dengan tulisan berbeda — mis. `%d:%02d` membuat "05:07" menjadi "5:07" di satu
 * tempat saja. Yang ketiga menghasilkan **durasi**, sehingga menyalin ejaannya begitu saja
 * menyembunyikan bahwa angkanya berarti lain.
 *
 * Uji ini menjaga dua hal: (1) hanya satu format string waktu di seluruh kode, dan (2) kedua
 * maknanya tetap bernama beda, supaya "01:30" tak pernah diam-diam berarti dua hal.
 */
class FormatWaktuSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun sumber(relatif: String): String =
        kode(File(root(), "src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    // ---- Satu ejaan -----------------------------------------------------------------------------

    /**
     * Hanya satu tempat yang boleh memuat format string waktunya. Format yang ditulis di dua tempat
     * dapat menyimpang tanpa ketahuan — dan menyimpangnya hanya terlihat oleh pengguna.
     */
    @Test
    fun formatWaktuHanyaDitulisDiSatuBerkas() {
        val pemakai = berkasMain()
            .filter { kode(it).contains("\"%02d:%02d\"") }
            .map { it.name }.sorted()
        assertEquals("format \"%02d:%02d\" ditulis di lebih dari satu berkas",
            listOf("FormatWaktu.kt"), pemakai)
    }

    /** Ketiga pemakai lama harus benar-benar memakai aturan bersama, bukan sekadar tak mengejanya. */
    @Test
    fun pemakaiLamaMemakaiAturanBersama() {
        assertTrue("PrayerCalc tidak memakai FormatWaktu.jamMenit saat menyusun waktu",
            sumber("core/util/PrayerCalc.kt").contains("FormatWaktu.jamMenit("))
        assertTrue("JadwalHariIni.geser tidak memakai FormatWaktu.jamMenit",
            sumber("core/util/JadwalHariIni.kt").contains("FormatWaktu.jamMenit("))
        assertTrue("layar Iqomah tidak memakai FormatWaktu.menitDetik untuk hitungan mundurnya",
            sumber("feature/adzan/IqomahDoaScreen.kt").contains("FormatWaktu.menitDetik("))
    }

    // ---- Dua makna ------------------------------------------------------------------------------

    /** Jam:menit adalah kebalikan `toMinutes` untuk **seluruh** hari, termasuk dua digit nol di depan. */
    @Test
    fun jamMenitKebalikanToMinutes() {
        (0 until PrayerCalc.MENIT_SEHARI).forEach { m ->
            val jam = FormatWaktu.jamMenit(m)
            assertEquals("jamMenit($m) tidak dapat dibaca kembali", m, PrayerCalc.toMinutes(jam))
            assertEquals("jamMenit($m) tidak dua digit di kedua sisi", 5, jam.length)
        }
        assertEquals("00:00", FormatWaktu.jamMenit(0))
        assertEquals("05:07", FormatWaktu.jamMenit(307))
        assertEquals("23:59", FormatWaktu.jamMenit(1439))
    }

    /** `geser` dan `jamMenit` harus sepakat — keduanya menyebut jam yang sama dengan ejaan yang sama. */
    @Test
    fun geserSepakatDenganJamMenit() {
        listOf("00:05" to -10, "23:55" to 10, "04:35" to 0,
            "12:00" to -PrayerCalc.MENIT_SEHARI, "00:00" to PrayerCalc.MENIT_SEHARI).forEach { (jam, d) ->
            // Lipatannya dari `PrayerCalc.putaranMenit`, bukan disalin ke sini.
            val total = PrayerCalc.putaranMenit(PrayerCalc.toMinutes(jam) + d)
            assertEquals("geser(\"$jam\", $d) tidak sejalan dengan FormatWaktu.jamMenit",
                FormatWaktu.jamMenit(total), JadwalHariIni.geser(jam, d))
        }
    }

    /**
     * Durasi dan jam **berbeda nama** meski ejaannya sama: `menitDetik(90)` berarti satu menit tiga
     * puluh detik, bukan pukul satu lewat tiga puluh menit. Nama yang sama akan menyembunyikan itu.
     */
    @Test
    fun durasiDanJamBernamaBeda() {
        assertEquals("01:30", FormatWaktu.menitDetik(90))
        assertEquals("00:00", FormatWaktu.menitDetik(0))
        assertEquals("00:59", FormatWaktu.menitDetik(59))
        // Ejaannya memang satu: yang membedakan hanya nama dan artinya.
        assertEquals(FormatWaktu.pasanganAngka(90 / 60, 90 % 60), FormatWaktu.menitDetik(90))
        assertEquals(FormatWaktu.pasanganAngka(90 / 60, 90 % 60), FormatWaktu.jamMenit(90))
    }

    /** Kedua angka selalu dua digit, termasuk saat nilainya nol. */
    @Test
    fun pasanganAngkaSelaluDuaDigit() {
        assertEquals("00:00", FormatWaktu.pasanganAngka(0, 0))
        assertEquals("09:05", FormatWaktu.pasanganAngka(9, 5))
        assertEquals("12:34", FormatWaktu.pasanganAngka(12, 34))
    }

    /**
     * Lipatan melewati tengah malam adalah urusan **pemanggil** ([JadwalHariIni.geser]), bukan
     * ejaannya. Bila [FormatWaktu.jamMenit] ikut melipat, nilai yang sudah dilipat pemanggil akan
     * dilipat dua kali — dan "24:00" yang sah bagi pemanggil menjadi "00:00" yang salah.
     */
    @Test
    fun jamMenitHanyaMengejaTidakMelipat() {
        assertEquals("24:00", FormatWaktu.jamMenit(PrayerCalc.MENIT_SEHARI))
        assertEquals("25:07", FormatWaktu.jamMenit(PrayerCalc.MENIT_SEHARI + 67))
    }
}
