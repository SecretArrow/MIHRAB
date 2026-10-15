package id.or.kemenag.yasintahlil.feature.zakat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.NamaLayar

/**
 * Jenis zakat yang bisa dihitung — labelnya menjadi teks chip pemilih sekaligus lencana hasil.
 */
private enum class JenisZakat(val label: String) {
    FITRAH("Fitrah"),
    MAL("Mal & tabungan"),
    PENGHASILAN("Penghasilan"),
}

/**
 * Kalkulator Zakat — perkiraan hitungan murni lokal, tanpa internet dan tanpa preferensi
 * tersimpan: pilih jenisnya (fitrah, mal & tabungan, penghasilan), isi angkanya, hasilnya
 * mengikuti ketikan.
 *
 * Rumusnya tidak menumpuk di komposisi: seluruhnya hidup di [ZakatHitung] — murni Kotlin tanpa
 * Android, teruji langsung di JVM. Hasil sengaja tidak menunggu tombol "Hitung": hitungannya
 * sekali lihat, dan jawaban yang tertunda di balik tombol hanya menambah satu langkah. Untuk
 * kepastian hukumnya (haul, utang, harga emas yang mana), pengguna diarahkan pada amil atau
 * ulama setempat — kalimat pengantar di kepala layar memegang peran itu.
 */
@Composable
fun ZakatScreen(go: (String) -> Unit) {
    // rememberSaveable: jenis yang dipilih beserta seluruh isian angkanya tidak boleh hilang
    // saat perangkat diputar — mengetik ulang harga emas hanya karena memutar layar adalah
    // kehilangan yang tak dapat dipulihkan sendiri.
    var jenis by rememberSaveable { mutableStateOf(JenisZakat.FITRAH) }
    var inputJiwa by rememberSaveable { mutableStateOf("") }
    var inputBeras by rememberSaveable { mutableStateOf("") }
    var inputHarta by rememberSaveable { mutableStateOf("") }
    var inputEmas by rememberSaveable { mutableStateOf("") }
    var inputGaji by rememberSaveable { mutableStateOf("") }

    // Angka dari isian, dihitung ulang tiap ketikan. Pemisah ribuan ('.' maupun ',') dibuang
    // saat membaca, bukan ditafsir desimal: isian layar ini semuanya rupiah utuh atau jiwa.
    val jiwa = remember(inputJiwa) { inputJiwa.filter(Char::isDigit).toIntOrNull() ?: 0 }
    val hargaBeras = remember(inputBeras) { angka(inputBeras) }
    val harta = remember(inputHarta) { angka(inputHarta) }
    val hargaEmas = remember(inputEmas) { angka(inputEmas) }
    val gaji = remember(inputGaji) { angka(inputGaji) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(NamaLayar.ZAKAT, style = MaterialTheme.typography.titleLarge)
        Text(
            "Perkiraan patokan umum — untuk ketentuan yang pasti, konsultasikan dengan amil atau ulama setempat.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BagianJudul("Jenis zakat")
        BarisChip {
            JenisZakat.entries.forEach { j ->
                FilterChip(jenis == j, { jenis = j }, { Text(j.label) })
            }
        }

        when (jenis) {
            JenisZakat.FITRAH -> {
                KolomAngka("Jumlah jiwa", inputJiwa, { inputJiwa = it },
                    "Semua jiwa yang ditanggung, termasuk diri sendiri")
                KolomAngka("Harga beras per kg (Rp)", inputBeras, { inputBeras = it },
                    "Harga beras yang biasa dimakan — misalnya 13.000")
                KartuPerkiraan(jenis.label) {
                    Text(ZakatHitung.rupiah(ZakatHitung.fitrah(jiwa, hargaBeras)),
                        style = MaterialTheme.typography.headlineMedium)
                    Text("Total beras: ${ZakatHitung.kg(ZakatHitung.fitrahKg(jiwa))}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Setara 2,5 kg beras per jiwa — boleh dibayar beras atau senilainya.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            JenisZakat.MAL -> {
                KolomAngka("Harta terzakati (Rp)", inputHarta, { inputHarta = it },
                    "Tabungan, emas, dan niaga setelah kebutuhan pokok")
                KolomAngka("Harga emas per gram (Rp)", inputEmas, { inputEmas = it },
                    "Penentu batas nisab — misalnya 1.350.000")
                KartuPerkiraan(jenis.label) {
                    // Nisab hanya berarti bila harga emasnya diisi: tanpa harga, "Rp 0" akan
                    // menyamar sebagai batas dan statusnya ikut berbohong. Yang ditampilkan
                    // adalah arahan mengisinya, bukan jawaban yang belum bisa dihitung.
                    if (hargaEmas > 0.0) {
                        val nisab = ZakatHitung.nisabEmas(hargaEmas)
                        val wajib = harta >= nisab
                        Text("Batas nisab (85 gram emas): ${ZakatHitung.rupiah(nisab)}",
                            style = MaterialTheme.typography.bodyMedium)
                        Lencana(if (wajib) "Sudah mencapai nisab" else "Belum mencapai nisab")
                        Text(
                            if (wajib) ZakatHitung.rupiah(ZakatHitung.mal(harta))
                            else "Belum wajib zakat",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    } else {
                        Text(
                            "Isi harga emas per gram untuk memeriksa batas nisab.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            JenisZakat.PENGHASILAN -> {
                KolomAngka("Penghasilan bulanan bersih (Rp)", inputGaji, { inputGaji = it },
                    "Setelah kebutuhan pokok — misalnya 5.000.000")
                KartuPerkiraan(jenis.label) {
                    Text(ZakatHitung.rupiah(ZakatHitung.penghasilan(gaji)),
                        style = MaterialTheme.typography.headlineMedium)
                    Text("Zakat penghasilan per bulan",
                        style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Setara 2,5% setelah kebutuhan pokok; banyak ulama menunaikannya setahun " +
                            "sekali bila telah mencapai nisab.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Kotak isian angka layar ini: papan ketik angka, satu baris, baris bantuan tetap di bawahnya.
 * Isian hanya meloloskan digit dan pemisah ribuan ('.' atau ',') — tumpukan huruf dari tempel
 * papan klip ditolak seketika, dan pemisahnya tetap tampil apa adanya karena pembacaannya ada
 * di [angka], bukan di sini.
 */
@Composable
private fun KolomAngka(
    label: String,
    nilai: String,
    ubah: (String) -> Unit,
    bantuan: String,
) {
    OutlinedTextField(
        nilai,
        { ubah(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) },
        label = { Text(label) },
        singleLine = true,
        supportingText = { Text(bantuan) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Kartu hasil hitungan: kepala kartunya selalu sama ("Perkiraan zakat" dengan lencana jenis yang
 * sedang dihitung), sehingga tiap cabang jenis cukup menata isinya tanpa mengulang kepala.
 */
@Composable
private fun KartuPerkiraan(jenis: String, isi: @Composable ColumnScope.() -> Unit) {
    KartuEntri(judul = "Perkiraan zakat", lencana = listOf(jenis), isi = isi)
}

/**
 * Angka dari teks isian: hanya digit yang dibaca, pemisah ribuan dibuang. Teks kosong atau tanpa
 * digit sama sekali menjadi 0 — pengguna yang baru membuka layar jangan dihadapkan galat.
 */
private fun angka(teks: String): Double = teks.filter(Char::isDigit).toDoubleOrNull() ?: 0.0
