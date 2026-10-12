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
    fun isBookmarked(kind: String, refId: String): Flow<Boolean>
    suspend fun toggleBookmark(kind: String, refId: String, label: String)

    fun recentHistory(limit: Int = 50): Flow<List<HistItem>>
    suspend fun pushHistory(kind: String, refId: String, title: String)
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
