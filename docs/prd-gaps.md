# Implementation status against the PRD

Audit of `docs/prd.md` against the code on 19 October 2026, including the commit that added local
JSON backup and the in-app privacy policy. Every entry was verified by reading the code and by
running the checks in "Release gate status", not inferred.

This replaces the previous audit at commit `cc74255` (17 October 2026). Several items that were
open then have since been closed; they are listed under "Closed since the previous audit".

The cashier-shift entry was updated on 22 October 2026 for the staff directory and staff selector.
Implementation and verification of that change are finalized by the orchestrator; this audit does
not claim new test, build, or device results for it.

Legend: **DONE** = usable by an end user today · **PARTIAL** = data/domain layer exists but the UI
or wiring does not · **MISSING** = nothing in the code.

## Summary

| # | PRD area | Status |
| --- | --- | --- |
| 1 | Store settings, tax, rounding | DONE |
| 2 | Thermal printer setup | DONE (Bluetooth only wired) |
| 3 | Barcode scanner | DONE (camera and hardware HID) |
| 4 | Local backup and restore (JSON) | DONE |
| 5 | PIN, biometrics, offline recovery | DONE (PIN, recovery code, audit and enforcement; biometrics deferred) |
| 6 | Returns and exchanges | PARTIAL (returns DONE, exchanges MISSING) |
| 7 | Cashier shifts | DONE (shift handover and staff selector; per-cashier PIN not included) |
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

- **Light-only theme (§3.1, §5.1, §9.3, §12).** Dark mode was removed by owner decision on
  2026-10-21 (PRD §15.1 Q12). The app now always renders the light theme, so the previous "dark
  mode not visually verified" gap no longer applies. The change is recorded in PRD §16 revision 1.7.
  Verified on 2026-10-21: `testDebugUnitTest` passed with 627 tests and 0 failures,
  `assembleDebug` succeeded, and 48 instrumented tests passed on the TB321FU tablet. The TB321FU
  tablet and the SM-S721B phone were inspected with Android night mode enabled: Kasir, Laporan,
  Lainnya/Pengaturan, the barcode dialog, and the 135% font scale all stayed light with readable
  system bars. Lint still reports 14 pre-existing errors unrelated to this change.
- **Launcher icon (§5.2).** The Android launcher icon now uses the green-and-white P mark from
  `newicon.jpeg`. The adaptive foreground, monochrome silhouette, square and round mipmaps at 48,
  72, 96, 144, and 192 px, and the adaptive background `#FCFFFE` were regenerated from that source.
  The P was scaled down to about 48% of the icon height, centered in the white field, so it has
  more breathing room from the border.
  Verified on 2026-10-21: `assembleDebug` and `assembleRelease` succeeded, the release APK keeps the
  adaptive, monochrome, and every legacy icon asset after resource shrinking, and the adaptive icon
  rendered correctly on the SM-S721B app-info screen. Legacy icons were not exercised on API 23–25
  because only Android 16 and 17 devices are connected.

## Still open

### 1. PIN, biometrics, and anti-lockout — DONE (PRD §6.6, §7.7)

Implemented on 2026-10-20 in two passes. The data and policy layer: `core/model/Security.kt`
(roles, the `CriticalAction` registry, security state, audit types), `core/contract/Security.kt`
(`PinHasher`), an extended `SecurityRepository`, database version 6 with `MIGRATION_5_6` adding the
PIN state columns and the `security_audit_log` table, `Pbkdf2PinHasher` (salted PBKDF2 that stores
the algorithm and iteration count inside the encoded value, with a SHA-1 fallback for API 23–25),
`PinPolicy` (role per action, discount and below-cost thresholds, lockout windows, session length),
`RecoveryCodeGenerator`, and `SecurityService` (unlock, enable, change, disable, recovery-code
rotation, session locking, audit). The JSON backup excludes `security_store` and
`security_audit_log`, and a restore forces the PIN off and writes `restore_reset_pin` to the audit
log.

The UI and enforcement layer: `ui/security/SecurityGate.kt` with one global PIN dialog mounted in
`AppRoot` (a single dialog, never stacked), `SecurityRoute`/`SecurityViewModel`/`SecurityScreen` for
enable, change, disable, recovery-code regeneration, the one-time recovery code with a mandatory
"code saved" acknowledgement and PDF printing through the Android Print Framework, and a read-only
audit history. The gate is applied to profit/HPP viewing (values masked until elevated), HPP and
price editing, item discounts above 10% or Rp 20.000, negotiated prices below cost, refund or
return, CSV/XLSX export, JSON backup export, backup restore, product deletion, tax and rounding,
QRIS removal, and debt payments. Manager-level actions accept a five-minute session; owner-level
actions always prompt. With the PIN off, every flow is unchanged.

