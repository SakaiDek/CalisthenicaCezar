package com.cezar.calisthenica.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cezar.calisthenica.R

/**
 * Dialog kecil untuk bikin program baru.
 *
 * Perhatikan bentuk fungsinya: dia TIDAK tahu apa itu Room, tidak tahu apa itu
 * database. Dia cuma bilang "user menekan Simpan, ini judul dan catatannya",
 * lewat `onSave`. Yang memutuskan mau diapakan data itu adalah HomeScreen.
 *
 * Pola ini namanya "state hoisted" -- kendali diangkat ke atas. Untungnya:
 * dialog ini bisa dipakai ulang nanti untuk mode EDIT program tanpa diubah
 * sedikit pun, dan kalau ada bug penyimpanan, kamu tahu pasti bukan di sini.
 */
@Composable
fun NewProgramDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, description: String) -> Unit,
) {
    // Dua kotak memori: satu untuk isi kolom judul, satu untuk catatan.
    // Di Compose, kotak teks TIDAK menyimpan isinya sendiri. Kamu yang pegang
    // nilainya, lalu kamu suruh dia menampilkan nilai itu. Terasa berputar,
    // tapi inilah yang bikin isi dialog bisa dipulihkan kapan pun.
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_new_program_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(R.string.dialog_field_title)) },
                    placeholder = { Text(text = stringResource(R.string.dialog_field_title_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text = stringResource(R.string.dialog_field_desc)) },
                    placeholder = { Text(text = stringResource(R.string.dialog_field_desc_hint)) },
                    // Boleh 3 baris, biar catatan panjang tetap kebaca.
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                // Tombol mati kalau judul masih kosong. Ini validasi paling
                // murah dan paling sopan: user tidak perlu ditegur pakai
                // pesan error, dia langsung lihat tombolnya belum bisa
                // ditekan. isNotBlank(), bukan isNotEmpty() -- spasi doang
                // tetap dihitung kosong.
                enabled = title.isNotBlank(),
                onClick = { onSave(title.trim(), description.trim()) },
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
