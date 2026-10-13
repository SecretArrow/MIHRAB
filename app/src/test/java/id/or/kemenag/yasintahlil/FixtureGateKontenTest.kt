package id.or.kemenag.yasintahlil

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Gate konten (`tools/validate_content.py`) harus punya **bukti bahwa ia benar-benar menolak**.
 *
 * Gerbang bundel sudah memilikinya sejak lama — `tools/fixtures/bundel/sah.json` yang wajib lolos
 * dan dua `*cacat.json` yang wajib ditolak, ketiganya dijalankan `ci.yml`. Gerbang konten tidak:
 * satu-satunya cara membuktikannya adalah menyunting aset yang sesungguhnya, sehingga tak ada
 * apa pun di repo yang menjaganya tetap menggigit. Harga dari gate yang tidak menggigit lebih
 * mahal daripada gate yang tidak ada, sebab ia **menjanjikan** pemeriksaan yang tidak pernah
 * berjalan — dan kegagalannya sunyi, persis yang dicegah seluruh keluarga uji di berkas ini.
 *
 * Dua hal karena itu dijaga di sini:
 *
 * 1. **Fixture-nya ada dan hanya menguji satu aturan.** `cacat.json` harus berbeda dari `sah.json`
 *    **hanya** pada `sumber_url` yang hilang; bila kelak ia menyimpang lebih jauh, kegagalannya
 *    tidak lagi dapat diatribusikan pada satu aturan, dan fixture itu berhenti membuktikan apa pun
 *    yang spesifik.
 * 2. **CI benar-benar menjalankannya**, dengan `--harus-gagal` pada yang cacat — tanpa itu fixture
 *    hanya berkas mati di repo.
 *
 * Uji ini membaca berkas, bukan menjalankan Python: gate-nya sendiri dijalankan CI, dan yang tidak
 * dapat dilihat CI adalah "apakah fixture-nya masih sah sebagai fixture".
 */
class FixtureGateKontenTest {

    private fun repoRoot(): File =
        generateSequence(KodeSumber.root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_content.py").isFile }
            ?: error("tools/validate_content.py tidak ditemukan dari ${KodeSumber.root()}")

    private fun fixture(nama: String): File = File(repoRoot(), "tools/fixtures/konten/$nama")

    private fun dokumen(nama: String): JSONObject = JSONObject(fixture(nama).readText())

    /** Entri teks keagamaan yang diperiksa gate: `kelompok[0].items[0]`. */
    private fun entri(d: JSONObject): JSONObject = d.getJSONArray("kelompok")
        .getJSONObject(0).getJSONArray("items").getJSONObject(0)

    private fun kunci(o: JSONObject): Set<String> = o.keys().asSequence().toSet()

    // --- Fixture ada dan hanya menguji satu aturan ----------------------------------------------

    @Test
    fun keduaFixtureAdaDanBerupaObjekJson() {
        assertTrue("fixture sah tidak ada", fixture("sah.json").isFile)
        assertTrue("fixture cacat tidak ada", fixture("cacat.json").isFile)
    }

    /** Tanpa entri, kedua berkas akan lolos gate — dan fixture itu tidak membuktikan apa pun. */
    @Test
    fun fixtureBenarBenarMemuatEntriYangDiperiksaGate() {
        listOf("sah.json", "cacat.json").forEach { nama ->
            val kelompok = dokumen(nama).getJSONArray("kelompok")
            assertEquals("$nama: jumlah kelompok", 1, kelompok.length())
            assertEquals("$nama: jumlah entri",
                1, kelompok.getJSONObject(0).getJSONArray("items").length())
        }
    }

    /**
     * `cacat.json` harus berbeda dari `sah.json` **hanya** pada `sumber_url` yang hilang.
     *
     * Bila kelak salah satunya disunting lebih jauh, kegagalan gate tidak lagi menunjuk satu aturan
     * tertentu — fixture-nya berhenti menjadi bukti, dan justru menjadi sumber kebingungan.
     */
    @Test
    fun cacatHanyaKehilanganSumberUrl() {
        val sah = dokumen("sah.json")
        val cacat = dokumen("cacat.json")

        assertEquals("kunci tingkat berkas berbeda", kunci(sah), kunci(cacat))
        assertEquals("nilai 'modul' berbeda", sah.optString("modul"), cacat.optString("modul"))

        val eSah = entri(sah)
        val eCacat = entri(cacat)
        assertTrue("fixture sah justru tanpa sumber_url", eSah.optString("sumber_url").isNotBlank())
        assertTrue("fixture cacat justru punya sumber_url", eCacat.optString("sumber_url").isBlank())
        assertEquals("selisih kunci selain 'sumber_url'",
            setOf("sumber_url"), kunci(eSah) - kunci(eCacat))
        assertEquals("cacat punya kunci yang tidak ada di sah",
            emptySet<String>(), kunci(eCacat) - kunci(eSah))
        kunci(eCacat).forEach { k ->
            assertEquals("nilai entri '$k' berbeda", eSah.opt(k).toString(), eCacat.opt(k).toString())
        }
    }

    // --- CI benar-benar menjalankannya ----------------------------------------------------------

    @Test
    fun ciMenjalankanKeduaFixture() {
        val ci = File(repoRoot(), ".github/workflows/ci.yml").readText()
        assertTrue("ci.yml tidak menjalankan fixture sah",
            ci.contains("tools/fixtures/konten/sah.json"))
        assertTrue("ci.yml tidak menuntut fixture cacat ditolak",
            ci.contains("--harus-gagal tools/fixtures/konten/cacat.json"))
    }

    /**
     * Gate harus **gagal keras** bila ia memeriksa nol entri, dan harus dapat diarahkan ke berkas
     * yang diberikan — tanpa itu fixture di atas tidak mungkin dijalankan lewat gate yang sama.
     *
     * Diperiksa pada teks skripnya sebab uji JVM tidak menjalankan Python; yang dijaga di sini
     * adalah penjaganya, bukan hasil jalannya (hasil jalannya diperiksa `ci.yml`).
     */
    @Test
    fun gateGagalKerasBilaTidakMemeriksaApaPunDanMenerimaBerkas() {
        val skrip = File(repoRoot(), "tools/validate_content.py").readText()
        assertTrue("gate tidak menerima berkas/direktori sebagai argumen",
            skrip.contains("def berkas_yang_diperiksa("))
        assertTrue("gate tidak mengenal mode --harus-gagal",
            skrip.contains("\"--harus-gagal\""))
        assertTrue("gate dapat melaporkan sukses tanpa memeriksa satu entri pun",
            skrip.contains("tidak satu pun entri konten ditemukan"))
    }
}
