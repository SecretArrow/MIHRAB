package id.or.kemenag.yasintahlil

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Titik masuk aplikasi + penyedia konfigurasi WorkManager yang mendukung injeksi Hilt. */
@HiltAndroidApp
class YasinApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var store: Store
    @Inject lateinit var userData: UserDataRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        migrateLegacyFavorites()
    }

    /** Sekali saja: pindahkan favorit warisan (DataStore) ke Room, lalu bersihkan penyimpanan lama. */
    private fun migrateLegacyFavorites() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val legacy = store.get(Store.FAV, BawaanPreferensi.FAVORIT)
                if (legacy.isEmpty()) return@runCatching
                if (userData.favorites().first().isNotEmpty()) return@runCatching
                legacy.forEach { id ->
                    // Pemisahnya dibaca lewat kosakata bersama, sama dengan penulis id di layar.
                    val (jenis, rujukan) = JenisBacaan.pisah(id)
                    // Id warisan yang tidak berbentuk `jenis:rujukan` dilewati: jenisnya jatuh ke
                    // `JenisBacaan.LAIN` yang sengaja tidak punya layar tujuan, jadi menyimpannya
                    // hanya menghasilkan baris favorit yang tak dapat dibuka.
                    if (jenis != JenisBacaan.LAIN) userData.toggleFavorite(jenis, rujukan, id)
                }
                store.set(Store.FAV, BawaanPreferensi.FAVORIT)
            }
        }
    }
}
