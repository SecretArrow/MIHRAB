package id.or.kemenag.yasintahlil.core.domain.repository

import id.or.kemenag.yasintahlil.core.domain.model.BookmarkItem
import id.or.kemenag.yasintahlil.core.domain.model.FavItem
import id.or.kemenag.yasintahlil.core.domain.model.HistItem
import id.or.kemenag.yasintahlil.core.domain.model.ProgressItem
import id.or.kemenag.yasintahlil.core.domain.model.RingkasanData
import kotlinx.coroutines.flow.Flow

/** Akses data terstruktur pengguna (favorit, bookmark, riwayat, progres). */
interface UserDataRepository {
    fun favorites(): Flow<List<FavItem>>
    fun isFavorite(kind: String, refId: String): Flow<Boolean>
    suspend fun toggleFavorite(kind: String, refId: String, title: String)

    fun bookmarks(kind: String): Flow<List<BookmarkItem>>
    /** Seluruh bookmark, lintas jenis — `plan.txt`: "Sediakan favorit, riwayat terakhir dibaca, bookmark, …". */
    fun allBookmarks(): Flow<List<BookmarkItem>>
    fun isBookmarked(kind: String, refId: String): Flow<Boolean>
    suspend fun toggleBookmark(kind: String, refId: String, label: String)

    /**
     * Riwayat terbaru, dipotong [limit] entri. Batasnya **wajib** disebut pemanggil dan tidak punya
     * bawaan: dahulu di sini tertulis `= 50`, padahal tak satu pun pemanggil memakainya (semuanya
     * menulis batasnya sendiri, dan salah satunya `60`) — sehingga bawaan itu hanya tampak seperti
     * kebijakan padahal bukan, dan layar yang berbeda-beda diam-diam membaca jatah yang berbeda.
     * Angka-angkanya kini tinggal di
     * [id.or.kemenag.yasintahlil.core.util.BatasRiwayat].
     */
    fun recentHistory(limit: Int): Flow<List<HistItem>>

    /**
     * **Jumlah** riwayat yang dimiliki pengguna — `COUNT(*)`, bukan ukuran daftar yang sudah
     * dipotong. Dipakai beranda untuk menjawab "Riwayat: N"; menghitungnya dari [recentHistory]
     * membuat pengguna dengan lebih dari N entri diberi tahu angka yang salah tanpa satu pun pesan
     * galat, dan berbeda dari angka yang sama di layar "Manajemen data offline" yang memakai
     * `ringkasan()`.
     */
    fun jumlahRiwayat(): Flow<Int>

    /**
     * Catat satu bacaan yang dibuka. [openedAt] dapat diisi waktu kejadian yang sebenarnya: riwayat
     * warisan dipindahkan dari versi lama beserta waktunya, sehingga urutan "terakhir dibaca" tidak
     * berubah hanya karena pemindahannya berjalan belakangan.
     */
    suspend fun pushHistory(
        kind: String,
        refId: String,
        title: String,
        openedAt: Long = System.currentTimeMillis(),
    )
    suspend fun clearHistory()

    /**
     * Jumlah entri yang tersimpan lokal. Dipakai layar "Manajemen data offline" (`plan.txt`) agar
     * pengguna melihat isinya sebelum memutuskan menghapus — bukan menghapus dalam keadaan buta.
     */
    suspend fun ringkasan(): RingkasanData

    /** Penghapusan per bagian, bukan "hapus semua": pengguna dapat membuang riwayat saja. */
    suspend fun clearFavorites()
    suspend fun clearBookmarks()
    suspend fun clearProgress()

    fun progress(kind: String, refId: String): Flow<ProgressItem?>
    fun progressByKind(kind: String): Flow<List<ProgressItem>>
    suspend fun setProgress(kind: String, refId: String, value: Int)

    /** Ekspor seluruh data terstruktur pengguna sebagai JSON (untuk backup). */
    suspend fun exportJson(): String

    /** Pulihkan data terstruktur dari JSON hasil [exportJson]; mengganti isi saat ini. */
    suspend fun restoreJson(json: String)
}
