package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Rentang volume suara adzan harus **satu**, dan setiap jalur yang menyentuh volume harus
 * memakai aturan itu — bukan menjepitnya sendiri-sendiri.
 *
 * Rentangnya dahulu tidak dinyatakan di mana pun. Ia hidup sebagai `coerceIn(0f, 1f)` yang ditulis
 * **tiga kali** — sekali di `AdzanPlayer.playOnce` dan dua kali di `AdzanPreview` (`play` dan
 * `setVolume`) — sementara slider di layar Adzan bahkan tidak punya `valueRange`, sehingga batas
 * yang dapat dipilih pengguna hanya **kebetulan** cocok dengan bawaan Compose. Karena setiap
 * salinan menjepit sendiri-sendiri, tak ada satu pun tempat yang dapat ditunjuk sebagai "rentang
 * volume aplikasi ini".
 *
 * Yang membuat salinan itu berbahaya bukan panjangnya, melainkan **jalur baca yang tidak menjepit
 * sama sekali**. Nilai `Store.ADZAN_VOL` dibaca mentah oleh layar Adzan dan oleh `ReminderWorker`.
 * Selama nilainya selalu datang dari slider (0–1), ketiga salinan itu tampak sepakat. Begitu nilai
 * datang dari luar slider — backup yang disunting, atau pemulihan dari versi lain — jalur bacanya
 * menghormatinya apa adanya: layar menampilkan `"Volume: 500%"` dan slidernya menggambar di ujung
 * kanan, sementara pemutarnya membunyikan 100%. Dua angka untuk satu pengaturan, dan tak satu pun
 * galat menyebut sebabnya. Pola ini sama persis dengan rentang jeda pra-adzan yang sudah lebih
 * dahulu dipusatkan ke [PengingatAdzan.preMenit].
 *
 * Karena itu uji ini memaku tiga hal: rentangnya dieja hanya di [PengingatAdzan], setiap penjepitan
 * volume **diturunkan** dari `PengingatAdzan.volume`, dan **kedua** jalur baca nilai tersimpan
 * melewatinya.
 *
 * Yang **tidak** diperiksa di sini: `coerceIn(0f, 1f)` yang menjepit **pecahan kemajuan** (progress
 * bar, alpha ornamen) — aturannya lain, dan mencampurnya di sini hanya akan membuat pesan gagalnya
 * menunjuk berkas yang salah. Penjaga ini terbatas pada `player.volume`.
 */
class VolumeAdzanSatuRentangTest {

    private fun berkas(relatif: String): String = KodeSumber.sumber(relatif)

    private fun kode(relatif: String): String = KodeSumber.kodeTanpaKomentar(berkas(relatif))

    /** Kode [f] tanpa komentar; dibaca langsung supaya jalurnya tak bergantung [KodeSumber.root]. */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    /** [teks] tanpa spasi, supaya ejaan `coerceIn(0f, 1f)` dan `coerceIn(0f,1f)` sama-sama terbaca. */
    private fun rapat(teks: String): String = teks.filterNot { it.isWhitespace() }

    private fun berkasMain(): List<File> =
        File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private val M = "src/main/java/id/or/kemenag/yasintahlil"
    private val aturan = "$M/core/util/PengingatAdzan.kt"
    private val pemutar = "$M/core/audio/AdzanPlayer.kt"
    private val pratinjau = "$M/core/audio/AdzanPreview.kt"
    private val layar = "$M/feature/adzan/AdzanScreen.kt"
    private val pekerja = "$M/core/data/ReminderWorker.kt"

    /** Volume yang dijepit ke rentang bersama: di luar rentang ditarik ke batasnya. */
    @Test
    fun volumeDijepitKeRentangBersama() {
        // Rentangnya dipaku pada nilainya yang benar: volume audio memang 0–1, dan melebarkannya
        // diam-diam membuat slider dapat memilih angka yang tak pernah dipakai pemutar mana pun.
        assertEquals("batas bawah volume berubah", 0f, PengingatAdzan.VOL_MIN, 0f)
        assertEquals("batas atas volume berubah", 1f, PengingatAdzan.VOL_MAKS, 0f)
        assertTrue("rentang volume terbalik", PengingatAdzan.VOL_MIN < PengingatAdzan.VOL_MAKS)
        assertTrue("volume bawaan di luar rentangnya sendiri",
            PengingatAdzan.VOL_DEFAULT in PengingatAdzan.VOL_MIN..PengingatAdzan.VOL_MAKS)
        assertEquals("volume di bawah batas tidak ditarik ke batas bawah",
            PengingatAdzan.VOL_MIN, PengingatAdzan.volume(PengingatAdzan.VOL_MIN - 0.5f), 0f)
        assertEquals("volume di atas batas tidak ditarik ke batas atas",
            PengingatAdzan.VOL_MAKS, PengingatAdzan.volume(PengingatAdzan.VOL_MAKS + 0.5f), 0f)
        assertEquals("volume yang sah ikut berubah", 0.4f, PengingatAdzan.volume(0.4f), 0f)
        assertEquals("volume bawaan berubah saat dijepit",
            PengingatAdzan.VOL_DEFAULT, PengingatAdzan.volume(PengingatAdzan.VOL_DEFAULT), 0f)
    }

