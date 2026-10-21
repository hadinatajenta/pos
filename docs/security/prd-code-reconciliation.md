# Validasi PRD vs Kode — Fitur PIN pada Aksi Critical

**Tanggal:** 20 Oktober 2026
**Metode:** pembacaan langsung kode dan `docs/prd.md` (read-only). Tidak ada file kode yang diubah. Laporan ini adalah satu-satunya file yang ditulis.
**Aturan bukti:** setiap klaim membawa `path:baris`. Yang tidak dapat diverifikasi ditandai **TIDAK TERVERIFIKASI**.

---

## Ringkasan (10 baris)

1. Ditemukan **14 temuan**: 5 di antaranya **BLOCKING** dan 9 **NON-BLOCKING**.
2. BLOCKING terbesar: PRD §8 (baris 369) mewajibkan `cashier_shifts.pin_hash` dan `cashier_shifts.recovery_answer_hash`, tetapi entity `CashierShiftEntity` sama sekali tidak punya kolom itu.
3. PIN di kode sekarang hidup di `security_store`, tabel yang **tidak ada sama sekali di PRD §8** — jadi PRD dan kode memakai dua lokasi berbeda untuk data keamanan.
4. BLOCKING kedua: PRD §7.6 (baris 332) melarang "menahan file backup di balik PIN kasir", sementara rencana kita memberi PIN pada aksi export backup — perlu penafsiran Anda.
5. BLOCKING ketiga: PRD §7.7 (baris 342) dan §6.6 (baris 231) mewajibkan biometrik; keputusan Anda hanya kode pemulihan, dan tidak ada kode maupun dependency biometrik di repo.
6. BLOCKING keempat: `tutup shift` dan `void` yang Anda minta **tidak punya call site sama sekali**, jadi tidak bisa di-gate di Phase 3.
7. BLOCKING kelima: role kasir/manajer/owner tidak punya dasar di PRD maupun skema kode — keputusan D1/D2 memblokir desain gate.
8. Koreksi terhadap rencana saya sendiri: klaim Tahap C "mungkin tidak perlu migrasi karena kolom PIN sudah ada" **salah** — `security_store` hanya punya 3 kolom, sehingga `MIGRATION_5_6` **wajib**.
9. Koreksi kedua: D8 saya keliru arah — kolom `recovery_answer_hash` **ada**, tapi di `security_store`, bukan di `cashier_shifts` seperti kata PRD.
10. **Rekomendasi: revisi PRD dulu, lalu lanjut Phase 0.** Versi DB masih 5 (rencana migrasi valid), call site aksi Tabel A terbukti ada, dan kontrak keamanan memang masih murni usulan. Phase 0 belum dimulai.

---

## Bagian 1 — Matriks Kesesuaian PRD vs Kode

Status: **SESUAI** = PRD dan kode cocok · **TIDAK ADA** = tidak ada di kode · **BERBEDA** = ada di kedua sisi tetapi tidak sama · **AMBIGU** = PRD dapat dibaca lebih dari satu cara.

### 1a. Skema tabel keamanan (PRD §8 vs kode)

| ID | Requirement PRD | Status | Bukti kode (path:baris) | Bukti PRD | Dampak ke rencana PIN | Rekomendasi |
|---|---|---|---|---|---|---|
| R-01 | Tabel `security_store` (tempat PIN disimpan di kode) | BERBEDA | `data/local/entity/SecurityStoreEntity.kt:7` (`@Entity(tableName = "security_store")`); migrasi pembuatannya tidak ada karena tabel sudah ada sejak skema v1 → `app/schemas/id.umkm.pos.pro.data.local.UmkmDatabase/1.json` | PRD §8 baris 361–369: tabel tidak menyebut `security_store` sama sekali | PIN kita akan disimpan di tabel yang tidak diakui PRD — sumber kebenaran dan kode bertentangan | **Update PRD** (tambahkan entitas + kolom baru) |
| R-02 | `cashier_shifts.pin_hash` | TIDAK ADA | `data/local/entity/CashierShiftEntity.kt:15-22` — kolom hanya `id, cashier_name, opened_at, closed_at, starting_cash, expected_ending_cash, actual_ending_cash` | PRD §8 baris 369 | Menentukan **di mana** PIN disimpan; rencana saya menaruh PIN di `security_store`, PRD di `cashier_shifts` | **Update PRD** (atau pindahkan data — lihat C-B1) |
| R-03 | `cashier_shifts.recovery_answer_hash` | TIDAK ADA | sama, `CashierShiftEntity.kt:15-22`; tidak ada di seluruh `data/local/` (grep `recovery_answer_hash`: hanya `SecurityStoreEntity.kt:11`) | PRD §8 baris 369 | Rencana Anda "tanpa pertanyaan keamanan" bertabrakan dengan nama kolom ini | **Update PRD** |
| R-04 | Kolom lain `cashier_shifts`: `cashier_name`, `opened_at`, `closed_at`, `starting_cash`, `expected_ending_cash`, `actual_ending_cash` | SESUAI | `CashierShiftEntity.kt:17-22`; tipe: `cashier_name` String (non-null), `opened_at` Long (non-null), `closed_at` Long? (nullable), `starting_cash` Long (non-null), `expected_ending_cash` Long? (nullable), `actual_ending_cash` Long? (nullable) | PRD §8 baris 369 | Tidak ada (tabel shift tidak dipakai PIN) | — |
| R-05 | Hash jawaban pemulihan (konsep) | BERBEDA | `SecurityStoreEntity.kt:11` (`recovery_answer_hash` String?) | PRD §8 baris 369 (menaruhnya di `cashier_shifts`) | Keputusan D8 harus diubah: kolomnya **ada**, hanya salah tabel menurut PRD | **Update PRD**; kode: biarkan tak terpakai (lihat C-N4) |
| R-06 | `security_store.pin_hash` | BERBEDA | `SecurityStoreEntity.kt:10` (`pin_hash` String?, **nullable**, tanpa salt) | PRD §8: tidak ada | Hash PIN tidak punya kolom salt; butuh kolom/enkode baru | **Update kode + PRD** |
| R-07 | `security_store.is_pin_enabled` | BERBEDA | `SecurityStoreEntity.kt:12` (Boolean, non-null) | PRD §8: tidak ada | Flag ada di dua tempat (lihat R-21) | **Update PRD** |
| R-08 | Tabel `security_users` (untuk role) | TIDAK ADA | tidak ada entity, tidak ada DAO (daftar entity: 12 file di `data/local/entity/`, tidak ada `SecurityUserEntity`) | PRD: tidak menyebut manajemen user/staff kecuali kata "staff/PIN" di §4 baris 70 | Rencana role kasir/manajer/owner tidak punya tempat penyimpanan | **Eskalasi** (keputusan D1/D2) |
| R-09 | Audit log keamanan | TIDAK ADA | grep `AuditLog|audit_log` → 0 hasil di `app/src/main` | PRD: tidak menyebut audit log di seluruh dokumen | Kolom "Audit" di Tabel A tidak punya tabel tujuan | **Update PRD** (tambah entitas) |
| R-10 | Tabel `app_settings` (pembawa `is_pin_enabled` kedua) | BERBEDA | `data/local/entity/SettingsEntity.kt:14`; ditulis oleh `SecurityRepositoryImpl.kt:31` (`settingsDao.updatePinEnabled`) | PRD §8 baris 361–369: `app_settings` tidak ada di tabel | Setelah restore, `app_settings` terimpor dan bisa membawa `is_pin_enabled = 1` tanpa hash | **Update PRD + kode** (paksa nonaktif setelah restore) |

