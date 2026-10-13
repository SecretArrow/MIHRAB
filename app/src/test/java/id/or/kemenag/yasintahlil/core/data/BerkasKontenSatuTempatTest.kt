package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nama berkas konten harus satu ejaan, dan ejaan itu harus menunjuk berkas yang benar-benar ada.
 *
 * Sebuah nama berkas konten adalah kunci ke tiga tempat sekaligus: `assets/content` (konten bawaan),
 * `filesDir/content_update/` (penimpaan yang dipasang pengguna), dan peta `berkas` pada sebuah bundel.
 * `ContentRepo` dahulu mengeja sebelas nama di dalam badannya sendiri — `"dzikir.json"`,
 * `"adzan.json"`, `"kota.json"`, dan seterusnya — dan **tidak ada apa pun** yang membandingkannya
 * dengan berkas yang benar-benar dikirim. Mengganti nama berkas di aset, atau salah mengejanya di
 * Kotlin, tetap lolos seluruh uji dan lolos CI: kegagalannya baru muncul di perangkat, saat layar itu
 * dibuka (`FileNotFoundException` dari `assets.open`), atau — pada lapisan pembaruan — sebagai
 * penimpaan yang **diam-diam diabaikan** sehingga pembaruan yang dilaporkan "terpasang" tidak
 * mengubah apa pun.
 *
 * Dua nama sudah lebih dulu tinggal di [BerkasKonten] beserta penjaganya; kesebelas sisanya kini
 * menyusul. Uji ini menutup tiga hal: namanya hanya dieja di satu berkas, namanya benar-benar ada di
 * aset, dan pembacanya ([ContentRepo]) memang mengambilnya dari sana — bukan mengeja ulang.
 */
class BerkasKontenSatuTempatTest {

    private val berkasKonten = "src/main/java/id/or/kemenag/yasintahlil/core/data/BerkasKonten.kt"
    private val repo = "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt"

    private val akar = File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")

    /** Direktori aset; uji berjalan dari modul `app/`, tetapi akar repo pun diterima. */
    private fun dirKonten(): File = listOf(
        File(KodeSumber.root(), "src/main/assets/content"),
        File("src/main/assets/content"),
    ).firstOrNull { it.isDirectory } ?: error("Direktori assets/content tidak ditemukan")

    private fun namaDiAset(): Set<String> =
        dirKonten().listFiles { f -> f.extension == "json" }?.map { it.name }?.toSet().orEmpty()

    /**
     * Kode [f] tanpa komentar, **string dipertahankan** — literal nama berkas justru yang dicari.
     *
     * KDoc di beberapa berkas menyebut nama berkas untuk menjelaskan dari mana asalnya; penjaga yang
     * membaca mentah akan menuduh berkas itu mengeja ulang padahal hanya menjelaskan.
     */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun kodeBerkas(relatif: String): String = kode(File(KodeSumber.root(), relatif))

    /** Seluruh nama berkas konten yang dibaca aplikasi, menurut aturan bersama. */
    private val semua: List<String> get() = BerkasKonten.SEMUA

    // --- Nama hanya boleh tinggal di satu tempat -------------------------------------------------

