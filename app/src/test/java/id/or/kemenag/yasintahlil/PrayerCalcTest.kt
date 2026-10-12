package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerCalcTest {

    private val jakarta = TimeZone.getTimeZone("Asia/Jakarta")

    private fun cal(y: Int, m: Int, d: Int, tz: TimeZone = jakarta) =
        Calendar.getInstance(tz).apply { set(y, m - 1, d, 12, 0, 0); set(Calendar.MILLISECOND, 0) }

    @Test
    fun jakartaJanuariNilaiAkurat() {
        val t = PrayerCalc.compute(cal(2026, 1, 15), PrayerCalc.Params(-6.2, 106.8, tz = jakarta))
        // Nilai acuan metode Kemenag/SIHAT (toleransi ±3 menit).
        assertTrue("Subuh ${t.subuh}", PrayerCalc.toMinutes(t.subuh) in (4 * 60 + 24)..(4 * 60 + 30))
        assertTrue("Zuhur ${t.zuhur}", PrayerCalc.toMinutes(t.zuhur) in (12 * 60 + 1)..(12 * 60 + 7))
        assertTrue("Magrib ${t.magrib}", PrayerCalc.toMinutes(t.magrib) in (18 * 60 + 14)..(18 * 60 + 20))
        assertTrue("Isya ${t.isya}", PrayerCalc.toMinutes(t.isya) in (19 * 60 + 29)..(19 * 60 + 35))
    }

    @Test
    fun urutanWaktuBenarDiBanyakKota() {
        val cities = listOf(
            "Jakarta" to Triple(-6.2, 106.8, jakarta),
            "Surabaya" to Triple(-7.25, 112.75, jakarta),
            "Medan" to Triple(3.59, 98.67, jakarta),
            "Makassar" to Triple(-5.15, 119.43, TimeZone.getTimeZone("Asia/Makassar")),
            "Jayapura" to Triple(-2.53, 140.72, TimeZone.getTimeZone("Asia/Jayapura")),
        )
        cities.forEach { (name, v) ->
            val (lat, lon, tz) = v
            val t = PrayerCalc.compute(cal(2026, 3, 21, tz), PrayerCalc.Params(lat, lon, tz = tz))
            val m = listOf(t.subuh, t.terbit, t.zuhur, t.asar, t.magrib, t.isya).map { PrayerCalc.toMinutes(it) }
            assertTrue("$name: urutan waktu harus naik $m", m.zipWithNext().all { (a, b) -> a < b })
        }
    }

    @Test
    fun asarHanafiLebihSiang() {
        val base = PrayerCalc.Params(-6.2, 106.8, tz = jakarta)
        val sy = PrayerCalc.compute(cal(2026, 6, 15), base.copy(asarFactor = 1.0))
        val hf = PrayerCalc.compute(cal(2026, 6, 15), base.copy(asarFactor = 2.0))
        assertTrue(PrayerCalc.toMinutes(hf.asar) > PrayerCalc.toMinutes(sy.asar))
    }

    @Test
    fun metodeMengubahSubuhDanIsya() {
        val base = PrayerCalc.Params(-6.2, 106.8, tz = jakarta)
        val kemenag = PrayerCalc.compute(cal(2026, 1, 15), base.copy(method = PrayerCalc.Method.KEMENAG))
        val mwl = PrayerCalc.compute(cal(2026, 1, 15), base.copy(method = PrayerCalc.Method.MWL))
        // Sudut Fajr MWL (18°) lebih kecil dari Kemenag (20°) → Subuh lebih siang.
        assertTrue(PrayerCalc.toMinutes(mwl.subuh) > PrayerCalc.toMinutes(kemenag.subuh))
    }

    @Test
    fun ummAlQuraIsyaSembilanPuluhMenitSetelahMagrib() {
        val t = PrayerCalc.compute(cal(2026, 1, 15),
            PrayerCalc.Params(-6.2, 106.8, method = PrayerCalc.Method.UMM_AL_QURA, tz = jakarta))
        assertEquals(90, PrayerCalc.toMinutes(t.isya) - PrayerCalc.toMinutes(t.magrib))
    }

    @Test
    fun koreksiMenitDiterapkan() {
        val base = PrayerCalc.Params(-6.2, 106.8, tz = jakarta)
        val plain = PrayerCalc.compute(cal(2026, 1, 15), base)
        val plus5 = PrayerCalc.compute(cal(2026, 1, 15), base.copy(corr = listOf(5, 0, 0, 0, 0, 0)))
        assertEquals(5, PrayerCalc.toMinutes(plus5.subuh) - PrayerCalc.toMinutes(plain.subuh))
    }

    @Test
    fun zonaWaktuMenggeserJam() {
        val base = PrayerCalc.Params(-6.2, 106.8)
        val wib = PrayerCalc.compute(cal(2026, 1, 15, jakarta), base.copy(tz = jakarta))
        val wita = PrayerCalc.compute(cal(2026, 1, 15, TimeZone.getTimeZone("Asia/Makassar")),
            base.copy(tz = TimeZone.getTimeZone("Asia/Makassar")))
        // WITA = WIB + 1 jam.
        assertEquals(60, PrayerCalc.toMinutes(wita.zuhur) - PrayerCalc.toMinutes(wib.zuhur))
    }

    @Test
    fun ekuatorWaktuUrutDanFormatValid() {
        val tz = TimeZone.getTimeZone("Asia/Pontianak")
        val t = PrayerCalc.compute(cal(2026, 3, 21, tz), PrayerCalc.Params(0.0, 109.33, tz = tz))
        // Urutan field `PrayerTimes` diambil dari daftar bersama (`JadwalHariIni.semua`), bukan
        // ditulis ulang di sini: satu daftar nama yang salah urut akan lolos dari uji ini kalau uji
        // ini memakai salinannya sendiri.
        val m = JadwalHariIni.semua(t).map { PrayerCalc.toMinutes(it) }
        assertTrue("Ekuator: urutan waktu harus naik $m", m.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun lintangTinggiTetapMenghasilkanFormatValid() {
        // Di lintang tinggi, Subuh/Isya bisa tak tercapai (sudut matahari tak dilewati).
        // Hasil harus tetap "HH:MM" — tanpa NaN/krash.
        val tz = TimeZone.getTimeZone("Atlantic/Reykjavik")
        val t = PrayerCalc.compute(cal(2026, 6, 21, tz), PrayerCalc.Params(64.15, -21.94, tz = tz))
        listOf(t.subuh, t.terbit, t.zuhur, t.asar, t.magrib, t.isya).forEach {
            assertTrue("Format waktu tidak sah: '$it'", Regex("^\\d{2}:\\d{2}$").matches(it))
        }
    }
}
