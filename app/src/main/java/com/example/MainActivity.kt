package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.firebase.configureAppCheck
import com.example.data.local.ProjectXLocalDatabase
import com.example.data.model.AppModule
import com.example.data.model.BusinessTransaction
import com.example.data.model.RegistrationDraft
import com.example.data.network.ConnectivityObserver
import com.example.data.repository.BusinessRepository
import com.example.data.repository.FirebaseAiAssistantRepository
import com.example.data.repository.LocalOverviewRepository
import com.example.data.service.FirebaseVertexTransactionGrowthService
import com.example.ui.components.ModuleTileCard
import com.example.ui.components.OfflineSyncStatusBanner
import com.example.ui.components.ProjectXLoadingScreen
import com.example.ui.components.moduleIcon
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BusinessDashboardScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DigitalMarketingScreen
import com.example.ui.screens.DigitalPaymentsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OnlineCommunicationScreen
import com.example.ui.screens.PerformanceAnalyticsScreen
import com.example.ui.screens.PosSaleDialog
import com.example.ui.screens.ReceiptPreviewDialog
import com.example.ui.screens.RecordKeepingScreen
import com.example.ui.screens.RecordTransactionDialog
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SuppliersScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.ProjectXTheme
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_WELCOME = "welcome"
const val ROUTE_ONBOARDING = "onboarding"
const val ROUTE_REGISTER = "register"
const val ROUTE_LOGIN = "login"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ProjectXTheme {
                ProjectXRootApp()
            }
        }
    }
}

@Composable
fun ProjectXRootApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var pendingDraft by remember { mutableStateOf<RegistrationDraft?>(null) }
    var autoSignInAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!autoSignInAttempted && auth.currentUser == null) {
            autoSignInAttempted = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = this
            )
        }
    }

    if (currentUser == null) {
        PreAuthNavigationFlow(
            initialDraft = pendingDraft ?: RegistrationDraft(),
            onUpdateDraft = { pendingDraft = it },
            onAuthSuccess = {}
        )
    } else {
        val user = currentUser!!
        key(user.uid) {
            AuthenticatedWorkspace(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName.orEmpty(),
                defaultEmail = user.email.orEmpty(),
                initialDraft = pendingDraft,
                onDraftConsumed = { pendingDraft = null }
            )
        }
    }
}

