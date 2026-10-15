package id.or.kemenag.yasintahlil.feature.tahlil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import id.or.kemenag.yasintahlil.core.data.TahlilItem
import id.or.kemenag.yasintahlil.core.data.TeksBacaan
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PesanKonten
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Tahlil lengkap, satu bagian per layar.
 *
 * Badan layarnya kini murni bacaan: keterangan susunan (nama susunan, catatan tradisi, sumber
 * sampai sunnah.com) yang dahulu menumpuk sebagai kartu teks di atasnya tinggal di layar
 * keterangan — dibuka lewat ikon Info pada baris judulnya. Penghitung ulangannya pun disembunyikan
 * dari layar; ia opt-in lewat "Penghitung ulangan" di tab Pengaturan, sehingga tampilan tetap
 * bersih bagi yang membaca tanpa menghitung. Tombol penuh "Tandai selesai" yang mengulang
 * tugas tombol "Berikutnya" juga sudah dibubarkan — cukup satu jalan menuju bagian berikutnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TahlilScreen(go: (String) -> Unit = {}, vm: TahlilViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val repo = rememberRepo()
    val store = rememberStore()
    // rememberSaveable: pilihan tampilan (mode jamaah, layar penuh, panel susunan) tidak hilang
    // saat layar diputar ulang.
    var jamaah by rememberSaveable { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var susun by rememberSaveable { mutableStateOf(false) }
    var daftar by remember { mutableStateOf(false) }
    // Penghitung ulangan dibaca reaktif dari preferensi terpusat: menyalakannya di tab Pengaturan
    // langsung memunculkannya di sini tanpa membuka ulang layar.
    val tampilPenghitung by store.flow(Store.SHOW_COUNTER, BawaanPreferensi.TAMPIL_PENGHITUNG)
        .collectAsStateWithLifecycle(BawaanPreferensi.TAMPIL_PENGHITUNG)
    FullscreenEffect(fullscreen)

    if (s.items.isEmpty()) {
        EmptyState("Konten tahlil tidak tersedia",
            PesanKonten.berkasTidakAda("susunan tahlil"))
        return
    }
    val cur = s.items[s.idx.coerceIn(0, s.items.size - 1)]
    val (arab, latin, trans) = remember(cur.id) {
        // Aturan cadangan teks dari `TeksBacaan` — sama dengan layar sholat, bukan salinan
        // `ifBlank { cur.arab }` yang dahulu diketik di sini.
        val (ra, rl, rt) = repo.resolveArabLatinTerjemah(cur.rujukanFile)
        TeksBacaan.pilih(cur, ra, rl, rt)
    }
    val selesai = s.count >= cur.ulangi

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("${NamaLayar.TAHLIL} — bagian ${s.idx + 1} dari ${s.items.size}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("tahlil")) }
        }
        // Keterangan susunan — nama susunan, catatan tradisi, sumber hingga sunnah.com — tidak lagi
        // menumpuk di sini: seluruhnya tinggal di layar keterangan yang dibuka ikon Info di atas,
        // dibaca ulang dari bundel yang sama. Susunan yang sedang aktif tetap terlihat dari tab
        // susunannya di bawah, dan sumbernya di sana disebut per susunan.

        if (s.susunan.size > 1) {
            // `PrimaryScrollableTabRow` (bukan `ScrollableTabRow` yang usang): indikatornya kini
            // batang membulat 24 dp di bawah label, sesuai varian tab M3 terbaru.
            PrimaryScrollableTabRow(s.susunan.indexOfFirst { it.id == s.susunanId }.coerceAtLeast(0)) {
                s.susunan.forEachIndexed { i, su ->
                    Tab(s.susunanId == su.id, { vm.setSusunan(su.id) }, text = { Text(su.nama) })
                }
            }
        }
        LinearProgressIndicator(progress = { ((s.idx + 1).toFloat() / s.items.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
        // Navigasi antarbagian: tombol langkah mengapit chip mode, semuanya pada baris yang dapat
        // digulir supaya tak pernah meluber di layar sempit.
        BarisChip(Modifier.padding(bottom = 4.dp)) {
            OutlinedButton(onClick = { vm.prev() }, enabled = s.idx > 0) { Text(NamaAntarmuka.SEBELUMNYA) }
            Button(onClick = { vm.next() }, enabled = s.idx < s.items.size - 1) { Text(NamaAntarmuka.BERIKUTNYA) }
            FilterChip(jamaah, { jamaah = !jamaah }, { Text("Jamaah") })
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH) })
            OutlinedButton(onClick = { susun = true }) { Text("Sesuaikan susunan") }
            OutlinedButton(onClick = { daftar = true }) { Text("Semua bagian") }
        }
        // Penghitung hanya tampil bila penggunanya menyalakannya di Pengaturan. Tanpa penghitung,
        // alur bacannya cukup "Berikutnya" setelah selesai — hitungan yang lama tetap tersimpan
        // dan penanda "✓ Selesai" di bawah kartu tetap mengikuti hitungan itu.
        if (tampilPenghitung) CounterRow(s.count, cur.ulangi,
            onPlus = { vm.setCount(s.count + 1) },
            onMinus = { vm.setCount(s.count - 1) },
            onReset = { vm.setCount(0) })
        // Dua mode berarti bacaan dibaca dari jauh — "Jamaah" (dibaca bersama) dan "Layar penuh" —
        // dan keduanya menaikkan ukuran Arab lewat aturan bersama yang sama dengan layar Ghaib,
        // Jenazah, Iqomah, Yasin, dan Dzikir. Sebelumnya hanya mode jamaah yang menaikkannya,
        // sehingga layar penuh di sini justru menampilkan teks lebih kecil daripada di layar lain.
        val bacaJauh = jamaah || fullscreen
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                if (jamaah) {
                    if (s.showArab && arab.isNotBlank()) ArabText(arab, UkuranBacaan.arabBacaJauh(s.arabSize, bacaJauh))
                    if (s.showTrans && trans.isNotBlank()) Text(trans, style = MaterialTheme.typography.headlineSmall)
                } else {
                    BacaanCard(JenisBacaan.id(JenisBacaan.TAHLIL, cur.id), cur.judul, arab, latin, trans,
                        s.showLatin, s.showTrans, UkuranBacaan.arabBacaJauh(s.arabSize, bacaJauh),
                        ulangi = cur.ulangi, sumber = cur.sumber,
                        showArab = s.showArab, kategori = cur.kategori)
                }
                if (selesai) Text("✓ Selesai — lanjut ke bagian berikutnya.",
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // Daftar seluruh bagian menjadi panel tersendiri: dahulu ia menumpuk di bawah kartu bacaan dan
    // mendorong bagian yang sedang dibaca keluar tampakan.
    if (daftar) {
        ModalBottomSheet(onDismissRequest = { daftar = false },
            sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Text("Semua bagian", style = MaterialTheme.typography.titleLarge)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(s.items) { it ->
                        val i = s.items.indexOf(it)
                        TextButton(onClick = { vm.jumpTo(i); daftar = false }) {
                            Text("${i + 1}. ${it.judul}${if (it.ulangi > 1) " (${it.ulangi}x)" else ""}")
                        }
                    }
                }
            }
        }
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
