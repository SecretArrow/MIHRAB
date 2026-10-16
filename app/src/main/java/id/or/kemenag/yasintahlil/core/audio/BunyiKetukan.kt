package id.or.kemenag.yasintahlil.core.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Bunyi "beep" ketukan tasbih lewat [ToneGenerator] — tanpa berkas audio, tanpa izin, tanpa
 * jaringan.
 *
 * **Mengapa ToneGenerator, bukan berkas audio atau SoundPool.** Aplikasi ini ramping dan
 * offline-first; menyertakan berkas bunyi berarti menambah berat APK untuk sesuatu yang hanya
 * diputar bila pengguna memilih mode Beep, sementara ToneGenerator menyediakan nada bip pendek
 * bawaan perangkat yang justru terasa seperti ketukan penghitung fisik. Ia memakai jalur
 * `STREAM_MUSIC` yang sama dengan murottal — volume media perangkat tetap berlaku sebagai batas
 * atasnya.
 *
 * **Satu instance untuk seumur layar.** Membuat ToneGenerator per ketukan memakan waktu puluhan
 * milidetik — terlalu lambat untuk ketukan tasbih yang beruntun. Karena itu instance dibuat malas
 * pada bunyi pertama dan dipakai ulang; ia dibangun ulang **hanya** bila volumenya berubah, sebab
 * volume ToneGenerator terpasang saat konstruksi dan tidak dapat diganti pada instance yang ada.
 * [lepas] wajib dipanggil `onDispose` layar — pola yang sama dengan `MurottalPlayer.lepas`.
 *
 * Konstruksi ToneGenerator dapat melempar `RuntimeException` pada sebagian perangkat ketika
 * sumber audionya habis; kegagalan seperti itu dibiarkan sunyi — ketukan yang hilang bunyinya
 * lebih baik daripada aplikasi yang mati.
 */
class BunyiKetukan {

    /** Pemain tunggal, dibuat malas; null sebelum bunyi pertama dan setelah [lepas]. */
    private var pemain: ToneGenerator? = null

    /** Volume yang tertancap pada [pemain] sekarang; -1 berarti belum ada. */
    private var volTercatat: Int = -1

    /**
     * Bunyikan bip pendek pada [volume] persen (0–100). Volume 0 dibiarkan sunyi — memanggilnya
     * dengan senyap hanya membuang kerja membangun pemainnya.
     */
    fun bunyi(volumePersen: Int) {
        val vol = volumePersen.coerceIn(PERSEN_MIN, PERSEN_MAKS)
        if (vol <= PERSEN_MIN) return
        val p = pemain
        if (p != null && volTercatat == vol) {
            jalankan(p)
            return
        }
        // Volume berubah: pemain lama dibongkar dan dibangun ulang dengan volume baru —
        // satu-satunya cara mengganti volume ToneGenerator.
        lepas()
        runCatching {
            ToneGenerator(AudioManager.STREAM_MUSIC, vol).also {
                pemain = it
                volTercatat = vol
            }
        }.getOrNull()?.let { jalankan(it) }
    }

    /** Bebaskan pemain seluruhnya — dipanggil `onDispose` layar Tasbih. */
    fun lepas() {
        pemain?.let { p -> runCatching { p.release() } }
        pemain = null
        volTercatat = -1
    }

    private fun jalankan(p: ToneGenerator) {
        runCatching { p.startTone(ToneGenerator.TONE_PROP_BEEP, DURASI_MS) }
    }

    companion object {
        /** Rentang persen yang diterima — nama sengaja bukan `VOL_MIN`/`VOL_MAKS`: ejaan itu
         *  dipaku penjaga untuk rentang volume adzan saja (`VolumeAdzanSatuRentangTest`). */
        const val PERSEN_MIN = 0
        const val PERSEN_MAKS = 100

        /** Durasi bip satu ketukan, milidetik: cukup terdengar, cukup pendek untuk ketukan cepat. */
        const val DURASI_MS = 60
    }
}
