package id.or.kemenag.yasintahlil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.or.kemenag.yasintahlil.navigation.AppNav
import id.or.kemenag.yasintahlil.core.designsystem.IslamiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IslamiTheme { AppNav() } }
    }
}
