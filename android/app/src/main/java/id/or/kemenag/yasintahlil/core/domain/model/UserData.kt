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

/**
 * Jumlah entri yang tersimpan di perangkat, untuk layar "Manajemen data offline" (`plan.txt`).
 * Ditampilkan lebih dulu supaya pengguna tahu apa yang akan hilang sebelum menghapusnya.
 */
data class RingkasanData(
    val favorit: Int = 0,
    val bookmark: Int = 0,
    val riwayat: Int = 0,
    val progres: Int = 0,
)
