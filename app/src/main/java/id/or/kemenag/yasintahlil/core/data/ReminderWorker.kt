package id.or.kemenag.yasintahlil.core.data

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import id.or.kemenag.yasintahlil.MainActivity
import id.or.kemenag.yasintahlil.core.scheduling.AdzanPlayer
import id.or.kemenag.yasintahlil.core.scheduling.NotifChannels
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler

/**
 * Pengingat satu waktu sholat. Membaca lokasi/metode/koreksi/zona waktu dari [Store] (bukan hardcode),
 * menghormati saklar per-waktu, memutar audio adzan pilihan pengguna di latar (foreground service media),
 * lalu menjadwalkan kemunculan berikutnya lewat [PrayerScheduler].
 */
class ReminderWorker(app: Context, params: WorkerParameters) : CoroutineWorker(app, params) {

    override suspend fun doWork(): Result {
        val nama = inputData.getString("nama") ?: return Result.success()
        val isAdzan = inputData.getBoolean("adzan", true)
        val store = Store(applicationContext)
        if (!store.get(Store.NOTIF, true)) return Result.success()

        val dnd = store.get(Store.DND_OVERRIDE, false)
        NotifChannels.ensure(applicationContext, dnd)

        val uri = store.get(Store.ADZAN_URI, "")
        val vol = store.get(Store.ADZAN_VOL, 0.8f)
        val preMin = store.get(Store.ADZAN_PRE_MIN, 0)

        if (!isAdzan) {
            notifyPre(nama, preMin)
            PrayerScheduler.rescheduleOne(applicationContext, nama)
            return Result.success()
        }

        val playAudio = uri.isNotBlank()
        if (playAudio) {
            runCatching { setForeground(foregroundInfo(nama)) }
        }
        notifyAdzan(nama, playAudio)
        if (playAudio) {
            AdzanPlayer.playOnce(applicationContext, uri, vol)
        }
        PrayerScheduler.rescheduleOne(applicationContext, nama)
        return Result.success()
    }

    private fun contentIntent(): PendingIntent {
        val i = Intent(applicationContext, MainActivity::class.java)
        return PendingIntent.getActivity(
            applicationContext, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun foregroundInfo(nama: String): ForegroundInfo {
        val notif = NotificationCompat.Builder(applicationContext, NotifChannels.ADZAN_SILENT)
            .setContentTitle("Adzan $nama")
            .setContentText("Sedang memutar adzan…")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(nama.hashCode(), notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            ForegroundInfo(nama.hashCode(), notif)
        }
    }

    private fun notifyAdzan(nama: String, playAudio: Boolean) {
        // Bila audio adzan diputar lewat ExoPlayer, pakai kanal senyap agar suara tidak ganda.
        val channel = if (playAudio) NotifChannels.ADZAN_SILENT else NotifChannels.ADZAN
        val notif = NotificationCompat.Builder(applicationContext, channel)
            .setContentTitle("Waktu $nama telah tiba")
            .setContentText("Segerakan sholat. Jadwal dihitung di perangkat (metode Kemenag/SIHAT).")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentIntent())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        post(nama.hashCode() + 1000, notif)
    }

    private fun notifyPre(nama: String, preMin: Int) {
        val notif = NotificationCompat.Builder(applicationContext, NotifChannels.PRE)
            .setContentTitle("$preMin menit lagi $nama")
            .setContentText("Bersiap untuk sholat $nama.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .build()
        post(nama.hashCode() + 2000, notif)
    }

    // Notifikasi hanya relevan bila izin notifikasi diberikan; kegagalan diabaikan agar pengingat lain tetap jalan.
    @SuppressLint("MissingPermission")
    private fun post(id: Int, notif: android.app.Notification) {
        val mgr = NotificationManagerCompat.from(applicationContext)
        if (!mgr.areNotificationsEnabled()) return
        runCatching { mgr.notify(id, notif) }
    }
}
