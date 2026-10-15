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
 * Sembilan belas nama sudah terlanjur ditulis berulang sebelum berkas itu ada:
 *
 * - *"Pengaturan bacaan"* adalah judul bagian di layar Pengaturan. Dahulu ia judul bottom sheet
 *   **dan** label tombol yang membukanya di sepuluh layar — sebelas tulisan untuk satu nama.
 *   Pengaturan itu kini terpusat, dan namanya tetap satu di tempat tunggalnya.
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
 *  - Enam **kendali tindakan** yang sama di dua layar atau lebih: *"Batalkan tanda"*
 *    pada rincian sholat dan Wudhu, *"Kosongkan"* pada tombol kosongkan Pencarian,
 *    *"Lanjutkan"* pada beranda dan daftar favorit, serta awalan *"Terakhir dibaca: …"* pada ketiga
 *    layar itu beserta Yasin dan Tahlil. Tiap pasangan menamai satu tindakan yang sama; kata yang
 *    berbeda di salah satunya membuat pengguna mengira ada dua tombol berbeda untuk hal yang sama.
 *    (Dua nama langkah — *"← Sebelumnya"* / *"Berikutnya →"* — dahulu juga hidup di sini; keduanya
 *    dibubarkan bersama tombolnya ketika panduan sholat dan Tahlil menjadi bersambung.)
 *  - Empat **kata tindakan baku** — *"Batal"*, *"Hapus"*, *"Reset"*, *"Buka"* — juga muncul di
 *    beberapa layar. Karena singkat dan umum, tiap tempat cenderung mengejanya sendiri; padahal
 *    *"Batal"* di dialog mana pun berarti menutup tanpa melakukan apa pun, dan *"Hapus"* di mana pun
 *    berarti menghapus yang tersimpan.
 *
 * Uji ini menjaga agar semuanya tetap satu tempat, dan agar varian yang disengaja tetap
 * terlihat sebagai varian — bukan menyamar sebagai salinan.
 */
class NamaAntarmukaSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun berkasUji(): List<File> =
        File(root(), "src/test/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Kode tanpa komentar: KDoc yang menjelaskan pola lama memuat tulisan yang sama persis. */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun layar(relatif: String): String =
        kode(File(root(), "src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private fun uiKit(): File =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/UiKit.kt")

    /** Nama-nama yang harus tinggal di `NamaAntarmuka`, beserta nilai yang diharapkan. */
    private fun daftarNama(): Map<String, String> = mapOf(
        "PENGATURAN_BACAAN" to NamaAntarmuka.PENGATURAN_BACAAN,
        "LAYAR_PENUH" to NamaAntarmuka.LAYAR_PENUH,
        "LAYAR_PENUH_IMAM" to NamaAntarmuka.LAYAR_PENUH_IMAM,
        "COBA_LAGI" to NamaAntarmuka.COBA_LAGI,
        "TAMPIL_ARAB" to NamaAntarmuku.TAMPIL_ARAB,
        "TAMPIL_LATIN" to NamaAntarmuka.TAMPIL_LATIN,
        "TAMPIL_TERJEMAHAN" to NamaAntarmuka.TAMPIL_TERJEMAHAN,
        "UKURAN_ARAB" to NamaAntarmuka.UKURAN_ARAB,
        "UKURAN_LATIN" to NamaAntarmuka.UKURAN_LATIN,
        "JARAK_ANTARBARIS" to NamaAntarmuka.JARAK_ANTARBARIS,
        "BATALKAN_TANDA" to NamaAntarmuka.BATALKAN_TANDA,
        "KOSONGKAN" to NamaAntarmuka.KOSONGKAN,
        "LANJUTKAN" to NamaAntarmuka.LANJUTKAN,
        "TERAKHIR_DIBACA" to NamaAntarmuka.TERAKHIR_DIBACA,
        "BATAL" to NamaAntarmuka.BATAL,
        "HAPUS" to NamaAntarmuka.HAPUS,
        "RESET" to NamaAntarmuka.RESET,
        "BUKA" to NamaAntarmuka.BUKA,
    )

    // ---- Ejaan label ---------------------------------------------------------------------------

    /**
     * Nama-nama itu **teks yang dibaca pengguna**. Mengubahnya adalah keputusan produk,
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
        assertEquals("Batalkan tanda", NamaAntarmuka.BATALKAN_TANDA)
        assertEquals("Kosongkan", NamaAntarmuka.KOSONGKAN)
        assertEquals("Lanjutkan", NamaAntarmuka.LANJUTKAN)
        assertEquals("Terakhir dibaca", NamaAntarmuka.TERAKHIR_DIBACA)
        assertEquals("Batal", NamaAntarmuka.BATAL)
        assertEquals("Hapus", NamaAntarmuka.HAPUS)
        assertEquals("Reset", NamaAntarmuka.RESET)
        assertEquals("Buka", NamaAntarmuka.BUKA)
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
        assertEquals("nama kendali berbeda tidak boleh ada yang sama",
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
     * Kata tindakan baku juga tidak boleh dieja ulang di dalam **uji**.
     *
     * Penjaga yang mencari `"Batal"` atau `"Hapus"` sebagai literal di layar akan tetap hijau
     * walaupun layarnya mengeja ulang kata itu sendiri — ia kehilangan tepat hal yang seharusnya
     * dijaganya. Itulah yang terjadi pada penjaga hapus di `JenisSuaraAdzanTest`
     * (`s.contains("Hapus")`) dan pada penjaga dialog di `ManajemenDataOfflineTest`
     * (`layar.contains("\"Batal\"")`): keduanya hanya menemukan kata itu, bukan pemakaian nama
     * bersama. Uji yang benar menunjuk `NamaAntarmuka.*`, sehingga ejaannya tetap satu tempat.
     *
     * `NamaAntarmukaSatuTempatTest.kt` sendiri dikecualikan — di sanalah ejaan keempat kata itu
     * memang dipaku, dan memakunya menuntut kata itu hadir sebagai literal.
     */
    @Test
    fun kataTindakanTidakDiejaUlangDiUji() {
        val kata = listOf("BATAL", "HAPUS", "RESET", "BUKA").map { it to daftarNama().getValue(it) }
        val pelanggar = mutableListOf<String>()
        berkasUji()
            .filter { it.name != "NamaAntarmukaSatuTempatTest.kt" }
            .forEach { f ->
                KodeSumber.literalString(f.readText()).forEach { teks ->
                    kata.filter { memakaiLabel(teks, it.second) }
                        .forEach { pelanggar += "${f.name}: \"${it.second}\"" }
                }
            }
        assertTrue("kata tindakan dieja ulang di uji — uji itu tak lagi membuktikan satu tempat: " +
                "$pelanggar", pelanggar.isEmpty())
    }

    /**
     * Perbaikan preferensi bacaan harus terlihat di satu-satunya tempatnya: layar Pengaturan
     * memakai keenam nama bersama itu. Dahulu ada dua tempat (sheet per layar + Pengaturan) yang
     * masing-masing mengeja sendiri — dua ejaan untuk satu preferensi membuat pengguna mengira
     * ada dua pengaturan.
     */    @Test
    fun layarPengaturanMemakaiNamaBersamaUntukPreferensiBacaan() {
        val pengaturan = layar("feature/settings/SettingsScreen.kt")
        listOf(
            "TAMPIL_ARAB", "TAMPIL_LATIN", "TAMPIL_TERJEMAHAN",
            "UKURAN_ARAB", "UKURAN_LATIN", "JARAK_ANTARBARIS",
        ).forEach { kunci ->
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

    // ---- Judul bagian pengaturan bacaan ---------------------------------------------------------

    /**
     * Judul bagian pengaturan bacaan di layar Pengaturan memakai nama bersama yang sama dengan
     * yang dijanjikan seluruh layar bacaan. Karena itu `NamaAntarmuka` hanya boleh memuat satu
     * nama untuknya: dua konstanta berbeda akan dapat menyimpang.
     */
    @Test
    fun judulBagianPengaturanBacaanSatuNama() {
        val berkas = KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/NamaAntarmuka.kt",
        )
        val deklarasi = Regex("=\\s*\"${Regex.escape(NamaAntarmuka.PENGATURAN_BACAAN)}\"")
            .findAll(KodeSumber.kodeTanpaKomentar(berkas)).count()
        assertEquals("nama pengaturan bacaan dideklarasikan lebih dari sekali", 1, deklarasi)

        assertTrue("judul bagian pengaturan bacaan di Pengaturan bukan dari kosakata bersama",
            layar("feature/settings/SettingsScreen.kt")
                .contains("BagianJudul(NamaAntarmuka.PENGATURAN_BACAAN)"))
    }

    /**
     * Pengaturan bacaan tidak lagi punya pintu per layar: tidak ada berkas lain yang membuka sheet
     * atau menamai kendalinya sendiri. Penjaga ini memastikan sisa-sisa pintu itu tidak kembali.
     */
    @Test
    fun pengaturanBacaanTidakPunyaPintuKedua() {
        val pelanggar = berkasMain()
            .filter { it.name != "SettingsScreen.kt" }
            .filter { kode(it).contains("NamaAntarmuka.PENGATURAN_BACAAN") }
            .map { it.name }
        assertTrue("layar selain Pengaturan masih menyebut pengaturan bacaan: $pelanggar",
            pelanggar.isEmpty())
    }

    // ---- Chip "Layar penuh" --------------------------------------------------------------------

    /**
     * Tiap layar yang punya kendali layar penuh memakai salah satu dari dua nama bersama — dan yang
     * benar untuk layar itu. Nama yang tertukar di sini bukan sekadar beda kata: pada Sholat Ghaib
     * kendali itu sekaligus menyalakan panduan mode imam, sehingga nama "Layar penuh" biasa di sana
     * menjanjikan hal yang tidak terjadi. Pada layar Yasin, Tahlil, dan kelompok dzikir kendali itu
     * berupa tombol **berikon** — namanya tetap dibaca pembaca layar lewat `contentDescription`.
     */
    @Test
    fun tiapLayarKendaliLayarPenuhMemakaiNamaYangBenar() {
        val berchip = listOf(
            "feature/adzan/IqomahDoaScreen.kt",
            "feature/kiblat/KiblatScreen.kt",
            "feature/sholat/JenazahScreen.kt",
        )
        berchip.forEach { berkas ->
            assertTrue("$berkas tidak memakai nama chip layar penuh bersama",
                layar(berkas).contains("Text(NamaAntarmuka.LAYAR_PENUH)"))
        }
        listOf(
            "feature/yasin/YasinScreen.kt",
            "feature/tahlil/TahlilScreen.kt",
            "feature/dzikir/DzikirGroupScreen.kt",
        ).forEach { berkas ->
            assertTrue("$berkas tidak memakai nama layar penuh bersama pada tombol berikonnya",
                layar(berkas).contains("NamaAntarmuka.LAYAR_PENUH"))
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
            "BATALKAN_TANDA" to listOf(
                "feature/sholat/SholatDetailScreen.kt", "feature/sholat/WudhuScreen.kt",
            ),
            "KOSONGKAN" to listOf(
                "feature/search/SearchScreen.kt",
            ),
            "LANJUTKAN" to listOf(
                "feature/home/HomeScreen.kt", "feature/favorit/FavoritScreen.kt",
            ),
            "TERAKHIR_DIBACA" to listOf(
                "feature/home/HomeScreen.kt", "feature/favorit/FavoritScreen.kt",
                "feature/yasin/YasinScreen.kt", "feature/tahlil/TahlilScreen.kt",
            ),
        )
        pemakaian.forEach { (kunci, berkas) ->
            berkas.forEach { b ->
                assertTrue("$b tidak memakai NamaAntarmuka.$kunci untuk kendali yang sama",
                    layar(b).contains("NamaAntarmuka.$kunci"))
            }
        }
    }

    /**
     * Empat **kata tindakan baku** yang sama di beberapa layar memakai satu nama — dan pemakaiannya
     * harus tetap ada di tiap sisi.
     *
     * Sama seperti [kendaliLangkahDanTindakanMemakaiNamaBersama]: [labelTidakDisalinKeLuarNamaAntarmuka]
     * menangkap salinannya, uji ini menjaga supaya perbaikannya tidak dipuaskan dengan menghapus
     * salah satu tombol.
     */
    @Test
    fun kataTindakanBakuMemakaiNamaBersama() {
        val pemakaian = mapOf(
            "BATAL" to listOf(
                "feature/adzan/AdzanScreen.kt", "feature/adzan/IqomahDoaScreen.kt",
                "feature/settings/ContentUpdateScreen.kt", "feature/settings/SettingsScreen.kt",
            ),
            "HAPUS" to listOf(
                "feature/adzan/AdzanScreen.kt", "feature/favorit/FavoritScreen.kt",
                "feature/settings/SettingsScreen.kt",
            ),
            "RESET" to listOf("feature/adzan/IqomahDoaScreen.kt"),
            "BUKA" to listOf(
                "feature/favorit/FavoritScreen.kt", "feature/search/SearchScreen.kt",
            ),
        )
        pemakaian.forEach { (kunci, berkas) ->
            berkas.forEach { b ->
                assertTrue("$b tidak memakai NamaAntarmuka.$kunci untuk tindakan yang sama",
                    layar(b).contains("NamaAntarmuka.$kunci"))
            }
        }
        // `UiKit` adalah komponen bersama: kedua tombolnya pun menyebut nama bersama, bukan hanya
        // layar-layar yang memakainya.
        listOf("RESET", "BUKA").forEach { kunci ->
            assertTrue("UiKit tidak memakai NamaAntarmuka.$kunci untuk kendali bersamanya",
                kode(uiKit()).contains("NamaAntarmuka.$kunci"))
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
