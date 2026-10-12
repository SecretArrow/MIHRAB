package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tata letak responsif (`plan.txt`: *"Layout responsif untuk berbagai ukuran layar dan kepadatan
 * piksel Android"*).
 *
 * Sebelum ini tak ada satu pun batas lebar: di tablet atau ponsel lanskap seluruh isi membentang
 * sepanjang layar, sehingga satu baris terjemahan dapat mencapai ratusan karakter. Baris sepanjang
 * itu sulit diikuti mata — pembaca kehilangan awal baris berikutnya saat berpindah dari ujung
 * kanan ke kiri. Karena itu isi dibatasi lebar lalu dipusatkan, dan **kartu bacaan dibatasi lebih
 * sempit lagi** ([LEBAR_BACA_MAKS]) karena teksnya yang paling panjang.
 */
val LEBAR_KONTEN_MAKS: Dp = 720.dp

/** Lebar maksimum satu kartu bacaan — lebih sempit dari [LEBAR_KONTEN_MAKS] agar nyaman dibaca. */
val LEBAR_BACA_MAKS: Dp = 640.dp

/**
 * Pusatkan [konten] pada kolom selebar maksimum [lebarMaks].
 *
 * Di layar sempit kolomnya mengisi seluruh lebar seperti biasa; batas ini hanya berpengaruh ketika
 * layar memang lebih lebar, jadi tata letak ponsel tidak berubah.
 */
@Composable
fun KolomKonten(
    modifier: Modifier = Modifier,
    lebarMaks: Dp = LEBAR_KONTEN_MAKS,
    konten: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = lebarMaks).fillMaxWidth().fillMaxHeight()) { konten() }
    }
}

/**
 * Batasi lebar node ini pada [LEBAR_BACA_MAKS] sambil tetap dipusatkan bila induknya lebih lebar.
 *
 * Dipakai bersama `Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)`, sehingga
 * urutannya menjadi: isi selebar induk → anak yang lebih sempit dipusatkan → anak dibatasi lebarnya.
 */
fun Modifier.lebarBaca(): Modifier = widthIn(max = LEBAR_BACA_MAKS)
