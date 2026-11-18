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
import com.example.data.model.isValidIsoDate
import com.example.data.model.todayIsoDate
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class WorkspaceBackupPayload(
    val profile: BusinessProfile?,
    val transactions: List<BusinessTransaction>,
    val inventory: List<InventoryItem>,
    val customers: List<CustomerRecord>,
    val suppliers: List<SupplierRecord>,
    val purchases: List<PurchaseOrderRecord>,
    val stockMovements: List<StockMovementRecord>,
    val campaigns: List<DigitalMarketingCampaign>,
    val communications: List<OnlineCommunicationLog>
)

data class BackupSummary(
    val exportedAtIso: String,
    val businessName: String,
    val profileIncluded: Boolean,
    val transactionsCount: Int,
    val inventoryCount: Int,
    val customersCount: Int,
    val suppliersCount: Int,
    val purchasesCount: Int,
    val stockMovementsCount: Int,
    val campaignsCount: Int,
    val communicationsCount: Int
) {
    val totalRecords: Int
        get() = transactionsCount + inventoryCount + customersCount + suppliersCount +
            purchasesCount + stockMovementsCount + campaignsCount + communicationsCount +
            if (profileIncluded) 1 else 0
}

data class ValidatedWorkspaceBackup(
    val schemaVersion: Int,
    val appId: String,
    val exportedAtIso: String,
    val payload: WorkspaceBackupPayload,
    val summary: BackupSummary
)

object ProjectXBackupManager {

    const val SCHEMA_VERSION = 1
    const val APP_ID = "com.aistudio.projectx.nqkwxm"
    private const val MAX_JSON_BYTES = 10 * 1024 * 1024 // 10 MB safety limit

    private val VALID_TX_TYPES = setOf(
        BusinessTransaction.TYPE_SALE,
        BusinessTransaction.TYPE_EXPENSE,
        BusinessTransaction.TYPE_OTHER
    )
    private val VALID_PAYMENT_METHODS = BusinessTransaction.allPaymentMethods.toSet()
    private val VALID_TX_STATUSES = BusinessTransaction.paymentStatuses.toSet()
    private val VALID_PO_STATUSES = PurchaseOrderRecord.paymentStatuses.toSet()
    private val VALID_MOVEMENT_TYPES = setOf(
        StockMovementRecord.TYPE_SALE_DEDUCTION,
        StockMovementRecord.TYPE_PURCHASE_RESTOCK,
        StockMovementRecord.TYPE_MANUAL_ADJUSTMENT,
        StockMovementRecord.TYPE_SALE_REVERSAL
    )
    private val VALID_CAMPAIGN_CHANNELS = DigitalMarketingCampaign.channels.toSet()
    private val VALID_CAMPAIGN_STATUSES = DigitalMarketingCampaign.statuses.toSet()
    private val VALID_COMM_CHANNELS = OnlineCommunicationLog.channels.toSet()
    private val VALID_COMM_CATEGORIES = OnlineCommunicationLog.categories.toSet()
    private val VALID_COMM_STATUSES = OnlineCommunicationLog.statuses.toSet()
    private val DOC_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

