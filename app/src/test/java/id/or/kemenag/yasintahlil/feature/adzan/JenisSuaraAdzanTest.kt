package id.or.kemenag.yasintahlil.feature.adzan

import id.or.kemenag.yasintahlil.core.data.DaftarSuaraAdzan
import id.or.kemenag.yasintahlil.core.data.SuaraAdzan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Fitur Adzan" meminta *"Pilihan suara adzan dari sumber audio yang legal digunakan"*
 * dan *"Pengaturan volume dan jenis suara"*.
 *
 * Layar Adzan dahulu hanya punya **satu** berkas audio (`Store.ADZAN_URI`) dan satu slider volume:
 * tidak ada "jenis suara" sama sekali, sehingga memilih berkas lain berarti kehilangan berkas
 * sebelumnya. Aplikasi tetap tidak menyertakan audio bawaan (lisensi) — yang ditambahkan adalah
 * daftar bernama milik pengguna, dengan tepat satu yang aktif. Uji ini menjaga agar daftar itu
 * tersimpan, dapat dipilih/dinamai/dihapus, dan agar berkas aktif tetap satu kunci yang sama dengan
 * yang dibaca penjadwal serta pekerja latar.
 */
class JenisSuaraAdzanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun layar(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt")

    private fun a(nama: String, uri: String) = SuaraAdzan(nama, uri)

    // ---- Aturan daftar (murni, diuji di JVM) ----

    @Test
    fun tulisBacaDaftarBertahan() {
        val daftar = listOf(a("Adzan masjid lokal", "content://x/1.mp3"), a("Adzan Nabawi", "content://x/2.mp3"))
        assertEquals(daftar, DaftarSuaraAdzan.parse(DaftarSuaraAdzan.serialize(daftar)))
    }

    /** Nama boleh memuat spasi dan tanda baca; hanya baris baru/pemisah yang dinetralkan. */
    @Test
    fun namaDenganSpasiDanTandaBacaTetapUtuh() {
        val daftar = listOf(a("Adzan — masjid \"Al-Ikhlas\", v2", "content://x/1.mp3"))
        assertEquals(daftar, DaftarSuaraAdzan.parse(DaftarSuaraAdzan.serialize(daftar)))
    }

    @Test
    fun barisCacatDanUriGandaDibuang() {
        val teks = "tanpa pemisah\nNama Satu\tcontent://x/1\nNama Dua\tcontent://x/1\n\tcontent://x/2\n\n"
        val hasil = DaftarSuaraAdzan.parse(teks)
        assertEquals(listOf("content://x/1", "content://x/2"), hasil.map { it.uri })
        assertEquals("nama berkas dipakai bila nama kosong", "2", hasil[1].nama)
    }

    @Test
    fun menambahBerkasBaruMenyimpannyaDiAkhir() {
        val awal = listOf(a("Satu", "content://x/1"))
        assertEquals(
            listOf("content://x/1", "content://x/2"),
            DaftarSuaraAdzan.tambah(awal, "Dua", "content://x/2").map { it.uri },
        )
        assertEquals("URI kosong diabaikan", awal, DaftarSuaraAdzan.tambah(awal, "Kosong", ""))
    }

    /** Memilih berkas yang sudah tersimpan memperbarui namanya, bukan menambah entri kedua. */
    @Test
    fun menambahUriYangSudahAdaHanyaMenggantiNama() {
        val awal = listOf(a("Lama", "content://x/1"), a("Lain", "content://x/2"))
        val hasil = DaftarSuaraAdzan.tambah(awal, "Baru", "content://x/1")
        assertEquals(2, hasil.size)
        assertEquals("Baru", hasil[0].nama)
        assertEquals("urutan tidak berubah", listOf("content://x/1", "content://x/2"), hasil.map { it.uri })
    }

    @Test
    fun namaKosongJatuhKeNamaBerkas() {
        val hasil = DaftarSuaraAdzan.tambah(emptyList(), "   ", "content://x/adzan%20subuh.mp3")
        assertEquals("adzan subuh", hasil.single().nama)
    }

    @Test
    fun menghapusSatuJenisSuaraTidakMenyentuhYangLain() {
        val awal = listOf(a("Satu", "content://x/1"), a("Dua", "content://x/2"))
        assertEquals(listOf("content://x/2"), DaftarSuaraAdzan.hapus(awal, "content://x/1").map { it.uri })
    }

    /** Menghapus jenis suara yang sedang aktif harus memindahkan pilihan, bukan menyisakan URI hantu. */
    @Test
    fun menghapusYangAktifMemindahkanPilihanKeBerkasTersisa() {
        val awal = listOf(a("Satu", "content://x/1"), a("Dua", "content://x/2"))
        assertEquals("content://x/2", DaftarSuaraAdzan.aktifSetelahHapus(awal, "content://x/1", "content://x/1"))
        assertEquals("content://x/1", DaftarSuaraAdzan.aktifSetelahHapus(awal, "content://x/2", "content://x/1"))
        assertEquals("", DaftarSuaraAdzan.aktifSetelahHapus(listOf(a("Satu", "content://x/1")), "content://x/1", "content://x/1"))
    }

    /** Pengguna versi lama hanya punya `ADZAN_URI`; berkas itu harus tetap terlihat dan terpilih. */
    @Test
    fun berkasAktifLamaMunculDiDaftar() {
        val tampil = DaftarSuaraAdzan.tampil(listOf(a("Baru", "content://x/2")), "content://x/1")
        assertEquals(listOf("content://x/1", "content://x/2"), tampil.map { it.uri })
        assertEquals("tidak digandakan bila sudah tercatat",
            listOf("content://x/2"), DaftarSuaraAdzan.tampil(listOf(a("Baru", "content://x/2")), "content://x/2").map { it.uri })
        assertTrue("tanpa berkas aktif, daftar apa adanya",
            DaftarSuaraAdzan.tampil(listOf(a("Baru", "content://x/2")), "").map { it.uri } == listOf("content://x/2"))
    }

    @Test
    fun namaAktifMenunjukBerkasYangBenarBenarAktif() {
        val daftar = listOf(a("Satu", "content://x/1"), a("Dua", "content://x/2"))
        assertEquals("Dua", DaftarSuaraAdzan.namaAktif(daftar, "content://x/2"))
        assertEquals("", DaftarSuaraAdzan.namaAktif(daftar, ""))
        assertEquals("subuh", DaftarSuaraAdzan.namaAktif(daftar, "content://x/subuh.mp3"))
    }

    // ---- Layar Adzan ----

    @Test
    fun layarMenawarkanPemilihJenisSuaraDanMenyimpannya() {
        val s = layar()
        assertTrue("layar tidak membaca daftar jenis suara tersimpan", s.contains("store.get(Store.ADZAN_SOUNDS"))
        assertTrue("layar tidak menyimpan daftar jenis suara", s.contains("store.set(Store.ADZAN_SOUNDS"))
        assertTrue("layar tidak memakai aturan DaftarSuaraAdzan", s.contains("DaftarSuaraAdzan."))
        assertTrue("tidak ada tombol menambah jenis suara", s.contains("Tambah jenis suara"))
        assertTrue("tidak ada cara memilih jenis suara aktif", s.contains("RadioButton("))
        assertTrue("tidak ada cara menghapus jenis suara", s.contains("Hapus"))
        assertTrue("tidak ada cara menamai jenis suara", s.contains("Ubah nama"))
    }

    /**
     * Berkas aktif tetap satu kunci ([Store.ADZAN_URI]) supaya penjadwal dan pekerja latar tidak
     * perlu tahu apa-apa tentang daftar jenis suara.
     */
    @Test
    fun memilihJenisSuaraMenulisKunciBerkasAktifYangSama() {
        val s = layar()
        assertTrue("pilihan jenis suara tidak menulis berkas aktif",
            s.contains("store.set(Store.ADZAN_URI,"))
        assertTrue("mengubah jenis suara tidak menjadwalkan ulang pengingat",
            s.contains("PrayerScheduler.scheduleAll("))
    }

    /** Volume tetap ada (`plan.txt`: *"Pengaturan volume dan jenis suara"*). */
    @Test
    fun pengaturanVolumeTetapAda() {
        val s = layar()
        assertTrue("slider volume hilang", s.contains("Store.ADZAN_VOL"))
        assertTrue("tombol pratinjau hilang", s.contains("Pratinjau"))
    }

    @Test
    fun kunciJenisSuaraIkutBackup() {
        val store = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt")
        assertTrue("Store.ADZAN_SOUNDS tidak dideklarasikan",
            store.contains("val ADZAN_SOUNDS = stringPreferencesKey(\"adzan_sounds\")"))
        val impor = store.substringAfter("fun importJson")
        assertTrue("ADZAN_SOUNDS tidak ikut dipulihkan dari backup (daftar jenis suara hilang)",
            impor.contains("str(ADZAN_SOUNDS)"))
    }

    /** Audio bawaan tetap tidak disertakan — jenis suara hanya berkas pilihan pengguna. */
    @Test
    fun tidakAdaAudioBawaanYangDisertakan() {
        val assets = File(root(), "src/main/assets")
        val audio = assets.walkTopDown().filter { it.isFile && it.extension.lowercase() in setOf("mp3", "ogg", "wav", "m4a", "aac") }
        assertTrue("audio bawaan ditemukan di assets: ${audio.map { it.name }.toList()}", audio.none())
        assertFalse("layar mengklaim menyertakan suara bawaan",
            layar().contains("suara bawaan dari aplikasi"))
    }
}
