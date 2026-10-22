# Architecture and Shared Contracts

Source of truth for product behavior is `docs/prd.md`. This document records the
structural decisions the orchestrator froze during Phase 0. Agents build against
these shapes and do not change them without orchestrator approval.

Current state recorded at commit `5f1bc73`, 19 October 2026. The cashier-shift section was added on
21 October 2026 and verified on 22 October 2026. The staff-directory section and the staff-linked
shift changes were added on 22 October 2026.

## Layout

- Single Gradle module `:app`.
- Base package and applicationId: `id.umkm.pos.pro`.
- `minSdk 23`, `targetSdk 36`, `compileSdk 36`.
- Money is always whole Rupiah in `Long`. Quantities and unit conversion factors
  are `Double`. `Long` is never used for quantities and `Double` is never used
  for money.

## Ownership

The table below records the Phase 0 allocation for parallel agents. It is
historical: not every planned directory exists yet.

| Owner | Paths under `app/src/main/java/id/umkm/pos/pro/` |
| --- | --- |
| Orchestrator | `app/`, `core/model/`, `core/contract/`, all `*.gradle.kts`, `gradle/libs.versions.toml` |
| Agent 1 — Data & Storage | `data/local/`, `data/backup/`, `data/file/` |
| Agent 2 — POS & Accounting | `domain/pos/`, `domain/accounting/`, `domain/qris/`, `domain/usecase/` |
| Agent 3 — Hardware & Receipts | `hardware/printer/`, `hardware/scanner/`, `hardware/receipt/` |
| Agent 4 — Reports & Ads | `feature/reports/`, `monetization/admob/` |
| Agent 5 — UI & Accessibility | `ui/theme/`, `ui/cashier/`, `ui/settings/`, `ui/common/`, `ui/security/` (not created), `ui/tablet/` (not created) |

`core/model/` and `core/contract/` are frozen. Only the orchestrator edits them.
The Room annotations live only in `data/local/`; `core/model/` stays free of
Android and Room types.

## Ratified contract decisions

- **D1** — `transactions.discount_total` is the sum of all discounts;
  `transactions.invoice_discount` is the invoice-level discount. Both exist.
- **D2** — `totalHpp` and `netProfit` are computed from
  `sum(qty × conversionFactor × baseHpp)`, floored once. `unit_hpp` is stored per
  line as `round(conversionFactor × baseHpp)` for audit and display.
- **D3** — PIN and recovery answer live in a dedicated security store behind
  `SecurityRepository`. `cashier_shifts` does not carry PIN fields. As of
  19 October 2026 only the table and repository exist: no PIN hashing, setup UI,
  biometric prompt, or recovery code has been implemented.
- **D4** — `min_stock_alert` is `Double`, expressed in base units.
- **D5** — Rounding increment is configurable and defaults to `0`; amounts round
  half-up to the nearest increment. `PricingContext.roundingIncrement` drives it.
- **D6** — `SettingsRepository` owns tax rate, rounding increment, store name,
  cashier name, PIN-enabled flag, printer address, and QRIS image path.
- **D7** — `transactions.subtotal` is the sum of item subtotals, after item
  discounts and before the invoice discount.
- **D8** — `SalesSummary.totalSales` is invoice net, pre-tax
  (`SUM(subtotal − invoice_discount)`), so `totalSales − totalHpp = netProfit`.
- **D9** — Returns are persisted in a `returns` / `return_lines` ledger. Reports
  subtract refunds and reversed HPP; the original transaction is left intact.
  `ReturnRepository.recordReturn` restocks products, records the ledger, and
  marks the transaction `RETURNED` in one atomic database transaction.
- **D10** — `TransactionItem.conversionFactor` (and the `transaction_items.conversion_factor`
  column) records the unit conversion factor applied at checkout. **Migration
  policy:** the column is nullable; rows created before schema v2 keep `NULL`,
  which means "unknown". Historical factors are never invented. Returns on such
  rows must supply the factor explicitly.
- **D11** — A completed sale decrements `stock_base_qty` by
  `qty × conversionFactor`, atomically with the sale insert. Overselling is
  permitted: stock may go negative and is never clamped. The Products UI shows
  an explicit oversold status (icon + label, not color alone).
