package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object ResearchFramework {
    const val RESEARCH_TITLE =
        "THE EFFECT OF DIGITAL TECHNOLOGY ADOPTION ON THE PERFORMANCE OF SMALL AND MEDIUM-SIZED ENTERPRISES IN NAKURU TOWN, KENYA."
    const val DEPENDENT_VARIABLE = "SME Performance"
    const val METHODOLOGY_NOTE =
        "Indicators show observed relationships in recorded enterprise data. Observed associations do not automatically establish causal effects."

    val independentVariables = listOf(
        "IV1: Digital Marketing",
        "IV2: Digital Payment Systems",
        "IV3: Computerized Record Keeping",
        "IV4: Online Communication"
    )

    val performanceDimensions = listOf(
        "Sales growth",
        "Customer growth and reach",
        "Operational efficiency",
        "Profitability and business growth"
    )
}

@Keep
@IgnoreExtraProperties
data class BusinessProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ownerName: String = "",
    val businessName: String = "",
    val businessType: String = "Retail Shop",
    val locationNakuru: String = "Biashara Street, Nakuru",
    val businessSize: String = "Small (6–20 staff)",
    val phone: String = "",
    val currency: String = "KES",
    val demoModeEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BusinessTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TYPE_SALE,
    val title: String = "",
    val category: String = "",
    val amountKes: Double = 0.0,
    val paymentMethod: String = METHOD_MPESA,
    val transactionRef: String = "",
    val paymentStatus: String = STATUS_COMPLETED,
    val transactionDate: String = "",
    val notes: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val inventoryItemId: String = "",
    val inventoryItemName: String = "",
    val quantitySold: Long = 0L,
    val unitPriceKes: Double = 0.0,
    val discountKes: Double = 0.0,
    val costOfGoodsSoldKes: Double = 0.0,
    val receiptNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isDigitalPayment: Boolean
        get() = paymentMethod != METHOD_CASH

    fun isCancelled(): Boolean = paymentStatus == STATUS_CANCELLED

    fun isExcludedFromTotals(): Boolean =
        paymentStatus == STATUS_CANCELLED || paymentStatus == STATUS_FAILED

    fun isActiveLedgerEntry(): Boolean = !isExcludedFromTotals()

    fun effectiveDate(): String = resolveEffectiveIsoDate(transactionDate, createdAt)

    companion object {
        const val TYPE_SALE = "SALE"
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_OTHER = "OTHER"

        const val METHOD_MPESA = "M-PESA"
        const val METHOD_BANK = "BANK"
        const val METHOD_CARD = "CARD"
        const val METHOD_OTHER_DIGITAL = "OTHER_DIGITAL"
        const val METHOD_CASH = "CASH"

        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"

        val allTypes = listOf(TYPE_SALE, TYPE_EXPENSE, TYPE_OTHER)

        val allPaymentMethods = listOf(
            METHOD_MPESA,
            METHOD_BANK,
            METHOD_CARD,
            METHOD_OTHER_DIGITAL,
            METHOD_CASH
        )

        val paymentStatuses = listOf(STATUS_COMPLETED, STATUS_PENDING, STATUS_FAILED, STATUS_CANCELLED)
    }
}

@Keep
@IgnoreExtraProperties
data class InventoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sku: String = "",
    val category: String = "General",
    val quantity: Long = 0L,
    val reorderLevel: Long = 5L,
    val unitPriceKes: Double = 0.0,
    val unitCostKes: Double = 0.0,
    val supplierId: String = "",
    val supplierName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isLowStock: Boolean get() = quantity <= reorderLevel

    @get:Exclude
    val stockRetailValueKes: Double get() = quantity * unitPriceKes

    @get:Exclude
    val stockCostValueKes: Double get() = quantity * unitCostKes
}

@Keep
@IgnoreExtraProperties
data class CustomerRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val location: String = "Nakuru Town",
    val notes: String = "",
    val totalSpentKes: Double = 0.0,
    val purchaseCount: Long = 0L,
    val lastPurchaseDate: String = "",
    val createdDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isRepeatCustomer: Boolean get() = purchaseCount >= 2L

    fun effectiveCreatedDate(): String = resolveEffectiveIsoDate(createdDate, createdAt)
}

@Keep
@IgnoreExtraProperties
data class SupplierRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val location: String = "Nakuru Industrial Area",
    val suppliedProductsSummary: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PurchaseOrderRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val inventoryItemId: String = "",
    val itemName: String = "",
    val quantity: Long = 0L,
    val unitCostKes: Double = 0.0,
    val totalCostKes: Double = 0.0,
    val paymentMethod: String = BusinessTransaction.METHOD_MPESA,
    val paymentStatus: String = STATUS_PAID,
    val purchaseDate: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveDate(): String = resolveEffectiveIsoDate(purchaseDate, createdAt)

    companion object {
        const val STATUS_PAID = "PAID"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_PARTIAL = "PARTIAL"

        val paymentStatuses = listOf(STATUS_PAID, STATUS_PENDING, STATUS_PARTIAL)
    }
}

