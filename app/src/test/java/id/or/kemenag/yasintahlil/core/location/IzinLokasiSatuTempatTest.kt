package id.or.kemenag.yasintahlil.core.location

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Daftar izin lokasi yang diminta aplikasi harus tetap **satu** — dan harus sepadan dengan yang
 * dinilai "sudah diberikan".
 *
 * Layar Kiblat dan Jadwal dahulu masing-masing menuliskan `arrayOf(ACCESS_FINE_LOCATION,
 * ACCESS_COARSE_LOCATION)` sendiri, dua kali per layar: **empat salinan** untuk satu keputusan.
 * Bahayanya bukan panjangnya, melainkan pasangannya — [LocationProvider.hasPermission] menilai
 * "izin sudah ada" dari daftar yang sama, sementara Android 12+ membiarkan pengguna menyetujui
 * **perkiraan saja** (hanya izin kasar). Salinan yang menyimpang membuat layar menunggu izin yang
 * tak pernah ia minta: tombol GPS-nya diam, tanpa satu pun pesan galat.
 *
 * Karena itu uji ini memaku tiga hal: daftarnya hanya dieja di `LocationProvider.kt`, layar
 * benar-benar memakainya, dan **manifest** menyatakan izin yang sama — `uses-permission` yang
 * hilang membuat permintaannya ditolak sistem tanpa suara.
 *
 * Penjaga "satu tempat" itu dibatasi pada izin **lokasi** (`Manifest.permission.ACCESS_*`).
 * Aplikasi meminta izin lain juga — notifikasi, misalnya, diminta `MainActivity` dan layar Adzan —
 * dan izin itu punya aturannya sendiri; mencampurnya di sini hanya akan membuat pesan gagalnya
 * menunjuk berkas yang salah.
 */
class IzinLokasiSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun berkas(relatif: String): String = KodeSumber.sumber(relatif)

    private fun kode(relatif: String): String = KodeSumber.kodeTanpaKomentar(berkas(relatif))

    private val lokasi get() = "src/main/java/id/or/kemenag/yasintahlil/core/location/LocationProvider.kt"

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /**
     * Nama izin yang diketik di blok `IZIN` pada `LocationProvider.kt`, urut seperti tertulis.
     *
     * Polanya sengaja sempit (`IZIN` sampai `)` pertama) supaya daftar lain di berkas yang sama tidak
     * ikut terbaca dan membuat penjaganya hampa.
     */
    private fun izinDiminta(): List<String> {
        val m = Regex("""IZIN\s*:\s*List<String>\s*=\s*listOf\(([^)]*)\)""").find(kode(lokasi))
        assertTrue("deklarasi LocationProvider.IZIN tidak ditemukan", m != null)
        return Regex("""Manifest\.permission\.(\w+)""")
            .findAll(m!!.groupValues[1]).map { it.groupValues[1] }.toList()
    }

    /** `Manifest.permission.X` bernilai `android.permission.X` — pemetaan tetap dari platform. */
    private fun namaManifest(konstanta: String) = "android.permission.$konstanta"

    @Test
    fun daftarIzinLokasiHanyaDiejaDiLocationProvider() {
        val pelanggar = mutableListOf<String>()
        berkasMain().filter { it.name != "LocationProvider.kt" }.forEach { f ->
            if (KodeSumber.kodeTanpaKomentar(f.readText()).contains("Manifest.permission.ACCESS_")) {
                pelanggar += f.name
            }
        }
        assertTrue("daftar izin lokasi dieja ulang di berkas lain: $pelanggar", pelanggar.isEmpty())
    }

    /** Yang diminta harus tetap dua izin itu, urut halus lalu kasar. */
    @Test
    fun izinYangDimintaDipaku() {
        assertEquals("izin lokasi yang diminta berubah",
            listOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"), izinDiminta())
    }

    /**
     * Penilaian "izin sudah ada" harus **diturunkan** dari daftar yang sama, bukan menuliskan
     * izinnya sendiri: itulah yang menjamin apa pun yang dapat dihasilkan dialog izin juga diakui
     * sudah diberikan.
     */
    @Test
    fun penilaianIzinDiturunkanDariDaftarYangSama() {
        val k = kode(lokasi)
        val badan = k.substringAfter("fun hasPermission(ctx: Context): Boolean =")
            .substringBefore("\n\n")
        assertTrue("hasPermission tidak lagi membaca LocationProvider.IZIN",
            badan.contains("IZIN.any"))
        assertTrue("hasPermission menuliskan izinnya sendiri alih-alih membaca daftarnya",
            !badan.contains("Manifest.permission."))
    }

    /** Kedua layar harus meminta izin lewat daftar bersama — bukan menyusun `arrayOf` sendiri. */
    @Test
    fun layarMemintaIzinLewatDaftarBersama() {
        mapOf(
            "Kiblat" to "src/main/java/id/or/kemenag/yasintahlil/feature/kiblat/KiblatScreen.kt",
            "Jadwal" to "src/main/java/id/or/kemenag/yasintahlil/feature/jadwal/JadwalScreen.kt",
        ).forEach { (nama, rel) ->
            val k = kode(rel)
            assertEquals("$nama tidak lagi meminta izin lokasi",
                2, Regex("""LocationProvider\.IZIN\.toTypedArray\(\)""").findAll(k).count())
            assertTrue("$nama menyusun daftar izin lokasinya sendiri",
                !k.contains("Manifest.permission.ACCESS_"))
        }
    }

    /**
     * Manifest dan daftar permintaan harus menyatakan izin lokasi yang **sama**. `uses-permission`
     * yang hilang membuat permintaannya ditolak sistem tanpa suara — layarnya menunggu jawaban yang
     * tak akan datang.
     */
    @Test
    fun manifestMenyatakanIzinYangSama() {
        val manifest = berkas("src/main/AndroidManifest.xml")
        val diManifest = Regex("""android:name="(android\.permission\.[A-Z_]*LOCATION)"""")
            .findAll(manifest).map { it.groupValues[1] }.toSet()
        assertEquals("manifest menyatakan izin lokasi yang berbeda dari yang diminta aplikasi",
            izinDiminta().map { namaManifest(it) }.toSet(), diManifest)
    }
}
