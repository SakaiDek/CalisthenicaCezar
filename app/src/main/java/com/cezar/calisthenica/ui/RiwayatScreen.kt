package com.cezar.calisthenica.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.AppDatabase
import com.cezar.calisthenica.model.ExerciseType
import com.cezar.calisthenica.model.Grup
import com.cezar.calisthenica.model.SessionExerciseLog
import com.cezar.calisthenica.model.SessionLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Format tanggal panjang untuk judul kartu: "Sabtu, 26 September 2026".
 * Locale("id") supaya nama hari & bulan bahasa Indonesia, bukan ikut bahasa
 * sistem. Dibangun SEKALI di tingkat file (bukan di dalam Composable) karena
 * membuat DateTimeFormatter itu tidak gratis -- percuma dibikin ulang tiap
 * kali layar menggambar ulang.
 */
private val FORMAT_TANGGAL: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id"))

/** Jam selesai gaya Indonesia: "19.30" (pakai titik, bukan titik dua). */
private val FORMAT_JAM: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH.mm", Locale("id"))

/**
 * Ubah teks ISO "2026-09-26" jadi "Sabtu, 26 September 2026".
 * runCatching: kalau suatu hari ada baris lama dengan tanggal aneh, jangan bikin
 * app tumbang -- cukup tampilkan teks aslinya apa adanya.
 */
private fun tanggalTampil(iso: String): String =
    runCatching { LocalDate.parse(iso).format(FORMAT_TANGGAL) }.getOrDefault(iso)

/**
 * Ubah jam-dinding millis jadi "19.30" di zona waktu HP. Instant itu titik waktu
 * absolut (UTC); .atZone(systemDefault()) yang menerjemahkannya ke jam lokal yang
 * user benar-benar lihat.
 */
private fun jamTampil(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(FORMAT_JAM)

/**
 * Layar Riwayat: daftar semua sesi yang sudah pernah kamu tuntaskan, terbaru di
 * atas. TANPA ViewModel -- sumber kebenaran ada di file database, jadi state yang
 * hilang saat rotasi langsung terisi ulang dari Flow. Sah untuk MVP.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember(context) { AppDatabase.get(context).sessionLogDao() }
    // initial = null: kita bedakan "DB belum menjawab" (null) dari "sudah
    // menjawab, memang kosong" (list kosong). Tanpa ini, EmptyState akan berkedip
    // sekejap tiap buka layar sebelum data pertama tiba.
    val muatan by dao.observeAll().collectAsState(initial = null)
    val sudahDijawab = muatan != null
    val daftar = muatan ?: emptyList()

    Scaffold(
        // Insets: Scaffold cuma bayar sisi KIRI-KANAN; jarak bawah (bilah navigasi)
        // dibayar sendiri di contentPadding LazyColumn supaya kartu terakhir tidak
        // ketutup navbar saat di-scroll mentok.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.riwayat_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.riwayat_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val jarakBawah = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        if (sudahDijawab && daftar.isEmpty()) {
            RiwayatEmptyState(Modifier.padding(innerPadding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, jarakBawah + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = daftar, key = { it.id }) { log -> RiwayatRow(log = log) }
            }
        }
    }
}

/**
 * Satu kartu riwayat, kini BISA DIBUKA. Klik badan kartu -> panel detail gerakan
 * mekar ke bawah; klik lagi -> menutup.
 *
 * PELAJARAN HARI INI: kenapa flag buka/tutup pakai `rememberSaveable(log.id)` dan
 * bukan satu variabel global. Tiap kartu wajib mengingat statusnya SENDIRI --
 * membuka kartu A tidak boleh ikut membuka/menutup kartu B. Kunci `log.id` bikin
 * status "menempel" ke sesi yang benar walau LazyColumn mendaur ulang barisnya
 * saat di-scroll. `rememberSaveable` (bukan `remember` biasa) supaya status buka
 * juga selamat saat layar diputar.
 */
// `internal` (bukan `private`) sejak Ronde 2 Kalender: KalenderScreen — masih
// satu modul `app`, paket `com.cezar.calisthenica.ui` — memakai ulang kartu ini
// untuk menampilkan sesi pada hari yang diketuk di kalender. Kartunya sudah
// mandiri (query detail sendiri saat di-expand), jadi dipinjam apa adanya tanpa
// menyalin kode. `internal` = terlihat di seluruh modul app, tapi tetap tertutup
// dari luar; cukup untuk berbagi antar-layar tanpa mengumbarnya jadi API publik.
@Composable
internal fun RiwayatRow(log: SessionLog) {
    var terbuka by rememberSaveable(log.id) { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            // Seluruh badan kartu jadi tombol buka/tutup -- target sentuh selebar
            // kartu, bukan cuma ikon kecil di pojok.
            .clickable { terbuka = !terbuka },
    ) {
        Column(Modifier.padding(16.dp)) {
            // Baris kepala: nama program menyerap sisa lebar (weight 1f) supaya
            // chevron selalu menempel di kanan, sepanjang apa pun namanya.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = log.programNama,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (terbuka) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (terbuka) R.string.riwayat_collapse else R.string.riwayat_expand,
                    ),
                )
            }
            Text(
                text = stringResource(
                    R.string.riwayat_waktu,
                    tanggalTampil(log.tanggal),
                    jamTampil(log.waktuSelesaiMillis),
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(
                    R.string.riwayat_ringkas,
                    labelDurasi(log.durasiDetik),
                    log.totalSetSelesai,
                    log.totalGerakan,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )

            // Panel detail HANYA diminta ke database saat kartu benar-benar
            // dibuka -- AnimatedVisibility tidak menyusun isinya selama tertutup,
            // jadi 50 kartu tertutup = 0 query gerakan. Baru pas dibuka, query-nya
            // jalan dan panel mekar ke bawah dengan animasi.
            AnimatedVisibility(visible = terbuka) {
                RiwayatDetail(sessionId = log.id)
            }
        }
    }
}

