package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.util.TimeZone

/**
 * Aturan bersama penyusun jadwal sholat dari preferensi mentah.
 *
 * Aturan ini dahulu **disalin** ke tiga tempat (`PrayerScheduler`, `JadwalViewModel`,
 * `HomeViewModel`): `parseCorr` yang identik, pemetaan mazhab Asar, dan pemilihan zona waktu. Tiga
 * salinan berarti tiga kesempatan untuk berbeda — dan bila berbeda, pengingat berbunyi pada menit
 * yang tidak sama dengan yang ditampilkan layar. Uji ini menjaga aturan tunggalnya, termasuk
 * kasus tepi yang mudah terlewat (CSV cacat, koreksi melewati tengah malam).
 */
class JadwalHariIniTest {

    // ---- Koreksi menit ----

    @Test
    fun koreksiSelaluEnamAngka() {
        assertEquals(listOf(1, -2, 3, 0, -1, 2), JadwalHariIni.parseCorr("1,-2,3,0,-1,2"))
        // Kelebihan dipotong, kekurangan diisi nol: indeks koreksi tidak boleh bergeser.
        assertEquals(listOf(1, 2, 3, 4, 5, 6), JadwalHariIni.parseCorr("1,2,3,4,5,6,7,8"))
        assertEquals(listOf(1, 2, 0, 0, 0, 0), JadwalHariIni.parseCorr("1,2"))
        assertEquals(List(6) { 0 }, JadwalHariIni.parseCorr(""))
    }

    @Test
    fun koreksiTidakValidDianggapNol() {
        assertEquals(listOf(0, 5, 0, 0, 0, 0), JadwalHariIni.parseCorr("abc, 5 ,,"))
        // Spasi di sekitar angka tidak boleh membuat koreksi hilang.
        assertEquals(listOf(3, 0, 0, 0, 0, 0), JadwalHariIni.parseCorr(" 3 , 0 , 0 , 0 , 0 , 0 "))
    }

    // ---- Mazhab Asar ----

    @Test
    fun hanyaHanafiYangMemakaiFaktorDua() {
        assertTrue(JadwalHariIni.asarHanafi("hanafi"))
        assertEquals(2.0, JadwalHariIni.asarFactor("hanafi"), 0.0)
        listOf("syafii", "maliki", "hambali", "", "HANAFI").forEach { key ->
            assertFalse("$key bukan mazhab Hanafi", JadwalHariIni.asarHanafi(key))
            assertEquals(1.0, JadwalHariIni.asarFactor(key), 0.0)
        }
    }

    // ---- Zona waktu ----

    @Test
    fun zonaKosongMemakaiZonaPerangkat() {
        assertEquals(TimeZone.getDefault(), JadwalHariIni.zona(""))
        assertEquals("Asia/Jakarta", JadwalHariIni.zona("Asia/Jakarta").id)
    }

    /**
     * Id zona yang **tak dikenal** harus jatuh ke zona perangkat, bukan GMT.
     *
     * `TimeZone.getTimeZone` mengembalikan GMT diam-diam untuk id asing — zona yang tak seorang pun
     * pilih — sementara `ZoneId.of` melempar. Selama kedua bentuk itu punya aturan sendiri-sendiri,
     * id yang rusak (mis. dari pemulihan cadangan versi lain) membuat jam beranda dan penjadwal
     * pengingat memakai GMT sementara kartu info harian memakai zona perangkat: dua waktu berbeda
     * untuk hari yang sama. [JadwalHariIni.zonaId] kini aturan tunggalnya, dan [JadwalHariIni.zona]
     * hanya bentuk `TimeZone`-nya.
     */
    @Test
    fun zonaTakDikenalMemakaiZonaPerangkat() {
        listOf("bukan/zona", "WIB", "Asia/Jakartaa", "   ").forEach { id ->
            assertEquals("id zona '$id' tidak jatuh ke zona perangkat",
                TimeZone.getDefault(), JadwalHariIni.zona(id))
            assertEquals("zonaId('$id') tidak jatuh ke zona perangkat",
                ZoneId.systemDefault(), JadwalHariIni.zonaId(id))
        }
        assertEquals(ZoneId.of("Asia/Jakarta"), JadwalHariIni.zonaId("Asia/Jakarta"))
        assertEquals(ZoneId.systemDefault(), JadwalHariIni.zonaId(JadwalHariIni.TZ_DEFAULT))
    }

