package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.analytics.AnalyticsFilterState
import com.example.data.analytics.Prompt4AnalyticsBundle
import com.example.data.analytics.Prompt4ForecastingEngine
import com.example.data.analytics.SmeAnalyticsEngine
import com.example.data.analytics.SmeFullAnalyticsReport
import com.example.data.backup.BackupSummary
import com.example.data.backup.ProjectXBackupManager
import com.example.data.backup.ValidatedWorkspaceBackup
import com.example.data.backup.WorkspaceBackupPayload
import com.example.data.model.AnalyticsDateFilter
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.DemoSampleData
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.PosCartLineItem
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.RegistrationDraft
import com.example.data.model.StockMovementRecord
import com.example.data.model.SupplierRecord
import com.example.data.model.formatKes
import com.example.data.model.isValidIsoDate
import com.example.data.model.todayIsoDate
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.BusinessRepository
import com.example.data.repository.LocalOverviewRepository
import com.example.data.repository.RoomBusinessOverviewState
import com.example.data.service.GrowthAnalysisReport
import com.example.data.service.GrowthAnalysisResult
import com.example.data.service.TransactionGrowthAnalyzer
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiResponseState {
    data object Idle : AiResponseState
    data object Loading : AiResponseState
    data class Streaming(val partialText: String) : AiResponseState
    data class Success(val text: String) : AiResponseState
    data class Error(val message: String) : AiResponseState
}

sealed interface GrowthAnalysisUiState {
    data object Idle : GrowthAnalysisUiState
    data object Loading : GrowthAnalysisUiState
    data class Success(val report: GrowthAnalysisReport) : GrowthAnalysisUiState
    data class EmptyLedger(val message: String) : GrowthAnalysisUiState
    data class Error(val message: String) : GrowthAnalysisUiState
}

sealed interface SalesOverviewAiState {
    data object Idle : SalesOverviewAiState
    data object Loading : SalesOverviewAiState
    data class Streaming(val partialText: String) : SalesOverviewAiState
    data class Success(
        val summaryText: String,
        val isDemoData: Boolean,
        val analyzedSalesCount: Int,
        val totalSalesKes: Double
    ) : SalesOverviewAiState
    data class EmptySales(val message: String) : SalesOverviewAiState
    data class Error(val message: String) : SalesOverviewAiState
}

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String
)

sealed interface BackupRestoreUiState {
    data object Idle : BackupRestoreUiState
    data object Exporting : BackupRestoreUiState
    data class ReadyToConfirmRestore(
        val backup: ValidatedWorkspaceBackup,
        val sourceLabel: String
    ) : BackupRestoreUiState
    data object Restoring : BackupRestoreUiState
    data class Success(val message: String) : BackupRestoreUiState
    data class Error(val message: String) : BackupRestoreUiState
}

data class DashboardSummary(
    val isDemoMode: Boolean,
    val hasAnyLiveRecords: Boolean,
    val businessName: String,
    val businessType: String,
    val locationNakuru: String,
    val businessSize: String,
    // DV: SME Performance Metrics
    val totalSalesKes: Double,
    val totalExpensesKes: Double,
    val otherTransactionsKes: Double,
    val netBalanceKes: Double,
    val lowStockCount: Int,
    val salesTransactionCount: Int,
    val otherTransactionCount: Int,
    val transactionCount: Int,
    val inventoryCount: Int,
    // IV2: Digital Payments Metrics
    val digitalPaymentCount: Int,
    val digitalPaymentAdoptionPct: Int,
    val totalDigitalPaymentsKes: Double,
    val mpesaSalesKes: Double,
    val bankSalesKes: Double,
    val cardSalesKes: Double,
    val otherDigitalSalesKes: Double,
    val cashSalesKes: Double,
    // IV1: Digital Marketing Metrics
    val campaignCount: Int,
    val totalMarketingSpendKes: Double,
    val totalCampaignReach: Long,
    val totalCampaignEngagement: Long,
    val totalMarketingInquiries: Long,
    // IV4: Online Communication Metrics
    val communicationCount: Int,
    val respondedOrConvertedComms: Int,
    val convertedCustomerLeads: Int,
    val avgResponseMinutes: Int,
    // Prompt 4 Extended Metrics (Customers, Suppliers, Inventory Valuation & Payables)
    val customerCount: Int = 0,
    val repeatCustomerCount: Int = 0,
    val supplierCount: Int = 0,
    val pendingTransactionsCount: Int = 0,
    val pendingTransactionsKes: Double = 0.0,
    val pendingPayablesCount: Int = 0,
    val pendingPayablesKes: Double = 0.0,
    val totalInventoryRetailValueKes: Double = 0.0,
    val totalInventoryCostValueKes: Double = 0.0,
    val grossProfitKes: Double? = null,
    val hasCostDataForGrossProfit: Boolean = false,
    val latestMoMSalesGrowthPct: Double? = null
)

