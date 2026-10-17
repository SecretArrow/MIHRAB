package id.or.kemenag.yasintahlil.feature.tahlil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Tune
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
 * Tahlil lengkap, seluruh bagian mengalir bersambung — sama seperti Surat Yasin.
 *
 * Dahulu satu bagian tampil per layar dengan tombol "Sebelumnya/Berikutnya" beserta tab susunan
 * dan bilah kemajuannya. Permintaan pengguna (2026-10-15): *"tahlil tampilannya seperti yasin
 * langsung bersambung di bawahnya, bukan next prev"* — maka seluruh bagian kini menjadi satu
 * daftar gulir panjang; posisi baca ditandai (bukan dilingkari tombol), dan lompatannya lewat
 * baris "Ke bagian" seperti baris "Ke ayat" di Yasin.
 *
 * Dua versi susunan NU — Ringkas dan Lengkap — dapat langsung dipilih dari baris chip di kepala
 * layar ini, persis dua tab di quran.nu.or.id/tahlil; seluruh susunan (termasuk "Susunan saya")
 * tetap dapat dilihat dari Pengaturan dan penyusunnya lewat ikon Tune. Keterangan susunan — nama
 * susunan, catatan tradisi, sumber sampai sunnah.com — tinggal di layar keterangan lewat ikon Info.
 * Chip "Teks besar" di baris yang sama dihapus (permintaan pengguna, 2026-10-16): ukuran bacaan
 * cukup diatur dari Pengaturan dan mode layar penuh, bukan pilihan ketiga yang menumpuk di sini.
 */
