package id.or.kemenag.yasintahlil

import android.Manifest
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
import id.or.kemenag.yasintahlil.core.scheduling.NotifChannels
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.navigation.AppNav
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val theme by vm.theme.collectAsStateWithLifecycle()
            val dark = when (theme) {
                "terang" -> false
                "gelap" -> true
                else -> isSystemInDarkTheme()
            }
            IslamiTheme(dark = dark) { AppNav() }
        }
    }
}
