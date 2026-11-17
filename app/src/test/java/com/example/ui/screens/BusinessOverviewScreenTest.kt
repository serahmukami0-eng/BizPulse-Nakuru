package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.RoomCategorySummary
import com.example.data.model.BusinessTransaction
import com.example.data.repository.RoomBusinessOverviewState
import com.example.ui.theme.ProjectXTheme
import com.example.ui.viewmodel.SalesOverviewAiState
import com.example.ui.viewmodel.buildOverviewSalesContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BusinessOverviewScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun analyzeSalesButton_triggersCallbackAndRendersSummaryCard() {
        var clickedCount = 0
        val overviewState = RoomBusinessOverviewState(
            isCacheReady = true,
            isDemoData = false,
            businessName = "Menengai Fresh Shop",
            totalSalesKes = 24000.0,
            totalExpensesKes = 9000.0,
            estimatedNetProfitKes = 15000.0,
            salesCount = 3,
            expenseCount = 2,
            averageSaleTicketKes = 8000.0,
            expenseToSalesRatioPct = 38,
            netMarginPct = 62,
            digitalPaymentsKes = 20000.0,
            salesByCategory = listOf(RoomCategorySummary("Retail Sales", 24000.0, 3))
        )
        val aiSuccess = SalesOverviewAiState.Success(
            summaryText = "Sales reached KES 24,000 across 3 transactions with a healthy 62% estimated margin.",
            isDemoData = false,
            analyzedSalesCount = 3,
            totalSalesKes = 24000.0
        )

        composeRule.setContent {
            ProjectXTheme {
                MainBusinessOverviewScreen(
                    overviewState = overviewState,
                    onRefreshRoomCache = {},
                    onOpenQuickSale = {},
                    onOpenQuickExpense = {},
                    onOpenModule = {},
                    salesAiState = aiSuccess,
                    onAnalyzeSalesWithAi = { clickedCount++ },
                    onClearSalesAiSummary = {}
                )
            }
        }

        composeRule.onNodeWithTag("overview_analyze_sales_ai_button").assertIsDisplayed().performClick()
        assertEquals(1, clickedCount)

        composeRule.onNodeWithTag("overview_sales_ai_card").assertIsDisplayed()
        composeRule.onNodeWithText("AI Sales Performance Summary").assertIsDisplayed()
        composeRule.onNodeWithText("Sales reached KES 24,000 across 3 transactions with a healthy 62% estimated margin.")
            .assertIsDisplayed()
    }

    @Test
    fun buildOverviewSalesContext_includesSalesCategoriesAndPaymentBreakdown() {
        val context = buildOverviewSalesContext(
            businessName = "Nakuru Electronics",
            businessType = "Retail Shop",
            businessSize = "Small (6–20 staff)",
            locationNakuru = "Kenyatta Avenue, Nakuru",
            isDemoMode = false,
            activeTransactions = listOf(
                BusinessTransaction(
                    id = "s1",
                    type = BusinessTransaction.TYPE_SALE,
                    title = "POS Terminal Till",
                    category = "Retail Sales",
                    amountKes = 14500.0,
                    paymentMethod = BusinessTransaction.METHOD_MPESA,
                    transactionDate = "2026-11-14"
                ),
                BusinessTransaction(
                    id = "e1",
                    type = BusinessTransaction.TYPE_EXPENSE,
                    title = "Stock Delivery",
                    category = "Stock Restock",
                    amountKes = 4500.0,
                    paymentMethod = BusinessTransaction.METHOD_BANK,
                    transactionDate = "2026-11-14"
                )
            ),
            expenseToSalesRatioPct = 31,
            netMarginPct = 69
        )

        assertTrue(context.contains("LIVE CLOUD FIRESTORE & ROOM SQLITE CACHE"))
        assertTrue(context.contains("KES 14,500"))
        assertTrue(context.contains("Retail Sales"))
        assertTrue(context.contains("M-PESA"))
        assertTrue(context.contains("Expense-to-Sales Ratio: 31%"))
    }
}
