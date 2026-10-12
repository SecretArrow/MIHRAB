package id.or.kemenag.yasintahlil.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.SumberKategori
import kotlinx.coroutines.launch

@Composable
fun rememberRepo(): ContentRepo {
    val ctx = LocalContext.current
    return remember { ContentRepo(ctx.applicationContext) }
}

@Composable
fun rememberStore(): Store {
    val ctx = LocalContext.current
    return remember { Store(ctx.applicationContext) }
}

@Composable
fun ArabText(arab: String, sizeSp: Float, modifier: Modifier = Modifier, lineHeightFactor: Float = 1.9f) {
    // Teks Arab ditata arah kanan-ke-kiri agar tanda baca & angka menempel benar.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(arab, fontSize = sizeSp.sp, lineHeight = (sizeSp * lineHeightFactor).sp,
            textAlign = TextAlign.Right, modifier = modifier.fillMaxWidth())
    }
}

fun Context.copyText(label: String, text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText(label, text))
}

fun Context.shareText(text: String) {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
    }, "Bagikan"))
}

/**
 * Label kecil kategori sumber bacaan (Al-Qur'an / Hadis / Dzikir umum / Tradisi lokal).
 * `plan.txt` meminta bacaan dibedakan menurut sumbernya; kunci yang tak dikenal tidak
 * ditampilkan sama sekali agar tidak ada klaim yang tak terverifikasi.
 */
@Composable
fun SumberKategoriLabel(kategori: String, modifier: Modifier = Modifier) {
    if (SumberKategori.dikenal(kategori)) {
        Text(SumberKategori.label(kategori), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary, modifier = modifier)
    }
}

@Composable
fun BacaanCard(
    id: String, judul: String,
    arab: String, latin: String, terjemahan: String,
    showLatin: Boolean, showTrans: Boolean,
    arabSize: Float, latinSize: Float,
    ulangi: Int = 1, sumber: String = "",
    lineHeightFactor: Float = 1.9f,
    onOpen: (() -> Unit)? = null,
    showArab: Boolean = true,
    kategori: String = "",
) {
    val ctx = LocalContext.current
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    var fav by remember { mutableStateOf(false) }
    val (favKind, favRef) = remember(id) { splitContentId(id) }
    LaunchedEffect(id) {
        userData.isFavorite(favKind, favRef).collect { fav = it }
    }
    // "Tema halaman" (plan.txt): kartu bacaan digambar di atas kertas pilihan pengguna. Skema
    // warna hanya diganti bila halaman memang bukan "ikut tema", sehingga tak ada biaya saat mati.
    val skema = skemaHalaman(LocalTemaHalaman.current)
    val isi: @Composable () -> Unit = {
        // Kartu bacaan dibatasi lebih sempit dari isi layar (`lebarBaca`): baris Arab dan
        // terjemahan adalah teks terpanjang di aplikasi, dan baris yang terlalu panjang melelahkan
        // mata. `animateContentSize` membuat tinggi kartu berubah halus saat chip "Arab"/"Latin"/
        // "Terjemahan" di Pengaturan bacaan dinyalakan atau dimatikan, bukan melompat seketika.
        Card(Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).lebarBaca()
            .animateContentSize().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(judul, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    // Penanda jumlah ulangan — teks, bukan tombol. Sebelumnya AssistChip dengan onClick
                    // kosong: tombol yang tampak bisa ditekan tetapi tak melakukan apa pun.
                    if (ulangi > 1) Text("${ulangi}x", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (showArab && arab.isNotBlank()) ArabText(arab, arabSize, lineHeightFactor = lineHeightFactor)
                if (showLatin && latin.isNotBlank()) Text(latin, fontSize = latinSize.sp,
                    color = MaterialTheme.colorScheme.secondary)
                if (showTrans && terjemahan.isNotBlank()) Text(terjemahan, fontSize = latinSize.sp)
                if (sumber.isNotBlank()) Text("Sumber: $sumber", style = MaterialTheme.typography.labelSmall)
                SumberKategoriLabel(kategori)
                Row {
                    TextButton(onClick = {
                        ctx.copyText(judul, "$judul\n$arab\n$latin\n$terjemahan")
                        scope.launch { userData.pushHistory(favKind, favRef, judul) }
                    }) { Text("Salin") }
                    TextButton(onClick = { ctx.shareText("$judul\n$arab\n$latin\n$terjemahan") }) { Text("Bagikan") }
                    TextButton(onClick = {
                        scope.launch { userData.toggleFavorite(favKind, favRef, judul) }
                    }) { Text(if (fav) "★ Favorit" else "☆ Favorit") }
                    if (onOpen != null) TextButton(onClick = onOpen) { Text("Buka") }
                }
            }
        }
    }
    if (skema == null) isi() else MaterialTheme(colorScheme = skema) { isi() }
}

