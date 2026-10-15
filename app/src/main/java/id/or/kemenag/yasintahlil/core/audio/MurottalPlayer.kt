package id.or.kemenag.yasintahlil.core.audio

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale

/**
 * Pola URL murottal Surat Yasin per ayat.
 *
 * Dipisah dari [MurottalPlayer] sebagai object murni — tanpa `Context`, tanpa Media3 — supaya
 * polanya dapat diuji langsung di JVM (`MurottalPlayerTest`); kelas pemutarnya tidak bisa,
 * karena ExoPlayer hanya hidup di perangkat.
 */
object MurottalSumber {

    /**
     * URL audio satu ayat Yasin (1..83) pada layanan everyayah.com: nomor surah `036` lalu nomor
     * ayat tiga digit. Padding tiga digit bukan gaya — nama berkas di server memang berpola
     * `036001.mp3` sampai `036083.mp3` — dan `Locale.US` memastikan angkanya selalu digit Latin:
     * pada lokal lain `%d` dapat menghasilkan digit non-ASCII sehingga URL-nya menunjuk berkas
     * yang tidak pernah ada.
     */
    fun url(ayah: Int): String = String.format(
        Locale.US, "https://everyayah.com/data/Alafasy_128kbps/036%03d.mp3", ayah,
    )
}

/**
 * Memutar murottal Surat Yasin satu ayat, atas permintaan pengguna sendiri dari lembar aksi
 * kartu ayatnya.
 *
 * **Mengapa streaming, bukan dibundel.** Aplikasi ini ramping (APK ±2,6 MB) dan offline-first;
 * membundel 83 berkas audio (masing-masing sekitar 1 MB pada 128 kbps) akan melipatgandakan
 * ukurannya demi sesuatu yang hanya diputar bila pengguna memintanya. Sebagai gantinya ayat
 * dialirkan dari everyayah.com — qari Mishary Rashid Alafasy — hanya saat pengguna menekan
 * aksi putarnya. Itulah satu-satunya pemakai izin `INTERNET` di seluruh aplikasi; janji
 * "jaringan hanya untuk murottal" dijaga `OfflineManifestTest`, yang memindai agar domain sumber
 * ini hanya muncul di paket `core.audio`.
 *
 * **Kegagalan jaringan adalah galat biasa.** Aplikasi tidak pernah memeriksa status jaringan
 * (izin `ACCESS_NETWORK_STATE` sengaja tidak diminta); satu-satunya cara tahu sambungan sedang
 * bermasalah adalah pemutaran yang gagal, dan kegagalannya cukup dilaporkan lewat [galat] —
 * sama seperti berkas adzan yang tak terbaca pada [AdzanPreview].
 *
 * **Bedanya dengan [AdzanPlayer]:** adzan adalah pengingat yang menyala sendiri pada waktunya,
 * jadi volumenya tunduk pada `PengingatAdzan.volume` agar tidak mengagetkan; murottal diputar
 * pengguna sendiri atas permintaannya, jadi volumenya penuh — memelankan suara yang barusan
 * diminta justru membingungkan ("kenapa suaranya kecil?").
 *
 * **Semua interaksi ExoPlayer di main thread** (syarat Media3) — pemanggilnya adalah UI Compose
 * yang memang berjalan di sana; pola yang sama dengan [AdzanPlayer] dan [AdzanPreview]. Keadaan
 * sengaja hidup sebagai keadaan Compose supaya layar cukup membacanya tanpa jembatan aliran
 * tambahan, dan memang tidak perlu selamat rotasi: pemutar dilepas saat layar pergi, dan
 * mengulang satu ayat cukup satu ketukan.
 */
class MurottalPlayer(ctx: Context) {

    /** Nomor ayat yang sedang diputar, atau null bila pemutar diam. */
    var ayahDiputar by mutableStateOf<Int?>(null)

    /** True saat audio sedang disiapkan (menyangga unduhan) dan belum berbunyi. */
    var memuat by mutableStateOf(false)

