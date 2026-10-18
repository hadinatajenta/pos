# Google Play release guide — UMKM POS Pro

Owner labels used below:

- **[YOU]** — only the account holder can do this (identity, payment, legal agreements, final publish).
- **[ME]** — I can do this on this machine or through the GitHub API.
- **[BOTH]** — I prepare it, you review or click the final confirmation.

## Facts that shape the plan

| Fact | Consequence |
| --- | --- |
| New apps must target Android 16 (API 36) since 31 August 2026 | Already done: `compileSdk`/`targetSdk` are 36, verified with `aapt2 dump badging` |
| Personal Play accounts created after 13 November 2023 need a closed test with **at least 12 testers opted in continuously for 14 days** before production access | This is the long pole, roughly 3 weeks end to end. Internal testing does **not** count toward it. Organization accounts (D-U-N-S) are exempt |
| Play App Signing: Google stores the app signing key, you upload with an upload key | The upload key already exists at `~/.keystores/umkm-pos-upload.jks` with credentials in `keystore.properties` (gitignored). **Back both up — losing them blocks future updates** |
| Package name `id.umkm.pos.pro` becomes permanent | No change needed, it is already the application id |
| Indonesia participates in Android developer verification (deadline was 30 September 2026) | Check the Play Console home page for a registration prompt for your developer account and package name |
| The app does not request `INTERNET` and collects nothing | The Data Safety form is the simple "no data collected, no data shared" path |

## Phase 0 — build artifacts

- Signed release bundle: `app/build/outputs/bundle/release/app-release.aab`
  (versionCode 1, versionName 1.0.0, ~5.7 MB)
- Signed release APK for direct install: `app/build/outputs/apk/release/app-release.apk`
  (~3.1 MB)
- Upload keystore and `keystore.properties` (gitignored). The signing certificate is
  `CN=UMKM POS Pro`.
- `targetSdk`/`compileSdk` 36, launcher icon, adaptive icon, round icon
- Privacy policy published at <https://hadinatajenta.github.io/pos/privacy-policy.html>

The artifacts above were rebuilt on 19 October 2026 from commit `5f1bc73`. The earlier GitHub
release `v1.0.0` predates the localization and settings work and should not be uploaded; rebuild
and re-tag before publishing.

## Current verification status (19 October 2026)

| Check | Result |
| --- | --- |
| Unit tests | 469 pass |
| Instrumented tests | 40 pass on a physical tablet, including the JSON export/import round trip |
| Release build | R8 and resource shrinking succeed |
| `lintRelease` | **Fails: 11 errors, 38 warnings** |
| Hardware | Bluetooth printer and HID scanner not yet exercised on physical hardware |

Do not upload until `lintRelease` is green and the printer and scanner paths are verified on real
hardware. See `docs/prd-gaps.md` for the exact fixes and the remaining feature gaps (backup,
PIN, camera scanning).

## Phase 1 — Account setup (only you)

1. **[YOU]** Create a Google Play Developer account at
   <https://play.google.com/console/signup>. One-time 25 USD fee, plus identity verification with a
   government ID. Verification can take 1–7 days.
2. **[YOU]** Decide the account type:
   - **Personal** — simpler to create, but the 12 testers / 14 days closed test gate applies.
   - **Organization** — requires a D-U-N-S number (free from Dun & Bradstreet), skips the testing
     gate. Choose this only if a registered business entity represents the app.
3. **[YOU]** Complete Android developer verification if the Console asks for it.
4. **[YOU]** Accept the Play Console agreements, then **Create app**: name `UMKM POS Pro`, default
   language Indonesian, type App, free.
5. **[YOU]** When first asked for the package name, use `id.umkm.pos.pro`.
6. **[BOTH]** Keep Google-managed Play App Signing enabled (the default). Upload the AAB signed with
   the existing upload key; do not generate a new key in the Console.

## Phase 2 — Store listing (I prepare, you approve)

Deliverables and exact requirements:

| Asset | Requirement | Owner |
| --- | --- | --- |
| App name | max 30 characters, e.g. `UMKM POS Pro — Kasir & Stok` | [ME] draft |
| Short description | max 80 characters | [ME] draft |
| Full description | max 4000 characters | [ME] draft |
| App icon | 512 × 512 px, 32-bit PNG, max 1 MB | [ME] generate from `docs/mocks/icon.jpeg` |
| Feature graphic | 1024 × 500 px, PNG or JPEG, no alpha, max 15 MB | [ME] generate |
| Phone screenshots | 2–8 images, 320–3840 px, aspect between 16:9 and 9:16 | [ME] capture from the S24 FE |
| 7-inch tablet screenshots | 2–8 images (required to be shown as tablet-optimized) | [ME] capture from the Legion Tab |
| 10-inch tablet screenshots | 2–8 images | [ME] capture from the Legion Tab |
| Promo video | optional YouTube link | [YOU] |
| Contact email | public on the listing, e.g. `hadinatajenta122@gmail.com` | [YOU] confirm |
| Website | optional, `https://hadinata.vercel.app` | [YOU] confirm |
| Privacy policy URL | required: `https://hadinatajenta.github.io/pos/privacy-policy.html` | [ME] published |

