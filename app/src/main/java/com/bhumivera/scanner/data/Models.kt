package com.bhumivera.scanner.data

import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(
    val success: Boolean? = null,
    val data: T? = null,
    val message: String? = null,
)

data class Product(
    val id: Int,
    val name: String,
    val slug: String? = null,
    val price: Double? = null,
    @SerializedName("discount_price") val discountPrice: Double? = null,
    val mrp: Double? = null,
    val description: String? = null,
    @SerializedName("short_description") val shortDescription: String? = null,
    val category: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    val image: String? = null,
    val images: List<ProductImage>? = null,
)

data class ProductImage(
    @SerializedName("file_path") val filePath: String? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    val url: String? = null,
)

data class CustomerUser(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
)

data class LoginRequest(val email: String, val password: String)

data class GoogleLoginRequest(
    val idToken: String,
    val termsAccepted: Boolean,
)

data class TwoFactorRequest(val email: String, val otp: String)

data class LoginResponse(
    val token: String? = null,
    val user: CustomerUser? = null,
    val requires2FA: Boolean = false,
    val email: String? = null,
    val message: String? = null,
)

data class AddCartRequest(
    @SerializedName("productId") val productId: Int,
    val quantity: Int,
)

data class UpdateCartRequest(val quantity: Int)

data class CartResponse(
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
)

data class CartItem(
    val id: Int? = null,
    @SerializedName("product_id") val productId: Int,
    val name: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double? = null,
    val subtotal: Double? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
)

// ==========================================
// 10x FAST SCANNER & E-BILL POS DATA MODELS
// ==========================================

data class ScannerProduct(
    @SerializedName("meta_id") val metaId: Int? = null,
    @SerializedName("metaId") val metaIdAlt: Int? = null,
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("productId") val productIdAlt: Int? = null,
    val name: String = "Bhumivera Formulation",
    val sku: String? = null,
    val barcode: String? = null,
    val slug: String? = null,
    val category: String? = null,
    val mrp: Double? = null,
    val price: Double? = null,
    @SerializedName("gst_rate") val gstRate: Double? = 5.0,
    @SerializedName("gstRate") val gstRateAlt: Double? = null,
    @SerializedName("gst_hsn_code") val gstHsnCode: String? = "30049011",
    @SerializedName("gstHsnCode") val gstHsnCodeAlt: String? = null,
    @SerializedName("available_stock") val availableStock: Int? = null,
    @SerializedName("availableStock") val availableStockAlt: Int? = null,
    @SerializedName("bin_code") val binCode: String? = null,
    @SerializedName("binCode") val binCodeAlt: String? = null,
    @SerializedName("rack_location") val rackLocation: String? = null,
    @SerializedName("batch_no") val batchNo: String? = null,
    @SerializedName("batchNo") val batchNoAlt: String? = null,
    val uom: String? = "Pcs",
    @SerializedName("verify_url") val verifyUrl: String? = null,
    @SerializedName("product_url") val productUrl: String? = null,
) {
    val displayMetaId: Int get() = metaId ?: metaIdAlt ?: (productId ?: productIdAlt ?: 0)
    val displayProductId: Int get() = productId ?: productIdAlt ?: 0
    val displayPrice: Double get() = price ?: mrp ?: 0.0
    val displayMrp: Double get() = mrp ?: displayPrice
    val displayStock: Int get() = availableStock ?: availableStockAlt ?: 0
    val displayGstRate: Double get() = gstRate ?: gstRateAlt ?: 5.0
    val displayHsn: String get() = gstHsnCode ?: gstHsnCodeAlt ?: "30049011"
    val displayBin: String get() = binCode ?: binCodeAlt ?: "ASN-SHOP-R1-B01"
    val displayBatch: String get() = batchNo ?: batchNoAlt ?: "BV-BATCH-2026"
    val displaySku: String get() = sku ?: "SKU-${displayMetaId}"
    val displayBarcode: String get() = barcode ?: displaySku
}

data class ScannerCatalogResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val syncedAt: String? = null,
    val items: List<ScannerProduct> = emptyList(),
    val lookupMap: Map<String, ScannerProduct> = emptyMap(),
)

data class ScannerLookupRequest(
    val code: String,
    val scannerApiKey: String? = null,
)

data class ScannerLookupResponse(
    val success: Boolean = false,
    val item: ScannerProduct? = null,
    val message: String? = null,
)

data class ScannerScanRequest(
    val qrCode: String,
    val quantity: Int = 1,
    val sessionCode: String = "",
    val deviceName: String = "Bhumivera Android Quick Scanner",
    val scannerApiKey: String? = null,
)

data class ScannerScanResponse(
    val success: Boolean = false,
    val message: String? = null,
    val scanned: List<ScannerProduct>? = null,
)

data class PosBillItem(
    val id: Int,
    val productId: Int,
    val name: String,
    val sku: String,
    val barcode: String,
    var quantity: Int,
    val price: Double,
    val mrp: Double,
    val gstRate: Double = 5.0,
    val gstHsnCode: String = "30049011",
    val binCode: String = "ASN-SHOP-R1-B01",
    val batchNo: String = "BV-BATCH-2026",
    val availableStock: Int = 0,
) {
    val lineSubtotal: Double get() = price * quantity
    val lineTaxAmount: Double get() = 0.0
    val lineTotal: Double get() = lineSubtotal
}

