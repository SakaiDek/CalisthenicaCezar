package com.cezar.calisthenica.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Tag untuk semua pesan log dari file ini.
 *
 * ATURAN BARU DI PROYEK INI, tolong dipegang: semua tag log kita mulai dengan
 * "Cezar". Alasannya praktis, bukan estetika. Log di HP itu satu ember besar
 * berisi pesan dari SEMUA app -- WhatsApp, launcher, sistem, semuanya. Kalau
 * tag kita bernama "MediaFiles", dia tenggelam dan `4-ambil-log-hp.bat` tidak
 * bisa menyaringnya keluar. Dengan awalan "Cezar", satu kata saring saja sudah
 * menangkap seluruh pesan buatan kita, dari file mana pun.
 *
 * Tag TIDAK boleh panjang: Android dulu memotongnya di 23 karakter.
 */
private const val TAG = "CezarMedia"

/**
 * Tempat semua urusan FILE MEDIA. Bukan database, bukan UI -- cuma "salin file
 * masuk, hapus file keluar".
 *
 * PELAJARAN PERTAMA HARI INI, dan ini pelajaran yang bikin banyak app galeri
 * rusak besok paginya: URI galeri itu TIKET MASUK, bukan alamat rumah.
 *
 * Waktu kamu memilih foto lewat Photo Picker, Android memberi app-mu sesuatu
 * seperti `content://media/picker/0/com.android.providers.media.photopicker/media/1000000034`.
 * Kelihatannya seperti alamat file, dan kalau kamu simpan teks itu ke database
 * lalu tampilkan, HARI INI dia jalan. Besok, setelah HP di-restart atau proses
 * app dimatikan sistem, izin bacanya hangus dan gambarnya jadi kotak kosong --
 * padahal fotonya masih ada di galeri, dan datanya masih ada di database. Bug
 * yang paling bikin frustrasi karena tidak ada error, tidak ada crash, cuma
 * gambar yang hilang tanpa alasan.
 *
 * Jadi begitu kamu memilih foto, kita langsung SALIN isinya ke folder privat
 * app (`filesDir`), lalu yang disimpan ke database cuma NAMA FILE-nya. Nama
 * file di folder sendiri itu milik app selamanya: tidak butuh permission, tidak
 * bisa hangus, dan tetap ada walau fotonya kamu hapus dari galeri.
 *
 * Ongkosnya: satu file jadi dua salinan di HP. Itu memang harga yang kita
 * bayar, dan untuk katalog gerakan yang isinya puluhan foto kecil, itu murah.
 */

/**
 * Salin gambar dari Uri galeri ke folder privat app. Mengembalikan NAMA FILE-nya,
 * atau null kalau gagal.
 *
 * `withContext(Dispatchers.IO)` itu wajib, bukan hiasan. Menyalin file itu
 * pekerjaan menunggu disk. Kalau dijalankan di thread utama, layar berhenti
 * menggambar selama penyalinan -- untuk foto kamera 5MB itu bisa setengah detik
 * penuh, dan Android akan menuduh app-mu "Not Responding" kalau kebetulan lama.
 * IO itu kolam thread khusus untuk pekerjaan menunggu semacam ini.
 *
 * Kenapa mengembalikan null dan bukan melempar Exception: gagal menyalin gambar
 * BUKAN keadaan darurat. User salah pilih file rusak, atau kartu memori penuh.
 * App tidak boleh mati karena itu; cukup foto tidak terpasang dan sisa formnya
 * tetap bisa disimpan.
 *
 * PELAJARAN HARI INI: "mengembalikan null" dan "MENELAN alasannya" itu dua hal
 * berbeda, dan sampai hari ini file ini melakukan dua-duanya.
 *
 * Versi lamanya menulis `catch (e: Exception) { null }`. Object `e` itu memegang
 * seluruh cerita -- kartu penuh? file rusak? izin dicabut? -- lalu dibuang tanpa
 * dibaca siapa pun. Akibatnya, kalau kamu melapor "fotonya gagal terus", saya
 * TIDAK PUNYA APA-APA untuk dibaca. Kita cuma bisa saling menebak.
 *
 * Sekarang setiap jalan gagal menulis satu baris ke logcat. Perhatikan pilihan
 * tingkatnya, karena bukan semuanya sama:
 *   Log.e = ERROR, sesuatu yang MEMANG SALAH dan mungkin bug kita.
 *   Log.w = WARNING, aneh tapi masih wajar terjadi.
 * Membedakan dua ini bukan formalitas: kalau semuanya ditandai ERROR, log-mu
 * penuh alarm palsu dan alarm yang asli jadi tidak kelihatan.
 *
 * Cara memakainya nanti: klik `4-ambil-log-hp.bat`, lakukan hal yang gagal itu
 * di HP, lalu bilang ke saya "baca logcat".
 */
