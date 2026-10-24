# Pookie POS

## Product Requirements Document

**Version:** 1.29 — Quick shift flow fix (2026-10-24; see §16)  
**Target implementation agent:** Deepseek V4.1 Flash (High Effort) with five parallel sub-agents  
**Development environment:** Antigravity IDE, Android SDK command-line tools, Gradle, ADB, scrcpy, and a physical Android device  
**Product promise:** **Free forever. No subscriptions. No paid feature tiers. No watermarks.**

> **Product direction:** A lightweight, offline-first POS that feels calm, clear, and fast. The interface should help a busy shop owner complete a sale with as few decisions and taps as possible. Modern visuals must support usability—not compete with it.

---

## 1. Executive Summary

Pookie POS is an Android point-of-sale application for Indonesian micro, small, and medium-sized businesses (UMKM). It is designed for low-end Android devices, works offline for core operations, and provides cashier, inventory, customer-debt, reporting, receipt-printing, and backup capabilities without subscription fees.

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
- Cash, QRIS, and bank transfer.
- Returns and exchanges.
- Transaction history and profit/loss reporting with cost of goods sold (HPP).
- Bluetooth and USB OTG thermal printing, bitmap receipt rendering, and receipt sharing.
- Camera barcode scanning and physical USB/Bluetooth HID scanner input.
- Local export/import and Google Drive appDataFolder backup.
- PIN/biometric protection for sensitive actions with an owner recovery path. *(Biometric recovery is an open decision, §15 Q3. The approved recovery path is the offline recovery code; see §7.7.)*
- Responsive phone/tablet UI, a single light theme, and adjustable text scaling.
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
- **Products** — catalog, prices, units, barcode, and stock.
- **Reports** — sales, profit, HPP, and transaction history.
- **More** — printer/scanner setup, backup, exports, staff/PIN, and settings.

Implemented deviation (19 October 2026): the shipped primary set is **Kasir, Produk,
Transaksi, Laporan, Lainnya**, so **Transactions** occupies the third slot. Customer data and
kasbon were removed on 23 October 2026, so no customer destination exists. This remains a
recorded deviation instead of a silent rewrite, because it changes what a cashier sees on
every screen.

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
- Always use the light theme, regardless of the system dark setting.

### 5.2 Iconography

Use one consistent modern icon set compatible with Jetpack Compose and Material 3, such as Material Symbols Rounded or the project's established Material icons.

- Pair icons with text labels for primary navigation and important actions.
- Icon-only buttons are acceptable for widely recognized actions (back, close, search, overflow) when they have a tooltip/content description and a sufficient touch target.
- Do not mix emoji, unrelated icon packs, or multiple visual styles.
- Use icons to help scanning—not as decoration.
- The launcher icon uses the green-and-white P mark in `newicon.jpeg`; preserve its shape and proportions.

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

- A compact centered top area with the title and status line; no header action buttons. Shift opening and switching are available from **More**, from the cart action once a cart exists, and from **Charge** when no shift is open.
- A searchable product list or grid with readable product names, prices, and stock cues.
- A cart summary that remains visible or is available in one obvious action on smaller phones.
- A prominent **Charge** action with the current total.
- Quantity changes directly in the cart; advanced price editing is available through a small, focused edit sheet.

**Rules:**

- Adding an item should provide immediate feedback without interrupting the workflow.
- A hardware barcode scanner must be able to add items without requiring the user to tap the search field each time when scanner mode is active.
- Keep the item count, subtotal, discount, tax (when enabled), and final total clear.
- Put negotiated price, wholesale tier details, and rarely used adjustments under an explicit **More options** action.
- Browsing products and building a cart stay available without a shift, but confirming a sale **requires an open shift**. When none is open, **Charge** opens a compact **Mulai Shift** sheet: the active/default cashier is preselected, starting cash is prefilled to **Rp 0** and remains editable, and **Mulai Shift** opens the shift and continues to payment. The full staff/starting-cash form remains available through **Atur detail**.
- The operational shift flow selects the cashier from the staff directory. Free-text cashier names are not accepted while opening or switching a shift; the directory is maintained in **Settings → Staf & Kasir** (§6.6).
- With no active shift, the quick form preselects an active staff member and prefills the starting cash to 0; **Atur detail** opens the full form to change either value. Opening is not PIN-gated.
- With an active shift, the shift sheet offers two tabs:
  - **Ganti kasir** — changes the active staff member without a cash count. The shift stays open, so its opening time and starting cash are unchanged.
  - **Tutup shift** — records the cash count and shows expected versus actual cash. It presents one primary action plus a **Lanjut ke kasir lain** toggle (default off): with the toggle off the action is **Tutup shift** and closes the shift; with it on the action becomes **Tutup & mulai shift** and closes the shift then opens a new one for the selected cashier.
- Both **Ganti kasir** and **Tutup shift** require an empty cart. If the cart has items, offer **Kosongkan & Ganti** behind a confirmation instead of discarding them silently.
- Closing or switching the shift still uses the owner-level `CriticalAction.SWITCH_CASHIER` PIN gate (§7.7). A per-cashier PIN is not part of this iteration.
- Never show ads on the cashier, cart, payment, transaction-success, or receipt-printing flow.

**Checkout flow:**

1. Review cart and total.
2. Choose payment method and enter the amount received where relevant.
3. Confirm the transaction.
4. Show a compact success state with next actions: **Print receipt**, **Share receipt**, or **New sale**.

Avoid long success paragraphs and avoid automatically opening an ad after a transaction.

### 6.2 Product and inventory screens

- Provide clear search, barcode/SKU lookup, and category filters.
- Show product name, selling price, and current stock. Product cards show stock as neutral text (`Stok: N`) only; low/out status is confined to filters and header summaries, never to cards.
- Keep **Add product** easy to find without dominating the screen.
- Use a focused product form with clearly grouped sections: **Basics**, **Pricing & units**, and **Stock alerts**.
- Advanced wholesale rules and unit conversions should be expandable rather than displayed as a dense form by default.
- Use inline validation and explain what needs correction in plain language.

### 6.3 Customers and debt (removed 2026-10-23)

Customer records, kasbon, partial debt, due dates, and debt payments were removed at the owner's
request. No screen records or displays customer or debt data. Historical SPLIT and KASBON
transactions were converted by `MIGRATION_10_11` as described in §8; the customer and debt tables
were dropped.

### 6.4 Reports and transaction history

- Start with a small set of useful summaries: sales, estimated net profit, HPP, and transaction count.
- Make date range selection easy to find.
- Use clean lists and restrained charts; avoid dashboards packed with many small cards.
- Provide clear drill-down from summary to the transactions or items behind it.
- Make export actions easy to discover but visually secondary to the report itself.
- Support historical aggregation across years without an arbitrary time limit.
- A single adaptive banner may appear below the report content. It must disappear entirely when offline or when no ad fills; it must never cover report values, export actions, or the navigation bar.

### 6.5 Backup, export, printer, and scanner setup

