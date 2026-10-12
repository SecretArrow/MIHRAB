package id.or.kemenag.yasintahlil.feature.adzan

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.AdzanDoa
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun vibrate(ctx: Context) {
    val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    runCatching { v.vibrate(VibrationEffect.createOneShot(900, VibrationEffect.DEFAULT_AMPLITUDE)) }
}

/**
 * Simpan panjang timer, lalu susun ulang pengingat.
 *
 * Tanpa penjadwalan ulang, mengubah menit hanya memengaruhi timer di layar sementara notifikasi
 * latar tetap memakai jeda lama — dua angka berbeda untuk hal yang sama.
 */
private fun simpanMins(store: Store, scope: CoroutineScope, mins: Int, ctx: Context) {
    scope.launch {
        store.set(Store.IQOMAH_MIN, mins)
        PrayerScheduler.scheduleAll(ctx.applicationContext)
    }
}

@Composable
fun IqomahScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val items = remember { repo.iqomah() }
    var mins by remember { mutableStateOf(PengingatAdzan.IQOMAH_DEFAULT_MENIT) }
    var remaining by rememberSaveable { mutableStateOf(-1) } // detik; -1 = tidak berjalan
    // rememberSaveable: mode layar penuh pilihan pengguna tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var ingatkan by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        mins = PengingatAdzan.iqomahMenit(
            store.get(Store.IQOMAH_MIN, PengingatAdzan.IQOMAH_DEFAULT_MENIT))
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        ingatkan = store.get(Store.IQOMAH_ON, BawaanPreferensi.PENGINGAT_IQOMAH)
    }
    FullscreenEffect(fullscreen)

    // Hitung mundur nyata per detik; getar sekali saat habis.
    LaunchedEffect(remaining) {
        if (remaining > 0) {
            delay(1_000)
            remaining -= 1
            if (remaining == 0) vibrate(ctx)
        }
    }
    val total = (mins * 60).coerceAtLeast(1)
    val running = remaining > 0
    val shown = if (remaining < 0) total else remaining
    val mm = shown / 60
    val ss = shown % 60

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Iqomah", style = MaterialTheme.typography.titleLarge)
        Text("Timer opsional setelah masuk waktu (± $mins mnt). Ini pengingat pribadi — waktu iqomah tiap masjid berbeda.",
            style = MaterialTheme.typography.labelMedium)

        Text("%02d:%02d".format(mm, ss), style = MaterialTheme.typography.displayMedium)
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else (1f - shown.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { remaining = total }, enabled = !running) { Text("Mulai") }
            OutlinedButton(onClick = { remaining = -1 }) { Text(if (running) "Batal" else "Reset") }
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH) })
        }
        Text("Timer berjalan selama layar ini terbuka; getar menandai waktu habis.",
            style = MaterialTheme.typography.labelSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            // Batasnya dari aturan bersama, sama dengan yang menjepit nilai tersimpan di penjadwal
            // dan pekerja latar — kalau tidak, layar bisa menampilkan angka yang tak pernah dipakai.
            OutlinedButton(onClick = {
                mins = PengingatAdzan.iqomahMenit(mins - 1); remaining = -1; simpanMins(store, scope, mins, ctx)
            }) { Text("−") }
            Text("$mins mnt", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = {
                mins = PengingatAdzan.iqomahMenit(mins + 1); remaining = -1; simpanMins(store, scope, mins, ctx)
            }) { Text("+") }
        }

        // `plan.txt` §"Fitur Iqomah": *"Tambahkan pengatur waktu Iqomah opsional setelah pengingat
        // masuk waktu sholat."* Timer di atas hanya hidup selama layar terbuka; saklar inilah yang
        // menambatkannya ke pengingat masuk waktu, sehingga tetap berbunyi saat aplikasi ditutup.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ingatkan saat perkiraan iqomah habis")
            Switch(ingatkan, {
                ingatkan = it
                scope.launch {
                    store.set(Store.IQOMAH_ON, ingatkan)
                    PrayerScheduler.scheduleAll(ctx.applicationContext)
                }
            })
        }
        Text(if (ingatkan) {
            "Bila aktif: notifikasi $mins menit setelah pengingat masuk waktu sholat, berjalan " +
                "walau layar ini tertutup. Ketukannya membuka layar ini. Waktu iqomah tiap masjid " +
                "berbeda, jadi ini perkiraan pribadi — bukan ketentuan syariat. Mengikuti saklar " +
                "per waktu di menu Adzan: waktu yang dimatikan tidak ikut mengingatkan."
        } else {
            "Bila diaktifkan: notifikasi $mins menit setelah pengingat masuk waktu sholat, " +
                "berjalan walau layar ini tertutup. Waktu iqomah tiap masjid berbeda, jadi ini " +
                "perkiraan pribadi — bukan ketentuan syariat."
        }, style = MaterialTheme.typography.labelSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { settings = true }) { Text(NamaAntarmuka.PENGATURAN_BACAAN) }
        }
        if (repo.iqomahCatatan().isNotBlank()) {
            Text(repo.iqomahCatatan(), style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp))
        }

        // `plan.txt` §"Fitur Iqomah" meminta *"urutan bacaan yang jelas"* dan sumber yang dapat
        // diperiksa. Tiap lafaz karena itu diberi nomor urut dan menyebut `sumber` bagiannya
        // ("HR Abu Dawud, Tirmidzi — sunnah.com"), bukan tampil sebagai rangkaian telanjang.
        // Nomor urut juga membuat id kartu unik: sebelumnya id memakai 10 huruf Arab pertama,
        // yang sama untuk lafaz berulang, sehingga favorit satu lafaz menandai lafaz lain juga.
        items.forEachIndexed { i, l ->
            BacaanCard(JenisBacaan.id(JenisBacaan.IQOMAH, (i + 1).toString()),
                "Lafaz iqomah ${i + 1} dari ${items.size}",
                l.arab, l.latin, l.terjemahan,
                true, true, UkuranBacaan.arabBacaJauh(arabSize, fullscreen), ulangi = l.ulangi,
                sumber = l.sumber, showArab = showArab, kategori = l.kategori)
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

@Composable
fun DoaAdzanScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val doa = remember { repo.doaAdzan() }
    val menjawab = remember { repo.menjawabAdzan() }
    val sholawat = remember { repo.sholawatAdzan() }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var ingatkan by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        ingatkan = store.get(Store.DOA_ADZAN_REMINDER, BawaanPreferensi.PENGINGAT_DOA_ADZAN)
    }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Doa sebelum & sesudah adzan", style = MaterialTheme.typography.titleLarge)
        Text("Tidak ada satu doa khusus yang wajib tepat sebelum adzan. Yang disyariatkan: menjawab adzan, sholawat, lalu doa setelah adzan.",
            style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { settings = true }) { Text(NamaAntarmuka.PENGATURAN_BACAAN) }
        }
        // `plan.txt` §"Bacaan Setelah Adzan": *"Tambahkan tombol untuk … mengaktifkan pengingat
        // membaca doa setelah adzan."* Saklar yang sama juga ada di menu Adzan; keduanya menulis
        // kunci `Store.DOA_ADZAN_REMINDER` yang sama, jadi tidak ada dua sumber kebenaran.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(PengingatAdzan.NAMA_PENGINGAT_DOA)
            Switch(ingatkan, {
                ingatkan = it
                scope.launch {
                    store.set(Store.DOA_ADZAN_REMINDER, ingatkan)
                    PrayerScheduler.scheduleAll(ctx.applicationContext)
                }
            })
        }
        // Sebab, ikut-saklar, dan batas syariat dari aturan bersama (`PengingatAdzan`), sama dengan
        // penjelasan di menu Adzan. Yang khas layar ini hanya ke mana ketukannya membuka ("layar
        // ini") dan bahwa pengingatnya tetap berbunyi saat aplikasi ditutup.
        Text(if (ingatkan) {
            "Bila aktif: ${PengingatAdzan.DOA_SEBAB}. Ketukannya langsung membuka layar ini, dan " +
                "pengingat tetap berbunyi walau aplikasi ditutup. ${PengingatAdzan.DOA_IKUT_SAKLAR} " +
                PengingatAdzan.DOA_BUKAN_SYARIAT
        } else {
            "Bila diaktifkan: ${PengingatAdzan.DOA_SEBAB}. Ketukannya langsung membuka layar ini, " +
                "dan pengingat tetap berbunyi walau aplikasi ditutup."
        }, style = MaterialTheme.typography.labelSmall)
        KartuDoa(JenisBacaan.id(JenisBacaan.DOA, "menjawab_adzan"), menjawab, arabSize, showArab)
        KartuDoa(JenisBacaan.id(JenisBacaan.DOA, "sholawat_adzan"), sholawat, arabSize, showArab)
        KartuDoa(JenisBacaan.id(JenisBacaan.DOA, "adzan"), doa, arabSize, showArab)
        Text("Suara adzan dan pengingat sebelum adzan diatur di menu Adzan; saklar pengingat doa di " +
                "atas dan di menu Adzan menulis preferensi yang sama. Iqomah memiliki timer " +
                "tersendiri di menu Iqomah.",
            style = MaterialTheme.typography.labelSmall)
    }

    if (settings) {
        BacaanSettingsSheet(
            onDismiss = { settings = false },
            showArab = showArab, onShowArab = { showArab = it; scope.launch { store.set(Store.SHOW_ARAB, it) } },
            arabSize = arabSize, onArabSize = { arabSize = it; scope.launch { store.set(Store.ARAB_SIZE, it) } },
        )
    }
}

/**
 * Satu bacaan adzan beserta **waktu membacanya** dan catatannya.
 *
 * `plan.txt` §"Bacaan Setelah Adzan" meminta *"Penjelasan waktu membacanya"*. Catatan dahulu
 * dirender hanya untuk sebagian entri, sehingga catatan pada entri lain akan hilang tanpa jejak
 * begitu bundel menambahkannya — satu jalur render untuk semuanya menutup celah itu.
 */
@Composable
private fun KartuDoa(id: String, entri: AdzanDoa, arabSize: Float, showArab: Boolean) {
    BacaanCard(id, entri.judul, entri.arab, entri.latin, entri.terjemahan,
        true, true, arabSize, sumber = entri.sumber, showArab = showArab, kategori = entri.kategori)
    if (entri.waktu.isNotBlank()) Text(
        "Kapan dibaca: ${entri.waktu}", style = MaterialTheme.typography.labelMedium)
    if (entri.catatan.isNotBlank()) Text(
        entri.catatan, style = MaterialTheme.typography.labelSmall)
}
