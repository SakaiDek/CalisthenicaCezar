"""
cek-optin.py -- penjaga API eksperimental Jetpack Compose.

KENAPA FILE INI ADA:
Build 5 Sept 2026 gagal dengan pesan "This foundation API is experimental".
cek-struktur.py lolos, kurung seimbang, import bersih -- tapi kode tetap tidak
mau di-compile. Sebabnya: Kotlin memperlakukan pemakaian API eksperimental
sebagai ERROR, bukan warning, sampai fungsi pemakainya diberi @OptIn.
Pengecekan struktur tidak akan pernah bisa melihat itu, jadi butuh alat sendiri.

CARA KERJA:
Potong file .kt per fungsi TOP-LEVEL (deklarasi `fun` di kolom 0). Untuk tiap
potongan, kumpulkan anotasi yang menempel di atas deklarasinya, lalu cek: kalau
di dalam badan fungsi ada simbol eksperimental, apakah anotasi @OptIn yang
sesuai ada di fungsi itu atau di header file (@file:OptIn)?

Jalankan: python3 tools/cek-optin.py
"""

import os
import re
import sys

# Simbol -> anotasi yang dibutuhkan, untuk versi yang dipin di proyek ini
# (compose foundation/ui 1.7.0, material3 1.3.0). Kalau nanti versi naik,
# beberapa simbol di sini bisa jadi stabil -- hapus barisnya saat itu terjadi.
WAJIB_OPTIN = {
    "ExperimentalFoundationApi": ["combinedClickable", "stickyHeader", "BasicTooltipBox"],
    "ExperimentalMaterial3Api": [
        "TopAppBar", "CenterAlignedTopAppBar", "MediumTopAppBar", "LargeTopAppBar",
        "TopAppBarDefaults", "ModalBottomSheet", "rememberModalBottomSheetState",
        "SearchBar", "DatePicker", "TimePicker", "TooltipBox", "SegmentedButton",
    ],
    "ExperimentalLayoutApi": ["FlowRow", "FlowColumn", "ContextualFlowRow"],
}

AKAR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SUMBER = os.path.join(AKAR, "app", "src", "main", "java")

RE_FUN = re.compile(r"^(?:@\w+.*)?(?:public |private |internal |)(?:inline )?fun\s+\w+")
RE_DEKLARASI = re.compile(r"^(?:public |private |internal |)(?:inline )?fun\s+(\w+)")
RE_ANOTASI = re.compile(r"^@")
RE_BLOK_KOMENTAR = re.compile(r"/\*.*?\*/", re.S)
RE_BARIS_KOMENTAR = re.compile(r"//.*")


def buang_komentar(teks):
    """JEBAKAN YANG PERNAH MENIPU ALAT INI: komentar dokumentasi sering MENYEBUT
    nama simbol eksperimental untuk menjelaskannya. Kalau komentar tidak dibuang,
    penjelasan bisa dihitung sebagai pemakaian."""
    return RE_BARIS_KOMENTAR.sub("", RE_BLOK_KOMENTAR.sub("", teks))


def potong_per_fungsi(baris):
    """Kembalikan daftar (nama, awal_anotasi, akhir) untuk fungsi top-level."""
    batas = []
    for i, b in enumerate(baris):
        m = RE_DEKLARASI.match(b)
        if not m:
            continue
        # naik ke atas selama masih anotasi menempel (tanpa baris kosong)
        awal = i
        j = i - 1
        while j >= 0 and RE_ANOTASI.match(baris[j]):
            awal = j
            j -= 1
        batas.append([m.group(1), awal, i])
    hasil = []
    for k, (nama, awal, decl) in enumerate(batas):
        akhir = batas[k + 1][1] if k + 1 < len(batas) else len(baris)
        hasil.append((nama, awal, decl, akhir))
    return hasil


def periksa(path):
    masalah = []
    with open(path, "r", encoding="utf-8") as f:
        isi = f.read()
    baris = isi.split("\n")
    # HANYA baris @file:OptIn yang dihitung sebagai izin tingkat file.
    # JEBAKAN PERTAMA YANG BIKIN ALAT INI DIAM: dulu di sini dicek 15 baris
    # pertama apa adanya -- padahal baris `import ...ExperimentalFoundationApi`
    # juga mengandung teks itu, jadi SEMUA file dianggap sudah punya izin.
    header = "\n".join(b for b in baris if b.startswith("@file:"))

    for nama, awal, decl, akhir in potong_per_fungsi(baris):
        anotasi = "\n".join(baris[awal:decl])
        badan = buang_komentar("\n".join(baris[decl:akhir]))
        for anot, simbols in WAJIB_OPTIN.items():
            for s in simbols:
                if not re.search(r"\b" + s + r"\s*\(", badan):
                    continue
                punya = anot in anotasi or anot in header
                if not punya:
                    nomor = decl + 1
                    masalah.append(
                        "%s:%d  fun %s() memakai %s tapi tidak ada "
                        "@OptIn(%s::class)" % (os.path.basename(path), nomor, nama, s, anot)
                    )
    return masalah


def main():
    if not os.path.isdir(SUMBER):
        print("TIDAK KETEMU folder sumber: %s" % SUMBER)
        return 2
    semua = []
    jumlah_file = 0
    for akar, _, files in os.walk(SUMBER):
        for f in sorted(files):
            if f.endswith(".kt"):
                jumlah_file += 1
                semua.extend(periksa(os.path.join(akar, f)))
    print("Diperiksa %d file .kt" % jumlah_file)
    if semua:
        print("\nADA %d PEMAKAIAN API EKSPERIMENTAL TANPA @OptIn:" % len(semua))
        for m in semua:
            print("  - %s" % m)
        print("\nIni akan jadi ERROR compile, bukan warning. Perbaiki dulu.")
        return 1
    print("OPT-IN BERSIH. Tidak ada API eksperimental yang belum ditandatangani.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
