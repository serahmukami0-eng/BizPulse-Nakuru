package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CustomerRecord
import com.example.data.model.DigitalMarketingCampaign
import com.example.data.model.InventoryItem
import com.example.data.model.OnlineCommunicationLog
import com.example.data.model.PurchaseOrderRecord
import com.example.data.model.StockMovementRecord
import com.example.data.model.SupplierRecord
import com.example.data.model.todayIsoDate
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BusinessRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeBusinessProfile(userId: String): Flow<BusinessProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            BusinessProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<BusinessTransaction>> = flow {
        val colRef = db.collection("users").document(userId).collection("transactions")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BusinessTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeInventory(userId: String): Flow<List<InventoryItem>> = flow {
        val colRef = db.collection("users").document(userId).collection("inventory")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InventoryItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeCustomers(userId: String): Flow<List<CustomerRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("customers")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomerRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.totalSpentKes }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeSuppliers(userId: String): Flow<List<SupplierRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("suppliers")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupplierRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observePurchases(userId: String): Flow<List<PurchaseOrderRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("purchases")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PurchaseOrderRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeStockMovements(userId: String): Flow<List<StockMovementRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("stockMovements")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StockMovementRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeCampaigns(userId: String): Flow<List<DigitalMarketingCampaign>> = flow {
        val colRef = db.collection("users").document(userId).collection("campaigns")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DigitalMarketingCampaign::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeCommunications(userId: String): Flow<List<OnlineCommunicationLog>> = flow {
        val colRef = db.collection("users").document(userId).collection("communications")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OnlineCommunicationLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun fetchBusinessProfileSnapshot(userId: String): BusinessProfile? {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(
                    BusinessProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun fetchTransactionHistorySnapshot(userId: String): List<BusinessTransaction> {
        val colRef = db.collection("users").document(userId).collection("transactions")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(
                BusinessTransaction::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun fetchInventorySnapshot(userId: String): List<InventoryItem> {
        val colRef = db.collection("users").document(userId).collection("inventory")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(
                InventoryItem::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.name.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun fetchCustomersSnapshot(userId: String): List<CustomerRecord> {
        val colRef = db.collection("users").document(userId).collection("customers")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(
                CustomerRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.totalSpentKes }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun fetchSuppliersSnapshot(userId: String): List<SupplierRecord> {
        val colRef = db.collection("users").document(userId).collection("suppliers")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(
                SupplierRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.name.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun fetchPurchasesSnapshot(userId: String): List<PurchaseOrderRecord> {
        val colRef = db.collection("users").document(userId).collection("purchases")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(
                PurchaseOrderRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun fetchCampaignsSnapshot(userId: String): List<DigitalMarketingCampaign> {
        val colRef = db.collection("users").document(userId).collection("campaigns")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(
                DigitalMarketingCampaign::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun fetchCommunicationsSnapshot(userId: String): List<OnlineCommunicationLog> {
        val colRef = db.collection("users").document(userId).collection("communications")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(
                OnlineCommunicationLog::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun saveBusinessProfile(
        ownerName: String,
        businessName: String,
        businessType: String,
        locationNakuru: String,
        businessSize: String = "Small (6–20 staff)",
        phone: String,
        demoModeEnabled: Boolean,
        preserveExistingDemoMode: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val effectiveDemoMode = if (preserveExistingDemoMode) {
                    existing.getBoolean("demoModeEnabled") ?: demoModeEnabled
                } else {
                    demoModeEnabled
                }
                val updates = mapOf(
                    "ownerName" to ownerName.trim().take(80),
                    "businessName" to businessName.trim().take(100),
                    "businessType" to businessType.trim().take(50),
                    "locationNakuru" to locationNakuru.trim().take(80),
                    "businessSize" to businessSize.trim().take(50),
                    "phone" to phone.trim().take(25),
                    "demoModeEnabled" to effectiveDemoMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "ownerName" to ownerName.trim().take(80),
                    "businessName" to businessName.trim().take(100),
                    "businessType" to businessType.trim().take(50),
                    "locationNakuru" to locationNakuru.trim().take(80),
                    "businessSize" to businessSize.trim().take(50),
                    "phone" to phone.trim().take(25),
                    "currency" to "KES",
                    "demoModeEnabled" to demoModeEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setDemoModeEnabled(enabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update(
                mapOf(
                    "demoModeEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addTransaction(
        type: String,
        title: String,
        category: String,
        amountKes: Double,
        paymentMethod: String,
        transactionRef: String = "",
        paymentStatus: String = BusinessTransaction.STATUS_COMPLETED,
        transactionDate: String = todayIsoDate(),
        notes: String = "",
        customerId: String = "",
        customerName: String = "",
        costOfGoodsSoldKes: Double = 0.0,
        receiptNumber: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document()
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("type", type)
            put("title", title.trim().take(100))
            put("category", category.trim().take(50))
            put("amountKes", amountKes)
            put("paymentMethod", paymentMethod)
            put("transactionRef", transactionRef.trim().take(80))
            put("paymentStatus", paymentStatus)
            put("transactionDate", transactionDate.trim().take(20))
            put("notes", notes.trim().take(300))
            if (customerId.isNotBlank()) put("customerId", customerId.trim().take(128))
            if (customerName.isNotBlank()) put("customerName", customerName.trim().take(100))
            if (costOfGoodsSoldKes > 0.0) put("costOfGoodsSoldKes", costOfGoodsSoldKes)
            if (receiptNumber.isNotBlank()) put("receiptNumber", receiptNumber.trim().take(50))
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordSaleWithInventory(
        title: String,
        category: String,
        paymentMethod: String,
        transactionRef: String = "",
        paymentStatus: String = BusinessTransaction.STATUS_COMPLETED,
        transactionDate: String = todayIsoDate(),
        notes: String = "",
        customerId: String = "",
        customerName: String = "",
        inventoryItemId: String = "",
        quantitySold: Long = 1L,
        unitPriceKes: Double = 0.0,
        discountKes: Double = 0.0,
        manualAmountKes: Double = 0.0
    ): Result<String> {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val txRef = userDoc.collection("transactions").document()
        val movementRef = userDoc.collection("stockMovements").document()
        val invRef = inventoryItemId.takeIf { it.isNotBlank() }?.let { userDoc.collection("inventory").document(it) }
        val custRef = customerId.takeIf { it.isNotBlank() }?.let { userDoc.collection("customers").document(it) }
        val receiptNo = "PX-${(System.currentTimeMillis() % 900000L + 100000L)}"

        return try {
            db.runTransaction { transaction ->
                val invSnap = invRef?.let { transaction.get(it) }
                val custSnap = custRef?.let { transaction.get(it) }

                var itemName = ""
                var effectiveUnitPrice = unitPriceKes.coerceAtLeast(0.0)
                var totalCogsKes = 0.0
                var nextStockQty = 0L

                if (invRef != null) {
                    if (invSnap == null || !invSnap.exists()) {
                        throw IllegalStateException("Selected inventory product no longer exists.")
                    }
                    val currentQty = invSnap.getLong("quantity") ?: 0L
                    if (quantitySold <= 0L) {
                        throw IllegalArgumentException("Sale quantity must be at least 1.")
                    }
                    if (currentQty < quantitySold) {
                        throw IllegalStateException(
                            "Insufficient stock for ${invSnap.getString("name").orEmpty()} (Available: $currentQty, Requested: $quantitySold)."
                        )
                    }
                    itemName = invSnap.getString("name").orEmpty()
                    if (effectiveUnitPrice <= 0.0) {
                        effectiveUnitPrice = invSnap.getDouble("unitPriceKes") ?: 0.0
                    }
                    val itemCost = invSnap.getDouble("unitCostKes") ?: 0.0
                    totalCogsKes = itemCost * quantitySold
                    nextStockQty = currentQty - quantitySold
                }

                if (paymentStatus != BusinessTransaction.STATUS_COMPLETED &&
                    paymentStatus != BusinessTransaction.STATUS_PENDING
                ) {
                    throw IllegalArgumentException("POS checkout status must be COMPLETED or PENDING.")
                }

                val safeDiscount = discountKes.coerceAtLeast(0.0)
                val grossTotal = if (invRef != null) effectiveUnitPrice * quantitySold else manualAmountKes
                if (grossTotal <= 0.0) {
                    throw IllegalArgumentException("Sale amount must exceed 0 KES.")
                }
                if (safeDiscount >= grossTotal) {
                    throw IllegalArgumentException("Discount cannot equal or exceed gross sale amount.")
                }
                val computedTotal = grossTotal - safeDiscount
                val finalTitle = title.trim().ifBlank {
                    if (itemName.isNotBlank()) "$itemName x$quantitySold" else "POS Sale"
                }.take(100)

                val txPayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("type", BusinessTransaction.TYPE_SALE)
                    put("title", finalTitle)
                    put("category", category.trim().ifBlank { "Retail Sales" }.take(50))
                    put("amountKes", computedTotal)
                    put("paymentMethod", paymentMethod)
                    put("transactionRef", transactionRef.trim().take(80))
                    put("paymentStatus", paymentStatus)
                    put("transactionDate", transactionDate.trim().take(20))
                    put("notes", notes.trim().take(300))
                    if (customerId.isNotBlank()) put("customerId", customerId.trim().take(128))
                    if (customerName.isNotBlank()) put("customerName", customerName.trim().take(100))
                    if (invRef != null) {
                        put("inventoryItemId", inventoryItemId.trim().take(128))
                        put("inventoryItemName", itemName.trim().take(100))
                        put("quantitySold", quantitySold)
                        put("unitPriceKes", effectiveUnitPrice)
                    }
                    if (safeDiscount > 0.0) put("discountKes", safeDiscount)
                    if (totalCogsKes > 0.0) put("costOfGoodsSoldKes", totalCogsKes)
                    put("receiptNumber", receiptNo)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                transaction.set(txRef, txPayload)

                if (invRef != null) {
                    transaction.update(
                        invRef,
                        mapOf(
                            "quantity" to nextStockQty,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val mvPayload = mapOf(
                        "userId" to uid,
                        "inventoryItemId" to inventoryItemId.trim().take(128),
                        "itemName" to itemName.trim().ifBlank { finalTitle }.take(100),
                        "movementType" to StockMovementRecord.TYPE_SALE_DEDUCTION,
                        "quantityDelta" to -quantitySold,
                        "balanceAfter" to nextStockQty,
                        "referenceId" to receiptNo,
                        "movementDate" to transactionDate.trim().take(20),
                        "notes" to "POS sale deduction ($receiptNo)".take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(movementRef, mvPayload)
                }

                if (custRef != null && custSnap != null && custSnap.exists() &&
                    paymentStatus != BusinessTransaction.STATUS_CANCELLED
                ) {
                    val prevSpent = custSnap.getDouble("totalSpentKes") ?: 0.0
                    val prevCount = custSnap.getLong("purchaseCount") ?: 0L
                    transaction.update(
                        custRef,
                        mapOf(
                            "totalSpentKes" to (prevSpent + computedTotal),
                            "purchaseCount" to (prevCount + 1L),
                            "lastPurchaseDate" to transactionDate.trim().take(20),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                txRef.id
            }.await()
            Result.success(txRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelSaleTransaction(tx: BusinessTransaction): Result<Unit> {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val txRef = userDoc.collection("transactions").document(tx.id)
        val invRef = tx.inventoryItemId.takeIf { it.isNotBlank() && tx.quantitySold > 0L }
            ?.let { userDoc.collection("inventory").document(it) }
        val custRef = tx.customerId.takeIf { it.isNotBlank() }
            ?.let { userDoc.collection("customers").document(it) }
        val movementRef = userDoc.collection("stockMovements").document()

        return try {
            db.runTransaction { transaction ->
                val txSnap = transaction.get(txRef)
                if (!txSnap.exists()) {
                    throw IllegalStateException("Transaction record not found.")
                }
                val currentStatus = txSnap.getString("paymentStatus").orEmpty()
                if (currentStatus == BusinessTransaction.STATUS_CANCELLED) {
                    throw IllegalStateException("Sale is already cancelled.")
                }
                val invSnap = invRef?.let { transaction.get(it) }
                val custSnap = custRef?.let { transaction.get(it) }

                transaction.update(
                    txRef,
                    mapOf(
                        "paymentStatus" to BusinessTransaction.STATUS_CANCELLED,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (invRef != null && invSnap != null && invSnap.exists()) {
                    val currentQty = invSnap.getLong("quantity") ?: 0L
                    val restoredQty = currentQty + tx.quantitySold
                    transaction.update(
                        invRef,
                        mapOf(
                            "quantity" to restoredQty,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val mvPayload = mapOf(
                        "userId" to uid,
                        "inventoryItemId" to tx.inventoryItemId.take(128),
                        "itemName" to (invSnap.getString("name").orEmpty().ifBlank { tx.inventoryItemName.ifBlank { tx.title } }).take(100),
                        "movementType" to StockMovementRecord.TYPE_SALE_REVERSAL,
                        "quantityDelta" to tx.quantitySold,
                        "balanceAfter" to restoredQty,
                        "referenceId" to tx.receiptNumber.ifBlank { tx.id }.take(128),
                        "movementDate" to todayIsoDate(),
                        "notes" to "Cancelled sale stock reversal".take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(movementRef, mvPayload)
                }

                if (custRef != null && custSnap != null && custSnap.exists()) {
                    val prevSpent = custSnap.getDouble("totalSpentKes") ?: 0.0
                    val prevCount = custSnap.getLong("purchaseCount") ?: 0L
                    transaction.update(
                        custRef,
                        mapOf(
                            "totalSpentKes" to (prevSpent - tx.amountKes).coerceAtLeast(0.0),
                            "purchaseCount" to (prevCount - 1L).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        transactionId: String,
        type: String,
        title: String,
        category: String,
        amountKes: Double,
        paymentMethod: String,
        transactionRef: String = "",
        paymentStatus: String = BusinessTransaction.STATUS_COMPLETED,
        transactionDate: String = todayIsoDate(),
        notes: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
        return try {
            docRef.update(
                mapOf(
                    "type" to type,
                    "title" to title.trim().take(100),
                    "category" to category.trim().take(50),
                    "amountKes" to amountKes,
                    "paymentMethod" to paymentMethod,
                    "transactionRef" to transactionRef.trim().take(80),
                    "paymentStatus" to paymentStatus,
                    "transactionDate" to transactionDate.trim().take(20),
                    "notes" to notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addInventoryItem(
        name: String,
        sku: String,
        category: String,
        quantity: Long,
        reorderLevel: Long,
        unitPriceKes: Double,
        unitCostKes: Double = 0.0,
        supplierId: String = "",
        supplierName: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inventory").document()
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("name", name.trim().take(100))
            put("sku", sku.trim().take(40))
            put("category", category.trim().take(50))
            put("quantity", quantity.coerceAtLeast(0L))
            put("reorderLevel", reorderLevel.coerceAtLeast(0L))
            put("unitPriceKes", unitPriceKes.coerceAtLeast(0.0))
            if (unitCostKes > 0.0) put("unitCostKes", unitCostKes)
            if (supplierId.isNotBlank()) put("supplierId", supplierId.trim().take(128))
            if (supplierName.isNotBlank()) put("supplierName", supplierName.trim().take(100))
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateInventoryItem(
        itemId: String,
        name: String,
        sku: String,
        category: String,
        quantity: Long,
        reorderLevel: Long,
        unitPriceKes: Double,
        unitCostKes: Double = 0.0,
        supplierId: String = "",
        supplierName: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inventory").document(itemId)
        return try {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(100),
                    "sku" to sku.trim().take(40),
                    "category" to category.trim().take(50),
                    "quantity" to quantity.coerceAtLeast(0L),
                    "reorderLevel" to reorderLevel.coerceAtLeast(0L),
                    "unitPriceKes" to unitPriceKes.coerceAtLeast(0.0),
                    "unitCostKes" to unitCostKes.coerceAtLeast(0.0),
                    "supplierId" to supplierId.trim().take(128),
                    "supplierName" to supplierName.trim().take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateInventoryQuantity(itemId: String, newQuantity: Long): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inventory").document(itemId)
        return try {
            docRef.update(
                mapOf(
                    "quantity" to newQuantity.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adjustInventoryQuantityWithAudit(
        itemId: String,
        delta: Long,
        notes: String = "Manual stock adjustment"
    ): Result<Long> {
        if (delta == 0L) return Result.success(0L)
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val invRef = userDoc.collection("inventory").document(itemId)
        val movementRef = userDoc.collection("stockMovements").document()

        return try {
            val nextQuantity = db.runTransaction { transaction ->
                val invSnap = transaction.get(invRef)
                if (!invSnap.exists()) {
                    throw IllegalStateException("Inventory item no longer exists.")
                }
                val currentQty = invSnap.getLong("quantity") ?: 0L
                val updatedQty = currentQty + delta
                if (updatedQty < 0L) {
                    throw IllegalArgumentException(
                        "Cannot reduce stock below 0 (Available: $currentQty)."
                    )
                }
                transaction.update(
                    invRef,
                    mapOf(
                        "quantity" to updatedQty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val itemName = invSnap.getString("name").orEmpty().ifBlank { "Stock Item" }
                val mvPayload = mapOf(
                    "userId" to uid,
                    "inventoryItemId" to itemId.take(128),
                    "itemName" to itemName.take(100),
                    "movementType" to StockMovementRecord.TYPE_MANUAL_ADJUSTMENT,
                    "quantityDelta" to delta,
                    "balanceAfter" to updatedQty,
                    "referenceId" to "ADJ-${System.currentTimeMillis() % 1000000L}",
                    "movementDate" to todayIsoDate(),
                    "notes" to notes.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(movementRef, mvPayload)
                updatedQty
            }.await()
            Result.success(nextQuantity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, invRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inventory").document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordStockPurchase(
        supplierId: String,
        supplierName: String,
        inventoryItemId: String,
        itemName: String,
        quantity: Long,
        unitCostKes: Double,
        paymentMethod: String,
        paymentStatus: String = PurchaseOrderRecord.STATUS_PAID,
        purchaseDate: String = todayIsoDate(),
        notes: String = "",
        logAsExpenseTransaction: Boolean = true
    ): Result<String> {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val poRef = userDoc.collection("purchases").document()
        val movementRef = userDoc.collection("stockMovements").document()
        val expRef = userDoc.collection("transactions").document()
        val invRef = inventoryItemId.takeIf { it.isNotBlank() }?.let { userDoc.collection("inventory").document(it) }

        val safeQty = quantity.coerceAtLeast(1L)
        val safeCost = unitCostKes.coerceAtLeast(0.0)
        val totalCost = safeQty * safeCost

        return try {
            db.runTransaction { transaction ->
                val invSnap = invRef?.let { transaction.get(it) }
                if (invRef != null && (invSnap == null || !invSnap.exists())) {
                    throw IllegalStateException("Selected inventory item no longer exists.")
                }

                val poPayload = buildMap<String, Any> {
                    put("userId", uid)
                    if (supplierId.isNotBlank()) put("supplierId", supplierId.trim().take(128))
                    if (supplierName.isNotBlank()) put("supplierName", supplierName.trim().take(100))
                    if (inventoryItemId.isNotBlank()) put("inventoryItemId", inventoryItemId.trim().take(128))
                    put("itemName", itemName.trim().take(100))
                    put("quantity", safeQty)
                    put("unitCostKes", safeCost)
                    put("totalCostKes", totalCost)
                    put("paymentMethod", paymentMethod)
                    put("paymentStatus", paymentStatus)
                    put("purchaseDate", purchaseDate.trim().take(20))
                    put("notes", notes.trim().take(300))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                transaction.set(poRef, poPayload)

                if (invRef != null && invSnap != null && invSnap.exists()) {
                    val prevQty = invSnap.getLong("quantity") ?: 0L
                    val nextQty = prevQty + safeQty
                    val invUpdates = buildMap<String, Any> {
                        put("quantity", nextQty)
                        if (safeCost > 0.0) put("unitCostKes", safeCost)
                        if (supplierId.isNotBlank()) put("supplierId", supplierId.trim().take(128))
                        if (supplierName.isNotBlank()) put("supplierName", supplierName.trim().take(100))
                        put("updatedAt", FieldValue.serverTimestamp())
                    }
                    transaction.update(invRef, invUpdates)

                    val mvPayload = buildMap<String, Any> {
                        put("userId", uid)
                        put("inventoryItemId", inventoryItemId.trim().take(128))
                        put("itemName", (invSnap.getString("name").orEmpty().ifBlank { itemName }).take(100))
                        put("movementType", StockMovementRecord.TYPE_PURCHASE_RESTOCK)
                        put("quantityDelta", safeQty)
                        put("balanceAfter", nextQty)
                        put("referenceId", poRef.id)
                        put("unitCostKes", safeCost)
                        put("totalCostKes", totalCost)
                        if (supplierId.isNotBlank()) put("supplierId", supplierId.trim().take(128))
                        if (supplierName.isNotBlank()) put("supplierName", supplierName.trim().take(100))
                        put("movementDate", purchaseDate.trim().take(20))
                        put("notes", notes.trim().ifBlank { "Supplier restock (${supplierName.ifBlank { "Supplier" }})" }.take(300))
                        put("createdAt", FieldValue.serverTimestamp())
                        put("updatedAt", FieldValue.serverTimestamp())
                    }
                    transaction.set(movementRef, mvPayload)
                }

                if (logAsExpenseTransaction && totalCost > 0.0) {
                    val txStatus = if (paymentStatus == PurchaseOrderRecord.STATUS_PAID) {
                        BusinessTransaction.STATUS_COMPLETED
                    } else {
                        BusinessTransaction.STATUS_PENDING
                    }
                    val expPayload = mapOf(
                        "userId" to uid,
                        "type" to BusinessTransaction.TYPE_EXPENSE,
                        "title" to "Restock: ${itemName.trim()} x$safeQty".take(100),
                        "category" to "Stock Restock",
                        "amountKes" to totalCost,
                        "paymentMethod" to paymentMethod,
                        "transactionRef" to poRef.id.take(80),
                        "paymentStatus" to txStatus,
                        "transactionDate" to purchaseDate.trim().take(20),
                        "notes" to "Supplier: ${supplierName.ifBlank { "Direct" }}".take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(expRef, expPayload)
                }
            }.await()
            Result.success(poRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, poRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePurchasePaymentStatus(purchaseId: String, newStatus: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("purchases").document(purchaseId)
        return try {
            docRef.update(
                mapOf(
                    "paymentStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePurchaseOrder(purchaseId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("purchases").document(purchaseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addCustomer(
        name: String,
        phone: String,
        email: String,
        location: String,
        notes: String = "",
        createdDate: String = todayIsoDate()
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("customers").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(100),
            "phone" to phone.trim().take(30),
            "email" to email.trim().take(120),
            "location" to location.trim().take(80),
            "notes" to notes.trim().take(300),
            "totalSpentKes" to 0.0,
            "purchaseCount" to 0L,
            "lastPurchaseDate" to "",
            "createdDate" to createdDate.trim().take(20),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(
        customerId: String,
        name: String,
        phone: String,
        email: String,
        location: String,
        notes: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("customers").document(customerId)
        return try {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(100),
                    "phone" to phone.trim().take(30),
                    "email" to email.trim().take(120),
                    "location" to location.trim().take(80),
                    "notes" to notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("customers").document(customerId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addSupplier(
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("suppliers").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(100),
            "contactPerson" to contactPerson.trim().take(100),
            "phone" to phone.trim().take(30),
            "email" to email.trim().take(120),
            "location" to location.trim().take(80),
            "suppliedProductsSummary" to suppliedProductsSummary.trim().take(250),
            "notes" to notes.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSupplier(
        supplierId: String,
        name: String,
        contactPerson: String,
        phone: String,
        email: String,
        location: String,
        suppliedProductsSummary: String,
        notes: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("suppliers").document(supplierId)
        return try {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(100),
                    "contactPerson" to contactPerson.trim().take(100),
                    "phone" to phone.trim().take(30),
                    "email" to email.trim().take(120),
                    "location" to location.trim().take(80),
                    "suppliedProductsSummary" to suppliedProductsSummary.trim().take(250),
                    "notes" to notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSupplier(supplierId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("suppliers").document(supplierId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addCampaign(
        title: String,
        channel: String,
        campaignDate: String = todayIsoDate(),
        spendKes: Double,
        reachCount: Long,
        engagementCount: Long,
        inquiriesGenerated: Long,
        status: String,
        notes: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("campaigns").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().take(100),
            "channel" to channel,
            "campaignDate" to campaignDate.trim().take(20),
            "spendKes" to spendKes.coerceAtLeast(0.0),
            "reachCount" to reachCount.coerceAtLeast(0L),
            "engagementCount" to engagementCount.coerceAtLeast(0L),
            "inquiriesGenerated" to inquiriesGenerated.coerceAtLeast(0L),
            "status" to status,
            "notes" to notes.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCampaign(
        campaignId: String,
        title: String,
        channel: String,
        campaignDate: String = todayIsoDate(),
        spendKes: Double,
        reachCount: Long,
        engagementCount: Long,
        inquiriesGenerated: Long,
        status: String,
        notes: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("campaigns").document(campaignId)
        return try {
            docRef.update(
                mapOf(
                    "title" to title.trim().take(100),
                    "channel" to channel,
                    "campaignDate" to campaignDate.trim().take(20),
                    "spendKes" to spendKes.coerceAtLeast(0.0),
                    "reachCount" to reachCount.coerceAtLeast(0L),
                    "engagementCount" to engagementCount.coerceAtLeast(0L),
                    "inquiriesGenerated" to inquiriesGenerated.coerceAtLeast(0L),
                    "status" to status,
                    "notes" to notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCampaign(campaignId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("campaigns").document(campaignId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addCommunicationLog(
        customerName: String,
        channel: String,
        subject: String,
        receivedDate: String = todayIsoDate(),
        responseDate: String = "",
        status: String,
        responseMinutes: Long,
        notes: String = "",
        category: String = OnlineCommunicationLog.CATEGORY_INQUIRY
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("communications").document()
        val payload = mapOf(
            "userId" to uid,
            "customerName" to customerName.trim().take(100),
            "channel" to channel,
            "category" to category,
            "subject" to subject.trim().take(120),
            "receivedDate" to receivedDate.trim().take(20),
            "responseDate" to responseDate.trim().take(20),
            "status" to status,
            "responseMinutes" to responseMinutes.coerceAtLeast(0L),
            "notes" to notes.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCommunicationLog(
        commId: String,
        customerName: String,
        channel: String,
        subject: String,
        receivedDate: String = todayIsoDate(),
        responseDate: String = "",
        status: String,
        responseMinutes: Long,
        notes: String = "",
        category: String = OnlineCommunicationLog.CATEGORY_INQUIRY
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("communications").document(commId)
        return try {
            docRef.update(
                mapOf(
                    "customerName" to customerName.trim().take(100),
                    "channel" to channel,
                    "category" to category,
                    "subject" to subject.trim().take(120),
                    "receivedDate" to receivedDate.trim().take(20),
                    "responseDate" to responseDate.trim().take(20),
                    "status" to status,
                    "responseMinutes" to responseMinutes.coerceAtLeast(0L),
                    "notes" to notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCommunicationStatus(commId: String, newStatus: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("communications").document(commId)
        val respDate = if (newStatus == OnlineCommunicationLog.STATUS_PENDING) "" else todayIsoDate()
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "responseDate" to respDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCommunicationLog(commId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("communications").document(commId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
