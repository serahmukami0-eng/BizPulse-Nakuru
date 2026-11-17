package com.example.data.analytics

import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.InventoryItem
import com.example.data.model.todayIsoDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class BarChartEntry(
    val label: String,
    val primaryValue: Double,
    val secondaryValue: Double? = null,
    val badge: String? = null
)

data class MonthlyPerformanceRow(
    val monthKey: String,
    val monthLabel: String,
    val salesKes: Double,
    val expensesKes: Double,
    val cogsKes: Double,
    val grossProfitKes: Double?,
    val netProfitKes: Double,
    val momSalesGrowthPct: Double?
)

data class SalesProjectionRow(
    val monthKey: String,
    val monthLabel: String,
    val actualSalesKes: Double?,
    val projectedSalesKes: Double,
    val isProjection: Boolean
)

data class SalesProjectionSummary(
    val horizonMonths: Int,
    val historicalMonthsCount: Int,
    val hasSufficientHistory: Boolean,
    val slopeKesPerMonth: Double,
    val interceptKes: Double,
    val warningMessage: String?,
    val methodologyNote: String,
    val rows: List<SalesProjectionRow>
)

enum class ForecastScenario(val label: String, val multiplier: Double) {
    CONSERVATIVE("Conservative (-15%)", 0.85),
    BASELINE("Baseline (OLS)", 1.00),
    OPTIMISTIC("Optimistic (+15%)", 1.15)
}

data class ExpenseDriverSummary(
    val category: String,
    val amountKes: Double,
    val sharePct: Double
)

data class FinancialForecastMetrics(
    val avgMonthlySalesKes: Double,
    val avgMonthlyExpensesKes: Double,
    val avgMonthlyNetCashflowKes: Double,
    val breakEvenMonthlyRevenueKes: Double,
    val breakEvenCoveragePct: Double?,
    val historicalFitErrorPct: Double?,
    val avgCustomerSpendKes: Double?,
    val topExpenseDrivers: List<ExpenseDriverSummary>
)

data class Prompt4AnalyticsBundle(
    val isDemoMode: Boolean,
    val horizonMonths: Int,
    val monthlyRows: List<MonthlyPerformanceRow>,
    val projection: SalesProjectionSummary,
    val monthlySalesBars: List<BarChartEntry>,
    val actualVsProjectedBars: List<BarChartEntry>,
    val revenueVsExpenseBars: List<BarChartEntry>,
    val profitabilityBars: List<BarChartEntry>,
    val customerGrowthBars: List<BarChartEntry>,
    val paymentMethodBars: List<BarChartEntry>,
    val inventoryLevelBars: List<BarChartEntry>,
    val hasCostDataForGrossProfit: Boolean,
    val totalGrossProfitKes: Double?,
    val totalCogsKes: Double,
    val latestMoMSalesGrowthPct: Double?,
    val repeatCustomerCount: Int,
    val customerRepeatRatePct: Double?,
    val totalInventoryRetailValueKes: Double,
    val totalInventoryCostValueKes: Double,
    val financialMetrics: FinancialForecastMetrics = FinancialForecastMetrics(
        avgMonthlySalesKes = 0.0,
        avgMonthlyExpensesKes = 0.0,
        avgMonthlyNetCashflowKes = 0.0,
        breakEvenMonthlyRevenueKes = 0.0,
        breakEvenCoveragePct = null,
        historicalFitErrorPct = null,
        avgCustomerSpendKes = null,
        topExpenseDrivers = emptyList()
    )
)

object Prompt4ForecastingEngine {

