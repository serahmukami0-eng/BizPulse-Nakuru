package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_transactions",
    indices = [
        Index(value = ["userId", "type"]),
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "paymentMethod"])
    ]
)
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val category: String,
    val amountKes: Double,
    val paymentMethod: String,
    val paymentStatus: String,
    val transactionDate: String,
    val isDemoData: Boolean,
    val syncedAtEpochMs: Long
)

@Entity(tableName = "local_kpi_snapshots")
data class LocalKpiSnapshotEntity(
    @PrimaryKey val userId: String,
    val businessName: String,
    val businessType: String,
    val locationNakuru: String,
    val totalSalesKes: Double,
    val totalExpensesKes: Double,
    val otherTransactionsKes: Double,
    val netProfitKes: Double,
    val salesCount: Int,
    val expenseCount: Int,
    val digitalPaymentsKes: Double,
    val digitalTxnCount: Int,
    val isDemoData: Boolean,
    val lastSyncedEpochMs: Long
)

data class RoomCategorySummary(
    val category: String,
    val totalKes: Double,
    val entryCount: Int
)

data class RoomPaymentSummary(
    val paymentMethod: String,
    val totalKes: Double,
    val entryCount: Int
)
