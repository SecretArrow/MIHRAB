package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.sholat.Mazhab
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.launch

/**
 * Mode belajar langkah-demi-langkah untuk setiap sholat.
 *
 * [go] dipakai untuk hal yang **bukan** langkah bacaan tetapi tetap bagian panduan menurut
 * `plan.txt` §"Tata Cara Sholat Langkah demi Langkah": bersuci, penentuan arah kiblat, dan dzikir
 * setelah sholat. Ketiganya sudah punya layar sendiri berisi konten terverifikasi; menautkannya di
 * sini membuat pembaca langkah demi langkah tidak perlu menebak di mana mencarinya.
 */
@Composable
fun SholatDetailScreen(id: String, go: (String) -> Unit) {
    val repo = rememberRepo()
    val store = rememberStore()
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    val bacaan = remember { repo.sholatBacaan() }
    val syarat = remember { repo.sholatSyarat() }
    val info = remember { (repo.sholatWajib() + repo.sholatSunnah()).find { it.id == id } }
    // rememberSaveable: posisi langkah, mode ringkas, dan tanda selesai tidak hilang saat layar
    // diputar ulang. Kehilangan `step` berarti pengguna yang sedang di langkah ketujuh terlempar
    // kembali ke langkah pertama hanya karena memutar perangkat.
    var step by rememberSaveable { mutableStateOf(0) }
    var ringkas by rememberSaveable { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(true) }
    var selesai by rememberSaveable { mutableStateOf(false) }
    var mazhab by remember { mutableStateOf(Mazhab.DEFAULT) }
    var settings by remember { mutableStateOf(false) }
    var syaratTerbuka by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        mazhab = store.get(Store.MADHAB, Mazhab.DEFAULT)
    }
    // Progres belajar tersimpan di Room — tetap ada setelah keluar layar.
    LaunchedEffect(id) { userData.progress(KIND, id).collect { selesai = (it?.value ?: 0) > 0 } }
    // Saring bacaan menurut mazhab panduan (mis. qunut hanya mazhab Syafi'i)
    // dan cakupan sholat (mis. qunut hanya untuk Subuh).
    val steps = remember(ringkas, mazhab, id) {
        // Mode ringkas memuat langkah pokok saja. Tasyahud awal sengaja tidak ikut: ia hanya
        // relevan pada sholat lebih dari dua rakaat, sedangkan ringkasan harus berlaku untuk
        // semua sholat. Keduanya langkah tersendiri — lihat `tasyahud_awal` dan `tasyahud_akhir`.
        val base = if (ringkas) bacaan.filter { it.id in listOf("niat", "takbir", "fatihah", "surat_setelah_fatihah", "rukuk", "sujud", "tasyahud_akhir", "salam") }
        else bacaan
        base.filter {
            (it.mazhab == null || it.mazhab == mazhab) &&
                (it.sholatScope.isEmpty() || id in it.sholatScope)
        }
    }
    // Nama mazhab dari aturan bersama: rantai `when` di sini dahulu menyalin labelnya, sehingga
    // mengubah satu label di layar Pengaturan tidak ikut mengubah kepala layar ini.
    val mazhabLabel = Mazhab.label(mazhab)
    // Jaga indeks langkah tetap sah bila daftar menyusut setelah ganti mazhab.
    LaunchedEffect(steps.size) { if (step > steps.size - 1) step = (steps.size - 1).coerceAtLeast(0) }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(info?.nama ?: id, style = MaterialTheme.typography.titleLarge)
        Text("${info?.rakaat.orEmpty()} • ${info?.waktu.orEmpty()}")
        // Sholat wajib tidak punya kolom tujuan, jadi baris ini hanya muncul untuk sholat sunnah.
        info?.tujuan?.takeIf { it.isNotBlank() }?.let { Text("Tujuan: $it") }
        info?.niat?.let { n ->
            BacaanCard("niat:$id", "Niat", n.arab, n.latin, n.terjemahan, true, true, arabSize,
                showArab = showArab, kategori = n.kategori)
        }
        if (info?.niat == null) {
            info?.niatCatatan?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (info?.deskripsi != null) Text(info.deskripsi)
        info?.ketentuan?.takeIf { it.isNotEmpty() }?.let { ket ->
            Text("Ketentuan:", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp))
            ket.forEach { k ->
                Text("• $k", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp))
            }
        }
        // --- Sebelum langkah pertama ---------------------------------------------------------
        // `plan.txt` §"Tata Cara Sholat Langkah demi Langkah" mendaftar "Syarat wajib dan syarat
        // sah", "Persiapan sebelum sholat", "Wudhu dan hal yang membatalkannya", "Penutup aurat
        // dan kebersihan tempat sholat", serta "Penentuan arah kiblat" sebagai bagian panduan ini.
        // Karena itu semuanya disajikan di sini, di depan langkah pertama: pembaca yang menekan
        // "Berikutnya" sampai takbir tanpa tahu syaratnya akan mengerjakan urutan yang belum tentu
        // sah. Wudhu dan kiblat punya layar sendiri, jadi cukup ditautkan — kontennya tidak disalin.
        Text("Sebelum sholat", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())) {
            OutlinedButton(onClick = { go(Routes.WUDHU) }) { Text("Wudhu & Tayamum") }
            OutlinedButton(onClick = { go(Routes.KIBLAT) }) { Text("Arah Kiblat") }
        }
        if (syarat.jumlah > 0) {
            TextButton(onClick = { syaratTerbuka = !syaratTerbuka }) {
                Text(if (syaratTerbuka) "Sembunyikan syarat & persiapan"
                    else "Syarat & persiapan sholat (${syarat.jumlah} butir)")
            }
            // Dibuka-tutup dengan transisi ringan agar kemunculan daftar yang panjang tidak
            // mengagetkan (`plan.txt`: "Gunakan animasi secukupnya dan hindari efek berlebihan").
            AnimatedVisibility(visible = syaratTerbuka) {
                Column {
                    BagianDaftar("Syarat wajib", syarat.syaratWajib)
                    BagianDaftar("Syarat sah", syarat.syaratSah)
                    BagianDaftar("Persiapan sebelum sholat", syarat.persiapan)
                    val aurat = syarat.penutupAurat
                    if (aurat.lakiLaki.isNotBlank() || aurat.perempuan.isNotBlank()) {
                        Text(aurat.judul.ifBlank { "Penutup aurat" }, style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp))
                        if (aurat.lakiLaki.isNotBlank()) Text("• ${aurat.lakiLaki}", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp))
                        if (aurat.perempuan.isNotBlank()) Text("• ${aurat.perempuan}", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp))
                        if (aurat.catatan.isNotBlank()) Text(aurat.catatan, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                    }
                    BagianDaftar("Kebersihan tempat sholat", syarat.kebersihanTempat)
                    if (syarat.catatanMazhab.isNotBlank()) {
                        Text(syarat.catatanMazhab, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    // Status sumber dinyatakan terbuka, bukan disembunyikan: daftar ini kerangka
                    // berbahasa Indonesia, dan pengguna berhak tahu bahwa rinciannya belum dicocokkan
                    // kata-per-kata dengan modul Fasolatan resmi.
                    if (syarat.catatanVerifikasi.isNotBlank()) {
                        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(syarat.catatanVerifikasi, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!ringkas, { ringkas = false }, { Text("Pemula") })
            FilterChip(ringkas, { ringkas = true }, { Text("Ringkas") })
            OutlinedButton(onClick = { settings = true }) { Text("Pengaturan bacaan") }
        }
        Text("Mazhab panduan: $mazhabLabel", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Mazhab.SEMUA.forEach { (key, label) ->
                FilterChip(mazhab == key, {
                    mazhab = key
                    scope.launch { store.set(Store.MADHAB, key) }
                }, { Text(label) })
            }
        }
        // Bundel konten dapat diperbarui dari layar "Pembaruan konten", sehingga id sholat yang
        // dahulu ada bisa hilang dan daftar langkahnya menjadi kosong. Dahulu baris di bawah ini
        // menghitung `coerceIn(0, steps.size - 1)` = `coerceIn(0, -1)` — rentang kosong — dan
        // aplikasi berhenti begitu layar ini dibuka. Kekosongan itu kini diberitahukan; bagian
        // sesudahnya (dzikir setelah sholat, catatan mazhab) tetap ditampilkan karena tetap berguna.
        if (steps.isEmpty()) {
            Text("Bacaan langkah untuk sholat ini belum ada di bundel konten.",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            Text("Muat ulang bundel konten di Pengaturan → Konten, atau pilih sholat lain dari daftar.",
                style = MaterialTheme.typography.labelSmall)
        } else {
            Text("Langkah ${step + 1} dari ${steps.size}")
            LinearProgressIndicator(progress = { ((step + 1).toFloat() / steps.size).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            val b = steps[step.coerceIn(0, steps.size - 1)]
            val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
                Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
            }
            Text(b.judul, style = MaterialTheme.typography.titleMedium)
            if (b.kapan.isNotBlank()) Text("Kapan: ${b.kapan}", style = MaterialTheme.typography.labelMedium)
            // Ilustrasi skematik posisi tubuh + deskripsi (plan: tiap gerakan punya visual yang sopan).
            Postures.of(b.id)?.let { p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        painter = painterResource(p.gambar),
                        contentDescription = "Ilustrasi posisi: ${p.label}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp),
                    )
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(p.label, style = MaterialTheme.typography.titleSmall)
                        Text(p.deskripsi, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            BacaanCard("langkah:$id:${b.id}", b.judul, a, l, t, true, true, arabSize, sumber = b.sumber,
                showArab = showArab, kategori = b.kategori)
            if (b.catatan.isNotBlank()) {
                Text(b.catatan, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (step > 0) step-- }, enabled = step > 0) { Text("← Sebelumnya") }
                Button(onClick = { step = 0 }) { Text("Ulangi") }
                Button(onClick = { if (step < steps.size - 1) step++ }, enabled = step < steps.size - 1) { Text("Berikutnya →") }
            }
            if (step == steps.size - 1 && !selesai) Text("Semua langkah sudah dibaca — tandai selesai bila sudah dipraktikkan.",
                color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { scope.launch { userData.setProgress(KIND, id, 1) } }, enabled = !selesai) {
                    Text(if (selesai) "✓ Sudah ditandai selesai" else "Tandai selesai")
                }
                if (selesai) OutlinedButton(onClick = { scope.launch { userData.setProgress(KIND, id, 0) } }) {
                    Text("Batalkan tanda")
                }
            }
        }
        // --- Setelah salam ---------------------------------------------------------------------
        // `plan.txt` menutup rangkaian langkah dengan "Dzikir dan doa setelah sholat". Layar Dzikir
        // membuka kelompok "Dzikir setelah sholat" lebih dahulu (urutan kelompok pada bundel),
        // sehingga tautan ini mendarat tepat di tempat yang dimaksud.
        OutlinedButton(onClick = { go(Routes.DZIKIR) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Dzikir & doa setelah sholat")
        }
        Text("Catatan mazhab: Indonesia umumnya Syafi'i (qunut Subuh sunnah). Bacaan bertanda mazhab " +
            "tertentu hanya tampil bila mazhab panduan cocok — perbedaan dicatat, tidak dicampuradukkan. Lihat SOURCES.md.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    }

    if (settings) {
        BacaanSettingsSheet(
            onDismiss = { settings = false },
            showArab = showArab, onShowArab = { showArab = it; scope.launch { store.set(Store.SHOW_ARAB, it) } },
            arabSize = arabSize, onArabSize = { arabSize = it; scope.launch { store.set(Store.ARAB_SIZE, it) } },
        )
    }
}

private const val KIND = "sholat"

/**
 * Satu bagian daftar syarat/persiapan. Bagian kosong tidak digambar sama sekali — judul tanpa isi
 * hanya menambah tinggi layar tanpa memberi tahu apa pun.
 */
@Composable
private fun BagianDaftar(judul: String, butir: List<String>) {
    if (butir.isEmpty()) return
    Text(judul, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    butir.forEach { b ->
        Text("• $b", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
    }
}
