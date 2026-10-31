package ir.smartcallcrm.data.local.dao

/** Aggregate rows returned by grouped DAO queries. */
data class TypeCount(val type: Int, val count: Int)

data class NumberStat(
    val number: String,
    val name: String?,
    val count: Int
)
