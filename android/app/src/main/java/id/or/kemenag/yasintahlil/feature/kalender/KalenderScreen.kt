package id.or.kemenag.yasintahlil.feature.kalender

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.HijriCalc
import id.or.kemenag.yasintahlil.core.util.KalenderJawa
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PuasaSunnah
import id.or.kemenag.yasintahlil.navigation.Routes
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar

/**
 * Kalender: kisi satu bulan Masehi bergaya kalender dinding — tiap sel memuat angka tanggal
 * Masehi yang **besar**, angka tanggal Hijriah **kecil di dalam lingkaran** di samping kiri nama
 * **pasaran** (Legi/Pahing/Pon/Wage/Kliwon), dan penanda puasa sunnah/hari besar. Nama hari
 * ("Jumat") tampil di kepala kolom, sehingga tiap tanggal terbaca weton lengkapnya — mis.
 * "Jumat Kliwon" — persis permintaan pengguna (2026-10-16): hari dan pasaran saja, ditampilkan
 * di angka tiap nomor tanggalan.
 *
 * Dahulu kedua kartu hari menyertakan angka **neptu**, bulan-tahun Jawa ("Jumadilawal 1370"),
 * dan dua kalimat keterangan budaya yang panjang; semuanya dihapus — yang tersisa weton
 * (hari + pasaran) saja, dan keterangan panjangnya pindah ke layar keterangan lewat ikon Info
 * bersama seluruh keterangan panjang layar lain. Angka Hijriah diberi lingkaran agar tetap
 * menarik dan mudah dibedakan dari angka Masehi yang besar. Kisi berada di dalam kartu dengan
 * sel berbentuk persegi yang tak lagi menempel satu sama lain.
 *
 * Nama layarnya juga disederhanakan menjadi **"Kalender"** atas permintaan yang sama — kisi
 * utamanya memang berbulan Masehi, dan rincian sumber tanggal Hijriah/pasarannya tetap
 * dijelaskan di layar keterangan.
 *
 * Kisinya sengaja bulan **Masehi**, bukan bulan Hijriah: kalender dinding di Indonesia memang
 * berbulan Masehi beranotasi Hijriah-pasaran, dan membangun kisi Hijriah menuntut konversi
 * balik (Hijriah → Masehi) yang belum dimiliki [HijriCalc] — mengarang konversi kedua hanya
 * untuk ini menambah tempat yang bisa salah sendiri. Anotasinya dihitung maju per sel lewat
 * [HijriCalc] + [KalenderJawa] + [PuasaSunnah], semuanya murni dan teruji di JVM.
 *
 * Koreksi Hijriah diikuti dari Pengaturan (Store.HIJRI_CORR) supaya tanggal Hijriah di sini
 * selalu sama dengan yang tertera di beranda — dua kalender yang tidak sepakat lebih buruk
 * daripada satu yang salah. Nilainya dijepit aturan bersama saat dibaca karena jalur pemulihan
 * backup bisa menulis angka di luar rentangnya.
 */