@Keep
@IgnoreExtraProperties
data class StockMovementRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val inventoryItemId: String = "",
    val itemName: String = "",
    val movementType: String = TYPE_PURCHASE_RESTOCK,
    val quantityDelta: Long = 0L,
    val balanceAfter: Long = 0L,
    val referenceId: String = "",
    val unitCostKes: Double = 0.0,
    val totalCostKes: Double = 0.0,
    val supplierId: String = "",
    val supplierName: String = "",
    val movementDate: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveDate(): String = resolveEffectiveIsoDate(movementDate, createdAt)

    companion object {
        const val TYPE_SALE_DEDUCTION = "SALE_DEDUCTION"
        const val TYPE_PURCHASE_RESTOCK = "PURCHASE_RESTOCK"
        const val TYPE_MANUAL_ADJUSTMENT = "MANUAL_ADJUSTMENT"
        const val TYPE_SALE_REVERSAL = "SALE_REVERSAL"
    }
}

@Keep
@IgnoreExtraProperties
data class DigitalMarketingCampaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val channel: String = CHANNEL_WHATSAPP,
    val campaignDate: String = "",
    val spendKes: Double = 0.0,
    val reachCount: Long = 0L,
    val engagementCount: Long = 0L,
    val inquiriesGenerated: Long = 0L,
    val status: String = STATUS_ACTIVE,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val engagementRatePct: Double
        get() = if (reachCount > 0L) (engagementCount.toDouble() / reachCount) * 100.0 else 0.0

    fun effectiveDate(): String = resolveEffectiveIsoDate(campaignDate, createdAt)

    companion object {
        const val CHANNEL_FACEBOOK = "FACEBOOK"
        const val CHANNEL_INSTAGRAM = "INSTAGRAM"
        const val CHANNEL_WHATSAPP = "WHATSAPP_STATUS"
        const val CHANNEL_TIKTOK = "TIKTOK"
        const val CHANNEL_GOOGLE_MAPS = "GOOGLE_MAPS"
        const val CHANNEL_SMS = "SMS"

        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"

        val channels = listOf(
            CHANNEL_WHATSAPP,
            CHANNEL_FACEBOOK,
            CHANNEL_INSTAGRAM,
            CHANNEL_TIKTOK,
            CHANNEL_GOOGLE_MAPS,
            CHANNEL_SMS
        )

        val statuses = listOf(STATUS_ACTIVE, STATUS_PAUSED, STATUS_COMPLETED)
    }
}

@Keep
@IgnoreExtraProperties
data class OnlineCommunicationLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val channel: String = CHANNEL_WHATSAPP,
    val category: String = CATEGORY_INQUIRY,
    val subject: String = "",
    val receivedDate: String = "",
    val responseDate: String = "",
    val status: String = STATUS_RESPONDED,
    val responseMinutes: Long = 10L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveReceivedDate(): String = resolveEffectiveIsoDate(receivedDate, createdAt)

    companion object {
        const val CHANNEL_WHATSAPP = "WHATSAPP"
        const val CHANNEL_FACEBOOK_DM = "FACEBOOK_DM"
        const val CHANNEL_INSTAGRAM_DM = "INSTAGRAM_DM"
        const val CHANNEL_EMAIL = "EMAIL"
        const val CHANNEL_SMS = "SMS"
        const val CHANNEL_WEB_CHAT = "WEB_CHAT"
        const val CHANNEL_OTHER = "OTHER"

        const val CATEGORY_INQUIRY = "INQUIRY"
        const val CATEGORY_COMPLAINT = "COMPLAINT"
        const val CATEGORY_ORDER_UPDATE = "ORDER_UPDATE"
        const val CATEGORY_FEEDBACK = "FEEDBACK"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_RESPONDED = "RESPONDED"
        const val STATUS_CONVERTED = "CONVERTED"

        val channels = listOf(
            CHANNEL_WHATSAPP,
            CHANNEL_FACEBOOK_DM,
            CHANNEL_INSTAGRAM_DM,
            CHANNEL_EMAIL,
            CHANNEL_SMS,
            CHANNEL_WEB_CHAT,
            CHANNEL_OTHER
        )

        val categories = listOf(
            CATEGORY_INQUIRY,
            CATEGORY_COMPLAINT,
            CATEGORY_ORDER_UPDATE,
            CATEGORY_FEEDBACK
        )

        val statuses = listOf(STATUS_PENDING, STATUS_RESPONDED, STATUS_CONVERTED)
    }
}

