package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlinx.coroutines.launch

/**
 * Sholat jenazah hadir: 4 takbir tanpa ruku/sujud, fardhu kifayah. Konten (niat & doa)
 * dibaca dari assets (sholat_jenazah.json + bacaan bersama) — tanpa teks Arab di Kotlin.
 */
@Composable
fun JenazahScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    var fullscreen by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(22f) }
    var showArab by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, 22f)
        showArab = store.get(Store.SHOW_ARAB, true)
    }
    FullscreenEffect(fullscreen)
    val info = remember { repo.sholatKhusus("sholat_jenazah.json") }
    val bacaan = remember { repo.sholatBacaan() + repo.sholatMayitBacaan() }
    fun cari(id: String) = bacaan.find { it.id == id }
    val steps = listOf(
        "Takbir 1 → Al-Fatihah" to cari("fatihah"),
        "Takbir 2 → Sholawat Ibrahimiyah" to cari("sholawat_ibrahimiyah"),
        "Takbir 3 → Doa untuk mayit" to cari("doa_mayit"),
        "Takbir 4 → Doa tambahan" to cari("doa_mayit_tambahan"),
        "Salam" to cari("salam"),
    )
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(info.judul.ifBlank { "Sholat Jenazah" } + " — ${info.takbir} takbir",
            style = MaterialTheme.typography.titleLarge)
        if (info.syarat.isNotEmpty()) {
            Text("Syarat: " + info.syarat.joinToString(", ") + ".",
                style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh") })
            FilterChip(showArab, {
                showArab = !showArab
                scope.launch { store.set(Store.SHOW_ARAB, showArab) }
            }, { Text("Arab") })
            OutlinedButton(onClick = {
                arabSize = (arabSize + 2).coerceAtMost(44f)
                scope.launch { store.set(Store.ARAB_SIZE, arabSize) }
            }) { Text("A+") }
            OutlinedButton(onClick = {
                arabSize = (arabSize - 2).coerceAtLeast(16f)
                scope.launch { store.set(Store.ARAB_SIZE, arabSize) }
            }) { Text("A−") }
        }
        info.niat.forEach { n ->
            BacaanCard("jenazah:niat:${n.id}", "Niat (${n.judul})", n.arab, n.latin, n.terjemahan,
                true, true, arabSize, 15f, sumber = n.sumber, showArab = showArab)
            if (n.catatan.isNotBlank()) Text(n.catatan, style = MaterialTheme.typography.labelSmall)
        }
        steps.forEachIndexed { i, (judul, b) ->
            Text("${i + 1}. $judul", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
            if (b != null) {
                val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
                    Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
                }
                BacaanCard("jenazah:${b.id}", b.judul, a, l, t, true, true,
                    if (fullscreen) arabSize + 8 else arabSize, 15f, sumber = b.sumber,
                    showArab = showArab)
            }
        }
        if (info.catatanMazhab.isNotBlank()) {
            Text("Catatan mazhab: ${info.catatanMazhab}",
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
