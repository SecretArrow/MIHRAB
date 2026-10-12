package id.or.kemenag.yasintahlil.feature.tentang

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.BuildConfig
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.data.SumberBundel
import id.or.kemenag.yasintahlil.core.legal.Blok
import id.or.kemenag.yasintahlil.core.legal.DokumenLegal
import id.or.kemenag.yasintahlil.core.legal.MarkdownRingkas
import id.or.kemenag.yasintahlil.core.legal.SumberKonten

/**
 * `plan.txt` §"Pengaturan Aplikasi": *"Tentang aplikasi, sumber konten, lisensi, serta kebijakan
 * privasi."* dan §"Keamanan dan Privasi": *"Dokumentasikan izin Android dan alasan penggunaannya."*
 *
 * Sebelumnya Pengaturan hanya memuat satu paragraf ringkas di dalam kode: kebijakan privasi
 * lengkap dan daftar izin ada di `docs/`, yang **tidak dapat dibaca pengguna aplikasi**. Layar ini
 * membuka keduanya beserta lisensi.
 *
 * Dokumennya tidak ditulis ulang di sini. Berkas `docs/PRIVACY.md`, `docs/PERMISSIONS.md`, dan
 * `LICENSE` disalin apa adanya ke `assets/legal/` dan dijaga tetap identik oleh uji, sehingga apa
 * yang dibaca pengguna selalu sama dengan apa yang didokumentasikan di repositori. Sumber konten
 * pun dibaca dari **seluruh nilai bersumber** pada bundel — bukan dari daftar tetap, dan bukan
 * hanya `sumber_url` tingkat berkas, sehingga domain yang disebut `sumber_tambahan` ikut tampil
 * dan layar ini tidak dapat berbohong soal dari mana teksnya berasal. Bila sebuah berkas konten
 * gagal dibaca, sebabnya ditampilkan di atas daftar: daftar yang menyusut tanpa keterangan sama
 * menyesatkannya dengan daftar yang bertambah tanpa keterangan.
 */
@Composable
fun TentangScreen() {
    val ctx = LocalContext.current
    val repo = rememberRepo()
    // Kegagalan membaca bundel tidak ditelan menjadi daftar kosong: sebabnya dibawa ke layar lewat
    // `bundel.gagal`, sebab daftar yang menyusut tanpa keterangan sama menyesatkannya dengan daftar
    // yang bertambah tanpa keterangan.
    val bundel = remember(repo) {
        runCatching { repo.sumberDipakai() }.getOrElse { e ->
            SumberBundel(emptyList(), listOf(e.message ?: e::class.java.simpleName))
        }
    }
    val sumber = bundel.host
    val privasi = remember { DokumenLegal.baca(ctx, DokumenLegal.PRIVASI) }
    val izin = remember { DokumenLegal.baca(ctx, DokumenLegal.IZIN) }
    val lisensi = remember { DokumenLegal.baca(ctx, DokumenLegal.LISENSI) }

    // rememberSaveable: pilihan bagian yang dibuka tidak hilang saat layar diputar ulang.
    var privasiBuka by rememberSaveable { mutableStateOf(true) }
    var izinBuka by rememberSaveable { mutableStateOf(false) }
    var lisensiBuka by rememberSaveable { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        item { Text("Tentang aplikasi", style = MaterialTheme.typography.titleLarge) }

        item {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Yasin dan Tahlil", style = MaterialTheme.typography.titleMedium)
                    Text("Versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelMedium)
                    Text("Aplikasi offline-first: seluruh bacaan, panduan, dan perhitungan jadwal " +
                            "berada di dalam perangkat. Tidak ada iklan, analitik, maupun pelacak.",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item {
            Text("Sumber konten", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp))
            Text("Teks Arab, terjemahan, dan hadis di aplikasi ini diambil dari domain resmi berikut.",
                style = MaterialTheme.typography.labelSmall)
        }
        if (sumber.isEmpty()) {
            item {
                Text("Daftar sumber tidak dapat dibaca di perangkat ini.",
                    style = MaterialTheme.typography.labelMedium)
            }
        }
        // Berkas konten yang gagal dibaca dapat menyembunyikan domainnya dari daftar di bawah. Tanpa
        // keterangan ini, daftar yang menyusut tampak sama dengan daftar yang lengkap.
        if (!bundel.lengkap) {
            item {
                Text("Sebagian berkas konten tidak dapat dibaca, sehingga domain yang dirujuknya " +
                        "belum tentu tercantum di bawah: ${bundel.gagal.joinToString("; ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        items(sumber) { host ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(host, style = MaterialTheme.typography.labelLarge)
                Text(SumberKonten.label(host) ?: "Belum ada keterangan pemilik domain ini.",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            Text("Berkas konten yang menyebut domain lain ditolak saat dibangun maupun saat " +
                    "diperbarui, sehingga daftar di atas tidak dapat bertambah diam-diam.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp))
        }

        item { Bagian("Kebijakan privasi", privasiBuka) { privasiBuka = !privasiBuka } }
        if (privasiBuka) item { Dokumen(privasi) }

        item { Bagian("Izin aplikasi dan alasannya", izinBuka) { izinBuka = !izinBuka } }
        if (izinBuka) item { Dokumen(izin) }

        item { Bagian("Lisensi", lisensiBuka) { lisensiBuka = !lisensiBuka } }
        if (lisensiBuka) item {
            Text(
                lisensi ?: "Dokumen lisensi tidak dapat dibaca di perangkat ini.",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

/** Judul bagian dengan tombol buka/tutup; isinya panjang, jadi tidak ditumpuk semua di layar. */
@Composable
private fun Bagian(judul: String, terbuka: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(judul, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onToggle) { Text(if (terbuka) "Sembunyikan" else "Tampilkan") }
    }
}

/** Isi satu dokumen legal; bila asetnya tak terbaca, katakan apa adanya alih-alih diam. */
@Composable
private fun Dokumen(isi: String?) {
    if (isi.isNullOrBlank()) {
        Text("Dokumen tidak dapat dibaca di perangkat ini.",
            style = MaterialTheme.typography.labelMedium)
        return
    }
    Column(Modifier.fillMaxWidth()) {
        MarkdownRingkas.blok(isi).forEach { RenderBlok(it) }
    }
}

@Composable
private fun RenderBlok(b: Blok) {
    when (b) {
        is Blok.Judul -> Text(b.teks, style = when {
            b.level <= 1 -> MaterialTheme.typography.titleLarge
            b.level == 2 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
        }, modifier = Modifier.padding(top = 8.dp))

        is Blok.Butir -> Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
            Text("• ")
            Text(b.teks, modifier = Modifier.weight(1f))
        }

        is Blok.Paragraf -> Text(b.teks, modifier = Modifier.padding(vertical = 2.dp))

        // Tabel izin ditampilkan per baris: empat kolom tidak terbaca di layar ponsel.
        is Blok.Tabel -> b.baris.forEach { baris ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text(baris.firstOrNull().orEmpty(), style = MaterialTheme.typography.titleSmall)
                    baris.drop(1).forEachIndexed { j, sel ->
                        val label = b.judul.getOrNull(j + 1)
                        Text(
                            if (label.isNullOrBlank()) sel else "$label: $sel",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
