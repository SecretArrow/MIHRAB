package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Bentuk dokumen konten yang dapat dirujuk — nama wadahnya (`bagian`, `bacaan`,
 * `kelompok[].items[]`) dan kunci `id` entrinya.
 *
 * Bentuk itu dibaca **dua peran** yang tak pernah saling melihat: [ContentRepo], yang membangun
 * daftar bacaan dari dokumennya, dan `entriRujukan`, yang mengikuti rujukan `berkas.json#id` dari
 * dokumen lain. Dahulu keduanya menuliskan nama-nama wadah dan kunci `id` sendiri-sendiri; salinan
 * yang menyimpang menyesatkan ke arah mana pun ia bergeser — pembaca menampilkan entri **kosong
 * tanpa galat**, atau pemeriksa menolak bundel yang sah dengan alasan "menunjuk entri yang tidak
 * ada". Uji ini menutup dua hal sekaligus: perilakunya (penelusuran benar-benar menemukan entri di
 * ketiga bentuk) dan sumbernya (nama wadah hanya boleh tinggal di [BentukKonten]).
 *
 * Peran ketiga yang tak pernah melihat keduanya adalah **gate CI** (`tools/validate_content.py`),
 * yang memeriksa struktur berkas konten yang sama sebelum dibangun. Ia dahulu menuliskan nama
 * wadahnya sendiri, dan salinan yang tertinggal di sana paling sunyi dari semuanya: `if "bagian"
 * in data` sekadar tidak pernah benar lagi, sehingga seluruh pemeriksaan tahlil **dilewati** dan
 * berkas cacat lolos CI tanpa satu pun galat — sementara aplikasi tetap membaca wadah itu lewat
 * [BentukKonten]. Karena itu gate pun membaca namanya dari sini, dan uji di bawah menjaganya.
 */
class BentukKontenTest {

    private val repo = "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt"
    private val rujukan = "src/main/java/id/or/kemenag/yasintahlil/core/data/Rujukan.kt"
    private val bentuk = "src/main/java/id/or/kemenag/yasintahlil/core/data/BentukKonten.kt"

    private fun kode(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(relatif))

