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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.scheduling.NotifChannels
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import kotlinx.coroutines.launch

/**
 * Pengaturan adzan & pengingat: saklar per waktu, berkas audio pilihan pengguna (aplikasi tidak
 * menyertakan audio bawaan karena lisensi), volume, jeda pengingat sebelum adzan, penembus mode
 * senyap, pratinjau, serta pintasan izin sistem (notifikasi, optimasi baterai, akses DND).
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
    var doaReminder by remember { mutableStateOf(false) }
    var dnd by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(22f) }
    var showArab by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    // Metode hisab yang benar-benar dipakai menjadwalkan pengingat. Sebelumnya layar ini menulis
    // nama metode secara hardcode — selalu Kemenag/SIHAT — sehingga berbohong begitu pengguna
    // memilih metode lain di menu Jadwal.
    var metode by remember { mutableStateOf(PrayerCalc.Method.KEMENAG) }
    // Berkas audio pengguna bisa saja tak terbaca (URI dicabut, format tak didukung, izin hilang).
    // Sebelumnya pratinjau diam saja saat gagal; kini sebabnya ditampilkan + tombol coba lagi.
    var putarGagal by remember { mutableStateOf<String?>(null) }
    val player = remember { ExoPlayer.Builder(ctx.applicationContext).build() }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                putarGagal = "Berkas audio tidak dapat diputar (${error.errorCodeName}). " +
                    "Pilih berkas lain atau pastikan aplikasi masih punya izin membaca berkas itu."
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

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
        doaReminder = store.get(Store.DOA_ADZAN_REMINDER, false)
        dnd = NotifChannels.canBypassDnd(ctx.applicationContext)
        arabSize = store.get(Store.ARAB_SIZE, 22f)
        showArab = store.get(Store.SHOW_ARAB, true)
        metode = PrayerCalc.Method.from(store.get(Store.METHOD, "KEMENAG"))
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
                    putarGagal = null
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                    player.volume = vol; player.prepare(); player.play()
                }
            }, enabled = uri.isNotBlank()) { Text("Pratinjau") }
            OutlinedButton(onClick = { player.stop() }) { Text("Stop") }
        }
        putarGagal?.let { pesan ->
            ErrorState(pesan, onRetry = {
                putarGagal = null
                runCatching {
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                    player.volume = vol; player.prepare(); player.play()
                }
            })
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
            Text("Pengingat membaca doa setelah adzan")
            Switch(doaReminder, {
                doaReminder = it
                scope.launch {
                    store.set(Store.DOA_ADZAN_REMINDER, doaReminder)
                    PrayerScheduler.scheduleAll(ctx.applicationContext)
                }
            })
        }
        Text("Bila aktif: pengingat kedua ${PrayerScheduler.DOA_DELAY_MIN} menit setelah waktu masuk, " +
                "berisi ajakan membaca sholawat dan doa setelah adzan. Ketukannya langsung membuka " +
                "layar Doa Adzan. Mengikuti saklar per waktu di atas — waktu yang dimatikan tidak " +
                "ikut mengingatkan. Waktunya pengingat pribadi, bukan ketentuan syariat.",
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
                "Berkas lafaz adzan tidak ditemukan di aplikasi. Pasang ulang aplikasi bila ini terus terjadi.")
        }
        lafaz.forEach { l ->
            BacaanCard("adzan:${l.arab.take(10)}", "Adzan", l.arab, l.latin, l.terjemahan,
                true, true, arabSize, 15f, l.ulangi, showArab = showArab, kategori = l.kategori)
        }
        Text(repo.adzanCatatan(), style = MaterialTheme.typography.labelMedium)
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