enum class DateFilterOption(val label: String, val maxDaysOld: Int?) {
    ALL("All Dates", null),
    TODAY("Today", 0),
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30)
}

enum class AnalyticsDateFilter(val label: String, val daySpan: Int?) {
    TODAY("Today", 1),
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_90_DAYS("Last 90 Days", 90),
    CUSTOM("Custom Range", null),
    ALL_TIME("All Time", null)
}

data class DateRangeWindow(
    val startIso: String?,
    val endIso: String?
) {
    val label: String
        get() = when {
            startIso == null && endIso == null -> "All recorded dates"
            startIso == endIso -> startIso ?: "Selected date"
            else -> "${startIso ?: "..."} to ${endIso ?: "..."}"
        }
}

data class RegistrationDraft(
    val ownerName: String = "",
    val businessName: String = "",
    val contactEmail: String = "",
    val businessType: String = "Retail Shop",
    val locationNakuru: String = "Biashara Street, Nakuru",
    val businessSize: String = "Small (6–20 staff)",
    val currency: String = "KES",
    val phone: String = ""
)

data class DailyChartPoint(
    val dayLabel: String,
    val salesKes: Double,
    val expensesKes: Double
)

enum class AppModule(
    val id: String,
    val code: String,
    val title: String,
    val shortLabel: String,
    val variableTag: String,
    val subtitle: String
) {
    DASHBOARD(
        id = "dashboard",
        code = "A",
        title = "Business Dashboard",
        shortLabel = "Dashboard",
        variableTag = "Research Hub (IV1–IV4 → DV)",
        subtitle = "Overview, sales, customer growth & profitability"
    ),
    DIGITAL_MARKETING(
        id = "digital_marketing",
        code = "B",
        title = "Digital Marketing",
        shortLabel = "Marketing",
        variableTag = "IV1: Digital Marketing",
        subtitle = "Campaigns, reach, engagement & marketing spend"
    ),
    DIGITAL_PAYMENTS(
        id = "digital_payments",
        code = "C",
        title = "Digital Payment Systems",
        shortLabel = "Payments",
        variableTag = "IV2: Digital Payments",
        subtitle = "M-Pesa, bank transfers, card & adoption share"
    ),
    RECORD_KEEPING(
        id = "record_keeping",
        code = "D",
        title = "Sales, POS & Inventory",
        shortLabel = "Sales/Stock",
        variableTag = "IV3: Digital Records",
        subtitle = "POS sales, receipts, expenses & stock movements"
    ),
    CUSTOMERS(
        id = "customers",
        code = "H",
        title = "Customer Management",
        shortLabel = "Customers",
        variableTag = "DV: Customer Growth",
        subtitle = "Customer directory, spend & repeat buyer insights"
    ),
    SUPPLIERS(
        id = "suppliers",
        code = "I",
        title = "Suppliers & Purchases",
        shortLabel = "Suppliers",
        variableTag = "IV3: Supply & Payables",
        subtitle = "Suppliers, stock restock orders & pending payables"
    ),
    ONLINE_COMMUNICATION(
        id = "online_communication",
        code = "E",
        title = "Online Communication",
        shortLabel = "Comms",
        variableTag = "IV4: Online Comms",
        subtitle = "Inquiries, complaints, response speed & channels"
    ),
    PERFORMANCE_ANALYTICS(
        id = "performance_analytics",
        code = "F",
        title = "Analytics & Projections",
        shortLabel = "Analytics",
        variableTag = "DV: SME Performance",
        subtitle = "7 bar charts, 3–12m forecasts & research metrics"
    ),
    REPORTS(
        id = "reports",
        code = "G",
        title = "Reports & Profile",
        shortLabel = "Reports",
        variableTag = "CSV, PDF, Print & Settings",
        subtitle = "Structured exports, printing & business profile"
    ),
    AI_ASSISTANT(
        id = "ai_assistant",
        code = "AI",
        title = "AI Business Intelligence",
        shortLabel = "AI Intel",
        variableTag = "Gemini Data Advisor",
        subtitle = "Data-grounded recommendations across IV1–IV4"
    )
}

object NakuruSmeCatalog {
    const val APP_TAGLINE = "Your Business. Your Numbers. Your Growth."

    val businessTypes = listOf(
        "Retail Shop",
        "Boutique & Apparel",
        "Salon & Barber",
        "Restaurant & Cafe",
        "Wholesaler & Distributor",
        "Mini-Supermarket",
        "Agrovet & Hardware"
    )

