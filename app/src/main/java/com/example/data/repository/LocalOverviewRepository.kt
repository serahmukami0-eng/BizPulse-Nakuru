package com.example.data.repository

import com.example.data.local.BusinessOverviewDao
import com.example.data.local.LocalKpiSnapshotEntity
import com.example.data.local.LocalTransactionEntity
import com.example.data.local.RoomCategorySummary
import com.example.data.local.RoomPaymentSummary
import com.example.data.model.BusinessTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.roundToInt

data class RoomBusinessOverviewState(
    val isCacheReady: Boolean = false,
    val isDemoData: Boolean = true,
    val businessName: String = "My Nakuru SME",
    val businessType: String = "Retail Shop",
    val locationNakuru: String = "Nakuru Town, Kenya",
    val lastSyncedEpochMs: Long? = null,
    val totalSalesKes: Double = 0.0,
    val totalExpensesKes: Double = 0.0,
    val otherTransactionsKes: Double = 0.0,
    val estimatedNetProfitKes: Double = 0.0,
    val salesCount: Int = 0,
    val expenseCount: Int = 0,
    val averageSaleTicketKes: Double = 0.0,
    val expenseToSalesRatioPct: Int? = null,
    val netMarginPct: Int? = null,
    val digitalPaymentsKes: Double = 0.0,
    val salesByCategory: List<RoomCategorySummary> = emptyList(),
    val expensesByCategory: List<RoomCategorySummary> = emptyList(),
    val paymentBreakdown: List<RoomPaymentSummary> = emptyList(),
    val recentCachedTransactions: List<LocalTransactionEntity> = emptyList()
)

