package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessOverviewDao {

    @Query(
        "SELECT COALESCE(SUM(amountKes), 0.0) FROM local_transactions " +
            "WHERE userId = :userId AND type = 'SALE' AND paymentStatus NOT IN ('CANCELLED', 'FAILED')"
    )
    fun observeTotalSalesKes(userId: String): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amountKes), 0.0) FROM local_transactions " +
            "WHERE userId = :userId AND type = 'EXPENSE' AND paymentStatus NOT IN ('CANCELLED', 'FAILED')"
    )
    fun observeTotalExpensesKes(userId: String): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amountKes), 0.0) FROM local_transactions " +
            "WHERE userId = :userId AND type = 'OTHER' AND paymentStatus NOT IN ('CANCELLED', 'FAILED')"
    )
    fun observeOtherTransactionsKes(userId: String): Flow<Double>

    @Query(
        "SELECT COUNT(*) FROM local_transactions " +
            "WHERE userId = :userId AND type = 'SALE' AND paymentStatus NOT IN ('CANCELLED', 'FAILED')"
    )
    fun observeSalesCount(userId: String): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM local_transactions " +
            "WHERE userId = :userId AND type = 'EXPENSE' AND paymentStatus NOT IN ('CANCELLED', 'FAILED')"
    )
    fun observeExpenseCount(userId: String): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(amountKes), 0.0) FROM local_transactions " +
            "WHERE userId = :userId AND paymentMethod != 'CASH' AND paymentStatus NOT IN ('CANCELLED', 'FAILED')"
    )
    fun observeDigitalPaymentsKes(userId: String): Flow<Double>

    @Query(
        "SELECT category, COALESCE(SUM(amountKes), 0.0) AS totalKes, COUNT(*) AS entryCount " +
            "FROM local_transactions WHERE userId = :userId AND type = :type " +
            "AND paymentStatus NOT IN ('CANCELLED', 'FAILED') " +
            "GROUP BY category ORDER BY totalKes DESC"
    )
    fun observeCategoryBreakdown(userId: String, type: String): Flow<List<RoomCategorySummary>>

    @Query(
        "SELECT paymentMethod, COALESCE(SUM(amountKes), 0.0) AS totalKes, COUNT(*) AS entryCount " +
            "FROM local_transactions WHERE userId = :userId " +
            "AND paymentStatus NOT IN ('CANCELLED', 'FAILED') " +
            "GROUP BY paymentMethod ORDER BY totalKes DESC"
    )
    fun observePaymentBreakdown(userId: String): Flow<List<RoomPaymentSummary>>

    @Query(
        "SELECT * FROM local_transactions WHERE userId = :userId " +
            "AND paymentStatus NOT IN ('CANCELLED', 'FAILED') " +
            "ORDER BY transactionDate DESC, syncedAtEpochMs DESC LIMIT :limit"
    )
    fun observeRecentTransactions(userId: String, limit: Int): Flow<List<LocalTransactionEntity>>

    @Query("SELECT * FROM local_kpi_snapshots WHERE userId = :userId LIMIT 1")
    fun observeKpiSnapshot(userId: String): Flow<LocalKpiSnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(items: List<LocalTransactionEntity>)

    @Query("DELETE FROM local_transactions WHERE userId = :userId")
    suspend fun deleteTransactionsForUser(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKpiSnapshot(snapshot: LocalKpiSnapshotEntity)

    @Query("DELETE FROM local_kpi_snapshots WHERE userId = :userId")
    suspend fun deleteSnapshotForUser(userId: String)

    @Transaction
    suspend fun replaceUserOverviewCache(
        userId: String,
        transactions: List<LocalTransactionEntity>,
        snapshot: LocalKpiSnapshotEntity
    ) {
        deleteTransactionsForUser(userId)
        if (transactions.isNotEmpty()) {
            insertTransactions(transactions)
        }
        upsertKpiSnapshot(snapshot)
    }
}
