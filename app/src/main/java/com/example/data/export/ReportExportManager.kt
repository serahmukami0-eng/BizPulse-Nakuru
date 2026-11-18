package com.example.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.analytics.Prompt4AnalyticsBundle
import com.example.data.analytics.SmeFullAnalyticsReport
import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.ResearchFramework
import com.example.data.model.formatKes
import com.example.data.model.isoDateDaysAgo
import com.example.data.model.todayIsoDate
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

enum class BusinessReportType(val title: String, val description: String) {
    DAILY_SALES("Daily Sales Report", "Sales & POS receipts for today"),
    WEEKLY_SALES("Weekly Sales Report", "Sales over the last 7 days"),
    MONTHLY_SALES("Monthly Sales Report", "Monthly sales & MoM growth"),
    ANNUAL_SALES("Annual Sales Report", "Last 365 days sales performance"),
    EXPENSE_REPORT("Expense Report", "Operating expenses & restock spend"),
    PROFIT_AND_LOSS("Profit & Loss Statement", "Revenue, COGS, expenses, gross & net profit"),
    INVENTORY_REPORT("Inventory & Stock Report", "SKU quantities, values & low-stock alerts"),
    CUSTOMER_REPORT("Customer Report", "Directory, spend & repeat customer tracking"),
    DIGITAL_MARKETING_REPORT("Digital Marketing Report", "IV1 campaigns, spend, reach & cost/enquiry"),
    DIGITAL_PAYMENT_REPORT("Digital Payment Report", "IV2 M-Pesa, bank, card & cash share"),
    OVERALL_PERFORMANCE("Overall Business Performance", "Research-aligned SME summary (IV1–IV4 → DV)"),
    RESEARCH_DATASET_SPSS("Research Dataset (SPSS / Excel)", "Anonymized Chapter 4 quantitative CSV across IV1–IV4 & DV")
}

data class GeneratedBusinessReport(
    val type: BusinessReportType,
    val title: String,
    val businessName: String,
    val location: String,
    val periodLabel: String,
    val generatedDateIso: String,
    val isDemoData: Boolean,
    val summaryPairs: List<Pair<String, String>>,
    val columns: List<String>,
    val rows: List<List<String>>,
    val footerNote: String
)

object ReportExportManager {