    fun compute(
        isDemoMode: Boolean,
        allTransactions: List<BusinessTransaction>,
        inventory: List<InventoryItem>,
        customers: List<CustomerRecord>,
        horizonMonths: Int = 6,
        referenceTodayIso: String = todayIsoDate()
    ): Prompt4AnalyticsBundle {
        val safeHorizon = horizonMonths.coerceIn(3, 12)
        val activeTx = allTransactions.filter { it.isActiveLedgerEntry() }

        val anyCogsRecorded = activeTx.any { it.type == BusinessTransaction.TYPE_SALE && it.costOfGoodsSoldKes > 0.0 }
        val totalCogs = activeTx.filter { it.type == BusinessTransaction.TYPE_SALE }.sumOf { it.costOfGoodsSoldKes }
        val totalSalesAll = activeTx.filter { it.type == BusinessTransaction.TYPE_SALE }.sumOf { it.amountKes }
        val totalGrossProfit = if (anyCogsRecorded) totalSalesAll - totalCogs else null

        // 1. Monthly buckets
        val txByMonth = activeTx
            .mapNotNull { tx ->
                val dateStr = tx.effectiveDate()
                val monthKey = dateStr.take(7)
                if (Regex("""^\d{4}-\d{2}$""").matches(monthKey)) monthKey to tx else null
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()

        var prevMonthSales: Double? = null
        val monthlyRows = txByMonth.entries.map { (monthKey, txs) ->
            val sales = txs.filter { it.type == BusinessTransaction.TYPE_SALE }.sumOf { it.amountKes }
            val expenses = txs.filter { it.type == BusinessTransaction.TYPE_EXPENSE }.sumOf { it.amountKes }
            val cogs = txs.filter { it.type == BusinessTransaction.TYPE_SALE }.sumOf { it.costOfGoodsSoldKes }
            val gross = if (anyCogsRecorded) sales - cogs else null
            val net = sales - expenses
            val mom = if (prevMonthSales != null && prevMonthSales!! > 0.0) {
                ((sales - prevMonthSales!!) / prevMonthSales!!) * 100.0
            } else {
                null
            }
            prevMonthSales = sales
            MonthlyPerformanceRow(
                monthKey = monthKey,
                monthLabel = formatMonthLabel(monthKey),
                salesKes = sales,
                expensesKes = expenses,
                cogsKes = cogs,
                grossProfitKes = gross,
                netProfitKes = net,
                momSalesGrowthPct = mom
            )
        }

        // 2. Linear Regression Forecast (3, 6, 12 months)
        val projection = computeSalesProjection(
            monthlyRows = monthlyRows,
            horizonMonths = safeHorizon,
            referenceTodayIso = referenceTodayIso
        )

        // Bar Graph 1: Monthly Sales Performance
        val monthlySalesBars = monthlyRows.map { row ->
            BarChartEntry(
                label = row.monthLabel,
                primaryValue = row.salesKes,
                badge = row.momSalesGrowthPct?.let { pct ->
                    val sign = if (pct >= 0) "+" else ""
                    "$sign${pct.toInt()}%"
                }
            )
        }

        // Bar Graph 2: Actual vs Projected Sales
        val actualVsProjectedBars = projection.rows.map { row ->
            BarChartEntry(
                label = row.monthLabel,
                primaryValue = row.actualSalesKes ?: 0.0,
                secondaryValue = row.projectedSalesKes,
                badge = if (row.isProjection) "PROJ" else null
            )
        }

        // Bar Graph 3: Revenue vs Expenses
        val revenueVsExpenseBars = monthlyRows.map { row ->
            BarChartEntry(
                label = row.monthLabel,
                primaryValue = row.salesKes,
                secondaryValue = row.expensesKes
            )
        }

        // Bar Graph 4: Profitability Trends (Gross Profit when cost exists + Net Profit)
        val profitabilityBars = monthlyRows.map { row ->
            BarChartEntry(
                label = row.monthLabel,
                primaryValue = row.grossProfitKes ?: row.salesKes,
                secondaryValue = row.netProfitKes
            )
        }

        // Bar Graph 5: Customer Growth over time (monthly new + cumulative)
        val custByMonth = customers
            .mapNotNull { c ->
                val mk = c.effectiveCreatedDate().take(7)
                if (Regex("""^\d{4}-\d{2}$""").matches(mk)) mk to c else null
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()

        var cumulativeCustomers = 0
        val customerGrowthBars = custByMonth.entries.map { (mk, list) ->
            cumulativeCustomers += list.size
            BarChartEntry(
                label = formatMonthLabel(mk),
                primaryValue = list.size.toDouble(),
                secondaryValue = cumulativeCustomers.toDouble()
            )
        }

        // Bar Graph 6: Digital Payment Method Analysis
        val methodLabels = listOf(
            BusinessTransaction.METHOD_MPESA to "M-Pesa",
            BusinessTransaction.METHOD_BANK to "Bank",
            BusinessTransaction.METHOD_CARD to "Card",
            BusinessTransaction.METHOD_OTHER_DIGITAL to "Other Dig.",
            BusinessTransaction.METHOD_CASH to "Cash"
        )
        val paymentMethodBars = methodLabels.map { (code, shortName) ->
            val sumValid = activeTx
                .filter { it.paymentMethod == code && it.paymentStatus != BusinessTransaction.STATUS_FAILED }
                .sumOf { it.amountKes }
            BarChartEntry(
                label = shortName,
                primaryValue = sumValid
            )
        }

        // Bar Graph 7: Inventory Performance (Stock vs Reorder Level)
        val inventoryLevelBars = inventory.take(8).map { item ->
            BarChartEntry(
                label = item.name.take(12),
                primaryValue = item.quantity.toDouble(),
                secondaryValue = item.reorderLevel.toDouble(),
                badge = if (item.isLowStock) "LOW" else null
            )
        }

        val repeatCount = customers.count { it.isRepeatCustomer }
        val repeatRate = if (customers.isNotEmpty()) (repeatCount * 100.0) / customers.size else null

        val avgMonthlySales = if (monthlyRows.isNotEmpty()) monthlyRows.map { it.salesKes }.average() else 0.0
        val avgMonthlyExpenses = if (monthlyRows.isNotEmpty()) monthlyRows.map { it.expensesKes }.average() else 0.0
        val avgMonthlyNet = avgMonthlySales - avgMonthlyExpenses
        val breakEvenCoverage = if (avgMonthlyExpenses > 0.0) (avgMonthlySales / avgMonthlyExpenses) * 100.0 else null
        val fitErrors = projection.rows
            .filter { !it.isProjection && it.actualSalesKes != null && it.actualSalesKes > 0.0 }
            .map { abs(it.actualSalesKes!! - it.projectedSalesKes) / it.actualSalesKes * 100.0 }
        val fitErrorPct = if (fitErrors.isNotEmpty()) fitErrors.average() else null
        val avgCustSpend = if (customers.isNotEmpty()) customers.sumOf { it.totalSpentKes } / customers.size else null

        val expensesActive = activeTx.filter { it.type == BusinessTransaction.TYPE_EXPENSE }
        val totalExpSum = expensesActive.sumOf { it.amountKes }
        val topDrivers = expensesActive
            .groupBy { it.category.ifBlank { "Uncategorized" } }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amountKes }
                ExpenseDriverSummary(
                    category = cat,
                    amountKes = sum,
                    sharePct = if (totalExpSum > 0.0) (sum / totalExpSum) * 100.0 else 0.0
                )
            }
            .sortedByDescending { it.amountKes }
            .take(5)

        return Prompt4AnalyticsBundle(
            isDemoMode = isDemoMode,
            horizonMonths = safeHorizon,
            monthlyRows = monthlyRows,
            projection = projection,
            monthlySalesBars = monthlySalesBars,
            actualVsProjectedBars = actualVsProjectedBars,
            revenueVsExpenseBars = revenueVsExpenseBars,
            profitabilityBars = profitabilityBars,
            customerGrowthBars = customerGrowthBars,
            paymentMethodBars = paymentMethodBars,
            inventoryLevelBars = inventoryLevelBars,
            hasCostDataForGrossProfit = anyCogsRecorded,
            totalGrossProfitKes = totalGrossProfit,
            totalCogsKes = totalCogs,
            latestMoMSalesGrowthPct = monthlyRows.lastOrNull()?.momSalesGrowthPct,
            repeatCustomerCount = repeatCount,
            customerRepeatRatePct = repeatRate,
            totalInventoryRetailValueKes = inventory.sumOf { it.stockRetailValueKes },
            totalInventoryCostValueKes = inventory.sumOf { it.stockCostValueKes },
            financialMetrics = FinancialForecastMetrics(
                avgMonthlySalesKes = avgMonthlySales,
                avgMonthlyExpensesKes = avgMonthlyExpenses,
                avgMonthlyNetCashflowKes = avgMonthlyNet,
                breakEvenMonthlyRevenueKes = avgMonthlyExpenses,
                breakEvenCoveragePct = breakEvenCoverage,
                historicalFitErrorPct = fitErrorPct,
                avgCustomerSpendKes = avgCustSpend,
                topExpenseDrivers = topDrivers
            )
        )
    }

