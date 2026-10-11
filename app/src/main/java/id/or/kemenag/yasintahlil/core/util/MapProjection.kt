package id.or.kemenag.yasintahlil.core.util

import kotlin.math.cos

/**
 * Proyeksi equirectangular untuk **peta skematik offline** (tanpa ubin jaringan, tanpa API key):
 * bujur → x (diskalakan `cos(lintang tengah)` agar tak melebar di lintang tinggi),
 * lintang → y dengan utara di atas. Murni perhitungan, sehingga dapat diuji tanpa Android.
 *
 * Hasil `project` dinormalkan: `y ∈ [0,1]`, `x ∈ [0, aspect]` — pemanggil tinggal
 * memasukkan kotak `aspect × 1` ke kanvas sambil menjaga rasio.
 */
object MapProjection {

    data class Bounds(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

    /**
     * Buka lipatan bujur: tiap nilai digeser kelipatan 360° agar berjarak ≤180° dari nilai
     * sebelumnya. Tanpa ini, rute yang melewati garis tanggal (mis. Alaska → Makkah) akan
     * tampak melintasi seluruh peta.
     */
    fun unwrapLongitudes(lons: List<Double>): List<Double> {
        if (lons.isEmpty()) return emptyList()
        val out = ArrayList<Double>(lons.size)
        out += lons.first()
        for (i in 1 until lons.size) {
            var v = lons[i]
            val prev = out[i - 1]
            while (v - prev > 180.0) v -= 360.0
            while (v - prev < -180.0) v += 360.0
            out += v
        }
        return out
    }

    /** Kotak batas titik-titik, diberi padding derajat agar penanda tidak menempel tepi. */
    fun bounds(points: List<Pair<Double, Double>>, padDeg: Double = 3.0): Bounds {
        val lats = points.map { it.first }
        val lons = points.map { it.second }
        return Bounds(
            minLat = lats.min() - padDeg,
            maxLat = lats.max() + padDeg,
            minLon = lons.min() - padDeg,
            maxLon = lons.max() + padDeg,
        )
    }

    private fun kx(b: Bounds): Double =
        cos(Math.toRadians((b.minLat + b.maxLat) / 2)).coerceAtLeast(1e-6)

    /** Rasio lebar/tinggi peta (memperhitungkan `cos` lintang tengah). */
    fun aspect(b: Bounds): Float {
        val h = (b.maxLat - b.minLat).coerceAtLeast(1e-6)
        val w = (b.maxLon - b.minLon).coerceAtLeast(1e-6) * kx(b)
        return (w / h).toFloat()
    }

    /** Koordinat ternormalisasi: `x ∈ [0, aspect]`, `y ∈ [0, 1]` (utara di atas). */
    fun project(lat: Double, lon: Double, b: Bounds): Pair<Float, Float> {
        val k = kx(b)
        val h = (b.maxLat - b.minLat).coerceAtLeast(1e-6)
        val x = (lon * k - b.minLon * k) / h
        val y = (b.maxLat - lat) / h
        return x.toFloat() to y.toFloat()
    }
}
