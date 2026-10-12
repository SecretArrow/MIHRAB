package id.or.kemenag.yasintahlil.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Memutar berkas adzan pilihan pengguna satu kali lalu berhenti.
 * Aplikasi TIDAK menyertakan audio adzan bawaan (lisensi) — berkas dipilih pengguna di menu Adzan.
 * Semua interaksi ExoPlayer dilakukan di main thread (syarat Media3).
 *
 * Berkas ini berada di paket `core.audio`, bukan `core.scheduling`: `plan.txt` §"Teknologi dan
 * Arsitektur" meminta *"arsitektur modular yang memisahkan UI, domain, data, penjadwalan, audio,
 * lokasi, dan sensor"*. Penjadwalan (kapan berbunyi) dan pemutaran (bagaimana berbunyi) berubah
 * karena alasan yang berbeda — menyatukannya membuat perubahan lisensi audio menyentuh kode
 * penjadwalan, dan sebaliknya.
 */
object AdzanPlayer {
    suspend fun playOnce(ctx: Context, uri: String, volume: Float, maxMillis: Long = 300_000): Boolean {
        if (uri.isBlank()) return false
        return withContext(Dispatchers.Main) {
            withTimeoutOrNull(maxMillis) {
                val player = ExoPlayer.Builder(ctx.applicationContext).build()
                try {
                    player.volume = volume.coerceIn(0f, 1f)
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                    player.prepare()
                    suspendCancellableCoroutine<Unit> { cont ->
                        val listener = object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_ENDED && cont.isActive) cont.resume(Unit)
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                        }
                        player.addListener(listener)
                        player.play()
                        cont.invokeOnCancellation { runCatching { player.stop() } }
                    }
                    true
                } catch (_: Exception) {
                    false
                } finally {
                    runCatching { player.release() }
                }
            } ?: false
        }
    }
}
