package de.meply.meply

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.meply.meply.data.RegisterRequest
import de.meply.meply.data.RegisterResponse
import de.meply.meply.network.ApiClient
import de.meply.meply.network.RateLimitChecker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var passwordConfirmInput: EditText
    private lateinit var registrationCodeInput: EditText
    private lateinit var privacyCheckbox: CheckBox
    private lateinit var registerButton: Button
    private lateinit var errorText: TextView
    private lateinit var successText: TextView
    private lateinit var backToLoginLink: TextView

    companion object {
        private const val TAG = "RegisterActivity"
        // Username validation: 3-20 characters, letters, numbers, _ and -
        private val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_-]{3,20}$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        passwordConfirmInput = findViewById(R.id.passwordConfirmInput)
        registrationCodeInput = findViewById(R.id.registrationCodeInput)
        privacyCheckbox = findViewById(R.id.privacyCheckbox)
        registerButton = findViewById(R.id.registerButton)
        errorText = findViewById(R.id.errorText)
        successText = findViewById(R.id.successText)
        backToLoginLink = findViewById(R.id.backToLoginLink)

        // Back to login link
        backToLoginLink.setOnClickListener {
            finish()
        }

        // Register button
        registerButton.setOnClickListener {
            performRegistration()
        }
    }

    private fun performRegistration() {
        // Reset error/success states
        errorText.text = ""
        successText.visibility = android.view.View.GONE

        // Get input values
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val passwordConfirm = passwordConfirmInput.text.toString()
        val registrationCode = registrationCodeInput.text.toString().trim()
        val privacyAccepted = privacyCheckbox.isChecked

        // Validation
        if (!privacyAccepted) {
            errorText.text = "Bitte bestätige die Datenschutzerklärung."
            return
        }

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || registrationCode.isEmpty()) {
            errorText.text = "Bitte alle Pflichtfelder ausfüllen."
            return
        }

        if (!USERNAME_PATTERN.matches(username)) {
            errorText.text = "Ungültiger Benutzername (3-20 Zeichen, nur Buchstaben, Zahlen, _ und -)"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText.text = "Ungültige E-Mail-Adresse."
            return
        }

        if (password.length < 8) {
            errorText.text = "Passwort muss mindestens 8 Zeichen lang sein."
            return
        }

        if (password != passwordConfirm) {
            errorText.text = "Die Passwörter stimmen nicht überein."
            return
        }

        // Disable button during request
        registerButton.isEnabled = false
        registerButton.text = "Wird geprüft..."

        // Check rate limit before registration
        RateLimitChecker.checkRegister { result ->
            runOnUiThread {
                if (result.blocked) {
                    registerButton.isEnabled = true
                    registerButton.text = "Registrieren"
                    val waitText = result.waitMinutes?.let { " Bitte warte ca. $it Minute(n)." } ?: ""
                    errorText.text = "Zu viele Registrierungsversuche.$waitText"
                    return@runOnUiThread
                }

                // Rate limit OK - proceed with registration
                registerButton.text = "Wird registriert..."
                executeRegistration(username, email, password, registrationCode, privacyAccepted)
            }
        }
    }

    private fun executeRegistration(
        username: String,
        email: String,
        password: String,
        registrationCode: String,
        privacyAccepted: Boolean
    ) {
        // Create request
        val request = RegisterRequest(
            username = username,
            email = email,
            password = password,
            registrationCode = registrationCode,
            privacyAccepted = privacyAccepted
        )

        // Make API call
        val call = ApiClient.retrofit.register(request)
        call.enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                registerButton.isEnabled = true
                registerButton.text = "Registrieren"

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        // Success
                        Log.d(TAG, "Registration successful for user: ${body.user?.id}")
                        showSuccess("Registrierung erfolgreich! Bitte überprüfe dein E-Mail-Postfach (auch den Spam-Ordner), um deinen Account zu bestätigen.")

                        // Clear form
                        clearForm()
                    } else {
                        // API returned success=false
                        val errorMsg = body?.message ?: "Registrierung fehlgeschlagen."
                        Log.e(TAG, "Registration failed: $errorMsg")
                        errorText.text = errorMsg
                    }
                } else {
                    // HTTP error
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Registration HTTP error: ${response.code()} - $errorBody")

                    // Try to parse error message
                    val errorMsg = try {
                        val gson = com.google.gson.Gson()
                        val errorResponse = gson.fromJson(errorBody, RegisterResponse::class.java)
                        errorResponse?.message ?: "Registrierung fehlgeschlagen. Bitte überprüfe deine Angaben."
                    } catch (e: Exception) {
                        "Registrierung fehlgeschlagen. Bitte überprüfe deine Angaben."
                    }
                    errorText.text = errorMsg
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                registerButton.isEnabled = true
                registerButton.text = "Registrieren"
                Log.e(TAG, "Registration network error", t)
                errorText.text = "Netzwerkfehler: ${t.message}"
            }
        })
    }

    private fun showSuccess(message: String) {
        errorText.text = ""
        successText.text = message
        successText.visibility = android.view.View.VISIBLE
    }

    private fun clearForm() {
        usernameInput.text.clear()
        emailInput.text.clear()
        passwordInput.text.clear()
        passwordConfirmInput.text.clear()
        registrationCodeInput.text.clear()
        privacyCheckbox.isChecked = false
    }
}
