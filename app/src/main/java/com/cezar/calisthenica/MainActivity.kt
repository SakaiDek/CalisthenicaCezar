package com.cezar.calisthenica

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cezar.calisthenica.data.AppDatabase
import com.cezar.calisthenica.model.WorkoutProgram
import com.cezar.calisthenica.ui.ExerciseCatalogScreen
import com.cezar.calisthenica.ui.KalenderDasborSection
import com.cezar.calisthenica.ui.NewProgramDialog
import com.cezar.calisthenica.ui.ProgramBuilderScreen
import com.cezar.calisthenica.ui.RiwayatScreen
import com.cezar.calisthenica.ui.SessionRunnerScreen
import com.cezar.calisthenica.ui.WorkoutCard
import com.cezar.calisthenica.ui.theme.CalisthenicaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * PELAJARAN PERTAMA HARI INI: "bilah hitam kaku" itu BUKAN warna app-mu.
         *
         * Kamu melaporkan area 3 tombol navigasi masih punya latar hitam kaku,
         * padahal `enableEdgeToEdge()` sudah dipanggil di baris ini sejak awal.
         * Saya tidak menebak -- saya mengukur pikselnya dari rekaman layarmu:
         *
         *   isi app  = #0b0a0c   (background gelap kita, benar)
         *   bilahnya = #0e1114   (lebih terang, dan agak biru. BUKAN punya kita)
         *
         * Kalau itu warna kita, dua angka itu harusnya sama persis. Bedanya itu
         * tanda tangan pihak ketiga: SISTEM yang menimpa bilah kita dengan
         * lapisan tipis semi-transparan. Namanya "navigation bar contrast
         * scrim", dan tugasnya melindungi tombol sistem supaya tetap kelihatan
         * di atas app yang isinya tidak dia kenal.
         *
         * Kenapa muncul padahal kita tidak meminta? Karena `enableEdgeToEdge()`
         * TANPA argumen memakai `SystemBarStyle.auto(...)`, dan mode auto itulah
         * yang menyalakan `isNavigationBarContrastEnforced = true` di Android 10+.
         * Jadi bilahnya sudah transparan sejak semalam; yang kamu lihat hitam itu
         * lapisan pelindung di atasnya.
         *
         * Dua baris di bawah mematikannya secara eksplisit. `Color.TRANSPARENT`
         * di sini itu `android.graphics.Color` (angka Int), BUKAN Color-nya
         * Compose -- gampang ketuker, dan kalau ketuker build-mu merah.
         *
         * Kenapa harus tahu gelap/terang sendiri: app ini punya tema terang ASLI
         * (lihat LightColors di Theme.kt). Kalau saya paksa `dark` terus-terusan,
         * ikon tombol sistem dibuat PUTIH -- dan di atas latar terang, tombol
         * Back-mu jadi putih di atas putih. Hilang. `light()` minta warna cadangan
         * karena Android 8.0 belum bisa membuat ikon navigasi jadi gelap, jadi di
         * HP setua itu dia butuh scrim samar supaya tetap terbaca.
         */
        val gelap = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val scrimJadul = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
        enableEdgeToEdge(
            statusBarStyle = if (gelap) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, scrimJadul)
            },
            navigationBarStyle = if (gelap) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, scrimJadul)
            },
        )

        // Sabuk pengaman kedua. `SystemBarStyle.dark/light` sudah mematikan
        // pemaksaan kontras, tapi sebagian custom ROM (dan HP-mu jalan custom ROM)
        // suka menyalakannya lagi. Baris ini menutup celah itu. Dijaga versi
        // karena propertinya baru lahir di Android 10 -- disentuh di Android 9
        // artinya crash NoSuchMethodError.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Bilah sistem disembunyikan. Penjelasan panjangnya ada di fungsi
        // `sembunyikanBilahSistem()` di bawah; dipanggil dari dua tempat karena
        // sekali saja tidak cukup.
        sembunyikanBilahSistem()

        setContent {
            CalisthenicaTheme {
                CalisthenicaApp()
            }
        }
    }

    /**
     * MODE IMMERSIVE: bilah status dan 3 tombol navigasi disembunyikan, muncul
     * sebentar kalau disapu dari tepi layar, lalu hilang lagi sendiri.
     *
     * ---------------------------------------------------------------------------
     * PELAJARAN HARI INI: ada TIGA cara "menyingkirkan" bilah sistem, dan cuma
     * satu yang kamu maksud. Bedanya wajib kamu pegang, karena dua yang lain akan
     * kelihatan mirip di layar tapi salah di tangan.
     *
     * 1. Edge-to-edge (yang kita pakai sejak semalam): bilahnya TETAP ADA, tapi
     *    transparan, dan isi app boleh menggambar di belakangnya. Cara resmi
     *    Android untuk app biasa.
     * 2. `hide()` + `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` (yang kamu minta, dan
     *    yang sekarang dipasang): bilahnya BENAR-BENAR PERGI, layar app jadi
     *    setinggi panel fisiknya. Sapu dari tepi = bilahnya melayang sebentar di
     *    ATAS isi app -- tanpa mendorong layout -- lalu menghilang sendiri.
     * 3. `hide()` + `BEHAVIOR_DEFAULT`: sekali disapu, bilahnya balik dan MENETAP.
     *    Ini yang bikin app terasa rusak setengah jalan, dan sengaja tidak dipakai.
     *
     * Kenapa `WindowCompat.getInsetsController(...)` dan bukan
     * `WindowInsetsControllerCompat(window, window.decorView)`: constructor itu
     * sudah deprecated. Fungsi ini mengembalikan objek yang sama, dan yang penting
     * dia MENGURUS perbedaan versi Android untukmu -- di API 30+ dia meneruskan ke
     * `WindowInsetsController` asli, di bawahnya dia menerjemahkan ke
     * `systemUiVisibility` gaya lama. Satu baris, jalan dari Android 8 sampai 16.
     *
     * URUTAN DUA BARIS DI DALAM SINI TIDAK BOLEH DIBALIK, dan ini jebakan halus:
     * `systemBarsBehavior` harus diset SEBELUM `hide()`. Kalau dibalik, penyembunyian
     * pertama masih memakai perilaku bawaan, dan sapuan pertamamu akan memunculkan
     * bilah yang menetap. Bug yang cuma terjadi sekali di awal, jadi paling
     * gampang lolos dari pengujian.
     *
     * KENAPA `systemBars()` DAN BUKAN CUMA `navigationBars()`: kamu minta "benar
     * benar FULL SCREEN seperti app fitness kompetitor", dan `systemBars()` =
     * status bar + navigation bar. Jam dan indikator baterai ikut hilang. Kalau
     * besok kamu ternyata mau jam tetap kelihatan, ganti satu kata di baris
     * `hide(...)` jadi `WindowInsetsCompat.Type.navigationBars()` -- tidak ada
     * bagian lain yang perlu disentuh.
     *
     * SATU AKIBAT YANG WAJIB KAMU TAHU SEBELUM MENCOBA (bukan tawaran, cuma
     * fakta): tombol Back sistem juga ikut hilang. Di dalam app ini itu aman --
     * setiap layar punya tombol X atau panah sendiri di kiri atas, dan
     * `BackHandler` tetap menangkap gestur/tombol back kalau bilahnya dipanggil.
     * Tapi kalau nanti ada layar baru TANPA tombol kembali di dalamnya, layar itu
     * jadi jalan buntu. Aturan baru untuk semua layar berikutnya: setiap layar
     * wajib punya jalan keluar yang kelihatan.
     * ---------------------------------------------------------------------------
     */
    private fun sembunyikanBilahSistem() {
        val pengendali = WindowCompat.getInsetsController(window, window.decorView)
        pengendali.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        pengendali.hide(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Dipanggil setiap kali jendela app ini mendapat atau kehilangan fokus.
     *
     * KENAPA INI WAJIB ADA, dan kenapa memanggil `hide()` sekali di `onCreate`
     * tidak cukup: penyembunyian bilah itu BUKAN pengaturan permanen, tapi
     * permintaan yang bisa dibatalkan sistem. Yang membatalkannya banyak dan
     * semuanya kejadian sehari-hari di app ini:
     *
     *   - Keyboard muncul (kamu mengetik nama alat) -- sebagian ROM memunculkan
     *     bilah navigasi bersama keyboard, lalu lupa menyembunyikannya lagi.
     *   - Photo Picker terbuka. Itu layar milik SISTEM, dengan aturan bilahnya
     *     sendiri; saat kamu balik ke app, bilahnya ikut terbawa.
     *   - Kamu pindah ke YouTube lalu kembali lewat recent apps.
     *   - Notifikasi ditarik turun lalu ditutup.
     *
     * `hasFocus` diperiksa dulu, dan jangan dihapus: memanggil `hide()` saat app
     * TIDAK sedang di depan artinya kita berkelahi dengan app yang sekarang
     * dipegang user. Yang benar cuma satu: rapikan rumah kita SENDIRI, tepat saat
     * kita dapat fokus kembali.
     *
     * Perhatikan juga ini bukan `LaunchedEffect` di dalam Compose. Fokus jendela
     * itu urusan Activity, satu tingkat di atas komposisi -- dan kejadiannya bisa
     * berlangsung saat tidak ada satu pun composable yang sedang aktif.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) sembunyikanBilahSistem()
    }
}

