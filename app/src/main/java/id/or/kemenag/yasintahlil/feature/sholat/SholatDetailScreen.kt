package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import kotlinx.coroutines.launch

/** Mode belajar langkah-demi-langkah untuk setiap sholat. */
@Composable
fun SholatDetailScreen(id: String) {
    val repo = rememberRepo()
    val store = rememberStore()
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    val bacaan = remember { repo.sholatBacaan() }
    val info = remember { (repo.sholatWajib() + repo.sholatSunnah()).find { it.id == id } }
    var step by remember { mutableStateOf(0) }
    var ringkas by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(22f) }
    var showArab by remember { mutableStateOf(true) }
    var selesai by remember { mutableStateOf(false) }
    var mazhab by remember { mutableStateOf("syafii") }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, 22f)
        showArab = store.get(Store.SHOW_ARAB, true)
        mazhab = store.get(Store.MADHAB, "syafii")
    }
    // Progres belajar tersimpan di Room — tetap ada setelah keluar layar.
    LaunchedEffect(id) { userData.progress(KIND, id).collect { selesai = (it?.value ?: 0) > 0 } }
    // Saring bacaan menurut mazhab panduan (mis. qunut hanya mazhab Syafi'i)
    // dan cakupan sholat (mis. qunut hanya untuk Subuh).
    val steps = remember(ringkas, mazhab, id) {
        val base = if (ringkas) bacaan.filter { it.id in listOf("niat", "takbir", "fatihah", "rukuk", "sujud", "tasyahud", "salam") }
        else bacaan
        base.filter {
            (it.mazhab == null || it.mazhab == mazhab) &&
                (it.sholatScope.isEmpty() || id in it.sholatScope)
        }
    }
    val mazhabLabel = when (mazhab) {
        "hanafi" -> "Hanafi"; "maliki" -> "Maliki"; "hambali" -> "Hambali"; else -> "Syafi'i"
    }
    // Jaga indeks langkah tetap sah bila daftar menyusut setelah ganti mazhab.
    LaunchedEffect(steps.size) { if (step > steps.size - 1) step = (steps.size - 1).coerceAtLeast(0) }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(info?.nama ?: id, style = MaterialTheme.typography.titleLarge)
        Text("${info?.rakaat.orEmpty()} • ${info?.waktu.orEmpty()}")
        info?.niat?.let { n ->
            BacaanCard("niat:$id", "Niat", n.arab, n.latin, n.terjemahan, true, true, arabSize, 15f,
                showArab = showArab)
        }
        if (info?.niat == null) {
            info?.niatCatatan?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (info?.deskripsi != null) Text(info.deskripsi)
        info?.ketentuan?.takeIf { it.isNotEmpty() }?.let { ket ->
            Text("Ketentuan:", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp))
            ket.forEach { k ->
                Text("• $k", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!ringkas, { ringkas = false }, { Text("Pemula") })
            FilterChip(ringkas, { ringkas = true }, { Text("Ringkas") })
            FilterChip(showArab, {
                showArab = !showArab
                scope.launch { store.set(Store.SHOW_ARAB, showArab) }
            }, { Text("Arab") })
        }
        Text("Mazhab panduan: $mazhabLabel", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            MAZHAB_OPTIONS.forEach { (key, label) ->
                FilterChip(mazhab == key, {
                    mazhab = key
                    scope.launch { store.set(Store.MADHAB, key) }
                }, { Text(label) })
            }
        }
        Text("Langkah ${step + 1} dari ${steps.size}")
        LinearProgressIndicator(progress = { ((step + 1).toFloat() / steps.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        val b = steps[step.coerceIn(0, steps.size - 1)]
        val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
            Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
        }
        Text(b.judul, style = MaterialTheme.typography.titleMedium)
        if (b.kapan.isNotBlank()) Text("Kapan: ${b.kapan}", style = MaterialTheme.typography.labelMedium)
        // Ilustrasi skematik posisi tubuh + deskripsi (plan: tiap gerakan punya visual yang sopan).
        Postures.of(b.id)?.let { p ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    painter = painterResource(p.gambar),
                    contentDescription = "Ilustrasi posisi: ${p.label}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text(p.label, style = MaterialTheme.typography.titleSmall)
                    Text(p.deskripsi, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        BacaanCard("langkah:$id:${b.id}", b.judul, a, l, t, true, true, arabSize, 15f, sumber = b.sumber,
            showArab = showArab)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (step > 0) step-- }, enabled = step > 0) { Text("← Sebelumnya") }
            Button(onClick = { step = 0 }) { Text("Ulangi") }
            Button(onClick = { if (step < steps.size - 1) step++ }, enabled = step < steps.size - 1) { Text("Berikutnya →") }
        }
        if (step == steps.size - 1 && !selesai) Text("Semua langkah sudah dibaca — tandai selesai bila sudah dipraktikkan.",
            color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { scope.launch { userData.setProgress(KIND, id, 1) } }, enabled = !selesai) {
                Text(if (selesai) "✓ Sudah ditandai selesai" else "Tandai selesai")
            }
            if (selesai) OutlinedButton(onClick = { scope.launch { userData.setProgress(KIND, id, 0) } }) {
                Text("Batalkan tanda")
            }
        }
        Text("Catatan mazhab: Indonesia umumnya Syafi'i (qunut Subuh sunnah). Bacaan bertanda mazhab " +
            "tertentu hanya tampil bila mazhab panduan cocok — perbedaan dicatat, tidak dicampuradukkan. Lihat SOURCES.md.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    }
}

private const val KIND = "sholat"

/** Pilihan mazhab panduan yang mengubah bacaan yang ditampilkan. */
private val MAZHAB_OPTIONS = listOf(
    "syafii" to "Syafi'i",
    "hanafi" to "Hanafi",
    "maliki" to "Maliki",
    "hambali" to "Hambali",
)
