package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Langkah
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import kotlinx.coroutines.launch

/**
 * Panduan bersuci: wudhu, tayamum, dan pembatal wudhu. Langkah disajikan sebagai
 * prosedur (kerangka Fasolatan/Bimas Islam) tanpa teks Arab — lafaz niat/doa menunggu
 * verifikasi sumber resmi, sesuai aturan proyek.
 */
@Composable
fun WudhuScreen() {
    val repo = rememberRepo()
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    val panduan = remember { repo.panduanBersuci() }
    var selesai by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { userData.progress(KIND, "wudhu").collect { selesai = (it?.value ?: 0) > 0 } }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Wudhu & Tayamum", style = MaterialTheme.typography.titleLarge)
        Text("Kerangka tata cara mengikuti program Fasolatan Ditjen Bimas Islam Kemenag; urutan rukun mazhab Syafi'i (umum di Indonesia).",
            style = MaterialTheme.typography.labelMedium)

        if (panduan.rukun.isNotEmpty()) {
            Text("Rukun wudhu", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            panduan.rukun.forEachIndexed { i, r -> Text("${i + 1}. $r", style = MaterialTheme.typography.bodyMedium) }
        }

        LangkahBagian("Tata cara wudhu", panduan.wudhu)
        LangkahBagian("Tata cara tayamum", panduan.tayamum)

        if (panduan.pembatal.isNotEmpty()) {
            Text("Yang membatalkan wudhu", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            panduan.pembatal.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }

        if (panduan.catatanMazhab.isNotBlank()) {
            Text(panduan.catatanMazhab, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp))
        }
        if (panduan.catatanLafaz.isNotBlank()) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(panduan.catatanLafaz, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(12.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { scope.launch { userData.setProgress(KIND, "wudhu", 1) } }, enabled = !selesai) {
                Text(if (selesai) "✓ Sudah ditandai dipelajari" else "Tandai sudah dipelajari")
            }
            if (selesai) OutlinedButton(onClick = { scope.launch { userData.setProgress(KIND, "wudhu", 0) } }) {
                Text("Batalkan tanda")
            }
        }
    }
}

@Composable
private fun LangkahBagian(title: String, steps: List<Langkah>) {
    if (steps.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    steps.forEach { l ->
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("${l.no}. ${l.judul}", style = MaterialTheme.typography.titleSmall)
                if (l.keterangan.isNotBlank()) Text(l.keterangan, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// Jenis progres panduan bersuci — dari kosakata bersama `JenisBacaan`. Hanya menyimpan progres:
// langkah wudhu tinggal di satu layar bersama tayamum, jadi tidak punya layar tujuan sendiri.
private const val KIND = JenisBacaan.WUDHU
