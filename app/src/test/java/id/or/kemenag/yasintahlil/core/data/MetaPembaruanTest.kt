package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.TimeZone

/**
 * `_meta.json` ditulis saat sebuah bundel dipasang dan dibaca kembali untuk menampilkan keadaan
 * konten di layar **"Pembaruan konten"**. Kunci-kuncinya dahulu dieja dua kali di dalam
 * `ContentUpdateManager` — tujuh kunci pada penulisnya (`metaJson`) dan tujuh lagi pada pembacanya
 * (`status()`) — tanpa apa pun yang membandingkan keduanya.
 *
 * Kekeliruannya sunyi seperti kekeliruan bundel yang lain: mengganti nama kunci di satu sisi saja
 * membuat layar melaporkan *tidak ada pembaruan terpasang* padahal berkasnya sudah ada dan sedang
 * dipakai. Karena tombol **"Kembalikan ke konten bawaan"** hanya muncul saat ada yang terpasang,
 * pengguna kehilangan satu-satunya jalan kembali ke konten bawaan — tanpa satu pun pesan galat.
 *
 * `ContentUpdateManager` sendiri butuh `Context` (direktori aplikasi dan assets), jadi penulis dan
 * pembacanya dipisahkan ke fungsi murni [MetaPembaruan.tulis]/[MetaPembaruan.baca] yang dapat
 * diuji langsung di sini — pola yang sama dengan [entriRujukan] pada rujukan berkas. Uji ini
 * menutup dua hal sekaligus: perilakunya (putaran tulis→baca) dan sumbernya (kunci tak boleh
 * dieja kembali di luar [MetaPembaruan]).
 */
class MetaPembaruanTest {

    private val manager =
        "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentUpdateManager.kt"
    private val meta = "src/main/java/id/or/kemenag/yasintahlil/core/data/MetaPembaruan.kt"

    /** Kunci yang pernah dieja di badan penulis dan pembacanya. */
    private val kunci = listOf(
        "skema", "dipasang", "penerbit", "catatan", "berkas", "nama", "kategori",
    )

    /**
     * Zona tetap untuk waktu pemasangan. Tanpa ini, `Date(0L)` tercetak sebagai `01:00` di mesin
     * berzona Eropa dan `07:00` di Jakarta — uji yang lulus atau gagal menurut tempat ia dijalankan,
     * bukan menurut benar-salahnya kode.
     */
    private val utc = TimeZone.getTimeZone("UTC")

    private fun kode(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(relatif))

    private fun ringkas(
        nama: String = "dzikir.json",
        sumber: String = "https://sunnah.com/muslim:591",
        kategori: List<String> = listOf("dzikir"),
    ) = BundelKonten.Berkas(nama, sumber, kategori)

    private fun hasil(
        skema: Int = 1,
        penerbit: String = "LPMQ Kemenag",
        catatan: String = "Pembaruan teks Qur'an edisi 2026",
    ) = BundelKonten.Hasil(skema = skema, penerbit = penerbit, catatan = catatan)

    // --- Putaran tulis → baca -------------------------------------------------------------------

    /**
     * Isi yang ditulis harus terbaca kembali utuh.
     *
     * Inilah uji yang menutup kekeliruan sebenarnya: penulis dan pembaca memakai konstanta yang
     * sama, jadi putaran ini tak dapat gagal kecuali salah satunya berhenti memakainya.
     */
    @Test
    fun tulisLaluBacaMengembalikanIsiYangSama() {
        val asli = ringkas(kategori = listOf("dzikir", "hadis"))
        val teks = MetaPembaruan.tulis(hasil(), listOf(asli), Date(0L), utc).toString(1)
        val s = MetaPembaruan.baca(teks)

        assertEquals("skema tidak terbaca kembali", 1, s.skema)
        assertEquals("penerbit tidak terbaca kembali", "LPMQ Kemenag", s.penerbit)
        assertEquals("catatan tidak terbaca kembali", "Pembaruan teks Qur'an edisi 2026", s.catatan)
        assertEquals("waktu pemasangan tidak terbaca kembali", "1970-01-01 00:00", s.dipasang)
        assertEquals("ringkasan berkas tidak terbaca kembali", listOf(asli), s.berkas)
        assertEquals("sumber berkas tidak terbaca kembali", asli.sumberUrl, s.berkas.single().sumberUrl)
        assertEquals("kategori berkas tidak terbaca kembali", asli.kategori, s.berkas.single().kategori)
    }

