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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.AdzanDoa
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.FormatWaktu
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.delay

/**
 * Getar penanda waktu iqomah habis.
 *
 * Diambil lewat `getSystemService(Vibrator::class.java)`, bukan `Context.VIBRATOR_SERVICE` yang
 * usang sejak API 31 (penggantinya `VibratorManager`). Bentuk berbasis kelas ini mengembalikan
 * getar bawaan yang sama — pada API 31+ `SystemServiceRegistry` sendiri yang mengarahkannya ke
 * `VibratorManager.getDefaultVibrator()` — jadi ia bekerja sejak `minSdk` 26 tanpa cabang versi
 * dan tanpa perlu membungkam peringatan usang.
 */
private fun vibrate(ctx: Context) {
    val v = ctx.getSystemService(Vibrator::class.java) ?: return
    runCatching { v.vibrate(VibrationEffect.createOneShot(900, VibrationEffect.DEFAULT_AMPLITUDE)) }
}

// Panjang timer (menit) tidak lagi diubah dari layar ini: tombol − / + -nya dipindah ke tab
// Pengaturan (bagian "Jadwal & pengingat" → "Durasi iqomah") yang menulis `Store.IQOMAH_MIN` dan
// menjadwalkan ulang pengingatnya. Saklar pengingatnya pun kini satu rumah dengan durasinya di
// sana. Layar ini cukup membaca nilainya lalu menjalankan hitung mundurnya — sama dengan
// notifikasi latar yang membaca kunci yang sama.

@Composable
fun IqomahScreen(go: (String) -> Unit = {}) {
    val repo = rememberRepo()
    val store = rememberStore()
    val ctx = LocalContext.current
    val items = remember { repo.iqomah() }
    var mins by remember { mutableStateOf(PengingatAdzan.IQOMAH_DEFAULT_MENIT) }
    var remaining by rememberSaveable { mutableStateOf(-1) } // detik; -1 = tidak berjalan
    // rememberSaveable: mode layar penuh pilihan pengguna tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(BawaanPreferensi.TAMPIL_ARAB) }
    var showLatin by remember { mutableStateOf(BawaanPreferensi.TAMPIL_LATIN) }
    var showTrans by remember { mutableStateOf(BawaanPreferensi.TAMPIL_TERJEMAHAN) }
    LaunchedEffect(Unit) {
        mins = PengingatAdzan.iqomahMenit(
            store.get(Store.IQOMAH_MIN, PengingatAdzan.IQOMAH_DEFAULT_MENIT))
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN)
        showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN)
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

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(NamaLayar.IQOMAH, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("iqomah")) }
        }
        Text("Timer opsional setelah masuk waktu (± $mins mnt) — durasi dan pengingatnya diatur " +
                "di tab Pengaturan (bagian \"Jadwal & pengingat\"). ${PengingatAdzan.IQOMAH_PERKIRAAN}",
            style = MaterialTheme.typography.labelMedium)

        // Durasi, bukan jam: ejaannya dari aturan bersama, tetapi namanya sengaja beda —
        // lihat `FormatWaktu.menitDetik`. Timer menjadi pusat layar: angkanya besar dan tengahannya
        // diberi ruang, karena itulah yang dilihat pengguna dari kejauhan saat menunggu iqomah.
        Text(
            FormatWaktu.menitDetik(shown),
            style = MaterialTheme.typography.displayMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else (1f - shown.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        BarisChip {
            Button(onClick = { remaining = total }, enabled = !running) { Text("Mulai") }
            OutlinedButton(onClick = { remaining = -1 }) { Text(if (running) NamaAntarmuka.BATAL else NamaAntarmuka.RESET) }
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH) })
        }
        // `plan.txt` §"Fitur Iqomah": *"Tambahkan pengatur waktu Iqomah opsional setelah pengingat
        // masuk waktu sholat."* Timer di atas hanya hidup selama layar terbuka; pengingat yang
        // tetap berbunyi saat aplikasi ditutup dinyalakan dari tab Pengaturan bersama durasinya —
        // satu keluarga pengaturan di satu rumah, bukan saklar yang tersebar di layar bacaan.
        Text("Timer berjalan selama layar ini terbuka; getar menandai waktu habis. Pengingat " +
                "perkiraan iqomah habis beserta durasinya diatur di tab Pengaturan " +
                "(bagian \"Jadwal & pengingat\"). ${PengingatAdzan.IQOMAH_PERKIRAAN}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp))

        if (repo.iqomahCatatan().isNotBlank()) {
            Text(repo.iqomahCatatan(), style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp))
        }

        // `plan.txt` §"Fitur Iqomah" meminta *"urutan bacaan yang jelas"* dan sumber yang dapat
        // diperiksa. Tiap lafaz karena itu diberi nomor urut dan menyebut `sumber` bagiannya
        // ("HR Abu Dawud, Tirmidzi — sunnah.com"), bukan tampil sebagai rangkaian telanjang.
        // Nomor urut juga membuat id kartu unik: sebelumnya id memakai 10 huruf Arab pertama,
        // yang sama untuk lafaz berulang, sehingga favorit satu lafaz menandai lafaz lain juga.
        BagianJudul("Lafaz iqomah")
        items.forEachIndexed { i, l ->
            BacaanCard(JenisBacaan.idLafazIqomah(i + 1),
                "Lafaz iqomah ${i + 1} dari ${items.size}",
                l.arab, l.latin, l.terjemahan,
                showLatin, showTrans, UkuranBacaan.arabBacaJauh(arabSize, fullscreen), ulangi = l.ulangi,
                sumber = l.sumber, showArab = showArab, kategori = l.kategori)
        }
    }

}

