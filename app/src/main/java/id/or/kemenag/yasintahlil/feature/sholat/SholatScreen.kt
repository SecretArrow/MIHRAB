package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun SholatScreen(go: (String) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Panduan Sholat (Fasolatan Kemenag)")
        Button(onClick = { go(Routes.JENAZAH) }) { Text("Sholat Jenazah (hadir)") }
        Button(onClick = { go(Routes.GHAIB) }) { Text("Sholat Ghaib (fardhu kifayah)") }
        Text("Wajib: Subuh, Zuhur, Asar, Magrib, Isya, Jumat. Sunnah: rawatib, dhuha, tahajud, witir, tarawih, dll.")
    }
}
