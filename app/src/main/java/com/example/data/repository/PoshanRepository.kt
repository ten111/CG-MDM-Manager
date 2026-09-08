package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PoshanRepository(private val db: AppDatabase) {

    init {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                db.stockTransactionDao().migrateLegacyItemIds()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    // ----------------- SCHOOL MASTER -----------------
    fun getSchool(): Flow<SchoolEntity?> = db.schoolDao().getSchool()

    suspend fun getSchoolDirect(): SchoolEntity? = withContext(Dispatchers.IO) {
        db.schoolDao().getSchoolDirect()
    }

    suspend fun saveSchool(school: SchoolEntity) = withContext(Dispatchers.IO) {
        db.schoolDao().insertOrUpdateSchool(school)

        // Synchronize Headmaster user account with the updated school profile
        val headmasters = db.userDao().getAllHeadmastersDirect()
        if (headmasters.isNotEmpty()) {
            for (hm in headmasters) {
                val updatedHm = hm.copy(
                    name = school.headTeacherName.ifBlank { hm.name },
                    mobile = school.headTeacherMobile.ifBlank { hm.mobile },
                    udiseCode = school.udiseCode.ifBlank { hm.udiseCode },
                    schoolId = school.schoolId
                )
                db.userDao().insertOrUpdateUser(updatedHm)
            }
        } else {
            val newHeadmaster = UserAccountEntity(
                userId = "USER-HM-${school.udiseCode.ifEmpty { "001" }}",
                schoolId = school.schoolId,
                udiseCode = school.udiseCode,
                name = school.headTeacherName,
                role = UserRole.HEADMASTER.code,
                mobile = school.headTeacherMobile,
                email = "",
                pin = "1234",
                securityQuestion = "What is your school name?",
                securityAnswer = school.villageName.ifEmpty { "bodla" }.lowercase(),
                isActive = true,
                createdAt = getCurrentTimestamp(),
                lastLogin = getCurrentTimestamp()
            )
            db.userDao().insertOrUpdateUser(newHeadmaster)
        }

        logAudit("UPDATE", "SCHOOL_MASTER", school.schoolId, "", school.schoolName, "School master updated & Headmaster user synced: ${school.headTeacherName}")
    }

    // ----------------- CALENDAR & HOLIDAYS (Full implementation at bottom) -----------------
    fun getCalendarEventsForMonth(monthPrefix: String): Flow<List<CalendarEventEntity>> =
        db.academicCalendarDao().getEventsForMonth(monthPrefix)
    suspend fun getEventForDate(date: String): CalendarEventEntity? = withContext(Dispatchers.IO) {
        db.academicCalendarDao().getEventByDate(date)
    }

    // ----------------- MONTHLY ENROLLMENT -----------------
    fun getAllEnrollments(): Flow<List<MonthlyEnrollmentEntity>> = db.monthlyEnrollmentDao().getAllEnrollments()
    fun getEnrollmentForMonth(monthYear: String): Flow<MonthlyEnrollmentEntity?> =
        db.monthlyEnrollmentDao().getEnrollmentForMonth(monthYear)
    suspend fun getEnrollmentForMonthDirect(monthYear: String): MonthlyEnrollmentEntity? = withContext(Dispatchers.IO) {
        db.monthlyEnrollmentDao().getEnrollmentForMonthDirect(monthYear)
    }
    suspend fun getLatestEnrollment(): MonthlyEnrollmentEntity? = withContext(Dispatchers.IO) {
        db.monthlyEnrollmentDao().getLatestEnrollment()
    }

    suspend fun saveEnrollment(enrollment: MonthlyEnrollmentEntity) = withContext(Dispatchers.IO) {
        val existing = db.monthlyEnrollmentDao().getEnrollmentForMonthDirect(enrollment.monthYear)
        val now = getCurrentTimestamp()
        val toSave = enrollment.copy(
            createdAt = existing?.createdAt?.ifEmpty { now } ?: now,
            updatedAt = now
        )
        db.monthlyEnrollmentDao().insertOrUpdateEnrollment(toSave)
        logAudit(
            if (existing == null) "CREATE" else "UPDATE",
            "MONTHLY_ENROLLMENT",
            enrollment.monthYear,
            existing?.totalEnrollment?.toString() ?: "None",
            enrollment.totalEnrollment.toString(),
            "Enrollment updated for ${enrollment.monthYear}"
        )
    }

    suspend fun copyPreviousMonthEnrollment(sourceMonthYear: String, targetMonthYear: String): Boolean = withContext(Dispatchers.IO) {
        val source = db.monthlyEnrollmentDao().getEnrollmentForMonthDirect(sourceMonthYear) ?: return@withContext false
        val now = getCurrentTimestamp()
        val newRecord = source.copy(
            monthYear = targetMonthYear,
            isLocked = false,
            createdAt = now,
            updatedAt = now
        )
        db.monthlyEnrollmentDao().insertOrUpdateEnrollment(newRecord)
        logAudit(
            "COPY_MONTH",
            "MONTHLY_ENROLLMENT",
            targetMonthYear,
            "Copied from $sourceMonthYear",
            "Total: ${newRecord.totalEnrollment}",
            "Replicated enrollment snapshot"
        )
        true
    }

    suspend fun lockMonthEnrollment(monthYear: String, lock: Boolean) = withContext(Dispatchers.IO) {
        val now = getCurrentTimestamp()
        db.monthlyEnrollmentDao().updateLockStatus(monthYear, lock, now)
        logAudit(
            if (lock) "LOCK_MONTH" else "REOPEN_MONTH",
            "MONTHLY_ENROLLMENT",
            monthYear,
            "Lock state toggled",
            "isLocked = $lock",
            "Month closing / unlock action"
        )
    }

    // ----------------- MONTHLY TEACHERS -----------------
    fun getAllTeacherSummaries(): Flow<List<MonthlyTeacherEntity>> = db.monthlyTeacherDao().getAllTeacherSummaries()
    fun getTeacherSummaryForMonth(monthYear: String): Flow<MonthlyTeacherEntity?> =
        db.monthlyTeacherDao().getTeacherSummaryForMonth(monthYear)
    suspend fun saveTeacherSummary(summary: MonthlyTeacherEntity) = withContext(Dispatchers.IO) {
        val existing = db.monthlyTeacherDao().getTeacherSummaryForMonthDirect(summary.monthYear)
        val now = getCurrentTimestamp()
        val toSave = summary.copy(
            createdAt = existing?.createdAt?.ifEmpty { now } ?: now,
            updatedAt = now
        )
        db.monthlyTeacherDao().insertOrUpdateTeacherSummary(toSave)
        logAudit("CREATE/UPDATE", "MONTHLY_TEACHER", summary.monthYear, "", "Total: ${summary.totalTeachers}", "Teacher data saved")
    }

    suspend fun copyPreviousMonthTeacherData(sourceMonthYear: String, targetMonthYear: String): Boolean = withContext(Dispatchers.IO) {
        val source = db.monthlyTeacherDao().getTeacherSummaryForMonthDirect(sourceMonthYear) ?: return@withContext false
        val now = getCurrentTimestamp()
        val newRecord = source.copy(
            monthYear = targetMonthYear,
            isLocked = false,
            createdAt = now,
            updatedAt = now
        )
        db.monthlyTeacherDao().insertOrUpdateTeacherSummary(newRecord)
        logAudit(
            "COPY_MONTH",
            "MONTHLY_TEACHER",
            targetMonthYear,
            "Copied from $sourceMonthYear",
            "Total: ${newRecord.totalTeachers}",
            "Replicated teacher snapshot"
        )
        true
    }

    // ----------------- COOKS -----------------
    fun getAllCooks(): Flow<List<CookEntity>> = db.cookDao().getAllCooks()
    fun getActiveCooks(): Flow<List<CookEntity>> = db.cookDao().getActiveCooks()
    suspend fun getActiveCooksDirect(): List<CookEntity> = withContext(Dispatchers.IO) {
        db.cookDao().getActiveCooksDirect()
    }
    suspend fun saveCook(cook: CookEntity) = withContext(Dispatchers.IO) {
        // Automatically mask account number if not masked
        val rawAcc = cook.bankAccountNo
        val masked = if (rawAcc.length > 4) "XXXX XXXX " + rawAcc.takeLast(4) else "XXXX XXXX 0000"
        val toSave = cook.copy(maskedAccountNo = masked)
        db.cookDao().insertOrUpdateCook(toSave)
        logAudit("SAVE_COOK", "COOK_MASTER", cook.cookId, "", cook.name, "Cook saved: ${cook.name}")
    }
    suspend fun updateCookStatus(cookId: String, status: String) = withContext(Dispatchers.IO) {
        db.cookDao().updateCookStatus(cookId, status)
        logAudit("STATUS_CHANGE", "COOK_MASTER", cookId, "", status, "Cook status changed to $status")
    }

    // ----------------- COOK ATTENDANCE -----------------
    fun getCookAttendancesForDate(date: String): Flow<List<CookAttendanceEntity>> =
        db.cookAttendanceDao().getAttendancesForDate(date)
    fun getCookAttendancesForMonth(monthPrefix: String): Flow<List<CookAttendanceEntity>> =
        db.cookAttendanceDao().getAttendancesForMonth(monthPrefix)
    suspend fun saveCookAttendances(date: String, attendances: List<CookAttendanceEntity>) = withContext(Dispatchers.IO) {
        db.cookAttendanceDao().deleteAttendancesForDate(date)
        db.cookAttendanceDao().insertAttendances(attendances)
    }

    // ----------------- COOKING AGENCY -----------------
    fun getAllAgencies(): Flow<List<CookingAgencyEntity>> = db.cookingAgencyDao().getAllAgencies()
    suspend fun saveAgency(agency: CookingAgencyEntity) = withContext(Dispatchers.IO) {
        db.cookingAgencyDao().insertOrUpdateAgency(agency)
        logAudit("SAVE_AGENCY", "COOKING_AGENCY", agency.agencyId, "", agency.name, "Agency details updated")
    }

    // ----------------- PDS SHOPS -----------------
    fun getAllPdsShops(): Flow<List<PdsShopEntity>> = db.pdsShopDao().getAllPdsShops()
    suspend fun savePdsShop(shop: PdsShopEntity) = withContext(Dispatchers.IO) {
        db.pdsShopDao().insertOrUpdatePdsShop(shop)
        logAudit("SAVE_PDS_SHOP", "PDS_SHOP", shop.pdsId, "", shop.shopName, "PDS shop saved")
    }

    // ----------------- RICE RECEIPTS & STOCK INTEGRATION -----------------
    fun getAllReceipts(): Flow<List<RiceReceiptEntity>> = db.riceReceiptDao().getAllReceipts()
    fun getReceiptsForMonth(monthPrefix: String): Flow<List<RiceReceiptEntity>> =
        db.riceReceiptDao().getReceiptsForMonth(monthPrefix)

    /**
     * Records rice received from PDS shop and AUTOMATICALLY increments the stock ledger.
     */
    suspend fun recordRiceReceipt(receipt: RiceReceiptEntity) = withContext(Dispatchers.IO) {
        db.riceReceiptDao().insertReceipt(receipt)

        val currentBalance = db.stockTransactionDao().getLatestBalanceDirectForItem("RICE") ?: 0.0
        val newBalance = currentBalance + receipt.quantityKg

        val transaction = StockTransactionEntity(
            transactionDate = receipt.receiptDate,
            itemType = "RICE",
            transactionType = "RECEIPT",
            quantityKg = receipt.quantityKg,
            runningBalanceKg = newBalance,
            referenceId = receipt.receiptId,
            description = "PDS Fortified Rice Receipt (Challan #${receipt.challanNumber})"
        )
        db.stockTransactionDao().insertTransaction(transaction)
        recalculateStockLedgerBalances("RICE")

        logAudit(
            "RECEIVE_RICE",
            "RICE_RECEIPT",
            receipt.receiptId,
            "Balance before: $currentBalance kg",
            "Added: ${receipt.quantityKg} kg -> New Bal: $newBalance kg",
            "PDS Challan #${receipt.challanNumber}"
        )
    }

    suspend fun editRiceReceipt(receipt: RiceReceiptEntity) = withContext(Dispatchers.IO) {
        val oldReceipt = db.riceReceiptDao().getReceiptById(receipt.receiptId)
        db.riceReceiptDao().insertReceipt(receipt)

        // Find linked transaction or create if missing
        val existingTxn = db.stockTransactionDao().getTransactionByReferenceId(receipt.receiptId)
        if (existingTxn != null) {
            val updatedTxn = existingTxn.copy(
                transactionDate = receipt.receiptDate,
                quantityKg = receipt.quantityKg,
                description = "PDS Fortified Rice Receipt (Challan #${receipt.challanNumber})"
            )
            db.stockTransactionDao().insertTransaction(updatedTxn)
        } else {
            val transaction = StockTransactionEntity(
                transactionDate = receipt.receiptDate,
                itemType = "RICE",
                transactionType = "RECEIPT",
                quantityKg = receipt.quantityKg,
                runningBalanceKg = receipt.quantityKg,
                referenceId = receipt.receiptId,
                description = "PDS Fortified Rice Receipt (Challan #${receipt.challanNumber})"
            )
            db.stockTransactionDao().insertTransaction(transaction)
        }

        recalculateStockLedgerBalances("RICE")
        logAudit(
            "EDIT_RECEIPT",
            "RICE_RECEIPT",
            receipt.receiptId,
            "Old: ${oldReceipt?.quantityKg} kg (Challan #${oldReceipt?.challanNumber})",
            "New: ${receipt.quantityKg} kg (Challan #${receipt.challanNumber})",
            "Receipt updated and stock ledger recalculated"
        )
    }

    suspend fun deleteRiceReceipt(receiptId: String) = withContext(Dispatchers.IO) {
        val oldReceipt = db.riceReceiptDao().getReceiptById(receiptId)
        db.riceReceiptDao().deleteReceiptById(receiptId)
        db.stockTransactionDao().deleteTransactionByReferenceId(receiptId)
        recalculateStockLedgerBalances("RICE")
        logAudit(
            "DELETE_RECEIPT",
            "RICE_RECEIPT",
            receiptId,
            "Deleted receipt of ${oldReceipt?.quantityKg} kg (Challan #${oldReceipt?.challanNumber})",
            "",
            "Receipt removed and stock ledger balance adjusted"
        )
    }

    // ----------------- STOCK LEDGER -----------------
    fun getAllStockTransactions(): Flow<List<StockTransactionEntity>> = db.stockTransactionDao().getAllTransactions()
    fun getStockTransactionsForMonth(monthPrefix: String): Flow<List<StockTransactionEntity>> =
        db.stockTransactionDao().getTransactionsForMonth(monthPrefix)
    fun getStockTransactionsForItem(itemType: String): Flow<List<StockTransactionEntity>> =
        db.stockTransactionDao().getTransactionsForItem(itemType)
    fun getLatestStockBalance(): Flow<Double?> = db.stockTransactionDao().getLatestBalance()
    fun getLatestStockBalanceForItem(itemType: String): Flow<Double?> =
        db.stockTransactionDao().getLatestBalanceForItem(itemType)

    suspend fun addStockAdjustment(date: String, quantityChangeKg: Double, reason: String, itemType: String = "RICE") = withContext(Dispatchers.IO) {
        val currentBalance = db.stockTransactionDao().getLatestBalanceDirectForItem(itemType) ?: 0.0
        val newBalance = currentBalance + quantityChangeKg
        val transaction = StockTransactionEntity(
            transactionDate = date,
            itemType = itemType,
            transactionType = "ADJUSTMENT",
            quantityKg = quantityChangeKg,
            runningBalanceKg = newBalance,
            referenceId = "ADJ-" + System.currentTimeMillis(),
            description = "Authorized Stock Adjustment: $reason"
        )
        db.stockTransactionDao().insertTransaction(transaction)
        recalculateStockLedgerBalances(itemType)
        logAudit("STOCK_ADJUSTMENT", "STOCK", date, "Old Bal: $currentBalance", "Adj: $quantityChangeKg -> New: $newBalance", reason)
    }

    suspend fun addStockTransaction(transaction: StockTransactionEntity) = withContext(Dispatchers.IO) {
        db.stockTransactionDao().insertTransaction(transaction)
        recalculateStockLedgerBalances(transaction.itemType)
        logAudit("ADD_STOCK_ENTRY", "STOCK", transaction.transactionDate, "", "${transaction.itemType}: ${transaction.quantityKg}", transaction.description)
    }

    suspend fun editStockTransaction(transaction: StockTransactionEntity) = withContext(Dispatchers.IO) {
        val oldTxn = db.stockTransactionDao().getTransactionById(transaction.transactionId)
        db.stockTransactionDao().insertTransaction(transaction)

        // If it was linked to a receipt, keep receipt in sync
        if (transaction.transactionType == "RECEIPT" && transaction.referenceId.isNotEmpty()) {
            val receipt = db.riceReceiptDao().getReceiptById(transaction.referenceId)
            if (receipt != null) {
                db.riceReceiptDao().insertReceipt(
                    receipt.copy(
                        receiptDate = transaction.transactionDate,
                        quantityKg = transaction.quantityKg
                    )
                )
            }
        }

        recalculateStockLedgerBalances(transaction.itemType)
        logAudit(
            "EDIT_STOCK_ENTRY",
            "STOCK",
            transaction.transactionId.toString(),
            "Old: ${oldTxn?.quantityKg} kg on ${oldTxn?.transactionDate}",
            "New: ${transaction.quantityKg} kg on ${transaction.transactionDate}",
            transaction.description
        )
    }

    suspend fun deleteStockTransaction(transactionId: Long) = withContext(Dispatchers.IO) {
        val txn = db.stockTransactionDao().getTransactionById(transactionId)
        if (txn != null) {
            db.stockTransactionDao().deleteTransactionById(transactionId)
            if (txn.transactionType == "RECEIPT" && txn.referenceId.isNotEmpty()) {
                db.riceReceiptDao().deleteReceiptById(txn.referenceId)
            }
            recalculateStockLedgerBalances(txn.itemType)
            logAudit("DELETE_STOCK_ENTRY", "STOCK", transactionId.toString(), "Deleted ${txn.description} (${txn.quantityKg} kg)", "", "Ledger balance recomputed")
        }
    }

    suspend fun clearAllStockData() = withContext(Dispatchers.IO) {
        db.stockTransactionDao().clearAllTransactions()
        db.riceReceiptDao().clearAllReceipts()
        logAudit(
            "CLEAR_STOCK_DATA",
            "STOCK_LEDGER",
            "ALL_ITEMS",
            "Existing stock records cleared",
            "0.0 kg",
            "Stock register cleared for fresh entry from scratch."
        )
    }

    suspend fun clearStockForItem(itemType: String) = withContext(Dispatchers.IO) {
        db.stockTransactionDao().clearTransactionsForItem(itemType)
        if (itemType.equals("RICE", ignoreCase = true)) {
            db.riceReceiptDao().clearAllReceipts()
        }
        logAudit(
            "CLEAR_STOCK_ITEM",
            "STOCK_LEDGER",
            itemType,
            "Cleared $itemType entries",
            "0.0",
            "$itemType stock transactions cleared from database."
        )
    }

    suspend fun setOpeningStockBalance(
        itemType: String,
        date: String,
        openingQuantityKg: Double,
        remarks: String
    ) = withContext(Dispatchers.IO) {
        val transaction = StockTransactionEntity(
            transactionDate = date,
            itemType = itemType,
            transactionType = "OPENING_BALANCE",
            quantityKg = openingQuantityKg,
            runningBalanceKg = openingQuantityKg,
            referenceId = "OPN-" + System.currentTimeMillis(),
            description = if (remarks.isNotBlank()) remarks else "Opening Balance ($itemType)"
        )
        db.stockTransactionDao().insertTransaction(transaction)
        recalculateStockLedgerBalances(itemType)
        logAudit(
            "SET_OPENING_BALANCE",
            "STOCK",
            itemType,
            "0.0",
            "$openingQuantityKg",
            "Set opening balance for $itemType on $date: $remarks"
        )
    }

    suspend fun recalculateStockLedgerBalances(itemType: String = "RICE") = withContext(Dispatchers.IO) {
        val transactions = db.stockTransactionDao().getTransactionsForItemAsc(itemType)
        var runningBal = 0.0
        for (t in transactions) {
            runningBal += t.quantityKg
            if (runningBal < 0.0) runningBal = 0.0
            if (Math.abs(t.runningBalanceKg - runningBal) > 0.001) {
                db.stockTransactionDao().updateRunningBalance(t.transactionId, runningBal)
            }
        }
    }

    // ----------------- DAILY MEAL RECORD -----------------
    fun getAllMealRecords(): Flow<List<DailyMealRecordEntity>> = db.dailyMealRecordDao().getAllRecords()
    fun getDailyMealRecord(date: String): Flow<DailyMealRecordEntity?> = db.dailyMealRecordDao().getRecordForDate(date)
    suspend fun getDailyMealRecordDirect(date: String): DailyMealRecordEntity? = withContext(Dispatchers.IO) {
        db.dailyMealRecordDao().getRecordForDateDirect(date)
    }
    fun getDailyMealRecordsForMonth(monthPrefix: String): Flow<List<DailyMealRecordEntity>> =
        db.dailyMealRecordDao().getRecordsForMonth(monthPrefix)
    suspend fun getDailyMealRecordsForMonthDirect(monthPrefix: String): List<DailyMealRecordEntity> = withContext(Dispatchers.IO) {
        db.dailyMealRecordDao().getRecordsForMonthDirect(monthPrefix)
    }

    suspend fun submitDailyMealReport(
        record: DailyMealRecordEntity,
        cookAttendances: List<CookAttendanceEntity>
    ) = withContext(Dispatchers.IO) {
        val now = getCurrentTimestamp()
        val toSave = record.copy(
            createdAt = record.createdAt.ifEmpty { now },
            updatedAt = now,
            syncStatus = "SYNCED"
        )
        db.dailyMealRecordDao().insertOrUpdateRecord(toSave)
        saveCookAttendances(record.date, cookAttendances)

        // If meal was served, calculate rice consumption and insert into stock ledger
        if (record.mealServed && record.studentsServed > 0) {
            val config = db.configNormsDao().getConfigDirect()
            val avgNormGrams = config?.primaryRiceNormGrams ?: 150.0
            val consumedKg = (record.studentsServed * avgNormGrams) / 1000.0

            val currentBalance = db.stockTransactionDao().getLatestBalanceDirect() ?: 0.0
            val newBalance = (currentBalance - consumedKg).coerceAtLeast(0.0)

            val existingTxn = db.stockTransactionDao().getTransactionByReferenceId(record.date)
            if (existingTxn != null) {
                db.stockTransactionDao().insertTransaction(
                    existingTxn.copy(
                        quantityKg = -consumedKg,
                        description = "Daily Meal Consumption for ${record.studentsServed} Students"
                    )
                )
            } else {
                val usageTransaction = StockTransactionEntity(
                    transactionDate = record.date,
                    itemType = "RICE",
                    transactionType = "USAGE",
                    quantityKg = -consumedKg,
                    runningBalanceKg = newBalance,
                    referenceId = record.date,
                    description = "Daily Meal Consumption for ${record.studentsServed} Students"
                )
                db.stockTransactionDao().insertTransaction(usageTransaction)
            }
            recalculateStockLedgerBalances("RICE")
        }

        logAudit(
            "DAILY_REPORT_SUBMIT",
            "DAILY_MEAL",
            record.date,
            "",
            "Served: ${record.studentsServed}, Cooks: ${cookAttendances.count { it.isPresent }}",
            "Daily PM POSHAN record submitted"
        )
    }

    suspend fun deleteDailyMealReport(date: String) = withContext(Dispatchers.IO) {
        val oldRecord = db.dailyMealRecordDao().getRecordForDateDirect(date)
        db.dailyMealRecordDao().deleteRecordByDate(date)
        db.cookAttendanceDao().deleteAttendancesForDate(date)
        db.stockTransactionDao().deleteTransactionByReferenceId(date)
        recalculateStockLedgerBalances("RICE")
        logAudit(
            "DELETE_DAILY_MEAL",
            "DAILY_MEAL",
            date,
            "Present: ${oldRecord?.studentsPresent ?: 0}, Served: ${oldRecord?.studentsServed ?: 0}",
            "",
            "Daily meal & attendance report for $date deleted (stock and cook attendance removed)"
        )
    }

    // ----------------- AUDIT LOGS -----------------
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>> = db.auditLogDao().getRecentAuditLogs()
    suspend fun logAudit(
        action: String,
        moduleName: String,
        recordId: String,
        previousValue: String = "",
        newValue: String = "",
        details: String = ""
    ) = withContext(Dispatchers.IO) {
        db.auditLogDao().insertAuditLog(
            AuditLogEntity(
                action = action,
                moduleName = moduleName,
                recordId = recordId,
                previousValue = previousValue,
                newValue = newValue,
                timestamp = getCurrentTimestamp(),
                details = details
            )
        )
    }

    // ----------------- CONFIG NORMS -----------------
    fun getConfigNorms(): Flow<ConfigNormsEntity?> = db.configNormsDao().getConfig()
    suspend fun saveConfigNorms(config: ConfigNormsEntity) = withContext(Dispatchers.IO) {
        db.configNormsDao().insertOrUpdateConfig(config)

        // Automatically update all existing daily meal records and recalculate stock ledger usage
        val allMeals = db.dailyMealRecordDao().getAllRecordsDirect()
        for (meal in allMeals) {
            if (meal.mealServed && meal.studentsServed > 0) {
                val newConsumedKg = (meal.studentsServed * config.primaryRiceNormGrams) / 1000.0
                db.dailyMealRecordDao().insertOrUpdateRecord(
                    meal.copy(riceConsumedKg = newConsumedKg)
                )
                val existingTxn = db.stockTransactionDao().getTransactionByReferenceId(meal.date)
                if (existingTxn != null) {
                    db.stockTransactionDao().insertTransaction(
                        existingTxn.copy(
                            quantityKg = -newConsumedKg,
                            description = "Daily Meal Consumption for ${meal.studentsServed} Students"
                        )
                    )
                } else {
                    val usageTransaction = StockTransactionEntity(
                        transactionDate = meal.date,
                        itemType = "RICE",
                        transactionType = "USAGE",
                        quantityKg = -newConsumedKg,
                        runningBalanceKg = 0.0,
                        referenceId = meal.date,
                        description = "Daily Meal Consumption for ${meal.studentsServed} Students"
                    )
                    db.stockTransactionDao().insertTransaction(usageTransaction)
                }
            }
        }
        recalculateStockLedgerBalances("RICE")

        logAudit("CONFIG_UPDATE", "CONFIG_NORMS", config.configId, "", "Updated thresholds/norms (${config.primaryRiceNormGrams}g rice, ${config.pulseNormGrams}g pulse, ${config.vegetableNormGrams}g veg, ${config.oilNormGrams}g oil, ${config.saltNormGrams}g salt)", "Config saved and all stock usage recalculated")
    }

    // ----------------- ACADEMIC CALENDAR & HOLIDAYS -----------------
    fun getAllCalendarEvents(): Flow<List<CalendarEventEntity>> = db.academicCalendarDao().getAllEvents()

    fun getCalendarEventsForYear(year: Int): Flow<List<CalendarEventEntity>> =
        db.academicCalendarDao().getEventsForYear(year.toString())

    suspend fun saveCalendarEvent(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        if (event.id != 0L) {
            db.academicCalendarDao().updateEvent(event)
            logAudit("UPDATE_HOLIDAY", "CALENDAR", event.eventDate, "", event.eventName, "Holiday updated: ${event.eventName}")
        } else {
            db.academicCalendarDao().insertEvent(event)
            logAudit("ADD_HOLIDAY", "CALENDAR", event.eventDate, "", event.eventName, "Holiday added: ${event.eventName}")
        }
    }

    suspend fun saveCalendarEvents(events: List<CalendarEventEntity>) = withContext(Dispatchers.IO) {
        db.academicCalendarDao().insertEvents(events)
        logAudit("BATCH_ADD_HOLIDAY", "CALENDAR", "${events.size} items", "", "", "Batch added ${events.size} holidays")
    }

    suspend fun deleteCalendarEvent(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        if (event.id != 0L) {
            db.academicCalendarDao().deleteEventById(event.id)
        } else {
            db.academicCalendarDao().deleteEvent(event)
        }
        logAudit("DELETE_HOLIDAY", "CALENDAR", event.eventDate, event.eventName, "", "Holiday removed: ${event.eventName}")
    }

    suspend fun deleteCalendarEventById(id: Long) = withContext(Dispatchers.IO) {
        db.academicCalendarDao().deleteEventById(id)
        logAudit("DELETE_HOLIDAY", "CALENDAR", "ID:$id", "", "", "Holiday ID $id removed")
    }

    suspend fun addVacationPeriod(
        startDate: String,
        endDate: String,
        vacationName: String,
        vacationType: String = "VACATION",
        authority: String = "School Education Dept, Govt of CG",
        orderNumber: String = ""
    ): Int = withContext(Dispatchers.IO) {
        val daysList = GovtHolidaysMaster.generateDateRange(
            startDate = startDate,
            endDate = endDate,
            name = vacationName,
            type = vacationType,
            orderNo = orderNumber,
            authority = authority
        )
        if (daysList.isNotEmpty()) {
            db.academicCalendarDao().insertEvents(daysList)
            logAudit("ADD_VACATION", "CALENDAR", "$startDate to $endDate", "", "$vacationName (${daysList.size} days)", "Vacation period added")
        }
        daysList.size
    }

    /**
     * Forced update from developer / Govt side:
     * Overwrites / merges official government holiday list for the given year.
     */
    suspend fun forceSyncOfficialGovtHolidays(year: Int): Int = withContext(Dispatchers.IO) {
        val officialList = GovtHolidaysMaster.getOfficialGovtHolidaysForYear(year)
        db.academicCalendarDao().insertEvents(officialList)
        logAudit(
            "GOVT_HOLIDAY_FORCE_SYNC",
            "CALENDAR",
            year.toString(),
            "Version ${GovtHolidaysMaster.GOVT_HOLIDAY_LIST_VERSION}",
            "${officialList.size} Official Holidays synced",
            "Govt Master Holiday update applied"
        )
        officialList.size
    }

    suspend fun importHolidaysCsv(csvContent: String): Int = withContext(Dispatchers.IO) {
        val lines = csvContent.lines()
        val parsedList = mutableListOf<CalendarEventEntity>()
        val sdfInput1 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val sdfInput2 = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US)
        val sdfInput3 = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)

        for ((index, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (index == 0 && (line.contains("Date", ignoreCase = true) || line.contains("दिनांक") || line.contains("Holiday_Name", ignoreCase = true))) {
                continue
            }
            // Parse CSV line handling potential quotes
            val parts = parseCsvLine(line)
            if (parts.size >= 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                var rawDate = parts[0].replace("\"", "").trim()
                val normalizedDate = try {
                    when {
                        rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> rawDate
                        rawDate.matches(Regex("\\d{2}-\\d{2}-\\d{4}")) -> {
                            val parsed = sdfInput2.parse(rawDate)
                            if (parsed != null) sdfInput1.format(parsed) else rawDate
                        }
                        rawDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                            val parsed = sdfInput3.parse(rawDate)
                            if (parsed != null) sdfInput1.format(parsed) else rawDate
                        }
                        else -> rawDate
                    }
                } catch (_: Exception) {
                    rawDate
                }

                val name = parts[1].replace("\"", "").trim()
                val rawType = if (parts.size >= 3) parts[2].replace("\"", "").trim().uppercase() else "STATE"
                val type = when {
                    rawType.contains("NATION") || rawType.contains("राष्ट्रीय") -> "NATIONAL"
                    rawType.contains("STATE") || rawType.contains("राज्य") -> "STATE"
                    rawType.contains("VACATION") || rawType.contains("ग्रीष्म") || rawType.contains("दशहरा") || rawType.contains("दीपावली") -> "VACATION"
                    rawType.contains("SCHOOL") || rawType.contains("विद्यालय") || rawType.contains("शाला") -> "SCHOOL"
                    rawType.contains("LOCAL") || rawType.contains("DISTRICT") || rawType.contains("स्थानीय") || rawType.contains("जिला") -> "LOCAL"
                    rawType.contains("EMERGENCY") || rawType.contains("आपदा") || rawType.contains("बारिश") -> "EMERGENCY"
                    else -> rawType.ifEmpty { "STATE" }
                }

                val orderNo = if (parts.size >= 4) parts[3].replace("\"", "").trim() else ""
                val authority = if (parts.size >= 5 && parts[4].isNotEmpty()) parts[4].replace("\"", "").trim() else "Govt of Chhattisgarh"
                val remarks = if (parts.size >= 6) parts[5].replace("\"", "").trim() else ""

                parsedList.add(
                    CalendarEventEntity(
                        eventDate = normalizedDate,
                        eventName = name,
                        eventType = type,
                        isMealReportingRequired = type == "NATIONAL" && (name.contains("Republic", ignoreCase = true) || name.contains("Independence", ignoreCase = true) || name.contains("गणतंत्र") || name.contains("स्वतंत्रता")),
                        orderNumber = orderNo,
                        authority = authority,
                        remarks = remarks
                    )
                )
            }
        }
        if (parsedList.isNotEmpty()) {
            db.academicCalendarDao().insertEvents(parsedList)
            logAudit("CSV_IMPORT", "CALENDAR", "${parsedList.size} rows", "", "", "CSV imported ${parsedList.size} holidays")
        }
        parsedList.size
    }

    // ----------------- USER & STAFF AUTHENTICATION (RBAC) -----------------
    fun getAllUsers(): Flow<List<UserAccountEntity>> = db.userDao().getAllUsers()

    suspend fun getActiveUsers(): List<UserAccountEntity> = withContext(Dispatchers.IO) {
        db.userDao().getActiveUsersDirect()
    }

    suspend fun getUserByPin(pin: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        db.userDao().getUserByPinDirect(pin)
    }

    suspend fun getHeadmaster(): UserAccountEntity? = withContext(Dispatchers.IO) {
        db.userDao().getHeadmasterDirect()
    }

    suspend fun saveUser(user: UserAccountEntity) = withContext(Dispatchers.IO) {
        db.userDao().insertOrUpdateUser(user)

        // If the saved user is the Headmaster, sync back to school profile
        if (user.role.equals(UserRole.HEADMASTER.code, ignoreCase = true)) {
            val school = db.schoolDao().getSchoolDirect()
            if (school != null) {
                val updatedSchool = school.copy(
                    headTeacherName = user.name.ifBlank { school.headTeacherName },
                    headTeacherMobile = user.mobile.ifBlank { school.headTeacherMobile }
                )
                db.schoolDao().insertOrUpdateSchool(updatedSchool)
            }
        }

        logAudit(
            "SAVE_USER",
            "USER_AUTH",
            user.userId,
            "",
            "${user.name} (${user.role})",
            "Staff user profile saved/updated"
        )
    }

    suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        val user = db.userDao().getUserByIdDirect(userId)
        db.userDao().deleteUserById(userId)
        logAudit(
            "DELETE_USER",
            "USER_AUTH",
            userId,
            user?.name ?: "",
            "",
            "Staff user removed: ${user?.name}"
        )
    }

    suspend fun registerHeadmasterAndSchool(
        school: SchoolEntity,
        headmasterUser: UserAccountEntity
    ) = withContext(Dispatchers.IO) {
        db.schoolDao().insertOrUpdateSchool(school)
        db.userDao().insertOrUpdateUser(headmasterUser)
        logAudit(
            "REGISTER_SCHOOL",
            "ONBOARDING",
            school.udiseCode,
            "",
            "${school.schoolName} / HM: ${headmasterUser.name}",
            "Initial school U-DISE registration and Headmaster onboarding completed"
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                (ch == ',' || ch == '\t') && !inQuotes -> {
                    result.add(curVal.toString().trim())
                    curVal = StringBuilder()
                }
                else -> curVal.append(ch)
            }
        }
        result.add(curVal.toString().trim())
        return result
    }
}
