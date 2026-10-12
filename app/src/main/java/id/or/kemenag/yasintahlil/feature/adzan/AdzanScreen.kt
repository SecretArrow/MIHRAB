package id.or.kemenag.yasintahlil.feature.adzan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.audio.AdzanPreview
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.DaftarSuaraAdzan
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.SuaraAdzan
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.scheduling.NotifChannels
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import id.or.kemenag.yasintahlil.core.util.PesanKonten
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Pengaturan adzan & pengingat: saklar per waktu, daftar **jenis suara** (berkas audio pilihan
 * pengguna yang diberi nama — aplikasi tidak menyertakan audio bawaan karena lisensi), volume,
 * jeda pengingat sebelum adzan, penembus mode senyap, pratinjau, serta pintasan izin sistem
 * (notifikasi, optimasi baterai, akses DND).
 */
@Composable
fun AdzanScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val lafaz = remember { repo.adzan() }
    var on by remember { mutableStateOf(List(PengingatAdzan.JUMLAH_WAKTU) { true }) }
    var uri by remember { mutableStateOf("") }
    var sounds by remember { mutableStateOf<List<SuaraAdzan>>(emptyList()) }
    var ubahNama by remember { mutableStateOf<SuaraAdzan?>(null) }
    var vol by remember { mutableStateOf(PengingatAdzan.VOL_DEFAULT) }
    var preMin by remember { mutableStateOf(PengingatAdzan.PRE_MIN_DEFAULT) }
    var doaReminder by remember { mutableStateOf(false) }
    var dnd by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    // Metode hisab yang benar-benar dipakai menjadwalkan pengingat. Sebelumnya layar ini menulis
    // nama metode secara hardcode — selalu Kemenag/SIHAT — sehingga berbohong begitu pengguna
    // memilih metode lain di menu Jadwal.
    var metode by remember { mutableStateOf(PrayerCalc.Method.KEMENAG) }
    // Sisa parameter hisab, dibaca dari kunci yang sama dengan menu Jadwal & penjadwal pengingat.
    // `plan.txt` §"Fitur Adzan" membuka daftar fitur wajibnya dengan *"Jadwal adzan untuk Subuh,
    // Zuhur, Asar, Magrib, dan Isya"*: saklar per waktu di bawah hanya menyebut nama waktunya,
    // sehingga layar ini tidak dapat menjawab "jam berapa adzan berbunyi" tanpa membuka menu lain.
    var lat by remember { mutableStateOf(JadwalHariIni.LAT_DEFAULT) }
    var lon by remember { mutableStateOf(JadwalHariIni.LON_DEFAULT) }
    var tzId by remember { mutableStateOf(JadwalHariIni.TZ_DEFAULT) }
    var corr by remember { mutableStateOf(JadwalHariIni.CORR_DEFAULT) }
    var asar by remember { mutableStateOf(JadwalHariIni.ASAR_DEFAULT) }
    // Berkas audio pengguna bisa saja tak terbaca (URI dicabut, format tak didukung, izin hilang).
    // Sebelumnya pratinjau diam saja saat gagal; kini sebabnya ditampilkan + tombol coba lagi.
    // Pemutarannya sendiri ada di lapisan `core.audio`, bukan di layar (plan.txt: lapisan audio
    // terpisah) — layar hanya memanggil play/stop/volume.
    var putarGagal by remember { mutableStateOf<String?>(null) }
    val player = remember { AdzanPreview(ctx) { putarGagal = it } }
    DisposableEffect(player) { onDispose { player.release() } }

    /** Jenis suara yang tampil: tersimpan, ditambah berkas aktif lama yang belum tercatat. */
    val daftarSuara = remember(sounds, uri) { DaftarSuaraAdzan.tampil(sounds, uri) }

    /** Simpan daftar jenis suara lalu tulis ulang berkas aktifnya dalam satu langkah. */
    fun simpanSuara(baru: List<SuaraAdzan>, aktifBaru: String) {
        sounds = baru
        uri = aktifBaru
        scope.launch {
            store.set(Store.ADZAN_SOUNDS, DaftarSuaraAdzan.serialize(baru))
            store.set(Store.ADZAN_URI, aktifBaru)
            PrayerScheduler.scheduleAll(ctx.applicationContext)
        }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        u?.let {
            // Simpan izin baca agar berkas tetap dapat diputar pekerja latar setelah app ditutup.
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val dipilih = it.toString()
            // Berkas yang baru dipilih langsung menjadi jenis suara aktif — itu yang diharapkan.
            simpanSuara(DaftarSuaraAdzan.tambah(sounds, DaftarSuaraAdzan.namaDariUri(dipilih), dipilih), dipilih)
        }
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        on = PengingatAdzan.parseOn(store.get(Store.ADZAN_ON, PengingatAdzan.ON_DEFAULT))
        uri = store.get(Store.ADZAN_URI, BawaanPreferensi.BERKAS_ADZAN)
        sounds = DaftarSuaraAdzan.parse(store.get(Store.ADZAN_SOUNDS, BawaanPreferensi.DAFTAR_SUARA_ADZAN))
        vol = store.get(Store.ADZAN_VOL, PengingatAdzan.VOL_DEFAULT)
        preMin = PengingatAdzan.preMenit(
            store.get(Store.ADZAN_PRE_MIN, PengingatAdzan.PRE_MIN_DEFAULT))
        doaReminder = store.get(Store.DOA_ADZAN_REMINDER, BawaanPreferensi.PENGINGAT_DOA_ADZAN)
        dnd = NotifChannels.canBypassDnd(ctx.applicationContext)
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        metode = PrayerCalc.Method.from(store.get(Store.METHOD, JadwalHariIni.METODE_DEFAULT))
        lat = store.get(Store.LAT, JadwalHariIni.LAT_DEFAULT)
        lon = store.get(Store.LON, JadwalHariIni.LON_DEFAULT)
        tzId = store.get(Store.TZ, JadwalHariIni.TZ_DEFAULT)
        corr = store.get(Store.CORR, JadwalHariIni.CORR_DEFAULT)
        asar = store.get(Store.ASAR, JadwalHariIni.ASAR_DEFAULT)
    }

    // Nama lima waktu dari aturan bersama, bukan daftar lokal: indeksnya harus sama dengan CSV
    // `Store.ADZAN_ON` yang ditulis saklar di bawah dan dibaca penjadwal pengingat.
    val names = JadwalHariIni.LIMA_WAKTU
    // Jadwal hari ini dihitung dengan aturan yang sama persis dengan penjadwal pengingat
    // (`JadwalHariIni`), jadi jam yang terlihat di sini adalah jam yang benar-benar dijadwalkan —
    // bukan perkiraan terpisah yang bisa menyimpang diam-diam.
    val tz = remember(tzId) { JadwalHariIni.zona(tzId) }
    val jam = remember(lat, lon, metode, asar, corr, tz) {
        JadwalHariIni.adzan(PrayerCalc.compute(Calendar.getInstance(tz), JadwalHariIni.params(
            lat = lat, lon = lon, method = metode, asarHanafi = JadwalHariIni.asarHanafi(asar),
            corr = JadwalHariIni.parseCorr(corr), tz = tz,
        )))
    }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Adzan & Pengingat", style = MaterialTheme.typography.titleLarge)
        Text("Aktif/nonaktif per waktu:", style = MaterialTheme.typography.titleMedium)
        names.forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(n)
                    // Jam adzan dan (bila aktif) jam pengingat sebelum adzan, keduanya dihitung
                    // dari koreksi yang sama dengan pekerja latar.
                    Text(
                        buildString {
                            append("Adzan ${jam[i]}")
                            if (preMin > 0) {
                                append(" • pengingat ${JadwalHariIni.geser(jam[i], -preMin)}")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Switch(on[i], {
                    on = on.toMutableList().also { it[i] = !it[i] }
                    scope.launch {
                        store.set(Store.ADZAN_ON, PengingatAdzan.serializeOn(on))
                        PrayerScheduler.scheduleAll(ctx.applicationContext)
                    }
                })
            }
        }
        Text("Jam di atas dihitung di perangkat untuk hari ini dari lokasi, metode, mazhab Asar, " +
                "koreksi menit, dan zona waktu yang sama dengan menu Jadwal (metode ${metode.label}). " +
                "Menonaktifkan sebuah waktu mematikan adzan sekaligus pengingatnya.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))

        // `plan.txt` §"Fitur Adzan": *"Pilihan suara adzan dari sumber audio yang legal digunakan"*
        // dan *"Pengaturan volume dan jenis suara"*. Aplikasi tidak menyertakan audio bawaan, jadi
        // jenis suara = berkas pilihan pengguna yang diberi nama; beberapa boleh disimpan sekaligus
        // dan satu dipilih sebagai yang aktif.
        Text("Jenis suara adzan", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        Text("Aplikasi tidak menyertakan audio bawaan karena lisensi: setiap jenis suara adalah " +
                "berkas pilihan Anda sendiri yang diberi nama. Simpan beberapa (mis. \"adzan masjid " +
                "lokal\") lalu pilih satu yang dipakai pengingat. Nama hanya penanda pilihan Anda.",
            style = MaterialTheme.typography.labelSmall)
        if (daftarSuara.isEmpty()) {
            Text("Belum ada jenis suara tersimpan.", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp))
        }
        daftarSuara.forEach { s ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = s.uri == uri, onClick = {
                        // Hanya berkas aktif yang berubah, jadi penjadwal & pekerja latar tetap
                        // membaca satu kunci yang sama.
                        uri = s.uri
                        scope.launch {
                            store.set(Store.ADZAN_URI, s.uri)
                            PrayerScheduler.scheduleAll(ctx.applicationContext)
                        }
                    })
                    Column {
                        Text(s.nama, style = MaterialTheme.typography.bodyMedium)
                        Text(if (s.uri == uri) "Aktif" else "Tidak aktif",
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row {
                    TextButton(onClick = { ubahNama = s }) { Text("Ubah nama") }
                    TextButton(
                        // Jenis suara terakhir tidak dapat dihapus: menghapusnya menyisakan layar
                        // tanpa berkas apa pun yang bisa dipilih atau dipratinjau.
                        enabled = daftarSuara.size > 1,
                        onClick = { simpanSuara(DaftarSuaraAdzan.hapus(daftarSuara, s.uri), DaftarSuaraAdzan.aktifSetelahHapus(daftarSuara, s.uri, uri)) },
                    ) { Text("Hapus") }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pick.launch(arrayOf("audio/*")) }) { Text("Tambah jenis suara") }
            Button(onClick = {
                if (uri.isNotBlank()) {
                    putarGagal = null
                    player.play(uri, vol)
                }
            }, enabled = uri.isNotBlank()) { Text("Pratinjau") }
            OutlinedButton(onClick = { player.stop() }) { Text("Stop") }
        }
        Text(if (uri.isBlank()) "Belum ada berkas dipilih"
                else "Aktif: ${DaftarSuaraAdzan.namaAktif(daftarSuara, uri)}",
            style = MaterialTheme.typography.labelSmall)
        putarGagal?.let { pesan ->
            ErrorState(pesan, onRetry = {
                putarGagal = null
                player.play(uri, vol)
            })
        }
        Text("Volume: ${(vol * 100).toInt()}%")
        Slider(vol, { vol = it; player.setVolume(it); scope.launch { store.set(Store.ADZAN_VOL, vol) } })

        Text("Pengingat sebelum adzan: ${if (preMin == 0) "nonaktif" else "$preMin menit"}")
        // Rentang dan bawaannya dari aturan bersama, sama dengan yang dijepit penjadwal & pekerja
        // latar — sehingga angka yang dapat dipilih di sini pasti angka yang dipakai notifikasi.
        Slider(
            value = preMin.toFloat(),
            onValueChange = { preMin = it.toInt() },
            valueRange = PengingatAdzan.PRE_MIN_MIN_MENIT.toFloat()..PengingatAdzan.PRE_MIN_MAKS_MENIT.toFloat(),
            steps = PengingatAdzan.PRE_MIN_MAKS_MENIT - PengingatAdzan.PRE_MIN_MIN_MENIT - 1,
            onValueChangeFinished = { scope.launch { store.set(Store.ADZAN_PRE_MIN, preMin); PrayerScheduler.scheduleAll(ctx.applicationContext) } },
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(PengingatAdzan.NAMA_PENGINGAT_DOA)
            Switch(doaReminder, {
                doaReminder = it
                scope.launch {
                    store.set(Store.DOA_ADZAN_REMINDER, doaReminder)
                    PrayerScheduler.scheduleAll(ctx.applicationContext)
                }
            })
        }
        // Kalimat sebab, ikut-saklar, dan batas syariat datang dari aturan bersama
        // (`PengingatAdzan`) — layar Iqomah menjelaskan saklar yang sama dengan kata-kata yang sama.
        // Yang tetap khas layar ini hanyalah ke mana ketukannya membuka: layar Doa Adzan.
        Text("Bila aktif: ${PengingatAdzan.DOA_SEBAB}. Ketukannya langsung membuka layar Doa Adzan. " +
                "${PengingatAdzan.DOA_IKUT_SAKLAR} ${PengingatAdzan.DOA_BUKAN_SYARIAT}",
            style = MaterialTheme.typography.labelSmall)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tembus mode senyap/DND")
            Switch(dnd, {
                // Penembusan DND diatur di sistem, bukan oleh aplikasi: buka Pengaturan akses.
                openDndAccessSettings(ctx)
            })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { openDndAccessSettings(ctx) }) { Text("Atur akses DND") }
            TextButton(onClick = { dnd = NotifChannels.canBypassDnd(ctx.applicationContext) }) { Text("Periksa status") }
        }
        Text(if (dnd) "Akses Kebijakan Notifikasi: diberikan. Pengingat dapat dikecualikan dari mode senyap/DND."
                else "Akses Kebijakan Notifikasi: belum diberikan. Aktifkan lewat \"Atur akses DND\" di Pengaturan sistem.",
            style = MaterialTheme.typography.labelSmall)

        Text("Izin yang dibutuhkan:", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                else openNotificationSettings(ctx)
            }) { Text("Izin notifikasi") }
            OutlinedButton(onClick = { openBatterySettings(ctx) }) { Text("Optimasi baterai") }
        }
        Text("Pengingat dijadwalkan lewat WorkManager (tanpa izin alarm tepat waktu): waktu dapat " +
                "bergeser beberapa menit saat perangkat tidur (Doze) atau dibatasi optimasi baterai — " +
                "buka \"Optimasi baterai\" di atas agar lebih tepat. Jadwal dihitung di perangkat " +
                "dengan metode ${metode.label} (Fajr ${metode.fajr}°, " +
                (if (metode.ishaIntervalMin != null) "Isya Magrib+${metode.ishaIntervalMin} menit"
                 else "Isya ${metode.isha}°") +
                "); ubah metode, koreksi menit, dan zona waktu di menu Jadwal.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp))

        Text("Lafaz adzan:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { settings = true }) { Text("Pengaturan bacaan") }
        }
        if (lafaz.isEmpty()) {
            EmptyState("Lafaz adzan tidak tersedia",
                PesanKonten.berkasTidakAda("lafaz adzan"))
        }
        // Urutan lafaz diberi nomor, dan tiap kartu menyebut sumbernya. Sebelumnya kartu hanya
        // berjudul "Adzan" dan tanpa sumber, sehingga lafaz yang berbunyi sama berulang kali
        // (takbir, syahadat, dan seruan hayya) tak dapat dibedakan satu sama lain. Karena judulnya
        // sama, id kartunya pun bertabrakan (`l.arab.take(10)`), dan kartu dengan id kembar berbagi
        // satu entri favorit — menandai satu lafaz membuat lafaz lain ikut bertanda. Nomor urut
        // menjadikan id-nya unik.
        lafaz.forEachIndexed { i, l ->
            BacaanCard(JenisBacaan.id(JenisBacaan.ADZAN, (i + 1).toString()),
                "Lafaz adzan ${i + 1} dari ${lafaz.size}",
                l.arab, l.latin, l.terjemahan,
                true, true, arabSize, ulangi = l.ulangi, sumber = l.sumber,
                showArab = showArab, kategori = l.kategori)
        }
        Text(repo.adzanCatatan(), style = MaterialTheme.typography.labelMedium)
    }

    ubahNama?.let { s ->
        var namaBaru by remember(s) { mutableStateOf(s.nama) }
        AlertDialog(
            onDismissRequest = { ubahNama = null },
            title = { Text("Ubah nama jenis suara") },
            text = {
                Column {
                    Text("Nama ini hanya penanda pilihan Anda — bukan nama muadzin atau masjid " +
                            "tertentu.", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(namaBaru, { namaBaru = it }, label = { Text("Nama") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Nama dikosongkan = kembali ke nama bawaan dari nama berkas, bukan entri tanpa
                    // label yang tak dapat dibedakan dari yang lain.
                    simpanSuara(DaftarSuaraAdzan.ubahNama(daftarSuara, s.uri, namaBaru), uri)
                    ubahNama = null
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { ubahNama = null }) { Text("Batal") } },
        )
    }

    if (settings) {
        BacaanSettingsSheet(
            onDismiss = { settings = false },
            showArab = showArab, onShowArab = { showArab = it; scope.launch { store.set(Store.SHOW_ARAB, it) } },
            arabSize = arabSize, onArabSize = { arabSize = it; scope.launch { store.set(Store.ARAB_SIZE, it) } },
        )
    }
}

private fun openNotificationSettings(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

private fun openBatterySettings(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

private fun openDndAccessSettings(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