@Composable
fun CounterRow(count: Int, target: Int, onPlus: () -> Unit, onMinus: () -> Unit, onReset: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(onClick = onMinus) { Text("−") }
        Text("$count / $target", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onPlus) { Text("+") }
        OutlinedButton(onClick = onReset) { Text("Reset") }
    }
    // Progres dihaluskan agar setiap ketukan penghitung terlihat sebagai gerakan, bukan lompatan —
    // "animasi secukupnya" (`plan.txt`) sekaligus umpan balik visual atas ketukan itu.
    val progres by animateFloatAsState(
        targetValue = (count.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f),
        label = "progres-hitung",
    )
    LinearProgressIndicator(progress = { progres },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
}

// --- Keadaan layar: kosong, gagal, memuat ------------------------------------------------
// `plan.txt`: "Berikan skeleton loading atau indikator proses saat memuat data" dan
// "Sediakan tampilan kosong, pesan kesalahan, dan tombol mencoba kembali". Ketiganya dibuat
// satu kali di sini agar pesan/istilahnya konsisten di seluruh layar.

/**
 * Tampilan kosong. [onTombol] diisi hanya bila keadaan itu memang bisa diulang dari layar ini;
 * kalau tidak, tombolnya tidak ditampilkan sama sekali (tidak ada tombol dekoratif).
 */
@Composable
fun EmptyState(
    judul: String,
    pesan: String = "",
    labelTombol: String = "Coba lagi",
    onTombol: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(judul, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (pesan.isNotBlank()) {
            Text(pesan, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        if (onTombol != null) OutlinedButton(onClick = onTombol) { Text(labelTombol) }
    }
}

/** Pesan kesalahan + tombol mencoba kembali. Tombol hanya muncul bila ada yang bisa diulang. */
@Composable
fun ErrorState(pesan: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(pesan, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) Button(onClick = onRetry) { Text("Coba lagi") }
    }
}

/** Indikator proses saat data sedang dimuat. */
@Composable
fun LoadingState(pesan: String = "Memuat…", modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator()
        Text(pesan, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

/**
 * Kerangka kartu (skeleton) sebagai penanda proses: menampilkan bentuk konten yang akan datang
 * tanpa mengarang isinya. Dipakai saat kontennya benar-benar menunggu data yang lambat (mis.
 * daftar hasil pencarian daring), bukan untuk menutupi jeda satu frame.
 */
@Composable
fun SkeletonCard(baris: Int = 3, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(baris.coerceAtLeast(1)) { i ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(if (i == 0) 0.5f else 1f).height(14.dp),
                ) {}
            }
        }
    }
}

// --- Bottom sheet pengaturan bacaan ------------------------------------------------------

