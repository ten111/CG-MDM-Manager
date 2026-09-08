package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools LIMIT 1")
    fun getSchool(): Flow<SchoolEntity?>

    @Query("SELECT * FROM schools LIMIT 1")
    suspend fun getSchoolDirect(): SchoolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSchool(school: SchoolEntity)
}

@Dao
interface AcademicCalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY eventDate ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT COUNT(*) FROM calendar_events")
    suspend fun getEventsCount(): Int

    @Query("SELECT * FROM calendar_events WHERE eventDate = :date LIMIT 1")
    suspend fun getEventByDate(date: String): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE eventDate LIKE :yearPrefix || '%' ORDER BY eventDate ASC")
    fun getEventsForYear(yearPrefix: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE eventDate LIKE :monthPrefix || '%' ORDER BY eventDate ASC")
    fun getEventsForMonth(monthPrefix: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events ORDER BY eventDate ASC")
    suspend fun getAllEventsDirect(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEventEntity>)

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM calendar_events WHERE eventDate LIKE :yearPrefix || '%'")
    suspend fun deleteEventsForYear(yearPrefix: String)

    @Query("DELETE FROM calendar_events")
    suspend fun clearAllEvents()
}

@Dao
interface MonthlyEnrollmentDao {
    @Query("SELECT * FROM monthly_enrollments ORDER BY monthYear DESC")
    fun getAllEnrollments(): Flow<List<MonthlyEnrollmentEntity>>

    @Query("SELECT * FROM monthly_enrollments WHERE monthYear = :monthYear LIMIT 1")
    fun getEnrollmentForMonth(monthYear: String): Flow<MonthlyEnrollmentEntity?>

    @Query("SELECT * FROM monthly_enrollments WHERE monthYear = :monthYear LIMIT 1")
    suspend fun getEnrollmentForMonthDirect(monthYear: String): MonthlyEnrollmentEntity?

    @Query("SELECT * FROM monthly_enrollments ORDER BY monthYear DESC LIMIT 1")
    suspend fun getLatestEnrollment(): MonthlyEnrollmentEntity?

    @Query("SELECT * FROM monthly_enrollments ORDER BY monthYear DESC")
    suspend fun getAllEnrollmentsDirect(): List<MonthlyEnrollmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollments(list: List<MonthlyEnrollmentEntity>)

    @Query("DELETE FROM monthly_enrollments")
    suspend fun clearAllEnrollments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEnrollment(enrollment: MonthlyEnrollmentEntity)

    @Query("UPDATE monthly_enrollments SET isLocked = :isLocked, updatedAt = :updatedAt WHERE monthYear = :monthYear")
    suspend fun updateLockStatus(monthYear: String, isLocked: Boolean, updatedAt: String)
}

@Dao
interface MonthlyTeacherDao {
    @Query("SELECT * FROM monthly_teachers ORDER BY monthYear DESC")
    fun getAllTeacherSummaries(): Flow<List<MonthlyTeacherEntity>>

    @Query("SELECT * FROM monthly_teachers WHERE monthYear = :monthYear LIMIT 1")
    fun getTeacherSummaryForMonth(monthYear: String): Flow<MonthlyTeacherEntity?>

    @Query("SELECT * FROM monthly_teachers WHERE monthYear = :monthYear LIMIT 1")
    suspend fun getTeacherSummaryForMonthDirect(monthYear: String): MonthlyTeacherEntity?

    @Query("SELECT * FROM monthly_teachers ORDER BY monthYear DESC")
    suspend fun getAllTeacherSummariesDirect(): List<MonthlyTeacherEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacherSummaries(list: List<MonthlyTeacherEntity>)

    @Query("DELETE FROM monthly_teachers")
    suspend fun clearAllTeacherSummaries()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTeacherSummary(summary: MonthlyTeacherEntity)

    @Query("UPDATE monthly_teachers SET isLocked = :isLocked, updatedAt = :updatedAt WHERE monthYear = :monthYear")
    suspend fun updateLockStatus(monthYear: String, isLocked: Boolean, updatedAt: String)
}

@Dao
interface CookDao {
    @Query("SELECT * FROM cooks ORDER BY name ASC")
    fun getAllCooks(): Flow<List<CookEntity>>

    @Query("SELECT * FROM cooks WHERE status = 'ACTIVE' ORDER BY name ASC")
    fun getActiveCooks(): Flow<List<CookEntity>>

    @Query("SELECT * FROM cooks WHERE status = 'ACTIVE' ORDER BY name ASC")
    suspend fun getActiveCooksDirect(): List<CookEntity>

    @Query("SELECT * FROM cooks WHERE cookId = :cookId LIMIT 1")
    suspend fun getCookById(cookId: String): CookEntity?

    @Query("SELECT * FROM cooks ORDER BY name ASC")
    suspend fun getAllCooksDirect(): List<CookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCooks(cooks: List<CookEntity>)

    @Query("DELETE FROM cooks")
    suspend fun clearAllCooks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCook(cook: CookEntity)

    @Query("UPDATE cooks SET status = :status WHERE cookId = :cookId")
    suspend fun updateCookStatus(cookId: String, status: String)
}

@Dao
interface CookAttendanceDao {
    @Query("SELECT * FROM cook_attendances WHERE date = :date")
    fun getAttendancesForDate(date: String): Flow<List<CookAttendanceEntity>>

    @Query("SELECT * FROM cook_attendances WHERE date = :date")
    suspend fun getAttendancesForDateDirect(date: String): List<CookAttendanceEntity>

    @Query("SELECT * FROM cook_attendances WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getAttendancesForMonth(monthPrefix: String): Flow<List<CookAttendanceEntity>>

    @Query("SELECT * FROM cook_attendances WHERE cookId = :cookId AND date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getAttendancesForCookMonth(cookId: String, monthPrefix: String): Flow<List<CookAttendanceEntity>>

    @Query("SELECT * FROM cook_attendances ORDER BY date ASC")
    suspend fun getAllAttendancesDirect(): List<CookAttendanceEntity>

    @Query("DELETE FROM cook_attendances")
    suspend fun clearAllAttendances()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<CookAttendanceEntity>)

    @Query("DELETE FROM cook_attendances WHERE date = :date")
    suspend fun deleteAttendancesForDate(date: String)
}

@Dao
interface CookingAgencyDao {
    @Query("SELECT * FROM cooking_agencies ORDER BY name ASC")
    fun getAllAgencies(): Flow<List<CookingAgencyEntity>>

    @Query("SELECT * FROM cooking_agencies WHERE agencyId = :agencyId LIMIT 1")
    suspend fun getAgencyById(agencyId: String): CookingAgencyEntity?

    @Query("SELECT * FROM cooking_agencies ORDER BY name ASC")
    suspend fun getAllAgenciesDirect(): List<CookingAgencyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgencies(list: List<CookingAgencyEntity>)

    @Query("DELETE FROM cooking_agencies")
    suspend fun clearAllAgencies()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAgency(agency: CookingAgencyEntity)
}

@Dao
interface PdsShopDao {
    @Query("SELECT * FROM pds_shops ORDER BY shopName ASC")
    fun getAllPdsShops(): Flow<List<PdsShopEntity>>

    @Query("SELECT * FROM pds_shops WHERE pdsId = :pdsId LIMIT 1")
    suspend fun getPdsShopById(pdsId: String): PdsShopEntity?

    @Query("SELECT * FROM pds_shops ORDER BY shopName ASC")
    suspend fun getAllPdsShopsDirect(): List<PdsShopEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdsShops(list: List<PdsShopEntity>)

    @Query("DELETE FROM pds_shops")
    suspend fun clearAllPdsShops()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePdsShop(shop: PdsShopEntity)
}

@Dao
interface RiceReceiptDao {
    @Query("SELECT * FROM rice_receipts ORDER BY receiptDate DESC")
    fun getAllReceipts(): Flow<List<RiceReceiptEntity>>

    @Query("SELECT * FROM rice_receipts WHERE receiptDate LIKE :monthPrefix || '%' ORDER BY receiptDate DESC")
    fun getReceiptsForMonth(monthPrefix: String): Flow<List<RiceReceiptEntity>>

    @Query("SELECT * FROM rice_receipts WHERE receiptId = :receiptId LIMIT 1")
    suspend fun getReceiptById(receiptId: String): RiceReceiptEntity?

    @Query("SELECT * FROM rice_receipts")
    suspend fun getAllReceiptsDirect(): List<RiceReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipts(receipts: List<RiceReceiptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: RiceReceiptEntity)

    @Query("DELETE FROM rice_receipts WHERE receiptId = :receiptId")
    suspend fun deleteReceiptById(receiptId: String)

    @Query("DELETE FROM rice_receipts")
    suspend fun clearAllReceipts()
}

@Dao
interface StockTransactionDao {
    @Query("SELECT * FROM stock_transactions ORDER BY transactionId DESC")
    fun getAllTransactions(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE transactionDate LIKE :monthPrefix || '%' ORDER BY transactionId DESC")
    fun getTransactionsForMonth(monthPrefix: String): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE itemType = :itemType ORDER BY transactionId DESC")
    fun getTransactionsForItem(itemType: String): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: Long): StockTransactionEntity?

    @Query("SELECT * FROM stock_transactions WHERE referenceId = :referenceId LIMIT 1")
    suspend fun getTransactionByReferenceId(referenceId: String): StockTransactionEntity?

    @Query("SELECT runningBalanceKg FROM stock_transactions WHERE itemType = :itemType ORDER BY transactionId DESC LIMIT 1")
    fun getLatestBalanceForItem(itemType: String): Flow<Double?>

    @Query("SELECT runningBalanceKg FROM stock_transactions ORDER BY transactionId DESC LIMIT 1")
    fun getLatestBalance(): Flow<Double?>

    @Query("SELECT runningBalanceKg FROM stock_transactions ORDER BY transactionId DESC LIMIT 1")
    suspend fun getLatestBalanceDirect(): Double?

    @Query("SELECT runningBalanceKg FROM stock_transactions WHERE itemType = :itemType ORDER BY transactionId DESC LIMIT 1")
    suspend fun getLatestBalanceDirectForItem(itemType: String): Double?

    @Query("SELECT * FROM stock_transactions ORDER BY transactionDate ASC, transactionId ASC")
    suspend fun getAllTransactionsAsc(): List<StockTransactionEntity>

    @Query("SELECT * FROM stock_transactions WHERE itemType = :itemType ORDER BY transactionDate ASC, transactionId ASC")
    suspend fun getTransactionsForItemAsc(itemType: String): List<StockTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<StockTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransactionEntity)

    @Query("UPDATE stock_transactions SET runningBalanceKg = :runningBalance WHERE transactionId = :transactionId")
    suspend fun updateRunningBalance(transactionId: Long, runningBalance: Double)

    @Query("UPDATE stock_transactions SET itemType = 'item_ifa' WHERE itemType = 'item_iron_folic_acid' OR itemType = 'iron_folic_acid'")
    suspend fun migrateLegacyItemIds()

    @Query("DELETE FROM stock_transactions WHERE transactionId = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("DELETE FROM stock_transactions WHERE referenceId = :referenceId")
    suspend fun deleteTransactionByReferenceId(referenceId: String)

    @Query("DELETE FROM stock_transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM stock_transactions WHERE itemType = :itemType")
    suspend fun clearTransactionsForItem(itemType: String)
}

@Dao
interface DailyMealRecordDao {
    @Query("SELECT * FROM daily_meal_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyMealRecordEntity>>

    @Query("SELECT * FROM daily_meal_records WHERE date = :date LIMIT 1")
    fun getRecordForDate(date: String): Flow<DailyMealRecordEntity?>

    @Query("SELECT * FROM daily_meal_records WHERE date = :date LIMIT 1")
    suspend fun getRecordForDateDirect(date: String): DailyMealRecordEntity?

    @Query("SELECT * FROM daily_meal_records WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getRecordsForMonth(monthPrefix: String): Flow<List<DailyMealRecordEntity>>

    @Query("SELECT * FROM daily_meal_records WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    suspend fun getRecordsForMonthDirect(monthPrefix: String): List<DailyMealRecordEntity>

    @Query("SELECT * FROM daily_meal_records ORDER BY date DESC")
    suspend fun getAllRecordsDirect(): List<DailyMealRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<DailyMealRecordEntity>)

    @Query("DELETE FROM daily_meal_records")
    suspend fun clearAllRecords()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: DailyMealRecordEntity)

    @Delete
    suspend fun deleteRecord(record: DailyMealRecordEntity)

    @Query("DELETE FROM daily_meal_records WHERE date = :date")
    suspend fun deleteRecordByDate(date: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY logId DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY logId DESC")
    suspend fun getAllAuditLogsDirect(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllAuditLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}

@Dao
interface ConfigNormsDao {
    @Query("SELECT * FROM config_norms LIMIT 1")
    fun getConfig(): Flow<ConfigNormsEntity?>

    @Query("SELECT * FROM config_norms LIMIT 1")
    suspend fun getConfigDirect(): ConfigNormsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: ConfigNormsEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY role ASC, name ASC")
    suspend fun getAllUsersDirect(): List<UserAccountEntity>

    @Query("SELECT * FROM users ORDER BY role ASC, name ASC")
    fun getAllUsers(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY role ASC, name ASC")
    suspend fun getActiveUsersDirect(): List<UserAccountEntity>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<UserAccountEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserByIdDirect(userId: String): UserAccountEntity?

    @Query("SELECT * FROM users WHERE role = 'HEADMASTER' AND isActive = 1 LIMIT 1")
    suspend fun getHeadmasterDirect(): UserAccountEntity?

    @Query("SELECT * FROM users WHERE role = 'HEADMASTER'")
    suspend fun getAllHeadmastersDirect(): List<UserAccountEntity>

    @Query("SELECT * FROM users WHERE pin = :pin AND isActive = 1 LIMIT 1")
    suspend fun getUserByPinDirect(pin: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserAccountEntity>)

    @Delete
    suspend fun deleteUser(user: UserAccountEntity)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)
}
