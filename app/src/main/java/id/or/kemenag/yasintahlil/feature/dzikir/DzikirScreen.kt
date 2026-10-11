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
    FullscreenEffect(fullscreen)

    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp)) { Text("Konten dzikir tidak tersedia.") }
        return
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Dzikir & Doa", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(s.showLatin, { vm.toggleLatin() }, { Text("Latin") })
            FilterChip(s.showTrans, { vm.toggleTrans() }, { Text("Terjemahan") })
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh") })
        }
        Text("Ukuran Arab: ${s.arabSize.toInt()}")
        Slider(s.arabSize, { vm.setArabSize(it) }, valueRange = 16f..40f)
        ScrollableTabRow(tab) {
            groups.forEachIndexed { i, g -> Tab(tab == i, { tab = i }, text = { Text(g.judul) }) }
        }
        val group = groups[tab.coerceIn(0, groups.size - 1)]
        LazyColumn(Modifier.fillMaxSize()) {
            items(group.items) { it ->
                val key = "${group.id}:${it.judul}"
                val c = s.counts[key] ?: 0
                BacaanCard(key, it.judul, it.arab, it.latin, it.terjemahan,
                    s.showLatin, s.showTrans, s.arabSize, 15f, it.ulangi, it.sumber)
                if (it.ulangi > 1) {
                    CounterRow(c, it.ulangi,
                        onPlus = { vm.setCount(key, (c + 1).coerceAtMost(it.ulangi)) },
                        onMinus = { vm.setCount(key, (c - 1)) },
                        onReset = { vm.reset(key) })
                }
            }
        }
    }
}
