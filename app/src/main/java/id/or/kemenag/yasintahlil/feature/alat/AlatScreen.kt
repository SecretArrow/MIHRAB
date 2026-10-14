package id.or.kemenag.yasintahlil.feature.alat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.designsystem.BagianJudul
import id.or.kemenag.yasintahlil.core.designsystem.MenuBaris
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Menu Alat: perangkat waktu sholat dan penunju ibadah, dikelompokkan sebagai "waktu & arah"
 * dan "temuan". Bentuk barisnya sama dengan menu Ibadah ([MenuBaris]) — dua daftar panjang yang
 * serupa bentuknya lebih mudah dipindai daripada tumpukan tombol terisi.
 */
@Composable
fun AlatScreen(go: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Menu ${NamaLayar.ALAT}", style = MaterialTheme.typography.titleLarge)

        // Label menu adalah penunjuk arah, bukan kutipan sumber. Dahulu kedua tombol ini menulis
        // "(SIHAT)" — padahal jadwal sholat dihitung **di perangkat** (algoritma Meeus, sudut
        // Kemenag) dan aplikasi tidak pernah menghubungi SIHAT; layar Pengaturan bahkan menyatakan
        // sumber online resmi belum diaktifkan. Klaim sumber yang tak dapat ditepati lebih buruk
        // daripada tanpa klaim: asal-usulnya kini disebut di layar masing-masing, dibaca dari bundel.
        BagianJudul("Waktu & arah")
        listOf(
            Triple(Routes.JADWAL, NamaLayar.JADWAL, Icons.Filled.Schedule),
            Triple(Routes.ADZAN, NamaLayar.ADZAN, Icons.Filled.NotificationsActive),
            Triple(Routes.IQOMAH, NamaLayar.IQOMAH, Icons.Filled.Timer),
            Triple(Routes.KIBLAT, NamaLayar.KIBLAT, Icons.Filled.Explore),
        ).forEach { (rute, label, ikon) -> MenuBaris(label, ikon, { go(rute) }) }

        BagianJudul("Temuan")
        listOf(
            Triple(Routes.SEARCH, NamaLayar.SEARCH, Icons.Filled.Search),
        ).forEach { (rute, label, ikon) -> MenuBaris(label, ikon, { go(rute) }) }
    }
}
