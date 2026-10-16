# UMKM POS Pro

## Product Requirements Document

**Version:** 1.2 — Responsive UX & Visual Reliability  
**Target implementation agent:** Deepseek V4.1 Flash (High Effort) with five parallel sub-agents  
**Development environment:** Antigravity IDE, Android SDK command-line tools, Gradle, ADB, scrcpy, and a physical Android device  
**Product promise:** **Free forever. No subscriptions. No paid feature tiers. No watermarks.**

> **Product direction:** A lightweight, offline-first POS that feels calm, clear, and fast. The interface should help a busy shop owner complete a sale with as few decisions and taps as possible. Modern visuals must support usability—not compete with it.

---

## 1. Executive Summary

UMKM POS Pro is an Android point-of-sale application for Indonesian micro, small, and medium-sized businesses (UMKM). It is designed for low-end Android devices, works offline for core operations, and provides cashier, inventory, customer-debt, reporting, receipt-printing, and backup capabilities without subscription fees.

The app targets a release size below **8 MB** where feasible and must remain usable on devices with **2 GB RAM**. Monetization uses Google AdMob only in non-critical areas. Ads must never interrupt checkout, payment, or receipt printing.

This revision keeps the existing business rules and technical scope while strengthening the mobile/tablet layout contract. It requires clean layouts, modern icons, short labels, readable financial values, predictable navigation, progressively disclosed advanced options, and explicit viewport testing. The application must never show a clipped desktop layout inside a phone-sized window or display multiple navigation systems at once.

## 2. Product Vision and Experience Principles

### 2.1 Vision

Give small businesses an easy-to-trust POS that is fast at the counter, dependable without an internet connection, and respectful of the owner's data and time.

### 2.2 Experience principles

1. **Cashier-first:** The most frequent task—selling an item—must be the easiest task to start and finish.
2. **Clear before clever:** Use familiar navigation and short labels. Avoid decorative complexity, dense paragraphs, and unexplained jargon.
3. **One primary action per screen:** Make the next step obvious. Secondary actions should remain available without competing visually.
4. **Progressive disclosure:** Show essential information first; place advanced pricing, tax, backup, and staff options in a clearly named secondary sheet or section.
5. **Offline by default:** Core transactions, inventory changes, local reports, and local backups must work without a network connection.
6. **Data belongs to the merchant:** PIN recovery and backup must not lock the owner out of their business data.
7. **Quiet monetization:** Advertising must not interrupt the cashier flow or create empty layout gaps when offline.
8. **Accessible and forgiving:** Support scalable text, clear focus states, touch-friendly targets, useful error messages, and safe recovery from mistakes.

## 3. Product Scope and Priorities

### 3.1 MVP scope

- Product catalog, barcode lookup, stock tracking, and multi-unit products.
- Fast checkout with tiered wholesale pricing, negotiated price overrides, discounts, tax, and rounding.
- Cash, QRIS, transfer, split payment, and customer debt (kasbon).
- Debt tracking, partial payments, due dates, returns, and exchanges.
- Transaction history and profit/loss reporting with cost of goods sold (HPP).
- Bluetooth and USB OTG thermal printing, bitmap receipt rendering, and receipt sharing.
- Camera barcode scanning and physical USB/Bluetooth HID scanner input.
- Local export/import and Google Drive appDataFolder backup.
- PIN/biometric protection for sensitive actions with an owner recovery path.
- Responsive phone/tablet UI, light/dark themes, and adjustable text scaling.
- AdMob placements limited to the non-critical screens specified in this PRD.

### 3.2 Out of scope for the first release

- Mandatory cloud accounts or a mandatory internet connection for checkout.
- Subscription plans or paid feature tiers.
- Ads on cashier, cart, payment, or receipt-completion screens.
- A complex desktop/web application; the target is Android phone and tablet.

## 4. Information Architecture and Navigation

Use a small, predictable set of primary destinations. On phones, use a bottom navigation bar with clear icon-plus-label items:

- **Cashier** — the default destination for daily sales.
- **Products** — catalog, prices, units, barcode, and stock.
- **Customers** — customer records, debt, and payments.
- **Reports** — sales, profit, HPP, and transaction history.
- **More** — printer/scanner setup, backup, exports, staff/PIN, and settings.

Do not expose every feature as a top-level tab. Secondary functions belong in the relevant context or under **More**. On tablets, use a responsive navigation rail or split layout, with a **60:40** content/detail arrangement where it improves speed and visibility.

