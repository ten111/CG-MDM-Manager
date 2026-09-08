package com.example.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val label: String) {
    HINDI("hi", "हिन्दी", "हिन्दी (HI)"),
    ENGLISH("en", "English", "English (EN)");

    fun toggle(): AppLanguage = if (this == HINDI) ENGLISH else HINDI
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.HINDI }

@Composable
fun stringResource(hi: String, en: String): String {
    return if (LocalAppLanguage.current == AppLanguage.HINDI) hi else en
}

fun getString(lang: AppLanguage, hi: String, en: String): String {
    return if (lang == AppLanguage.HINDI) hi else en
}

object AppStrings {
    // Top Bar & Branding
    fun appTitle(lang: AppLanguage, schoolName: String?) =
        schoolName?.ifBlank { null } ?: if (lang == AppLanguage.HINDI) "शाला प्रबंधन"
        else "School Manager"

    fun appSubtitle(lang: AppLanguage, udise: String?, block: String?, dist: String?): String {
        val parts = mutableListOf<String>()
        if (!udise.isNullOrBlank()) {
            parts.add("UDISE: $udise")
        }
        val locationParts = listOfNotNull(block?.ifBlank { null }, dist?.ifBlank { null })
        if (locationParts.isNotEmpty()) {
            parts.add(locationParts.joinToString(", "))
        }
        return if (parts.isNotEmpty()) parts.joinToString(" • ") else if (lang == AppLanguage.HINDI) "डिजिटल शाला पंजी" else "Digital School Register"
    }

    fun portalTag(lang: AppLanguage) =
        if (lang == AppLanguage.HINDI) "CG-MDM Helper • डिजिटल शाला पंजी"
        else "CG-MDM Assistant • Digital Register"

