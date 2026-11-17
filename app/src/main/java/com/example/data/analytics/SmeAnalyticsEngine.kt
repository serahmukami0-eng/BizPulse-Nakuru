package com.example.data.analytics

import com.example.data.model.AnalyticsDateFilter
import com.example.data.model.BusinessTransaction
import com.example.data.model.DailyChartPoint
import com.example.data.model.DateRangeWindow
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.computePreviousDateRange
import com.example.data.model.formatKes
import com.example.data.model.isIsoDateWithinRange
import com.example.data.model.isoDateDaysAgo
import com.example.data.model.resolveDateRangeWindow
import com.example.data.model.todayIsoDate
import kotlin.math.sqrt

data class AnalyticsFilterState(
    val filter: AnalyticsDateFilter = AnalyticsDateFilter.LAST_30_DAYS,
    val customStartIso: String = isoDateDaysAgo(29),
    val customEndIso: String = todayIsoDate()
)

data class LabeledValuePoint(
    val label: String,
    val value: Double,
    val secondaryValue: Double = 0.0
)

data class CampaignPerformanceItem(
    val title: String,
    val channel: String,
    val date: String,
    val spendKes: Double,
    val reach: Long,
    val engagement: Long,
    val inquiries: Long,
    val costPerInquiryKes: Double?
)

data class DigitalMarketingAnalytics(
    val campaignCount: Int,
    val totalSpendKes: Double,
    val totalReach: Long,
    val totalEngagement: Long,
    val totalInquiries: Long,
    val avgEngagementRatePct: Double?,
    val costPerInquiryKes: Double?,
    val campaignComparisons: List<CampaignPerformanceItem>,
    val trendPoints: List<LabeledValuePoint>
)

data class PaymentMethodUsageItem(
    val methodCode: String,
    val displayLabel: String,
    val transactionCount: Int,
    val totalAmountKes: Double,
    val salesAmountKes: Double,
    val shareOfTransactionsPct: Double
)

data class DigitalPaymentAnalytics(
    val totalDigitalPaymentsKes: Double,
    val digitalTransactionCount: Int,
    val totalTransactionCount: Int,
    val digitalAdoptionPct: Int?,
    val digitalSalesKes: Double,
    val totalRecordedSalesKes: Double,
    val digitalAsPercentOfSales: Double?,
    val cashSalesKes: Double,
    val methodBreakdown: List<PaymentMethodUsageItem>,
    val trendPoints: List<LabeledValuePoint>
)

data class RecordKeepingAnalytics(
    val recordedSalesKes: Double,
    val recordedExpensesKes: Double,
    val otherRecordsKes: Double,
    val estimatedNetProfitKes: Double,
    val salesCount: Int,
    val expenseCount: Int,
    val otherCount: Int,
    val totalTransactionsCount: Int,
    val recordCompletenessPct: Int?,
    val completenessDetails: String,
    val inventoryCount: Int,
    val lowStockCount: Int,
    val revenueExpenseTrend: List<DailyChartPoint>
)

data class ChannelEnquirySummary(
    val channel: String,
    val totalCount: Int,
    val respondedCount: Int,
    val pendingCount: Int
)

data class OnlineCommunicationAnalytics(
    val totalEnquiries: Int,
    val respondedEnquiries: Int,
    val convertedEnquiries: Int,
    val pendingEnquiries: Int,
    val responseRatePct: Double?,
    val avgResponseMinutes: Int?,
    val enquiriesByChannel: List<ChannelEnquirySummary>,
    val trendPoints: List<LabeledValuePoint>
)

data class SmePerformanceAnalytics(
    val currentSalesKes: Double,
    val previousSalesKes: Double?,
    val salesGrowthPct: Double?,
    val salesGrowthNote: String,
    val currentCustomerCount: Int,
    val previousCustomerCount: Int?,
    val customerGrowthPct: Double?,
    val customerGrowthNote: String,
    val recordedTransactionsCount: Int,
    val recordCompletenessPct: Int?,
    val enquiryResponseRatePct: Double?,
    val digitalPaymentAdoptionPct: Int?,
    val estimatedNetProfitKes: Double,
    val netMarginPct: Double?,
    val profitabilityTrend: List<LabeledValuePoint>,
    val overviewSummary: String
)