Keep device and data utilities under **More** with simple status rows and clear actions.

- Printer setup should show connection status, selected printer, test-print action, and reconnect action.
- Scanner setup should explain camera scanning and hardware scanner support briefly.
- Backup must distinguish **Local backup** from **Google Drive backup**.
- Local backup must remain available when offline and must not be blocked by an unavailable ad.
- Show file type and destination before export; show a clear success or failure state afterward.

### 6.6 Setup, cashier PIN, and recovery

- Keep initial setup short and allow the owner to reach the cashier quickly.
- Maintain cashiers in a staff directory under **Settings → Staf & Kasir**: add, rename, and activate or deactivate staff. Staff names must be unique regardless of letter case, and at least one active staff member must remain. Free-text cashier entry is allowed only on this screen; the operational shift flow selects from the directory.
- Seed the directory with one default staff member so the first shift can start without setup. When an existing installation upgrades, the seed uses the previous settings cashier name if present, otherwise **Kasir Utama**.
- Open the first shift from **Charge**, the cart action, or **More**. The quick form preselects the active/default cashier and prefills starting cash to 0; **Atur detail** opens the full form. Opening a shift does not require a PIN.
- Explain PIN protection only when asking the user to enable it.
- Use device biometrics where available and explicitly enabled by the owner. *(Not implemented in code; open decision §15 Q3.)*
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

### 7.3 Payments, returns, and exchanges

Support the following payment methods:

- Cash
- QRIS
- Bank transfer
- Debit
- Credit

The cashier payment page shows cash and QRIS first, with a More options action that reveals transfer,
debit, and credit. QRIS requires a configured static payload. Every sale must be fully paid; partial
payment, outstanding balance, and due dates are not supported. Return/exchange logic must correct
stock and HPP consistently and preserve the transaction audit trail.

The payment UI should show **Total**, **Paid**, and **Change** only when relevant. Do not show
irrelevant fields for a payment method.

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

- Camera scanning with CameraX and the `zxing.core` decoder the app already ships for QR
  generation, so no second decoding engine is added. Decode from the luminance plane and
  restrict the format set to the retail symbologies to keep the cost per frame low on a 2 GB
  device. Feeding and throttling rules are fixed in `docs/design/barcode-scan.md`.
- Do not use the Play Services ML Kit variant. It fetches its model over the network, which
  contradicts the offline-first requirement and the absence of the `INTERNET` permission.
- Bundled Google ML Kit Barcode Scanning is the documented fallback. Switch to it if the
  measured first-try read rate misses the acceptance gate in `docs/design/barcode-scan.md`
  (19 of 20 reads within two seconds on real product labels), then re-measure the release size
  against the budget in 9.2.
- Hardware USB OTG and Bluetooth HID input through a keyboard-event interceptor.
- Handle unexpected detach events gracefully. Removing a USB scanner or printer must not crash the application.

### 7.6 Backup and data ownership

Provide local export/backup formats: SQLite `.db`, `.json`, `.csv`, and `.xlsx` where relevant. Support synchronization to the user's personal Google Drive using Drive API v3 `appDataFolder`.

- Clearly distinguish local backup from cloud backup.
- Keep local backup available offline.
- Do not encrypt, hide, or hold local backup files hostage behind the cashier PIN. *(Open decision §15 Q2. Until it is decided, this is read as protecting access to the backup files themselves, not as forbidding an authorization check before an export action.)*
- Give the user clear confirmation when a backup succeeds or fails.
- Explain that cloud backup requires connectivity and appropriate account authorization.

### 7.7 PIN, biometric access, and anti-lockout

PIN protection may restrict sensitive actions, such as deleting transaction history or allowing staff to view HPP. It must not prevent the owner from accessing their business data permanently.

Provide:

- Device biometric recovery when available and enabled. *(Not implemented in code and no biometric dependency is present; open decision §15 Q3.)*
- An offline recovery code generated during PIN setup, with an option to print or save it securely. *(Confirmed by the owner on 2026-10-20 as the only recovery path. Security questions are out of scope. The code is printed through the Android Print Framework so it can be saved as a PDF; a thermal printer is optional.)*
- Clear recovery instructions that do not depend on internet access.

**Implemented 2026-10-20.** The decisions are in §15.1. Phase 0–1 built the storage and policy layer: the `security_store` schema at database version 6 with `MIGRATION_5_6`, salted PBKDF2 hashing that stores the algorithm and the iteration count inside the encoded value, the PIN policy (required role per action, discount and below-cost thresholds, five-attempt lockout, ten-attempt global lockout, five-minute session unlock), a local `security_audit_log` table, exclusion of both security tables from the JSON backup, and a forced PIN reset after a restore. Phase 2–3 added the security settings screen (enable, change, disable, regenerate the recovery code), the one-time recovery-code screen that requires acknowledging the code was saved and prints it to PDF through the Android Print Framework, the read-only audit history, and one global PIN gate. Gated actions: viewing profit and HPP (values are masked until the session is elevated), editing HPP and selling prices, an item discount above 10% or Rp 20.000, a negotiated price below cost, refund or return, CSV/XLSX report export, JSON backup export, backup restore, product deletion, tax and rounding changes, QRIS removal, recording a debt payment, and closing, switching, or ending a cashier shift. Manager-level actions accept a five-minute session; owner-level actions always prompt. With the PIN off, every flow behaves exactly as before. Biometric recovery stays deferred (§15.1 Q3).

**Shift scope, added 2026-10-21; updated 2026-10-22 for the staff directory.** A shift is required before checkout; browsing the catalogue and building a cart remain available without one. Opening a shift is not gated. Closing a shift, closing with a handover to another staff member, and **Ganti kasir** are `CriticalAction.SWITCH_CASHIER`, an owner-level action that always prompts when PIN protection is enabled. **Tutup shift** and **Tutup & mulai shift** record a cash count, reconcile expected cash over the shift's opened–closed window, and require explicit confirmation when actual cash differs from expected cash. **Ganti kasir** takes no cash count: the shift stays open and only the active staff member changes. The cash formula and refund assumption are in §8 and §15.1 Q13.

**Per-cashier PIN, deferred 2026-10-22.** This iteration identifies the operator through the staff directory and attributes shifts and transactions to a staff row. It does not authenticate the individual cashier. The owner PIN still protects the cash handover, and a per-cashier PIN is a later security step (§15.1 Q15), not a shipped capability.

### 7.8 AdMob policy — zero friction at checkout