/**
 * Doa sebelum adzan — lafaz menjawab adzan beserta waktunya (permintaan pengguna, 2026-10-16):
 * dahulu bacaan ini menumpuk bersama sholawat dan doa setelah adzan di satu layar "Doa Adzan";
 * kini tiap waktu bacaan layar sendiri pada bagian "Adzan" di menu Ibadah.
 */
@Composable
fun DoaSebelumAdzanScreen(go: (String) -> Unit = {}) {
    val repo = rememberRepo()
    val store = rememberStore()
    val menjawab = remember { repo.menjawabAdzan() }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(BawaanPreferensi.TAMPIL_ARAB) }
    var showLatin by remember { mutableStateOf(BawaanPreferensi.TAMPIL_LATIN) }
    var showTrans by remember { mutableStateOf(BawaanPreferensi.TAMPIL_TERJEMAHAN) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN)
        showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN)
    }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(NamaLayar.DOA_SEBELUM_ADZAN, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("doa_sebelum_adzan")) }
        }
        Text("Bacaan yang berlaku ketika adzan dikumandangkan: jawab lafaz muadzin selama adzan " +
                "belum selesai. Doa setelah adzan ada di layar tersendiri setelah ini.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp))
        KartuDoa(JenisBacaan.id(JenisBacaan.DOA, "menjawab_adzan"), menjawab, arabSize,
            showArab, showLatin, showTrans)
    }
}

/**
 * Doa setelah adzan — sholawat lalu doanya, dalam urutan waktunya (permintaan pengguna,
 * 2026-10-16): dahulu keduanya menumpuk bersama bacaan menjawab adzan di satu layar "Doa Adzan".
 *
 * Ketukan notifikasi pengingat doa setelah adzan mendarat di layar ini (`Routes.DOA_SETELAH_ADZAN`
 * meneruskan peran rute lama), sehingga keterangannya tetap menyebut rumah saklarnya di menu Adzan.
 */
@Composable
fun DoaSesudahAdzanScreen(go: (String) -> Unit = {}) {
    val repo = rememberRepo()
    val store = rememberStore()
    val doa = remember { repo.doaAdzan() }
    val sholawat = remember { repo.sholawatAdzan() }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(BawaanPreferensi.TAMPIL_ARAB) }
    var showLatin by remember { mutableStateOf(BawaanPreferensi.TAMPIL_LATIN) }
    var showTrans by remember { mutableStateOf(BawaanPreferensi.TAMPIL_TERJEMAHAN) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN)
        showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN)
    }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(NamaLayar.DOA_SETELAH_ADZAN, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("doa_sesudah_adzan")) }
        }
        // `plan.txt` §"Bacaan Setelah Adzan": *"Tambahkan tombol untuk … mengaktifkan pengingat
        // membaca doa setelah adzan."* Saklarnya hidup di menu Adzan — hub seluruh pengaturan
        // pengingat adzan — dan tidak digandakan di sini (satu pengaturan, satu rumah): pengguna
        // yang menemukan doanya di layar ini tetap diarahkan ke saklarnya.
        //
        // Sebab, ikut-saklar, dan batas syariatnya tetap dari aturan bersama (`PengingatAdzan`),
        // sama persis dengan yang ditulis menu Adzan — dua penjelasan untuk satu pengingat tidak
        // boleh berbeda kata.
        Text("\"${PengingatAdzan.NAMA_PENGINGAT_DOA}\" dinyalakan dari menu Adzan. Bila aktif: " +
                "${PengingatAdzan.DOA_SEBAB}; ketukannya langsung membuka layar ini dan pengingatnya " +
                "tetap berbunyi walau aplikasi ditutup. ${PengingatAdzan.DOA_IKUT_SAKLAR} " +
                PengingatAdzan.DOA_BUKAN_SYARIAT,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp))
        KartuDoa(JenisBacaan.id(JenisBacaan.DOA, "sholawat_adzan"), sholawat, arabSize,
            showArab, showLatin, showTrans)
        KartuDoa(JenisBacaan.id(JenisBacaan.DOA, "adzan"), doa, arabSize,
            showArab, showLatin, showTrans)
        Text("Suara adzan, pengingat sebelum adzan, dan saklar pengingat doanya diatur di menu " +
                "Adzan. Bacaan ketika adzan dikumandangkan ada di layar ${NamaLayar.DOA_SEBELUM_ADZAN}; " +
                "Iqomah memiliki timer tersendiri di menu Iqomah.",
            style = MaterialTheme.typography.labelSmall)
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
private fun KartuDoa(
    id: String, entri: AdzanDoa, arabSize: Float,
    showArab: Boolean, showLatin: Boolean, showTrans: Boolean,
) {
    BacaanCard(id, entri.judul, entri.arab, entri.latin, entri.terjemahan,
        showLatin, showTrans, arabSize, sumber = entri.sumber, showArab = showArab, kategori = entri.kategori)
    if (entri.waktu.isNotBlank()) Text(
        "Kapan dibaca: ${entri.waktu}", style = MaterialTheme.typography.labelMedium)
    if (entri.catatan.isNotBlank()) Text(
        entri.catatan, style = MaterialTheme.typography.labelSmall)
}
