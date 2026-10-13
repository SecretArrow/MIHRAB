package id.or.kemenag.yasintahlil.core.designsystem

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nama **kendali antarmuka** yang muncul di lebih dari satu tempat hanya boleh hidup di
 * `NamaAntarmuka`.
 *
 * Lima belas nama sudah terlanjur ditulis berulang sebelum berkas itu ada:
 *
 *  - *"Pengaturan bacaan"* adalah judul sheet `BacaanSettingsSheet` **dan** label tombol yang
 *    membukanya di sepuluh layar. Sebelas tulisan untuk satu nama: mengganti nama sheet-nya
 *    meninggalkan sepuluh tombol yang menyebut nama lama, sehingga tombol tidak lagi menyebut apa
 *    yang dibukanya — pengguna menekan "Pengaturan bacaan" lalu mendapat sheet berjudul lain.
 *  - *"Layar penuh"* adalah label chip di enam layar bacaan. Satu varian memang berbeda
 *    (`LAYAR_PENUH_IMAM` — pada Sholat Ghaib layar penuh sekaligus mode imam), dan perbedaan itu
 *    **disengaja**: yang dijanjikan tombolnya memang berbeda. Yang tidak boleh adalah salinan yang
 *    berbeda tanpa alasan.
 *  - *"Coba lagi"* ditulis dua kali **di dalam berkas yang sama**: sebagai bawaan `labelTombol`
 *    pada `EmptyState` dan sebagai literal tombol pada `ErrorState`.
 *  - Enam **preferensi tampilan bacaan** (tiga sakelar dan tiga pengukur) ada di **dua berkas**:
 *    sheet `BacaanSettingsSheet` dan layar Pengaturan. Keduanya menulis kunci `Store` yang sama,
 *    jadi keduanya mengatur satu preferensi yang sama — tetapi masing-masing mengeja namanya
 *    sendiri. Mengganti nama satu sakelar di sheet meninggalkan Pengaturan menyebut preferensi
 *    yang sama dengan kata lama, dan pengguna mengira ada dua pengaturan.
 *  - Enam **kendali langkah dan tindakan** juga ada di dua layar atau lebih dengan arti yang sama:
 *    *"← Sebelumnya"* / *"Berikutnya →"* pada panduan sholat dan susunan Tahlil, *"Batalkan tanda"*
 *    pada rincian sholat dan Wudhu, *"Kosongkan"* pada penyaring Panduan Sholat dan Dzikir,
 *    *"Lanjutkan"* pada beranda dan daftar favorit, serta awalan *"Terakhir dibaca: …"* pada ketiga
 *    layar itu beserta Yasin. Tiap pasangan menamai satu tindakan yang sama; kata yang berbeda di
 *    salah satunya membuat pengguna mengira ada dua tombol berbeda untuk hal yang sama.
 *
 * Uji ini menjaga agar kelimabelasnya tetap satu tempat, dan agar varian yang disengaja tetap
 * terlihat sebagai varian — bukan menyamar sebagai salinan.
 */
class NamaAntarmukaSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Kode tanpa komentar: KDoc yang menjelaskan pola lama memuat tulisan yang sama persis. */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun layar(relatif: String): String =
        kode(File(root(), "src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private fun uiKit(): File =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/UiKit.kt")

    /** Kelimabelas nama yang harus tinggal di `NamaAntarmuka`, beserta nilai yang diharapkan. */
    private fun daftarNama(): Map<String, String> = mapOf(
        "PENGATURAN_BACAAN" to NamaAntarmuka.PENGATURAN_BACAAN,
        "LAYAR_PENUH" to NamaAntarmuka.LAYAR_PENUH,
        "LAYAR_PENUH_IMAM" to NamaAntarmuka.LAYAR_PENUH_IMAM,
        "COBA_LAGI" to NamaAntarmuka.COBA_LAGI,
        "TAMPIL_ARAB" to NamaAntarmuka.TAMPIL_ARAB,
        "TAMPIL_LATIN" to NamaAntarmuka.TAMPIL_LATIN,
        "TAMPIL_TERJEMAHAN" to NamaAntarmuka.TAMPIL_TERJEMAHAN,
        "UKURAN_ARAB" to NamaAntarmuka.UKURAN_ARAB,
        "UKURAN_LATIN" to NamaAntarmuka.UKURAN_LATIN,
        "JARAK_ANTARBARIS" to NamaAntarmuka.JARAK_ANTARBARIS,
        "SEBELUMNYA" to NamaAntarmuka.SEBELUMNYA,
        "BERIKUTNYA" to NamaAntarmuka.BERIKUTNYA,
        "BATALKAN_TANDA" to NamaAntarmuka.BATALKAN_TANDA,
        "KOSONGKAN" to NamaAntarmuka.KOSONGKAN,
        "LANJUTKAN" to NamaAntarmuka.LANJUTKAN,
        "TERAKHIR_DIBACA" to NamaAntarmuka.TERAKHIR_DIBACA,
    )

    // ---- Ejaan label ---------------------------------------------------------------------------

    /**
     * Kelimabelas nama itu **teks yang dibaca pengguna**. Mengubahnya adalah keputusan produk,
     * bukan bagian dari merapikan kode: uji ini memaksanya disengaja, sehingga penggantian nama yang
     * tidak diniatkan langsung ketahuan.
     */
    @Test
    fun ejaanLabelAntarmukaTidakBerubah() {
        assertEquals("Pengaturan bacaan", NamaAntarmuka.PENGATURAN_BACAAN)
        assertEquals("Layar penuh", NamaAntarmuka.LAYAR_PENUH)
        assertEquals("Layar penuh (imam)", NamaAntarmuka.LAYAR_PENUH_IMAM)
        assertEquals("Coba lagi", NamaAntarmuka.COBA_LAGI)
        assertEquals("Arab", NamaAntarmuka.TAMPIL_ARAB)
        assertEquals("Latin", NamaAntarmuka.TAMPIL_LATIN)
        assertEquals("Terjemahan", NamaAntarmuka.TAMPIL_TERJEMAHAN)
        assertEquals("Ukuran Arab", NamaAntarmuka.UKURAN_ARAB)
        assertEquals("Ukuran Latin", NamaAntarmuka.UKURAN_LATIN)
        assertEquals("Jarak antarbaris", NamaAntarmuka.JARAK_ANTARBARIS)
        assertEquals("← Sebelumnya", NamaAntarmuka.SEBELUMNYA)
        assertEquals("Berikutnya →", NamaAntarmuka.BERIKUTNYA)
        assertEquals("Batalkan tanda", NamaAntarmuka.BATALKAN_TANDA)
        assertEquals("Kosongkan", NamaAntarmuka.KOSONGKAN)
        assertEquals("Lanjutkan", NamaAntarmuka.LANJUTKAN)
        assertEquals("Terakhir dibaca", NamaAntarmuka.TERAKHIR_DIBACA)
    }

    /** Nama kosong membuat tombol tanpa label — kegagalan yang tidak terlihat di layar. */
    @Test
    fun tidakAdaLayarTanpaNama() {
        val nama = daftarNama()
        nama.forEach { (kunci, nilai) ->
            assertTrue("nama $kunci kosong", nilai.isNotBlank())
            assertEquals("nama $kunci tidak sama dengan dirinya sendiri setelah dirapikan",
                nilai, nilai.trim())
        }
        assertEquals("kelimabelas nama berbeda tidak boleh ada yang sama",
            nama.size, nama.values.toSet().size)
    }

    /**
     * Apakah [teks] memakai [label] sebagai **label kendali** — bukan sekadar menyebut katanya di
     * dalam kalimat.
     *
     * Label kendali adalah **seluruh** teks kendalinya, atau teksnya beserta nilainya
     * (`"Ukuran Arab: 24"` pada pengukur di layar Pengaturan). Kalimat penjelas yang kebetulan
     * memuat kata "Arab" — *"Teks Arab wajib berharakat"*, *"Menyembunyikan Arab berguna saat…"* —
     * bukan salinan kendali, dan menuduhnya sama dengan memaksa prosa menyingkirkan kata biasa.
     */
    private fun memakaiLabel(teks: String, label: String): Boolean =
        teks == label || teks.startsWith("$label:")

    /**
     * Inti perbaikan: tidak ada berkas lain yang mengeja ulang nama itu sebagai literal. Yang
     * menyalinnya ke layar dapat menyebut hal yang sama dengan kata yang berbeda — persis yang
     * terjadi sebelum `NamaAntarmuka` ada.
     *
     * Pemindainya membaca **isi** tiap literal ([KodeSumber.literalString]), bukan potongan
     * berkutip utuh: label yang disisipkan ke dalam kalimat panjang — `"Ukuran Arab: ${…}"` di layar
     * Pengaturan sebelum diperbaiki — tak terbaca oleh pencarian `"Ukuran Arab"` yang menuntut
     * kutipnya utuh, sehingga salinannya lolos. [memakaiLabel] yang memisahkan label kendali dari
     * kata biasa di dalam prosa.
     *
     * Dua berkas dikecualikan, dan keduanya beralasan:
     *
     *  - `NamaAntarmuka.kt` sendiri — tempat nama itu memang tinggal;
     *  - `AtribusiTeks.kt` — memakai kata *"Terjemahan"* untuk hal yang **berbeda**: label baris
     *    asal-usul teks ("Terjemahan: LPMQ Kemenag"), bukan sakelar tampilan. Pengecualian ini
     *    dipaku [terjemahanAtribusiTetapAturanBerbeda], supaya tidak menjadi tempat menyembunyikan
     *    salinan.
     */
    @Test
    fun labelTidakDisalinKeLuarNamaAntarmuka() {
        val nama = daftarNama().values.toList()
        val pelanggar = mutableListOf<String>()
        berkasMain()
            .filter { it.name !in setOf("NamaAntarmuka.kt", "AtribusiTeks.kt") }
            .forEach { f ->
                KodeSumber.literalString(f.readText()).forEach { teks ->
                    nama.filter { memakaiLabel(teks, it) }
                        .forEach { pelanggar += "${f.name}: \"$it\" di dalam \"${teks.take(50)}\"" }
                }
            }
        assertTrue("nama kendali disalin ke luar `NamaAntarmuka`: $pelanggar", pelanggar.isEmpty())
    }

    /**
     * `AtribusiTeks` memang boleh mengeja *"Terjemahan"*: itu label baris **asal-usul** teks, bukan
     * sakelar tampilan. Uji ini memakunya supaya pengecualian di
     * [labelTidakDisalinKeLuarNamaAntarmuka] tidak berubah menjadi celah: bila label itu dihapus
     * dari `AtribusiTeks`, berkasnya tak lagi berhak dikecualikan.
     */
    @Test
    fun terjemahanAtribusiTetapAturanBerbeda() {
        val berkas = KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/data/AtribusiTeks.kt",
        )
        assertTrue("label asal-usul \"Terjemahan\" di AtribusiTeks hilang — pengecualian penjaganya " +
                "kini tanpa alasan",
            KodeSumber.literalString(berkas).any { it == NamaAntarmuka.TAMPIL_TERJEMAHAN })
        assertTrue("AtribusiTeks tidak lagi menyusun baris \"Terjemahan: …\" dari labelnya sendiri",
            berkas.contains("TERJEMAHAN to s.sumber"))
    }

    /**
     * Perbaikan preferensi bacaan harus terlihat **di kedua tempat**: sheet dan layar Pengaturan
     * sama-sama memakai nama bersama. Tanpa ini, penjaga di atas dapat dipuaskan dengan menghapus
     * salah satu tempat, padahal yang salah adalah salinannya, bukan kendalinya.
     */
    @Test
    fun keduaTempatPreferensiBacaanMemakaiNamaBersama() {
        val sheet = kode(uiKit())
        val pengaturan = layar("feature/settings/SettingsScreen.kt")
        listOf(
            "TAMPIL_ARAB", "TAMPIL_LATIN", "TAMPIL_TERJEMAHAN",
            "UKURAN_ARAB", "UKURAN_LATIN", "JARAK_ANTARBARIS",
        ).forEach { kunci ->
            assertTrue("sheet pengaturan bacaan tidak memakai NamaAntarmuka.$kunci",
                sheet.contains("NamaAntarmuka.$kunci"))
            assertTrue("layar Pengaturan tidak memakai NamaAntarmuka.$kunci",
                pengaturan.contains("NamaAntarmuka.$kunci"))
        }
    }

    /**
     * Penjaga di atas hanya berarti bila pemindai komentarnya benar-benar bekerja: satu-satunya
     * sisa tulisan *"Coba lagi"* di `src/main` ada di dalam komentar `SearchScreen.kt`. Kalau
     * komentar tidak dibuang, penjaga itu akan menuduh berkas yang justru menjelaskan perbaikannya —
     * dan kalau pemindainya rusak hingga tak menemukan apa pun, penjaganya hijau tanpa arti.
     */
    @Test
    fun pemindaiKomentarBenarBenarMembuangPenjelasan() {
        val search = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/feature/search/SearchScreen.kt",
        )
        assertTrue("prasyarat: SearchScreen.kt tidak lagi menjelaskan tombol \"Coba lagi\"",
            search.readText().contains(NamaAntarmuka.COBA_LAGI))
        assertFalse("pemindai tidak membuang komentar sehingga menuduh penjelasan yang benar",
            kode(search).contains(NamaAntarmuka.COBA_LAGI))
    }

    // ---- Judul sheet = label tombol ------------------------------------------------------------

    /**
     * Judul sheet pengaturan bacaan dan label tombol yang membukanya adalah **satu** nama, bukan dua
     * yang kebetulan sama. Karena itu `NamaAntarmuka` hanya boleh memuat satu nama untuknya: dua
     * konstanta berbeda akan dapat menyimpang, dan tombol kembali menyebut hal yang tidak dibukanya.
     */
    @Test
    fun judulSheetDanLabelTombolAdalahSatuNama() {
        val berkas = KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/NamaAntarmuka.kt",
        )
        val deklarasi = Regex("=\\s*\"${Regex.escape(NamaAntarmuka.PENGATURAN_BACAAN)}\"")
            .findAll(KodeSumber.kodeTanpaKomentar(berkas)).count()
        assertEquals("nama pengaturan bacaan dideklarasikan lebih dari sekali", 1, deklarasi)

        assertTrue("judul sheet pengaturan bacaan bukan dari kosakata bersama",
            kode(uiKit()).contains("Text(NamaAntarmuka.PENGATURAN_BACAAN"))
    }

    /** Tombol yang membuka sheet harus menyebut namanya dari kosakata yang sama. */
    @Test
    fun tombolPembukaSheetMemakaiNamaYangSama() {
        val pembuka = berkasMain().filter { kode(it).contains("BacaanSettingsSheet(") }
        assertTrue("tak ada layar yang membuka sheet pengaturan bacaan", pembuka.isNotEmpty())
        val pelanggar = pembuka
            .filterNot { kode(it).contains("NamaAntarmuka.PENGATURAN_BACAAN") }
            .map { it.name }
        assertTrue("layar membuka sheet pengaturan bacaan tanpa menyebut namanya: $pelanggar",
            pelanggar.isEmpty())
    }

    // ---- Chip "Layar penuh" --------------------------------------------------------------------

    /**
     * Tiap layar yang punya chip layar penuh memakai salah satu dari dua nama bersama — dan yang
     * benar untuk layar itu. Nama yang tertukar di sini bukan sekadar beda kata: pada Sholat Ghaib
     * chip itu sekaligus menyalakan panduan mode imam, sehingga chip "Layar penuh" biasa di sana
     * menjanjikan hal yang tidak terjadi.
     */
    @Test
    fun tiapLayarChipMemakaiNamaLayarPenuhYangBenar() {
        val biasa = listOf(
            "feature/yasin/YasinScreen.kt",
            "feature/tahlil/TahlilScreen.kt",
            "feature/dzikir/DzikirScreen.kt",
            "feature/adzan/IqomahDoaScreen.kt",
            "feature/kiblat/KiblatScreen.kt",
            "feature/sholat/JenazahScreen.kt",
        )
        biasa.forEach { berkas ->
            assertTrue("$berkas tidak memakai nama chip layar penuh bersama",
                layar(berkas).contains("Text(NamaAntarmuka.LAYAR_PENUH)"))
        }
        val ghaib = layar("feature/sholat/GhaibScreen.kt")
        assertTrue("Sholat Ghaib tidak memakai varian chip mode imam",
            ghaib.contains("Text(NamaAntarmuka.LAYAR_PENUH_IMAM)"))
        assertFalse("Sholat Ghaib memakai chip layar penuh biasa padahal ada varian imam",
            ghaib.contains("Text(NamaAntarmuka.LAYAR_PENUH)"))
    }

    /**
     * Varian imam harus tetap terbaca sebagai **varian** dari nama dasarnya, bukan nama lepas: yang
     * membedakan hanya penanda mode, dan itu yang dijanjikan tombolnya.
     */
    @Test
    fun varianImamTetapVarianDariNamaDasarnya() {
        assertTrue("varian imam tidak berangkat dari nama layar penuh",
            NamaAntarmuka.LAYAR_PENUH_IMAM.startsWith(NamaAntarmuka.LAYAR_PENUH))
        assertTrue("varian imam tidak berbeda dari nama dasarnya",
            NamaAntarmuka.LAYAR_PENUH_IMAM != NamaAntarmuka.LAYAR_PENUH)
        assertTrue("varian imam tidak menandai mode imam",
            NamaAntarmuka.LAYAR_PENUH_IMAM.contains("imam"))
    }

    // ---- "Coba lagi" dua tempat dalam satu berkas ----------------------------------------------

    /**
     * Dahulu `UiKit.kt` memuat nama itu dua kali di dalam dirinya sendiri: sebagai bawaan
     * `labelTombol` pada `EmptyState` dan sebagai literal tombol pada `ErrorState`. Dua tombol yang
     * sama-sama berarti "ulangi" dapat berubah kata tanpa disadari. Kini keduanya satu nama — dan
     * berkasnya harus tetap memuat **kedua** pemakaian itu, agar perbaikannya tidak menghapus salah
     * satunya.
     */
    @Test
    fun keduaTombolCobaLagiDiUiKitMemakaiNamaBersama() {
        val k = kode(uiKit())
        val pemakaian = Regex("""NamaAntarmuka\.COBA_LAGI""").findAll(k).count()
        assertTrue("kedua tombol \"Coba lagi\" di UiKit tidak memakai nama bersama (ditemukan $pemakaian)",
            pemakaian >= 2)
        assertTrue("bawaan `labelTombol` EmptyState bukan dari kosakata bersama",
            k.contains("labelTombol: String = NamaAntarmuka.COBA_LAGI"))
        assertTrue("tombol `ErrorState` bukan dari kosakata bersama",
            k.contains("Button(onClick = onRetry) { Text(NamaAntarmuka.COBA_LAGI) }"))
    }

    /**
     * Kendali langkah dan tindakan yang sama di dua layar (atau lebih) memakai satu nama — dan
     * pemakaiannya harus tetap ada di **kedua** sisi.
     *
     * [labelTidakDisalinKeLuarNamaAntarmuka] menangkap salinannya; uji ini menjaga supaya
     * perbaikannya tidak dipuaskan dengan menghapus salah satu tombol, yang akan menyisakan satu
     * layar tanpa kendali sama sekali.
     */
    @Test
    fun kendaliLangkahDanTindakanMemakaiNamaBersama() {
        val pemakaian = mapOf(
            "SEBELUMNYA" to listOf(
                "feature/sholat/SholatDetailScreen.kt", "feature/tahlil/TahlilScreen.kt",
            ),
            "BERIKUTNYA" to listOf(
                "feature/sholat/SholatDetailScreen.kt", "feature/tahlil/TahlilScreen.kt",
            ),
            "BATALKAN_TANDA" to listOf(
                "feature/sholat/SholatDetailScreen.kt", "feature/sholat/WudhuScreen.kt",
            ),
            "KOSONGKAN" to listOf(
                "feature/sholat/SholatScreen.kt", "feature/dzikir/DzikirScreen.kt",
            ),
            "LANJUTKAN" to listOf(
                "feature/home/HomeScreen.kt", "feature/favorit/FavoritScreen.kt",
            ),
            "TERAKHIR_DIBACA" to listOf(
                "feature/home/HomeScreen.kt", "feature/favorit/FavoritScreen.kt",
                "feature/yasin/YasinScreen.kt",
            ),
        )
        pemakaian.forEach { (kunci, berkas) ->
            berkas.forEach { b ->
                assertTrue("$b tidak memakai NamaAntarmuka.$kunci untuk kendali yang sama",
                    layar(b).contains("NamaAntarmuka.$kunci"))
            }
        }
    }

    /** Aturan nama bersama tetap murni: tanpa Android, agar dapat diuji langsung di JVM. */
    @Test
    fun namaAntarmukaTidakMenyentuhAndroid() {
        val berkas = KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/NamaAntarmuka.kt",
        )
        assertFalse("NamaAntarmuka mengimpor Android sehingga tak dapat diuji di JVM",
            berkas.contains("import android."))
    }
}
