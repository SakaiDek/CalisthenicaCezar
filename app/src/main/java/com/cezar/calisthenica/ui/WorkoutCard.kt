package com.cezar.calisthenica.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R
import com.cezar.calisthenica.data.RingkasanGrup
import com.cezar.calisthenica.model.Grup
import com.cezar.calisthenica.model.WorkoutProgram

/**
 * Satu kartu program latihan. Bisa ditekan untuk buka/tutup detail.
 *
 * Cara baca kode di bawah: dari LUAR ke DALAM, seperti kotak di dalam kotak.
 *
 *   ElevatedCard          <- kotak melayang (ada bayangannya), bisa ditekan
 *     Column              <- di dalam kartu, isinya ditumpuk atas ke bawah
 *       Text (judul)
 *       Text (deskripsi)
 *       Text ("6 gerakan - sekitar 18 menit")
 *       [detail]          <- hanya digambar kalau expanded == true
 *
 * ---------------------------------------------------------------------------
 * PELAJARAN HARI INI: kartu ini baru saja berhenti berbohong.
 *
 * Sampai kemarin, bagian "Struktur sesi" di bawah berisi TIGA BARIS TEKS YANG
 * SAYA TULIS TANGAN: "Group 1 - Pemanasan", dan seterusnya. Selalu sama, untuk
 * program apa pun, bahkan untuk program yang isinya nol gerakan. Dan angka
 * "8 gerakan / 45 menit" datang dari `program.exerciseCount` dan
 * `program.estimatedMinutes` -- dua kolom yang tidak pernah ada yang mengisi,
 * jadi isinya selalu 0.
 *
 * Itu bukan bug kecil. Itu app yang MENAMPILKAN SESUATU YANG BUKAN DATANYA, dan
 * jenis kebohongan ini paling berbahaya justru karena kelihatan meyakinkan: kamu
 * melihat kartunya penuh, lalu percaya fiturnya sudah jalan.
 *
 * Sekarang setiap angka di kartu ini datang dari `program_exercises` lewat
 * `observeRingkasan()`. Kalau kartunya bilang "0 gerakan", itu karena programnya
 * memang kosong -- dan itu kabar yang berguna.
 * ---------------------------------------------------------------------------
 *
 * KENAPA `ringkasan` DIKIRIM SEBAGAI PARAMETER, bukan dibaca sendiri di sini.
 *
 * Kartu ini bisa saja memanggil database sendiri: satu Flow per kartu, bertanya
 * "berapa gerakan di programku". Kelihatan lebih rapi -- kartunya jadi mandiri.
 * Tapi sepuluh kartu berarti SEPULUH langganan database yang masing-masing
 * membangunkan SQLite, dan setiap kartu yang lahir saat kamu scroll bikin
 * langganan baru. Itu resep tersendat.
 *
 * Jadi dasbor bertanya SEKALI untuk semua program, mengelompokkannya jadi Map,
 * lalu menyuapkan bagian masing-masing ke kartunya. Kartunya jadi "bodoh": dia
 * cuma menggambar apa yang dikasih. Komponen bodoh itu komponen yang gampang
 * dites dan tidak pernah mengejutkanmu.
 */
