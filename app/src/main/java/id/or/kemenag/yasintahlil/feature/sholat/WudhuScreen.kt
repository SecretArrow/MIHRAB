package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Langkah
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.KunciProgres
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.launch

/**
 * Panduan bersuci: wudhu, tayamum, dan pembatal wudhu. Langkah disajikan sebagai
 * prosedur (kerangka Fasolatan/Bimas Islam) tanpa teks Arab — lafaz niat/doa menunggu
 * verifikasi sumber resmi, sesuai aturan proyek.
 *
 * Catatan mazhab dan catatan lafaz tinggal di layar keterangan (dibuka lewat ikon Info),
 * sehingga badan layar hanya memuat prosedurnya.
 */
@Composable
fun WudhuScreen(go: (String) -> Unit = {}) {
    val repo = rememberRepo()
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    val panduan = remember { repo.panduanBersuci() }
    var selesai by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { userData.progress(KIND, KunciProgres.WUDHU).collect { selesai = (it?.value ?: 0) > 0 } }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(NamaLayar.WUDHU, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("wudhu")) }
        }
        // Provenans dibaca dari bundel (`wudhu.json` → `edisi`) dan tetap tampil sekilas sebagai
        // kartu keterangan; catatan mazhab dan catatan lafaz yang menyertainya dibuka lewat ikon
        // Info. Bila berkasnya tidak mencantumkannya, kartu ini tidak digambar sama sekali.
        if (panduan.edisi.isNotBlank()) {
            KartuKeterangan(
                onInfo = { go(Routes.keterangan("wudhu")) },
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(panduan.edisi, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (panduan.rukun.isNotEmpty()) {
            BagianJudul("Rukun wudhu")
            panduan.rukun.forEachIndexed { i, r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LangkahNomor(i + 1)
                    Text(r, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                }
            }
        }

        // Judul kedua bagian dibaca dari bundel (`wudhu.json` → `wudhu.judul` / `tayamum.judul`).
        // Salinan lama di sini berbunyi "Tata cara tayamum" saja, sedangkan berkasnya menambahkan
        // "(pengganti wudhu saat tidak ada air / sakit)" — keterangan yang justru menjawab kapan
        // bagian itu dipakai. Cadangan teks hanya jaring pengaman bila berkasnya kehilangan judulnya.
        LangkahBagian(panduan.judulWudhu.ifBlank { "Tata cara wudhu" }, panduan.wudhu)
        LangkahBagian(panduan.judulTayamum.ifBlank { "Tata cara tayamum" }, panduan.tayamum)

        if (panduan.pembatal.isNotEmpty()) {
            BagianJudul("Yang membatalkan wudhu")
            panduan.pembatal.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
            Button(onClick = { scope.launch { userData.setProgress(KIND, KunciProgres.WUDHU, 1) } }, enabled = !selesai) {
                Text(if (selesai) "✓ Sudah ditandai dipelajari" else "Tandai sudah dipelajari")
            }
            if (selesai) OutlinedButton(onClick = { scope.launch { userData.setProgress(KIND, KunciProgres.WUDHU, 0) } }) {
                Text(NamaAntarmuka.BATALKAN_TANDA)
            }
        }
    }
}

@Composable
private fun LangkahBagian(title: String, steps: List<Langkah>) {
    if (steps.isEmpty()) return
    BagianJudul(title)
    steps.forEach { l ->
        KartuEntri(
            judul = l.judul,
            lencana = listOf("Langkah ${l.no}"),
        ) {
            if (l.keterangan.isNotBlank()) {
                Text(l.keterangan, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Jenis progres panduan bersuci — dari kosakata bersama `JenisBacaan`, dan rujukan barisnya dari
// `KunciProgres`. Hanya menyimpan progres: langkah wudhu tinggal di satu layar bersama tayamum, jadi
// tidak punya layar tujuan sendiri.
private const val KIND = JenisBacaan.WUDHU
