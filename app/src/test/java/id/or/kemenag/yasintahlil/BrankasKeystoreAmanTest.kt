package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Brankas keystore rilis boleh tinggal di repo, tetapi **hanya dalam bentuk terenkripsi**.
 *
 * Repositori ini **publik**, dan keystore rilis adalah identitas aplikasi: siapa pun yang
 * memegangnya dapat menerbitkan pembaruan yang dipasang Android sebagai lanjutan resmi aplikasi
 * ini. Keputusan pemilik proyek: arsipnya ikut dilacak supaya proyek dapat dipulihkan utuh,
 * sedangkan sandinya **tidak** — sandi hanya hidup di GitHub Secrets dan di penyimpanan pemilik.
 *
 * Uji ini menjaga sisi yang bisa diperiksa mesin dari keputusan itu:
 *
 * 1. Bentuk **telanjang** (`*.jks`, `*.keystore`, `keystore.properties`) tetap tidak boleh masuk —
 *    `.gitignore` dulu hanya menutup ketiganya, tetapi arsip kemasannya belum pernah disebut,
 *    sehingga status arsipnya bergantung pada kebiasaan, bukan aturan.
 * 2. Arsip yang dilacak itu harus **benar-benar** terenkripsi. Nama berkas yang berbunyi "zip
 *    terenkripsi" tidak membuktikan apa pun: `zip -P` bisa lupa dipakai, dan ZipCrypto yang lupa
 *    sandi menghasilkan arsip yang terbuka bebas namun tampak sama saja di daftar berkas.
 *
 * Yang **tidak** dapat diperiksa mesin — dan karena itu wajib ditinjau manusia — adalah kekuatan
 * sandinya sendiri. Sandi pendek yang mudah ditebak membuat kedua jaminan di atas tak berarti.
 */
class BrankasKeystoreAmanTest {

    private val namaBrankas = listOf("key-sign-apk.zip", "key-sign-apk.zip.enc")

    /** Akar repo: folder yang memuat `.gitignore` sekaligus `app/src/main`. */
    private fun akarRepo(): File {
        val kandidat = listOf(File("."), File("app"), File(".."), File("../.."))
        return kandidat.firstOrNull {
            File(it, ".gitignore").isFile && File(it, "app/src/main").isDirectory
        } ?: error("akar repo tidak ditemukan dari ${File(".").absolutePath}")
    }

    private fun barisGitignore(): List<String> =
        File(akarRepo(), ".gitignore").readLines().map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Bit 0 dari *general purpose bit flag* di *local file header* menandai entri terenkripsi.
     * Itu satu-satunya penanda enkripsi yang dapat dipercaya tanpa mengetahui sandinya.
     */
    private fun zipTerenkripsi(berkas: File): Boolean {
        val h = berkas.readBytes()
        if (h.size < 8) return false
        if (h[0] != 0x50.toByte() || h[1] != 0x4B.toByte() ||
            h[2] != 0x03.toByte() || h[3] != 0x04.toByte()
        ) {
            return false
        }
        val flag = (h[6].toInt() and 0xFF) or ((h[7].toInt() and 0xFF) shl 8)
        return flag and 0x0001 != 0
    }

    @Test
    fun bentukTelanjangKeystoreTetapDiabaikan() {
        val baris = barisGitignore()
        for (pola in listOf("*.jks", "*.keystore", "keystore.properties")) {
            assertTrue(
                "`.gitignore` tidak lagi mengabaikan `$pola` — repositori ini publik, jadi keystore " +
                    "telanjang yang lolos ke sini sama dengan menyerahkan identitas rilis aplikasi",
                baris.contains(pola),
            )
        }
    }

    @Test
    fun brankasTerenkripsiDilacakDiRepo() {
        val akar = akarRepo()
        for (nama in namaBrankas) {
            assertTrue(
                "brankas `$nama` tidak ada di akar repo — keputusan proyek adalah melacaknya agar " +
                    "proyek dapat dipulihkan utuh",
                File(akar, nama).isFile,
            )
        }
    }

    /** Nama berkas tidak membuktikan apa pun; hanya bit enkripsinya yang membuktikan. */
    @Test
    fun brankasZipBenarBenarTerenkripsi() {
        val zip = File(akarRepo(), "key-sign-apk.zip")
        assertTrue(
            "`key-sign-apk.zip` ada tetapi TIDAK terenkripsi — keystore telanjang di dalam arsip " +
                "yang dilacak repo publik. Periksa apakah `zip -P` masih dipakai saat mengemas",
            zipTerenkripsi(zip),
        )
        val enc = File(akarRepo(), "key-sign-apk.zip.enc")
        assertFalse(
            "`key-sign-apk.zip.enc` ternyata masih berupa arsip biasa (mulai dengan 'PK') — " +
                "lapisan AES-256-nya hilang",
            enc.readBytes().take(2) == listOf(0x50.toByte(), 0x4B.toByte()),
        )
    }

    /**
     * Keystore telanjang di mana pun — termasuk yang terselip di dalam `assets/`, yang ikut
     * dibundel ke APK sehingga bocor ke setiap pemasangan.
     */
    @Test
    fun tidakAdaKeystoreTelanjangDiSeluruhRepo() {
        val akar = akarRepo()
        val lewati = setOf(".git", "build", ".gradle", ".idea", ".kotlin")
        val ditemukan = akar.walkTopDown()
            .onEnter { it.name !in lewati }
            .filter { it.isFile }
            .filter { f ->
                val n = f.name.lowercase()
                n.endsWith(".jks") || n.endsWith(".keystore") || n == "keystore.properties"
            }
            .map { it.relativeTo(akar).path }
            .toList()
        assertTrue("keystore telanjang terlacak di repo: $ditemukan", ditemukan.isEmpty())
    }

    /** Tanpa alasan yang tertulis, baris pengaman ini mudah dianggap berlebihan lalu dihapus. */
    @Test
    fun alasanKeputusanTerdokumentasi() {
        val teks = File(akarRepo(), ".gitignore").readText()
        assertTrue(
            "`.gitignore` tak lagi menjelaskan bahwa repositori ini publik — alasan baris " +
                "pengaman brankas hilang, jadi mudah dihapus orang berikutnya",
            teks.contains("PUBLIK"),
        )
        assertTrue(
            "`.gitignore` tak lagi menyatakan brankas terenkripsi SENGAJA dilacak sementara " +
                "bentuk telanjangnya tidak — pembaca berikutnya akan mengira arsip itu luput",
            teks.contains("SENGAJA"),
        )
        assertTrue(
            "`.gitignore` tak lagi menyebut nama brankas yang dilacak",
            teks.contains("key-sign-apk.zip"),
        )
    }
}
