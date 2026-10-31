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

    /** Jarak lingkaran besar ke Ka'bah cocok dengan acuan umum (±1%). */
    @Test fun jarakKeKabahSesuaiAcuan() {
        // Jakarta ~7.915 km, Medan ~6.646 km, Ambon ~9.988 km (acuan haversine).
        val jakarta = QiblaCalc.distanceKm(-6.2, 106.8)
        assertTrue("Jakarta ~7915 km, got $jakarta", jakarta in 7830.0..8000.0)
        val medan = QiblaCalc.distanceKm(3.5952, 98.6722)
        assertTrue("Medan ~6646 km, got $medan", medan in 6570.0..6720.0)
        val ambon = QiblaCalc.distanceKm(-3.6954, 128.1814)
        assertTrue("Ambon ~9988 km, got $ambon", ambon in 9880.0..10100.0)
    }

    /** Di titik Ka'bah jaraknya nol dan tak ada busur untuk digambar. */
    @Test fun jarakNolDanBusurTunggalDiKabah() {
        assertEquals(0.0, QiblaCalc.distanceKm(QiblaCalc.KABAH_LAT, QiblaCalc.KABAH_LON), 1e-6)
        val p = QiblaCalc.greatCirclePath(QiblaCalc.KABAH_LAT, QiblaCalc.KABAH_LON)
        assertEquals("Busur di Ka'bah harus satu titik", 1, p.size)
    }

    /** Rute lingkaran besar: berawal di titik pengguna, berakhir di Ka'bah, dan terus mendekat. */
    @Test fun ruteLingkaranBesarUtuhDanMonoton() {
        val (la, lo) = -6.2 to 106.8
        val path = QiblaCalc.greatCirclePath(la, lo, 96)
        assertEquals("Lintang awal bukan titik pengguna", la, path.first().first, 1e-9)
        assertEquals("Bujur awal bukan titik pengguna", lo, path.first().second, 1e-9)
        val end = path.last()
        assertEquals(QiblaCalc.KABAH_LAT, end.first, 1e-6)
        assertEquals(QiblaCalc.KABAH_LON, end.second, 1e-6)

        var prev = Double.MAX_VALUE
        path.forEach { (pla, plo) ->
            val d = QiblaCalc.distanceKm(pla, plo)
            assertTrue("Jarak tidak monoton menurun: $d setelah $prev", d <= prev + 1e-6)
            prev = d
        }
    }

    /** Rute melintasi garis tanggal (Alaska → Makkah) tetap menghasilkan bujur yang sah. */
    @Test fun ruteLintasGarisTanggalTetapSah() {
        val path = QiblaCalc.greatCirclePath(64.2, -149.5, 48)
        assertEquals(49, path.size)
        path.forEach { (la, lo) ->
            assertTrue("lintang $la di luar rentang", la in -90.0..90.0)
            assertTrue("bujur $lo di luar rentang", lo in -180.0..180.0)
        }
    }
}
