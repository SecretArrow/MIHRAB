package id.or.kemenag.yasintahlil.feature.jadwal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.location.LocationProvider
import id.or.kemenag.yasintahlil.core.util.FormatWaktu
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun JadwalScreen(go: (String) -> Unit = {}, vm: JadwalViewModel = hiltViewModel()) {
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
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(NamaLayar.JADWAL, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("jadwal")) }
        }
        Text(
            "Metode: ${s.method.label} • ${JadwalHariIni.metodeRingkas(s.method)} • Asar ${JadwalHariIni.asarLabelFaktor(s.asarHanafi)}",
            style = MaterialTheme.typography.labelMedium,
        )

        // Metode perhitungan
        OutlinedButton(onClick = { methodDrop = !methodDrop; tzDrop = false; drop = false },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text("Metode: ${s.method.label} ▾")
        }
        if (methodDrop) PrayerCalc.Method.entries.forEach { m ->
            TextButton(onClick = { vm.setMethod(m); methodDrop = false }) { Text(m.label) }
        }

        // Kota
        OutlinedButton(onClick = { drop = !drop; methodDrop = false; tzDrop = false },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text("Kota: ${s.city} ▾") }
        // Daftar kosong diberi keterangan, bukan dibiarkan tampak seperti tombol yang rusak: sebabnya
        // dibawa dari `JadwalViewModel.kotaGagal` (bundel konten tidak terbaca).
        if (drop && vm.kota.isEmpty()) {
            Text(vm.kotaGagal ?: "Daftar kota pada bundel konten kosong.",
                style = MaterialTheme.typography.labelSmall)
        }
        if (drop) vm.kota.forEach { k ->
            TextButton(onClick = { vm.selectCity(k); drop = false }) { Text(k.nama) }
        }
        OutlinedButton(onClick = {
            if (!vm.hasLocationPermission()) {
                mintaIkuti = false
                needPermission = true
                permLauncher.launch(LocationProvider.IZIN.toTypedArray())
            } else {
                vm.useGps()
            }
        }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text("Pakai lokasi GPS") }

        // `plan.txt`: "Pembaruan jadwal saat lokasi berubah" — tanpa saklar ini jadwal hanya
        // diperbarui saat tombol di atas ditekan, bukan saat pengguna benar-benar berpindah.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ikuti perubahan lokasi")
            Switch(s.followGps, { on ->
                if (on && !vm.hasLocationPermission()) {
                    mintaIkuti = true
                    needPermission = true
                    permLauncher.launch(LocationProvider.IZIN.toTypedArray())
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
        OutlinedButton(onClick = { tzDrop = !tzDrop; methodDrop = false; drop = false },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            val label = JadwalViewModel.TZ_OPTIONS.firstOrNull { it.first == s.tzId }?.second ?: s.tzId
            Text("Zona waktu: $label ▾")
        }
        if (tzDrop) JadwalViewModel.TZ_OPTIONS.forEach { (id, label) ->
            TextButton(onClick = { vm.setTimezone(id); tzDrop = false }) { Text(label) }
        }

        // Mazhab Asar. Nama mazhabnya dari aturan bersama; awalan "Asar" milik layar ini karena
        // chip di sini berdiri tanpa judul bagian (di menu Pengaturan sudah ada judul "Mazhab Asar:").
        BarisChip {
            FilterChip(!s.asarHanafi, { vm.setAsarHanafi(false) },
                { Text("Asar ${JadwalHariIni.asarLabel(false)}") })
            FilterChip(s.asarHanafi, { vm.setAsarHanafi(true) },
                { Text("Asar ${JadwalHariIni.asarLabel(true)}") })
        }

        // Koreksi menit per waktu dahulu menempel di sini sebagai deretan tombol − / +. Kendali itu
        // kini terpusat di tab Pengaturan (bagian "Jadwal & lokasi") — layar ini cukup membaca dan
        // menampilkan hasilnya: jam yang tercetak di bawah tetap memakai koreksi yang dipilih.

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
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
                // Nama dan jamnya datang bersama dari aturan bersama — sama dengan hitung mundur di
                // beranda, sehingga kedua layar tak dapat menunjuk waktu yang berlainan.
                val berikut = JadwalHariIni.berikutnya(s.nowMin, t)
                // Jedanya dieja aturan bersama `FormatWaktu` — sama dengan hitung mundur beranda.
                // Sebelumnya kedua layar menuliskan pembagiannya sendiri dan hasilnya berbeda
                // (`2j 15m lagi` vs `dalam 2 jam 15 mnt`), keduanya selalu menyebut jamnya
                // sehingga jeda sepuluh menit terbaca `0 jam 10 mnt`.
                Text("Berikutnya: ${berikut.nama} dalam " +
                    FormatWaktu.durasiJamMenit(berikut.sisaMenit),
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }

            else -> EmptyState(
                "Jadwal belum tersedia",
                "Waktu sholat belum terhitung untuk lokasi ini.",
                labelTombol = "Hitung ulang",
                onTombol = { vm.muatUlang() },
            )
        }
    }
}