@Composable
private fun PreAuthNavigationFlow(
    initialDraft: RegistrationDraft,
    onUpdateDraft: (RegistrationDraft) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_WELCOME) {
        composable(ROUTE_WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(ROUTE_ONBOARDING) },
                onCreateAccount = { navController.navigate(ROUTE_REGISTER) },
                onSignIn = { navController.navigate(ROUTE_LOGIN) }
            )
        }
        composable(ROUTE_ONBOARDING) {
            OnboardingScreen(
                initialDraft = initialDraft,
                onDraftUpdated = onUpdateDraft,
                onBack = { navController.popBackStack() },
                onFinishToRegister = { navController.navigate(ROUTE_REGISTER) },
                onGoToLogin = { navController.navigate(ROUTE_LOGIN) }
            )
        }
        composable(ROUTE_REGISTER) {
            RegistrationScreen(
                initialDraft = initialDraft,
                onDraftUpdated = onUpdateDraft,
                onBack = { navController.popBackStack() },
                onGoToLogin = { navController.navigate(ROUTE_LOGIN) },
                onAuthSuccess = onAuthSuccess
            )
        }
        composable(ROUTE_LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onGoToRegister = { navController.navigate(ROUTE_REGISTER) },
                onAuthSuccess = onAuthSuccess
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedWorkspace(
    currentUserId: String,
    defaultDisplayName: String,
    defaultEmail: String,
    initialDraft: RegistrationDraft?,
    onDraftConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DashboardViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val businessRepository = BusinessRepository(db)
                val localOverviewRepository = LocalOverviewRepository(
                    ProjectXLocalDatabase.getInstance(app).businessOverviewDao()
                )
                DashboardViewModel(
                    repository = businessRepository,
                    aiRepository = FirebaseAiAssistantRepository(),
                    growthAnalysisService = FirebaseVertexTransactionGrowthService(businessRepository),
                    currentUserId = currentUserId,
                    initialDraft = initialDraft,
                    defaultDisplayName = defaultDisplayName,
                    onDraftConsumed = onDraftConsumed,
                    localOverviewRepository = localOverviewRepository
                )
            }
        }
    )

    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val analyticsReport by viewModel.analyticsReport.collectAsStateWithLifecycle()
    val prompt4Analytics by viewModel.prompt4Analytics.collectAsStateWithLifecycle()
    val forecastHorizonMonths by viewModel.forecastHorizonMonths.collectAsStateWithLifecycle()
    val roomOverviewState by viewModel.roomOverviewState.collectAsStateWithLifecycle()
    val salesOverviewAiState by viewModel.salesOverviewAiState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val inventoryState by viewModel.inventoryState.collectAsStateWithLifecycle()
    val campaignsState by viewModel.campaignsState.collectAsStateWithLifecycle()
    val communicationsState by viewModel.communicationsState.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val suppliersState by viewModel.suppliersState.collectAsStateWithLifecycle()
    val purchasesState by viewModel.purchasesState.collectAsStateWithLifecycle()
    val stockMovementsState by viewModel.stockMovementsState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val growthAnalysisState by viewModel.growthAnalysisState.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val hasPendingSyncWrites by viewModel.hasPendingSyncWrites.collectAsStateWithLifecycle()
    val backupRestoreState by viewModel.backupRestoreState.collectAsStateWithLifecycle()
    val latestPosReceipt by viewModel.latestPosReceipt.collectAsStateWithLifecycle()
    val isOnline by remember(context) {
        ConnectivityObserver.observeIsOnline(context)
    }.collectAsStateWithLifecycle(initialValue = ConnectivityObserver.isCurrentlyOnline(context))

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    var activeModule by remember { mutableStateOf(AppModule.DASHBOARD) }
    var showAllModulesSheet by remember { mutableStateOf(false) }
    var quickDialogType by remember { mutableStateOf<String?>(null) }
    var isSidebarExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = activeModule != AppModule.DASHBOARD) {
        activeModule = AppModule.DASHBOARD
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
                                label = {
                                    Text(
                                        text = mod.shortLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
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
                                    label = {
                                        Text(
                                            text = "${mod.code}. ${mod.title}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
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
                                    label = {
                                        Text(
                                            text = "${mod.code}. ${mod.shortLabel}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
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
                    if (profileState is UiState.Loading && transactionsState is UiState.Loading) {
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
                        onSelectDateFilter = viewModel::setAnalyticsDateFilter,
                        onApplyCustomDateRange = viewModel::setCustomDateRange,
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        onOpenModule = openModule,
                        onOpenQuickSale = { quickDialogType = BusinessTransaction.TYPE_SALE },
                        onOpenQuickExpense = { quickDialogType = BusinessTransaction.TYPE_EXPENSE },
                        onOpenProfileSetup = { openModule(AppModule.REPORTS) },
                        roomOverviewState = roomOverviewState,
                        onRefreshRoomCache = viewModel::refreshRoomOverviewCache,
                        salesAiState = salesOverviewAiState,
                        onAnalyzeSalesWithAi = viewModel::generateOverviewSalesSummary,
                        onClearSalesAiSummary = viewModel::clearOverviewSalesSummary,
                        prompt4Analytics = prompt4Analytics
                    )

                    AppModule.DIGITAL_MARKETING -> DigitalMarketingScreen(
                        summary = summary,
                        campaignsState = campaignsState,
                        onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                        onOpenAllModules = { showAllModulesSheet = true },
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        onAddCampaign = viewModel::addCampaign,
                        onUpdateCampaign = viewModel::updateCampaign,
                        onDeleteCampaign = viewModel::deleteCampaign
                    )

                    AppModule.DIGITAL_PAYMENTS -> DigitalPaymentsScreen(
                        summary = summary,
                        transactionsState = transactionsState,
                        onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                        onOpenAllModules = { showAllModulesSheet = true },
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        onAddTransaction = viewModel::addTransaction,
                        onUpdateTransaction = viewModel::updateTransaction,
                        onDeleteTransaction = viewModel::deleteTransaction
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
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        onAddTransaction = viewModel::addTransaction,
                        onRecordPosSale = viewModel::recordPosSale,
                        onRecordPosCart = viewModel::recordPosCartSale,
                        onCancelSale = viewModel::cancelSaleTransaction,
                        onUpdateTransaction = viewModel::updateTransaction,
                        onDeleteTransaction = viewModel::deleteTransaction,
                        onAddInventory = viewModel::addInventoryItem,
                        onUpdateInventory = viewModel::updateInventoryItem,
                        onAdjustStock = viewModel::adjustInventoryQuantity,
                        onDeleteInventory = viewModel::deleteInventoryItem,
                        onRecordStockPurchase = viewModel::recordStockPurchase
                    )

                    AppModule.CUSTOMERS -> CustomersScreen(
                        summary = summary,
                        customersState = customersState,
                        onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                        onOpenAllModules = { showAllModulesSheet = true },
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        onAddCustomer = viewModel::addCustomer,
                        onUpdateCustomer = viewModel::updateCustomer,
                        onDeleteCustomer = viewModel::deleteCustomer
                    )

                    AppModule.SUPPLIERS -> SuppliersScreen(
                        summary = summary,
                        suppliersState = suppliersState,
                        purchasesState = purchasesState,
                        inventoryState = inventoryState,
                        onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                        onOpenAllModules = { showAllModulesSheet = true },
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        onAddSupplier = viewModel::addSupplier,
                        onUpdateSupplier = viewModel::updateSupplier,
                        onDeleteSupplier = viewModel::deleteSupplier,
                        onRecordStockPurchase = viewModel::recordStockPurchase,
                        onMarkPurchasePaid = viewModel::markPurchasePaid,
                        onDeletePurchaseOrder = viewModel::deletePurchaseOrder
                    )

                    AppModule.ONLINE_COMMUNICATION -> OnlineCommunicationScreen(
                        summary = summary,
                        communicationsState = communicationsState,
                        onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                        onOpenAllModules = { showAllModulesSheet = true },
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        onAddCommunicationLog = viewModel::addCommunicationLog,
                        onUpdateCommunicationLog = viewModel::updateCommunicationLog,
                        onUpdateCommunicationStatus = viewModel::updateCommunicationStatus,
                        onDeleteCommunicationLog = viewModel::deleteCommunicationLog
                    )

                    AppModule.PERFORMANCE_ANALYTICS -> PerformanceAnalyticsScreen(
                        summary = summary,
                        analyticsReport = analyticsReport,
                        transactionsState = transactionsState,
                        inventoryState = inventoryState,
                        campaignsState = campaignsState,
                        communicationsState = communicationsState,
                        onSelectDateFilter = viewModel::setAnalyticsDateFilter,
                        onApplyCustomDateRange = viewModel::setCustomDateRange,
                        onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                        onOpenAllModules = { showAllModulesSheet = true },
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        prompt4Analytics = prompt4Analytics,
                        forecastHorizonMonths = forecastHorizonMonths,
                        onSelectForecastHorizon = viewModel::setForecastHorizonMonths,
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
                        onRequestBackupJson = viewModel::generateWorkspaceBackupJson,
                        onReportBackupExportOutcome = viewModel::reportBackupExportOutcome,
                        onInspectBackupJson = viewModel::inspectBackupFileJson,
                        onConfirmRestoreBackup = viewModel::confirmRestoreBackup,
                        onDismissRestoreConfirmation = viewModel::dismissRestoreConfirmation,
                        onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                        onOpenAllModules = { showAllModulesSheet = true },
                        onToggleDemoMode = viewModel::toggleDemoMode,
                        onSaveProfile = viewModel::saveBusinessProfile,
                        onSignOut = {
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    )

                    AppModule.AI_ASSISTANT -> AiAssistantScreen(
                        summary = summary,
                        aiState = aiState,
                        messages = aiMessages,
                        growthAnalysisState = growthAnalysisState,
                        onBackToDashboard = { openModule(AppModule.DASHBOARD) },
                        onOpenAllModules = { showAllModulesSheet = true },
                        onSendPrompt = viewModel::askAiAssistant,
                        onAnalyzeTransactions = viewModel::analyzeTransactionHistory
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
                viewModel.recordPosSale(
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
                viewModel.recordPosCartSale(
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
                viewModel.addTransaction(
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
            onDismiss = viewModel::dismissLatestPosReceipt
        )
    }
}
