package id.or.kemenag.yasintahlil.feature.kiblat

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.BarisChip
import id.or.kemenag.yasintahlil.core.designsystem.ErrorState
import id.or.kemenag.yasintahlil.core.designsystem.KartuKeterangan
import id.or.kemenag.yasintahlil.core.designsystem.LoadingState
import id.or.kemenag.yasintahlil.core.designsystem.NamaAntarmuka
import id.or.kemenag.yasintahlil.core.designsystem.TampilanKompas
import id.or.kemenag.yasintahlil.core.designsystem.TombolInfo
import id.or.kemenag.yasintahlil.core.location.LocationProvider
import id.or.kemenag.yasintahlil.core.sensor.KalibrasiKompas
import id.or.kemenag.yasintahlil.core.sensor.StatusKompas
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Kompas kiblat: azimut matematis (Ka'bah SIHAT Kemenag) + jarum sensor. Akurasi sensor tidak dijamin. */
@Composable
fun KiblatScreen(go: (String) -> Unit = {}, vm: KiblatViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // rememberSaveable: mode layar penuh pilihan pengguna tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    FullscreenEffect(fullscreen)

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> vm.onPermissionResult(result.values.any { it }) }

    // Sinkronkan status izin saat layar dibuka kembali dari Pengaturan sistem.
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && vm.state.value.useGps &&
                LocationProvider.hasPermission(ctx)) {
                vm.refreshLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(s.useGps, s.hasPermission) {
        if (s.useGps && s.hasPermission) vm.refreshLocation()
    }

    val kiblat = s.bearing
    val arah = listOf("U", "TL", "T", "TG", "S", "BD", "B", "BL")[(kiblat / 45.0).roundToInt() % 8]
    // Satuan dan konversinya adalah satu aturan bersama (`TampilanKompas`), bukan perhitungan yang
    // ditulis ulang di layar ini: chip satuan di Pengaturan dan angka di sini harus tidak dapat
    // berbeda pendapat soal apa arti "mil".
    val azimutTeks = TampilanKompas.formatAzimut(kiblat, s.unit)

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(NamaLayar.KIBLAT, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("kiblat")) }
        }
        // Azimut adalah jawaban utama layar ini, jadi ia tampil besar di depan; kompas dan peta di
        // bawahnya menjadi alat verifikasi, bukan sebaliknya.
        Text("$azimutTeks $arah", style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary)
        Text("dari (%.4f, %.4f)%s".format(
            s.lat, s.lon, if (s.liveFix) " • GPS live" else ""),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Judul dahulu berbunyi "(SIHAT Kemenag)" — melekatkan nama lembaga pada angka yang justru
        // dihitung sendiri di perangkat. Yang benar-benar berasal dari SIHAT adalah **koordinat
        // Ka'bah** yang dipakai sebagai titik acuan, jadi itulah yang disebut. Penjelasan lengkap
        // (rashdul kiblat, rincian kompas & peta) dibuka lewat ikon Info — kartu keterangannya
        // dihapus karena mengulang isi layar keterangan itu.
        Text(
            "Azimut dihitung di perangkat dari koordinat Ka'bah yang diterbitkan SIHAT Bimas Islam (E-Kiblat).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            if (s.useGps) "Sumber lokasi: GPS perangkat."
            else "Sumber lokasi: manual/kota terpilih (atur di menu Jadwal).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!s.hasPermission) {
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Izin lokasi diperlukan untuk memakai GPS.", style = MaterialTheme.typography.titleSmall)
                    Text("Tanpa izin, aplikasi tetap berfungsi: pilih kota di menu Jadwal lalu gunakan sudut azimut di atas dengan peta.",
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = {
                            permLauncher.launch(LocationProvider.IZIN.toTypedArray())
                        }) { Text("Izinkan lokasi") }
                        OutlinedButton(onClick = {
                            runCatching {
                                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", ctx.packageName, null)))
                            }
                        }) { Text("Buka pengaturan") }
                    }
                }
            }
        }

        // Indikator status kalibrasi (plan.txt §"Arah Kiblat") — ditampilkan **selalu**, bukan hanya
        // saat bermasalah. Layar yang hanya diam saat semuanya baik membuat pengguna tak tahu apakah
        // jarum yang dilihatnya layak dipercaya; sebaliknya, "terkalibrasi" tidak boleh diklaim
        // sebelum ada satu pun pembacaan sensor.
        val status = KalibrasiKompas.status(s.hasCompass, s.calibrated)
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Status kalibrasi sensor", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${KalibrasiKompas.tanda(status)} ${KalibrasiKompas.label(status)}",
                    color = when {
                        KalibrasiKompas.perluPerhatian(status) -> MaterialTheme.colorScheme.error
                        status == StatusKompas.TERKALIBRASI -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (KalibrasiKompas.perluPolaDelapan(status)) Text(
                    "Gerakkan perangkat membentuk pola angka delapan, jauhkan dari magnet, casing " +
                        "magnetik, dan benda logam, lalu ulangi sampai statusnya berubah.",
                    style = MaterialTheme.typography.labelSmall)
                if (status == StatusKompas.TANPA_SENSOR) Text(
                    "Perangkat ini tidak punya magnetometer, jadi kompas langsung tidak tersedia. " +
                        "Pakai angka azimut dan peta di bawah.",
                    style = MaterialTheme.typography.labelSmall)
                if (status == StatusKompas.TERKALIBRASI) Text(
                    "Akurasi kompas tetap tidak dijamin: casing magnetik, benda logam, dan medan " +
                        "magnet di sekitar dapat menyimpangkannya. Bandingkan dengan peta dan angka azimut.",
                    style = MaterialTheme.typography.labelSmall)
            }
        }

        // Keadaan menunggu/gagal GPS: sebelumnya layar diam saja saat GPS tak kunjung memberi
        // perbaikan, sehingga pengguna mengira aplikasinya rusak.
        if (s.useGps && s.hasPermission && s.mencari) {
            LoadingState("Mencari perbaikan lokasi GPS…")
        }
        if (s.useGps) s.pesanLokasi?.let { pesan ->
            ErrorState(pesan, onRetry = { vm.refreshLocation() })
        }

        // Jarum hanya digambar bila sensornya ada **dan** pengguna memilih tampilan kompas. Tanpa
        // magnetometer sudut hadap perangkat tak diketahui, sehingga jarum akan menunjuk arah yang
        // salah padahal tampak meyakinkan — plan.txt meminta perangkat tanpa magnetometer dilayani
        // lewat peta/sudut hasil perhitungan, bukan kompas yang mengarang arah.
        val gambarJarum = TampilanKompas.gambarJarum(s.tampilan, s.hasCompass)
        if (gambarJarum) {
            Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier.size(240.dp).semantics {
                        contentDescription = "Kompas arah kiblat: kiblat $azimutTeks ke arah $arah, " +
                            "perangkat menghadap ${"%.0f".format(s.azimuth)} derajat. " +
                            "Akurasi sensor tidak dijamin."
                    }
                ) {
                    val c = center
                    val r = size.minDimension / 2
                    drawCircle(Color(0xFF0B7A55), r, c)
                    drawCircle(Color.White, r - 4, c)
                    val q = Math.toRadians(kiblat)
                    val a = Math.toRadians(s.azimuth.toDouble())
                    // Utara perangkat.
                    drawLine(Color.Gray, c, c + Offset(-sin(a).toFloat() * r, (-cos(a)).toFloat() * r), 4f)
                    // Arah kiblat relatif terhadap layar = azimut kiblat − arah hadap perangkat.
                    val rel = q - a
                    drawLine(Color(0xFFC9A227), c,
                        c + Offset(sin(rel).toFloat() * (r - 12), (-cos(rel)).toFloat() * (r - 12)), 10f)
                    drawCircle(Color.Red, 8f, c)
                }
            }
            Text("Jarum emas = kiblat • garis abu = utara.",
                style = MaterialTheme.typography.labelSmall)
        } else {
            Text("Gunakan angka azimut di atas bersama peta di bawah.",
                style = MaterialTheme.typography.labelSmall)
        }
        Text("Peta arah kiblat (offline)", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp))
        KiblatMap(s.lat, s.lon, s.bearing, Modifier.padding(top = 4.dp))

        BarisChip(Modifier.padding(top = 8.dp)) {
            FilterChip(s.useGps, {
                if (!s.hasPermission) {
                    permLauncher.launch(LocationProvider.IZIN.toTypedArray())
                } else {
                    vm.setUseGps(!s.useGps)
                }
            }, { Text("Pakai GPS") })
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH) })
            OutlinedButton(onClick = { vm.refreshLocation() }, enabled = s.hasPermission) { Text("Perbarui lokasi") }
        }
        // Satuan azimut dan tampilan kompas (jarum & peta, atau angka & peta) tidak lagi punya
        // chipnya di sini — rumahnya di tab Pengaturan (bagian "Kompas kiblat") bersama seluruh
        // pengaturan lain, dan pilihan itu langsung berlaku di layar ini. Satu pengaturan, satu rumah.
        Text("Satuan azimut dan tampilan kompasnya diatur di tab Pengaturan (bagian \"Kompas kiblat\").",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
    }
}
