# 🤖 AGENTS.md — Bhumivera Scanner Agent Directives

> **APPLIES TO ALL AUTONOMOUS AGENTS (Antigravity, Gemini, Claude, GPT, Cursor, Cline, Windsurf, Aider):**

Welcome, Agent. You have arrived at the **`Bhumivera_Scanner`** repository.

## 🛑 MANDATORY FIRST ACTION
Do NOT guess or analyze the scanner codebase from scratch.
**Your very first action MUST be to inspect:**
* [`CODEBASE_MEMORY.md`](./CODEBASE_MEMORY.md) — Hardware pipeline, architecture, and API contracts.

---

## 📊 CURRENT SYSTEM BASELINE COUNTERS
* **Kotlin Source Files**: 10 files
  1. `MainActivity.kt` — Host activity & permissions
  2. `ui/BhumiveraApp.kt` — Tab navigation (Scan, POS, Queue, Settings)
  3. `ui/ScannerScreen.kt` — Viewfinder, reticle, audio/haptic trigger
  4. `ui/Theme.kt` — Obsidian & gold design tokens
  5. `scanner/CameraScannerView.kt` — CameraX lifecycle binding
  6. `scanner/BarcodeAnalyzer.kt` — ML Kit sub-15ms QR & EAN-13 frame analyzer
  7. `scanner/QrCodeGenerator.kt` — ZXing UPI dynamic QR generator
  8. `data/BhumiveraService.kt` — OkHttp client with Keep-Alive connection pool
  9. `data/Models.kt` — DTOs
  10. `data/SessionStore.kt` — Encrypted SharedPreferences
* **Target SDK**: Android 7.0+ (API 24 to 35)

---

## ✍️ SELF-UPDATING REQUIREMENT
Whenever you add, modify, or delete a Kotlin source file or composable:
1. Recalculate file and composable counts.
2. Update the counter in [`CODEBASE_MEMORY.md`](./CODEBASE_MEMORY.md).
3. Document the delta (`Previous Count -> New Count`) and rationale in the memory log.
