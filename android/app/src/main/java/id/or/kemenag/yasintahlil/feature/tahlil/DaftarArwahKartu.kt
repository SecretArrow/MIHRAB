package id.or.kemenag.yasintahlil.feature.tahlil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Kartu Daftar Arwah — menempel di bawah kartu Bagian 4 Buku Imam ("Hadiah Al-Fatihah untuk
 * Arwah yang Dituju"). Buku menulis titik-titik ("hadhrati ruhi…, wa hadhrati ruhi…") sebagai
 * tempat imam menyebut nama arwah yang dituju; kartu ini menyediakan daftarnya.
 *
 * Prinsipnya (permintaan pengguna, 2026-10-20): nama-nama adalah **data pengguna sendiri** —
 * bukan teks dari bundel dan bukan karangan aplikasi — disimpan lokal di perangkat
 * (`Store.ARWAH`), dan lafaz bacaan tidak pernah diubah untuk mengisinya. Pengelolaan
 * langsung di kartu: tulis nama, ketuk tambah; tiap baris punya tombol hapus.
 *
 * Sengaja berkas sendiri (bukan badan `TahlilScreen`): isian teks milik fitur ini terpisah
 * dari layar bacaan — layar Tahlil tetap tanpa satu pun isian, sesuai sanaknya yang dihapus
 * 2026-10-17 (kolom "Bagian").
 */
@Composable
internal fun KartuDaftarArwah(
    nama: List<String>,
    onTambah: (String) -> Unit,
    onHapus: (Int) -> Unit,
) {
    var isian by rememberSaveable { mutableStateOf("") }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Daftar arwah yang dituju", style = MaterialTheme.typography.titleSmall)
            Text(
                "Titik-titik pada bagian ini adalah tempat menyebut nama arwah yang dituju — " +
                    "adatnya berbeda antar daerah. Nama tersimpan hanya di perangkat Anda dan " +
                    "lafaz bacaan tetap persis buku.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (nama.isEmpty()) {
                Text("Belum ada nama — tulis di bawah lalu ketuk Tambah.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 6.dp))
            }
            nama.forEachIndexed { i, n ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}. $n", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { onHapus(i) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus nama $n")
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = isian,
                    onValueChange = { isian = it },
                    label = { Text("Nama arwah") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onTambah(isian); isian = "" },
                    enabled = isian.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Tambah")
                }
            }
        }
    }
}

/**
 * Panel pengingat Daftar Arwah di kartu doa 20 ("Hadiah Pahala Bacaan") dan 21 ("Ampunan
 * untuk Ahli Kubur"). Doa Bagian IV bukunya tak menuliskan slot nama — lafaznya umum
 * ("lahum" untuk ahlil qubur) — tetapi frasa *"man ijtama'na hahuna bisababihi"* dan adat
 * banyak majelis membuka tempat menyebut nama di titik ini. Lafaz tetap utuh; panel ini
 * hanya mengingatkan nama siapa yang dituju, dibaca sesuai adat majelis masing-masing.
 */
@Composable
internal fun PanelPengingatArwah(nama: List<String>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text("Daftar arwah: ${DaftarArwah.ringkas(nama)}",
                style = MaterialTheme.typography.labelMedium)
            Text(
                "Lafaz doa tetap sesuai buku — sebutkan nama-nama ini pada waktunya, " +
                    "sesuai adat majelis Anda.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