class DashboardViewModel(
    private val repository: BusinessRepository,
    private val aiRepository: AiAssistantRepository,
    private val growthAnalysisService: TransactionGrowthAnalyzer,
    private val currentUserId: String,
    initialDraft: RegistrationDraft? = null,
    defaultDisplayName: String = "",
    onDraftConsumed: () -> Unit = {},
    private val localOverviewRepository: LocalOverviewRepository? = null
) : ViewModel() {

    val profileState: StateFlow<UiState<BusinessProfile?>> =
        repository.observeBusinessProfile(currentUserId)
            .map<BusinessProfile?, UiState<BusinessProfile?>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing business profile", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load business profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val transactionsState: StateFlow<UiState<List<BusinessTransaction>>> =
        repository.observeTransactions(currentUserId)
            .map<List<BusinessTransaction>, UiState<List<BusinessTransaction>>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing transactions", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load transactions"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val inventoryState: StateFlow<UiState<List<InventoryItem>>> =
        repository.observeInventory(currentUserId)
            .map<List<InventoryItem>, UiState<List<InventoryItem>>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing inventory", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load inventory"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val campaignsState: StateFlow<UiState<List<DigitalMarketingCampaign>>> =
        repository.observeCampaigns(currentUserId)
            .map<List<DigitalMarketingCampaign>, UiState<List<DigitalMarketingCampaign>>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing marketing campaigns", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load campaigns"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val communicationsState: StateFlow<UiState<List<OnlineCommunicationLog>>> =
        repository.observeCommunications(currentUserId)
            .map<List<OnlineCommunicationLog>, UiState<List<OnlineCommunicationLog>>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing online communications", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load communications"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val customersState: StateFlow<UiState<List<CustomerRecord>>> =
        repository.observeCustomers(currentUserId)
            .map<List<CustomerRecord>, UiState<List<CustomerRecord>>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing customers", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load customers"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val suppliersState: StateFlow<UiState<List<SupplierRecord>>> =
        repository.observeSuppliers(currentUserId)
            .map<List<SupplierRecord>, UiState<List<SupplierRecord>>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing suppliers", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load suppliers"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val purchasesState: StateFlow<UiState<List<PurchaseOrderRecord>>> =
        repository.observePurchases(currentUserId)
            .map<List<PurchaseOrderRecord>, UiState<List<PurchaseOrderRecord>>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing purchases", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load purchases"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val stockMovementsState: StateFlow<UiState<List<StockMovementRecord>>> =
        repository.observeStockMovements(currentUserId)
            .map<List<StockMovementRecord>, UiState<List<StockMovementRecord>>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing stock movements", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load stock movements"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _localDemoPreview = MutableStateFlow(false)
    private val _forecastHorizonMonths = MutableStateFlow(6)
    val forecastHorizonMonths: StateFlow<Int> = _forecastHorizonMonths.asStateFlow()

    val prompt4Analytics: StateFlow<Prompt4AnalyticsBundle> = combine(
        profileState,
        transactionsState,
        inventoryState,
        customersState,
        suppliersState,
        purchasesState,
        campaignsState,
        communicationsState,
        _localDemoPreview,
        _forecastHorizonMonths
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profile = (values[0] as UiState<BusinessProfile?> as? UiState.Success)?.data
        @Suppress("UNCHECKED_CAST")
        val liveTx = (values[1] as UiState<List<BusinessTransaction>> as? UiState.Success)?.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val liveInv = (values[2] as UiState<List<InventoryItem>> as? UiState.Success)?.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val liveCust = (values[3] as UiState<List<CustomerRecord>> as? UiState.Success)?.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val liveSup = (values[4] as UiState<List<SupplierRecord>> as? UiState.Success)?.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val livePo = (values[5] as UiState<List<PurchaseOrderRecord>> as? UiState.Success)?.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val liveCamp = (values[6] as UiState<List<DigitalMarketingCampaign>> as? UiState.Success)?.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val liveComm = (values[7] as UiState<List<OnlineCommunicationLog>> as? UiState.Success)?.data.orEmpty()
        val localDemo = values[8] as Boolean
        val horizon = values[9] as Int

        val hasLiveRecords = liveTx.isNotEmpty() || liveInv.isNotEmpty() ||
            liveCust.isNotEmpty() || liveSup.isNotEmpty() || livePo.isNotEmpty() ||
            liveCamp.isNotEmpty() || liveComm.isNotEmpty()
        val isDemo = (profile?.demoModeEnabled ?: localDemo) && !hasLiveRecords

        Prompt4ForecastingEngine.compute(
            isDemoMode = isDemo,
            allTransactions = if (isDemo) DemoSampleData.sampleTransactions else liveTx,
            inventory = if (isDemo) DemoSampleData.sampleInventory else liveInv,
            customers = if (isDemo) DemoSampleData.sampleCustomers else liveCust,
            horizonMonths = horizon
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = Prompt4ForecastingEngine.compute(
            isDemoMode = false,
            allTransactions = emptyList(),
            inventory = emptyList(),
            customers = emptyList(),
            horizonMonths = 6
        )
    )

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        profileState,
        transactionsState,
        inventoryState,
        campaignsState,
        communicationsState,
        customersState,
        suppliersState,
        purchasesState,
        prompt4Analytics,
        _localDemoPreview
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profileRes = values[0] as UiState<BusinessProfile?>
        @Suppress("UNCHECKED_CAST")
        val txRes = values[1] as UiState<List<BusinessTransaction>>
        @Suppress("UNCHECKED_CAST")
        val invRes = values[2] as UiState<List<InventoryItem>>
        @Suppress("UNCHECKED_CAST")
        val campRes = values[3] as UiState<List<DigitalMarketingCampaign>>
        @Suppress("UNCHECKED_CAST")
        val commRes = values[4] as UiState<List<OnlineCommunicationLog>>
        @Suppress("UNCHECKED_CAST")
        val custRes = values[5] as UiState<List<CustomerRecord>>
        @Suppress("UNCHECKED_CAST")
        val supRes = values[6] as UiState<List<SupplierRecord>>
        @Suppress("UNCHECKED_CAST")
        val poRes = values[7] as UiState<List<PurchaseOrderRecord>>
        val p4 = values[8] as Prompt4AnalyticsBundle
        val localDemo = values[9] as Boolean

        val profile = (profileRes as? UiState.Success)?.data
        val liveTx = (txRes as? UiState.Success)?.data.orEmpty()
        val liveInv = (invRes as? UiState.Success)?.data.orEmpty()
        val liveCamps = (campRes as? UiState.Success)?.data.orEmpty()
        val liveComms = (commRes as? UiState.Success)?.data.orEmpty()
        val liveCusts = (custRes as? UiState.Success)?.data.orEmpty()
        val liveSups = (supRes as? UiState.Success)?.data.orEmpty()
        val livePos = (poRes as? UiState.Success)?.data.orEmpty()

        val hasLiveRecords = liveTx.isNotEmpty() || liveInv.isNotEmpty() ||
            liveCamps.isNotEmpty() || liveComms.isNotEmpty() ||
            liveCusts.isNotEmpty() || liveSups.isNotEmpty() || livePos.isNotEmpty()
        val demoSetting = profile?.demoModeEnabled ?: localDemo
        val isDemoMode = demoSetting && !hasLiveRecords

        val activeTx = (if (isDemoMode) DemoSampleData.sampleTransactions else liveTx).filter { it.isActiveLedgerEntry() }
        val activeInv = if (isDemoMode) DemoSampleData.sampleInventory else liveInv
        val activeCamps = if (isDemoMode) DemoSampleData.sampleCampaigns else liveCamps
        val activeComms = if (isDemoMode) DemoSampleData.sampleCommunications else liveComms
        val activeCusts = if (isDemoMode) DemoSampleData.sampleCustomers else liveCusts
        val activeSups = if (isDemoMode) DemoSampleData.sampleSuppliers else liveSups
        val activePos = if (isDemoMode) DemoSampleData.samplePurchases else livePos

        val sales = activeTx.filter { it.type == BusinessTransaction.TYPE_SALE }
        val expenses = activeTx.filter { it.type == BusinessTransaction.TYPE_EXPENSE }
        val others = activeTx.filter { it.type == BusinessTransaction.TYPE_OTHER }
        val totalSales = sales.sumOf { it.amountKes }
        val totalExpenses = expenses.sumOf { it.amountKes }
        val otherTotal = others.sumOf { it.amountKes }

        val digitalTx = activeTx.filter { it.isDigitalPayment }
        val digitalCount = digitalTx.size
        val digitalAdoptionPct = if (activeTx.isNotEmpty()) ((digitalCount * 100.0) / activeTx.size).toInt() else 0
        val totalDigitalPaymentsKes = digitalTx.sumOf { it.amountKes }

        val mpesaSales = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_MPESA }.sumOf { it.amountKes }
        val bankSales = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_BANK }.sumOf { it.amountKes }
        val cardSales = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_CARD }.sumOf { it.amountKes }
        val otherDigitalSales = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_OTHER_DIGITAL }.sumOf { it.amountKes }
        val cashSales = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_CASH }.sumOf { it.amountKes }

        val convertedComms = activeComms.count { it.status == OnlineCommunicationLog.STATUS_CONVERTED }
        val respondedOrConverted = activeComms.count {
            it.status == OnlineCommunicationLog.STATUS_RESPONDED || it.status == OnlineCommunicationLog.STATUS_CONVERTED
        }
        val avgResp = if (activeComms.isNotEmpty()) activeComms.map { it.responseMinutes }.average().toInt() else 0

        val pendingTxList = activeTx.filter { it.paymentStatus == BusinessTransaction.STATUS_PENDING }
        val pendingPoList = activePos.filter { it.paymentStatus != PurchaseOrderRecord.STATUS_PAID }

        DashboardSummary(
            isDemoMode = isDemoMode,
            hasAnyLiveRecords = hasLiveRecords,
            businessName = profile?.businessName?.ifBlank { "My Nakuru SME" } ?: "My Nakuru SME",
            businessType = profile?.businessType?.ifBlank { "Retail Shop" } ?: "Retail Shop",
            locationNakuru = profile?.locationNakuru?.ifBlank { "Nakuru Town, Kenya" } ?: "Nakuru Town, Kenya",
            businessSize = profile?.businessSize?.ifBlank { "Small (6–20 staff)" } ?: "Small (6–20 staff)",
            totalSalesKes = totalSales,
            totalExpensesKes = totalExpenses,
            otherTransactionsKes = otherTotal,
            netBalanceKes = totalSales - totalExpenses,
            lowStockCount = activeInv.count { it.isLowStock },
            salesTransactionCount = sales.size,
            otherTransactionCount = others.size,
            transactionCount = activeTx.size,
            inventoryCount = activeInv.size,
            digitalPaymentCount = digitalCount,
            digitalPaymentAdoptionPct = digitalAdoptionPct,
            totalDigitalPaymentsKes = totalDigitalPaymentsKes,
            mpesaSalesKes = mpesaSales,
            bankSalesKes = bankSales,
            cardSalesKes = cardSales,
            otherDigitalSalesKes = otherDigitalSales,
            cashSalesKes = cashSales,
            campaignCount = activeCamps.size,
            totalMarketingSpendKes = activeCamps.sumOf { it.spendKes },
            totalCampaignReach = activeCamps.sumOf { it.reachCount },
            totalCampaignEngagement = activeCamps.sumOf { it.engagementCount },
            totalMarketingInquiries = activeCamps.sumOf { it.inquiriesGenerated },
            communicationCount = activeComms.size,
            respondedOrConvertedComms = respondedOrConverted,
            convertedCustomerLeads = convertedComms,
            avgResponseMinutes = avgResp,
            customerCount = activeCusts.size,
            repeatCustomerCount = activeCusts.count { it.isRepeatCustomer },
            supplierCount = activeSups.size,
            pendingTransactionsCount = pendingTxList.size,
            pendingTransactionsKes = pendingTxList.sumOf { it.amountKes },
            pendingPayablesCount = pendingPoList.size,
            pendingPayablesKes = pendingPoList.sumOf { it.totalCostKes },
            totalInventoryRetailValueKes = p4.totalInventoryRetailValueKes,
            totalInventoryCostValueKes = p4.totalInventoryCostValueKes,
            grossProfitKes = p4.totalGrossProfitKes,
            hasCostDataForGrossProfit = p4.hasCostDataForGrossProfit,
            latestMoMSalesGrowthPct = p4.latestMoMSalesGrowthPct
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardSummary(
            isDemoMode = false,
            hasAnyLiveRecords = false,
            businessName = "My Nakuru SME",
            businessType = "Retail Shop",
            locationNakuru = "Nakuru Town, Kenya",
            businessSize = "Small (6–20 staff)",
            totalSalesKes = 0.0,
            totalExpensesKes = 0.0,
            otherTransactionsKes = 0.0,
            netBalanceKes = 0.0,
            lowStockCount = 0,
            salesTransactionCount = 0,
            otherTransactionCount = 0,
            transactionCount = 0,
            inventoryCount = 0,
            digitalPaymentCount = 0,
            digitalPaymentAdoptionPct = 0,
            totalDigitalPaymentsKes = 0.0,
            mpesaSalesKes = 0.0,
            bankSalesKes = 0.0,
            cardSalesKes = 0.0,
            otherDigitalSalesKes = 0.0,
            cashSalesKes = 0.0,
            campaignCount = 0,
            totalMarketingSpendKes = 0.0,
            totalCampaignReach = 0L,
            totalCampaignEngagement = 0L,
            totalMarketingInquiries = 0L,
            communicationCount = 0,
            respondedOrConvertedComms = 0,
            convertedCustomerLeads = 0,
            avgResponseMinutes = 0
        )
    )

    private val _analyticsFilterState = MutableStateFlow(AnalyticsFilterState())
    val analyticsFilterState: StateFlow<AnalyticsFilterState> = _analyticsFilterState.asStateFlow()

    val analyticsReport: StateFlow<SmeFullAnalyticsReport> = combine(
        profileState,
        transactionsState,
        inventoryState,
        campaignsState,
        communicationsState,
        _localDemoPreview,
        _analyticsFilterState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profileRes = values[0] as UiState<BusinessProfile?>
        @Suppress("UNCHECKED_CAST")
        val txRes = values[1] as UiState<List<BusinessTransaction>>
        @Suppress("UNCHECKED_CAST")
        val invRes = values[2] as UiState<List<InventoryItem>>
        @Suppress("UNCHECKED_CAST")
        val campRes = values[3] as UiState<List<DigitalMarketingCampaign>>
        @Suppress("UNCHECKED_CAST")
        val commRes = values[4] as UiState<List<OnlineCommunicationLog>>
        val localDemo = values[5] as Boolean
        val filterState = values[6] as AnalyticsFilterState

        val profile = (profileRes as? UiState.Success)?.data
        val liveTx = (txRes as? UiState.Success)?.data.orEmpty()
        val liveInv = (invRes as? UiState.Success)?.data.orEmpty()
        val liveCamps = (campRes as? UiState.Success)?.data.orEmpty()
        val liveComms = (commRes as? UiState.Success)?.data.orEmpty()

        val hasLiveRecords = liveTx.isNotEmpty() || liveInv.isNotEmpty() || liveCamps.isNotEmpty() || liveComms.isNotEmpty()
        val demoSetting = profile?.demoModeEnabled ?: localDemo
        val isDemoMode = demoSetting && !hasLiveRecords

        SmeAnalyticsEngine.compute(
            isDemoMode = isDemoMode,
            filterState = filterState,
            allTransactions = if (isDemoMode) DemoSampleData.sampleTransactions else liveTx,
            inventory = if (isDemoMode) DemoSampleData.sampleInventory else liveInv,
            allCampaigns = if (isDemoMode) DemoSampleData.sampleCampaigns else liveCamps,
            allCommunications = if (isDemoMode) DemoSampleData.sampleCommunications else liveComms
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SmeAnalyticsEngine.compute(
            isDemoMode = false,
            filterState = AnalyticsFilterState(),
            allTransactions = emptyList(),
            inventory = emptyList(),
            allCampaigns = emptyList(),
            allCommunications = emptyList()
        )
    )

    private val _aiState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val aiState: StateFlow<AiResponseState> = _aiState.asStateFlow()

    private val _growthAnalysisState = MutableStateFlow<GrowthAnalysisUiState>(GrowthAnalysisUiState.Idle)
    val growthAnalysisState: StateFlow<GrowthAnalysisUiState> = _growthAnalysisState.asStateFlow()

    private val _salesOverviewAiState = MutableStateFlow<SalesOverviewAiState>(SalesOverviewAiState.Idle)
    val salesOverviewAiState: StateFlow<SalesOverviewAiState> = _salesOverviewAiState.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _backupRestoreState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val backupRestoreState: StateFlow<BackupRestoreUiState> = _backupRestoreState.asStateFlow()

    private val _latestPosReceipt = MutableStateFlow<List<BusinessTransaction>?>(null)
    val latestPosReceipt: StateFlow<List<BusinessTransaction>?> = _latestPosReceipt.asStateFlow()

    val hasPendingSyncWrites: StateFlow<Boolean> =
        repository.observeHasPendingWrites(currentUserId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = false
            )

    val roomOverviewState: StateFlow<RoomBusinessOverviewState> =
        (localOverviewRepository?.observeOverviewKpis(currentUserId)
            ?: flowOf(RoomBusinessOverviewState()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = RoomBusinessOverviewState()
            )

    init {
        if (initialDraft != null && initialDraft.businessName.isNotBlank()) {
            viewModelScope.launch {
                val res = repository.saveBusinessProfile(
                    ownerName = initialDraft.ownerName.ifBlank { defaultDisplayName.ifBlank { "Business Owner" } },
                    businessName = initialDraft.businessName,
                    businessType = initialDraft.businessType,
                    locationNakuru = initialDraft.locationNakuru,
                    businessSize = initialDraft.businessSize,
                    phone = initialDraft.phone,
                    demoModeEnabled = false,
                    preserveExistingDemoMode = true
                )
                if (res.isSuccess) {
                    onDraftConsumed()
                    _actionMessage.value = "Business profile saved to Cloud Firestore."
                } else {
                    _actionMessage.value = res.exceptionOrNull()?.localizedMessage
                        ?: "Could not sync onboarding business profile."
                }
            }
        }

        if (localOverviewRepository != null) {
            viewModelScope.launch {
                combine(
                    profileState,
                    transactionsState,
                    inventoryState,
                    campaignsState,
                    communicationsState,
                    _localDemoPreview
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val profile = (values[0] as UiState<BusinessProfile?> as? UiState.Success)?.data
                    @Suppress("UNCHECKED_CAST")
                    val liveTx = (values[1] as UiState<List<BusinessTransaction>> as? UiState.Success)?.data.orEmpty()
                    @Suppress("UNCHECKED_CAST")
                    val liveInv = (values[2] as UiState<List<InventoryItem>> as? UiState.Success)?.data.orEmpty()
                    @Suppress("UNCHECKED_CAST")
                    val liveCamps = (values[3] as UiState<List<DigitalMarketingCampaign>> as? UiState.Success)?.data.orEmpty()
                    @Suppress("UNCHECKED_CAST")
                    val liveComms = (values[4] as UiState<List<OnlineCommunicationLog>> as? UiState.Success)?.data.orEmpty()
                    val localDemo = values[5] as Boolean

                    val hasLiveRecords = liveTx.isNotEmpty() || liveInv.isNotEmpty() || liveCamps.isNotEmpty() || liveComms.isNotEmpty()
                    val demoSetting = profile?.demoModeEnabled ?: localDemo
                    val isDemoMode = demoSetting && !hasLiveRecords
                    val activeTx = if (isDemoMode) DemoSampleData.sampleTransactions else liveTx

                    RoomSyncPayload(
                        businessName = profile?.businessName?.ifBlank { "My Nakuru SME" } ?: "My Nakuru SME",
                        businessType = profile?.businessType?.ifBlank { "Retail Shop" } ?: "Retail Shop",
                        locationNakuru = profile?.locationNakuru?.ifBlank { "Nakuru Town, Kenya" } ?: "Nakuru Town, Kenya",
                        isDemoData = isDemoMode,
                        transactions = activeTx
                    )
                }.collectLatest { payload ->
                    runCatching {
                        localOverviewRepository.syncOverviewCache(
                            userId = currentUserId,
                            businessName = payload.businessName,
                            businessType = payload.businessType,
                            locationNakuru = payload.locationNakuru,
                            isDemoData = payload.isDemoData,
                            transactions = payload.transactions
                        )
                    }.onFailure { err ->
                        Log.w(TAG, "Failed syncing Room overview cache", err)
                    }
                }
            }
        }
    }

    fun refreshRoomOverviewCache() {
        val localRepo = localOverviewRepository ?: return
        val summary = dashboardSummary.value
        val liveTx = (transactionsState.value as? UiState.Success)?.data.orEmpty()
        val activeTx = if (summary.isDemoMode) DemoSampleData.sampleTransactions else liveTx
        viewModelScope.launch {
            runCatching {
                localRepo.syncOverviewCache(
                    userId = currentUserId,
                    businessName = summary.businessName,
                    businessType = summary.businessType,
                    locationNakuru = summary.locationNakuru,
                    isDemoData = summary.isDemoMode,
                    transactions = activeTx
                )
            }.onSuccess {
                _actionMessage.value = "Room KPI overview synced."
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to refresh Room KPI cache."
            }
        }
    }

    private data class RoomSyncPayload(
        val businessName: String,
        val businessType: String,
        val locationNakuru: String,
        val isDemoData: Boolean,
        val transactions: List<BusinessTransaction>
    )

    fun setForecastHorizonMonths(months: Int) {
        _forecastHorizonMonths.value = months.coerceIn(3, 12)
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun setAnalyticsDateFilter(filter: AnalyticsDateFilter) {
        _analyticsFilterState.value = _analyticsFilterState.value.copy(filter = filter)
    }

    fun setCustomDateRange(startIso: String, endIso: String) {
        _analyticsFilterState.value = AnalyticsFilterState(
            filter = AnalyticsDateFilter.CUSTOM,
            customStartIso = startIso,
            customEndIso = endIso
        )
    }

    fun toggleDemoMode(enabled: Boolean) {
        _localDemoPreview.value = enabled
        val existingProfile = (profileState.value as? UiState.Success)?.data
        if (existingProfile != null) {
            viewModelScope.launch {
                val result = repository.setDemoModeEnabled(enabled)
                if (result.isFailure) {
                    _actionMessage.value = result.exceptionOrNull()?.localizedMessage
                        ?: "Could not update workspace mode."
                }
            }
        }
    }

    fun saveBusinessProfile(
        ownerName: String,
        businessName: String,
        businessType: String,
        locationNakuru: String,
        businessSize: String = "Small (6–20 staff)",
        phone: String,
        demoModeEnabled: Boolean
    ) {
        if (_isSaving.value) return
        if (ownerName.isBlank() || businessName.isBlank() || locationNakuru.isBlank()) {
            _actionMessage.value = "Please enter owner name, business name, and location."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveBusinessProfile(
                ownerName = ownerName,
                businessName = businessName,
                businessType = businessType,
                locationNakuru = locationNakuru,
                businessSize = businessSize,
                phone = phone,
                demoModeEnabled = demoModeEnabled
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Business profile synced to Firestore."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to save business profile."
            }
        }
    }

    fun addTransaction(
        type: String,
        title: String,
        category: String,
        amountText: String,
        paymentMethod: String,
        transactionRef: String = "",
        paymentStatus: String = BusinessTransaction.STATUS_COMPLETED,
        transactionDate: String = com.example.data.model.todayIsoDate(),
        notes: String = ""
    ) {
        if (_isSaving.value) return
        val amount = amountText.trim().toDoubleOrNull()
        if (title.isBlank() || amount == null || amount <= 0.0) {
            _actionMessage.value = "Enter a valid description and positive KES amount."
            return
        }
        if (!isValidIsoDate(transactionDate)) {
            _actionMessage.value = "Use YYYY-MM-DD format for transaction date."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addTransaction(
                type = type,
                title = title,
                category = category,
                amountKes = amount,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                paymentStatus = paymentStatus,
                transactionDate = transactionDate,
                notes = notes
            )
            _isSaving.value = false
            val typeLabel = when (type) {
                BusinessTransaction.TYPE_SALE -> "Sale"
                BusinessTransaction.TYPE_EXPENSE -> "Expense"
                else -> "Business record"
            }
            _actionMessage.value = if (result.isSuccess) {
                "$typeLabel recorded (${formatKes(amount)}, $paymentMethod)."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to save transaction."
            }
        }
    }

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
    ) {
        if (_isSaving.value) return
        val amount = amountText.trim().toDoubleOrNull()
        if (title.isBlank() || amount == null || amount <= 0.0) {
            _actionMessage.value = "Enter a valid description and positive KES amount."
            return
        }
        if (!isValidIsoDate(transactionDate)) {
            _actionMessage.value = "Use YYYY-MM-DD format for transaction date."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateTransaction(
                transactionId = transactionId,
                type = type,
                title = title,
                category = category,
                amountKes = amount,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                paymentStatus = paymentStatus,
                transactionDate = transactionDate,
                notes = notes
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Record updated (${formatKes(amount)})."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to update record."
            }
        }
    }

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
    ) {
        if (_isSaving.value) return
        val qty = quantityText.trim().toLongOrNull() ?: 1L
        val unitPrice = unitPriceText.trim().toDoubleOrNull() ?: 0.0
        val discount = discountText.trim().toDoubleOrNull() ?: 0.0
        val manualAmount = manualAmountText.trim().toDoubleOrNull() ?: 0.0
        if (!isValidIsoDate(transactionDate)) {
            _actionMessage.value = "Use YYYY-MM-DD format for sale date."
            return
        }
        if (discount < 0.0) {
            _actionMessage.value = "Discount cannot be negative."
            return
        }
        if (inventoryItemId.isNotBlank() && (qty <= 0L || unitPrice <= 0.0)) {
            _actionMessage.value = "Enter a valid sale quantity (>= 1) and unit price."
            return
        }
        if (inventoryItemId.isBlank() && (title.isBlank() || manualAmount <= 0.0)) {
            _actionMessage.value = "Enter a sale description and positive KES amount."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val lineItem = if (inventoryItemId.isNotBlank()) {
                PosCartLineItem(
                    lineId = "single_1",
                    inventoryItemId = inventoryItemId,
                    itemName = title.ifBlank { "POS Item" },
                    category = category,
                    quantity = qty,
                    unitPriceKes = unitPrice,
                    lineDiscountKes = discount
                )
            } else {
                PosCartLineItem(
                    lineId = "single_1",
                    inventoryItemId = "",
                    itemName = title,
                    category = category,
                    quantity = 1L,
                    unitPriceKes = manualAmount,
                    lineDiscountKes = discount
                )
            }
            val result = repository.recordMultiItemPosCart(
                cartItems = listOf(lineItem),
                defaultCategory = category,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                paymentStatus = paymentStatus,
                transactionDate = transactionDate,
                notes = notes,
                customerId = customerId,
                customerName = customerName,
                cartDiscountKes = 0.0
            )
            _isSaving.value = false
            if (result.isSuccess) {
                val created = result.getOrNull().orEmpty()
                if (created.isNotEmpty()) {
                    _latestPosReceipt.value = created
                }
                _actionMessage.value = if (inventoryItemId.isNotBlank()) {
                    "POS sale recorded & stock updated."
                } else {
                    "Sale recorded."
                }
            } else {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to record POS sale."
            }
        }
    }

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
    ) {
        if (_isSaving.value) return
        if (cartItems.isEmpty()) {
            _actionMessage.value = "Add at least one item to the POS cart."
            return
        }
        val cartDiscount = cartDiscountText.trim().toDoubleOrNull() ?: 0.0
        if (cartDiscount < 0.0) {
            _actionMessage.value = "Cart discount cannot be negative."
            return
        }
        if (!isValidIsoDate(transactionDate)) {
            _actionMessage.value = "Use YYYY-MM-DD format for sale date."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.recordMultiItemPosCart(
                cartItems = cartItems,
                defaultCategory = defaultCategory,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                paymentStatus = paymentStatus,
                transactionDate = transactionDate,
                notes = notes,
                customerId = customerId,
                customerName = customerName,
                cartDiscountKes = cartDiscount
            )
            _isSaving.value = false
            if (result.isSuccess) {
                val created = result.getOrNull().orEmpty()
                if (created.isNotEmpty()) {
                    _latestPosReceipt.value = created
                }
                _actionMessage.value = "Multi-item POS sale (${created.size} line(s)) recorded & stock updated."
            } else {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to record POS cart sale."
            }
        }
    }

    fun dismissLatestPosReceipt() {
        _latestPosReceipt.value = null
    }

    fun cancelSaleTransaction(tx: BusinessTransaction) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.cancelSaleTransaction(tx)
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Sale voided & linked stock/customer totals reversed."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to cancel sale."
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(id)
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Could not delete entry."
            } else {
                _actionMessage.value = "Record removed from Firestore."
            }
        }
    }

    fun addInventoryItem(
        name: String,
        sku: String,
        category: String,
        quantityText: String,
        reorderLevelText: String,
        unitPriceText: String,
        unitCostText: String = "0",
        supplierId: String = "",
        supplierName: String = ""
    ) {
        if (_isSaving.value) return
        val qty = quantityText.trim().toLongOrNull()
        val reorder = reorderLevelText.trim().toLongOrNull() ?: 5L
        val price = unitPriceText.trim().toDoubleOrNull()
        val cost = unitCostText.trim().toDoubleOrNull() ?: 0.0
        if (name.isBlank() || qty == null || qty < 0 || price == null || price < 0 || cost < 0) {
            _actionMessage.value = "Enter valid product name, stock count, and KES unit price."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addInventoryItem(
                name = name,
                sku = sku,
                category = category,
                quantity = qty,
                reorderLevel = reorder.coerceAtLeast(0L),
                unitPriceKes = price,
                unitCostKes = cost,
                supplierId = supplierId,
                supplierName = supplierName
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Added $name to computerized inventory."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to add stock item."
            }
        }
    }

    fun updateInventoryItem(
        itemId: String,
        name: String,
        sku: String,
        category: String,
        quantityText: String,
        reorderLevelText: String,
        unitPriceText: String,
        unitCostText: String = "0",
        supplierId: String = "",
        supplierName: String = ""
    ) {
        if (_isSaving.value) return
        val qty = quantityText.trim().toLongOrNull()
        val reorder = reorderLevelText.trim().toLongOrNull() ?: 5L
        val price = unitPriceText.trim().toDoubleOrNull()
        val cost = unitCostText.trim().toDoubleOrNull() ?: 0.0
        if (name.isBlank() || qty == null || qty < 0 || price == null || price < 0 || cost < 0) {
            _actionMessage.value = "Enter valid product name, stock count, and KES unit price."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateInventoryItem(
                itemId = itemId,
                name = name,
                sku = sku,
                category = category,
                quantity = qty,
                reorderLevel = reorder.coerceAtLeast(0L),
                unitPriceKes = price,
                unitCostKes = cost,
                supplierId = supplierId,
                supplierName = supplierName
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Updated stock item $name."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to update stock item."
            }
        }
    }

    fun adjustInventoryQuantity(item: InventoryItem, delta: Long) {
        if (delta == 0L) return
        if (item.quantity + delta < 0L) {
            _actionMessage.value = "Stock quantity for ${item.name} cannot go below 0."
            return
        }
        viewModelScope.launch {
            val result = repository.adjustInventoryQuantityWithAudit(
                itemId = item.id,
                delta = delta,
                notes = if (delta > 0L) "Manual stock increment (+$delta)" else "Manual stock decrement ($delta)"
            )
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update stock."
            }
        }
    }

    fun deleteInventoryItem(itemId: String) {
        viewModelScope.launch {
            val result = repository.deleteInventoryItem(itemId)
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete item."
            } else {
                _actionMessage.value = "Product deleted from inventory."
            }
        }
    }

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
    ) {
        if (_isSaving.value) return
        val qty = quantityText.trim().toLongOrNull()
        val unitCost = unitCostText.trim().toDoubleOrNull()
        if (itemName.isBlank() || qty == null || qty <= 0L || unitCost == null || unitCost < 0.0) {
            _actionMessage.value = "Enter a valid item, positive restock quantity, and unit cost."
            return
        }
        if (!isValidIsoDate(purchaseDate)) {
            _actionMessage.value = "Use YYYY-MM-DD format for purchase date."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.recordStockPurchase(
                supplierId = supplierId,
                supplierName = supplierName,
                inventoryItemId = inventoryItemId,
                itemName = itemName,
                quantity = qty,
                unitCostKes = unitCost,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                purchaseDate = purchaseDate,
                notes = notes,
                logAsExpenseTransaction = logAsExpense
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Restocked $qty units of $itemName (${formatKes(qty * unitCost)})."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to record stock purchase."
            }
        }
    }

    fun markPurchasePaid(purchaseId: String) {
        viewModelScope.launch {
            val res = repository.updatePurchasePaymentStatus(purchaseId, PurchaseOrderRecord.STATUS_PAID)
            _actionMessage.value = if (res.isSuccess) "Supplier payable marked PAID."
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to update payable."
        }
    }

    fun deletePurchaseOrder(purchaseId: String) {
        viewModelScope.launch {
            val res = repository.deletePurchaseOrder(purchaseId)
            _actionMessage.value = if (res.isSuccess) "Purchase record deleted."
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to delete purchase record."
        }
    }

    fun addCustomer(name: String, phone: String, email: String, location: String, notes: String) {
        if (_isSaving.value) return
        if (name.isBlank()) {
            _actionMessage.value = "Customer name is required."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.addCustomer(name, phone, email, location, notes)
            _isSaving.value = false
            _actionMessage.value = if (res.isSuccess) "Customer $name added."
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to add customer."
        }
    }

    fun updateCustomer(id: String, name: String, phone: String, email: String, location: String, notes: String) {
        if (_isSaving.value) return
        if (name.isBlank()) {
            _actionMessage.value = "Customer name is required."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updateCustomer(id, name, phone, email, location, notes)
            _isSaving.value = false
            _actionMessage.value = if (res.isSuccess) "Customer $name updated."
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to update customer."
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            val res = repository.deleteCustomer(customerId)
            _actionMessage.value = if (res.isSuccess) "Customer removed."
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to delete customer."
        }
    }

    fun addSupplier(
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String
    ) {
        if (_isSaving.value) return
        if (name.isBlank()) {
            _actionMessage.value = "Supplier name is required."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.addSupplier(name, contactPerson, phone, email, location, suppliedProductsSummary, notes)
            _isSaving.value = false
            _actionMessage.value = if (res.isSuccess) "Supplier $name added."
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to add supplier."
        }
    }

    fun updateSupplier(
        id: String,
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String
    ) {
        if (_isSaving.value) return
        if (name.isBlank()) {
            _actionMessage.value = "Supplier name is required."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updateSupplier(id, name, contactPerson, phone, email, location, suppliedProductsSummary, notes)
            _isSaving.value = false
            _actionMessage.value = if (res.isSuccess) "Supplier $name updated."
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to update supplier."
        }
    }

    fun deleteSupplier(supplierId: String) {
        viewModelScope.launch {
            val res = repository.deleteSupplier(supplierId)
            _actionMessage.value = if (res.isSuccess) "Supplier removed."
            else res.exceptionOrNull()?.localizedMessage ?: "Failed to delete supplier."
        }
    }

    fun addCampaign(
        title: String,
        channel: String,
        campaignDate: String = com.example.data.model.todayIsoDate(),
        spendText: String,
        reachText: String,
        engagementText: String,
        inquiriesText: String,
        status: String,
        notes: String
    ) {
        if (_isSaving.value) return
        val spend = spendText.trim().toDoubleOrNull() ?: 0.0
        val reach = reachText.trim().toLongOrNull() ?: 0L
        val engagement = engagementText.trim().toLongOrNull() ?: 0L
        val inquiries = inquiriesText.trim().toLongOrNull() ?: 0L
        if (title.isBlank() || spend < 0.0 || reach < 0L || engagement < 0L || inquiries < 0L) {
            _actionMessage.value = "Enter a campaign title and valid non-negative metrics."
            return
        }
        if (!isValidIsoDate(campaignDate)) {
            _actionMessage.value = "Use YYYY-MM-DD format for campaign date."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addCampaign(
                title = title,
                channel = channel,
                campaignDate = campaignDate,
                spendKes = spend,
                reachCount = reach,
                engagementCount = engagement,
                inquiriesGenerated = inquiries,
                status = status,
                notes = notes
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Digital marketing campaign logged ($channel)."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to save campaign."
            }
        }
    }

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
    ) {
        if (_isSaving.value) return
        val spend = spendText.trim().toDoubleOrNull() ?: 0.0
        val reach = reachText.trim().toLongOrNull() ?: 0L
        val engagement = engagementText.trim().toLongOrNull() ?: 0L
        val inquiries = inquiriesText.trim().toLongOrNull() ?: 0L
        if (title.isBlank() || spend < 0.0 || reach < 0L || engagement < 0L || inquiries < 0L) {
            _actionMessage.value = "Enter a campaign title and valid non-negative metrics."
            return
        }
        if (!isValidIsoDate(campaignDate)) {
            _actionMessage.value = "Use YYYY-MM-DD format for campaign date."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateCampaign(
                campaignId = campaignId,
                title = title,
                channel = channel,
                campaignDate = campaignDate,
                spendKes = spend,
                reachCount = reach,
                engagementCount = engagement,
                inquiriesGenerated = inquiries,
                status = status,
                notes = notes
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Campaign updated ($title)."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to update campaign."
            }
        }
    }

    fun deleteCampaign(campaignId: String) {
        viewModelScope.launch {
            val result = repository.deleteCampaign(campaignId)
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete campaign."
            } else {
                _actionMessage.value = "Campaign deleted."
            }
        }
    }

    fun addCommunicationLog(
        customerName: String,
        channel: String,
        subject: String,
        receivedDate: String = todayIsoDate(),
        responseDate: String = "",
        status: String,
        responseMinutesText: String,
        notes: String,
        category: String = OnlineCommunicationLog.CATEGORY_INQUIRY
    ) {
        if (_isSaving.value) return
        val responseMinutes = responseMinutesText.trim().toLongOrNull() ?: 0L
        if (customerName.isBlank() || subject.isBlank() || responseMinutes < 0L) {
            _actionMessage.value = "Enter customer name, inquiry subject, and valid response minutes."
            return
        }
        if (!isValidIsoDate(receivedDate) || (responseDate.isNotBlank() && !isValidIsoDate(responseDate))) {
            _actionMessage.value = "Use YYYY-MM-DD format for communication dates."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addCommunicationLog(
                customerName = customerName,
                channel = channel,
                subject = subject,
                receivedDate = receivedDate,
                responseDate = responseDate,
                status = status,
                responseMinutes = responseMinutes,
                notes = notes,
                category = category
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Online communication logged for $customerName."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to save communication log."
            }
        }
    }

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
        category: String = OnlineCommunicationLog.CATEGORY_INQUIRY
    ) {
        if (_isSaving.value) return
        val responseMinutes = responseMinutesText.trim().toLongOrNull() ?: 0L
        if (customerName.isBlank() || subject.isBlank() || responseMinutes < 0L) {
            _actionMessage.value = "Enter customer name, inquiry subject, and valid response minutes."
            return
        }
        if (!isValidIsoDate(receivedDate) || (responseDate.isNotBlank() && !isValidIsoDate(responseDate))) {
            _actionMessage.value = "Use YYYY-MM-DD format for communication dates."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateCommunicationLog(
                commId = commId,
                customerName = customerName,
                channel = channel,
                subject = subject,
                receivedDate = receivedDate,
                responseDate = responseDate,
                status = status,
                responseMinutes = responseMinutes,
                notes = notes,
                category = category
            )
            _isSaving.value = false
            _actionMessage.value = if (result.isSuccess) {
                "Communication record updated for $customerName."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to update communication log."
            }
        }
    }

    fun updateCommunicationStatus(commId: String, newStatus: String) {
        viewModelScope.launch {
            val result = repository.updateCommunicationStatus(commId, newStatus)
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update communication status."
            }
        }
    }

    fun deleteCommunicationLog(commId: String) {
        viewModelScope.launch {
            val result = repository.deleteCommunicationLog(commId)
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete log."
            } else {
                _actionMessage.value = "Communication log deleted."
            }
        }
    }

    fun askAiAssistant(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return

        val summary = dashboardSummary.value
        val analytics = analyticsReport.value
        val p4 = prompt4Analytics.value
        val contextBlock = buildString {
            appendLine("Business Name: ${summary.businessName} (${summary.businessType}, ${summary.businessSize}, ${summary.locationNakuru})")
            appendLine("Data Mode: ${if (summary.isDemoMode) "DEMO DATA — SAMPLE ONLY (not live business results)" else "LIVE CLOUD FIRESTORE WORKSPACE"}")
            appendLine("Active Analytics Date Filter: ${analytics.filterState.filter.label} (${analytics.activeWindow.label})")
            appendLine("IV1 Digital Marketing: ${analytics.marketing.campaignCount} campaigns | Spend: ${formatKes(analytics.marketing.totalSpendKes)} | Reach: ${analytics.marketing.totalReach} | Engagement: ${analytics.marketing.totalEngagement} | Inquiries: ${analytics.marketing.totalInquiries}")
            appendLine("IV2 Digital Payment Systems: ${analytics.payments.digitalAdoptionPct ?: 0}% digital adoption (${analytics.payments.digitalTransactionCount}/${analytics.payments.totalTransactionCount} txns) | Digital Total: ${formatKes(analytics.payments.totalDigitalPaymentsKes)} | Digital Sales Share: ${analytics.payments.digitalAsPercentOfSales?.let { "${it.toInt()}%" } ?: "N/A"}")
            appendLine("IV3 Computerized Record Keeping: Revenue: ${formatKes(analytics.recordKeeping.recordedSalesKes)} | Expenses: ${formatKes(analytics.recordKeeping.recordedExpensesKes)} | Estimated Net Profit: ${formatKes(analytics.recordKeeping.estimatedNetProfitKes)} | Gross Profit: ${p4.totalGrossProfitKes?.let { formatKes(it) } ?: "Cost missing"} | Completeness: ${analytics.recordKeeping.recordCompletenessPct ?: 0}% | Inventory SKUs: ${analytics.recordKeeping.inventoryCount} (Low Stock: ${analytics.recordKeeping.lowStockCount}, Value: ${formatKes(summary.totalInventoryRetailValueKes)})")
            appendLine("IV4 Online Communication & CRM/Supply: ${analytics.communication.totalEnquiries} logs | Response Rate: ${analytics.communication.responseRatePct?.let { "${it.toInt()}%" } ?: "N/A"} | Customers: ${summary.customerCount} (${summary.repeatCustomerCount} repeat) | Suppliers: ${summary.supplierCount} | Pending Payables: ${formatKes(summary.pendingPayablesKes)}")
            appendLine("Monthly Trajectory (${p4.monthlyRows.size} mo): OLS Slope=${String.format(java.util.Locale.US, "%+.0f", p4.projection.slopeKesPerMonth)} KES/mo | Sufficient >=3m history: ${p4.projection.hasSufficientHistory}")
            if (analytics.dataGapsAndLimitations.isNotEmpty()) {
                appendLine("Missing Data / Limitations (DO NOT INVENT): ${analytics.dataGapsAndLimitations.joinToString("; ")}")
            }
        }

        val userMsg = AiChatMessage(
            id = "u_${System.currentTimeMillis()}",
            isUser = true,
            text = cleanPrompt
        )
        _aiMessages.value = _aiMessages.value + userMsg

        viewModelScope.launch {
            _aiState.value = AiResponseState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    aiRepository.streamBusinessAdvice(cleanPrompt, contextBlock).collect { chunk ->
                        accumulated.append(chunk)
                        _aiState.value = AiResponseState.Streaming(accumulated.toString())
                    }
                }
                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _aiMessages.value = _aiMessages.value + AiChatMessage(
                        id = "a_${System.currentTimeMillis()}",
                        isUser = false,
                        text = finalText
                    )
                    _aiState.value = AiResponseState.Success(finalText)
                } else {
                    _aiState.value = AiResponseState.Error("Gemini returned an empty response. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Assistant timed out", e)
                _aiState.value = AiResponseState.Error("AI response timed out after 60 seconds. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Assistant failed", e)
                _aiState.value = AiResponseState.Error(
                    e.localizedMessage ?: "Could not reach Gemini AI Business Intelligence."
                )
            }
        }
    }

    fun clearOverviewSalesSummary() {
        _salesOverviewAiState.value = SalesOverviewAiState.Idle
    }

    fun generateOverviewSalesSummary() {
        val summary = dashboardSummary.value
        val roomState = roomOverviewState.value
        val liveTx = (transactionsState.value as? UiState.Success)?.data.orEmpty()
        val activeTx = (if (summary.isDemoMode) DemoSampleData.sampleTransactions else liveTx)
            .filter { it.isActiveLedgerEntry() }
        val salesTransactions = activeTx.filter { it.type == BusinessTransaction.TYPE_SALE }

        if (salesTransactions.isEmpty()) {
            _salesOverviewAiState.value = SalesOverviewAiState.EmptySales(
                "No sales records found yet. Log at least one sale (or enable Demo Mode) so Firebase AI can analyze your sales performance."
            )
            return
        }

        val totalSalesKes = salesTransactions.sumOf { it.amountKes }
        val contextBlock = buildOverviewSalesContext(
            businessName = summary.businessName,
            businessType = summary.businessType,
            businessSize = summary.businessSize,
            locationNakuru = summary.locationNakuru,
            isDemoMode = summary.isDemoMode,
            activeTransactions = activeTx,
            expenseToSalesRatioPct = roomState.expenseToSalesRatioPct,
            netMarginPct = roomState.netMarginPct
        )

        val prompt = "Analyze the existing sales data above and provide a clear, concise text-based performance summary " +
            "for this Nakuru SME owner in Kenyan Shillings (KES). Cover: (1) Overall Sales & Margin Health, " +
            "(2) Top Revenue Categories & Digital vs Cash Payment Adoption, and (3) Two practical next actions grounded " +
            "strictly in the recorded numbers. Mention clearly if DEMO DATA is active and do not fabricate missing figures."

        viewModelScope.launch {
            _salesOverviewAiState.value = SalesOverviewAiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    aiRepository.streamBusinessAdvice(prompt, contextBlock).collect { chunk ->
                        accumulated.append(chunk)
                        _salesOverviewAiState.value = SalesOverviewAiState.Streaming(accumulated.toString())
                    }
                }
                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _salesOverviewAiState.value = SalesOverviewAiState.Success(
                        summaryText = finalText,
                        isDemoData = summary.isDemoMode,
                        analyzedSalesCount = salesTransactions.size,
                        totalSalesKes = totalSalesKes
                    )
                } else {
                    _salesOverviewAiState.value = SalesOverviewAiState.Error(
                        "Firebase AI returned an empty sales summary. Please try again."
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Overview AI sales analysis timed out", e)
                _salesOverviewAiState.value = SalesOverviewAiState.Error(
                    "Sales analysis timed out after 60 seconds. Please retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Overview AI sales analysis failed", e)
                _salesOverviewAiState.value = SalesOverviewAiState.Error(
                    e.localizedMessage ?: "Failed to analyze sales data with Firebase AI."
                )
            }
        }
    }

    fun analyzeTransactionHistory() {
        val allowDemo = dashboardSummary.value.isDemoMode
        viewModelScope.launch {
            _growthAnalysisState.value = GrowthAnalysisUiState.Loading
            try {
                when (
                    val result = growthAnalysisService.analyzeTransactionHistory(
                        userId = currentUserId,
                        allowDemoFallback = allowDemo
                    )
                ) {
                    is GrowthAnalysisResult.Success -> {
                        _growthAnalysisState.value = GrowthAnalysisUiState.Success(result.report)
                    }

                    is GrowthAnalysisResult.EmptyLedger -> {
                        _growthAnalysisState.value = GrowthAnalysisUiState.EmptyLedger(result.message)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Business growth analysis timed out", e)
                _growthAnalysisState.value = GrowthAnalysisUiState.Error(
                    "Analysis timed out after 60 seconds. Please retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Business growth analysis failed", e)
                _growthAnalysisState.value = GrowthAnalysisUiState.Error(
                    e.localizedMessage ?: "Failed to analyze Cloud Firestore records."
                )
            }
        }
    }

    fun generateWorkspaceBackupJson(
        onResult: (Result<Pair<String, BackupSummary>>) -> Unit
    ) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            _backupRestoreState.value = BackupRestoreUiState.Exporting
            val snapshotRes = repository.fetchWorkspaceBackupSnapshot(currentUserId)
            val payload = snapshotRes.getOrElse {
                // Fallback to currently synced live StateFlows if offline/timed out
                WorkspaceBackupPayload(
                    profile = (profileState.value as? UiState.Success)?.data,
                    transactions = (transactionsState.value as? UiState.Success)?.data.orEmpty(),
                    inventory = (inventoryState.value as? UiState.Success)?.data.orEmpty(),
                    customers = (customersState.value as? UiState.Success)?.data.orEmpty(),
                    suppliers = (suppliersState.value as? UiState.Success)?.data.orEmpty(),
                    purchases = (purchasesState.value as? UiState.Success)?.data.orEmpty(),
                    stockMovements = (stockMovementsState.value as? UiState.Success)?.data.orEmpty(),
                    campaigns = (campaignsState.value as? UiState.Success)?.data.orEmpty(),
                    communications = (communicationsState.value as? UiState.Success)?.data.orEmpty()
                )
            }
            val json = ProjectXBackupManager.serializeWorkspaceBackup(payload)
            val parsed = ProjectXBackupManager.parseAndValidateBackup(json)
            _isSaving.value = false
            if (parsed.isSuccess) {
                val summary = parsed.getOrThrow().summary
                _backupRestoreState.value = BackupRestoreUiState.Success(
                    "Backup ready: ${summary.totalRecords} live record(s) exported."
                )
                onResult(Result.success(json to summary))
            } else {
                val errMsg = if (payload.profile == null &&
                    payload.transactions.isEmpty() &&
                    payload.inventory.isEmpty() &&
                    payload.customers.isEmpty() &&
                    payload.suppliers.isEmpty() &&
                    payload.purchases.isEmpty() &&
                    payload.campaigns.isEmpty() &&
                    payload.communications.isEmpty()
                ) {
                    "No live business records found to back up yet."
                } else {
                    parsed.exceptionOrNull()?.localizedMessage ?: "Failed to generate backup."
                }
                _backupRestoreState.value = BackupRestoreUiState.Error(errMsg)
                onResult(Result.failure(IllegalStateException(errMsg)))
            }
        }
    }

    fun reportBackupExportOutcome(success: Boolean, message: String) {
        _backupRestoreState.value = if (success) {
            BackupRestoreUiState.Success(message)
        } else {
            BackupRestoreUiState.Error(message)
        }
        _actionMessage.value = message
    }

    fun inspectBackupFileJson(rawJson: String, sourceLabel: String) {
        val parsed = ProjectXBackupManager.parseAndValidateBackup(rawJson)
        if (parsed.isSuccess) {
            _backupRestoreState.value = BackupRestoreUiState.ReadyToConfirmRestore(
                backup = parsed.getOrThrow(),
                sourceLabel = sourceLabel
            )
        } else {
            val errMsg = parsed.exceptionOrNull()?.localizedMessage ?: "Invalid Project X backup file."
            _backupRestoreState.value = BackupRestoreUiState.Error(errMsg)
            _actionMessage.value = errMsg
        }
    }

    fun confirmRestoreBackup(replaceExisting: Boolean) {
        val ready = _backupRestoreState.value as? BackupRestoreUiState.ReadyToConfirmRestore ?: return
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            _backupRestoreState.value = BackupRestoreUiState.Restoring
            val res = repository.restoreWorkspaceBackup(
                backup = ready.backup,
                replaceExisting = replaceExisting
            )
            _isSaving.value = false
            if (res.isSuccess) {
                val summary = res.getOrThrow()
                val modeLabel = if (replaceExisting) "replaced existing workspace with" else "merged"
                val msg = "Restore complete: $modeLabel ${summary.totalRecords} record(s)."
                _backupRestoreState.value = BackupRestoreUiState.Success(msg)
                _actionMessage.value = msg
            } else {
                val msg = res.exceptionOrNull()?.localizedMessage ?: "Failed to restore workspace backup."
                _backupRestoreState.value = BackupRestoreUiState.Error(msg)
                _actionMessage.value = msg
            }
        }
    }

    fun dismissRestoreConfirmation() {
        _backupRestoreState.value = BackupRestoreUiState.Idle
    }

    fun clearBackupRestoreState() {
        _backupRestoreState.value = BackupRestoreUiState.Idle
    }

    private fun unwrapFirestoreError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "DashboardViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val AI_TIMEOUT_MS = 60_000L
    }
}

