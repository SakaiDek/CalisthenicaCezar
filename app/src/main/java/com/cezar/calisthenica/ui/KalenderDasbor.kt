package com.cezar.calisthenica.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.AppDatabase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * KALENDER + STREAK — bagian yang DITANAM di Dasbor, bukan layar terpisah.
 *
 * FILOSOFI RONDE 2: nol tabel baru, nol migration. Semua yang kamu lihat di sini
 * cuma HASIL BACA ulang kolom `tanggal` (teks ISO "yyyy-MM-dd") yang sudah ditulis
 * Runner tiap sesi selesai. Ini buah keputusan lama kita pasang minSdk 26: `java.time`
 * jalan native, jadi hitung tanggal/streak nggak butuh library desugaring apa pun.
 */

/** Judul kalender: "September 2026". YearMonth mendukung .format() langsung. */
private val FORMAT_BULAN: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))

/** Judul bagian sesi terpilih: "Sabtu, 26 September 2026". */
private val FORMAT_TANGGAL_PANJANG: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id"))

/**
 * Label kolom hari, dibangun SEKALI di tingkat file. `DayOfWeek.values()` selalu
 * urut MONDAY..SUNDAY, jadi hasilnya [Sen, Sel, Rab, Kam, Jum, Sab, Min] — persis
 * urutan grid kita yang mulai Senin. Menyusun label dari enum (bukan mengetik manual
 * "Sen","Sel",...) bikin dia ikut bahasa "id" otomatis dan mustahil salah urut.
 */
private val LABEL_HARI: List<String> =
    DayOfWeek.values().map { it.getDisplayName(TextStyle.SHORT, Locale("id")) }

/**
 * Saver YearMonth <-> String. rememberSaveable cuma bisa menyimpan tipe primitif ke
 * Bundle; YearMonth bukan primitif, jadi kita ajari cara ubah bolak-balik: simpan
 * jadi "2026-09", pulihkan lewat parse. Tanpa ini, bulan yang sedang dilihat akan
 * lompat balik ke bulan ini tiap layar diputar.
 */
private val YearMonthSaver: Saver<YearMonth, String> = Saver(
    save = { it.toString() },
    restore = { YearMonth.parse(it) },
)

/**
 * Saver untuk tanggal terpilih yang BOLEH null (belum ada yang diketuk). null kita
 * wakili string kosong "", karena Saver tidak boleh mengembalikan null saat restore
 * gagal — jadi "" = "tidak ada yang dipilih", selain itu parse jadi LocalDate.
 */
private val TanggalPilihanSaver: Saver<LocalDate?, String> = Saver(
    save = { it?.toString() ?: "" },
    restore = { if (it.isEmpty()) null else LocalDate.parse(it) },
)

/**
 * Hitung streak: berapa HARI BERUNTUN sampai hari ini kamu latihan.
 *
 * ATURAN "GRACE" (yang kita sepakati): kalau HARI INI belum sempat latihan, streak
 * TIDAK langsung dianggap putus — kita mulai hitung dari KEMARIN. Streak baru putus
 * setelah satu hari penuh benar-benar bolong. Ini bikin angka streak nggak bikin
 * panik tiap pagi sebelum kamu olahraga.
 *
 * Fungsi MURNI (input Set + tanggal -> Int, tanpa sentuh database/UI): gampang diuji,
 * gampang dipindah, dan hasilnya selalu sama untuk input yang sama.
 */
internal fun hitungStreak(hariLatihan: Set<LocalDate>, hariIni: LocalDate): Int {
    if (hariLatihan.isEmpty()) return 0
    // Kursor mulai dari hari ini kalau sudah latihan; kalau belum, mundur ke kemarin.
    var kursor = if (hariIni in hariLatihan) hariIni else hariIni.minusDays(1)
    var jumlah = 0
    while (kursor in hariLatihan) {
        jumlah++
        kursor = kursor.minusDays(1)
    }
    return jumlah
}

/**
 * BAGIAN utama yang ditanam di Dasbor: Kartu Streak + judul + navigator bulan +
 * grid tanggal + (kalau ada hari diketuk) daftar sesi hari itu memakai kartu
 * Riwayat ber-thumbnail yang sama persis.
 *
 * KENAPA `internal fun` dan bukan layar penuh: dia dipanggil sebagai satu `item {}`
 * di dalam LazyColumn Dasbor (MainActivity), jadi ikut ter-scroll bareng kartu
 * program. Dia mengurus SATU hal (kalender+streak), bukan Scaffold/TopAppBar —
 * itu urusan Dasbor yang meminjamnya.
 *
 * State lokal, TANPA ViewModel (pola rumah): sumber kebenaran ada di database, jadi
 * daftar tanggal terisi ulang sendiri dari Flow. Yang perlu diselamatkan saat rotasi
 * cuma "bulan mana yang dilihat" dan "tanggal mana yang diketuk" -> rememberSaveable.
 */
