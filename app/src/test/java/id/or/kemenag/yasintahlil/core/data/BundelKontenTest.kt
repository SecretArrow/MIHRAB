package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    // --- Rujukan berkas (`berkas.json#id`) ----------------------------------------------------

    /**
     * Bundel yang merujuk berkas **bawaan** aplikasi (tidak ikut di dalam bundel).
     *
     * Rujukan seperti ini memang dipakai bundel: entri dzikir baru dapat menunjuk lafaz yang sudah
     * terverifikasi di `sholat_mayit.json` alih-alih menyalinnya.
     */
    private fun bundelRujukan(rujukan: String): String = """
        {
          "_bundel": { "skema": 1 },
          "berkas": {
            "dzikir.json": {
              "sumber_url": "https://sunnah.com/muslim:591",
              "kelompok": [ { "id": "g", "judul": "Grup",
                              "items": [ { "judul": "Entri", "rujukan_file": "$rujukan",
                                           "sumber": "HR Muslim" } ] } ]
            }
          }
        }
    """.trimIndent()

    /** Rujukan ke berkas bawaan yang ada — sah, tanpa galat. */
    @Test
    fun rujukanKeBerkasBawaanDiterima() {
        val h = BundelKonten.baca(bundelRujukan("sholat_mayit.json#doa_mayit"))
        assertTrue(h.ditolak.toString(), h.sah)
        val galat = BundelKonten.periksaRujukan(h, setOf("sholat_mayit.json", "dzikir.json"))
        assertTrue("rujukan ke berkas bawaan yang ada seharusnya diterima: $galat", galat.isEmpty())
    }

    /**
     * Rujukan mati ditolak **saat dipasang**, bukan saat layarnya dibuka.
     *
     * Sebelumnya rujukan seperti ini lolos pemeriksaan (sumber resmi, kategori, harakat, bebas
     * placeholder semuanya terpenuhi), lalu `ContentRepo.doc` melempar `FileNotFoundException` di
     * dalam `remember { repo.dzikir() }` pada layar — menghentikan seluruh layar Dzikir tanpa satu
     * pun petunjuk sebabnya, jauh dari tempat yang dapat diperbaiki pengguna.
     */
    @Test
    fun rujukanKeBerkasYangTidakAdaDitolak() {
        val h = BundelKonten.baca(bundelRujukan("berkas_hantu.json#doa"))
        assertTrue(h.ditolak.toString(), h.sah)
        val galat = BundelKonten.periksaRujukan(h, setOf("sholat_mayit.json"))
        assertTrue("rujukan mati seharusnya ditolak: $galat",
            galat.any { "berkas yang tidak ada" in it })
    }

    /** Target di dalam bundel yang sama diperiksa lebih jauh: id-nya harus benar-benar ada. */
    @Test
    fun rujukanKeEntriDiDalamBundelDiperiksa() {
        val teks = """
            {
              "_bundel": { "skema": 1 },
              "berkas": {
                "sholat_mayit.json": {
                  "sumber_url": "https://kemenag.go.id/",
                  "bacaan": [ { "id": "doa_mayit", "judul": "Doa mayit" } ]
                },
                "dzikir.json": {
                  "sumber_url": "https://sunnah.com/muslim:591",
                  "kelompok": [ { "id": "g", "judul": "Grup",
                                  "items": [ { "judul": "Entri",
                                               "rujukan_file": "sholat_mayit.json#doa_mayit" } ] } ]
                }
              }
            }
        """.trimIndent()
        val h = BundelKonten.baca(teks)
        assertTrue(h.ditolak.toString(), h.sah)
        assertTrue("target yang benar-benar ada seharusnya diterima",
            BundelKonten.periksaRujukan(h, emptySet()).isEmpty())

        val mati = BundelKonten.baca(teks.replace("#doa_mayit", "#tidak_ada"))
        assertTrue(mati.ditolak.toString(), mati.sah)
        val galat = BundelKonten.periksaRujukan(mati, emptySet())
        assertTrue("id yang tidak ada di berkas target seharusnya ditolak: $galat",
            galat.any { "entri yang tidak ada" in it })
    }

    /** Larik `rujukan` (beberapa lafaz dari satu entri) ikut diperiksa, bukan hanya `rujukan_file`. */
    @Test
    fun larikRujukanIkutDiperiksa() {
        val teks = bundelRujukan("sholat_mayit.json#doa_mayit")
            .replace("\"rujukan_file\": \"sholat_mayit.json#doa_mayit\",",
                "\"rujukan\": [\"sholat_mayit.json#doa_mayit\", \"berkas_hantu.json\"],")
        val h = BundelKonten.baca(teks)
        assertTrue(h.ditolak.toString(), h.sah)
        val galat = BundelKonten.periksaRujukan(h, setOf("sholat_mayit.json"))
        assertTrue("rujukan mati di dalam larik tidak diperiksa: $galat",
            galat.any { "berkas_hantu.json" in it && "tidak ada" in it })
    }

    /**
     * Bentuk yang salah ditolak di sini, bukan saat dibaca.
     *
     * `ContentRepo` membaca larik `rujukan` dengan `getJSONArray`, jadi `"rujukan": "berkas.json"`
     * bukan sekadar tidak berguna — ia melempar saat layar membacanya.
     */
    @Test
    fun rujukanBerbentukSalahDitolak() {
        val salahBentuk = BundelKonten.baca(
            bundelRujukan("x").replace("\"rujukan_file\": \"x\",", "\"rujukan\": \"sholat_mayit.json\","))
        assertTrue(salahBentuk.ditolak.toString(), salahBentuk.sah)
        val galat1 = BundelKonten.periksaRujukan(salahBentuk, setOf("sholat_mayit.json"))
        assertTrue("'rujukan' berupa teks seharusnya ditolak: $galat1",
            galat1.any { "harus berupa larik teks" in it })

        val kosong = BundelKonten.baca(bundelRujukan("   "))
        assertTrue(kosong.ditolak.toString(), kosong.sah)
        val galat2 = BundelKonten.periksaRujukan(kosong, emptySet())
        assertTrue("rujukan kosong seharusnya ditolak: $galat2", galat2.any { "kosong" in it })
    }

    /**
     * Pemeriksaan rujukan harus benar-benar dijalankan saat memasang bundel.
     *
     * Aturan yang tidak dipanggil sama saja dengan tidak ada: `periksaRujukan` memerlukan daftar
     * berkas bawaan, dan daftar itu hanya ada pada `Context` milik `ContentUpdateManager`. Karena
     * itu pengaitan keduanya dijaga di sini.
     */
    @Test
    fun pemasanganBundelMenjalankanPemeriksaanRujukan() {
        val manager = KodeSumber.kodeTanpaKomentar(File(KodeSumber.root(),
            "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentUpdateManager.kt").readText())
        assertTrue("ContentUpdateManager tidak memeriksa rujukan berkas",
            manager.contains("BundelKonten.periksaRujukan("))
        assertTrue("daftar berkas bawaan tidak diberikan kepada pemeriksaan rujukan",
            manager.contains("berkasAset()"))
        assertTrue("galat rujukan tidak ikut membatalkan pemasangan",
            manager.contains("galatRujukan"))
    }

    /**
     * Rujukan yang tidak dapat ditelusuri mengembalikan teks kosong, bukan melempar.
     *
     * Pembacaannya ada di dalam `remember { repo.… }` pada layar, sehingga satu rujukan mati
     * menaikkan `FileNotFoundException` sampai ke composable dan menghentikan seluruh layar.
     * Jalan mundur ke teks milik entri sendiri sudah ada (`o.optString("arab", a)`), jadi yang
     * perlu dijamin hanyalah pembacaan berkasnya tidak menjatuhkan layar.
     */
    @Test
    fun rujukanKontenTidakMenjatuhkanLayar() {
        val repo = KodeSumber.kodeTanpaKomentar(File(KodeSumber.root(),
            "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt").readText())
        assertTrue("pembacaan rujukan memakai doc() yang melempar bila berkasnya tidak ada",
            repo.contains("docAtauNull(nama)"))
        assertFalse("pembacaan rujukan masih memanggil doc() langsung",
            repo.contains("val d = doc(nama)"))
    }
}
