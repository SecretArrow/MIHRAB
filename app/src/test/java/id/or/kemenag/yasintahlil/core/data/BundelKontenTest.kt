package id.or.kemenag.yasintahlil.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menjaga mekanisme pembaruan konten (`plan.txt`: *"Sediakan mekanisme pembaruan konten dengan
 * validasi sumber."*).
 *
 * Yang diuji bukan tata cara menulis berkas, melainkan **aturan penerimaan**: bundel hanya boleh
 * dipasang bila seluruh berkasnya bersumber resmi, berkategori jelas, berharakat, dan bebas
 * placeholder. Sebuah bundel yang gagal harus ditolak utuh — kalau tidak, konten yang sudah
 * diverifikasi bisa tergantikan sebagian oleh teks yang belum diperiksa.
 *
 * Teks Arab di berkas uji ini disalin dari assets yang sudah lolos gate konten (bukan dikarang):
 * `أَسْتَغْفِرُ اللهَ` dari `dzikir.json`.
 */
class BundelKontenTest {

    private val arabBerharakat = "أَسْتَغْفِرُ اللهَ"

    /** Bundel valid: tanpa teks Arab, jadi `kategori` tidak diwajibkan. */
    private fun bundelKota(sumber: String = "https://sihat.kemenag.dev/") = """
        {
          "_bundel": { "skema": 1, "dibuat": "2026-02-19", "penerbit": "Uji" },
          "berkas": {
            "kota.json": {
              "sumber_url": "$sumber",
              "kota": [ { "nama": "Jakarta", "lat": -6.2, "lon": 106.8 } ]
            }
          }
        }
    """.trimIndent()

    private fun bundelArab(arab: String, kategori: String?): String {
        val barisKategori = if (kategori == null) "" else "\"kategori\": \"$kategori\","
        return """
            {
              "_bundel": { "skema": 1 },
              "berkas": {
                "dzikir.json": {
                  "sumber_url": "https://sunnah.com/muslim:591",
                  $barisKategori
                  "kelompok": [ { "id": "g", "judul": "Istighfar",
                                  "items": [ { "judul": "Istighfar", "arab": "$arab" } ] } ]
                }
              }
            }
        """.trimIndent()
    }

    @Test
    fun bundelSahDiterimaUtuh() {
        val h = BundelKonten.baca(bundelKota())
        assertTrue("Bundel seharusnya diterima: ${h.ditolak}", h.sah)
        assertEquals(listOf("kota.json"), h.berkas.keys.toList())
        assertEquals(1, h.skema)
        val ringkas = BundelKonten.ringkasan(h)
        assertEquals("https://sihat.kemenag.dev/", ringkas.single().sumberUrl)
    }

    @Test
    fun sumberTidakResmiDitolak() {
        val h = BundelKonten.baca(bundelKota("https://blog-contoh.example/kota"))
        assertFalse("Bundel bersumber tidak resmi harus ditolak", h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "domain resmi" in it })
        assertTrue("Tidak ada berkas yang boleh lolos", h.berkas.isEmpty())
    }

    /** Anti-spoofing: domain resmi sebagai bagian dari host asing tetap ditolak. */
    @Test
    fun sumberMenyerupaiResmiDitolak() {
        val h = BundelKonten.baca(bundelKota("https://kemenag.go.id.penyerang.example/x"))
        assertFalse(h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "domain resmi" in it })
    }

    @Test
    fun berkasBerArabTanpaKategoriDitolak() {
        val h = BundelKonten.baca(bundelArab(arabBerharakat, null))
        assertFalse(h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "tanpa 'kategori'" in it })
    }

    @Test
    fun kategoriTakDikenalDitolak() {
        val h = BundelKonten.baca(bundelArab(arabBerharakat, "hadits"))
        assertFalse(h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "kategori tak dikenal" in it })
    }

    @Test
    fun berkasBerArabBerkategoriDiterima() {
        val h = BundelKonten.baca(bundelArab(arabBerharakat, SumberKategori.HADIS))
        assertTrue(h.ditolak.toString(), h.sah)
        assertEquals(listOf(SumberKategori.HADIS), BundelKonten.ringkasan(h).single().kategori)
    }

    @Test
    fun teksArabTanpaHarakatDitolak() {
        val h = BundelKonten.baca(bundelArab("الصلاة خير من النوم", SumberKategori.HADIS))
        assertFalse("Teks Arab mentah tanpa harakat harus ditolak", h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "tanpa harakat" in it })
    }

    @Test
    fun placeholderDitolak() {
        val h = BundelKonten.baca(bundelArab("— lihat rujukan file —", SumberKategori.DZIKIR))
        assertFalse(h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "prosa penunjuk" in it })
    }

    @Test
    fun skemaTakDidukungDitolak() {
        val h = BundelKonten.baca(bundelKota().replace("\"skema\": 1", "\"skema\": 99"))
        assertFalse(h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "Skema bundel 99" in it })
    }

    /** Nama berkas tidak boleh menunjuk ke luar folder konten (path traversal). */
    @Test
    fun namaBerkasBerbahayaDitolak() {
        listOf("../evil.json", "sub/evil.json", "/tmp/evil.json", ".evil.json").forEach { nama ->
            assertFalse("'$nama' seharusnya tidak sah", BundelKonten.namaBerkasSah(nama))
        }
        assertTrue(BundelKonten.namaBerkasSah("yasin_full.json"))
        assertTrue("Metadata bukan berkas konten", !BundelKonten.namaBerkasSah("_meta.json"))
    }

    @Test
    fun namaBerkasBerbahayaTidakLolosBaca() {
        val h = BundelKonten.baca(bundelKota().replace("\"kota.json\"", "\"../kota.json\""))
        assertFalse(h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "tidak sah" in it })
    }

    /** Satu berkas gagal = seluruh bundel batal; konten lama tak boleh tercampur setengah. */
    @Test
    fun satuBerkasGagalMembatalkanSeluruhBundel() {
        val teks = """
            {
              "_bundel": { "skema": 1 },
              "berkas": {
                "kota.json": { "sumber_url": "https://sihat.kemenag.dev/", "kota": [] },
                "dzikir.json": { "sumber_url": "https://blog-contoh.example/doa",
                                 "kategori": "dzikir", "arab": "$arabBerharakat" }
              }
            }
        """.trimIndent()
        val h = BundelKonten.baca(teks)
        assertFalse("Bundel dengan satu berkas cacat harus ditolak utuh", h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { it.startsWith("dzikir.json") })
        assertTrue("kota.json yang sehat pun tidak boleh dipasang", h.berkas.isEmpty())
    }

    @Test
    fun jsonRusakDitolakTanpaMelempar() {
        val h = BundelKonten.baca("{ ini bukan json")
        assertFalse(h.sah)
        assertTrue(h.ditolak.toString(), h.ditolak.any { "bukan JSON" in it })
    }

    @Test
    fun bundelTanpaBerkasDitolak() {
        assertFalse(BundelKonten.baca("""{ "_bundel": { "skema": 1 }, "berkas": {} }""").sah)
        assertFalse(BundelKonten.baca("""{ "_bundel": { "skema": 1 } }""").sah)
    }

    /** `kategori` di tingkat berkas berlaku untuk seluruh entri di dalamnya. */
    @Test
    fun kategoriTingkatBerkasBerlaku() {
        val teks = """
            {
              "_bundel": { "skema": 1 },
              "berkas": {
                "tahlil_full.json": {
                  "sumber_url": "https://nu.or.id/",
                  "kategori": "tradisi",
                  "bagian": [ { "id": "a", "judul": "Pembuka", "arab": "$arabBerharakat" } ]
                }
              }
            }
        """.trimIndent()
        val h = BundelKonten.baca(teks)
        assertTrue(h.ditolak.toString(), h.sah)
        assertEquals(listOf(SumberKategori.TRADISI), BundelKonten.ringkasan(h).single().kategori)
    }
}
