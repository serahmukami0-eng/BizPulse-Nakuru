package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.analytics.LabeledValuePoint
import com.example.data.analytics.Prompt4AnalyticsBundle
import com.example.data.analytics.Prompt4ForecastingEngine
import com.example.data.analytics.SmeFullAnalyticsReport
import com.example.data.model.AnalyticsDateFilter
import com.example.data.model.AppModule
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.ResearchFramework
import com.example.data.model.formatKes
import com.example.data.repository.RoomBusinessOverviewState
import com.example.ui.components.AnalyticsDateFilterCard
import com.example.ui.components.DemoDataModeBanner
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.ModuleTileCard
import com.example.ui.components.PaymentMethodDonutChartCard
import com.example.ui.components.PerformanceTrendLineChartCard
import com.example.ui.components.ProjectXBrandMark
import com.example.ui.components.SectionHeader
import com.example.ui.components.VerticalGroupedBarChartCard
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandNavyDeep
import com.example.ui.theme.BrandNavyMid
import com.example.ui.theme.BrandNavySoft
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleBright
import com.example.ui.theme.BrandPurpleSoft
import com.example.ui.theme.BrandSlateSilver
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealBright
import com.example.ui.theme.BrandTealDark
import com.example.ui.theme.BrandTealLight
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberDark
import com.example.ui.theme.DemoAmberLight
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.SalesOverviewAiState
import com.example.ui.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessDashboardScreen(
    summary: DashboardSummary,
    analyticsReport: SmeFullAnalyticsReport,
    profileState: UiState<BusinessProfile?>,
    transactionsState: UiState<List<BusinessTransaction>>,
    onSelectDateFilter: (AnalyticsDateFilter) -> Unit,
    onApplyCustomDateRange: (startIso: String, endIso: String) -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onOpenModule: (AppModule) -> Unit,
    onOpenQuickSale: () -> Unit,
    onOpenQuickExpense: () -> Unit,
    onOpenProfileSetup: () -> Unit,
    roomOverviewState: RoomBusinessOverviewState = RoomBusinessOverviewState(),
    onRefreshRoomCache: () -> Unit = {},
    salesAiState: SalesOverviewAiState = SalesOverviewAiState.Idle,
    onAnalyzeSalesWithAi: () -> Unit = {},
    onClearSalesAiSummary: () -> Unit = {},
    prompt4Analytics: Prompt4AnalyticsBundle = Prompt4ForecastingEngine.compute(
        isDemoMode = summary.isDemoMode,
        allTransactions = emptyList(),
        inventory = emptyList(),
        customers = emptyList()
    )
) {
    val profileMissing = profileState is UiState.Success && profileState.data == null
    val perf = analyticsReport.performance
    val mkt = analyticsReport.marketing
    val pay = analyticsReport.payments
    val rec = analyticsReport.recordKeeping
    val comm = analyticsReport.communication

    val revenueExpenseLinePoints = rec.revenueExpenseTrend.map { pt ->
        LabeledValuePoint(
            label = pt.dayLabel,
            value = pt.salesKes,
            secondaryValue = pt.expensesKes
        )
    }
    val recentFeed = analyticsReport.filteredTransactions.take(5)

    val customerGrowthDisplay = when {
        perf.customerGrowthPct != null -> {
            val prefix = if (perf.customerGrowthPct >= 0) "+" else ""
            "$prefix${String.format(Locale.US, "%.0f%%", perf.customerGrowthPct)}"
        }
        summary.customerCount > 0 -> "${summary.customerCount} tracked"
        perf.currentCustomerCount > 0 -> "${perf.currentCustomerCount} active"
        else -> "N/A"
    }
    val customerGrowthSubtitle = when {
        summary.customerCount > 0 -> "${summary.repeatCustomerCount} repeat • ${summary.customerCount} total in CRM"
        perf.customerGrowthPct != null -> "${perf.previousCustomerCount} → ${perf.currentCustomerCount} unique customers"
        perf.currentCustomerCount > 0 -> "Need prior-period data for % growth"
        else -> "Log sales or customers to track"
    }

    val nextProjectionRow = prompt4Analytics.projection.rows.firstOrNull { it.isProjection }
    val momBadgeText = summary.latestMoMSalesGrowthPct?.let { pct ->
        val sign = if (pct >= 0) "+" else ""
        "MoM $sign${String.format(Locale.US, "%.1f%%", pct)}"
    } ?: "MoM Pending"
    val totalOutstandingKes = summary.pendingTransactionsKes + summary.pendingPayablesKes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProjectXBrandMark(compact = true, showTagline = true)
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = summary.locationNakuru,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Next-Gen Dark Navy/Black + Purple Accent AI Forecasting Hero (MindInventory UI Aesthetic)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.2.dp, BrandPurple.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(BrandNavyDeep, BrandNavy, BrandNavyMid)
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandPurple.copy(alpha = 0.28f),
                                border = BorderStroke(1.dp, BrandSlateSilver.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "PROJECT X – SME MANAGEMENT SYSTEM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPurpleBright,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandTeal.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = momBadgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTealBright,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = summary.businessName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${summary.businessType} • ${summary.businessSize} • Multi-Outlet POS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandSlateSilver
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = BrandPurpleBright,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Next-Month Trajectory & Scenario Strip
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.07f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (nextProjectionRow != null) {
                                            "PROJECTION (${nextProjectionRow.monthLabel}) • ${prompt4Analytics.horizonMonths}M OLS"
                                        } else {
                                            "AI TRAJECTORY (${prompt4Analytics.projection.historicalMonthsCount}/3 mo)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPurpleBright
                                    )
                                    Text(
                                        text = nextProjectionRow?.let { formatKes(it.projectedSalesKes) }
                                            ?: "Log sales across months to unlock forecast",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (prompt4Analytics.projection.hasSufficientHistory) {
                                            val slope = prompt4Analytics.projection.slopeKesPerMonth
                                            "Slope: ${if (slope >= 0) "+" else ""}${formatKes(slope)}/mo (${prompt4Analytics.projection.historicalMonthsCount} mo history)"
                                        } else {
                                            "Preliminary estimate (< 3 months recorded)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandSlateSilver
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onOpenModule(AppModule.PERFORMANCE_ANALYTICS) },
                                    border = BorderStroke(1.dp, BrandPurpleBright),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("hero_open_forecast_button")
                                ) {
                                    Text(
                                        text = "Forecast View",
                                        color = BrandPurpleBright,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenQuickSale,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPurple,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("dashboard_quick_sale_button")
                            ) {
                                Icon(Icons.Filled.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("POS Sale", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = onOpenQuickExpense,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("dashboard_quick_expense_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Expense", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { onOpenModule(AppModule.PERFORMANCE_ANALYTICS) },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("dashboard_analytics_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("7 Dashboards", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { onOpenModule(AppModule.AI_ASSISTANT) },
                                border = BorderStroke(1.dp, BrandTealBright),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("dashboard_ai_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrandTealBright, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Intel", color = BrandTealBright, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (profileMissing) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, BrandTeal)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Complete Your Business Profile",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Save your Nakuru SME name, size & sector to Cloud Firestore.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onOpenProfileSetup,
                                modifier = Modifier.testTag("complete_profile_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Set Up")
                            }
                        }
                    }
                }
            }

            if (transactionsState is UiState.Error) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Could not sync records: ${transactionsState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item {
                DemoDataModeBanner(
                    isDemoMode = summary.isDemoMode,
                    hasLiveRecords = summary.hasAnyLiveRecords,
                    onToggleDemoMode = onToggleDemoMode
                )
            }

            item {
                RoomBusinessOverviewSection(
                    overviewState = roomOverviewState,
                    onRefreshRoomCache = onRefreshRoomCache,
                    onOpenQuickSale = onOpenQuickSale,
                    onOpenQuickExpense = onOpenQuickExpense,
                    onOpenModule = onOpenModule,
                    salesAiState = salesAiState,
                    onAnalyzeSalesWithAi = onAnalyzeSalesWithAi,
                    onClearSalesAiSummary = onClearSalesAiSummary
                )
            }

            item {
                AnalyticsDateFilterCard(
                    filterState = analyticsReport.filterState,
                    activeWindow = analyticsReport.activeWindow,
                    onSelectFilter = onSelectDateFilter,
                    onApplyCustomRange = onApplyCustomDateRange,
                    testTagPrefix = "dashboard"
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RESEARCH CONCEPTUAL FRAMEWORK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandTealDark
                        )
                        Text(
                            text = ResearchFramework.RESEARCH_TITLE,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ResearchFramework.independentVariables.forEach { iv ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrandNavySoft
                                ) {
                                    Text(
                                        text = iv,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandNavy,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandTealLight
                            ) {
                                Text(
                                    text = "→ DV: ${ResearchFramework.DEPENDENT_VARIABLE}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTealDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Comprehensive Prompt 4 Executive KPI Grid
            item {
                val grossProfitValue = if (summary.hasCostDataForGrossProfit && summary.grossProfitKes != null) {
                    formatKes(summary.grossProfitKes)
                } else {
                    "Cost data needed"
                }
                val grossProfitSubtitle = if (summary.hasCostDataForGrossProfit) {
                    "All-time Sales - Tracked COGS"
                } else {
                    "Add SKU unit cost to compute"
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 700.dp
                    if (isWide) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Total Sales Revenue",
                                    valueText = formatKes(rec.recordedSalesKes),
                                    subtitle = "${rec.salesCount} sales • $momBadgeText",
                                    icon = Icons.Filled.PointOfSale,
                                    accentColor = BrandTeal,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                                )
                                KpiMetricCard(
                                    title = "Total Expenses",
                                    valueText = formatKes(rec.recordedExpensesKes),
                                    subtitle = "${rec.expenseCount} expense entries",
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                    accentColor = AlertCoral,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                                )
                                KpiMetricCard(
                                    title = "Estimated Net Profit",
                                    valueText = formatKes(rec.estimatedNetProfitKes),
                                    subtitle = "Gross: $grossProfitValue",
                                    icon = Icons.Filled.AccountBalanceWallet,
                                    accentColor = if (rec.estimatedNetProfitKes >= 0) BrandNavy else AlertCoral,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.PERFORMANCE_ANALYTICS) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Customers & Retention",
                                    valueText = "${summary.customerCount} ($customerGrowthDisplay)",
                                    subtitle = customerGrowthSubtitle,
                                    icon = Icons.Filled.Groups,
                                    accentColor = BrandPurple,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.CUSTOMERS) }
                                )
                                KpiMetricCard(
                                    title = "Products & Low Stock",
                                    valueText = "${summary.inventoryCount} SKUs • ${summary.lowStockCount} low",
                                    subtitle = "Retail value: ${formatKes(summary.totalInventoryRetailValueKes)}",
                                    icon = Icons.Filled.Inventory2,
                                    accentColor = if (summary.lowStockCount > 0) DemoAmber else BrandTeal,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                                )
                                KpiMetricCard(
                                    title = "Outstanding Payments",
                                    valueText = formatKes(totalOutstandingKes),
                                    subtitle = "${summary.pendingTransactionsCount} sales • ${summary.pendingPayablesCount} supplier bills",
                                    icon = Icons.Filled.LocalShipping,
                                    accentColor = if (totalOutstandingKes > 0) AlertCoral else BrandTealDark,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.SUPPLIERS) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Digital Payments",
                                    valueText = formatKes(pay.totalDigitalPaymentsKes),
                                    subtitle = "${pay.digitalTransactionCount} digital txns • ${pay.digitalAdoptionPct ?: 0}% share",
                                    icon = Icons.Filled.Payments,
                                    accentColor = BrandTeal,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.DIGITAL_PAYMENTS) }
                                )
                                KpiMetricCard(
                                    title = "Marketing Spend & Leads",
                                    valueText = formatKes(mkt.totalSpendKes),
                                    subtitle = "${mkt.campaignCount} campaigns • ${mkt.totalReach} reach",
                                    icon = Icons.Filled.Campaign,
                                    accentColor = BrandNavy,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.DIGITAL_MARKETING) }
                                )
                                KpiMetricCard(
                                    title = "Online Comms (IV4)",
                                    valueText = "${comm.totalEnquiries} enquiries",
                                    subtitle = "Response: ${comm.responseRatePct?.let { "${it.toInt()}%" } ?: "N/A"} • Avg ${comm.avgResponseMinutes ?: 0}m",
                                    icon = Icons.Filled.Forum,
                                    accentColor = DemoAmber,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.ONLINE_COMMUNICATION) }
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Total Sales Revenue",
                                    valueText = formatKes(rec.recordedSalesKes),
                                    subtitle = "${rec.salesCount} sales • $momBadgeText",
                                    icon = Icons.Filled.PointOfSale,
                                    accentColor = BrandTeal,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                                )
                                KpiMetricCard(
                                    title = "Total Expenses",
                                    valueText = formatKes(rec.recordedExpensesKes),
                                    subtitle = "${rec.expenseCount} expense logs",
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                    accentColor = AlertCoral,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Estimated Net Profit",
                                    valueText = formatKes(rec.estimatedNetProfitKes),
                                    subtitle = "Period Sales - Expenses",
                                    icon = Icons.Filled.AccountBalanceWallet,
                                    accentColor = if (rec.estimatedNetProfitKes >= 0) BrandNavy else AlertCoral,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.PERFORMANCE_ANALYTICS) }
                                )
                                KpiMetricCard(
                                    title = "Gross Profit",
                                    valueText = grossProfitValue,
                                    subtitle = grossProfitSubtitle,
                                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                                    accentColor = BrandPurple,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.PERFORMANCE_ANALYTICS) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Customers",
                                    valueText = "${summary.customerCount} ($customerGrowthDisplay)",
                                    subtitle = customerGrowthSubtitle,
                                    icon = Icons.Filled.Groups,
                                    accentColor = BrandPurple,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.CUSTOMERS) }
                                )
                                KpiMetricCard(
                                    title = "Inventory & Low Stock",
                                    valueText = "${summary.inventoryCount} SKUs • ${summary.lowStockCount} low",
                                    subtitle = "Stock value: ${formatKes(summary.totalInventoryRetailValueKes)}",
                                    icon = Icons.Filled.Inventory2,
                                    accentColor = if (summary.lowStockCount > 0) DemoAmber else BrandTeal,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Outstanding Payments",
                                    valueText = formatKes(totalOutstandingKes),
                                    subtitle = "${summary.pendingTransactionsCount} txns • ${summary.pendingPayablesCount} bills",
                                    icon = Icons.Filled.LocalShipping,
                                    accentColor = if (totalOutstandingKes > 0) AlertCoral else BrandTealDark,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.SUPPLIERS) }
                                )
                                KpiMetricCard(
                                    title = "Digital Payments",
                                    valueText = formatKes(pay.totalDigitalPaymentsKes),
                                    subtitle = "${pay.digitalTransactionCount} digital (${pay.digitalAdoptionPct ?: 0}%)",
                                    icon = Icons.Filled.Payments,
                                    accentColor = BrandTeal,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.DIGITAL_PAYMENTS) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiMetricCard(
                                    title = "Marketing Spend",
                                    valueText = formatKes(mkt.totalSpendKes),
                                    subtitle = "${mkt.campaignCount} campaigns • ${mkt.totalReach} reach",
                                    icon = Icons.Filled.Campaign,
                                    accentColor = BrandNavy,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.DIGITAL_MARKETING) }
                                )
                                KpiMetricCard(
                                    title = "Enquiries & Engagement",
                                    valueText = "${comm.totalEnquiries} chats",
                                    subtitle = "Response: ${comm.responseRatePct?.let { "${it.toInt()}%" } ?: "N/A"}",
                                    icon = Icons.Filled.Forum,
                                    accentColor = DemoAmber,
                                    isDemoBadge = summary.isDemoMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenModule(AppModule.ONLINE_COMMUNICATION) }
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Performance Bar Graph Preview on Home Dashboard
            item {
                VerticalGroupedBarChartCard(
                    title = "Monthly Business Performance Bar Graph (KES)",
                    subtitle = "Recorded Monthly Sales (${prompt4Analytics.monthlyRows.size} mo) • Tap Analytics for all 7 Bar Dashboards & 12 Forecast Views",
                    entries = prompt4Analytics.monthlySalesBars,
                    primaryLabel = "Monthly Sales (KES)",
                    primaryColor = BrandPurple,
                    isDemoData = summary.isDemoMode,
                    emptyMessage = "Log dated sales to render the Monthly Sales Bar Graph."
                )
            }

            item {
                PerformanceTrendLineChartCard(
                    title = "Business Performance Trend (KES)",
                    subtitle = "Recorded Revenue vs. Expenses (${analyticsReport.filterState.filter.label})",
                    points = revenueExpenseLinePoints,
                    primaryLabel = "Sales Revenue (KES)",
                    primaryColor = BrandTeal,
                    secondaryLabel = "Expenses (KES)",
                    secondaryColor = AlertCoral,
                    isDemoData = summary.isDemoMode,
                    emptyMessage = "No dated sales or expenses recorded in ${analyticsReport.activeWindow.label}."
                )
            }

            item {
                PaymentMethodDonutChartCard(
                    title = "Payment Method Distribution (KES)",
                    subtitle = "M-Pesa, Bank, Card, Other Digital & Cash usage (${analyticsReport.filterState.filter.label})",
                    breakdown = pay.methodBreakdown,
                    isDemoData = summary.isDemoMode
                )
            }

            item {
                SectionHeader(
                    title = "SME Management Modules (A–I & AI)",
                    subtitle = "Open any module aligned with Digital Technology Adoption, POS, CRM & Forecasting"
                )
            }

            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val twoCols = maxWidth >= 600.dp
                    val modules = AppModule.entries
                    if (twoCols) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            modules.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { mod ->
                                        ModuleTileCard(
                                            module = mod,
                                            onClick = { onOpenModule(mod) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            modules.forEach { mod ->
                                ModuleTileCard(
                                    module = mod,
                                    onClick = { onOpenModule(mod) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = if (summary.isDemoMode) "Sample Ledger Entries (DEMO DATA)" else "Recent Entries (${analyticsReport.filterState.filter.label})",
                    subtitle = if (summary.isDemoMode) "Preview entries — toggle Demo Mode off or log a transaction to start live" else "Filtered from Cloud Firestore records"
                )
            }

            if (transactionsState is UiState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (recentFeed.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions in this date range",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'POS Sale' above or widen your date filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentFeed, key = { it.id }) { tx ->
                    val isSale = tx.type == BusinessTransaction.TYPE_SALE
                    val prefix = when (tx.type) {
                        BusinessTransaction.TYPE_SALE -> "+"
                        BusinessTransaction.TYPE_EXPENSE -> "-"
                        else -> ""
                    }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (tx.receiptNumber.isNotBlank()) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = BrandPurpleSoft) {
                                            Text(
                                                text = tx.receiptNumber,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BrandPurple,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (summary.isDemoMode) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("DEMO", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                Text(
                                    text = buildString {
                                        append("${tx.category} • ${tx.paymentMethod} • ${tx.effectiveDate()}")
                                        if (tx.customerName.isNotBlank()) append(" • ${tx.customerName}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$prefix${formatKes(tx.amountKes)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSale) BrandTeal else if (tx.type == BusinessTransaction.TYPE_EXPENSE) AlertCoral else BrandNavy
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