### 1b. Biometrik (PRD §7.7)

| ID | Requirement PRD | Status | Bukti kode | Bukti PRD | Dampak ke rencana PIN | Rekomendasi |
|---|---|---|---|---|---|---|
| R-11 | Biometrik perangkat untuk pemulihan/akses | TIDAK ADA | grep `BiometricPrompt\|androidx.biometric` → 0 hasil di `app/src/main`; dependency biometrik tidak ada di `app/build.gradle.kts` (blok `dependencies` baris 120–170) | PRD §7.7 baris 342; §6.6 baris 231; §3.1 baris 51; §10 baris 443 | Bertabrakan langsung dengan keputusan Anda "recovery hanya kode pemulihan" | **Update PRD** (nyatakan biometrik ditunda) — lihat C-B3 |

### 1c. Shift

| ID | Requirement PRD | Status | Bukti kode | Bukti PRD | Dampak ke rencana PIN | Rekomendasi |
|---|---|---|---|---|---|---|
| R-12 | Entity `cashier_shifts` ada | SESUAI | `data/local/entity/CashierShiftEntity.kt:8-23`; terdaftar di `data/local/UmkmDatabase.kt:42`; DAO `data/local/dao/ShiftDao.kt`; repo `data/local/repository/ShiftRepositoryImpl.kt:16,19,31` | PRD §8 baris 369 | Tidak memblokir PIN selama shift tidak di-gate | — |
| R-13 | UI buka/tutup shift | TIDAK ADA | grep `openShift\|closeShift\|getActiveShift` → pemanggil hanya ada di `app/DefaultAppContainer.kt:87`, `core/contract/Repositories.kt:49-51`, `ShiftRepositoryImpl.kt`, `ShiftDao.kt`, `PreviewAppContainer.kt:54`, dan fake test `androidTest/.../AppRootFakeContainer.kt:143-148`. **Tidak ada composable yang memanggilnya** | PRD §8 menyediakan modelnya; `docs/prd-gaps.md:120-122` menandai PARTIAL | Aksi "tutup shift" di Tabel A **tidak bisa di-gate** | **Tunda** — lihat C-B4 |
| R-14 | Kolom rekonsiliasi kas (`expected_ending_cash`, `actual_ending_cash`) ada | SESUAI | `CashierShiftEntity.kt:21-22` | PRD §8 baris 369 | Tidak ada | — |

### 1d. Call site aksi critical (Tabel A)

| ID | Aksi (Tabel A) | Status call site | Bukti kode | Bukti PRD | Dampak ke rencana PIN | Rekomendasi |
|---|---|---|---|---|---|---|
| R-15 | #1 Lihat laba & HPP di Laporan | ADA | `feature/reports/ReportsScreen.kt:294` (`summary.netProfit`), `:303` (`summary.totalHpp`), `:354`, `:369` | PRD §7.7 baris 338 ("allowing staff to view HPP") | Gate bisa dipasang; tetapi nilainya ditampilkan di kartu, bukan di balik satu aksi | Lanjut Phase 3 |
| R-16 | #2/#3 Edit HPP & harga produk | ADA | `ui/products/ProductFormPanel.kt:509-517` (label/value/error/focus `BASE_HPP`), `ui/products/ProductsViewModel.kt:372`, string `ui/common/PosStrings.kt:220` | Tidak disebut eksplisit (§7.7 baris 338 menyebut "view HPP") | HPP, harga jual, dan stok berada di **satu form yang sama** — menyembunyikan hanya HPP mengubah layout form | Lanjut + keputusan Q10 |
| R-17 | #4 Diskon item | ADA | `ui/cashier/CashierCart.kt:434` (field), string `ui/common/PosStrings.kt:135`, `IdStrings.kt:128`; engine: `domain/pos/DefaultPriceEngine.kt:91,105` | PRD §7.2 baris 258, 263 (`D_item`) | Threshold D3 bisa diterapkan | Lanjut (butuh angka D3) |
| R-18 | #5 Harga nego / price override | ADA | `ui/cashier/CashierCart.kt:425-426`, string `PosStrings.kt:133-134`; prioritas harga: `DefaultPriceEngine`; PRD §7.1 baris 244 | PRD §7.1 baris 244, 248 | Model sudah menyimpan `is_nego_price` di `transaction_items` (PRD §8 baris 367) | Lanjut |
| R-19 | #6 Refund / retur | ADA | Menu: `feature/transactions/TransactionActionsMenu.kt:110`; presenter: `feature/reports/ReportsPresenter.kt:191` (`requestReturn`); layar: `feature/transactions/TransactionsRoute.kt:106` (`ReturnRoute`); use case `domain/usecase/ProcessReturnUseCase.kt` | PRD §7.3 baris 289 (audit trail) | Gate bisa dipasang di jalur konfirmasi retur | Lanjut |
| R-20 | #7 Export laporan CSV/XLSX | ADA | `ReportsPresenter.kt:204` (`exportReport`); UI `feature/reports/ReportsScreen.kt:108`, `feature/reports/ReportsFilterSheet.kt:305` | PRD §7.4 baris 296; §7.8 baris 352 | **Sudah ada gate iklan berhadiah** di `ReportsPresenter.kt:210` → PIN akan jadi gate kedua | Lanjut + keputusan Q7 |
| R-21 | #8/#9 Export & restore backup JSON | ADA | Export: `ui/settings/SettingsBackupScreen.kt:112,116`, `ui/settings/SettingsViewModel.kt:382`. Restore: `SettingsBackupScreen.kt:71`, `SettingsViewModel.kt:398` | PRD §7.6 baris 328, 332 | Bertabrakan dengan larangan "hostage" di §7.6 baris 332 | **Eskalasi** — lihat C-B2 |
| R-22 | #10 Hapus produk | ADA | Dialog: `ui/products/ProductsRoute.kt:209`; handler `ui/products/ProductsViewModel.kt:230,236`; aksi `ui/products/ProductsActions.kt:20`; repo `data/local/repository/ProductRepositoryImpl.kt:36-37` | PRD §4 baris 105 (konfirmasi aksi destruktif) | Sudah ada dialog konfirmasi; PIN ditambahkan di atasnya | Lanjut |
| R-23 | #11 Pajak & pembulatan | ADA | `ui/settings/SettingsStoreScreen.kt:184` → `ui/settings/SettingsViewModel.kt:335` (`onSaveStoreSettings`); signature `ui/settings/SettingsActions.kt:9` | PRD §7.2 baris 260, 272; **tidak** menyebut PIN untuk pajak | Aksi ada, dasar PIN-nya tidak ada di PRD | **Update PRD** (perluas §7.7) |
| R-24 | #12 Hapus/ganti QRIS | ADA | `ui/settings/SettingsQrisScreen.kt:329` → `SettingsViewModel.kt:318` (`onRemoveQris`); aksi `SettingsActions.kt:7` | PRD §7.4 baris 299; §6.5 baris 217-225; tidak menyebut PIN | Idem R-23 | **Update PRD** |
| R-25 | #13 Catat pembayaran utang | ADA | `ui/customers/RecordPaymentSheet.kt:164` → `ui/customers/CustomersActions.kt:14,33` → `ui/customers/CustomersViewModel.kt:128`; use case `domain/usecase/RecordDebtPaymentUseCase.kt:38` | PRD §7.3 baris 289; §6.3 baris 203 | Idem R-23 | **Update PRD** |
| R-26 | A2 #14 Tutup shift | TIDAK ADA | lihat R-13 | `docs/prd-gaps.md:122` (PARTIAL) | Tidak bisa di-gate | **Tunda** (C-B4) |
| R-27 | A2 #15 Void transaksi | TIDAK ADA | grep `void`/aksi void → tidak ada; aksi destruktif terdekat hanyalah retur (R-19) | PRD: tidak ada void di MVP §3.1 | Tidak bisa di-gate | **Tunda** (C-B4) |
| R-28 | A2 #16 Diskon per transaksi | TIDAK ADA (input) | `ui/cashier/CashierViewModel.kt:432`, `:491` menulis `invoiceDiscount = 0L`; engine mendukungnya `DefaultPriceEngine.kt:91,105` dan `CheckoutUseCase.kt:87,106`; diisi hanya oleh seeder demo `data/seed/DefaultDemoDataSeeder.kt:197` | PRD §7.2 baris 259 defines `D_invoice` | Tidak ada UI untuk di-gate | **Update kode** (fase terpisah) |
| R-29 | A2 #17 Manajemen user/staff | TIDAK ADA | tidak ada entity/DAO/layar; `cashier_id` selalu `null` di jalur checkout (`domain/usecase/CheckoutUseCase.kt:102`) | PRD §4 baris 70 ("staff/PIN") saja | Role tidak punya identitas | **Eskalasi** (C-B5) |
| R-30 | A2 #18 Hapus pelanggan | TIDAK ADA (UI) | `core/contract/Repositories.kt:30` (`deleteCustomer`) ada; tidak ada pemanggil di `app/src/main` | PRD: tidak menyebut hapus pelanggan | Tidak ada yang bisa di-gate | **Tunda** |

