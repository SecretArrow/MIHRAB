package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` (Doa Sebelum dan Sesudah Adzan) meminta tombol untuk *"mengaktifkan pengingat membaca
 * doa setelah adzan"*, dan pengingat itu harus mengantar ke bacaannya — bukan sekadar berbunyi lalu
 * membuka beranda.
 *
 * Uji ini menjaga rantainya tetap utuh: preferensi tersimpan, penjadwal benar-benar mengantrekan
 * pengingat kedua, pekerja notifikasi menunjuk layar Doa Adzan, dan rute yang boleh dibuka dari
 * notifikasi terbatas pada rute tanpa argumen (rute berargumen akan membuat aplikasi berhenti).
 */
class PengingatDoaAdzanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val routes: String
        get() = sumber("src/main/java/id/or/kemenag/yasintahlil/navigation/Routes.kt")

    @Test
    fun hanyaRuteTanpaArgumenYangBolehDibukaDariNotifikasi() {
        val blok = routes.substringAfter("val LANGSUNG: Set<String> = setOf(").substringBefore(")")
        val semua = Regex("""const val (\w+) = """").findAll(routes).map { it.groupValues[1] }.toList()
        assertTrue("rute tak terbaca", semua.size >= 15)

        val kurang = semua.filterNot { it == "SHOLAT_DETAIL" || blok.contains(it) }
        assertTrue("rute tidak boleh dibuka dari notifikasi: $kurang", kurang.isEmpty())
        assertFalse("rute berargumen tidak boleh ikut", blok.contains("SHOLAT_DETAIL"))
    }

    @Test
    fun saklarPengingatAdaDiLayarAdzanDanMenjadwalkanUlang() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt")
        assertTrue("saklar pengingat doa tidak ada di layar Adzan", layar.contains("DOA_ADZAN_REMINDER"))
        assertTrue("mengubah saklar tidak menjadwalkan ulang pengingat",
            layar.contains("PrayerScheduler.scheduleAll"))
    }

    /**
     * Saklar yang sama harus tersedia di layar Doa Adzan — di situlah orang membacanya, jadi
     * mengaktifkannya tidak boleh menuntut berpindah ke menu Adzan lebih dulu. Keduanya menulis
     * kunci preferensi yang sama supaya tidak lahir dua sumber kebenaran.
     */
    @Test
    fun saklarPengingatJugaAdaDiLayarDoaAdzan() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/IqomahDoaScreen.kt")
        val doa = layar.substringAfter("fun DoaAdzanScreen(").substringBefore("private fun KartuDoa(")
        assertTrue("layar Doa Adzan tidak punya saklar pengingat", doa.contains("DOA_ADZAN_REMINDER"))
        assertTrue("mengubah saklar di layar Doa Adzan tidak menjadwalkan ulang pengingat",
            doa.contains("PrayerScheduler.scheduleAll"))
        assertTrue("saklar di layar Doa Adzan tidak dipulihkan dari preferensi",
            doa.contains("store.get(Store.DOA_ADZAN_REMINDER"))
    }

    @Test
    fun penjadwalMengantrekanPengingatKeduaSetelahWaktuMasuk() {
        val penjadwal = sumber("src/main/java/id/or/kemenag/yasintahlil/core/scheduling/PrayerScheduler.kt")
        assertTrue("penjadwal tidak mengenal jenis pekerjaan doa", penjadwal.contains("JENIS_DOA"))
        assertTrue("penjadwal tidak membaca preferensi pengingat doa",
            penjadwal.contains("Store.DOA_ADZAN_REMINDER"))
        // Jeda dari aturan bersama (`PengingatAdzan`), bukan angka yang diingat penjadwal sendiri:
        // penjelasan di layar Adzan dan Iqomah menyebut angka yang sama, jadi keduanya harus
        // membacanya dari tempat yang sama.
        assertTrue("jeda pengingat doa tidak memakai konstanta bersama",
            penjadwal.contains("PengingatAdzan.DOA_DELAY_MIN"))
    }

    @Test
    fun notifikasiPengingatMenujuLayarDoaAdzan() {
        val pekerja = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ReminderWorker.kt")
        assertTrue("pekerja tidak menangani jenis doa", pekerja.contains("JENIS_DOA"))
        assertTrue("notifikasi pengingat tidak menunjuk layar Doa Adzan",
            pekerja.contains("Routes.DOA_ADZAN"))
        assertTrue("pekerja tidak meneruskan rute lewat intent",
            pekerja.contains("MainActivity.EXTRA_ROUTE"))
    }

    /** Nama kunci tersimpan dibekukan: mengubahnya membuat pengaturan lama tak terbaca. */
    @Test
    fun kunciPengingatDoaAdzanDideklarasikan() {
        val store = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt")
        assertTrue("Store.DOA_ADZAN_REMINDER tidak dideklarasikan dengan nama tersimpannya",
            store.contains("val DOA_ADZAN_REMINDER = booleanPreferencesKey(\"doa_adzan_reminder\")"))
    }
}
