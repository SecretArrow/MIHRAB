package id.or.kemenag.yasintahlil.feature.favorit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    // Satu daftar bergulir untuk seluruh bagian: favorit, bookmark, dan riwayat bisa panjang,
    // sehingga tak satu pun bagian boleh mendorong bagian lain keluar layar.
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Text("Favorit, Bookmark, Riwayat & Terakhir dibaca",
                style = MaterialTheme.typography.titleLarge)
        }
        if (s.last.isNotBlank()) {
            item {
                Text("Terakhir dibaca: ${s.last}")
                AksiBuka(s.last.substringBefore(':'), grupDzikir, "Lanjutkan", go)
            }
        }

        item {
            Text("Favorit (${s.favorites.size}):", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
        }
        if (s.favorites.isEmpty()) item {
            Text("Belum ada favorit. Tandai dengan tombol ☆ di tiap bacaan.",
                style = MaterialTheme.typography.labelSmall)
        }
        items(s.favorites, key = { "f:${it.kind}:${it.refId}" }) { f ->
            BarisTersimpan(f.title.ifBlank { "${f.kind}:${f.refId}" }, f.kind, grupDzikir, go,
                onHapus = { vm.remove(f) })
        }

        item {
            Text("Bookmark (${s.bookmarks.size}):", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
        }
        if (s.bookmarks.isEmpty()) item {
            Text("Belum ada bookmark. Tandai ayat dengan tombol ☆ Bookmark di layar bacaan.",
                style = MaterialTheme.typography.labelSmall)
        }
        items(s.bookmarks, key = { "b:${it.kind}:${it.refId}" }) { b ->
            BarisTersimpan(b.label.ifBlank { "${b.kind}:${b.refId}" }, b.kind, grupDzikir, go,
                onHapus = { vm.removeBookmark(b) })
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Riwayat terbaru:", style = MaterialTheme.typography.titleMedium)
                if (s.history.isNotEmpty()) TextButton(onClick = { vm.clearHistory() }) { Text("Bersihkan") }
            }
        }
        if (s.history.isEmpty()) item {
            Text("Belum ada riwayat.", style = MaterialTheme.typography.labelMedium)
        }
        // Tanpa kunci: satu bacaan bisa muncul berkali-kali di riwayat, dan kunci ganda akan
        // membuat Compose mengeluh soal item yang sama.
        itemsIndexed(s.history.take(12)) { _, h ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${h.kind}: ${h.title}", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f))
                AksiBuka(h.kind, grupDzikir, "Buka", go)
            }
        }
    }
}

/** Satu baris favorit atau bookmark: judul, layar tujuannya, tombol buka dan hapus. */
@Composable
private fun BarisTersimpan(
    judul: String,
    kind: String,
    grupDzikir: Set<String>,
    go: (String) -> Unit,
    onHapus: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(judul)
            Text(RuteKonten.namaTujuan(kind, grupDzikir) ?: "Layar tujuan tidak dikenal",
                style = MaterialTheme.typography.labelSmall)
        }
        AksiBuka(kind, grupDzikir, "Buka", go)
        TextButton(onClick = onHapus) { Text("Hapus") }
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