    val nakuruLocations = listOf(
        "Biashara Street, Nakuru",
        "Kenyatta Avenue, Nakuru",
        "Milimani, Nakuru",
        "Lanet, Nakuru",
        "Shabab, Nakuru",
        "Free Area, Nakuru",
        "Section 58, Nakuru",
        "London Estate, Nakuru"
    )

    val businessSizes = listOf(
        "Micro (1–5 staff)",
        "Small (6–20 staff)",
        "Medium (21–50 staff)"
    )

    val currencies = listOf("KES (Kenyan Shilling)")

    val saleCategories = listOf("Retail Sales", "Wholesale", "Services", "Takeaway / Dining", "Online Orders")
    val expenseCategories = listOf("Stock Restock", "Digital Marketing", "Shop Rent", "Staff Wages", "Electricity & Water", "Transport")
    val otherCategories = listOf("Owner Draw", "Capital Injection", "Supplier Deposit", "Equipment Adjustment", "Tax / Permit")
    val inventoryCategories = listOf("Dry Goods", "Beverages", "Apparel", "Salon Supplies", "Fresh Produce", "General")
}

object DemoSampleData {
    const val DEMO_BADGE_TEXT = "DEMO DATA — SAMPLE ONLY"

    val sampleSuppliers = listOf(
        SupplierRecord(
            id = "demo_sup_1",
            name = "Rift Valley Millers Ltd",
            contactPerson = "Peter Karanja",
            phone = "+254722110440",
            email = "orders@rvml.co.ke",
            location = "Industrial Area, Nakuru",
            suppliedProductsSummary = "Pembe Maize Flour 2kg, Wheat Flour",
            notes = "Sample demonstration supplier"
        ),
        SupplierRecord(
            id = "demo_sup_2",
            name = "Menengai Wholesalers",
            contactPerson = "Grace Chebet",
            phone = "+254733890211",
            email = "sales@menengaiwholesale.co.ke",
            location = "Timber Mill Rd, Nakuru",
            suppliedProductsSummary = "Fresh Fri Cooking Oil 1L, Menen Tea Leaves",
            notes = "Sample demonstration supplier"
        ),
        SupplierRecord(
            id = "demo_sup_3",
            name = "Laneti Packaging & Supplies",
            contactPerson = "David Mwangi",
            phone = "+254710554320",
            email = "",
            location = "Lanet, Nakuru",
            suppliedProductsSummary = "Carrier bags, POS thermal rolls",
            notes = "Sample demonstration supplier"
        )
    )

    val sampleCustomers: List<CustomerRecord>
        get() {
            val today = todayIsoDate()
            val oneDayAgo = isoDateDaysAgo(1)
            val thirtyFiveDaysAgo = isoDateDaysAgo(35)
            val sixtyEightDaysAgo = isoDateDaysAgo(68)
            val ninetyFiveDaysAgo = isoDateDaysAgo(95)
            return listOf(
                CustomerRecord(
                    id = "demo_cust_1",
                    name = "Wanjiku Njoroge",
                    phone = "+254712301982",
                    email = "wanjiku.n@example.co.ke",
                    location = "Milimani, Nakuru",
                    totalSpentKes = 24850.0,
                    purchaseCount = 5L,
                    lastPurchaseDate = today,
                    createdDate = ninetyFiveDaysAgo,
                    notes = "Repeat wholesale & cooking oil customer (Sample)"
                ),
                CustomerRecord(
                    id = "demo_cust_2",
                    name = "Kamau Kiprop",
                    phone = "+254720884319",
                    email = "",
                    location = "Shabab, Nakuru",
                    totalSpentKes = 15600.0,
                    purchaseCount = 3L,
                    lastPurchaseDate = oneDayAgo,
                    createdDate = sixtyEightDaysAgo,
                    notes = "Bi-weekly dry goods orders (Sample)"
                ),
                CustomerRecord(
                    id = "demo_cust_3",
                    name = "Amina Hassan",
                    phone = "+254735910422",
                    email = "amina.h@example.co.ke",
                    location = "Section 58, Nakuru",
                    totalSpentKes = 9400.0,
                    purchaseCount = 2L,
                    lastPurchaseDate = isoDateDaysAgo(3),
                    createdDate = thirtyFiveDaysAgo,
                    notes = "Instagram DM buyer (Sample)"
                ),
                CustomerRecord(
                    id = "demo_cust_4",
                    name = "Brian Ochieng",
                    phone = "+254719402810",
                    email = "",
                    location = "Free Area, Nakuru",
                    totalSpentKes = 3850.0,
                    purchaseCount = 1L,
                    lastPurchaseDate = today,
                    createdDate = today,
                    notes = "Walk-in M-Pesa customer (Sample)"
                )
            )
        }

