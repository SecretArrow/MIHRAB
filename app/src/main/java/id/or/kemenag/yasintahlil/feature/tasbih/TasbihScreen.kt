package id.or.kemenag.yasintahlil.feature.tasbih

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.BarisChip
import id.or.kemenag.yasintahlil.core.designsystem.NamaAntarmuka
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.delay

/**
 * Layar Tasbih Digital: penghitung harian yang diketuk, bukan ditekan tombol − / + (tombol teks
 * semacam itu hanya hidup di Pengaturan). Satu ketukan pada lingkaran besar menimpa baris hari ini
 * di Room; getaran singkat menandai tiap ketukan, dan getaran kedua — diberi jeda agar terasa
 * sebagai dua — menandai target tercapai (TANPA teks "Target tercapai" di layar; umpan baliknya
 * cukup dari getar dan ganti warna lingkaran).
 *
 * Susunannya mengikuti desain terkini: target chips di **atas**, diikuti baris "Reset hari ini" +
 * "Riwayat 7 hari" (riwayatnya layar tersendiri), dan lingkaran ketuk di **bawah** — angka besar
 * dengan hitungan putaran ("2x") di bawahnya saat hitungan sudah melewati target. Ikon bel di
 * kanan-atas membuka layar Keterangan Tasbih, rumah teks penjelasan yang dahulu menumpuk di sini.
 *
 * Hitungan teratur ulang tiap ganti hari — satu baris Room per tanggal — dan 7 hari terakhir
 * tetap dapat dibuka dari layar riwayatnya.
 */
@Composable
fun TasbihScreen(go: (String) -> Unit) {
    val vm: TasbihViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    val getar = LocalHapticFeedback.current

    // rememberSaveable: dialog target dan angka yang sedang diketik tidak boleh hilang saat
    // perangkat diputar — mengetik ulang angkanya adalah harga yang tidak perlu.
    var targetKustom by rememberSaveable { mutableStateOf("") }
    var dialogTerbuka by rememberSaveable { mutableStateOf(false) }
    // Tanda menyeberangi target — sengaja tidak disimpan: hanya berumur selama ketukannya.
    var lewatTarget by remember { mutableStateOf(false) }
    LaunchedEffect(lewatTarget) {
        if (lewatTarget) {
            delay(150)
            getar.performHapticFeedback(HapticFeedbackType.LongPress)
            lewatTarget = false
        }
    }
    val targetPilihan = listOf(33, 99, 100, 999)
    // Jumlah putaran penuh: contoh target 33 dan sudah terklik lebih dari 66 → "2x".
    val putaran = if (s.target > 0) s.hitung / s.target else 0

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(NamaLayar.TASBIH, style = MaterialTheme.typography.titleLarge)

        // ---- Ikon bel → layar Keterangan Tasbih ---------------------------------------------------
        // Teks penjelasan (getar, putaran, reset harian, target) tinggal di layar keterangan yang
        // dibuka dari sini — badan layar cukup alat hitungnya saja.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(
                onClick = { go(Routes.TASBIH_INFO) },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = NamaLayar.TASBIH_INFO,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        // ---- Target — di atas ------------------------------------------------------------------------
        BarisChip {
            targetPilihan.forEach { t ->
                FilterChip(s.target == t, { vm.aturTarget(t) }, { Text("$t") })
            }
            // "Lainnya" tampak terpilih saat target hari ini bukan salah satu chip di kirinya,
            // supaya pengguna tidak mengira targetnya hilang dari pilihan.
            FilterChip(s.target !in targetPilihan, { dialogTerbuka = true }, { Text("Lainnya") })
        }

        // ---- Reset + riwayat — mengikuti baris target -------------------------------------------------
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.reset() }) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset hari ini")
            }
            OutlinedButton(onClick = { go(Routes.TASBIH_RIWAYAT) }) {
                Text("Riwayat ${TasbihViewModel.RIWAYAT_HARI} hari")
            }
        }

        // ---- Lingkaran ketuk — di bawah -----------------------------------------------------------------
        // Warna lingkaran berganti saat target tercapai supaya capaian itu terlihat sekilas dari
        // jauh — pengguna tasbih sering tidak menatap layar saat menghitung. Getar memakai
        // `HapticFeedbackType.LongPress` karena ia tersedia di seluruh versi Compose yang dipakai
        // aplikasi ini (`HeavyClick` hadir belakangan); getar kedua diberi jeda 150 ms agar
        // terasa sebagai penanda tersendiri, bukan getar pertama yang memanjang. Di bawah angka
        // besarnya tampil hitungan putaran ("2x") setelah target terlewati, atau "dari N" sebelum.
        val tercapai = s.targetTercapai
        val latar = if (tercapai) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        val warnaIsi = if (tercapai) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(latar)
                .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                .clickable {
                    val lama = s.hitung
                    vm.tap()
                    getar.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (s.target > 0 && lama < s.target && lama + 1 >= s.target) {
                        lewatTarget = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "${s.hitung}",
                    style = MaterialTheme.typography.displayMedium,
                    color = warnaIsi,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (putaran >= 1) "${putaran}x" else "dari ${s.target}",
                    style = MaterialTheme.typography.labelLarge,
                    color = warnaIsi,
                )
            }
        }
    }

    if (dialogTerbuka) {
        AlertDialog(
            onDismissRequest = { dialogTerbuka = false },
            title = { Text("Target lainnya") },
            text = {
                OutlinedTextField(
                    targetKustom,
                    { targetKustom = it },
                    label = {
                        Text(
                            "Jumlah hitungan " +
                                "(${TasbihViewModel.TARGET_MIN}-${TasbihViewModel.TARGET_MAKS})",
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Di luar rentang yang sah, ketukan "Atur" diabaikan — dialog tetap
                    // terbuka supaya salah ketik dapat langsung diperbaiki, bukan memulai
                    // dialognya dari awal.
                    val n = targetKustom.toIntOrNull()
                    if (n != null && n in TasbihViewModel.TARGET_MIN..TasbihViewModel.TARGET_MAKS) {
                        vm.aturTarget(n)
                        targetKustom = ""
                        dialogTerbuka = false
                    }
                }) { Text("Atur") }
            },
            dismissButton = {
                TextButton(onClick = { dialogTerbuka = false }) { Text(NamaAntarmuka.BATAL) }
            },
        )
    }
}