data class ResearchVariableComparison(
    val variableCode: String,
    val title: String,
    val comparisonLabel: String,
    val ivMetricSummary: String,
    val dvMetricSummary: String,
    val pairedObservationsCount: Int,
    val correlationR: Double?,
    val interpretation: String,
    val limitationNote: String?
)

data class SmeFullAnalyticsReport(
    val isDemoMode: Boolean,
    val filterState: AnalyticsFilterState,
    val activeWindow: DateRangeWindow,
    val previousWindow: DateRangeWindow?,
    val filteredTransactions: List<BusinessTransaction>,
    val filteredCampaigns: List<DigitalMarketingCampaign>,
    val filteredCommunications: List<OnlineCommunicationLog>,
    val marketing: DigitalMarketingAnalytics,
    val payments: DigitalPaymentAnalytics,
    val recordKeeping: RecordKeepingAnalytics,
    val communication: OnlineCommunicationAnalytics,
    val performance: SmePerformanceAnalytics,
    val researchComparisons: List<ResearchVariableComparison>,
    val dataGapsAndLimitations: List<String>
)

object SmeAnalyticsEngine {

    fun compute(
        isDemoMode: Boolean,
        filterState: AnalyticsFilterState,
        allTransactions: List<BusinessTransaction>,
        inventory: List<InventoryItem>,
        allCampaigns: List<DigitalMarketingCampaign>,
        allCommunications: List<OnlineCommunicationLog>,
        referenceTodayIso: String = todayIsoDate()
    ): SmeFullAnalyticsReport {
        val activeWindow = resolveDateRangeWindow(
            filter = filterState.filter,
            customStartIso = filterState.customStartIso,
            customEndIso = filterState.customEndIso,
            referenceTodayIso = referenceTodayIso
        )
        val previousWindow = computePreviousDateRange(activeWindow)

        val currentTx = allTransactions.filter {
            it.isActiveLedgerEntry() && isIsoDateWithinRange(it.transactionDate, it.createdAt, activeWindow)
        }
        val currentCampaigns = allCampaigns.filter {
            isIsoDateWithinRange(it.campaignDate, it.createdAt, activeWindow)
        }
        val currentComms = allCommunications.filter {
            isIsoDateWithinRange(it.receivedDate, it.createdAt, activeWindow)
        }

        val prevTx = previousWindow?.let { win ->
            allTransactions.filter {
                it.isActiveLedgerEntry() && isIsoDateWithinRange(it.transactionDate, it.createdAt, win)
            }
        }.orEmpty()
        val prevComms = previousWindow?.let { win ->
            allCommunications.filter { isIsoDateWithinRange(it.receivedDate, it.createdAt, win) }
        }.orEmpty()

        val marketing = computeMarketingAnalytics(currentCampaigns)
        val payments = computePaymentAnalytics(currentTx)
        val recordKeeping = computeRecordKeepingAnalytics(currentTx, inventory)
        val communication = computeCommunicationAnalytics(currentComms)

        val performance = computeSmePerformance(
            currentTx = currentTx,
            prevTx = prevTx,
            currentComms = currentComms,
            prevComms = prevComms,
            hasPrevWindow = previousWindow != null,
            marketing = marketing,
            payments = payments,
            recordKeeping = recordKeeping,
            communication = communication
        )

        val comparisons = computeResearchComparisons(
            currentTx = currentTx,
            currentCampaigns = currentCampaigns,
            currentComms = currentComms,
            marketing = marketing,
            payments = payments,
            recordKeeping = recordKeeping,
            communication = communication,
            performance = performance
        )

        val gaps = collectDataGaps(
            isDemoMode = isDemoMode,
            activeWindow = activeWindow,
            previousWindow = previousWindow,
            currentTx = currentTx,
            prevTx = prevTx,
            currentCampaigns = currentCampaigns,
            currentComms = currentComms,
            prevComms = prevComms,
            inventory = inventory,
            comparisons = comparisons
        )

        return SmeFullAnalyticsReport(
            isDemoMode = isDemoMode,
            filterState = filterState,
            activeWindow = activeWindow,
            previousWindow = previousWindow,
            filteredTransactions = currentTx,
            filteredCampaigns = currentCampaigns,
            filteredCommunications = currentComms,
            marketing = marketing,
            payments = payments,
            recordKeeping = recordKeeping,
            communication = communication,
            performance = performance,
            researchComparisons = comparisons,
            dataGapsAndLimitations = gaps
        )
    }

