package id.or.kemenag.yasintahlil.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Mode "Bacakan" kartu bacaan: teksnya (judul + latin + terjemahan) dibacakan suara
 * TextToSpeech bahasa Indonesia — aksesibilitas bagi lansia dan pengguna dengan penglihatan
 * terbatas. Menekan aksinya lagi menghentikan pembacaan.
 *
 * **Kenapa singleton.** Mesin TTS mahal dibangun — mengikat layanan sistem lewat IPC; membangun
 * satu per kartu yang ditekan lama hanya untuk dibuang saat lembar aksinya tertutup adalah
 * pemborosan yang terasa sebagai jeda. Satu instance untuk seluruh proses, dibangun saat pertama
 * kali [bacakan] dipanggil. Saat menganggur mesin TTS nyaris tak memakan sumber daya, dan sistem
 * membubarkannya sendiri saat proses aplikasi mati — karena itu [lepas] **tidak** dipanggil dari
 * komposisi (lembar aksi tidak punya daur hidup yang cocok untuk mematikan mesin yang dipakai
 * seluruh aplikasi).
 *
 * **Kenapa bukan media3.** TTS adalah API kerangka (`android.speech.tts`), bukan pemutar media —
 * berkas ini hanya kebetulan sedang memakai "suara", jadi ia tinggal di paket `core.audio`
 * sesuai pemisahan lapisan audio pada `plan.txt`, tanpa menyeret ExoPlayer.
 *
 * **Benang.** `onInit` datang dari benang yang tidak ditentukan dan balasan kemajuan ucapan
 * (`UtteranceProgressListener`) datang dari benang binder — komentar di AOSP-nya menyebutnya
 * "can be called from multiple threads". `sedangBaca`/`siap`/`galatTts` memakai keadaan snapshot
 * Compose yang memang aman ditulis dari benang mana pun, dan `mesin` ditandai `@Volatile`.
 * Pemanggil tetap bebas memanggil dari benang utama (tempat lambda klik hidup).
 */
object TtsBacakan {

    /** Penanda ucapan pada mesin, agar balasan kemajuannya dapat dihubungkan ke pembacaan ini. */
    private const val ID_UCAPAN = "bacaan"

    @Volatile
    private var mesin: TextToSpeech? = null

    /** Teks yang menunggu mesin selesai dinyalakan — baca pertama kali tak boleh hilang diam-diam. */
    @Volatile
    private var teksTertunda: String? = null

    /** Sedang membacakan suara? Dibaca langsung di komposisi untuk membalik label aksinya. */
    var sedangBaca by mutableStateOf(false)

    /** Mesin sudah menyala dan bahasanya terpasang — siap bicara tanpa jeda lagi. */
    var siap by mutableStateOf(false)

    /** Pembacaan terakhir gagal (mesin/bahasa tak tersedia) — untuk ditampilkan pemanggil. */
    var galatTts by mutableStateOf(false)

    /**
     * Bacakan [teks] — atau hentikan bila sedang membaca (nilai balik `false` berarti "berhenti",
     * bukan gagal; pemanggil boleh mengabaikannya). `false` juga berarti teksnya tidak bisa
     * dibacakan: kosong, bahasa Indonesia tidak tersedia, atau mesinnya menolak.
     *
     * Mesin dibangun malas pada panggilan pertama; karena `onInit` datang belakangan, teksnya
     * dititipkan ke [teksTertunda] dan mulai dibacakan begitu mesin siap — penekan pertama tetap
     * terdengar tanpa harus menekan dua kali. Kontrak konstruktor TTS mengizinkan `onInit` gagal
     * **seketika sebelum konstruktor pulang**; kasus itu hanya mungkin untuk kegagalan, dan
     * tertangani lewat pemeriksaan `galatTts` setelah pembangunan di bawah.
     */
    fun bacakan(ctx: Context, teks: String): Boolean {
        if (teks.isBlank()) return false
        if (sedangBaca) {
            berhenti()
            return false
        }
        galatTts = false
        teksTertunda = teks
        if (mesin == null) {
            mesin = TextToSpeech(ctx.applicationContext, ::saatMesinSiap)
            if (galatTts) {
                // Kegagalan instan: onInit sudah memanggil saatMesinSiap dengan mesin masih null.
                matikanMesin()
                return false
            }
        }
        val siapkan = mesin
        if (siapkan != null && siap) {
            teksTertunda = null
            return mulaiBaca(siapkan, teks)
        }
        // Mesin masih dalam proses menyala — saatMesinSiap meneruskannya begitu siap.
        return true
    }

