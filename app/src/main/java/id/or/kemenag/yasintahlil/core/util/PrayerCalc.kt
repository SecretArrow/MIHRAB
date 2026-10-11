package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Jadwal sholat akurat berbasis posisi matahari (algoritma Jean Meeus, Astronomical Algorithms).
 * Sudut default mengikuti metode Kemenag/SIHAT Bimas Islam (Subuh −20°, Isya −18°, ihtiyat +2 mnt);
 * metode lain (MWL, Umm al-Qura, Egyptian, Makkah) dapat dipilih pengguna.
 * Verifikasi: https://sihat.kemenag.dev (per kab/kota).
 */
object PrayerCalc {

    enum class Method(
        val label: String,
        val fajr: Double,
        val isha: Double,
        val ishaIntervalMin: Int?,
        val ihtiyat: Int,
    ) {
        KEMENAG("Kemenag/SIHAT (Indonesia)", 20.0, 18.0, null, 2),
        MWL("Muslim World League", 18.0, 17.0, null, 0),
        UMM_AL_QURA("Umm al-Qura (Makkah)", 18.5, 0.0, 90, 0),
        EGYPTIAN("Egyptian General Authority", 19.5, 17.5, null, 0),
        MAKKAH("Makkah", 18.5, 0.0, 90, 0);

        companion object {
            fun from(id: String?): Method =
                entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: KEMENAG
        }
    }

    data class Params(
        val lat: Double,
        val lon: Double,
        val method: Method = Method.KEMENAG,
        val asarFactor: Double = 1.0, // 1 Syafi'i, 2 Hanafi
        val corr: List<Int> = listOf(0, 0, 0, 0, 0, 0), // subuh,terbit,zuhur,asar,magrib,isya
        val tz: TimeZone = TimeZone.getDefault(),
    )

    /** Julian Day pada 0h UT untuk tanggal masehi. */
    private fun julianDay(y0: Int, m0: Int, d: Int): Double {
        var y = y0
        var m = m0
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    /** Deklinasi matahari (derajat) dan equation of time (menit) — Meeus (akurasi ~0,01°). */
    private fun sunPosition(days: Double): Pair<Double, Double> {
        val g = Math.toRadians((357.529 + 0.98560028 * days) % 360.0)
        val q = (280.459 + 0.98564736 * days) % 360.0
        val l = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360.0)
        val e = Math.toRadians(23.439 - 0.00000036 * days)
        val decl = asin(sin(e) * sin(l))
        val ra = Math.toDegrees(atan2(cos(e) * sin(l), cos(l))) / 15.0
        var eqt = q / 15.0 - ra
        eqt = ((eqt + 12) % 24 + 24) % 24 - 12
        return Math.toDegrees(decl) to eqt * 60.0
    }

    fun compute(date: Calendar = Calendar.getInstance(), p: Params): PrayerTimes {
        val jd = julianDay(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH))
        val days = jd - 2451545.0 + 0.5
        val (decl, eotMin) = sunPosition(days)
        val dr = Math.toRadians(decl)
        val lr = Math.toRadians(p.lat)
        fun hourAngle(altDeg: Double): Double {
            val alt = Math.toRadians(altDeg)
            val c = ((sin(alt) - sin(lr) * sin(dr)) / (cos(lr) * cos(dr))).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(c)) / 15.0
        }
        val tzHours = p.tz.getOffset(date.timeInMillis) / 3600000.0
        val noon = 12.0 + tzHours - p.lon / 15.0 - eotMin / 60.0
        val altAsar = Math.toDegrees(atan(1.0 / (p.asarFactor + tan(abs(lr - dr)))))
        val c = p.corr + List(6) { 0 }
        fun fmt(h: Double, corrMin: Int): String {
            var m = ((h * 60.0).roundToInt() + corrMin + p.method.ihtiyat) % 1440
            if (m < 0) m += 1440
            return "%02d:%02d".format(m / 60, m % 60)
        }
        val magrib = fmt(noon + hourAngle(-0.833), c[4])
        val isya = if (p.method.ishaIntervalMin != null) {
            fmt(noon + hourAngle(-0.833) + p.method.ishaIntervalMin / 60.0, c[5])
        } else {
            fmt(noon + hourAngle(-p.method.isha), c[5])
        }
        return PrayerTimes(
            fmt(noon - hourAngle(-p.method.fajr), c[0]),
            fmt(noon - hourAngle(-0.833), c[1]),
            fmt(noon, c[2]),
            fmt(noon + hourAngle(altAsar), c[3]),
            magrib,
            isya,
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