| Screen / feature                                              | Ad treatment                               | Behavior                                                                                                                        |
| ------------------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------- |
| Cashier, cart, payment, transaction success, receipt printing | **No ads**                                 | Never load banner or interstitial ads in this flow.                                                                             |
| Reports and stock management                                  | Adaptive banner may appear                 | Place at the bottom, outside the content and action area. Hide the entire ad container when offline or when no ad is available. |
| Excel export                                                  | Rewarded video                             | Shows a rewarded video ("Watch & Export" / "Tonton & Ekspor") after PIN verification and storage permissions. Watching the ad completes the export; dismissing early cancels the export with a notice (`reportsAdRequired`) without writing a file. If the ad cannot load or show (offline, no fill, SDK error, no Activity), fail-open: the export proceeds so the owner is never locked out of business data. Production unit: `ca-app-pub-5729173430518647/6119412059` (`pookie_rewards`); test unit: `ca-app-pub-3940256099942544/5224354917`. |
| Google Drive backup, bulk shelf-label printing                 | Optional rewarded video per product policy | Planned/deferred. Use a short, clear explanation before the user opts in. Do not present an ad as a system error. |
| Local `.db` backup while offline                              | **Always available**                       | Do not require an ad or internet connection.                                                                                    |

No ad may cover buttons, cause layout jumps, or delay a checkout. The product remains free without subscriptions or paid feature tiers; the UX must clearly explain any optional ad-supported action before it starts.

## 8. Local Data Model

Use Room with SQLite and Write-Ahead Logging (WAL). The following entities and fields form the initial schema contract; add indexes, relations, and constraints as needed without losing the required fields.

| Entity                   | Required fields                                                                                                                                                                                                                                                                                                                                                                                       |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `products`               | `id` (PK), `sku`, `barcode`, `name`, `category`, `base_unit`, `base_hpp` (Long), `base_price` (Long), `stock_base_qty` (Double), `min_stock_alert`, `is_active`                                                                                                                                                                                                                                       |
| `product_variants_units` | `id` (PK), `product_id` (FK), `variant_name`, `conversion_factor`, `custom_barcode`, `unit_price` (Long), `wholesale_min_qty` (Int), `wholesale_price` (Long)                                                                                                                                                                                                                                         |
| `transactions`           | `id` (PK), `invoice_no`, `created_at` (EpochMillis), `cashier_id`, `cashier_name`, `staff_id` (nullable FK), `shift_id` (nullable FK), `subtotal`, `discount_total`, `invoice_discount`, `tax_total`, `rounding`, `grand_total`, `total_hpp`, `net_profit` (Long), `payment_method` (CASH, QRIS, TRANSFER), `paid_amount`, `change_amount`, `status` (COMPLETED, RETURNED), `notes` |
| `transaction_items`      | `id` (PK), `transaction_id` (FK), `product_id` (FK), `variant_name`, `qty` (Double), `unit_hpp` (Long), `applied_unit_price` (Long), `is_nego_price` (Boolean), `is_wholesale_applied` (Boolean), `item_discount` (Long), `subtotal` (Long)                                                                                                                                                           |
| `cashier_shifts`         | `id` (PK), `cashier_name`, `opened_by_staff_id` (nullable FK), `closed_by_staff_id` (nullable FK), `closed_by_name`, `pin_hash`†, `recovery_answer_hash`†, `opened_at`, `closed_at`, `starting_cash` (Long), `expected_ending_cash` (Long), `actual_ending_cash` (Long)                                                                                                                                     |
| `staffs`                 | `id` (PK), `name` (unique, case-insensitive), `role` (CASHIER for this iteration), `is_active`, `created_at`, `updated_at`                                                                                                                                                                                                                                                                             |

† `pin_hash` and `recovery_answer_hash` do not exist on the shipped `cashier_shifts` entity (`app/src/main/java/id/umkm/pos/pro/data/local/entity/CashierShiftEntity.kt:15-22`). PIN material lives in `security_store` (`data/local/entity/SecurityStoreEntity.kt`), which is the location decided on 2026-10-20 (see §15.1 Q1).

**Shift reconciliation (2026-10-21; staff links added 2026-10-22).** `cashier_shifts` is the operational shift record, not a PIN store. One shift is active at a time. Opening writes `cashier_name`, `opened_by_staff_id`, `opened_at`, and `starting_cash`. Closing writes `closed_at`, `expected_ending_cash`, `actual_ending_cash`, and the closer in `closed_by_staff_id`/`closed_by_name`. Changing the active operator through **Ganti kasir** does not close the shift; it updates `app_settings.active_staff_id`. `ShiftRepository` computes expected cash over the shift's `opened_at`–`closed_at` window:

`expectedCash = startingCash + cashSales − cashRefunds`

- `cashSales` includes CASH as `paid_amount − change_amount`; QRIS and TRANSFER contribute no cash.
- `cashRefunds` comes from the `returns` ledger. Refunds are assumed to be cash for the MVP because the return flow has no refund-method field.

`variance = actualEndingCash − expectedEndingCash`; a non-zero variance requires explicit confirmation before the shift closes. `transactions.staff_id` records the active staff member at sale time and `transactions.shift_id` records the shift that was open. `transactions.cashier_name` remains a display snapshot so a historical receipt reprint names the cashier who made that sale, even after a later handover or rename. Cash reconciliation still sums over the shift's time window rather than filtering by `staff_id`, so changing the operator inside one shift does not split its cash summary. The `SWITCH_CASHIER` PIN gate is described in §7.7.

**Present in code but missing from the table above (verified 2026-10-20; `cashier_name` added 2026-10-21; `staffs` added 2026-10-22; customer removal 2026-10-23):** `returns`, `return_lines`, `app_settings`, `security_store`, `security_audit_log`, `invoice_counters`. The database is at Room `version = 11` with explicit migrations `MIGRATION_1_2` through `MIGRATION_10_11` and no destructive fallback (`data/local/UmkmDatabase.kt`). `MIGRATION_6_7` adds the nullable `transactions.cashier_name` column. `MIGRATION_7_8` creates `staffs` with a unique case-insensitive name index, adds `app_settings.active_staff_id`, `cashier_shifts.opened_by_staff_id`/`closed_by_staff_id`/`closed_by_name`, and `transactions.shift_id`/`staff_id` plus their indexes, then backfills the active staff, the open shift's opener, and historical transaction staff links. `MIGRATION_9_10` adds the nullable `products.icon` column. `MIGRATION_10_11` was approved by the owner as a destructive removal: it rebuilds `transactions` without `customer_id`, `remaining_debt`, `due_date`, and the customer foreign key; preserves `transaction_items`; drops `customers` and `debt_payments`; maps legacy `SPLIT` and `KASBON` rows to `CASH`; and maps `PARTIAL_DEBT` to `COMPLETED`.

Backup and restore iterate a fixed table list that includes `staffs` and **excludes** `security_store` and `security_audit_log` (`data/backup/BackupTables.kt`); a restore forces the PIN off and writes an audit entry. A backup whose `schemaVersion` is below 8 is accepted without a `staffs` table, and the restore seeds **Kasir Utama** as the default staff; backups at schema version 8 or later must contain `staffs`. Evidence: `docs/security/prd-code-reconciliation.md` R-01, R-02, R-31.

Use automatic schema migration where safe. Database upgrades must not silently delete user data. Destructive migrations require an explicit, tested migration plan and a backup strategy.

## 9. Technical Architecture and Performance

### 9.1 Stack

