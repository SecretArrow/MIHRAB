package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.feature.tahlil.SusunanSaya
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Pengaturan Aplikasi" meminta *"Preferensi susunan Tahlil"* ada di Pengaturan.
 *
 * Layar Pengaturan dulu hanya mencetak *"Susunan Tahlil: lazim Indonesia (lihat modul Tahlil)"* —
 * nama susunan yang ditulis tetap, sehingga berbohong begitu pengguna memilih susunan lain atau
 * menyusun "Susunan saya" di modul Tahlil. Uji ini menjaga agar pilihannya benar-benar ada di
 * Pengaturan, tersimpan pada kunci yang sama dengan modul Tahlil, dan yang ditampilkan adalah
 * susunan yang sungguh aktif.
 */
class PreferensiSusunanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun settingsScreen(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt")

    private fun tahlilViewModel(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilViewModel.kt")

    private fun susunan(id: String, nama: String = "Susunan $id") =
        TahlilSusunan(id, nama, listOf("a", "b"))

    // ---- Aturan pemilihan ----

    @Test
    fun tersediaMenambahkanSusunanSayaDiAkhir() {
        val berkas = listOf(susunan("lazim_indonesia"))
        val saya = susunan(PreferensiSusunan.ID_SAYA, PreferensiSusunan.NAMA_SAYA)
        assertEquals(listOf("lazim_indonesia", "saya"),
            PreferensiSusunan.tersedia(berkas, saya).map { it.id })
        assertEquals("tanpa susunan buatan pengguna, daftar berkas dipakai apa adanya",
            listOf("lazim_indonesia"), PreferensiSusunan.tersedia(berkas, null).map { it.id })
    }

    @Test
    fun idTersimpanYangMasihAdaDipakaiApaAdanya() {
        val daftar = listOf(susunan("lazim_indonesia"), susunan("saya"))
        assertEquals("saya", PreferensiSusunan.idAktif("saya", daftar))
    }

    /**
     * Nilai tersimpan bisa menunjuk susunan yang sudah tidak ada — bundel konten diganti, atau
     * "Susunan saya" dihapus. Harus jatuh ke susunan pertama, bukan ke daftar kosong yang membuat
     * layar Tahlil buntu.
     */
    @Test
    fun idTersimpanYangSudahTidakAdaJatuhKeSusunanPertama() {
        val daftar = listOf(susunan("lazim_indonesia"), susunan("kedua"))
        assertEquals("lazim_indonesia", PreferensiSusunan.idAktif("sudah_hilang", daftar))
        assertEquals("lazim_indonesia", PreferensiSusunan.idAktif("", daftar))
    }

    @Test
    fun daftarKosongMenghasilkanIdKosong() {
        assertEquals("", PreferensiSusunan.idAktif("apa saja", emptyList()))
        assertEquals("", PreferensiSusunan.namaAktif("apa saja", emptyList()))
    }

    @Test
    fun namaAktifMengikutiSusunanYangBenarBenarAktif() {
        val daftar = listOf(susunan("a", "Susunan A"), susunan("b", "Susunan B"))
        assertEquals("Susunan B", PreferensiSusunan.namaAktif("b", daftar))
        assertEquals("Susunan A", PreferensiSusunan.namaAktif("sudah_hilang", daftar))
    }

    @Test
    fun csvSusunanSayaMembuangDuplikatDanBagianKosong() {
        assertEquals(listOf("a", "b", "c"), PreferensiSusunan.parse("a, b ,,a,c"))
        assertTrue(PreferensiSusunan.parse(" , , ").isEmpty())
        assertEquals("a,b", PreferensiSusunan.serialize(listOf("a", " b ", "a")))
    }

    @Test
    fun susunanSayaKosongBilaBelumDisusun() {
        assertNull(PreferensiSusunan.susunanSaya(emptyList()))
        val s = PreferensiSusunan.susunanSaya(listOf("a", "b"))
        assertEquals(PreferensiSusunan.ID_SAYA, s?.id)
        assertEquals(PreferensiSusunan.NAMA_SAYA, s?.nama)
    }

    /** Modul Tahlil dan Pengaturan harus menyebut susunan buatan pengguna dengan id yang sama. */
    @Test
    fun idDanNamaSusunanSayaSamaDenganYangDipakaiModulTahlil() {
        assertEquals(SusunanSaya.ID, PreferensiSusunan.ID_SAYA)
        assertEquals(SusunanSaya.NAMA, PreferensiSusunan.NAMA_SAYA)
        assertEquals(SusunanSaya.parse("a,,a,b"), PreferensiSusunan.parse("a,,a,b"))
        assertEquals(SusunanSaya.susunan(listOf("a"))?.bagian, PreferensiSusunan.susunanSaya(listOf("a"))?.bagian)
    }

    // ---- Layar Pengaturan ----

    @Test
    fun pengaturanMenawarkanPemilihSusunanDanMenyimpannya() {
        val s = settingsScreen()
        assertTrue("Pengaturan tidak membaca daftar susunan dari konten",
            s.contains("repo.tahlilSusunan()"))
        assertTrue("Pengaturan tidak memakai aturan PreferensiSusunan",
            s.contains("PreferensiSusunan.tersedia(") && s.contains("PreferensiSusunan.idAktif("))
        assertTrue("Pengaturan tidak menyimpan pilihan susunan",
            s.contains("store.set(Store.TAHLIL_SUSUNAN, s.id)"))
        assertTrue("Pengaturan tidak membaca pilihan susunan yang tersimpan",
            s.contains("store.get(Store.TAHLIL_SUSUNAN"))
    }

    /** Klaim lama yang bisa berbohong tidak boleh kembali. */
    @Test
    fun pengaturanTidakMengklaimSatuSusunanSecaraTetap() {
        val s = settingsScreen()
        assertFalse("Pengaturan kembali mengklaim susunan secara hardcode",
            s.contains("Susunan Tahlil: lazim Indonesia"))
        assertFalse("Pengaturan menulis id susunan berkas secara hardcode",
            s.contains("lazim_indonesia"))
        assertTrue("Nama susunan aktif harus dibaca dari daftar, bukan ditulis tetap",
            s.contains("PreferensiSusunan.namaAktif("))
    }

    /**
     * Mengganti susunan mengubah urutan bagian, jadi posisi baca yang tersimpan tidak lagi
     * menunjuk bagian yang sama. Penandanya harus dikosongkan, seperti di modul Tahlil.
     */
    @Test
    fun pengaturanMengosongkanPenandaPosisiBacaSaatSusunanDiganti() {
        val s = settingsScreen()
        // Jenisnya kini datang dari kosakata bersama `JenisBacaan` — sama dengan yang dipakai
        // pemetaan tujuan tombol "Buka" — dan rujukan barisnya dari `KunciProgres`, sama dengan yang
        // dibaca `TahlilViewModel`. Keduanya harus sama persis: menulis ke jenis atau rujukan lain
        // berarti mengosongkan baris yang tidak dibaca siapa pun, sementara posisi lama tetap utuh.
        assertTrue("posisi baca lama tidak dikosongkan saat susunan diganti",
            s.contains("userData.setProgress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_POSISI, 0)"))
        assertTrue("hitungan bacaan lama tidak dikosongkan saat susunan diganti",
            s.contains("userData.setProgress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_HITUNGAN, 0)"))
    }

    /** Modul Tahlil memakai aturan yang sama, supaya keduanya tak berbeda pendapat. */
    @Test
    fun viewModelTahlilMemakaiAturanYangSama() {
        val vm = tahlilViewModel()
        assertTrue("ViewModel Tahlil tidak memakai PreferensiSusunan.tersedia",
            vm.contains("PreferensiSusunan.tersedia("))
        assertTrue("ViewModel Tahlil tidak memakai PreferensiSusunan.idAktif",
            vm.contains("PreferensiSusunan.idAktif("))
        assertTrue("Kind progres Tahlil tidak lagi berasal dari satu sumber",
            vm.contains("JenisBacaan.TAHLIL"))
    }

    @Test
    /** Nama kunci tersimpan dibekukan: mengubahnya membuat susunan lama tak terbaca. */
    @Test
    fun kunciPreferensiSusunanDideklarasikan() {
        val s = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt")
        assertTrue("Store.TAHLIL_SUSUNAN tidak dideklarasikan dengan nama tersimpannya",
            s.contains("val TAHLIL_SUSUNAN = stringPreferencesKey(\"tahlil_susunan\")"))
        assertTrue("Store.TAHLIL_CUSTOM tidak dideklarasikan dengan nama tersimpannya",
            s.contains("val TAHLIL_CUSTOM = stringPreferencesKey(\"tahlil_custom\")"))
    }
}
