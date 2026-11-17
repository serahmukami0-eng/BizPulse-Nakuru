package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.analytics.BarChartEntry
import com.example.data.analytics.ForecastScenario
import com.example.data.analytics.LabeledValuePoint
import com.example.data.analytics.Prompt4AnalyticsBundle
import com.example.data.analytics.Prompt4ForecastingEngine
import com.example.data.analytics.ResearchVariableComparison
import com.example.data.analytics.SmeFullAnalyticsReport
import com.example.data.model.AnalyticsDateFilter
import com.example.data.model.AppModule
import com.example.data.model.BusinessTransaction
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.ResearchFramework
import com.example.data.model.formatKes
import com.example.ui.components.AnalyticsDateFilterCard
import com.example.ui.components.CategoryComparisonBarChartCard
import com.example.ui.components.DemoDataModeBanner
import com.example.ui.components.EmptyModulePlaceholder
import com.example.ui.components.ErrorModulePlaceholder
import com.example.ui.components.LoadingModulePlaceholder
import com.example.ui.components.MethodologyNoticeCard
import com.example.ui.components.ModulePageHeader
import com.example.ui.components.PaymentMethodDonutChartCard
import com.example.ui.components.PerformanceTrendLineChartCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.VerticalGroupedBarChartCard
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandNavySoft
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleBright
import com.example.ui.theme.BrandPurpleDark
import com.example.ui.theme.BrandPurpleSoft
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealDark
import com.example.ui.theme.BrandTealLight
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberDark
import com.example.ui.theme.DemoAmberLight
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.UiState
import java.util.Locale

private enum class AnalyticsSectionFocus(val label: String) {
    ALL("All Analytics"),
    SEVEN_DASHBOARDS("7 Dashboards (Bar)"),
    FORECAST_12("12 Forecast Views"),
    PERFORMANCE("SME Performance"),
    MARKETING("Marketing (IV1)"),
    PAYMENTS("Payments (IV2)"),
    RECORDS("Records (IV3)"),
    COMMUNICATIONS("Comms (IV4)"),
    RELATIONSHIPS("Research Links")
}