@Composable
internal fun KalenderDasborSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember(context) { AppDatabase.get(context).sessionLogDao() }

    // Tiga keadaan: null = DB belum menjawab; kosong = memang belum ada sesi;
    // isi = ada tanggal. initial=null mencegah "kedip kosong" sekejap saat buka app.
    val tanggalFlow = remember(dao) { dao.observeSemuaTanggal() }
    val muatanTanggal by tanggalFlow.collectAsState(initial = null)

    // Ubah daftar teks ISO jadi Set<LocalDate>: cek "hari X ada sesi?" jadi O(1),
    // dan streak-nya bisa dihitung dari sini. runCatching menjaga baris tanggal aneh
    // (kalau suatu hari ada) tidak bikin app tumbang -- baris rusak cukup diabaikan.
    val hariLatihan: Set<LocalDate> = remember(muatanTanggal) {
        (muatanTanggal ?: emptyList()).mapNotNull { iso ->
            runCatching { LocalDate.parse(iso) }.getOrNull()
        }.toSet()
    }
    val hariIni = remember { LocalDate.now() }
    val streak = remember(hariLatihan, hariIni) { hitungStreak(hariLatihan, hariIni) }

    var bulan by rememberSaveable(stateSaver = YearMonthSaver) { mutableStateOf(YearMonth.now()) }
    var dipilih by rememberSaveable(stateSaver = TanggalPilihanSaver) { mutableStateOf<LocalDate?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        KartuStreak(streak = streak)
        Spacer(Modifier.height(16.dp))

        NavigatorBulan(
            bulan = bulan,
            onPrev = { bulan = bulan.minusMonths(1) },
            onNext = { bulan = bulan.plusMonths(1) },
        )
        Spacer(Modifier.height(8.dp))

        GridKalender(
            bulan = bulan,
            hariLatihan = hariLatihan,
            hariIni = hariIni,
            dipilih = dipilih,
            // Ketuk hari yang sama dua kali -> tutup lagi (toggle). Enak buat nutup
            // panel detail tanpa harus punya tombol X terpisah.
            onPilih = { tgl -> dipilih = if (dipilih == tgl) null else tgl },
        )

        BagianSesiTerpilih(
            dao = dao,
            dipilih = dipilih,
            adaSesiSamaSekali = hariLatihan.isNotEmpty(),
            sudahDijawab = muatanTanggal != null,
        )
    }
}

/**
 * Panel di bawah grid. Tiga kemungkinan tampil, saling eksklusif:
 *   - ADA hari diketuk  -> judul "Sesi pada <tanggal>" + kartu Riwayat hari itu.
 *   - Belum ada sesi sama sekali (DB sudah jawab, tetap kosong) -> ajakan mulai.
 *   - Ada sesi tapi belum ada yang diketuk -> hint "ketuk tanggal bertitik".
 * Kalau DB belum menjawab, sengaja tidak menggambar apa pun (hindari kedip).
 *
 * `observeByTanggal(iso)` dibungkus `remember(iso)`: query baru dibuat HANYA saat
 * tanggal terpilih berganti, bukan tiap recompose. Pakai bind-argument `:tanggal`
 * di DAO -> aman dari SQL injection walau nilainya dari ketukan user.
 */
@Composable
private fun BagianSesiTerpilih(
    dao: com.cezar.calisthenica.data.SessionLogDao,
    dipilih: LocalDate?,
    adaSesiSamaSekali: Boolean,
    sudahDijawab: Boolean,
) {
    val terpilih = dipilih
    when {
        terpilih != null -> {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(
                    R.string.kalender_sesi_pada,
                    terpilih.format(FORMAT_TANGGAL_PANJANG),
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))

            val iso = terpilih.toString()
            val sesiFlow = remember(iso) { dao.observeByTanggal(iso) }
            val sesiHari by sesiFlow.collectAsState(initial = null)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Kartu Riwayat dipinjam apa adanya (RiwayatRow `internal`): dia
                // mandiri, query detailnya sendiri saat di-expand, thumbnail lewat
                // FotoAlat. Nol kode disalin ulang.
                (sesiHari ?: emptyList()).forEach { log -> RiwayatRow(log) }
            }
        }

        sudahDijawab && !adaSesiSamaSekali -> {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.kalender_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        adaSesiSamaSekali -> {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.kalender_pilih_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Kartu Streak — "pahlawan" psikologis Dasbor: angka besar yang bikin kamu nggak
 * mau memutus rantainya. Kalau streak 0, jangan pajang angka 0 yang bikin lemas;
 * ganti jadi ajakan hangat "mulai hari ini".
 */
@Composable
private fun KartuStreak(streak: Int) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.kalender_streak_judul),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            if (streak > 0) {
                Text(
                    text = stringResource(R.string.kalender_streak_hari, streak),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = stringResource(R.string.kalender_streak_kosong),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * Baris navigasi bulan: panah kiri | "September 2026" | panah kanan.
 * Panah pakai `KeyboardArrowLeft/Right` yang MEMANG ADA di material-icons-core --
 * nggak perlu menarik icons-extended yang bikin APK & build di laptop 8GB membengkak.
 * Judulnya `weight(1f)` + rata tengah supaya kedua panah nemplok rapi di tepi.
 */
@Composable
private fun NavigatorBulan(bulan: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.kalender_bulan_sebelumnya),
            )
        }
        Text(
            text = bulan.format(FORMAT_BULAN),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = stringResource(R.string.kalender_bulan_berikutnya),
            )
        }
    }
}

