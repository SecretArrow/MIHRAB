package id.or.kemenag.yasintahlil.core.sholat

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Kosakata id bacaan sholat ([BacaanSholat]) harus tetap sepadan dengan bundel konten **dan** dengan
 * layar yang menampilkannya.
 *
 * Id bacaan dahulu dieja sendiri-sendiri di empat tempat — peta posisi tubuh, daftar langkah mode
 * ringkas, dan daftar langkah takbir layar Sholat Ghaib serta Sholat Jenazah. Keempatnya tidak
 * pernah dibandingkan, sehingga satu salah eja tidak berbunyi apa pun: `cari(id)` mengembalikan
 * `null` dan kartu bacaan langkah itu hilang sementara judul bernomor di atasnya tetap tampil,
 * atau satu langkah pokok diam-diam terlewat dari mode ringkas. Uji ini menjaga ketiga sisi tetap
 * memakai satu ejaan: kosakata, bundel, dan layar.
 *
 * Dua kata pada kosakata ini — `niat` dan `takbir` — sekaligus dipakai untuk hal lain: keduanya
 * kunci pada skema konten (`ContentRepo`) dan `niat` juga jenis id tersimpan
 * ([id.or.kemenag.yasintahlil.core.util.JenisBacaan.NIAT]). Karena itu penjaga "tidak dieja ulang"
 * mengecualikan keduanya, dan uji terpisah memaku berkas mana saja yang boleh menuliskannya —
 * supaya pengecualian itu tidak diam-diam melebar.
 */
class BacaanSholatTest {

    private fun berkas(relatif: String): String = KodeSumber.sumber(relatif)

    private fun kode(relatif: String): String = KodeSumber.kodeTanpaKomentar(berkas(relatif))

    private fun berkasMain(): List<File> =
        File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Id bacaan pada larik `bacaan` sebuah bundel, urut seperti tertulis. */
    private fun idBundel(nama: String): List<String> =
        Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(berkas("src/main/assets/content/$nama").substringAfter("\"bacaan\""))
            .map { it.groupValues[1] }.toList()

    private val idSemua: List<String> = BacaanSholat.SEMUA + BacaanSholat.MAYIT + BacaanSholat.MAYIT_WANITA

    /**
     * Kata pada kosakata yang **juga** punya arti lain di luar bacaan sholat: `niat` adalah
     * sub-objek pada `sholat_daftar.json` sekaligus jenis id tersimpan, dan `takbir` adalah jumlah
     * takbir pada `sholat_ghaib.json`/`sholat_jenazah.json`. Kebetulan bunyinya sama; keduanya bukan
     * id bacaan di tempat itu.
     */
    private val kunciSkema: Set<String> = setOf(BacaanSholat.NIAT, BacaanSholat.TAKBIR)

    /** Nama konstanta pada [BacaanSholat], untuk menerjemahkan `BacaanSholat.X` di berkas layar. */
    private val konstanta: Map<String, String> =
        Regex("const val (\\w+) = \"([^\"]+)\"")
            .findAll(berkas("src/main/java/id/or/kemenag/yasintahlil/core/sholat/BacaanSholat.kt"))
            .associate { it.groupValues[1] to it.groupValues[2] }

    // ---- Ejaan yang dipaku -------------------------------------------------------------------

    @Test
    fun ejaanIdBacaanTidakBerubah() {
        assertEquals(
            "id bacaan sholat berubah — bundel konten memakai ejaan yang lain",
            listOf(
                "niat", "takbir", "iftitah", "fatihah", "surat_setelah_fatihah", "rukuk",
                "itidal", "qunut", "sujud", "duduk", "tasyahud_awal", "tasyahud_akhir",
                "sholawat_ibrahimiyah", "salam",
            ),
            BacaanSholat.SEMUA,
        )
        assertEquals(
            "id bacaan sholat mayit berubah",
            listOf("doa_mayit", "doa_mayit_tambahan"),
            BacaanSholat.MAYIT,
        )
        // Daftar ringkas dan urutan takbir adalah keputusan produk yang dibaca layar, bukan sekadar
        // penyaring: menambah atau membuang satu id di sini langsung mengubah panduan yang dibaca
        // pengguna, jadi ejaannya dipaku.
        assertEquals(
            "langkah mode ringkas berubah",
            listOf(
                "niat", "takbir", "fatihah", "surat_setelah_fatihah", "rukuk", "sujud",
                "tasyahud_akhir", "salam",
            ),
            BacaanSholat.RINGKAS,
        )
        assertEquals(
            "urutan bacaan tiap takbir sholat mayit berubah",
            listOf("fatihah", "sholawat_ibrahimiyah", "doa_mayit", "doa_mayit_tambahan", "salam"),
            BacaanSholat.TAKBIR_MAYIT,
        )
    }