@Composable
fun PerformanceAnalyticsScreen(
    summary: DashboardSummary,
    analyticsReport: SmeFullAnalyticsReport,
    transactionsState: UiState<List<BusinessTransaction>>,
    inventoryState: UiState<List<InventoryItem>>,
    campaignsState: UiState<List<DigitalMarketingCampaign>>,
    communicationsState: UiState<List<OnlineCommunicationLog>>,
    onSelectDateFilter: (AnalyticsDateFilter) -> Unit,
    onApplyCustomDateRange: (startIso: String, endIso: String) -> Unit,
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    prompt4Analytics: Prompt4AnalyticsBundle = Prompt4ForecastingEngine.compute(
        isDemoMode = summary.isDemoMode,
        allTransactions = emptyList(),
        inventory = emptyList(),
        customers = emptyList()
    ),
    forecastHorizonMonths: Int = 6,
    onSelectForecastHorizon: (Int) -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    onOpenReports: () -> Unit = {}
) {
    var focusSection by remember { mutableStateOf(AnalyticsSectionFocus.ALL) }
    var selectedScenario by remember { mutableStateOf(ForecastScenario.BASELINE) }

    val isLoading = !summary.isDemoMode && (
        transactionsState is UiState.Loading ||
            inventoryState is UiState.Loading ||
            campaignsState is UiState.Loading ||
            communicationsState is UiState.Loading
        )

    val firstError = listOfNotNull(
        (transactionsState as? UiState.Error)?.message,
        (inventoryState as? UiState.Error)?.message,
        (campaignsState as? UiState.Error)?.message,
        (communicationsState as? UiState.Error)?.message
    ).firstOrNull()

    val perf = analyticsReport.performance
    val mkt = analyticsReport.marketing
    val pay = analyticsReport.payments
    val rec = analyticsReport.recordKeeping
    val comm = analyticsReport.communication

    val hasAnyFilteredData =
        analyticsReport.filteredTransactions.isNotEmpty() ||
            analyticsReport.filteredCampaigns.isNotEmpty() ||
            analyticsReport.filteredCommunications.isNotEmpty() ||
            rec.inventoryCount > 0 ||
            prompt4Analytics.monthlyRows.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
        ) {
            ModulePageHeader(
                module = AppModule.PERFORMANCE_ANALYTICS,
                onBackToDashboard = onBackToDashboard,
                onOpenAllModules = onOpenAllModules
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DemoDataModeBanner(
                        isDemoMode = summary.isDemoMode,
                        hasLiveRecords = summary.hasAnyLiveRecords,
                        onToggleDemoMode = onToggleDemoMode
                    )
                }

                item {
                    SectionHeader(
                        title = "7 Executive Bar Dashboards, 12 Forecast Views & Research Analytics",
                        subtitle = if (summary.isDemoMode) {
                            "Sample DEMO DATA across IV1–IV4, Multi-Month Projections & Stock (${analyticsReport.activeWindow.label})"
                        } else {
                            "Calculated strictly from your live Firestore records (${analyticsReport.activeWindow.label})"
                        }
                    )
                }

                item {
                    AnalyticsDateFilterCard(
                        filterState = analyticsReport.filterState,
                        activeWindow = analyticsReport.activeWindow,
                        onSelectFilter = onSelectDateFilter,
                        onApplyCustomRange = onApplyCustomDateRange,
                        testTagPrefix = "analytics"
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnalyticsSectionFocus.entries.forEach { sec ->
                            FilterChip(
                                selected = focusSection == sec,
                                onClick = { focusSection = sec },
                                label = { Text(sec.label) },
                                modifier = Modifier.testTag("analytics_focus_${sec.name.lowercase()}")
                            )
                        }
                    }
                }

                if (firstError != null && !summary.isDemoMode) {
                    item { ErrorModulePlaceholder(firstError) }
                }

                if (isLoading) {
                    item { LoadingModulePlaceholder("Loading Cloud Firestore analytics...") }
                } else if (!hasAnyFilteredData && !summary.isDemoMode) {
                    item {
                        EmptyModulePlaceholder(
                            icon = Icons.Filled.Analytics,
                            title = "No business records in ${analyticsReport.filterState.filter.label}",
                            tip = "Record sales, expenses, digital payments, marketing campaigns, or customer enquiries—or widen the date range."
                        )
                    }
                } else {
                    // 7 Advanced Dashboards (Dear Lucy Style Bar Graphs)
                    if (focusSection == AnalyticsSectionFocus.ALL || focusSection == AnalyticsSectionFocus.SEVEN_DASHBOARDS) {
                        item {
                            SectionHeader(
                                title = "7 Executive Dashboards (Bar Visualizations)",
                                subtitle = "Interactive revenue, forecast, profitability, customer, channel & stock performance"
                            )
                        }
                        item {
                            VerticalGroupedBarChartCard(
                                title = "Dashboard 1 • Monthly Sales Performance (MoM %)",
                                subtitle = "Monthly billed sales revenue with month-over-month % growth badges",
                                entries = prompt4Analytics.monthlySalesBars,
                                primaryLabel = "Sales (KES)",
                                primaryColor = BrandPurple,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "Record dated sales across months to plot monthly revenue bars."
                            )
                        }
                        item {
                            val scenarioAdjustedBars = prompt4Analytics.projection.rows.map { row ->
                                val adjustedProjected = if (row.isProjection) {
                                    row.projectedSalesKes * selectedScenario.multiplier
                                } else {
                                    row.projectedSalesKes
                                }
                                BarChartEntry(
                                    label = row.monthLabel,
                                    primaryValue = row.actualSalesKes ?: 0.0,
                                    secondaryValue = adjustedProjected,
                                    badge = if (row.isProjection) "PROJ" else null
                                )
                            }
                            VerticalGroupedBarChartCard(
                                title = "Dashboard 2 • Actual vs. Projected Sales (${forecastHorizonMonths}M • ${selectedScenario.label})",
                                subtitle = "Historical sales vs OLS trendline (${prompt4Analytics.projection.historicalMonthsCount} mo base)",
                                entries = scenarioAdjustedBars,
                                primaryLabel = "Actual Sales (KES)",
                                primaryColor = BrandNavy,
                                secondaryLabel = "Projected / Fitted (KES)",
                                secondaryColor = BrandPurpleBright,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "Log dated sales to render Actual vs Projected comparisons."
                            )
                        }
                        item {
                            VerticalGroupedBarChartCard(
                                title = "Dashboard 3 • Revenue vs. Operating Expenses",
                                subtitle = "Monthly gross inflows against recorded operating outflows",
                                entries = prompt4Analytics.revenueVsExpenseBars,
                                primaryLabel = "Revenue (KES)",
                                primaryColor = BrandTeal,
                                secondaryLabel = "Expenses (KES)",
                                secondaryColor = AlertCoral,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "No monthly revenue & expense pairs logged yet."
                            )
                        }
                        item {
                            VerticalGroupedBarChartCard(
                                title = "Dashboard 4 • Profitability Trends (Gross vs. Net Profit)",
                                subtitle = if (prompt4Analytics.hasCostDataForGrossProfit) {
                                    "Monthly Gross Profit (Sales - COGS) alongside Net Profit (Sales - Expenses)"
                                } else {
                                    "Add SKU unit cost to compute Gross Profit cleanly; currently comparing Sales vs. Net Profit"
                                },
                                entries = prompt4Analytics.profitabilityBars,
                                primaryLabel = if (prompt4Analytics.hasCostDataForGrossProfit) "Gross Profit (KES)" else "Total Sales (KES)",
                                primaryColor = BrandPurpleDark,
                                secondaryLabel = "Net Profit (KES)",
                                secondaryColor = BrandTealDark,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "Log dated transactions to evaluate monthly profitability."
                            )
                        }
                        item {
                            VerticalGroupedBarChartCard(
                                title = "Dashboard 5 • Customer Growth Over Time",
                                subtitle = "Monthly newly registered customers vs. cumulative customer base",
                                entries = prompt4Analytics.customerGrowthBars,
                                primaryLabel = "New Customers",
                                primaryColor = BrandTeal,
                                secondaryLabel = "Cumulative Total",
                                secondaryColor = BrandPurple,
                                valueFormatter = { "${it.toInt()}" },
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "Register customers in Module H to plot acquisition growth."
                            )
                        }
                        item {
                            VerticalGroupedBarChartCard(
                                title = "Dashboard 6 • Digital Payment Method Comparison (KES)",
                                subtitle = "M-Pesa, Bank Transfers, Card, Other Digital & Cash volume across active records",
                                entries = prompt4Analytics.paymentMethodBars,
                                primaryLabel = "Volume (KES)",
                                primaryColor = BrandTealDark,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "No active transactions available for channel comparison."
                            )
                        }
                        item {
                            VerticalGroupedBarChartCard(
                                title = "Dashboard 7 • Inventory Stock vs. Reorder Threshold",
                                subtitle = "Units currently in stock vs. configured reorder alert level",
                                entries = prompt4Analytics.inventoryLevelBars,
                                primaryLabel = "Stock Qty",
                                primaryColor = BrandNavy,
                                secondaryLabel = "Reorder Level",
                                secondaryColor = DemoAmber,
                                valueFormatter = { "${it.toInt()} u" },
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "Add products in Inventory (IV3) to monitor stock bars."
                            )
                        }
                    }

                    // Top 12 Financial Forecasting Features + Real Views
                    if (focusSection == AnalyticsSectionFocus.ALL || focusSection == AnalyticsSectionFocus.FORECAST_12) {
                        item {
                            FinancialForecastingSuiteSection(
                                summary = summary,
                                bundle = prompt4Analytics,
                                forecastHorizonMonths = forecastHorizonMonths,
                                selectedScenario = selectedScenario,
                                onSelectHorizon = onSelectForecastHorizon,
                                onSelectScenario = { selectedScenario = it },
                                onOpenAiAssistant = onOpenAiAssistant,
                                onOpenReports = onOpenReports
                            )
                        }
                    }

                    if (focusSection == AnalyticsSectionFocus.ALL || focusSection == AnalyticsSectionFocus.PERFORMANCE) {
                        item {
                            SmePerformanceSectionCard(
                                report = analyticsReport,
                                isDemo = summary.isDemoMode
                            )
                        }
                        item {
                            PerformanceTrendLineChartCard(
                                title = "Estimated Net Profit Trend (KES)",
                                subtitle = "Estimated Net Profit = Recorded Sales - Recorded Expenses (${analyticsReport.filterState.filter.label})",
                                points = perf.profitabilityTrend,
                                primaryLabel = "Estimated Net Profit (KES)",
                                primaryColor = BrandNavy,
                                secondaryLabel = "Sales Revenue (KES)",
                                secondaryColor = BrandTeal,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "Log dated sales or expenses to view profitability trends."
                            )
                        }
                    }

                    if (focusSection == AnalyticsSectionFocus.ALL || focusSection == AnalyticsSectionFocus.MARKETING) {
                        item {
                            MarketingAnalyticsCard(report = analyticsReport)
                        }
                        item {
                            CategoryComparisonBarChartCard(
                                title = "Campaign Performance Comparison (Leads Generated)",
                                subtitle = "Top campaigns in ${analyticsReport.filterState.filter.label}",
                                bars = mkt.campaignComparisons.map { c ->
                                    LabeledValuePoint(
                                        label = "${c.title} (${c.channel})",
                                        value = c.inquiries.toDouble()
                                    )
                                },
                                barColor = BrandNavy,
                                valueFormatter = { "${it.toInt()} leads" },
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "No digital marketing campaigns recorded in this period."
                            )
                        }
                        item {
                            PerformanceTrendLineChartCard(
                                title = "Marketing Activity Over Time",
                                subtitle = "Leads generated vs. KES marketing spend by campaign date",
                                points = mkt.trendPoints,
                                primaryLabel = "Leads Generated",
                                primaryColor = BrandTeal,
                                secondaryLabel = "Spend (KES)",
                                secondaryColor = DemoAmber,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "No dated campaigns in this range to plot."
                            )
                        }
                    }

                    if (focusSection == AnalyticsSectionFocus.ALL || focusSection == AnalyticsSectionFocus.PAYMENTS) {
                        item {
                            PaymentAnalyticsCard(report = analyticsReport)
                        }
                        item {
                            PaymentMethodDonutChartCard(
                                title = "Payment Method Usage Breakdown (KES)",
                                subtitle = "Distribution across M-Pesa, Bank Transfers, Cards, Other Digital & Cash",
                                breakdown = pay.methodBreakdown,
                                isDemoData = summary.isDemoMode
                            )
                        }
                        item {
                            PerformanceTrendLineChartCard(
                                title = "Digital vs. Cash Payment Trend (KES)",
                                subtitle = "Recorded digital settlements compared against cash entries over time",
                                points = pay.trendPoints,
                                primaryLabel = "Digital Payments (KES)",
                                primaryColor = BrandTeal,
                                secondaryLabel = "Cash Payments (KES)",
                                secondaryColor = AlertCoral,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "No payment transactions in this period."
                            )
                        }
                    }

                    if (focusSection == AnalyticsSectionFocus.ALL || focusSection == AnalyticsSectionFocus.RECORDS) {
                        item {
                            RecordKeepingAnalyticsCard(report = analyticsReport)
                        }
                        item {
                            PerformanceTrendLineChartCard(
                                title = "Computerized Revenue & Expense Trends (KES)",
                                subtitle = "Recorded sales inflows vs. operating expenses by transaction date",
                                points = rec.revenueExpenseTrend.map { pt ->
                                    LabeledValuePoint(pt.dayLabel, pt.salesKes, pt.expensesKes)
                                },
                                primaryLabel = "Recorded Sales (KES)",
                                primaryColor = BrandTeal,
                                secondaryLabel = "Recorded Expenses (KES)",
                                secondaryColor = AlertCoral,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "No sales or expense records in this date window."
                            )
                        }
                    }

                    if (focusSection == AnalyticsSectionFocus.ALL || focusSection == AnalyticsSectionFocus.COMMUNICATIONS) {
                        item {
                            OnlineCommunicationAnalyticsCard(report = analyticsReport)
                        }
                        item {
                            CategoryComparisonBarChartCard(
                                title = "Customer Enquiries by Communication Channel",
                                subtitle = "Volume across WhatsApp, Instagram, Facebook, Email, SMS, Website & Other",
                                bars = comm.enquiriesByChannel.map { ch ->
                                    LabeledValuePoint(
                                        label = "${ch.channel} (${ch.respondedCount} answered)",
                                        value = ch.totalCount.toDouble()
                                    )
                                },
                                barColor = BrandTealDark,
                                valueFormatter = { "${it.toInt()} chats" },
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "No customer enquiries recorded in this period."
                            )
                        }
                        item {
                            PerformanceTrendLineChartCard(
                                title = "Communication Activity Over Time",
                                subtitle = "Enquiries received vs. answered by date",
                                points = comm.trendPoints,
                                primaryLabel = "Total Enquiries",
                                primaryColor = BrandNavy,
                                secondaryLabel = "Responded / Converted",
                                secondaryColor = BrandTeal,
                                isDemoData = summary.isDemoMode,
                                emptyMessage = "No customer communication logs in this date range."
                            )
                        }
                    }

                    if (focusSection == AnalyticsSectionFocus.ALL || focusSection == AnalyticsSectionFocus.RELATIONSHIPS) {
                        item {
                            ResearchRelationshipSectionCard(
                                comparisons = analyticsReport.researchComparisons,
                                dataGaps = analyticsReport.dataGapsAndLimitations,
                                isDemoData = summary.isDemoMode
                            )
                        }
                    }
                }

                item {
                    MethodologyNoticeCard(
                        title = "Methodological Distinction: Observed Association vs. Causation",
                        body = ResearchFramework.METHODOLOGY_NOTE +
                            if (summary.isDemoMode) " Note: Currently displaying clearly labeled DEMO DATA, not field research results." else ""
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinancialForecastingSuiteSection(
    summary: DashboardSummary,
    bundle: Prompt4AnalyticsBundle,
    forecastHorizonMonths: Int,
    selectedScenario: ForecastScenario,
    onSelectHorizon: (Int) -> Unit,
    onSelectScenario: (ForecastScenario) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenReports: () -> Unit
) {
    val fm = bundle.financialMetrics
    val proj = bundle.projection
    val futureRows = proj.rows.filter { it.isProjection }
    val cumulativeProjectedTotal = futureRows.sumOf { it.projectedSalesKes * selectedScenario.multiplier }
    val grossMarginPct = if (bundle.hasCostDataForGrossProfit && bundle.totalGrossProfitKes != null && summary.totalSalesKes > 0.0) {
        (bundle.totalGrossProfitKes / summary.totalSalesKes) * 100.0
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Financial Forecasting & Unit Economics Suite (12 Real Views)",
            subtitle = "Rolling OLS multi-horizon forecasts, scenario planning, break-even coverage, burn rate & working capital"
        )

        // 1 & 2. Horizon Selector (3M / 6M / 12M) + What-If Scenario Planning
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1–2. Rolling Forecast Horizon & What-If Scenario",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (summary.isDemoMode) DemoBadgeChip()
                }

                Text("Forecast Horizon", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 6, 12).forEach { months ->
                        FilterChip(
                            selected = forecastHorizonMonths == months,
                            onClick = { onSelectHorizon(months) },
                            label = { Text("$months Months") },
                            modifier = Modifier.testTag("forecast_horizon_$months")
                        )
                    }
                }

                Text("What-If Sensitivity Scenario", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ForecastScenario.entries.forEach { sc ->
                        FilterChip(
                            selected = selectedScenario == sc,
                            onClick = { onSelectScenario(sc) },
                            label = { Text(sc.label) },
                            modifier = Modifier.testTag("forecast_scenario_${sc.name.lowercase()}")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandPurpleSoft
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            text = "${forecastHorizonMonths}M Cumulative Sales PROJECTION (${selectedScenario.label})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurpleDark
                        )
                        Text(
                            text = formatKes(cumulativeProjectedTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPurple
                        )
                        Text(
                            text = "OLS Slope: ${if (proj.slopeKesPerMonth >= 0) "+" else ""}${formatKes(proj.slopeKesPerMonth)}/mo • Anchor: ${proj.historicalMonthsCount} historical month(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandNavy
                        )
                    }
                }

                if (!proj.hasSufficientHistory && proj.warningMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DemoAmberLight,
                        border = BorderStroke(1.dp, DemoAmber)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = DemoAmberDark, modifier = Modifier.size(18.dp))
                            Text(
                                text = proj.warningMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = DemoAmberDark
                            )
                        }
                    }
                }
            }
        }

        // 3–6. Cashflow, Burn Rate, Break-Even, Accuracy, Unit Economics & Working Capital
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "3–6. Cash Flow Burn, Break-Even, Accuracy & Unit Economics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AnalyticsRow("Avg Monthly Sales Inflow", formatKes(fm.avgMonthlySalesKes), BrandTeal)
                AnalyticsRow("Avg Monthly Expense Burn", formatKes(fm.avgMonthlyExpensesKes), AlertCoral)
                AnalyticsRow(
                    "Avg Monthly Net Cashflow",
                    formatKes(fm.avgMonthlyNetCashflowKes),
                    if (fm.avgMonthlyNetCashflowKes >= 0) BrandNavy else AlertCoral
                )
                AnalyticsRow(
                    "Break-Even Monthly Revenue Target",
                    "${formatKes(fm.breakEvenMonthlyRevenueKes)} (${fm.breakEvenCoveragePct?.let { "${it.toInt()}% covered" } ?: "N/A"})",
                    if ((fm.breakEvenCoveragePct ?: 0.0) >= 100.0) BrandTealDark else DemoAmberDark
                )
                AnalyticsRow(
                    "Historical OLS Fit Error (MAPE)",
                    fm.historicalFitErrorPct?.let { "${String.format(Locale.US, "%.1f%%", it)} avg variance" }
                        ?: "Need >= 2 months to measure fit",
                    BrandPurple
                )
                AnalyticsRow(
                    "Gross Profit & Margin %",
                    if (bundle.hasCostDataForGrossProfit && bundle.totalGrossProfitKes != null) {
                        "${formatKes(bundle.totalGrossProfitKes)} (${String.format(Locale.US, "%.1f%%", grossMarginPct ?: 0.0)})"
                    } else {
                        "Add SKU unit cost to compute Gross Profit"
                    },
                    BrandPurpleDark
                )
                AnalyticsRow(
                    "Tracked COGS (Sold Goods Cost)",
                    if (bundle.hasCostDataForGrossProfit) formatKes(bundle.totalCogsKes) else "Cost data missing",
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 7–10. Expense Drivers, Payables, Customer LTV & Inventory Capital Risk
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "7–10. Expense Drivers, Payables, Customer LTV & Stock Risk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AnalyticsRow("Pending Supplier Payables", formatKes(summary.pendingPayablesKes), AlertCoral)
                AnalyticsRow("Pending Customer Payments", formatKes(summary.pendingTransactionsKes), DemoAmberDark)
                AnalyticsRow(
                    "Customer Avg Spend / Repeat Rate",
                    "${fm.avgCustomerSpendKes?.let { formatKes(it) } ?: "N/A"} • ${bundle.customerRepeatRatePct?.let { "${it.toInt()}% repeat" } ?: "0%"}",
                    BrandTealDark
                )
                AnalyticsRow(
                    "Inventory Retail vs Cost Capital",
                    "${formatKes(bundle.totalInventoryRetailValueKes)} retail / ${formatKes(bundle.totalInventoryCostValueKes)} cost",
                    BrandNavy
                )
                AnalyticsRow(
                    "Low-Stock Reorder Risk SKUs",
                    "${summary.lowStockCount} alert(s) across ${summary.inventoryCount} SKUs",
                    if (summary.lowStockCount > 0) AlertCoral else BrandTeal
                )

                if (fm.topExpenseDrivers.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Top Operating Expense Cost Drivers", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    fm.topExpenseDrivers.forEach { driver ->
                        ShareBarRow(
                            label = driver.category,
                            valueLabel = "${formatKes(driver.amountKes)} (${driver.sharePct.toInt()}%)",
                            progress = (driver.sharePct / 100.0).toFloat(),
                            color = AlertCoral
                        )
                    }
                }
            }
        }

        // 11–12. Historical + PROJECTION Revenue Table & AI Briefing / Export Actions
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "11–12. Monthly Projection Ledger, AI Narrative & Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (proj.rows.isEmpty()) {
                    Text(
                        text = "No sales data recorded yet to build a monthly projection schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Month", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Actual", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Projected (${selectedScenario.name.take(4)})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                    }
                    HorizontalDivider()
                    proj.rows.forEach { row ->
                        val scenarioVal = if (row.isProjection) row.projectedSalesKes * selectedScenario.multiplier else row.projectedSalesKes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(row.monthLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                if (row.isProjection) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = BrandPurpleSoft) {
                                        Text(
                                            text = "PROJ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandPurpleDark,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = row.actualSalesKes?.let { formatKes(it) } ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatKes(scenarioVal),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (row.isProjection) FontWeight.Bold else FontWeight.Normal,
                                color = if (row.isProjection) BrandPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }

                Text(
                    text = proj.methodologyNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenAiAssistant,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("forecast_ai_briefing_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Forecast Briefing")
                        }
                        OutlinedButton(
                            onClick = onOpenReports,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("forecast_export_reports_button")
                        ) {
                            Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV / PDF Reports")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmePerformanceSectionCard(
    report: SmeFullAnalyticsReport,
    isDemo: Boolean
) {
    val perf = report.performance
    val salesGrowthText = perf.salesGrowthPct?.let {
        "${if (it >= 0) "+" else ""}${String.format(Locale.US, "%.1f%%", it)}"
    } ?: "Insufficient prior data"
    val customerGrowthText = perf.customerGrowthPct?.let {
        "${if (it >= 0) "+" else ""}${String.format(Locale.US, "%.1f%%", it)}"
    } ?: "Insufficient prior data"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6. SME Performance Overview (Dependent Variable)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isDemo) {
                    DemoBadgeChip()
                }
            }

            Text(
                text = perf.overviewSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SubBlockTitle("A. Sales Growth")
            AnalyticsRow("Current Period Sales", formatKes(perf.currentSalesKes), BrandTeal)
            AnalyticsRow(
                "Previous Period Sales",
                perf.previousSalesKes?.let { formatKes(it) } ?: "Not available",
                MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnalyticsRow(
                "Sales Growth %",
                salesGrowthText,
                if ((perf.salesGrowthPct ?: 0.0) >= 0) BrandTealDark else AlertCoral
            )
            Text(perf.salesGrowthNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SubBlockTitle("B. Customer Growth")
            AnalyticsRow(
                "Active Customers Logged",
                if (perf.currentCustomerCount > 0) "${perf.currentCustomerCount} customer(s)" else "None logged",
                BrandNavy
            )
            AnalyticsRow("Customer Growth %", customerGrowthText, BrandTealDark)
            Text(perf.customerGrowthNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SubBlockTitle("C. Operational Efficiency")
            AnalyticsRow("Recorded Transaction Activity", "${perf.recordedTransactionsCount} entries", BrandNavy)
            AnalyticsRow(
                "Record Completeness Score",
                perf.recordCompletenessPct?.let { "$it%" } ?: "N/A",
                BrandTealDark
            )
            AnalyticsRow(
                "Enquiry Response Rate",
                perf.enquiryResponseRatePct?.let { "${String.format(Locale.US, "%.0f%%", it)}" } ?: "N/A (no comms)",
                BrandTeal
            )

            SubBlockTitle("D. Estimated Profitability (Sales - Expenses)")
            AnalyticsRow(
                "Estimated Net Profit",
                formatKes(perf.estimatedNetProfitKes),
                if (perf.estimatedNetProfitKes >= 0) BrandNavy else AlertCoral
            )
            AnalyticsRow(
                "Estimated Net Margin",
                perf.netMarginPct?.let { "${String.format(Locale.US, "%.1f%%", it)}" } ?: "N/A (0 sales)",
                DemoAmberDark
            )
        }
    }
}

@Composable
private fun MarketingAnalyticsCard(report: SmeFullAnalyticsReport) {
    val mkt = report.marketing
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Digital Marketing Analytics (IV1)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (report.isDemoMode) DemoBadgeChip()
            }
            AnalyticsRow("Campaigns in Period", "${mkt.campaignCount}", BrandNavy)
            AnalyticsRow("Marketing Expenditure", formatKes(mkt.totalSpendKes), AlertCoral)
            AnalyticsRow("Total Campaign Reach", "${mkt.totalReach}", BrandTeal)
            AnalyticsRow(
                "Customer Engagement",
                "${mkt.totalEngagement}${mkt.avgEngagementRatePct?.let { " (${String.format(Locale.US, "%.1f%%", it)})" } ?: ""}",
                BrandTealDark
            )
            AnalyticsRow("Enquiries Generated", "${mkt.totalInquiries}", BrandNavy)
            AnalyticsRow(
                "Cost per Enquiry",
                mkt.costPerInquiryKes?.let { formatKes(it, includeDecimals = true) } ?: "Insufficient data (0 enquiries)",
                DemoAmberDark
            )
        }
    }
}

