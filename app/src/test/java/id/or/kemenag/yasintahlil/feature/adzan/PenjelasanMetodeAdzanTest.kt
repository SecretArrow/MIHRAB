package id.or.kemenag.yasintahlil.feature.adzan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Jadwal Sholat" meminta *"Penjelasan metode perhitungan yang sedang digunakan."*
 *
 * Penjelasan itu harus **benar**. Layar Adzan sebelumnya menulis *"Metode jadwal: Kemenag/SIHAT"*
 * secara hardcode dan menyuruh pengguna mencari koreksi serta zona waktu *di Pengaturan* —
 * padahal metode, koreksi menit, dan zona waktu diatur di menu **Jadwal**, dan pengguna bebas
 * memilih metode lain (MWL, Umm al-Qura, Egyptian, Makkah). Teks yang menyebut metode yang salah
 * lebih buruk daripada tidak menyebut sama sekali: pengguna memercayai jadwal yang bukan
 * jadwalnya.
 */
class PenjelasanMetodeAdzanTest {

    private fun sumber(relatif: String): String {
        val root = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }
        return File(root, relatif).readText()
    }

    private fun adzanScreen(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt")

    @Test
    fun metodeDibacaDariPreferensiBukanDitulisTetap() {
        val s = adzanScreen()
        assertTrue("AdzanScreen tidak membaca metode dari preferensi",
            s.contains("PrayerCalc.Method.from(store.get(Store.METHOD"))
        assertTrue("AdzanScreen tidak menampilkan nama metode yang aktif", s.contains("metode.label"))
    }

    /** Klaim lama yang bisa berbohong tidak boleh kembali. */
    @Test
    fun tidakMengklaimMetodeSecaraHardcode() {
        val s = adzanScreen()
        assertFalse("AdzanScreen kembali mengklaim metode secara hardcode",
            s.contains("Metode jadwal: Kemenag/SIHAT"))
    }

    /** Angka sudut yang ditampilkan harus datang dari definisi metode, bukan ditulis ulang. */
    @Test
    fun penjelasanMemakaiParameterMetodeYangSama() {
        val s = adzanScreen()
        assertTrue("penjelasan tidak menyebut sudut Fajr metode", s.contains("metode.fajr"))
        assertTrue("penjelasan tidak menangani metode berjeda Isya (Umm al-Qura/Makkah)",
            s.contains("metode.ishaIntervalMin"))
    }

    /** Pengguna harus diarahkan ke layar yang benar-benar memuat pengaturan itu. */
    @Test
    fun penggunaDiarahkanKeLayarJadwal() {
        val s = adzanScreen()
        assertTrue("AdzanScreen tidak mengarahkan ke menu Jadwal", s.contains("di menu Jadwal"))
        assertFalse("AdzanScreen menyuruh mencari koreksi/zona waktu di Pengaturan",
            s.contains("koreksi & zona waktu di Pengaturan"))
    }

    /**
     * Notifikasi adzan juga menyebut metode, dan ia muncul justru saat pengguna paling mungkin
     * memercayainya. Klaim hardcode di sana sama menyesatkannya dengan di layar.
     */
    @Test
    fun notifikasiAdzanTidakMengklaimMetodeTetap() {
        val s = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ReminderWorker.kt")
        assertFalse("notifikasi adzan mengklaim metode secara hardcode",
            s.contains("Jadwal dihitung di perangkat (metode Kemenag/SIHAT)."))
        assertTrue("notifikasi adzan tidak membaca metode dari preferensi",
            s.contains("PrayerCalc.Method.from(store.get(Store.METHOD"))
    }
}
