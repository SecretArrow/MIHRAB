package id.or.kemenag.yasintahlil.core.sholat

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.data.SholatInfo
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Sholat Sunnah": *"Kelompokkan sholat berdasarkan waktu pelaksanaan, jumlah rakaat,
 * dan tujuan."*
 *
 * Ujinya memakai daftar sholat sungguhan dari bundel konten: yang penting bukan apakah
 * pengelompokannya benar untuk contoh buatan, melainkan apakah **setiap** sholat sunnah yang
 * benar-benar ada jatuh ke satu kelompok yang bernama dan tidak ada yang hilang.
 *
 * Uji terakhir menjaga satu hal yang berbeda: nama rentang waktu ditulis **sekali**. Sebelumnya
 * setiap nama hidup dua kali — pada cabang [KelompokSholat.waktu] dan pada daftar urutannya — dan
 * tak ada yang menjaga keduanya sejalan. Mengubah satu sisi sudah cukup membuat kelompok itu
 * terlempar diam-diam ke urutan paling akhir (`indexOf` yang gagal mengembalikan −1), tanpa satu pun
 * pesan galat.
 */
class KelompokSholatTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun entri(): List<SholatInfo> {
        val d = JSONObject(File(root(), "src/main/assets/content/sholat_daftar.json").readText())
        val arr = d.getJSONArray("sunnah")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SholatInfo(
                o.getString("id"), o.getString("nama"), o.getString("rakaat"),
                o.getString("waktu"), o.getString("deskripsi"),
                tujuan = o.optString("tujuan"),
            )
        }
    }

    private fun contoh(id: String, rakaat: String, waktu: String, tujuan: String) =
        SholatInfo(id, id, rakaat, waktu, "deskripsi", tujuan = tujuan)

    /** Teks `waktu` yang mewakili setiap rentang — dipakai uji cakupan di bawah. */
    private val contohWaktu = listOf(
        "Setelah matahari terbit hingga sebelum Zuhur",
        "Sepertiga malam terakhir",
        "Malam Ramadan setelah Isya",
        "Sebelum/sesudah sholat wajib",
        "Kapan saja selain waktu terlarang",
        "Saat masuk masjid",
        "Saat gerhana berlangsung",
        "Entah kapan",
    )

    /** Semua teks `waktu` yang harus terbaca: contoh di atas **dan** seluruh entri bundel. */
    private fun korpus(): List<String> = (contohWaktu + entri().map { it.waktu }).distinct()

    @Test
    fun rentangWaktuDibacaDariTeksEntri() {
        assertEquals(KelompokSholat.PAGI,
            KelompokSholat.waktu("Setelah matahari terbit hingga sebelum Zuhur"))
        assertEquals(KelompokSholat.MALAM, KelompokSholat.waktu("Sepertiga malam terakhir"))
        assertEquals(KelompokSholat.MALAM, KelompokSholat.waktu("Malam Ramadan setelah Isya"))
        assertEquals(KelompokSholat.MALAM, KelompokSholat.waktu("Setelah Isya hingga sebelum Subuh"))
        assertEquals(KelompokSholat.KAPAN_SAJA,
            KelompokSholat.waktu("Kapan saja selain waktu terlarang"))
        assertEquals(KelompokSholat.MENGIRINGI,
            KelompokSholat.waktu("Sebelum/sesudah sholat wajib"))
        assertEquals(KelompokSholat.PERISTIWA, KelompokSholat.waktu("Saat masuk masjid"))
        assertEquals(KelompokSholat.PERISTIWA, KelompokSholat.waktu("Saat gerhana berlangsung"))
        // Teks yang tak terbaca tidak ditebak ke rentang mana pun.
        assertEquals(KelompokSholat.LAIN, KelompokSholat.waktu("Entah kapan"))
    }

    /** Setiap sholat sunnah pada bundel harus punya rentang waktu yang bernama. */
    @Test
    fun setiapSholatSunnahPunyaRentangWaktu() {
        val e = entri()
        assertTrue("daftar sholat sunnah kosong", e.size >= 10)
        val takTerbaca = e.filter { KelompokSholat.waktu(it.waktu) == KelompokSholat.LAIN }
            .map { "${it.id} (${it.waktu})" }
        assertTrue("sholat sunnah tanpa rentang waktu: $takTerbaca", takTerbaca.isEmpty())
    }

    /** `tujuan` ada di bundel sejak awal tetapi tak pernah dibaca kode — data mati. */
    @Test
    fun setiapSholatSunnahPunyaTujuanYangTerbaca() {
        val tanpaTujuan = entri().filter { it.tujuan.isBlank() }.map { it.id }
        assertTrue("sholat sunnah tanpa tujuan: $tanpaTujuan", tanpaTujuan.isEmpty())
    }

    /** Ketiga cara pengelompokan harus memuat seluruh entri, masing-masing tepat sekali. */
    @Test
    fun tidakAdaEntriYangHilangAtauBerganda() {
        val e = entri()
        val semuaId = e.map { it.id }.sorted()
        CaraKelompok.entries.forEach { cara ->
            val datar = KelompokSholat.kelompok(e, cara).flatMap { it.second.map { s -> s.id } }
            assertEquals("pengelompokan $cara mengubah isi daftar", semuaId, datar.sorted())
        }
    }

    /** Rakaat dan tujuan harus benar-benar menggabungkan, bukan sekadar daftar ulang. */
    @Test
    fun rakaatMenggabungkanBeberapaSholatDalamSatuKelompok() {
        val k = KelompokSholat.kelompok(entri(), CaraKelompok.RAKAAT)
        assertTrue("kelompok rakaat tidak pernah berisi lebih dari satu sholat: $k",
            k.any { it.second.size > 1 })
    }

    @Test
    fun kelompokTujuanMemakaiTujuanEntriItuSendiri() {
        val e = entri()
        val k = KelompokSholat.kelompok(e, CaraKelompok.TUJUAN)
        val tujuanEntri = e.map { it.tujuan }.toSet()
        assertTrue("ada kelompok tujuan yang bukan milik entri mana pun",
            k.map { it.first }.all { it in tujuanEntri })
        assertTrue("tujuan ada yang hilang", k.map { it.first }.containsAll(tujuanEntri))
    }

    /** Urutan waktu mengikuti pagi → malam → tak terikat waktu, bukan urutan abjad. */
    @Test
    fun urutanWaktuMulaiDariPagi() {
        val k = KelompokSholat.kelompok(entri(), CaraKelompok.WAKTU)
        assertEquals(KelompokSholat.PAGI, k.first().first)
        val nama = k.map { it.first }
        assertTrue("malam harus sesudah pagi",
            nama.indexOf(KelompokSholat.MALAM) > nama.indexOf(KelompokSholat.PAGI))
    }

    /** Entri tanpa tujuan tetap tampil, tidak dibuang diam-diam. */
    @Test
    fun entriTanpaTujuanTetapTampil() {
        val k = KelompokSholat.kelompok(
            listOf(contoh("a", "2 rakaat", KelompokSholat.KAPAN_SAJA, "")),
            CaraKelompok.TUJUAN,
        )
        assertEquals(1, k.size)
        assertEquals(listOf("a"), k.first().second.map { it.id })
    }

    @Test
    fun daftarKosongTidakMenghasilkanKelompok() {
        CaraKelompok.entries.forEach {
            assertTrue("kelompok kosong untuk $it", KelompokSholat.kelompok(emptyList(), it).isEmpty())
        }
    }

    // ---- Satu nama rentang, satu tempat --------------------------------------------------------

    /**
     * Nama-nama ini dibaca pengguna sebagai judul kelompok. Mengubahnya keputusan produk, bukan
     * bagian merapikan kode — jadi ejaannya dipaku di sini, sama seperti nama layar dan nama kendali
     * antarmuka.
     */
    @Test
    fun ejaanRentangWaktuTidakBerubah() {
        assertEquals("Pagi hingga sebelum Zuhur", KelompokSholat.PAGI)
        assertEquals("Malam", KelompokSholat.MALAM)
        assertEquals("Mengiringi sholat wajib", KelompokSholat.MENGIRINGI)
        assertEquals("Kapan saja selain waktu terlarang", KelompokSholat.KAPAN_SAJA)
        assertEquals("Peristiwa tertentu", KelompokSholat.PERISTIWA)
        assertEquals("Waktu lain", KelompokSholat.LAIN)
        // Urutan tampilnya juga keputusan produk: pagi → malam → tak terikat waktu, dan yang tak
        // terbaca paling akhir. Ditulis sebagai teks, bukan salinan daftar di kode.
        assertEquals(
            "urutan tampil rentang waktu berubah",
            listOf(
                "Pagi hingga sebelum Zuhur", "Malam", "Mengiringi sholat wajib",
                "Kapan saja selain waktu terlarang", "Peristiwa tertentu", "Waktu lain",
            ),
            KelompokSholat.URUTAN_WAKTU,
        )
    }

    /**
     * Setiap nama rentang ditulis **sekali** — pada deklarasi konstantanya. Bila ada cabang
     * [KelompokSholat.waktu] yang kembali mengeja teksnya sendiri, di situlah salinan kedua lahir,
     * dan salinan itu dapat menyimpang tanpa ketahuan.
     */
    @Test
    fun namaRentangDitulisSekali() {
        val kode = KodeSumber.kodeTanpaKomentar(
            KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/core/sholat/KelompokSholat.kt"),
        )
        val nama = KelompokSholat.URUTAN_WAKTU
        assertEquals("ada nama rentang yang kembar", nama.size, nama.toSet().size)
        val berganda = nama.filter { kode.split("\"$it\"").size > 2 }
        assertTrue("nama rentang ditulis lebih dari sekali di kode: $berganda", berganda.isEmpty())
    }

    /** Setiap rentang yang keluar dari [KelompokSholat.waktu] harus punya tempat di urutan tampil. */
    @Test
    fun setiapRentangYangDihasilkanAdaDiUrutan() {
        val dihasilkan = korpus().map { KelompokSholat.waktu(it) }.toSet()
        val tanpaTempat = dihasilkan - KelompokSholat.URUTAN_WAKTU.toSet()
        assertTrue("rentang yang tak punya tempat di URUTAN_WAKTU: $tanpaTempat — " +
            "kelompok itu akan terlempar ke urutan paling akhir tanpa pesan galat",
            tanpaTempat.isEmpty())
    }

    /** Tidak ada nama di urutan tampil yang tak pernah dihasilkan — daftar mati yang menyesatkan. */
    @Test
    fun urutanTidakMemuatRentangYangTakPernahDihasilkan() {
        val dihasilkan = korpus().map { KelompokSholat.waktu(it) }.toSet()
        val mati = KelompokSholat.URUTAN_WAKTU.toSet() - dihasilkan
        assertTrue("URUTAN_WAKTU memuat rentang yang tak pernah dihasilkan: $mati", mati.isEmpty())
    }

    /** Rentang tak terbaca selalu di urutan terakhir, bukan terselip di antara rentang bernama. */
    @Test
    fun lainSelaluTerakhir() {
        assertEquals(KelompokSholat.LAIN, KelompokSholat.URUTAN_WAKTU.last())
    }

    /** Layar menawarkan ketiga cara dan menampilkan tujuan yang selama ini tak terbaca. */
    @Test
    fun layarMenawarkanPengelompokanDanTujuan() {
        val s = File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/sholat/SholatScreen.kt").readText()
        assertTrue("layar tidak menawarkan cara pengelompokan",
            s.contains("CaraKelompok.entries.forEach"))
        // Dikelompokkan dari daftar yang **tersaring**, bukan daftar mentah: bila keduanya
        // berbeda, kotak pencarian di halaman ini akan berbenturan dengan pengelompokannya.
        assertTrue("layar tidak mengelompokkan daftar sunnah yang tampil",
            s.contains("KelompokSholat.kelompok(sunnahTampil, cara)"))
        assertTrue("hasil pengelompokan tidak dipakai saat menggambar daftar",
            s.contains("kelompokSunnah.forEach"))
        assertTrue("tujuan tidak ditampilkan", s.contains("s.tujuan"))
        assertTrue("judul tujuan tidak ada", s.contains("\"Tujuan: "))
    }
}