    /**
     * Tidak ada layar yang boleh menyusun sendiri zona waktunya.
     *
     * Beranda pernah menulis `if (tzId.isBlank()) TimeZone.getDefault() else
     * TimeZone.getTimeZone(tzId)` langsung di composable-nya — salinan aturan yang, untuk id tak
     * dikenal, menghasilkan GMT sementara `InfoHarian.zona` menghasilkan zona perangkat.
     */
    @Test
    fun tidakAdaLayarYangMenyusunZonaWaktuSendiri() {
        val fitur = File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
        val pelanggar = fitur.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { f ->
                val kode = KodeSumber.kodeTanpaKomentar(f.readText())
                kode.contains("TimeZone.getTimeZone(") || kode.contains("TimeZone.getDefault(") ||
                    kode.contains("ZoneId.systemDefault(")
            }
            .map { it.name }.toList()
        assertTrue("layar menyusun zona waktu sendiri alih-alih memakai JadwalHariIni.zona()/zonaId(): $pelanggar",
            pelanggar.isEmpty())
    }

    // ---- Parameter hisab ----

    @Test
    fun paramsMenerjemahkanNilaiMentah() {
        val p = JadwalHariIni.params(
            lat = -6.2, lon = 106.8, methodKey = "UMM_AL_QURA", asarKey = "hanafi",
            corrCsv = "0,1,0,-2,0,0", tz = TimeZone.getTimeZone("Asia/Jakarta"),
        )
        assertEquals(PrayerCalc.Method.UMM_AL_QURA, p.method)
        assertEquals(2.0, p.asarFactor, 0.0)
        assertEquals(listOf(0, 1, 0, -2, 0, 0), p.corr)
        assertEquals("Asia/Jakarta", p.tz.id)
    }

    @Test
    fun metodeTakDikenalJatuhKeKemenag() {
        assertEquals(PrayerCalc.Method.KEMENAG, JadwalHariIni.params(
            lat = 0.0, lon = 0.0, methodKey = "tidak-ada", asarKey = "syafii",
            corrCsv = "", tz = TimeZone.getDefault(),
        ).method)
    }

    /** Varian terurai harus menghasilkan parameter yang sama dengan varian mentah. */
    @Test
    fun keduaVarianParamsSepakat() {
        val tz = TimeZone.getTimeZone("Asia/Jakarta")
        val mentah = JadwalHariIni.params(-6.2, 106.8, "MWL", "hanafi", "0,1,0,-2,0,0", tz)
        val terurai = JadwalHariIni.params(
            lat = -6.2, lon = 106.8, method = PrayerCalc.Method.MWL,
            asarHanafi = true, corr = listOf(0, 1, 0, -2, 0, 0), tz = tz,
        )
        assertEquals(mentah, terurai)
    }

    // ---- Pergeseran jam ----

    @Test
    fun geserMemutarMelewatiTengahMalam() {
        assertEquals("04:25", JadwalHariIni.geser("04:35", -10))
        assertEquals("04:45", JadwalHariIni.geser("04:35", 10))
        // Pengingat sebelum Subuh kerap jatuh pada hari sebelumnya.
        assertEquals("23:55", JadwalHariIni.geser("00:05", -10))
        assertEquals("00:05", JadwalHariIni.geser("23:55", 10))
        assertEquals("00:00", JadwalHariIni.geser("23:30", 30))
        assertEquals("00:00", JadwalHariIni.geser("00:00", 0))
    }

    @Test
    fun geserMenjagaDuaDigit() {
        assertEquals("09:05", JadwalHariIni.geser("09:10", -5))
        assertEquals("23:59", JadwalHariIni.geser("00:04", -5))
    }