### 1e. Backup

| ID | Requirement PRD | Status | Bukti kode | Bukti PRD | Dampak ke rencana PIN | Rekomendasi |
|---|---|---|---|---|---|---|
| R-31 | Daftar tabel backup & posisi `security_store` | BERBEDA | `data/backup/BackupTables.kt:7-20` — `insertOrder` = products(8), product_variants_units(9), customers(10), transactions(11), transaction_items(12), debt_payments(13), returns(14), return_lines(15), cashier_shifts(16), **security_store(17)**, app_settings(18), invoice_counters(19). `deleteOrder` = kebalikannya (`:22`) | PRD §7.6 baris 328 hanya menyebut format; §8 tidak menyebut tabel keamanan | **`security_store` memang ikut terekspor hari ini** — konfirmasi penuh atas rencana pengecualian | Update kode (Phase 1) |
| R-32 | `isSupported()` sebagai pengendali | BERBEDA | `BackupTables.kt:24`; satu-satunya pemakaian ada di test `app/src/test/.../BackupJsonCodecTest.kt` | — | Mengubah `isSupported` saja **tidak** akan menghentikan ekspor; yang berlaku adalah `insertOrder` | Catat di Phase 1 |
| R-33 | Validasi backup mensyaratkan semua tabel `insertOrder` | SESUAI | `data/backup/BackupJsonCodec.kt:29-31` (`containsAll(BackupTables.insertOrder)`), `FORMAT_VERSION = 1` (`BackupTables.kt:5`) | PRD §7.6 baris 333 (konfirmasi sukses/gagal) | Mengeluarkan `security_store` dari `insertOrder` tetap membuat backup lama (yang memuat key itu) valid | Aman — tidak perlu naikkan `formatVersion` |
| R-34 | Restore menimpa seluruh tabel | SESUAI | `data/backup/JsonDataBackupService.kt:93-103` (delete semua `deleteOrder`, lalu insert) | PRD §8 baris 371 | Karena `deleteOrder` turunan `insertOrder`, mengeluarkan `security_store` berarti SQL lama **tidak** lagi terhapus → reset PIN harus jadi langkah eksplisit | Update kode (Phase 1) |
| R-35 | Backup lokal tidak boleh di sandera PIN | AMBIGU | gate PIN belum ada | PRD §7.6 baris 332; §7.8 baris 353 | Menentukan apakah #8/#9 boleh di-gate | **Eskalasi** (C-B2) |

### 1f. Migrasi Room

| ID | Requirement PRD | Status | Bukti kode | Bukti PRD | Dampak ke rencana PIN | Rekomendasi |
|---|---|---|---|---|---|---|
| R-36 | Versi DB saat ini | SESUAI (rencana) | `UmkmDatabase.kt:34` (`@Database(`), `:49` (`version = 5`) | PRD §8 baris 371 | `MIGRATION_5_6` **masih relevan**; versi kode belum naik | Lanjut Phase 1 |
| R-37 | Migrasi eksplisit terdaftar | SESUAI | `UmkmDatabase.kt:78` (1→2), `:89` (2→3), `:95` (3→4), `:101` (4→5), didaftarkan `:112` (`addMigrations(...)`). Tidak ada `fallbackToDestructiveMigration` di file ini | PRD §8 baris 371 | Pola yang harus ditiru untuk 5→6 | Lanjut |
| R-38 | Skema terekam untuk test migrasi | SESUAI | `app/schemas/id.umkm.pos.pro.data.local.UmkmDatabase/` berisi `1.json`–`5.json` (tidak ada `6.json`); `app/build.gradle.kts:104-105` menaruh folder itu sebagai assets `androidTest` | PRD §12 baris 526 | Test migrasi 5→6 bisa ditulis dengan pola yang sudah ada | Lanjut |
| R-39 | Pola test migrasi yang sudah ada | SESUAI | `app/src/androidTest/java/id/umkm/pos/pro/data/local/UmkmDatabaseMigrationTest.kt:22-25` (`MigrationTestHelper`), test per versi di `:38, :64, :90, :115` | PRD §12 baris 526 | Tinggal menambah satu test 5→6 dengan pola identik | Lanjut |

### 1g. Kontrak keamanan yang diusulkan

