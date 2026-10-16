package id.or.kemenag.yasintahlil.feature.dzikir

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Satu kelompok dzikir: seluruh bacaannya mengalir bersambung dalam satu daftar gulir —
 * bukan tab lagi, dan bukan pula satu entri per layar.
 *
 * Judulnya dibaca dari bundel konten (kelompok dinamai berkasnya), sama seperti rincian
 * per-sholat yang dinamai daftarnya — karena itu layar ini sengaja tidak punya konstanta di
 * [id.or.kemenag.yasintahlil.core.util.NamaLayar]: tidak ada nama yang bisa menyimpang,
 * sebab tidak ada yang dieja di kode.
 *
 * Penghitung ulangan tetap opt-in dari Pengaturan ("Penghitung ulangan") dan hitungan tiap
 * bacaan tetap tersimpan di Room — keduanya menempel di layar ini karena menghitung tasbih
 * memang terjadi pada bacaannya.
 */
@Composable
fun DzikirGroupScreen(id: String, go: (String) -> Unit = {}, vm: DzikirViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val store = rememberStore()
    // rememberSaveable: mode layar penuh tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val tampilPenghitung by store.flow(Store.SHOW_COUNTER, BawaanPreferensi.TAMPIL_PENGHITUNG)
        .collectAsStateWithLifecycle(BawaanPreferensi.TAMPIL_PENGHITUNG)
    FullscreenEffect(fullscreen)

    val group = remember(id) { vm.groups.firstOrNull { it.id == id } }
    if (group == null) {
        EmptyState(
            "Kelompok dzikir tidak tersedia",
            "Kelompok ini mungkin sudah diganti atau dibuang oleh bundel konten yang terpasang. " +
                "Buka kembali menu ${NamaLayar.DZIKIR} lalu pilih kelompok yang tersedia.",
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        if (!fullscreen) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(group.judul, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { fullscreen = true }) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = NamaAntarmuka.LAYAR_PENUH)
                }
                TombolInfo { go(Routes.keterangan("dzikir")) }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { fullscreen = false }) {
                    Icon(Icons.Filled.FullscreenExit, contentDescription = "Keluar layar penuh")
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(group.items) { it ->
                // Id kartu dzikir memakai id kelompoknya sebagai jenis — daftar kelompok datang dari
                // bundel konten, jadi jenisnya tidak dapat ditulis tetap di sini.
                val key = JenisBacaan.id(group.id, it.judul)
                val c = s.counts[key] ?: 0
                // Mode layar penuh berarti bacaan dibaca dari jauh — aturan yang sama dipakai
                // layar Ghaib, Jenazah, Iqomah, Tahlil, dan Yasin; lihat `UkuranBacaan.arabBacaJauh`.
                BacaanCard(key, it.judul, it.arab, it.latin, it.terjemahan,
                    s.showLatin, s.showTrans, UkuranBacaan.arabBacaJauh(s.arabSize, fullscreen),
                    ulangi = it.ulangi, sumber = it.sumber,
                    showArab = s.showArab, kategori = it.kategori, ayat = it.ayat)
                // Hitungan tiap bacaan tetap tersimpan di Room untuk semua entri, bukan hanya yang
                // ulangi > 1 — penghitungnya sendiri hanya tampil bila dinyalakan di Pengaturan,
                // sehingga layar berpuluhan bacaan ini tidak dipenuhi deretan tombol tambah/kurang.
                if (tampilPenghitung) CounterRow(c, it.ulangi,
                    onPlus = { vm.setCount(key, c + 1) },
                    onMinus = { vm.setCount(key, c - 1) },
                    onReset = { vm.reset(key) })
            }
        }
    }
}
