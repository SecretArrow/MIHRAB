package id.or.kemenag.yasintahlil

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import id.or.kemenag.yasintahlil.core.designsystem.IslamiTheme
import id.or.kemenag.yasintahlil.core.designsystem.ProvideTemaHalaman
import id.or.kemenag.yasintahlil.core.scheduling.NotifChannels
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.navigation.AppNav
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Rute yang diminta dari luar layar (mis. ketukan notifikasi "Baca doa setelah adzan").
     * Diset dari intent pembuka maupun [onNewIntent], lalu dikosongkan setelah dibuka.
     */
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        lifecycleScope.launch {
            NotifChannels.ensure(applicationContext)
            // Susun ulang pengingat sesuai lokasi/metode/koreksi terkini.
            PrayerScheduler.scheduleAll(applicationContext)
        }
        pendingRoute.value = routeOf(intent)
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val theme by vm.theme.collectAsStateWithLifecycle()
            val dark = when (theme) {
                "terang" -> false
                "gelap" -> true
                else -> isSystemInDarkTheme()
            }
            val route by pendingRoute.collectAsStateWithLifecycle()
            val pageTheme by vm.pageTheme.collectAsStateWithLifecycle()
            IslamiTheme(dark = dark) {
                ProvideTemaHalaman(pageTheme) {
                    AppNav(pendingRoute = route, onRouteConsumed = { pendingRoute.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = routeOf(intent)
    }

    private fun routeOf(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_ROUTE)?.takeIf { it.isNotBlank() }

    companion object {
        /** Nama extra intent berisi rute tujuan (lihat [Routes.LANGSUNG]). */
        const val EXTRA_ROUTE = "rute"
    }
}