### 4.1 Responsive navigation contract — mandatory

Choose the navigation pattern from the **available app window size**, not from an assumed device model. Use Compose window size classes or an equivalent constraint-based approach.

- **Compact phone window:** show the bottom navigation bar only. The screen content occupies the full area above it. Do not render a permanent left navigation rail or desktop sidebar.
- **Expanded tablet/window:** use a navigation rail or side navigation only when there is enough width. Do not also show the phone bottom navigation at the same time.
- **Medium or split-screen window:** select one navigation pattern that fits the current width and keep content usable. Recalculate when the window resizes or the app enters split-screen mode.
- Keep the same destinations and navigation state across layout changes; changing the navigation presentation must not reset the current task, cart, search query, or unsaved form state.

**Explicitly prohibited layout failures:**

- A desktop or tablet layout squeezed into a phone-width window.
- A left rail/sidebar and bottom navigation displayed simultaneously without a deliberate, documented use case.
- Page titles, buttons, product cards, or form fields clipped by the left edge, right edge, system bars, or another panel.
- Content starting underneath a sidebar or appearing shifted off-center because navigation width was not accounted for.
- Horizontal scrolling of the entire screen to reach primary content.
- Fixed-width content panels that exceed the available window width.
- Oversized empty cards or blank panels that push useful content below the fold.

### 4.2 Navigation rules

- Keep the current destination visually obvious.
- Preserve search, filters, and relevant context when navigating back.
- Do not open multiple overlapping dialogs for one task.
- Use bottom sheets for quick edits on phones; use side panels or inline forms on tablets.
- Confirm destructive actions such as deleting products or clearing history, but do not add confirmation dialogs to routine reversible actions.

## 5. Visual Design System

### 5.1 Overall style

Use a **modern, simple, clean Material 3 interface** with the restraint of a well-designed productivity app. Keep the visual system consistent across cashier, stock, debt, reports, and settings.

- Calm neutral surfaces with one restrained primary accent color.
- Strong text/background contrast and clear typography hierarchy.
- Thin dividers only where they clarify grouping; avoid putting every item in a separate bordered card.
- Subtle corner radii and low-cost surface elevation; no heavy blur or expensive visual effects.
- Consistent spacing based on an **8 dp grid**.
- Use whitespace to separate tasks, not to waste space on low-end phones.
- Use short headings, concise helper text, and clear empty states.
- Default to the system theme, with both light and dark themes available.

### 5.2 Iconography

Use one consistent modern icon set compatible with Jetpack Compose and Material 3, such as Material Symbols Rounded or the project's established Material icons.

- Pair icons with text labels for primary navigation and important actions.
- Icon-only buttons are acceptable for widely recognized actions (back, close, search, overflow) when they have a tooltip/content description and a sufficient touch target.
- Do not mix emoji, unrelated icon packs, or multiple visual styles.
- Use icons to help scanning—not as decoration.

### 5.3 Typography and density

- Use a clear type scale for screen title, section heading, body, supporting label, and financial value.
- Keep paragraphs short; convert long explanations into concise helper text or progressive disclosure.
- Distinguish important totals using weight and spacing rather than many bright colors.
- Support font scaling from **100% to 135%** without clipping or hiding primary actions.
- Use tabular-number styling for prices, quantities, totals, and reports when available.

### 5.4 Components and actions

- One prominent primary action per screen or step (for example, **Charge**, **Save product**, or **Complete return**).
- Secondary actions use quieter button styles or overflow menus.
- Use full-width or clearly emphasized checkout actions that remain easy to reach.
- Keep touch targets comfortably sized and spaced.
- Use consistent field labels, inline validation, and helpful error states.
- Prefer concise status chips such as **Low stock**, **Unpaid**, or **Saved** over lengthy status sentences.
- Avoid decorative gradients, animated backgrounds, crowded dashboards, and excessive card borders.

### 5.5 Layout integrity and content density

