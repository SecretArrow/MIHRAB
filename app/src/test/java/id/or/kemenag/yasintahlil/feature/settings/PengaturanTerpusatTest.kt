package id.or.kemenag.yasintahlil.feature.settings

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Permintaan pengguna, disampaikan dua kali: *"segala sesuatu pengaturan ya tempatnya jadikan
 * satu saja di Pengaturan biar gampang"* — dan khusus tombolnya: *"pindahkan + dan - itu di
 * pengaturan saja dari seluruh yang ada"*.
 *
 * Yang sudah dipindah ke sini lewat perbaikan ini:
 *
 * - **Koreksi menit per waktu** — dahulu deretan − / + di layar Jadwal; layar itu kini cukup
 *   membaca `Store.CORR` dan menampilkan hasilnya.
 * - **Durasi iqomah** — dahulu tombol − / + di layar Iqomah; layar itu kini cukup membaca
 *   `Store.IQOMAH_MIN` (sama seperti notifikasi latarnya).
 * - **Penghitung ulangan** (Tahlil dan Dzikir) — bukan dipindah begitu saja, sebab menghitung
 *   tasbih memang terjadi di layar bakunya; ia jadi **opt-in**: mati secara bawaan dan
 *   dinyalakan dari sini (`Store.SHOW_COUNTER`), sehingga layar bacaan bersih secara bawaan.
 * - **Saklar pengingat waktu sholat** — dahulu kartu saklar di beranda; beranda kini murni
 *   dasbor, dan saklarnya hidup di sini bersama kendali lainnya.
 *
 * Uji ini menjaga agar keempatnya tidak kembali berpencar, dan agar tidak ada tombol teks
 * "−"/"+" baru yang lahir di layar lain.
 */
class PengaturanTerpusatTest {

