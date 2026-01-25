package de.meply.meply.network

import de.meply.meply.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val BASE_URL = BuildConfig.API_BASE
    val STRAPI_IMAGE_BASE = BuildConfig.IMAGE_BASE
    // APP_JWT - used for user authentication (login/register)
    private val APP_JWT = BuildConfig.APP_JWT

    @Volatile private var userJwt: String? = null

    fun setJwt(jwt: String?) {
        userJwt = jwt
    }

    fun clearJwt() {
        userJwt = null
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request()
            val path = req.url.encodedPath
            val isLogin = path.contains("/auth/local")
            val isRegister = path.contains("/auth/register")
            val b = req.newBuilder()

            val existingAuthHeader = req.header("Authorization")

            if (existingAuthHeader != null) {
                // Request already has explicit Authorization header - don't override
            } else if (isRegister) {
                // Register is public - no auth header
            } else if (isLogin) {
                b.header("Authorization", "Bearer $APP_JWT")
            } else {
                val currentJwt = userJwt
                if (!currentJwt.isNullOrBlank()) {
                    b.header("Authorization", "Bearer $currentJwt")
                }
            }
            chain.proceed(b.build())
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
