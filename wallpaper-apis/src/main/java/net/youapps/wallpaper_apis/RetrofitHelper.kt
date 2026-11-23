package net.youapps.wallpaper_apis

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object RetrofitHelper {
    private const val USER_AGENT_HEADER =
        "Mozilla/5.0 (X11; Linux x86_64; rv:151.0) Gecko/20100101 Firefox/151.0"

    /**
     * Requests with this header are not logged, e.g. because they contain passwords or access tokens
     * in their body. The header is removed before the request is sent.
     */
    const val NO_LOGGING_HEADER = "X-WallYou-No-Logging"

    val json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = true
            encodeDefaults = true
        }
    }

    val okHttpClient by lazy {
        val builder = OkHttpClient.Builder()

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        // never leak credentials of logged in users to the logs
        loggingInterceptor.redactHeader("X-API-Key")
        loggingInterceptor.redactHeader("Authorization")
        builder.addInterceptor { chain ->
            val request = chain.request()

            // requests that contain credentials (e.g. passwords) in their body are not logged at all
            if (request.header(NO_LOGGING_HEADER) != null) {
                chain.proceed(request.newBuilder().removeHeader(NO_LOGGING_HEADER).build())
            } else {
                loggingInterceptor.intercept(chain)
            }
        }

        // add user agent to all requests
        builder.addInterceptor { interceptorChain ->
            val request = interceptorChain.request()
                .newBuilder()
                .addHeader("User-Agent", USER_AGENT_HEADER)
                .addHeader("Accept", "*/*")
            interceptorChain.proceed(request.build())
        }

        builder.build()
    }

    inline fun <reified T> create(baseUrl: String): T {
        val mediaType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .build()
            .create(T::class.java)
    }
}