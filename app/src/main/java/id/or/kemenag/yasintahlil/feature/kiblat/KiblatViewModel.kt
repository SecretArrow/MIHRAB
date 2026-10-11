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
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.location.LocationProvider
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.QiblaCalc
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import javax.inject.Inject

data class KiblatUiState(
    val lat: Double = -6.2,
    val lon: Double = 106.8,
    val useGps: Boolean = false,
    val hasPermission: Boolean = false,
    val bearing: Double = 295.0,
    val azimuth: Float = 0f,
    val hasCompass: Boolean = true,
    val calibrated: Boolean = true,
    /** True bila koordinat berasal dari perbaikan GPS live, bukan lokasi manual/kota. */
    val liveFix: Boolean = false,
    /** Satuan tampilan azimut: "derajat" atau "mil". */
    val unit: String = "derajat",
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
    private var rescheduled = false

    init {
        viewModelScope.launch {
            val useGps = store.get(Store.USE_GPS, false)
            val lat = store.get(Store.LAT, -6.2)
            val lon = store.get(Store.LON, 106.8)
            _state.update {
                it.copy(
                    useGps = useGps, lat = lat, lon = lon,
                    hasPermission = LocationProvider.hasPermission(ctx),
                    bearing = QiblaCalc.bearing(lat, lon),
                    unit = store.get(Store.COMPASS_UNIT, "derajat"),
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
            _state.update { it.copy(liveFix = false) }
        }
    }

    /** Muat perbaikan terakhir lalu ikuti pembaruan live (bila izin ada). */
    fun refreshLocation() {
        if (!LocationProvider.hasPermission(ctx)) {
            _state.update { it.copy(hasPermission = false) }
            return
        }
        _state.update { it.copy(hasPermission = true) }
        LocationProvider.lastKnown(ctx)?.let { applyLocation(it, live = false) }
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            LocationProvider.updates(ctx).collect { applyLocation(it, live = true) }
        }
    }

    private fun applyLocation(loc: Location, live: Boolean) {
        val bearing = QiblaCalc.bearing(loc.latitude, loc.longitude)
        _state.update {
            it.copy(lat = loc.latitude, lon = loc.longitude, bearing = bearing, liveFix = live)
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

    private fun startCompass() {
        val sm = sensorManager
        val rot = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sm == null || (rot == null && sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
            _state.update { it.copy(hasCompass = false, calibrated = false) }
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
            _state.update { it.copy(hasCompass = false, calibrated = false) }
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
        super.onCleared()
    }
}
