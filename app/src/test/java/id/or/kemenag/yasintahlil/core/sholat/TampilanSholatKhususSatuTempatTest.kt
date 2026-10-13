package id.or.kemenag.yasintahlil.core.sholat

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dua layar sholat mayit — Ghaib dan Jenazah — menampilkan **medan yang sama** dari berkas
 * kontennya, jadi perangkaian teksnya harus satu tempat.
 *
 * Sebelum `TampilanSholatKhusus` ada, tiap layar merangkainya sendiri dan salinannya sudah
 * menyimpang tanpa ketahuan: syarat yang sama dipisah `"; "` di Ghaib tetapi `", "` lalu diberi
 * titik di Jenazah; dalil yang dimuat `sholat_jenazah.json` tak pernah tampil; judul layar
 * menyebut *"tanpa ruku/sujud"* di Ghaib saja; dan niat ditampilkan tanpa terjemahan di Ghaib
 * tetapi dengan terjemahan di Jenazah. Tak satu pun perbedaan itu tercatat sebagai pilihan.
 *
 * Uji ini menjaga tiga hal yang berlawanan arah: (1) aturan yang **sama** tidak boleh dieja ulang di
 * layar mana pun, (2) perbedaan yang **disengaja** — judul takbir 1 dan salam, serta label chip
 * layar penuh — harus tetap berbeda supaya tidak "dirapikan" menjadi salinan, dan (3) judul takbir
 * 2–4 yang memang sama di kedua layar harus tinggal di **satu** tempat, bukan di dua salinan yang
 * bisa menyimpang tanpa ketahuan.
 *
 * Satu aturan pernah tinggal di sini lalu **pindah**: cadangan teks bacaan (`teksBacaan`) ternyata
 * bukan aturan sholat mayit — layar Panduan Sholat, rincian langkah sholat, dan Tahlil memakainya
 * juga, masing-masing dengan salinan sendiri. Rumahnya kini `core/data/TeksBacaan.kt`, dan
 * penjaganya `TeksBacaanSatuTempatTest`. Di sini yang tinggal hanya aturan yang benar-benar
 * milik kedua layar mayit.
 */
class TampilanSholatKhususSatuTempatTest {

    private fun root() = KodeSumber.root()

