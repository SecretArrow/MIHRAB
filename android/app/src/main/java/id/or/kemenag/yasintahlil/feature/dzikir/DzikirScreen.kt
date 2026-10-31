package id.or.kemenag.yasintahlil.feature.dzikir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import id.or.kemenag.yasintahlil.core.designsystem.MenuBaris
import id.or.kemenag.yasintahlil.core.designsystem.TombolInfo
import id.or.kemenag.yasintahlil.core.designsystem.EmptyState
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PesanKonten
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Menu Dzikir & Doa — deretan kelompoknya, bentuknya sama dengan menu Ibadah.
 *
 * Dahulu layar ini membuka isinya lewat deretan tab kelompok. Permintaan pengguna (2026-10-15):
 * *"setelah klik dzikir dan doa tampilannya adalah sama seperti menu Ibadah bukan tabs"* — maka
 * tiap kelompok kini menjadi baris menu ([MenuBaris]) yang membuka bacaannya di layar tersendiri
 * yang mengalir bersambung ([DzikirGroupScreen]).
 *
 * Mencari satu doa bukan lagi tugas layar ini (kotak penyaring per-tab-nya sudah dibubarkan):
 * layar Pencarian menelusuri seluruh entri dzikir lintas kelompok sekaligus.
 */
@Composable
fun DzikirScreen(go: (String) -> Unit = {}, vm: DzikirViewModel = hiltViewModel()) {
    val groups = vm.groups

    if (groups.isEmpty()) {
        EmptyState("Konten dzikir tidak tersedia",
            PesanKonten.berkasTidakAda("kumpulan dzikir"))
        return
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(NamaLayar.DZIKIR, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("dzikir")) }
        }
        // Ikon kelompok berputar dari daftar kecil yang terverifikasi ada di pustaka ikon.
        // Kelompoknya datang dari bundel konten, jadi ikonnya tidak dapat dipetakan per kelompok
        // tanpa menulis daftar tetap yang akan basi begitu bundel menambah kelompok baru.
        val ikon = listOf(
            Icons.Filled.AutoAwesome, Icons.Filled.Favorite, Icons.Filled.Campaign,
            Icons.Filled.Schedule, Icons.Filled.SelfImprovement, Icons.Filled.VolunteerActivism,
            Icons.Filled.Star, Icons.Filled.Groups,
        )
        groups.forEachIndexed { i, g ->
            MenuBaris(
                g.judul,
                ikon[i % ikon.size],
                onClick = { go(Routes.dzikirGrup(g.id)) },
                keterangan = "${g.items.size} bacaan",
            )
        }
    }
}
