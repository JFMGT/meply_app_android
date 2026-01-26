package de.meply.meply.network

import android.util.Log
import de.meply.meply.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

/**
 * Rate Limit Checker - communicates with seclogin.meply.de
 * to check if login/register attempts are allowed.
 */
object RateLimitChecker {

    private const val TAG = "RateLimitChecker"
    private val client = OkHttpClient()

    data class RateLimitResult(
        val allowed: Boolean,
        val blocked: Boolean = false,
        val reason: String? = null,
        val waitMinutes: Int? = null,
        val error: String? = null
    )

    /**
     * Check rate limit for login attempts.
     * Limit: 20 attempts per 5 minutes (300 seconds)
     */
    fun checkLogin(callback: (RateLimitResult) -> Unit) {
        checkRateLimit("login", 20, 300, callback)
    }

    /**
     * Check rate limit for registration attempts.
     * Limit: 5 attempts per 1 hour (3600 seconds)
     */
    fun checkRegister(callback: (RateLimitResult) -> Unit) {
        checkRateLimit("register", 5, 3600, callback)
    }

    /**
     * Main rate limit check function.
     * Calls the seclogin.meply.de service with a signed request.
     */
    private fun checkRateLimit(
        scope: String,
        limit: Int,
        windowSec: Int,
        callback: (RateLimitResult) -> Unit
    ) {
        val secret = BuildConfig.SECLOGIN_SECRET
        val baseUrl = BuildConfig.SECLOGIN_URL

        // Skip rate limiting if secret is not configured (DEV mode)
        if (secret.isEmpty() || secret.startsWith("HIER_")) {
            Log.d(TAG, "Rate limit skipped: SECLOGIN_SECRET not configured")
            callback(RateLimitResult(allowed = true))
            return
        }

        try {
            // Build request body (IP will be detected by server from request)
            val jsonBody = JSONObject().apply {
                put("scope", scope)
                put("limit", limit)
                put("windowSec", windowSec)
            }
            val body = jsonBody.toString()

            // Create HMAC-SHA256 signature
            val timestamp = System.currentTimeMillis() / 1000
            val signatureData = "$timestamp.$body"
            val signature = hmacSha256(signatureData, secret)

            // Build HTTP request
            val requestBody = body.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/precheck/rate-limit")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("X-Timestamp", timestamp.toString())
                .header("X-Signature", signature)
                .build()

            // Execute async
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Rate limit check failed: ${e.message}")
                    // On network error, allow the attempt (fail-open for usability)
                    callback(RateLimitResult(allowed = true, error = e.message))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        val responseBody = resp.body?.string()
                        Log.d(TAG, "Rate limit response: ${resp.code} - $responseBody")

                        when (resp.code) {
                            200 -> {
                                // Parse response to check action
                                val result = parseSuccessResponse(responseBody)
                                callback(result)
                            }
                            429 -> {
                                // Rate limit exceeded
                                val result = parse429Response(responseBody)
                                callback(result)
                            }
                            else -> {
                                // Server error - fail open
                                Log.e(TAG, "Unexpected response: ${resp.code}")
                                callback(RateLimitResult(allowed = true, error = "Server error: ${resp.code}"))
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Rate limit check exception: ${e.message}")
            callback(RateLimitResult(allowed = true, error = e.message))
        }
    }

    private fun parseSuccessResponse(responseBody: String?): RateLimitResult {
        if (responseBody.isNullOrEmpty()) {
            return RateLimitResult(allowed = true)
        }

        return try {
            val json = JSONObject(responseBody)
            val action = json.optString("action", "allow")

            if (action == "block") {
                val reason = json.optString("reason", "rate-limit")
                val until = json.optLong("until", 0)
                val waitMinutes = if (until > 0) {
                    val now = System.currentTimeMillis() / 1000
                    ceil((until - now) / 60.0).toInt().coerceAtLeast(1)
                } else null

                RateLimitResult(
                    allowed = false,
                    blocked = true,
                    reason = reason,
                    waitMinutes = waitMinutes
                )
            } else {
                RateLimitResult(allowed = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing success response: ${e.message}")
            RateLimitResult(allowed = true)
        }
    }

    private fun parse429Response(responseBody: String?): RateLimitResult {
        var waitMinutes: Int? = null

        if (!responseBody.isNullOrEmpty()) {
            try {
                val json = JSONObject(responseBody)
                val until = json.optLong("until", 0)
                if (until > 0) {
                    val now = System.currentTimeMillis() / 1000
                    waitMinutes = ceil((until - now) / 60.0).toInt().coerceAtLeast(1)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 429 response: ${e.message}")
            }
        }

        return RateLimitResult(
            allowed = false,
            blocked = true,
            reason = "rate-limit",
            waitMinutes = waitMinutes
        )
    }

    /**
     * Create HMAC-SHA256 signature
     */
    private fun hmacSha256(data: String, secret: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), algorithm)
        mac.init(keySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
