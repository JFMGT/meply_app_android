package de.meply.meply.data

data class AuthResponse(
    val jwt: String,
    val user: User
)
