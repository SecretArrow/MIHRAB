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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun vibrate(ctx: Context) {
    val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    runCatching { v.vibrate(VibrationEffect.createOneShot(900, VibrationEffect.DEFAULT_AMPLITUDE)) }
}

@Composable
fun IqomahScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val items = remember { repo.iqomah() }
    var mins by remember { mutableStateOf(10) }
    var remaining by rememberSaveable { mutableStateOf(-1) } // detik; -1 = tidak berjalan
    var fullscreen by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(24f) }
    var showArab by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        mins = store.get(Store.IQOMAH_MIN, 10)
        arabSize = store.get(Store.ARAB_SIZE, 24f)
        showArab = store.get(Store.SHOW_ARAB, true)
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
    val mm = shown / 60
    val ss = shown % 60

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Iqomah", style = MaterialTheme.typography.titleLarge)
        Text("Timer opsional setelah masuk waktu (± $mins mnt). Ini pengingat pribadi — waktu iqomah tiap masjid berbeda.",
            style = MaterialTheme.typography.labelMedium)

        Text("%02d:%02d".format(mm, ss), style = MaterialTheme.typography.displayMedium)
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else (1f - shown.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { remaining = total }, enabled = !running) { Text("Mulai") }
            OutlinedButton(onClick = { remaining = -1 }) { Text(if (running) "Batal" else "Reset") }
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh") })
        }
        Text("Timer berjalan selama layar ini terbuka; getar menandai waktu habis.",
            style = MaterialTheme.typography.labelSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { mins = (mins - 1).coerceAtLeast(1); remaining = -1; scope.launch { store.set(Store.IQOMAH_MIN, mins) } }) { Text("−") }
            Text("$mins mnt", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { mins = (mins + 1).coerceAtMost(60); remaining = -1; scope.launch { store.set(Store.IQOMAH_MIN, mins) } }) { Text("+") }
        }

        Text("Ukuran Arab: ${arabSize.toInt()}", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                arabSize = (arabSize + 2).coerceAtMost(40f); scope.launch { store.set(Store.ARAB_SIZE, arabSize) }
            }) { Text("A+") }
            OutlinedButton(onClick = {
                arabSize = (arabSize - 2).coerceAtLeast(16f); scope.launch { store.set(Store.ARAB_SIZE, arabSize) }
            }) { Text("A−") }
        }
        if (repo.iqomahCatatan().isNotBlank()) {
            Text(repo.iqomahCatatan(), style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp))
        }

        items.forEach { l ->
            BacaanCard("iqomah:${l.arab.take(10)}", "Iqomah", l.arab, l.latin, l.terjemahan,
                true, true, if (fullscreen) arabSize + 8 else arabSize, 15f, l.ulangi,
                showArab = showArab)
        }
    }
}

@Composable
fun DoaAdzanScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    val (arab, latin, trans) = remember { repo.doaAdzan() }
    val (jawabArab, jawabLatin, jawabTrans) = remember { repo.menjawabAdzan() }
    var arabSize by remember { mutableStateOf(22f) }
    var showArab by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, 22f)
        showArab = store.get(Store.SHOW_ARAB, true)
    }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Doa sebelum & sesudah adzan", style = MaterialTheme.typography.titleLarge)
        Text("Tidak ada satu doa khusus yang wajib tepat sebelum adzan. Yang disyariatkan: menjawab adzan, sholawat, lalu doa setelah adzan.",
            style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(showArab, {
                showArab = !showArab
                scope.launch { store.set(Store.SHOW_ARAB, showArab) }
            }, { Text("Arab") })
            OutlinedButton(onClick = {
                arabSize = (arabSize + 2).coerceAtMost(40f)
                scope.launch { store.set(Store.ARAB_SIZE, arabSize) }
            }) { Text("A+") }
            OutlinedButton(onClick = {
                arabSize = (arabSize - 2).coerceAtLeast(16f)
                scope.launch { store.set(Store.ARAB_SIZE, arabSize) }
            }) { Text("A−") }
        }
        Text("Menjawab muadzin", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        Text(repo.menjawabAdzanCatatan(), style = MaterialTheme.typography.labelMedium)
        BacaanCard("jawab:adzan", "Jawaban saat seruan", jawabArab, jawabLatin, jawabTrans,
            true, true, arabSize, 15f, sumber = repo.adzanSumber("menjawab_adzan"),
            showArab = showArab)
        BacaanCard("doa:adzan", "Doa setelah adzan", arab, latin, trans,
            true, true, arabSize, 15f, sumber = repo.adzanSumber("doa_setelah_adzan"),
            showArab = showArab)
        Text("Adzan dan pengingat sebelum adzan diatur di menu Adzan; iqomah memiliki timer tersendiri di menu Iqomah.",
            style = MaterialTheme.typography.labelSmall)
    }
}
