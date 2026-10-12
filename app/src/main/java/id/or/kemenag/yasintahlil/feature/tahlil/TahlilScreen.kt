package id.or.kemenag.yasintahlil.feature.tahlil

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
fun TahlilScreen(vm: TahlilViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val repo = rememberRepo()
    var jamaah by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    FullscreenEffect(fullscreen)

    if (s.items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp)) { Text("Konten tahlil tidak tersedia.") }
        return
    }
    val cur = s.items[s.idx.coerceIn(0, s.items.size - 1)]
    val (arab, latin, trans) = remember(cur.id) {
        repo.resolveArabLatinTerjemah(cur.rujukanFile).let {
            Triple(it.first.ifBlank { cur.arab }, it.second.ifBlank { cur.latin }, it.third.ifBlank { cur.terjemahan })
        }
    }
    val selesai = s.count >= cur.ulangi

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Tahlil — bagian ${s.idx + 1} dari ${s.items.size}", style = MaterialTheme.typography.titleLarge)
        Text("Susunan lazim Indonesia; dapat berbeda menurut tradisi.",
            style = MaterialTheme.typography.labelMedium)

        if (s.susunan.size > 1) {
            ScrollableTabRow(s.susunan.indexOfFirst { it.id == s.susunanId }.coerceAtLeast(0)) {
                s.susunan.forEachIndexed { i, su ->
                    Tab(s.susunanId == su.id, { vm.setSusunan(su.id) }, text = { Text(su.nama) })
                }
            }
        }
        LinearProgressIndicator(progress = { ((s.idx + 1).toFloat() / s.items.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.prev() }, enabled = s.idx > 0) { Text("← Sebelumnya") }
            Button(onClick = { vm.next() }, enabled = s.idx < s.items.size - 1) { Text("Berikutnya →") }
            FilterChip(jamaah, { jamaah = !jamaah }, { Text("Jamaah") })
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(s.showArab, { vm.toggleArab() }, { Text("Arab") })
            FilterChip(s.showLatin, { vm.toggleLatin() }, { Text("Latin") })
            FilterChip(s.showTrans, { vm.toggleTrans() }, { Text("Terjemahan") })
        }
        CounterRow(s.count, cur.ulangi,
            onPlus = { vm.setCount(s.count + 1) },
            onMinus = { vm.setCount(s.count - 1) },
            onReset = { vm.setCount(0) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.next() }, enabled = s.idx < s.items.size - 1 && selesai) {
                Text(if (selesai) "✓ Tandai selesai →" else "Selesaikan dulu")
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                if (jamaah) {
                    if (s.showArab && arab.isNotBlank()) ArabText(arab, (s.arabSize + 8).coerceAtMost(44f))
                    if (s.showTrans && trans.isNotBlank()) Text(trans, style = MaterialTheme.typography.headlineSmall)
                } else {
                    BacaanCard("tahlil:${cur.id}", cur.judul, arab, latin, trans,
                        s.showLatin, s.showTrans, s.arabSize, 16f, cur.ulangi, cur.sumber,
                        showArab = s.showArab)
                }
                if (selesai) Text("✓ Selesai — lanjut ke bagian berikutnya.",
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                Text("Semua bagian:", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp))
            }
            items(s.items) { it ->
                val i = s.items.indexOf(it)
                TextButton(onClick = { vm.jumpTo(i) }) {
                    Text("${i + 1}. ${it.judul}${if (it.ulangi > 1) " (${it.ulangi}x)" else ""}")
                }
            }
        }
    }
}
