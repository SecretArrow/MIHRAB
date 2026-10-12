package id.or.kemenag.yasintahlil

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.or.kemenag.yasintahlil.core.location.LocationProvider
import id.or.kemenag.yasintahlil.core.util.QiblaCalc
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * plan.txt: "Pengujian izin lokasi dan sensor tidak tersedia."
 *
 * Tanpa izin lokasi aplikasi harus tetap berguna: arah kiblat dihitung dari koordinat kota
 * yang dipilih manual, dan aliran lokasi berhenti sendiri alih-alih melempar error.
 * (Jalur tanpa kompas diuji di [ThemeAndScreenSizeTest] lewat peta skematik offline.)
 */
@RunWith(AndroidJUnit4::class)
class KiblatLocationPermissionTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        // Cabut izin agar jalur "tanpa izin" benar-benar teruji, bukan kebetulan sudah kosong.
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .forEach { runCatching { ui.revokeRuntimePermission(ctx.packageName, it) } }
    }

    @Test
    fun tanpaIzinTidakAdaPerbaikanLokasi() {
        assertFalse("izin lokasi seharusnya belum diberikan", LocationProvider.hasPermission(ctx))
        assertNull("lastKnown tidak boleh diisi tanpa izin", LocationProvider.lastKnown(ctx))
    }

    @Test
    fun aliranLokasiBerhentiSendiriTanpaIzin() {
        runBlocking {
            val hasil = withTimeoutOrNull(2_000) { LocationProvider.updates(ctx).toList() }
            assertNotNull("aliran lokasi menggantung tanpa izin", hasil)
            assertEquals("aliran lokasi harus langsung selesai tanpa izin", 0, hasil!!.size)
        }
    }

    @Test
    fun kiblatTetapTerhitungDariKotaManual() {
        // Jakarta — acuan umum azimut kiblat ~295°.
        val b = QiblaCalc.bearing(-6.2, 106.8)
        assertTrue("bearing $b di luar rentang wajar", b in 290.0..300.0)
        // Jarak ke Ka'bah tetap bisa ditampilkan tanpa GPS.
        assertTrue(QiblaCalc.distanceKm(-6.2, 106.8) > 0.0)
    }
}