    private fun computeMarketingAnalytics(
        campaigns: List<DigitalMarketingCampaign>
    ): DigitalMarketingAnalytics {
        val totalSpend = campaigns.sumOf { it.spendKes }
        val totalReach = campaigns.sumOf { it.reachCount }
        val totalEngagement = campaigns.sumOf { it.engagementCount }
        val totalInquiries = campaigns.sumOf { it.inquiriesGenerated }

        val avgEngPct = if (totalReach > 0L) (totalEngagement.toDouble() / totalReach) * 100.0 else null
        val costPerInquiry = if (campaigns.isNotEmpty() && totalInquiries > 0L) {
            totalSpend / totalInquiries.toDouble()
        } else {
            null
        }

        val comparisons = campaigns.map { camp ->
            CampaignPerformanceItem(
                title = camp.title,
                channel = camp.channel,
                date = camp.effectiveDate(),
                spendKes = camp.spendKes,
                reach = camp.reachCount,
                engagement = camp.engagementCount,
                inquiries = camp.inquiriesGenerated,
                costPerInquiryKes = if (camp.inquiriesGenerated > 0L) camp.spendKes / camp.inquiriesGenerated else null
            )
        }.sortedByDescending { it.inquiries }

        val trendPoints = campaigns
            .groupBy { it.effectiveDate() }
            .toSortedMap()
            .map { (date, list) ->
                LabeledValuePoint(
                    label = shortDateLabel(date),
                    value = list.sumOf { it.inquiriesGenerated }.toDouble(),
                    secondaryValue = list.sumOf { it.spendKes }
                )
            }

        return DigitalMarketingAnalytics(
            campaignCount = campaigns.size,
            totalSpendKes = totalSpend,
            totalReach = totalReach,
            totalEngagement = totalEngagement,
            totalInquiries = totalInquiries,
            avgEngagementRatePct = avgEngPct,
            costPerInquiryKes = costPerInquiry,
            campaignComparisons = comparisons,
            trendPoints = trendPoints
        )
    }

    private fun computePaymentAnalytics(
        transactions: List<BusinessTransaction>
    ): DigitalPaymentAnalytics {
        val validDigitalTx = transactions.filter {
            it.isDigitalPayment && it.paymentStatus != BusinessTransaction.STATUS_FAILED
        }
        val totalDigitalKes = validDigitalTx.sumOf { it.amountKes }
        val digitalCount = transactions.count { it.isDigitalPayment }
        val adoptionPct = if (transactions.isNotEmpty()) ((digitalCount * 100.0) / transactions.size).toInt() else null

        val sales = transactions.filter { it.type == BusinessTransaction.TYPE_SALE }
        val totalSales = sales.sumOf { it.amountKes }
        val digitalSales = sales.filter {
            it.isDigitalPayment && it.paymentStatus != BusinessTransaction.STATUS_FAILED
        }.sumOf { it.amountKes }
        val cashSales = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_CASH }.sumOf { it.amountKes }
        val digitalPctOfSales = if (totalSales > 0.0) (digitalSales / totalSales) * 100.0 else null

        val methodLabels = mapOf(
            BusinessTransaction.METHOD_MPESA to "M-Pesa / Till",
            BusinessTransaction.METHOD_BANK to "Bank Transfer",
            BusinessTransaction.METHOD_CARD to "Card / POS",
            BusinessTransaction.METHOD_OTHER_DIGITAL to "Other Digital",
            BusinessTransaction.METHOD_CASH to "Cash Drawer"
        )
        val txCountTotal = transactions.size.coerceAtLeast(1)
        val breakdown = BusinessTransaction.allPaymentMethods.map { method ->
            val matching = transactions.filter { it.paymentMethod == method }
            val matchingValid = matching.filter { it.paymentStatus != BusinessTransaction.STATUS_FAILED }
            val matchingSales = matchingValid.filter { it.type == BusinessTransaction.TYPE_SALE }
            PaymentMethodUsageItem(
                methodCode = method,
                displayLabel = methodLabels[method] ?: method,
                transactionCount = matching.size,
                totalAmountKes = matchingValid.sumOf { it.amountKes },
                salesAmountKes = matchingSales.sumOf { it.amountKes },
                shareOfTransactionsPct = if (transactions.isNotEmpty()) (matching.size * 100.0) / txCountTotal else 0.0
            )
        }

