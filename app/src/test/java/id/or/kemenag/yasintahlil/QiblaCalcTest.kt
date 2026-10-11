package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.*
import org.junit.Test

class QiblaCalcTest {
    @Test fun jakartaBearingSanity() {
        val b = QiblaCalc.bearing(-6.2, 106.8)
        assertTrue("Jakarta ~295°, got $b", b in 290.0..300.0)
    }
    @Test fun makassarBearingSanity() {
        val b = QiblaCalc.bearing(-5.14, 119.42)
        assertTrue("Makassar ~292°, got $b", b in 287.0..297.0)
    }
    @Test fun jayapuraBearingSanity() {
        val b = QiblaCalc.bearing(-2.53, 140.72)
        assertTrue("Jayapura ~291°, got $b", b in 286.0..296.0)
    }

    /** Berbagai lintang (termasuk kutub selatan, Eropa, dan titik Ka'bah) tetap sah. */
    @Test fun bearingSelaluDalamRentang() {
        val pts = listOf(
            -6.2 to 106.8, 0.0 to 109.3, 21.4225 to 39.8262,
            -33.87 to 151.21, 51.5 to -0.13, -64.15 to -21.94,
        )
        pts.forEach { (la, lo) ->
            val b = QiblaCalc.bearing(la, lo)
            assertTrue("bearing $la,$lo = $b di luar 0..360", b.isFinite() && b in 0.0..360.0)
        }
    }
}
