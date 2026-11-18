package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.analytics.AnalyticsFilterState
import com.example.data.analytics.Prompt4AnalyticsBundle
import com.example.data.analytics.Prompt4ForecastingEngine
import com.example.data.analytics.SmeAnalyticsEngine
import com.example.data.analytics.SmeFullAnalyticsReport
import com.example.data.backup.BackupSummary
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
import com.example.data.model.StockMovementRecord
import com.example.data.model.SupplierRecord
import com.example.data.model.formatKes
import com.example.data.model.todayIsoDate
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.FirebaseAiAssistantRepository
import com.example.data.repository.RoomBusinessOverviewState
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiResponseState
import com.example.ui.viewmodel.BackupRestoreUiState
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.GrowthAnalysisUiState
import com.example.ui.viewmodel.SalesOverviewAiState
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.buildOverviewSalesContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Locale

@Composable
fun PublicDemoWorkspace(
    onSignInClick: () -> Unit,
    onExitToWelcome: () -> Unit
) {
    val demoViewModel: PublicDemoViewModel = viewModel()
    ProjectXWorkspaceShell(
        host = demoViewModel,
        defaultDisplayName = "Public Visitor",
        defaultEmail = "Public Demo Preview (Sign in to save)",
        onSignOutOrSignIn = onSignInClick,
        isPublicPreviewMode = true,
        onExitPublicPreview = onExitToWelcome
    )
}

/**
 * In-memory interactive Public Demo workspace host backed exclusively by [DemoSampleData]
 * with zero Cloud Firestore reads or writes until the user signs in with Google.
 */
