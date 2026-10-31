package ir.smartcallcrm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A scheduled follow-up task attached to a customer. */
@Entity(
    tableName = "follow_ups",
    indices = [Index(value = ["customerId"]), Index(value = ["status"]), Index(value = ["dueAt"])]
)
data class FollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val dueAt: Long,
    val note: String,
    /** PENDING | DONE */
    val status: String = "PENDING",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