internal fun buildOverviewSalesContext(
    businessName: String,
    businessType: String,
    businessSize: String,
    locationNakuru: String,
    isDemoMode: Boolean,
    activeTransactions: List<BusinessTransaction>,
    expenseToSalesRatioPct: Int?,
    netMarginPct: Int?
): String {
    val validTx = activeTransactions.filter { it.isActiveLedgerEntry() }
    val salesTransactions = validTx.filter { it.type == BusinessTransaction.TYPE_SALE }
    val totalSalesKes = salesTransactions.sumOf { it.amountKes }
    val totalExpensesKes = validTx
        .filter { it.type == BusinessTransaction.TYPE_EXPENSE }
        .sumOf { it.amountKes }
    val netProfitKes = totalSalesKes - totalExpensesKes
    val avgTicketKes = if (salesTransactions.isNotEmpty()) totalSalesKes / salesTransactions.size else 0.0
    val categoryTotals = salesTransactions
        .groupBy { it.category.ifBlank { "Uncategorized" } }
        .mapValues { (_, list) -> list.sumOf { it.amountKes } to list.size }
        .entries
        .sortedByDescending { it.value.first }
    val paymentTotals = salesTransactions
        .groupBy { it.paymentMethod }
        .mapValues { (_, list) -> list.sumOf { it.amountKes } to list.size }
        .entries
        .sortedByDescending { it.value.first }

    return buildString {
        appendLine("Business: $businessName ($businessType, $businessSize, $locationNakuru)")
        appendLine("Dataset Mode: ${if (isDemoMode) "DEMO DATA — SAMPLE ONLY (do not treat as real field study findings)" else "LIVE CLOUD FIRESTORE & ROOM SQLITE CACHE"}")
        appendLine("Total Sales: ${formatKes(totalSalesKes)} across ${salesTransactions.size} sale(s) | Avg Ticket: ${formatKes(avgTicketKes)}")
        appendLine("Total Expenses: ${formatKes(totalExpensesKes)} | Estimated Net Profit: ${formatKes(netProfitKes)}")
        if (expenseToSalesRatioPct != null) {
            appendLine("Room SQLite Expense-to-Sales Ratio: $expenseToSalesRatioPct% | Net Margin: ${netMarginPct ?: 0}%")
        }
        appendLine("Sales by Category:")
        categoryTotals.forEach { (cat, pair) ->
            appendLine(" - $cat: ${formatKes(pair.first)} (${pair.second} sales)")
        }
        appendLine("Sales by Payment Method:")
        paymentTotals.forEach { (method, pair) ->
            appendLine(" - $method: ${formatKes(pair.first)} (${pair.second} sales)")
        }
        appendLine("Recorded Sales Ledger (up to 30):")
        salesTransactions.take(30).forEachIndexed { index, tx ->
            appendLine(" ${index + 1}. ${tx.title} | ${tx.category} | ${formatKes(tx.amountKes)} | ${tx.paymentMethod} (${tx.paymentStatus}) | ${tx.effectiveDate()}")
        }
    }
}
