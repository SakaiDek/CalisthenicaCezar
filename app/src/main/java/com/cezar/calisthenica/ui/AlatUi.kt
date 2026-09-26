package com.cezar.calisthenica.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.fileGambar
import com.cezar.calisthenica.data.simpanGambarKeInternal
import com.cezar.calisthenica.model.Equipment
import com.cezar.calisthenica.model.Ketinggian
import kotlinx.coroutines.launch

/**
 * SEMUA cara menggambar "alat" di app ini, dikumpulkan di satu file.
 *
 * KENAPA SATU FILE, dan kenapa ini bukan pengulangan kesalahan yang saya sendiri
 * peringatkan di `ExerciseDetailScreen.kt` ("jangan berbagi potongan UI kecil
 * sampai kamu YAKIN dua tempat harus selalu terlihat sama")?
 *
 * Karena yang dibagi di sini bukan TATA LETAK, tapi JAWABAN atas satu pertanyaan
 * yang muncul di tiga layar sekaligus: "kalau alat ini belum difoto, apa yang
 * digambar?" Jawabannya harus sama di form, di layar detail, dan nanti di lembar
 * filter -- kalau tidak, alat yang sama terlihat seperti dua benda berbeda
 * tergantung kamu sedang di layar mana. Bentuk barisnya sendiri tetap boleh beda:
 * lihat `BarisAlat` (besar, untuk dipilih) dan `PilAlat` (kecil, untuk dibaca).
 *
 * ATURAN FOTO PROYEK INI TETAP BERLAKU: tidak ada `ContentScale.Crop` satu pun
 * di file ini. Bedanya dengan `FotoGerakan.kt`, di sini kotaknya sengaja UKURAN
 * TETAP dan tidak menyesuaikan diri dengan rasio fotonya. Alasannya sama seperti
 * pengecualian di `ExerciseCatalogScreen.Thumbnail`: ini deretan baris, dan
 * deretan baris yang tingginya berbeda-beda terlihat seperti layar yang rusak.
 * Jadi kotaknya diam, `ContentScale.Fit` yang mengalah, dan selisihnya jadi
 * bilah gelap tipis -- bukan potongan. Nol piksel fotomu hilang.
 */

/**
 * Foto satu alat di dalam kotak berukuran tetap. Kalau belum ada fotonya, yang
 * digambar HURUF PERTAMA nama alatnya di atas warna aksen.
 *
 * Kenapa huruf dan bukan ikon "gambar rusak": huruf itu MEMBEDAKAN. Sepuluh alat
 * tanpa foto dengan ikon yang sama terlihat seperti sepuluh baris kosong yang
 * identik, dan matamu tidak punya pegangan. Dengan huruf, "M" (Matras) dan "H"
 * (Handuk) sudah bisa kamu bedakan dari ujung mata sebelum membaca namanya.
 * Ini trik lama dari daftar kontak, dan dia bekerja karena alasan yang benar.
 */
