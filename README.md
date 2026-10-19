# UMKM POS Pro

Offline-first point-of-sale app for Indonesian small businesses (UMKM). Built with Kotlin, Jetpack
Compose, Material 3, and Room.

![App icon](docs/mocks/icon.jpeg)

## Features

Cashier
- Product search by name, SKU, or barcode, with quantity and price adjustment, line and invoice
  discounts, tax, and explicit currency rounding rules
- Tiered wholesale pricing, negotiated price overrides, and multi-unit products
- Payment methods: cash, QRIS, bank transfer, split payment, and customer debt (kasbon), with
  change and remaining-balance calculation
- QRIS: the merchant's static payload is converted on-device into a dynamic QR with the exact
  amount; no payment gateway is involved, so payment confirmation stays manual
- Receipt sharing as text, plus a receipt preview and test print from Settings
- Offline by default: no account, no sign-in, and no network access (the app does not request the
  `INTERNET` permission)

Catalogue and stock
- Products with category, unit, cost price (HPP), selling price, tier pricing, and optional photo
- Multi-unit stock tracking with available, low-stock, out-of-stock, and oversold states

Customers and transactions
- Customer records with an optional contact picker, debts with partial payments and due dates
- Transaction history with filters and detail drill-down, plus a returns ledger

Reports
- Total sales, estimated net profit, HPP, transaction count, and average transaction for the
  selected range
- Sales chart and payment-method breakdown
- CSV and Excel (`.xlsx`) export through the system file picker, with a clear completion card that
  shows the saved file name and location plus **Open** and **Share** actions

Settings
- Store profile (store name, cashier name), tax rate, and rounding increment
- Bluetooth thermal printer: connection status, paired-device list, paper width, test print, and
  receipt preview
- Static QRIS image upload and replacement
- Backup and restore: export every database table to a single JSON file in `Download` and
  restore it back, with validation and a confirmation before data is replaced. Image files
  (product photos and the QRIS image) are referenced by path but not bundled, so copy them
  separately when moving to a new device
- App language: Indonesian, English, and Simplified Chinese. The interface, receipts, printer
  output, and report exports follow the selected language
- About and privacy: app version and an in-app link to the privacy policy

## Not implemented yet

- **PIN, biometrics, and offline recovery**: the `security_store` table and repository exist, but
  there is no setup or verification UI, no PIN hashing, and no enforcement on sensitive actions.
- **Barcode scanning**: stage 1 is in. A USB or Bluetooth HID scanner now adds items without
  touching the screen, More then Settings has a **Pemindai Barcode** page with an on/off switch
  and a live test area, and both the cashier and the product form offer manual code entry.
  Camera scanning is still missing: no CameraX dependency, no `CAMERA` permission, no capture UI.
  Design and engine decision: [docs/design/barcode-scan.md](docs/design/barcode-scan.md).
- **Returns exchange flow**: `ExchangeSettlement` and `settleExchange` exist with tests, but the
  returns UI does not use them.
- **Cashier shifts**: `CashierShiftEntity` and `ShiftRepository` exist without any UI.
- **Receipt PNG sharing**: sharing is `text/plain` only; the printed receipt does include the QRIS
  image when configured.
- **Google Drive backup** (the JSON backup is local only), **bulk shelf-label printing**, and
  **AdMob**: not implemented. `AdController` is wired to `NoAdsController`.
- **Baseline Profiles and cold-start measurement**: not configured or measured.

See [docs/prd-gaps.md](docs/prd-gaps.md) for the full audit against the PRD.

## Requirements

- Android 6.0 (API 23) or newer
- JDK 17 and Android SDK 36 for building

The default `java` on the development machine is JDK 8, so export JDK 17 before running Gradle:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
```

## Build

```bash
./gradlew assembleDebug              # debug build, package id.umkm.pos.pro.debug
./gradlew testDebugUnitTest          # unit tests
./gradlew assembleRelease            # signed release APK
./gradlew bundleRelease              # signed release AAB for Google Play
./gradlew connectedDebugAndroidTest  # instrumented tests, needs a device
./gradlew lintRelease                # lint; currently fails, see docs/prd-gaps.md
```

Release signing reads `keystore.properties` from the repository root:

```properties
storeFile=/absolute/path/to/upload-keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

The file is gitignored. Without it the release build stays unsigned and cannot be uploaded to
Google Play.

Install a build on a connected device:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Verification status

Verified on 19 October 2026 at commit `5f1bc73`:

| Check | Result |
| --- | --- |
| `testDebugUnitTest` | 469 tests, 0 failures, 62 classes |
| `connectedDebugAndroidTest` | 40 tests pass on a physical tablet, including the JSON export/import round trip |
| `assembleRelease` | Builds with R8 and resource shrinking |
| `bundleRelease` | Builds a signed AAB |
| `lintRelease` | 11 errors (9 `NewApi` false positives from a guarded helper, 2 `ProduceStateDoesNotAssignValue`), 38 warnings |

The release APK was installed and smoke-tested on a tablet, and the report export, JSON backup
export/import, and the in-app privacy policy link were exercised on a physical phone. Bluetooth
printer and HID scanner paths have not been exercised on physical hardware.

## Demo data

Debug builds only: open **Lainnya → Data Demo** to insert sample products and transactions. The
section is hidden in release builds by `BuildConfig.DEBUG`.

## Project layout

```
app/src/main/java/id/umkm/pos/pro/
  app/       application container and composition root
  core/      models and contracts (frozen shared API)
  data/      Room database, repositories, image store, demo seed
    backup/  JSON backup payload, codec, and export/import service
    file/    file sinks and file name helpers shared by exports and backups
  domain/    pricing, accounting, QRIS payload, use cases
  feature/   reports and export
  hardware/  Bluetooth/USB printer, HID scanner, receipt rendering
  monetization/admob/  AdController implementations (currently NoAdsController)
  ui/        Compose screens, components, theme, localization catalogues
docs/        product requirements, architecture, implementation status, publishing guide
```

## Documentation

- [docs/prd.md](docs/prd.md) — product requirements, the source of truth
- [docs/architecture.md](docs/architecture.md) — module layout and frozen contract decisions
- [docs/prd-gaps.md](docs/prd-gaps.md) — implementation status against the PRD
- [docs/design/barcode-scan.md](docs/design/barcode-scan.md) — barcode scanning design, frozen and not implemented
- [docs/dev-environment.md](docs/dev-environment.md) — toolchain and build constraints
- [docs/playstore-release.md](docs/playstore-release.md) — Google Play release process
- [docs/privacy-policy.html](docs/privacy-policy.html) — privacy policy

## License

© 2026 Hadinata Jenta. All rights reserved.

Navigation icons are from [Lucide](https://lucide.dev), used under the ISC License.