    /**
     * Batasnya tidak boleh dieja ulang. Dulu nilainya hidup sebagai literal di tiga berkas; bila
     * salinannya kembali, satu tempat dapat berubah tanpa yang lain — persis yang membuat rentang
     * ini tidak punya rumah.
     */
    @Test
    fun rentangHanyaDiejaDiPengingatAdzan() {
        val pelanggar = berkasMain()
            .filter { it.name != "PengingatAdzan.kt" }
            .filter { Regex("""(?:const\s+)?val\s+VOL_(?:MIN|MAKS)\s*=""").containsMatchIn(kode(it)) }
            .map { it.name }
        assertTrue("rentang volume dieja ulang di berkas lain: $pelanggar", pelanggar.isEmpty())
    }

    /**
     * Tidak ada jalur mana pun yang menjepit volume dengan **batas literal**. Pola ini sengaja
     * menyasar penugasan ke `player.volume`, sehingga `coerceIn(0f, 1f)` milik progress bar dan
     * alpha tidak ikut tertangkap.
     */
    @Test
    fun takAdaPenjepitanVolumeBerbatasLiteral() {
        val pelanggar = mutableListOf<String>()
        berkasMain().forEach { f ->
            if (Regex("""player\.volume\s*=\s*\w+(\.\w+)*\.coerceIn\(""").containsMatchIn(kode(f))) {
                pelanggar += f.name
            }
        }
        assertTrue("volume masih dijepit dengan batas literal alih-alih aturan bersama: $pelanggar",
            pelanggar.isEmpty())
    }

    /** Pemutar dan pratinjau harus **menurunkan** penjepitannya dari aturan bersama. */
    @Test
    fun pemutarMemakaiAturanBersama() {
        listOf("AdzanPlayer" to pemutar, "AdzanPreview" to pratinjau).forEach { (nama, rel) ->
            val k = kode(rel)
            assertTrue("$nama masih menjepit volume sendiri",
                !rapat(k).contains("coerceIn(0f,1f)"))
            assertTrue("$nama tidak memakai PengingatAdzan.volume",
                k.contains("PengingatAdzan.volume("))
        }
        // Pratinjau menjepit di dua tempat: saat mulai memutar dan saat volume diubah dari slider.
        assertEquals("AdzanPreview tidak lagi menjepit volume di kedua jalurnya",
            2, Regex("""PengingatAdzan\.volume\(""").findAll(kode(pratinjau)).count())
    }

    /** Slider volume di layar harus memakai rentang bersama, bukan mengandalkan bawaan Compose. */
    @Test
    fun sliderVolumeMemakaiRentangBersama() {
        val k = kode(layar)
        assertTrue("slider volume tidak punya valueRange dari aturan bersama",
            rapat(k).contains("valueRange=PengingatAdzan.VOL_MIN..PengingatAdzan.VOL_MAKS"))
        assertTrue("layar Adzan masih menjepit volume sendiri",
            !rapat(k).contains("coerceIn(0f,1f)"))
    }

    /**
     * Kedua jalur baca nilai tersimpan — layar Adzan dan pekerja latar — harus melewati aturan
     * bersama. Inilah inti kekeliruannya: selama hanya pemutar yang menjepit, layar tetap dapat
     * menampilkan angka yang tidak pernah berbunyi.
     */
    @Test
    fun nilaiTersimpanDijepitSaatDibaca() {
        var dibaca = 0
        val pelanggar = mutableListOf<String>()
        berkasMain().forEach { f ->
            val k = kode(f)
            Regex("""store\.get\(Store\.ADZAN_VOL""").findAll(k).forEach { m ->
                dibaca++
                val sebelum = k.substring(maxOf(0, m.range.first - 30), m.range.first)
                if (!sebelum.contains("PengingatAdzan.volume(")) pelanggar += f.name
            }
        }
        // Pola yang berhenti cocok akan membuat uji ini hijau tanpa memeriksa apa pun.
        assertEquals("penjaga tidak lagi menemukan pembacaan Store.ADZAN_VOL — polanya salah",
            2, dibaca)
        assertTrue("nilai volume tersimpan dibaca tanpa dijepit aturan bersama: $pelanggar",
            pelanggar.isEmpty())
    }
}
