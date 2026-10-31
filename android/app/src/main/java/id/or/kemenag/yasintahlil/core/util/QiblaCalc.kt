package id.or.kemenag.yasintahlil.core.util

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Azimut kiblat: busur terpendek dari utara sejati menuju Ka'bah.
// Ka'bah resmi SIHAT Kemenag: 21.422485, 39.826206 (21°25'21.0"U 39°49'34.3"T)
object QiblaCalc {
    const val KABAH_LAT = 21.422485
    const val KABAH_LON = 39.826206

    /** Jari-jari rata-rata bumi (km) — IUGG. */
    private const val EARTH_RADIUS_KM = 6371.0088

    fun bearing(lat: Double, lon: Double): Double {
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(KABAH_LAT)
        val dLon = Math.toRadians(KABAH_LON - lon)
        val y = sin(dLon)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** Sudut pusat (radian) antara titik dan Ka'bah — dasar jarak & interpolasi. */
    private fun centralAngle(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KABAH_LAT)
        val dLon = Math.toRadians(KABAH_LON - lon)
        val h = sin((p2 - p1) / 2).let { it * it } +
            cos(p1) * cos(p2) * sin(dLon / 2).let { it * it }
        return 2 * asin(sqrt(h).coerceIn(0.0, 1.0))
    }

    /** Jarak lingkaran besar (km) dari titik ke Ka'bah. */
    fun distanceKm(lat: Double, lon: Double): Double = EARTH_RADIUS_KM * centralAngle(lat, lon)

    /**
     * Titik-titik di sepanjang lingkaran besar dari (lat, lon) menuju Ka'bah, inklusif
     * kedua ujungnya — dipakai menggambar rute pada peta. Interpolasi bola (slerp).
     */
    fun greatCirclePath(lat: Double, lon: Double, steps: Int = 64): List<Pair<Double, Double>> {
        val n = steps.coerceAtLeast(1)
        val d = centralAngle(lat, lon)
        // Titik berimpit dengan Ka'bah: tak ada busur untuk digambar.
        if (d < 1e-9) return listOf(lat to lon)
        val p1 = Math.toRadians(lat)
        val l1 = Math.toRadians(lon)
        val p2 = Math.toRadians(KABAH_LAT)
        val l2 = Math.toRadians(KABAH_LON)
        return (0..n).map { i ->
            val f = i.toDouble() / n
            val a = sin((1 - f) * d) / sin(d)
            val b = sin(f * d) / sin(d)
            val x = a * cos(p1) * cos(l1) + b * cos(p2) * cos(l2)
            val y = a * cos(p1) * sin(l1) + b * cos(p2) * sin(l2)
            val z = a * sin(p1) + b * sin(p2)
            Math.toDegrees(atan2(z, sqrt(x * x + y * y))) to Math.toDegrees(atan2(y, x))
        }
    }
}
