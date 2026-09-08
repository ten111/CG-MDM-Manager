package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val schoolId: String = "SCH-CG-RPR-001",
    val udiseCode: String = "22010100101",
    val schoolName: String = "Govt. Primary & Middle School, Raipur",
    val stateName: String = "Chhattisgarh",
    val districtName: String = "Raipur",
    val blockName: String = "Dharsiwa",
    val clusterName: String = "Birgaon",
    val villageName: String = "Urla",
    val schoolType: String = "Primary with Upper Primary (Class 1-8)",
    val headTeacherName: String = "Shri Rajesh Kumar Sharma",
    val headTeacherMobile: String = "9826012345",
    val status: String = "ACTIVE"
)

@Entity(tableName = "academic_years")
data class AcademicYearEntity(
    @PrimaryKey val yearId: String = "2026-27",
    val displayName: String = "2026-27",
    val startDate: String = "2026-04-01",
    val endDate: String = "2027-03-31",
    val isCurrent: Boolean = true
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventDate: String, // YYYY-MM-DD
    val eventName: String,
    val eventType: String = "HOLIDAY", // WORKING_DAY, HOLIDAY, VACATION, SPECIAL_WORKING_DAY, EMERGENCY_CLOSURE
    val isMealReportingRequired: Boolean = false,
    val orderNumber: String = "",
    val authority: String = "School Education Dept, Govt of Chhattisgarh",
    val remarks: String = ""
)

@Entity(tableName = "monthly_enrollments")
data class MonthlyEnrollmentEntity(
    @PrimaryKey val monthYear: String, // e.g., "2026-08"
    val schoolId: String = "SCH-CG-RPR-001",
    val academicYear: String = "2026-27",
    val totalEnrollment: Int,
    val totalBoys: Int,
    val totalGirls: Int,
    val scCount: Int,
    val stCount: Int,
    val obcCount: Int,
    val generalCount: Int,
    val cwsnCount: Int, // Children with Special Needs (Divyang)
    val minorityCount: Int,
    val pvtgCount: Int = 0, // Particularly Vulnerable Tribal Groups
    val classBreakupJson: String = "", // Detailed class 1-8 counts
    val isLocked: Boolean = false,
    val createdBy: String = "Head Teacher",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "monthly_teachers")
data class MonthlyTeacherEntity(
    @PrimaryKey val monthYear: String, // e.g., "2026-08"
    val schoolId: String = "SCH-CG-RPR-001",
    val academicYear: String = "2026-27",
    val totalTeachers: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val trainedCount: Int = 0,
    val untrainedCount: Int = 0,
    val scCount: Int = 0,
    val stCount: Int = 0,
    val obcCount: Int = 0,
    val generalCount: Int = 0,
    // Detailed Category-wise Male / Female / Trained / Untrained
    val stTrainedMale: Int = 0,
    val stTrainedFemale: Int = 0,
    val stUntrainedMale: Int = 0,
    val stUntrainedFemale: Int = 0,
    val scTrainedMale: Int = 0,
    val scTrainedFemale: Int = 0,
    val scUntrainedMale: Int = 0,
    val scUntrainedFemale: Int = 0,
    val obcTrainedMale: Int = 0,
    val obcTrainedFemale: Int = 0,
    val obcUntrainedMale: Int = 0,
    val obcUntrainedFemale: Int = 0,
    val genTrainedMale: Int = 0,
    val genTrainedFemale: Int = 0,
    val genUntrainedMale: Int = 0,
    val genUntrainedFemale: Int = 0,
    val isLocked: Boolean = false,
    val createdBy: String = "Head Teacher",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "cooks")
data class CookEntity(
    @PrimaryKey val cookId: String,
    val schoolId: String = "SCH-CG-RPR-001",
    val name: String,
    val guardianName: String = "",
    val address: String = "",
    val village: String = "",
    val mobile: String = "",
    val bankAccountNo: String = "",
    val maskedAccountNo: String = "",
    val bankName: String = "",
    val branchName: String = "",
    val ifscCode: String = "",
    val joiningDate: String = "2024-06-15",
    val status: String = "ACTIVE", // ACTIVE, INACTIVE, ON_LEAVE, LEFT
    val associatedAgency: String = "Maa Danteshwari SHG"
)

@Entity(tableName = "cook_attendances")
data class CookAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val cookId: String,
    val cookName: String,
    val isPresent: Boolean,
    val absenceReason: String = "", // Leave, Illness, Personal, Other
    val remarks: String = "",
    val schoolId: String = "SCH-CG-RPR-001"
)