/**
 * Bottom sheet pengaturan bacaan singkat (`plan.txt`: *"Gunakan bottom sheet untuk pengaturan
 * singkat"*). Sebelumnya tiap layar bacaan menaruh chip dan slider-nya sendiri di badan layar;
 * kini kontrol tampilan yang sama tinggal dibuka dari satu tombol, sehingga badan layar tetap
 * bersih dan istilahnya seragam.
 *
 * Parameter opsional ([showLatin], [showTrans], [latinSize], [lineHeight]) hanya ditampilkan bila
 * layar memang menyediakannya — layar yang tak punya bacaan Latin/terjemahan tidak menampilkan chip
 * yang tak berefek.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacaanSettingsSheet(
    onDismiss: () -> Unit,
    showArab: Boolean, onShowArab: (Boolean) -> Unit,
    showLatin: Boolean? = null, onShowLatin: ((Boolean) -> Unit)? = null,
    showTrans: Boolean? = null, onShowTrans: ((Boolean) -> Unit)? = null,
    arabSize: Float, onArabSize: (Float) -> Unit,
    latinSize: Float? = null, onLatinSize: ((Float) -> Unit)? = null,
    lineHeight: Float? = null, onLineHeight: ((Float) -> Unit)? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Pengaturan bacaan", style = MaterialTheme.typography.titleMedium)
            Text("Berlaku untuk semua bacaan dan tersimpan otomatis.",
                style = MaterialTheme.typography.labelSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                FilterChip(showArab, { onShowArab(!showArab) }, { Text("Arab") })
                if (showLatin != null && onShowLatin != null) {
                    FilterChip(showLatin, { onShowLatin(!showLatin) }, { Text("Latin") })
                }
                if (showTrans != null && onShowTrans != null) {
                    FilterChip(showTrans, { onShowTrans(!showTrans) }, { Text("Terjemahan") })
                }
            }
            UkuranSlider("Ukuran Arab", arabSize, ARAB_SIZE_RANGE, onArabSize)
            if (latinSize != null && onLatinSize != null) {
                UkuranSlider("Ukuran Latin", latinSize, LATIN_SIZE_RANGE, onLatinSize)
            }
            if (lineHeight != null && onLineHeight != null) {
                UkuranSlider("Jarak antarbaris", lineHeight, LINE_HEIGHT_RANGE, onLineHeight, desimal = 1)
            }
            TemaHalamanChips()
        }
    }
}

/**
 * Pemilih "tema halaman" (`plan.txt`: *"Pengaturan jarak antarbaris dan tema halaman"*).
 *
 * Menulis sendiri ke preferensi — seperti tombol favorit di [BacaanCard] — supaya tak perlu
 * dioper dari tiap layar. Efeknya langsung terlihat karena akar aplikasi ([ProvideTemaHalaman])
 * membaca preferensi yang sama secara reaktif, dan setiap [BacaanCard] menghormatinya.
 */
@Composable
private fun TemaHalamanChips() {
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val aktif by store.flow(Store.PAGE_THEME, TemaHalaman.IKUT)
        .collectAsStateWithLifecycle(TemaHalaman.IKUT)
    Column(Modifier.fillMaxWidth()) {
        Text("Tema halaman: ${TemaHalaman.label(aktif)}", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemaHalaman.SEMUA.forEach { t ->
                FilterChip(
                    aktif == t,
                    { scope.launch { store.set(Store.PAGE_THEME, t) } },
                    { Text(TemaHalaman.label(t)) },
                )
            }
        }
    }
}

/** Rentang ukuran dan jarak antarbaris yang sah — dipakai bersama oleh sheet dan uji statis. */
val ARAB_SIZE_RANGE = 16f..40f
val LATIN_SIZE_RANGE = 12f..24f
val LINE_HEIGHT_RANGE = 1.4f..2.6f

@Composable
private fun UkuranSlider(
    label: String,
    nilai: Float,
    rentang: ClosedFloatingPointRange<Float>,
    onUbah: (Float) -> Unit,
    desimal: Int = 0,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${"%.${desimal}f".format(nilai)}", style = MaterialTheme.typography.labelLarge)
        Slider(nilai, onUbah, valueRange = rentang)
    }
}
