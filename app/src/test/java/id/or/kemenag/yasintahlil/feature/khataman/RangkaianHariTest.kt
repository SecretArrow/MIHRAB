package id.or.kemenag.yasintahlil.feature.khataman

import id.or.kemenag.yasintahlil.feature.tasbih.TasbihHarian
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Patokan hitungan rangkaian hari (streak) dipakukan di sini — angkanya tampil kepada pengguna
 * sebagai capaian pribadi ("N hari berturut-turut"), jadi definisi yang bergeser sedikit pun
 * (kapan rangkaian dianggap putus, bagaimana menghitung lubang) harus ketahuan sebelum terbit.
 * `RangkaianHari` sengaja tanpa Android: uji ini berjalan murni di JVM.
 *
 * Definisi yang dipaku: rangkaian yang sedang berjalan adalah hari-hari berturut-turut yang
 * berakhir pada hari ini, ATAU pada kemarin bila hari ini belum ada aktivitas; lubang memutus
 * rangkaian — hari yang sendirian di seberang lubang adalah rangkaian baru panjangnya satu, dan
 * rangkaian lamanya hanya hidup di `terpanjang`.
 */
class RangkaianHariTest {

    /** Patokan tetap, bukan `LocalDate.now()`: hasil uji tidak boleh bergantung hari ia dijalankan. */
    private val hariIni = LocalDate.of(2026, 3, 7)

    private fun iso(tanggal: LocalDate): String = RangkaianHari.iso(tanggal)

    @Test
    fun tanpaAktivitasRangkaianNol() {
        assertEquals(0, RangkaianHari.rangkaian(emptyList(), iso(hariIni)))
    }

    @Test
    fun hariIniSajaSatu() {
        assertEquals(1, RangkaianHari.rangkaian(listOf(iso(hariIni)), iso(hariIni)))
    }

    @Test
    fun kemarinSajaMasihHidup() {
        // Hari ini belum beraktivitas: rangkaian kemarin belum putus — mengukurnya nol hanya
        // karena pengguna membuka layar ini sebelum sempat membaca.
        assertEquals(1, RangkaianHari.rangkaian(listOf(iso(hariIni.minusDays(1))), iso(hariIni)))
    }

    @Test
    fun kemarinDanHariIniDua() {
        assertEquals(
            2,
            RangkaianHari.rangkaian(
                listOf(iso(hariIni.minusDays(1)), iso(hariIni)),
                iso(hariIni),
            ),
        )
    }

    @Test
    fun duaHariKosongBeruntunPutus() {
        // Terakhir aktif dua hari lalu: hari ini dan kemarin sama-sama kosong → rangkaian 0.
        assertEquals(0, RangkaianHari.rangkaian(listOf(iso(hariIni.minusDays(2))), iso(hariIni)))
    }

    @Test
    fun lubangMemisahkanHariIniDariRangkaianLama() {
        // h-3 dan h-2 tercatat, h-1 bolong, hari ini tercatat: hari ini terpisah dari rangkaian
        // lamanya, jadi yang sedang berjalan panjangnya satu — bukan dua.
        val tanggal = listOf(iso(hariIni.minusDays(3)), iso(hariIni.minusDays(2)), iso(hariIni))
        assertEquals(1, RangkaianHari.rangkaian(tanggal, iso(hariIni)))
        // Rangkaian lama [h-3, h-2] tidak lenyap: ia tetap tercatat sebagai rekor di terpanjang.
        assertEquals(2, RangkaianHari.terpanjang(tanggal))
    }

    @Test
    fun urutanTeracakDihitungBerturutTurut() {
        // Tabel tidak menjanjikan urutan; hitungan tidak boleh menuntut pemanggil yang rajin menyortir.
        val acak = listOf(iso(hariIni), iso(hariIni.minusDays(2)), iso(hariIni.minusDays(1)))
        assertEquals(3, RangkaianHari.rangkaian(acak, iso(hariIni)))
    }

    @Test
    fun tanggalKembarDihitungSatuHari() {
        // Satu hari boleh memuat dua jenis kegiatan (yasin_buka dan tahlil_buka) — keduanya
        // tetap satu hari baca, bukan dua.
        val kemarin = iso(hariIni.minusDays(1))
        assertEquals(2, RangkaianHari.rangkaian(listOf(kemarin, kemarin, iso(hariIni)), iso(hariIni)))
    }

    @Test
    fun hariIniTakTerbacaMengembalikanNol() {
        assertEquals(0, RangkaianHari.rangkaian(listOf(iso(hariIni)), "bukan-tanggal"))
    }

    @Test
    fun tanggalMasaDepanTidakMenghidupkanRangkaian() {
        // Aktivitas tercatat besok (mis. jam perangkat sempat melompat): rangkaian hari ini tetap
        // diukur dari hari ini ke belakang, bukan dari baris yang paling muda.
        assertEquals(0, RangkaianHari.rangkaian(listOf(iso(hariIni.plusDays(1))), iso(hariIni)))
    }

    @Test
    fun terpanjangMemilihRekorBukanYangTerakhir() {
        // Dua rangkaian: [h-9..h-7] panjang 3, [h-3..hari ini] panjang 4 — rekor 4; rangkaian
        // yang lebih baru tidak boleh menimpa rekor yang lebih panjang.
        val tanggal = (7..9).map { iso(hariIni.minusDays(it.toLong())) } + listOf(
            iso(hariIni.minusDays(3)), iso(hariIni.minusDays(2)), iso(hariIni.minusDays(1)), iso(hariIni),
        )
        assertEquals(4, RangkaianHari.terpanjang(tanggal))
    }

    @Test
    fun terpanjangTanpaAktivitasNol() {
        assertEquals(0, RangkaianHari.terpanjang(emptyList()))
    }

    @Test
    fun isoBentukTetapEmpatDigitTahunDuaDigitSisanya() {
        assertEquals("2026-03-07", RangkaianHari.iso(LocalDate.of(2026, 3, 7)))
        assertEquals("2026-02-03", RangkaianHari.iso(LocalDate.of(2026, 2, 3)))
    }

    @Test
    fun isoSejajarDenganTasbihHarian() {
        // RangkaianHari.iso adalah rujukan ke TasbihHarian.iso, bukan salinan kedua — bentuk
        // keduanya tidak boleh berbeda untuk tanggal apa pun, sebab keduanya mengisi kolom
        // tanggal tabel yang sama.
        listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(1, 1, 1)).forEach { t ->
            assertEquals(TasbihHarian.iso(t), RangkaianHari.iso(t))
        }
    }

    @Test
    fun jenisKegiatanPersisKolomTabel() {
        // Ejaan ini adalah isi kolom `jenis` (KegiatanEntity) — salah eja berarti baris baru tak
        // lagi mengenali baris lama, jadi dipaku di sini.
        assertEquals("yasin_buka", RangkaianHari.YASIN_BUKA)
        assertEquals("tahlil_buka", RangkaianHari.TAHIL_BUKA)
        assertEquals("khatam_yasin", RangkaianHari.KHATAM_YASIN)
    }
}