/**
 * Panel detail: daftar gerakan MILIK satu sesi, dibaca dari tabel anak
 * `session_exercise_logs` lewat `observeBySession(sessionId)`.
 *
 * PELAJARAN HARI INI: tiga keadaan, bukan dua. Kita pakai `initial = null` lagi
 * supaya bisa membedakan:
 *   null       -> DB belum menjawab (sekejap, jangan gambar apa-apa),
 *   list kosong -> sesi LAMA dari sebelum DB v8 yang memang tidak punya rincian
 *                  gerakan; kita jujur bilang "detail tidak tercatat", bukan
 *                  pura-pura sesinya kosong,
 *   list berisi -> gambar tiap gerakannya.
 * Kalau kita cuma pakai dua keadaan (kosong vs isi), sesi lama akan tampak seperti
 * sedang loading selamanya. Tiga keadaan bikin perbedaannya jujur.
 */
@Composable
private fun RiwayatDetail(sessionId: Long) {
    val context = LocalContext.current
    val selDao = remember(context) { AppDatabase.get(context).sessionExerciseLogDao() }
    val detail by selDao.observeBySession(sessionId).collectAsState(initial = null)

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val isi = detail
        when {
            isi == null -> Unit
            isi.isEmpty() -> Text(
                text = stringResource(R.string.riwayat_detail_kosong),
                style = MaterialTheme.typography.bodySmall,
            )
            else -> isi.forEach { item -> RiwayatDetailRow(item) }
        }
    }
}

/**
 * Satu baris gerakan di dalam panel detail: thumbnail + nama + grup di kiri,
 * beban set di kanan.
 *
 * PELAJARAN HARI INI: kenapa thumbnail-nya pakai `FotoAlat` yang sudah ada, bukan
 * `AsyncImage` baru. `FotoAlat` (di `AlatUi.kt`, satu paket dengan file ini jadi
 * tanpa import) DI DALAMNYA memang sudah memakai `AsyncImage`/Coil -- plus satu
 * bonus yang persis kita butuhkan di sini: kalau `namaFile` kosong ATAU filenya
 * tak ada lagi di disk, ia jatuh dengan anggun ke kotak placeholder, bukan crash.
 * Itu langsung menjawab syaratmu "kalau master gerakannya sudah dihapus, biarin
 * thumbnail-nya jadi placeholder kosong". Membuat komponen kedua yang isinya sama
 * cuma menambah tempat yang harus diperbaiki dua kali.
 *
 * `item.fotoUri ?: ""` -- kolom ini `String?` (baris riwayat lama bernilai null).
 * `FotoAlat` maunya String non-null, jadi null kita ubah jadi teks kosong, yang
 * olehnya sudah diperlakukan sama dengan "tidak ada foto" -> placeholder. Satu
 * jalur untuk dua sebab (sesi lama TANPA kolom, dan gerakan yang fotonya memang
 * kosong).
 *
 * Beban set diformat beda tergantung `tipe`. Untuk HOLD sengaja pakai detik
 * MENTAH (mis. "3 set x tahan 160 dtk"), BUKAN labelDurasi() -- labelDurasi
 * membulatkan ke menit untuk >=60 dtk, jadi 160 dtk akan salah tampil "3 menit"
 * di sini. Di daftar rincian, angka detik apa adanya justru lebih jujur.
 *
 * `Grup.dari(item.grup)`: nomor grup tersimpan (1/2/3) diubah balik jadi label
 * ("Pemanasan"/"Inti"/"Pendinginan"). Nomor tak dikenal jatuh ke INTI, tidak
 * bikin app tumbang.
 */
@Composable
private fun RiwayatDetailRow(item: SessionExerciseLog) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Thumbnail kotak kecil di paling kiri. 44dp cukup untuk dikenali tanpa
        // mencuri ruang teks; sudut 10dp menyenadakan dengan kartu-kartu lain.
        FotoAlat(
            namaFile = item.fotoUri ?: "",
            nama = item.namaGerakan,
            modifier = Modifier.size(44.dp),
            sudut = 10.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.namaGerakan,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = Grup.dari(item.grup).label,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = if (item.tipe == ExerciseType.HOLD) {
                stringResource(R.string.riwayat_detail_hold, item.setCount, item.target)
            } else {
                stringResource(R.string.riwayat_detail_reps, item.setCount, item.target)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Layar kosong: belum ada satu pun sesi tuntas. */
@Composable
private fun RiwayatEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.riwayat_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.riwayat_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
