package ir.smartcallcrm.data.repo

import ir.smartcallcrm.data.local.dao.CustomerDao
import ir.smartcallcrm.data.local.dao.FollowUpDao
import ir.smartcallcrm.data.local.dao.FollowUpRow
import ir.smartcallcrm.data.local.entity.CustomerEntity
import ir.smartcallcrm.data.local.entity.FollowUpEntity
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val followUpDao: FollowUpDao
) {
    fun observeCustomers(query: String, status: String): Flow<List<CustomerEntity>> =
        customerDao.observeAll(query.trim(), status)

    fun observeCustomer(id: Long): Flow<CustomerEntity?> = customerDao.observeById(id)

    suspend fun setStatus(id: Long, status: String) = customerDao.setStatus(id, status)
    suspend fun setNotes(id: Long, notes: String) = customerDao.setNotes(id, notes)

    suspend fun updateProfile(customer: CustomerEntity) =
        customerDao.update(customer.copy(synced = false))

    fun observeNewCount(since: Long): Flow<Int> = customerDao.observeNewCount(since)

    // Follow-ups
    fun observeFollowUps(status: String): Flow<List<FollowUpRow>> = followUpDao.observe(status)
    fun observePendingFollowUps(): Flow<Int> = followUpDao.observePendingCount()
    fun observeOverdueFollowUps(now: Long): Flow<Int> = followUpDao.observeOverdueCount(now)

    suspend fun addFollowUp(customerId: Long, dueAt: Long, note: String) =
        followUpDao.insert(FollowUpEntity(customerId = customerId, dueAt = dueAt, note = note))

    suspend fun completeFollowUp(id: Long) = followUpDao.markDone(id)

    suspend fun deleteAll() {
        followUpDao.deleteAll()
        customerDao.deleteAll()
    }
}
