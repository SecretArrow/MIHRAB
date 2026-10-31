package id.or.kemenag.yasintahlil.feature.home

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.util.InfoHarian
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keadaan beranda (`plan.txt` §"Beranda Utama").
 *
 * Sebelumnya beranda menyimpan belasan `remember { mutableStateOf(...) }` lalu mengisinya sekali
 * lewat satu `LaunchedEffect(Unit)`: nilai Store dibaca sekali dan tidak reaktif, dan seluruh
 * keadaan hilang saat konfigurasi berubah (rotasi). Di sini setiap nilai dibaca sebagai aliran
 * dari [Store], sehingga mengubah lokasi/metode di layar Jadwal langsung terlihat di beranda
 * tanpa membuka ulang layar.
 *
 * Saklar pengingat waktu sholat dan ringkasan favorit/riwayat yang dahulu ikut di sini sudah
 * pulang: saklarnya ke tab Pengaturan (rumah seluruh pengaturan), angka simpanannya ke layar
 * Favorit & Riwayat yang memang menampilkan daftarnya — beranda tak lagi membawa keadaan yang
 * tak dipakainya sendiri.
 */
data class HomeUiState(
    val city: String = JadwalHariIni.KOTA_DEFAULT,
    val lat: Double = JadwalHariIni.LAT_DEFAULT,
    val lon: Double = JadwalHariIni.LON_DEFAULT,
    val asarHanafi: Boolean = JadwalHariIni.asarHanafi(JadwalHariIni.ASAR_DEFAULT),
    val method: PrayerCalc.Method = PrayerCalc.Method.from(JadwalHariIni.METODE_DEFAULT),
    val tzId: String = JadwalHariIni.TZ_DEFAULT,
    val corr: List<Int> = JadwalHariIni.parseCorr(JadwalHariIni.CORR_DEFAULT),
    val last: String = BawaanPreferensi.TERAKHIR_DIBACA,
    val infoCard: Boolean = BawaanPreferensi.KARTU_INFO,
    /** Preferensi "Arab" dari Pengaturan — kartu harian ikut menyembunyikan Arab bila dimatikan. */
    val showArab: Boolean = BawaanPreferensi.TAMPIL_ARAB,
    /** Butir "Info Islami harian" untuk hari ini; `null` bila belum ada butir yang layak. */
    val info: InfoHarian.Butir? = null,
    /** Sebab butir harian tidak dapat dibaca dari bundel konten, atau `null` bila normal. */
    val infoGagal: String? = null,
    /**
     * Id kelompok dzikir dari bundel konten — dipakai `RuteKonten` untuk mengenali bacaan dzikir
     * pada "Terakhir dibaca". Beranda dahulu tidak memilikinya sama sekali, sehingga jenis itu
     * selalu terbaca sebagai tidak punya tujuan.
     */
    val grupDzikir: Set<String> = emptySet(),
    val hijriCorr: Int = BawaanPreferensi.KOREKSI_HIJRIAH,
    /** Detak jam beranda — diperbarui tiap detik agar hitung mundur tetap hidup. */
    val nowMillis: Long = System.currentTimeMillis(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val store: Store,
    private val repo: ContentRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        alir(Store.CITY, JadwalHariIni.KOTA_DEFAULT) { s, v -> s.copy(city = v) }
        alir(Store.LAT, JadwalHariIni.LAT_DEFAULT) { s, v -> s.copy(lat = v) }
        alir(Store.LON, JadwalHariIni.LON_DEFAULT) { s, v -> s.copy(lon = v) }
        alir(Store.ASAR, JadwalHariIni.ASAR_DEFAULT) { s, v ->
            s.copy(asarHanafi = JadwalHariIni.asarHanafi(v))
        }
        alir(Store.METHOD, JadwalHariIni.METODE_DEFAULT) { s, v -> s.copy(method = PrayerCalc.Method.from(v)) }
        alir(Store.TZ, JadwalHariIni.TZ_DEFAULT) { s, v -> s.copy(tzId = v) }
        alir(Store.CORR, JadwalHariIni.CORR_DEFAULT) { s, v -> s.copy(corr = JadwalHariIni.parseCorr(v)) }
        alir(Store.LAST, BawaanPreferensi.TERAKHIR_DIBACA) { s, v -> s.copy(last = v) }
        alir(Store.INFO_CARD, BawaanPreferensi.KARTU_INFO) { s, v -> s.copy(infoCard = v) }
        alir(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB) { s, v -> s.copy(showArab = v) }
        alir(Store.HIJRI_CORR, BawaanPreferensi.KOREKSI_HIJRIAH) { s, v -> s.copy(hijriCorr = v) }

        // Jam beranda: satu detak tiap detik, bukan satu penjadwalan ulang layar.
        viewModelScope.launch {
            while (true) {
                _state.update { it.copy(nowMillis = System.currentTimeMillis()) }
                delay(1_000)
            }
        }

        // Kartu "Info Islami harian": butirnya berganti saat tanggal berganti — dahulu kartu ini
        // memuat satu kalimat tetap yang khusus hari Jumat tetapi tampil setiap hari. Tanggalnya
        // diikuti dari zona waktu tampilan, sama seperti jam di atasnya. Diperiksa tiap menit, bukan
        // tiap detik: tanggal hanya berubah sekali sehari, dan nilai yang sama tidak memicu
        // recomposisi karena `StateFlow` menyaring nilai yang setara.
        viewModelScope.launch {
            val hasil = runCatching { repo.dzikir() }
            val grup = hasil.getOrDefault(emptyList())
            val kandidat = hasil.map { InfoHarian.dariDzikir(it) }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    // Id kelompok dzikir diambil dari pembacaan yang sama dengan butir harian —
                    // satu pembacaan bundel, satu sebab kegagalan, bukan dua yang bisa berbeda.
                    grupDzikir = grup.map { g -> g.id }.toSet(),
                    infoGagal = hasil.exceptionOrNull()?.let { e ->
                        "Bacaan tidak dapat dibaca dari bundel konten " +
                            "(${e.message ?: e::class.java.simpleName}); info harian dan pengenalan " +
                            "tujuan bacaan tersimpan ikut terganggu."
                    },
                )
            }
            while (true) {
                val tanggal = InfoHarian.tanggal(System.currentTimeMillis(), InfoHarian.zona(_state.value.tzId))
                _state.update { it.copy(info = InfoHarian.pilih(tanggal, kandidat)) }
                delay(60_000)
            }
        }
    }

    /** Ikuti satu preferensi Store sebagai aliran, bukan membacanya sekali di layar. */
    private fun <T> alir(key: Preferences.Key<T>, def: T, ubah: (HomeUiState, T) -> HomeUiState) {
        viewModelScope.launch {
            store.flow(key, def).collect { v -> _state.update { ubah(it, v) } }
        }
    }
}
