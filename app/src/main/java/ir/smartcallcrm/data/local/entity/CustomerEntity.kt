package ir.smartcallcrm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A CRM customer. One profile per normalized phone number, so the same number
 * seen on both phones maps to a single shared customer while each phone's calls
 * stay recorded separately in [CallEntity].
 */
@Entity(
    tableName = "customers",
    indices = [Index(value = ["numberNormalized"], unique = true)]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numberNormalized: String,
    val displayName: String,
    val company: String? = null,
    /** NEW | CONTACTED | QUALIFIED | FOLLOWUP | WON | LOST */
    val status: String = "NEW",
    val notes: String? = null,
    val lastCallAt: Long = 0,
    val callCount: Int = 0,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
