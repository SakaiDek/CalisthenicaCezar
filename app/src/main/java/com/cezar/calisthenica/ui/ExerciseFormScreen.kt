package com.cezar.calisthenica.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.AppDatabase
import com.cezar.calisthenica.data.simpanGambarKeInternal
import com.cezar.calisthenica.model.BodyRegion
import com.cezar.calisthenica.model.Equipment
import com.cezar.calisthenica.model.Exercise
import com.cezar.calisthenica.model.ExerciseCategory
import com.cezar.calisthenica.model.ExerciseType
import com.cezar.calisthenica.model.Ketinggian
import com.cezar.calisthenica.model.Muscle
import kotlinx.coroutines.launch

/**
 * Jumlah langkah wizard. Ditulis sebagai konstanta, bukan angka yang bertebaran
 * di enam tempat.
 *
 * DAN HARI INI ANGKA ITU BERUBAH: 3 -> 4. Baca lagi komentar versi sebelumnya
 * yang masih saya tinggalkan utuh di bawah, karena dia menebak persis apa yang
 * terjadi hari ini:
 *
 *   "Suatu hari kamu mau memecah langkah 2 jadi dua (alat sendiri, otot
 *   sendiri). Kalau angka 3 ditulis manual di mana-mana, kamu akan mengubah lima
 *   dan lupa yang keenam, lalu bingung kenapa tulisannya 'Langkah 4 dari 3'."
 *
 * Itu hari ini, dan buktinya ada di depan matamu: SATU BARIS ini yang saya ubah,
 * dan garis penunjuk langkah di `StepHeader` langsung jadi empat, `coerceIn` di
 * `keStep` langsung mengizinkan langkah 4, dan tombol Simpan langsung pindah ke
 * langkah 4 sendiri. Nol baris lain yang perlu saya sentuh untuk itu.
 *
 * Simpan pelajaran ini: konstanta bukan soal rapi-rapian. Dia soal seberapa
 * banyak tempat yang bisa salah saat kamu berubah pikiran.
 *
 * KENAPA DIPECAH JADI EMPAT. Karena judul langkah alat yang kamu minta --
 * "Apakah gerakan ini membutuhkan alat?" -- bentuknya SATU PERTANYAAN. Dan satu
 * pertanyaan pantas dapat satu layar; menempelkan 30 chip otot di bawah sebuah
 * pertanyaan bikin pertanyaannya jadi sekadar judul yang dilewati mata. Ditambah
 * satu alasan ukuran yang lebih keras: baris alat sekarang tinggi (~104dp
 * masing-masing), jadi alat + otot dalam satu langkah akan mengembalikan tembok
 * gulir yang baru kita bongkar.
 */
private const val TOTAL_STEP = 4

