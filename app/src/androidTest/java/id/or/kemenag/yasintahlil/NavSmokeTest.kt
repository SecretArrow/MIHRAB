package id.or.kemenag.yasintahlil

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavSmokeTest {
    @Test fun routesExist() {
        assertEquals("home", id.or.kemenag.yasintahlil.navigation.Routes.HOME)
        assertEquals("sholat/ghaib", id.or.kemenag.yasintahlil.navigation.Routes.GHAIB)
    }
}
