package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * Jadwal sholat metode baku Kemenag (SIHAT/Bimas Islam):
 * Subuh -20°, Isya -18°, Asar Syafi'i faktor 1 (opsional Hanafi 2),
 * Magrib saat matahari terbenam, ihtiyat +2 menit.
 * Untuk produksi harian: verifikasi dengan https://sihat.kemenag.dev (per kab/kota).
 */
object PrayerCalc {
    data class Params(
        val lat: Double, val lon: Double,
        val fajrAngle: Double = 20.0, val ishaAngle: Double = 18.0,
        val asarFactor: Double = 1.0, // 1 Syafi'i, 2 Hanafi
        val corr: List<Int> = listOf(0, 0, 0, 0, 0, 0), // subuh,terbit,zuhur,asar,magrib,isya
        val tz: TimeZone = TimeZone.getDefault()
    )

    fun compute(date: Calendar = Calendar.getInstance(), p: Params): PrayerTimes {
        val n = date.get(Calendar.DAY_OF_YEAR)
        val lngHour = p.lon / 15.0
        // equation of time + declination (aproksimasi standar)
        val b = 2 * PI * (n - 81) / 364.0
        val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        val decl = 23.45 * sin(2 * PI * (284 + n) / 365.0)
        val dRad = Math.toRadians(decl)
        val latRad = Math.toRadians(p.lat)
        fun hourAngle(altDeg: Double): Double {
            val alt = Math.toRadians(altDeg)
            var cosH = (sin(alt) - sin(latRad) * sin(dRad)) / (cos(latRad) * cos(dRad))
            cosH = cosH.coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosH)) / 15.0
        }
        val tzo = p.tz.getOffset(date.timeInMillis) / 3600000.0
        val noon = 12.0 + tzo - lngHour - eot / 60.0
        val altAsar = Math.toDegrees(atan(1.0 / (p.asarFactor + tan(abs(latRad - dRad)))))
        fun fmt(h: Double, corrMin: Int): String {
            var m = ((h * 60.0).roundToInt() + corrMin + 2) % 1440 // ihtiyat 2 mnt
            if (m < 0) m += 1440
            return "%02d:%02d".format(m / 60, m % 60)
        }
        val c = p.corr + List(6) { 0 }
        return PrayerTimes(
            fmt(noon - hourAngle(-p.fajrAngle), c[0]),
            fmt(noon - hourAngle(-0.833), c[1]),
            fmt(noon, c[2]),
            fmt(noon + hourAngle(altAsar), c[3]),
            fmt(noon + hourAngle(-0.833), c[4]),
            fmt(noon + hourAngle(-p.ishaAngle), c[5])
        )
    }

    fun toMinutes(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return h * 60 + m
    }

    /** Nama sholat berikutnya + menit menuju. */
    fun next(nowMin: Int, t: PrayerTimes): Pair<String, Int> {
        val order = listOf("Subuh" to t.subuh, "Terbit" to t.terbit, "Zuhur" to t.zuhur,
            "Asar" to t.asar, "Magrib" to t.magrib, "Isya" to t.isya)
        for ((name, hhmm) in order) {
            val d = toMinutes(hhmm) - nowMin
            if (d > 0) return name to d
        }
        return "Subuh" to (toMinutes(t.subuh) + 1440 - nowMin)
    }
}
