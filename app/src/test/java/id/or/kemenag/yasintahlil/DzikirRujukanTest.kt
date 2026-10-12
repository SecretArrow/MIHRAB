package id.or.kemenag.yasintahlil.core.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Pustaka Doa dan Dzikir" meminta koleksi itu memuat *"Doa untuk orang yang
 * meninggal"* dan *"Doa memohon ampunan"*. Keduanya sudah ada dan terverifikasi — tetapi tidak
 * sebagai salinan baru: menyalin lafaznya ke `dzikir.json` akan membuat dua teks yang bisa
 * menyimpang dari sumber yang sama. Karena itu entri dzikir hanya **menunjuk** berkas rujukan
 * (`berkas#id`, lihat `ContentRepo.resolveArabLatinTerjemah`), dan uji ini menjaga rantainya:
 * entrinya ada, tidak menyimpan salinan teks sendiri, dan id yang dirujuk benar-benar ada.
 *
 * Pemeriksaannya berlaku untuk **seluruh** entri penunjuk di bundel, bukan daftar judul yang
 * ditulis tetap — supaya kelompok yang ditambahkan kemudian ikut terjaga tanpa menyunting uji.
 */
class DzikirRujukanTest {

    private val arab = Regex("[\\u0600-\\u06FF\\u0750-\\u077F]")

