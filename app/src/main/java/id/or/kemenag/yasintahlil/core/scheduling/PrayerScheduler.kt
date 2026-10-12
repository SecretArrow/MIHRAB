package id.or.kemenag.yasintahlil.core.scheduling

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import id.or.kemenag.yasintahlil.core.data.ReminderWorker
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Penjadwal pengingat waktu sholat berbasis WorkManager.
 *
 * Prinsip: setiap waktu sholat yang aktif punya satu pekerjaan terjadwal untuk kemunculan
 * berikutnya (hari ini bila belum lewat, atau besok). Setelah pekerjaan berjalan, ia menjadwalkan
 * ulang dirinya untuk hari berikutnya — sehingga rantai pengingat berjalan tanpa perlu membuka app.
 * Semua parameter (lokasi, metode, mazhab Asar, koreksi, zona waktu) dibaca dari [Store].
 *
 * Catatan ketepatan: WorkManager tidak dijamin tepat detik; OEM/optimasi baterai dapat menunda.
 * Pengguna diberi tahu di menu Adzan.
 */
object PrayerScheduler {
    const val TAG = "sholat"
    private val NAMES = listOf("Subuh", "Zuhur", "Asar", "Magrib", "Isya")

    /** Jenis pekerjaan: adzan (waktu masuk), pra-adzan (pengingat sebelum), doa & iqomah (sesudah). */
    const val JENIS_ADZAN = "adzan"
    const val JENIS_PRA = "pra"
    const val JENIS_DOA = "doa"
    const val JENIS_IQOMAH = "iqomah"

    /** Jeda pengingat "baca doa setelah adzan" dari waktu masuk, dalam menit. */
    const val DOA_DELAY_MIN = 5

    private data class Cfg(
        val lat: Double, val lon: Double, val method: PrayerCalc.Method,
        val asarFactor: Double, val corr: List<Int>, val tz: TimeZone,
        val on: List<Boolean>, val preMin: Int, val doaReminder: Boolean,
        val iqomahOn: Boolean, val iqomahMin: Int,
    )

    private suspend fun loadCfg(ctx: Context): Cfg {
        val store = Store(ctx)
        val tzId = store.get(Store.TZ, "")
        return Cfg(
            lat = store.get(Store.LAT, -6.2),
            lon = store.get(Store.LON, 106.8),
            method = PrayerCalc.Method.from(store.get(Store.METHOD, "KEMENAG")),
            asarFactor = if (store.get(Store.ASAR, "syafii") == "hanafi") 2.0 else 1.0,
            corr = parseCorr(store.get(Store.CORR, "0,0,0,0,0,0")),
            tz = if (tzId.isBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(tzId),
            on = (store.get(Store.ADZAN_ON, "1,1,1,1,1").split(",").map { it.trim() == "1" } + List(5) { true }).take(5),
            preMin = store.get(Store.ADZAN_PRE_MIN, 0),
            doaReminder = store.get(Store.DOA_ADZAN_REMINDER, false),
            iqomahOn = store.get(Store.IQOMAH_ON, false),
            iqomahMin = store.get(Store.IQOMAH_MIN, 10).coerceIn(1, 60),
        )
    }

    /** Susun ulang SELURUH pengingat dari preferensi tersimpan. Panggil saat app dibuka, setelah reboot, atau usai ubah pengaturan. */
    suspend fun scheduleAll(ctx: Context) {
        val store = Store(ctx)
        WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG)
        if (!store.get(Store.NOTIF, true)) return
        val cfg = loadCfg(ctx)
        NAMES.forEachIndexed { i, n -> if (cfg.on.getOrElse(i) { true }) enqueueNext(ctx, cfg, n) }
    }

    /** Jadwalkan kemunculan berikutnya untuk satu waktu sholat. Dipakai pekerja setelah ia berjalan. */
    suspend fun rescheduleOne(ctx: Context, nama: String) {
        val store = Store(ctx)
        if (!store.get(Store.NOTIF, true)) return
        val idx = NAMES.indexOf(nama)
        if (idx < 0) return
        val cfg = loadCfg(ctx)
        if (!cfg.on.getOrElse(idx) { true }) return
        enqueueNext(ctx, cfg, nama)
    }

    private fun enqueueNext(ctx: Context, cfg: Cfg, nama: String) {
        val now = Calendar.getInstance(cfg.tz)
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val times = PrayerCalc.compute(now, PrayerCalc.Params(
            lat = cfg.lat, lon = cfg.lon, method = cfg.method,
            asarFactor = cfg.asarFactor, corr = cfg.corr, tz = cfg.tz,
        ))
        val map = mapOf(
            "Subuh" to times.subuh, "Zuhur" to times.zuhur, "Asar" to times.asar,
            "Magrib" to times.magrib, "Isya" to times.isya,
        )
        val target = PrayerCalc.toMinutes(map.getValue(nama))
        var delta = target - nowMin
        if (delta <= 0) delta += 1440
        enqueue(ctx, "sholat_$nama", nama, delta.toLong(), JENIS_ADZAN)
        if (cfg.preMin > 0) {
            var d = delta - cfg.preMin
            if (d <= 0) d += 1440
            enqueue(ctx, "pre_$nama", nama, d.toLong(), JENIS_PRA)
        }
        // Pekerjaan yang berbunyi SESUDAH waktu masuk memakai kunci bertanggal.
        //
        // Alasannya bukan kerapian: pekerja adzan, begitu berbunyi, menjadwalkan ulang dirinya untuk
        // kemunculan berikutnya (besok), dan penjadwalan ulang itu juga mengantrekan pekerjaan
        // susulan. Dengan nama tetap, `ExistingWorkPolicy.REPLACE` akan **membatalkan** pekerjaan
        // susulan hari ini yang tinggal beberapa menit lagi berbunyi — sehingga pengingat doa (dan
        // iqomah) tak pernah terdengar sama sekali. Pembeda tanggal memisahkan keduanya: susulan
        // hari ini tetap berjalan, susulan besok memakai nama lain.
        val hari = kunciHari(now, delta.toLong())
        if (cfg.doaReminder) {
            enqueue(ctx, "doa_${nama}_$hari", nama, (delta + DOA_DELAY_MIN).toLong(), JENIS_DOA)
        }
        if (cfg.iqomahOn) {
            enqueue(ctx, "iqomah_${nama}_$hari", nama, (delta + cfg.iqomahMin).toLong(), JENIS_IQOMAH)
        }
    }

    /**
     * Tanggal kemunculan yang sedang dijadwalkan (zona waktu pengguna), sebagai pembeda nama
     * pekerjaan susulan. Memakai [Locale.US] agar angkanya selalu 0–9 — pada lokal berangka lain
     * `%d` dapat menghasilkan angka yang berbeda-beda bentuk dan nama pekerjaan jadi tak terduga.
     */
    private fun kunciHari(now: Calendar, deltaMenit: Long): String {
        val c = now.clone() as Calendar
        c.add(Calendar.MINUTE, deltaMenit.toInt())
        return String.format(
            Locale.US, "%04d%02d%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun enqueue(ctx: Context, unique: String, nama: String, delayMin: Long, jenis: String) {
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMin, TimeUnit.MINUTES)
            .setInputData(workDataOf("nama" to nama, "jenis" to jenis))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(unique, ExistingWorkPolicy.REPLACE, req)
    }

    private fun parseCorr(csv: String): List<Int> =
        (csv.split(",").map { it.trim().toIntOrNull() ?: 0 } + List(6) { 0 }).take(6)
}