    // ---- Lima waktu beradzan ----

    /** Terbit matahari bukan waktu sholat, jadi ia tidak boleh ikut dalam daftar adzan. */
    @Test
    fun adzanHanyaLimaWaktuTanpaTerbit() {
        val t = PrayerTimes("04:35", "05:50", "11:52", "15:12", "18:02", "19:15")
        assertEquals(listOf("04:35", "11:52", "15:12", "18:02", "19:15"), JadwalHariIni.adzan(t))
        assertFalse("Terbit tidak beradzan", JadwalHariIni.adzan(t).contains(t.terbit))
    }

    // ---- Urutan nama sejajar dengan jam & CSV ----

    /**
     * Indeks daftar nama **adalah** arti CSV `Store.ADZAN_ON` dan `Store.CORR`. Bila nama dan jam
     * tak lagi sejajar, mematikan "Magrib" akan mematikan waktu lain, atau koreksi Asar diterapkan
     * pada Zuhur — tanpa satu pun pesan galat.
     */
    @Test
    fun urutanNamaSejajarDenganJamnya() {
        val t = PrayerTimes("04:35", "05:50", "11:52", "15:12", "18:02", "19:15")
        assertEquals(JadwalHariIni.LIMA_WAKTU.size, JadwalHariIni.adzan(t).size)
        assertEquals(JadwalHariIni.ENAM_WAKTU.size, JadwalHariIni.semua(t).size)
        assertEquals(5, JadwalHariIni.LIMA_WAKTU.size)
        assertEquals(6, JadwalHariIni.ENAM_WAKTU.size)
        // Lima waktu beradzan adalah enam waktu minus Terbit, dengan urutan yang sama.
        assertEquals(JadwalHariIni.LIMA_WAKTU, JadwalHariIni.ENAM_WAKTU - "Terbit")
        assertEquals(JadwalHariIni.ENAM_WAKTU.indexOf("Terbit"), 1)
        // Jam tiap nama dapat ditanyakan satu per satu dan hasilnya sama dengan daftar.
        JadwalHariIni.ENAM_WAKTU.forEachIndexed { i, n ->
            assertEquals("jam($n) tidak sejajar dengan semua()[$i]", JadwalHariIni.semua(t)[i],
                JadwalHariIni.jam(t, n))
        }
    }

    @Test
    fun namaTakDikenalTidakDiberiJamTebakan() {
        val t = PrayerTimes("04:35", "05:50", "11:52", "15:12", "18:02", "19:15")
        assertEquals(null, JadwalHariIni.jam(t, "Dhuha"))
        assertEquals("18:02", JadwalHariIni.jam(t, "Magrib"))
    }

    // ---- Bawaan konfigurasi ----

    /**
     * Bawaan koordinat, kota, dan metode adalah **satu** nilai yang dipakai sembilan tempat
     * (layar Jadwal, Beranda, Adzan, Kiblat, penjadwal, pekerja latar). Bila salah satunya bergeser
     * tanpa yang lain, layar dan pengingat menghitung untuk tempat yang berlainan tanpa satu pun
     * pesan galat — pengguna hanya melihat dua jam yang tidak sama untuk sholat yang sama.
     */
    @Test
    fun bawaanKonfigurasiJadwalKonsisten() {
        assertTrue("Lintang di luar rentang", JadwalHariIni.LAT_DEFAULT in -90.0..90.0)
        assertTrue("Bujur di luar rentang", JadwalHariIni.LON_DEFAULT in -180.0..180.0)
        assertTrue("Kota bawaan kosong", JadwalHariIni.KOTA_DEFAULT.isNotBlank())
        // Jakarta, bukan sekadar koordinat mana pun yang sah.
        assertEquals(-6.2, JadwalHariIni.LAT_DEFAULT, 1e-9)
        assertEquals(106.8, JadwalHariIni.LON_DEFAULT, 1e-9)
        // Metode bawaan harus sama dengan jatuhan kunci tak dikenal, supaya "belum diatur" dan
        // "kunci rusak" menghasilkan jadwal yang sama.
        assertEquals(PrayerCalc.Method.KEMENAG, PrayerCalc.Method.from(JadwalHariIni.METODE_DEFAULT))
        assertEquals(
            PrayerCalc.Method.from("tidak-ada"),
            PrayerCalc.Method.from(JadwalHariIni.METODE_DEFAULT),
        )
    }

