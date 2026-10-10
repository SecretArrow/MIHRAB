package id.or.kemenag.yasintahlil.feature.kiblat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.util.QiblaCalc

@Composable
fun KiblatScreen() {
    val jkt = QiblaCalc.bearing(-6.2, 106.8)
    Column(Modifier.padding(16.dp)) {
        Text("Arah Kiblat (SIHAT Kemenag)")
        Text("Contoh Jakarta: %.2f° dari utara sejati".format(jkt))
        Text("Ka'bah: 21°25'21.0\"U 39°49'34.3\"T. Pisahkan azimut matematis vs akurasi sensor. Fallback: rashdul kiblat.")
    }
}
