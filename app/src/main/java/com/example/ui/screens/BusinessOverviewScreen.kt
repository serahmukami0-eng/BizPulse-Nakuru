package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RoomCategorySummary
import com.example.data.model.AppModule
import com.example.data.model.formatKes
import com.example.data.repository.RoomBusinessOverviewState
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandNavySoft
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealDark
import com.example.ui.theme.BrandTealLight
import com.example.ui.theme.DemoAmber
import com.example.ui.viewmodel.SalesOverviewAiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainBusinessOverviewScreen(
    overviewState: RoomBusinessOverviewState,
    onRefreshRoomCache: () -> Unit,
    onOpenQuickSale: () -> Unit,
    onOpenQuickExpense: () -> Unit,
    onOpenModule: (AppModule) -> Unit,
    salesAiState: SalesOverviewAiState = SalesOverviewAiState.Idle,
    onAnalyzeSalesWithAi: () -> Unit = {},
    onClearSalesAiSummary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
                .testTag("main_business_overview_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RoomBusinessOverviewSection(
                    overviewState = overviewState,
                    onRefreshRoomCache = onRefreshRoomCache,
                    onOpenQuickSale = onOpenQuickSale,
                    onOpenQuickExpense = onOpenQuickExpense,
                    onOpenModule = onOpenModule,
                    salesAiState = salesAiState,
                    onAnalyzeSalesWithAi = onAnalyzeSalesWithAi,
                    onClearSalesAiSummary = onClearSalesAiSummary,
                    showQuickActionButtons = true
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomBusinessOverviewSection(
    overviewState: RoomBusinessOverviewState,
    onRefreshRoomCache: () -> Unit,
    onOpenQuickSale: () -> Unit,
    onOpenQuickExpense: () -> Unit,
    onOpenModule: (AppModule) -> Unit,
    modifier: Modifier = Modifier,
    salesAiState: SalesOverviewAiState = SalesOverviewAiState.Idle,
    onAnalyzeSalesWithAi: () -> Unit = {},
    onClearSalesAiSummary: () -> Unit = {},
    showQuickActionButtons: Boolean = false
) {
    val lastSyncFormatted = remember(overviewState.lastSyncedEpochMs) {
        overviewState.lastSyncedEpochMs?.let { epoch ->
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(epoch))
        } ?: "Syncing..."
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "Main Business Overview",
                subtitle = "Real-time KPIs from Room SQLite local store (KES)",
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onRefreshRoomCache,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("room_overview_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh Room KPI cache",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sync")
            }
        }

        // Room cache badge & status bar
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (overviewState.isDemoData) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                BrandTealLight.copy(alpha = 0.65f)
            },
            border = BorderStroke(
                1.dp,
                if (overviewState.isDemoData) DemoAmber.copy(alpha = 0.35f) else BrandTeal.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("room_cache_status_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = "Room Database Storage",
                        tint = if (overviewState.isDemoData) DemoAmber else BrandTealDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (overviewState.isDemoData) {
                                "ROOM LOCAL CACHE • DEMO DATA ONLY"
                            } else {
                                "ROOM SQLITE KPI CACHE • OFFLINE READY"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (overviewState.isDemoData) DemoAmber else BrandTealDark
                        )
                        Text(
                            text = "${overviewState.salesCount} sales & ${overviewState.expenseCount} expenses indexed • Synced $lastSyncFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showQuickActionButtons) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenQuickSale,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandTeal,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("overview_quick_sale_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Sale")
                }
                OutlinedButton(
                    onClick = onOpenQuickExpense,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("overview_quick_expense_button")
                ) {
                    Text("Log Expense")
                }
            }
        }

        val isAnalyzingSales = salesAiState is SalesOverviewAiState.Loading ||
            salesAiState is SalesOverviewAiState.Streaming

        Button(
            onClick = onAnalyzeSalesWithAi,
            enabled = !isAnalyzingSales,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandNavy,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("overview_analyze_sales_ai_button")
        ) {
            if (isAnalyzingSales) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Sales Data with Firebase AI...", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Analyze sales data with Firebase AI",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI Sales Performance Summary", fontWeight = FontWeight.SemiBold)
            }
        }

        AnimatedVisibility(visible = salesAiState !is SalesOverviewAiState.Idle) {
            SalesOverviewAiSummaryCard(
                salesAiState = salesAiState,
                onRetry = onAnalyzeSalesWithAi,
                onDismiss = onClearSalesAiSummary
            )
        }

        // Primary Room-computed Financial KPIs
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 700.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Total Sales (Room)",
                        valueText = formatKes(overviewState.totalSalesKes),
                        subtitle = "${overviewState.salesCount} sale(s) • Avg ${formatKes(overviewState.averageSaleTicketKes)}",
                        icon = Icons.Filled.PointOfSale,
                        accentColor = BrandTeal,
                        isDemoBadge = overviewState.isDemoData,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_kpi_total_sales"),
                        onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                    )
                    KpiMetricCard(
                        title = "Expense Summary (Room)",
                        valueText = formatKes(overviewState.totalExpensesKes),
                        subtitle = "${overviewState.expenseCount} expense(s)${overviewState.expenseToSalesRatioPct?.let { " • $it% of sales" } ?: ""}",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        accentColor = AlertCoral,
                        isDemoBadge = overviewState.isDemoData,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_kpi_total_expenses"),
                        onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                    )
                    KpiMetricCard(
                        title = "Est. Net Profit (Room)",
                        valueText = formatKes(overviewState.estimatedNetProfitKes),
                        subtitle = overviewState.netMarginPct?.let { "$it% net margin" } ?: "Sales minus expenses",
                        icon = Icons.Filled.AccountBalanceWallet,
                        accentColor = if (overviewState.estimatedNetProfitKes >= 0.0) BrandNavy else AlertCoral,
                        isDemoBadge = overviewState.isDemoData,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_kpi_net_profit"),
                        onClick = { onOpenModule(AppModule.PERFORMANCE_ANALYTICS) }
                    )
                    KpiMetricCard(
                        title = "Digital Payments (Room)",
                        valueText = formatKes(overviewState.digitalPaymentsKes),
                        subtitle = "M-Pesa, Bank & Card settled",
                        icon = Icons.Filled.Payments,
                        accentColor = BrandTealDark,
                        isDemoBadge = overviewState.isDemoData,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_kpi_digital_payments"),
                        onClick = { onOpenModule(AppModule.DIGITAL_PAYMENTS) }
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "Total Sales (Room)",
                            valueText = formatKes(overviewState.totalSalesKes),
                            subtitle = "${overviewState.salesCount} sale(s) • Avg ${formatKes(overviewState.averageSaleTicketKes)}",
                            icon = Icons.Filled.PointOfSale,
                            accentColor = BrandTeal,
                            isDemoBadge = overviewState.isDemoData,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("room_kpi_total_sales"),
                            onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                        )
                        KpiMetricCard(
                            title = "Expense Summary",
                            valueText = formatKes(overviewState.totalExpensesKes),
                            subtitle = "${overviewState.expenseCount} expense(s)${overviewState.expenseToSalesRatioPct?.let { " • $it% ratio" } ?: ""}",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            accentColor = AlertCoral,
                            isDemoBadge = overviewState.isDemoData,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("room_kpi_total_expenses"),
                            onClick = { onOpenModule(AppModule.RECORD_KEEPING) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "Estimated Net Profit",
                            valueText = formatKes(overviewState.estimatedNetProfitKes),
                            subtitle = overviewState.netMarginPct?.let { "$it% net margin" } ?: "Sales - Expenses",
                            icon = Icons.Filled.AccountBalanceWallet,
                            accentColor = if (overviewState.estimatedNetProfitKes >= 0.0) BrandNavy else AlertCoral,
                            isDemoBadge = overviewState.isDemoData,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("room_kpi_net_profit"),
                            onClick = { onOpenModule(AppModule.PERFORMANCE_ANALYTICS) }
                        )
                        KpiMetricCard(
                            title = "Digital Payments",
                            valueText = formatKes(overviewState.digitalPaymentsKes),
                            subtitle = "M-Pesa, Bank & Card total",
                            icon = Icons.Filled.Payments,
                            accentColor = BrandTealDark,
                            isDemoBadge = overviewState.isDemoData,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("room_kpi_digital_payments"),
                            onClick = { onOpenModule(AppModule.DIGITAL_PAYMENTS) }
                        )
                    }
                }
            }
        }

        // Sales vs Expense Ratio & Category Breakdown Card (computed by Room SQLite GROUP BY)
        SalesAndExpenseSummaryCard(overviewState = overviewState)
    }
}