    private fun berkasMain(): List<File> =
        File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun kode(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(
            KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    /**
     * Tombol teks "−" dan "+" hanya boleh hidup di layar Pengaturan — satu rumah bagi seluruh
     * kendali pengaturan. Ikon (mis. tambah/kurang pada penghitung) tidak dihitung: yang dilarang
     * pengguna adalah deretan tombol teks, bukan ikon kecil yang menjadi bagian kendali layarnya.
     */
    @Test
    fun tombolTeksMinusPlusHanyaDiLayarPengaturan() {
        val pelanggar = berkasMain()
            .filter { it.name != "SettingsScreen.kt" }
            .filter { f -> KodeSumber.literalString(f.readText()).any { it == "−" || it == "+" } }
            .map { it.name }
        assertTrue("tombol teks − / + di luar layar Pengaturan: $pelanggar", pelanggar.isEmpty())
    }

    /** Koreksi menit per waktu: kendalinya di Pengaturan, hasilnya dibaca layar Jadwal. */
    @Test
    fun koreksiMenitPerWaktuKendalinyaDiPengaturan() {
        val pengaturan = kode("feature/settings/SettingsScreen.kt")
        assertTrue("Pengaturan tidak menyediakan koreksi menit per waktu",
            pengaturan.contains("Koreksi menit per waktu"))
        assertTrue("Pengaturan tidak menyimpan koreksi menit ke Store.CORR",
            pengaturan.contains("store.set(Store.CORR"))

        val jadwal = kode("feature/jadwal/JadwalScreen.kt")
        assertFalse("layar Jadwal masih membawa kendali koreksi menit — pindahkan ke Pengaturan",
            jadwal.contains("Koreksi menit per waktu") || jadwal.contains("setCorrection"))
        // ViewModel Jadwal tetap membaca Store.CORR: jam yang tercetak di sana harus memakai
        // koreksi yang dipilih pengguna di Pengaturan, sama seperti pengingatnya.
        val vm = kode("feature/jadwal/JadwalViewModel.kt")
        assertTrue("JadwalViewModel tidak lagi membaca koreksi menit — jadwalnya berbohong",
            vm.contains("store.get(Store.CORR"))
    }

    /** Durasi iqomah: ditulis dari Pengaturan, hanya dibaca oleh layar Iqomah. */
    @Test
    fun durasiIqomahDitulisDariPengaturan() {
        val pengaturan = kode("feature/settings/SettingsScreen.kt")
        assertTrue("Pengaturan tidak menyediakan durasi iqomah",
            pengaturan.contains("Durasi iqomah"))
        assertTrue("Pengaturan tidak menyimpan durasi iqomah ke Store.IQOMAH_MIN",
            pengaturan.contains("store.set(Store.IQOMAH_MIN"))

        val iqomah = kode("feature/adzan/IqomahDoaScreen.kt")
        assertTrue("layar Iqomah tidak lagi membaca durasi timer dari preferensi",
            iqomah.contains("store.get(Store.IQOMAH_MIN"))
        assertFalse("layar Iqomah masih menulis durasi — kendalinya milik Pengaturan",
            iqomah.contains("store.set(Store.IQOMAH_MIN"))
    }

    /**
     * Penghitung ulangan di Tahlil dan Dzikir hanya tampil bila dinyalakan dari Pengaturan.
     * Tanpa penjaga ini, penghitung mudah kembali menempel permanen — dan bersamanya deretan
     * tombol − / + yang baru saja dipindahkan.
     */
    @Test
    fun penghitungUlanganOptInDariPengaturan() {
        val pengaturan = kode("feature/settings/SettingsScreen.kt")
        assertTrue("Pengaturan tidak menyediakan saklar penghitung ulangan",
            pengaturan.contains("Penghitung ulangan"))
        assertTrue("Pengaturan tidak menyimpan pilihan penghitung ke Store.SHOW_COUNTER",
            pengaturan.contains("store.set(Store.SHOW_COUNTER"))

        listOf(
            "feature/tahlil/TahlilScreen.kt",
            "feature/dzikir/DzikirGroupScreen.kt",
        ).forEach { rel ->
            val layar = kode(rel)
            assertTrue("$rel tidak lagi mengikuti preferensi penghitung ulangan",
                layar.contains("Store.SHOW_COUNTER"))
            assertTrue("$rel kehilangan penghitungnya sama sekali",
                layar.contains("CounterRow("))
        }
    }

    /**
     * Keterangan susunan Tahlil (nama, catatan tradisi, sumber hingga sunnah.com) tidak boleh
     * kembali menumpuk sebagai kartu di atas layar Tahlil — rumahnya kini layar keterangan.
     */
    @Test
    fun keteranganSusunanTahlilTidakMenumpukDiLayar() {
        val layar = kode("feature/tahlil/TahlilScreen.kt")
        assertFalse("layar Tahlil kembali menumpuk keterangan susunan di badannya",
            layar.contains("KartuKeterangan"))
        assertTrue("layar Tahlil kehilangan pintu keterangannya",
            layar.contains("Routes.keterangan(\"tahlil\")"))
    }

    /**
     * Saklar pengingat waktu sholat tidak boleh kembali ke beranda: beranda adalah dasbor, bukan
     * lemari pengaturan — kartu saklar di sana mengulangi tab Pengaturan dan memakan tempat yang
     * lebih berguna. Pemusatan ini tidak boleh berubah menjadi penghilangan: saklarnya harus tetap
     * hidup di Pengaturan beserta penjadwalan ulangnya.
     */
    @Test
    fun saklarPengaturanTidakKembaliKeBeranda() {
        val home = kode("feature/home/HomeScreen.kt")
        assertFalse("beranda kembali membawa saklar pengingat waktu sholat",
            home.contains("Switch(") || home.contains("setNotif"))
        assertFalse("ViewModel beranda kembali membawa penulisan preferensi pengingat",
            kode("feature/home/HomeViewModel.kt").contains("store.set(Store.NOTIF"))

        val pengaturan = kode("feature/settings/SettingsScreen.kt")
        assertTrue("Pengaturan kehilangan saklar pengingat waktu sholat",
            pengaturan.contains("PengingatAdzan.NAMA_PENGINGAT_WAKTU"))
        assertTrue("saklar pengingat di Pengaturan tidak lagi menyimpan ke Store.NOTIF",
            pengaturan.contains("store.set(Store.NOTIF"))
        assertTrue("saklar pengingat di Pengaturan tidak lagi menjadwalkan ulang pengingatnya",
            pengaturan.contains("PrayerScheduler.scheduleAll"))
    }
}