    private fun layar(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private val ghaib get() = layar("feature/sholat/GhaibScreen.kt")
    private val jenazah get() = layar("feature/sholat/JenazahScreen.kt")
    private val inti get() = layar("core/sholat/TampilanSholatKhusus.kt")

    /** Pasangan `BacaanSholat.<ID> to "<judul>"` yang diketik sebuah berkas, sebagai peta id→judul. */
    private fun petaJudul(teks: String): Map<String, String> =
        Regex("""BacaanSholat\.(\w+) to "([^"]*)"""")
            .findAll(teks).associate { it.groupValues[1] to it.groupValues[2] }

    // ---- Aturan bersama -------------------------------------------------------------------------

    /** Kedua layar harus memanggil tiap aturan bersama — bukan hanya sebagian. */
    @Test
    fun keduaLayarMemakaiAturanBersama() {
        val aturan = listOf("judul(", "langkah(", "syarat(", "dalil(", "catatanMazhab(")
        mapOf("Ghaib" to ghaib, "Jenazah" to jenazah).forEach { (nama, teks) ->
            aturan.forEach { a ->
                assertTrue("$nama tidak memakai TampilanSholatKhusus.$a untuk teks yang sama",
                    teks.contains("TampilanSholatKhusus.$a"))
            }
            // Cadangan teks bacaan tidak lagi milik objek ini: aturannya dipakai juga oleh layar
            // sholat biasa dan Tahlil, jadi rumahnya `TeksBacaan` — lihat `TeksBacaanSatuTempatTest`.
            assertTrue("$nama tidak memakai aturan cadangan teks bersama (TeksBacaan.pilih)",
                teks.contains("TeksBacaan.pilih("))
        }
    }

    /**
     * Aturan bersama itu tidak boleh dieja ulang sebagai literal di layarnya. Yang dicari adalah
     * awalan labelnya: salinan yang menyimpang tetap memuat awalan yang sama, dan itulah yang
     * membuat dua layar menyebut hal yang sama dengan cara berbeda.
     */
    @Test
    fun aturanBersamaTidakDiejaUlangDiLayar() {
        val terlarang = listOf("Syarat: ", "Dalil: ", "Catatan mazhab: ", "\${i + 1}. ")
        mapOf("Ghaib" to ghaib, "Jenazah" to jenazah).forEach { (nama, teks) ->
            terlarang.forEach { t ->
                assertTrue("$nama mengeja ulang \"$t\" alih-alih memakai TampilanSholatKhusus",
                    !teks.contains(t))
            }
        }
    }

    /** Perangkaian syarat dipaku: satu pemisah dan satu penutup untuk kedua layar. */
    @Test
    fun perangkaianSyaratSatuAturan() {
        assertEquals("Syarat: a; b; c.",
            TampilanSholatKhusus.syarat(listOf("a", "b", "c")))
        assertEquals("Syarat: satu.", TampilanSholatKhusus.syarat(listOf("satu")))
    }

    /**
     * Judul, langkah, dalil, dan catatan mazhab dipaku ejaannya.
     *
     * Cadangan teks bacaan dahulu ikut diuji di sini; aturannya kini tinggal di
     * `core/data/TeksBacaan.kt` karena dipakai juga oleh layar sholat biasa dan Tahlil, dan
     * pengujiannya pindah ke `TeksBacaanSatuTempatTest`.
     */
    @Test
    fun ejaanAturanBersamaTidakBerubah() {
        assertEquals("Sholat Ghaib — 4 takbir, tanpa ruku/sujud",
            TampilanSholatKhusus.judul("Sholat Ghaib", 4))
        assertEquals("3. Takbir 3 → Doa untuk mayit",
            TampilanSholatKhusus.langkah(3, "Takbir 3 → Doa untuk mayit"))
        assertEquals("Dalil: HR contoh.", TampilanSholatKhusus.dalil("HR contoh."))
        assertEquals("Catatan mazhab: Syafi'i.", TampilanSholatKhusus.catatanMazhab("Syafi'i."))
    }

    // ---- Perbedaan yang disengaja ---------------------------------------------------------------

    /**
     * Judul langkah takbir memang khas tiap layar — Ghaib membaca ta'awudz sebelum Al-Fatihah dan
     * salamnya menoleh dua sisi, Jenazah tidak. Uji ini memakunya: bila kelak "dirapikan" menjadi
     * satu peta bersama, perbedaan yang disengaja itu hilang tanpa ketahuan.
     */
    @Test
    fun judulLangkahTetapKhasTiapLayar() {
        val g = petaJudul(ghaib)
        val j = petaJudul(jenazah)
        assertEquals("kedua layar tidak lagi menjuduli urutan id yang sama",
            g.keys, j.keys)
        val berbeda = g.filter { (k, v) -> j[k] != v }.keys
        assertEquals("perbedaan judul langkah di luar yang disengaja (ta'awudz & arah salam)",
            setOf("FATIHAH", "SALAM"), berbeda)
    }

    /**
     * Judul takbir 2–4 sama di kedua layar, jadi ejaannya tinggal **satu tempat**: di
     * [TampilanSholatKhusus.judulTakbirBersama].
     *
     * Sebelumnya ketiganya diketik utuh di GhaibScreen **dan** JenazahScreen. Penjaga lama hanya
     * membandingkan kedua peta, sehingga satu perbaikan ejaan yang dikenakan pada kedua salinan
     * lolos tanpa ketahuan — dan bila hanya satu yang dikenai, dua layar menyebut langkah yang
     * sama dengan dua nama. Yang dijaga di sini adalah salinannya sendiri tidak ada lagi.
     */
    @Test
    fun judulTakbirBersamaTinggalDiSatuTempat() {
        // Ejaannya dipaku di sini: kedua layar menyebut judul yang sama, jadi satu perubahan pada
        // peta bersama mengubah dua layar sekaligus — bukan alasan untuk lolos diam-diam.
        assertEquals("judul takbir bersama berubah ejaannya",
            mapOf(
                BacaanSholat.SHOLAWAT_IBRAHIMIYAH to "Takbir 2 → Sholawat Ibrahimiyah",
                BacaanSholat.DOA_MAYIT to "Takbir 3 → Doa untuk mayit",
                BacaanSholat.DOA_MAYIT_TAMBAHAN to "Takbir 4 → Doa tambahan",
            ),
            TampilanSholatKhusus.judulTakbirBersama)
        // Dan salinannya tidak boleh hidup lagi di layarnya.
        val bersama = TampilanSholatKhusus.judulTakbirBersama.values.toList()
        assertEquals("entri bersama bukan lagi tiga judul takbir tengah", 3, bersama.size)
        bersama.forEach { t ->
            mapOf("Ghaib" to ghaib, "Jenazah" to jenazah).forEach { (nama, teks) ->
                assertFalse("$nama mengeja ulang judul bersama \"$t\" alih-alih mengambilnya dari " +
                        "TampilanSholatKhusus.judulTakbirBersama", teks.contains("\"$t\""))
            }
        }
    }

    /**
     * Gabungan entri bersama dan judul khas tiap layar harus **menutupi seluruh** urutan takbir:
     * layar memanggil `judulLangkah.getValue(id)` untuk setiap id pada [BacaanSholat.TAKBIR_MAYIT],
     * jadi satu id yang tak punya judul menjatuhkan layarnya — bukan sekadar menghilangkan kartu.
     *
     * Cakupan itu dulu terjaga karena tiap layar mengetik kelima judulnya; sekarang ia bergantung
     * pada dua berkas, jadi harus dipaku melintasi keduanya.
     */
    @Test
    fun cakupanJudulLangkahMenutupiSeluruhUrutanTakbir() {
        val bersama = petaJudul(inti).keys
        assertEquals("entri bersama bukan lagi tiga takbir tengah",
            setOf("SHOLAWAT_IBRAHIMIYAH", "DOA_MAYIT", "DOA_MAYIT_TAMBAHAN"), bersama)
        val urutan = setOf("FATIHAH", "SHOLAWAT_IBRAHIMIYAH", "DOA_MAYIT", "DOA_MAYIT_TAMBAHAN", "SALAM")
        mapOf("Ghaib" to ghaib, "Jenazah" to jenazah).forEach { (nama, teks) ->
            val khas = petaJudul(teks).keys
            // Layar harus benar-benar **menggabungkan** lewat aturan bersama, bukan menyusun
            // petanya sendiri yang kebetulan menutupi urutan yang sama.
            assertTrue("$nama tidak menggabungkan judul bersama lewat " +
                    "TampilanSholatKhusus.judulLangkah()", teks.contains("TampilanSholatKhusus.judulLangkah("))
            assertEquals("$nama menjuduli id di luar urutan takbir bersama",
                emptySet<String>(), khas - urutan)
            assertEquals("$nama menimpa entri bersama dengan judul khasnya sendiri",
                emptySet<String>(), khas intersect bersama)
            assertEquals("gabungan judul $nama tidak menutupi seluruh urutan takbir — " +
                    "getValue(id) bisa gagal dan menjatuhkan layarnya", urutan, bersama + khas)
        }
    }

    /** Penggabungannya satu aturan, dan judul khas yang menimpa entri bersama ditolak. */
    @Test
    fun judulLangkahMenggabungkanEntriBersamaDanKhas() {
        val peta = TampilanSholatKhusus.judulLangkah(
            mapOf(
                BacaanSholat.FATIHAH to "Takbir 1 → Al-Fatihah",
                BacaanSholat.SALAM to "Salam",
            ),
        )
        assertEquals("gabungan tidak menutupi seluruh urutan takbir",
            BacaanSholat.TAKBIR_MAYIT.toSet(), peta.keys)
        assertEquals("Takbir 3 → Doa untuk mayit", peta[BacaanSholat.DOA_MAYIT])
        assertEquals("Salam", peta[BacaanSholat.SALAM])
        // Perbedaan yang disengaja harus disengaja: salinan yang tertinggal tidak boleh menang
        // diam-diam atas entri bersama.
        assertThrows(IllegalArgumentException::class.java) {
            TampilanSholatKhusus.judulLangkah(mapOf(BacaanSholat.DOA_MAYIT to "salinan lama"))
        }
    }

    /** Label chip layar penuh juga sengaja berbeda: pada Ghaib, layar penuh sekaligus mode imam. */
    @Test
    fun labelLayarPenuhSengajaBerbeda() {
        assertTrue("Ghaib tidak lagi memakai LAYAR_PENUH_IMAM",
            ghaib.contains("NamaAntarmuka.LAYAR_PENUH_IMAM"))
        assertTrue("Jenazah tidak lagi memakai LAYAR_PENUH",
            jenazah.contains("NamaAntarmuka.LAYAR_PENUH)"))
    }

    // ---- Konten yang tak boleh terbuang ----------------------------------------------------------

    /**
     * Dalil yang dimuat `sholat_jenazah.json` dulu tak pernah tampil. Uji ini menuntut medannya
     * **ada isinya** dan **ditampilkan** — tanpa prasyarat isinya, penjaganya bisa dipuaskan dengan
     * mengosongkan medannya.
     */
    @Test
    fun dalilJenazahDitampilkan() {
        val isi = KodeSumber.sumber("src/main/assets/content/sholat_jenazah.json")
        assertTrue("sholat_jenazah.json tidak lagi memuat dalil — penjaga tampilannya jadi hampa",
            Regex(""""dalil"\s*:\s*"[^"]+"""").containsMatchIn(isi))
        assertTrue("layar Jenazah tidak menampilkan dalil yang dimuat berkas kontennya",
            jenazah.contains("TampilanSholatKhusus.dalil("))
    }

    /**
     * Terjemahan niat ditampilkan di **kedua** layar. Sebelumnya Ghaib memanggil `BacaanCard`
     * dengan `showTrans = false` padahal berkasnya memuat terjemahan — konten yang ada tetapi tak
     * terbaca, dan tak ada alasan yang tercatat.
     *
     * Yang dijaga bukan lagi literal `true`, melainkan bahwa kartu niat menerima tampil-tidaknya
     * terjemahan dari **preferensi pengguna** (`showTrans`), sebab literal `false` di sinilah yang
     * dulu membuang terjemahannya. Bawaannya `true`, jadi bagi pengguna yang belum menyentuh
     * pengaturannya hasilnya sama seperti sebelumnya — tetapi kini ia dapat mematikannya, dan
     * mematikannya berlaku di kedua layar.
     */
    @Test
    fun terjemahanNiatTampilDiKeduaLayar() {
        // Jarak baris sengaja tidak dipaku: yang dijaga adalah pasangan `n.terjemahan` lalu
        // `showLatin, showTrans` pada kartu niat yang sama, bukan lekuk indentasinya.
        val kartuNiatDariPreferensi = Regex("""n\.terjemahan,\s*\n\s*showLatin,\s*showTrans,""")
        listOf("Ghaib" to ghaib, "Jenazah" to jenazah).forEach { (nama, teks) ->
            assertTrue("$nama tidak menampilkan terjemahan niat lewat preferensi pengguna",
                kartuNiatDariPreferensi.containsMatchIn(teks))
        }
        assertTrue("\"tampil terjemahan\" tidak lagi menyala secara bawaan, sehingga niat kembali " +
            "tanpa terjemahan bagi pengguna yang belum menyentuh pengaturannya",
            BawaanPreferensi.TAMPIL_TERJEMAHAN)
    }
}
