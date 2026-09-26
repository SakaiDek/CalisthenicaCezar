package com.cezar.calisthenica.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.AppDatabase
import com.cezar.calisthenica.data.BarisRakitan
import com.cezar.calisthenica.model.Exercise
import com.cezar.calisthenica.model.ExerciseType
import com.cezar.calisthenica.model.Grup
import com.cezar.calisthenica.model.ProgramExerciseRef
import com.cezar.calisthenica.model.WorkoutProgram
import com.cezar.calisthenica.model.perkiraanDetik
import kotlinx.coroutines.launch

/**
 * LAYAR PERAKIT PROGRAM. Di sinilah program berhenti jadi judul kosong dan mulai
 * jadi sesi latihan.
 *
 * Bentuknya satu daftar panjang berisi TIGA bagian tetap -- Pemanasan, Inti,
 * Pendinginan -- yang selalu ada walau kosong. Ini keputusan sadar, dan alasannya
 * bukan teknis tapi soal cara orang berpikir: grup kosong yang tetap kelihatan
 * itu UNDANGAN ("isi aku"). Grup yang baru muncul setelah diisi bikin kamu harus
 * menghafal cara memanggilnya.
 *
 * ---------------------------------------------------------------------------
 * PELAJARAN HARI INI: SATU BENDA, SATU PEMILIK.
 *
 * Perhatikan pembagian tugas di bawah, karena inilah yang membuat layar rumit
 * tetap bisa dibaca:
 *
 *   DATABASE yang memiliki urutan, angka, dan isi program. Bukan memori app.
 *   Tidak ada satu pun `var daftarGerakan` di sini yang menyimpan salinan.
 *   Setiap tombol naik/turun/simpan MENULIS KE DATABASE, lalu `Flow` yang
 *   memberitahu layar bahwa datanya berubah, lalu layar menggambar ulang.
 *
 *   Kelihatan berputar jauh, kan? Tekan tombol naik, data ke disk, balik lagi
 *   ke layar. Kenapa tidak langsung tukar posisi di memori saja supaya cepat?
 *   Karena kalau layar punya salinannya sendiri, kamu punya DUA kebenaran, dan
 *   suatu hari keduanya berbeda: urutan di layar sudah tertukar tapi yang di
 *   disk belum, lalu app dimatikan. Satu sumber kebenaran itu harga yang
 *   dibayar sekali di sini, untuk bug yang tidak pernah lahir.
 *
 *   MEMORI cuma memiliki hal-hal yang TIDAK ADA di database: grup mana yang
 *   sedang membuka pemilih gerakan, dan baris mana yang sedang diatur
 *   parameternya. Dua-duanya lenyap saat layar ditutup, dan memang seharusnya.
 * ---------------------------------------------------------------------------
 *
 * TIGA PENAWARAN MVP yang saya buka terang-terangan, supaya kamu tidak
 * menemukannya sendiri lalu merasa dikibuli:
 *
 * 1. URUTAN DIUBAH PAKAI TOMBOL PANAH, BUKAN DITARIK (drag and drop).
 *    Menarik-jatuhkan di LazyColumn butuh menghitung posisi tiap baris,
 *    menerjemahkan geseran jari jadi indeks, plus animasi baris yang menyingkir.
 *    Itu satu layar penuh kode sendiri. Tombol panah menyelesaikan pekerjaan
 *    yang sama dalam 10 baris, dan untuk sesi berisi 6 gerakan bedanya cuma
 *    rasa. Kita bikin rasanya enak nanti, setelah semua fiturnya ada.
 *
 * 2. TOMBOL "KELUARKAN DARI PROGRAM" SENGAJA DISEMBUNYIKAN DI DALAM DIALOG
 *    PARAMETER, bukan dipajang sebagai ikon tong sampah di setiap baris. Pola
 *    yang sama persis dengan tombol hapus program di `WorkoutCard`: aksi yang
 *    tidak bisa dibatalkan tidak boleh gampang kesenggol. Bonusnya, karena
 *    membukanya sudah butuh satu ketukan sadar, dia tidak perlu dialog
 *    konfirmasi lagi -- dialognya sendiri sudah jadi konfirmasinya.
 *
 * 3. NAMA DAN DESKRIPSI PROGRAM BELUM BISA DIUBAH DARI SINI. Layar ini cuma
 *    mengurus ISI program. Mengubah judulnya nanti satu tombol edit sendiri.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramBuilderScreen(
    program: WorkoutProgram,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val dao = remember { db.programExerciseDao() }

    /*
     * `remember(program.id)`, dengan KUNCI. Ini bukan hiasan.
     *
     * `observeRakitan` itu pabrik Flow: setiap kali dipanggil dia membuat objek
     * Flow baru yang bertanya soal satu program tertentu. Tanpa `remember`,
     * objek baru lahir tiap recompose dan `collectAsState` menyimpulkan
     * "sumbernya ganti" lalu berlangganan ulang ke database -- puluhan kali per
     * detik saat kamu mengetik di dialog.
     *
     * Dan kuncinya WAJIB `program.id`, bukan kosong. `remember { }` tanpa kunci
     * berarti "hitung sekali, simpan selamanya". Kalau nanti layar ini dipakai
     * untuk program lain tanpa dibuang dari komposisi dulu, dia akan tetap
     * menampilkan isi program yang lama. Kunci itu yang bilang: "hitung ulang
     * kalau programnya ganti."
     */
    val rakitanFlow = remember(program.id) { dao.observeRakitan(program.id) }
    val rakitan by rakitanFlow.collectAsState(initial = emptyList())

    val katalogFlow = remember { db.exerciseDao().observeAll() }
    val katalog by katalogFlow.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    // Dua satu-satunya keadaan yang tinggal di memori. Kalau suatu hari daftar
    // ini bertambah panjang, itu tandanya layar ini sudah pantas punya ViewModel.
    var grupTujuan by remember { mutableStateOf<Grup?>(null) }
    var sedangDiatur by remember { mutableStateOf<BarisRakitan?>(null) }

    val jarakBawah = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    /*
     * PEMILIH GERAKAN, digambar sebagai `return` LEBIH AWAL.
     *
     * Pola yang sama dengan `LayarKetinggian`, dan aturannya cuma satu tapi
     * mutlak: `return` ini harus duduk SETELAH semua `remember` di atas.
     * Komposisi Compose itu berdasarkan POSISI -- kalau `return` menyalip satu
     * `remember`, benda yang di-remember itu dianggap keluar dari komposisi dan
     * nilainya hilang. Jadi setiap kali pemilih dibuka-tutup, angka dan langganan
     * database di atas akan direset. Bug yang bikin pusing karena kodenya
     * kelihatan benar.
     *
     * Kenapa `return` dan bukan sekadar `if` yang membungkus seluruh Scaffold:
     * dengan `return`, Scaffold utamanya benar-benar TIDAK DIBUAT selama pemilih
     * terbuka. Kalau dibungkus `if`, saya harus menuliskan `else` yang isinya 200
     * baris, dan lekukan kodenya masuk satu tingkat lebih dalam untuk seluruh
     * sisa file.
     */
    val tujuan = grupTujuan
    if (tujuan != null) {
        LayarPilihGerakan(
            grup = tujuan,
            katalog = katalog,
            onPilih = { gerakan ->
                scope.launch {
                    /*
                     * Tanya database dulu nomor urut terakhir di grup ini, baru
                     * sisipkan. Dua perintah, dan sengaja BERURUTAN di dalam satu
                     * coroutine -- gerakan berikutnya tidak akan bertanya sebelum
                     * yang ini selesai menulis, jadi tidak ada dua baris yang
                     * kebagian nomor urut sama.
                     */
                    val urutBaru = dao.urutanTerakhir(program.id, tujuan.nomor) + 1

                    /*
                     * `tipe` DISALIN dari `gerakan.defaultType`, dan inilah saat
                     * kata "SARAN" di `Exercise.defaultType` akhirnya berarti
                     * sesuatu: yang di katalog cuma usulan, yang tersimpan di sini
                     * keputusan. Setelah baris ini lahir, mengubah satuan default
                     * di katalog TIDAK akan mengubah program yang sudah kamu susun.
                     * Itu memang yang kita mau.
                     *
                     * Angka bawaannya beda per satuan, karena 10 detik tahan itu
                     * angka yang tidak masuk akal untuk siapa pun: HOLD mulai dari
                     * 30 detik, REPS mulai dari 10 repetisi. Tebakan awal yang
                     * masuk akal itu bagian dari sopan santun app.
                     */
                    dao.insert(
                        ProgramExerciseRef(
                            programId = program.id,
                            exerciseId = gerakan.id,
                            grup = tujuan.nomor,
                            urutan = urutBaru,
                            tipe = gerakan.defaultType,
                            target = if (gerakan.defaultType == ExerciseType.HOLD) 30 else 10,
                        ),
                    )
                }
            },
            // Pemilih TIDAK menutup sendiri setiap kali satu gerakan dipilih.
            // Kamu menyusun sesi, bukan memilih satu benda: enam gerakan berarti
            // enam ketukan, bukan enam kali buka-tutup layar.
            onTutup = { grupTujuan = null },
        )
        return
    }

    /*
     * BackHandler layar perakit, dan POSISINYA yang bikin dia benar.
     *
     * Dia duduk SETELAH `return` di atas. Artinya saat pemilih gerakan terbuka,
     * baris ini tidak pernah dijalankan -- BackHandler ini keluar dari komposisi
     * dan melepaskan pendaftarannya, lalu BackHandler milik pemilih yang
     * mengambil alih. Begitu pemilih ditutup, urutannya berbalik sendiri.
     *
     * Jadi aturan rumah "satu layar, satu BackHandler" tetap utuh tanpa saya
     * perlu mengurus prioritas atau `enabled` sama sekali. Susunan kode yang
     * benar mengalahkan logika yang pintar.
     *
     * Kenapa ini WAJIB ada, bukan pemanis: mode immersive menyembunyikan tiga
     * tombol navigasi HP-mu. Tanpa baris ini, Back (gestur atau tombol yang
     * muncul setelah bilahnya ditarik) akan MENUTUP APP dari layar perakit,
     * bukan kembali ke dasbor -- karena BackHandler induk di `CalisthenicaApp`
     * sengaja tidur selama kita di HOME.
     */
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Judulnya nama program, bukan "Perakit Program". Kamu selalu
                    // tahu program mana yang sedang kamu bongkar.
                    Text(
                        text = program.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.rakit_back),
                        )
                    }
                },
            )
        },
        // Pola baku app ini: sisi kiri-kanan dihormati (buat mode landscape),
        // sisi bawah dinolkan supaya daftar bisa lewat di belakang bilah
        // navigasi. Yang membayar jarak bawahnya `contentPadding` di bawah.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                // Tidak ada FAB di layar ini, jadi cukup jarak nyaman + tinggi
                // bilah navigasi. Angka 96dp di layar lain itu ruang untuk FAB;
                // menirunya di sini cuma bikin lubang kosong di bawah.
                bottom = 24.dp + jarakBawah,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "ringkasan") {
                RingkasanAtas(rakitan = rakitan)
            }

            /*
             * `forEach` di dalam LazyColumn. Boleh, dan di sini justru benar.
             *
             * Yang dilarang itu membuat RIBUAN item lewat loop biasa, karena
             * blok ini dijalankan sekaligus untuk mendaftarkan semua item.
             * Grupnya cuma tiga dan jumlahnya tidak akan pernah tumbuh dari data
             * user -- yang panjang cuma `items(...)` di dalamnya, dan itu memang
             * dimalasi (lazy) oleh LazyColumn seperti seharusnya.
             */
            Grup.urut().forEach { grup ->
                val isiGrup = rakitan.filter { it.ref.grup == grup.nomor }

                item(key = "kepala-${grup.nomor}") {
                    KepalaGrup(grup = grup, isi = isiGrup)
                }

                /*
                 * `key = { _, b -> b.ref.id }` -- kuncinya id baris, BUKAN
                 * indeksnya. Ini yang memberitahu LazyColumn "baris ini masih
                 * baris yang sama walau posisinya bergeser". Dengan kunci indeks
                 * (atau tanpa kunci sama sekali), menggeser satu gerakan bikin
                 * Compose menganggap SEMUA baris di bawahnya berganti isi, lalu
                 * menggambar ulang seluruh grup -- termasuk memuat ulang fotonya.
                 */
                itemsIndexed(
                    items = isiGrup,
                    key = { _, b -> b.ref.id },
                ) { index, baris ->
                    BarisRakit(
                        baris = baris,
                        bisaNaik = index > 0,
                        bisaTurun = index < isiGrup.lastIndex,
                        onKlik = { sedangDiatur = baris },
                        onGeser = { arah ->
                            val tukar = isiGrup.toMutableList()
                            val tujuanIndex = index + arah
                            val ambil = tukar[index]
                            tukar[index] = tukar[tujuanIndex]
                            tukar[tujuanIndex] = ambil

                            scope.launch {
                                /*
                                 * SELURUH GRUP dinomori ulang dari 1, bukan cuma
                                 * dua baris yang bertukar. Kelihatan berlebihan --
                                 * kenapa menulis enam baris kalau yang pindah dua?
                                 *
                                 * Karena ini yang membuat nomor urut MENYEMBUHKAN
                                 * DIRI. Kalau suatu hari ada grup yang nomornya
                                 * bolong (1, 2, 5) gara-gara baris di tengah
                                 * dikeluarkan, satu kali kamu geser apa pun di
                                 * grup itu, semuanya rapat kembali. Tidak ada kode
                                 * pembersih khusus yang harus saya tulis, dan tidak
                                 * ada keadaan cacat yang bisa menetap.
                                 *
                                 * Satu `updateSemua`, bukan enam `update`: satu
                                 * transaksi, jadi mustahil ada dua baris bernomor
                                 * sama walau app mati di tengah jalan.
                                 */
                                dao.updateSemua(
                                    tukar.mapIndexed { posisi, b ->
                                        b.ref.copy(urutan = posisi + 1)
                                    },
                                )
                            }
                        },
                    )
                }

                item(key = "tambah-${grup.nomor}") {
                    BarisTambahGerakan(onKlik = { grupTujuan = grup })
                }
            }
        }
    }

    val diatur = sedangDiatur
    if (diatur != null) {
        DialogParameter(
            baris = diatur,
            onSimpan = { refBaru ->
                sedangDiatur = null
                scope.launch { dao.update(refBaru) }
            },
            onKeluarkan = {
                sedangDiatur = null
                scope.launch { dao.delete(diatur.ref) }
            },
            onTutup = { sedangDiatur = null },
        )
    }
}

