package id.or.kemenag.yasintahlil.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.HijriCalc
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import id.or.kemenag.yasintahlil.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

/**
 * Beranda (`plan.txt` §"Beranda Utama").
 *
 * Seluruh keadaan datang dari [HomeViewModel] sebagai `StateFlow` — bukan `remember` di layar —
 * sehingga bertahan saat rotasi dan ikut berubah begitu pengguna mengganti lokasi/metode di layar
 * Jadwal atau mematikan pengingat, tanpa membuka ulang beranda.
 */
@Composable
fun HomeScreen(go: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    val tz = remember(s.tzId) { if (s.tzId.isBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(s.tzId) }
    val now = remember(s.nowMillis, tz) { Calendar.getInstance(tz).apply { timeInMillis = s.nowMillis } }
    val jam = SimpleDateFormat("HH:mm", Locale("id")).format(now.time)
    val masehi = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id")).format(now.time)
    val times = remember(s.lat, s.lon, s.asarHanafi, s.method, tz, s.corr, now.get(Calendar.DAY_OF_YEAR)) {
        // Parameter disusun lewat `JadwalHariIni` yang sama dengan penjadwal pengingat & menu
        // Adzan, sehingga hitung mundur di beranda tidak dapat berbeda dari jam yang benar-benar
        // dijadwalkan hanya karena aturannya ditulis ulang di sini.
        PrayerCalc.compute(now, JadwalHariIni.params(
            lat = s.lat, lon = s.lon, method = s.method,
            asarHanafi = s.asarHanafi, corr = s.corr, tz = tz,
        ))
    }
    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val (berikut, sisa) = PrayerCalc.next(nowMin, times)
    val berikutJam = when (berikut) {
        "Subuh" -> times.subuh; "Terbit" -> times.terbit; "Zuhur" -> times.zuhur
        "Asar" -> times.asar; "Magrib" -> times.magrib; else -> times.isya
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        // Ornamen geometris Islami halus (`plan.txt` §"Konsep dan Identitas Aplikasi"). Digambar di
        // belakang blok salam dengan alfa sangat rendah dan mengikuti tinggi blok itu, sehingga
        // berfungsi sebagai tekstur latar — bukan hiasan yang mengurangi keterbacaan teks di atasnya.
        Box(Modifier.fillMaxWidth()) {
            OrnamenIslami(Modifier.matchParentSize())
            Column {
                Text("Assalamu'alaikum", style = MaterialTheme.typography.titleLarge)
                Text("$masehi • $jam")
                Text(HijriCalc.format(now, s.hijriCorr), style = MaterialTheme.typography.labelMedium)
            }
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("${s.city} — Berikutnya: $berikut $berikutJam (${sisa / 60}j ${sisa % 60}m lagi)",
                    style = MaterialTheme.typography.titleMedium)
                Text("Subuh ${times.subuh} • Zuhur ${times.zuhur} • Asar ${times.asar} • Magrib ${times.magrib} • Isya ${times.isya}")
                Text("Metode: ${s.method.label}", style = MaterialTheme.typography.labelSmall)
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
                    Switch(s.notif, { vm.setNotif(it) })
                }
                Text("Pengingat memakai lokasi & metode hisab dari menu Jadwal. Atur suara adzan di menu Adzan.",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Favorit: ${s.favCount} • Riwayat: ${s.histCount}", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { go(Routes.FAVORIT) }) { Text("Buka favorit & riwayat") }
            }
        }
        if (s.last.isNotBlank()) Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Terakhir dibaca: ${s.last}")
                Button(onClick = { go(if (s.last.startsWith("yasin")) Routes.YASIN else Routes.FAVORIT) }) { Text("Lanjutkan") }
            }
        }
        if (s.infoCard) Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Info Islami harian", style = MaterialTheme.typography.titleMedium)
                Text("Perbanyak sholawat di hari Jumat (HR Abu Dawud). Sumber: sunnah.com")
            }
        }
    }
}
