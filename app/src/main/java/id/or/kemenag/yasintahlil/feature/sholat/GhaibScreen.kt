package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Sholat Ghaib: 4 takbir tanpa ruku/sujud, fardhu kifayah.
// Dalil primer: HR Muttafaq Alaih (Abu Hurairah) — sholat Najasyi.
// Rujukan tata cara: imbauan Kemenag (Cianjur 2022, Turki-Suriah 2023, Maroko-Libya 2023).
// Catatan mazhab: Syafi'i/Hambali membolehkan; Hanafi/Maliki masyhur membatasi.
@Composable
fun GhaibScreen() {
    val steps = listOf(
        "1. Niat dalam hati (laki/perempuan/jamak/tak dikenal nama) — fardhu kifayah.",
        "2. Takbir 1 → ta'awudz + Al-Fatihah (HR Bukhari 756, Muslim 394; Bukhari 1335).",
        "3. Takbir 2 → Sholawat Ibrahimiyah (Musnad Syafi'i 588).",
        "4. Takbir 3 → Doa untuk mayit (Allahummaghfir lahu/laha...).",
        "5. Takbir 4 → Doa Allahumma la tahrimna ajrahu...",
        "6. Salam kanan-kiri.",
        "Syarat: jenazah jauh/sulit dijangkau + sudah dimandikan. Rujukan Indonesia: Syafi'i."
    )
    Column(Modifier.padding(16.dp)) {
        Text("Sholat Ghaib — 4 takbir, tanpa ruku/sujud")
        steps.forEach { Text(it) }
        Text("Sumber: Kemenag.go.id • sunnah.com • SOURCES.md")
    }
}
