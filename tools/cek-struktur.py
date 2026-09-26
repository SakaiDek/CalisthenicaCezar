"""
Cek struktur cepat SEBELUM build, untuk menangkap kesalahan yang cuma bisa
dilihat compiler: kurung tidak seimbang, import Compose ketinggalan, dan
R.string yang dipakai di Kotlin tapi belum didaftarkan di strings.xml.

Ini BUKAN pengganti build -- dia tidak mengerti tipe, tidak mengerti Compose.
Gunanya cuma satu: memotong siklus "build 3 menit lalu gagal karena satu import".

Jalankan dari mana saja:  python3 tools/cek-struktur.py
Keluar dengan kode 1 kalau ada masalah, 0 kalau bersih.
"""
import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app/src/main/java/com/cezar/calisthenica")
STRINGS = os.path.join(ROOT, "app/src/main/res/values/strings.xml")

problems = []

def strip_code(s):
    out = []
    i, n = 0, len(s)
    while i < n:
        c = s[i]
        if c == '/' and i + 1 < n and s[i+1] == '/':
            while i < n and s[i] != '\n':
                i += 1
            continue
        if c == '/' and i + 1 < n and s[i+1] == '*':
            depth = 1
            i += 2
            while i < n and depth:
                if s.startswith('/*', i):
                    depth += 1; i += 2
                elif s.startswith('*/', i):
                    depth -= 1; i += 2
                else:
                    i += 1
            continue
        if s.startswith('"""', i):
            i += 3
            while i < n and not s.startswith('"""', i):
                i += 1
            i += 3
            continue
        if c == '"':
            i += 1
            while i < n and s[i] != '"':
                if s[i] == '\\':
                    i += 1
                i += 1
            i += 1
            continue
        if c == "'":
            i += 1
            while i < n and s[i] != "'":
                if s[i] == '\\':
                    i += 1
                i += 1
            i += 1
            continue
        out.append(c)
        i += 1
    return ''.join(out)

