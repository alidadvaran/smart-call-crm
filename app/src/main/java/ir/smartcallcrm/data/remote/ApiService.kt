package ir.smartcallcrm.data.remote

import ir.smartcallcrm.data.remote.dto.Ack
import ir.smartcallcrm.data.remote.dto.AppReleaseDto
import ir.smartcallcrm.data.remote.dto.RegisterDeviceRequest
import ir.smartcallcrm.data.remote.dto.RegisterDeviceResponse
import ir.smartcallcrm.data.remote.dto.ReportRequest
import ir.smartcallcrm.data.remote.dto.UploadCallsRequest
import ir.smartcallcrm.data.remote.dto.UploadCallsResponse
import ir.smartcallcrm.data.remote.dto.UploadCustomersRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {

    @POST("api/devices/register")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest): RegisterDeviceResponse

    @POST("api/calls/upload")
    suspend fun uploadCalls(@Body body: UploadCallsRequest): UploadCallsResponse

    @POST("api/customers/upload")
    suspend fun uploadCustomers(@Body body: UploadCustomersRequest): Ack

    @POST("api/reports/send")
    suspend fun sendReport(@Body body: ReportRequest): Ack

    @GET("api/app/latest")
    suspend fun latestRelease(): AppReleaseDto

    /** Full URL is returned by latestRelease(); stream it to a file. */
    @Streaming
    @GET
    suspend fun downloadApk(@Url url: String): ResponseBody
}
