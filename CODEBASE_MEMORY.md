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