@Entity(tableName = "cooking_agencies")
data class CookingAgencyEntity(
    @PrimaryKey val agencyId: String,
    val name: String,
    val contactPerson: String,
    val mobile: String,
    val address: String,
    val villageOrCity: String,
    val block: String = "Dharsiwa",
    val district: String = "Raipur",
    val maskedAccountNo: String = "XXXX XXXX 8912",
    val bankName: String = "State Bank of India",
    val branchName: String = "Urla Industrial Area",
    val ifscCode: String = "SBIN0004521",
    val agreementOrderNo: String = "CG/MDM/RPR/2025/112",
    val agreementStartDate: String = "2025-04-01",
    val agreementEndDate: String = "2027-03-31",
    val status: String = "ACTIVE", // ACTIVE, EXPIRING_SOON, EXPIRED
    val associatedSchools: String = "Govt. PS Urla, Govt. MS Urla"
)

@Entity(tableName = "pds_shops")
data class PdsShopEntity(
    @PrimaryKey val pdsId: String,
    val fpsNumber: String, // Fair Price Shop number
    val shopName: String,
    val dealerName: String,
    val mobile: String,
    val address: String,
    val village: String,
    val block: String = "Dharsiwa",
    val district: String = "Raipur",
    val licenseNumber: String = "FPS/RPR/UR/849",
    val associatedSchool: String = "Govt. Primary & Middle School, Raipur",
    val status: String = "ACTIVE"
)

@Entity(tableName = "rice_receipts")
data class RiceReceiptEntity(
    @PrimaryKey val receiptId: String,
    val schoolId: String = "SCH-CG-RPR-001",
    val pdsId: String,
    val pdsShopName: String,
    val receiptDate: String, // YYYY-MM-DD
    val quantityKg: Double,
    val challanNumber: String,
    val govtRefNumber: String = "",
    val remarks: String = "",
    val photoUri: String = "",
    val receivedBy: String = "Head Teacher",
    val status: String = "VERIFIED"
)

@Entity(tableName = "stock_transactions")
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true) val transactionId: Long = 0,
    val schoolId: String = "SCH-CG-RPR-001",
    val transactionDate: String, // YYYY-MM-DD
    val itemType: String = "RICE", // RICE, PULSES, OIL, SALT
    val transactionType: String, // RECEIPT, USAGE, ADJUSTMENT
    val quantityKg: Double,
    val runningBalanceKg: Double,
    val referenceId: String = "", // e.g. receiptId or date
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_meal_records")
data class DailyMealRecordEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val schoolId: String = "SCH-CG-RPR-001",
    val academicYear: String = "2026-27",
    val calendarStatus: String = "WORKING_DAY", // WORKING_DAY, HOLIDAY, VACATION, SPECIAL_WORKING_DAY, EMERGENCY_CLOSURE
    val holidayReason: String = "",
    val enrolledStudents: Int = 0,
    val enrolledBoys: Int = 0,
    val enrolledGirls: Int = 0,
    val boysPresent: Int = 0,
    val girlsPresent: Int = 0,
    val studentsPresent: Int = 0,
    val mealServed: Boolean = true,
    val boysServed: Int = 0,
    val girlsServed: Int = 0,
    val studentsServed: Int = 0,
    val menuDetails: String = "Rice, Dal, Mixed Veg (चावल, दाल, मिश्रित सब्जी)",
    val cooksPresentCount: Int = 0,
    val totalCooksCount: Int = 0,
    val tastingDone: Boolean = true,
    val tastedBy: String = "Head Teacher & Cook",
    val tasteQuality: String = "GOOD", // GOOD, SATISFACTORY, POOR
    val hygieneChecked: Boolean = true,
    val photoUri: String = "",
    val riceConsumedKg: Double = 0.0,
    val customItemsUsedJson: String = "", // JSON array of custom item IDs used for this meal
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, FAILED
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val userName: String = "Head Teacher (Rajesh Sharma)",
    val userRole: String = "HEAD_TEACHER",
    val action: String, // CREATE, UPDATE, DELETE, CLOSE_MONTH, REOPEN_MONTH, ADJUSTMENT
    val moduleName: String, // ENROLLMENT, TEACHER, COOK, DAILY_MEAL, RICE_RECEIPT, STOCK, CALENDAR
    val recordId: String,
    val previousValue: String = "",
    val newValue: String = "",
    val timestamp: String = "",
    val details: String = ""
)

