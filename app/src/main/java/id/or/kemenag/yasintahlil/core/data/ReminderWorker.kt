package id.or.kemenag.yasintahlil.core.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Pengingat waktu sholat via WorkManager. Ketepatan dapat dipengaruhi optimasi baterai/OEM. */
class ReminderWorker(app: Context, params: WorkerParameters) : CoroutineWorker(app, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val nama = inputData.getString("nama") ?: return@withContext Result.success()
        val store = Store(applicationContext)
        if (!store.get(Store.NOTIF, true)) return@withContext Result.success()
        val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            mgr.createNotificationChannel(NotificationChannel("adzan", "Adzan", NotificationManager.IMPORTANCE_HIGH))
        } catch (_: Exception) { }
        val notif = NotificationCompat.Builder(applicationContext, "adzan")
            .setContentTitle("Waktu $nama telah tiba")
            .setContentText("Segerakan sholat. Jadwal metode Kemenag — verifikasi sihat.kemenag.dev")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true).build()
        try { mgr.notify(nama.hashCode(), notif) } catch (_: Exception) { }
        // jadwalkan ulang besok
        scheduleToday(applicationContext)
        Result.success()
    }

    companion object {
        fun scheduleToday(ctx: Context) {
            runCatching {
                val store = Store(ctx)
                val wm = WorkManager.getInstance(ctx)
                wm.cancelAllWorkByTag("sholat")
                val now = Calendar.getInstance()
                val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                // baca prefs sinkron-seperlunya via runBlocking di worker aman; di UI pakai default dulu
                val names = listOf("Subuh", "Zuhur", "Asar", "Magrib", "Isya")
                val t = PrayerCalc.compute(now, PrayerCalc.Params(-6.2, 106.8))
                val map = mapOf("Subuh" to t.subuh, "Zuhur" to t.zuhur, "Asar" to t.asar,
                    "Magrib" to t.magrib, "Isya" to t.isya)
                names.forEach { n ->
                    val d = PrayerCalc.toMinutes(map[n]!!) - nowMin
                    if (d > 1) {
                        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                            .setInitialDelay(d.toLong(), TimeUnit.MINUTES)
                            .setInputData(workDataOf("nama" to n))
                            .addTag("sholat").build()
                        wm.enqueue(req)
                    }
                }
            }
        }
    }
}
