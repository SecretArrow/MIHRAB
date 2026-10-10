package id.or.kemenag.yasintahlil.feature.tahlil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TahlilScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Tahlil — susunan dapat berbeda menurut tradisi. Bukan satu-satunya versi yang benar.")
        Text("Sumber bacaan Qur'an: LPMQ Kemenag; doa: kutubussittah via sunnah.com.")
    }
}
