package id.or.kemenag.yasintahlil.feature.yasin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun YasinScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Surat Yasin — 83 ayat")
        Text("Sumber: Mushaf Standar Indonesia, LPMQ Kemenag RI (quran.kemenag.go.id). Full teks dimuat offline dari assets/content/yasin.json")
    }
}
