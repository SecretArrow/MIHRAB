package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Rujukan berkas pada konten **bawaan** harus hidup, dan diperiksa oleh aturan yang sama dengan
 * rujukan pada bundel pembaruan.
 *
 * Sebuah entri konten dapat menunjuk berkas lain (`rujukan_file`, `rujukanFile`, larik `rujukan`)
 * alih-alih menyalin teksnya — supaya satu lafaz terverifikasi dipakai beberapa modul tanpa salinan
 * yang bisa menyimpang. Aturan bahwa rujukan itu harus menunjuk berkas yang benar-benar ada sudah
 * lama ditulis di [BundelKonten.periksaRujukan], dan aplikasi menjalankannya **saat memasang**
 * bundel yang dipilih pengguna.
 *
 * Yang tidak pernah dijalankan atas berkas yang benar-benar dikirim adalah aturan yang sama.
 * `DzikirRujukanTest` memeriksa rujukan `dzikir.json` saja, dan hanya yang berbentuk `berkas#id`;
 * dari **dua puluh** nilai rujukan yang ada di `assets/content` hari ini, **lima** yang diperiksa —
 * kelimanya pun milik `dzikir.json`. `tools/validate_content.py` tidak mengenal kunci rujukan sama
 * sekali, sehingga berkas yang benar-benar dikirim tak pernah diperiksa oleh satu pun gerbang.
 * Rujukan yang mati tidak memunculkan galat apa pun: `ContentRepo.resolveArabLatinTerjemah` membaca
 * sasarannya lewat `docAtauNull` dan mengembalikan teks kosong, lalu pemanggilnya jatuh ke
 * `o.optString("arab", "")` yang juga kosong — entri tampil **tanpa lafaz, tanpa satu pun pesan**.
 * Pada `tahlil_full.json` seluruh lafaz Qur'annya datang lewat rujukan seperti itu, dan pada
 * `dzikir.json` kelompok "Doa memohon ampunan" dibangun dari larik `rujukan` yang sama sekali tidak
 * pernah diperiksa.
 *
 * Karena itu uji ini tidak menuliskan aturannya lagi: ia **memanggil pemeriksa yang sama** yang
 * dipakai saat memasang bundel, dengan seluruh berkas bawaan sebagai dokumen sekaligus sebagai
 * daftar sasaran yang sah. Menambah rujukan baru berarti ikut diperiksa tanpa menyunting uji ini.
 *
 * Satu rujukan sengaja tidak menghasilkan lafaz: `sholat_daftar.json` menunjuk `sholat_ghaib.json`,
 * yang hanya memuat keterangan tingkat modul (tanpa `arab`/`verses`). Rujukan itu sah dan
 * sasarannya tetap diperiksa di sini; jangan menambahkan syarat "setiap rujukan harus menghasilkan
 * teks" — justru rujukan inilah yang akan menggagalkannya.
 */
class RujukanKontenBawaanTest {

    private fun dirKonten(): File = listOf(
        File(KodeSumber.root(), "src/main/assets/content"),
        File("src/main/assets/content"),
    ).firstOrNull { it.isDirectory } ?: error("Direktori assets/content tidak ditemukan")

    private val berkasKonten: List<File>
        get() = dirKonten().listFiles { f -> f.extension == "json" }?.sortedBy { it.name }.orEmpty()

    private fun dokumen(f: File): JSONObject = JSONObject(f.readText())

    private fun hasilDari(berkas: List<File>): BundelKonten.Hasil =
        BundelKonten.Hasil(berkas = berkas.associate { it.name to dokumen(it) })

    /** Bundel buatan dari pasangan nama → teks dokumen, untuk menguji pemeriksanya sendiri. */
    private fun hasil(vararg pasangan: Pair<String, String>): BundelKonten.Hasil =
        BundelKonten.Hasil(berkas = pasangan.associate { it.first to JSONObject(it.second) })

    // --- Aturan yang sama, dijalankan atas konten yang benar-benar dikirim -----------------------

    @Test
    fun setiapRujukanPadaKontenBawaanHidup() {
        val berkas = berkasKonten
        assertTrue("tidak ada berkas konten di assets/content", berkas.size >= 15)
        val nama = berkas.map { it.name }.toSet()
        val galat = BundelKonten.periksaRujukan(hasilDari(berkas), nama)
        assertTrue(
            "rujukan pada konten bawaan menunjuk berkas atau entri yang tidak ada — entri itu " +
                "tampil tanpa lafaz dan tanpa satu pun pesan: $galat",
            galat.isEmpty(),
        )
    }

    /**
     * Pemeriksa itu harus benar-benar menelusuri **setiap** berkas, bukan berhenti di yang pertama.
     * Bundel yang hanya berisi sebagian berkas membuat uji di atas hijau tanpa memeriksa apa pun.
     */
    @Test
    fun pemeriksaDijalankanAtasSeluruhBerkasKonten() {
        val berkas = berkasKonten
        val hasil = hasilDari(berkas)
        assertEquals("tidak seluruh berkas konten ikut diperiksa",
            berkas.map { it.name }.toSet(), hasil.berkas.keys)
    }

    // --- Bukti bahwa pemeriksaannya benar-benar menggigit ---------------------------------------

