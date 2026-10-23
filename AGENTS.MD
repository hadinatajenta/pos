# Pookie POS — Project Instructions

## Source of truth

- Read `docs/prd.md` before planning or implementing any feature.
- Treat `docs/prd.md` as the product requirements source of truth. If code, assumptions, or a chat prompt conflicts with it, call out the conflict before changing behavior.
- Inspect the existing repository and current implementation before creating, moving, or replacing files. This is an existing Android POS project, not a greenfield demo.
- Preserve working behavior unless the task explicitly asks to change it.
- Ask for clarification only when a decision materially affects money calculations, data integrity, security, architecture, or user flow. Otherwise choose the smallest reasonable implementation and document the assumption.

## Code intelligence — mandatory

`.codegraph/` exists in this repository, so CodeGraph replaces pre-emptive file reading for all code questions.

- Call `codegraph_explore` or `codegraph_node` before `read`, `grep`, or `glob` on any Kotlin, XML, or Gradle source under `app/`.
- Known symbol or file → `codegraph_node`. Flow or unknown location → `codegraph_explore` with `maxFiles: 3`.
- Source returned by CodeGraph is already read. Do not re-open it, and do not re-verify it with `grep`.
- Read a file directly only when CodeGraph returns no match, flags the file as changed or omitted, or the target is not code (docs, config, resources).
- Shell fallback: `codegraph explore "<symbols or question>"`, `codegraph node <symbol>`, `codegraph callers|callees|impact <symbol>`.

## Documentation of changes — mandatory

Every change must be documented in the same task that makes it. An undocumented change is an incomplete change.

- Always record **the date** in ISO form (`YYYY-MM-DD`) and **the PRD reference** (the section numbers in `docs/prd.md`, plus requirement IDs such as `R-31` or `C-B2` from `docs/security/prd-code-reconciliation.md` when relevant).
- State what changed, why it changed, which files, schema objects, or user-visible behavior were touched, and exactly what was verified.
- Update `docs/prd.md` §16 (Revision History) whenever product behavior, a requirement, the schema contract, or any other statement in the PRD changes.
- Update `docs/prd-gaps.md` whenever implementation status changes.
- Keep the record in the repository. A chat message is not documentation.
- If a change would contradict `docs/prd.md` and the resolution is not yet approved, record it as an open decision in `docs/prd.md` §15 instead of changing behavior.
- Never record a change without its date, and never leave a dated entry without the PRD section it affects.

## Product priorities

1. Correct and reliable transaction calculations.
2. Offline-first operation and merchant ownership of data.
3. Fast, low-friction cashier workflow.
4. Clean, modern, restrained UI that remains usable on low-end Android devices.
5. Maintainable code and reproducible verification.

## Required stack and constraints

Follow the existing project configuration and `docs/prd.md`. The intended stack is:

- Kotlin 2.0+ and Android SDK configuration defined by the project/PRD.
- Jetpack Compose with Material 3.
- Room/SQLite for local persistence, using the database configuration specified in the PRD.
- Coroutines for asynchronous work; do not block the main thread with database, printer, scanner, export, or backup operations.
- Keep dependencies lean. Check whether an existing library or project abstraction already solves the problem before introducing a new dependency.
- Keep core checkout and local data operations usable offline.
- Never put ads in the cashier, cart, payment, transaction-success, or receipt-printing flow.

Do not upgrade Kotlin, Gradle, Android Gradle Plugin, SDK levels, or major dependencies unless the task requires it. Explain the compatibility reason before making a broad upgrade.

## Architecture and code quality

- Follow existing package naming, module boundaries, naming conventions, and patterns unless they conflict with the PRD.
- Keep UI, domain/business rules, persistence, and hardware integration separated.
- Put shared models and interfaces in the agreed `core/model/` and `core/contract/` locations if those packages are used by this repository.
- Do not let parallel agents change shared contracts without explicit approval from the orchestrator.
- Prefer small, focused changes over sweeping rewrites.
- Do not put business calculations directly inside composables.
- Use immutable UI state and unidirectional data flow where consistent with the existing architecture.
- Represent money in whole Rupiah using `Long` as required by the PRD. Do not use floating-point values for currency amounts.
- Use explicit, tested rounding rules at currency boundaries.
- Handle database, file, network, USB, Bluetooth, and printer failures gracefully. Do not swallow exceptions silently.
- Never log PINs, recovery answers, customer-sensitive information, or other secrets.
- Do not introduce fake data into production flows or claim a feature works when it is only a placeholder.

## UI/UX rules — mandatory

