package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Sisi satu ubin ornamen. */
private val UBIN_ORNAMEN: Dp = 64.dp

/** Alfa bawaan ornamen — cukup terlihat sebagai tekstur, tidak sampai mengganggu bacaan. */
private const val ALFA_ORNAMEN = 0.07f

/**
 * Ornamen geometris Islami yang halus (`plan.txt` §"Konsep dan Identitas Aplikasi": *"Ornamen
 * geometris Islami yang halus, tanpa membuat antarmuka terlihat ramai"*).
 *
 * Motifnya **khatam** — dua bujur sangkar yang sama, satu diputar 45°, sehingga membentuk bintang
 * delapan. Motif ini murni geometris: tidak ada lafaz, huruf, atau klaim keagamaan di dalamnya,
 * jadi tidak ada teks yang perlu diverifikasi sumbernya. Digambar sebagai garis tipis dengan alfa
 * sangat rendah dan diulang sebagai kisi, supaya berfungsi sebagai tekstur latar — bukan hiasan
 * yang berebut perhatian dengan teks.
 *
 * @param warna warna garis; bawaan mengikuti warna utama tema.
 * @param alpha kepekatan ornamen (0..1). Bawaan sengaja sangat rendah.
 * @param ubin jarak antarpusat motif.
 */
@Composable
fun OrnamenIslami(
    modifier: Modifier = Modifier,
    warna: Color = MaterialTheme.colorScheme.primary,
    alpha: Float = ALFA_ORNAMEN,
    ubin: Dp = UBIN_ORNAMEN,
) {
    Canvas(modifier) {
        val sisi = ubin.toPx()
        if (sisi <= 0f) return@Canvas
        val garis = warna.copy(alpha = alpha.coerceIn(0f, 1f))
        val tebal = (sisi * 0.035f).coerceAtLeast(1f)
        val panjang = sisi * 0.40f
        val kolom = (size.width / sisi).toInt() + 1
        val baris = (size.height / sisi).toInt() + 1
        for (r in 0 until baris) {
            for (c in 0 until kolom) {
                val pusat = Offset(c * sisi + sisi / 2f, r * sisi + sisi / 2f)
                val kiriAtas = Offset(pusat.x - panjang / 2f, pusat.y - panjang / 2f)
                val kotak = Size(panjang, panjang)
                drawRect(garis, topLeft = kiriAtas, size = kotak, style = Stroke(tebal))
                // Salinan yang diputar 45° — bersama kotak pertama membentuk bintang delapan.
                rotate(45f, pusat) {
                    drawRect(garis, topLeft = kiriAtas, size = kotak, style = Stroke(tebal))
                }
            }
        }
    }
}