- **D12** — Invoice numbers are `INV-yyyyMMdd-NNNN`, produced by an atomic
  database counter, so two sales in the same millisecond cannot collide. The
  unique index on `transactions.invoice_no` is retained as a second guard.
- **D13** — Core library desugaring is enabled so `java.time` works on minSdk 23
  (Android 6). `desugar_jdk_libs` is pinned in the version catalog.
- **D14** — Cashier identity comes from the `staffs` directory (`Core.model.Staff`,
  `StaffRepository`), not from free text in the shift flow. `transactions.staff_id` and
  `transactions.shift_id` are the identity links; `transactions.cashier_name` remains a
  display snapshot for reprints. `app_settings.active_staff_id` is the current operator and
  is independent of whether a shift is open. A per-cashier PIN is not part of this contract.

## Application layer

`domain/usecase/` is orchestrator-owned and coordinates domain rules with the
repository contracts:

- `CheckoutUseCase` — loads products/units, prices the cart, allocates payment,
  and persists the sale atomically (a kasbon/partial sale increments customer
  debt inside the same transaction).
- `RecordDebtPaymentUseCase` — applies a kasbon payment through
  `TransactionRepository.recordDebtPayment`.
- `ProcessReturnUseCase` — corrects returns and persists them through
  `ReturnRepository`.

`AppContainer` lists only dependencies that have implementations. Local JSON
backup is wired through `DataBackupService`. Google Drive backup and the
`ImportExportService` CSV/XLSX/JSON façade are still absent. Ads are represented
by `NoAdsController` until a real provider lands.

## Files and backup export

Writing a file to the public `Download` folder is a data-layer concern:

- `data/file/BinarySink.kt` and `data/file/CsvSink.kt` are the write contracts
  (`BinarySink` for bytes, `CsvSink` for text).
- `data/file/DownloadsFileSink.kt` implements both, using `MediaStore` on API 29+
  and public storage below it. `DESTINATION_LABEL` is `Download`.
- `data/file/FileNames.kt` normalizes file names and maps extensions to MIME
  types (`csv`, `xlsx`, `json`).

JSON backup lives in `data/backup/`:

- `BackupTables` fixes the table order: parents first for inserts, reversed for
  deletes, and marks the supported tables.
- `BackupPayload` and `BackupJsonCodec` read and write a versioned document
  (`formatVersion`, `schemaVersion`, `exportedAt`, and one row list per table
  with `JsonElement` values).
- `JsonDataBackupService` exports every table and restores them inside one Room
  transaction. Import validates the format version, rejects a newer schema,
  requires every table to be present, and rejects unknown columns before it
  deletes anything. `staffs` is part of the payload. A backup with
  `schemaVersion < 8` is accepted without `staffs`, and the restore seeds
  **Kasir Utama** and points `app_settings.active_staff_id` at it; a backup at
  schema version 8 or later must contain the table.
- `BackupFileNames` produces deterministic `umkm-pos-backup-yyyyMMdd-HHmmss.json`
  names.

`ui/common/DownloadedFileActions.kt` resolves a saved file back to a URI (via
`MediaStore` on API 29+, otherwise a `FileProvider` declared in the manifest as
`${applicationId}.fileprovider`) and opens or shares it. `ui/components/ExportFeedback.kt`
renders the shared running, failed, and completed-with-actions states used by
both the report export and the backup screen.

## Localization

User-facing text is resolved through one contract:

- `ui/common/PosStrings.kt` declares one member per user-facing string (575
  members). `IdStrings`, `EnStrings`, and `ZhStrings` implement it for
  Indonesian, English, and Simplified Chinese.
- `stringsFor(AppLanguage)` selects a catalogue. `LocalPosStrings` and
  `LocalCurrentLanguage` expose it to Compose; the selected code is persisted in
  `app_settings.language_code` through migration 4→5.
- ViewModels never hold rendered text. They emit `UiText` (`UiText.from { strings
  -> ... }` or `UiText.literal`), resolved at the Compose boundary with
  `asText()`. `AppError` is mapped by type through `toUiText()`; the raw
  `AppError.message` is technical diagnostic text and is never rendered.
- Non-Compose output (receipt text, ESC/POS, CSV/XLSX headers and file names)
  takes an explicit `PosStrings` parameter.
