# Barcode scanning: design

Status: implemented. Stage 1 (hardware HID, settings, manual entry) and stage 2 (camera) are in.
The read-rate gate has not been run for lack of a physical barcode in front of the camera.
Antislop mode DURING. Date: 19 October 2026.

The owner accepted the recommendation in this document and delegated the remaining choices
to it. PRD 7.5 was updated to match: CameraX with the existing `zxing.core` decoder first,
bundled ML Kit only if the measured read rate misses the acceptance gate.

Source of truth for behaviour is `docs/prd.md`. This document designs the barcode scanning feature
required by the PRD and records the decisions with their reasons (antislop R-31).

PRD requirements covered:

- 6.1: "A compact top bar with store context and a clearly accessible scanner/search action."
- 6.1: "A hardware barcode scanner must be able to add items without requiring the user to tap the
  search field each time when scanner mode is active."
- 6.2: "Provide clear search, barcode/SKU lookup, and category filters."
- 6.5: "Scanner setup should explain camera scanning and hardware scanner support briefly."
- 7.5: "Camera scanning with Google ML Kit Barcode Scanning." and "Hardware USB OTG and Bluetooth
  HID input through a keyboard-event interceptor."
- 7.5: "Handle unexpected detach events gracefully. Removing a USB scanner or printer must not crash
  the application."
- 9.2: release package below 8 MB.

## What already exists, and what is missing

Verified by reading the code, not assumed:

| Piece | State |
| --- | --- |
| `ScannerInput` contract with `scanEvents: Flow<ScanEvent>` | Exists in `core/contract/Hardware.kt` |
| `ScanSource` enum with `CAMERA` and `HARDWARE_HID` | Exists, unused |
| `KeyboardWedgeScanDecoder` (inter-character timing, Enter/Tab terminators) | Exists, unit tested |
| `HardwareScannerInput.handleKeyEvent(event)` | Exists |
| Cashier consumes `scannerInput.scanEvents` and adds to cart | Exists (`CashierViewModel`, `addScannedCode`) |
| Anything that calls `handleKeyEvent` | **Missing.** No `dispatchKeyEvent` in `MainActivity` |
| `CAMERA` permission and `uses-feature` | **Missing** from the manifest |
| Camera scanning, scan UI, scanner setup screen | **Missing** |

Two consequences drive the whole design:

1. The hardware HID path is **dead code today**. The decoder and the ViewModel side are ready, but
   no key events ever reach them. Wiring the interceptor is a prerequisite, not a nice to have.
2. `addScannedCode` already implements the correct behaviour (unit barcode first, then product
   barcode, then "barcode not found"). Camera scanning must reuse it instead of writing a second
   add-to-cart path.

## Design Read

> Reading this as: an in-app capture tool inside a cashier-first POS for Indonesian small shops, in
> the existing restrained Material 3 language, dial ENERGY 1 / RHYTHM 1 / MOTION 1.

Reason for the dials: the app is a work tool used in a hurry, often on a low-end phone in a shop.
The existing system is calm and uniform, and the PRD section 5 asks for restraint, so the scanner
inherits it. A loud capture screen would be a different product's visual language.

Direction source: no `DESIGN.md` exists, but direction is not missing. It lives in `docs/prd.md`
section 5 plus the implemented tokens (`PosColors`, `PosTypography`, `PosShapes`, `PosSpacing`) and
the existing component set. This design extends them and introduces no new palette, typeface, or
shape.

## Architecture

Contract first, so the UI never touches ML Kit or CameraX types:

```
ui/scanner/BarcodeScanScreen.kt        Compose camera surface: preview host, overlay, states, torch (stage 2)
ui/scanner/BarcodeEntryDialog.kt       stage 1: manual entry plus an honest camera status row
ui/scanner/BarcodeScanState.kt         sealed state: RequestingPermission, Ready, Denied, Unavailable, Failed
hardware/scanner/BarcodeFrameReader    interface: decode one luminance frame, return the first code
hardware/scanner/ZxingBarcodeFrameReader    implementation on the zxing.core already in the app
hardware/scanner/MlKitBarcodeFrameReader    alternative implementation, added only if measurement demands it
hardware/scanner/HardwareScannerInput  unchanged, now actually fed by MainActivity
core/contract/Hardware.kt              unchanged
MainActivity                           dispatchKeyEvent -> HardwareScannerInput.handleKeyEvent
app/DefaultAppContainer.kt             picks the reader implementation, one line to swap
```

