package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.BerkasKonten
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlinx.coroutines.launch

/**
 * Sholat jenazah hadir: 4 takbir tanpa ruku/sujud, fardhu kifayah. Konten (niat & doa)
 * dibaca dari assets ([BerkasKonten.SHOLAT_JENAZAH] + bacaan bersama) — tanpa teks Arab di Kotlin.
 */
@Composable
fun JenazahScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    // rememberSaveable: mode layar penuh pilihan pengguna tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
    }
    FullscreenEffect(fullscreen)
    val info = remember { repo.sholatKhusus(BerkasKonten.SHOLAT_JENAZAH) }
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
            OutlinedButton(onClick = { settings = true }) { Text("Pengaturan bacaan") }
        }
        info.niat.forEach { n ->
            // Varian niat diberi rujukan `niat:<id varian>` supaya tidak bertabrakan dengan id
            // langkah takbir di bawah (`jenazah:<id bacaan>`).
            BacaanCard(JenisBacaan.id(JenisBacaan.JENAZAH, JenisBacaan.id(JenisBacaan.NIAT, n.id)),
                "Niat (${n.judul})", n.arab, n.latin, n.terjemahan,
                true, true, arabSize, sumber = n.sumber, showArab = showArab, kategori = n.kategori)
            if (n.catatan.isNotBlank()) Text(n.catatan, style = MaterialTheme.typography.labelSmall)
        }
        steps.forEachIndexed { i, (judul, b) ->
            Text("${i + 1}. $judul", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
            if (b != null) {
                val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
                    Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
                }
                BacaanCard(JenisBacaan.id(JenisBacaan.JENAZAH, b.id), b.judul, a, l, t, true, true,
                    UkuranBacaan.arabBacaJauh(arabSize, fullscreen), sumber = b.sumber,
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