@Composable
private fun PaymentAnalyticsCard(report: SmeFullAnalyticsReport) {
    val pay = report.payments
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3. Digital Payment Analytics (IV2)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (report.isDemoMode) DemoBadgeChip()
            }
            AnalyticsRow("Total Digital Payments", formatKes(pay.totalDigitalPaymentsKes), BrandTeal)
            AnalyticsRow("Digital Txns / All Txns", "${pay.digitalTransactionCount} / ${pay.totalTransactionCount} (${pay.digitalAdoptionPct ?: 0}%)", BrandNavy)
            AnalyticsRow(
                "Digital Sales % of Recorded Sales",
                pay.digitalAsPercentOfSales?.let { "${String.format(Locale.US, "%.1f%%", it)} (${formatKes(pay.digitalSalesKes)} / ${formatKes(pay.totalRecordedSalesKes)})" }
                    ?: "Insufficient sales data",
                BrandTealDark
            )
            AnalyticsRow("Cash Sales Recorded", formatKes(pay.cashSalesKes), AlertCoral)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Payment Provider Notice: M-Pesa, Bank Transfer, and Card figures are calculated strictly from manually logged business entries. Direct Daraja STK-Push or bank webhook reconciliation is not configured.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordKeepingAnalyticsCard(report: SmeFullAnalyticsReport) {
    val rec = report.recordKeeping
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "4. Computerized Record Keeping Analytics (IV3)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (report.isDemoMode) DemoBadgeChip()
            }
            AnalyticsRow("Recorded Sales (${rec.salesCount})", formatKes(rec.recordedSalesKes), BrandTeal)
            AnalyticsRow("Recorded Expenses (${rec.expenseCount})", formatKes(rec.recordedExpensesKes), AlertCoral)
            AnalyticsRow("Other Business Records (${rec.otherCount})", formatKes(rec.otherRecordsKes), BrandNavy)
            AnalyticsRow(
                "Estimated Net Profit (Sales - Expenses)",
                formatKes(rec.estimatedNetProfitKes),
                if (rec.estimatedNetProfitKes >= 0) BrandNavy else AlertCoral
            )
            AnalyticsRow("Total Recorded Transactions", "${rec.totalTransactionsCount}", BrandNavy)
            AnalyticsRow(
                "Record Completeness",
                rec.recordCompletenessPct?.let { "$it%" } ?: "N/A",
                BrandTealDark
            )
            Text(
                text = rec.completenessDetails + " Note: Estimated Net Profit is derived solely from user-recorded entries and is not a verified audited accounting statement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnlineCommunicationAnalyticsCard(report: SmeFullAnalyticsReport) {
    val comm = report.communication
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "5. Online Communication Analytics (IV4)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (report.isDemoMode) DemoBadgeChip()
            }
            AnalyticsRow("Total Customer Enquiries", "${comm.totalEnquiries}", BrandNavy)
            AnalyticsRow("Responded Enquiries (incl. Converted)", "${comm.respondedEnquiries}", BrandTeal)
            AnalyticsRow("Pending Enquiries", "${comm.pendingEnquiries}", if (comm.pendingEnquiries > 0) DemoAmberDark else BrandTealDark)
            AnalyticsRow(
                "Response Rate",
                comm.responseRatePct?.let { "${String.format(Locale.US, "%.1f%%", it)}" } ?: "Insufficient data (0 enquiries)",
                BrandTealDark
            )
            AnalyticsRow(
                "Avg Response Time",
                comm.avgResponseMinutes?.let { "$it min" } ?: "N/A",
                BrandNavy
            )
        }
    }
}

