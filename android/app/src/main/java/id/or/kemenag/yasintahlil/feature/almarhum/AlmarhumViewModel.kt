package id.or.kemenag.yasintahlil.feature.almarhum

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.or.kemenag.yasintahlil.core.data.local.AlmarhumDao
import id.or.kemenag.yasintahlil.core.data.local.AlmarhumEntity
import id.or.kemenag.yasintahlil.core.scheduling.AlmarhumWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel Tahlil Almarhum. Daftar almarhum hidup di Room (tabel `almarhum`) dan dipantau
 * reaktif — menambah maupun menghapus dari layar ini menyegarkan daftar yang sama tanpa
 * pengkabelan tambahan, pola yang sama dengan `TasbihViewModel`.
 *
 * Penjadwalan pengingat ikut dipicu dari sini, bukan dari `YasinApp`: satu-satunya cara memiliki
 * daftar almarhum adalah mencatatnya lewat layar ini, jadi membuka layar inilah tanda pertama
 * bahwa pengingatnya menjadi relevan. Pekerjaannya periodik dengan
 * [ExistingPeriodicWorkPolicy.KEEP]: membuka layar ini berkali-kali tidak menumpuk jadwal baru,
 * dan karena WorkManager menyimpan jadwalnya secara persisten, pemeriksaan harian tetap berjalan
 * setelah aplikasi ditutup maupun perangkat dinyalakan ulang.
 */
@HiltViewModel
class AlmarhumViewModel @Inject constructor(
    private val dao: AlmarhumDao,
    // `@param:` menegaskan penanda DI ini berlaku pada parameter konstruktor saja — persis
    // perilaku yang berlaku sekarang. Tanpa itu Kotlin 2.3 memperingatkan bahwa kelak anotasi
    // yang sama juga dipasang ke field (KT-73255).
    @param:ApplicationContext private val ctx: Context,
) : ViewModel() {

    private val _daftar = MutableStateFlow<List<AlmarhumEntity>>(emptyList())

    /** Daftar almarhum terbaru — kosong sampai Room selesai ditanya, bukan null. */
    val daftar: StateFlow<List<AlmarhumEntity>> = _daftar.asStateFlow()

    init {
        viewModelScope.launch {
            dao.observeAll().collect { _daftar.value = it }
        }
        // Nama unik + KEEP menjadikannya idempoten terhadap layar yang terus-menerus dibuka;
        // pekerjaan yang sudah terjadwal dibiarkan hidup dengan periodenya sendiri.
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            NAMA_PEKERJAAN,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AlmarhumWorker>(24, TimeUnit.HOURS).build(),
        )
    }

    /**
     * Tambah satu catatan. Nilai balik `false` berarti isian ditolak — nama kosong, tanggal tak
     * terbaca, atau tanggal wafat di masa depan — dan pemanggil membiarkan dialognya terbuka
     * supaya salah ketik diperbaiki di tempat, bukan dimulai dari awal (pola dialog "Lainnya"
     * Tasbih). Validasi dihitung ulang di sini meski dialog juga mengecek isiannya: jalur lain
     * yang kelak menambah baris tidak boleh bisa menitipkan tanggal yang tak sah.
     */
    fun tambah(nama: String, tanggalIso: String, catatan: String): Boolean {
        val namaBersih = nama.trim()
        val wafat = AlmarhumHitung.dariIso(tanggalIso)
        if (namaBersih.isBlank()) return false
        if (wafat == null || wafat.isAfter(LocalDate.now())) return false
        // Tanggal disimpan dalam bentuk baku iso() supaya baris dari jalur mana pun ejaannya
        // sama — bentuk longgar akan membuat satu tanggal tampil sebagai dua baris berbeda.
        viewModelScope.launch {
            dao.upsert(
                AlmarhumEntity(
                    nama = namaBersih,
                    tanggalWafat = AlmarhumHitung.iso(wafat),
                    catatan = catatan.trim(),
                ),
            )
        }
        return true
    }

    /**
     * Hapus satu catatan. Pengingatnya berhenti dengan sendirinya: pekerja harian membaca tabel
     * yang sama, dan baris yang tak ada tak lagi dapat mengingatkan.
     */
    fun hapus(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }

    companion object {
        /** Nama unik pekerjaan periodik pemeriksaan peringatan — dipakai bersama kebijakan KEEP. */
        const val NAMA_PEKERJAAN = "almarhum_cek"
    }
}
