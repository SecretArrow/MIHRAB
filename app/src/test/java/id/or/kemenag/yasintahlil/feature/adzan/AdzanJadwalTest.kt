package id.or.kemenag.yasintahlil.feature.adzan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Fitur Adzan" membuka daftar fitur wajibnya dengan *"Jadwal adzan untuk Subuh,
 * Zuhur, Asar, Magrib, dan Isya"*. Layar Adzan dahulu hanya menampilkan **saklar** per waktu
 * beserta namanya: untuk mengetahui jam adzan, pengguna harus membuka menu Jadwal, dan layar yang
 * mengatur kapan adzan berbunyi tidak dapat menjawab "jam berapa".
 *
 * Sekaligus menjaga agar aturan hisabnya **tidak disalin ulang**: `parseCorr`, pemetaan mazhab
 * Asar, dan pemilihan zona waktu dahulu identik di tiga berkas (`PrayerScheduler`,
 * `JadwalViewModel`, `HomeViewModel`). Bila salah satunya diubah tanpa yang lain, pengingat akan
 * berbunyi pada menit yang tidak sama dengan yang ditampilkan layar — jadi salinan barunya ditolak.
 */
class AdzanJadwalTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val layarRel = "src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt"
    private val aturanRel = "src/main/java/id/or/kemenag/yasintahlil/core/util/JadwalHariIni.kt"

    // ---- Layar menampilkan jadwal ----

    @Test
    fun layarAdzanMembacaParameterHisabDariPenyimpanan() {
        val layar = sumber(layarRel)
        listOf("Store.LAT", "Store.LON", "Store.TZ", "Store.CORR", "Store.ASAR").forEach { kunci ->
            assertTrue("layar Adzan tidak membaca $kunci untuk menghitung jadwal",
                layar.contains(kunci))
        }
    }

    @Test
    fun layarAdzanMenampilkanJamAdzanDanPengingatnya() {
        val layar = sumber(layarRel)
        assertTrue("layar Adzan tidak menghitung jadwal lima waktu",
            layar.contains("JadwalHariIni.adzan("))
        assertTrue("jam adzan tidak ditampilkan per waktu",
            layar.contains("\"Adzan \${jam[i]}\""))
        // Pengingat sebelum adzan harus tampil pada jamnya sendiri, bukan hanya sebagai angka menit.
        assertTrue("jam pengingat sebelum adzan tidak ditampilkan",
            layar.contains("JadwalHariIni.geser(jam[i], -preMin)"))
    }

    @Test
    fun layarAdzanMemakaiAturanHisabYangSamaDenganPenjadwal() {
        val layar = sumber(layarRel)
        assertTrue("layar Adzan menyusun parameter hisab sendiri, bukan lewat aturan bersama",
            layar.contains("JadwalHariIni.params("))
        assertTrue("mazhab Asar ditentukan ulang di layar, bukan lewat aturan bersama",
            layar.contains("JadwalHariIni.asarHanafi(asar)"))
    }

    // ---- Aturan tunggal (tidak disalin ulang) ----

    @Test
    fun tidakAdaSalinanParseCorrLagi() {
        val berkas = listOf(
            "src/main/java/id/or/kemenag/yasintahlil/core/scheduling/PrayerScheduler.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/jadwal/JadwalViewModel.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/home/HomeViewModel.kt",
        )
        berkas.forEach { rel ->
            assertFalse("$rel masih menyalin parseCorr; pakai JadwalHariIni.parseCorr",
                sumber(rel).contains("private fun parseCorr"))
        }
    }

    @Test
    fun tidakAdaPemetaanMazhabAsarYangDitulisUlang() {
        listOf(
            "src/main/java/id/or/kemenag/yasintahlil/core/scheduling/PrayerScheduler.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/jadwal/JadwalViewModel.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/home/HomeScreen.kt",
        ).forEach { rel ->
            assertFalse("$rel masih memetakan mazhab Asar sendiri; pakai JadwalHariIni",
                sumber(rel).contains("== \"hanafi\") 2.0 else 1.0"))
        }
    }

    @Test
    fun penjadwalPengingatMemakaiAturanBersama() {
        val penjadwal = sumber("src/main/java/id/or/kemenag/yasintahlil/core/scheduling/PrayerScheduler.kt")
        assertTrue("penjadwal pengingat tidak memakai aturan hisab bersama",
            penjadwal.contains("JadwalHariIni.params("))
        assertTrue("penjadwal pengingat tidak memakai zona waktu bersama",
            penjadwal.contains("JadwalHariIni.zona("))
        assertTrue("penjadwal pengingat tidak memakai daftar nama waktu bersama",
            penjadwal.contains("JadwalHariIni.LIMA_WAKTU"))
    }

    /**
     * Urutan nama waktu adalah arti indeks CSV `Store.ADZAN_ON` dan `Store.CORR`. Daftar lokal di
     * layar Adzan, layar Jadwal, atau beranda adalah salinan yang dapat berbeda urutan dari yang
     * dibaca penjadwal — jadi hanya aturan bersama yang boleh menyediakannya.
     */
    @Test
    fun tidakAdaDaftarNamaWaktuYangDitulisUlang() {
        listOf(
            "src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/jadwal/JadwalScreen.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/home/HomeScreen.kt",
        ).forEach { rel ->
            assertFalse("$rel menulis ulang daftar nama waktu; pakai JadwalHariIni",
                sumber(rel).contains("listOf(\"Subuh\""))
        }
    }

    /**
     * `Store.ADZAN_ON` dan `Store.IQOMAH_MIN` diurai dan dijepit di lebih dari satu tempat: layar
     * Adzan menulis CSV-nya, penjadwal membacanya, pekerja latar memakai panjang iqomahnya, layar
     * Iqomah menampilkan angkanya, dan layar Pengaturan (rumah baru tombol − / + durasi iqomah)
     * menulisnya. Salinan aturan di tempat-tempat itu berarti layar dapat menampilkan angka yang
     * tidak pernah dipakai notifikasi.
     */
    @Test
    fun tidakAdaSalinanAturanPengingatLagi() {
        listOf(
            "src/main/java/id/or/kemenag/yasintahlil/core/scheduling/PrayerScheduler.kt",
            "src/main/java/id/or/kemenag/yasintahlil/core/data/ReminderWorker.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/adzan/IqomahDoaScreen.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt",
        ).forEach { rel ->
            val isi = sumber(rel)
            // Baris yang memuat kunci ADZAN_ON sekaligus split(",") berarti CSV-nya diurai di situ,
            // bukan lewat aturan bersama.
            isi.lines().forEach { baris ->
                assertFalse("$rel masih mengurai CSV ADZAN_ON sendiri; pakai PengingatAdzan.parseOn",
                    baris.contains("ADZAN_ON") && baris.contains("split"))
            }
            assertFalse("$rel masih menjepit menit iqomah sendiri; pakai PengingatAdzan.iqomahMenit",
                isi.contains("coerceIn(1, 60)") || isi.contains("coerceAtMost(60)"))
        }
    }

    /** Aturan bersama harus tetap murni: tanpa Android, agar dapat diuji langsung. */
    @Test
    fun aturanBersamaTidakMenyentuhAndroid() {
        val aturan = sumber(aturanRel)
        assertFalse("JadwalHariIni mengimpor Android sehingga tak dapat diuji di JVM",
            aturan.contains("import android."))
    }
}
