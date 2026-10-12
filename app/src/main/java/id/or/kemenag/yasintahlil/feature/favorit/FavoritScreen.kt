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
import id.or.kemenag.yasintahlil.navigation.RuteKonten

@Composable
fun FavoritScreen(go: (String) -> Unit, vm: FavoritViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val repo = rememberRepo()
    // Id kelompok dzikir dibaca dari bundel konten, bukan ditulis tetap di sini: kelompok yang
    // ditambahkan bundel otomatis dikenali sehingga favoritnya tidak lagi jatuh ke Beranda.
    val grupDzikir = remember(repo) {
        runCatching { repo.dzikir().map { it.id }.toSet() }.getOrDefault(emptySet())
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Favorit, Riwayat & Terakhir dibaca", style = MaterialTheme.typography.titleLarge)
        if (s.last.isNotBlank()) {
            Text("Terakhir dibaca: ${s.last}")
            AksiBuka(s.last.substringBefore(':'), grupDzikir, "Lanjutkan", go)
        }
        Text("Favorit (${s.favorites.size}):", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        if (s.favorites.isEmpty()) Text("Belum ada favorit. Tandai dengan tombol ☆ di tiap bacaan.",
            style = MaterialTheme.typography.labelSmall)
        LazyColumn(Modifier.weight(1f)) {
            items(s.favorites, key = { "${it.kind}:${it.refId}" }) { f ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(f.title.ifBlank { "${f.kind}:${f.refId}" })
                        Text(RuteKonten.namaTujuan(f.kind, grupDzikir) ?: "Layar tujuan tidak dikenal",
                            style = MaterialTheme.typography.labelSmall)
                    }
                    AksiBuka(f.kind, grupDzikir, "Buka", go)
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
                AksiBuka(h.kind, grupDzikir, "Buka", go)
            }
        }
    }
}

/**
 * Tombol menuju layar asal sebuah jenis konten.
 *
 * Bila jenis itu tidak punya layar sendiri (lihat [RuteKonten]), yang tampil adalah keterangan —
 * bukan tombol yang membuka layar lain. Tombol yang membawa pengguna ke tempat yang salah membuat
 * isinya tampak hilang, dan itu lebih buruk daripada tidak ada tombol sama sekali.
 */
@Composable
private fun AksiBuka(kind: String, grupDzikir: Set<String>, label: String, go: (String) -> Unit) {
    val rute = RuteKonten.rute(kind, grupDzikir)
    if (rute != null) {
        TextButton(onClick = { go(rute) }) { Text(label) }
    } else {
        Text("Layar tujuan tidak dikenal", style = MaterialTheme.typography.labelSmall)
    }
}
