# Implementation status against the PRD

Audit of `docs/prd.md` against the code on 19 October 2026, including the commit that added local
JSON backup and the in-app privacy policy. Every entry was verified by reading the code and by
running the checks in "Release gate status", not inferred.

This replaces the previous audit at commit `cc74255` (17 October 2026). Several items that were
open then have since been closed; they are listed under "Closed since the previous audit".

Legend: **DONE** = usable by an end user today · **PARTIAL** = data/domain layer exists but the UI
or wiring does not · **MISSING** = nothing in the code.

## Summary

| # | PRD area | Status |
| --- | --- | --- |
| 1 | Store settings, tax, rounding | DONE |
| 2 | Thermal printer setup | DONE (Bluetooth only wired) |
| 3 | Barcode scanner | PARTIAL (HID only) |
| 4 | Local backup and restore (JSON) | DONE |
| 5 | PIN, biometrics, offline recovery | MISSING |
| 6 | Returns and exchanges | PARTIAL (returns DONE, exchanges MISSING) |
| 7 | Cashier shifts | PARTIAL |
| 8 | Receipt output | PARTIAL |
| 9 | Reports summary and exports | DONE |
| 10 | Ads | PARTIAL (placeholder) |
| 11 | Bulk shelf-label printing | MISSING |
| 12 | Performance and verification | PARTIAL |
| 13 | Navigation and information architecture | Deviation, decision required |
| 14 | Localization (not in the original audit) | DONE |
| 15 | Privacy policy in the app | DONE |

## Closed since the previous audit

- **Store settings** (§6.6, §7.1, §7.2). The Settings hub writes store name, cashier name, tax
  rate, rounding increment, printer selection, paper width, and the QRIS image. Tax and rounding
  are consumed by checkout.
- **Printer setup** (§6.5, §7.5). `ui/settings/SettingsPrinterScreen.kt` exposes connection status,
  the paired-device list, paper width, connect/disconnect, test print, and receipt preview.
  `DefaultAppContainer.receiptPrinter` is a `BluetoothReceiptPrinter`.
- **QRIS image selection** (§7.4, §6.5). `ui/settings/SettingsQrisScreen.kt` validates, stores, and
  replaces the static payload and image; `AndroidReceiptRenderer` draws the image on printed
  receipts. `QrisPayload.withAmount` builds the dynamic QR with the exact amount.
- **Reports summary** (§6.4). The summary cards now show total turnover, estimated net profit, HPP,
  transaction count, and average transaction, alongside the chart and payment breakdown.
- **Localization** (§9.3, in practice a product requirement). Interface, receipts, printer output,
  and report exports follow Indonesian, English, or Simplified Chinese. `ui/common/PosStrings.kt`
  is the single contract; `IdStrings`, `EnStrings`, and `ZhStrings` are the catalogues; `UiText`
  carries language-neutral messages from ViewModels.
- **Settings structure** (§6.5). Settings is a hub with dedicated sub-pages for language, printer,
  QRIS, store profile, backup, and about, with a shared `ui/components/MenuListRow.kt`.
- **Local backup and restore** (§6.5, §7.6, JSON part). `data/backup/JsonDataBackupService` exports
  every table to a versioned JSON document in `Download` and restores it inside one Room
  transaction. Import validates the format version, refuses a newer schema, requires every table,
  and rejects unknown columns before deleting anything, with a confirmation dialog. Product photo
  and QRIS image files are stored as paths, not bundled, so a restore on a new device keeps the
  references but not the images. The SQLite `.db` export and the Google Drive path are still
  missing.
- **Privacy policy in the app** (Play requirement). **Pengaturan → Tentang & Privasi** shows the
  app version and opens the hosted policy.
- **Export feedback** (§7.4). Report exports and the JSON backup now show a completion card with
  the file name, the destination folder, and **Open** and **Share** actions instead of a one-line
  message.

## Still open

### 1. PIN, biometrics, and anti-lockout — MISSING (MVP scope)

PRD §6.6, §7.7. `security_store`, `SecurityRepository`, and the `isPinEnabled` flag exist, but
nothing writes or verifies a PIN: no `ui/security` package, no hashing, no biometric prompt, no
offline recovery code, and no enforcement on sensitive actions such as deleting history or viewing
HPP. `HashComparison.matchesHash` only compares strings; no PIN hash is produced anywhere.

### 2. Camera barcode scanning — MISSING (MVP scope)

PRD §6.5, §7.5. `HardwareScannerInput` and `KeyboardWedgeScanDecoder` handle USB/Bluetooth HID
input. There is no CameraX or ML Kit dependency, no `CAMERA` permission, and no scanner setup
screen.

