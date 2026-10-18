package id.or.kemenag.yasintahlil.feature.almarhum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.or.kemenag.yasintahlil.core.data.local.AlmarhumEntity
import id.or.kemenag.yasintahlil.core.designsystem.BagianJudul
import id.or.kemenag.yasintahlil.core.designsystem.EmptyState
import id.or.kemenag.yasintahlil.core.designsystem.KartuEntri
import id.or.kemenag.yasintahlil.core.designsystem.NamaAntarmuka
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import java.time.LocalDate

/**
 * Layar Tahlil Almarhum: catatan almarhum (nama, tanggal wafat, catatan) beserta jadwal
 * peringatannya — hari ke-7, 40, 100, 1000, dan haul menurut tradisi tahlil masyarakat
 * Indonesia — plus pengingat harian lewat notifikasi.
 *
 * Hitungan tanggal tidak menumpuk di komposisi: seluruhnya hidup di [AlmarhumHitung] — murni
 * Kotlin tanpa Android, teruji langsung di JVM. Daftarnya reaktif dari Room: menambah lewat
 * dialog maupun menghapus dari kartu langsung menyegarkan daftar tanpa pengkabelan tambahan.
 *
 * [go] dipertahankan di tanda tangan karena `AppNav` memanggilnya seragam untuk seluruh layar;
 * layar ini belum punya tujuan lain yang perlu dibuka dari sini (pola yang sama dengan layar
 * Kalkulator Zakat).
 */
@Composable
fun AlmarhumScreen(go: (String) -> Unit) {
    val vm: AlmarhumViewModel = hiltViewModel()
    val daftar by vm.daftar.collectAsStateWithLifecycle()

    // rememberSaveable: dialog tambah beserta seluruh isiannya tidak boleh hilang saat perangkat
    // diputar — mengetik ulang nama dan tanggal wafat hanya karena memutar layar adalah kehilangan
    // yang tak dapat dipulihkan sendiri.
    var dialogTerbuka by rememberSaveable { mutableStateOf(false) }
    var namaBaru by rememberSaveable { mutableStateOf("") }
    var tanggalBaru by rememberSaveable { mutableStateOf("") }
    var catatanBaru by rememberSaveable { mutableStateOf("") }
    // Konfirmasi hapus sengaja tidak disimpan (pola `hapusTarget` di Pengaturan): menanyakan
    // ulang setelah perangkat diputar jauh lebih murah daripada menjaga aksi menghapus tetap
    // terpasang melewati perubahan konfigurasi.
    var hapusTarget by remember { mutableStateOf<AlmarhumEntity?>(null) }
    // "Hari ini" dibekukan sekali per komposisi: layar yang terbuka terus lewat tengah malam
    // memang masih menampilkan daftar hari lamanya, dan membuka ulang layarnya menyegarkannya
    // (pola `TasbihState.hariIni`).
    val hariIni = remember { LocalDate.now() }

    // Validasi tanggal dihitung dari isiannya sendiri sehingga pesan galat mengikuti tiap
    // ketikan — pola "hasil mengikuti ketikan" Kalkulator Zakat. Keputusan menyimpan tetap pada
    // ViewModel, satu-satunya pintu masuk ke Room.
    val tanggal = AlmarhumHitung.dariIso(tanggalBaru)
    val tanggalGalat = when {
        tanggalBaru.isBlank() -> null
        tanggal == null -> "Bentuknya tahun-bulan-tanggal, contohnya 2000-12-31"
        tanggal.isAfter(hariIni) -> "Tanggal wafat tidak boleh di masa depan"
        else -> null
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(NamaLayar.ALMARHUM, style = MaterialTheme.typography.titleLarge)
        Text(
            AlmarhumHitung.KETERANGAN_TRADISI,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = { dialogTerbuka = true }) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tambah catatan")
        }

        BagianJudul("Daftar almarhum")
        if (daftar.isEmpty()) {
            // Tanpa tombol: satu-satunya yang bisa dilakukan pada keadaan ini — mencatat — sudah
            // ada tombolnya di atas, dan mengulangnya dari sini hanya menduplikasi pintu masuk.
            EmptyState(
                judul = "Belum ada catatan",
                pesan = "Catat nama dan tanggal wafat; hari ke-7, 40, 100, 1000, dan haul " +
                    "dihitung beserta pengingatnya.",
            )
        } else {
            daftar.forEach { baris ->
                KartuAlmarhum(baris, hariIni, onHapus = { hapusTarget = baris })
            }
        }
    }

    if (dialogTerbuka) {
        AlertDialog(
            onDismissRequest = { dialogTerbuka = false },
            title = { Text("Tambah catatan almarhum") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        namaBaru,
                        { namaBaru = it },
                        label = { Text("Nama") },
                        singleLine = true,
                        supportingText = { Text("Nama lengkap atau sapaan yang dikenal keluarga") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        tanggalBaru,
                        { tanggalBaru = it.filter { c -> c.isDigit() || c == '-' }.take(AlmarhumHitung.ISO_PANJANG) },
                        label = { Text("Tanggal wafat") },
                        singleLine = true,
                        isError = tanggalGalat != null,
                        supportingText = {
                            Text(
                                tanggalGalat
                                    ?: if (tanggal != null) {
                                        "Dibaca: ${AlmarhumHitung.tanggalIndo(tanggal)}"
                                    } else {
                                        "Contoh: 2000-12-31"
                                    },
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        catatanBaru,
                        { catatanBaru = it },
                        label = { Text("Catatan (opsional)") },
                        supportingText = { Text("Mis. hubungan keluarga atau tempat dimakamkan") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    // Tombolnya mati selama isian belum sah, dan ketukan yang tak lolos tetap
                    // diabaikan di dalamnya — dialog tetap terbuka supaya salah ketik diperbaiki
                    // di tempat, bukan dimulai dari awal (pola dialog "Lainnya" Tasbih).
                    onClick = {
                        if (vm.tambah(namaBaru, tanggalBaru, catatanBaru)) {
                            namaBaru = ""
                            tanggalBaru = ""
                            catatanBaru = ""
                            dialogTerbuka = false
                        }
                    },
                    enabled = namaBaru.isNotBlank() && tanggal != null && tanggalGalat == null,
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { dialogTerbuka = false }) { Text(NamaAntarmuka.BATAL) }
            },
        )
    }

    hapusTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { hapusTarget = null },
            title = { Text("Hapus catatan ${target.nama}?") },
            text = { Text("Tanggal wafat beserta seluruh jadwal peringatannya ikut terhapus.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.hapus(target.id)
                    hapusTarget = null
                }) { Text(NamaAntarmuka.HAPUS) }
            },
            dismissButton = {
                TextButton(onClick = { hapusTarget = null }) { Text(NamaAntarmuka.BATAL) }
            },
        )
    }
}