| ID | Item rencana saya | Status di kode | Bukti kode | Dampak ke rencana PIN | Rekomendasi |
|---|---|---|---|---|---|
| R-40 | `GrantToken` | TIDAK ADA | grep `GrantToken` → 0 hasil di seluruh repo | Masih usulan murni; bebas dibentuk di Phase 0 | Lanjut Phase 0 |
| R-41 | `SecurityPolicy` / policy engine | TIDAK ADA | grep `SecurityPolicy` → 0 hasil; tidak ada direktori `domain/security/` (isi `domain/` = `qris/`, `pos/`, `usecase/`, `accounting/`) | Idem | Lanjut Phase 0 |
| R-42 | `CriticalAction` (registry) | TIDAK ADA | grep `CriticalAction` → 0 hasil | Idem; registry belum punya rumah | Lanjut Phase 0 |
| R-43 | Paket UI keamanan | TIDAK ADA | tidak ada `app/src/main/java/id/umkm/pos/pro/ui/security/` (glob mengembalikan 0 file); PRD §10 baris 443 menugaskan direktori itu ke Agent 5 | Sesuai rencana SA-3 | Lanjut |
| R-44 | Hashing PIN | TIDAK ADA | grep `PBKDF2\|Pbkdf2\|Kdf` → 0 hasil; `data/local/util/HashComparison.kt:5-6` hanya `MessageDigest.isEqual` | Sudah saya duga; hashing memang harus dibuat | Lanjut Phase 1 |
| R-45 | Interface `SecurityRepository` yang ada | SESUAI (terbatas) | `core/contract/Repositories.kt:54-59`: `isPinEnabled`, `setPin(pinHash, recoveryAnswerHash)`, `clearPin`, `verifyPin(pinHash)`, `verifyRecoveryAnswer(recoveryAnswerHash)`; impl `data/local/repository/SecurityRepositoryImpl.kt:17-57`; wiring `app/DefaultAppContainer.kt:90-91`; dipakai UI hanya sebagai Preview `ui/settings/SettingsScreen.kt:277` | Tidak menyebut role, lockout, sesi, audit, atau kode pemulihan | Perluasan kontrak lewat Phase 0 (milik orchestrator) |

### 1h. Kendala ukuran, SDK, dan dependency

| ID | Item | Status | Bukti | Dampak ke rencana PIN | Rekomendasi |
|---|---|---|---|---|---|
| R-46 | `minSdk` | SESUAI | `app/build.gradle.kts:25` (`minSdk = 23`) | `PBKDF2WithHmacSHA256` tersedia mulai API 26 → perangkat API 23–25 butuh fallback; **tidak terverifikasi di kode** mana pun karena hashing belum ada | Kunci D5: PBKDF2 + fallback |
| R-47 | `targetSdk` / `compileSdk` | SESUAI | `app/build.gradle.kts:21` (36), `:26` (36) | Tidak ada | — |
| R-48 | Anggaran ukuran rilis < 8 MB | SESUAI (PRD) / klaim ukuran **TIDAK TERVERIFIKASI** | PRD §9.2 baris 392; `docs/prd-gaps.md` menyebut APK 3,1 MB — saya **tidak** membangun apa pun untuk memverifikasi | Menambah dependency biometrik/Argon2 menggerus anggaran | Pertahankan tanpa dependency baru |
| R-49 | Dependency kripto/biometrik | TIDAK ADA | `app/build.gradle.kts` blok `dependencies`, tidak ada `androidx.biometric`, tidak ada `security-crypto`, tidak ada Argon2/libsodium | D5 aman dengan jalur tanpa dependency | Kunci D5 |
| R-50 | Tidak ada permission `INTERNET` | SESUAI | `app/src/main/AndroidManifest.xml:4-11` — hanya BLUETOOTH/BLUETOOTH_ADMIN/BLUETOOTH_CONNECT/BLUETOOTH_SCAN/ACCESS_NETWORK_STATE/CAMERA/READ_CONTACTS/WRITE_EXTERNAL_STORAGE. Tidak ada `INTERNET` | Relevan untuk D6: biometrik maupun kode pemulihan harus tetap offline | — |

### 1i. Baseline test

| ID | Item | Status | Bukti | Dampak | Rekomendasi |
|---|---|---|---|---|---|
| R-51 | Klaim "469 tests, 62 classes" | BERBEDA | `docs/prd-gaps.md:166`; hasil hitung saya: **505 anotasi `@Test` di 66 file** `app/src/test/**`, dan **45 anotasi `@Test` di 10 file** `app/src/androidTest/**` | Angka baseline di acceptance criteria saya harus diperbarui; jumlah test yang benar-benar *lulus* **TIDAK TERVERIFIKASI** (saya tidak menjalankan Gradle) | Update dokumen; jalankan suite saat Phase 0 |
| R-52 | Test migrasi & backup yang relevan sudah ada | SESUAI | `app/src/androidTest/.../UmkmDatabaseMigrationTest.kt`, `app/src/androidTest/.../JsonDataBackupServiceTest.kt` (4 `@Test`), `app/src/test/.../BackupJsonCodecTest.kt` (8 `@Test`) | Titik sambung test Phase 1 sudah tersedia | Lanjut |
| R-53 | PRD §12 blok perintah uji | BERBEDA (typo PRD) | — | PRD baris 476 menulis `adb shell monkey -p id.umkm.pos. pro ...` — ada spasi di dalam package name, perintah itu akan gagal | Update PRD (perbaikan kecil) |
| R-54 | PRD §12 mewajibkan uji pemulihan PIN + migrasi | SESUAI (mandat) | — | PRD baris 525 (uji PIN recovery & backup tetap bisa diakses), baris 526 (migration test terhadap data pengguna) | Test plan saya sudah selaras |

---

## Bagian 2 — Daftar Konflik

### BLOCKING (perlu keputusan Anda sebelum Phase 0)

**C-B1 — Lokasi penyimpanan PIN berbeda antara PRD dan kode**
- Deskripsi: PRD §8 menempatkan `pin_hash` dan `recovery_answer_hash` di `cashier_shifts`; kode menyimpan PIN di `security_store` dan `cashier_shifts` tidak punya kolom itu.
- Bukti: PRD baris 369 vs `data/local/entity/CashierShiftEntity.kt:15-22` dan `data/local/entity/SecurityStoreEntity.kt:10-12`.
- Opsi A — **Update PRD**: PIN tinggal di `security_store`; hapus `pin_hash`/`recovery_answer_hash` dari baris `cashier_shifts`. Dampak: tidak ada perubahan kode tambahan, satu sumber kebenaran PIN, `cashier_shifts` tetap murni data shift.
- Opsi B — **Update kode**: tambahkan kolom ke `cashier_shifts` dan pindahkan PIN ke sana. Dampak: dua tempat menyimpan data keamanan, migrasi lebih besar dan berisiko, dan `security_store` menjadi mubazir; PIN per shift juga bertabrakan dengan model "satu toko satu POS".

