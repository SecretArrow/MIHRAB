package id.or.kemenag.yasintahlil

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import id.or.kemenag.yasintahlil.core.data.local.MigrasiWarisan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Titik masuk aplikasi + penyedia konfigurasi WorkManager yang mendukung injeksi Hilt. */
@HiltAndroidApp
class YasinApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var migrasiWarisan: MigrasiWarisan

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Sekali saja saat aplikasi mulai: pindahkan data warisan (DataStore) ke Room. Pemindahnya
        // tinggal di `MigrasiWarisan` karena layar Pengaturan juga memanggilnya setelah memulihkan
        // backup lama yang hanya memuat preferensi.
        CoroutineScope(Dispatchers.IO).launch { migrasiWarisan.jalankan() }
    }
}