data class PosDraftOrder(
    val id: String,
    val title: String,
    val customerName: String,
    val customerPhone: String,
    val paymentMode: String,
    val items: List<PosBillItem>,
    val savedAt: Long,
    val orderId: String = "",
    val status: String = "draft",
    val createdByName: String = "Staff",
)

data class ScannerConnectRequest(
    val sessionCode: String = "",
    val operatorName: String = "Akash",
    val deviceName: String = "Bombera Android QR Scanner",
    val resume: Boolean = true,
    val scannerApiKey: String? = null,
)

data class ScannerConnectResponse(
    val success: Boolean = false,
    val connected: Boolean = false,
    val resumed: Boolean = false,
    val sessionCode: String? = null,
    val idleTimeoutMinutes: Int = 10,
    val idleTimeoutSeconds: Int = 600,
    val idleExpiresAt: String? = null,
    val message: String? = null,
    val queue: List<ScannerQueueItem> = emptyList(),
    val drafts: List<PosDraftOrder> = emptyList(),
)

data class SavePosDraftApiRequest(
    val id: String? = null,
    val sessionCode: String = "",
    val orderId: String = "",
    val title: String? = null,
    val status: String = "draft",
    val customerName: String = "Walk-in Retail Shopper",
    val customerPhone: String = "",
    val paymentMode: String = "UPI",
    val items: List<PosBillItem> = emptyList(),
    val operatorName: String = "Akash",
    val savedAt: Long = System.currentTimeMillis(),
    val scannerApiKey: String? = null,
)

data class SavePosDraftApiResponse(
    val success: Boolean = false,
    val message: String? = null,
    val draft: PosDraftOrder? = null,
    val drafts: List<PosDraftOrder> = emptyList(),
)

data class PosDraftsListResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val drafts: List<PosDraftOrder> = emptyList(),
    val message: String? = null,
)

data class ClaimOrderRequest(
    val orderId: String,
    val sessionCode: String = "",
    val operatorName: String = "Akash",
    val lockType: String = "BILLING",
    val scannerApiKey: String? = null,
)

data class ClaimOrderResponse(
    val success: Boolean = false,
    val code: String? = null,
    val lockedBy: String? = null,
    val orderId: String? = null,
    val message: String? = null,
)

data class PosCustomer(
    val name: String = "Walk-in Retail Shopper",
    val phone: String = "-",
    val gstin: String = "",
)

data class PosCheckoutRequest(
    val customer: PosCustomer,
    val items: List<Map<String, Any>>,
    val sessionCode: String = "",
    val orderId: String = "",
    val draftId: String = "",
    val operatorName: String = "Akash",
    val extraCharges: List<Map<String, Any>> = emptyList(),
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val paidAmount: Double? = null,
    val paymentMode: String = "Cash",
    val upiReference: String = "",
    val notes: String = "Bhumivera Android Quick Scanner POS Checkout",
    val scannerApiKey: String? = null,
)

data class PosCheckoutResponse(
    val success: Boolean = false,
    val message: String? = null,
    val sale: PosSaleRecord? = null,
)

data class PosSaleRecord(
    val id: Int? = null,
    val invoiceNo: String? = null,
    val date: String? = null,
    val subTotal: Double = 0.0,
    val tax: Double = 0.0,
    val taxRate: Double = 0.0,
    val billTotal: Double = 0.0,
    val paid: Double = 0.0,
    val paymentMode: String = "Cash",
    val cashier: String = "scanner",
    val cust: PosCustomer? = null,
)

data class ScannerQueueResponse(
    val success: Boolean = false,
    val message: String? = null,
    val queue: List<ScannerQueueItem> = emptyList(),
)

data class ScannerQueueItem(
    val id: Long? = null,
    @SerializedName("session_code") val sessionCode: String? = null,
    @SerializedName("meta_id") val metaId: Int? = null,
    @SerializedName("product_id") val productId: Int? = null,
    val sku: String = "",
    val barcode: String? = null,
    @SerializedName("product_name") val productName: String = "",
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    val mrp: Double = 0.0,
    @SerializedName("gst_hsn_code") val gstHsnCode: String? = null,
    @SerializedName("gst_rate") val gstRate: Double = 0.0,
    val uom: String? = "Pcs",
    val quantity: Int = 1,
    @SerializedName("available_stock") val availableStock: Int = 0,
    val status: String = "queued",
)

data class ScannerQueueItemRequest(
    val sessionCode: String = "",
    val sku: String? = null,
    val metaId: Int? = null,
    val quantity: Int? = null,
    val delta: Int? = null,
    val price: Double? = null,
    val name: String? = null,
    val availableStock: Int? = null,
    val scannerApiKey: String? = null,
)

data class ScannerQueueRemoveRequest(
    val sessionCode: String = "",
    val sku: String? = null,
    val id: Long? = null,
    val metaId: Int? = null,
    val scannerApiKey: String? = null,
)

