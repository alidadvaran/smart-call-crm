package ir.smartcallcrm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ir.smartcallcrm.data.local.entity.FollowUpEntity
import kotlinx.coroutines.flow.Flow

/** Follow-up joined with its customer's name/number for list display. */
data class FollowUpRow(
    val id: Long,
    val customerId: Long,
    val dueAt: Long,
    val note: String,
    val status: String,
    val customerName: String,
    val customerNumber: String
)

@Dao
interface FollowUpDao {

    @Insert
    suspend fun insert(followUp: FollowUpEntity): Long

    @Query(
        """
        SELECT f.id AS id, f.customerId AS customerId, f.dueAt AS dueAt,
               f.note AS note, f.status AS status,
               c.displayName AS customerName, c.numberNormalized AS customerNumber
        FROM follow_ups f
        INNER JOIN customers c ON c.id = f.customerId
        WHERE (:status = '' OR f.status = :status)
        ORDER BY f.dueAt ASC
        """
    )
    fun observe(status: String): Flow<List<FollowUpRow>>

    @Query("UPDATE follow_ups SET status = 'DONE', synced = 0 WHERE id = :id")
    suspend fun markDone(id: Long)

    @Query("SELECT COUNT(*) FROM follow_ups WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM follow_ups WHERE status = 'PENDING' AND dueAt < :now")
    fun observeOverdueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM follow_ups WHERE synced = 0 LIMIT :limit")
    suspend fun unsynced(limit: Int): List<FollowUpEntity>

    @Query("UPDATE follow_ups SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM follow_ups")
    suspend fun deleteAll()
}
