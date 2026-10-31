package id.or.kemenag.yasintahlil

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Kunci JSON yang ditulis dua kali dalam satu objek.
 *
 * JSON tidak melarangnya, tetapi setiap parser — termasuk `org.json` yang dipakai aplikasi —
 * hanya menyimpan **yang terakhir**. Nilai pertama hilang tanpa galat, tanpa peringatan, dan tanpa
 * jejak. Itu bukan kerusakan teoretis: `adzan.json` pernah menulis `catatan` dua kali pada entri
 * "Menjawab adzan", sehingga penjelasan lengkap cara menjawab adzan (menjawab lafaz yang sama,
 * kecuali dua seruan *hayya* yang dijawab dengan lafaz lain) tertimpa versi pendek yang tertinggal.
 * Yang dibaca pengguna adalah versi yang lebih sedikit menjelaskan — dan tak ada yang menandainya.
 *
 * Uji ini memindai **teks mentah** berkas konten, karena setelah di-parse jejaknya sudah hilang.
 */
class KontenTanpaKunciGandaTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun berkasKonten(): List<File> =
        File(root(), "src/main/assets/content").listFiles { f -> f.extension == "json" }
            ?.sortedBy { it.name } ?: emptyList()

    /**
     * Kunci yang muncul dua kali dalam satu objek, dari teks JSON mentah.
     *
     * Pemindainya sengaja sederhana: ia membaca token string utuh (sehingga `{`/`}` di dalam teks
     * tidak mengacaukan kedalaman) lalu menandai string yang langsung diikuti `:` sebagai kunci.
     * Pada JSON yang sah tidak ada string **nilai** yang diikuti `:`, jadi penandaan itu tepat.
     */
    private fun kunciGanda(teks: String): List<String> {
        val tumpukan = ArrayDeque<MutableSet<String>>()
        val hasil = mutableListOf<String>()
        var i = 0
        while (i < teks.length) {
            when (val c = teks[i]) {
                '{' -> { tumpukan.addLast(mutableSetOf()); i++ }
                '}' -> { if (tumpukan.isNotEmpty()) tumpukan.removeLast(); i++ }
                '"' -> {
                    val sb = StringBuilder()
                    i++
                    while (i < teks.length && teks[i] != '"') {
                        if (teks[i] == '\\') { sb.append(teks[i]); i++ }
                        if (i < teks.length) { sb.append(teks[i]); i++ }
                    }
                    i++ // tutup kutip
                    var j = i
                    while (j < teks.length && teks[j].isWhitespace()) j++
                    if (j < teks.length && teks[j] == ':' && tumpukan.isNotEmpty()) {
                        val kunci = sb.toString()
                        if (!tumpukan.last().add(kunci)) hasil += kunci
                    }
                }
                else -> i++
            }
        }
        return hasil
    }

    @Test
    fun setiapBerkasKontenBebasKunciGanda() {
        val konten = berkasKonten()
        assertTrue("berkas konten tidak ditemukan", konten.size >= 10)
        val pelanggar = konten.mapNotNull { f ->
            val ganda = kunciGanda(f.readText()).distinct()
            if (ganda.isEmpty()) null else "${f.name}: ${ganda.joinToString(", ")}"
        }
        assertTrue("kunci JSON ditulis dua kali (nilai pertama hilang tanpa jejak): $pelanggar",
            pelanggar.isEmpty())
    }

    /** Pemindainya sendiri harus benar: ia menemukan duplikat pada contoh buatan, bukan selalu kosong. */
    @Test
    fun pemindaiMenemukanDuplikatPadaContohBuatan() {
        assertTrue("duplikat pada objek yang sama tidak terdeteksi",
            kunciGanda("""{"a": 1, "a": 2}""").contains("a"))
        assertTrue("duplikat di objek bersarang tidak terdeteksi",
            kunciGanda("""{"luar": {"b": 1, "b": 2}}""").contains("b"))
        // Kunci sama di objek **berbeda** bukan duplikat.
        assertTrue("kunci sama di objek berbeda keliru dianggap duplikat",
            kunciGanda("""{"x": {"k": 1}, "y": {"k": 2}}""").isEmpty())
        // Kurawal di dalam teks tidak boleh mengacaukan kedalaman.
        assertTrue("kurawal di dalam string mengacaukan pemindaian",
            kunciGanda("""{"a": "kurung { dan }", "b": 2}""").isEmpty())
    }

    /**
     * Penjaga regresi: keterangan yang dulu tertimpa harus tetap ada. Yang hilang bukan sekadar
     * kalimat, melainkan satu-satunya penjelasan bahwa dua seruan *hayya* dijawab berbeda.
     */
    @Test
    fun catatanMenjawabAdzanTidakLagiTertimpaVersiPendek() {
        val f = berkasKonten().firstOrNull { it.name == "adzan.json" }
        assertTrue("adzan.json tidak ditemukan", f != null)
        val catatan = JSONObject(f!!.readText()).getJSONObject("menjawab_adzan").optString("catatan")
        assertTrue("catatan menjawab adzan kosong", catatan.isNotBlank())
        assertTrue("catatan kehilangan penjelasan pengecualian dua seruan hayya: $catatan",
            catatan.contains("hayya"))
    }

    @Test
    fun seluruhBerkasKontenMasihDapatDiParse() {
        berkasKonten().forEach { f ->
            val o = runCatching { JSONObject(f.readText()) }.getOrNull()
            assertTrue("${f.name} gagal di-parse", o != null)
        }
    }
}
