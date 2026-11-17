package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppModule
import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.DateFilterOption
import com.example.data.model.DemoSampleData
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.NakuruSmeCatalog
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.StockMovementRecord
import com.example.data.model.SupplierRecord
import com.example.data.model.formatKes
import com.example.data.model.isValidIsoDate
import com.example.data.model.matchesDateFilter
import com.example.data.model.todayIsoDate
import com.example.ui.components.DemoDataModeBanner
import com.example.ui.components.EmptyModulePlaceholder
import com.example.ui.components.ErrorModulePlaceholder
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LoadingModulePlaceholder
import com.example.ui.components.MethodologyNoticeCard
import com.example.ui.components.ModulePageHeader
import com.example.ui.components.SearchAndDateFilterBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandPurple
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

@Composable
fun DigitalMarketingScreen(
    summary: DashboardSummary,
    campaignsState: UiState<List<DigitalMarketingCampaign>>,
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onAddCampaign: (
        title: String,
        channel: String,
        campaignDate: String,
        spend: String,
        reach: String,
        engagement: String,
        inquiries: String,
        status: String,
        notes: String
    ) -> Unit,
    onUpdateCampaign: (
        campaignId: String,
        title: String,
        channel: String,
        campaignDate: String,
        spend: String,
        reach: String,
        engagement: String,
        inquiries: String,
        status: String,
        notes: String
    ) -> Unit,
    onDeleteCampaign: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCampaign by remember { mutableStateOf<DigitalMarketingCampaign?>(null) }
    var deletingCampaign by remember { mutableStateOf<DigitalMarketingCampaign?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf(DateFilterOption.ALL) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedChannelFilter by remember { mutableStateOf("ALL") }

    val liveCampaigns = (campaignsState as? UiState.Success)?.data.orEmpty()
    val campaigns = if (summary.isDemoMode) DemoSampleData.sampleCampaigns else liveCampaigns
    val filteredCampaigns = remember(
        campaigns,
        searchQuery,
        selectedDateFilter,
        selectedStatusFilter,
        selectedChannelFilter
    ) {
        val q = searchQuery.trim().lowercase()
        campaigns.filter { camp ->
            val matchesQuery = q.isEmpty() ||
                camp.title.lowercase().contains(q) ||
                camp.channel.lowercase().contains(q) ||
                camp.notes.lowercase().contains(q) ||
                camp.effectiveDate().lowercase().contains(q)
            val matchesStatus = selectedStatusFilter == "ALL" || camp.status == selectedStatusFilter
            val matchesChannel = selectedChannelFilter == "ALL" || camp.channel == selectedChannelFilter
            val matchesDate = matchesDateFilter(camp.campaignDate, camp.createdAt, selectedDateFilter)
            matchesQuery && matchesStatus && matchesChannel && matchesDate
        }
    }

    val avgEngagementPct = if (summary.totalCampaignReach > 0L) {
        ((summary.totalCampaignEngagement.toDouble() / summary.totalCampaignReach) * 100.0)
    } else 0.0

    ModuleScaffoldContainer(
        module = AppModule.DIGITAL_MARKETING,
        onBackToDashboard = onBackToDashboard,
        onOpenAllModules = onOpenAllModules
    ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Marketing Spend",
                        valueText = formatKes(summary.totalMarketingSpendKes),
                        subtitle = "${campaigns.size} campaign(s)",
                        icon = Icons.Filled.Campaign,
                        accentColor = BrandNavy,
                        isDemoBadge = summary.isDemoMode,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Reach & Inquiries",
                        valueText = "${summary.totalCampaignReach}",
                        subtitle = "${String.format(Locale.US, "%.1f", avgEngagementPct)}% eng • ${summary.totalMarketingInquiries} leads",
                        icon = Icons.Filled.Forum,
                        accentColor = BrandTeal,
                        isDemoBadge = summary.isDemoMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Digital Campaigns (IV1)",
                        subtitle = "Track reach, engagement, spend & inquiries across digital channels",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_campaign_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Campaign")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchAndDateFilterBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedDateFilter = selectedDateFilter,
                        onDateFilterChange = { selectedDateFilter = it },
                        searchPlaceholder = "Search campaigns by name, platform, date, or notes",
                        testTagPrefix = "marketing"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatusFilter == "ALL",
                            onClick = { selectedStatusFilter = "ALL" },
                            label = { Text("All Statuses") }
                        )
                        DigitalMarketingCampaign.statuses.forEach { st ->
                            FilterChip(
                                selected = selectedStatusFilter == st,
                                onClick = { selectedStatusFilter = st },
                                label = { Text(st) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedChannelFilter == "ALL",
                            onClick = { selectedChannelFilter = "ALL" },
                            label = { Text("All Platforms") }
                        )
                        DigitalMarketingCampaign.channels.forEach { ch ->
                            FilterChip(
                                selected = selectedChannelFilter == ch,
                                onClick = { selectedChannelFilter = ch },
                                label = { Text(ch) }
                            )
                        }
                    }
                }
            }

            when {
                campaignsState is UiState.Loading -> item { LoadingModulePlaceholder("Loading digital marketing campaigns...") }
                campaignsState is UiState.Error -> item { ErrorModulePlaceholder(campaignsState.message) }
                campaigns.isEmpty() -> item {
                    EmptyModulePlaceholder(
                        icon = Icons.Filled.Campaign,
                        title = "No digital marketing campaigns logged",
                        tip = "Tap 'Log Campaign' to record WhatsApp Status, Instagram, Facebook, TikTok, or Google Maps activities."
                    )
                }
                filteredCampaigns.isEmpty() -> item {
                    EmptyModulePlaceholder(
                        icon = Icons.Filled.Search,
                        title = "No campaigns match your filters",
                        tip = "Clear the search query or switch date/platform/status filters."
                    )
                }
                else -> items(filteredCampaigns, key = { it.id }) { camp ->
                    CampaignCard(
                        campaign = camp,
                        isDemo = summary.isDemoMode,
                        onEdit = if (summary.isDemoMode) null else {
                            { editingCampaign = camp }
                        },
                        onDelete = if (summary.isDemoMode) null else {
                            { deletingCampaign = camp }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CampaignFormDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, channel, date, spend, reach, eng, inq, status, notes ->
                onAddCampaign(title, channel, date, spend, reach, eng, inq, status, notes)
                showAddDialog = false
            }
        )
    }

    editingCampaign?.let { target ->
        CampaignFormDialog(
            initial = target,
            onDismiss = { editingCampaign = null },
            onConfirm = { title, channel, date, spend, reach, eng, inq, status, notes ->
                onUpdateCampaign(target.id, title, channel, date, spend, reach, eng, inq, status, notes)
                editingCampaign = null
            }
        )
    }

    deletingCampaign?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Campaign?",
            message = "Remove '${target.title}' from Digital Marketing records?",
            onDismiss = { deletingCampaign = null },
            onConfirm = {
                onDeleteCampaign(target.id)
                deletingCampaign = null
            }
        )
    }
}