    fun serializeWorkspaceBackup(
        payload: WorkspaceBackupPayload,
        exportedAtIso: String = currentUtcIsoTimestamp()
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("appId", APP_ID)
        root.put("exportedAtIso", exportedAtIso)

        if (payload.profile != null) {
            val p = payload.profile
            root.put(
                "businessProfile",
                JSONObject().apply {
                    put("ownerName", p.ownerName)
                    put("businessName", p.businessName)
                    put("businessType", p.businessType)
                    put("locationNakuru", p.locationNakuru)
                    put("businessSize", p.businessSize)
                    put("phone", p.phone)
                    put("currency", "KES")
                    put("demoModeEnabled", false)
                }
            )
        } else {
            root.put("businessProfile", JSONObject.NULL)
        }

        root.put(
            "transactions",
            JSONArray().apply {
                payload.transactions.forEach { tx ->
                    put(
                        JSONObject().apply {
                            put("id", tx.id)
                            put("type", tx.type)
                            put("title", tx.title)
                            put("category", tx.category)
                            put("amountKes", tx.amountKes)
                            put("paymentMethod", tx.paymentMethod)
                            put("transactionRef", tx.transactionRef)
                            put("paymentStatus", tx.paymentStatus)
                            put("transactionDate", tx.effectiveDate())
                            put("notes", tx.notes)
                            put("customerId", tx.customerId)
                            put("customerName", tx.customerName)
                            put("inventoryItemId", tx.inventoryItemId)
                            put("inventoryItemName", tx.inventoryItemName)
                            put("quantitySold", tx.quantitySold)
                            put("unitPriceKes", tx.unitPriceKes)
                            put("discountKes", tx.discountKes)
                            put("costOfGoodsSoldKes", tx.costOfGoodsSoldKes)
                            put("receiptNumber", tx.receiptNumber)
                        }
                    )
                }
            }
        )

        root.put(
            "inventory",
            JSONArray().apply {
                payload.inventory.forEach { item ->
                    put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("name", item.name)
                            put("sku", item.sku)
                            put("category", item.category)
                            put("quantity", item.quantity)
                            put("reorderLevel", item.reorderLevel)
                            put("unitPriceKes", item.unitPriceKes)
                            put("unitCostKes", item.unitCostKes)
                            put("supplierId", item.supplierId)
                            put("supplierName", item.supplierName)
                        }
                    )
                }
            }
        )

        root.put(
            "customers",
            JSONArray().apply {
                payload.customers.forEach { c ->
                    put(
                        JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                            put("phone", c.phone)
                            put("email", c.email)
                            put("location", c.location)
                            put("notes", c.notes)
                            put("totalSpentKes", c.totalSpentKes)
                            put("purchaseCount", c.purchaseCount)
                            put("lastPurchaseDate", c.lastPurchaseDate)
                            put("createdDate", c.effectiveCreatedDate())
                        }
                    )
                }
            }
        )

        root.put(
            "suppliers",
            JSONArray().apply {
                payload.suppliers.forEach { s ->
                    put(
                        JSONObject().apply {
                            put("id", s.id)
                            put("name", s.name)
                            put("contactPerson", s.contactPerson)
                            put("phone", s.phone)
                            put("email", s.email)
                            put("location", s.location)
                            put("suppliedProductsSummary", s.suppliedProductsSummary)
                            put("notes", s.notes)
                        }
                    )
                }
            }
        )

        root.put(
            "purchases",
            JSONArray().apply {
                payload.purchases.forEach { po ->
                    put(
                        JSONObject().apply {
                            put("id", po.id)
                            put("supplierId", po.supplierId)
                            put("supplierName", po.supplierName)
                            put("inventoryItemId", po.inventoryItemId)
                            put("itemName", po.itemName)
                            put("quantity", po.quantity)
                            put("unitCostKes", po.unitCostKes)
                            put("totalCostKes", po.totalCostKes)
                            put("paymentMethod", po.paymentMethod)
                            put("paymentStatus", po.paymentStatus)
                            put("purchaseDate", po.effectiveDate())
                            put("notes", po.notes)
                        }
                    )
                }
            }
        )

        root.put(
            "stockMovements",
            JSONArray().apply {
                payload.stockMovements.forEach { mv ->
                    put(
                        JSONObject().apply {
                            put("id", mv.id)
                            put("inventoryItemId", mv.inventoryItemId)
                            put("itemName", mv.itemName)
                            put("movementType", mv.movementType)
                            put("quantityDelta", mv.quantityDelta)
                            put("balanceAfter", mv.balanceAfter)
                            put("referenceId", mv.referenceId)
                            put("unitCostKes", mv.unitCostKes)
                            put("totalCostKes", mv.totalCostKes)
                            put("supplierId", mv.supplierId)
                            put("supplierName", mv.supplierName)
                            put("movementDate", mv.effectiveDate())
                            put("notes", mv.notes)
                        }
                    )
                }
            }
        )

        root.put(
            "campaigns",
            JSONArray().apply {
                payload.campaigns.forEach { camp ->
                    put(
                        JSONObject().apply {
                            put("id", camp.id)
                            put("title", camp.title)
                            put("channel", camp.channel)
                            put("campaignDate", camp.effectiveDate())
                            put("spendKes", camp.spendKes)
                            put("reachCount", camp.reachCount)
                            put("engagementCount", camp.engagementCount)
                            put("inquiriesGenerated", camp.inquiriesGenerated)
                            put("status", camp.status)
                            put("notes", camp.notes)
                        }
                    )
                }
            }
        )

        root.put(
            "communications",
            JSONArray().apply {
                payload.communications.forEach { comm ->
                    put(
                        JSONObject().apply {
                            put("id", comm.id)
                            put("customerName", comm.customerName)
                            put("channel", comm.channel)
                            put("category", comm.category)
                            put("subject", comm.subject)
                            put("receivedDate", comm.effectiveReceivedDate())
                            put("responseDate", comm.responseDate)
                            put("status", comm.status)
                            put("responseMinutes", comm.responseMinutes)
                            put("notes", comm.notes)
                        }
                    )
                }
            }
        )

        return root.toString(2)
    }

    fun parseAndValidateBackup(rawJson: String): Result<ValidatedWorkspaceBackup> {
        return runCatching {
            val trimmed = rawJson.trim()
            require(trimmed.isNotEmpty()) { "Backup file is empty." }
            require(trimmed.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) {
                "Backup file exceeds the 10 MB maximum size."
            }

            val root = JSONObject(trimmed)
            val schemaVersion = root.optInt("schemaVersion", -1)
            require(schemaVersion == SCHEMA_VERSION) {
                "Unsupported backup schema version ($schemaVersion). Expected version $SCHEMA_VERSION."
            }
            val appId = root.optString("appId", "")
            require(appId == APP_ID) {
                "Backup file does not belong to Project X ($APP_ID)."
            }
            val exportedAtIso = root.optString("exportedAtIso", "").ifBlank { todayIsoDate() }

            val profile = if (root.has("businessProfile") && !root.isNull("businessProfile")) {
                val pObj = root.getJSONObject("businessProfile")
                val ownerName = pObj.optString("ownerName", "").trim().take(80)
                val businessName = pObj.optString("businessName", "").trim().take(100)
                val businessType = pObj.optString("businessType", "").trim().take(50)
                val locationNakuru = pObj.optString("locationNakuru", "").trim().take(80)
                require(ownerName.isNotEmpty() && businessName.isNotEmpty() && businessType.isNotEmpty() && locationNakuru.isNotEmpty()) {
                    "Backup businessProfile is missing required ownerName, businessName, businessType, or location."
                }
                val currency = pObj.optString("currency", "KES").trim()
                require(currency == "KES") { "Unsupported backup currency '$currency'. Expected KES." }
                BusinessProfile(
                    ownerName = ownerName,
                    businessName = businessName,
                    businessType = businessType,
                    locationNakuru = locationNakuru,
                    businessSize = pObj.optString("businessSize", "Small (6–20 staff)").trim().take(50),
                    phone = pObj.optString("phone", "").trim().take(25),
                    currency = "KES",
                    demoModeEnabled = false
                )
            } else null

            val txArr = root.optJSONArray("transactions")
                ?: throw IllegalArgumentException("Backup is missing 'transactions' array.")
            val transactions = buildList {
                for (i in 0 until txArr.length()) {
                    val o = txArr.getJSONObject(i)
                    val type = o.optString("type", "")
                    require(type in VALID_TX_TYPES) { "Invalid transaction type '$type' at index $i." }
                    val title = o.optString("title", "").trim().take(100)
                    val category = o.optString("category", "").trim().take(50)
                    require(title.isNotEmpty() && category.isNotEmpty()) {
                        "Transaction at index $i is missing title or category."
                    }
                    val amountKes = o.optDouble("amountKes", Double.NaN)
                    require(amountKes.isFinite() && amountKes > 0.0 && amountKes <= 100_000_000.0) {
                        "Transaction '$title' has invalid KES amount."
                    }
                    val method = o.optString("paymentMethod", "")
                    require(method in VALID_PAYMENT_METHODS) {
                        "Transaction '$title' has invalid paymentMethod '$method'."
                    }
                    val status = o.optString("paymentStatus", BusinessTransaction.STATUS_COMPLETED)
                    require(status in VALID_TX_STATUSES) {
                        "Transaction '$title' has invalid paymentStatus '$status'."
                    }
                    val txDate = o.optString("transactionDate", "").trim().take(20)
                    if (txDate.isNotEmpty()) {
                        require(isValidIsoDate(txDate)) { "Transaction '$title' has invalid date '$txDate'." }
                    }
                    val qtySold = o.optLong("quantitySold", 0L).coerceAtLeast(0L)
                    val unitPrice = o.optDouble("unitPriceKes", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                    val discount = o.optDouble("discountKes", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                    val cogs = o.optDouble("costOfGoodsSoldKes", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                    add(
                        BusinessTransaction(
                            id = sanitizeDocId(o.optString("id", ""), "tx", i),
                            type = type,
                            title = title,
                            category = category,
                            amountKes = amountKes,
                            paymentMethod = method,
                            transactionRef = o.optString("transactionRef", "").trim().take(80),
                            paymentStatus = status,
                            transactionDate = txDate,
                            notes = o.optString("notes", "").trim().take(300),
                            customerId = o.optString("customerId", "").trim().take(128),
                            customerName = o.optString("customerName", "").trim().take(100),
                            inventoryItemId = o.optString("inventoryItemId", "").trim().take(128),
                            inventoryItemName = o.optString("inventoryItemName", "").trim().take(100),
                            quantitySold = qtySold,
                            unitPriceKes = unitPrice,
                            discountKes = discount,
                            costOfGoodsSoldKes = cogs,
                            receiptNumber = o.optString("receiptNumber", "").trim().take(50)
                        )
                    )
                }
            }

            val invArr = root.optJSONArray("inventory")
                ?: throw IllegalArgumentException("Backup is missing 'inventory' array.")
            val inventory = buildList {
                for (i in 0 until invArr.length()) {
                    val o = invArr.getJSONObject(i)
                    val name = o.optString("name", "").trim().take(100)
                    val category = o.optString("category", "").trim().take(50)
                    require(name.isNotEmpty() && category.isNotEmpty()) {
                        "Inventory item at index $i is missing name or category."
                    }
                    val qty = o.optLong("quantity", -1L)
                    val reorder = o.optLong("reorderLevel", 0L)
                    val unitPrice = o.optDouble("unitPriceKes", Double.NaN)
                    val unitCost = o.optDouble("unitCostKes", 0.0)
                    require(qty in 0L..1_000_000L && reorder in 0L..1_000_000L) {
                        "Inventory '$name' has invalid stock quantity or reorder level."
                    }
                    require(unitPrice.isFinite() && unitPrice in 0.0..100_000_000.0 && unitCost.isFinite() && unitCost >= 0.0) {
                        "Inventory '$name' has invalid unit price or cost."
                    }
                    add(
                        InventoryItem(
                            id = sanitizeDocId(o.optString("id", ""), "inv", i),
                            name = name,
                            sku = o.optString("sku", "").trim().take(40),
                            category = category,
                            quantity = qty,
                            reorderLevel = reorder,
                            unitPriceKes = unitPrice,
                            unitCostKes = unitCost,
                            supplierId = o.optString("supplierId", "").trim().take(128),
                            supplierName = o.optString("supplierName", "").trim().take(100)
                        )
                    )
                }
            }

            val custArr = root.optJSONArray("customers")
                ?: throw IllegalArgumentException("Backup is missing 'customers' array.")
            val customers = buildList {
                for (i in 0 until custArr.length()) {
                    val o = custArr.getJSONObject(i)
                    val name = o.optString("name", "").trim().take(100)
                    require(name.isNotEmpty()) { "Customer at index $i is missing name." }
                    val spent = o.optDouble("totalSpentKes", 0.0)
                    val count = o.optLong("purchaseCount", 0L)
                    require(spent.isFinite() && spent in 0.0..100_000_000.0 && count in 0L..1_000_000L) {
                        "Customer '$name' has invalid spend or purchase count."
                    }
                    add(
                        CustomerRecord(
                            id = sanitizeDocId(o.optString("id", ""), "cust", i),
                            name = name,
                            phone = o.optString("phone", "").trim().take(30),
                            email = o.optString("email", "").trim().take(120),
                            location = o.optString("location", "").trim().take(80),
                            notes = o.optString("notes", "").trim().take(300),
                            totalSpentKes = spent,
                            purchaseCount = count,
                            lastPurchaseDate = o.optString("lastPurchaseDate", "").trim().take(20),
                            createdDate = o.optString("createdDate", "").trim().take(20)
                        )
                    )
                }
            }

            val supArr = root.optJSONArray("suppliers")
                ?: throw IllegalArgumentException("Backup is missing 'suppliers' array.")
            val suppliers = buildList {
                for (i in 0 until supArr.length()) {
                    val o = supArr.getJSONObject(i)
                    val name = o.optString("name", "").trim().take(100)
                    require(name.isNotEmpty()) { "Supplier at index $i is missing name." }
                    add(
                        SupplierRecord(
                            id = sanitizeDocId(o.optString("id", ""), "sup", i),
                            name = name,
                            contactPerson = o.optString("contactPerson", "").trim().take(100),
                            phone = o.optString("phone", "").trim().take(30),
                            email = o.optString("email", "").trim().take(120),
                            location = o.optString("location", "").trim().take(80),
                            suppliedProductsSummary = o.optString("suppliedProductsSummary", "").trim().take(250),
                            notes = o.optString("notes", "").trim().take(300)
                        )
                    )
                }
            }

            val poArr = root.optJSONArray("purchases")
                ?: throw IllegalArgumentException("Backup is missing 'purchases' array.")
            val purchases = buildList {
                for (i in 0 until poArr.length()) {
                    val o = poArr.getJSONObject(i)
                    val itemName = o.optString("itemName", "").trim().take(100)
                    require(itemName.isNotEmpty()) { "Purchase order at index $i is missing itemName." }
                    val qty = o.optLong("quantity", 0L)
                    val unitCost = o.optDouble("unitCostKes", 0.0)
                    val totalCost = o.optDouble("totalCostKes", 0.0)
                    val method = o.optString("paymentMethod", BusinessTransaction.METHOD_MPESA)
                    val status = o.optString("paymentStatus", PurchaseOrderRecord.STATUS_PAID)
                    require(qty in 1L..1_000_000L && unitCost.isFinite() && unitCost >= 0.0 && totalCost.isFinite() && totalCost >= 0.0) {
                        "Purchase order '$itemName' has invalid quantity or cost."
                    }
                    require(method in VALID_PAYMENT_METHODS && status in VALID_PO_STATUSES) {
                        "Purchase order '$itemName' has invalid paymentMethod or paymentStatus."
                    }
                    add(
                        PurchaseOrderRecord(
                            id = sanitizeDocId(o.optString("id", ""), "po", i),
                            supplierId = o.optString("supplierId", "").trim().take(128),
                            supplierName = o.optString("supplierName", "").trim().take(100),
                            inventoryItemId = o.optString("inventoryItemId", "").trim().take(128),
                            itemName = itemName,
                            quantity = qty,
                            unitCostKes = unitCost,
                            totalCostKes = totalCost,
                            paymentMethod = method,
                            paymentStatus = status,
                            purchaseDate = o.optString("purchaseDate", "").trim().take(20),
                            notes = o.optString("notes", "").trim().take(300)
                        )
                    )
                }
            }

            val mvArr = root.optJSONArray("stockMovements")
                ?: throw IllegalArgumentException("Backup is missing 'stockMovements' array.")
            val stockMovements = buildList {
                for (i in 0 until mvArr.length()) {
                    val o = mvArr.getJSONObject(i)
                    val invId = o.optString("inventoryItemId", "").trim().take(128)
                    val itemName = o.optString("itemName", "").trim().take(100)
                    val movementType = o.optString("movementType", "")
                    val delta = o.optLong("quantityDelta", 0L)
                    val balanceAfter = o.optLong("balanceAfter", -1L)
                    require(invId.isNotEmpty() && itemName.isNotEmpty() && movementType in VALID_MOVEMENT_TYPES) {
                        "Stock movement at index $i has invalid item or movementType."
                    }
                    require(delta in -1_000_000L..1_000_000L && balanceAfter in 0L..1_000_000L) {
                        "Stock movement '$itemName' has invalid quantity delta or balance."
                    }
                    add(
                        StockMovementRecord(
                            id = sanitizeDocId(o.optString("id", ""), "mv", i),
                            inventoryItemId = invId,
                            itemName = itemName,
                            movementType = movementType,
                            quantityDelta = delta,
                            balanceAfter = balanceAfter,
                            referenceId = o.optString("referenceId", "").trim().take(128),
                            unitCostKes = o.optDouble("unitCostKes", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                            totalCostKes = o.optDouble("totalCostKes", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                            supplierId = o.optString("supplierId", "").trim().take(128),
                            supplierName = o.optString("supplierName", "").trim().take(100),
                            movementDate = o.optString("movementDate", "").trim().take(20),
                            notes = o.optString("notes", "").trim().take(300)
                        )
                    )
                }
            }

            val campArr = root.optJSONArray("campaigns")
                ?: throw IllegalArgumentException("Backup is missing 'campaigns' array.")
            val campaigns = buildList {
                for (i in 0 until campArr.length()) {
                    val o = campArr.getJSONObject(i)
                    val title = o.optString("title", "").trim().take(100)
                    val channel = o.optString("channel", "")
                    val status = o.optString("status", DigitalMarketingCampaign.STATUS_ACTIVE)
                    val spend = o.optDouble("spendKes", 0.0)
                    val reach = o.optLong("reachCount", 0L)
                    val eng = o.optLong("engagementCount", 0L)
                    val inq = o.optLong("inquiriesGenerated", 0L)
                    require(title.isNotEmpty() && channel in VALID_CAMPAIGN_CHANNELS && status in VALID_CAMPAIGN_STATUSES) {
                        "Campaign at index $i has invalid title, channel, or status."
                    }
                    require(spend.isFinite() && spend >= 0.0 && reach >= 0L && eng >= 0L && inq >= 0L) {
                        "Campaign '$title' has negative metrics."
                    }
                    add(
                        DigitalMarketingCampaign(
                            id = sanitizeDocId(o.optString("id", ""), "camp", i),
                            title = title,
                            channel = channel,
                            campaignDate = o.optString("campaignDate", "").trim().take(20),
                            spendKes = spend,
                            reachCount = reach,
                            engagementCount = eng,
                            inquiriesGenerated = inq,
                            status = status,
                            notes = o.optString("notes", "").trim().take(300)
                        )
                    )
                }
            }

            val commArr = root.optJSONArray("communications")
                ?: throw IllegalArgumentException("Backup is missing 'communications' array.")
            val communications = buildList {
                for (i in 0 until commArr.length()) {
                    val o = commArr.getJSONObject(i)
                    val customerName = o.optString("customerName", "").trim().take(100)
                    val channel = o.optString("channel", "")
                    val category = o.optString("category", OnlineCommunicationLog.CATEGORY_INQUIRY)
                    val subject = o.optString("subject", "").trim().take(120)
                    val status = o.optString("status", OnlineCommunicationLog.STATUS_RESPONDED)
                    val responseMinutes = o.optLong("responseMinutes", 0L)
                    require(
                        customerName.isNotEmpty() &&
                            subject.isNotEmpty() &&
                            channel in VALID_COMM_CHANNELS &&
                            category in VALID_COMM_CATEGORIES &&
                            status in VALID_COMM_STATUSES &&
                            responseMinutes >= 0L
                    ) {
                        "Communication log at index $i has invalid required fields."
                    }
                    add(
                        OnlineCommunicationLog(
                            id = sanitizeDocId(o.optString("id", ""), "comm", i),
                            customerName = customerName,
                            channel = channel,
                            category = category,
                            subject = subject,
                            receivedDate = o.optString("receivedDate", "").trim().take(20),
                            responseDate = o.optString("responseDate", "").trim().take(20),
                            status = status,
                            responseMinutes = responseMinutes,
                            notes = o.optString("notes", "").trim().take(300)
                        )
                    )
                }
            }

            val payload = WorkspaceBackupPayload(
                profile = profile,
                transactions = transactions,
                inventory = inventory,
                customers = customers,
                suppliers = suppliers,
                purchases = purchases,
                stockMovements = stockMovements,
                campaigns = campaigns,
                communications = communications
            )
            val summary = BackupSummary(
                exportedAtIso = exportedAtIso,
                businessName = profile?.businessName ?: "Unspecified Business Profile",
                profileIncluded = profile != null,
                transactionsCount = transactions.size,
                inventoryCount = inventory.size,
                customersCount = customers.size,
                suppliersCount = suppliers.size,
                purchasesCount = purchases.size,
                stockMovementsCount = stockMovements.size,
                campaignsCount = campaigns.size,
                communicationsCount = communications.size
            )
            require(summary.totalRecords > 0) {
                "Backup file contains zero business records to restore."
            }
            ValidatedWorkspaceBackup(
                schemaVersion = schemaVersion,
                appId = appId,
                exportedAtIso = exportedAtIso,
                payload = payload,
                summary = summary
            )
        }
    }

    private fun sanitizeDocId(rawId: String, prefix: String, index: Int): String {
        val candidate = rawId.trim()
        return if (DOC_ID_REGEX.matches(candidate)) {
            candidate
        } else {
            "restored_${prefix}_${index}_${System.currentTimeMillis() % 100000L}"
        }
    }

    private fun currentUtcIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
