package id.or.kemenag.yasintahlil

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Dua dokumen manusia menyatakan **banyaknya** isi tiap berkas konten, dan keduanya dibaca orang
 * untuk menilai apakah sebuah berkas masih lengkap:
 *
 * - `docs/CONTENT_SOURCES.md` di tabel provenansnya — *"Yasin 83 ayat"*, *"15 bagian susunan
 *   tahlil"*, *"5 kelompok"* dzikir;
 * - `SOURCES.md` §1 — *"Edisi terjemah: Kemenag 2019. Yasin = 83 ayat"*.
 *
 * Tak ada apa pun yang membandingkannya dengan berkas yang benar-benar dikirim. Gate konten
 * (`tools/validate_content.py`) menjaga konsistensi **di dalam** bundel (`ayat_count` harus sama
 * dengan jumlah `verses`), tetapi ia tidak pernah membaca dokumennya. Jadi angka di dokumen dapat
 * menua sendiri: berkas yang kehilangan beberapa ayat tetap ditulis *"83 ayat"*, dan pembaca
 * menyimpulkan ada isi yang sebenarnya tidak pernah ikut terkirim. Uji ini membandingkan angka-angka
 * itu dengan bundelnya — lewat `ayat_count`, bukan lewat jumlah `verses`, sebab gate sudah menjamin
 * keduanya sama.
 *
 * Angka yang dinyatakan **dua kali** justru yang paling mudah menua: yang satu ikut diperbarui, yang
 * satu ketinggalan, dan tak ada yang memberi tahu. Karena itu tiap dokumen diuji sendiri, bukan
 * hanya salah satunya.
 */
class HitungKontenDiDokumenTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    /**
     * Akar repositori, tempat `app/` dan `docs/` bersebelahan. Uji berjalan dengan direktori kerja
     * modul (`app/`), jadi [root] menunjuk ke `app`.
     */
    private fun repoRoot(): File =
        generateSequence(root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "docs/CONTENT_SOURCES.md").isFile }
            ?: error("docs/CONTENT_SOURCES.md tidak ditemukan dari ${root().absolutePath}")

    private fun dokumen(): String = File(repoRoot(), "docs/CONTENT_SOURCES.md").readText()

    /** Dokumen sumber resmi (`SOURCES.md`), tempat §1 mengulang jumlah ayat Yasin. */
    private fun dokumenSumber(): String = File(repoRoot(), "SOURCES.md").readText()

    private fun konten(nama: String): JSONObject =
        JSONObject(File(root(), "src/main/assets/content/$nama").readText())

    /**
     * Kolom keterangan baris provenans satu berkas konten — satu-satunya tempat angka dan nama
     * kelompoknya dinyatakan. Sel pertama harus **persis** nama berkas itu, sehingga tabel lain di
     * dokumen yang sama (kategori bacaan, kekurangan yang menunggu sumber) tak ikut terbaca.
     */
    private fun keterangan(nama: String): String {
        val baris = dokumen().lines().singleOrNull {
            it.startsWith("|") &&
                it.trim().trimStart('|').substringBefore('|').trim() == "`$nama`"
        } ?: error("baris provenans untuk '$nama' tidak ditemukan di docs/CONTENT_SOURCES.md")
        val sel = baris.split("|")
        assertTrue("baris provenans '$nama' tidak lagi punya kolom keterangan", sel.size >= 4)
        return sel[2].trim()
    }

    private fun angkaDi(keterangan: String, satuan: String): Int =
        Regex("(\\d+)\\s*$satuan").find(keterangan)?.groupValues?.get(1)?.toInt()
            ?: error("angka '$satuan' tidak terbaca dari keterangan: $keterangan")

    private fun panjang(d: JSONObject, kunci: String): Int = d.getJSONArray(kunci).length()

    @Test
    fun jumlahAyatYasinDiDokumenSelarasDenganBundel() {
        assertEquals("jumlah ayat Yasin di docs/CONTENT_SOURCES.md berbeda dari bundel",
            konten("yasin_full.json").getInt("ayat_count"), angkaDi(keterangan("yasin_full.json"), "ayat"))
    }

    /**
     * Jumlah ayat Yasin dinyatakan **dua** dokumen, dan yang di `SOURCES.md` dahulu tak dijaga:
     * tabel provenans ikut diperbarui saat bundelnya berubah, sedangkan §1 `SOURCES.md` tetap
     * menulis angka lama — dan `SOURCES.md` justru dokumen yang dibaca penulis konten sebagai
     * rujukan resmi. Angka yang keliru di sana membuat orang mengira ada ayat yang tak pernah
     * terkirim, atau menganggap berkas yang lebih lengkap sebagai yang benar.
     *
     * Pengambilnya `error(...)`, bukan sekadar `null` yang diabaikan: menghapus klaim itu dari
     * `SOURCES.md` harus membuat penjaganya gagal keras, bukan meloloskannya dalam diam.
     */
    @Test
    fun jumlahAyatYasinDiSumberMdSelarasDenganBundel() {
        val ayat = Regex("""Yasin\s*=\s*(\d+)\s*ayat""").find(dokumenSumber())
            ?.groupValues?.get(1)?.toInt()
            ?: error("SOURCES.md tidak lagi menyatakan jumlah ayat Yasin (\"Yasin = N ayat\")")
        assertEquals("jumlah ayat Yasin di SOURCES.md berbeda dari bundel — dokumen sumber resmi " +
                "menjanjikan jumlah ayat yang tak pernah ikut terkirim",
            konten("yasin_full.json").getInt("ayat_count"), ayat)
    }

    @Test
    fun jumlahBagianTahlilDiDokumenSelarasDenganBundel() {
        assertEquals("jumlah bagian tahlil di docs/CONTENT_SOURCES.md berbeda dari bundel",
            panjang(konten("tahlil_full.json"), "bagian"), angkaDi(keterangan("tahlil_full.json"), "bagian"))
    }

    @Test
    fun jumlahKelompokDzikirDiDokumenSelarasDenganBundel() {
        assertEquals("jumlah kelompok dzikir di docs/CONTENT_SOURCES.md berbeda dari bundel",
            panjang(konten("dzikir.json"), "kelompok"), angkaDi(keterangan("dzikir.json"), "kelompok"))
    }

    /**
     * Nama kelompok yang disebut dokumen harus benar-benar menunjuk kelompok yang ada.
     *
     * Dokumen menyingkat namanya (`*setelah sholat*` untuk *"Dzikir setelah sholat"*), jadi yang
     * dituntut bukan kesamaan — melainkan bahwa tiap sebutan masih terkandung dalam salah satu nama
     * kelompok. Cukup untuk menangkap kelompok yang berganti nama atau hilang dari bundel, tanpa
     * memaksa dokumen melepas gaya ringkasnya. Sel keterangan saja yang dibaca: sel sumber di
     * kolom berikutnya juga memakai penekanan (`**sunnah.com**`) dan bukan nama kelompok.
     */
    @Test
    fun namaKelompokDiDokumenMasihMenunjukKelompokYangAda() {
        val d = konten("dzikir.json")
        val namaDiBundel = (0 until panjang(d, "kelompok"))
            .map { d.getJSONArray("kelompok").getJSONObject(it).getString("judul").lowercase() }

        val disebut = Regex("""\*([^*]+)\*""").findAll(keterangan("dzikir.json"))
            .map { it.groupValues[1].trim().lowercase() }.toList()
        assertTrue("dokumen tidak lagi menyebut nama kelompok dzikir", disebut.isNotEmpty())

        val hilang = disebut.filterNot { s -> namaDiBundel.any { it.contains(s) } }
        assertTrue(
            "nama kelompok di docs/CONTENT_SOURCES.md tidak ada di bundel — dokumen menjanjikan " +
                "kelompok yang tak pernah ikut terkirim: $hilang",
            hilang.isEmpty(),
        )
    }
}
