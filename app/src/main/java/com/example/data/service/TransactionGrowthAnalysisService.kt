package com.example.data.service

import android.util.Log
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.DemoSampleData
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.ResearchFramework
import com.example.data.model.formatKes
import com.example.data.repository.BusinessRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import com.example.data.analytics.Prompt4AnalyticsBundle
import com.example.data.analytics.Prompt4ForecastingEngine
import com.example.data.model.CustomerRecord
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.SupplierRecord
import org.json.JSONObject

import com.example.data.analytics.SmeAnalyticsEngine
import com.example.data.analytics.AnalyticsFilterState

data class GrowthOpportunity(
    val title: String,
    val variableFocus: String,
    val actionStep: String,
    val supportingIndicators: String = "",
    val expectedImpact: String,
    val priority: String
)

data class GrowthAnalysisReport(
    val isDemoData: Boolean,
    val periodLabel: String = "Recorded Workspace",
    val businessName: String,
    val analyzedTransactionCount: Int,
    val analyzedCampaignCount: Int,
    val analyzedCommunicationCount: Int,
    val totalSalesKes: Double,
    val totalExpensesKes: Double,
    val netCashflowKes: Double,
    val digitalPaymentSharePct: Int,
    val healthStatus: String,
    val executiveSummary: String,
    val positiveTrends: List<String> = emptyList(),
    val negativeTrends: List<String> = emptyList(),
    val weakAdoptionAreas: List<String> = emptyList(),
    val missingDataGaps: List<String> = emptyList(),
    val digitalMarketingAdvice: String,
    val digitalPaymentsAdvice: String,
    val recordKeepingAdvice: String,
    val onlineCommunicationAdvice: String,
    val cashflowInsight: String,
    val topCostDriver: String,
    val correlationCaveat: String,
    val growthOpportunities: List<GrowthOpportunity>,
    val nakuruLocalTip: String
)

sealed interface GrowthAnalysisResult {
    data class Success(val report: GrowthAnalysisReport) : GrowthAnalysisResult
    data class EmptyLedger(val message: String) : GrowthAnalysisResult
}

interface TransactionGrowthAnalyzer {
    suspend fun analyzeTransactionHistory(
        userId: String,
        allowDemoFallback: Boolean
    ): GrowthAnalysisResult
}

