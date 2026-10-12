package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.data.DzikirGroup
import id.or.kemenag.yasintahlil.core.data.DzikirItem
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * `plan.txt` §"Beranda Utama" meminta *"kartu informasi Islami harian yang dapat dinonaktifkan"*.
 *
 * Kartu itu dahulu memuat satu kalimat **tetap** — *"Perbanyak sholawat di hari Jumat (HR Abu
 * Dawud)"* — yang ditampilkan setiap hari, sehingga judulnya "harian" sementara isinya hari Jumat.
 * Uji ini menjaga agar isinya benar-benar berganti mengikuti tanggal, tetap berasal dari konten yang
 * sudah terverifikasi, dan tidak pernah menampilkan anjuran hari lain pada hari yang salah.
 */
class InfoHarianTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun butir(judul: String) = InfoHarian.Butir(judul, "اَللّٰهُمَّ", "terjemahan $judul", "HR Test — sunnah.com")

    /** Kandidat nyata dari bundel konten, dibaca seperti yang dibaca aplikasi. */
    private fun kandidatBundel(): List<InfoHarian.Butir> {
        val d = JSONObject(File(root(), "src/main/assets/content/dzikir.json").readText())
        val arr = d.getJSONArray("kelompok")
        val grup = (0 until arr.length()).map { gi ->
            val g = arr.getJSONObject(gi)
            val items = g.getJSONArray("items")
            DzikirGroup(g.getString("id"), g.getString("judul"),
                (0 until items.length()).map { i ->
                    val o = items.getJSONObject(i)
                    DzikirItem(o.getString("judul"), o.optString("arab"), o.optString("latin"),
                        o.optString("terjemahan"), o.optInt("ulangi", 1), o.optString("sumber"))
                })
        }
        return InfoHarian.dariDzikir(grup)
    }

    @Test
    fun butirBergilirMenurutTanggalDanTidakBerubahPadaHariYangSama() {
        val k = listOf(butir("A"), butir("B"), butir("C"))
        // 5 Januari 2026 (Senin) dan 6 Januari 2026 (Selasa) — dua hari berturut yang bukan Jumat.
        val senin = LocalDate.of(2026, 1, 5)
        val selasa = LocalDate.of(2026, 1, 6)
        assertTrue("tanggal uji bukan hari kerja biasa",
            !InfoHarian.jumat(senin) && !InfoHarian.jumat(selasa))
        assertEquals("hari ke-5 tidak memilih butir ketiga",
            "C", InfoHarian.pilih(senin, k)?.judul)
        assertEquals("hari ke-6 tidak memilih butir pertama",
            "A", InfoHarian.pilih(selasa, k)?.judul)
        assertEquals("butir berubah pada hari yang sama",
            InfoHarian.pilih(senin, k), InfoHarian.pilih(senin, k))
    }

    /** Anjuran sholawat memang khusus hari Jumat; ia tidak boleh muncul pada hari lain. */
    @Test
    fun anjuranJumatHanyaTampilPadaHariJumat() {
        val k = listOf(butir("A"), butir("B"))
        val jumat = LocalDate.of(2026, 10, 16)
        assertTrue("tanggal uji bukan Jumat", InfoHarian.jumat(jumat))
        val b = InfoHarian.pilih(jumat, k)!!
        assertEquals("hari Jumat tidak memakai anjuran sholawat", InfoHarian.PESAN_JUMAT, b.teks)
        assertTrue("sumber anjuran Jumat hilang", b.sumber.contains("sunnah.com"))
        // Enam hari sisanya tidak boleh satu pun menampilkan anjuran itu.
        (17..22).map { LocalDate.of(2026, 10, it) }.forEach { t ->
            assertNotEquals("anjuran Jumat tampil pada $t", InfoHarian.PESAN_JUMAT,
                InfoHarian.pilih(t, k)?.teks)
        }
    }

    /** Tanpa kandidat, hari biasa tidak menampilkan bacaan milik hari lain — hanya kartu kosong. */
    @Test
    fun tanpaKandidatHariBiasaTidakMenampilkanApaPun() {
        assertNull("hari biasa menampilkan butir meski tak ada kandidat",
            InfoHarian.pilih(LocalDate.of(2026, 1, 5), emptyList()))
        // Anjuran Jumat tidak bergantung pada bundel konten: ia ditulis di aturan, bukan di bacaan.
        assertEquals(InfoHarian.PESAN_JUMAT,
            InfoHarian.pilih(LocalDate.of(2026, 10, 16), emptyList())?.teks)
    }

    /**
     * Butir harian tidak boleh mengarang teks: seluruhnya berasal dari pustaka dzikir di bundel,
     * lengkap dengan terjemahan dan sumbernya, dan lafaznya berharakat.
     */
    @Test
    fun butirBerasalDariBundelDanBerharakat() {
        val k = kandidatBundel()
        assertTrue("kandidat butir harian terlalu sedikit: ${k.size}", k.size >= 10)
        k.forEach { b ->
            assertTrue("butir tanpa judul", b.judul.isNotBlank())
            assertTrue("butir tanpa terjemahan: ${b.judul}", b.teks.isNotBlank())
            assertTrue("butir tanpa sumber: ${b.judul}", b.sumber.isNotBlank())
            assertTrue("lafaz ${b.judul} tanpa harakat",
                b.arab.any { it.code in 0x064B..0x0652 })
        }
        // Entri penunjuk (tanpa lafaz/terjemahan sendiri) tidak ikut menjadi kandidat.
        assertTrue("entri penunjuk ikut menjadi butir harian",
            k.none { it.arab.isBlank() || it.teks.isBlank() })
    }

    /** "Hari ini" mengikuti zona waktu tampilan, sama seperti jam di beranda. */
    @Test
    fun tanggalMengikutiZonaWaktuTampilan() {
        // 1 Januari 2026 pukul 17.30 UTC masih 2 Januari di Jakarta (UTC+7) tetapi 1 Januari di London.
        val t = java.time.Instant.parse("2026-01-01T17:30:00Z").toEpochMilli()
        assertEquals(LocalDate.of(2026, 1, 2), InfoHarian.tanggal(t, ZoneId.of("Asia/Jakarta")))
        assertEquals(LocalDate.of(2026, 1, 1), InfoHarian.tanggal(t, ZoneId.of("Europe/London")))
        // Id zona yang kosong atau rusak tidak boleh menghentikan aplikasi.
        assertEquals(ZoneId.systemDefault(), InfoHarian.zona(""))
        assertEquals(ZoneId.systemDefault(), InfoHarian.zona("bukan/zona"))
        assertEquals(ZoneId.of("Asia/Jakarta"), InfoHarian.zona("Asia/Jakarta"))
    }

    /**
     * Aturan ini tinggal di satu tempat: beranda tidak boleh kembali menuliskan sendiri kalimat
     * anjuran Jumat — itulah yang dahulu membuatnya tampil setiap hari.
     */
    @Test
    fun berandaMemakaiAturanBersama() {
        val layar = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/feature/home/HomeScreen.kt"))
        assertTrue("beranda masih menuliskan sendiri anjuran hari Jumat",
            !layar.contains("Perbanyak sholawat"))
        assertTrue("beranda tidak menampilkan butir dari HomeUiState.info",
            layar.contains("b.judul"))
        assertTrue("kegagalan membaca butir harian tidak diberitahukan di beranda",
            layar.contains("s.infoGagal"))
        // Kalimat anjuran Jumat tinggal di aturan, bukan di layar, supaya tidak ada dua salinan.
        assertTrue("anjuran Jumat tidak ada lagi di InfoHarian", KodeSumber.sumber(
            "src/main/java/id/or/kemenag/yasintahlil/core/util/InfoHarian.kt")
            .contains("Perbanyak sholawat"))
    }
}
