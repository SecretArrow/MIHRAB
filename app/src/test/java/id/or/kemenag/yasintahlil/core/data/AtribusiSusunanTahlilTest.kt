package id.or.kemenag.yasintahlil.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Fitur Tahlil Lengkap" meminta susunan yang *"berdasarkan sumber atau tradisi yang
 * jelas"*, dan melarang menyatakan satu susunan sebagai satu-satunya yang benar.
 *
 * Layar Tahlil dahulu hanya mencetak **nama** susunan (*"Susunan tahlil lazim Indonesia; susunan
 * dapat berbeda menurut tradisi"*). Nama bukan sumber: berkas `tahlil.json` sudah memuat
 * keterangan asal-usulnya (`edisi` tingkat modul), tetapi keterangan itu tidak pernah sampai ke
 * layar mana pun — jadi pengguna tidak dapat memeriksa dari mana rangkaian itu berasal. Uji ini
 * menjaga agar sumber susunan dibaca, diteruskan, dan ditampilkan; dan agar "Susunan saya" **tidak**
 * mengklaim rujukan apa pun.
 */
class AtribusiSusunanTahlilTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun susunan(id: String, sumberTeks: String) =
        TahlilSusunan(id, "Susunan $id", listOf("a", "b"), sumberTeks)

    // ---- Aturan (murni, diuji di JVM) ----

    @Test
    fun sumberAktifMengikutiSusunanYangBenarBenarAktif() {
        val daftar = listOf(susunan("a", "Sumber A"), susunan("b", "Sumber B"))
        assertEquals("Sumber B", PreferensiSusunan.sumberAktif("b", daftar))
        // Nilai tersimpan yang sudah tidak ada jatuh ke susunan pertama, sama seperti `idAktif`.
        assertEquals("Sumber A", PreferensiSusunan.sumberAktif("sudah_hilang", daftar))
        assertEquals("", PreferensiSusunan.sumberAktif("apa saja", emptyList()))
    }

    /** Susunan buatan pengguna tidak boleh tampil seolah berasal dari rujukan tertentu. */
    @Test
    fun susunanSayaTidakMengklaimSumber() {
        val saya = PreferensiSusunan.susunanSaya(listOf("a", "b"))
        assertEquals("", saya?.sumber)
        assertEquals("", PreferensiSusunan.sumberAktif(PreferensiSusunan.ID_SAYA, listOfNotNull(saya)))
    }

    /** Susunan tanpa sumber tetap dapat dipilih — yang kosong hanyalah atribusinya. */
    @Test
    fun susunanTanpaSumberTetapTerbaca() {
        val daftar = listOf(susunan("a", ""))
        assertEquals("Susunan a", PreferensiSusunan.namaAktif("a", daftar))
        assertEquals("", PreferensiSusunan.sumberAktif("a", daftar))
    }

    // ---- Pembacaan bundel konten ----

    /**
     * Susunan dari berkas wajib mendapat sumber: dari `sumber` miliknya sendiri, atau dari
     * keterangan `edisi` tingkat modul. Tanpa cadangan itu, susunan yang belum punya `sumber`
     * tampil sebagai rangkaian telanjang.
     */
    @Test
    fun repoMembacaSumberSusunanDenganCadanganEdisiModul() {
        val s = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt")
        assertTrue("ContentRepo tidak membaca keterangan edisi tingkat modul",
            s.contains("optString(\"edisi\""))
        assertTrue("ContentRepo tidak membaca sumber per-susunan",
            s.contains("optString(\"sumber\", edisiModul)"))
    }

    /** Cadangan itu harus benar-benar ada di bundel, jika tidak sumbernya kosong. */
    @Test
    fun bundelMencantumkanEdisiSebagaiCadangan() {
        val json = sumber("src/main/assets/content/tahlil.json")
        assertTrue("tahlil.json tidak mencantumkan keterangan edisi", json.contains("\"edisi\""))
        assertTrue("tahlil.json tidak mencantumkan sumber_url", json.contains("\"sumber_url\""))
        assertTrue("tahlil.json tidak memuat daftar susunan", json.contains("\"susunan\""))
    }

    // ---- Layar ----

    @Test
    fun modulTahlilMenampilkanSumberSusunanAktif() {
        val vm = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilViewModel.kt")
        assertTrue("keadaan Tahlil tidak meneruskan sumber susunan aktif",
            vm.contains("val sumberSusunan"))
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilScreen.kt")
        assertTrue("layar Tahlil tidak menampilkan sumber susunan", layar.contains("s.sumberSusunan"))
        assertTrue("layar Tahlil tidak menyatakan susunan tanpa sumber secara terus terang",
            layar.contains("disusun sendiri oleh Anda"))
    }

    @Test
    fun pengaturanMenampilkanSumberSusunanAktif() {
        val s = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt")
        assertTrue("Pengaturan tidak memakai PreferensiSusunan.sumberAktif",
            s.contains("PreferensiSusunan.sumberAktif("))
        assertTrue("Pengaturan tidak menampilkan sumber susunan aktif",
            s.contains("Sumber susunan aktif"))
    }

    /** Nama susunan saja tidak boleh kembali menjadi satu-satunya atribusi yang terlihat. */
    @Test
    fun namaSusunanBukanSatuSatunyaAtribusi() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilScreen.kt")
        assertTrue("layar Tahlil tidak lagi menyebut sumber susunan sama sekali",
            layar.contains("Sumber:"))
        assertFalse("layar Tahlil mengarang rujukan untuk susunan buatan pengguna",
            layar.contains("Sumber: rujukan"))
    }
}
