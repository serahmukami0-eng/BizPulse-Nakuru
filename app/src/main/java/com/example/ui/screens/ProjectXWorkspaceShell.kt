package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.analytics.Prompt4AnalyticsBundle
import com.example.data.analytics.SmeFullAnalyticsReport
import com.example.data.backup.BackupSummary
import com.example.data.model.AnalyticsDateFilter
import com.example.data.model.AppModule
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.PosCartLineItem
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.StockMovementRecord
import com.example.data.model.SupplierRecord
import com.example.data.network.ConnectivityObserver
import com.example.data.repository.RoomBusinessOverviewState
import com.example.ui.components.ModuleTileCard
import com.example.ui.components.OfflineSyncStatusBanner
import com.example.ui.components.ProjectXLoadingScreen
import com.example.ui.components.moduleIcon
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleSoft
import com.example.ui.theme.BrandTeal
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiResponseState
import com.example.ui.viewmodel.BackupRestoreUiState
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.GrowthAnalysisUiState
import com.example.ui.viewmodel.SalesOverviewAiState
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.flow.StateFlow

@Stable
interface ProjectXWorkspaceHost {
    val dashboardSummary: StateFlow<DashboardSummary>
    val analyticsReport: StateFlow<SmeFullAnalyticsReport>
    val prompt4Analytics: StateFlow<Prompt4AnalyticsBundle>
    val forecastHorizonMonths: StateFlow<Int>
    val roomOverviewState: StateFlow<RoomBusinessOverviewState>
    val salesOverviewAiState: StateFlow<SalesOverviewAiState>
    val profileState: StateFlow<UiState<BusinessProfile?>>
    val transactionsState: StateFlow<UiState<List<BusinessTransaction>>>
    val inventoryState: StateFlow<UiState<List<InventoryItem>>>
    val campaignsState: StateFlow<UiState<List<DigitalMarketingCampaign>>>
    val communicationsState: StateFlow<UiState<List<OnlineCommunicationLog>>>
    val customersState: StateFlow<UiState<List<CustomerRecord>>>
    val suppliersState: StateFlow<UiState<List<SupplierRecord>>>
    val purchasesState: StateFlow<UiState<List<PurchaseOrderRecord>>>
    val stockMovementsState: StateFlow<UiState<List<StockMovementRecord>>>
    val aiState: StateFlow<AiResponseState>
    val growthAnalysisState: StateFlow<GrowthAnalysisUiState>
    val aiMessages: StateFlow<List<AiChatMessage>>
    val actionMessage: StateFlow<String?>
    val isSaving: StateFlow<Boolean>
    val hasPendingSyncWrites: StateFlow<Boolean>
    val backupRestoreState: StateFlow<BackupRestoreUiState>
    val latestPosReceipt: StateFlow<List<BusinessTransaction>?>

