package com.example

import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.service.buildAnalysisPrompt
import com.example.data.service.parseGrowthAnalysisJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionGrowthAnalysisServiceTest {

    @Test
    fun parseGrowthAnalysisJson_parsesResearchVariableFields() {
        val sampleJson = """
            {
              "healthStatus": "STRONG",
              "executiveSummary": "Sales of KES 30,750 exceed expenses of KES 9,000, producing a positive net cashflow of KES 21,750.",
              "digitalMarketingAdvice": "Reallocate budget toward WhatsApp Status which drove highest inquiry conversion.",
              "digitalPaymentsAdvice": "Maintain M-Pesa Till and Bank transfer settlement records for clean reconciliation.",
              "recordKeepingAdvice": "Restock Fresh Fri 1L before weekend peak to prevent lost counter sales.",
              "onlineCommunicationAdvice": "Keep WhatsApp response times under 10 minutes to preserve inquiry conversion.",
              "cashflowInsight": "M-Pesa accounts for 60% of sales volume, keeping digital reconciliation clean.",
              "topCostDriver": "Stock Restock represents KES 7,200 (80% of total expenses).",
              "correlationCaveat": "These observed patterns reflect association in recorded enterprise metrics and do not establish causal proof.",
              "growthOpportunities": [
                {
                  "title": "Restock Fast-Moving Cooking Oil",
                  "variableFocus": "Record Keeping",
                  "actionStep": "Reorder 24 units of Fresh Fri 1L before weekend peak.",
                  "expectedImpact": "Prevents KES 8,160 in lost weekend revenue.",
                  "priority": "HIGH"
                },
                {
                  "title": "Follow Up Pending WhatsApp Leads",
                  "variableFocus": "Online Communication",
                  "actionStep": "Reply within 10 minutes with bundle pricing in KES.",
                  "expectedImpact": "Improves inquiry-to-order conversion rate.",
                  "priority": "MEDIUM"
                }
              ],
              "nakuruLocalTip": "Align bulk restocks with Tuesday wholesale market deliveries in Nakuru CBD to cut transport fees."
            }
        """.trimIndent()

        val report = parseGrowthAnalysisJson(
            jsonString = sampleJson,
            isDemoData = false,
            businessName = "Rift Valley Mart",
            analyzedTransactionCount = 4,
            analyzedCampaignCount = 2,
            analyzedCommunicationCount = 3,
            totalSalesKes = 30750.0,
            totalExpensesKes = 9000.0,
            digitalPaymentSharePct = 75
        )

        assertEquals("STRONG", report.healthStatus)
        assertEquals("Rift Valley Mart", report.businessName)
        assertEquals(21750.0, report.netCashflowKes, 0.01)
        assertEquals(75, report.digitalPaymentSharePct)
        assertEquals(2, report.growthOpportunities.size)
        assertEquals("Record Keeping", report.growthOpportunities.first().variableFocus)
        assertTrue(report.correlationCaveat.contains("causal", ignoreCase = true))
        assertTrue(report.nakuruLocalTip.contains("Nakuru"))
    }

    @Test
    fun buildAnalysisPrompt_includesAllFourIndependentVariablesAndLowStock() {
        val profile = BusinessProfile(
            userId = "u1",
            ownerName = "Serah",
            businessName = "Menengai Grocers",
            businessType = "Retail Shop",
            locationNakuru = "Milimani, Nakuru"
        )
        val txs = listOf(
            BusinessTransaction(
                id = "t1",
                type = BusinessTransaction.TYPE_SALE,
                title = "Morning Sales",
                category = "Retail Sales",
                amountKes = 5000.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA
            ),
            BusinessTransaction(
                id = "t2",
                type = BusinessTransaction.TYPE_EXPENSE,
                title = "Transport",
                category = "Transport",
                amountKes = 1200.0,
                paymentMethod = BusinessTransaction.METHOD_CASH
            )
        )
        val inv = listOf(
            InventoryItem(
                id = "i1",
                name = "Sugar 2kg",
                quantity = 3,
                reorderLevel = 8,
                unitPriceKes = 320.0
            )
        )
        val campaigns = listOf(
            DigitalMarketingCampaign(
                id = "c1",
                title = "Weekend Promo",
                channel = DigitalMarketingCampaign.CHANNEL_WHATSAPP,
                spendKes = 500.0,
                reachCount = 400L,
                engagementCount = 60L,
                inquiriesGenerated = 15L
            )
        )
        val comms = listOf(
            OnlineCommunicationLog(
                id = "m1",
                customerName = "Wanjiku",
                channel = OnlineCommunicationLog.CHANNEL_WHATSAPP,
                subject = "Order inquiry",
                status = OnlineCommunicationLog.STATUS_CONVERTED,
                responseMinutes = 5L
            )
        )

        val prompt = buildAnalysisPrompt(profile, txs, inv, campaigns, comms, usingDemo = false)
        assertTrue(prompt.contains("LIVE CLOUD FIRESTORE WORKSPACE"))
        assertTrue(prompt.contains("IV1: DIGITAL MARKETING"))
        assertTrue(prompt.contains("IV2: DIGITAL PAYMENT SYSTEMS"))
        assertTrue(prompt.contains("IV3: COMPUTERIZED RECORD KEEPING"))
        assertTrue(prompt.contains("IV4: ONLINE COMMUNICATION"))
        assertTrue(prompt.contains("KES 5,000"))
        assertTrue(prompt.contains("LOW STOCK: Sugar 2kg"))
        assertTrue(prompt.contains("Weekend Promo"))
        assertTrue(prompt.contains("Wanjiku"))
    }
}