REQ = {
    "stringResource": "androidx.compose.ui.res.stringResource",
    "rememberCoroutineScope": "androidx.compose.runtime.rememberCoroutineScope",
    "mutableStateOf": "androidx.compose.runtime.mutableStateOf",
    "collectAsState": "androidx.compose.runtime.collectAsState",
    "rememberLauncherForActivityResult": "androidx.activity.compose.rememberLauncherForActivityResult",
    "BackHandler": "androidx.activity.compose.BackHandler",
    "LocalContext": "androidx.compose.ui.platform.LocalContext",
    "LocalHapticFeedback": "androidx.compose.ui.platform.LocalHapticFeedback",
    "HapticFeedbackType": "androidx.compose.ui.hapticfeedback.HapticFeedbackType",
    "ContentScale": "androidx.compose.ui.layout.ContentScale",
    "TextOverflow": "androidx.compose.ui.text.style.TextOverflow",
    "TextAlign": "androidx.compose.ui.text.style.TextAlign",
    "AsyncImage": "coil.compose.AsyncImage",
    "RoundedCornerShape": "androidx.compose.foundation.shape.RoundedCornerShape",
    "PickVisualMediaRequest": "androidx.activity.result.PickVisualMediaRequest",
    "ActivityResultContracts": "androidx.activity.result.contract.ActivityResultContracts",
    "KeyboardOptions": "androidx.compose.foundation.text.KeyboardOptions",
    # Ditambah 7 Sept 2026, bareng layar perakit program. `KeyboardOptions` sudah
    # dijaga sejak lama, tapi PASANGANNYA belum -- dan dua-duanya selalu dipakai
    # bersama (`KeyboardOptions(keyboardType = KeyboardType.Number)`). Satu dijaga
    # satu tidak itu penjaga yang setengah hati.
    "KeyboardType": "androidx.compose.ui.text.input.KeyboardType",
    # Dua fungsi item LazyColumn. Sengaja SEKARANG baru ditambahkan, dan cuma
    # `itemsIndexed`: token `items` telanjang juga muncul sebagai NAMA PARAMETER
    # (`items = programs`), jadi menjaganya bakal ramai lapor palsu. `itemsIndexed`
    # tidak punya masalah itu -- namanya cuma pernah dipakai sebagai fungsi.
    "itemsIndexed": "androidx.compose.foundation.lazy.itemsIndexed",
    "FlowRow": "androidx.compose.foundation.layout.FlowRow",
    "LazyColumn": "androidx.compose.foundation.lazy.LazyColumn",
    "PaddingValues": "androidx.compose.foundation.layout.PaddingValues",
    "Arrangement": "androidx.compose.foundation.layout.Arrangement",
    "Alignment": "androidx.compose.ui.Alignment",
    "Spacer": "androidx.compose.foundation.layout.Spacer",
    "Box": "androidx.compose.foundation.layout.Box",
    "Row": "androidx.compose.foundation.layout.Row",
    "Column": "androidx.compose.foundation.layout.Column",
    "Checkbox": "androidx.compose.material3.Checkbox",
    "CardDefaults": "androidx.compose.material3.CardDefaults",
    "ButtonDefaults": "androidx.compose.material3.ButtonDefaults",
    "TopAppBarDefaults": "androidx.compose.material3.TopAppBarDefaults",
    "HorizontalDivider": "androidx.compose.material3.HorizontalDivider",
    "OutlinedButton": "androidx.compose.material3.OutlinedButton",
    "OutlinedTextField": "androidx.compose.material3.OutlinedTextField",
    "TextButton": "androidx.compose.material3.TextButton",
    "AlertDialog": "androidx.compose.material3.AlertDialog",
    "FilterChip": "androidx.compose.material3.FilterChip",
    "IconButton": "androidx.compose.material3.IconButton",
    "ElevatedCard": "androidx.compose.material3.ElevatedCard",
    "ExtendedFloatingActionButton": "androidx.compose.material3.ExtendedFloatingActionButton",
    "Scaffold": "androidx.compose.material3.Scaffold",
    "TopAppBar": "androidx.compose.material3.TopAppBar",
    "MaterialTheme": "androidx.compose.material3.MaterialTheme",
    "Icon": "androidx.compose.material3.Icon",
    "Icons": "androidx.compose.material.icons.Icons",
    "launch": "kotlinx.coroutines.launch",
    "LaunchedEffect": "androidx.compose.runtime.LaunchedEffect",
    "Surface": "androidx.compose.material3.Surface",
    "rememberLazyListState": "androidx.compose.foundation.lazy.rememberLazyListState",
    "LazyListState": "androidx.compose.foundation.lazy.LazyListState",
    "Log": "android.util.Log",
    "Intent": "android.content.Intent",
    "ActivityNotFoundException": "android.content.ActivityNotFoundException",
    "Migration": "androidx.room.migration.Migration",
    "SupportSQLiteDatabase": "androidx.sqlite.db.SupportSQLiteDatabase",
    # Ditambah 5 Sept 2026, bareng mode immersive + ImageLoader Coil bersama.
    # Tiga token androidx.core.view di bawah ini gampang lupa di-import karena
    # namanya mirip satu sama lain; regexnya aman -- `WindowInsetsCompat\b`
    # TIDAK cocok di tengah `WindowInsetsControllerCompat` (tidak ada batas
    # kata antara "t" dan "C"), dan sebaliknya.
    "WindowCompat": "androidx.core.view.WindowCompat",
    "WindowInsetsCompat": "androidx.core.view.WindowInsetsCompat",
    "WindowInsetsControllerCompat": "androidx.core.view.WindowInsetsControllerCompat",
    "enableEdgeToEdge": "androidx.activity.enableEdgeToEdge",
    "SystemBarStyle": "androidx.activity.SystemBarStyle",
    "WindowInsets": "androidx.compose.foundation.layout.WindowInsets",
    "WindowInsetsSides": "androidx.compose.foundation.layout.WindowInsetsSides",
    "Application": "android.app.Application",
    "ImageLoader": "coil.ImageLoader",
    "ImageLoaderFactory": "coil.ImageLoaderFactory",
    "MemoryCache": "coil.memory.MemoryCache",
    # Ditambah 5 Sept 2026, bareng slider ketinggian alat (LayarKetinggian.kt).
    #
    # `Dp` dan `Button` sengaja ikut walau kelihatan terlalu umum: keduanya
    # SANGAT sering dipakai tanpa di-import karena ada di banyak file lain,
    # dan setiap file yang sudah memakainya hari ini sudah meng-import-nya
    # (buktinya: build hijau terakhir). Jadi token seumum ini cuma bisa
    # menangkap file BARU yang lupa -- persis kejadian yang mau dicegah.
    "Switch": "androidx.compose.material3.Switch",
    "Button": "androidx.compose.material3.Button",
    "BoxWithConstraints": "androidx.compose.foundation.layout.BoxWithConstraints",
    "detectVerticalDragGestures": "androidx.compose.foundation.gestures.detectVerticalDragGestures",
    "rememberUpdatedState": "androidx.compose.runtime.rememberUpdatedState",
    "LocalDensity": "androidx.compose.ui.platform.LocalDensity",
    "FontWeight": "androidx.compose.ui.text.font.FontWeight",
    "Dp": "androidx.compose.ui.unit.Dp",
    # -----------------------------------------------------------------------
    # Ditambah 8 Sept 2026, bareng Session Runner (ui/SessionRunnerScreen.kt).
    #
    # Sebelas token, dan tujuh di antaranya BARU PERTAMA KALI dipakai di app ini.
    # Token yang baru dipakai satu kali itu justru yang paling rawan: belum ada
    # file lain yang jadi contoh, jadi tidak ada yang mengingatkan kalau lupa.
    #
    # SOAL REGEX `Vibrator` vs `VibratorManager`. Pola penjaganya
    # `(?<![\w.])TOKEN\b`, jadi `Vibrator\b` TIDAK cocok di tengah
    # `VibratorManager` -- tidak ada batas kata antara "r" dan "M". Logika yang
    # sama sudah dibuktikan di blok `WindowInsetsCompat` di atas. Aman.
    #
    # SOAL TIGA TOKEN YANG "TERLALU UMUM" (`Build`, `Context`, `delay`). Ketiganya
    # sudah saya grep dulu ke 30 file yang ada SEBELUM ditambahkan, karena satu
    # lapor palsu bikin penjaga ini kehilangan wibawa dan mulai diabaikan:
    #   Build   -> MainActivity.kt, ui/theme/Theme.kt, SessionRunnerScreen.kt.
    #              Ketiganya sudah import android.os.Build. Ada satu lagi di
    #              KOMENTAR ExerciseFormScreen.kt ("Build-nya hijau") -- tidak
    #              masalah, `strip_code` membuang komentar sebelum diperiksa.
    #   Context -> AppDatabase.kt, MediaFiles.kt, SessionRunnerScreen.kt, semua
    #              sudah import. `withContext` TIDAK ikut kecocokan (huruf "h"
    #              sebelum "C" itu huruf, jadi lookbehind menolaknya), begitu
    #              juga `LocalContext` dan `context` huruf kecil.
    #   delay   -> cuma SessionRunnerScreen.kt.
    # -----------------------------------------------------------------------
    "rememberSaveable": "androidx.compose.runtime.saveable.rememberSaveable",
    "DisposableEffect": "androidx.compose.runtime.DisposableEffect",
    "LocalView": "androidx.compose.ui.platform.LocalView",
    "LinearProgressIndicator": "androidx.compose.material3.LinearProgressIndicator",
    "SystemClock": "android.os.SystemClock",
    "VibratorManager": "android.os.VibratorManager",
    "Vibrator": "android.os.Vibrator",
    "VibrationEffect": "android.os.VibrationEffect",
    "Build": "android.os.Build",
    "Context": "android.content.Context",
    "delay": "kotlinx.coroutines.delay",
    # -----------------------------------------------------------------------
    # Ditambah 25 Sept 2026, bareng tombol "perbesar foto" (ui/FotoLayarPenuh.kt).
    #
    # Lima token, semuanya BARU pertama kali dipakai di app ini, jadi belum ada
    # file contoh yang mengingatkan kalau lupa import -- persis kasus yang paling
    # rawan. Kelimanya sudah dicek aman dari lapor palsu:
    #   Dialog            -> pola `(?<![\w.])Dialog\b` TIDAK cocok di tengah
    #                        `AlertDialog` (ada huruf "t" sebelum "D") maupun
    #                        `DialogProperties` (tidak ada batas kata sebelum "P").
    #                        Jadi tiga layar yang memakai `AlertDialog` aman.
    #   DialogProperties  -> cuma FotoLayarPenuh.kt.
    #   CircleShape       -> cuma FotoLayarPenuh.kt; `RoundedCornerShape` beda token.
    #   materialIcon /    -> helper penggambar ikon dari material-icons-core,
    #   materialPath         cuma FotoLayarPenuh.kt. Ini yang bikin kita TIDAK perlu
    #                        menarik material-icons-extended demi satu ikon fullscreen.
    # -----------------------------------------------------------------------
    "Dialog": "androidx.compose.ui.window.Dialog",
    "DialogProperties": "androidx.compose.ui.window.DialogProperties",
    "CircleShape": "androidx.compose.foundation.shape.CircleShape",
    "materialIcon": "androidx.compose.material.icons.materialIcon",
    "materialPath": "androidx.compose.material.icons.materialPath",
    # -----------------------------------------------------------------------
    # Ditambah 25 Sept 2026, bareng rombakan layar Runner jadi IMERSIF
    # (foto jadi wallpaper penuh + scrim gradient + teks melayang).
    #
    # Tiga token, ketiganya BARU pertama kali dipakai di app ini dan cuma
    # muncul di SessionRunnerScreen.kt -- jadi belum ada file contoh yang
    # mengingatkan kalau lupa import, persis kasus paling rawan. Sudah dicek
    # (grep) ketiganya aman dari lapor palsu: tidak ada file lain yang menyebut
    # namanya, dan satu-satunya file yang memakai sudah meng-import ketiganya.
    #   Brush                   -> `Brush.verticalGradient(...)`, si scrim gelap.
    #   CompositionLocalProvider-> pembungkus yang memaksa LocalContentColor putih.
    #   LocalContentColor       -> warna default teks di atas foto.
    #
    # KENAPA `Color` SENGAJA TIDAK IKUT DIJAGA, walau baru dipakai banyak di sini:
    # MainActivity.kt memakai `Color.argb`/`Color.TRANSPARENT` dari
    # `android.graphics.Color` (Int), BUKAN `androidx.compose.ui.graphics.Color`.
    # Menjaga token `Color` akan menuduh MainActivity lupa import padahal tidak --
    # satu lapor palsu, dan penjaga ini kehilangan wibawa. Jadi `Color` di luar.
    # -----------------------------------------------------------------------
    "Brush": "androidx.compose.ui.graphics.Brush",
    "CompositionLocalProvider": "androidx.compose.runtime.CompositionLocalProvider",
    "LocalContentColor": "androidx.compose.material3.LocalContentColor",
    # -----------------------------------------------------------------------
    # Ditambah 25 Sept 2026, bareng revisi UI Runner: tombol intip pindah ke
    # kanan-bawah + foto latar bisa diketuk buka detail. Foto yang diketuk pakai
    # `MutableInteractionSource` supaya bisa `indication = null` (mematikan riak
    # selebar layar). Token BARU pertama kali dipakai di app ini dan cuma muncul
    # di SessionRunnerScreen.kt (sudah di-grep, tak ada file lain yang menyebut),
    # jadi belum ada file contoh yang mengingatkan kalau lupa import -- rawan.
    # -----------------------------------------------------------------------
    "MutableInteractionSource": "androidx.compose.foundation.interaction.MutableInteractionSource",
    # -----------------------------------------------------------------------
    # Ditambah 25 Sept 2026, bareng detail gerakan yang dirombak jadi PANEL
    # BAWAH (ModalBottomSheet) di dalam Session Runner -- ganti overlay penuh
    # yang dulu memuat ulang foto. Dua token, keduanya BARU pertama kali dipakai
    # di app ini dan cuma muncul di ExerciseDetailScreen.kt (fungsi
    # `PanelDetailGerakan`), jadi belum ada file contoh yang mengingatkan kalau
    # lupa import -- kasus paling rawan. Sudah dicek aman dari lapor palsu:
    #   ModalBottomSheet             -> tak ada token lain memuatnya sebagai
    #                                   substring dengan batas kata yang cocok.
    #   rememberModalBottomSheetState-> pola `remember\s*[({]` di penjaga bawah
    #                                   TIDAK cocok (setelah "remember" ada "M",
    #                                   bukan kurung), jadi tak salah tuduh.
    # Keduanya juga API eksperimental -> lihat cek-optin.py, sudah terdaftar di
    # ExperimentalMaterial3Api dan `PanelDetailGerakan` diberi @OptIn sendiri.
    # -----------------------------------------------------------------------
    "ModalBottomSheet": "androidx.compose.material3.ModalBottomSheet",
    "rememberModalBottomSheetState": "androidx.compose.material3.rememberModalBottomSheetState",
}
MOD = {
    "fillMaxWidth": "androidx.compose.foundation.layout.fillMaxWidth",
    "fillMaxSize": "androidx.compose.foundation.layout.fillMaxSize",
    "fillMaxHeight": "androidx.compose.foundation.layout.fillMaxHeight",
    "padding": "androidx.compose.foundation.layout.padding",
    "width": "androidx.compose.foundation.layout.width",
    "height": "androidx.compose.foundation.layout.height",
    "size": "androidx.compose.foundation.layout.size",
    "aspectRatio": "androidx.compose.foundation.layout.aspectRatio",
    "imePadding": "androidx.compose.foundation.layout.imePadding",
    "clip": "androidx.compose.ui.draw.clip",
    "background": "androidx.compose.foundation.background",
    "animateContentSize": "androidx.compose.animation.animateContentSize",
    "combinedClickable": "androidx.compose.foundation.combinedClickable",
    "clickable": "androidx.compose.foundation.clickable",
    "verticalScroll": "androidx.compose.foundation.verticalScroll",
    "rememberScrollState": "androidx.compose.foundation.rememberScrollState",
    # Ditambah 26 Sept 2026 (Fase 4 Session Runner). Dipakai sebagai
    # .heightIn(min = tinggiRuang) di isi tengah runner supaya kolom yang bisa
    # di-scroll tetap "center kalau muat, scroll kalau meluber". Lupa import =
    # error kompilasi, tapi tanpa entri ini penjaga tidak akan menangkapnya.
    "heightIn": "androidx.compose.foundation.layout.heightIn",
    # Ditambah 5 Sept 2026. Empat modifier inset ini yang dipakai supaya isi app
    # tidak ketimpa bilah sistem; salah satu ketinggalan = kesalahan yang cuma
    # muncul sebagai "kok jaraknya hilang", bukan sebagai pesan error.
    "navigationBarsPadding": "androidx.compose.foundation.layout.navigationBarsPadding",
    "windowInsetsPadding": "androidx.compose.foundation.layout.windowInsetsPadding",
    "asPaddingValues": "androidx.compose.foundation.layout.asPaddingValues",
    "only": "androidx.compose.foundation.layout.only",
    # Ditambah 5 Sept 2026, bareng slider ketinggian alat. Satu-satunya modifier
    # di app ini yang menerima blok sentuhan mentah; lupa import-nya berarti
    # tangga ketinggian tampil rapi tapi tidak bisa digeser sama sekali.
    "pointerInput": "androidx.compose.ui.input.pointer.pointerInput",
}