    /** Pesan galat pemutaran terakhir; null bila tidak ada. */
    var galat by mutableStateOf<String?>(null)

    // Context aplikasi — bukan Activity — karena pemutar dibuat malas, bisa lama setelah layar
    // yang memanggilnya pergi; menyimpan Activity di sini akan membocorkannya.
    private val appContext = ctx.applicationContext

    /** Pemutar tunggal, dibuat malas; null sebelum putaran pertama dan setelah [lepas]. */
    private var player: ExoPlayer? = null

    /**
     * Satu instance ExoPlayer untuk seumur layar, dibuat malas pada putaran pertama. Membuat
     * pemutar baru per ayat meninggalkan utas dan penyangga yang harus dibongkar satu per satu;
     * berpindah ayat cukup mengganti butir medianya. Wajib dipanggil di main thread — dan
     * memang demikian, karena satu-satunya pemanggilnya adalah UI Compose.
     */
    private fun pemain(): ExoPlayer {
        player?.let { return it }
        val baru = ExoPlayer.Builder(appContext).build()
        baru.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                // Ayat terputar habis: kembali dianggap diam agar mini-player lenyap dan ketukan
                // berikutnya pada ayat yang sama benar-benar memutar dari awal, bukan menghentikan.
                if (state == Player.STATE_ENDED) resetKeadaan()
            }

            override fun onPlayerError(error: PlaybackException) {
                galat = PESAN_GALAT
                resetKeadaan()
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                if (ayahDiputar != null) memuat = isLoading
            }
        })
        player = baru
        return baru
    }

    /**
     * Putar murottal satu [ayah] (1..83). Menyentuh ayat yang sedang diputar berarti berhenti —
     * satu ketukan untuk menyalakan dan mematikan, dari tempat yang sama. Memutar ayat lain
     * mengganti butir medianya pada pemutar yang sama, tanpa membangun ulang apa pun.
     */
    fun putar(ayah: Int) {
        if (ayah == ayahDiputar) {
            berhenti()
            return
        }
        val p = pemain()
        ayahDiputar = ayah
        galat = null
        memuat = true
        runCatching {
            p.stop()
            p.clearMediaItems()
            p.setMediaItem(MediaItem.fromUri(Uri.parse(MurottalSumber.url(ayah))))
            p.prepare()
            p.play()
        }.onFailure {
            galat = PESAN_GALAT
            resetKeadaan()
        }
    }

    /**
     * Hentikan pemutaran dan kosongkan pesan galat. Tombol "Hentikan" mini-player memakainya
     * baik saat sedang berbunyi maupun saat barisnya hanya menampilkan pesan galat — keduanya
     * memang satu maksud: menutup baris itu.
     */
    fun berhenti() {
        player?.let { p ->
            runCatching {
                p.stop()
                p.clearMediaItems()
            }
        }
        resetKeadaan()
        galat = null
    }

    /**
     * Bebaskan pemutar seluruhnya — dipanggil `onDispose` layar. Berbeda dari [berhenti],
     * `release` membongkar utas dan penyangganya; pemutar yang hanya dihentikan tetap
     * memegangnya selama objek ini hidup.
     */
    fun lepas() {
        player?.let { p -> runCatching { p.release() } }
        player = null
        resetKeadaan()
        galat = null
    }

    /**
     * Kembalikan ke keadaan diam. Tidak menyentuh [galat]: pemanggilnya mungkin baru saja
     * memasang pesan galatnya, dan pesan itu justru yang harus tetap terbaca pengguna.
     */
    private fun resetKeadaan() {
        ayahDiputar = null
        memuat = false
    }

    companion object {
        /**
         * Satu kalimat untuk seluruh kegagalan pemutaran murottal. Karena aplikasi tidak pernah
         * memeriksa status jaringan (lihat KDoc kelas), pesan ini sekaligus menyebut satu-satunya
         * hal yang dapat diperiksa pengguna sendiri: sambungan internetnya.
         */
        private const val PESAN_GALAT =
            "Murottal tidak dapat diputar — periksa sambungan internet"
    }
}
