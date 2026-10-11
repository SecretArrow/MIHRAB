package id.or.kemenag.yasintahlil.feature.favorit

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
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun FavoritScreen(go: (String) -> Unit, vm: FavoritViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Favorit, Riwayat & Terakhir dibaca", style = MaterialTheme.typography.titleLarge)
        if (s.last.isNotBlank()) {
            Text("Terakhir dibaca: ${s.last}")
            Button(onClick = { go(ruteOf(s.last.substringBefore(':'))) }) { Text("Lanjutkan") }
        }
        Text("Favorit (${s.favorites.size}):", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        if (s.favorites.isEmpty()) Text("Belum ada favorit. Tandai dengan tombol ☆ di tiap bacaan.",
            style = MaterialTheme.typography.labelSmall)
        LazyColumn(Modifier.weight(1f)) {
            items(s.favorites, key = { "${it.kind}:${it.refId}" }) { f ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(f.title.ifBlank { "${f.kind}:${f.refId}" }, modifier = Modifier.weight(1f))
                    TextButton(onClick = { go(ruteOf(f.kind)) }) { Text("Buka") }
                    TextButton(onClick = { vm.remove(f) }) { Text("Hapus") }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Riwayat terbaru:", style = MaterialTheme.typography.titleMedium)
            if (s.history.isNotEmpty()) TextButton(onClick = { vm.clearHistory() }) { Text("Bersihkan") }
        }
        if (s.history.isEmpty()) Text("Belum ada riwayat.", style = MaterialTheme.typography.labelMedium)
        s.history.take(12).forEach { h ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${h.kind}: ${h.title}", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { go(ruteOf(h.kind)) }) { Text("Buka") }
            }
        }
    }
}

private fun ruteOf(kind: String) = when (kind) {
    "yasin" -> Routes.YASIN
    "tahlil" -> Routes.TAHLIL
    "dzikir", "doa", "jawab" -> Routes.DZIKIR
    "bacaan", "niat", "langkah", "ghaib", "jenazah" -> Routes.SHOLAT
    "adzan", "iqomah" -> Routes.ADZAN
    else -> Routes.HOME
}
