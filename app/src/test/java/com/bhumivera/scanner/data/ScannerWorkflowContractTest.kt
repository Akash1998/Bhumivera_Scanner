package com.bhumivera.scanner.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerWorkflowContractTest {
    @Test
    fun posCheckoutIncludesThePairedTerminalSession() {
        val request = PosCheckoutRequest(
            customer = PosCustomer(),
            items = listOf(mapOf("productId" to 12, "quantity" to 2)),
            sessionCode = "POS-ASN-01",
        )

        val serialized = JsonParser.parseString(Gson().toJson(request)).asJsonObject

        assertEquals("POS-ASN-01", serialized.get("sessionCode").asString)
    }

    @Test
    fun savedDraftRoundTripsAllBillLines() {
        val draft = PosDraftOrder(
            id = "draft-1",
            title = "Walk-in · 15:20",
            customerName = "Walk-in Retail Shopper",
            customerPhone = "9876543210",
            paymentMode = "UPI",
            items = listOf(
                PosBillItem(
                    id = 4,
                    productId = 13,
                    name = "Charcoal Soap",
                    sku = "BV-LUX-CSOAP-04",
                    barcode = "8908020260046",
                    quantity = 2,
                    price = 199.0,
                    mrp = 399.0,
                ),
            ),
            savedAt = 1_794_045_600_000L,
        )

        val restored = Gson().fromJson(Gson().toJson(draft), PosDraftOrder::class.java)

        assertEquals(draft, restored)
    }
}