@Composable
private fun ResearchRelationshipSectionCard(
    comparisons: List<ResearchVariableComparison>,
    dataGaps: List<String>,
    isDemoData: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7. Digital Tech Adoption vs. SME Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isDemoData) DemoBadgeChip()
                }
                Text(
                    text = "Descriptive comparison & Pearson correlation (computed strictly when >= 3 valid paired daily observations exist). Observed correlation never establishes causation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                comparisons.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(6.dp), color = BrandNavySoft) {
                                    Text(
                                        text = item.variableCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandNavy,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = item.correlationR?.let {
                                        "r = ${String.format(Locale.US, "%.2f", it)} (n=${item.pairedObservationsCount} days)"
                                    } ?: "n=${item.pairedObservationsCount} paired days",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.correlationR != null) BrandTealDark else DemoAmberDark
                                )
                            }
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Adoption Indicator: ${item.ivMetricSummary}", style = MaterialTheme.typography.bodySmall)
                            Text("Performance Indicator: ${item.dvMetricSummary}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = item.interpretation,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            item.limitationNote?.let { note ->
                                Text(
                                    text = "Limitation: $note",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DemoAmberDark
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dataGaps.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DemoAmberLight.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, DemoAmber)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = DemoAmberDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Identified Data Gaps & Analysis Limitations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DemoAmberDark
                        )
                    }
                    dataGaps.forEach { gap ->
                        Text(
                            text = "• $gap",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoBadgeChip() {
    Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
        Text(
            text = "DEMO DATA",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = DemoAmberDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SubBlockTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = BrandPurple,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun AnalyticsRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun ShareBarRow(
    label: String,
    valueLabel: String,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
