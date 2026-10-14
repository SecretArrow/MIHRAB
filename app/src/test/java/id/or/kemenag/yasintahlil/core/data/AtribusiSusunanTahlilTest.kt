package id.or.kemenag.yasintahlil.core.data

import org.json.JSONObject
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
 *
 * `edisi` itu sendiri dahulu ditulis **dua kali** — di `tahlil.json` (dibaca) dan `tahlil_full.json`
 * (tak pernah dibaca) dengan ejaan berbeda, dan salinan yang dibaca justru yang lebih pendek
 * sehingga asal bacaan Qur'an tampil lebih longgar daripada yang dicatat sumbernya. Uji terakhir di
 * bawah menjaga agar keterangan itu tinggal di satu berkas.
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
     *
     * Polanya disebut **utuh** (`val edisiModul = d.optString("edisi", "")`), bukan sekadar
     * `optString("edisi"` — sebab pola pendek itu juga muncul di `panduanBersuci()` (wudhu), jadi
     * penjaga ini akan tetap hijau meski pembacaan `edisi` di [tahlilSusunan] diganti nilai kosong.
     * Itu bukan dugaan: mutasi `E repoBerhentiMembacaEdisiModul` pada `mutate64.py` memang lolos
     * sebelum pola ini dipersempit.
     */
    @Test
    fun repoMembacaSumberSusunanDenganCadanganEdisiModul() {
        val s = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt")
        assertTrue("ContentRepo tidak membaca keterangan edisi tingkat modul di tahlilSusunan()",
            s.contains("val edisiModul = d.optString(\"edisi\", \"\")"))
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

    /**
     * `edisi` tingkat modul Tahlil tinggal di **satu** berkas, bukan disalin ke keduanya.
     *
     * Dahulu `tahlil.json` dan `tahlil_full.json` sama-sama mencantumkannya, dengan ejaan yang
     * berbeda — *"… bacaan Qur'an Kemenag 2019 …"* versus *"… bacaan Qur'an memakai MSI LPMQ Kemenag
     * + terjemah Kemenag edisi 2019 …"*. Yang dibaca kode hanya `tahlil.json` (lihat
     * [repoMembacaSumberSusunanDenganCadanganEdisiModul]), sehingga salinan di `tahlil_full.json`
     * tak pernah tampil: memperbaruinya tak mengubah layar apa pun. Salinan mati itu dihapus, dan
     * ejaan yang lebih lengkap dipindah ke berkas yang dibaca — satu keterangan, satu rumah.
     */
    @Test
    fun edisiModulTinggalDiSatuBerkasTahlil() {
        val pemilik = listOf("tahlil.json", "tahlil_full.json").filter { berkas ->
            JSONObject(sumber("src/main/assets/content/$berkas")).has("edisi")
        }
        assertEquals("edisi tingkat modul punya salinan kedua yang tak pernah dibaca",
            listOf("tahlil.json"), pemilik)
    }

    /**
     * Yang tinggal adalah ejaan yang **lebih lengkap** — salinan yang lebih pendek menyebut asal
     * bacaan Qur'an hanya sebagai *"Kemenag 2019"*, sedangkan berkasnya sendiri mencatat Mushaf
     * Standar Indonesia (MSI) LPMQ Kemenag dan terjemah edisi 2019. Memindahkan yang lebih pendek
     * berarti layar terus menyebut sumbernya lebih longgar daripada yang dicatat sumbernya.
     */
    @Test
    fun edisiModulMenyebutMsiLpmqDanEdisiTerjemah() {
        val isi = JSONObject(sumber("src/main/assets/content/tahlil.json"))
            .getString("edisi")
        assertTrue("edisi modul tidak lagi menyebut MSI LPMQ Kemenag: $isi",
            isi.contains("MSI LPMQ Kemenag"))
        assertTrue("edisi modul tidak lagi menyebut terjemah Kemenag edisi 2019: $isi",
            isi.contains("terjemah Kemenag edisi 2019"))
    }

    // ---- Layar ----

    /**
     * Sumber susunan tetap sampai ke pengguna, kini lewat layar keterangan: layar Tahlil
     * menyediakan pintunya (ikon Info), dan keterangan Tahlil mencantumkan sumber **tiap**
     * susunan yang tersedia — nama saja tidak cukup, nama bukan sumber.
     *
     * Dahulu sumber susunan aktif menumpuk sebagai kartu teks di atas layar Tahlil; kartu itu
     * dihapus agar badan layar murni bacaan, dan seluruh keterangannya pindah ke layar keterangan
     * yang membacanya ulang dari bundel yang sama.
     */
    @Test
    fun modulTahlilMembukaSumberSusunanMelaluiKeterangan() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilScreen.kt")
        assertTrue("layar Tahlil tidak lagi membuka keterangan susunannya lewat ikon Info",
            layar.contains("Routes.keterangan(\"tahlil\")"))
        val keterangan = sumber(
            "src/main/java/id/or/kemenag/yasintahlil/feature/keterangan/KeteranganScreen.kt")
        assertTrue("keterangan Tahlil tidak mencantumkan sumber tiap susunan",
            keterangan.contains("s.sumber"))
        assertTrue("keterangan Tahlil tidak membaca daftar susunan dari bundel",
            keterangan.contains("repo.tahlilSusunan()"))
        // Susunan tanpa sumber tidak boleh mengklaim rujukan apa pun — di daftar itu ia tampil
        // sebagai nama telanjang, tanpa " — sumber" karangan.
        assertFalse("keterangan Tahlil mengarang rujukan untuk susunan tanpa sumber",
            keterangan.contains("Sumber: rujukan"))
    }

    /** Susunan aktif tetap terlihat dari tab susunannya; sumbernya di keterangan per susunan. */
    @Test
    fun susunanAktifTetapTergantiDariLayarTahlil() {
        val layar = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilScreen.kt")
        assertTrue("layar Tahlil tidak lagi mengganti susunan",
            layar.contains("vm.setSusunan("))
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
        // Atribusi susunan kini tinggal di layar keterangan Tahlil: bagian "Susunan yang tersedia"
        // menyebut sumber tiap susunan dari bundel, dan "Susunan saya" dinyatakan terus terang
        // sebagai buatan sendiri — bukan diisi rujukan karangan.
        val keterangan = sumber(
            "src/main/java/id/or/kemenag/yasintahlil/feature/keterangan/KeteranganScreen.kt")
        assertTrue("keterangan Tahlil tidak lagi menyebut sumber susunan sama sekali",
            keterangan.contains("s.sumber"))
        assertTrue("keterangan Tahlil tidak menyatakan susunan tanpa sumber secara terus terang",
            keterangan.contains("tidak mengklaim rujukan"))
        assertFalse("keterangan Tahlil mengarang rujukan untuk susunan buatan pengguna",
            keterangan.contains("Sumber: rujukan"))
    }
}