    fun generateReport(
        type: BusinessReportType,
        businessName: String,
        location: String,
        isDemoData: Boolean,
        transactions: List<BusinessTransaction>,
        inventory: List<InventoryItem>,
        customers: List<CustomerRecord>,
        purchases: List<PurchaseOrderRecord>,
        campaigns: List<DigitalMarketingCampaign>,
        communications: List<OnlineCommunicationLog>,
        analytics: SmeFullAnalyticsReport,
        prompt4: Prompt4AnalyticsBundle,
        periodLabel: String
    ): GeneratedBusinessReport {
        val today = todayIsoDate()
        val activeTx = transactions.filter { it.isActiveLedgerEntry() }
        val activeSales = activeTx.filter { it.type == BusinessTransaction.TYPE_SALE }

        fun salesReportForCutoff(cutoffIso: String, titleOverride: String, winLabel: String): GeneratedBusinessReport {
            val matching = activeSales.filter { it.effectiveDate() >= cutoffIso }
            val total = matching.sumOf { it.amountKes }
            val digitalTotal = matching.filter { it.isDigitalPayment }.sumOf { it.amountKes }
            return GeneratedBusinessReport(
                type = type,
                title = titleOverride,
                businessName = businessName,
                location = location,
                periodLabel = winLabel,
                generatedDateIso = today,
                isDemoData = isDemoData,
                summaryPairs = listOf(
                    "Sales Count" to "${matching.size}",
                    "Total Sales (KES)" to formatKes(total),
                    "Digital Sales (KES)" to formatKes(digitalTotal),
                    "Cash Sales (KES)" to formatKes(total - digitalTotal)
                ),
                columns = listOf("Date", "Receipt", "Description", "Customer", "Method", "Status", "Amount (KES)"),
                rows = matching.map { tx ->
                    listOf(
                        tx.effectiveDate(),
                        tx.receiptNumber.ifBlank { "-" },
                        tx.title,
                        tx.customerName.ifBlank { "Walk-in" },
                        tx.paymentMethod,
                        tx.paymentStatus,
                        String.format(Locale.US, "%.2f", tx.amountKes)
                    )
                },
                footerNote = buildFooterNote(isDemoData)
            )
        }

        return when (type) {
            BusinessReportType.DAILY_SALES ->
                salesReportForCutoff(today, "Daily Sales Report ($today)", "Today ($today)")

            BusinessReportType.WEEKLY_SALES ->
                salesReportForCutoff(isoDateDaysAgo(6), "Weekly Sales Report", "Last 7 Days")

            BusinessReportType.MONTHLY_SALES -> {
                GeneratedBusinessReport(
                    type = type,
                    title = "Monthly Sales Report",
                    businessName = businessName,
                    location = location,
                    periodLabel = "All Recorded Months (${prompt4.monthlyRows.size} mo)",
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Recorded Months" to "${prompt4.monthlyRows.size}",
                        "Total Sales (KES)" to formatKes(prompt4.monthlyRows.sumOf { it.salesKes }),
                        "Latest MoM Growth" to (prompt4.latestMoMSalesGrowthPct?.let {
                            String.format(Locale.US, "%+.1f%%", it)
                        } ?: "N/A")
                    ),
                    columns = listOf("Month", "Sales (KES)", "Expenses (KES)", "Net Profit (KES)", "MoM Growth"),
                    rows = prompt4.monthlyRows.map { row ->
                        listOf(
                            row.monthLabel,
                            String.format(Locale.US, "%.2f", row.salesKes),
                            String.format(Locale.US, "%.2f", row.expensesKes),
                            String.format(Locale.US, "%.2f", row.netProfitKes),
                            row.momSalesGrowthPct?.let { String.format(Locale.US, "%+.1f%%", it) } ?: "N/A"
                        )
                    },
                    footerNote = buildFooterNote(isDemoData)
                )
            }

            BusinessReportType.ANNUAL_SALES ->
                salesReportForCutoff(isoDateDaysAgo(364), "Annual Sales Report", "Last 365 Days")

            BusinessReportType.EXPENSE_REPORT -> {
                val expenses = activeTx.filter { it.type == BusinessTransaction.TYPE_EXPENSE }
                val totalExp = expenses.sumOf { it.amountKes }
                val pendingPayables = purchases.filter { it.paymentStatus != PurchaseOrderRecord.STATUS_PAID }.sumOf { it.totalCostKes }
                GeneratedBusinessReport(
                    type = type,
                    title = "Operating Expense & Payables Report",
                    businessName = businessName,
                    location = location,
                    periodLabel = periodLabel,
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Expense Entries" to "${expenses.size}",
                        "Total Expenses (KES)" to formatKes(totalExp),
                        "Pending Supplier Payables" to formatKes(pendingPayables)
                    ),
                    columns = listOf("Date", "Category", "Description", "Method", "Status", "Amount (KES)"),
                    rows = expenses.map { tx ->
                        listOf(
                            tx.effectiveDate(),
                            tx.category.ifBlank { "General" },
                            tx.title,
                            tx.paymentMethod,
                            tx.paymentStatus,
                            String.format(Locale.US, "%.2f", tx.amountKes)
                        )
                    },
                    footerNote = buildFooterNote(isDemoData)
                )
            }

