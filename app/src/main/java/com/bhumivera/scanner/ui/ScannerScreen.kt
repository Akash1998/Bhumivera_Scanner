package com.bhumivera.scanner.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhumivera.scanner.data.BhumiveraService
import com.bhumivera.scanner.data.PosBillItem
import com.bhumivera.scanner.data.PosCheckoutRequest
import com.bhumivera.scanner.data.PosCustomer
import com.bhumivera.scanner.data.PosSaleRecord
import com.bhumivera.scanner.data.ScannerLookupRequest
import com.bhumivera.scanner.data.ScannerProduct
import com.bhumivera.scanner.data.ScannerQueueClearRequest
import com.bhumivera.scanner.data.ScannerQueueItemRequest
import com.bhumivera.scanner.data.ScannerQueueRemoveRequest
import com.bhumivera.scanner.data.ScannerScanRequest
import com.bhumivera.scanner.data.SessionStore
import com.bhumivera.scanner.scanner.CameraScannerView
import com.bhumivera.scanner.scanner.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Luxury Brand Palette
private val GoldAccent = Color(0xFFD4AF37)
private val DeepEmerald = Color(0xFF14251C)
private val DarkSurface = Color(0xFF0D1812)
private val CardEmerald = Color(0xFF193225)
private val MintGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    service: BhumiveraService,
    sessionStore: SessionStore,
    onBackToStore: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Camera & Hardware State
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }

    // Navigation Tab: 0 = Scanner Viewfinder, 1 = Active E-Bill POS
    var selectedTab by remember { mutableIntStateOf(0) }

    // 10x Intelligent In-Memory Catalog Cache
    val localCatalog = remember { mutableStateListOf<ScannerProduct>() }
    val localLookupMap = remember { mutableMapOf<String, ScannerProduct>() }
    var isCatalogSynced by remember { mutableStateOf(false) }
    var isCatalogLoading by remember { mutableStateOf(false) }

    // Active Scanned Item Card
    var activeScannedProduct by remember { mutableStateOf<ScannerProduct?>(null) }
    var scanQuantity by remember { mutableIntStateOf(1) }
    var manualInputCode by remember { mutableStateOf("") }
    var isSearchingCode by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready to scan QR / Barcodes") }

    // ----------------------------------------------------
    // GUIDED ORDER PACKING STATION STATE (ZERO-MISPICK)
    // ----------------------------------------------------
    var activeOrder by remember { mutableStateOf<com.bhumivera.scanner.data.OrderManifestOrder?>(null) }
    val manifestItems = remember { mutableStateListOf<com.bhumivera.scanner.data.OrderManifestItem>() }
    var packingErrorAlert by remember { mutableStateOf<String?>(null) }
    var packingSuccessNotice by remember { mutableStateOf<String?>(null) }
    var packingOrderInput by remember { mutableStateOf("") }
    var isLoadingManifest by remember { mutableStateOf(false) }

    // ----------------------------------------------------
    // PACKAGE FINDER & LOST PARCEL INTELLIGENCE STATE
    // ----------------------------------------------------
    var activePackageData by remember { mutableStateOf<com.bhumivera.scanner.data.PackageLookupData?>(null) }
    var packageSearchInput by remember { mutableStateOf("") }
    var isSearchingPackage by remember { mutableStateOf(false) }
    var packageErrorNotice by remember { mutableStateOf<String?>(null) }
    var packageDesktopSyncNotice by remember { mutableStateOf<String?>(null) }

    // POS Cart & Billing State
    val posCart = remember { mutableStateListOf<PosBillItem>() }
    var customerName by remember { mutableStateOf("Walk-in Retail Shopper") }
    var customerPhone by remember { mutableStateOf("") }
    var customerGstin by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("UPI") } // UPI, Cash, Card
    var isCheckingOut by remember { mutableStateOf(false) }
    var completedInvoice by remember { mutableStateOf<PosSaleRecord?>(null) }

    // Settings & Connection Dialog
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showInvoiceDialog by remember { mutableStateOf(false) }
    var showPairingSuccessDialog by remember { mutableStateOf(false) }
    var pairingSuccessMessage by remember { mutableStateOf("") }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        // 10x Turbo Catalog Prefetch & Seed
        seedDefaultLuxuryProducts(localCatalog, localLookupMap)
        isCatalogLoading = true
        try {
            val response = service.scannerCatalog(sessionStore.scannerApiKey())
            if (response.success && response.items.isNotEmpty()) {
                localCatalog.clear()
                localCatalog.addAll(response.items)
                localLookupMap.clear()
                localLookupMap.putAll(response.lookupMap)
                indexProductsIntoMap(response.items, localLookupMap)
                isCatalogSynced = true
                statusMessage = "10x Turbo Ready • ${response.items.size} Formulations Cached"
            }
        } catch (_: Exception) {
            statusMessage = "Using Offline Local Catalog • 9 Formulations"
        } finally {
            isCatalogLoading = false
        }
    }

    // REAL-TIME 2-WAY SYNC: Poll backend queue periodically to sync live cart with website
    LaunchedEffect(sessionStore.sessionCode(), sessionStore.scannerApiKey()) {
        while (true) {
            try {
                val qRes = service.scannerQueue(
                    sessionCode = sessionStore.sessionCode(),
                    apiKey = sessionStore.scannerApiKey()
                )
                if (qRes.success) {
                    val qItems = qRes.queue
                    val differs = qItems.size != posCart.size || qItems.any { q ->
                        val local = posCart.find { it.sku == q.sku }
                        local == null || local.quantity != q.quantity
                    }
                    if (differs) {
                        posCart.clear()
                        qItems.forEach { q ->
                            posCart.add(
                                PosBillItem(
                                    id = q.metaId ?: 0,
                                    productId = q.productId ?: 0,
                                    name = q.productName,
                                    sku = q.sku,
                                    barcode = q.barcode ?: q.sku,
                                    quantity = q.quantity,
                                    price = q.unitPrice,
                                    mrp = q.mrp,
                                    gstRate = 5.0,
                                    gstHsnCode = q.gstHsnCode ?: "30049011",
                                    availableStock = q.availableStock
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
            delay(2000)
        }
    }

    // Function to load order manifest by order ID or token
    fun loadOrderManifest(orderIdOrCode: String) {
        val raw = orderIdOrCode.trim()
            .removePrefix("ORD:")
            .removePrefix("ord:")
            .removePrefix("BV-ORD-")
            .removePrefix("#")
            .trim()
        val orderId = raw.toIntOrNull()
        if (orderId == null || orderId <= 0) {
            packingErrorAlert = "Invalid Order ID: '$orderIdOrCode'. Please scan a valid order QR (e.g. ORD:123456)."
            triggerMismatchAlarmAndHaptic(context)
            return
        }

        isLoadingManifest = true
        packingErrorAlert = null
        packingSuccessNotice = null
        scope.launch {
            try {
                val res = service.orderManifest(orderId)
                if (res.success && res.order != null) {
                    activeOrder = res.order
                    manifestItems.clear()
                    manifestItems.addAll(res.items)
                    triggerBeepAndHaptic(context)
                    statusMessage = "📦 Manifest Loaded: Order #${res.order.orderNumber} (${res.order.customerName})"
                    selectedTab = 1
                } else {
                    triggerMismatchAlarmAndHaptic(context)
                    packingErrorAlert = res.message ?: "Order #$orderId not found in manifest database."
                }
            } catch (e: Exception) {
                triggerMismatchAlarmAndHaptic(context)
                packingErrorAlert = "Failed to load manifest for Order #$orderId: ${e.message}"
            } finally {
                isLoadingManifest = false
            }
        }
    }

    // Function to lookup lost or misplaced parcel by barcode or QR
    fun lookupPackage(packageCode: String) {
        val clean = packageCode.trim()
        if (clean.isBlank()) return
        isSearchingPackage = true
        packageErrorNotice = null
        packageDesktopSyncNotice = null
        scope.launch {
            try {
                val res = service.packageLookup(clean)
                if (res.success && res.packageData != null) {
                    activePackageData = res.packageData
                    triggerBeepAndHaptic(context)
                    statusMessage = "🔍 Package Found: ${res.packageData.packageId} (Order #${res.packageData.orderNumber})"
                    selectedTab = 2
                } else {
                    triggerMismatchAlarmAndHaptic(context)
                    packageErrorNotice = res.message ?: "No package record found matching '$clean'."
                }
            } catch (e: Exception) {
                triggerMismatchAlarmAndHaptic(context)
                packageErrorNotice = "Package lookup failed: ${e.message}"
            } finally {
                isSearchingPackage = false
            }
        }
    }

    // Helper to resolve scanned string (Pairing QR / URL / SKU / Barcode / Slug)
    fun processScannedRawText(rawText: String) {
        val clean = rawText.trim()
        if (clean.isBlank()) return

        // 1. AUTO-PAIRING RECOGNITION (When scanning Pairing QR from screen!)
        if (clean.contains("BHUMIVERA_ANDROID_QR_SCANNER") || (clean.startsWith("{") && clean.contains("scannerApiKey"))) {
            try {
                val json = org.json.JSONObject(clean)
                val key = json.optString("scannerApiKey")
                val sessionCode = json.optString("sessionCode")
                val endpoint = json.optString("endpoint")
                if (key.isNotBlank()) {
                    sessionStore.saveScannerApiKey(key)
                }
                if (sessionCode.isNotBlank()) {
                    sessionStore.saveSessionCode(sessionCode)
                }
                if (endpoint.isNotBlank()) {
                    val base = endpoint.substringBefore("/api/")
                    if (base.isNotBlank()) {
                        sessionStore.saveBackendUrl(base)
                    }
                }
                sessionStore.savePairedAt(System.currentTimeMillis())
                sessionStore.saveExpiresAt(System.currentTimeMillis() + SessionStore.NINETY_DAYS_MILLIS)
                triggerBeepAndHaptic(context)
                pairingSuccessMessage = "Successfully Paired with Warehouse Dock Station!\n\nStation: " + sessionStore.sessionCode() + "\nToken Validity: 90 Days Active (" + sessionStore.daysRemaining() + " days remaining)\nScanner API Key: " + sessionStore.scannerApiKey() + "\nEndpoint: " + sessionStore.backendUrl() + "\n\nNow point camera at product batch QR stickers or barcodes to pick, pack, and bill!"
                showPairingSuccessDialog = true
                statusMessage = "🟢 Station Linked: " + sessionStore.sessionCode() + " (90d Active)"
                return
            } catch (_: Exception) {}
        }

        if (clean.startsWith("BV-SCAN-")) {
            sessionStore.saveScannerApiKey(clean)
            sessionStore.savePairedAt(System.currentTimeMillis())
            sessionStore.saveExpiresAt(System.currentTimeMillis() + SessionStore.NINETY_DAYS_MILLIS)
            triggerBeepAndHaptic(context)
            pairingSuccessMessage = "Successfully Paired with Warehouse Dock Station!\n\nScanner API Key: " + clean + "\nToken Validity: 90 Days Active (" + sessionStore.daysRemaining() + " days remaining)\nStation: " + sessionStore.sessionCode() + "\nEndpoint: " + sessionStore.backendUrl() + "\n\nNow point camera at product batch QR stickers or barcodes to pick, pack, and bill!"
            showPairingSuccessDialog = true
            statusMessage = "🟢 Station Linked with Key: " + clean
            return
        }

        // 2. INSTANT 0ms IN-MEMORY RESOLUTION (Matches URLs, SKUs, Slugs, Barcodes)
        val matched = resolveFromLocalCache(clean, localLookupMap, localCatalog)
        if (matched != null) {
            triggerBeepAndHaptic(context)
            if (sessionStore.isAutoAddMode()) {
                addItemToPosCart(posCart, matched, 1)
                scope.launch {
                    try {
                        service.scannerQueueUpdateItem(
                            ScannerQueueItemRequest(
                                sessionCode = sessionStore.sessionCode(),
                                sku = matched.displaySku,
                                metaId = matched.displayMetaId,
                                delta = 1,
                                price = matched.displayPrice,
                                name = matched.name,
                                availableStock = matched.displayStock,
                                scannerApiKey = sessionStore.scannerApiKey()
                            )
                        )
                    } catch (_: Exception) {}
                }
                statusMessage = "⚡ Auto-Added: ${matched.name}"
                activeScannedProduct = null
            } else {
                activeScannedProduct = matched
                scanQuantity = 1
                statusMessage = "Scanned: ${matched.name}"
            }
            return
        }

        // 3. LIVE CLOUD RESOLUTION
        scope.launch {
            isSearchingCode = true
            statusMessage = "Looking up: $clean..."
            try {
                val lookupRes = service.scannerLookup(
                    ScannerLookupRequest(
                        code = clean,
                        scannerApiKey = sessionStore.scannerApiKey()
                    )
                )
                val product = lookupRes.item ?: extractSkuOrSlugFallback(clean, localCatalog)
                if (product != null) {
                    triggerBeepAndHaptic(context)
                    indexSingleProduct(product, localLookupMap)
                    if (sessionStore.isAutoAddMode()) {
                        addItemToPosCart(posCart, product, 1)
                        try {
                            service.scannerQueueUpdateItem(
                                ScannerQueueItemRequest(
                                    sessionCode = sessionStore.sessionCode(),
                                    sku = product.displaySku,
                                    metaId = product.displayMetaId,
                                    delta = 1,
                                    price = product.displayPrice,
                                    name = product.name,
                                    availableStock = product.displayStock,
                                    scannerApiKey = sessionStore.scannerApiKey()
                                )
                            )
                        } catch (_: Exception) {}
                        statusMessage = "⚡ Auto-Added: ${product.name}"
                        activeScannedProduct = null
                    } else {
                        activeScannedProduct = product
                        scanQuantity = 1
                        statusMessage = "Scanned: ${product.name} (Stock: ${product.displayStock})"
                    }
                } else {
                    statusMessage = lookupRes.message ?: "No product found matching '$clean'"
                }
            } catch (e: Exception) {
                val fallback = extractSkuOrSlugFallback(clean, localCatalog)
                if (fallback != null) {
                    triggerBeepAndHaptic(context)
                    if (sessionStore.isAutoAddMode()) {
                        addItemToPosCart(posCart, fallback, 1)
                        activeScannedProduct = null
                    } else {
                        activeScannedProduct = fallback
                        scanQuantity = 1
                        statusMessage = "Offline Match: ${fallback.name}"
                    }
                } else {
                    statusMessage = "Scan error: ${e.message}"
                }
            } finally {
                isSearchingCode = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BHUMIVERA",
                                color = GoldAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MintGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "2.0 HIGH SPEED",
                                    color = MintGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = Color(0xFF0F766E).copy(alpha = 0.35f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${sessionStore.daysRemaining()}d Left",
                                    color = Color(0xFF5EEAD4),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = statusMessage,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        // Torch Toggle
                        IconButton(onClick = { isTorchOn = !isTorchOn }) {
                            Text(
                                text = if (isTorchOn) "🔦" else "💡",
                                fontSize = 18.sp
                            )
                        }
                        // Camera Flip
                        IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                            Text(text = "🔄", fontSize = 18.sp)
                        }
                    }
                    // Settings / API Config
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Text(text = "⚙️", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepEmerald)
            )
        },
        bottomBar = {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepEmerald,
                contentColor = GoldAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "📷 Quick Scanner",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val cartCount = posCart.sumOf { it.quantity }
                        val cartSum = posCart.sumOf { it.lineTotal }
                        Text(
                            text = if (cartCount > 0) "🧾 E-Bill ($cartCount • ₹${cartSum.toInt()})" else "🧾 E-Bill POS",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        },
        containerColor = DarkSurface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab == 0) {
                // SCANNER SCREEN
                Column(modifier = Modifier.fillMaxSize()) {
                    // Camera Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (hasCameraPermission) {
                            CameraScannerView(
                                modifier = Modifier.fillMaxSize(),
                                isTorchOn = isTorchOn,
                                useFrontCamera = useFrontCamera,
                                onBarcodeScanned = { rawCode ->
                                    processScannedRawText(rawCode)
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Camera permission required for scanning", color = Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                    ) {
                                        Text("Grant Camera Permission", color = DeepEmerald)
                                    }
                                }
                            }
                        }

                        // Top Helper Pill
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (sessionStore.isAutoAddMode()) "⚡ Auto-Add to Bill ON" else "🔍 Inspect Details Mode",
                                    color = if (sessionStore.isAutoAddMode()) MintGreen else GoldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Floating POS Quick Cart Bar if items exist
                        if (posCart.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth()
                                    .clickable { selectedTab = 1 },
                                color = DeepEmerald,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Active Bill: ${posCart.sumOf { it.quantity }} items",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Tap to review & generate E-Bill",
                                            color = GoldAccent,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = "₹${posCart.sumOf { it.lineTotal }.toInt()} →",
                                        color = MintGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Search & Manual Barcode Entry Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardEmerald,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Manual Input Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualInputCode,
                                    onValueChange = { manualInputCode = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Enter SKU, Barcode, or paste URL...", fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldAccent,
                                        unfocusedBorderColor = Color.Gray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (manualInputCode.isNotBlank()) {
                                            processScannedRawText(manualInputCode)
                                            manualInputCode = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                ) {
                                    if (isSearchingCode) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepEmerald)
                                    } else {
                                        Text("Lookup", color = DeepEmerald, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Quick Demo SKU Pills
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Quick Demo Scans:", color = Color.Gray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Face Wash" to "BV-LUX-CFW-01",
                                    "Sea Buckthorn" to "BV-LUX-SBFW-02",
                                    "Shampoo" to "BV-LUX-SH2IN1-03",
                                    "Charcoal Soap" to "BV-LUX-CSOAP-04"
                                ).forEach { (label, sku) ->
                                    Surface(
                                        color = DeepEmerald,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { processScannedRawText(sku) }
                                    ) {
                                        Text(
                                            text = label,
                                            color = GoldAccent,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SCANNED PRODUCT POPUP BOTTOM CARD
                activeScannedProduct?.let { product ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { activeScannedProduct = null },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = false) {},
                            color = CardEmerald,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Header Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = GoldAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "100% LABEL DISCLOSED • GENUINE",
                                            color = GoldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { activeScannedProduct = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("✕", color = Color.Gray, fontSize = 16.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Product Title
                                Text(
                                    text = product.name,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = product.category ?: "Botanical Formulations",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Details Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("SKU / BARCODE", color = Color.Gray, fontSize = 10.sp)
                                        Text(
                                            text = product.displaySku,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Column {
                                        Text("LIVE STOCK", color = Color.Gray, fontSize = 10.sp)
                                        Text(
                                            text = "${product.displayStock} Units",
                                            color = if (product.displayStock > 10) MintGreen else Color(0xFFF59E0B),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Column {
                                        Text("BIN LOCATION", color = Color.Gray, fontSize = 10.sp)
                                        Text(
                                            text = product.displayBin,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Price & Quantity Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "₹${product.displayPrice.toInt()}",
                                                color = GoldAccent,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            if (product.displayMrp > product.displayPrice) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "MRP ₹${product.displayMrp.toInt()}",
                                                    color = Color.Gray,
                                                    fontSize = 13.sp,
                                                    textDecoration = TextDecoration.LineThrough
                                                )
                                            }
                                        }
                                        Text(
                                            text = "HSN: ${product.displayHsn} • Inclusive Price",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }

                                    // Stepper
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(DeepEmerald, RoundedCornerShape(8.dp))
                                            .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    ) {
                                        IconButton(
                                            onClick = { if (scanQuantity > 1) scanQuantity-- },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("-", color = Color.White, fontSize = 18.sp)
                                        }
                                        Text(
                                            text = "$scanQuantity",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        IconButton(
                                            onClick = { scanQuantity++ },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("+", color = Color.White, fontSize = 18.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Add to Active Bill
                                    Button(
                                        onClick = {
                                            addItemToPosCart(posCart, product, scanQuantity)
                                            triggerBeepAndHaptic(context)
                                            statusMessage = "Added ${product.name} (×$scanQuantity) to E-Bill!"
                                            scope.launch {
                                                try {
                                                    service.scannerQueueUpdateItem(
                                                        ScannerQueueItemRequest(
                                                            sessionCode = sessionStore.sessionCode(),
                                                            sku = product.displaySku,
                                                            metaId = product.displayMetaId,
                                                            quantity = scanQuantity,
                                                            price = product.displayPrice,
                                                            name = product.name,
                                                            availableStock = product.displayStock,
                                                            scannerApiKey = sessionStore.scannerApiKey()
                                                        )
                                                    )
                                                } catch (_: Exception) {}
                                            }
                                            activeScannedProduct = null
                                        },
                                        modifier = Modifier.weight(1.3f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                    ) {
                                        Text("🛒 Add to E-Bill ($scanQuantity)", color = DeepEmerald, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { activeScannedProduct = null },
                                        modifier = Modifier.weight(0.7f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                                    ) {
                                        Text("Cancel", color = Color.LightGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // GUIDED ORDER PACKING STATION VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📦 ORDER PACKING STATION",
                                color = GoldAccent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Zero-Defect Verification • Anti-Mispick Lock",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        if (activeOrder != null) {
                            TextButton(
                                onClick = {
                                    activeOrder = null
                                    manifestItems.clear()
                                    packingErrorAlert = null
                                    packingSuccessNotice = null
                                }
                            ) {
                                Text("Switch Order", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeOrder == null) {
                        // Scan or enter order token
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardEmerald),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Scan Customer Order Pick Slip / QR Token",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Point camera at Order QR (e.g. ORD:123456) or enter Order ID below to load items checklist.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = packingOrderInput,
                                        onValueChange = { packingOrderInput = it },
                                        placeholder = { Text("Order ID / ORD:...", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = GoldAccent
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (packingOrderInput.isNotBlank()) {
                                                loadOrderManifest(packingOrderInput)
                                                packingOrderInput = ""
                                            }
                                        },
                                        enabled = !isLoadingManifest,
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                    ) {
                                        Text(
                                            if (isLoadingManifest) "Loading..." else "Load",
                                            color = DeepEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Camera Viewport for scanning Order Token
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            if (hasCameraPermission) {
                                CameraScannerView(
                                    modifier = Modifier.fillMaxSize(),
                                    isTorchOn = isTorchOn,
                                    useFrontCamera = useFrontCamera,
                                    onBarcodeScanned = { rawCode ->
                                        processScannedRawText(rawCode)
                                    }
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                color = DeepEmerald.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "Point camera at ORD:... pick token",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        // Order is active!
                        val totalUnits = manifestItems.sumOf { it.quantity }
                        val verifiedUnits = manifestItems.sumOf { it.verifiedCount }
                        val progress = if (totalUnits > 0) verifiedUnits.toFloat() / totalUnits.toFloat() else 0f
                        val isOrderComplete = verifiedUnits >= totalUnits && totalUnits > 0

                        // Order Summary Header
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardEmerald),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Order #${activeOrder!!.orderNumber}",
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Surface(
                                        color = if (isOrderComplete) MintGreen.copy(alpha = 0.2f) else GoldAccent.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isOrderComplete) "READY TO SEAL" else "PACKING IN PROGRESS",
                                            color = if (isOrderComplete) MintGreen else GoldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Customer: ${activeOrder!!.customerName ?: "Valued Customer"} • ${activeOrder!!.city ?: "Destination"}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Verification Progress", color = Color.Gray, fontSize = 11.sp)
                                    Text("$verifiedUnits / $totalUnits Units", color = if (isOrderComplete) MintGreen else GoldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isOrderComplete) MintGreen else GoldAccent,
                                    trackColor = Color.Black.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Alerts
                        if (packingErrorAlert != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A0E17)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⛔", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = packingErrorAlert!!,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (packingSuccessNotice != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3D26)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MintGreen)
                            ) {
                                Text(
                                    text = packingSuccessNotice!!,
                                    color = MintGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        // Active Viewport (Mini Camera Viewport)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            if (hasCameraPermission) {
                                CameraScannerView(
                                    modifier = Modifier.fillMaxSize(),
                                    isTorchOn = isTorchOn,
                                    useFrontCamera = useFrontCamera,
                                    onBarcodeScanned = { rawCode ->
                                        processScannedRawText(rawCode)
                                    }
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(6.dp),
                                color = DeepEmerald.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "Scan physical formulation barcode / QR",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Items Manifest Checklist
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ORDER FORMULATIONS MANIFEST (${manifestItems.size} SKUs)",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(manifestItems) { item ->
                                val isDone = item.isFulfilled
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDone) Color(0xFF132B1F) else CardEmerald
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isDone) MintGreen.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "SKU: ${item.sku ?: "N/A"} • Bin: ${item.binCode ?: "ASN-01"}",
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${item.verifiedCount} / ${item.quantity}",
                                                color = if (isDone) MintGreen else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isDone) "✅" else "⚪",
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 2) {
                // PACKAGE FINDER & LOST PARCEL INTELLIGENCE VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🔍 PACKAGE FINDER & RECOVERY",
                        color = GoldAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Point camera at any lost parcel label or Package QR (PKG:...)",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = packageSearchInput,
                            onValueChange = { packageSearchInput = it },
                            placeholder = { Text("PKG:... / Order # / AWB Tracking", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GoldAccent
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (packageSearchInput.isNotBlank()) {
                                    lookupPackage(packageSearchInput)
                                    packageSearchInput = ""
                                }
                            },
                            enabled = !isSearchingPackage,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Text(
                                if (isSearchingPackage) "Searching..." else "Find",
                                color = DeepEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (packageErrorNotice != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4A0E17)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "⚠️ " + packageErrorNotice!!,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (packageDesktopSyncNotice != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3D26)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "🖥️ " + packageDesktopSyncNotice!!,
                                color = MintGreen,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (activePackageData == null) {
                        // Camera Viewport for scanning lost parcel
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            if (hasCameraPermission) {
                                CameraScannerView(
                                    modifier = Modifier.fillMaxSize(),
                                    isTorchOn = isTorchOn,
                                    useFrontCamera = useFrontCamera,
                                    onBarcodeScanned = { rawCode ->
                                        processScannedRawText(rawCode)
                                    }
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                color = DeepEmerald.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "Scan PKG:123 or AWB barcode on misplaced box",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        // Package Recovery Dossier Card
                        val pkg = activePackageData!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(containerColor = CardEmerald),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pkg.packageId ?: "PKG:${pkg.orderId}",
                                            color = GoldAccent,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                        Surface(
                                            color = MintGreen.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = pkg.status.uppercase(),
                                                color = MintGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Order #${pkg.orderNumber} • ₹${pkg.totalAmount.toInt()} (${pkg.paymentMode?.uppercase() ?: "PREPAID"})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 6.dp))
                                }

                                item {
                                    Text("DESTINATION CONGIGNEE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(pkg.customerName ?: "Valued Customer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("TEL: ${pkg.customerPhone ?: "N/A"}", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${pkg.shippingAddress ?: ""}, ${pkg.city ?: ""}, ${pkg.state ?: ""} - ${pkg.pincode ?: ""}", color = Color.LightGray, fontSize = 11.sp)
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 6.dp))
                                }

                                item {
                                    Text("COURIER LOGISTICS ROUTING", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("Carrier: ${pkg.courier ?: "Delhivery Express"}", color = Color.White, fontSize = 12.sp)
                                    Text("AWB: ${pkg.trackingNumber ?: "BV-TRK-${pkg.orderId}"}", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 6.dp))
                                }

                                item {
                                    Text("PARCEL FORMULATIONS (${pkg.items.size} ITEMS)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                items(pkg.items) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("SKU: ${item.sku ?: "N/A"}", color = Color.Gray, fontSize = 9.sp)
                                        }
                                        Text("${item.quantity} Unit(s)", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                // Push notice to desktop
                                                scope.launch {
                                                    try {
                                                        service.scannerScan(
                                                            com.bhumivera.scanner.data.ScannerScanRequest(
                                                                sessionCode = sessionStore.sessionCode(),
                                                                qrCode = pkg.packageId ?: "PKG:${pkg.orderId}",
                                                                scannerApiKey = sessionStore.scannerApiKey()
                                                            )
                                                        )
                                                        packageDesktopSyncNotice = "Pushed Package ${pkg.packageId} to Warehouse Desktop Terminal (${sessionStore.sessionCode()})!"
                                                    } catch (e: Exception) {
                                                        packageDesktopSyncNotice = "Sent signal to desktop."
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                        ) {
                                            Text("Push to Desktop", color = DeepEmerald, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                activePackageData = null
                                                packageDesktopSyncNotice = null
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Text("Scan Another", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // POS ACTIVE E-BILL & QUICK CHECKOUT TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🧾 INSTANT E-BILL POS CHECKOUT",
                        color = GoldAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (posCart.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🛍️", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("E-Bill Cart is Empty", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Switch to Quick Scanner and scan any product QR code.",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { selectedTab = 0 },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                ) {
                                    Text("Open Scanner", color = DeepEmerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Customer Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CUSTOMER RECIPIENT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customerName,
                                        onValueChange = { customerName = it },
                                        label = { Text("Name", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = GoldAccent
                                        )
                                    )
                                    OutlinedTextField(
                                        value = customerPhone,
                                        onValueChange = { customerPhone = it },
                                        label = { Text("Phone (WhatsApp)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("9876543210", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = GoldAccent
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Cart Items List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(posCart) { item ->
                                Surface(
                                    color = DeepEmerald,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "₹${item.price.toInt()} × ${item.quantity} = ₹${(item.price * item.quantity).toInt()}",
                                                color = GoldAccent,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (item.quantity > 1) {
                                                        val newQty = item.quantity - 1
                                                        val idx = posCart.indexOf(item)
                                                        if (idx != -1) posCart[idx] = item.copy(quantity = newQty)
                                                        scope.launch {
                                                            try {
                                                                service.scannerQueueUpdateItem(
                                                                    ScannerQueueItemRequest(
                                                                        sessionCode = sessionStore.sessionCode(),
                                                                        sku = item.sku,
                                                                        metaId = item.id,
                                                                        delta = -1,
                                                                        scannerApiKey = sessionStore.scannerApiKey()
                                                                    )
                                                                )
                                                            } catch (_: Exception) {}
                                                        }
                                                    } else {
                                                        posCart.remove(item)
                                                        scope.launch {
                                                            try {
                                                                service.scannerQueueRemove(
                                                                    ScannerQueueRemoveRequest(
                                                                        sessionCode = sessionStore.sessionCode(),
                                                                        sku = item.sku,
                                                                        metaId = item.id,
                                                                        scannerApiKey = sessionStore.scannerApiKey()
                                                                    )
                                                                )
                                                            } catch (_: Exception) {}
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("-", color = Color.White, fontSize = 16.sp)
                                            }
                                            Text(
                                                text = "${item.quantity}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            IconButton(
                                                onClick = {
                                                    val newQty = item.quantity + 1
                                                    val idx = posCart.indexOf(item)
                                                    if (idx != -1) posCart[idx] = item.copy(quantity = newQty)
                                                    scope.launch {
                                                        try {
                                                            service.scannerQueueUpdateItem(
                                                                ScannerQueueItemRequest(
                                                                    sessionCode = sessionStore.sessionCode(),
                                                                    sku = item.sku,
                                                                    metaId = item.id,
                                                                    delta = 1,
                                                                    scannerApiKey = sessionStore.scannerApiKey()
                                                                )
                                                            )
                                                        } catch (_: Exception) {}
                                                    }
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("+", color = Color.White, fontSize = 16.sp)
                                            }
                                            IconButton(
                                                onClick = {
                                                    posCart.remove(item)
                                                    scope.launch {
                                                        try {
                                                            service.scannerQueueRemove(
                                                                ScannerQueueRemoveRequest(
                                                                    sessionCode = sessionStore.sessionCode(),
                                                                    sku = item.sku,
                                                                    metaId = item.id,
                                                                    scannerApiKey = sessionStore.scannerApiKey()
                                                                )
                                                            )
                                                        } catch (_: Exception) {}
                                                    }
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("🗑️", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Payment Mode & Bill Calculation
                        val subTotal = posCart.sumOf { it.price * it.quantity }
                        val grandTotal = subTotal

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal:", color = Color.Gray, fontSize = 12.sp)
                                    Text("₹${"%.2f".format(subTotal)}", color = Color.White, fontSize = 12.sp)
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = GoldAccent.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("TOTAL BILL AMOUNT:", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("₹${grandTotal.toInt()}", color = MintGreen, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Payment Mode Selector
                                Text("PAYMENT MODE:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("UPI", "Cash", "Card").forEach { mode ->
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { paymentMode = mode },
                                            color = if (paymentMode == mode) GoldAccent else DeepEmerald,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (paymentMode == mode) GoldAccent else Color.Gray.copy(alpha = 0.4f)
                                            )
                                        ) {
                                            Text(
                                                text = mode,
                                                color = if (paymentMode == mode) DeepEmerald else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                // If UPI Mode, show Live QR Option
                                if (paymentMode == "UPI") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val upiPayload = "upi://pay?pa=bhumivera@okhdfcbank&pn=Bhumivera+Organics&am=${grandTotal.toInt()}&cu=INR"
                                    val upiBitmap = remember(grandTotal) {
                                        QrCodeGenerator.generateQrBitmap(upiPayload, 260)
                                    }
                                    if (upiBitmap != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                bitmap = upiBitmap.asImageBitmap(),
                                                contentDescription = "UPI Scan to Pay QR",
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Customer UPI Scan to Pay", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("bhumivera@okhdfcbank", color = Color.DarkGray, fontSize = 11.sp)
                                                Text("Instant settlement of ₹${grandTotal.toInt()}", color = DeepEmerald, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Checkout Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isCheckingOut = true
                                    try {
                                        val itemsList = posCart.map {
                                            mapOf(
                                                "id" to it.id,
                                                "productId" to it.productId,
                                                "name" to it.name,
                                                "sku" to it.sku,
                                                "barcode" to it.barcode,
                                                "price" to it.price,
                                                "p" to it.price,
                                                "quantity" to it.quantity,
                                                "q" to it.quantity,
                                                "gstRate" to it.gstRate,
                                                "gstHsnCode" to it.gstHsnCode
                                            )
                                        }
                                        val checkoutReq = PosCheckoutRequest(
                                            customer = PosCustomer(
                                                name = customerName.ifBlank { "Walk-in Retail Shopper" },
                                                phone = customerPhone.ifBlank { "-" },
                                                gstin = customerGstin
                                            ),
                                            items = itemsList,
                                            taxRate = 0.0,
                                            paidAmount = grandTotal,
                                            paymentMode = paymentMode,
                                            scannerApiKey = sessionStore.scannerApiKey()
                                        )
                                        val resp = service.posCheckout(checkoutReq)
                                        if (resp.success && resp.sale != null) {
                                            completedInvoice = resp.sale
                                            showInvoiceDialog = true
                                            posCart.clear()
                                        } else {
                                            statusMessage = resp.message ?: "POS Checkout failed"
                                        }
                                    } catch (e: Exception) {
                                        statusMessage = "Checkout error: ${e.message}"
                                    } finally {
                                        isCheckingOut = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            if (isCheckingOut) {
                                CircularProgressIndicator(color = DeepEmerald, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "⚡ GENERATE E-BILL & COMPLETE POS (₹${grandTotal.toInt()})",
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // COMPLETED INVOICE & WHATSAPP E-BILL DIALOG
    // ==========================================
    if (showInvoiceDialog && completedInvoice != null) {
        val invoice = completedInvoice!!
        AlertDialog(
            onDismissRequest = { showInvoiceDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🌿 BHUMIVERA ORGANICS 🌿", color = GoldAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("Official Tax E-Bill Generated", color = Color.White, fontSize = 12.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("INVOICE #: ${invoice.invoiceNo}", color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("DATE: ${invoice.date ?: "Live Settlement"}", color = Color.LightGray, fontSize = 11.sp)
                            Text("CUSTOMER: ${invoice.cust?.name ?: customerName} (${invoice.cust?.phone ?: customerPhone})", color = Color.White, fontSize = 11.sp)
                            Text("PAYMENT MODE: ${invoice.paymentMode}", color = GoldAccent, fontSize = 11.sp)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.Gray.copy(alpha = 0.3f))
                            Text("NET AMOUNT: ₹${invoice.billTotal.toInt()}", color = MintGreen, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("Website stock atomically updated.", color = Color.Gray, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1-Click WhatsApp E-Bill Button
                    Button(
                        onClick = {
                            val cleanPhone = (invoice.cust?.phone ?: customerPhone).replace("+", "").replace(" ", "").replace("-", "")
                            val formattedReceipt = buildWhatsAppInvoiceText(invoice)
                            val encoded = URLEncoder.encode(formattedReceipt, StandardCharsets.UTF_8.toString())
                            val url = if (cleanPhone.length >= 10) {
                                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encoded"
                            } else {
                                "https://api.whatsapp.com/send?text=$encoded"
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📲 Send E-Bill on WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInvoiceDialog = false
                        selectedTab = 0
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text("Done / Scan Next", color = DeepEmerald, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardEmerald
        )
    }

    // ==========================================
    // SETTINGS / API PAIRING DIALOG
    // ==========================================
    if (showPairingSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showPairingSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Terminal Paired!", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(pairingSuccessMessage, color = Color.White, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showPairingSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text("Start Scanning Products", color = DeepEmerald, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardEmerald
        )
    }

    if (showSettingsDialog) {
        var tempUrl by remember { mutableStateOf(sessionStore.backendUrl()) }
        var tempKey by remember { mutableStateOf(sessionStore.scannerApiKey()) }
        var tempSession by remember { mutableStateOf(sessionStore.sessionCode()) }
        var tempAutoAdd by remember { mutableStateOf(sessionStore.isAutoAddMode()) }
        var pingStatus by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Scanner & API Configuration", color = GoldAccent, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Backend API URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("Scanner API Key (x-scanner-api-key)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempSession,
                        onValueChange = { tempSession = it },
                        label = { Text("Desktop POS Session Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { tempAutoAdd = !tempAutoAdd }
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = tempAutoAdd,
                            onCheckedChange = { tempAutoAdd = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Add to POS Bill on Scan", color = Color.White, fontSize = 12.sp)
                    }

                    if (pingStatus.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(pingStatus, color = MintGreen, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pingStatus = "Testing 10x connection..."
                                try {
                                    val ping = service.scannerPing(tempKey)
                                    pingStatus = "🟢 Ping Successful: ${ping.message ?: "Connected"}"
                                } catch (e: Exception) {
                                    pingStatus = "🔴 Ping Failed: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test API Connection", color = GoldAccent)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionStore.saveBackendUrl(tempUrl)
                        sessionStore.saveScannerApiKey(tempKey)
                        sessionStore.saveSessionCode(tempSession)
                        sessionStore.setAutoAddMode(tempAutoAdd)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text("Save & Apply", color = DeepEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardEmerald
        )
    }
}

// ==========================================
// CACHE RESOLUTION & SEED HELPERS
// ==========================================

private fun resolveFromLocalCache(
    rawInput: String,
    lookupMap: Map<String, ScannerProduct>,
    catalog: List<ScannerProduct>
): ScannerProduct? {
    val clean = rawInput.trim()
    if (clean.isBlank()) return null

    var candidate = clean.lowercase()
    var parsedBatch: String? = null

    // Check Bhumivera Batch QR: BV:<SKU>:<BATCH> or BV|<SKU>|<BATCH>
    if (clean.startsWith("BV:", ignoreCase = true) || clean.startsWith("BV|", ignoreCase = true)) {
        val parts = clean.split(":", "|").map { it.trim() }
        if (parts.size >= 2) {
            candidate = parts[1].lowercase()
            if (parts.size >= 3) {
                parsedBatch = parts[2]
            }
        }
    } else if (clean.startsWith("{") && clean.endsWith("}")) {
        try {
            val j = org.json.JSONObject(clean)
            if (j.has("sku")) candidate = j.optString("sku").lowercase()
            if (j.has("batch")) parsedBatch = j.optString("batch")
        } catch (_: Exception) {}
    }

    // Extract query parameters if URL
    if (clean.startsWith("http://") || clean.startsWith("https://") || clean.contains("bhumivera.com")) {
        try {
            val uri = Uri.parse(if (clean.startsWith("http")) clean else "https://$clean")
            val skuParam = uri.getQueryParameter("sku")
            val serialParam = uri.getQueryParameter("serial") ?: uri.getQueryParameter("code")
            val pathSegments = uri.pathSegments
            val prodIdx = pathSegments.indexOfFirst { it == "product" || it == "combo" }

            if (!skuParam.isNullOrBlank()) {
                candidate = skuParam.lowercase()
            } else if (prodIdx != -1 && prodIdx + 1 < pathSegments.size) {
                candidate = pathSegments[prodIdx + 1].lowercase()
            } else if (!serialParam.isNullOrBlank()) {
                candidate = serialParam.lowercase()
            }
        } catch (_: Exception) {}
    }

    // Direct lookup in index
    lookupMap[candidate]?.let { return it }

    // Fuzzy match in catalog
    val direct = lookupMap[candidate] ?: catalog.firstOrNull {
        it.displaySku.equals(candidate, ignoreCase = true) ||
        it.displayBarcode.equals(candidate, ignoreCase = true) ||
        it.name.contains(candidate, ignoreCase = true) ||
        (it.slug != null && it.slug.equals(candidate, ignoreCase = true)) ||
        (it.batchNo != null && it.batchNo.equals(candidate, ignoreCase = true))
    }

    if (direct != null && !parsedBatch.isNullOrBlank()) {
        return direct.copy(batchNo = parsedBatch, batchNoAlt = parsedBatch)
    }
    return direct
}

private fun indexProductsIntoMap(items: List<ScannerProduct>, map: MutableMap<String, ScannerProduct>) {
    items.forEach { indexSingleProduct(it, map) }
}

private fun indexSingleProduct(p: ScannerProduct, map: MutableMap<String, ScannerProduct>) {
    p.sku?.let { map[it.lowercase()] = p }
    p.barcode?.let { map[it.lowercase()] = p }
    p.slug?.let { map[it.lowercase()] = p }
    p.batchNo?.let { map[it.lowercase()] = p }
    map["sku-${p.displayMetaId}".lowercase()] = p
}

private fun seedDefaultLuxuryProducts(
    catalog: MutableList<ScannerProduct>,
    map: MutableMap<String, ScannerProduct>
) {
    val seeds = listOf(
        ScannerProduct(
            metaId = 1,
            productId = 10,
            name = "Bhumivera Activated Charcoal Purifying Luxury Face Wash (100ml)",
            sku = "BV-LUX-CFW-01",
            barcode = "8908020260015",
            slug = "activated-charcoal-luxury-face-wash",
            category = "Luxury Facial Cleansers",
            price = 299.0,
            mrp = 699.0,
            availableStock = 249,
            binCode = "ASN-SHOP-R1-B01",
            batchNo = "BV-BATCH-2026-CF01"
        ),
        ScannerProduct(
            metaId = 2,
            productId = 11,
            name = "Bhumivera Himalayan Sea Buckthorn Cellular Radiance Face Wash (100ml)",
            sku = "BV-LUX-SBFW-02",
            barcode = "8908020260022",
            slug = "himalayan-seabuckthorn-luxury-face-wash",
            category = "Luxury Facial Cleansers",
            price = 399.0,
            mrp = 799.0,
            availableStock = 249,
            binCode = "ASN-SHOP-R1-B02",
            batchNo = "BV-BATCH-2026-SB02"
        ),
        ScannerProduct(
            metaId = 3,
            productId = 12,
            name = "Bhumivera Royal Botanical 2-in-1 Luxury Shampoo with Conditioner (200ml)",
            sku = "BV-LUX-SH2IN1-03",
            barcode = "8908020260039",
            slug = "luxury-2-in-1-botanical-shampoo-conditioner",
            category = "Luxury Hair Care",
            price = 449.0,
            mrp = 899.0,
            availableStock = 199,
            binCode = "ASN-SHOP-R2-B01",
            batchNo = "BV-BATCH-2026-SH03"
        ),
        ScannerProduct(
            metaId = 4,
            productId = 13,
            name = "Bhumivera Charcoal Deep Cleansing Bar (Handmade Ayurvedic Luxury Soap)",
            sku = "BV-LUX-CSOAP-04",
            barcode = "8908020260046",
            slug = "artisanal-charcoal-handmade-soap",
            category = "Artisanal Ayurvedic Cleansing Bars",
            price = 199.0,
            mrp = 399.0,
            availableStock = 299,
            binCode = "ASN-SHOP-R2-B02",
            batchNo = "BV-BATCH-2026-CS04"
        ),
        ScannerProduct(
            metaId = 5,
            productId = 14,
            name = "Bhumivera Soothing Bar (Aloe Vera, Neem & Tulsi Handmade Soap)",
            sku = "BV-LUX-ASOAP-05",
            barcode = "8908020260053",
            slug = "artisanal-aloe-vera-handmade-soap",
            category = "Artisanal Ayurvedic Cleansing Bars",
            price = 199.0,
            mrp = 349.0,
            availableStock = 299,
            binCode = "ASN-SHOP-R2-B03",
            batchNo = "BV-BATCH-2026-AS05"
        ),
        ScannerProduct(
            metaId = 6,
            productId = 15,
            name = "Bhumivera Kalpvriksh Satva Tulsi (Liquid Extract of 7 Types of Tulsi 30ml)",
            sku = "BV-LUX-TULSI-06",
            barcode = "8908020260060",
            slug = "kalpvriksh-satva-tulsi-7-tulsi-extract",
            category = "Luxury Ayurvedic Wellness & Immunity Drops",
            price = 299.0,
            mrp = 499.0,
            availableStock = 299,
            binCode = "ASN-SHOP-R3-B01",
            batchNo = "BV-BATCH-2026-TL06"
        ),
        ScannerProduct(
            metaId = 7,
            productId = 16,
            name = "The Complete Botanical Daily Regimen (3-Piece Bundle)",
            sku = "BV-BND-DAILY-01",
            barcode = "8908020260077",
            slug = "bundle-daily-regimen",
            category = "Maison Regimen Bundles",
            price = 1499.0,
            mrp = 2047.0,
            availableStock = 200,
            binCode = "ASN-SHOP-R3-B02",
            batchNo = "BV-BATCH-2026-BND01"
        )
    )

    catalog.addAll(seeds)
    indexProductsIntoMap(seeds, map)
}

private fun addItemToPosCart(cart: MutableList<PosBillItem>, product: ScannerProduct, quantity: Int) {
    val idx = cart.indexOfFirst { it.id == product.displayMetaId || it.sku == product.displaySku }
    if (idx != -1) {
        val existing = cart[idx]
        cart[idx] = existing.copy(quantity = existing.quantity + quantity)
    } else {
        cart.add(
            PosBillItem(
                id = product.displayMetaId,
                productId = product.displayProductId,
                name = product.name,
                sku = product.displaySku,
                barcode = product.displayBarcode,
                quantity = quantity,
                price = product.displayPrice,
                mrp = product.displayMrp,
                gstRate = 5.0,
                gstHsnCode = product.displayHsn,
                binCode = product.displayBin,
                batchNo = product.displayBatch,
                availableStock = product.displayStock
            )
        )
    }
}

private fun triggerMismatchAlarmAndHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 180, 80, 180), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(longArrayOf(0, 180, 80, 180), -1)
        }
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 450)
    } catch (_: Exception) {}
}

private fun triggerBeepAndHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    } catch (_: Exception) {}
}

private fun buildWhatsAppInvoiceText(invoice: PosSaleRecord): String {
    return """
🌿 *BHUMIVERA ORGANICS INDIA PRIVATE LIMITED* 🌿
*The Transparent Maison — 100% Label Disclosed Formulations*
CIN: U20237WB2026PTC295842 | GSTIN: 19AAKCB2026A1Z5
G.T. Road, Asansol, West Bengal - 713304

*OFFICIAL RETAIL E-BILL RECEIPT*
━━━━━━━━━━━━━━━━━━━━━━━━━━
📄 *Invoice #:* ${invoice.invoiceNo ?: "BV-POS-2026"}
📅 *Date:* ${invoice.date ?: "03 Nov 2026"}
👤 *Customer:* ${invoice.cust?.name ?: "Walk-in Patron"}
💳 *Payment:* ${invoice.paymentMode} (Status: Paid)
━━━━━━━━━━━━━━━━━━━━━━━━━━
💰 *Subtotal:* ₹${invoice.subTotal.toInt()}
💎 *TOTAL PAID:* ₹${invoice.billTotal.toInt()} (Tax Included / Nil)
━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 *Verify Formulation Authenticity:*
https://www.bhumivera.com/verify?inv=${invoice.invoiceNo}

*Thank you for supporting pure, cold-pressed botanical craftsmanship!*
    """.trimIndent()
}

private fun extractSkuOrSlugFallback(clean: String, catalog: List<ScannerProduct>): ScannerProduct? {
    val lower = clean.lowercase()
    for (item in catalog) {
        if (!item.sku.isNullOrBlank() && lower.contains(item.sku.lowercase())) return item
        if (!item.barcode.isNullOrBlank() && lower.contains(item.barcode.lowercase())) return item
        if (!item.slug.isNullOrBlank() && lower.contains(item.slug.lowercase())) return item
    }
    return null
}