/**
 * Grid tanggal, mulai hari SENIN.
 *
 * PELAJARAN HARI INI — matematika grid: tanggal 1 sebuah bulan jarang jatuh di
 * kolom pertama. `dayOfWeek.value` = 1 (Senin)..7 (Minggu); kita kurangi 1 supaya
 * Senin -> 0 sel kosong di depan, Minggu -> 6 sel kosong. Itu `offset`. Lalu kita
 * susun: [offset sel null] + [tanggal 1..akhir bulan], dan ditambal sel null di
 * BELAKANG sampai kelipatan 7 supaya `.chunked(7)` menghasilkan baris-baris yang
 * rapi tujuh kolom. Sel null = kotak penyeimbang kosong, bukan tanggal.
 */
@Composable
private fun GridKalender(
    bulan: YearMonth,
    hariLatihan: Set<LocalDate>,
    hariIni: LocalDate,
    dipilih: LocalDate?,
    onPilih: (LocalDate) -> Unit,
) {
    // Baris label kolom hari (Sen..Min), tiap kolom weight sama dengan sel di bawah.
    Row(modifier = Modifier.fillMaxWidth()) {
        LABEL_HARI.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(4.dp))

    val offset = bulan.atDay(1).dayOfWeek.value - 1
    val sel: List<LocalDate?> = buildList {
        repeat(offset) { add(null) }
        for (h in 1..bulan.lengthOfMonth()) add(bulan.atDay(h))
    }
    val sisa = (7 - sel.size % 7) % 7
    val penuh = sel + List(sisa) { null }

    penuh.chunked(7).forEach { minggu ->
        Row(modifier = Modifier.fillMaxWidth()) {
            minggu.forEach { tgl ->
                SelHari(
                    tanggal = tgl,
                    adaSesi = tgl != null && tgl in hariLatihan,
                    iniHariIni = tgl != null && tgl == hariIni,
                    terpilih = tgl != null && tgl == dipilih,
                    onClick = { if (tgl != null) onPilih(tgl) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Satu sel tanggal. Empat status visual, bertumpuk dari lemah ke kuat:
 *   - biasa (tanpa sesi): angka polos, TIDAK bisa diketuk (ngapain buka hari kosong).
 *   - ada sesi: lingkaran `primaryContainer`, BISA diketuk.
 *   - hari ini: cincin garis tipis (border) supaya "sekarang" gampang dicari mata.
 *   - terpilih: lingkaran `primary` penuh + teks `onPrimary`, status paling kuat.
 *
 * `Modifier` dibangun bertahap pakai `var kotak = ...` lalu ditambah kondisional --
 * Modifier itu nilai biasa (bukan composable), jadi merangkainya begini sah dan
 * lebih terbaca ketimbang satu rantai raksasa penuh `if`.
 *
 * Sel null (penyeimbang) langsung berhenti lebih awal: kotak kosong tanpa angka,
 * tanpa klik. `aspectRatio(1f)` menjaga tiap sel tetap bujur sangkar seberapa pun
 * lebar layarnya.
 */
@Composable
private fun SelHari(
    tanggal: LocalDate?,
    adaSesi: Boolean,
    iniHariIni: Boolean,
    terpilih: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (tanggal == null) return@Box

        val warnaLatar = when {
            terpilih -> MaterialTheme.colorScheme.primary
            adaSesi -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        }
        val warnaTeks = when {
            terpilih -> MaterialTheme.colorScheme.onPrimary
            adaSesi -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }

        var kotak = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(warnaLatar)
        if (iniHariIni && !terpilih) {
            kotak = kotak.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        }
        if (adaSesi) {
            kotak = kotak.clickable(onClick = onClick)
        }

        Box(modifier = kotak, contentAlignment = Alignment.Center) {
            Text(
                text = tanggal.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = warnaTeks,
                fontWeight = if (adaSesi || iniHariIni) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
