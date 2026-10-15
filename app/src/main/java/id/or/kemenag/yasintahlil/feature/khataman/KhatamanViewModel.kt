package id.or.kemenag.yasintahlil.feature.khataman

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.local.KegiatanDao
import id.or.kemenag.yasintahlil.core.data.local.KegiatanEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Keadaan satu layar Khataman & Streak: statistik yang seluruhnya terbaca dari tabel `kegiatan`.
 *
 * Nilai bawaannya sengaja cocok dengan keadaan "belum ada baris" — angka 0 dan daftar kosong —
 * sehingga layar tidak sempat menampilkan angka lain sebelum Room selesai ditanya (pola yang sama
 * dengan [id.or.kemenag.yasintahlil.feature.tasbih.TasbihState]).
 */
data class KhatamanState(
    /**
     * "Hari ini" dibekukan saat keadaan pertama kali dibuat (pola `TasbihState.hariIni`). Layar
     * yang terbuka terus lewat tengah malam memang masih menatap tanggal lamanya — satu-satunya
     * akibatnya, khataman pertama hari esok tercatat saat layar dibuka ulang.
     */
    val hariIni: String = RangkaianHari.iso(LocalDate.now()),
    /** Total hari yang pernah dicatat sebagai khataman Yasin — menghitung hari, bukan ketukan. */
    val totalKhatam: Int = 0,
    /** Riwayat khataman terbaru dulu — bahan daftar riwayat di layar. */
    val riwayat: List<KegiatanEntity> = emptyList(),
    /**
     * Tanggal-tanggal yang punya aktivitas baca. Diambil dari `observeAll()` lalu disaring di
     * sini — bukan kueri per jenis — karena satu hari boleh memuat dua jenis sekaligus
     * (`yasin_buka` dan `tahlil_buka`) dan keduanya tetap dihitung sebagai satu hari baca;
     * [RangkaianHari] sendiri menanggung tanggal kembar dan urutan yang tak beraturan.
     */
    val tanggalBaca: List<String> = emptyList(),
) {
    /** Rangkaian hari baca yang sedang berjalan — dihitung dari [tanggalBaca], bukan disimpan. */
    val rangkaian: Int get() = RangkaianHari.rangkaian(tanggalBaca, hariIni)

    /** Rekor rangkaian terpanjang yang pernah ada — dihitung ulang tiap daftarnya berubah. */
    val terpanjang: Int get() = RangkaianHari.terpanjang(tanggalBaca)

    /** Khataman hari ini sudah tercatat bila riwayat memuat baris bertanggal hari ini. */
    val khatamanHariIni: Boolean get() = riwayat.any { it.tanggal == hariIni }
}

/**
 * ViewModel Khataman & Streak. Statistiknya hidup dari tiga aliran tabel `kegiatan` (jumlah dan
 * riwayat `khatam_yasin`, serta seluruh baris untuk tanggal baca) — sehingga menekan tombol
 * khataman ikut menyegarkan seluruh angka di layar tanpa pengkabelan tambahan.
 *
 * Tidak ada penghapusan per baris: `KegiatanDao` memang tak menyediakannya (hanya `clear()`
 * menyeluruh), dan menambah pintu hapus bukan urusan layar ini — kendali data yang tersimpan
 * berumah di Pengaturan bagian "Data lokal" (aturan satu pengaturan satu rumah). Bagian itu
 * saat ini baru memuat favorit/bookmark/riwayat/progres; pintu hapus tabel `kegiatan` tinggal
 * ditambahkan pemilik layar Pengaturan di sana, bukan di sini. Mencatat ulang hari yang sama
 * juga tidak menambah baris — totalnya adalah jumlah HARI, bukan jumlah ketukan.
 */
@HiltViewModel
class KhatamanViewModel @Inject constructor(
    private val dao: KegiatanDao,
) : ViewModel() {

    private val _state = MutableStateFlow(KhatamanState())
    val state: StateFlow<KhatamanState> = _state.asStateFlow()

    /** Pengantri pencatatan — ketukan ganda yang berebut sebelum aliran Room sempat memancar. */
    private val kunciCatat = Mutex()

    init {
        viewModelScope.launch {
            dao.observeJumlah(RangkaianHari.KHATAM_YASIN).collect { n ->
                _state.update { it.copy(totalKhatam = n) }
            }
        }
        viewModelScope.launch {
            dao.observeByJenis(RangkaianHari.KHATAM_YASIN).collect { riwayat ->
                _state.update { it.copy(riwayat = riwayat) }
            }
        }
        viewModelScope.launch {
            dao.observeAll().collect { semua ->
                _state.update { s ->
                    s.copy(
                        tanggalBaca = semua
                            .filter { it.jenis == RangkaianHari.YASIN_BUKA || it.jenis == RangkaianHari.TAHIL_BUKA }
                            .map { it.tanggal },
                    )
                }
            }
        }
    }

    /**
     * Catat khataman Yasin hari ini — idempoten: bila hari ini sudah tercatat, ketukan ini tidak
     * menambah baris. Aliran `observeByJenis` kemudian menggantikan tombol di layar menjadi
     * lencana "sudah tercatat", sehingga tidak ada tombol yang tinggal terpajang tanpa guna.
     *
     * Pemeriksaan-sekali-per-hari beserta sisipannya dijalankan di dalam [Mutex]: dua ketukan
     * yang beruntun sebelum aliran Room sempat memancarkan baris baru memang kalah cepat dari
     * penggantian tombol, dan tanpa antrean keduanya bisa sama-sama lolos dari pemeriksaan itu.
     */
    fun catatKhatam() {
        viewModelScope.launch {
            kunciCatat.withLock {
                val hariIni = _state.value.hariIni
                if (dao.jumlahHariIni(hariIni, RangkaianHari.KHATAM_YASIN) == 0) {
                    dao.insert(
                        KegiatanEntity(
                            tanggal = hariIni,
                            jenis = RangkaianHari.KHATAM_YASIN,
                            keterangan = "",
                        ),
                    )
                }
            }
        }
    }
}
