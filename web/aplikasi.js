/* MIHRAB Web — Tahlil & Doa Arwah.
 *
 * Konten dibaca dari web/content/ — SALINAN dari bundel Android yang dibuat
 * tools/sinkron_web.py (sumber tunggal tetap bundel Android). Bagian yang menunjuk
 * rujukan_file (kartu surat pada Ringkas/Lengkap) diselesaikan di sini persis seperti
 * ContentRepo di Android — lihat bagian "Rujukan berkas" di bawah.
 * Keadaan pengguna (susunan terpilih, Mode Imam, Daftar Arwah) disimpan di
 * localStorage peramban — data pengguna, bukan teks bundel, sama seperti di Android.
 */
"use strict";

const KUNCI_SUSUNAN = "tahlil_susunan";
const KUNCI_IMAM = "tahlil_imam";
const KUNCI_ARWAH = "daftar_arwah";

/** Id kartu Buku Imam tempat fitur menempel — sama dengan aplikasi Android (bi_04, bi_20, bi_21). */
const ID_ARWAH_UTAMA = "bi_04";
const ID_ARWAH_DOA = ["bi_20", "bi_21"];

const keadaan = {
  susunanId: localStorage.getItem(KUNCI_SUSUNAN) || "nu_online_ringkas",
  modeImam: localStorage.getItem(KUNCI_IMAM) === "1",
  arwah: JSON.parse(localStorage.getItem(KUNCI_ARWAH) || "[]"),
  susunan: [],
  bagian: [],
  lencana: {},
  rujukan: {},
};

function simpan() {
  localStorage.setItem(KUNCI_SUSUNAN, keadaan.susunanId);
  localStorage.setItem(KUNCI_IMAM, keadaan.modeImam ? "1" : "0");
  localStorage.setItem(KUNCI_ARWAH, JSON.stringify(keadaan.arwah));
}

function el(tag, kelas, isi) {
  const e = document.createElement(tag);
  if (kelas) e.className = kelas;
  if (isi !== undefined) e.textContent = isi;
  return e;
}

/** Nama arwah dibersihkan seperti DaftarArwah di Android: rapikan spasi, batas jumlah. */
function bersihNama(s) {
  return s.replace(/\s+/g, " ").trim().slice(0, 60);
}
const MAKS_ENTRI = 30;

function tambahArwah(nama) {
  const n = bersihNama(nama);
  if (!n) return;
  if (keadaan.arwah.some((a) => a.toLowerCase() === n.toLowerCase())) return;
  keadaan.arwah = [...keadaan.arwah, n].slice(0, MAKS_ENTRI);
  simpan();
  render();
}

function hapusArwah(i) {
  keadaan.arwah = keadaan.arwah.filter((_, idx) => idx !== i);
  simpan();
  render();
}

function ringkasArwah(daftar, maks = 6) {
  const potong = daftar.slice(0, maks);
  const sisa = daftar.length - potong.length;
  return potong.join(" • ") + (sisa > 0 ? ` • … (+${sisa})` : "");
}

function kartuArwahUtama() {
  const kotak = el("div", "arwah-kotak");
  kotak.appendChild(el("h3", null, "Daftar arwah yang dituju"));
  kotak.appendChild(el("p", "ket",
    "Titik-titik pada bagian ini (\u201chadhrati ruhi\u2026\u201d) adalah tempat menyebut nama " +
    "arwah yang dituju — adatnya berbeda antar daerah. Nama tersimpan hanya di peramban Anda " +
    "dan lafaz bacaan tetap persis buku."));
  if (keadaan.arwah.length === 0) {
    kotak.appendChild(el("p", null, "Belum ada nama — tulis di bawah lalu ketuk Tambah."));
  }
  keadaan.arwah.forEach((n, i) => {
    const baris = el("div", "arwah-baris");
    baris.appendChild(el("span", null, `${i + 1}. ${n}`));
    const tombol = el("button", null, "Hapus");
    tombol.setAttribute("aria-label", `Hapus nama ${n}`);
    tombol.addEventListener("click", () => hapusArwah(i));
    baris.appendChild(tombol);
    kotak.appendChild(baris);
  });
  const baris = el("div", "arwah-tambah");
  const isian = el("input");
  isian.type = "text";
  isian.placeholder = "Nama arwah";
  isian.maxLength = 60;
  isian.setAttribute("aria-label", "Nama arwah");
  const tombol = el("button", null, "Tambah");
  tombol.disabled = true;
  isian.addEventListener("input", () => { tombol.disabled = isian.value.trim() === ""; });
  isian.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && isian.value.trim() !== "") tambahArwah(isian.value);
  });
  tombol.addEventListener("click", () => { tambahArwah(isian.value); });
  baris.appendChild(isian);
  baris.appendChild(tombol);
  kotak.appendChild(baris);
  return kotak;
}

