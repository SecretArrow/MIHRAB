package id.or.kemenag.yasintahlil.feature.ibadah

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun IbadahScreen(go: (String) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Menu Ibadah")
        Button(onClick = { go(Routes.YASIN) }) { Text("Yasin (83 ayat, MSI Kemenag)") }
        Button(onClick = { go(Routes.TAHLIL) }) { Text("Tahlil") }
        Button(onClick = { go(Routes.SHOLAT) }) { Text("Sholat Wajib & Sunnah") }
        Button(onClick = { go(Routes.GHAIB) }) { Text("Sholat Ghaib") }
        Button(onClick = { go(Routes.DZIKIR) }) { Text("Dzikir & Doa") }
    }
}
