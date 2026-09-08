package com.example.auth

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.remote.dto.FirestoreSchool
import com.example.data.remote.dto.FirestoreUser
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class FirebaseAuthRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val authSessionManager: AuthSessionManager = AuthSessionManager(context)
) {
    private val firebaseAuth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseAuthRepo", "Firebase Auth not initialized: ${e.message}")
            null
        }
    }

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseAuthRepo", "Firestore not initialized: ${e.message}")
            null
        }
    }

    private val otpService = OtpAuthService.getInstance(context)

    suspend fun requestEmailOtp(email: String): OtpResult<SendOtpResponse> {
        return otpService.requestOtp(email)
    }

    suspend fun verifyOtpAndSignIn(email: String, otp: String): OtpResult<FirestoreUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val verifyResult = otpService.verifyOtp(cleanEmail, otp)

        if (verifyResult is OtpResult.Error) {
            return@withContext verifyResult
        }

        val verifyData = (verifyResult as OtpResult.Success).data
        val generatedUid = verifyData.uid ?: "user_${cleanEmail.hashCode()}"

        try {
            // 1. Authenticate with Firebase if instance is available
            var firebaseUid = generatedUid
            firebaseAuth?.let { auth ->
                try {
                    if (auth.currentUser == null) {
                        val authResult = auth.signInAnonymously().await()
                        firebaseUid = authResult.user?.uid ?: generatedUid
                    } else {
                        firebaseUid = auth.currentUser?.uid ?: generatedUid
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseAuthRepo", "Firebase sign in fallback: ${e.message}")
                }
            }

            // 2. Fetch or initialize User Profile in Firestore
            var userProfile: FirestoreUser? = null
            firestore?.let { fs ->
                try {
                    val userDoc = fs.collection("users").document(firebaseUid).get().await()
                    if (userDoc.exists()) {
                        userProfile = userDoc.toObject(FirestoreUser::class.java)
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseAuthRepo", "Firestore user query note: ${e.message}")
                }
            }

            // 3. Fallback to local Room School & User if not yet on cloud
            val localSchool = db.schoolDao().getSchoolDirect()
            val localUser = db.userDao().getHeadmasterDirect() ?: db.userDao().getAllUsersDirect().firstOrNull()

            val schoolId = if (!userProfile?.schoolId.isNullOrBlank()) userProfile?.schoolId!!
            else if (!localSchool?.udiseCode.isNullOrBlank()) localSchool?.udiseCode!!
            else "22010100101"

            val schoolName = if (!userProfile?.schoolName.isNullOrBlank()) userProfile?.schoolName!!
            else if (!localSchool?.schoolName.isNullOrBlank()) localSchool?.schoolName!!
            else "Govt. School"

            val fallbackName = cleanEmail.substringBefore("@").replace(".", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

            val userName = if (!userProfile?.name.isNullOrBlank()) userProfile?.name!!
            else if (!localSchool?.headTeacherName.isNullOrBlank()) localSchool?.headTeacherName!!
            else if (!localUser?.name.isNullOrBlank()) localUser?.name!!
            else fallbackName

            val userRole = if (!userProfile?.role.isNullOrBlank()) userProfile?.role!!
            else if (!localUser?.role.isNullOrBlank()) localUser?.role!!
            else "HEADMASTER"

            val resolvedProfile = FirestoreUser(
                uid = firebaseUid,
                name = userName,
                email = cleanEmail,
                mobile = localSchool?.headTeacherMobile ?: "",
                role = userRole,
                schoolId = schoolId,
                schoolName = schoolName,
                active = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // 4. Upsert user profile to Firestore
            firestore?.let { fs ->
                try {
                    fs.collection("users").document(firebaseUid)
                        .set(resolvedProfile, SetOptions.merge())
                } catch (e: Exception) {
                    Log.w("FirebaseAuthRepo", "Firestore upsert note: ${e.message}")
                }
            }

            // 5. Update local session manager
            authSessionManager.saveCloudSession(
                firebaseUid = firebaseUid,
                email = cleanEmail,
                role = userRole,
                schoolId = schoolId,
                schoolName = schoolName,
                userName = userName
            )

            OtpResult.Success(resolvedProfile)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepo", "Sign in error", e)
            OtpResult.Error("लॉगिन विफल: ${e.localizedMessage ?: "अज्ञात त्रुटि"}", OtpErrorCode.SERVER_ERROR)
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w("FirebaseAuthRepo", "Sign out error: ${e.message}")
        }
        authSessionManager.clearSession()
    }

    fun getCurrentUserUid(): String? {
        return firebaseAuth?.currentUser?.uid ?: authSessionManager.getFirebaseUid()
    }

    fun isUserLoggedIn(): Boolean {
        return authSessionManager.isLoggedIn()
    }
}
