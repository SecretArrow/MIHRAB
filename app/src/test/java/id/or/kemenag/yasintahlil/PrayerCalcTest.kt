package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerCalcTest2 {
    private fun cal(y: Int, m: Int, d: Int) = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
        set(y, m - 1, d, 12, 0, 0); set(Calendar.MILLISECOND, 0)
    }

    @Test fun jakartaJan KisaranWajar() {
        val t = PrayerCalc.compute(cal(2026, 1, 15),
            PrayerCalc.Params(-6.2, 106.8, tz = TimeZone.getTimeZone("Asia/Jakarta")))
        val s = PrayerCalc.toMinutes(t.subuh)
        assertTrue("Subuh Jkt Jan ~04:20-04:45, got ${t.subuh}", s in 260..285)
        assertTrue("Magrib Jkt Jan ~18:05-18:25, got ${t.magrib}", PrayerCalc.toMinutes(t.magrib) in 1085..1105)
        assertTrue("Urutan waktu benar",
            s < PrayerCalc.toMinutes(t.terbit) && PrayerCalc.toMinutes(t.terbit) < PrayerCalc.toMinutes(t.zuhur) &&
                PrayerCalc.toMinutes(t.zuhur) < PrayerCalc.toMinutes(t.asar) &&
                PrayerCalc.toMinutes(t.asar) < PrayerCalc.toMinutes(t.magrib) &&
                PrayerCalc.toMinutes(t.magrib) < PrayerCalc.toMinutes(t.isya))
    }

    @Test fun asarHanafiLebihSiang() {
        val base = PrayerCalc.Params(-6.2, 106.8, tz = TimeZone.getTimeZone("Asia/Jakarta"))
        val sy = PrayerCalc.compute(cal(2026, 6, 15), base.copy(asarFactor = 1.0))
        val hf = PrayerCalc.compute(cal(2026, 6, 15), base.copy(asarFactor = 2.0))
        assertTrue(PrayerCalc.toMinutes(hf.asar) > PrayerCalc.toMinutes(sy.asar))
    }

    @Test fun nextSholat() {
        val t = PrayerCalc.compute(cal(2026, 1, 15),
            PrayerCalc.Params(-6.2, 106.8, tz = TimeZone.getTimeZone("Asia/Jakarta")))
        val (nama, sisa) = PrayerCalc.next(3 * 60, t) // jam 03:00
        assertEquals("Subuh", nama)
        assertTrue(sisa in 60..120)
    }
}
