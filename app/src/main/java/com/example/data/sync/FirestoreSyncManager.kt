package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.auth.AuthSessionManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CookAttendanceEntity
import com.example.data.local.entity.CookEntity
import com.example.data.local.entity.DailyMealRecordEntity
import com.example.data.local.entity.SchoolEntity
import com.example.data.remote.dto.FirestoreCook
import com.example.data.remote.dto.FirestoreCookAttendance
import com.example.data.remote.dto.FirestoreDailyMeal
import com.example.data.remote.dto.FirestoreSchool
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    SYNCING,
    SYNC_FAILED,
    OFFLINE
}

data class SyncState(
    val status: SyncStatus = SyncStatus.SYNCED,
    val pendingCount: Int = 0,
    val lastSyncTime: Long = 0L,
    val lastSyncFormatted: String = "कभी नहीं (Never)",
    val errorMessage: String? = null
)

class FirestoreSyncManager(
    private val context: Context,
    private val db: AppDatabase,
    private val authSessionManager: AuthSessionManager = AuthSessionManager(context)
) {
    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore not available: ${e.message}")
            null
        }
    }

    private val _syncState = MutableStateFlow(
        SyncState(
            lastSyncTime = authSessionManager.getLastSyncTimestamp(),
            lastSyncFormatted = formatTimestamp(authSessionManager.getLastSyncTimestamp())
        )
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun syncPendingData(): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore
        val udiseCode = authSessionManager.getSchoolUdise().ifBlank {
            db.schoolDao().getSchoolDirect()?.udiseCode ?: "22010100101"
        }

        if (!isOnline() || fs == null) {
            _syncState.value = _syncState.value.copy(
                status = if (!isOnline()) SyncStatus.OFFLINE else SyncStatus.SYNC_FAILED,
                errorMessage = if (!isOnline()) "ऑफ़लाइन मोड (Offline mode)" else "क्लाउड सेवा अनुपलब्ध (Cloud unavailable)"
            )
            return@withContext false
        }

        try {
            _syncState.value = _syncState.value.copy(status = SyncStatus.SYNCING, errorMessage = null)

            // 1. Sync School Master Details
            val localSchool = db.schoolDao().getSchoolDirect()
            if (localSchool != null) {
                val cloudSchool = FirestoreSchool(
                    schoolId = localSchool.udiseCode,
                    udiseCode = localSchool.udiseCode,
                    schoolName = localSchool.schoolName,
                    address = "${localSchool.villageName}, ${localSchool.clusterName}",
                    district = localSchool.districtName,
                    block = localSchool.blockName,
                    cluster = localSchool.clusterName,
                    village = localSchool.villageName,
                    schoolType = localSchool.schoolType,
                    headTeacherName = localSchool.headTeacherName,
                    headTeacherMobile = localSchool.headTeacherMobile,
                    active = true,
                    updatedAt = Timestamp.now()
                )
                fs.collection("schools").document(localSchool.udiseCode)
                    .set(cloudSchool, SetOptions.merge()).await()
            }

            // 2. Sync Daily Meal Records (Only aggregate counts, no individual student tracking)
            val allMeals = db.dailyMealRecordDao().getAllRecordsDirect()
            val pendingMeals = allMeals.filter { it.syncStatus != "SYNCED" }

            for (meal in pendingMeals) {
                val recordId = "${udiseCode}_${meal.date}"
                val firestoreMeal = FirestoreDailyMeal(
                    recordId = recordId,
                    schoolId = udiseCode,
                    date = meal.date,
                    academicYear = meal.academicYear,
                    calendarStatus = meal.calendarStatus,
                    holidayReason = meal.holidayReason,
                    enrolledStudents = meal.enrolledStudents,
                    studentsPresent = meal.studentsPresent,
                    mealServed = meal.mealServed,
                    studentMealCount = meal.studentsServed,
                    boysServed = meal.boysServed,
                    girlsServed = meal.girlsServed,
                    menu = meal.menuDetails,
                    cooksPresentCount = meal.cooksPresentCount,
                    tastingDone = meal.tastingDone,
                    tastedBy = meal.tastedBy,
                    tasteQuality = meal.tasteQuality,
                    hygieneChecked = meal.hygieneChecked,
                    photoUri = meal.photoUri,
                    riceConsumedKg = meal.riceConsumedKg,
                    remarks = meal.holidayReason,
                    createdBy = authSessionManager.getCurrentUserId(),
                    updatedBy = authSessionManager.getCurrentUserId(),
                    updatedAt = Timestamp.now()
                )

                fs.collection("schools").document(udiseCode)
                    .collection("daily_meals").document(recordId)
                    .set(firestoreMeal, SetOptions.merge()).await()

                // Mark local record as SYNCED
                db.dailyMealRecordDao().insertOrUpdateRecord(meal.copy(syncStatus = "SYNCED"))
            }

            // 3. Sync Cooks
            val cooks = db.cookDao().getAllCooksDirect()
            for (cook in cooks) {
                val firestoreCook = FirestoreCook(
                    cookId = cook.cookId,
                    schoolId = udiseCode,
                    name = cook.name,
                    guardianName = cook.guardianName,
                    address = cook.address,
                    mobile = cook.mobile,
                    bankAccountNumber = cook.bankAccountNo,
                    ifsc = cook.ifscCode,
                    branchName = cook.branchName,
                    bankName = cook.bankName,
                    joiningDate = cook.joiningDate,
                    active = cook.status == "ACTIVE",
                    associatedAgency = cook.associatedAgency,
                    updatedAt = Timestamp.now()
                )
                fs.collection("schools").document(udiseCode)
                    .collection("cooks").document(cook.cookId)
                    .set(firestoreCook, SetOptions.merge()).await()
            }

            // 4. Sync Cook Attendances
            val attendances = db.cookAttendanceDao().getAllAttendancesDirect()
            for (att in attendances) {
                val attId = "${att.cookId}_${att.date}"
                val firestoreAtt = FirestoreCookAttendance(
                    attendanceId = attId,
                    cookId = att.cookId,
                    schoolId = udiseCode,
                    date = att.date,
                    status = if (att.isPresent) "PRESENT" else "ABSENT",
                    absenceReason = att.absenceReason,
                    remarks = att.remarks,
                    updatedAt = Timestamp.now()
                )
                fs.collection("schools").document(udiseCode)
                    .collection("cook_attendances").document(attId)
                    .set(firestoreAtt, SetOptions.merge()).await()
            }

            val now = System.currentTimeMillis()
            authSessionManager.setLastSyncTimestamp(now)
            _syncState.value = SyncState(
                status = SyncStatus.SYNCED,
                pendingCount = 0,
                lastSyncTime = now,
                lastSyncFormatted = formatTimestamp(now),
                errorMessage = null
            )
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Sync failure", e)
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.SYNC_FAILED,
                errorMessage = "सिंक त्रुटि: ${e.localizedMessage ?: "क्लाउड कनेक्शन विफल"}"
            )
            false
        }
    }

    /**
     * Download authorized cloud data to Room when a user installs the app on a new device.
     */
    suspend fun initialPullFromCloud(udiseCode: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        if (!isOnline()) return@withContext false

        try {
            _syncState.value = _syncState.value.copy(status = SyncStatus.SYNCING)

            // 1. Pull School Document
            val schoolSnap = fs.collection("schools").document(udiseCode).get().await()
            if (schoolSnap.exists()) {
                val cloudSchool = schoolSnap.toObject(FirestoreSchool::class.java)
                if (cloudSchool != null) {
                    db.schoolDao().insertOrUpdateSchool(
                        SchoolEntity(
                            schoolId = cloudSchool.schoolId.ifBlank { "SCH-$udiseCode" },
                            udiseCode = cloudSchool.udiseCode.ifBlank { udiseCode },
                            schoolName = cloudSchool.schoolName,
                            stateName = cloudSchool.state,
                            districtName = cloudSchool.district,
                            blockName = cloudSchool.block,
                            clusterName = cloudSchool.cluster,
                            villageName = cloudSchool.village,
                            schoolType = cloudSchool.schoolType,
                            headTeacherName = cloudSchool.headTeacherName,
                            headTeacherMobile = cloudSchool.headTeacherMobile,
                            status = if (cloudSchool.active) "ACTIVE" else "INACTIVE"
                        )
                    )
                }
            }

            // 2. Pull Daily Meal Records
            val mealsSnap = fs.collection("schools").document(udiseCode)
                .collection("daily_meals").get().await()

            for (doc in mealsSnap.documents) {
                val m = doc.toObject(FirestoreDailyMeal::class.java) ?: continue
                db.dailyMealRecordDao().insertOrUpdateRecord(
                    DailyMealRecordEntity(
                        date = m.date,
                        schoolId = m.schoolId.ifBlank { udiseCode },
                        academicYear = m.academicYear,
                        calendarStatus = m.calendarStatus,
                        holidayReason = m.holidayReason,
                        enrolledStudents = m.enrolledStudents,
                        studentsPresent = m.studentsPresent,
                        mealServed = m.mealServed,
                        boysServed = m.boysServed,
                        girlsServed = m.girlsServed,
                        studentsServed = m.studentMealCount,
                        menuDetails = m.menu,
                        cooksPresentCount = m.cooksPresentCount,
                        tastingDone = m.tastingDone,
                        tastedBy = m.tastedBy,
                        tasteQuality = m.tasteQuality,
                        hygieneChecked = m.hygieneChecked,
                        photoUri = m.photoUri,
                        riceConsumedKg = m.riceConsumedKg,
                        syncStatus = "SYNCED"
                    )
                )
            }

            // 3. Pull Cooks
            val cooksSnap = fs.collection("schools").document(udiseCode)
                .collection("cooks").get().await()

            for (doc in cooksSnap.documents) {
                val c = doc.toObject(FirestoreCook::class.java) ?: continue
                db.cookDao().insertOrUpdateCook(
                    CookEntity(
                        cookId = c.cookId,
                        schoolId = c.schoolId.ifBlank { udiseCode },
                        name = c.name,
                        guardianName = c.guardianName,
                        address = c.address,
                        mobile = c.mobile,
                        bankAccountNo = c.bankAccountNumber,
                        ifscCode = c.ifsc,
                        branchName = c.branchName,
                        bankName = c.bankName,
                        joiningDate = c.joiningDate,
                        status = if (c.active) "ACTIVE" else "INACTIVE",
                        associatedAgency = c.associatedAgency
                    )
                )
            }

            // 4. Pull Cook Attendances
            val attSnap = fs.collection("schools").document(udiseCode)
                .collection("cook_attendances").get().await()

            for (doc in attSnap.documents) {
                val a = doc.toObject(FirestoreCookAttendance::class.java) ?: continue
                val localCook = db.cookDao().getCookById(a.cookId)
                db.cookAttendanceDao().insertAttendances(
                    listOf(
                        CookAttendanceEntity(
                            date = a.date,
                            cookId = a.cookId,
                            cookName = localCook?.name ?: "Cook",
                            isPresent = a.status == "PRESENT",
                            absenceReason = a.absenceReason,
                            remarks = a.remarks,
                            schoolId = a.schoolId.ifBlank { udiseCode }
                        )
                    )
                )
            }

            val now = System.currentTimeMillis()
            authSessionManager.setLastSyncTimestamp(now)
            _syncState.value = SyncState(
                status = SyncStatus.SYNCED,
                pendingCount = 0,
                lastSyncTime = now,
                lastSyncFormatted = formatTimestamp(now),
                errorMessage = null
            )
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Initial cloud pull error", e)
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.SYNC_FAILED,
                errorMessage = "क्लाउड डेटा डाउनलोड विफल: ${e.localizedMessage ?: "त्रुटि"}"
            )
            false
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "कभी नहीं (Never)"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN"))
        return sdf.format(Date(timestamp))
    }
}