/**
 * Satu kartu almarhum: nama, tanggal wafat, dan catatannya; peringatan terdekat ditonjolkan di
 * bawahnya dengan sisa jadwal yang ringkas; tombol hapus di ujung kartu menanyakan konfirmasi
 * lebih dulu — menghapus catatan duka bukan ketukan yang boleh salah.
 */
@Composable
private fun KartuAlmarhum(
    baris: AlmarhumEntity,
    hariIni: LocalDate,
    onHapus: () -> Unit,
) {
    // Tanggal wafat dibaca lewat jalur yang sama dengan penyimpannya. Bila ternyata tak terbaca
    // (mis. baris dari backup yang disunting), kartunya tetap tampil dengan identitas yang pasti
    // ada — nama dan catatannya — bukan lenyap atau menggagalkan seluruh daftar.
    val wafat = AlmarhumHitung.dariIso(baris.tanggalWafat)
    val berikutnya = wafat?.let { AlmarhumHitung.berikutnya(it, hariIni) }
    // Sisa jadwal sesudah peringatan yang ditonjolkan, terbatas tiga: kartu ini adalah daftar,
    // bukan kalender — yang perlu sekali lihat adalah yang terdekat dan kira-kira yang menyusul.
    val menyusul = if (wafat == null || berikutnya == null) emptyList()
        else AlmarhumHitung.peringatan(wafat)
            .dropWhile { it.tanggal != berikutnya.tanggal }
            .drop(1)
            .take(3)

    KartuEntri(
        judul = baris.nama,
        isi = {
            Text(
                "Wafat ${wafat?.let { AlmarhumHitung.tanggalIndo(it) } ?: baris.tanggalWafat}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (baris.catatan.isNotBlank()) {
                Text(baris.catatan, style = MaterialTheme.typography.bodyMedium)
            }
            if (berikutnya != null) {
                // Hari-H disebut "Hari ini", bukan "Berikutnya" — pada hari peringatan itulah
                // notifikasinya berbunyi, dan layarnya tidak boleh menyebutnya hari lain.
                Text(
                    if (berikutnya.tanggal == hariIni) "Hari ini" else "Berikutnya",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${berikutnya.label} — ${AlmarhumHitung.tanggalIndo(berikutnya.tanggal)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (wafat != null) {
                // Wafatnya lebih dari tiga tahun lalu: seluruh jadwal yang dicatat telah lewat,
                // dan mengarang peringatan baru justru berbohong tentang apa yang diingatkan.
                Text(
                    "Tidak ada peringatan mendatang yang tercatat.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            menyusul.forEach { p ->
                Text(
                    "${p.label} • ${AlmarhumHitung.tanggalIndo(p.tanggal)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        aksi = {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onHapus) {
                Icon(
                    Icons.Filled.PersonOff,
                    contentDescription = "Hapus catatan ${baris.nama}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