        val trendPoints = transactions
            .groupBy { it.effectiveDate() }
            .toSortedMap()
            .map { (date, list) ->
                val digAmount = list.filter {
                    it.isDigitalPayment && it.paymentStatus != BusinessTransaction.STATUS_FAILED
                }.sumOf { it.amountKes }
                val cashAmount = list.filter { it.paymentMethod == BusinessTransaction.METHOD_CASH }.sumOf { it.amountKes }
                LabeledValuePoint(
                    label = shortDateLabel(date),
                    value = digAmount,
                    secondaryValue = cashAmount
                )
            }

        return DigitalPaymentAnalytics(
            totalDigitalPaymentsKes = totalDigitalKes,
            digitalTransactionCount = digitalCount,
            totalTransactionCount = transactions.size,
            digitalAdoptionPct = adoptionPct,
            digitalSalesKes = digitalSales,
            totalRecordedSalesKes = totalSales,
            digitalAsPercentOfSales = digitalPctOfSales,
            cashSalesKes = cashSales,
            methodBreakdown = breakdown,
            trendPoints = trendPoints
        )
    }

    private fun computeRecordKeepingAnalytics(
        transactions: List<BusinessTransaction>,
        inventory: List<InventoryItem>
    ): RecordKeepingAnalytics {
        val sales = transactions.filter { it.type == BusinessTransaction.TYPE_SALE }
        val expenses = transactions.filter { it.type == BusinessTransaction.TYPE_EXPENSE }
        val others = transactions.filter { it.type == BusinessTransaction.TYPE_OTHER }

        val salesKes = sales.sumOf { it.amountKes }
        val expensesKes = expenses.sumOf { it.amountKes }
        val otherKes = others.sumOf { it.amountKes }
        val estimatedNet = salesKes - expensesKes

        val completenessPct = if (transactions.isNotEmpty()) {
            val scores = transactions.map { tx ->
                var pts = 0
                if (tx.title.isNotBlank()) pts++
                if (tx.category.isNotBlank()) pts++
                if (tx.transactionDate.isNotBlank()) pts++
                if (!tx.isDigitalPayment || tx.transactionRef.isNotBlank()) pts++
                pts / 4.0
            }
            ((scores.average()) * 100.0).toInt()
        } else {
            null
        }

        val withRefCount = transactions.count { it.isDigitalPayment && it.transactionRef.isNotBlank() }
        val digitalCount = transactions.count { it.isDigitalPayment }
        val completenessDetails = if (transactions.isEmpty()) {
            "No ledger transactions in selected period to score completeness."
        } else {
            "$withRefCount/$digitalCount digital entries include payment references; ${transactions.count { it.transactionDate.isNotBlank() }}/${transactions.size} include explicit dates."
        }

        val trend = transactions
            .groupBy { it.effectiveDate() }
            .toSortedMap()
            .map { (date, list) ->
                DailyChartPoint(
                    dayLabel = shortDateLabel(date),
                    salesKes = list.filter { it.type == BusinessTransaction.TYPE_SALE }.sumOf { it.amountKes },
                    expensesKes = list.filter { it.type == BusinessTransaction.TYPE_EXPENSE }.sumOf { it.amountKes }
                )
            }

        return RecordKeepingAnalytics(
            recordedSalesKes = salesKes,
            recordedExpensesKes = expensesKes,
            otherRecordsKes = otherKes,
            estimatedNetProfitKes = estimatedNet,
            salesCount = sales.size,
            expenseCount = expenses.size,
            otherCount = others.size,
            totalTransactionsCount = transactions.size,
            recordCompletenessPct = completenessPct,
            completenessDetails = completenessDetails,
            inventoryCount = inventory.size,
            lowStockCount = inventory.count { it.isLowStock },
            revenueExpenseTrend = trend
        )
    }

    private fun computeCommunicationAnalytics(
        logs: List<OnlineCommunicationLog>
    ): OnlineCommunicationAnalytics {
        val total = logs.size
        val responded = logs.count {
            it.status == OnlineCommunicationLog.STATUS_RESPONDED ||
                it.status == OnlineCommunicationLog.STATUS_CONVERTED
        }
        val converted = logs.count { it.status == OnlineCommunicationLog.STATUS_CONVERTED }
        val pending = logs.count { it.status == OnlineCommunicationLog.STATUS_PENDING }

        val responseRatePct = if (total > 0) (responded.toDouble() / total) * 100.0 else null
        val avgMinutes = if (total > 0) logs.map { it.responseMinutes }.average().toInt() else null

        val byChannel = OnlineCommunicationLog.channels.mapNotNull { ch ->
            val subset = logs.filter { it.channel == ch }
            if (subset.isEmpty()) null
            else ChannelEnquirySummary(
                channel = ch,
                totalCount = subset.size,
                respondedCount = subset.count {
                    it.status == OnlineCommunicationLog.STATUS_RESPONDED ||
                        it.status == OnlineCommunicationLog.STATUS_CONVERTED
                },
                pendingCount = subset.count { it.status == OnlineCommunicationLog.STATUS_PENDING }
            )
        }.sortedByDescending { it.totalCount }

        val trend = logs
            .groupBy { it.effectiveReceivedDate() }
            .toSortedMap()
            .map { (date, list) ->
                val answered = list.count {
                    it.status == OnlineCommunicationLog.STATUS_RESPONDED ||
                        it.status == OnlineCommunicationLog.STATUS_CONVERTED
                }
                LabeledValuePoint(
                    label = shortDateLabel(date),
                    value = list.size.toDouble(),
                    secondaryValue = answered.toDouble()
                )
            }

        return OnlineCommunicationAnalytics(
            totalEnquiries = total,
            respondedEnquiries = responded,
            convertedEnquiries = converted,
            pendingEnquiries = pending,
            responseRatePct = responseRatePct,
            avgResponseMinutes = avgMinutes,
            enquiriesByChannel = byChannel,
            trendPoints = trend
        )
    }

    private fun computeSmePerformance(
        currentTx: List<BusinessTransaction>,
        prevTx: List<BusinessTransaction>,
        currentComms: List<OnlineCommunicationLog>,
        prevComms: List<OnlineCommunicationLog>,
        hasPrevWindow: Boolean,
        marketing: DigitalMarketingAnalytics,
        payments: DigitalPaymentAnalytics,
        recordKeeping: RecordKeepingAnalytics,
        communication: OnlineCommunicationAnalytics
    ): SmePerformanceAnalytics {
        val currentSales = recordKeeping.recordedSalesKes
        val prevSales = if (hasPrevWindow) {
            prevTx.filter { it.type == BusinessTransaction.TYPE_SALE }.sumOf { it.amountKes }
        } else {
            null
        }
        val salesGrowthPct = if (prevSales != null && prevSales > 0.0) {
            ((currentSales - prevSales) / prevSales) * 100.0
        } else {
            null
        }
        val salesGrowthNote = when {
            !hasPrevWindow -> "Select Today, 7d, 30d, 90d, or Custom Range to compare against a prior equal period."
            prevSales == null || prevSales <= 0.0 -> "No recorded sales in the preceding period to compute % growth."
            else -> "Compared against ${formatKes(prevSales)} in the preceding equal period."
        }

        val currentCustomers = currentComms
            .map { it.customerName.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .size
        val prevCustomers = if (hasPrevWindow) {
            prevComms.map { it.customerName.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().size
        } else {
            null
        }
        val customerGrowthPct = if (prevCustomers != null && prevCustomers > 0) {
            ((currentCustomers - prevCustomers).toDouble() / prevCustomers) * 100.0
        } else {
            null
        }
        val customerGrowthNote = when {
            currentCustomers == 0 -> "No named customer communication records in this period yet."
            !hasPrevWindow -> "All-time view active; select a bounded period to compare customer growth."
            prevCustomers == null || prevCustomers <= 0 -> "$currentCustomers customer(s) recorded; no prior-period customer records to compute % growth."
            else -> "Unique logged customers grew/changed from $prevCustomers to $currentCustomers."
        }

        val marginPct = if (currentSales > 0.0) {
            (recordKeeping.estimatedNetProfitKes / currentSales) * 100.0
        } else {
            null
        }

        val profitTrend = recordKeeping.revenueExpenseTrend.map { pt ->
            LabeledValuePoint(
                label = pt.dayLabel,
                value = pt.salesKes - pt.expensesKes,
                secondaryValue = pt.salesKes
            )
        }

        val availableDims = listOfNotNull(
            if (recordKeeping.salesCount > 0) "Sales (${recordKeeping.salesCount})" else null,
            if (currentCustomers > 0 || marketing.totalInquiries > 0L) "Customers" else null,
            if (currentTx.isNotEmpty() || currentComms.isNotEmpty()) "Efficiency" else null,
            if (recordKeeping.salesCount > 0 || recordKeeping.expenseCount > 0) "Profitability" else null
        )
        val overview = if (availableDims.isEmpty()) {
            "Insufficient live records in the selected date window to evaluate SME performance."
        } else {
            "Evaluating ${availableDims.joinToString(", ")} from ${currentTx.size} transaction(s), ${marketing.campaignCount} campaign(s), and ${communication.totalEnquiries} enquiry log(s)."
        }

        return SmePerformanceAnalytics(
            currentSalesKes = currentSales,
            previousSalesKes = prevSales,
            salesGrowthPct = salesGrowthPct,
            salesGrowthNote = salesGrowthNote,
            currentCustomerCount = currentCustomers,
            previousCustomerCount = prevCustomers,
            customerGrowthPct = customerGrowthPct,
            customerGrowthNote = customerGrowthNote,
            recordedTransactionsCount = currentTx.size,
            recordCompletenessPct = recordKeeping.recordCompletenessPct,
            enquiryResponseRatePct = communication.responseRatePct,
            digitalPaymentAdoptionPct = payments.digitalAdoptionPct,
            estimatedNetProfitKes = recordKeeping.estimatedNetProfitKes,
            netMarginPct = marginPct,
            profitabilityTrend = profitTrend,
            overviewSummary = overview
        )
    }

    private fun computeResearchComparisons(
        currentTx: List<BusinessTransaction>,
        currentCampaigns: List<DigitalMarketingCampaign>,
        currentComms: List<OnlineCommunicationLog>,
        marketing: DigitalMarketingAnalytics,
        payments: DigitalPaymentAnalytics,
        recordKeeping: RecordKeepingAnalytics,
        communication: OnlineCommunicationAnalytics,
        performance: SmePerformanceAnalytics
    ): List<ResearchVariableComparison> {
        val allDates = (
            currentTx.map { it.effectiveDate() } +
                currentCampaigns.map { it.effectiveDate() } +
                currentComms.map { it.effectiveReceivedDate() }
            ).distinct().sorted()

        val salesByDate = currentTx.filter { it.type == BusinessTransaction.TYPE_SALE }
            .groupBy { it.effectiveDate() }
            .mapValues { (_, list) -> list.sumOf { it.amountKes } }

        // 1. IV1 Digital Marketing vs Sales Performance
        val mktDates = allDates.filter { d ->
            currentCampaigns.any { it.effectiveDate() == d } && salesByDate.containsKey(d)
        }
        val mktXs = mktDates.map { d -> currentCampaigns.filter { it.effectiveDate() == d }.sumOf { it.spendKes } }
        val mktYs = mktDates.map { d -> salesByDate[d] ?: 0.0 }
        val mktR = computePearsonCorrelation(mktXs, mktYs)

        // 2. IV2 Digital Payments vs Recorded Sales
        val payDates = allDates.filter { d -> currentTx.any { it.effectiveDate() == d } }
        val payXs = payDates.map { d ->
            currentTx.filter { it.effectiveDate() == d && it.isDigitalPayment && it.paymentStatus != BusinessTransaction.STATUS_FAILED }
                .sumOf { it.amountKes }
        }
        val payYs = payDates.map { d -> salesByDate[d] ?: 0.0 }
        val payR = computePearsonCorrelation(payXs, payYs)

        // 3. IV3 Computerized Record Keeping vs Operational Efficiency (completeness + volume vs digital/efficiency score)
        val recXs = payDates.map { d ->
            currentTx.count { it.effectiveDate() == d }.toDouble()
        }
        val recYs = payDates.map { d ->
            val dayTx = currentTx.filter { it.effectiveDate() == d }
            if (dayTx.isEmpty()) 0.0
            else (dayTx.count { it.transactionRef.isNotBlank() || !it.isDigitalPayment }.toDouble() / dayTx.size) * 100.0
        }
        val recR = computePearsonCorrelation(recXs, recYs)

        // 4. IV4 Online Communication vs Customer Growth & Engagement
        val commDates = allDates.filter { d -> currentComms.any { it.effectiveReceivedDate() == d } }
        val commXs = commDates.map { d -> currentComms.count { it.effectiveReceivedDate() == d }.toDouble() }
        val commYs = commDates.map { d ->
            currentComms.count {
                it.effectiveReceivedDate() == d &&
                    (it.status == OnlineCommunicationLog.STATUS_RESPONDED || it.status == OnlineCommunicationLog.STATUS_CONVERTED)
            }.toDouble()
        }
        val commR = computePearsonCorrelation(commXs, commYs)

        return listOf(
            ResearchVariableComparison(
                variableCode = "IV1 → DV",
                title = "Digital Marketing vs. Sales Performance",
                comparisonLabel = "Marketing spend & inquiries vs. KES sales",
                ivMetricSummary = "${marketing.campaignCount} campaigns • ${formatKes(marketing.totalSpendKes)} spend • ${marketing.totalInquiries} inquiries",
                dvMetricSummary = "${formatKes(performance.currentSalesKes)} sales (${recordKeeping.salesCount} entries)",
                pairedObservationsCount = mktDates.size,
                correlationR = mktR,
                interpretation = formatCorrelationText(mktR, "marketing activity", "daily sales"),
                limitationNote = if (mktR == null) "Requires ≥ 3 overlapping days with both marketing activity and recorded sales (with varying values)." else null
            ),
            ResearchVariableComparison(
                variableCode = "IV2 → DV",
                title = "Digital Payment Usage vs. Recorded Sales",
                comparisonLabel = "Digital payment KES volume vs. daily recorded sales",
                ivMetricSummary = "${payments.digitalAdoptionPct ?: 0}% digital adoption • ${formatKes(payments.totalDigitalPaymentsKes)} digital payments",
                dvMetricSummary = "${payments.digitalAsPercentOfSales?.let { String.format(java.util.Locale.US, "%.1f%%", it) } ?: "N/A"} of recorded sales paid digitally",
                pairedObservationsCount = payDates.size,
                correlationR = payR,
                interpretation = formatCorrelationText(payR, "digital payment volume", "recorded sales"),
                limitationNote = if (payR == null) "Requires ≥ 3 active transaction days with varying digital payment and sales figures." else null
            ),
            ResearchVariableComparison(
                variableCode = "IV3 → DV",
                title = "Computerized Record Keeping vs. Operational Efficiency",
                comparisonLabel = "Daily ledger volume vs. record documentation completeness",
                ivMetricSummary = "${recordKeeping.totalTransactionsCount} records • Completeness: ${recordKeeping.recordCompletenessPct ?: 0}%",
                dvMetricSummary = "Response rate: ${communication.responseRatePct?.let { "${it.toInt()}%" } ?: "N/A"} • Low stock alerts: ${recordKeeping.lowStockCount}",
                pairedObservationsCount = payDates.size,
                correlationR = recR,
                interpretation = formatCorrelationText(recR, "daily record count", "documentation completeness"),
                limitationNote = if (recR == null) "Requires ≥ 3 active transaction days with varying transaction counts and completeness scores." else null
            ),
            ResearchVariableComparison(
                variableCode = "IV4 → DV",
                title = "Online Communication vs. Customer Engagement & Conversions",
                comparisonLabel = "Daily online inquiries vs. responded/converted leads",
                ivMetricSummary = "${communication.totalEnquiries} enquiries • Response rate: ${communication.responseRatePct?.let { "${it.toInt()}%" } ?: "N/A"}",
                dvMetricSummary = "${performance.currentCustomerCount} unique customers • ${communication.convertedEnquiries} converted",
                pairedObservationsCount = commDates.size,
                correlationR = commR,
                interpretation = formatCorrelationText(commR, "daily enquiries", "responded/converted customers"),
                limitationNote = if (commR == null) "Requires ≥ 3 days of logged customer communications with varying counts." else null
            )
        )
    }

    internal fun computePearsonCorrelation(xs: List<Double>, ys: List<Double>): Double? {
        if (xs.size != ys.size || xs.size < 3) return null
        val n = xs.size.toDouble()
        val meanX = xs.sum() / n
        val meanY = ys.sum() / n
        var sumXY = 0.0
        var sumX2 = 0.0
        var sumY2 = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - meanX
            val dy = ys[i] - meanY
            sumXY += dx * dy
            sumX2 += dx * dx
            sumY2 += dy * dy
        }
        val denom = sqrt(sumX2 * sumY2)
        if (denom <= 1e-9) return null
        return (sumXY / denom).coerceIn(-1.0, 1.0)
    }

    private fun formatCorrelationText(r: Double?, xLabel: String, yLabel: String): String {
        if (r == null) {
            return "Insufficient paired observations to compute a reliable Pearson correlation between $xLabel and $yLabel."
        }
        val strength = when {
            kotlin.math.abs(r) >= 0.7 -> "strong"
            kotlin.math.abs(r) >= 0.4 -> "moderate"
            kotlin.math.abs(r) >= 0.2 -> "weak"
            else -> "negligible"
        }
        val direction = if (r >= 0) "positive" else "negative"
        return "Observed $strength $direction association (r = ${String.format(java.util.Locale.US, "%.2f", r)}) between $xLabel and $yLabel. Correlation does not prove causation."
    }

    private fun collectDataGaps(
        isDemoMode: Boolean,
        activeWindow: DateRangeWindow,
        previousWindow: DateRangeWindow?,
        currentTx: List<BusinessTransaction>,
        prevTx: List<BusinessTransaction>,
        currentCampaigns: List<DigitalMarketingCampaign>,
        currentComms: List<OnlineCommunicationLog>,
        prevComms: List<OnlineCommunicationLog>,
        inventory: List<InventoryItem>,
        comparisons: List<ResearchVariableComparison>
    ): List<String> {
        val gaps = mutableListOf<String>()
        if (isDemoMode) {
            gaps.add("DEMO DATA active: analytics currently reflect sample preview records, not live business transactions.")
        }
        if (currentTx.isEmpty()) {
            gaps.add("No sales, expense, or payment transactions recorded in ${activeWindow.label}.")
        }
        if (previousWindow != null && prevTx.none { it.type == BusinessTransaction.TYPE_SALE }) {
            gaps.add("No prior-period sales (${previousWindow.label}) available to compute Sales Growth %.")
        }
        if (currentComms.isEmpty()) {
            gaps.add("No online customer communication logs in this period — Customer Growth and Response Rate cannot be measured.")
        } else if (previousWindow != null && prevComms.isEmpty()) {
            gaps.add("No prior-period customer communication records to compute Customer Growth %.")
        }
        if (currentCampaigns.isEmpty()) {
            gaps.add("No digital marketing campaigns logged in this period — Cost per Enquiry and Marketing ROI are unavailable.")
        }
        if (inventory.isEmpty()) {
            gaps.add("No inventory items recorded — stock health compliance cannot be assessed.")
        }
        val missingCorr = comparisons.count { it.correlationR == null }
        if (missingCorr > 0) {
            gaps.add("$missingCorr of 4 research variable comparisons lack ≥ 3 paired daily observations for Pearson correlation.")
        }
        return gaps
    }

    private fun shortDateLabel(isoDate: String): String {
        return if (isoDate.length >= 10) isoDate.substring(5, 10) else isoDate
    }
}
