package id.or.kemenag.yasintahlil.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Penjaga kontrak widget jadwal sholat — versi "parser berkas" ala budaya uji repo ini
 * (lihat `IconAndThemeResourcesTest`, `OfflineManifestTest`): widget RemoteViews tidak dapat
 * dijalankan di JVM, sehingga yang dijaga adalah **janji-janjinya** lewat isi berkasnya:
 *
 *  - tata letak hanya memuat jenis view yang didukung RemoteViews (di sini: `LinearLayout` dan
 *    `TextView` saja — jejak minimal; view lain seperti `EditText` membuat widget gagal dimuat
 *    di layar utama tanpa satu pun galat waktu kompilasi);
 *  - metadata memakai periode pembaruan yang sah (≥ 30 menit — sistem menolak nilai di bawahnya)
 *    dan menunjuk tata letak yang benar;
 *  - manifes mendaftarkan receiver widget lengkap dengan aksi & meta-datanya — receiver yang
 *    tidak terdaftar tidak pernah muncul di pemilih widget, dan widget yang tidak pernah
 *    dipilih tidak akan pernah diuji siapa pun;
 *  - hisabnya memakai aturan bersama (`PrayerCalc` + `JadwalHariIni`) dan tidak menyebut
 *    jaringan sama sekali — janji offline dijaga kode, bukan komentar;
 *  - keputusan "tanpa dependensi baru" tetap utuh: tidak ada Glance di berkas gradle.
 */
class WidgetJadwalTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun res(path: String): File = File(root(), "src/main/res/$path")

    private fun manifest(): String = File(root(), "src/main/AndroidManifest.xml").readText()

    private fun provider(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/widget/WidgetJadwalProvider.kt").readText()

    /** XML tanpa komentar — komentar yang menyebut nama tag tidak boleh menipu pemindai. */
    private fun xmlTanpaKomentar(teks: String): String = teks.replace(Regex("<!--[\\s\\S]*?-->"), "")

    // ---- Tata letak -------------------------------------------------------------------------

    @Test
    fun layoutWidgetAdaDanHanyaMemuatViewYangDidukungRemoteViews() {
        val berkas = res("layout/widget_jadwal.xml")
        assertTrue("layout/widget_jadwal.xml tidak ada", berkas.isFile)
        val xml = xmlTanpaKomentar(berkas.readText())
        val tag = Regex("<\\s*([A-Za-z][A-Za-z0-9.]*)").findAll(xml).map { it.groupValues[1] }.toSet()
        val asing = tag - setOf("LinearLayout", "TextView")
        assertTrue("layout widget memuat view yang tidak didukung RemoteViews: $asing", asing.isEmpty())
    }

    @Test
    fun layoutMemuatSeluruhIdYangDiisiKode() {
        val xml = res("layout/widget_jadwal.xml").readText()
        val id = listOf("widget_judul", "widget_tanggal", "widget_kota") +
            listOf("subuh", "zuhur", "asar", "magrib", "isya").flatMap {
                listOf("widget_nama_$it", "widget_jam_$it")
            }
        val hilang = id.filter { !xml.contains("+id/$it\"") }
        assertTrue("id tampilan widget tidak ada di layout: $hilang", hilang.isEmpty())
    }

    // ---- Metadata widget --------------------------------------------------------------------

    @Test
    fun metadataWidgetSahDanMenunjukLayoutYangBenar() {
        val berkas = res("xml/widget_jadwal_info.xml")
        assertTrue("xml/widget_jadwal_info.xml tidak ada", berkas.isFile)
        val xml = berkas.readText()
        assertEquals("initialLayout harus menunjuk widget_jadwal",
            "@layout/widget_jadwal", Regex("android:initialLayout=\"([^\"]+)\"").find(xml)?.groupValues?.get(1))
        val periode = Regex("android:updatePeriodMillis=\"(\\d+)\"").find(xml)?.groupValues?.get(1)?.toInt() ?: -1
        assertTrue(
            "updatePeriodMillis $periode di bawah 1800000 — sistem menolak nilai di bawah 30 menit " +
                "dan diam-diam memakai 30 menit, jadi menuliskannya hanya menipu pembaca kode",
            periode >= 1_800_000,
        )
        assertTrue("resizeMode tidak lengkap (widget 4x3 dapat digeser dua arah)",
            xml.contains("android:resizeMode=\"horizontal|vertical\""))
    }

    @Test
    fun latarWidgetAdalahBentukBerwarnaPalet() {
        val berkas = res("drawable/widget_latar.xml")
        assertTrue("drawable/widget_latar.xml tidak ada", berkas.isFile)
        val xml = berkas.readText()
        assertTrue("latar widget bukan shape", xml.contains("<shape"))
        assertTrue("latar widget tanpa sudut membulat", xml.contains("<corners"))
        assertTrue(
            "latar widget memakai warna di luar palet colors.xml — identitas zamrud harusnya " +
                "datang dari @color, bukan hex lepas yang bisa menyimpang diam-diam",
            xml.contains("@color/"),
        )
    }

    // ---- Manifes ------------------------------------------------------------------------

    @Test
    fun manifesMendaftarkanReceiverWidgetLengkap() {
        val nama = "id.or.kemenag.yasintahlil.widget.WidgetJadwalProvider"
        val m = manifest()
        assertTrue("receiver widget tidak terdaftar di manifes", m.contains(nama))
        // Aksi dan meta-data diperiksa di dalam blok receivernya sendiri, bukan di seluruh
        // manifes: FileProvider juga memakai android:resource, dan pencarian longgar akan hijau
        // meskipun meta-data widget hilang.
        val blok = m.substringAfter(nama).substringBefore("</receiver>")
        assertTrue("receiver widget tanpa aksi APPWIDGET_UPDATE — tidak muncul di pemilih widget",
            blok.contains("android.appwidget.action.APPWIDGET_UPDATE"))
        assertTrue("receiver widget tanpa meta-data provider", blok.contains("android.appwidget.provider"))
        assertEquals("meta-data widget harus menunjuk widget_jadwal_info",
            "@xml/widget_jadwal_info",
            Regex("android:resource=\"([^\"]+)\"").find(blok)?.groupValues?.get(1))
        assertTrue("receiver widget seharusnya exported=false (templat Android modern)",
            blok.contains("android:exported=\"false\""))
    }

    // ---- Hisab offline ------------------------------------------------------------------

    @Test
    fun providerMenghitungDenganAturanBersamaTanpaJaringan() {
        val s = provider()
        assertTrue("provider tidak memuat layout widget", s.contains("R.layout.widget_jadwal"))
        assertTrue("hisab widget tidak memakai PrayerCalc — janji offline berarti dihitung di perangkat",
            s.contains("PrayerCalc"))
        assertTrue("pasangan nama→jam widget tidak memakai aturan bersama JadwalHariIni",
            s.contains("JadwalHariIni"))
        assertFalse("kode widget menyebut jaringan — widget harus murni offline",
            s.contains("http"))
    }

    // ---- Tanpa dependensi baru ----------------------------------------------------------

    @Test
    fun berkasGradleTidakMenambahkanGlance() {
        // `root()` adalah direktori modul `app` (tempat `src/main` berada) — katalog versi ada
        // di akar repo (satu tingkat di atas modul), sedangkan skrip gradle modul di modulnya sendiri.
        listOf("../gradle/libs.versions.toml", "build.gradle.kts").forEach { jalur ->
            val berkas = File(root(), jalur)
            assertTrue("$jalur tidak ditemukan", berkas.isFile)
            assertFalse(
                "$jalur menyebut Glance — keputusan widget ini murni RemoteViews klasik, tanpa " +
                    "dependensi yang tak dapat dicoba pipeline tanpa build lokal",
                berkas.readText().contains(Regex("glance", RegexOption.IGNORE_CASE)),
            )
        }
    }
}
