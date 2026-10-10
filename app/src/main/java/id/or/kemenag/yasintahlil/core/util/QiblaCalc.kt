package id.or.kemenag.yasintahlil.core.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Azimut kiblat: busur terpendek dari utara sejati menuju Ka'bah.
// Ka'bah resmi SIHAT Kemenag: 21.422485, 39.826206 (21°25'21.0"U 39°49'34.3"T)
object QiblaCalc {
    const val KABAH_LAT = 21.422485
    const val KABAH_LON = 39.826206

    fun bearing(lat: Double, lon: Double): Double {
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(KABAH_LAT)
        val dLon = Math.toRadians(KABAH_LON - lon)
        val y = sin(dLon)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }
}