/**
 * Daftar layar yang ada. Enum biasa, sesederhana itu.
 *
 * Kenapa cukup enum: karena ketiga layar ini tidak perlu MEMBAWA data apa pun.
 * "Buka katalog" ya buka katalog, "buka riwayat" ya buka riwayat, titik.
 *
 * KOREKSI, 5 September 2026 -- baca ini, karena isinya pelajaran tentang cara
 * membaca kode, bukan cuma tentang navigasi.
 *
 * Di tempat ini dulu saya menulis: "Begitu nanti ada Layar Detail Gerakan yang
 * harus tahu gerakan nomor berapa yang dibuka, enum sudah tidak cukup -- di titik
 * itu kita naik ke Navigation Compose." Layar detailnya sekarang SUDAH ADA, dan
 * kalimat itu terbukti keliru. Membawa satu `Long` ke layar anak tidak butuh
 * library navigasi sama sekali; `var detailId: Long?` di dalam katalog sudah
 * cukup. Lihat sendiri di ExerciseCatalogScreen.kt.
 *
 * Navigation Compose baru benar-benar membayar ongkosnya kalau satu layar bisa
 * dimasuki dari BANYAK pintu, atau kalau kamu butuh deep link dari luar app, atau
 * butuh back stack yang selamat saat proses app dibunuh sistem. Belum satu pun
 * yang kita punya.
 *
 * Komentar yang salah TIDAK dibiarkan hidup di dalam kode. Komentar dibaca orang
 * sebagai janji; komentar basi bikin kamu -- atau saya, tiga bulan lagi --
 * menambah library yang tidak dibutuhkan cuma karena ada tulisan yang menyuruh.
 */