@Composable
private fun SalesAndExpenseSummaryCard(
    overviewState: RoomBusinessOverviewState
) {
    val totalFlow = (overviewState.totalSalesKes + overviewState.totalExpensesKes).coerceAtLeast(1.0)
    val salesShare = (overviewState.totalSalesKes / totalFlow).toFloat().coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_sales_expense_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sales vs. Expense Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aggregated locally via Room SQLite queries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = BrandTeal
                )
            }

            // Proportional Sales vs Expense Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(AlertCoral.copy(alpha = 0.22f))
                ) {
                    if (overviewState.totalSalesKes > 0.0 || overviewState.totalExpensesKes > 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(salesShare)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(99.dp))
                                .background(BrandTeal)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandTeal)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sales: ${formatKes(overviewState.totalSalesKes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTealDark
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AlertCoral)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expenses: ${formatKes(overviewState.totalExpensesKes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AlertCoral
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val twoCol = maxWidth >= 620.dp
                if (twoCol) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CategorySummaryColumn(
                            heading = "Top Sales Categories",
                            items = overviewState.salesByCategory,
                            totalKes = overviewState.totalSalesKes,
                            barColor = BrandTeal,
                            emptyHint = "No sales cached yet.",
                            modifier = Modifier.weight(1f)
                        )
                        CategorySummaryColumn(
                            heading = "Expense Summary by Category",
                            items = overviewState.expensesByCategory,
                            totalKes = overviewState.totalExpensesKes,
                            barColor = AlertCoral,
                            emptyHint = "No expenses cached yet.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CategorySummaryColumn(
                            heading = "Top Sales Categories",
                            items = overviewState.salesByCategory,
                            totalKes = overviewState.totalSalesKes,
                            barColor = BrandTeal,
                            emptyHint = "No sales cached yet."
                        )
                        CategorySummaryColumn(
                            heading = "Expense Summary by Category",
                            items = overviewState.expensesByCategory,
                            totalKes = overviewState.totalExpensesKes,
                            barColor = AlertCoral,
                            emptyHint = "No expenses cached yet."
                        )
                    }
                }
            }

            if (overviewState.paymentBreakdown.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Payment Channels (Room SQLite)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        overviewState.paymentBreakdown.forEach { method ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandNavySoft
                            ) {
                                Text(
                                    text = "${method.paymentMethod}: ${formatKes(method.totalKes)} (${method.entryCount})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryColumn(
    heading: String,
    items: List<RoomCategorySummary>,
    totalKes: Double,
    barColor: Color,
    emptyHint: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BrandNavy
        )
        if (items.isEmpty()) {
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val safeTotal = totalKes.coerceAtLeast(1.0)
            items.take(4).forEach { cat ->
                val fraction = (cat.totalKes / safeTotal).toFloat().coerceIn(0.04f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${cat.category} (${cat.entryCount})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatKes(cat.totalKes),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = barColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(barColor.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(99.dp))
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesOverviewAiSummaryCard(
    salesAiState: SalesOverviewAiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val isError = salesAiState is SalesOverviewAiState.Error
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    } else {
        BrandTeal.copy(alpha = 0.55f)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overview_sales_ai_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Filled.Info else Icons.Filled.AutoAwesome,
                        contentDescription = "Firebase AI Sales Performance Summary",
                        tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else BrandTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Sales Performance Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else BrandNavy
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("overview_sales_ai_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss AI sales summary",
                        tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (salesAiState) {
                is SalesOverviewAiState.Idle -> Unit
                is SalesOverviewAiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Evaluating recorded sales revenue, categories & payment mix...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is SalesOverviewAiState.Streaming -> {
                    Text(
                        text = salesAiState.partialText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is SalesOverviewAiState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (salesAiState.isDemoData) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            BrandTealLight
                        }
                    ) {
                        Text(
                            text = "${if (salesAiState.isDemoData) "DEMO DATA" else "LIVE SALES"} • ${salesAiState.analyzedSalesCount} sale(s) • ${formatKes(salesAiState.totalSalesKes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (salesAiState.isDemoData) DemoAmber else BrandTealDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = salesAiState.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("overview_sales_ai_refresh_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Regenerate")
                        }
                    }
                }
                is SalesOverviewAiState.EmptySales -> {
                    Text(
                        text = salesAiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is SalesOverviewAiState.Error -> {
                    Text(
                        text = salesAiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("overview_sales_ai_retry_button")
                        ) {
                            Text("Retry Analysis")
                        }
                    }
                }
            }
        }
    }
}
