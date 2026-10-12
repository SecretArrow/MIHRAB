# Changelog

Format mengikuti [Keep a Changelog](https://keepachangelog.com/id/1.1.0/).
Versi mengikuti [Semantic Versioning](https://semver.org/lang/id/).

## [Unreleased]

### Ditambahkan
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
  versi sebelumnya tetap terbaca lewat field `adzan` sebagai cadangan.
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

### Diperbaiki
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
- Berkas `assets/content/yasin.json` yang yatim (metadata Yasin tak dibaca kode mana pun —
  aplikasi memakai `yasin_full.json`, yang juga sudah memuat metadata surat). Rujukan di
  `SOURCES.md` dan `docs/CONTENT_SOURCES.md` disesuaikan.
- Alur kerja `autofix.yml` yang hanya menjalankan `./gradlew tasks` (tanpa formatter nyata);
  pemeriksaan tetap dilakukan oleh lint & unit test di `ci.yml`.
- Izin `INTERNET` dan `ACCESS_NETWORK_STATE` dari manifes: tidak ada kode jaringan sama sekali,
  sehingga deklarasi izin kini jujur sejalan dengan sifat aplikasi yang 100% offline.

### Catatan
- Sumber jadwal/kalender **online resmi** belum diaktifkan karena SIHAT Bimas Islam tidak
  menyediakan API publik untuk aplikasi pihak ketiga; perhitungan lokal tetap akurat dan
  bekerja tanpa internet.

## [1.0.0] - 2026-10-10
- Rilis awal: Yasin, Tahlil, panduan sholat, dzikir & doa, kiblat, jadwal sholat, adzan,
  pencarian, favorit & riwayat.