    @Test
    fun tidakAdaBerkasLainYangMengejaNamaBerkasKonten() {
        val pelanggar = akar.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "BerkasKonten.kt" }
            .mapNotNull { f ->
                // Nama dicari **apa adanya**, tanpa tanda kutip pengapit: di dalam sebuah string
                // Kotlin, kutipnya ditulis `\"`, sehingga pola berkutip justru melewatkan ejaan
                // yang paling sering muncul — contoh bundel pada layar pembaruan konten.
                val lokal = semua.filter { kode(f).contains(it) }
                if (lokal.isEmpty()) null else "${f.name} → $lokal"
            }
            .toList()
        assertTrue(
            "nama berkas konten ditulis ulang di luar BerkasKonten — salinan yang menyimpang " +
                "membuat pembaca mencari berkas yang tidak ada, tanpa galat saat dibangun: $pelanggar",
            pelanggar.isEmpty(),
        )
    }

    /** Sebaliknya, pembaca konten memang harus lewat [BerkasKonten] untuk setiap berkasnya. */
    @Test
    fun contentRepoMembacaSetiapNamaDariBerkasKonten() {
        val k = kodeBerkas(repo)
        val konstantanya = listOf(
            "YASIN_FULL", "TAHLIL_FULL", "TAHLIL_SUSUNAN",
            "SHOLAT_DAFTAR", "SHOLAT_BACAAN", "SHOLAT_MAYIT", "SHOLAT_SYARAT",
            "WUDHU", "DZIKIR", "ADZAN", "KOTA",
        )
        val kurang = konstantanya.filterNot { k.contains("BerkasKonten.$it") }
        assertTrue("ContentRepo tidak membaca nama berkas lewat BerkasKonten: $kurang", kurang.isEmpty())
    }

    /** Ejaan di [BerkasKonten] sendiri harus lengkap: nama kosong atau kembar membuat penjaga buta. */
    @Test
    fun daftarSemuaSejajarDenganKonstanta() {
        assertEquals(
            "SEMUA tidak lagi memuat seluruh nama berkas konten",
            listOf(
                BerkasKonten.YASIN_FULL, BerkasKonten.TAHLIL_FULL, BerkasKonten.TAHLIL_SUSUNAN,
                BerkasKonten.SHOLAT_DAFTAR, BerkasKonten.SHOLAT_BACAAN, BerkasKonten.SHOLAT_MAYIT,
                BerkasKonten.SHOLAT_SYARAT, BerkasKonten.WUDHU, BerkasKonten.DZIKIR,
                BerkasKonten.ADZAN, BerkasKonten.KOTA,
                BerkasKonten.SHOLAT_GHAIB, BerkasKonten.SHOLAT_JENAZAH,
            ),
            semua,
        )
        assertTrue("ada nama berkas yang kosong", semua.none { it.isBlank() })
        assertTrue("ada nama berkas yang tidak berakhiran .json", semua.all { it.endsWith(".json") })
        assertEquals("ada nama berkas yang kembar", semua.size, semua.toSet().size)
    }

    /** Modul sholat khusus pun harus berasal dari daftar yang sama, bukan daftar kedua. */
    @Test
    fun modulSholatKhususMemakaiNamaDariDaftarSemua() {
        val namaModul = BerkasKonten.SHOLAT_KHUSUS.map { it.berkas }
        assertTrue("modul sholat khusus memakai nama di luar SEMUA", semua.containsAll(namaModul))
    }

    /** Id seluruh entri `sholat_daftar.json` — dari kedua bagiannya, `wajib` dan `sunnah`. */
    private fun idDiDaftarSholat(): Set<String> {
        val d = JSONObject(File(dirKonten(), BerkasKonten.SHOLAT_DAFTAR).readText())
        return listOf("wajib", "sunnah").flatMap { bagian ->
            val arr = d.optJSONArray(bagian) ?: return@flatMap emptyList()
            (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
        }.toSet()
    }

    /**
     * Id entri daftar yang membuka tiap modul sholat khusus harus benar-benar ada di
     * `sholat_daftar.json`.
     *
     * Id itu adalah **kunci** yang menyambung berkas konten dengan layar: `SholatScreen` menawarkan
     * tombol "Buka" kepada entri yang id-nya tercatat di [BerkasKonten.SHOLAT_KHUSUS]. Selama id-nya
     * dieja di dalam layar, menggantinya di bundel — karena entri itu dinamai ulang, misalnya —
     * **diam-diam** menghapus tombol itu: entri tersebut lalu jatuh ke tombol "Tata cara" yang
     * membuka halaman rincian tanpa isi, dan tak satu pun uji gagal. Uji ini menutupnya dari sisi
     * lain: yang menunjuk harus benar-benar ada.
     *
     * Daftar yang menunjuk id kembar pun ditolak — dua modul dengan id sama berarti salah satunya
     * tidak pernah dapat dibuka, sebab `firstOrNull` hanya mengembalikan yang pertama.
     */
    @Test
    fun idDaftarModulSholatKhususBenarBenarAdaDiDaftar() {
        val khusus = BerkasKonten.SHOLAT_KHUSUS.map { it.idDaftar }
        assertTrue("modul sholat khusus tanpa id daftar", khusus.none { it.isBlank() })
        assertEquals("ada id daftar kembar pada modul sholat khusus", khusus.size, khusus.toSet().size)
        assertTrue("ada modul sholat khusus tanpa nama layar",
            BerkasKonten.SHOLAT_KHUSUS.none { it.nama.isBlank() })

        val id = idDiDaftarSholat()
        assertTrue("id entri sholat tidak terbaca dari sholat_daftar.json", id.size >= 5)
        val hilang = khusus.filterNot { it in id }
        assertTrue(
            "id daftar modul sholat khusus tidak ada di sholat_daftar.json — tombol \"Buka\" bagi " +
                "entri itu diam-diam hilang dan entri jatuh ke halaman rincian tanpa isi: $hilang",
            hilang.isEmpty(),
        )
    }

    // --- Nama harus menunjuk berkas yang benar-benar dikirim -------------------------------------

    /**
     * Setiap nama yang dibaca aplikasi wajib ada di `assets/content`.
     *
     * Inilah pemeriksaan yang dahulu tidak ada sama sekali: berkas yang diganti namanya di aset
     * membuat layarnya gagal saat dibuka pengguna, sementara seluruh uji tetap hijau.
     */
    @Test
    fun setiapNamaBerkasBenarBenarAdaDiAset() {
        val aset = namaDiAset()
        assertTrue("tidak ada berkas konten di assets/content", aset.isNotEmpty())
        val hilang = semua.filterNot { it in aset }
        assertTrue("nama berkas yang dibaca aplikasi tidak ada di assets/content: $hilang", hilang.isEmpty())
    }

    /**
     * Berkas aset di luar [BerkasKonten.SEMUA] harus benar-benar **dirujuk** oleh berkas konten lain.
     *
     * Lima berkas (`surah_001/112/113/114.json`, `ayat_kursi.json`) tidak dibaca langsung oleh
     * Kotlin, melainkan lewat rujukan `berkas.json#id` di dalam dokumen konten. Daftarnya dipaku di
     * sini supaya berkas aset yang tak seorang pun membaca — ikut terkirim di APK, tetapi tak pernah
     * tampil — tidak dapat bertambah diam-diam; dan supaya nama yang keliru dihapus dari [SEMUA]
     * tidak berlalu sebagai "memang berkas rujukan".
     */
    @Test
    fun berkasAsetDiLuarSemuaHanyaBerkasRujukan() {
        val sisa = namaDiAset() - semua.toSet()
        assertEquals(
            "berkas aset di luar BerkasKonten.SEMUA berubah: berkas baru yang tak seorang pun " +
                "membaca, atau nama yang hilang dari SEMUA",
            setOf(
                "surah_001.json", "surah_112.json", "surah_113.json", "surah_114.json",
                "ayat_kursi.json",
            ),
            sisa,
        )
        val seluruhTeks = dirKonten().listFiles { f -> f.extension == "json" }
            ?.joinToString("\n") { it.readText() }.orEmpty()
        val takDirujuk = sisa.filterNot { seluruhTeks.contains("\"$it\"") }
        assertTrue(
            "berkas aset yang tidak dibaca Kotlin dan tidak dirujuk berkas konten mana pun — " +
                "ikut terkirim di APK tanpa pernah tampil: $takDirujuk",
            takDirujuk.isEmpty(),
        )
    }
}