- Use responsive containers and available-width constraints. Do not hard-code major screen widths or use absolute offsets to place core content.
- On phones, use a single content column. Cards, tables, and forms must fit the available width; convert wide tables into compact rows or stacked details when needed.
- Use consistent horizontal content padding. Titles, descriptions, and primary actions must align to the same content edge.
- Do not reserve desktop sidebar space when the sidebar is hidden, and do not allow a sidebar to overlap the main content.
- Keep content cards close to the height needed for their contents. Avoid large, empty dashboard cards that create excessive scrolling.
- Navigation bars, system bars, snackbars, sheets, and the on-screen keyboard must not cover fields, totals, or primary actions. Apply correct system-bar and IME insets.
- For scrolling screens, preserve a clear content start and end. Add bottom content padding when necessary so the last row or button can scroll fully above the navigation bar.
- Do not rely on color alone to communicate selection, stock status, errors, or payment state.

## 6. Key Screens and User Flows

### 6.1 Cashier screen — the primary workspace

The cashier screen must optimize for speed and confidence.

**Layout:**

- A compact top bar with store context and a clearly accessible scanner/search action.
- A searchable product list or grid with readable product names, prices, and stock cues.
- A cart summary that remains visible or is available in one obvious action on smaller phones.
- A prominent **Charge** action with the current total.
- Quantity changes directly in the cart; advanced price editing is available through a small, focused edit sheet.

**Rules:**

- Adding an item should provide immediate feedback without interrupting the workflow.
- A hardware barcode scanner must be able to add items without requiring the user to tap the search field each time when scanner mode is active.
- Keep the item count, subtotal, discount, tax (when enabled), and final total clear.
- Put negotiated price, wholesale tier details, and rarely used adjustments under an explicit **More options** action.
- Never show ads on the cashier, cart, payment, transaction-success, or receipt-printing flow.

**Checkout flow:**

1. Review cart and total.
2. Choose payment method and enter the amount received where relevant.
3. Confirm the transaction.
4. Show a compact success state with next actions: **Print receipt**, **Share receipt**, or **New sale**.

Avoid long success paragraphs and avoid automatically opening an ad after a transaction.

### 6.2 Product and inventory screens

- Provide clear search, barcode/SKU lookup, and category filters.
- Show product name, selling price, current stock, and a concise stock-status indicator.
- Keep **Add product** easy to find without dominating the screen.
- Use a focused product form with clearly grouped sections: **Basics**, **Pricing & units**, and **Stock alerts**.
- Advanced wholesale rules and unit conversions should be expandable rather than displayed as a dense form by default.
- Use inline validation and explain what needs correction in plain language.

### 6.3 Customers and debt (kasbon)

- Show customer name, outstanding debt, and next due date where available.
- Provide a quick **Record payment** action.
- Make partial payments and payment history easy to understand.
- Use clear states such as **Paid**, **Partially paid**, and **Overdue**; do not rely on color alone.
- Ad banners, if available, must be confined to the bottom of the debt list and must not cover data or actions.

### 6.4 Reports and transaction history

- Start with a small set of useful summaries: sales, estimated net profit, HPP, and transaction count.
- Make date range selection easy to find.
- Use clean lists and restrained charts; avoid dashboards packed with many small cards.
- Provide clear drill-down from summary to the transactions or items behind it.
- Make export actions easy to discover but visually secondary to the report itself.
- Support historical aggregation across years without an arbitrary time limit.

### 6.5 Backup, export, printer, and scanner setup

Keep device and data utilities under **More** with simple status rows and clear actions.

- Printer setup should show connection status, selected printer, test-print action, and reconnect action.
- Scanner setup should explain camera scanning and hardware scanner support briefly.
- Backup must distinguish **Local backup** from **Google Drive backup**.
- Local backup must remain available when offline and must not be blocked by an unavailable ad.
- Show file type and destination before export; show a clear success or failure state afterward.

### 6.6 Setup, cashier PIN, and recovery

- Keep initial setup short and allow the owner to reach the cashier quickly.
- Explain PIN protection only when asking the user to enable it.
- Use device biometrics where available and explicitly enabled by the owner.
- Always provide the documented offline recovery path; never create an unrecoverable owner lockout.

## 7. Functional Requirements

### 7.1 Product pricing, wholesale tiers, and units

Each product stores HPP (cost of goods), a base retail price, stock in a base unit, unit conversions, and optional quantity-based wholesale prices.

Examples of unit conversions include 1 bundle = 10 pieces and 1 carton = 40 pieces. The product model must support variant/unit names, conversion factors, custom barcodes, and wholesale thresholds.

**Unit-price priority:**

1. If a negotiated price override exists for the cart line, use it.
2. Otherwise, use the eligible wholesale tier with the highest minimum quantity that does not exceed the purchased quantity.
3. Otherwise, use the base retail price.