- Kotlin **2.0+**
- Minimum SDK **23** (Android 6.0)
- Target SDK **36** (`targetSdk` and `compileSdk`), required for new Play releases since 31 August 2026
- Jetpack Compose + Material 3
- Room + SQLite WAL
- CameraX for camera capture, with the existing `zxing.core` decoder for camera barcode
  scanning. Bundled ML Kit only if the measured read rate misses the acceptance gate
- Android Bluetooth and USB APIs for printer/scanner support
- Google Drive API v3 `appDataFolder` for cloud backup (planned, not implemented; see
  `docs/prd-gaps.md`)
- Google AdMob for designated non-critical placements. **Implemented 2026-10-24:** Google Mobile
  Ads SDK **24.0.0** with one adaptive banner at the bottom of Reports and a rewarded ad placement for
  report export (`AdMobAdController`). Debug builds use Google test app ID (`ca-app-pub-3940256099942544~3347511713`),
  test banner (`ca-app-pub-3940256099942544/6300978111`), and test rewarded unit (`ca-app-pub-3940256099942544/5224354917`);
  release builds use production app ID (`ca-app-pub-5729173430518647~5768680380`), banner `Pos_Reports_Banner`
  (`ca-app-pub-5729173430518647/1672900587`), and rewarded unit `pookie_rewards` (`ca-app-pub-5729173430518647/6119412059`).
  Server-side verification (SSV) and frequency capping are off. Rewarded export uses a fail-open policy so business data
  remains accessible offline or on ad load failure. Other rewarded surfaces (cloud backup, shelf labels) remain deferred.

### 9.2 Low-end device targets

- Release package target: **below 8 MB**, subject to dependency and asset feasibility. Measured
  release APK on 2026-10-24 with the AdMob SDK: **6.5 MB** (debug 21 MB).
- Target device class: Android phones with **2 GB RAM**.
- Cold-start goal: **under 800 ms** on the target device class; record measured results rather than treating this as guaranteed on every device.
- Enable R8 full mode (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) for release builds.
- Use Baseline Profiles and profile the critical cashier path.
- Keep expensive drawing effects and unnecessary animations out of core flows.
- Database report queries must not block sales entry.

### 9.3 UX and accessibility implementation

