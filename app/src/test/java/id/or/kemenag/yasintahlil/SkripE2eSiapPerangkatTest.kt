package id.or.kemenag.yasintahlil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Penyiapan emulator sebelum uji instrumented adalah **satu urutan**, bukan tiga baris yang
 * ditulis ulang di tiap jalur.
 *
 * Run E2E `main` `38352153888` merah tanpa menjalankan satu uji pun: percobaan pertama gagal
 * karena emulator tak responsif, lalu **ulangannya** gagal dengan
 * *"Device emulator-5554 API level=1. Cannot install split APKs with API level < 21"*. Sebabnya
 * bukan ujinya: jalur pertama memanggil `adb wait-for-device`, `tunggu_boot`, **dan**
 * `tunggu_properti`, sedangkan jalur ulangan hanya `tunggu_boot`. Bila boot sudah selesai
 * sementara adb belum bisa membaca properti, `tunggu_boot` langsung lewat dan AGP membaca
 * API level sebagai `1` — pemasangan APK ditolak, 0 uji berjalan, dan yang menunggu hanya
 * menyimpulkan ujinya merah padahal tidak ada uji yang berjalan.
 *
 * Salinan kedua yang menyimpang inilah yang ditutup: urutannya kini hidup di
 * `siapkan_perangkat()`, dan kedua jalur memanggilnya. Uji ini menuntut urutan itu tetap **satu**
 * tempat, dan menuntut pemeriksaan angka API tetap ada supaya nilai sampah tidak diteruskan ke AGP.
 */
class SkripE2eSiapPerangkatTest {

    private fun repo(): File =
        generateSequence(KodeSumber.root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, ".github/scripts").isDirectory }
            ?: error(".github/scripts tidak ditemukan dari ${KodeSumber.root()}")

    private fun skrip(): String =
        File(repo(), ".github/scripts/e2e-instrumented.sh").readText()

    /** Baris komentar dibuang: komentar menjelaskan pola lama dengan kata yang sama. */
    private fun tanpaKomentar(teks: String): List<String> =
        teks.lines().filterNot { it.trimStart().startsWith("#") }

    /** Baris (tanpa indentasi) yang isinya **tepat** [isi] — bukan sekadar memuatnya. */
    private fun baris(bersih: List<String>, isi: String): Int =
        bersih.count { it.trim() == isi }

    @Test
    fun urutanPenyiapanPerangkatAdaDiSatuTempat() {
        val bersih = tanpaKomentar(skrip())
        val langkah = listOf("adb wait-for-device", "tunggu_boot", "tunggu_properti")
        langkah.forEach { l ->
            assertEquals("langkah penyiapan \"$l\" tidak lagi dipanggil dari satu tempat saja",
                1, baris(bersih, l))
        }
        assertTrue("urutan penyiapan perangkat tidak lagi dirakit oleh siapkan_perangkat()",
            bersih.any { it.trim() == "siapkan_perangkat() {" })
    }

    /**
     * Kedua jalur — percobaan pertama dan ulangan — harus memakai urutan yang sama. Satu pemanggil
     * berarti salah satu jalur kembali menyusun urutannya sendiri, persis yang membuat ulangan
     * gagal dengan "API level=1".
     */
    @Test
    fun keduaJalurMemakaiPenyiapanYangSama() {
        val bersih = tanpaKomentar(skrip())
        assertEquals("hanya satu jalur yang memakai siapkan_perangkat() — jalur lain menyusun " +
                "urutannya sendiri", 2, baris(bersih, "siapkan_perangkat || exit 1"))
    }

    /**
     * API level yang terbaca harus berupa angka sebelum diteruskan ke AGP: keluaran sampah
     * diterjemahkan AGP menjadi `API level=1`, dan APK-nya lalu ditolak.
     */
    @Test
    fun apiLevelDiperiksaSebelumDiteruskan() {
        val bersih = tanpaKomentar(skrip()).joinToString("\n")
        assertTrue("tunggu_properti tidak lagi menolak API level yang bukan angka",
            bersih.contains("*[!0-9]*"))
        assertFalse("skrip tidak lagi menolak API level kosong",
            bersih.contains("tunggu_properti") && !bersih.contains("Emulator tidak melaporkan API level"))
    }
}
