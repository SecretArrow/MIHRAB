package id.or.kemenag.yasintahlil.feature.favorit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.RuteKonten

@Composable
fun FavoritScreen(go: (String) -> Unit, vm: FavoritViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    // Id kelompok dzikir datang dari keadaan layar (dibaca `FavoritViewModel` dari bundel konten),
    // bukan dari pembacaan di sini yang kegagalannya ditelan menjadi himpunan kosong: dahulu itu
    // membuat tiap baris dzikir menampilkan "Layar tujuan tidak dikenal" seolah tujuannya memang
    // tidak ada, padahal yang gagal adalah membaca bundelnya.
    val grupDzikir = s.grupDzikir
    // Satu daftar bergulir untuk seluruh bagian: favorit, bookmark, dan riwayat bisa panjang,
    // sehingga tak satu pun bagian boleh mendorong bagian lain keluar layar.
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Text("${NamaLayar.FAVORIT} — Bookmark & terakhir dibaca",
                style = MaterialTheme.typography.titleLarge)
        }
        // Kegagalan membaca bundel konten diberitahukan di atas daftar: tanpa ini baris dzikir di
        // bawah tampak seperti bacaan yang tujuannya memang tak dikenal, bukan bundel yang rusak.
        s.dzikirGagal?.let { pesan ->
            item {
                Text(pesan, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        if (s.last.isNotBlank()) {
            item {
                Text("${NamaAntarmuka.TERAKHIR_DIBACA}: ${s.last}")
                AksiBuka(RuteKonten.tujuanTerakhir(s.last, grupDzikir), NamaAntarmuka.LANJUTKAN, go)
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
        // Kunci `kind:refId`: `FavoritViewModel` sudah menyaring riwayat agar satu bacaan hanya
        // menempati satu baris, sehingga kuncinya pasti unik — bukan lagi sekadar menghindari
        // keluhan Compose, tetapi memang satu baris per bacaan. Jatah tampilnya pun sudah dipotong
        // ViewModel menurut `BatasRiwayat`, jadi layar tidak memotongnya lagi di sini.
        items(s.history, key = { "h:${it.kind}:${it.refId}" }) { h ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${h.kind}: ${h.title}", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f))
                AksiBuka(RuteKonten.tujuan(h.kind, grupDzikir), NamaAntarmuka.BUKA, go)
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
    // Tujuan dihitung sekali: dahulu nama tujuan ditulis di sini **dan** keterangan "tidak dikenal"
    // ditulis lagi oleh [AksiBuka] ketika rutenya kosong, sehingga satu baris yang tak punya tujuan
    // menampilkan kalimat itu dua kali berturut-turut.
    val tujuan = RuteKonten.tujuan(kind, grupDzikir)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(judul)
            tujuan?.let { Text(it.nama, style = MaterialTheme.typography.labelSmall) }
        }
        AksiBuka(tujuan, NamaAntarmuka.BUKA, go)
        TextButton(onClick = onHapus) { Text(NamaAntarmuka.HAPUS) }
    }
}

/**
 * Tombol menuju layar asal sebuah bacaan tersimpan.
 *
 * Tujuannya sudah diputuskan pemanggil lewat [RuteKonten] — bukan dihitung di sini — supaya aturan
 * "jenis ini menuju layar mana" hanya ada satu, sama untuk favorit, bookmark, riwayat, maupun
 * "terakhir dibaca" di beranda.
 *
 * Bila jenis itu tidak punya layar sendiri, yang tampil adalah keterangan — bukan tombol yang
 * membuka layar lain. Tombol yang membawa pengguna ke tempat yang salah membuat isinya tampak
 * hilang, dan itu lebih buruk daripada tidak ada tombol sama sekali.
 */
@Composable
private fun AksiBuka(tujuan: RuteKonten.TujuanKonten?, label: String, go: (String) -> Unit) {
    if (tujuan != null) {
        TextButton(onClick = { go(tujuan.rute) }) { Text(label) }
    } else {
        Text(RuteKonten.TIDAK_DIKENAL, style = MaterialTheme.typography.labelSmall)
    }
}
