package id.or.kemenag.yasintahlil.feature.khataman

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.designsystem.BagianJudul
import id.or.kemenag.yasintahlil.core.designsystem.EmptyState
import id.or.kemenag.yasintahlil.core.designsystem.Lencana
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.feature.almarhum.AlmarhumHitung

/**
 * Layar Khataman & Streak: statistik rangkaian hari baca — hari-hari berturut-turut membuka
 * layar Yasin/Tahlil, yang tercatat otomatis sehingga tak ada tombol "catat bacaan" yang bisa
 * terlupa — beserta rekor terpanjangnya, total khataman, dan tombol mencatat khataman Yasin.
 *
 * Satu baris di kepala layar menyebut tradisi mengkhatamkan Yasin berjamaah (misalnya malam
 * Jumat, atau dibagi guna tujuh orang) sebagai konteks budaya, bukan instruksi: mencatat
 * khataman versi sendirian sama sahnya dengan yang berjamaah.
 *
 * [go] dipertahankan demi tanda tangan yang seragam dengan layar lain (`AppNav` memanggil semua
 * layar dengan bentuk yang sama); layar ini belum punya tujuan lanjutan sehingga belum dipakai.
 */
@Composable
fun KhatamanScreen(go: (String) -> Unit) {
    val vm: KhatamanViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(NamaLayar.KHATAMAN, style = MaterialTheme.typography.titleLarge)
        Text(
            "Di banyak tempat, Yasin dikhatamkan berjamaah — misalnya pada malam Jumat, atau " +
                "dibagi guna tujuh orang; di sini tercatat khataman versimu sendiri.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Tiga kartu statistik dari tabel yang sama: rangkaian yang sedang berjalan, rekor
        // terpanjangnya, dan jumlah hari khataman. Sebaris agar ketiganya terbaca sekali lihat.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KartuAngka("${s.rangkaian}", "Rangkaian hari baca", "hari berturut-turut", Modifier.weight(1f))
            KartuAngka("${s.terpanjang}", "Terpanjang", "hari berturut-turut", Modifier.weight(1f))
            KartuAngka("${s.totalKhatam}", "Total khatam Yasin", "khataman tercatat", Modifier.weight(1f))
        }

        // Tombol diganti lencana begitu hari ini tercatat — tidak ada tombol yang terpajang
        // tanpa guna setelah fungsinya selesai (tanpa tombol dekoratif).
        if (s.khatamanHariIni) {
            Lencana("Khataman hari ini sudah tercatat")
        } else {
            FilledTonalButton(onClick = { vm.catatKhatam() }, modifier = Modifier.fillMaxWidth()) {
                Text("Catat khataman Yasin hari ini")
            }
        }

        BagianJudul("Riwayat khataman")
        if (s.riwayat.isEmpty()) {
            EmptyState(
                "Belum ada khataman tercatat",
                "Khataman pertamamu muncul di sini setelah tombol di atas ditekan.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                s.riwayat.forEach { k ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tanggalTampil(k.tanggal), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Khatam Yasin",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Satu kartu statistik: angka besar di tengah, nama statistiknya di bawah, satu baris kecil
 * penjelas satuan. Bentuknya mengikuti kartu entri aplikasi (bingkai tipis, sudut besar) supaya
 * deretannya terbaca sebagai satu keluarga dengan layar lain.
 */
@Composable
private fun KartuAngka(angka: String, judul: String, keterangan: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(angka, style = MaterialTheme.typography.headlineMedium)
            Text(judul, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            Text(
                keterangan,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Tanggal riwayat dalam bentuk Indonesia ("17 Agustus 2025") — formaternya **dirujuk** dari
 * [AlmarhumHitung.tanggalIndo] beserta pembaca [AlmarhumHitung.dariIso], bukan ditulis salinan
 * kedua, supaya kartu almarhum dan riwayat khataman menyebut tanggal dengan cara yang sama.
 * Tanggal yang tak terbaca tampil apa adanya — barisnya tetap tampil, tidak hilang.
 */
private fun tanggalTampil(iso: String): String =
    AlmarhumHitung.dariIso(iso)?.let { AlmarhumHitung.tanggalIndo(it) } ?: iso
