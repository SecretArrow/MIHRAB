package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.sholat.CaraKelompok
import id.or.kemenag.yasintahlil.core.sholat.KelompokSholat
import id.or.kemenag.yasintahlil.core.util.Saring
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun SholatScreen(go: (String) -> Unit) {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val wajib = remember { repo.sholatWajib() }
    val sunnah = remember { repo.sholatSunnah() }
    var tab by remember { mutableStateOf(0) }
    var cara by remember { mutableStateOf(CaraKelompok.WAKTU) }
    var showArab by remember { mutableStateOf(true) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var settings by remember { mutableStateOf(false) }
    // Penyaring di layar (`plan.txt`: "pencarian dan akses cepat pada halaman yang memiliki banyak
    // konten"). Panduan sholat memuat puluhan entri dalam tiga tab; kotak ini menyaring tab yang
    // sedang dibuka tanpa berpindah ke layar Pencarian.
    var q by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) {
        showArab = store.get(Store.SHOW_ARAB, true)
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
    }
    val bacaan = remember { repo.sholatBacaan() }
    val wajibTampil = remember(q, wajib) {
        Saring.tapis(q, wajib) { listOf(it.nama, it.deskripsi, it.waktu, it.rakaat) }
    }
    val sunnahTampil = remember(q, sunnah) {
        Saring.tapis(q, sunnah) { listOf(it.nama, it.deskripsi, it.waktu, it.rakaat, it.tujuan) }
    }
    val bacaanTampil = remember(q, bacaan) {
        Saring.tapis(q, bacaan) { listOf(it.judul, it.arab, it.latin, it.terjemahan, it.kapan, it.catatan) }
    }
    val kelompokSunnah = remember(sunnahTampil, cara) { KelompokSholat.kelompok(sunnahTampil, cara) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Panduan Sholat", style = MaterialTheme.typography.titleLarge)
        // Sumber panduan dibaca dari bundel (`sholat_daftar.json` → `panduan`): sebelumnya layar
        // hanya menulis "(Fasolatan Kemenag)" sebagai teks hardcode, sedangkan berkasnya mencatat
        // keterangan yang lebih tepat — termasuk bahwa hadisnya diverifikasi lewat sunnah.com.
        val panduan = remember { repo.sholatPanduan() }
        if (panduan.isNotBlank()) {
            Text("Sumber panduan: $panduan", style = MaterialTheme.typography.labelSmall)
        }
        TabRow(tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Wajib") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Sunnah") })
            Tab(tab == 2, { tab = 2 }, text = { Text("Bacaan") })
        }
        OutlinedTextField(
            q, { q = it },
            label = { Text("Cari di tab ini (nama, arti, keterangan)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        val jumlahTab = when (tab) {
            0 -> wajibTampil.size
            1 -> sunnahTampil.size
            else -> bacaanTampil.size
        }
        if (q.isNotBlank()) Text(
            "$jumlahTab hasil untuk \"${q.trim()}\"",
            style = MaterialTheme.typography.labelSmall,
        )
        if (jumlahTab == 0) {
            if (q.isBlank()) EmptyState("Tab ini kosong", "Belum ada entri pada tab ini.")
            else EmptyState(
                "Tidak ada yang cocok",
                "Tidak ada entri di tab ini yang memuat \"${q.trim()}\". Coba kata lain, atau " +
                    "kosongkan kotak pencarian.",
                labelTombol = "Kosongkan",
                onTombol = { q = "" },
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (tab == 0) items(wajibTampil) { s ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(s.nama, style = MaterialTheme.typography.titleMedium)
                        Text("${s.rakaat} • ${s.waktu}")
                        Text(s.deskripsi)
                        Row {
                            if (s.id == "ghaib") Button(onClick = { go(Routes.GHAIB) }) { Text("Buka panduan Ghaib") }
                            else if (s.id == "jenazah") Button(onClick = { go(Routes.JENAZAH) }) { Text("Buka panduan") }
                            else Button(onClick = { go("${Routes.SHOLAT_DETAIL}/${s.id}") }) { Text("Tata cara") }
                        }
                    }
                }
            }
            if (tab == 1) {
                // `plan.txt` §"Sholat Sunnah": "Kelompokkan sholat berdasarkan waktu pelaksanaan,
                // jumlah rakaat, dan tujuan." Ketiganya ditawarkan karena satu cara pengelompokan
                // tidak menjawab semua pertanyaan: "mana yang dikerjakan malam ini" berbeda dari
                // "mana yang tujuannya memohon ampunan".
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Kelompok:", style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.CenterVertically))
                        CaraKelompok.entries.forEach { c ->
                            FilterChip(cara == c, { cara = c }, { Text(c.label) })
                        }
                    }
                }
                kelompokSunnah.forEach { (nama, isi) ->
                    item(key = "k:$nama") {
                        Text("$nama (${isi.size})", style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    items(isi, key = { "s:${it.id}" }) { s ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(s.nama, style = MaterialTheme.typography.titleMedium)
                                Text("${s.rakaat} • ${s.waktu}")
                                if (s.tujuan.isNotBlank()) Text("Tujuan: ${s.tujuan}")
                                Text(s.deskripsi)
                                Button(onClick = { go("${Routes.SHOLAT_DETAIL}/${s.id}") }) { Text("Tata cara") }
                            }
                        }
                    }
                }
            }
            if (tab == 2) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)) {
                        OutlinedButton(onClick = { settings = true }) { Text("Pengaturan bacaan") }
                    }
                }
                items(bacaanTampil) { b ->
                    val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
                        Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
                    }
                    BacaanCard("bacaan:${b.id}", b.judul, a, l, t, true, true, arabSize,
                        sumber = b.sumber.ifBlank { b.kapan }, showArab = showArab, kategori = b.kategori)
                    if (b.catatan.isNotBlank()) {
                        Text(b.catatan, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
        }
    }

    if (settings) {
        BacaanSettingsSheet(
            onDismiss = { settings = false },
            showArab = showArab, onShowArab = { showArab = it; scope.launch { store.set(Store.SHOW_ARAB, it) } },
            arabSize = arabSize, onArabSize = { arabSize = it; scope.launch { store.set(Store.ARAB_SIZE, it) } },
        )
    }
}
