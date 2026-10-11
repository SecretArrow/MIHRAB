package id.or.kemenag.yasintahlil.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var theme by remember { mutableStateOf("sistem") }
    var arab by remember { mutableStateOf(24f) }
    var latin by remember { mutableStateOf(16f) }
    var showL by remember { mutableStateOf(true) }
    var showT by remember { mutableStateOf(true) }
    var asar by remember { mutableStateOf("syafii") }
    var city by remember { mutableStateOf("Jakarta") }
    var notif by remember { mutableStateOf(true) }
    var backup by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        theme = store.get(Store.DARK, "sistem")
        arab = store.get(Store.ARAB_SIZE, 24f); latin = store.get(Store.LATIN_SIZE, 16f)
        showL = store.get(Store.SHOW_LATIN, true); showT = store.get(Store.SHOW_TRANS, true)
        asar = store.get(Store.ASAR, "syafii"); city = store.get(Store.CITY, "Jakarta")
        notif = store.get(Store.NOTIF, true)
    }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Pengaturan", style = MaterialTheme.typography.titleLarge)
        Text("Tema:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("terang", "gelap", "sistem").forEach { t ->
                FilterChip(theme == t, { theme = t; scope.launch { store.set(Store.DARK, t) } }, { Text(t) })
            }
        }
        Text("Ukuran Arab: ${arab.toInt()}"); Slider(arab, { arab = it; scope.launch { store.set(Store.ARAB_SIZE, arab) } }, valueRange = 16f..40f)
        Text("Ukuran Latin: ${latin.toInt()}"); Slider(latin, { latin = it; scope.launch { store.set(Store.LATIN_SIZE, latin) } }, valueRange = 12f..24f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(showL, { showL = !showL; scope.launch { store.set(Store.SHOW_LATIN, showL) } }, { Text("Latin") })
            FilterChip(showT, { showT = !showT; scope.launch { store.set(Store.SHOW_TRANS, showT) } }, { Text("Terjemahan") })
        }
        Text("Mazhab Asar:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(asar == "syafii", { asar = "syafii"; scope.launch { store.set(Store.ASAR, asar) } }, { Text("Syafi'i") })
            FilterChip(asar == "hanafi", { asar = "hanafi"; scope.launch { store.set(Store.ASAR, asar) } }, { Text("Hanafi") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pengingat ibadah"); Switch(notif, { notif = it; scope.launch { store.set(Store.NOTIF, notif) } })
        }
        Text("Backup / pulihkan data lokal:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { backup = store.exportJson(); ctx.copyText("backup", backup) } }) { Text("Salin backup") }
            Button(onClick = { scope.launch { store.importJson(backup) } }) { Text("Pulihkan") }
        }
        OutlinedTextField(backup, { backup = it }, label = { Text("Data backup") }, modifier = Modifier.fillMaxWidth())
        Text("Kota: $city (ubah di menu Jadwal). Susunan Tahlil: lazim Indonesia (lihat modul Tahlil).",
            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
        Text("Tentang & sumber", style = MaterialTheme.typography.titleMedium)
        Text("Teks Qur'an: LPMQ Kemenag MSI (quran.kemenag.go.id). Jadwal: metode SIHAT Bimas Islam (sihat.kemenag.dev). Kiblat: SIHAT E-Kiblat. Hadis: sunnah.com. Izin: lokasi (kiblat/jadwal), notifikasi (pengingat). Data tersimpan lokal; lokasi tidak dikirim ke server mana pun.")
    }
}
