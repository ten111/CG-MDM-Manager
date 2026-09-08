package com.example.cloud

import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object GoogleDriveBackupSerializer {

    fun serialize(payload: PoshanBackupPayload): String {
        val root = JSONObject()

        // Metadata
        val meta = JSONObject().apply {
            put("backupId", payload.metadata.backupId)
            put("fileId", payload.metadata.fileId)
            put("fileName", payload.metadata.fileName)
            put("rootFolder", payload.metadata.rootFolder)
            put("monthFolder", payload.metadata.monthFolder)
            put("folderPath", payload.metadata.folderPath)
            put("udiseCode", payload.metadata.udiseCode)
            put("schoolName", payload.metadata.schoolName)
            put("timestamp", payload.metadata.timestamp)
            put("timestampMillis", payload.metadata.timestampMillis)
            put("createdBy", payload.metadata.createdBy)
            put("appVersion", payload.metadata.appVersion)
            put("databaseVersion", payload.metadata.databaseVersion)
            put("sizeBytes", payload.metadata.sizeBytes)
            put("checksumSha256", payload.metadata.checksumSha256)
            put("isCloudBackup", payload.metadata.isCloudBackup)

            val counts = JSONObject().apply {
                put("studentsCount", payload.metadata.recordCounts.studentsCount)
                put("teachersCount", payload.metadata.recordCounts.teachersCount)
                put("cooksCount", payload.metadata.recordCounts.cooksCount)
                put("mealsCount", payload.metadata.recordCounts.mealsCount)
                put("receiptsCount", payload.metadata.recordCounts.receiptsCount)
                put("stockTransactionsCount", payload.metadata.recordCounts.stockTransactionsCount)
                put("auditLogsCount", payload.metadata.recordCounts.auditLogsCount)
                put("calendarEventsCount", payload.metadata.recordCounts.calendarEventsCount)
            }
            put("recordCounts", counts)
        }
        root.put("metadata", meta)

        // School
        payload.school?.let { s ->
            val schoolObj = JSONObject().apply {
                put("schoolId", s.schoolId)
                put("udiseCode", s.udiseCode)
                put("schoolName", s.schoolName)
                put("stateName", s.stateName)
                put("districtName", s.districtName)
                put("blockName", s.blockName)
                put("clusterName", s.clusterName)
                put("villageName", s.villageName)
                put("schoolType", s.schoolType)
                put("headTeacherName", s.headTeacherName)
                put("headTeacherMobile", s.headTeacherMobile)
                put("status", s.status)
            }
            root.put("school", schoolObj)
        }

        // Calendar Events
        val eventsArray = JSONArray()
        for (e in payload.calendarEvents) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("eventDate", e.eventDate)
                put("eventName", e.eventName)
                put("eventType", e.eventType)
                put("isMealReportingRequired", e.isMealReportingRequired)
                put("orderNumber", e.orderNumber)
                put("authority", e.authority)
                put("remarks", e.remarks)
            }
            eventsArray.put(obj)
        }
        root.put("calendarEvents", eventsArray)

        // Monthly Enrollments
        val enrollArray = JSONArray()
        for (en in payload.enrollments) {
            val obj = JSONObject().apply {
                put("monthYear", en.monthYear)
                put("schoolId", en.schoolId)
                put("academicYear", en.academicYear)
                put("totalEnrollment", en.totalEnrollment)
                put("totalBoys", en.totalBoys)
                put("totalGirls", en.totalGirls)
                put("scCount", en.scCount)
                put("stCount", en.stCount)
                put("obcCount", en.obcCount)
                put("generalCount", en.generalCount)
                put("cwsnCount", en.cwsnCount)
                put("minorityCount", en.minorityCount)
                put("pvtgCount", en.pvtgCount)
                put("classBreakupJson", en.classBreakupJson)
                put("isLocked", en.isLocked)
                put("createdBy", en.createdBy)
                put("createdAt", en.createdAt)
                put("updatedAt", en.updatedAt)
            }
            enrollArray.put(obj)
        }
        root.put("enrollments", enrollArray)

        // Teachers
        val teacherArray = JSONArray()
        for (t in payload.teachers) {
            val obj = JSONObject().apply {
                put("monthYear", t.monthYear)
                put("schoolId", t.schoolId)
                put("academicYear", t.academicYear)
                put("totalTeachers", t.totalTeachers)
                put("maleCount", t.maleCount)
                put("femaleCount", t.femaleCount)
                put("trainedCount", t.trainedCount)
                put("untrainedCount", t.untrainedCount)
                put("scCount", t.scCount)
                put("stCount", t.stCount)
                put("obcCount", t.obcCount)
                put("generalCount", t.generalCount)
                put("stTrainedMale", t.stTrainedMale)
                put("stTrainedFemale", t.stTrainedFemale)
                put("stUntrainedMale", t.stUntrainedMale)
                put("stUntrainedFemale", t.stUntrainedFemale)
                put("scTrainedMale", t.scTrainedMale)
                put("scTrainedFemale", t.scTrainedFemale)
                put("scUntrainedMale", t.scUntrainedMale)
                put("scUntrainedFemale", t.scUntrainedFemale)
                put("obcTrainedMale", t.obcTrainedMale)
                put("obcTrainedFemale", t.obcTrainedFemale)
                put("obcUntrainedMale", t.obcUntrainedMale)
                put("obcUntrainedFemale", t.obcUntrainedFemale)
                put("genTrainedMale", t.genTrainedMale)
                put("genTrainedFemale", t.genTrainedFemale)
                put("genUntrainedMale", t.genUntrainedMale)
                put("genUntrainedFemale", t.genUntrainedFemale)
                put("isLocked", t.isLocked)
                put("createdBy", t.createdBy)
                put("createdAt", t.createdAt)
                put("updatedAt", t.updatedAt)
            }
            teacherArray.put(obj)
        }
        root.put("teachers", teacherArray)

        // Cooks
        val cookArray = JSONArray()
        for (c in payload.cooks) {
            val obj = JSONObject().apply {
                put("cookId", c.cookId)
                put("schoolId", c.schoolId)
                put("name", c.name)
                put("guardianName", c.guardianName)
                put("address", c.address)
                put("village", c.village)
                put("mobile", c.mobile)
                put("bankAccountNo", c.bankAccountNo)
                put("maskedAccountNo", c.maskedAccountNo)
                put("bankName", c.bankName)
                put("branchName", c.branchName)
                put("ifscCode", c.ifscCode)
                put("joiningDate", c.joiningDate)
                put("status", c.status)
                put("associatedAgency", c.associatedAgency)
            }
            cookArray.put(obj)
        }
        root.put("cooks", cookArray)

        // Cook Attendances
        val cookAttArray = JSONArray()
        for (ca in payload.cookAttendances) {
            val obj = JSONObject().apply {
                put("id", ca.id)
                put("date", ca.date)
                put("cookId", ca.cookId)
                put("cookName", ca.cookName)
                put("isPresent", ca.isPresent)
                put("absenceReason", ca.absenceReason)
                put("remarks", ca.remarks)
                put("schoolId", ca.schoolId)
            }
            cookAttArray.put(obj)
        }
        root.put("cookAttendances", cookAttArray)

        // Cooking Agencies
        val agencyArray = JSONArray()
        for (ag in payload.cookingAgencies) {
            val obj = JSONObject().apply {
                put("agencyId", ag.agencyId)
                put("name", ag.name)
                put("contactPerson", ag.contactPerson)
                put("mobile", ag.mobile)
                put("address", ag.address)
                put("villageOrCity", ag.villageOrCity)
                put("block", ag.block)
                put("district", ag.district)
                put("maskedAccountNo", ag.maskedAccountNo)
                put("bankName", ag.bankName)
                put("branchName", ag.branchName)
                put("ifscCode", ag.ifscCode)
                put("agreementOrderNo", ag.agreementOrderNo)
                put("agreementStartDate", ag.agreementStartDate)
                put("agreementEndDate", ag.agreementEndDate)
                put("status", ag.status)
                put("associatedSchools", ag.associatedSchools)
            }
            agencyArray.put(obj)
        }
        root.put("cookingAgencies", agencyArray)

        // PDS Shops
        val pdsArray = JSONArray()
        for (p in payload.pdsShops) {
            val obj = JSONObject().apply {
                put("pdsId", p.pdsId)
                put("fpsNumber", p.fpsNumber)
                put("shopName", p.shopName)
                put("dealerName", p.dealerName)
                put("mobile", p.mobile)
                put("address", p.address)
                put("village", p.village)
                put("block", p.block)
                put("district", p.district)
                put("licenseNumber", p.licenseNumber)
                put("associatedSchool", p.associatedSchool)
                put("status", p.status)
            }
            pdsArray.put(obj)
        }
        root.put("pdsShops", pdsArray)

        // Rice Receipts
        val receiptArray = JSONArray()
        for (r in payload.riceReceipts) {
            val obj = JSONObject().apply {
                put("receiptId", r.receiptId)
                put("schoolId", r.schoolId)
                put("pdsId", r.pdsId)
                put("pdsShopName", r.pdsShopName)
                put("receiptDate", r.receiptDate)
                put("quantityKg", r.quantityKg)
                put("challanNumber", r.challanNumber)
                put("govtRefNumber", r.govtRefNumber)
                put("remarks", r.remarks)
                put("photoUri", r.photoUri)
                put("receivedBy", r.receivedBy)
                put("status", r.status)
            }
            receiptArray.put(obj)
        }
        root.put("riceReceipts", receiptArray)

        // Stock Transactions
        val transArray = JSONArray()
        for (st in payload.stockTransactions) {
            val obj = JSONObject().apply {
                put("transactionId", st.transactionId)
                put("schoolId", st.schoolId)
                put("transactionDate", st.transactionDate)
                put("itemType", st.itemType)
                put("transactionType", st.transactionType)
                put("quantityKg", st.quantityKg)
                put("runningBalanceKg", st.runningBalanceKg)
                put("referenceId", st.referenceId)
                put("description", st.description)
                put("timestamp", st.timestamp)
            }
            transArray.put(obj)
        }
        root.put("stockTransactions", transArray)

        // Daily Meal Records
        val mealsArray = JSONArray()
        for (dm in payload.dailyMeals) {
            val obj = JSONObject().apply {
                put("date", dm.date)
                put("schoolId", dm.schoolId)
                put("academicYear", dm.academicYear)
                put("calendarStatus", dm.calendarStatus)
                put("holidayReason", dm.holidayReason)
                put("enrolledStudents", dm.enrolledStudents)
                put("enrolledBoys", dm.enrolledBoys)
                put("enrolledGirls", dm.enrolledGirls)
                put("boysPresent", dm.boysPresent)
                put("girlsPresent", dm.girlsPresent)
                put("studentsPresent", dm.studentsPresent)
                put("mealServed", dm.mealServed)
                put("boysServed", dm.boysServed)
                put("girlsServed", dm.girlsServed)
                put("studentsServed", dm.studentsServed)
                put("menuDetails", dm.menuDetails)
                put("cooksPresentCount", dm.cooksPresentCount)
                put("totalCooksCount", dm.totalCooksCount)
                put("tastingDone", dm.tastingDone)
                put("tastedBy", dm.tastedBy)
                put("tasteQuality", dm.tasteQuality)
                put("hygieneChecked", dm.hygieneChecked)
                put("photoUri", dm.photoUri)
                put("riceConsumedKg", dm.riceConsumedKg)
                put("customItemsUsedJson", dm.customItemsUsedJson)
                put("syncStatus", dm.syncStatus)
                put("createdAt", dm.createdAt)
                put("updatedAt", dm.updatedAt)
            }
            mealsArray.put(obj)
        }
        root.put("dailyMeals", mealsArray)

        // Audit Logs
        val auditArray = JSONArray()
        for (al in payload.auditLogs) {
            val obj = JSONObject().apply {
                put("logId", al.logId)
                put("userName", al.userName)
                put("userRole", al.userRole)
                put("action", al.action)
                put("moduleName", al.moduleName)
                put("recordId", al.recordId)
                put("previousValue", al.previousValue)
                put("newValue", al.newValue)
                put("timestamp", al.timestamp)
                put("details", al.details)
            }
            auditArray.put(obj)
        }
        root.put("auditLogs", auditArray)

        // Config Norms
        payload.configNorms?.let { cn ->
            val cnObj = JSONObject().apply {
                put("configId", cn.configId)
                put("stateName", cn.stateName)
                put("primaryRiceNormGrams", cn.primaryRiceNormGrams)
                put("upperPrimaryRiceNormGrams", cn.upperPrimaryRiceNormGrams)
                put("pulseNormGrams", cn.pulseNormGrams)
                put("oilNormGrams", cn.oilNormGrams)
                put("vegetableNormGrams", cn.vegetableNormGrams)
                put("saltNormGrams", cn.saltNormGrams)
                put("primaryReimbursementRate", cn.primaryReimbursementRate)
                put("middleReimbursementRate", cn.middleReimbursementRate)
                put("reimbursementRate", cn.reimbursementRate)
                put("cookingCostRate", cn.cookingCostRate)
                put("lowStockThresholdKg", cn.lowStockThresholdKg)
                put("goodStockThresholdDays", cn.goodStockThresholdDays)
                put("lowStockThresholdDays", cn.lowStockThresholdDays)
                put("customItemsJson", cn.customItemsJson)
            }
            root.put("configNorms", cnObj)
        }

        // Users
        val usersArray = JSONArray()
        for (u in payload.users) {
            val obj = JSONObject().apply {
                put("userId", u.userId)
                put("schoolId", u.schoolId)
                put("udiseCode", u.udiseCode)
                put("name", u.name)
                put("role", u.role)
                put("mobile", u.mobile)
                put("email", u.email)
                put("pin", u.pin)
                put("securityQuestion", u.securityQuestion)
                put("securityAnswer", u.securityAnswer)
                put("isActive", u.isActive)
                put("createdAt", u.createdAt)
                put("lastLogin", u.lastLogin)
            }
            usersArray.put(obj)
        }
        root.put("users", usersArray)

        return root.toString(2)
    }

    fun deserialize(jsonStr: String): PoshanBackupPayload {
        val root = JSONObject(jsonStr)

        val metaObj = root.optJSONObject("metadata") ?: JSONObject()
        val countsObj = metaObj.optJSONObject("recordCounts") ?: JSONObject()

        val recordCounts = BackupRecordCounts(
            studentsCount = countsObj.optInt("studentsCount", 0),
            teachersCount = countsObj.optInt("teachersCount", 0),
            cooksCount = countsObj.optInt("cooksCount", 0),
            mealsCount = countsObj.optInt("mealsCount", 0),
            receiptsCount = countsObj.optInt("receiptsCount", 0),
            stockTransactionsCount = countsObj.optInt("stockTransactionsCount", 0),
            auditLogsCount = countsObj.optInt("auditLogsCount", 0),
            calendarEventsCount = countsObj.optInt("calendarEventsCount", 0)
        )

        val metadata = PoshanBackupMetadata(
            backupId = metaObj.optString("backupId", "BACKUP-UNKNOWN"),
            fileId = metaObj.optString("fileId", ""),
            fileName = metaObj.optString("fileName", "PM_POSHAN_BACKUP.json"),
            rootFolder = metaObj.optString("rootFolder", "CG-MDM Manager Mobile"),
            monthFolder = metaObj.optString("monthFolder", "08-2026"),
            folderPath = metaObj.optString("folderPath", "CG-MDM Manager Mobile/08-2026"),
            udiseCode = metaObj.optString("udiseCode", ""),
            schoolName = metaObj.optString("schoolName", ""),
            timestamp = metaObj.optString("timestamp", ""),
            timestampMillis = metaObj.optLong("timestampMillis", System.currentTimeMillis()),
            createdBy = metaObj.optString("createdBy", "Headmaster"),
            appVersion = metaObj.optString("appVersion", "2.4.0"),
            databaseVersion = metaObj.optInt("databaseVersion", 9),
            recordCounts = recordCounts,
            sizeBytes = metaObj.optLong("sizeBytes", 0),
            checksumSha256 = metaObj.optString("checksumSha256", ""),
            isCloudBackup = metaObj.optBoolean("isCloudBackup", true)
        )

        // School
        var school: SchoolEntity? = null
        root.optJSONObject("school")?.let { s ->
            school = SchoolEntity(
                schoolId = s.optString("schoolId", "SCH-001"),
                udiseCode = s.optString("udiseCode", "22080100308"),
                schoolName = s.optString("schoolName", ""),
                stateName = s.optString("stateName", "Chhattisgarh"),
                districtName = s.optString("districtName", "Kawardha"),
                blockName = s.optString("blockName", "Bodla"),
                clusterName = s.optString("clusterName", "Bodla"),
                villageName = s.optString("villageName", "Bodla"),
                schoolType = s.optString("schoolType", "Middle with primary 1-8"),
                headTeacherName = s.optString("headTeacherName", ""),
                headTeacherMobile = s.optString("headTeacherMobile", ""),
                status = s.optString("status", "ACTIVE")
            )
        }

        // Calendar Events
        val calendarEvents = mutableListOf<CalendarEventEntity>()
        root.optJSONArray("calendarEvents")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                calendarEvents.add(
                    CalendarEventEntity(
                        id = obj.optLong("id", 0L),
                        eventDate = obj.optString("eventDate"),
                        eventName = obj.optString("eventName"),
                        eventType = obj.optString("eventType", "HOLIDAY"),
                        isMealReportingRequired = obj.optBoolean("isMealReportingRequired", false),
                        orderNumber = obj.optString("orderNumber", ""),
                        authority = obj.optString("authority", ""),
                        remarks = obj.optString("remarks", "")
                    )
                )
            }
        }

        // Enrollments
        val enrollments = mutableListOf<MonthlyEnrollmentEntity>()
        root.optJSONArray("enrollments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                enrollments.add(
                    MonthlyEnrollmentEntity(
                        monthYear = obj.optString("monthYear"),
                        schoolId = obj.optString("schoolId"),
                        academicYear = obj.optString("academicYear", "2026-27"),
                        totalEnrollment = obj.optInt("totalEnrollment"),
                        totalBoys = obj.optInt("totalBoys"),
                        totalGirls = obj.optInt("totalGirls"),
                        scCount = obj.optInt("scCount"),
                        stCount = obj.optInt("stCount"),
                        obcCount = obj.optInt("obcCount"),
                        generalCount = obj.optInt("generalCount"),
                        cwsnCount = obj.optInt("cwsnCount"),
                        minorityCount = obj.optInt("minorityCount"),
                        pvtgCount = obj.optInt("pvtgCount"),
                        classBreakupJson = obj.optString("classBreakupJson", ""),
                        isLocked = obj.optBoolean("isLocked", false),
                        createdBy = obj.optString("createdBy", "Headmaster"),
                        createdAt = obj.optString("createdAt", ""),
                        updatedAt = obj.optString("updatedAt", "")
                    )
                )
            }
        }

        // Teachers
        val teachers = mutableListOf<MonthlyTeacherEntity>()
        root.optJSONArray("teachers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                teachers.add(
                    MonthlyTeacherEntity(
                        monthYear = obj.optString("monthYear"),
                        schoolId = obj.optString("schoolId"),
                        academicYear = obj.optString("academicYear", "2026-27"),
                        totalTeachers = obj.optInt("totalTeachers"),
                        maleCount = obj.optInt("maleCount"),
                        femaleCount = obj.optInt("femaleCount"),
                        trainedCount = obj.optInt("trainedCount"),
                        untrainedCount = obj.optInt("untrainedCount"),
                        scCount = obj.optInt("scCount"),
                        stCount = obj.optInt("stCount"),
                        obcCount = obj.optInt("obcCount"),
                        generalCount = obj.optInt("generalCount"),
                        stTrainedMale = obj.optInt("stTrainedMale"),
                        stTrainedFemale = obj.optInt("stTrainedFemale"),
                        stUntrainedMale = obj.optInt("stUntrainedMale"),
                        stUntrainedFemale = obj.optInt("stUntrainedFemale"),
                        scTrainedMale = obj.optInt("scTrainedMale"),
                        scTrainedFemale = obj.optInt("scTrainedFemale"),
                        scUntrainedMale = obj.optInt("scUntrainedMale"),
                        scUntrainedFemale = obj.optInt("scUntrainedFemale"),
                        obcTrainedMale = obj.optInt("obcTrainedMale"),
                        obcTrainedFemale = obj.optInt("obcTrainedFemale"),
                        obcUntrainedMale = obj.optInt("obcUntrainedMale"),
                        obcUntrainedFemale = obj.optInt("obcUntrainedFemale"),
                        genTrainedMale = obj.optInt("genTrainedMale"),
                        genTrainedFemale = obj.optInt("genTrainedFemale"),
                        genUntrainedMale = obj.optInt("genUntrainedMale"),
                        genUntrainedFemale = obj.optInt("genUntrainedFemale"),
                        isLocked = obj.optBoolean("isLocked", false),
                        createdBy = obj.optString("createdBy", "Headmaster"),
                        createdAt = obj.optString("createdAt", ""),
                        updatedAt = obj.optString("updatedAt", "")
                    )
                )
            }
        }

        // Cooks
        val cooks = mutableListOf<CookEntity>()
        root.optJSONArray("cooks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                cooks.add(
                    CookEntity(
                        cookId = obj.optString("cookId"),
                        schoolId = obj.optString("schoolId"),
                        name = obj.optString("name"),
                        guardianName = obj.optString("guardianName", ""),
                        address = obj.optString("address", ""),
                        village = obj.optString("village", ""),
                        mobile = obj.optString("mobile", ""),
                        bankAccountNo = obj.optString("bankAccountNo", ""),
                        maskedAccountNo = obj.optString("maskedAccountNo", ""),
                        bankName = obj.optString("bankName", ""),
                        branchName = obj.optString("branchName", ""),
                        ifscCode = obj.optString("ifscCode", ""),
                        joiningDate = obj.optString("joiningDate", "2024-06-15"),
                        status = obj.optString("status", "ACTIVE"),
                        associatedAgency = obj.optString("associatedAgency", "")
                    )
                )
            }
        }

        // Cook Attendances
        val cookAttendances = mutableListOf<CookAttendanceEntity>()
        root.optJSONArray("cookAttendances")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                cookAttendances.add(
                    CookAttendanceEntity(
                        id = obj.optLong("id", 0L),
                        date = obj.optString("date"),
                        cookId = obj.optString("cookId"),
                        cookName = obj.optString("cookName"),
                        isPresent = obj.optBoolean("isPresent", true),
                        absenceReason = obj.optString("absenceReason", ""),
                        remarks = obj.optString("remarks", ""),
                        schoolId = obj.optString("schoolId", "")
                    )
                )
            }
        }

        // Cooking Agencies
        val cookingAgencies = mutableListOf<CookingAgencyEntity>()
        root.optJSONArray("cookingAgencies")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                cookingAgencies.add(
                    CookingAgencyEntity(
                        agencyId = obj.optString("agencyId"),
                        name = obj.optString("name"),
                        contactPerson = obj.optString("contactPerson", ""),
                        mobile = obj.optString("mobile", ""),
                        address = obj.optString("address", ""),
                        villageOrCity = obj.optString("villageOrCity", ""),
                        block = obj.optString("block", ""),
                        district = obj.optString("district", ""),
                        maskedAccountNo = obj.optString("maskedAccountNo", ""),
                        bankName = obj.optString("bankName", ""),
                        branchName = obj.optString("branchName", ""),
                        ifscCode = obj.optString("ifscCode", ""),
                        agreementOrderNo = obj.optString("agreementOrderNo", ""),
                        agreementStartDate = obj.optString("agreementStartDate", ""),
                        agreementEndDate = obj.optString("agreementEndDate", ""),
                        status = obj.optString("status", "ACTIVE"),
                        associatedSchools = obj.optString("associatedSchools", "")
                    )
                )
            }
        }

        // PDS Shops
        val pdsShops = mutableListOf<PdsShopEntity>()
        root.optJSONArray("pdsShops")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                pdsShops.add(
                    PdsShopEntity(
                        pdsId = obj.optString("pdsId"),
                        fpsNumber = obj.optString("fpsNumber", ""),
                        shopName = obj.optString("shopName"),
                        dealerName = obj.optString("dealerName", ""),
                        mobile = obj.optString("mobile", ""),
                        address = obj.optString("address", ""),
                        village = obj.optString("village", ""),
                        block = obj.optString("block", ""),
                        district = obj.optString("district", ""),
                        licenseNumber = obj.optString("licenseNumber", ""),
                        associatedSchool = obj.optString("associatedSchool", ""),
                        status = obj.optString("status", "ACTIVE")
                    )
                )
            }
        }

        // Rice Receipts
        val riceReceipts = mutableListOf<RiceReceiptEntity>()
        root.optJSONArray("riceReceipts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                riceReceipts.add(
                    RiceReceiptEntity(
                        receiptId = obj.optString("receiptId"),
                        schoolId = obj.optString("schoolId"),
                        pdsId = obj.optString("pdsId", ""),
                        pdsShopName = obj.optString("pdsShopName", ""),
                        receiptDate = obj.optString("receiptDate"),
                        quantityKg = obj.optDouble("quantityKg", 0.0),
                        challanNumber = obj.optString("challanNumber", ""),
                        govtRefNumber = obj.optString("govtRefNumber", ""),
                        remarks = obj.optString("remarks", ""),
                        photoUri = obj.optString("photoUri", ""),
                        receivedBy = obj.optString("receivedBy", "Headmaster"),
                        status = obj.optString("status", "VERIFIED")
                    )
                )
            }
        }

        // Stock Transactions
        val stockTransactions = mutableListOf<StockTransactionEntity>()
        root.optJSONArray("stockTransactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                stockTransactions.add(
                    StockTransactionEntity(
                        transactionId = obj.optLong("transactionId", 0L),
                        schoolId = obj.optString("schoolId"),
                        transactionDate = obj.optString("transactionDate"),
                        itemType = obj.optString("itemType", "RICE"),
                        transactionType = obj.optString("transactionType", "USAGE"),
                        quantityKg = obj.optDouble("quantityKg", 0.0),
                        runningBalanceKg = obj.optDouble("runningBalanceKg", 0.0),
                        referenceId = obj.optString("referenceId", ""),
                        description = obj.optString("description", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        // Daily Meals
        val dailyMeals = mutableListOf<DailyMealRecordEntity>()
        root.optJSONArray("dailyMeals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                dailyMeals.add(
                    DailyMealRecordEntity(
                        date = obj.optString("date"),
                        schoolId = obj.optString("schoolId"),
                        academicYear = obj.optString("academicYear", "2026-27"),
                        calendarStatus = obj.optString("calendarStatus", "WORKING_DAY"),
                        holidayReason = obj.optString("holidayReason", ""),
                        enrolledStudents = obj.optInt("enrolledStudents"),
                        enrolledBoys = obj.optInt("enrolledBoys"),
                        enrolledGirls = obj.optInt("enrolledGirls"),
                        boysPresent = obj.optInt("boysPresent"),
                        girlsPresent = obj.optInt("girlsPresent"),
                        studentsPresent = obj.optInt("studentsPresent"),
                        mealServed = obj.optBoolean("mealServed", true),
                        boysServed = obj.optInt("boysServed"),
                        girlsServed = obj.optInt("girlsServed"),
                        studentsServed = obj.optInt("studentsServed"),
                        menuDetails = obj.optString("menuDetails", "Rice, Dal"),
                        cooksPresentCount = obj.optInt("cooksPresentCount"),
                        totalCooksCount = obj.optInt("totalCooksCount"),
                        tastingDone = obj.optBoolean("tastingDone", true),
                        tastedBy = obj.optString("tastedBy", "Head Teacher & Cook"),
                        tasteQuality = obj.optString("tasteQuality", "GOOD"),
                        hygieneChecked = obj.optBoolean("hygieneChecked", true),
                        photoUri = obj.optString("photoUri", ""),
                        riceConsumedKg = obj.optDouble("riceConsumedKg", 0.0),
                        customItemsUsedJson = obj.optString("customItemsUsedJson", ""),
                        syncStatus = obj.optString("syncStatus", "SYNCED"),
                        createdAt = obj.optString("createdAt", ""),
                        updatedAt = obj.optString("updatedAt", "")
                    )
                )
            }
        }

        // Audit Logs
        val auditLogs = mutableListOf<AuditLogEntity>()
        root.optJSONArray("auditLogs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                auditLogs.add(
                    AuditLogEntity(
                        logId = obj.optLong("logId", 0L),
                        userName = obj.optString("userName", ""),
                        userRole = obj.optString("userRole", "HEADMASTER"),
                        action = obj.optString("action", "BACKUP"),
                        moduleName = obj.optString("moduleName", "CLOUD_BACKUP"),
                        recordId = obj.optString("recordId", ""),
                        previousValue = obj.optString("previousValue", ""),
                        newValue = obj.optString("newValue", ""),
                        timestamp = obj.optString("timestamp", ""),
                        details = obj.optString("details", "")
                    )
                )
            }
        }

        // Config Norms
        var configNorms: ConfigNormsEntity? = null
        root.optJSONObject("configNorms")?.let { cn ->
            configNorms = ConfigNormsEntity(
                configId = cn.optString("configId", "DEFAULT_CG_CONFIG"),
                stateName = cn.optString("stateName", "Chhattisgarh"),
                primaryRiceNormGrams = cn.optDouble("primaryRiceNormGrams", 100.0),
                upperPrimaryRiceNormGrams = cn.optDouble("upperPrimaryRiceNormGrams", 150.0),
                pulseNormGrams = cn.optDouble("pulseNormGrams", 30.0),
                oilNormGrams = cn.optDouble("oilNormGrams", 7.5),
                vegetableNormGrams = cn.optDouble("vegetableNormGrams", 75.0),
                saltNormGrams = cn.optDouble("saltNormGrams", 5.0),
                primaryReimbursementRate = cn.optDouble("primaryReimbursementRate", 5.45),
                middleReimbursementRate = cn.optDouble("middleReimbursementRate", 8.17),
                reimbursementRate = cn.optDouble("reimbursementRate", 10.17),
                cookingCostRate = cn.optDouble("cookingCostRate", 10.17),
                lowStockThresholdKg = cn.optDouble("lowStockThresholdKg", 50.0),
                goodStockThresholdDays = cn.optInt("goodStockThresholdDays", 15),
                lowStockThresholdDays = cn.optInt("lowStockThresholdDays", 5),
                customItemsJson = cn.optString("customItemsJson", "")
            )
        }

        // Users
        val users = mutableListOf<UserAccountEntity>()
        root.optJSONArray("users")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                users.add(
                    UserAccountEntity(
                        userId = obj.optString("userId"),
                        schoolId = obj.optString("schoolId"),
                        udiseCode = obj.optString("udiseCode"),
                        name = obj.optString("name"),
                        role = obj.optString("role", "HEADMASTER"),
                        mobile = obj.optString("mobile", ""),
                        email = obj.optString("email", ""),
                        pin = obj.optString("pin", "1234"),
                        securityQuestion = obj.optString("securityQuestion", "What is your school name?"),
                        securityAnswer = obj.optString("securityAnswer", ""),
                        isActive = obj.optBoolean("isActive", true),
                        createdAt = obj.optString("createdAt", ""),
                        lastLogin = obj.optString("lastLogin", "")
                    )
                )
            }
        }

        return PoshanBackupPayload(
            metadata = metadata,
            school = school,
            calendarEvents = calendarEvents,
            enrollments = enrollments,
            teachers = teachers,
            cooks = cooks,
            cookAttendances = cookAttendances,
            cookingAgencies = cookingAgencies,
            pdsShops = pdsShops,
            riceReceipts = riceReceipts,
            stockTransactions = stockTransactions,
            dailyMeals = dailyMeals,
            auditLogs = auditLogs,
            configNorms = configNorms,
            users = users
        )
    }

    fun computeSha256(content: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(content.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