All price overrides and wholesale pricing decisions must be visible in the cart so the cashier can verify the applied price.

### 7.2 Accurate transaction calculations

Use integer `Long` values for Rupiah amounts to avoid floating-point currency errors. Quantity and unit-conversion calculations may use the appropriate numeric representation, with explicit rounding rules at currency boundaries.

Let:

- `Q` = item quantity in the selected unit.
- `P_unit(Q)` = negotiated, wholesale, or base price selected by the rules above.
- `D_item` = item-level nominal discount.
- `D_invoice` = invoice-level discount.
- `R_tax` = configured tax rate as a percentage.

**Item subtotal**  
`ItemSubtotal = Q × P_unit(Q) − D_item`

**Invoice net before tax**  
`InvoiceNet = sum(ItemSubtotal for all lines) − D_invoice`

**Tax**  
`Tax = floor(InvoiceNet × R_tax / 100)`

**Grand total**  
`GrandTotal = InvoiceNet + Tax + RoundingAdjustment`

**Estimated net profit per transaction**  
`NetProfit = InvoiceNet − sum(Q × UnitConversionFactor × BaseHPP for each line)`

The implementation must have unit tests for tier selection, negotiated prices, multi-unit conversion, discounts, tax, rounding, and profit calculations. The UI must show discount, tax, rounding, and final total distinctly when those values apply.

### 7.3 Payments, debt, returns, and exchanges

Support the following payment methods:

- Cash
- QRIS
- Bank transfer
- Split payment
- Kasbon (customer debt)

Support partial payment, outstanding balance, due date, installment/payment history, returns, and exchanges. Return/exchange logic must correct stock and HPP consistently and preserve the transaction audit trail.

The payment UI should show **Total**, **Paid**, **Change**, and **Remaining balance** only when relevant. Do not show irrelevant fields for a payment method.

### 7.4 Reports and exports

- Aggregate sales, HPP, and profit/loss across all available years.
- Export CSV and lightweight XLSX without Apache POI.
- Export/import local SQLite `.db` and JSON files.
- Generate receipt images as PNG and support sharing them through Android intents, including WhatsApp where installed.
- Generate a receipt that includes a static QRIS image when configured.

### 7.5 Thermal printers and barcode scanners

**Thermal printers:**

- ESC/POS over Bluetooth RFCOMM via `BluetoothSocket`.
- USB OTG via `UsbManager`.
- Support text and bitmap/image receipt modes, including low-cost printers with poor font rendering.
- Store the selected printer's MAC address and attempt auto-reconnect.
- Reference-width assumptions: 58 mm ≈ 32 characters; 80 mm ≈ 48 characters. Validate layout against actual printer behavior.

**Barcode scanners:**

- Camera scanning with Google ML Kit Barcode Scanning.
- Hardware USB OTG and Bluetooth HID input through a keyboard-event interceptor.
- Handle unexpected detach events gracefully. Removing a USB scanner or printer must not crash the application.

### 7.6 Backup and data ownership

Provide local export/backup formats: SQLite `.db`, `.json`, `.csv`, and `.xlsx` where relevant. Support synchronization to the user's personal Google Drive using Drive API v3 `appDataFolder`.

- Clearly distinguish local backup from cloud backup.
- Keep local backup available offline.
- Do not encrypt, hide, or hold local backup files hostage behind the cashier PIN.
- Give the user clear confirmation when a backup succeeds or fails.
- Explain that cloud backup requires connectivity and appropriate account authorization.

### 7.7 PIN, biometric access, and anti-lockout

PIN protection may restrict sensitive actions, such as deleting transaction history or allowing staff to view HPP. It must not prevent the owner from accessing their business data permanently.

Provide:

- Device biometric recovery when available and enabled.
- An offline recovery code generated during PIN setup, with an option to print or save it securely.
- Clear recovery instructions that do not depend on internet access.

### 7.8 AdMob policy — zero friction at checkout

| Screen / feature                                              | Ad treatment                               | Behavior                                                                                                                        |
| ------------------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------- |
| Cashier, cart, payment, transaction success, receipt printing | **No ads**                                 | Never load banner or interstitial ads in this flow.                                                                             |
| Debt list and stock management                                | Adaptive banner may appear                 | Place at the bottom, outside the content and action area. Hide the entire ad container when offline or when no ad is available. |
| Excel export, Google Drive backup, bulk shelf-label printing  | Optional rewarded video per product policy | Use a short, clear explanation before the user opts in. Do not present an ad as a system error.                                 |
| Local `.db` backup while offline                              | **Always available**                       | Do not require an ad or internet connection.                                                                                    |

