package id.or.kemenag.yasintahlil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Konsep dan Identitas Aplikasi" meminta tiga hal yang mudah hilang tanpa penjaga:
 *
 * - *"Layout responsif untuk berbagai ukuran layar dan kepadatan piksel Android"* — sebelumnya tidak
 *   ada satu pun batas lebar, sehingga di tablet atau ponsel lanskap satu baris terjemahan membentang
 *   sepanjang layar dan sulit diikuti mata.
 * - *"Ornamen geometris Islami yang halus, tanpa membuat antarmuka terlihat ramai"* — sebelumnya
 *   tidak ada ornamen sama sekali.
 * - *"Ikon konsisten, animasi ringan, transisi halus, serta feedback visual pada setiap interaksi"*
 *   — sebelumnya tidak ada animasi apa pun.
 *
 * Ornamen yang dipakai adalah motif **khatam** (dua bujur sangkar, satu diputar 45°): murni
 * geometris, jadi tidak memuat lafaz yang perlu diverifikasi sumbernya. Uji ini menegakkan itu —
 * bila kelak seseorang menambahkan teks Arab ke berkas ornamen, uji ini gagal.
 */
class TataLetakResponsifTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val ds = "src/main/java/id/or/kemenag/yasintahlil/core/designsystem"
    private val responsif get() = sumber("$ds/Responsif.kt")
    private val ornamen get() = sumber("$ds/Ornamen.kt")
    private val uiKit get() = sumber("$ds/UiKit.kt")
    private val appNav = "src/main/java/id/or/kemenag/yasintahlil/navigation/AppNav.kt"
    private val home = "src/main/java/id/or/kemenag/yasintahlil/feature/home/HomeScreen.kt"
    private val sholatDetail = "src/main/java/id/or/kemenag/yasintahlil/feature/sholat/SholatDetailScreen.kt"

    /** Badan sebuah fungsi (di dalam `{ … }`), dicari dengan kurung kurawal yang seimbang. */
    private fun badan(src: String, nama: String): String {
        val mulai = src.indexOf("fun $nama(")
        if (mulai < 0) return ""
        var i = mulai + "fun $nama".length
        var depth = 0
        while (i < src.length) {
            when (src[i]) {
                '(' -> depth++
                ')' -> { depth--; if (depth == 0) { i++; break } }
            }
            i++
        }
        while (i < src.length && src[i] != '{') i++
        if (i >= src.length) return ""
        val dari = i
        depth = 0
        while (i < src.length) {
            when (src[i]) {
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) { i++; break } }
            }
            i++
        }
        return src.substring(dari, minOf(i, src.length))
    }

    /** Nilai dp dari `val NAMA: Dp = <angka>.dp`. */
    private fun dp(src: String, nama: String): Float =
        Regex("""val\s+$nama\s*:\s*Dp\s*=\s*([0-9.]+)\.dp""").find(src)?.groupValues?.get(1)?.toFloat()
            ?: error("Batas '$nama' tidak ditemukan")

    @Test
    fun batasLebarDideklarasikanDanBacaLebihSempitDariIsi() {
        val r = responsif
        assertTrue("Responsif.kt tidak mendeklarasikan LEBAR_KONTEN_MAKS",
            r.contains("val LEBAR_KONTEN_MAKS"))
        assertTrue("Responsif.kt tidak mendeklarasikan LEBAR_BACA_MAKS",
            r.contains("val LEBAR_BACA_MAKS"))
        assertTrue("Responsif.kt tidak menyediakan KolomKonten",
            Regex("""fun\s+KolomKonten\s*\(""").containsMatchIn(r))
        assertTrue("Responsif.kt tidak menyediakan perluasan lebarBaca",
            Regex("""fun\s+Modifier\.lebarBaca\s*\(""").containsMatchIn(r))

        val konten = dp(r, "LEBAR_KONTEN_MAKS")
        val baca = dp(r, "LEBAR_BACA_MAKS")
        // Kartu bacaan memuat teks terpanjang di aplikasi, jadi kolomnya harus lebih sempit —
        // kalau tidak, batas baca ini hanya mengulang batas isi tanpa manfaat.
        assertTrue("LEBAR_BACA_MAKS ($baca) harus lebih sempit dari LEBAR_KONTEN_MAKS ($konten)",
            baca < konten)
    }

    /** Batas lebar dipasang sekali di navigasi, sehingga setiap layar ikut tanpa pengkabelan per layar. */
    @Test
    fun seluruhIsiAplikasiDibatasiLebarnya() {
        val nav = sumber(appNav)
        val kolom = nav.indexOf("KolomKonten(")
        assertTrue("AppNav tidak memakai KolomKonten — isi layar membentang sepanjang layar lebar",
            kolom >= 0)
        val layarPertama = nav.indexOf("composable(")
        assertTrue("composable tidak ditemukan di AppNav", layarPertama >= 0)
        assertTrue("KolomKonten tidak membungkus NavHost (dipasang setelah layar pertama)",
            kolom < layarPertama)
    }

    /** Membatasi lebar isi tidak boleh mengubah navigasi bawah yang sudah ada. */
    @Test
    fun navigasiBawahTetapUtuh() {
        val nav = sumber(appNav)
        assertTrue("NavigationBar hilang", nav.contains("NavigationBar {"))
        assertTrue("NavigationBarItem hilang", nav.contains("NavigationBarItem("))
        listOf("Beranda", "Ibadah", "Alat", "Pengaturan").forEach { label ->
            assertTrue("tab '$label' hilang dari navigasi bawah", nav.contains("\"$label\""))
        }
        assertTrue("ikon navigasi bawah tanpa label aksesibilitas",
            nav.contains("contentDescription = label"))
    }

    /** Kartu bacaan dipusatkan pada kolom yang lebih sempit, bukan dibiarkan selebar layar. */
    @Test
    fun kartuBacaanDibatasiLebihSempitDanDipusatkan() {
        val b = badan(uiKit, "BacaanCard")
        assertTrue("BacaanCard tidak ditemukan", b.isNotBlank())
        assertTrue("kartu bacaan tidak memakai batas lebar baca", b.contains("lebarBaca()"))
        assertTrue("kartu bacaan tidak dipusatkan saat layar lebih lebar",
            b.contains("wrapContentWidth(Alignment.CenterHorizontally)"))
    }

    @Test
    fun ornamenGeometrisTanpaTeksAgama() {
        val o = ornamen
        assertTrue("Ornamen.kt tidak mendeklarasikan OrnamenIslami",
            Regex("""fun\s+OrnamenIslami\s*\(""").containsMatchIn(o))
        // Motifnya harus benar-benar geometris: dua bujur sangkar, satu diputar 45°.
        assertTrue("ornamen bukan motif khatam (tidak ada putaran 45°)", o.contains("rotate(45f"))
        assertTrue("ornamen tidak digambar sebagai garis", o.contains("Stroke("))

        // Tidak boleh ada lafaz: ornamen murni geometris, jadi tak ada teks yang perlu diverifikasi.
        val arab = Regex("[\\u0600-\\u06FF\\u0750-\\u077F]").find(o)
        assertTrue("Ornamen.kt memuat teks Arab '${arab?.value}' — ornamen harus murni geometris",
            arab == null)
        assertTrue("Ornamen.kt memuat kunci teks keagamaan", !o.contains("\"arab\"") && !o.contains("\"latin\""))

        // "Halus": alfa bawaan harus rendah agar tidak berebut perhatian dengan teks.
        val alfa = Regex("""ALFA_ORNAMEN\s*=\s*([0-9.]+)f""").find(o)?.groupValues?.get(1)?.toFloat()
            ?: error("ALFA_ORNAMEN tidak ditemukan")
        assertTrue("alfa ornamen $alfa terlalu pekat — ornamen harus halus", alfa in 0.02f..0.15f)
    }

    @Test
    fun ornamenDipakaiDiBeranda() {
        assertTrue("Beranda tidak memakai ornamen", sumber(home).contains("OrnamenIslami("))
        // Digambar di belakang isi, bukan menutupinya.
        assertTrue("ornamen tidak dipasang sebagai latar (matchParentSize)",
            sumber(home).contains("OrnamenIslami(Modifier.matchParentSize())"))
    }

    @Test
    fun animasiRinganDipakaiPadaUmpanBalikDanTransisi() {
        val counter = badan(uiKit, "CounterRow")
        assertTrue("CounterRow tidak ditemukan", counter.isNotBlank())
        assertTrue("progres penghitung tidak dianimasikan",
            counter.contains("animateFloatAsState"))

        val kartu = badan(uiKit, "BacaanCard")
        assertTrue("tinggi kartu bacaan tidak dianimasikan", kartu.contains("animateContentSize"))

        assertTrue("daftar syarat sholat muncul seketika, tanpa transisi",
            sumber(sholatDetail).contains("AnimatedVisibility("))
    }

    // ---- Tepi-ke-tepi (inset bilah sistem) ----

    private val main = "src/main/java/id/or/kemenag/yasintahlil/MainActivity.kt"

    /**
     * Batas lebar saja belum cukup untuk "berbagai ukuran layar": bilah sistem juga memakan tepi
     * layar. Tanpa tepi-ke-tepi, area di sekitar poni (notch) dan di belakang bilah gestur tidak
     * terpakai — dan bila `targetSdk` kelak dinaikkan ke 35, tepi-ke-tepi diberlakukan sistem
     * sehingga tata letak yang tak menghitung inset akan tertutup bilah.
     */
    @Test
    fun aplikasiMenggambarTepiKeTepi() {
        val src = sumber(main)
        val tepi = src.indexOf("enableEdgeToEdge()")
        assertTrue("MainActivity tidak memanggil enableEdgeToEdge()", tepi >= 0)
        val isi = src.indexOf("setContent {")
        assertTrue("setContent tidak ditemukan di MainActivity", isi >= 0)
        // Jendela harus diatur sebelum frame pertama digambar; memanggilnya di dalam komposisi
        // berarti bingkai pertama masih memakai bilah berlatar warna.
        assertTrue("enableEdgeToEdge() harus dipanggil sebelum setContent", tepi < isi)
    }

    /**
     * Inset bilah sistem ditambahkan sekali di navigasi, bukan diabaikan. `Scaffold` Material3 sudah
     * memakai `WindowInsets.systemBars` sebagai bawaan; yang berbahaya adalah menimpanya dengan inset
     * nol, karena itu mematikan penambahan inset untuk **setiap** layar sekaligus.
     */
    @Test
    fun insetSistemDitambahkanDiNavigasi() {
        val nav = sumber(appNav)
        assertTrue("Scaffold hilang dari AppNav", nav.contains("Scaffold("))
        assertTrue("Scaffold menonaktifkan inset bilah sistem (WindowInsets(0))",
            !nav.contains("contentWindowInsets = WindowInsets(0"))
        assertTrue("isi layar tidak memakai padding inset dari Scaffold",
            nav.contains("KolomKonten(Modifier.padding(pad))"))
    }

    /**
     * Warna latar bilah sistem kini tembus pandang, jadi keterbacaan ikonnya ditentukan **tema
     * aplikasi** — bukan tema sistem. Pengguna dapat memilih tema yang berlawanan dengan
     * perangkatnya; tanpa sinkronisasi ini ikon status bar tetap terang di atas latar terang.
     */
    @Test
    fun ikonBilahSistemMengikutiTemaAplikasi() {
        val src = sumber(main)
        assertTrue("ikon bilah status tidak disinkronkan dengan tema aplikasi",
            src.contains("isAppearanceLightStatusBars = !dark"))
        assertTrue("ikon bilah navigasi tidak disinkronkan dengan tema aplikasi",
            src.contains("isAppearanceLightNavigationBars = !dark"))
        // Diambil dari aturan tema bersama, bukan pembacaan kedua `Store.DARK` yang dapat berbeda.
        assertTrue("warna ikon bilah sistem tidak berasal dari aturan tema bersama",
            src.contains("TemaAplikasi.gelap("))
    }

    /** Isi berkas relatif terhadap akar repositori — uji dijalankan dari `app/` maupun akar. */
    private fun berkasRepo(relatif: String): String =
        listOf(File("."), File("app"), File("..")).map { File(it, relatif) }
            .firstOrNull { it.isFile }?.readText()
            ?: error("$relatif tidak ditemukan dari ${File(".").absolutePath}")

    /** Animasi yang dipakai harus benar-benar ada di kelas path, bukan sekadar mengandalkan transitif. */
    @Test
    fun dependensiAnimasiDideklarasikan() {
        assertTrue("app/build.gradle.kts tidak mendeklarasikan compose-animation",
            berkasRepo("app/build.gradle.kts").contains("libs.compose.animation"))
        assertTrue("libs.versions.toml tidak memuat entri compose-animation",
            berkasRepo("gradle/libs.versions.toml").contains("androidx.compose.animation"))
    }
}
