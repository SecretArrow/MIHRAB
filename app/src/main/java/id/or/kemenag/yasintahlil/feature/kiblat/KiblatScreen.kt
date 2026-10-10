package id.or.kemenag.yasintahlil.feature.kiblat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.util.QiblaCalc
import kotlinx.coroutines.launch
import kotlin.math.*

/** Kompas kiblat: azimut matematis (SIHAT) + jarum sensor. Akurasi sensor tidak dijamin. */
@Composable
fun KiblatScreen() {
    val ctx = LocalContext.current
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    var lat by remember { mutableStateOf(-6.2) }
    var lon by remember { mutableStateOf(106.8) }
    var azimuth by remember { mutableStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var calibrated by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (store.get(Store.USE_GPS, false)) {
            try {
                val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                if (loc != null) { lat = loc.latitude; lon = loc.longitude }
            } catch (_: SecurityException) { }
        } else {
            lat = store.get(Store.LAT, -6.2); lon = store.get(Store.LON, 106.8)
        }
    }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (acc == null || mag == null) { hasSensor = false; calibrated = false }
        val grav = FloatArray(3); val geo = FloatArray(3)
        var hasG = false; var hasM = false
        val li = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) { e.values.copyInto(grav); hasG = true }
                else { e.values.copyInto(geo); hasM = true }
                if (hasG && hasM) {
                    val r = FloatArray(9); val o = FloatArray(3)
                    if (SensorManager.getRotationMatrix(r, null, grav, geo)) {
                        SensorManager.getOrientation(r, o)
                        azimuth = Math.toDegrees(o[0].toDouble()).toFloat()
                    }
                    val magStrength = sqrt((geo[0] * geo[0] + geo[1] * geo[1] + geo[2] * geo[2]).toDouble())
                    calibrated = magStrength in 20.0..80.0
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {
                if (a == android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE) calibrated = false
            }
        }
        if (acc != null && mag != null) {
            sm.registerListener(li, acc, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(li, mag, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm.unregisterListener(li) }
    }
    val kiblat = QiblaCalc.bearing(lat, lon)
    val arah = listOf("U", "TL", "T", "TG", "S", "BD", "B", "BL")[(Math.round(kiblat / 45.0) % 8).toInt()]
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Arah Kiblat (SIHAT Kemenag)", style = MaterialTheme.typography.titleLarge)
        Text("Azimut: %.1f° %s dari (%.3f, %.3f)".format(kiblat, arah, lat, lon))
        if (!hasSensor) Text("⚠ Perangkat tanpa magnetometer — gunakan sudut di atas dengan peta. Kompas langsung tidak tersedia.",
            color = MaterialTheme.colorScheme.error)
        if (!calibrated) Text("⚠ Sensor belum terkalibrasi / ada gangguan magnet. Gerakkan perangkat pola angka 8, jauhkan dari magnet & casing magnetik.",
            color = MaterialTheme.colorScheme.error)
        Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(240.dp)) {
                val c = center; val R = size.minDimension / 2
                drawCircle(Color(0xFF0B7A55), R, c)
                drawCircle(Color.White, R - 4, c)
                val q = Math.toRadians(kiblat); val a = Math.toRadians(azimuth.toDouble())
                // utara perangkat
                drawLine(Color.Gray, c, c + Offset(-sin(a).toFloat() * R, (-cos(a)).toFloat() * R), 4f)
                // arah kiblat relatif layar = kiblat - azimuth
                val rel = q - a
                drawLine(Color(0xFFC9A227), c,
                    c + Offset(sin(rel).toFloat() * (R - 12), (-cos(rel)).toFloat() * (R - 12)), 10f)
                drawCircle(Color.Red, c, 8f)
            }
        }
        Text("Jarum emas = kiblat • garis abu = utara • Akurasi kompas tidak dijamin; pisahkan azimut matematis vs sensor.",
            style = MaterialTheme.typography.labelSmall)
        Text("Verifikasi tanpa alat: Rashdul kiblat — 2x setahun matahari tepat di atas Ka'bah; bayangan benda tegak menunjuk kiblat. Jadwal: sihat.kemenag.dev/arah-kiblat",
            style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { store.set(Store.USE_GPS, true) } }) { Text("Pakai GPS") }
            OutlinedButton(onClick = { scope.launch { store.set(Store.USE_GPS, false) } }) { Text("Lokasi manual") }
        }
        Text("Butuh izin lokasi. Tanpa izin, gunakan lokasi manual di Pengaturan.",
            style = MaterialTheme.typography.labelSmall)
    }
}
