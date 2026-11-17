package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppModule
import com.example.data.model.formatKes
import com.example.data.service.GrowthAnalysisReport
import com.example.data.service.GrowthOpportunity
import com.example.ui.components.MethodologyNoticeCard
import com.example.ui.components.ModulePageHeader
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.AlertCoralLight
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandNavySoft
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealBright
import com.example.ui.theme.BrandTealDark
import com.example.ui.theme.BrandTealLight
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberDark
import com.example.ui.theme.DemoAmberLight
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiResponseState
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.GrowthAnalysisUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    summary: DashboardSummary,
    aiState: AiResponseState,
    messages: List<AiChatMessage>,
    growthAnalysisState: GrowthAnalysisUiState,
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onAnalyzeTransactions: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

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
                module = AppModule.AI_ASSISTANT,
                onBackToDashboard = onBackToDashboard,
                onOpenAllModules = onOpenAllModules
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "AI Business Intelligence (IV1–IV4 → DV)",
                    subtitle = "${summary.businessName} (${if (summary.isDemoMode) "Demo Preview" else "Live Cloud Firestore"})"
                )

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Research Growth Audit") },
                        modifier = Modifier.testTag("tab_growth_analyzer")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Advisor Chat") },
                        modifier = Modifier.testTag("tab_advisor_chat")
                    )
                }

                if (selectedTab == 0) {
                    GrowthAnalyzerContent(
                        summary = summary,
                        state = growthAnalysisState,
                        onAnalyzeTransactions = onAnalyzeTransactions,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    AdvisorChatContent(
                        aiState = aiState,
                        messages = messages,
                        onSendPrompt = onSendPrompt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GrowthAnalyzerContent(
    summary: DashboardSummary,
    state: GrowthAnalysisUiState,
    onAnalyzeTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = null,
                                tint = BrandTealBright
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini Multi-Variable Audit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = if (summary.isDemoMode) DemoAmberLight else BrandTealLight
                        ) {
                            Text(
                                text = if (summary.isDemoMode) "DEMO DATA — SAMPLE ONLY" else "LIVE FIRESTORE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.isDemoMode) DemoAmberDark else BrandTealDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Analyzes Digital Marketing, Digital Payments, Record Keeping, and Online Communication records in Firestore to recommend practical SME improvements in ${summary.locationNakuru}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${summary.transactionCount} txns • ${summary.campaignCount} campaigns • ${summary.communicationCount} chats",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onAnalyzeTransactions,
                            enabled = state !is GrowthAnalysisUiState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("analyze_transactions_button")
                        ) {
                            Icon(
                                imageVector = if (state is GrowthAnalysisUiState.Success) Icons.Filled.Refresh else Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state is GrowthAnalysisUiState.Success) "Refresh Audit" else "Run AI Audit",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        when (state) {
            GrowthAnalysisUiState.Idle -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ready to Analyze Digital Technology Adoption",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap 'Run AI Audit' above to evaluate your business across Digital Marketing (IV1), Digital Payment Systems (IV2), Computerized Record Keeping (IV3), Online Communication (IV4), and SME Performance (DV).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            GrowthAnalysisUiState.Loading -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Column {
                                Text(
                                    text = "Analyzing Cloud Firestore records...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Synthesizing recommendations across IV1–IV4 and KES SME performance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            is GrowthAnalysisUiState.EmptyLedger -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DemoAmberLight.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, DemoAmber)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "Empty ledger notice",
                                tint = DemoAmberDark
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "No Live Business Records Found",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DemoAmberDark
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            is GrowthAnalysisUiState.Error -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Analysis Failed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = onAnalyzeTransactions,
                                modifier = Modifier.testTag("retry_analysis_button")
                            ) {
                                Text("Retry Audit")
                            }
                        }
                    }
                }
            }

            is GrowthAnalysisUiState.Success -> {
                val report = state.report
                item { ReportSummaryCard(report = report) }
                if (report.positiveTrends.isNotEmpty() || report.negativeTrends.isNotEmpty() || report.weakAdoptionAreas.isNotEmpty() || report.missingDataGaps.isNotEmpty()) {
                    item { TrendsAndGapsSection(report = report) }
                }
                item { VariableRecommendationsSection(report = report) }
                item { InsightPairSection(report = report) }
                item {
                    MethodologyNoticeCard(
                        title = "Correlation vs. Causation Methodological Caveat",
                        body = report.correlationCaveat
                    )
                }
                item {
                    SectionHeader(
                        title = "Prioritized Growth Actions",
                        subtitle = "${report.growthOpportunities.size} data-grounded recommendations for ${report.businessName}"
                    )
                }
                items(report.growthOpportunities) { opportunity ->
                    GrowthOpportunityCard(opportunity = opportunity)
                }
                item { NakuruLocalTipCard(tip = report.nakuruLocalTip) }
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(report: GrowthAnalysisReport) {
    val (badgeBg, badgeFg) = when (report.healthStatus.uppercase()) {
        "STRONG" -> BrandTealLight to BrandTealDark
        "STEADY" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "WATCH" -> DemoAmberLight to DemoAmberDark
        else -> AlertCoralLight to AlertCoral
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = badgeBg) {
                        Text(
                            text = "STATUS: ${report.healthStatus.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (report.isDemoData) {
                        Surface(shape = RoundedCornerShape(8.dp), color = DemoAmberLight) {
                            Text(
                                text = "DEMO DATA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DemoAmberDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${report.analyzedTransactionCount} txns • ${report.digitalPaymentSharePct}% digital",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = report.executiveSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 460.dp
                if (isCompact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("Revenue", formatKes(report.totalSalesKes), BrandTeal)
                        MetricPill("Expenses", formatKes(report.totalExpensesKes), DemoAmber)
                        MetricPill(
                            "Net Cashflow",
                            formatKes(report.netCashflowKes),
                            if (report.netCashflowKes >= 0) BrandNavy else AlertCoral
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill("Revenue", formatKes(report.totalSalesKes), BrandTeal, Modifier.weight(1f))
                        MetricPill("Expenses", formatKes(report.totalExpensesKes), DemoAmber, Modifier.weight(1f))
                        MetricPill(
                            "Net Cashflow",
                            formatKes(report.netCashflowKes),
                            if (report.netCashflowKes >= 0) BrandNavy else AlertCoral,
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendsAndGapsSection(report: GrowthAnalysisReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (report.positiveTrends.isNotEmpty() || report.negativeTrends.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Observed Positive & Negative Trends",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    report.positiveTrends.forEach { trend ->
                        Text(
                            text = "+ $trend",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandTealDark
                        )
                    }
                    report.negativeTrends.forEach { trend ->
                        Text(
                            text = "- $trend",
                            style = MaterialTheme.typography.bodySmall,
                            color = AlertCoral
                        )
                    }
                }
            }
        }
        if (report.weakAdoptionAreas.isNotEmpty()) {
            VariableAdviceCard(
                icon = Icons.Filled.WarningAmber,
                title = "Weak Digital Technology Adoption Areas",
                body = report.weakAdoptionAreas.joinToString("\n• ", prefix = "• "),
                accent = DemoAmber
            )
        }
        if (report.missingDataGaps.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DemoAmberLight.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, DemoAmber)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Missing Information Limiting Reliable Analysis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DemoAmberDark
                    )
                    report.missingDataGaps.forEach { gap ->
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
private fun VariableRecommendationsSection(report: GrowthAnalysisReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VariableAdviceCard(
            icon = Icons.Filled.Campaign,
            title = "IV1: Improving Digital Marketing",
            body = report.digitalMarketingAdvice,
            accent = BrandTeal
        )
        VariableAdviceCard(
            icon = Icons.Filled.Payments,
            title = "IV2: Increasing Digital Payment Adoption",
            body = report.digitalPaymentsAdvice,
            accent = BrandNavy
        )
        VariableAdviceCard(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = "IV3: Computerized Record Keeping",
            body = report.recordKeepingAdvice,
            accent = BrandTealDark
        )
        VariableAdviceCard(
            icon = Icons.Filled.Forum,
            title = "IV4: Online Customer Communication",
            body = report.onlineCommunicationAdvice,
            accent = DemoAmber
        )
    }
}

@Composable
private fun VariableAdviceCard(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun InsightPairSection(report: GrowthAnalysisReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VariableAdviceCard(
            icon = Icons.Filled.AccountBalanceWallet,
            title = "Cashflow & Payment Mix",
            body = report.cashflowInsight,
            accent = BrandTeal
        )
        VariableAdviceCard(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            title = "Top Cost Driver",
            body = report.topCostDriver,
            accent = BrandNavy
        )
    }
}

@Composable
private fun GrowthOpportunityCard(opportunity: GrowthOpportunity) {
    val (pBg, pFg) = when (opportunity.priority.uppercase()) {
        "HIGH" -> AlertCoralLight to AlertCoral
        "LOW" -> BrandTealLight to BrandTealDark
        else -> DemoAmberLight to DemoAmberDark
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = opportunity.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = pBg) {
                    Text(
                        text = opportunity.priority.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = pFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Surface(shape = RoundedCornerShape(6.dp), color = BrandNavySoft) {
                Text(
                    text = opportunity.variableFocus,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandNavy,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = opportunity.actionStep,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (opportunity.supportingIndicators.isNotBlank()) {
                Text(
                    text = "Supporting Indicators: ${opportunity.supportingIndicators}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = BrandNavy
                )
            }
            Text(
                text = "Expected Impact: ${opportunity.expectedImpact}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandTealDark
            )
        }
    }
}

@Composable
private fun NakuruLocalTipCard(tip: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandTealLight.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, BrandTeal)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.LocationCity,
                contentDescription = null,
                tint = BrandTealDark
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Nakuru Market Advantage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandTealDark
                )
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AdvisorChatContent(
    aiState: AiResponseState,
    messages: List<AiChatMessage>,
    onSendPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputPrompt by remember { mutableStateOf("") }
    val quickPrompts = listOf(
        "How can I improve digital marketing ROI?",
        "How do I increase M-Pesa & digital payment adoption?",
        "Review my KES record keeping & profit margins",
        "Improve online customer response conversions"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { prompt ->
                AssistChip(
                    onClick = { onSendPrompt(prompt) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = BrandTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(prompt) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && aiState is AiResponseState.Idle) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrandTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nakuru SME Research Advisor Ready",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ask about Digital Marketing, M-Pesa/Bank payments, computerized record keeping, online communication speed, or KES profitability. Tap a chip above to start.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (msg.isUser) BrandNavy else MaterialTheme.colorScheme.surface,
                        border = if (msg.isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.widthIn(max = 560.dp)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (aiState is AiResponseState.Loading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Thinking with Gemini...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (aiState is AiResponseState.Streaming) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = aiState.partialText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else if (aiState is AiResponseState.Error) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Ask Gemini about your Nakuru SME...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input"),
                shape = RoundedCornerShape(16.dp),
                maxLines = 3
            )
            FilledIconButton(
                onClick = {
                    if (inputPrompt.isNotBlank()) {
                        onSendPrompt(inputPrompt)
                        inputPrompt = ""
                    }
                },
                enabled = inputPrompt.isNotBlank() && aiState !is AiResponseState.Loading && aiState !is AiResponseState.Streaming,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_send_button"),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = BrandNavy)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", tint = Color.White)
            }
        }
    }
}
