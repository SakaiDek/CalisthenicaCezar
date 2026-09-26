package com.cezar.calisthenica.ui

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.AppDatabase
import com.cezar.calisthenica.data.BarisRakitan
import com.cezar.calisthenica.model.ExerciseType
import com.cezar.calisthenica.model.Grup
import com.cezar.calisthenica.model.WorkoutProgram
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.cezar.calisthenica.data.fileGambar

/**
 * ===========================================================================
 * PELAJARAN HARI INI, dan ini pelajaran terpenting di seluruh proyek sejauh
 * ini: TIMER YANG BENAR TIDAK MENGHITUNG. DIA MENGURANGI DUA TANDA WAKTU.
 * ===========================================================================
 *
 * Versi naif yang ditulis hampir semua tutorial Compose di internet:
 *
 *     var detik by remember { mutableStateOf(160) }
 *     LaunchedEffect(Unit) {
 *         while (detik > 0) { delay(1000); detik-- }
 *     }
 *
 * Kelihatan waras. Dua penyakitnya:
 *
 * 1. `delay(1000)` itu janji "TIDUR MINIMAL 1000ms", bukan "tepat 1000ms".
 *    Yang sebenarnya terjadi: 1000ms tidur, plus waktu yang dipakai Compose
 *    untuk menggambar ulang layar, plus waktu penjadwal Android memberi giliran
 *    lagi ke coroutine kita. Setiap putaran ngutang beberapa milidetik. Utangnya
 *    MENUMPUK. Setelah 160 putaran, penahan leg up the wall-mu yang seharusnya
 *    160 detik jadi 163-165 detik. Kamu tidak akan pernah sadar dari melihat
 *    layar -- angkanya turun rapi 160, 159, 158 -- padahal jamnya sudah bohong.
 *
 * 2. Angka itu cuma hidup di RAM. HP diputar ke landscape, layar sempat mati,
 *    Android membunuh app-mu di latar untuk merebut memori -> hitungannya
 *    lenyap, dan kamu harus mulai set itu dari nol.
 *
 * ViewModel -- yang saya sendiri janjikan di catatan urutan fitur -- MENYEMBUHKAN
 * PENYAKIT KEDUA DAN SAMA SEKALI TIDAK MENYENTUH YANG PERTAMA. Jadi kalau saya
 * pasang ViewModel lalu tetap menulis `detik--`, saya cuma menambah satu kelas
 * baru untuk menyimpan angka yang tetap salah.
 *
 * YANG DIPAKAI DI FILE INI: catat SATU tanda waktu saat sebuah fase dimulai
 * (`faseMulai`), lalu setiap kali menggambar layar hitung
 *
 *     sisa = target - (sekarang - faseMulai)
 *
 * Nol akumulasi, jadi mustahil ngutang. Mau layar digambar 60 kali per detik
 * atau 2 kali per detik, hasilnya identik. `delay(200)` di bawah BUKAN sumber
 * kebenaran waktu -- dia cuma alarm "ayo lihat jam lagi". Kalau dia terlambat 80
 * milidetik, angka yang muncul tetap benar, cuma munculnya sedikit terlambat.
 *
 * DAN karena seluruh keadaan sesi ini cuma beberapa angka (`Long` dan `Int`),
 * `rememberSaveable` sanggup menyelamatkannya -- bukan cuma dari rotasi, tapi
 * juga dari PROSES APP DIBUNUH, sesuatu yang ViewModel biasa tidak bisa.
 * Jadi pilihan yang lebih sederhana di sini justru yang lebih benar.
 *
 * ViewModel tetap akan datang. Tempatnya nanti: saat rest timer harus tetap
 * berdentang walau kamu keluar dari app (itu Foreground Service, dan pekerjaan
 * yang hidup di luar layar TIDAK bisa dititipkan ke composition).
 *
 * ---------------------------------------------------------------------------
 * KENAPA `SystemClock.elapsedRealtime()` DAN BUKAN `System.currentTimeMillis()`
 *
 * `currentTimeMillis` itu jam dinding: berapa milidetik sejak 1970. Nilainya
 * bisa MELOMPAT MAJU atau MUNDUR kapan saja -- operator seluler menyinkronkan
 * jam HP lewat NTP, kamu ganti zona waktu, atau kamu betulan mengubah jamnya.
 * Kalau itu terjadi di tengah penahan 160 detik, hitunganmu rusak.
 *
 * `elapsedRealtime` itu jam pengukur: berapa milidetik sejak HP terakhir
 * dinyalakan. Dia TIDAK PERNAH mundur, tidak peduli NTP, dan TETAP MENGHITUNG
 * selama HP tidur. Untuk mengukur SELANG WAKTU, ini satu-satunya jam yang benar.
 *
 * Satu sudut gelap yang layak kamu tahu: kalau HP di-reboot, `elapsedRealtime`
 * balik ke nol. Tanda waktu lama jadi lebih besar dari "sekarang", dan
 * pengurangannya negatif. Itu sebabnya ada `.coerceAtLeast(0)` di bawah -- app
 * cuma menganggap fase itu baru mulai, bukan crash. Dalam praktik ini mustahil
 * kejadian (reboot menghapus simpanan `rememberSaveable` juga), tapi penjaga
 * satu baris lebih murah daripada satu jam mencari sebab.
 * ---------------------------------------------------------------------------
 *
 * MESIN KEADAANNYA CUMA TIGA FASE, dan saya sengaja memangkasnya dari empat.
 *
 * Rancangan awal saya: SIAP, KERJA, ISTIRAHAT, SELESAI. Lalu saya sadar
 * ISTIRAHAT dan SIAP itu layar yang SAMA -- dua-duanya "kamu belum bekerja, dan
 * ada satu tombol untuk mulai". Bedanya cuma ada tidaknya hitungan yang jalan.
 * Jadi hitungan itu saya jadikan DATA (`istirahatTarget`), bukan FASE.
 *
 * Ini penerapan aturan rumah yang sudah kita pakai di Ketinggian Alat: kalau
 * dua keadaan digambar dengan kode yang sama, itu satu keadaan dengan satu
 * angka pembeda. Setiap fase yang tidak ada itu satu transisi yang tidak bisa
 * salah.
 *
 * Alur yang kamu pilih sendiri -- "istirahat auto, set nunggu ketuk" -- jadi
 * jatuh dengan sendirinya:
 *
 *   SIAP (istirahat jalan otomatis, habis -> getar) --ketuk--> KERJA
 *   KERJA tipe HOLD  --hitungan habis, otomatis--> SIAP berikutnya
 *   KERJA tipe REPS  --ketuk "Set selesai"------> SIAP berikutnya
 *   set & gerakan habis ---------------------------> SELESAI
 *
 * Perhatikan asimetrinya, karena itu keputusan sadar: yang otomatis cuma yang
 * BISA diukur mesin. HOLD punya batas waktu, jadi mesin tahu kapan selesai.
 * REPS tidak -- cuma kamu yang tahu push-up ke-12 sudah naik atau belum. Mesin
 * tidak boleh menebak sesuatu yang cuma badanmu yang tahu.
 */
private enum class Fase {
    SIAP,
    KERJA,
    SELESAI,
    ;

