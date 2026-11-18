package com.example.data.export

import com.example.data.analytics.AnalyticsFilterState
import com.example.data.analytics.Prompt4ForecastingEngine
import com.example.data.analytics.SmeAnalyticsEngine
import com.example.data.model.DemoSampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ReportExportManagerTest {

    @Test
    fun generateReport_all12ReportTypes_produceValidCsvContent() {
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

            if (reportType == BusinessReportType.RESEARCH_DATASET_SPSS) {
                val lines = csvText.lines().filter { it.isNotBlank() }
                assertTrue(
                    "SPSS CSV header row missing (was: ${lines.firstOrNull()})",
                    lines.first().startsWith("Record_Scope,Period_Label,SME_Code,Nakuru_Location,Data_Mode")
                )
                assertFalse("SPSS CSV must not contain # comment lines", lines.any { it.startsWith("#") })
                assertFalse("SPSS CSV must anonymize real business name", csvText.contains("Nakuru Fresh Mart"))
                assertTrue("SPSS CSV must contain SME- code", csvText.contains("SME-"))
                assertTrue("SPSS CSV must contain OVERALL_WINDOW", csvText.contains("OVERALL_WINDOW"))
                assertTrue("SPSS CSV must contain MONTHLY_PANEL", csvText.contains("MONTHLY_PANEL"))
                assertTrue("SPSS CSV must contain IV/DV headers", lines.first().contains("DV_Sales_Revenue_KES"))
            } else {
                assertTrue("${reportType.name} CSV header missing", csvText.contains("Nakuru Fresh Mart"))
                assertTrue("${reportType.name} CSV DEMO tag missing", csvText.contains("DEMO SAMPLE DATA"))
            }
        }

        assertEquals(12, BusinessReportType.entries.size)
    }
}
