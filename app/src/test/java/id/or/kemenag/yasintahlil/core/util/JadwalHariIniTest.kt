package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
