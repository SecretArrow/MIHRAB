package id.or.kemenag.yasintahlil.feature.jadwal

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.PrayerCalc

@Composable
fun JadwalScreen(vm: JadwalViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var drop by remember { mutableStateOf(false) }
    var methodDrop by remember { mutableStateOf(false) }
    var tzDrop by remember { mutableStateOf(false) }
    var needPermission by remember { mutableStateOf(false) }
    // Izin bisa diminta dari dua tombol berbeda (ambil sekali vs ikuti perubahan); yang terakhir
    // ditekan menentukan tindakan setelah izin diberikan.
    var mintaIkuti by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        needPermission = false
        // Setelah izin diberikan, langsung coba ambil perbaikan; bila belum ada, useGps() mengisi
        // pesanLokasi sehingga layar menampilkan penjelasan + tombol coba lagi.
        if (result.values.any { it }) {
            if (mintaIkuti) vm.setFollowGps(true) else vm.useGps()
        }
        mintaIkuti = false
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Jadwal Sholat", style = MaterialTheme.typography.titleLarge)
        Text(
            "Metode: ${s.method.label} • Fajr ${s.method.fajr}° / Isya ${if (s.method.ishaIntervalMin != null) "Magrib+${s.method.ishaIntervalMin}mnt" else "${s.method.isha}°"} • Asar ${if (s.asarHanafi) "Hanafi (2)" else "Syafi'i (1)"}",
            style = MaterialTheme.typography.labelMedium,
        )

        // Metode perhitungan
        OutlinedButton(onClick = { methodDrop = !methodDrop; tzDrop = false; drop = false }) {
            Text("Metode: ${s.method.label} ▾")
        }
        if (methodDrop) PrayerCalc.Method.entries.forEach { m ->
            TextButton(onClick = { vm.setMethod(m); methodDrop = false }) { Text(m.label) }
        }

        // Kota
        OutlinedButton(onClick = { drop = !drop; methodDrop = false; tzDrop = false }) { Text("Kota: ${s.city} ▾") }
        if (drop) vm.kota.forEach { k ->
            TextButton(onClick = { vm.selectCity(k); drop = false }) { Text(k.nama) }
        }
        OutlinedButton(onClick = {
            if (!vm.hasLocationPermission()) {
                mintaIkuti = false
                needPermission = true
                permLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ))
            } else {
                vm.useGps()
            }
        }) { Text("Pakai lokasi GPS") }

        // `plan.txt`: "Pembaruan jadwal saat lokasi berubah" — tanpa saklar ini jadwal hanya
        // diperbarui saat tombol di atas ditekan, bukan saat pengguna benar-benar berpindah.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ikuti perubahan lokasi")
            Switch(s.followGps, { on ->
                if (on && !vm.hasLocationPermission()) {
                    mintaIkuti = true
                    needPermission = true
                    permLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ))
                } else {
                    vm.setFollowGps(on)
                }
            })
        }
        Text(
            if (s.followGps) "Aktif: jadwal dihitung ulang saat Anda berpindah tempat " +
                "(pergeseran ≥ ${(JadwalViewModel.AMBANG_PINDAH_METER / 1000).toInt()} km), " +
                "tanpa perlu menekan tombol."
            else "Nonaktif: jadwal hanya diperbarui saat Anda menekan \"Pakai lokasi GPS\" " +
                "atau memilih kota secara manual.",
            style = MaterialTheme.typography.labelSmall,
        )

        if (needPermission) Text(
            "Izinkan lokasi agar jadwal memakai koordinat GPS perangkat; bila tidak, pilih kota secara manual.",
            style = MaterialTheme.typography.labelSmall,
        )
        s.pesanLokasi?.let { pesan ->
            ErrorState(pesan, onRetry = { vm.useGps() })
        }

        // Zona waktu
        OutlinedButton(onClick = { tzDrop = !tzDrop; methodDrop = false; drop = false }) {
            val label = JadwalViewModel.TZ_OPTIONS.firstOrNull { it.first == s.tzId }?.second ?: s.tzId
            Text("Zona waktu: $label ▾")
        }
        if (tzDrop) JadwalViewModel.TZ_OPTIONS.forEach { (id, label) ->
            TextButton(onClick = { vm.setTimezone(id); tzDrop = false }) { Text(label) }
        }

        // Mazhab Asar
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!s.asarHanafi, { vm.setAsarHanafi(false) }, { Text("Asar Syafi'i") })
            FilterChip(s.asarHanafi, { vm.setAsarHanafi(true) }, { Text("Asar Hanafi") })
        }

        Text("Koreksi menit per waktu", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp))
        // Nama dari aturan bersama: indeksnya harus sama dengan CSV `Store.CORR` yang diurai
        // `JadwalHariIni.parseCorr`, karena `s.corr[i]` di bawah dipasangkan ke waktu ke-i.
        val names = JadwalHariIni.ENAM_WAKTU
        names.forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(n)
                Row {
                    OutlinedButton(onClick = { vm.setCorrection(i, -1) }) { Text("−") }
                    Text("  ${if (s.corr[i] > 0) "+" else ""}${s.corr[i]}  ",
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { vm.setCorrection(i, +1) }) { Text("+") }
                }
            }
        }

        Divider(Modifier.padding(vertical = 8.dp))
        when {
            s.memuat -> {
                Text("Jadwal hari ini", style = MaterialTheme.typography.titleSmall)
                SkeletonCard(baris = 6)
            }

            s.gagalMuat != null -> ErrorState(
                "Jadwal gagal dimuat: ${s.gagalMuat}. Jadwal dihitung di perangkat, jadi ini bukan " +
                    "masalah jaringan — coba muat ulang.",
                onRetry = { vm.muatUlang() },
            )

            s.times != null -> {
                val t = s.times!!
                // Dipasangkan dari aturan bersama, sehingga nama dan jamnya tak dapat tertukar.
                val rows = JadwalHariIni.ENAM_WAKTU.zip(JadwalHariIni.semua(t))
                rows.forEach { (n, j) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(n, style = MaterialTheme.typography.titleMedium)
                        Text(j, style = MaterialTheme.typography.titleMedium)
                    }
                }
                val (berikut, sisa) = PrayerCalc.next(s.nowMin, t)
                Text("Berikutnya: $berikut dalam ${sisa / 60} jam ${sisa % 60} mnt",
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }

            else -> EmptyState(
                "Jadwal belum tersedia",
                "Waktu sholat belum terhitung untuk lokasi ini.",
                labelTombol = "Hitung ulang",
                onTombol = { vm.muatUlang() },
            )
        }
        Text(
            "Jadwal dihitung di perangkat (algoritma Meeus), bukan diambil dari server — jadi " +
                "tidak ada cache daring yang bisa basi dan jadwal tetap tersedia tanpa internet. " +
                "Verifikasi: sihat.kemenag.dev",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp),
        )
    }
}
