package id.or.kemenag.yasintahlil.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan

/**
 * Pemutar pratinjau berkas adzan pilihan pengguna (plan.txt §"Fitur Adzan": *"Tombol pratinjau
 * audio"*).
 *
 * Dipisahkan dari layar agar **tidak ada pemakaian ExoPlayer langsung di lapisan UI**: `plan.txt`
 * §"Teknologi dan Arsitektur" meminta lapisan audio berdiri sendiri. Layar cukup memanggil
 * [play], [stop], dan [setVolume]; bila berkasnya tak terbaca (URI dicabut, format tak didukung,
 * izin hilang) sebabnya diteruskan lewat [onError] supaya layar dapat menjelaskannya — sebelumnya
 * pratinjau diam saja saat gagal.
 *
 * Semua metode harus dipanggil dari main thread (syarat Media3); Compose sudah menjalankannya
 * di sana.
 */
class AdzanPreview(ctx: Context, private val onError: (String) -> Unit) {

    private val player = ExoPlayer.Builder(ctx.applicationContext).build()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onError(
                    "Berkas audio tidak dapat diputar (${error.errorCodeName}). Pilih berkas lain " +
                        "atau pastikan aplikasi masih punya izin membaca berkas itu."
                )
            }
        })
    }

    /** Putar [uri] dari awal pada [volume]. Mengembalikan false bila [uri] kosong. */
    fun play(uri: String, volume: Float): Boolean {
        if (uri.isBlank()) return false
        runCatching {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            player.volume = PengingatAdzan.volume(volume)
            player.prepare()
            player.play()
        }.onFailure { onError("Berkas audio tidak dapat diputar: ${it.message ?: it::class.simpleName}.") }
        return true
    }

    fun stop() { runCatching { player.stop() } }

    fun setVolume(v: Float) {
        runCatching { player.volume = PengingatAdzan.volume(v) }
    }

    fun release() { runCatching { player.release() } }
}