    private fun contentDir(): File {
        val candidates = listOf(
            File("src/main/assets/content"),
            File("app/src/main/assets/content"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Direktori assets/content tidak ditemukan dari ${File(".").absolutePath}")
    }

    private fun dokumen(nama: String): JSONObject = JSONObject(File(contentDir(), nama).readText())

    private fun kelompok(): List<JSONObject> {
        val arr = dokumen("dzikir.json").getJSONArray("kelompok")
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    /** Seluruh entri `dzikir.json`, berpasangan dengan id kelompoknya. */
    private fun semuaEntri(): List<Pair<String, JSONObject>> = kelompok().flatMap { g ->
        val items = g.getJSONArray("items")
        (0 until items.length()).map { g.getString("id") to items.getJSONObject(it) }
    }

    private fun entriDzikir(judul: String): JSONObject =
        semuaEntri().firstOrNull { it.second.optString("judul") == judul }?.second
            ?: error("Entri dzikir '$judul' tidak ditemukan di dzikir.json")

    /** Rujukan berkas mana pun yang dipakai entri ini — `rujukan_file` maupun larik `rujukan`. */
    private fun rujukanDari(o: JSONObject): List<String> {
        val out = mutableListOf<String>()
        if (o.has("rujukan_file")) out += o.getString("rujukan_file")
        o.optJSONArray("rujukan")?.let { ra -> for (i in 0 until ra.length()) out += ra.getString(i) }
        return out.filter { it.isNotBlank() }
    }

    private fun entriPenunjuk(): List<Pair<String, JSONObject>> =
        semuaEntri().filter { rujukanDari(it.second).isNotEmpty() }

    /** Seluruh nilai string pada sebuah simpul JSON — dipakai untuk memindai teks Arab. */
    private fun nilaiTeks(node: Any?): List<String> = when (node) {
        is JSONObject -> node.keys().asSequence().flatMap { nilaiTeks(node.opt(it)) }.toList()
        is JSONArray -> (0 until node.length()).flatMap { nilaiTeks(node.opt(it)) }
        is String -> listOf(node)
        else -> emptyList()
    }

    @Test
    fun doaUntukOrangYangMeninggalAdaDiPustakaDzikir() {
        val o = entriDzikir("Doa untuk orang yang meninggal")
        assertTrue("entri tanpa sumber per-entri", o.optString("sumber").isNotBlank())
        assertTrue("entri tidak menunjuk berkas rujukan", o.optString("rujukan_file").isNotBlank())
    }

    /**
     * `plan.txt` §"Pustaka Doa dan Dzikir" mendaftar *"Doa memohon ampunan"* sebagai koleksi
     * tersendiri. Lafaznya sudah terverifikasi (Istighfar, Sayyidul Istighfar, Subhanallahi wa
     * bihamdih) tetapi tersebar di kelompok lain; tanpa kelompok bernama, pengguna yang mencari
     * "doa ampunan" tidak menemukan tempat yang menjawabnya.
     */
    @Test
    fun doaMemohonAmpunanAdaSebagaiKoleksiBernama() {
        val ampunan = kelompok().firstOrNull { it.getString("id") == "ampunan" }
            ?: error("dzikir.json tidak punya kelompok 'ampunan'")
        assertEquals("judul kelompok ampunan tidak sesuai plan.txt",
            "Doa memohon ampunan", ampunan.getString("judul"))
        assertTrue("kelompok ampunan terlalu sedikit entrinya",
            ampunan.getJSONArray("items").length() >= 3)
    }

    /** Teks tidak boleh disalin: salinan inline adalah cara paling halus untuk menyimpang. */
    @Test
    fun entriPenunjukTidakMenyimpanSalinanTeksSendiri() {
        val pelanggar = entriPenunjuk().filter { (_, o) ->
            listOf("arab", "latin", "terjemahan").any { o.optString(it).isNotBlank() }
        }.map { "${it.first}/${it.second.optString("judul")}" }
        assertTrue("entri penunjuk menyimpan salinan teks sendiri: $pelanggar", pelanggar.isEmpty())
    }

    /**
     * Kelompok "Doa memohon ampunan" tidak boleh memuat satu huruf Arab pun. Bila kelak ada yang
     * menempelkan lafaz langsung ke sana — alih-alih menunjuk entri yang sudah terverifikasi —
     * uji ini gagal. Penolakan itu disengaja: lafaz baru tanpa sumber resmi adalah persis yang
     * dilarang aturan proyek.
     */
    @Test
    fun kelompokAmpunanTanpaTeksArabLangsung() {
        val ampunan = kelompok().first { it.getString("id") == "ampunan" }
        val arabDi = nilaiTeks(ampunan).filter { arab.containsMatchIn(it) }
        assertTrue("kelompok ampunan memuat teks Arab langsung $arabDi — lafaz harus lewat rujukan",
            arabDi.isEmpty())
    }

    /** Setiap entri penunjuk wajib menyebut sumbernya — pengguna berhak tahu hadis mana yang dimaksud. */
    @Test
    fun setiapEntriPenunjukMenyebutSumber() {
        val tanpa = entriPenunjuk().filter { (_, o) -> o.optString("sumber").isBlank() }
            .map { "${it.first}/${it.second.optString("judul")}" }
        assertTrue("entri penunjuk tanpa 'sumber': $tanpa", tanpa.isEmpty())
    }

    /** Rujukan `berkas#id` harus menunjuk entri yang benar-benar ada — bukan tautan mati. */
    @Test
    fun rujukanMenunjukEntriYangBenarBenarAda() {
        val mati = mutableListOf<String>()
        entriPenunjuk().forEach { (grup, o) ->
            rujukanDari(o).filter { it.contains("#") }.forEach { rujukan ->
                val (berkas, id) = rujukan.split("#", limit = 2)
                val target = runCatching { dokumen(berkas.trim()) }.getOrNull()
                if (target == null || entriRujukan(target, id.trim()) == null) {
                    mati += "$grup/${o.optString("judul")} → $rujukan"
                }
            }
        }
        assertTrue("rujukan dzikir menunjuk entri yang tidak ada: $mati", mati.isEmpty())
    }

    /**
     * Rujukan ber-id juga harus **menghasilkan teks yang tampil** — bukan sekadar id yang cocok
     * tetapi entri kosong. Inilah yang membuat pengguna melihat lafaz, bukan kartu kosong, di
     * kelompok "Doa memohon ampunan".
     */
    @Test
    fun rujukanBerIdMenghasilkanTeksYangTampil() {
        val kosong = mutableListOf<String>()
        entriPenunjuk().forEach { (grup, o) ->
            rujukanDari(o).filter { it.contains("#") }.forEach { rujukan ->
                val (berkas, id) = rujukan.split("#", limit = 2)
                val t = runCatching { entriRujukan(dokumen(berkas.trim()), id.trim()) }.getOrNull()
                if (t == null || t.optString("arab").isBlank() || t.optString("terjemahan").isBlank()) {
                    kosong += "$grup/${o.optString("judul")} → $rujukan"
                }
            }
        }
        assertTrue("rujukan dzikir tidak menghasilkan teks: $kosong", kosong.isEmpty())
    }

    /**
     * Setiap entri dzikir punya `id` unik: `rujukan_file` = `dzikir.json#id` hanya sah bila id-nya
     * menunjuk tepat satu entri. Id ganda membuat rujukan diam-diam menunjuk entri yang salah.
     */
    @Test
    fun setiapEntriDzikirPunyaIdUnik() {
        val ids = semuaEntri().map { it.second.optString("id") }
        val tanpaId = semuaEntri().filter { it.second.optString("id").isBlank() }
            .map { it.second.optString("judul") }
        assertTrue("entri dzikir tanpa id: $tanpaId", tanpaId.isEmpty())
        val ganda = ids.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue("id entri dzikir ganda: $ganda", ganda.isEmpty())
    }
}