    /**
     * Waktu pemasangan ditulis pada zona yang diberikan, bukan pada zona mesin yang kebetulan
     * menjalankannya — inilah yang membuat hasilnya sama bagi setiap pengguna.
     */
    @Test
    fun waktuPemasanganDiformatPadaZonaYangDiberikan() {
        val jakarta = MetaPembaruan.tulis(
            hasil(), listOf(ringkas()), Date(0L), TimeZone.getTimeZone("Asia/Jakarta"),
        ).optString(MetaPembaruan.DIPASANG)
        assertEquals("waktu pemasangan harus memakai zona yang diberikan", "1970-01-01 07:00", jakarta)
    }

    /** [MetaPembaruan.Status.aktif] harus **mengikuti** berkasnya, bukan disimpan terpisah. */
    @Test
    fun aktifMengikutiAdaTidaknyaBerkas() {
        assertFalse("status kosong tidak boleh aktif", MetaPembaruan.Status().aktif)
        assertTrue(
            "status dengan satu berkas harus aktif",
            MetaPembaruan.baca(MetaPembaruan.tulis(hasil(), listOf(ringkas()), Date(0L)).toString()).aktif,
        )
    }

    /** Beberapa berkas tetap terurut seperti ringkasannya, bukan dibalik atau hilang. */
    @Test
    fun beberapaBerkasTerbacaSemua() {
        val a = ringkas("dzikir.json")
        val b = ringkas("yasin_full.json", "https://quran.kemenag.go.id/x", listOf("quran"))
        val s = MetaPembaruan.baca(MetaPembaruan.tulis(hasil(), listOf(a, b), Date(0L)).toString())
        assertEquals(listOf(a, b), s.berkas)
    }

    // --- Berkas rusak atau kosong ---------------------------------------------------------------

    /**
     * Metadata yang rusak hanya berarti aplikasi kembali membaca konten bawaan — bukan alasan
     * menjatuhkan layar Pengaturan. Pembacaannya karena itu tidak boleh melempar.
     */
    @Test
    fun teksRusakAtauKosongMenghasilkanStatusBawaan() {
        listOf("", "   ", "bukan json", "{", """{"berkas": "bukan larik"}""").forEach { teks ->
            val s = MetaPembaruan.baca(teks)
            assertFalse("'$teks' seharusnya menghasilkan status tidak aktif", s.aktif)
            assertTrue("'$teks' seharusnya menghasilkan status bawaan", s.berkas.isEmpty())
        }
    }

    /** Berkas tanpa nama tak dapat ditampilkan maupun dijelaskan asalnya; dilewati, bukan jadi baris kosong. */
    @Test
    fun berkasTanpaNamaDilewati() {
        val teks = """{"berkas":[{"kategori":["dzikir"]},{"nama":"a.json"},{"nama":"  "}]}"""
        val s = MetaPembaruan.baca(teks)
        assertEquals("hanya berkas bernama yang boleh tampil", listOf("a.json"), s.berkas.map { it.nama })
    }

    /** Larik `kategori` yang hilang atau berisi bukan teks tidak melempar. */
    @Test
    fun kategoriHilangAtauAnehTidakMelempar() {
        listOf(
            """{"berkas":[{"nama":"a.json"}]}""",
            """{"berkas":[{"nama":"a.json","kategori":[]}]}""",
        ).forEach { teks ->
            assertEquals("kategori kosong harus jadi daftar kosong",
                emptyList<String>(), MetaPembaruan.baca(teks).berkas.single().kategori)
        }
    }

    // --- Sumber: kunci hanya boleh tinggal di satu tempat ---------------------------------------

