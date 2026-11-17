package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BusinessTransaction
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.OnlineCommunicationLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BusinessRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProfile_authenticatedOwner_createsAndUpdatesProfileWithSizeAndCustomLocation() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = BusinessRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveBusinessProfile(
                ownerName = "Serah Mukami",
                businessName = "Nakuru Fresh Mart",
                businessType = "Retail Shop",
                locationNakuru = "Biashara Street, Nakuru",
                businessSize = "Small (6–20 staff)",
                phone = "+254712345678",
                demoModeEnabled = true
            )
        }
        assertTrue(saveResult.isSuccess)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveBusinessProfile(
                ownerName = "Serah Mukami",
                businessName = "Nakuru Fresh Mart Ltd",
                businessType = "Mini-Supermarket",
                locationNakuru = "Njoro Junction, Nakuru County",
                businessSize = "Medium (21–50 staff)",
                phone = "+254712345678",
                demoModeEnabled = false
            )
        }
        assertTrue(updateResult.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBusinessProfile(uid).first { it?.businessName == "Nakuru Fresh Mart Ltd" }
        }
        assertEquals("Medium (21–50 staff)", profile?.businessSize)
        assertEquals("Njoro Junction, Nakuru County", profile?.locationNakuru)
        assertEquals("KES", profile?.currency)
    }

    @Test
    fun crudAllResearchCollections_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = BusinessRepository(firestore, auth)

        val txId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTransaction(
                type = BusinessTransaction.TYPE_OTHER,
                title = "County Business Permit",
                category = "Tax / Permit",
                amountKes = 4500.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionRef = "TI92NK410X",
                paymentStatus = BusinessTransaction.STATUS_PENDING,
                transactionDate = "2026-11-14",
                notes = "Annual renewal"
            ).getOrThrow()
        }

        val updateTxRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateTransaction(
                transactionId = txId,
                type = BusinessTransaction.TYPE_OTHER,
                title = "County Business Permit",
                category = "Tax / Permit",
                amountKes = 4500.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionRef = "TI92NK410X",
                paymentStatus = BusinessTransaction.STATUS_COMPLETED,
                transactionDate = "2026-11-14",
                notes = "Confirmed settlement"
            )
        }
        assertTrue(updateTxRes.isSuccess)

        val transactions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(uid).first { list ->
                list.any { it.id == txId && it.paymentStatus == BusinessTransaction.STATUS_COMPLETED }
            }
        }
        assertTrue(transactions.any { it.id == txId && it.transactionRef == "TI92NK410X" })

        val invId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addInventoryItem(
                name = "Sugar 2kg",
                sku = "SUG-02",
                category = "Dry Goods",
                quantity = 20L,
                reorderLevel = 5L,
                unitPriceKes = 310.0
            ).getOrThrow()
        }
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateInventoryItem(invId, "Sugar 2kg Premium", "SUG-02P", "Dry Goods", 25L, 6L, 325.0)
            }.isSuccess
        )

        val inventory = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventory(uid).first { list -> list.any { it.id == invId && it.quantity == 25L } }
        }
        assertTrue(inventory.any { it.id == invId && it.name == "Sugar 2kg Premium" })

        val campId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addCampaign(
                title = "Nakuru CBD WhatsApp Promo",
                channel = DigitalMarketingCampaign.CHANNEL_WHATSAPP,
                campaignDate = "2026-11-14",
                spendKes = 900.0,
                reachCount = 540L,
                engagementCount = 82L,
                inquiriesGenerated = 18L,
                status = DigitalMarketingCampaign.STATUS_ACTIVE,
                notes = "Weekend campaign"
            ).getOrThrow()
        }
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateCampaign(
                    campaignId = campId,
                    title = "Nakuru CBD WhatsApp Promo v2",
                    channel = DigitalMarketingCampaign.CHANNEL_WHATSAPP,
                    campaignDate = "2026-11-14",
                    spendKes = 1100.0,
                    reachCount = 700L,
                    engagementCount = 105L,
                    inquiriesGenerated = 24L,
                    status = DigitalMarketingCampaign.STATUS_COMPLETED,
                    notes = "Completed"
                )
            }.isSuccess
        )
        val campaigns = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCampaigns(uid).first { list -> list.any { it.id == campId && it.reachCount == 700L } }
        }
        assertTrue(campaigns.any { it.id == campId && it.campaignDate == "2026-11-14" })

        val commId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addCommunicationLog(
                customerName = "Wanjiku",
                channel = OnlineCommunicationLog.CHANNEL_OTHER,
                subject = "Delivery quote to Milimani",
                receivedDate = "2026-11-14",
                responseDate = "2026-11-14",
                status = OnlineCommunicationLog.STATUS_CONVERTED,
                responseMinutes = 6L,
                notes = "Paid via M-Pesa"
            ).getOrThrow()
        }
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateCommunicationLog(
                    commId = commId,
                    customerName = "Wanjiku N.",
                    channel = OnlineCommunicationLog.CHANNEL_WHATSAPP,
                    subject = "Delivery quote to Milimani",
                    receivedDate = "2026-11-14",
                    responseDate = "2026-11-14",
                    status = OnlineCommunicationLog.STATUS_CONVERTED,
                    responseMinutes = 5L,
                    notes = "Delivered"
                )
            }.isSuccess
        )
        val comms = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCommunications(uid).first { list -> list.any { it.id == commId && it.responseMinutes == 5L } }
        }
        assertTrue(comms.any { it.id == commId && it.customerName == "Wanjiku N." })

        // Prompt 4: Customer, Supplier, Purchase Restock, POS Sale + Cancellation
        val supId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addSupplier(
                name = "Rift Valley Millers",
                contactPerson = "Peter Karanja",
                phone = "+254722110440",
                email = "orders@rvml.co.ke",
                location = "Industrial Area, Nakuru",
                suppliedProductsSummary = "Maize Flour & Sugar",
                notes = "Primary mill"
            ).getOrThrow()
        }
        val suppliers = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSuppliers(uid).first { list -> list.any { it.id == supId } }
        }
        assertTrue(suppliers.any { it.id == supId && it.name == "Rift Valley Millers" })

        val custId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addCustomer(
                name = "Grace Wambui",
                phone = "+254712990112",
                email = "grace@nakuru.co.ke",
                location = "Milimani, Nakuru",
                notes = "Repeat buyer"
            ).getOrThrow()
        }

        val poId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordStockPurchase(
                supplierId = supId,
                supplierName = "Rift Valley Millers",
                inventoryItemId = invId,
                itemName = "Sugar 2kg Premium",
                quantity = 10L,
                unitCostKes = 240.0,
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = com.example.data.model.PurchaseOrderRecord.STATUS_PAID,
                purchaseDate = "2026-11-15",
                notes = "Weekly restock"
            ).getOrThrow()
        }
        val invAfterRestock = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventory(uid).first { list -> list.any { it.id == invId && it.quantity == 35L } }
        }
        assertEquals(240.0, invAfterRestock.first { it.id == invId }.unitCostKes, 0.01)

        val saleId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordSaleWithInventory(
                title = "Counter Sugar POS",
                category = "Retail Sales",
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionRef = "TI99POS01",
                transactionDate = "2026-11-15",
                customerId = custId,
                customerName = "Grace Wambui",
                inventoryItemId = invId,
                quantitySold = 5L,
                unitPriceKes = 325.0,
                discountKes = 25.0
            ).getOrThrow()
        }
        val invAfterSale = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventory(uid).first { list -> list.any { it.id == invId && it.quantity == 30L } }
        }
        assertEquals(30L, invAfterSale.first { it.id == invId }.quantity)

        val custAfterSale = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomers(uid).first { list -> list.any { it.id == custId && it.purchaseCount == 1L } }
        }.first { it.id == custId }
        assertEquals(1600.0, custAfterSale.totalSpentKes, 0.01)

        val recordedSale = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(uid).first { list -> list.any { it.id == saleId } }
        }.first { it.id == saleId }
        assertTrue(recordedSale.receiptNumber.startsWith("PX-"))

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.cancelSaleTransaction(recordedSale) }.isSuccess)
        val invAfterCancel = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventory(uid).first { list -> list.any { it.id == invId && it.quantity == 35L } }
        }
        assertEquals(35L, invAfterCancel.first { it.id == invId }.quantity)

        val custAfterCancel = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomers(uid).first { list -> list.any { it.id == custId && it.purchaseCount == 0L } }
        }.first { it.id == custId }
        assertEquals(0.0, custAfterCancel.totalSpentKes, 0.01)

        val movements = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStockMovements(uid).first { list -> list.size >= 3 }
        }
        assertTrue(movements.any { it.referenceId == poId })

        // Atomic manual stock adjustment creates TYPE_MANUAL_ADJUSTMENT audit trail & blocks negative stock
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.adjustInventoryQuantityWithAudit(invId, -5L, "Damaged cartons")
            }.isSuccess
        )
        val invAfterManualAdjust = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventory(uid).first { list -> list.any { it.id == invId && it.quantity == 30L } }
        }
        assertEquals(30L, invAfterManualAdjust.first { it.id == invId }.quantity)
        val movementsAfterManual = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStockMovements(uid).first { list ->
                list.any { it.movementType == com.example.data.model.StockMovementRecord.TYPE_MANUAL_ADJUSTMENT }
            }
        }
        assertTrue(movementsAfterManual.any { it.quantityDelta == -5L && it.balanceAfter == 30L })

        val negativeAdjustResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.adjustInventoryQuantityWithAudit(invId, -999L, "Impossible deduction")
        }
        assertTrue(negativeAdjustResult.isFailure)

        // Reject excessive discount (>= gross total) & rejected CANCELLED/FAILED statuses on POS sale
        val badDiscountResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordSaleWithInventory(
                title = "Overdiscounted Sale",
                category = "Retail Sales",
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                transactionDate = "2026-11-15",
                inventoryItemId = invId,
                quantitySold = 1L,
                unitPriceKes = 325.0,
                discountKes = 500.0
            )
        }
        assertTrue(badDiscountResult.isFailure)

        val badStatusResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordSaleWithInventory(
                title = "Cancelled Checkout",
                category = "Retail Sales",
                paymentMethod = BusinessTransaction.METHOD_MPESA,
                paymentStatus = BusinessTransaction.STATUS_CANCELLED,
                transactionDate = "2026-11-15",
                inventoryItemId = invId,
                quantitySold = 1L,
                unitPriceKes = 325.0
            )
        }
        assertTrue(badStatusResult.isFailure)
    }

    @Test
    fun observeAndMutate_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = BusinessRepository(firestore, auth)
        val txId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addTransaction(
                type = BusinessTransaction.TYPE_EXPENSE,
                title = "Shop Rent",
                category = "Shop Rent",
                amountKes = 12000.0,
                paymentMethod = BusinessTransaction.METHOD_BANK
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = BusinessRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeTransactions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's transactions")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
                ?: throw AssertionError("Expected FirebaseFirestoreException, got $e", e)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeCustomers(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's customers")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
                ?: throw AssertionError("Expected FirebaseFirestoreException, got $e", e)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeCampaigns(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's campaigns")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
                ?: throw AssertionError("Expected FirebaseFirestoreException, got $e", e)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }

        // Bob attempting to delete Alice's doc via raw Firestore path
        val bobDeleteResult = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("transactions").document(txId)
                    .delete().let { com.google.android.gms.tasks.Tasks.await(it) }
            }
        }
        assertTrue(bobDeleteResult.isFailure)
        val delEx = bobDeleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, delEx?.code)
    }

    @Test
    fun observeProfile_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = BusinessRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBusinessProfile("some_user_123").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated caller reads profile")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
                ?: throw AssertionError("Expected FirebaseFirestoreException, got $e", e)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice.nakuru@test.com"
        const val BOB_EMAIL = "bob.nakuru@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
