package ir.smartcallcrm.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Builds an [ApiService] for a given base URL. The auth token is read lazily
 * on every request via [tokenProvider] so a freshly-registered device starts
 * authenticating without rebuilding the client.
 */
object ApiClientFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun create(baseUrl: String, tokenProvider: () -> String?): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                tokenProvider()?.let { builder.header("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}