@Composable
fun DigitalPaymentsScreen(
    summary: DashboardSummary,
    transactionsState: UiState<List<BusinessTransaction>>,
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onAddTransaction: (
        type: String,
        title: String,
        category: String,
        amount: String,
        method: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String
    ) -> Unit,
    onUpdateTransaction: (
        transactionId: String,
        type: String,
        title: String,
        category: String,
        amount: String,
        method: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String
    ) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<BusinessTransaction?>(null) }
    var deletingPayment by remember { mutableStateOf<BusinessTransaction?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf(DateFilterOption.ALL) }
    var selectedMethodFilter by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    val liveTx = (transactionsState as? UiState.Success)?.data.orEmpty()
    val txList = if (summary.isDemoMode) DemoSampleData.sampleTransactions else liveTx
    val filteredTx = remember(
        txList,
        searchQuery,
        selectedDateFilter,
        selectedMethodFilter,
        selectedStatusFilter
    ) {
        val q = searchQuery.trim().lowercase()
        txList.filter { tx ->
            val matchesQuery = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.paymentMethod.lowercase().contains(q) ||
                tx.transactionRef.lowercase().contains(q) ||
                tx.notes.lowercase().contains(q) ||
                tx.effectiveDate().lowercase().contains(q)
            val matchesMethod = selectedMethodFilter == "ALL" || tx.paymentMethod == selectedMethodFilter
            val matchesStatus = selectedStatusFilter == "ALL" || tx.paymentStatus == selectedStatusFilter
            val matchesDate = matchesDateFilter(tx.transactionDate, tx.createdAt, selectedDateFilter)
            matchesQuery && matchesMethod && matchesStatus && matchesDate
        }
    }

    val digitalVolume = summary.mpesaSalesKes + summary.bankSalesKes + summary.cardSalesKes + summary.otherDigitalSalesKes
    val totalVolume = (digitalVolume + summary.cashSalesKes).coerceAtLeast(1.0)

    ModuleScaffoldContainer(
        module = AppModule.DIGITAL_PAYMENTS,
        onBackToDashboard = onBackToDashboard,
        onOpenAllModules = onOpenAllModules
    ) {
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
                MethodologyNoticeCard(
                    title = "Digital Payment Logging Notice",
                    body = "Entries here record M-Pesa, bank transfers, card, and other digital receipts inside Cloud Firestore to track adoption. Direct M-Pesa Daraja API STK-Push or automated bank webhook settlement is not active without merchant API configuration."
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Total Digital Payments",
                        valueText = formatKes(summary.totalDigitalPaymentsKes),
                        subtitle = "${summary.digitalPaymentCount}/${summary.transactionCount} digital txns",
                        icon = Icons.Filled.Payments,
                        accentColor = BrandTeal,
                        isDemoBadge = summary.isDemoMode,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Digital Adoption Rate",
                        valueText = "${summary.digitalPaymentAdoptionPct}%",
                        subtitle = "Digital sales: ${formatKes(digitalVolume)}",
                        icon = Icons.Filled.PointOfSale,
                        accentColor = BrandNavy,
                        isDemoBadge = summary.isDemoMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
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
                            text = "Payment Method Mix (Sales Revenue in KES)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        PaymentChannelBar("M-Pesa / Till", summary.mpesaSalesKes, (summary.mpesaSalesKes / totalVolume).toFloat(), BrandTeal)
                        PaymentChannelBar("Bank Transfer", summary.bankSalesKes, (summary.bankSalesKes / totalVolume).toFloat(), BrandNavy)
                        PaymentChannelBar("Card / POS", summary.cardSalesKes, (summary.cardSalesKes / totalVolume).toFloat(), BrandTealDark)
                        PaymentChannelBar("Other Digital", summary.otherDigitalSalesKes, (summary.otherDigitalSalesKes / totalVolume).toFloat(), DemoAmber)
                        PaymentChannelBar("Cash Drawer", summary.cashSalesKes, (summary.cashSalesKes / totalVolume).toFloat(), AlertCoral)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Payment Transactions (IV2)",
                        subtitle = "${filteredTx.size} of ${txList.size} entries shown",
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showAddPaymentDialog = true },
                        modifier = Modifier.testTag("add_digital_payment_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Payment")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchAndDateFilterBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedDateFilter = selectedDateFilter,
                        onDateFilterChange = { selectedDateFilter = it },
                        searchPlaceholder = "Search by description, reference code, method, or date",
                        testTagPrefix = "payments"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethodFilter == "ALL",
                            onClick = { selectedMethodFilter = "ALL" },
                            label = { Text("All Methods") }
                        )
                        BusinessTransaction.allPaymentMethods.forEach { method ->
                            FilterChip(
                                selected = selectedMethodFilter == method,
                                onClick = { selectedMethodFilter = method },
                                label = { Text(method) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatusFilter == "ALL",
                            onClick = { selectedStatusFilter = "ALL" },
                            label = { Text("All Statuses") }
                        )
                        BusinessTransaction.paymentStatuses.forEach { st ->
                            FilterChip(
                                selected = selectedStatusFilter == st,
                                onClick = { selectedStatusFilter = st },
                                label = { Text(st) }
                            )
                        }
                    }
                }
            }

            when {
                transactionsState is UiState.Loading -> item { LoadingModulePlaceholder("Loading digital payment ledger...") }
                transactionsState is UiState.Error -> item { ErrorModulePlaceholder(transactionsState.message) }
                txList.isEmpty() -> item {
                    EmptyModulePlaceholder(
                        icon = Icons.Filled.Payments,
                        title = "No payment transactions logged yet",
                        tip = "Tap 'Log Payment' to record M-Pesa, Bank, Card, or Cash settlements."
                    )
                }
                filteredTx.isEmpty() -> item {
                    EmptyModulePlaceholder(
                        icon = Icons.Filled.Search,
                        title = "No payments match your filters",
                        tip = "Clear search text or reset payment method, status, and date filters."
                    )
                }
                else -> items(filteredTx, key = { it.id }) { tx ->
                    TransactionItemCard(
                        item = tx,
                        isDemo = summary.isDemoMode,
                        showTimestamp = true,
                        onEdit = if (summary.isDemoMode) null else {
                            { editingPayment = tx }
                        },
                        onDelete = if (summary.isDemoMode) null else {
                            { deletingPayment = tx }
                        }
                    )
                }
            }
        }
    }

    if (showAddPaymentDialog) {
        RecordTransactionDialog(
            type = BusinessTransaction.TYPE_SALE,
            allowTypeToggle = true,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { type, title, category, amount, method, ref, status, date, notes ->
                onAddTransaction(type, title, category, amount, method, ref, status, date, notes)
                showAddPaymentDialog = false
            }
        )
    }

    editingPayment?.let { target ->
        RecordTransactionDialog(
            type = target.type,
            initialTransaction = target,
            allowTypeToggle = true,
            onDismiss = { editingPayment = null },
            onConfirm = { type, title, category, amount, method, ref, status, date, notes ->
                onUpdateTransaction(target.id, type, title, category, amount, method, ref, status, date, notes)
                editingPayment = null
            }
        )
    }

    deletingPayment?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Payment Record?",
            message = "Delete '${target.title}' (${formatKes(target.amountKes)}) from Firestore?",
            onDismiss = { deletingPayment = null },
            onConfirm = {
                onDeleteTransaction(target.id)
                deletingPayment = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordKeepingScreen(
    summary: DashboardSummary,
    transactionsState: UiState<List<BusinessTransaction>>,
    inventoryState: UiState<List<InventoryItem>>,
    customersState: UiState<List<CustomerRecord>> = UiState.Success(emptyList()),
    suppliersState: UiState<List<SupplierRecord>> = UiState.Success(emptyList()),
    stockMovementsState: UiState<List<StockMovementRecord>> = UiState.Success(emptyList()),
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onAddTransaction: (
        type: String,
        title: String,
        category: String,
        amount: String,
        method: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String
    ) -> Unit,
    onRecordPosSale: (
        title: String,
        category: String,
        paymentMethod: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String,
        customerId: String,
        customerName: String,
        inventoryItemId: String,
        quantityText: String,
        unitPriceText: String,
        discountText: String,
        manualAmountText: String
    ) -> Unit = { title, cat, method, ref, status, date, notes, _, _, _, _, _, _, manualAmt ->
        onAddTransaction(BusinessTransaction.TYPE_SALE, title, cat, manualAmt, method, ref, status, date, notes)
    },
    onCancelSale: (BusinessTransaction) -> Unit = {},
    onUpdateTransaction: (
        transactionId: String,
        type: String,
        title: String,
        category: String,
        amount: String,
        method: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String
    ) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddInventory: (
        name: String,
        sku: String,
        category: String,
        qty: String,
        reorder: String,
        price: String,
        unitCost: String,
        supplierId: String,
        supplierName: String
    ) -> Unit,
    onUpdateInventory: (
        itemId: String,
        name: String,
        sku: String,
        category: String,
        qty: String,
        reorder: String,
        price: String,
        unitCost: String,
        supplierId: String,
        supplierName: String
    ) -> Unit,
    onAdjustStock: (InventoryItem, Long) -> Unit,
    onDeleteInventory: (String) -> Unit,
    onRecordStockPurchase: (
        supplierId: String,
        supplierName: String,
        inventoryItemId: String,
        itemName: String,
        quantityText: String,
        unitCostText: String,
        paymentMethod: String,
        paymentStatus: String,
        purchaseDate: String,
        notes: String,
        logAsExpense: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPosSaleDialog by remember { mutableStateOf(false) }
    var showTransactionDialogType by remember { mutableStateOf<String?>(null) }
    var editingTransaction by remember { mutableStateOf<BusinessTransaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<BusinessTransaction?>(null) }
    var cancellingSale by remember { mutableStateOf<BusinessTransaction?>(null) }
    var receiptTransaction by remember { mutableStateOf<BusinessTransaction?>(null) }

    var showInventoryDialog by remember { mutableStateOf(false) }
    var editingInventory by remember { mutableStateOf<InventoryItem?>(null) }
    var deletingInventory by remember { mutableStateOf<InventoryItem?>(null) }
    var restockingItem by remember { mutableStateOf<InventoryItem?>(null) }

    var ledgerSearchQuery by remember { mutableStateOf("") }
    var ledgerDateFilter by remember { mutableStateOf(DateFilterOption.ALL) }
    var ledgerMethodFilter by remember { mutableStateOf("ALL") }
    var inventorySearchQuery by remember { mutableStateOf("") }
    var inventoryCategoryFilter by remember { mutableStateOf("ALL") }

    val liveTx = (transactionsState as? UiState.Success)?.data.orEmpty()
    val liveInv = (inventoryState as? UiState.Success)?.data.orEmpty()
    val liveCust = (customersState as? UiState.Success)?.data.orEmpty()
    val liveSup = (suppliersState as? UiState.Success)?.data.orEmpty()
    val liveMovements = (stockMovementsState as? UiState.Success)?.data.orEmpty()

    val txSource = if (summary.isDemoMode) DemoSampleData.sampleTransactions else liveTx
    val invSource = if (summary.isDemoMode) DemoSampleData.sampleInventory else liveInv
    val custSource = if (summary.isDemoMode) DemoSampleData.sampleCustomers else liveCust
    val supSource = if (summary.isDemoMode) DemoSampleData.sampleSuppliers else liveSup
    val movementsSource = if (summary.isDemoMode) DemoSampleData.sampleStockMovements else liveMovements

    val salesList = txSource.filter { it.type == BusinessTransaction.TYPE_SALE }
    val expenseList = txSource.filter { it.type == BusinessTransaction.TYPE_EXPENSE }
    val otherList = txSource.filter { it.type == BusinessTransaction.TYPE_OTHER }

    val filterTransactions: (List<BusinessTransaction>) -> List<BusinessTransaction> = { list ->
        val q = ledgerSearchQuery.trim().lowercase()
        list.filter { tx ->
            val matchesQuery = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.transactionRef.lowercase().contains(q) ||
                tx.notes.lowercase().contains(q) ||
                tx.effectiveDate().lowercase().contains(q)
            val matchesMethod = ledgerMethodFilter == "ALL" || tx.paymentMethod == ledgerMethodFilter
            val matchesDate = matchesDateFilter(tx.transactionDate, tx.createdAt, ledgerDateFilter)
            matchesQuery && matchesMethod && matchesDate
        }
    }

    val filteredSales = remember(salesList, ledgerSearchQuery, ledgerDateFilter, ledgerMethodFilter) {
        filterTransactions(salesList)
    }
    val filteredExpenses = remember(expenseList, ledgerSearchQuery, ledgerDateFilter, ledgerMethodFilter) {
        filterTransactions(expenseList)
    }
    val filteredOther = remember(otherList, ledgerSearchQuery, ledgerDateFilter, ledgerMethodFilter) {
        filterTransactions(otherList)
    }
    val filteredInventory = remember(invSource, inventorySearchQuery, inventoryCategoryFilter) {
        val q = inventorySearchQuery.trim().lowercase()
        invSource.filter { item ->
            val matchesQuery = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.sku.lowercase().contains(q) ||
                item.category.lowercase().contains(q)
            val matchesCat = inventoryCategoryFilter == "ALL" || item.category == inventoryCategoryFilter
            matchesQuery && matchesCat
        }
    }

    ModuleScaffoldContainer(
        module = AppModule.RECORD_KEEPING,
        onBackToDashboard = onBackToDashboard,
        onOpenAllModules = onOpenAllModules
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sales (${salesList.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("tab_records_sales")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Exp (${expenseList.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("tab_records_expenses")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Other (${otherList.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("tab_records_other")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Stock (${invSource.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("tab_records_inventory")
                )
            }

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
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Revenue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatKes(summary.totalSalesKes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandTeal)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatKes(summary.totalExpensesKes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AlertCoral)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Other Records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatKes(summary.otherTransactionsKes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandNavy)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Profit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = formatKes(summary.netBalanceKes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (summary.netBalanceKes >= 0) BrandNavy else AlertCoral
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedTab in 0..2) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchAndDateFilterBar(
                                query = ledgerSearchQuery,
                                onQueryChange = { ledgerSearchQuery = it },
                                selectedDateFilter = ledgerDateFilter,
                                onDateFilterChange = { ledgerDateFilter = it },
                                searchPlaceholder = "Search ledger by description, category, ref, or notes",
                                testTagPrefix = "records"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = ledgerMethodFilter == "ALL",
                                    onClick = { ledgerMethodFilter = "ALL" },
                                    label = { Text("All Methods") }
                                )
                                BusinessTransaction.allPaymentMethods.forEach { m ->
                                    FilterChip(
                                        selected = ledgerMethodFilter == m,
                                        onClick = { ledgerMethodFilter = m },
                                        label = { Text(m) }
                                    )
                                }
                            }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    title = "Sales & POS Ledger (IV3)",
                                    subtitle = "${filteredSales.size} of ${salesList.size} sales shown",
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showPosSaleDialog = true },
                                    modifier = Modifier.testTag("add_sale_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("POS Sale")
                                }
                            }
                        }
                        when {
                            transactionsState is UiState.Loading -> item { LoadingModulePlaceholder("Loading sales records...") }
                            transactionsState is UiState.Error -> item { ErrorModulePlaceholder(transactionsState.message) }
                            salesList.isEmpty() -> item {
                                EmptyModulePlaceholder(
                                    icon = Icons.Filled.PointOfSale,
                                    title = "No sales recorded yet",
                                    tip = "Tap 'POS Sale' to ring up items, apply discounts, deduct stock, and print receipts."
                                )
                            }
                            filteredSales.isEmpty() -> item {
                                EmptyModulePlaceholder(
                                    icon = Icons.Filled.Search,
                                    title = "No sales match your filters",
                                    tip = "Clear your search or reset date and payment method filters."
                                )
                            }
                            else -> items(filteredSales, key = { it.id }) { item ->
                                TransactionItemCard(
                                    item = item,
                                    isDemo = summary.isDemoMode,
                                    showTimestamp = true,
                                    onViewReceipt = { receiptTransaction = item },
                                    onCancelSale = if (summary.isDemoMode || item.isCancelled()) null else {
                                        { cancellingSale = item }
                                    },
                                    onEdit = if (summary.isDemoMode || item.isCancelled()) null else {
                                        { editingTransaction = item }
                                    },
                                    onDelete = if (summary.isDemoMode) null else {
                                        { deletingTransaction = item }
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    title = "Expense Ledger (IV3)",
                                    subtitle = "${filteredExpenses.size} of ${expenseList.size} expenses shown",
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showTransactionDialogType = BusinessTransaction.TYPE_EXPENSE },
                                    modifier = Modifier.testTag("add_expense_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DemoAmber)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Expense")
                                }
                            }
                        }
                        when {
                            transactionsState is UiState.Loading -> item { LoadingModulePlaceholder("Loading expense records...") }
                            transactionsState is UiState.Error -> item { ErrorModulePlaceholder(transactionsState.message) }
                            expenseList.isEmpty() -> item {
                                EmptyModulePlaceholder(
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                    title = "No expenses logged yet",
                                    tip = "Track rent, stock purchases, utilities, and wages in KES."
                                )
                            }
                            filteredExpenses.isEmpty() -> item {
                                EmptyModulePlaceholder(
                                    icon = Icons.Filled.Search,
                                    title = "No expenses match your filters",
                                    tip = "Clear your search or reset date and payment method filters."
                                )
                            }
                            else -> items(filteredExpenses, key = { it.id }) { item ->
                                TransactionItemCard(
                                    item = item,
                                    isDemo = summary.isDemoMode,
                                    showTimestamp = true,
                                    onEdit = if (summary.isDemoMode) null else {
                                        { editingTransaction = item }
                                    },
                                    onDelete = if (summary.isDemoMode) null else {
                                        { deletingTransaction = item }
                                    }
                                )
                            }
                        }
                    }

                    2 -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    title = "Other Business Records (IV3)",
                                    subtitle = "${filteredOther.size} of ${otherList.size} entries (permits, draws, capital, deposits)",
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showTransactionDialogType = BusinessTransaction.TYPE_OTHER },
                                    modifier = Modifier.testTag("add_other_record_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Other")
                                }
                            }
                        }
                        when {
                            transactionsState is UiState.Loading -> item { LoadingModulePlaceholder("Loading other business records...") }
                            transactionsState is UiState.Error -> item { ErrorModulePlaceholder(transactionsState.message) }
                            otherList.isEmpty() -> item {
                                EmptyModulePlaceholder(
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                    title = "No other business records yet",
                                    tip = "Record permits, owner draws, capital injections, or supplier deposits."
                                )
                            }
                            filteredOther.isEmpty() -> item {
                                EmptyModulePlaceholder(
                                    icon = Icons.Filled.Search,
                                    title = "No records match your filters",
                                    tip = "Clear your search or reset date and payment method filters."
                                )
                            }
                            else -> items(filteredOther, key = { it.id }) { item ->
                                TransactionItemCard(
                                    item = item,
                                    isDemo = summary.isDemoMode,
                                    showTimestamp = true,
                                    onEdit = if (summary.isDemoMode) null else {
                                        { editingTransaction = item }
                                    },
                                    onDelete = if (summary.isDemoMode) null else {
                                        { deletingTransaction = item }
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = BrandPurpleSoft.copy(alpha = 0.55f)),
                                border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Stock Retail Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatKes(summary.totalInventoryRetailValueKes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandNavy)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Stock Cost Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatKes(summary.totalInventoryCostValueKes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandPurple)
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    title = "Stock Records (IV3)",
                                    subtitle = "${invSource.size} SKUs • ${invSource.count { it.isLowStock }} low stock",
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showInventoryDialog = true },
                                    modifier = Modifier.testTag("add_inventory_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Stock")
                                }
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = inventorySearchQuery,
                                    onValueChange = { inventorySearchQuery = it },
                                    placeholder = { Text("Search stock by product name, SKU, or category") },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                                    trailingIcon = {
                                        if (inventorySearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { inventorySearchQuery = "" }) {
                                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("records_inventory_search_input")
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = inventoryCategoryFilter == "ALL",
                                        onClick = { inventoryCategoryFilter = "ALL" },
                                        label = { Text("All Categories") }
                                    )
                                    NakuruSmeCatalog.inventoryCategories.forEach { cat ->
                                        FilterChip(
                                            selected = inventoryCategoryFilter == cat,
                                            onClick = { inventoryCategoryFilter = cat },
                                            label = { Text(cat) }
                                        )
                                    }
                                }
                            }
                        }
                        when {
                            inventoryState is UiState.Loading -> item { LoadingModulePlaceholder("Loading inventory...") }
                            inventoryState is UiState.Error -> item { ErrorModulePlaceholder(inventoryState.message) }
                            invSource.isEmpty() -> item {
                                EmptyModulePlaceholder(
                                    icon = Icons.Filled.Inventory2,
                                    title = "Inventory catalog is empty",
                                    tip = "Add your products with KES cost & unit prices and reorder levels."
                                )
                            }
                            filteredInventory.isEmpty() -> item {
                                EmptyModulePlaceholder(
                                    icon = Icons.Filled.Search,
                                    title = "No stock items match your filter",
                                    tip = "Clear search text or select All Categories."
                                )
                            }
                            else -> items(filteredInventory, key = { it.id }) { stock ->
                                InventoryItemCard(
                                    item = stock,
                                    isDemo = summary.isDemoMode,
                                    onRestock = if (summary.isDemoMode) null else {
                                        { restockingItem = stock }
                                    },
                                    onEdit = if (summary.isDemoMode) null else {
                                        { editingInventory = stock }
                                    },
                                    onIncrement = { if (!summary.isDemoMode) onAdjustStock(stock, 1L) },
                                    onDecrement = { if (!summary.isDemoMode) onAdjustStock(stock, -1L) },
                                    onDelete = if (summary.isDemoMode) null else {
                                        { deletingInventory = stock }
                                    }
                                )
                            }
                        }
                        if (movementsSource.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Recent Stock Movements",
                                    subtitle = "Immutable audit trail of POS deductions, restocks & voids"
                                )
                            }
                            items(movementsSource.take(8), key = { it.id }) { mv ->
                                StockMovementCard(movement = mv, isDemo = summary.isDemoMode)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPosSaleDialog) {
        PosSaleDialog(
            inventory = invSource,
            customers = custSource,
            businessLocation = summary.locationNakuru,
            onDismiss = { showPosSaleDialog = false },
            onConfirmPosSale = { title, cat, method, ref, status, date, notes, custId, custName, invId, qtyText, unitPriceText, discText, manualAmt ->
                onRecordPosSale(title, cat, method, ref, status, date, notes, custId, custName, invId, qtyText, unitPriceText, discText, manualAmt)
                showPosSaleDialog = false
            }
        )
    }

    if (showTransactionDialogType != null) {
        RecordTransactionDialog(
            type = showTransactionDialogType!!,
            onDismiss = { showTransactionDialogType = null },
            onConfirm = { selectedType, title, category, amount, method, ref, status, date, notes ->
                onAddTransaction(selectedType, title, category, amount, method, ref, status, date, notes)
                showTransactionDialogType = null
            }
        )
    }

    editingTransaction?.let { target ->
        RecordTransactionDialog(
            type = target.type,
            initialTransaction = target,
            allowTypeToggle = true,
            onDismiss = { editingTransaction = null },
            onConfirm = { selectedType, title, category, amount, method, ref, status, date, notes ->
                onUpdateTransaction(target.id, selectedType, title, category, amount, method, ref, status, date, notes)
                editingTransaction = null
            }
        )
    }

    receiptTransaction?.let { target ->
        ReceiptPreviewDialog(
            transaction = target,
            businessName = summary.businessName,
            businessLocation = summary.locationNakuru,
            onDismiss = { receiptTransaction = null }
        )
    }

    cancellingSale?.let { target ->
        ConfirmDeleteDialog(
            title = "Cancel / Void Sale?",
            message = "Mark '${target.title}' (${formatKes(target.amountKes)}) as CANCELLED and atomically restore linked inventory stock & customer spend?",
            confirmLabel = "Void Sale",
            onDismiss = { cancellingSale = null },
            onConfirm = {
                onCancelSale(target)
                cancellingSale = null
            }
        )
    }

    deletingTransaction?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Record?",
            message = "Permanently remove '${target.title}' (${formatKes(target.amountKes)}) from Firestore?",
            onDismiss = { deletingTransaction = null },
            onConfirm = {
                onDeleteTransaction(target.id)
                deletingTransaction = null
            }
        )
    }

    if (showInventoryDialog) {
        InventoryFormDialog(
            initial = null,
            suppliers = supSource,
            onDismiss = { showInventoryDialog = false },
            onConfirm = { name, sku, category, qty, reorder, price, unitCost, supId, supName ->
                onAddInventory(name, sku, category, qty, reorder, price, unitCost, supId, supName)
                showInventoryDialog = false
            }
        )
    }

    editingInventory?.let { target ->
        InventoryFormDialog(
            initial = target,
            suppliers = supSource,
            onDismiss = { editingInventory = null },
            onConfirm = { name, sku, category, qty, reorder, price, unitCost, supId, supName ->
                onUpdateInventory(target.id, name, sku, category, qty, reorder, price, unitCost, supId, supName)
                editingInventory = null
            }
        )
    }

    deletingInventory?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Product?",
            message = "Remove '${target.name}' (${target.quantity} units) from inventory?",
            onDismiss = { deletingInventory = null },
            onConfirm = {
                onDeleteInventory(target.id)
                deletingInventory = null
            }
        )
    }

    restockingItem?.let { target ->
        StockPurchaseDialog(
            suppliers = supSource,
            inventory = invSource,
            initialSupplier = supSource.firstOrNull { it.id == target.supplierId },
            initialInventoryItem = target,
            onDismiss = { restockingItem = null },
            onConfirm = { supId, supName, invId, itemName, qtyText, costText, method, status, date, notes, logExp ->
                onRecordStockPurchase(supId, supName, invId, itemName, qtyText, costText, method, status, date, notes, logExp)
                restockingItem = null
            }
        )
    }
}

