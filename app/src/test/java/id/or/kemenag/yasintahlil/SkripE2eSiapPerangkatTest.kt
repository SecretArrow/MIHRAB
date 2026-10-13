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
 *
 * Penjaga yang sama juga menutup dua penyimpangan berikutnya, keduanya ditemukan dari run `main`
 * `38366620795` — gerbangnya melaporkan perangkat siap, lalu AGP tetap melewatinya:
 *
 * 1. Gerbang kesiapan mengukur dengan perintah yang **berbeda** daripada pembacanya (satu properti
 *    versus seluruh tabel properti), sehingga lulus sementara pembacaan penuh masih kehabisan waktu.
 * 2. Runner menyediakan `/dev/kvm` tetapi tidak membukanya, jadi aksi emulator memaksa `-accel off`
 *    dan emulatornya berjalan tanpa akselerasi perangkat keras.
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

    /**
     * Isi fungsi shell bernama [nama], dari `nama() {` sampai `}` penutup di kolom nol.
     *
     * Diperlukan karena pencarian potongan yang longgar di seluruh berkas mudah tertipu: setelah
     * `perintah_siap` dipakai di dua fungsi, memeriksa "berkas memuat `perintah_siap`" tetap hijau
     * meski fungsi gerbangnya berhenti memakainya.
     */
    private fun badanFungsi(bersih: List<String>, nama: String): String {
        val mulai = bersih.indexOfFirst { it.trim() == "$nama() {" }
        assertTrue("fungsi $nama() tidak ditemukan", mulai >= 0)
        val akhir = (mulai + 1 until bersih.size).first { bersih[it] == "}" }
        return bersih.subList(mulai, akhir + 1).joinToString("\n")
    }

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

    /**
     * Kesiapan diukur dengan perintah yang **sama** dengan yang dipakai pembacanya.
     *
     * ddmlib — lewat AGP — membaca seluruh tabel properti (`adb shell getprop` tanpa argumen)
     * dengan batas 5 detik, dan perintah itulah yang gagal dengan `ShellCommandUnresponsiveException`
     * sehingga AGP berkata "Unknown API Level". Gerbang yang hanya membaca **satu** properti jauh
     * lebih ringan, jadi ia bisa lulus sementara pembacaan penuh masih kehabisan waktu: run `main`
     * `38366620795` melaporkan `API='29'` dua kali, lalu AGP tetap melewati perangkat dua kali.
     */
    @Test
    fun kesiapanDiukurDenganPerintahYangSamaDenganPembacanya() {
        val bersih = tanpaKomentar(skrip())
        val teks = bersih.joinToString("\n")
        assertTrue("kesiapan perangkat tidak lagi diukur oleh satu fungsi bersama",
            teks.contains("perintah_siap() {"))
        // Bentuk tanpa argumen — persis perintah ddmlib. Bentuk berargumen (`… getprop <kunci>`)
        // tidak dihitung: itulah pembacaan satu properti yang dulu dipakai sebagai gerbang.
        assertTrue("perintah kesiapan bukan pembacaan SELURUH tabel properti seperti ddmlib",
            teks.contains("adb shell getprop > /dev/null 2>&1"))
        assertTrue("perintah kesiapan tidak dibatasi batas waktu ddmlib",
            teks.contains("timeout \"\$BATAS_PROPERTI\" adb shell getprop > /dev/null 2>&1"))
        // Angkanya dipaku pada bawaan ddmlib (`PropertyFetcher.GETPROP_TIMEOUT_SEC` = 2 detik).
        // Gerbang yang lebih longgar lulus justru untuk perangkat yang, di mata AGP, perintah
        // penuhnya masih kehabisan waktu.
        assertTrue("batas waktu ddmlib tidak lagi dipaku pada bawaannya (2 detik)",
            teks.contains("BATAS_PROPERTI=2"))
        // Kedua penunggu harus memakai perintah yang sama; diperiksa per fungsi supaya satu
        // pemakaian di fungsi lain tidak menutupi hilangnya pemakaian di gerbangnya.
        listOf("tunggu_properti", "tunggu_tenang").forEach { f ->
            assertTrue("$f() tidak menunggu perintah kesiapan yang sama",
                badanFungsi(bersih, f).contains("perintah_siap"))
        }
    }

    /**
     * Ulangan menunggu **bukti**, bukan angka tetap.
     *
     * Dahulu di jalur ulangan berdiri `sleep 15` — angka tunggu yang tak berhubungan dengan apa pun
     * yang diperiksa, sehingga perangkat "sudah siap" menurut urutan penyiapan tetapi tetap belum
     * responsif ketika AGP bertanya. Yang ditunggu kini adalah buktinya sendiri.
     */
    @Test
    fun ulanganMenungguBuktiBukanAngkaTetap() {
        val bersih = tanpaKomentar(skrip())
        assertFalse("jalur ulangan kembali menunggu dengan angka tetap (sleep 15)",
            bersih.any { it.trim() == "sleep 15" })
        // Yang dituntut adalah **pemanggilannya**, bukan sekadar definisinya: baris definisi
        // `tunggu_tenang() {` juga diawali namanya, sehingga pencarian yang longgar tetap hijau
        // meski tak satu pun jalur memanggilnya.
        assertTrue("jalur ulangan tidak menunggu emulator benar-benar tenang",
            bersih.any { it.trim().startsWith("tunggu_tenang ||") })
    }

    /**
     * Akselerasi perangkat keras dibuka **sebelum** aksi emulator dipanggil.
     *
     * Aksi `reactivecircus/android-emulator-runner` memutuskan lewat
     * `accessSync('/dev/kvm', R_OK | W_OK)`. Di runner ini `/dev/kvm` ada tetapi tidak terbuka untuk
     * pengguna `runner` (emulator melaporkannya sendiri: *"This user doesn't have permissions to use
     * KVM (/dev/kvm)"*), sehingga aksinya memaksa `-accel off`. Emulasi perangkat lunak itu begitu
     * lambat sampai pembacaan properti ddmlib kehabisan waktu — itulah sebab sebenarnya kegagalan
     * "Unknown API Level", bukan CPU atau RAM.
     */
    @Test
    fun aksesKvmDibukaSebelumEmulatorDinyalakan() {
        val alur = File(repo(), ".github/workflows/e2e.yml").readText()
        val aksi = alur.indexOf("reactivecircus/android-emulator-runner")
        assertTrue("aksi emulator tidak lagi dipakai di e2e.yml", aksi >= 0)
        val kvm = alur.indexOf("sudo chmod 666 /dev/kvm")
        assertTrue("workflow tidak membuka izin /dev/kvm", kvm >= 0)
        assertTrue("izin /dev/kvm dibuka SETELAH aksi emulator — pemeriksaan aksinya sudah terlanjur " +
                "membaca /dev/kvm yang belum terbuka, jadi `-accel off` tetap dipaksakan", kvm < aksi)
        assertTrue("perintah pembuka izin tidak dijaga `|| true`: job dapat jatuh hanya karena " +
                "/dev/kvm tidak ada", alur.contains("sudo chmod 666 /dev/kvm || true"))
        assertFalse("workflow memaksa emulator tanpa akselerasi perangkat keras",
            alur.contains("disable-linux-hw-accel: true"))
    }
}
