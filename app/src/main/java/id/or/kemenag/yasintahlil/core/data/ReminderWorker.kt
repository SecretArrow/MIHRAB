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
import id.or.kemenag.yasintahlil.core.audio.AdzanPlayer
import id.or.kemenag.yasintahlil.core.scheduling.NotifChannels
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Pengingat satu waktu sholat. Membaca lokasi/metode/koreksi/zona waktu dari [Store] (bukan hardcode),
 * menghormati saklar per-waktu, memutar audio adzan pilihan pengguna di latar (foreground service media),
 * lalu menjadwalkan kemunculan berikutnya lewat [PrayerScheduler].
 *
 * Empat jenis pekerjaan: [PrayerScheduler.JENIS_ADZAN] (waktu masuk), [PrayerScheduler.JENIS_PRA]
 * (pengingat sebelum waktu), [PrayerScheduler.JENIS_DOA] (pengingat membaca doa setelah adzan), dan
 * [PrayerScheduler.JENIS_IQOMAH] (perkiraan waktu iqomah telah lewat).
 */
class ReminderWorker(app: Context, params: WorkerParameters) : CoroutineWorker(app, params) {

    override suspend fun doWork(): Result {
        val nama = inputData.getString("nama") ?: return Result.success()
        // Pekerjaan lama (dari versi sebelumnya) hanya membawa "adzan"; dibaca sebagai cadangan.
        val jenis = inputData.getString("jenis")
            ?: if (inputData.getBoolean("adzan", true)) PrayerScheduler.JENIS_ADZAN else PrayerScheduler.JENIS_PRA
        val store = Store(applicationContext)
        if (!store.get(Store.NOTIF, true)) return Result.success()

        NotifChannels.ensure(applicationContext)

        if (jenis == PrayerScheduler.JENIS_PRA) {
            notifyPre(nama, store.get(Store.ADZAN_PRE_MIN, 0))
            PrayerScheduler.rescheduleOne(applicationContext, nama)
            return Result.success()
        }
        if (jenis == PrayerScheduler.JENIS_DOA) {
            // Pengingat ini hanya masuk akal bila saklarnya masih menyala saat pekerjaan berjalan.
            if (store.get(Store.DOA_ADZAN_REMINDER, false)) notifyDoa(nama)
            return Result.success()
        }
        if (jenis == PrayerScheduler.JENIS_IQOMAH) {
            // Sama: saklar dapat dimatikan antara penjadwalan dan saat pekerjaan berjalan.
            if (store.get(Store.IQOMAH_ON, false)) {
                // Panjang timer dibaca lewat aturan bersama: angka yang tertulis di notifikasi harus
                // sama dengan yang dihitung penjadwal dan yang ditampilkan layar Iqomah.
                notifyIqomah(nama, PengingatAdzan.iqomahMenit(
                    store.get(Store.IQOMAH_MIN, PengingatAdzan.IQOMAH_DEFAULT_MENIT)))
            }
            return Result.success()
        }

        val uri = store.get(Store.ADZAN_URI, "")
        val vol = store.get(Store.ADZAN_VOL, PengingatAdzan.VOL_DEFAULT)
        val playAudio = uri.isNotBlank()
        if (playAudio) {
            runCatching { setForeground(foregroundInfo(nama)) }
        }
        notifyAdzan(nama, playAudio, PrayerCalc.Method.from(store.get(Store.METHOD, "KEMENAG")).label)
        if (playAudio) {
            AdzanPlayer.playOnce(applicationContext, uri, vol)
        }
        PrayerScheduler.rescheduleOne(applicationContext, nama)
        return Result.success()
    }

    /** Intent pembuka aplikasi; [rute] membuka layar tertentu (lihat `Routes.LANGSUNG`). */
    private fun contentIntent(rute: String? = null): PendingIntent {
        val i = Intent(applicationContext, MainActivity::class.java)
        if (rute != null) i.putExtra(MainActivity.EXTRA_ROUTE, rute)
        return PendingIntent.getActivity(
            applicationContext, rute?.hashCode() ?: 0, i,
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

    private fun notifyAdzan(nama: String, playAudio: Boolean, metode: String) {
        // Bila audio adzan diputar lewat ExoPlayer, pakai kanal senyap agar suara tidak ganda.
        val channel = if (playAudio) NotifChannels.ADZAN_SILENT else NotifChannels.ADZAN
        val notif = NotificationCompat.Builder(applicationContext, channel)
            .setContentTitle("Waktu $nama telah tiba")
            // Nama metode dibaca dari preferensi: menyebut "Kemenag/SIHAT" secara tetap akan
            // berbohong begitu pengguna memilih metode lain di menu Jadwal.
            .setContentText("Segerakan sholat. Jadwal dihitung di perangkat dengan metode $metode.")
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

    /**
     * Pengingat membaca doa setelah adzan. Ketukannya membuka layar Doa Adzan, bukan hanya beranda,
     * supaya pengingat ini benar-benar mengantar ke bacaannya.
     */
    private fun notifyDoa(nama: String) {
        val notif = NotificationCompat.Builder(applicationContext, NotifChannels.PRE)
            .setContentTitle("Baca doa setelah adzan $nama")
            .setContentText("Sholawat lalu doa setelah adzan. Ketuk untuk membuka bacaannya.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentIntent(Routes.DOA_ADZAN))
            .setAutoCancel(true)
            .build()
        post(nama.hashCode() + 3000, notif)
    }

    /**
     * Pengingat bahwa perkiraan waktu iqomah telah lewat. Ketukannya membuka layar Iqomah.
     *
     * Teksnya sengaja menyebut "perkiraan" dan jumlah menit yang dipilih pengguna: waktu iqomah
     * berbeda di tiap masjid, jadi notifikasi ini tidak boleh terdengar seperti ketentuan syariat
     * atau jadwal masjid tempat pengguna sholat.
     */
    private fun notifyIqomah(nama: String, menit: Int) {
        val notif = NotificationCompat.Builder(applicationContext, NotifChannels.PRE)
            .setContentTitle("Perkiraan waktu iqomah $nama")
            .setContentText("Sekitar $menit menit setelah adzan $nama. Waktu iqomah tiap masjid " +
                "berbeda — ini pengingat pribadi.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentIntent(Routes.IQOMAH))
            .setAutoCancel(true)
            .build()
        post(nama.hashCode() + 4000, notif)
    }

    // Notifikasi hanya relevan bila izin notifikasi diberikan; kegagalan diabaikan agar pengingat lain tetap jalan.
    @SuppressLint("MissingPermission", "NotificationPermission")
    private fun post(id: Int, notif: android.app.Notification) {
        val mgr = NotificationManagerCompat.from(applicationContext)
        if (!mgr.areNotificationsEnabled()) return
        runCatching { mgr.notify(id, notif) }
    }
}
