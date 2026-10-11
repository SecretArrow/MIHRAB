package id.or.kemenag.yasintahlil.core.scheduling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * Kanal notifikasi aplikasi.
 * - [ADZAN]: pemberitahuan masuk waktu sholat (penting, bersuara) — dipakai bila tidak ada berkas adzan.
 * - [ADZAN_SILENT]: kanal senyap untuk notifikasi foreground saat audio adzan diputar lewat ExoPlayer
 *   (mencegah suara ganda).
 * - [PRE]: pengingat beberapa menit sebelum waktu sholat (tanpa suara keras).
 */
object NotifChannels {
    const val ADZAN = "adzan"
    const val ADZAN_SILENT = "adzan_silent"
    const val PRE = "preadzan"

    fun ensure(ctx: Context, bypassDnd: Boolean = false) {
        val mgr = ctx.getSystemService<NotificationManager>() ?: return
        val adzan = NotificationChannel(ADZAN, "Adzan", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pemberitahuan masuk waktu sholat"
            enableVibration(true)
            this.bypassDnd = bypassDnd
        }
        val silent = NotificationChannel(ADZAN_SILENT, "Adzan (audio sendiri)", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Ditampilkan saat aplikasi memutar berkas adzan pilihan Anda"
            setSound(null, null)
            enableVibration(false)
        }
        val pre = NotificationChannel(PRE, "Sebelum adzan", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Pengingat beberapa menit sebelum waktu sholat"
        }
        mgr.createNotificationChannels(listOf(adzan, silent, pre))
    }
}
