package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ProjectXLocalDatabase
import com.example.data.model.BusinessTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalOverviewRepositoryTest {

    private lateinit var database: ProjectXLocalDatabase
    private lateinit var repository: LocalOverviewRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ProjectXLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalOverviewRepository(database.businessOverviewDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun syncOverviewCache_computesSalesExpensesProfitAndBreakdownsViaRoomFlows() = runBlocking {
        val transactions = listOf(
            BusinessTransaction(
                id = "t1",
                type = BusinessTransaction.TYPE_SALE,
                title = "Morning Till Sales",
                category = "Retail Sales",
                amountKes = 15000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-14"
            ),
            BusinessTransaction(
                id = "t2",
                type = BusinessTransaction.TYPE_SALE,
                title = "Walk-in Cash Sales",
                category = "Wholesale",
                amountKes = 5000.0,
                paymentMethod = BusinessTransaction.METHOD_CASH,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-14"
            ),
            BusinessTransaction(
                id = "t3",
                type = BusinessTransaction.TYPE_EXPENSE,
                title = "Flour & Cooking Oil",
                category = "Stock Restock",
                amountKes = 6000.0,
                paymentMethod = BusinessTransaction.METHOD_BANK,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-14"
            ),
            BusinessTransaction(
                id = "t4",
                type = BusinessTransaction.TYPE_EXPENSE,
                title = "Shop Power Bill",
                category = "Electricity & Water",
                amountKes = 2000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-13"
            )
        )

        repository.syncOverviewCache(
            userId = "user_nakuru_1",
            businessName = "Biashara Smart Shop",
            businessType = "Retail Shop",
            locationNakuru = "Biashara Street, Nakuru",
            isDemoData = false,
            transactions = transactions,
            syncTimestampMs = 1763100000000L
        )

        val state = repository.observeOverviewKpis("user_nakuru_1")
            .first { it.isCacheReady }

        assertFalse(state.isDemoData)
        assertEquals(20000.0, state.totalSalesKes, 0.01)
        assertEquals(8000.0, state.totalExpensesKes, 0.01)
        assertEquals(12000.0, state.estimatedNetProfitKes, 0.01)
        assertEquals(2, state.salesCount)
        assertEquals(2, state.expenseCount)
        assertEquals(10000.0, state.averageSaleTicketKes, 0.01)
        assertEquals(40, state.expenseToSalesRatioPct)
        assertEquals(60, state.netMarginPct)
        // Digital payments exclude CASH (15,000 + 6,000 + 2,000)
        assertEquals(23000.0, state.digitalPaymentsKes, 0.01)
        assertEquals("Retail Sales", state.salesByCategory.first().category)
        assertEquals("Stock Restock", state.expensesByCategory.first().category)
        assertEquals(3, state.paymentBreakdown.size)
    }

    @Test
    fun syncOverviewCache_isolatesUsersAndClearsOldEntries() = runBlocking {
        repository.syncOverviewCache(
            userId = "user_a",
            businessName = "Shop A",
            businessType = "Retail Shop",
            locationNakuru = "Nakuru",
            isDemoData = false,
            transactions = listOf(
                BusinessTransaction(
                    id = "a1",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Sale A",
                    category = "Retail Sales",
                    amountKes = 10000.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA
                )
            )
        )
        repository.syncOverviewCache(
            userId = "user_b",
            businessName = "Shop B",
            businessType = "Salon & Barber",
            locationNakuru = "Milimani",
            isDemoData = false,
            transactions = listOf(
                BusinessTransaction(
                    id = "b1",
                    type = BusinessTransaction.TYPE_EXPENSE,
                    title = "Expense B",
                    category = "Shop Rent",
                    amountKes = 4500.0,
                    paymentMethod = BusinessTransaction.METHOD_BANK
                )
            )
        )

        val stateA = repository.observeOverviewKpis("user_a").first { it.isCacheReady }
        val stateB = repository.observeOverviewKpis("user_b").first { it.isCacheReady }

        assertEquals(10000.0, stateA.totalSalesKes, 0.01)
        assertEquals(0.0, stateA.totalExpensesKes, 0.01)
        assertEquals(0.0, stateB.totalSalesKes, 0.01)
        assertEquals(4500.0, stateB.totalExpensesKes, 0.01)

        repository.clearUserCache("user_a")
        val clearedA = repository.observeOverviewKpis("user_a").first { !it.isCacheReady }
        assertTrue(clearedA.recentCachedTransactions.isEmpty())
        assertEquals(0.0, clearedA.totalSalesKes, 0.01)
    }

    @Test
    fun syncOverviewCache_excludesCancelledAndFailedTransactionsFromRoomKpis() = runBlocking {
        val transactions = listOf(
            BusinessTransaction(
                id = "valid_sale",
                type = BusinessTransaction.TYPE_SALE,
                title = "Valid M-Pesa Sale",
                category = "Retail Sales",
                amountKes = 10000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-16"
            ),
            BusinessTransaction(
                id = "void_sale",
                type = BusinessTransaction.TYPE_SALE,
                title = "Voided Sale",
                category = "Retail Sales",
                amountKes = 9000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_CANCELLED,
                transactionDate = "2026-11-16"
            ),
            BusinessTransaction(
                id = "failed_sale",
                type = BusinessTransaction.TYPE_SALE,
                title = "Failed STK Push",
                category = "Retail Sales",
                amountKes = 7500.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_FAILED,
                transactionDate = "2026-11-16"
            ),
            BusinessTransaction(
                id = "failed_expense",
                type = BusinessTransaction.TYPE_EXPENSE,
                title = "Reverted Bank Transfer",
                category = "Shop Rent",
                amountKes = 4000.0,
                paymentMethod = BusinessTransaction.METHOD_BANK,
                paymentStatus = BusinessTransaction.STATUS_FAILED,
                transactionDate = "2026-11-16"
            )
        )

        repository.syncOverviewCache(
            userId = "user_void_filter",
            businessName = "Nakuru Filter Shop",
            businessType = "Retail Shop",
            locationNakuru = "Nakuru CBD",
            isDemoData = false,
            transactions = transactions,
            syncTimestampMs = 1763200000000L
        )

        val state = repository.observeOverviewKpis("user_void_filter").first { it.isCacheReady }
        assertEquals(10000.0, state.totalSalesKes, 0.01)
        assertEquals(0.0, state.totalExpensesKes, 0.01)
        assertEquals(10000.0, state.estimatedNetProfitKes, 0.01)
        assertEquals(1, state.salesCount)
        assertEquals(0, state.expenseCount)
        assertEquals(10000.0, state.digitalPaymentsKes, 0.01)
        assertEquals(1, state.recentCachedTransactions.size)
        assertEquals("user_void_filter__valid_sale", state.recentCachedTransactions.first().id)
    }
}