    fun clearActionMessage()
    fun dismissLatestPosReceipt()
    fun setAnalyticsDateFilter(filter: AnalyticsDateFilter)
    fun setCustomDateRange(startIso: String, endIso: String)
    fun toggleDemoMode(enabled: Boolean)
    fun refreshRoomOverviewCache()
    fun generateOverviewSalesSummary()
    fun clearOverviewSalesSummary()
    fun addCampaign(
        title: String,
        channel: String,
        campaignDate: String,
        spendText: String,
        reachText: String,
        engagementText: String,
        inquiriesText: String,
        status: String,
        notes: String
    )
    fun updateCampaign(
        campaignId: String,
        title: String,
        channel: String,
        campaignDate: String,
        spendText: String,
        reachText: String,
        engagementText: String,
        inquiriesText: String,
        status: String,
        notes: String
    )
    fun deleteCampaign(campaignId: String)
    fun addTransaction(
        type: String,
        title: String,
        category: String,
        amountText: String,
        paymentMethod: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String
    )
    fun updateTransaction(
        transactionId: String,
        type: String,
        title: String,
        category: String,
        amountText: String,
        paymentMethod: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String
    )
    fun recordPosSale(
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
    )
    fun recordPosCartSale(
        cartItems: List<PosCartLineItem>,
        defaultCategory: String,
        paymentMethod: String,
        transactionRef: String,
        paymentStatus: String,
        transactionDate: String,
        notes: String,
        customerId: String,
        customerName: String,
        cartDiscountText: String
    )
    fun cancelSaleTransaction(tx: BusinessTransaction)
    fun deleteTransaction(id: String)
    fun addInventoryItem(
        name: String,
        sku: String,
        category: String,
        quantityText: String,
        reorderLevelText: String,
        unitPriceText: String,
        unitCostText: String,
        supplierId: String,
        supplierName: String
    )
    fun updateInventoryItem(
        itemId: String,
        name: String,
        sku: String,
        category: String,
        quantityText: String,
        reorderLevelText: String,
        unitPriceText: String,
        unitCostText: String,
        supplierId: String,
        supplierName: String
    )
    fun adjustInventoryQuantity(item: InventoryItem, delta: Long)
    fun deleteInventoryItem(itemId: String)
    fun recordStockPurchase(
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
    )
    fun markPurchasePaid(purchaseId: String)
    fun deletePurchaseOrder(purchaseId: String)
    fun addCustomer(name: String, phone: String, email: String, location: String, notes: String)
    fun updateCustomer(id: String, name: String, phone: String, email: String, location: String, notes: String)
    fun deleteCustomer(customerId: String)
    fun addSupplier(
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String
    )
    fun updateSupplier(
        id: String,
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String
    )
    fun deleteSupplier(supplierId: String)
    fun addCommunicationLog(
        customerName: String,
        channel: String,
        subject: String,
        receivedDate: String,
        responseDate: String,
        status: String,
        responseMinutesText: String,
        notes: String,
        category: String
    )
    fun updateCommunicationLog(
        commId: String,
        customerName: String,
        channel: String,
        subject: String,
        receivedDate: String,
        responseDate: String,
        status: String,
        responseMinutesText: String,
        notes: String,
        category: String
    )
    fun updateCommunicationStatus(commId: String, newStatus: String)
    fun deleteCommunicationLog(commId: String)
    fun setForecastHorizonMonths(months: Int)
    fun generateWorkspaceBackupJson(onResult: (Result<Pair<String, BackupSummary>>) -> Unit)
    fun reportBackupExportOutcome(success: Boolean, message: String)
    fun inspectBackupFileJson(rawJson: String, sourceLabel: String)
    fun confirmRestoreBackup(replaceExisting: Boolean)
    fun dismissRestoreConfirmation()
    fun saveBusinessProfile(
        ownerName: String,
        businessName: String,
        businessType: String,
        locationNakuru: String,
        businessSize: String,
        phone: String,
        demoModeEnabled: Boolean
    )
    fun askAiAssistant(prompt: String)
    fun analyzeTransactionHistory()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectXWorkspaceShell(
    host: ProjectXWorkspaceHost,
    defaultDisplayName: String,
    defaultEmail: String,
    onSignOutOrSignIn: () -> Unit,
    isPublicPreviewMode: Boolean = false,
    onExitPublicPreview: () -> Unit = {}
) {
    val context = LocalContext.current
    val summary by host.dashboardSummary.collectAsStateWithLifecycle()
    val analyticsReport by host.analyticsReport.collectAsStateWithLifecycle()
    val prompt4Analytics by host.prompt4Analytics.collectAsStateWithLifecycle()
    val forecastHorizonMonths by host.forecastHorizonMonths.collectAsStateWithLifecycle()
    val roomOverviewState by host.roomOverviewState.collectAsStateWithLifecycle()
    val salesOverviewAiState by host.salesOverviewAiState.collectAsStateWithLifecycle()
    val profileState by host.profileState.collectAsStateWithLifecycle()
    val transactionsState by host.transactionsState.collectAsStateWithLifecycle()
    val inventoryState by host.inventoryState.collectAsStateWithLifecycle()
    val campaignsState by host.campaignsState.collectAsStateWithLifecycle()
    val communicationsState by host.communicationsState.collectAsStateWithLifecycle()
    val customersState by host.customersState.collectAsStateWithLifecycle()
    val suppliersState by host.suppliersState.collectAsStateWithLifecycle()
    val purchasesState by host.purchasesState.collectAsStateWithLifecycle()
    val stockMovementsState by host.stockMovementsState.collectAsStateWithLifecycle()
    val aiState by host.aiState.collectAsStateWithLifecycle()
    val growthAnalysisState by host.growthAnalysisState.collectAsStateWithLifecycle()
    val aiMessages by host.aiMessages.collectAsStateWithLifecycle()
    val actionMessage by host.actionMessage.collectAsStateWithLifecycle()
    val isSaving by host.isSaving.collectAsStateWithLifecycle()
    val hasPendingSyncWrites by host.hasPendingSyncWrites.collectAsStateWithLifecycle()
    val backupRestoreState by host.backupRestoreState.collectAsStateWithLifecycle()
    val latestPosReceipt by host.latestPosReceipt.collectAsStateWithLifecycle()
    val isOnline by remember(context) {
        ConnectivityObserver.observeIsOnline(context)
    }.collectAsStateWithLifecycle(initialValue = ConnectivityObserver.isCurrentlyOnline(context))

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            host.clearActionMessage()
        }
    }

    var activeModule by remember { mutableStateOf(AppModule.DASHBOARD) }
    var showAllModulesSheet by remember { mutableStateOf(false) }
    var quickDialogType by remember { mutableStateOf<String?>(null) }
    var isSidebarExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = activeModule != AppModule.DASHBOARD || isPublicPreviewMode) {
        if (activeModule != AppModule.DASHBOARD) {
            activeModule = AppModule.DASHBOARD
        } else if (isPublicPreviewMode) {
            onExitPublicPreview()
        }
    }

    val openModule: (AppModule) -> Unit = { module ->
        activeModule = module
        showAllModulesSheet = false
    }

    val primaryMobileModules = listOf(
        AppModule.DASHBOARD,
        AppModule.RECORD_KEEPING,
        AppModule.PERFORMANCE_ANALYTICS,
        AppModule.AI_ASSISTANT
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val isExpandedWidth = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        primaryMobileModules.forEach { mod ->
                            NavigationBarItem(
                                selected = activeModule == mod,
                                onClick = { openModule(mod) },
                                icon = { Icon(moduleIcon(mod), contentDescription = mod.title) },
                                label = { Text(mod.shortLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_${mod.id}")
                            )
                        }
                        NavigationBarItem(
                            selected = showAllModulesSheet || activeModule !in primaryMobileModules,
                            onClick = { showAllModulesSheet = true },
                            icon = { Icon(Icons.Filled.Apps, contentDescription = "All Research Modules") },
                            label = { Text("Modules", maxLines = 1) },
                            modifier = Modifier.testTag("nav_all_modules")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    if (isSidebarExpanded && isExpandedWidth) {
                        PermanentDrawerSheet(
                            modifier = Modifier
                                .width(236.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PROJECT X",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandPurple
                                )
                                IconButton(
                                    onClick = { isSidebarExpanded = false },
                                    modifier = Modifier.testTag("collapse_sidebar_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = "Collapse sidebar")
                                }
                            }
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                            AppModule.entries.forEach { mod ->
                                NavigationDrawerItem(
                                    selected = activeModule == mod,
                                    onClick = { openModule(mod) },
                                    icon = { Icon(moduleIcon(mod), contentDescription = mod.title) },
                                    label = { Text("${mod.code}. ${mod.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .testTag("drawer_${mod.id}")
                                )
                            }
                        }
                    } else {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            header = {
                                IconButton(
                                    onClick = { isSidebarExpanded = !isSidebarExpanded },
                                    modifier = Modifier.testTag("expand_sidebar_button")
                                ) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Toggle expanded sidebar")
                                }
                            }
                        ) {
                            AppModule.entries.forEach { mod ->
                                NavigationRailItem(
                                    selected = activeModule == mod,
                                    onClick = { openModule(mod) },
                                    icon = { Icon(moduleIcon(mod), contentDescription = mod.title) },
                                    label = { Text("${mod.code}. ${mod.shortLabel}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("rail_${mod.id}")
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (isPublicPreviewMode) {
                        PublicPreviewTopBar(
                            onSignIn = onSignOutOrSignIn,
                            onExit = onExitPublicPreview
                        )
                    } else {
                        if (isSaving) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("workspace_saving_indicator"),
                                color = BrandPurple
                            )
                        }
                        OfflineSyncStatusBanner(
                            isOnline = isOnline,
                            hasPendingWrites = hasPendingSyncWrites
                        )
                    }

                    if (!isPublicPreviewMode && profileState is UiState.Loading && transactionsState is UiState.Loading) {
                        ProjectXLoadingScreen(
                            statusMessage = "Syncing live Cloud Firestore workspace...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (activeModule) {
                                AppModule.DASHBOARD -> BusinessDashboardScreen(
                                    summary = summary,
                                    analyticsReport = analyticsReport,
                                    profileState = profileState,
                                    transactionsState = transactionsState,
                                    onSelectDateFilter = host::setAnalyticsDateFilter,
                                    onApplyCustomDateRange = host::setCustomDateRange,
                                    onToggleDemoMode = host::toggleDemoMode,
                                    onOpenModule = openModule,
                                    onOpenQuickSale = { quickDialogType = BusinessTransaction.TYPE_SALE },
                                    onOpenQuickExpense = { quickDialogType = BusinessTransaction.TYPE_EXPENSE },
                                    onOpenProfileSetup = { openModule(AppModule.REPORTS) },
                                    roomOverviewState = roomOverviewState,
                                    onRefreshRoomCache = host::refreshRoomOverviewCache,
                                    salesAiState = salesOverviewAiState,
                                    onAnalyzeSalesWithAi = host::generateOverviewSalesSummary,
                                    onClearSalesAiSummary = host::clearOverviewSalesSummary,
                                    prompt4Analytics = prompt4Analytics
                                )

                                AppModule.DIGITAL_MARKETING -> DigitalMarketingScreen(
                                    summary = summary,
                                    campaignsState = campaignsState,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onToggleDemoMode = host::toggleDemoMode,
                                    onAddCampaign = host::addCampaign,
                                    onUpdateCampaign = host::updateCampaign,
                                    onDeleteCampaign = host::deleteCampaign
                                )

                                AppModule.DIGITAL_PAYMENTS -> DigitalPaymentsScreen(
                                    summary = summary,
                                    transactionsState = transactionsState,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onToggleDemoMode = host::toggleDemoMode,
                                    onAddTransaction = host::addTransaction,
                                    onUpdateTransaction = host::updateTransaction,
                                    onDeleteTransaction = host::deleteTransaction
                                )

                                AppModule.RECORD_KEEPING -> RecordKeepingScreen(
                                    summary = summary,
                                    transactionsState = transactionsState,
                                    inventoryState = inventoryState,
                                    customersState = customersState,
                                    suppliersState = suppliersState,
                                    stockMovementsState = stockMovementsState,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onToggleDemoMode = host::toggleDemoMode,
                                    onAddTransaction = host::addTransaction,
                                    onRecordPosSale = host::recordPosSale,
                                    onRecordPosCart = host::recordPosCartSale,
                                    onCancelSale = host::cancelSaleTransaction,
                                    onUpdateTransaction = host::updateTransaction,
                                    onDeleteTransaction = host::deleteTransaction,
                                    onAddInventory = host::addInventoryItem,
                                    onUpdateInventory = host::updateInventoryItem,
                                    onAdjustStock = host::adjustInventoryQuantity,
                                    onDeleteInventory = host::deleteInventoryItem,
                                    onRecordStockPurchase = host::recordStockPurchase
                                )

                                AppModule.CUSTOMERS -> CustomersScreen(
                                    summary = summary,
                                    customersState = customersState,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onToggleDemoMode = host::toggleDemoMode,
                                    onAddCustomer = host::addCustomer,
                                    onUpdateCustomer = host::updateCustomer,
                                    onDeleteCustomer = host::deleteCustomer
                                )

                                AppModule.SUPPLIERS -> SuppliersScreen(
                                    summary = summary,
                                    suppliersState = suppliersState,
                                    purchasesState = purchasesState,
                                    inventoryState = inventoryState,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onToggleDemoMode = host::toggleDemoMode,
                                    onAddSupplier = host::addSupplier,
                                    onUpdateSupplier = host::updateSupplier,
                                    onDeleteSupplier = host::deleteSupplier,
                                    onRecordStockPurchase = host::recordStockPurchase,
                                    onMarkPurchasePaid = host::markPurchasePaid,
                                    onDeletePurchaseOrder = host::deletePurchaseOrder
                                )

                                AppModule.ONLINE_COMMUNICATION -> OnlineCommunicationScreen(
                                    summary = summary,
                                    communicationsState = communicationsState,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onToggleDemoMode = host::toggleDemoMode,
                                    onAddCommunicationLog = host::addCommunicationLog,
                                    onUpdateCommunicationLog = host::updateCommunicationLog,
                                    onUpdateCommunicationStatus = host::updateCommunicationStatus,
                                    onDeleteCommunicationLog = host::deleteCommunicationLog
                                )

                                AppModule.PERFORMANCE_ANALYTICS -> PerformanceAnalyticsScreen(
                                    summary = summary,
                                    analyticsReport = analyticsReport,
                                    transactionsState = transactionsState,
                                    inventoryState = inventoryState,
                                    campaignsState = campaignsState,
                                    communicationsState = communicationsState,
                                    onSelectDateFilter = host::setAnalyticsDateFilter,
                                    onApplyCustomDateRange = host::setCustomDateRange,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onToggleDemoMode = host::toggleDemoMode,
                                    prompt4Analytics = prompt4Analytics,
                                    forecastHorizonMonths = forecastHorizonMonths,
                                    onSelectForecastHorizon = host::setForecastHorizonMonths,
                                    onOpenAiAssistant = { openModule(AppModule.AI_ASSISTANT) },
                                    onOpenReports = { openModule(AppModule.REPORTS) }
                                )

                                AppModule.REPORTS -> ReportsScreen(
                                    currentUserDisplayName = defaultDisplayName,
                                    currentUserEmail = defaultEmail,
                                    profileState = profileState,
                                    summary = summary,
                                    transactionsState = transactionsState,
                                    campaignsState = campaignsState,
                                    communicationsState = communicationsState,
                                    inventoryState = inventoryState,
                                    customersState = customersState,
                                    purchasesState = purchasesState,
                                    analyticsReport = analyticsReport,
                                    prompt4Analytics = prompt4Analytics,
                                    isSaving = isSaving,
                                    backupRestoreState = backupRestoreState,
                                    onRequestBackupJson = host::generateWorkspaceBackupJson,
                                    onReportBackupExportOutcome = host::reportBackupExportOutcome,
                                    onInspectBackupJson = host::inspectBackupFileJson,
                                    onConfirmRestoreBackup = host::confirmRestoreBackup,
                                    onDismissRestoreConfirmation = host::dismissRestoreConfirmation,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onToggleDemoMode = host::toggleDemoMode,
                                    onSaveProfile = host::saveBusinessProfile,
                                    onSignOut = onSignOutOrSignIn,
                                    isPublicPreviewMode = isPublicPreviewMode
                                )

                                AppModule.AI_ASSISTANT -> AiAssistantScreen(
                                    summary = summary,
                                    aiState = aiState,
                                    messages = aiMessages,
                                    growthAnalysisState = growthAnalysisState,
                                    onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                                    onOpenAllModules = { showAllModulesSheet = true },
                                    onSendPrompt = host::askAiAssistant,
                                    onAnalyzeTransactions = host::analyzeTransactionHistory
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAllModulesSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAllModulesSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Project X SME Modules (A–I & AI)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Digital Technology Adoption, POS, CRM, Suppliers & AI Forecasting",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AppModule.entries, key = { it.id }) { mod ->
                        ModuleTileCard(
                            module = mod,
                            onClick = { openModule(mod) }
                        )
                    }
                }
            }
        }
    }

    if (quickDialogType == BusinessTransaction.TYPE_SALE) {
        val liveInventory = (inventoryState as? UiState.Success)?.data.orEmpty()
        val liveCustomers = (customersState as? UiState.Success)?.data.orEmpty()
        PosSaleDialog(
            inventory = liveInventory,
            customers = liveCustomers,
            businessLocation = summary.locationNakuru,
            onDismiss = { quickDialogType = null },
            onConfirmPosSale = { title, category, method, ref, status, date, notes, custId, custName, invId, qty, unitPrice, discount, manualAmt ->
                host.recordPosSale(
                    title = title,
                    category = category,
                    paymentMethod = method,
                    transactionRef = ref,
                    paymentStatus = status,
                    transactionDate = date,
                    notes = notes,
                    customerId = custId,
                    customerName = custName,
                    inventoryItemId = invId,
                    quantityText = qty,
                    unitPriceText = unitPrice,
                    discountText = discount,
                    manualAmountText = manualAmt
                )
                quickDialogType = null
            },
            onConfirmPosCart = { cartItems, defaultCategory, method, ref, status, date, notes, custId, custName, cartDiscountText ->
                host.recordPosCartSale(
                    cartItems = cartItems,
                    defaultCategory = defaultCategory,
                    paymentMethod = method,
                    transactionRef = ref,
                    paymentStatus = status,
                    transactionDate = date,
                    notes = notes,
                    customerId = custId,
                    customerName = custName,
                    cartDiscountText = cartDiscountText
                )
                quickDialogType = null
            }
        )
    } else if (quickDialogType != null) {
        RecordTransactionDialog(
            type = quickDialogType!!,
            onDismiss = { quickDialogType = null },
            onConfirm = { selectedType, title, category, amount, method, ref, status, date, notes ->
                host.addTransaction(
                    type = selectedType,
                    title = title,
                    category = category,
                    amountText = amount,
                    paymentMethod = method,
                    transactionRef = ref,
                    paymentStatus = status,
                    transactionDate = date,
                    notes = notes
                )
                quickDialogType = null
            }
        )
    }

    val receiptLines = latestPosReceipt
    if (!receiptLines.isNullOrEmpty()) {
        ReceiptPreviewDialog(
            transaction = receiptLines.first(),
            receiptItems = receiptLines,
            businessName = summary.businessName,
            businessLocation = summary.locationNakuru,
            onDismiss = host::dismissLatestPosReceipt
        )
    }
}

@Composable
private fun PublicPreviewTopBar(
    onSignIn: () -> Unit,
    onExit: () -> Unit
) {
    Surface(
        color = BrandPurpleSoft,
        border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("public_demo_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = "Public demo preview",
                    tint = BrandPurple,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Public Demo Preview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    Text(
                        text = "Sign in to save real SME records in Cloud Firestore",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandNavy.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.testTag("public_demo_exit_button")
                ) {
                    Text("Welcome", color = BrandNavy)
                }
                Button(
                    onClick = onSignIn,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandTeal,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("public_demo_sign_in_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sign In", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