**C-B2 — PIN pada export/restore backup vs larangan "hostage"**
- Deskripsi: rencana memberi PIN pada export backup JSON (#8) dan restore (#9). PRD §7.6 melarang "menahan file backup di balik PIN kasir".
- Bukti: PRD baris 332 dan 353; call site `ui/settings/SettingsBackupScreen.kt:112,116,71`.
- Opsi A — **Update PRD**: perjelas bahwa larangan itu tentang enkripsi/penyembunyian file, bukan tentang meminta PIN sebelum aksi export. Dampak: gate PIN pada export & restore sah; data keuangan tetap terlindungi.
- Opsi B — **Kode**: hanya restore yang di-gate; export bebas PIN. Dampak: siapa pun yang memegang tablet bisa mengekspor seluruh database (nama, telepon, utang pelanggan) ke file dan mengirimnya keluar.

**C-B3 — Biometrik wajib di PRD, tidak ada di kode, dan ditolak lingkup**
- Deskripsi: PRD mewajibkan biometrik perangkat; keputusan Anda "kode pemulihan saja"; tidak ada kode maupun dependency biometrik.
- Bukti: PRD baris 51, 231, 342, 443; grep `BiometricPrompt|androidx.biometric` tanpa hasil; `app/build.gradle.kts` tanpa dependency terkait.
- Opsi A — **Update PRD**: nyatakan biometrik ditunda dan kode pemulihan offline adalah jalur pemulihan resmi. Dampak: Phase 1–3 bisa berjalan offline, tanpa dependency, anggaran 8 MB aman; PRD tidak lagi menjanjikan fitur yang tidak ada.
- Opsi B — **Update kode**: tambah `androidx.biometric` + prompt. Dampak: dependency baru, kerja UI/keamanan tambahan, dan perangkat tanpa biometrik tetap butuh jalur kode pemulihan — jadi bukan pengganti, hanya tambahan.

**C-B4 — Aksi tanpa call site: tutup shift dan void**
- Deskripsi: dua aksi yang Anda minta sebagai critical tidak ada di UI, sehingga tidak ada yang bisa di-gate di Phase 3.
- Bukti: R-13 (shift), R-27 (void).
- Opsi A — **Keluarkan dari cakupan PIN sekarang**, catat sebagai deferred di PRD. Dampak: Phase 3 hanya menyentuh 13 aksi yang benar-benar ada; tidak ada gate "hantu".
- Opsi B — **Update kode lebih dulu**: buat UI shift (dan void) sebagai fase terpisah sebelum PIN-nya. Dampak: scope bertambah signifikan (layar, use case, test, rekonsiliasi kas) dan menunda fitur PIN.

**C-B5 — Role kasir/manajer/owner tidak punya dasar di PRD maupun skema**
- Deskripsi: seluruh kolom "Role" dan "Level PIN" di Tabel A mengandaikan sistem identitas. PRD hanya menyebut "staff/PIN" sepintas; kode hanya punya satu nama kasir bebas teks dan `cashier_id` yang selalu `null`.
- Bukti: PRD baris 70; `core/model/Settings.kt:3` (`cashierName: String?`); `domain/usecase/CheckoutUseCase.kt:102` (`cashier_id` null); tidak ada entity user (R-08).
- Opsi A — **Update PRD + desain single-PIN owner** untuk MVP. Dampak: PIN tunggal; "level" direduksi menjadi satu tingkat; tidak perlu tabel user; scope paling kecil dan sesuai kenyataan "satu toko satu POS".
- Opsi B — **Update PRD + kode**: bangun `security_users` dengan role. Dampak: tabel, layar manajemen user, migrasi, dan matriks hak akses; scope jauh lebih besar dan menyentuh hampir semua layar.

### NON-BLOCKING (dokumentasi atau lanjut dengan catatan)

| ID | Konflik | Bukti | Dampak | Rekomendasi |
|---|---|---|---|---|
| C-N1 | `security_store` tidak ada di PRD §8 | PRD baris 361–369 vs `SecurityStoreEntity.kt:7` | PRD tidak lengkap, bukan penghalang | Update PRD |
| C-N2 | PRD §8 hanya memuat 7 entitas; kode punya 12 | PRD baris 363–369 vs 12 file di `data/local/entity/` dan `UmkmDatabase.kt:35-48` | `returns`, `return_lines`, `app_settings`, `security_store`, `invoice_counters` absen dari PRD | Update PRD |
| C-N3 | Audit log tidak disebut PRD sama sekali | grep `audit_log` → 0 hasil; PRD tanpa kata "audit log" | Kolom Audit di Tabel A tanpa dasar dokumen | Update PRD |
| C-N4 | `recovery_answer_hash` ada di kode tetapi bertentangan dengan "tanpa pertanyaan keamanan" | `SecurityStoreEntity.kt:11` | Dapat dibiarkan tak terpakai; tidak menghalangi Phase 1–3 | Biarkan + dokumentasikan; hapus di fase pembersihan |
| C-N5 | `is_pin_enabled` tersimpan dua kali | `SecurityStoreEntity.kt:12` dan `SettingsEntity.kt:14`; ditulis `SecurityRepositoryImpl.kt:31,44` | Setelah restore, `app_settings` bisa membawa `1` tanpa hash → PIN "aktif" tapi tak bisa diverifikasi | Paksa nonaktif setelah restore (Phase 1) |
| C-N6 | Baseline test di dokumen tidak cocok dengan kode | `docs/prd-gaps.md:166` vs 505/66 (`app/src/test`) dan 45/10 (`app/src/androidTest`) | Angka acceptance criteria harus diperbarui | Update dokumen; jalankan suite |
| C-N7 | Export laporan sudah punya gate iklan, akan ditambah PIN | `ReportsPresenter.kt:204,210`; label `IdStrings.kt:461` ("Tonton & Ekspor") | Potensi dua gate berurutan pada satu aksi | Keputusan Q7 |
| C-N8 | Aksi tambahan D12 tidak ada di PRD | R-23, R-24, R-25 | PIN pada pajak/pembulatan, QRIS, pembayaran utang tidak punya dasar dokumen | Update PRD (perluas §7.7) |
| C-N9 | Kolom baru PIN belum ada, jadi migrasi wajib | `SecurityStoreEntity.kt:10-12` hanya 3 kolom (tanpa salt/lockout/sesi/kode pemulihan) | Koreksi rencana saya: `MIGRATION_5_6` tidak opsional | Update rencana Phase 1 |

---

## Bagian 3 — Dampak ke Rencana Phase 0–3 (D1–D13 setelah validasi)

| ID | Keputusan sebelumnya | Status setelah validasi | Perubahan/perbaikan yang diusulkan | Bukti |
|---|---|---|---|---|
| D1 | PIN tunggal owner vs multi-user | **Tetap valid, naik jadi BLOCKING** | Belum ada tempat menyimpan identitas kedua; keputusan ini menentukan apakah Phase 1 perlu tabel `security_users` | `CashierShiftEntity.kt:15-22`, tidak ada entity user, `CheckoutUseCase.kt:102` |
| D2 | Manajemen user masuk MVP? | **Tetap valid, terikat D1** | Sama seperti D1; jangan mulai Phase 1 sebelum dijawab | R-08, R-29 |
| D3 | Ambang diskon & nego | **Tetap valid** | Call site terbukti ada, jadi threshold bisa diterapkan apa adanya; saya masih butuh angkanya | `CashierCart.kt:425,434` |
| D4 | Format hash: string terenkode vs kolom salt | **Tetap valid, diperkuat** | Rekomendasi string terenkode tetap berlaku; tambahan: `security_store` tidak punya kolom salt, jadi string terenkode memang pilihan termurah | `SecurityStoreEntity.kt:10-12` |
| D5 | PBKDF2 + fallback vs minSdk 26 vs dependency | **Tetap valid, diperkuat** | `minSdk 23` terkonfirmasi; tidak ada dependency kripto; anggaran 8 MB menolak Argon2. Konsekuensi: fallback `PBKDF2WithHmacSHA1` untuk API 23–25, algoritma+iterasi disimpan di dalam string hash | `app/build.gradle.kts:25`, `:21,26`; PRD baris 392 |
| D6 | Biometrik MVP atau nanti | **BERUBAH** | Bukan sekadar "nanti": PRD **mewajibkannya** di tiga tempat, sehingga wajib ada revisi PRD eksplisit, bukan sekadar penundaan diam-diam | PRD baris 51, 231, 342; R-11 |
| D7 | Sesi unlock: durasi & aksi | **Tetap valid, bertambah biaya** | Karena kolom sesi belum ada, opsi sesi menambah kolom (`session_role`, `session_unlocked_until`) ke migrasi yang sama; kalau Anda ingin migrasi minimal, sesi bisa ditunda ke Phase 4 | `SecurityStoreEntity.kt:10-12` |
| D8 | Kolom `recovery_answer_hash`: biarkan vs hapus | **BERUBAH — premis saya salah** | Saya menyatakan "ada tapi tak terpakai"; faktanya kolom itu **ada di `security_store`**, sementara PRD menaruhnya di `cashier_shifts` yang tidak punya kolom itu. Usulan baru: pertahankan kolom di `security_store` sebagai unused, koreksi baris `cashier_shifts` di PRD | `SecurityStoreEntity.kt:11` vs PRD baris 369 |
| D9 | UI shift sekarang atau nanti | **Tetap valid, jadi BLOCKING** | Tidak ada satu pun composable yang memanggil shift; aksi "tutup shift" harus dikeluarkan dari cakupan PIN atau UI shift dibuat lebih dulu | R-13 |
| D10 | Void & diskon per transaksi sekarang atau nanti | **Tetap valid, diperkuat** | Void tidak ada; `invoiceDiscount` ditulis `0L` di jalur checkout walau engine mendukungnya | `CashierViewModel.kt:432,491`; R-28 |
| D11 | Audit log: retensi & tampilan | **Tetap valid, plus kewajiban PRD** | Tabel audit tidak ada di kode **dan** tidak ada di PRD; usulan tabel + tampilan di Pengaturan Keamanan menuntut penambahan ke PRD | R-09, C-N3 |
| D12 | Aksi tambahan jadi critical | **Tetap valid, plus kewajiban PRD** | Call site terbukti ada untuk pajak/pembulatan, QRIS, pembayaran utang, hapus produk; tetapi tidak satu pun punya dasar PIN di PRD. Hapus pelanggan tetap tanpa UI | R-22…R-25, R-30 |
| D13 | Konflik PRD perlu update? | **Terbukti ya, dan bertambah** | Tambahan temuan: entitas PRD hanya 7 dari 12, `security_store` absen, audit log absen, typo perintah di PRD baris 476, baseline test di `docs/prd-gaps.md:166` sudah usang | C-N1…C-N3, C-N6, R-53 |

---

## Bagian 4 — Verifikasi Baseline

| Item | Nilai terverifikasi | Bukti |
|---|---|---|
| Versi Room | **5** | `data/local/UmkmDatabase.kt:34` (`@Database(`), `:49` (`version = 5`) |
| Migrasi terdaftar | 1→2, 2→3, 3→4, 4→5 | `UmkmDatabase.kt:78, :89, :95, :101`, didaftarkan `:112` |
| `MIGRATION_5_6` | **Belum ada; masih relevan untuk dibuat** | tidak ada di `UmkmDatabase.kt`; skema terbaru `app/schemas/.../5.json` |
| Migrasi destruktif | Tidak ada | tidak ditemukan `fallbackToDestructiveMigration` di `UmkmDatabase.kt` |
| Entity terdaftar | **12** | `UmkmDatabase.kt:35-48`; 12 file di `data/local/entity/` |
| Entity `security_store` | **ADA** | `data/local/entity/SecurityStoreEntity.kt:7`; kolom `pin_hash` (`:10`, String?, nullable), `recovery_answer_hash` (`:11`, String?, nullable), `is_pin_enabled` (`:12`, Boolean, non-null); PK `id` (`:9`, default 1) |
| Entity `cashier_shifts` | **ADA**, tanpa kolom PIN | `data/local/entity/CashierShiftEntity.kt:8-23`; kolom: `id` (`:16`), `cashier_name` (`:17`, String), `opened_at` (`:18`, Long), `closed_at` (`:19`, Long?), `starting_cash` (`:20`, Long), `expected_ending_cash` (`:21`, Long?), `actual_ending_cash` (`:22`, Long?) |
| `BackupTables.insertOrder` (12 tabel, urut) | products, product_variants_units, customers, transactions, transaction_items, debt_payments, returns, return_lines, cashier_shifts, **security_store**, app_settings, invoice_counters | `data/backup/BackupTables.kt:7-20`; `security_store` di baris **:17** |
| `BackupTables.deleteOrder` | kebalikan dari `insertOrder` | `BackupTables.kt:22` |
| `FORMAT_VERSION` backup | 1 | `BackupTables.kt:5` |
| minSdk / targetSdk / compileSdk | **23 / 36 / 36** | `app/build.gradle.kts:25`, `:26`, `:21` |
| Permission `INTERNET` | Tidak ada | `app/src/main/AndroidManifest.xml:4-11` |
| Test unit | **505 anotasi `@Test` di 66 file** | hitung pada `app/src/test/**/*.kt` |
| Test instrumented | **45 anotasi `@Test` di 10 file** | hitung pada `app/src/androidTest/**/*.kt` |
| Klaim "469 tests, 62 classes" | **Tidak cocok** dengan hitungan di atas; jumlah test yang benar-benar lulus **TIDAK TERVERIFIKASI** (Gradle tidak dijalankan) | `docs/prd-gaps.md:166` |
| Ukuran APK rilis | **TIDAK TERVERIFIKASI** — saya tidak membangun apa pun | klaim 3,1 MB hanya ada di dokumen |
| Kontrak keamanan (`GrantToken`, `SecurityPolicy`, `CriticalAction`, `domain/security/`, `ui/security/`, hashing) | **Tidak ada satu pun di kode** — masih murni usulan | grep tanpa hasil; glob `ui/security` & `domain/security` kosong |
| Biometrik | **Tidak ada** di kode maupun dependency | grep tanpa hasil; `app/build.gradle.kts` tanpa dependency biometrik |

---

## Bagian 5 — Pertanyaan Terbuka Baru

| Q | Pertanyaan | Kenapa tidak bisa saya putuskan |
|---|---|---|
| Q1 | PIN disimpan di `security_store` (kode) atau dipindah ke `cashier_shifts` (PRD §8 baris 369)? | Menyangkut integritas data keamanan; butuh keputusan pemilik produk (C-B1) |
| Q2 | Apakah "jangan menahan file backup di balik PIN" (PRD baris 332) berarti export backup JSON **tidak boleh** meminta PIN? | PRD bisa dibaca dua arah (C-B2) |
| Q3 | Biometrik: dihapus dari PRD, atau tetap dijadwalkan sebagai fase terpisah setelah PIN inti? | Keputusan produk; PRD mewajibkannya tetapi Anda memilih kode pemulihan saja (C-B3) |
| Q4 | Berapa role final, dan apakah ada identitas user (nama + PIN per orang) atau cukup PIN tunggal? | Tidak ada dasar di PRD maupun skema (C-B5) |
| Q5 | Tutup shift: tunggu sampai UI shift dibuat, atau UI shift dimasukkan sebagai fase terpisah dalam inisiatif ini? | Keputusan lingkup (C-B4) |
| Q6 | Setujui tabel audit log baru + penambahannya ke PRD, termasuk retensinya? | PRD tidak menyebut audit log sama sekali (C-N3) |
| Q7 | Export laporan: PIN dulu lalu iklan, atau PIN menggantikan iklan? | Sudah ada gate iklan di `ReportsPresenter.kt:210`; PRD baris 352 menyebut iklan itu opsional (C-N7) |
| Q8 | Saat PIN diganti: wajib memverifikasi PIN lama, atau cukup kode pemulihan? | Perilaku produk yang tidak ditentukan PRD |
| Q9 | Restore backup yang memuat key `security_store`: diabaikan senyap, atau diberi tahu ke pemilik? | Tidak ditentukan; menyangkut ekspektasi pengguna (R-33) |
| Q10 | Di form produk, apakah field HPP **disembunyikan** untuk role tanpa hak, atau form tetap tampil dan hanya penyimpanan yang di-gate? | Menyangkut privasi data vs kesederhanaan UI; HPP berada satu form dengan harga & stok (R-16) |

---

## Catatan Keterbatasan

- Saya **tidak** menjalankan Gradle, jadi tidak ada satu pun klaim "test lulus" dalam laporan ini. Yang saya laporkan hanyalah hasil hitung anotasi `@Test` dan pembacaan kode.
- Saya **tidak** membangun APK, jadi ukuran rilis tidak diverifikasi.
- Saya **tidak** memeriksa perilaku runtime atau tampilan; tidak ada screenshot yang diambil.
- Dua baris berikut saya catat apa adanya tanpa menelusuri konteks penuhnya: `ui/cashier/CashierRoute.kt:179` dan `feature/transactions/TransactionsScreen.kt:299` (keduanya menulis `invoiceDiscount = 0L`, kemungkinan blok `@Preview`). Jalur produksi yang saya verifikasi adalah `ui/cashier/CashierViewModel.kt:432` dan `:491`.

---

**Phase 0 belum dimulai. Menunggu persetujuan pemilik produk atas laporan ini dan jawaban atas pertanyaan terbuka.**

---

## Change record — 2026-10-20 (revision pass approved by the owner)

Recorded per the "Documentation of changes — mandatory" rule added to `AGENTS.md` in the same pass. No application code was changed; no decision in §15 was taken.

| Date | File | Change | PRD reference | Verified |
|---|---|---|---|---|
| 2026-10-20 | `AGENTS.md` | Added the "Documentation of changes — mandatory" section; added workflow step 9; added a Definition-of-done bullet. All three require a date (`YYYY-MM-DD`) plus the affected PRD sections for every change. | §16 (rule itself); no PRD statement changed | File re-read after edit; section present at lines 11–21, step 9 at line 95, DoD bullet at line 107 |
| 2026-10-20 | `.agents/AGENTS.MD` | Added handoff item 7 requiring the date and the PRD sections touched, so a handoff without them is incomplete. | §16; no PRD statement changed | File re-read after edit; item present at line 172 |
| 2026-10-20 | `docs/prd.md` | Version raised to 1.3. Corrected the `cashier_shifts` row in §8 and added footnote †; listed the five entities missing from §8 and recorded that `security_store` is exported today; marked biometric recovery as an open decision in §3.1, §6.6, §7.7; added the "Proposed 2026-10-20" PIN scope paragraph in §7.7; noted the open decision in §7.6; fixed the `adb shell monkey` command in §12; corrected the PIN test note in §12; added §15 Open Decisions and §16 Revision History. | §3.1, §6.6, §7.6, §7.7, §8, §12, §15, §16 | Each edit re-read after writing; §15 at lines 563–579, §16 at lines 581–589 |
| 2026-10-20 | `docs/prd-gaps.md` | Added "Test count correction (2026-10-20)" so the stale "469 tests, 62 classes" figure is not read as current. | §12 (verification) | File re-read after edit; note present at lines 173–179 |

**Not changed in the documentation-revision pass:** any file under `app/`, the decision list in §15, and the implementation status of PIN in `docs/prd-gaps.md` (still MISSING at that time).

---

## Change record — 2026-10-20 (Phase 0–1 implementation)

Recorded per the "Documentation of changes — mandatory" rule in `AGENTS.md`. The owner delegated the open decisions on 2026-10-20 ("do your best; I will say if I disagree"); the decisions taken are in `docs/prd.md` §15.1 and remain revisable.

**Why:** nothing in the app wrote or verified a PIN, PIN material was exported in the JSON backup, and there was no way to describe what a critical action needed. Phase 0 froze the shared contracts; Phase 1 built the storage, hashing, policy, audit, and backup behaviour on top of them.

**New files**

| File | Purpose |
| --- | --- |
| `core/model/Security.kt` | `SecurityRole`, the `CriticalAction` registry, `SecurityState`, `SecurityCredentials`, `SecurityAuditResult`, `SecurityAuditEntry` |
| `core/contract/Security.kt` | `PinHasher` interface |
| `data/local/entity/SecurityAuditEntity.kt` | `security_audit_log` entity |
| `data/local/dao/SecurityAuditDao.kt` | insert, newest-first read, retention trim |
| `data/local/security/Pbkdf2PinHasher.kt` | salted PBKDF2, encoded `pbkdf2$<alg>$<iterations>$<saltHex>$<hashHex>` |
| `data/local/mapper/SecurityMapper.kt` | entity ↔ model mapping for state and audit |
| `domain/security/PinPolicy.kt` | required role per action, thresholds, lockout and session maths |
| `domain/security/RecoveryCode.kt` | Crockford-style recovery code generator and normaliser |
| `domain/security/SecurityService.kt` | unlock, enable, change, disable, rotate recovery code, audit |
| `app/src/test/.../Pbkdf2PinHasherTest.kt`, `PinPolicyTest.kt`, `RecoveryCodeTest.kt`, `SecurityServiceTest.kt`, `data/local/mapper/SecurityMapperTest.kt` | 59 new unit tests covering hashing, thresholds, lockout escalation, recovery-code handling, and audit writes |

**Changed files:** `core/contract/Repositories.kt` (the `SecurityRepository` contract replaced), `core/contract/AppContainer.kt` (+`pinHasher`, +`securityService`), `core/contract/DataBackupService.kt` (import summary now reports the security reset), `data/local/entity/SecurityStoreEntity.kt` (+`recovery_code_hash`, `failed_attempts`, `lockout_until`, `session_role`, `session_unlocked_until`, `pin_updated_at`), `data/local/dao/SecurityDao.kt` (+`observeSecurityStore`), `data/local/repository/SecurityRepositoryImpl.kt` (rewritten), `data/local/UmkmDatabase.kt` (version 6, new entity and DAO, `MIGRATION_5_6`, explicit column list in the singleton insert), `data/backup/BackupTables.kt` (`security_store` removed from `insertOrder`; `excludedTables` added), `data/backup/JsonDataBackupService.kt` (post-restore PIN reset plus audit entry), `app/DefaultAppContainer.kt`, `ui/common/PreviewAppContainer.kt`, `androidTest/.../AppRootFakeContainer.kt`, `androidTest/.../UmkmDatabaseMigrationTest.kt` (5→6 test), `app/src/test/.../ReturnRepositoryImplTest.kt`, `app/src/test/.../BackupJsonCodecTest.kt`.

**Deleted:** `data/local/util/HashComparison.kt` — its only caller was the old `SecurityRepositoryImpl`, and a constant-time string compare is not PIN verification.

**Verification (actual results)**

- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest assembleDebug --console=plain` → **BUILD SUCCESSFUL**, 564 unit tests, 0 failures.
- `./gradlew compileDebugAndroidTestKotlin` → compiles; the instrumented tests were **not executed** (no device attached).
- A first `./gradlew` run failed because the default JVM is Java 8; `docs/dev-environment.md` already documents the JDK 17 requirement.
- `app/schemas/...6.json` was generated and the `security_audit_log` `createSql` matches the `MIGRATION_5_6` statement exactly (column order, types, `AUTOINCREMENT` primary key).

**Known gaps**

1. No UI, no enforcement: `requiresPin` is never called from a screen, so no action is gated yet (Phase 2–3).
2. The instrumented migration test for 5→6 has not run on a device. `failed_attempts` is added as `INTEGER NOT NULL DEFAULT 0` while the entity declares no default — the same shape as `paper_width_mm` in `MIGRATION_4_5`, which passes today, but this is proven only by that on-device test.
3. Audit atomicity: `SecurityService.recordDecision` writes its own entry after the gated action changes data. Making the audit entry part of the same transaction as the action's write is Phase 3 work.
4. Session elevation is honoured by `PinPolicy.alwaysPrompts` only through the required role; the UI must still avoid stacking dialogs (PRD §4).

---

## Change record — 2026-10-20 (Phase 2–3: PIN UI and enforcement)

**Why:** Phase 1 left `requiresPin` unused, so no action was protected. Phase 2–3 added the screens the owner needs and a single gate in front of every critical action.

**Orchestration.** The owner delegated control on 2026-10-20 and authorized three sub-agents. Before delegating, the orchestrator froze the shared contract so the agents could not diverge: the string keys for the gate and the settings row (`ui/common/PosStrings.kt` + the three catalogues), and `ui/security/SecurityGate.kt` (`SecurityGate` interface, `SecurityGateController`, `GateFeedback`, `LocalSecurityGate`, `rememberSecurityGate()`) plus `ui/security/SecurityGateHost.kt` (the one dialog), mounted once in `AppRoot` together with `rememberSecurityGateController`. Ownership was disjoint: SA-1 owned `ui/security/**` screens and appended its own string block; SA-2 owned `ui/cashier/**`, `feature/transactions/**`, `ui/returns/**`; SA-3 owned `ui/settings/**`, `ui/products/**`, `ui/customers/**`, `feature/reports/**`. No file had two owners, and `core/**`, `domain/**`, `data/**`, `AppRoot.kt` and `docs/**` stayed with the orchestrator.

| Agent | Delivered | Files (new unless noted) |
| --- | --- | --- |
| SA-1 Security UI | SecurityRoute/Screen/ViewModel/UiState/Actions, recovery-code print launcher, 63 localized string members, 2 test classes | `ui/security/{SecurityRoute,SecurityScreen,SecurityViewModel,SecurityUiState,SecurityActions,RecoveryCodePrinting}.kt`; appends to the four string files |
| SA-2 Cashier & transactions | Draft-pricing snapshot for the line editor, one owner-level prompt when discount **and** below-cost both apply, refund gate, return-submit gate, `LineEditorPinGate` helper + tests | `ui/cashier/{LineEditorPinGate,CashierUiState,CashierViewModel,CashierCart}.kt`; `feature/transactions/TransactionActionsMenu.kt`; `ui/returns/ReturnRoute.kt` |
| SA-3 Settings, products, customers, reports | Security row in the settings hub, backup export/restore gates + post-restore notice, tax/rounding and QRIS gates, HPP masking and HPP/price gates, delete-product gate, debt-payment gate, profit/HPP masking + reveal, export gate before the existing ad flow | `ui/settings/{SettingsScreen,SettingsBackupScreen,BackupUiState,SettingsViewModel,SettingsStoreScreen,StoreSettingsGate,SettingsQrisScreen}.kt`; `ui/products/{ProductFormPanel,ProductsUiState,ProductsViewModel,ProductsRoute,ProductPricingGate}.kt`; `ui/customers/RecordPaymentSheet.kt`; `feature/reports/ReportsScreen.kt` |

**Integration fixes made by the orchestrator (each verified after the fix):**

1. `SecurityViewModel` referenced a non-existent string `securitySessionLockedMessage`; the key is `securitySessionLocked`. Compile blocker, one-line fix.
2. The audit list was stale when the screen was reopened: the ViewModel outlives the destination and only loaded once. Added `LaunchedEffect(viewModel) { onRefreshAudit() }` in `SecurityRoute`; on device the list went from "1 aktivitas" to all 6 rows.
3. A recovery-code unlock left an OWNER session behind while the PIN was off, so the screen claimed "Sesi sedang terbuka" next to "Aktifkan PIN". `unlockWithRecoveryCode` no longer opens a session (`UnlockOutcome.Granted.sessionUntilMillis` is now nullable), and the screen ignores sessions while the PIN is disabled.
4. Audit rows attributed every unlock attempt to `change_pin`. `unlockWithPin`/`unlockWithRecoveryCode` now accept the attempted `CriticalAction` and the gate passes it, so the log reads `export_backup / denied` or `view_profit_and_cost / granted`. Duplicate grant logging in the gate was removed.
5. The migration test asserted three products in an empty v5 fixture; it now inserts a product and asserts it survives, which is what the test is for.

**Verification (actual results, 2026-10-20):**

- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL, **627 unit tests, 0 failures**.
- `compileDebugAndroidTestKotlin` compiles; **48 instrumented tests pass on a physical tablet** (`TB321FU`), including `migration5To6…` and two new backup tests proving the payload contains no `security_store`/`pin_hash`/`recovery_code_hash` and that a restore turns the PIN off, records `restore_reset_pin`, and ignores a legacy payload carrying `security_store`.
- On device (tablet): PIN enable with the §6.6 explanation, one-time recovery code `HRKDH-…` with the mandatory acknowledgement, wrong PIN → "PIN salah. Sisa 4 percobaan.", correct PIN → values revealed, profit/HPP masked while locked, the owner-level backup export prompting inside an open session, the recovery code disabling the PIN and completing the export, and the audit history showing `Diizinkan / Ekspor backup`, `Kode pemulihan dipakai`, `Diizinkan / Lihat laba & HPP`.
- Backed-up JSON pulled from the device: 11 tables, **no** `security_store`, **no** `pin_hash`, **no** `security_audit_log`.
- Layout: tablet expanded (rail only, no bottom navigation), 135% font on the security screen without clipping, phone compact (bottom navigation only, single column). Font scale and night mode restored to their original values.

**Known gaps from this pass:** the Android print dialog was not exercised (a notification stole focus twice); the UI restore path through the file picker was not exercised (the service path is covered by the instrumented test); the five-attempt lockout was covered by unit tests but not driven on device; dark mode was not visually verified because switching night mode restarted the launcher; biometrics, per-user PINs, shift and void remain deferred by §15.1.

**Environment note:** debug builds were installed with `install -r --user 0` on the owner's tablet and phone (data preserved, nothing uninstalled). An unrelated app (HoYoLAB) opened on the tablet while tapping, caused by a notification; it was left with back/home and no text was submitted.
