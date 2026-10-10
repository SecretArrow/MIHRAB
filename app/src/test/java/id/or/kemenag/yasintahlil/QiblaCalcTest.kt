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
}
