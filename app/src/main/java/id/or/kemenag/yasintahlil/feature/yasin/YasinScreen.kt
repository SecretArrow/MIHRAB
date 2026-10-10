package id.or.kemenag.yasintahlil.feature.yasin

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import kotlinx.coroutines.launch

@Composable
fun YasinScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val surah = remember { repo.yasin }
    var arabSize by remember { mutableStateOf(24f) }
    var latinSize by remember { mutableStateOf(16f) }
    var showLatin by remember { mutableStateOf(true) }
    var showTrans by remember { mutableStateOf(true) }
    var focus by remember { mutableStateOf(false) }
    var keepOn by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf("") }
    var mark by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, 24f)
        latinSize = store.get(Store.LATIN_SIZE, 16f)
        showLatin = store.get(Store.SHOW_LATIN, true)
        showTrans = store.get(Store.SHOW_TRANS, true)
        mark = store.get(Store.YASIN_MARK, 0)
    }
    DisposableEffect(keepOn) {
        val w = (ctx as? Activity)?.window
        if (keepOn) w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        if (!focus) {
            Text("Surat Yasin — 83 ayat (MSI Kemenag)", style = MaterialTheme.typography.titleLarge)
            Text("Terakhir dibaca: ayat $mark", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(jump, { jump = it.filter(Char::isDigit).take(2) },
                    label = { Text("Ayat") }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    jump.toIntOrNull()?.let { n ->
                        if (n in 1..83) scope.launch {
                            listState.animateScrollToItem((n - 1).coerceAtLeast(0))
                            store.set(Store.LAST, "yasin:$n"); store.set(Store.YASIN_MARK, n)
                            mark = n
                        }
                    }
                }) { Text("Ke ayat") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(showLatin, { showLatin = !showLatin; scope.launch { store.set(Store.SHOW_LATIN, showLatin) } }, { Text("Latin") })
                FilterChip(showTrans, { showTrans = !showTrans; scope.launch { store.set(Store.SHOW_TRANS, showTrans) } }, { Text("Terjemahan") })
                FilterChip(focus, { focus = !focus }, { Text("Fokus") })
                FilterChip(keepOn, { keepOn = !keepOn }, { Text("Tetap menyala") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { arabSize = (arabSize - 2).coerceAtLeast(16f); scope.launch { store.set(Store.ARAB_SIZE, arabSize) } }) { Text("A−") }
                OutlinedButton(onClick = { arabSize = (arabSize + 2).coerceAtMost(40f); scope.launch { store.set(Store.ARAB_SIZE, arabSize) } }) { Text("A+") }
                if (mark in 1..83) Button(onClick = { scope.launch { listState.animateScrollToItem((mark - 1).coerceAtLeast(0)) } }) { Text("Lanjut ayat $mark") }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { focus = false }) { Text("Keluar mode fokus") }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(surah.verses) { v ->
                Row {
                    Text("${v.nomor}", style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 8.dp, top = 12.dp))
                    Column(Modifier.weight(1f)) {
                        BacaanCard("yasin:${v.nomor}", "Ayat ${v.nomor}", v.arab, v.latin, v.terjemahan,
                            showLatin, showTrans, arabSize, latinSize)
                        TextButton(onClick = {
                            scope.launch { store.set(Store.YASIN_MARK, v.nomor); store.set(Store.LAST, "yasin:${v.nomor}"); mark = v.nomor }
                        }) { Text(if (mark == v.nomor) "✓ Penanda terakhir" else "Tandai terakhir dibaca") }
                    }
                }
            }
        }
    }
}
