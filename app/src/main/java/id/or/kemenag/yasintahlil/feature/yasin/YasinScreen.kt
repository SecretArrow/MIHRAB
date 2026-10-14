package id.or.kemenag.yasintahlil.feature.yasin

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.data.AtribusiTeks
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Surat Yā-Sīn.
 *
 * Kepala layarnya kini minimalis: judul berikut ikon Info, satu baris penanda terakhir dibaca
 * yang sekaligus menjadi tombol lompat, dan baris "Ayat … / Ke ayat" yang diakhiri tombol layar
 * penuh berikon. Aksi per-ayat (salin, bagikan, favorit, tandai terakhir dibaca, bookmark)
 * dibuka dengan **menekan lama** kartu ayatnya; nomor ayat di luar kartu dihapus karena judul
 * kartu sudah menyebutnya.
 *
 * Mencari ayat bukan lagi tugas layar ini — kotak carinya yang lama menyaring daftar dan membuat
 * tombol "Ke ayat"/bookmark mendarat di ayat yang salah sebelum diperbaiki. Pencarian kini
 * terpusat di layar Pencarian yang menelusuri seluruh konten aplikasi.
 */
@Composable
fun YasinScreen(go: (String) -> Unit = {}, vm: YasinViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val surah = vm.surah
    val ctx = LocalContext.current
    val store = rememberStore()
    // rememberSaveable: mode baca dan nomor yang sedang diketik tidak hilang saat diputar ulang.
    // "Tetap menyala" kini preferensi global (Store.KEEP_ON, diatur di tab Pengaturan) — ia bertahan
    // bahkan saat proses aplikasi mati, bukan sekadar selama rotasi.
    var focus by rememberSaveable { mutableStateOf(false) }
    var jump by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keepOn by store.flow(Store.KEEP_ON, BawaanPreferensi.TETAP_MENYALA)
        .collectAsStateWithLifecycle(BawaanPreferensi.TETAP_MENYALA)

    FullscreenEffect(focus)
    DisposableEffect(keepOn) {
        val w = (ctx as? Activity)?.window
        if (keepOn) w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Ayat yang sedang dituju tombol "Ke ayat" / baris penanda terakhir dibaca. Diselesaikan di
    // efek ini, bukan langsung, agar gulirannya berjalan setelah daftar tersusun.
    var targetAyat by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(targetAyat) {
        val n = targetAyat ?: return@LaunchedEffect
        val idx = surah.verses.indexOfFirst { it.nomor == n }
        if (idx >= 0) listState.animateScrollToItem(idx)
        targetAyat = null
    }

    /**
     * Lompat ke ayat [n]. Indeksnya dicari dari daftar yang sedang tampil lewat nomor ayatnya —
     * bukan `n - 1` — sehingga tetap benar berapa pun susunan daftarnya.
     */
    fun lompatKe(n: Int, tandai: Boolean = false) {
        if (n !in 1..surah.ayatCount) return
        if (tandai) vm.markRead(n)
        targetAyat = n
    }

    // Asal teks Arab/transliterasi/terjemahan dibaca dari bundel untuk menentukan ada-tidaknya
    // keterangan yang layak dibuka; kalimatnya yang utuh tinggal di layar keterangan, sehingga
    // badan layar tidak lagi dibuka oleh tiga baris atribusi.
    val atribusi = remember(surah) { AtribusiTeks.surah(surah) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        if (!focus) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${NamaLayar.YASIN} — ${surah.ayatCount} ayat",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (atribusi.isNotBlank()) {
                    TombolInfo { go(Routes.keterangan("yasin")) }
                }
            }
            // Baris penanda terakhir dibaca sekaligus menjadi tombol lompat: satu baris, satu
            // maksud — tanpa chip "Lanjut ayat N" tambahan yang mengulang informasi yang sama.
            if (s.mark in 1..surah.ayatCount) {
                TextButton(onClick = { lompatKe(s.mark) }) {
                    Text(
                        "${NamaAntarmuka.TERAKHIR_DIBACA}: ayat ${s.mark}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                Text(
                    "${NamaAntarmuka.TERAKHIR_DIBACA}: belum ada",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(jump, { jump = it.filter(Char::isDigit).take(2) },
                    label = { Text("Ayat") }, singleLine = true,
                    modifier = Modifier.weight(1f))
                Button(onClick = { jump.toIntOrNull()?.let { lompatKe(it, tandai = true) } }) { Text("Ke ayat") }
                // Tombol layar penuh berikon, menempel di kanan tombol "Ke ayat": kendali baca
                // yang paling sering dipakai berada satu baris dengan lompatan ayat.
                IconButton(onClick = { focus = true }) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = NamaAntarmuka.LAYAR_PENUH)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { focus = false }) {
                    Icon(Icons.Filled.FullscreenExit, contentDescription = "Keluar layar penuh")
                }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(surah.verses) { v ->
                val marked = s.bookmarks.any { it.refId == v.nomor.toString() }
                // Mode fokus menyembunyikan bilah bawah dan berarti bacaan dibaca dari
                // jauh, jadi ukuran Arabnya dinaikkan lewat aturan bersama yang sama
                // dengan layar Ghaib, Jenazah, Iqomah, dan Tahlil.
                BacaanCard(
                    JenisBacaan.id(JenisBacaan.YASIN, v.nomor.toString()),
                    "Ayat ${v.nomor}", v.arab, v.latin, v.terjemahan,
                    s.showLatin, s.showTrans, UkuranBacaan.arabBacaJauh(s.arabSize, focus),
                    showArab = s.showArab, kategori = surah.kategori,
                    aksiEkstra = listOf(
                        AksiBacaan(
                            if (s.mark == v.nomor) "✓ Penanda terakhir dibaca" else "Tandai terakhir dibaca",
                            Icons.Filled.BookmarkAdded,
                        ) { vm.markRead(v.nomor) },
                        AksiBacaan(
                            if (marked) "Hapus bookmark" else "Bookmark",
                            if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        ) { vm.toggleBookmark(v.nomor) },
                    ),
                )
            }
        }
    }
}