class FirebaseVertexTransactionGrowthService(
    private val repository: BusinessRepository
) : TransactionGrowthAnalyzer {

    private val growthReportSchema = Schema.obj(
        properties = mapOf(
            "healthStatus" to Schema.enumeration(
                values = listOf("STRONG", "STEADY", "WATCH", "CRITICAL"),
                description = "Overall SME financial & operational status grounded strictly in provided records."
            ),
            "executiveSummary" to Schema.string(
                description = "2-3 sentence summary of SME Performance (sales, customer reach, profitability, efficiency) in KES."
            ),
            "digitalMarketingAdvice" to Schema.string(
                description = "Recommendation for IV1 (Digital Marketing) grounded in campaign spend, reach, engagement, and leads."
            ),
            "digitalPaymentsAdvice" to Schema.string(
                description = "Recommendation for IV2 (Digital Payment Systems: M-Pesa, bank, card vs cash) based on adoption metrics."
            ),
            "recordKeepingAdvice" to Schema.string(
                description = "Recommendation for IV3 (Computerized Record Keeping & inventory tracking)."
            ),
            "onlineCommunicationAdvice" to Schema.string(
                description = "Recommendation for IV4 (Online Customer Communication response times, channels, and inquiry conversions)."
            ),
            "cashflowInsight" to Schema.string(
                description = "Cashflow & liquidity analysis across payment methods."
            ),
            "topCostDriver" to Schema.string(
                description = "Largest expense category or cost factor observed in recorded transactions."
            ),
            "correlationCaveat" to Schema.string(
                description = "Brief methodological statement clarifying that observed links between digital adoption and SME performance in this dataset are associative, not causal proof."
            ),
            "positiveTrends" to Schema.array(
                items = Schema.string(),
                description = "2-3 positive business or digital adoption trends observed strictly in the supplied records."
            ),
            "negativeTrends" to Schema.array(
                items = Schema.string(),
                description = "1-3 negative cost, revenue, response, or stock trends observed in the supplied records (or state if none observed due to sparse data)."
            ),
            "weakAdoptionAreas" to Schema.array(
                items = Schema.string(),
                description = "Specific weak areas across Digital Marketing, Digital Payments, Record Keeping, or Online Communication."
            ),
            "missingDataGaps" to Schema.array(
                items = Schema.string(),
                description = "Missing records, channels, or historical baselines that limit reliable analysis."
            ),
            "growthOpportunities" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Short action title"),
                        "variableFocus" to Schema.enumeration(
                            values = listOf(
                                "Digital Marketing",
                                "Digital Payments",
                                "Record Keeping",
                                "Online Communication",
                                "SME Performance"
                            ),
                            description = "Which research construct this recommendation addresses"
                        ),
                        "actionStep" to Schema.string(description = "Concrete step for the SME owner"),
                        "supportingIndicators" to Schema.string(description = "Exact recorded metrics/counts from the prompt that support this recommendation"),
                        "expectedImpact" to Schema.string(description = "Expected operational or KES improvement"),
                        "priority" to Schema.enumeration(
                            values = listOf("HIGH", "MEDIUM", "LOW"),
                            description = "Implementation urgency"
                        )
                    )
                ),
                description = "3-4 prioritized recommendations grounded strictly in recorded metrics."
            ),
            "nakuruLocalTip" to Schema.string(
                description = "Practical local tip contextualized for Nakuru Town SMEs."
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = growthReportSchema
            },
            systemInstruction = content {
                text(
                    "You are Project X AI Business Intelligence supporting the research study: " +
                        "'${ResearchFramework.RESEARCH_TITLE}'. Analyze records across the 4 independent variables " +
                        "(Digital Marketing, Digital Payment Systems, Computerized Record Keeping, Online Communication) " +
                        "and the dependent variable (SME Performance). Quote money strictly in Kenyan Shillings (KES). " +
                        "Never invent transactions, sales figures, customer numbers, or fabricated research findings. " +
                        "State missing data gaps explicitly when observations are absent or incomplete, cite exact supporting indicators " +
                        "for each recommendation, and distinguish observed associations from causal proof."
                )
            }
        )
    }

    override suspend fun analyzeTransactionHistory(
        userId: String,
        allowDemoFallback: Boolean
    ): GrowthAnalysisResult {
        val profile = repository.fetchBusinessProfileSnapshot(userId)
        val liveTransactions = repository.fetchTransactionHistorySnapshot(userId)
        val liveInventory = repository.fetchInventorySnapshot(userId)
        val liveCampaigns = repository.fetchCampaignsSnapshot(userId)
        val liveCommunications = repository.fetchCommunicationsSnapshot(userId)

        val liveCustomers = repository.fetchCustomersSnapshot(userId)
        val liveSuppliers = repository.fetchSuppliersSnapshot(userId)
        val livePurchases = repository.fetchPurchasesSnapshot(userId)

        val hasAnyLiveData = liveTransactions.isNotEmpty() ||
            liveInventory.isNotEmpty() ||
            liveCampaigns.isNotEmpty() ||
            liveCommunications.isNotEmpty() ||
            liveCustomers.isNotEmpty() ||
            liveSuppliers.isNotEmpty() ||
            livePurchases.isNotEmpty()

        if (!hasAnyLiveData && !allowDemoFallback) {
            return GrowthAnalysisResult.EmptyLedger(
                "Your live Cloud Firestore workspace has no recorded transactions, campaigns, customers, or communications yet. Record live business data across Digital Marketing, Digital Payments, Record Keeping, or Online Communication—or enable Demo Data Preview."
            )
        }

        val usingDemo = !hasAnyLiveData && allowDemoFallback
        val transactions = if (usingDemo) DemoSampleData.sampleTransactions else liveTransactions
        val inventory = if (usingDemo) DemoSampleData.sampleInventory else liveInventory
        val campaigns = if (usingDemo) DemoSampleData.sampleCampaigns else liveCampaigns
        val communications = if (usingDemo) DemoSampleData.sampleCommunications else liveCommunications
        val customers = if (usingDemo) DemoSampleData.sampleCustomers else liveCustomers
        val suppliers = if (usingDemo) DemoSampleData.sampleSuppliers else liveSuppliers
        val purchases = if (usingDemo) DemoSampleData.samplePurchases else livePurchases

        val analyticsSnapshot = SmeAnalyticsEngine.compute(
            isDemoMode = usingDemo,
            filterState = AnalyticsFilterState(),
            allTransactions = transactions,
            inventory = inventory,
            allCampaigns = campaigns,
            allCommunications = communications
        )
        val prompt4Bundle = Prompt4ForecastingEngine.compute(
            isDemoMode = usingDemo,
            allTransactions = transactions,
            inventory = inventory,
            customers = customers,
            horizonMonths = 6
        )

        val prompt = buildAnalysisPrompt(
            profile = profile,
            transactions = transactions,
            inventory = inventory,
            campaigns = campaigns,
            communications = communications,
            usingDemo = usingDemo,
            dataGaps = analyticsSnapshot.dataGapsAndLimitations,
            customers = customers,
            suppliers = suppliers,
            purchases = purchases,
            prompt4 = prompt4Bundle
        )

        val activeTransactions = transactions.filter { it.isActiveLedgerEntry() }
        val totalSales = activeTransactions.filter { it.type == BusinessTransaction.TYPE_SALE }.sumOf { it.amountKes }
        val totalExpenses = activeTransactions.filter { it.type == BusinessTransaction.TYPE_EXPENSE }.sumOf { it.amountKes }
        val digitalCount = activeTransactions.count { it.isDigitalPayment }
        val digitalSharePct = if (activeTransactions.isNotEmpty()) ((digitalCount * 100.0) / activeTransactions.size).toInt() else 0
        val businessName = profile?.businessName?.ifBlank { "My Nakuru SME" } ?: "My Nakuru SME"

        val response = withTimeout(ANALYSIS_TIMEOUT_MS) {
            structuredModel.generateContent(prompt)
        }
        val rawJson = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Firebase AI returned an empty analysis response.")

        return try {
            val report = parseGrowthAnalysisJson(
                jsonString = rawJson,
                isDemoData = usingDemo,
                businessName = businessName,
                analyzedTransactionCount = activeTransactions.size,
                analyzedCampaignCount = campaigns.size,
                analyzedCommunicationCount = communications.size,
                totalSalesKes = totalSales,
                totalExpensesKes = totalExpenses,
                digitalPaymentSharePct = digitalSharePct,
                periodLabel = if (usingDemo) "Demo Sample Dataset" else "Live Firestore Workspace"
            )
            GrowthAnalysisResult.Success(report)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse structured growth analysis JSON", e)
            throw IllegalStateException("Unable to parse AI growth report: ${e.localizedMessage}", e)
        }
    }

    private companion object {
        const val TAG = "GrowthAnalysisService"
        const val ANALYSIS_TIMEOUT_MS = 60_000L
    }
}

