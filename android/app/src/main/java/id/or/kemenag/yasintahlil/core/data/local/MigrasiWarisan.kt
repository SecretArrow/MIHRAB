package id.or.kemenag.yasintahlil.core.data.local

import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.KunciProgres
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pemindah **data warisan** dari DataStore ke Room.
 *
 * v1.0.0 menyimpan tiga hal di DataStore — favorit, riwayat bacaan, dan posisi Tahlil — sedangkan
 * ketiganya kini ada di Room. Dahulu hanya favorit yang dipindahkan; riwayat dan posisi Tahlil
 * tertinggal. Kerusakannya sunyi: datanya masih ada di perangkat, tetapi tak satu pun layar
 * membacanya lagi, sehingga pengguna yang memperbarui aplikasi hanya melihat riwayatnya kosong dan
 * Tahlil-nya kembali ke bagian pertama — tanpa galat, tanpa cara mengetahui bahwa datanya
 * sebenarnya belum hilang.
 *
 * Ada **dua** jalan masuknya data warisan ke DataStore, dan keduanya harus memanggil pemindah ini:
 *
 * - **Pembaruan aplikasi** — kunci warisan sudah ada di perangkat sejak v1.0.0
 *   ([YasinApp][id.or.kemenag.yasintahlil.YasinApp] memanggilnya saat `onCreate`).
 * - **Pemulihan backup lama** — berkas cadangan v1.0.0 hanya memuat preferensi, jadi
 *   `Store.importJson` menulis ketiga kunci itu kembali ke DataStore ([jalankan] dipanggil lagi
 *   sesudahnya oleh layar Pengaturan).
 *
 * Sebelumnya pemindahnya tinggal di dalam `YasinApp` sebagai fungsi privat, sehingga jalan kedua
 * tak dapat memakainya: memulihkan backup v1.0.0 "berhasil" tanpa galat, tetapi favorit, riwayat,
 * dan posisi Tahlil pengguna tak pernah muncul — kekeliruan yang sama, lewat pintu yang lain.
 *
 * Ketiganya dipindahkan dengan aturan yang sama: hanya bila sisi Room masih kosong, lalu kuncinya
 * dikosongkan agar tidak dipindahkan dua kali. Karena itu [jalankan] aman dipanggil berkali-kali.
 */
@Singleton
class MigrasiWarisan @Inject constructor(
    private val store: Store,
    private val userData: UserDataRepository,
) {

    /**
     * Pindahkan seluruh data warisan. Aman dipanggil berulang kali dan tidak pernah melempar:
     * kegagalan satu bagian tidak boleh menggagalkan pemindahan bagian lain, dan pemanggilnya
     * berjalan di latar saat aplikasi mulai.
     */
    suspend fun jalankan() {
        runCatching {
            favorit()
            riwayat()
            posisiTahlil()
        }
    }

    /** Favorit warisan: himpunan id `jenis:rujukan`. */
    private suspend fun favorit() {
        val legacy = store.get(Store.FAV, BawaanPreferensi.FAVORIT)
        if (legacy.isEmpty()) return
        if (userData.favorites().first().isNotEmpty()) return
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

    /** Satu entri riwayat warisan: `jenis:rujukan|waktu`. */
    private data class RiwayatWarisan(
        val jenis: String,
        val rujukan: String,
        val judul: String,
        val waktu: Long,
    )

    /**
     * Riwayat warisan berbentuk `jenis:rujukan|waktu;…`, **terbaru lebih dahulu** (penulisnya
     * menyisipkan di depan). Judulnya dipakai apa adanya — id lamanya — karena itulah yang
     * ditampilkan layar versi lama: memindahkan data tidak boleh mengubah apa yang dilihat pengguna.
     * Waktunya ikut dipindahkan supaya urutannya tetap sama seperti sebelumnya.
     */
    private suspend fun riwayat() {
        val legacy = store.get(Store.HIST, BawaanPreferensi.RIWAYAT)
        if (legacy.isBlank()) return
        if (userData.recentHistory(1).first().isNotEmpty()) return
        val entri = legacy.split(';').filter { it.isNotBlank() }.mapNotNull { baris ->
            val id = baris.substringBefore('|')
            val waktu = baris.substringAfter('|', "").toLongOrNull() ?: System.currentTimeMillis()
            val (jenis, rujukan) = JenisBacaan.pisah(id)
            if (jenis == JenisBacaan.LAIN) null else RiwayatWarisan(jenis, rujukan, id, waktu)
        }
        // Paling lama lebih dahulu, searah dengan waktunya.
        entri.asReversed().forEach { userData.pushHistory(it.jenis, it.rujukan, it.judul, it.waktu) }
        store.set(Store.HIST, BawaanPreferensi.RIWAYAT)
    }

    /**
     * Posisi Tahlil warisan berbentuk `posisi:hitungan` — **persis** dua angka yang kini disimpan
     * sebagai baris progres tersendiri ([KunciProgres.TAHLIL_POSISI] dan
     * [KunciProgres.TAHLIL_HITUNGAN]), jadi pemindahannya satu lawan satu dan tidak menebak apa pun.
     * Bentuk nilainya dibaca lewat [KunciProgres.bacaWarisan], bukan diurai sendiri di sini, supaya
     * pemisahnya tidak pernah berbeda dari yang dulu menuliskannya.
     */
    private suspend fun posisiTahlil() {
        val legacy = store.get(Store.TAHLIL_PROG, BawaanPreferensi.PROGRES_TAHLIL)
        if (legacy.isBlank()) return
        if (userData.progress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_POSISI).first() != null) return
        val (posisi, hitungan) = KunciProgres.bacaWarisan(legacy) ?: return
        userData.setProgress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_POSISI, posisi)
        userData.setProgress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_HITUNGAN, hitungan)
        store.set(Store.TAHLIL_PROG, BawaanPreferensi.PROGRES_TAHLIL)
    }
}
