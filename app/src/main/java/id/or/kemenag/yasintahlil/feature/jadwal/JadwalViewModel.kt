package id.or.kemenag.yasintahlil.feature.jadwal

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Kota
import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.location.LocationProvider
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.PesanLokasi
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

data class JadwalUiState(
    val city: String = JadwalHariIni.KOTA_DEFAULT,
    val lat: Double = JadwalHariIni.LAT_DEFAULT,
    val lon: Double = JadwalHariIni.LON_DEFAULT,
    val method: PrayerCalc.Method = PrayerCalc.Method.from(JadwalHariIni.METODE_DEFAULT),
    val asarHanafi: Boolean = JadwalHariIni.asarHanafi(JadwalHariIni.ASAR_DEFAULT),
    val tzId: String = JadwalHariIni.TZ_DEFAULT,
    val corr: List<Int> = JadwalHariIni.parseCorr(JadwalHariIni.CORR_DEFAULT),
    val times: PrayerTimes? = null,
    val nowMin: Int = 0,
    /** Koordinat GPS sedang diikuti: jadwal dihitung ulang saat pengguna berpindah tempat. */
    val followGps: Boolean = false,
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

    /**
     * Kota yang dapat dipilih, dibaca dari bundel konten.
     *
     * Bundel konten dapat diperbarui atau diganti pengguna (layar "Pembaruan konten"); bila gagal
     * dibaca, daftarnya kosong. Dahulu kegagalan itu ditelan (`getOrDefault(emptyList())`) sehingga
     * tombol "Kota: …" membuka daftar kosong **tanpa satu pun keterangan** — pengguna menyimpulkan
     * tombolnya rusak, padahal bundelnya yang bermasalah. Sebabnya kini ikut dibawa ke layar lewat
     * [kotaGagal], sama seperti kegagalan membaca preferensi yang sudah punya `gagalMuat`.
     */
    private val hasilKota: Result<List<Kota>> = runCatching { repo.kota() }

    val kota: List<Kota> = hasilKota.getOrDefault(emptyList())

    /** Sebab daftar kota kosong, atau `null` bila bundel konten terbaca normal. */
    val kotaGagal: String? = hasilKota.exceptionOrNull()
        ?.let { "Daftar kota tidak dapat dibaca dari bundel konten (${it.message ?: it::class.java.simpleName})." }

    private val _state = MutableStateFlow(JadwalUiState())
    val state: StateFlow<JadwalUiState> = _state.asStateFlow()

    /** Pekerjaan yang mengikuti pembaruan lokasi live; dibatalkan saat lokasi manual dipilih. */
    private var locationJob: Job? = null

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
                val city = store.get(Store.CITY, JadwalHariIni.KOTA_DEFAULT)
                val lat = store.get(Store.LAT, JadwalHariIni.LAT_DEFAULT)
                val lon = store.get(Store.LON, JadwalHariIni.LON_DEFAULT)
                val method = PrayerCalc.Method.from(store.get(Store.METHOD, JadwalHariIni.METODE_DEFAULT))
                val asarH = JadwalHariIni.asarHanafi(store.get(Store.ASAR, JadwalHariIni.ASAR_DEFAULT))
                val tzId = store.get(Store.TZ, JadwalHariIni.TZ_DEFAULT)
                val corr = JadwalHariIni.parseCorr(store.get(Store.CORR, JadwalHariIni.CORR_DEFAULT))
                val ikut = store.get(Store.JADWAL_IKUT, BawaanPreferensi.JADWAL_IKUT_LOKASI)
                _state.value = _state.value.copy(
                    city = city, lat = lat, lon = lon, method = method,
                    asarHanafi = asarH, tzId = tzId, corr = corr, followGps = ikut,
                )
                recompute()
                // "Pembaruan jadwal saat lokasi berubah" (plan.txt): bila pengguna dahulu memilih
                // mengikuti GPS, ikuti lagi setelah aplikasi dibuka ulang — tanpa ini pilihannya
                // hanya berlaku sampai proses aplikasi mati.
                if (ikut && LocationProvider.hasPermission(ctx)) mulaiIkutiLokasi()
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    gagalMuat = e.message ?: e::class.simpleName ?: "penyebab tidak diketahui",
                )
            }
            _state.value = _state.value.copy(memuat = false)
        }
    }

    private fun currentTz(): TimeZone = JadwalHariIni.zona(_state.value.tzId)

    fun recompute() {
        val s = _state.value
        val now = Calendar.getInstance(currentTz())
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        // Aturan hisab disusun lewat `JadwalHariIni` — sama persis dengan penjadwal pengingat,
        // sehingga jam yang terlihat di layar ini adalah jam yang benar-benar dijadwalkan.
        val times = PrayerCalc.compute(now, JadwalHariIni.params(
            lat = s.lat, lon = s.lon, method = s.method,
            asarHanafi = s.asarHanafi, corr = s.corr, tz = currentTz(),
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
            _state.value = _state.value.copy(pesanLokasi = PesanLokasi.TANPA_FIX_JADWAL)
            return false
        }
        terapkanKoordinat(loc.latitude, loc.longitude, pesan = null)
        simpanDanJadwalkan(loc.latitude, loc.longitude)
        return true
    }

    /**
     * Ikuti (atau berhenti mengikuti) pembaruan lokasi live.
     *
     * `plan.txt` §"Jadwal Sholat dan Pengaturan Adzan" meminta *"Pembaruan jadwal saat lokasi
     * berubah"*. Sebelumnya layar ini hanya membaca satu perbaikan terakhir dan diam sesudahnya,
     * sehingga jadwal tetap memakai koordinat lama sampai pengguna menekan tombol GPS lagi.
     */
    fun setFollowGps(on: Boolean) {
        if (!on) {
            hentikanIkutiLokasi()
            viewModelScope.launch { store.set(Store.JADWAL_IKUT, false) }
            return
        }
        _state.value = _state.value.copy(followGps = true, pesanLokasi = null)
        viewModelScope.launch { store.set(Store.JADWAL_IKUT, true) }
        // Perbaikan terakhir dipakai lebih dahulu agar angka berubah seketika, lalu aliran live
        // menyusul — menunggu satelit tanpa menampilkan apa pun membuat layar tampak beku.
        LocationProvider.lastKnown(ctx)?.let {
            terapkanKoordinat(it.latitude, it.longitude, pesan = null)
            simpanDanJadwalkan(it.latitude, it.longitude)
        }
        mulaiIkutiLokasi()
    }

    /**
     * Ikuti pembaruan lokasi live. Perhitungan hanya diulang bila perpindahan melewati
     * [AMBANG_PINDAH_METER]: GPS bergoyang beberapa meter saat perangkat diam, dan menghitung
     * ulang (serta menjadwalkan ulang pengingat) pada setiap goyangan hanya membuang baterai.
     */
    private fun mulaiIkutiLokasi() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            var pernahAdaFix = false
            LocationProvider.updates(ctx).collect { loc ->
                pernahAdaFix = true
                val s = _state.value
                if (jarakMeter(s.lat, s.lon, loc.latitude, loc.longitude) >= AMBANG_PINDAH_METER) {
                    terapkanKoordinat(loc.latitude, loc.longitude, pesan = null)
                    simpanDanJadwalkan(loc.latitude, loc.longitude)
                }
            }
            // Aliran berakhir sendiri saat tak ada penyedia lokasi aktif (mis. GPS dimatikan).
            // Mengikuti lokasi tak lagi mungkin, jadi saklarnya dikembalikan — lebih jujur
            // daripada tetap menyala sementara tak ada yang diperbarui. State diubah langsung,
            // bukan lewat hentikanIkutiLokasi(), supaya koroutine ini tidak membatalkan dirinya
            // sendiri sebelum sempat menyimpan pilihannya.
            locationJob = null
            _state.value = _state.value.copy(
                followGps = false,
                pesanLokasi = if (pernahAdaFix) null else PesanLokasi.TANPA_FIX_JADWAL,
            )
            store.set(Store.JADWAL_IKUT, false)
        }
    }

    private fun hentikanIkutiLokasi() {
        locationJob?.cancel()
        locationJob = null
        _state.value = _state.value.copy(followGps = false)
    }

    /** Set koordinat pada state lalu hitung ulang jadwal. */
    private fun terapkanKoordinat(lat: Double, lon: Double, pesan: String?) {
        _state.value = _state.value.copy(
            city = LocationProvider.GPS_LABEL, lat = lat, lon = lon, pesanLokasi = pesan,
        )
        recompute()
    }

    private fun simpanDanJadwalkan(lat: Double, lon: Double) {
        viewModelScope.launch {
            store.set(Store.CITY, LocationProvider.GPS_LABEL)
            store.set(Store.LAT, lat)
            store.set(Store.LON, lon)
            store.set(Store.USE_GPS, true)
            PrayerScheduler.scheduleAll(ctx)
        }
    }

    /** Jarak antara dua koordinat dalam meter. */
    private fun jarakMeter(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Float {
        val out = FloatArray(1)
        Location.distanceBetween(aLat, aLon, bLat, bLon, out)
        return out[0]
    }

    /** Buang pesan "belum ada perbaikan lokasi" (mis. setelah pengguna memilih kota manual). */
    fun bersihkanPesanLokasi() {
        _state.value = _state.value.copy(pesanLokasi = null)
    }

    fun selectCity(k: Kota) {
        // Lokasi manual menang atas GPS: berhenti mengikuti, supaya perbaikan GPS berikutnya
        // tidak menimpa kota yang baru saja dipilih pengguna.
        hentikanIkutiLokasi()
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
        // Kunci disimpan lewat aturan bersama: `"syafii"` yang diketik ulang di sini dahulu menjadi
        // salinan kelima dari nilai yang sama, dan salah ketik satu huruf membuat pilihannya jatuh
        // ke faktor jumhur tanpa pesan galat. Pasangan boolean→kunci pun kini tinggal di
        // `JadwalHariIni.asarKey`, tempat yang sama dengan kebalikannya (`asarHanafi`).
        val kunci = JadwalHariIni.asarKey(hanafi)
        viewModelScope.launch { store.set(Store.ASAR, kunci); PrayerScheduler.scheduleAll(ctx) }
    }

    fun setTimezone(tzId: String) {
        _state.value = _state.value.copy(tzId = tzId); recompute()
        viewModelScope.launch { store.set(Store.TZ, tzId); PrayerScheduler.scheduleAll(ctx) }
    }

    fun setCorrection(index: Int, delta: Int) {
        val corr = _state.value.corr.toMutableList()
        // Rentangnya dari aturan bersama, sama dengan yang dipakai `parseCorr` saat membaca:
        // batas slider dan batas jalur baca tidak boleh berbeda.
        corr[index] = (corr[index] + delta).coerceIn(
            JadwalHariIni.KOREKSI_MIN_MENIT, JadwalHariIni.KOREKSI_MAKS_MENIT,
        )
        _state.value = _state.value.copy(corr = corr); recompute()
        viewModelScope.launch { store.set(Store.CORR, corr.joinToString(",")); PrayerScheduler.scheduleAll(ctx) }
    }

    companion object {
        // Sebab "belum ada perbaikan GPS" dan sarannya kini tinggal di `PesanLokasi`, bersama
        // dengan layar Kiblat: penjelasan atas kegagalan yang sama tidak boleh berbeda kata
        // antarlayar. Yang memang berbeda — saran untuk layar berpemilih kota (layar ini) versus
        // layar yang kotanya dipilih di menu Jadwal — tetap dipisah di sana.

        /**
         * Jarak minimum (meter) sebelum jadwal dihitung ulang saat mengikuti lokasi live.
         * Satu kilometer jauh lebih kecil daripada jarak antarkota, tetapi cukup besar untuk
         * menyerap goyangan GPS saat perangkat diam.
         */
        const val AMBANG_PINDAH_METER = 1_000f

        val TZ_OPTIONS = listOf(
            "" to "Perangkat (otomatis)",
            "Asia/Jakarta" to "WIB (UTC+7)",
            "Asia/Makassar" to "WITA (UTC+8)",
            "Asia/Jayapura" to "WIT (UTC+9)",
        )
    }
}