No ad may cover buttons, cause layout jumps, or delay a checkout. The product remains free without subscriptions or paid feature tiers; the UX must clearly explain any optional ad-supported action before it starts.

## 8. Local Data Model

Use Room with SQLite and Write-Ahead Logging (WAL). The following entities and fields form the initial schema contract; add indexes, relations, and constraints as needed without losing the required fields.

| Entity                   | Required fields                                                                                                                                                                                                                                                                                                                                                                                       |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `products`               | `id` (PK), `sku`, `barcode`, `name`, `category`, `base_unit`, `base_hpp` (Long), `base_price` (Long), `stock_base_qty` (Double), `min_stock_alert`, `is_active`                                                                                                                                                                                                                                       |
| `product_variants_units` | `id` (PK), `product_id` (FK), `variant_name`, `conversion_factor`, `custom_barcode`, `unit_price` (Long), `wholesale_min_qty` (Int), `wholesale_price` (Long)                                                                                                                                                                                                                                         |
| `customers`              | `id` (PK), `name`, `whatsapp_number`, `total_debt` (Long), `notes`                                                                                                                                                                                                                                                                                                                                    |
| `transactions`           | `id` (PK), `invoice_no`, `created_at` (EpochMillis), `cashier_id`, `customer_id` (nullable FK), `subtotal`, `discount_total`, `tax_total`, `rounding`, `grand_total`, `total_hpp`, `net_profit` (Long), `payment_method` (CASH, QRIS, TRANSFER, SPLIT, KASBON), `paid_amount`, `change_amount`, `remaining_debt` (Long), `due_date` (nullable), `status` (COMPLETED, PARTIAL_DEBT, RETURNED), `notes` |
| `transaction_items`      | `id` (PK), `transaction_id` (FK), `product_id` (FK), `variant_name`, `qty` (Double), `unit_hpp` (Long), `applied_unit_price` (Long), `is_nego_price` (Boolean), `is_wholesale_applied` (Boolean), `item_discount` (Long), `subtotal` (Long)                                                                                                                                                           |
| `debt_payments`          | `id` (PK), `transaction_id` (FK), `customer_id` (FK), `paid_at`, `amount` (Long), `payment_method`, `note`                                                                                                                                                                                                                                                                                            |
| `cashier_shifts`         | `id` (PK), `cashier_name`, `pin_hash`, `recovery_answer_hash`, `opened_at`, `closed_at`, `starting_cash` (Long), `expected_ending_cash` (Long), `actual_ending_cash` (Long)                                                                                                                                                                                                                           |

Use automatic schema migration where safe. Database upgrades must not silently delete user data. Destructive migrations require an explicit, tested migration plan and a backup strategy.

## 9. Technical Architecture and Performance

### 9.1 Stack

- Kotlin **2.0+**
- Minimum SDK **23** (Android 6.0)
- Target SDK **35**
- Jetpack Compose + Material 3
- Room + SQLite WAL
- Google ML Kit Barcode Scanning
- Android Bluetooth and USB APIs for printer/scanner support
- Google Drive API v3 `appDataFolder` for cloud backup
- Google AdMob for designated non-critical placements

### 9.2 Low-end device targets

- Release package target: **below 8 MB**, subject to dependency and asset feasibility.
- Target device class: Android phones with **2 GB RAM**.
- Cold-start goal: **under 800 ms** on the target device class; record measured results rather than treating this as guaranteed on every device.
- Enable R8 full mode (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) for release builds.
- Use Baseline Profiles and profile the critical cashier path.
- Keep expensive drawing effects and unnecessary animations out of core flows.
- Database report queries must not block sales entry.

### 9.3 UX and accessibility implementation

- Support light and dark themes.
- Support text scaling from 100% to 135%.
- Use semantic labels and content descriptions for icons and controls.
- Provide visible focus/pressed/disabled states.
- Keep important state understandable without color alone.
- Respect Android accessibility settings and system font scaling.
- Use phone single-column layouts with bottom sheets and tablet split views where appropriate.

### 9.4 Responsive implementation requirements — mandatory

Implement layouts using Jetpack Compose constraints and window size classes. A screen must adapt to the current available width and height rather than rely on the emulator/device label.