    // ---- Kosakata vs bundel konten -----------------------------------------------------------

    @Test
    fun idBacaanSejajarDenganBundelKonten() {
        assertEquals("id pada sholat_bacaan.json berbeda dari kosakata bersama",
            BacaanSholat.SEMUA, idBundel("sholat_bacaan.json"))
        // sholat_mayit.json kini memuat varian dhamir wanita juga (urutannya sama dengan MAYIT):
        // pilihan "Wanita" pada layar Jenazah/Ghaib memakai entri ini, bukan menyalin lafaz.
        assertEquals("id pada sholat_mayit.json berbeda dari kosakata bersama",
            BacaanSholat.MAYIT + BacaanSholat.MAYIT_WANITA, idBundel("sholat_mayit.json"))
    }

    @Test
    fun ringkasTerletakDiDalamSemuaDanTetapUrut() {
        val posisi = BacaanSholat.RINGKAS.map { BacaanSholat.SEMUA.indexOf(it) }
        assertTrue("daftar ringkas memuat id di luar SEMUA: " +
            BacaanSholat.RINGKAS.filterIndexed { i, _ -> posisi[i] < 0 },
            posisi.none { it < 0 })
        // Langkah dibaca berurutan lewat tombol "Berikutnya", jadi ringkasan yang urutannya
        // melompat-lompat menuntun pembaca mengerjakan gerakan di luar urutannya.
        assertEquals("urutan daftar ringkas tidak mengikuti urutan SEMUA", posisi.sorted(), posisi)
    }

    @Test
    fun takbirMayitSepenuhnyaAdaDiBundel() {
        val kembar = BacaanSholat.TAKBIR_MAYIT.size - BacaanSholat.TAKBIR_MAYIT.toSet().size
        assertEquals("ada id takbir mayit yang muncul dua kali", 0, kembar)
        val luar = BacaanSholat.TAKBIR_MAYIT.filter { it !in idSemua }
        assertTrue("id takbir mayit tidak ada di bundel: $luar", luar.isEmpty())
        // Salam menutup sholat; takbir kelima berarti ada bacaan yang tertukar tempat.
        assertEquals("urutan takbir mayit tidak diakhiri salam",
            BacaanSholat.SALAM, BacaanSholat.TAKBIR_MAYIT.last())
        assertEquals("sholat mayit bukan empat takbir", 4,
            BacaanSholat.TAKBIR_MAYIT.indexOf(BacaanSholat.SALAM))
    }

    // ---- Kosakata vs layar -------------------------------------------------------------------

    /**
     * Layar tidak boleh mengeja id bacaan sendiri. Penjaganya mencari **literal berkutip** pada
     * seluruh kode `src/main`, bukan hanya berkas layar sholat: id yang dieja ulang di tempat lain
     * pun akan menyimpang dengan cara yang sama.
     */
    @Test
    fun idBacaanTidakDiejaUlangDiLuarKosakata() {
        val pelanggar = mutableListOf<String>()
        berkasMain().filter { it.name != "BacaanSholat.kt" }.forEach { f ->
            val k = KodeSumber.kodeTanpaKomentar(f.readText())
            idSemua.filter { it !in kunciSkema && k.contains("\"$it\"") }
                .forEach { pelanggar += "${f.name}: \"$it\"" }
        }
        assertTrue("id bacaan dieja ulang di luar `BacaanSholat`: $pelanggar", pelanggar.isEmpty())
    }