- Helpers that produce text (`paymentMethodLabel`, `stockStatusLabel`,
  `debtStatusLabel`, `transactionStatusLabel`, `formatDate`, `formatDateTime`,
  `ReportDates`, `compactRupiah`) take the catalogue as an argument.
- Previews, sample/demo data, product and customer names, invoice numbers, and
  language endonyms are intentionally not translated.

## Settings navigation

`ui/settings/SettingsScreen.kt` is a hub, not a long scrolling page. A private
`SettingsDestination` enum selects one of the sub-screens:

- `SettingsLanguageScreen` — ordered radio list of the three languages.
- `SettingsPrinterScreen` — device list, paper width, connect/disconnect, test
  print, and receipt preview dialog.
- `SettingsQrisScreen` — QRIS image upload, replacement, and removal.
- `SettingsStoreScreen` — store name, cashier name, tax, and rounding.
- `StaffRoute` (`ui/staff/`) — the staff directory: add, rename, and activate or deactivate
  cashiers. This is the only screen that accepts a free-text cashier name.
- `SettingsBackupScreen` — JSON backup export, import with a confirmation
  dialog, and the shared export result card.
- `SettingsAboutScreen` — app name and version, plus the privacy policy link.

`ui/components/MenuListRow.kt` is shared with the More hub, so both use the same
row component. On wide windows the screen is master-detail
(`PosBreakpoints.twoPane`, 840dp); on compact windows it pushes a sub-screen with
a back action. Feedback messages are scoped to the page that produced them and
are consumed on navigation.

## Staff directory

Cashiers are records, not free text. **Settings → Staf & Kasir** is the only screen that accepts a
staff name; the shift flow selects from the active entries.

- `staffs` (`StaffEntity`, `data/local/entity/StaffEntity.kt`) holds `id`, `name`, `role`,
  `is_active`, `created_at`, and `updated_at`. The `name` index is unique and case-insensitive
  (`COLLATE NOCASE`). `role` is `SecurityRole.CASHIER` for this iteration.
- `StaffDao` (`data/local/dao/StaffDao.kt`) and `StaffRepositoryImpl`
  (`data/local/repository/StaffRepositoryImpl.kt`) implement `StaffRepository`
  (`core/contract/Repositories.kt`): observe all staff or the active one, add, rename,
  activate/deactivate, and `ensureDefaultStaff`.
- Schema v8 (`MIGRATION_7_8`) creates `staffs`, adds `app_settings.active_staff_id`, the staff
  columns on `cashier_shifts`, and `transactions.staff_id`/`shift_id` with indexes, then seeds the
  default staff and backfills the active operator, the open shift's opener, and historical
  transaction staff links.
- Names are trimmed, limited to 60 characters, and unique case-insensitively. Deactivating the last
  active staff member is rejected, and deactivating the current operator moves
  `app_settings.active_staff_id` to another active staff member.
- The active operator is stored in `app_settings.active_staff_id` (`SettingsEntity.activeStaffId`)
  and exposed through `StaffRepository.observeActiveStaff`. It is separate from the active shift.
- Seeding keeps the first run usable: a new database inserts **Kasir Utama**; `MIGRATION_7_8`
  inserts the previous settings cashier name when it is not blank, otherwise **Kasir Utama**.
  A restored pre-v8 backup seeds **Kasir Utama**.
- A staff row is an identity and attribution link, not an authentication credential. The owner
  PIN remains the only gate for cash handover; a per-cashier PIN is deferred.

## Cashier shifts

Shift handover is a first-class flow: the shift bar shows the active staff member and opens,
switches, or closes a shift. **More → Shift & Kasir** shows the active shift and links to the
cashier for switching; it also lists the last 30 shifts.

- `ShiftRepository` (`core/contract/Repositories.kt`) is the contract;
  `ShiftRepositoryImpl` (`data/local/repository/ShiftRepositoryImpl.kt`) owns the Room work over
  `CashierShiftEntity`, `ShiftDao`, `StaffDao`, `TransactionDao`, `DebtPaymentDao`, and `ReturnDao`.
- `openShift(staffId, startingCash)` succeeds only when no shift is active and the selected staff
  member exists and is active. It writes `cashier_name` and `opened_by_staff_id`, then points
  `app_settings.active_staff_id` at the opener.