private enum class Screen { HOME, CATALOG, RIWAYAT }

/**
 * Pemegang keputusan "layar mana yang tampil" + tuan rumah Bilah Navigasi Bawah.
 *
 * PERUBAHAN BESAR (Ronde 2, 26 Sept 2026): navigasi antar-layar utama PINDAH dari
 * tombol pojok kanan atas ke Bilah Navigasi Bawah yang nempel di dasar layar --
 * lebih gampang dijangkau jempol, gaya app fitness papan atas. Tiga tab: Dasbor,
 * Katalog, Riwayat.
 *
 * KENAPA `jalankanProgram` & `rakitProgram` DIANGKAT KE SINI dari dalam HomeScreen:
 * dua sub-alur itu IMERSIF -- runner latihan dan perakit program mengambil SATU
 * layar penuh TANPA bilah nav di bawahnya. Kalau state-nya tetap di HomeScreen
 * (yang sekarang jadi isi Scaffold ber-bilah), bilah nav bakal ikut nongol di
 * bawah timer. Jadi dua sub-alur ini diperiksa PALING DULU: gambar layar penuh
 * lalu `return`, sebelum Scaffold bilah nav dibangun. Sesi menang atas perakit
 * (urutan sama seperti dulu): kalau dua-duanya kebetulan terisi, timer yang tampil.
 */