    /**
     * Bukti perilaku: rujukan ke berkas yang tidak ada harus dilaporkan.
     *
     * Tanpa uji ini, uji di atas tetap hijau meski `periksaRujukan` berhenti memeriksa apa pun —
     * dan gate yang tidak menggigit lebih buruk daripada gate yang tidak ada, sebab ia menjanjikan
     * pemeriksaan yang tidak pernah berjalan.
     */
    @Test
    fun pemeriksaMenolakRujukanBerkasYangTidakAda() {
        val galat = BundelKonten.periksaRujukan(
            hasil(
                "dzikir.json" to """{"kelompok":[{"id":"g","items":[
                    {"judul":"Entri","rujukan_file":"tidak_ada.json"}]}]}""",
            ),
            setOf("dzikir.json"),
        )
        assertTrue("rujukan ke berkas yang tidak ada tidak dilaporkan: $galat",
            galat.any { it.contains("tidak_ada.json") && it.contains("tidak ada") })
    }

    /** Larik `rujukan` pun harus diperiksa — bentuk inilah yang dipakai kelompok "ampunan". */
    @Test
    fun pemeriksaMenolakRujukanLarikYangTidakAda() {
        val galat = BundelKonten.periksaRujukan(
            hasil(
                "dzikir.json" to """{"kelompok":[{"id":"g","items":[
                    {"judul":"Entri","rujukan":["surah_999.json"]}]}]}""",
            ),
            setOf("dzikir.json"),
        )
        assertTrue("larik rujukan ke berkas yang tidak ada tidak dilaporkan: $galat",
            galat.any { it.contains("surah_999.json") })
    }

    /** Rujukan `berkas#id` harus memeriksa entri di dalam berkasnya, bukan hanya berkasnya. */
    @Test
    fun pemeriksaMenolakRujukanIdYangTidakAda() {
        val galat = BundelKonten.periksaRujukan(
            hasil(
                "dzikir.json" to """{"kelompok":[{"id":"g","items":[
                    {"judul":"Entri","rujukan_file":"tahlil_full.json#tidak_ada_id"}]}]}""",
                "tahlil_full.json" to """{"bagian":[{"id":"istighfar","judul":"Istighfar"}]}""",
            ),
            setOf("dzikir.json", "tahlil_full.json"),
        )
        assertTrue("rujukan ke entri yang tidak ada tidak dilaporkan: $galat",
            galat.any { it.contains("tidak_ada_id") && it.contains("entri") })
    }

    /**
     * Konten bawaan memang memuat rujukan, dan pemeriksaannya bergantung pada itu.
     *
     * Bila kelak seluruh rujukan dihapus dari bundel, uji-uji di atas tetap hijau — bukan karena
     * rujukannya sehat, melainkan karena tidak ada lagi yang diperiksa. Daftar berkas di bawah
     * membuat perubahan itu terlihat, sekaligus mencatat berkas mana yang paling bergantung pada
     * rujukan.
     *
     * Kuncinya dicari sebagai **kunci JSON** (`"rujukan_file"`, `"rujukan"`), bukan sebagai kata
     * biasa. `catatan_tradisi` pada `tahlil_full.json` memuat kata "rujukan" di dalam prosa
     * ("…berbeda menurut tradisi dan rujukan…"), sehingga pencarian kata akan hijau meski berkas
     * itu tidak merujuk ke mana pun — persis kegagalan yang seharusnya dicegah uji ini.
     */
    @Test
    fun berkasYangMemakaiRujukanBenarBenarMemuatnya() {
        val kunci = (KUNCI_RUJUKAN + KUNCI_RUJUKAN_DAFTAR).map { "\"$it\"" }
        val pemakai = listOf("dzikir.json", "sholat_bacaan.json", "tahlil_full.json", "sholat_daftar.json")
        val tidakMemuat = pemakai.filterNot { nama ->
            val f = File(dirKonten(), nama)
            f.isFile && kunci.any { f.readText().contains(it) }
        }
        assertTrue("berkas ini tidak lagi memuat rujukan apa pun: $tidakMemuat", tidakMemuat.isEmpty())
    }

    /**
     * Jumlah kunci rujukan per berkas dipaku, sebab menghapus satu rujukan pun tidak menggagalkan
     * pemeriksaan di atas: rujukan yang sudah tidak ada tidak mungkin menunjuk sasaran yang salah.
     * Yang hilang justru **lafaznya** — kelompok "Doa memohon ampunan" pada `dzikir.json` dibangun
     * seluruhnya dari larik `rujukan`, dan menghapus larik itu menyisakan kelompok kosong tanpa
     * satu pun galat. Menambah rujukan berarti memperbarui angka ini; itu disengaja.
     */
    @Test
    fun jumlahRujukanPerBerkasTidakBerkurangDiamDiam() {
        val diharapkan = mapOf(
            "dzikir.json" to 7,
            "sholat_bacaan.json" to 3,
            "sholat_daftar.json" to 1,
            "tahlil_full.json" to 6,
        )
        val kunci = (KUNCI_RUJUKAN + KUNCI_RUJUKAN_DAFTAR).map { "\"$it\"" }
        val nyata = diharapkan.keys.associateWith { nama ->
            val teks = File(dirKonten(), nama).readText()
            kunci.sumOf { k -> Regex(Regex.escape(k) + "\\s*:").findAll(teks).count() }
        }
        assertEquals("jumlah rujukan per berkas konten berubah", diharapkan, nyata)
    }
}
