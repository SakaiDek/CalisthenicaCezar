package com.cezar.calisthenica.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * PALET "BLUEPRINT", 5 September 2026.
 *
 * Angka-angka di bawah ini TIDAK saya karang. Delapan gambar blueprint-mu saya
 * kecilkan, lalu saya hitung warna apa yang paling banyak menutupi pikselnya.
 * Hasilnya konsisten di kedelapan gambar, dan tiga angka ini muncul terus:
 *
 *     latar          #09080A .. #0D0A0B   -> hampir hitam murni
 *     oranye terang  #FB8D05 .. #FC9005   -> tombol & elemen aktif
 *     amber gelap    #54290D .. #5B2C06   -> chip terpilih, kartu menyala
 *
 * Kenapa saya repot mengukur, bukan menebak "pokoknya oranye"? Karena "oranye"
 * itu keluarga besar. #FF7A00 (oranye kemerahan) dan #FB8D05 (oranye keemasan)
 * dua-duanya sah disebut oranye, tapi ditempel di layar hitam hasilnya terasa
 * beda: yang pertama seperti peringatan bahaya, yang kedua seperti sinar. Kamu
 * sudah memilih yang kedua di blueprint-mu. Tugas saya menyalinnya, bukan
 * menafsirkannya.
 *
 * PELAJARAN HARI INI -- kenapa Material 3 minta BANYAK warna, bukan satu:
 * satu warna aksen saja tidak cukup, karena warna butuh PASANGAN. Setiap
 * "primary" wajib punya "onPrimary" (warna tulisan DI ATAS-nya). Itu sebabnya
 * kamu tidak akan pernah menemukan tulisan hitam di atas latar hitam di app
 * Material 3 yang ditulis benar: warnanya selalu datang berpasangan, dan
 * komponen M3 selalu memakai pasangannya, bukan menebak sendiri.
 */

// ============================================================
//  GELAP -- ini yang sebenarnya kamu pakai sehari-hari.
//  Blueprint-mu gelap semua, dan HP-mu memang di mode malam.
// ============================================================

// Oranye terang: dipakai tombol isi, FAB, indikator langkah aktif.
val DarkPrimary = Color(0xFFFB8D05)
// Tulisan di ATAS oranye terang. Coklat sangat gelap, bukan hitam murni --
// hitam murni di atas oranye terasa terlalu keras dan bikin huruf "bergetar".
val DarkOnPrimary = Color(0xFF2A1400)
// Amber gelap yang saya ukur dari blueprint. Ini "oranye yang diredam":
// dipakai untuk area besar yang perlu terlihat aktif tanpa menyilaukan.
val DarkPrimaryContainer = Color(0xFF552A0C)
val DarkOnPrimaryContainer = Color(0xFFFFDCBE)

// Amber keemasan yang lebih tenang. Wadahnya (secondaryContainer) itu warna
// yang dipakai FilterChip saat TERPILIH dan kartu katalog saat tercentang --
// jadi angka ini yang paling sering kamu lihat berubah di layar.
val DarkSecondary = Color(0xFFD8A55C)
val DarkOnSecondary = Color(0xFF3A2A0A)
val DarkSecondaryContainer = Color(0xFF5A3A12)
val DarkOnSecondaryContainer = Color(0xFFFFDDB3)

// Biru-slate. Ada di blueprint-mu (#262F4D, #2C3A42) dan gunanya penting:
// pembanding dingin. Kalau SEMUA aksen oranye, tidak ada yang menonjol lagi --
// mata butuh satu warna lain untuk mengukur. Ini dipakai penanda satuan
// (Repetisi / Detik), bukan untuk tombol.
val DarkTertiary = Color(0xFFA8C7E0)
val DarkOnTertiary = Color(0xFF0C2233)
val DarkTertiaryContainer = Color(0xFF2C3A42)
val DarkOnTertiaryContainer = Color(0xFFCFE5F6)

// Hampir hitam, sedikit condong ke ungu supaya tidak terasa "mati".
val DarkBackground = Color(0xFF0B0A0C)
val DarkOnBackground = Color(0xFFECE6E1)
val DarkSurface = Color(0xFF0B0A0C)
val DarkOnSurface = Color(0xFFECE6E1)

// Abu-abu netral yang saya ukur (#3D3D40). Ini latar kotak foto kosong dan
// kotak isian -- yang membedakan "ada wadahnya" dari "layar kosong".
val DarkSurfaceVariant = Color(0xFF3D3C41)
// Naik dari #9C9B9C hasil ukuran ke #B5AFAC dengan alasan yang bisa dihitung:
// #9C9B9C di atas #3D3C41 rasio kontrasnya cuma 4,4:1, di bawah ambang 4,5:1
// yang dipakai standar aksesibilitas untuk teks biasa. #B5AFAC jadi 5,0:1.
// Empat unit warna yang tidak akan kamu sadari; selisih yang bikin teks
// keterangan tetap terbaca di bawah matahari saat kamu latihan di luar.
val DarkOnSurfaceVariant = Color(0xFFB5AFAC)
// Garis tepi OutlinedTextField. Sama ceritanya: hasil ukuran #5C5B5C hanya
// 2,96:1 di atas hitam, sedikit di bawah ambang 3:1 untuk garis. #706C6A = 4:1.
val DarkOutline = Color(0xFF706C6A)