Decisions and reasons:

- **Reuse `addScannedCode`.** Camera codes and HID codes converge in `CashierViewModel` through one
  entry point. A second path would drift from the stock rules already implemented there (stock
  check, quantity step, unit barcode priority).
- **The analyzer lives in `hardware/`, not in `ui/`.** ML Kit types stay out of the UI layer, the
  same way `hardware/receipt` owns rendering and `ui` only displays it.
- **The scan surface is a modal that does not navigate.** After closing it the cashier is exactly
  where they were, with the cart untouched. Reason: a sale in progress must not be interrupted.
- **The product form swaps to the scanner instead of stacking a second dialog.** PRD 4.2 says not to
  open multiple overlapping dialogs for one task, and the form is already a dialog on phones.
- **Camera is bound to the scan surface's lifecycle.** The analyzer is attached when the surface
  appears and cleared when it leaves, including backgrounding the app. Reason: a camera left open
  drains the battery and blocks other apps.

### Engine decision: start without ML Kit

The PRD names ML Kit, but the PRD also sets the release budget under 8 MB (9.2) and the target
device class is a 2 GB RAM phone. Those two constraints point away from ML Kit for the first
version, so this was a deliberate, documented deviation from PRD 7.5. It was approved on
19 October 2026 and PRD 7.5 now records the decision.

Recommendation: **CameraX plus the `zxing.core` that is already a dependency**, behind a
`BarcodeFrameReader` interface, with ML Kit kept as a one-file swap if measurement demands it.

Why this order, and not ML Kit first:

1. It keeps the package small. The marginal dependency is CameraX alone, roughly 1 MB. The bundled
   ML Kit model adds an estimated 2.5 to 3.5 MB, which would nearly double a 3.1 MB download for a
   user on a slow rural connection. The Play AAB is split per ABI and density, so the real per-device
   download is smaller than the universal APK, but the split still carries the same model.
2. It is lighter on the target hardware. ZXing decodes from the luminance plane, so no colour
   conversion per frame and no model resident in memory. On a 2 GB device that is measurable.
3. It reuses what exists. `zxing.core` is already in `libs.versions.toml` for QR generation, so this
   is not a new decoding engine in the product, only a second use of one already shipped.
4. The failure mode is recoverable. A missed read costs one retry and the manual entry fallback is
   always on screen; a wrong read is the outcome that would actually hurt, and both engines are
   strong there on clean retail labels.
5. The swap stays cheap. The reader sits behind an interface, so choosing ML Kit later is one new
   class, one dependency line, and one changed line in the container, with no UI or ViewModel
   change. That is what makes "measure first" safe rather than a bet.

What ML Kit is genuinely better at: torn, faded, curved, or very small labels, and low light. If the
measured first-try rate on your own products falls below the bar in the gate below, switch to ML Kit
and accept the size.

Rejected outright: `com.google.android.gms:play-services-mlkit-barcode-scanning`. It fetches the model
through Google Play Services, needs the network on first use, and breaks the offline-first promise
and the current "no data collected" position.

Decoder configuration that keeps ZXing both fast and accurate:

- Restrict `PossibleFormats` to the retail set: EAN_13, EAN_8, UPC_A, UPC_E, CODE_128, CODE_39, ITF.
  Fewer formats means fewer passes per frame and fewer false positives.
- Feed the Y plane of the YUV frame as the luminance source, no RGB conversion.
- Leave `TRY_HARDER` off; it costs latency and the code fills a large part of the frame at the
  distance a shop actually scans from.
- Continuous autofocus with the focus point at the finder frame centre.

### Performance budget

The scanner must not slow the cashier, so these are budget items, not suggestions:

