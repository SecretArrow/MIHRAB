package id.or.kemenag.yasintahlil.feature.yasin

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.data.AtribusiTeks
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect

@Composable
fun YasinScreen(vm: YasinViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val surah = vm.surah
    val ctx = LocalContext.current
    // rememberSaveable: mode baca, kata kunci, dan daftar bookmark tidak hilang saat diputar ulang.
    // `jump` khususnya: angka yang sudah diketik pengguna untuk melompat ke ayat tertentu lenyap
    // begitu perangkat diputar, dan ia harus mengetiknya lagi.
    var focus by rememberSaveable { mutableStateOf(false) }
    var keepOn by rememberSaveable { mutableStateOf(false) }
    var jump by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
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

    // Ayat yang sedang dituju tombol "Ke ayat" / daftar bookmark. Diselesaikan di efek ini, bukan
    // langsung, karena indeksnya bergantung pada daftar yang sedang tampil.
    var targetAyat by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(targetAyat, shown) {
        val n = targetAyat ?: return@LaunchedEffect
        val idx = shown.indexOfFirst { it.nomor == n }
        when {
            idx >= 0 -> { listState.animateScrollToItem(idx); targetAyat = null }
            // Ayatnya tersaring oleh pencarian: buang penyaringnya, lalu ulangi.
            query.isNotBlank() -> query = ""
            // Ayat di luar surat ini (mis. bookmark dari bundel lain) — jangan menebak.
            else -> targetAyat = null
        }
    }

    /**
     * Lompat ke ayat [n] pada daftar yang **sedang tampil**.
     *
     * Sebelumnya indeksnya langsung `n - 1`, yang hanya benar bila tak ada pencarian aktif: dengan
     * penyaring pencarian, ayat ke-n bukan lagi item ke-(n-1), sehingga tombol "Ke ayat" dan daftar
     * bookmark mendarat di ayat yang salah tanpa memberi tahu pengguna.
     */
    fun lompatKe(n: Int, tandai: Boolean = false) {
        if (n !in 1..surah.ayatCount) return
        if (tandai) vm.markRead(n)
        targetAyat = n
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        if (!focus) {
            Text("Surat Yasin — ${surah.ayatCount} ayat", style = MaterialTheme.typography.titleLarge)
            // Sumber teks ditampilkan apa adanya dari bundel: teks Arab, transliterasi, dan
            // terjemahan dicatat terpisah di berkasnya (MSI LPMQ Kemenag / transliterasi resmi
            // Kemenag / terjemah edisi 2019). Sebelumnya layar hanya menuliskan "(MSI Kemenag)"
            // di judul — hardcode yang tidak menyebut asal transliterasi maupun terjemahan, dan
            // tidak berubah bila bundelnya berubah.
            val atribusi = remember(surah) { AtribusiTeks.surah(surah) }
            if (atribusi.isNotBlank()) {
                Text(atribusi, style = MaterialTheme.typography.labelSmall)
            }
            Text("Terakhir dibaca: ${if (s.mark in 1..surah.ayatCount) "ayat ${s.mark}" else "belum ada"}",
                style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(jump, { jump = it.filter(Char::isDigit).take(2) },
                    label = { Text("Ayat") }, modifier = Modifier.weight(1f))
                Button(onClick = { jump.toIntOrNull()?.let { lompatKe(it, tandai = true) } }) { Text("Ke ayat") }
            }
            OutlinedTextField(query, { query = it }, label = { Text("Cari dalam Yasin (teks/ayat)") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            if (query.isNotBlank()) Text("${shown.size} ayat cocok", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { settings = true }) { Text("Pengaturan bacaan") }
                FilterChip(focus, { focus = !focus }, { Text("Layar penuh") })
                FilterChip(keepOn, { keepOn = !keepOn }, { Text("Tetap menyala") })
                FilterChip(showBookmarks, { showBookmarks = !showBookmarks }, { Text("Bookmark ${s.bookmarks.size}") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.mark in 1..surah.ayatCount) Button(onClick = { lompatKe(s.mark) }) { Text("Lanjut ayat ${s.mark}") }
            }
            if (showBookmarks) {
                Text("Bookmark:", style = MaterialTheme.typography.titleSmall)
                if (s.bookmarks.isEmpty()) Text("Belum ada bookmark. Tandai ayat untuk menyimpan.",
                    style = MaterialTheme.typography.labelSmall)
                s.bookmarks.forEach { b ->
                    TextButton(onClick = {
                        b.refId.toIntOrNull()?.let { n -> lompatKe(n); showBookmarks = false }
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
                            s.showLatin, s.showTrans, s.arabSize,
                            showArab = s.showArab, kategori = surah.kategori)
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
