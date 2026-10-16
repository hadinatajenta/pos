# PRD implementation gaps

Audit of `docs/prd.md` against the code at commit `cc74255`, 17 October 2026.
Every entry was verified by reading the code, not inferred.

Legend: **MISSING** = nothing in the code · **PARTIAL** = data/domain layer exists but the UI or the
wiring does not · **DONE** = usable by an end user today.

## 1. Store settings — PARTIAL

PRD §6.6, §7.1, §7.2. `core/model/Settings.kt` already models `storeName`, `cashierName`,
`taxRatePercent`, `roundingIncrement`, `isPinEnabled`, `printerAddress`, `qrisImagePath`.
`SettingsEntity`, `SettingsMapper`, `SettingsRepository` all exist.

What is missing: nothing in the UI ever writes settings. `CashierViewModel` and `DashboardViewModel`
only call `observeSettings()`. Consequences: tax and rounding are effectively fixed, the store name
cannot be changed, and there is no way to set the printer address or the QRIS image.

Work: build the **Pengaturan** screen (store identity, tax rate, rounding increment, QRIS image
picker, printer address, PIN toggle), enable the `MoreRoute` entry, and surface tax/rounding in the
cashier summary.

## 2. Thermal printer setup — PARTIAL

PRD §6.5, §7.5. Implemented in `hardware/printer/`: `BluetoothReceiptPrinter`, `UsbOtgReceiptPrinter`,
`EscPosReceiptPrinter`, `PrintMode`; receipt rendering in `hardware/receipt/`:
`AndroidReceiptRenderer`, `MonochromeRasterizer`, `ReceiptLayout`, `EscPosCommandBuilder`,
`ReceiptPaperWidth`. The cashier success state already has a **Cetak struk** action wired through
`CashierViewModel.receiptPrinter`.

What is missing: the setup screen behind the `MoreRoute` entry "Printer Struk"
(`isAvailable = false`): connection status, paired-printer list, selected printer, test print,
reconnect action, and paper-width selection. `AppSettings.printerAddress` is never written, so
printing is unreachable for a real user.

Work: printer setup screen, persistence through settings, test print, auto-reconnect, and detach
error handling.

## 3. Barcode scanner — PARTIAL

PRD §6.5, §7.5. Implemented: `hardware/scanner/HardwareScannerInput` and
`KeyboardWedgeScanDecoder` (hardware wedge input), plus cashier search by name, SKU, or barcode.

What is missing: camera scanning with ML Kit (no dependency in `libs.versions.toml`, no `CAMERA`
permission), the scanner setup screen behind "Scanner Barcode", and scanner mode where a hardware
scanner adds items without the cashier tapping the search field.

## 4. Backup and restore — MISSING

PRD §6.5, §7.6. There is no `data/backup` module, no SQLite `.db` or JSON export/import, and no
Google Drive `appDataFolder` integration (no Drive API or OAuth dependency). CSV and XLSX export
exists, but only for reports.

Work: local backup and restore (`.db` and `.json`) with validation and clear success/failure states,
always available offline, then the Google Drive path. Google Drive also requires OAuth client setup
in Google Cloud and a reviewed consent screen.

## 5. PIN, biometrics, and anti-lockout — PARTIAL

PRD §6.6, §7.7. `security_store` table and the `isPinEnabled` flag exist.

What is missing: PIN setup and verification logic, hashing, biometric prompt, the offline recovery
code and its instructions, and enforcement on sensitive actions such as deleting history or viewing
HPP. There is no `ui/security` package.

## 6. Exchanges — PARTIAL

PRD §7.3. `domain/pos/ReturnCorrection.kt` contains `ExchangeSettlement` and `settleExchange` with
tests, but nothing in `ui/returns` references them, so the app has returns without exchanges.

## 7. Cashier shifts — PARTIAL

PRD §8. `CashierShiftEntity` and `ShiftRepository` exist; no UI uses them.
Missing: open/close shift, starting cash, expected versus actual ending cash.

## 8. Receipt output — PARTIAL

PRD §7.4. Printing paths exist, and `AndroidReceiptRenderer` already draws a QRIS bitmap from
`receipt.qrisImagePath`.

Missing: PNG receipt generation with an Android share intent (only `text/plain` exists in
`ui/common/ReceiptSharing.kt`), and QRIS image selection, which depends on the settings screen.

## 9. Reports summary — PARTIAL

PRD §6.4 asks the summary set to cover sales, estimated net profit, HPP, and transaction count.
The headline cards show Total Penjualan, Jumlah Transaksi, and Rata-rata Transaksi. Net profit
appears per day in the breakdown ("Laba ..."), and `totalHpp` exists in the summary and in the
CSV/XLSX exports.

Missing: estimated net profit and HPP in the summary cards.

## 10. Ads — PARTIAL (placeholder)

PRD §7.8. `AdPolicy`, the `AdController` contract, `NoAdsController`, and `BannerAdSlot` exist, and
the slot is placed in Products and Customers.

Missing: the real AdMob SDK, the `INTERNET` permission, ad unit ids, rewarded video for Excel
export, Google Drive backup, and shelf-label printing, plus verified offline hiding. Note that
adding real ads changes the privacy policy, the Data Safety form, and the offline-only positioning.

## 11. Bulk shelf-label printing — MISSING

Mentioned in the PRD §7.8 ad table as a rewarded-video action. No code.

## 12. Performance and verification — PARTIAL

- Baseline Profiles (PRD §9.2): **MISSING**. R8 full mode and resource shrinking are enabled, and the
  release APK is 2.78 MB, inside the 8 MB target.
- Cold-start measurement on a 2 GB device (PRD §9.2): not measured on real hardware.
- Screenshot acceptance matrix (PRD §12): 360×800, 393×852, 320×720, 135% font scale, tablet
  800×1280, and runtime resize. Partially verified through manual device screenshots; there is no
  repeatable capture for every row of the matrix.

## 13. Navigation deviation — decision needed

PRD §4 lists Cashier, Products, Customers, Reports, and More as the primary destinations. The app
ships Beranda, Kasir, Produk, Laporan, and Lainnya, with Customers under Lainnya. This matches the
approved mockups, so it is a deliberate deviation; confirm whether it stays.

## Suggested order

1. **Store settings** — unblocks tax, rounding, store identity, printer address, and QRIS image.
2. **Printer setup** — makes the existing ESC/POS engine reachable for daily use.
3. **Receipt PNG, QRIS image, and share intent** — small, high value for WhatsApp receipts.
4. **Reports summary (HPP and net profit)** — small, completes §6.4.
5. **PIN and biometrics with recovery code** — protects sensitive actions and history.
6. **Local backup and restore** — data ownership, works offline.
7. **Exchanges in the returns flow** — domain logic already exists.
8. **Shift management UI** — closes the §8 data model.
9. **Camera scanning (ML Kit) and scanner setup** — new dependency and permission.
10. **Google Drive backup** — largest item, needs Google Cloud OAuth setup.
11. **Bulk shelf labels**, **AdMob**, and **Baseline Profiles** — polish and monetization.
