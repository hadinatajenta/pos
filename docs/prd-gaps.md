# Implementation status against the PRD

## Quick shift flow fix (2026-10-24)

The new-user shift dead end is fixed. Previously the **Buka shift** action in More only changed the
destination to Cashier, the cart had no shift action, and tapping **Proses** without a shift showed a
non-actionable `cashierShiftRequired` message. The flow now works from three places:

1. **Proses without a shift** opens a compact **Mulai Shift** sheet. The active/default cashier is
   preselected, starting cash is prefilled to **Rp 0** and remains editable, and **Mulai Shift**
   opens the shift and auto-continues to payment. **Atur detail** opens the full staff/starting-cash
   form.
2. **Cart** shows `Shift belum dibuka · Mulai shift` when no shift is open. With an active shift and
   items in the cart it shows a secondary **Ganti kasir** action that uses the existing empty-cart
   guard.
3. **More → Buka shift** navigates to Cashier and opens the same quick sheet over the cart. With an
   active shift the button remains **Ganti kasir** and navigates to the cart for the guard.

The blocking `activeStaff == null` payment guard was removed. A shift without an active staff now
falls back to the shift's `cashier_name`, matching the existing checkout fallback. The quick form
does not change the schema or the shift-close reconciliation rules.

Verified on 2026-10-24 with **707 unit tests (0 failures)**, `assembleDebug`, `assembleRelease`
(R8 and `lintVitalRelease`), and `assembleDebugAndroidTest` compilation. Manual/device and the
instrumented suite were not run; the owner will verify manually.

Remaining gaps:

- The first-run guide and its replay entry in More are deferred to Phase 2.
- With an active shift and an **empty cart**, More → Ganti kasir still navigates to Cashier; the
  cart switch action is only visible when the cart has items. A shared cart-count source is needed
  to open the switch sheet directly from More without bypassing the guard.
- The quick form defaults starting cash to 0. If the owner had opening cash and does not edit it,
  expected cash at close reflects 0; the hint on the field makes this visible.

## AdMob banner on Reports and rewarded export gate (2026-10-24)

Live AdMob placements are implemented:
1. A single adaptive anchored banner sits at the bottom of the Reports screen, below the report content
   and above the navigation bar. It never renders on Cashier, cart, payment, transaction success, or
   receipt screens. It renders no container while offline, in Compose previews, or when the ad fails
   or has no fill, so no blank space is reserved. The export toast lifts above a loaded banner.
2. A rewarded video ad gate for Report export (`AdPlacement.EXCEL_EXPORT`). When exporting from Reports
   (after PIN unlock and storage permissions), `AdMobAdController` requests a rewarded video. If the
   user watches to completion, the export succeeds. If the ad is dismissed before earning the reward,
   export is cancelled with a localized message (`reportsAdRequired`) and no file is written. If the ad
   fails to load or show (offline, no fill, missing Activity, SDK error), the flow fails open and completes
   the export so business data remains accessible. Production rewarded unit: `ca-app-pub-5729173430518647/6119412059`
   (`pookie_rewards`); test unit: `ca-app-pub-3940256099942544/5224354917`. Server-side verification (SSV)
   and frequency capping are disabled.

The Google Mobile Ads SDK is pinned to **24.0.0**. Versions 24.1.0+ require Kotlin 2.1.0 while this
project uses 2.0.21, and 25.5.0 raises `minSdk` to 24 while this project targets 23. Debug builds use
Google's test app ID (`ca-app-pub-3940256099942544~3347511713`), test banner unit (`ca-app-pub-3940256099942544/6300978111`),
and test rewarded unit (`ca-app-pub-3940256099942544/5224354917`); release builds use the production app ID
(`ca-app-pub-5729173430518647~5768680380`), banner unit `Pos_Reports_Banner` (`ca-app-pub-5729173430518647/1672900587`),
and rewarded unit `pookie_rewards` (`ca-app-pub-5729173430518647/6119412059`). A compile-classpath-only force for
`com.google.guava:listenablefuture:1.0` is required because the Ads SDK's empty-artifact constraint
otherwise removes the class CameraX needs at compile time; the runtime keeps Guava.

Verified on 2026-10-24 with 694 unit tests (0 failures), `assembleDebug`, `assembleRelease` (R8 and
`lintVitalRelease`), and a smoke test on an AOSP emulator where a rewarded test ad appeared and
completing it wrote the export. The release APK measured 6.5 MB. Real-device ad show and the
instrumented suite were not run. `CurrentActivityProvider` follows resumed/paused/destroyed
lifecycle so a backgrounded app does not start an ad. See PRD §7.8, §9.1, §15.1 Q7, and §16
version 1.28.

Remaining gaps:

- The AdMob app is not linked to a public Play Store listing yet. Until it is, the app is treated as
  unpublished and receives limited ad serving.
- `app-ads.txt` must be published at the root of the developer website declared in the Play listing
  before AdMob can verify the app.
- Real-device ad show was not tested. Production ads must be tested with registered test devices; do
  not click production ads.