/**
 * Layar Keterangan Tasbih — rumah teks penjelasan yang dahulu menumpuk di badan layar Tasbih
 * (getar tiap ketukan, hitungan putaran, reset harian, dan pilihan target). Dibuka dari ikon bel
 * pada layar Tasbih; isinya statis karena menjelaskan PERILAKU aplikasi, bukan isi bundel.
 */
@Composable
fun TasbihInfoScreen() {
    val butir = listOf(
        "Getar tiap ketukan" to
            "Setiap ketukan memberi getar singkat sebagai tanda hitungan masuk. Saat target " +
            "tercapai (ketukan habis dibagi target), perangkat bergetar ganda — tanpa perlu " +
            "melihat layar.",
        "Hitungan putaran" to
            "Melewati target, lingkaran menampilkan hitungan putaran di bawah angka besar. " +
            "Contoh: target 33 dan sudah terklik lebih dari 66 → lingkaran menulis \"2x\", " +
            "artinya dua kali target. Sebelum target tercapai, di bawah angka tampil " +
            "\"dari 33\" (target yang sedang dipilih).",
        "Penghitung teratur ulang tiap hari" to
            "Hitungan ketukan dimulai dari 0 setiap ganti hari secara otomatis. Tujuh hari " +
            "terakhir tetap tersimpan dan dapat dibuka lewat tombol \"Riwayat 7 hari\" di " +
            "layar Tasbih.",
        "Target bawaan" to
            "Pilihan target tersedia 33, 99, 100, 999, atau \"Lainnya\" untuk angka sendiri. " +
            "Target tersimpan per hari; mengganti target tidak menghapus hitungan yang sudah " +
            "berjalan.",
    )
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(NamaLayar.TASBIH_INFO, style = MaterialTheme.typography.titleLarge)
        butir.forEach { (judul, isi) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(judul, style = MaterialTheme.typography.titleSmall)
                    Text(
                        isi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Layar Riwayat Tasbih — rekap ketukan 7 hari terakhir yang dahulu menumpuk di badan layar
 * Tasbih. Tiap baris menyebut jumlah ketukan, target hari itu, dan putarannya, dengan bilah
 * proporsi terhadap hari yang paling panjang; seluruh riwayat dapat dikosongkan dari sini.
 */
@Composable
fun TasbihRiwayatScreen() {
    val vm: TasbihViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    var konfirmasi by rememberSaveable { mutableStateOf(false) }
    // Proporsi bilah dihitung terhadap hari terbesar — bukan terhadap target — sehingga
    // perbandingan antarhari terbaca walau targetnya berbeda-beda.
    val terbesar = s.riwayat.maxOfOrNull { it.hitung }?.coerceAtLeast(1) ?: 1

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(NamaLayar.TASBIH_RIWAYAT, style = MaterialTheme.typography.titleLarge)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Rekap ketukan tasbih ${TasbihViewModel.RIWAYAT_HARI} hari terakhir. Tiap hari " +
                    "menghitung sendiri; target mengikuti pilihan terakhir di layar Tasbih.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                if (s.riwayat.isEmpty()) {
                    Text(
                        "Belum ada riwayat — mulai ketuk di layar Tasbih.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                s.riwayat.forEach { hari ->
                    val p = if (hari.target > 0) hari.hitung / hari.target else 0
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            TasbihHarian.label(hari.tanggal, s.hariIni),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "${hari.hitung} ketukan · target ${hari.target}" +
                                if (p >= 1) " · ${p}x" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LinearProgressIndicator(
                            progress = { (hari.hitung.toFloat() / terbesar).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp)
                                .clip(RoundedCornerShape(50)),
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { konfirmasi = true },
            enabled = s.riwayat.isNotEmpty(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text(NamaAntarmuka.KOSONGKAN) }
    }

    if (konfirmasi) {
        AlertDialog(
            onDismissRequest = { konfirmasi = false },
            title = { Text("Kosongkan riwayat tasbih?") },
            text = {
                Text("Seluruh hitungan ${TasbihViewModel.RIWAYAT_HARI} hari terakhir — termasuk " +
                    "hitungan hari ini — akan dihapus dari perangkat dan tidak dapat dikembalikan.")
            },
            confirmButton = {
                TextButton(onClick = {
                    konfirmasi = false
                    vm.kosongkan()
                }) { Text(NamaAntarmuka.KOSONGKAN) }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasi = false }) { Text(NamaAntarmuka.BATAL) }
            },
        )
    }
}
