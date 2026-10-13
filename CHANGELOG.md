# Changelog

Format mengikuti [Keep a Changelog](https://keepachangelog.com/id/1.1.0/).
Versi mengikuti [Semantic Versioning](https://semver.org/lang/id/).

## [Unreleased]

### Ditambahkan
- **Jenis suara adzan** (`plan.txt` §"Fitur Adzan": *"Pilihan suara adzan dari sumber audio yang
  legal digunakan"* dan *"Pengaturan volume dan jenis suara"*): layar Adzan hanya punya **satu**
  berkas audio (`Store.ADZAN_URI`) dan satu slider volume — "jenis suara" tidak ada sama sekali,
  sehingga memilih berkas lain berarti kehilangan berkas sebelumnya tanpa cara kembali. Kini
  pengguna dapat menyimpan **beberapa** jenis suara sekaligus, masing-masing berkas pilihannya
  sendiri yang ia beri nama (mis. "adzan masjid lokal"), lalu memilih satu yang aktif, menamainya
  ulang, atau menghapusnya. Aplikasi tetap **tidak menyertakan audio bawaan** (lisensi) — jenis
  suara tidak pernah berarti katalog suara dari aplikasi. Daftarnya disimpan di kunci **baru**
  `Store.ADZAN_SOUNDS`, sedangkan berkas yang berbunyi tetap satu kunci lama `Store.ADZAN_URI`,
  sehingga penjadwal dan pekerja latar tidak perlu tahu apa pun tentang daftar itu — berkas yang
  terdengar dijamin sama dengan yang terlihat aktif. Berkas aktif dari versi sebelumnya tetap
  muncul di daftar meski belum tercatat, jenis suara terakhir tidak dapat dihapus (menghapusnya
  menyisakan layar tanpa apa pun yang bisa dipilih), dan menghapus yang sedang aktif memindahkan
  pilihan ke berkas tersisa. Aturannya murni (tanpa Android) di `DaftarSuaraAdzan` dan dijaga
  `JenisSuaraAdzanTest`, yang juga menolak audio bawaan di `assets/`.
- **Jadwal sholat mengikuti perubahan lokasi** (`plan.txt` §"Jadwal Sholat dan Pengaturan Adzan":
  *"Pembaruan jadwal saat lokasi berubah"*): sebelumnya tombol "Pakai lokasi GPS" hanya membaca
  **satu** perbaikan terakhir (`LocationProvider.lastKnown`) lalu diam — jadwal tetap memakai
  koordinat tempat pengguna pertama kali menekannya sampai ia menekan tombol itu lagi, meskipun
  ia sudah berpindah kota. Layar Kiblat sudah lama mengikuti aliran lokasi live; Jadwal kini
  melakukan hal yang sama lewat saklar **"Ikuti perubahan lokasi"**. Hitung ulang hanya terjadi
  bila perpindahan melewati **1 km** (`AMBANG_PINDAH_METER`): GPS bergoyang beberapa meter saat
  perangkat diam, dan menghitung ulang sekaligus menjadwalkan ulang pengingat pada setiap goyangan
  hanya membuang baterai. Memilih kota secara manual menghentikan ikutan ini (lokasi manual menang
  atas GPS), dan bila penyedia lokasi mati saklarnya dikembalikan sendiri agar tidak menyala
  sementara tak ada yang diperbarui. Pilihannya disimpan di kunci **baru** `Store.JADWAL_IKUT` —
  bukan menumpang `Store.USE_GPS` yang berarti "koordinat berasal dari GPS" dan dipakai bersama
  layar Kiblat — dan ikut backup/pemulihan. Dijaga `JadwalLokasiTest`.
- **Penyaring di dalam halaman Dzikir & Panduan Sholat** (`plan.txt` §"UI/UX dan Navigasi":
  *"Sediakan pencarian dan akses cepat pada halaman yang memiliki banyak konten"*): pencarian
  terpadu sudah ada di menu Pencarian, tetapi ia menjawab pertanyaan lain ("di mana bacaan X di
  seluruh aplikasi") dan berpindah layar. Dua halaman terpadat — Pustaka Dzikir (31 entri dalam 5
  kelompok) dan Panduan Sholat (puluhan entri dalam 3 tab) — sebelumnya hanya bisa ditelusuri
  dengan menggulir dan berpindah tab. Kini masing-masing punya kotak pencarian yang menyaring
  daftar **di tempat**, mencocokkan judul, teks Arab, transliterasi, dan terjemahan; jumlah hasil
  ditampilkan, dan kata kunci yang tak menemukan apa pun dijelaskan beserta tombol mengosongkan.
  Logikanya dipisahkan ke `core/util/Saring.kt` yang murni tanpa Android sehingga dapat diuji
  langsung (`SaringTest`) — kata kunci kosong berarti **semua** entri, bukan nol: daftar yang
  sedang dibaca tidak boleh mendadak kosong hanya karena kotak pencariannya belum diisi.
- **"Doa memohon ampunan" sebagai koleksi bernama** (`plan.txt` §"Pustaka Doa dan Dzikir"
  mendaftarnya sebagai salah satu koleksi yang harus disediakan): lafaznya sudah ada dan
  terverifikasi, tetapi **tersebar** — Istighfar di "Dzikir setelah sholat", Sayyidul Istighfar dan
  Subhanallahi wa bihamdih di "Dzikir pagi & petang", Istighfar 3x di modul Tahlil — sehingga
  pengguna yang mencari "doa ampunan" tidak menemukan tempat yang menjawabnya. Kini ada kelompok
  **"Doa memohon ampunan"** di pustaka dzikir berisi ketiganya. Isinya **tidak satu pun disalin**:
  setiap entri hanya menunjuk lafaz yang sudah ada (`tahlil_full.json#istighfar`,
  `dzikir.json#sayyidul_istighfar`, `dzikir.json#subhanallahi_wa_bihamdih`), jadi seluruh aplikasi
  tetap memuat **satu salinan** per doa — `DzikirRujukanTest` menolak entri penunjuk yang menyimpan
  teksnya sendiri maupun yang menunjuk id tidak ada. Agar rujukan ke berkas yang sama mungkin,
  setiap entri `dzikir.json` kini punya `id` stabil dan penyelesai rujukan dipisahkan ke
  `entriRujukan()` (`core/data/Rujukan.kt`) yang mencari `#id` di `bagian`, `bacaan`, **dan**
  `kelompok[].items[]` — sebelumnya bentuk ketiga tidak dikenali, sehingga rujukan di dalam pustaka
  dzikir akan menjadi tautan mati. `RujukanTest` menguji penyelesai itu langsung (tanpa `Context`)
  sekaligus memastikan `ContentRepo` benar-benar memakainya, bukan menyalin logikanya.
- **Tata letak responsif, ornamen geometris Islami, dan animasi ringan** (`plan.txt` §"Konsep dan
  Identitas Aplikasi": *"Layout responsif untuk berbagai ukuran layar dan kepadatan piksel
  Android"*, *"Ornamen geometris Islami yang halus, tanpa membuat antarmuka terlihat ramai"*, dan
  *"Ikon konsisten, animasi ringan, transisi halus, serta feedback visual pada setiap interaksi"*):
  ketiganya dulu **tidak ada sama sekali**. Yang paling merugikan adalah batas lebar — tanpa satu
  pun batas, di tablet atau ponsel lanskap satu baris terjemahan membentang sepanjang layar,
  sehingga mata kehilangan awal baris berikutnya saat berpindah dari ujung kanan ke kiri. Kini
  seluruh isi dipusatkan pada kolom `LEBAR_KONTEN_MAKS` (720 dp) lewat `KolomKonten` yang dipasang
  **sekali di `AppNav`**, dan **kartu bacaan dibatasi lebih sempit lagi** (`LEBAR_BACA_MAKS`, 640 dp)
  karena teksnya yang terpanjang — di layar ponsel tata letaknya tidak berubah sama sekali, batas
  ini hanya bekerja saat layar memang lebih lebar. **Ornamen**nya motif **khatam** (dua bujur
  sangkar, satu diputar 45°): murni geometris, jadi tidak memuat lafaz yang perlu diverifikasi
  sumbernya — `TataLetakResponsifTest` menolak masuknya karakter Arab ke berkas itu, sama seperti
  penjaga pada `sholat_syarat.json`. Digambar sebagai garis tipis beralfa 0,07 di belakang blok
  salam Beranda agar menjadi tekstur latar, bukan hiasan yang berebut perhatian. **Animasinya**
  dibatasi pada tiga tempat yang memang butuh umpan balik: progres penghitung tasbih dihaluskan
  (`animateFloatAsState`), tinggi kartu bacaan ikut beranimasi saat chip "Arab"/"Latin"/"Terjemahan"
  dinyalakan (`animateContentSize`), dan daftar syarat sholat muncul dengan transisi
  (`AnimatedVisibility`). Dependensi `androidx.compose.animation` dideklarasikan eksplisit alih-alih
  mengandalkan transitif `foundation`.
- **Syarat, persiapan, dan penutup aurat sholat — di depan langkah pertama** (`plan.txt` §"Tata
  Cara Sholat Langkah demi Langkah" membuka daftar langkahnya dengan *"Syarat wajib dan syarat
  sah"*, *"Persiapan sebelum sholat"*, *"Wudhu dan hal yang membatalkannya"*, *"Penutup aurat dan
  kebersihan tempat sholat"*, dan *"Penentuan arah kiblat"*): kelimanya dulu **tidak ada sama
  sekali di alur belajar**. Layar tata cara langsung dimulai dari niat dan langkah pertama,
  sedangkan wudhu dan arah kiblat hanya dapat dicapai lewat menu lain — sehingga pembaca yang
  mengikuti tombol "Berikutnya" sampai takbir tidak pernah diberi tahu bahwa ada syarat yang harus
  dipenuhi lebih dulu. Kini layar tata cara membuka dengan bagian **"Sebelum sholat"**: tombol
  menuju **Wudhu & Tayamum** dan **Arah Kiblat** (kontennya **ditautkan, tidak disalin** — lafaz
  tetap hanya ada di `wudhu.json`), serta daftar **syarat wajib, syarat sah, persiapan, penutup
  aurat, dan kebersihan tempat sholat** dari berkas baru `sholat_syarat.json`. Di ujung rangkaian
  ada tombol **"Dzikir & doa setelah sholat"** (`plan.txt` menutup langkahnya dengan *"Dzikir dan
  doa setelah sholat"*). Berkas syarat itu **sengaja tanpa satu pun huruf Arab**: isinya daftar
  periksa berbahasa Indonesia, dan aturan proyek melarang menyusun lafaz/hadis tanpa sumber resmi
  — statusnya dinyatakan terbuka di layar lewat `catatan_verifikasi` (kerangka mengikuti Fasolatan
  Kemenag, rincian kata masih menunggu pencocokan dengan modul resmi), bukan disembunyikan.
  `SyaratSholatTest` menolak masuknya karakter Arab maupun kunci `arab`/`latin` ke berkas itu,
  sehingga ia tidak dapat diam-diam berubah menjadi tempat menaruh teks agama yang belum
  terverifikasi. Seluruh butir syarat juga masuk ke indeks **Pencarian**.
- **Tasyahud awal dan tasyahud akhir sebagai dua langkah tersendiri** (`plan.txt` §"Tata Cara Sholat
  Langkah demi Langkah" mendaftar *"Tasyahud awal **jika relevan**"* dan *"Tasyahud akhir"* sebagai
  dua langkah): dulu keduanya satu entri "Tasyahud (awal & akhir)", sehingga pengguna yang
  mengikuti tombol "Berikutnya" tidak pernah diberi tahu bahwa tasyahud awal hanya ada pada sholat
  lebih dari dua rakaat. Kini masing-masing punya keterangan waktunya sendiri, dan mode **Ringkas**
  sengaja melewati tasyahud awal karena ringkasan harus berlaku untuk semua sholat. Pemisahan ini
  **tidak menambah satu pun teks Arab baru**: entri `tasyahud_awal` hanya merujuk lafaz yang sudah
  ada lewat `rujukan_file` (`sholat_bacaan.json#tasyahud_akhir`), mekanisme yang sama yang dipakai
  `niat` dan `fatihah`. Ilustrasi posisinya juga dipisah: duduk akhir punya skema **tawarruk**
  sendiri (`posisi_duduk_akhir.xml`) dengan deskripsi yang menyebut kedua bentuk duduk menurut
  jumlah rakaat, karena sebelumnya langkah tasyahud akhir menampilkan skema **iftirasy** — yang
  justru bukan bentuk duduk akhir pada sholat tiga/empat rakaat. Sholawat Ibrahimiyah ikut
  dipetakan ke duduk akhir, bukan duduk iftirasy.
- **Pengingat waktu iqomah yang tertambat pada pengingat masuk waktu** (`plan.txt` §"Fitur
  Iqomah": *"Tambahkan pengatur waktu Iqomah opsional setelah pengingat masuk waktu sholat. Fitur
  ini hanya berfungsi sebagai pengingat pribadi dan tidak boleh menganggap waktu Iqomah sama di
  semua masjid"*): timer iqomah dulu **hanya hidup selama layarnya terbuka** — persis keadaan di
  mana pengingat semacam ini paling tidak berguna, karena pengguna menutup ponsel lalu
  meninggalkannya untuk berjamaah. Kini ada saklar **"Ingatkan saat perkiraan iqomah habis"** di
  layar Iqomah yang mengantrekan notifikasi `IQOMAH_MIN` menit setelah pengingat masuk waktu,
  berjalan walau aplikasi ditutup, dan ketukannya membuka layar Iqomah. Kalimatnya tidak
  mengklaim waktu masjid mana pun: judulnya "Perkiraan waktu iqomah", isinya menyebut jumlah menit
  yang dipilih pengguna sendiri beserta keterangan bahwa waktu iqomah tiap masjid berbeda. Panjang
  timer kini ikut disimpan saat diubah, dan penjadwalan disusun ulang agar notifikasi latar tidak
  memakai jeda lama yang berbeda dari angka di layar.

- **Pengelompokan sholat sunnah menurut waktu, rakaat, dan tujuan** (`plan.txt` §"Sholat Sunnah":
  *"Kelompokkan sholat berdasarkan waktu pelaksanaan, jumlah rakaat, dan tujuan"*): tab Sunnah dulu
  hanya daftar datar, dan kolom `tujuan` setiap sholat sunnah — sudah ada di bundel konten dan
  lolos gerbang konten sejak awal — **tidak pernah dibaca kode sama sekali**: data yang ditulis,
  divalidasi, lalu tak sampai ke pengguna. Kini tujuannya tampil di kartu dan di layar tata cara,
  dan daftarnya dapat dikelompokkan menurut **Waktu**, **Rakaat**, atau **Tujuan**. Ketiganya
  ditawarkan karena satu cara tidak menjawab semua pertanyaan: "mana yang dikerjakan malam ini"
  berbeda dari "mana yang tujuannya memohon ampunan". Pengelompokannya tidak mengarang keterangan
  baru: rakaat dan tujuan memakai nilai yang sudah tertulis di entri itu sendiri, sedangkan waktu
  memetakan teks `waktu` yang terlalu khusus ("Sepertiga malam terakhir", "Saat masuk masjid") ke
  rentang yang lebih kasar **berdasarkan kata-kata pada teks itu sendiri**. Entri yang tidak
  terbaca tidak ditebak ke rentang mana pun, dan uji memakai daftar sholat sungguhan untuk
  memastikan setiap sholat sunnah jatuh ke satu kelompok bernama serta tidak ada yang hilang atau
  berganda pada ketiga cara.
- **Layar Tentang: kebijakan privasi, izin, lisensi, dan sumber konten** (`plan.txt` §"Pengaturan
  Aplikasi": *"Tentang aplikasi, sumber konten, lisensi, serta kebijakan privasi"*; §"Keamanan dan
  Privasi": *"Dokumentasikan izin Android dan alasan penggunaannya"*): Pengaturan dulu hanya
  memuat satu paragraf ringkas di dalam kode, sedangkan kebijakan privasi dan daftar izin yang
  lengkap hanya ada di berkas `docs/` — **tidak dapat dibaca pengguna aplikasi**. Kini ada layar
  **Tentang** yang memuat identitas aplikasi beserta versinya, **daftar sumber konten**, kebijakan
  privasi, tabel izin beserta alasan dan akibat bila ditolak, dan lisensi. Dokumennya **tidak
  ditulis ulang**: `docs/PRIVACY.md`, `docs/PERMISSIONS.md`, dan `LICENSE` disalin apa adanya ke
  `assets/legal/` dan dijaga tetap identik byte per byte oleh uji, sehingga apa yang dibaca
  pengguna tidak dapat menyimpang dari apa yang didokumentasikan. Sumber konten pun dibaca dari
  `sumber_url` bundel, bukan dari daftar tetap, sehingga layar ini tidak dapat mengklaim sumber
  yang tidak dipakai — dan uji menuntut setiap domain resmi punya keterangan pemiliknya. Markdown
  sederhana (judul, butir, paragraf, tabel) diurai oleh `MarkdownRingkas` agar mengikuti tema
  aplikasi; tabel izin ditampilkan per baris karena empat kolom tidak terbaca di layar ponsel.
- **Bookmark terlihat di layar Favorit** (`plan.txt` §"Pencarian": *"Sediakan favorit, riwayat
  terakhir dibaca, bookmark, dan pencarian terbaru"*; §"Yasin": *"Bookmark ayat"*): bookmark memang
  sudah disimpan di Room sejak lama, tetapi **tidak pernah tampil di satu pun layar daftar** —
  pengguna hanya melihat jumlahnya di Pengaturan, tanpa cara membukanya. Data yang tersimpan tetapi
  tak dapat dijangkau sama saja dengan hilang. Kini layar Favorit punya bagian **Bookmark** dengan
  judul, tujuan, tombol Buka, dan tombol Hapus, sama seperti bagian Favorit; daftarnya dibaca lintas
  jenis (`allBookmarks()`), sebab bookmark ditulis dari layar bacaan mana pun dan jenisnya beragam —
  bukan hanya `yasin`. Bagian ini ikut dalam satu daftar bergulir bersama favorit dan riwayat,
  sehingga tak ada bagian yang mendorong bagian lain keluar layar.
- **Lompat ke ayat yang benar saat pencarian aktif** (`plan.txt` §"Yasin"): tombol "Ke ayat", tombol
  "Lanjut ayat N", dan daftar bookmark dulu menghitung indeks gulir sebagai `n - 1`. Itu hanya benar
  bila tak ada pencarian aktif: daftar yang digulir adalah hasil penyaringan, sehingga dengan
  pencarian aktif ayat ke-n bukan lagi item ke-(n-1) dan pengguna mendarat di ayat yang salah tanpa
  diberi tahu — persis kelas kerusakan "tombol membawa ke tempat yang salah" yang sudah diperbaiki
  pada tombol "Buka" di Favorit. Sekarang nomor ayat dicari pada daftar yang **sedang tampil**;
  bila ayat itu sedang tersaring, penyaringnya dibuka lalu lompatan diulang; bila nomornya di luar
  surat ini (mis. bookmark dari bundel lain), lompatan dibatalkan alih-alih menebak ayat terdekat.
  Ketiga jalur lompat kini lewat satu fungsi, dan `LompatKeAyatTest` menjaga indeksnya tidak kembali
  dihitung mentah.
- **Penjelasan waktu membaca tiap bacaan adzan** (`plan.txt` §"Bacaan Setelah Adzan": *"Penjelasan
  waktu membacanya"*): layar Doa Adzan hanya menyebut urutannya dalam satu kalimat, sehingga
  pengguna yang membukanya saat adzan sedang berkumandang tidak tahu bacaan mana yang berlaku
  **sekarang**. Kini tiap entri di `adzan.json` (`menjawab_adzan`, `sholawat_setelah_adzan`,
  `doa_setelah_adzan`) menyatakan sendiri kapan dibacanya, dan layar menampilkannya sebagai baris
  "Kapan dibaca: …" di bawah judulnya. Waktunya mengikuti urutan yang diperintahkan pada hadis yang
  **sudah dikutip entri itu sendiri** (menjawab → sholawat → doa); yang ditambahkan hanyalah
  keterangan berbahasa Indonesia, bukan lafaz atau hadis baru, dan itu dicatat di
  `docs/CONTENT_SOURCES.md`. Sekaligus, ketiga bacaan itu kini dirender lewat **satu jalur yang
  sama**: sebelumnya catatan hanya dirender untuk sebagian entri, sehingga catatan pada entri lain
  akan hilang tanpa jejak begitu bundel menambahkannya — kelas kerusakan sunyi yang sama dengan
  kunci JSON ganda. Gate konten (`tools/validate_content.py`, dan lewatnya juga
  `tools/validate_bundle.py`) menolak bacaan adzan yang kehilangan `waktu`, fixture `cacat.json`
  ikut memuat contohnya, dan `WaktuBacaanAdzanTest` menjaganya.
- **Indikator status kalibrasi kompas** (`plan.txt` §"Arah Kiblat": *"Indikator status kalibrasi
  sensor"*, *"Petunjuk menggerakkan perangkat membentuk pola angka delapan jika sensor memerlukan
  kalibrasi"*, dan *"Jangan mengklaim arah kompas selalu akurat"*): layar Kiblat dulu hanya
  menampilkan peringatan **saat bermasalah** — dan status awalnya bahkan "terkalibrasi" sebelum
  sensor mengirim satu pun nilai, klaim yang belum terbukti. Kini ada kartu **"Status kalibrasi
  sensor"** yang selalu tampil dengan empat keadaan yang dibedakan jelas: *tidak ada sensor*,
  *menunggu pembacaan* (belum ada nilai sama sekali — bukan berarti sudah terkalibrasi),
  *terbaca & terkalibrasi*, dan *perlu kalibrasi*. Petunjuk pola angka delapan hanya muncul pada
  keadaan yang memang memerlukannya, keadaan "terkalibrasi" tetap disertai pengingat bahwa casing
  magnetik dan logam di sekitar dapat menyimpangkan arah, dan setiap status punya tanda teks
  sendiri sehingga tidak bergantung pada warna saja. Status kalibrasi awal di `KiblatUiState`
  diubah menjadi `null` ("belum diketahui"), dan aturannya dikumpulkan di `KalibrasiKompas`
  beserta ujinya.
- **Tampilan kompas kiblat** (`plan.txt`: *"Pengaturan satuan dan tampilan kompas"* dan *"Dukungan
  perangkat yang tidak memiliki magnetometer melalui tampilan arah berbasis peta atau sudut hasil
  perhitungan, dengan penjelasan bahwa kompas langsung tidak tersedia"*): pengguna kini dapat
  memilih **"Kompas & peta"** atau **"Angka & peta"** — tersedia di layar Kiblat maupun di
  Pengaturan. Lebih penting lagi, **perangkat tanpa magnetometer tidak lagi menggambar jarum**:
  tanpa sensor, sudut hadap perangkat tidak diketahui, sehingga jarum yang tetap digambar hanya
  menunjuk arah dugaan yang tampak meyakinkan. Layar lalu menjelaskan bahwa jarum tidak tersedia
  dan mengarahkan ke angka azimut beserta peta — persis yang diminta `plan.txt`. Jarum juga
  mendapat label `contentDescription` agar pembaca layar tetap menyampaikan arahnya (TalkBack).
  Aturan "jarum butuh sensor **dan** pilihan pengguna" dikumpulkan di satu fungsi dan diuji
  terpisah, termasuk pemilih yang sengaja tidak ditawarkan pada perangkat tanpa sensor.
- **Doa untuk orang yang meninggal di pustaka dzikir** (`plan.txt` §"Pustaka Doa dan Dzikir":
  *"Doa untuk orang yang meninggal"*): layar Dzikir & Doa kini punya kelompok **"Doa khusus"**
  berisi doa jenazah (takbir ke-3) dan doa tambahannya (takbir ke-4), yang sebelumnya hanya dapat
  dibaca di dalam alur sholat jenazah/ghaib. Teksnya **tidak disalin**: entri itu menunjuk berkas
  rujukan (`sholat_mayit.json#doa_mayit`), sehingga hanya ada satu salinan teks terverifikasi di
  seluruh aplikasi dan tidak mungkin ada dua bacaan berbeda dari satu sumber. Resolusi rujukan
  `berkas#id` diperluas ke daftar `bacaan` (sebelumnya hanya `bagian` pada tahlil), dan dijaga dua
  uji: satu memastikan rujukan tidak menjadi tautan mati atau salinan teks, satu lagi memastikan
  setiap entri dzikir benar-benar menghasilkan teks pada konten sungguhan.
- **Tema halaman bacaan** (`plan.txt`: *"Pengaturan jarak antarbaris dan tema halaman"*): bottom
  sheet pengaturan bacaan kini punya pilihan **Ikut tema / Krem / Gelap** yang mengubah "kertas"
  halaman bacaan — krem gading dengan tinta cokelat, atau halaman gelap — terlepas dari tema
  antarmuka, supaya nyaman dibaca lama tanpa mengubah seluruh aplikasi. Hanya warna permukaan dan
  aksen yang diganti (tipografi, ukuran, dan jarak antarbaris tetap), dan setiap pasangan warna
  baru diuji rasio kontras WCAG AA-nya. Pilihan dibaca di akar aplikasi lewat satu penyedia
  bersama, sehingga **setiap** kartu bacaan menghormatinya tanpa pengkabelan per layar; kunci
  preferensinya ikut serta dalam backup/pemulihan.
- **Susunan tahlil yang dapat dikonfigurasi** (`plan.txt`: *"Sediakan susunan bacaan yang dapat
  dikonfigurasi"*, *"Izinkan pengguna memilih susunan bacaan tanpa mengubah teks sumber secara
  otomatis"*, dan *"Jangan menyatakan bahwa satu susunan Tahlil merupakan satu-satunya susunan yang
  benar"*): layar Tahlil kini punya tombol **"Sesuaikan susunan"** yang membuka penyusun berisi
  seluruh bagian tahlil. Pengguna memilih bagian mana yang dibaca, mengatur urutannya, lalu
  menyimpannya sebagai **"Susunan saya"** yang muncul berdampingan dengan susunan lazim di tab
  pemilih susunan. Yang berubah hanya **pilihan dan urutan** bagian — teks tiap bagian tetap dibaca
  apa adanya dari berkas sumbernya beserta atribusinya, sehingga tidak ada teks keagamaan yang
  dikarang atau disusun ulang. Susunan kosong tidak dapat disimpan (akan membuat layar buntu), id
  bagian yang tak dikenal dilewati alih-alih membuat layar gagal, dan teks penjelas menegaskan bahwa
  susunan ini buatan pengguna — bukan susunan satu-satunya yang benar dan bukan pula susunan suatu
  tradisi. Aturannya diuji terpisah (`SusunanSayaTest`) karena penyusunnya sengaja murni tanpa
  Android, dan pilihan tersimpan mengikuti mekanisme backup/pemulihan preferensi yang sudah ada.
- **Pengingat membaca doa setelah adzan** (`plan.txt`: *"Tambahkan tombol untuk menyalin bacaan,
  membagikan doa, menyimpan favorit, dan mengaktifkan pengingat membaca doa setelah adzan"*):
  menu Adzan kini punya saklar yang menjadwalkan pengingat **kedua** lima menit setelah waktu masuk
  — satu per waktu sholat, mengikuti saklar per waktu di atasnya. Ketukan pada notifikasinya
  **membuka layar Doa Adzan**, bukan hanya beranda: intent notifikasi membawa rute tujuan, dan
  aplikasi hanya membuka rute yang dikenal (`Routes.LANGSUNG`, tanpa rute berargumen) sehingga
  permintaan dari luar tak pernah membuat aplikasi berhenti. `MainActivity` memakai `launchMode`
  `singleTop` agar ketukan saat aplikasi sudah terbuka diteruskan lewat `onNewIntent` alih-alih
  menumpuk instance. Jenis pekerjaan kini eksplisit (`adzan`/`pra`/`doa`), dan pekerjaan lama dari
  versi sebelumnya tetap terbaca lewat field `adzan` sebagai cadangan. Saklar yang sama kini juga
  ada di **layar Doa Adzan** — di situlah orang membacanya, jadi mengaktifkannya tidak lagi
  menuntut berpindah ke menu Adzan lebih dulu. Kedua layar menulis kunci preferensi yang sama
  (`Store.DOA_ADZAN_REMINDER`) dan menjadwalkan ulang, sehingga tidak lahir dua sumber kebenaran;
  dijaga `PengingatDoaAdzanTest.saklarPengingatJugaAdaDiLayarDoaAdzan`.
- **Manajemen data offline** (`plan.txt`: *"Manajemen data offline."*): bagian baru di Pengaturan
  menampilkan **jumlah** favorit, bookmark, riwayat, dan progres yang tersimpan di perangkat
  sebelum apa pun dihapus, lalu menyediakan tombol hapus **per bagian**. Menghapus riwayat tidak
  menyentuh favorit dan bookmark, dan sebaliknya; setiap penghapusan lewat dialog konfirmasi yang
  menyebut berapa entri akan hilang, bukan "hapus semua" tanpa peringatan. Dihitung langsung dari
  database (`SELECT COUNT(*)` per tabel) sehingga angkanya tidak menyesatkan, dan uji instrumented
  membuktikan penghapusan tersimpan setelah aplikasi dibuka ulang (`UserDataPersistenceTest`),
  sementara uji unit menjaga agar keempat bagian tetap punya tombolnya sendiri
  (`ManajemenDataOfflineTest`).
- **Pencarian terpadu atas seluruh konten** (`plan.txt`: *"Sediakan pencarian dan akses cepat pada
  halaman yang memiliki banyak konten"*): layar Pencarian kini menyusun indeks dari **semua** modul
  konten — Yasin (per ayat), Tahlil, pustaka doa & dzikir, panduan sholat wajib/sunnah beserta
  ketentuannya, bacaan sholat, panduan bersuci (wudhu/tayamum/pembatal), sholat ghaib & jenazah
  beserta varian niatnya, doa seputar adzan, dan lafaz iqomah. Pencocokan tidak lagi hanya atas
  teks Indonesia: nomor ayat dicocokkan **persis** (mencari "1" tidak lagi memunculkan ayat 12),
  teks Arab dapat dicari, dan cuplikan hasil diambil dari bagian yang memuat kata kunci lalu
  dipangkas dengan elipsis. Indeks dibangun sekali di luar thread UI, dan kegagalannya tidak lagi
  ditelan diam-diam melainkan tampil sebagai pesan kesalahan dengan tombol mencoba kembali; ada
  pula keadaan memuat, keadaan kosong, dan daftar "pencarian terbaru". Aturan pencocokan diuji
  terpisah (`PencarianTest`) karena mesinnya sengaja murni tanpa Android.
- **Pembaruan konten dengan validasi sumber** (`plan.txt`: *"Sediakan mekanisme pembaruan konten
  dengan validasi sumber."*): Pengaturan → Pembaruan konten memungkinkan pengguna memasang berkas
  bundel (`.json`) pilihannya sendiri. Bundel **tidak pernah diunduh otomatis** dan tidak menambah
  izin apa pun — berkas dibuka lewat pemilih berkas sistem, tanpa jaringan. Sebelum dipasang,
  setiap berkas di dalamnya diperiksa: `sumber_url` wajib dari domain resmi (pencocokan host
  persis/subdomain, sehingga domain yang menyerupai pun ditolak), berkas ber-Arab wajib
  menyatakan `kategori` yang dikenal, teks Arab wajib berharakat, dan field `arab`/`latin` tidak
  boleh berisi prosa penunjuk. Satu berkas yang gagal membatalkan seluruh bundel, jadi konten
  yang sudah diverifikasi tidak pernah tercampur setengah pembaruan. Pembaruan disimpan di
  `filesDir/content_update/` dan dibaca lebih dulu oleh `ContentRepo`; berkas yang rusak atau
  kehilangan sumber resmi otomatis diabaikan sehingga aplikasi kembali ke konten bawaan, dan
  tombol "Kembalikan ke konten bawaan" menghapus seluruh pembaruan. Aturannya dijalankan dua
  sisi dari sumber yang sama — `BundelKonten` di aplikasi (dijaga `BundelKontenTest`) dan
  `tools/validate_bundle.py` di CI, yang memanggil pemeriksa `tools/validate_content.py`
  sehingga tidak ada aturan yang disalin ulang. Lihat `docs/CONTENT_UPDATE.md`.
- **Doa & lafaz adzan yang tadinya kosong**: doa keluar masjid (HR Abu Dawud 465), doa masuk
  rumah (HR Abu Dawud 5096 — derajat **daif** menurut Al-Albani, dicantumkan terbuka pada field
  `sumber`), doa bepergian/safar (HR Muslim 1342), serta **lafaz sholawat setelah adzan**
  (HR Bukhari 3370) yang kini tampil di layar Doa Adzan. Semuanya dari sunnah.com, tanpa teks
  yang dikarang. Lafaz menjawab adzan dipindahkan dari kode Kotlin ke `adzan.json`, dan uji baru
  menjaga agar **tidak ada teks Arab hardcoded di seluruh kode aplikasi**.
- **Uji instrumented lengkap** (`app/src/androidTest`) menutup kriteria pengujian `plan.txt`:
  penyimpanan bookmark/progres/favorit/riwayat beserta backup–pulih (Room), mode offline
  (seluruh modul konten terbaca dari aset + aplikasi terbukti tidak meminta izin `INTERNET`),
  izin lokasi dan kiblat tanpa izin, penjadwalan pengingat (jumlah pekerjaan WorkManager per
  saklar waktu & pra-adzan, pembatalan saat pengingat dimatikan, `BootReceiver` terdaftar),
  serta tema terang/gelap dan lebar layar ponsel kecil maupun tablet.
- **Aksesibilitas peta kiblat**: ringkasan peta (rute, jarak, azimut) diumumkan lewat
  `contentDescription` karena labelnya digambar di kanvas — pembaca layar kini memahami peta.
- **Peta arah kiblat (offline)**: peta skematik tanpa internet — menampilkan posisi pengguna,
  Ka'bah, dan rute lingkaran besar di antaranya (`QiblaCalc.greatCirclePath`/`distanceKm` +
  `MapProjection`), menjadi acuan arah bila perangkat tak punya magnetometer atau kompas belum
  terkalibrasi. Bujur di-*unwrap* agar rute yang melewati garis tanggal tetap benar.
- **Ilustrasi gerakan sholat**: setiap langkah panduan sholat kini menampilkan skema posisi
  tubuh (`res/drawable/posisi_*.xml` — garis geometris, tanpa penggambaran figuratif dan tanpa
  teks Arab) beserta deskripsi posisi yang prosedural; pemetaan bacaan→posisi dijaga oleh uji
  (`PostureTest`).
- **Langkah "Surat atau ayat setelah Al-Fatihah"** pada panduan tata cara sholat (rakaat pertama
  & kedua) — sebelumnya langkah ini tidak ada sama sekali meski `plan.txt` mensyaratkannya.
  Teks contohnya (Al-Ikhlas) diambil dari assets MSI Kemenag, dengan catatan bahwa surat/ayat
  lain juga sah dan rakaat ketiga/keempat cukup Al-Fatihah (HR Bukhari 759 — sunnah.com).
- **Jadwal sholat**: pemilihan metode hisab (Kemenag/SIHAT, MWL, Umm al-Qura, Egyptian,
  Makkah), mazhab Asar (Syafi'i/Hanafi), zona waktu, dan koreksi menit per waktu
  (`JadwalViewModel` + `JadwalScreen`).
- **Adzan & pengingat**: pemutaran audio adzan pilihan pengguna di latar melalui
  *foreground service* media; pengingat pra-adzan; opsi tembus mode senyap/DND; pintasan
  izin sistem (notifikasi, optimasi baterai, alarm tepat waktu).
- **Timer iqomah**: hitung mundur nyata dengan getar saat berakhir.
- **Kalender Hijriah**: koreksi hari manual agar selaras dengan isbat Kemenag.
- **Penyusun pengingat** (`PrayerScheduler`) yang membaca lokasi/metode/koreksi tersimpan,
  berjalan harian, dan disusun ulang otomatis setelah reboot (`BootReceiver`).
- Dokumentasi izin (`docs/PERMISSIONS.md`) dan kebijakan privasi (`docs/PRIVACY.md`).
- **Kiblat**: permintaan izin lokasi saat runtime, lokasi GPS live (bukan hanya
  `lastKnownLocation`), sensor rotasi bila tersedia (cadangan akselerometer + magnetometer),
  mode layar penuh, dan tombol "Perbarui lokasi".
- **Jadwal sholat**: tombol "Pakai lokasi GPS" dengan permintaan izin runtime.
- **Aksesibilitas**: teks Arab memakai arah tata letak RTL; ikon bilah bawah punya
  `contentDescription`.
- **Yasin/Tahlil/Dzikir**: ukuran font & jarak antarbaris, penanda ayat terakhir, bookmark,
  pencarian internal, counter persisten (Room), pemilih susunan tahlil, dan mode layar penuh
  yang benar-benar menyembunyikan bilah navigasi.
- **Favorit & Riwayat**: disimpan di Room (tahan rotasi & penutupan aplikasi), dengan
  migrasi sekali dari penyimpanan lama.
- Dependabot untuk dependensi Gradle & GitHub Actions (`.github/dependabot.yml`).
- Berkas `LICENSE` (Apache-2.0) untuk kode aplikasi, dengan catatan bahwa konten keagamaan
  tetap tunduk pada sumber resminya masing-masing; serta `.editorconfig`.
- **Wudhu & Tayamum**: panduan langkah demi langkah (kerangka Fasolatan/Bimas Islam, mazhab
  Syafi'i), daftar pembatal wudhu, dan penanda "sudah dipelajari" yang tersimpan di Room.
  Lafaz Arab sengaja belum dicantumkan — menunggu verifikasi sumber resmi.
- **Backup/pemulihan penuh**: satu berkas memuat preferensi (DataStore) sekaligus favorit,
  bookmark, riwayat, dan progres (Room); tetap kompatibel dengan backup lama.
- **Pemilihan mazhab panduan** (Syafi'i/Hanafi/Maliki/Hambali) yang benar-benar mengubah
  bacaan: entri bertanda mazhab (mis. doa qunut — Syafi'i) hanya tampil bila mazhab cocok;
  bacaan juga dapat dibatasi per sholat (qunut hanya muncul di Subuh). Dapat diubah di
  Pengaturan maupun langsung di layar panduan sholat.
- **Penjelasan khusus sholat**: daftar ketentuan untuk Jumat (siapa yang wajib, khutbah)
  dan keringanan safar (qashar & jamak) kini tampil di panduan sholat. Lafaz niat Jumat
  ditandai belum dicantumkan — menunggu verifikasi sumber resmi.
- **Identitas visual**: ikon peluncur adaptif (bulan sabit & bintang, latar gradasi zamrud),
  termasuk lapisan *monochrome* untuk ikon bertema Android 13+. Nama aplikasi dipindahkan
  ke `strings.xml`.
- **Tema jendela terang/gelap** (`values`/`values-night`): latar jendela disamakan dengan
  palet aplikasi sehingga tidak ada kedipan putih saat dibuka dalam mode gelap; warna bilah
  status/navigasi mengikuti tema. Palet XML dijaga selaras dengan palet Compose lewat uji.
- **Perapian higiene teks otomatis** (`plan.txt` baris 1: *"auto fix"*): sebelumnya tidak ada
  pemasang apa pun di repo ini — alur kerja `autofix.yml` yang lama hanya menjalankan
  `./gradlew tasks`, sehingga tak pernah memperbaiki apa-apa. Kini `tools/format_teks.py` merapikan
  lima hal yang **tidak dapat mengubah perilaku**: akhir baris LF saja, tanpa spasi/tab di akhir
  baris, tanpa baris berisi spasi saja, tidak lebih dari dua baris kosong beruntun, dan tepat satu
  baris baru di akhir berkas. Alat yang sama dipakai di dua tempat — **gate** di `ci.yml` (mode
  periksa) dan **pemasang** di `.github/workflows/autofix.yml` (mode `--fix`) — sehingga aturan
  periksa dan perbaikan mustahil berbeda. Alur kerja itu dijalankan sengaja lewat
  `workflow_dispatch`, plus sapuan mingguan sebagai jaring pengaman `main`, dan **hanya meng-commit
  bila ada temuan**. Ia sengaja **tidak** dipicu `pull_request`: dibuktikan di PR #37 bahwa commit
  bot ke cabang PR membuat GitHub menahan rangkaian run untuk commit itu di `action_required`
  (aktor `github-actions[bot]` bukan kolaborator), sehingga commit perbaikannya justru tidak pernah
  tervalidasi lint/unit/build — melanggar prinsip "setiap commit yang digabung sudah lulus CI".
  Pemicu itu baru bersih dengan token ber-aktor kolaborator (PAT/GitHub App), yang sengaja tidak
  disimpan di repo ini. Pembagiannya: `ci.yml` **mencegah**, alur ini **memperbaiki**. Batas yang
  diketahui (dinyatakan di komentar alur kerja): commit hasil perbaikan dibuat dengan
  `GITHUB_TOKEN`, dan `push` yang dipicu `GITHUB_TOKEN` tidak memunculkan alur kerja baru —
  sehingga commit itu tidak divalidasi `ci.yml`. Diterima karena alat ini hanya menyentuh spasi dan
  baris baru sehingga secara rancangan tidak dapat mengubah perilaku. Alat ini sengaja **tidak**
  menyentuh indentasi, panjang baris, urutan impor, atau isi teks keagamaan.

### Diperbaiki
- **Catatan mazhab iqomah kini tinggal di bagiannya sendiri, dan yang tampil adalah ejaan lengkap**:
  `adzan.json` memuat keterangan *"susunan iqomah dapat berbeda menurut mazhab/tradisi"* **dua kali**
  — di tingkat modul (*"…; perbedaan dicatat apa adanya"*) dan di dalam bagian `iqomah` (*"… (mis.
  jumlah lafaz). Perbedaan dicatat apa adanya"*). Yang dibaca `iqomahCatatan()` adalah salinan
  tingkat modul, sehingga salinan di dalam `iqomah` tak pernah tampil; jadi yang terlihat di layar
  Iqomah bukan sekadar salinan yang lebih pendek, melainkan salinan yang **kehilangan contoh** yang
  menjelaskan apa yang berbeda antar mazhab. Keterangan itu kini dibaca dari bagiannya sendiri
  (`adzan.json#iqomah#catatan_mazhab`), mengikuti konvensi berkasnya yang sudah menaruh
  `catatan_subuh` di dalam bagian `adzan`; salinan tingkat modul dihapus. Dijaga
  `AtribusiLafazAdzanTest` — `catatanPerBagianTinggalDiBagiannya` (kedua arah perpindahan),
  `catatanMazhabIqomahMenyebutContohPerbedaan` (kelengkapan ejaan), dan
  `repoMembacaCatatanMazhabIqomahDariBagiannya` (jalur pembacaannya). Uji tampilnya ikut diperketat:
  ia dahulu hanya mencari `repo.iqomahCatatan()`, yang tetap ada di dalam cabang yang dimatikan
  (`if (false)`), sehingga kini **syarat** tampilnya yang diperiksa.
- **`edisi` tingkat modul Tahlil kini punya satu rumah, dan ejaan yang tampil adalah yang lengkap**:
  keterangan provenans *edisi* hidup **dua kali** di bundel — `tahlil.json` (*"Susunan lazim
  Indonesia; bacaan Qur'an Kemenag 2019, doa/dzikir sunnah.com"*) dan `tahlil_full.json` (*"… bacaan
  Qur'an memakai MSI LPMQ Kemenag + terjemah Kemenag edisi 2019 …"*). Yang dibaca kode hanya
  `tahlil.json`, karena keterangan itulah yang menjadi sumber cadangan tiap susunan di
  `tahlilSusunan()` dan tampil sebagai baris *"Sumber: …"* di layar Tahlil; salinan di
  `tahlil_full.json` tak pernah terbaca sehingga memperbaruinya tak mengubah layar apa pun.
  Akibatnya bukan sekadar salinan mati: ejaan yang **dibaca** justru yang lebih pendek, sehingga
  layar menyebut asal bacaan Qur'annya sebagai *"Kemenag 2019"* saja sementara sumbernya sendiri
  mencatat *Mushaf Standar Indonesia (MSI) LPMQ Kemenag* dan *terjemah Kemenag edisi 2019*. Ejaan
  yang lebih lengkap kini pindah ke `tahlil.json` (berkas yang dibaca) dan salinannya di
  `tahlil_full.json` dihapus — satu keterangan, satu rumah. Dijaga
  `AtribusiSusunanTahlilTest` — `edisiModulTinggalDiSatuBerkasTahlil` (faktanya) dan
  `edisiModulMenyebutMsiLpmqDanEdisiTerjemah` (kelengkapan ejaannya). Penjaga lama
  `repoMembacaSumberSusunanDenganCadanganEdisiModul` ikut dipersempit: ia dahulu hanya mencari
  `optString("edisi"`, pola yang juga muncul di `panduanBersuci()` (wudhu), sehingga tetap hijau
  meski pembacaan `edisi` di `tahlilSusunan()` diganti nilai kosong — mutasi `E` pada
  `mutate64.py` membuktikannya, dan pola utuh `val edisiModul = d.optString("edisi", "")` kini
  menutupnya.
- **Catatan tradisi susunan Tahlil kini punya satu rumah, dan layar Pengaturan ikut membacanya**:
  keterangan *"susunan tahlil dapat berbeda menurut tradisi"* hidup **dua kali** di bundel —
  `tahlil.json` (*"…; bukan satu-satunya versi yang benar"*) dan `tahlil_full.json` (*"… Ini salah
  satu susunan yang lazim di Indonesia, bukan satu-satunya yang benar"*) — sementara kode hanya
  membaca `tahlil.json`, sehingga salinan di `tahlil_full.json` tak pernah tampil dan memperbaruinya
  tak mengubah layar apa pun. Salinan mati itu bahkan sempat menyesatkan: dokumentasi perbaikan ini
  mula-mula mengutip kalimatnya seolah kalimat itu yang ditampilkan. Ejaan yang **lebih lengkap**
  kini pindah ke `tahlil.json` (berkas yang dibaca) dan salinannya dihapus — satu keterangan, satu
  rumah. Pada saat yang sama layar **Pengaturan**, yang sebelumnya mengetik versinya sendiri
  (*"Susunan dapat berbeda menurut tradisi dan rujukan — tidak ada susunan yang diklaim paling
  benar"*), kini memakai `repo.tahlilCatatanTradisi()` yang sama dengan layar Tahlil: satu
  keterangan itu tadinya punya tiga ejaan, dan memperbarui kalimat di berkasnya hanya mengubah dua
  di antaranya. Dijaga `AtribusiModulTest` — `catatanTradisiTinggalDiSatuBerkasTahlil` (faktanya
  tidak boleh punya salinan kedua), `catatanTradisiMenyebutSusunanIniLazimDiIndonesia` (ejaan yang
  lebih lengkap benar-benar yang tersisa), dan `layarPengaturanMembacaCatatanTradisiDariBundel`
  (layar Pengaturan membacanya, dan tidak lagi memuat salinan lamanya). Uji terakhir membaca
  layarnya **tanpa komentar** — KDoc di layar itu mengutip kalimat lama, dan kutipan itu akan
  memuaskan penjaganya seolah salinannya masih hidup.
- **Judul bagian layar Wudhu kini dibaca dari bundel, bukan diketik di layar**: `wudhu.json`
  mencantumkan `wudhu.judul` dan `tayamum.judul`, tetapi `WudhuScreen` mengetik kedua headingnya
  sendiri — dan salinannya sudah menyimpang. Judul tayamum di berkas berbunyi *"Tata cara tayamum
  (pengganti wudhu saat tidak ada air / sakit)"*, sedangkan layar hanya menampilkan *"Tata cara
  tayamum"*; keterangan di dalam tanda kurung itulah yang menjawab kapan bagian itu dipakai, dan
  pengguna tidak pernah melihatnya. Karena salinannya di kode, mengubah judul di berkasnya pun tak
  mengubah layar sama sekali. Kedua judul kini sampai lewat `PanduanBersuci.judulWudhu` /
  `judulTayamum`, dengan cadangan teks di layar hanya sebagai jaring pengaman bila berkasnya
  kehilangan judulnya — persis pola yang sudah dipakai provenans `edisi` pada layar yang sama.
  Dijaga `JudulBagianBersuciTest`, yang juga menolak argumen judul `LangkahBagian` berupa literal
  dan menuntut keterangan *"pengganti wudhu"* benar-benar ada di berkasnya.
- **Label dan rujukan layar sholat mayit kini dibaca dari bundel, bukan diketik di layar**: tiga
  fakta yang berkasnya sudah mencatat dipaksakan ulang sebagai literal di kode, masing-masing
  dengan salinan yang menyimpang. **Pertama**, `sholat_ghaib.json` dan `sholat_jenazah.json`
  memuat `tanpa_ruku_sujud: true` yang tak pernah dibaca — judul layar selalu menambahkan
  ", tanpa ruku/sujud" apa pun isi berkasnya, sehingga modul sholat khusus yang **dengan**
  ruku/sujud akan tetap dijuduli begitu. **Kedua**, `hukum` (`fardhu_kifayah`) juga tak pernah
  dibaca: layar Ghaib mengetik sendiri *"— fardhu kifayah"* di judul langkah pertamanya, padahal
  kartu bacaan takbir 1 sudah menampilkan `sumber` bundelnya yang lengkap. Ketiganya kini satu
  tempat: `TampilanSholatKhusus.judul(nama, takbir, tanpaRukuSujud)` mengikuti bendera berkasnya,
  `TampilanSholatKhusus.hukum(kode)` memetakan kode bundel ke labelnya, dan judul takbir 1 tak lagi
  mengutip hadisnya sendiri — kartu di bawahnya sudah menampilkan kutipan yang bersumber lengkap
  (*"HR Bukhari 756, Muslim 394 (la sholata liman lam yaqra bi fatihatil kitab) — sunnah.com"*),
  sehingga layar tak lagi memuat **dua** kutipan berbeda untuk bacaan yang sama. Dijaga
  `TampilanSholatKhususSatuTempatTest`, yang juga memaku bahwa `langkah` di bundel sepanjang
  urutan takbir bersama.
- **Gate higiene komentar Kotlin berhenti menuduh berkas yang sebenarnya bersih**:
  `check_kotlin_lex.py` melewati literal string mentah dengan berhenti pada tiga kutipan
  **pertama** yang ditemukannya. Bentuk yang dipakai delapan berkas uji —
  `Regex("""[^"]+"""").…trim('"')` — berakhir dengan kutip, jadi penutupnya adalah tiga kutipan
  **terakhir** dari rentetan di ujungnya; sisa kutipan itu terbaca sebagai awal literal biasa,
  apostrof di dalam kode sesudahnya (mis. `ta'awudz` di dalam sebuah KDoc) membuka literal
  karakter palsu yang menelan `/*` berikutnya, dan gate melaporkan `*/` tanpa pembuka pada berkas
  yang tidak cacat. Pemindainya kini mengikuti aturan Kotlin, dan `--uji-diri` — dijalankan CI
  bersama pemindaian biasa — memeriksanya terhadap lima kasus: tiga yang harus lolos (termasuk
  bentuk di atas) dan dua berkas cacat yang harus tetap tertangkap.
- **Provenans modul yang tertulis di bundel kini benar-benar tampil — dan layar tak lagi menyatakan
  lebih daripada sumbernya**: `SOURCES.md` §6 mencatat `edisi` dan `catatan_mazhab` sebagai
  satu-satunya metadata provenans yang **tidak** diperiksa gerbang konten, sehingga kelengkapannya
  hanya terjaga bila kode membacanya. Ternyata tidak: dua di antaranya ditulis di bundel, lalu tak
  pernah sampai ke layar, dan layar mencetak parafrase yang menyimpang sebagai gantinya.
  `sholat_daftar.json#catatan_mazhab` menyatakan qunut Subuh termasuk ketentuan yang *"memiliki
  perbedaan pendapat"*, sedangkan layar rincian langkah sholat menulis *"Indonesia umumnya Syafi'i
  (qunut Subuh sunnah)"* — menyatakan sebagai mapan hal yang sumbernya sendiri catat sebagai
  diperselisihkan. `wudhu.json#edisi` menyebut kerangka *"wudhu & tayamum … Fasolatan/Ngaji
  Fasolatan"*, sedangkan layar Wudhu menulis parafrase yang lebih pendek dan menghilangkan tayamum
  serta "Ngaji Fasolatan". Keduanya kini dibaca lewat `ContentRepo.sholatCatatanMazhab()` dan
  `PanduanBersuci.edisi` lalu ditampilkan apa adanya, dan `AtribusiModulTest` menolak layar yang
  kembali mengarang versinya sendiri.
- **Nomor urut lafaz iqomah kini satu aturan — lafaz yang sama tak lagi tersimpan dengan dua id**:
  lafaz iqomah diberi id kartu dari nomor urutnya (`iqomah:1`, `iqomah:2`, …) karena beberapa
  lafaznya memang berulang dan id yang diambil dari teks Arab membuat favorit satu lafaz menandai
  lafaz lain. Tetapi nomor urut itu dihitung di **dua** tempat dengan dua hasil: layar Iqomah
  memakai `i + 1` sedangkan indeks pencarian memakai indeks mentah `i`. Lafaz yang sama karena itu
  tersimpan sebagai `iqomah:1` di kartunya dan `iqomah:0` di dokumen pencariannya — pengguna yang
  menandai lafaz pertama sebagai favorit lalu menemukannya lewat pencarian melihat dua bacaan
  berbeda untuk lafaz yang sama, tanpa satu pun pesan galat. Aturannya kini tinggal di
  `JenisBacaan.idLafazIqomah` (bernomor urut mulai 1), dipakai kedua tempat, dan
  `JenisBacaanSatuTempatTest` menolak tempat pemakaian mana pun yang menyusun id iqomah sendiri.
- **Jatah tampil daftar riwayat memakai batas bersama, bukan angka di layar**: penjaga
  `BatasRiwayatSatuTempatTest` dahulu hanya menangkap batas **baca** yang dieja
  (`recentHistory(<angka>)`), sehingga jatah **tampil** yang ditulis sendiri lolos. Sudah terjadi:
  Pencarian memotong daftar "pencarian terbaru" dengan `take(10)` sementara
  `BatasRiwayat.DITAMPILKAN` menyatakan `12`, jadi layar itu menampilkan jumlah baris yang berbeda
  dari daftar riwayat lain — daftar yang lebih pendek dari yang seharusnya, tanpa satu pun pesan
  galat. Penjaganya kini juga menolak `take(<angka>)` di layar-layar daftar riwayat.
- **Satu lafaz, satu bacaan — lafaz yang sama tidak lagi diterjemahkan berbeda di dua berkas**:
  bundel konten sengaja tidak menyatukan lafaz yang dipakai berulang, supaya tiap modul dapat
  diperbarui sendiri. Harga dari kemandirian itu adalah salinan, dan salinan yang tak dibandingkan
  pasti menyimpang. **Sudah terjadi**: doa untuk mayit ditulis dua kali — `sholat_mayit.json`
  `bacaan[0]` dan `tahlil_full.json` `bagian[13]`, keduanya ber-`id` `doa_mayit` dan bersumber sama
  (HR Muslim — sunnah.com), dengan lafaz Arab yang identik sampai harakatnya — tetapi terjemahan
  Indonesianya berbeda satu klausa: *"…, rahmatilah dia, …"* di satu berkas dan *"…, berilah dia
  rahmat, …"* di berkas lain. Pengguna membaca doa yang sama di dua layar dan mendapat dua kalimat
  yang tidak persis sama, tanpa satu pun pesan galat. Lebih lebar lagi, `adzan.json` menulis **enam
  lafaz adzan dua kali** — sekali di `adzan.daftar` dan sekali lagi di `iqomah.daftar`, hanya
  berbeda jumlah ulangan — dan keduanya tampil di layar Adzan; sampai hari ini sejalan, tetapi tak
  ada apa pun yang menjaganya. Kedua terjemahan doa mayit kini sejalan (ejaan yang dipakai adalah
  milik modul sholat mayit, karena klausanya memetakan satu-satu ke lafaz Arabnya:
  *ighfir* / *irham* / *'aafi* / *wa'fu*), aturannya tinggal di satu tempat
  (`LafazSama.penyimpangan`, murni tanpa Android), dan `LafazSamaSatuTerjemahanTest` menuntut
  **setiap** lafaz yang muncul di lebih dari satu tempat dibaca dan diterjemahkan dengan kata yang
  sama — salinan baru yang menyimpang tertangkap tanpa perlu didaftar di uji. Aturannya sengaja
  **tidak** disalin ke `tools/validate_content.py` yang memeriksa berkas yang sama: gate itu
  membaca *konstanta* dari Kotlin, bukan logika, sehingga menaruh pemeriksaan ini di sana berarti
  menulis ulang logikanya — persis kekeliruan yang sedang ditutup. Dibuktikan `mutate57.py`:
  **10/10 mutasi tertangkap**, berkas tidak ada yang tertinggal dalam keadaan termutasi.
- **Mode baca dari jauh kini menaikkan ukuran Arab di semua layar yang punya mode itu**: aturan
  `UkuranBacaan.arabBacaJauh` (mode layar penuh, mode fokus, atau mode jamaah menaikkan ukuran Arab
  `+ 8` sp sampai batas `44f`) sudah tinggal di satu tempat dan dijaga agar tak ada layar
  menuliskannya kembali — tetapi **pemakaiannya** tidak dijaga, dan tiga layar yang jelas punya mode
  itu tidak pernah memanggilnya: mode fokus Yasin, layar penuh Dzikir, dan layar penuh Tahlil sendiri
  (Tahlil hanya menaikkannya di mode jamaah). Ketiganya menyembunyikan bilah bawah dan meminta bacaan
  dibaca dari jauh, tetapi teks Arabnya tetap seukuran bacaan biasa — sedangkan di layar Ghaib,
  Jenazah, Iqomah, dan mode jamaah Tahlil teksnya membesar. Pengguna yang menekan "Layar penuh" di
  Yasin atau Dzikir karena itu tidak mendapat apa yang ia dapat di layar lain, tanpa satu pun pesan
  galat. Ketiga layar kini menyalurkan ukurannya lewat aturan bersama (`TahlilScreen` menyatukan
  kedua modenya lebih dahulu lewat `val bacaJauh = jamaah || fullscreen`), dan
  `UkuranBacaanModeBacaTest` menjaga agar setiap layar bermode baca jarak jauh tetap memanggilnya —
  layarnya **ditemukan dari sumber** (`FullscreenEffect(...)` yang juga menampilkan `BacaanCard(`/
  `ArabText(`), bukan didaftar di uji, sehingga layar baru ikut terjaga; kompas Kiblat yang juga
  bermode layar penuh tetapi tak menampilkan teks Arab sengaja tidak dituntut.
- **Bentuk berkas cadangan tinggal di satu tempat, dan pemulihan tak menghapus bagian yang tak ada
  di berkasnya**: berkas cadangan punya dua sisi yang tak pernah bertemu — penulis
  (`SettingsScreen` + `RoomUserDataRepository.exportJson`) dan pembaca (`restoreJson` + layar
  Pengaturan lagi) — dan keduanya mengeja **enam belas** nama kunci JSON secara terpisah
  (`"userData"`, `"favorites"`, `"createdAt"`, …), tanpa satu pun pemeriksaan. Bedanya sunyi, dan
  akibatnya lebih besar daripada kehilangan satu pengaturan: `restoreJson` mengosongkan keempat
  tabel **lebih dahulu**, baru mengisi ulang dari berkas, sehingga satu nama kunci yang meleset
  berakhir dengan seluruh favorit, bookmark, riwayat, dan progres terhapus, nol baris dimasukkan,
  dan tak satu pun pesan galat — pengguna menekan "Pulihkan" dan kehilangan seluruh datanya,
  termasuk data yang tidak diminta diganti. Bentuknya kini tinggal di `FormatCadangan`, dan
  pemulihan hanya menyentuh bagian yang **benar-benar ada** di berkas: berkas yang memuat
  preferensi tanpa `userData` — atau versi lain, atau berkas separuh — tak lagi mengosongkan apa
  pun. Dijaga `FormatCadanganSatuTempatTest` (penulis dan pembaca harus memakai kunci yang sama
  **beserta jumlah kemunculannya**, tiap pengosongan harus membaca lariknya lebih dahulu dan
  bergantung pada keberadaannya, dan tak ada kunci yang dieja kembali di tempat pemakaian), dengan
  uji instrumented `UserDataPersistenceTest` membuktikan berkas sebagian tak menghapus bagian lain.
  Nama keempat larik yang kebetulan sama dengan nama tabel Room sengaja **tidak** disatukan: nama
  tabel adalah urusan skema penyimpanan, nama kunci adalah urusan berkas yang keluar dari aplikasi.
- **Jumlah riwayat di beranda bukan jumlah riwayat, dan batas daftar riwayat kini satu rumah**:
  kartu ringkasan beranda menulis *"Favorit: 12 • Riwayat: 50"*, tetapi kedua angka di baris yang
  sama itu dihitung dengan cara berbeda — favorit dari daftar **penuh** (`favorites()`), sedangkan
  riwayat dari `recentHistory(50).size`, yaitu ukuran daftar yang sudah **dipotong** jatah bacanya.
  Pengguna dengan lebih dari 50 riwayat diberi tahu "Riwayat: 50", sementara layar "Manajemen data
  offline" menyebut angka yang sebenarnya karena membacanya dari `ringkasan()` (`COUNT(*)`): satu
  fakta, dua jawaban, tanpa satu pun pesan galat. Beranda kini memakai `jumlahRiwayat()` — `COUNT(*)`
  sebagai aliran — sehingga angkanya benar **dan** ikut berubah saat riwayat bertambah. Angka
  batasnya sendiri tak lagi dieja di tempat pemakaian: `recentHistory(50)` di beranda dan Pencarian,
  `recentHistory(60)` di Favorit, lalu jatah tampil dipotong `20` di ViewModel Favorit dan `12` lagi
  di layarnya — hubungan "baca longgar → saring → potong" tak terlihat dari mana pun, dan
  membalikkannya tidak berbunyi apa pun. Semuanya kini tinggal di `BatasRiwayat` (`DIBACA_DAFTAR`,
  `DIBACA_RINGKAS`, `DITAMPILKAN`), dengan hubungan antarangka itu dipaku sebagai aturan. Bawaan
  `recentHistory(limit = 50)` yang **tak satu pun** pemanggil pakai ikut dihapus, supaya tak ada lagi
  kebijakan yang tampak berlaku padahal bukan. Dijaga `BatasRiwayatSatuTempatTest`; uji instrumented
  `UserDataPersistenceTest` membuktikan `jumlahRiwayat()` menghitung seluruh riwayat sementara daftar
  terbaru tetap dipotong jatah bacanya.
- **Cadangan teks bacaan tinggal di satu tempat**: bacaan pada bundel konten boleh menunjuk teksnya
  ke entri lain (`rujukanFile`, mis. `surah_001.json#1`), dan rujukan itu boleh gagal — berkasnya
  hilang, id-nya berganti, atau bundelnya lebih tua daripada kodenya.
  `ContentRepo.resolveArabLatinTerjemah` mengembalikan teks **kosong** untuk rujukan yang tak dapat
  ditelusuri, bukan melempar, sehingga tiap layar harus tahu apa yang ditampilkan bila rujukannya
  kosong: teks milik entri itu sendiri. Aturan cadangan itu dulu hidup di **lima** tempat — dua
  layar sholat mayit (Ghaib, Jenazah) memanggil salinannya lewat `TampilanSholatKhusus.teksBacaan`,
  sementara layar Panduan Sholat, rincian langkah sholat, dan layar Tahlil masing-masing mengeja
  `Triple(it.first.ifBlank { b.arab }, …)` sendiri. Kelimanya hari ini menghasilkan hal yang sama —
  dan justru itulah yang membuatnya berbahaya: satu perubahan aturan (mis. ikut memangkas spasi,
  atau memakai judul entri sebagai cadangan) yang dikenakan pada satu salinan saja tidak berbunyi
  apa pun, dan pengguna membaca teks yang berbeda dari layar yang menampilkan entri yang sama.
  Aturannya kini satu rumah, `core/data/TeksBacaan`, dengan dua bentuk entri (`Bacaan` dan
  `TahlilItem`) dan ketiga medan dipilih **sendiri-sendiri** — sebab satu rujukan boleh memuat
  Arabnya tetapi tidak terjemahannya, dan memilih ketiganya sekaligus akan membuang terjemahan entri
  yang justru ada. Dijaga `TeksBacaanSatuTempatTest`, yang memaku aturannya sekaligus menuntut
  kelima layar memakainya dan tak satu pun mengeja `ifBlank { … .arab|latin|terjemahan }` kembali;
  penjaga lama `TampilanSholatKhususSatuTempatTest` kini menyebut rumah barunya, bukan lagi
  `teksBacaan` yang sudah tidak ada.
- **Hitung mundur *"Berikutnya: …"* tinggal di satu tempat**: jeda ke waktu sholat berikutnya
  dahulu **dieja tiap layar sendiri-sendiri** meski waktu berikutnya sudah lama datang dari satu
  tempat (`JadwalHariIni.berikutnya`). Beranda menulis `$sisaMenit / 60` lalu `j` dan
  `$sisaMenit % 60` lalu `m lagi` (`2j 15m lagi`); layar Jadwal menulis pembagian yang sama dengan
  satuan `jam` dan `mnt` (`dalam 2 jam 15 mnt`) — dua pembagian yang sama, dua tulisan yang berbeda
  untuk satu jeda. Keduanya juga **selalu menyebut jamnya**, sehingga jeda sepuluh menit terbaca
  *"0 jam 10 mnt"*: jam yang tidak ada, disebut. Ejaannya kini satu, `FormatWaktu.durasiJamMenit`,
  yang memilih satuannya menurut nilainya — jamnya hilang bila nol, menitnya hilang bila pas,
  `kurang dari 1 mnt` bila jedanya nol — seperti orang mengucapkannya. Aritmetika menit dindingnya
  juga kembali ke aturan bersama: `HomeScreen` dan `JadwalViewModel.recompute` menulis
  `h * 60 + m` sendiri, padahal penjadwal pengingat sudah memakai `PrayerCalc.menitHari`; salinan
  itu dapat menyimpang tanpa satu pun pesan galat, dan yang dipertaruhkan bukan kerapian — jam yang
  dihitung layar harus sama dengan jam yang dijadwalkan. Yang paling sunyi: `JadwalViewModel` mengisi
  `nowMin` **hanya saat layar dimuat** (`recompute`), sehingga baris *"Berikutnya: …"* membeku pada
  menit pengguna membuka layar sementara hitung mundur beranda berdetak tiap detik — dua perilaku
  untuk satu baris yang sama. Kini `JadwalViewModel` punya detak jam sendiri (`detak()`) yang
  menyegarkan `nowMin` tiap detik lewat `delay(1_000)` dan menghitung ulang jadwalnya saat tanggal
  berganti, dan `recompute` menulis lewat `update` agar detak itu tidak menimpa pilihan kota yang
  dibaca sebelum pengguna memilihnya. Dijaga `HitungMundurBerikutnyaSatuTempatTest` (ejaan, pemakaian
  oleh kedua layar, dan detak jamnya) dan `MenitHariSatuTempatTest.layarMemakaiMenitHariBersama`
  (tak ada `h * 60 + m` yang dieja sendiri di luar `PrayerCalc.menitHari`).
- **"Tampil Latin" dan "Tampil terjemahan" berlaku di setiap layar bacaan**: ketiga kunci
  `SHOW_ARAB`/`SHOW_LATIN`/`SHOW_TRANS` menjawab satu pertanyaan yang sama — bagian mana dari
  bacaan yang ditampilkan — tetapi hanya `showArab` yang dihormati semua layar. Tujuh layar
  (Sholat, SholatDetail, Ghaib, Jenazah, Adzan, Iqomah, Doa adzan) menuliskan `true` langsung di
  setiap pemanggilan `BacaanCard`, sementara Yasin, Tahlil, dan Dzikir membacanya dari preferensi.
  Akibatnya pengguna yang mematikan "Tampil terjemahan" di Pengaturan **masih membaca terjemahan**
  di ketujuh layar itu — dan di sana tombolnya pun tak pernah muncul di "Pengaturan bacaan", sebab
  `BacaanSettingsSheet` menerima keduanya sebagai parameter **opsional** dengan alasan "layar yang
  tak punya bacaan Latin/terjemahan tidak menampilkan chip yang tak berefek"; alasan itu keliru,
  ketujuh layar itu memang menampilkan keduanya. Kini ketujuh layar memegang `showLatin`/`showTrans`
  sebagai keadaan yang dimuat dari `Store.SHOW_LATIN`/`SHOW_TRANS` (bawaan
  `BawaanPreferensi.TAMPIL_LATIN`/`TAMPIL_TERJEMAHAN`), mengopernya ke setiap `BacaanCard`, dan
  menyediakan sakelarnya di sheet — bentuk opsionalnya dihapus sehingga layar tak punya cara lagi
  menghilangkannya tanpa jejak. Dijaga `ReadingDisplayPrefsTest`, yang kini memaku aturan yang sama
  untuk ketiga kunci sekaligus (termasuk bentuk **wajib** parameter sheet, dan jumlah pemanggilan
  yang harus ditemukan agar penjaganya tidak hijau tanpa memeriksa apa pun). Satu jalur pelanggaran
  lolos dari pencarian pemanggilan dan baru ketahuan di runner: layar Doa Setelah Adzan merender
  kartunya lewat fungsi pembantu `KartuDoa`, dan fungsi itu menerima `showArab` saja — pemanggilan
  `BacaanCard` di dalamnya tetap memuat `showLatin`/`showTrans`, jadi penjaganya hijau, tetapi
  nama-nama itu di luar jangkauan dan layarnya gagal dikompilasi. Parameter fungsi pembantu karena
  itu ikut dipaku: fungsi di layar fitur yang menerima `showArab: Boolean` harus menerima
  `showLatin: Boolean` dan `showTrans: Boolean` juga.
- **Rentang volume suara adzan punya satu rumah**: rentangnya dahulu tidak dinyatakan di mana pun —
  ia hidup sebagai `coerceIn(0f, 1f)` yang ditulis **tiga kali**, sekali di `AdzanPlayer.playOnce`
  dan dua kali di `AdzanPreview` (`play` dan `setVolume`), sementara slider di layar Adzan bahkan
  tidak punya `valueRange` sehingga batas yang dapat dipilih pengguna hanya **kebetulan** cocok
  dengan bawaan Compose. Yang membuatnya berbahaya bukan panjangnya, melainkan **jalur baca yang
  tidak menjepit sama sekali**: `Store.ADZAN_VOL` dibaca mentah oleh layar Adzan dan oleh pekerja
  latar. Selama nilainya selalu datang dari slider, ketiga salinan itu tampak sepakat; begitu nilai
  datang dari luar slider — backup yang disunting, atau pemulihan dari versi lain — layar
  menampilkan `"Volume: 500%"` sementara pemutarnya membunyikan 100%, dua angka untuk satu
  pengaturan tanpa satu pun galat yang menyebut sebabnya. Pola ini sama persis dengan rentang jeda
  pra-adzan yang sudah lebih dahulu dipusatkan ke `PengingatAdzan.preMenit`. Kini `VOL_MIN`/
  `VOL_MAKS`/`volume(nilai)` tinggal di `PengingatAdzan`, dan **seluruh** jalur memakainya:
  `AdzanPlayer`, kedua metode `AdzanPreview`, `valueRange` slider, nilai awal layar, serta kedua
  pembacaan nilai tersimpan (layar Adzan dan `ReminderWorker`). Dijaga `VolumeAdzanSatuRentangTest`
  — yang juga memaku bahwa `coerceIn(0f, 1f)` milik **progress bar dan alpha** tidak ikut tertarik
  ke aturan ini, sebab aturannya lain.
- **Nilai awal state preferensi di layar sama dengan bawaannya**: setiap layar memegang pengaturan
  dalam `remember` dan mengisinya dari `store.get(Store.K, BAKU)` di dalam `LaunchedEffect`. Ada
  **dua** nilai yang menjawab pertanyaan yang sama ("apa isi pengaturan ini bagi pengguna yang belum
  menyentuhnya?") — `BAKU`, dan nilai awal `mutableStateOf(...)` yang tampil selama pemuatan belum
  selesai. Empat belas pasangan di tujuh layar menjawabnya dengan **literal**, bukan dengan bawaannya.
  Kerusakannya sunyi berupa kedipan: layar sempat menampilkan pengaturan lain lalu berubah. Dua di
  antaranya sudah pernah menjadi kenyataan: layar Adzan menulis `PrayerCalc.Method.KEMENAG` sebagai
  nilai awal `metode` — tepat hardcode "selalu Kemenag/SIHAT" yang komentar di atasnya sendiri
  menyebut sudah diperbaiki, sehingga layar itu masih sempat berbohong kepada pengguna yang memilih
  metode lain — dan `showArab` ditulis `true` di tujuh layar padahal Pengaturan memakai
  `BawaanPreferensi.TAMPIL_ARAB`, sehingga mengubah konstanta itu menyembunyikan tulisan Arab di
  Pengaturan sementara tujuh layar lain masih sempat menampilkannya. Kini setiap nilai awal diambil
  dari bentuk yang **sama persis** dengan pemuatannya — `BawaanPreferensi.TAMPIL_ARAB`,
  `PengingatAdzan.parseOn(PengingatAdzan.ON_DEFAULT)`, `PrayerCalc.Method.from(JadwalHariIni.METODE_DEFAULT)`,
  `DaftarSuaraAdzan.parse(BawaanPreferensi.DAFTAR_SUARA_ADZAN)`, dan seterusnya. Penjaga
  `NilaiAwalPreferensiTest` menuntut, untuk setiap `var N by remember… { mutableStateOf(AWAL) }` yang
  dimuat dari `store.get(Store.K, BAKU)`, bahwa `AWAL` adalah `BAKU` itu sendiri atau seluruh bentuk
  pemuatannya dengan `store.get(...)` diganti `BAKU`; sisi pemuatan pun dijaga, sehingga bawaan yang
  ditulis ulang sebagai literal di sana (`store.get(Store.SHOW_ARAB, true)`) ikut tertangkap. Nilai
  awal state yang bukan berasal dari `store.get` (state murni tampilan) dan state ViewModel yang
  dibaca lewat `Store.flow` sengaja di luar cakupan, sebab di sana tak ada sepasang pun untuk
  dibandingkan — batas itu dinyatakan di KDoc kelasnya.
- **Daftar izin lokasi tinggal di satu tempat**: layar Kiblat dan Jadwal masing-masing mengetik
  `arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)` **dua kali** — empat salinan untuk satu
  keputusan. Yang membuat salinan itu berbahaya bukan panjangnya, melainkan pasangannya:
  `LocationProvider.hasPermission` menilai "izin sudah ada" dari daftar yang sama, sementara
  Android 12+ membiarkan pengguna menyetujui **perkiraan saja** (hanya izin kasar). Bila satu layar
  berhenti meminta salah satu izin, layar itu tak pernah lagi dianggap berizin meski pengguna sudah
  menyetujui dialognya — tombol GPS-nya diam, tanpa satu pun pesan galat. Daftarnya kini tinggal di
  `LocationProvider.IZIN` (bentuk `List`, bukan `Array`, supaya tak ada pemanggil yang dapat
  mengubahnya diam-diam) dan diserahkan ke peluncur izin lewat `toTypedArray()`; `hasPermission`
  diturunkan darinya lewat `IZIN.any`. Penjaganya memaku tiga hal sekaligus: daftar itu hanya
  dieja di `LocationProvider.kt`, kedua layar benar-benar memakainya di **kedua** tempat
  permintaannya, dan `AndroidManifest.xml` menyatakan izin lokasi yang sama — `uses-permission`
  yang hilang membuat permintaannya ditolak sistem tanpa suara. Penjaga "satu tempat" itu sengaja
  dibatasi pada izin **lokasi** (`Manifest.permission.ACCESS_*`); izin lain seperti notifikasi
  punya aturannya sendiri.
- **Judul langkah takbir yang sama tinggal di satu tempat**: layar Sholat Ghaib dan Sholat Jenazah
  menyebut takbir 2–4 dengan **judul yang sama persis** — *"Takbir 2 → Sholawat Ibrahimiyah"*,
  *"Takbir 3 → Doa untuk mayit"*, *"Takbir 4 → Doa tambahan"* — tetapi ketiganya diketik di kedua
  layar. Dua salinan berarti satu perbaikan ejaan harus dikenakan dua kali, dan bila hanya satu
  yang dikenai, dua layar menyebut langkah yang sama dengan dua nama berbeda tanpa galat apa pun.
  Ketiganya kini tinggal di `TampilanSholatKhusus.judulTakbirBersama`, dan tiap layar
  menggabungkannya dengan judul yang **benar-benar khas** baginya lewat
  `TampilanSholatKhusus.judulLangkah` — Ghaib menambahkan ta'awudz pada takbir 1 dan arah toleh
  pada salam, Jenazah tidak. Entri khas yang menimpa entri bersama **ditolak**, bukan dibiarkan
  menang diam-diam, supaya perbedaan yang disengaja tetap disengaja. Penjaga lamanya hanya
  membandingkan kedua peta, sehingga satu perubahan yang dikenakan pada **kedua** salinan lolos;
  penjaga barunya memaku cakupan seluruh urutan takbir (satu id tanpa judul menjatuhkan layarnya,
  bukan sekadar menghilangkan kartunya), menolak salinan yang hidup lagi di layar, dan memaku
  ejaan ketiga judul bersama itu.
- **Gerbang kesiapan emulator diukur dengan perintah yang sama dengan pembacanya**: E2E `main`
  `38366620795` merah tanpa menjalankan satu uji pun, dan sebabnya bukan uji yang gagal. Gerbang
  kesiapan menunggu sampai **satu** properti terbaca (`adb shell getprop ro.build.version.sdk`),
  sementara ddmlib — lewat AGP — membaca **seluruh** tabel properti (`adb shell getprop` tanpa
  argumen) dengan batas waktu 2 detik (`PropertyFetcher.GETPROP_TIMEOUT_SEC`). Perintah yang kedua
  jauh lebih berat, jadi gerbangnya bisa lulus sementara pembacaan penuh masih kehabisan waktu:
  lognya melaporkan `API='29'` **dua kali**, lalu AGP tetap berkata *"Unknown API Level"* **dua
  kali** dan melewati perangkat. Kini keduanya memakai satu perintah (`perintah_siap`), dibatasi
  `BATAS_PROPERTI` yang dipaku pada bawaan ddmlib.
  Jalur ulangan pun berhenti menunggu dengan angka tetap (`sleep 15` — lama tunggu yang tak
  berhubungan dengan apa pun yang diperiksa) dan menunggu **bukti**: tiga pembacaan penuh
  berturut-turut yang semuanya selesai dalam batas itu. Sebab sebenarnya kegagalan itu juga
  ditemukan dan ditutup: runner menyediakan `/dev/kvm` tetapi tidak membukanya untuk pengguna
  `runner` (`ProbeKVM: This user doesn't have permissions to use KVM (/dev/kvm)`), dan aksi
  emulator memutuskan lewat `accessSync('/dev/kvm', R_OK | W_OK)` — sehingga ia memaksa
  `-accel off` dan emulator berjalan dengan emulasi perangkat lunak yang begitu lambat sampai
  pembacaan properti kehabisan waktu. Izinnya kini dibuka **sebelum** aksi emulator dipanggil
  (dijaga `|| true`, supaya `/dev/kvm` yang memang tak ada tidak menjatuhkan job). Dijaga
  `SkripE2eSiapPerangkatTest`, yang juga menolak gerbang kembali membaca satu properti, menolak
  batas waktunya dilonggarkan, menolak `sleep 15` di jalur ulangan, dan menuntut izin `/dev/kvm`
  dibuka sebelum aksi emulator.
- **Aritmetika "menit ke hari" tinggal di satu tempat**: angka `1440` diketik telanjang di **tiga**
  berkas — `PrayerCalc.compute` melipat hasil hisab dengannya, `JadwalHariIni.geser`/`berikutnya`
  memutar jam melewati tengah malam dengannya, dan `PrayerScheduler.enqueueNext` menghitung jeda
  pengingat dengannya (`if (d <= 0) d += 1440`, ditulis **dua kali** dalam satu fungsi). Yang
  dipertaruhkan bukan kerapian: jeda itulah yang menentukan **kapan pengingat berbunyi**, sementara
  jam yang sama menentukan **apa yang terbaca pengguna** — satu salah ketik (`1439`, atau lipatan
  yang lupa menangani sisa bagi negatif) membuat pengingat berbunyi pada menit yang tidak sama
  dengan jam di layar, tanpa satu pun pesan galat. Kini angkanya satu (`PrayerCalc.MENIT_SEHARI`),
  lipatannya satu (`PrayerCalc.putaranMenit`), menit dindingnya satu (`PrayerCalc.menitHari`, yang
  juga dipakai `toMinutes`), dan aturan jeda ke kemunculan berikutnya satu
  (`JadwalHariIni.sisaMenit`). Yang terakhir memuat kasus tepi yang dahulu tak terdokumentasi: jam
  yang **tepat** sekarang dibulatkan ke **depan**, bukan menjadi jeda nol — pekerja pengingat
  menjadwalkan ulang dirinya sesudah berbunyi, sehingga jeda nol berarti pengingat yang berbunyi
  berulang tanpa henti. Jeda pra-adzan kini dihitung dari **jam yang ditampilkan layar**
  (`JadwalHariIni.geser(jam, -preMin)`) alih-alih dari `delta - preMin` yang ditulis ulang
  penjadwal. Dijaga `MenitHariSatuTempatTest`, yang juga menolak `1440` diketik di berkas lain.
- **Ejaan angka waktu tinggal di satu tempat**: format `"%02d:%02d"` ditulis ulang di **tiga**
  berkas — `PrayerCalc.compute` saat menyusun waktu sholat, `JadwalHariIni.geser` saat menggesernya,
  dan layar Iqomah saat menghitung mundur — sehingga tak ada satu pun tempat yang tahu ejaan mana
  yang benar. Dua yang pertama menghasilkan **jam** yang sama, jadi salinan yang menyimpang membuat
  jam yang dihitung dan jam yang digeser disebut dengan tulisan berbeda (mis. `%d:%02d` menjadikan
  "05:07" terbaca "5:07" hanya di satu layar). Yang ketiga menghasilkan **durasi**, sehingga
  menyalin ejaannya begitu saja menyembunyikan bahwa angkanya berarti lain: "01:30" pada hitungan
  mundur iqomah adalah satu menit tiga puluh detik, bukan pukul satu lewat tiga puluh menit. Ejaannya
  kini satu di `FormatWaktu` — `pasanganAngka` satu-satunya format string, `jamMenit` untuk jam
  (kebalikan `PrayerCalc.toMinutes`, sengaja **tidak** melipat melewati tengah malam karena lipatan
  adalah urusan pemanggil), dan `menitDetik` untuk durasi yang **sengaja bernama beda** supaya kedua
  makna tak pernah tertukar tanpa ketahuan. Dijaga `FormatWaktuSatuTempatTest`, yang juga menolak
  format string waktu ditulis di berkas lain.
- **Aturan teks dua layar sholat mayit tinggal di satu tempat**: layar Sholat Ghaib dan Sholat
  Jenazah menampilkan **medan yang sama** dari berkas kontennya (`sholat_ghaib.json` /
  `sholat_jenazah.json`) — judul, jumlah takbir, syarat, dalil, catatan mazhab, niat, dan langkah
  takbir — tetapi tiap layar merangkainya sendiri, dan salinannya sudah menyimpang **tanpa satu pun
  perbedaan tercatat sebagai pilihan**: syarat yang sama dipisah `"; "` di Ghaib tetapi `", "` lalu
  diberi titik di Jenazah, sehingga satu daftar tampak seperti dua aturan berbeda; dalil yang
  dimuat `sholat_jenazah.json` **tak pernah tampil** di layarnya, padahal Ghaib menampilkan medan
  yang sama — konten yang ditulis, divalidasi gerbang konten, lalu tak sampai ke pengguna; judul
  layar menyebut *"tanpa ruku/sujud"* di Ghaib saja, padahal dalil kedua entri menyatakan hal yang
  sama; dan terjemahan niat ditampilkan di Jenazah tetapi disembunyikan di Ghaib
  (`showTrans = false`) padahal kedua berkas memuat terjemahannya. Perangkaian teksnya kini satu
  tempat di `TampilanSholatKhusus` (`judul`, `langkah`, `syarat`, `dalil`, `catatanMazhab`,
  `teksBacaan`) dan dipakai kedua layar. Yang **khas tiap layar** tetap tinggal di layarnya: peta
  judul langkah takbir (Ghaib membaca ta'awudz sebelum Al-Fatihah dan salamnya menoleh dua sisi,
  Jenazah tidak) dan label chip layar penuh (`LAYAR_PENUH_IMAM` vs `LAYAR_PENUH`) — perbedaan yang
  disengaja, dan `TampilanSholatKhususSatuTempatTest` memakunya supaya tidak "dirapikan" menjadi
  salinan.
- **Kata tindakan baku tinggal di satu tempat**: empat kata yang dipakai beberapa layar — *"Batal"*
  (tombol dialog di Adzan, Pembaruan konten, dan Pengaturan, serta pembatal timer Iqomah),
  *"Hapus"* (jenis suara adzan, baris favorit/bookmark, dan satu bagian data di Pengaturan),
  *"Reset"* (penghitung di `UiKit` dan timer Iqomah), dan *"Buka"* (kartu bacaan di `UiKit`, baris
  tersimpan di Favorit, dan hasil Pencarian) — masing-masing dieja sendiri di tiap tempatnya.
  Karena singkat dan umum, salinannya mudah menyimpang tanpa ketahuan: mengganti kata di satu layar
  meninggalkan layar lain menyebut tindakan yang sama dengan kata lain. Keempatbelas pemakaian kini
  lahir dari `NamaAntarmuka` (`BATAL`, `HAPUS`, `RESET`, `BUKA`) — *"Batal"* sengaja tetap terpisah
  dari `BATALKAN_TANDA` karena yang dijanjikan tombolnya berbeda (menutup dialog vs membatalkan
  tanda progres). Dijaga `NamaAntarmukaSatuTempatTest`, yang memastikan tiap layar tetap memuat
  kendalinya **dan** memakai nama bersama. Dua penjaga lama yang mencari **kata** itu sendiri —
  `ManajemenDataOfflineTest` (`layar.contains("\"Batal\"")`) dan `JenisSuaraAdzanTest`
  (`s.contains("Hapus")`) — kini menunjuk `NamaAntarmuka.BATAL`/`NamaAntarmuka.HAPUS`: yang pertama
  gagal begitu katanya pindah ke satu tempat (justru karena ia mencari ejaan, bukan pemakaiannya),
  dan yang kedua tetap hijau walau layarnya mengeja ulang kata itu sehingga tak lagi membuktikan
  apa pun. `kataTindakanTidakDiejaUlangDiUji` (**baru**) menutup celah itu: tak satu pun uji boleh
  mengeja ulang keempat kata sebagai literal, karena penjaga seperti itu kehilangan tepat hal yang
  seharusnya dijaganya.
- **Penyiapan emulator E2E tinggal di satu tempat**: skrip uji instrumented menyusun urutan
  penyiapan perangkat — menunggu perangkat, menunggu boot selesai, memastikan properti API terbaca
  — di **dua** jalur, dan salinan keduanya menyimpang. Jalur pertama memanggil ketiganya; jalur
  **ulangan** (yang dipakai saat emulator tak responsif) hanya menunggu boot. Bila boot sudah
  selesai sementara adb belum bisa membaca properti, `tunggu_boot` langsung lewat, AGP membaca API
  level sebagai `1`, dan pemasangan APK ditolak — *"Cannot install split APKs with API level < 21"*.
  Run `main` `38352153888` merah karena itu: **0 uji berjalan**, dan yang menunggu hanya
  menyimpulkan ujinya merah padahal tidak ada uji yang dijalankan. Urutannya kini hidup di satu
  fungsi `siapkan_perangkat()` yang dipanggil kedua jalur, dan API level diperiksa harus berupa
  angka sebelum diteruskan ke AGP. Dijaga `SkripE2eSiapPerangkatTest`.
- **Kendali langkah dan tindakan tinggal di satu tempat**: enam kata yang dipakai bersama beberapa
  layar — *"← Sebelumnya"* dan *"Berikutnya →"* (panduan sholat dan susunan Tahlil), *"Batalkan
  tanda"* (rincian sholat dan Wudhu), *"Kosongkan"* (penyaring Panduan Sholat dan Dzikir),
  *"Lanjutkan"* (beranda dan daftar favorit), serta awalan *"Terakhir dibaca: …"* (beranda,
  favorit, dan Yasin) — masing-masing dieja sendiri di tiap layar yang memakainya. Tiap pasangan
  menamai **satu** tindakan, jadi kata yang berbeda di salah satunya membuat pengguna mengira ada
  dua tombol berbeda untuk hal yang sama; mengganti kata di satu layar meninggalkan layar lain
  menyebut tindakan itu dengan kata lama. Ketigabelas pemakaian kini lahir dari `NamaAntarmuka`,
  sehingga judul tombol dan tindakan yang dijanjikannya tak dapat berbeda pendapat. Dijaga
  `NamaAntarmukaSatuTempatTest`, yang memastikan tiap layar tetap memuat kendalinya **dan**
  memakai nama bersama — bukan menyelesaikan salinan dengan menghapus salah satu tombol.
- **Nama layar yang dibuka dari Pengaturan tinggal di satu tempat**: berkas `NamaLayar` menyebut
  Pembaruan konten dan Tentang sebagai pengecualian, dengan alasan bahwa tombolnya menyebut
  **tindakannya** ("Buka pembaruan konten"), bukan nama layarnya. Alasan itu tidak bertahan begitu
  diperiksa: tiap bagian di Pengaturan diawali **judul bagian** yang menyebut layar tujuannya, jadi
  namanya memang muncul lebih dari sekali — dan sudah menyimpang. Layar Tentang ditulis **tiga**
  cara sekaligus (judul layarnya *"Tentang aplikasi"*, judul bagian di Pengaturan *"Tentang &
  sumber"*, tombolnya *"Buka Tentang & privasi"*), sementara Pembaruan konten memakai huruf
  besar-kecil yang berbeda antara judul bagian dan tombolnya. Dua uji bahkan **memaku** kedua ejaan
  Tentang itu — satu menuntut layarnya memuat *"Tentang aplikasi"*, satu lagi menuntut Pengaturan
  memuat *"Tentang & sumber"* — masing-masing hijau, dan bersama-sama menjaga agar satu layar tetap
  punya dua nama. Keduanya kini memakai nama bersama `NamaLayar.TENTANG` (*"Tentang aplikasi"*) dan
  `NamaLayar.PEMBARUAN_KONTEN`, judul bagian dan label tombolnya ikut lahir dari sana
  (`"Buka ${NamaLayar.TENTANG}"`), dan uji instrumented yang menggulir ke judul bagian itu pun
  menunjuk lewat nama bersama. Dijaga `NamaLayarSatuTempatTest`, yang kini memeriksa pula kedua
  layar Pengaturan itu beserta judul bagian dan tombolnya.
- **Id entri daftar sholat khusus tinggal di satu tempat**: layar Panduan Sholat mengenali entri yang
  punya modul sendiri (Ghaib, Jenazah) dengan mengeja id-nya sendiri — `s.id == "ghaib"`,
  `s.id == "jenazah"` — padahal id itu datang dari `sholat_daftar.json`. Tak ada apa pun yang
  menyandingkan kedua ejaan itu, sehingga mengganti id di bundel (entri itu dinamai ulang, misalnya)
  **diam-diam** menghapus tombol "Buka" bagi entri tersebut: ia jatuh ke tombol "Tata cara" yang
  membuka halaman rincian tanpa isi, dan tak satu pun uji gagal. Menambah modul khusus baru pun tak
  menolong — yang baru tidak pernah dikenali betapa pun lengkapnya barisnya di
  `BerkasKonten.SHOLAT_KHUSUS`. Daftar bersama itu kini menyimpan pula **id entri daftarnya** dan
  **nama layarnya** (`BerkasKonten.Modul`), layar membacanya lewat
  `BerkasKonten.SHOLAT_KHUSUS.firstOrNull { it.idDaftar == s.id }`, dan nama tombolnya ikut lahir dari
  sana (`"Buka ${modul.nama}"`) alih-alih ditulis ulang. Dijaga dari dua sisi: id itu harus benar-benar
  ada di `sholat_daftar.json` (dan tak kembar), sedangkan namanya harus sama dengan yang diberikan
  peta bersama `RuteKonten` untuk rute itu — modul yang menulis *"Ghaib"* untuk rute Jenazah ditolak.
- **Angka konten di dua dokumen ikut dijaga**: jumlah ayat Yasin dinyatakan **dua** dokumen — tabel
  provenans di `docs/CONTENT_SOURCES.md` dan §1 `SOURCES.md` (*"Edisi terjemah: Kemenag 2019. Yasin =
  83 ayat"*) — tetapi hanya yang pertama dibandingkan dengan bundel yang benar-benar dikirim. Justru
  `SOURCES.md`, dokumen yang dibaca penulis konten sebagai rujukan resmi, bebas menua sendiri:
  bundelnya kehilangan beberapa ayat, tabelnya ikut diperbarui, sedangkan angka di sana tetap lama —
  dan pembaca menyimpulkan ada isi yang tak pernah ikut terkirim, atau menganggap berkas yang lebih
  lengkap sebagai yang benar. `HitungKontenDiDokumenTest` kini membaca klaim itu dari `SOURCES.md`
  pula dan membandingkannya dengan `ayat_count` bundel; menghapus klaimnya dari dokumen pun membuat
  penjaganya gagal keras, bukan lolos dalam diam.
- **Uji instrumented berhenti menyalin nama layar — dan `e2e` kembali berarti**: ketika nama layar
  disatukan ke `NamaLayar`, menu Ibadah berhenti menulis *"Yasin"* dan mulai memakai
  `NamaLayar.YASIN` (*"Surat Yasin"*). `NavSmokeTest` tidak ikut berubah: ia masih menekan
  `onNodeWithText("Yasin")`, pencocokan **tepat**, sehingga tak ada simpul yang cocok. Uji itu gagal
  bukan karena aplikasinya salah, dan sejak commit itu **setiap penggabungan ke `main` meninggalkan
  job `e2e` merah** — sinyalnya berhenti berarti, kegagalan sungguhan tak lagi dapat dibedakan dari
  yang sudah biasa. Kini `NavSmokeTest` menavigasi lewat `NamaLayar.*` (tab, judul menu, dan tombol
  Yasin), dan penjaga `NamaLayarDiUjiInstrumentedTest` memaku dua hal: argumen pencocok simpul
  (`onNodeWithText`, `hasText`, …) di seluruh `src/androidTest` tidak boleh memuat nama layar
  **maupun menjadi penggalan salah satunya** (`"Yasin"` adalah penggalan *"Surat Yasin"* — bentuk
  kegagalan yang sesungguhnya), dan `NavSmokeTest` benar-benar memakai nama bersama. Yang diperiksa
  hanya argumen pencocok, bukan setiap literal: pesan kegagalan seperti *"Surat Yasin tidak menyebut
  sumber teks Arab"* memang menyebut nama layar — dan itu wajar, sebab ia menjelaskan.
- **Label preferensi bacaan tinggal di satu tempat**: enam kendali — tiga sakelar (Arab, Latin,
  Terjemahan) dan tiga pengukur (Ukuran Arab, Ukuran Latin, Jarak antarbaris) — ada di **dua**
  berkas, sheet *Pengaturan bacaan* dan layar Pengaturan, dan masing-masing mengeja namanya sendiri.
  Keduanya menulis kunci `Store` yang **sama** (`SHOW_ARAB`/`SHOW_LATIN`/`SHOW_TRANS`,
  `ARAB_SIZE`/`LATIN_SIZE`/`LINE_HEIGHT`), jadi keduanya memang mengatur satu preferensi yang sama:
  mengganti nama satu sakelar di sheet akan meninggalkan layar Pengaturan menyebut preferensi yang
  sama dengan kata lama, dan pengguna mengira ada dua pengaturan — persis yang dicegah aturan mazhab
  Asar di layar itu. Enam nama itu kini tinggal di `NamaAntarmuka`, dan layar Pengaturan menurunkan
  teks pengukurnya (`"${NamaAntarmuka.UKURAN_ARAB}: 24"`). Penjaga
  `NamaAntarmukaSatuTempatTest.labelTidakDisalinKeLuarNamaAntarmuka` kini membaca **isi literal
  string**, bukan potongan berkutip utuh — sehingga label yang disisipkan ke dalam kalimat panjang
  (`"Ukuran Arab: ${…}"`) ikut tertangkap — dengan pembeda `memakaiLabel` yang membedakan label
  kendali dari kata biasa di dalam prosa (kalimat seperti *"Teks Arab wajib berharakat"* bukan
  salinan kendali). Satu pengecualian beralasan: `AtribusiTeks.kt` memakai kata *"Terjemahan"* untuk
  label **asal-usul** teks, bukan sakelar tampilan — pengecualian itu dipaku uji tersendiri supaya
  tidak berubah menjadi celah.
- **Daftar domain resmi benar-benar tinggal di satu tempat**: kalimat *"dari mana konten aplikasi
  berasal"* ditulis di **empat** layar sekaligus, dan tiap layar mengejanya sendiri. Pengaturan
  menulis satu kalimat panjang yang mendaftar sumbernya sendiri — tiga host, dari lima domain yang
  benar-benar diterima gerbang konten — sehingga domain yang ditambahkan kelak tak akan pernah
  muncul di sana; layar Jadwal dan Kiblat masing-masing mengeja `sihat.kemenag.dev` untuk menunjuk
  tempat memverifikasi hasil hitungan; dan kartu info harian mengeja `sunnah.com` sebagai asal
  hadisnya. Penjaganya sebenarnya sudah ada, tetapi menuntut hostnya menjadi potongan berkutip
  **utuh** (`"sunnah.com"`) — dan host di dalam kalimat panjang bukan potongan berkutip utuh,
  sehingga keempat layar itu lolos tanpa ketahuan. Kini pemindaiannya membaca **isi literal string**
  (termasuk bentuk mentah `"""…"""`), dan keempatnya memakai sumber bersama: Pengaturan menurunkan
  daftarnya dari `ContentValidator.allowedHosts` — pola yang sudah dipakai layar Pembaruan konten —
  sedangkan situs SIHAT dan situs teks hadis yang **disebut nama** layar datang dari
  `ContentValidator.SITUS_SIHAT`/`SITUS_HADIS`, nama pada daftar yang sama, bukan salinan yang
  dapat basi. Dijaga `SumberResmiSatuTempatTest`: tak satu berkas Kotlin di luar
  `ContentValidator.kt` dan `DokumenLegal.kt` boleh memuat host resmi di dalam string mana pun, dan
  kedua situs yang disebut layar wajib benar-benar diterima `isOfficialSource`.
- **Data warisan yang hilang tanpa galat kini dipindahkan di kedua pintunya**: v1.0.0 menyimpan
  **tiga** hal di DataStore — favorit, riwayat bacaan, dan posisi Tahlil — sedangkan ketiganya kini
  ada di Room. Pemindahnya dahulu hanya memindahkan favorit; `Store.HIST` dan `Store.TAHLIL_PROG`
  tetap tertulis di perangkat pengguna tetapi **tak satu pun** layar membacanya lagi, sehingga
  pengguna yang memperbarui aplikasi melihat riwayatnya kosong dan Tahlil-nya kembali ke bagian
  pertama — tanpa galat, tanpa cara mengetahui bahwa datanya sebenarnya belum hilang. Ketiga kunci
  warisan kini dipindahkan dengan aturan yang sama (hanya bila sisi Room masih kosong, lalu kuncinya
  dikosongkan agar tidak dipindahkan dua kali): riwayat dipindahkan **beserta waktu kejadiannya**
  (`pushHistory` menerima `openedAt`) supaya urutan "terakhir dibaca" tidak berubah hanya karena
  pemindahannya berjalan belakangan, dan posisi Tahlil dipindahkan satu lawan satu ke dua baris
  progresnya. Bentuk nilai warisan Tahlil (`posisi:hitungan`) dibaca lewat
  `KunciProgres.bacaWarisan` — satu tempat, sehingga pemisahnya tidak pernah berbeda dari yang dulu
  menuliskannya, dan nilai yang rusak dilewati alih-alih ditebak separuh.
- **Pemulihan backup lama ikut memindahkan data warisan**: data warisan masuk ke DataStore lewat
  **dua** pintu, dan yang kedua juga terbuka. Berkas cadangan v1.0.0 hanya memuat preferensi
  (`prefs`), sehingga `Store.importJson` menuliskan `fav`, `hist`, dan `tahlil_prog` kembali ke
  DataStore — dan karena tak ada yang membacanya lagi, menekan "Pulihkan" **tampak berhasil**
  sementara favorit, riwayat, dan posisi Tahlil pengguna tak pernah muncul. Pemindahnya dahulu
  berupa fungsi privat di `YasinApp`, jadi pintu kedua tak dapat memakainya. Kini pemindahnya
  tinggal di `MigrasiWarisan` (dipanggil saat `onCreate` **dan** sesudah pemulihan), aman dipanggil
  berulang kali, dan dijaga `PemakaianKunciTest`: setiap kunci yang dideklarasikan wajib dipakai
  kode aplikasi, setiap kunci bertanda warisan wajib dibaca **dan** dikosongkan pemindahnya, rantai
  pemanggilannya diperiksa dari `onCreate` sampai ketiga fungsi pemindahnya, dan pemulihan backup
  wajib menjalankan pemindah **setelah** preferensinya dipulihkan.
- **Aturan menerangkan metode hisab tinggal di satu tempat**: kalimat *"bagaimana metode yang
  sedang berlaku diterangkan ke pengguna"* ditulis ulang di tiap layar yang menyebutnya, dan
  ketiganya berbeda. Kepala layar Jadwal menuliskan `Magrib+…mnt`, penjelasan di menu Adzan
  menuliskan `Isya Magrib+… menit` — dua ejaan untuk satu aturan — sedangkan layar Pengaturan
  menuliskan **Kemenag/SIHAT** secara tetap. Yang terakhir paling berbahaya: pemilih metode ada di
  menu Jadwal (MWL, Umm al-Qura, Egyptian, Makkah), tetapi siapa pun yang memilihnya membaca di
  Pengaturan bahwa jadwalnya dihitung dengan Kemenag/SIHAT — keterangan yang berbohong tanpa satu
  pun pesan galat, dan layar Pengaturan bahkan tak pernah membaca `Store.METHOD`. Kini aturannya
  tinggal di `JadwalHariIni.metodeRingkas`, dibaca dari `PrayerCalc.Method` yang sedang aktif;
  ketiga layar memanggilnya, dan Pengaturan membaca metode aktif untuk menyebut namanya. Dijaga
  `AturanMetodeSatuTempatTest` — ringkasannya diperiksa **terhadap definisi setiap metode** (jadi
  mengubah sudut sebuah metode tidak membuat uji hijau secara palsu), setiap layar yang menerangkan
  metode wajib memanggil aturan bersama, tak satu pun boleh mengetik sudut/selang/nama metode
  sendiri, dan format `Magrib+…mnt` dibuktikan hanya hidup di satu berkas kode produksi.
  `PenjelasanMetodeAdzanTest` disesuaikan: yang diperiksa kini bahwa layar Adzan **memakai** aturan
  bersama, bukan menyalinnya lagi.
- **Dokumen berhenti menjanjikan pemeriksaan yang tidak ada**: lima klaim di dokumentasi tidak
  sesuai kenyataan, dan tak satu pun diperiksa — kelas yang sama dengan bug-bug sebelumnya, hanya
  kini dokumennya yang berbohong kepada pembaca, bukan kode kepada dirinya sendiri. (1) `SOURCES.md`
  menyatakan *"Setiap entri JSON wajib: `arab, latin, terjemahan, sumber_url, edisi, catatan_mazhab`"*,
  padahal `tools/validate_content.py` tak pernah menyebut `edisi`/`catatan_mazhab` dan kontennya
  sendiri hanya memuatnya di sebagian berkas (keduanya metadata tingkat berkas/bagian, bukan
  per-entri). (2) `docs/PERMISSIONS.md` menunjuk `OfflineManifestTest.izin_alarm_tepat_waktu_sejalan_dengan_pemakaian`
  sebagai penjaga aturan *"izin hanya dideklarasikan bila benar-benar dipakai"*, padahal uji itu
  hanya mengawasi pasangan `AlarmManager` ↔ izin alarm — akibatnya `WAKE_LOCK` duduk di manifes
  tanpa satu pun pemakaian di kode aplikasi. (3) `docs/CONTENT_SOURCES.md` mengutip *"Muslim 963"*
  untuk `sholat_mayit.json`, padahal berkasnya tak memuat angka 963 sama sekali. (4) Bagian
  *"Sisi CI"* `docs/CONTENT_UPDATE.md` menulis "dua fixture" dan tak menyebut fixture konten
  maupun `bundel/bacaan-cacat.json`. (5) Nama `e2e.yml` menjanjikan pemicu *"label e2e"* yang tidak
  pernah ada. Perbaikannya: aturan di `SOURCES.md` kini menyatakan apa yang benar-benar diperiksa
  **dan** apa yang tidak; `WAKE_LOCK` dihapus dari manifes (WorkManager mencantumkannya sendiri di
  manifesnya, jadi manifes gabungan tetap memuatnya tanpa aplikasi ikut meminta); tabel provenans
  menyebut sumber yang benar-benar dikutip berkasnya; dokumen CI menyebut kelima fixture; dan nama
  `e2e.yml` hanya menyebut pemicu yang ada. Masing-masing dikunci penjaga baru —
  `KlaimAturanKontenTest`, `PemakaianIzinTest`, `FixtureCiTerdokumentasiTest`, `NamaAlurKerjaJujurTest`,
  dan `TabelProvenansKontenTest.nomorHadisYangDikutipBenarBenarAdaDiBerkasnya`.
- **Gerbang konten akhirnya punya bukti bahwa ia menolak**: gerbang bundel sudah lama memiliki
  fixture — `tools/fixtures/bundel/sah.json` yang wajib lolos dan dua `*cacat.json` yang wajib
  ditolak, ketiganya dijalankan `ci.yml`. Gerbang konten (`tools/validate_content.py`) tidak punya
  satu pun, sehingga satu-satunya cara membuktikan ia menolak adalah menyunting aset yang
  sesungguhnya — dan tak ada apa pun di repo yang menjaganya tetap menggigit. Yang lebih buruk,
  skripnya dahulu **melaporkan sukses tanpa memeriksa satu entri pun**: bila penelusuran entri
  berhenti mengumpulkan apa pun, keluarannya "0 galat" dan CI tetap hijau untuk berkas yang tak
  pernah dibuka — persis kegagalan yang oleh aturan proyek ini disebut lebih buruk daripada gate
  yang tidak ada, sebab ia menjanjikan pemeriksaan yang tidak pernah berjalan. Kini gate menerima
  berkas/direktori sebagai argumen (tanpa argumen tetap seluruh `assets/content`), mengenal mode
  `--harus-gagal`, dan **gagal keras** bila tidak satu pun entri ditemukan — alasan yang sama
  dengan `load_allowed_hosts()` yang sudah lebih dulu menolak daftar host kosong. Fixture baru
  `tools/fixtures/konten/sah.json` dan `cacat.json` dijalankan `ci.yml`; keduanya berbeda **hanya**
  pada `sumber_url` yang hilang, sehingga kegagalannya menunjuk tepat satu aturan.
  `FixtureGateKontenTest` menjaga tiga hal yang tidak dapat dilihat CI: fixture-nya ada, ia masih
  hanya berbeda pada satu aturan, dan CI benar-benar menjalankannya dengan `--harus-gagal`. Sepuluh
  mutasi terverifikasi tertangkap, termasuk menghilangkan fixture dan mencabut penjaga nol-entri.
- **Rujukan pada konten bawaan akhirnya diperiksa**: sebuah entri konten dapat menunjuk berkas lain
  (`rujukan_file`, `rujukanFile`, larik `rujukan`) alih-alih menyalin lafaznya, supaya satu teks
  terverifikasi dipakai beberapa modul tanpa salinan yang bisa menyimpang. Aturan bahwa rujukan itu
  harus menunjuk berkas dan entri yang benar-benar ada sudah lama ditulis di
  `BundelKonten.periksaRujukan` — dan dijalankan aplikasi **saat memasang** bundel yang dipilih
  pengguna. Yang tidak pernah dijalankan atas berkas yang benar-benar dikirim adalah aturan yang
  sama: `DzikirRujukanTest` memeriksa rujukan `dzikir.json` saja, dan hanya yang berbentuk
  `berkas#id`, sehingga dari **dua puluh** nilai rujukan di `assets/content` hanya **lima** yang
  diperiksa; `tools/validate_content.py` tidak mengenal kunci rujukan sama sekali. Rujukan yang
  mati pun tidak memunculkan galat: `resolveArabLatinTerjemah` membaca sasarannya lewat
  `docAtauNull`, mengembalikan teks kosong, lalu pemanggilnya jatuh ke teks milik entrinya sendiri
  yang juga kosong — entri tampil **tanpa lafaz, tanpa satu pun pesan**. Pada `tahlil_full.json`
  seluruh lafaz Qur'annya datang lewat rujukan seperti itu, dan kelompok "Doa memohon ampunan" pada
  `dzikir.json` dibangun seluruhnya dari larik `rujukan` yang tak pernah diperiksa. Kini
  `RujukanKontenBawaanTest` **memanggil pemeriksa yang sama** dengan seluruh berkas bawaan sebagai
  dokumen sekaligus daftar sasaran yang sah — menambah rujukan baru berarti ikut diperiksa tanpa
  menyunting uji. Uji itu juga memaku jumlah kunci rujukan per berkas, sebab menghapus satu rujukan
  tidak menggagalkan pemeriksaan sasaran (yang hilang justru lafaznya), dan mencari kuncinya
  sebagai **kunci JSON**, bukan sebagai kata — `catatan_tradisi` pada `tahlil_full.json` memuat
  kata "rujukan" di dalam prosa, sehingga pencarian kata biasa hijau meski berkasnya tidak merujuk
  apa pun. Sebelas mutasi terverifikasi tertangkap, termasuk merusak rujukan pada berkas aset yang
  sesungguhnya dan nilai yang dahulu tak diperiksa siapa pun. Dua klaim pada KDoc `periksaRujukan`
  yang sudah tidak benar ikut diperbaiki: rujukan mati tidak lagi menaikkan `FileNotFoundException`,
  dan "gerbang konten di CI" yang disebut memeriksa berkas bawaan ternyata tidak memeriksanya.
- **Nama berkas konten akhirnya satu ejaan**: `ContentRepo` mengeja sebelas nama berkas di dalam
  badannya sendiri (`"dzikir.json"`, `"adzan.json"`, `"kota.json"`, …), sementara `BerkasKonten`
  hanya menyimpan dua nama modul sholat khusus. Nama berkas bukan sekadar label: ia kunci ke tiga
  tempat sekaligus — `assets/content`, `filesDir/content_update/` tempat bundel pembaruan dipasang,
  dan peta `berkas` di dalam bundel itu sendiri. Yang lebih buruk, **tidak ada apa pun** yang
  membandingkan nama yang dibaca Kotlin dengan berkas yang benar-benar dikirim: mengganti nama
  sebuah berkas di aset, atau salah mengejanya di Kotlin, tetap lolos seluruh uji dan lolos CI, lalu
  gagal di perangkat saat layar itu dibuka (`FileNotFoundException` dari `assets.open`) — atau, pada
  lapisan pembaruan, sebagai penimpaan yang **diam-diam diabaikan** sehingga pembaruan yang
  dilaporkan "terpasang" tidak mengubah apa pun. Kini ketiga belas nama tinggal di `BerkasKonten`
  beserta daftar `SEMUA`, dan `BerkasKontenSatuTempatTest` menutup tiga arah: nama hanya dieja di
  sana (contoh bundel di layar Pembaruan Konten pun kini memakai konstanta, bukan teks yang bisa
  tertinggal), setiap nama benar-benar ada di `assets/content`, dan setiap berkas aset yang tidak
  dibaca langsung Kotlin (`surah_001/112/113/114.json`, `ayat_kursi.json`) benar-benar dirujuk
  berkas konten lain — sehingga tidak ada konten yang ikut terkirim tanpa pernah tampil. Enam belas
  mutasi terverifikasi tertangkap, termasuk mengganti nama berkas aset yang sesungguhnya.
- **Bentuk wadah `bacaan` akhirnya diperiksa gate**: dari tiga bentuk dokumen konten yang dibaca
  aplikasi, hanya dua yang pernah diperiksa gate CI — `bagian` (Tahlil) dan `kelompok[].items[]`
  (Dzikir). Bentuk ketiga, `bacaan` (`sholat_bacaan.json`, `sholat_mayit.json`), tak diperiksa sama
  sekali. Bedanya menentukan: `ContentRepo` membacanya dengan akses yang **melempar**
  (`getJSONArray`, `getString`), bukan `opt…`, jadi entri yang kehilangan kunci `id` atau `judul`
  tidak menampilkan teks kosong — ia **menghentikan seluruh layar** yang membacanya (Panduan
  Sholat, Jenazah, Ghaib) tanpa satu pun petunjuk sebabnya, dan itu berlaku baik untuk berkas
  bawaan maupun berkas dari bundel pembaruan yang dipasang pengguna. Kedua bentuk tingkat dokumen
  kini diperiksa bersama lewat satu aturan yang sama, memakai nama wadah yang dibaca dari
  `BentukKonten` — sama seperti yang dibaca aplikasi. Pemeriksaannya pun tidak lagi melempar bila
  isi wadah ternyata bukan objek (dahulu gate berhenti dengan jejak galat, bukan laporan). Sebuah
  fixture baru (`tools/fixtures/bundel/bacaan-cacat.json`) yang **satu-satunya** cacatnya adalah
  entri `bacaan` tanpa `id` kini dijalankan CI dengan `--harus-gagal`, sehingga pemeriksaan itu
  terbukti benar-benar berjalan; `BentukKontenTest` menuntut setiap fixture `*cacat.json` benar-benar
  dijalankan CI. Sepuluh mutasi terverifikasi tertangkap, dan satu mutasi perilaku terverifikasi
  membuat fixture itu lolos — bukti bahwa gate inilah yang menahannya.
- **Gate CI membaca bentuk dokumen konten dari satu tempat**: `tools/validate_content.py` mengeja
  sendiri nama wadah entri konten (`bagian`, `kelompok`, `items`) dan kunci `id`-nya, padahal
  aplikasi membacanya dari `BentukKonten`. Salinan itu paling sunyi penyimpangannya dari semua
  salinan sejenis: pemeriksaan bentuk tahlil dimulai dengan pertanyaan *"apakah berkas ini punya
  wadah itu"*, jadi salinan yang tertinggal **tidak pernah cocok lagi** — seluruh pemeriksaan
  berkas tahlil dilewati, dan berkas yang kehilangan `id`/`judul` di tiap bagiannya lolos CI tanpa
  satu pun galat, sementara aplikasi tetap membaca wadah itu lewat `BentukKonten`. Nama wadah dan
  kunci `id` kini dibaca gate dari `BentukKonten.kt` lewat pola pengambil yang ditulis persis sama
  di penjaganya, dan `tools/validate_bundle.py` — yang memanggil pemeriksa yang sama — ikut memakai
  kunci bersama itu alih-alih mengeja labelnya sendiri. `BentukKontenTest` menuntut gate benar-benar
  menunjuk berkas itu, memakai pola pengambil yang sama, dan tidak menyimpan satu pun nama wadah;
  uji itu juga membuktikan polanya menghasilkan nilai aplikasi, bukan sekadar cocok. Lima belas
  mutasi terverifikasi tertangkap.
- **Bentuk dokumen konten tak lagi dieja di dua pembaca**: berkas konten menyimpan entri yang dapat
  dirujuk dalam tiga bentuk — `bagian`, `bacaan`, dan `kelompok[].items[]` — beserta kunci `id`
  entrinya. Bentuk itu dibaca **dua peran yang tak pernah saling melihat**: `ContentRepo`, yang
  membangun daftar bacaan dari dokumennya, dan `entriRujukan`, yang mengikuti rujukan
  `berkas.json#id` dari dokumen lain (dan karena itu juga menentukan apakah pemeriksa bundel
  menyebut sebuah rujukan sah atau mati). Dahulu keduanya menuliskan nama wadah dan kunci `id`
  sendiri-sendiri — sebelas tempat di satu sisi, empat di sisi lain — dan salinan yang menyimpang
  menyesatkan ke arah mana pun ia bergeser: pembaca menampilkan entri **kosong tanpa galat**, atau
  pemeriksa menolak bundel yang sah dengan alasan "menunjuk entri yang tidak ada" sehingga
  kesalahannya dituduhkan ke konten, bukan ke kode. Nama wadah, kunci `id`, dan daftar bentuk
  tingkat dokumen kini tinggal di `BentukKonten`; `ContentRepo` dan `entriRujukan` sama-sama
  memakainya, sehingga menambah atau mengganti satu bentuk memindahkan keduanya sekaligus.
  `BentukKontenTest` menutup dua hal sekaligus: perilakunya (penelusuran benar-benar menemukan entri
  di ketiga bentuk, dan bentuk pertama pada daftar yang menang bila dokumen keliru memuat dua) dan
  sumbernya (kedua berkas tak boleh mengeja satu pun nama wadah itu lagi). Dua puluh mutasi
  terverifikasi tertangkap.
- **Kunci data masukan pekerja pengingat tak lagi dieja di dua sisi**: `PrayerScheduler` menulis
  `workDataOf("nama" to …, "jenis" to …)` dan `ReminderWorker` membacanya dengan ejaan sendiri —
  dua kelas yang tak pernah saling melihat, sehingga tak ada yang membandingkan keduanya. Salinan
  yang menyimpang gagal dengan sunyi: pekerja membaca `nama` sebagai null lalu **langsung**
  mengembalikan `Result.success()`, jadi pengingat adzan tak pernah berbunyi dan tak ada satu pun
  galat yang menyebut sebabnya. Kuncinya kini tinggal di `PengingatAdzan` (`KUNCI_NAMA`,
  `KUNCI_JENIS`, `KUNCI_ADZAN_LAMA`) dan dipakai kedua sisi. `PengingatAdzanTest` memindai seluruh
  kode utama agar tak ada `workDataOf`/`inputData` yang mengeja kunci langsung, memaku nilainya
  (data masukan yang terantre bertahan melintasi pemutakhiran aplikasi), dan menuntut penulis serta
  pembacanya benar-benar memakai kosakata bersama itu. Sebelas mutasi terverifikasi tertangkap.
- **Kunci `_meta.json` tak lagi dieja dua kali**: berkas metadata pembaruan ditulis oleh `metaJson`
  dan dibaca oleh `status()` di `ContentUpdateManager` — tujuh kunci, dua kali, tanpa apa pun yang
  membandingkannya. Mengganti nama kunci di satu sisi saja membuat layar **"Pembaruan konten"**
  melaporkan *tidak ada pembaruan terpasang* padahal berkasnya sudah ada dan sedang dipakai; karena
  tombol **"Kembalikan ke konten bawaan"** hanya muncul saat ada yang terpasang, pengguna kehilangan
  satu-satunya jalan kembali ke konten bawaan — tanpa satu pun pesan galat. Penulis dan pembacanya
  kini tinggal bersama di `MetaPembaruan` sebagai fungsi murni (dapat diuji tanpa `Context`) dan
  memakai konstanta kunci yang sama; `MetaPembaruanTest` menuntut `ContentUpdateManager` tidak lagi
  mengeja satu pun kunci itu sendiri. Waktu pemasangan pun kini diformat pada zona yang diberikan,
  sehingga ujinya sama di mesin mana pun. Tiga belas mutasi terverifikasi tertangkap.
- **Aturan prosa penunjuk di dokumen kedua ikut dijaga**: `docs/CONTENT_SOURCES.md` mengulang aturan
  yang sama dalam bentuk **prosa** — *"menolak **placeholder** di field `arab`/`latin`"* beserta
  contohnya (`"— lihat rujukan file —"`) — dan tak ada apa pun yang membandingkannya dengan kode.
  Prosa lebih mudah menyimpang daripada tabel karena tak ada bentuk yang memaksa: `FIELD_TEKS` yang
  bertambah field membuat dokumen mengecilkan cakupan pemeriksaan, dan contoh prosa yang sudah tidak
  ditolak lagi mengajarkan gaya penulisan yang justru lolos ke layar. Dua uji baru di
  `AturanBundelSatuTempatTest` menuntut dokumen menyebut field yang persis dibaca gate (dengan
  lookahead, supaya *"`arab`/`latin`"* tidak lolos hanya karena menjadi awalan dari daftar yang lebih
  panjang) dan contoh prosanya benar-benar ada di daftar yang ditolak. Tujuh mutasi terverifikasi
  tertangkap.
- **Field tempat prosa penunjuk dicari tidak lagi dapat menyimpang dari gate dan dokumen**: aturan
  *"prosa penunjuk di `arab`/`latin`"* hidup di tiga tempat — badan `fieldTeks` di `BundelKonten`,
  pemeriksa `check_placeholders` di `tools/validate_content.py` yang menyimpan `("arab", "latin")`
  tersendiri, dan baris aturan `docs/CONTENT_UPDATE.md` yang menyebut field yang sama kepada
  pembaca. Menambah satu field di satu sisi saja menyempitkan pemeriksaan **tanpa galat apa pun**:
  prosa penunjuk di field baru lolos ke layar sebagai bacaan, atau dokumen menjanjikan pemeriksaan
  yang tak pernah dijalankan. Daftar field kini dinyatakan sebagai data (`BundelKonten.FIELD_TEKS`)
  dan **dibaca** gate dari sana, sama seperti daftar prosanya; `AturanBundelSatuTempatTest` menuntut
  gate tidak mengeja ulang daftarnya, `fieldTeks` benar-benar memakainya, dan baris dokumen menyebut
  field yang persis sama — sekaligus memastikan tiap prosa yang dicontohkan dokumen masih benar-benar
  ditolak aplikasi (daftarnya boleh tetap disingkat dengan *"…"*, tetapi begitu berhenti menyingkat
  ia justru harus lengkap). Sembilan mutasi terverifikasi tertangkap.
- **Angka isi konten di dokumen tidak lagi dapat menua sendiri**: tabel provenans
  `docs/CONTENT_SOURCES.md` menyatakan **banyaknya** isi tiap berkas — *"Yasin 83 ayat"*,
  *"15 bagian susunan tahlil"*, *"5 kelompok"* dzikir — dan angka itulah yang dibaca orang untuk
  menilai apakah sebuah berkas masih lengkap. Tak ada apa pun yang membandingkannya dengan berkas
  yang benar-benar dikirim: gate konten menjaga konsistensi **di dalam** bundel (`ayat_count` vs
  jumlah `verses`) tetapi tidak pernah membaca dokumennya, sehingga berkas yang kehilangan beberapa
  ayat tetap ditulis *"83 ayat"* dan pembaca menyimpulkan ada isi yang tidak pernah ikut terkirim.
  `HitungKontenDiDokumenTest` membandingkan angka-angka itu dengan bundelnya, sekaligus menuntut
  tiap nama kelompok dzikir yang disebut dokumen masih menunjuk kelompok yang ada — tanpa memaksa
  dokumen melepas gaya ringkasnya (`*setelah sholat*` untuk *"Dzikir setelah sholat"*). Tujuh
  mutasi terverifikasi tertangkap.
- **Patokan toolchain di README tidak lagi dapat menua diam-diam**: `README.md` menyatakan satu
  baris yang justru dibaca lebih dulu oleh siapa pun yang hendak membangun — *"Toolchain dipatok
  (AGP 8.5.x, Kotlin 2.0.20, `compileSdk`/`targetSdk` 34)"* — dan "Butuh Android Studio + JDK 17".
  Nilainya hidup di berkas lain (`gradle/libs.versions.toml` untuk AGP dan Kotlin,
  `app/build.gradle.kts` untuk tingkat SDK dan Java), dan tak ada apa pun yang membandingkannya.
  Setelah toolchain naik, baris itu tetap menuliskan versi lama, dan kontributor yang membangun
  dengan toolchain berbeda mengejar galat yang tak ada di proyek. Kini `ReadmeToolchainTest`
  membandingkan keduanya: major.minor AGP (patch-nya bebas naik, sebab `README` memang menulis
  `8.5.x`), versi Kotlin, angka `compileSdk`/`targetSdk`, dan tingkat JDK. Satu angka SDK di README
  juga dituntut hanya sah selama `compileSdk` dan `targetSdk` benar-benar sama. Sepuluh mutasi
  terverifikasi tertangkap.
- **Kosakata mazhab Asar tidak lagi dapat lepas dari kosakata mazhab panduan**: kunci mazhab
  (`syafii`, `hanafi`) dan namanya (`Syafi'i`, `Hanafi`) hidup di dua tempat — `Mazhab` untuk
  pilihan mazhab panduan sholat, dan `JadwalHariIni.ASAR_*` untuk perhitungan jam Asar. Layar
  Pengaturan menampilkan **kedua daftar itu berdampingan** di halaman yang sama, tetapi tak ada
  apa pun yang membandingkannya: mengubah nama di `Mazhab` tak menyentuh `JadwalHariIni`, sehingga
  satu layar dapat menawarkan *"Syafi'i"* untuk mazhab panduan dan nama lain untuk mazhab Asar —
  dua sebutan untuk satu mazhab, bersebelahan, tanpa satu pun galat. Kini `JadwalHariIni` **tidak
  mengeja ulang** kosakata itu: `ASAR_SYAFII`/`ASAR_HANAFI` meminjam konstanta `Mazhab`, dan
  `ASAR_SEMUA` diturunkan dari `Mazhab.SEMUA` — hanya nama yang sudah dikenal panduan yang dapat
  muncul, dan ejaannya dijamin sama. `SebutanSatuTempatTest` menjaga dua arah: daftar Asar harus
  tetap bagian dari daftar panduan (dan tepat dua, sebab hanya dua mazhab yang jam Asarnya
  berbeda), dan `JadwalHariIni` tidak boleh mengeja kembali kunci maupun namanya. Enam mutasi
  terverifikasi tertangkap.
- **Kunci entri bacaan adzan tidak lagi dapat menyimpang dari bundel, pembaca, dan gate**: tiga
  bacaan setelah adzan — menjawab adzan, sholawat, dan doa setelah adzan — punya kunci yang ditulis
  di **empat** tempat: `adzan.json`, pembaca `ContentRepo.adzanDoa("…")`, daftar kunci di gate
  `tools/validate_content.py` (yang menuntut tiap entri menyatakan **kapan dibaca**), dan daftar di
  dalam ujinya sendiri. Tiap salinan bergerak sendiri-sendiri, dan yang paling sunyi adalah gate:
  `data.get(k)` mengembalikan `None` untuk kunci yang salah tulis, syarat `isinstance(entri, dict)`
  tak terpenuhi, dan pemeriksaan "kapan dibaca" **dilewati tanpa satu pun galat** — padahal justru
  itu yang diminta `plan.txt`. Kini `WaktuBacaanAdzanTest` mengikat keempatnya: daftar uji harus
  sama dengan entri ber-`waktu` di bundel, daftar gate harus sama dengan bundel, dan pembaca di
  `ContentRepo` harus sama dengan daftar gate — sehingga kunci yang berubah di sisi mana pun
  sendirian akan tertangkap. Sekaligus dua pemeriksaan render diperketat: pencarian biasa
  `"KartuDoa("` juga cocok dengan **definisi** fungsinya, dan `"entri.catatan"` juga cocok dengan
  `entri.catatanKaki`, sehingga entri yang diam-diam dirender lewat helper lain — atau catatannya
  tak pernah tampil — tetap hijau. Sembilan mutasi terverifikasi tertangkap.
- **Kosakata kategori bacaan di dokumen tidak lagi dapat menyimpang dari yang tampil di layar**:
  `docs/CONTENT_SOURCES.md` memuat tabel kategori (kunci, arti, **label di layar**) sebagai
  penjelasan kategori yang wajib dinyatakan tiap berkas ber-Arab, dan `docs/CONTENT_UPDATE.md`
  menyebut daftar kategori yang sah di baris aturan bundel. Keduanya salinan dari
  `SumberKategori.SEMUA` dan `SumberKategori.label()` — dan tak ada apa pun yang membandingkannya,
  sebab penjaga yang ada hanya menelusuri berkas Kotlin dan gate Python. Akibatnya sunyi dua arah:
  kategori yang ditambahkan di kode membuat dokumen berbohong tentang apa yang dapat dilihat
  pengguna, dan label yang diubah di kode tanpa ikut disunting membuat dokumen menuliskan tulisan
  layar yang tak pernah muncul. Kini `SumberKategoriTest` menuntut kesamaan kunci **dan** label
  antara tabel dokumen dengan `SumberKategori`, serta kesamaan daftar di `docs/CONTENT_UPDATE.md`.
  Tujuh mutasi terverifikasi tertangkap, termasuk kategori yang ditambahkan serentak di kode dan
  gate konten (sehingga hanya dokumen yang tertinggal).
- **Tabel provenans konten tidak lagi dapat ketinggalan berkas**: `docs/CONTENT_SOURCES.md`
  menyatakan dirinya mencatat *"asal setiap file konten"*, tetapi tabelnya ditulis tangan dan
  terpisah dari berkas yang benar-benar dikirim di dalam APK — tak ada apa pun yang membandingkan
  keduanya. Berkas konten yang tak tercatat justru yang paling sulit ditemukan: ia ikut terkirim,
  terbaca aplikasi, dan asalnya tak pernah diperiksa siapa pun karena tabel itu satu-satunya daftar
  yang ada. Arah sebaliknya juga sunyi: baris yang menunjuk berkas yang sudah dihapus membuat tabel
  tampak lebih lengkap daripada kenyataan. Kini `TabelProvenansKontenTest` menuntut kesamaan
  **dua arah** antara tabel dan `assets/content/*.json` — termasuk menguraikan bentuk singkat
  (`surah_001/112/113/114.json`) menjadi keempat namanya, sehingga penulisan tabel yang ringkas pun
  tetap terbaca lengkap. Lima mutasi terverifikasi tertangkap.
- **Dokumen yang mengulang aturan kode tidak lagi dapat menyimpang diam-diam**: dua dokumen
  manusia menyalin ulang aturan yang sudah hidup di kode, dan tak ada apa pun yang membandingkan
  keduanya. Pertama, **daftar izin**: `docs/PERMISSIONS.md` — yang ikut dibundel ke aset dan
  ditampilkan apa adanya di bagian *"Izin aplikasi dan alasannya"* pada layar Tentang — menyalin
  daftar `<uses-permission>` dari `AndroidManifest.xml`; pemeriksaan yang ada hanya satu arah
  (setiap izin di dokumen harus ada di manifes), sehingga izin baru yang ditambahkan ke manifes
  akan membuat aplikasi meminta akses yang **tak pernah diterangkan** kepada pengguna maupun
  peninjau toko aplikasi. Kedua, **daftar host resmi**: `SOURCES.md` §6 dan
  `docs/CONTENT_SOURCES.md` sama-sama menuliskan daftar host yang diizinkan gate, padahal daftar
  yang sebenarnya dipakai aplikasi ada di `ContentValidator.allowedHosts`; penjaga yang ada hanya
  menelusuri berkas `.kt`, sehingga kedua dokumen itu luput. Akibatnya mengenai penulis konten:
  host yang ditambahkan di kode membuat dokumen berbohong, dan orang yang mengikuti dokumen
  menambahkan entri dengan domain yang justru **ditolak** aplikasi — berkasnya tampak terpasang,
  lalu pembaca diam-diam kembali ke konten bawaan. Kini arah manifes→dokumen diperiksa
  (`MarkdownRingkasTest.setiapIzinDiManifestDijelaskanDiDokumen`) dan kesepadanan kedua dokumen
  dengan `ContentValidator.allowedHosts` dijaga
  (`SumberResmiSatuTempatTest.dokumenMengulangDaftarHostYangSama`). Enam mutasi terverifikasi
  tertangkap, dua arah.
- **Kunci preferensi baru tidak lagi luput dari pemulihan backup**: `Store.exportJson` menulis
  seluruh isi DataStore apa adanya (`d.asMap()`), sehingga ia lengkap dengan sendirinya; `importJson`
  tidak — ia menyebut satu per satu kunci yang dipulihkan. Dua daftar itu — deklarasi kunci di
  `Store` dan daftar di `importJson` — ditulis terpisah, dan tak ada apa pun yang membandingkannya.
  Bedanya sunyi dan datang pada saat paling buruk: pengguna mencadangkan pengaturannya, memasang
  ulang aplikasi, lalu memulihkan backup — dan pemulihannya **tampak berhasil**, berkasnya terbaca,
  tak ada galat, layar menampilkan pesan sukses, sementara satu pengaturan diam-diam kembali ke
  bawaan tanpa cara mengetahui yang mana kecuali mencoba satu per satu. Itulah yang membuat tiap
  kunci baru menambahkan pemeriksaannya sendiri: tema halaman, tampilan kompas, susunan tahlil, jenis
  suara adzan, pengingat iqomah, pengingat doa adzan, dan jadwal-ikut-lokasi masing-masing punya satu
  di berkas uji fiturnya — sehingga aturan "kunci baru wajib ikut dipulihkan" tinggal tersebar di
  **tujuh** tempat, dan kunci berikutnya tetap akan tertinggal. Pemeriksaan itu pun tidak utuh:
  `substringAfter("fun importJson")` mengembalikan seluruh berkas bila penandanya tidak ada, jadi
  mengganti nama fungsinya membuat semuanya lolos tanpa memeriksa apa pun. Kini ada satu penjaga
  untuk seluruh kelasnya, `StoreBackupLengkapTest`: daftar kunci diambil dari deklarasi `Store`,
  daftar pemulihan dari badan `importJson`, dan keduanya harus memuat nama yang **sama persis** —
  nama yang hilang maupun nama yang tidak pernah dideklarasikan sama-sama gagal, penanda fungsinya
  wajib ada, dan tipe kunci yang belum dikenal penjaga itu ikut dilaporkan. Pemeriksaan per-fitur
  yang tergantikan dihapus; yang tersisa di sana hanya pembekuan nama kunci tersimpannya. Sembilan
  mutasi terverifikasi tertangkap.
- **Keterangan batas timer iqomah kini satu kalimat**: batas tanggung jawab aplikasi atas timer
  iqomah — bahwa waktu iqomah **berbeda tiap masjid** dan karena itu hanya perkiraan pribadi, bukan
  ketentuan syariat — ditulis di **empat** tempat dengan empat kata yang berbeda: dua kali di layar
  Iqomah dan sekali di notifikasi yang berbunyi saat perkiraan waktunya habis. Salinan di notifikasi
  justru versi yang paling pendek — *"Waktu iqomah tiap masjid berbeda — ini pengingat pribadi"* —
  tanpa menyebut bahwa ini bukan ketentuan syariat, padahal itulah batas yang sesungguhnya;
  sedangkan layar menyebut *"jadi ini perkiraan pribadi — bukan ketentuan syariat"*. Pengguna yang
  membuka layar setelah notifikasi berbunyi membaca dua keterangan berbeda untuk keadaan yang sama,
  dan perbaikan pada satu salinan tidak sampai ke salinan lain. Kini satu kalimat di
  `PengingatAdzan.IQOMAH_PERKIRAAN`, dipakai layar Iqomah (di bawah timer **dan** pada penjelasan
  saklarnya) maupun notifikasi. Dijaga `PengingatAdzanTest`: ejaan kalimatnya dipaku, potongan
  khasnya (*"masjid berbeda"*, *"pengingat pribadi"*, *"perkiraan pribadi"*, *"bukan ketentuan
  syariat"*) tidak boleh muncul di berkas lain, dan kedua tempat itu harus benar-benar membaca
  kalimat bersama — bukan menggantinya dengan versi yang dieja sedikit berbeda. Delapan mutasi
  terverifikasi tertangkap.
- **Id bacaan sholat kini satu kosakata**: id yang menghubungkan bundel konten (`sholat_bacaan.json`,
  `sholat_mayit.json`) dengan layar yang menampilkannya dahulu dieja sendiri-sendiri di **empat**
  tempat — peta posisi tubuh (14 id), daftar langkah mode ringkas (8 id), dan daftar langkah takbir
  layar Sholat Ghaib serta Sholat Jenazah (masing-masing 5 id). Keempatnya tidak pernah dibandingkan
  satu sama lain, sehingga satu salah eja pada salah satunya tidak berbunyi apa pun: `cari(id)`
  mengembalikan `null` dan kartu bacaan langkah itu **hilang** sementara judul bernomor di atasnya
  tetap tampil — pengguna melihat *"Takbir 3 → Doa untuk mayit"* tanpa isi dan menyangka doanya memang
  tidak ada; `it.id in listOf(…)` pada mode ringkas sekadar tidak cocok, sehingga satu langkah pokok
  diam-diam terlewat dari panduan yang justru dipakai pemula; dan `Postures.of(id)` mengembalikan
  `null` sehingga ilustrasi posisi tubuhnya hilang tanpa keterangan. Seluruh id kini tinggal di
  `core/sholat/BacaanSholat.kt` (`SEMUA`, `MAYIT`, `RINGKAS`, `TAKBIR_MAYIT`), dan layar sholat tidak
  lagi mengeja id apa pun — urutan takbir sholat mayit pun datang dari satu daftar bersama, sementara
  tiap layar hanya menyimpan judulnya sendiri yang khas, dipetakan lewat id bersama sehingga satu id
  tanpa judul gagal keras alih-alih menghilang diam-diam. Dijaga `BacaanSholatTest`: ejaan keempat
  daftar dipaku, id pada kosakata harus sejajar urut dengan bundel konten, urutan takbir bersama harus
  diakhiri salam pada takbir keempat, dan tidak satu pun berkas `src/main` boleh menuliskan id bacaan
  sebagai literal — kecuali dua kata yang sekaligus kunci skema konten (`niat`, `takbir`), yang
  berkas pemakainya dipaku hanya `ContentRepo.kt` dan `JenisBacaan.kt` supaya pengecualiannya tidak
  melebar. Empat belas mutasi terverifikasi tertangkap.
- **Daftar host sumber resmi kini satu daftar**: aplikasi menolak berkas konten yang `sumber_url`-nya
  di luar daftar host resmi saat runtime, sementara gate CI menilai berkas yang sama saat dibangun —
  tetapi keduanya menyimpan **salinan** daftar itu sendiri-sendiri, satu di Kotlin
  (`ContentValidator.allowedHosts`) dan satu di Python (`tools/validate_content.py`), tanpa apa pun
  yang membandingkannya. Dua salinan berarti dua kebijakan: berkas dengan domain yang hanya ada di
  daftar Python **lolos gate**, tampak *"terpasang"* bagi pengguna, lalu setiap berkasnya ditolak
  pembaca dan aplikasi diam-diam kembali memakai konten bawaan — persis kegagalan sunyi yang sudah
  pernah terjadi pada ejaan kunci `sumber_url`. Sebaliknya, host yang ditambahkan di Kotlin tanpa ikut
  di gate membuat CI gagal pada berkas yang justru sah. Gate kini **membaca** daftarnya dari
  `ContentValidator.kt` — dan gagal keras bila tidak terbaca, supaya gate yang kehilangan daftarnya
  tidak diam-diam meloloskan segalanya; `tools/validate_bundle.py` memakai modul gate itu, jadi ikut
  terbawa. Dijaga `SumberResmiSatuTempatTest`: gate harus menyebut `ContentValidator.kt`, tidak boleh
  memuat salinan daftar, harus memakai pola pengambil yang **sama persis** dengan yang dipaku di uji,
  pola itu harus benar-benar menghasilkan daftar aplikasi, dan berkas Kotlin ketiga yang mulai
  mengeja host resmi harus ketahuan. Tujuh mutasi terverifikasi tertangkap. e50ca26 (fix(konten): daftar host sumber resmi satu daftar)
- **Nama rentang waktu sholat sunnah kini satu tempat**: pengelompokan sholat sunnah menuliskan
  keenam nama rentangnya **dua kali** — sekali sebagai literal pada cabang `KelompokSholat.waktu`,
  sekali lagi pada daftar urutan tampilnya. Keduanya adalah daftar teks yang sama, jadi tak ada yang
  menjaga keduanya tetap sejalan: mengubah nama di salah satu sisi sudah cukup membuat kelompok itu
  **terlempar diam-diam ke urutan paling akhir**, karena `indexOf` yang gagal mengembalikan −1 dan
  tak ada satu pun pesan galat — kelompok "Peristiwa tertentu" yang seharusnya tampil sebelum "Waktu
  lain" mendadak muncul di bawahnya. Kini `waktu` mengembalikan konstanta (`PAGI`, `MALAM`,
  `MENGIRINGI`, `KAPAN_SAJA`, `PERISTIWA`, `LAIN`), dan `URUTAN_WAKTU` menyusun dirinya dari
  konstanta yang sama. Dijaga `KelompokSholatTest`, yang menambah empat uji: setiap nama rentang
  ditulis **sekali** saja di kode, setiap rentang yang dihasilkan punya tempat di urutan, tak ada
  nama mati di urutan, dan rentang tak terbaca selalu terakhir. Dua belas mutasi terverifikasi
  tertangkap.
- **Nama layar kini satu tempat**: nama sebuah layar ditulis ulang di tiap tempat yang menunjuknya,
  dan salinan-salinannya sudah menyimpang — layar yang sama disebut *"Yasin"* di kartu beranda tetapi
  *"Surat Yasin"* di label tujuan tombol "Buka"; *"Adzan"* di kartu beranda tetapi *"Adzan &
  Pengingat"* di menu Alat; *"Kiblat"*, *"Kompas Kiblat"*, dan *"Arah Kiblat"* untuk satu layar yang
  sama; *"Cari"* untuk layar Pencarian; *"Sholat Wajib & Sunnah"* untuk Panduan Sholat; *"Tahlil +
  counter"* dan *"Iqomah + timer"* untuk layar yang counter dan timer-nya memang ada di dalamnya.
  Pengguna menekan *"Kompas Kiblat"* lalu tiba di layar berjudul *"Arah Kiblat"* — tidak ada pesan
  galat, karena aplikasinya memang berjalan benar; yang berbeda hanya **nama**-nya. Kini tinggal di
  `NamaLayar` (`core/util`), dan setiap tempat yang menyebut sebuah layar — kartu beranda, menu
  Ibadah/Alat, pintasan di layar sholat, tab bawah, `RuteKonten.namaTujuan`, dan judul layarnya
  sendiri — membaca nama dari sana. Dijaga `NamaLayarSatuTempatTest` (11 uji), yang menuntut dua hal
  terpisah: tidak ada salinan nama di luar `NamaLayar`, **dan** tidak ada nama yang tertukar — tiap
  tempat yang menunjuk sebuah layar harus memakai nama layar itu, bukan nama layar lain. Judul menu
  Ibadah dan Alat sekaligus memakai tipografi judul yang sama dengan layar lain. Dua belas mutasi
  terverifikasi tertangkap.
- **Nama kendali antarmuka kini satu tempat**: tiga nama yang muncul di lebih dari satu tempat
  masih dieja ulang di tiap pemakaian. *"Pengaturan bacaan"* adalah judul sheet `BacaanSettingsSheet`
  **dan** label tombol yang membukanya di sepuluh layar — sebelas tulisan untuk satu nama, sehingga
  mengganti nama sheet-nya meninggalkan sepuluh tombol yang menyebut nama lama: tombol tidak lagi
  menyebut apa yang dibukanya. *"Layar penuh"* adalah label chip di enam layar bacaan, dengan satu
  varian yang memang berbeda (`LAYAR_PENUH_IMAM` — pada Sholat Ghaib layar penuh sekaligus menampilkan
  panduan mode imam). *"Coba lagi"* bahkan ditulis dua kali **di dalam berkas yang sama**: sebagai
  bawaan `labelTombol` pada `EmptyState` dan sebagai literal tombol pada `ErrorState`. Ketiganya kini
  tinggal di `NamaAntarmuka`, sehingga nama kendali dan komponen yang memakainya tidak dapat berbeda
  pendapat. Dijaga `NamaAntarmukaSatuTempatTest` (10 uji baru), yang juga menuntut varian imam tetap
  terbaca sebagai varian dari nama dasarnya dan tiap layar memakai varian yang benar — bukan sekadar
  "ada konstanta". Sepuluh mutasi terverifikasi, termasuk pemindai komentar yang diuji agar tidak
  menuduh KDoc yang justru menjelaskan perbaikannya.
- **Nama saklar pengingat kini satu tempat**: satu saklar dapat dinyalakan/dimatikan dari beberapa
  layar, dan tiap layar menuliskan namanya sendiri. Kalimat penjelasnya sudah lama disatukan di
  `PengingatAdzan`, tetapi **namanya** belum — dan salah satunya sudah berkembang menjadi dua:
  saklar utama `Store.NOTIF` bernama *"Pengingat waktu sholat"* di beranda tetapi *"Pengingat
  ibadah"* di Pengaturan. Pengguna yang mematikan pengingat di beranda lalu membuka Pengaturan
  menemukan saklar yang sama dengan nama lain, atau mengira keduanya pengaturan yang berbeda.
  Nama saklar *"Pengingat membaca doa setelah adzan"* (`Store.DOA_ADZAN_REMINDER`) juga ditulis
  lengkap di dua layar (Adzan dan Iqomah) yang sama-sama mengaturnya. Keduanya kini tinggal di
  `PengingatAdzan` (`NAMA_PENGINGAT_WAKTU`, `NAMA_PENGINGAT_DOA`), dan ujinya menuntut lebih dari
  sekadar "memakai konstanta": tiap tempat saklar harus menyebut nama miliknya **dan tidak
  menyebut nama saklar yang lain**, karena nama yang tertukar sama menyesatkannya dengan saklar
  tanpa nama. Dijaga `PengingatAdzanTest` (4 uji baru), dengan enam mutasi terverifikasi.
- **Rujukan baris progres kini satu tempat**: baris progres bacaan disimpan sebagai pasangan
  `(jenis, rujukan)`. Jenisnya sudah lama datang dari `JenisBacaan`, tetapi **rujukannya** masih
  diketik ulang di tempat pemakaian: `"idx"` dan `"count"` muncul sepuluh kali di
  `TahlilViewModel` — penulis sekaligus pembacanya — lalu diketik ulang lagi di `SettingsScreen`,
  yang mengosongkan progres Tahlil saat pengguna mengganti susunan; `"wudhu"` diketik di tiga
  tempat di `WudhuScreen` padahal jenisnya sudah dari kosakata bersama. Karena penulis dan pembaca
  baris yang sama tinggal di berkas berbeda, satu ejaan yang berubah di salah satu sisi tidak
  menghasilkan galat apa pun: pengguna menekan "mulai dari awal" dan penghitungnya tidak bergerak
  (baris baru ditulis, baris lama tetap terbaca), atau tanda "sudah dipelajari" di panduan wudhu
  hilang sendiri. Semuanya kini membaca `KunciProgres` di `core/util/`, dan aturannya dibuat lebih
  tegas: **tidak ada satu pun pemanggilan `progress`/`setProgress` yang boleh memberi rujukan berupa
  literal** — rujukan yang berupa id konten (mis. id sholat) tetap boleh karena ia variabel, bukan
  ejaan yang disalin. Nilainya dibekukan karena baris itu sudah ada di perangkat pengguna. Dijaga
  `KunciProgresSatuTempatTest` (6 uji), dengan enam mutasi terverifikasi.
- **Jenis bacaan tersimpan kini satu tempat**: bagian sebelum `:` pada id bacaan — `yasin`, `tahlil`,
  `dzikir`, `adzan`, `iqomah`, `doa`, `ghaib`, `jenazah`, `bacaan`, `niat`, `langkah`, `search` —
  adalah **kunci sambung** antara penulis dan pembaca: layar menulisnya sebagai awalan id kartu yang
  disimpan `BacaanCard` ke favorit, riwayat, dan progres, lalu `RuteKonten` membacanya kembali untuk
  menentukan layar yang dibuka tombol "Buka". Keduanya dahulu mengeja jenisnya sendiri-sendiri —
  `"yasin"` di layar Yasin **dan** di `RuteKonten`, `"adzan"` di layar Adzan dan di `RuteKonten` —
  sehingga satu huruf yang berubah di salah satu sisi membuat favorit terbuka ke layar yang salah
  atau berakhir *"Layar tujuan tidak dikenal"*, **tanpa satu pun pesan galat**; `PreferensiSusunan`
  bahkan menyimpan ejaan ketiga untuk `"tahlil"`. Kini semuanya membaca kosakata bersama
  `JenisBacaan` di `core/util/`, lengkap dengan pemisah id dan aturan membacanya
  (`id`/`pisah`/`jenisDari`) — begitu pula `splitContentId` dan migrasi favorit warisan di
  `YasinApp`, yang dahulu menyalin pemisah `':'` sendiri. Id yang tersimpan **tidak berubah satu
  byte pun** (`yasin:58`, `ghaib:niat:syafii`, `langkah:<id sholat>:<id bacaan>`), sehingga favorit,
  riwayat, dan progres lama tetap menunjuk bacaan yang sama. Dijaga `JenisBacaanSatuTempatTest`,
  yang sekaligus mengunci ejaan tiap jenis: string itu tersimpan di perangkat dan bertahan melintasi
  pembaruan aplikasi, jadi menggantinya harus disengaja dan disertai migrasi.
- **Sebutan untuk hal yang sama kini satu ejaan**: tiga sebutan ditulis di beberapa tempat
  sekaligus. **Nama berkas modul sholat khusus** (`sholat_ghaib.json`, `sholat_jenazah.json`) ada di
  layar Sholat Ghaib, layar Sholat Jenazah, dan indeks pencarian; nama berkas adalah kunci ke bundel
  konten, jadi salah ketik di satu tempat membuat layar menampilkan "konten tidak tersedia"
  sementara pencarian tetap mendaftarkannya. Kini keduanya tinggal di `BerkasKonten` — bersama
  layar tujuannya, sebab pencarian harus menunjuk ke mana hasilnya membuka — dan sebuah uji
  memastikan berkas yang disebutnya benar-benar ada di `assets/content`. **Judul satu ayat Yasin**
  (`Yasin ayat 58`) ditulis di `YasinViewModel` (riwayat & favorit) dan di indeks pencarian,
  sehingga ayat yang sama dapat muncul dengan dua nama; kini `JudulBacaan.yasinAyat(nomor)`.
  **Nama mazhab Asar** beserta faktor bayangannya (`Syafi'i (1)`) ditulis di menu Jadwal dan menu
  Pengaturan yang keduanya mengatur `Store.ASAR` — angkanya bahkan diketik langsung di kepala layar
  Jadwal padahal faktor itu dipakai perhitungan; kini `JadwalHariIni.ASAR_SEMUA`/`asarLabel`/
  `asarLabelFaktor` membacanya dari `asarFactor`. Sekalian: mengubah mazhab Asar dari menu
  Pengaturan dahulu hanya menyimpan nilainya — pengingat yang sudah terjadwal tetap memakai mazhab
  lama sampai ada hal lain memicu penjadwalan ulang; kini layar itu menyusun ulang pengingat seperti
  menu Jadwal. Dijaga `SebutanSatuTempatTest`.
- **Kalimat penjelas keadaan yang sama kini tinggal di satu tempat**: tiga kalimat yang dilihat
  pengguna ditulis berulang, dan tiap salinan dapat berbeda sendiri. *"Belum ada perbaikan lokasi
  GPS…"* ada di `JadwalViewModel` **dan** `KiblatViewModel` — penjelasan atas satu kegagalan yang
  sama, sehingga pengguna yang berpindah layar membaca dua sebab berbeda; kini sebabnya satu di
  `PesanLokasi`, sedangkan **saran**nya tetap berbeda karena memang berbeda jalan keluarnya (Jadwal
  punya pemilih kotanya sendiri, Kiblat tidak). Saran *"Berkas … tidak ditemukan di aplikasi.
  Pasang ulang aplikasi bila ini terus terjadi."* ada di empat layar (Tahlil, Pengaturan, Dzikir,
  Adzan), masing-masing menuliskan sebabnya sendiri; kini kalimatnya disusun `PesanKonten.berkasTidakAda(nama)`
  dan pemanggil hanya menyebut **berkas mana** yang hilang. Penjelasan pengingat "baca doa setelah
  adzan" ada di layar Adzan **dan** layar Doa Adzan dengan kata-kata masing-masing, padahal
  keduanya mengatur saklar yang sama — sebab, ikut-saklar, dan batas syariatnya kini dari
  `PengingatAdzan` (`DOA_SEBAB`/`DOA_IKUT_SAKLAR`/`DOA_BUKAN_SYARIAT`), dan jeda 5 menitnya
  (`DOA_DELAY_MIN`) dibaca penjadwal dari konstanta yang sama dengan yang disebut penjelasan itu —
  dahulu penjadwal menyimpan angkanya sendiri sehingga penjelasan di layar dapat menyebut angka
  yang tidak pernah dipakai notifikasi. Dijaga `PesanKeadaanSatuTempatTest`.
- **Urutan nama waktu sholat punya satu tempat**: pasangan nama→jam sholat (`"Subuh"` ↔
  `t.subuh`, …) ditulis di **tiga** tempat. `JadwalHariIni` adalah pemiliknya, tetapi
  `PrayerCalc.next` menyalinnya sendiri lalu mengembalikan **nama saja**, dan baris jam beranda
  menyalinnya sekali lagi. Ketiganya harus sepakat tanpa apa pun yang menjaganya: bila salah satu
  bergeser, hitung mundur beranda menunjuk satu waktu sementara baris jam di bawahnya menampilkan
  jam waktu lain — tanpa satu pun pesan galat. Pemanggil `next` pun harus mencarikan jamnya
  sendiri lewat `JadwalHariIni.jam(...)` dengan cabang cadangan `?: times.isya`, cabang yang
  diam-diam menampilkan jam sholat lain daripada tak ada. `PrayerCalc.next` kini dihapus;
  `JadwalHariIni.berikutnya()` mengembalikan **nama, jam, dan sisa menitnya sekaligus** (tipe
  `JadwalHariIni.Berikutnya`) dengan urutan dari `ENAM_WAKTU` yang sama dengan `jam()`, `semua()`,
  dan `adzan()` — sehingga nama dan jamnya tak dapat dipisahkan lagi, dan tak ada cabang cadangan
  yang perlu ditebak. Baris jam beranda memakai `LIMA_WAKTU.zip(adzan(times))`. Penjaga
  `pasanganNamaJamHanyaDitulisDiJadwalHariIni` gagal bila medan `PrayerTimes` diakses langsung dari
  berkas lain, dan `PrayerCalcTest` kini mengambil urutan field dari `JadwalHariIni.semua()`.
- **Zona waktu tampilan punya satu aturan, dan tak ada layar yang menyusunnya sendiri**: `Store.TZ`
  menyimpan id zona waktu pilihan pengguna, dan dua bentuk zona dipakai di seluruh aplikasi —
  `TimeZone` untuk jam beranda, penjadwal pengingat, menu Adzan, dan layar Jadwal; `ZoneId` untuk
  tanggal butir info harian. Keduanya punya aturan sendiri-sendiri untuk id yang **tak dikenal**:
  `TimeZone.getTimeZone` mengembalikan **GMT** diam-diam, sedangkan `ZoneId.of` melempar. Id yang
  rusak — bukan kasus teoretis, sebab `Store.TZ` ikut backup/pemulihan sehingga nilai dari versi
  lain dapat masuk tanpa melewati daftar pilihan layar Pengaturan — karena itu membuat jam beranda
  dan penjadwal pengingat memakai GMT sementara kartu info harian memakai zona perangkat: dua waktu
  berbeda untuk hari yang sama. Beranda pun menyusun zonanya sendiri langsung di composable
  (`if (tzId.isBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(tzId)`), salinan aturan yang
  ketiga. Aturannya kini satu — `JadwalHariIni.zonaId`, jatuh ke zona perangkat untuk id kosong
  **maupun** tak dikenal — dan `JadwalHariIni.zona` hanya bentuk `TimeZone`-nya, `InfoHarian.zona`
  memakainya, dan beranda memanggil `JadwalHariIni.zona()`. Dijaga `JadwalHariIniTest`, termasuk
  penjaga bahwa tidak ada berkas di `feature/` yang menyusun zona waktunya sendiri.
- **Ejaan kunci konten dibaca sama oleh pemeriksa dan pembaca**: skema menerima dua ejaan untuk
  sumber tingkat berkas (`sumber_url`/`sumberUrl`) dan untuk rujukan berkas
  (`rujukan_file`/`rujukanFile`, ditambah larik `rujukan`). `BundelKonten` dan gerbang CI bundel
  mengenal keduanya, tetapi **pembaca** kontenlah yang tertinggal: `ContentRepo` hanya membaca
  `sumber_url` (kecuali `dzikir()`, satu-satunya yang mengenal `rujukanFile`), sedangkan Tahlil dan
  bacaan sholat membaca `rujukan_file` saja. Bundel ber-`sumberUrl` karena itu **lolos pemasangan**
  dan dilaporkan *"terpasang"*, lalu setiap berkasnya ditolak pembaca saat dibuka — tanpa sumber
  resmi, berkas pembaruan dianggap tak bersumber — sehingga aplikasi diam-diam kembali ke konten
  bawaan: pembaruan yang tampak berhasil tanpa mengubah apa pun, tanpa satu pun pesan yang menyebut
  sebabnya. Pada Tahlil dan bacaan sholat hasilnya teks **kosong tanpa galat**, sebab rujukan
  ber-`rujukanFile` sudah "diperiksa" sebagai sah tetapi tidak pernah dibaca. Daftar ejaan kini
  hidup di satu tempat — `ContentValidator.KUNCI_SUMBER`/`sumberBerkas()` untuk sumber,
  `Rujukan.KUNCI_RUJUKAN`/`rujukanTeks()`/`rujukanTeksSemua()` untuk rujukan — dan seluruh pemeriksa
  maupun pembaca memakainya lewat fungsi bersama, termasuk `ContentUpdateManager` saat membaca
  kembali metadata yang ditulisnya sendiri. `rujukanTeksSemua` mengembalikan **setiap** ejaan yang
  ada, bukan hanya yang pertama: pemeriksa menelusuri tiap kemunculan kunci rujukan, jadi pembaca
  yang hanya mengambil satu akan memeriksa sesuatu yang tidak pernah ia baca. Dijaga
  `KunciKontenSatuTempatTest`, `RujukanTest`, dan `ContentValidatorTest`.
- **Rujukan berkas pada bundel pembaruan diperiksa saat dipasang, bukan saat layarnya dibuka**:
  entri konten dapat **menunjuk** lafaz yang sudah terverifikasi di berkas lain
  (`rujukan_file` = `berkas.json#id`, larik `rujukan`) alih-alih menyalinnya. Tetapi
  `BundelKonten` hanya memeriksa sumber resmi, kategori, harakat, dan placeholder — tidak pernah
  sasarannya. Bundel dengan rujukan mati karena itu **diterima sebagai sah**, lalu
  `ContentRepo.doc` melempar `FileNotFoundException` di dalam `remember { repo.dzikir() }` pada
  layar, sehingga satu rujukan mati menghentikan **seluruh** layar Dzikir/Tahlil/Panduan Sholat
  tanpa satu pun petunjuk sebabnya — jauh dari tempat yang dapat diperbaiki pengguna. Kini
  `BundelKonten.periksaRujukan` menolak bundel yang merujuk berkas tak ada (di dalam bundel
  maupun di antara berkas bawaan), entri `#id` yang tidak ada di berkas target, bentuk yang salah
  (`"rujukan"` berupa teks padahal pembacanya memakai `getJSONArray`), dan rujukan kosong.
  Rujukan yang tetap lolos — mis. berkas bawaan yang sasarannya hilang karena satu berkas dihapus —
  tidak lagi menjatuhkan layar: `resolveArabLatinTerjemah` mengembalikan teks kosong sehingga jalan
  mundur ke teks milik entri sendiri (`o.optString("arab", a)`) yang sudah ada tetap bekerja.
  Dijaga `BundelKontenTest`.
- **Beranda dan layar Favorit tidak lagi membaca aturan tujuan dengan cara berbeda**: `RuteKonten`
  memutuskan layar tujuan sebuah bacaan tersimpan, tetapi beranda memanggil `ruteTerakhir` **tanpa**
  id kelompok dzikir lalu `namaTujuan` tanpa argumen apa pun, sementara layar Favorit menyertakan
  keduanya. Jenis yang sama karena itu dapat terbaca berbeda di dua layar: bacaan dzikir terakhir
  tampil sebagai *"Layar tujuan tidak dikenal"* di beranda walau layar Favorit mengenalinya, dan nama
  tujuan yang kosong membuat baris tersimpan tampak seperti tautan rusak. Rute dan namanya kini
  diputuskan bersama lewat `RuteKonten.tujuan(kind, grupDzikir)` / `tujuanTerakhir(last, grupDzikir)`,
  dan beranda menerima id kelompok dzikir dari `HomeViewModel` — dari **pembacaan bundel yang sama**
  dengan butir info harian, sehingga satu pembacaan menghasilkan satu sebab kegagalan. Kalimat
  *"Layar tujuan tidak dikenal"* kini satu konstanta (`RuteKonten.TIDAK_DIKENAL`): dahulu ia disalin
  ke `FavoritScreen` dua kali sehingga satu baris tanpa tujuan menampilkannya **dua kali
  berturut-turut**, dan `HomeScreen` menyimpannya sekali lagi. Dijaga `RuteKontenTest`.
- **Berkas konten yang gagal dibaca tidak lagi menyembunyikan domainnya dari layar Tentang**:
  layar Tentang menjanjikan bahwa daftar sumbernya *"tidak dapat bertambah diam-diam"*, tetapi
  `ContentRepo.sumberDipakai()` melewati berkas konten yang gagal diurai
  (`mapNotNull { runCatching { doc(f) }.getOrNull() }`) dan tetap mengembalikan daftar yang tampak
  utuh — sehingga daftar itu justru dapat **menyusut** tanpa satu pun tanda, dan bagi pengguna
  daftar yang kehilangan satu domain tidak dapat dibedakan dari daftar yang memang lengkap.
  Kegagalannya kini dibawa keluar sebagai `SumberBundel(host, gagal)`: berkas yang tak terbaca
  beserta sebabnya dicatat, dan layar Tentang menampilkannya di atas daftar agar janji
  "tidak bertambah diam-diam" tidak berhenti pada satu arah saja. Kegagalan membaca direktori
  aset pun tidak lagi menyamar sebagai "tidak ada sumber". Dijaga `TentangTest`.
- **Kegagalan membaca kelompok dzikir tidak lagi menyamar sebagai "Layar tujuan tidak dikenal"**:
  `FavoritScreen` membaca id kelompok dzikir dari bundel konten dengan
  `runCatching { repo.dzikir() … }.getOrDefault(emptySet())`. Kegagalan membaca bundel karena itu
  menjadi himpunan kosong, dan setiap baris favorit/riwayat **dzikir** — yang `kind`-nya adalah id
  kelompok, bukan `"dzikir"` — jatuh ke keterangan *"Layar tujuan tidak dikenal"*. Padahal tujuannya
  memang dikenal (layar Dzikir & Doa); yang gagal adalah mengenali id kelompoknya, sehingga pengguna
  menyimpulkan bacaannya hilang dan menandainya ulang. Id kelompok kini dibaca `FavoritViewModel`
  dan sebab kegagalannya dibawa ke layar lewat `dzikirGagal` — ditampilkan di atas daftar, sama
  seperti `kotaGagal` pada layar Jadwal — bukan disamarkan. Dijaga `RuteKontenTest`.
- **URL sumber di dalam daftar JSON tidak lagi luput dari pemeriksaan**: aturan `SOURCES.md`
  mengharuskan setiap URL yang dinyatakan sebagai **sumber** berasal dari domain resmi, dan layar
  Tentang menjanjikan kepada pengguna bahwa *"berkas konten yang menyebut domain lain ditolak saat
  dibangun maupun saat diperbarui"*. Kenyataannya penelusurnya hanya menangani objek: elemen di
  dalam **daftar** tidak pernah diperiksa, sehingga `sumber_tambahan` — yang berbentuk daftar URL
  pada `sholat_ghaib.json` dan `sholat_jenazah.json` — dapat memuat domain mana pun tanpa satu pun
  pemeriksaan gagal. Lubangnya dibuktikan dengan menyisipkan domain non-resmi ke elemen pertama
  `sumber_tambahan`: gate tetap melaporkan `0 galat`. Kini kunci induk dibawa turun ke elemen daftar
  di **ketiga** jalur yang memakainya — gate `tools/validate_content.py`, penelusur bersama
  `ContentValidator.sumberUrl`, dan daftar sumber layar Tentang (lewat `ContentRepo.sumberDipakai`)
  — supaya ketiganya tidak dapat berbeda. Karena URL di `sumber_tambahan` kebetulan semuanya resmi,
  kehilangan kemampuan menelusuri daftar **tidak terlihat** dari hasil hijau; uji karenanya menuntut
  penelusur benar-benar membaca isi daftar pada bundel nyata, bukan sekadar tidak menemukan
  pelanggaran. Dijaga `ContentSourceTest` dan `TentangTest`.
- **"Info Islami harian" di beranda kini benar-benar harian**: kartu itu memuat **satu kalimat
  tetap** — *"Perbanyak sholawat di hari Jumat (HR Abu Dawud). Sumber: sunnah.com"* — yang
  ditampilkan setiap hari. Judulnya "harian", isinya hari Jumat: pada hari Senin pengguna membaca
  anjuran yang menyebut hari Jumat, dan sejak hari kedua kartu itu berhenti memberi apa pun yang
  baru — padahal `plan.txt` §"Beranda Utama" meminta *"kartu informasi Islami harian"*. Isinya kini
  berganti mengikuti tanggal, dan **tidak satu pun teks baru dikarang**: butir harian diambil dari
  pustaka dzikir yang sudah terverifikasi di bundel konten (lafaz berharakat, terjemahan, dan
  sumbernya ikut ditampilkan), sedangkan hari Jumat memakai anjuran sholawat yang memang khusus hari
  itu. Tanggalnya diikuti dari zona waktu tampilan, sama seperti jam di atasnya, sehingga kartu tidak
  menyebut hari yang berbeda dari tanggal yang tertera. Bundel konten yang tidak terbaca menyisakan
  kartu tanpa butir **beserta sebabnya** — bukan butir milik hari lain. Aturannya murni (tanpa
  Android) di `InfoHarian` dan dijaga `InfoHarianTest`.
- **Kegagalan memutar adzan tidak lagi ditelan**: aplikasi tidak menyertakan berkas adzan (lisensi),
  sehingga berkasnya dipilih pengguna sendiri dan **berkas yang dipindah atau dicabut izinnya adalah
  keadaan yang wajar**. Dahulu dua kegagalan pada jalur itu dibuang begitu saja: nilai balik
  `AdzanPlayer.playOnce` diabaikan, dan kegagalan `setForeground` ditelan `runCatching`. Akibatnya
  yang terdengar adalah **hening**: notifikasi senyap sudah tampil, tidak ada suara, dan tidak ada
  satu pun jejak untuk menelusuri sebabnya — pengguna yang kehilangan suara adzan juga kehilangan
  cara mengetahuinya. Kini penolakan layanan latar oleh sistem (mungkin sejak Android 12) mematikan
  audio **sebelum** notifikasi dipilih, sehingga kanal bersuara yang dipakai — pengguna tetap
  dikejutkan tepat waktu meski berkasnya bermasalah; dan bila berkasnya sendiri gagal diputar,
  sebabnya dicatat ke `logcat` (`Log.w(TAG)`) **dan** notifikasi senyap tadi **diganti** (id sama,
  bukan ditumpuk) oleh notifikasi yang menyebut berkas adzan tidak dapat diputar beserta jalan
  keluarnya. Kanal notifikasi kini dipilih dari audio yang **benar-benar** diputar
  (`notifyAdzan(nama, audio, metode)`), bukan dari niat memutarnya. Dijaga `LapisanAudioTest`.
- **Daftar langkah sholat yang kosong tidak lagi menghentikan aplikasi**: bundel konten dapat
  diperbarui pengguna lewat layar "Pembaruan konten", sehingga id sholat yang dahulu ada bisa hilang
  dari `sholat_bacaan.json` dan daftar langkahnya menjadi kosong. Dahulu baris
  `steps[step.coerceIn(0, steps.size - 1)]` menghitung `coerceIn(0, -1)` — **rentang kosong** — dan
  aplikasi berhenti begitu layar ini dibuka, sementara bilah kemajuan membagi dengan `steps.size`.
  Kekosongan itu kini ditangani lebih dahulu dan **diberitahukan** (sebabnya serta cara memuat ulang
  bundel), dan bagian sesudahnya — tautan dzikir setelah sholat dan catatan mazhab — tetap
  ditampilkan karena tetap berguna. Dijaga `LangkahSholatTest`.
- **Daftar kota yang gagal dibaca tidak lagi menjadi daftar kosong tanpa keterangan**:
  `JadwalViewModel` membaca daftar kota dengan `runCatching { repo.kota() }.getOrDefault(emptyList())`,
  sehingga bundel konten yang bermasalah membuat tombol "Kota: …" membuka **daftar kosong tanpa satu
  pun penjelasan** — pengguna menyimpulkan tombolnya rusak, padahal bundelnya yang bermasalah.
  Sebabnya kini dibawa ke layar lewat `kotaGagal`, sejalan dengan `gagalMuat` untuk preferensi, dan
  layar menampilkannya saat daftarnya kosong. Dijaga `JadwalLokasiTest`.
- **Konversi satuan azimut tidak lagi ditulis ulang di layar**: layar Kiblat menghitung sendiri
  `kiblat * MIL_PER_DEG` dengan konstanta `17.7778` miliknya sendiri, sedangkan aturan satuan
  (`TampilanKompas`) sudah memutuskan satuan mana yang sah dan bagaimana ia dinamai. Satuan yang
  ditambahkan kelak harus tahu cara mengonversi agar tidak tampil sebagai derajat — persis kerusakan
  yang pernah terjadi pada kunci satuannya (dahulu ditulis di tiga berkas). Konversinya kini tinggal
  di `TampilanKompas.formatAzimut`, dengan perilaku yang sama seperti `labelSatuan`: satuan tak
  dikenal diperlakukan sebagai derajat, sehingga pilihan yang rusak tidak mengubah angkanya. Dijaga
  `TampilanKompasTest`.
- **Penjaga "isi berkas" tidak lagi tertipu oleh komentarnya sendiri**: beberapa uji di repositori
  ini bekerja dengan mencari potongan kode di dalam berkas lain ("layar ini tidak boleh lagi
  menghitung tujuannya sendiri"). KDoc yang menjelaskan pola lama memuat tulisan yang **sama persis**
  dengan yang dilarang, sehingga penjaganya gagal justru karena penjelasannya bagus — keliru tiga
  kali dalam satu rangkaian perubahan. Pemindai yang membuang komentar lebih dahulu kini tinggal di
  satu tempat (`KodeSumber`), bukan salinan yang berbeda-beda di tiap berkas uji — persis kekeliruan
  yang sedang dihindari penjaga yang memakainya.
- **"Terakhir dibaca" di beranda membuka layar yang benar**: beranda memutuskan tujuannya sendiri
  dengan `if (last.startsWith("yasin")) YASIN else FAVORIT`, sedangkan `Store.LAST` hanya pernah
  berisi `yasin:…` dan `tahlil:…` — jadi membaca Tahlil lalu menekan **"Lanjutkan"** membuka layar
  **Favorit**, bukan Tahlil. Layar Favorit sendiri sudah memetakan nilai yang sama dengan benar,
  sehingga satu nilai punya dua jawaban berbeda. Kini keduanya memakai satu aturan di `RuteKonten`
  (`ruteTerakhir`), tombol menampilkan nama layar tujuannya, dan jenis yang tidak punya layar
  sendiri ditampilkan sebagai keterangan — bukan diarahkan ke layar lain. Dijaga `RuteKontenTest`.
- **Bawaan preferensi kini satu rumah** (`BawaanPreferensi`): nilai bawaan ditulis ulang sebagai
  literal di setiap tempat yang membaca preferensinya — `store.get(Store.SHOW_ARAB, true)` di
  sebelas layar, `store.get(Store.NOTIF, true)` di empat berkas, batas koreksi Hijriah `-2`/`2` di
  dua tombol. Preferensinya satu kunci, tetapi bawaannya bercabang: begitu satu layar mengubah
  bawaannya, layar lain diam-diam tidak ikut berubah, dan pengguna yang belum pernah menyentuh
  pengaturannya melihat perilaku berbeda di tiap layar — kerusakan yang pernah benar-benar terjadi
  pada ukuran bacaan (`UkuranBacaan`). Seluruh bawaan kini ada di satu objek murni-tanpa-Android,
  termasuk pembatas koreksi Hijriah (`koreksiHijriah`), dan **tidak satu pun nilai berubah** — ini
  penggabungan salinan, bukan perubahan perilaku. `BawaanPreferensiTest` memindai seluruh
  `app/src/main` dan gagal bila ada pembacaan preferensi yang membawa bawaannya sendiri sebagai
  literal.
- **`workflow_dispatch` pada alur rilis tidak lagi menerbitkan rilis palsu**: `release.yml`
  mendeklarasikan `workflow_dispatch`, tetapi langkah `softprops/action-gh-release` tidak punya
  pembatas ref — dan action itu menormalkan `github.ref_name` menjadi nama tag. Akibatnya menjalankan
  alur rilis dari sebuah cabang (mis. untuk menguji build rilis) akan mencoba menerbitkan rilis
  bernama cabang itu. Kini langkah rilis dijaga `github.ref_type == 'tag' || inputs.tag != ''`, dan
  dispatch mendapat masukan **`tag`**: diisi → menerbitkan rilis untuk tag itu, dikosongkan → sekadar
  menguji build rilis tanpa menerbitkan apa pun. Alur tag (`v*.*.*`) tidak berubah.
- **Higiene berkas teks**: 13 berkas konten JSON (`adzan`, `ayat_kursi`, `dzikir`, `kota`,
  `sholat_bacaan`, `sholat_daftar`, `surah_001/112/113/114`, `tahlil`, `tahlil_full`,
  `yasin_full`) tidak diakhiri baris baru, dan `YasinApp.kt` menyisakan satu baris kosong di
  akhir — keduanya menimbulkan "\\ No newline at end of file" pada `git diff` dan derau pada tiap
  suntingan berikutnya. Dirapikan `tools/format_teks.py`; diff-nya murni spasi/baris baru (tidak
  satu karakter bermakna berubah, dibuktikan `git diff -w --ignore-blank-lines` yang kosong), dan
  ke-18 berkas JSON tetap sah dibaca. Dijaga gate `format_teks.py` di `ci.yml`.
- **Pilihan tampilan tidak lagi lenyap saat perangkat diputar**: `MainActivity` tidak mengunci
  orientasi (`android:screenOrientation` tidak ada di manifest), jadi memutar perangkat membangun
  ulang Activity beserta seluruh komposisinya — dan apa pun yang disimpan di `remember` biasa ikut
  hilang. Yang paling merugikan adalah posisi **langkah panduan sholat**: pengguna yang sedang di
  langkah ketujuh terlempar kembali ke langkah pertama hanya karena memutar ponsel. Hal yang sama
  terjadi pada tab panduan sholat dan kelompok dzikir, cara pengelompokan, mode layar penuh
  (Yasin, Tahlil, Kiblat, Dzikir, Ghaib, Jenazah, Iqomah), mode jamaah dan panel susunan Tahlil,
  mode ringkas dan tanda selesai panduan sholat, mode baca fokus dan layar tetap menyala Yasin,
  daftar bookmark, serta kata kunci yang sedang diketik di Yasin dan Pencarian — semuanya kini
  `rememberSaveable`. Keadaan yang **sengaja** tetap `remember` (panel pengaturan modal, indeks
  pencarian dan pesan galat yang dibangun ulang oleh efek, penghitung percobaan ulang) dinyatakan
  terang-terangan di `KeadaanLayarSaatDiputarTest`, supaya tidak ada yang "memperbaikinya" keliru.
- **Bilah sistem tidak lagi memakan tepi layar**: `plan.txt` §"UI/UX dan Navigasi" meminta *"Layout
  responsif untuk berbagai ukuran layar"*, tetapi yang dijaga baru **lebarnya** — inset bilah sistem
  sama sekali tidak dihitung. Jendela memakai bilah berlatar warna, sehingga pita di sekitar poni
  (notch) dan di belakang bilah gestur menjadi ruang mati, dan ikon bilah sistem mengikuti tema
  **perangkat** — bukan tema yang dipilih pengguna, sehingga memilih tema yang berlawanan dengan
  perangkatnya meninggalkan ikon status bar terang di atas latar terang. Kini `MainActivity`
  memanggil `enableEdgeToEdge()` **sebelum** `setContent` (jendela harus diatur sebelum bingkai
  pertama digambar), warna ikon bilah status dan navigasi disinkronkan dari nilai `dark` yang sama
  dengan tema aplikasi lewat `SideEffect`, dan inset-nya ditambahkan sekali di `AppNav` oleh
  `Scaffold` — yang bawaannya memang `WindowInsets.systemBars` — sehingga setiap layar ikut tanpa
  pengkabelan per layar. Warna bilah di `themes.xml` sengaja **dibiarkan**: itu nilai untuk jendela
  pembuka sebelum Compose menggambar, dan zamrud `#0B7A55` memang butuh ikon terang. Ini juga
  menyiapkan aplikasi bila `targetSdk` kelak dinaikkan: mulai API 35 tepi-ke-tepi diberlakukan
  sistem, dan tata letak yang tak menghitung inset akan tertutup bilah. Dijaga
  `TataLetakResponsifTest` (`aplikasiMenggambarTepiKeTepi`, `insetSistemDitambahkanDiNavigasi`,
  `ikonBilahSistemMengikutiTemaAplikasi`), yang juga menolak `Scaffold` yang mematikan insetnya
  sendiri dengan `WindowInsets(0)`.
- **Bawaan konfigurasi jadwal dan pengingat kini punya satu rumah, bukan sembilan salinan**: kuncinya
  memang satu, tetapi **bawaannya** bercabang di mana-mana. Koordinat Jakarta (`-6.2`/`106.8`) ditulis
  sembilan kali di `PrayerScheduler`, `HomeViewModel`, `JadwalViewModel`, `KiblatViewModel`, dan layar
  Adzan; `"KEMENAG"` lima kali, `"syafii"` lima kali (termasuk `JadwalViewModel.setAsarHanafi` yang
  memetakannya sendiri dari boolean), `"Jakarta"` tiga kali, `"0,0,0,0,0,0"` empat kali, zona waktu
  `""` empat kali, `"derajat"`/`"mil"` tiga kali, dan CSV saklar `"1,1,1,1,1"` dua kali. Tak ada satu
  pun tempat yang tahu berapa bawaannya seharusnya: bila salah satunya berbeda, layar Jadwal, arah
  Kiblat, layar Adzan, dan pengingat latar menghitung untuk tempat yang berlainan **tanpa satu pun
  pesan galat** — pengguna hanya melihat dua jam yang tidak sama untuk sholat yang sama. Semuanya kini
  tinggal di rumah masing-masing: `JadwalHariIni` (`LAT_DEFAULT`, `LON_DEFAULT`, `KOTA_DEFAULT`,
  `METODE_DEFAULT`, `ASAR_SYAFII`/`ASAR_HANAFI`/`ASAR_DEFAULT`, `CORR_DEFAULT`, `TZ_DEFAULT`),
  `PengingatAdzan` (`ON_DEFAULT`, `PRE_MIN_DEFAULT` beserta rentangnya), `TampilanKompas`
  (`SATUAN`/`SATUAN_DEFAULT`/`satuanSah`/`labelSatuan`), dan `Mazhab` yang baru (`core/sholat/Mazhab.kt`,
  menggantikan daftar yang dahulu disalin di layar panduan dan layar Pengaturan). Dijaga
  `JadwalHariIniTest`, `PengingatAdzanTest`, `TampilanKompasTest`, dan `MazhabTest`, yang masing-masing
  menolak bawaannya ditulis ulang di luar rumahnya.
- **Rentang yang dahulu hanya ada di slider kini juga menjepit jalur baca**: batas koreksi menit
  (`±30`) hidup hanya sebagai `valueRange` slider di layar Jadwal, dan batas jeda pengingat sebelum
  adzan (`0..30`) hanya sebagai `valueRange` di layar Adzan — nilai tersimpan di luar rentang itu (mis.
  dari backup yang disunting) dihormati apa adanya oleh seluruh layar **dan** penjadwal, sehingga
  jadwal dapat bergeser jauh lebih banyak daripada yang dapat dipilih pengguna. Keduanya kini dijepit
  saat dibaca (`JadwalHariIni.parseCorr` dan `PengingatAdzan.preMenit`), dengan rentang dari satu
  sumber yang sama dengan slidernya.
- **Arah kiblat bawaan dihitung, bukan angka tetap**: `KiblatUiState` memulai dengan `295.0` yang
  ditulis tetap — benar untuk Jakarta dan **hanya** Jakarta. Angka itu harus disunting sendiri setiap
  kali lokasi bawaan berubah, dan bila terlewat layar sempat menampilkan arah kiblat kota lain. Kini
  ia dihitung `QiblaCalc.bearing(LAT_DEFAULT, LON_DEFAULT)` dengan rumus yang sama dengan nilai
  sesungguhnya.
- **Mode tema kini punya satu daftar, satu bawaan, dan satu pemetaan**: nilai `"terang"`, `"gelap"`,
  dan `"sistem"` beserta artinya dahulu tinggal di **tiga** berkas — `MainViewModel` menulis bawaan
  `"sistem"`, `MainActivity` memetakan `"terang"`/`"gelap"` ke `dark: Boolean` lewat rantai `when`,
  dan layar Pengaturan menyusun sendiri daftar chip `listOf("terang", "gelap", "sistem")`. Kuncinya
  memang satu (`Store.DARK`), tetapi daftarnya tiga; menambah satu mode berarti harus ingat
  menyunting ketiganya, dan mode yang lolos dari pemetaan diam-diam jatuh ke "ikut sistem" —
  pilihan pengguna tampak tersimpan, chip-nya terlihat aktif, tetapi tema tidak berubah. Ketiganya
  kini di `TemaAplikasi` (murni, tanpa Android, dapat diuji di JVM), termasuk pemetaannya
  (`gelap(v, sistemGelap)`), sehingga mode yang tak dikenal pun jatuh dengan cara yang sama di mana
  pun. Dijaga `TemaAplikasiTest`.
- **Ukuran bacaan kini punya satu bawaan dan satu rentang**: `Store.ARAB_SIZE` dibaca di **sebelas**
  tempat dengan bawaan yang bercabang — enam memakai `22f`, lima `24f` — ditambah tiga nilai awal di
  ViewModel (22/24/24). Kuncinya memang satu, tetapi selama pengguna belum pernah menyentuh
  slidernya bacaan yang sama dirender 22 sp di layar Sholat dan 24 sp di layar Yasin, sementara layar
  Pengaturan menyebut 24: tiga angka untuk satu pengaturan, tanpa satu pun pesan galat. Rentang
  slidernya pun tinggal di `UiKit` sementara bawaannya tersebar, sehingga tak ada satu tempat yang
  dapat memastikan bawaannya masuk rentang. Keduanya kini di `UkuranBacaan` (murni, tanpa Android,
  dapat diuji di JVM), termasuk angka `+ 8`/`44f` untuk bacaan jarak jauh yang dahulu ditulis ulang
  di empat layar (Ghaib, Jenazah, Iqomah, Tahlil) — tiga di antaranya bahkan tanpa batas atas,
  sehingga ukuran maksimum 40 melompat ke 48 dan melewati lebar kartu. Dijaga `UkuranBacaanTest`,
  yang juga menolak bawaan maupun angka jarak jauh itu ditulis ulang di luar `UkuranBacaan`.
  Bawaan jarak antarbaris yang sama (`1.9f`) pun masih tertulis di tanda tangan `ArabText` — satu
  salinan lagi yang tak terlihat oleh layar Pengaturan; kini diambil dari
  `UkuranBacaan.JARAK_DEFAULT`.
- **Aturan preferensi pengingat adzan kini juga tunggal**: CSV saklar per waktu (`Store.ADZAN_ON`)
  diurai di dua tempat — `PrayerScheduler` (pembaca) dan layar Adzan (penulis) — dengan panjang dan
  padding yang ditulis ulang masing-masing; bila keduanya berbeda, menyalakan satu waktu di layar
  mematikan waktu lain di penjadwal. Batas panjang timer iqomah (1–60 menit) pun ditulis di **tiga**
  tempat: dua tombol di layar Iqomah, penjadwal pengingat, dan pekerja latar — sehingga layar dapat
  menampilkan angka yang tidak pernah dipakai notifikasi. Keduanya kini tinggal di `PengingatAdzan`
  (murni, tanpa Android): `parseOn`/`serializeOn` dan `iqomahMenit`, dipakai keempat berkas itu.
  Dijaga `PengingatAdzanTest`, dan `AdzanJadwalTest` menolak salinan barunya muncul lagi. Hal yang
  sama berlaku untuk volume suara: bawaan `0.8f` ditulis di **dua** tempat — pekerja latar yang
  benar-benar memutar suaranya dan layar Adzan yang menampilkan slidernya — sehingga angka di layar
  bisa berbeda dari volume yang berbunyi; kini satu, `PengingatAdzan.VOL_DEFAULT`.
- **Urutan nama waktu sholat kini juga tunggal**: nama lima waktu beradzan (`Subuh, Zuhur, Asar,
  Magrib, Isya`) dan enam waktu berkoreksi (dengan `Terbit`) dahulu ditulis ulang di **empat**
  tempat — `PrayerScheduler`, layar Adzan, layar Jadwal, dan beranda. Urutan itu bukan hiasan:
  `Store.ADZAN_ON` adalah CSV lima angka yang indeksnya berarti waktu ke-`i`, dan `Store.CORR` CSV
  enam angka dengan arti yang sama. Daftar yang berbeda urutan membuat mematikan "Magrib"
  mematikan waktu lain, atau koreksi Asar diterapkan pada Zuhur — tanpa satu pun pesan galat.
  Keduanya kini tinggal di `JadwalHariIni` (`LIMA_WAKTU`, `ENAM_WAKTU`), dan jam tiap waktu
  diambil dari nama yang sama (`adzan`, `semua`, `jam`) sehingga nama dan jamnya tak dapat
  tertukar. Beranda juga berhenti memetakan nama ke jam lewat rangkaian `when` yang menutup dengan
  `else -> isya` — cabang cadangan yang diam-diam menampilkan jam sholat lain lebih buruk daripada
  tak ada. Dijaga `JadwalHariIniTest` dan `AdzanJadwalTest`.
- **Aturan jadwal sholat kini tunggal, tidak lagi disalin ke tiga tempat**: `parseCorr`, pemetaan
  mazhab Asar ke faktor bayangan, dan pemilihan zona waktu dahulu identik di **tiga** berkas —
  `PrayerScheduler` (penjadwal pengingat), `JadwalViewModel` (menu Jadwal), dan `HomeViewModel`
  (beranda) — masing-masing menyalin ulang aturan yang sama. Tiga salinan berarti tiga kesempatan
  untuk berbeda: satu koma yang salah ketik, atau mazhab Asar yang diubah di layar tetapi tidak di
  penjadwal, membuat **pengingat berbunyi pada menit yang tidak sama dengan jam yang ditampilkan
  layar** — persis kegagalan yang paling sulit terlihat pengguna. Aturannya kini hidup di satu
  tempat, `JadwalHariIni` (murni, tanpa Android, sehingga dapat diuji langsung di JVM), dan keempat
  pemakainya — termasuk layar Adzan — menyusun `PrayerCalc.Params` lewat fungsi yang sama.
  Dijaga `JadwalHariIniTest` (termasuk kasus tepi: CSV cacat, koreksi melewati tengah malam) dan
  `AdzanJadwalTest` yang menolak salinan `parseCorr` maupun pemetaan mazhab Asar baru muncul lagi.
- **Layar Adzan kini menjawab "jam berapa adzan berbunyi"** (`plan.txt` §"Fitur Adzan" membuka
  daftar fitur wajibnya dengan *"Jadwal adzan untuk Subuh, Zuhur, Asar, Magrib, dan Isya"*): layar
  yang mengatur kapan adzan berbunyi dahulu hanya menampilkan **saklar** per waktu beserta namanya,
  sehingga untuk mengetahui jamnya pengguna harus membuka menu lain. Kini setiap waktu menampilkan
  jam adzan **dan** jam pengingatnya (bila diaktifkan), dihitung di perangkat dari lokasi, metode,
  mazhab Asar, koreksi menit, dan zona waktu yang **sama persis** dengan penjadwal pengingat.
- **Keterangan sumber tingkat modul kini dibaca, bukan disalin ke kode**: tiga berkas mencatat
  keterangan yang berlaku untuk seluruh isinya, dan ketiganya tidak pernah dibaca kode — jadi layar
  mengarang versi yang lebih pendek atau tidak menyebut asal-usulnya sama sekali. Layar Tahlil
  menulis *"susunan dapat berbeda menurut tradisi"* padahal `tahlil.json` memuat kalimat yang lebih
  lengkap (*"…; bukan satu-satunya versi yang benar"*); sebagian besar bagian tahlil tidak menyebut
  sumbernya sendiri sehingga `tahlil_full.json` menyediakan `sumber_umum` yang tak pernah tampil;
  dan layar Panduan Sholat menulis "(Fasolatan Kemenag)" sementara `sholat_daftar.json` mencatat
  keterangan yang lebih tepat, termasuk bahwa hadisnya diverifikasi lewat sunnah.com. Ketiganya kini
  dibaca dari berkasnya (dengan kalimat cadangan bila berkasnya kehilangan kuncinya) dan ditampilkan
  sebagai baris atribusi. Dijaga `AtribusiModulTest`.
- **Teks Surat Yasin kini menyebut asal teks Arab, transliterasi, dan terjemahannya**: berkas Qur'an
  di `assets/content` mencatat **tiga** sumber secara terpisah — `sumber_arab` (Mushaf Standar
  Indonesia LPMQ Kemenag), `sumber_latin` (transliterasi resmi Kemenag), dan `sumber_terjemahan`
  (terjemah edisi 2019) — tetapi `Surah` hanya membawa yang terakhir, dan **tidak satu pun**
  ditampilkan di layar: judul Yasin hanya menulis "(MSI Kemenag)" sebagai teks hardcode yang tak
  menyebut asal transliterasi maupun terjemahan, dan tidak ikut berubah bila bundelnya diperbarui.
  `Surah` kini membawa `sumberArab`/`sumberLatin`, dan baris atribusi disusun `AtribusiTeks` (murni,
  tanpa Android, sehingga dapat diuji langsung): hanya bagian yang benar-benar diklaim yang tampil,
  dan bila tidak ada keterangan sama sekali hasilnya kosong — bukan kalimat karangan. Dijaga
  `AtribusiTeksQuranTest`.
- **Lafaz adzan & iqomah kini menyebut sumbernya, dan urutannya bernomor**: `assets/content/adzan.json`
  sudah mencatat rujukan tiap bagian (`adzan.sumber` = *"HR Bukhari-Muslim — sunnah.com"*,
  `iqomah.sumber` = *"HR Abu Dawud, Tirmidzi — sunnah.com"*), tetapi `AdzanLafaz` tidak punya kolom
  sumber sehingga keterangan itu tak pernah dibaca kode — layar Adzan dan Iqomah menampilkan lafaz
  telanjang tanpa asal-usulnya, padahal permintaan tetapnya adalah *"pastikan semuanya berasal dari
  sumber resmi"*. Kini rujukan dibaca per-entri dengan cadangan keterangan tingkat bagian (tidak ada
  teks baru yang dikarang) dan ditampilkan di kartu tiap lafaz. Sekalian, tiap lafaz diberi nomor
  urut: sebelumnya judulnya sama-sama "Adzan"/"Iqomah" dan id kartunya memakai 10 huruf Arab pertama
  — yang **berulang** untuk lafaz yang sama (`اللهُ أَكْبَرُ`, `أَشْهَدُ أَنْ …`, `حَيَّ عَلَى …`), jadi
  dua lafaz berbeda berbagi satu id dan menandai satu lafaz sebagai favorit ikut menandai lafaz
  lainnya. Dijaga `AtribusiLafazAdzanTest`.
- **Susunan Tahlil kini menyebut sumbernya di layar** (`plan.txt` §"Fitur Tahlil Lengkap":
  *"Sediakan beberapa susunan Tahlil berdasarkan sumber atau tradisi yang jelas"*): layar Tahlil
  hanya mencetak **nama** susunan (*"Susunan tahlil lazim Indonesia; susunan dapat berbeda menurut
  tradisi"*), dan Pengaturan hanya *"Susunan aktif: …"*. Nama bukan sumber — berkas
  `assets/content/tahlil.json` sudah memuat keterangan asal-usulnya (`edisi` tingkat modul), tetapi
  keterangan itu **tidak pernah sampai ke layar mana pun**, sehingga pengguna tidak dapat memeriksa
  dari mana rangkaian itu berasal. `TahlilSusunan` kini membawa `sumber`, diisi dari `sumber`
  per-susunan bila ada dan jatuh ke `edisi` tingkat modul bila tidak (jadi tidak ada teks baru yang
  dikarang di bundel), lalu ditampilkan di modul Tahlil **dan** di Pengaturan. "Susunan saya" sengaja
  bersumber kosong dan layar mengatakannya terus terang — susunan buatan pengguna tidak boleh tampil
  seolah berasal dari rujukan atau tradisi tertentu. Dijaga `AtribusiSusunanTahlilTest` dan uji
  instrumented `OfflineContentTest.susunanDariBundelMenyebutSumbernya`.
- **Riwayat "terakhir dibaca" akhirnya terisi oleh bacaan yang benar-benar dibaca** (`plan.txt`
  §"Pencarian, Favorit, dan Riwayat": *"Sediakan favorit, riwayat terakhir dibaca, bookmark, dan
  pencarian terbaru"*): satu-satunya penulis riwayat adalah tombol **"Salin"** pada kartu bacaan
  (serta riwayat pencarian), sedangkan modul **Yasin** dan **Tahlil** — dua layar baca utama —
  **tidak pernah menulisnya sama sekali**. Daftar "Riwayat terbaru" karena itu memuat bacaan yang
  *disalin*, bukan yang *dibaca*, dan membaca Yasin dari ayat 1 sampai 83 tidak meninggalkan jejak
  apa pun. Kini `markRead` di Yasin dan perpindahan bagian di Tahlil (Berikutnya/Sebelumnya/lompat)
  mencatat posisinya. Pencatatan **tidak** ditempelkan pada `persist()` Tahlil maupun pada
  penghitung dzikir: menghitung tasbih juga memanggil keduanya, dan mencatat tiap ketukan akan
  membanjiri riwayat dengan satu bagian yang sama. Karena pengulangan kini jauh lebih mungkin,
  satu bacaan disaring agar hanya menempati satu baris — dan pengambilan riwayat dilakukan lebih
  longgar (60) sebelum disaring, sebab bila 20 baris terbaru semuanya bacaan yang sama,
  penyaringan setelah batas akan menyembunyikan bacaan lain yang sebenarnya ada.
- **Doa qunut terletak setelah i'tidal pada daftar langkah**: entri qunut berdiri di antara sholawat
  Ibrahimiyah dan salam — bertentangan dengan keterangan waktunya sendiri ("Rakaat kedua Subuh
  setelah i'tidal"), sehingga pembaca yang menekan "Berikutnya" sampai ke qunut justru setelah
  tasyahud. Daftar langkah dibaca berurutan, jadi letaknya kini mengikuti gerakannya.
- **Pengingat setelah waktu masuk tidak lagi membatalkan dirinya sendiri**: pekerja adzan, begitu
  berbunyi, menjadwalkan ulang dirinya untuk kemunculan berikutnya — dan penjadwalan ulang itu
  ikut mengantrekan pekerjaan susulan (pengingat doa, dan kini iqomah) dengan **nama unik yang
  sama**. Karena `ExistingWorkPolicy.REPLACE` membatalkan pekerjaan bernama sama yang masih
  menunggu, susulan hari itu yang tinggal beberapa menit lagi berbunyi ikut terhapus, sehingga
  **pengingat membaca doa setelah adzan tidak pernah terdengar sama sekali** sejak fitur itu ada.
  Pekerjaan susulan kini memakai kunci bertanggal (tanggal kemunculan di zona waktu pengguna),
  sehingga susulan hari ini tetap berjalan dan susulan besok memakai nama lain. Uji unit menolak
  setiap pekerjaan susulan yang kembali memakai nama tetap.
- **Layar Sunnah gagal dikompilasi**: baris pilihan pengelompokan mengoper `modifier` sebagai
  argumen posisi sekaligus bernama, sehingga build berhenti sebelum satu pun uji berjalan.

### Diubah
- **Beranda dipindahkan ke `HomeViewModel`** (`plan.txt` §"Teknologi dan Arsitektur" meminta
  *"ViewModel dan StateFlow"* dan *"state management yang terstruktur"*): beranda adalah satu-satunya
  layar inti tanpa ViewModel — belasan `remember { mutableStateOf(...) }` diisi sekali lewat satu
  `LaunchedEffect(Unit)`. Akibatnya nilai Store tidak reaktif (mengubah lokasi atau metode hisab di
  layar Jadwal baru terlihat setelah beranda dibuka ulang) dan seluruh keadaan hilang saat
  konfigurasi berubah (rotasi). Kini setiap preferensi dibaca `HomeViewModel` sebagai aliran dari
  `Store`, ringkasan favorit/riwayat mengikuti aliran Room, saklar pengingat menjadwalkan ulang
  pekerja latar dari ViewModel, dan jam beranda berdetak di sana — bukan membangun ulang layar tiap
  detik. Layar hanya menggambar `HomeUiState`. Dijaga `BerandaViewModelTest`, yang juga menolak
  kembalinya `mutableStateOf`/`rememberStore()` di layar beranda.
- **Layar Pengaturan kini memuat keempat preferensi bacaan** (`plan.txt` §"Fitur Surat Yasin":
  *"Pengaturan jarak antarbaris dan tema halaman"*, dan §"Pengaturan Aplikasi": *"Ukuran teks Arab
  dan Latin"*): layar Pengaturan menyediakan "Ukuran Arab" dan "Ukuran Latin", tetapi **tidak**
  "jarak antarbaris" maupun "tema halaman" — keduanya hanya ada di bottom sheet pengaturan bacaan.
  Jadi pengguna yang mencarinya di Pengaturan tidak menemukannya, meski keempatnya preferensi
  global yang sama-sama dipakai setiap kartu bacaan. Kini keduanya ada di Pengaturan, menulis kunci
  `Store.LINE_HEIGHT`/`Store.PAGE_THEME` yang **sama** dengan sheet (bukan salinan kedua) dan
  memakai rentang bersama `LINE_HEIGHT_RANGE` serta daftar `TemaHalaman.SEMUA`. Dijaga
  `PengaturanBacaanTest`.
- **"Ukuran Latin" dan "Jarak antarbaris" kini benar-benar global** (`plan.txt` §"Pengaturan
  Aplikasi" meminta pengaturan *"Ukuran teks Arab dan Latin"*, dan §"Pustaka Doa dan Dzikir"
  meminta setiap bacaan mendukung transliterasi): hanya layar Yasin yang membaca
  `Store.LATIN_SIZE`/`Store.LINE_HEIGHT`; seluruh layar bacaan lain — Dzikir & Doa, Panduan Sholat,
  detail sholat, Ghaib, Jenazah, Adzan, Iqomah, dan Tahlil — menuliskan angka tetap (`15f`/`16f`)
  pada panggilan `BacaanCard`. Jadi slider "Ukuran Latin" di Pengaturan hanya mengubah satu layar,
  dan label sheet "berlaku untuk semua bacaan" tidak benar. Kini `BacaanCard` membaca kedua
  preferensi itu sendiri (nilai yang dioper layar, bila ada, tetap menang), dan slider-nya
  ditulis langsung oleh `BacaanSettingsSheet` — sama seperti "tema halaman" — sehingga tersedia di
  semua layar bacaan tanpa dioper satu per satu. `YasinViewModel` tidak lagi menyimpan salinannya,
  supaya hanya ada satu sumber kebenaran. Dijaga `UkuranBacaanGlobalTest`, yang juga menolak angka
  tetap pada panggilan `BacaanCard` mana pun.
- **Lapisan audio dipisahkan dari penjadwalan dan dari UI** (`plan.txt` §"Teknologi dan Arsitektur":
  *"Terapkan arsitektur modular yang memisahkan UI, domain, data, penjadwalan, audio, lokasi, dan
  sensor"*): pemutaran adzan sebelumnya tinggal di `core/scheduling/AdzanPlayer.kt` — satu paket
  dengan penjadwalan yang mengubah *kapan* berbunyi — dan layar Adzan membuat `ExoPlayer` sendiri
  untuk pratinjaunya. Akibatnya perubahan lisensi berkas adzan menyentuh kode penjadwalan, dan
  lapisan UI memegang pemutar media. Kini ada paket **`core/audio`** berisi `AdzanPlayer`
  (pemutaran latar oleh pekerja pengingat) dan `AdzanPreview` (pratinjau dengan pelaporan galat
  yang sama); layar hanya memanggil play/stop/volume. `LapisanAudioTest` menolak `AdzanPlayer`
  kembali ke `core/scheduling` dan menolak pemakaian `androidx.media3` langsung di `feature/`.
- **Tombol "Buka" di Favorit & Riwayat kini membuka layar yang benar** (`plan.txt`: *"Pastikan setiap
  tombol memiliki fungsi nyata"*): pemetaan jenis konten ke rute ada di dalam layar Favorit dan
  ditutup `else -> HOME`, sehingga tombolnya **membuka layar yang salah tanpa memberi tahu siapa
  pun** — favorit dzikir membuka Beranda (karena `kind`-nya adalah id kelompok, `pagi_petang` dan
  sejenisnya, bukan `"dzikir"`), favorit doa adzan membuka Dzikir, favorit iqomah membuka layar
  Adzan, dan baris riwayat pencarian juga membuka Beranda. Pengguna yang melihat isinya "hilang"
  akan menandai ulang hal yang sama. Pemetaannya kini dikumpulkan di `RuteKonten` dan jenis yang
  memang tidak punya layar sendiri menghasilkan **keterangan**, bukan tombol yang menyesatkan; tiap
  tombol juga menyebut nama layar tujuannya. Id kelompok dzikir **dibaca dari bundel konten**, bukan
  ditulis tetap — daftar tetap akan kembali salah begitu bundel menambah kelompok baru. `RuteKontenTest`
  memeriksa setiap `kind` yang benar-benar ditulis aplikasi, membuktikan id kelompok berasal dari
  parameter (bukan daftar tetap), dan menjaga agar layar Favorit tidak lagi menyimpan fallback sendiri.
- **Keterangan cara menjawab adzan kembali utuh** (`plan.txt`: *"Jangan menghasilkan teks Arab
  secara acak"* dan §"Akurasi Konten Keagamaan"): `adzan.json` menulis kunci `catatan` **dua kali**
  pada entri "Menjawab adzan". JSON tidak melarangnya, tetapi setiap parser — termasuk `org.json`
  yang dipakai aplikasi — hanya menyimpan yang terakhir, sehingga penjelasan lengkapnya (menjawab
  setiap lafaz dengan lafaz yang sama, **kecuali** dua seruan *hayya* yang dijawab dengan lafaz
  lain) tertimpa versi pendek yang tertinggal. Yang dibaca pengguna adalah versi yang lebih sedikit
  menjelaskan, dan tak ada satu pun galat atau peringatan yang menandainya. Nilai yang benar kini
  dipertahankan, dan **kerusakan sunyi seperti ini ditolak oleh gate**: `tools/validate_content.py`
  (serta `tools/validate_bundle.py` atas teks mentah bundel) menolak kunci yang ditulis dua kali di
  objek mana pun, fixture `cacat.json` ikut memuat contohnya agar aturannya benar-benar dijalankan
  CI, dan `KontenTanpaKunciGandaTest` memindai teks mentah berkas konten karena jejaknya sudah
  hilang begitu JSON di-parse.
- **Preferensi susunan Tahlil kini ada di Pengaturan — dan tidak lagi berbohong** (`plan.txt`
  §"Pengaturan Aplikasi": *"Preferensi susunan Tahlil"*): Pengaturan dulu hanya mencetak satu baris
  tetap, *"Susunan Tahlil: lazim Indonesia (lihat modul Tahlil)"*, yang menyebut nama susunan tanpa
  pernah membacanya — sehingga tetap menyebut "lazim Indonesia" meski pengguna sudah menyusun
  "Susunan saya" sendiri. Kini Pengaturan **menampilkan susunan yang benar-benar aktif** dan,
  begitu ada lebih dari satu pilihan, menyediakan pemilihnya langsung di sana. Pilihannya memakai
  kunci preferensi yang sama dengan modul Tahlil (satu sumber kebenaran), jadi mengubahnya di mana
  pun berlaku di keduanya. Mengganti susunan **mengosongkan penanda posisi baca**: urutan bagiannya
  berbeda, sehingga posisi lama akan menunjuk bagian yang bukan bagian yang sedang dibaca pengguna.
  Saat hanya ada satu susunan di bundel konten, pemilihnya sengaja tidak ditawarkan — satu chip
  tunggal hanyalah hiasan — dan teksnya jujur menyebut bahwa susunan lain menunggu rujukan tradisi
  yang jelas, bukan disusun sendiri. Aturan "susunan mana yang aktif" (termasuk nilai tersimpan yang
  sudah tidak ada di bundel, yang harus jatuh ke susunan pertama alih-alih mengosongkan layar)
  dikumpulkan di `PreferensiSusunan` dan dipakai bersama oleh layar Pengaturan dan modul Tahlil.
- **Penjelasan metode hisab di layar Adzan kini benar** (`plan.txt`: *"Penjelasan metode
  perhitungan yang sedang digunakan"*): layar Adzan selalu menulis *"Metode jadwal: Kemenag/SIHAT"*
  dan menyuruh mencari koreksi serta zona waktu *di Pengaturan* — padahal metode, koreksi menit,
  dan zona waktu diatur di menu **Jadwal**, dan pengguna bebas memilih metode lain (MWL, Umm
  al-Qura, Egyptian, Makkah). Teks yang menyebut metode yang salah lebih buruk daripada tidak
  menyebut sama sekali, karena pengguna memercayai jadwal yang bukan jadwalnya. Kini nama metode,
  sudut Fajr, dan jeda Isya dibaca dari definisi metode yang sama dengan yang dipakai menghitung
  (`PrayerCalc.Method`), termasuk metode berjeda seperti Umm al-Qura, dan pengguna diarahkan ke
  layar yang benar-benar memuat pengaturan itu. Klaim yang sama di **notifikasi adzan** ("Jadwal
  dihitung di perangkat (metode Kemenag/SIHAT).") juga diperbaiki — notifikasi itu muncul justru
  saat pengguna paling mungkin memercayainya, dan kini menyebut metode yang benar-benar dipakai.
- **Dua gate cepat sebelum kompilasi**, keduanya menangkap galat yang tadinya baru ketahuan setelah
  satu siklus CI terbuang:
  - `tools/check_workflows.py` — GitHub tidak melaporkan berkas workflow yang salah parse sebagai
    kesalahan yang jelas: ia langsung menggagalkan run **tanpa membuat satu pun job**, sehingga
    pemeriksaan yang seharusnya menjaga PR hilang tanpa jejak di daftar check. Itu nyata terjadi:
    satu nama langkah yang memuat `": "` tanpa tanda kutip membuat seluruh `ci.yml` tidak sah dan
    PR tidak punya check sama sekali. Gate ini memvalidasi setiap berkas di `.github/workflows/`
    (YAML terbaca, ada `jobs`, tiap job punya `runs-on` + `steps`) dan menyebut baris/kolom
    galatnya; ia dijalankan workflow **terpisah** (`workflow-lint.yml`) karena `ci.yml` yang rusak
    tidak bisa memberi tahu siapa pun, dan sekali lagi dari job `lint` agar tetap terjaga bila
    berkas terpisah itu sendiri hilang.
  - `tools/check_kotlin_lex.py` — blok komentar Kotlin berakhir pada `*/` **pertama**, jadi
    menuliskan `*/` di dalam komentar (mis. saat menyebut pola MIME `` `*/*` ``) menutup komentar
    lebih awal dan sisa teksnya menjadi kode; itu pun nyata terjadi dan menggagalkan kompilasi
    seluruh modul. Gate ini menolak baris ber-`*/` ganda, `/*` ganda, `*/` tanpa pembuka, dan
    komentar yang tidak tertutup — tanpa perlu membangun proyek.
- **Pengaturan tampilan bacaan kini satu bottom sheet** (`plan.txt`: *"Gunakan bottom sheet untuk
  pengaturan singkat"*): chip Arab/Latin/Terjemahan dan slider ukuran yang tadinya berserakan di
  badan sepuluh layar bacaan (Yasin, Tahlil, Dzikir, Adzan, Iqomah, Doa Adzan, Sholat, Tata Cara,
  Ghaib, Jenazah) dipindahkan ke `BacaanSettingsSheet` yang dibuka dari satu tombol. Rentang ukuran
  yang sah (`ARAB_SIZE_RANGE`, `LATIN_SIZE_RANGE`, `LINE_HEIGHT_RANGE`) kini ditulis sekali di
  komponen bersama dan dipakai juga oleh Pengaturan, sehingga keduanya tak bisa berbeda diam-diam.
  Layar yang tak punya bacaan Latin/terjemahan tidak menampilkan chip yang tak berefek.
- **Keadaan kosong/gagal/memuat yang tadinya diam** (`plan.txt`: *"Sediakan tampilan kosong, pesan
  kesalahan, dan tombol mencoba kembali"*, *"Berikan skeleton loading atau indikator proses saat
  memuat data"*). Tiga jalan buntu diperbaiki sekaligus:
  - **Jadwal** — tombol "Pakai lokasi GPS" dulu mengembalikan `false` tanpa penjelasan saat belum
    ada perbaikan lokasi; kini layar menampilkan sebabnya + tombol coba lagi, memuat preferensi
    ditampilkan sebagai kerangka (`SkeletonCard`), dan kegagalan membaca penyimpanan ditampilkan
    sebagai pesan kesalahan yang bisa diulang.
  - **Kiblat** — saat aliran lokasi berakhir tanpa satu pun perbaikan (mis. GPS perangkat mati),
    layar dulu membeku tanpa keterangan; kini ada indikator proses selama menunggu dan penjelasan
    setelah 20 detik tanpa perbaikan, keduanya dengan tombol coba lagi.
  - **Adzan** — berkas audio pilihan pengguna yang tak terbaca membuat pratinjau diam saja; kini
    sebabnya ditampilkan (`PlaybackException`) dengan tombol coba lagi.
- **Hitungan dzikir selalu tampil**, bukan hanya untuk bacaan yang diulang lebih dari satu kali —
  progresnya tersimpan di Room, jadi pengguna bisa melihat dan mengulang meski targetnya sekali.
- **Tombol dekoratif dihapus**: penanda jumlah ulangan di kartu bacaan dulu `AssistChip` dengan
  `onClick` kosong — tombol yang tampak bisa ditekan tetapi tak melakukan apa pun; kini teks biasa.
  Invariannya dijaga `UxStateTest` (komponen keadaan harus dipakai, layar bacaan wajib membuka
  bottom sheet-nya, dan `onClick = {}` hanya boleh pada kontrol yang memang `enabled = false`).
- **Bacaan kini dibedakan menurut sumbernya** (`plan.txt`: *"Bedakan bacaan yang bersumber dari
  Al-Qur'an, hadis, dzikir umum, dan tradisi lokal"*): setiap berkas konten ber-Arab menyatakan
  `kategori` — di tingkat berkas, atau per entri bila berkasnya bercampur (`sholat_bacaan.json`
  mayoritas hadis, tetapi lafaz niatnya tradisi; `tahlil_full.json` mayoritas dzikir, doa jenazah
  hadis, dan ayat Qur'annya quran). Layar menampilkan labelnya di kartu bacaan
  (`SumberKategoriLabel`), dan kunci yang tak dikenal **tidak** ditampilkan sama sekali agar tidak
  ada klaim yang belum diverifikasi. Nilai sah hanya `quran`, `hadis`, `dzikir`, `tradisi`;
  invariannya dijaga di dua sisi yang harus tetap sepakat — gate CI
  (`tools/validate_content.py`: berkas ber-Arab tanpa `kategori` = galat) dan uji
  `SumberKategoriTest` (membandingkan `SumberKategori.SEMUA` dengan konstanta `KATEGORI` di gate).
- **Plafon Dependabot diperbaiki**: batas versi kini diambil dari catatan rilis resmi, bukan
  tebakan. Rilis **Dagger/Hilt 2.59** adalah yang pertama mewajibkan AGP >= 9.0.0, jadi plafon
  dipasang di 2.59 (sebelumnya 2.60 — akibatnya PR dependabot mengusulkan Hilt 2.60.1 dan
  gagal dengan "only compatible with AGP version 9.0.0 or higher"). Keluarga `androidx.hilt:*`,
  `androidx.test*`, espresso, dan uiautomator (dibangun terhadap SDK 35) ikut diabaikan.
  **Toolchain** (AGP, Kotlin, KSP, `kotlinx-coroutines`, Compose BOM) juga dipagari: keempatnya
  hanya boleh diabaikan **sampai minor berikutnya**, sehingga bump patch tetap mengalir sementara
  bump minor yang mustahil hijau (PR #22 mengusulkan Kotlin 2.4 + KSP 2.3 + Hilt 2.59 sekaligus)
  tidak lagi dibuat. Alasan tiap plafon ditulis di komentar agar tidak perlu ditebak lagi, dan
  `DependabotConfigTest` menolak plafon basi, plafon di atas minor berikutnya, atau pola tanpa
  pasangan kunci versi di `libs.versions.toml`.
- **Provenans per-entri dzikir**: setiap entri di `dzikir.json` kini mencantumkan sumbernya sendiri
  — Istighfar (HR Muslim 591), tasbih/tahmid/takbir (HR Bukhari 843 & Muslim 597), Ayat Kursi
  (QS Al-Baqarah 255), doa masuk masjid (HR Muslim 713), doa keluar rumah (HR Abu Dawud 5095,
  sahih), doa sebelum makan (Muwatta Malik 49/34 — atsar), dan doa sesudah makan (HR Abu Dawud
  3850 & Ibn Majah 3283 — derajat **daif** dinyatakan terbuka). Sebelumnya sumber hanya ada di
  tingkat berkas (`sumber_url`), sehingga pengguna tak dapat membedakan ayat Qur'an, hadis sahih,
  dan hadis daif. Entri "Ayat Kursi + Al-Ikhlas/Al-Falaq/An-Nas" yang menyimpan prosa penunjuk di
  field `arab`/`latin` (`"(dibaca penuh dari mushaf)"`, `"— lihat rujukan file —"`) kini bersih:
  teksnya diambil sepenuhnya dari `rujukan`. Invarian baru dijaga gate konten
  (`tools/validate_content.py`: sumber per-entri + deteksi placeholder) dan uji
  `ContentSourceTest.everyDzikirItemDeclaresItsOwnSource`.
- **Teks Al-Qur'an kini 100% dari sumber resmi**: seluruh ayat (`yasin_full.json`,
  `surah_001/112/113/114.json`, `ayat_kursi.json`, serta ayat Qur'an di `tahlil_full.json`)
  memakai **Mushaf Standar Indonesia (MSI) LPMQ Kemenag** beserta **transliterasi Latin resmi
  Kemenag** dan terjemah Kemenag edisi 2019 — menggantikan teks Utsmani (Tanzil) dan
  transliterasi Inggris dari `api.quran.com` yang non-resmi. Pengambilan data dapat diaudit dan
  diulang lewat `tools/fetch_quran_kemenag.py` (alat pengembang, tidak dijalankan di CI);
  invarian sumber dijaga `ContentSourceTest`. Perbaikan sampingan: BOM tersembunyi di ayat
  pertama Al-Fatihah ikut hilang.
- **Semua teks Arab kini berharakat**: dua lafaz di `adzan.json` sebelumnya tertulis tanpa baris
  (`الصلاة خير من النوم` pada catatan adzan Subuh, dan `حي على الصلاة`/`حي على الفلاح` pada
  catatan menjawab adzan) sehingga sulit dibaca pemula; keduanya kini lengkap harakatnya
  (`الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ`). Prosa penjelas yang tadinya diselipkan ke field `arab`
  pada "Menjawab adzan" dipindahkan ke field `catatan`, dan layar "Doa sebelum & sesudah adzan"
  membaca lafaznya dari assets (`ContentRepo.menjawabAdzan()`) — tidak ada lagi teks Arab
  hardcoded di kode Kotlin. Invariant dijaga gate `tools/validate_content.py` (kata Arab tanpa
  harakat = galat) dan uji `ArabicTextTest`.
- **Kontrol tampilan bacaan kini benar-benar bekerja di semua layar**: preferensi
  "Arab / Latin / Terjemahan" sebelumnya hanya tersimpan tanpa mengubah apa pun di Yasin,
  Tahlil, Dzikir, Adzan, dan panduan sholat. Kini `BacaanCard` menerima `showArab` dan setiap
  layar bacaan (Yasin, Tahlil, Dzikir, Adzan, Iqomah, Doa adzan, Sholat Wajib/Sunnah, Tata cara
  sholat, Ghaib, Jenazah) menghormatinya — dengan chip **Arab** di masing-masing layar dan
  penjelasan cakupannya di Pengaturan. Ukuran Arab di layar Iqomah, Ghaib, dan Jenazah juga
  ikut tersimpan (sebelumnya pengatur A+/A− hanya mengubah nilai di memori).
- **Kontrol yang menyesatkan dibersihkan**: `DND_OVERRIDE` (kunci preferensi yang tak pernah
  dibaca) dihapus; tombol "Kartu info di beranda" di Pengaturan kini benar-benar mengubah
  beranda (reaktif lewat `Store.flow`); catatan mazhab iqomah (`catatan_mazhab` dari
  `adzan.json`) akhirnya ditampilkan; dan teks di layar "Doa sebelum & sesudah adzan" yang
  menyuruh mengaktifkan pengingat di menu Adzan (kontrol yang tidak ada) diganti penjelasan
  yang benar. Kartu "Menjawab muadzin" tidak lagi menaruh kalimat instruksi di dalam kolom
  teks Arab.
- **`e2e.yml`**: grup `concurrency` kini per-ref (`e2e-${{ github.ref }}`), bukan global
  `e2e-main`. Sebelumnya `workflow_dispatch` pada branch membatalkan run `main` yang sedang
  berjalan, sehingga verifikasi `main` bisa hilang tanpa disadari.
- **Gate konten diperketat**: `ci.yml` kini menjalankan `tools/validate_content.py --strict`
  pada **setiap PR** (sebelumnya hanya di rilis), sehingga entri tanpa sumber resmi tidak dapat
  masuk ke `main`. Pemeriksaan dependensi dilakukan Dependabot (Gradle + GitHub Actions), jadi
  tidak ada job CI tambahan yang perlu dijalankan berulang.
- **`e2e.yml`**: APK uji dibangun **sebelum** emulator menyala (kompilasi Gradle yang berjalan
  bersamaan dengan emulator di runner 2-vCPU membuat emulator tak responsif →
  "Unknown API Level" → 0 perangkat, dan seluruh uji dilewati). Emulator diberi `-memory 4096`,
  dan uji diulang **sekali** hanya bila log memuat tanda-tangan kegagalan infrastruktur —
  uji yang benar-benar gagal tetap langsung dinyatakan gagal. Logikanya diletakkan di
  `.github/scripts/e2e-instrumented.sh` karena aksi emulator menjalankan setiap baris `script`
  sebagai `sh -c` terpisah, sehingga loop/fungsi multi-baris tidak akan utuh. Setelah uji hijau,
  buffer crash/ANR diperiksa agar proses yang sempat mati tidak lolos diam-diam.
- **Kejujuran izin**: izin `SCHEDULE_EXACT_ALARM` dihapus dari manifes karena pengingat memakai
  WorkManager dan tidak ada pemakaian `AlarmManager` sama sekali; tombol "Alarm tepat waktu"
  (yang membuka pengaturan izin tak terpakai) ikut dihapus, dan teks di menu Adzan kini
  menjelaskan bahwa waktu pengingat dapat bergeser saat perangkat masuk Doze. Invarian
  "izin alarm tepat waktu hanya bila `AlarmManager` dipakai" dijaga uji.
- **Pengaturan bahasa**: baris "Bahasa aplikasi" kini menjelaskan bahwa antarmuka hanya
  tersedia dalam bahasa Indonesia (sesuai `plan.txt`), alih-alih menyimpan kunci `lang`
  yang tidak pernah dibaca kode mana pun.
- **Perhitungan jadwal** memakai algoritma Meeus (deklinasi matahari + *equation of time*)
  dan terverifikasi terhadap acuan SIHAT Bimas Islam.
- Beranda menampilkan hitung mundur sholat berikutnya secara *live*.
- Favorit, bookmark, riwayat, dan progres dipindahkan dari DataStore ke Room; DataStore
  tetap dipakai untuk preferensi.
- **Aksesibilitas warna**: tema gelap memakai varian terang untuk `primary`/`secondary`
  dengan teks gelap di atas tombol, dan warna teks Latin (emas) digelapkan — semuanya agar
  lolos ambang kontras WCAG AA. Ditambah uji kontras otomatis (`ContrastTest`).
- **Sholat Ghaib & Jenazah**: niat dan doa mayit dipindahkan dari kode Kotlin ke assets
  (`sholat_ghaib.json`, `sholat_jenazah.json`, `sholat_mayit.json`) agar tervalidasi gate
  konten; tidak ada lagi teks Arab hardcoded di layar.

### Dihapus
- Kunci preferensi `Store.SOURCE` beserta dua penulis DataStore lama `Store.toggleFav` dan
  `Store.pushHist` yang tidak dipakai apa pun. `SOURCE` — sisa fitur "sumber online" yang tak pernah
  diaktifkan — ikut dideklarasikan, ikut disebut `importJson`, dan ikut dipulihkan dari backup
  padahal tak satu layar pun membacanya; kunci seperti itu lebih sunyi daripada izin yang tak
  dipakai, karena izin masih terlihat di peninjau toko aplikasi sedangkan kunci preferensi tidak
  terlihat siapa pun.
- Dependensi uji instrumentasi yang **tidak dipakai** — `espresso-core`, `uiautomator`, dan
  `kotlinx-coroutines-test` — beserta plafon Dependabot untuk dua yang terakhir. Ketiganya
  dideklarasikan di `androidTestImplementation` padahal tidak satu pun kelasnya diimpor berkas di
  `src/androidTest` (uji UI memakai `compose-ui-test-junit4`; uji yang ada memakai `runBlocking`,
  bukan `runTest`). `espresso-core` dan `kotlinx-coroutines-test` tetap ada di classpath karena
  `androidx.compose.ui:ui-test-junit4` menariknya sendiri — diperiksa pada POM-nya — sedangkan
  `uiautomator` tidak dipakai apa pun, sehingga deklarasinya hanya memperbesar APK uji dan
  memperlambat `e2e.yml` tanpa menambah satu pun kemampuan. Aturan barunya satu untuk semua:
  `DependensiUjiTerpakaiTest` menolak alias `libs.*` pada `androidTestImplementation` yang tidak
  diimpor uji instrumentasi, dan menolak alias baru yang belum punya padanan paket.
- Berkas `assets/content/yasin.json` yang yatim (metadata Yasin tak dibaca kode mana pun —
  aplikasi memakai `yasin_full.json`, yang juga sudah memuat metadata surat). Rujukan di
  `SOURCES.md` dan `docs/CONTENT_SOURCES.md` disesuaikan.
- Alur kerja `autofix.yml` versi lama yang hanya menjalankan `./gradlew tasks` (tanpa formatter
  nyata). Diganti alur kerja bernama sama yang menjalankan pemasang sesungguhnya
  (`tools/format_teks.py --fix`) — lihat bagian **Ditambahkan**.
- Izin `INTERNET` dan `ACCESS_NETWORK_STATE` dari manifes: tidak ada kode jaringan sama sekali,
  sehingga deklarasi izin kini jujur sejalan dengan sifat aplikasi yang 100% offline.

### Catatan
- Sumber jadwal/kalender **online resmi** belum diaktifkan karena SIHAT Bimas Islam tidak
  menyediakan API publik untuk aplikasi pihak ketiga; perhitungan lokal tetap akurat dan
  bekerja tanpa internet.

## [1.0.0] - 2026-10-10
- Rilis awal: Yasin, Tahlil, panduan sholat, dzikir & doa, kiblat, jadwal sholat, adzan,
  pencarian, favorit & riwayat.