class LocalOverviewRepository(
    private val dao: BusinessOverviewDao
) {
    fun observeOverviewKpis(userId: String): Flow<RoomBusinessOverviewState> {
        val coreTotalsFlow = combine(
            dao.observeTotalSalesKes(userId),
            dao.observeTotalExpensesKes(userId),
            dao.observeOtherTransactionsKes(userId),
            dao.observeSalesCount(userId),
            dao.observeExpenseCount(userId)
        ) { salesKes, expensesKes, otherKes, salesCount, expenseCount ->
            CoreTotals(salesKes, expensesKes, otherKes, salesCount, expenseCount)
        }

        val breakdownFlow = combine(
            dao.observeDigitalPaymentsKes(userId),
            dao.observeCategoryBreakdown(userId, BusinessTransaction.TYPE_SALE),
            dao.observeCategoryBreakdown(userId, BusinessTransaction.TYPE_EXPENSE),
            dao.observePaymentBreakdown(userId),
            dao.observeRecentTransactions(userId, limit = 5)
        ) { digitalKes, salesCategories, expenseCategories, paymentBreakdown, recentTx ->
            BreakdownBundle(digitalKes, salesCategories, expenseCategories, paymentBreakdown, recentTx)
        }

        return combine(
            coreTotalsFlow,
            breakdownFlow,
            dao.observeKpiSnapshot(userId)
        ) { core, breakdown, snapshot ->
            val netProfit = core.salesKes - core.expensesKes
            val avgSaleTicket = if (core.salesCount > 0) core.salesKes / core.salesCount else 0.0
            val expenseRatioPct = if (core.salesKes > 0.0) {
                ((core.expensesKes / core.salesKes) * 100.0).roundToInt()
            } else {
                null
            }
            val netMarginPct = if (core.salesKes > 0.0) {
                ((netProfit / core.salesKes) * 100.0).roundToInt()
            } else {
                null
            }

            RoomBusinessOverviewState(
                isCacheReady = snapshot != null,
                isDemoData = snapshot?.isDemoData ?: true,
                businessName = snapshot?.businessName?.ifBlank { "My Nakuru SME" } ?: "My Nakuru SME",
                businessType = snapshot?.businessType?.ifBlank { "Retail Shop" } ?: "Retail Shop",
                locationNakuru = snapshot?.locationNakuru?.ifBlank { "Nakuru Town, Kenya" } ?: "Nakuru Town, Kenya",
                lastSyncedEpochMs = snapshot?.lastSyncedEpochMs,
                totalSalesKes = core.salesKes,
                totalExpensesKes = core.expensesKes,
                otherTransactionsKes = core.otherKes,
                estimatedNetProfitKes = netProfit,
                salesCount = core.salesCount,
                expenseCount = core.expenseCount,
                averageSaleTicketKes = avgSaleTicket,
                expenseToSalesRatioPct = expenseRatioPct,
                netMarginPct = netMarginPct,
                digitalPaymentsKes = breakdown.digitalKes,
                salesByCategory = breakdown.salesCategories,
                expensesByCategory = breakdown.expenseCategories,
                paymentBreakdown = breakdown.paymentBreakdown,
                recentCachedTransactions = breakdown.recentTx
            )
        }
    }

    suspend fun syncOverviewCache(
        userId: String,
        businessName: String,
        businessType: String,
        locationNakuru: String,
        isDemoData: Boolean,
        transactions: List<BusinessTransaction>,
        syncTimestampMs: Long = System.currentTimeMillis()
    ) {
        val entities = transactions.mapIndexed { idx, tx ->
            val resolvedId = tx.id.ifBlank { "${userId}_local_$idx" }
            LocalTransactionEntity(
                id = "${userId}__$resolvedId",
                userId = userId,
                type = tx.type,
                title = tx.title,
                category = tx.category.ifBlank { "Uncategorized" },
                amountKes = tx.amountKes,
                paymentMethod = tx.paymentMethod,
                paymentStatus = tx.paymentStatus,
                transactionDate = tx.effectiveDate(),
                isDemoData = isDemoData,
                syncedAtEpochMs = syncTimestampMs
            )
        }

        val activeTransactions = transactions.filter { it.isActiveLedgerEntry() }
        val sales = activeTransactions.filter { it.type == BusinessTransaction.TYPE_SALE }
        val expenses = activeTransactions.filter { it.type == BusinessTransaction.TYPE_EXPENSE }
        val others = activeTransactions.filter { it.type == BusinessTransaction.TYPE_OTHER }
        val digital = activeTransactions.filter { it.isDigitalPayment }
        val salesSum = sales.sumOf { it.amountKes }
        val expenseSum = expenses.sumOf { it.amountKes }

        val snapshot = LocalKpiSnapshotEntity(
            userId = userId,
            businessName = businessName,
            businessType = businessType,
            locationNakuru = locationNakuru,
            totalSalesKes = salesSum,
            totalExpensesKes = expenseSum,
            otherTransactionsKes = others.sumOf { it.amountKes },
            netProfitKes = salesSum - expenseSum,
            salesCount = sales.size,
            expenseCount = expenses.size,
            digitalPaymentsKes = digital.sumOf { it.amountKes },
            digitalTxnCount = digital.size,
            isDemoData = isDemoData,
            lastSyncedEpochMs = syncTimestampMs
        )

        dao.replaceUserOverviewCache(
            userId = userId,
            transactions = entities,
            snapshot = snapshot
        )
    }

    suspend fun clearUserCache(userId: String) {
        dao.deleteTransactionsForUser(userId)
        dao.deleteSnapshotForUser(userId)
    }

    private data class CoreTotals(
        val salesKes: Double,
        val expensesKes: Double,
        val otherKes: Double,
        val salesCount: Int,
        val expenseCount: Int
    )

    private data class BreakdownBundle(
        val digitalKes: Double,
        val salesCategories: List<RoomCategorySummary>,
        val expenseCategories: List<RoomCategorySummary>,
        val paymentBreakdown: List<RoomPaymentSummary>,
        val recentTx: List<LocalTransactionEntity>
    )
}