/**
 * Baris paling atas: seluruh program dalam satu kalimat.
 *
 * Angkanya dihitung di Kotlin dari daftar yang sudah ada di tangan, BUKAN dengan
 * query baru. Datanya sudah lengkap di memori; bertanya lagi ke database untuk
 * menjumlahkan sepuluh angka itu pemborosan yang tidak menghasilkan apa-apa.
 */
@Composable
private fun RingkasanAtas(rakitan: List<BarisRakitan>) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        if (rakitan.isEmpty()) {
            Text(
                text = stringResource(R.string.rakit_total_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val totalDetik = rakitan.sumOf { it.ref.perkiraanDetik() }
            Text(
                text = stringResource(
                    R.string.rakit_total,
                    rakitan.size,
                    labelDurasi(totalDetik),
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.rakit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Judul satu grup, plus hitungannya. */
@Composable
private fun KepalaGrup(grup: Grup, isi: List<BarisRakitan>) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)) {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            // Labelnya dari enum, bukan strings.xml. Gaya rumah -- lihat
            // komentar di `Grup` kalau mau tahu alasannya.
            text = grup.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (isi.isEmpty()) {
                stringResource(R.string.rakit_grup_kosong)
            } else {
                stringResource(
                    R.string.rakit_grup_isi,
                    isi.size,
                    labelDurasi(isi.sumOf { it.ref.perkiraanDetik() }),
                )
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Satu gerakan di dalam program. Ditekan untuk mengatur angkanya.
 *
 * `onGeser` menerima -1 atau 1, bukan dua lambda `onNaik`/`onTurun`. Isinya akan
 * sama persis kecuali satu tanda minus, dan dua lambda yang isinya sama itu dua
 * tempat yang harus diperbaiki setiap kali logikanya berubah.
 */
@Composable
private fun BarisRakit(
    baris: BarisRakitan,
    bisaNaik: Boolean,
    bisaTurun: Boolean,
    onKlik: () -> Unit,
    onGeser: (Int) -> Unit,
) {
    val ref = baris.ref

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onKlik),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            /*
             * FotoAlat DIPAKAI ULANG untuk foto gerakan. Namanya memang bicara
             * soal alat, tapi isinya sudah lama tidak: dia cuma "kotak gambar
             * dengan huruf awal sebagai cadangan kalau fotonya belum ada".
             *
             * Kenapa BUKAN `FotoGerakan` yang jelas-jelas namanya lebih cocok:
             * `FotoGerakan` MENGANIMASIKAN `Modifier.aspectRatio`, dan animasi
             * yang mengubah UKURAN memaksa Compose mengukur ulang tata letak
             * setiap frame. Di layar detail yang cuma punya satu foto besar itu
             * tidak terasa. Di daftar yang di-scroll dengan enam baris terlihat,
             * itu masuk ke daftar tersangka patah-patah yang masih kita kejar.
             *
             * Pelajarannya: nama fungsi yang kurang pas itu utang kecil, memilih
             * komponen yang salah demi namanya itu utang yang berbunga.
             */
            FotoAlat(
                namaFile = baris.thumbnail,
                nama = baris.namaGerakan,
                modifier = Modifier.size(52.dp),
                sudut = 12.dp,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    text = baris.namaGerakan,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.rakit_ringkas,
                        ref.setCount,
                        ref.target,
                        stringResource(satuanDari(ref.tipe)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (ref.istirahatDetik > 0) {
                        stringResource(R.string.rakit_istirahat, ref.istirahatDetik)
                    } else {
                        stringResource(R.string.rakit_tanpa_istirahat)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Catatan cuma digambar kalau ada isinya. Baris kosong yang
                // selalu nongkrong bikin tiap baris lebih tinggi tanpa memberi
                // informasi apa pun.
                if (ref.catatan.isNotBlank()) {
                    Text(
                        text = ref.catatan,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column {
                /*
                 * `enabled = false` bukan "tombolnya disembunyikan". Tombolnya
                 * tetap ada, tetap memakan ruang, cuma redup dan tidak bisa
                 * ditekan. Ini pilihan sengaja: kalau tombol paling atas
                 * kehilangan panah atasnya, seluruh baris jadi lebih pendek dan
                 * ikonnya bergeser -- daftarnya kelihatan goyang setiap kali kamu
                 * menggeser gerakan. Ruang yang tetap itu yang bikin mata tenang.
                 */
                IconButton(
                    onClick = { onGeser(-1) },
                    enabled = bisaNaik,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.rakit_naik),
                    )
                }
                IconButton(
                    onClick = { onGeser(1) },
                    enabled = bisaTurun,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.rakit_turun),
                    )
                }
            }
        }
    }
}

/** Tombol tambah, satu per grup. */
@Composable
private fun BarisTambahGerakan(onKlik: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onKlik),
        // Warna yang lebih redup dari baris isi. Tombol ini pelayan, bukan
        // bintang: dia harus gampang ditemukan tanpa menarik perhatian dari
        // gerakan yang sudah tersusun.
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.rakit_tambah),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * Dialog pengatur angka untuk SATU baris.
 *
 * Semua kolom di sini menyimpan `String`, bukan `Int`. Ini bukan kemalasan.
 * Kolom teks yang isinya angka HARUS boleh kosong sementara: begitu kamu
 * menghapus satu-satunya digit di kolom "Set", nilainya jadi teks kosong. Kalau
 * yang disimpan `Int`, teks kosong tidak punya wujud -- pilihannya cuma memaksa
 * jadi 0 (kolomnya tiba-tiba berisi "0" yang harus kamu hapus dulu, menjengkelkan)
 * atau melempar exception. Teks dulu, angka nanti, dan penerjemahannya sekali
 * saja saat Simpan ditekan.
 */
@Composable
private fun DialogParameter(
    baris: BarisRakitan,
    onSimpan: (ProgramExerciseRef) -> Unit,
    onKeluarkan: () -> Unit,
    onTutup: () -> Unit,
) {
    val ref = baris.ref

    var tipe by remember { mutableStateOf(ref.tipe) }
    var target by remember { mutableStateOf(ref.target.toString()) }
    var setCount by remember { mutableStateOf(ref.setCount.toString()) }
    var istirahat by remember { mutableStateOf(ref.istirahatDetik.toString()) }
    var catatan by remember { mutableStateOf(ref.catatan) }

    AlertDialog(
        onDismissRequest = onTutup,
        title = {
            Text(
                text = baris.namaGerakan,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.param_tipe),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Dua chip, bukan Switch. Switch cuma bisa bilang
                    // "nyala/mati" dan tidak ada satu pun kata yang benar untuk
                    // "mati" di sini -- "bukan repetisi" itu bukan satuan.
                    ExerciseType.entries.forEach { pilihan ->
                        FilterChip(
                            selected = tipe == pilihan,
                            onClick = { tipe = pilihan },
                            label = { Text(text = pilihan.label) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KolomAngka(
                        nilai = setCount,
                        onUbah = { setCount = it },
                        label = stringResource(R.string.param_set),
                        modifier = Modifier.weight(1f),
                    )
                    KolomAngka(
                        nilai = target,
                        // Label kolom ini BERUBAH mengikuti chip di atas. Satu
                        // kolom, dua arti -- persis seperti kolom `target` di
                        // database. Layar yang menampilkan data wajib bicara
                        // dengan bahasa yang sama dengan datanya.
                        onUbah = { target = it },
                        label = stringResource(satuanDari(tipe)),
                        modifier = Modifier.weight(1f),
                    )
                }

                KolomAngka(
                    nilai = istirahat,
                    onUbah = { istirahat = it },
                    label = stringResource(R.string.param_istirahat),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text(text = stringResource(R.string.param_catatan)) },
                    placeholder = { Text(text = stringResource(R.string.param_catatan_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                TextButton(onClick = onKeluarkan) {
                    Text(
                        text = stringResource(R.string.param_keluarkan),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    /*
                     * `ref.copy(...)` -- HUKUM RUMAH, dan pelanggaran hukum ini
                     * yang dulu menghapus foto gerakanmu tanpa error.
                     *
                     * Menyimpan satu baris berarti MENIMPA SELURUH BARIS. Kalau
                     * di sini saya menulis `ProgramExerciseRef(...)` dari nol,
                     * saya harus menyebutkan ulang `id`, `programId`,
                     * `exerciseId`, `grup`, dan `urutan` -- dan yang lupa
                     * disebut akan diisi nilai bawaan. `id = 0` artinya baris
                     * baru, jadi baris lamamu tetap ada DAN muncul kembarannya.
                     *
                     * `copy` membalik bebannya: yang tidak saya sebut TIDAK
                     * BERUBAH. Di form, itu bedanya antara data yang utuh dan
                     * data yang hilang diam-diam.
                     *
                     * `coerceIn` menjaga angka mustahil: 0 set itu bukan latihan,
                     * dan istirahat 9999 detik itu salah ketik. `toIntOrNull`
                     * mengurus kolom yang dikosongkan -- jatuh ke nilai lama,
                     * bukan ke nol.
                     */
                    onSimpan(
                        ref.copy(
                            tipe = tipe,
                            target = target.toIntOrNull()?.coerceIn(1, 9999) ?: ref.target,
                            setCount = setCount.toIntOrNull()?.coerceIn(1, 99) ?: ref.setCount,
                            istirahatDetik = istirahat.toIntOrNull()?.coerceIn(0, 900)
                                ?: ref.istirahatDetik,
                            catatan = catatan.trim(),
                        ),
                    )
                },
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onTutup) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * Satu kolom isi angka.
 *
 * `keyboardType = Number` itu PERMINTAAN, bukan jaminan -- dia cuma menyuruh HP
 * menampilkan papan tombol angka. Keyboard fisik, tempel-salin, dan beberapa
 * papan tombol pihak ketiga tetap bisa memasukkan huruf. Jadi penyaringnya di
 * `onValueChange`, tempat yang tidak bisa dilewati siapa pun.
 *
 * `take(4)` supaya tidak ada yang mengetik 999999 detik istirahat lalu bingung
 * kenapa perkiraan waktunya jadi 11 hari.
 */
@Composable
private fun KolomAngka(
    nilai: String,
    onUbah: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = nilai,
        onValueChange = { baru -> onUbah(baru.filter { it.isDigit() }.take(4)) },
        label = { Text(text = label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/**
 * Layar pemilih gerakan. Bukan dialog, dan itu keputusan yang sama dengan layar
 * ketinggian alat: daftar yang panjang butuh seluruh tinggi kaca, dan dialog
 * yang isinya bisa 60 baris itu dialog yang salah bentuk.
 *
 * `BackHandler` di sini SATU-SATUNYA yang hidup saat layar ini tampil, karena
 * layar perakit di atas digambar lewat `return` lebih awal -- jadi `BackHandler`
 * miliknya tidak pernah ikut terdaftar. Aturan "satu layar satu BackHandler"
 * tetap utuh tanpa saya perlu mengurus prioritas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayarPilihGerakan(
    grup: Grup,
    katalog: List<Exercise>,
    onPilih: (Exercise) -> Unit,
    onTutup: () -> Unit,
) {
    BackHandler(onBack = onTutup)

    val jarakBawah = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.rakit_pilih_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        // Grup tujuannya WAJIB kelihatan. Tanpa baris ini, kamu
                        // menekan "Tambah gerakan" di Pendinginan, layar berganti,
                        // lalu lima gerakan berikutnya kamu pilih tanpa tahu
                        // sedang mengisi grup mana.
                        Text(
                            text = stringResource(R.string.rakit_pilih_sub, grup.label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onTutup) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.rakit_back),
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        if (katalog.isEmpty()) {
            // Layar kosong tanpa jalan keluar itu jebakan, terutama karena mode
            // immersive menyembunyikan tombol Back sistem. Panah di kiri atas
            // sudah ada, dan kalimat ini yang memberitahu harus ke mana.
            Text(
                text = stringResource(R.string.rakit_pilih_kosong),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp + jarakBawah,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = katalog,
                key = { it.id },
            ) { gerakan ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPilih(gerakan) },
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FotoAlat(
                            namaFile = gerakan.thumbnailFile,
                            nama = gerakan.name,
                            modifier = Modifier.size(52.dp),
                            sudut = 12.dp,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                        ) {
                            Text(
                                text = gerakan.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = gerakan.category.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // Ikon + di kanan, bukan tanda centang. Centang berarti
                        // "sudah terpilih" dan itu bohong: satu gerakan boleh
                        // ditambahkan berkali-kali ke program yang sama, dan
                        // tabelnya memang dirancang mengizinkannya.
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.rakit_tambah),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** Nama satuan untuk sebuah tipe. Dipakai di baris ringkas dan di label kolom. */
private fun satuanDari(tipe: ExerciseType): Int =
    if (tipe == ExerciseType.HOLD) R.string.rakit_satuan_detik else R.string.rakit_satuan_reps

/**
 * Detik jadi kalimat yang enak dibaca.
 *
 * Di bawah satu menit ditulis dalam detik, karena "1 menit" untuk pemanasan 40
 * detik itu pembulatan yang menyesatkan. Di atasnya dibulatkan KE ATAS: sesi 61
 * detik lebih baik disebut 2 menit daripada 1 menit. Perkiraan waktu latihan
 * sebaiknya sedikit berlebih, bukan sedikit kurang.
 *
 * `internal`, BUKAN `private`, dan bedanya nyata di Kotlin: `private` di tingkat
 * file berarti "cuma file ini". `WorkoutCard.kt` butuh fungsi yang sama supaya
 * kartu dasbor dan layar perakit menyebut durasi dengan cara yang sama persis.
 * Dua fungsi kembar yang mengubah detik jadi menit itu undangan untuk beda
 * pembulatan -- kartunya bilang 17 menit, perakitnya bilang 18, lalu kamu curiga
 * datanya rusak padahal cuma penulisannya beda.
 *
 * `internal` = seluruh modul app boleh pakai, tapi bukan API publik. Pola yang
 * sama dengan `FotoAlat` di `AlatUi.kt`.
 */
@Composable
internal fun labelDurasi(detik: Int): String =
    if (detik < 60) {
        stringResource(R.string.rakit_detik, detik)
    } else {
        stringResource(R.string.rakit_menit, (detik + 59) / 60)
    }
