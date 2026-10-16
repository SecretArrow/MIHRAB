package id.or.kemenag.yasintahlil.feature.alat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
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
 * Menu Alat: perangkat penunjang ibadah dalam dua kelompok — "Waktu & arah" (jadwal, pengingat
 * adzan, iqomah, kompas kiblat, kalender) dan "Hitung & ingatan" (tasbih digital, kalkulator
 * zakat, tahlil almarhum — pindahan dari menu Ibadah, permintaan pengguna 2026-10-16: tab Alat
 * adalah rumah bagi penghitung dan hitungan, sementara menu Ibadah murni bacaan dan panduan).
 * Bentuk barisnya sama dengan menu Ibadah ([MenuBaris]) supaya kedua daftar itu terbaca sebagai
 * satu keluarga bentuk.
 *
 * Bagian "Temuan" yang dahulu hanya memuat satu baris Pencarian sudah dibubarkan: pintu itu
 * sudah ada di menu Ibadah (bagian Simpanan) dan di ikon Pencarian pada kepala beranda, dan
 * bagian bersetifikat satu isi hanya memakan tempat.
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
            Triple(Routes.KALENDER, NamaLayar.KALENDER, Icons.Filled.DateRange),
        ).forEach { (rute, label, ikon) -> MenuBaris(label, ikon, { go(rute) }) }

        // Pindahan dari menu Ibadah: penghitung ibadah, hitungan zakat, dan peringatan tahlil
        // untuk almarhum — alat bantu, bukan bacaan, sebab itu rumahnya di tab ini.
        BagianJudul("Hitung & ingatan")
        listOf(
            Triple(Routes.TASBIH, NamaLayar.TASBIH, Icons.Filled.TouchApp),
            Triple(Routes.ZAKAT, NamaLayar.ZAKAT, Icons.Filled.Calculate),
            Triple(Routes.ALMARHUM, NamaLayar.ALMARHUM, Icons.Filled.Event),
        ).forEach { (rute, label, ikon) -> MenuBaris(label, ikon, { go(rute) }) }
    }
}
