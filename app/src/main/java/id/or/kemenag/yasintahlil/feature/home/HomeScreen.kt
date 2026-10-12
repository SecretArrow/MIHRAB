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
import id.or.kemenag.yasintahlil.navigation.RuteKonten
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

    // Zona waktu dari aturan bersama (`JadwalHariIni`), bukan disusun ulang di sini: id yang tak
    // dikenal pernah membuat jam beranda memakai GMT (`TimeZone.getTimeZone` mengembalikan GMT
    // untuk id asing) sementara kartu info harian memakai zona perangkat — dua waktu, satu hari.
    val tz = remember(s.tzId) { JadwalHariIni.zona(s.tzId) }
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
    // Nama→jam dari aturan bersama, bukan rangkaian `when` yang menutup dengan `else -> isya`:
    // cabang cadangan yang diam-diam menampilkan jam sholat lain lebih buruk daripada tak ada.
    val berikutJam = JadwalHariIni.jam(times, berikut) ?: times.isya

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
                // Tujuan ditentukan `RuteKonten` — aturan yang sama dengan layar Favorit — bukan
                // cabang `startsWith("yasin")` yang dahulu mengirim setiap bacaan Tahlil ke Favorit.
                // Id kelompok dzikir dari keadaan layar ikut disertakan: dahulu tidak, sehingga
                // jenis yang sama terbaca berbeda di beranda dan di layar Favorit.
                val tujuan = RuteKonten.tujuanTerakhir(s.last, s.grupDzikir)
                if (tujuan != null) {
                    Text(tujuan.nama, style = MaterialTheme.typography.labelSmall)
                    Button(onClick = { go(tujuan.rute) }) { Text("Lanjutkan") }
                } else {
                    Text(RuteKonten.TIDAK_DIKENAL, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (s.infoCard) Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Info Islami harian", style = MaterialTheme.typography.titleMedium)
                // Isinya berganti mengikuti tanggal dan diambil dari pustaka dzikir yang sudah
                // terverifikasi (`InfoHarian`) — dahulu kartu ini memuat satu kalimat tetap yang
                // khusus hari Jumat tetapi ditampilkan setiap hari.
                val gagal = s.infoGagal
                val b = s.info
                when {
                    gagal != null -> Text(gagal, style = MaterialTheme.typography.labelSmall)
                    b == null -> Text("Belum ada butir untuk hari ini.",
                        style = MaterialTheme.typography.labelSmall)
                    else -> {
                        Text(b.judul, style = MaterialTheme.typography.titleSmall)
                        // Ukuran Arab di kartu ini tetap: kartu beranda adalah ringkasan, bukan
                        // permukaan bacaan — ukuran pilihan pengguna berlaku di pustaka dzikirnya.
                        // Namun chip "Arab" di Pengaturan tetap dihormati: bila dimatikan, teks Arab
                        // disembunyikan di sini juga, agar preferensi itu tak menjadi kontrol mati.
                        if (s.showArab && b.arab.isNotBlank()) ArabText(b.arab, sizeSp = 20f)
                        if (b.teks.isNotBlank()) Text(b.teks)
                        if (b.sumber.isNotBlank()) Text("Sumber: ${b.sumber}",
                            style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { go(Routes.DZIKIR) }) { Text("Buka pustaka dzikir") }
                    }
                }
            }
        }
    }
}