/**
 * Form gerakan baru, sekarang berbentuk WIZARD EMPAT LANGKAH.
 *
 * PELAJARAN PERTAMA HARI INI: kenapa satu halaman panjang itu bukan cuma
 * "kurang cantik", tapi memang salah secara teknis.
 *
 * Form versi lama memuat tujuh bagian dalam satu `verticalScroll`: nama, foto,
 * 7 chip kategori, 2 chip satuan, 13 chip alat, 30 chip otot, instruksi, dan
 * link. Saya hitung dari rekaman layarmu sendiri: kurang lebih EMPAT tinggi
 * layar Poco F5 -- dan itu belum dihitung keyboard.
 *
 * Nah, keyboard itu bagian yang paling jarang orang sadari, dan di rekamanmu
 * kelihatan jelas: keyboard TERBUKA terus selama kamu memilih chip otot, karena
 * fokus masih nyangkut di kolom nama dan tidak ada yang pernah melepasnya. Jadi
 * 30 chip itu tidak kamu lihat di layar penuh, tapi di sisa 45% layar di atas
 * keyboard. Empat tinggi layar berubah jadi sembilan. Itu sumber pusingnya, dan
 * itu bukan soal selera.
 *
 * Wizard memperbaiki dua-duanya sekaligus:
 *   1. Satu langkah = satu keputusan = satu layar. Tidak ada scroll panjang.
 *   2. Setiap kali pindah langkah, keyboard DIPAKSA tutup (`focus.clearFocus()`),
 *      jadi langkah 2 dapat tinggi layar penuh untuk chip-chipnya.
 *
 * PELAJARAN KEDUA: kenapa SEMUA `remember` ada di atas, sebelum `when (step)`.
 *
 * Compose membagikan "slot memori" berdasarkan URUTAN pemanggilan. Kalau kamu
 * menaruh `remember` di dalam cabang -- misalnya `var muscles` ditulis di dalam
 * blok langkah 2 -- maka begitu kamu pindah ke langkah 3, blok itu dibongkar,
 * slot-nya dilepas, dan pilihan ototmu HILANG. Kamu kembali ke langkah 2 dan
 * semuanya kosong lagi. Ini persis penyakit yang kita perbaiki di katalog
 * kemarin (pencarian yang ke-reset), cuma bajunya beda.
 *
 * Aturannya sekarang berlaku di seluruh proyek ini, hafalkan: SATU blok state di
 * paling atas, percabangan dan `return` selalu di bawahnya. Tanpa kecuali.
 *
 * @param awal Gerakan yang sedang DISUNTING, atau null kalau ini gerakan baru.
 * Satu parameter ini yang mengubah wizard tambah jadi wizard ubah -- tidak ada
 * layar kedua, tidak ada file baru, tidak ada duplikat 1500 baris yang harus
 * kamu perbaiki dua kali tiap ada bug. Semua state di bawah cuma perlu tahu
 * "nilai awalmu dari mana", dan sisanya jalan sendiri.
 *
 * @param onDiscardThumbnail Dipanggil kalau ada FILE FOTO yang harus dibuang --
 * saat kamu mengganti foto, atau saat kamu batal mengisi form padahal fotonya
 * sudah tersalin. Kenapa tidak dihapus di sini saja? Karena begitu form ini
 * ditutup, `rememberCoroutineScope` miliknya langsung dibatalkan bersama
 * layarnya, dan perintah hapus yang belum selesai ikut mati di tengah jalan.
 * Katalog di belakang sana masih hidup, jadi dia yang saya suruh mengeksekusi.
 * Pola yang sama seperti onSave: form melapor, orang lain bertindak.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFormScreen(
    awal: Exercise? = null,
    onCancel: () -> Unit,
    onSave: (Exercise) -> Unit,
    onDiscardThumbnail: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pemegang kendali fokus. Inilah yang bisa menyuruh keyboard menyingkir.
    val focus = LocalFocusManager.current

    /*
     * ==========================================================================
     *  PENGAKUAN JUJUR: MULAI HARI INI FORM KENAL ROOM.
     * ==========================================================================
     *
     * Sampai kemarin aturan file ini tegas: "form tidak kenal Room" -- dia cuma
     * merakit satu object `Exercise` lalu menyerahkannya lewat `onSave`, dan
     * katalog yang menyimpannya. Aturan itu masih berlaku UNTUK GERAKAN, dan
     * jangan kamu longgarkan. Tapi untuk ALAT, aturan itu saya langgar dengan
     * sadar, dan kamu berhak tahu alasannya sebelum kamu menemukannya sendiri
     * dan menyangka saya lupa.
     *
     * Alasannya satu baris di `EquipmentDao`: `insert` mengembalikan `Long` --
     * id baris yang baru lahir. Dan HANYA YANG MEMANGGIL `insert` yang menerima
     * angka itu. Kalau penyimpanan alat baru saya lempar ke atas seperti
     * `onSave`, maka form ini tidak akan pernah tahu id alat yang baru kamu
     * buat. Akibatnya di layar: kamu menekan "+ Tambah alat", mengetik "Matras",
     * menekan Simpan -- dan barisnya muncul dalam keadaan TIDAK tercentang. Kamu
     * harus mencarinya lagi di daftar dan menekannya sendiri. Satu langkah
     * konyol yang terjadi tiap kali, cuma demi menjaga kemurnian aturan.
     *
     * Jadi bedanya begini, dan ini garis yang layak kamu pakai di layar lain:
     * gerakan itu HASIL dari form ini (dilaporkan ke atas, disimpan orang lain),
     * sedangkan alat cuma BAHAN yang dipakai form ini (dikelola di tempat dia
     * dipakai). Yang dilaporkan ke atas itu hasil, bukan bahan.
     *
     * Pola `remember(context) { ... }` lalu `remember(dao) { dao.observeAll() }`
     * disalin apa adanya dari katalog, dan bukan karena malas. `observeAll()`
     * membuat objek Flow BARU setiap kali dipanggil. Kalau dia dipanggil telanjang
     * di dalam `collectAsState`, setiap recomposition -- dan recomposition terjadi
     * tiap kali kamu mengetik satu huruf di kolom Nama -- akan MEMBATALKAN
     * langganan lama dan membuka langganan baru ke database. App-nya tetap jalan,
     * tapi HP-mu bekerja keras tanpa hasil. `remember` yang menahannya.
     */
    val alatDao = remember(context) { AppDatabase.get(context).equipmentDao() }
    val alatFlow = remember(alatDao) { alatDao.observeAll() }

    // Di sini `initial = emptyList()` memang benar, BEDA dengan katalog yang
    // sengaja memakai `initial = null`. Katalog perlu membedakan "database belum
    // menjawab" dari "sudah dijawab, isinya kosong", supaya tulisan "Katalog
    // masih kosong" tidak berkedip sepersekian detik tiap kali dibuka. Di sini
    // pembedaan itu tidak perlu: langkah alat baru terlihat setelah kamu menekan
    // Lanjut, dan database sudah lama menjawab jauh sebelum jempolmu sampai
    // sana. Menambah keadaan yang tidak bisa terlihat itu cuma menambah `if`.
    val daftarAlat by alatFlow.collectAsState(initial = emptyList())

    // ============================================================
    //  SATU-SATUNYA BLOK STATE. Semuanya di sini, di atas segalanya.
    //
    //  PELAJARAN KEDUA HARI INI: mode ubah itu cuma soal NILAI AWAL.
    //
    //  Perhatikan pola `awal?.x ?: bawaan` yang berulang di bawah. Itu seluruh
    //  rahasianya. `remember { mutableStateOf(...) }` menjalankan isi kurungnya
    //  TEPAT SEKALI -- saat layar ini pertama muncul -- lalu tidak pernah lagi.
    //  Jadi menyuapkan data gerakan lama ke situ sudah cukup: kamu mengetik,
    //  state-nya berubah seperti biasa, dan `awal` tidak pernah ikut berubah.
    //
    //  Konsekuensi yang harus kamu tahu (dan ini SENGAJA): kalau `awal` berganti
    //  isi sementara form-nya sedang terbuka, kolom-kolom ini TIDAK ikut
    //  berganti. Di alur kita itu mustahil -- form dibuka dari satu gerakan dan
    //  ditutup sebelum pindah ke gerakan lain. Kalau suatu hari alurnya berubah,
    //  obatnya `remember(awal?.id) { ... }`, bukan menambah LaunchedEffect.
    // ============================================================

    // Langkah yang sedang tampil, 1..TOTAL_STEP. Mode ubah pun mulai dari 1,
    // bukan langsung ke langkah terakhir: menyunting sering berarti "saya mau
    // lihat dulu semua isinya", dan melewati langkah 1 justru menyembunyikan
    // nama dan kategori yang paling sering salah.
    var step by remember { mutableStateOf(1) }

    var name by remember { mutableStateOf(awal?.name ?: "") }

    // Sengaja null di awal, bukan langsung VERTICAL_PULL. Kalau ada nilai
    // default, kamu bisa menyimpan 20 gerakan yang semuanya berlabel Vertical
    // Pull tanpa sadar -- dan itu ketahuannya nanti, waktu filter katalog
    // terasa ngawur. Lebih baik dipaksa memilih satu kali sekarang.
    //
    // Di mode ubah dia TIDAK PERNAH null, karena gerakan yang sudah tersimpan
    // wajib punya kategori. Jadi tombol Simpan langsung hidup -- benar, dan itu
    // memang yang kamu harapkan saat cuma mau menambah satu chip otot.
    var category by remember { mutableStateOf<ExerciseCategory?>(awal?.category) }

    var type by remember { mutableStateOf(awal?.defaultType ?: ExerciseType.REPS) }

    // Alat yang dipakai, disimpan sebagai KUMPULAN ID (angka), bukan kumpulan
    // object Equipment. Ini pelajaran yang sudah pernah kamu lihat di katalog
    // (`var selected` di ExerciseCatalogScreen) dan sekarang berlaku di sini
    // dengan alasan yang PERSIS SAMA, cuma lebih tajam:
    //
    // Alat sekarang bisa kamu ubah namanya dan ganti fotonya KAPAN SAJA, bahkan
    // dari dalam form ini juga. Kalau yang saya simpan object-nya, lalu kamu
    // tekan lama "Matras" dan menggantinya jadi "Yoga Mat", object di dalam Set
    // ini jadi versi LAMA -- dan `alat in equipment` mendadak bernilai false,
    // karena data class membandingkan SELURUH isinya, bukan cuma id-nya.
    // Barisnya akan kelihatan tidak tercentang padahal kamu tidak pernah
    // melepasnya. Bug yang mustahil kamu duga dari layar.
    //
    // Id itu satu-satunya bagian yang tidak pernah berubah. Jadi id yang
    // dipegang -- dan kebetulan itu juga persis bentuk yang mau disimpan ke
    // kolom database nanti. Dua alasan yang menunjuk arah yang sama.
    //
    // Tetap TIDAK ADA nilai awal dan tidak ada pilihan "Tanpa Alat". Set kosong
    // ITULAH jawaban "cukup badan sendiri". Baca Equipment.kt kalau lupa kenapa.
    //
    // `toSet()` di mode ubah bukan hiasan: yang tersimpan di baris gerakan itu
    // `List<Long>`, sedangkan yang dipegang layar ini `Set<Long>`. Kalau List
    // dipaksa masuk, `equipment - id` dan `equipment + id` di langkah 2 tetap
    // jalan tapi hasilnya bisa berisi id ganda -- dan alat yang sama tercentang
    // dua kali di dalam data, satu kali di layar. Tipe yang benar menutup
    // seluruh kelas bug itu tanpa satu pun `if`.
    var equipment by remember { mutableStateOf(awal?.equipmentIds?.toSet() ?: emptySet<Long>()) }

    /*
     * KETINGGIAN ALAT. Dua variabel, dan dua-duanya perlu -- baca kenapa, karena
     * ini pola yang akan kamu pakai berulang kali: satu untuk NILAI, satu untuk
     * LAYAR yang mengubah nilai itu.
     *
     * `tinggi` = angka `Ketinggian.tingkat` yang akan masuk ke kolom
     * `exercises.equipmentHeight`. 0 = belum diatur.
     *
     * `aturTinggiUntuk` = alat mana yang sedang dibuka pemilihnya. `null` berarti
     * pemilih tertutup. Perhatikan tipenya `Equipment?` dan bukan `Boolean`:
     * layar pemilih menampilkan NAMA alatnya di judul ("Ring kayu bikinan
     * sendiri"), jadi dia butuh alatnya, bukan cuma tahu bahwa dia sedang
     * terbuka. Menyimpan bendanya sekaligus menutup pertanyaan "yang mana".
     */
    var tinggi by remember { mutableStateOf(awal?.equipmentHeight ?: 0) }
    var aturTinggiUntuk by remember { mutableStateOf<Equipment?>(null) }

    // Set, bukan List. Bedanya nyata di sini: Set menolak isi ganda dengan
    // sendirinya, jadi mustahil ada BICEPS tercatat dua kali walau chip-nya
    // ditekan dua puluh kali.
    var muscles by remember { mutableStateOf(awal?.muscles?.toSet() ?: emptySet<Muscle>()) }

    var instruction by remember { mutableStateOf(awal?.instruction ?: "") }
    var youtube by remember { mutableStateOf(awal?.youtubeUrl ?: "") }

    // Pilihan format "Cara melakukan" di langkah 4: true = langkah bernomor,
    // false = paragraf. Diangkat ke sini (bukan di-`remember` dalam Step4)
    // karena `remember` mati bersama composable-nya, jadi pilihannya akan
    // ter-reset setiap kali kamu menekan Kembali lalu Lanjut lagi.
    //
    // Nilai awalnya DIBACA dari teks yang sudah ada, bukan dihardcode: gerakan
    // lama yang instruksinya berbentuk paragraf akan terbuka di mode paragraf.
    // Gerakan baru (teks masih kosong) mulai di mode langkah, karena itu yang
    // paling sering dipakai untuk "cara melakukan".
    var pakaiLangkah by remember {
        val awalnya = awal?.instruction ?: ""
        mutableStateOf(awalnya.isBlank() || instruksiBernomor(awalnya))
    }

    // Nama file foto yang sudah tersalin ke folder app. Kosong = belum ada.
    // Perhatikan yang TIDAK disimpan di sini: Uri galerinya. Sekali disalin,
    // Uri itu tidak berguna lagi -- lihat penjelasan panjang di MediaFiles.kt.
    var thumbnail by remember { mutableStateOf(awal?.thumbnailFile ?: "") }
    var copying by remember { mutableStateOf(false) }
    var copyFailed by remember { mutableStateOf(false) }

    /*
     * JEBAKAN PALING BERBAHAYA DI SELURUH PERUBAHAN HARI INI. Baca sampai habis,
     * karena kalau baris ini tidak ada, app-mu MENGHAPUS FOTO YANG KAMU SIMPAN
     * dan tidak ada satu pun pesan error yang memberitahu.
     *
     * Ceritanya begini. Aturan pembersihan foto di form ini dibuat waktu form
     * cuma bisa MENAMBAH: setiap foto yang tersalin tapi tidak jadi dipakai
     * harus dibuang, kalau tidak dia jadi file yatim yang memakan penyimpanan
     * HP-mu selamanya. Jadi `batal` menghapus `thumbnail` tanpa tanya.
     *
     * Sekarang form yang sama dipakai untuk MENGUBAH. Kamu buka gerakan "Muscle
     * Up" yang fotonya sudah ada sejak seminggu lalu, lihat-lihat, lalu tekan X
     * karena tidak ada yang mau diubah. Aturan lama tadi jalan: fotonya dihapus
     * dari folder app. Barisnya di database masih menyebut nama file itu, jadi
     * kartunya sekarang menunjuk foto yang sudah lenyap. Kotak kosong permanen
     * yang cuma bisa kamu perbaiki dengan memilih ulang fotonya.
     *
     * `fotoAwal` adalah pembatasnya: nama file MILIK DATABASE. Aturan barunya
     * satu kalimat, dan berlaku di tiga tempat di bawah -- jangan pernah buang
     * file yang namanya sama dengan `fotoAwal`, KECUALI di dalam `simpan()`
     * setelah kepastian ada foto pengganti yang tersimpan.
     *
     * Ini bentuk umum dari pelajaran yang lebih besar: begitu satu layar melayani
     * dua maksud, semua kode pembersihan di dalamnya wajib diperiksa ulang. Yang
     * berubah bukan kodenya, tapi ARTI dari nilai yang dia pegang.
     */
    val fotoAwal = awal?.thumbnailFile ?: ""

    // ---------- Keadaan dialog kelola alat (tambah / ubah / hapus) ----------
    //
    // TIGA variabel untuk satu dialog. Kelihatan berlebihan, jadi baca kenapa
    // tidak bisa lebih sedikit.
    //
    // `alatDiubah` menyimpan alat mana yang sedang disunting. `null` di situ
    // ambigu -- dia bisa berarti "tidak ada dialog" ATAU "dialog terbuka dalam
    // mode tambah, jadi belum ada alat yang dipegang". Satu variabel tidak bisa
    // mengucapkan dua hal berbeda dengan satu nilai yang sama. Itu sebabnya ada
    // `alatDitambah` sebagai saklar terpisah.
    //
    // Kenapa tidak satu enum kecil saja (`Mode.TUTUP / TAMBAH / UBAH`)? Boleh,
    // dan di layar yang lebih rumit itu memang jawabannya. Di sini dua variabel
    // masih lebih mudah dibaca daripada enum + nullable yang harus dibaca
    // berpasangan. Aturan praktisnya: bikin enum saat keadaannya sudah tiga
    // ke atas DAN ada yang tidak boleh terjadi bersamaan.
    var alatDiubah by remember { mutableStateOf<Equipment?>(null) }
    var alatDitambah by remember { mutableStateOf(false) }

    // Alat yang menunggu konfirmasi hapus, plus jumlah gerakan yang memakainya.
    // Dua-duanya harus ada SEBELUM dialog konfirmasi digambar, karena angka itu
    // bagian dari kalimat peringatannya ("dipakai di 3 gerakan"). Angkanya
    // `Int?` -- null berarti "masih dihitung", dan selama null tombol Hapus
    // dimatikan. Menyuruh orang menyetujui penghapusan sebelum angkanya keluar
    // itu sama dengan tidak memperingatkan sama sekali.
    var alatDihapus by remember { mutableStateOf<Equipment?>(null) }
    var jumlahPemakaiAlat by remember { mutableStateOf<Int?>(null) }

    // Posisi scroll. Ikut diangkat ke atas, dan alasannya beda dari yang lain:
    // bukan supaya diingat, tapi supaya bisa DIRESET. Lihat LaunchedEffect di
    // bawah.
    val scrollState = rememberScrollState()

    /*
     * Posisi scroll DAFTAR ALAT, dan ini benda yang BERBEDA dari `scrollState`
     * di atasnya. Sejak hari ini langkah 2 tidak lagi ikut menumpang scroll
     * wizard: dia punya `LazyColumn` sendiri, dan daftar malas menyimpan
     * posisinya di `LazyListState`, bukan di `ScrollState`.
     *
     * Kenapa diangkat sampai ke sini, bukan di-`remember` di dalam `StepAlat`:
     * `remember` mati bersama composable yang memuatnya. Kalau ingatan posisinya
     * tinggal di dalam `StepAlat`, maka setiap kali kamu tekan Lanjut lalu
     * Kembali, daftar alatmu melompat balik ke puncak -- padahal kamu tadi sudah
     * scroll jauh ke bawah untuk mencentang alat ke sepuluh. Di sini, ingatannya
     * hidup selama form-nya hidup.
     *
     * Perhatikan dia SENGAJA tidak ikut direset oleh `LaunchedEffect(step)` di
     * bawah. Reset itu untuk mencegah langkah 3 muncul dalam keadaan ter-scroll
     * gara-gara langkah 2 -- masalah yang sekarang mustahil terjadi, karena
     * dua langkah itu tidak lagi berbagi satu ingatan.
     */
    val alatListState = rememberLazyListState()

    /**
     * PELAJARAN KETIGA HARI INI: Photo Picker, dan kenapa dia NOL PERMISSION.
     *
     * Cara jadul minta foto adalah `READ_EXTERNAL_STORAGE` (atau
     * `READ_MEDIA_IMAGES` di Android 13+): kamu minta izin membaca SELURUH
     * galeri, user melihat popup menakutkan "Izinkan app mengakses foto dan
     * media di perangkat?", dan sebagian orang menekan Tolak lalu fiturmu mati.
     *
     * Photo Picker membalik arahnya. Yang membuka galeri BUKAN app-mu -- itu
     * layar milik sistem, di luar proses app-mu. App-mu tidak pernah bisa
     * melihat galeri; dia hanya menerima satu Uri untuk satu foto yang user
     * benar-benar tunjuk sendiri. Karena tidak ada yang perlu diizinkan, tidak
     * ada popup sama sekali. Nol baris di AndroidManifest.
     */
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        // uri null = user menekan Back di layar galeri. Bukan error, jadi
        // jangan tampilkan pesan gagal apa pun. Diam saja.
        if (uri != null) {
            copying = true
            copyFailed = false
            scope.launch {
                val fotoLama = thumbnail
                val namaBaru = simpanGambarKeInternal(context, uri)
                copying = false
                if (namaBaru == null) {
                    copyFailed = true
                } else {
                    thumbnail = namaBaru
                    // Foto lama dibuang HANYA setelah yang baru berhasil
                    // tersalin. Urutan ini penting: kalau dihapus lebih dulu
                    // lalu penyalinan gagal, kamu kehilangan dua-duanya.
                    //
                    // Syarat kedua (`!= fotoAwal`) itu penjaga mode ubah. Kamu
                    // ganti foto, lalu berubah pikiran dan tekan X -- kalau foto
                    // lama sudah saya hapus di sini, gerakan tersimpan itu
                    // kehilangan fotonya padahal kamu membatalkan. Selama masih
                    // milik database, file itu belum boleh disentuh; yang
                    // memutuskan nanti `simpan()`, saat penggantinya sudah pasti.
                    if (fotoLama.isNotBlank() && fotoLama != fotoAwal) {
                        onDiscardThumbnail(fotoLama)
                    }
                }
            }
        }
    }

    /**
     * Batal = buang juga fotonya, kalau ada -- kecuali foto itu MILIK DATABASE.
     *
     * `thumbnail != fotoAwal` itu satu-satunya yang membedakan "file nganggur
     * yang saya salin barusan" dari "foto gerakan yang sudah tersimpan". Tanpa
     * dia, menekan X di mode ubah menghapus foto asli. Lihat penjelasan panjang
     * di `fotoAwal` kalau kamu lupa kenapa.
     */
    val batal = {
        if (thumbnail.isNotBlank() && thumbnail != fotoAwal) onDiscardThumbnail(thumbnail)
        onCancel()
    }

    // Syarat lolos dari langkah 1. Dipakai dua kali: mengunci tombol Lanjut,
    // dan mengunci tombol Simpan di langkah 3. Satu aturan, satu tempat.
    val step1Lengkap = name.isNotBlank() && category != null

    /**
     * Satu-satunya pintu untuk berpindah langkah.
     *
     * Kenapa dibungkus fungsi, bukan `step = step + 1` ditulis di tombolnya?
     * Karena berpindah langkah itu TIGA pekerjaan, bukan satu: tutup keyboard,
     * ganti nomor langkah, dan (lewat LaunchedEffect di bawah) balik ke puncak
     * layar. Kalau ditulis manual di tiap tombol, cepat atau lambat ada satu
     * tombol yang lupa menutup keyboard -- dan kamu akan menyalahkan Compose,
     * padahal salahnya di sini.
     *
     * `coerceIn` itu jaring pengaman: apa pun yang dikirim, hasilnya dijamin
     * 1..TOTAL_STEP. Jadi mustahil ada langkah 0 atau langkah 5 yang layarnya
     * kosong. Perhatikan batas atasnya ditulis `TOTAL_STEP`, bukan angka -- itu
     * sebabnya memecah wizard jadi empat langkah hari ini tidak menyentuh fungsi
     * ini sama sekali.
     */
    fun keStep(tujuan: Int) {
        focus.clearFocus()
        step = tujuan.coerceIn(1, TOTAL_STEP)
    }

    /**
     * Tiap ganti langkah, layar dikembalikan ke puncak.
     *
     * `scrollState` sengaja saya angkat ke atas supaya isinya SATU untuk ketiga
     * langkah -- hemat, tapi ada efek sampingnya: kalau kamu scroll jauh ke
     * bawah di langkah 2 lalu tekan Lanjut, langkah 3 muncul dalam keadaan
     * SUDAH ter-scroll. Kolom "Cara melakukan" ada di atas sana, di luar
     * pandangan, dan kamu akan mengira langkah 3 kosong.
     *
     * Ini contoh bagus dari hal yang tidak akan pernah muncul di build-log:
     * kode-nya benar, compiler senang, tapi manusianya bingung. Satu baris di
     * bawah ini yang membetulkannya.
     */
    LaunchedEffect(step) { scrollState.scrollTo(0) }

    /**
     * Menghitung berapa gerakan yang memakai alat yang mau dihapus.
     *
     * PELAJARAN HARI INI: kenapa ini `LaunchedEffect` dan bukan dipanggil
     * langsung di dalam tombol "Hapus alat".
     *
     * `jumlahPemakai` itu fungsi `suspend` -- dia menyentuh disk, dan menyentuh
     * disk dari thread utama itu yang bikin app "nge-freeze" sepersekian detik.
     * Fungsi suspend cuma bisa dipanggil dari dalam coroutine, dan `LaunchedEffect`
     * itu coroutine yang UMURNYA TERIKAT pada layar ini: kalau form ditutup di
     * tengah perhitungan, coroutine-nya ikut dibatalkan sendiri. Tidak ada sisa
     * pekerjaan yang menulis ke state milik layar yang sudah tidak ada.
     *
     * `key` = `alatDihapus`. Artinya: blok ini dijalankan ulang setiap kali
     * isinya BERGANTI, bukan setiap recomposition. Jadi mengetik nama di kolom
     * lain tidak memicu perhitungan ulang ke database.
     *
     * Baris `jumlahPemakaiAlat = null` di awal itu wajib, dan ini jebakan halus:
     * tanpa dia, saat kamu membuka konfirmasi untuk alat KEDUA, angka milik alat
     * PERTAMA masih terpampang sepersekian detik. Peringatan yang menyebut angka
     * salah lebih berbahaya daripada peringatan yang bilang "tunggu, sedang
     * dihitung".
     */
    LaunchedEffect(alatDihapus) {
        val sasaran = alatDihapus
        jumlahPemakaiAlat = null
        if (sasaran != null) {
            jumlahPemakaiAlat = alatDao.jumlahPemakai(sasaran.id)
        }
    }

    /*
     * ==================================================================
     * SATU ATURAN UNTUK ANGKA KETINGGIAN: kalau tidak ada satu pun alat
     * tercentang yang boleh diatur, angkanya WAJIB 0.
     *
     * Kenapa ini `LaunchedEffect` dan bukan `if` yang ditempel di tombolnya:
     * ada TIGA jalan berbeda yang bisa membuat aturan ini dilanggar, dan saya
     * cuma mau menuliskan obatnya sekali.
     *   1. Kamu melepas centang alatnya di langkah 2.
     *   2. Kamu tekan-lama alatnya, lalu MEMATIKAN sakelar "bisa diatur".
     *   3. Kamu MENGHAPUS alatnya dari gudang.
     * Jalan 2 dan 3 tidak lewat tombol centang sama sekali, jadi `if` di sana
     * tidak akan pernah menangkapnya. Yang mereka bertiga ubah cuma satu hal
     * yang sama: jawaban atas pertanyaan "masih ada alat yang bisa diatur?".
     * Jadi itulah yang diawasi.
     *
     * Kalau aturan ini tidak ada, kamu bisa menyimpan "Push Up" yang di
     * database-nya tertulis ketinggian "Sepinggul" -- angka hantu yang tidak
     * tampil di mana pun, sampai suatu hari kamu mencentang ring di gerakan itu
     * dan mendadak ketinggiannya sudah terisi sendiri. Data yang tidak
     * konsisten selalu menagih di kemudian hari.
     *
     * SEKARANG BAGIAN YANG PALING GAMPANG SALAH, dan salahnya berupa DATA
     * HILANG tanpa error. `daftarAlat` datang dari Flow database, dan Flow
     * selalu punya nilai awal `emptyList()` di frame pertama -- isi gudang baru
     * menyusul beberapa milidetik kemudian. Di frame pertama itu, jawaban
     * "masih ada alat yang bisa diatur?" adalah TIDAK, karena daftarnya masih
     * kosong. Tanpa penjaga, aturan ini langsung menolkan `tinggi` yang baru
     * saja dibaca dari gerakan tersimpanmu. Kamu membuka Ring Row yang
     * ketinggiannya "Sekepala", dan angkanya lenyap sebelum matamu sampai ke
     * layar.
     *
     * `daftarAlat.isEmpty()` dipasang jadi KUNCI KEDUA, bukan cuma penjaga di
     * dalam blok. Bedanya penting: sebagai kunci, dia memaksa blok ini
     * dijalankan ULANG tepat saat gudang selesai terisi. Kalau dia cuma
     * penjaga di dalam, blok ini tidak pernah berjalan lagi setelah frame
     * pertama (jawaban "tidak ada" tidak berubah nilainya), dan angka hantu
     * milik alat yang sudah kamu hapus akan lolos.
     * ==================================================================
     */
    val adaAlatBisaDiatur = daftarAlat.any { it.adjustableHeight && it.id in equipment }
    LaunchedEffect(adaAlatBisaDiatur, daftarAlat.isEmpty()) {
        if (!adaAlatBisaDiatur && daftarAlat.isNotEmpty()) tinggi = 0
    }

    /**
     * Tombol Back HP: turun satu langkah dulu, baru keluar dari form.
     *
     * Ini perilaku yang orang harapkan tanpa pernah diberi tahu. Kalau Back
     * langsung membuang seluruh form dari langkah 3, kamu kehilangan tiga menit
     * pengisian karena satu tekanan yang maksudnya cuma "eh, balik dulu".
     *
     * BackHandler tetap di SINI, bukan di katalog, supaya Back melewati
     * pembersihan foto yang sama dengan tombol X. Aturan umumnya: yang tahu apa
     * yang perlu dibersihkan, dia yang pegang Back.
     *
     * TAMBAHAN 5 Sept 2026 -- dan ini alasan kenapa layar pemilih ketinggian
     * TIDAK memasang BackHandler-nya sendiri. Kalau dua composable sama-sama
     * memasang BackHandler, yang menang adalah yang paling belakang dipasang, dan
     * "paling belakang" itu urusan urutan komposisi yang mudah berubah tanpa kamu
     * sadari. Jadi Back tetap SATU pemilik, dan pemiliknya menyusun urutan
     * pulangnya sebagai daftar prioritas: tutup pemilih dulu (kalau terbuka),
     * baru mundur langkah, baru keluar form. Urutan `when` di bawah ITULAH
     * urutan prioritasnya -- yang di atas menang.
     */
    BackHandler {
        when {
            aturTinggiUntuk != null -> aturTinggiUntuk = null
            step > 1 -> keStep(step - 1)
            else -> batal()
        }
    }

    /**
     * Merakit object Exercise lalu menyerahkannya lewat `onSave`.
     *
     * Perhatikan yang TIDAK terjadi di sini walau hari ini form sudah kenal Room:
     * gerakannya tetap TIDAK disimpan di fungsi ini. Dia dilaporkan ke atas, dan
     * katalog yang memanggil `dao.insert`. Alasannya bukan kemurnian aturan --
     * lihat penjelasan `onDiscardThumbnail` di atas: begitu form ditutup,
     * `rememberCoroutineScope` miliknya dibatalkan, dan perintah simpan yang
     * belum selesai ikut mati di tengah jalan. Katalog di belakang masih hidup.
     */
    fun simpan() {
        // Disalin ke variabel lokal supaya Kotlin yakin nilainya tidak berubah
        // jadi null di tengah jalan. Ini yang namanya smart cast, dan cuma
        // jalan pada val lokal.
        val chosen = category ?: return

        // Foto asli baru boleh dibuang DI SINI, dan cuma kalau memang sudah
        // diganti. Di titik ini penggantinya sudah pasti ada di folder app dan
        // barisnya sebentar lagi ditimpa, jadi tidak ada momen di mana database
        // menunjuk file yang tidak ada.
        if (fotoAwal.isNotBlank() && fotoAwal != thumbnail) onDiscardThumbnail(fotoAwal)

        onSave(
            Exercise(
                // ============ DUA BARIS YANG MENENTUKAN TAMBAH vs UBAH ============
                //
                // `id` itu satu-satunya pembeda antara "gerakan baru" dan
                // "gerakan yang sama, isinya diperbarui". Room mencocokkan
                // `@Update` lewat @PrimaryKey -- kalau id-nya 0, dia tidak
                // menemukan baris mana pun untuk ditimpa dan perubahanmu hilang
                // tanpa error. Dan di jalur tambah, 0 itu justru yang benar:
                // `autoGenerate = true` membaca 0 sebagai "kamu yang kasih
                // nomor".
                id = awal?.id ?: 0,
                name = name.trim(),
                category = chosen,
                // Simpan mengikuti FORMAT YANG DIPILIH (pakaiLangkah), BUKAN
                // menebak-nebak dari isi teks. Inilah penutup bug "tab sudah di
                // Langkah bernomor tapi tersimpan sebagai paragraf": dulu baris
                // ini cuma `instruction.trim()` apa adanya, jadi teks campur-aduk
                // hasil paste ("1. a\nb\nc") tersimpan mentah dan terbaca
                // paragraf. Sekarang PILIHAN TAB yang jadi hakim -- tak perlu
                // dipancing klik dua kali lagi.
                instruction = rapikanInstruksi(instruction, pakaiLangkah),
                defaultType = type,
                // Diurutkan ulang mengikuti urutan enum, bukan urutan kamu
                // menekan chip. Supaya "Latissimus, Biceps" selalu tampil
                // dengan urutan yang sama di seluruh app.
                muscles = Muscle.entries.filter { it in muscles },
                // `sorted()` di sini bukan supaya tampilannya urut -- yang
                // mengurutkan tampilan itu `ORDER BY name` di EquipmentDao.
                // Ini supaya ISI KOLOMNYA tidak goyang: `Set` di Kotlin tidak
                // menjamin urutan, jadi alat yang sama persis bisa tersimpan
                // sebagai "6,11" hari ini dan "11,6" besok. Dua teks berbeda
                // untuk satu keadaan yang sama itu bikin dua hal jadi susah:
                // membandingkan baris, dan membaca isi database saat kamu
                // berburu bug. Satu kata ini menutupnya sekarang, gratis.
                equipmentIds = equipment.sorted(),
                // Satu angka untuk satu gerakan. Kalau tidak ada alat yang bisa
                // diatur tercentang, nilainya sudah dijaga 0 oleh logika toggle
                // di langkah 2 -- jadi di sini tidak perlu ada `if` lagi.
                // Aturan yang pantas dicatat: bersihkan nilai di tempat
                // penyebabnya terjadi, bukan di tempat penyimpanannya. Kalau
                // pembersihan ditaruh di sini, ringkasan langkah 4 masih akan
                // memamerkan ketinggian untuk gerakan yang sudah tidak beralat
                // sama sekali, dan kamu baru tahu salahnya setelah tersimpan.
                equipmentHeight = tinggi,
                thumbnailFile = thumbnail,
                // JEBAKAN YANG TIDAK AKAN PERNAH MUNCUL DI BUILD-LOG, dan ini
                // pelajaran terakhir hari ini. Form ini belum punya kolom video
                // sama sekali. Kalau baris ini tidak ada, `videoFile` terisi
                // nilai bawaannya ("") -- dan begitu kamu menyunting satu
                // gerakan cuma untuk memperbaiki typo namanya, video yang sudah
                // kamu pasang lewat jalur lain IKUT TERHAPUS dari barisnya.
                //
                // Aturannya, dan pakai ini di setiap form ubah yang kamu tulis
                // seumur hidup: form wajib MENERUSKAN kolom yang tidak bisa dia
                // sunting. Constructor tidak tahu apa-apa soal nilai lama; dia
                // menulis ulang SELURUH baris, bukan cuma yang kamu sentuh.
                videoFile = awal?.videoFile ?: "",
                youtubeUrl = youtube.trim(),
            ),
        )
    }

    val judulStep = stringResource(
        when (step) {
            1 -> R.string.form_step1_label
            2 -> R.string.form_step2_label
            3 -> R.string.form_step3_label
            else -> R.string.form_step4_label
        },
    )

    /*
     * ==================================================================
     * LAPISAN PEMILIH KETINGGIAN, dan TIGA hal di sini yang wajib kamu pahami
     * karena dua di antaranya bisa merusak state form tanpa satu pun error.
     *
     * SATU -- kenapa `return`, bukan dialog atau bottom sheet.
     * `return` dari sebuah @Composable itu sah di Kotlin, dan artinya: semua yang
     * ditulis DI BAWAH baris ini tidak digambar sama sekali. Bukan disembunyikan,
     * tidak digambar. Jadi selama pemilih terbuka, Scaffold beserta seluruh
     * wizard-nya benar-benar tidak ada di layar. Itu yang kita mau: satu layar
     * penuh, tanpa jendela baru. (Kenapa bukan ModalBottomSheet padahal kamu
     * sudah menyetujuinya: alasannya tiga, tertulis lengkap di KDoc
     * LayarKetinggian.kt. Saya sebutkan juga di chat, bukan cuma di kode.)
     *
     * DUA -- kenapa dia HARUS di bawah semua `remember`, dan ini jebakannya.
     * Compose mengenali state dari POSISI URUT pemanggilan, bukan dari nama
     * variabel. Kalau baris `return` ini saya taruh di atas, misalnya sebelum
     * `var thumbnail by remember { ... }`, maka saat pemilih terbuka Compose tidak
     * pernah sampai ke `remember` itu; dia menganggapnya keluar dari komposisi
     * dan MEMBUANG ingatannya. Begitu pemilih ditutup, `thumbnail` lahir ulang
     * dengan nilai awal -- nama file foto yang kamu pilih tadi lenyap dari state,
     * padahal filenya sudah tersalin di folder app. Foto yatim, dan form yang
     * seperti lupa. Aturannya sekalimat: `return` di composable selalu SETELAH
     * semua ingatan terpasang.
     *
     * TIGA -- `val sasaran = aturTinggiUntuk` dulu, baru diperiksa.
     * `aturTinggiUntuk` itu `var` hasil delegasi state; Kotlin tidak mau
     * mempersempit tipenya dari `Equipment?` jadi `Equipment` walau sudah
     * diperiksa `!= null`, karena secara teori nilainya bisa berganti di antara
     * pemeriksaan dan pemakaian. Disalin ke `val` lokal, smart cast-nya jalan.
     * Pola yang sama sudah kamu lihat di `val chosen = category ?: return`.
     * ==================================================================
     */
    val sasaranTinggi = aturTinggiUntuk
    if (sasaranTinggi != null) {
        LayarKetinggian(
            alat = sasaranTinggi,
            terpilih = tinggi,
            // Perhatikan: setiap geseran jempol langsung menulis ke `tinggi`,
            // tidak ada nilai sementara yang menunggu tombol "Pakai ini".
            // Tombol itu cuma MENUTUP layar. Kenapa begitu: kalau ada nilai
            // sementara, kita butuh satu state lagi plus aturan "kalau Back
            // ditekan, buang perubahannya" -- dan pemilih ini bukan tempat yang
            // pantas menanggung kerumitan itu. Yang tersimpan ke database baru
            // terjadi saat kamu menekan Simpan di langkah 4, jadi Back dari
            // seluruh form tetap membatalkan segalanya.
            onPilih = { tinggi = it },
            onSelesai = { aturTinggiUntuk = null },
        )
        return
    }

    Scaffold(
        topBar = {
            // TopAppBar dan penunjuk langkah dibungkus satu Column supaya
            // keduanya ikut dianggap "bagian atas" oleh Scaffold. Kalau
            // StepHeader ditaruh di dalam area isi, dia akan ikut ter-scroll
            // dan kamu kehilangan penunjuk langkah tepat saat paling butuh.
            Column {
                TopAppBar(
                    // Judulnya ikut memberi tahu kamu sedang di mode apa. Ini
                    // bukan kosmetik: dua mode yang tampilannya identik itu cara
                    // tercepat bikin kamu mengisi form tambah padahal maksudmu
                    // mengubah, lalu bingung kenapa gerakannya jadi dua.
                    title = {
                        Text(
                            text = stringResource(
                                if (awal == null) R.string.form_title else R.string.form_title_edit,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = batal) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.dialog_cancel),
                            )
                        }
                    },
                )
                StepHeader(step = step, judul = judulStep)
            }
        },
        bottomBar = {
            /*
             * BUG YANG KAMU LAPORKAN 5 Sept 2026 -- dan ini laporan bug terbaik
             * yang kamu kirim sejauh ini, karena kamu menyebutkan GEJALANYA:
             * "mau mencet lanjut harus dipencet spacenya biar ga kepencet 3
             * button navigasi". Itu bukan tombolnya salah tempat. Itu tombolnya
             * ADA DI BAWAH tombol navigasi Android-mu.
             *
             * PELAJARAN KETIGA HARI INI, dan ini konsep Android murni yang wajib
             * kamu pegang seumur hidup ngoding Android:
             *
             * `enableEdgeToEdge()` di MainActivity.kt menyuruh Android
             * menggambar app kita SAMPAI KE UJUNG kaca -- ke belakang bilah
             * status di atas dan ke belakang bilah navigasi di bawah. Itu memang
             * arah resmi Android 15+ (HP-mu Android 16, jadi ini bukan pilihan
             * lagi), dan itu yang bikin app terlihat modern.
             *
             * Tapi "menggambar sampai ujung" itu izin, BUKAN penanganan. Yang
             * harus kita lakukan sendiri: memberi jarak pada benda yang bisa
             * DISENTUH, supaya dia tidak berada di wilayah yang jarimu
             * sebenarnya sedang mengarah ke tombol sistem.
             *
             * Kenapa Scaffold tidak mengurusnya padahal isi layar aman? Karena
             * Scaffold memang menghitung inset untuk AREA ISI (itu `innerPadding`
             * di bawah), tapi slot `bottomBar` sengaja diserahkan ke kita.
             * Alasannya: NavigationBar M3 mengurus insetnya sendiri di dalam, dan
             * kalau Scaffold ikut menambah, jaraknya jadi dobel. Konsekuensinya,
             * bilah bawah buatan sendiri seperti Column ini TIDAK dapat apa-apa.
             *
             * `navigationBarsPadding()` = "sisakan ruang setinggi bilah navigasi".
             * Di HP dengan gesture bar nilainya kecil (~24dp), di HP-mu dengan
             * 3 tombol nilainya ~48dp. Satu baris ini benar di dua-duanya, dan
             * itulah kenapa kita tidak boleh menuliskan angka dp sendiri.
             *
             * imePadding = kalau keyboard muncul, baris tombol ini naik ke atas
             * keyboard, tidak ketimbun. Tanpa satu baris ini, kamu mengetik
             * instruksi lalu bingung tombol Simpan-nya ke mana.
             *
             * ================ UTANG YANG SAYA BAYAR HARI INI ================
             *
             * Di tempat ini semalam saya menulis dua modifier bertumpuk
             * (`navigationBarsPadding()` lalu `imePadding()`) dan mengaku terus
             * terang bahwa saat keyboard terbuka ada ~48dp ruang ekstra yang
             * kebuang, plus janji: "cara betulnya pakai
             * `WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)`, dan itu
             * kita pasang NANTI setelah build ini hijau." Build-nya hijau. Ini
             * penagihannya.
             *
             * Kenapa dua modifier itu bertumpuk: keduanya menjawab pertanyaan
             * yang sama ("berapa jarak dari dasar?") lalu HASILNYA DIJUMLAHKAN.
             * Padahal keyboard yang muncul sudah menutupi wilayah bilah navigasi
             * -- jaraknya harusnya diambil yang TERBESAR, bukan ditotal.
             *
             * `safeDrawing` itu satu inset yang sudah menggabungkan (union)
             * bilah sistem + lubang kamera + keyboard, jadi "ambil yang terbesar"
             * sudah terjadi di dalam sana. `.only(WindowInsetsSides.Bottom)`
             * membuang sisi kiri/kanan/atas -- tanpa dia, baris tombol ini juga
             * dapat jarak dari bilah status di atas, dan itu jelas ngawur.
             *
             * Perhatikan juga yang saya pakai: `windowInsetsPadding(...)`, bukan
             * `padding(...)`. Bedanya nyata: `windowInsetsPadding` MENANDAI inset
             * itu sudah dipakai, jadi benda lain di dalam tidak menghitungnya
             * lagi. Itu jaring pengaman yang membuat kesalahan dobel-hitung tadi
             * mustahil terulang di dalam sini.
             *
             * Dan ini yang bikin saya berani sekarang padahal semalam menolak:
             * keempat API ini STABIL di Compose foundation 1.7.0 -- nol @OptIn,
             * nol risiko build merah. Yang saya hindari semalam bukan API-nya,
             * tapi mempertaruhkan satu build yang sedang merah untuk sesuatu yang
             * belum saya pastikan. Urutan kerja itu yang mau saya tularkan:
             * benerin merahnya dulu, rapikan sesudahnya.
             */
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(16.dp),
            ) {
                // Keterangan syarat HANYA muncul di langkah 1, dan hanya kalau
                // syaratnya belum terpenuhi. Peringatan yang selalu nongol itu
                // berhenti dibaca setelah hari kedua.
                if (step == 1 && !step1Lengkap) {
                    Text(
                        text = stringResource(R.string.form_hint_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (step > 1) {
                        // weight 1 lawan 2: tombol maju sengaja dibuat dua kali
                        // lebar tombol mundur. Dua tombol sama besar memaksa
                        // mata memilih; ukuran yang beda sudah menjawabnya.
                        OutlinedButton(
                            onClick = { keStep(step - 1) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(R.string.form_back))
                        }
                    }
                    if (step < TOTAL_STEP) {
                        Button(
                            onClick = { keStep(step + 1) },
                            // Gerbangnya cuma di langkah 1. Alat dan otot boleh
                            // dilewati -- gerakan tanpa alat itu sah, dan otot
                            // bisa ditandai belakangan. Memaksa keduanya diisi
                            // cuma bikin kamu memilih chip ngawur biar lolos.
                            enabled = step != 1 || step1Lengkap,
                            modifier = Modifier.weight(2f),
                        ) {
                            Text(text = stringResource(R.string.form_next))
                        }
                    } else {
                        Button(
                            onClick = { simpan() },
                            enabled = step1Lengkap,
                            modifier = Modifier.weight(2f),
                        ) {
                            Text(text = stringResource(R.string.form_save))
                        }
                    }
                }
            }
        },
    ) { innerPadding ->

        /*
         * ===================================================================
         * PELAJARAN PERTAMA HARI INI, dan ini jawaban langsung atas laporanmu
         * "scroll daftar equipment patah-patah".
         *
         * Langkah 2 KELUAR dari scroll wizard. Dia sekarang layar dengan daftar
         * malas (`LazyColumn`) sendiri, bukan lagi penumpang di dalam
         * `Column.verticalScroll` yang kamu lihat di bawah.
         *
         * KENAPA ITU YANG MEMPERBAIKI PATAH-PATAHNYA -- baca angkanya, jangan
         * percaya kata "optimasi":
         *
         * `Column.verticalScroll` itu satu kolom PANJANG yang seluruh isinya
         * hidup sekaligus. Bukan cuma dibuat sekali lalu didiamkan: setiap kali
         * Compose perlu mengukur ulang layar, dia mengukur SEMUA anaknya, dari
         * baris pertama sampai terakhir, termasuk yang jauh di luar pandangan.
         * Sepuluh alat = sepuluh baris + sepuluh foto diukur ulang tiap kali.
         *
         * Sekarang hitung berapa kali "mengukur ulang" itu terjadi saat kamu
         * mengetik nama alat: keyboard MUNCULNYA dianimasikan, dan tiap frame
         * animasi itu mengubah tinggi area isi. Layar 120Hz, animasi keyboard
         * ~300ms = sekitar 36 frame, dan di SETIAP frame seluruh sepuluh baris
         * diukur ulang. Itu persis detik-detik yang kamu tandai merah di
         * rekamanmu (t=33s, t=40s, t=124s -- semuanya saat keyboard naik).
         *
         * `LazyColumn` cuma menyusun dan mengukur yang MASUK LAYAR, kira-kira
         * tiga sampai empat baris. Ongkos yang tadi tumbuh mengikuti jumlah
         * alatmu sekarang berhenti tumbuh; mau 10 alat atau 60, kerjanya sama.
         *
         * ONGKOS YANG SAYA BAYAR SUPAYA KAMU TAHU: daftar malas TIDAK BOLEH
         * ditaruh di dalam kolom yang bisa di-scroll. Dua-duanya sama-sama
         * bertanya "berapa tinggi maksimalku?" dan tidak ada yang menjawab,
         * hasilnya crash "measured with an infinity maximum height". Larangan
         * lama di file ini BENAR dan masih berlaku untuk grid otot di langkah 3
         * yang induknya memang masih scroll. Jalan keluarnya bukan memaksakan
         * keduanya berdampingan, tapi memisahkan: langkah 2 tidak lagi punya
         * induk yang scroll, jadi dia bebas jadi daftar malas.
         *
         * `return@Scaffold` itu pola yang sama yang sudah kamu pakai di
         * `ExerciseCatalogScreen` untuk bertukar layar: keluar lebih awal supaya
         * sisa fungsi ini tidak perlu diapa-apakan sama sekali. Nol baris di
         * langkah 1, 3, dan 4 yang berubah hari ini.
         * ===================================================================
         */
        if (step == 2) {
            StepAlat(
                daftarAlat = daftarAlat,
                terpilih = equipment,
                listState = alatListState,
                tinggi = tinggi,
                onAturKetinggian = { alat -> aturTinggiUntuk = alat },
                /*
                 * DUA PEKERJAAN dalam satu lambda.
                 *
                 * Perhatikan baris pertama: hasilnya dihitung ke `val baru` DULU,
                 * baru dipasang ke `equipment`. Kenapa tidak langsung
                 * `equipment = ...` lalu di bawahnya membaca `equipment` lagi?
                 * Karena `equipment` itu state Compose, dan membacanya kembali di
                 * baris berikutnya masuk wilayah kelabu: penulisan state tidak
                 * dijamin langsung terlihat oleh pembacaan setelahnya. Menghitung
                 * ke val lokal menghapus pertanyaannya sepenuhnya.
                 *
                 * Kalau alat yang baru dicentang ternyata bisa diatur, pemilihnya
                 * langsung dibuka. Ini yang kamu setujui: jangan menyuruh user
                 * mencari tombol untuk pekerjaan yang sudah pasti dia butuhkan.
                 * Kalau ternyata dia tidak mau mengaturnya, Back atau "Pakai ini"
                 * dengan pilihan "Belum diatur" tetap tersedia.
                 *
                 * Yang SENGAJA TIDAK ada di sini: pembersihan `tinggi` saat alat
                 * dilepas. Itu diurus satu aturan tunggal di `LaunchedEffect` di
                 * atas -- alasannya ada di sana, dan pantas dibaca.
                 */
                onToggleAlat = { id ->
                    val baru = if (id in equipment) equipment - id else equipment + id
                    val mencentang = id !in equipment
                    equipment = baru
                    if (mencentang) {
                        val alat = daftarAlat.firstOrNull { it.id == id }
                        if (alat != null && alat.adjustableHeight) aturTinggiUntuk = alat
                    }
                },
                onTambahAlat = { alatDitambah = true },
                onUbahAlat = { alat -> alatDiubah = alat },
                // `padding(innerPadding)` di LUAR LazyColumn, bukan di
                // `contentPadding`-nya: bagian ini menghitung ruang untuk
                // TopAppBar dan bottomBar, dan itu batas wilayah, bukan jarak
                // isi. Jarak isi diatur di dalam `StepAlat` sendiri.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // verticalScroll DULU, baru padding kiri-kanan. Kalau dibalik,
                // area yang bisa di-scroll ikut terpotong 16dp di tiap sisi dan
                // jarimu terasa "kena tembok" di pinggir layar.
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        ) {
            // Perhatikan: TIDAK ADA satu pun `remember` di dalam ketiga fungsi
            // langkah di bawah. Mereka murni penampil -- semua nilai dikirim dari
            // atas, semua perubahan dilaporkan ke atas. Itulah kenapa berpindah
            // langkah tidak menghapus apa pun.
            when (step) {
                1 -> Step1Identitas(
                    name = name,
                    onNameChange = { name = it },
                    thumbnail = thumbnail,
                    copying = copying,
                    copyFailed = copyFailed,
                    onPickPhoto = {
                        // ImageOnly, bukan ImageAndVideo. Ini kolom thumbnail --
                        // membiarkan video terpilih di sini cuma bikin kamu
                        // memilih file 80MB lalu bingung kenapa gambarnya tidak
                        // muncul.
                        pickImage.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    onRemovePhoto = {
                        val lama = thumbnail
                        thumbnail = ""
                        copyFailed = false
                        // Penjaga `fotoAwal` yang ketiga dan terakhir. Melepas
                        // foto di layar TIDAK sama dengan menghapus filenya:
                        // selama barisnya di database masih menyebut nama itu,
                        // filenya harus tetap ada. Yang menghapusnya nanti
                        // `simpan()`, setelah kamu benar-benar menyimpan
                        // keputusan "gerakan ini tanpa foto".
                        if (lama.isNotBlank() && lama != fotoAwal) onDiscardThumbnail(lama)
                    },
                    category = category,
                    onCategoryChange = { category = it },
                    type = type,
                    onTypeChange = { type = it },
                    onFocusKeluar = { focus.clearFocus() },
                )

                // Langkah 2 TIDAK ADA di sini, dan itu sengaja: dia sudah
                // ditangani lebih awal di atas (`if (step == 2) { ... }`) karena
                // butuh jadi daftar malas tanpa induk yang scroll. Jangan
                // kembalikan cabang `2 ->` ke sini "biar rapi satu tempat" --
                // itu memulangkan patah-patahnya.

                3 -> StepOtot(
                    muscles = muscles,
                    onToggleMuscle = {
                        muscles = if (it in muscles) muscles - it else muscles + it
                    },
                )

                else -> Step4Instruksi(
                    instruction = instruction,
                    onInstructionChange = { instruction = it },
                    pakaiLangkah = pakaiLangkah,
                    // Satu ekspresi mengurus DUA arah konversi, dan itu bukan
                    // kebetulan: `pecahLangkah` membuang nomor kalau ada, dan
                    // tidak melakukan apa-apa kalau tidak ada. Jadi kata-kata
                    // yang sudah kamu tulis tidak pernah hilang saat pindah
                    // format -- yang berubah cuma nomornya.
                    onPakaiLangkahChange = { pakai ->
                        pakaiLangkah = pakai
                        // Ganti format = rapikan teks mengikuti pilihan baru.
                        // Logika ini dulu ditulis inline di sini; sekarang dia
                        // tinggal di SATU fungsi (`rapikanInstruksi`) yang juga
                        // dipakai `simpan()`, supaya tombol ganti-format dan
                        // tombol Simpan tak pernah beda pendapat.
                        instruction = rapikanInstruksi(instruction, pakai)
                    },
                    youtube = youtube,
                    onYoutubeChange = { youtube = it },
                    name = name,
                    category = category,
                    type = type,
                    // Nama alatnya disebut, bukan cuma jumlahnya -- dan ini
                    // BEDA dengan otot di baris bawahnya, jadi baca kenapa.
                    // Alat yang kamu punya jumlahnya belasan dan namanya pendek
                    // ("Matras, Handuk"), jadi menyebutnya menjawab pertanyaan
                    // "tadi aku centang yang mana ya?" dalam satu kedipan mata.
                    // Otot bisa 30 dan namanya panjang -- menyebutnya di sini
                    // cuma memindahkan tembok chip yang baru kita bongkar.
                    // Aturannya: sebut isinya kalau pendek, sebut jumlahnya
                    // kalau panjang. Bukan "selalu ringkas" atau "selalu detail".
                    //
                    // `filter` jalan atas `daftarAlat`, jadi urutannya ikut
                    // urutan nama dari database, bukan urutan kamu menekan.
                    ringkasanAlat = daftarAlat
                        .filter { it.id in equipment }
                        .joinToString(", ") { it.name },
                    tinggi = tinggi,
                    jumlahOtot = muscles.size,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    /*
     * ==========================================================================
     *  DUA DIALOG ALAT, DIPASANG DI LUAR SCAFFOLD.
     * ==========================================================================
     *
     * Kenapa di luar, bukan di dalam Column yang bisa di-scroll? Karena dialog di
     * Compose itu JENDELA SENDIRI, bukan benda di dalam layar -- dia tidak
     * menempati satu piksel pun di layout induknya. Menaruhnya di dalam Column
     * yang bisa digulir tetap jalan, tapi menyesatkan orang yang membaca kodenya:
     * dia akan mencarinya di antara isi layar. Pola ini sama dengan dialog hapus
     * massal di `ExerciseCatalogScreen` -- setelah Scaffold, sebelum tutup fungsi.
     *
     * Syarat `alatDitambah || alatDiubah != null` itu yang membuat SATU komposabel
     * `DialogAlat` melayani dua mode. Yang membedakannya cuma nilai `awal`:
     * null = tambah, terisi = ubah.
     */
    if (alatDitambah || alatDiubah != null) {
        DialogAlat(
            awal = alatDiubah,
            onTutup = {
                alatDiubah = null
                alatDitambah = false
            },
            onSimpan = { nama, namaFoto, bisaDiatur ->
                // Disalin ke val lokal SEBELUM state-nya dikosongkan. Kalau
                // `alatDiubah` dibaca di dalam `scope.launch` di bawah, yang
                // terbaca sudah null -- dan alat yang kamu sunting malah lahir
                // sebagai alat baru. Ini jebakan klasik: lambda membaca nilai
                // TERBARU, bukan nilai saat lambda dibuat.
                val sasaran = alatDiubah
                alatDiubah = null
                alatDitambah = false

                scope.launch {
                    if (sasaran == null) {
                        // MODE TAMBAH. Inilah alasan `insert` mengembalikan Long,
                        // dan alasan seluruh pengelolaan alat tinggal di dalam
                        // form ini: begitu id-nya kembali, alat baru langsung
                        // dicentang. Kamu tidak perlu mencarinya lagi di daftar.
                        val idBaru = alatDao.insert(
                            Equipment(
                                name = nama,
                                photoFile = namaFoto,
                                adjustableHeight = bisaDiatur,
                            ),
                        )
                        equipment = equipment + idBaru

                        // Alat baru yang bisa diatur langsung dicentang, jadi
                        // pemilih ketinggiannya pantas langsung terbuka -- sama
                        // perlakuannya dengan mencentang alat yang sudah ada.
                        // Objectnya dirakit di sini dengan `id = idBaru`, bukan
                        // dicari lagi di `daftarAlat`: Flow database belum tentu
                        // sudah menyusulkan barisnya di milidetik ini, dan
                        // mencari benda yang belum sampai selalu berujung null.
                        if (bisaDiatur) {
                            aturTinggiUntuk = Equipment(
                                id = idBaru,
                                name = nama,
                                photoFile = namaFoto,
                                adjustableHeight = true,
                            )
                        }
                    } else {
                        // MODE UBAH. Foto LAMA milik baris database dibuang di
                        // sini, bukan di dalam dialog -- dialog tidak tahu
                        // simpanannya berhasil atau user membatalkannya. Yang
                        // tahu itu yang memanggil. Baca lagi doc `DialogAlat`.
                        if (sasaran.photoFile.isNotBlank() &&
                            sasaran.photoFile != namaFoto
                        ) {
                            onDiscardThumbnail(sasaran.photoFile)
                        }
                        // `copy` MENERUSKAN semua kolom yang dialog tidak sunting
                        // -- di sini `id`-nya. Aturan yang sama dengan
                        // `videoFile = awal?.videoFile ?: ""` di `simpan()`, cuma
                        // di sini Kotlin yang mengerjakannya untuk kita.
                        alatDao.update(
                            sasaran.copy(
                                name = nama,
                                photoFile = namaFoto,
                                adjustableHeight = bisaDiatur,
                            ),
                        )
                    }
                }
            },
            onMintaHapus = {
                val sasaran = alatDiubah
                alatDiubah = null
                alatDitambah = false
                // Diisi PALING AKHIR. Begitu variabel ini berisi, LaunchedEffect
                // di atas berangkat menghitung pemakainya, dan dialog konfirmasi
                // di bawah muncul.
                alatDihapus = sasaran
            },
            // Form ini sudah punya saluran pembuangan file yang benar (dieksekusi
            // di scope KATALOG, yang hidup lebih lama dari form). Foto alat dan
            // foto gerakan sama-sama file di folder app, jadi salurannya sama --
            // yang berbeda cuma siapa yang memilikinya.
            onBuangFoto = onDiscardThumbnail,
        )
    }

    // Konfirmasi hapus alat. `mauDihapus` disalin ke val lokal supaya Kotlin mau
    // melakukan smart cast dari `Equipment?` jadi `Equipment` -- pola yang sama
    // dengan `val chosen = category` dan `val dibuka = detailId` di katalog.
    val mauDihapus = alatDihapus
    if (mauDihapus != null) {
        val jumlah = jumlahPemakaiAlat
        AlertDialog(
            onDismissRequest = { alatDihapus = null },
            title = {
                Text(text = stringResource(R.string.alat_delete_title, mauDihapus.name))
            },
            text = {
                Text(
                    text = when {
                        // Tiga kalimat berbeda untuk tiga keadaan berbeda, dan
                        // tidak satu pun yang menyebut angka yang belum diketahui.
                        jumlah == null -> stringResource(R.string.alat_delete_counting)
                        jumlah == 0 -> stringResource(R.string.alat_delete_body_unused)
                        else -> stringResource(R.string.alat_delete_body_used, jumlah)
                    },
                )
            },
            confirmButton = {
                TextButton(
                    // Mati selama angkanya belum keluar. Ini bukan kehati-hatian
                    // berlebihan: seluruh guna dialog ini ada di angka itu.
                    enabled = jumlah != null,
                    onClick = {
                        alatDihapus = null

                        // Tanda centangnya dilepas duluan, di thread ini, supaya
                        // barisnya langsung hilang dari hitungan "N dari M
                        // dipilih" tanpa menunggu database.
                        equipment = equipment - mauDihapus.id

                        scope.launch { alatDao.delete(mauDihapus) }
                        if (mauDihapus.photoFile.isNotBlank()) {
                            onDiscardThumbnail(mauDihapus.photoFile)
                        }

                        // PERHATIKAN APA YANG TIDAK SAYA LAKUKAN DI SINI, dan ini
                        // keputusan yang paling layak kamu ingat dari build ini:
                        // saya TIDAK menyisir tabel `exercises` untuk mencabut id
                        // ini dari gerakan-gerakan lain. Id yatim dibiarkan tinggal
                        // di sana, dan layar yang membuangnya saat menggambar
                        // (`mapNotNull { peta[it] }`).
                        //
                        // Alasannya: menulis ulang puluhan baris gerakan cuma
                        // karena satu alat dihapus itu operasi berisiko yang
                        // hasilnya tidak bisa dibatalkan. Membiarkan angka yatim
                        // itu murah, tidak terlihat oleh user, dan berkat
                        // AUTOINCREMENT (baca AppDatabase.kt) angka itu tidak akan
                        // pernah didaur ulang jadi alat lain. Jadi tidak ada
                        // gerakan yang mendadak "butuh Ring" karena kamu menghapus
                        // matras.
                    },
                ) {
                    Text(
                        text = stringResource(R.string.dialog_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { alatDihapus = null }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

/**
 * Penunjuk langkah: tiga garis di atas, nomor dan judul di bawahnya.
 *
 * Kenapa tiga garis, bukan tulisan "1/3" saja? Karena garis menjawab pertanyaan
 * yang tidak kamu ucapkan: "masih jauh?" Tiga batang dengan satu menyala itu
 * dibaca mata dalam sepersepuluh detik, tanpa membaca angka. Progress bar bulat
 * yang berputar justru tidak menjawab apa pun -- dia cuma bilang "sabar".
 *
 * Perhatikan `if (i < step)`, bukan `i == step`: langkah yang SUDAH dilewati
 * tetap menyala. Jadi garisnya bertambah panjang, bukan berpindah -- dan rasa
 * "sudah sejauh ini" itu yang bikin orang menyelesaikan form.
 */
@Composable
private fun StepHeader(step: Int, judul: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(TOTAL_STEP) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        // clip DULU, baru background. Kalau dibalik, sudut
                        // membulatnya digambar di atas warna yang sudah memenuhi
                        // kotak persegi, dan kamu akan melihat pojok tajam yang
                        // tidak kamu minta.
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (i < step) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.form_step_of, step, TOTAL_STEP),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = judul, style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * LANGKAH 1 -- Identitas & Foto.
 *
 * Isinya cuma empat hal: nama, foto, kategori, satuan. Itu SENGAJA sedikit.
 * Langkah pertama sebuah form harus terasa gampang, karena di sinilah orang
 * memutuskan mau lanjut atau menutup app.
 *
 * Perhatikan komposable ini tidak punya `Column` sendiri. Dia dipanggil dari
 * dalam Column yang bisa di-scroll di `ExerciseFormScreen`, dan apa pun yang
 * dia gambar langsung jadi anak Column itu -- jadi tetap tersusun ke bawah.
 * Menambah Column lagi di sini cuma menambah satu lapis layout tanpa guna.
 */
@Composable
private fun Step1Identitas(
    name: String,
    onNameChange: (String) -> Unit,
    thumbnail: String,
    copying: Boolean,
    copyFailed: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    category: ExerciseCategory?,
    onCategoryChange: (ExerciseCategory) -> Unit,
    type: ExerciseType,
    onTypeChange: (ExerciseType) -> Unit,
    onFocusKeluar: () -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(text = stringResource(R.string.form_name)) },
        placeholder = { Text(text = stringResource(R.string.form_name_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    SectionTitle(text = stringResource(R.string.form_thumbnail))
    Text(
        text = stringResource(R.string.form_thumbnail_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(10.dp))
    ThumbnailPicker(
        thumbnail = thumbnail,
        copying = copying,
        copyFailed = copyFailed,
        onPick = onPickPhoto,
        onRemove = onRemovePhoto,
    )

    SectionTitle(text = stringResource(R.string.form_category))
    /*
     * Tujuh kategori, dua kolom. Coba hitung sendiri: tujuh chip berjejer satu
     * baris di layar 6,67 inci itu mustahil, dan satu chip per baris makan tujuh
     * baris. Dua kolom = empat baris. Itu aritmatika, bukan selera.
     *
     * `onFocusKeluar()` dipanggil SEBELUM pilihannya diganti. Alasannya kamu
     * lihat sendiri di rekaman: keyboard tetap terbuka sepanjang kamu memilih
     * chip, karena fokus masih nempel di kolom Nama. Setengah layar hilang.
     * Menekan chip artinya "saya sudah selesai mengetik" -- jadi keyboard-nya
     * yang harus mengerti, bukan kamu yang harus menekan Back dulu.
     */
    ChipGrid(
        items = ExerciseCategory.entries,
        label = { it.label },
        selected = { it == category },
        onToggle = {
            onFocusKeluar()
            onCategoryChange(it)
        },
    )

    SectionTitle(text = stringResource(R.string.form_type))
    Text(
        text = stringResource(R.string.form_type_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(10.dp))
    ChipGrid(
        items = ExerciseType.entries,
        label = { it.label },
        selected = { it == type },
        onToggle = {
            onFocusKeluar()
            onTypeChange(it)
        },
    )
}

/**
 * LANGKAH 2 -- "Apakah gerakan ini membutuhkan alat?"
 *
 * Judul langkah ini SUDAH berbentuk pertanyaan, dan itu permintaanmu sendiri.
 * Konsekuensinya jadi mengikat: layar ini tidak boleh berisi apa pun selain
 * jawaban atas pertanyaan itu. Otot pindah ke langkah 3 karena itu.
 *
 * PELAJARAN HARI INI: KENAPA ALAT DAPAT BARIS, OTOT TETAP DAPAT CHIP.
 *
 * Chip cuma bisa membawa tulisan. Alat sekarang punya foto, dan fotonya itu
 * yang membuatmu mengenali "palang pintu kamar" tanpa membaca satu huruf --
 * gambar dikenali lebih cepat daripada teks. Chip 32dp juga di bawah batas
 * sentuh 48dp, sementara baris alat masih harus sanggup menerima tekan-lama
 * untuk menyunting.
 *
 * Harganya saya sebut jujur: satu baris ~104dp, satu chip ~40dp. Untuk 30 otot
 * baris itu mahal sekali (3120dp, sembilan layar). Untuk alat yang benar-benar
 * kamu punya di rumah -- realistis 3 sampai 10 -- itu satu sampai dua layar.
 * Jadi aturannya bukan "baris lebih bagus dari chip", tapi "baris untuk daftar
 * pendek yang berfoto, chip untuk daftar panjang yang cuma bernama".
 *
 * SEKARANG PAKAI `LazyColumn`. Komentar lama di tempat ini yang melarangnya
 * sudah saya hapus karena SUDAH TIDAK BENAR. Larangannya dulu benar dengan
 * alasan yang benar: daftar malas di dalam `verticalScroll` memang crash dengan
 * "measured with an infinity maximum height". Yang berubah bukan alasannya, tapi
 * keadaannya -- sejak build ini langkah 2 tidak punya induk yang scroll lagi
 * (lihat `if (step == 2)` di `ExerciseFormScreen`), jadi larangan itu tidak
 * berlaku di sini. Grid otot di langkah 3 MASIH punya induk yang scroll, jadi
 * larangan itu masih berlaku di sana -- jangan ikut "dibetulkan".
 *
 * Kenapa harus malas: baca komentar panjang di `if (step == 2)`. Ringkasnya,
 * `forEach` mengukur sepuluh baris + sepuluh foto setiap kali layar diukur
 * ulang, dan keyboard yang naik memicu pengukuran itu puluhan kali per detik.
 */
@Composable
private fun StepAlat(
    daftarAlat: List<Equipment>,
    terpilih: Set<Long>,
    listState: LazyListState,
    tinggi: Int,
    onAturKetinggian: (Equipment) -> Unit,
    onToggleAlat: (Long) -> Unit,
    onTambahAlat: () -> Unit,
    onUbahAlat: (Equipment) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        /*
         * `contentPadding`, BUKAN `Modifier.padding`. Bedanya nyata dan
         * kelihatan: `Modifier.padding` memotong KOTAKNYA, jadi baris yang
         * di-scroll terpotong di garis 24dp dari bawah dan terlihat "terguntung
         * rapi" -- jelek. `contentPadding` cuma menggeser ISINYA, jadi baris
         * tetap boleh lewat sampai tepi layar sambil awal dan akhir daftar tetap
         * punya nafas.
         *
         * 16dp kiri-kanan itu ganti rugi: dulu jarak ini datang dari
         * `.padding(horizontal = 16.dp)` milik Column wizard yang sekarang tidak
         * lagi kita lewati.
         */
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        /*
         * KEPALA DAFTAR. Status + kalimat petunjuk ikut jadi "item", bukan
         * ditaruh di luar daftar, supaya dia IKUT TER-SCROLL ke atas dan
         * memberikan layar penuh ke barisnya. Yang menempel permanen di layar
         * sempit itu ongkos, bukan fasilitas.
         *
         * `key = "kepala"`: kunci wajib STABIL dan UNIK, tidak wajib berupa
         * angka. Teks tetap seperti ini sah karena tidak akan pernah bertabrakan
         * dengan id alat yang tipenya `Long`.
         */
        item(key = "kepala") {
            if (daftarAlat.isEmpty()) {
                /*
                 * GUDANG KOSONG. Ini keadaan yang kamu lihat PERTAMA KALI di
                 * Poco F5, dan itu BUKAN bug: app tidak punya satu pun alat
                 * bawaan. Yang muncul di sini cuma yang kamu daftarkan sendiri.
                 *
                 * Layar kosong yang cuma menulis "kosong" itu jalan buntu. Yang
                 * ini menyebut CONTOH -- palang di rumah, matras, handuk --
                 * supaya kamu langsung paham daftar ini isinya barang milikmu,
                 * bukan katalog toko.
                 */
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.alat_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.alat_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Column {
                    /*
                     * Satu baris status yang menjawab "sudah berapa yang saya
                     * centang?" tanpa kamu perlu menghitung centangnya sendiri.
                     *
                     * Perhatikan WARNANYA ikut berubah, dan itu bukan hiasan:
                     * abu saat belum ada yang dipilih, oranye begitu ada. Nol
                     * alat itu jawaban yang SAH di app ini (artinya badan
                     * sendiri), jadi keadaan itu tidak boleh terlihat seperti
                     * peringatan. Yang berubah warna cuma yang berubah makna.
                     */
                    Text(
                        text = if (terpilih.isEmpty()) {
                            stringResource(R.string.alat_none_selected)
                        } else {
                            stringResource(
                                R.string.alat_selected_of,
                                terpilih.size,
                                daftarAlat.size,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (terpilih.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Kalimat yang mengajari dua gerakan tangan sekaligus:
                    // tekan = pilih, tekan lama = sunting. Tekan-lama itu
                    // gerakan yang TIDAK KELIHATAN, jadi dia wajib ditulis.
                    // Fitur tersembunyi yang tidak pernah diberitahukan sama
                    // saja dengan fitur yang tidak ada.
                    Text(
                        text = stringResource(R.string.alat_step_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        /*
         * INI INTI PERMINTAANMU: `key = { it.id }`, bukan indeks.
         *
         * Kenapa indeks itu salah, dengan contoh yang bisa kamu coba sendiri
         * nanti: kamu punya Matras (indeks 0) dan Palang (indeks 1), Palang
         * kamu centang. Lalu kamu tambah "Handuk" yang secara alfabet masuk di
         * tengah. Dengan kunci indeks, Compose menyimpulkan "isi kotak nomor 1
         * berubah dari Palang jadi Handuk", jadi dia MEMAKAI ULANG baris Palang
         * -- termasuk foto yang sudah didekode di dalamnya -- dan menimpanya
         * dengan data Handuk. Yang terlihat: foto sempat salah orang sekejap.
         *
         * Dengan `it.id`, Compose tahu Palang cuma PINDAH posisi, bukan berubah
         * isi. Barisnya digeser utuh, fotonya tidak didekode ulang sama sekali.
         *
         * Kunci ini juga yang membuat posisi scroll-mu tidak melompat setiap
         * kali kamu menambah alat baru dari dialog.
         */
        items(items = daftarAlat, key = { it.id }) { alat ->
            // Pil ketinggian cuma muncul kalau DUA syarat terpenuhi sekaligus:
            // alatnya diizinkan diatur, DAN alatnya sedang tercentang. Syarat
            // kedua itu yang menjaga daftar tetap tenang -- tanpa dia, gudang
            // berisi lima ring akan memamerkan lima pil "Atur ketinggian" yang
            // tidak satu pun relevan dengan gerakan yang sedang kamu buat.
            //
            // `null` di `ketinggian` bukan "tinggi 0", tapi "baris ini tidak
            // punya urusan dengan ketinggian". Dua keadaan yang beda, dan
            // BarisAlat memang membedakannya: null = pil tidak digambar, BELUM =
            // pil digambar tapi bertuliskan "Atur ketinggian".
            val bolehAtur = alat.adjustableHeight && alat.id in terpilih
            BarisAlat(
                alat = alat,
                terpilih = alat.id in terpilih,
                onKlik = { onToggleAlat(alat.id) },
                onTekanLama = { onUbahAlat(alat) },
                ketinggian = if (bolehAtur) Ketinggian.dari(tinggi) else null,
                onAturKetinggian = { onAturKetinggian(alat) },
            )
        }

        // Tombol tambah ADA DI DUA KEADAAN, gudang kosong maupun terisi, dan
        // selalu di posisi yang sama: paling bawah. Kalau dia ditaruh di atas
        // daftar, dia akan bergeser setiap kali jumlah alatmu berubah. Yang
        // tetap di tempat itu yang dihafal jempol tanpa mata perlu ikut mencari.
        item(key = "tambah") {
            BarisTambahAlat(onKlik = onTambahAlat)
        }
    }
}

/**
 * LANGKAH 3 -- Otot Target.
 *
 * Isinya PERSIS seperti sebelumnya, cuma sekarang dapat layar sendiri. Grid dua
 * kolom dan rak per kelompok tubuh saya pindahkan tanpa mengubah satu baris pun:
 * dua-duanya sudah kamu setujui dan sudah terbukti jalan di Poco F5, dan kode
 * yang sudah terbukti itu lebih berharga daripada kode yang lebih pintar tapi
 * belum pernah dijalankan.
 *
 * Kenapa grid dua kolom dan bukan chip mengalir: 30 chip yang mengalir dengan
 * lebar berbeda-beda memaksa mata mencari ujung setiap baris. Dua kolom rata
 * memberi mata satu jalur lurus ke bawah, dan jumlah baris yang harus dipindai
 * turun jadi 15.
 *
 * Yang TIDAK saya lakukan: memakai LazyVerticalGrid. Grid malas di dalam Column
 * yang bisa di-scroll itu crash -- "measured with an infinity maximum height",
 * karena dua-duanya sama-sama bertanya "berapa tinggi maksimalku?" dan tidak ada
 * yang menjawab. Grid di sini dibuat tangan: `chunked(2)` jadi baris, tiap chip
 * `weight(1f)`. Sederhana, dan tidak bisa crash.
 *
 * Tidak ada `Spacer` pembuka di sini, dan itu sengaja: `SectionTitle` sudah
 * membawa `padding(top = 22.dp)` sendiri. Menumpuk jarak di atas jarak itu cara
 * paling gampang bikin satu layar terasa "melorot" tanpa ada yang bisa
 * menunjuk penyebabnya.
 */
@Composable
private fun StepOtot(
    muscles: Set<Muscle>,
    onToggleMuscle: (Muscle) -> Unit,
) {
    SectionTitle(text = stringResource(R.string.form_muscles))
    Text(
        text = stringResource(R.string.form_muscles_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))

    /*
     * `BodyRegion.entries.forEach` -- rak otomatis. Perhatikan: kalau besok kamu
     * menambah satu otot di Muscle.kt, rak yang benar akan menampungnya sendiri
     * tanpa satu baris pun disunting di sini. Itu bedanya menyusun layar dari
     * DATA (enum) dan menyusun layar dari daftar chip yang ditulis tangan.
     *
     * DIUBAH 5 Sept 2026 setelah kamu bilang layar ini "berantakan sampai bikin
     * pusing". Yang berubah bukan datanya -- rak-raknya sudah benar dari dulu --
     * tapi CARA raknya dibatasi. Sebelum ini setiap rak cuma diberi judul teks
     * oranye, lalu chipnya langsung mengalir ke bawah tanpa dinding apa pun.
     *
     * PELAJARAN KEEMPAT HARI INI, soal kenapa itu terasa berantakan padahal
     * urutannya rapi: mata mengelompokkan benda dari KEDEKATAN dan LATAR, bukan
     * dari judul. Kalau 30 chip berbaris dengan jarak 8dp yang seragam dari atas
     * ke bawah, lima judul di antaranya tidak cukup kuat untuk memecahnya --
     * yang terlihat tetap satu dinding panjang berisi 30 benda. Otak membaca
     * jarak sebelum membaca huruf.
     *
     * Yang dilakukan `KartuSeksi`: memberi tiap rak LATAR sendiri yang sedikit
     * lebih terang dari layar (surfaceContainer, lihat Color.kt). Sekarang yang
     * dilihat matamu bukan 30 chip, tapi 6 kotak. Enam benda itu jumlah yang
     * bisa dipindai sekali lihat; 30 tidak. Isi kotaknya tidak berkurang satu
     * pun -- yang berkurang cuma beban memindainya.
     */
    BodyRegion.entries.forEach { region ->
        val ototRegion = Muscle.entries.filter { it.region == region }
        KartuSeksi(
            judul = region.label,
            terpilih = ototRegion.count { it in muscles },
            total = ototRegion.size,
        ) {
            ChipGrid(
                items = ototRegion,
                label = { it.label },
                selected = { it in muscles },
                onToggle = onToggleMuscle,
            )
        }
    }
}

/**
 * Satu kotak seksi: judul, penghitung, keterangan opsional, lalu isinya.
 *
 * Kenapa `Surface` dan bukan `Card`? Keduanya bisa. `Card` itu `Surface` yang
 * sudah dipakaikan bayangan, dan bayangan di atas latar hitam pekat tidak
 * terlihat sama sekali -- jadi kita cuma membayar komponen yang lebih rumit
 * untuk hasil yang identik. Yang membedakan kotak dari latar di mode gelap itu
 * WARNA, bukan bayangan.
 *
 * Angka "2/6" di kanan judul itu sengaja, dan bukan hiasan: satu-satunya cara
 * kamu tahu rak "Punggung" masih menyimpan pilihan yang kamu buat lima menit
 * lalu adalah dengan membuka dan memeriksanya. Penghitung menjawab pertanyaan
 * itu tanpa kamu perlu menggeser layar. Bentuknya angka murni ("2/6"), jadi
 * tidak perlu masuk strings.xml -- tidak ada kata yang bisa salah bahasa.
 *
 * PADDING-nya saya pisah: 16dp kiri-kanan, 14dp atas-bawah. Bukan 16 semua.
 * Aturan praktis Material 3 yang layak kamu bawa ke layar lain: ruang HORIZONTAL
 * boleh lebih longgar dari vertikal, karena tinggi layar itu barang langka
 * (kamu menggeser untuk mendapatkannya) sementara lebar layar gratis.
 */
@Composable
private fun KartuSeksi(
    judul: String,
    terpilih: Int,
    total: Int,
    keterangan: String? = null,
    isi: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = judul,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$terpilih/$total",
                    style = MaterialTheme.typography.labelMedium,
                    // Angkanya menyala oranye HANYA kalau ada isinya. Rak kosong
                    // tidak perlu menarik perhatian; rak terisi perlu.
                    color = if (terpilih > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (keterangan != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = keterangan,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            isi()
        }
    }
}

/**
 * LANGKAH 4 -- Instruksi, Video, dan RINGKASAN.
 *
 * Kartu ringkasan di bawah itu bukan hiasan, dan ini alasan yang layak kamu
 * ingat untuk semua wizard yang kamu bikin nanti: wizard punya satu penyakit
 * bawaan -- orang lupa apa yang dia isi tiga langkah lalu. Di langkah 4 dia
 * memegang tombol Simpan tapi kolom Nama sudah tidak kelihatan sejak tadi.
 * Menaruh ringkasan tepat di atas tombol Simpan menutup lubang itu tanpa
 * memaksanya menekan Kembali tiga kali untuk memastikan.
 *
 * PELAJARAN HARI INI, dan ini yang berubah di build ini: ALAT SEKARANG DISEBUT
 * NAMANYA, OTOT MASIH DISEBUT JUMLAHNYA. Kelihatan tidak konsisten, dan memang
 * tidak -- tapi bukan karena malas.
 *
 * Ringkasan itu untuk MEMERIKSA, dan yang bisa diperiksa cuma yang bisa dibaca
 * sekali lihat. "Matras, Handuk" itu dua kata; kamu tahu benar-salahnya tanpa
 * berpikir. Tiga puluh nama otot yang disambung koma jadi satu paragraf yang
 * justru tidak akan kamu baca, dan ringkasan yang tidak dibaca sama saja dengan
 * ringkasan yang tidak ada. Untuk itu "7 dipilih" lebih jujur.
 *
 * Aturannya jadi: SEBUT ISINYA KALAU PENDEK, SEBUT JUMLAHNYA KALAU PANJANG.
 * Batas praktisnya kira-kira satu baris layar.
 *
 * Perhatikan juga tipe parameternya: `ringkasanAlat: String`, bukan
 * `List<Equipment>`. Yang merangkai teksnya si pemanggil, karena dia yang punya
 * peta id-ke-alat. Fungsi ini tetap penyaji murni yang tidak tahu apa itu Room.
 */
@Composable
private fun Step4Instruksi(
    instruction: String,
    onInstructionChange: (String) -> Unit,
    pakaiLangkah: Boolean,
    onPakaiLangkahChange: (Boolean) -> Unit,
    youtube: String,
    onYoutubeChange: (String) -> Unit,
    name: String,
    category: ExerciseCategory?,
    type: ExerciseType,
    ringkasanAlat: String,
    tinggi: Int,
    jumlahOtot: Int,
) {
    // Judulnya pindah ke luar kolom ketikan, karena di mode langkah bernomor
    // tidak ada satu kolom besar yang bisa memasang label "Cara melakukan".
    Text(
        text = stringResource(R.string.form_instruction),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PilihanFormat(
            teks = stringResource(R.string.form_format_steps),
            dipilih = pakaiLangkah,
            onKlik = { onPakaiLangkahChange(true) },
            modifier = Modifier.weight(1f),
        )
        PilihanFormat(
            teks = stringResource(R.string.form_format_paragraph),
            dipilih = !pakaiLangkah,
            onKlik = { onPakaiLangkahChange(false) },
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))

    if (pakaiLangkah) {
        DaftarLangkah(instruction = instruction, onInstructionChange = onInstructionChange)
    } else {
        OutlinedTextField(
            value = instruction,
            onValueChange = onInstructionChange,
            placeholder = { Text(text = stringResource(R.string.form_instruction_hint)) },
            // minLines 5: kolom instruksi yang tingginya satu baris itu bohong
            // soal niatnya. Bentuk kolomnya sendiri yang harus bilang "di sini
            // tempatnya beberapa baris", tanpa perlu tulisan tambahan.
            minLines = 5,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = youtube,
        onValueChange = onYoutubeChange,
        label = { Text(text = stringResource(R.string.form_youtube)) },
        placeholder = { Text(text = stringResource(R.string.form_youtube_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    SectionTitle(text = stringResource(R.string.form_review))
    RingkasanKartu(
        name = name,
        category = category,
        type = type,
        ringkasanAlat = ringkasanAlat,
        tinggi = tinggi,
        jumlahOtot = jumlahOtot,
    )
}

/** Satu chip pemilih format. Dua-duanya tidak pernah mati bersamaan. */
@Composable
private fun PilihanFormat(
    teks: String,
    dipilih: Boolean,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = dipilih,
        onClick = onKlik,
        label = {
            Text(
                text = teks,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = modifier,
    )
}

/**
 * Mode langkah bernomor: satu kolom ketikan per langkah, plus tombol tambah.
 *
 * Perhatikan dia TIDAK menyimpan `List<String>` sendiri. Daftarnya dipecah dari
 * `instruction` setiap kali digambar, dan setiap ketikan dirakit kembali jadi
 * satu String. Satu sumber kebenaran, jadi mustahil ada keadaan "yang di layar
 * beda dengan yang akan disimpan". `pecahLangkah(gabungLangkah(x)) == x` persis,
 * dan itu syarat mutlak supaya kursor tidak melompat ke ujung saat mengetik.
 */
@Composable
private fun DaftarLangkah(
    instruction: String,
    onInstructionChange: (String) -> Unit,
) {
    // ifEmpty: teks kosong tetap harus punya satu kolom untuk ditulisi.
    val langkah = pecahLangkah(instruction).ifEmpty { listOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        langkah.forEachIndexed { i, teks ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${i + 1}.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(26.dp),
                )
                OutlinedTextField(
                    value = teks,
                    onValueChange = { baru ->
                        // AUTO-SPLIT PASTE: kalau teks yang masuk mengandung
                        // newline (paste multi-baris dari catatan/web), pecah
                        // jadi beberapa langkah TERPISAH lalu sisipkan di posisi
                        // kotak ini -- jangan biarkan menumpuk di satu kotak
                        // sebagai baris-baris tak bernomor yang nanti terbaca
                        // "paragraf". Ketikan biasa (tanpa newline) lewat jalur
                        // lama supaya ringan dan kursor tidak melompat.
                        if (baru.contains('\n')) {
                            val potongan = baru.split("\n")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            val diperbarui = langkah.toMutableList().also {
                                it.removeAt(i)
                                it.addAll(i, potongan.ifEmpty { listOf("") })
                            }
                            onInstructionChange(gabungLangkah(diperbarui))
                        } else {
                            onInstructionChange(
                                gabungLangkah(langkah.toMutableList().also { it[i] = baru }),
                            )
                        }
                    },
                    placeholder = { Text(text = stringResource(R.string.form_step_placeholder)) },
                    // singleLine WAJIB di sini. Satu Enter di dalam kolom ini
                    // akan menambah baris baru TANPA nomor, dan teks yang
                    // barisnya tidak bernomor otomatis dianggap paragraf saat
                    // dibuka lagi. Formatmu bisa berubah sendiri gara-gara satu
                    // tombol Enter.
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        onInstructionChange(
                            gabungLangkah(langkah.filterIndexed { indeks, _ -> indeks != i }),
                        )
                    },
                    // Langkah terakhir yang sudah kosong tidak ada gunanya
                    // dihapus, dan tombol yang tidak mengubah apa pun lebih baik
                    // dimatikan daripada membuat orang menebak.
                    enabled = langkah.size > 1 || teks.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.form_step_remove),
                    )
                }
            }
        }

        TextButton(onClick = { onInstructionChange(gabungLangkah(langkah + "")) }) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.form_step_add))
        }
    }
}

/** Kartu ringkasan sebelum Simpan. Baca alasannya di Step4Instruksi. */
@Composable
private fun RingkasanKartu(
    name: String,
    category: ExerciseCategory?,
    type: ExerciseType,
    ringkasanAlat: String,
    tinggi: Int,
    jumlahOtot: Int,
) {
    val kosong = stringResource(R.string.form_none)

    // Kalimat "tanpa alat" diambil DI ATAS, bukan di dalam `ifBlank` di bawah.
    // Alasannya sama dengan `kosong` di baris atasnya: `stringResource` itu fungsi
    // @Composable, dan memanggilnya di dalam lambda milik fungsi lain (walau
    // inline) itu wilayah yang tidak perlu kita uji keberuntungannya. Ambil dulu,
    // pakai belakangan -- lebih membosankan, dan membosankan itu bagus.
    val tanpaAlat = stringResource(R.string.detail_no_equipment)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RingkasanBaris(
                label = stringResource(R.string.form_name),
                // ifBlank, BUKAN isEmpty: nama berisi satu spasi itu tetap kosong
                // bagi manusia, dan `simpan()` memang men-trim-nya nanti.
                nilai = name.ifBlank { kosong },
            )
            RingkasanBaris(
                label = stringResource(R.string.form_category),
                nilai = category?.label ?: kosong,
            )
            RingkasanBaris(
                label = stringResource(R.string.form_type),
                nilai = type.label,
            )
            RingkasanBaris(
                label = stringResource(R.string.form_equipment),
                // `ifBlank` sekali lagi, dan di sini dia yang menerjemahkan
                // "tidak ada alat dipilih" jadi kalimat manusia. Perhatikan
                // kalimatnya bukan "Belum ada" seperti baris lain, tapi "Tanpa
                // alat -- cukup badan sendiri": nol alat itu JAWABAN, bukan
                // kolom yang belum diisi. Bahasa di layar harus membedakan
                // keduanya, karena databasenya tidak bisa.
                nilai = ringkasanAlat.ifBlank { tanpaAlat },
            )

            // Baris ini MUNCUL DAN HILANG, tidak cuma berganti isi. Bandingkan
            // dengan lima baris lainnya yang selalu ada: kategori, tipe, alat --
            // semuanya punya jawaban untuk setiap gerakan, termasuk jawaban
            // "belum diisi". Ketinggian tidak. Untuk push-up, pertanyaan
            // "setinggi apa?" bukan pertanyaan yang belum dijawab, tapi
            // pertanyaan yang TIDAK BERLAKU. Baris yang bertuliskan "Ketinggian:
            // belum ada" di gerakan push-up cuma menambah satu hal untuk dibaca
            // dan nol informasi.
            //
            // Aturan yang bisa kamu pakai di seluruh app: yang tidak berlaku
            // DIHILANGKAN, yang berlaku tapi kosong DITULIS kosong.
            if (tinggi != 0) {
                RingkasanBaris(
                    label = stringResource(R.string.form_height),
                    nilai = Ketinggian.dari(tinggi).label,
                )
            }
            RingkasanBaris(
                label = stringResource(R.string.form_muscles),
                nilai = if (jumlahOtot == 0) {
                    kosong
                } else {
                    stringResource(R.string.catalog_selection_count, jumlahOtot)
                },
            )
        }
    }
}

/**
 * Satu baris ringkasan: label kiri, nilai kanan.
 *
 * `weight(1f)` lawan `weight(1.4f)` -- nilainya dapat ruang lebih besar karena
 * dialah yang panjang ("Tanpa alat, cukup badan sendiri."). Label cuma satu-dua
 * kata. Membagi 50:50 di sini bikin nilai terpaksa turun baris padahal label
 * menyisakan ruang kosong separuh.
 */
@Composable
private fun RingkasanBaris(label: String, nilai: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = nilai,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

/**
 * GRID CHIP BUATAN TANGAN -- komponen paling penting di file ini.
 *
 * Satu fungsi ini yang menggantikan tiga tumpukan FlowRow: kategori, alat, dan
 * lima rak otot. Dia GENERIK (`<T>`), jadi dia tidak tahu apa-apa soal otot
 * atau alat: kamu yang memberi tahu cara mengambil labelnya, cara tahu dia
 * terpilih, dan apa yang terjadi saat ditekan. Itulah kenapa dia bisa dipakai
 * tujuh kali tanpa satu pun `if` khusus di dalamnya.
 *
 * CARA KERJA GRID-NYA, dan ini triknya:
 * `chunked(2)` memotong daftar jadi pasangan -- [A,B,C,D,E] jadi
 * [[A,B],[C,D],[E]]. Tiap pasangan jadi satu Row, tiap chip `weight(1f)` supaya
 * lebarnya SAMA PERSIS, bukan selebar tulisannya. Baris terakhir yang cuma
 * berisi satu chip diberi `Spacer(weight(1f))` sebagai pengganti chip kedua,
 * supaya chip terakhir tidak melebar dua kali lipat dan merusak barisan.
 *
 * Kenapa lebar sama itu penting: mata membaca grid dengan cara mengunci tepi
 * kiri kolom. Chip selebar tulisannya membuat tepi kanan bergerigi dan tepi
 * kolom kedua pindah-pindah tiap baris -- itu yang bikin 30 chip terasa seperti
 * 60.
 */
@Composable
private fun <T> ChipGrid(
    items: List<T>,
    label: (T) -> String,
    selected: (T) -> Boolean,
    onToggle: (T) -> Unit,
    columns: Int = 2,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(columns).forEach { baris ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                baris.forEach { item ->
                    FilterChip(
                        selected = selected(item),
                        onClick = { onToggle(item) },
                        label = {
                            Text(
                                text = label(item),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - baris.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Judul bagian di dalam satu langkah. Jarak atasnya lebih besar dari bawahnya,
 *  supaya judul terasa MILIK isi di bawahnya, bukan mengapung di tengah. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
    )
}

/**
 * Pemilih foto gerakan.
 *
 * Tiga keadaan yang dia bedakan: sedang menyalin (`copying`), gagal menyalin
 * (`copyFailed`), dan sudah ada foto (`thumbnail` tidak kosong). Yang disimpan
 * di database itu NAMA FILE, bukan path -- jadi alamat lengkapnya selalu
 * dirakit ulang saat mau dipakai (sekarang di dalam `FotoGerakan`). Itu yang
 * bikin foto tidak "hilang" walau folder app berpindah.
 */
@Composable
private fun ThumbnailPicker(
    thumbnail: String,
    copying: Boolean,
    copyFailed: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    if (thumbnail.isNotBlank()) {
        // DIUBAH 5 Sept 2026: dulu di sini AsyncImage dengan kotak 16:10 +
        // ContentScale.Crop. Sekarang satu panggilan ke FotoGerakan, yang
        // memakai aturan "tidak pernah dipotong" yang sama dengan layar detail
        // dan katalog. Yang penting dari perubahan ini bukan kodenya jadi lebih
        // pendek -- tapi kamu sekarang MELIHAT DI FORM apa yang akan tampil di
        // layar detail. Pratinjau yang bohong itu lebih buruk daripada tidak ada
        // pratinjau.
        FotoGerakan(
            namaFile = thumbnail,
            contentDescription = stringResource(R.string.catalog_thumbnail),
            sudut = 14.dp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPick, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.form_thumbnail_change))
            }
            OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.form_thumbnail_remove))
            }
        }
    } else {
        /*
         * Belum ada foto: kotak kosong plus satu tombol.
         *
         * Kotaknya 16:10 -- sekitar 225dp di HP-mu. Sengaja TIDAK saya samakan
         * dengan kotak foto yang akan menggantikannya (3:4, sekitar 480dp),
         * walau dulu komentar di sini bilang sebaliknya. Alasannya berubah
         * karena angkanya berubah: kotak kosong setinggi 480dp akan memakan
         * seluruh Langkah 1 dan mendorong kolom Nama keluar layar, cuma untuk
         * menampilkan tulisan "Belum ada foto".
         *
         * Jadi ya, saat kamu memilih foto, isi di bawahnya akan bergeser sekali.
         * Itu saya terima dengan sadar: pergeseran yang terjadi TEPAT setelah
         * kamu menekan sesuatu itu terbaca sebagai "perintahku diterima", bukan
         * sebagai kedutan. Yang haram itu layar bergeser sendiri saat kamu tidak
         * melakukan apa-apa.
         */
        Surface(
            // surfaceContainerHigh, bukan surfaceVariant. Selisihnya #241F22
            // lawan #3D3C41: yang lama abu-abu terang dan bikin kotak kosong
            // MENARIK PERHATIAN lebih dari isi yang sebenarnya penting.
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = when {
                        copying -> stringResource(R.string.form_thumbnail_working)
                        copyFailed -> stringResource(R.string.form_thumbnail_failed)
                        else -> stringResource(R.string.catalog_no_thumbnail)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (copyFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onPick, enabled = !copying) {
                    Text(text = stringResource(R.string.form_thumbnail_pick))
                }
            }
        }
    }
}