    val sampleTransactions: List<BusinessTransaction>
        get() {
            val today = todayIsoDate()
            val yesterday = isoDateDaysAgo(1)
            val threeDaysAgo = isoDateDaysAgo(3)
            val oneMonthAgo = isoDateDaysAgo(32)
            val twoMonthsAgo = isoDateDaysAgo(63)
            val threeMonthsAgo = isoDateDaysAgo(94)
            return listOf(
                BusinessTransaction(
                    id = "demo_1",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Morning M-Pesa Counter Sales",
                    category = "Retail Sales",
                    amountKes = 18450.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionRef = "TI84MP921K",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = today,
                    customerId = "demo_cust_1",
                    customerName = "Wanjiku Njoroge",
                    inventoryItemId = "demo_inv_1",
                    inventoryItemName = "Pembe Maize Flour 2kg",
                    quantitySold = 30L,
                    unitPriceKes = 615.0,
                    costOfGoodsSoldKes = 12300.0,
                    receiptNumber = "PX-1042",
                    notes = "Sample demonstration record"
                ),
                BusinessTransaction(
                    id = "demo_2",
                    type = BusinessTransaction.TYPE_EXPENSE,
                    title = "Wholesale Flour & Oil Restock",
                    category = "Stock Restock",
                    amountKes = 7200.0,
                    paymentMethod = BusinessTransaction.METHOD_BANK,
                    transactionRef = "EFT-20419",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = yesterday,
                    notes = "Sample demonstration record"
                ),
                BusinessTransaction(
                    id = "demo_3",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Afternoon Walk-in Cash Sales",
                    category = "Retail Sales",
                    amountKes = 9800.0,
                    paymentMethod = BusinessTransaction.METHOD_CASH,
                    transactionRef = "",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = yesterday,
                    customerId = "demo_cust_2",
                    customerName = "Kamau Kiprop",
                    costOfGoodsSoldKes = 6400.0,
                    receiptNumber = "PX-1041",
                    notes = "Sample demonstration record"
                ),
                BusinessTransaction(
                    id = "demo_4",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Online Delivery Order (Card / Till)",
                    category = "Online Orders",
                    amountKes = 6400.0,
                    paymentMethod = BusinessTransaction.METHOD_CARD,
                    transactionRef = "POS-88241",
                    paymentStatus = BusinessTransaction.STATUS_PENDING,
                    transactionDate = threeDaysAgo,
                    customerId = "demo_cust_3",
                    customerName = "Amina Hassan",
                    inventoryItemId = "demo_inv_2",
                    inventoryItemName = "Fresh Fri Cooking Oil 1L",
                    quantitySold = 18L,
                    unitPriceKes = 360.0,
                    discountKes = 80.0,
                    costOfGoodsSoldKes = 4680.0,
                    receiptNumber = "PX-1040",
                    notes = "Sample demonstration record"
                ),
                BusinessTransaction(
                    id = "demo_5",
                    type = BusinessTransaction.TYPE_EXPENSE,
                    title = "Meta & WhatsApp Boost Budget",
                    category = "Digital Marketing",
                    amountKes = 2500.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionRef = "TI82AD118M",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = threeDaysAgo,
                    notes = "Sample demonstration record"
                ),
                BusinessTransaction(
                    id = "demo_6",
                    type = BusinessTransaction.TYPE_OTHER,
                    title = "County Single Business Permit Renewal",
                    category = "Tax / Permit",
                    amountKes = 4500.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionRef = "NKC-PERMIT-09",
                    paymentStatus = BusinessTransaction.STATUS_PENDING,
                    transactionDate = today,
                    notes = "Sample demonstration record"
                ),
                BusinessTransaction(
                    id = "demo_hist_m1_sale",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Previous Month Digital & Till Sales",
                    category = "Retail Sales",
                    amountKes = 29400.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionRef = "TI71MP409A",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = oneMonthAgo,
                    costOfGoodsSoldKes = 18900.0,
                    receiptNumber = "PX-0988",
                    notes = "Historical sample record (-1 month)"
                ),
                BusinessTransaction(
                    id = "demo_hist_m1_exp",
                    type = BusinessTransaction.TYPE_EXPENSE,
                    title = "Previous Month Shop Rent & Utilities",
                    category = "Shop Rent",
                    amountKes = 12800.0,
                    paymentMethod = BusinessTransaction.METHOD_BANK,
                    transactionRef = "EFT-19881",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = oneMonthAgo,
                    notes = "Historical sample record (-1 month)"
                ),
                BusinessTransaction(
                    id = "demo_hist_m2_sale",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Prior 2-Month Wholesale Bales",
                    category = "Wholesale",
                    amountKes = 24600.0,
                    paymentMethod = BusinessTransaction.METHOD_BANK,
                    transactionRef = "EFT-18701",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = twoMonthsAgo,
                    costOfGoodsSoldKes = 16500.0,
                    receiptNumber = "PX-0921",
                    notes = "Historical sample record (-2 months)"
                ),
                BusinessTransaction(
                    id = "demo_hist_m2_exp",
                    type = BusinessTransaction.TYPE_EXPENSE,
                    title = "Prior 2-Month Restock & Wages",
                    category = "Staff Wages",
                    amountKes = 11400.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionRef = "TI60WG992L",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = twoMonthsAgo,
                    notes = "Historical sample record (-2 months)"
                ),
                BusinessTransaction(
                    id = "demo_hist_m3_sale",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Prior 3-Month Opening Period Sales",
                    category = "Retail Sales",
                    amountKes = 21200.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionRef = "TI51MP212B",
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = threeMonthsAgo,
                    costOfGoodsSoldKes = 14100.0,
                    receiptNumber = "PX-0850",
                    notes = "Historical sample record (-3 months)"
                ),
                BusinessTransaction(
                    id = "demo_hist_m3_exp",
                    type = BusinessTransaction.TYPE_EXPENSE,
                    title = "Prior 3-Month Shop Rent & Transport",
                    category = "Shop Rent",
                    amountKes = 10900.0,
                    paymentMethod = BusinessTransaction.METHOD_CASH,
                    paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                    transactionDate = threeMonthsAgo,
                    notes = "Historical sample record (-3 months)"
                )
            )
        }