    // Navigation Bottom Bar
    fun navDashboard(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "डैशबोर्ड" else "Dashboard"
    fun navDailyMeal(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "दैनिक भोजन" else "Daily Meal"
    fun navMonthlyMeal(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "मासिक भोजन" else "Monthly Meal"
    fun navStock(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "स्टॉक" else "Stock"
    fun navReports(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "प्रतिवेदन" else "Reports"
    fun navMore(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "अधिक" else "More"

    // Dashboard Cards & Headers
    fun attentionRequired(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "आवश्यक ध्यान (Attention Required)" else "Attention Required"
    fun allUpToDate(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "सभी अभिलेख अद्यतन हैं (All Up to Date)" else "All Records Up to Date"
    fun allGoodSubtitle(lang: AppLanguage) =
        if (lang == AppLanguage.HINDI) "🟢 आज का भोजन विवरण, रसोइया उपस्थिति एवं स्टॉक संतुलन पूर्णतः व्यवस्थित है।"
        else "🟢 Today's meal record, cook attendance, and stock balance are properly updated."

    fun todayMealTitle(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "आज का मध्यान्ह भोजन" else "Today's Mid-Day Meal"
    fun recorded(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "✓ दर्ज किया गया" else "✓ Recorded"
    fun pending(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "लंबित (Pending)" else "Pending"
    fun studentsPresent(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "उपस्थित छात्र" else "Students Present"
    fun mealBenefited(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "भोजन लाभान्वित" else "Meal Served"
    fun cookAttendance(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "रसोइया उपस्थिति" else "Cook Attendance"
    fun presentCount(lang: AppLanguage, present: Int, total: Int) =
        if (lang == AppLanguage.HINDI) "$present / $total उपस्थित" else "$present / $total Present"
    fun studentsCount(lang: AppLanguage, count: Int) =
        if (lang == AppLanguage.HINDI) "$count छात्र" else "$count Students"
    fun daysCount(lang: AppLanguage, count: Int) =
        if (lang == AppLanguage.HINDI) "$count दिवस" else "$count Days"

    fun btnEnterMeal(lang: AppLanguage, isRecorded: Boolean) =
        if (lang == AppLanguage.HINDI) {
            if (isRecorded) "दैनिक रिपोर्ट देखें / संपादित करें" else "आज का भोजन प्रविष्टि दर्ज करें (30 सेकंड)"
        } else {
            if (isRecorded) "View / Edit Daily Report" else "Enter Today's Meal Record (30 sec)"
        }

    // Monthly Census Card
    fun monthlyCensusTitle(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "मासिक छात्र नामांकन (Census)" else "Monthly Student Census"
    fun viewDetails(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "विवरण >" else "Details >"
    fun totalStudents(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "कुल छात्र" else "Total Students"
    fun boys(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "बालक (Boys)" else "Boys"
    fun girls(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "बालिका (Girls)" else "Girls"
    fun cwsn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "दिव्यांग (CWSN)" else "CWSN (Divyang)"
    fun minority(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "अल्पसंख्यक (Minority)" else "Minority"

    // Stock Card
    fun riceStockTitle(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "चावल स्टॉक एवं PDS आपूर्ति" else "Rice Stock & PDS Supply"
    fun availableBalance(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "उपलब्ध स्टॉक (Balance)" else "Available Stock"
    fun receivedInMonth(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "माह में प्राप्त (PDS)" else "Received This Month"
    fun mealDaysInMonth(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "माह में भोजन दिवस" else "Meal Days This Month"

    // Quick management
    fun quickManagement(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "त्वरित प्रबंधन (Quick Management)" else "Quick Management"
    fun cookManagement(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "रसोइया प्रबंधन" else "Cook Management"
    fun activeCooksSubtitle(lang: AppLanguage, count: Int) =
        if (lang == AppLanguage.HINDI) "$count सक्रिय रसोइया" else "$count Active Cooks"
    fun riceReceiptPds(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "चावल प्राप्ति (PDS)" else "Rice Receipt (PDS)"
    fun challanStockPlus(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "चालान / स्टॉक +" else "Challan / Stock +"

    // Language Toggle
    fun switchLanguage(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "English में बदलें" else "Switch to हिन्दी"
    fun languageLabel(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "भाषा (Language)" else "Language (भाषा)"
}

fun formatDateToDdMmYyyy(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val parts = dateStr.trim().split("-")
        if (parts.size == 3) {
            if (parts[0].length == 4) {
                // yyyy-MM-dd -> dd-MM-yyyy
                "${parts[2].padStart(2, '0')}-${parts[1].padStart(2, '0')}-${parts[0]}"
            } else if (parts[2].length == 4) {
                // Already dd-MM-yyyy
                "${parts[0].padStart(2, '0')}-${parts[1].padStart(2, '0')}-${parts[2]}"
            } else {
                dateStr
            }
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

fun formatMonthToMmYyyy(monthStr: String?): String {
    if (monthStr.isNullOrBlank()) return ""
    return try {
        val parts = monthStr.trim().split("-")
        if (parts.size == 2) {
            if (parts[0].length == 4) {
                // yyyy-MM -> MM-yyyy
                "${parts[1].padStart(2, '0')}-${parts[0]}"
            } else {
                monthStr
            }
        } else {
            monthStr
        }
    } catch (_: Exception) {
        monthStr
    }
}

fun formatMonthFullName(monthYearStr: String?, isHi: Boolean): String {
    if (monthYearStr.isNullOrBlank()) return ""
    return try {
        val parts = monthYearStr.trim().split("-")
        val (year, monthNum) = if (parts.size == 2) {
            if (parts[0].length == 4) {
                Pair(parts[0], parts[1].toIntOrNull() ?: 1)
            } else {
                Pair(parts[1], parts[0].toIntOrNull() ?: 1)
            }
        } else {
            Pair("2026", 8)
        }

        val hindiMonths = listOf(
            "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून",
            "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
        )
        val englishMonths = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val idx = (monthNum - 1).coerceIn(0, 11)
        if (isHi) {
            "${hindiMonths[idx]} $year"
        } else {
            "${englishMonths[idx]} $year"
        }
    } catch (_: Exception) {
        monthYearStr
    }
}

fun formatDateDisplayWithDay(dateStr: String?, isHi: Boolean): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val date = sdf.parse(dateStr) ?: return formatDateToDdMmYyyy(dateStr)
        val cal = java.util.Calendar.getInstance().apply { time = date }
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val monthNum = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)

        val hindiDays = mapOf(
            java.util.Calendar.SUNDAY to "रविवार",
            java.util.Calendar.MONDAY to "सोमवार",
            java.util.Calendar.TUESDAY to "मंगलवार",
            java.util.Calendar.WEDNESDAY to "बुधवार",
            java.util.Calendar.THURSDAY to "गुरुवार",
            java.util.Calendar.FRIDAY to "शुक्रवार",
            java.util.Calendar.SATURDAY to "शनिवार"
        )
        val englishDays = mapOf(
            java.util.Calendar.SUNDAY to "Sunday",
            java.util.Calendar.MONDAY to "Monday",
            java.util.Calendar.TUESDAY to "Tuesday",
            java.util.Calendar.WEDNESDAY to "Wednesday",
            java.util.Calendar.THURSDAY to "Thursday",
            java.util.Calendar.FRIDAY to "Friday",
            java.util.Calendar.SATURDAY to "Saturday"
        )

        val monthName = formatMonthFullName("$year-${monthNum.toString().padStart(2, '0')}", isHi).split(" ")[0]
        val dayName = if (isHi) hindiDays[dayOfWeek] ?: "" else englishDays[dayOfWeek] ?: ""

        "$day $monthName $year ($dayName)"
    } catch (_: Exception) {
        formatDateToDdMmYyyy(dateStr)
    }
}

