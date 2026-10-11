package id.or.kemenag.yasintahlil.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Akses lokasi perangkat untuk arah kiblat & jadwal sholat.
 *
 * Aplikasi tetap berfungsi penuh tanpa izin ini: pengguna dapat memilih kota secara manual di
 * menu Jadwal. Lokasi tidak pernah dikirim ke server mana pun (lihat docs/PRIVACY.md).
 */
object LocationProvider {

    /** Label kota saat koordinat berasal dari GPS perangkat, bukan pilihan manual. */
    const val GPS_LABEL = "Lokasi GPS"

    /** True bila izin lokasi (halus atau kasar) sudah diberikan pengguna. */
    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Perbaikan terakhir yang diketahui (tanpa menunggu satelit). Bisa null bila belum ada. */
    @SuppressLint("MissingPermission") // Dijaga oleh hasPermission() di baris pertama.
    fun lastKnown(ctx: Context): Location? {
        if (!hasPermission(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        }.getOrNull()
    }

    /**
     * Aliran pembaruan lokasi live (GPS + jaringan). Berakhir sendiri bila izin tidak ada atau
     * tidak ada penyedia lokasi yang aktif — pemanggil tidak perlu menangani error.
     */
    @SuppressLint("MissingPermission") // Dijaga oleh hasPermission() di baris pertama.
    fun updates(ctx: Context): Flow<Location> = callbackFlow {
        if (!hasPermission(ctx)) {
            close()
            return@callbackFlow
        }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            close()
            return@callbackFlow
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) {
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            @Deprecated("Diperlukan oleh antarmuka LocationListener lama")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        runCatching {
            providers.forEach { p ->
                lm.requestLocationUpdates(p, MIN_INTERVAL_MS, MIN_DISTANCE_M, listener, Looper.getMainLooper())
            }
        }
        awaitClose { runCatching { lm.removeUpdates(listener) } }
    }

    private const val MIN_INTERVAL_MS = 5_000L
    private const val MIN_DISTANCE_M = 10f
}
