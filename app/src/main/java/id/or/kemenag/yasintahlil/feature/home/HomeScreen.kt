package id.or.kemenag.yasintahlil.feature.home

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
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.HijriCalc
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(go: (String) -> Unit) {
    val store = rememberStore()
    val userData = rememberUserData()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var city by remember { mutableStateOf("Jakarta") }
    var lat by remember { mutableStateOf(-6.2) }
    var lon by remember { mutableStateOf(106.8) }
    var asarH by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf(PrayerCalc.Method.KEMENAG) }
    var tzId by remember { mutableStateOf("") }
    var corr by remember { mutableStateOf(List(6) { 0 }) }
    var last by remember { mutableStateOf("") }
    var infoOn by remember { mutableStateOf(true) }
    var hijriCorr by remember { mutableStateOf(0) }
    var notif by remember { mutableStateOf(true) }
    var favCount by remember { mutableStateOf(0) }
    var histCount by remember { mutableStateOf(0) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        city = store.get(Store.CITY, "Jakarta")
        lat = store.get(Store.LAT, -6.2); lon = store.get(Store.LON, 106.8)
        asarH = store.get(Store.ASAR, "syafii") == "hanafi"
        method = PrayerCalc.Method.from(store.get(Store.METHOD, "KEMENAG"))
        tzId = store.get(Store.TZ, "")
        corr = (store.get(Store.CORR, "0,0,0,0,0,0").split(",").map { it.trim().toIntOrNull() ?: 0 } + List(6) { 0 }).take(6)
        last = store.get(Store.LAST, "")
        hijriCorr = store.get(Store.HIJRI_CORR, 0)
        notif = store.get(Store.NOTIF, true)
    }
    // Reaktif: kartu info langsung hilang/muncul saat diubah di Pengaturan.
    LaunchedEffect(Unit) { store.flow(Store.INFO_CARD, true).collect { infoOn = it } }
    // Ringkasan favorit & riwayat (Room) — ikut terbarui saat berubah di layar lain.
    LaunchedEffect(Unit) { userData.favorites().collect { favCount = it.size } }
    LaunchedEffect(Unit) { userData.recentHistory(50).collect { histCount = it.size } }
    // Hitung mundur live (perbarui tiap detik).
    LaunchedEffect(Unit) { while (true) { delay(1_000); tick = System.currentTimeMillis() } }

    val tz = remember(tzId) { if (tzId.isBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(tzId) }
    val now = remember(tick, tz) { Calendar.getInstance(tz).apply { timeInMillis = tick } }
    val jam = SimpleDateFormat("HH:mm", Locale("id")).format(now.time)
    val masehi = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id")).format(now.time)
    val times = remember(lat, lon, asarH, method, tz, corr, now.get(Calendar.DAY_OF_YEAR)) {
        PrayerCalc.compute(now, PrayerCalc.Params(
            lat = lat, lon = lon, method = method,
            asarFactor = if (asarH) 2.0 else 1.0, corr = corr, tz = tz,
        ))
    }
    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val (berikut, sisa) = PrayerCalc.next(nowMin, times)
    val berikutJam = when (berikut) {
        "Subuh" -> times.subuh; "Terbit" -> times.terbit; "Zuhur" -> times.zuhur
        "Asar" -> times.asar; "Magrib" -> times.magrib; else -> times.isya
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Assalamu'alaikum", style = MaterialTheme.typography.titleLarge)
        Text("$masehi • $jam")
        Text(HijriCalc.format(now, hijriCorr), style = MaterialTheme.typography.labelMedium)
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("$city — Berikutnya: $berikut $berikutJam (${sisa / 60}j ${sisa % 60}m lagi)",
                    style = MaterialTheme.typography.titleMedium)
                Text("Subuh ${times.subuh} • Zuhur ${times.zuhur} • Asar ${times.asar} • Magrib ${times.magrib} • Isya ${times.isya}")
                Text("Metode: ${method.label}", style = MaterialTheme.typography.labelSmall)
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
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pengingat waktu sholat", style = MaterialTheme.typography.titleMedium)
                    Switch(notif, {
                        notif = it
                        scope.launch {
                            store.set(Store.NOTIF, notif)
                            PrayerScheduler.scheduleAll(ctx.applicationContext)
                        }
                    })
                }
                Text("Pengingat memakai lokasi & metode hisab dari menu Jadwal. Atur suara adzan di menu Adzan.",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Favorit: $favCount • Riwayat: $histCount", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { go(Routes.FAVORIT) }) { Text("Buka favorit & riwayat") }
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
