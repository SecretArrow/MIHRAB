package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.designsystem.*
import kotlinx.coroutines.launch

/** Sholat Ghaib: 4 takbir tanpa ruku/sujud, fardhu kifayah. Dalil: HR Muttafaq Alaih (Najasyi). */
@Composable
fun GhaibScreen() {
    val repo = rememberRepo()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var fullscreen by remember { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(24f) }
    val bacaan = remember { repo.sholatBacaan() }
    fun cari(id: String) = bacaan.find { it.id == id }
    val steps = listOf(
        "Niat dalam hati (varian di bawah) — fardhu kifayah" to cari("niat"),
        "Takbir 1 → ta'awudz + Al-Fatihah (HR Bukhari 756, Muslim 394)" to cari("fatihah"),
        "Takbir 2 → Sholawat Ibrahimiyah" to cari("sholawat_ibrahimiyah"),
        "Takbir 3 → Doa untuk mayit" to null,
        "Takbir 4 → Doa tambahan" to null,
        "Salam kanan-kiri" to cari("salam")
    )
    val niatVarian = listOf(
        Triple("Laki-laki (diketahui nama)",
            "أُصَلِّي عَلَى مَيِّتِ (فُلَانِ) الْغَائِبِ أَرْبَعَ تَكْبِيرَاتٍ فَرْضَ الْكِفَايَةِ إِمَامًا/مَأْمُوْمًا لِلّٰهِ تَعَالَى",
            "Ushalli 'ala mayyiti (fulan) al-gha-ibi arba'a takbiratin fardhal kifayati imaman/ma'muman lillahi ta'ala. Artinya: aku menyolatkan jenazah (nama) yang ghaib 4 takbir fardhu kifayah karena Allah."),
        Triple("Perempuan",
            "أُصَلِّي عَلَى مَيِّتَةِ (فُلَانَةٍ) الْغَائِبَةِ أَرْبَعَ تَكْبِيرَاتٍ فَرْضَ الْكِفَايَةِ إِمَامًا/مَأْمُوْمًا لِلّٰهِ تَعَالَى",
            "Ushalli 'ala mayyitati (fulanah) al-ghaibati ... Artinya sama untuk jenazah perempuan."),
        Triple("Jamak / tidak diketahui",
            "أُصَلِّي عَلَى مَنْ صَلَّى عَلَيْهِ الْإِمَامُ أَرْبَعَ تَكْبِيرَاتٍ فَرْضَ الْكِفَايَةِ مَأْمُوْمًا لِلّٰهِ تَعَالَى",
            "Ushalli 'ala man shalla 'alaihil imamu ... Artinya: aku sholat ghaib mengikuti imam 4 takbir fardhu kifayah karena Allah.")
    )
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Sholat Ghaib — 4 takbir, tanpa ruku/sujud", style = MaterialTheme.typography.titleLarge)
        Text("Dalil: HR Muttafaq Alaih dari Abu Hurairah (sholat Najasyi). Rujukan tata cara: imbauan Kemenag; syarat & kritik sanad: NU Online. Lihat SOURCES.md.",
            style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text("Layar penuh (imam)") })
            OutlinedButton(onClick = { arabSize = (arabSize + 2).coerceAtMost(44f) }) { Text("A+") }
            OutlinedButton(onClick = { arabSize = (arabSize - 2).coerceAtLeast(16f) }) { Text("A−") }
        }
        Text("Varian niat:", style = MaterialTheme.typography.titleMedium)
        niatVarian.forEach { (judul, arab, latin) ->
            BacaanCard("ghaib:niat:$judul", judul, arab, latin, "", true, false, arabSize, 15f)
        }
        steps.forEachIndexed { i, (judul, b) ->
            Text("${i + 1}. $judul", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
            if (i == 3) BacaanCard("ghaib:doa3", "Doa takbir ketiga",
                "اَللّٰهُمَّ اغْفِرْ لَهُ وَارْحَمْهُ وَعَافِهِ وَاعْفُ عَنْهُ",
                "Allahummaghfir lahu warhamhu wa 'afihi wa'fu 'anhu. (Perempuan: laha/warhamha/wa 'afiha/wa'fu 'anha)",
                "Ya Allah, ampunilah dia, rahmatilah dia, sejahterakanlah dia dan maafkanlah dia.",
                true, true, arabSize, 15f, sumber = "HR Muslim — sunnah.com")
            else if (i == 4) BacaanCard("ghaib:doa4", "Doa takbir keempat",
                "اَللّٰهُمَّ لَا تَحْرِمْنَا أَجْرَهُ وَلَا تَفْتِنَّا بَعْدَهُ وَاغْفِرْ لَنَا وَلَهُ",
                "Allahumma la tahrimna ajrahu wa la taftinna ba'dahu waghfir lana wa lah.",
                "Ya Allah, jangan halangi pahala (jenazah) sampai kepada kami, jangan beri kami fitnah sepeninggalnya, dan ampunilah kami dan dia.",
                true, true, arabSize, 15f, sumber = "HR Tirmidzi, Ibnu Majah — sunnah.com")
            else if (b != null) {
                val (a, l, t) = repo.resolveArabLatinTerjemah(b.rujukanFile).let {
                    Triple(it.first.ifBlank { b.arab }, it.second.ifBlank { b.latin }, it.third.ifBlank { b.terjemahan })
                }
                BacaanCard("ghaib:${b.id}", b.judul, a, l, t, true, true, if (fullscreen) arabSize + 8 else arabSize, 15f, sumber = b.sumber)
            }
        }
        Text("Catatan mazhab: Syafi'i & Hambali membolehkan; Hanafi & Maliki (masyhur) membatasi. Syarat sah: jenazah jauh/sulit dijangkau dan sudah dimandikan.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    }
}
