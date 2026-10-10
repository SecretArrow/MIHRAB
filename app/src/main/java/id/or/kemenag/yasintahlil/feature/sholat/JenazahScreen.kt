package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.designsystem.*

/** Sholat jenazah hadir: 4 takbir tanpa ruku/sujud, fardhu kifayah. */
@Composable
fun JenazahScreen() {
    val repo = rememberRepo()
    val bacaan = remember { repo.sholatBacaan() }
    fun cari(id: String) = bacaan.find { it.id == id }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Sholat Jenazah — 4 takbir", style = MaterialTheme.typography.titleLarge)
        Text("Syarat: jenazah sudah dimandikan & dikafani, suci, menghadap kiblat.",
            style = MaterialTheme.typography.labelMedium)
        BacaanCard("jenazah:niat", "Niat (laki-laki; perempuan: مَيِّتَةِ ... الْغَائِبَةِ tanpa kata ghaib)",
            "أُصَلِّي عَلَى هٰذَا الْمَيِّتِ أَرْبَعَ تَكْبِيرَاتٍ فَرْضَ الْكِفَايَةِ إِمَامًا/مَأْمُوْمًا لِلّٰهِ تَعَالَى",
            "Ushalli 'ala hadzal mayyiti arba'a takbiratin fardhal kifayati imaman/ma'muman lillahi ta'ala.",
            "Aku menyolatkan jenazah ini 4 takbir fardhu kifayah karena Allah.",
            true, true, 22f, 15f)
        listOf(
            "Takbir 1 → Al-Fatihah" to cari("fatihah"),
            "Takbir 2 → Sholawat Ibrahimiyah" to cari("sholawat_ibrahimiyah"),
            "Takbir 3 → Doa mayit" to null,
            "Takbir 4 → Doa tambahan" to null,
            "Salam" to cari("salam")
        ).forEachIndexed { i, (judul, b) ->
            Text("${i + 1}. $judul", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
            when {
                judul.contains("mayit") -> BacaanCard("jenazah:doa3", "Doa takbir ketiga",
                    "اَللّٰهُمَّ اغْفِرْ لَهُ وَارْحَمْهُ وَعَافِهِ وَاعْفُ عَنْهُ",
                    "Allahummaghfir lahu warhamhu wa 'afihi wa'fu 'anhu.",
                    "Ya Allah, ampunilah dia, rahmatilah dia, sejahterakanlah dia dan maafkanlah dia.",
                    true, true, 22f, 15f, sumber = "HR Muslim — sunnah.com")
                judul.contains("tambahan") -> BacaanCard("jenazah:doa4", "Doa takbir keempat",
                    "اَللّٰهُمَّ لَا تَحْرِمْنَا أَجْرَهُ وَلَا تَفْتِنَّا بَعْدَهُ وَاغْفِرْ لَنَا وَلَهُ",
                    "Allahumma la tahrimna ajrahu wa la taftinna ba'dahu waghfir lana wa lah.",
                    "Ya Allah, jangan halangi pahalanya sampai kepada kami, jangan beri kami fitnah sepeninggalnya, dan ampunilah kami dan dia.",
                    true, true, 22f, 15f, sumber = "HR Tirmidzi — sunnah.com")
                b != null -> {
                    val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
                        Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
                    }
                    BacaanCard("jenazah:${b.id}", b.judul, a, l, t, true, true, 22f, 15f, sumber = b.sumber)
                }
            }
        }
    }
}
