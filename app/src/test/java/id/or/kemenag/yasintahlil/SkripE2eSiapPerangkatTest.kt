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

    /**
     * Jalur kegagalan harus **membuang bukti**, bukan hanya 60 baris log Gradle.
     *
     * Run `main` `38466684400` merah dengan keluaran uji hanya *"No compose hierarchies found in the
     * app"* — galat yang dapat berarti Activity-nya tak pernah jalan, isinya tak pernah di-`setContent`,
     * atau prosesnya mati — dan tak ada satu pun jejak untuk membedakannya, sebab `periksa_crash`
     * hanya dipanggil di jalur sukses. Akibatnya setiap kegagalan berbiaya satu siklus CI tambahan
     * hanya untuk menebak sebabnya. Uji ini menuntut jalur kegagalan memanggil `buang_bukti`, dan
     * menuntut fungsi itu benar-benar membaca logcat **dan** laporan uji.
     */
    @Test
    fun jalurKegagalanMembuangBukti() {
        val bersih = tanpaKomentar(skrip())
        // Potongan sesudah cabang sukses = jalur kegagalan. Diukur dari `if jalankan_uji; then`,
        // bukan dari pencarian longgar di seluruh berkas: definisi fungsinya sendiri juga memuat
        // namanya, sehingga "berkas memuat buang_bukti" tetap hijau meski tak satu jalur pun
        // memanggilnya.
        val sesudahSukses = bersih.dropWhile { it.trim() != "if jalankan_uji; then" }.joinToString("\n")
        // Kedua jalur — percobaan pertama dan ulangan — harus membuang bukti. Satu saja berarti
        // jalur lainnya kembali merah tanpa jejak, persis yang membuat kegagalan berikutnya
        // berbiaya satu siklus CI tambahan.
        assertEquals("hanya satu jalur kegagalan yang memanggil buang_bukti()", 2,
            baris(sesudahSukses.lines(), "buang_bukti"))

        val badan = badanFungsi(bersih, "buang_bukti")
        assertTrue("buang_bukti() tidak membaca buffer crash", badan.contains("-b crash"))
        assertTrue("buang_bukti() tidak membaca logcat aplikasi", badan.contains("adb logcat -d"))
        assertTrue("buang_bukti() tidak memeriksa apakah proses aplikasinya masih hidup",
            badan.contains("ps -A"))
        assertTrue("buang_bukti() tidak membuang laporan uji XML",
            badan.contains("androidTest-results"))
    }

    /**
     * `NavSmokeTest` memberi izin notifikasi **sebelum** Activity diluncurkan.
     *
     * `MainActivity.onCreate` meminta `POST_NOTIFICATIONS` sejak API 33. Di emulator API 29 yang
     * dipakai sebelumnya cabang itu tak pernah jalan; setelah `api-level` naik ke 36, dialog izin
     * sistem muncul tepat saat Activity start dan mengambil fokus dari jendela aplikasi — uji yang
     * meluncurkan Activity sungguhan lalu gagal dengan *"No compose hierarchies found"* meski
     * aplikasinya sehat (run `main` `38466684400`: 40 uji lain lolos, hanya dua uji kelas ini merah).
     *
     * Pemberian izin itu harus berada di aturan **luar** rantai: `@Before` sudah terlambat, sebab
     * aturan Compose meluncurkan Activity-nya lebih dulu.
     */
    @Test
    fun ujiAsapMemberiIzinNotifikasiSebelumActivityDiluncurkan() {
        val teks = File(
            repo(),
            "app/src/androidTest/java/id/or/kemenag/yasintahlil/NavSmokeTest.kt",
        ).readText()
        val kode = KodeSumber.kodeTanpaKomentar(teks)

        assertTrue("NavSmokeTest tidak lagi meluncurkan Activity sungguhan",
            kode.contains("createAndroidComposeRule<MainActivity>()"))
        assertTrue("NavSmokeTest tidak lagi memberi izin POST_NOTIFICATIONS",
            kode.contains("grantRuntimePermission"))
        assertTrue("izin tidak diberikan lewat aturan luar rantai — @Before sudah terlambat karena " +
                "aturan Compose meluncurkan Activity lebih dulu",
            kode.contains("RuleChain.outerRule(izinNotifikasi).around(rule)"))

        // Aturan Compose tidak boleh ber-`@Rule` sendiri: dengan begitu ia diterapkan dua kali
        // (sekali langsung, sekali di dalam rantai) dan Activity-nya diluncurkan dua kali.
        val anotasi = Regex("""@get:Rule""").findAll(kode).count()
        assertEquals("NavSmokeTest memasang $anotasi aturan; hanya rantainya yang boleh ber-@Rule",
            1, anotasi)
        val baris = kode.lines()
        val rantai = baris.indexOfFirst { it.contains("val rantai") }
        assertTrue("deklarasi rantai aturan tidak ditemukan", rantai > 0)
        val sebelumnya = baris.subList(0, rantai).last { it.isNotBlank() }.trim()
        assertEquals("@Rule tidak lagi menempel pada rantai aturannya", "@get:Rule", sebelumnya)
    }

    /**
     * Kesalahan kompilasi di `app/src/androidTest` harus gugur **sebelum** merge.
     *
     * `lintDebug`, `testDebugUnitTest`, `assembleDebug`, dan `assembleRelease` tak satu pun
     * menyentuh `app/src/androidTest`; `e2e.yml` yang mengompilasinya — dan alur itu sengaja hanya
     * jalan di `main`/nightly. Tanpa langkah kompilasi di `ci.yml`, uji instrumented yang tidak
     * dapat dikompilasi tetap lolos seluruh gerbang PR, lalu merah setelah digabung: satu siklus
     * PR penuh untuk sesuatu yang seharusnya gugur di depan. Tugas yang dituntut di sini persis
     * yang dipakai `e2e.yml`, sehingga hasilnya terpakai lewat `--build-cache`.
     */
    @Test
    fun androidTestDikompilasiSebelumMerge() {
        val alur = File(repo(), ".github/workflows/ci.yml").readText()
        assertTrue("ci.yml tidak lagi mengompilasi sumber androidTest — kesalahan kompilasi di " +
                "sana baru terbaca setelah merge, saat e2e.yml menjalankannya",
            alur.contains(":app:assembleDebugAndroidTest"))
        val e2e = File(repo(), ".github/workflows/e2e.yml").readText()
        assertTrue("e2e.yml tidak lagi memakai tugas kompilasi yang sama dengan ci.yml, sehingga " +
                "hasil ci.yml tidak terpakai lewat --build-cache",
            e2e.contains(":app:assembleDebugAndroidTest"))
    }
}