kt_files = []
for base, _, files in os.walk(SRC):
    for f in files:
        if f.endswith(".kt"):
            kt_files.append(os.path.join(base, f))

used_strings = set()
for path in sorted(kt_files):
    rel = os.path.relpath(path, ROOT)
    raw = open(path, encoding="utf-8").read()
    code = strip_code(raw)

    for oc, cc, label in (("{", "}", "kurawal"), ("(", ")", "kurung")):
        d = code.count(oc) - code.count(cc)
        if d != 0:
            problems.append("%s: %s tidak seimbang (%+d)" % (rel, label, d))

    imports = set(re.findall(r"^import\s+([\w.]+)", raw, re.M))
    local = set(re.findall(r"\b(?:fun|class|object|interface)\s+(\w+)", code))

    for tok, imp in REQ.items():
        if re.search(r"(?<![\w.])" + tok + r"\b", code) and imp not in imports and tok not in local:
            problems.append("%s: pakai `%s` tapi import `%s` tidak ada" % (rel, tok, imp))
    for tok, imp in MOD.items():
        if re.search(r"\.\s*" + tok + r"\s*\(", code) and imp not in imports and tok not in local:
            problems.append("%s: pakai `.%s()` tapi import `%s` tidak ada" % (rel, tok, imp))

    if re.search(r"\bby\s+(remember|\w*[Ff]low)", code) and "androidx.compose.runtime.getValue" not in imports:
        problems.append("%s: pakai delegasi `by` tapi import getValue tidak ada" % rel)
    if re.search(r"\bvar\s+\w+\s+by\b", code) and "androidx.compose.runtime.setValue" not in imports:
        problems.append("%s: pakai `var ... by` tapi import setValue tidak ada" % rel)
    if "@Composable" in code and "androidx.compose.runtime.Composable" not in imports:
        problems.append("%s: pakai @Composable tapi importnya tidak ada" % rel)
    if re.search(r"(?<![\w.])remember\s*[({]", code) and "androidx.compose.runtime.remember" not in imports:
        problems.append("%s: pakai remember tapi importnya tidak ada" % rel)
    if re.search(r"(?<![\w.])Modifier\b", code) and "androidx.compose.ui.Modifier" not in imports:
        problems.append("%s: pakai Modifier tapi importnya tidak ada" % rel)
    if re.search(r"\d\s*\.\s*dp\b", code) and "androidx.compose.ui.unit.dp" not in imports:
        problems.append("%s: pakai .dp tapi importnya tidak ada" % rel)
    if re.search(r"\d\s*\.\s*sp\b", code) and "androidx.compose.ui.unit.sp" not in imports:
        problems.append("%s: pakai .sp tapi importnya tidak ada" % rel)

    for name in set(re.findall(r"Icons\.Default\.(\w+)", code)):
        imp = "androidx.compose.material.icons.filled." + name
        if imp not in imports:
            problems.append("%s: pakai Icons.Default.%s tapi import `%s` tidak ada" % (rel, name, imp))
    for name in set(re.findall(r"Icons\.AutoMirrored\.Filled\.(\w+)", code)):
        imp = "androidx.compose.material.icons.automirrored.filled." + name
        if imp not in imports:
            problems.append("%s: pakai Icons.AutoMirrored.Filled.%s tapi import `%s` tidak ada" % (rel, name, imp))
    for dep in ("Icons.Default.ArrowBack", "Icons.Default.List", "Icons.Default.Send", "Icons.Default.ArrowForward"):
        if dep in code:
            problems.append("%s: %s deprecated, pakai Icons.AutoMirrored.Filled.*" % (rel, dep))

    for bad in ("SISA_FILE", "PLACEHOLDER_", "TODO()"):
        if bad in raw:
            problems.append("%s: masih ada penanda `%s`" % (rel, bad))

    if re.search(r"\bR\.(string|drawable|xml)\.", code) and "com.cezar.calisthenica.R" not in imports and "package com.cezar.calisthenica\n" not in raw:
        problems.append("%s: pakai R.* tapi import R tidak ada" % rel)

    used_strings |= set(re.findall(r"R\.string\.(\w+)", code))

