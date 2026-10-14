package id.or.kemenag.yasintahlil.feature.dzikir

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PesanKonten
import id.or.kemenag.yasintahlil.core.util.Saring
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun DzikirScreen(go: (String) -> Unit = {}, vm: DzikirViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val groups = vm.groups
    // rememberSaveable: tab dan mode layar penuh pilihan pengguna tidak hilang saat diputar ulang.
    var tab by rememberSaveable { mutableStateOf(0) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    // Kata kunci penyaring di layar ini (`plan.txt`: "pencarian dan akses cepat pada halaman yang
    // memiliki banyak konten"). Pustaka dzikir memuat puluhan entri dalam lima kelompok; sebelum
    // ada kotak ini satu-satunya cara menemukan satu doa adalah menggulir tiap tab.
    var q by rememberSaveable { mutableStateOf("") }
    FullscreenEffect(fullscreen)

    if (groups.isEmpty()) {
        EmptyState("Konten dzikir tidak tersedia",
            PesanKonten.berkasTidakAda("kumpulan dzikir"))
        return
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(NamaLayar.DZIKIR, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("dzikir")) }
        }
        BarisChip(Modifier.padding(vertical = 4.dp)) {
            OutlinedButton(onClick = { settings = true }) { Text(NamaAntarmuka.PENGATURAN_BACAAN) }
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH) })
        }
        // `PrimaryScrollableTabRow` (bukan `ScrollableTabRow` yang usang): indikatornya kini batang
        // membulat 24 dp di bawah label, sesuai varian tab M3 terbaru. Tetap dapat digulir karena
        // kelompok dzikir lebih banyak daripada lebar layar.
        PrimaryScrollableTabRow(tab) {
            groups.forEachIndexed { i, g -> Tab(tab == i, { tab = i }, text = { Text(g.judul) }) }
        }
        val group = groups[tab.coerceIn(0, groups.size - 1)]
        val tersaring = Saring.tapis(q, group.items) {
            listOf(it.judul, it.arab, it.latin, it.terjemahan)
        }
        OutlinedTextField(
            q, { q = it },
            label = { Text("Cari di kelompok \"${group.judul}\"") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        if (tersaring.isEmpty()) {
            if (q.isBlank()) EmptyState(
                "Kelompok ini kosong",
                "Belum ada bacaan pada kelompok \"${group.judul}\".",
            ) else EmptyState(
                "Tidak ada bacaan yang cocok",
                "Tidak ada entri di kelompok ini yang memuat \"${q.trim()}\". Coba kata lain, " +
                    "atau kosongkan kotak pencarian untuk melihat seluruh kelompok.",
                labelTombol = NamaAntarmuka.KOSONGKAN,
                onTombol = { q = "" },
            )
            return@Column
        }
        if (q.isNotBlank()) Text(
            "${tersaring.size} dari ${group.items.size} bacaan cocok dengan \"${q.trim()}\"",
            style = MaterialTheme.typography.labelSmall,
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(tersaring) { it ->
                // Id kartu dzikir memakai id kelompoknya sebagai jenis — daftar kelompok datang dari
                // bundel konten, jadi jenisnya tidak dapat ditulis tetap di sini.
                val key = JenisBacaan.id(group.id, it.judul)
                val c = s.counts[key] ?: 0
                // Mode layar penuh berarti bacaan dibaca dari jauh — aturan yang sama dipakai
                // layar Ghaib, Jenazah, Iqomah, dan Tahlil; lihat `UkuranBacaan.arabBacaJauh`.
                BacaanCard(key, it.judul, it.arab, it.latin, it.terjemahan,
                    s.showLatin, s.showTrans, UkuranBacaan.arabBacaJauh(s.arabSize, fullscreen),
                    ulangi = it.ulangi, sumber = it.sumber,
                    showArab = s.showArab, kategori = it.kategori)
                // Hitungan ditampilkan untuk semua bacaan, bukan hanya yang ulangi > 1: progresnya
                // tersimpan di Room, jadi pengguna bisa melihat/mengulang meski targetnya satu kali.
                CounterRow(c, it.ulangi,
                    onPlus = { vm.setCount(key, c + 1) },
                    onMinus = { vm.setCount(key, c - 1) },
                    onReset = { vm.reset(key) })
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
