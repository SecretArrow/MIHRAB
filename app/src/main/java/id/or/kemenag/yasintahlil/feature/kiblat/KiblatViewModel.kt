package id.or.kemenag.yasintahlil.feature.kiblat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.TampilanKompas
import id.or.kemenag.yasintahlil.core.location.LocationProvider
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.PesanLokasi
import id.or.kemenag.yasintahlil.core.util.QiblaCalc
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import javax.inject.Inject

data class KiblatUiState(
    val lat: Double = JadwalHariIni.LAT_DEFAULT,
    val lon: Double = JadwalHariIni.LON_DEFAULT,
    val useGps: Boolean = false,
    val hasPermission: Boolean = false,
    /**
     * Azimut awal dari lokasi bawaan, dihitung dengan rumus yang sama dengan nilai sesungguhnya —
     * bukan angka `295.0` yang ditulis tetap. Angka tetap itu benar untuk Jakarta dan **hanya**
     * Jakarta: ia harus disunting sendiri setiap kali lokasi bawaannya berubah, dan bila terlewat
     * layar sempat menampilkan arah kiblat kota lain.
     */
    val bearing: Double = QiblaCalc.bearing(JadwalHariIni.LAT_DEFAULT, JadwalHariIni.LON_DEFAULT),
    val azimuth: Float = 0f,
    val hasCompass: Boolean = true,
    /**
     * Akurasi sensor terakhir: `true` layak dipercaya, `false` tidak, `null` **belum ada pembacaan**.
     * Bukan boolean biasa: sebelum sensor mengirim nilai, statusnya belum diketahui — dan
     * menampilkan "terkalibrasi" saat itu adalah klaim yang belum terbukti.
     */
    val calibrated: Boolean? = null,
    /** True bila koordinat berasal dari perbaikan GPS live, bukan lokasi manual/kota. */
    val liveFix: Boolean = false,
    /** Sedang menunggu perbaikan GPS — layar menampilkan indikator proses, bukan kompas beku. */
    val mencari: Boolean = false,
    /** Pesan bila GPS diminta tetapi belum ada perbaikan lokasi yang bisa dipakai. */
    val pesanLokasi: String? = null,
    /** Satuan tampilan azimut — salah satu dari [TampilanKompas.SATUAN]. */
    val unit: String = TampilanKompas.SATUAN_DEFAULT,
    /** Tampilan kompas: jarum digambar atau hanya angka & peta (lihat [TampilanKompas]). */
    val tampilan: String = TampilanKompas.KOMPAS,
)

/**
 * Arah kiblat: azimut matematis (Ka'bah SIHAT Kemenag) dipadukan dengan kompas perangkat.
 * Memakai sensor rotasi bila tersedia (lebih stabil), jika tidak akselerometer + magnetometer.
 */
