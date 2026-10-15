package id.or.kemenag.yasintahlil

import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pengingat imsak — beberapa menit sebelum Subuh.
 *
 * Fondasinya (kunci `Store.IMSAK_ON`/`IMSAK_MIN`, nilai bawaan, penjepit menit) sudah diletakkan
 * lebih dulu, tetapi tak satu pun kode memakainya: pipelinenya putus di tengah — kunci
 * dideklarasikan (dan ikut dipulihkan dari backup) sementara penjadwal, pekerja, notifikasi, dan
 * kendalinya belum ada. Perbaikan yang dijaga uji ini menyambungkannya ujung ke ujung.
 *
 * Tiga hal yang tidak boleh melonggar:
 *
 * - **Rantainya utuh**: penjadwal benar-benar mengantrekan pekerja imsak (hanya untuk Subuh, pada
 *   jam yang digeser dari jadwal Subuh dengan aturan bersama), pekerja menangani jenisnya dengan
 *   membaca ulang saklar dan menitnya, dan kendalinya hidup di tab Pengaturan.
 * - **Sifat pilihannya (opt-in)**: imsak hanya bermakna bagi yang berpuasa, jadi bawaannya mati
 *   dan saklarnya hanya boleh hidup di satu tempat (satu pengaturan, satu rumah).
 * - **Kejujurannya**: imsak di sini diturunkan dari jadwal Subuh yang dihitung di perangkat,
 *   bukan dari imsakiyah resmi — kalimatnya satu ([PengingatAdzan.IMSAK_PERKIRAAN]) untuk
 *   notifikasi dan Pengaturan, bukan dua versi yang dapat berbeda.
 */
class PengingatImsakTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val penjadwal: String
        get() = sumber("src/main/java/id/or/kemenag/yasintahlil/core/scheduling/PrayerScheduler.kt")

    private val pekerja: String
        get() = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ReminderWorker.kt")

    private val pengaturan: String
        get() = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt")

    /**
     * Kendali imsak hidup di tab Pengaturan, satu keluarga dengan pengingat iqomah — bukan di menu
     * Adzan (hub saklar per waktu) dan bukan di layar lain. Tanpa penjaga ini saklarnya mudah
     * menempel di layar itu sebagai salinan kedua.
     */
    @Test
    fun saklarPengingatImsakDitulisDariPengaturan() {
        assertTrue("Pengaturan tidak menyediakan saklar pengingat imsak",
            pengaturan.contains("Store.IMSAK_ON"))
        assertTrue("Pengaturan tidak menyediakan saklar imsak dengan namanya",
            pengaturan.contains("\"Ingatkan imsak sebelum Subuh\""))
        // Saklar dan penjadwalan ulangnya satu baris yang sama — pola iqomah; baris yang berpisah
        // berarti menyimpan tanpa menjadwalkan ulang (atau sebaliknya).
        assertTrue("mengubah saklar imsak di Pengaturan tidak menjadwalkan ulang pengingat",
            pengaturan.lines().filter { it.contains("store.set(Store.IMSAK_ON") }
                .any { it.contains("PrayerScheduler.scheduleAll") })

        val adzan = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt")
        assertFalse("saklar imsak menempel di menu Adzan — rumahnya di Pengaturan",
            adzan.contains("Store.IMSAK_ON"))
    }

    /** Menit imsak: ditulis dari Pengaturan, terjepit lewat aturan bersama, dan ikut dijadwalkan ulang. */
    @Test
    fun menitImsakDitulisDariPengaturanTerjepit() {
        assertTrue("Pengaturan tidak menyediakan pengatur menit imsak",
            pengaturan.contains("\"Menit imsak sebelum Subuh\""))
        // Blok kendalinya saja — kata yang sama dapat muncul di keterangan bagian jadwal dengan
        // makna naratif, bukan kendali.
        val blok = pengaturan.substringAfter("\"Menit imsak sebelum Subuh\"")
            .substringBefore("// Koreksi menit per waktu")
        assertTrue("Pengaturan tidak menyimpan menit imsak ke Store.IMSAK_MIN",
            blok.contains("store.set(Store.IMSAK_MIN"))
        assertTrue("tombol menit imsak tidak menjepit lewat aturan bersama",
            blok.contains("BawaanPreferensi.menitImsak"))
        assertTrue("mengubah menit imsak tidak menjadwalkan ulang pengingatnya",
            blok.contains("PrayerScheduler.scheduleAll"))
    }

    /** Penjadwal mengantrekan imsak hanya untuk Subuh, pada jam yang digeser dari jadwal Subuh. */
    @Test
    fun penjadwalMengantrekanImsakHanyaUntukSubuh() {
        assertTrue("penjadwal tidak mengenal jenis pekerjaan imsak",
            penjadwal.contains("JENIS_IMSAK"))
        assertTrue("penjadwal tidak membaca saklar pengingat imsak",
            penjadwal.contains("Store.IMSAK_ON"))
        assertTrue("penjadwal tidak membaca menit imsak",
            penjadwal.contains("Store.IMSAK_MIN"))
        // Subuh ditandai dari daftar waktu bersama, bukan diketik sendiri — salinan yang dapat
        // menunjuk waktu lain membuat imsak berbunyi pada menit yang salah.
        assertTrue("penjadwal tidak menandai Subuh dari daftar waktu bersama",
            penjadwal.contains("val NAMA_SUBUH = NAMES.first()"))

        val badan = penjadwal.substringAfter("private fun enqueueNext")
            .substringBefore("private fun kunciHari")
        assertTrue("imsak tidak dibatasi untuk Subuh",
            badan.contains("cfg.imsakOn && nama == NAMA_SUBUH"))
        // Jam imsaknya digeser dari jadwal Subuh dengan aturan yang sama dengan pra-adzan dan
        // jam yang ditampilkan menu Adzan, lalu jedanya dihitung dari jam itu.
        assertTrue("jam imsak tidak memakai aturan geser bersama",
            badan.contains("JadwalHariIni.geser("))
        assertTrue("jeda imsak tidak dihitung dari jam yang digeser",
            badan.contains("JadwalHariIni.sisaMenit("))
    }

    /**
     * Kunci pekerjaan imsak bertanggal. Pekerja imsak memang tidak menjadwalkan ulang apa pun,
     * tetapi pekerja adzan Subuh menjadwalkan ulang dirinya sesudah berbunyi — dan penjadwalan
     * ulang itu ikut mengantrekan imsak esok hari. Dengan nama tetap, `ExistingWorkPolicy.REPLACE`
     * membatalkan pekerjaan imsak yang masih menggantung (regresi yang sama dengan doa/iqomah).
     */
    @Test
    fun pekerjaanImsakMemakaiKunciBertanggal() {
        val badan = penjadwal.substringAfter("private fun enqueueNext")
            .substringBefore("private fun kunciHari")
        val imsak = badan.lines().filter { it.contains("enqueue(ctx,") && it.contains("JENIS_IMSAK") }
        assertTrue("penjadwal tidak mengantrekan pekerjaan imsak: $imsak", imsak.size == 1)
        assertTrue("pekerjaan imsak memakai nama tetap sehingga penjadwalan ulang adzan Subuh " +
            "membatalkannya: $imsak", imsak.all { it.contains("kunciHari") })
    }

    /**
     * Pekerja membaca ulang saklar dan menitnya saat berjalan (keduanya dapat berubah setelah
     * penjadwalan), tidak ikut memutar adzan, dan notifikasinya jujur: kanal yang benar, menuju
     * menu Jadwal, menyebut jumlah menitnya, dan memakai keterangan bersama.
     */
    @Test
    fun pekerjaMenanganiImsakTanpaMenjadwalkanUlang() {
        assertTrue("pekerja tidak menangani jenis imsak", pekerja.contains("JENIS_IMSAK"))
        val cabang = pekerja.substringAfter("if (jenis == PrayerScheduler.JENIS_IMSAK)")
            .substringBefore("val uri =")
        assertTrue("pekerja imsak tidak memeriksa saklarnya saat berjalan — saklar dapat dimatikan " +
            "setelah penjadwalan", cabang.contains("store.get(Store.IMSAK_ON"))
        assertTrue("pekerja imsak tidak membaca menit lewat penjepit bersama",
            cabang.contains("BawaanPreferensi.menitImsak"))
        assertTrue("pekerja imsak tidak membunyikan notifikasinya",
            cabang.contains("notifyImsak("))
        // Imsak terjadi sebelum adzan Subuh; rantai hariannya cukup dijaga pekerja adzan Subuh.
        // Pekerja imsak yang ikut menjadwalkan ulang berarti dua penjaga rantai untuk satu hari.
        assertFalse("pekerja imsak menjadwalkan ulang sesuatu — rantai harian Subuh dijaga " +
            "pekerja adzannya", cabang.contains("rescheduleOne"))
        // Posisi cabang juga penting: ia harus kembali sebelum jalur adzan yang memutar audio —
        // bila diletakkan di bawahnya, imsak malah membunyikan adzan Subuh dua kali.
        assertTrue("cabang imsak tidak mengembalikan hasil sebelum jalur adzan",
            cabang.contains("return Result.success()"))
    }

    /** Bentuk notifikasi imsak: kanal, tujuan ketukan, judul, keterangan, dan id-nya sendiri. */
    @Test
    fun notifikasiImsakMenujuMenuJadwal() {
        val badan = pekerja.substringAfter("private fun notifyImsak")
            .substringBefore("private fun post")
        assertTrue("notifikasi imsak tidak memakai kanal sebelum/sesudah adzan",
            badan.contains("NotifChannels.PRE"))
        assertTrue("ketukan notifikasi imsak tidak membuka menu Jadwal",
            badan.contains("Routes.JADWAL"))
        assertTrue("notifikasi imsak tidak menyebut jumlah menitnya",
            badan.contains("Imsak sekitar \$menit menit lagi"))
        assertTrue("notifikasi imsak tidak memakai keterangan bersama",
            badan.contains("PengingatAdzan.IMSAK_PERKIRAAN"))
        assertTrue("notifikasi imsak tidak memakai id sendiri — dapat menabrak notifikasi jenis " +
            "lain", badan.contains("5000 + \"Subuh\".hashCode()"))
    }

    /** Menit imsak terjepit ke rentang sahnya; bawaannya berada di dalam rentang itu. */
    @Test
    fun menitImsakDijepitKeRentangSahnya() {
        assertEquals(BawaanPreferensi.RENTANG_MENIT_IMSAK.first, BawaanPreferensi.menitImsak(0))
        assertEquals(BawaanPreferensi.RENTANG_MENIT_IMSAK.first, BawaanPreferensi.menitImsak(-7))
        assertEquals(BawaanPreferensi.RENTANG_MENIT_IMSAK.last, BawaanPreferensi.menitImsak(31))
        assertEquals(BawaanPreferensi.RENTANG_MENIT_IMSAK.last, BawaanPreferensi.menitImsak(9_999))
        assertEquals(10, BawaanPreferensi.menitImsak(10))
        assertEquals("bawaan menit imsak harus tak berubah oleh penjepitnya sendiri",
            BawaanPreferensi.MENIT_IMSAK, BawaanPreferensi.menitImsak(BawaanPreferensi.MENIT_IMSAK))
        assertTrue("bawaan menit imsak harus berada di dalam rentangnya",
            BawaanPreferensi.MENIT_IMSAK in BawaanPreferensi.RENTANG_MENIT_IMSAK)
    }

    /**
     * Imsak opt-in: hanya bermakna bagi yang berpuasa, jadi tidak menyala sendiri — pengingat yang
     * mengejutkan pengguna non-puasa di tengah malam adalah gangguan, bukan bantuan.
     */
    @Test
    fun imsakTidakMenyalaSendiri() {
        assertFalse("pengingat imsak menyala sendiri tanpa diminta",
            BawaanPreferensi.PENGINGAT_IMSAK)
    }

    /** Nama kunci tersimpan dibekukan: mengubahnya membuat pengaturan lama tak terbaca. */
    @Test
    fun kunciPengingatImsakDideklarasikan() {
        val store = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt")
        assertTrue("Store.IMSAK_ON tidak dideklarasikan dengan nama tersimpannya",
            store.contains("val IMSAK_ON = booleanPreferencesKey(\"imsak_on\")"))
        assertTrue("Store.IMSAK_MIN tidak dideklarasikan dengan nama tersimpannya",
            store.contains("val IMSAK_MIN = intPreferencesKey(\"imsak_min\")"))
    }

    /**
     * Keterangan batas tanggung jawab imsak hanya boleh hidup di [PengingatAdzan] — satu kalimat
     * untuk notifikasi dan Pengaturan, seperti [PengingatAdzan.IQOMAH_PERKIRAAN] untuk iqomah.
     * Salinan yang dieja sedikit berbeda justru yang paling sering terjadi, sehingga yang dicek
     * adalah potongan khasnya, bukan kalimat utuhnya.
     */
    @Test
    fun keteranganImsakSatuRumahKalimat() {
        assertEquals("Perkiraan imsak dihitung dari jadwal Subuh di perangkat.",
            PengingatAdzan.IMSAK_PERKIRAAN)
        val penyalin = File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "PengingatAdzan.kt" }
            .filter { f -> KodeSumber.kodeTanpaKomentar(f.readText()).contains("Perkiraan imsak") }
            .map { it.name }.toList()
        assertTrue("keterangan imsak disalin ke luar `PengingatAdzan`: $penyalin", penyalin.isEmpty())
        // Kedua pemakainya benar-benar membaca konstantanya, bukan mengarang versi sendiri.
        assertTrue("notifikasi imsak tidak memakai keterangan bersama",
            pekerja.contains("PengingatAdzan.IMSAK_PERKIRAAN"))
        assertTrue("Pengaturan tidak memakai keterangan bersama",
            pengaturan.contains("PengingatAdzan.IMSAK_PERKIRAAN"))
    }
}
