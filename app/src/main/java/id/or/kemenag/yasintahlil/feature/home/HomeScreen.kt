package id.or.kemenag.yasintahlil.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.HijriCalc
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import id.or.kemenag.yasintahlil.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(go: (String) -> Unit) {
    val store = rememberStore()
    var city by remember { mutableStateOf("Jakarta") }
    var lat by remember { mutableStateOf(-6.2) }
    var lon by remember { mutableStateOf(106.8) }
    var asarH by remember { mutableStateOf(false) }
    var last by remember { mutableStateOf("") }
    var infoOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        city = store.get(Store.CITY, "Jakarta")
        lat = store.get(Store.LAT, -6.2); lon = store.get(Store.LON, 106.8)
        asarH = store.get(Store.ASAR, "syafii") == "hanafi"
        last = store.get(Store.LAST, "")
        infoOn = store.get(Store.NOTIF, true)
    }
    val now = Calendar.getInstance()
    val jam = SimpleDateFormat("HH:mm", Locale("id")).format(now.time)
    val masehi = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id")).format(now.time)
    val times = remember(lat, lon, asarH) {
        PrayerCalc.compute(now, PrayerCalc.Params(lat, lon, asarFactor = if (asarH) 2.0 else 1.0))
    }
    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val (berikut, sisa) = PrayerCalc.next(nowMin, times)
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Assalamu'alaikum", style = MaterialTheme.typography.titleLarge)
        Text("$masehi • $jam")
        Text(HijriCalc.format(now), style = MaterialTheme.typography.labelMedium)
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("$city — Berikutnya: $berikut ${times.let {
                    listOf(it.subuh, it.terbit, it.zuhur, it.asar, it.magrib, it.isya)[
                        listOf("Subuh", "Terbit", "Zuhur", "Asar", "Magrib", "Isya").indexOf(berikut)]
                }} (${sisa / 60}j ${sisa % 60}m lagi)", style = MaterialTheme.typography.titleMedium)
                Text("Subuh ${times.subuh} • Zuhur ${times.zuhur} • Asar ${times.asar} • Magrib ${times.magrib} • Isya ${times.isya}")
                Button(onClick = { go(Routes.JADWAL) }) { Text("Jadwal lengkap / ganti lokasi") }
            }
        }
        Text("Aksi cepat:", style = MaterialTheme.typography.titleMedium)
        listOf("Yasin" to Routes.YASIN, "Tahlil" to Routes.TAHLIL, "Panduan Sholat" to Routes.SHOLAT,
            "Sholat Ghaib" to Routes.GHAIB, "Adzan" to Routes.ADZAN, "Doa Adzan" to Routes.DOA_ADZAN,
            "Iqomah" to Routes.IQOMAH, "Kiblat" to Routes.KIBLAT, "Dzikir & Doa" to Routes.DZIKIR,
            "Cari" to Routes.SEARCH, "Favorit & Riwayat" to Routes.FAVORIT).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, r) -> Button(onClick = { go(r) }, modifier = Modifier.weight(1f)) { Text(label) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (last.isNotBlank()) Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Terakhir dibaca: $last")
                Button(onClick = { go(if (last.startsWith("yasin")) Routes.YASIN else Routes.FAVORIT) }) { Text("Lanjutkan") }
            }
        }
        if (infoOn) Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Info Islami harian", style = MaterialTheme.typography.titleMedium)
                Text("Perbanyak sholawat di hari Jumat (HR Abu Dawud). Sumber: sunnah.com")
            }
        }
    }
}
