package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SchoolEntity::class,
        AcademicYearEntity::class,
        CalendarEventEntity::class,
        MonthlyEnrollmentEntity::class,
        MonthlyTeacherEntity::class,
        CookEntity::class,
        CookAttendanceEntity::class,
        CookingAgencyEntity::class,
        PdsShopEntity::class,
        RiceReceiptEntity::class,
        StockTransactionEntity::class,
        DailyMealRecordEntity::class,
        AuditLogEntity::class,
        ConfigNormsEntity::class,
        UserAccountEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun academicCalendarDao(): AcademicCalendarDao
    abstract fun monthlyEnrollmentDao(): MonthlyEnrollmentDao
    abstract fun monthlyTeacherDao(): MonthlyTeacherDao
    abstract fun cookDao(): CookDao
    abstract fun cookAttendanceDao(): CookAttendanceDao
    abstract fun cookingAgencyDao(): CookingAgencyDao
    abstract fun pdsShopDao(): PdsShopDao
    abstract fun riceReceiptDao(): RiceReceiptDao
    abstract fun stockTransactionDao(): StockTransactionDao
    abstract fun dailyMealRecordDao(): DailyMealRecordDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun configNormsDao(): ConfigNormsDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE config_norms ADD COLUMN adminOfficeName TEXT NOT NULL DEFAULT 'Block Education Officer'")
                db.execSQL("ALTER TABLE config_norms ADD COLUMN adminOfficeWhatsapp TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE config_norms ADD COLUMN adminOfficeEmail TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poshan_school_manager.db"
                )
                    .addMigrations(MIGRATION_11_12)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialMasterData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (database.configNormsDao().getConfigDirect() == null) {
                                populateInitialMasterData(database)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
            }
        }

        suspend fun populateInitialMasterData(db: AppDatabase) {
            // 1. Config Norms (Standard Chhattisgarh defaults)
            if (db.configNormsDao().getConfigDirect() == null) {
                val defaultCustomItemsJson = com.example.data.model.CustomFoodItemParser.toJson(
                    com.example.data.model.CustomFoodItemParser.getDefaultCustomItems()
                )
                db.configNormsDao().insertOrUpdateConfig(
                    ConfigNormsEntity(
                        configId = "DEFAULT_CG_CONFIG",
                        stateName = "Chhattisgarh",
                        primaryRiceNormGrams = 150.0,
                        upperPrimaryRiceNormGrams = 150.0,
                        pulseNormGrams = 30.0,
                        oilNormGrams = 7.5,
                        vegetableNormGrams = 75.0,
                        saltNormGrams = 5.0,
                        primaryReimbursementRate = 0.0,
                        middleReimbursementRate = 0.0,
                        reimbursementRate = 0.0,
                        cookingCostRate = 0.0,
                        lowStockThresholdKg = 50.0,
                        goodStockThresholdDays = 15,
                        lowStockThresholdDays = 5,
                        customItemsJson = defaultCustomItemsJson
                    )
                )
            }

            // 2. Calendar Events & Holidays (Chhattisgarh State & Academic Events)
            if (db.academicCalendarDao().getEventsCount() == 0) {
                val calendarEvents = listOf(
                    CalendarEventEntity(
                        eventDate = "2026-08-09",
                        eventName = "रविवार (Sunday)",
                        eventType = "HOLIDAY",
                        isMealReportingRequired = false,
                        authority = "General Administration"
                    ),
                    CalendarEventEntity(
                        eventDate = "2026-08-15",
                        eventName = "स्वतंत्रता दिवस (Independence Day) - विशेष उपस्थिति",
                        eventType = "SPECIAL_WORKING_DAY",
                        isMealReportingRequired = true,
                        orderNumber = "CG/EDU/2026/78",
                        authority = "School Education Dept, CG",
                        remarks = "Special national event with sweet distribution & mid-day meal"
                    ),
                    CalendarEventEntity(
                        eventDate = "2026-08-16",
                        eventName = "रविवार (Sunday)",
                        eventType = "HOLIDAY",
                        isMealReportingRequired = false
                    ),
                    CalendarEventEntity(
                        eventDate = "2026-08-23",
                        eventName = "रविवार (Sunday)",
                        eventType = "HOLIDAY",
                        isMealReportingRequired = false
                    ),
                    CalendarEventEntity(
                        eventDate = "2026-08-27",
                        eventName = "हरेली पर्व (Hareli Festival - CG State Holiday)",
                        eventType = "HOLIDAY",
                        isMealReportingRequired = false,
                        orderNumber = "CG-GA-HOL-2026-14",
                        authority = "Govt of Chhattisgarh"
                    ),
                    CalendarEventEntity(
                        eventDate = "2026-08-30",
                        eventName = "रविवार (Sunday)",
                        eventType = "HOLIDAY",
                        isMealReportingRequired = false
                    ),
                    CalendarEventEntity(
                        eventDate = "2026-09-04",
                        eventName = "तीजा / हरतालिका तीज (Teeja Festival)",
                        eventType = "HOLIDAY",
                        isMealReportingRequired = false,
                        authority = "Govt of Chhattisgarh"
                    ),
                    CalendarEventEntity(
                        eventDate = "2026-10-18",
                        eventName = "दशहरा अवकाश (Dussehra Vacation)",
                        eventType = "VACATION",
                        isMealReportingRequired = false,
                        authority = "School Education Dept, CG"
                    ),
                    CalendarEventEntity(
                        eventDate = "2026-11-01",
                        eventName = "छत्तीसगढ़ राज्य स्थापना दिवस (CG Foundation Day)",
                        eventType = "HOLIDAY",
                        isMealReportingRequired = false,
                        authority = "Govt of Chhattisgarh"
                    )
                )
                db.academicCalendarDao().insertEvents(calendarEvents)
            }
        }
    }
}
