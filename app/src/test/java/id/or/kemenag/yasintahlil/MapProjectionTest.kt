package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Peta kiblat harus tetap benar di berbagai lokasi, termasuk yang rutenya melintasi
 * garis tanggal. Proyeksi diuji murni (tanpa Android).
 */
class MapProjectionTest {

    @Test fun bukaLipatanBujurMenghapusLompatan360() {
        // Alaska (-149.5) → Makkah (39.8) melintasi garis tanggal.
        val raw = listOf(-149.5, 171.2, -178.8, 39.8)
        val out = MapProjection.unwrapLongitudes(raw)
        assertEquals(raw.size, out.size)
        out.zipWithNext { a, b ->
            assertTrue("Lompatan bujur ${b - a} pada $a→$b", abs(b - a) <= 180.0)
        }
    }

    @Test fun kotakBatasMemberiPadding() {
        val b = MapProjection.bounds(listOf(-6.2 to 106.8, 21.4225 to 39.8262), 4.0)
        assertTrue(b.minLat < -6.2 && b.maxLat > 21.4225)
        assertTrue(b.minLon < 39.8262 && b.maxLon > 106.8)
    }

    @Test fun utaraBeradaDiAtas() {
        val b = MapProjection.bounds(listOf(-6.2 to 106.8, 21.4225 to 39.8262), 4.0)
        val (_, yUtara) = MapProjection.project(b.maxLat, b.minLon, b)
        val (_, ySelatan) = MapProjection.project(b.minLat, b.minLon, b)
        assertEquals("Tepi utara harus y=0", 0f, yUtara, 1e-4f)
        assertEquals("Tepi selatan harus y=1", 1f, ySelatan, 1e-4f)
    }

    @Test fun semuaTitikMasukKeDalamKotak() {
        val user = -6.2 to 106.8
        val path = QiblaCalc.greatCirclePath(user.first, user.second, 64)
        val lons = MapProjection.unwrapLongitudes(path.map { it.second })
        val pts = path.map { it.first }.zip(lons)
        val b = MapProjection.bounds(pts, 4.0)
        val aspect = MapProjection.aspect(b)
        assertTrue("aspect harus positif", aspect > 0f)

        pts.forEach { (la, lo) ->
            val (x, y) = MapProjection.project(la, lo, b)
            assertTrue("x=$x di luar 0..$aspect", x >= -1e-4f && x <= aspect + 1e-4f)
            assertTrue("y=$y di luar 0..1", y >= -1e-4f && y <= 1f + 1e-4f)
        }
    }
}
