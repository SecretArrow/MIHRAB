package id.or.kemenag.yasintahlil.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Menyusun ulang pengingat waktu sholat setelah perangkat menyala ulang (rantai WorkManager hilang saat reboot). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerScheduler.scheduleAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