internal fun buildAnalysisPrompt(
    profile: BusinessProfile?,
    transactions: List<BusinessTransaction>,
    inventory: List<InventoryItem>,
    campaigns: List<DigitalMarketingCampaign> = emptyList(),
    communications: List<OnlineCommunicationLog> = emptyList(),
    usingDemo: Boolean,
    dataGaps: List<String> = emptyList(),
    customers: List<CustomerRecord> = emptyList(),
    suppliers: List<SupplierRecord> = emptyList(),
    purchases: List<PurchaseOrderRecord> = emptyList(),
    prompt4: Prompt4AnalyticsBundle? = null
): String {
    val activeTx = transactions.filter { it.isActiveLedgerEntry() }
    val sales = activeTx.filter { it.type == BusinessTransaction.TYPE_SALE }
    val expenses = activeTx.filter { it.type == BusinessTransaction.TYPE_EXPENSE }
    val totalSales = sales.sumOf { it.amountKes }
    val totalExpenses = expenses.sumOf { it.amountKes }
    val net = totalSales - totalExpenses

    val digitalTx = activeTx.filter { it.isDigitalPayment }
    val digitalSharePct = if (activeTx.isNotEmpty()) ((digitalTx.size * 100.0) / activeTx.size).toInt() else 0
    val mpesaTotal = activeTx.filter { it.paymentMethod == BusinessTransaction.METHOD_MPESA }.sumOf { it.amountKes }
    val bankTotal = activeTx.filter { it.paymentMethod == BusinessTransaction.METHOD_BANK }.sumOf { it.amountKes }
    val cardTotal = activeTx.filter { it.paymentMethod == BusinessTransaction.METHOD_CARD }.sumOf { it.amountKes }
    val otherDigitalTotal = activeTx.filter { it.paymentMethod == BusinessTransaction.METHOD_OTHER_DIGITAL }.sumOf { it.amountKes }
    val cashTotal = activeTx.filter { it.paymentMethod == BusinessTransaction.METHOD_CASH }.sumOf { it.amountKes }

    val expenseByCategory = expenses.groupBy { it.category.ifBlank { "Uncategorized" } }
        .mapValues { (_, list) -> list.sumOf { it.amountKes } }
        .entries
        .sortedByDescending { it.value }

    val lowStockItems = inventory.filter { it.isLowStock }

    val totalMarketingSpend = campaigns.sumOf { it.spendKes }
    val totalReach = campaigns.sumOf { it.reachCount }
    val totalEngagement = campaigns.sumOf { it.engagementCount }
    val totalLeads = campaigns.sumOf { it.inquiriesGenerated }

    val convertedInquiries = communications.count { it.status == OnlineCommunicationLog.STATUS_CONVERTED }
    val pendingInquiries = communications.count { it.status == OnlineCommunicationLog.STATUS_PENDING }
    val avgResponseMins = if (communications.isNotEmpty()) communications.map { it.responseMinutes }.average() else 0.0

    return buildString {
        appendLine("Analyze this Nakuru SME's records using the research framework and return structured JSON.")
        appendLine("Data Mode: ${if (usingDemo) "DEMO DATA — SAMPLE ONLY (do not treat as real field study findings)" else "LIVE CLOUD FIRESTORE WORKSPACE"}")
        appendLine("Business Name: ${profile?.businessName?.ifBlank { "My Nakuru SME" } ?: "My Nakuru SME"}")
        appendLine("Sector: ${profile?.businessType?.ifBlank { "Retail Shop" } ?: "Retail Shop"} | Size: ${profile?.businessSize?.ifBlank { "Small" } ?: "Small"}")
        appendLine("Location: ${profile?.locationNakuru?.ifBlank { "Nakuru Town, Kenya" } ?: "Nakuru Town, Kenya"}")
        appendLine()
        appendLine("=== IV1: DIGITAL MARKETING ===")
        appendLine("Campaigns: ${campaigns.size} | Spend: ${formatKes(totalMarketingSpend)} | Total Reach: $totalReach | Engagements: $totalEngagement | Inquiries: $totalLeads")
        campaigns.take(15).forEach { c ->
            appendLine(" - [${c.status}] ${c.title} (${c.channel}, ${c.effectiveDate()}) | Spend: ${formatKes(c.spendKes)} | Reach: ${c.reachCount} | Engagement: ${c.engagementCount} | Leads: ${c.inquiriesGenerated}")
        }
        appendLine()
        appendLine("=== IV2: DIGITAL PAYMENT SYSTEMS ===")
        appendLine("Digital Adoption Rate (txns): $digitalSharePct% (${digitalTx.size}/${transactions.size})")
        appendLine("Volume -> M-Pesa: ${formatKes(mpesaTotal)}, Bank: ${formatKes(bankTotal)}, Card: ${formatKes(cardTotal)}, Other Digital: ${formatKes(otherDigitalTotal)}, Cash: ${formatKes(cashTotal)}")
        appendLine()
        appendLine("=== IV3: COMPUTERIZED RECORD KEEPING & DV PERFORMANCE ===")
        val otherTx = activeTx.filter { it.type == BusinessTransaction.TYPE_OTHER }
        appendLine("Sales Entries: ${sales.size} (${formatKes(totalSales)}) | Expense Entries: ${expenses.size} (${formatKes(totalExpenses)}) | Other Records: ${otherTx.size} | Net Profit: ${formatKes(net)}")
        if (expenseByCategory.isNotEmpty()) {
            appendLine("Expenses by Category:")
            expenseByCategory.forEach { (cat, amt) -> appendLine(" - $cat: ${formatKes(amt)}") }
        }
        appendLine("Tracked Inventory SKUs: ${inventory.size}, Low-Stock Alerts: ${lowStockItems.size}")
        lowStockItems.take(8).forEach { item ->
            appendLine(" - LOW STOCK: ${item.name} (Qty: ${item.quantity}, Reorder: ${item.reorderLevel}, Price: ${formatKes(item.unitPriceKes)})")
        }
        appendLine("Recent Ledger Sample (up to 25):")
        activeTx.take(25).forEachIndexed { i, tx ->
            val refText = if (tx.transactionRef.isNotBlank()) " | Ref: ${tx.transactionRef}" else ""
            appendLine(" ${i + 1}. [${tx.type}/${tx.paymentStatus}] ${tx.title} (${tx.effectiveDate()}) | ${tx.category} | ${formatKes(tx.amountKes)} | ${tx.paymentMethod}$refText")
        }
        appendLine()
        appendLine("=== IV4: ONLINE COMMUNICATION & CUSTOMERS / SUPPLIERS ===")
        appendLine("Logged Inquiries: ${communications.size} | Converted: $convertedInquiries | Pending: $pendingInquiries | Avg Response Time: ${avgResponseMins.toInt()} mins")
        appendLine("Registered Customers: ${customers.size} (Repeat 2+: ${customers.count { it.isRepeatCustomer }}) | Suppliers: ${suppliers.size} | Pending Payables: ${formatKes(purchases.filter { it.paymentStatus != PurchaseOrderRecord.STATUS_PAID }.sumOf { it.totalCostKes })}")
        if (prompt4 != null) {
            appendLine("Monthly Trajectory (${prompt4.monthlyRows.size} mo): Slope=${String.format(java.util.Locale.US, "%+.0f", prompt4.projection.slopeKesPerMonth)} KES/mo | Gross Profit=${prompt4.totalGrossProfitKes?.let { formatKes(it) } ?: "Cost data missing"}")
        }
        communications.take(15).forEach { log ->
            appendLine(" - [${log.category}/${log.status}] ${log.customerName} via ${log.channel} on ${log.effectiveReceivedDate()} (${log.responseMinutes}m): ${log.subject}")
        }
        if (dataGaps.isNotEmpty()) {
            appendLine()
            appendLine("=== IDENTIFIED DATA GAPS (DO NOT INVENT MISSING METRICS) ===")
            dataGaps.forEach { gap -> appendLine(" - $gap") }
        }
    }
}