    /**
     * Akar repositori, tempat `app/` dan `tools/` bersebelahan.
     *
     * Uji berjalan dengan direktori kerja modul (`app/`), jadi [KodeSumber.root] menunjuk ke `app` —
     * naik satu tingkat dulu sebelum mencari gate.
     */
    private fun repoRoot(): File =
        generateSequence(KodeSumber.root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_content.py").isFile }
            ?: error("tools/validate_content.py tidak ditemukan dari ${KodeSumber.root()}")

    private val gerbangKonten: String
        get() = File(repoRoot(), "tools/validate_content.py").readText()

    private val gerbangBundel: String
        get() = File(repoRoot(), "tools/validate_bundle.py").readText()

    // --- Perilaku: penelusuran menemukan entri di setiap bentuk ---------------------------------

    /**
     * Setiap bentuk yang dikenali [BentukKonten] harus benar-benar dapat ditelusuri. Bentuk yang
     * tercantum di sana tetapi tak terbaca berarti rujukan mati yang lolos pemeriksaan — persis
     * teks yang tampil kosong tanpa satu pun galat.
     */
    @Test
    fun entriRujukanMenemukanEntriDiSetiapBentuk() {
        listOf(
            Triple("bagian", """{"bagian":[{"id":"takbir","judul":"Takbir"}]}""", "takbir"),
            Triple("bacaan", """{"bacaan":[{"id":"niat_jenazah","judul":"Niat"}]}""", "niat_jenazah"),
            Triple(
                "kelompok[].items[]",
                """{"kelompok":[{"id":"g1","items":[{"id":"ayat_kursi","judul":"Ayat Kursi"}]}]}""",
                "ayat_kursi",
            ),
        ).forEach { (bentuk, teks, id) ->
            val entri = entriRujukan(JSONObject(teks), id)
            assertEquals("entri '$id' tidak ditemukan pada bentuk $bentuk", id,
                entri?.optString(BentukKonten.ID))
            assertTrue("entri '$id' pada $bentuk kehilangan field lainnya",
                entri?.optString("judul").orEmpty().isNotBlank())
        }
    }

    /** Id yang tidak ada, id kosong, dan dokumen tanpa wadah mana pun berarti rujukan mati. */
    @Test
    fun entriRujukanMengembalikanNullBilaTidakAda() {
        val bagian = """{"bagian":[{"id":"takbir","judul":"Takbir"}]}"""
        assertNull(entriRujukan(JSONObject(bagian), "tidak_ada"))
        assertNull(entriRujukan(JSONObject(bagian), "  "))
        assertNull(entriRujukan(JSONObject("""{"judul":"tanpa wadah"}"""), "takbir"))
        // Kelompok tanpa `items` dilewati, bukan menjatuhkan seluruh penelusuran.
        assertNull(entriRujukan(JSONObject("""{"kelompok":[{"id":"g1"}]}"""), "ayat_kursi"))
    }

    /**
     * Bila satu dokumen memuat dua bentuk tingkat dokumen, yang **pertama** pada
     * [BentukKonten.DAFTAR_ENTRI] yang dipakai — bukan campuran keduanya. Tanpa urutan yang pasti,
     * dokumen yang keliru mencantumkan dua bentuk akan menampilkan sebagian entri dari masing-masing.
     */
    @Test
    fun bentukTingkatDokumenDitelusuriMenurutUrutanDaftar() {
        val teks = """{"bagian":[{"id":"a","judul":"dari bagian"}],
            "bacaan":[{"id":"a","judul":"dari bacaan"}]}"""
        assertEquals("dari bagian", entriRujukan(JSONObject(teks), "a")?.optString("judul"))
        assertEquals("bagian", BentukKonten.DAFTAR_ENTRI.first())
    }

    // --- Sumber: bentuk hanya boleh tinggal di satu tempat ---------------------------------------

    /** Daftar wadah yang ditelusuri harus benar-benar memuat kedua bentuk tingkat dokumen. */
    @Test
    fun daftarEntriSejajarDenganKonstanta() {
        assertEquals(
            "DAFTAR_ENTRI tidak lagi memuat kedua bentuk tingkat dokumen",
            listOf(BentukKonten.BAGIAN, BentukKonten.BACAAN),
            BentukKonten.DAFTAR_ENTRI,
        )
    }

    /**
     * Nilai kuncinya dipaku: berkas konten di bundel memakai ejaan ini, dan mengubahnya membuat
     * dokumen yang sudah terverifikasi tak lagi terbaca — di sisi mana pun perubahan itu dilakukan.
     */
    @Test
    fun nilaiKunciBentukDipaku() {
        assertEquals("ejaan kunci id berubah", "id", BentukKonten.ID)
        assertEquals("ejaan wadah bagian berubah", "bagian", BentukKonten.BAGIAN)
        assertEquals("ejaan wadah bacaan berubah", "bacaan", BentukKonten.BACAAN)
        assertEquals("ejaan wadah kelompok berubah", "kelompok", BentukKonten.KELOMPOK)
        assertEquals("ejaan wadah items berubah", "items", BentukKonten.ITEMS)

        val semua = listOf(
            BentukKonten.ID, BentukKonten.BAGIAN, BentukKonten.BACAAN,
            BentukKonten.KELOMPOK, BentukKonten.ITEMS,
        )
        assertTrue("ada nama bentuk yang kosong", semua.all { it.isNotBlank() })
        assertEquals("ada nama bentuk yang kembar", semua.size, semua.toSet().size)
    }

    /**
     * Pembaca konten dan penelusur rujukan tidak boleh mengeja satu pun nama wadah itu sendiri.
     *
     * Inilah penjaga yang membuat kekeliruannya tak dapat kembali: selama keduanya memakai
     * [BentukKonten], menambah atau mengganti nama wadah memindahkan keduanya sekaligus.
     */
    @Test
    fun contentRepoDanRujukanTidakMengejaWadahSendiri() {
        val nama = listOf(
            BentukKonten.ID, BentukKonten.BAGIAN, BentukKonten.BACAAN,
            BentukKonten.KELOMPOK, BentukKonten.ITEMS,
        )
        listOf(repo, rujukan).forEach { berkas ->
            val lokal = nama.filter { kode(berkas).contains("\"$it\"") }
            assertTrue(
                "$berkas mengeja ulang nama wadah konten — pembaca dan penelusur rujukan bisa " +
                    "berbeda tanpa galat: $lokal",
                lokal.isEmpty(),
            )
        }
    }

    /** Sebaliknya, keduanya memang harus lewat [BentukKonten]. */
    @Test
    fun contentRepoDanRujukanMemakaiBentukKonten() {
        val k = kode(repo)
        listOf("BAGIAN", "BACAAN", "KELOMPOK", "ITEMS", "ID").forEach { n ->
            assertTrue("ContentRepo tidak memakai BentukKonten.$n", k.contains("BentukKonten.$n"))
        }
        val r = kode(rujukan)
        listOf("DAFTAR_ENTRI", "KELOMPOK", "ITEMS", "ID").forEach { n ->
            assertTrue("Rujukan tidak memakai BentukKonten.$n", r.contains("BentukKonten.$n"))
        }
    }

    // --- Sumber: gate CI membaca bentuk yang sama -----------------------------------------------

    /**
     * Gate konten harus membaca nama wadah dari [BentukKonten], bukan menyimpannya.
     *
     * Salinan di gate menyempitkan pemeriksaan **tanpa galat**: `if "bagian" in data` yang tidak
     * pernah lagi benar membuat seluruh pemeriksaan tahlil dilewati, dan berkas yang kehilangan
     * `id`/`judul` di tiap bagiannya lolos CI tanpa satu pun pesan.
     */
    @Test
    fun gerbangKontenMembacaBentukDariBentukKonten() {
        assertTrue("gate konten tidak menunjuk BentukKonten.kt",
            gerbangKonten.contains("/ \"BentukKonten.kt\")"))
        listOf("ID", "BAGIAN", "KELOMPOK", "ITEMS").forEach { n ->
            assertTrue("gate konten tidak membaca BentukKonten.$n",
                gerbangKonten.contains("""_bentuk("$n")"""))
        }
        assertTrue("gate konten memakai pola pengambil yang berbeda dari penjaganya",
            gerbangKonten.contains("baca_teks(SUMBER_BENTUK, nama, BENTUK_KONTEN)"))
        assertTrue("gate konten membaca bentuk dari berkas lain",
            gerbangKonten.contains("SUMBER_BENTUK = _sumber(BENTUK_KONTEN)"))
        // Nama wadah dan kunci `id` tidak boleh kembali disalin ke gate.
        listOf("bagian", "kelompok", "items").forEach { n ->
            assertFalse("gate konten menyimpan salinan nama wadah '$n'",
                gerbangKonten.contains("\"$n\""))
        }
        assertFalse("gate konten mengeja kunci id entri sendiri",
            gerbangKonten.contains("get(\"id\")"))
        assertFalse("gate konten mengeja pasangan kunci tahlil sendiri",
            gerbangKonten.contains("(\"id\", \"judul\")"))
    }

    /** Gate bundel memanggil pemeriksa yang sama, jadi ia pun tak boleh mengeja kunci `id`. */
    @Test
    fun gerbangBundelMemakaiKunciIdBersama() {
        assertTrue("gate bundel tidak membaca kunci id dari gate konten",
            gerbangBundel.contains("vc.KUNCI_ID"))
        assertFalse("gate bundel mengeja kunci id entri sendiri",
            gerbangBundel.contains("get(\"id\")"))
    }

    /**
     * Pola pengambil yang dipakai gate benar-benar menghasilkan nilai aplikasi — bukan sekadar
     * cocok. Pola yang salah (mis. membaca `DAFTAR_ENTRI` alih-alih `BAGIAN`) tetap hijau tanpa uji
     * ini, sebab gate hanya akan memeriksa wadah yang keliru.
     */
    @Test
    fun polaPengambilBentukMenghasilkanNilaiAplikasi() {
        val sumber = KodeSumber.sumber(bentuk)
        assertEquals("kunci id yang dibaca gate berbeda dari yang dipakai aplikasi",
            BentukKonten.ID, teks(sumber, "ID"))
        assertEquals("wadah bagian yang dibaca gate berbeda dari yang dipakai aplikasi",
            BentukKonten.BAGIAN, teks(sumber, "BAGIAN"))
        assertEquals("wadah kelompok yang dibaca gate berbeda dari yang dipakai aplikasi",
            BentukKonten.KELOMPOK, teks(sumber, "KELOMPOK"))
        assertEquals("wadah items yang dibaca gate berbeda dari yang dipakai aplikasi",
            BentukKonten.ITEMS, teks(sumber, "ITEMS"))
    }

    /** Isi literal string Kotlin → teks sebenarnya (`\\` → `\`, `\"` → `"`). */
    private fun unescape(t: String): String {
        val keluar = StringBuilder()
        var i = 0
        while (i < t.length) {
            if (t[i] == '\\' && i + 1 < t.length) {
                keluar.append(t[i + 1])
                i += 2
            } else {
                keluar.append(t[i])
                i++
            }
        }
        return keluar.toString()
    }

    /** Nilai `<nama> = "…"` dengan pola pengambil yang **sama persis** seperti gate Python. */
    private fun teks(sumber: String, nama: String): String =
        Regex("\\b$nama\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(sumber)
            ?.groupValues?.get(1)?.let { unescape(it) }
            ?: error("'$nama = \"...\"' tidak ditemukan di $bentuk")
}