- `switchShift` closes the current shift and inserts the next one inside one
  `database.withTransaction`, so a failed handover cannot leave two active shifts or silently drop
  the close. It writes the closer to `closed_by_staff_id`/`closed_by_name` and the next opener to
  `opened_by_staff_id`.
- `switchOperator(staffId)` implements **Ganti operator**: it requires an open shift, validates
  that the staff member is active, and only changes `app_settings.active_staff_id`. It takes no
  cash count and leaves `cashier_shifts` untouched.
- Cash reconciliation lives in `domain/pos/ShiftReconciliation.kt`. It sums over the shift's
  `opened_at`–`closed_at` window: CASH sales as `paid_amount − change_amount`, SPLIT and KASBON
  deposits as `paid_amount`, CASH debt payments, and return refunds.
  `expectedEndingCash = startingCash + cashSales + cashDebtPayments − cashRefunds`. Refunds are
  assumed cash because the return flow has no refund-method field.
- `CashierShift`, `ShiftCashSummary`, and `ShiftSwitchResult` (`core/model/Sales.kt`) carry the
  closed and opened records, including `openedByStaffId`, `closedByStaffId`, and `closedByName`.
- `transactions.staff_id` and `transactions.shift_id` (schema v8, `MIGRATION_7_8`) link each sale to
  the operator and the open shift. `CheckoutUseCase` takes both from the cashier state. Cash
  reconciliation still sums over the shift's time window, so an operator change inside one shift
  does not split its cash summary.
- `transactions.cashier_name` (schema v7, `MIGRATION_6_7`) remains a display snapshot taken at
  checkout through `CheckoutUseCase`; `StoredReceiptLoader` prefers it over the current settings
  name, so a reprint keeps the name used at sale time even after a handover or a staff rename.
- Closing a shift, closing with a handover, and changing operator are `CriticalAction.SWITCH_CASHIER`,
  mapped to `SecurityRole.OWNER` with `PinPolicy.alwaysPrompts`. Opening is ungated. A non-zero variance
  requires explicit confirmation in the shift flow; the repository stores the counted cash and the
  computed expected cash.
- **Ganti operator** records no cash count and leaves the shift open; it only changes
  `app_settings.active_staff_id`. This iteration does not authenticate a staff member: `staffs`
  provides attribution, not credentials, and a per-cashier PIN is deferred (PRD §15.1 Q15).

## Barcode scanning

Camera capture and hardware HID are both implemented. The design, the accepted engine decision, and
the measured gates live in `docs/design/barcode-scan.md`. Structural points that belong here:

- The scanner contract stays unchanged: `ScannerInput.scanEvents: Flow<ScanEvent>` with
  `ScanSource.CAMERA` and `ScanSource.HARDWARE_HID`. No contract change is planned.
- Camera capture and decoding live in `hardware/scanner/` behind a `BarcodeFrameReader` interface,
  so ZXing types never reach `ui/`, following the same split as `hardware/receipt`. Frame extraction
  is a separate pure object so it can be unit tested without a camera.
- The cashier consumes `scanEvents` through `addScannedCode`, and `MainActivity.dispatchKeyEvent`
  forwards key events to `HardwareScannerInput`. Camera scans will reuse the same ViewModel entry
  point instead of adding a second add-to-cart path.
- `ScannerPreferences` (a `SharedPreferences` store, not the Room settings table) holds the
  per-device hardware scanner switch, so the backup payload format is unchanged.
- Accepted decoder: the existing `zxing.core`, with bundled ML Kit as the documented fallback.
  PRD 7.5 records the decision and the acceptance gate.
- Dependencies: `zxing.core` (already present for QR generation) and CameraX, which took the release
  APK from 3.16 MB to 3.90 MB.

## Calculation order

1. `ItemSubtotal = qty × unitPrice − itemDiscount`
2. `InvoiceNet = sum(ItemSubtotal) − invoiceDiscount`
3. `Tax = floor(InvoiceNet × taxRatePercent / 100)`
4. `GrandTotal = InvoiceNet + Tax + rounding`
5. `NetProfit = InvoiceNet − totalHpp`

Unit price priority: negotiated override, then the eligible wholesale tier with
the highest minimum quantity not exceeding the purchased quantity, then the base
retail price.

## Build constraint

`org.gradle.workers.max=2` is required on the development machine. Higher values
crash the AAPT2 daemon during release resource compilation. See
`docs/dev-environment.md`.
