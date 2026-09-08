package com.example.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthSessionManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.PoshanRepository
import com.example.presentation.common.AppLanguage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PoshanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PoshanRepository(db)
    val syncManager = com.example.data.sync.FirestoreSyncManager(application, db)
    val syncState = syncManager.syncState
    val firebaseAuthRepo = com.example.auth.FirebaseAuthRepository(application, db)

    companion object {
        val IST_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
        fun getIstDateFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = IST_TIMEZONE
        }
        fun getIstMonthFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = IST_TIMEZONE
        }
    }

    // User & Authentication States
    private val _isUserLoggedIn = MutableStateFlow(AuthSessionManager.isLoggedIn(application))
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _isSchoolRegistered = MutableStateFlow(AuthSessionManager.isRegistered(application))
    val isSchoolRegistered: StateFlow<Boolean> = _isSchoolRegistered.asStateFlow()

    private val _currentUserId = MutableStateFlow(AuthSessionManager.getCurrentUserId(application))
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _currentUserRole = MutableStateFlow(AuthSessionManager.getCurrentUserRole(application))
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    val allUsers: StateFlow<List<UserAccountEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<UserAccountEntity?> = _currentUserId
        .flatMapLatest { uid -> repository.getAllUsers().map { list -> list.find { it.userId == uid } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val existingConfig = db.configNormsDao().getConfigDirect()
                if (existingConfig == null) {
                    AppDatabase.populateInitialMasterData(db)
                } else if (existingConfig.primaryReimbursementRate == 5.45 && existingConfig.middleReimbursementRate == 8.17) {
                    val updated = existingConfig.copy(
                        primaryReimbursementRate = 0.0,
                        middleReimbursementRate = 0.0,
                        reimbursementRate = 0.0,
                        cookingCostRate = 0.0
                    )
                    db.configNormsDao().insertOrUpdateConfig(updated)
                }
                syncHeadmasterIfMismatched()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private suspend fun syncHeadmasterIfMismatched() {
        try {
            val school = db.schoolDao().getSchoolDirect() ?: return
            val headmasters = db.userDao().getAllHeadmastersDirect()
            if (school.headTeacherName.isNotBlank()) {
                if (headmasters.isNotEmpty()) {
                    for (hm in headmasters) {
                        if (hm.name != school.headTeacherName ||
                            (school.headTeacherMobile.isNotBlank() && hm.mobile != school.headTeacherMobile)) {
                            val updatedHm = hm.copy(
                                name = school.headTeacherName,
                                mobile = school.headTeacherMobile.ifBlank { hm.mobile },
                                udiseCode = school.udiseCode.ifBlank { hm.udiseCode }
                            )
                            db.userDao().insertOrUpdateUser(updatedHm)

                            val app = getApplication<Application>()
                            if (_currentUserRole.value == UserRole.HEADMASTER || _currentUserId.value == hm.userId) {
                                AuthSessionManager.saveLoginSession(app, updatedHm)
                            }
                        }
                    }
                } else {
                    val newHm = UserAccountEntity(
                        userId = "USER-HM-${school.udiseCode.ifEmpty { "001" }}",
                        schoolId = school.schoolId,
                        udiseCode = school.udiseCode,
                        name = school.headTeacherName,
                        role = UserRole.HEADMASTER.code,
                        mobile = school.headTeacherMobile,
                        pin = "1234",
                        securityQuestion = "What is your school name?",
                        securityAnswer = school.villageName.ifEmpty { "bodla" }.lowercase(),
                        isActive = true,
                        createdAt = getIstDateFormat().format(Date()),
                        lastLogin = getIstDateFormat().format(Date())
                    )
                    db.userDao().insertOrUpdateUser(newHm)
                }
            }
        } catch (e: Exception) {
            // non-fatal
        }
    }

    // Language State (Hindi / English)
    private val _currentLanguage = MutableStateFlow(AppLanguage.HINDI)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        _currentLanguage.value = _currentLanguage.value.toggle()
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    // Current selection states (Indian Standard Time: Asia/Kolkata)
    private val _selectedMonth = MutableStateFlow(getIstMonthFormat().format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(getIstDateFormat().format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // School State
    val school: StateFlow<SchoolEntity?> = repository.getSchool()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Config Norms
    val configNorms: StateFlow<ConfigNormsEntity?> = repository.getConfigNorms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Monthly Enrollment for Selected Month
    val currentMonthEnrollment: StateFlow<MonthlyEnrollmentEntity?> = _selectedMonth
        .flatMapLatest { month -> repository.getEnrollmentForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allEnrollments: StateFlow<List<MonthlyEnrollmentEntity>> = repository.getAllEnrollments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Teacher Census for Selected Month
    val currentMonthTeachers: StateFlow<MonthlyTeacherEntity?> = _selectedMonth
        .flatMapLatest { month -> repository.getTeacherSummaryForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTeachers: StateFlow<List<MonthlyTeacherEntity>> = repository.getAllTeacherSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cooks
    val allCooks: StateFlow<List<CookEntity>> = repository.getAllCooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCooks: StateFlow<List<CookEntity>> = repository.getActiveCooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cook Attendance for Selected Date
    val dateCookAttendances: StateFlow<List<CookAttendanceEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getCookAttendancesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cook Attendances for Selected Month
    val monthCookAttendances: StateFlow<List<CookAttendanceEntity>> = _selectedMonth
        .flatMapLatest { month -> repository.getCookAttendancesForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cooking Agencies
    val allAgencies: StateFlow<List<CookingAgencyEntity>> = repository.getAllAgencies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PDS Shops
    val allPdsShops: StateFlow<List<PdsShopEntity>> = repository.getAllPdsShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rice Receipts
    val allReceipts: StateFlow<List<RiceReceiptEntity>> = repository.getAllReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stock Transactions & Balances
    val stockTransactions: StateFlow<List<StockTransactionEntity>> = repository.getAllStockTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentRiceStockKg: StateFlow<Double> = repository.getLatestStockBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Calendar Events
    val allCalendarEvents: StateFlow<List<CalendarEventEntity>> = repository.getAllCalendarEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Meal Record for Selected Date
    val dateMealRecord: StateFlow<DailyMealRecordEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getDailyMealRecord(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Monthly Meal Records
    val monthMealRecords: StateFlow<List<DailyMealRecordEntity>> = _selectedMonth
        .flatMapLatest { month -> repository.getDailyMealRecordsForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMealRecords: StateFlow<List<DailyMealRecordEntity>> = repository.getAllMealRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit Logs
    val recentAuditLogs: StateFlow<List<AuditLogEntity>> = repository.getRecentAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attention Alerts (Dynamic calculation)
    data class AttentionAlert(
        val severity: AlertSeverity,
        val title: String,
        val description: String,
        val targetTab: String = ""
    )

    enum class AlertSeverity { CRITICAL, WARNING, INFO }

    val attentionAlerts: StateFlow<List<AttentionAlert>> = combine(
        currentRiceStockKg,
        dateMealRecord,
        currentMonthEnrollment,
        dateCookAttendances,
        activeCooks
    ) { stockKg, mealRecord, enrollment, cookAtts, cooks ->
        // Tuple data holder
        Tuple5(stockKg, mealRecord, enrollment, cookAtts, cooks)
    }.combine(currentLanguage) { data, lang ->
        val (stockKg, mealRecord, enrollment, cookAtts, cooks) = data
        val list = mutableListOf<AttentionAlert>()
        val isHi = lang == AppLanguage.HINDI

        // 1. Stock Check
        if (stockKg < 50.0) {
            list.add(
                AttentionAlert(
                    severity = AlertSeverity.CRITICAL,
                    title = if (isHi) "चावल स्टॉक अत्यधिक कम" else "Critical Rice Stock Level",
                    description = if (isHi)
                        "वर्तमान स्टॉक मात्र ${String.format(Locale.getDefault(), "%.1f", stockKg)} कि.ग्रा. शेष है। PDS से नया कोटा प्राप्त करें।"
                    else
                        "Current balance is only ${String.format(Locale.getDefault(), "%.1f", stockKg)} kg. Request immediate PDS quota allocation.",
                    targetTab = "Stock"
                )
            )
        } else if (stockKg < 150.0) {
            list.add(
                AttentionAlert(
                    severity = AlertSeverity.WARNING,
                    title = if (isHi) "कम चावल स्टॉक (Low Stock)" else "Low Rice Stock Warning",
                    description = if (isHi)
                        "वर्तमान स्टॉक ${String.format(Locale.getDefault(), "%.1f", stockKg)} कि.ग्रा. है। अगले 10 दिनों में नया आवंटन आवश्यक होगा।"
                    else
                        "Current balance is ${String.format(Locale.getDefault(), "%.1f", stockKg)} kg. Next allotment required within 10 days.",
                    targetTab = "Stock"
                )
            )
        }

        // 2. Daily Report Check
        if (mealRecord == null) {
            list.add(
                AttentionAlert(
                    severity = AlertSeverity.WARNING,
                    title = if (isHi) "आज की मध्यान्ह भोजन रिपोर्ट लंबित" else "Today's Mid-Day Meal Report Pending",
                    description = if (isHi)
                        "आज के भोजन वितरण एवं उपस्थिति की प्रविष्टि पूर्ण करें।"
                    else
                        "Please complete today's meal serving and attendance record.",
                    targetTab = "Meal"
                )
            )
        }

        // 3. Cook Attendance Check
        if (cooks.isNotEmpty() && cookAtts.isEmpty() && mealRecord == null) {
            list.add(
                AttentionAlert(
                    severity = AlertSeverity.WARNING,
                    title = if (isHi) "रसोइया उपस्थिति दर्ज नहीं" else "Cook Attendance Not Marked",
                    description = if (isHi)
                        "${cooks.size} सक्रिय रसोइयों की उपस्थिति दर्ज करना शेष है।"
                    else
                        "Attendance for ${cooks.size} active cooks needs to be recorded.",
                    targetTab = "Meal"
                )
            )
        }

        // 4. Monthly Enrollment Check
        if (enrollment == null) {
            list.add(
                AttentionAlert(
                    severity = AlertSeverity.WARNING,
                    title = if (isHi) "मासिक छात्र नामांकन दर्ज नहीं" else "Monthly Student Census Missing",
                    description = if (isHi)
                        "वर्तमान माह का छात्र नामांकन डाटा प्रविष्ट करें अथवा पिछले माह से कॉपी करें।"
                    else
                        "Enter current month student enrollment census or copy from previous month.",
                    targetTab = "More"
                )
            )
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class Tuple5<A, B, C, D, E>(
        val a: A,
        val b: B,
        val c: C,
        val d: D,
        val e: E
    )

    // ----------------- MUTATION METHODS -----------------

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        if (date.length >= 7) {
            val month = date.take(7)
            if (_selectedMonth.value != month) {
                _selectedMonth.value = month
            }
        }
    }

    fun saveSchool(school: SchoolEntity) {
        viewModelScope.launch {
            repository.saveSchool(school)
            val app = getApplication<Application>()
            AuthSessionManager.saveSchoolInfo(app, school.udiseCode, school.schoolName)
            val headmaster = repository.getHeadmaster()
            if (headmaster != null && (_currentUserRole.value == UserRole.HEADMASTER || _currentUserId.value == headmaster.userId)) {
                AuthSessionManager.saveLoginSession(app, headmaster)
            }
        }
    }

    fun saveEnrollment(enrollment: MonthlyEnrollmentEntity) {
        viewModelScope.launch {
            repository.saveEnrollment(enrollment)
        }
    }

    fun copyPreviousMonthEnrollment(sourceMonth: String, targetMonth: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.copyPreviousMonthEnrollment(sourceMonth, targetMonth)
            onResult(success)
        }
    }

    fun toggleMonthLock(monthYear: String, isLocked: Boolean) {
        viewModelScope.launch {
            repository.lockMonthEnrollment(monthYear, isLocked)
        }
    }

    fun saveTeacherSummary(summary: MonthlyTeacherEntity) {
        viewModelScope.launch {
            repository.saveTeacherSummary(summary)
        }
    }

    fun copyPreviousMonthTeacherData(sourceMonth: String, targetMonth: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.copyPreviousMonthTeacherData(sourceMonth, targetMonth)
            onResult(success)
        }
    }

    fun seedCooksIfEmpty() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                AppDatabase.populateInitialMasterData(db)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun saveCook(cook: CookEntity) {
        viewModelScope.launch {
            repository.saveCook(cook)
        }
    }

    fun toggleCookStatus(cookId: String, currentStatus: String) {
        viewModelScope.launch {
            val newStatus = if (currentStatus == "ACTIVE") "INACTIVE" else "ACTIVE"
            repository.updateCookStatus(cookId, newStatus)
        }
    }

    fun saveRiceReceipt(receipt: RiceReceiptEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.recordRiceReceipt(receipt)
            onComplete()
        }
    }

    fun editRiceReceipt(receipt: RiceReceiptEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.editRiceReceipt(receipt)
            onComplete()
        }
    }

    fun deleteRiceReceipt(receiptId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteRiceReceipt(receiptId)
            onComplete()
        }
    }

    fun addStockAdjustment(date: String, quantityChangeKg: Double, reason: String, itemType: String = "RICE", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addStockAdjustment(date, quantityChangeKg, reason, itemType)
            onComplete()
        }
    }

    fun addStockTransaction(transaction: StockTransactionEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addStockTransaction(transaction)
            onComplete()
        }
    }

    fun editStockTransaction(transaction: StockTransactionEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.editStockTransaction(transaction)
            onComplete()
        }
    }

    fun deleteStockTransaction(transactionId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteStockTransaction(transactionId)
            onComplete()
        }
    }

    fun recalculateStockLedger(itemType: String = "RICE", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.recalculateStockLedgerBalances(itemType)
            onComplete()
        }
    }

    fun clearAllStockData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllStockData()
            onComplete()
        }
    }

    fun clearStockForItem(itemType: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearStockForItem(itemType)
            onComplete()
        }
    }

    fun setOpeningStock(
        itemType: String,
        date: String,
        openingKg: Double,
        remarks: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.setOpeningStockBalance(itemType, date, openingKg, remarks)
            onComplete()
        }
    }

    fun saveDailyMeal(
        record: DailyMealRecordEntity,
        cookAttendances: List<CookAttendanceEntity>,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.submitDailyMealReport(record, cookAttendances)

            // Trigger continuous Google Drive auto-backup if configured
            try {
                val app = getApplication<Application>()
                val driveService = com.example.cloud.GoogleDriveService(app)
                val config = driveService.getAutoBackupConfig()
                if (config.enabled && config.frequency == com.example.cloud.BackupFrequency.ON_MEAL_SUBMIT) {
                    val db = AppDatabase.getDatabase(app)
                    val engine = com.example.cloud.GoogleDriveBackupEngine(app, db, driveService)
                    engine.performCloudBackup(createdBy = "Auto (Daily Meal)")
                }
            } catch (e: Exception) {
                // Non-blocking background sync catch
            }

            onComplete()
        }
    }

    fun refreshAllData() {
        val currDate = _selectedDate.value
        _selectedDate.value = ""
        _selectedDate.value = currDate
    }

    fun deleteDailyMeal(date: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteDailyMealReport(date)
            onComplete()
        }
    }

    fun saveCalendarEvent(event: CalendarEventEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveCalendarEvent(event)
            onComplete()
        }
    }

    fun deleteCalendarEvent(event: CalendarEventEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(event)
            onComplete()
        }
    }

    fun deleteCalendarEventById(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCalendarEventById(id)
            onComplete()
        }
    }

    fun addVacationPeriod(
        startDate: String,
        endDate: String,
        vacationName: String,
        vacationType: String = "VACATION",
        authority: String = "School Education Dept, Govt of CG",
        orderNumber: String = "",
        onResult: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val count = repository.addVacationPeriod(startDate, endDate, vacationName, vacationType, authority, orderNumber)
            onResult(count)
        }
    }

    fun forceSyncGovtHolidays(year: Int, onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.forceSyncOfficialGovtHolidays(year)
            onResult(count)
        }
    }

    fun importHolidaysFromCsv(csvText: String, onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.importHolidaysCsv(csvText)
            onResult(count)
        }
    }

    fun saveCookingAgency(agency: CookingAgencyEntity) {
        viewModelScope.launch {
            repository.saveAgency(agency)
        }
    }

    fun savePdsShop(shop: PdsShopEntity) {
        viewModelScope.launch {
            repository.savePdsShop(shop)
        }
    }

    fun saveConfigNorms(config: ConfigNormsEntity) {
        viewModelScope.launch {
            repository.saveConfigNorms(config)
        }
    }

    // ----------------- USER AUTH & STAFF MANAGEMENT (RBAC) -----------------

    fun loginWithPin(pin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByPin(pin.trim())
            if (user != null && user.isActive) {
                val app = getApplication<Application>()
                AuthSessionManager.saveLoginSession(app, user)
                _currentUserId.value = user.userId
                _currentUserRole.value = UserRole.fromCode(user.role)
                _isUserLoggedIn.value = true
                repository.logAudit(
                    "LOGIN_SUCCESS",
                    "AUTH",
                    user.userId,
                    "",
                    "${user.name} (${user.role})",
                    "User logged in via 4-Digit Quick PIN"
                )
                onResult(true, null)
            } else {
                onResult(false, if (_currentLanguage.value == AppLanguage.HINDI) "गलत 4-अंकीय पिन दर्ज किया गया!" else "Incorrect 4-digit PIN entered!")
            }
        }
    }

    fun switchCurrentUser(user: UserAccountEntity, enteredPin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (user.pin == enteredPin.trim()) {
                val app = getApplication<Application>()
                AuthSessionManager.saveLoginSession(app, user)
                _currentUserId.value = user.userId
                _currentUserRole.value = UserRole.fromCode(user.role)
                _isUserLoggedIn.value = true
                repository.logAudit(
                    "USER_SWITCH",
                    "AUTH",
                    user.userId,
                    "",
                    "${user.name} (${user.role})",
                    "Switched active user to ${user.name}"
                )
                onResult(true, null)
            } else {
                onResult(false, if (_currentLanguage.value == AppLanguage.HINDI) "गलत 4-अंकीय पिन!" else "Incorrect PIN for ${user.name}!")
            }
        }
    }

    fun logout() {
        val app = getApplication<Application>()
        AuthSessionManager.logout(app)
        _isUserLoggedIn.value = false
    }

    fun registerHeadmasterAndSchool(
        udiseCode: String,
        schoolName: String,
        schoolType: String,
        stateName: String,
        districtName: String,
        blockName: String,
        clusterName: String,
        villageName: String,
        headmasterName: String,
        mobile: String,
        email: String,
        pin: String,
        securityQuestion: String,
        securityAnswer: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val school = SchoolEntity(
                    schoolId = "SCH-$udiseCode",
                    udiseCode = udiseCode.trim(),
                    schoolName = schoolName.trim(),
                    stateName = stateName.trim(),
                    districtName = districtName.trim(),
                    blockName = blockName.trim(),
                    clusterName = clusterName.trim(),
                    villageName = villageName.trim(),
                    schoolType = schoolType,
                    headTeacherName = headmasterName.trim(),
                    headTeacherMobile = mobile.trim(),
                    status = "ACTIVE"
                )

                val headmasterUser = UserAccountEntity(
                    userId = "USER-HM-$udiseCode",
                    schoolId = "SCH-$udiseCode",
                    udiseCode = udiseCode.trim(),
                    name = headmasterName.trim(),
                    role = UserRole.HEADMASTER.code,
                    mobile = mobile.trim(),
                    email = email.trim(),
                    pin = pin.trim(),
                    securityQuestion = securityQuestion,
                    securityAnswer = securityAnswer.trim().lowercase(),
                    isActive = true,
                    createdAt = getIstDateFormat().format(Date()),
                    lastLogin = getIstDateFormat().format(Date())
                )

                repository.registerHeadmasterAndSchool(school, headmasterUser)

                val app = getApplication<Application>()
                AuthSessionManager.saveSchoolInfo(app, udiseCode.trim(), schoolName.trim())
                AuthSessionManager.setRegistered(app, true)
                AuthSessionManager.saveLoginSession(app, headmasterUser)

                // Configure Google Drive backup with registered email
                if (email.isNotBlank()) {
                    com.example.cloud.GoogleDriveService(app).updateAccount(email.trim(), headmasterName.trim())
                }

                _isSchoolRegistered.value = true
                _currentUserId.value = headmasterUser.userId
                _currentUserRole.value = UserRole.HEADMASTER
                _isUserLoggedIn.value = true

                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage)
            }
        }
    }

    fun saveUserStaff(user: UserAccountEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveUser(user)
            val app = getApplication<Application>()
            if (_currentUserId.value == user.userId) {
                AuthSessionManager.saveLoginSession(app, user)
            }
            onComplete()
        }
    }

    fun deleteUserStaff(userId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            onComplete()
        }
    }

    fun resetPinForUser(userId: String, newPin: String, securityAnswer: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.getActiveUsers().find { it.userId == userId }
            if (user == null) {
                onResult(false, if (_currentLanguage.value == AppLanguage.HINDI) "उपयोगकर्ता नहीं मिला!" else "User not found!")
                return@launch
            }
            if (user.securityAnswer.trim().lowercase() != securityAnswer.trim().lowercase()) {
                onResult(false, if (_currentLanguage.value == AppLanguage.HINDI) "सुरक्षा उत्तर गलत है!" else "Incorrect security answer!")
                return@launch
            }
            val updatedUser = user.copy(pin = newPin.trim())
            repository.saveUser(updatedUser)
            onResult(true, null)
        }
    }

    fun syncDataNow(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = syncManager.syncPendingData()
            onComplete(result)
        }
    }

    fun pullCloudData(udiseCode: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = syncManager.initialPullFromCloud(udiseCode)
            onComplete(result)
        }
    }

    fun logoutUser() {
        firebaseAuthRepo.signOut()
        val app = getApplication<Application>()
        AuthSessionManager.logout(app)
        _isUserLoggedIn.value = false
    }
}