- Use one responsive scaffold/navigation owner so phone bottom navigation and tablet rail/side navigation cannot accidentally render together.
- Keep each screen's content within the measured content bounds after navigation and system insets are applied.
- Use `fillMaxWidth`, `weight`, adaptive grid columns, and constrained containers appropriately; avoid hardcoded widths for primary content and avoid `offset` as a fix for broken layout.
- Add bottom padding that accounts for the bottom navigation bar and system navigation area. Add IME insets to forms so the keyboard does not hide the active field or Save/Charge button.
- For inventory/product lists, use adaptive grids only when each item has enough room. Otherwise switch to a single-column list; never squeeze cards until text or prices are clipped.
- Handle device rotation, split-screen, and runtime window resizing without keeping stale width measurements.
- If a compact viewport cannot support a split view, collapse to one column instead of clipping or horizontally scrolling the whole page.

## 10. Implementation Orchestration for AI Agents

Use a master orchestrator plus five sub-agents. The master must establish shared contracts before parallel work begins to reduce merge conflicts.

### Phase 0 — master setup

The master agent must:

- Create the Gradle project skeleton and configure `settings.gradle.kts`, `build.gradle.kts`, and `gradle.properties` with parallel build/caching where appropriate.
- Create stable shared data models under `core/model/` and interfaces under `core/contract/`.
- Freeze `core/contract/` while sub-agents work; only the master may change shared contracts unless explicitly authorized.
- Define package ownership and integration tests before parallel implementation starts.

### Parallel work allocation

| Agent                         | Focus                                       | Isolated directories                                          | Responsibilities                                                                                                         |
| ----------------------------- | ------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| 1 — Data & Storage            | Room, pricing data, backup/Drive            | `data/local/`, `data/backup/`                                 | Entities, DAOs, safe import/export, schema migrations, Drive appDataFolder.                                              |
| 2 — POS & Accounting          | Checkout, wholesale, units, debt, returns   | `domain/pos/`, `domain/accounting/`                           | Price engine, calculation accuracy, split payments, partial debt, return/exchange corrections to stock and HPP.          |
| 3 — Hardware & Receipts       | Printers, scanners, receipts                | `hardware/printer/`, `hardware/scanner/`, `hardware/receipt/` | ESC/POS over Bluetooth/USB, bitmap receipt renderer, QRIS image, PNG receipt, share intent, disconnect safety.           |
| 4 — Reports & Ads             | Reports, export, AdMob                      | `feature/reports/`, `monetization/admob/`                     | Profit/HPP queries without arbitrary time limits, lightweight CSV/XLSX, approved ad placements and offline behavior.     |
| 5 — Mobile UI & Accessibility | Phone/tablet UI, quick mode, PIN/biometrics | `ui/theme/`, `ui/cashier/`, `ui/tablet/`, `ui/security/`      | Modern Material 3 design system, clear cashier flow, responsive layouts, scalable typography, PIN/biometric recovery UI. |

All agents must follow the visual system and UX rules in Sections 4–6. UI work must not introduce dense text, competing primary buttons, inconsistent icon sets, or ads in the checkout path.

## 11. Reliability and Error Handling

### USB and Bluetooth detach handling

Wrap USB/printer/scanner I/O in appropriate coroutine error handling. Listen for `UsbManager.ACTION_USB_DEVICE_DETACHED`. Unexpected device removal should produce a concise, actionable message and must not crash the app.

For Bluetooth, handle disconnections, permission failures, and unavailable devices with clear recovery actions such as **Reconnect** or **Choose printer**.

### Transaction integrity

- Persist a completed transaction atomically.
- Prevent duplicate sales from repeated taps or scanner events.
- Show progress only when an action genuinely takes time.
- Preserve the cart if a non-destructive operation fails.
- Avoid losing unsaved product edits when the app rotates or resumes where supported by the architecture.

### Error message style

Use plain language, explain the next action, and avoid raw exception text. Detailed technical logs may be retained for debugging but must not be the user-facing message.

## 12. Verification and Testing

Run these checks after integration and before a release candidate.

```bash
# Run unit tests, check the connected device, install, launch, and monitor runtime errors
./gradlew testDebugUnitTest
adb devices
./gradlew installDebug
adb shell monkey -p id.umkm.pos. pro -c android.intent.category.LAUNCHER 1
adb logcat -v time AndroidRuntime:E *:S
```

### UX verification checklist