xml = open(STRINGS, encoding="utf-8").read()
declared = set(re.findall(r'<string\s+name="([^"]+)"', xml))
for s in sorted(used_strings - declared):
    problems.append("strings.xml: R.string.%s dipakai di Kotlin tapi TIDAK ADA di strings.xml" % s)
unused = sorted(declared - used_strings)

# ---------------------------------------------------------------------------
# JUMLAH ARGUMEN stringResource vs jumlah penanda di strings.xml.
# Ditambahkan 7 September 2026, bareng layar perakit program.
#
# KENAPA INI PENTING, dan kenapa penjaga lain tidak bisa menangkapnya:
# `stringResource(R.string.foo, a, b)` itu SAH di mata compiler apa pun jumlah
# argumennya, karena tanda tangannya `vararg formatArgs: Any`. Yang menghitung
# penanda `%1$s` cuma Android saat layarnya digambar. Jadi salah hitung di sini
# = build HIJAU, lalu app MATI persis waktu layar itu dibuka. Kelas kesalahan
# paling jahat yang ada: lolos semua pemeriksaan, muncul di tangan user.
#
# Build ini sendiri menambahkan sepuluh string berpenanda sekaligus -- terbanyak
# sepanjang proyek. Itu alasan yang cukup.
#
# DUA JEBAKAN yang sudah menipu versi pertama skrip ini:
#   1. TRAILING COMMA. Gaya Kotlin di proyek ini menutup daftar argumen dengan
#      koma: `labelDurasi(x),`. Potongan setelah koma terakhir itu spasi kosong,
#      BUKAN argumen. Versi pertama melaporkan 12 kesalahan palsu gara-gara ini.
#   2. Komentar. Blok penjelasan di file ini sering MENYEBUT `stringResource`.
#      Jadi komentar dibuang dulu -- tapi string literal HARUS diselamatkan,
#      karena argumennya bisa berupa teks (`stringResource(R.string.x, "a")`).
#      Itu sebabnya di bawah ada pembuang komentar sendiri, bukan `strip_code`
#      yang juga melahap isi string.
#
# Sekalian dijaga penanda TELANJANG (`%s` tanpa nomor). Di layar berbahasa lain
# urutan kata bisa berbeda, dan penanda tanpa nomor tidak bisa ditukar posisinya.
# ---------------------------------------------------------------------------
def buang_komentar_saja(s):
    """Sama seperti strip_code, tapi dengan DUA beda penting:

    1. ISI STRING DIBIARKAN UTUH, karena argumen bisa berupa teks.
    2. NOMOR BARIS DIJAGA. Komentar tidak dihapus, tapi ditimpa spasi, dan
       setiap baris baru di dalamnya dipertahankan. Kalau komentar dibuang
       mentah-mentah, satu blok penjelasan 40 baris menggeser semua nomor baris
       sesudahnya -- dan pesan error yang menunjuk baris salah itu pesan yang
       menyuruhmu mencari di tempat yang keliru.
    """
    out = []
    i, n = 0, len(s)
    while i < n:
        if s[i] == '/' and i + 1 < n and s[i+1] == '/':
            while i < n and s[i] != '\n':
                out.append(' ')
                i += 1
            continue
        if s[i] == '/' and i + 1 < n and s[i+1] == '*':
            depth = 1
            out.append('  ')
            i += 2
            while i < n and depth:
                if s.startswith('/*', i):
                    depth += 1; out.append('  '); i += 2
                elif s.startswith('*/', i):
                    depth -= 1; out.append('  '); i += 2
                else:
                    out.append('\n' if s[i] == '\n' else ' ')
                    i += 1
            continue
        out.append(s[i])
        i += 1
    return ''.join(out)


