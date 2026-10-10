package id.or.kemenag.yasintahlil.feature.tahlil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import kotlinx.coroutines.launch

@Composable
fun TahlilScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val items = remember { repo.tahlil() }
    var idx by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(0) }
    var jamaah by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(24f) }
    var showLatin by remember { mutableStateOf(true) }
    var showTrans by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, 24f)
        val p = store.get(Store.TAHLIL_PROG, "0:0").split(":")
        idx = p.getOrNull(0)?.toIntOrNull()?.coerceIn(0, items.size - 1) ?: 0
        count = p.getOrNull(1)?.toIntOrNull() ?: 0
    }
    fun save() = scope.launch { store.set(Store.TAHLIL_PROG, "$idx:$count") }
    val cur = items[idx]
    val (arab, latin, trans) = repo.resolveArabLatinTerjemah(cur.rujukanFile).let {
        Triple(it.first.ifBlank { cur.arab }, it.second.ifBlank { cur.latin }, it.third.ifBlank { cur.terjemahan })
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Tahlil — bagian ${idx + 1} dari ${items.size}", style = MaterialTheme.typography.titleLarge)
        Text("Susunan lazim Indonesia; dapat berbeda menurut tradisi.",
            style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(progress = { (idx.toFloat() / items.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (idx > 0) { idx--; count = 0; save() } }, enabled = idx > 0) { Text("← Sebelumnya") }
            Button(onClick = {
                if (idx < items.size - 1) { idx++; count = 0; save() }
            }, enabled = idx < items.size - 1) { Text("Berikutnya →") }
            FilterChip(jamaah, { jamaah = !jamaah }, { Text("Jamaah") })
        }
        CounterRow(count, cur.ulangi,
            onPlus = { if (count < cur.ulangi) { count++; save() } },
            onMinus = { if (count > 0) { count--; save() } },
            onReset = { count = 0; save() })
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                if (jamaah) {
                    ArabText(arab, (arabSize + 8).coerceAtMost(44f))
                    if (trans.isNotBlank()) Text(trans, style = MaterialTheme.typography.headlineSmall)
                } else {
                    BacaanCard("tahlil:${cur.id}", cur.judul, arab, latin, trans,
                        showLatin, showTrans, arabSize, 16f, cur.ulangi, cur.sumber)
                }
                if (count >= cur.ulangi) Text("✓ Selesai — lanjut ke bagian berikutnya.",
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                Text("Semua bagian:", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp))
            }
            items(items) { it ->
                TextButton(onClick = { idx = items.indexOf(it); count = 0; save() }) {
                    Text("${items.indexOf(it) + 1}. ${it.judul}${if (it.ulangi > 1) " (${it.ulangi}x)" else ""}")
                }
            }
        }
    }
}