@Composable
fun OnlineCommunicationScreen(
    summary: DashboardSummary,
    communicationsState: UiState<List<OnlineCommunicationLog>>,
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onAddCommunicationLog: (
        customerName: String,
        channel: String,
        subject: String,
        receivedDate: String,
        responseDate: String,
        status: String,
        responseMinutes: String,
        notes: String,
        category: String
    ) -> Unit,
    onUpdateCommunicationLog: (
        commId: String,
        customerName: String,
        channel: String,
        subject: String,
        receivedDate: String,
        responseDate: String,
        status: String,
        responseMinutes: String,
        notes: String,
        category: String
    ) -> Unit,
    onUpdateCommunicationStatus: (String, String) -> Unit,
    onDeleteCommunicationLog: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<OnlineCommunicationLog?>(null) }
    var deletingLog by remember { mutableStateOf<OnlineCommunicationLog?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf(DateFilterOption.ALL) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedChannelFilter by remember { mutableStateOf("ALL") }

    val liveLogs = (communicationsState as? UiState.Success)?.data.orEmpty()
    val logs = if (summary.isDemoMode) DemoSampleData.sampleCommunications else liveLogs
    val filteredLogs = remember(
        logs,
        searchQuery,
        selectedDateFilter,
        selectedStatusFilter,
        selectedChannelFilter
    ) {
        val q = searchQuery.trim().lowercase()
        logs.filter { log ->
            val matchesQuery = q.isEmpty() ||
                log.customerName.lowercase().contains(q) ||
                log.subject.lowercase().contains(q) ||
                log.channel.lowercase().contains(q) ||
                log.notes.lowercase().contains(q) ||
                log.effectiveReceivedDate().lowercase().contains(q)
            val matchesStatus = selectedStatusFilter == "ALL" || log.status == selectedStatusFilter
            val matchesChannel = selectedChannelFilter == "ALL" || log.channel == selectedChannelFilter
            val matchesDate = matchesDateFilter(log.receivedDate, log.createdAt, selectedDateFilter)
            matchesQuery && matchesStatus && matchesChannel && matchesDate
        }
    }

    ModuleScaffoldContainer(
        module = AppModule.ONLINE_COMMUNICATION,
        onBackToDashboard = onBackToDashboard,
        onOpenAllModules = onOpenAllModules
    ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Avg Response Speed",
                        valueText = "${summary.avgResponseMinutes} min",
                        subtitle = "${summary.respondedOrConvertedComms}/${summary.communicationCount} answered",
                        icon = Icons.Filled.Forum,
                        accentColor = BrandTeal,
                        isDemoBadge = summary.isDemoMode,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Converted Enquiries",
                        valueText = "${summary.convertedCustomerLeads}",
                        subtitle = "Across digital chat channels",
                        icon = Icons.Filled.PointOfSale,
                        accentColor = BrandNavy,
                        isDemoBadge = summary.isDemoMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Customer Communication Log (IV4)",
                        subtitle = "Track WhatsApp, Instagram, Facebook, Email, Website & SMS enquiries",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_communication_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Inquiry")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchAndDateFilterBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedDateFilter = selectedDateFilter,
                        onDateFilterChange = { selectedDateFilter = it },
                        searchPlaceholder = "Search by customer name, enquiry, channel, or notes",
                        testTagPrefix = "comms"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatusFilter == "ALL",
                            onClick = { selectedStatusFilter = "ALL" },
                            label = { Text("All Statuses") }
                        )
                        OnlineCommunicationLog.statuses.forEach { st ->
                            FilterChip(
                                selected = selectedStatusFilter == st,
                                onClick = { selectedStatusFilter = st },
                                label = { Text(st) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedChannelFilter == "ALL",
                            onClick = { selectedChannelFilter = "ALL" },
                            label = { Text("All Channels") }
                        )
                        OnlineCommunicationLog.channels.forEach { ch ->
                            FilterChip(
                                selected = selectedChannelFilter == ch,
                                onClick = { selectedChannelFilter = ch },
                                label = { Text(ch) }
                            )
                        }
                    }
                }
            }

            when {
                communicationsState is UiState.Loading -> item { LoadingModulePlaceholder("Loading online communication logs...") }
                communicationsState is UiState.Error -> item { ErrorModulePlaceholder(communicationsState.message) }
                logs.isEmpty() -> item {
                    EmptyModulePlaceholder(
                        icon = Icons.Filled.Forum,
                        title = "No customer communications logged",
                        tip = "Tap 'Log Inquiry' to record customer enquiries, channels, response speed, and conversions."
                    )
                }
                filteredLogs.isEmpty() -> item {
                    EmptyModulePlaceholder(
                        icon = Icons.Filled.Search,
                        title = "No communications match your filters",
                        tip = "Clear your search or switch date, channel, or status filters."
                    )
                }
                else -> items(filteredLogs, key = { it.id }) { log ->
                    CommunicationLogCard(
                        log = log,
                        isDemo = summary.isDemoMode,
                        onEdit = if (summary.isDemoMode) null else {
                            { editingLog = log }
                        },
                        onUpdateStatus = if (summary.isDemoMode) null else { newStatus ->
                            onUpdateCommunicationStatus(log.id, newStatus)
                        },
                        onDelete = if (summary.isDemoMode) null else {
                            { deletingLog = log }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CommunicationFormDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, channel, category, subject, recDate, respDate, status, mins, notes ->
                onAddCommunicationLog(name, channel, subject, recDate, respDate, status, mins, notes, category)
                showAddDialog = false
            }
        )
    }

    editingLog?.let { target ->
        CommunicationFormDialog(
            initial = target,
            onDismiss = { editingLog = null },
            onConfirm = { name, channel, category, subject, recDate, respDate, status, mins, notes ->
                onUpdateCommunicationLog(target.id, name, channel, subject, recDate, respDate, status, mins, notes, category)
                editingLog = null
            }
        )
    }

    deletingLog?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Communication Log?",
            message = "Remove communication log for ${target.customerName} (${target.channel})?",
            onDismiss = { deletingLog = null },
            onConfirm = {
                onDeleteCommunicationLog(target.id)
                deletingLog = null
            }
        )
    }
}

