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
 * Sholat Ghaib: 4 takbir tanpa ruku/sujud, fardhu kifayah. Seluruh konten (niat & doa)
 * dibaca dari assets (sholat_ghaib.json + bacaan bersama) — tidak ada teks Arab di Kotlin.
 */
@Composable
fun GhaibScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    var fullscreen by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(24f) }
    var showArab by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, 24f)
        showArab = store.get(Store.SHOW_ARAB, true)
    }
    FullscreenEffect(fullscreen)
    val info = remember { repo.sholatKhusus("sholat_ghaib.json") }
    val bacaan = remember { repo.sholatBacaan() + repo.sholatMayitBacaan() }
    fun cari(id: String) = bacaan.find { it.id == id }
    val steps = listOf(
        "Niat dalam hati (varian di bawah) — fardhu kifayah" to null,
        "Takbir 1 → ta'awudz + Al-Fatihah (HR Bukhari 756, Muslim 394)" to cari("fatihah"),
        "Takbir 2 → Sholawat Ibrahimiyah" to cari("sholawat_ibrahimiyah"),
        "Takbir 3 → Doa untuk mayit" to cari("doa_mayit"),
        "Takbir 4 → Doa tambahan" to cari("doa_mayit_tambahan"),
        "Salam kanan-kiri" to cari("salam"),
    )
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(info.judul.ifBlank { "Sholat Ghaib" } + " — ${info.takbir} takbir, tanpa ruku/sujud",
            style = MaterialTheme.typography.titleLarge)
        Text("Dalil: ${info.dalil} Rujukan tata cara: imbauan Kemenag; syarat & kritik sanad: NU Online. Lihat SOURCES.md.",
            style = MaterialTheme.typography.labelMedium)
        if (info.syarat.isNotEmpty()) {
            Text("Syarat: " + info.syarat.joinToString("; "), style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh (imam)") })
            OutlinedButton(onClick = { settings = true }) { Text("Pengaturan bacaan") }
        }
        if (info.niat.isNotEmpty()) {
            Text("Varian niat:", style = MaterialTheme.typography.titleMedium)
            info.niat.forEach { n ->
                BacaanCard("ghaib:niat:${n.id}", n.judul, n.arab, n.latin, n.terjemahan,
                    true, false, arabSize, sumber = n.sumber, showArab = showArab, kategori = n.kategori)
            }
        }
        steps.forEachIndexed { i, (judul, b) ->
            Text("${i + 1}. $judul", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
            if (b != null) {
                val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
                    Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
                }
                BacaanCard("ghaib:${b.id}", b.judul, a, l, t, true, true,
                    if (fullscreen) arabSize + 8 else arabSize, sumber = b.sumber,
                    showArab = showArab, kategori = b.kategori)
            }
        }
        if (info.catatanMazhab.isNotBlank()) {
            Text("Catatan mazhab: ${info.catatanMazhab}",
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        }
    }

    if (settings) {
        BacaanSettingsSheet(
            onDismiss = { settings = false },
            showArab = showArab, onShowArab = { showArab = it; scope.launch { store.set(Store.SHOW_ARAB, it) } },
            arabSize = arabSize, onArabSize = { arabSize = it; scope.launch { store.set(Store.ARAB_SIZE, it) } },
        )
    }
}