            BusinessReportType.PROFIT_AND_LOSS -> {
                val totalSales = activeSales.sumOf { it.amountKes }
                val totalExp = activeTx.filter { it.type == BusinessTransaction.TYPE_EXPENSE }.sumOf { it.amountKes }
                val cogs = prompt4.totalCogsKes
                val grossProfit = prompt4.totalGrossProfitKes
                val netProfit = totalSales - totalExp
                GeneratedBusinessReport(
                    type = type,
                    title = "Profit & Loss Statement (KES)",
                    businessName = businessName,
                    location = location,
                    periodLabel = periodLabel,
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Total Revenue" to formatKes(totalSales),
                        "Tracked COGS" to if (prompt4.hasCostDataForGrossProfit) formatKes(cogs) else "Cost data missing",
                        "Gross Profit" to (grossProfit?.let { formatKes(it) } ?: "N/A (Add product cost)"),
                        "Operating Expenses" to formatKes(totalExp),
                        "Estimated Net Profit" to formatKes(netProfit)
                    ),
                    columns = listOf("Month", "Sales (KES)", "COGS (KES)", "Gross Profit", "Expenses (KES)", "Net Profit"),
                    rows = prompt4.monthlyRows.map { r ->
                        listOf(
                            r.monthLabel,
                            String.format(Locale.US, "%.2f", r.salesKes),
                            if (prompt4.hasCostDataForGrossProfit) String.format(Locale.US, "%.2f", r.cogsKes) else "N/A",
                            r.grossProfitKes?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A",
                            String.format(Locale.US, "%.2f", r.expensesKes),
                            String.format(Locale.US, "%.2f", r.netProfitKes)
                        )
                    },
                    footerNote = buildFooterNote(isDemoData)
                )
            }

            BusinessReportType.INVENTORY_REPORT -> {
                GeneratedBusinessReport(
                    type = type,
                    title = "Inventory Valuation & Stock Report",
                    businessName = businessName,
                    location = location,
                    periodLabel = "Current Stock Snapshot ($today)",
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Total SKUs" to "${inventory.size}",
                        "Low Stock Alerts" to "${inventory.count { it.isLowStock }}",
                        "Stock Retail Value" to formatKes(prompt4.totalInventoryRetailValueKes),
                        "Stock Cost Value" to formatKes(prompt4.totalInventoryCostValueKes)
                    ),
                    columns = listOf("Item Name", "SKU", "Category", "Qty", "Reorder", "Cost KES", "Price KES", "Status"),
                    rows = inventory.map { item ->
                        listOf(
                            item.name,
                            item.sku.ifBlank { "-" },
                            item.category,
                            item.quantity.toString(),
                            item.reorderLevel.toString(),
                            String.format(Locale.US, "%.2f", item.unitCostKes),
                            String.format(Locale.US, "%.2f", item.unitPriceKes),
                            if (item.isLowStock) "LOW STOCK" else "OK"
                        )
                    },
                    footerNote = buildFooterNote(isDemoData)
                )
            }

            BusinessReportType.CUSTOMER_REPORT -> {
                GeneratedBusinessReport(
                    type = type,
                    title = "Customer Directory & Growth Report",
                    businessName = businessName,
                    location = location,
                    periodLabel = "Registered Directory ($today)",
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Total Customers" to "${customers.size}",
                        "Repeat Customers (2+)" to "${prompt4.repeatCustomerCount}",
                        "Total Tracked Spend" to formatKes(customers.sumOf { it.totalSpentKes })
                    ),
                    columns = listOf("Customer Name", "Phone", "Location", "Purchases", "Total Spent (KES)", "Last Purchase"),
                    rows = customers.map { c ->
                        listOf(
                            c.name,
                            c.phone.ifBlank { "-" },
                            c.location.ifBlank { "-" },
                            c.purchaseCount.toString(),
                            String.format(Locale.US, "%.2f", c.totalSpentKes),
                            c.lastPurchaseDate.ifBlank { "-" }
                        )
                    },
                    footerNote = buildFooterNote(isDemoData)
                )
            }

            BusinessReportType.DIGITAL_MARKETING_REPORT -> {
                GeneratedBusinessReport(
                    type = type,
                    title = "Digital Marketing Performance Report (IV1)",
                    businessName = businessName,
                    location = location,
                    periodLabel = periodLabel,
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Campaigns" to "${campaigns.size}",
                        "Total Spend" to formatKes(campaigns.sumOf { it.spendKes }),
                        "Total Reach" to "${campaigns.sumOf { it.reachCount }}",
                        "Total Enquiries" to "${campaigns.sumOf { it.inquiriesGenerated }}"
                    ),
                    columns = listOf("Date", "Campaign", "Channel", "Spend (KES)", "Reach", "Engagements", "Enquiries"),
                    rows = campaigns.map { c ->
                        listOf(
                            c.effectiveDate(),
                            c.title,
                            c.channel,
                            String.format(Locale.US, "%.2f", c.spendKes),
                            c.reachCount.toString(),
                            c.engagementCount.toString(),
                            c.inquiriesGenerated.toString()
                        )
                    },
                    footerNote = buildFooterNote(isDemoData)
                )
            }

            BusinessReportType.DIGITAL_PAYMENT_REPORT -> {
                val payments = analytics.payments
                GeneratedBusinessReport(
                    type = type,
                    title = "Digital Payment Adoption Report (IV2)",
                    businessName = businessName,
                    location = location,
                    periodLabel = analytics.activeWindow.label,
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Digital Payments Total" to formatKes(payments.totalDigitalPaymentsKes),
                        "Digital Tx Share" to "${payments.digitalAdoptionPct ?: 0}%",
                        "Digital % of Sales" to (payments.digitalAsPercentOfSales?.let {
                            String.format(Locale.US, "%.1f%%", it)
                        } ?: "N/A")
                    ),
                    columns = listOf("Payment Method", "Tx Count", "Total Volume (KES)", "Sales Volume (KES)", "Tx Share %"),
                    rows = payments.methodBreakdown.map { m ->
                        listOf(
                            m.displayLabel,
                            m.transactionCount.toString(),
                            String.format(Locale.US, "%.2f", m.totalAmountKes),
                            String.format(Locale.US, "%.2f", m.salesAmountKes),
                            String.format(Locale.US, "%.1f%%", m.shareOfTransactionsPct)
                        )
                    },
                    footerNote = buildFooterNote(isDemoData)
                )
            }

            BusinessReportType.OVERALL_PERFORMANCE -> {
                GeneratedBusinessReport(
                    type = type,
                    title = "Overall SME Performance & Research Summary",
                    businessName = businessName,
                    location = location,
                    periodLabel = analytics.activeWindow.label,
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Sales Revenue" to formatKes(analytics.recordKeeping.recordedSalesKes),
                        "Operating Expenses" to formatKes(analytics.recordKeeping.recordedExpensesKes),
                        "Estimated Net Profit" to formatKes(analytics.recordKeeping.estimatedNetProfitKes),
                        "Customers / Repeat" to "${customers.size} (${prompt4.repeatCustomerCount} repeat)",
                        "Inventory SKUs / Low" to "${inventory.size} (${inventory.count { it.isLowStock }} low)",
                        "Digital Payment Share" to "${analytics.payments.digitalAdoptionPct ?: 0}%",
                        "Enquiry Response Rate" to (analytics.communication.responseRatePct?.let { "${it.toInt()}%" } ?: "N/A")
                    ),
                    columns = listOf("Research Variable", "Dimension", "Key Observation", "Status"),
                    rows = listOf(
                        listOf(
                            "IV1: Digital Marketing",
                            "Campaigns & Enquiries",
                            "${analytics.marketing.campaignCount} campaigns, ${analytics.marketing.totalInquiries} enquiries, spend ${formatKes(analytics.marketing.totalSpendKes)}",
                            if (analytics.marketing.campaignCount > 0) "Active" else "No records"
                        ),
                        listOf(
                            "IV2: Digital Payments",
                            "M-Pesa / Bank / Card",
                            "${analytics.payments.digitalAdoptionPct ?: 0}% adoption, ${formatKes(analytics.payments.totalDigitalPaymentsKes)}",
                            if (analytics.payments.digitalTransactionCount > 0) "Active" else "No records"
                        ),
                        listOf(
                            "IV3: Computerized Records",
                            "Sales, Expenses & Stock",
                            "${analytics.recordKeeping.totalTransactionsCount} entries, ${analytics.recordKeeping.recordCompletenessPct ?: 0}% complete",
                            if (analytics.recordKeeping.totalTransactionsCount > 0) "Active" else "No records"
                        ),
                        listOf(
                            "IV4: Online Comms",
                            "WhatsApp / Social DMs",
                            "${analytics.communication.totalEnquiries} logs, avg ${analytics.communication.avgResponseMinutes ?: 0}m reply",
                            if (analytics.communication.totalEnquiries > 0) "Active" else "No records"
                        ),
                        listOf(
                            "DV: SME Performance",
                            "Profit & Growth",
                            "Net ${formatKes(analytics.recordKeeping.estimatedNetProfitKes)}, ${customers.size} customers, ${prompt4.monthlyRows.size} monthly periods",
                            if (prompt4.projection.hasSufficientHistory) "Trend ready" else "Limited history"
                        )
                    ),
                    footerNote = buildFooterNote(isDemoData)
                )
            }

            BusinessReportType.RESEARCH_DATASET_SPSS -> {
                val smeCode = "SME-${(kotlin.math.abs(businessName.hashCode()) % 9000) + 1000}"
                val dataMode = if (isDemoData) "DEMO_SAMPLE" else "LIVE_FIRESTORE"
                val columns = listOf(
                    "Record_Scope",
                    "Period_Label",
                    "SME_Code",
                    "Nakuru_Location",
                    "Data_Mode",
                    "IV1_Campaign_Count",
                    "IV1_Marketing_Spend_KES",
                    "IV1_Reach",
                    "IV1_Engagement",
                    "IV1_Enquiries_Generated",
                    "IV1_Cost_Per_Enquiry_KES",
                    "IV2_Digital_Tx_Count",
                    "IV2_Total_Tx_Count",
                    "IV2_Digital_Adoption_Pct",
                    "IV2_Mpesa_Sales_KES",
                    "IV2_Total_Digital_KES",
                    "IV2_Digital_Sales_Share_Pct",
                    "IV3_Active_Ledger_Count",
                    "IV3_Record_Completeness_Pct",
                    "IV3_Inventory_SKUs",
                    "IV3_Low_Stock_Count",
                    "IV4_Enquiries_Logged",
                    "IV4_Responded_Or_Converted",
                    "IV4_Response_Rate_Pct",
                    "IV4_Avg_Response_Minutes",
                    "DV_Sales_Revenue_KES",
                    "DV_Operating_Expenses_KES",
                    "DV_Estimated_Net_Profit_KES",
                    "DV_Gross_Profit_KES",
                    "DV_MoM_Sales_Growth_Pct",
                    "DV_Customer_Count",
                    "DV_Repeat_Customer_Count",
                    "Pearson_r_DigitalPay_vs_Sales",
                    "Pearson_r_Mktg_vs_Sales",
                    "Pearson_r_Comms_vs_Sales"
                )
                val rDigital = analytics.researchComparisons.find { it.variableCode == "IV2 → DV" }?.correlationR
                val rMktg = analytics.researchComparisons.find { it.variableCode == "IV1 → DV" }?.correlationR
                val rComm = analytics.researchComparisons.find { it.variableCode == "IV4 → DV" }?.correlationR

                val mpesaSalesTotal = activeSales
                    .filter { it.paymentMethod == BusinessTransaction.METHOD_MPESA }
                    .sumOf { it.amountKes }

                val respondedOrConvertedComms =
                    analytics.communication.respondedEnquiries + analytics.communication.convertedEnquiries

                val overallRow = listOf(
                    "OVERALL_WINDOW",
                    analytics.activeWindow.label,
                    smeCode,
                    location,
                    dataMode,
                    analytics.marketing.campaignCount.toString(),
                    String.format(Locale.US, "%.2f", analytics.marketing.totalSpendKes),
                    analytics.marketing.totalReach.toString(),
                    analytics.marketing.totalEngagement.toString(),
                    analytics.marketing.totalInquiries.toString(),
                    analytics.marketing.costPerInquiryKes?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                    analytics.payments.digitalTransactionCount.toString(),
                    analytics.payments.totalTransactionCount.toString(),
                    analytics.payments.digitalAdoptionPct?.toString() ?: "",
                    String.format(Locale.US, "%.2f", mpesaSalesTotal),
                    String.format(Locale.US, "%.2f", analytics.payments.totalDigitalPaymentsKes),
                    analytics.payments.digitalAsPercentOfSales?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                    analytics.recordKeeping.totalTransactionsCount.toString(),
                    analytics.recordKeeping.recordCompletenessPct?.toString() ?: "",
                    inventory.size.toString(),
                    inventory.count { it.isLowStock }.toString(),
                    analytics.communication.totalEnquiries.toString(),
                    respondedOrConvertedComms.toString(),
                    analytics.communication.responseRatePct?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                    analytics.communication.avgResponseMinutes?.toString() ?: "",
                    String.format(Locale.US, "%.2f", analytics.recordKeeping.recordedSalesKes),
                    String.format(Locale.US, "%.2f", analytics.recordKeeping.recordedExpensesKes),
                    String.format(Locale.US, "%.2f", analytics.recordKeeping.estimatedNetProfitKes),
                    prompt4.totalGrossProfitKes?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                    prompt4.latestMoMSalesGrowthPct?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                    customers.size.toString(),
                    prompt4.repeatCustomerCount.toString(),
                    rDigital?.let { String.format(Locale.US, "%.4f", it) } ?: "",
                    rMktg?.let { String.format(Locale.US, "%.4f", it) } ?: "",
                    rComm?.let { String.format(Locale.US, "%.4f", it) } ?: ""
                )

                val monthRows = prompt4.monthlyRows.map { m ->
                    val mKey = m.monthKey
                    val monthTx = activeTx.filter { it.effectiveDate().startsWith(mKey) }
                    val monthSalesTx = monthTx.filter { it.type == BusinessTransaction.TYPE_SALE }
                    val monthDigitalTx = monthTx.filter { it.isDigitalPayment }
                    val monthAdoptionPct = if (monthTx.isNotEmpty()) ((monthDigitalTx.size * 100.0) / monthTx.size).toInt() else null
                    val monthMpesaSales = monthSalesTx.filter { it.paymentMethod == BusinessTransaction.METHOD_MPESA }.sumOf { it.amountKes }
                    val monthDigitalKes = monthDigitalTx.sumOf { it.amountKes }
                    val monthDigitalShare = if (m.salesKes > 0.0) ((monthSalesTx.filter { it.isDigitalPayment }.sumOf { it.amountKes } / m.salesKes) * 100.0) else null
                    val monthCamps = campaigns.filter { it.effectiveDate().startsWith(mKey) }
                    val mCampSpend = monthCamps.sumOf { it.spendKes }
                    val mCampInq = monthCamps.sumOf { it.inquiriesGenerated }
                    val mCpe = if (mCampInq > 0L) mCampSpend / mCampInq else null
                    val monthComms = communications.filter { it.effectiveReceivedDate().startsWith(mKey) }
                    val mRespCount = monthComms.count {
                        it.status == OnlineCommunicationLog.STATUS_RESPONDED || it.status == OnlineCommunicationLog.STATUS_CONVERTED
                    }
                    val mRespRate = if (monthComms.isNotEmpty()) (mRespCount * 100.0) / monthComms.size else null
                    val mAvgResp = if (monthComms.isNotEmpty()) monthComms.map { it.responseMinutes }.average().toLong() else null

                    listOf(
                        "MONTHLY_PANEL",
                        m.monthKey,
                        smeCode,
                        location,
                        dataMode,
                        monthCamps.size.toString(),
                        String.format(Locale.US, "%.2f", mCampSpend),
                        monthCamps.sumOf { it.reachCount }.toString(),
                        monthCamps.sumOf { it.engagementCount }.toString(),
                        mCampInq.toString(),
                        mCpe?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        monthDigitalTx.size.toString(),
                        monthTx.size.toString(),
                        monthAdoptionPct?.toString() ?: "",
                        String.format(Locale.US, "%.2f", monthMpesaSales),
                        String.format(Locale.US, "%.2f", monthDigitalKes),
                        monthDigitalShare?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        monthTx.size.toString(),
                        "",
                        inventory.size.toString(),
                        inventory.count { it.isLowStock }.toString(),
                        monthComms.size.toString(),
                        mRespCount.toString(),
                        mRespRate?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        mAvgResp?.toString() ?: "",
                        String.format(Locale.US, "%.2f", m.salesKes),
                        String.format(Locale.US, "%.2f", m.expensesKes),
                        String.format(Locale.US, "%.2f", m.netProfitKes),
                        m.grossProfitKes?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        m.momSalesGrowthPct?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        customers.size.toString(),
                        prompt4.repeatCustomerCount.toString(),
                        "",
                        "",
                        ""
                    )
                }

                GeneratedBusinessReport(
                    type = type,
                    title = "Academic Research Dataset (SPSS / Excel CSV)",
                    businessName = smeCode,
                    location = location,
                    periodLabel = "${analytics.activeWindow.label} + ${prompt4.monthlyRows.size} Month(s)",
                    generatedDateIso = today,
                    isDemoData = isDemoData,
                    summaryPairs = listOf(
                        "Anonymized SME Code" to smeCode,
                        "Panel Observations" to "${1 + monthRows.size} row(s) (${monthRows.size} monthly + 1 overall)",
                        "Research Variables" to "${columns.size} numeric/categorical fields (IV1–IV4 → DV)",
                        "Dataset Mode" to dataMode
                    ),
                    columns = columns,
                    rows = listOf(overallRow) + monthRows,
                    footerNote = "Anonymized SPSS/Excel panel dataset — ${ResearchFramework.METHODOLOGY_NOTE}"
                )
            }
        }
    }

    fun buildCsvContent(report: GeneratedBusinessReport): String {
        fun escapeCsv(cell: String): String {
            val needsQuote = cell.contains(',') || cell.contains('"') || cell.contains('\n') || cell.contains('\r')
            val escaped = cell.replace("\"", "\"\"")
            return if (needsQuote) "\"$escaped\"" else escaped
        }

        return buildString {
            appendLine("# ${report.title}")
            appendLine("# Business: ${report.businessName} (${report.location})")
            appendLine("# Period: ${report.periodLabel} | Generated: ${report.generatedDateIso} | Mode: ${if (report.isDemoData) "DEMO SAMPLE DATA" else "LIVE DATA"}")
            report.summaryPairs.forEach { (k, v) ->
                appendLine("# $k: $v")
            }
            appendLine()
            appendLine(report.columns.joinToString(",") { escapeCsv(it) })
            report.rows.forEach { row ->
                appendLine(row.joinToString(",") { escapeCsv(it) })
            }
            appendLine()
            appendLine("# Note: ${report.footerNote}")
        }
    }

    fun buildRawTabularCsvContent(report: GeneratedBusinessReport): String {
        fun escapeCsv(cell: String): String {
            val needsQuote = cell.contains(',') || cell.contains('"') || cell.contains('\n') || cell.contains('\r')
            val escaped = cell.replace("\"", "\"\"")
            return if (needsQuote) "\"$escaped\"" else escaped
        }

        return buildString {
            appendLine(report.columns.joinToString(",") { escapeCsv(it) })
            report.rows.forEach { row ->
                appendLine(row.joinToString(",") { escapeCsv(it) })
            }
        }
    }

    fun writeCsvToStream(report: GeneratedBusinessReport, outputStream: OutputStream) {
        val csvContent = if (report.type == BusinessReportType.RESEARCH_DATASET_SPSS) {
            buildRawTabularCsvContent(report)
        } else {
            buildCsvContent(report)
        }
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(csvContent)
            writer.flush()
        }
    }

    fun writePdfToStream(report: GeneratedBusinessReport, outputStream: OutputStream) {
        val document = PdfDocument()
        try {
            renderReportIntoPdfDocument(document, report)
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    fun printReport(context: Context, report: GeneratedBusinessReport) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "ProjectX_${report.type.name}_${report.generatedDateIso}"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val doc = PdfDocument()
                try {
                    renderReportIntoPdfDocument(doc, report)
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    doc.close()
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    private fun renderReportIntoPdfDocument(document: PdfDocument, report: GeneratedBusinessReport) {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - margin * 2

        val headerBgPaint = Paint().apply { color = Color.rgb(10, 29, 55) }
        val accentPaint = Paint().apply { color = Color.rgb(124, 58, 237) }
        val whiteTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val whiteSubPaint = Paint().apply {
            color = Color.rgb(216, 224, 240)
            textSize = 9.5f
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(10, 29, 55)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(32, 38, 48)
            textSize = 9f
        }
        val boldBodyPaint = Paint().apply {
            color = Color.rgb(10, 29, 55)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(98, 108, 122)
            textSize = 8f
        }
        val rulePaint = Paint().apply {
            color = Color.rgb(218, 223, 232)
            strokeWidth = 0.8f
        }

        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = 0f

        fun drawPageHeader(isFirstPage: Boolean) {
            if (isFirstPage) {
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 76f, headerBgPaint)
                canvas.drawRect(0f, 76f, pageWidth.toFloat(), 80f, accentPaint)
                canvas.drawText("PROJECT X — ${report.title}", margin, 30f, whiteTitlePaint)
                canvas.drawText(
                    "${report.businessName} (${report.location}) • Period: ${report.periodLabel}",
                    margin,
                    48f,
                    whiteSubPaint
                )
                val modeBadge = if (report.isDemoData) "DEMO SAMPLE DATA • Generated ${report.generatedDateIso}" else "Live Cloud Firestore • Generated ${report.generatedDateIso}"
                canvas.drawText(modeBadge, margin, 64f, whiteSubPaint)
                y = 102f
            } else {
                canvas.drawText("PROJECT X — ${report.title} (Page $pageNum)", margin, 34f, mutedPaint)
                canvas.drawLine(margin, 42f, pageWidth - margin, 42f, rulePaint)
                y = 58f
            }
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > pageHeight - 44f) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                drawPageHeader(isFirstPage = false)
            }
        }

        drawPageHeader(isFirstPage = true)

        // Summary KPIs
        canvas.drawText("Executive Summary", margin, y, sectionPaint)
        y += 14f
        report.summaryPairs.forEach { (label, value) ->
            ensureSpace(16f)
            canvas.drawText("$label:", margin, y, bodyPaint)
            canvas.drawText(value, margin + 180f, y, boldBodyPaint)
            y += 14f
        }
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, rulePaint)
        y += 16f

        // Table
        val colCount = report.columns.size.coerceAtLeast(1)
        val colW = contentWidth / colCount
        val maxChars = (colW / 5.2f).toInt().coerceAtLeast(8)

        fun drawTableRow(cells: List<String>, isHeader: Boolean) {
            ensureSpace(18f)
            val paintToUse = if (isHeader) boldBodyPaint else bodyPaint
            cells.forEachIndexed { idx, cell ->
                val clipped = if (cell.length > maxChars) cell.take(maxChars - 1) + "…" else cell
                canvas.drawText(clipped, margin + idx * colW, y, paintToUse)
            }
            y += 14f
            if (isHeader) {
                canvas.drawLine(margin, y - 9f, pageWidth - margin, y - 9f, rulePaint)
            }
        }

        drawTableRow(report.columns, isHeader = true)
        if (report.rows.isEmpty()) {
            ensureSpace(18f)
            canvas.drawText("No records match the selected report filter.", margin, y, mutedPaint)
            y += 16f
        } else {
            report.rows.forEach { row ->
                drawTableRow(row, isHeader = false)
            }
        }

        ensureSpace(28f)
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, rulePaint)
        y += 14f
        canvas.drawText(report.footerNote.take(105), margin, y, mutedPaint)
        document.finishPage(page)
    }

    private fun buildFooterNote(isDemoData: Boolean): String {
        val prefix = if (isDemoData) "DEMO SAMPLE DATA — " else ""
        return "${prefix}Project X SME Platform (KES) • ${ResearchFramework.METHODOLOGY_NOTE}"
    }
}
