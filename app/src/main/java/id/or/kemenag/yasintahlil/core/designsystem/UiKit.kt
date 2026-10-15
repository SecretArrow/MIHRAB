package id.or.kemenag.yasintahlil.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.audio.TtsBacakan
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.SumberKategori
import id.or.kemenag.yasintahlil.core.util.BagikanGambar
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
fun ArabText(
    arab: String,
    sizeSp: Float,
    modifier: Modifier = Modifier,
    lineHeightFactor: Float = UkuranBacaan.JARAK_DEFAULT,
) {
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

/**
 * Satu aksi tambahan pada lembar aksi kartu bacaan — mis. "Tandai terakhir dibaca" dan "Bookmark"
 * pada kartu ayat Yasin. Aksi inti (salin, bagikan, favorit) sudah disediakan kartunya sendiri;
 * daftar ini milik layar yang tahu apa lagi yang bisa dilakukan pada bacaannya.
 */
data class AksiBacaan(
    val label: String,
    val ikon: ImageVector? = null,
    val onAksi: () -> Unit,
)

/**
 * Satu bacaan: judul, Arab, transliterasi, terjemahan, dan sumber.
 *
 * Aksi bacaan (salin, bagikan, favorit, serta [aksiEkstra] milik layarnya) dibuka dengan
 * **menekan lama** kartunya — tidak lagi deretan tombol yang menempel di setiap kartu, sehingga
 * daftar bacaan yang panjang terbaca sebagai bacaan, bukan deretan panel alat.
 *
 * [latinSize] dan [lineHeightFactor] boleh dibiarkan `null`: keduanya adalah preferensi bacaan
 * **global** (Pengaturan → "Ukuran Latin" dan "Jarak antarbaris") yang dibaca langsung dari
 * preferensi di sini. Layar yang tidak mengoper nilainya karena itu tetap menghormati pilihan
 * pengguna, bukan angka tetap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacaanCard(
    id: String, judul: String,
    arab: String, latin: String, terjemahan: String,
    showLatin: Boolean, showTrans: Boolean,
    arabSize: Float, latinSize: Float? = null,
    ulangi: Int = 1, sumber: String = "",
    lineHeightFactor: Float? = null,
    onOpen: (() -> Unit)? = null,
    showArab: Boolean = true,
    kategori: String = "",
    aksiEkstra: List<AksiBacaan> = emptyList(),
) {
    val ctx = LocalContext.current
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    var fav by remember { mutableStateOf(false) }
    val (favKind, favRef) = remember(id) { splitContentId(id) }
    LaunchedEffect(id) {
        userData.isFavorite(favKind, favRef).collect { fav = it }
    }
    // Preferensi bacaan global — lihat KDoc di atas. Dibaca reaktif agar perubahan dari sheet
    // pengaturan bacaan (atau layar Pengaturan) langsung terlihat tanpa membuka ulang layar.
    val store = rememberStore()
    val latinGlobal by store.flow(Store.LATIN_SIZE, UkuranBacaan.LATIN_DEFAULT)
        .collectAsStateWithLifecycle(UkuranBacaan.LATIN_DEFAULT)
    val tinggiGlobal by store.flow(Store.LINE_HEIGHT, UkuranBacaan.JARAK_DEFAULT)
        .collectAsStateWithLifecycle(UkuranBacaan.JARAK_DEFAULT)
    val ukuranLatin = latinSize ?: latinGlobal
    val tinggiBaris = lineHeightFactor ?: tinggiGlobal
    // "Tema halaman" (plan.txt): kartu bacaan digambar di atas kertas pilihan pengguna. Skema
    // warna hanya diganti bila halaman memang bukan "ikut tema", sehingga tak ada biaya saat mati.
    val skema = skemaHalaman(LocalTemaHalaman.current)
    // Warna PNG "Bagikan sebagai gambar" mengikuti kertas yang sedang dipakai kartu ini: tema
    // halaman (krem/gelap) bila dipilih pengguna, atau tema aplikasi (terang/gelap). Wajib
    // diambil SEKARANG, saat komposisi — lambda klik di bawah berjalan di luar komposisi sehingga
    // tidak boleh lagi membaca MaterialTheme di dalamnya.
    val skemaKartu = skema ?: MaterialTheme.colorScheme
    val latarKartu = skemaKartu.surface.toArgb()
    val teksKartu = skemaKartu.onSurface.toArgb()
    val aksenKartu = skemaKartu.primary.toArgb()
    // Tekan lama kartu membuka lembar aksinya (salin, bagikan, favorit, plus aksi layarnya).
    // Ketukan biasa sengaja tak melakukan apa pun: membuka lembar lewat ketukan biasa akan
    // membuatnya terbuka setiap kali pengguna menggulir dengan jari yang menempel layar.
    var lembarAksi by remember { mutableStateOf(false) }
    val isi: @Composable () -> Unit = {
        // Kartu bacaan dibatasi lebih sempit dari isi layar (`lebarBaca`): baris Arab dan
        // terjemahan adalah teks terpanjang di aplikasi, dan baris yang terlalu panjang melelahkan
        // mata. `animateContentSize` membuat tinggi kartu berubah halus saat chip "Arab"/"Latin"/
        // "Terjemahan" di Pengaturan dinyalakan atau dimatikan, bukan melompat seketika.
        Card(
            Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).lebarBaca()
                .animateContentSize().padding(vertical = 4.dp)
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(onClick = {}, onLongClick = { lembarAksi = true }),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(judul, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    // Penanda jumlah ulangan — lencana tonal bersama ([Lencana]), bukan tombol.
                    // Sebelumnya AssistChip dengan onClick kosong: tombol yang tampak bisa
                    // ditekan tetapi tak melakukan apa pun.
                    if (ulangi > 1) Lencana("${ulangi}x")
                }
                // Kertas bacaan Arab: permukaan pasir lembut dengan sudut membulat, sehingga
                // baris Arab terbaca sebagai satu bidang mushaf — bukan teks mengambang. Pada
                // tema halaman krem/gelap, `surfaceVariant` sudah diganti skemaHalaman menjadi
                // warna kertasnya, jadi kertas ini ikut berganti tanpa pengkabelan tambahan.
                if (showArab && arab.isNotBlank()) Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    ArabText(
                        arab, arabSize,
                        lineHeightFactor = tinggiBaris,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
                if (showLatin && latin.isNotBlank()) Text(
                    latin, fontSize = ukuranLatin.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (showTrans && terjemahan.isNotBlank()) Text(
                    terjemahan, fontSize = ukuranLatin.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (sumber.isNotBlank()) Text(
                    "Sumber: $sumber",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
                SumberKategoriLabel(kategori, Modifier.padding(top = 2.dp))
            }
        }
    }
    if (skema == null) isi() else MaterialTheme(colorScheme = skema) { isi() }

    // Lembar aksi dibuka dengan menekan lama kartunya. Judul bacaannya ditulis di kepala lembar
    // supaya jelas kartu mana yang sedang ditindak — bukan sekadar daftar kata kerja tanpa objek.
    if (lembarAksi) {
        ModalBottomSheet(onDismissRequest = { lembarAksi = false },
            sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 28.dp)) {
                Text(judul, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp))
                BarisAksi(Icons.Filled.ContentCopy, "Salin") {
                    ctx.copyText(judul, "$judul\n$arab\n$latin\n$terjemahan")
                    scope.launch { userData.pushHistory(favKind, favRef, judul) }
                    lembarAksi = false
                }
                BarisAksi(Icons.Filled.Share, "Bagikan") {
                    ctx.shareText("$judul\n$arab\n$latin\n$terjemahan")
                    lembarAksi = false
                }
                // Kartu dirender jadi PNG (Bitmap + StaticLayout) di benang IO — merender bitmap
                // di benang utama menjegal animasi lembar yang sedang menutup. PNG lama
                // dibersihkan SETELAH render: berkas yang barusan dibuat adalah yang termuda,
                // jadi tidak ikut terbuang (lihat BagikanGambar.bersihkan). Ikonnya sengaja
                // kosong: belum ada ikon gambar dalam himpunan ikon yang terverifikasi tersedia
                // untuk repo ini, dan BarisAksi memang menerima ikon kosong (preseden aksi Buka
                // pada baris di bawah).
                BarisAksi(null, "Bagikan sebagai gambar") {
                    lembarAksi = false
                    scope.launch {
                        val berkas = withContext(Dispatchers.IO) {
                            BagikanGambar.buat(
                                ctx, judul, arab, latin, terjemahan, sumber,
                                latarKartu, teksKartu, aksenKartu,
                            )
                        }
                        BagikanGambar.bersihkan(ctx)
                        BagikanGambar.bagikan(ctx, berkas)
                    }
                }
                // Mode Bacakan: dibacakan suara TTS bahasa Indonesia — aksesibilitas lansia/
                // penglihatan terbatas; menekannya lagi menghentikan. Teks Arab sengaja tidak
                // dibacakan — mesin TTS umum tidak bisa membaca teks Arab berharakat dengan
                // benar (lihat TtsBacakan.susunTeksBacaan).
                BarisAksi(
                    Icons.Filled.Campaign,
                    if (TtsBacakan.sedangBaca) "Hentikan bacaan" else "Bacakan",
                ) {
                    if (TtsBacakan.sedangBaca) TtsBacakan.berhenti()
                    else TtsBacakan.bacakan(ctx, TtsBacakan.susunTeksBacaan(judul, latin, terjemahan))
                    lembarAksi = false
                }
                BarisAksi(
                    if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
                    if (fav) "Hapus favorit" else "Favorit",
                ) {
                    scope.launch { userData.toggleFavorite(favKind, favRef, judul) }
                    lembarAksi = false
                }
                if (onOpen != null) {
                    BarisAksi(null, NamaAntarmuka.BUKA) { onOpen(); lembarAksi = false }
                }
                aksiEkstra.forEach { aksi ->
                    BarisAksi(aksi.ikon, aksi.label) { aksi.onAksi(); lembarAksi = false }
                }
            }
        }
    }
}

/**
 * Satu baris aksi pada lembar aksi kartu bacaan: ikon di kiri, label di sampingnya. Ikon boleh
 * kosong — aksi seperti "Buka" sudah jelas tanpa gambar, dan ikon karangan justru menambah derau.
 */
@Composable
private fun BarisAksi(ikon: ImageVector?, label: String, onKlik: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onKlik)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (ikon != null) {
            Icon(ikon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CounterRow(count: Int, target: Int, onPlus: () -> Unit, onMinus: () -> Unit, onReset: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        // Baris penghitung dipadatkan: tombol ikon tonal mengapit angkanya, sehingga deretan
        // penghitung pada layar yang menampilkannya per bacaan (Dzikir) tidak memakan tinggi
        // layar lebih besar daripada bacaannya sendiri.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onMinus) {
                Icon(Icons.Filled.Remove, contentDescription = "Kurangi")
            }
            Text("$count / $target", style = MaterialTheme.typography.titleLarge)
            FilledTonalIconButton(onClick = onPlus) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah")
            }
        }
        // Progres dihaluskan agar setiap ketukan penghitung terlihat sebagai gerakan, bukan
        // lompatan — "animasi secukupnya" (`plan.txt`) sekaligus umpan balik visual atas ketukan
        // itu. Tombol reset menempel di ujungnya sebagai ikon, bukan tombol penuh lebar.
        val progres by animateFloatAsState(
            targetValue = (count.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f),
            label = "progres-hitung",
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LinearProgressIndicator(
                progress = { progres },
                modifier = Modifier.weight(1f).height(6.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = NamaAntarmuka.RESET,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
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
    labelTombol: String = NamaAntarmuka.COBA_LAGI,
    onTombol: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(36.dp),
        )
        Text(judul, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (pesan.isNotBlank()) {
            Text(pesan, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(36.dp),
        )
        Text(pesan, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) Button(onClick = onRetry) { Text(NamaAntarmuka.COBA_LAGI) }
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
    Card(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
