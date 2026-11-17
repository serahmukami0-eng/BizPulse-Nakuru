package com.example.data.analytics

import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.DemoSampleData
import com.example.data.model.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Prompt4ForecastingEngineTest {

    @Test
    fun compute_withSufficientHistory_generatesOlsProjectionsAndAllSevenBars() {
        val bundle = Prompt4ForecastingEngine.compute(
            isDemoMode = true,
            allTransactions = DemoSampleData.sampleTransactions,
            inventory = DemoSampleData.sampleInventory,
            customers = DemoSampleData.sampleCustomers,
            horizonMonths = 6,
            referenceTodayIso = "2026-11-17"
        )

        assertTrue(bundle.projection.hasSufficientHistory)
        assertNull(bundle.projection.warningMessage)
        assertTrue(bundle.monthlyRows.size >= 3)
        assertEquals(6, bundle.projection.rows.count { it.isProjection })
        assertEquals(bundle.monthlyRows.size, bundle.monthlySalesBars.size)
        assertEquals(bundle.projection.rows.size, bundle.actualVsProjectedBars.size)
        assertEquals(bundle.monthlyRows.size, bundle.revenueVsExpenseBars.size)
        assertEquals(bundle.monthlyRows.size, bundle.profitabilityBars.size)
        assertTrue(bundle.customerGrowthBars.isNotEmpty())
        assertEquals(5, bundle.paymentMethodBars.size)
        assertTrue(bundle.inventoryLevelBars.isNotEmpty())
        assertTrue(bundle.hasCostDataForGrossProfit)
        assertNotNull(bundle.totalGrossProfitKes)
        assertTrue(bundle.financialMetrics.topExpenseDrivers.isNotEmpty())
    }

    @Test
    fun compute_fewerThanThreeMonths_flagsInsufficientHistoryWarning() {
        val sparseTx = listOf(
            BusinessTransaction(
                id = "t1",
                type = BusinessTransaction.TYPE_SALE,
                title = "Sale Oct",
                amountKes = 10000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionDate = "2026-10-12"
            ),
            BusinessTransaction(
                id = "t2",
                type = BusinessTransaction.TYPE_SALE,
                title = "Sale Nov",
                amountKes = 12000.0,
                paymentMethod = BusinessTransaction.METHOD_CASH,
                transactionDate = "2026-11-12"
            )
        )
        val bundle = Prompt4ForecastingEngine.compute(
            isDemoMode = false,
            allTransactions = sparseTx,
            inventory = emptyList(),
            customers = emptyList(),
            horizonMonths = 3,
            referenceTodayIso = "2026-11-17"
        )

        assertFalse(bundle.projection.hasSufficientHistory)
        assertEquals(2, bundle.projection.historicalMonthsCount)
        assertNotNull(bundle.projection.warningMessage)
        assertTrue(bundle.projection.warningMessage!!.contains("< 3 months"))
        assertFalse(bundle.hasCostDataForGrossProfit)
        assertNull(bundle.totalGrossProfitKes)
        assertEquals(3, bundle.projection.rows.count { it.isProjection })
        assertEquals(20.0, bundle.latestMoMSalesGrowthPct ?: 0.0, 0.01)
    }

    @Test
    fun compute_excludesCancelledSalesFromTotalsAndBars() {
        val txs = listOf(
            BusinessTransaction(
                id = "s1",
                type = BusinessTransaction.TYPE_SALE,
                title = "Valid Sale",
                amountKes = 5000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-10"
            ),
            BusinessTransaction(
                id = "s2",
                type = BusinessTransaction.TYPE_SALE,
                title = "Voided Sale",
                amountKes = 9000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_CANCELLED,
                transactionDate = "2026-11-10"
            )
        )
        val bundle = Prompt4ForecastingEngine.compute(
            isDemoMode = false,
            allTransactions = txs,
            inventory = listOf(InventoryItem(id = "i1", name = "Tea", quantity = 3L, reorderLevel = 5L, unitPriceKes = 150.0, unitCostKes = 100.0)),
            customers = listOf(CustomerRecord(id = "c1", name = "Asha", purchaseCount = 2L, totalSpentKes = 5000.0, createdDate = "2026-11-10")),
            horizonMonths = 3,
            referenceTodayIso = "2026-11-17"
        )

        assertEquals(1, bundle.monthlyRows.size)
        assertEquals(5000.0, bundle.monthlyRows.first().salesKes, 0.01)
        assertEquals(1, bundle.repeatCustomerCount)
        assertEquals(100.0, bundle.customerRepeatRatePct ?: 0.0, 0.01)
        assertEquals("LOW", bundle.inventoryLevelBars.first().badge)
    }

    @Test
    fun compute_excludesFailedTransactionsAndHandlesZeroExpensesSafely() {
        val txs = listOf(
            BusinessTransaction(
                id = "s_ok",
                type = BusinessTransaction.TYPE_SALE,
                title = "Confirmed Counter Sale",
                amountKes = 6000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-11"
            ),
            BusinessTransaction(
                id = "s_failed",
                type = BusinessTransaction.TYPE_SALE,
                title = "Failed M-Pesa Timeout",
                amountKes = 15000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_FAILED,
                transactionDate = "2026-11-11"
            )
        )
        val bundle = Prompt4ForecastingEngine.compute(
            isDemoMode = false,
            allTransactions = txs,
            inventory = emptyList(),
            customers = emptyList(),
            horizonMonths = 3,
            referenceTodayIso = "2026-11-17"
        )

        assertEquals(6000.0, bundle.monthlyRows.first().salesKes, 0.01)
        assertEquals(0.0, bundle.monthlyRows.first().expensesKes, 0.01)
        assertEquals(6000.0, bundle.monthlyRows.first().netProfitKes, 0.01)
        assertNull(bundle.financialMetrics.breakEvenCoveragePct)
    }
}
