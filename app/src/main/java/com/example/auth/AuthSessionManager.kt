package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.UserRole

class AuthSessionManager(private val context: Context) {

    private fun getPrefs(): SharedPreferences = getPrefs(context)

    fun isRegistered(): Boolean = isRegistered(context)
    fun setRegistered(registered: Boolean) = setRegistered(context, registered)
    fun isLoggedIn(): Boolean = isLoggedIn(context)
    fun setLoggedIn(loggedIn: Boolean) = setLoggedIn(context, loggedIn)
    fun getFirebaseUid(): String? = getFirebaseUid(context)
    fun getCurrentUserId(): String = getCurrentUserId(context)
    fun getCurrentUserName(): String = getCurrentUserName(context)
    fun getCurrentUserEmail(): String = getCurrentUserEmail(context)
    fun getCurrentUserRole(): UserRole = getCurrentUserRole(context)
    fun getSchoolUdise(): String = getSchoolUdise(context)
    fun getSchoolName(): String = getSchoolName(context)
    fun getLastSyncTimestamp(): Long = getLastSyncTimestamp(context)
    fun setLastSyncTimestamp(timestamp: Long) = setLastSyncTimestamp(context, timestamp)

    fun saveCloudSession(
        firebaseUid: String,
        email: String,
        role: String,
        schoolId: String,
        schoolName: String,
        userName: String
    ) {
        saveCloudSession(context, firebaseUid, email, role, schoolId, schoolName, userName)
    }

    fun clearSession() {
        logout(context)
    }

    companion object {
        private const val PREFS_NAME = "poshan_auth_prefs"
        private const val KEY_IS_REGISTERED = "key_is_registered"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_FIREBASE_UID = "key_firebase_uid"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_USER_MOBILE = "key_user_mobile"
        private const val KEY_SCHOOL_UDISE = "key_school_udise"
        private const val KEY_SCHOOL_NAME = "key_school_name"
        private const val KEY_QUICK_PIN_ENABLED = "key_quick_pin_enabled"
        private const val KEY_LAST_LOGIN_TIMESTAMP = "key_last_login_timestamp"
        private const val KEY_LAST_SYNC_TIMESTAMP = "key_last_sync_timestamp"

        private fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun isRegistered(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_IS_REGISTERED, false)
        }

        fun setRegistered(context: Context, registered: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_IS_REGISTERED, registered).apply()
        }

        fun isLoggedIn(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
        }

        fun setLoggedIn(context: Context, loggedIn: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
        }

        fun getFirebaseUid(context: Context): String? {
            return getPrefs(context).getString(KEY_FIREBASE_UID, null)
        }

        fun getCurrentUserId(context: Context): String {
            return getPrefs(context).getString(KEY_USER_ID, "") ?: ""
        }

        fun getCurrentUserName(context: Context): String {
            return getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
        }

        fun getCurrentUserEmail(context: Context): String {
            return getPrefs(context).getString(KEY_USER_EMAIL, "") ?: ""
        }

        fun getCurrentUserRole(context: Context): UserRole {
            val roleCode = getPrefs(context).getString(KEY_USER_ROLE, UserRole.HEADMASTER.code) ?: UserRole.HEADMASTER.code
            return UserRole.fromCode(roleCode)
        }

        fun getCurrentUserMobile(context: Context): String {
            return getPrefs(context).getString(KEY_USER_MOBILE, "") ?: ""
        }

        fun getSchoolUdise(context: Context): String {
            return getPrefs(context).getString(KEY_SCHOOL_UDISE, "") ?: ""
        }

        fun getSchoolName(context: Context): String {
            return getPrefs(context).getString(KEY_SCHOOL_NAME, "") ?: ""
        }

        fun getLastSyncTimestamp(context: Context): Long {
            return getPrefs(context).getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        }

        fun setLastSyncTimestamp(context: Context, timestamp: Long) {
            getPrefs(context).edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
        }

        fun saveCloudSession(
            context: Context,
            firebaseUid: String,
            email: String,
            role: String,
            schoolId: String,
            schoolName: String,
            userName: String
        ) {
            getPrefs(context).edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_REGISTERED, true)
                .putString(KEY_FIREBASE_UID, firebaseUid)
                .putString(KEY_USER_ID, firebaseUid)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_SCHOOL_UDISE, schoolId)
                .putString(KEY_SCHOOL_NAME, schoolName)
                .putString(KEY_USER_NAME, userName)
                .putLong(KEY_LAST_LOGIN_TIMESTAMP, System.currentTimeMillis())
                .apply()
        }

        fun saveLoginSession(context: Context, user: UserAccountEntity) {
            getPrefs(context).edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, user.userId)
                .putString(KEY_USER_NAME, user.name)
                .putString(KEY_USER_ROLE, user.role)
                .putString(KEY_USER_MOBILE, user.mobile)
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_SCHOOL_UDISE, user.udiseCode)
                .putLong(KEY_LAST_LOGIN_TIMESTAMP, System.currentTimeMillis())
                .apply()
        }

        fun saveSchoolInfo(context: Context, udise: String, name: String) {
            getPrefs(context).edit()
                .putString(KEY_SCHOOL_UDISE, udise)
                .putString(KEY_SCHOOL_NAME, name)
                .apply()
        }

        fun updateCurrentUserName(context: Context, newName: String, newMobile: String = "") {
            val editor = getPrefs(context).edit().putString(KEY_USER_NAME, newName)
            if (newMobile.isNotBlank()) {
                editor.putString(KEY_USER_MOBILE, newMobile)
            }
            editor.apply()
        }

        fun logout(context: Context) {
            getPrefs(context).edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply()
        }

        fun isQuickPinEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_QUICK_PIN_ENABLED, true)
        }

        fun setQuickPinEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_QUICK_PIN_ENABLED, enabled).apply()
        }

        fun resetRegistration(context: Context) {
            getPrefs(context).edit().clear().apply()
        }
    }
}
