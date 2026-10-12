package id.or.kemenag.yasintahlil.feature.alat

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
fun AlatScreen(go: (String) -> Unit) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Menu ${NamaLayar.ALAT}", style = MaterialTheme.typography.titleLarge)
        // Label menu adalah penunjuk arah, bukan kutipan sumber. Dahulu kedua tombol ini menulis
        // "(SIHAT)" — padahal jadwal sholat dihitung **di perangkat** (algoritma Meeus, sudut
        // Kemenag) dan aplikasi tidak pernah menghubungi SIHAT; layar Pengaturan bahkan menyatakan
        // sumber online resmi belum diaktifkan. Klaim sumber yang tak dapat ditepati lebih buruk
        // daripada tanpa klaim: asal-usulnya kini disebut di layar masing-masing, dibaca dari bundel.
        Button(onClick = { go(Routes.KIBLAT) }) { Text(NamaLayar.KIBLAT) }
        Button(onClick = { go(Routes.JADWAL) }) { Text(NamaLayar.JADWAL) }
        Button(onClick = { go(Routes.ADZAN) }) { Text(NamaLayar.ADZAN) }
        Button(onClick = { go(Routes.IQOMAH) }) { Text(NamaLayar.IQOMAH) }
        Button(onClick = { go(Routes.SEARCH) }) { Text(NamaLayar.SEARCH) }
    }
}