Verified: `testDebugUnitTest` 627 tests / 0 failures, `assembleDebug` succeeds, and 48 instrumented
tests pass on a physical tablet (including the 5→6 migration and a backup round trip that proves no
PIN material is exported and that a restore turns the PIN off). On device: PIN enable, one-time
recovery code, wrong-PIN feedback, correct-PIN unlock, profit/HPP masking, the owner-level gate
prompting inside an open session, recovery code disabling the PIN, and the audit history showing
every decision. A 135% font check and the compact phone layout were inspected; both are clean.

**Remaining, deliberately deferred:** biometric recovery (PRD §15.1 Q3), per-user PINs and role
management (Q4), and transaction void (no UI exists). The print dialog itself was not exercised on
a device.

### 2. Barcode scanning — DONE (camera and hardware HID, read-rate gate pending)

PRD §6.1, §6.5, §7.5. Stage 1 landed on 19 October 2026:

- `MainActivity.dispatchKeyEvent` now feeds `HardwareScannerInput`, so a USB or Bluetooth HID
  scanner turns into `scanEvents` and the cashier adds the item with no tap on the search field
(PRD 6.1). Verified on device by injecting a fast key sequence.
- The interceptor only consumes Enter or Tab while a scan sequence is still live, and a stale
  sequence is discarded instead of swallowing the key. Without that guard, a stray Enter would
  have been eaten with no scan produced.
- **More, Settings, Pemindai Barcode** explains both capture methods, carries the hardware
  scanner switch (`ScannerPreferences`, stored per device, not in the backup payload), and shows
  the last decoded code so the owner can confirm the scanner before a real sale.
- The cashier search row has a scan action, and the product form's barcode field has a scan
  icon; both open the same scan surface. At the time of stage 1 its camera section was labelled
  **Belum tersedia** rather than shown as a working control; stage 2 replaced that with the live preview.
Stage 2 landed the same day:

- Camera capture uses CameraX with the `zxing.core` decoder already in the app, behind a
  `BarcodeFrameReader` interface, so ML Kit was not needed and the release APK grew from 3.16 MB to
  **3.90 MB**, well inside the 8 MB budget in PRD 9.2.
- Analysis is capped at 1280x720, keeps only the latest frame, throttles to about six frames per
  second, and decodes the luminance plane without a colour conversion.
- The scan surface has seven states: permission request, permission blocked with a route to app
  settings, no camera hardware, live preview, camera failure with retry, unknown code, and success.
  Manual entry stays available in every one of them.
- Text never sits on the camera image. Every label is on a solid surface, and the white finder frame
  is drawn over the 55% scrim, which keeps it at 7:1 even against a pure white scene.
- The app still requests no `INTERNET` permission, and camera frames are discarded after analysis.
- Hardened after the first field report: the frame is rotated upright before decoding, two matching
  reads are required before a code is accepted, autofocus is driven at the finder centre every 1.8
  seconds, `CODE_39` is dropped as a false-positive source, and implausible codes are rejected.
- **Pending:** the read-rate gate. A physical barcode has not been scanned from this machine because
  there is no second screen to aim the phone at; the owner tests on their own device. The decoder is
  unit tested by rendering an EAN-13, reading it back, and doing the same after a quarter-turn rotation.

The design is frozen in `docs/design/barcode-scan.md`, and PRD 7.5 now records the accepted
engine decision: CameraX with the existing `zxing.core` decoder first, bundled ML Kit only if the
measured read rate misses the acceptance gate. Delivery order is hardware interceptor and
scanner setup first (no new dependency, no size change), then camera capture.

### 3. Returns exchange flow — PARTIAL

PRD §7.3. `domain/pos/ReturnCorrection.kt` contains `ExchangeSettlement` and `settleExchange` with
tests, but no UI references them. The app records returns without the exchange path.

### 4. Cashier shifts and staff selector — DONE (handover); per-cashier PIN deferred (PRD §6.1, §6.6, §7.7, §8)

Shift handover was implemented on 2026-10-21 in `feat/switch-cashier`. The staff directory and
staff selector landed on 2026-10-22 in the same branch. The shift bar names the active staff member
and offers **Ganti** or **Buka**; **More → Shift & Kasir** shows the active shift and its history,
and links to the cashier for switching. The history lists the last 30 with cashier, times, starting,
expected, and actual cash plus variance. A shift is required before checkout, while browsing and
cart building stay available.

**Staff directory.** Cashiers are rows in `staffs` (`id`, case-insensitive unique `name`, `role`,
`is_active`, timestamps) behind `StaffRepository`. **Settings → Staf & Kasir** is the only place
that accepts free text: the owner can add, rename, and activate or deactivate staff. The directory
is seeded with one default staff member from the previous settings cashier name, or **Kasir Utama**
when that name is blank. The active operator is stored in `app_settings.active_staff_id`.

