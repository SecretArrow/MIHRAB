package id.or.kemenag.yasintahlil

import id.or.kemenag.yasintahlil.core.util.LafazSama
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Penjaga **"satu lafaz, satu bacaan"** — aturannya tinggal di [LafazSama], di sini hanya
 * pemindaian berkasnya.
 *
 * Bundel konten sengaja tidak menyatukan lafaz yang dipakai berulang: tiap modul berdiri sendiri
 * supaya pengguna dapat memasang pembaruan salah satunya saja. Harga dari kemandirian itu adalah
 * salinan, dan salinan yang tak dibandingkan pasti menyimpang. Sudah terjadi pada doa untuk mayit:
 * `sholat_mayit.json` `bacaan[0]` dan `tahlil_full.json` `bagian[13]` memuat lafaz Arab yang sama
 * (id keduanya `doa_mayit`, sumber keduanya HR Muslim — sunnah.com), tetapi terjemahan Indonesianya
 * berbeda satu klausa — *"…, rahmatilah dia, …"* lawan *"…, berilah dia rahmat, …"*. Pengguna
 * membaca doa yang sama di dua layar dan mendapat dua kalimat yang tidak persis sama.
 *
 * Uji ini memindai **seluruh** berkas `assets/content` dan menuntut agar setiap lafaz yang muncul
 * di lebih dari satu tempat dibaca dan diterjemahkan dengan kata yang sama. Salinan baru yang
 * menyimpang karena itu tertangkap tanpa perlu didaftar di sini — persis seperti penjaga layar pada
 * `UkuranBacaanModeBacaTest` yang menemukan layarnya dari sumber.
 */
class LafazSamaSatuTerjemahanTest {

    /** Satu entri konten ber-`arab`, beserta id dan tempatnya. */
    private data class Entri(
        val tempat: String,
        val id: String,
        val arab: String,
        val latin: String,
        val terjemahan: String,
    )

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun berkasKonten(): List<File> =
        File(root(), "src/main/assets/content").listFiles { f -> f.extension == "json" }
            ?.sortedBy { it.name } ?: emptyList()

    /** Setiap objek JSON yang memuat lafaz Arab, di kedalaman mana pun. */
    private fun entri(): List<Entri> {
        val hasil = mutableListOf<Entri>()
        berkasKonten().forEach { f ->
            fun turun(node: Any?, jalur: String) {
                when (node) {
                    is JSONObject -> {
                        val arab = node.optString("arab")
                        if (arab.isNotBlank()) hasil += Entri(
                            tempat = "${f.name}$jalur",
                            id = node.optString("id"),
                            arab = arab,
                            latin = node.optString("latin"),
                            terjemahan = node.optString("terjemahan"),
                        )
                        node.keys().forEach { k -> turun(node.opt(k), "$jalur.$k") }
                    }
                    is JSONArray -> for (i in 0 until node.length()) turun(node.opt(i), "$jalur[$i]")
                    else -> Unit
                }
            }
            turun(JSONObject(f.readText()), "")
        }
        return hasil
    }

    private fun pemakaian(): List<LafazSama.Pemakaian> =
        entri().map { LafazSama.Pemakaian(it.tempat, it.arab, it.latin, it.terjemahan) }

    /**
     * Invariannya. Kegagalan menyebut lafaz, kedua tempat, dan kedua ejaannya — supaya perbaikan
     * tidak perlu mencari sendiri salinan mana yang menyimpang.
     */
    @Test
    fun setiapLafazYangSamaDibacaDanDiterjemahkanSama() {
        val semua = pemakaian()
        assertTrue("berkas konten tidak terbaca (hanya ${semua.size} lafaz)", semua.size >= 150)

        val berganda = LafazSama.kelompok(semua).filterValues { it.size > 1 }
        assertTrue(
            "penanda tidak menemukan satu pun lafaz bersalin — pemindaiannya sendiri yang rusak",
            berganda.size >= 6,
        )

        val menyimpang = LafazSama.penyimpangan(semua)
        assertTrue(
            "lafaz yang sama dibaca/diterjemahkan berbeda di dua tempat:\n" +
                menyimpang.joinToString("\n"),
            menyimpang.isEmpty(),
        )
    }

    /** Penandanya sendiri harus benar: ia menangkap salinan buatan yang menyimpang. */
    @Test
    fun penandaMenemukanSalinanYangMenyimpang() {
        // Lafaznya sengaja bukan teks Arab sungguhan: yang diuji di sini pemisahan salinan, bukan
        // isinya. Teks Arab hanya boleh berasal dari assets (lihat `ArabicTextTest`).
        val lafaz = "lafaz-contoh"
        val sama = listOf(
            LafazSama.Pemakaian("a.json[0]", lafaz, "Salam.", "Damai."),
            LafazSama.Pemakaian("b.json[1]", lafaz, "Salam.", "Damai."),
        )
        assertTrue("salinan yang sama keliru dianggap menyimpang", LafazSama.penyimpangan(sama).isEmpty())

        val bedaTerjemahan = sama + LafazSama.Pemakaian("c.json[2]", lafaz, "Salam.", "Sejahtera.")
        val t = LafazSama.penyimpangan(bedaTerjemahan)
        assertEquals("terjemahan yang berbeda tidak terdeteksi", 1, t.size)
        assertTrue("pesan tidak menyebut kedua tempat: $t", t[0].contains("a.json[0]"))
        assertTrue("pesan tidak menyebut tempat yang menyimpang: $t", t[0].contains("c.json[2]"))

        val bedaLatin = sama + LafazSama.Pemakaian("c.json[2]", lafaz, "Salaam.", "Damai.")
        assertTrue("bacaan Latin yang berbeda tidak terdeteksi", LafazSama.penyimpangan(bedaLatin).size == 1)

        // Spasi tepi bukan penyimpangan, dan lafaz kosong bukan lafaz.
        val spasi = listOf(
            LafazSama.Pemakaian("a.json[0]", " $lafaz ", "Salam.", "Damai."),
            LafazSama.Pemakaian("b.json[1]", lafaz, " Salam. ", "Damai."),
            LafazSama.Pemakaian("c.json[2]", "   ", "", ""),
        )
        assertTrue("spasi tepi keliru dianggap menyimpang", LafazSama.penyimpangan(spasi).isEmpty())
    }

    /**
     * Penjaga regresi untuk penyimpangan yang benar-benar ada: doa mayit di dua modul. Yang diuji
     * bukan sekadar "penyimpangannya kosong" — uji pertama sudah menuntut itu — melainkan bahwa
     * **kedua** salinannya masih ada dan masih sejalan, sehingga salah satunya tidak dapat
     * "diperbaiki" dengan cara menghapusnya.
     */
    @Test
    fun doaMayitTetapAdaDiDuaModulDenganTerjemahanYangSama() {
        val doaMayit = entri().filter { it.id == "doa_mayit" }
        val berkas = doaMayit.map { it.tempat.substringBefore(".json") + ".json" }.distinct().sorted()
        assertEquals(
            "doa mayit harus tetap ada di kedua modul (sholat mayit & tahlil), bukan dihapus",
            listOf("sholat_mayit.json", "tahlil_full.json"), berkas,
        )

        val terjemahan = doaMayit.map { it.terjemahan.trim() }.distinct()
        assertEquals("doa mayit kembali punya dua terjemahan berbeda: $terjemahan", 1, terjemahan.size)
        assertEquals(
            "terjemahan doa mayit berubah dari ejaan yang disepakati",
            "Ya Allah, ampunilah dia, rahmatilah dia, sejahterakanlah dia dan maafkanlah dia.",
            terjemahan[0],
        )
    }
}
