package id.or.kemenag.yasintahlil.feature.tasbih

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.BagianJudul
import id.or.kemenag.yasintahlil.core.designsystem.BarisChip
import id.or.kemenag.yasintahlil.core.designsystem.Lencana
import id.or.kemenag.yasintahlil.core.designsystem.NamaAntarmuka
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import kotlinx.coroutines.delay

/**
 * Layar Tasbih Digital: penghitung harian yang diketuk, bukan ditekan tombol − / + (tombol teks
 * semacam itu hanya hidup di Pengaturan). Satu ketukan pada lingkaran besar menimpa baris hari ini
 * di Room; getaran singkat menandai tiap ketukan, dan getaran kedua — diberi jeda agar terasa
 * sebagai dua — menandai target tercapai.
 *
 * Target dipilih lewat chip (33/99/100/999) atau diisi sendiri lewat dialog "Lainnya". Riwayat
 * 7 hari dibaca dari tabel yang sama, sehingga daftarnya ikut menyegarkan diri tiap ketukan.
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

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(NamaLayar.TASBIH, style = MaterialTheme.typography.titleLarge)
        Text(
            "Ketuk lingkaran untuk menghitung — getaran singkat menandai tiap ketukan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ---- Lingkaran ketuk --------------------------------------------------------------------
        // Warna lingkaran berganti saat target tercapai supaya capaian itu terlihat sekilas dari
        // jauh — pengguna tasbih sering tidak menatap layar saat menghitung. Getar memakai
        // `HapticFeedbackType.LongPress` karena ia tersedia di seluruh versi Compose yang dipakai
        // aplikasi ini (`HeavyClick` hadir belakangan); getar kedua diberi jeda 150 ms agar
        // terasa sebagai penanda tersendiri, bukan getar pertama yang memanjang.
        val tercapai = s.targetTercapai
        val latar = if (tercapai) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        val warnaIsi = if (tercapai) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(220.dp)
                .clip(CircleShape)
                .background(latar)
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
                Text("${s.hitung}", style = MaterialTheme.typography.headlineLarge, color = warnaIsi)
                Text("dari ${s.target}", style = MaterialTheme.typography.labelLarge, color = warnaIsi)
                if (tercapai) Lencana("Target tercapai")
            }
        }

        // ---- Target -----------------------------------------------------------------------------
        BarisChip {
            targetPilihan.forEach { t ->
                FilterChip(s.target == t, { vm.aturTarget(t) }, { Text("$t") })
            }
            // "Lainnya" tampak terpilih saat target hari ini bukan salah satu chip di kirinya,
            // supaya pengguna tidak mengira targetnya hilang dari pilihan.
            FilterChip(s.target !in targetPilihan, { dialogTerbuka = true }, { Text("Lainnya") })
        }

        OutlinedButton(onClick = { vm.reset() }) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ulang dari nol")
        }

        // ---- Riwayat ----------------------------------------------------------------------------
        BagianJudul("Riwayat ${TasbihViewModel.RIWAYAT_HARI} hari")
        if (s.riwayat.isEmpty()) {
            Text(
                "Belum ada hitungan tersimpan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                s.riwayat.forEach { hari ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            TasbihHarian.label(hari.tanggal, s.hariIni),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text("${hari.hitung}", style = MaterialTheme.typography.bodyLarge)
                    }
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
}
