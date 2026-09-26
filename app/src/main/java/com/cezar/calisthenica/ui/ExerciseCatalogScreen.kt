package com.cezar.calisthenica.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.AppDatabase
import com.cezar.calisthenica.data.fileGambar
import com.cezar.calisthenica.data.hapusFileInternal
import com.cezar.calisthenica.model.Exercise
import com.cezar.calisthenica.model.ExerciseCategory
import kotlinx.coroutines.launch

/**
 * Layar Katalog Gerakan. Ini "kamus" gerakanmu: didaftarkan sekali, dipakai di
 * program mana pun tanpa ditulis ulang.
 *
 * PELAJARAN KEEMPAT HARI INI: MODE KONTEKSTUAL.
 *
 * Kamu minta "select manual satu-satu atau select all", dan itu permintaan yang
 * benar. Tapi perhatikan apa yang TIDAK kita lakukan untuk memenuhinya: kita
 * tidak menambah tombol "Pilih" permanen di pojok, dan tidak menambah checkbox
 * yang selalu nongol di setiap kartu. Dua-duanya akan membebani layar 99% waktu
 * demi kegiatan yang kamu lakukan 1% waktu.
 *
 * Yang kita pakai namanya mode kontekstual, dan aturannya cuma satu: SELAMA
 * TIDAK ADA YANG TERPILIH, MODE ITU TIDAK ADA. Tekan lama satu kartu -> mode
 * lahir, checkbox muncul, TopAppBar berubah warna dan isi, FAB menyingkir.
 * Pilihan terakhir dilepas -> mode mati sendiri, semuanya kembali. Tidak ada
 * tombol "keluar mode" yang harus kamu ingat, karena keadaannya DITURUNKAN dari
 * satu hal saja: `selected.isNotEmpty()`.
 *
 * Ini pola resmi Material Design (dulu namanya Contextual Action Bar), dan
 * kamu sudah memakainya seumur hidup tanpa memberinya nama: tekan lama satu
 * foto di Google Photos, tekan lama satu chat di WhatsApp. Paham sampai sini?
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseCatalogScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val dao = remember(context) { AppDatabase.get(context).exerciseDao() }
    val exercisesFlow = remember(dao) { dao.observeAll() }

    // Perhatikan `initial = null`, bukan `emptyList()`. Bedanya halus tapi nyata
    // di matamu: dengan emptyList, frame PERTAMA setiap kali kamu membuka
    // katalog menampilkan "Katalog masih kosong" -- karena database belum
    // sempat menjawab. Isinya lalu masuk sekejap kemudian dan tulisan itu
    // hilang. Hasilnya kedipan tulisan yang salah, setiap kali. Dengan null
    // kita bisa membedakan "belum dijawab" dari "dijawab, isinya kosong".
    val muatan by exercisesFlow.collectAsState(initial = null)
    val sudahDijawab = muatan != null
    val exercises = muatan ?: emptyList()

    /*
     * ==========================================================================
     *  KAMUS ALAT. Lahir 5 Sept 2026, saat alat berhenti jadi enum.
     * ==========================================================================
     *
     * Baris gerakan di database sekarang cuma menyimpan ANGKA: "6,11". Angka itu
     * tidak bisa dicari, tidak bisa ditampilkan, dan tidak bisa dibaca manusia.
     * Yang menerjemahkannya jadi "Matras, Handuk" ada di tabel lain -- jadi layar
     * ini wajib memegang dua sumber sekaligus dan menjodohkan mereka sendiri.
     *
     * PELAJARAN HARI INI: KENAPA `associateBy` DAN BUKAN `find`.
     *
     * Cara paling gampang menerjemahkan satu id: `alatList.find { it.id == id }`.
     * Itu jalan, dan itu juga jebakan yang tidak kelihatan sampai katalogmu besar.
     * `find` membaca daftarnya dari depan sampai ketemu. Kalau kamu punya 40 alat
     * dan 300 gerakan yang masing-masing memakai 2 alat, satu kali pencarian
     * berarti 300 x 2 x (rata-rata 20 langkah) = 12.000 perbandingan -- DAN itu
     * terjadi ULANG setiap satu huruf kamu ketik di kotak pencarian.
     *
     * `associateBy { it.id }` membangun peta (id -> alat) SEKALI, lalu setiap
     * terjemahan cuma satu lompatan. 12.000 perbandingan jadi 600 lompatan.
     * Ongkosnya: peta itu harus dibangun ulang tiap kali daftar alat berubah --
     * itulah gunanya `remember(alatList)`. Ini pola yang akan kamu pakai terus:
     * kalau satu daftar dipakai untuk MENCARI berulang kali, jadikan dia peta.
     *
     * `initial = emptyList()` di sini, bukan null. Bedanya dengan gerakan di atas:
     * daftar alat tidak pernah menentukan tulisan "katalog kosong", jadi tidak ada
     * kedipan tulisan salah yang perlu dihindari. Frame pertama tanpa nama alat
     * cuma berarti pencarian belum bisa mencocokkan nama alat selama sekitar satu
     * frame -- tidak ada mata yang bisa menangkapnya.
     */
    val alatDao = remember(context) { AppDatabase.get(context).equipmentDao() }
    val alatFlow = remember(alatDao) { alatDao.observeAll() }
    val alatList by alatFlow.collectAsState(initial = emptyList())
    val alatMap = remember(alatList) { alatList.associateBy { it.id } }

    val scope = rememberCoroutineScope()

    // ==========================================================================
    //  SEMUA STATE LAYAR INI BERDIRI DI SATU BLOK, DI ATAS SEMUA `return`.
    //
    //  PELAJARAN KEDUA HARI INI, dan ini jebakan yang hampir masuk ke app-mu.
    //  Sebelum hari ini, `filter`, `query`, dan `selected` ditulis DI BAWAH blok
    //  `if (showForm) { ...; return }`. Itu jalan -- selama cuma ada satu layar
    //  anak dan kamu tidak pernah kembali dari sana sambil berharap sesuatu.
    //
    //  Masalahnya: Compose mengingat isi `remember` berdasarkan URUTAN
    //  pemanggilannya. Sebuah `remember` yang ditulis setelah return bersyarat
    //  ikut LAHIR DAN MATI bersama syarat itu. Jadi begitu ada layar detail:
    //  kamu ketik "lats", buka satu gerakan, kembali -- dan kotak pencarianmu
    //  kosong lagi, chip kategori balik ke "Semua", daftarnya melompat ke atas.
    //  Tidak ada error, tidak ada crash, cuma app yang terasa pelupa. Bug jenis
    //  ini yang paling lama tidak ketemu karena tidak ada yang bisa dibaca.
    //
    //  Aturan yang mulai berlaku di seluruh proyek ini: SATU BLOK STATE DI ATAS,
    //  `return` MENYUSUL DI BAWAH. Paham sampai sini?
    // ==========================================================================
    var showForm by remember { mutableStateOf(false) }

    /*
     * Gerakan yang sedang DISUNTING. Null = tidak ada yang disunting.
     *
     * PELAJARAN PERTAMA HARI INI: kenapa DUA variabel untuk satu form.
     *
     * Godaannya besar untuk memakai `editing` saja dan membuang `showForm`.
     * Tidak bisa, dan alasannya sama persis dengan `alatDitambah` vs `alatDiubah`
     * di dalam form gerakan -- kamu sudah pernah membacanya di sana, sekarang
     * lihat pola yang sama muncul di tempat lain:
     *
     * `null` di `editing` harus bisa mengucapkan DUA hal yang berbeda: "form
     * tertutup" DAN "form terbuka dalam mode tambah, jadi belum ada gerakan yang
     * dipegang". Satu nilai tidak bisa berarti dua keadaan. Titik. Kalau
     * dipaksakan, kamu akan menambal dengan `if` di enam tempat dan tetap salah
     * di satu tempat yang baru kelihatan sebulan kemudian.
     *
     * Aturan praktisnya, hafalkan: kalau `null` sudah punya arti, dia tidak boleh
     * dipinjam untuk arti kedua. Tambah saklar terpisah -- itu satu Boolean,
     * ongkosnya nol.
     */
    var editing by remember { mutableStateOf<Exercise?>(null) }

    // Id gerakan yang detailnya sedang dibuka. Null = tidak ada yang dibuka.
    // Ini SATU-SATUNYA bekal yang dibutuhkan untuk pindah ke layar detail: satu
    // angka. Bukan library navigasi, bukan route berupa teks, bukan back stack.
    var detailId by remember { mutableStateOf<Long?>(null) }
    var filter by remember { mutableStateOf<ExerciseCategory?>(null) }
    var query by remember { mutableStateOf("") }

    // Yang terpilih disimpan sebagai kumpulan ID, BUKAN kumpulan object Exercise.
    //
    // Ini bukan selera. Kalau saya menyimpan object-nya, lalu kamu menyunting
    // nama salah satu gerakan itu, object di dalam Set jadi versi LAMA -- dan
    // `it in selected` mendadak bernilai false karena data class membandingkan
    // seluruh isinya. Kartunya kelihatan tercentang tapi tidak ikut terhapus.
    // Bug yang mustahil kamu duga. Id itu satu-satunya bagian yang tidak pernah
    // berubah, jadi id yang kita pegang.
    var selected by remember { mutableStateOf(emptySet<Long>()) }

    var confirmBulkDelete by remember { mutableStateOf(false) }

    // Posisi scroll daftar ikut diangkat ke sini, dengan alasan yang PERSIS sama.
    // Kalau dibiarkan dibuat sendiri oleh LazyColumn di bawah, dia lahir ulang
    // tiap kali kamu kembali dari detail: kamu scroll ke gerakan ke-14, buka
    // detailnya, kembali, dan mendarat lagi di paling atas daftar. Itu keluhan
    // yang paling cepat muncul dan paling sepele penyebabnya.
    val listState = rememberLazyListState()

    /*
     * DIHAPUS 5 September 2026: `fun hapusSatu(ex: Exercise)`.
     *
     * Dulu fungsi ini dipanggil tombol "Hapus gerakan" di layar detail. Tombolnya
     * sudah jadi "Edit gerakan", jadi fungsi ini tidak punya pemanggil lagi -- dan
     * kode tanpa pemanggil WAJIB ikut dibuang, bukan ditinggal dengan alasan
     * "siapa tahu nanti dipakai". Alasannya bukan kerapian: fungsi yatim tetap
     * ikut di-compile tiap kali kamu build di laptop 8GB itu, dan lebih buruk lagi
     * dia bikin orang yang membaca file ini (termasuk kamu, dua bulan lagi)
     * mengira masih ada jalur hapus satuan yang tersembunyi di suatu tempat.
     *
     * Penghapusan gerakan sekarang punya SATU jalur: tekan lama di daftar -> pilih
     * -> tombol hapus di bilah atas -> `hapusTerpilih()` di bawah. Jalur itu juga
     * yang melayani "hapus lima sekaligus", jadi yang hilang cuma cara keduanya,
     * bukan kemampuannya.
     */

    /*
     * SATU PINTU untuk wizard gerakan, dipakai dua maksud: tambah dan ubah.
     *
     * `sedangUbah` disalin ke val lokal supaya Kotlin mau smart cast dari
     * `Exercise?` jadi `Exercise` -- pola yang sama dengan `val dibuka = detailId`
     * di bawah dan `val chosen = category` di dalam form.
     *
     * Yang saya TIDAK lakukan di sini: membuat layar kedua bernama
     * ExerciseEditScreen. Isinya akan 95% sama dengan wizard yang sudah ada, dan
     * setiap bug yang kamu temukan harus diperbaiki dua kali -- cepat atau lambat
     * satu ketinggalan, lalu mode tambah dan mode ubah berperilaku beda tanpa ada
     * yang sengaja membuatnya beda. Satu layar, satu parameter `awal`, selesai.
     */
    val sedangUbah = editing
    if (showForm || sedangUbah != null) {
        ExerciseFormScreen(
            awal = sedangUbah,
            onCancel = {
                showForm = false
                editing = null
            },
            onSave = { exercise ->
                scope.launch {
                    /*
                     * PELAJARAN KEDUA HARI INI: yang memutuskan simpan-baru vs
                     * timpa itu `id`, BUKAN variabel mode di layar ini.
                     *
                     * Saya bisa saja menulis `if (sedangUbah == null)`. Kelihatan
                     * sama, tapi ada bedanya yang penting: itu bertanya pada
                     * KEADAAN LAYAR, sedangkan `exercise.id` bertanya pada BENDA
                     * YANG MAU DISIMPAN. Kalau suatu hari ada jalur ketiga yang
                     * mengirim gerakan ke sini -- duplikat, impor dari file,
                     * apa pun -- `id` tetap benar dengan sendirinya, sedangkan
                     * variabel mode harus diingat untuk diset. Percaya pada data,
                     * bukan pada state yang bisa lupa.
                     *
                     * 0 itu bukan angka sembarangan: `@PrimaryKey(autoGenerate =
                     * true)` di Exercise.kt membaca 0 sebagai "belum punya nomor,
                     * kamu yang kasih". Jadi baris gerakan yang sudah tersimpan
                     * MUSTAHIL punya id 0, dan pemeriksaan ini tidak bisa keliru.
                     *
                     * `update` mencocokkan baris lewat @PrimaryKey -- itulah
                     * sebabnya `simpan()` di form wajib membawa `id` lamanya.
                     * Kalau id-nya hilang di tengah jalan, `update` tidak
                     * menemukan baris untuk ditimpa dan suntinganmu lenyap TANPA
                     * pesan error apa pun. Room tidak menganggap "tidak ada yang
                     * cocok" sebagai kesalahan.
                     */
                    if (exercise.id == 0L) {
                        dao.insert(exercise)
                    } else {
                        dao.update(exercise)
                    }
                }
                showForm = false
                editing = null
            },
            // Form melapor "foto ini sudah tidak dipakai", saya yang menghapus.
            // Dieksekusi di scope MILIK KATALOG, yang tetap hidup walau form-nya
            // sudah tertutup -- lihat penjelasan di ExerciseFormScreen.
            onDiscardThumbnail = { nama ->
                scope.launch { hapusFileInternal(context, nama) }
            },
        )
        return
    }

    // Layar detail: pintu keluar KEDUA dari fungsi ini, dan bentuknya sengaja
    // dibuat kembar dengan pintu pertama di atas. Dua pintu, satu pola -- begitu
    // kamu paham satu, kamu paham semuanya tanpa perlu membacanya ulang.
    //
    // URUTAN DUA `if` INI PENTING, dan ini bukan kebetulan yang beruntung. Form
    // diperiksa DULU, detail sesudahnya. Akibatnya: saat kamu menekan "Edit
    // gerakan" di layar detail, `detailId` sengaja saya BIARKAN terisi. Wizard
    // menang karena dia diperiksa lebih dulu, lalu begitu kamu menekan Simpan dan
    // `editing` kembali null, `detailId` yang masih terisi itu membuka layar
    // detail lagi -- dengan isi yang sudah diperbarui sendiri lewat `observeById`.
    // Kalau urutannya dibalik, kamu terlempar ke daftar setiap kali menyimpan.
    //
    // `dibuka` disalin ke val lokal supaya Kotlin mau melakukan smart cast dari
    // `Long?` jadi `Long`. Kalau `detailId` dipakai langsung, compiler menolak:
    // dia tidak bisa menjamin isinya masih bukan null di baris berikutnya, karena
    // `detailId` itu `var` yang boleh diubah siapa saja. Ini pola yang sama
    // dengan `val chosen = category` di form gerakan.
    val dibuka = detailId
    if (dibuka != null) {
        ExerciseDetailScreen(
            exerciseId = dibuka,
            onBack = { detailId = null },
            // Layar detail cuma menunjuk "yang ini yang mau diubah". Yang membuka
            // wizard-nya saya, di sini. Pola yang sama seperti onDiscardThumbnail:
            // layar anak melapor, katalog bertindak.
            onEdit = { ex -> editing = ex },
        )
        return
    }

    // Inilah "satu sumber kebenaran" yang saya sebut di atas. Tidak ada variabel
    // `selectionMode` yang bisa lupa dimatikan; dia dihitung, bukan disimpan.
    val selectionMode = selected.isNotEmpty()

    // Back di mode pilih = keluar dari mode, BUKAN kembali ke dasbor. Ini yang
    // diharapkan orang, dan `enabled` bikin BackHandler ini mengalah begitu
    // tidak ada yang terpilih -- supaya Back tetap berfungsi normal.
    BackHandler(enabled = selectionMode) { selected = emptySet() }

    val shown = remember(exercises, filter, query, alatMap) {
        val kata = query.trim()
        exercises.filter { ex ->
            val lolosKategori = filter == null || ex.category == filter
            // Pencarian menyapu TIGA kolom, bukan cuma nama. Alasannya praktis:
            // kamu lebih sering ingat "gerakan yang pakai ring itu apa ya" atau
            // "yang buat lats" daripada ingat nama persisnya.
            //
            // Baris alat sekarang lewat KAMUS dulu, dan perhatikan `== true` di
            // ujungnya. `alatMap[it]` mengembalikan `Equipment?` -- null kalau
            // id-nya yatim (alatnya sudah kamu hapus, tapi angkanya masih
            // tertulis di baris gerakan). `?.` membuat seluruh ekspresi jadi
            // `Boolean?`, dan `if` tidak menerima Boolean?. `== true` yang
            // memutuskan bahwa "tidak diketahui" dihitung sebagai TIDAK COCOK --
            // pilihan yang benar, karena id yatim memang tidak punya nama untuk
            // dicocokkan dengan apa pun.
            //
            // `alatMap` WAJIB masuk daftar kunci `remember` di atas. Kalau tidak,
            // kamu menambah alat lewat form, kembali ke katalog, mengetik namanya
            // -- dan tidak ketemu, karena hasil filter yang lama masih diingat
            // dari kamus yang lama. Bug paling membingungkan justru datang dari
            // cache yang lupa dibilangi kapan harus basi.
            val lolosCari = kata.isBlank() ||
                ex.name.contains(kata, ignoreCase = true) ||
                ex.muscles.any { it.label.contains(kata, ignoreCase = true) } ||
                ex.equipmentIds.any {
                    alatMap[it]?.name?.contains(kata, ignoreCase = true) == true
                }
            lolosKategori && lolosCari
        }
    }

    // "Pilih semua" berarti semua yang SEDANG TERLIHAT, bukan semua isi tabel.
    //
    // Ini keputusan yang sengaja, dan justru inilah yang bikin fiturnya berguna:
    // ketik "lats", tekan Pilih semua, hapus. Kalau tombolnya menyapu seluruh
    // tabel, dia jadi jebakan -- kamu memfilter untuk mempersempit sasaran, lalu
    // app-mu diam-diam melebarkannya lagi.
    val semuaTerlihatTerpilih = shown.isNotEmpty() && shown.all { it.id in selected }
    val idTerlihat = shown.map { it.id }.toSet()

    fun hapusTerpilih() {
        val sasaran = exercises.filter { it.id in selected }
        val idSasaran = sasaran.map { it.id }
        selected = emptySet()
        scope.launch {
            // Barisnya dulu, filenya belakangan. Kalau dibalik dan penghapusan
            // baris gagal, kamu punya baris yang menunjuk foto yang sudah lenyap
            // -- kartu dengan gambar rusak yang tidak bisa diperbaiki dari dalam
            // app. Urutan ini menjamin kegagalan paling buruk cuma menyisakan
            // file nganggur, bukan data yang cacat.
            dao.deleteByIds(idSasaran)
            sasaran.forEach { hapusFileInternal(context, it.thumbnailFile) }
        }
    }
    // Tinggi bilah navigasi sistem, ditanya langsung ke sistem. Sama seperti di
    // dasbor -- lihat MainActivity.kt kalau mau baca alasan panjangnya.
    val jarakBawah = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            // Dua TopAppBar, dipilih satu. Bukan satu TopAppBar yang isinya
            // penuh `if` di setiap sudut -- itu cara tercepat bikin file ini
            // tidak bisa dibaca lagi enam bulan kemudian.
            if (selectionMode) {
                SelectionTopBar(
                    count = selected.size,
                    semuaTerpilih = semuaTerlihatTerpilih,
                    onCancel = { selected = emptySet() },
                    onToggleAll = {
                        selected = if (semuaTerlihatTerpilih) {
                            selected - idTerlihat
                        } else {
                            selected + idTerlihat
                        }
                    },
                    onDelete = { confirmBulkDelete = true },
                )
            } else {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.catalog_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                // AutoMirrored, bukan Icons.Default.ArrowBack.
                                // Ini yang menghilangkan dua baris "w:" di log
                                // build-mu semalam. Gunanya: di bahasa yang
                                // ditulis kanan-ke-kiri (Arab, Ibrani) panah ini
                                // otomatis membalik arah. App-mu memang bahasa
                                // Indonesia, tapi yang lama itu sudah ditandai
                                // deprecated -- artinya suatu hari dia dihapus,
                                // dan saat itu build-mu berhenti. Bereskan
                                // sekarang saat harganya satu baris.
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.catalog_back),
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            // FAB disembunyikan selama memilih. Tombol "Gerakan baru" di tengah
            // kegiatan menghapus itu tidak masuk akal, dan lebih buruk lagi dia
            // menutupi kartu paling bawah yang mungkin mau kamu centang.
            if (!selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { showForm = true },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text(text = stringResource(R.string.catalog_add)) },
                    // Dinaikkan setinggi bilah navigasi, karena insets Scaffold
                    // di bawah sudah dinolkan. Tanpa baris ini FAB-nya duduk
                    // persis di atas tombol Home sistem.
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
        // Kaca dipakai penuh sampai ujung bawah; kartu gerakan lewat di belakang
        // tiga tombol navigasi saat di-scroll. Harganya dibayar di dua tempat:
        // FAB di atas, dan `contentPadding` LazyColumn di bawah. Sisi samping
        // tetap dihormati untuk mode landscape -- lihat MainActivity.kt.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (exercises.isNotEmpty()) {
                SearchField(query = query, onQueryChange = { query = it })
                CategoryFilterRow(selected = filter, onSelect = { filter = it })
                Text(
                    text = if (selectionMode) {
                        stringResource(R.string.catalog_selection_count, selected.size)
                    } else {
                        stringResource(R.string.catalog_count, shown.size)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }
            when {
                // Belum dijawab database -> jangan gambar apa pun. Satu frame
                // kosong tidak akan kamu sadari; satu frame berisi tulisan yang
                // SALAH akan kamu sadari setiap kali.
                !sudahDijawab -> Unit

                exercises.isEmpty() -> CatalogEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                )

                shown.isEmpty() -> Text(
                    text = if (query.isBlank()) {
                        stringResource(R.string.catalog_filter_empty)
                    } else {
                        stringResource(R.string.catalog_search_empty, query.trim())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )

                else -> LazyColumn(
                    // `state = listState` inilah yang menyimpan posisi scroll.
                    // Tanpa baris ini, LazyColumn bikin state-nya sendiri di
                    // dalam dirinya -- dan begitu layar detail muncul, seluruh
                    // LazyColumn ini dibongkar, state-nya ikut mati, dan kamu
                    // balik ke daftar dari puncak. Scroll ke gerakan ke-40,
                    // buka detailnya, tekan Back, mulai dari nol lagi.
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        // 96dp untuk FAB + setinggi bilah navigasi. Ini yang
                        // menjamin kartu TERAKHIR tetap bisa dibaca utuh saat
                        // scroll-nya sudah mentok, walaupun kartu-kartu di
                        // atasnya bebas lewat di belakang bilah.
                        bottom = 96.dp + jarakBawah,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Petunjuk tekan-lama ditaruh SEBAGAI ITEM PERTAMA di dalam
                    // daftar, dan hanya saat belum ada yang terpilih. Jadi dia
                    // ikut ter-scroll dan hilang dari pandangan begitu kamu
                    // mulai membaca daftarnya -- bukan tulisan permanen yang
                    // memakan tempat selamanya untuk sesuatu yang cuma perlu
                    // kamu tahu sekali.
                    if (!selectionMode) {
                        item(key = "hint") {
                            Text(
                                text = stringResource(R.string.catalog_hint_longpress),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }

                    items(items = shown, key = { it.id }) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            selectionMode = selectionMode,
                            isSelected = exercise.id in selected,
                            onToggleSelect = {
                                selected = if (exercise.id in selected) {
                                    selected - exercise.id
                                } else {
                                    selected + exercise.id
                                }
                            },
                            // Tekan kartu = BUKA detailnya. Kartu ini tidak lagi
                            // punya tombol hapus sendiri: penghapusan sekarang
                            // hidup di layar detail (satu gerakan) dan di app bar
                            // mode pilih (banyak gerakan sekaligus).
                            //
                            // Kenapa dipindah: tombol hapus di setiap kartu itu
                            // ranjau. Dia duduk beberapa milimeter dari area yang
                            // kamu tekan puluhan kali sehari, dan yang dia lakukan
                            // permanen. Di layar detail, dia berada paling bawah,
                            // setelah kamu jelas-jelas melihat gerakan mana yang
                            // sedang kamu buka.
                            onOpen = { detailId = exercise.id },
                        )
                    }
                }
            }
        }
    }

    if (confirmBulkDelete) {
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            title = { Text(text = stringResource(R.string.dialog_delete_many_title, selected.size)) },
            text = { Text(text = stringResource(R.string.dialog_delete_many_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmBulkDelete = false
                        hapusTerpilih()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.dialog_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkDelete = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}
/**
 * TopAppBar versi "sedang memilih".
 *
 * Warnanya sengaja DIGANTI, bukan cuma isinya. Ini bukan dekorasi: perubahan
 * warna sepanjang layar adalah cara tercepat memberitahu mata bahwa aturan
 * layarnya sekarang berbeda -- tekan kartu sekarang berarti mencentang, bukan
 * membuka. Kalau warnanya sama, kamu akan menekan kartu dengan harapan lama dan
 * merasa app-nya salah.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    semuaTerpilih: Boolean,
    onCancel: () -> Unit,
    onToggleAll: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = stringResource(R.string.catalog_selection_count, count)) },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.catalog_selection_cancel),
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleAll) {
                Icon(
                    imageVector = Icons.Default.Done,
                    // Ikonnya sama, artinya berbalik tergantung keadaan. Jadi
                    // keterangan suaranya HARUS ikut berbalik -- kalau tidak,
                    // pengguna TalkBack diberitahu "Pilih semua" padahal
                    // menekannya justru membersihkan pilihan.
                    contentDescription = stringResource(
                        if (semuaTerpilih) R.string.catalog_select_none
                        else R.string.catalog_select_all,
                    ),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.catalog_delete_selected),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}
/**
 * Kotak pencarian.
 *
 * KENAPA `OutlinedTextField` dan bukan `SearchBar` milik Material 3?
 * M3 punya `SearchBar` asli yang bisa mengembang jadi layar penuh berisi
 * saran -- dan tampilannya memang lebih keren. Masalahnya dia
 * `@ExperimentalMaterial3Api` dan kontrak parameternya (`expanded`,
 * `onExpandedChange`, `inputField`) berubah bentuk antar versi library. Saya
 * tidak bisa menjalankan build dari sisi saya untuk membuktikan tebakan saya
 * benar, jadi saya pilih yang pasti kompilasi. Kalau nanti kamu mau versi yang
 * mengembang itu, kita tukar SATU composable ini saja -- sisanya tidak
 * tersentuh, karena dia cuma menerima `query` dan melapor lewat
 * `onQueryChange`. Deal?
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = stringResource(R.string.catalog_search)) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.catalog_search_clear),
                    )
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
    )
}

/**
 * PELAJARAN SESI LALU: FilterChip.
 *
 * `selected` bukan disimpan di dalam chip-nya. Chip cuma DIBERI TAHU dia sedang
 * terpilih atau tidak, dan cuma MELAPOR kalau ditekan. Yang memegang jawabannya
 * satu variabel di atas sana. Dengan satu sumber kebenaran, mustahil ada dua
 * chip mengaku terpilih di saat yang sama.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilterRow(
    selected: ExerciseCategory?,
    onSelect: (ExerciseCategory?) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(text = stringResource(R.string.catalog_filter_all)) },
        )
        ExerciseCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) },
                label = { Text(text = category.label) },
            )
        }
    }
}

@Composable
private fun CatalogEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.catalog_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.catalog_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
/**
 * Satu kartu gerakan. BerGAMBAR, bisa dicentang, dan sejak hari ini dia PINTU --
 * bukan lagi lipatan yang mengembang di tempat.
 *
 * PELAJARAN KELIMA HARI INI: satu kartu, DUA arti tekanan.
 *
 * `combinedClickable` memberi kita dua pintu masuk: tekan biasa dan tekan lama.
 * Yang menarik bukan itu, tapi apa yang dilakukan tekan BIASA -- artinya
 * berubah tergantung keadaan layar. Di keadaan normal dia membuka layar detail.
 * Begitu ada satu saja yang tercentang, dia mencentang/melepas. Kenapa harus
 * begitu? Karena setelah kartu pertama tercentang, tidak ada orang yang mau
 * menekan lama sebelas kali lagi. Coba sendiri di Google Photos: yang kedua dan
 * seterusnya cukup tekan sekali.
 *
 * `performHapticFeedback` itu satu baris yang bedanya besar. Tekan lama tidak
 * punya batas yang kelihatan -- kamu tidak tahu 400ms sudah lewat atau belum.
 * Getaran kecil itu yang memberitahu jarimu "sudah, boleh dilepas". Tanpa dia,
 * tekan lama terasa seperti menebak.
 *
 * YANG DIBUANG HARI INI, dan kenapa saya buang alih-alih menumpuk:
 * kartu ini dulu memegang `expanded`, `confirmDelete`, teks instruksi, dan satu
 * `AlertDialog` sendiri -- artinya SETIAP kartu di daftar mengangkut satu dialog
 * hapus yang menunggu. Sekarang semua itu pindah ke `ExerciseDetailScreen`.
 *
 * Ini bukan sekadar rapi-rapi. Kartu yang mengembang di tempat memaksa isinya
 * dipotong supaya daftarnya tidak melompat: instruksi kepotong 3 baris, otot
 * kepotong satu baris, foto tetap 88dp. Begitu isinya berhak dapat satu layar
 * penuh, kamu tidak perlu memotong apa pun -- foto besar 16:9, instruksi bernomor
 * urut, alat dan otot lengkap. Kartu balik ke tugas aslinya: memberi cukup
 * petunjuk untuk memilih, lalu MINGGIR.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // Kartu terpilih diberi warna. Checkbox saja tidak cukup: dia cuma 20dp di
    // pojok, dan saat kamu memindai dua belas kartu, yang kamu cari adalah blok
    // warna, bukan kotak kecil.
    val cardColors = if (isSelected) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
    } else {
        CardDefaults.elevatedCardColors()
    }

    ElevatedCard(
        colors = cardColors,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // PERHATIKAN DI MANA `combinedClickable` DIPASANG: di Column DI DALAM
        // kartu, bukan di modifier kartunya.
        //
        // Kalau dipasang di luar, kartunya tetap bisa ditekan -- tapi lingkaran
        // riak (ripple) saat kamu menekan digambar SEBELUM kartu memotong
        // isinya mengikuti sudut membulat. Hasilnya riak itu tumpah keluar dan
        // kamu melihat empat pojok kotak sekilas di kartu yang seharusnya
        // membulat. Dari dalam sini, riaknya ikut terpotong rapi.
        //
        // Ini juga alasan kenapa saya tidak memakai `ElevatedCard(onClick = ...)`
        // yang sudah tersedia: dia tidak punya `onLongClick`, dan tekan lama itu
        // justru inti fitur hari ini.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (selectionMode) onToggleSelect() else onOpen()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleSelect()
                    },
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gambarnya menempel RATA ke tepi kartu, tanpa padding. Kartu M3
                // memotong isinya mengikuti sudut membulatnya sendiri, jadi
                // pojok kiri gambar ikut membulat tanpa kita atur apa pun.
                Thumbnail(
                    exercise = exercise,
                    // Lebar dipatok, TINGGI dihitung dari rasio 3:4 di dalam
                    // Thumbnail. Dulu di sini `size(88.dp)` -- kotak persegi.
                    // Kotak persegi itu bentuk yang paling jauh dari foto HP
                    // (9:16), jadi dialah yang paling banyak memotong.
                    modifier = Modifier.width(84.dp),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = exercise.category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = exercise.defaultType.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (exercise.muscles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        /*
                         * "BUG KEPOTONG" YANG KAMU LAPORKAN LEWAT KOLOM PENCARIAN
                         * APP-NYA SENDIRI, 5 September 2026. Saya baca. Kamu benar,
                         * dan ini penyebabnya -- bukan gambarnya, tapi baris ini.
                         *
                         * Dulu di sini semua otot digabung jadi satu kalimat lalu
                         * dipotong dengan `maxLines = 1`. Masalahnya, Compose
                         * memotong di karakter mana pun yang kebetulan pas di tepi.
                         * Wall Angle punya banyak otot, jadi yang muncul di HP-mu
                         * "Upper Traps, Rhomboids, Latissim..." -- kata yang
                         * terbelah separuh. Bagi mata itu terbaca sebagai RUSAK,
                         * bukan sebagai "masih ada lagi".
                         *
                         * PELAJARANNYA, dan ini berlaku untuk semua teks yang
                         * dipotong: ellipsis itu jaring pengaman, BUKAN rencana.
                         * Kalau kamu sudah tahu isinya akan kepanjangan, kamu yang
                         * harus memutuskan di mana potongannya -- di batas KATA,
                         * bukan di batas piksel. Di sini: tiga otot pertama, lalu
                         * "+N lagi" yang mengaku terus terang ada berapa sisanya.
                         *
                         * Ongkosnya saya sebutkan supaya kamu tidak kaget: kartu
                         * dengan banyak otot sekarang boleh setinggi dua baris,
                         * jadi tingginya tidak lagi seragam sempurna. Selisih ~16dp
                         * itu saya tukar dengan hilangnya kata terbelah. Kalau kamu
                         * lebih suka seragam, ganti `take(3)` jadi `take(2)` dan
                         * `maxLines` jadi 1.
                         */
                        val tampil = exercise.muscles.take(3)
                        val sisa = exercise.muscles.size - tampil.size
                        val ekor = stringResource(R.string.catalog_muscle_more, sisa)
                        Text(
                            text = tampil.joinToString(", ") { it.label } +
                                if (sisa > 0) " $ekor" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (selectionMode) {
                    // onCheckedChange = null. Bukan kelalaian: itu membuat
                    // Checkbox TIDAK bisa ditekan sendiri, sehingga tekanan
                    // jatuh ke kartunya. Hasilnya seluruh kartu jadi satu sasaran
                    // besar -- kalau checkbox-nya juga bisa ditekan, kamu punya
                    // dua sasaran bertumpuk dan sasaran 20dp itu yang menang.
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Kotak gambar gerakan. Kalau ada fotonya, tampilkan; kalau belum, tampilkan
 * huruf pertama namanya.
 *
 * PELAJARAN KEENAM (yang terakhir hari ini): kenapa HURUF, bukan ikon.
 *
 * Pilihan gampangnya adalah menaruh ikon gambar-rusak abu-abu di setiap kartu
 * tanpa foto. Masalahnya, kalau kamu punya dua belas gerakan dan sepuluh belum
 * berfoto, kamu dapat sepuluh ikon abu-abu IDENTIK -- daftar itu justru jadi
 * lebih sulit dipindai daripada sebelum ada gambar. Huruf pertama tetap
 * MEMBEDAKAN satu kartu dari kartu lain, jadi matamu masih bisa memakainya
 * untuk melompat ke gerakan yang kamu cari.
 *
 * Bonus praktis: `material-icons-core` yang kita pakai memang tidak punya
 * `Icons.Default.Image`. Ikon set penuh (`-extended`) itu ribuan ikon dan
 * menambah waktu build -- ongkos yang tidak masuk akal untuk satu placeholder.
 */
@Composable
private fun Thumbnail(exercise: Exercise, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adaFoto = exercise.thumbnailFile.isNotBlank()
    Box(
        modifier = modifier
            // 3:4 tegak, bukan persegi. Alasannya bisa dihitung: foto HP itu
            // 9:16 (0,56). Di kotak persegi (1,0) yang selamat cuma 56% tinggi
            // fotonya; di kotak 3:4 (0,75) yang selamat 75%. Dan karena isinya
            // dipasang dengan Fit, yang "tidak selamat" itu bukan gambarnya --
            // cuma ruang kosong di kiri-kanan. Nol piksel hilang.
            .aspectRatio(3f / 4f)
            .background(
                // Foto duduk di atas hitam supaya bilah kosongnya tidak
                // kelihatan; huruf cadangan duduk di atas amber supaya justru
                // kelihatan. Dua isi berbeda, dua latar berbeda.
                if (adaFoto) {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (adaFoto) {
            // `remember(...)` dengan kunci nama file. Alasan lengkapnya saya
            // tulis panjang di `FotoAlat` (AlatUi.kt) supaya tidak dua kali:
            // singkatnya ini mencegah satu objek `File` baru dibuat-dan-dibuang
            // di setiap recompose. Bukan bug -- Coil tetap mengenali
            // permintaannya sama karena `File` dibandingkan per path -- tapi
            // sampah sekecil ini dikali jumlah kartu yang lewat saat scroll
            // adalah kerja untuk garbage collector di detik paling salah.
            val berkas = remember(exercise.thumbnailFile) {
                fileGambar(context, exercise.thumbnailFile)
            }
            AsyncImage(
                // Coil menerima File langsung, tidak perlu diubah jadi String
                // atau Uri. `fileGambar` sengaja satu-satunya tempat yang tahu
                // nama file itu tinggal di mana.
                model = berkas,
                contentDescription = stringResource(R.string.catalog_thumbnail),
                // DIUBAH 5 Sept 2026: dulu Crop. Di daftar, Crop memang bikin
                // gambar terlihat "penuh dan rapi" -- tapi yang dipotongnya
                // justru bagian yang kamu pakai untuk mengenali gerakannya.
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = exercise.name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