- Server-side verification (SSV) and frequency capping are currently off.
- Other rewarded placements (Google Drive cloud backup, bulk shelf-label printing) remain deferred.
- The Ads SDK merges `com.google.android.gms.permission.AD_ID` and the Privacy Sandbox
  permissions. Complete the Play Console Data Safety and Advertising ID declarations before release.
- The User Messaging Platform consent flow for EEA/UK users is not implemented. Add it before
  serving personalized ads in those regions.
- A future Ads SDK upgrade needs a toolchain decision: 24.1.0+ requires Kotlin 2.1.0, and 25.5.0
  raises `minSdk` to 24. Neither change was made in this iteration.

## Centered headers across screens (2026-10-23)

Cashier, Transactions, Products, Reports, and the More hub now share `CenteredScreenHeader`: centered
title and subtitle, no header action buttons, and a 28 dp gap below. Products keeps its add action as
a floating bottom-right button at every width. Reports moves Export into the period-control row;
the custom period chip remains the entry to the date filter. Verified with 682 unit tests,
`assembleDebug`, and an emulator screenshot of Reports. See PRD §16 version 1.26.

## Transactions header and cards (2026-10-23)

The Transactions header is centered and contains only the title and count/period line; the calendar
and Add actions were removed. The search bar is borderless white with a soft shadow. Status filters
live in one white rounded container, and transaction rows are borderless white cards with 12 dp gaps
and 20 dp padding. Invoice and amount are ExtraBold, while the date and method are muted. The table
layout is no longer used; the card list is used at all widths. Verified with 682 unit tests,
`assembleDebug`, and an emulator screenshot. See PRD §16 version 1.25.

## Product drawer motion (2026-10-23)

The Products add/edit drawer now animates on both open and close: a right slide with a fading scrim
on phones and a right-edge expand/shrink with fade on tablets. The close animation retains the last
form, and the bottom navigation returns only after the slide finishes. No behavior or data changes.
Verified with unit tests and `assembleDebug`. See PRD §16 version 1.24.

## Cashier cart and payment flow (2026-10-23)

The cashier checkout is now page-based on phones: catalog, cart, payment, and confirmation. The cart
uses the shared product card, quantity steppers, swipe-left delete, and a dashed subtotal/total
separator. Payment supports cash, QRIS, transfer, debit, and credit, with cash and QRIS shown first.
Cash uses the system numeric keyboard, automatic thousand separators, and quick amounts derived from
the total. Confirmation is a bottom sheet with a highlighted change card and Confirm & Pay. Receipt
printing and sharing are unchanged. The cart line editor, discount, and negotiated price were removed
from the UI. Verified with 682 unit tests, `assembleDebug`, and manual emulator checks. See PRD §16
version 1.23.

## Cashier search and scan styling (2026-10-23)

The Cashier filter icon button was removed. The search field and barcode scan button now use white
surfaces with a soft 2 dp shadow. Category filtering remains available from the chip row; the
in-stock-only filter was only available from the removed menu and is no longer reachable. Product
cards are unchanged. Verified with unit tests, `assembleDebug`, and an emulator screenshot. See PRD
§16 version 1.22.

## Cashier header and filter container (2026-10-23)

The Cashier header is now centered with no action buttons, the identity/open-shift pill was removed,
and the search gap was increased. Category chips live in one white rounded container with a soft
shadow and horizontal scrolling; inactive chips are transparent and active chips use the brand
green. Product cards and the search field were not changed. Shift opening remains available from
More and from the cart action once a cart exists, so an empty cart has no cashier-local open-shift
control. Verified with unit tests, `assembleDebug`, and an emulator screenshot. See PRD §16 version
1.21.

## Premium Reports UI (2026-10-23)

The Reports screen was restyled without layout or copy changes: the original one-card totals block
(Total Sales, Net Profit, Cost of Goods with a divider) and two side-by-side metric cards are kept,
with borderless white cards, large radii, soft 4–6 dp shadows, an off-white page background, and
muted Inter labels. Filter chips are pills and export feedback is a floating toast with Open/Share
actions. The filter sheet and export dialog use the same tokens. Verified with unit tests and
`assembleDebug`; visual QA is left to the owner. See PRD §16 version 1.20.

## Customer, kasbon, and split-payment removal (2026-10-23)

Customer records, kasbon, partial debt, due dates, and the SPLIT and KASBON payment methods were
removed at the owner's request. Payment methods are now CASH, QRIS, and TRANSFER with full payment
only. The `customers` and `debt_payments` tables and the `transactions.customer_id`,
`remaining_debt`, and `due_date` columns were dropped through the destructive
`MIGRATION_10_11`; legacy SPLIT/KASBON rows map to CASH and PARTIAL_DEBT maps to COMPLETED. The
backup table list and demo seed no longer include customers or debt. Verified with 679 unit tests
(0 failures) and `assembleDebug`; the migration instrumented test did not run because the tablet
was disconnected. See PRD §16 version 1.19.

## Products, tablet navigation, and demo data update (2026-10-23)

