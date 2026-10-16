package id.or.kemenag.yasintahlil.feature.tasbih

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.local.TasbihDao
import id.or.kemenag.yasintahlil.core.data.local.TasbihEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Keadaan satu layar Tasbih Digital: hitungan dan target hari ini, beserta riwayat 7 hari.
 *
 * Nilai bawaannya sengaja cocok dengan keadaan "belum ada baris hari ini" — hitungan 0 dan target
 * [TasbihViewModel.TARGET_BAWAAN] — sehingga layar tidak sempat menampilkan angka lain sebelum
 * Room selesai ditanya (pola yang sama dengan `DzikirUiState`).
 */
data class TasbihState(
    /**
     * "Hari ini" dibekukan saat keadaan pertama kali dibuat. Layar yang terbuka terus lewat
     * tengah malam memang masih menyimpan hitungan tanggal lamanya — satu-satunya akibatnya,
     * baris baru hari esok dimulai saat layar dibuka ulang.
     */
    val hariIni: String = TasbihHarian.iso(LocalDate.now()),
    val hitung: Int = 0,
    val target: Int = TasbihViewModel.TARGET_BAWAAN,
    val riwayat: List<TasbihEntity> = emptyList(),
) {
    /** Target dianggap tercapai bila hitungan menyamai/melebihi target yang sah. */
    val targetTercapai: Boolean get() = target > 0 && hitung >= target
}

/**
 * ViewModel Tasbih Digital. Hitungan dan target hari ini hidup di satu baris Room (tabel
 * `tasbih`, kuncinya tanggal) yang ditimpa tiap ketukan — sehingga hitungan tidak pernah hilang
 * oleh rotasi/process-death, dan riwayat 7 hari tinggal dibaca dari tabel yang sama.
 *
 * Menghitung memang terjadi di layar ini, bukan di Pengaturan: pengguna tidak memakai tombol
 * − / +, melainkan mengetuk lingkaran besar, memilih chip target, atau mengulang dari nol.
 */
@HiltViewModel
class TasbihViewModel @Inject constructor(
    private val dao: TasbihDao,
) : ViewModel() {

    private val _state = MutableStateFlow(TasbihState())
    val state: StateFlow<TasbihState> = _state.asStateFlow()

    init {
        val hariIni = _state.value.hariIni
        // Dua aliran dari tabel yang sama: baris hari ini untuk lingkaran ketuk, dan 7 hari
        // terakhir untuk daftar riwayat. Keduanya hidup reaktif — mengetuk lingkaran juga
        // menyegarkan riwayat tanpa pengkabelan tambahan.
        viewModelScope.launch {
            dao.observe(hariIni).collect { list ->
                val baris = list.firstOrNull()
                _state.update {
                    it.copy(
                        hitung = baris?.hitung ?: 0,
                        target = baris?.target ?: TARGET_BAWAAN,
                    )
                }
            }
        }
        viewModelScope.launch {
            dao.observeTerbaru(RIWAYAT_HARI).collect { riwayat ->
                _state.update { it.copy(riwayat = riwayat) }
            }
        }
    }

    /** Satu ketukan: hitungan hari ini bertambah satu, targetnya tetap. */
    fun tap() = ubah(_state.value.hitung + 1, _state.value.target)

    /** Ganti target hari ini; hitungannya tetap — mengganti target bukan mengulang hitungan. */
    fun aturTarget(n: Int) = ubah(_state.value.hitung, n)

    /** Mulai hitungan hari ini dari nol lagi; riwayat hari-hari sebelumnya tidak tersentuh. */
    fun reset() = ubah(0, _state.value.target)

    /**
     * Kosongkan seluruh riwayat — termasuk baris hari ini — dari layar Riwayat Tasbih. Keadaan di
     * memori ditimpa lebih dulu (polanya sama dengan [ubah]) supaya layar tidak sempat menampilkan
     * baris yang sudah dihapus; aliran `observe`/`observeTerbaru` kemudian menegaskannya.
     */
    fun kosongkan() {
        _state.update { it.copy(hitung = 0, riwayat = emptyList()) }
        viewModelScope.launch { dao.clear() }
    }

    private fun ubah(hitung: Int, target: Int) {
        // Diperbarui di memori lebih dulu supaya ketukan yang beruntun tidak tertinggal oleh
        // perjalanan bolak-balik Room — pola yang sama dengan penghitung dzikir (`setCount`).
        // Aliran `observe` kemudian menegaskan nilai yang sama dari simpanannya.
        _state.update { it.copy(hitung = hitung, target = target) }
        val hariIni = _state.value.hariIni
        viewModelScope.launch {
            dao.upsert(TasbihEntity(hariIni, hitung, target, System.currentTimeMillis()))
        }
    }

    companion object {
        /** Target hari baru yang belum dipilih penggunanya: 33 — jumlah lazim satu putaran tasbih. */
        const val TARGET_BAWAAN = 33

        /** Batas target yang sah; satu-satunya tempatnya agar dialog "Lainnya" tak punya angka sendiri. */
        const val TARGET_MIN = 1
        const val TARGET_MAKS = 9999

        /** Panjang daftar riwayat yang ditampilkan layar. */
        const val RIWAYAT_HARI = 7
    }
}
