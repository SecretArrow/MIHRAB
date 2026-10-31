package id.or.kemenag.yasintahlil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.TemaAplikasi
import id.or.kemenag.yasintahlil.core.designsystem.TemaHalaman
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Menyediakan tema aktif (terang/gelap/sistem) ke seluruh aplikasi. */
@HiltViewModel
class MainViewModel @Inject constructor(
    store: Store,
) : ViewModel() {
    val theme: StateFlow<String> =
        store.flow(Store.DARK, TemaAplikasi.SISTEM)
            .stateIn(viewModelScope, SharingStarted.Eagerly, TemaAplikasi.SISTEM)

    /** Tema halaman bacaan (`ikut`/`krem`/`gelap`) — dibaca di akar agar berlaku untuk semua layar. */
    val pageTheme: StateFlow<String> =
        store.flow(Store.PAGE_THEME, TemaHalaman.IKUT)
            .stateIn(viewModelScope, SharingStarted.Eagerly, TemaHalaman.IKUT)
}
