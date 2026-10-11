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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.scheduling.NotifChannels
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import kotlinx.coroutines.launch

/**
 * Pengaturan adzan & pengingat: saklar per waktu, berkas audio pilihan pengguna (aplikasi tidak
 * menyertakan audio bawaan karena lisensi), volume, jeda pengingat sebelum adzan, penembus mode
 * senyap, pratinjau, serta pintasan izin sistem (notifikasi, baterai, alarm tepat waktu).
 */
@Composable
fun AdzanScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val lafaz = remember { repo.adzan() }
    var on by remember { mutableStateOf(List(5) { true }) }
    var uri by remember { mutableStateOf("") }
    var vol by remember { mutableStateOf(0.8f) }
    var preMin by remember { mutableStateOf(0) }
    var dnd by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(22f) }
    val player = remember { ExoPlayer.Builder(ctx.applicationContext).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        u?.let {
            // Simpan izin baca agar berkas tetap dapat diputar pekerja latar setelah app ditutup.
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            uri = it.toString()
            scope.launch {
                store.set(Store.ADZAN_URI, uri)
                PrayerScheduler.scheduleAll(ctx.applicationContext)
            }
        }
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        val parsed = store.get(Store.ADZAN_ON, "1,1,1,1,1").split(",").map { it.trim() == "1" }
        on = (parsed + List(5) { true }).take(5)
        uri = store.get(Store.ADZAN_URI, "")
        vol = store.get(Store.ADZAN_VOL, 0.8f)
        preMin = store.get(Store.ADZAN_PRE_MIN, 0)
        dnd = NotifChannels.canBypassDnd(ctx.applicationContext)
        arabSize = store.get(Store.ARAB_SIZE, 22f)
    }

    val names = listOf("Subuh", "Zuhur", "Asar", "Magrib", "Isya")
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Adzan & Pengingat", style = MaterialTheme.typography.titleLarge)
        Text("Aktif/nonaktif per waktu:", style = MaterialTheme.typography.titleMedium)
        names.forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(n)
                Switch(on[i], {
                    on = on.toMutableList().also { it[i] = !it[i] }
                    scope.launch {
                        store.set(Store.ADZAN_ON, on.map { if (it) "1" else "0" }.joinToString(","))
                        PrayerScheduler.scheduleAll(ctx.applicationContext)
                    }
                })
            }
        }

        Text("Suara adzan (berkas audio pilihan Anda — aplikasi tidak menyertakan audio karena lisensi):",
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(if (uri.isBlank()) "Belum ada berkas dipilih" else uri, style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pick.launch(arrayOf("audio/*")) }) { Text("Pilih audio") }
            Button(onClick = {
                if (uri.isNotBlank()) {
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                    player.volume = vol; player.prepare(); player.play()
                }
            }, enabled = uri.isNotBlank()) { Text("Pratinjau") }
            OutlinedButton(onClick = { player.stop() }) { Text("Stop") }
        }
        Text("Volume: ${(vol * 100).toInt()}%")
        Slider(vol, { vol = it; player.volume = it; scope.launch { store.set(Store.ADZAN_VOL, vol) } })

        Text("Pengingat sebelum adzan: ${if (preMin == 0) "nonaktif" else "$preMin menit"}")
        Slider(
            value = preMin.toFloat(),
            onValueChange = { preMin = it.toInt() },
            valueRange = 0f..30f,
            steps = 29,
            onValueChangeFinished = { scope.launch { store.set(Store.ADZAN_PRE_MIN, preMin); PrayerScheduler.scheduleAll(ctx.applicationContext) } },
        )

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
        if (Build.VERSION.SDK_INT >= 31) {
            OutlinedButton(onClick = { openExactAlarmSettings(ctx) }) { Text("Alarm tepat waktu") }
        }
        Text("Metode jadwal: Kemenag/SIHAT. Koreksi & zona waktu di Pengaturan. Batasan Android modern dapat menunda pengingat — nonaktifkan optimasi baterai agar lebih tepat.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp))

        Text("Lafaz adzan:", style = MaterialTheme.typography.titleMedium)
        lafaz.forEach { l ->
            BacaanCard("adzan:${l.arab.take(10)}", "Adzan", l.arab, l.latin, l.terjemahan,
                true, true, arabSize, 15f, l.ulangi)
        }
        Text(repo.adzanCatatan(), style = MaterialTheme.typography.labelMedium)
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

private fun openExactAlarmSettings(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${ctx.packageName}")
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