    @Test
    fun koreksiBawaanSejajarDenganEnamWaktu() {
        val corr = JadwalHariIni.parseCorr(JadwalHariIni.CORR_DEFAULT)
        assertEquals(JadwalHariIni.ENAM_WAKTU.size, corr.size)
        assertEquals(List(JadwalHariIni.ENAM_WAKTU.size) { 0 }, corr)
    }

    /**
     * Rentang koreksi dahulu hanya hidup sebagai batas slider di layar Jadwal. Nilai tersimpan yang
     * berada di luarnya (mis. dari backup yang disunting) harus dijepit di jalur baca juga, agar
     * penjadwal tidak memakai koreksi yang tak dapat dipilih pengguna.
     */
    @Test
    fun koreksiTersimpanDijepitKeRentang() {
        assertTrue(JadwalHariIni.KOREKSI_MIN_MENIT < 0)
        assertTrue(JadwalHariIni.KOREKSI_MAKS_MENIT > 0)
        val corr = JadwalHariIni.parseCorr("999,-999,30,-30,0,0")
        assertEquals(
            listOf(
                JadwalHariIni.KOREKSI_MAKS_MENIT, JadwalHariIni.KOREKSI_MIN_MENIT,
                JadwalHariIni.KOREKSI_MAKS_MENIT, JadwalHariIni.KOREKSI_MIN_MENIT, 0, 0,
            ),
            corr,
        )
    }

    @Test
    fun asarBawaanBukanHanafi() {
        // Jumhur (Syafi'i, Maliki, Hambali) memakai faktor 1; Hanafi adalah pilihan yang harus
        // dinyatakan pengguna, bukan bawaan diam-diam.
        assertFalse(JadwalHariIni.asarHanafi(JadwalHariIni.ASAR_DEFAULT))
        assertEquals(JadwalHariIni.ASAR_SYAFII, JadwalHariIni.ASAR_DEFAULT)
        assertTrue(JadwalHariIni.asarHanafi(JadwalHariIni.ASAR_HANAFI))
        assertFalse(JadwalHariIni.ASAR_SYAFII == JadwalHariIni.ASAR_HANAFI)
    }

    @Test
    fun zonaBawaanMengikutiPerangkat() {
        assertTrue(JadwalHariIni.TZ_DEFAULT.isBlank())
        assertEquals(TimeZone.getDefault(), JadwalHariIni.zona(JadwalHariIni.TZ_DEFAULT))
    }

    /**
     * Bawaan konfigurasi jadwal tak boleh ditulis ulang di tempat yang membacanya dari `Store`.
     *
     * Inilah penjaga sesungguhnya: sembilan tempat dahulu masing-masing mengetik koordinat, metode,
     * mazhab Asar, koreksi, dan zona waktunya sendiri. Uji ini gagal begitu ada yang kembali menulis
     * nilai mentah di luar [JadwalHariIni] — sebelum perbedaannya sempat menjadi dua jam yang tidak
     * sama untuk sholat yang sama.
     */
    @Test
    fun bawaanJadwalTidakDitulisUlang() {
        val pelanggar = mutableListOf<String>()
        val pola = listOf(
            Regex("""Store\.(LAT|LON)\s*,\s*-?\d"""),
            Regex("""Store\.(CITY|METHOD|ASAR|CORR|TZ)\s*,\s*""""),
        )
        File(root(), "src/main/java/id/or/kemenag/yasintahlil").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { f ->
                val isi = f.readText()
                pola.forEach { p -> p.findAll(isi).forEach { pelanggar += "${f.name}: ${it.value}" } }
            }
        assertTrue("Bawaan jadwal ditulis ulang di luar JadwalHariIni: $pelanggar", pelanggar.isEmpty())
    }

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }
}
