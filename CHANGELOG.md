# Changelog

Format mengikuti [Keep a Changelog](https://keepachangelog.com/id/1.1.0/).
Versi mengikuti [Semantic Versioning](https://semver.org/lang/id/).

## [Unreleased]

### Ditambahkan
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

### Diubah
- **Berkas workflow kini diperiksa dapat dijalankan** (`tools/check_workflows.py`): GitHub tidak
  melaporkan berkas workflow yang salah parse sebagai kesalahan yang jelas — ia langsung
  menggagalkan run **tanpa membuat satu pun job**, sehingga pemeriksaan yang seharusnya menjaga PR
  hilang tanpa jejak di daftar check. Itu nyata terjadi: satu nama langkah yang memuat `": "` tanpa
  tanda kutip membuat seluruh `ci.yml` tidak sah dan PR tidak punya check sama sekali. Gate baru
  memvalidasi setiap berkas di `.github/workflows/` (YAML terbaca, ada `jobs`, tiap job punya
  `runs-on` + `steps`) dan menyebut baris/kolom galatnya. Ia dijalankan workflow **terpisah**
  (`workflow-lint.yml`) karena `ci.yml` yang rusak tidak bisa memberi tahu siapa pun, dan sekali lagi
  dari job `lint` di `ci.yml` agar tetap terjaga bila berkas terpisah itu sendiri hilang.
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
