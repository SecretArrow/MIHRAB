package id.or.kemenag.yasintahlil.feature.kiblat

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.ErrorState
import id.or.kemenag.yasintahlil.core.designsystem.LoadingState
import id.or.kemenag.yasintahlil.core.designsystem.TampilanKompas
import id.or.kemenag.yasintahlil.core.location.LocationProvider
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Kompas kiblat: azimut matematis (Ka'bah SIHAT Kemenag) + jarum sensor. Akurasi sensor tidak dijamin. */
@Composable
fun KiblatScreen(vm: KiblatViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var fullscreen by remember { mutableStateOf(false) }
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
    val azimutTeks =
        if (s.unit == "mil") "${(kiblat * MIL_PER_DEG).roundToInt()} mil"
        else "%.1f°".format(kiblat)

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Arah Kiblat (SIHAT Kemenag)", style = MaterialTheme.typography.titleLarge)
        Text("Azimut: $azimutTeks $arah dari (%.4f, %.4f)%s".format(
            s.lat, s.lon, if (s.liveFix) " • GPS live" else ""))
        Text(
            if (s.useGps) "Sumber lokasi: GPS perangkat."
            else "Sumber lokasi: manual/kota terpilih (atur di menu Jadwal).",
            style = MaterialTheme.typography.labelMedium,
        )

        if (!s.hasPermission) {
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Izin lokasi diperlukan untuk memakai GPS.", style = MaterialTheme.typography.titleSmall)
                    Text("Tanpa izin, aplikasi tetap berfungsi: pilih kota di menu Jadwal lalu gunakan sudut azimut di atas dengan peta.",
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = {
                            permLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ))
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

        if (!s.hasCompass) Text(
            "⚠ Perangkat tanpa magnetometer — gunakan sudut azimut di atas dengan peta. Kompas langsung tidak tersedia.",
            color = MaterialTheme.colorScheme.error)
        if (!s.calibrated) Text(
            "⚠ Sensor belum terkalibrasi / ada gangguan magnet. Gerakkan perangkat pola angka 8, jauhkan dari magnet & casing magnetik.",
            color = MaterialTheme.colorScheme.error)

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
            Text("Jarum emas = kiblat • garis abu = utara • Akurasi kompas tidak dijamin; pisahkan azimut matematis vs sensor.",
                style = MaterialTheme.typography.labelSmall)
        } else {
            Text("Jarum kompas tidak digambar — gunakan angka azimut di atas bersama peta di bawah. " +
                    "Jarum hanya berarti bila perangkat punya magnetometer dan arahnya terkalibrasi.",
                style = MaterialTheme.typography.labelSmall)
        }
        Text("Peta arah kiblat (offline)", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp))
        KiblatMap(s.lat, s.lon, s.bearing, Modifier.padding(top = 4.dp))
        Text("Peta skematik tanpa internet: posisi Anda, Ka'bah, dan rute lingkaran besar di antaranya. " +
            "Jadi acuan arah bila perangkat tak punya magnetometer atau kompas belum terkalibrasi.",
            style = MaterialTheme.typography.labelSmall)
        Text("Verifikasi tanpa alat: Rashdul kiblat — 2x setahun matahari tepat di atas Ka'bah; bayangan benda tegak menunjuk kiblat. Jadwal: sihat.kemenag.dev/arah-kiblat",
            style = MaterialTheme.typography.labelSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(s.useGps, {
                if (!s.hasPermission) {
                    permLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ))
                } else {
                    vm.setUseGps(!s.useGps)
                }
            }, { Text("Pakai GPS") })
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh") })
            FilterChip(s.unit == "derajat", { vm.setUnit("derajat") }, { Text("Derajat") })
            FilterChip(s.unit == "mil", { vm.setUnit("mil") }, { Text("Mil") })
            OutlinedButton(onClick = { vm.refreshLocation() }, enabled = s.hasPermission) { Text("Perbarui lokasi") }
        }
        // Pemilih tampilan hanya ditawarkan bila sensor memang ada: menawarkan "Kompas & peta"
        // pada perangkat tanpa magnetometer berarti menawarkan pilihan yang tak bisa dipakai.
        if (s.hasCompass) Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState()),
        ) {
            Text("Tampilan kompas:", style = MaterialTheme.typography.labelMedium)
            TampilanKompas.SEMUA.forEach { t ->
                FilterChip(s.tampilan == t, { vm.setTampilan(t) }, { Text(TampilanKompas.label(t)) })
            }
        }
    }
}

/** 1 derajat = 17,7778 mil (lingkaran penuh 6400 mil). */
private const val MIL_PER_DEG = 17.7778