@Entity(tableName = "config_norms")
data class ConfigNormsEntity(
    @PrimaryKey val configId: String = "DEFAULT_CG_CONFIG",
    val stateName: String = "Chhattisgarh",
    val primaryRiceNormGrams: Double = 150.0, // 150g (0.150 kg) per child in Class 1-5
    val upperPrimaryRiceNormGrams: Double = 150.0, // 150g (0.150 kg) per child in Class 6-8
    val pulseNormGrams: Double = 30.0, // 30g (0.030 kg) per child
    val oilNormGrams: Double = 7.5, // 7.5g (0.0075 kg) per child
    val vegetableNormGrams: Double = 75.0, // 75g (0.075 kg) per child
    val saltNormGrams: Double = 5.0, // 5g (0.005 kg) per child
    val primaryReimbursementRate: Double = 0.0, // ₹ per student per day for Primary (Class 1-5) (blank by default)
    val middleReimbursementRate: Double = 0.0, // ₹ per student per day for Middle (Class 6-8) (blank by default)
    val reimbursementRate: Double = 0.0, // ₹ per student per day (overall / combined)
    val cookingCostRate: Double = 0.0, // ₹ per student per day
    val lowStockThresholdKg: Double = 50.0, // Low stock alert threshold in kg
    val goodStockThresholdDays: Int = 15,
    val lowStockThresholdDays: Int = 5,
    val customItemsJson: String = "", // JSON array for custom food/nutrition items
    val adminOfficeName: String = "Block Education Officer", // Default office name (विकासखंड शिक्षा अधिकारी)
    val adminOfficeWhatsapp: String = "", // Administration office WhatsApp mobile number
    val adminOfficeEmail: String = "" // Administration office email address
)

enum class UserRole(val code: String, val titleHi: String, val titleEn: String, val descriptionHi: String) {
    SUPER_ADMIN(
        code = "SUPER_ADMIN",
        titleHi = "राज्य / सुपर एडमिन (Super Admin)",
        titleEn = "State / Super Admin",
        descriptionHi = "समस्त विद्यालय, जिला एवं राज्य स्तरीय डेटा नियंत्रण एवं निगरानी"
    ),
    DISTRICT_ADMIN(
        code = "DISTRICT_ADMIN",
        titleHi = "जिला नोडल अधिकारी (District Admin)",
        titleEn = "District Nodal Officer",
        descriptionHi = "जिले के अंतर्गत आने वाले समस्त विकासखंडों एवं विद्यालयों की निगरानी"
    ),
    BLOCK_ADMIN(
        code = "BLOCK_ADMIN",
        titleHi = "विकासखंड शिक्षा अधिकारी (Block Admin)",
        titleEn = "Block Education Officer",
        descriptionHi = "विकासखंड स्तर पर खाद्यान्न आवंटन एवं मासिक प्रतिपूर्ति अनुमोदन"
    ),
    HEADMASTER(
        code = "HEADMASTER",
        titleHi = "प्रधानाध्यापक (Headmaster / Principal)",
        titleEn = "Headmaster / Principal",
        descriptionHi = "पूर्ण अधिकार: विद्यालय प्रोफाइल, स्टाफ रोल, स्टॉक, वित्तीय प्रतिपूर्ति एवं बैकअप प्रबंधन"
    ),
    MDM_INCHARGE(
        code = "MDM_INCHARGE",
        titleHi = "एमडीएम प्रभारी शिक्षक (MDM In-Charge)",
        titleEn = "MDM In-Charge Teacher",
        descriptionHi = "दैनिक भोजन उपस्थिति, स्टॉक प्राप्ति (PDS), दैनिक मेनू एवं रिपोर्ट प्रविष्टि"
    ),
    SHG_REPRESENTATIVE(
        code = "SHG_REPRESENTATIVE",
        titleHi = "स्वयं सहायता समूह प्रतिनिधि (SHG / Cook Head)",
        titleEn = "SHG / Cook Representative",
        descriptionHi = "रसोइया उपस्थिति, दैनिक सामग्री आवंटन एवं भोजन मेनू अवलोकन"
    ),
    ASSISTANT_TEACHER(
        code = "ASSISTANT_TEACHER",
        titleHi = "सहायक शिक्षक (Assistant Teacher)",
        titleEn = "Assistant Teacher",
        descriptionHi = "कक्षावार छात्र दैनिक उपस्थिति एवं भोजन वितरण प्रविष्टि"
    );

    companion object {
        fun fromCode(code: String): UserRole = values().find { it.code.equals(code, ignoreCase = true) } ?: HEADMASTER
    }
}

@Entity(tableName = "users")
data class UserAccountEntity(
    @PrimaryKey val userId: String,
    val schoolId: String = "SCH-CG-RPR-001",
    val udiseCode: String = "22080100308",
    val name: String,
    val role: String = UserRole.HEADMASTER.code,
    val mobile: String = "",
    val email: String = "",
    val pin: String = "1234", // 4-digit quick unlock PIN
    val securityQuestion: String = "What is your school name?",
    val securityAnswer: String = "",
    val isActive: Boolean = true,
    val createdAt: String = "",
    val lastLogin: String = ""
)
