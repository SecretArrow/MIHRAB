package id.or.kemenag.yasintahlil.core.data

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Pustaka Doa dan Dzikir" meminta koleksi itu memuat *"Doa untuk orang yang
 * meninggal"*. Doa jenazah sudah ada dan terverifikasi di `sholat_mayit.json` (dipakai sholat
 * jenazah & sholat ghaib); menyalin teksnya ke `dzikir.json` akan membuat dua salinan yang bisa
 * menyimpang dari sumber yang sama. Karena itu entri dzikir hanya **menunjuk** berkas rujukan
 * (`berkas#id`, lihat `ContentRepo.resolveArabLatinTerjemah`), dan uji ini menjaga rantainya:
 * entrinya tetap ada, tidak menyimpan salinan teks sendiri, dan id yang dirujuk benar-benar ada.
 */
class DzikirRujukanTest {

    private fun contentDir(): File {
        val candidates = listOf(
            File("src/main/assets/content"),
            File("app/src/main/assets/content"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Direktori assets/content tidak ditemukan dari ${File(".").absolutePath}")
    }

    private fun dokumen(nama: String): JSONObject = JSONObject(File(contentDir(), nama).readText())

    private fun entriDzikir(judul: String): JSONObject {
        val arr = dokumen("dzikir.json").getJSONArray("kelompok")
        for (gi in 0 until arr.length()) {
            val items = arr.getJSONObject(gi).getJSONArray("items")
            for (ii in 0 until items.length()) {
                val o = items.getJSONObject(ii)
                if (o.optString("judul") == judul) return o
            }
        }
        error("Entri dzikir '$judul' tidak ditemukan di dzikir.json")
    }

    @Test
    fun doaUntukOrangYangMeninggalAdaDiPustakaDzikir() {
        val o = entriDzikir("Doa untuk orang yang meninggal")
        assertTrue("entri tanpa sumber per-entri", o.optString("sumber").isNotBlank())
        assertTrue("entri tidak menunjuk berkas rujukan", o.optString("rujukan_file").isNotBlank())
    }

    /** Teks tidak boleh disalin: salinan inline adalah cara paling halus untuk menyimpang. */
    @Test
    fun entriRujukanTidakMenyimpanSalinanTeksSendiri() {
        listOf("Doa untuk orang yang meninggal", "Doa tambahan untuk jenazah").forEach { judul ->
            val o = entriDzikir(judul)
            listOf("arab", "latin", "terjemahan").forEach { k ->
                assertTrue(
                    "'$judul' menyimpan '$k' salinan sendiri padahal sudah menunjuk rujukan",
                    !o.has(k) || o.optString(k).isBlank(),
                )
            }
        }
    }

    /** Rujukan `berkas#id` harus menunjuk entri yang benar-benar ada — bukan tautan mati. */
    @Test
    fun rujukanMenunjukEntriYangBenarBenarAda() {
        listOf("Doa untuk orang yang meninggal", "Doa tambahan untuk jenazah").forEach { judul ->
            val rujukan = entriDzikir(judul).getString("rujukan_file")
            val pisah = rujukan.split("#", limit = 2)
            assertTrue("rujukan '$rujukan' tanpa '#id'", pisah.size == 2 && pisah[1].isNotBlank())
            val target = dokumen(pisah[0])
            val arr = target.optJSONArray("bagian") ?: target.optJSONArray("bacaan")
                ?: error("berkas rujukan ${pisah[0]} tak memuat 'bagian'/'bacaan'")
            val ids = (0 until arr.length()).map { arr.getJSONObject(it).optString("id") }
            assertTrue(
                "'$judul' merujuk '${pisah[1]}' yang tidak ada di ${pisah[0]}",
                ids.contains(pisah[1]),
            )
        }
    }
}
