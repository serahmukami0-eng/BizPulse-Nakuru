package com.example.data.analytics

import com.example.data.model.AnalyticsDateFilter
import com.example.data.model.BusinessTransaction
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmeAnalyticsEngineTest {

    @Test
    fun compute_calculatesNetProfitDigitalAdoptionAndPeriodGrowth() {
        val today = "2026-11-14"
        val transactions = listOf(
            // Current 7d period (2026-11-08 to 2026-11-14)
            BusinessTransaction(
                id = "t1",
                type = BusinessTransaction.TYPE_SALE,
                title = "M-Pesa Sales",
                category = "Retail Sales",
                amountKes = 12000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionRef = "TI01",
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-14"
            ),
            BusinessTransaction(
                id = "t2",
                type = BusinessTransaction.TYPE_SALE,
                title = "Cash Sales",
                category = "Retail Sales",
                amountKes = 3000.0,
                paymentMethod = BusinessTransaction.METHOD_CASH,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-12"
            ),
            BusinessTransaction(
                id = "t3",
                type = BusinessTransaction.TYPE_EXPENSE,
                title = "Restock",
                category = "Stock Restock",
                amountKes = 5000.0,
                paymentMethod = BusinessTransaction.METHOD_BANK,
                transactionRef = "EFT01",
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-11"
            ),
            // Prior 7d period (2026-11-01 to 2026-11-07)
            BusinessTransaction(
                id = "t_prev",
                type = BusinessTransaction.TYPE_SALE,
                title = "Last Week Sales",
                category = "Retail Sales",
                amountKes = 10000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionRef = "TI00",
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-05"
            )
        )

        val campaigns = listOf(
            DigitalMarketingCampaign(
                id = "c1",
                title = "WhatsApp Promo",
                channel = DigitalMarketingCampaign.CHANNEL_WHATSAPP,
                campaignDate = "2026-11-14",
                spendKes = 1000.0,
                reachCount = 500L,
                engagementCount = 100L,
                inquiriesGenerated = 20L
            )
        )

        val comms = listOf(
            OnlineCommunicationLog(
                id = "m1",
                customerName = "Wanjiku",
                channel = OnlineCommunicationLog.CHANNEL_WHATSAPP,
                subject = "Order inquiry",
                receivedDate = "2026-11-14",
                responseDate = "2026-11-14",
                status = OnlineCommunicationLog.STATUS_CONVERTED,
                responseMinutes = 5L
            ),
            OnlineCommunicationLog(
                id = "m2",
                customerName = "Kamau",
                channel = OnlineCommunicationLog.CHANNEL_EMAIL,
                subject = "Quote inquiry",
                receivedDate = "2026-11-13",
                status = OnlineCommunicationLog.STATUS_PENDING,
                responseMinutes = 30L
            ),
            OnlineCommunicationLog(
                id = "m_prev",
                customerName = "Amina",
                channel = OnlineCommunicationLog.CHANNEL_WHATSAPP,
                subject = "Prior inquiry",
                receivedDate = "2026-11-04",
                responseDate = "2026-11-04",
                status = OnlineCommunicationLog.STATUS_RESPONDED,
                responseMinutes = 10L
            )
        )

        val report = SmeAnalyticsEngine.compute(
            isDemoMode = false,
            filterState = AnalyticsFilterState(filter = AnalyticsDateFilter.LAST_7_DAYS),
            allTransactions = transactions,
            inventory = listOf(InventoryItem(id = "i1", name = "Flour", quantity = 20, reorderLevel = 5, unitPriceKes = 200.0)),
            allCampaigns = campaigns,
            allCommunications = comms,
            referenceTodayIso = today
        )

        // Recorded sales = 15,000; Expenses = 5,000; Net = 10,000
        assertEquals(15000.0, report.recordKeeping.recordedSalesKes, 0.01)
        assertEquals(5000.0, report.recordKeeping.recordedExpensesKes, 0.01)
        assertEquals(10000.0, report.recordKeeping.estimatedNetProfitKes, 0.01)

        // Sales growth: 15,000 vs 10,000 = +50%
        assertNotNull(report.performance.salesGrowthPct)
        assertEquals(50.0, report.performance.salesGrowthPct!!, 0.01)

        // Customer growth: 2 unique vs 1 unique = +100%
        assertEquals(2, report.performance.currentCustomerCount)
        assertNotNull(report.performance.customerGrowthPct)
        assertEquals(100.0, report.performance.customerGrowthPct!!, 0.01)

        // Digital payments: 12,000 M-Pesa + 5,000 Bank = 17,000; Digital sales % of 15,000 = 80%
        assertEquals(17000.0, report.payments.totalDigitalPaymentsKes, 0.01)
        assertEquals(80.0, report.payments.digitalAsPercentOfSales!!, 0.01)

        // Marketing cost per enquiry = 1000 / 20 = 50 KES
        assertEquals(50.0, report.marketing.costPerInquiryKes!!, 0.01)

        // Comms response rate = 1 / 2 = 50%
        assertEquals(50.0, report.communication.responseRatePct!!, 0.01)
    }

    @Test
    fun compute_doesNotInventMissingMetricsOrCorrelationsWithSparseData() {
        val report = SmeAnalyticsEngine.compute(
            isDemoMode = false,
            filterState = AnalyticsFilterState(
                filter = AnalyticsDateFilter.CUSTOM,
                customStartIso = "2026-11-10",
                customEndIso = "2026-11-14"
            ),
            allTransactions = listOf(
                BusinessTransaction(
                    id = "t1",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "Single Sale",
                    category = "Retail Sales",
                    amountKes = 4000.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionDate = "2026-11-14"
                )
            ),
            inventory = emptyList(),
            allCampaigns = emptyList(),
            allCommunications = emptyList(),
            referenceTodayIso = "2026-11-14"
        )

        assertNull(report.performance.salesGrowthPct)
        assertNull(report.performance.customerGrowthPct)
        assertNull(report.marketing.costPerInquiryKes)
        assertNull(report.communication.responseRatePct)
        assertTrue(report.researchComparisons.all { it.correlationR == null })
        assertTrue(report.dataGapsAndLimitations.isNotEmpty())
    }

    @Test
    fun pearsonCorrelation_computesExactCorrelationWhenSufficientData() {
        val r = SmeAnalyticsEngine.computePearsonCorrelation(
            xs = listOf(100.0, 200.0, 300.0, 400.0),
            ys = listOf(1000.0, 2000.0, 3000.0, 4000.0)
        )
        assertNotNull(r)
        assertEquals(1.0, r!!, 0.001)

        assertNull(SmeAnalyticsEngine.computePearsonCorrelation(listOf(1.0, 2.0), listOf(3.0, 4.0)))
        assertNull(SmeAnalyticsEngine.computePearsonCorrelation(listOf(5.0, 5.0, 5.0), listOf(1.0, 2.0, 3.0)))
    }

    @Test
    fun compute_excludesFailedAndCancelledTransactionsFromMetrics() {
        val transactions = listOf(
            BusinessTransaction(
                id = "ok_1",
                type = BusinessTransaction.TYPE_SALE,
                title = "Confirmed Sale",
                category = "Retail Sales",
                amountKes = 8000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-14"
            ),
            BusinessTransaction(
                id = "bad_failed",
                type = BusinessTransaction.TYPE_SALE,
                title = "Failed Card Sale",
                category = "Retail Sales",
                amountKes = 12000.0,
                paymentMethod = BusinessTransaction.METHOD_CARD,
                paymentStatus = BusinessTransaction.STATUS_FAILED,
                transactionDate = "2026-11-14"
            ),
            BusinessTransaction(
                id = "bad_cancelled",
                type = BusinessTransaction.TYPE_EXPENSE,
                title = "Voided Expense",
                category = "Stock Restock",
                amountKes = 3500.0,
                paymentMethod = BusinessTransaction.METHOD_BANK,
                paymentStatus = BusinessTransaction.STATUS_CANCELLED,
                transactionDate = "2026-11-14"
            )
        )
        val report = SmeAnalyticsEngine.compute(
            isDemoMode = false,
            filterState = AnalyticsFilterState(filter = AnalyticsDateFilter.LAST_7_DAYS),
            allTransactions = transactions,
            inventory = emptyList(),
            allCampaigns = emptyList(),
            allCommunications = emptyList(),
            referenceTodayIso = "2026-11-14"
        )

        assertEquals(8000.0, report.recordKeeping.recordedSalesKes, 0.01)
        assertEquals(0.0, report.recordKeeping.recordedExpensesKes, 0.01)
        assertEquals(8000.0, report.payments.totalDigitalPaymentsKes, 0.01)
        assertEquals(1, report.filteredTransactions.size)
    }
}
