package id.or.kemenag.yasintahlil.feature.dzikir

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect

@Composable
fun DzikirScreen(vm: DzikirViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val groups = vm.groups
    var tab by remember { mutableStateOf(0) }
    var fullscreen by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    FullscreenEffect(fullscreen)

    if (groups.isEmpty()) {
        EmptyState("Konten dzikir tidak tersedia",
            "Berkas kumpulan dzikir tidak ditemukan di aplikasi. Pasang ulang aplikasi bila ini terus terjadi.")
        return
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Dzikir & Doa", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { settings = true }) { Text("Pengaturan bacaan") }
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh") })
        }
        ScrollableTabRow(tab) {
            groups.forEachIndexed { i, g -> Tab(tab == i, { tab = i }, text = { Text(g.judul) }) }
        }
        val group = groups[tab.coerceIn(0, groups.size - 1)]
        LazyColumn(Modifier.fillMaxSize()) {
            items(group.items) { it ->
                val key = "${group.id}:${it.judul}"
                val c = s.counts[key] ?: 0
                BacaanCard(key, it.judul, it.arab, it.latin, it.terjemahan,
                    s.showLatin, s.showTrans, s.arabSize, 15f, it.ulangi, it.sumber,
                    showArab = s.showArab, kategori = it.kategori)
                // Hitungan ditampilkan untuk semua bacaan, bukan hanya yang ulangi > 1: progresnya
                // tersimpan di Room, jadi pengguna bisa melihat/mengulang meski targetnya satu kali.
                CounterRow(c, it.ulangi,
                    onPlus = { vm.setCount(key, c + 1) },
                    onMinus = { vm.setCount(key, c - 1) },
                    onReset = { vm.reset(key) })
            }
        }
    }

    if (settings) {
        BacaanSettingsSheet(
            onDismiss = { settings = false },
            showArab = s.showArab, onShowArab = { vm.setShowArab(it) },
            showLatin = s.showLatin, onShowLatin = { vm.setShowLatin(it) },
            showTrans = s.showTrans, onShowTrans = { vm.setShowTrans(it) },
            arabSize = s.arabSize, onArabSize = { vm.setArabSize(it) },
        )
    }
}
