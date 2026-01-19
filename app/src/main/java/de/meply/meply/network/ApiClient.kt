package de.meply.meply.network // Dein Package-Name

import android.util.Log // Sicherstellen, dass der Import da ist
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

    // API_TOKEN - used for system-level operations (like creating boardgames)
    // This corresponds to STRAPI_API_TOKEN in the web version
    private const val API_TOKEN = "7352c93f31e79eab4f2bb18404507a5a42a163e6b3f24a7bce10b552bb927c301b191dc92fccabfa58c57191c33afa9a263bdec073d24562734ee67776b374eee0c0fb6220be872403aed4fad711ec160a36a481f648a73622c11e3d41349bf7d991d77c837b398541d649ece4c2cc74f2aa29b6abe1f9386613e755a6160ffa"
    // TOKE FÜR DEV 6c7f20141f6312b426b587a0d40a469cb72f85f3b2e51e750d86b7700f75f03d2de6097882b576d3f700e31b0a6baa67b2e4e9e182509d8f2242fd8432635ed204badad96a09c5775b9f1d63ae1851ad86d8a62cfe08d613cea65d3431f80f57df3836a5c80909bb87ea5f19db85cd4317c5803c28206ef2d966c60cd5b041eb
    @Volatile private var userJwt: String? = null

    fun setJwt(jwt: String?) {
        Log.d("ApiClientAuth", "ApiClient: Setting JWT to: $jwt") // << --- LOG HINZUGEFÜGT
        userJwt = jwt
    }

    fun clearJwt() {
        Log.d("ApiClientAuth", "ApiClient: Clearing JWT. Was: $userJwt") // << --- LOG HINZUGEFÜGT
        userJwt = null
    }

    /**
     * Get the system API token for operations that require system-level permissions
     * (e.g., creating boardgames - like web version uses useSystemToken=true with STRAPI_API_TOKEN)
     */
    fun getSystemToken(): String = "Bearer $API_TOKEN"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        // Um den Authorization Header im Log zu sehen, dies VORÜBERGEHEND auskommentieren:
        // redactHeader("Authorization")
    }

    // Explizite öffentliche Getter-Methode für den Zugriff von außen (z.B. für Logs)
    fun getCurrentJwt(): String? {
        return userJwt
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request()
            val isLoginOrRegister = req.url.encodedPath.contains("/auth/") // Erfasst /auth/local, /auth/register etc.
            val b = req.newBuilder()

            // Check if request already has an Authorization header (e.g., from @Header annotation)
            val existingAuthHeader = req.header("Authorization")

            // Logging für jede Anfrage im Auth Interceptor
            Log.d("ApiClientAuth", "ApiClient Interceptor: Request URL: ${req.url}")
            Log.d("ApiClientAuth", "ApiClient Interceptor: Is Auth call (login/register): $isLoginOrRegister")
            Log.d("ApiClientAuth", "ApiClient Interceptor: Existing Auth header: ${existingAuthHeader != null}")

            if (existingAuthHeader != null) {
                // Request already has explicit Authorization header - don't override it
                // This is used for system token calls (like creating boardgames)
                Log.d("ApiClientAuth", "ApiClient Interceptor: Using explicit Authorization header (system token)")
            } else if (isLoginOrRegister) {
                // Keine Aktion für Login- oder Registrierungs-Aufrufe, da kein JWT benötigt wird
                b.header("Authorization", "Bearer $APP_JWT")
                Log.d("ApiClientAuth", "ApiClient Interceptor: Auth call, using APP_JWT.")
            } else {
                val currentJwt = userJwt // Hole den aktuellen JWT
                // SEHR WICHTIGES LOG, um den Zustand des JWT vor der Verwendung zu sehen:
                Log.d("ApiClientAuth", "ApiClient Interceptor: Non-auth call. Current JWT in ApiClient: '$currentJwt'")
                if (!currentJwt.isNullOrBlank()) {
                    b.header("Authorization", "Bearer $currentJwt")
                    Log.i("ApiClientAuth", "ApiClient Interceptor: Authorization header ADDED: Bearer $currentJwt")
                } else {
                    Log.w("ApiClientAuth", "ApiClient Interceptor: Authorization header NOT ADDED (JWT in ApiClient is null or blank).")
                }
            }
            chain.proceed(b.build())
        }
        .addInterceptor(loggingInterceptor) // Logging Interceptor hinzufügen
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
