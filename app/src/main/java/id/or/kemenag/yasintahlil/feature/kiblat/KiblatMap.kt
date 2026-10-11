package id.or.kemenag.yasintahlil.feature.kiblat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.or.kemenag.yasintahlil.core.util.MapProjection
import id.or.kemenag.yasintahlil.core.util.QiblaCalc
import kotlin.math.roundToInt

/**
 * Peta skematik **offline**: menampilkan posisi pengguna, Ka'bah, dan rute lingkaran besar
 * di antara keduanya — tanpa ubin jaringan dan tanpa API key (aplikasi tetap 100% offline).
 * Latar berupa garis bantu (gratikula) 5°, bukan garis pantai, agar tidak menyajikan
 * geografi yang tidak akurat.
 */
@Composable
fun KiblatMap(lat: Double, lon: Double, bearing: Double, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()

    // Rute + ujung-ujungnya (titik pertama = pengguna, terakhir = Ka'bah), bujur di-unwrap.
    val path = remember(lat, lon) { QiblaCalc.greatCirclePath(lat, lon, 96) }
    val pts = remember(path) {
        val lons = MapProjection.unwrapLongitudes(path.map { it.second })
        path.map { it.first }.zip(lons)
    }
    val box = remember(pts) { MapProjection.bounds(pts, 4.0) }
    val aspect = remember(box) { MapProjection.aspect(box) }
    val jarakKm = remember(lat, lon) { QiblaCalc.distanceKm(lat, lon).roundToInt() }

    val grid = MaterialTheme.colorScheme.outlineVariant
    val route = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.secondary
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(color = ink, fontSize = 10.sp)
    val strongStyle = TextStyle(color = route, fontSize = 11.sp)

    Canvas(modifier.fillMaxWidth().height(240.dp)) {
        val pad = 30f
        val availW = (size.width - 2 * pad).coerceAtLeast(1f)
        val availH = (size.height - 2 * pad).coerceAtLeast(1f)
        val scale = minOf(availW / aspect, availH)
        val mapW = aspect * scale
        val mapH = scale
        val ox = (size.width - mapW) / 2f
        val oy = (size.height - mapH) / 2f

        fun at(la: Double, lo: Double): Offset {
            val (x, y) = MapProjection.project(la, lo, box)
            return Offset(ox + x * scale, oy + y * scale)
        }

        // Panel peta.
        drawRoundRect(
            color = panel,
            topLeft = Offset(ox, oy),
            size = Size(mapW, mapH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
        )

        // Gratikula: garis lintang/bujur tiap 5° di dalam kotak.
        var la = Math.ceil(box.minLat / 5.0) * 5.0
        while (la < box.maxLat) {
            val y = oy + MapProjection.project(la, box.minLon, box).second * scale
            drawLine(grid, Offset(ox, y), Offset(ox + mapW, y), 1f)
            la += 5.0
        }
        var lo = Math.ceil(box.minLon / 5.0) * 5.0
        while (lo < box.maxLon) {
            val x = ox + MapProjection.project(box.maxLat, lo, box).first * scale
            drawLine(grid, Offset(x, oy), Offset(x, oy + mapH), 1f)
            lo += 5.0
        }

        // Rute lingkaran besar (putus-putus).
        val p = Path()
        pts.forEachIndexed { i, (pla, plo) ->
            val o = at(pla, plo)
            if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y)
        }
        drawPath(p, route, style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))

        // Penanda Ka'bah (bintang emas).
        val kabah = at(QiblaCalc.KABAH_LAT, QiblaCalc.KABAH_LON)
        drawStar(kabah, 9f, gold)

        // Penanda pengguna.
        val user = at(lat, lon)
        drawCircle(route, 7f, user)
        drawCircle(Color.White, 3f, user)

        // Label.
        val kabahLabel = tm.measure("Ka'bah", labelStyle)
        drawText(kabahLabel, topLeft = Offset(kabah.x - kabahLabel.size.width / 2f, kabah.y - 24f))
        val userLabel = tm.measure("Anda", strongStyle)
        drawText(userLabel, topLeft = Offset(user.x - userLabel.size.width / 2f, user.y + 10f))

        // Arah utara.
        val north = tm.measure("U ↑", labelStyle)
        drawText(north, topLeft = Offset(ox + 6f, oy + 4f))
        // Jarak pada rute.
        val jarak = tm.measure("≈ $jarakKm km • azimut ${"%.1f".format(bearing)}°", labelStyle)
        drawText(jarak, topLeft = Offset(ox + 6f, oy + mapH - jarak.size.height - 4f))
    }
}

/** Bintang lima sudut sederhana (penanda Ka'bah) — geometri, tanpa makna figuratif. */
private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val inner = r * 0.42f
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else inner
        val a = Math.toRadians(-90.0 + i * 36.0)
        val x = c.x + (rad * Math.cos(a)).toFloat()
        val y = c.y + (rad * Math.sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
