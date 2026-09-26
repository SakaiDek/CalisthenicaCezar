package com.cezar.calisthenica.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R
import com.cezar.calisthenica.model.Equipment
import com.cezar.calisthenica.model.Ketinggian

/**
 * Layar penuh untuk memilih ketinggian satu alat.
 *
 * ==================================================================
 * PENGAKUAN DULU, SEBELUM KODENYA: kamu menyetujui BOTTOM SHEET, dan
 * saya mengubahnya jadi layar penuh. Alasannya tiga, dan bukan selera:
 *
 *   1. `ModalBottomSheet` membuat JENDELA SENDIRI, seperti dialog. Dan kita
 *      sudah tahu dari build kemarin bahwa jendela dialog punya permintaan
 *      visibilitas bilah sistemnya sendiri -- bilah navigasi muncul lagi
 *      setiap kali dialog terbuka. Mode immersive yang baru kita pasang akan
 *      bocor tepat di layar baru ini.
 *   2. `ModalBottomSheet` masih `@ExperimentalMaterial3Api`. Di proyek ini API
 *      eksperimental itu ERROR compile, bukan peringatan, jadi dia menyeret
 *      `@OptIn` -- ongkos permanen untuk satu layar.
 *   3. Yang paling menentukan: slider ini DISERET VERTIKAL. Bottom sheet juga
 *      diseret vertikal, untuk menutup dirinya. Dua gerakan yang sama arah di
 *      dua lapisan yang bertumpuk = salah satu pasti kalah, dan yang kalah
 *      biasanya yang kamu maksud. Layar penuh menghapus perebutan itu.
 *
 * Kalau kamu tetap mau bottom sheet setelah tahu ketiganya, bilang -- itu
 * hakmu. Tapi jangan sampai saya diam-diam mengganti yang kamu setujui.
 * ==================================================================
 *
 * Tidak ada `BackHandler` di sini. Yang memegang Back tetap `ExerciseFormScreen`,
 * karena dia yang tahu urutan pulangnya: tutup layar ini dulu, baru mundur
 * langkah, baru keluar form. Satu tombol, satu pemilik.
 */
@Composable
internal fun LayarKetinggian(
    alat: Equipment,
    terpilih: Int,
    onPilih: (Int) -> Unit,
    onSelesai: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // safeDrawing, bukan navigationBars: layar ini menutupi
                // Scaffold, jadi tidak ada siapa pun di atasnya yang sudah
                // membayarkan jarak ke bilah status dan bilah navigasi.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.tinggi_kicker),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = alat.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Ketinggian.dari(terpilih).label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.tinggi_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            TanggaKetinggian(
                terpilih = terpilih,
                onPilih = onPilih,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSelesai,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(text = stringResource(R.string.tinggi_done))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Slidernya: sebuah TANGGA berdiri, atas = tinggi, bawah = rendah, isian oranye
 * tumbuh dari bawah ke atas. Bukan deretan titik horizontal.
 *
 * Kenapa berbentuk tangga dan bukan garis biasa: yang kamu atur ini benda
 * fisik yang memang punya anak tangga -- lubang palang, simpul tali. Bentuk yang
 * meniru bendanya bikin kamu tidak perlu menerjemahkan apa pun di kepala.
 *
 * PELAJARAN PENTING SOAL ISIAN: tidak ada satu baris pun yang menghitung
 * "setinggi apa oranyenya". Setiap anak tangga cuma menjawab satu pertanyaan
 * lokal -- "tingkatku di bawah atau sama dengan yang dipilih?" Kalau ya, dia
 * menyala. Isian yang tumbuh dari bawah itu MUNCUL SENDIRI dari sembilan
 * jawaban lokal. Pola ini namanya menghindari state turunan: makin sedikit
 * angka yang kamu hitung dan simpan, makin sedikit yang bisa keluar sinkron.
 */
@Composable
private fun TanggaKetinggian(
    terpilih: Int,
    onPilih: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dari tinggi ke rendah. `remember` supaya pengurutannya tidak diulang
    // setiap kali jempolmu bergerak satu piksel.
    val daftar = remember { Ketinggian.urutTampil() }
    val haptic = LocalHapticFeedback.current

    // ==============================================================
    // DUA BARIS DI BAWAH INI YANG PALING GAMPANG SALAH DI SELURUH FILE,
    // dan salahnya tidak kelihatan sebagai error.
    //
    // `pointerInput(kunci) { ... }` MENGINGAT lambda-nya. Selama kuncinya tidak
    // berubah, lambda LAMA yang terus jalan -- lengkap dengan nilai yang dia
    // tangkap saat pertama dibuat. Jadi kalau di dalam sana saya membaca
    // `terpilih` langsung, angkanya BEKU di nilai pertama: seret ke atas
    // sekali jalan, lalu perbandingan "sudah berubah belum" memakai angka
    // basi selamanya.
    //
    // `rememberUpdatedState` membungkus nilainya jadi kotak yang isinya boleh
    // diganti. Lambda menangkap KOTAKNYA, bukan isinya, jadi setiap kali dia
    // membuka kotak itu yang keluar nilai terbaru. Ingat trik ini; dia wajib
    // dipakai di setiap lambda yang hidup lebih lama dari satu recomposition:
    // pointerInput, LaunchedEffect, DisposableEffect.
    // ==============================================================
    val terpilihKini by rememberUpdatedState(terpilih)
    val onPilihKini by rememberUpdatedState(onPilih)

    BoxWithConstraints(modifier = modifier) {
        // Tinggi baris DIBAGI dari ruang yang tersedia, bukan dipatok 44.dp.
        // Bedanya kelihatan di HP pendek: kalau dipatok, anak tangga paling
        // bawah terpotong keluar layar dan kamu tidak bisa memilih "Belum
        // diatur" sama sekali. Dibagi begini, tangganya selalu utuh -- di HP
        // tinggi barisnya jadi longgar, di HP pendek jadi rapat.
        val tinggiBaris: Dp = maxHeight / daftar.size
        val tinggiBarisPx = with(LocalDensity.current) { tinggiBaris.toPx() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tinggiBarisPx) {
                    // `jariY` sengaja BUKAN state Compose, dan ini keputusan
                    // yang patut kamu tiru. Angka ini cuma dibaca di dalam
                    // gerakan jempol; tidak ada satu pun bagian layar yang
                    // membacanya untuk menggambar. Kalau saya menjadikannya
                    // `mutableStateOf`, setiap piksel pergerakan jempolmu akan
                    // memicu satu recomposition -- puluhan kali per detik,
                    // untuk angka yang tidak pernah ditampilkan.
                    //
                    // `var` biasa di dalam blok ini sudah cukup, karena blok
                    // pointerInput hidup selama kuncinya tidak berubah, dan
                    // kedua lambda di bawah menangkap variabel yang SAMA.
                    // Aturannya: state itu untuk yang DILIHAT layar. Sisanya
                    // variabel biasa.
                    var jariY = 0f
                    detectVerticalDragGestures(
                        // Posisi jempol dihitung dari ATAS kotak ini, dan
                        // dihitung ULANG dari nilai mutlak setiap kali -- bukan
                        // ditumpuk-tumpuk. Kalau saya menambah-nambah indeks
                        // per pergerakan, kesalahan pembulatan sekecil apa pun
                        // menumpuk dan slidernya perlahan melenceng dari
                        // jempolmu.
                        onDragStart = { titik -> jariY = titik.y },
                        onVerticalDrag = { perubahan, geseran ->
                            perubahan.consume()
                            jariY += geseran
                            val indeks = (jariY / tinggiBarisPx).toInt()
                                .coerceIn(0, daftar.size - 1)
                            val baru = daftar[indeks].tingkat
                            if (baru != terpilihKini) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPilihKini(baru)
                            }
                        },
                    )
                },
        ) {
            daftar.forEach { tingkatan ->
                AnakTangga(
                    tingkatan = tingkatan,
                    terpilih = terpilih,
                    tinggi = tinggiBaris,
                    onKlik = {
                        if (tingkatan.tingkat != terpilih) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPilih(tingkatan.tingkat)
                        }
                    },
                )
            }
        }
    }
}

