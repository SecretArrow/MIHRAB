package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aturan preferensi pengingat adzan: saklar per waktu (`Store.ADZAN_ON`), panjang timer iqomah, dan
 * volume suara bawaan.
 *
 * Ketiganya dahulu ditulis di beberapa tempat sekaligus — `PrayerScheduler` dan layar Adzan untuk
 * CSV-nya, tiga tempat untuk batas menit iqomah, dan dua tempat untuk volume (`0.8f`). Bila salinannya
 * berbeda, layar dapat menampilkan angka yang tak pernah dipakai notifikasi, atau menyalakan satu waktu
 * mematikan waktu lain. Uji ini menjaga agar aturannya tetap satu.
 */
class PengingatAdzanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    // ---- Saklar per waktu ----

    @Test
    fun saklarSelaluLimaDanSejajarDenganDaftarNama() {
        assertEquals(5, PengingatAdzan.parseOn("1,1,1,1,1").size)
        assertEquals(JadwalHariIni.LIMA_WAKTU.size, PengingatAdzan.JUMLAH_WAKTU)
        assertEquals(
            listOf(true, false, true, false, true),
            PengingatAdzan.parseOn("1,0,1,0,1"),
        )
    }

    /** Entri yang hilang dianggap **menyala**: aplikasi bawaannya memang mengingatkan semua waktu. */
    @Test
    fun kekuranganDiisiMenyalaDanKelebihanDipotong() {
        assertEquals(listOf(true, false, true, true, true), PengingatAdzan.parseOn("1,0"))
        assertEquals(listOf(false, true, true, true, true), PengingatAdzan.parseOn("0"))
        assertEquals(5, PengingatAdzan.parseOn("1,1,1,1,1,1,1,1").size)
        assertEquals(listOf(true, true, true, true, true), PengingatAdzan.parseOn("1,1,1,1,1,0,0"))
    }

    @Test
    fun nilaiSelainSatuDianggapMatiDanSpasiDiabaikan() {
        assertEquals(listOf(false, true, true, true, true), PengingatAdzan.parseOn(" 0 , 1 "))
        // CSV kosong: entri pertama terbaca mati, sisanya diisi menyala.
        assertEquals(listOf(false, true, true, true, true), PengingatAdzan.parseOn(""))
    }

    /** Tulis-baca harus bolak-balik sama, karena satu pihak menulis dan pihak lain membaca. */
    @Test
    fun serializeDanParseBolakBalikSepakat() {
        listOf(
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, false, true, false, true),
        ).forEach { asli ->
            assertEquals(asli, PengingatAdzan.parseOn(PengingatAdzan.serializeOn(asli)))
        }
        assertEquals("1,0,1,0,1", PengingatAdzan.serializeOn(listOf(true, false, true, false, true)))
    }

    // ---- Panjang timer iqomah ----

    @Test
    fun menitIqomahDijepitKeRentangSah() {
        assertEquals(PengingatAdzan.IQOMAH_MIN_MENIT, PengingatAdzan.iqomahMenit(0))
        assertEquals(PengingatAdzan.IQOMAH_MIN_MENIT, PengingatAdzan.iqomahMenit(-5))
        assertEquals(PengingatAdzan.IQOMAH_MAKS_MENIT, PengingatAdzan.iqomahMenit(61))
        assertEquals(PengingatAdzan.IQOMAH_MAKS_MENIT, PengingatAdzan.iqomahMenit(9_999))
        assertEquals(25, PengingatAdzan.iqomahMenit(25))
    }

    /** Bawaan harus berada di dalam rentang, jika tidak penjepitannya langsung mengubah angkanya. */
    @Test
    fun bawaanBeradaDiDalamRentang() {
        assertEquals(PengingatAdzan.IQOMAH_DEFAULT_MENIT,
            PengingatAdzan.iqomahMenit(PengingatAdzan.IQOMAH_DEFAULT_MENIT))
        assertTrue(PengingatAdzan.IQOMAH_DEFAULT_MENIT >= PengingatAdzan.IQOMAH_MIN_MENIT)
        assertTrue(PengingatAdzan.IQOMAH_DEFAULT_MENIT <= PengingatAdzan.IQOMAH_MAKS_MENIT)
        assertFalse(PengingatAdzan.IQOMAH_MIN_MENIT > PengingatAdzan.IQOMAH_MAKS_MENIT)
    }

    // ---- Volume suara ----

    /** Volume di luar 0–1 akan dijepit pemutar suara, sehingga angka di layar tak lagi berarti. */
    @Test
    fun volumeBawaanBeradaDiDalamRentang() {
        assertTrue("volume bawaan harus terdengar", PengingatAdzan.VOL_DEFAULT > 0f)
        assertTrue("volume bawaan tak boleh melebihi 1", PengingatAdzan.VOL_DEFAULT <= 1f)
    }

    /** Volume bawaan tak boleh ditulis ulang di tempat yang membaca `Store.ADZAN_VOL`. */
    @Test
    fun volumeBawaanTidakDitulisUlang() {
        val pelanggar = mutableListOf<String>()
        val pola = Regex("""Store\.ADZAN_VOL\s*,\s*[\d.]+f""")
        File(root(), "src/main/java/id/or/kemenag/yasintahlil").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { f ->
                val isi = f.readText()
                pola.findAll(isi).forEach { pelanggar += "${f.name}: ${it.value}" }
            }
        assertTrue("Volume adzan ditulis ulang di luar PengingatAdzan: $pelanggar", pelanggar.isEmpty())
    }

    // ---- Saklar bawaan ----

    /**
     * CSV saklar bawaan dahulu diketik langsung di `PrayerScheduler` dan layar Adzan. Panjangnya
     * harus sepanjang daftar waktu: CSV yang lebih panjang memotong waktu terakhir, dan yang lebih
     * pendek menyisakan entri yang diisi "menyala" — perbedaannya tak terlihat sampai satu waktu
     * diam-diam mati.
     */
    @Test
    fun saklarBawaanSejajarDenganDaftarWaktu() {
        assertEquals(JadwalHariIni.LIMA_WAKTU.size, PengingatAdzan.parseOn(PengingatAdzan.ON_DEFAULT).size)
        assertEquals(List(PengingatAdzan.JUMLAH_WAKTU) { true }, PengingatAdzan.parseOn(PengingatAdzan.ON_DEFAULT))
        assertEquals("1,1,1,1,1", PengingatAdzan.ON_DEFAULT)
        // Tulis-baca bolak-balik: bawaan yang ditulis penjadwal harus terbaca sama oleh pekerja.
        assertEquals(PengingatAdzan.ON_DEFAULT, PengingatAdzan.serializeOn(PengingatAdzan.parseOn(PengingatAdzan.ON_DEFAULT)))
    }

    // ---- Jeda pengingat sebelum adzan ----

    @Test
    fun jedaPreAdzanDijepitKeRentangSah() {
        assertEquals(PengingatAdzan.PRE_MIN_MIN_MENIT, PengingatAdzan.preMenit(-1))
        assertEquals(PengingatAdzan.PRE_MIN_MIN_MENIT, PengingatAdzan.preMenit(-99))
        assertEquals(PengingatAdzan.PRE_MIN_MAKS_MENIT, PengingatAdzan.preMenit(31))
        assertEquals(PengingatAdzan.PRE_MIN_MAKS_MENIT, PengingatAdzan.preMenit(9_999))
        assertEquals(10, PengingatAdzan.preMenit(10))
    }

    /**
     * Bawaan 0 berarti **tanpa** pengingat tambahan — nilai yang sah, bukan "belum diatur". Bila
     * bawaannya keluar rentang, penjepitan akan langsung mengubahnya menjadi pengingat yang tak
     * pernah diminta pengguna.
     */
    @Test
    fun jedaPreAdzanBawaanBeradaDiDalamRentang() {
        assertEquals(PengingatAdzan.PRE_MIN_MIN_MENIT, PengingatAdzan.PRE_MIN_DEFAULT)
        assertEquals(PengingatAdzan.PRE_MIN_DEFAULT, PengingatAdzan.preMenit(PengingatAdzan.PRE_MIN_DEFAULT))
        assertFalse(PengingatAdzan.PRE_MIN_MIN_MENIT > PengingatAdzan.PRE_MIN_MAKS_MENIT)
    }

    /** Saklar dan jeda bawaan tak boleh ditulis ulang di tempat yang membacanya dari `Store`. */
    @Test
    fun bawaanPengingatTidakDitulisUlang() {
        val pelanggar = mutableListOf<String>()
        val pola = listOf(
            Regex("""Store\.ADZAN_ON\s*,\s*"[\d,]+""""),
            Regex("""Store\.ADZAN_PRE_MIN\s*,\s*\d+"""),
        )
        File(root(), "src/main/java/id/or/kemenag/yasintahlil").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { f ->
                val isi = f.readText()
                pola.forEach { p -> p.findAll(isi).forEach { pelanggar += "${f.name}: ${it.value}" } }
            }
        assertTrue("Bawaan pengingat ditulis ulang di luar PengingatAdzan: $pelanggar", pelanggar.isEmpty())
    }

    // ---- Nama saklar yang muncul di lebih dari satu layar -------------------------------------

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Kode tanpa komentar: KDoc yang menjelaskan pola lama memuat tulisan yang sama persis. */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun layar(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    @Test
    fun namaSaklarTidakKosongDanTidakSama() {
        assertTrue("nama saklar pengingat waktu kosong", PengingatAdzan.NAMA_PENGINGAT_WAKTU.isNotBlank())
        assertTrue("nama saklar pengingat doa kosong", PengingatAdzan.NAMA_PENGINGAT_DOA.isNotBlank())
        assertFalse("dua saklar berbeda memakai nama yang sama",
            PengingatAdzan.NAMA_PENGINGAT_WAKTU == PengingatAdzan.NAMA_PENGINGAT_DOA)
    }

    /**
     * Nama kedua saklar itu hanya boleh hidup di `PengingatAdzan`. Menyalinnya ke layar berarti
     * layar itu dapat menyebut saklar yang sama dengan kata yang berbeda — persis yang terjadi pada
     * saklar utama, yang dahulu bernama "Pengingat ibadah" di Pengaturan sementara beranda
     * menyebutnya "Pengingat waktu sholat".
     */
    @Test
    fun namaSaklarTidakDisalinKeLayar() {
        val nama = listOf(PengingatAdzan.NAMA_PENGINGAT_WAKTU, PengingatAdzan.NAMA_PENGINGAT_DOA)
        val penyalin = berkasMain()
            .filter { it.name != "PengingatAdzan.kt" }
            .filter { f -> nama.any { kode(f).contains("\"$it\"") } }
            .map { it.name }
        assertTrue("nama saklar disalin ke luar `PengingatAdzan`: $penyalin", penyalin.isEmpty())
    }

    /** Nama lama yang berbeda untuk saklar utama tidak boleh kembali. */
    @Test
    fun namaSaklarUtamaYangBerbedaTidakKembali() {
        val pelanggar = berkasMain().filter { kode(it).contains("\"Pengingat ibadah\"") }.map { it.name }
        assertTrue("saklar `Store.NOTIF` disebut dengan nama lain: $pelanggar", pelanggar.isEmpty())
    }

    /**
     * Setiap tempat yang menyalakan/mematikan saklar menyebut namanya dari kosakata bersama, nama
     * itu menempel pada kunci `Store` yang benar, dan **nama saklar yang lain tidak ikut hadir** di
     * layar itu. Nama yang tertukar sama menyesatkannya dengan saklar tanpa nama: pengguna membaca
     * "Pengingat membaca doa setelah adzan" sementara saklar yang bergerak adalah pengingat waktu.
     */
    @Test
    fun tiapTempatSaklarMemakaiNamaDanKunciYangBenar() {
        data class Harapan(val ada: List<String>, val tidakAda: List<String>)
        val harapan = mapOf(
            "feature/home/HomeScreen.kt" to Harapan(
                listOf("PengingatAdzan.NAMA_PENGINGAT_WAKTU", "vm.setNotif"),
                listOf("PengingatAdzan.NAMA_PENGINGAT_DOA")),
            "feature/settings/SettingsScreen.kt" to Harapan(
                listOf("PengingatAdzan.NAMA_PENGINGAT_WAKTU", "Store.NOTIF"),
                listOf("PengingatAdzan.NAMA_PENGINGAT_DOA")),
            "feature/adzan/AdzanScreen.kt" to Harapan(
                listOf("PengingatAdzan.NAMA_PENGINGAT_DOA", "Store.DOA_ADZAN_REMINDER"),
                listOf("PengingatAdzan.NAMA_PENGINGAT_WAKTU")),
            "feature/adzan/IqomahDoaScreen.kt" to Harapan(
                listOf("PengingatAdzan.NAMA_PENGINGAT_DOA", "Store.DOA_ADZAN_REMINDER"),
                listOf("PengingatAdzan.NAMA_PENGINGAT_WAKTU")),
        )
        harapan.forEach { (berkas, h) ->
            val k = layar(berkas)
            h.ada.forEach { potongan ->
                assertTrue("$berkas tidak memuat `$potongan`", k.contains(potongan))
            }
            h.tidakAda.forEach { potongan ->
                assertFalse("$berkas menyebut saklar yang bukan miliknya: `$potongan`", k.contains(potongan))
            }
        }
    }

    /** Aturan bersama harus tetap murni: tanpa Android, agar dapat diuji langsung. */
    @Test
    fun aturanPengingatTidakMenyentuhAndroid() {
        val berkas = File(
            root(),
            "src/main/java/id/or/kemenag/yasintahlil/core/util/PengingatAdzan.kt",
        ).readText()
        assertFalse("PengingatAdzan mengimpor Android sehingga tak dapat diuji di JVM",
            berkas.contains("import android."))
    }
}
