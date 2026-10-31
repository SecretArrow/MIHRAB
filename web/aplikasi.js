/* MIHRAB Web — Tahlil & Doa Arwah.
 *
 * Konten dibaca dari web/content/ — SALINAN dari bundel Android yang dibuat
 * tools/sinkron_web.py (sumber tunggal tetap bundel Android).
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

mulai();
