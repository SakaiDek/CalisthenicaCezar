package com.cezar.calisthenica.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cezar.calisthenica.data.fileGambar
import com.cezar.calisthenica.data.rasioGambar

/**
 * SATU TEMPAT untuk menampilkan foto gerakan, dan satu aturan yang berlaku di
 * seluruh app: FOTO USER TIDAK PERNAH DIPOTONG.
 *
 * ------------------------------------------------------------------
 * KENAPA FILE INI ADA, padahal di `ExerciseDetailScreen.kt` saya sendiri menulis
 * "jangan berbagi potongan UI kecil sampai kamu YAKIN dua tempat harus selalu
 * terlihat sama"?
 *
 * Karena sekarang saya yakin, dan alasannya bukan kerapian: kalau aturan
 * "jangan dipotong" ditulis dua kali di dua file, besok salah satunya akan
 * dilupakan. Bug foto kepotong yang kamu laporkan itu muncul di TIGA tempat
 * sekaligus -- katalog, detail, form -- karena keputusan yang sama ditulis tiga
 * kali. Yang boleh dibagi bersama itu ATURAN, bukan tampilan.
 * ------------------------------------------------------------------
 *
 * MATEMATIKA DI BALIK KELUHANMU, karena ini pantas kamu tahu angkanya -- dan
 * karena permintaanmu tadi ("rasio 16:9 proporsional tanpa memotong bagian
 * penting") itu dua hal yang saling bertabrakan, jadi saya harus menawar satu:
 *
 * Foto sumbermu screenshot HP, rasionya 9:16 = 0,5625 (tinggi jauh lebih besar
 * dari lebar). Kalau dijejalkan ke kotak 16:9 = 1,778 dengan ContentScale.Crop,
 * yang selamat cuma 0,5625 / 1,778 = 32% dari tinggi fotonya. Enam puluh delapan
 * persen sisanya -- kepala dan kaki -- dibuang. Itu bukan bug pengaturan, itu
 * konsekuensi aritmetika: kotak 16:9 memang TIDAK BISA memuat gambar 9:16 tanpa
 * memotong. Tidak ada nilai ContentScale yang membatalkan itu.
 *
 * PENAWARANNYA: kotaknya yang menyesuaikan diri, bukan fotonya yang dikorbankan.
 *   1. Baca rasio asli fotonya (murah, lihat `rasioGambar`).
 *   2. Pakai rasio itu sebagai bentuk kotaknya, TAPI dijepit di rentang aman
 *      0,75 (3:4 tegak) sampai 1,778 (16:9 lebar).
 *   3. Isi dengan ContentScale.Fit -- yang artinya "muat SELURUHNYA".
 *
 * Kenapa dijepit di 0,75 dan tidak dibiarkan 0,5625 apa adanya? Karena foto
 * 9:16 apa adanya akan setinggi 1,78 kali lebar layar. Di HP-mu itu ~740dp:
 * satu foto memakan dua layar penuh dan judul gerakannya terdorong keluar. Jadi
 * kotaknya berhenti di 3:4, dan sisa selisihnya jadi bilah gelap tipis di kiri
 * kanan (~9% lebar tiap sisi) -- bukan potongan. Tidak ada satu piksel gambarmu
 * yang hilang; hanya ada ruang kosong yang jujur.
 *
 * `Modifier.blur` untuk mengisi bilah itu dengan versi kabur fotonya SENGAJA
 * belum saya pakai: `blur` tidak melakukan apa pun di bawah Android 12, dan
 * minSdk kita 26. Efek yang cuma jalan di HP baru itu bukan efek, itu utang.
 *
 * @param rasioBawaan dipakai selama rasio asli belum terbaca (sekitar satu-dua
 *   frame). Nilainya sengaja 3:4, bukan 16:9 -- isi galerimu foto tegak, jadi
 *   tebakan yang paling sering benar bikin kotaknya tidak perlu bergerak sama
 *   sekali. Menebak dengan benar itu cara termurah menghilangkan kedipan layout.
 */
@Composable
fun FotoGerakan(
    namaFile: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    rasioBawaan: Float = 3f / 4f,
    rasioMin: Float = 3f / 4f,
    rasioMax: Float = 16f / 9f,
    sudut: Dp = 0.dp,
) {
    val context = LocalContext.current

    /*
     * `remember(namaFile)` -- perhatikan kuncinya. Tanpa kunci itu, saat kamu
     * menekan "Ganti foto" di form, kotaknya akan tetap memakai rasio foto LAMA
     * sampai yang baru selesai diukur. Kunci `namaFile` memaksa ingatannya
     * dibuang begitu nama filenya berubah, jadi foto baru diukur dari nol.
     *
     * LaunchedEffect dengan kunci yang sama menjalankan pengukuran di luar
     * thread UI. Membaca file, walau cuma kepalanya, tetap menyentuh NVMe/flash
     * dan itu tidak boleh dilakukan di thread yang sedang menggambar layar.
     */
    var rasioAsli by remember(namaFile) { mutableStateOf<Float?>(null) }
    LaunchedEffect(namaFile) {
        rasioAsli = rasioGambar(context, namaFile)
    }

    val target = (rasioAsli ?: rasioBawaan).coerceIn(rasioMin, rasioMax)

    // Kalau tebakan bawaan ternyata salah (foto lanskap), kotaknya berubah
    // bentuk. `animateFloatAsState` bikin perubahan itu MELUNCUR, bukan
    // MELOMPAT. Ini bedanya "app terasa halus" dan "app terasa berkedut", dan
    // ongkosnya satu baris.
    val rasio by animateFloatAsState(targetValue = target, label = "rasioFoto")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(rasio)
            .clip(RoundedCornerShape(sudut))
            // Bilah gelap di sisi yang tidak terisi. Memakai slot warna paling
            // gelap di tangga wadah (lihat Color.kt), jadi di mode malam bilah
            // ini nyaris tidak terlihat dan foto terasa mengapung.
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = fileGambar(context, namaFile),
            contentDescription = contentDescription,
            // Fit, bukan Crop. Satu kata ini inti dari seluruh file ini.
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
