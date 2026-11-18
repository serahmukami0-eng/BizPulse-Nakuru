package com.example.data.backup

import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.StockMovementRecord
import com.example.data.model.SupplierRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProjectXBackupManagerTest {

    @Test
    fun serializeAndParse_roundTripsFullWorkspacePayload() {
        val payload = WorkspaceBackupPayload(
            profile = BusinessProfile(
                ownerName = "Serah Mukami",
                businessName = "Rift Valley Mart",
                businessType = "Retail Shop",
                locationNakuru = "Kenyatta Avenue, Nakuru",
                businessSize = "Small (6–20 staff)",
                phone = "+254712345678",
                currency = "KES",
                demoModeEnabled = true
            ),
            transactions = listOf(
                BusinessTransaction(
                    id = "tx_sale_1",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Maize Flour 2kg x5",
                    category = "Retail Sales",
                    amountKes = 1250.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionRef = "TI98PX100",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = "2026-11-16",
                    customerId = "cust_1",
                    customerName = "Grace Wambui",
                    inventoryItemId = "inv_1",
                    inventoryItemName = "Maize Flour 2kg",
                    quantitySold = 5L,
                    unitPriceKes = 250.0,
                    costOfGoodsSoldKes = 950.0,
                    receiptNumber = "PX-880112"
                )
            ),
            inventory = listOf(
                InventoryItem(
                    id = "inv_1",
                    name = "Maize Flour 2kg",
                    sku = "MF-02",
                    category = "Dry Goods",
                    quantity = 40L,
                    reorderLevel = 10L,
                    unitPriceKes = 250.0,
                    unitCostKes = 190.0,
                    supplierId = "sup_1",
                    supplierName = "Unga Millers"
                )
            ),
            customers = listOf(
                CustomerRecord(
                    id = "cust_1",
                    name = "Grace Wambui",
                    phone = "+254712000111",
                    email = "grace@example.co.ke",
                    location = "Milimani, Nakuru",
                    totalSpentKes = 1250.0,
                    purchaseCount = 1L,
                    lastPurchaseDate = "2026-11-16",
                    createdDate = "2026-11-10"
                )
            ),
            suppliers = listOf(
                SupplierRecord(
                    id = "sup_1",
                    name = "Unga Millers",
                    contactPerson = "Peter Karanja",
                    phone = "+254722111222",
                    location = "Industrial Area, Nakuru",
                    suppliedProductsSummary = "Maize & Wheat Flour"
                )
            ),
            purchases = listOf(
                PurchaseOrderRecord(
                    id = "po_1",
                    supplierId = "sup_1",
                    supplierName = "Unga Millers",
                    inventoryItemId = "inv_1",
                    itemName = "Maize Flour 2kg",
                    quantity = 20L,
                    unitCostKes = 190.0,
                    totalCostKes = 3800.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    paymentStatus = PurchaseOrderRecord.STATUS_PAID,
                    purchaseDate = "2026-11-12"
                )
            ),
            stockMovements = listOf(
                StockMovementRecord(
                    id = "mv_1",
                    inventoryItemId = "inv_1",
                    itemName = "Maize Flour 2kg",
                    movementType = StockMovementRecord.TYPE_PURCHASE_RESTOCK,
                    quantityDelta = 20L,
                    balanceAfter = 45L,
                    referenceId = "po_1",
                    unitCostKes = 190.0,
                    totalCostKes = 3800.0,
                    supplierId = "sup_1",
                    supplierName = "Unga Millers",
                    movementDate = "2026-11-12"
                )
            ),
            campaigns = listOf(
                DigitalMarketingCampaign(
                    id = "camp_1",
                    title = "WhatsApp Broadcast Nov",
                    channel = DigitalMarketingCampaign.CHANNEL_WHATSAPP,
                    campaignDate = "2026-11-14",
                    spendKes = 600.0,
                    reachCount = 450L,
                    engagementCount = 85L,
                    inquiriesGenerated = 16L,
                    status = DigitalMarketingCampaign.STATUS_ACTIVE
                )
            ),
            communications = listOf(
                OnlineCommunicationLog(
                    id = "comm_1",
                    customerName = "Grace Wambui",
                    channel = OnlineCommunicationLog.CHANNEL_WHATSAPP,
                    category = OnlineCommunicationLog.CATEGORY_INQUIRY,
                    subject = "Delivery quote",
                    receivedDate = "2026-11-16",
                    responseDate = "2026-11-16",
                    status = OnlineCommunicationLog.STATUS_CONVERTED,
                    responseMinutes = 4L
                )
            )
        )

        val json = ProjectXBackupManager.serializeWorkspaceBackup(
            payload = payload,
            exportedAtIso = "2026-11-16T10:00:00Z"
        )
        val parsedRes = ProjectXBackupManager.parseAndValidateBackup(json)
        assertTrue(parsedRes.isSuccess)
        val parsed = parsedRes.getOrThrow()

        assertEquals(ProjectXBackupManager.SCHEMA_VERSION, parsed.schemaVersion)
        assertEquals(ProjectXBackupManager.APP_ID, parsed.appId)
        assertEquals("Rift Valley Mart", parsed.summary.businessName)
        assertEquals(false, parsed.payload.profile?.demoModeEnabled)
        assertEquals(1, parsed.summary.transactionsCount)
        assertEquals(1, parsed.summary.inventoryCount)
        assertEquals(1, parsed.summary.customersCount)
        assertEquals(1, parsed.summary.suppliersCount)
        assertEquals(1, parsed.summary.purchasesCount)
        assertEquals(1, parsed.summary.stockMovementsCount)
        assertEquals(1, parsed.summary.campaignsCount)
        assertEquals(1, parsed.summary.communicationsCount)
        assertEquals(9, parsed.summary.totalRecords)
        assertEquals(1250.0, parsed.payload.transactions.first().amountKes, 0.01)
        assertEquals("PX-880112", parsed.payload.transactions.first().receiptNumber)
    }

    @Test
    fun parseAndValidateBackup_rejectsMalformedUnsupportedAndEmptyFiles() {
        assertTrue(ProjectXBackupManager.parseAndValidateBackup("").isFailure)
        assertTrue(ProjectXBackupManager.parseAndValidateBackup("{not valid json").isFailure)

        val wrongAppId = """
            {
              "schemaVersion": 1,
              "appId": "com.other.app",
              "businessProfile": null,
              "transactions": [{"id":"t1","type":"SALE","title":"Tea","category":"Retail","amountKes":100,"paymentMethod":"M-PESA"}],
              "inventory": [], "customers": [], "suppliers": [], "purchases": [],
              "stockMovements": [], "campaigns": [], "communications": []
            }
        """.trimIndent()
        assertTrue(ProjectXBackupManager.parseAndValidateBackup(wrongAppId).isFailure)

        val negativeAmount = """
            {
              "schemaVersion": 1,
              "appId": "${ProjectXBackupManager.APP_ID}",
              "businessProfile": null,
              "transactions": [{"id":"t1","type":"SALE","title":"Tea","category":"Retail","amountKes":-50,"paymentMethod":"M-PESA"}],
              "inventory": [], "customers": [], "suppliers": [], "purchases": [],
              "stockMovements": [], "campaigns": [], "communications": []
            }
        """.trimIndent()
        assertTrue(ProjectXBackupManager.parseAndValidateBackup(negativeAmount).isFailure)

        val emptyRecords = ProjectXBackupManager.serializeWorkspaceBackup(
            WorkspaceBackupPayload(
                profile = null,
                transactions = emptyList(),
                inventory = emptyList(),
                customers = emptyList(),
                suppliers = emptyList(),
                purchases = emptyList(),
                stockMovements = emptyList(),
                campaigns = emptyList(),
                communications = emptyList()
            )
        )
        assertTrue(ProjectXBackupManager.parseAndValidateBackup(emptyRecords).isFailure)
    }
}
