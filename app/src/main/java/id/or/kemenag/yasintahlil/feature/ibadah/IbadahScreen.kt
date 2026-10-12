package id.or.kemenag.yasintahlil.feature.ibadah

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.Routes

@Composable
fun IbadahScreen(go: (String) -> Unit) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Menu ${NamaLayar.IBADAH}", style = MaterialTheme.typography.titleLarge)
        // Judul menu bukan tempat mengarang asal-usul teks. Dahulu tombol ini menulis
        // "(83 ayat, MSI Kemenag)": jumlah ayat dan nama sumbernya dipatok di kode, sehingga tidak
        // dapat ikut berubah bila bundelnya diperbarui — dan layar Yasin sendiri sudah menolak
        // hardcode serupa (`AtribusiTeksQuranTest`). Atribusi teks Arab, transliterasi, dan
        // terjemahan kini tampil di layar Yasin, dibaca dari bundelnya.
        Button(onClick = { go(Routes.YASIN) }) { Text(NamaLayar.YASIN) }
        Button(onClick = { go(Routes.TAHLIL) }) { Text(NamaLayar.TAHLIL) }
        Button(onClick = { go(Routes.SHOLAT) }) { Text(NamaLayar.SHOLAT) }
        Button(onClick = { go(Routes.WUDHU) }) { Text(NamaLayar.WUDHU) }
        Button(onClick = { go(Routes.GHAIB) }) { Text(NamaLayar.GHAIB) }
        Button(onClick = { go(Routes.JENAZAH) }) { Text(NamaLayar.JENAZAH) }
        Button(onClick = { go(Routes.DZIKIR) }) { Text(NamaLayar.DZIKIR) }
        Button(onClick = { go(Routes.DOA_ADZAN) }) { Text(NamaLayar.DOA_ADZAN) }
        Button(onClick = { go(Routes.SEARCH) }) { Text(NamaLayar.SEARCH) }
        Button(onClick = { go(Routes.FAVORIT) }) { Text(NamaLayar.FAVORIT) }
    }
}
