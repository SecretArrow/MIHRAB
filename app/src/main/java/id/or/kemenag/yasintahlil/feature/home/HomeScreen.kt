package id.or.kemenag.yasintahlil.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.ArabText
import id.or.kemenag.yasintahlil.core.designsystem.BagianJudul
import id.or.kemenag.yasintahlil.core.designsystem.GradienZamrud
import id.or.kemenag.yasintahlil.core.designsystem.MenuTile
import id.or.kemenag.yasintahlil.core.designsystem.NamaAntarmuka
import id.or.kemenag.yasintahlil.core.designsystem.OrnamenIslami
import id.or.kemenag.yasintahlil.core.util.FormatWaktu
import id.or.kemenag.yasintahlil.core.util.HijriCalc
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import id.or.kemenag.yasintahlil.navigation.Routes
import id.or.kemenag.yasintahlil.navigation.RuteKonten
import java.text.SimpleDateFormat
import java.util.*

/**
 * Beranda (`plan.txt` §"Beranda Utama").
 *
 * Seluruh keadaan datang dari [HomeViewModel] sebagai `StateFlow` — bukan `remember` di layar —
 * sehingga bertahan saat rotasi dan ikut berubah begitu pengguna mengganti lokasi/metode di layar
 * Jadwal tanpa membuka ulang beranda.
 *
 * Tata letaknya ringkas: kepala sapaan berikut ikon Pencarian di atas bidang mint bermotif khatam,
 * kartu waktu sholat berikutnya (seluruh kartunya dapat diketuk untuk membuka jadwal lengkap),
 * lalu kisi aksi cepat dua baris — enam tujuan yang paling sering dibuka. Saklar pengingat dan
 * ringkasan simpanan yang dahulu menumpuk di sini tinggal di rumahnya masing-masing: seluruh
 * pengaturan di tab Pengaturan, angka favorit & riwayat di layar Favorit & Riwayat.
 */