Category: **Business**. Tags: point of sale, cashier, inventory, small business.

## Phase 3 — App content declarations (only you, answers prepared by me)

**App content** in the Console, with the answers this app supports:

| Section | Answer |
| --- | --- |
| Privacy policy | The URL above |
| App access | All functionality is available without restrictions (no login, no paywall) |
| Ads | No (the current version contains no ads) |
| Content rating | Fill the IARC questionnaire as a business/utility app: no violence, no user-generated content, no gambling, no location sharing. The app records business transactions only |
| Target audience | 18 and over (business tool) |
| News app | No |
| Data safety | No data collected, no data shared. The app has no `INTERNET` permission; everything is stored on the device. Contacts are read on the device only when the merchant taps "Pilih dari kontak" for one specific contact (`READ_CONTACTS`), and nothing leaves the device |
| Government app | No |
| Financial features | None of the listed features apply. The app records sales locally, it does not process payments or offer lending, banking, or investment services |
| Account deletion | Not applicable, the app has no accounts |

The Settings screen now exists and contains an in-app link to the privacy policy (**Pengaturan →
Tentang & Privasi**). Republish `docs/privacy-policy.html` to GitHub Pages so the hosted page
matches the current effective date and content.

## Phase 4 — Upload and tracks

1. **[ME]** Provide the AAB and release notes ("What's new", max 500 characters per track language).
2. **[YOU]** Upload to **Internal testing** first: drag the AAB into the Console, add your own email
   as a tester, install through the opt-in link, and smoke test on both the phone and the tablet.
   Internal testing has no review step and up to 100 testers.
3. **[YOU]** Promote the same bundle to **Closed testing**: create a tester list with at least 12
   real Google accounts (recruit 14–15 to absorb drop-off). Testers must stay opted in for 14
   consecutive days and actually use the app. Review for a new app can take a few days, so send the
   opt-in link only after the release is available.
4. **[BOTH]** Recruit testers; I can prepare the invitation message and the opt-in link text.
5. **[YOU]** Apply for production access on the Dashboard once the 12 testers have 14 continuous
   days. Answer the questionnaire honestly, including what testers reported and what you changed.
   Review usually takes seven days or less.
6. **[YOU]** Once approved: **Production → Create release → upload the same AAB → Start rollout**.
   The first production review for a new app can take up to 7 days. After this, updates go live
   faster and no longer need the closed-test gate.

## Phase 5 — Automation for later releases

You need two things once per project:

1. **[YOU]** In Play Console → **Setup → API access**, link a Google Cloud project and create a
   **service account**, then grant it *Release to testing tracks* (and later *Release to production*)
   in **Users and permissions**. Download its JSON key file.
2. **[ME]** Then I can automate every future upload. Two options:

   **Option A — Google Play Gradle plugin** (no extra toolchain, added to the build):

   ```bash
   ./gradlew publishReleaseBundle
   ```

   **Option B — fastlane supply** (needs Ruby + fastlane installed):

   ```bash
   fastlane supply --aab app/build/outputs/bundle/release/app-release.aab \
     --track internal --json_key ~/.keys/play-service-account.json
   ```

3. **[ME]** GitHub Actions workflow on tag push: build the signed AAB from secrets, upload to the
   Play internal track, and attach the artifacts to the GitHub release. This needs the keystore and
   the service account JSON stored as repository secrets; I can add them through the GitHub API if
   you hand me the JSON key file and the keystore password.

## What I cannot do

- Create, pay for, or verify your Play developer account and legal agreements.
- Submit the Data Safety, content rating, and target audience forms, because they require the
  account holder's declaration.
- Publish to production on your behalf. The upload can be automated, the final rollout decision
  stays yours.

## Release checklist

- [ ] Play developer account verified
- [ ] App entry created with package `id.umkm.pos.pro`
- [ ] Store listing assets uploaded (icon, feature graphic, screenshots)
- [ ] Privacy policy URL, contact email, and website set
- [ ] App content forms submitted
- [ ] AAB uploaded to internal testing and smoke tested on device
- [ ] Closed testing started with 14+ testers
- [ ] 14 continuous days completed
- [ ] Production access requested and approved
- [ ] Production release rolled out
