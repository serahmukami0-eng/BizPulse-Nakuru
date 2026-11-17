package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.analytics.AnalyticsFilterState
import com.example.data.analytics.Prompt4ForecastingEngine
import com.example.data.analytics.SmeAnalyticsEngine
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.InventoryItem
import com.example.ui.components.ProjectXLoadingScreen
import com.example.ui.theme.ProjectXTheme
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProjectXUiAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun businessProfile_defaultsToProductionLiveMode() {
        val defaultProfile = BusinessProfile()
        assertFalse(defaultProfile.demoModeEnabled)
    }

    @Test
    fun projectXLoadingScreen_rendersBrandedShellAndStatus() {
        composeRule.setContent {
            ProjectXTheme {
                ProjectXLoadingScreen(statusMessage = "Syncing live Cloud Firestore workspace...")
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("projectx_loading_screen").assertIsDisplayed()
        composeRule.onNodeWithText("PROJECT X – SME MANAGEMENT SYSTEM").assertIsDisplayed()
        composeRule.onNodeWithText("Syncing live Cloud Firestore workspace...").assertIsDisplayed()
    }

    @Test
    fun loginScreen_rendersProjectXBrandingAndGoogleSignIn() {
        composeRule.setContent {
            ProjectXTheme {
                LoginScreen(
                    onBack = {},
                    onGoToRegister = {},
                    onAuthSuccess = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PROJECT X – SME MANAGEMENT SYSTEM").assertIsDisplayed()
        composeRule.onNodeWithText("Nakuru SME Cloud Portal").assertIsDisplayed()
        composeRule.onNodeWithTag("login_google_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reportsScreen_rendersAboutAndSupportDiagnosticsCards() {
        val analytics = SmeAnalyticsEngine.compute(
            isDemoMode = false,
            filterState = AnalyticsFilterState(),
            allTransactions = emptyList(),
            inventory = emptyList(),
            allCampaigns = emptyList(),
            allCommunications = emptyList()
        )
        val prompt4 = Prompt4ForecastingEngine.compute(
            isDemoMode = false,
            allTransactions = emptyList(),
            inventory = emptyList(),
            customers = emptyList(),
            horizonMonths = 6
        )
        val summary = DashboardSummary(
            isDemoMode = false,
            hasAnyLiveRecords = true,
            businessName = "Rift Valley Supplies",
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
        composeRule.setContent {
            ProjectXTheme {
                ReportsScreen(
                    currentUserDisplayName = "Owner",
                    currentUserEmail = "owner@example.com",
                    profileState = UiState.Success(BusinessProfile(businessName = "Rift Valley Supplies")),
                    summary = summary,
                    transactionsState = UiState.Success(emptyList()),
                    campaignsState = UiState.Success(emptyList()),
                    communicationsState = UiState.Success(emptyList()),
                    inventoryState = UiState.Success(emptyList()),
                    customersState = UiState.Success(emptyList()),
                    purchasesState = UiState.Success(emptyList()),
                    analyticsReport = analytics,
                    prompt4Analytics = prompt4,
                    isSaving = false,
                    onBackToDashboard = {},
                    onOpenAllModules = {},
                    onToggleDemoMode = {},
                    onSaveProfile = { _, _, _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reports_lazy_list").performScrollToNode(hasTestTag("about_project_x_card"))
        composeRule.onNodeWithTag("about_project_x_card").assertIsDisplayed()
        composeRule.onNodeWithTag("reports_lazy_list").performScrollToNode(hasTestTag("support_contact_card"))
        composeRule.onNodeWithTag("support_contact_card").assertIsDisplayed()
        composeRule.onNodeWithTag("reports_lazy_list").performScrollToNode(hasTestTag("copy_support_summary_button"))
        composeRule.onNodeWithTag("copy_support_summary_button").assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_rendersResearchSummaryAndRoutesButtons() {
        var started = 0
        var register = 0
        var signIn = 0
        composeRule.setContent {
            ProjectXTheme {
                WelcomeScreen(
                    onGetStarted = { started++ },
                    onCreateAccount = { register++ },
                    onSignIn = { signIn++ }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Digital Technology Adoption & SME Performance (KES)").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_get_started_button").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, started)
    }

    @Test
    fun posSaleDialog_rejectsInsufficientStockAndExcessiveDiscount() {
        var confirmed = 0
        composeRule.setContent {
            ProjectXTheme {
                PosSaleDialog(
                    inventory = listOf(
                        InventoryItem(
                            id = "sku1",
                            name = "Sugar 2kg",
                            sku = "SUG-02",
                            category = "Dry Goods",
                            quantity = 4L,
                            reorderLevel = 2L,
                            unitPriceKes = 300.0,
                            unitCostKes = 240.0
                        )
                    ),
                    customers = emptyList(),
                    businessLocation = "Kenyatta Ave, Nakuru",
                    onDismiss = {},
                    onConfirmPosSale = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> confirmed++ }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pos_qty_input").performScrollTo().performTextReplacement("10")
        composeRule.onNodeWithTag("pos_checkout_confirm_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Insufficient stock: only 4 in stock.").performScrollTo().assertIsDisplayed()
        assertEquals(0, confirmed)

        composeRule.onNodeWithTag("pos_qty_input").performScrollTo().performTextReplacement("1")
        composeRule.onNodeWithTag("pos_discount_input").performScrollTo().performTextReplacement("500")
        composeRule.onNodeWithTag("pos_checkout_confirm_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Net payable total must exceed 0 KES.").performScrollTo().assertIsDisplayed()
        assertEquals(0, confirmed)
    }

    @Test
    fun recordTransactionDialog_locksLinkedPosSaleTypeAndDisallowsManualCancel() {
        val linkedSale = BusinessTransaction(
            id = "sale_pos_1",
            type = BusinessTransaction.TYPE_SALE,
            title = "Sugar 2kg Counter Sale",
            category = "Retail Sales",
            amountKes = 600.0,
            paymentMethod = BusinessTransaction.METHOD_MPESA,
            paymentStatus = BusinessTransaction.STATUS_COMPLETED,
            transactionDate = "2026-11-16",
            inventoryItemId = "sku1",
            quantitySold = 2L,
            receiptNumber = "PX-123456"
        )
        composeRule.setContent {
            ProjectXTheme {
                RecordTransactionDialog(
                    type = BusinessTransaction.TYPE_SALE,
                    initialTransaction = linkedSale,
                    allowTypeToggle = true,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("EXPENSE").assertIsNotEnabled()
        composeRule.onNodeWithText("Use 'Void Sale' on the transaction card to cancel a sale and restore stock.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("CANCELLED").assertDoesNotExist()
    }
}
