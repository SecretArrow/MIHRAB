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
}
