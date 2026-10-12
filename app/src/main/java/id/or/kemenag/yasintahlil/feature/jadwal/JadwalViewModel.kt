package id.or.kemenag.yasintahlil.feature.jadwal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Kota
import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.location.LocationProvider
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

data class JadwalUiState(
    val city: String = "Jakarta",
    val lat: Double = -6.2,
    val lon: Double = 106.8,
    val method: PrayerCalc.Method = PrayerCalc.Method.KEMENAG,
    val asarHanafi: Boolean = false,
    val tzId: String = "",
    val corr: List<Int> = List(6) { 0 },
    val times: PrayerTimes? = null,
    val nowMin: Int = 0,
    /** Preferensi sedang dibaca dari penyimpanan — layar menampilkan kerangka, bukan angka kosong. */
    val memuat: Boolean = true,
    /** Pesan bila preferensi gagal dibaca; layar menampilkan keadaan gagal + tombol coba lagi. */
    val gagalMuat: String? = null,
    /** Pesan bila GPS diminta tetapi belum ada perbaikan lokasi. */
    val pesanLokasi: String? = null,
)

@HiltViewModel
class JadwalViewModel @Inject constructor(
    private val store: Store,
    private val repo: ContentRepo,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {

    val kota: List<Kota> = runCatching { repo.kota() }.getOrDefault(emptyList())

    private val _state = MutableStateFlow(JadwalUiState())
    val state: StateFlow<JadwalUiState> = _state.asStateFlow()

    init { muatUlang() }

    /**
     * Baca preferensi lalu hitung ulang jadwal. Kegagalan membaca penyimpanan tidak dibiarkan
     * menjadi layar kosong: [JadwalUiState.gagalMuat] diisi sehingga layar dapat menawarkan
     * tombol mencoba kembali (`plan.txt`: "tampilan kosong, pesan kesalahan, dan tombol mencoba
     * kembali").
     */
    fun muatUlang() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, gagalMuat = null)
            runCatching {
                val city = store.get(Store.CITY, "Jakarta")
                val lat = store.get(Store.LAT, -6.2)
                val lon = store.get(Store.LON, 106.8)
                val method = PrayerCalc.Method.from(store.get(Store.METHOD, "KEMENAG"))
                val asarH = store.get(Store.ASAR, "syafii") == "hanafi"
                val tzId = store.get(Store.TZ, "")
                val corr = parseCorr(store.get(Store.CORR, "0,0,0,0,0,0"))
                _state.value = _state.value.copy(
                    city = city, lat = lat, lon = lon, method = method,
                    asarHanafi = asarH, tzId = tzId, corr = corr,
                )
                recompute()
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    gagalMuat = e.message ?: e::class.simpleName ?: "penyebab tidak diketahui",
                )
            }
            _state.value = _state.value.copy(memuat = false)
        }
    }

    private fun parseCorr(csv: String): List<Int> =
        (csv.split(",").map { it.trim().toIntOrNull() ?: 0 } + List(6) { 0 }).take(6)

    private fun currentTz(): TimeZone =
        _state.value.tzId.takeIf { it.isNotBlank() }?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()

    fun recompute() {
        val s = _state.value
        val now = Calendar.getInstance(currentTz())
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val times = PrayerCalc.compute(now, PrayerCalc.Params(
            lat = s.lat, lon = s.lon, method = s.method,
            asarFactor = if (s.asarHanafi) 2.0 else 1.0, corr = s.corr, tz = currentTz(),
        ))
        _state.value = s.copy(times = times, nowMin = nowMin)
    }

    /** Apakah izin lokasi sudah diberikan (untuk menampilkan tombol GPS). */
    fun hasLocationPermission(): Boolean = LocationProvider.hasPermission(ctx)

    /**
     * Pakai koordinat GPS perangkat. Mengembalikan false bila izin belum ada atau belum ada
     * perbaikan lokasi — pemanggil dapat meminta izin lalu mencoba lagi. Bila izin sudah ada
     * tetapi belum ada perbaikan, [JadwalUiState.pesanLokasi] diisi agar layar menjelaskan
     * alasannya dan menawarkan tombol coba lagi.
     */
    fun useGps(): Boolean {
        val loc = LocationProvider.lastKnown(ctx)
        if (loc == null) {
            _state.value = _state.value.copy(pesanLokasi = PESAN_TANPA_FIX)
            return false
        }
        _state.value = _state.value.copy(
            city = LocationProvider.GPS_LABEL, lat = loc.latitude, lon = loc.longitude,
            pesanLokasi = null,
        )
        recompute()
        viewModelScope.launch {
            store.set(Store.CITY, LocationProvider.GPS_LABEL)
            store.set(Store.LAT, loc.latitude)
            store.set(Store.LON, loc.longitude)
            store.set(Store.USE_GPS, true)
            PrayerScheduler.scheduleAll(ctx)
        }
        return true
    }

    /** Buang pesan "belum ada perbaikan lokasi" (mis. setelah pengguna memilih kota manual). */
    fun bersihkanPesanLokasi() {
        _state.value = _state.value.copy(pesanLokasi = null)
    }

    fun selectCity(k: Kota) {
        _state.value = _state.value.copy(city = k.nama, lat = k.lat, lon = k.lon, pesanLokasi = null)
        recompute()
        viewModelScope.launch {
            store.set(Store.CITY, k.nama); store.set(Store.LAT, k.lat); store.set(Store.LON, k.lon)
            store.set(Store.USE_GPS, false)
            PrayerScheduler.scheduleAll(ctx)
        }
    }

    fun setMethod(m: PrayerCalc.Method) {
        _state.value = _state.value.copy(method = m); recompute()
        viewModelScope.launch { store.set(Store.METHOD, m.name); PrayerScheduler.scheduleAll(ctx) }
    }

    fun setAsarHanafi(hanafi: Boolean) {
        _state.value = _state.value.copy(asarHanafi = hanafi); recompute()
        viewModelScope.launch { store.set(Store.ASAR, if (hanafi) "hanafi" else "syafii"); PrayerScheduler.scheduleAll(ctx) }
    }

    fun setTimezone(tzId: String) {
        _state.value = _state.value.copy(tzId = tzId); recompute()
        viewModelScope.launch { store.set(Store.TZ, tzId); PrayerScheduler.scheduleAll(ctx) }
    }

    fun setCorrection(index: Int, delta: Int) {
        val corr = _state.value.corr.toMutableList()
        corr[index] = (corr[index] + delta).coerceIn(-30, 30)
        _state.value = _state.value.copy(corr = corr); recompute()
        viewModelScope.launch { store.set(Store.CORR, corr.joinToString(",")); PrayerScheduler.scheduleAll(ctx) }
    }

    companion object {
        /** Ditampilkan saat GPS diminta tetapi belum ada perbaikan lokasi yang bisa dipakai. */
        const val PESAN_TANPA_FIX =
            "Belum ada perbaikan lokasi GPS. Pastikan GPS aktif dan berada di luar ruangan, " +
                "lalu coba lagi — atau pilih kota secara manual."

        val TZ_OPTIONS = listOf(
            "" to "Perangkat (otomatis)",
            "Asia/Jakarta" to "WIB (UTC+7)",
            "Asia/Makassar" to "WITA (UTC+8)",
            "Asia/Jayapura" to "WIT (UTC+9)",
        )
    }
}