internal fun parseGrowthAnalysisJson(
    jsonString: String,
    isDemoData: Boolean,
    businessName: String,
    analyzedTransactionCount: Int,
    analyzedCampaignCount: Int = 0,
    analyzedCommunicationCount: Int = 0,
    totalSalesKes: Double,
    totalExpensesKes: Double,
    digitalPaymentSharePct: Int = 0,
    periodLabel: String = "Recorded Workspace"
): GrowthAnalysisReport {
    val json = JSONObject(jsonString)

    fun readStringList(key: String): List<String> {
        val arr = json.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            arr.optString(idx).takeIf { it.isNotBlank() }
        }
    }

    val opportunitiesArray = json.optJSONArray("growthOpportunities")
    val opportunities = if (opportunitiesArray != null) {
        (0 until opportunitiesArray.length()).map { idx ->
            val obj = opportunitiesArray.getJSONObject(idx)
            GrowthOpportunity(
                title = obj.getString("title"),
                variableFocus = obj.optString("variableFocus", "SME Performance"),
                actionStep = obj.getString("actionStep"),
                supportingIndicators = obj.optString("supportingIndicators", ""),
                expectedImpact = obj.getString("expectedImpact"),
                priority = obj.optString("priority", "MEDIUM")
            )
        }
    } else {
        emptyList()
    }

    return GrowthAnalysisReport(
        isDemoData = isDemoData,
        periodLabel = periodLabel,
        businessName = businessName,
        analyzedTransactionCount = analyzedTransactionCount,
        analyzedCampaignCount = analyzedCampaignCount,
        analyzedCommunicationCount = analyzedCommunicationCount,
        totalSalesKes = totalSalesKes,
        totalExpensesKes = totalExpensesKes,
        netCashflowKes = totalSalesKes - totalExpensesKes,
        digitalPaymentSharePct = digitalPaymentSharePct,
        healthStatus = json.optString("healthStatus", "STEADY"),
        executiveSummary = json.getString("executiveSummary"),
        positiveTrends = readStringList("positiveTrends"),
        negativeTrends = readStringList("negativeTrends"),
        weakAdoptionAreas = readStringList("weakAdoptionAreas"),
        missingDataGaps = readStringList("missingDataGaps"),
        digitalMarketingAdvice = json.optString("digitalMarketingAdvice", "Review campaign conversion efficiency."),
        digitalPaymentsAdvice = json.optString("digitalPaymentsAdvice", "Monitor M-Pesa and bank transfer share."),
        recordKeepingAdvice = json.optString("recordKeepingAdvice", "Keep daily digital entries up to date."),
        onlineCommunicationAdvice = json.optString("onlineCommunicationAdvice", "Reduce response time on customer chats."),
        cashflowInsight = json.getString("cashflowInsight"),
        topCostDriver = json.getString("topCostDriver"),
        correlationCaveat = json.optString(
            "correlationCaveat",
            ResearchFramework.METHODOLOGY_NOTE
        ),
        growthOpportunities = opportunities,
        nakuruLocalTip = json.getString("nakuruLocalTip")
    )
}
