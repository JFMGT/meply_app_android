package de.meply.meply.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://admin.meeplemates.de/api/"
    const val STRAPI_IMAGE_BASE = "https://admin.meeplemates.de"
    // APP_JWT - used for user authentication (login/register)
    private const val APP_JWT = "7390af640e9802bca102d00294a68093bac023d80b545a2ce0dfa527af366e47c033f3a6c99fc4a5334a9ad7582f8f433415f0ada1d9bfa65e52104e1b3e347f88b2b7e960f6bbdcce27b1e163b9601de2dbb4ba0c71acf14326527f0b733bda507b96b23108a94961ddf3e3f5b92308ea5627f2557d5aded1ef44715cde8bda"
    //private const val APP_JWT = "6c7f20141f6312b426b587a0d40a469cb72f85f3b2e51e750d86b7700f75f03d2de6097882b576d3f700e31b0a6baa67b2e4e9e182509d8f2242fd8432635ed204badad96a09c5775b9f1d63ae1851ad86d8a62cfe08d613cea65d3431f80f57df3836a5c80909bb87ea5f19db85cd4317c5803c28206ef2d966c60cd5b041eb"

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
