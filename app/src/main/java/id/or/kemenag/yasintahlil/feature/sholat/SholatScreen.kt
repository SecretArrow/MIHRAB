package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun SholatScreen(go: (String) -> Unit) {
    val repo = rememberRepo()
    val wajib = remember { repo.sholatWajib() }
    val sunnah = remember { repo.sholatSunnah() }
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Panduan Sholat (Fasolatan Kemenag)", style = MaterialTheme.typography.titleLarge)
        TabRow(tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Wajib") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Sunnah") })
            Tab(tab == 2, { tab = 2 }, text = { Text("Bacaan") })
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (tab == 0) items(wajib) { s ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(s.nama, style = MaterialTheme.typography.titleMedium)
                        Text("${s.rakaat} • ${s.waktu}")
                        Text(s.deskripsi)
                        Row {
                            if (s.id == "ghaib") Button(onClick = { go(Routes.GHAIB) }) { Text("Buka panduan Ghaib") }
                            else if (s.id == "jenazah") Button(onClick = { go(Routes.JENAZAH) }) { Text("Buka panduan") }
                            else Button(onClick = { go("${Routes.SHOLAT_DETAIL}/${s.id}") }) { Text("Tata cara") }
                        }
                    }
                }
            }
            if (tab == 1) items(sunnah) { s ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(s.nama, style = MaterialTheme.typography.titleMedium)
                        Text("${s.rakaat} • ${s.waktu}")
                        Text(s.deskripsi)
                        Button(onClick = { go("${Routes.SHOLAT_DETAIL}/${s.id}") }) { Text("Tata cara") }
                    }
                }
            }
            if (tab == 2) {
                val bacaan = remember { repo.sholatBacaan() }
                items(bacaan) { b ->
                    val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
                        Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
                    }
                    BacaanCard("bacaan:${b.id}", b.judul, a, l, t, true, true, 22f, 15f, sumber = b.sumber.ifBlank { b.kapan })
                }
            }
        }
    }
}
