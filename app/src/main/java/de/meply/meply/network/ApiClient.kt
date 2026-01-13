package de.meply.meply.network // Dein Package-Name

import android.util.Log // Sicherstellen, dass der Import da ist
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://admin.meeplemates.de/api/"
    const val STRAPI_IMAGE_BASE = "https://admin.meeplemates.de"
    private const val APP_JWT = "6c7f20141f6312b426b587a0d40a469cb72f85f3b2e51e750d86b7700f75f03d2de6097882b576d3f700e31b0a6baa67b2e4e9e182509d8f2242fd8432635ed204badad96a09c5775b9f1d63ae1851ad86d8a62cfe08d613cea65d3431f80f57df3836a5c80909bb87ea5f19db85cd4317c5803c28206ef2d966c60cd5b041eb"
    // API_TOKEN wird aktuell nicht verwendet, ggf. später relevant
    // private const val API_TOKEN = "7352c93f31e79eab4f2bb18404507a5a42a163e6b3f24a7bce10b552bb927c301b191dc92fccabfa58c57191c33afa9a263bdec073d24562734ee67776b374eee0c0fb6220be872403aed4fad711ec160a36a481f648a73622c11e3d41349bf7d991d77c837b398541d649ece4c2cc74f2aa29b6abe1f9386613e755a6160ffa"
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

            // Logging für jede Anfrage im Auth Interceptor
            Log.d("ApiClientAuth", "ApiClient Interceptor: Request URL: ${req.url}")
            Log.d("ApiClientAuth", "ApiClient Interceptor: Is Auth call (login/register): $isLoginOrRegister")

            if (isLoginOrRegister) {
                // Keine Aktion für Login- oder Registrierungs-Aufrufe, da kein JWT benötigt wird
                b.header("Authorization", "Bearer $APP_JWT")
                Log.d("ApiClientAuth", "ApiClient Interceptor: Auth call, no Authorization header added.")
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
