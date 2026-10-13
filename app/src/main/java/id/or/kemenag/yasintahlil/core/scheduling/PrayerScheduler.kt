package id.or.kemenag.yasintahlil.core.scheduling

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.ReminderWorker
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import java.util.Calendar
import java.util.Locale
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

    /**
     * Lima waktu beradzan, urutannya sama dengan CSV `Store.ADZAN_ON` dan dengan
     * [JadwalHariIni.adzan] — lihat [JadwalHariIni.LIMA_WAKTU]. Daftar ini dahulu ditulis ulang di
     * sini dan di layar Adzan; salinan yang berbeda urutan akan mematikan waktu yang salah.
     */
    private val NAMES = JadwalHariIni.LIMA_WAKTU

    /** Jenis pekerjaan: adzan (waktu masuk), pra-adzan (pengingat sebelum), doa & iqomah (sesudah). */
    const val JENIS_ADZAN = "adzan"
    const val JENIS_PRA = "pra"
    const val JENIS_DOA = "doa"
    const val JENIS_IQOMAH = "iqomah"

    private data class Cfg(
        /**
         * Parameter hisab yang sudah jadi, bukan potongan-potongan nilai mentah.
         *
         * Dahulu `Cfg` menyimpan `lat`/`lon`/`method`/`asarFactor`/`corr`/`tz` terpisah lalu
         * menyusunnya kembali menjadi `PrayerCalc.Params` di tempat pemakaian. Menyusunnya sekali di
         * sini membuat pengingat memakai **persis** aturan yang sama dengan layar Jadwal, Beranda,
         * dan menu Adzan — bukan salinan yang bisa berbeda diam-diam.
         */
        val params: PrayerCalc.Params,
        val on: List<Boolean>, val preMin: Int, val doaReminder: Boolean,
        val iqomahOn: Boolean, val iqomahMin: Int,
    )

    private suspend fun loadCfg(ctx: Context): Cfg {
        val store = Store(ctx)
        val tz = JadwalHariIni.zona(store.get(Store.TZ, JadwalHariIni.TZ_DEFAULT))
        return Cfg(
            params = JadwalHariIni.params(
                lat = store.get(Store.LAT, JadwalHariIni.LAT_DEFAULT),
                lon = store.get(Store.LON, JadwalHariIni.LON_DEFAULT),
                methodKey = store.get(Store.METHOD, JadwalHariIni.METODE_DEFAULT),
                asarKey = store.get(Store.ASAR, JadwalHariIni.ASAR_DEFAULT),
                corrCsv = store.get(Store.CORR, JadwalHariIni.CORR_DEFAULT),
                tz = tz,
            ),
            on = PengingatAdzan.parseOn(store.get(Store.ADZAN_ON, PengingatAdzan.ON_DEFAULT)),
            preMin = PengingatAdzan.preMenit(
                store.get(Store.ADZAN_PRE_MIN, PengingatAdzan.PRE_MIN_DEFAULT)),
            doaReminder = store.get(Store.DOA_ADZAN_REMINDER, BawaanPreferensi.PENGINGAT_DOA_ADZAN),
            iqomahOn = store.get(Store.IQOMAH_ON, BawaanPreferensi.PENGINGAT_IQOMAH),
            iqomahMin = PengingatAdzan.iqomahMenit(
                store.get(Store.IQOMAH_MIN, PengingatAdzan.IQOMAH_DEFAULT_MENIT)),
        )
    }

    /** Susun ulang SELURUH pengingat dari preferensi tersimpan. Panggil saat app dibuka, setelah reboot, atau usai ubah pengaturan. */
    suspend fun scheduleAll(ctx: Context) {
        val store = Store(ctx)
        WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG)
        if (!store.get(Store.NOTIF, BawaanPreferensi.PENGINGAT_WAKTU)) return
        val cfg = loadCfg(ctx)
        NAMES.forEachIndexed { i, n -> if (cfg.on.getOrElse(i) { true }) enqueueNext(ctx, cfg, n) }
    }

    /** Jadwalkan kemunculan berikutnya untuk satu waktu sholat. Dipakai pekerja setelah ia berjalan. */
    suspend fun rescheduleOne(ctx: Context, nama: String) {
        val store = Store(ctx)
        if (!store.get(Store.NOTIF, BawaanPreferensi.PENGINGAT_WAKTU)) return
        val idx = NAMES.indexOf(nama)
        if (idx < 0) return
        val cfg = loadCfg(ctx)
        if (!cfg.on.getOrElse(idx) { true }) return
        enqueueNext(ctx, cfg, nama)
    }

    private fun enqueueNext(ctx: Context, cfg: Cfg, nama: String) {
        val now = Calendar.getInstance(cfg.params.tz)
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val times = PrayerCalc.compute(now, cfg.params)
        // Pasangan nama→jam diturunkan dari daftar nama yang sama dengan yang mengindeks
        // `cfg.on`: menulisnya manual di sini berarti salinan kedua yang dapat berbeda urutan.
        val map = NAMES.zip(JadwalHariIni.adzan(times)).toMap()
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
            enqueue(ctx, "doa_${nama}_$hari", nama, (delta + PengingatAdzan.DOA_DELAY_MIN).toLong(), JENIS_DOA)
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
            // Kunci data masukan datang dari kosakata bersama ([PengingatAdzan]); mengejanya di sini
            // membuat sisi penulis dan sisi pembacanya dapat berbeda tanpa satu pun galat.
            .setInputData(workDataOf(
                PengingatAdzan.KUNCI_NAMA to nama,
                PengingatAdzan.KUNCI_JENIS to jenis,
            ))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(unique, ExistingWorkPolicy.REPLACE, req)
    }
}
