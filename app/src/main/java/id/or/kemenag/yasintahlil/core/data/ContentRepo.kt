package id.or.kemenag.yasintahlil.core.data

import android.content.Context
import android.util.Log
import id.or.kemenag.yasintahlil.core.util.ContentValidator
import org.json.JSONObject
import java.io.File

/**
 * Repository offline-first: konten dibaca dari assets/content (sumber resmi), dengan lapisan
 * pembaruan opsional di `filesDir/content_update/` (lihat [ContentUpdateManager]).
 *
 * Lapisan pembaruan hanya dipakai bila berkasnya ada **dan** masih lolos pemeriksaan sumber:
 * berkas yang rusak, hilang, atau kehilangan `sumber_url` resmi otomatis diabaikan sehingga
 * aplikasi kembali membaca konten bawaan yang sudah diverifikasi.
 */
class ContentRepo(private val ctx: Context) {
    private val cache = mutableMapOf<String, JSONObject>()
    private val updateDir = File(ctx.filesDir, ContentUpdateManager.DIR)
    private val metaPembaruan = File(updateDir, ContentUpdateManager.META)

    /** Cap terakhir pembaruan; berubahnya cap membatalkan cache agar tak menyajikan teks lama. */
    private var cap = metaPembaruan.lastModified()

    private fun doc(file: String): JSONObject {
        val capKini = metaPembaruan.lastModified()
        if (capKini != cap) {
            cache.clear()
            cap = capKini
        }
        return cache.getOrPut(file) {
            dariPembaruan(file) ?: dariAset(file)
        }
    }

    /** Dokumen dari lapisan pembaruan, atau null bila tak ada / tak lagi layak dipakai. */
    private fun dariPembaruan(file: String): JSONObject? {
        val f = File(updateDir, file)
        if (!f.isFile) return null
        val json = runCatching { JSONObject(f.readText()) }.getOrElse {
            Log.w("ContentRepo", "Konten pembaruan $file rusak — memakai bawaan", it)
            return null
        }
        val src = json.optString("sumber_url", "")
        if (src.isBlank() || !ContentValidator.isOfficialSource(src)) {
            Log.w("ContentRepo", "Konten pembaruan $file tanpa sumber_url resmi — memakai bawaan")
            return null
        }
        return json
    }

    private fun dariAset(file: String): JSONObject {
        val json = JSONObject(ctx.assets.open("content/$file").bufferedReader().readText())
        // Penjaga runtime: ingatkan bila konten kehilangan atribusi sumber resmi.
        val src = json.optString("sumber_url", "")
        if (src.isBlank() || !ContentValidator.isOfficialSource(src)) {
            Log.w("ContentRepo", "Konten $file tanpa sumber_url resmi — periksa SOURCES.md")
        }
        return json
    }

    private fun verse(o: JSONObject) = Verse(
        o.getInt("nomor"), o.getString("arab"),
        o.optString("latin"), o.optString("terjemahan")
    )

    fun surah(file: String): Surah {
        val d = doc(file)
        val arr = d.getJSONArray("verses")
        return Surah(
            d.optString("surah", file), d.optInt("ayat_count", arr.length()),
            (0 until arr.length()).map { verse(arr.getJSONObject(it)) },
            d.optString("sumber_terjemahan", ""),
            d.optString("kategori", ""),
        )
    }

    val yasin: Surah get() = surah("yasin_full.json")

    /**
     * Teks Arab/Latin/terjemahan dari sebuah berkas rujukan.
     *
     * Nama berkas boleh diikuti `#id` untuk menunjuk **satu entri** di dalam berkas yang memuat
     * daftar (`bagian` pada tahlil, `bacaan` pada bacaan sholat), mis.
     * `sholat_mayit.json#doa_mayit`. Dengan begitu teks yang sama dapat dipakai di beberapa modul
     * tanpa disalin ulang — salinan yang menyimpang adalah cara paling halus untuk membuat dua
     * bacaan berbeda dari satu sumber yang sama.
     */
    fun resolveArabLatinTerjemah(rujukanFile: String?): Triple<String, String, String> {
        if (rujukanFile.isNullOrBlank()) return Triple("", "", "")
        val pisah = rujukanFile.split("#", limit = 2)
        val nama = pisah[0].trim()
        if (nama.isBlank()) return Triple("", "", "")
        val d = doc(nama)
        val id = pisah.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        if (id != null) {
            val arr = d.optJSONArray("bagian") ?: d.optJSONArray("bacaan") ?: return Triple("", "", "")
            val o = (0 until arr.length()).map { arr.getJSONObject(it) }
                .firstOrNull { it.optString("id") == id } ?: return Triple("", "", "")
            return Triple(o.optString("arab"), o.optString("latin"), o.optString("terjemahan"))
        }
        return if (d.has("verses")) {
            val arr = d.getJSONArray("verses")
            Triple(
                (0 until arr.length()).joinToString("\n") { i ->
                    val o = arr.getJSONObject(i); "${o.getInt("nomor")}. ${o.getString("arab")}" },
                (0 until arr.length()).joinToString("\n") { i ->
                    val o = arr.getJSONObject(i); "${o.getInt("nomor")}. ${o.optString("latin")}" },
                (0 until arr.length()).joinToString("\n") { i ->
                    val o = arr.getJSONObject(i); "${o.getInt("nomor")}. ${o.optString("terjemahan")}" }
            )
        } else Triple(d.optString("arab"), d.optString("latin"), d.optString("terjemahan"))
    }