data class ScannerQueueClearRequest(
    val sessionCode: String = "",
    val scannerApiKey: String? = null,
)

// ==========================================
// GUIDED ORDER PACKING & ZERO-MISPICK MODELS
// ==========================================

data class OrderManifestResponse(
    val success: Boolean = false,
    val message: String? = null,
    val order: OrderManifestOrder? = null,
    val items: List<OrderManifestItem> = emptyList(),
)

data class OrderManifestOrder(
    val id: Int = 0,
    @SerializedName("orderNumber") val orderNumber: String = "",
    val status: String = "",
    @SerializedName("customerName") val customerName: String? = null,
    @SerializedName("customerPhone") val customerPhone: String? = null,
    @SerializedName("shippingAddress") val shippingAddress: String? = null,
    @SerializedName("shippingCity") val city: String? = null,
    @SerializedName("shippingState") val state: String? = null,
    @SerializedName("shippingPincode") val pincode: String? = null,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0,
    @SerializedName("paymentMode") val paymentMode: String? = null,
)

data class OrderManifestItem(
    @SerializedName("orderItemId") val id: Int? = null,
    @SerializedName("productId") val productId: Int? = null,
    val sku: String? = null,
    val barcode: String? = null,
    val name: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    @SerializedName("binCode") val binCode: String? = null,
    @SerializedName("batchNo") val batchNo: String? = null,
    var verifiedCount: Int = 0,
) {
    val isFulfilled: Boolean get() = verifiedCount >= quantity
}

data class VerifyPackRequest(
    val orderId: String,
    val barcode: String,
    val sessionCode: String = "",
    val scannerApiKey: String? = null,
)

data class VerifyPackResponse(
    val success: Boolean = false,
    val verified: Boolean = false,
    val orderComplete: Boolean = false,
    val orderStatus: String? = null,
    val message: String? = null,
    val code: String? = null,
    val progress: PackProgress? = null,
    val product: VerifyPackProduct? = null,
)

data class PackProgress(
    val orderItemId: Int? = null,
    val verifiedCount: Int = 0,
    val quantity: Int = 0,
    val totalVerified: Int = 0,
    val totalUnits: Int = 0,
)

data class VerifyPackProduct(
    val productId: Int? = null,
    val name: String? = null,
    val sku: String? = null,
)

// ==========================================
// PACKAGE FINDER & LOST PARCEL MODELS
// ==========================================

data class PackageLookupResponse(
    val success: Boolean = false,
    val message: String? = null,
    @SerializedName("package") val packageData: PackageLookupData? = null,
)

data class PackageLookupData(
    val packageId: String? = null,
    val orderId: Int = 0,
    val orderNumber: String = "",
    val status: String = "",
    val customerName: String? = null,
    val customerPhone: String? = null,
    val shippingAddress: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val trackingNumber: String? = null,
    val courier: String? = null,
    val totalAmount: Double = 0.0,
    val paymentMode: String? = null,
    val items: List<OrderManifestItem> = emptyList(),
)

data class StaffLoginRequest(
    val username: String,
    val password: String,
    val deviceName: String = "Bombera Android Scanner App",
)

data class StaffPermissions(
    val can_scan: Boolean = true,
    val can_pack: Boolean = true,
    val can_bill: Boolean = true,
)

data class StaffProfile(
    val id: Int? = null,
    val username: String = "",
    val fullName: String = "",
    val role: String = "cashier",
    val assignedHub: String = "Bhumivera Flagship Hub — Asansol",
    val permissions: StaffPermissions? = null,
)

data class StaffLoginResponse(
    val success: Boolean = false,
    val token: String? = null,
    val sessionCode: String? = null,
    val staff: StaffProfile? = null,
    val message: String? = null,
)

data class WorkroomToggleRequest(
    val onWork: Boolean = true,
    val mode: String = "IDLE",
    val activeOrder: String = "",
    val deviceName: String = "Bombera Android Scanner App",
    val operatorUsername: String? = null,
    val operatorName: String? = null,
    val sessionCode: String? = null,
    val scannerApiKey: String? = null,
)

data class ActiveWorker(
    val username: String = "",
    val fullName: String = "",
    val role: String = "",
    val mode: String = "IDLE",
    val activeOrder: String = "",
    val sessionCode: String = "",
    val lastSeenAt: String? = null,
)

data class WorkroomPresenceResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val activeWorkers: List<ActiveWorker> = emptyList(),
)

data class StackedOrderRecord(
    val orderId: Int,
    val orderNumber: String,
    val customerName: String? = null,
    val totalAmount: Double = 0.0,
    val items: List<OrderManifestItem> = emptyList(),
    val verifiedItems: List<Map<String, Any>> = emptyList(),
    val packedAt: Long = System.currentTimeMillis(),
    val packedBy: String = "Staff",
)

data class BatchPushRequest(
    val stackedOrders: List<StackedOrderRecord> = emptyList(),
    val operatorUsername: String? = null,
    val operatorName: String? = null,
    val sessionCode: String? = null,
    val scannerApiKey: String? = null,
)

data class BatchPushResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val message: String? = null,
)
