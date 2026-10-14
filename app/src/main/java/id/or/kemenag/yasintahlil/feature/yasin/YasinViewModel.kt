package id.or.kemenag.yasintahlil.feature.yasin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.Surah
import id.or.kemenag.yasintahlil.core.domain.model.BookmarkItem
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.JudulBacaan
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YasinUiState(
    val arabSize: Float = UkuranBacaan.ARAB_DEFAULT,
    // Keadaan awal memakai bawaan yang sama dengan nilai yang dibaca dari preferensi; kalau tidak,
    // bacaan sempat tampil dengan pengaturan lain sebelum DataStore selesai dibaca.
    val showLatin: Boolean = BawaanPreferensi.TAMPIL_LATIN,
    val showTrans: Boolean = BawaanPreferensi.TAMPIL_TERJEMAHAN,
    val showArab: Boolean = BawaanPreferensi.TAMPIL_ARAB,
    val mark: Int = BawaanPreferensi.PENANDA_YASIN,
    val bookmarks: List<BookmarkItem> = emptyList(),
)

@HiltViewModel
class YasinViewModel @Inject constructor(
    private val store: Store,
    private val repo: ContentRepo,
    private val userData: UserDataRepository,
) : ViewModel() {

    val surah: Surah = repo.yasin

    private val _state = MutableStateFlow(YasinUiState())
    val state: StateFlow<YasinUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT),
                    showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN),
                    showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN),
                    showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB),
                    mark = store.get(Store.YASIN_MARK, BawaanPreferensi.PENANDA_YASIN),
                )
            }
        }
        // Bookmark tersimpan di Room (tahan rotasi & process-death).
        viewModelScope.launch {
            userData.bookmarks(JenisBacaan.YASIN).collect { list -> _state.update { s -> s.copy(bookmarks = list) } }
        }
    }

    // Catatan: pengaturan tampilan bacaan (Arab/Latin/terjemahan, ukuran Arab) tidak lagi punya
    // pengubahnya di layar ini — semuanya terpusat di tab Pengaturan dan layar membacanya lewat
    // keadaan di bawah, jadi perubahan di Pengaturan tetap diterapkan saat layar dibuka.

    fun markRead(n: Int) {
        _state.update { it.copy(mark = n) }
        save { store.set(Store.YASIN_MARK, n); store.set(Store.LAST, JenisBacaan.id(JenisBacaan.YASIN, n.toString())) }
        // Riwayat "terakhir dibaca" (`plan.txt` §"Pencarian, Favorit, dan Riwayat"). Sebelumnya
        // modul Yasin tidak pernah menulis riwayat sama sekali: satu-satunya penulis riwayat adalah
        // tombol "Salin" pada kartu bacaan, sehingga daftar "Riwayat terbaru" tidak memuat bacaan
        // yang benar-benar dibuka. Ditulis di sini — bukan pada tiap gulir — karena `markRead`
        // hanya dipanggil saat pengguna memang menandai posisi bacaannya.
        save { userData.pushHistory(JenisBacaan.YASIN, n.toString(), JudulBacaan.yasinAyat(n)) }
    }

    fun toggleBookmark(n: Int) {
        viewModelScope.launch { userData.toggleBookmark(JenisBacaan.YASIN, n.toString(), JudulBacaan.yasinAyat(n)) }
    }

    private fun save(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