    val sampleInventory = listOf(
        InventoryItem(
            id = "demo_inv_1",
            name = "Pembe Maize Flour 2kg",
            sku = "PMF-002",
            category = "Dry Goods",
            quantity = 42,
            reorderLevel = 15,
            unitPriceKes = 195.0,
            unitCostKes = 155.0,
            supplierId = "demo_sup_1",
            supplierName = "Rift Valley Millers Ltd"
        ),
        InventoryItem(
            id = "demo_inv_2",
            name = "Fresh Fri Cooking Oil 1L",
            sku = "FFO-010",
            category = "Dry Goods",
            quantity = 6,
            reorderLevel = 10,
            unitPriceKes = 340.0,
            unitCostKes = 260.0,
            supplierId = "demo_sup_2",
            supplierName = "Menengai Wholesalers"
        ),
        InventoryItem(
            id = "demo_inv_3",
            name = "Menen Tea Leaves 250g",
            sku = "MTL-250",
            category = "Beverages",
            quantity = 4,
            reorderLevel = 8,
            unitPriceKes = 165.0,
            unitCostKes = 125.0,
            supplierId = "demo_sup_2",
            supplierName = "Menengai Wholesalers"
        )
    )

    val samplePurchases: List<PurchaseOrderRecord>
        get() {
            val yesterday = isoDateDaysAgo(1)
            val fiveDaysAgo = isoDateDaysAgo(5)
            return listOf(
                PurchaseOrderRecord(
                    id = "demo_po_1",
                    supplierId = "demo_sup_1",
                    supplierName = "Rift Valley Millers Ltd",
                    inventoryItemId = "demo_inv_1",
                    itemName = "Pembe Maize Flour 2kg",
                    quantity = 50L,
                    unitCostKes = 155.0,
                    totalCostKes = 7750.0,
                    paymentMethod = BusinessTransaction.METHOD_BANK,
                    paymentStatus = PurchaseOrderRecord.STATUS_PAID,
                    purchaseDate = fiveDaysAgo,
                    notes = "Sample restock order"
                ),
                PurchaseOrderRecord(
                    id = "demo_po_2",
                    supplierId = "demo_sup_2",
                    supplierName = "Menengai Wholesalers",
                    inventoryItemId = "demo_inv_2",
                    itemName = "Fresh Fri Cooking Oil 1L",
                    quantity = 20L,
                    unitCostKes = 260.0,
                    totalCostKes = 5200.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    paymentStatus = PurchaseOrderRecord.STATUS_PENDING,
                    purchaseDate = yesterday,
                    notes = "Net-7 supplier invoice payable (Sample)"
                )
            )
        }

