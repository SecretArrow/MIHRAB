package id.or.kemenag.yasintahlil.feature.adzan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import kotlinx.coroutines.launch

@Composable
fun IqomahScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val items = remember { repo.iqomah() }
    var mins by remember { mutableStateOf(10) }
    var fullscreen by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(24f) }
    LaunchedEffect(Unit) { mins = store.get(Store.IQOMAH_MIN, 10) }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Iqomah", style = MaterialTheme.typography.titleLarge)
        Text("Timer opsional setelah masuk waktu (± $mins mnt). Ini pengingat pribadi — waktu iqomah tiap masjid berbeda.",
            style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { mins = (mins - 1).coerceAtLeast(0); scope.launch { store.set(Store.IQOMAH_MIN, mins) } }) { Text("−") }
            Text("$mins mnt", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { mins = (mins + 1).coerceAtMost(60); scope.launch { store.set(Store.IQOMAH_MIN, mins) } }) { Text("+") }
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh") })
        }
        items.forEach { l ->
            BacaanCard("iqomah:${l.arab.take(10)}", "Iqomah", l.arab, l.latin, l.terjemahan,
                true, true, if (fullscreen) arabSize + 8 else arabSize, 15f, l.ulangi)
        }
    }
}

@Composable
fun DoaAdzanScreen() {
    val repo = rememberRepo()
    val (arab, latin, trans) = remember { repo.doaAdzan() }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Doa sebelum & sesudah adzan", style = MaterialTheme.typography.titleLarge)
        Text("Tidak ada satu doa khusus yang wajib tepat sebelum adzan. Yang disyariatkan: menjawab adzan, sholawat, lalu doa setelah adzan.",
            style = MaterialTheme.typography.labelMedium)
        BacaanCard("jawab:adzan", "Menjawab muadzin",
            "Menjawab seperti lafaz muadzin; saat حي على الصلاة / حي على الفلاح membaca: لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ",
            "La haula wa la quwwata illa billah.",
            "Tidak ada daya dan kekuatan kecuali dengan Allah.",
            true, true, 22f, 15f, sumber = "HR Bukhari-Muslim — sunnah.com")
        BacaanCard("doa:adzan", "Doa setelah adzan", arab, latin, trans,
            true, true, 22f, 15f, sumber = "HR Bukhari — sunnah.com")
        Text("Aktifkan pengingat membaca doa setelah adzan di menu Adzan.",
            style = MaterialTheme.typography.labelSmall)
    }
}