// ------------------------------------------------------------
//  TANGGA WADAH (ditambah 5 Sept 2026)
//
//  PELAJARAN PERTAMA HARI INI, dan ini penyebab keluhanmu "surface-nya nggak
//  konsisten" yang selama ini saya belum jawab dengan benar.
//
//  Material 3 tidak cuma punya `surface`. Dia punya TANGGA: surfaceContainer
//  Lowest -> Low -> (biasa) -> High -> Highest. Gunanya menyatakan kedalaman
//  TANPA bayangan: makin ke dalam susunan, makin terang sedikit. Di mode gelap
//  itu satu-satunya cara mata membedakan "kartu" dari "latar", karena bayangan
//  hitam di atas hitam tidak terlihat sama sekali.
//
//  Yang terjadi di app kita sampai hari ini: kita cuma mengisi `surface`, dan
//  `Card` bawaan M3 TIDAK memakai `surface` -- dia memakai surfaceContainerLow.
//  Slot yang tidak kita isi tidak dibiarkan kosong oleh Compose; dia diisi
//  DEFAULT BAWAAN M3, dan default bawaan itu abu-abu KEUNGUAN (#1D1B20).
//  Jadi setiap kartu di app oranye-hitam kita sebenarnya memakai wadah ungu
//  pucat, sementara latarnya hitam hangat buatan kita. Itu selisih yang tidak
//  bisa kamu sebut namanya tapi bisa kamu lihat -- dan kamu memang melihatnya.
//
//  Angkanya saya susun beda 14-16 unit per tingkat. Itu bukan selera: di bawah
//  ~10 unit dua tingkat tidak terbedakan di layar OLED terang, di atas ~25 unit
//  kartunya mulai terlihat "abu-abu" bukan "hitam yang terangkat".
// ------------------------------------------------------------

// Paling gelap dari semuanya. Ini yang saya pakai jadi bilah hitam di kiri-kanan
// foto potret (letterbox). Sengaja lebih gelap dari latar supaya foto terasa
// mengapung, bukan tenggelam.
val DarkSurfaceContainerLowest = Color(0xFF060507)
val DarkSurfaceContainerLow = Color(0xFF141114)
// Ini yang dipakai kartu seksi di Langkah 2 (Alat, Bahu, Dada, ...).
val DarkSurfaceContainer = Color(0xFF1A171A)
val DarkSurfaceContainerHigh = Color(0xFF241F22)
val DarkSurfaceContainerHighest = Color(0xFF2E2829)
val DarkSurfaceDim = Color(0xFF0B0A0C)
val DarkSurfaceBright = Color(0xFF322C2E)

// Garis pemisah TIPIS -- beda tugas dari `outline`. `outline` itu tepi benda
// yang bisa disentuh (kotak isian, chip); `outlineVariant` itu garis pembatas
// yang cuma memisahkan, tidak mengajak diklik. Kalau slot ini kosong, M3
// mengisinya #49454F: ungu lagi.
val DarkOutlineVariant = Color(0xFF3A3436)
val DarkScrim = Color(0xFF000000)

// Merah untuk aksi merusak (tombol "Hapus program", teks gagal salin foto).
// App sudah memakai `colorScheme.error` sejak 4 Sept, tapi angkanya selama ini
// milik M3, bukan milik kita. Sekarang resmi jadi milik kita.
val DarkError = Color(0xFFFFB4A9)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD4)

// Dipakai Snackbar: latarnya sengaja TERBALIK dari tema supaya pemberitahuan
// terasa menempel di atas app, bukan bagian dari app.
val DarkInverseSurface = Color(0xFFECE6E1)
val DarkInverseOnSurface = Color(0xFF201A18)
val DarkInversePrimary = Color(0xFF8A4B00)

// ============================================================
//  TERANG -- cadangan, kalau HP-mu dipindah ke mode siang.
//  Warna oranye yang sama, digelapkan supaya terbaca di atas putih.
// ============================================================

val LightPrimary = Color(0xFF8A4B00)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFDCBE)
val LightOnPrimaryContainer = Color(0xFF2C1600)
val LightSecondary = Color(0xFF755A42)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFDDB3)
val LightOnSecondaryContainer = Color(0xFF2A1800)
val LightTertiary = Color(0xFF3D5A73)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFC8E2FA)
val LightOnTertiaryContainer = Color(0xFF001E30)
val LightBackground = Color(0xFFFFF8F4)
val LightOnBackground = Color(0xFF201A16)
val LightSurface = Color(0xFFFFF8F4)
val LightOnSurface = Color(0xFF201A16)
val LightSurfaceVariant = Color(0xFFF1E0D2)
val LightOnSurfaceVariant = Color(0xFF51443A)
val LightOutline = Color(0xFF837469)

// Tangga wadah versi terang. Arahnya KEBALIKAN dari mode gelap: makin ke dalam
// susunan, makin GELAP sedikit. Logikanya sama -- yang di atas harus lebih dekat
// ke cahaya. Kamu tidak akan sering melihat blok ini (HP-mu mode malam), tapi
// tanpa ini mode siang akan menampilkan kartu ungu pucat yang sama.
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFFF1E8)
val LightSurfaceContainer = Color(0xFFFCEBE1)
val LightSurfaceContainerHigh = Color(0xFFF6E5DB)
val LightSurfaceContainerHighest = Color(0xFFF0DFD6)
val LightSurfaceDim = Color(0xFFE7E0DA)
val LightSurfaceBright = Color(0xFFFFF8F4)

val LightOutlineVariant = Color(0xFFD5C3B6)
val LightScrim = Color(0xFF000000)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightInverseSurface = Color(0xFF362F2B)
val LightInverseOnSurface = Color(0xFFFBEEE8)
val LightInversePrimary = Color(0xFFFFB876)
