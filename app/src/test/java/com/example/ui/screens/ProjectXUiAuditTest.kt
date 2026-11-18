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
import com.example.data.backup.ProjectXBackupManager
import com.example.data.backup.WorkspaceBackupPayload
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.InventoryItem
import com.example.data.model.PosCartLineItem
import com.example.ui.components.OfflineSyncStatusBanner
import com.example.ui.components.ProjectXLoadingScreen
import com.example.ui.theme.ProjectXTheme
import com.example.ui.viewmodel.BackupRestoreUiState
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        composeRule.onNodeWithTag("reports_lazy_list").performScrollToNode(hasTestTag("export_spss_research_csv_button"))
        composeRule.onNodeWithTag("export_spss_research_csv_button").assertIsDisplayed()
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

    @Test
    fun offlineSyncStatusBanner_displaysOfflineAndPendingSyncMessages() {
        composeRule.setContent {
            ProjectXTheme {
                OfflineSyncStatusBanner(
                    isOnline = false,
                    hasPendingWrites = true
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("offline_sync_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Offline Mode • Pending Cloud Sync").assertIsDisplayed()
    }

    @Test
    fun reportsScreen_rendersBackupCardAndConfirmRestoreWarningDialog() {
        val sampleBackupJson = ProjectXBackupManager.serializeWorkspaceBackup(
            WorkspaceBackupPayload(
                profile = BusinessProfile(
                    ownerName = "Serah",
                    businessName = "Rift Valley Supplies",
                    businessType = "Retail Shop",
                    locationNakuru = "Nakuru Town"
                ),
                transactions = listOf(
                    BusinessTransaction(
                        id = "tx1",
                        type = BusinessTransaction.TYPE_SALE,
                        title = "Flour",
                        category = "Retail Sales",
                        amountKes = 1200.0,
                        paymentMethod = BusinessTransaction.METHOD_MPESA,
                        transactionDate = "2026-11-16"
                    )
                ),
                inventory = emptyList(),
                customers = emptyList(),
                suppliers = emptyList(),
                purchases = emptyList(),
                stockMovements = emptyList(),
                campaigns = emptyList(),
                communications = emptyList()
            )
        )
        val validated = ProjectXBackupManager.parseAndValidateBackup(sampleBackupJson).getOrThrow()
        var chosenReplace: Boolean? = null

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
            totalSalesKes = 1200.0,
            totalExpensesKes = 0.0,
            otherTransactionsKes = 0.0,
            netBalanceKes = 1200.0,
            lowStockCount = 0,
            salesTransactionCount = 1,
            otherTransactionCount = 0,
            transactionCount = 1,
            inventoryCount = 0,
            digitalPaymentCount = 1,
            digitalPaymentAdoptionPct = 100,
            totalDigitalPaymentsKes = 1200.0,
            mpesaSalesKes = 1200.0,
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
                    transactionsState = UiState.Success(validated.payload.transactions),
                    campaignsState = UiState.Success(emptyList()),
                    communicationsState = UiState.Success(emptyList()),
                    inventoryState = UiState.Success(emptyList()),
                    customersState = UiState.Success(emptyList()),
                    purchasesState = UiState.Success(emptyList()),
                    analyticsReport = analytics,
                    prompt4Analytics = prompt4,
                    isSaving = false,
                    backupRestoreState = BackupRestoreUiState.ReadyToConfirmRestore(
                        backup = validated,
                        sourceLabel = "backup.json"
                    ),
                    onConfirmRestoreBackup = { chosenReplace = it },
                    onBackToDashboard = {},
                    onOpenAllModules = {},
                    onToggleDemoMode = {},
                    onSaveProfile = { _, _, _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reports_lazy_list").performScrollToNode(hasTestTag("backup_restore_card"))
        composeRule.onNodeWithTag("backup_restore_card").assertIsDisplayed()
        composeRule.onNodeWithTag("export_backup_button").assertIsDisplayed()
        composeRule.onNodeWithTag("restore_backup_button").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_restore_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm Database Restore").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_restore_replace_button").performClick()
        assertEquals(true, chosenReplace)
    }

    @Test
    fun dashboardAnalytics_recalculatesLiveTotalsAndMonthlyChartsFromSavedRecords() {
        val before = Prompt4ForecastingEngine.compute(
            isDemoMode = false,
            allTransactions = emptyList(),
            inventory = emptyList(),
            customers = emptyList(),
            horizonMonths = 6
        )
        assertEquals(0.0, before.monthlyRows.sumOf { it.salesKes }, 0.01)

        val savedSale = BusinessTransaction(
            id = "sale_saved_1",
            type = BusinessTransaction.TYPE_SALE,
            title = "Rice 5kg Bag",
            category = "Retail Sales",
            amountKes = 3500.0,
            paymentMethod = BusinessTransaction.METHOD_MPESA,
            paymentStatus = BusinessTransaction.STATUS_COMPLETED,
            transactionDate = "2026-11-16",
            costOfGoodsSoldKes = 2400.0
        )
        val after = Prompt4ForecastingEngine.compute(
            isDemoMode = false,
            allTransactions = listOf(savedSale),
            inventory = emptyList(),
            customers = emptyList(),
            horizonMonths = 6
        )
        assertEquals(3500.0, after.monthlyRows.sumOf { it.salesKes }, 0.01)
        assertEquals(1100.0, after.totalGrossProfitKes ?: 0.0, 0.01)
    }

    @Test
    fun posSaleDialog_multiItemCart_addsItemsAndSubmitsViaOnConfirmPosCart() {
        var submittedCart: List<PosCartLineItem> = emptyList()
        var submittedCartDiscount = ""
        composeRule.setContent {
            ProjectXTheme {
                PosSaleDialog(
                    inventory = listOf(
                        InventoryItem(
                            id = "sku1",
                            name = "Sugar 2kg",
                            sku = "SUG-02",
                            category = "Dry Goods",
                            quantity = 8L,
                            reorderLevel = 2L,
                            unitPriceKes = 300.0,
                            unitCostKes = 240.0
                        ),
                        InventoryItem(
                            id = "sku2",
                            name = "Cooking Oil 1L",
                            sku = "OIL-01",
                            category = "Pantry",
                            quantity = 5L,
                            reorderLevel = 2L,
                            unitPriceKes = 350.0,
                            unitCostKes = 280.0
                        )
                    ),
                    customers = emptyList(),
                    businessLocation = "Kenyatta Ave, Nakuru",
                    onDismiss = {},
                    onConfirmPosSale = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onConfirmPosCart = { cartItems, _, _, _, _, _, _, _, _, cartDisc ->
                        submittedCart = cartItems
                        submittedCartDiscount = cartDisc
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // Add 2x Sugar 2kg to cart
        composeRule.onNodeWithTag("pos_qty_input").performScrollTo().performTextReplacement("2")
        composeRule.onNodeWithTag("pos_add_to_cart_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Switch to Cooking Oil 1L and add 1x to cart
        composeRule.onNodeWithText("Cooking Oil 1L (5 left)").performScrollTo().performClick()
        composeRule.onNodeWithTag("pos_add_to_cart_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pos_cart_list").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("POS Cart (2 item(s))").assertIsDisplayed()
        composeRule.onNodeWithTag("pos_cart_discount_input").performScrollTo().performTextReplacement("50")
        composeRule.onNodeWithTag("pos_checkout_confirm_button").performClick()
        composeRule.waitForIdle()

        assertEquals(2, submittedCart.size)
        assertEquals("sku1", submittedCart[0].inventoryItemId)
        assertEquals(2L, submittedCart[0].quantity)
        assertEquals("sku2", submittedCart[1].inventoryItemId)
        assertEquals(1L, submittedCart[1].quantity)
        assertEquals("50", submittedCartDiscount)
    }

    @Test
    fun receiptPreviewDialog_multiItem_rendersItemizedReceiptAndShareButtons() {
        val items = listOf(
            BusinessTransaction(
                id = "tx_c1",
                type = BusinessTransaction.TYPE_SALE,
                title = "Sugar 2kg x2",
                inventoryItemName = "Sugar 2kg",
                quantitySold = 2L,
                unitPriceKes = 300.0,
                amountKes = 600.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionRef = "TI99RECEIPT",
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-18",
                customerName = "Grace Wambui",
                receiptNumber = "PX-777888"
            ),
            BusinessTransaction(
                id = "tx_c2",
                type = BusinessTransaction.TYPE_SALE,
                title = "Cooking Oil 1L x1",
                inventoryItemName = "Cooking Oil 1L",
                quantitySold = 1L,
                unitPriceKes = 350.0,
                amountKes = 350.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionRef = "TI99RECEIPT",
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-18",
                customerName = "Grace Wambui",
                receiptNumber = "PX-777888"
            )
        )
        composeRule.setContent {
            ProjectXTheme {
                ReceiptPreviewDialog(
                    transaction = items.first(),
                    receiptItems = items,
                    businessName = "Nakuru Fresh Mart",
                    businessLocation = "Biashara Street, Nakuru",
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Digital POS Receipt (2 items)").assertIsDisplayed()
        composeRule.onNodeWithText("Sugar 2kg").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cooking Oil 1L").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_share_text_button").assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_share_pdf_button").assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_print_button").assertIsDisplayed()

        val shareText = formatReceiptShareText(
            items = items,
            businessName = "Nakuru Fresh Mart",
            location = "Biashara Street, Nakuru",
            receiptNo = "PX-777888"
        )
        assertTrue(shareText.contains("PX-777888"))
        assertTrue(shareText.contains("Sugar 2kg"))
        assertTrue(shareText.contains("Cooking Oil 1L"))
        assertTrue(shareText.contains("950"))
    }
}
