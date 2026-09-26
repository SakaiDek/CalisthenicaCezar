package com.cezar.calisthenica.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.AppDatabase
import com.cezar.calisthenica.model.Exercise
import com.cezar.calisthenica.model.Ketinggian

/**
 * LAYAR DETAIL GERAKAN. Layar penuh, dan inilah yang terbuka sekarang kalau kamu
 * menekan satu kartu di katalog.
 *
 * PELAJARAN PERTAMA HARI INI, dan saya harus mulai dengan MERALAT DIRI SENDIRI.
 *
 * Sesi lalu, di komentar `Screen` di MainActivity.kt, saya menulis: "begitu nanti
 * ada Layar Detail Gerakan yang harus tahu 'gerakan nomor berapa yang dibuka',
 * enum sudah tidak cukup -- di titik itu kita naik ke Navigation Compose."
 *
 * Kalimat itu keliru, dan hari ini kamu memegang buktinya: layar ini jalan tanpa
 * satu baris pun library navigasi. Kekeliruan saya adalah menyamakan "harus
 * membawa data" dengan "harus pakai router". Padahal yang layar ini butuh cuma
 * SATU ANGKA -- id gerakan yang dibuka -- dan angka itu sudah punya tempat yang
 * wajar: disimpan di katalog sebagai `detailId: Long?`, di mana null berarti
 * "tidak ada yang sedang dibuka".
 *
 * Navigation Compose baru layak dibayar kalau salah satu ini muncul: satu layar
 * punya BANYAK pintu masuk (dari dasbor, dari pencarian, dari notifikasi), ada
 * deep link dari luar app, atau kamu butuh tumpukan Back yang selamat walau
 * proses app dimatikan sistem. Kita belum punya satu pun. Menambahnya sekarang
 * cuma menambah waktu build di laptop 8GB-mu untuk masalah yang belum ada.
 *
 * Catat cara saya meralatnya, karena itu bagian dari pelajarannya: komentar yang
 * salah TIDAK dibiarkan hidup di kode. Komentar bohong lebih berbahaya daripada
 * tidak ada komentar -- enam bulan lagi kamu akan mempercayainya.
 *
 * @param onEdit Layar ini TIDAK menyunting sendiri, dia cuma melapor "gerakan ini
 * yang mau diubah" dan katalog yang membuka wizard-nya. Pola yang sama seperti
 * `onSave` di form: yang tahu keadaan melapor, yang umurnya lebih panjang
 * bertindak.
 *
 * PERUBAHAN 25 September 2026 -- `onEdit` sekarang NULLABLE (default null), dan
 * itu bukan sekadar kemalasan. Layar ini dipakai DUA tempat sekarang: dari
 * katalog (boleh menyunting -> kirim callback) DAN sebagai overlay di tengah
 * sesi latihan (SessionRunnerScreen), tempat menyunting gerakan sambil timer
 * jalan itu justru bikin celaka. Waktu `onEdit` null, tombol "Ubah" di dasar
 * layar TIDAK dirakit sama sekali -- bukan disembunyikan pakai `alpha`, bukan
 * dimatikan; komponennya memang tidak ada. Tombol yang tidak ada tidak bisa
 * salah ditekan, dan itu lebih murah daripada tombol kelabu yang tetap
 * memakan ruang layout.
 *
 * PERUBAHAN 5 September 2026 -- `onDelete` DIHAPUS dari layar ini, dan alasannya
 * layak kamu catat karena ini keputusan desain, bukan pemangkasan kode.
 *
 * Dulu di dasar layar ini ada tombol "Hapus gerakan". Masalahnya: menghapus itu
 * tindakan yang tidak bisa dibatalkan, dan tombolnya berada tepat di tempat mata
 * berhenti setelah membaca instruksi -- satu jempol salah sasaran, satu gerakan
 * lenyap. Sekarang app ini punya SATU pintu penghapusan: tekan lama di katalog,
 * pilih, lalu hapus dari bilah atas. Jalur itu memaksa kamu menyatakan sasaran
 * dulu sebelum menyebut tindakan, dan sekalian melayani "hapus lima sekaligus".
 *
 * Aturan umumnya, pakai di layar mana pun: SATU tindakan destruktif sebaiknya
 * punya SATU jalur. Dua jalur berarti dua dialog konfirmasi yang harus dijaga
 * sama, dan cepat atau lambat yang satu tertinggal saat yang lain diperbaiki.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onBack: () -> Unit,
    onEdit: ((Exercise) -> Unit)? = null,
) {
    val context = LocalContext.current
    val dao = remember(context) { AppDatabase.get(context).exerciseDao() }

    // `observeById`, BUKAN mencari di dalam daftar yang sudah dipegang katalog.
    //
    // Katalog memang sudah punya semua gerakan di memorinya, jadi mencari di situ
    // "lebih hemat". Tapi lihat untungnya bertanya langsung ke database: layar
    // ini jadi tidak bergantung pada siapa yang membukanya. Dan JANJI ITU DITAGIH
    // HARI INI: tombol "Edit gerakan" di dasar layar ini membuka wizard, kamu
    // simpan, layar ini kembali -- dan isinya sudah berubah tanpa ada satu pun
    // kode yang memberitahunya. Room yang memberi kabar, karena tabelnya yang
    // diawasi, bukan variabelnya.
    val flow = remember(dao, exerciseId) { dao.observeById(exerciseId) }
    val exercise by flow.collectAsState(initial = null)

    // SEMUA `remember` di layar ini WAJIB berdiri di atas `return` di bawah sana.
    // Ini jebakan yang tidak pernah kelihatan sampai dia menggigit: slot memori
    // Compose dihitung dari URUTAN pemanggilan. Kalau sebuah `remember` ditulis
    // SETELAH sebuah return bersyarat, dia lahir dan mati mengikuti syarat itu --
    // artinya isinya ke-reset diam-diam setiap kali syaratnya berbalik.
    //
    // `confirmDelete` yang dulu ada di sini sudah dihapus bersama tombol hapusnya.
    // Perhatikan: state yang tidak dipakai lagi WAJIB ikut dibuang, jangan cuma
    // tombolnya. State yatim itu yang bikin orang (termasuk saya, tiga bulan lagi)
    // mengira fitur hapusnya masih ada di sini dan mencari-cari tombolnya.
    var gagalBukaLink by remember { mutableStateOf(false) }

    // "Gerakan ini pernah ada, saya sudah lihat sendiri." Kenapa perlu dicatat,
    // ada di LaunchedEffect di bawah.
    var pernahAda by remember { mutableStateOf(false) }

    /*
     * KAMUS ALAT, pola yang sama persis dengan di katalog -- dan pengulangan itu
     * sengaja. Layar ini tidak menerima daftar alat dari katalog lewat parameter;
     * dia bertanya sendiri ke Room, dengan alasan yang sama seperti kenapa dia
     * memakai `observeById` dan bukan mencari di daftar milik katalog: layar yang
     * mengambil datanya sendiri tidak bisa menampilkan data basi.
     *
     * Untungnya nyata dan bisa kamu lihat nanti di Poco F5: buka detail gerakan
     * yang pakai matras, lalu ganti nama alatnya jadi "Yoga Mat" dari form. Layar
     * ini ikut berubah tanpa satu baris kode pun yang memberitahunya. Itu buah
     * dari menyimpan ID di kolom gerakan, bukan menyimpan namanya -- pelajaran
     * yang saya janjikan waktu memutuskan alat pantas jadi tabel.
     */
    val alatDao = remember(context) { AppDatabase.get(context).equipmentDao() }
    val alatFlow = remember(alatDao) { alatDao.observeAll() }
    val alatList by alatFlow.collectAsState(initial = emptyList())
    val alatMap = remember(alatList) { alatList.associateBy { it.id } }

    BackHandler { onBack() }

    // PELAJARAN KEDUA HARI INI: layar yang datanya bisa lenyap di bawah kakinya.
    //
    // Bayangkan urutan ini: kamu buka detail, tekan Hapus, dialog dikonfirmasi.
    // Baris di database hilang, dan `observeById` mengabarkan `null`. Kalau layar
    // ini tidak menyiapkan apa pun untuk keadaan itu, dia akan berdiri kosong --
    // atau lebih buruk, crash karena membaca `ex.name` dari null.
    //
    // Tapi `null` di sini punya DUA arti yang berbeda, dan itu intinya:
    //   1. "Database belum menjawab" -- normal, terjadi di frame pertama.
    //   2. "Barisnya sudah tidak ada" -- gerakannya benar-benar terhapus.
    // Bedanya cuma bisa dilihat dari sejarah: kalau kita PERNAH melihat isinya,
    // maka null yang datang sekarang pasti arti nomor dua. Itu tugas `pernahAda`.
    LaunchedEffect(exercise) {
        if (exercise != null) pernahAda = true else if (pernahAda) onBack()
    }

    // Frame pertama: belum dijawab, jadi jangan gambar apa pun. Satu frame kosong
    // tidak akan kamu sadari; satu frame berisi nama kosong akan kamu sadari.
    val ex = exercise ?: return

    // Tinggi bilah navigasi sistem. Dipakai di Spacer paling bawah.
    val jarakBawah = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ex.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.catalog_back),
                        )
                    }
                },
            )
        },
        // Insets bawah dinolkan supaya isi layar boleh mengalir sampai ujung
        // kaca dan lewat di belakang tiga tombol navigasi. Gantinya, Spacer
        // paling bawah (cari `jarakBawah` di dekat tombol Edit) yang menjaga
        // tombolnya tetap di atas bilah. Pola yang sama seperti dasbor.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // verticalScroll DULU, padding kiri-kanan MENYUSUL -- tapi
                // perhatikan padding-nya tidak dipasang di sini sama sekali.
                // Sengaja: foto di bawah harus menempel rata ke tepi layar, dan
                // itu mustahil kalau seluruh isi Column sudah dipadding 16dp.
                // Jadi paddingnya dipindah ke Column KEDUA, di bawah fotonya.
                .verticalScroll(rememberScrollState()),
        ) {
            HeroImage(exercise = ex)

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = ex.name,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = ex.category.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = ex.defaultType.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ALAT DITARUH PALING ATAS, di atas otot. Ini bukan urutan asal.
                // Sebelum kamu peduli otot mana yang kena, kamu harus tahu dulu
                // apakah gerakan ini BISA kamu lakukan sekarang -- ada pull up
                // bar di rumah atau tidak. Informasi yang menentukan "lanjut atau
                // lewati" selalu naik ke atas.
                DetailSection(title = stringResource(R.string.detail_equipment)) {
                    // Di sinilah aturan "list kosong = badan sendiri" akhirnya
                    // diucapkan jadi bahasa manusia. Perhatikan yang menerjemahkan
                    // adalah LAYAR, bukan database dan bukan enum. Database cuma
                    // menyimpan fakta; memberinya arti itu tugas layar.
                    //
                    // PELAJARAN HARI INI, dan ini yang paling layak kamu ingat dari
                    // seluruh build ini: `mapNotNull`, bukan `map`.
                    //
                    // Kolom gerakan menyimpan angka. Kalau kamu menghapus satu alat,
                    // angkanya TETAP tertulis di gerakan-gerakan yang memakainya --
                    // saya sengaja tidak menyisir tabel gerakan saat alat dihapus
                    // (baca alasannya di ExerciseFormScreen). Jadi id yatim itu
                    // nyata, dan pasti suatu hari lewat di sini.
                    //
                    // `mapNotNull` membuang yang tidak ketemu di kamus tanpa
                    // bersuara. Kalau saya pakai `map`, hasilnya `List<Equipment?>`
                    // yang berisi null, dan null itu akan berjalan terus sampai
                    // menabrak `it.name` -- crash, di layar yang cuma mau
                    // memperlihatkan gerakan. Pembersihan data paling murah selalu
                    // dilakukan di titik TAMPIL, bukan dengan menulis ulang
                    // database.
                    //
                    // `sortedBy { it.name }` supaya urutannya sama dengan urutan di
                    // form (yang juga urut nama). Tanpa ini urutannya ikut angka id,
                    // alias ikut urutan kamu mendaftarkan alat -- yang bagi matamu
                    // terlihat seperti tidak ada urutan sama sekali.
                    val alatDipakai = ex.equipmentIds
                        .mapNotNull { alatMap[it] }
                        .sortedBy { it.name }

                    if (alatDipakai.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_no_equipment),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // `PilAlat` (dari AlatUi.kt), bukan `Pill` polos: sekarang
                        // alat punya foto, dan foto kecil di sebelah namanya bikin
                        // kamu mengenali alatnya tanpa membaca. Pill teks tetap
                        // dipakai untuk otot di bawah -- otot tidak punya foto.
                        PillRow {
                            alatDipakai.forEach { PilAlat(alat = it) }
                        }

                        // ==========================================================
                        // BARIS KETINGGIAN. Inilah "ketinggian rings: misal
                        // overhead" yang kamu minta.
                        //
                        // Syaratnya DUA, dan yang kedua yang gampang dilupakan:
                        //   1. angkanya bukan 0 (0 = belum diatur, dan itu jawaban
                        //      yang benar untuk hampir semua gerakan).
                        //   2. masih ada alat yang MEMANG boleh diatur di antara
                        //      alat yang terpakai.
                        //
                        // Kenapa syarat kedua perlu padahal form sudah menolkan
                        // angkanya saat alat terakhir dilepas: karena angka itu
                        // bisa jadi yatim lewat jalan yang tidak lewat form sama
                        // sekali -- kamu menghapus ring dari gudang, dan gerakan
                        // lama masih menyimpan angka 8 di kolomnya. Ini pola yang
                        // sama dengan `mapNotNull` di atas: yang bertugas
                        // menyembunyikan data yatim adalah LAYAR, bukan penyisiran
                        // database. Satu kalimat untuk dihafal: layar tampil harus
                        // selalu tahan terhadap data yang sudah tidak masuk akal.
                        // ==========================================================
                        if (ex.equipmentHeight != 0 && alatDipakai.any { it.adjustableHeight }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.alat_height_set,
                                    Ketinggian.dari(ex.equipmentHeight).label,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                DetailSection(title = stringResource(R.string.detail_muscles)) {
                    if (ex.muscles.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_no_muscles),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        PillRow {
                            ex.muscles.forEach { Pill(text = it.label) }
                        }
                    }
                }

                DetailSection(title = stringResource(R.string.catalog_instruction)) {
                    // DIUBAH 5 Sept 2026. Dulu layar ini MEMAKSA setiap baris
                    // jadi langkah bernomor -- jadi penjelasan berbentuk paragraf
                    // tetap dapat lencana "1", dan itulah yang bikin instruksi
                    // antar gerakan kelihatan tidak konsisten.
                    //
                    // Sekarang formatnya dibaca dulu dari isi teksnya (lihat
                    // Instruksi.kt): teks yang setiap barisnya bernomor tampil
                    // sebagai daftar langkah, sisanya tampil apa adanya sebagai
                    // paragraf. Tetap NOL kolom baru di database.
                    //
                    // `.map { it.trim() }.filter { it.isNotBlank() }` bukan
                    // kerapian: orang menekan Enter dua kali tanpa sadar, dan
                    // tanpa dua baris itu kamu dapat langkah nomor 3 yang isinya
                    // kosong. Data dari tangan manusia SELALU perlu dibersihkan.
                    val bernomor = instruksiBernomor(ex.instruction)
                    val langkah = if (bernomor) {
                        pecahLangkah(ex.instruction).map { it.trim() }.filter { it.isNotBlank() }
                    } else {
                        emptyList()
                    }
                    val paragraf = if (bernomor) "" else ex.instruction.trim()

                    when {
                        langkah.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            langkah.forEachIndexed { i, teks ->
                                StepRow(nomor = i + 1, teks = teks)
                            }
                        }

                        paragraf.isNotBlank() -> Text(
                            text = paragraf,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        else -> Text(
                            text = stringResource(R.string.catalog_no_instruction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Seluruh bagian ini HILANG kalau linknya kosong -- bukan tampil
                // sebagai kotak "belum ada video". Kotak kosong bertuliskan
                // "belum ada" itu janji yang belum kamu tepati, dan sepuluh kotak
                // begitu di satu app bikin app-nya terasa setengah jadi.
                if (ex.youtubeUrl.isNotBlank()) {
                    DetailSection(title = stringResource(R.string.detail_youtube)) {
                        Text(
                            text = ex.youtubeUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                gagalBukaLink = false
                                // Intent ACTION_VIEW = "siapa pun yang bisa
                                // membuka alamat ini, silakan." Android yang
                                // memilih: app YouTube kalau ada, kalau tidak
                                // browser. App kita tidak perlu tahu keduanya.
                                //
                                // try/catch-nya WAJIB, bukan kehati-hatian
                                // berlebihan: di HP tanpa browser dan tanpa
                                // YouTube -- dan itu ada, sebagian custom ROM
                                // seperti punyamu bisa dicabut app-nya -- baris
                                // ini melempar ActivityNotFoundException dan
                                // app-mu MATI di depan user. Satu tangkapan
                                // mengubah crash jadi satu baris tulisan merah.
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(ex.youtubeUrl)),
                                    )
                                } catch (e: ActivityNotFoundException) {
                                    gagalBukaLink = true
                                }
                            },
                        ) {
                            Text(text = stringResource(R.string.detail_youtube_open))
                        }
                        if (gagalBukaLink) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.detail_youtube_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                // Tombol "Ubah" cuma dirakit kalau ADA yang mau menerima
                // laporannya (katalog). Di overlay sesi latihan `onEdit` null,
                // jadi seluruh blok di bawah -- pemisah, komentar, dan tombolnya
                // -- memang tidak ada, bukan sekadar disembunyikan. Lihat KDoc
                // `onEdit` di kepala file untuk alasan lengkapnya.
                if (onEdit != null) {
                Spacer(modifier = Modifier.height(28.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                /*
                 * "Edit gerakan" -- pengganti "Hapus gerakan" yang dulu berdiri di
                 * titik ini. Tiga hal ikut berubah selain tulisannya, dan itu
                 * pelajarannya: mengganti tombol destruktif jadi tombol biasa
                 * bukan pekerjaan tukar teks.
                 *
                 * 1. BENTUKNYA naik kelas. Dulu `TextButton` -- jenis tombol
                 *    paling lemah -- karena memang SENGAJA dibikin tidak menarik
                 *    perhatian. Sekarang `Button` penuh selebar layar. Tekanan
                 *    tidak sengaja tidak lagi merusak apa pun, jadi tidak ada
                 *    alasan menyembunyikannya.
                 * 2. WARNANYA pindah ke warna utama (oranye), bukan merah
                 *    `error`. Di Material 3 merah itu bahasa, bukan selera:
                 *    artinya "hati-hati, ini bisa merugikanmu". Memakainya untuk
                 *    menyunting sama dengan berbohong lewat warna.
                 * 3. DIALOG KONFIRMASINYA hilang, dan itu bukan kemalasan.
                 *    Konfirmasi cuma layak dibayar kalau tindakannya tidak bisa
                 *    ditarik kembali. Masuk ke wizard lalu tekan X sudah jadi
                 *    pembatalannya sendiri -- konfirmasi di depan pintu yang
                 *    sudah punya pintu keluar cuma satu ketukan sia-sia yang
                 *    kamu lakukan setiap kali.
                 *
                 * Perhatikan juga apa yang TIDAK dipanggil di sini: `onBack()`.
                 * Layar ini sengaja dibiarkan hidup di belakang wizard. Katalog
                 * memeriksa form SEBELUM memeriksa detail (lihat urutan `return`
                 * di ExerciseCatalogScreen), jadi begitu kamu menekan Simpan,
                 * wizard tertutup dan kamu MENDARAT DI SINI LAGI -- dengan isi
                 * yang sudah berubah sendiri lewat `observeById`. Kalau saya
                 * memanggil onBack, kamu terlempar ke daftar dan harus mencari
                 * gerakan itu lagi cuma untuk memastikan suntinganmu masuk.
                 */
                Button(
                    onClick = { onEdit(ex) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.detail_edit))
                }
                } // tutup if (onEdit != null)

                // 32dp jarak nyaman + setinggi bilah navigasi. Karena insets
                // Scaffold sudah dinolkan, Spacer inilah satu-satunya yang
                // menahan tombol Edit supaya tidak duduk di atas tombol Back
                // sistem. Kalau nanti kamu hapus `contentWindowInsets` di atas,
                // `jarakBawah` di sini WAJIB ikut dihapus -- kalau tidak,
                // jaraknya dihitung dua kali dan ada lubang kosong di dasar.
                Spacer(modifier = Modifier.height(32.dp + jarakBawah))
            }
        }
    }
}

/**
 * Foto besar di kepala layar, menempel rata ke tepi kiri-kanan.
 *
 * DIUBAH 5 Sept 2026 setelah kamu melaporkan "foto Wall Angle kepotong jelek".
 * Kamu benar, dan yang saya buang bukan pengaturannya tapi ASUMSINYA: dulu di
 * sini ditulis `aspectRatio(16f / 9f)` + `ContentScale.Crop`, artinya "kotaknya
 * tetap, fotonya yang harus mengalah". Untuk screenshot 9:16 itu berarti 68%
 * gambarnya dibuang -- kepala dan kaki gerakannya.
 *
 * Sekarang kebalikannya: kotaknya yang mengalah. Seluruh perhitungannya saya
 * pindahkan ke `FotoGerakan` supaya katalog, detail, dan form memakai satu
 * aturan yang sama; kalau besok kamu mau mengubah batas rasionya, cukup satu
 * file yang disunting, bukan tiga. Baca komentar panjang di `FotoGerakan.kt`.
 *
 * Kalau belum ada fotonya, yang tampil huruf pertama nama gerakan, sama seperti
 * di kartu katalog. Sengaja konsisten: huruf yang sama di dua tempat memberi tahu
 * matamu "ini benda yang sama", dan itu gratis. Kotak huruf ini TETAP 16:9 --
 * huruf tidak bisa kepotong, jadi tidak ada alasan memberinya ruang lebih.
 */
@Composable
private fun HeroImage(exercise: Exercise) {
    if (exercise.thumbnailFile.isNotBlank()) {
        // DIUBAH 25 Sept 2026: dibungkus `FotoBisaDiperbesar` supaya muncul tombol
        // "perbesar" di pojok foto. Default-nya TETAP `FotoGerakan` apa adanya
        // (utuh, Fit) -- tombol itu cuma menambah PILIHAN melihat penuh satu layar,
        // tidak mengubah tampilan awal. Aturan "tidak dipotong" tidak tersentuh.
        FotoBisaDiperbesar(
            namaFile = exercise.thumbnailFile,
            contentDescription = stringResource(R.string.catalog_thumbnail),
        ) {
            FotoGerakan(
                namaFile = exercise.thumbnailFile,
                contentDescription = stringResource(R.string.catalog_thumbnail),
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = exercise.name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.catalog_no_thumbnail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/**
 * Judul bagian + isinya.
 *
 * PELAJARAN KEEMPAT HARI INI, dan ini soal Kotlin, bukan Compose: file ini punya
 * `DetailSection` sendiri padahal `ExerciseFormScreen.kt` sudah punya
 * `SectionTitle` yang mirip. Itu BUKAN duplikasi karena kelalaian.
 *
 * `private` di Kotlin untuk deklarasi tingkat-atas artinya "privat untuk FILE
 * ini", bukan untuk paket atau modul. Jadi `SectionTitle` di file form memang
 * tidak bisa dilihat dari sini sama sekali -- mau bagaimanapun.
 *
 * Dan itu memang saya biarkan. Kalau saya menaikkannya jadi `internal` supaya
 * bisa dipakai bersama, dua layar itu jadi terikat: mengubah jarak judul di form
 * ikut mengubah tampilan detail, diam-diam. Aturan praktisnya: jangan berbagi
 * potongan UI kecil sampai kamu YAKIN dua tempat itu harus selalu terlihat sama.
 * Menyatukan yang sebenarnya berbeda jauh lebih mahal daripada menulis dua kali.
 */
@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
    content()
}

/** Wadah pil yang otomatis turun baris. Jaraknya sengaja sama dengan chip form. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PillRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

/**
 * Pil kecil berisi satu label. Ini PIL, bukan FilterChip -- dan bedanya penting.
 *
 * FilterChip itu tombol: dia punya keadaan terpilih/tidak, riak saat ditekan, dan
 * bentuk yang mengundang jari. Di layar detail tidak ada yang bisa dipilih; ini
 * cuma keterangan. Memakai chip di sini berarti kamu memasang sesuatu yang
 * KELIHATAN bisa ditekan tapi tidak melakukan apa-apa, dan tidak ada yang lebih
 * cepat merusak kepercayaan pada sebuah app daripada tombol yang bohong.
 *
 * `Surface` dipakai karena dia yang tahu cara memasangkan warna latar dengan
 * warna teks di atasnya: sekali kamu memberi `color` dan `contentColor`, semua
 * `Text` di dalamnya mewarisi warna yang benar tanpa kamu sebut lagi. Di mode
 * gelap pasangannya ikut berubah sendiri.
 */
@Composable
private fun Pill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/**
 * Satu langkah instruksi: nomornya di kiri, teksnya di kanan.
 *
 * `Modifier.width(24.dp)` pada nomornya itu satu-satunya alasan daftar ini
 * kelihatan rapi. Tanpa lebar tetap, "1." dan "10." punya lebar berbeda, dan
 * seluruh kalimat di kanannya bergeser -- tepi kiri teksmu jadi zig-zag. Dengan
 * lebar dipatok, semua kalimat mulai di garis yang sama.
 */
@Composable
private fun StepRow(nomor: Int, teks: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$nomor.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = teks,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * PANEL DETAIL versi RINGKAS -- dipakai DI DALAM sesi latihan (Session Runner),
 * bukan dari katalog. Muncul sebagai ModalBottomSheet yang ditarik dari bawah.
 *
 * KENAPA FUNGSI TERPISAH, bukan memanggil `ExerciseDetailScreen` yang sudah ada:
 * dua layar ini beda TUJUAN walau isinya mirip. `ExerciseDetailScreen` layar
 * penuh untuk MEMPELAJARI gerakan -- ada foto besar, tombol YouTube, tombol Ubah.
 * Panel ini muncul DI TENGAH latihan, di atas foto gerakan yang SUDAH jadi
 * wallpaper. Memuat ulang foto + video + tombol edit di sini itu redundan dan
 * berat (keluhan Sakai 25 Sept 2026). Jadi panel ini teks-saja: Alat, Otot, Cara.
 *
 * KENAPA DUDUK DI FILE INI, bukan di SessionRunnerScreen: dia meminjam empat
 * helper file-privat yang sudah matang di sini -- `DetailSection`, `PillRow`,
 * `Pill`, `StepRow` -- plus pola baca datanya (observe by id + kamus alat). Kalau
 * ditaruh di file runner, keempat helper itu harus dinaikkan jadi `internal` dan
 * dua file jadi saling terikat. Di sini = nol perubahan pada yang sudah jalan.
 *
 * BEDA `Pill(text = it.name)` DENGAN `PilAlat` DI ATAS: `PilAlat` menampilkan
 * FOTO kecil alat. Di panel ini foto sengaja dibuang total (itu inti permintaan),
 * jadi alat pun cukup pil teks -- sama seperti otot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDetailGerakan(
    exerciseId: Long,
    onTutup: () -> Unit,
) {
    val context = LocalContext.current
    val dao = remember(context) { AppDatabase.get(context).exerciseDao() }
    val flow = remember(dao, exerciseId) { dao.observeById(exerciseId) }
    val ex by flow.collectAsState(initial = null)

    val alatDao = remember(context) { AppDatabase.get(context).equipmentDao() }
    val alatFlow = remember(alatDao) { alatDao.observeAll() }
    val alatList by alatFlow.collectAsState(initial = emptyList())
    val alatMap = remember(alatList) { alatList.associateBy { it.id } }

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            val gerakan = ex
            if (gerakan == null) {
                // Satu frame kosong saat Room belum menjawab -- wajar, bukan error.
                Text(
                    text = stringResource(R.string.run_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = gerakan.name,
                    style = MaterialTheme.typography.headlineSmall,
                )
                DetailSection(title = stringResource(R.string.detail_equipment)) {
                    // Sama seperti di ExerciseDetailScreen: id yatim (alat sudah
                    // dihapus dari gudang) dibuang di titik TAMPIL lewat mapNotNull.
                    val alatDipakai = gerakan.equipmentIds
                        .mapNotNull { alatMap[it] }
                        .sortedBy { it.name }
                    if (alatDipakai.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_no_equipment),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // Pil TEKS, bukan PilAlat: panel ini sengaja tanpa foto.
                        PillRow {
                            alatDipakai.forEach { Pill(text = it.name) }
                        }
                        if (gerakan.equipmentHeight != 0 && alatDipakai.any { it.adjustableHeight }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.alat_height_set,
                                    Ketinggian.dari(gerakan.equipmentHeight).label,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                DetailSection(title = stringResource(R.string.detail_muscles)) {
                    if (gerakan.muscles.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_no_muscles),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        PillRow {
                            gerakan.muscles.forEach { Pill(text = it.label) }
                        }
                    }
                }

                DetailSection(title = stringResource(R.string.catalog_instruction)) {
                    val bernomor = instruksiBernomor(gerakan.instruction)
                    val langkah = if (bernomor) {
                        pecahLangkah(gerakan.instruction).map { it.trim() }.filter { it.isNotBlank() }
                    } else {
                        emptyList()
                    }
                    val paragraf = if (bernomor) "" else gerakan.instruction.trim()
                    when {
                        langkah.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            langkah.forEachIndexed { i, teks ->
                                StepRow(nomor = i + 1, teks = teks)
                            }
                        }
                        paragraf.isNotBlank() -> Text(
                            text = paragraf,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        else -> Text(
                            text = stringResource(R.string.catalog_no_instruction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
