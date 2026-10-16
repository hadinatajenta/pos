# UMKM POS Pro

Offline-first point-of-sale app for Indonesian small businesses (UMKM). Built with Kotlin, Jetpack
Compose, Material 3, and Room.

![App icon](docs/mocks/icon.jpeg)

## Features

Cashier
- Product search, cart, quantity and price adjustment, line and cart discounts, tax, and explicit
  currency rounding rules
- Payment methods: cash, QRIS, and bank transfer, with change calculation
- Receipt sharing as text
- Offline by default: no account, no sign-in, and no network access (the app does not request the
  `INTERNET` permission)

Catalogue and stock
- Products with category, unit, cost price (HPP), selling price, and tier pricing
- Optional product photo per product
- Stock tracking with available, low-stock, and out-of-stock states

Records
- Sales reports per date range with a sales chart, payment-method breakdown, and drill-down to
  individual transactions
- CSV and Excel (`.xlsx`) export written to the device `Download` folder
- Returns ledger, operating expenses, and customer debts with partial payments

## Requirements

- Android 6.0 (API 23) or newer
- JDK 17 and Android SDK 36 for building

## Build

```bash
./gradlew assembleDebug        # debug build, package id.umkm.pos.pro.debug
./gradlew testDebugUnitTest    # unit tests
./gradlew assembleRelease      # signed release APK
./gradlew bundleRelease        # signed release AAB for Google Play
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

## Demo data

Debug builds only: open **Lainnya → Data Demo** to insert sample products and transactions.

## Not implemented yet

These **Lainnya** entries are visible but marked *Segera hadir*:

- Printer Struk (thermal printer setup)
- Scanner Barcode (camera or USB/Bluetooth scanner)
- Cadangkan & Pulihkan (local and Google Drive backup and restore)
- Keamanan & PIN (PIN or biometric lock with offline recovery code)
- Pengaturan (store identity, tax, and rounding configuration; both are currently fixed)

Also pending: QRIS static image display, receipt PNG sharing, bulk shelf-label printing, manual
stock adjustment, and ads.

## Project layout

```
app/src/main/java/id/umkm/pos/pro/
  core/      models, contracts, use cases
  data/      Room database, repositories, CSV/XLSX writers, image store
  feature/   reports, export, printing
  ui/        Compose screens, components, theme
docs/        product requirements, design mockups, publishing guide
```

## Publishing

See [docs/playstore-release.md](docs/playstore-release.md) for the Google Play release process and
[docs/privacy-policy.html](docs/privacy-policy.html) for the privacy policy.

## License

© 2026 Hadinata Jenta. All rights reserved.

Navigation icons are from [Lucide](https://lucide.dev), used under the ISC License.