The Products page was restored to its pre-redesign shell on 2026-10-23: title header with an add
action, search and sort bar, stock and category chips, and a responsive card grid (one column on
phones, two on tablets). On phones the add action floats at the bottom-right above the navigation bar;
tablets keep it in the header. The compact row card is shared with the cashier, whose catalog is a
single-column list with a 104 dp trailing action area (add button or counter). Add/Edit opens as a
right-side drawer, a slide-over on phones and a 40% inline panel on tablets, with rounded top-left and
bottom-left corners. The earlier hero,
floating search, and sticky-filter description no longer applies. The Products page shares the app
background `#F6F8F7` with the cashier. Tablet navigation keeps the left rail, now 88 dp wide with the
selected label below its icon, and uses the phone navigation styling. Demo seed data no longer uses the word "demo": categories
are plain, SKUs use `SMP-`, and notes use `SMP`; the clear action still removes legacy `DEMO-`
records. `products.icon` is nullable in schema v10; the requested Spring Boot endpoints do not exist
because this repository is Android Room/Compose. Verified with `testDebugUnitTest`, `assembleDebug`,
`ProductsScreenTest` (5/5), and `AppRootTest` (6/6) on TB321FU. See PRD §16 version 1.18.

Audit of `docs/prd.md` against the code on 19 October 2026, including the commit that added local
JSON backup and the in-app privacy policy. Every entry was verified by reading the code and by
running the checks in "Release gate status", not inferred.

This replaces the previous audit at commit `cc74255` (17 October 2026). Several items that were
open then have since been closed; they are listed under "Closed since the previous audit".

The cashier-shift entry was updated on 22 October 2026 for the staff directory, the staff selector,
and the shift/cashier and staff UI redesign. Verification results for the redesign are recorded in
the dated note below and in PRD §16.

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
staff selector landed on 2026-10-22 in the same branch. The cashier identity pill names the active
staff member and opens the shift sheet (**Buka shift** when no shift is open); **More → Shift & Kasir**
shows the active shift and its history, and links to the cashier for switching. The history lists the
last 30 with cashier, times, starting, expected, and actual cash plus variance. A shift is required
before checkout, while browsing and cart building stay available.

**UI redesign, 2026-10-22.** The shift/cashier and staff UI were redesigned. The cashier header now
uses a tonal identity pill (monogram + name + chevron) instead of a separate shift bar under the top
bar; the switch/close sheet uses a full-width segmented control, account rows with initials
monograms, a tonal cash-summary surface without nested cards, one primary action whose label follows
a **Lanjut ke kasir lain** handover toggle (default off), safe status-bar insets, and
thousand-separated money inputs. The staff cards use monogram avatars instead of the shared 3D
avatar, drop the amber **Aktif** badge, and are border-only. Status remains **DONE**. No schema,
data, money-calculation, or permission changes were made. Preview:
`docs/preview/switch-cashier-preview.html`. Verified on 2026-10-22 with 667 unit tests (0 failures),
190 instrumented tests on `legion_tab(AVD)` and `s24fe(AVD)` (189 passed, 1 expanded-device test
skipped, 0 failures), `assembleDebug`, and manual phone/tablet QA at 100% and 135% font scale.
Screenshots are in `docs/preview/screenshots/`.

**Staff directory.** Cashiers are rows in `staffs` (`id`, case-insensitive unique `name`, `role`,
`is_active`, timestamps) behind `StaffRepository`. **Settings → Staf & Kasir** is the only place
that accepts free text: the owner can add, rename, and activate or deactivate staff. The directory
is seeded with one default staff member from the previous settings cashier name, or **Kasir Utama**
when that name is blank. The active operator is stored in `app_settings.active_staff_id`.

**Shift flows.** Opening a shift needs no PIN and selects an active staff member plus starting cash.
With an active shift the sheet separates two flows:

- **Ganti kasir** changes the active staff member without a cash count. The shift stays open, so
  its opening time and starting cash do not change.
- **Tutup shift** records the cash count, shows expected versus actual cash, and closes the shift.
  A **Lanjut ke kasir lain** toggle (default off) can then open a new shift for another staff member;
  the primary action label changes from **Tutup shift** to **Tutup & mulai shift**.

Both flows require an empty cart; otherwise **Kosongkan & Ganti** asks for confirmation. Closing,
switching, and changing kasir use the owner-level `CriticalAction.SWITCH_CASHIER` gate. `ShiftRepository` computes
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

**23 October 2026 — More hub redesign.** The More ("Lainnya") hub now has one store and shift hero,
one grouped navigation surface under "Kelola", and a neutral debug surface under "Alat". On tablets
the detail pane starts empty instead of auto-selecting Settings. Expected cash has an explicit error
state with retry, and the demo actions stack at large font scales. Addresses audit findings 1–19 for
PRD §4, §5, §5.3, §5.5, and §6.5. Test and device verification are recorded in PRD §16 by the
orchestrator.

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
update the PRD if so. The More hub layout was redesigned on 2026-10-23 (see the dated note in §4);
the destination deviation itself is unchanged.

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