def potong_argumen(teks, i):
    """i menunjuk ke '(' pembuka. Kembalikan daftar argumen tingkat atas.

    Kedalaman kurung dihitung sendiri supaya `labelDurasi(totalDetik)` dianggap
    SATU argumen, bukan dua. Kutip dilompati supaya koma di dalam teks tidak
    dihitung sebagai pemisah.
    """
    depth, mulai, args = 0, i + 1, []
    j = i
    while j < len(teks):
        c = teks[j]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
            if depth == 0:
                args.append(teks[mulai:j])
                return args
        elif c == "," and depth == 1:
            args.append(teks[mulai:j])
            mulai = j + 1
        elif c == '"':
            j += 1
            while j < len(teks) and teks[j] != '"':
                if teks[j] == "\\":
                    j += 1
                j += 1
        j += 1
    return args


# Komentar XML dibuang dulu: contoh penanda di dalam komentar bukan penanda asli.
xml_tanpa_komentar = re.sub(r"<!--.*?-->", "", xml, flags=re.S)
butuh_arg = {}
for m in re.finditer(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', xml_tanpa_komentar, re.S):
    isi = m.group(2)
    bernomor = [int(x) for x in re.findall(r"%(\d+)\$", isi)]
    telanjang = re.findall(r"%(?!\d+\$)(?!%)[sdf]", isi)
    butuh_arg[m.group(1)] = (max(bernomor) if bernomor else 0, len(telanjang))

for path in sorted(kt_files):
    rel = os.path.relpath(path, ROOT)
    teks = buang_komentar_saja(open(path, encoding="utf-8").read())
    for m in re.finditer(r"stringResource\s*\(", teks):
        args = [a for a in potong_argumen(teks, m.end() - 1) if a.strip()]
        if not args:
            continue
        kunci = re.search(r"R\.string\.(\w+)", args[0])
        if not kunci:
            continue
        nama = kunci.group(1)
        if nama not in butuh_arg:
            continue  # sudah dilaporkan pemeriksaan di atas
        perlu, telanjang = butuh_arg[nama]
        dikirim = len(args) - 1
        baris = teks[:m.start()].count("\n") + 1
        if telanjang:
            problems.append(
                "%s:%d: `%s` pakai penanda telanjang (%%s / %%d). Harus bernomor "
                "(%%1$s), kalau tidak urutannya tidak bisa ditukar di bahasa lain."
                % (rel, baris, nama)
            )
        if perlu != dikirim:
            problems.append(
                "%s:%d: `%s` butuh %d argumen, dikirim %d. Ini CRASH saat layarnya "
                "dibuka, bukan error build." % (rel, baris, nama, perlu, dikirim)
            )

# ---------------------------------------------------------------------------
# Pemeriksaan file XML. Ditambahkan 5 September 2026, setelah build gagal di
# `mergeDebugResources` gara-gara SATU komentar di strings.xml yang berisi dua
# tanda minus berurutan. Kotlin-nya sempurna, penjaga ini hijau, tapi APK-nya
# tidak pernah lahir.
#
# Aturan yang saya pegang: begitu satu kelas kesalahan berhasil lolos sampai ke
# build, dia jadi tugas skrip ini. Bukan tugas ingatan manusia.
#
# Dua hal yang diperiksa:
#   1. `--` di dalam komentar XML. Standar XML melarangnya, karena parser tidak
#      bisa memastikan apakah itu mau menutup komentar atau bukan.
#   2. Seluruh file diparse ulang pakai expat (parser XML asli Python). Ini
#      menangkap semua sisanya sekaligus: tag lupa ditutup, `&` telanjang,
#      kutip yang tidak berpasangan.
# ---------------------------------------------------------------------------
from xml.parsers import expat

xml_files = []
for dirpath, _dirs, files in os.walk(os.path.join(ROOT, "app/src/main/res")):
    for f in files:
        if f.endswith(".xml"):
            xml_files.append(os.path.join(dirpath, f))
manifest = os.path.join(ROOT, "app/src/main/AndroidManifest.xml")
if os.path.exists(manifest):
    xml_files.append(manifest)

for path in sorted(xml_files):
    relx = os.path.relpath(path, ROOT).replace("\\", "/")
    teks = open(path, encoding="utf-8").read()
    for m in re.finditer(r"<!--(.*?)-->", teks, re.S):
        if "--" in m.group(1):
            baris = teks[:m.start()].count("\n") + 1
            problems.append(
                "%s:%d: komentar XML mengandung `--`, dan XML melarang itu. "
                "Ganti pakai titik koma atau em dash." % (relx, baris)
            )
    try:
        expat.ParserCreate().Parse(teks.encode("utf-8"), True)
    except expat.ExpatError as e:
        problems.append(
            "%s:%d: XML tidak sah, %s" % (relx, e.lineno, expat.ErrorString(e.code))
        )

print("File .kt diperiksa :", len(kt_files))
print("File .xml diperiksa:", len(xml_files))
print("R.string dipakai   :", len(used_strings), "| terdaftar:", len(declared))
if unused:
    print("Terdaftar tapi belum dipakai (bukan error):", ", ".join(unused))
print()
if problems:
    print("=== MASALAH (%d) ===" % len(problems))
    for p in problems:
        print(" -", p)
    sys.exit(1)
print("=== STRUKTUR BERSIH ===")
