package id.or.kemenag.yasintahlil.feature.settings

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
import id.or.kemenag.yasintahlil.core.data.BerkasKonten
import id.or.kemenag.yasintahlil.core.data.BundelKonten
import id.or.kemenag.yasintahlil.core.data.ContentUpdateManager
import id.or.kemenag.yasintahlil.core.data.MetaPembaruan
import id.or.kemenag.yasintahlil.core.data.SumberKategori
import id.or.kemenag.yasintahlil.core.designsystem.ErrorState
import id.or.kemenag.yasintahlil.core.designsystem.LoadingState
import id.or.kemenag.yasintahlil.core.util.ContentValidator
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pembaruan konten dengan validasi sumber — `plan.txt`: *"Sediakan mekanisme pembaruan konten
 * dengan validasi sumber."*
 *
 * Bundel **tidak diunduh sendiri**: pengguna memilih berkasnya dari penyimpanan (tanpa izin
 * apa pun, tanpa jaringan). Setiap berkas di dalamnya diperiksa sebelum dipasang — sumber resmi,
 * kategori, harakat, dan bebas placeholder — dan bundel ditolak utuh bila ada satu yang gagal.
 * Konten bawaan tetap tersimpan dan bisa dipakai lagi kapan saja lewat tombol kembalikan.
 */
@Composable
fun ContentUpdateScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val mgr = remember { ContentUpdateManager(ctx.applicationContext) }
    var status by remember { mutableStateOf(MetaPembaruan.Status()) }
    var hasil by remember { mutableStateOf<ContentUpdateManager.Hasil?>(null) }
    var memproses by remember { mutableStateOf(false) }
    var konfirmasiKembali by remember { mutableStateOf(false) }
    // Keadaan selalu dibaca ulang dari disk, bukan disimpan di memori — jadi tetap benar
    // setelah rotasi maupun setelah aplikasi dibuka kembali.
    LaunchedEffect(Unit) { status = withContext(Dispatchers.IO) { mgr.status() } }

    val pemilih = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        memproses = true
        hasil = null
        scope.launch {
            hasil = withContext(Dispatchers.IO) { mgr.pasangDariUri(uri) }
            status = withContext(Dispatchers.IO) { mgr.status() }
            memproses = false
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(NamaLayar.PEMBARUAN_KONTEN, style = MaterialTheme.typography.titleLarge)
        Text("Aplikasi tidak pernah mengunduh konten sendiri dan tidak meminta izin jaringan. " +
                "Pembaruan hanya masuk bila Anda memilih sendiri berkas bundel (.json) yang " +
                "diterbitkan sumber resmi — jadi tidak ada teks keagamaan baru yang datang tanpa " +
                "tindakan sadar Anda.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))

        if (memproses) LoadingState("Memeriksa dan memasang bundel…")

        when (val h = hasil) {
            is ContentUpdateManager.Hasil.Terpasang -> {
                Text("Bundel diterima: ${h.berkas.size} berkas dipasang.",
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp))
                h.berkas.forEach { b -> Text("• ${b.nama} — ${b.sumberUrl}", style = MaterialTheme.typography.labelSmall) }
            }
            is ContentUpdateManager.Hasil.Ditolak -> {
                Text("Bundel ditolak — tidak ada berkas yang dipasang.",
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                ErrorState(
                    pesan = "Perbaiki bundelnya lalu pilih ulang:\n" +
                        h.alasan.joinToString("\n") { "• $it" },
                    onRetry = { pemilih.launch(arrayOf(MIME_JSON)) },
                )
            }
            null -> Unit
        }

        Text("Keadaan sekarang", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp))
        if (status.aktif) {
            Text("Pembaruan aktif — dipasang ${status.dipasang}" +
                    status.penerbit.takeIf { it.isNotBlank() }?.let { " oleh $it" }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium)
            if (status.catatan.isNotBlank()) Text(status.catatan, style = MaterialTheme.typography.labelSmall)
            status.berkas.forEach { b ->
                Text("• ${b.nama} — ${b.sumberUrl}" +
                        b.kategori.joinToString(prefix = " [", postfix = "]") { SumberKategori.label(it) },
                    style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = { konfirmasiKembali = true },
                modifier = Modifier.padding(top = 8.dp)) { Text("Kembalikan ke konten bawaan") }
        } else {
            Text("Memakai konten bawaan aplikasi (assets) — tidak ada pembaruan terpasang.",
                style = MaterialTheme.typography.bodyMedium)
        }

        Button(onClick = { pemilih.launch(arrayOf(MIME_JSON)) }, enabled = !memproses,
            modifier = Modifier.padding(top = 12.dp)) { Text("Pilih berkas bundel") }

        Text("Yang diperiksa sebelum dipasang", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp))
        Text("• setiap berkas menyatakan `sumber_url` dari domain resmi " +
                "(${ContentValidator.allowedHosts.joinToString()}) — pencocokan host " +
                "persis/subdomain, jadi domain yang menyerupai pun ditolak;\n" +
                "• berkas ber-Arab menyatakan `kategori` sumbernya " +
                "(${SumberKategori.SEMUA.joinToString { SumberKategori.label(it) }});\n" +
                "• teks Arab wajib berharakat dan bebas prosa penunjuk;\n" +
                "• nama berkas tidak boleh menunjuk ke luar folder konten;\n" +
                "• rujukan berkas (`rujukan_file`, larik `rujukan`) menunjuk berkas yang benar-benar " +
                "ada — di dalam bundel ini atau di antara berkas bawaan aplikasi.\n" +
                "Bila ada satu berkas yang gagal, seluruh bundel ditolak — konten yang sudah " +
                "diverifikasi tidak pernah tercampur setengah pembaruan.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))

        Text("Berkas bundel memuat metadata dan peta nama-berkas → dokumen konten " +
                "(`{\"${BundelKonten.KUNCI_META}\": {\"skema\": ${BundelKonten.SKEMA}, \"penerbit\": \"…\"}, " +
                "\"${BundelKonten.KUNCI_BERKAS}\": {\"${BerkasKonten.YASIN_FULL}\": { … }}}`). " +
                "Simpan salinan bundelnya di tempat aman; konten bawaan tetap ada di dalam aplikasi.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    }

    if (konfirmasiKembali) {
        AlertDialog(
            onDismissRequest = { konfirmasiKembali = false },
            title = { Text("Kembalikan ke konten bawaan?") },
            text = { Text("Seluruh berkas pembaruan dihapus dan aplikasi kembali memakai konten " +
                "bawaan yang sudah diverifikasi. Bundel aslinya tidak ikut terhapus.") },
            confirmButton = {
                TextButton(onClick = {
                    konfirmasiKembali = false
                    scope.launch {
                        withContext(Dispatchers.IO) { mgr.kembalikan() }
                        hasil = null
                        status = withContext(Dispatchers.IO) { mgr.status() }
                    }
                }) { Text("Kembalikan") }
            },
            dismissButton = { TextButton(onClick = { konfirmasiKembali = false }) { Text("Batal") } },
        )
    }
}

/** MIME yang diminta dari pemilih berkas sistem: hanya berkas JSON yang ditampilkan. */
private const val MIME_JSON = "application/json"
