package id.or.kemenag.yasintahlil.core.data.local

import id.or.kemenag.yasintahlil.core.data.FormatCadangan
import id.or.kemenag.yasintahlil.core.domain.model.BookmarkItem
import id.or.kemenag.yasintahlil.core.domain.model.FavItem
import id.or.kemenag.yasintahlil.core.domain.model.HistItem
import id.or.kemenag.yasintahlil.core.domain.model.ProgressItem
import id.or.kemenag.yasintahlil.core.domain.model.RingkasanData
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomUserDataRepository @Inject constructor(
    private val db: AppDatabase,
) : UserDataRepository {

    private val fav = db.favoriteDao()
    private val mark = db.bookmarkDao()
    private val hist = db.historyDao()
    private val prog = db.progressDao()

    override fun favorites(): Flow<List<FavItem>> =
        fav.observeAll().map { list -> list.map { FavItem(it.kind, it.refId, it.title, it.createdAt) } }

    override fun isFavorite(kind: String, refId: String): Flow<Boolean> = fav.observeIsFavorite(kind, refId)

    override suspend fun toggleFavorite(kind: String, refId: String, title: String) {
        if (fav.exists(kind, refId)) fav.delete(kind, refId) else fav.upsert(FavoriteEntity(kind, refId, title))
    }

    override fun bookmarks(kind: String): Flow<List<BookmarkItem>> =
        mark.observeByKind(kind).map { list -> list.map { BookmarkItem(it.kind, it.refId, it.label, it.createdAt) } }

    override fun allBookmarks(): Flow<List<BookmarkItem>> =
        mark.observeAll().map { list -> list.map { BookmarkItem(it.kind, it.refId, it.label, it.createdAt) } }

    override fun isBookmarked(kind: String, refId: String): Flow<Boolean> = mark.observeIsBookmarked(kind, refId)

    override suspend fun toggleBookmark(kind: String, refId: String, label: String) {
        if (mark.exists(kind, refId)) mark.delete(kind, refId) else mark.upsert(BookmarkEntity(kind, refId, label))
    }

    override fun recentHistory(limit: Int): Flow<List<HistItem>> =
        hist.observeRecent(limit).map { list -> list.map { HistItem(it.kind, it.refId, it.title, it.openedAt) } }

    override fun jumlahRiwayat(): Flow<Int> = hist.observeCount()

    override suspend fun pushHistory(kind: String, refId: String, title: String, openedAt: Long) {
        hist.insert(HistoryEntity(kind = kind, refId = refId, title = title, openedAt = openedAt))
    }

    override suspend fun clearHistory() = hist.clear()

    override suspend fun ringkasan(): RingkasanData = RingkasanData(
        favorit = fav.count(),
        bookmark = mark.count(),
        riwayat = hist.count(),
        progres = prog.count(),
    )

    override suspend fun clearFavorites() = fav.clear()

    override suspend fun clearBookmarks() = mark.clear()

    override suspend fun clearProgress() = prog.clear()

    override fun progress(kind: String, refId: String): Flow<ProgressItem?> =
        prog.observe(kind, refId).map { list ->
            list.firstOrNull()?.let { p -> ProgressItem(p.kind, p.refId, p.value, p.updatedAt) }
        }

    override fun progressByKind(kind: String): Flow<List<ProgressItem>> =
        prog.observeByKind(kind).map { list -> list.map { ProgressItem(it.kind, it.refId, it.value, it.updatedAt) } }

    override suspend fun setProgress(kind: String, refId: String, value: Int) {
        prog.upsert(ProgressEntity(kind, refId, value))
    }

    override suspend fun exportJson(): String {
        val root = JSONObject()
        root.put(FormatCadangan.FAVORIT, JSONArray().apply {
            fav.getAll().forEach { f ->
                put(JSONObject().put(FormatCadangan.KIND, f.kind).put(FormatCadangan.REF_ID, f.refId)
                    .put(FormatCadangan.JUDUL, f.title).put(FormatCadangan.DIBUAT, f.createdAt))
            }
        })
        root.put(FormatCadangan.BOOKMARK, JSONArray().apply {
            mark.getAll().forEach { b ->
                put(JSONObject().put(FormatCadangan.KIND, b.kind).put(FormatCadangan.REF_ID, b.refId)
                    .put(FormatCadangan.LABEL, b.label).put(FormatCadangan.DIBUAT, b.createdAt))
            }
        })
        root.put(FormatCadangan.PROGRES, JSONArray().apply {
            prog.getAll().forEach { p ->
                put(JSONObject().put(FormatCadangan.KIND, p.kind).put(FormatCadangan.REF_ID, p.refId)
                    .put(FormatCadangan.NILAI, p.value).put(FormatCadangan.DIPERBARUI, p.updatedAt))
            }
        })
        root.put(FormatCadangan.RIWAYAT, JSONArray().apply {
            hist.getAll(HISTORY_BACKUP_LIMIT).forEach { h ->
                put(JSONObject().put(FormatCadangan.ID, h.id)
                    .put(FormatCadangan.KIND, h.kind).put(FormatCadangan.REF_ID, h.refId)
                    .put(FormatCadangan.JUDUL, h.title).put(FormatCadangan.DIBUKA, h.openedAt))
            }
        })
        return root.toString()
    }

    override suspend fun restoreJson(json: String) {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return
        // Baca SEMUA larik dulu (entri rusak dilewati) sebelum menyentuh data lama, agar berkas
        // backup yang cacat tidak membuat pengguna kehilangan data yang sedang tersimpan.
        val favArr = root.optJSONArray(FormatCadangan.FAVORIT)
        val markArr = root.optJSONArray(FormatCadangan.BOOKMARK)
        val progArr = root.optJSONArray(FormatCadangan.PROGRES)
        val histArr = root.optJSONArray(FormatCadangan.RIWAYAT)
        val favs = favArr.toEntities { j ->
            FavoriteEntity(j.getString(FormatCadangan.KIND), j.getString(FormatCadangan.REF_ID),
                j.optString(FormatCadangan.JUDUL), j.optLong(FormatCadangan.DIBUAT, System.currentTimeMillis()))
        }
        val marks = markArr.toEntities { j ->
            BookmarkEntity(j.getString(FormatCadangan.KIND), j.getString(FormatCadangan.REF_ID),
                j.optString(FormatCadangan.LABEL), j.optLong(FormatCadangan.DIBUAT, System.currentTimeMillis()))
        }
        val progs = progArr.toEntities { j ->
            ProgressEntity(j.getString(FormatCadangan.KIND), j.getString(FormatCadangan.REF_ID),
                j.optInt(FormatCadangan.NILAI), j.optLong(FormatCadangan.DIPERBARUI, System.currentTimeMillis()))
        }
        val hists = histArr.toEntities { j ->
            HistoryEntity(j.optLong(FormatCadangan.ID, 0), j.getString(FormatCadangan.KIND),
                j.getString(FormatCadangan.REF_ID), j.optString(FormatCadangan.JUDUL),
                j.optLong(FormatCadangan.DIBUKA, System.currentTimeMillis()))
        }

        // Tiap bagian diganti **hanya bila berkasnya memang memuat larik itu**. Berkas yang tak
        // menyebut sebuah bagian — versi lain, berkas separuh, atau kunci yang meleset — tak boleh
        // mengosongkan bagian tersebut: dahulu keempat tabel dibersihkan lebih dahulu, sehingga satu
        // nama kunci yang berganti nama menghapus seluruh data pengguna tanpa satu pun pesan galat.
        if (favArr != null) { fav.clear(); if (favs.isNotEmpty()) fav.insertAll(favs) }
        if (markArr != null) { mark.clear(); if (marks.isNotEmpty()) mark.insertAll(marks) }
        if (progArr != null) { prog.clear(); if (progs.isNotEmpty()) prog.insertAll(progs) }
        if (histArr != null) { hist.clear(); if (hists.isNotEmpty()) hist.insertAll(hists) }
    }

    /** Petakan array JSON ke entitas; lewati entri yang gagal di-parse (mis. backup cacat). */
    private fun <T> JSONArray?.toEntities(map: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { i ->
            runCatching { map(getJSONObject(i)) }.getOrNull()
        }
    }

    private companion object {
        /** Batasi riwayat yang ikut di-backup agar berkas tidak membengkak. */
        const val HISTORY_BACKUP_LIMIT = 200
    }
}
