package ir.smartcallcrm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.smartcallcrm.data.local.entity.CallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {

    /** Returns the new row id, or -1 when the unique index rejected a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(call: CallEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(calls: List<CallEntity>): List<Long>

    @Query("SELECT COALESCE(MAX(sourceCallId), -1) FROM calls WHERE deviceUuid = :deviceUuid")
    suspend fun maxSourceCallId(deviceUuid: String): Long

    @Query(
        """
        SELECT * FROM calls
        WHERE (:device IS NULL OR deviceUuid = :device)
          AND startedAt >= :since
          AND (:type < 0 OR type = :type)
        ORDER BY startedAt DESC
        LIMIT :limit
        """
    )
    fun observeFiltered(
        device: String?,
        since: Long,
        type: Int,
        limit: Int
    ): Flow<List<CallEntity>>

    @Query(
        """
        SELECT type AS type, COUNT(*) AS count FROM calls
        WHERE (:device IS NULL OR deviceUuid = :device) AND startedAt >= :since
        GROUP BY type
        """
    )
    fun observeTypeCounts(device: String?, since: Long): Flow<List<TypeCount>>

    @Query(
        """
        SELECT COALESCE(SUM(durationSec), 0) FROM calls
        WHERE (:device IS NULL OR deviceUuid = :device) AND startedAt >= :since
        """
    )
    fun observeTotalTalk(device: String?, since: Long): Flow<Long>

    @Query(
        """
        SELECT COUNT(*) FROM calls
        WHERE (:device IS NULL OR deviceUuid = :device) AND startedAt >= :since
        """
    )
    fun observeTotal(device: String?, since: Long): Flow<Int>

    @Query(
        """
        SELECT numberNormalized AS number, MAX(contactName) AS name, COUNT(*) AS count
        FROM calls
        WHERE (:device IS NULL OR deviceUuid = :device) AND startedAt >= :since
        GROUP BY numberNormalized
        ORDER BY count DESC
        LIMIT :limit
        """
    )
    fun observeTopNumbers(device: String?, since: Long, limit: Int): Flow<List<NumberStat>>

    @Query("SELECT * FROM calls WHERE synced = 0 ORDER BY startedAt ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<CallEntity>

    @Query("SELECT COUNT(*) FROM calls WHERE synced = 0")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE calls SET synced = 1 WHERE clientUuid IN (:uuids)")
    suspend fun markSynced(uuids: List<String>)

    @Query("UPDATE calls SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: String)

    @Query("SELECT COUNT(*) FROM calls WHERE numberNormalized = :number")
    suspend fun countForNumber(number: String): Int

    @Query("DELETE FROM calls")
    suspend fun deleteAll()
}
