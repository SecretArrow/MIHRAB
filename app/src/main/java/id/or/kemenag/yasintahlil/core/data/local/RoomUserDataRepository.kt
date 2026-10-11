package id.or.kemenag.yasintahlil.core.data.local

import id.or.kemenag.yasintahlil.core.domain.model.BookmarkItem
import id.or.kemenag.yasintahlil.core.domain.model.FavItem
import id.or.kemenag.yasintahlil.core.domain.model.HistItem
import id.or.kemenag.yasintahlil.core.domain.model.ProgressItem
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

    override fun isBookmarked(kind: String, refId: String): Flow<Boolean> = mark.observeIsBookmarked(kind, refId)

    override suspend fun toggleBookmark(kind: String, refId: String, label: String) {
        if (mark.exists(kind, refId)) mark.delete(kind, refId) else mark.upsert(BookmarkEntity(kind, refId, label))
    }

    override fun recentHistory(limit: Int): Flow<List<HistItem>> =
        hist.observeRecent(limit).map { list -> list.map { HistItem(it.kind, it.refId, it.title, it.openedAt) } }

    override suspend fun pushHistory(kind: String, refId: String, title: String) {
        hist.insert(HistoryEntity(kind = kind, refId = refId, title = title))
    }

    override suspend fun clearHistory() = hist.clear()

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
        root.put("favorites", JSONArray().apply {
            fav.getAll().forEach { f -> put(JSONObject().put("kind", f.kind).put("refId", f.refId)
                .put("title", f.title).put("createdAt", f.createdAt)) }
        })
        root.put("bookmarks", JSONArray().apply {
            mark.getAll().forEach { b -> put(JSONObject().put("kind", b.kind).put("refId", b.refId)
                .put("label", b.label).put("createdAt", b.createdAt)) }
        })
        root.put("progress", JSONArray().apply {
            prog.getAll().forEach { p -> put(JSONObject().put("kind", p.kind).put("refId", p.refId)
                .put("value", p.value).put("updatedAt", p.updatedAt)) }
        })
        root.put("history", JSONArray().apply {
            hist.getAll(HISTORY_BACKUP_LIMIT).forEach { h -> put(JSONObject().put("id", h.id)
                .put("kind", h.kind).put("refId", h.refId).put("title", h.title).put("openedAt", h.openedAt)) }
        })
        return root.toString()
    }

    override suspend fun restoreJson(json: String) {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return
        fav.clear(); mark.clear(); prog.clear(); hist.clear()

        root.optJSONArray("favorites")?.let { arr ->
            fav.insertAll((0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                FavoriteEntity(j.getString("kind"), j.getString("refId"),
                    j.optString("title"), j.optLong("createdAt", System.currentTimeMillis()))
            })
        }
        root.optJSONArray("bookmarks")?.let { arr ->
            mark.insertAll((0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                BookmarkEntity(j.getString("kind"), j.getString("refId"),
                    j.optString("label"), j.optLong("createdAt", System.currentTimeMillis()))
            })
        }
        root.optJSONArray("progress")?.let { arr ->
            prog.insertAll((0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                ProgressEntity(j.getString("kind"), j.getString("refId"),
                    j.optInt("value"), j.optLong("updatedAt", System.currentTimeMillis()))
            })
        }
        root.optJSONArray("history")?.let { arr ->
            hist.insertAll((0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                HistoryEntity(j.optLong("id", 0), j.getString("kind"), j.getString("refId"),
                    j.optString("title"), j.optLong("openedAt", System.currentTimeMillis()))
            })
        }
    }

    private companion object {
        /** Batasi riwayat yang ikut di-backup agar berkas tidak membengkak. */
        const val HISTORY_BACKUP_LIMIT = 200
    }
}