**Shift flows.** Opening a shift needs no PIN and selects an active staff member plus starting cash.
With an active shift the sheet separates two flows:

- **Ganti operator** changes the active staff member without a cash count. The shift stays open, so
  its opening time and starting cash do not change.
- **Tutup shift** records the cash count, shows expected versus actual cash, and closes the shift.
  It may optionally open a new shift for another staff member.

Both flows require an empty cart; otherwise **Kosongkan & Ganti** asks for confirmation. Closing,
switching, and changing operator use the owner-level `CriticalAction.SWITCH_CASHIER` gate. `ShiftRepository` computes
expected cash from the shift time window
(`startingCash + cashSales + cashDebtPayments − cashRefunds`), requires confirmation for a non-zero
variance, keeps close and open atomic, and links transactions to `staff_id` and `shift_id` while
retaining the `cashier_name` snapshot. Refunds are assumed cash because the return flow has no
refund method. Schema version 8 and `MIGRATION_7_8` add `staffs`,
`cashier_shifts.opened_by_staff_id`/`closed_by_staff_id`/`closed_by_name`,
`transactions.staff_id`/`shift_id`, and `app_settings.active_staff_id`. Backups include `staffs`;
older backups without it are accepted and seed a default staff.

**Verification.** Verified on 2026-10-22:
`testDebugUnitTest` 654 tests / 0 failures; `connectedDebugAndroidTest` 90 tests on `s24fe(AVD)`
(89 passed, 1 expanded-device test skipped, 0 failed); `assembleDebug` and `assembleRelease`
succeed. Manual emulator QA covered staff add, search, on-duty status, the 3D staff avatar,
overflow menu, FAB, the two-tab operator/close flow, operator switching, the staff selector, and
the close form. Active and on-duty badges passed WCAG contrast checks (8.15:1 and 10.86:1). The earlier phone QA covered opening a
shift, a cash sale updating expected cash to Rp 204.000, the owner-PIN gate, the non-empty-cart
guard, a non-zero variance (Kurang Rp 5.000) requiring the checkbox, the More card/history, and the
close form at 135% font. The TB321FU tablet was disconnected for these runs. Lint reported 14
pre-existing errors, 57 warnings, and no staff/shift-specific findings.

**Remaining, deliberately deferred:** a per-cashier PIN. The owner PIN still protects the cash
handover, and the staff selector fixes names and operator attribution, but the till is not
authenticated per person yet. Per-user credentials, enrollment, recovery, and role-based staff
permissions are a later security step (PRD §15.1 Q15), not part of this change.

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

### Test count correction (2026-10-20)

The `testDebugUnitTest` figure in the table above (469 tests, 62 classes) predates the barcode
stage-2 work and is not reproducible from the source tree. Counting annotation occurrences on
2026-10-20 gives **505 `@Test` in 66 files** under `app/src/test/` and **45 `@Test` in 10 files**
under `app/src/androidTest/`. The suite has not been re-run since, so no pass count is claimed
here. Reference: `docs/security/prd-code-reconciliation.md` R-51.

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

1. **Lint to green and stabilize `PosAppTest`**: a release cannot ship with a red gate.
2. **Hardware verification**: the Bluetooth printer, and a physical HID scanner on the new
   Pemindai Barcode test area.
3. **PIN UI and enforcement** — done on 2026-10-20. The next useful step is a hands-on pass on the
   owner's device: print the recovery code to PDF, run five wrong PINs to see the lockout, and
   restore a backup through the file picker.
4. **Barcode scanning**: both stages are in. What remains is the read-rate gate on real product
   labels with a physical device aimed at a barcode.
5. **Per-cashier PIN and staff roles**: the next security step after the staff directory. Add
   per-person credentials, enrollment and recovery, and role-based permissions so the till
   authenticates the operator instead of relying only on the owner PIN (PRD §15.1 Q15).
6. **PNG receipt and image share**: small, high value for WhatsApp receipts.
7. **Exchanges in the returns UI**: domain logic already exists.
8. **Shift management UI**: shipped on 2026-10-21, with the staff directory and staff selector
   added on 2026-10-22. Phone QA on 2026-10-22 covered open shift, cash sale, expected-cash update,
   the owner-PIN gate, the cart guard, a non-zero variance confirmation, history, and 135% font.
   What remains is the same pass on the tablet, a completed handover after entering the owner PIN,
   and device verification of the staff selector and both shift flows.
9. **Google Drive backup and SQLite `.db` export**: the remaining half of §7.6.
10. **AdMob, shelf labels, Baseline Profiles**: monetization and polish.
