package id.or.kemenag.yasintahlil.feature.sholat

import id.or.kemenag.yasintahlil.core.data.Aurat
import id.or.kemenag.yasintahlil.core.data.SholatSyarat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Tata Cara Sholat Langkah demi Langkah" membuka daftar langkahnya dengan
 * *"Syarat wajib dan syarat sah"*, *"Persiapan sebelum sholat"*, *"Wudhu dan hal yang
 * membatalkannya"*, *"Penutup aurat dan kebersihan tempat sholat"*, dan *"Penentuan arah
 * kiblat"* — semuanya **di dalam** panduan, sebelum takbiratul ihram.
 *
 * Sebelum ini kelimanya tidak ada di mana pun pada alur belajar: layar detail langsung memulai
 * dari niat dan langkah pertama, sedangkan wudhu serta kiblat hanya dapat dicapai lewat menu lain.
 * Pembaca yang mengikuti tombol "Berikutnya" karena itu tidak pernah diberi tahu bahwa ada syarat
 * yang harus dipenuhi lebih dulu.
 *
 * Dua hal dijaga ketat di sini:
 *
 * 1. **Tidak ada teks Arab pada berkas syarat.** Aturan proyek melarang menyusun lafaz/hadis tanpa
 *    sumber resmi; berkas ini hanya daftar periksa berbahasa Indonesia. Uji ini menolak masuknya
 *    kunci `arab`/`latin` maupun karakter Arab, sehingga berkasnya tidak dapat diam-diam berubah
 *    menjadi tempat menaruh teks agama yang belum terverifikasi.
 * 2. **Status sumber dinyatakan terbuka.** Rinciannya masih menunggu pencocokan dengan modul
 *    Fasolatan resmi, dan pengguna berhak melihat keterangan itu di layar — bukan hanya di kode.
 */
class SyaratSholatTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun berkas(): JSONObject =
        JSONObject(File(root(), "src/main/assets/content/sholat_syarat.json").readText())

    private fun layar(): String = File(root(),
        "src/main/java/id/or/kemenag/yasintahlil/feature/sholat/SholatDetailScreen.kt").readText()

    private fun daftar(kunci: String): List<String> {
        val arr = berkas().getJSONArray(kunci)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    @Test
    fun berkasSyaratAdaDanBersumberResmi() {
        assertTrue("sumber_url bukan domain resmi Kemenag: ${berkas().getString("sumber_url")}",
            berkas().getString("sumber_url").contains("kemenag.go.id"))
        listOf("syarat_wajib", "syarat_sah", "persiapan", "kebersihan_tempat").forEach { k ->
            assertTrue("daftar '$k' kosong", daftar(k).isNotEmpty())
        }
        assertTrue("penutup_aurat tidak ada", berkas().has("penutup_aurat"))
    }

    /** Bukan tempat menaruh lafaz: berkas ini tidak boleh memuat teks Arab sama sekali. */
    @Test
    fun syaratTanpaTeksArab() {
        val teks = File(root(), "src/main/assets/content/sholat_syarat.json").readText()
        val arab = teks.filter { it.code in 0x0600..0x06FF }
        assertTrue("berkas syarat memuat teks Arab: $arab", arab.isEmpty())
        // Kunci `arab`/`latin` akan menuntut harakat dan kategori sumber; menolaknya di sini
        // menjaga berkas ini tetap daftar periksa, bukan wadah bacaan yang belum terverifikasi.
        assertTrue("berkas syarat memuat kunci 'arab'", !teks.contains("\"arab\""))
        assertTrue("berkas syarat memuat kunci 'latin'", !teks.contains("\"latin\""))
    }

    @Test
    fun setiapButirTerisiDanTidakBerulang() {
        listOf("syarat_wajib", "syarat_sah", "persiapan", "kebersihan_tempat").forEach { k ->
            val butir = daftar(k)
            assertTrue("'$k' terlalu sedikit: ${butir.size}", butir.size >= 3)
            assertTrue("'$k' punya butir kosong", butir.all { it.isNotBlank() })
            assertEquals("'$k' punya butir kembar", butir.size, butir.toSet().size)
        }
    }

    /** Batas aurat laki-laki dan perempuan berbeda; keduanya harus disebut, bukan salah satu saja. */
    @Test
    fun penutupAuratMenyebutKeduaBatasDanPerbedaanMazhab() {
        val a = berkas().getJSONObject("penutup_aurat")
        assertTrue("batas aurat laki-laki kosong", a.getString("laki_laki").isNotBlank())
        assertTrue("batas aurat perempuan kosong", a.getString("perempuan").isNotBlank())
        assertTrue("batas aurat laki-laki tidak menyebut pusar/lutut: ${a.getString("laki_laki")}",
            a.getString("laki_laki").lowercase().contains("pusar") &&
                a.getString("laki_laki").lowercase().contains("lutut"))
        assertTrue("batas aurat perempuan tidak menyebut wajah/telapak tangan",
            a.getString("perempuan").lowercase().contains("wajah") &&
                a.getString("perempuan").lowercase().contains("telapak tangan"))
        assertTrue("catatan aurat tidak menyebut perbedaan mazhab",
            a.getString("catatan").lowercase().contains("mazhab"))
    }

    @Test
    fun catatanMazhabDanStatusVerifikasiDinyatakan() {
        assertTrue("catatan mazhab kosong", berkas().getString("catatan_mazhab").isNotBlank())
        val verifikasi = berkas().getString("catatan_verifikasi")
        assertTrue("catatan verifikasi kosong", verifikasi.isNotBlank())
        assertTrue("catatan verifikasi tidak menyatakan status menunggu: $verifikasi",
            verifikasi.lowercase().contains("menunggu"))
    }

    /** Model harus membaca berkasnya utuh — daftar yang hilang berarti bagian itu tak tampil. */
    @Test
    fun modelMembacaSeluruhBagian() {
        val b = berkas()
        val aurat = b.getJSONObject("penutup_aurat")
        val m = SholatSyarat(
            syaratWajib = daftar("syarat_wajib"),
            syaratSah = daftar("syarat_sah"),
            persiapan = daftar("persiapan"),
            penutupAurat = Aurat(
                judul = aurat.getString("judul"),
                lakiLaki = aurat.getString("laki_laki"),
                perempuan = aurat.getString("perempuan"),
                catatan = aurat.getString("catatan"),
            ),
            kebersihanTempat = daftar("kebersihan_tempat"),
        )
        val diharapkan = daftar("syarat_wajib").size + daftar("syarat_sah").size +
            daftar("persiapan").size + daftar("kebersihan_tempat").size + 2
        assertEquals("jumlah butir tidak dihitung dari seluruh bagian", diharapkan, m.jumlah)
        assertTrue("judul aurat kosong", m.penutupAurat.judul.isNotBlank())
    }

    /** Layar detail harus menautkan layar yang sudah ada — kontennya tidak disalin. */
    @Test
    fun layarDetailMenautkanWudhuKiblatDanDzikir() {
        val s = layar()
        listOf(
            "Routes.WUDHU" to "wudhu",
            "Routes.KIBLAT" to "arah kiblat",
            "Routes.DZIKIR" to "dzikir setelah sholat",
        ).forEach { (rute, apa) ->
            assertTrue("layar detail tidak menautkan $apa ($rute)", s.contains("go($rute)"))
        }
    }

    @Test
    fun layarDetailMenerimaNavigatorDanMembacanya() {
        val s = layar()
        assertTrue("SholatDetailScreen tidak menerima navigator", s.contains("go: (String) -> Unit"))
        val nav = File(root(), "src/main/java/id/or/kemenag/yasintahlil/navigation/AppNav.kt").readText()
        assertTrue("AppNav tidak meneruskan navigator ke SholatDetailScreen",
            nav.contains("SholatDetailScreen(") && nav.contains("nav.navigate(r)"))
    }

    /**
     * Syarat harus terbaca **sebelum** langkah pertama. Daftar langkah dibaca berurutan, jadi
     * syarat yang diletakkan setelah takbiratul ihram tidak lagi berguna bagi pembaca yang
     * mengikuti tombol "Berikutnya".
     */
    @Test
    fun syaratTampilSebelumLangkahPertama() {
        val s = layar()
        val syarat = s.indexOf("Text(\"Sebelum sholat\"")
        val langkah = s.indexOf("val b = steps[")
        assertTrue("judul 'Sebelum sholat' tidak ada di layar detail", syarat >= 0)
        assertTrue("navigator langkah tidak ditemukan", langkah >= 0)
        assertTrue("syarat diletakkan setelah langkah pertama", syarat < langkah)
        // Tombol pembuka syarat harus benar-benar mengubah keadaan, bukan tombol hiasan.
        assertTrue("tombol syarat tidak mengubah apa pun",
            s.contains("syaratTerbuka = !syaratTerbuka"))
    }

    /** Status verifikasi harus sampai ke pengguna, bukan berhenti di berkas JSON. */
    @Test
    fun statusVerifikasiDitampilkanDiLayar() {
        assertTrue("layar tidak menampilkan catatan verifikasi",
            layar().contains("syarat.catatanVerifikasi"))
    }
}
