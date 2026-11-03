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
    @SerializedName("gst_rate") val gstRate: Double? = 18.0,
    @SerializedName("gstRate") val gstRateAlt: Double? = null,
    @SerializedName("gst_hsn_code") val gstHsnCode: String? = "33049990",
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
    val displayGstRate: Double get() = gstRate ?: gstRateAlt ?: 18.0
    val displayHsn: String get() = gstHsnCode ?: gstHsnCodeAlt ?: "33049990"
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
    val gstRate: Double = 18.0,
    val gstHsnCode: String = "33049990",
    val binCode: String = "ASN-SHOP-R1-B01",
    val batchNo: String = "BV-BATCH-2026",
    val availableStock: Int = 0,
) {
    val lineSubtotal: Double get() = price * quantity
    val lineTaxAmount: Double get() = Math.round(lineSubtotal * (gstRate / 100.0) * 100.0) / 100.0
    val lineTotal: Double get() = lineSubtotal + lineTaxAmount
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
    val taxRate: Double = 18.0,
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
    val taxRate: Double = 18.0,
    val billTotal: Double = 0.0,
    val paid: Double = 0.0,
    val paymentMode: String = "Cash",
    val cashier: String = "scanner",
    val cust: PosCustomer? = null,
)
