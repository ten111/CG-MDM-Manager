package com.example.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendOtpRequest(
    @Json(name = "email") val email: String,
    @Json(name = "clientTimestamp") val clientTimestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SendOtpResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "cooldownSeconds") val cooldownSeconds: Int = 60,
    @Json(name = "expiresInSeconds") val expiresInSeconds: Int = 300,
    @Json(name = "sessionId") val sessionId: String = ""
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    @Json(name = "email") val email: String,
    @Json(name = "otp") val otp: String,
    @Json(name = "sessionId") val sessionId: String = ""
)

@JsonClass(generateAdapter = true)
data class VerifyOtpResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "firebaseCustomToken") val firebaseCustomToken: String? = null,
    @Json(name = "uid") val uid: String? = null,
    @Json(name = "schoolId") val schoolId: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "attemptsRemaining") val attemptsRemaining: Int? = null
)

sealed class OtpResult<out T> {
    data class Success<T>(val data: T) : OtpResult<T>()
    data class Error(val message: String, val code: OtpErrorCode = OtpErrorCode.UNKNOWN) : OtpResult<Nothing>()
    object Loading : OtpResult<Nothing>()
}

enum class OtpErrorCode {
    INVALID_EMAIL,
    RATE_LIMITED,
    OTP_EXPIRED,
    INVALID_OTP,
    TOO_MANY_ATTEMPTS,
    SERVER_ERROR,
    NETWORK_ERROR,
    UNKNOWN
}
