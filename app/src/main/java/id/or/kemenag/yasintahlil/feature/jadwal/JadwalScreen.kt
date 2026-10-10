package id.or.kemenag.yasintahlil.feature.jadwal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JadwalScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Jadwal Sholat — metode baku SIHAT Kemenag (sihat.kemenag.dev)")
        Text("Online: API SIHAT per kab/kota. Offline: cache 30 hari + perkiraan lokal berlabel.")
    }
}
