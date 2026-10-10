package id.or.kemenag.yasintahlil.feature.dzikir

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DzikirScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Dzikir & Doa — Arab + Latin + ID, favorit + counter, fullscreen.")
        Text("Sumber: LPMQ Kemenag (ayat) + sunnah.com (hadis). Lihat SOURCES.md.")
    }
}