@Composable
private fun CalisthenicaApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var rakitProgram by remember { mutableStateOf<WorkoutProgram?>(null) }
    var jalankanProgram by remember { mutableStateOf<WorkoutProgram?>(null) }

    // Sub-alur imersif diperiksa sebelum apa pun digambar. `return` di sini AMAN
    // karena ketiga `remember` di atas SUDAH dieksekusi -- komposisi berbasis
    // posisi tidak kehilangan apa pun; yang dilewati cuma Scaffold di bawah.
    val sedangJalan = jalankanProgram
    if (sedangJalan != null) {
        SessionRunnerScreen(program = sedangJalan, onBack = { jalankanProgram = null })
        return
    }
    val sedangRakit = rakitProgram
    if (sedangRakit != null) {
        ProgramBuilderScreen(program = sedangRakit, onBack = { rakitProgram = null })
        return
    }

    // Tombol Back HP: di tab selain Dasbor, Back kembali ke Dasbor; di Dasbor, Back
    // MENGALAH supaya app bisa ditutup seperti biasa. Bilah nav jadi jalan utama,
    // tapi Back tetap dihormati supaya konsisten dengan kebiasaan Android.
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    Scaffold(
        bottomBar = { BilahNavigasiBawah(aktif = screen, onPilih = { screen = it }) },
        // Insets dinolkan DI SINI supaya tidak dobel: tiap layar anak (Dasbor,
        // Katalog, Riwayat) sudah mengurus inset-nya sendiri lewat Scaffold
        // masing-masing. Yang tetap kita ambil dari Scaffold luar cuma TINGGI bilah
        // nav-nya, yang otomatis sudah masuk ke `padding` di bawah ini.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        // Box + padding: isi tab digambar DI ATAS bilah nav, tak pernah ketutup.
        Box(modifier = Modifier.padding(padding)) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    onRakit = { program -> rakitProgram = program },
                    onMulai = { program -> jalankanProgram = program },
                )
                Screen.CATALOG -> ExerciseCatalogScreen(onBack = { screen = Screen.HOME })
                Screen.RIWAYAT -> RiwayatScreen(onBack = { screen = Screen.HOME })
            }
        }
    }
}

/**
 * Bilah Navigasi Bawah Material 3. Tiga tujuan utama, ikon dari material-icons-CORE
 * (aturan lama tetap berlaku: -extended DILARANG demi APK & waktu build ringan):
 *   - Dasbor  -> Home       (rumah = titik mula; di sinilah streak & kalender dipajang)
 *   - Katalog -> List       (daftar gerakan; ikon yang sama dengan tombol lama)
 *   - Riwayat -> DateRange  (riwayat memang dibaca per tanggal)
 *
 * `NavigationBar`/`NavigationBarItem` itu komponen M3 yang STABIL -- tidak butuh
 * `@OptIn` seperti TopAppBar. `selected` otomatis menyalakan sorotan (pil di balik
 * ikon + warna aktif), jadi kita tidak menggambar highlight manual. Label sengaja
 * dibiarkan selalu tampil (default `alwaysShowLabel = true`) sesuai permintaanmu.
 */
