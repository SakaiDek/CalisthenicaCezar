# Calisthenica Cezar 🏋️‍♂️🔥

Aplikasi pelacak latihan kalistenik modern yang dibangun secara *native* untuk Android dengan fokus pada performa lokal, privasi, dan pengalaman pengguna yang mulus. Terinspirasi dari estetika aplikasi fitness kelas atas.

## 🚀 Fitur Unggulan
* **Dasbor & Streak Lokal:** Memantau konsistensi latihan harian secara instan menggunakan `java.time.LocalDate` (minSdk 26).
* **Grid Kalender Interaktif:** Menandai hari-hari latihan dan terhubung langsung ke detail riwayat sesi.
* **Floating Undo/Redo (5 Titik):** Membatalkan atau mengembalikan aksi (geser, duplikat, hapus, tambah, edit) dengan bilah melayang yang ergonomis.
* **Snapshot Foto Riwayat:** Menyimpan salinan foto gerakan dalam log latihan (`fotoUri`) agar riwayat masa lalu tetap hidup meskipun master gerakan di katalog dihapus.
* **Floating Bottom Navigation:** Navigasi modern bergaya *Die Ringe* yang bersih dan mudah dijangkau jempol.

## 📱 Tech Stack
* **UI:** Jetpack Compose, Material 3
* **Database:** Room DB (Local-first)
* **Asynchronous:** Kotlin Coroutines & Flow
* **Image Loading:** Coil