    companion object {
        /**
         * `rememberSaveable` menyimpan lewat Bundle Android, dan yang paling
         * aman masuk Bundle itu tipe dasar. Enum-nya memang Serializable jadi
         * SEBENARNYA bisa langsung disimpan, tapi kita simpan namanya sebagai
         * String -- gaya rumah yang sama dengan `Grup.dari` dan
         * `Ketinggian.dari`: nama tak dikenal jatuh ke nilai awal yang waras,
         * TIDAK melempar exception. Kalau suatu hari nama fase diubah,
         * sesi yang sedang jalan cuma mulai ulang, bukan bikin app crash.
         */
        fun dari(nama: String): Fase = entries.firstOrNull { it.name == nama } ?: SIAP
    }
}

/**
 * Layar eksekusi latihan. Dipanggil dari kartu dasbor lewat tombol "Mulai".
 *
 * SATU-SATUNYA yang dibaca dari database: daftar gerakan program ini, lewat
 * `observeRakitan` yang sudah dipakai layar perakit. TIDAK ada tulisan ke
 * database sama sekali di build ini -- riwayat sesi itu tabel yang belum ada,
 * dan saya lebih suka layar ini jujur bilang "belum tercatat" daripada
 * diam-diam membuang hasil kerjamu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRunnerScreen(program: WorkoutProgram, onBack: () -> Unit) {
    val context = LocalContext.current
    val peDao = remember(context) { AppDatabase.get(context).programExerciseDao() }

    // `remember(program.id)` -- KUNCINYA WAJIB. Tanpa kunci, `remember` berarti
    // "hitung sekali, simpan selamanya", dan kalau kamu keluar lalu membuka
    // program LAIN, layar ini akan menjalankan daftar gerakan program yang
    // pertama. Bug yang sama pernah saya jelaskan di layar perakit.
    val rakitanFlow = remember(program.id) { peDao.observeRakitan(program.id) }
    val rakitan by rakitanFlow.collectAsState(initial = emptyList())

    // ======================= KEADAAN SESI =======================
    // Semuanya `rememberSaveable`, dan semuanya tipe dasar. Itu bukan
    // kebetulan: bentuk data yang sederhana ini YANG MEMBUAT `rememberSaveable`
    // cukup, dan itulah kenapa ViewModel belum perlu.
    //
    // Coba bayangkan alternatifnya: kalau saya menyimpan objek `BarisRakitan`
    // yang sedang dikerjakan, saya harus mengajari Android cara membungkus
    // objek itu ke Bundle (Parcelable/Saver sendiri). Menyimpan NOMOR URUTNYA
    // saja (`langkahKe`) menghindari seluruh urusan itu -- barisnya tinggal
    // diambil ulang dari daftar. Simpan penunjuk, jangan simpan bendanya.
    var faseNama by rememberSaveable { mutableStateOf(Fase.SIAP.name) }
    var langkahKe by rememberSaveable { mutableStateOf(0) }
    var setKe by rememberSaveable { mutableStateOf(1) }

    // Tanda waktu mulainya fase sekarang. INI jantung seluruh timer.
    var faseMulai by rememberSaveable { mutableStateOf(SystemClock.elapsedRealtime()) }

    // Berapa detik istirahat yang sedang dihitung di fase SIAP. 0 = tidak ada
    // hitungan sama sekali, yaitu keadaan paling awal sesi ("Siap?").
    var istirahatTarget by rememberSaveable { mutableStateOf(0) }

    var setSelesai by rememberSaveable { mutableStateOf(0) }
    var sesiMulai by rememberSaveable { mutableStateOf(SystemClock.elapsedRealtime()) }

    // Lama sesi DIBEKUKAN saat sesi berakhir, bukan dihitung terus di layar
    // ringkasan. Kalau dihitung terus, angka "Durasi 24 menit" akan naik sendiri
    // selama kamu memandangi layar ringkasan -- lucu, tapi salah.
    var durasiSesiDetik by rememberSaveable { mutableStateOf(0) }

    var konfirmasiKeluar by remember { mutableStateOf(false) }

    // Mode "intip foto": true = tampilkan semua teks/tombol di atas foto latar;
    // false = sembunyikan semuanya, sisakan fotonya saja yang bersih. Di-reset
    // ke true tiap pindah gerakan/set (lihat `majuKe`) supaya info gerakan baru
    // selalu muncul dulu -- menyembunyikan itu niat sesaat, bukan menetap.
    var chromeTerlihat by rememberSaveable { mutableStateOf(true) }

    // Id gerakan yang detailnya sedang dibuka sebagai PANEL BAWAH (ModalBottomSheet)
    // di atas sesi. null = tidak ada panel. Ketuk afordans "^ Lihat detail gerakan"
    // untuk mengisinya; geser panel ke bawah, ketuk area gelap, atau tombol Back
    // menutupnya kembali ke null. Disimpan `rememberSaveable` supaya kalau HP
    // diputar sambil membaca detail, panelnya tidak lenyap. Long? aman ke Bundle.
    //
    // PENTING: membuka panel ini MEMBEKUKAN timer (lihat `jeda`, `jedaMulai`,
    // `bukaDetail`, `tutupDetail` di bawah). Membaca instruksi = berhenti sejenak,
    // bukan balapan dengan hitungan.
    var lihatDetailId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Tanda waktu saat panel detail DIBUKA (elapsedRealtime). 0L = tidak sedang
    // membeku. Kita simpan ini, bukan "berapa lama sudah beku", karena tanda
    // waktu tidak bisa hanyut -- lamanya jeda diturunkan saat menutup:
    // lamaJeda = sekarang - jedaMulai. `rememberSaveable` supaya beku selamat rotasi.
    var jedaMulai by rememberSaveable { mutableStateOf(0L) }

    val fase = Fase.dari(faseNama)

    // Timer BEKU selama panel detail terbuka. Satu turunan dari lihatDetailId,
    // dipakai di dua tempat: (1) menghentikan loop LaunchedEffect (lihat kunci
    // efek + early-return), (2) membekukan tampilan lewat `acuan` di bawah.
    val jeda = lihatDetailId != null

    // Satu-satunya nilai yang berubah lima kali per detik. Sengaja dipisah dari
    // semua yang lain: setiap perubahan di sini menggambar ulang layar, jadi dia
    // harus memegang SESEDIKIT mungkin. Tidak perlu `rememberSaveable` -- ini
    // cuma cermin jam, bukan data. Hilang pun langsung terisi ulang.
    var sekarang by remember { mutableStateOf(SystemClock.elapsedRealtime()) }

    val baris = rakitan.getOrNull(langkahKe)
    val ref = baris?.ref

    /*
     * Batas hitungan fase ini. 0 berarti "fase ini tidak punya batas waktu".
     *
     * Perhatikan bahwa NILAINYA DITURUNKAN, bukan disimpan. Tidak ada variabel
     * "sisaDetik" yang harus dijaga tetap sinkron dengan yang lain. Data yang
     * diturunkan tidak bisa tidak sinkron -- itu alasan yang sama kenapa kartu
     * dasbor berhenti memakai kolom `exerciseCount`.
     */
    val batasDetik = when {
        ref == null -> 0
        fase == Fase.KERJA -> if (ref.tipe == ExerciseType.HOLD) ref.target else 0
        fase == Fase.SIAP -> istirahatTarget
        else -> 0
    }

    // Titik acuan "jam sekarang". Saat BEKU (panel detail terbuka) kita pakai
    // tanda waktu saat mulai beku, bukan jam yang terus berjalan -- jadi angka
    // di layar berhenti persis di tempatnya walau `sekarang` sempat terbaca
    // sekali lagi sebelum loop mati. Selesai beku, `tutupDetail` sudah menggeser
    // `faseMulai` maju sebanyak lama beku, jadi hitungan lanjut mulus dari sini.
    val acuan = if (jeda && jedaMulai != 0L) jedaMulai else sekarang
    val lewatDetik = ((acuan - faseMulai) / 1000L).toInt().coerceAtLeast(0)
    val sisaDetik = (batasDetik - lewatDetik).coerceAtLeast(0)

    /*
     * LAYAR JANGAN TIDUR SELAMA SESI BERJALAN.
     *
     * Kenapa ini WAJIB dan bukan kenyamanan: layar mati -> Activity masuk
     * `onStop` -> Android boleh membongkar composition -> `LaunchedEffect` di
     * bawah dibatalkan. Kamu buka layar lagi dan menemukan penahan 160 detikmu
     * membeku di detik 41. Kita memang menyimpan tanda waktu jadi hitungannya
     * akan terkejar sendiri, tapi getarnya sudah telat satu menit dan itu tidak
     * ada gunanya untuk latihan.
     *
     * `onDispose` ITU WAJIB, BUKAN OPSIONAL. Tanpa dia, `keepScreenOn` tetap
     * hidup setelah kamu keluar dari layar ini, dan layar HP-mu TIDAK AKAN
     * PERNAH TIDUR LAGI sampai app dimatikan. Baterai habis dalam semalam, dan
     * kamu tidak akan menghubungkannya dengan app latihan.
     *
     * Aturan yang mau saya tanam: setiap kali kamu MENYALAKAN sesuatu di luar
     * Compose, `DisposableEffect` adalah satu-satunya tempat yang benar, karena
     * dia satu-satunya yang punya tempat resmi untuk MEMATIKANNYA lagi.
     */
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // --------- tiga fungsi kecil pengubah keadaan ---------
    // Bukan composable, cuma fungsi biasa yang menulis ke state. Ditaruh di
    // dalam supaya bisa membaca `ref`/`rakitan` versi terbaru tanpa dilempar
    // sebagai parameter berlapis-lapis.

    fun majuKe(indeksBaru: Int, setBaru: Int, istirahat: Int) {
        langkahKe = indeksBaru
        setKe = setBaru
        istirahatTarget = istirahat
        // Jam fase di-reset. `sekarang` ikut di-set supaya layar frame ini
        // langsung menampilkan angka penuh, tidak sekejap menampilkan sisa
        // hitungan fase sebelumnya.
        faseMulai = SystemClock.elapsedRealtime()
        sekarang = faseMulai
        faseNama = Fase.SIAP.name
        // Gerakan/set baru -> selalu munculkan lagi info & tombolnya. Kalau tidak
        // di-reset, kamu menekan "sembunyikan" di gerakan A lalu gerakan B ikut
        // datang telanjang tanpa nama/target -- membingungkan.
        chromeTerlihat = true
    }

    fun akhiriSesi() {
        durasiSesiDetik = ((SystemClock.elapsedRealtime() - sesiMulai) / 1000L).toInt()
        faseNama = Fase.SELESAI.name
        // Layar ringkasan tidak punya foto latar, jadi tombol "intip foto" pun
        // hilang. Kalau chrome ditinggal dalam keadaan tersembunyi dari set
        // terakhir, tidak akan ada lagi tombol untuk memunculkannya -> ringkasan
        // ikut tak terlihat. Reset di sini menutup celah itu.
        chromeTerlihat = true
    }

    fun selesaikanSet() {
        val sekarangRef = ref ?: return
        setSelesai += 1
        when {
            // Masih ada set lagi di gerakan yang sama.
            setKe < sekarangRef.setCount ->
                majuKe(langkahKe, setKe + 1, sekarangRef.istirahatDetik)
            // Gerakan ini habis, lanjut ke gerakan berikutnya. Istirahatnya
            // memakai angka gerakan yang BARU SAJA selesai, bukan yang akan
            // datang -- istirahat itu pemulihan dari kerja yang sudah lewat.
            langkahKe < rakitan.lastIndex ->
                majuKe(langkahKe + 1, 1, sekarangRef.istirahatDetik)
            else -> akhiriSesi()
        }
    }

    fun lewatiGerakan() {
        if (langkahKe < rakitan.lastIndex) {
            // Istirahat 0: kamu melewati gerakan ini berarti kamu tidak
            // bekerja, jadi tidak ada yang perlu dipulihkan.
            majuKe(langkahKe + 1, 1, 0)
        } else {
            akhiriSesi()
        }
    }

    fun mulaiKerja() {
        faseMulai = SystemClock.elapsedRealtime()
        sekarang = faseMulai
        faseNama = Fase.KERJA.name
    }

    /*
     * MUNDUR SATU SET. Kebalikan dari `selesaikanSet`, tapi dengan SATU beda
     * yang jadi inti permintaanmu: istirahat DIPAKSA 0.
     *
     * `selesaikanSet` MEMBAWA `sekarangRef.istirahatDetik` karena maju itu
     * transisi normal -- kamu baru saja bekerja, badan perlu pulih. Mundur itu
     * KOREKSI ("eh, set tadi kehitung padahal belum bener"), bukan kerja; tidak
     * ada yang perlu dipulihkan, jadi menyuruhnya menghitung istirahat lagi cuma
     * menahanmu di depan layar tanpa alasan. Makanya semua cabang mengirim 0.
     *
     * `setSelesai` diturunkan (tak boleh minus). Kalau tidak, mundur lalu maju
     * lagi menghitung set yang sama DUA KALI di ringkasan akhir -- angka bohong.
     * Diturunkan HANYA saat benar-benar berpindah; kalau sudah mentok di paling
     * awal, tidak ada yang berubah dan hitungannya pun tidak disentuh.
     */
    fun mundurSet() {
        when {
            // Masih ada set sebelumnya di gerakan yang sama.
            setKe > 1 -> {
                setSelesai = (setSelesai - 1).coerceAtLeast(0)
                majuKe(langkahKe, setKe - 1, 0)
            }
            // Sudah di set 1: mundur ke gerakan sebelumnya, ke SET TERAKHIRNYA.
            // `setCount`-nya dibaca dari rakitan, bukan ditebak -- gerakan lain
            // bisa saja punya jumlah set berbeda.
            langkahKe > 0 -> {
                setSelesai = (setSelesai - 1).coerceAtLeast(0)
                val sebelum = rakitan.getOrNull(langkahKe - 1)
                majuKe(langkahKe - 1, sebelum?.ref?.setCount ?: 1, 0)
            }
            // Gerakan 1, set 1: paling awal, tidak ada yang bisa dimundurkan.
            // Tombolnya memang sudah dimatikan di keadaan ini; ini jaring pengaman.
            else -> Unit
        }
    }

    /*
     * MEMBUKA & MENUTUP PANEL DETAIL = MEMBEKUKAN & MELANJUTKAN TIMER.
     *
     * Ini jantung fitur "pause". Hukum timer app ini tidak berubah: kita tetap
     * MENGURANGI dua tanda waktu, tidak pernah menjumlah. Yang kita lakukan cuma
     * MENGGESER titik nolnya.
     *
     * `bukaDetail` mencatat KAPAN beku dimulai (`jedaMulai`) lalu membuka panel.
     * Begitu `lihatDetailId` terisi, `jeda` jadi true -> loop di atas berhenti
     * (early-return) dan `acuan` mengunci tampilan di `jedaMulai`. Angka membeku.
     *
     * `tutupDetail` menghitung SUDAH BERAPA LAMA beku (sekarang - jedaMulai), lalu
     * MENGGESER `faseMulai` DAN `sesiMulai` maju sebanyak itu. Efeknya: waktu yang
     * dihabiskan membaca instruksi seolah tidak pernah terjadi bagi hitungan --
     * `lewat = acuan - faseMulai` otomatis mengecualikan durasi beku. `sesiMulai`
     * ikut digeser supaya total durasi di ringkasan akhir juga tidak menghitung
     * waktu baca.
     *
     * Pergeseran ini dilakukan SINKRON di sini, BUKAN di dalam LaunchedEffect.
     * Kalau ditunda ke efek, ada satu frame di mana panel sudah tertutup
     * (`jeda` false) tapi `faseMulai` belum digeser -- loop bangun, melihat
     * `faseMulai` lama, mengira hitungan sudah lewat, lalu getar + pindah set
     * SENDIRI. Menutup panel tidak boleh menuntaskan set. Menggeser di sini,
     * sebelum `lihatDetailId` di-null-kan, menutup celah balapan itu.
     */
    fun bukaDetail(exerciseId: Long) {
        jedaMulai = SystemClock.elapsedRealtime()
        lihatDetailId = exerciseId
    }

    fun tutupDetail() {
        if (jedaMulai != 0L) {
            val lamaJeda = SystemClock.elapsedRealtime() - jedaMulai
            faseMulai += lamaJeda
            sesiMulai += lamaJeda
            sekarang = SystemClock.elapsedRealtime()
            jedaMulai = 0L
        }
        lihatDetailId = null
    }

    /*
     * JAM BERDETAK + TRANSISI OTOMATIS.
     *
     * Kunci-kuncinya penting semua, dan `baris` yang paling gampang dilupakan:
     * frame PERTAMA layar ini pasti punya `rakitan` kosong (`initial =
     * emptyList()` di atas), jadi `baris` masih null dan `batasDetik` masih 0.
     * Tanpa `baris` sebagai kunci, efek ini akan lahir dengan batas 0 lalu TIDAK
     * PERNAH lahir ulang saat datanya datang -- timer HOLD-mu tidak akan pernah
     * berhenti sendiri. Jenis bug yang cuma muncul di HP betulan, tidak pernah
     * di kepala.
     *
     * `delay(200)` dan bukan `delay(1000)`: dengan 1000 ms, tampilan bisa
     * ketinggalan hampir satu detik penuh dari kenyataan -- kamu melihat "3"
     * padahal sudah lewat 0. Dengan 200 ms, ketinggalan maksimalnya seperlima
     * detik, mata tidak menangkapnya, dan ongkosnya tetap remeh (lima kali
     * gambar ulang per detik untuk SATU angka).
     *
     * `break` setelah transisi, bukan `continue`: begitu keadaan berubah, kunci
     * efek ini berubah, dan Compose akan MEMBATALKAN loop ini lalu menjalankan
     * yang baru. Membiarkannya lanjut berarti dua loop hidup bersamaan sekejap.
     *
     * `jeda` juga jadi kunci: saat panel detail terbuka (`jeda` true) efek ini
     * lahir ulang dan langsung berhenti (early-return di bawah) -- loop mati,
     * `sekarang` berhenti diperbarui, tidak ada getar/pindah set liar selama
     * kamu membaca. Menutup panel menggeser `faseMulai` (lihat `tutupDetail`),
     * yang juga kunci, jadi efek lahir ulang lagi dan hitungan lanjut mulus.
     */
    LaunchedEffect(fase, langkahKe, setKe, faseMulai, baris, jeda) {
        if (fase == Fase.SELESAI) return@LaunchedEffect
        if (jeda) return@LaunchedEffect
        while (true) {
            sekarang = SystemClock.elapsedRealtime()
            if (batasDetik > 0 && sekarang - faseMulai >= batasDetik * 1000L) {
                // Getar dulu, baru ubah keadaan. Kalau dibalik, getarnya
                // dijalankan setelah Compose sibuk menggambar layar baru dan
                // terasa terlambat.
                getar(context, if (fase == Fase.KERJA) GETAR_KERJA else GETAR_ISTIRAHAT)
                // Habisnya hitungan KERJA berarti setnya tuntas. Habisnya
                // hitungan SIAP cuma berarti istirahatnya sudah cukup -- tidak
                // ada yang berpindah, tombolnya menunggumu. Itu persis pilihan
                // "istirahat auto, set nunggu ketuk".
                if (fase == Fase.KERJA) selesaikanSet()
                break
            }
            delay(200)
        }
    }

    /*
     * BackHandler-nya WAJIB, bukan pilihan: app ini immersive, jadi tombol Back
     * sistem tidak kelihatan, dan BackHandler induk di `CalisthenicaApp` sedang
     * tidur (`enabled = screen != Screen.HOME`, dan kita masih di HOME).
     *
     * Sesi yang sedang jalan TIDAK boleh hilang karena satu gestur geser tak
     * sengaja -- tanganmu basah dan kamu sedang di tengah set. Jadi Back
     * memunculkan konfirmasi, kecuali di layar ringkasan yang memang sudah
     * tidak ada yang bisa hilang.
     */
    BackHandler {
        if (fase == Fase.SELESAI) onBack() else konfirmasiKeluar = true
    }

    val jarakBawah = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    /*
     * ARSITEKTUR IMERSIF (dirombak 25 Sept 2026). Dulu layar ini `Column`: foto
     * jadi thumbnail 140dp kecil di tengah, ditumpuk teks di atas latar polos --
     * kaku, gaya lama. Sekarang tiga LAPIS dalam satu `Box` akar:
     *   lapis 1  foto gerakan PENUH sebagai wallpaper (Crop, fillMaxSize),
     *   lapis 2  scrim gradient hitam supaya teks tetap terbaca di atas foto,
     *   lapis 3  Scaffold TEMBUS PANDANG berisi semua teks/tombol yang melayang.
     *
     * KENAPA `Crop` DI SINI BUKAN MELANGGAR HUKUM "foto user tak pernah dipotong":
     * layar detail gerakan tetap `Fit` (utuh) -- itu tempat MEMPELAJARI bentuk.
     * Runner adalah MOMEN AKSI, bukan momen belajar; di sini yang penting foto
     * mengisi layar tanpa bilah hitam, dan tepi yang tersembunyi masih bisa
     * dilihat lengkap di layar detail. Beda peran, beda aturan.
     *
     * `fotoLatar` hanya untuk fase SIAP/KERJA. Layar SELESAI tak punya `baris`,
     * jadi sengaja null di sana -> `adaFoto` false -> ringkasan tampil di atas
     * latar biasa, bukan foto.
     */
    val fotoLatar = if (fase != Fase.SELESAI) baris?.thumbnail?.takeIf { it.isNotBlank() } else null
    val berkasLatar = remember(fotoLatar) { fotoLatar?.let { fileGambar(context, it) } }
    val adaFoto = fotoLatar != null

    // Di atas foto yang digelapkan scrim, teks WAJIB terang -- tak peduli HP mode
    // terang atau gelap. `onSurfaceVariant` bawaan tema bisa jadi abu gelap di
    // mode terang lalu hilang di atas foto. Jadi teks redup kita paksa putih
    // transparan HANYA saat melayang di atas foto; selain itu ikut tema.
    val warnaRedup = if (adaFoto) {
        Color.White.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Sumber interaksi milik foto latar. Dipakai supaya ketukan foto TIDAK
    // memunculkan riak (ripple) -- riak selebar layar itu norak. Diingat di sini
    // (bukan di dalam `if (adaFoto)`) supaya dipanggil tanpa syarat tiap komposisi;
    // `remember` di dalam cabang if itu rawan reset diam-diam saat cabangnya
    // berganti.
    val fotoInteraksi = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Warna dasar Box akar HANYA terlihat saat tidak ada foto (mis. layar
            // SELESAI). Saat ada foto, foto Crop menutupinya penuh tanpa celah.
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (adaFoto) {
            AsyncImage(
                model = berkasLatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // Ketuk foto -> buka detail gerakan (panel bawah). Timer
                    // otomatis BEKU selama panel terbuka (lihat `bukaDetail`).
                    // Hanya aktif saat chrome tampil: itu tanda kamu sedang
                    // "membaca" gerakan, bukan sedang menikmati foto bersih.
                    // `indication = null` mematikan riak selebar layar. Ketukan di
                    // area kosong pun sampai ke sini karena scrim & Column di
                    // atasnya tidak menyerap sentuhan.
                    .clickable(
                        interactionSource = fotoInteraksi,
                        indication = null,
                        enabled = chromeTerlihat && ref != null,
                    ) {
                        ref?.let { bukaDetail(it.exerciseId) }
                    },
            )
            // Scrim cuma saat chrome tampil. Menekan "intip foto" mematikan scrim
            // juga, supaya fotonya benar-benar bersih tanpa lapisan gelap.
            if (chromeTerlihat) {
                // KERJA: lebih gelap & rata -- timer di tengah wajib terbaca
                // sekilas walau kamu gemetar di detik terakhir sebuah penahan.
                // SIAP: tengahnya sengaja bening supaya bentuk gerakan di foto
                // terlihat; gelap cuma di pucuk (judul) dan dasar (tombol).
                val scrim = if (fase == Fase.KERJA) {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.5f to Color.Black.copy(alpha = 0.45f),
                        1f to Color.Black.copy(alpha = 0.78f),
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.50f),
                        0.4f to Color.Black.copy(alpha = 0.12f),
                        1f to Color.Black.copy(alpha = 0.70f),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrim),
                )
            }
        }

        Scaffold(
            // Tembus pandang: warna latar diserahkan ke Box akar + foto di
            // belakangnya. Tanpa ini Scaffold menutup fotomu dengan warna solid.
            containerColor = Color.Transparent,
            topBar = {
                // Bilah atas ikut tembus pandang; judul + X + tombol "intip"
                // berdiri langsung di atas foto. Saat chrome disembunyikan, judul
                // & X ditiadakan -- sisakan tombol intip supaya UI bisa balik.
                TopAppBar(
                    title = {
                        if (chromeTerlihat) {
                            Text(
                                text = program.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        if (chromeTerlihat) {
                            IconButton(
                                onClick = {
                                    if (fase == Fase.SELESAI) onBack() else konfirmasiKeluar = true
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.run_close),
                                )
                            }
                        }
                    },
                    // CATATAN 25 Sept 2026: tombol "intip foto" DIPINDAH dari sini
                    // (pojok kanan-atas) ke pojok kanan-BAWAH, mengambang di Box
                    // akar. Alasannya ada di komentar tombol itu. `actions`
                    // sengaja dikosongkan, bukan diisi tombol lain.
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        // Di atas foto: putih. Tanpa foto (layar SELESAI): ikut tema.
                        titleContentColor = if (adaFoto) Color.White else MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = if (adaFoto) Color.White else MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = if (adaFoto) Color.White else MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
            // Pola baku app ini: kiri-kanan dihormati, bawah dinolkan dan dibayar
            // sendiri di bawah supaya isi bisa lewat di belakang bilah navigasi.
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            // Semua teks/tombol disembunyikan saat kamu memilih "intip foto"
            // (chrome off) DAN memang ada foto untuk diintip. Tanpa foto tidak
            // ada yang perlu disembunyikan, jadi isinya selalu tampil.
            if (chromeTerlihat || !adaFoto) {
                // Di atas foto, paksa warna konten jadi terang lewat
                // LocalContentColor -- semua Text tanpa `color` eksplisit (nama
                // gerakan, angka timer besar) ikut jadi putih. Yang masih pakai
                // warna tema (primary oranye) sengaja dibiarkan: oranye tetap
                // terbaca di atas scrim gelap, dan dia penanda identitas app.
                CompositionLocalProvider(
                    LocalContentColor provides
                        if (adaFoto) Color.White else MaterialTheme.colorScheme.onSurface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp),
                    ) {
                        if (rakitan.isEmpty()) {
                            /*
                             * PENJAGA FRAME PERTAMA. `collectAsState(initial =
                             * emptyList())` memaksa frame pertama berisi daftar
                             * KOSONG, selalu -- bahkan untuk program penuh. Kalau
                             * langsung `rakitan[langkahKe]`, app mati dengan
                             * IndexOutOfBounds di frame pertama. Aturan seluruh
                             * app: FRAME PERTAMA SELALU KOSONG, itu keadaan SAH.
                             */
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.run_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = warnaRedup,
                                )
                            }
                        } else if (fase == Fase.SELESAI) {
                            IsiSelesai(
                                setSelesai = setSelesai,
                                jumlahGerakan = rakitan.size,
                                durasiDetik = durasiSesiDetik,
                                jarakBawah = jarakBawah,
                                onTutup = onBack,
                            )
                        } else {
                            // ---------------- kepala: posisi di dalam program ----------------
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.run_progress, langkahKe + 1, rakitan.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = warnaRedup,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                // Versi lambda, BUKAN Float langsung (deprecated di
                                // M3): lambda cuma dibaca saat menggambar, jadi angka
                                // tiap 200ms tidak memaksa seluruh layar dikomposisi.
                                progress = { (langkahKe + 1).toFloat() / rakitan.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                            )

                            // ---------------- tengah: isi fase ----------------
                            // Dulu `Box(weight(1f))`. Tapi `Box` MENGUNCI tinggi
                            // anaknya ke tinggi kotak: saat fase ISTIRAHAT isinya
                            // membengkak (hitungan mundur + bar + pratinjau gerakan
                            // berikutnya), lalu bagian bawah -- afordans "^ Lihat
                            // detail gerakan" -- terdorong KELUAR layar dan hilang
                            // diam-diam. `BottomCenter` pun tak menolong karena anak
                            // sudah sebesar kotak. Itulah "tombol detail lenyap pas
                            // istirahat" yang kamu temukan.
                            //
                            // Obatnya: Column yang BISA di-scroll, tinggi minimum =
                            // tinggi ruang (dari BoxWithConstraints). Kalau isi MUAT,
                            // `heightIn(min)` menahan Column setinggi ruang jadi
                            // `Arrangement.Center`/`Bottom` tetap bekerja seperti dulu.
                            // Kalau isi LEBIH TINGGI dari ruang, Column ikut isi dan
                            // bisa digeser -- tak ada lagi yang terpotong diam-diam.
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) {
                                val tinggiRuang = maxHeight
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .heightIn(min = tinggiRuang)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    // Di atas foto, fase SIAP duduk di DASAR supaya
                                    // bagian atas foto (badan/bentuk gerakan) terlihat.
                                    // KERJA tetap di TENGAH -- timer fokus mata.
                                    verticalArrangement = if (fase != Fase.KERJA && adaFoto) {
                                        Arrangement.Bottom
                                    } else {
                                        Arrangement.Center
                                    },
                                ) {
                                    // `baris` mestinya tidak null di sini, tapi
                                    // `langkahKe` BISA melewati ujung kalau gerakan
                                    // dihapus dari perakit saat sesi masih hidup di
                                    // latar. `?:`/null-check ini menutup celah tanpa crash.
                                    val isi = baris
                                    if (isi == null) {
                                        Text(
                                            text = stringResource(R.string.run_loading),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = warnaRedup,
                                        )
                                    } else if (fase == Fase.KERJA) {
                                        IsiKerja(
                                            baris = isi,
                                            setKe = setKe,
                                            sisaDetik = sisaDetik,
                                            lewatDetik = lewatDetik,
                                            warnaRedup = warnaRedup,
                                            onBukaDetail = { bukaDetail(isi.ref.exerciseId) },
                                        )
                                    } else {
                                        IsiSiap(
                                            baris = isi,
                                            setKe = setKe,
                                            istirahatTarget = istirahatTarget,
                                            sisaDetik = sisaDetik,
                                            warnaRedup = warnaRedup,
                                            onBukaDetail = { bukaDetail(isi.ref.exerciseId) },
                                        )
                                    }
                                }
                            }

                            // ---------------- bawah: tombol aksi ----------------
                            if (ref != null) {
                                if (fase == Fase.KERJA) {
                                    // KERJA = satu tombol lebar saja. Panah "mundur
                                    // set" SENGAJA tak ada di sini (revisi 25 Sept
                                    // 2026, permintaan Sakai): begitu set jalan,
                                    // matamu di badan bukan di layar, dan mundur di
                                    // tengah kerja itu salah waktu. Undo-nya pindah
                                    // ke fase SIAP, sejajar tombol "Mulai".
                                    Button(
                                        onClick = { selesaikanSet() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                    ) {
                                        Text(
                                            text = if (ref.tipe == ExerciseType.HOLD) {
                                                stringResource(R.string.run_stop_early)
                                            } else {
                                                stringResource(R.string.run_set_done)
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    }
                                    // Penyeimbang tinggi "Lewati" yang cuma ada di
                                    // SIAP -- dasar layar tak melompat ganti fase.
                                    Spacer(modifier = Modifier.height(8.dp))
                                } else {
                                    // SIAP/ISTIRAHAT: tombol "Mulai set N" berbagi
                                    // baris dengan panah "mundur set" di KIRI. DI
                                    // SINILAH mundur paling masuk akal (revisi 25
                                    // Sept 2026, permintaan Sakai) -- sebagai Undo
                                    // SEBELUM mulai: salah pencet, atau mau ulang
                                    // set sebelumnya. Sejajar tombol Mulai biar
                                    // sekali lihat, sekali jangkau jempol.
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Dimatikan HANYA di gerakan 1 set 1: tak
                                        // ada apa pun untuk dimundurkan. Tombol
                                        // kelabu MENJELASKAN itu, bukan diam gagal.
                                        IconButton(
                                            onClick = { mundurSet() },
                                            enabled = !(langkahKe == 0 && setKe == 1),
                                            modifier = Modifier.size(56.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(R.string.run_mundur),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { mulaiKerja() },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.run_start_set, setKe),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        }
                                    }
                                    // "Lewati" cuma ada di SIAP dan itu sengaja.
                                    // Melewati gerakan yang sudah dikerjakan itu
                                    // niat membingungkan -- setnya dihitung atau
                                    // tidak? Tombolnya hilang, pertanyaannya hilang.
                                    TextButton(
                                        onClick = { lewatiGerakan() },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.run_skip),
                                            color = warnaRedup,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp + jarakBawah))
                        }
                    }
                }
            }
        }

        // Tombol "intip foto" -- PINDAH dari pojok kanan-ATAS (revisi 25 Sept
        // 2026). Dulu dia di `actions` TopAppBar, tapi foto latihan Sakai itu
        // screenshot ber-UI: ada ikon share di kanan-atas & panah navigasi di
        // tengah-kanan. Tombol kita jadi nyaru, sampai panah DI FOTO dikira
        // tombol "next set". Dipindah ke kanan-BAWAH, jauh dari dua zona jebakan
        // itu. Muncul HANYA kalau ada foto -- tanpa foto tak ada yang perlu
        // diintip. Sengaja TIDAK ikut disembunyikan `chromeTerlihat`: justru saat
        // chrome mati, tombol inilah satu-satunya jalan memunculkannya lagi.
        if (adaFoto) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // Diangkat jauh dari dasar supaya bersih dari tumpukan tombol
                    // (utama 56dp + "Lewati" ~48dp) DAN bilah navigasi HP.
                    .padding(end = 16.dp, bottom = jarakBawah + 132.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    // Lingkaran gelap: ikon putih tetap terbaca di atas foto
                    // terang maupun gelap. Sama resep dengan tombol di FotoLayarPenuh.
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                    .clickable { chromeTerlihat = !chromeTerlihat },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconMenuhiLayar,
                    contentDescription = if (chromeTerlihat) {
                        stringResource(R.string.run_sembunyikan_ui)
                    } else {
                        stringResource(R.string.run_tampilkan_ui)
                    },
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Detail gerakan sebagai PANEL BAWAH (ModalBottomSheet) -- anak TERAKHIR
        // Box akar. KENAPA panel, BUKAN ganti layar: runner tetap TERKOMPOSISI di
        // belakang, jadi state timer (`rememberSaveable`) TIDAK dibuang. Ganti
        // layar (Navigation) akan membuang runner + state timer, hitungan reset
        // saat balik -- persis yang Sakai larang.
        //
        // KENAPA ModalBottomSheet, BUKAN overlay full-screen (revisi 25 Sept 2026):
        // overlay lama memuat ULANG foto gerakan padahal foto itu SUDAH jadi
        // wallpaper di belakang -- redundan & berat. Panel ini teks-saja (Alat,
        // Otot, Cara), muncul dari bawah, foto latar tetap kelihatan di atasnya.
        // Bisa ditutup dengan geser ke bawah, ketuk area gelap, atau Back (semua
        // lewat onDismissRequest di dalam PanelDetailGerakan).
        //
        // TIMER BEKU SELAMA PANEL TERBUKA (revisi 26 Sept 2026). Dulu timer jalan
        // terus di latar -- sesi bisa maju fase / bergetar sendiri saat kamu baca.
        // Sekarang membuka panel = jeda: `bukaDetail` mencatat waktu, loop timer
        // berhenti, angka membeku; `tutupDetail` menggeser `faseMulai`/`sesiMulai`
        // maju sebanyak lama beku, jadi hitungan lanjut mulus dari titik yang sama.
        val detailId = lihatDetailId
        if (detailId != null) {
            PanelDetailGerakan(
                exerciseId = detailId,
                onTutup = { tutupDetail() },
            )
        }
    }


    if (konfirmasiKeluar) {
        AlertDialog(
            onDismissRequest = { konfirmasiKeluar = false },
            title = { Text(text = stringResource(R.string.run_quit_title)) },
            text = { Text(text = stringResource(R.string.run_quit_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        konfirmasiKeluar = false
                        onBack()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.run_quit_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiKeluar = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

/**
 * Isi layar saat kamu SEDANG bekerja.
 *
 * Angka besarnya beda arti tergantung tipe, dan itu memang inti dari satu kolom
 * `target` dua arti yang kita pilih di `ProgramExerciseRef`:
 *
 * - HOLD: hitungan MUNDUR. Yang kamu butuh tahu cuma "berapa lagi".
 * - REPS: jumlah repetisi SASARAN, angka yang diam. Waktu tetap ditampilkan
 *   tapi kecil, karena bukan dia yang menentukan kapan setnya selesai.
 */
@Composable
private fun IsiKerja(
    baris: BarisRakitan,
    setKe: Int,
    sisaDetik: Int,
    lewatDetik: Int,
    // Warna teks redup. Default = warna tema (dipakai saat TANPA foto latar);
    // saat melayang di atas foto, pemanggil mengirim putih-transparan supaya
    // terbaca di atas scrim gelap. Lihat alasan lengkap di badan SessionRunnerScreen.
    warnaRedup: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    // Dipanggil saat afordans "^ Lihat detail gerakan" diketuk -> buka panel
    // detail (timer otomatis BEKU selama panel terbuka). Nullable supaya fungsi
    // ini tetap bisa dipakai di tempat tanpa detail; kalau null, afordansnya tak
    // dirakit. NAMA gerakan BUKAN lagi tombol (revisi 26 Sept 2026: dulu dobel-
    // pemicu dengan afordans + riaknya kotak kaku) -- pemicunya kini cuma afordans.
    onBukaDetail: (() -> Unit)? = null,
) {
    val ref = baris.ref
    val hold = ref.tipe == ExerciseType.HOLD

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = Grup.dari(ref.grup).label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = baris.namaGerakan,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            // NAMA = teks biasa, BUKAN tombol (revisi 26 Sept 2026). Dulu dia ikut
            // membuka detail, tapi itu pemicu KEDUA yang dobel dengan afordans
            // "^ Lihat detail" di bawah, dan riak ketuknya membentuk kotak kaku.
            // Satu pemicu, satu tempat -- interaksi lebih mulus & nyatu.
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.run_set, setKe, ref.setCount),
            style = MaterialTheme.typography.bodyMedium,
            color = warnaRedup,
        )

        // Ajakan buka detail: ikon panah-atas + label kecil. Hanya muncul kalau
        // ada penerima ketukan. Ketuk -> panel detail naik (timer BEKU).
        if (onBukaDetail != null) {
            Spacer(modifier = Modifier.height(4.dp))
            PetunjukDetail(onBuka = onBukaDetail, warna = warnaRedup)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (hold) jamPasir(sisaDetik) else ref.target.toString(),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (hold) {
                stringResource(R.string.run_remaining)
            } else {
                stringResource(R.string.run_unit_reps)
            },
            style = MaterialTheme.typography.labelLarge,
            color = warnaRedup,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (hold) {
            // Batang kemajuan SET INI, terpisah dari batang kemajuan program di
            // atas. Dua batang berbeda arti, dan yang ini yang kamu lihat sambil
            // gemetar di detik terakhir.
            LinearProgressIndicator(
                progress = {
                    if (ref.target <= 0) 0f else 1f - sisaDetik.toFloat() / ref.target
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.run_elapsed, jamPasir(lewatDetik)),
                style = MaterialTheme.typography.bodySmall,
                color = warnaRedup,
            )
        }

        if (ref.catatan.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Catatan(teks = ref.catatan)
        }
    }
}

/**
 * Isi layar saat kamu BELUM bekerja: entah baru buka sesi, entah sedang
 * istirahat setelah set sebelumnya.
 *
 * FOTO SUDAH TIDAK ADA DI SINI (dirombak 25 Sept 2026). Dulu fase SIAP
 * menggambar thumbnail 140dp lewat `FotoBisaDiperbesar`. Sekarang fotonya jadi
 * WALLPAPER seluruh layar di badan `SessionRunnerScreen`, jadi di sini tinggal
 * teksnya saja yang melayang. Alasan foto tetap milik fase SIAP/istirahat dan
 * bukan KERJA tidak berubah: foto itu pengingat bentuk gerakan, dilihat SEBELUM
 * mulai; di tengah set matamu tidak di HP.
 */
@Composable
private fun IsiSiap(
    baris: BarisRakitan,
    setKe: Int,
    istirahatTarget: Int,
    sisaDetik: Int,
    warnaRedup: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    // Sama seperti di IsiKerja: dipanggil oleh afordans "^ Lihat detail gerakan"
    // (timer BEKU saat panel terbuka). NAMA bukan lagi tombol. null = afordans
    // tak dirakit.
    onBukaDetail: (() -> Unit)? = null,
) {
    val ref = baris.ref
    val sedangIstirahat = istirahatTarget > 0 && sisaDetik > 0

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when {
                sedangIstirahat -> stringResource(R.string.run_rest)
                istirahatTarget > 0 -> stringResource(R.string.run_rest_done)
                else -> stringResource(R.string.run_ready)
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (sedangIstirahat) {
                warnaRedup
            } else {
                MaterialTheme.colorScheme.primary
            },
        )

        if (istirahatTarget > 0) {
            Text(
                text = jamPasir(sisaDetik),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 1f - sisaDetik.toFloat() / istirahatTarget },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
        }

        // Foto gerakan TIDAK digambar di sini lagi -- sejak 25 Sept 2026 dia jadi
        // wallpaper seluruh layar (lihat badan SessionRunnerScreen). Yang tersisa
        // di fase SIAP tinggal teks yang melayang di atas foto itu.
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.run_next),
            style = MaterialTheme.typography.labelSmall,
            color = warnaRedup,
        )
        Text(
            text = baris.namaGerakan,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            // NAMA = teks biasa, bukan tombol (revisi 26 Sept 2026). Pemicu detail
            // dipusatkan di afordans "^ Lihat detail" di bawah, bukan di nama.
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.run_set, setKe, ref.setCount),
            style = MaterialTheme.typography.bodyMedium,
            color = warnaRedup,
        )
        Text(
            text = if (ref.tipe == ExerciseType.HOLD) {
                stringResource(R.string.run_target_hold, jamPasir(ref.target))
            } else {
                stringResource(R.string.run_target_reps, ref.target)
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        // Ajakan buka detail (sama seperti di IsiKerja). Ketuk -> panel detail
        // teks naik dari bawah, timer BEKU. Selalu ada di fase SIAP/ISTIRAHAT
        // -- justru saat istirahat inilah momen belajar gerakan berikutnya.
        if (onBukaDetail != null) {
            Spacer(modifier = Modifier.height(8.dp))
            PetunjukDetail(onBuka = onBukaDetail, warna = warnaRedup)
        }

        if (ref.catatan.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Catatan(teks = ref.catatan)
        }
    }
}

/**
 * Ajakan "buka detail" yang dipasang di bawah nama/target gerakan di IsiKerja &
 * IsiSiap. SATU pemicu, KLIK saja (revisi 26 Sept 2026).
 *
 * Dulu node ini punya dua gestur: ketuk DAN geser-ke-atas (detectVerticalDrag).
 * Geser DIBUANG karena isi tengah layar sekarang BISA di-scroll (itu perbaikan
 * "tombol detail lenyap pas istirahat") -- swipe-up pada panah akan berebut
 * dengan gestur scroll dan terasa patah. Klik itu jalur andalan yang kamu sendiri
 * minta ("atau mengkliknya"), dan panel ModalBottomSheet toh sudah bisa ditarik/
 * digeser sendiri begitu terbuka. Jadi tak ada yang benar-benar hilang.
 *
 * RIAK (ripple) DIMATIKAN (`indication = null`). Ripple bawaan menggambar kotak
 * kaku di sekeliling node -- di atas foto latar terlihat seperti "card" nyempil,
 * persis yang kamu keluhkan. Tanpa indication, ketukan tetap jalan tapi mulus &
 * nyatu dengan latar. Resep yang sama dipakai foto latar (fotoInteraksi).
 * `onClickLabel` tetap ada supaya TalkBack mengumumkan "Lihat detail gerakan".
 *
 * `warna` diwarisi dari `warnaRedup` pemanggil supaya ikut aturan kontras di atas
 * foto (putih-transparan saat ada wallpaper, warna tema saat tidak).
 */
@Composable
private fun PetunjukDetail(
    onBuka: () -> Unit,
    warna: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.run_lihat_detail),
                onClick = onBuka,
            )
            .padding(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = warna,
        )
        Text(
            text = stringResource(R.string.run_lihat_detail),
            style = MaterialTheme.typography.labelSmall,
            color = warna,
        )
    }
}