@Composable
internal fun FotoAlat(
    namaFile: String,
    nama: String,
    modifier: Modifier = Modifier,
    sudut: Dp = 14.dp,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(sudut))
            .background(
                if (namaFile.isBlank()) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (namaFile.isBlank()) {
            Text(
                // `take(1)` bukan `first()`: nama kosong bikin `first()` melempar
                // exception, `take(1)` cuma menghasilkan teks kosong. Nama alat
                // memang tidak boleh kosong, tapi UI tidak seharusnya crash cuma
                // karena data yang seharusnya tidak mungkin ternyata mungkin.
                text = nama.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            /*
             * `remember(namaFile)`, bukan `fileGambar(context, namaFile)` telanjang
             * di dalam `model =`. Ini kebersihan, bukan perbaikan bug -- dan saya
             * mau kamu tahu bedanya supaya tidak salah menyimpulkan.
             *
             * Yang TIDAK terjadi sebelumnya: Coil TIDAK memuat ulang fotonya tiap
             * recompose. `File` membandingkan dirinya berdasarkan PATH, jadi
             * `File` baru dengan path sama dianggap `==` dengan yang lama, dan
             * Coil menyimpulkan "permintaannya sama, tidak perlu apa-apa".
             *
             * Yang terjadi: satu objek `File` baru DIBUAT setiap recompose lalu
             * langsung dibuang. Di satu baris tidak ada artinya. Di daftar yang
             * di-scroll dengan 4 baris terlihat, angkanya jadi puluhan objek per
             * detik yang cuma jadi sampah -- dan sampah itu yang nanti dipungut
             * oleh garbage collector, tepat saat kamu sedang scroll. Itu salah
             * satu penyebab klasik frame yang "nyangkut" sekejap.
             *
             * Kuncinya `namaFile`: hitung ulang HANYA kalau nama filenya berubah.
             * `context` tidak perlu ikut jadi kunci -- dia tidak pernah berganti
             * selama Activity yang sama masih hidup.
             */
            val berkas = remember(namaFile) { fileGambar(context, namaFile) }
            AsyncImage(
                model = berkas,
                contentDescription = stringResource(R.string.alat_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * SATU BARIS ALAT: foto lanskap di kiri, nama besar di kanan, SELURUH baris bisa
 * ditekan. Ini bentuk yang kamu tandai "mau seperti ini" di rekaman Die Ringe.
 *
 * PELAJARAN HARI INI -- kenapa baris seperti ini menang atas chip, padahal chip
 * jauh lebih murah dibuat dan sudah kita punya (`ChipGrid` di form):
 *
 *   1. Chip cuma bisa membawa teks. Alat itu BENDA, dan benda dikenali dari
 *      bentuknya lebih cepat daripada dari namanya. "Parallettes" perlu kamu
 *      baca; fotonya tidak.
 *   2. Sasaran tekannya lebar penuh. Chip setinggi 32dp di layar 6,67 inci itu
 *      target kecil, dan jempol manusia bukan kursor mouse. Panduan Material
 *      minta minimum 48dp; baris ini 90dp lebih.
 *   3. Chip berjejer menyamping -- 20 alat jadi tembok teks yang harus dibaca
 *      seluruhnya. Baris menurun ke bawah, dan mata manusia memang menelusuri
 *      daftar vertikal jauh lebih cepat.
 *
 * Harganya jujur: satu alat sekarang makan ~104dp tinggi, bukan ~40dp. Sepuluh
 * alat berarti menggulir. Itu pertukaran yang benar di sini, karena alat yang
 * kamu punya jumlahnya belasan, bukan tiga puluhan seperti otot -- dan otot
 * memang tetap pakai chip.
 *
 * `combinedClickable` menempel di Surface, BUKAN di dalamnya, supaya efek riak
 * (ripple) menyapu seluruh baris. Kalau clickable dipasang di kotak fotonya saja,
 * user akan menekan namanya dan merasa app-nya tidak merespons.
 *
 * Kenapa ada `@OptIn(ExperimentalFoundationApi::class)` di bawah: `combinedClickable`
 * masih ditandai eksperimental oleh tim Compose (foundation 1.7.0). Kotlin
 * TIDAK menganggap itu peringatan -- dia menolak meng-compile sampai kita
 * tanda tangan "saya sadar API ini bisa berubah". Pola sama dipakai di
 * `ExerciseRow` pada ExerciseCatalogScreen.kt.
 *
 * @param ketinggian null berarti "baris ini tidak sedang membicarakan
 *   ketinggian" -- itu keadaan normal untuk hampir semua baris. Isi hanya untuk
 *   alat yang sakelar `adjustableHeight`-nya hidup DAN sedang tercentang.
 *   Dua parameter terakhir punya nilai bawaan, jadi pemanggil lama tidak
 *   perlu diubah sama sekali -- ini gunanya nilai bawaan di Kotlin: menambah
 *   kemampuan tanpa membuat kode yang sudah jalan jadi merah.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BarisAlat(
    alat: Equipment,
    terpilih: Boolean,
    onKlik: () -> Unit,
    onTekanLama: () -> Unit,
    ketinggian: Ketinggian? = null,
    onAturKetinggian: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        // Warna yang MEMBEDAKAN, bukan cuma garis tepi. Di latar hampir hitam,
        // garis tepi tipis nyaris tidak terlihat -- ini pelajaran yang sama yang
        // bikin KartuSeksi memakai Surface berwarna, bukan Card berbayang.
        color = if (terpilih) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onKlik,
                onLongClick = {
                    // Getaran itu SATU-SATUNYA cara user tahu tekan-lama sudah
                    // terdaftar. Tanpa ini dia akan menahan lebih lama, lalu
                    // mengangkat jari sambil bingung kenapa tidak terjadi apa-apa.
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTekanLama()
                },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            FotoAlat(
                namaFile = alat.photoFile,
                nama = alat.name,
                // Kotak lanskap 4:3 = 120x90dp. Lanskap karena hampir semua foto
                // alat yang kamu ambil nanti bentuknya mendatar (palang, matras,
                // ring tergantung), dan karena inilah bentuk yang kamu tandai.
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(4f / 3f),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    // Dua baris, lalu elipsis. Nama sepanjang "Pull Up Bar pintu
                    // kamar sebelah" harus boleh ditulis; yang tidak boleh itu
                    // barisnya melar sampai fotonya kelihatan aneh.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (ketinggian != null) {
                    Spacer(modifier = Modifier.height(6.dp))

                    val diatur = ketinggian != Ketinggian.BELUM

                    // Pil ini SEKALIGUS jawaban dan tombolnya. Kalau saya
                    // menaruh tombol "Ubah" terpisah di sebelahnya, barisnya
                    // punya tiga sasaran tekan yang artinya beda-beda dalam
                    // ruang sempit -- resep salah tekan. Yang menampilkan nilai
                    // dan yang mengubah nilai boleh jadi benda yang sama.
                    //
                    // `clickable` di anak, di dalam `combinedClickable` induk:
                    // yang paling dalam menang untuk tekanan singkat. Ini pola
                    // yang sama dipakai LazyColumn (induk menyeret, anak
                    // diklik) -- bukan kebetulan, memang begitu Compose
                    // menyalurkan sentuhan.
                    Surface(
                        color = if (diatur) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onAturKetinggian),
                    ) {
                        Text(
                            text = if (diatur) {
                                stringResource(R.string.alat_height_set, ketinggian.label)
                            } else {
                                stringResource(R.string.alat_height_unset)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (diatur) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            if (terpilih) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

/**
 * Baris terakhir di daftar alat: "Tambah alat".
 *
 * Sengaja BERBENTUK BARIS, bukan tombol biasa di atas atau FAB di sudut. Alasannya
 * satu dan penting: dia harus terlihat seperti anggota daftar yang belum terisi.
 * Mata menelusuri daftar dari atas ke bawah, sampai di ujung, dan menemukan
 * "tambah" tepat di tempat dia sedang melihat -- bukan harus balik ke atas
 * mencari tombol. Warnanya `surfaceContainerLowest` (paling gelap di tangga
 * wadah, lihat Color.kt) supaya dia terbaca sebagai lubang kosong, bukan sebagai
 * alat sungguhan.
 */
@Composable
internal fun BarisTambahAlat(onKlik: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            // Baris ini cuma butuh SATU arti tekanan, jadi dia pakai `clickable`
            // yang sudah stabil -- bukan `combinedClickable` yang eksperimental.
            // Aturan umumnya: jangan bayar ongkos API eksperimental untuk
            // kemampuan (tekan-lama) yang tidak kamu pakai.
            .clickable(onClick = onKlik),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.alat_add),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Versi kecil untuk layar detail: foto 40dp + nama, dalam satu pil.
 *
 * Ini pengganti `Pill` teks-saja yang dulu dipakai di sana. Bedanya bukan
 * kosmetik: di layar detail kamu sedang membaca "gerakan ini butuh apa", dan
 * foto menjawab pertanyaan itu lebih cepat daripada kata. Ukurannya kecil karena
 * di sini alat cuma INFORMASI, bukan sesuatu yang harus dipilih -- besar-kecilnya
 * elemen di layar harus sebanding dengan seberapa penting dia untuk ditekan.
 */
@Composable
internal fun PilAlat(alat: Equipment) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        ) {
            FotoAlat(
                namaFile = alat.photoFile,
                nama = alat.name,
                sudut = 8.dp,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = alat.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Dialog tambah/ubah alat. Dua kolom isian saja: nama dan foto.
 *
 * KENAPA DIALOG DAN BUKAN LAYAR PENUH -- ini penawaran yang kamu setujui, dan
 * aturannya sudah jadi hukum kecil di proyek ini: dua sampai tiga isian = dialog,
 * lebih dari itu = layar sendiri. Layar penuh untuk dua isian bikin user merasa
 * dia "pergi ke tempat lain", padahal dia cuma mampir sebentar dan mau langsung
 * balik ke pekerjaan yang tadi.
 *
 * Yang lebih penting: dialog ini muncul DARI DALAM langkah alat di form gerakan.
 * Kamu menyadari matrasmu belum terdaftar tepat saat sedang mendaftarkan sebuah
 * gerakan -- bukan besok, bukan di menu terpisah. Alur itu yang jadi alasan
 * utamanya, bukan hemat kode.
 *
 * SOAL FILE FOTO, dan ini bagian yang paling gampang bocor jadi bug. Setiap foto
 * yang dipilih LANGSUNG tersalin ke folder app (lihat MediaFiles.kt), jadi dialog
 * ini bisa meninggalkan file yatim dalam tiga cara. Tiga-tiganya ditutup:
 *
 *   1. Pilih foto, lalu pilih foto lagi -> salinan pertama dibuang di sini.
 *   2. Pilih foto, lalu tekan Batal -> salinan itu dibuang di sini.
 *   3. Ganti foto, lalu tekan Simpan -> foto LAMA-nya yang harus dibuang, dan
 *      itu BUKAN tugas dialog ini. Dialog tidak tahu barisnya berhasil tersimpan
 *      atau tidak; yang tahu itu pemanggilnya. Jadi aturannya: dialog cuma
 *      membersihkan file yang DIA sendiri buat lalu DIA sendiri tinggalkan.
 *      Foto milik baris yang sudah ada di database adalah tanggung jawab yang
 *      memanggil `onSimpan`.
 *
 * @param awal null = mode tambah, terisi = mode ubah. Satu dialog, dua mode --
 *   karena isiannya identik, dan membuat dua dialog yang sama artinya besok
 *   memperbaiki bug yang sama dua kali.
 * @param onSimpan menerima nama, NAMA FILE fotonya (boleh kosong), dan posisi
 *   sakelar "bisa diatur ketinggiannya".
 * @param onMintaHapus dipanggil saat tombol "Hapus alat" ditekan. Perhatikan
 *   namanya: MINTA hapus, bukan hapus. Dialog ini tidak menghapus apa pun, dia
 *   cuma menutup diri dan menyerahkan permintaannya ke atas -- karena konfirmasi
 *   penghapusan butuh angka "dipakai di N gerakan" yang cuma database tahu.
 */
@Composable
internal fun DialogAlat(
    awal: Equipment?,
    onTutup: () -> Unit,
    onSimpan: (nama: String, namaFoto: String, bisaDiatur: Boolean) -> Unit,
    onMintaHapus: () -> Unit,
    onBuangFoto: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Foto yang sudah tercatat di database sebelum dialog dibuka. Pembanding
    // inilah yang membedakan "file yang saya buat sendiri barusan" (boleh
    // dibuang) dari "file milik baris yang sudah ada" (jangan disentuh).
    val fotoAwal = awal?.photoFile ?: ""

    var nama by remember { mutableStateOf(awal?.name ?: "") }
    var foto by remember { mutableStateOf(fotoAwal) }
    var menyalin by remember { mutableStateOf(false) }
    var gagal by remember { mutableStateOf(false) }

    // Sakelar "bisa diatur ketinggiannya". Nilainya diambil dari baris yang
    // sedang diubah, dan `false` untuk alat baru -- karena mayoritas alat memang
    // tidak bisa dinaik-turunkan, dan default yang benar adalah default yang
    // paling sering benar.
    var bisaDiatur by remember { mutableStateOf(awal?.adjustableHeight ?: false) }

    val pilihFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            menyalin = true
            gagal = false
            scope.launch {
                val fotoLama = foto
                val namaBaru = simpanGambarKeInternal(context, uri)
                menyalin = false
                if (namaBaru == null) {
                    gagal = true
                } else {
                    foto = namaBaru
                    // Urutannya sengaja: yang lama dibuang HANYA setelah yang
                    // baru terbukti berhasil. Kalau dibalik dan penyalinan
                    // gagal, kamu kehilangan dua-duanya.
                    if (fotoLama.isNotBlank() && fotoLama != fotoAwal) onBuangFoto(fotoLama)
                }
            }
        }
    }

    fun batal() {
        if (foto.isNotBlank() && foto != fotoAwal) onBuangFoto(foto)
        onTutup()
    }

    AlertDialog(
        onDismissRequest = { batal() },
        title = {
            Text(
                text = stringResource(
                    if (awal == null) R.string.alat_dialog_new else R.string.alat_dialog_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text(text = stringResource(R.string.alat_name)) },
                    placeholder = { Text(text = stringResource(R.string.alat_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Foto dan tombolnya berdampingan, bukan bertumpuk. Di dalam
                // dialog ruang vertikal itu barang mahal: dialog yang lebih
                // tinggi dari layar akan menggulir sendiri dan tombol Simpan-nya
                // hilang di bawah -- salah satu cara terhalus membuat user
                // merasa app-nya rusak.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FotoAlat(
                        namaFile = foto,
                        // Huruf pertama ikut hidup saat kamu mengetik namanya.
                        // Umpan balik kecil, tapi dia yang bikin kotak itu
                        // terasa "sudah jadi sesuatu" walau fotonya belum ada.
                        nama = nama,
                        sudut = 12.dp,
                        modifier = Modifier
                            .width(96.dp)
                            .aspectRatio(4f / 3f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TextButton(
                            // Mati selagi menyalin. Menekan dua kali saat file
                            // sedang disalin menghasilkan dua salinan, dan yang
                            // pertama langsung jadi file yatim.
                            enabled = !menyalin,
                            onClick = {
                                pilihFoto.launch(
                                    PickVisualMediaRequest(
                                        // ImageOnly: video tidak muncul sama
                                        // sekali di pemilih. Mencegah kesalahan
                                        // lebih baik daripada menegurnya.
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                        ) {
                            Text(
                                text = stringResource(
                                    if (foto.isBlank()) {
                                        R.string.form_thumbnail_pick
                                    } else {
                                        R.string.form_thumbnail_change
                                    },
                                ),
                            )
                        }
                        if (foto.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    val dibuang = foto
                                    foto = ""
                                    // Sekali lagi pembanding `fotoAwal`: kalau
                                    // yang dibuang ini foto milik baris di
                                    // database, JANGAN dihapus dari disk
                                    // sekarang -- user masih boleh menekan
                                    // Batal dan mengembalikan segalanya.
                                    if (dibuang != fotoAwal) onBuangFoto(dibuang)
                                },
                            ) {
                                Text(text = stringResource(R.string.form_thumbnail_remove))
                            }
                        }
                    }
                }

                if (menyalin) {
                    Text(
                        text = stringResource(R.string.form_thumbnail_working),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (gagal) {
                    Text(
                        text = stringResource(R.string.form_thumbnail_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                // ==================================================
                // SAKELAR KETINGGIAN. Ini jawaban atas pertanyaanmu
                // "apakah gymnastic rings dibuat bawaan?" -- jawabannya TIDAK,
                // dan sakelar inilah gantinya.
                //
                // Kalau saya menanam ring sebagai alat bawaan, kode harus
                // mengenalinya dengan `if (name == "Gymnastic Rings")`. Detik
                // kamu menamainya "Ring kayu bikinan sendiri", atau beli TRX,
                // atau punya palang portabel -- slidernya tidak muncul, dan tidak
                // ada satu pun error yang memberitahu kenapa. Dengan sakelar,
                // yang menentukan bukan NAMA tapi IZIN yang kamu berikan
                // sendiri. Data yang mengatur perilaku, bukan kode yang menebak.
                //
                // Seluruh baris ini bisa ditekan, bukan cuma sakelarnya. Kotak
                // sentuh sebesar barisnya itu aturan Material 3 yang paling
                // sering dilewatkan orang: sakelar aslinya cuma ~52x32dp, dan
                // 32dp itu di bawah 48dp minimum yang nyaman untuk jempol.
                // ==================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { bisaDiatur = !bisaDiatur }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.alat_adjustable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.alat_adjustable_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = bisaDiatur,
                        // `onCheckedChange = null` akan membuat sakelarnya
                        // mati total. Yang kita mau: dia tetap bisa ditekan
                        // sendiri, DAN barisnya juga bisa ditekan. Dua jalan
                        // ke satu perubahan yang sama.
                        onCheckedChange = { bisaDiatur = it },
                    )
                }

                // Tombol hapus cuma ada di mode ubah, dan sengaja ditaruh di
                // dalam isi dialog -- bukan di deretan tombol bawah bersama
                // Simpan/Batal. Alasannya keselamatan jempol: aksi yang tidak
                // bisa dibatalkan tidak boleh duduk sebelah aksi yang paling
                // sering ditekan.
                if (awal != null) {
                    TextButton(
                        onClick = {
                            // Kalau user sudah memilih foto baru lalu memilih
                            // menghapus alatnya, salinan baru itu tidak akan
                            // pernah terpakai. Buang sekarang.
                            if (foto.isNotBlank() && foto != fotoAwal) onBuangFoto(foto)
                            onMintaHapus()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.alat_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nama.isNotBlank() && !menyalin,
                onClick = { onSimpan(nama.trim(), foto, bisaDiatur) },
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { batal() }) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
