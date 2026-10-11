package id.or.kemenag.yasintahlil.core.domain.model

/** Model domain (tanpa ketergantungan Android) untuk data pengguna. */

data class FavItem(
    val kind: String,
    val refId: String,
    val title: String,
    val createdAt: Long,
)

data class BookmarkItem(
    val kind: String,
    val refId: String,
    val label: String,
    val createdAt: Long,
)

data class HistItem(
    val kind: String,
    val refId: String,
    val title: String,
    val openedAt: Long,
)

data class ProgressItem(
    val kind: String,
    val refId: String,
    val value: Int,
    val updatedAt: Long,
)