@Composable
fun KalenderScreen(go: (String) -> Unit) {
    val store = rememberStore()
    val koreksiMentah by store.flow(Store.HIJRI_CORR, BawaanPreferensi.KOREKSI_HIJRIAH)
        .collectAsStateWithLifecycle(BawaanPreferensi.KOREKSI_HIJRIAH)
    val koreksi = BawaanPreferensi.koreksiHijriah(koreksiMentah)

    // Bulan yang sedang dilihat sebagai geseran dari bulan ini, dan hari yang dipilih pengguna —
    // keduanya tahan putar layar: melihat kalender Desember lalu memutar perangkat tidak boleh
    // melempar pengguna kembali ke bulan berjalan (penjaga KeadaanLayarSaatDiputarTest).
    var bulanGeser by rememberSaveable { mutableStateOf(0) }
    var hariPilih by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }

    val hariIni = remember { LocalDate.now() }
    val bulan = remember(bulanGeser) { YearMonth.now().plusMonths(bulanGeser.toLong()) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(NamaLayar.KALENDER, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            // Keterangan panjang (perkiraan Hijriah tabular, catatan budaya pasaran) tinggal di
            // layar keterangan — badan layar murni kalender.
            TombolInfo { go(Routes.keterangan("kalender")) }
        }

        KartuHariIni(hariIni, koreksi)

        // Navigasi bulan: dua tombol panah di tepi dengan label bulan di tengah — ringkas dan
        // tidak menyita lebar baris. Arah panahnya mengikuti arah baca (AutoMirrored).
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { bulanGeser-- }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Bulan sebelumnya")
            }
            Text(
                "${KalenderJawa.BULAN_MASEHI[bulan.monthValue - 1]} ${bulan.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { bulanGeser++ }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Bulan berikutnya")
            }
        }

        KisiBulan(
            bulan = bulan,
            hariIni = hariIni,
            koreksi = koreksi,
            terpilih = hariPilih,
            pilih = { hariPilih = it.toString() },
        )

        // Legenda kisi: dua titik warna yang maknanya tidak bisa ditebak tanpa ini.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TitikLegenda(MaterialTheme.colorScheme.primary, "Puasa sunnah")
            TitikLegenda(MaterialTheme.colorScheme.error, "Hari besar Islam")
        }

        KartuHariTerpilih(LocalDate.parse(hariPilih), koreksi)
    }
}

/**
 * Kartu ringkasan hari ini: Masehi (judul), Hijriah dengan koreksi (lencana), dan weton —
 * hari + pasaran saja; bulan-tahun Jawa ("Jumadilawal 1370") tidak lagi ditampilkan.
 */