suspend fun simpanGambarKeInternal(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            // Nama file pakai cap waktu, bukan nama asli dari galeri.
            // Dua alasan: (1) nama asli bisa sama ("IMG_20260101.jpg" dua kali)
            // dan yang kedua akan menimpa yang pertama secara diam-diam;
            // (2) nama asli bisa mengandung karakter aneh atau spasi yang bikin
            // pusing belakangan. Cap waktu selalu unik dan selalu aman.
            val namaFile = "thumb_${System.currentTimeMillis()}.jpg"
            val tujuan = File(context.filesDir, namaFile)

            val masuk = context.contentResolver.openInputStream(uri)
            if (masuk == null) {
                // Ini kejadian nyata, bukan teori: user memilih foto dari
                // Google Photos yang isinya masih di cloud dan belum terunduh
                // ke HP. Uri-nya sah, tapi tidak ada isi yang bisa dibaca.
                // Tanpa baris ini, gejalanya cuma "foto tidak muncul" tanpa
                // sebab -- dan kita akan mencurigai Coil, database, form,
                // semuanya kecuali yang benar.
                Log.e(TAG, "openInputStream balikan null. Uri=$uri")
                return@withContext null
            }

            // .use{} = "pakai lalu tutup, apa pun yang terjadi". Tanpa ini,
            // satu error di tengah penyalinan meninggalkan file yang terbuka
            // selamanya sampai app ditutup. Sepuluh kali begitu, app-mu
            // kehabisan file handle -- error yang penyebabnya kelihatan sama
            // sekali tidak berhubungan.
            masuk.use { sumber ->
                tujuan.outputStream().use { keluar ->
                    sumber.copyTo(keluar)
                }
            }
            namaFile
        } catch (e: Exception) {
            // Perhatikan `e` diikutkan sebagai ARGUMEN KETIGA, bukan ditempel
            // ke dalam teks pesan pakai "$e". Bedanya besar: sebagai argumen
            // ketiga, logcat mencetak seluruh jejak tumpukan (stack trace) --
            // baris demi baris sampai persis di file dan nomor baris tempat
            // kejadiannya. Ditempel ke teks, kamu cuma dapat satu kalimat
            // tanpa alamat, dan alamat itulah yang paling kita butuhkan.
            Log.e(TAG, "Gagal menyalin gambar. Uri=$uri", e)
            null
        }
    }

/**
 * Hapus satu file media dari folder privat app.
 *
 * KENAPA fungsi ini harus ada: kalau gerakan dihapus dari database tapi
 * filenya tidak, foto itu jadi FILE YATIM -- masih memakan tempat di HP tapi
 * tidak ada satu pun baris database yang menunjuk ke dia, jadi tidak ada cara
 * lagi untuk melihat atau menghapusnya dari dalam app. Sepuluh gerakan dihapus,
 * sepuluh foto nyangkut permanen. Setahun kemudian kamu bingung kenapa app
 * kalistenik memakan 200MB.
 *
 * Aman dipanggil dengan nama kosong atau nama yang filenya sudah tidak ada:
 * dua-duanya cuma menghasilkan `false`, bukan error. Tapi mulai sekarang
 * dua-duanya juga MENINGGALKAN JEJAK di logcat -- dengan tingkat yang berbeda,
 * dan bedanya itu isi pelajarannya. Baca komentar di dalam badan fungsinya.
 */
suspend fun hapusFileInternal(context: Context, namaFile: String): Boolean =
    withContext(Dispatchers.IO) {
        // Nama kosong artinya gerakan itu memang tidak berfoto. Ini kejadian
        // NORMAL, dan sengaja TIDAK dicatat. Log yang mencatat hal normal sama
        // tidak bergunanya dengan log yang tidak mencatat apa-apa: dua-duanya
        // bikin kamu tidak bisa menemukan yang penting.
        if (namaFile.isBlank()) return@withContext false
        try {
            val f = File(context.filesDir, namaFile)
            if (!f.exists()) {
                // WARNING, bukan ERROR. Ini masih bisa wajar -- misal form
                // dibatalkan sebelum fotonya selesai tersalin, jadi database
                // sudah menyebut satu nama yang filenya tidak pernah jadi.
                //
                // TAPI kalau baris ini muncul SETIAP kali kamu menghapus
                // gerakan yang jelas-jelas berfoto di layar, artinya nama yang
                // kita simpan di database tidak sama dengan nama file yang
                // benar-benar ada di HP. Itu bug serius, dan tanpa baris ini
                // kamu tidak akan pernah tahu -- karena gejalanya nol: gerakan
                // tetap terhapus, layar tetap benar, cuma penyimpanan HP-mu
                // pelan-pelan penuh oleh foto yang tidak ada pemiliknya.
                Log.w(TAG, "Tidak ada yang dihapus, file tidak ada: $namaFile")
                return@withContext false
            }
            val terhapus = f.delete()
            if (!terhapus) {
                // Ini jalan gagal PALING SUNYI di seluruh Java: `delete()`
                // mengembalikan `false` tanpa melempar exception apa pun.
                // Filenya ada, perintahnya dijalankan, jawabannya "tidak", dan
                // tidak ada satu pun mekanisme yang memaksa kamu memeriksanya.
                // Makanya ERROR: kalau ini muncul, foto itu resmi jadi file
                // yatim dan tidak ada lagi jalan menghapusnya dari dalam app.
                Log.e(TAG, "delete() menolak menghapus: $namaFile")
            }
            terhapus
        } catch (e: Exception) {
            Log.e(TAG, "Error saat menghapus file: $namaFile", e)
            false
        }
    }

