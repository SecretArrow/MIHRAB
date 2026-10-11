package id.or.kemenag.yasintahlil

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
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
                val legacy = store.get(Store.FAV, emptySet())
                if (legacy.isEmpty()) return@runCatching
                if (userData.favorites().first().isNotEmpty()) return@runCatching
                legacy.forEach { id ->
                    val i = id.indexOf(':')
                    if (i > 0) userData.toggleFavorite(id.substring(0, i), id.substring(i + 1), id)
                }
                store.set(Store.FAV, emptySet())
            }
        }
    }
}