function panelArwah() {
  const kotak = el("div", "arwah-panel");
  const baris = el("p", null, "Daftar arwah: ");
  baris.appendChild(el("span", "daftar", ringkasArwah(keadaan.arwah)));
  kotak.appendChild(baris);
  kotak.appendChild(el("p", "ket",
    "Lafaz doa tetap sesuai buku — sebutkan nama-nama ini pada waktunya, sesuai adat " +
    "majelis Anda."));
  return kotak;
}

function render() {
  const wadah = document.getElementById("daftar-bacaan");
  wadah.textContent = "";
  const items = itemsAktif();
  document.getElementById("jumlah-bagian").textContent =
    `${items.length} bagian` + (keadaan.modeImam ? " — Mode Imam (lencana: siapa yang membaca)" : "");

  items.forEach((it, i) => {
    const kartu = el("article");
    const atas = el("div", "kartu-atas");
    atas.appendChild(el("h2", null, `Bagian ${i + 1} — ${it.judul}`));
    if (it.ulangi > 1) atas.appendChild(el("span", "ulangi", `(${it.ulangi}x)`));
    if (keadaan.modeImam && keadaan.lencana[it.id]) {
      atas.appendChild(el("span", "lencana", keadaan.lencana[it.id]));
    }
    kartu.appendChild(atas);
    if (it.arab) kartu.appendChild(el("p", "arab", it.arab));
    if (it.latin) kartu.appendChild(el("p", "latin", it.latin));
    if (it.terjemahan) kartu.appendChild(el("p", "terjemahan", it.terjemahan));
    if (it.sumber) {
      const det = el("details", "sumber");
      det.appendChild(el("summary", null, "Sumber"));
      det.appendChild(el("p", null, it.sumber));
      kartu.appendChild(det);
    }
    // Daftar Arwah — data pengguna; lafaz di atas tidak pernah diubah untuk mengisinya.
    if (it.id === ID_ARWAH_UTAMA) kartu.appendChild(kartuArwahUtama());
    if (ID_ARWAH_DOA.includes(it.id) && keadaan.arwah.length > 0) {
      kartu.appendChild(panelArwah());
    }
    wadah.appendChild(kartu);
  });
}

function itemsAktif() {
  const s = keadaan.susunan.find((x) => x.id === keadaan.susunanId) || keadaan.susunan[0];
  if (!s) return [];
  const byId = new Map(keadaan.bagian.map((b) => [b.id, b]));
  return s.bagian.map((id) => byId.get(id)).filter(Boolean);
}

function renderChip() {
  const baris = document.getElementById("chip-susunan");
  baris.textContent = "";
  keadaan.susunan.forEach((s) => {
    const b = el("button", keadaan.susunanId === s.id ? "aktif" : "", s.nama);
    b.addEventListener("click", () => {
      keadaan.susunanId = s.id;
      keadaan.modeImam = false;
      simpan();
      renderChip();
      render();
    });
    baris.appendChild(b);
  });
  const barisImam = document.getElementById("chip-imam");
  barisImam.textContent = "";
  const imam = el("button",
    keadaan.modeImam ? "imam-aktif" : "", "Imam");
  imam.title = "Mode Imam — lencana siapa yang membaca tiap bagian";
  imam.addEventListener("click", () => {
    keadaan.modeImam = !keadaan.modeImam;
    if (keadaan.modeImam) {
      const buku = keadaan.susunan.find((s) => s.id === "buku_imam");
      if (buku) keadaan.susunanId = buku.id; // Mode Imam membaca susunan Buku Imam (sama dengan Android)
    }
    simpan();
    renderChip();
    render();
  });
  barisImam.appendChild(imam);
}