    internal fun computeSalesProjection(
        monthlyRows: List<MonthlyPerformanceRow>,
        horizonMonths: Int,
        referenceTodayIso: String
    ): SalesProjectionSummary {
        val n = monthlyRows.size
        if (n == 0) {
            return SalesProjectionSummary(
                horizonMonths = horizonMonths,
                historicalMonthsCount = 0,
                hasSufficientHistory = false,
                slopeKesPerMonth = 0.0,
                interceptKes = 0.0,
                warningMessage = "Insufficient historical data: no monthly sales records exist yet to generate projections.",
                methodologyNote = "OLS linear regression requires recorded monthly sales history.",
                rows = emptyList()
            )
        }

        val ys = monthlyRows.map { it.salesKes }
        val (slope, intercept) = if (n == 1) {
            0.0 to ys[0]
        } else {
            val xs = (0 until n).map { it.toDouble() }
            val meanX = xs.average()
            val meanY = ys.average()
            var num = 0.0
            var den = 0.0
            for (i in 0 until n) {
                val dx = xs[i] - meanX
                num += dx * (ys[i] - meanY)
                den += dx * dx
            }
            val b = if (abs(den) < 1e-9) 0.0 else num / den
            val a = meanY - b * meanX
            b to a
        }

        val hasSufficient = n >= 3
        val warning = when {
            n == 1 -> "Insufficient historical data (< 3 months): only 1 month recorded. Future values repeat the current baseline until 3+ months are logged."
            n == 2 -> "Insufficient historical data (< 3 months): only 2 months recorded. Projections are preliminary linear estimates until 3+ months exist."
            else -> null
        }

        val historicalRows = monthlyRows.mapIndexed { idx, row ->
            val fitted = (intercept + slope * idx).coerceAtLeast(0.0)
            SalesProjectionRow(
                monthKey = row.monthKey,
                monthLabel = row.monthLabel,
                actualSalesKes = row.salesKes,
                projectedSalesKes = fitted,
                isProjection = false
            )
        }

        val anchorMonthKey = monthlyRows.lastOrNull()?.monthKey ?: referenceTodayIso.take(7)
        val futureRows = (1..horizonMonths).map { step ->
            val futureKey = addMonthsToKey(anchorMonthKey, step)
            val predicted = (intercept + slope * (n - 1 + step)).coerceAtLeast(0.0)
            SalesProjectionRow(
                monthKey = futureKey,
                monthLabel = formatMonthLabel(futureKey),
                actualSalesKes = null,
                projectedSalesKes = predicted,
                isProjection = true
            )
        }

        val direction = when {
            slope > 100.0 -> "upward (+${String.format(Locale.US, "%,.0f", slope)} KES/month)"
            slope < -100.0 -> "downward (${String.format(Locale.US, "%,.0f", slope)} KES/month)"
            else -> "flat/stable"
        }

        val methodology = "Ordinary least-squares (OLS) linear trend computed over $n recorded month(s) showing a $direction slope. Future months are labeled PROJECTION and do not account for external seasonal shocks."

        return SalesProjectionSummary(
            horizonMonths = horizonMonths,
            historicalMonthsCount = n,
            hasSufficientHistory = hasSufficient,
            slopeKesPerMonth = slope,
            interceptKes = intercept,
            warningMessage = warning,
            methodologyNote = methodology,
            rows = historicalRows + futureRows
        )
    }

    internal fun formatMonthLabel(monthKey: String): String {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US).apply { isLenient = false }
        val formatter = SimpleDateFormat("MMM yy", Locale.US)
        val date = runCatching { parser.parse(monthKey) }.getOrNull() ?: return monthKey
        return formatter.format(date)
    }

    internal fun addMonthsToKey(monthKey: String, monthsToAdd: Int): String {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US).apply { isLenient = false }
        val cal = Calendar.getInstance(Locale.US)
        cal.time = runCatching { parser.parse(monthKey) }.getOrNull() ?: cal.time
        cal.add(Calendar.MONTH, monthsToAdd)
        return parser.format(cal.time)
    }
}