| Item | Value | Reason |
| --- | --- | --- |
| Analysis resolution | 1280 x 720 | Enough to resolve EAN-13 at 20 to 30 cm, cheap per frame |
| Backpressure | `STRATEGY_KEEP_ONLY_LATEST` | Drops stale frames instead of queueing them, which is what causes lag and heat |
| Analysis rate | At most one frame per 150 to 200 ms | A read is not needed 30 times a second |
| Decode work | Single background thread, luminance only | No colour conversion, no per-frame allocation |
| Buffer hygiene | `ImageProxy.close()` in a `finally`, no retained frames | A leaked frame stalls the pipeline |
| Lifecycle | Bind on show, unbind on hide, stop on `ON_PAUSE` | A camera left open drains battery and blocks other apps |
| Preview | 1080p maximum, Compose overlay above it | The overlay is text and a border, so it costs almost nothing |
| Torch | Off unless the user turns it on | Battery |

Expected cost, to be confirmed on device rather than assumed: a ZXing decode of a 720p luminance
frame is in the tens of milliseconds, so at five frames per second the analyzer uses well under a
fifth of one core, and the cashier UI thread is untouched because Compose only reads a state value.

## Flows

### Stage 1 as built

The interceptor runs in `MainActivity.dispatchKeyEvent`, and it consumes Enter or Tab only while a scan
sequence is still inside the timing window. A sequence that went stale is discarded instead of
swallowing the key, which is what the implementation found out the hard way: without that guard, a
lone Enter after slow typing was eaten and produced nothing. `ScannerPreferences` stores the on/off
switch in `SharedPreferences`, not in `Room`, so the backup payload format did not change. The
settings test area shows the last decoded code, which is what proves the whole chain on a real
device before a sale depends on it.

### Stage 2 as built

Camera capture runs on CameraX with the ZXing reader behind `BarcodeFrameReader`. The frame extraction
lives in `LuminanceFrame`, a pure object, so the padded-stride and short-buffer cases are unit tested
without a camera. The decoder is unit tested by rendering an EAN-13 and reading it back.

The surface is a full-screen dialog in the app's own language: live preview, 55% scrim with a clean
cut-out, a white finder frame drawn over the scrim (7:1 even against a pure white scene), the scanning
hint on an inverse-surface chip, a torch toggle, and manual entry one tap away. States covered:
permission request, permission blocked with a route to system settings, no camera hardware, camera
failure with retry, live scanning, unknown code, and success. Camera binding is tied to the surface
lifecycle and the torch is switched off when it closes.

Measured on release: APK 3.90 MB (was 3.16 MB), still well inside the 8 MB budget, and the app still
requests no `INTERNET` permission.

Hardening after the first field report, where a Le Minerale barcode read late or read wrong:

- The frame is rotated upright using `ImageProxy.imageInfo.rotationDegrees` before decoding. A portrait
  phone delivers a landscape buffer, so a horizontal barcode arrived rotated a quarter turn and ZXing
  had to find it with a slower fallback pass. `LuminanceFrame.rotateQuarterTurn` is a pure object, so
  the rotation maths and a rotated EAN-13 round trip are unit tested.
- Two matching reads are required before a code is accepted (`BarcodeVoteCollector`), which is what
  stops a single blurry frame from adding the wrong product.
- Autofocus is driven explicitly at the centre of the finder frame and re-triggered every 1.8 seconds,
  because close-range products were simply out of focus.
- `CODE_39` was dropped from the format list: it has no check digit and is the usual source of false
  positives on a busy label. Retail numeric codes outside 8 to 14 digits are rejected as implausible.
- Analysis prefers the closest supported resolution at or below 1280x960 rather than 1280x720, because
  in portrait the short axis carries the barcode's horizontal detail.
- If nothing is read after six seconds the surface adds a line telling the cashier to move closer, hold
  still, or turn the light on, so a slow read does not look like a frozen screen.

