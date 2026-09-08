package com.example.data.remote.dto

import com.google.firebase.Timestamp

/**
 * Cloud Firestore User Profile Document (users/{uid})
 */
data class FirestoreUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val role: String = "HEADMASTER",
    val schoolId: String = "", // U-DISE Code as School Unique ID
    val schoolName: String = "",
    val active: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

/**
 * Cloud Firestore School Document (schools/{udiseCode})
 */
data class FirestoreSchool(
    val schoolId: String = "", // 11-digit UDISE Code
    val udiseCode: String = "",
    val schoolName: String = "",
    val address: String = "",
    val state: String = "Chhattisgarh",
    val district: String = "",
    val block: String = "",
    val cluster: String = "",
    val village: String = "",
    val schoolType: String = "",
    val headTeacherName: String = "",
    val headTeacherMobile: String = "",
    val active: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

/**
 * Cloud Firestore Daily Meal Record Document (schools/{udiseCode}/daily_meals/{recordId})
 * Note: Does not track individual students. Only records daily aggregated numbers.
 */
data class FirestoreDailyMeal(
    val recordId: String = "", // "{udiseCode}_{date}"
    val schoolId: String = "",
    val date: String = "", // YYYY-MM-DD
    val academicYear: String = "2026-27",
    val calendarStatus: String = "WORKING_DAY",
    val holidayReason: String = "",
    val enrolledStudents: Int = 0,
    val studentsPresent: Int = 0,
    val mealServed: Boolean = true,
    val studentMealCount: Int = 0, // Total number of students served MDM
    val boysServed: Int = 0,
    val girlsServed: Int = 0,
    val menu: String = "",
    val cooksPresentCount: Int = 0,
    val tastingDone: Boolean = true,
    val tastedBy: String = "",
    val tasteQuality: String = "GOOD",
    val hygieneChecked: Boolean = true,
    val photoUri: String = "",
    val riceConsumedKg: Double = 0.0,
    val remarks: String = "",
    val createdBy: String = "",
    val updatedBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

/**
 * Cloud Firestore Cook Document (schools/{udiseCode}/cooks/{cookId})
 */
data class FirestoreCook(
    val cookId: String = "",
    val schoolId: String = "",
    val name: String = "",
    val guardianName: String = "",
    val address: String = "",
    val mobile: String = "",
    val bankAccountNumber: String = "",
    val ifsc: String = "",
    val branchName: String = "",
    val bankName: String = "",
    val joiningDate: String = "",
    val active: Boolean = true,
    val associatedAgency: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

/**
 * Cloud Firestore Cook Attendance Document (schools/{udiseCode}/cook_attendances/{attendanceId})
 */
data class FirestoreCookAttendance(
    val attendanceId: String = "", // "{cookId}_{date}"
    val cookId: String = "",
    val schoolId: String = "",
    val date: String = "", // YYYY-MM-DD
    val status: String = "PRESENT", // PRESENT, ABSENT, LEAVE
    val absenceReason: String = "",
    val remarks: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