@Composable
fun TahlilScreen(go: (String) -> Unit = {}, vm: TahlilViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val repo = rememberRepo()
    val store = rememberStore()
    // rememberSaveable: pilihan tampilan (layar penuh, panel susunan, nomor yang sedang diketik)
    // tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var susun by rememberSaveable { mutableStateOf(false) }
    var jump by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
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
    // Bagian penanda terakhir dibaca — pengganti "bagian yang sedang dibuka" pada tampilan
    // next/prev yang lama: penghitung ulangannya mengikuti bagian yang ditandai ini.
    val cur = s.items[s.idx.coerceIn(0, s.items.size - 1)]
    val selesai = s.count >= cur.ulangi

    // Buka layar pada penanda terakhir dibaca — kelanjutan posisi baca yang dahulu diberikan
    // tombol "Berikutnya". Tanpa animasi supaya pembukaan layar tidak terasa melompat sendiri,
    // dan cukup sekali: sekali sudah, guliran pengguna tidak boleh digeser lagi.
    var sudahKePenanda by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(s.items.size) {
        if (!sudahKePenanda && s.items.isNotEmpty()) {
            sudahKePenanda = true
            listState.scrollToItem(s.idx.coerceIn(0, s.items.size - 1))
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        if (!fullscreen) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("${NamaLayar.TAHLIL} — ${s.items.size} bagian",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f))
                TombolInfo { go(Routes.keterangan("tahlil")) }
            }
            // Baris penanda terakhir dibaca sekaligus menjadi tombol lompat — pola yang sama
            // dengan baris "Terakhir dibaca" di Surat Yasin.
            TextButton(onClick = { vm.jumpTo(s.idx) }) {
                Text(
                    "${NamaAntarmuka.TERAKHIR_DIBACA}: bagian ${s.idx + 1} — ${cur.judul}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(jump, { jump = it.filter(Char::isDigit).take(2) },
                    label = { Text("Bagian") }, singleLine = true,
                    modifier = Modifier.weight(1f))
                Button(onClick = { jump.toIntOrNull()?.let { vm.jumpTo(it - 1) } }) { Text("Ke bagian") }
                // Penyusun "Susunan saya": satu-satunya pengatur susunan yang memang milik
                // bacaan ini — memilih bagian dan menatanya, bukan memilih susunan jadi
                // (yang terpusat di Pengaturan bersama pilihan ibadah lainnya).
                IconButton(onClick = { susun = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Atur susunan")
                }
                // Tombol layar penuh berikon, menempel di kanan tombol "Ke bagian" — pola yang
                // sama dengan baris "Ke ayat" di Surat Yasin.
                IconButton(onClick = { fullscreen = true }) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = NamaAntarmuka.LAYAR_PENUH)
                }
            }
            BarisChip {
                // Pemilih versi tahlil NU — dua id susunan dari berkas konten (`tahlil.json`),
                // bukan susunan karangan: bila bundelnya diganti, chip yang tak dikenal tidak
                // digambar dan pilihan tinggal yang sah. Bila "Susunan saya" yang aktif, kedua
                // chip ini libur terpilih — penyusunnya tetap lewat ikon Tune.
                listOf("nu_online_ringkas" to "Ringkas", "nu_online_lengkap" to "Lengkap")
                    .forEach { (id, label) ->
                        if (s.susunan.any { it.id == id }) {
                            FilterChip(!s.modeImam && s.susunanId == id,
                                { vm.setSusunan(id) }, { Text(label) })
                        }
                    }
                // Mode Imam — bacaannya SAMA dengan Lengkap; yang bertambah hanya lencana siapa
                // yang membaca tiap bagian (kartunya sendiri), sehingga makmum tahu bagian mana
                // yang dibaca bersama. Keterangannya lewat ikon Info di kepala layar.
                FilterChip(s.modeImam, { vm.setModeImam(!s.modeImam) }, { Text("Imam") })
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { fullscreen = false }) {
                    Icon(Icons.Filled.FullscreenExit, contentDescription = "Keluar layar penuh")
                }
            }
        }
        // Penghitung hanya tampil bila penggunanya menyalakannya di Pengaturan. Ia menghitung
        // ulangan bagian yang ditandai terakhir dibaca — menandai bagian lain memindahkan
        // hitungannya ke sana, seperti halnya penghitung lama mengikuti bagian yang dibuka.
        if (tampilPenghitung) {
            Text(
                "Penghitung ulangan — bagian ${s.idx + 1}: ${cur.judul}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CounterRow(s.count, cur.ulangi,
                onPlus = { vm.setCount(s.count + 1) },
                onMinus = { vm.setCount(s.count - 1) },
                onReset = { vm.setCount(0) })
            if (selesai) Text("✓ Bagian ini sudah dihitung lengkap.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        // Peta pembagi Mode Imam (id bagian → siapa membaca) dibaca sekali dari bundel — statis
        // selama layar hidup; bila berkasnya tak terbaca, peta kosong dan lencananya tidak tampil.
        val imam = remember { repo.tahlilImam() }
        // Mode baca jarak jauh — "Layar penuh" — menaikkan ukuran Arab lewat aturan bersama yang
        // sama dengan layar Ghaib, Jenazah, Iqomah, Yasin, dan Dzikir. Chip "Teks besar" yang dahulu
        // ikut menaikkan ukuran dihapus (permintaan pengguna, 2026-10-16); ukuran lazimnya diatur
        // dari Pengaturan.
        val bacaJauh = fullscreen
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(s.items, key = { _, it -> it.id }) { i, it ->
                // Aturan cadangan teks dari `TeksBacaan` — sama dengan layar sholat, bukan salinan
                // `ifBlank { cur.arab }` yang dahulu diketik di sini.
                val (arab, latin, trans) = remember(it.id) {
                    val (ra, rl, rt) = repo.resolveArabLatinTerjemah(it.rujukanFile)
                    TeksBacaan.pilih(it, ra, rl, rt)
                }
                BacaanCard(
                        JenisBacaan.id(JenisBacaan.TAHLIL, it.id),
                        "Bagian ${i + 1} — ${it.judul}", arab, latin, trans,
                        s.showLatin, s.showTrans, UkuranBacaan.arabBacaJauh(s.arabSize, bacaJauh),
                        ulangi = it.ulangi, sumber = it.sumber,
                        showArab = s.showArab, kategori = it.kategori,
                        lencanaKanan = if (s.modeImam) imam[it.id]?.siapa.orEmpty() else "",
                        aksiEkstra = listOf(
                            AksiBacaan(
                                if (i == s.idx) "✓ Penanda terakhir dibaca" else "Tandai terakhir dibaca",
                                Icons.Filled.BookmarkAdded,
                            ) { vm.jumpTo(i) },
                        ),
                    )
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
