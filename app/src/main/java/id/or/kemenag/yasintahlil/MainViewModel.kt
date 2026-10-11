package id.or.kemenag.yasintahlil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.Store
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
        store.flow(Store.DARK, "sistem").stateIn(viewModelScope, SharingStarted.Eagerly, "sistem")
}