- A new user can find the Cashier destination immediately.
- A sale can be completed without navigating through settings or dismissing ads.
- Product search and scanner input do not require unnecessary taps.
- The total, payment, change, and outstanding balance are clear at the right time.
- Buttons remain visible at 135% font scale and on smaller screens.
- Empty, loading, success, offline, and error states are concise and actionable.
- Light and dark themes both have readable contrast.
- Icons are consistent and have appropriate accessibility labels.
- Tablet layouts use available space without creating a dense dashboard.
- Ad containers disappear cleanly when offline or when no ad is available.

### Responsive layout acceptance matrix — release blocker

Capture and review real screenshots for the following available window sizes. The exact device may differ, but the measured app window must match the target size as closely as practical.

| Test case                   | Target size / state                        | Required behavior                                                                                           |
| --------------------------- | ------------------------------------------ | ----------------------------------------------------------------------------------------------------------- |
| Compact phone               | 360 × 800 dp                               | Single content column; bottom navigation only; no clipped title, button, card, or left edge.                |
| Typical phone               | 393 × 852 dp                               | Cashier and Products screens remain readable; primary actions stay visible and reachable.                   |
| Narrow phone / split window | 320 × 720 dp or narrowest supported window | Content reflows; no full-page horizontal scrolling; secondary detail moves to a sheet or a separate screen. |
| Large phone, larger text    | Typical phone at 135% font scale           | Labels wrap or reflow; critical totals and actions do not overlap or disappear.                             |
| Tablet                      | 800 × 1280 dp or equivalent expanded width | A rail or side navigation may appear; main content uses the remaining width and stays aligned.              |
| Resized / split-screen      | Window width changed while app is open     | Navigation and screen layout adapt without duplicated navigation, stale offsets, or lost task state.        |

**Screenshot-specific regression rules:**

- Never show the phone bottom navigation and tablet/desktop sidebar/rail simultaneously.
- Never let the page title or primary action be partially hidden behind navigation.
- Never render a desktop-width inventory view within a narrow viewport.
- Never leave unused reserved space where a hidden sidebar would have been.
- Never let cards expand into large empty blocks merely because a wide layout failed to reflow.
- Verify the Cashier and Products screens independently; passing one screen does not establish that the whole app is responsive.

The build is not accepted on the basis of compilation alone. Review screenshots at every test size and fix all clipping, overlap, horizontal overflow, duplicated navigation, or inaccessible primary actions before release.

### Functional and regression verification

- Test tiered wholesale pricing, negotiated prices, units, discounts, tax, rounding, HPP, and net profit.
- Test partial debt, installment payments, returns, exchanges, stock updates, and transaction history.
- Test both 58 mm and 80 mm printer layouts, bitmap receipts, and reconnect behavior.
- Test camera barcode scanning and hardware scanner input.
- Unplug USB devices during use and verify that the app remains stable.
- Test local backup/restore, JSON/CSV/XLSX export, and Google Drive backup behavior online and offline.
- Test PIN recovery and ensure local backup remains accessible.
- Run migration tests against existing user data.
- Check performance on an actual low-end Android device; do not infer performance from emulator results alone.

## 13. Definition of Done

The release candidate is ready when:

- The UI follows a consistent, modern, clean Material 3 design system.
- Cashier tasks are short, obvious, and uninterrupted by ads.
- Primary screens have clear hierarchy and no unnecessary text-heavy panels.
- Core pricing, payment, accounting, and stock rules pass automated tests.
- Backup and recovery respect merchant data ownership.
- Hardware disconnects do not crash the app.
- Phone and tablet layouts are usable, including at larger text sizes.
- All required viewport screenshots have been reviewed, and there is no clipped content, duplicated navigation, unexpected horizontal overflow, or overlapping primary action.
- The compact-phone layout shows bottom navigation only; expanded layouts show a rail/side navigation only where appropriate.
- The app builds and launches on a physical device.
- Known platform limitations and unverified performance targets are documented.

## 14. Product Success Measures

Track these measures during testing and, where appropriate, after release:

- Time and number of taps required to complete a typical sale.
- Checkout error and duplicate-transaction rate.
- Success rate for barcode scanning and receipt printing.
- Crash rate during USB/Bluetooth disconnection and app resume.
- Backup creation and restore success rate.
- Frame responsiveness and cold-start time on low-end devices.
- Usability at 100–135% font scale and ability to complete checkout without encountering ads.