@Composable
private fun ModuleScaffoldContainer(
    module: AppModule,
    onBackToDashboard: () -> Unit,
    onOpenAllModules: () -> Unit,
    content: @Composable () -> Unit
) {
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
                module = module,
                onBackToDashboard = onBackToDashboard,
                onOpenAllModules = onOpenAllModules
            )
            content()
        }
    }
}

@Composable
private fun CampaignCard(
    campaign: DigitalMarketingCampaign,
    isDemo: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(campaign.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (isDemo) {
                            Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                                Text(
                                    text = "DEMO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DemoAmberDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${campaign.channel} • ${campaign.status} • ${campaign.effectiveDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_campaign_${campaign.id}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit campaign", tint = BrandNavy)
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete campaign")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricMini("Spend", formatKes(campaign.spendKes))
                MetricMini("Reach", "${campaign.reachCount}")
                MetricMini("Engagement", "${campaign.engagementCount} (${String.format(Locale.US, "%.1f", campaign.engagementRatePct)}%)")
                MetricMini("Leads", "${campaign.inquiriesGenerated}")
            }

            if (campaign.notes.isNotBlank()) {
                Text(
                    text = campaign.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunicationLogCard(
    log: OnlineCommunicationLog,
    isDemo: Boolean,
    onEdit: (() -> Unit)?,
    onUpdateStatus: ((String) -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val (statusBg, statusFg) = when (log.status) {
        OnlineCommunicationLog.STATUS_CONVERTED -> BrandTealLight to BrandTealDark
        OnlineCommunicationLog.STATUS_RESPONDED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> DemoAmberLight to DemoAmberDark
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(log.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(6.dp), color = BrandPurpleSoft) {
                            Text(
                                text = log.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (isDemo) {
                            Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                                Text(
                                    text = "DEMO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DemoAmberDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append("${log.channel} • Received: ${log.effectiveReceivedDate()} • Reply: ${log.responseMinutes} min")
                            if (log.responseDate.isNotBlank()) append(" (Responded: ${log.responseDate})")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                        Text(
                            text = log.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_comm_${log.id}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit communication log", tint = BrandNavy)
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete inquiry log")
                        }
                    }
                }
            }
            Text(log.subject, style = MaterialTheme.typography.bodyMedium)
            if (log.notes.isNotBlank()) {
                Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onUpdateStatus != null && log.status != OnlineCommunicationLog.STATUS_CONVERTED) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (log.status == OnlineCommunicationLog.STATUS_PENDING) {
                        OutlinedButton(onClick = { onUpdateStatus(OnlineCommunicationLog.STATUS_RESPONDED) }) {
                            Text("Mark Responded")
                        }
                    }
                    OutlinedButton(onClick = { onUpdateStatus(OnlineCommunicationLog.STATUS_CONVERTED) }) {
                        Text("Mark Converted")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaymentChannelBar(
    label: String,
    amountKes: Double,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(formatKes(amountKes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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

@Composable
private fun TransactionItemCard(
    item: BusinessTransaction,
    isDemo: Boolean,
    showTimestamp: Boolean = false,
    onViewReceipt: (() -> Unit)? = null,
    onCancelSale: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)?
) {
    val isSale = item.type == BusinessTransaction.TYPE_SALE
    val isOther = item.type == BusinessTransaction.TYPE_OTHER
    val isCancelled = item.isCancelled()
    val amountPrefix = when {
        isCancelled -> ""
        item.type == BusinessTransaction.TYPE_SALE -> "+"
        item.type == BusinessTransaction.TYPE_EXPENSE -> "-"
        else -> ""
    }
    val amountColor = when {
        isCancelled -> MaterialTheme.colorScheme.onSurfaceVariant
        isSale -> BrandTeal
        isOther -> BrandNavy
        else -> AlertCoral
    }
    val (statusBg, statusFg) = when (item.paymentStatus) {
        BusinessTransaction.STATUS_FAILED, BusinessTransaction.STATUS_CANCELLED ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        BusinessTransaction.STATUS_PENDING -> DemoAmberLight to DemoAmberDark
        else -> BrandTealLight to BrandTealDark
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (item.receiptNumber.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(6.dp), color = BrandPurpleSoft) {
                                Text(
                                    text = item.receiptNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPurple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (isDemo) {
                            Surface(shape = RoundedCornerShape(6.dp), color = DemoAmberLight) {
                                Text(
                                    text = "DEMO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DemoAmberDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append("${item.type} • ${item.category} • ${item.paymentMethod}")
                            if (showTimestamp) append(" • ${item.effectiveDate()}")
                            if (item.customerName.isNotBlank()) append(" • ${item.customerName}")
                            if (item.quantitySold > 0L) append(" • ×${item.quantitySold}")
                            if (item.transactionRef.isNotBlank()) append(" • Ref: ${item.transactionRef}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$amountPrefix${formatKes(item.amountKes)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                        Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                            Text(
                                text = item.paymentStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_transaction_${item.id}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit entry", tint = BrandNavy)
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onViewReceipt != null || onCancelSale != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onViewReceipt != null) {
                        OutlinedButton(onClick = onViewReceipt, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receipt")
                        }
                    }
                    if (onCancelSale != null) {
                        OutlinedButton(onClick = onCancelSale, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp), tint = AlertCoral)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Void Sale", color = AlertCoral)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    isDemo: Boolean,
    onRestock: (() -> Unit)? = null,
    onEdit: (() -> Unit)?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (item.isLowStock) AlertCoral.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (item.isLowStock) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                Text(
                                    text = "LOW STOCK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append("${item.category} • Price: ${formatKes(item.unitPriceKes)}")
                            if (item.unitCostKes > 0.0) append(" • Cost: ${formatKes(item.unitCostKes)}")
                            if (item.supplierName.isNotBlank()) append(" • ${item.supplierName}")
                            if (item.sku.isNotBlank()) append(" • SKU: ${item.sku}")
                            if (isDemo) append(" • DEMO")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isDemo) {
                        IconButton(onClick = onDecrement) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease stock")
                        }
                    }
                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    if (!isDemo) {
                        IconButton(onClick = onIncrement) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase stock")
                        }
                        if (onEdit != null) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.testTag("edit_inventory_${item.id}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit stock item", tint = BrandNavy)
                            }
                        }
                        if (onDelete != null) {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete stock item")
                            }
                        }
                    }
                }
            }
            if (onRestock != null) {
                OutlinedButton(onClick = onRestock, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restock Purchase Order")
                }
            }
        }
    }
}

@Composable
private fun StockMovementCard(movement: StockMovementRecord, isDemo: Boolean) {
    val isInflow = movement.quantityDelta >= 0L
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${movement.itemName} (${movement.movementType})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = buildString {
                        append("${movement.effectiveDate()} • Bal: ${movement.balanceAfter}")
                        if (movement.supplierName.isNotBlank()) append(" • ${movement.supplierName}")
                        if (movement.referenceId.isNotBlank()) append(" • Ref: ${movement.referenceId}")
                        if (isDemo) append(" • DEMO")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${if (isInflow) "+" else ""}${movement.quantityDelta}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isInflow) BrandTeal else AlertCoral
            )
        }
    }
}

@Composable
fun RecordTransactionDialog(
    type: String,
    initialTransaction: BusinessTransaction? = null,
    allowTypeToggle: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        type: String,
        title: String,
        category: String,
        amount: String,
        method: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String
    ) -> Unit
) {
    val isEditing = initialTransaction != null
    val isLinkedPosSale = isEditing && (
        initialTransaction?.inventoryItemId?.isNotBlank() == true ||
            initialTransaction?.receiptNumber?.isNotBlank() == true
        )
    var selectedType by remember(initialTransaction, type) {
        mutableStateOf(initialTransaction?.type ?: type)
    }

    val categoriesForType: (String) -> List<String> = { txType ->
        when (txType) {
            BusinessTransaction.TYPE_SALE -> NakuruSmeCatalog.saleCategories
            BusinessTransaction.TYPE_EXPENSE -> NakuruSmeCatalog.expenseCategories
            else -> NakuruSmeCatalog.otherCategories
        }
    }

    var title by remember(initialTransaction) { mutableStateOf(initialTransaction?.title ?: "") }
    var category by remember(initialTransaction, selectedType) {
        val validList = categoriesForType(initialTransaction?.type ?: type)
        val initialCat = initialTransaction?.category.orEmpty()
        mutableStateOf(if (initialCat in validList) initialCat else validList.first())
    }
    var amount by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.amountKes?.let {
            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
        } ?: "")
    }
    var paymentMethod by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.paymentMethod ?: BusinessTransaction.METHOD_MPESA)
    }
    var transactionRef by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.transactionRef ?: "")
    }
    var paymentStatus by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.paymentStatus ?: BusinessTransaction.STATUS_COMPLETED)
    }
    var transactionDate by remember(initialTransaction) {
        mutableStateOf(initialTransaction?.effectiveDate() ?: todayIsoDate())
    }
    var notes by remember(initialTransaction) { mutableStateOf(initialTransaction?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val activeCategories = categoriesForType(selectedType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    isEditing -> "Edit Record (KES)"
                    allowTypeToggle -> "Log Digital / Cash Payment (KES)"
                    selectedType == BusinessTransaction.TYPE_SALE -> "Record Sale (KES)"
                    selectedType == BusinessTransaction.TYPE_EXPENSE -> "Record Expense (KES)"
                    else -> "Add Other Business Record (KES)"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (allowTypeToggle || isEditing) {
                    Text("Record Type", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BusinessTransaction.allTypes.forEach { t ->
                            FilterChip(
                                selected = selectedType == t,
                                enabled = !isLinkedPosSale || t == selectedType,
                                onClick = {
                                    selectedType = t
                                    val nextCats = categoriesForType(t)
                                    if (category !in nextCats) category = nextCats.first()
                                },
                                label = { Text(t) }
                            )
                        }
                    }
                    if (isLinkedPosSale) {
                        Text(
                            text = "POS inventory-linked sale type is locked to protect stock & customer totals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Description *") },
                    placeholder = {
                        Text(
                            when (selectedType) {
                                BusinessTransaction.TYPE_SALE -> "e.g., M-Pesa Till Counter Sales"
                                BusinessTransaction.TYPE_EXPENSE -> "e.g., Stock Restock"
                                else -> "e.g., County Business Permit Renewal"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_tx_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            validationError = null
                        },
                        label = { Text("Amount (KES) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_tx_amount_input")
                    )
                    OutlinedTextField(
                        value = transactionDate,
                        onValueChange = {
                            transactionDate = it
                            validationError = null
                        },
                        label = { Text("Date (YYYY-MM-DD) *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_tx_date_input")
                    )
                }
                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text("Transaction Ref / Receipt (Optional)") },
                    placeholder = { Text("e.g., TI84MP921K or EFT-20419") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_tx_ref_input")
                )
                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessTransaction.allPaymentMethods.forEach { m ->
                        FilterChip(
                            selected = paymentMethod == m,
                            onClick = { paymentMethod = m },
                            label = { Text(m) }
                        )
                    }
                }
                Text("Payment Status", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessTransaction.paymentStatuses
                        .filter { it != BusinessTransaction.STATUS_CANCELLED || paymentStatus == BusinessTransaction.STATUS_CANCELLED }
                        .forEach { st ->
                            FilterChip(
                                selected = paymentStatus == st,
                                enabled = st != BusinessTransaction.STATUS_CANCELLED,
                                onClick = { paymentStatus = st },
                                label = { Text(st) }
                            )
                        }
                }
                if (selectedType == BusinessTransaction.TYPE_SALE) {
                    Text(
                        text = "Use 'Void Sale' on the transaction card to cancel a sale and restore stock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.trim().toDoubleOrNull()
                    when {
                        title.isBlank() -> {
                            validationError = "Please enter a transaction description."
                        }
                        parsedAmount == null || parsedAmount <= 0.0 -> {
                            validationError = "Enter a valid positive KES amount."
                        }
                        !isValidIsoDate(transactionDate) -> {
                            validationError = "Use YYYY-MM-DD format for date (e.g., ${todayIsoDate()})."
                        }
                        else -> {
                            validationError = null
                            onConfirm(
                                selectedType,
                                title.trim(),
                                category,
                                amount.trim(),
                                paymentMethod,
                                transactionRef.trim(),
                                paymentStatus,
                                transactionDate.trim(),
                                notes.trim()
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("dialog_tx_save_button")
            ) {
                Text(if (isEditing) "Update Record" else "Save to Cloud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InventoryFormDialog(
    initial: InventoryItem?,
    suppliers: List<SupplierRecord> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        sku: String,
        category: String,
        qty: String,
        reorder: String,
        price: String,
        unitCost: String,
        supplierId: String,
        supplierName: String
    ) -> Unit
) {
    val isEditing = initial != null
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var sku by remember(initial) { mutableStateOf(initial?.sku ?: "") }
    var category by remember(initial) {
        mutableStateOf(initial?.category ?: NakuruSmeCatalog.inventoryCategories.first())
    }
    var qty by remember(initial) { mutableStateOf(initial?.quantity?.toString() ?: "10") }
    var reorder by remember(initial) { mutableStateOf(initial?.reorderLevel?.toString() ?: "5") }
    var price by remember(initial) {
        mutableStateOf(initial?.unitPriceKes?.let {
            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
        } ?: "")
    }
    var unitCost by remember(initial) {
        mutableStateOf(initial?.unitCostKes?.takeIf { it > 0.0 }?.let {
            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
        } ?: "0")
    }
    var selectedSupplier by remember(initial, suppliers) {
        mutableStateOf(suppliers.firstOrNull { it.id == initial?.supplierId })
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Stock Item" else "Add Stock Item") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = {
                            qty = it
                            validationError = null
                        },
                        label = { Text("Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reorder,
                        onValueChange = {
                            reorder = it
                            validationError = null
                        },
                        label = { Text("Min Alert *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = {
                            price = it
                            validationError = null
                        },
                        label = { Text("Sell Price (KES) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitCost,
                        onValueChange = {
                            unitCost = it
                            validationError = null
                        },
                        label = { Text("Unit Cost (KES)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU / Code (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (suppliers.isNotEmpty()) {
                    Text("Preferred Supplier", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSupplier == null,
                            onClick = { selectedSupplier = null },
                            label = { Text("None") }
                        )
                        suppliers.forEach { sup ->
                            FilterChip(
                                selected = selectedSupplier?.id == sup.id,
                                onClick = { selectedSupplier = sup },
                                label = { Text(sup.name) }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NakuruSmeCatalog.inventoryCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedQty = qty.trim().toLongOrNull()
                    val parsedReorder = reorder.trim().toLongOrNull()
                    val parsedPrice = price.trim().toDoubleOrNull()
                    val parsedCost = unitCost.trim().toDoubleOrNull() ?: 0.0
                    when {
                        name.isBlank() -> validationError = "Enter product name."
                        parsedQty == null || parsedQty < 0L -> validationError = "Enter non-negative quantity."
                        parsedReorder == null || parsedReorder < 0L -> validationError = "Enter non-negative alert level."
                        parsedPrice == null || parsedPrice < 0.0 -> validationError = "Enter valid KES unit price."
                        parsedCost < 0.0 -> validationError = "Unit cost cannot be negative."
                        else -> {
                            validationError = null
                            onConfirm(
                                name.trim(),
                                sku.trim(),
                                category,
                                qty.trim(),
                                reorder.trim(),
                                price.trim(),
                                unitCost.trim().ifBlank { "0" },
                                selectedSupplier?.id ?: "",
                                selectedSupplier?.name ?: ""
                            )
                        }
                    }
                }
            ) {
                Text(if (isEditing) "Update Item" else "Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CampaignFormDialog(
    initial: DigitalMarketingCampaign?,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        channel: String,
        campaignDate: String,
        spend: String,
        reach: String,
        engagement: String,
        inquiries: String,
        status: String,
        notes: String
    ) -> Unit
) {
    val isEditing = initial != null
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var channel by remember(initial) {
        mutableStateOf(initial?.channel ?: DigitalMarketingCampaign.channels.first())
    }
    var campaignDate by remember(initial) {
        mutableStateOf(initial?.effectiveDate() ?: todayIsoDate())
    }
    var spend by remember(initial) {
        mutableStateOf(initial?.spendKes?.let {
            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
        } ?: "500")
    }
    var reach by remember(initial) { mutableStateOf(initial?.reachCount?.toString() ?: "600") }
    var engagement by remember(initial) { mutableStateOf(initial?.engagementCount?.toString() ?: "80") }
    var inquiries by remember(initial) { mutableStateOf(initial?.inquiriesGenerated?.toString() ?: "15") }
    var status by remember(initial) {
        mutableStateOf(initial?.status ?: DigitalMarketingCampaign.STATUS_ACTIVE)
    }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Marketing Campaign" else "Log Digital Marketing Campaign") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Campaign Title *") },
                    placeholder = { Text("e.g., Nakuru End-Month Promo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = campaignDate,
                    onValueChange = {
                        campaignDate = it
                        validationError = null
                    },
                    label = { Text("Campaign Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Platform / Channel", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DigitalMarketingCampaign.channels.forEach { ch ->
                        FilterChip(
                            selected = channel == ch,
                            onClick = { channel = ch },
                            label = { Text(ch) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = spend,
                        onValueChange = {
                            spend = it
                            validationError = null
                        },
                        label = { Text("Spend (KES)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reach,
                        onValueChange = {
                            reach = it
                            validationError = null
                        },
                        label = { Text("Reach") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = engagement,
                        onValueChange = {
                            engagement = it
                            validationError = null
                        },
                        label = { Text("Engagements") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = inquiries,
                        onValueChange = {
                            inquiries = it
                            validationError = null
                        },
                        label = { Text("Leads") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DigitalMarketingCampaign.statuses.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedSpend = spend.trim().toDoubleOrNull()
                    val parsedReach = reach.trim().toLongOrNull()
                    val parsedEng = engagement.trim().toLongOrNull()
                    val parsedInq = inquiries.trim().toLongOrNull()
                    when {
                        title.isBlank() -> validationError = "Enter a campaign title."
                        !isValidIsoDate(campaignDate) -> validationError = "Use YYYY-MM-DD format for date."
                        parsedSpend == null || parsedSpend < 0.0 -> validationError = "Spend must be 0 or higher."
                        parsedReach == null || parsedReach < 0L || parsedEng == null || parsedEng < 0L || parsedInq == null || parsedInq < 0L -> {
                            validationError = "Reach, engagements, and leads must be non-negative integers."
                        }
                        else -> {
                            validationError = null
                            onConfirm(
                                title.trim(),
                                channel,
                                campaignDate.trim(),
                                spend.trim(),
                                reach.trim(),
                                engagement.trim(),
                                inquiries.trim(),
                                status,
                                notes.trim()
                            )
                        }
                    }
                }
            ) {
                Text(if (isEditing) "Update Campaign" else "Save Campaign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CommunicationFormDialog(
    initial: OnlineCommunicationLog?,
    onDismiss: () -> Unit,
    onConfirm: (
        customerName: String,
        channel: String,
        category: String,
        subject: String,
        receivedDate: String,
        responseDate: String,
        status: String,
        responseMinutes: String,
        notes: String
    ) -> Unit
) {
    val isEditing = initial != null
    var customerName by remember(initial) { mutableStateOf(initial?.customerName ?: "") }
    var channel by remember(initial) {
        mutableStateOf(initial?.channel ?: OnlineCommunicationLog.channels.first())
    }
    var category by remember(initial) {
        mutableStateOf(initial?.category ?: OnlineCommunicationLog.CATEGORY_INQUIRY)
    }
    var subject by remember(initial) { mutableStateOf(initial?.subject ?: "") }
    var receivedDate by remember(initial) {
        mutableStateOf(initial?.effectiveReceivedDate() ?: todayIsoDate())
    }
    var responseDate by remember(initial) { mutableStateOf(initial?.responseDate ?: "") }
    var responseMinutes by remember(initial) {
        mutableStateOf(initial?.responseMinutes?.toString() ?: "10")
    }
    var status by remember(initial) {
        mutableStateOf(initial?.status ?: OnlineCommunicationLog.STATUS_RESPONDED)
    }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Customer Communication" else "Log Customer Communication") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        validationError = null
                    },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        validationError = null
                    },
                    label = { Text("Enquiry / Topic *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Communication Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OnlineCommunicationLog.categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = receivedDate,
                        onValueChange = {
                            receivedDate = it
                            validationError = null
                        },
                        label = { Text("Received Date *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = responseDate,
                        onValueChange = {
                            responseDate = it
                            validationError = null
                        },
                        label = { Text("Response Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Digital Channel", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OnlineCommunicationLog.channels.forEach { ch ->
                        FilterChip(
                            selected = channel == ch,
                            onClick = { channel = ch },
                            label = { Text(ch) }
                        )
                    }
                }
                OutlinedTextField(
                    value = responseMinutes,
                    onValueChange = {
                        responseMinutes = it
                        validationError = null
                    },
                    label = { Text("Response Time (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OnlineCommunicationLog.statuses.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = {
                                status = st
                                if (st != OnlineCommunicationLog.STATUS_PENDING && responseDate.isBlank()) {
                                    responseDate = todayIsoDate()
                                }
                            },
                            label = { Text(st) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Engagement Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedMins = responseMinutes.trim().toLongOrNull()
                    when {
                        customerName.isBlank() -> validationError = "Enter customer name."
                        subject.isBlank() -> validationError = "Enter the customer enquiry or topic."
                        !isValidIsoDate(receivedDate) -> validationError = "Use YYYY-MM-DD format for received date."
                        responseDate.isNotBlank() && !isValidIsoDate(responseDate) -> validationError = "Use YYYY-MM-DD format for response date."
                        parsedMins == null || parsedMins < 0L -> validationError = "Response minutes must be 0 or higher."
                        else -> {
                            validationError = null
                            onConfirm(
                                customerName.trim(),
                                channel,
                                category,
                                subject.trim(),
                                receivedDate.trim(),
                                responseDate.trim(),
                                status,
                                responseMinutes.trim(),
                                notes.trim()
                            )
                        }
                    }
                }
            ) {
                Text(if (isEditing) "Update Log" else "Save Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