**Flow 1, cashier camera scan.** Cashier taps the scan action in the search row, the scan surface
opens and asks for the camera on first use, the preview shows a finder frame and one instruction
line, a barcode is recognised, the item is added with the existing rules, and a confirmation strip
inside the surface names the product and the quantity now in the cart. The surface stays open so the
cashier can keep scanning; closing is one tap or system back.

**Flow 2, cashier hardware scan (scanner mode).** With a USB or Bluetooth HID scanner paired, the
scanner types the code and sends Enter or Tab. `MainActivity.dispatchKeyEvent` forwards events to
`HardwareScannerInput`, the decoder assembles the code, and `scanEvents` fires. The cashier never
touches the search field. This works with the scan surface closed, which is what PRD 6.1 asks for.

**Flow 3, product form code fill.** In the product form, the barcode field has a scan action. The
form swaps to the scanner, the found code fills the field, and the form returns. Nothing is added to
a cart, because there is no cart in this flow.

**Flow 4, scanner setup.** More, Settings, Scanner explains both methods in a short block, shows
whether the camera is available, offers the manual-entry test field so the owner can confirm a HID
scanner works before a real sale, and holds the hardware scanner mode switch.

## Screens and components

**Entry points.** Cashier search row: `[search field] [scan] [filter]`. The scan action is its own
44dp+ tap target with the icon and label "Pindai", not an icon hidden inside the field, because the
field's trailing slot is already used by clear-text. Product form: a scan action as the barcode
field's trailing icon, because there the code goes into that field and nowhere else.

**Scan surface anatomy**, top to bottom:

1. Header bar on a solid surface: title "Pindai barcode", close action.
2. Camera preview, square or 4:3, inside the app's large radius. A scrim of 55% black covers the
   preview, with a clear cut-out as the finder frame so the live image shows only where a barcode
   belongs. A single white stroked frame, radius from the shape system.
3. One instruction line on a solid surface under the preview: "Arahkan kamera ke barcode produk".
   Never text floating over the preview, for the contrast reason in the accessibility section.
4. Action row on the solid surface: torch toggle, "Ketik manual" fallback.
5. Confirmation strip that appears in place after each successful scan: product name, unit price,
   and the resulting cart quantity. This is the "immediate feedback without interrupting the
   workflow" from PRD 6.1.

**Manual entry fallback.** A one-line field that accepts a code and runs the same lookup. It exists
because a damaged label, a curved surface, or a worn barcode is normal in a small shop, and because
it keeps the sale moving when the camera will not focus.

**No animated laser line.** The finder frame is static. An animated scan line is the cliché of every
scanner screen, it communicates nothing the static frame does not, and a perpetual loop violates
R-19 and the declared MOTION 1 dial.

## States

Every state must exist and say what happened and what to do next (R-27). Nothing may be colour-only.

| State | Surface shows | Action offered |
| --- | --- | --- |
| Permission not yet asked | Short reason: the camera is used only to read barcodes on this device | "Izinkan kamera" |
| Permission denied once | The same reason, plus that the feature needs it | "Coba lagi" |
| Permission permanently denied | That Android will not ask again, and where to change it | "Buka pengaturan aplikasi" |
| No camera hardware | That this device has no camera, and that a HID scanner still works | "Ketik manual", "Tutup" |
| Camera unavailable or held by another app | That the camera is busy | "Coba lagi", "Ketik manual" |
| Scanning, nothing found yet | Live preview with the finder frame | none, this is the working state |
| Code read but not in the catalogue | The code itself, verbatim, so the cashier can check it | "Cari manual", "Tutup" |
| Code read and added | Product name and new cart quantity | none, keep scanning |
| Camera error | What failed, in plain language | "Coba lagi", "Ketik manual" |

The last two rows of copy matter: "barcode tidak dikenali" alone is a dead end, so the design always
shows the code and offers the next step.

## Accessibility

- Contrast is deterministic because **no text sits on the camera preview**. Every label lives on a
  solid surface: measured 17.45:1 in the light theme (`#111C18` on `#FFFFFF`) and 15.28:1 in the
  dark theme (`#ECF2EF` on `#151C19`). Secondary text on the bar is 6.60:1.
