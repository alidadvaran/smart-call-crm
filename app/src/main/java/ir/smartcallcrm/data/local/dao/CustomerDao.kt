package ir.smartcallcrm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.smartcallcrm.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE numberNormalized = :number LIMIT 1")
    suspend fun findByNumber(number: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<CustomerEntity?>

    @Query(
        """
        SELECT * FROM customers
        WHERE (:q = '' OR displayName LIKE '%' || :q || '%' OR numberNormalized LIKE '%' || :q || '%')
          AND (:status = '' OR status = :status)
        ORDER BY lastCallAt DESC
        """
    )
    fun observeAll(q: String, status: String): Flow<List<CustomerEntity>>

    @Query("UPDATE customers SET status = :status, synced = 0 WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("UPDATE customers SET notes = :notes, synced = 0 WHERE id = :id")
    suspend fun setNotes(id: Long, notes: String)

    @Query(
        """
        UPDATE customers
        SET callCount = callCount + 1,
            lastCallAt = MAX(lastCallAt, :callAt),
            synced = 0
        WHERE numberNormalized = :number
        """
    )
    suspend fun registerCall(number: String, callAt: Long)

    @Query("SELECT COUNT(*) FROM customers WHERE createdAt >= :since")
    fun observeNewCount(since: Long): Flow<Int>

    @Query("SELECT * FROM customers WHERE synced = 0 LIMIT :limit")
    suspend fun unsynced(limit: Int): List<CustomerEntity>

    @Query("UPDATE customers SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