/**
 * Layar ringkasan. Sengaja MENGAKUI bahwa hasilnya belum disimpan.
 *
 * Ini penerapan aturan yang sudah kita pegang sejak kartu dasbor berhenti
 * menampilkan "Group 1 - Pemanasan" palsu: app tidak boleh terlihat lebih jadi
 * daripada isinya. Angka-angka di sini nyata untuk sesi yang baru saja kamu
 * kerjakan, dan catatan di bawahnya jujur bahwa besok angka itu sudah hilang.
 */
@Composable
private fun IsiSelesai(
    setSelesai: Int,
    jumlahGerakan: Int,
    durasiDetik: Int,
    jarakBawah: Dp,
    onTutup: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.run_finish_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.run_finish_sets, setSelesai),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            // `labelDurasi` dipakai ulang dari layar perakit -- fungsi yang sama
            // yang bikin kartu dasbor menulis "sekitar 18 menit". Satu cara
            // menulis durasi di seluruh app.
            text = stringResource(
                R.string.run_finish_body,
                jumlahGerakan,
                labelDurasi(durasiDetik),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = stringResource(R.string.run_finish_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onTutup,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(text = stringResource(R.string.run_finish_close))
        }
        Spacer(modifier = Modifier.height(12.dp + jarakBawah))
    }
}

