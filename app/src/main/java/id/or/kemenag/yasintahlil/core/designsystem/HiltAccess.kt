package id.or.kemenag.yasintahlil.core.designsystem

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.or.kemenag.yasintahlil.core.data.local.MigrasiWarisan
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.util.JenisBacaan

/** Titik akses Hilt untuk composable yang perlu repositori data pengguna tanpa ViewModel. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserDataEntryPoint {
    fun userData(): UserDataRepository

    /**
     * Pemindah data warisan. Layar Pengaturan memakainya setelah memulihkan backup lama: berkas
     * cadangan v1.0.0 hanya memuat preferensi, sehingga favorit, riwayat, dan posisi Tahlil
     * pengguna ditulis kembali ke DataStore — dan tanpa pemanggilan ini, tak ada yang memindahkannya
     * ke Room.
     */
    fun migrasiWarisan(): MigrasiWarisan
}

@Composable
fun rememberUserData(): UserDataRepository {
    val ctx = LocalContext.current
    return remember(ctx) {
        EntryPointAccessors.fromApplication(ctx.applicationContext, UserDataEntryPoint::class.java).userData()
    }
}

@Composable
fun rememberMigrasiWarisan(): MigrasiWarisan {
    val ctx = LocalContext.current
    return remember(ctx) {
        EntryPointAccessors.fromApplication(ctx.applicationContext, UserDataEntryPoint::class.java)
            .migrasiWarisan()
    }
}

/**
 * Pisahkan id gabungan `jenis:rujukan` menjadi pasangannya.
 *
 * Aturannya tinggal di `JenisBacaan.pisah` — tempat yang sama dengan yang dipakai penulis id
 * ([JenisBacaan.id]) dan pemetaan tujuan (`RuteKonten`). Menyalin pemisahnya di sini dahulu berarti
 * satu sisi dapat berubah tanpa sisi lain ikut.
 */
fun splitContentId(id: String): Pair<String, String> = JenisBacaan.pisah(id)