- The white finder frame over the 55% scrim is 7.00:1 even against a pure white scene, which is the
  worst case the rule asks us to test. At 45% it would fall to 4.74:1, so 55% is the specified value.
- Torch icon on the bar: 7.97:1 when active, 6.60:1 when inactive, both above the 3:1 bar for a
  control.
- Feedback is never colour-only: a found code produces a text strip, a changed cart line, and a
  haptic `CONFIRM`. An unknown code produces text plus a haptic `REJECT`.
- Keyboard and TalkBack: the scan surface is reachable and closable by system back, every action is
  labelled, the preview is `contentDescription = null` because it is not information, and focus
  moves to the confirmation strip after a successful read so a screen reader announces the result.
- Touch targets: every action in the surface is at least 44dp, including the torch and close.

## Anti-slop decisions and reasons (R-31)

| Decision | One-line reason |
| --- | --- |
| Modal scan surface, no navigation | A sale in progress must not be abandoned to scan one item. |
| Static finder frame, no laser animation | The frame already says where to aim; a loop adds motion with no information and breaks MOTION 1. |
| Scrim at 55% black with a cut-out | It guarantees the frame reads at 7:1 over any camera scene, including a white one. |
| Text only on solid surfaces | Contrast over a live image is unpredictable, so the design removes the dependence instead of hoping. |
| Torch off by default | Most scans happen in daylight; a torch default costs battery for no benefit. |
| Surface stays open after a scan | Adding several items is one continuous task, and closing per item would add a tap each time. |
| Manual entry always visible | A worn or curved label is normal in a shop, and the sale must not stop for it. |
| Haptics, no custom sound | Confirmation without looking at the screen, and no new audio asset to invent or ship. |
| Reuse `addScannedCode` | Stock, quantity step, and unit-barcode rules already live there; a second path would drift. |
| No new palette, typeface, or shape | The capture surface is part of the product, not a separate app with its own identity. |

Explicitly rejected: animated laser line, glow on the frame, pulsing status dot, "AI Powered" style
badge, emoji in any label, and a fake terminal or illustration anywhere in the flow.

## Explicitly out of scope

Shelf-label printing (PRD 7.8, separate feature), batch or continuous multi-barcode capture,
OCR of price tags, decoding from a gallery image (the QRIS flow already covers image import), and
any server-side or cloud decoding.

## Privacy

Camera frames are analysed on device and discarded immediately. No frame is stored, and nothing is
transmitted. The app still does not request `INTERNET`, so this stays true by construction rather
than by promise. The privacy policy needs one added line about the camera, and the Play Data Safety
form stays at "no data collected" because camera frames are processed locally and not retained.

## Risks

| Risk | Mitigation |
| --- | --- |
| Release APK grows past the 8 MB target | Measure right after adding the dependency; exclude unused ML Kit assets; fall back to ZXing if the measurement breaks the budget. |
| Repeated callbacks add the same item many times | Per-code debounce window inside the analyzer, plus the existing stock cap in `addScannedCode`. |
| HID interceptor swallows normal typing | The decoder only completes on a fast character run followed by Enter or Tab, and settings carries an off switch. |
| Camera held by another app | An explicit state with retry and manual entry, never a blank preview. |
| Cheap tablets without a camera | `uses-feature` marked not required, the scan action hides when no camera exists, HID stays available. |
| ML Kit version conflicts with `compileSdk 36` | Verify at implementation and record the resolved versions. |

## Test plan

Unit: barcode normalisation (trim, whitespace, leading zeros preserved), debounce window, permission
state mapping, and `addScannedCode` reuse for a code arriving from the camera source. The existing
`KeyboardWedgeScanDecoderTest` stays as the HID decoder's coverage.

Instrumented: the scan surface renders each state without a camera, the permission rationale appears
on first use, the product form swaps to the scanner and returns with the field filled, and the
scanner settings screen renders.

Manual on device: a real EAN-13 and a printed Code-128, a bright white scene (to confirm the scrim),
a dim scene with the torch, the permission denial path including "do not ask again", and the HID
path without a physical scanner by sending fast keystrokes plus Enter through `adb`.