/** Kotak catatan bebas ("rompi 5kg", "tempo turun 3 detik"). */
@Composable
private fun Catatan(teks: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = teks,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/**
 * Detik jadi "2:40".
 *
 * KENAPA TIDAK `String.format("%d:%02d", ...)`, dan ini jebakan yang nyata:
 * `String.format` tanpa `Locale` memakai locale HP. Di beberapa locale
 * (contohnya `ar-EG`) hasilnya angka Arab-Indic -- ٢:٤٠ -- di tengah layar
 * latihan berbahasa Indonesia. Menempelkan digit sendiri tidak punya locale
 * untuk disalahpahami.
 *
 * Kenapa MM:SS dan bukan "160 detik": mata membaca "2:40" sebagai LAMA WAKTU
 * tanpa berpikir, sementara "160" harus dibagi enam puluh di kepala dulu.
 * Sedang menahan posisi bukan saat yang baik untuk berhitung.
 */
private fun jamPasir(detik: Int): String {
    val aman = detik.coerceAtLeast(0)
    val sisa = aman % 60
    return "${aman / 60}:${if (sisa < 10) "0$sisa" else "$sisa"}"
}

/**
 * Getar sekali.
 *
 * DUA CABANG API, dan bukan karena rewel: `VibratorManager` baru ada di API 31
 * (Android 12). Di bawahnya harus lewat `Vibrator` langsung. minSdk kita 26,
 * jadi kedua jalan wajib ada -- kalau saya cuma menulis jalan yang baru,
 * app-nya crash di HP Android 8-11 dengan NoClassDefFoundError, dan build-nya
 * tetap HIJAU karena compileSdk kita 34 tahu kelas itu ada.
 *
 * `hasVibrator()` diperiksa karena tidak semua perangkat Android punya motor
 * getar (emulator, tablet murah, TV). Memanggil `vibrate()` di situ tidak
 * crash, tapi memeriksanya bikin niat kodenya jelas.
 *
 * Ini butuh `<uses-permission android:name="android.permission.VIBRATE" />` di
 * manifest. Izin itu tingkat NORMAL: Android memberikannya otomatis saat
 * install, TIDAK ada dialog yang muncul ke user. Beda kelas dengan kamera atau
 * lokasi yang harus diminta saat app berjalan. Tapi kalau barisnya tidak ada di
 * manifest, `vibrate()` melempar SecurityException -- gagal senyap yang cuma
 * kelihatan di logcat.
 */
private fun getar(context: Context, lamaMillis: Long) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }
    if (vibrator == null || !vibrator.hasVibrator()) return
    vibrator.vibrate(
        VibrationEffect.createOneShot(lamaMillis, VibrationEffect.DEFAULT_AMPLITUDE),
    )
}

/**
 * Dua panjang getar yang BERBEDA, dan ini bukan detail kosmetik.
 *
 * Getar panjang = "setnya sudah selesai, berhenti". Getar pendek = "istirahat
 * habis, siap-siap". Kamu akan sering mendengarnya dengan HP di lantai dan
 * mata terpejam, jadi panjangnya harus bisa dibedakan tanpa melihat layar.
 * Antarmuka yang bisa dipakai tanpa dilihat itu antarmuka yang paling dipakai.
 */
private const val GETAR_KERJA = 320L
private const val GETAR_ISTIRAHAT = 120L
