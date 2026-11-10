package com.bhumivera.scanner.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerWorkflowContractTest {
    @Test
    fun posCheckoutIncludesPairedTerminalSessionAndOrderLock() {
        val request = PosCheckoutRequest(
            customer = PosCustomer(),
            items = listOf(mapOf("productId" to 12, "quantity" to 2)),
            sessionCode = "STAFF-AKASH",
            orderId = "1234",
            operatorName = "Akash",
        )

        val serialized = JsonParser.parseString(Gson().toJson(request)).asJsonObject

        assertEquals("STAFF-AKASH", serialized.get("sessionCode").asString)
        assertEquals("1234", serialized.get("orderId").asString)
        assertEquals("Akash", serialized.get("operatorName").asString)
    }

    @Test
    fun savedDraftRoundTripsAllBillLinesAndPausedStatus() {
        val draft = PosDraftOrder(
            id = "draft-1234",
            title = "Order #1234 · Walk-in",
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
            orderId = "1234",
            status = "paused",
            createdByName = "Akash",
        )

        val restored = Gson().fromJson(Gson().toJson(draft), PosDraftOrder::class.java)

        assertEquals(draft, restored)
    }
}