- Use a single light theme.
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
adb shell monkey -p id.umkm.pos.pro -c android.intent.category.LAUNCHER 1
adb logcat -v time AndroidRuntime:E *:S
```

### UX verification checklist

- A new user can find the Cashier destination immediately.
- A sale can be completed without navigating through settings or dismissing ads.
- Product search and scanner input do not require unnecessary taps.
- The total, payment, change, and outstanding balance are clear at the right time.
- Buttons remain visible at 135% font scale and on smaller screens.
- Empty, loading, success, offline, and error states are concise and actionable.
- The light theme has readable contrast at 100% and 135% font scale.
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
- Test PIN recovery and ensure local backup remains accessible. *(PIN is not implemented; see §15.)*
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

## 15. PIN decisions and remaining open items

Recorded 2026-10-20 from `docs/security/prd-code-reconciliation.md`. The owner delegated these decisions to the main agent on 2026-10-20 ("do your best; I will say if I disagree"). The decisions actually taken are in §15.1 and remain revisable by the owner at any time. The table below is kept as the record of the options that were on the table.

| ID | Open decision | Options | Blocking |
| --- | --- | --- | --- |
| Q1 | Where PIN material is stored | **A:** keep `security_store` and correct the `cashier_shifts` row in §8. **B:** add `pin_hash`/`recovery_answer_hash` to `cashier_shifts` and move PIN storage there. | Yes |
| Q2 | Whether the backup export action may require a PIN | **A:** allow a PIN check before export and clarify §7.6. **B:** gate only restore; leave export open. | Yes |
| Q3 | Biometric recovery | **A:** defer it and say so in §3.1/§6.6/§7.7. **B:** add `androidx.biometric` and build the prompt now. | Yes |
| Q4 | Identity model | **A:** single owner PIN for the MVP. **B:** a `security_users` table with cashier/manager/owner roles. | Yes |
| Q5 | Cashier-shift UI | **A:** keep "close shift" out of PIN scope until the shift UI exists (no UI calls `ShiftRepository` today). **B:** build the shift UI first. **Resolved 2026-10-21:** option B — the shift UI is built. Opening a shift needs no PIN; closing, switching, and ending it are owner-PIN gated through `CriticalAction.SWITCH_CASHIER` and always prompt. **Updated 2026-10-22:** the flow selects staff from a directory and separates **Ganti operator** (shift stays open) from **Tutup shift** (cash count and close). Q5 is no longer deferred. | No (resolved) |
| Q6 | Audit log | **A:** approve a local `security_audit_log` table and its retention. **B:** no audit log. | No |
| Q7 | Report export: PIN and rewarded ad | **A:** PIN first, then the ad. **B:** PIN replaces the ad gate. | No |
| Q8 | Changing the PIN | **A:** require the current PIN. **B:** the recovery code alone is sufficient. | No |
| Q9 | Restore from a backup that contains `security_store` | **A:** ignore the key silently. **B:** tell the owner it was ignored. | No |
| Q10 | HPP field in the product form | **A:** hide the field for roles without the right. **B:** keep it visible and gate only saving. | No |
| Q11 | PIN thresholds and levels | Discount percentage/nominal that requires a PIN, the negotiated-price floor, and which role each level maps to. Numbers to be supplied by the owner. | Yes |

### 15.1 Decisions recorded 2026-10-20 and 2026-10-21

| ID | Decision | Rationale | Revisable |
| --- | --- | --- | --- |
| Q1 | PIN material stays in `security_store`; the `cashier_shifts` row in §8 is corrected and footnote † stands. | One shop, one POS, one authority. Two PIN locations would create two sources of truth and a larger migration for no benefit. | Yes |
| Q2 | A PIN is required before exporting the JSON backup. The §7.6 prohibition is read as protecting access to the backup files, not as forbidding an authorization check before the export action. | The JSON backup carries sales and stock data; customer and debt tables were removed on 23 October 2026. Local backup still works offline and is never blocked by an ad. | Yes |
| Q3 | Biometric recovery is deferred. The offline recovery code is the only recovery path. | No biometric code or dependency exists today; recovery must stay offline, and the release APK must stay under 8 MB. | Yes |
| Q4 | A single owner PIN. `SecurityRole` exists in the model as CASHIER/MANAGER/OWNER and every critical action declares the role it needs, but the MVP verifies only the owner PIN and ships no `security_users` table. | Matches a one-shop-one-POS product and keeps a path to per-user PINs without building one now. | Yes |
| Q5 | Shift UI and close-shift PIN: the shift UI is built. Opening a shift is ungated; closing, switching, and ending it are owner-level `CriticalAction.SWITCH_CASHIER` and always prompt when PIN protection is enabled. Q5 is closed; Q13 records the flow details. | A mandatory shift gives the till a traceable owner, and gating the money handover while keeping the start fast follows the cashier-speed priority. | Yes |
| Q6 | A local `security_audit_log` table is approved: it records the action, the actor label, the result, and the target; it never records the PIN; it is excluded from backups; the newest 1000 entries are retained. | Without it, denials, lockouts, and recoveries leave no trace. | Yes |
| Q7 | Report export prompts for the PIN first, then presents a rewarded ad (`AdPlacement.EXCEL_EXPORT`) before exporting. If the user earns the reward, export writes the file. If dismissed early, export is cancelled and `reportsAdRequired` is shown. If the ad cannot load or show (offline, no fill, missing Activity, SDK error), the flow fails open and proceeds with the export so data ownership is preserved. | Gating export behind rewarded video monetizes high-value report generation while the fail-open rule ensures the merchant is never locked out of their data. | Yes |
| Q8 | Changing the PIN requires the current PIN. The recovery code remains the escape hatch. | Stops an unlocked session from silently replacing the PIN. | Yes |
| Q9 | A restore whose backup contains `security_store` ignores that key and tells the owner it was ignored and that the PIN is now off. | §7.6 requires clear confirmation; removing a security setting silently would be confusing. | Yes |
| Q10 | Profit and HPP values stay hidden, and HPP/price editing stays gated, until the session is elevated. | §7.7 explicitly allows a PIN to restrict staff viewing HPP, and hiding a value is more honest than gating a save whose reason the user cannot see. | Yes |
| Q11 | Thresholds: an item discount above 10% or above Rp 20.000 requires a PIN; a negotiated unit price below the product's HPP requires a PIN; owner-level actions (change PIN, restore backup, change tax and rounding, remove QRIS) always prompt even during an elevated session; all other gated actions accept a 5-minute session unlock. Constants live in `PinPolicy` and are tunable. | Balances the cashier-speed priority (§3.1) against the risk of money walking out of the till. | Yes |
| Q12 | Dark mode is removed. The app always renders the light theme; the dark palette, night resources, and the system-theme switch are deleted. | The dark palette was never audited against the many hardcoded light colors in the cashier, receipt, thumbnail, and chart surfaces, so shipping it risked low-contrast screens. The owner chose light-only to reduce that risk and the QA matrix. | Yes |
| Q13 | A shift is required before checkout; browsing and cart building stay available. The shift bar shows the active staff member with **Ganti**/**Buka**; **More → Shift & Kasir** shows the active shift and its history, and links to the cashier for switching. Opening selects an active staff member from the directory and enters starting cash; it needs no PIN. With an active shift, the shift sheet separates two flows: **Ganti operator** changes the active staff member with no cash count and leaves the shift open, while **Tutup shift** records the cash count, shows expected versus actual cash, and closes the shift with an optional new shift for another staff member. History lists the last 30 with cashier, times, starting, expected, and actual cash plus variance. Closing, switching, and changing operator are owner-level `CriticalAction.SWITCH_CASHIER` and always prompt when PIN protection is enabled. `expectedCash = startingCash + cashSales − cashRefunds`, where cash sales include only CASH (`paid − change`); QRIS and TRANSFER contribute no cash. Refunds are assumed cash because the return flow has no refund method. `variance = actualCash − expectedCash`; a non-zero variance needs explicit confirmation. The cart must be empty before either flow; otherwise **Kosongkan & Ganti** asks for confirmation. Transactions store `staff_id` and `shift_id` and keep a `cashier_name` snapshot. | An unattributed till makes a shortage impossible to trace to a cashier, and a mandatory shift is the smallest reliable boundary. A directory stops dirty free-text names and makes operator attribution reliable, while separating operator change from close avoids a cash count for a mid-shift handover. Keeping opening ungated preserves the fast cashier start; gating close and switch protects the money handover. The `cashier_name` snapshot keeps reprints truthful after the next handover or a staff rename. A per-cashier PIN is deliberately excluded from this iteration (Q15). | Yes |
| Q14 | Staff identity comes from a `staffs` directory seeded with one default cashier. The shift flow never accepts a free-text cashier name; **Settings → Staf & Kasir** is the only place to add, rename, activate, or deactivate staff. Names are unique case-insensitively, at least one active staff member is required, and the active operator is stored in `app_settings.active_staff_id`. | Free text produced misspelled and duplicate cashier names, which broke shift history and per-cashier attribution. A small directory fixes the data at the source and keeps the cashier flow to a selection. | Yes |
| Q15 | A per-cashier PIN is deferred. The owner PIN remains the only authentication for cash handover; the staff selector is attribution and name hygiene, not per-person authentication. | Building per-user credentials needs a `security_users` model, enrollment, recovery, and a role policy that this iteration does not have. Shipping the directory first fixes attribution without pretending the till is authenticated per person. | Yes |
| Q16 | Cashier header is a centered title and status line with no action buttons; the identity pill was removed on 2026-10-23 at the owner's request. Shift opening and switching now use More, the cart action, or Charge when no shift is open. Charge opens a compact Mulai Shift sheet with the cashier preselected and starting cash 0; a successful shift auto-continues to payment. | The owner prefers a calmer header and accepts that an empty cart has no cashier-local open-shift control. The fallback remains in More. | Yes |

**Deferred, not dropped:** biometric recovery (Q3), transaction void and per-invoice discount input (no UI exists), per-cashier PINs and role-based staff permissions (Q4, Q15), and Google Drive backup. Dark mode is not deferred: it is removed by decision (Q12).

## 16. Revision History

Every change to this document is recorded here with its date and the sections it touched. See the "Documentation of changes" rule in the repository `AGENTS.md`.

| Date | Version | Change | Sections | Reference |
| --- | --- | --- | --- | --- |
| 2026-10-24 | 1.29 | Fixed the new-user shift dead end. The More shift action previously only navigated to Cashier, and the cart had no shift CTA; tapping Charge without a shift showed a non-actionable `cashierShiftRequired` message. Added a compact Mulai Shift sheet that preselects the active/default cashier, prefills starting cash to Rp 0 (editable), and opens from Charge, the cart summary, and More. Charge without a shift opens the sheet and auto-continues to payment after the shift opens; the cart and More entries return to the cart. The cart shows "Shift belum dibuka · Mulai shift" and a "Ganti kasir" action when a shift is active and the cart has items, using the existing empty-cart guard. Removed the blocking active-staff guard; a shift without an active staff uses the shift's cashier name. Files: `ShiftViewModel`, `ShiftUiState`, `ShiftHost`, `SwitchCashierSheet`, `CashierViewModel`, `CashierUiState`, `CashierActions`, `CashierRoute`, `CashierCart`, `MoreHubSections`, `ShiftLauncher`, and localized `shiftQuick*` strings. Verified with 707 unit tests (0 failures), `assembleDebug`, `assembleRelease` (R8 and `lintVitalRelease`), and `assembleDebugAndroidTest` compilation. Manual/device verification was not run; the owner will verify. | §6.1, §6.6, §15.1 Q13/Q16, §16 | owner report 2026-10-24 |
| 2026-10-24 | 1.28 | Implemented AdMob rewarded ad gate for Report export (`AdPlacement.EXCEL_EXPORT`). After format selection, PIN verification, and storage permissions, a rewarded video ad is requested via `AdMobAdController` before loading report content or writing the export file. If the user completes the ad and earns the reward, the file is written and the success toast appears. If dismissed early without earning the reward, export is aborted and localized feedback (`reportsAdRequired`) is displayed without writing a file. If the ad fails to load or show (offline, no fill, no current Activity, SDK error, or unexpected exception), the flow fails open and continues with the export, upholding the product promise that the owner is never locked out of business data. Debug builds use Google test rewarded unit `ca-app-pub-3940256099942544/5224354917`; release builds use production unit `ca-app-pub-5729173430518647/6119412059` (`pookie_rewards`). Server-side verification (SSV) and frequency capping are disabled. Current Activity lifecycle is tracked via `CurrentActivityProvider` registered in `PosApplication.onCreate` and cleared on pause/destroy so a backgrounded app does not present an ad. Verified with 694 unit tests (0 failures), `assembleDebug`, `assembleRelease` (R8 and `lintVitalRelease`), and a smoke test on an AOSP emulator where a rewarded test ad appeared and completing it wrote the export. Release APK measured 6.5 MB. The physical device and instrumented test suites were not run. | §7.8, §9.1, §15.1 Q7, §16 | owner request 2026-10-24; AdMob rewarded unit `ca-app-pub-5729173430518647/6119412059` (`pookie_rewards`), debug `ca-app-pub-3940256099942544/5224354917` |
| 2026-10-24 | 1.27 | Added the first live AdMob placement: one adaptive anchored banner at the bottom of Reports, below the report content and above the navigation bar. The Google Mobile Ads SDK is pinned to **24.0.0** because 24.1.0+ requires Kotlin 2.1.0 (the project uses 2.0.21) and 25.5.0 raises `minSdk` to 24 (the project targets 23). The manifest declares `INTERNET` and uses a build-type placeholder for the AdMob app ID; debug builds use Google's test app ID and test banner unit, release builds use the production app ID and `Pos_Reports_Banner`. `AdPlacement.REPORTS` was added to `AdPolicy` as a banner surface; the slot renders nothing while offline, during Compose previews, or when the ad fails or has no fill, so no blank space is reserved. The export toast lifts above a loaded banner. A compile-classpath-only force for `com.google.guava:listenablefuture:1.0` is required because the Ads SDK's empty-artifact constraint otherwise removes the class CameraX needs at compile time; the runtime keeps Guava. Core checkout, reports, and local backup remain offline-capable. Verified with 682 unit tests (0 failures), `assembleDebug`, `assembleRelease` (R8 and `lintVitalRelease`), and an AOSP emulator: a test banner rendered on Reports and disappeared after airplane mode with no leftover gap. Release APK measured 6.5 MB (debug 21 MB). The instrumented suite was not run. | §6.4, §7.8, §9.1, §9.2, §15.1 Q3, §16 | owner request 2026-10-24; AdMob app `ca-app-pub-5729173430518647~5768680380`, unit `Pos_Reports_Banner` |
| 2026-10-23 | 1.26 | Unified the top-of-screen headers for Cashier, Transactions, Products, Reports, and the More hub through one `CenteredScreenHeader`. Titles and subtitles are centered, header action buttons are removed, and a 28 dp bottom gap separates the header from content. Products keeps its add action as a floating bottom-right button on every width. Reports moves Export into the period-control row and removes the header calendar action; the custom period chip still opens the filter sheet. No data or behavior changes beyond the moved actions. Verified with 682 unit tests (0 failures), `assembleDebug`, and an emulator screenshot of Reports. | §5.1, §6.1, §6.2, §6.4, §6.5, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.25 | Revised the Transactions screen. The header is centered with only the title and count/period subtitle; the calendar and Add actions were removed. The search bar is borderless white with a soft 3 dp shadow. Status filters sit in one white 18 dp rounded container with a soft shadow and horizontal scrolling; active chips use the brand green. Transaction cards are borderless white with a 4 dp soft shadow, 20 dp radius, 20 dp padding, and 12 dp gaps; invoice is ExtraBold, date/method muted, and the amount ExtraBold. The expense table layout is replaced by the same card list at all widths. Verified with 682 unit tests (0 failures), `assembleDebug`, and an emulator screenshot. | §5.1, §6.4, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.24 | Added open and close motion to the Products add/edit drawer. Phone drawers slide in and out from the right with a fading scrim; tablet panels expand and shrink toward the right edge with a fade. The last form is retained during the exit animation, and the full-screen navigation state is released after the close animation so the bottom bar does not jump in mid-slide. No behavior, data, or validation changes. Verified with `testDebugUnitTest` (0 failures), `assembleDebug`, and Kotlin compilation. | §5.1, §6.2, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.23 | Added the page-based cashier checkout flow. The cart is now a full page with the shared product card, quantity steppers, swipe-left delete, and a dashed separator between subtotal and total. The payment page centers the total, shows cash and QRIS first with a More options reveal for transfer, debit, and credit, disables QRIS until a static payload exists, and shows the QR only after tapping Pay. Cash uses the system numeric keyboard with automatic thousand separators and helpful quick amounts derived from the total. A bottom-sheet confirmation shows total, cash received, and a highlighted change card, with Confirm & Pay and a ghost Cancel. Receipt printing and sharing are unchanged. Removed the cart line editor, discount, and negotiated unit price from the UI. Verified with 682 unit tests (0 failures), `assembleDebug`, and manual emulator checks of cart, swipe delete, payment, confirmation, and receipt. | §6.1, §7.3, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.22 | Removed the Cashier filter icon button and made the search field and barcode scan button white with a soft 2 dp shadow. The centered Cashier header title now uses ExtraBold weight. Category filtering remains available through the chip row; the in-stock-only option was only in the removed filter menu and is no longer reachable. Product cards are unchanged. Verified with `testDebugUnitTest` (0 failures), `assembleDebug`, and an emulator screenshot. | §5.1, §6.1, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.21 | Updated the Cashier UI. The header now centers the title and status line, removes all header action buttons including the cashier identity/open-shift pill, uses minimal top padding, and adds a 28 dp gap before the search row. All category chips now sit inside one white 18 dp rounded container with a soft 4 dp shadow and horizontal scrolling; inactive chips are transparent with muted text, active chips use the deep-emerald brand color with white text. The search bar and product cards are unchanged. Shift opening and switching remain available from More and from the cart action once a cart exists; an empty cart has no cashier-local open-shift control. Verified with `testDebugUnitTest` (0 failures), `assembleDebug`, and a manual emulator screenshot at phone width. | §5.1, §6.1, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.20 | Restyled the Reports screen without changing its layout or copy. The original structure is retained: one large summary card containing Total Sales, Net Profit, and Cost of Goods separated by a divider, followed by two side-by-side cards for Transaction Count and Average Transaction. Cards are borderless white surfaces with 24 dp/16 dp radii and soft 4–6 dp shadows on a `#F4F7F6` background. Typography uses Inter with 12–13 sp muted labels and a 34 sp Black Total Sales value positioned close to its label. Filter chips are pill-shaped with a deep-emerald active state. Export feedback remains a floating toast with Open/Share actions. The original filter sheet and export dialog use the same palette and radii. No supporting copy was added. Verified with `testDebugUnitTest` (0 failures), `assembleDebug`, and Kotlin compilation; visual QA is left to the owner. | §5.1, §6.4, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.19 | Removed the customer, kasbon, and split-payment features at the owner's request. Payment methods are now CASH, QRIS, and TRANSFER, and every sale must be fully paid. Customer pickers, due dates, debt recording, the customer hub entry, the debt report filter, and the SPLIT/KASBON payment options were removed. Schema v11 adds `MIGRATION_10_11`: it rebuilds `transactions` without `customer_id`, `remaining_debt`, `due_date`, and the customer foreign key, preserves `transaction_items`, drops `customers` and `debt_payments`, maps legacy SPLIT/KASBON rows to CASH, and maps PARTIAL_DEBT to COMPLETED. The backup table list no longer includes customers or debt payments, and demo data no longer creates customers or debt. Verified with 679 unit tests (0 failures) and `assembleDebug`; the migration instrumented test could not run because the tablet disconnected. | §3.1, §4, §6.3, §7.3, §8, §15 Q2/Q13, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.18 | Restored the Products page to the pre-redesign shell: title header with add action, search/sort bar, stock and category chips, and a responsive card grid (one column on phones, two on tablets). Products and the cashier share one compact row card. On phones the add action floats at the bottom-right above the navigation bar; tablets keep it in the header. The cashier catalog is a single-column list with 10 dp gaps and 20 dp side padding; each row keeps a 104 dp trailing action area with a 46 dp add button at quantity zero or a 104×40 counter above zero, plus a 2 dp focus outline. Selected navigation icons no longer shrink. Add/Edit is a right-side drawer: a slide-over on phones and a 40% inline panel on tablets, both rounded on the top-left and bottom-left corners. The Products page uses the shared app background `#F6F8F7`, matching the cashier page, and the search bar uses a 1 dp shadow. Tablet navigation stays on the left but adopts the phone navigation language: a narrower 88 dp floating dark rail, a white selected pill with the icon above its label, and white inactive icons. Demo seed data no longer contains the word "demo": categories are plain, SKUs use `SMP-`, and notes use `SMP`; clearing still removes legacy `DEMO-` records. No schema, repository, money-calculation, or destination changes. Verified on 2026-10-23 with `testDebugUnitTest` (0 failures), `assembleDebug`, `ProductsScreenTest` 5/5 and `AppRootTest` 6/6 on TB321FU, and a tablet screenshot at 100%. The full instrumented suite and lint were not re-run. | §4.1, §5.1, §5.3, §6.2, §6.5, §16 | owner request 2026-10-23 |
| 2026-10-23 | 1.17 | Redesigned the Products list and Add/Edit Product screen, and applied the shared product card to the cashier catalog. The Products screen now uses a gradient hero, compact stock-value summary, floating search and sort, sticky stock/category filters, a two-column card grid, a green add action, and a floating dark bottom navigation. Product cards show only neutral `Stok: N`, with category gradients (Makanan, Minuman, Sembako, Kebersihan, deterministic fallback palette) and a secondary menu for Duplikat/Arsipkan/Hapus. Add/Edit is a full-screen route with a live preview, category and base-unit dropdowns, side-by-side selling/cost price, equal stock and low-stock steppers, inline validation, a trash-icon delete confirmation, and a green save action. Opens the product card on the cashier catalog with no status badge, no dimming, and the existing quantity controls. Schema v10 adds the nullable `products.icon` column via `MIGRATION_9_10`; existing cost, threshold, SKU, and visibility fields reuse the existing `base_hpp`, `min_stock_alert`, `sku`, and `is_active` columns. No ViewModel/repository logic, money calculations, or other screens changed. Stack note: this repository is Android Kotlin/Compose/Room; the requested Spring Boot endpoints and Jakarta Validation do not apply. Verified only with `:app:compileDebugKotlin`; per owner instruction no tests, lint, emulator, or screenshot run was performed for this final revision. | §5.1, §6.2, §8, §16 | owner request 2026-10-23, `anti-slop/evidence/2026-10-23/products-redesign/` |
| 2026-10-23 | 1.16 | Redesigned the More ("Lainnya") hub into one store-and-shift hero, one grouped "Kelola" navigation surface, and a neutral debug-only "Alat" surface. The store block uses initials branding, shows the cashier once, and is display-only, removing the duplicate Settings path. Expected cash now has explicit loading, ready, and error-with-retry states. The tablet detail pane starts empty with a selection prompt instead of auto-opening Settings, and the selected row is visibly highlighted. Demo actions stack full-width at 135% so "Hapus Demo" no longer clips. No schema, repository, money-calculation, or destination changes. Verified on 2026-10-23 with 667 unit tests (0 failures), the 6 new MoreHubTest instrumented tests on `s24fe(AVD)` (0 failures), `assembleDebug`, and manual QA at phone 100%/135% and tablet 640 dp plus 1024 dp two-pane at 100%/135%; the 1024 dp layout used a temporary `wm size` override that was reset, and font scales were restored to 1.0. Evidence: `anti-slop/evidence/2026-10-23/redesign/`. The full instrumented suite and lint were not re-run. | §4, §5, §5.1, §5.3, §5.5, §6.5, §16 | `anti-slop/audit-001-2026-10-23.md`, `anti-slop/implementation-001-2026-10-23.md` |
| 2026-10-22 | 1.15 | Redesigned the Ganti Kasir sheet and the Staff screen. The cashier header now uses a tonal identity pill; the switch/close sheet uses a full-width segmented control, account rows with initials monograms, a tonal cash-summary surface without nested cards, a single primary action with an optional handover toggle, safe status-bar insets, and thousand-separated money inputs. The Staff screen uses initials monograms instead of the shared 3D avatar, removes the amber Aktif badge, and uses border-only cards. Behavior, money calculations, shift rules, and PIN policy are unchanged. Verified on 2026-10-22 with 667 unit tests (0 failures), 190 instrumented tests across `legion_tab(AVD)` and `s24fe(AVD)` (189 passed, 1 expanded-device test skipped, 0 failures), `assembleDebug`, and manual phone/tablet QA including the 135% font scale, status-bar insets, thousand-separated cash input, handover toggle, and the Kelola staf navigation. Lint still reports 14 pre-existing errors and 56 warnings, with no findings in the changed files. | §5.1, §5.4, §6.1, §6.6, §7.7, §15.1, §16 | `feat/switch-cashier`, inspection `switch-cashier-inspection.md`, preview `docs/preview/switch-cashier-preview.html` |
| 2026-10-22 | 1.14 | Redesigned the Staff Management screen: headline and softer subtitle, search action with filter field, active/inactive section labels, 16 dp staff cards, the provided 3D avatar from `docs/3D.png`, soft status badges, an overflow menu for edit/activate, and a bottom-right add FAB. Behavior, data, and permission rules are unchanged. Verified with 654 unit tests, 90 instrumented tests on `s24fe(AVD)` (89 pass, 1 expanded-device test skipped, 0 failures), `assembleDebug`, `assembleRelease`, phone/tablet screenshots, and WCAG checks for the Active and On-duty badges. | §5.2, §6.6, §16 | `docs/3D.png`, 2026-10-22 |
| 2026-10-22 | 1.13 | Added a staff directory and replaced free-text cashier entry in the operational shift flow with staff selection. **Settings → Staf & Kasir** adds, renames, and activates or deactivates staff; the directory is seeded with the previous settings cashier name or **Kasir Utama**. An active shift now offers **Ganti operator** (no cash count, shift stays open, active staff changes) and **Tutup shift** (cash count, expected versus actual, close and optionally open a new staff shift); either flow requires an empty cart. The owner-level `CriticalAction.SWITCH_CASHIER` PIN still gates close and switch; a per-cashier PIN is intentionally deferred. Schema version 8 adds `staffs`, staff links on `cashier_shifts` and `transactions`, and `app_settings.active_staff_id` through `MIGRATION_7_8`; backups include `staffs`, while older backups without it are accepted and seed a default staff. `cashier_name` snapshots remain for reprints and `staff_id` is the new identity link. Verified on 2026-10-22 with 654 unit tests (0 failures), 88 instrumented tests on `s24fe(AVD)` (87 passed, 1 expanded-device test skipped, 0 failed), `assembleDebug`, `assembleRelease`, and manual emulator QA of staff add/on-duty status, the two-tab operator/close flow, operator switching, and the staff selector. Lint still reports 14 pre-existing errors and 57 warnings with no staff/shift-specific findings. | §6.1, §6.6, §7.7, §8, §15, §15.1, §16 | `feat/switch-cashier` staff-directory commit `29b1daf`, 2026-10-22 |
| 2026-10-22 | 1.12 | Implemented switch kasir (shift handover): a shift is required before checkout while browsing and cart building stay available; opening needs no PIN; closing, switching, and ending are owner-PIN gated through `CriticalAction.SWITCH_CASHIER`; cash reconciliation uses `startingCash + cashSales + cashDebtPayments − cashRefunds` with refunds assumed cash; the cart must be empty before switching; transactions snapshot `cashier_name`; shift attribution uses the active shift's time window and no `shift_id` in the MVP; the cashier header, More card, and 30-shift history are part of the flow. Verified with 642 unit tests, 60 instrumented tests on the SM-S721B (59 pass, 1 skipped, 0 failures), `assembleDebug`, `assembleRelease`, and manual phone QA of the open/close/cash/PIN/cart-guard/variance/135% flows. The TB321FU tablet was disconnected for the final run. | §6.1, §6.6, §7.7, §8, §15, §15.1, §16 | `feat/switch-cashier` implementation, 2026-10-21–22 |
| 2026-10-21 | 1.11 | Made the four tablet report metric cards equal width (removed the 1.15/1.0 weights) and shortened the turnover label to `Total Penjualan` / `Total Sales` / `销售总额` so it no longer ellipsizes at the equal width. Emphasis on sales and profit remains through the larger value typography. Verified on the TB321FU tablet at 2560×1600 with `testDebugUnitTest` and `assembleDebug`. | §5.1, §6.4, §12, §16 | owner report 2026-10-21 |
| 2026-10-21 | 1.10 | Aligned the tablet navigation rail with the phone bottom bar: light `surface` background with a 1 dp `outlineVariant` right border, the same `TextPrimary`/`Accent` icon and label hierarchy, an `Accent` selection indicator, and the same 11 sp label size with the 135% font-scale cap. The rail pattern, width, destinations, navigation state, and test tags are unchanged. Verified with `testDebugUnitTest` and `assembleDebug`. | §4.1, §5.1, §12, §16 | owner request 2026-10-21 |
| 2026-10-21 | 1.9 | Renamed the product from UMKM POS Pro to Pookie POS. Updated the launcher label (`app_name`), the welcome and receipt strings in all three languages, the receipt fallback store name, the recovery-code print text, and the product documentation. Package name, applicationId, theme name, and the signing certificate DN are unchanged. | §1, §16 | owner request 2026-10-21 |
| 2026-10-21 | 1.8 | Replaced the Android launcher icon with the green-and-white P mark from `newicon.jpeg`, with increased padding around the P. Regenerated the adaptive foreground, all legacy square and round mipmaps, changed the adaptive background to `#FCFFFE`, and added a monochrome layer for themed icons. | §5.2, §16 | `newicon.jpeg` |
| 2026-10-21 | 1.7 | Removed dark mode by owner decision. The app now always renders the light theme; the dark palette, night resources, and the system-theme requirement were removed. | §3.1, §5.1, §9.3, §12, §15.1, §16 | owner decision 2026-10-21 |
| 2026-10-20 | 1.6 | PIN Phase 2–3 implemented: security settings and recovery-code screens (one-time code, print to PDF, mandatory acknowledgement), audit history, and the global gate on all critical actions with profit/HPP masking. §7.7 now states the feature is implemented and lists the gated actions. | §7.7, §16 | `docs/security/prd-code-reconciliation.md` change record (Phase 2–3) |
| 2026-10-20 | 1.5 | PIN Phase 0–1 implemented: shared security model and contracts, database version 6 with `MIGRATION_5_6` (PIN state columns plus `security_audit_log`), salted PBKDF2 hashing, the PIN policy and `SecurityService`, and the backup exclusion with a forced PIN reset after restore. §7.7 now separates what is implemented from what Phase 2–3 still owes. No action is gated yet. Verified: `testDebugUnitTest` (564 tests) and `assembleDebug` succeed; instrumented tests were not run. | §7.7, §8, §15.1, §16 | `docs/security/prd-code-reconciliation.md` change record |
| 2026-10-20 | 1.4 | Recorded the owner-delegated decisions for Q1–Q11 in §15.1 and marked the deferred items. Records decisions only; no behaviour changed by that revision. | §15, §15.1, §16 | owner delegation, 2026-10-20 |
| 2026-10-20 | 1.3 | Reconciled the data model, backup, and PIN statements with the code: corrected the `cashier_shifts` column list, listed the entities that exist in code but were missing from §8, recorded that `security_store` was exported at the time, marked biometric recovery as an open decision, recorded the owner's recovery-code-only decision, fixed the `adb shell monkey` command, corrected the stale PIN test note, and added §15. No product decision was made in that revision. | §3.1, §6.6, §7.6, §7.7, §8, §12, §15 | `docs/security/prd-code-reconciliation.md` |
| — | 1.2 | Responsive UX & Visual Reliability: layout contract, viewport screenshot matrix, iconography, density rules. | §4.1, §5, §9.4, §12 | previous revision, not re-verified in this pass |
