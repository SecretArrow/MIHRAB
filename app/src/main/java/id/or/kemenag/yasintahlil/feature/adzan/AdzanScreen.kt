package id.or.kemenag.yasintahlil.feature.adzan

import android.net.Uri
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
import kotlinx.coroutines.launch

/** Pengaturan adzan: jadwal per waktu, suara (file pilihan pengguna — tanpa audio bawaan karena lisensi), volume, pratinjau. */
@Composable
fun AdzanScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val lafaz = remember { repo.adzan() }
    var on by remember { mutableStateOf(listOf(true, true, true, true, true)) }
    var uri by remember { mutableStateOf("") }
    var vol by remember { mutableStateOf(0.8f) }
    var arabSize by remember { mutableStateOf(22f) }
    val player = remember {
        ExoPlayer.Builder(ctx.applicationContext).build()
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        u?.let { uri = it.toString(); scope.launch { store.set(Store.ADZAN_URI, uri) } }
    }
    LaunchedEffect(Unit) {
        on = store.get(Store.ADZAN_ON, "1,1,1,1,1").split(",").map { it == "1" } + List(5) { true }.take(0)
        if (on.size != 5) on = List(5) { true }
        uri = store.get(Store.ADZAN_URI, "")
        vol = store.get(Store.ADZAN_VOL, 0.8f)
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
                    scope.launch { store.set(Store.ADZAN_ON, on.map { if (it) "1" else "0" }.joinToString(",")) }
                })
            }
        }
        Text("Suara adzan (file audio pilihan Anda — aplikasi tidak menyertakan audio karena lisensi):",
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(if (uri.isBlank()) "Belum ada file dipilih" else uri, style = MaterialTheme.typography.labelSmall)
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
        Text("Metode jadwal: Kemenag/SIHAT. Koreksi & zona waktu di Pengaturan. Izin: notifikasi + baterai (nonaktifkan optimasi baterai agar pengingat tepat waktu). Batasan Android modern dapat menunda pemutaran latar — bukan kegagalan aplikasi.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp))
        Text("Lafaz adzan:", style = MaterialTheme.typography.titleMedium)
        lafaz.forEach { l ->
            BacaanCard("adzan:${l.arab.take(10)}", "Adzan", l.arab, l.latin, l.terjemahan,
                true, true, arabSize, 15f, l.ulangi)
        }
        Text(repo.adzanCatatan(), style = MaterialTheme.typography.labelMedium)
    }
}