@HiltViewModel
class KiblatViewModel @Inject constructor(
    private val store: Store,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(KiblatUiState())
    val state: StateFlow<KiblatUiState> = _state.asStateFlow()

    private val sensorManager: SensorManager? =
        ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var compassListener: SensorEventListener? = null
    private var locationJob: Job? = null
    private var watchdogJob: Job? = null
    private var rescheduled = false

    init {
        viewModelScope.launch {
            val useGps = store.get(Store.USE_GPS, BawaanPreferensi.LOKASI_DARI_GPS)
            val lat = store.get(Store.LAT, JadwalHariIni.LAT_DEFAULT)
            val lon = store.get(Store.LON, JadwalHariIni.LON_DEFAULT)
            _state.update {
                it.copy(
                    useGps = useGps, lat = lat, lon = lon,
                    hasPermission = LocationProvider.hasPermission(ctx),
                    bearing = QiblaCalc.bearing(lat, lon),
                    unit = store.get(Store.COMPASS_UNIT, TampilanKompas.SATUAN_DEFAULT),
                    tampilan = store.get(Store.COMPASS_VIEW, TampilanKompas.KOMPAS),
                )
            }
            if (useGps) refreshLocation()
        }
        startCompass()
    }

    /** Dipanggil ulang setelah pengguna kembali dari dialog izin sistem. */
    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasPermission = granted) }
        if (granted) {
            setUseGps(true)
        } else {
            setUseGps(false)
        }
    }

    fun setUseGps(on: Boolean) {
        _state.update { it.copy(useGps = on) }
        viewModelScope.launch { store.set(Store.USE_GPS, on) }
        if (on) {
            refreshLocation()
        } else {
            locationJob?.cancel()
            watchdogJob?.cancel()
            _state.update { it.copy(liveFix = false, mencari = false, pesanLokasi = null) }
        }
    }

    /**
     * Muat perbaikan terakhir lalu ikuti pembaruan live (bila izin ada).
     *
     * Aliran lokasi bisa berakhir sendiri tanpa pernah mengirim satu pun perbaikan — mis. saat
     * GPS perangkat dimatikan. Sebelumnya layar hanya diam (kompas tampak beku, tanpa penjelasan);
     * kini keadaan itu diisi [KiblatUiState.pesanLokasi] sehingga pengguna tahu sebabnya dan bisa
     * mencoba lagi, dan selama menunggu [KiblatUiState.mencari] menampilkan indikator proses.
     */
    fun refreshLocation() {
        if (!LocationProvider.hasPermission(ctx)) {
            _state.update { it.copy(hasPermission = false, mencari = false, pesanLokasi = null) }
            return
        }
        _state.update { it.copy(hasPermission = true, mencari = true, pesanLokasi = null) }
        LocationProvider.lastKnown(ctx)?.let { applyLocation(it, live = false) }
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            LocationProvider.updates(ctx).collect { applyLocation(it, live = true) }
            // Berakhirnya aliran = tidak ada penyedia lokasi aktif.
            tandaiTanpaFix()
        }
        watchdogJob?.cancel()
        watchdogJob = viewModelScope.launch {
            delay(TIMEOUT_LOKASI_MS)
            tandaiTanpaFix()
        }
    }

    /** Bila belum ada perbaikan sama sekali, jelaskan dan berhenti "mencari". */
    private fun tandaiTanpaFix() {
        _state.update { s ->
            if (s.liveFix) s.copy(mencari = false, pesanLokasi = null)
            else s.copy(mencari = false, pesanLokasi = PesanLokasi.TANPA_FIX_KIBLAT)
        }
    }

    private fun applyLocation(loc: Location, live: Boolean) {
        val bearing = QiblaCalc.bearing(loc.latitude, loc.longitude)
        _state.update {
            it.copy(lat = loc.latitude, lon = loc.longitude, bearing = bearing, liveFix = live,
                mencari = false, pesanLokasi = null)
        }
        viewModelScope.launch {
            store.set(Store.LAT, loc.latitude)
            store.set(Store.LON, loc.longitude)
            store.set(Store.CITY, LocationProvider.GPS_LABEL)
            // Susun ulang pengingat sekali setelah perbaikan pertama agar memakai koordinat baru.
            if (live && !rescheduled) {
                rescheduled = true
                PrayerScheduler.scheduleAll(ctx)
            }
        }
    }

    /** Satuan tampilan azimut. */
    fun setUnit(u: String) {
        _state.update { it.copy(unit = u) }
        viewModelScope.launch { store.set(Store.COMPASS_UNIT, u) }
    }

    /** Tampilan kompas: jarum & peta, atau angka & peta saja. */
    fun setTampilan(v: String) {
        if (!TampilanKompas.sah(v)) return
        _state.update { it.copy(tampilan = v) }
        viewModelScope.launch { store.set(Store.COMPASS_VIEW, v) }
    }

    private fun startCompass() {
        val sm = sensorManager
        val rot = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sm == null || (rot == null && sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
            _state.update { it.copy(hasCompass = false, calibrated = null) }
            return
        }
        if (rot != null) {
            val li = object : SensorEventListener {
                private val rotMat = FloatArray(9)
                private val orient = FloatArray(3)

                override fun onSensorChanged(e: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotMat, e.values)
                    SensorManager.getOrientation(rotMat, orient)
                    _state.update {
                        it.copy(
                            azimuth = Math.toDegrees(orient[0].toDouble()).toFloat(),
                            calibrated = e.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE,
                        )
                    }
                }

                override fun onAccuracyChanged(s: Sensor?, a: Int) {
                    if (a == SensorManager.SENSOR_STATUS_UNRELIABLE) _state.update { it.copy(calibrated = false) }
                }
            }
            compassListener = li
            sm.registerListener(li, rot, SensorManager.SENSOR_DELAY_UI)
            return
        }
        // Cadangan: akselerometer + magnetometer.
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (acc == null || mag == null) {
            _state.update { it.copy(hasCompass = false, calibrated = null) }
            return
        }
        val grav = FloatArray(3)
        val geo = FloatArray(3)
        var hasG = false
        var hasM = false
        val li = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    e.values.copyInto(grav); hasG = true
                } else {
                    e.values.copyInto(geo); hasM = true
                }
                if (!hasG || !hasM) return
                val r = FloatArray(9)
                val o = FloatArray(3)
                if (SensorManager.getRotationMatrix(r, null, grav, geo)) {
                    SensorManager.getOrientation(r, o)
                    _state.update { it.copy(azimuth = Math.toDegrees(o[0].toDouble()).toFloat()) }
                }
                val strength = sqrt((geo[0] * geo[0] + geo[1] * geo[1] + geo[2] * geo[2]).toDouble())
                _state.update { it.copy(calibrated = strength in 20.0..80.0) }
            }

            override fun onAccuracyChanged(s: Sensor?, a: Int) {
                if (a == SensorManager.SENSOR_STATUS_UNRELIABLE) _state.update { it.copy(calibrated = false) }
            }
        }
        compassListener = li
        sm.registerListener(li, acc, SensorManager.SENSOR_DELAY_UI)
        sm.registerListener(li, mag, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onCleared() {
        compassListener?.let { sensorManager?.unregisterListener(it) }
        locationJob?.cancel()
        watchdogJob?.cancel()
        super.onCleared()
    }

    companion object {
        /** Lama menunggu perbaikan GPS sebelum layar menjelaskan bahwa belum ada perbaikan. */
        const val TIMEOUT_LOKASI_MS = 20_000L

        // Sebab kegagalan GPS dan sarannya tinggal di `PesanLokasi` bersama layar Jadwal; layar ini
        // memakai varian yang sarannya berbeda (tanpa pemilih kota sendiri).
    }
}
