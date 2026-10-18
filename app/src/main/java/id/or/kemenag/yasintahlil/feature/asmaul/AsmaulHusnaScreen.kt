package id.or.kemenag.yasintahlil.feature.asmaul

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Asmaul Husna: sembilan puluh sembilan nama Allah berikut artinya — leksikon yang digulir,
 * bukan bacaan ritual. Karena itu tulisan Arabnya memakai ukuran tetap (bukan preferensi ukuran
 * bacaan di Pengaturan): pengguna menaikkan ukuran bacaan Yasin/Tahlil untuk membaca di waktu
 * panjang, sedangkan di sini ia cukup sekali lihat, dan 99 kartu yang ikut membesar justru
 * memakan ruang daftarnya sendiri. Arab juga tidak tunduk pada sakelar "tampilkan Arab" di
 * Pengaturan — sakelar itu mengatur bacaan; pada daftar nama, justru Arabnya-lah isi utamanya.
 *
 * Daftarnya dibaca dari aset sekali saat layar terbuka (di benang IO); selama itu layar
 * menampilkan keadaan memuat, dan bila berkasnya gagal dibaca, keadaan gagal dengan tombol
 * coba lagi — bukan daftar kosong yang tampak seperti "99 nama hilang tiga".
 *
 * Mencari nama tertentu bukan tugas kotak cari di layar ini (pencarian teks terpusat di layar
 * Pencarian, dan kotak cari per layar sudah lama dibubarkan): cukup lompat ke nomornya — pola
 * "Ke ayat" milik Surat Yasin, satu baris isian angka yang langsung menggulir daftarnya.
 *
 * [go] dipertahankan di tanda tangan karena `AppNav` memanggilnya seragam untuk seluruh layar;
 * layar ini belum punya tujuan lain yang perlu dibuka dari sini (pola yang sama dengan layar
 * Kalkulator Zakat).
 */
@Composable
fun AsmaulHusnaScreen(go: (String) -> Unit) {
    val ctx = LocalContext.current
    // Lompat ke nomor nama: isian angkanya tahan putar layar supaya pengguna tidak mengetik
    // ulang nomornya hanya karena perangkat diputar (penjaga KeadaanLayarSaatDiputarTest).
    var lompat by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var daftar by remember { mutableStateOf<AsmaulData.Daftar?>(null) }
    var gagal by remember { mutableStateOf(false) }
    // Keadaan kosong (belum selesai membaca) dan galat dipisah: pengguna yang menunggu diberi
    // penanda proses, sedangkan galat diberi tombol coba lagi — `ulang` menandai percobaan baru.
    var ulang by remember { mutableStateOf(0) }
    LaunchedEffect(ulang) {
        val hasil = withContext(Dispatchers.IO) { AsmaulData.muat(ctx) }
        if (hasil == null) gagal = true else { gagal = false; daftar = hasil }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(NamaLayar.ASMAUL, style = MaterialTheme.typography.titleLarge)

        val d = daftar
        when {
            gagal -> ErrorState(
                pesan = "Daftar Asmaul Husna tidak dapat dibaca dari penyimpanan aplikasi.",
                onRetry = { ulang++ },
                modifier = Modifier.fillMaxWidth(),
            )
            d == null -> LoadingState(modifier = Modifier.fillMaxWidth())
            else -> {
                if (d.keterangan.isNotBlank()) {
                    Text(
                        d.keterangan,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        lompat,
                        { lompat = it.filter(Char::isDigit).take(2) },
                        label = { Text("Nama ke-") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = {
                        // Lompatan diselesaikan lewat efek: menggulir dari dalam komposisi
                        // langsung tidak boleh — daftar belum tentu terukur saat baris ini
                        // disusun (pola "Ke ayat" Yasin).
                        lompat.toIntOrNull()?.let { no ->
                            // Batas bawah `coerceAtLeast(0)` menjaga daftar yang ternyata
                            // kosong — `coerceIn(0, -1)` melempar, dan satu berkas rusak tidak
                            // boleh meruntuhkan seluruh layarnya.
                            val idx = (no - 1).coerceIn(0, d.nama.lastIndex.coerceAtLeast(0))
                            scope.launch { listState.animateScrollToItem(idx) }
                        }
                    }) {
                        Text("Ke nama")
                    }
                }

                LazyColumn(state = listState) {
                    items(d.nama, key = { it.no }) { n ->
                        // `isi` disebut bernama: parameter terakhir KartuEntri adalah `aksi`,
                        // dan lambda ekor tanpa nama akan mendarat di sana.
                        KartuEntri(judul = n.latin, lencana = listOf("${n.no}"), isi = {
                            ArabText(n.arab, 28f, modifier = Modifier.padding(top = 8.dp))
                            Text(
                                n.arti,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        })
                    }
                }
            }
        }
    }
}