    val sampleStockMovements: List<StockMovementRecord>
        get() {
            val today = todayIsoDate()
            val yesterday = isoDateDaysAgo(1)
            return listOf(
                StockMovementRecord(
                    id = "demo_mv_1",
                    inventoryItemId = "demo_inv_1",
                    itemName = "Pembe Maize Flour 2kg",
                    movementType = StockMovementRecord.TYPE_SALE_DEDUCTION,
                    quantityDelta = -8L,
                    balanceAfter = 42L,
                    referenceId = "PX-1042",
                    movementDate = today,
                    notes = "Sample POS sale deduction"
                ),
                StockMovementRecord(
                    id = "demo_mv_2",
                    inventoryItemId = "demo_inv_2",
                    itemName = "Fresh Fri Cooking Oil 1L",
                    movementType = StockMovementRecord.TYPE_PURCHASE_RESTOCK,
                    quantityDelta = 20L,
                    balanceAfter = 24L,
                    referenceId = "demo_po_2",
                    unitCostKes = 260.0,
                    totalCostKes = 5200.0,
                    supplierId = "demo_sup_2",
                    supplierName = "Menengai Wholesalers",
                    movementDate = yesterday,
                    notes = "Sample supplier restock"
                )
            )
        }

    val sampleCampaigns: List<DigitalMarketingCampaign>
        get() {
            val today = todayIsoDate()
            val twoDaysAgo = isoDateDaysAgo(2)
            val nineDaysAgo = isoDateDaysAgo(9)
            return listOf(
                DigitalMarketingCampaign(
                    id = "demo_camp_1",
                    title = "WhatsApp Status Weekend Offers",
                    channel = DigitalMarketingCampaign.CHANNEL_WHATSAPP,
                    campaignDate = today,
                    spendKes = 800.0,
                    reachCount = 920L,
                    engagementCount = 148L,
                    inquiriesGenerated = 32L,
                    status = DigitalMarketingCampaign.STATUS_ACTIVE,
                    notes = "Sample demonstration campaign"
                ),
                DigitalMarketingCampaign(
                    id = "demo_camp_2",
                    title = "Nakuru CBD Instagram Reels Promo",
                    channel = DigitalMarketingCampaign.CHANNEL_INSTAGRAM,
                    campaignDate = twoDaysAgo,
                    spendKes = 1700.0,
                    reachCount = 2450L,
                    engagementCount = 310L,
                    inquiriesGenerated = 41L,
                    status = DigitalMarketingCampaign.STATUS_ACTIVE,
                    notes = "Sample demonstration campaign"
                ),
                DigitalMarketingCampaign(
                    id = "demo_camp_3",
                    title = "Google Maps Store Visibility Update",
                    channel = DigitalMarketingCampaign.CHANNEL_GOOGLE_MAPS,
                    campaignDate = nineDaysAgo,
                    spendKes = 0.0,
                    reachCount = 610L,
                    engagementCount = 89L,
                    inquiriesGenerated = 18L,
                    status = DigitalMarketingCampaign.STATUS_COMPLETED,
                    notes = "Sample demonstration campaign"
                )
            )
        }

    val sampleCommunications: List<OnlineCommunicationLog>
        get() {
            val today = todayIsoDate()
            val yesterday = isoDateDaysAgo(1)
            return listOf(
                OnlineCommunicationLog(
                    id = "demo_comm_1",
                    customerName = "Wanjiku Njoroge",
                    channel = OnlineCommunicationLog.CHANNEL_WHATSAPP,
                    category = OnlineCommunicationLog.CATEGORY_INQUIRY,
                    subject = "Stock & price check for cooking oil 5L",
                    receivedDate = today,
                    responseDate = today,
                    status = OnlineCommunicationLog.STATUS_CONVERTED,
                    responseMinutes = 5L,
                    notes = "Sample demonstration record"
                ),
                OnlineCommunicationLog(
                    id = "demo_comm_2",
                    customerName = "Kamau Kiprop",
                    channel = OnlineCommunicationLog.CHANNEL_FACEBOOK_DM,
                    category = OnlineCommunicationLog.CATEGORY_ORDER_UPDATE,
                    subject = "Wholesale bale discount inquiry",
                    receivedDate = yesterday,
                    responseDate = yesterday,
                    status = OnlineCommunicationLog.STATUS_RESPONDED,
                    responseMinutes = 14L,
                    notes = "Sample demonstration record"
                ),
                OnlineCommunicationLog(
                    id = "demo_comm_3",
                    customerName = "Amina Hassan",
                    channel = OnlineCommunicationLog.CHANNEL_INSTAGRAM_DM,
                    category = OnlineCommunicationLog.CATEGORY_COMPLAINT,
                    subject = "Milimani delivery delay follow-up",
                    receivedDate = today,
                    responseDate = "",
                    status = OnlineCommunicationLog.STATUS_PENDING,
                    responseMinutes = 25L,
                    notes = "Sample demonstration record"
                )
            )
        }

    val sampleWeeklyChart = listOf(
        DailyChartPoint("Mon", 24500.0, 9200.0),
        DailyChartPoint("Tue", 28100.0, 11400.0),
        DailyChartPoint("Wed", 21800.0, 8100.0),
        DailyChartPoint("Thu", 31400.0, 12900.0),
        DailyChartPoint("Fri", 36900.0, 14500.0),
        DailyChartPoint("Sat", 42300.0, 15200.0),
        DailyChartPoint("Sun", 19800.0, 6400.0)
    )
}

