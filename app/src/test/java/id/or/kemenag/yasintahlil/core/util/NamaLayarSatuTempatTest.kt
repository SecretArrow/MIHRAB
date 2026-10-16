package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Satu layar, satu nama — di **semua** tempat yang menyebutnya kepada pengguna.
 *
 * Sebelum `NamaLayar` ada, nama sebuah layar ditulis ulang di tiap tempat yang menunjuknya, dan
 * salinan-salinannya sudah menyimpang: layar yang sama disebut *"Yasin"* di kartu beranda,
 * *"Surat Yasin"* di label tujuan tombol "Buka"; *"Adzan"* di kartu beranda tetapi *"Adzan &
 * Pengingat"* di menu Alat; *"Kiblat"*, *"Kompas Kiblat"*, dan *"Arah Kiblat"* untuk satu layar
 * yang sama.
 *
 * Dua layar yang dibuka dari Pengaturan dahulu berada di luar berkas ini dengan alasan bahwa
 * tombolnya menyebut **tindakannya**, bukan nama layarnya. Alasan itu tidak bertahan: tiap bagian
 * di Pengaturan diawali judul bagian yang menyebut layar tujuannya, sehingga nama itu memang muncul
 * lebih dari sekali — dan sudah menyimpang. Layar Tentang ditulis **tiga** cara sekaligus
 * (*"Tentang aplikasi"*, *"Tentang & sumber"*, *"Buka Tentang & privasi"*), sementara Pembaruan
 * konten memakai huruf besar-kecil yang berbeda antara judul bagian dan tombolnya. Dua uji bahkan
 * memaku kedua ejaan Tentang itu — masing-masing hijau, bersama-sama menjaga agar satu layar tetap
 * punya dua nama.
 *
 * Uji ini menjaga dua hal yang berbeda:
 *
 *  1. **Tidak ada salinan** — nama itu tidak boleh dieja ulang di luar `NamaLayar`.
 *  2. **Tidak ada yang tertukar** — tiap tempat yang menunjuk sebuah layar (kartu beranda, menu
 *     Ibadah/Alat, pintasan di layar sholat, tab bawah, dan label tujuan tombol "Buka") harus
 *     memakai nama **layar itu**, bukan nama layar lain. Salinan yang hilang akan ketahuan oleh
 *     pemeriksaan pertama; nama yang tertukar hanya ketahuan oleh yang kedua.
 */
class NamaLayarSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun kode(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Rute → berkas layarnya dan nama konstanta yang harus dipakai **semua** tempat. */
    private val tujuan: Map<String, Pair<String, String>> = mapOf(
        "YASIN" to ("feature/yasin/YasinScreen.kt" to "YASIN"),
        "TAHLIL" to ("feature/tahlil/TahlilScreen.kt" to "TAHLIL"),
        "SHOLAT" to ("feature/sholat/SholatScreen.kt" to "SHOLAT"),
        "WUDHU" to ("feature/sholat/WudhuScreen.kt" to "WUDHU"),
        "GHAIB" to ("feature/sholat/GhaibScreen.kt" to "GHAIB"),
        "JENAZAH" to ("feature/sholat/JenazahScreen.kt" to "JENAZAH"),
        "DZIKIR" to ("feature/dzikir/DzikirScreen.kt" to "DZIKIR"),
        "DOA_ADZAN" to ("feature/adzan/IqomahDoaScreen.kt" to "DOA_ADZAN"),
        "IQOMAH" to ("feature/adzan/IqomahDoaScreen.kt" to "IQOMAH"),
        "ADZAN" to ("feature/adzan/AdzanScreen.kt" to "ADZAN"),
        "KIBLAT" to ("feature/kiblat/KiblatScreen.kt" to "KIBLAT"),
        "JADWAL" to ("feature/jadwal/JadwalScreen.kt" to "JADWAL"),
        "SEARCH" to ("feature/search/SearchScreen.kt" to "SEARCH"),
        "FAVORIT" to ("feature/favorit/FavoritScreen.kt" to "FAVORIT"),
        "SETTINGS" to ("feature/settings/SettingsScreen.kt" to "PENGATURAN"),
        "IBADAH" to ("feature/ibadah/IbadahScreen.kt" to "IBADAH"),
        "ALAT" to ("feature/alat/AlatScreen.kt" to "ALAT"),
        "HOME" to ("feature/home/HomeScreen.kt" to "BERANDA"),
        // Dua layar yang dibuka dari tombol di Pengaturan. Keduanya dahulu di luar berkas ini
        // dengan alasan bahwa tombolnya menyebut **tindakannya**, bukan nama layarnya — padahal
        // tiap bagian di Pengaturan diawali judul bagian yang menyebut layar tujuannya, dan nama
        // itu sudah menyimpang: Tentang ditulis tiga cara, Pembaruan konten dua ejaan.
        "TENTANG" to ("feature/tentang/TentangScreen.kt" to "TENTANG"),
        "KONTEN_UPDATE" to ("feature/settings/ContentUpdateScreen.kt" to "PEMBARUAN_KONTEN"),
        // Amalan & referensi: tasbih, Asmaul Husna, zakat, kalender, khataman, tahlil almarhum.
        // Onboarding tidak termasuk: namanya hanya muncul sekali (layar pengantar sekali di awal).
        "TASBIH" to ("feature/tasbih/TasbihScreen.kt" to "TASBIH"),
        "ASMAUL" to ("feature/asmaul/AsmaulHusnaScreen.kt" to "ASMAUL"),
        "ZAKAT" to ("feature/zakat/ZakatScreen.kt" to "ZAKAT"),
        "KALENDER" to ("feature/kalender/KalenderScreen.kt" to "KALENDER"),
        "KHATAMAN" to ("feature/khataman/KhatamanScreen.kt" to "KHATAMAN"),
        "ALMARHUM" to ("feature/almarhum/AlmarhumScreen.kt" to "ALMARHUM"),
    )

    /**
     * Layar yang judulnya memang **bukan** nama layar: beranda dibuka dengan sapaan. Sisanya harus
     * memuat namanya sendiri, karena pengguna yang datang dari menu harus menemukan layar yang
     * disebut tombolnya.
     */
    private val judulBukanNama = setOf("HOME")

    // ---- Ejaan ---------------------------------------------------------------------------------

    /** Nama-nama ini dibaca pengguna; mengubahnya keputusan produk, bukan bagian merapikan kode. */
    @Test
    fun ejaanNamaLayarTidakBerubah() {
        assertEquals("Beranda", NamaLayar.BERANDA)
        assertEquals("Ibadah", NamaLayar.IBADAH)
        assertEquals("Alat", NamaLayar.ALAT)
        assertEquals("Pengaturan", NamaLayar.PENGATURAN)
        assertEquals("Surat Yasin", NamaLayar.YASIN)
        assertEquals("Tahlil", NamaLayar.TAHLIL)
        assertEquals("Dzikir & Doa", NamaLayar.DZIKIR)
        assertEquals("Panduan Sholat", NamaLayar.SHOLAT)
        assertEquals("Wudhu & Tayamum", NamaLayar.WUDHU)
        assertEquals("Sholat Ghaib", NamaLayar.GHAIB)
        assertEquals("Sholat Jenazah", NamaLayar.JENAZAH)
        assertEquals("Adzan & Pengingat", NamaLayar.ADZAN)
        assertEquals("Doa Adzan", NamaLayar.DOA_ADZAN)
        assertEquals("Iqomah", NamaLayar.IQOMAH)
        assertEquals("Jadwal Sholat", NamaLayar.JADWAL)
        assertEquals("Arah Kiblat", NamaLayar.KIBLAT)
        assertEquals("Pencarian", NamaLayar.SEARCH)
        assertEquals("Favorit & Riwayat", NamaLayar.FAVORIT)
        assertEquals("Tentang aplikasi", NamaLayar.TENTANG)
        assertEquals("Pembaruan konten", NamaLayar.PEMBARUAN_KONTEN)
        assertEquals("Tasbih Digital", NamaLayar.TASBIH)
        assertEquals("Asmaul Husna", NamaLayar.ASMAUL)
        assertEquals("Kalkulator Zakat", NamaLayar.ZAKAT)
        assertEquals("Kalender", NamaLayar.KALENDER)
        assertEquals("Khataman & Streak", NamaLayar.KHATAMAN)
        assertEquals("Tahlil Almarhum", NamaLayar.ALMARHUM)
    }

    /**
     * Dua layar berbeda tidak boleh bernama sama: tombol yang menyebut nama itu akan menunjuk dua
     * tujuan, dan pengguna tak punya cara membedakannya.
     */
    @Test
    fun tidakAdaNamaLayarKembar() {
        val nama = listOf(
            NamaLayar.BERANDA, NamaLayar.IBADAH, NamaLayar.ALAT, NamaLayar.PENGATURAN,
            NamaLayar.YASIN, NamaLayar.TAHLIL, NamaLayar.DZIKIR, NamaLayar.SHOLAT,
            NamaLayar.WUDHU, NamaLayar.GHAIB, NamaLayar.JENAZAH, NamaLayar.ADZAN,
            NamaLayar.DOA_ADZAN, NamaLayar.IQOMAH, NamaLayar.JADWAL, NamaLayar.KIBLAT,
            NamaLayar.SEARCH, NamaLayar.FAVORIT, NamaLayar.TENTANG, NamaLayar.PEMBARUAN_KONTEN,
            NamaLayar.TASBIH, NamaLayar.ASMAUL, NamaLayar.ZAKAT, NamaLayar.KALENDER,
            NamaLayar.KHATAMAN, NamaLayar.ALMARHUM,
        )
        nama.forEach { assertTrue("nama layar kosong", it.isNotBlank()) }
        assertEquals("ada nama layar yang kembar", nama.size, nama.toSet().size)
    }

    /**
     * Inti perbaikan: nama itu tidak boleh hidup di luar `NamaLayar`. Berkas yang menyalinnya dapat
     * menyebut layar yang sama dengan kata yang berbeda — persis yang terjadi pada "Yasin",
     * "Adzan", "Kiblat", dan "Cari" sebelum berkas itu ada.
     */
    @Test
    fun namaLayarTidakDisalinKeLuarNamaLayar() {
        val nama = listOf(
            NamaLayar.BERANDA, NamaLayar.IBADAH, NamaLayar.ALAT, NamaLayar.PENGATURAN,
            NamaLayar.YASIN, NamaLayar.TAHLIL, NamaLayar.DZIKIR, NamaLayar.SHOLAT,
            NamaLayar.WUDHU, NamaLayar.GHAIB, NamaLayar.JENAZAH, NamaLayar.ADZAN,
            NamaLayar.DOA_ADZAN, NamaLayar.IQOMAH, NamaLayar.JADWAL, NamaLayar.KIBLAT,
            NamaLayar.SEARCH, NamaLayar.FAVORIT, NamaLayar.TENTANG, NamaLayar.PEMBARUAN_KONTEN,
            NamaLayar.TASBIH, NamaLayar.ASMAUL, NamaLayar.ZAKAT, NamaLayar.KALENDER,
            NamaLayar.KHATAMAN, NamaLayar.ALMARHUM,
        )
        val penyalin = mutableListOf<String>()
        berkasMain().filter { it.name != "NamaLayar.kt" }.forEach { f ->
            val k = KodeSumber.kodeTanpaKomentar(f.readText())
            nama.filter { k.contains("\"$it\"") }.forEach { penyalin += "${f.name}: \"$it\"" }
        }
        assertTrue("nama layar disalin ke luar `NamaLayar`: $penyalin", penyalin.isEmpty())
    }

    /**
     * Penjaga di atas hanya berarti bila pemindai komentarnya bekerja: `KiblatScreen.kt` menyebut
     * *"Arah Kiblat"* di dalam komentar yang mengutip `plan.txt`, bukan di kode. Kalau komentar
     * tidak dibuang, penjaganya akan menuduh berkas yang justru menjelaskan asal-usul namanya —
     * dan pemindai yang rusak akan membuat penjaganya hijau tanpa arti.
     */
    @Test
    fun pemindaiKomentarBenarBenarMembuangPenjelasan() {
        val f = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/feature/kiblat/KiblatScreen.kt",
        )
        assertTrue("prasyarat: KiblatScreen.kt tidak lagi mengutip nama layarnya di komentar",
            f.readText().contains("\"${NamaLayar.KIBLAT}\""))
        assertFalse("pemindai tidak membuang komentar sehingga menuduh penjelasan yang benar",
            kode("feature/kiblat/KiblatScreen.kt").contains("\"${NamaLayar.KIBLAT}\""))
    }

    // ---- Judul layar ---------------------------------------------------------------------------

    /** Jendela kode sebelum setiap `titleLarge` — yaitu ekspresi judul layarnya. */
    private fun jendelaJudul(k: String): List<String> =
        Regex("""typography\.titleLarge""").findAll(k)
            .map { k.substring(maxOf(0, it.range.first - 400), it.range.first) }
            .toList()

    /**
     * Setiap layar menyebut namanya sendiri di judulnya. Tanpa ini, pengguna yang menekan tombol
     * bernama X tiba di layar yang tak menyebut X di mana pun — persis yang membuat "Kompas Kiblat"
     * terasa menyesatkan.
     */
    @Test
    fun judulLayarMemuatNamanyaSendiri() {
        val pelanggar = mutableListOf<String>()
        tujuan.forEach { (rute, t) ->
            if (rute in judulBukanNama) return@forEach
            val (berkas, konstanta) = t
            val jendela = jendelaJudul(kode(berkas))
            if (jendela.isEmpty()) pelanggar += "$berkas: tak ada judul"
            else if (jendela.none { it.contains("NamaLayar.$konstanta") }) {
                pelanggar += "$berkas: judul tidak memuat NamaLayar.$konstanta"
            }
        }
        assertTrue("judul layar tidak menyebut namanya sendiri: $pelanggar", pelanggar.isEmpty())
    }

    /**
     * Berkas yang memuat **dua** layar (Iqomah dan Doa Adzan tinggal di satu berkas) harus menyebut
     * nama yang benar di **masing-masing** judulnya, bukan satu nama untuk keduanya.
     */
    @Test
    fun tiapJudulDalamSatuBerkasMemakaiNamanyaSendiri() {
        val jendela = jendelaJudul(kode("feature/adzan/IqomahDoaScreen.kt"))
        assertEquals("jumlah judul di IqomahDoaScreen.kt berubah", 2, jendela.size)
        assertTrue("judul Iqomah tidak memakai NamaLayar.IQOMAH",
            jendela.any { it.contains("NamaLayar.IQOMAH") })
        assertTrue("judul Doa Adzan tidak memakai NamaLayar.DOA_ADZAN",
            jendela.any { it.contains("NamaLayar.DOA_ADZAN") })
    }

    // ---- Tempat yang menunjuk layar ------------------------------------------------------------

    /** Pasangan `(rute, konstanta)` yang ditemukan sebuah pemindai, beserta namanya untuk pesan galat. */
    private fun pasangan(berkas: String, pola: String): Map<String, String> =
        Regex(pola).findAll(kode(berkas)).associate { it.groupValues[1] to it.groupValues[2] }

    private fun periksa(nama: String, ditemukan: Map<String, String>, harusAda: Set<String>) {
        assertEquals("$nama tidak menunjuk rute yang diharapkan", harusAda, ditemukan.keys)
        val tertukar = ditemukan.filter { (rute, konstanta) ->
            konstanta != tujuan.getValue(rute).second
        }
        assertTrue("$nama menamai layar dengan nama yang bukan miliknya: $tertukar", tertukar.isEmpty())
    }

    /**
     * Kartu "Aksi cepat" di beranda — kisi ubin `Triple(NamaLayar, Routes, ikon)`.
     *
     * Kisi ini kini ringkas: enam tujuan terbanyak dalam dua baris utuh. Tujuan lain tidak hilang
     * — Pencarian punya ikonnya di kepala beranda, jadwal dibuka dengan mengetuk kartu waktu
     * sholat, dan sisanya tinggal di menu Ibadah/Alat — tetapi ubin yang mengisi empat baris
     * hanya mengulangi menu itu dan memakan tempat yang lebih berguna bagi bacaan.
     */
    @Test
    fun kartuBerandaMemakaiNamaLayarYangBenar() {
        periksa(
            "HomeScreen.kt",
            pasangan("feature/home/HomeScreen.kt", """NamaLayar\.(\w+),\s*Routes\.(\w+),""")
                .entries.associate { (konstanta, rute) -> rute to konstanta },
            setOf("YASIN", "TAHLIL", "DZIKIR", "SHOLAT", "KIBLAT", "FAVORIT"),
        )
    }

    /**
     * Tombol menu Ibadah dan Alat — dua tempat yang dahulu memakai nama paling berbeda.
     *
     * Kedua menu kini berbentuk baris ubin `Triple(Routes, NamaLayar, ikon)` yang diberikan ke
     * komponen bersama `MenuBaris`; labelnya bukan lagi `Button { Text(NamaLayar) }`, tetapi
     * pasangan rute→nama tetap dipetakan dari daftar itu — nama yang salah pasang tetap tertangkap.
     */
    @Test
    fun menuIbadahDanAlatMenamaiLayarTujuannya() {
        periksa(
            "IbadahScreen.kt",
            pasangan("feature/ibadah/IbadahScreen.kt",
                """Routes\.(\w+),\s*NamaLayar\.(\w+),"""),
            // Tasbih, zakat, dan tahlil almarhum pindah ke menu Alat (2026-10-16): menu Ibadah
            // murni bacaan, panduan, referensi, dan simpanan.
            setOf("YASIN", "TAHLIL", "SHOLAT", "WUDHU", "GHAIB", "JENAZAH", "DZIKIR",
                "DOA_ADZAN", "SEARCH", "FAVORIT", "ASMAUL", "KHATAMAN"),
        )
        periksa(
            "AlatScreen.kt",
            pasangan("feature/alat/AlatScreen.kt",
                """Routes\.(\w+),\s*NamaLayar\.(\w+),"""),
            // Pencarian tidak lagi diulang di sini: bagian "Temuan" yang hanya memuat satu
            // baris sudah dibubarkan — pintunya tetap ada di menu Ibadah (bagian Simpanan)
            // dan di ikon Pencarian pada kepala beranda. Tasbih, zakat, dan tahlil almarhum
            // pindah ke sini dari menu Ibadah (2026-10-16) — penghitung dan hitungan rumahnya
            // di tab Alat.
            setOf("KIBLAT", "JADWAL", "ADZAN", "IQOMAH", "KALENDER", "TASBIH", "ZAKAT", "ALMARHUM"),
        )
        periksa(
            "SholatDetailScreen.kt",
            pasangan("feature/sholat/SholatDetailScreen.kt",
                """go\(Routes\.(\w+)\)\s*\}\)\s*\{\s*Text\(NamaLayar\.(\w+)\)"""),
            setOf("WUDHU", "KIBLAT"),
        )
    }

    /** Tab bawah menyebut bagiannya dengan nama yang sama dengan judul menunya. */
    @Test
    fun tabBawahMemakaiNamaBagianYangSama() {
        periksa(
            "AppNav.kt",
            pasangan("navigation/AppNav.kt", """Triple\(Routes\.(\w+),\s*NamaLayar\.(\w+),"""),
            setOf("HOME", "IBADAH", "ALAT", "SETTINGS"),
        )
    }

    /**
     * Judul bagian dan tombol di Pengaturan menyebut layar tujuannya dengan nama bersama.
     *
     * Bagian inilah yang dahulu membuat berkas ini menyebut Pengaturan sebagai pengecualian:
     * tombolnya dianggap menyebut **tindakannya** saja. Nyatanya tiap bagian diawali judul bagian
     * yang menyebut layar tujuannya, sehingga namanya muncul dua kali di layar yang sama — dan
     * keduanya sudah berbeda dari judul layar yang dibukanya.
     *
     * Judul bagian Pengaturan kini seragam memakai `BagianJudul` (pengelompokan berkartu); namanya
     * tetap dibaca dari kosakata bersama — satu nama untuk satu layar, di mana pun ia disebut.
     */
    @Test
    fun pengaturanMenamaiLayarTujuannyaDenganNamaBersama() {
        val k = kode("feature/settings/SettingsScreen.kt")
        listOf("NamaLayar.TENTANG" to "Tentang", "NamaLayar.PEMBARUAN_KONTEN" to "Pembaruan konten")
            .forEach { (rujukan, sebutan) ->
                assertTrue("judul bagian $sebutan di Pengaturan tidak memakai $rujukan",
                    k.contains("BagianJudul($rujukan"))
                assertTrue("tombol $sebutan di Pengaturan tidak menyusun labelnya dari $rujukan",
                    k.contains("Buka \${$rujukan}"))
            }
        // Kalimat keterangan di bawahnya pun menyebut layar itu kepada pengguna; ejaan yang
        // berbeda di sana menyesatkan sama seperti judul yang berbeda.
        assertTrue("keterangan sumber di Pengaturan menyebut layar Tentang dengan ejaan lain",
            k.contains("ada di \${NamaLayar.TENTANG}"))
    }

    /**
     * Label tujuan tombol "Buka" bacaan tersimpan — nama yang paling perlu sejajar, karena di
     * sinilah pengguna diberi tahu ke mana tombolnya akan membawanya.
     */
    @Test
    fun namaTujuanMemakaiNamaLayarYangBenar() {
        val k = kode("navigation/RuteKonten.kt")
        assertFalse("`namaTujuan` masih menuliskan nama layar sebagai literal",
            Regex("Routes\\.\\w+\\s*->\\s*\"").containsMatchIn(k))
        periksa(
            "RuteKonten.namaTujuan",
            pasangan("navigation/RuteKonten.kt", """Routes\.(\w+)\s*->\s*NamaLayar\.(\w+)"""),
            setOf("YASIN", "TAHLIL", "DZIKIR", "DOA_ADZAN", "IQOMAH", "ADZAN", "SHOLAT",
                "GHAIB", "JENAZAH", "SEARCH"),
        )
    }

    /** Aturan nama layar tetap murni: tanpa Android, agar dapat diuji langsung di JVM. */
    @Test
    fun namaLayarTidakMenyentuhAndroid() {
        val berkas = KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/util/NamaLayar.kt",
        )
        assertFalse("NamaLayar mengimpor Android sehingga tak dapat diuji di JVM",
            berkas.contains("import android."))
    }
}
