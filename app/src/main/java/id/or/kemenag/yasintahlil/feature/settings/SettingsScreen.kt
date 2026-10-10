package id.or.kemenag.yasintahlil.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Pengaturan")
        Text("Tema, bahasa, font Arab/Latin, transliterasi, metode SIHAT Kemenag, lokasi.")
        Text("Tentang: teks LPMQ Kemenag, jadwal SIHAT Bimas Islam, hadis sunnah.com. https://quran.kemenag.go.id • https://sihat.kemenag.dev")
    }
}