fun todayIsoDate(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(Date())
}

fun isoDateDaysAgo(daysAgo: Int): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val millis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo.toLong())
    return fmt.format(Date(millis))
}

fun isValidIsoDate(value: String): Boolean {
    val trimmed = value.trim()
    if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(trimmed)) return false
    return runCatching {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        fmt.parse(trimmed) != null
    }.getOrDefault(false)
}

fun resolveEffectiveIsoDate(explicitIsoDate: String, fallbackTimestamp: Timestamp?): String {
    if (explicitIsoDate.isNotBlank()) return explicitIsoDate.trim()
    val date = fallbackTimestamp?.toDate() ?: return todayIsoDate()
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
}

fun matchesDateFilter(
    explicitIsoDate: String,
    fallbackTimestamp: Timestamp?,
    filter: DateFilterOption
): Boolean {
    val maxDays = filter.maxDaysOld ?: return true
    val isoStr = resolveEffectiveIsoDate(explicitIsoDate, fallbackTimestamp)
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val itemDate = runCatching { fmt.parse(isoStr) }.getOrNull() ?: return true
    val todayDate = runCatching { fmt.parse(todayIsoDate()) }.getOrNull() ?: return true
    val diffDays = TimeUnit.MILLISECONDS.toDays(todayDate.time - itemDate.time)
    return diffDays in 0..maxDays.toLong()
}

fun resolveDateRangeWindow(
    filter: AnalyticsDateFilter,
    customStartIso: String = "",
    customEndIso: String = "",
    referenceTodayIso: String = todayIsoDate()
): DateRangeWindow {
    return when (filter) {
        AnalyticsDateFilter.TODAY -> DateRangeWindow(referenceTodayIso, referenceTodayIso)
        AnalyticsDateFilter.LAST_7_DAYS -> DateRangeWindow(
            offsetIsoDate(referenceTodayIso, -6),
            referenceTodayIso
        )
        AnalyticsDateFilter.LAST_30_DAYS -> DateRangeWindow(
            offsetIsoDate(referenceTodayIso, -29),
            referenceTodayIso
        )
        AnalyticsDateFilter.LAST_90_DAYS -> DateRangeWindow(
            offsetIsoDate(referenceTodayIso, -89),
            referenceTodayIso
        )
        AnalyticsDateFilter.CUSTOM -> {
            val start = customStartIso.trim().takeIf { isValidIsoDate(it) }
            val end = customEndIso.trim().takeIf { isValidIsoDate(it) }
            if (start != null && end != null && start > end) {
                DateRangeWindow(end, start)
            } else {
                DateRangeWindow(start, end)
            }
        }
        AnalyticsDateFilter.ALL_TIME -> DateRangeWindow(null, null)
    }
}

fun offsetIsoDate(baseIsoDate: String, deltaDays: Int): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val baseDate = runCatching { fmt.parse(baseIsoDate) }.getOrNull() ?: Date()
    val shiftedMillis = baseDate.time + TimeUnit.DAYS.toMillis(deltaDays.toLong())
    return fmt.format(Date(shiftedMillis))
}

fun computePreviousDateRange(window: DateRangeWindow): DateRangeWindow? {
    val startStr = window.startIso ?: return null
    val endStr = window.endIso ?: return null
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val startDate = runCatching { fmt.parse(startStr) }.getOrNull() ?: return null
    val endDate = runCatching { fmt.parse(endStr) }.getOrNull() ?: return null
    val spanDays = (TimeUnit.MILLISECONDS.toDays(endDate.time - startDate.time) + 1L).coerceAtLeast(1L)
    val prevEnd = offsetIsoDate(startStr, -1)
    val prevStart = offsetIsoDate(prevEnd, -(spanDays - 1L).toInt())
    return DateRangeWindow(prevStart, prevEnd)
}

fun isIsoDateWithinRange(
    explicitIsoDate: String,
    fallbackTimestamp: Timestamp?,
    window: DateRangeWindow
): Boolean {
    val effective = resolveEffectiveIsoDate(explicitIsoDate, fallbackTimestamp)
    if (!isValidIsoDate(effective)) return true
    val afterStart = window.startIso == null || effective >= window.startIso
    val beforeEnd = window.endIso == null || effective <= window.endIso
    return afterStart && beforeEnd
}

fun formatKes(amount: Double, includeDecimals: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (includeDecimals) 2 else 0
        maximumFractionDigits = if (includeDecimals) 2 else 0
    }
    return "KES ${formatter.format(amount)}"
}
