package com.example.cloud

import com.example.data.local.entity.*

enum class BackupStatus {
    IDLE,
    PREPARING,
    UPLOADING,
    DOWNLOADING,
    RESTORING,
    SUCCESS,
    FAILED
}

enum class BackupFrequency(val code: String, val titleHi: String, val titleEn: String) {
    DAILY_AFTER_SCHOOL("DAILY", "प्रतिदिन शाला उपरांत (4:00 PM)", "Daily After School (4:00 PM)"),
    WEEKLY_SATURDAY("WEEKLY", "साप्ताहिक (प्रत्येक शनिवार 1:00 PM)", "Weekly (Every Saturday 1:00 PM)"),
    ON_MEAL_SUBMIT("ON_MEAL_SUBMIT", "दैनिक भोजन उपस्थिति दर्ज होते ही", "Continuous (On Meal Attendance Saved)"),
    MANUAL_ONLY("MANUAL", "केवल मैन्युअल (जब चाहें तब)", "Manual Only")
}

data class GoogleDriveAccountInfo(
    val email: String = "info@kk86taxsolution.com",
    val displayName: String = "CG-MDM Manager Cloud Backup",
    val photoUrl: String = "",
    val isConnected: Boolean = true,
    val isPermissionGranted: Boolean = true,
    val grantedScope: String = "https://www.googleapis.com/auth/drive.file",
    val permissionGrantedAt: String = "",
    val storageTotalBytes: Long = 15L * 1024 * 1024 * 1024, // 15 GB
    val storageUsedBytes: Long = 2L * 1024 * 1024, // 2.1 MB used by app
    val driveRootFolder: String = "CG-MDM Manager Mobile",
    val driveFolderId: String = "drive_folder_cg_mdm_22080100308",
    val lastSyncedAt: String = "",
    val backupCount: Int = 0
)

data class BackupRecordCounts(
    val studentsCount: Int = 0,
    val teachersCount: Int = 0,
    val cooksCount: Int = 0,
    val mealsCount: Int = 0,
    val receiptsCount: Int = 0,
    val stockTransactionsCount: Int = 0,
    val auditLogsCount: Int = 0,
    val calendarEventsCount: Int = 0
)

data class PoshanBackupMetadata(
    val backupId: String,
    val fileId: String = "",
    val fileName: String,
    val rootFolder: String = "CG-MDM Manager Mobile",
    val monthFolder: String = "08-2026",
    val folderPath: String = "CG-MDM Manager Mobile/08-2026",
    val udiseCode: String,
    val schoolName: String,
    val timestamp: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val createdBy: String,
    val appVersion: String = "2.4.0",
    val databaseVersion: Int = 9,
    val recordCounts: BackupRecordCounts = BackupRecordCounts(),
    val sizeBytes: Long = 0,
    val checksumSha256: String = "",
    val isCloudBackup: Boolean = true,
    val downloadUrl: String = ""
)

data class DriveMonthFolderGroup(
    val monthYearCode: String, // e.g. "08-2026" or "FY 2026-27"
    val displayLabel: String,  // e.g. "08-2026 (August 2026)"
    val isFinancialYear: Boolean = false,
    val backups: List<PoshanBackupMetadata> = emptyList(),
    val totalSizeBytes: Long = 0,
    val lastModified: String = ""
)

data class PoshanBackupPayload(
    val metadata: PoshanBackupMetadata,
    val school: SchoolEntity?,
    val academicYears: List<AcademicYearEntity> = emptyList(),
    val calendarEvents: List<CalendarEventEntity> = emptyList(),
    val enrollments: List<MonthlyEnrollmentEntity> = emptyList(),
    val teachers: List<MonthlyTeacherEntity> = emptyList(),
    val cooks: List<CookEntity> = emptyList(),
    val cookAttendances: List<CookAttendanceEntity> = emptyList(),
    val cookingAgencies: List<CookingAgencyEntity> = emptyList(),
    val pdsShops: List<PdsShopEntity> = emptyList(),
    val riceReceipts: List<RiceReceiptEntity> = emptyList(),
    val stockTransactions: List<StockTransactionEntity> = emptyList(),
    val dailyMeals: List<DailyMealRecordEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val configNorms: ConfigNormsEntity? = null,
    val users: List<UserAccountEntity> = emptyList()
)

data class AutoBackupConfig(
    val enabled: Boolean = true,
    val frequency: BackupFrequency = BackupFrequency.DAILY_AFTER_SCHOOL,
    val scheduledTime: String = "16:00",
    val wifiOnly: Boolean = false,
    val keepLastCount: Int = 10,
    val lastBackupTimestamp: String = "",
    val lastBackupStatus: String = "SUCCESS",
    val lastBackupSize: Long = 0
)
