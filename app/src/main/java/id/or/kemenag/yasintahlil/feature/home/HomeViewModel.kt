package id.or.kemenag.yasintahlil.feature.home

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
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
 * dari [Store] dan Room, sehingga mengubah lokasi/metode di layar Jadwal atau mematikan pengingat
 * langsung terlihat di beranda tanpa membuka ulang layar.
 */
data class HomeUiState(
    val city: String = "Jakarta",
    val lat: Double = -6.2,
    val lon: Double = 106.8,
    val asarHanafi: Boolean = false,
    val method: PrayerCalc.Method = PrayerCalc.Method.KEMENAG,
    val tzId: String = "",
    val corr: List<Int> = List(6) { 0 },
    val last: String = "",
    val infoCard: Boolean = true,
    val hijriCorr: Int = 0,
    val notif: Boolean = true,
    val favCount: Int = 0,
    val histCount: Int = 0,
    /** Detak jam beranda — diperbarui tiap detik agar hitung mundur tetap hidup. */
    val nowMillis: Long = System.currentTimeMillis(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val store: Store,
    userData: UserDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        alir(Store.CITY, "Jakarta") { s, v -> s.copy(city = v) }
        alir(Store.LAT, -6.2) { s, v -> s.copy(lat = v) }
        alir(Store.LON, 106.8) { s, v -> s.copy(lon = v) }
        alir(Store.ASAR, "syafii") { s, v -> s.copy(asarHanafi = v == "hanafi") }
        alir(Store.METHOD, "KEMENAG") { s, v -> s.copy(method = PrayerCalc.Method.from(v)) }
        alir(Store.TZ, "") { s, v -> s.copy(tzId = v) }
        alir(Store.CORR, "0,0,0,0,0,0") { s, v -> s.copy(corr = parseCorr(v)) }
        alir(Store.LAST, "") { s, v -> s.copy(last = v) }
        alir(Store.INFO_CARD, true) { s, v -> s.copy(infoCard = v) }
        alir(Store.HIJRI_CORR, 0) { s, v -> s.copy(hijriCorr = v) }
        alir(Store.NOTIF, true) { s, v -> s.copy(notif = v) }

        // Ringkasan favorit & riwayat (Room) — ikut terbarui saat berubah di layar lain.
        viewModelScope.launch {
            userData.favorites().collect { list -> _state.update { it.copy(favCount = list.size) } }
        }
        viewModelScope.launch {
            userData.recentHistory(50).collect { list -> _state.update { it.copy(histCount = list.size) } }
        }

        // Jam beranda: satu detak tiap detik, bukan satu penjadwalan ulang layar.
        viewModelScope.launch {
            while (true) {
                _state.update { it.copy(nowMillis = System.currentTimeMillis()) }
                delay(1_000)
            }
        }
    }

    /** Menyalakan/mematikan pengingat waktu sholat lalu menjadwalkan ulang pekerja latar. */
    fun setNotif(v: Boolean) {
        viewModelScope.launch {
            store.set(Store.NOTIF, v)
            PrayerScheduler.scheduleAll(ctx)
        }
    }

    /** Ikuti satu preferensi Store sebagai aliran, bukan membacanya sekali di layar. */
    private fun <T> alir(key: Preferences.Key<T>, def: T, ubah: (HomeUiState, T) -> HomeUiState) {
        viewModelScope.launch {
            store.flow(key, def).collect { v -> _state.update { ubah(it, v) } }
        }
    }

    /** "1,0,1,…" → enam angka; panjang selalu tepat enam agar indeks koreksi tak bergeser. */
    private fun parseCorr(csv: String): List<Int> =
        (csv.split(",").map { it.trim().toIntOrNull() ?: 0 } + List(6) { 0 }).take(6)
}
