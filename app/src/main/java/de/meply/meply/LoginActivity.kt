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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Auto-Login: JWT vorhanden? Direkt weiter.
        AuthManager.getJwt(this)?.let {
            goToHome("Nutzer")   // Falls du den echten Namen brauchst, kannst du ihn später mit /users/me holen.
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        errorText = findViewById(R.id.errorText)

        loginButton.setOnClickListener {
            val identifier = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                errorText.text = "Bitte alle Felder ausfüllen."
                return@setOnClickListener
            }

            val request = LoginRequest(identifier, password)
            val call = ApiClient.retrofit.login(request)
            call.enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val jwt = body?.jwt
                        val username = body?.user?.username ?: "Unbekannt"

                        if (jwt.isNullOrBlank()) {
                            Log.e("LoginActivityAuth", "Login-Antwort erfolgreich, aber JWT ist null oder leer.")
                            errorText.text = "Kein Token erhalten."
                            return
                        }

                        Log.d("LoginActivityAuth", "Login erfolgreich! Erhaltener JWT: '$jwt'")

                        // ✅ JWT im AuthManager für Persistenz speichern (z.B. SharedPreferences)
                        AuthManager.saveJwt(this@LoginActivity, jwt)

                        // ✅ JWT AUCH im ApiClient für den aktuellen Laufzeitgebrauch im Interceptor setzen
                        ApiClient.setJwt(jwt)
                        Log.d(TAG, "Manueller Login: Nach ApiClient.setJwt. Aktueller JWT im ApiClient: '${ApiClient.getCurrentJwt()}'")


                        Log.d("LoginActivityAuth", "JWT wurde an ApiClient übergeben. Starte HomeActivity.")

                        // ✅ Weiterleiten
                        goToHome(username)
                        finish()
                    } else {
                        val errMsg = response.errorBody()?.string().orEmpty()
                        Log.e("LoginActivityAuth", "Login API-Fehler: ${response.code()} - $errMsg")
                        errorText.text = "Login fehlgeschlagen (${response.code()}): $errMsg"
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    errorText.text = "Netzwerkfehler: ${t.message}"
                }
            })
        }
    }

    private fun goToHome(username: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("USERNAME", username)
        startActivity(intent)
    }
}
