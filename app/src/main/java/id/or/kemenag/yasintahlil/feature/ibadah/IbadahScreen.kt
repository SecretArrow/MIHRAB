package id.or.kemenag.yasintahlil.feature.ibadah

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun IbadahScreen(go: (String) -> Unit) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Menu Ibadah")
        // Judul menu bukan tempat mengarang asal-usul teks. Dahulu tombol ini menulis
        // "(83 ayat, MSI Kemenag)": jumlah ayat dan nama sumbernya dipatok di kode, sehingga tidak
        // dapat ikut berubah bila bundelnya diperbarui — dan layar Yasin sendiri sudah menolak
        // hardcode serupa (`AtribusiTeksQuranTest`). Atribusi teks Arab, transliterasi, dan
        // terjemahan kini tampil di layar Yasin, dibaca dari bundelnya.
        Button(onClick = { go(Routes.YASIN) }) { Text("Yasin") }
        Button(onClick = { go(Routes.TAHLIL) }) { Text("Tahlil + counter") }
        Button(onClick = { go(Routes.SHOLAT) }) { Text("Sholat Wajib & Sunnah") }
        Button(onClick = { go(Routes.WUDHU) }) { Text("Wudhu & Tayamum") }
        Button(onClick = { go(Routes.GHAIB) }) { Text("Sholat Ghaib") }
        Button(onClick = { go(Routes.JENAZAH) }) { Text("Sholat Jenazah") }
        Button(onClick = { go(Routes.DZIKIR) }) { Text("Dzikir & Doa") }
        Button(onClick = { go(Routes.DOA_ADZAN) }) { Text("Doa Adzan") }
        Button(onClick = { go(Routes.SEARCH) }) { Text("Pencarian") }
        Button(onClick = { go(Routes.FAVORIT) }) { Text("Favorit & Riwayat") }
    }
}