@Composable
private fun BilahNavigasiBawah(aktif: Screen, onPilih: (Screen) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = aktif == Screen.HOME,
            onClick = { onPilih(Screen.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(text = stringResource(R.string.nav_dasbor)) },
        )
        NavigationBarItem(
            selected = aktif == Screen.CATALOG,
            onClick = { onPilih(Screen.CATALOG) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(text = stringResource(R.string.nav_katalog)) },
        )
        NavigationBarItem(
            selected = aktif == Screen.RIWAYAT,
            onClick = { onPilih(Screen.RIWAYAT) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            label = { Text(text = stringResource(R.string.nav_riwayat)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRakit: (WorkoutProgram) -> Unit,
    onMulai: (WorkoutProgram) -> Unit,
) {
    val context = LocalContext.current

    // remember(context) = buka database SEKALI, bukan tiap kali layar
    // digambar ulang. Tanpa remember, setiap ketikan di dialog nanti bakal
    // memicu pembukaan ulang koneksi database. Itu boros dan bikin lag.
    val dao = remember(context) { AppDatabase.get(context).programDao() }
    val programsFlow = remember(dao) { dao.observeAll() }

    // DAO kedua, untuk tabel jembatan `program_exercises`. Satu database, dua
    // pintu -- `AppDatabase.get()` memang mengembalikan objek yang SAMA setiap
    // kali dipanggil (pola singleton di dalamnya), jadi ini tidak membuka
    // koneksi kedua.
    val peDao = remember(context) { AppDatabase.get(context).programExerciseDao() }

    /*
     * SATU query untuk hitungan SEMUA program, bukan satu per kartu.
     *
     * Hasilnya satu baris per (program, grup) -- maksimal tiga baris per program.
     * `groupBy` membaginya jadi Map supaya tiap kartu bisa mengambil bagiannya
     * dengan satu lookup, bukan menyapu ulang seluruh daftar.
     *
     * Kenapa penting: tanpa Map, setiap kartu harus `filter` seluruh daftar
     * ringkasan. Sepuluh kartu menyapu tiga puluh baris = tiga ratus
     * perbandingan, DI SETIAP FRAME saat kamu scroll. Dengan Map, sepuluh
     * lookup. Untuk lima program bedanya nol; kebiasaannya yang saya mau kamu
     * bawa ke fitur berikutnya.
     */
    val ringkasanFlow = remember(peDao) { peDao.observeRingkasan() }
    val ringkasanSemua by ringkasanFlow.collectAsState(initial = emptyList())
    val ringkasanPerProgram = remember(ringkasanSemua) { ringkasanSemua.groupBy { it.programId } }

    // Inilah sambungannya: keran (Flow) dari database disambung jadi State
    // Compose. Isi tabel berubah -> nilai `programs` berubah -> layar
    // menggambar ulang. Kita tidak pernah menyuruhnya refresh.
    val programs by programsFlow.collectAsState(initial = emptyList())

    // scope = "tempat menaruh pekerjaan yang butuh waktu", umurnya seumur
    // layar ini. Kalau layarnya hilang, pekerjaannya ikut dibatalkan.
    val scope = rememberCoroutineScope()
    var showNewDialog by remember { mutableStateOf(false) }

    // Program mana yang sedang dirakit. `null` = tidak ada, tampilkan dasbor.
    //
    // Ini SUB-LAPIS di dalam HomeScreen, bukan anggota baru enum `Screen`.
    // Alasannya: layar perakit selalu MILIK sebuah program, jadi dia butuh objek
    // programnya, dan enum tidak bisa membawa muatan. Menaruhnya di `Screen`
    // berarti saya harus menyimpan "program yang sedang dibuka" di tempat lain
    // lagi -- dua kotak memori untuk satu keadaan, yang suatu hari pasti tidak
    // sinkron.
    var rakitProgram by remember { mutableStateOf<WorkoutProgram?>(null) }

    // Program mana yang sedang DIJALANKAN. Sub-lapis kedua, alasannya sama
    // persis: sesi latihan selalu MILIK sebuah program.
    //
    // Kenapa dua kotak memori terpisah dan bukan satu variabel dengan penanda
    // "mode": karena dua-duanya tidak bisa hidup bersamaan, dan dengan dua
    // variabel `null` sudah menjadi penandanya sendiri. Satu variabel plus enum
    // mode berarti ada keadaan yang bisa ditulis tapi tidak masuk akal
    // (program = null tapi mode = JALAN). Bentuk data yang tidak bisa salah
    // selalu lebih baik daripada bentuk data yang dijaga supaya tidak salah --
    // aturan yang sama yang bikin kolom `target` cuma satu di tabel jembatan.
    var jalankanProgram by remember { mutableStateOf<WorkoutProgram?>(null) }

    /*
     * Sapu bersih baris yatim, SEKALI saat dasbor pertama tampil.
     *
     * `LaunchedEffect(Unit)` -- kuncinya `Unit`, artinya "jalan sekali seumur
     * layar ini, jangan diulang walau layarnya digambar ulang seribu kali".
     * Kalau kuncinya `programs`, dia akan jalan ulang setiap kali daftar program
     * berubah, yaitu setiap kali kamu menambah atau menghapus program.
     *
     * Yang dibersihkan: baris jembatan yang gerakannya sudah kamu hapus dari
     * katalog. `INNER JOIN` sudah membuatnya tidak tampil, tapi dia masih ikut
     * dihitung `observeRingkasan` -- jadi tanpa baris ini, kartu bisa bilang
     * "5 gerakan" sementara perakitnya cuma menampilkan 4. Angka yang tidak
     * cocok dengan isinya itu jenis bug yang bikin kamu curiga ke seluruh app.
     */
    LaunchedEffect(Unit) { peDao.hapusYatim() }

    // Tinggi bilah navigasi, ditanya langsung ke sistem -- BUKAN angka hafalan.
    // Di HP-mu (3 tombol) nilainya sekitar 48dp; di HP yang pakai gestur cuma
    // ~24dp; kalau HP-nya diputar landscape, bilahnya pindah ke samping dan
    // nilai ini jadi 0. Menulis "48.dp" di sini berarti app-mu benar cuma di
    // satu HP dengan satu setelan. Menanya seperti ini berarti selalu benar.
    val jarakBawah = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    /*
     * SESI LATIHAN, diperiksa PALING DULU di antara dua sub-lapis.
     *
     * Urutannya sengaja: sesi yang sedang berjalan itu keadaan paling "modal" di
     * app ini -- ada timer hidup dan layar yang dipaksa tidak tidur. Kalau suatu
     * hari ada jalan yang bikin kedua variabel terisi sekaligus (misalnya
     * pintasan baru dari layar lain), yang menang harus sesi, bukan perakit.
     *
     * Aturan yang sama seperti di bawah: `return` ini WAJIB setelah seluruh
     * `remember`, `collectAsState`, dan `LaunchedEffect`.
     */
    val sedangJalan = jalankanProgram
    if (sedangJalan != null) {
        SessionRunnerScreen(
            program = sedangJalan,
            onBack = { jalankanProgram = null },
        )
        return
    }

    /*
     * LAYAR PERAKIT, digambar sebagai `return` LEBIH AWAL.
     *
     * Pola yang sama dengan pemilih ketinggian alat, dan aturannya tetap mutlak:
     * `return` ini WAJIB duduk setelah SELURUH `remember`, `collectAsState`, dan
     * `LaunchedEffect` di atas. Komposisi Compose itu berdasarkan POSISI -- kalau
     * `return` menyalip satu `remember`, benda yang di-remember dianggap keluar
     * dari komposisi dan nilainya hilang. Efeknya: setiap kali kamu keluar dari
     * perakit, langganan database dasbor direset dan `hapusYatim()` jalan lagi.
     *
     * Kenapa aman soal tombol Back: `CalisthenicaApp` memasang
     * `BackHandler(enabled = screen != Screen.HOME)`, dan kita SEDANG di HOME --
     * jadi BackHandler induk itu sedang tidur. Tidak ada rebutan prioritas;
     * `ProgramBuilderScreen` bebas memasang BackHandler-nya sendiri.
     */
    val sedangRakit = rakitProgram
    if (sedangRakit != null) {
        ProgramBuilderScreen(
            program = sedangRakit,
            onBack = { rakitProgram = null },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home_title)) },
                actions = {
                    // Pintu ke Riwayat. Ikon DateRange (kalender) dipilih karena
                    // riwayat memang dibaca per tanggal, dan sengaja pakai ikon
                    // dari material-icons-CORE, bukan -extended: paket extended
                    // itu ribuan ikon yang menggelembungkan APK cuma demi satu
                    // gambar. Aturan lama yang tetap berlaku.
                    IconButton(onClick = onOpenRiwayat) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.riwayat_open),
                        )
                    }
                    // Pintu ke katalog. Ditaruh di TopAppBar, bukan jadi kartu
                    // di daftar program: katalog itu alat, bukan salah satu
                    // program latihanmu. Menaruhnya di daftar bakal bikin dua
                    // hal berbeda kelihatan sederajat.
                    IconButton(onClick = onOpenCatalog) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.home_open_catalog),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewDialog = true },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.home_add_program)) },
                // Satu-satunya hal di layar ini yang WAJIB dijauhkan dari bilah
                // navigasi: benda yang bisa dipencet. Isi daftar boleh lewat di
                // belakang bilah -- tombol tidak boleh, karena jempolmu bakal
                // kena tombol Home sistem, bukan tombol app kita.
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        // INI baris yang mengubah tampilan malam ini. Bawaannya, Scaffold
        // menyuntikkan tinggi bilah navigasi ke `innerPadding` -- hasilnya isi
        // layar BERHENTI di atas bilah, dan sisa ~48dp di bawah jadi pita kosong
        // berwarna background. Pita kosong itulah yang kamu baca sebagai
        // "latar hitam kaku": bukan warnanya yang salah, tapi memang tidak ada
        // apa-apa di situ.
        //
        // Dengan insets dinolkan, kaca dipakai penuh sampai ujung bawah, dan
        // kartu program benar-benar LEWAT di belakang tiga tombol navigasi saat
        // kamu scroll. Itu arti "edge-to-edge" yang sesungguhnya.
        //
        // Konsekuensinya harus kita bayar sendiri, dan cuma di dua tempat:
        // tombol (di atas) dan ujung daftar (di `contentPadding` bawah). Jangan
        // pernah menolkan insets tanpa membayar dua-duanya -- itu cara paling
        // cepat bikin isi layar tidak bisa dijangkau.
        //
        // Kenapa `.only(Horizontal)` dan bukan nol semua: kalau HP-mu diputar
        // landscape, tiga tombol navigasi itu PINDAH ke sisi kanan. Yang mau kita
        // buka cuma sisi bawah; sisi samping tetap harus dihormati, kalau tidak
        // nama gerakan bisa nongol persis di balik tombol Back. Alat yang sama
        // (`.only(...)`) juga dipakai di bottomBar wizard -- beda sisi, satu ide.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        if (programs.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                // bottom = 96dp (ruang FAB) + setinggi bilah navigasi.
                //
                // PELAJARAN: `contentPadding` di LazyColumn itu BUKAN margin. Dia
                // ruang di DALAM area scroll. Bedanya kelihatan pas kamu geser:
                // kartu paling bawah bisa naik melewati bilah navigasi (cantik,
                // immersive), tapi saat berhenti di dasar, kartu terakhir tetap
                // berhenti di atas bilah -- jadi tidak ada satu pun kartu yang
                // ketutup tombol sistem secara permanen.
                //
                // Kalau ini dipakai `.padding()` biasa, area scroll-nya sendiri
                // yang mengkerut: kartunya tidak akan pernah lewat di belakang
                // bilah, dan kita balik lagi ke pita kosong yang kamu keluhkan.
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 96.dp + jarakBawah,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = programs, key = { it.id }) { program ->
                    WorkoutCard(
                        program = program,
                        // Bagian ringkasan MILIK program ini saja. `?: emptyList()`
                        // itu keadaan yang paling sering terjadi, bukan kasus
                        // langka: program yang belum diisi TIDAK punya baris di
                        // hasil `GROUP BY` sama sekali.
                        ringkasan = ringkasanPerProgram[program.id] ?: emptyList(),
                        onRakit = { rakitProgram = program },
                        // Satu ketukan, satu variabel. Kartu tidak tahu apa itu
                        // timer, tidak tahu apa itu getar; dia cuma melapor
                        // "user mau menjalankan program ini". Komponen bodoh
                        // yang tidak pernah mengejutkanmu.
                        onMulai = { jalankanProgram = program },
                        // Kartu tidak tahu apa itu database. Dia cuma melapor
                        // "user sudah yakin mau hapus", dan HomeScreen yang
                        // mengeksekusi. Pola yang sama seperti dialog tambah.
                        onDelete = {
                            scope.launch {
                                /*
                                 * URUTANNYA TIDAK BOLEH DIBALIK, dan inilah harga
                                 * dari keputusan "tanpa foreign key" yang saya
                                 * tulis terang-terangan di `ProgramExerciseRef.kt`.
                                 *
                                 * Isi dulu yang dibuang, programnya belakangan.
                                 * Kalau dibalik -- program dihapus dulu, lalu app
                                 * mati sebelum perintah kedua jalan -- baris-baris
                                 * isinya tertinggal di disk TANPA JALAN PULANG:
                                 * id programnya sudah tidak ada di layar mana pun,
                                 * jadi tidak ada satu pun tombol yang bisa
                                 * memerintahkan penghapusannya lagi. Sampah abadi
                                 * yang tetap ikut dihitung.
                                 *
                                 * Dua perintah di dalam SATU `launch`, jadi
                                 * berurutan pasti: yang kedua tidak mulai sebelum
                                 * yang pertama selesai menulis.
                                 */
                                peDao.hapusSemuaDiProgram(program.id)
                                dao.delete(program)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        NewProgramDialog(
            onDismiss = { showNewDialog = false },
            onSave = { title, description ->
                // insert() itu suspend, jadi harus dijalankan di dalam scope.
                // id tidak diisi -> database yang menomori sendiri.
                scope.launch {
                    dao.insert(WorkoutProgram(title = title, description = description))
                }
                showNewDialog = false
            },
        )
    }
}

/**
 * Tampilan saat tabel masih kosong.
 *
 * Ini bukan tempelan. Daftar bohongan selalu kelihatan penuh, daftar asli
 * selalu mulai kosong -- dan layar kosong tanpa penjelasan adalah cara
 * tercepat bikin user bingung app-nya rusak atau memang belum diisi.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

// Preview-nya sekarang cuma untuk EmptyState. HomeScreen sudah menyentuh
// database asli, dan preview tidak punya HP untuk dibuka databasenya.
@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    CalisthenicaTheme(dynamicColor = false) {
        EmptyState(modifier = Modifier.fillMaxSize().padding(32.dp))
    }
}
