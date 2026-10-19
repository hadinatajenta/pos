# Architecture and Shared Contracts

Source of truth for product behavior is `docs/prd.md`. This document records the
structural decisions the orchestrator froze during Phase 0. Agents build against
these shapes and do not change them without orchestrator approval.

Current state recorded at commit `5f1bc73`, 19 October 2026.

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
  deletes anything.
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
`SettingsDestination` enum selects one of six sub-screens:

- `SettingsLanguageScreen` — ordered radio list of the three languages.
- `SettingsPrinterScreen` — device list, paper width, connect/disconnect, test
  print, and receipt preview dialog.
- `SettingsQrisScreen` — QRIS image upload, replacement, and removal.
- `SettingsStoreScreen` — store name, cashier name, tax, and rounding.
- `SettingsBackupScreen` — JSON backup export, import with a confirmation
  dialog, and the shared export result card.
- `SettingsAboutScreen` — app name and version, plus the privacy policy link.

`ui/components/MenuListRow.kt` is shared with the More hub, so both use the same
row component. On wide windows the screen is master-detail
(`PosBreakpoints.twoPane`, 840dp); on compact windows it pushes a sub-screen with
a back action. Feedback messages are scoped to the page that produced them and
are consumed on navigation.

## Barcode scanning

Stage 1 (hardware HID, scanner settings, manual entry) is implemented. Camera capture is not, and
the design plus the accepted engine decision are frozen in `docs/design/barcode-scan.md`.
Structural points that belong in this document:

- The scanner contract stays unchanged: `ScannerInput.scanEvents: Flow<ScanEvent>` with
  `ScanSource.CAMERA` and `ScanSource.HARDWARE_HID`. No contract change is planned.
- Camera capture and decoding will live in `hardware/scanner/` behind a `BarcodeFrameReader`
  interface, so ML Kit or ZXing types never reach `ui/`, following the same split as
  `hardware/receipt`.
- The cashier consumes `scanEvents` through `addScannedCode`, and `MainActivity.dispatchKeyEvent`
  forwards key events to `HardwareScannerInput`. Camera scans will reuse the same ViewModel entry
  point instead of adding a second add-to-cart path.
- `ScannerPreferences` (a `SharedPreferences` store, not the Room settings table) holds the
  per-device hardware scanner switch, so the backup payload format is unchanged.
- Accepted decoder: the existing `zxing.core`, with bundled ML Kit as the documented fallback.
  PRD 7.5 records the decision and the acceptance gate.
- Order of work: stage 1 shipped with no new dependency and no size change; stage 2 is camera
  capture behind a `BarcodeFrameReader`.

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