    /**
     * Pengecualian [kunciSkema] hanya sah selama dua kata itu benar-benar dipakai sebagai kunci
     * skema konten atau jenis id tersimpan. Uji ini memaku berkas mana yang boleh menuliskannya,
     * sehingga pengecualiannya tidak melebar menjadi "id bacaan boleh dieja di mana saja".
     */
    @Test
    fun pengecualianKunciSkemaTetapTerbatas() {
        val pemakai = berkasMain()
            .filter { it.name != "BacaanSholat.kt" }
            .filter { f ->
                val k = KodeSumber.kodeTanpaKomentar(f.readText())
                kunciSkema.any { id -> k.contains("\"$id\"") }
            }
            .map { it.name }.sorted()
        assertEquals("kata yang sekaligus kunci skema konten dipakai di berkas lain",
            listOf("ContentRepo.kt", "JenisBacaan.kt"), pemakai)
    }

    /**
     * Urutan takbir kedua layar sholat mayit harus datang dari [BacaanSholat.TAKBIR_MAYIT], bukan
     * dari daftar yang diketik di masing-masing layar: itulah yang membuat satu salah eja hanya
     * menghilangkan takbir di satu layar, tanpa galat.
     *
     * Layar kini hanya mengetik judul yang **khas** baginya — takbir 2–4 yang sama di keduanya
     * tinggal di `TampilanSholatKhusus.judulTakbirBersama`; penjaga cakupan penuhnya ada di
     * `TampilanSholatKhususSatuTempatTest`. Yang dipaku di sini: tiap id yang masih diketik layar
     * harus id pada urutan bersama, dan tak satu pun diketik dua kali.
     */
    @Test
    fun layarSholatMayitMenyusunLangkahDariUrutanBersama() {
        listOf(
            "src/main/java/id/or/kemenag/yasintahlil/feature/sholat/GhaibScreen.kt",
            "src/main/java/id/or/kemenag/yasintahlil/feature/sholat/JenazahScreen.kt",
        ).forEach { rel ->
            val k = kode(rel)
            assertTrue("$rel tidak memakai urutan takbir bersama",
                k.contains("BacaanSholat.TAKBIR_MAYIT"))
            assertTrue("$rel memanggil cari() dengan id yang diketik sendiri", !k.contains("cari(\""))
            // Judulnya memang khas tiap layar (sholat ghaib membaca ta'awudz, salamnya menoleh),
            // jadi yang dipaku di sini adalah **urutan id** yang dijuduli, bukan teks judulnya.
            val id = Regex("BacaanSholat\\.(\\w+)\\s+to\\s+\"")
                .findAll(k).map { it.groupValues[1] }.toList()
                .map { konstanta[it] ?: it }
            assertTrue("$rel tidak lagi menjuduli satu langkah pun", id.isNotEmpty())
            assertEquals("$rel menjuduli langkah takbir di luar urutan bersama",
                emptyList<String>(), id - BacaanSholat.TAKBIR_MAYIT)
            assertEquals("$rel menjuduli satu id lebih dari sekali", id.distinct(), id)
        }
    }

    /** Peta posisi tubuh memakai id yang sama dengan bundel — bukan ejaannya sendiri. */
    @Test
    fun petaPosisiMemakaiKosakataBersama() {
        val k = kode("src/main/java/id/or/kemenag/yasintahlil/feature/sholat/Posture.kt")
        val id = Regex("BacaanSholat\\.(\\w+) to ")
            .findAll(k).map { it.groupValues[1] }.toList()
            .map { konstanta[it] ?: it }
        assertEquals("setiap bacaan harus punya satu posisi",
            BacaanSholat.SEMUA.size, id.size)
        assertEquals("peta posisi tidak sejajar dengan kosakata bersama",
            BacaanSholat.SEMUA.toSet(), id.toSet())
    }

    /** Mode ringkas di layar panduan sholat membaca daftar bersama, bukan salinannya. */
    @Test
    fun layarPanduanSholatMemakaiDaftarRingkasBersama() {
        val k = kode("src/main/java/id/or/kemenag/yasintahlil/feature/sholat/SholatDetailScreen.kt")
        assertTrue("layar panduan sholat tidak memakai daftar ringkas bersama",
            k.contains("BacaanSholat.RINGKAS"))
    }

    /** Kosakata bersama harus tetap murni: tanpa Android, agar dapat diuji langsung di JVM. */
    @Test
    fun kosakataBacaanTidakMenyentuhAndroid() {
        val teks = berkas("src/main/java/id/or/kemenag/yasintahlil/core/sholat/BacaanSholat.kt")
        assertTrue("BacaanSholat mengimpor Android sehingga tak dapat diuji di JVM",
            !teks.contains("import android."))
    }
}