/**
 * Satu anak tangga. Tingginya DIKIRIM dari induknya, bukan ditentukan di sini --
 * itu yang membuat titik dan tulisannya tidak mungkin melenceng satu sama lain,
 * dan yang membuat hitungan posisi jempol di induk selalu benar.
 */
@Composable
private fun AnakTangga(
    tingkatan: Ketinggian,
    terpilih: Int,
    tinggi: Dp,
    onKlik: () -> Unit,
) {
    val ini = tingkatan.tingkat
    val aktif = ini == terpilih

    // "Sudah kamu lewati dari bawah." Tingkat 0 tidak pernah menyala, karena
    // "Belum diatur" bukan sebuah ketinggian -- dia jawaban kosong.
    val menyala = ini in 1..terpilih

    val warnaRel = if (menyala) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val warnaPalang = if (menyala) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val tebalPalang = when {
        aktif -> 10.dp
        menyala -> 5.dp
        else -> 3.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(tinggi)
            .clickable(onClick = onKlik),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dua rel tegak + satu palang di antaranya. Rel digambar SETINGGI BARIS,
        // jadi rel baris-baris yang bersebelahan menyambung sendiri jadi satu
        // garis panjang. Tidak ada garis panjang yang perlu saya gambar, dan
        // tidak ada apa pun yang perlu diukur ulang saat pilihanmu berubah --
        // cuma warna yang berganti, dan ganti warna itu operasi termurah yang
        // ada di Compose (tidak menyentuh layout sama sekali).
        Row(
            modifier = Modifier
                .width(96.dp)
                .fillMaxHeight(),
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(warnaRel),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (ini > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(tebalPalang)
                            .clip(RoundedCornerShape(50))
                            .background(warnaPalang),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(warnaRel),
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // Dua cabang, dan paddingnya SENGAJA sama persis di dua-duanya. Kalau
        // yang tidak aktif tidak diberi padding, tulisannya akan bergeser ke
        // kiri-kanan setiap kali kamu memindahkan pilihan -- gerakan yang tidak
        // kamu minta, dan mata langsung menangkapnya sebagai "goyang".
        if (aktif) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = tingkatan.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        } else {
            Text(
                text = tingkatan.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}
