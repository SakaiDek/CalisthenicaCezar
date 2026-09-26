package com.cezar.calisthenica

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache

/**
 * KELAS APPLICATION. Ini benda pertama yang hidup saat app dibuka dan benda
 * terakhir yang mati, jadi apa pun yang ditaruh di sini umurnya SEUMUR PROSES --
 * bukan seumur layar.
 *
 * Namanya `CezarApp`, bukan `CalisthenicaApp`, dan itu bukan selera: di
 * `MainActivity.kt` sudah ada composable bernama `CalisthenicaApp()` di package
 * yang sama. Dua benda dengan nama sama di satu package = build merah.
 *
 * ---------------------------------------------------------------------------
 * PELAJARAN PERTAMA HARI INI: kenapa "memory cache" Coil butuh kelas Application,
 * padahal kamu sudah pakai Coil dari kemarin dan gambarnya muncul.
 *
 * Coil punya SATU ImageLoader bersama yang dia bikin sendiri kalau kamu tidak
 * menyediakannya. Cache-nya nempel di ImageLoader itu, bukan di layarmu. Kalau
 * kita bikin ImageLoader dari dalam Activity (`Coil.setImageLoader(...)` di
 * `onCreate`), setiap kali Activity lahir ulang -- HP diputar, tema berubah,
 * bahasa berubah -- kita bikin ImageLoader BARU, dan cache lama dibuang bersama
 * yang tua. Hasilnya kebalikan dari yang kita mau: bukannya gambar makin cepat,
 * dia malah didekode ulang dari nol setiap rotasi.
 *
 * Application tidak lahir ulang saat rotasi. Makanya ImageLoader tinggal di sini.
 * Cara Coil menemukannya: kelas ini mengimplementasikan `ImageLoaderFactory`, dan
 * Coil otomatis memanggil `newImageLoader()` sekali saja, saat pertama kali ada
 * gambar diminta. Tidak ada satu baris pun yang perlu diubah di layar mana pun.
 *
 * Satu-satunya syarat: kelas ini WAJIB didaftarkan di AndroidManifest lewat
 * `android:name=".CezarApp"`. Tanpa baris itu Android memakai Application bawaan,
 * kelas ini tidak pernah dipanggil, dan seluruh file ini jadi hiasan mati tanpa
 * satu pun peringatan dari compiler. Sudah saya daftarkan; kalau suatu hari
 * gambar terasa lambat lagi, periksa baris manifest itu SEBELUM curiga ke Coil.
 * ---------------------------------------------------------------------------
 */
class CezarApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        /*
         * CACHE MEMORI, dan angka 25% itu ada hitungannya.
         *
         * Coil sudah menyalakan cache memori secara bawaan (~20% dari jatah RAM
         * app). Yang saya lakukan di sini bukan "menyalakan", tapi MEMBESARKAN
         * dan menuliskannya supaya kelihatan.
         *
         * Kenapa persen dan bukan megabyte: Android memberi setiap app jatah RAM
         * (`largeHeap` mati = biasanya 192-512MB tergantung HP). Menulis "64MB"
         * artinya di HP kentang app-mu boros dan dibunuh sistem, di Poco F5-mu
         * malah kekecilan. Persen menyesuaikan diri sendiri.
         *
         * Kenapa tidak 50%: cache itu tetangga yang tinggal serumah dengan
         * bitmap yang SEDANG dipakai layar. Kalau cache boleh makan setengah
         * jatah, sisanya tidak cukup untuk menggambar, dan Android mulai
         * membuang -- yang justru bikin patah-patah. 25% itu titik aman yang
         * dipakai Coil, Glide, dan Fresco.
         */
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        /*
         * DOWNSAMPLING + separuh memori per gambar.
         *
         * RGB_565 artinya satu piksel disimpan dalam 2 byte, bukan 4: merah 5
         * bit, hijau 6, biru 5, dan TANPA saluran transparansi. Untuk foto alat
         * dan foto gerakan -- JPEG dari galeri, yang memang tidak punya bagian
         * transparan -- ini gratis: separuh RAM, separuh data yang harus dikirim
         * ke GPU tiap frame, gambar terlihat sama.
         *
         * Kata "allow", bukan "force", dan bedanya penting: Coil hanya memakainya
         * kalau gambarnya DIJAMIN tidak punya alpha. PNG ikon transparan tetap
         * didekode 4 byte per piksel, jadi tidak ada yang rusak.
         *
         * Yang TIDAK saya lakukan: memaksa `bitmapConfig(RGB_565)`. Itu berlaku
         * ke semua gambar termasuk yang butuh alpha, dan gradasi gelap -- persis
         * latar hitam #0B0A0C kita -- akan terlihat bergaris (banding).
         */
        .allowRgb565(true)
        /*
         * Crossfade DIMATIKAN, dan ini penawaran yang saya ambil sendiri.
         *
         * Crossfade itu animasi transparansi 100-200ms per gambar. Satu gambar
         * tidak terasa. Tapi di daftar yang di-scroll, setiap baris yang masuk
         * layar memulai animasinya sendiri -- lima baris berarti lima animasi
         * jalan bersamaan, tiap frame-nya minta layer transparan baru. Di daftar
         * alat yang kamu keluhkan patah-patah, ini ongkos yang tidak membeli
         * apa-apa: fotonya sudah ada di cache, jadi yang dianimasikan cuma
         * "muncul dari cache" yang seharusnya instan.
         *
         * Nilai bawaan Coil sebenarnya sudah mati; saya tulis eksplisit supaya
         * besok tidak ada yang menyalakannya "biar halus" tanpa membaca alasan
         * ini dulu.
         */
        .crossfade(false)
        .build()
}
