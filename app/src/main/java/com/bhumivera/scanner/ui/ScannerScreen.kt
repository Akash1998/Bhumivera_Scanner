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
import com.bhumivera.scanner.data.ScannerScanRequest
import com.bhumivera.scanner.data.SessionStore
import com.bhumivera.scanner.scanner.CameraScannerView
import com.bhumivera.scanner.scanner.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
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
                triggerBeepAndHaptic(context)
                pairingSuccessMessage = "Successfully Paired with Asansol POS Terminal!\n\nScanner API Key: " + sessionStore.scannerApiKey() + "\nSession Code: " + sessionStore.sessionCode() + "\nEndpoint: " + sessionStore.backendUrl() + "\n\nNow point your camera at any product QR code or barcode to scan and bill!"
                showPairingSuccessDialog = true
                statusMessage = "🟢 Paired with Asansol POS Terminal!"
                return
            } catch (_: Exception) {}
        }

        if (clean.startsWith("BV-SCAN-")) {
            sessionStore.saveScannerApiKey(clean)
            triggerBeepAndHaptic(context)
            pairingSuccessMessage = "Successfully Paired with Asansol POS Terminal!\n\nScanner API Key: " + clean + "\nSession Code: " + sessionStore.sessionCode() + "\nEndpoint: " + sessionStore.backendUrl() + "\n\nNow point your camera at any product QR code or barcode to scan and bill!"
            showPairingSuccessDialog = true
            statusMessage = "🟢 Paired with Scanner Key: " + clean
            return
        }

        // 2. INSTANT 0ms IN-MEMORY RESOLUTION (Matches URLs, SKUs, Slugs, Barcodes)
        val matched = resolveFromLocalCache(clean, localLookupMap, localCatalog)
        if (matched != null) {
            triggerBeepAndHaptic(context)
            activeScannedProduct = matched
            scanQuantity = 1
            statusMessage = "Resolved: " + matched.name

            if (sessionStore.isAutoAddMode()) {
                addItemToPosCart(posCart, matched, 1)
            }
            scope.launch {
                try {
                    service.scannerScan(
                        ScannerScanRequest(
                            qrCode = matched.displaySku,
                            quantity = 1,
                            sessionCode = sessionStore.sessionCode(),
                            scannerApiKey = sessionStore.scannerApiKey()
                        )
                    )
                } catch (_: Exception) {}
            }
            return
        }

        // 3. LIVE CLOUD RESOLUTION VIA POST /scanner/scan (PROVEN ON RAILWAY)
        scope.launch {
            isSearchingCode = true
            statusMessage = "Scanning with Cloud API: " + clean + "..."
            try {
                val scanReq = ScannerScanRequest(
                    qrCode = clean,
                    quantity = 1,
                    sessionCode = sessionStore.sessionCode(),
                    scannerApiKey = sessionStore.scannerApiKey()
                )
                val res = service.scannerScan(scanReq)
                val product = res.scanned?.firstOrNull()
                if (product != null) {
                    triggerBeepAndHaptic(context)
                    activeScannedProduct = product
                    scanQuantity = 1
                    statusMessage = "Scanned: " + product.name + " (Stock: " + product.displayStock + ")"
                    indexSingleProduct(product, localLookupMap)
                    if (sessionStore.isAutoAddMode()) {
                        addItemToPosCart(posCart, product, 1)
                    }
                } else {
                    val fallback = extractSkuOrSlugFallback(clean, localCatalog)
                    if (fallback != null) {
                        triggerBeepAndHaptic(context)
                        activeScannedProduct = fallback
                        scanQuantity = 1
                        statusMessage = "Matched: " + fallback.name
                        if (sessionStore.isAutoAddMode()) {
                            addItemToPosCart(posCart, fallback, 1)
                        }
                    } else {
                        statusMessage = res.message ?: ("No product found matching '" + clean + "'")
                    }
                }
            } catch (e: Exception) {
                val fallback = extractSkuOrSlugFallback(clean, localCatalog)
                if (fallback != null) {
                    triggerBeepAndHaptic(context)
                    activeScannedProduct = fallback
                    scanQuantity = 1
                    statusMessage = "Offline Match: " + fallback.name
                    if (sessionStore.isAutoAddMode()) {
                        addItemToPosCart(posCart, fallback, 1)
                    }
                } else {
                    statusMessage = "Scan error: " + e.message
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
                                    text = "SCANNER POS",
                                    color = MintGreen,
                                    fontSize = 10.sp,
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
                                            text = "GST ${product.displayGstRate.toInt()}% Included (HSN: ${product.displayHsn})",
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
                                            activeScannedProduct = null
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                    ) {
                                        Text("🛒 Add to E-Bill", color = DeepEmerald, fontWeight = FontWeight.Bold)
                                    }

                                    // Push to Desktop Terminal Queue
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val scanReq = ScannerScanRequest(
                                                        qrCode = product.displaySku,
                                                        quantity = scanQuantity,
                                                        sessionCode = sessionStore.sessionCode(),
                                                        scannerApiKey = sessionStore.scannerApiKey()
                                                    )
                                                    val res = service.scannerScan(scanReq)
                                                    statusMessage = res.message ?: "Synced to Desktop POS!"
                                                    activeScannedProduct = null
                                                } catch (e: Exception) {
                                                    statusMessage = "Terminal sync failed: ${e.message}"
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                                    ) {
                                        Text("🔄 Desktop POS", color = GoldAccent, fontSize = 12.sp)
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
                                                text = "₹${item.price.toInt()} × ${item.quantity} = ₹${item.lineTotal.toInt()} (GST 18%)",
                                                color = GoldAccent,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (item.quantity > 1) {
                                                        item.quantity--
                                                    } else {
                                                        posCart.remove(item)
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
                                                onClick = { item.quantity++ },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("+", color = Color.White, fontSize = 16.sp)
                                            }
                                            IconButton(
                                                onClick = { posCart.remove(item) },
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
                        val subTotal = posCart.sumOf { it.lineSubtotal }
                        val gstTotal = posCart.sumOf { it.lineTaxAmount }
                        val grandTotal = subTotal + gstTotal

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
                                    Text("Subtotal (Taxable):", color = Color.Gray, fontSize = 12.sp)
                                    Text("₹${"%.2f".format(subTotal)}", color = Color.White, fontSize = 12.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("GST 18% (CGST 9% + SGST 9%):", color = Color.Gray, fontSize = 12.sp)
                                    Text("₹${"%.2f".format(gstTotal)}", color = Color.White, fontSize = 12.sp)
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
                                            taxRate = 18.0,
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
    return catalog.firstOrNull {
        it.displaySku.equals(candidate, ignoreCase = true) ||
        it.displayBarcode.equals(candidate, ignoreCase = true) ||
        it.name.contains(candidate, ignoreCase = true) ||
        (it.slug != null && it.slug.equals(candidate, ignoreCase = true))
    }
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
    val existing = cart.firstOrNull { it.id == product.displayMetaId || it.sku == product.displaySku }
    if (existing != null) {
        existing.quantity += quantity
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
                gstRate = product.displayGstRate,
                gstHsnCode = product.displayHsn,
                binCode = product.displayBin,
                batchNo = product.displayBatch,
                availableStock = product.displayStock
            )
        )
    }
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

*OFFICIAL GST TAX E-BILL RECEIPT*
━━━━━━━━━━━━━━━━━━━━━━━━━━
📄 *Invoice #:* ${invoice.invoiceNo ?: "BV-POS-2026"}
📅 *Date:* ${invoice.date ?: "03 Nov 2026"}
👤 *Customer:* ${invoice.cust?.name ?: "Walk-in Patron"}
💳 *Payment:* ${invoice.paymentMode} (Status: Paid)
━━━━━━━━━━━━━━━━━━━━━━━━━━
💰 *Subtotal (Taxable):* ₹${invoice.subTotal.toInt()}
📊 *GST 18%:* ₹${invoice.tax.toInt()}
💎 *TOTAL PAID:* ₹${invoice.billTotal.toInt()}
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