    /**
     * `ContentUpdateManager` tidak boleh mengeja kunci `_meta.json` sendiri lagi.
     *
     * Inilah penjaga yang membuat kekeliruannya tak dapat kembali: selama penulis dan pembaca
     * memakai konstanta [MetaPembaruan], mengganti nama kunci memindahkan keduanya sekaligus.
     */
    @Test
    fun contentUpdateManagerTidakMengejaKunciMetaSendiri() {
        val lokal = kunci.filter { kode(manager).contains("\"$it\"") }
        assertTrue(
            "ContentUpdateManager mengeja ulang kunci `_meta.json` — penulis dan pembacanya bisa " +
                "berbeda tanpa galat: $lokal",
            lokal.isEmpty(),
        )
    }

    /** Sebaliknya, penulis dan pembacanya memang harus lewat [MetaPembaruan]. */
    @Test
    fun contentUpdateManagerMemakaiPenulisDanPembacaBersama() {
        assertTrue("ContentUpdateManager tidak memakai MetaPembaruan.tulis()",
            kode(manager).contains("MetaPembaruan.tulis("))
        assertTrue("ContentUpdateManager tidak memakai MetaPembaruan.baca()",
            kode(manager).contains("MetaPembaruan.baca("))
        // Tipe hasilnya pun milik tempat bersama itu, supaya layar tak dapat menyusun sendiri
        // status yang tak pernah cocok dengan isi berkasnya.
        assertTrue("status() tidak mengembalikan MetaPembaruan.Status",
            kode(manager).contains("MetaPembaruan.Status"))
        assertFalse("ContentUpdateManager menyimpan model Status-nya sendiri",
            kode(manager).contains("data class Status"))
    }

    /**
     * Setiap kunci yang dideklarasikan benar-benar dipakai menulis dan membaca.
     *
     * Kunci yang ditulis tetapi tak pernah dibaca (atau sebaliknya) adalah tanda penulis dan
     * pembaca sudah tidak lagi berbicara tentang berkas yang sama.
     */
    @Test
    fun setiapKunciDipakaiMenulisDanMembaca() {
        val k = kode(meta)
        kunci.forEach { nama ->
            assertTrue("kunci '$nama' tidak dideklarasikan di MetaPembaruan",
                k.contains("const val ${nama.uppercase()} = \"$nama\""))
        }
        // `skema` ditulis untuk keterangan; sisanya dibaca kembali menjadi Status.
        assertTrue("penulis tidak memakai SKEMA", k.contains(".put(SKEMA,"))
        assertTrue("penulis tidak memakai DIPASANG", k.contains(".put(DIPASANG,"))
        assertTrue("penulis tidak memakai PENERBIT", k.contains(".put(PENERBIT,"))
        assertTrue("penulis tidak memakai CATATAN", k.contains(".put(CATATAN,"))
        assertTrue("penulis tidak memakai BERKAS", k.contains(".put(BERKAS,"))
        assertTrue("penulis tidak memakai NAMA", k.contains(".put(NAMA,"))
        assertTrue("penulis tidak memakai KATEGORI", k.contains(".put(KATEGORI,"))
        listOf("SKEMA", "DIPASANG", "PENERBIT", "CATATAN", "BERKAS", "NAMA", "KATEGORI")
            .forEach { assertTrue("pembaca tidak memakai $it", k.contains("optInt($it") ||
                k.contains("optString($it") || k.contains("optJSONArray($it")) }
    }

    /** Metadata bundel yang dipasang tetap tercatat lengkap — berkasnya harus benar-benar ada. */
    @Test
    fun penulisanMenghasilkanJsonYangDapatDibacaUlang() {
        val j = MetaPembaruan.tulis(hasil(), listOf(ringkas()), Date(0L))
        assertTrue("hasil penulisan bukan JSON yang sah", JSONObject(j.toString()).length() == 5)
        // Ejaan sumber ditulis lewat ejaan utama bersama, bukan ejaan lokal.
        val b = j.getJSONArray(MetaPembaruan.BERKAS).getJSONObject(0)
        assertEquals("https://sunnah.com/muslim:591", b.optString("sumber_url"))
    }
}
