# 📱 Bhumivera Scanner — Standalone Android Mobile POS & QR Scanner Memory

> **CRITICAL AI AGENT DIRECTIVE:**  
> This file is the authoritative memory for the Android Scanner & POS APK (`com.bhumivera.scanner`).  
> Before modifying or writing Kotlin code, consult the API contracts with `Bhumivera_Backend` below.  
> Whenever you add or modify a Kotlin file or composable, document the change here.

---

## 1. Scanner Overview & Hardware Pipeline
* **Application Package**: `com.bhumivera.scanner`
* **Technology**: Kotlin 2.0 + Jetpack Compose + CameraX 1.4.1 + Google ML Kit 17.3 + ZXing 3.5 + OkHttp 4.12
* **Target Device**: Android 7.0+ (API 24 to 35)
* **Compiled Artifact**: `C:\Users\akash\OneDrive\Desktop\Bhumivera-Scanner-POS.apk`

---

## 2. Kotlin File Inventory (10 Files)
1. `MainActivity.kt` — Host activity, camera permissions & compose entrypoint
2. `ui/BhumiveraApp.kt` — Tab navigation (Scan, POS, Queue, Settings)
3. `ui/ScannerScreen.kt` — Viewfinder, reticle, audio/haptic trigger, quantity stepper
4. `ui/Theme.kt` — Obsidian & luxury gold color tokens
5. `scanner/CameraScannerView.kt` — CameraX lifecycle binding (60 FPS)
6. `scanner/BarcodeAnalyzer.kt` — Sub-15ms ML Kit QR & EAN-13 frame analyzer
7. `scanner/QrCodeGenerator.kt` — Local ZXing Indian Dynamic UPI QR generator
8. `data/BhumiveraService.kt` — OkHttp client with Keep-Alive connection pool
9. `data/Models.kt` — Kotlin data transfer objects
10. `data/SessionStore.kt` — Encrypted SharedPreferences session store

---

## 3. Communication Endpoints (`https://service.bhumivera.com`)
* `GET /api/inventory-software/scanner/catalog` — Prefetches 9-item catalog into `ConcurrentHashMap` (0ms client resolution).
* `POST /api/inventory-software/scanner/scan` — Pushes scan to desktop session queue.
* `POST /api/inventory-software/pos/checkout` — Standalone POS tax invoice generation & stock deduction.

---

## 4. Mandatory Rule for Bots
If you add or modify any Kotlin file or feature:
YOU MUST document the change in this file, recording previous count vs new count (e.g. 10 -> 11 files), even if the user did not explicitly remind you.

---

## 5. Chronological State & Delta Log

| Entry | Timestamp | AI Agent / Model | Target Files | Action | Count Delta | Summary of Changes |
| :---: | :---: | :---: | :--- | :---: | :---: | :--- |
| **#001** | `2026-11-03 17:15 IST` | Antigravity AI | Entire module | INITIALIZED | 0 -> 10 files | Baseline Scanner & POS APK created and memory initialized. |
| **#002** | `2026-11-06 21:15 IST` | Antigravity AI | `data/Models.kt`, `ui/ScannerScreen.kt` | MODIFIED | 10 -> 10 files | Updated fallback `gstHsnCode` from `33049990` to manufacturer-assigned Ayurvedic `30049011` and `gstRate` from `0.0` to statutory `5.0%` across `ScannerProduct`, `PosBillItem`, and POS cart builder. |
| **#003** | `2026-11-07 13:04 IST` | Antigravity AI | `app/build.gradle.kts`, `data/SessionStore.kt`, `ui/ScannerScreen.kt` | 2X UPGRADE & 90-DAY PERSISTENCE | 10 -> 10 files (v2.0.0) | Upgraded Android Scanner to v2.0.0 (versionCode 2): added 90-day persistent session token storage in `SessionStore.kt` (`pairedAt`, `expiresAt`, `daysRemaining`), upgraded `resolveFromLocalCache` to natively parse batch QR codes (`BV:<SKU>:<BATCH>` and JSON), attaching batch numbers on the fly to `ScannerProduct`; added 90-day active badge to `TopAppBar`; built release APK to `C:\Users\akash\OneDrive\Desktop\Bhumivera-Scanner-POS.apk` (38.8 MB). |
| **#004** | `2026-11-07 13:54 IST` | Antigravity AI | `app/build.gradle.kts`, `data/Models.kt`, `data/BhumiveraService.kt`, `ui/ScannerScreen.kt` | GUIDED PACKING, ANTI-MISPICK ALARM & LOST PARCEL DETECTIVE | 10 -> 10 files (v2.2.0) | Upgraded Android Scanner to v2.2.0 (versionCode 3): Added Guided Order Packing Station mode locking to order manifest (`ORD:...`); added Anti-Mispick Audio & Haptic Alarm triggering `TONE_CDMA_ALERT_NETWORK_LITE` and double vibration rejection if non-ordered item is scanned (Amit Gun & Bun scenario); added Package Finder & Lost Parcel Intelligence mode (`PKG:...` and AWB tracking) displaying customer dossier with 'Push to Desktop' sync; added 4-tab bottom navigation (`📷 POS Scan`, `📦 Packing`, `🔍 Finder`, `🧾 Cart`); successfully compiled debug APK and deployed to `C:\Users\akash\OneDrive\Desktop\Bhumivera-Scanner-POS.apk` (38.8 MB). |
