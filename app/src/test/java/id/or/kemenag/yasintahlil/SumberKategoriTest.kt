package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.core.data.SumberKategori
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Menjaga pembedaan sumber bacaan yang diminta `plan.txt`: *"Bedakan bacaan yang bersumber dari
 * Al-Qur'an, hadis, dzikir umum, dan tradisi lokal."*
 *
 * Ada dua sisi yang harus sepakat: gate CI (`tools/validate_content.py`) menolak berkas ber-Arab
 * tanpa `kategori`, dan aplikasi menampilkan labelnya ([SumberKategori]). Kalau salah satu sisi
 * berubah sendiri, pengguna bisa melihat label yang tak pernah diverifikasi gate — jadi uji ini
 * membandingkan daftar kategori di kedua sisi, lalu memastikan seluruh assets patuh.
 */
class SumberKategoriTest {

    private fun root(): File =
        listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    /**
     * Akar repositori, tempat `app/` dan `tools/` bersebelahan. Uji berjalan dengan direktori kerja
     * modul (`app/`), jadi [root] menunjuk ke `app` — naik satu tingkat dulu sebelum mencari gate.
     */
    private fun repoRoot(): File =
        generateSequence(root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_content.py").isFile }
            ?: root().absoluteFile

    private fun contentDir(): File {
        val candidates = listOf(
            File("src/main/assets/content"),
            File("app/src/main/assets/content"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Direktori assets/content tidak ditemukan dari ${File(".").absolutePath}")
    }

    private fun contentFiles(): List<File> =
        contentDir().listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }

    /** Nilai `kategori` di berkas konten, tanpa peduli kedalamannya (regex cukup untuk JSON flat). */
    private fun kategoriDi(text: String): List<String> =
        Regex("\"kategori\"\\s*:\\s*\"([^\"]*)\"").findAll(text)
            .map { it.groupValues[1] }.toList()

    /**
     * Daftar kategori aplikasi harus sama persis dengan `KATEGORI` di gate konten. Kalau gate
     * menambah kategori baru tanpa model, atau sebaliknya, uji ini gagal lebih dulu daripada
     * pengguna menemukan label "tidak dikenal".
     */
    @Test
    fun daftarKategoriSelarasDenganGateKonten() {
        val script = File(repoRoot(), "tools/validate_content.py")
        assertTrue("tools/validate_content.py tidak ditemukan di ${repoRoot().absolutePath}", script.isFile)
        val baris = Regex("^KATEGORI\\s*=\\s*\\(([^)]*)\\)", RegexOption.MULTILINE)
            .find(script.readText())?.groupValues?.get(1)
            ?: error("Konstanta KATEGORI tidak ditemukan di tools/validate_content.py")
        val dariGate = Regex("\"([^\"]+)\"").findAll(baris).map { it.groupValues[1] }.toList()

        assertEquals("Kategori di SumberKategori berbeda dengan gate konten", dariGate, SumberKategori.SEMUA)
    }

    /** Setiap nilai `kategori` di assets harus dikenal aplikasi. */
    @Test
    fun semuaKategoriDiAssetsDikenal() {
        val files = contentFiles()
        assertTrue("Tidak ada file konten ditemukan", files.isNotEmpty())

        val takDikenal = mutableListOf<String>()
        files.forEach { f ->
            kategoriDi(f.readText()).filterNot { SumberKategori.dikenal(it) }
                .forEach { takDikenal += "${f.name}: '$it'" }
        }
        assertTrue(
            "Kategori tak dikenal (pilihan: ${SumberKategori.SEMUA.joinToString()}): $takDikenal",
            takDikenal.isEmpty(),
        )
    }

    /**
     * Setiap berkas yang memuat bacaan ber-Arab wajib menyatakan kategori — di tingkat berkas
     * atau pada tiap entri. Berkas tanpa teks Arab (mis. wudhu.json, koordinat kota) dikecualikan.
     */
    @Test
    fun setiapBerkasBerArabMenyatakanKategori() {
        val arabic = Regex("[\\u0600-\\u06FF]")
        val pelanggar = mutableListOf<String>()
        contentFiles().forEach { f ->
            val text = f.readText()
            if (arabic.containsMatchIn(text) && kategoriDi(text).isEmpty()) pelanggar += f.name
        }
        assertTrue(
            "Berkas ber-Arab tanpa 'kategori' sumber (pilihan: ${SumberKategori.SEMUA.joinToString()}): $pelanggar",
            pelanggar.isEmpty(),
        )
    }

    /** Label tampilan: tiap kategori dikenal punya label manusiawi yang berbeda. */
    @Test
    fun labelKategoriManusiawiDanUnik() {
        val labels = SumberKategori.SEMUA.map { SumberKategori.label(it) }
        labels.forEach { assertTrue("Label kosong", it.isNotBlank()) }
        assertEquals("Label kategori ada yang kembar", labels.size, labels.toSet().size)
        assertEquals("Al-Qur'an", SumberKategori.label(SumberKategori.QURAN))
        assertEquals("Hadis", SumberKategori.label(SumberKategori.HADIS))
        assertEquals("Dzikir umum", SumberKategori.label(SumberKategori.DZIKIR))
        assertEquals("Tradisi lokal", SumberKategori.label(SumberKategori.TRADISI))
    }

    /** Kunci kosong/asing bukan kategori — layar harus menyembunyikan labelnya, bukan menebak. */
    @Test
    fun kunciAsingBukanKategori() {
        assertFalse(SumberKategori.dikenal(""))
        assertFalse(SumberKategori.dikenal("qur'an"))
        assertFalse(SumberKategori.dikenal("hadits"))
        SumberKategori.SEMUA.forEach { assertTrue(it, SumberKategori.dikenal(it)) }
    }
}
