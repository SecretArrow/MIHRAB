package id.or.kemenag.yasintahlil.feature.jadwal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.rememberRepo
import id.or.kemenag.yasintahlil.core.designsystem.rememberStore
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun JadwalScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val kota = remember { repo.kota() }
    var city by remember { mutableStateOf("Jakarta") }
    var lat by remember { mutableStateOf(-6.2) }
    var lon by remember { mutableStateOf(106.8) }
    var asarH by remember { mutableStateOf(false) }
    var corrTxt by remember { mutableStateOf("0,0,0,0,0,0") }
    var times by remember { mutableStateOf<PrayerTimes?>(null) }
    var nowMin by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        city = store.get(Store.CITY, "Jakarta")
        lat = store.get(Store.LAT, -6.2); lon = store.get(Store.LON, 106.8)
        asarH = store.get(Store.ASAR, "syafii") == "hanafi"
        corrTxt = store.get(Store.CORR, "0,0,0,0,0,0")
        recompute()
    }
    fun recompute() {
        val c = corrTxt.split(",").map { it.trim().toIntOrNull() ?: 0 } + List(6) { 0 }
        val now = Calendar.getInstance()
        nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        times = PrayerCalc.compute(now, PrayerCalc.Params(lat, lon,
            asarFactor = if (asarH) 2.0 else 1.0, corr = c.take(6)))
    }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Jadwal Sholat — metode Kemenag", style = MaterialTheme.typography.titleLarge)
        Text("Subuh −20° • Isya −18° • Asar ${if (asarH) "Hanafi (2)" else "Syafi'i (1)"} • ihtiyat +2 mnt. Verifikasi: sihat.kemenag.dev",
            style = MaterialTheme.typography.labelMedium)
        var drop by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { drop = !drop }) { Text("Kota: $city ▾") }
        if (drop) kota.forEach { k ->
            TextButton(onClick = {
                city = k.nama; lat = k.lat; lon = k.lon; drop = false; recompute()
                scope.launch { store.set(Store.CITY, city); store.set(Store.LAT, lat); store.set(Store.LON, lon) }
            }) { Text(k.nama) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!asarH, { asarH = false; recompute(); scope.launch { store.set(Store.ASAR, "syafii") } }, { Text("Asar Syafi'i") })
            FilterChip(asarH, { asarH = true; recompute(); scope.launch { store.set(Store.ASAR, "hanafi") } }, { Text("Asar Hanafi") })
        }
        OutlinedTextField(corrTxt, {
            corrTxt = it; scope.launch { store.set(Store.CORR, corrTxt) }
        }, label = { Text("Koreksi menit (subuh,terbit,zuhur,asar,magrib,isya)") },
            modifier = Modifier.fillMaxWidth())
        Button(onClick = { recompute() }) { Text("Hitung ulang") }
        times?.let { t ->
            val rows = listOf("Subuh" to t.subuh, "Terbit" to t.terbit, "Zuhur" to t.zuhur,
                "Asar" to t.asar, "Magrib" to t.magrib, "Isya" to t.isya)
            rows.forEach { (n, j) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(n, style = MaterialTheme.typography.titleMedium); Text(j, style = MaterialTheme.typography.titleMedium)
            } }
            val (berikut, sisa) = PrayerCalc.next(nowMin, t)
            Text("Berikutnya: $berikut dalam ${sisa / 60} jam ${sisa % 60} mnt",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Text("Cache offline: jadwal dihitung di perangkat, tetap tersedia tanpa internet.",
                style = MaterialTheme.typography.labelSmall)
        }
    }
}
