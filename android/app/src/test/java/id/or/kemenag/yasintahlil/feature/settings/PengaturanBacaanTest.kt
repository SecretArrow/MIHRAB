package id.or.kemenag.yasintahlil.feature.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Pengaturan Aplikasi" meminta pengaturan *"Ukuran teks Arab dan Latin"*, dan
 * §"Fitur Surat Yasin" meminta *"Pengaturan jarak antarbaris dan tema halaman"*.
 *
 * Keempat preferensi bacaan itu adalah preferensi **global** yang dipakai setiap kartu bacaan
 * (lihat `UkuranBacaanGlobalTest`). Sebelumnya layar Pengaturan hanya menyediakan "Ukuran Arab" dan
 * "Ukuran Latin"; "jarak antarbaris" dan "tema halaman" hanya ada di bottom sheet pengaturan bacaan,
 * sehingga pengguna yang mencarinya di Pengaturan tidak menemukannya. Uji ini menjaga agar keempatnya
 * tetap ada di satu tempat dan menulis kunci preferensi yang **sama** — bukan salinan kedua.
 */
class PengaturanBacaanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun layar(): String = File(
        root(), "src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt",
    ).readText()

    @Test
    fun pengaturanMenyediakanKeempatPreferensiBacaan() {
        val s = layar()
        assertTrue("Pengaturan tidak menyediakan ukuran Arab", s.contains("Store.ARAB_SIZE"))
        assertTrue("Pengaturan tidak menyediakan ukuran Latin", s.contains("Store.LATIN_SIZE"))
        assertTrue("Pengaturan tidak menyediakan jarak antarbaris", s.contains("Store.LINE_HEIGHT"))
        assertTrue("Pengaturan tidak menyediakan tema halaman", s.contains("Store.PAGE_THEME"))
    }

    /** Rentangnya dipakai bersama sheet — angka tetap di sini akan menyimpang dari sheet. */
    @Test
    fun rentangJarakAntarbarisDipakaiBersama() {
        val s = layar()
        assertTrue("Pengaturan menuliskan rentang jarak antarbaris sendiri, bukan yang bersama",
            s.contains("UkuranBacaan.JARAK"))
    }

    /** Tema halaman ditawarkan lewat daftar pilihan yang sah, bukan chip tetap. */
    @Test
    fun temaHalamanDitawarkanLewatDaftarSah() {
        val s = layar()
        assertTrue("Pengaturan tidak memakai daftar tema halaman yang sah",
            s.contains("TemaHalaman.SEMUA"))
        assertTrue("Pengaturan tidak memakai label tema halaman bersama",
            s.contains("TemaHalaman.label("))
    }

    /** Label durasi/desimal jarak antarbaris ditampilkan dengan satu angka di belakang koma. */
    @Test
    fun jarakAntarbarisDitampilkanDenganSatuDesimal() {
        assertTrue("jarak antarbaris ditampilkan tanpa desimal",
            layar().contains("\"%.1f\".format(lineHeight)"))
    }
}
