package id.or.kemenag.yasintahlil.feature.alat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun AlatScreen(go: (String) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Menu Alat")
        Button(onClick = { go(Routes.KIBLAT) }) { Text("Kompas Kiblat (SIHAT)") }
        Button(onClick = { go(Routes.JADWAL) }) { Text("Jadwal Sholat (SIHAT)") }
        Button(onClick = { go(Routes.ADZAN) }) { Text("Adzan & Pengingat") }
    }
}
