package id.or.kemenag.yasintahlil.feature.favorit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.DzikirGroup
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.domain.model.BookmarkItem
import id.or.kemenag.yasintahlil.core.domain.model.FavItem
import id.or.kemenag.yasintahlil.core.domain.model.HistItem
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.navigation.RuteKonten
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritUiState(
    val favorites: List<FavItem> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val history: List<HistItem> = emptyList(),
    val last: String = "",
    /**
     * Id kelompok dzikir pada bundel konten — dipakai [id.or.kemenag.yasintahlil.navigation.RuteKonten]
     * untuk mengenali favorit/riwayat dzikir, yang `kind`-nya adalah id kelompok, bukan `"dzikir"`.
     */
    val grupDzikir: Set<String> = emptySet(),
    /** Sebab daftar kelompok dzikir tidak terbaca, atau `null` bila bundel konten normal. */
    val dzikirGagal: String? = null,
)

@HiltViewModel
class FavoritViewModel @Inject constructor(
    private val store: Store,
    private val userData: UserDataRepository,
    private val repo: ContentRepo,
) : ViewModel() {

    /**
     * Id kelompok dzikir dibaca dari bundel konten, bukan ditulis tetap di layar: kelompok yang
     * ditambahkan bundel otomatis dikenali sehingga favoritnya tidak lagi jatuh ke Beranda.
     *
     * Dahulu pembacaan ini ada di dalam `FavoritScreen` dan kegagalannya ditelan
     * (`runCatching { … }.getOrDefault(emptySet())`). Akibatnya bundel konten yang bermasalah
     * membuat setiap baris favorit/riwayat **dzikir** menampilkan *"Layar tujuan tidak dikenal"* —
     * padahal tujuannya memang dikenal (layar Dzikir & Doa); yang gagal adalah mengenali id
     * kelompoknya. Pengguna menyimpulkan isinya hilang. Sebabnya kini dibawa ke layar lewat
     * [FavoritUiState.dzikirGagal], sama seperti `kotaGagal` pada layar Jadwal.
     */
    private val hasilDzikir: Result<List<DzikirGroup>> = runCatching { repo.dzikir() }

    private val _state = MutableStateFlow(
        FavoritUiState(
            grupDzikir = hasilDzikir.getOrDefault(emptyList()).map { it.id }.toSet(),
            dzikirGagal = hasilDzikir.exceptionOrNull()?.let { e ->
                "Daftar kelompok dzikir tidak dapat dibaca dari bundel konten " +
                    "(${e.message ?: e::class.java.simpleName}); sebagian baris dzikir di bawah " +
                    "dapat menampilkan \"${RuteKonten.TIDAK_DIKENAL}\"."
            },
        ),
    )
    val state: StateFlow<FavoritUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { userData.favorites().collect { f -> _state.update { it.copy(favorites = f) } } }
        viewModelScope.launch { userData.allBookmarks().collect { b -> _state.update { it.copy(bookmarks = b) } } }
        // Riwayat "terakhir dibaca" kini juga ditulis modul Yasin dan Tahlil, jadi satu ayat atau
        // satu bagian dapat tercatat berkali-kali. Tanpa penyaringan, daftar 12 baris akan berisi
        // bacaan yang sama berulang kali dan mendorong bacaan lain keluar. Diambil 60 dulu, baru
        // disaring, agar pengulangan tidak menghabiskan jatah 20 baris sebelum penyaringan.
        viewModelScope.launch {
            userData.recentHistory(60).collect { h ->
                _state.update { it.copy(history = h.distinctBy { i -> i.kind to i.refId }.take(20)) }
            }
        }
        viewModelScope.launch { _state.update { it.copy(last = store.get(Store.LAST, BawaanPreferensi.TERAKHIR_DIBACA)) } }
    }

    fun remove(item: FavItem) {
        viewModelScope.launch { userData.toggleFavorite(item.kind, item.refId, item.title) }
    }

    fun removeBookmark(item: BookmarkItem) {
        viewModelScope.launch { userData.toggleBookmark(item.kind, item.refId, item.label) }
    }

    fun clearHistory() {
        viewModelScope.launch { userData.clearHistory() }
    }
}
