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
    val sessionCode: String = "DEFAULT-POS-TERMINAL",
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

data class PosCustomer(
    val name: String = "Walk-in Retail Shopper",
    val phone: String = "-",
    val gstin: String = "",
)

data class PosCheckoutRequest(
    val customer: PosCustomer,
    val items: List<Map<String, Any>>,
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
    val sessionCode: String = "DEFAULT-POS-TERMINAL",
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
    val sessionCode: String = "DEFAULT-POS-TERMINAL",
    val sku: String? = null,
    val id: Long? = null,
    val metaId: Int? = null,
    val scannerApiKey: String? = null,
)

data class ScannerQueueClearRequest(
    val sessionCode: String = "DEFAULT-POS-TERMINAL",
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
    val city: String? = null,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0,
    @SerializedName("paymentMode") val paymentMode: String? = null,
)

data class OrderManifestItem(
    val id: Int? = null,
    @SerializedName("productId") val productId: Int? = null,
    val sku: String? = null,
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
    val orderId: Int,
    val barcode: String,
    val scannerApiKey: String? = null,
)

data class VerifyPackResponse(
    val success: Boolean = false,
    val verified: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    val product: VerifyPackProduct? = null,
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