@Composable
fun WorkoutCard(
    program: WorkoutProgram,
    ringkasan: List<RingkasanGrup>,
    onRakit: () -> Unit,
    onMulai: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kartu ini punya DUA kotak memori sekarang: satu untuk buka/tutup detail,
    // satu lagi untuk "apakah dialog konfirmasi hapus sedang tampil".
    var confirmDelete by remember { mutableStateOf(false) }

    // ===================== INTI COMPOSE =====================
    // `remember { mutableStateOf(false) }` = kotak memori milik kartu ini.
    //
    // - mutableStateOf : nilai yang BOLEH berubah, dan setiap kali berubah
    //   Compose otomatis menggambar ulang bagian yang memakainya.
    // - remember       : jangan direset tiap kali digambar ulang. Tanpa ini,
    //   nilainya balik ke false terus dan kartunya tidak akan pernah kebuka.
    //
    // Analogi: ini stopwatch yang kamu pegang saat nahan leg up the wall.
    // Angkanya berubah (mutableState), tapi stopwatch-nya tidak kamu tukar
    // dengan yang baru setiap detik (remember).
    var expanded by remember { mutableStateOf(false) }
    // ========================================================

    // Dua penjumlahan biasa, TIDAK di-`remember`. Sengaja, dan ini patut
    // dijelaskan karena `remember` sering dipakai berlebihan: dia sendiri punya
    // biaya (menyimpan nilai lama, membandingkan kunci). Menjumlahkan tiga angka
    // itu jauh lebih murah daripada mengurus penyimpanannya. `remember` baru
    // pantas kalau perhitungannya berat ATAU hasilnya harus identik antar
    // recompose (misalnya objek yang dipakai sebagai kunci).
    val totalGerakan = ringkasan.sumOf { it.jumlahGerakan }
    val totalDetik = ringkasan.sumOf { it.totalDetik }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            // clickable = "kartu ini mendengarkan sentuhan". Inilah yang tadi
            // belum ada, makanya kartunya terasa mati.
            .clickable { expanded = !expanded }
            // animateContentSize = kalau tinggi kartu berubah, jangan lompat,
            // tapi melar/menyusut halus. Satu baris, efeknya besar.
            .animateContentSize(),
    ) {
        Column(
            // Jarak 16dp ke semua tepi kartu, biar teks tidak nempel ke pinggir.
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = program.title,
                style = MaterialTheme.typography.titleLarge,
            )

            // Spacer = ruang kosong. Sengaja pakai ini, bukan padding, karena
            // ini soal jarak ANTAR dua benda, bukan jarak ke tepi wadah.
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = program.description,
                style = MaterialTheme.typography.bodyMedium,
                // Warna "onSurfaceVariant" = teks sekunder di atas permukaan.
                // Kita minta PERAN warnanya, bukan kode warnanya. Jadi saat HP
                // pindah ke mode gelap, Material 3 tukar sendiri.
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Program yang baru kamu bikin belum punya gerakan sama sekali.
            // Menulis "0 gerakan, 0 menit" itu jujur tapi jelek dan tidak
            // menolong. Lebih baik satu baris yang menyebut keadaannya.
            if (totalGerakan == 0) {
                Text(
                    text = stringResource(R.string.card_no_exercise),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.card_total,
                        totalGerakan,
                        labelDurasi(totalDetik),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // `if` biasa di dalam UI. Ini kekuatan Compose: tidak ada
            // View.setVisibility(GONE) seperti di Android jadul. Kalau
            // syaratnya salah, benda ini memang TIDAK PERNAH dibuat.
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.card_session_structure),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(6.dp))

                /*
                 * TIGA GRUP SELALU DISEBUT, termasuk yang kosong.
                 *
                 * Kelihatannya boros -- kenapa menampilkan "Pendinginan: belum
                 * diisi"? Karena kartu ini juga berfungsi sebagai DAFTAR PERIKSA.
                 * Grup yang hilang dari daftar saat kosong bikin kamu tidak
                 * pernah sadar sudah lupa pendinginan tiga program berturut-turut.
                 *
                 * `firstOrNull` dan bukan `first`: grup yang belum punya satu pun
                 * gerakan TIDAK punya baris di hasil `GROUP BY`. Barisnya bukan
                 * "nol", barisnya TIDAK ADA. `first` akan melempar exception
                 * untuk keadaan yang paling normal di app ini, yaitu program yang
                 * baru dibuat.
                 */
                Grup.urut().forEach { grup ->
                    val baris = ringkasan.firstOrNull { it.grup == grup.nomor }
                    Text(
                        text = if (baris == null || baris.jumlahGerakan == 0) {
                            stringResource(R.string.card_group_empty, grup.label)
                        } else {
                            stringResource(
                                R.string.card_group_line,
                                grup.label,
                                baris.jumlahGerakan,
                                labelDurasi(baris.totalDetik),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (baris == null || baris.jumlahGerakan == 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                /*
                 * TIGA tindakan, DUA baris, dan pembagiannya bukan selera.
                 *
                 * Baris atas: dua tindakan yang kamu memang datang untuk
                 * melakukannya. "Susun gerakan" jadi OutlinedButton (garis luar,
                 * tanpa isi) dan "Mulai" jadi Button terisi. Di Material 3 itu
                 * yang namanya hierarki penekanan: dalam satu kelompok hanya
                 * BOLEH ada satu tombol terisi, karena kalau dua-duanya terisi
                 * matamu harus memutuskan sendiri mana yang utama, dan itu kerja
                 * yang seharusnya sudah diselesaikan oleh desainnya.
                 *
                 * Baris bawah, sendirian: "Hapus program". Aksi yang tidak bisa
                 * dibatalkan tidak boleh duduk bersebelahan dengan aksi yang
                 * paling sering kamu ketuk. Jempol yang meleset 6 milimeter
                 * jangan sampai menghapus program yang kamu susun setengah jam.
                 *
                 * KENAPA "Mulai" DIMATIKAN SAAT PROGRAMNYA KOSONG, dan ini
                 * pelajaran yang mau saya tanam: mencegah keadaan mustahil di
                 * PINTU MASUK jauh lebih murah daripada menanganinya di dalam.
                 * Runner tanpa gerakan berarti `rakitan[0]` atas daftar kosong,
                 * yaitu IndexOutOfBoundsException. Saya memang sudah memasang
                 * penjaga di dalam runner juga (frame pertama SELALU kosong,
                 * jadi penjaga itu wajib) -- tapi tombol mati bikin app-nya
                 * MENJELASKAN keadaannya, bukan cuma tidak crash. Tombol yang
                 * kelabu sudah bilang "isi dulu" tanpa satu kata pun.
                 */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onRakit,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.card_open_builder),
                            maxLines = 1,
                        )
                    }

                    Button(
                        onClick = onMulai,
                        enabled = totalGerakan > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.card_start),
                            maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }

                TextButton(onClick = { confirmDelete = true }) {
                    Text(
                        text = stringResource(R.string.card_delete),
                        // Warna "error" = peran warna untuk hal berbahaya.
                        // Lagi-lagi kita minta perannya, bukan "merah", jadi di
                        // mode gelap Material 3 pilih merah yang masih kebaca
                        // sendiri.
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    // Konfirmasi. Ini bukan formalitas: `delete` di Room itu permanen, tidak
    // ada tong sampah dan tidak ada Ctrl+Z. Satu ketukan salah = program yang
    // kamu susun setengah jam hilang.
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(text = stringResource(R.string.dialog_delete_title)) },
            text = { Text(text = stringResource(R.string.dialog_delete_body, program.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.dialog_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}