class PublicDemoViewModel(
    private val aiRepository: AiAssistantRepository = FirebaseAiAssistantRepository()
) : ViewModel(), ProjectXWorkspaceHost {

    private val demoProfile = BusinessProfile(
        userId = "public_demo",
        ownerName = "Nakuru Demo SME",
        businessName = "Biashara Fresh Mart (Demo)",
        businessType = "Retail Shop",
        locationNakuru = "Biashara Street, Nakuru",
        businessSize = "Small (6–20 staff)",
        phone = "+254700000000",
        demoModeEnabled = true
    )

    override val profileState: StateFlow<UiState<BusinessProfile?>> =
        MutableStateFlow<UiState<BusinessProfile?>>(UiState.Success(demoProfile)).asStateFlow()

    override val transactionsState: StateFlow<UiState<List<BusinessTransaction>>> =
        MutableStateFlow<UiState<List<BusinessTransaction>>>(UiState.Success(DemoSampleData.sampleTransactions)).asStateFlow()

    override val inventoryState: StateFlow<UiState<List<InventoryItem>>> =
        MutableStateFlow<UiState<List<InventoryItem>>>(UiState.Success(DemoSampleData.sampleInventory)).asStateFlow()

    override val campaignsState: StateFlow<UiState<List<DigitalMarketingCampaign>>> =
        MutableStateFlow<UiState<List<DigitalMarketingCampaign>>>(UiState.Success(DemoSampleData.sampleCampaigns)).asStateFlow()

    override val communicationsState: StateFlow<UiState<List<OnlineCommunicationLog>>> =
        MutableStateFlow<UiState<List<OnlineCommunicationLog>>>(UiState.Success(DemoSampleData.sampleCommunications)).asStateFlow()

    override val customersState: StateFlow<UiState<List<CustomerRecord>>> =
        MutableStateFlow<UiState<List<CustomerRecord>>>(UiState.Success(DemoSampleData.sampleCustomers)).asStateFlow()

    override val suppliersState: StateFlow<UiState<List<SupplierRecord>>> =
        MutableStateFlow<UiState<List<SupplierRecord>>>(UiState.Success(DemoSampleData.sampleSuppliers)).asStateFlow()

    override val purchasesState: StateFlow<UiState<List<PurchaseOrderRecord>>> =
        MutableStateFlow<UiState<List<PurchaseOrderRecord>>>(UiState.Success(DemoSampleData.samplePurchases)).asStateFlow()

    override val stockMovementsState: StateFlow<UiState<List<StockMovementRecord>>> =
        MutableStateFlow<UiState<List<StockMovementRecord>>>(UiState.Success(DemoSampleData.sampleStockMovements)).asStateFlow()

    private val _filterState = MutableStateFlow(AnalyticsFilterState())
    private val _forecastHorizonMonths = MutableStateFlow(6)
    override val forecastHorizonMonths: StateFlow<Int> = _forecastHorizonMonths.asStateFlow()

    override val analyticsReport: StateFlow<SmeFullAnalyticsReport> = _filterState
        .combine(_forecastHorizonMonths) { filter, _ ->
            SmeAnalyticsEngine.compute(
                isDemoMode = true,
                filterState = filter,
                allTransactions = DemoSampleData.sampleTransactions,
                inventory = DemoSampleData.sampleInventory,
                allCampaigns = DemoSampleData.sampleCampaigns,
                allCommunications = DemoSampleData.sampleCommunications
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SmeAnalyticsEngine.compute(
                isDemoMode = true,
                filterState = AnalyticsFilterState(),
                allTransactions = DemoSampleData.sampleTransactions,
                inventory = DemoSampleData.sampleInventory,
                allCampaigns = DemoSampleData.sampleCampaigns,
                allCommunications = DemoSampleData.sampleCommunications
            )
        )

    override val prompt4Analytics: StateFlow<Prompt4AnalyticsBundle> = _forecastHorizonMonths
        .combine(_filterState) { horizon, _ ->
            Prompt4ForecastingEngine.compute(
                isDemoMode = true,
                allTransactions = DemoSampleData.sampleTransactions,
                inventory = DemoSampleData.sampleInventory,
                customers = DemoSampleData.sampleCustomers,
                horizonMonths = horizon
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = Prompt4ForecastingEngine.compute(
                isDemoMode = true,
                allTransactions = DemoSampleData.sampleTransactions,
                inventory = DemoSampleData.sampleInventory,
                customers = DemoSampleData.sampleCustomers,
                horizonMonths = 6
            )
        )

    override val dashboardSummary: StateFlow<DashboardSummary> = prompt4Analytics
        .combine(_filterState) { p4, _ -> buildDemoSummary(p4) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = buildDemoSummary(prompt4Analytics.value)
        )

    override val roomOverviewState: StateFlow<RoomBusinessOverviewState> =
        MutableStateFlow(
            RoomBusinessOverviewState(
                isCacheReady = true,
                isDemoData = true,
                businessName = demoProfile.businessName,
                businessType = demoProfile.businessType,
                locationNakuru = demoProfile.locationNakuru
            )
        ).asStateFlow()

    private val _salesOverviewAiState = MutableStateFlow<SalesOverviewAiState>(SalesOverviewAiState.Idle)
    override val salesOverviewAiState: StateFlow<SalesOverviewAiState> = _salesOverviewAiState.asStateFlow()

    private val _aiState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    override val aiState: StateFlow<AiResponseState> = _aiState.asStateFlow()

    private val _growthAnalysisState = MutableStateFlow<GrowthAnalysisUiState>(GrowthAnalysisUiState.Idle)
    override val growthAnalysisState: StateFlow<GrowthAnalysisUiState> = _growthAnalysisState.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    override val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    override val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    override val isSaving: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val hasPendingSyncWrites: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val backupRestoreState: StateFlow<BackupRestoreUiState> =
        MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle).asStateFlow()

    private val _latestPosReceipt = MutableStateFlow<List<BusinessTransaction>?>(null)
    override val latestPosReceipt: StateFlow<List<BusinessTransaction>?> = _latestPosReceipt.asStateFlow()

    private fun buildDemoSummary(p4: Prompt4AnalyticsBundle): DashboardSummary {
        val activeTx = DemoSampleData.sampleTransactions.filter { it.isActiveLedgerEntry() }
        val sales = activeTx.filter { it.type == BusinessTransaction.TYPE_SALE }
        val expenses = activeTx.filter { it.type == BusinessTransaction.TYPE_EXPENSE }
        val others = activeTx.filter { it.type == BusinessTransaction.TYPE_OTHER }
        val digitalTx = activeTx.filter { it.isDigitalPayment }
        val totalSales = sales.sumOf { it.amountKes }
        val totalExpenses = expenses.sumOf { it.amountKes }
        val digitalCount = digitalTx.size
        val adoptionPct = if (activeTx.isNotEmpty()) ((digitalCount * 100.0) / activeTx.size).toInt() else 0
        val activeComms = DemoSampleData.sampleCommunications
        val convertedComms = activeComms.count { it.status == OnlineCommunicationLog.STATUS_CONVERTED }
        val respondedOrConverted = activeComms.count {
            it.status == OnlineCommunicationLog.STATUS_RESPONDED || it.status == OnlineCommunicationLog.STATUS_CONVERTED
        }
        val avgResp = if (activeComms.isNotEmpty()) activeComms.map { it.responseMinutes }.average().toInt() else 0
        val pendingTx = activeTx.filter { it.paymentStatus == BusinessTransaction.STATUS_PENDING }
        val pendingPo = DemoSampleData.samplePurchases.filter { it.paymentStatus != PurchaseOrderRecord.STATUS_PAID }

        return DashboardSummary(
            isDemoMode = true,
            hasAnyLiveRecords = false,
            businessName = demoProfile.businessName,
            businessType = demoProfile.businessType,
            locationNakuru = demoProfile.locationNakuru,
            businessSize = demoProfile.businessSize,
            totalSalesKes = totalSales,
            totalExpensesKes = totalExpenses,
            otherTransactionsKes = others.sumOf { it.amountKes },
            netBalanceKes = totalSales - totalExpenses,
            lowStockCount = DemoSampleData.sampleInventory.count { it.isLowStock },
            salesTransactionCount = sales.size,
            otherTransactionCount = others.size,
            transactionCount = activeTx.size,
            inventoryCount = DemoSampleData.sampleInventory.size,
            digitalPaymentCount = digitalCount,
            digitalPaymentAdoptionPct = adoptionPct,
            totalDigitalPaymentsKes = digitalTx.sumOf { it.amountKes },
            mpesaSalesKes = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_MPESA }.sumOf { it.amountKes },
            bankSalesKes = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_BANK }.sumOf { it.amountKes },
            cardSalesKes = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_CARD }.sumOf { it.amountKes },
            otherDigitalSalesKes = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_OTHER_DIGITAL }.sumOf { it.amountKes },
            cashSalesKes = sales.filter { it.paymentMethod == BusinessTransaction.METHOD_CASH }.sumOf { it.amountKes },
            campaignCount = DemoSampleData.sampleCampaigns.size,
            totalMarketingSpendKes = DemoSampleData.sampleCampaigns.sumOf { it.spendKes },
            totalCampaignReach = DemoSampleData.sampleCampaigns.sumOf { it.reachCount },
            totalCampaignEngagement = DemoSampleData.sampleCampaigns.sumOf { it.engagementCount },
            totalMarketingInquiries = DemoSampleData.sampleCampaigns.sumOf { it.inquiriesGenerated },
            communicationCount = activeComms.size,
            respondedOrConvertedComms = respondedOrConverted,
            convertedCustomerLeads = convertedComms,
            avgResponseMinutes = avgResp,
            customerCount = DemoSampleData.sampleCustomers.size,
            repeatCustomerCount = DemoSampleData.sampleCustomers.count { it.isRepeatCustomer },
            supplierCount = DemoSampleData.sampleSuppliers.size,
            pendingTransactionsCount = pendingTx.size,
            pendingTransactionsKes = pendingTx.sumOf { it.amountKes },
            pendingPayablesCount = pendingPo.size,
            pendingPayablesKes = pendingPo.sumOf { it.totalCostKes },
            totalInventoryRetailValueKes = p4.totalInventoryRetailValueKes,
            totalInventoryCostValueKes = p4.totalInventoryCostValueKes,
            grossProfitKes = p4.totalGrossProfitKes,
            hasCostDataForGrossProfit = p4.hasCostDataForGrossProfit,
            latestMoMSalesGrowthPct = p4.latestMoMSalesGrowthPct
        )
    }

    private fun notifySignInRequired(feature: String = "save live SME records") {
        _actionMessage.value = "Public Demo Preview: Tap 'Sign In' at the top to $feature in Cloud Firestore."
    }

    override fun clearActionMessage() {
        _actionMessage.value = null
    }

    override fun dismissLatestPosReceipt() {
        _latestPosReceipt.value = null
    }

    override fun setAnalyticsDateFilter(filter: AnalyticsDateFilter) {
        _filterState.value = _filterState.value.copy(filter = filter)
    }

    override fun setCustomDateRange(startIso: String, endIso: String) {
        _filterState.value = AnalyticsFilterState(
            filter = AnalyticsDateFilter.CUSTOM,
            customStartIso = startIso,
            customEndIso = endIso
        )
    }

    override fun toggleDemoMode(enabled: Boolean) {
        if (!enabled) notifySignInRequired("switch to your private Live Cloud Firestore workspace")
    }

    override fun refreshRoomOverviewCache() {
        _actionMessage.value = "Public Demo KPIs refreshed."
    }

    override fun clearOverviewSalesSummary() {
        _salesOverviewAiState.value = SalesOverviewAiState.Idle
    }

    override fun generateOverviewSalesSummary() {
        val summary = dashboardSummary.value
        val activeTx = DemoSampleData.sampleTransactions.filter { it.isActiveLedgerEntry() }
        val sales = activeTx.filter { it.type == BusinessTransaction.TYPE_SALE }
        val contextBlock = buildOverviewSalesContext(
            businessName = summary.businessName,
            businessType = summary.businessType,
            businessSize = summary.businessSize,
            locationNakuru = summary.locationNakuru,
            isDemoMode = true,
            activeTransactions = activeTx,
            expenseToSalesRatioPct = null,
            netMarginPct = null
        )
        val prompt = "Analyze this Nakuru SME sample sales dataset and provide a concise performance summary in KES."
        viewModelScope.launch {
            _salesOverviewAiState.value = SalesOverviewAiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(45_000L) {
                    aiRepository.streamBusinessAdvice(prompt, contextBlock).collect { chunk ->
                        accumulated.append(chunk)
                        _salesOverviewAiState.value = SalesOverviewAiState.Streaming(accumulated.toString())
                    }
                }
                val text = accumulated.toString().trim()
                _salesOverviewAiState.value = if (text.isNotEmpty()) {
                    SalesOverviewAiState.Success(text, isDemoData = true, analyzedSalesCount = sales.size, totalSalesKes = sales.sumOf { it.amountKes })
                } else {
                    SalesOverviewAiState.Error("Gemini returned an empty response.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _salesOverviewAiState.value = SalesOverviewAiState.Error(e.localizedMessage ?: "Sign in or check network for live AI summary.")
            }
        }
    }

    override fun recordPosSale(
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
        val qty = quantityText.trim().toLongOrNull() ?: 1L
        val unitPrice = unitPriceText.trim().toDoubleOrNull() ?: 0.0
        val disc = (discountText.trim().toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        val manualAmt = manualAmountText.trim().toDoubleOrNull() ?: 0.0
        val receiptNo = "PX-DEMO-${(System.currentTimeMillis() % 9000L + 1000L)}"
        val netAmt = if (inventoryItemId.isNotBlank()) (qty * unitPrice - disc).coerceAtLeast(1.0) else manualAmt.coerceAtLeast(1.0)
        val line = BusinessTransaction(
            id = "demo_pos_${System.currentTimeMillis()}",
            type = BusinessTransaction.TYPE_SALE,
            title = title.ifBlank { "Demo Counter Sale" },
            category = category,
            amountKes = netAmt,
            paymentMethod = paymentMethod,
            transactionRef = transactionRef,
            paymentStatus = paymentStatus,
            transactionDate = transactionDate.ifBlank { todayIsoDate() },
            notes = notes,
            customerId = customerId,
            customerName = customerName,
            inventoryItemId = inventoryItemId,
            inventoryItemName = title,
            quantitySold = if (inventoryItemId.isNotBlank()) qty else 1L,
            unitPriceKes = if (inventoryItemId.isNotBlank()) unitPrice else manualAmt,
            discountKes = disc,
            receiptNumber = receiptNo
        )
        _latestPosReceipt.value = listOf(line)
        _actionMessage.value = "Demo receipt $receiptNo generated (Sign in to save to Cloud Firestore)."
    }

    override fun recordPosCartSale(
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
        if (cartItems.isEmpty()) return
        val cartDisc = (cartDiscountText.trim().toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        val preTotal = cartItems.sumOf { it.netBeforeCartDiscountKes }.coerceAtLeast(1.0)
        val receiptNo = "PX-DEMO-${(System.currentTimeMillis() % 9000L + 1000L)}"
        var allocated = 0.0
        val lines = cartItems.mapIndexed { idx, item ->
            val extraDisc = if (cartDisc <= 0.0) 0.0
            else if (idx == cartItems.lastIndex) (cartDisc - allocated).coerceAtLeast(0.0)
            else kotlin.math.round((cartDisc * (item.netBeforeCartDiscountKes / preTotal)) * 100.0) / 100.0.also { allocated += it }
            val lineNet = (item.netBeforeCartDiscountKes - extraDisc).coerceAtLeast(0.01)
            BusinessTransaction(
                id = "demo_cart_${idx}_${System.currentTimeMillis()}",
                type = BusinessTransaction.TYPE_SALE,
                title = "${item.itemName} x${item.quantity}",
                category = item.category.ifBlank { defaultCategory },
                amountKes = lineNet,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                paymentStatus = paymentStatus,
                transactionDate = transactionDate.ifBlank { todayIsoDate() },
                notes = notes,
                customerId = customerId,
                customerName = customerName,
                inventoryItemId = item.inventoryItemId,
                inventoryItemName = item.itemName,
                quantitySold = item.quantity,
                unitPriceKes = item.unitPriceKes,
                discountKes = item.lineDiscountKes + extraDisc,
                receiptNumber = receiptNo
            )
        }
        _latestPosReceipt.value = lines
        _actionMessage.value = "Demo multi-item receipt $receiptNo ready (Sign in to save permanently)."
    }

    override fun askAiAssistant(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        val summary = dashboardSummary.value
        val analytics = analyticsReport.value
        val contextBlock = "PUBLIC DEMO MODE | ${summary.businessName} | Sales: ${formatKes(summary.totalSalesKes)} | Expenses: ${formatKes(summary.totalExpensesKes)} | Digital Adoption: ${analytics.payments.digitalAdoptionPct ?: 0}%"
        _aiMessages.value = _aiMessages.value + AiChatMessage("u_${System.currentTimeMillis()}", true, clean)
        viewModelScope.launch {
            _aiState.value = AiResponseState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(45_000L) {
                    aiRepository.streamBusinessAdvice(clean, contextBlock).collect { chunk ->
                        accumulated.append(chunk)
                        _aiState.value = AiResponseState.Streaming(accumulated.toString())
                    }
                }
                val text = accumulated.toString().trim()
                if (text.isNotEmpty()) {
                    _aiMessages.value = _aiMessages.value + AiChatMessage("a_${System.currentTimeMillis()}", false, text)
                    _aiState.value = AiResponseState.Success(text)
                } else {
                    _aiState.value = AiResponseState.Error("Gemini returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                _aiState.value = AiResponseState.Error("AI response timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiResponseState.Error(e.localizedMessage ?: "AI preview unavailable offline.")
            }
        }
    }

    override fun analyzeTransactionHistory() {
        notifySignInRequired("run full Cloud Firestore historical growth audits")
    }

    override fun setForecastHorizonMonths(months: Int) {
        _forecastHorizonMonths.value = months.coerceIn(3, 12)
    }

    override fun addCampaign(title: String, channel: String, campaignDate: String, spendText: String, reachText: String, engagementText: String, inquiriesText: String, status: String, notes: String) = notifySignInRequired("save marketing campaigns")
    override fun updateCampaign(campaignId: String, title: String, channel: String, campaignDate: String, spendText: String, reachText: String, engagementText: String, inquiriesText: String, status: String, notes: String) = notifySignInRequired("update campaigns")
    override fun deleteCampaign(campaignId: String) = notifySignInRequired("delete campaigns")
    override fun addTransaction(type: String, title: String, category: String, amountText: String, paymentMethod: String, transactionRef: String, paymentStatus: String, transactionDate: String, notes: String) = notifySignInRequired("save ledger records")
    override fun updateTransaction(transactionId: String, type: String, title: String, category: String, amountText: String, paymentMethod: String, transactionRef: String, paymentStatus: String, transactionDate: String, notes: String) = notifySignInRequired("edit ledger records")
    override fun cancelSaleTransaction(tx: BusinessTransaction) = notifySignInRequired("void sales")
    override fun deleteTransaction(id: String) = notifySignInRequired("delete transactions")
    override fun addInventoryItem(name: String, sku: String, category: String, quantityText: String, reorderLevelText: String, unitPriceText: String, unitCostText: String, supplierId: String, supplierName: String) = notifySignInRequired("add inventory SKUs")
    override fun updateInventoryItem(itemId: String, name: String, sku: String, category: String, quantityText: String, reorderLevelText: String, unitPriceText: String, unitCostText: String, supplierId: String, supplierName: String) = notifySignInRequired("update inventory")
    override fun adjustInventoryQuantity(item: InventoryItem, delta: Long) = notifySignInRequired("adjust stock balances")
    override fun deleteInventoryItem(itemId: String) = notifySignInRequired("delete inventory SKUs")
    override fun recordStockPurchase(supplierId: String, supplierName: String, inventoryItemId: String, itemName: String, quantityText: String, unitCostText: String, paymentMethod: String, paymentStatus: String, purchaseDate: String, notes: String, logAsExpense: Boolean) = notifySignInRequired("record supplier restock orders")
    override fun markPurchasePaid(purchaseId: String) = notifySignInRequired("update supplier payables")
    override fun deletePurchaseOrder(purchaseId: String) = notifySignInRequired("delete purchase orders")
    override fun addCustomer(name: String, phone: String, email: String, location: String, notes: String) = notifySignInRequired("save customers")
    override fun updateCustomer(id: String, name: String, phone: String, email: String, location: String, notes: String) = notifySignInRequired("update customers")
    override fun deleteCustomer(customerId: String) = notifySignInRequired("delete customers")
    override fun addSupplier(name: String, contactPerson: String, phone: String, email: String, location: String, suppliedProductsSummary: String, notes: String) = notifySignInRequired("save suppliers")
    override fun updateSupplier(id: String, name: String, contactPerson: String, phone: String, email: String, location: String, suppliedProductsSummary: String, notes: String) = notifySignInRequired("update suppliers")
    override fun deleteSupplier(supplierId: String) = notifySignInRequired("delete suppliers")
    override fun addCommunicationLog(customerName: String, channel: String, subject: String, receivedDate: String, responseDate: String, status: String, responseMinutesText: String, notes: String, category: String) = notifySignInRequired("log communication enquiries")
    override fun updateCommunicationLog(commId: String, customerName: String, channel: String, subject: String, receivedDate: String, responseDate: String, status: String, responseMinutesText: String, notes: String, category: String) = notifySignInRequired("update communication logs")
    override fun updateCommunicationStatus(commId: String, newStatus: String) = notifySignInRequired("update enquiry status")
    override fun deleteCommunicationLog(commId: String) = notifySignInRequired("delete communication logs")
    override fun generateWorkspaceBackupJson(onResult: (Result<Pair<String, BackupSummary>>) -> Unit) {
        notifySignInRequired("back up live Cloud Firestore records")
        onResult(Result.failure(IllegalStateException("Sign in with Google to export a live Cloud Firestore backup.")))
    }
    override fun reportBackupExportOutcome(success: Boolean, message: String) = Unit
    override fun inspectBackupFileJson(rawJson: String, sourceLabel: String) = notifySignInRequired("restore backups into Cloud Firestore")
    override fun confirmRestoreBackup(replaceExisting: Boolean) = Unit
    override fun dismissRestoreConfirmation() = Unit
    override fun saveBusinessProfile(ownerName: String, businessName: String, businessType: String, locationNakuru: String, businessSize: String, phone: String, demoModeEnabled: Boolean) = notifySignInRequired("save your SME business profile")
}

fun DashboardViewModel.asWorkspaceHost(): ProjectXWorkspaceHost = object : ProjectXWorkspaceHost {
    override val dashboardSummary = this@asWorkspaceHost.dashboardSummary
    override val analyticsReport = this@asWorkspaceHost.analyticsReport
    override val prompt4Analytics = this@asWorkspaceHost.prompt4Analytics
    override val forecastHorizonMonths = this@asWorkspaceHost.forecastHorizonMonths
    override val roomOverviewState = this@asWorkspaceHost.roomOverviewState
    override val salesOverviewAiState = this@asWorkspaceHost.salesOverviewAiState
    override val profileState = this@asWorkspaceHost.profileState
    override val transactionsState = this@asWorkspaceHost.transactionsState
    override val inventoryState = this@asWorkspaceHost.inventoryState
    override val campaignsState = this@asWorkspaceHost.campaignsState
    override val communicationsState = this@asWorkspaceHost.communicationsState
    override val customersState = this@asWorkspaceHost.customersState
    override val suppliersState = this@asWorkspaceHost.suppliersState
    override val purchasesState = this@asWorkspaceHost.purchasesState
    override val stockMovementsState = this@asWorkspaceHost.stockMovementsState
    override val aiState = this@asWorkspaceHost.aiState
    override val growthAnalysisState = this@asWorkspaceHost.growthAnalysisState
    override val aiMessages = this@asWorkspaceHost.aiMessages
    override val actionMessage = this@asWorkspaceHost.actionMessage
    override val isSaving = this@asWorkspaceHost.isSaving
    override val hasPendingSyncWrites = this@asWorkspaceHost.hasPendingSyncWrites
    override val backupRestoreState = this@asWorkspaceHost.backupRestoreState
    override val latestPosReceipt = this@asWorkspaceHost.latestPosReceipt

    override fun clearActionMessage() = this@asWorkspaceHost.clearActionMessage()
    override fun dismissLatestPosReceipt() = this@asWorkspaceHost.dismissLatestPosReceipt()
    override fun setAnalyticsDateFilter(filter: AnalyticsDateFilter) = this@asWorkspaceHost.setAnalyticsDateFilter(filter)
    override fun setCustomDateRange(startIso: String, endIso: String) = this@asWorkspaceHost.setCustomDateRange(startIso, endIso)
    override fun toggleDemoMode(enabled: Boolean) = this@asWorkspaceHost.toggleDemoMode(enabled)
    override fun refreshRoomOverviewCache() = this@asWorkspaceHost.refreshRoomOverviewCache()
    override fun generateOverviewSalesSummary() = this@asWorkspaceHost.generateOverviewSalesSummary()
    override fun clearOverviewSalesSummary() = this@asWorkspaceHost.clearOverviewSalesSummary()
    override fun addCampaign(title: String, channel: String, campaignDate: String, spendText: String, reachText: String, engagementText: String, inquiriesText: String, status: String, notes: String) =
        this@asWorkspaceHost.addCampaign(title, channel, campaignDate, spendText, reachText, engagementText, inquiriesText, status, notes)
    override fun updateCampaign(campaignId: String, title: String, channel: String, campaignDate: String, spendText: String, reachText: String, engagementText: String, inquiriesText: String, status: String, notes: String) =
        this@asWorkspaceHost.updateCampaign(campaignId, title, channel, campaignDate, spendText, reachText, engagementText, inquiriesText, status, notes)
    override fun deleteCampaign(campaignId: String) = this@asWorkspaceHost.deleteCampaign(campaignId)
    override fun addTransaction(type: String, title: String, category: String, amountText: String, paymentMethod: String, transactionRef: String, paymentStatus: String, transactionDate: String, notes: String) =
        this@asWorkspaceHost.addTransaction(type, title, category, amountText, paymentMethod, transactionRef, paymentStatus, transactionDate, notes)
    override fun updateTransaction(transactionId: String, type: String, title: String, category: String, amountText: String, paymentMethod: String, transactionRef: String, paymentStatus: String, transactionDate: String, notes: String) =
        this@asWorkspaceHost.updateTransaction(transactionId, type, title, category, amountText, paymentMethod, transactionRef, paymentStatus, transactionDate, notes)
    override fun recordPosSale(title: String, category: String, paymentMethod: String, transactionRef: String, paymentStatus: String, transactionDate: String, notes: String, customerId: String, customerName: String, inventoryItemId: String, quantityText: String, unitPriceText: String, discountText: String, manualAmountText: String) =
        this@asWorkspaceHost.recordPosSale(title, category, paymentMethod, transactionRef, paymentStatus, transactionDate, notes, customerId, customerName, inventoryItemId, quantityText, unitPriceText, discountText, manualAmountText)
    override fun recordPosCartSale(cartItems: List<PosCartLineItem>, defaultCategory: String, paymentMethod: String, transactionRef: String, paymentStatus: String, transactionDate: String, notes: String, customerId: String, customerName: String, cartDiscountText: String) =
        this@asWorkspaceHost.recordPosCartSale(cartItems, defaultCategory, paymentMethod, transactionRef, paymentStatus, transactionDate, notes, customerId, customerName, cartDiscountText)
    override fun cancelSaleTransaction(tx: BusinessTransaction) = this@asWorkspaceHost.cancelSaleTransaction(tx)
    override fun deleteTransaction(id: String) = this@asWorkspaceHost.deleteTransaction(id)
    override fun addInventoryItem(name: String, sku: String, category: String, quantityText: String, reorderLevelText: String, unitPriceText: String, unitCostText: String, supplierId: String, supplierName: String) =
        this@asWorkspaceHost.addInventoryItem(name, sku, category, quantityText, reorderLevelText, unitPriceText, unitCostText, supplierId, supplierName)
    override fun updateInventoryItem(itemId: String, name: String, sku: String, category: String, quantityText: String, reorderLevelText: String, unitPriceText: String, unitCostText: String, supplierId: String, supplierName: String) =
        this@asWorkspaceHost.updateInventoryItem(itemId, name, sku, category, quantityText, reorderLevelText, unitPriceText, unitCostText, supplierId, supplierName)
    override fun adjustInventoryQuantity(item: InventoryItem, delta: Long) = this@asWorkspaceHost.adjustInventoryQuantity(item, delta)
    override fun deleteInventoryItem(itemId: String) = this@asWorkspaceHost.deleteInventoryItem(itemId)
    override fun recordStockPurchase(supplierId: String, supplierName: String, inventoryItemId: String, itemName: String, quantityText: String, unitCostText: String, paymentMethod: String, paymentStatus: String, purchaseDate: String, notes: String, logAsExpense: Boolean) =
        this@asWorkspaceHost.recordStockPurchase(supplierId, supplierName, inventoryItemId, itemName, quantityText, unitCostText, paymentMethod, paymentStatus, purchaseDate, notes, logAsExpense)
    override fun markPurchasePaid(purchaseId: String) = this@asWorkspaceHost.markPurchasePaid(purchaseId)
    override fun deletePurchaseOrder(purchaseId: String) = this@asWorkspaceHost.deletePurchaseOrder(purchaseId)
    override fun addCustomer(name: String, phone: String, email: String, location: String, notes: String) =
        this@asWorkspaceHost.addCustomer(name, phone, email, location, notes)
    override fun updateCustomer(id: String, name: String, phone: String, email: String, location: String, notes: String) =
        this@asWorkspaceHost.updateCustomer(id, name, phone, email, location, notes)
    override fun deleteCustomer(customerId: String) = this@asWorkspaceHost.deleteCustomer(customerId)
    override fun addSupplier(name: String, contactPerson: String, phone: String, email: String, location: String, suppliedProductsSummary: String, notes: String) =
        this@asWorkspaceHost.addSupplier(name, contactPerson, phone, email, location, suppliedProductsSummary, notes)
    override fun updateSupplier(id: String, name: String, contactPerson: String, phone: String, email: String, location: String, suppliedProductsSummary: String, notes: String) =
        this@asWorkspaceHost.updateSupplier(id, name, contactPerson, phone, email, location, suppliedProductsSummary, notes)
    override fun deleteSupplier(supplierId: String) = this@asWorkspaceHost.deleteSupplier(supplierId)
    override fun addCommunicationLog(customerName: String, channel: String, subject: String, receivedDate: String, responseDate: String, status: String, responseMinutesText: String, notes: String, category: String) =
        this@asWorkspaceHost.addCommunicationLog(customerName, channel, subject, receivedDate, responseDate, status, responseMinutesText, notes, category)
    override fun updateCommunicationLog(commId: String, customerName: String, channel: String, subject: String, receivedDate: String, responseDate: String, status: String, responseMinutesText: String, notes: String, category: String) =
        this@asWorkspaceHost.updateCommunicationLog(commId, customerName, channel, subject, receivedDate, responseDate, status, responseMinutesText, notes, category)
    override fun updateCommunicationStatus(commId: String, newStatus: String) = this@asWorkspaceHost.updateCommunicationStatus(commId, newStatus)
    override fun deleteCommunicationLog(commId: String) = this@asWorkspaceHost.deleteCommunicationLog(commId)
    override fun setForecastHorizonMonths(months: Int) = this@asWorkspaceHost.setForecastHorizonMonths(months)
    override fun generateWorkspaceBackupJson(onResult: (Result<Pair<String, BackupSummary>>) -> Unit) =
        this@asWorkspaceHost.generateWorkspaceBackupJson(onResult)
    override fun reportBackupExportOutcome(success: Boolean, message: String) = this@asWorkspaceHost.reportBackupExportOutcome(success, message)
    override fun inspectBackupFileJson(rawJson: String, sourceLabel: String) = this@asWorkspaceHost.inspectBackupFileJson(rawJson, sourceLabel)
    override fun confirmRestoreBackup(replaceExisting: Boolean) = this@asWorkspaceHost.confirmRestoreBackup(replaceExisting)
    override fun dismissRestoreConfirmation() = this@asWorkspaceHost.dismissRestoreConfirmation()
    override fun saveBusinessProfile(ownerName: String, businessName: String, businessType: String, locationNakuru: String, businessSize: String, phone: String, demoModeEnabled: Boolean) =
        this@asWorkspaceHost.saveBusinessProfile(ownerName, businessName, businessType, locationNakuru, businessSize, phone, demoModeEnabled)
    override fun askAiAssistant(prompt: String) = this@asWorkspaceHost.askAiAssistant(prompt)
    override fun analyzeTransactionHistory() = this@asWorkspaceHost.analyzeTransactionHistory()
}
