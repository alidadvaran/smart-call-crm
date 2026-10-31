package ir.smartcallcrm.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val deviceUuid: String,
    val name: String,
    val role: String
)

@Serializable
data class RegisterDeviceResponse(
    val deviceId: String,
    val token: String
)

@Serializable
data class CallDto(
    val clientUuid: String,
    val sourceId: Long,
    val number: String,
    val numberNormalized: String,
    val contactName: String? = null,
    val type: Int,
    val startedAt: Long,
    val durationSec: Long,
    val phoneAccountId: String? = null,
    val category: String = "UNSET"
)

@Serializable
data class UploadCallsRequest(
    val deviceUuid: String,
    val calls: List<CallDto>
)

@Serializable
data class UploadCallsResponse(
    val accepted: Int = 0,
    val duplicates: Int = 0
)

@Serializable
data class CustomerDto(
    val numberNormalized: String,
    val displayName: String,
    val company: String? = null,
    val status: String = "NEW",
    val notes: String? = null,
    val lastCallAt: Long = 0,
    val callCount: Int = 0
)

@Serializable
data class UploadCustomersRequest(
    val customers: List<CustomerDto>
)

@Serializable
data class ReportRequest(
    /** DAILY | WEEKLY | MANUAL */
    val type: String,
    val recipient: String
)

@Serializable
data class Ack(
    val ok: Boolean = true,
    val message: String? = null
)

@Serializable
data class AppReleaseDto(
    val versionCode: Int = 0,
    val versionName: String = "",
    val url: String? = null,
    val sizeBytes: Long = 0
)
