package id.or.kemenag.yasintahlil.feature.tahlil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.data.TahlilItem
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect

@Composable
fun TahlilScreen(vm: TahlilViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val repo = rememberRepo()
    var jamaah by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var susun by remember { mutableStateOf(false) }
    FullscreenEffect(fullscreen)

    if (s.items.isEmpty()) {
        EmptyState("Konten tahlil tidak tersedia",
            "Berkas susunan tahlil tidak ditemukan di aplikasi. Pasang ulang aplikasi bila ini terus terjadi.")
        return
    }
    val cur = s.items[s.idx.coerceIn(0, s.items.size - 1)]
    val (arab, latin, trans) = remember(cur.id) {
        repo.resolveArabLatinTerjemah(cur.rujukanFile).let {
            Triple(it.first.ifBlank { cur.arab }, it.second.ifBlank { cur.latin }, it.third.ifBlank { cur.terjemahan })
        }
    }
    val selesai = s.count >= cur.ulangi

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Tahlil — bagian ${s.idx + 1} dari ${s.items.size}", style = MaterialTheme.typography.titleLarge)
        Text("${s.namaSusunan.ifBlank { "Susunan tahlil" }}; susunan dapat berbeda menurut tradisi.",
            style = MaterialTheme.typography.labelMedium)
        // `plan.txt` §"Fitur Tahlil Lengkap" meminta susunan yang *"berdasarkan sumber atau tradisi
        // yang jelas"*. Menampilkan nama susunan saja tidak cukup: nama bukan sumber. Bila susunan
        // itu tidak mengklaim sumber apa pun (mis. "Susunan saya"), hal itu dikatakan terus terang —
        // bukan diisi rujukan karangan.
        Text(if (s.sumberSusunan.isNotBlank()) "Sumber: ${s.sumberSusunan}"
                else "Sumber: susunan ini disusun sendiri oleh Anda, bukan rangkaian dari rujukan atau tradisi tertentu.",
            style = MaterialTheme.typography.labelSmall)

        if (s.susunan.size > 1) {
            ScrollableTabRow(s.susunan.indexOfFirst { it.id == s.susunanId }.coerceAtLeast(0)) {
                s.susunan.forEachIndexed { i, su ->
                    Tab(s.susunanId == su.id, { vm.setSusunan(su.id) }, text = { Text(su.nama) })
                }
            }
        }
        LinearProgressIndicator(progress = { ((s.idx + 1).toFloat() / s.items.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.prev() }, enabled = s.idx > 0) { Text("← Sebelumnya") }
            Button(onClick = { vm.next() }, enabled = s.idx < s.items.size - 1) { Text("Berikutnya →") }
            FilterChip(jamaah, { jamaah = !jamaah }, { Text("Jamaah") })
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { settings = true }) { Text("Pengaturan bacaan") }
            OutlinedButton(onClick = { susun = true }) { Text("Sesuaikan susunan") }
        }
        CounterRow(s.count, cur.ulangi,
            onPlus = { vm.setCount(s.count + 1) },
            onMinus = { vm.setCount(s.count - 1) },
            onReset = { vm.setCount(0) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.next() }, enabled = s.idx < s.items.size - 1 && selesai) {
                Text(if (selesai) "✓ Tandai selesai →" else "Selesaikan dulu")
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                if (jamaah) {
                    if (s.showArab && arab.isNotBlank()) ArabText(arab, (s.arabSize + 8).coerceAtMost(44f))
                    if (s.showTrans && trans.isNotBlank()) Text(trans, style = MaterialTheme.typography.headlineSmall)
                } else {
                    BacaanCard("tahlil:${cur.id}", cur.judul, arab, latin, trans,
                        s.showLatin, s.showTrans, s.arabSize, ulangi = cur.ulangi, sumber = cur.sumber,
                        showArab = s.showArab, kategori = cur.kategori)
                }
                if (selesai) Text("✓ Selesai — lanjut ke bagian berikutnya.",
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                Text("Semua bagian:", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp))
            }
            items(s.items) { it ->
                val i = s.items.indexOf(it)
                TextButton(onClick = { vm.jumpTo(i) }) {
                    Text("${i + 1}. ${it.judul}${if (it.ulangi > 1) " (${it.ulangi}x)" else ""}")
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

    if (susun) {
        SusunanSayaSheet(
            semua = s.semuaBagian,
            awal = s.items.map { it.id },
            ada = s.adaSusunanSaya || s.pilihan.isNotEmpty(),
            onDismiss = { susun = false },
            onSimpan = { vm.simpanSusunanSaya(it); susun = false },
            onHapus = { vm.hapusSusunanSaya(); susun = false },
        )
    }
}

/**
 * Penyusun "Susunan saya" (`plan.txt`: *"Sediakan susunan bacaan yang dapat dikonfigurasi"*):
 * pengguna memilih bagian mana yang ikut dibaca dan mengatur urutannya. Yang berubah hanya
 * pilihan dan urutan — teks tiap bagian tetap dibaca dari berkas sumbernya beserta atribusinya,
 * sehingga tidak ada teks keagamaan yang disusun ulang di sini.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SusunanSayaSheet(
    semua: List<TahlilItem>,
    awal: List<String>,
    ada: Boolean,
    onDismiss: () -> Unit,
    onSimpan: (List<String>) -> Unit,
    onHapus: () -> Unit,
) {
    var kerja by remember(awal) { mutableStateOf(awal) }
    val byId = remember(semua) { semua.associateBy { it.id } }
    val terpilih = kerja.mapNotNull { byId[it] }
    val belum = semua.filterNot { kerja.contains(it.id) }

    fun tukar(i: Int, j: Int) {
        if (i in kerja.indices && j in kerja.indices) {
            kerja = kerja.toMutableList().also { it[i] = kerja[j]; it[j] = kerja[i] }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(SusunanSaya.NAMA, style = MaterialTheme.typography.titleMedium)
            Text("Pilih bagian yang dibaca lalu atur urutannya. Teks tiap bagian tetap diambil dari " +
                    "sumbernya; susunan ini buatan Anda sendiri, bukan klaim satu-satunya susunan " +
                    "yang benar dan bukan pula susunan suatu tradisi.",
                style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { onSimpan(kerja) }, enabled = kerja.isNotEmpty()) { Text("Simpan susunan") }
                if (ada) OutlinedButton(onClick = onHapus) { Text("Kembalikan ke susunan lazim") }
            }
            if (kerja.isEmpty()) {
                Text("Belum ada bagian dipilih — tambahkan dari daftar di bawah.",
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp).padding(top = 8.dp)) {
                itemsIndexed(terpilih) { i, it ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. ${it.judul}", modifier = Modifier.weight(1f))
                        TextButton(onClick = { tukar(i, i - 1) }, enabled = i > 0) { Text("▲") }
                        TextButton(onClick = { tukar(i, i + 1) }, enabled = i < terpilih.size - 1) { Text("▼") }
                        TextButton(onClick = { kerja = kerja - it.id }) { Text("Buang") }
                    }
                }
                if (belum.isNotEmpty()) {
                    item {
                        Text("Belum dipakai:", style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    itemsIndexed(belum) { _, it ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(it.judul, modifier = Modifier.weight(1f))
                            TextButton(onClick = { kerja = kerja + it.id }) { Text("Tambah") }
                        }
                    }
                }
            }
        }
    }
}
