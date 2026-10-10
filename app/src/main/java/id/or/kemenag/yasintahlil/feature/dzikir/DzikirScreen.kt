package id.or.kemenag.yasintahlil.feature.dzikir

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*

@Composable
fun DzikirScreen() {
    val repo = rememberRepo()
    val groups = remember { repo.dzikir() }
    var tab by remember { mutableStateOf(0) }
    var arabSize by remember { mutableStateOf(22f) }
    var showLatin by remember { mutableStateOf(true) }
    var showTrans by remember { mutableStateOf(true) }
    val store = rememberStore()
    LaunchedEffect(Unit) { arabSize = store.get(Store.ARAB_SIZE, 22f) }
    // counter per item
    val counts = remember { mutableStateMapOf<String, Int>() }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Dzikir & Doa", style = MaterialTheme.typography.titleLarge)
        TabRow(tab) { groups.forEachIndexed { i, g -> Tab(tab == i, { tab = i }, text = { Text(g.judul.take(14)) }) } }
        LazyColumn(Modifier.fillMaxSize()) {
            items(groups[tab.coerceIn(0, groups.size - 1)].items) { it ->
                val key = "${groups[tab].id}:${it.judul}"
                val c = counts[key] ?: 0
                BacaanCard(key, it.judul, it.arab, it.latin, it.terjemahan,
                    showLatin, showTrans, arabSize, 15f, it.ulangi, it.sumber)
                if (it.ulangi > 1) CounterRow(c, it.ulangi,
                    onPlus = { counts[key] = (c + 1).coerceAtMost(it.ulangi) },
                    onMinus = { counts[key] = (c - 1).coerceAtLeast(0) },
                    onReset = { counts[key] = 0 })
            }
        }
    }
}
