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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlinx.coroutines.launch

@Composable
fun YasinScreen(vm: YasinViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val surah = vm.surah
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var focus by remember { mutableStateOf(false) }
    var keepOn by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var showBookmarks by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    FullscreenEffect(focus)
    DisposableEffect(keepOn) {
        val w = (ctx as? Activity)?.window
        if (keepOn) w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val shown = remember(query, surah) {
        if (query.isBlank()) surah.verses
        else surah.verses.filter {
            it.terjemahan.contains(query, true) || it.latin.contains(query, true) ||
                it.nomor.toString() == query.trim()
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        if (!focus) {
            Text("Surat Yasin — ${surah.ayatCount} ayat (MSI Kemenag)", style = MaterialTheme.typography.titleLarge)
            Text("Terakhir dibaca: ${if (s.mark in 1..surah.ayatCount) "ayat ${s.mark}" else "belum ada"}",
                style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(jump, { jump = it.filter(Char::isDigit).take(2) },
                    label = { Text("Ayat") }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    jump.toIntOrNull()?.let { n ->
                        if (n in 1..surah.ayatCount) scope.launch {
                            listState.animateScrollToItem((n - 1).coerceAtLeast(0))
                            vm.markRead(n)
                        }
                    }
                }) { Text("Ke ayat") }
            }
            OutlinedTextField(query, { query = it }, label = { Text("Cari dalam Yasin (teks/ayat)") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            if (query.isNotBlank()) Text("${shown.size} ayat cocok", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.showArab, { vm.toggleArab() }, { Text("Arab") })
                FilterChip(s.showLatin, { vm.toggleLatin() }, { Text("Latin") })
                FilterChip(s.showTrans, { vm.toggleTrans() }, { Text("Terjemahan") })
                FilterChip(focus, { focus = !focus }, { Text("Layar penuh") })
                FilterChip(keepOn, { keepOn = !keepOn }, { Text("Tetap menyala") })
                FilterChip(showBookmarks, { showBookmarks = !showBookmarks }, { Text("Bookmark ${s.bookmarks.size}") })
            }
            Text("Ukuran Arab: ${s.arabSize.toInt()}"); Slider(s.arabSize, { vm.setArabSize(it) }, valueRange = 16f..40f)
            Text("Ukuran Latin: ${s.latinSize.toInt()}"); Slider(s.latinSize, { vm.setLatinSize(it) }, valueRange = 12f..24f)
            Text("Jarak antarbaris: %.1f".format(s.lineHeight))
            Slider(s.lineHeight, { vm.setLineHeight(it) }, valueRange = 1.4f..2.6f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.mark in 1..surah.ayatCount) Button(onClick = {
                    scope.launch { listState.animateScrollToItem((s.mark - 1).coerceAtLeast(0)) }
                }) { Text("Lanjut ayat ${s.mark}") }
            }
            if (showBookmarks) {
                Text("Bookmark:", style = MaterialTheme.typography.titleSmall)
                if (s.bookmarks.isEmpty()) Text("Belum ada bookmark. Tandai ayat untuk menyimpan.",
                    style = MaterialTheme.typography.labelSmall)
                s.bookmarks.forEach { b ->
                    TextButton(onClick = {
                        b.refId.toIntOrNull()?.let { n ->
                            scope.launch { listState.animateScrollToItem((n - 1).coerceAtLeast(0)) }
                            showBookmarks = false
                        }
                    }) { Text(b.label.ifBlank { "Ayat ${b.refId}" }) }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { focus = false }) { Text("Keluar mode fokus") }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(shown) { v ->
                val marked = s.bookmarks.any { it.refId == v.nomor.toString() }
                Row {
                    Text("${v.nomor}", style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 8.dp, top = 12.dp))
                    Column(Modifier.weight(1f)) {
                        BacaanCard("yasin:${v.nomor}", "Ayat ${v.nomor}", v.arab, v.latin, v.terjemahan,
                            s.showLatin, s.showTrans, s.arabSize, s.latinSize,
                            lineHeightFactor = s.lineHeight, showArab = s.showArab)
                        Row {
                            TextButton(onClick = { vm.markRead(v.nomor) }) {
                                Text(if (s.mark == v.nomor) "✓ Penanda terakhir" else "Tandai terakhir dibaca")
                            }
                            TextButton(onClick = { vm.toggleBookmark(v.nomor) }) {
                                Text(if (marked) "★ Bookmark" else "☆ Bookmark")
                            }
                        }
                    }
                }
            }
        }
    }
}
