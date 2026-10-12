package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Daftar host resmi ([ContentValidator.allowedHosts]) harus tetap **satu** — dipakai aplikasi saat
 * runtime *dan* gate konten di CI.
 *
 * Aplikasi menolak berkas konten yang `sumber_url`-nya di luar daftar ini
 * ([ContentValidator.isOfficialSource] dipanggil `ContentRepo` dan `BundelKonten`), sementara
 * `tools/validate_content.py` menilai berkas yang sama saat dibangun. Dahulu keduanya menyimpan
 * salinannya sendiri-sendiri — satu di Kotlin, satu di Python — dan tak ada apa pun yang
 * membandingkannya. Dua salinan berarti dua kebijakan: berkas dengan domain yang hanya ada di daftar
 * Python **lolos gate**, tampak "terpasang" bagi pengguna, lalu setiap berkasnya ditolak pembaca dan
 * aplikasi diam-diam kembali ke konten bawaan — persis kegagalan sunyi yang sudah pernah terjadi pada
 * ejaan kunci `sumber_url`. Sebaliknya, host yang ditambahkan di Kotlin tanpa ikut di gate membuat CI
 * gagal pada berkas yang justru sah.
 *
 * Gate kini **membaca** daftarnya dari [ContentValidator]; uji ini menjaga agar ia tidak pernah
 * kembali menyimpan salinan, dan agar pola pengambilnya benar-benar menghasilkan daftar yang sama.
 *
 * Dua berkas Kotlin memang boleh mengeja host itu, dan keduanya sudah dijaga uji lain: yang
 * menyatakan kebijakannya ([ContentValidator]), dan yang memberi keterangan pemiliknya
 * (`SumberKonten.nama` di `core/legal/DokumenLegal.kt`, kelengkapannya dipaku `TentangTest`).
 * [daftarHostResmiHanyaDinyatakanDiBerkasYangDijaga] memaku **kedua nama itu saja**, sehingga
 * berkas ketiga yang mulai mengeja host resmi tidak dapat menyelinap tanpa ketahuan.
 *
 * Dua dokumen manusia mengulang daftar yang sama — `SOURCES.md` dan `docs/CONTENT_SOURCES.md`.
 * Keduanya tak terjangkau penjagaan berkas Kotlin di atas, jadi kesepadatannya dengan
 * [ContentValidator.allowedHosts] dijaga tersendiri oleh [dokumenMengulangDaftarHostYangSama].
 */
class SumberResmiSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    /**
     * Akar repositori, tempat `app/` dan `tools/` bersebelahan.
     *
     * Uji berjalan dengan direktori kerja modul (`app/`), jadi [root] menunjuk ke `app` — naik satu
     * tingkat dulu sebelum mencari gate. Tanpa ini, `tools/validate_content.py` dicari di dalam
     * `app/` dan penjaganya gagal karena berkas yang sebenarnya ada.
     */
    private fun repoRoot(): File =
        generateSequence(root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_content.py").isFile }
            ?: error("tools/validate_content.py tidak ditemukan dari ${root().absolutePath}")

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private val gerbang: String
        get() = File(repoRoot(), "tools/validate_content.py").readText()

    /**
     * Pola pengambil daftar host dari `ContentValidator.kt`. Ditulis **persis sama** di gate Python
     * dan di sini, dan uji [gerbangKontenMembacaDaftarYangSama] memaku keduanya sama — sehingga
     * pola yang berbeda antar-keduanya tidak dapat lolos tanpa ketahuan.
     */
    private val pola = """allowedHosts\s*=\s*listOf\(([^)]*)\)""".toRegex()

    private val berkasValidator = "src/main/java/id/or/kemenag/yasintahlil/core/util/ContentValidator.kt"

    @Test
    fun daftarHostResmiHanyaDinyatakanDiBerkasYangDijaga() {
        val pelanggar = mutableListOf<String>()
        berkasMain().filter { it.name !in setOf("ContentValidator.kt", "DokumenLegal.kt") }
            .forEach { f ->
                val kode = KodeSumber.kodeTanpaKomentar(f.readText())
                ContentValidator.allowedHosts.filter { kode.contains("\"$it\"") }
                    .forEach { pelanggar += "${f.name}: \"$it\"" }
            }
        assertTrue("daftar host resmi dieja ulang di berkas yang tidak dijaga: $pelanggar",
            pelanggar.isEmpty())
    }

    @Test
    fun gerbangKontenMembacaDaftarYangSama() {
        assertTrue("gate konten tidak membaca daftar host dari ContentValidator.kt",
            gerbang.contains("ContentValidator.kt"))
        assertTrue("gate konten memakai pola pengambil yang berbeda dari penjaganya",
            gerbang.contains(pola.pattern))
        // Salinan daftar di gate akan membuat dua kebijakan yang dapat berbeda tanpa galat.
        val salinan = ContentValidator.allowedHosts.filter { gerbang.contains("\"$it\"") }
        assertTrue("gate konten menyimpan salinan daftar host: $salinan", salinan.isEmpty())
    }

    /** Pola pengambil benar-benar menghasilkan daftar aplikasi — bukan sekadar cocok. */
    @Test
    fun polaPengambilMenghasilkanDaftarAplikasi() {
        val m = pola.find(KodeSumber.sumber(berkasValidator))
        assertTrue("pola pengambil tidak cocok dengan ContentValidator.kt", m != null)
        val hosts = Regex("\"([^\"]+)\"")
            .findAll(m!!.groupValues[1]).map { it.groupValues[1] }.toList()
        assertEquals("daftar host yang dibaca gate berbeda dari yang dipakai aplikasi",
            ContentValidator.allowedHosts, hosts)
        // Daftar kosong akan membuat gate meloloskan segalanya; host bersusun huruf besar akan
        // gagal mencocokkan host yang sudah di-lowercase `isOfficialSource`.
        assertTrue("daftar host resmi kosong", hosts.isNotEmpty())
        assertEquals("host resmi harus huruf kecil", hosts, hosts.map { it.lowercase() })
    }

    /**
     * Dua **dokumen manusia** mengulang daftar host yang sama: `SOURCES.md` §6 (aturan konten) dan
     * `docs/CONTENT_SOURCES.md` (sumber yang diizinkan gate). Keduanya salinan kedua dari
     * [ContentValidator.allowedHosts] — dan `daftarHostResmiHanyaDinyatakanDiBerkasYangDijaga` tak
     * dapat melihatnya: ia hanya menelusuri berkas `.kt`.
     *
     * Akibatnya sunyi, dan justru mengenai **penulis konten**: host yang ditambahkan di kode
     * membuat kedua dokumen berbohong, sehingga orang yang mengikuti dokumen menambahkan entri
     * dengan domain yang justru **ditolak** aplikasi ([ContentValidator.isOfficialSource]) — berkas
     * itu tampak terpasang, lalu pembaca diam-diam kembali ke konten bawaan. Sebaliknya, host yang
     * dihapus di kode meninggalkan dokumen yang menjanjikan domain yang sudah tak diterima.
     *
     * Baris aturannya dikenali dari pasangan `kemenag.dev` + `nu.or.id`: `nu.or.id` hanya muncul di
     * baris itu pada kedua dokumen, sedangkan `kemenag.dev` juga muncul di tautan SIHAT — jadi
     * keduanya bersama-sama menandai baris daftar, bukan tautan.
     */
    @Test
    fun dokumenMengulangDaftarHostYangSama() {
        listOf("SOURCES.md", "docs/CONTENT_SOURCES.md").forEach { nama ->
            val isi = File(repoRoot(), nama).readText()
            val baris = isi.lines().filter { it.contains("kemenag.dev") && it.contains("nu.or.id") }
            assertEquals("$nama tidak lagi memuat tepat satu baris daftar host resmi", 1, baris.size)

            val hosts = Regex("""[a-z0-9-]+(?:\.[a-z0-9-]+)+""").findAll(baris.single())
                .map { it.value }.toSet()
            assertEquals(
                "daftar host di $nama berbeda dari yang dipakai aplikasi — dokumen akan " +
                    "menjanjikan domain yang ditolak, atau menyembunyikan domain yang diterima",
                ContentValidator.allowedHosts.toSet(),
                hosts,
            )
        }
    }
}
