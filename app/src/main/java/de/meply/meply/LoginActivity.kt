package de.meply.meply

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.meply.meply.HomeActivity
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.AuthResponse
import de.meply.meply.data.LoginRequest
import de.meply.meply.network.ApiClient
import de.meply.meply.network.RateLimitChecker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.view.View

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var errorText: TextView
    private lateinit var registerLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Auto-Login: JWT vorhanden? Direkt weiter.
        AuthManager.getJwt(this)?.let { jwt ->
            // ✅ JWT auch im ApiClient setzen für API-Calls
            ApiClient.setJwt(jwt)

            // Fetch profile documentId if not already saved
            if (AuthManager.getProfileDocumentId(this) == null) {
                fetchAndSaveProfileId("Nutzer")
            } else {
                goToHome("Nutzer")
                finish()
            }
            return
        }

        setContentView(R.layout.activity_login)

        // Show DEV badge for debug builds
        if (BuildConfig.DEBUG) {
            findViewById<TextView>(R.id.devBadge).visibility = View.VISIBLE
        }

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        errorText = findViewById(R.id.errorText)
        registerLink = findViewById(R.id.registerLink)

        // Register link click handler
        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val identifier = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                errorText.text = "Bitte alle Felder ausfüllen."
                return@setOnClickListener
            }

            // Disable button during rate limit check
            loginButton.isEnabled = false
            errorText.text = ""

            // Check rate limit before login
            RateLimitChecker.checkLogin { result ->
                runOnUiThread {
                    if (result.blocked) {
                        loginButton.isEnabled = true
                        val waitText = result.waitMinutes?.let { " Bitte in ca. $it Minute(n) erneut versuchen." } ?: ""
                        errorText.text = "Zu viele Anmeldeversuche.$waitText"
                        return@runOnUiThread
                    }

                    // Rate limit OK - proceed with login
                    performLogin(identifier, password)
                }
            }
        }
    }

    private fun performLogin(identifier: String, password: String) {
        val request = LoginRequest(identifier, password)
        val call = ApiClient.retrofit.login(request)
        call.enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                loginButton.isEnabled = true
                if (response.isSuccessful) {
                    val body = response.body()
                    val jwt = body?.jwt
                    val username = body?.user?.username ?: "Unbekannt"

                    if (jwt.isNullOrBlank()) {
                        errorText.text = "Kein Token erhalten."
                        return
                    }

                    // JWT im AuthManager für Persistenz speichern
                    AuthManager.saveJwt(this@LoginActivity, jwt)

                    // JWT auch im ApiClient für den aktuellen Laufzeitgebrauch setzen
                    ApiClient.setJwt(jwt)

                    // Fetch and save profile documentId
                    fetchAndSaveProfileId(username)
                } else {
                    val errMsg = response.errorBody()?.string().orEmpty()
                    Log.e("LoginActivityAuth", "Login API-Fehler: ${response.code()} - $errMsg")
                    errorText.text = "Login fehlgeschlagen (${response.code()}): $errMsg"
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                loginButton.isEnabled = true
                errorText.text = "Netzwerkfehler: ${t.message}"
            }
        })
    }

    private fun fetchAndSaveProfileId(username: String) {
        val call = ApiClient.retrofit.getCurrentUser()
        call.enqueue(object : Callback<de.meply.meply.network.ApiService.UserMe> {
            override fun onResponse(
                call: Call<de.meply.meply.network.ApiService.UserMe>,
                response: Response<de.meply.meply.network.ApiService.UserMe>
            ) {
                if (response.isSuccessful) {
                    val user = response.body()
                    val profileDocumentId = user?.profile?.documentId
                    if (profileDocumentId != null) {
                        AuthManager.saveProfileDocumentId(this@LoginActivity, profileDocumentId)
                        Log.d("LoginActivityAuth", "Profile documentId saved: $profileDocumentId")
                    }
                    // Save numeric profile ID
                    val profileId = user?.profile?.id?.toIntOrNull()
                    if (profileId != null) {
                        AuthManager.saveProfileId(this@LoginActivity, profileId)
                        Log.d("LoginActivityAuth", "Profile ID saved: $profileId")
                    }
                    val userDocumentId = user?.documentId
                    if (userDocumentId != null) {
                        AuthManager.saveUserDocumentId(this@LoginActivity, userDocumentId)
                        Log.d("LoginActivityAuth", "User documentId saved: $userDocumentId")
                    }
                } else {
                    Log.e("LoginActivityAuth", "Failed to fetch user profile: ${response.code()}")
                }
                // Navigate to home regardless of profile fetch result
                goToHome(username)
                finish()
            }

            override fun onFailure(
                call: Call<de.meply.meply.network.ApiService.UserMe>,
                t: Throwable
            ) {
                Log.e("LoginActivityAuth", "Error fetching user profile", t)
                // Navigate to home even if profile fetch fails
                goToHome(username)
                finish()
            }
        })
    }

    private fun goToHome(username: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("USERNAME", username)
        startActivity(intent)
    }
}
