package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Bacaan
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.BerkasKonten
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.sholat.BacaanSholat
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlinx.coroutines.launch

/**
 * Judul langkah takbir yang khas layar ini — sholat jenazah hadir tidak membaca ta'awudz dan
 * salamnya tanpa keterangan arah. Kuncinya adalah id bacaan dari [BacaanSholat.TAKBIR_MAYIT], jadi
 * setiap id pada urutan bersama itu pasti punya judul; `BacaanSholatTest` menjaga keduanya tetap
 * sepadan sehingga `getValue` tidak pernah gagal.
 */
private val judulLangkah: Map<String, String> = mapOf(
    BacaanSholat.FATIHAH to "Takbir 1 → Al-Fatihah",
    BacaanSholat.SHOLAWAT_IBRAHIMIYAH to "Takbir 2 → Sholawat Ibrahimiyah",
    BacaanSholat.DOA_MAYIT to "Takbir 3 → Doa untuk mayit",
    BacaanSholat.DOA_MAYIT_TAMBAHAN to "Takbir 4 → Doa tambahan",
    BacaanSholat.SALAM to "Salam",
)

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
    val steps: List<Pair<String, Bacaan?>> =
        // Urutan bacaan takbir datang dari `BacaanSholat.TAKBIR_MAYIT`; yang khas layar ini hanya
        // judulnya. Sebelumnya kelima idnya diketik ulang di sini, sehingga satu salah eja cukup
        // untuk menghilangkan satu takbir — kartunya tak muncul, judulnya tetap ada.
        BacaanSholat.TAKBIR_MAYIT.map { judulLangkah.getValue(it) to cari(it) }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(info.judul.ifBlank { NamaLayar.JENAZAH } + " — ${info.takbir} takbir",
            style = MaterialTheme.typography.titleLarge)
        if (info.syarat.isNotEmpty()) {
            Text("Syarat: " + info.syarat.joinToString(", ") + ".",
                style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH) })
            OutlinedButton(onClick = { settings = true }) { Text(NamaAntarmuka.PENGATURAN_BACAAN) }
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
