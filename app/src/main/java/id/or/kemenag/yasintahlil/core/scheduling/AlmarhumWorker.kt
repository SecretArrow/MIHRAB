package id.or.kemenag.yasintahlil.core.scheduling

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.or.kemenag.yasintahlil.MainActivity
import id.or.kemenag.yasintahlil.core.data.local.AlmarhumDao
import id.or.kemenag.yasintahlil.core.data.local.AlmarhumEntity
import id.or.kemenag.yasintahlil.feature.almarhum.AlmarhumHitung
import id.or.kemenag.yasintahlil.navigation.Routes
import java.time.LocalDate

/**
 * Pintu akses Hilt bagi [AlmarhumWorker]. WorkManager membangun pekerjanya lewat `WorkerFactory`
 * sendiri, jadi DAO tidak dapat datang lewat konstruktor; mengambilnya dari grafik singleton
 * lewat EntryPoint menjaga instance Room tetap tunggal (provider `AppModule`), bukan database
 * kedua yang dibangun terpisah — pola yang sama dengan `UserDataEntryPoint` untuk composable.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AlmarhumPintu {
    fun almarhumDao(): AlmarhumDao
}

/**
 * Pemeriksa harian peringatan tahlil almarhum: bila hari ini adalah hari ke-7, ke-40, ke-100,
 * ke-1000, atau haul sejak wafatnya salah satu catatan, kirim notifikasi ke kanal
 * [NotifChannels.ALMARHUM].
 *
 * Pekerjaan ini dijadwalkan periodik (24 jam) oleh `AlmarhumViewModel` dan sengaja **tidak**
 * menjadwalkan ulang dirinya dari `doWork`: pekerjaan periodik WorkManager diulang sendiri oleh
 * sistem selama periodenya — penjadwalan ulang manual dari dalam pekerja justru menumpuk
 * antrean.
 *
 * Selalu `Result.success()`: daftar kosong atau hari tanpa peringatan bukan kegagalan, dan
 * `Result.failure` hanya membuat WorkManager mencoba ulang pekerjaan yang memang tidak ada
 * kerjanya pada hari itu.
 */
class AlmarhumWorker(app: Context, params: WorkerParameters) : CoroutineWorker(app, params) {

    override suspend fun doWork(): Result {
        NotifChannels.ensure(applicationContext)
        val dao = EntryPointAccessors
            .fromApplication(applicationContext, AlmarhumPintu::class.java).almarhumDao()
        val hariIni = LocalDate.now()
        dao.getAll().forEach { baris ->
            val wafat = AlmarhumHitung.dariIso(baris.tanggalWafat)
            if (wafat == null) {
                // Baris rusak dilewati dengan jejak, bukan diam-diam: catatannya tetap tampil di
                // layarnya, dan satu baris yang bermasalah tidak boleh mematikan pemeriksaan
                // baris sesudahnya.
                Log.w(TAG, "tanggal wafat tidak terbaca, catatan dilewati: ${baris.tanggalWafat}")
                return@forEach
            }
            val berikutnya = AlmarhumHitung.berikutnya(wafat, hariIni) ?: return@forEach
            if (berikutnya.tanggal == hariIni) notify(baris, berikutnya, wafat)
        }
        return Result.success()
    }

    /**
     * Intent pembuka layar Tahlil Almarhum — pola `ReminderWorker.contentIntent`: rute dikirim
     * lewat extra `MainActivity.EXTRA_ROUTE`. Kode permintaannya id almarhumnya, bukan angka
     * tetap, supaya notifikasi beberapa almarhum pada hari yang sama tidak saling menimpa
     * intentnya.
     */
    private fun contentIntent(idAlmarhum: Long): PendingIntent {
        val i = Intent(applicationContext, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, Routes.ALMARHUM)
        return PendingIntent.getActivity(
            applicationContext, idAlmarhum.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Notifikasi satu peringatan. Judulnya menyebut namanya; isinya label peringatan dan tanggal
     * wafatnya — bila pengguna mencatat beberapa orang, isi notifikasi tetap cukup untuk tahu
     * untuk siapa tahlil diadakan tanpa membuka aplikasinya. Labelnya dipakai apa adanya
     * ("Hari ke-40 …", "Haul ke-2 …") sehingga notifikasi dan kartu di layar menyebut peringatan
     * yang sama dengan kata yang sama.
     */
    private fun notify(baris: AlmarhumEntity, peringatan: AlmarhumHitung.Peringatan, wafat: LocalDate) {
        val notif = NotificationCompat.Builder(applicationContext, NotifChannels.ALMARHUM)
            .setContentTitle("Tahlil untuk ${baris.nama} hari ini")
            .setContentText(
                "${peringatan.label} sejak wafatnya — ${AlmarhumHitung.tanggalIndo(wafat)}. " +
                    "Semoga Allah merahmatinya.",
            )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentIntent(baris.id))
            .setAutoCancel(true)
            .build()
        // Id dari id almarhumnya: pemeriksaan ulang pada hari yang sama mengganti, bukan menumpuk.
        post((baris.id + ID_NOTIF).toInt(), notif)
    }

    // Notifikasi hanya relevan bila izin notifikasi diberikan; kegagalan diabaikan agar pemeriksaan
    // esok hari tetap berjalan.
    @SuppressLint("MissingPermission", "NotificationPermission")
    private fun post(id: Int, notif: android.app.Notification) {
        val mgr = NotificationManagerCompat.from(applicationContext)
        if (!mgr.areNotificationsEnabled()) return
        runCatching { mgr.notify(id, notif) }
    }

    private companion object {
        /** Penanda `logcat` untuk baris yang dilewati tanpa notifikasi. */
        const val TAG = "AlmarhumWorker"

        /** Pergeser id notifikasi dari id baris almarhumnya agar tak berhimpit dengan id adzan. */
        const val ID_NOTIF = 5_000L
    }
}
