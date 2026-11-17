package com.example.data.export

import com.example.data.analytics.AnalyticsFilterState
import com.example.data.analytics.Prompt4ForecastingEngine
import com.example.data.analytics.SmeAnalyticsEngine
import com.example.data.model.DemoSampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ReportExportManagerTest {

    @Test
    fun generateReport_all11ReportTypes_produceValidCsvContent() {
        val txs = DemoSampleData.sampleTransactions
        val inv = DemoSampleData.sampleInventory
        val cust = DemoSampleData.sampleCustomers
        val pos = DemoSampleData.samplePurchases
        val camps = DemoSampleData.sampleCampaigns
        val comms = DemoSampleData.sampleCommunications

        val analytics = SmeAnalyticsEngine.compute(
            isDemoMode = true,
            filterState = AnalyticsFilterState(),
            allTransactions = txs,
            inventory = inv,
            allCampaigns = camps,
            allCommunications = comms
        )
        val prompt4 = Prompt4ForecastingEngine.compute(
            isDemoMode = true,
            allTransactions = txs,
            inventory = inv,
            customers = cust,
            horizonMonths = 6
        )

        BusinessReportType.entries.forEach { reportType ->
            val report = ReportExportManager.generateReport(
                type = reportType,
                businessName = "Nakuru Fresh Mart",
                location = "Biashara Street, Nakuru",
                isDemoData = true,
                transactions = txs,
                inventory = inv,
                customers = cust,
                purchases = pos,
                campaigns = camps,
                communications = comms,
                analytics = analytics,
                prompt4 = prompt4,
                periodLabel = analytics.activeWindow.label
            )

            assertTrue("${reportType.name} should have columns", report.columns.isNotEmpty())
            assertTrue("${reportType.name} should have summaryPairs", report.summaryPairs.isNotEmpty())

            val csvOut = ByteArrayOutputStream()
            ReportExportManager.writeCsvToStream(report, csvOut)
            val csvText = csvOut.toString(Charsets.UTF_8.name())
            assertTrue("${reportType.name} CSV header missing", csvText.contains("Nakuru Fresh Mart"))
            assertTrue("${reportType.name} CSV DEMO tag missing", csvText.contains("DEMO SAMPLE DATA"))
        }

        assertEquals(11, BusinessReportType.entries.size)
    }
}
