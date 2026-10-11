package id.or.kemenag.yasintahlil.core.designsystem

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository

/** Titik akses Hilt untuk composable yang perlu repositori data pengguna tanpa ViewModel. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserDataEntryPoint {
    fun userData(): UserDataRepository
}

@Composable
fun rememberUserData(): UserDataRepository {
    val ctx = LocalContext.current
    return remember(ctx) {
        EntryPointAccessors.fromApplication(ctx.applicationContext, UserDataEntryPoint::class.java).userData()
    }
}

/** Pisahkan id gabungan "kind:refId" menjadi pasangannya. */
fun splitContentId(id: String): Pair<String, String> {
    val i = id.indexOf(':')
    return if (i <= 0) "lain" to id else id.substring(0, i) to id.substring(i + 1)
}