@Composable
fun HomeScreen(go: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    // Zona waktu dari aturan bersama (`JadwalHariIni`), bukan disusun ulang di sini: id yang tak
    // dikenal pernah membuat jam beranda memakai GMT (`TimeZone.getTimeZone` mengembalikan GMT
    // untuk id asing) sementara kartu info harian memakai zona perangkat — dua waktu, satu hari.
    val tz = remember(s.tzId) { JadwalHariIni.zona(s.tzId) }
    val now = remember(s.nowMillis, tz) { Calendar.getInstance(tz).apply { timeInMillis = s.nowMillis } }
    // `Locale.forLanguageTag` — bukan `Locale("id")` yang usang: konstruktor itu tidak membedakan
    // kode bahasa dari kode negara, sedangkan tag "id" di sini memang bahasa Indonesia.
    val jam = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id")).format(now.time)
    val masehi = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id")).format(now.time)
    val times = remember(s.lat, s.lon, s.asarHanafi, s.method, tz, s.corr, now.get(Calendar.DAY_OF_YEAR)) {
        // Parameter disusun lewat `JadwalHariIni` yang sama dengan penjadwal pengingat & menu
        // Adzan, sehingga hitung mundur di beranda tidak dapat berbeda dari jam yang benar-benar
        // dijadwalkan hanya karena aturannya ditulis ulang di sini.
        PrayerCalc.compute(now, JadwalHariIni.params(
            lat = s.lat, lon = s.lon, method = s.method,
            asarHanafi = s.asarHanafi, corr = s.corr, tz = tz,
        ))
    }
    // Menit dinding lewat aturan bersama `PrayerCalc.menitHari` — bukan `h * 60 + m` yang
    // diketik di sini. Aritmetika yang sama sudah dipakai penjadwal pengingat; mengejanya kembali
    // membuat jam yang dihitung layar dan jam yang dijadwalkan dapat berbeda tanpa pesan galat.
    val nowMin = PrayerCalc.menitHari(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    // Nama berikutnya dan jamnya datang sebagai satu nilai dari aturan bersama (`JadwalHariIni`),
    // sehingga hitung mundur ini tidak dapat menunjuk waktu yang berlainan daripada baris jam di
    // bawahnya. Dahulu `PrayerCalc.next` menuliskan pasangan nama→jamnya sendiri dan jamnya dicari
    // ulang di sini dengan cabang cadangan `?: times.isya` — cabang yang diam-diam menampilkan jam
    // sholat lain lebih buruk daripada tak ada.
    val berikut = JadwalHariIni.berikutnya(nowMin, times)
    // Baris lima waktu memakai daftar nama & jam yang sama dengan hitung mundur di atas dan
    // dengan penjadwal pengingat (`LIMA_WAKTU`/`adzan`), bukan pasangan nama→field yang
    // ditulis ulang di sini — salinan itu dapat menampilkan jam yang berbeda dari waktu yang
    // sedang dihitung mundurnya.
    val jadwal = JadwalHariIni.LIMA_WAKTU.zip(JadwalHariIni.adzan(times))
    // Aksi cepat: label dari satu sumber nama (`NamaLayar`) + rute + ikon. Enam tujuan paling
    // sering dibuka — dua baris utuh, bukan deretan ubin yang mengisi seluruh layar; tujuan lain
    // tinggal di menu Ibadah/Alat, Pencarian punya ikonnya sendiri di kepala, dan jadwal dibuka
    // dengan mengetuk kartu waktu sholat.
    val aksiCepat = listOf(
        Triple(NamaLayar.YASIN, Routes.YASIN, Icons.AutoMirrored.Filled.MenuBook),
        Triple(NamaLayar.TAHLIL, Routes.TAHLIL, Icons.Filled.VolunteerActivism),
        Triple(NamaLayar.DZIKIR, Routes.DZIKIR, Icons.Filled.AutoAwesome),
        Triple(NamaLayar.SHOLAT, Routes.SHOLAT, Icons.Filled.SelfImprovement),
        Triple(NamaLayar.KIBLAT, Routes.KIBLAT, Icons.Filled.Explore),
        Triple(NamaLayar.FAVORIT, Routes.FAVORIT, Icons.Filled.Favorite),
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Kepala sapaan: bidang mint penuh dengan ornamen khatam di belakangnya — tekstur, bukan
        // hiasan yang mengurangi keterbacaan (`plan.txt` §"Konsep dan Identitas Aplikasi"). Ikon
        // Pencarian menempel di ujungnya: pintu termurah ke seluruh konten aplikasi.
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)) {
            OrnamenIslami(Modifier.matchParentSize())
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 18.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Assalamu'alaikum",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "$masehi • $jam",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            HijriCalc.format(now, s.hijriCorr),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                IconButton(onClick = { go(Routes.SEARCH) }) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = NamaLayar.SEARCH,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            // Kartu waktu sholat berikutnya: satu-satunya blok berwarna penuh di beranda, supaya
            // mata jatuh ke sana lebih dulu — informasi paling penting bagi pengguna harian.
            // Seluruh kartunya dapat diketuk ke jadwal lengkap; metode hisabnya dibaca di layar
            // itu, bukan diulang di sini.
            Box(
                Modifier.fillMaxWidth().padding(top = 14.dp)
                    .clip(MaterialTheme.shapes.large).background(GradienZamrud)
                    .clickable { go(Routes.JADWAL) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            s.city,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "Waktu sholat berikutnya",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                            Text(
                                berikut.nama,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                            )
                            Text(
                                berikut.jam,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                FormatWaktu.durasiJamMenit(berikut.sisaMenit),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                            )
                            Text(
                                "lagi",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        jadwal.forEach { (nama, jamnya) ->
                            val aktif = nama == berikut.nama
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (aktif) Color.White.copy(alpha = 0.20f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    nama,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (aktif) Color.White
                                    else Color.White.copy(alpha = 0.7f),
                                )
                                Text(
                                    jamnya,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                    // Penanda bahwa kartunya dapat diketuk — satu baris kecil, bukan tombol
                    // penuh lebar yang mengulangi tujuan yang sama.
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Jadwal lengkap & lokasi",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Aksi cepat: kisi tiga ubin berikon, bukan deretan tombol teks.
            BagianJudul("Aksi cepat")
            aksiCepat.chunked(3).forEach { baris ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    baris.forEach { (label, rute, ikon) ->
                        MenuTile(label, ikon, { go(rute) }, Modifier.weight(1f))
                    }
                    if (baris.size < 3) Spacer(Modifier.weight(1f))
                }
            }

            if (s.last.isNotBlank()) KartuIsi {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IkonBulat(Icons.AutoMirrored.Filled.MenuBook)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${NamaAntarmuka.TERAKHIR_DIBACA}: ${s.last}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    // Tujuan ditentukan `RuteKonten` — aturan yang sama dengan layar Favorit — bukan
                    // cabang `startsWith("yasin")` yang dahulu mengirim setiap bacaan Tahlil ke Favorit.
                    // Id kelompok dzikir dari keadaan layar ikut disertakan: dahulu tidak, sehingga
                    // jenis yang sama terbaca berbeda di beranda dan di layar Favorit.
                    val tujuan = RuteKonten.tujuanTerakhir(s.last, s.grupDzikir)
                    if (tujuan != null) {
                        Text(
                            tujuan.nama,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        FilledTonalButton(
                            onClick = { go(tujuan.rute) },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { Text(NamaAntarmuka.LANJUTKAN) }
                    } else {
                        Text(
                            RuteKonten.TIDAK_DIKENAL,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            // Kartu info harian dapat dimatikan dari Pengaturan; isinya berganti mengikuti tanggal
            // dan diambil dari pustaka dzikir yang sudah terverifikasi (`InfoHarian`).
            if (s.infoCard) KartuIsi {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IkonBulat(
                            Icons.Filled.AutoAwesome,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Info Islami harian", style = MaterialTheme.typography.titleMedium)
                    }
                    val gagal = s.infoGagal
                    val b = s.info
                    when {
                        gagal != null -> Text(
                            gagal,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        b == null -> Text(
                            "Belum ada butir untuk hari ini.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        else -> {
                            Text(
                                b.judul,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            // Ukuran Arab di kartu ini tetap: kartu beranda adalah ringkasan, bukan
                            // permukaan bacaan — ukuran pilihan pengguna berlaku di pustaka dzikirnya.
                            // Namun chip "Arab" di Pengaturan tetap dihormati: bila dimatikan, teks Arab
                            // disembunyikan di sini juga, agar preferensi itu tak menjadi kontrol mati.
                            // Penjaga dan pemanggilnya sengaja satu baris: pemindai penjaga
                            // `ReadingDisplayPrefsTest` membaca dari awal baris pemanggilan.
                            if (s.showArab && b.arab.isNotBlank()) ArabText(
                                b.arab, sizeSp = 20f, modifier = Modifier.padding(top = 6.dp)
                            )
                            if (b.teks.isNotBlank()) {
                                Text(b.teks, modifier = Modifier.padding(top = 6.dp))
                            }
                            if (b.sumber.isNotBlank()) {
                                Text(
                                    "Sumber: ${b.sumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            TextButton(onClick = { go(Routes.DZIKIR) }) { Text("Buka pustaka dzikir") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Kartu isi standar beranda: permukaan terangkat, sudut besar, garis tepi tipis. */
@Composable
private fun KartuIsi(konten: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) { konten() }
    }
}

/** Wadah ikon bulat di dalam kartu — wadah zamrud lembut, atau warna lain bila [warna] diisi. */
@Composable
private fun IkonBulat(
    ikon: ImageVector,
    warna: Color = MaterialTheme.colorScheme.primaryContainer,
    warnaIkon: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(shape = CircleShape, color = warna) {
        Icon(
            ikon,
            contentDescription = null,
            tint = warnaIkon,
            modifier = Modifier.padding(10.dp).size(20.dp),
        )
    }
}