### 3. Returns exchange flow — PARTIAL

PRD §7.3. `domain/pos/ReturnCorrection.kt` contains `ExchangeSettlement` and `settleExchange` with
tests, but no UI references them. The app records returns without the exchange path.

### 4. Cashier shifts — PARTIAL

PRD §8. `CashierShiftEntity` and `ShiftRepository` exist; no UI opens or closes a shift, records
starting cash, or reconciles expected versus actual cash.

### 5. Receipt output — PARTIAL

PRD §7.4. Printed receipts and `text/plain` sharing work. PNG receipt generation and an image
share intent are missing (`ui/common/ReceiptSharing.kt` sends text only).

### 6. Google Drive backup — MISSING

PRD §7.6. No Drive API or OAuth dependency, and the app intentionally has no `INTERNET`
permission. This is the largest remaining item and cannot work offline.

### 7. Ads — PARTIAL (placeholder)

PRD §7.8. `AdPolicy`, `AdController`, `NoAdsController`, and `BannerAdSlot` exist; the production
container uses `NoAdsController`. There is no AdMob SDK, no ad unit ids, and no rewarded video for
export or backup. The current app is ad-free, which satisfies "no friction at checkout" but does
not implement the ad-supported placements in the PRD.

### 8. Bulk shelf-label printing — MISSING

Referenced by the PRD §7.8 ad table. No code.

### 9. Performance and verification — PARTIAL

- R8 full mode and resource shrinking are enabled; the release APK is 3.1 MB and the AAB is 5.7 MB,
  both inside the 8 MB target.
- Baseline Profiles are not configured and cold start has not been measured on a 2 GB device.
- The full screenshot matrix in PRD §12 has been spot-checked on a phone and a tablet, including
  135% font scale, but there is no repeatable capture for every row.
- Bluetooth printer and HID scanner paths have not been exercised on physical hardware.

### 10. Navigation — decision required

PRD §4 lists Cashier, Products, Customers, Reports, and More as primary destinations. The app ships
Kasir, Produk, Transaksi, Laporan, and Lainnya, with Customers under More. The previous audit
recorded this as a deliberate deviation matching the approved mockups; confirm whether it stays and
update the PRD if so.

## Release gate status

| Check | Result |
| --- | --- |
| `testDebugUnitTest` | 469 tests, 0 failures, 62 classes |
| `connectedDebugAndroidTest` | 40 tests pass on a physical tablet, including a JSON export/import round trip against a real Room database. An intermittent `PosAppTest` failure was observed once on an earlier revision and did not reproduce. |
| `assembleRelease` | Success with R8 and resource shrinking; signed APK 3.1 MB |
| `bundleRelease` | Success; signed AAB 5.7 MB |
| `lintRelease` | **Fails**: 11 errors, 38 warnings |
| Release smoke test | Release APK installs and starts on a tablet; report export, JSON backup export/import, and the privacy policy link were exercised on a physical phone |

### Lint errors

- 9 × `NewApi` in `data/image/ImageDecoding.kt`. The code is guarded by
  `Build.VERSION.SDK_INT >= P`, but lint cannot see the guard across the extracted helper.
  Add `@RequiresApi(Build.VERSION_CODES.P)` to `decodeWithImageDecoder`.
- 2 × `ProduceStateDoesNotAssignValue` in `ui/cashier/CashierPayment.kt` and
  `ui/components/FileImage.kt`. Both assign `value` inside a nested block; restructure so the
  assignment is at the top level of the producer lambda.

### Flaky instrumented test

`PosAppTest.primaryDestinationsAreDisplayed` marks onboarding complete in `@Before`, after the
activity has already launched, then asserts on the navigation labels without waiting for the root
to switch. Wait for the main navigation to exist (for example
`waitUntilAtLeastOneExists(hasText("Lainnya"), 5_000)`) or complete onboarding before launch.

## Suggested order

1. **Lint to green and stabilize `PosAppTest`** — a release cannot ship with a red gate.
2. **Hardware verification** — Bluetooth printer and HID scanner on real devices.
3. **PIN, biometrics, and recovery code** — protects sensitive actions; must include the offline
   recovery path.
4. **Camera scanning** — new dependency, `CAMERA` permission, and scanner setup.
5. **PNG receipt and image share** — small, high value for WhatsApp receipts.
6. **Exchanges in the returns UI** — domain logic already exists.
7. **Shift management UI** — closes the §8 data model.
8. **Google Drive backup and SQLite `.db` export** — the remaining half of §7.6.
9. **AdMob, shelf labels, Baseline Profiles** — monetization and polish.
