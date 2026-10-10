package id.or.kemenag.yasintahlil.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun HomeScreen(go: (String) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Assalamu'alaikum — Yasin dan Tahlil")
        Text("Sumber: LPMQ Kemenag (MSI) • Jadwal: SIHAT Bimas Islam")
        Button(onClick = { go(Routes.YASIN) }) { Text("Buka Yasin") }
        Button(onClick = { go(Routes.TAHLIL) }) { Text("Buka Tahlil") }
        Button(onClick = { go(Routes.SHOLAT) }) { Text("Panduan Sholat") }
        Button(onClick = { go(Routes.KIBLAT) }) { Text("Arah Kiblat") }
        Button(onClick = { go(Routes.JADWAL) }) { Text("Jadwal Sholat") }
    }
}