- Build a simple, clean, modern Material 3 interface. Prefer neutral surfaces, restrained accent color, clear typography, consistent spacing, and one primary action per screen.
- Use one consistent icon family. Pair icons with labels for important navigation/actions; do not use emoji as UI icons.
- Keep text concise. Avoid stacked helper paragraphs, decorative cards, excessive borders, gradients, and oversized empty panels.
- Optimize the cashier flow first. Make product search, cart quantity changes, total, payment, and transaction completion easy to find.
- Use progressive disclosure for infrequent or advanced fields; do not show every option at once.
- Use responsive Compose layouts and current window constraints/window size classes rather than fixed screen widths or absolute offsets.
- Compact phone windows: bottom navigation only. Expanded tablet windows: navigation rail or side navigation only when appropriate. Do not show a permanent sidebar and bottom navigation at the same time.
- Never allow clipping, overlap, horizontal screen overflow, content hidden behind system bars/navigation/keyboard, or controls outside the visible viewport.
- Apply system-bar and IME insets correctly. The last form field and primary action must remain reachable when the keyboard is open.
- Support font scaling from 100% to 135% without clipping critical values or actions.
- Do not rely on color alone to communicate stock, error, payment, or selection states.
- Ad containers must disappear entirely when ads are unavailable/offline; do not leave blank reserved space.

## Data safety and finance correctness

- Never perform a destructive database migration without a migration plan and tests proving existing user data is preserved.
- Backup/restore and import/export must validate input and report failures clearly.
- Keep local backup possible offline. An unavailable ad or cloud service must never block local backup.
- Do not lock the owner out of their business data because a PIN or cloud service is unavailable.
- Add unit tests for pricing tiers, unit conversions, negotiated prices, discounts, taxes, rounding, HPP, returns, debt, and partial payments whenever the relevant logic changes.
- Use transactions for multi-step database updates that must succeed or fail together.

## Parallel-agent protocol

- Follow the roles and directory ownership in `.agents/agents.md`.
- Before delegating work, the orchestrator must inspect the project and establish shared interfaces/data contracts in a sequential Phase 0.
- Parallelize only tasks with clear directory ownership and low coupling.
- Agents must not edit files owned by another agent without explicit coordination.
- Shared Gradle files, navigation roots, application composition, `core/model/`, and `core/contract/` are orchestrator-owned unless the orchestrator explicitly assigns them.
- Each agent must report: files changed, public APIs/contracts introduced, tests run, test results, assumptions, and known gaps.
- The orchestrator integrates changes and resolves conflicts; do not leave integration to the user.

## Required workflow for every task

1. Read `docs/prd.md`. Locate existing code with CodeGraph (see "Code intelligence"), then inspect only what it returned.
2. Summarize the intended change and likely affected files before broad edits.
3. Implement the smallest complete vertical slice; avoid unrelated refactors.
4. Add or update automated tests for business logic and state transitions.
5. Run the most relevant checks. At minimum, use the commands that apply to the repository:
   - `./gradlew testDebugUnitTest`
   - `./gradlew assembleDebug`
   - `./gradlew installDebug` when a physical device/emulator is connected and installation testing is appropriate.
6. For responsive UI work, inspect actual screenshots at the PRD's required phone/tablet sizes and font scales. Use emulator/device screenshots or `scrcpy` where available.
7. Review diffs for accidental changes, hardcoded secrets, unsafe data deletion, duplicated logic, and unrelated formatting churn.
8. Report what was actually tested. Never claim browser/device/GPU/rendering validation unless it really ran.
9. Document the change with its date and the PRD sections it touches (see "Documentation of changes — mandatory"), updating `docs/prd.md` §16 when the PRD itself changed, before reporting the task as done.

## Definition of done

A task is not done merely because code was generated or the project compiles. It is done when:

- The implementation matches `docs/prd.md` and the explicit request.
- The relevant tests and build checks have been run or their blockers reported.
- Existing affected features have been regression-checked.
- UI changes have been visually checked at relevant viewport sizes.
- Data safety, errors, empty states, and loading states have been considered.
- The final response summarizes changes, verification, and known limitations.
- The change is documented with its date and its PRD reference, and `docs/prd.md` §16 is updated whenever the PRD itself changed.

## Physical device rules (mandatory)

A connected device can hold real test data that the owner cares about. Never run a command that removes
an app or wipes its data unless the owner explicitly asks for it.

- Replacing a build is fine: `adb -s <serial> install -r --user 0 <apk>`. It keeps the app data.
- Do not run `adb uninstall`, `adb shell pm clear`, or `adb shell pm uninstall` on a connected device.
  If a build genuinely cannot be replaced, stop and ask the owner first.
- `connectedDebugAndroidTest` removes the app APKs after the run by default, which deletes the owner's
  data. Always run it as:
  `./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`
- On a device with a Secure Folder or a second profile, always pass `--user 0` so the app lands in the
  owner's own profile instead of the other one.
- Device settings changed for a test (dark mode, font scale, permissions) are restored before the
  session ends, and the owner is told what was changed.
