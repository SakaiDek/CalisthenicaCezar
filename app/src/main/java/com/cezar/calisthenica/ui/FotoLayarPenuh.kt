package com.cezar.calisthenica.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.fileGambar

/**
 * SATU TEMPAT untuk aturan baru: "foto boleh diperbesar MEMENUHI layar, tapi
 * hanya kalau kamu yang menekan tombolnya."
 *
 * ------------------------------------------------------------------
 * KENAPA FILE INI TERPISAH dari `FotoGerakan.kt`, padahal dua-duanya soal foto?
 * Karena `FotoGerakan.kt` menjaga SATU hukum yang tidak boleh goyah: tampilan
 * default tidak pernah memotong (`ContentScale.Fit`). File ini memegang
 * PENGECUALIAN atas hukum itu. Menaruh pengecualian di dalam file hukumnya cuma
 * bikin besok orang ragu mana yang berlaku. Jadi hukum tetap di sana, utuh;
 * pengecualiannya tinggal di sini, dan cuma jalan saat kamu menekan tombol.
 *
 * KENAPA `ContentScale.Crop` DI SINI BUKAN PENGKHIANATAN. Keluhanmu ("banyak
 * bilah hitam, kaku") dan ketakutanmu ("jangan sampai kepala/kaki kepotong") itu
 * dua hal yang saling menarik ke arah berlawanan. Tidak ada satu tampilan yang
 * memenangkan keduanya sekaligus. Jadi kita tidak memilih salah satu -- kita
 * kasih DUA:
 *   - Default: `Fit`, gambar utuh, dengan bilah gelap tipis. Nol piksel hilang.
 *   - Tombol expand: `Crop`, memenuhi layar, tanpa bilah. Sebagian tepi tersembunyi.
 * Karena default-nya selalu menyimpan gambar utuh, `Crop` di layar penuh TIDAK
 * menghilangkan apa pun secara permanen: yang tersembunyi di sini masih bisa
 * kamu lihat lengkap begitu layar penuh ini ditutup. Itu bedanya "auto-crop
 * permanen" (yang kamu tolak) dengan "zoom sesaat atas perintahmu" (yang kamu minta).
 * ------------------------------------------------------------------
 *
 * IKON FULLSCREEN DIGAMBAR SENDIRI, dan ini pelajaran hari ini soal dependency.
 * Proyek kita memakai `material-icons-core` -- paket ikon RINGKAS, isinya cuma
 * belasan ikon paling umum (Add, Close, Search, ...). `Icons.Default.Fullscreen`
 * TIDAK ADA di situ; dia tinggal di `material-icons-extended`, paket berisi
 * ribuan ikon yang bikin APK membengkak dan waktu build di laptop 8GB-mu makin
 * lama. Menarik ribuan ikon demi SATU glyph itu ongkos yang tidak sepadan.
 *
 * Jadi kita gambar sendiri satu ikon itu pakai `materialIcon` -- helper yang
 * SUDAH ada di `material-icons-core`. Empat kurung siku di empat sudut: bahasa
 * universal untuk "perbesar". `materialPath` otomatis mengisi hitam, lalu `Icon`
 * mewarnainya ulang sesuai `tint`, jadi warnanya ikut tema. Nol dependency baru.
 *
 * `internal`, BUKAN `private`: sejak layar Session Runner memakai foto sebagai
 * latar penuh, dia meminjam glyph yang sama untuk tombol "sembunyikan info".
 * Satu ikon, satu definisi, dipakai dua tempat -- daripada menggambar ulang
 * empat kurung siku yang sama di file lain.
 */
internal val IconMenuhiLayar = materialIcon(name = "Filled.Fullscreen") {
    materialPath {
        // Sudut kiri-bawah.
        moveTo(7f, 14f)
        lineTo(5f, 14f)
        lineTo(5f, 19f)
        lineTo(10f, 19f)
        lineTo(10f, 17f)
        lineTo(7f, 17f)
        lineTo(7f, 14f)
        close()
        // Sudut kiri-atas.
        moveTo(5f, 10f)
        lineTo(7f, 10f)
        lineTo(7f, 7f)
        lineTo(10f, 7f)
        lineTo(10f, 5f)
        lineTo(5f, 5f)
        lineTo(5f, 10f)
        close()
        // Sudut kanan-bawah.
        moveTo(17f, 17f)
        lineTo(14f, 17f)
        lineTo(14f, 19f)
        lineTo(19f, 19f)
        lineTo(19f, 14f)
        lineTo(17f, 14f)
        lineTo(17f, 17f)
        close()
        // Sudut kanan-atas.
        moveTo(14f, 5f)
        lineTo(14f, 7f)
        lineTo(17f, 7f)
        lineTo(17f, 10f)
        lineTo(19f, 10f)
        lineTo(19f, 5f)
        lineTo(14f, 5f)
        close()
    }
}