@Composable
private fun KartuHariIni(t: LocalDate, koreksi: Int) {
    val (hHari, hBulan, hTahun) = HijriCalc.from(keKalender(t), koreksi)
    KartuEntri(
        judul = KalenderJawa.tanggalIndo(t),
        lencana = listOf("${hHari} ${HijriCalc.bulan[hBulan - 1]} $hTahun H"),
        isi = {
            Text(
                KalenderJawa.weton(t),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        },
    )
}

/** Kisi satu bulan: kepala tujuh **nama hari penuh** (Minggu duluan — urutan [KalenderJawa.HARI]). */
@Composable
private fun KisiBulan(
    bulan: YearMonth,
    hariIni: LocalDate,
    koreksi: Int,
    terpilih: String,
    pilih: (LocalDate) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Nama hari penuh dari [KalenderJawa.HARI] — satu sumber dengan weton di kartu; huruf
            // kecil 9sp agar tujuh nama itu muat sebaris di lebar layar mana pun.
            Row(Modifier.fillMaxWidth()) {
                KalenderJawa.HARI.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // Data sel dihitung sekali per bulan+koreksi, bukan per komposisi ulang: 31 konversi
            // Hijriah yang diulang tiap kali pengguna mengetuk sel hanya membakar waktu tanpa
            // hasil yang berbeda.
            val sel = remember(bulan, koreksi) {
                val awal = bulan.atDay(1)
                val kosong = awal.dayOfWeek.value % 7 // Minggu = 0 (DayOfWeek.SUNDAY == 7)
                val isi = (1..bulan.lengthOfMonth()).map { hari ->
                    val t = bulan.atDay(hari)
                    val (hHari, hBulan, _) = HijriCalc.from(keKalender(t), koreksi)
                    SelKalendar(
                        tanggal = t,
                        hijriHari = hHari,
                        pasaran = KalenderJawa.pasaran(t),
                        puasa = PuasaSunnah.puasaSunnah(hHari, hBulan, t.dayOfWeek.value),
                        besar = PuasaSunnah.hariBesar(hHari, hBulan),
                    )
                }
                List(kosong) { null } + isi
            }
            sel.chunked(7).forEach { baris ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    baris.forEach { s ->
                        if (s == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            // `weight` milik RowScope sehingga hanya bisa dipanggil di sini — di dalam
                            // SelBulan ia tak beresolusi. Ukurannya karena itu dioper sebagai modifier.
                            SelBulan(s, s.tanggal == hariIni, s.tanggal.toString() == terpilih,
                                Modifier.weight(1f), pilih)
                        }
                    }
                    if (baris.size < 7) repeat(7 - baris.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private data class SelKalendar(
    val tanggal: LocalDate,
    val hijriHari: Int,
    val pasaran: String,
    val puasa: List<String>,
    val besar: String?,
)

/**
 * Satu sel tanggal: angka Masehi **besar**, angka Hijriah **kecil berlingkaran** di samping
 * kiri nama pasaran, dan titik penanda di bawahnya — gaya kalender dinding Jawa. Selnya
 * berbentuk persegi ([aspectRatio]) dengan latar hanya pada hari ini dan hari terpilih — kisi
 * yang sepi lebih mudah dipindai daripada kisi yang tiap selnya berlatar.
 */
@Composable
private fun SelBulan(
    sel: SelKalendar,
    hariIni: Boolean,
    terpilih: Boolean,
    modifier: Modifier,
    pilih: (LocalDate) -> Unit,
) {
    val latar = when {
        hariIni -> MaterialTheme.colorScheme.primary
        terpilih -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val warnaUtama = when {
        hariIni -> MaterialTheme.colorScheme.onPrimary
        terpilih -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val warnaKecil = when {
        hariIni -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        terpilih -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(latar)
            .then(
                if (terpilih && !hariIni) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = { pilih(sel.tanggal) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
    ) {
        // Angka Masehi tetap yang besar — inilah identitas tanggal di kalender dinding.
        Text(
            "${sel.tanggal.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = warnaUtama,
        )
        // Angka Hijriah kecil di dalam lingkaran, di samping kiri nama pasaran — keduanya
        // berukuran mini agar baris ini tidak merebut perhatian dari angka Masehi di atasnya.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .border(1.dp, warnaKecil.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${sel.hijriHari}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                    color = warnaKecil,
                )
            }
            Text(
                sel.pasaran,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = warnaKecil,
                maxLines = 1,
            )
        }
        // Titik penanda: puasa sunnah warna pokok, hari besar warna galat — keduanya bisa hadir
        // bersamaan pada satu tanggal (mis. Senin 13 Syawal).
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (sel.puasa.isNotEmpty()) {
                Box(Modifier.size(4.dp).background(warnaUtama, CircleShape))
            }
            if (sel.besar != null) {
                Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.error, CircleShape))
            }
        }
    }
}

/** Kartu detail hari yang diketuk pengguna di kisi — weton (hari + pasaran) sebagai lencananya. */
@Composable
private fun KartuHariTerpilih(t: LocalDate, koreksi: Int) {
    val (hHari, hBulan, hTahun) = HijriCalc.from(keKalender(t), koreksi)
    val puasa = PuasaSunnah.puasaSunnah(hHari, hBulan, t.dayOfWeek.value)
    val besar = PuasaSunnah.hariBesar(hHari, hBulan)
    KartuEntri(
        judul = KalenderJawa.tanggalIndo(t),
        lencana = listOf(KalenderJawa.weton(t)),
        isi = {
            // Hijriah saja — bulan-tahun Jawa ("Jumadilawal 1370") tidak lagi ditampilkan.
            Text(
                "${hHari} ${HijriCalc.bulan[hBulan - 1]} $hTahun H",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (besar != null) {
                Text(
                    besar,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (puasa.isNotEmpty()) {
                Text(
                    puasa.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (besar == null && puasa.isEmpty()) {
                Text(
                    "Tanpa peringatan khusus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
    )
}

/** Satu butir legenda: titik warna kecil dengan maknanya di sampingnya. */
@Composable
private fun TitikLegenda(warna: Color, teks: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(warna, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(teks, style = MaterialTheme.typography.labelSmall)
    }
}

/** [LocalDate] → [Calendar] untuk [HijriCalc.from] — tanpa jam/menit supaya tidak membocori zona. */
private fun keKalender(t: LocalDate): Calendar = Calendar.getInstance().apply {
    clear()
    set(t.year, t.monthValue - 1, t.dayOfMonth)
}
