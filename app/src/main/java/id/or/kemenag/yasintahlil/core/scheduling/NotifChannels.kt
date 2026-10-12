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
 * - [PRE]: pengingat di sekitar waktu sholat yang tidak perlu bersuara keras — sebelum adzan,
 *   ajakan membaca doa setelah adzan, dan perkiraan waktu iqomah habis.
 *
 * Catatan DND: penembusan mode Jangan Ganggu (DND) TIDAK dapat diatur aplikasi lewat API publik
 * (`setBypassDnd` adalah API sistem). Aplikasi hanya dapat meminta akses kebijakan notifikasi
 * ([canBypassDnd] memeriksa statusnya); pengecualian sebenarnya ditetapkan pengguna di Pengaturan
 * sistem. Kanal tetap dibuat dengan IMPORTANCE_HIGH + kategori ALARM agar bila dikecualikan,
 * notifikasi dapat berbunyi.
 */
object NotifChannels {
    const val ADZAN = "adzan"
    const val ADZAN_SILENT = "adzan_silent"
    const val PRE = "preadzan"

    fun ensure(ctx: Context) {
        val mgr = ctx.getSystemService<NotificationManager>() ?: return
        val adzan = NotificationChannel(ADZAN, "Adzan", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pemberitahuan masuk waktu sholat"
            enableVibration(true)
        }
        val silent = NotificationChannel(ADZAN_SILENT, "Adzan (audio sendiri)", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Ditampilkan saat aplikasi memutar berkas adzan pilihan Anda"
            setSound(null, null)
            enableVibration(false)
        }
        val pre = NotificationChannel(PRE, "Sebelum/sesudah adzan", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Pengingat beberapa menit sebelum waktu sholat, pengingat membaca doa " +
                "setelah adzan, dan perkiraan waktu iqomah habis"
        }
        mgr.createNotificationChannels(listOf(adzan, silent, pre))
    }

    /** Apakah pengguna telah memberi aplikasi akses kebijakan notifikasi (dapat dikecualikan dari DND). */
    fun canBypassDnd(ctx: Context): Boolean {
        val mgr = ctx.getSystemService<NotificationManager>() ?: return false
        return runCatching { mgr.isNotificationPolicyAccessGranted }.getOrDefault(false)
    }
}