async function mulai() {
  try {
    const [tahlil, full, imam] = await Promise.all([
      fetch("content/tahlil.json").then((r) => r.json()),
      fetch("content/tahlil_full.json").then((r) => r.json()),
      fetch("content/tahlil_imam.json").then((r) => r.json()),
    ]);
    keadaan.susunan = tahlil.susunan || [];
    keadaan.bagian = full.bagian || [];
    await selesaikanRujukan();
    const aba = (imam && imam.aba) || {};
    keadaan.lencana = Object.fromEntries(
      Object.entries(aba).map(([id, a]) => [id, a.siapa || ""]),
    );
    renderChip();
    render();
  } catch (e) {
    document.getElementById("galat").hidden = false;
    console.error(e);
  }
}

/* ---------- Rujukan berkas (rujukan_file) ----------
 *
 * Kartu surat pada Ringkas/Lengkap tidak menyimpan teksnya — ia menunjuk berkas lain
 * (surah_001/112/113/114.json, ayat_kursi.json) lewat rujukan_file, sama seperti di
 * Android. Resolver di bawah meniru ContentRepo.resolveArabLatinTerjemah +
 * entriRujukan supaya yang dibaca web persis sama dengan yang dibaca aplikasi:
 *   - berkas "verses" (surat) dirangkai — Arab dengan koma Arab (U+060C) seperti
 *     mushaf dibaca bersambung, Latin & terjemahan dengan spasi;
 *   - akhiran "#id" menunjuk satu entri di dalam berkas (wadah "bagian"/"bacaan"/
 *     "kelompok[].items[]" — BentukKonten.DAFTAR_ENTRI);
 *   - teks inline pada bagian tetap menang di atas hasil rujukan;
 *   - rujukan mati dibiarkan kosong, bukan mengarang isi (gerbang sinkron_web.py
 *     sudah menolak rujukan mati sebelum halaman ini terbit).
 */

const WADAH_ENTRI = ["bagian", "bacaan"];

function cariEntri(dok, id) {
  for (const kunci of WADAH_ENTRI) {
    const daftar = dok[kunci];
    if (Array.isArray(daftar)) {
      const o = daftar.find((e) => e && e.id === id);
      if (o) return o;
    }
  }
  if (Array.isArray(dok.kelompok)) {
    for (const g of dok.kelompok) {
      const items = g && Array.isArray(g.items) ? g.items : [];
      const o = items.find((e) => e && e.id === id);
      if (o) return o;
    }
  }
  return null;
}

function teksRujukan(dok, id) {
  if (id) {
    const o = cariEntri(dok, id);
    return o ? { arab: o.arab || "", latin: o.latin || "", terjemahan: o.terjemahan || "" } : null;
  }
  if (Array.isArray(dok.verses)) {
    const rangkai = (kunci, pisah) => dok.verses.map((v) => v[kunci] || "").join(pisah);
    return {
      arab: rangkai("arab", "\u060C "),
      latin: rangkai("latin", " "),
      terjemahan: rangkai("terjemahan", " "),
    };
  }
  return { arab: dok.arab || "", latin: dok.latin || "", terjemahan: dok.terjemahan || "" };
}

async function selesaikanRujukan() {
  const nama = new Set();
  for (const b of keadaan.bagian) {
    for (const kunci of ["rujukan_file", "rujukanFile"]) {
      const nilai = typeof b[kunci] === "string" ? b[kunci] : "";
      const n = nilai.split("#", 1)[0].trim();
      if (n) nama.add(n);
    }
  }
  keadaan.rujukan = {};
  await Promise.all(
    [...nama].map(async (n) => {
      const r = await fetch(`content/${encodeURIComponent(n)}`);
      if (!r.ok) throw new Error(`Rujukan konten ${n} tidak dapat dibaca`);
      keadaan.rujukan[n] = await r.json();
    }),
  );
  for (const b of keadaan.bagian) {
    for (const kunci of ["rujukan_file", "rujukanFile"]) {
      if (typeof b[kunci] !== "string") continue;
      const potong = b[kunci].split("#");
      const namaBerkas = potong[0].trim();
      if (!namaBerkas) continue;
      const dok = keadaan.rujukan[namaBerkas];
      const t = dok ? teksRujukan(dok, (potong[1] || "").trim()) : null;
      if (!t) continue; // rujukan mati → tetap kosong, sama seperti Android
      if (!b.arab) b.arab = t.arab;
      if (!b.latin) b.latin = t.latin;
      if (!b.terjemahan) b.terjemahan = t.terjemahan;
      break;
    }
  }
}

mulai();