Gate: after implementation, measure the release APK and record the number in this document.

## Decisions

Resolved on 19 October 2026. The owner delegated these to the recommendations in this document, so the
reason for each is recorded here (R-31).

1. **Engine**: CameraX plus the existing `zxing.core`, with bundled ML Kit behind the same interface as
   the fallback. Reason: it keeps the added weight near 1 MB, keeps a decoding model out of memory on a
   2 GB device, and reuses a decoder the app already ships. The swap stays one class, one dependency
   line, and one line in the container. PRD 7.5 was updated to match.
2. **After a successful scan in the cashier**: the surface closes. Implementation revised this from
   "stay open": the surface is a full-screen camera dialog, so a open surface hides the very cart the
   cashier needs to verify, and the existing cashier message already names the product that was added
   ("Kopi Susu ditambahkan"), which is stronger feedback than echoing the code. Reopening is one tap.
   The scanning hint chip inside the preview replaces the planned confirmation strip.
3. **Hardware scanner mode**: always on, with an off switch in Settings. Reason: the decoder only
   completes on a fast keystroke run that ends in Enter or Tab, so ordinary typing is unaffected, and
   the cashier should never have to arm the scanner in the middle of a sale.
4. **Feedback**: haptics only, `CONFIRM` on a read and `REJECT` on a miss. Reason: confirmation without
   looking at the screen, and no new audio asset to invent or ship.
5. **Scan action placement**: its own labelled button next to the search field, not an icon inside it.
   Reason: the field's trailing slot already carries clear-text, and a distinct action deserves a
   distinct tap target.

Not a preference, a measurement: whether the engine stays on ZXing is decided by the read-rate gate,
not by anyone's taste.

## Delivery Gate at design stage

Design-stage report. Items that can only be proven by running code are marked as implementation
gates, not claimed as passing here.

- R-02 PASS: no em dash in this document or in the specified copy.
- R-03 PASS by design: the scan surface uses the window size classes already in the app, phone
  bottom-sheet shape and tablet panel, and no fixed widths.
- R-25 PASS by measurement: 17.45:1 and 15.28:1 for text on the solid bars, 7.00:1 for the finder
  frame over the worst-case scene, 7.97:1 and 6.60:1 for the torch states.
- R-19 PASS: no animation except press states, matching MOTION 1; no endless loop.
- R-26 PASS by design: every action in the state table has a real outcome, and no control exists
  without a destination.
- R-27 PASS by design: the state table covers first run, permission denied both ways, no hardware,
  camera busy, not found, success, and error, each with cause and next action.
- R-29 PASS: no new colour is introduced; the surface uses existing tokens.
- R-31 PASS: every decision above carries its one-line reason.
- R-34 PASS by design: every colour cited is a theme token, so both themes inherit the measured
  ratios.
- R-35 STAGE 1 PASS: build, 483 unit tests, 45 instrumented tests, and a device click-through of
  the settings switch, the HID injection, the cashier dialog, and the product form scan action.
- R-35 STAGE 2: build, 492 unit tests, 45 instrumented tests, and a device click-through of the
  permission flow, the live preview, the torch toggle, manual mode, and the settings states. The
  release size is recorded below. Still open: a physical barcode read.
- Size gate PASS: release APK 3.90 MB against the PRD 9.2 budget of 8 MB. CameraX cost about 0.74 MB
  because the decoder was already in the app; bundled ML Kit would have cost an estimated 2.5 to 3.5 MB
  for the same job.
- Read-rate gate NOT RUN: on your own product labels, at least 19 of 20 first-try reads must succeed
  within two seconds. This needs a physical device aimed at a real barcode, which this setup cannot do
  because there is no second screen to hold the barcode. Below the bar, switch the reader implementation
  to bundled ML Kit and re-measure the size.
- R-37 PASS: direction taken from the PRD section 5 and the implemented token system, so this is not
  a draft without direction.
- R-38 PASS: no invented metric, asset, or claim appears in this design.