    /** Hentikan pembacaan yang sedang berjalan (aman dipanggil kapan pun). */
    fun berhenti() {
        mesin?.stop()
        sedangBaca = false
    }

    /**
     * Matikan dan buang mesinnya. Karena mesin ini milik seluruh proses (lihat KDoc objek),
     * fungsi ini bukan untuk dipanggil dari komposisi — cukup tersedia untuk penghentian yang
     * memang disengaja, mis. saat pengguna mematikan Mode Bacakan dari Pengaturan.
     */
    fun lepas() {
        matikanMesin()
        sedangBaca = false
        siap = false
        teksTertunda = null
    }

    /**
     * Susun teks yang dibacakan dari kartu bacaan: judul, latin, lalu terjemahan, disambung
     * titik. Teks Arab sengaja **tidak** ikut: mesin TTS umumnya tidak bisa membaca teks Arab
     * berharakat dengan benar — hasilnya sekadar bunyi menggerutu yang justru mengganggu; latin
     * (transliterasi) sudah menjadi jalur bacanya. Bagian ini murni agar susunannya bisa diuji.
     */
    fun susunTeksBacaan(judul: String, latin: String, terjemahan: String): String {
        val potongan = mutableListOf(judul)
        if (latin.isNotBlank()) potongan += latin
        if (terjemahan.isNotBlank()) potongan += terjemahan
        return potongan.joinToString(". ")
    }

    private fun saatMesinSiap(status: Int) {
        val siapkan = mesin ?: run {
            // Dipanggil sebelum assignment di bacakan() selesai — kontraknya hanya terjadi pada
            // kegagalan instan, jadi cukup tandai galatnya; bacakan() mematikan mesinnya nanti.
            if (status != TextToSpeech.SUCCESS) galatTts = true
            return
        }
        if (status != TextToSpeech.SUCCESS) {
            galatTts = true
            teksTertunda = null
            matikanMesin()
            return
        }
        var hasil = siapkan.setLanguage(Locale("id", "ID"))
        if (hasil == TextToSpeech.LANG_MISSING_DATA || hasil == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Beberapa mesin hanya mengenal bahasanya tanpa pasangan negaranya.
            hasil = siapkan.setLanguage(Locale("id"))
        }
        if (hasil == TextToSpeech.LANG_MISSING_DATA || hasil == TextToSpeech.LANG_NOT_SUPPORTED) {
            galatTts = true
            teksTertunda = null
            matikanMesin()
            return
        }
        pasangPendengar(siapkan)
        siap = true
        teksTertunda?.let {
            teksTertunda = null
            mulaiBaca(siapkan, it)
        }
    }

    private fun mulaiBaca(mesin: TextToSpeech, teks: String): Boolean {
        // Batas panjang ucapan mesin dijaga: teks yang lebih panjang dari kemampuannya membuatnya
        // diam sama sekali, bukan terpotong — kartu bacaan memang pendek, tetapi menjepitnya di
        // sini lebih murah daripada membiarkan satu kartu panjang merusak seluruh fitur.
        val ucap = teks.take(TextToSpeech.getMaxSpeechInputLength())
        return if (mesin.speak(ucap, TextToSpeech.QUEUE_FLUSH, null, ID_UCAPAN) == TextToSpeech.SUCCESS) {
            sedangBaca = true
            true
        } else {
            sedangBaca = false
            galatTts = true
            false
        }
    }

    private fun pasangPendengar(mesin: TextToSpeech) {
        mesin.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            // Datang dari benang binder (lihat KDoc objek) — keadaan snapshot aman dari mana saja.
            override fun onStart(utteranceId: String?) {
                sedangBaca = true
            }

            override fun onDone(utteranceId: String?) {
                sedangBaca = false
            }

            @Suppress("DEPRECATION") // anggota abstrak lawas; galat sesungguhnya ditangani onError(String, Int)
            override fun onError(utteranceId: String?) {
                sedangBaca = false
            }

            override fun onError(utteranceId: String?, kodeGalat: Int) {
                sedangBaca = false
                galatTts = true
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                sedangBaca = false
            }
        })
    }

    private fun matikanMesin() {
        mesin?.shutdown()
        mesin = null
    }
}