/**
 * Bungkus foto apa pun (mau `FotoGerakan` yang menyesuaikan rasio, mau `FotoAlat`
 * yang kotak tetap) supaya dapat tombol "perbesar" di pojok + layar penuhnya.
 *
 * KENAPA PAKAI SLOT (`isi: @Composable () -> Unit`) DAN BUKAN MENERIMA namaFile
 * LALU MENGGAMBAR SENDIRI: dua pemanggilnya menggambar foto dengan cara yang
 * BEDA -- layar detail pakai kotak yang memanjang mengikuti rasio, layar Siap
 * pakai kotak 140dp tetap. Kalau file ini yang memutuskan cara menggambar, dia
 * harus tahu dua cara itu dan tumbuh tiap ada cara ketiga. Dengan slot, dia cuma
 * mengurus SATU hal yang benar-benar sama di kedua tempat: tombol + layar penuh.
 * Bentuk fotonya tetap urusan pemanggil. Ini pola yang sama dengan alasan
 * `FotoGerakan` berbagi ATURAN, bukan tampilan.
 *
 * `namaFile` tetap diminta terpisah karena layar penuh perlu tahu file mana yang
 * dibuka -- dan karena tombolnya HANYA muncul kalau ada fotonya. Kalau `namaFile`
 * kosong, yang tergambar cuma huruf pertama nama; tidak ada apa pun untuk
 * diperbesar, jadi tombolnya sengaja tidak ada. Tombol yang tidak melakukan apa-apa
 * lebih membingungkan daripada tombol yang absen.
 */
@Composable
fun FotoBisaDiperbesar(
    namaFile: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isi: @Composable () -> Unit,
) {
    // Kunci `namaFile`: ganti foto -> layar penuh yang mungkin sedang terbuka
    // untuk foto lama otomatis dilupakan, tidak nyangkut membuka gambar usang.
    var penuh by remember(namaFile) { mutableStateOf(false) }

    Box(modifier = modifier) {
        isi()

        if (namaFile.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    // Lingkaran gelap semi-transparan: ikon putih di atasnya tetap
                    // terbaca entah fotonya terang atau gelap di bagian pojok itu.
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                    .clickable(onClick = { penuh = true }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconMenuhiLayar,
                    contentDescription = stringResource(R.string.foto_perbesar),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (penuh) {
        FullscreenFotoDialog(
            namaFile = namaFile,
            contentDescription = contentDescription,
            onTutup = { penuh = false },
        )
    }
}

/**
 * Foto satu layar penuh di atas latar hitam. Ditutup lewat tombol X atau tombol
 * Back HP (Dialog menangani Back sendiri lewat `onDismissRequest`).
 *
 * `usePlatformDefaultWidth = false` itu kuncinya: tanpa baris ini, Dialog dibatasi
 * lebar "kotak dialog" bawaan sistem (kira-kira selebar AlertDialog), dan gambarmu
 * malah tampil kecil di tengah -- persis kebalikan dari yang kamu minta. Dengan
 * `false`, dialog boleh selebar dan setinggi layar.
 *
 * `remember(namaFile)` membungkus `fileGambar`: sama seperti di `FotoAlat`, ini
 * mencegah objek `File` baru dibuat tiap recompose. Di sini recompose jarang,
 * tapi kebiasaannya kita jaga konsisten supaya tidak jadi contoh buruk.
 *
 * CATATAN JUJUR: bilah navigasi HP mungkin masih ikut muncul di atas layar penuh
 * ini -- itu bug lama "nav bar bocor saat Dialog Compose terbuka" yang belum kita
 * tutup, bukan sesuatu yang baru. Untuk sekarang tidak mengganggu; kalau nanti
 * mau benar-benar imersif tanpa bilah, itu perlu menyetel window dialognya sendiri.
 */
@Composable
private fun FullscreenFotoDialog(
    namaFile: String,
    contentDescription: String,
    onTutup: () -> Unit,
) {
    val context = LocalContext.current
    val berkas = remember(namaFile) { fileGambar(context, namaFile) }

    Dialog(
        onDismissRequest = onTutup,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AsyncImage(
                model = berkas,
                contentDescription = contentDescription,
                // Crop HANYA di sini. Bukan pelanggaran hukum "tidak dipotong":
                // ini pengecualian sesaat yang KAMU picu dengan menekan tombol,
                // dan tampilan default tetap menyimpan gambar utuhnya. Baca alasan
                // panjangnya di KDoc paling atas file ini.
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // `windowInsetsPadding(safeDrawing)` menurunkan tombol X ke bawah
            // poni/status bar, jadi dia tidak pernah tertutup takik kamera.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                    .clickable(onClick = onTutup),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.foto_tutup),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}



