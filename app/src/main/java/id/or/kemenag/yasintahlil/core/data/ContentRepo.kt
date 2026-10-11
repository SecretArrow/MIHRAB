package id.or.kemenag.yasintahlil.core.data

import android.content.Context
import android.util.Log
import id.or.kemenag.yasintahlil.core.util.ContentValidator
import org.json.JSONObject

/** Repository offline-first: seluruh konten dibaca dari assets/content (sumber resmi). */
class ContentRepo(private val ctx: Context) {
    private val cache = mutableMapOf<String, JSONObject>()
    private fun doc(file: String): JSONObject =
        cache.getOrPut(file) {
            val json = JSONObject(ctx.assets.open("content/$file").bufferedReader().readText())
            // Penjaga runtime: ingatkan bila konten kehilangan atribusi sumber resmi.
            val src = json.optString("sumber_url", "")
            if (src.isBlank() || !ContentValidator.isOfficialSource(src)) {
                Log.w("ContentRepo", "Konten $file tanpa sumber_url resmi — periksa SOURCES.md")
            }
            json
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
            d.optString("sumber_terjemahan", "")
        )
    }

    val yasin: Surah get() = surah("yasin_full.json")

    fun resolveArabLatinTerjemah(rujukanFile: String?): Triple<String, String, String> {
        if (rujukanFile == null) return Triple("", "", "")
        val d = doc(rujukanFile)
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
        val arr = doc("tahlil_full.json").getJSONArray("bagian")
        val all = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val (a, l, t) = resolveArabLatinTerjemah(o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") })
            TahlilItem(
                o.getString("id"), o.getString("judul"),
                o.optString("arab", a), o.optString("latin", l), o.optString("terjemahan", t),
                o.optInt("ulangi", 1),
                o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") },
                o.optString("sumber", "")
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
        val arr = doc("sholat_daftar.json").getJSONArray(key)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val n = o.optJSONObject("niat")
            SholatInfo(
                o.getString("id"), o.getString("nama"), o.getString("rakaat"),
                o.getString("waktu"), o.getString("deskripsi"),
                n?.let { Niat(it.getString("arab"), it.getString("latin"), it.getString("terjemahan")) },
                o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") }
            )
        }
    }

    fun sholatBacaan(): List<Bacaan> {
        val arr = doc("sholat_bacaan.json").getJSONArray("bacaan")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val (a, l, t) = resolveArabLatinTerjemah(o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") })
            Bacaan(o.getString("id"), o.getString("judul"), o.optString("kapan"),
                o.optString("arab", a), o.optString("latin", l), o.optString("terjemahan", t),
                o.optString("sumber"),
                o.optString("rujukan_file", null).takeIf { o.has("rujukan_file") })
        }
    }

    fun dzikir(): List<DzikirGroup> {
        val arr = doc("dzikir.json").getJSONArray("kelompok")
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
                        all.firstOrNull(), all.drop(1))
                })
        }
    }

    fun adzan(): List<AdzanLafaz> {
        val arr = doc("adzan.json").getJSONObject("adzan").getJSONArray("daftar")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AdzanLafaz(o.getString("arab"), o.getString("latin"), o.optString("terjemahan"), o.optInt("ulangi", 1))
        }
    }

    fun adzanCatatan(): String = doc("adzan.json").getJSONObject("adzan").optString("catatan_subuh")
    fun iqomah(): List<AdzanLafaz> {
        val arr = doc("adzan.json").getJSONObject("iqomah").getJSONArray("daftar")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AdzanLafaz(o.getString("arab"), o.getString("latin"), o.optString("terjemahan"), o.optInt("ulangi", 1))
        }
    }

    fun doaAdzan(): Triple<String, String, String> {
        val o = doc("adzan.json").getJSONObject("doa_setelah_adzan")
        return Triple(o.getString("arab"), o.getString("latin"), o.getString("terjemahan"))
    }

    fun kota(): List<Kota> {
        val arr = doc("kota.json").getJSONArray("kota")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i); Kota(o.getString("nama"), o.getDouble("lat"), o.getDouble("lon"))
        }
    }
}