/**
 * Ubah nama file jadi File utuh yang bisa dibaca Coil. Null kalau namanya kosong.
 *
 * Ini kebalikan dari keputusan di atas: database menyimpan nama, tapi Coil butuh
 * benda yang bisa dibuka. Konversinya sengaja dikumpulkan di SATU fungsi supaya
 * kalau suatu hari kita pindah folder (misal ke `cacheDir` atau ke sub-folder
 * "thumbs/"), yang perlu diubah cuma baris ini -- bukan setiap layar yang
 * kebetulan menampilkan gambar.
 */
fun fileGambar(context: Context, namaFile: String): File? =
    if (namaFile.isBlank()) null else File(context.filesDir, namaFile)

/**
 * Baca RASIO (lebar / tinggi) sebuah foto tanpa membuka fotonya.
 *
 * PELAJARAN KEDUA HARI INI, dan ini yang menyelamatkan RAM 8GB-mu sekaligus RAM
 * HP-mu: kamu TIDAK perlu memuat gambar untuk tahu ukurannya.
 *
 * Foto 12 megapiksel dari galeri, kalau dibuka jadi bitmap, memakan sekitar
 * 4 lebar x tinggi byte = ~48 MB memori. Untuk satu foto. Dan yang kita
 * butuhkan cuma dua angka.
 *
 * `inJustDecodeBounds = true` menyuruh Android membaca KEPALA file saja --
 * belasan byte pertama yang menyebutkan "aku 1080x2400" -- lalu berhenti.
 * `decodeFile` sengaja mengembalikan null di mode ini, dan itu BUKAN kegagalan:
 * jawabannya tidak ada di nilai kembalian, tapi ditulis ke dalam `opsi`.
 * Ini API yang bentuknya aneh (peninggalan Android jaman awal), jadi jangan
 * heran kalau `null`-nya bikin kamu curiga -- memang begitu cara kerjanya.
 *
 * SOAL EXIF, dan ini jebakan yang bikin foto kamera terlihat "salah kotak":
 * kamera HP hampir selalu menyimpan piksel dalam posisi mendatar, lalu
 * menempelkan catatan kecil "tolong putar 90 derajat saat ditampilkan". Coil
 * MEMBACA catatan itu; `outWidth/outHeight` TIDAK. Jadi tanpa lima baris di
 * bawah, foto potret dari kamera akan kita ukur sebagai lanskap, kita siapkan
 * kotak lanskap, lalu Coil menampilkannya potret di dalamnya -- persis bug
 * "kok fotonya kekecilan di tengah" yang susah dilacak. Screenshot tidak punya
 * catatan ini, jadi di HP-mu belum kelihatan; foto kamera besok kelihatan.
 *
 * Balikan null artinya "tidak tahu" -- bukan error. Pemanggilnya wajib punya
 * rencana untuk "tidak tahu", dan di UI rencananya: pakai rasio bawaan.
 */
suspend fun rasioGambar(context: Context, namaFile: String): Float? =
    withContext(Dispatchers.IO) {
        val file = fileGambar(context, namaFile)
        if (file == null || !file.exists()) return@withContext null
        try {
            val opsi = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opsi)
            var lebar = opsi.outWidth
            var tinggi = opsi.outHeight
            if (lebar <= 0 || tinggi <= 0) {
                Log.w(TAG, "Kepala file gambar tidak terbaca: $namaFile")
                return@withContext null
            }

            // Kalau catatan EXIF menyebut foto ini diputar seperempat putaran,
            // lebar dan tinggi bertukar tempat. `runCatching` dipakai karena
            // PNG dan screenshot sering TIDAK punya blok EXIF sama sekali dan
            // ExifInterface melempar IOException untuk itu -- keadaan normal,
            // bukan kesalahan, jadi tidak layak dicatat sebagai error.
            val diputar = runCatching {
                val exif = ExifInterface(file.absolutePath)
                val orientasi = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
                orientasi == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientasi == ExifInterface.ORIENTATION_ROTATE_270 ||
                    orientasi == ExifInterface.ORIENTATION_TRANSPOSE ||
                    orientasi == ExifInterface.ORIENTATION_TRANSVERSE
            }.getOrDefault(false)

            if (diputar) {
                val simpan = lebar
                lebar = tinggi
                tinggi = simpan
            }

            Log.d(TAG, "Rasio $namaFile = ${lebar}x$tinggi (diputar=$diputar)")
            lebar.toFloat() / tinggi.toFloat()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca ukuran gambar: $namaFile", e)
            null
        }
    }
