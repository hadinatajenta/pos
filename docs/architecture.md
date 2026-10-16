# Architecture and Shared Contracts

Source of truth for product behavior is `docs/prd.md`. This document records the
structural decisions the orchestrator froze during Phase 0. Agents build against
these shapes and do not change them without orchestrator approval.

## Layout

- Single Gradle module `:app`.
- Base package and applicationId: `id.umkm.pos.pro`.
- `minSdk 23`, `targetSdk 35`, `compileSdk 35`.
- Money is always whole Rupiah in `Long`. Quantities and unit conversion factors
  are `Double`. `Long` is never used for quantities and `Double` is never used
  for money.

## Ownership

| Owner | Paths under `app/src/main/java/id/umkm/pos/pro/` |
| --- | --- |
| Orchestrator | `app/`, `core/model/`, `core/contract/`, all `*.gradle.kts`, `gradle/libs.versions.toml` |
| Agent 1 — Data & Storage | `data/local/`, `data/backup/` |
| Agent 2 — POS & Accounting | `domain/pos/`, `domain/accounting/` |
| Agent 3 — Hardware & Receipts | `hardware/printer/`, `hardware/scanner/`, `hardware/receipt/` |
| Agent 4 — Reports & Ads | `feature/reports/`, `monetization/admob/` |
| Agent 5 — UI & Accessibility | `ui/theme/`, `ui/cashier/`, `ui/tablet/`, `ui/security/` |

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
  `SecurityRepository`. `cashier_shifts` does not carry PIN fields.
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

`AppContainer` lists only dependencies that have implementations. Deferred
services (backup, import/export, AdMob) are added to the container when their
implementations land.

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