    fun tahlil(susunanId: String? = null): List<TahlilItem> {
        val d = doc("tahlil_full.json")
        val arr = d.getJSONArray("bagian")
        val def = d.optString("kategori", "")
        val all = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val (a, l, t) = resolveArabLatinTerjemah(o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") })
            TahlilItem(
                o.getString("id"), o.getString("judul"),
                o.optString("arab", a), o.optString("latin", l), o.optString("terjemahan", t),
                o.optInt("ulangi", 1),
                o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") },
                o.optString("sumber", ""),
                o.optString("kategori", def)
            )
        }
        if (susunanId == null) return all
        val order = tahlilSusunan().firstOrNull { it.id == susunanId }?.bagian ?: return all
        val byId = all.associateBy { it.id }
        return order.mapNotNull { byId[it] }.ifEmpty { all }
    }

    /** Daftar susunan tahlil yang tersedia (mis. "lazim Indonesia"). */
    fun tahlilSusunan(): List<TahlilSusunan> {
        val arr = doc("tahlil.json").optJSONArray("susunan") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val b = o.optJSONArray("bagian") ?: org.json.JSONArray()
            TahlilSusunan(
                o.getString("id"), o.optString("nama", o.getString("id")),
                (0 until b.length()).map { b.getString(it) }
            )
        }
    }

    fun sholatWajib(): List<SholatInfo> = sholatList("wajib")
    fun sholatSunnah(): List<SholatInfo> = sholatList("sunnah")

    private fun sholatList(key: String): List<SholatInfo> {
        val d = doc("sholat_daftar.json")
        val defKategori = d.optString("kategori", "")
        val arr = d.getJSONArray(key)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val n = o.optJSONObject("niat")
            val ket = o.optJSONArray("ketentuan")?.let { k -> (0 until k.length()).map { k.getString(it) } }.orEmpty()
            SholatInfo(
                o.getString("id"), o.getString("nama"), o.getString("rakaat"),
                o.getString("waktu"), o.getString("deskripsi"),
                n?.let {
                    Niat(it.getString("arab"), it.getString("latin"), it.getString("terjemahan"),
                        it.optString("kategori", defKategori))
                },
                o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") },
                ketentuan = ket,
                niatCatatan = o.optString("niat_catatan", "")
            )
        }
    }

    fun sholatBacaan(): List<Bacaan> = parseBacaan("sholat_bacaan.json")

    /** Bacaan doa mayit (dipakai sholat jenazah & ghaib) — berkas terpisah agar tidak
     *  tercampur ke daftar langkah sholat umum. */
    fun sholatMayitBacaan(): List<Bacaan> = parseBacaan("sholat_mayit.json")

    private fun parseBacaan(file: String): List<Bacaan> {
        val d = doc(file)
        val defKategori = d.optString("kategori", "")
        val arr = d.getJSONArray("bacaan")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val (a, l, t) = resolveArabLatinTerjemah(o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") })
            val scope = o.optJSONArray("sholat")?.let { s -> (0 until s.length()).map { s.getString(it) } }.orEmpty()
            Bacaan(o.getString("id"), o.getString("judul"), o.optString("kapan"),
                o.optString("arab", a), o.optString("latin", l), o.optString("terjemahan", t),
                o.optString("sumber"),
                o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") },
                catatan = o.optString("catatan", ""),
                mazhab = o.optString("mazhab", "").takeIf { it.isNotBlank() },
                sholatScope = scope,
                kategori = o.optString("kategori", defKategori))
        }
    }

    fun dzikir(): List<DzikirGroup> {
        val d = doc("dzikir.json")
        val defKategori = d.optString("kategori", "")
        val arr = d.getJSONArray("kelompok")
        return (0 until arr.length()).map { gi ->
            val g = arr.getJSONObject(gi)
            val items = g.getJSONArray("items")
            DzikirGroup(g.getString("id"), g.getString("judul"),
                (0 until items.length()).map { i ->
                    val o = items.getJSONObject(i)
                    val ruj = mutableListOf<String>()
                    if (o.has("rujukan")) { val ra = o.getJSONArray("rujukan"); repeat(ra.length()) { ruj += ra.getString(it) } }
                    val singles = mutableListOf<String?>()
                    o.optString("rujukanFile", null).takeIf { o.has("rujukanFile") }?.let { singles += it }
                    o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") }?.let { singles += it }
                    val all = (singles.filterNotNull() + ruj)
                    val joined = all.map { resolveArabLatinTerjemah(it) }
                    val ja = joined.joinToString("\n\n") { it.first }.ifBlank { o.optString("arab") }
                    val jl = joined.joinToString("\n\n") { it.second }.ifBlank { o.optString("latin") }
                    val jt = joined.joinToString("\n\n") { it.third }.ifBlank { o.optString("terjemahan") }
                    DzikirItem(o.getString("judul"), o.optString("arab", ja),
                        o.optString("latin", jl), o.optString("terjemahan", jt),
                        o.optInt("ulangi", 1), o.optString("sumber"),
                        all.firstOrNull(), all.drop(1),
                        o.optString("kategori", defKategori))
                })
        }
    }

    fun adzan(): List<AdzanLafaz> {
        val d = doc("adzan.json")
        val def = d.optString("kategori", "")
        val arr = d.getJSONObject("adzan").getJSONArray("daftar")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AdzanLafaz(o.getString("arab"), o.getString("latin"), o.optString("terjemahan"),
                o.optInt("ulangi", 1), o.optString("kategori", def))
        }
    }

    fun adzanCatatan(): String = doc("adzan.json").getJSONObject("adzan").optString("catatan_subuh")

    /** Catatan perbedaan susunan iqomah antar mazhab/tradisi — ditampilkan apa adanya. */
    fun iqomahCatatan(): String = doc("adzan.json").optString("catatan_mazhab")

    fun iqomah(): List<AdzanLafaz> {
        val d = doc("adzan.json")
        val def = d.optString("kategori", "")
        val arr = d.getJSONObject("iqomah").getJSONArray("daftar")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AdzanLafaz(o.getString("arab"), o.getString("latin"), o.optString("terjemahan"),
                o.optInt("ulangi", 1), o.optString("kategori", def))
        }
    }

    fun doaAdzan(): AdzanDoa = adzanDoa("doa_setelah_adzan")

    /** Entri adzan.json sebagai [AdzanDoa] — kategori disimpulkan dari tingkat berkas bila kosong. */
    private fun adzanDoa(key: String): AdzanDoa {
        val d = doc("adzan.json")
        val o = d.getJSONObject(key)
        return AdzanDoa(o.getString("judul"), o.getString("arab"), o.getString("latin"),
            o.optString("terjemahan"), o.optString("catatan"), o.optString("sumber"),
            o.optString("kategori", d.optString("kategori", "")))
    }

    /** Lafaz menjawab adzan beserta catatan kapan lafaz ini dibaca. */
    fun menjawabAdzan(): AdzanDoa = adzanDoa("menjawab_adzan")

    /** Sholawat yang dibaca setelah menjawab adzan (sebelum doa setelah adzan). */
    fun sholawatAdzan(): AdzanDoa = adzanDoa("sholawat_setelah_adzan")

    fun kota(): List<Kota> {
        val arr = doc("kota.json").getJSONArray("kota")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i); Kota(o.getString("nama"), o.getDouble("lat"), o.getDouble("lon"))
        }
    }

    /** Panduan bersuci (wudhu, tayamum, pembatal) — langkah prosedural tanpa teks Arab. */
    fun panduanBersuci(): PanduanBersuci {
        val d = doc("wudhu.json")
        fun langkah(key: String): List<Langkah> {
            val arr = d.optJSONObject(key)?.optJSONArray("langkah") ?: return emptyList()
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Langkah(o.optInt("no", i + 1), o.getString("judul"), o.optString("keterangan"))
            }
        }
        fun stringList(key: String): List<String> {
            val arr = d.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }
        return PanduanBersuci(
            rukun = stringList("rukun"),
            wudhu = langkah("wudhu"),
            tayamum = langkah("tayamum"),
            pembatal = stringList("pembatal"),
            catatanMazhab = d.optString("catatan_mazhab"),
            catatanLafaz = d.optString("catatan_lafaz"),
        )
    }

    /** Konten sholat khusus (ghaib/jenazah) dari assets — termasuk varian niat. */
    fun sholatKhusus(file: String): SholatKhusus {
        val d = doc(file)
        fun stringList(key: String): List<String> {
            val arr = d.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }
        val niatArr = d.optJSONArray("niat") ?: org.json.JSONArray()
        val defKategori = d.optString("kategori", "")
        val niat = (0 until niatArr.length()).map { i ->
            val o = niatArr.getJSONObject(i)
            Bacaan(
                o.optString("id", "niat_$i"), o.optString("judul"),
                o.optString("kapan"), o.optString("arab"), o.optString("latin"),
                o.optString("terjemahan"), o.optString("sumber"),
                catatan = o.optString("catatan"),
                kategori = o.optString("kategori", defKategori),
            )
        }
        return SholatKhusus(
            judul = d.optString("judul"),
            hukum = d.optString("hukum"),
            takbir = d.optInt("takbir", 4),
            dalil = d.optString("dalil"),
            catatanMazhab = d.optString("catatan_mazhab"),
            syarat = stringList("syarat"),
            niat = niat,
            langkah = stringList("langkah"),
        )
    }
}
