package id.or.kemenag.yasintahlil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import id.or.kemenag.yasintahlil.core.data.ReminderWorker
import id.or.kemenag.yasintahlil.core.designsystem.IslamiTheme
import id.or.kemenag.yasintahlil.navigation.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ReminderWorker.scheduleToday(this)
        setContent { IslamiTheme { AppNav() } }
    }
}
