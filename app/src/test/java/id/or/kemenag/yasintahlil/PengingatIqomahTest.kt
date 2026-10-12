package id.or.kemenag.yasintahlil

import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Fitur Iqomah": *"Tambahkan pengatur waktu Iqomah opsional setelah pengingat masuk
 * waktu sholat. Fitur ini hanya berfungsi sebagai pengingat pribadi dan tidak boleh menganggap waktu
 * Iqomah sama di semua masjid."*
 *
 * Dua hal yang dijaga di sini. Pertama, timer iqomah harus benar-benar **tertambat** pada pengingat
 * masuk waktu — bukan hanya berjalan selama layarnya terbuka, karena justru saat layar tertutuplah
 * pengingat ini berguna. Kedua, kalimatnya tidak boleh mengklaim waktu masjid mana pun: waktu iqomah
 * berbeda di tiap masjid, jadi notifikasi yang berbunyi "waktu iqomah telah habis" akan berbohong.
 */
class PengingatIqomahTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val layar: String
        get() = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/IqomahDoaScreen.kt")

    private val penjadwal: String
        get() = sumber("src/main/java/id/or/kemenag/yasintahlil/core/scheduling/PrayerScheduler.kt")

    private val pekerja: String
        get() = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ReminderWorker.kt")

    @Test
    fun saklarPengingatIqomahAdaDiLayarDanMenjadwalkanUlang() {
        assertTrue("saklar pengingat iqomah tidak ada di layar Iqomah",
            layar.contains("Store.IQOMAH_ON"))
        assertTrue("mengubah saklar tidak menjadwalkan ulang pengingat",
            layar.contains("PrayerScheduler.scheduleAll"))
        assertTrue("panjang timer tidak memakai kunci yang sama dengan pengingat",
            layar.contains("Store.IQOMAH_MIN"))
    }

    @Test
    fun penjadwalMengantrekanPengingatIqomahSetelahWaktuMasuk() {
        assertTrue("penjadwal tidak mengenal jenis pekerjaan iqomah",
            penjadwal.contains("JENIS_IQOMAH"))
        assertTrue("penjadwal tidak membaca saklar pengingat iqomah",
            penjadwal.contains("Store.IQOMAH_ON"))
        assertTrue("jeda pengingat iqomah tidak memakai panjang timer pengguna",
            penjadwal.contains("Store.IQOMAH_MIN"))
    }

    @Test
    fun notifikasiIqomahMenujuLayarIqomah() {
        assertTrue("pekerja tidak menangani jenis iqomah", pekerja.contains("JENIS_IQOMAH"))
        assertTrue("notifikasi iqomah tidak menunjuk layar Iqomah",
            pekerja.contains("Routes.IQOMAH"))
        assertTrue("pekerja tidak meneruskan rute lewat intent",
            pekerja.contains("MainActivity.EXTRA_ROUTE"))
    }

    /** Waktu iqomah tiap masjid berbeda; kalimatnya harus mengaku sebagai perkiraan. */
    @Test
    fun notifikasiTidakMengklaimWaktuMasjidTertentu() {
        val badan = pekerja.substringAfter("private fun notifyIqomah")
            .substringBefore("private fun post")
        assertTrue("notifikasi iqomah tidak menyebut perkiraan", badan.contains("Perkiraan"))
        // Kalimat batas tanggung jawabnya kini **satu** dengan yang dibaca layar Iqomah, bukan
        // versi pendek yang hanya hidup di notifikasi ini. Ejaannya sendiri dipaku di
        // `PengingatAdzanTest`; yang dijaga di sini adalah notifikasi ini benar-benar membacanya
        // dari kosakata bersama, sehingga layar dan notifikasi tidak dapat berbeda lagi.
        assertTrue("notifikasi iqomah tidak memakai keterangan bersama",
            badan.contains("PengingatAdzan.IQOMAH_PERKIRAAN"))
        assertFalse("notifikasi iqomah menyebut waktu masjid seolah pasti",
            badan.contains("telah habis"))
        assertTrue("keterangan bersama tidak lagi mengingatkan bahwa waktu masjid berbeda",
            PengingatAdzan.IQOMAH_PERKIRAAN.contains("tiap masjid"))
    }

    /**
     * Regresi: pekerjaan yang berbunyi sesudah waktu masuk tidak boleh memakai nama tetap.
     *
     * Pekerja adzan, begitu berbunyi, menjadwalkan ulang dirinya untuk kemunculan berikutnya —
     * dan penjadwalan ulang itu juga mengantrekan pekerjaan susulan. Dengan nama tetap,
     * `ExistingWorkPolicy.REPLACE` membatalkan pekerjaan susulan hari itu yang tinggal beberapa
     * menit lagi berbunyi, sehingga pengingat doa dan iqomah tak pernah terdengar sama sekali.
     */
    @Test
    fun susulanSesudahWaktuMasukMemakaiKunciBertanggal() {
        val badan = penjadwal.substringAfter("private fun enqueueNext")
            .substringBefore("private fun kunciHari")
        val susulan = badan.lines().filter {
            it.contains("enqueue(ctx,") && (it.contains("JENIS_DOA") || it.contains("JENIS_IQOMAH"))
        }
        assertTrue("penjadwal tidak mengantrekan pekerjaan susulan: $susulan", susulan.size >= 2)
        val tanpaTanggal = susulan.filterNot { it.contains("\$hari") }
        assertTrue("pekerjaan susulan memakai nama tetap sehingga penjadwalan ulang adzan " +
            "membatalkannya: $tanpaTanggal", tanpaTanggal.isEmpty())
        assertTrue("pembeda tanggal tidak dihitung dari zona waktu pengguna",
            penjadwal.contains("fun kunciHari"))
    }

    /** Nama kunci tersimpan dibekukan: mengubahnya membuat pengaturan lama tak terbaca. */
    @Test
    fun kunciPengingatIqomahDideklarasikan() {
        val store = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt")
        assertTrue("Store.IQOMAH_ON tidak dideklarasikan dengan nama tersimpannya",
            store.contains("val IQOMAH_ON = booleanPreferencesKey(\"iqomah_on\")"))
    }
}
