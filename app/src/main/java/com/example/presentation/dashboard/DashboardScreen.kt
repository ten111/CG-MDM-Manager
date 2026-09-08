package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.*
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class HolidayDisplayItem(
    val dateStr: String,
    val formattedDate: String,
    val dayOfWeekName: String,
    val title: String,
    val holidayType: String,
    val orderNo: String = "",
    val remarks: String = "",
    val isSunday: Boolean = false
)

@Composable
fun DashboardScreen(
    viewModel: PoshanViewModel,
    onNavigateToMeal: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToEnrollment: () -> Unit,
    onNavigateToCooks: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    onNavigateToGoogleDriveBackup: () -> Unit = {},
    onNavigateToLoginPin: () -> Unit = {}
) {
    val school by viewModel.school.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val enrollment by viewModel.currentMonthEnrollment.collectAsState()
    val mealRecord by viewModel.dateMealRecord.collectAsState()
    val activeCooks by viewModel.activeCooks.collectAsState()
    val dateCookAttendances by viewModel.dateCookAttendances.collectAsState()
    val monthCookAttendances by viewModel.monthCookAttendances.collectAsState()
    val configNorms by viewModel.configNorms.collectAsState()
    val riceStockKg by viewModel.currentRiceStockKg.collectAsState()
    val attentionAlerts by viewModel.attentionAlerts.collectAsState()
    val monthRecords by viewModel.monthMealRecords.collectAsState()
    val allReceipts by viewModel.allReceipts.collectAsState()
    val allCalendarEvents by viewModel.allCalendarEvents.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    var showLogoutDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showHolidayPopup by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showBeneficiariesDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showCookingCostDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showRiceExpenseDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    val istZone = remember { TimeZone.getTimeZone("Asia/Kolkata") }

    val totalMonthRiceReceived = allReceipts
        .filter { it.receiptDate.startsWith(selectedMonth) }
        .sumOf { it.quantityKg }

    val totalMonthMealsServed = monthRecords.sumOf { it.studentsServed }
    val totalMealDays = monthRecords.count { it.mealServed }
    val totalBoys = monthRecords.sumOf { it.boysServed }
    val totalGirls = monthRecords.sumOf { it.girlsServed }
    val avgAttendance = if (totalMealDays > 0) (totalMonthMealsServed / totalMealDays) else 0
    val cookingCostRate = configNorms?.cookingCostRate ?: 10.17
    val totalCookingCost = totalMonthMealsServed * cookingCostRate

    // Total Rice Expense Calculation for the Month
    val schoolType = school?.schoolType ?: "PRIMARY"
    val isUpperOrMiddle = schoolType.contains("MIDDLE", ignoreCase = true) ||
            schoolType.contains("UPPER", ignoreCase = true) ||
            schoolType.contains("6-8", ignoreCase = true)
    val riceNormGrams = if (isUpperOrMiddle) {
        configNorms?.upperPrimaryRiceNormGrams ?: 150.0
    } else {
        configNorms?.primaryRiceNormGrams ?: 150.0
    }
    val totalRiceExpenseKg = if (monthRecords.any { it.riceConsumedKg > 0.0 }) {
        monthRecords.sumOf { it.riceConsumedKg }
    } else {
        (totalMonthMealsServed * riceNormGrams) / 1000.0
    }
    val riceExpenseDisplay = if (totalRiceExpenseKg == totalRiceExpenseKg.toLong().toDouble() && totalRiceExpenseKg > 0) {
        "${totalRiceExpenseKg.toLong()} ${if (isHi) "कि.ग्रा." else "Kg"}"
    } else {
        "${String.format(Locale.US, "%.3f", totalRiceExpenseKg)} ${if (isHi) "कि.ग्रा." else "Kg"}"
    }

    // Dynamic Monthly Holidays & Sundays Calculation
    val monthHolidaysList = remember(selectedMonth, allCalendarEvents, isHi) {
        val items = mutableListOf<HolidayDisplayItem>()
        try {
            val parts = selectedMonth.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val monthNum = parts.getOrNull(1)?.toIntOrNull() ?: 6

            val istZone = TimeZone.getTimeZone("Asia/Kolkata")
            val cal = Calendar.getInstance(istZone).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthNum - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone }
            val sdfDisplay = SimpleDateFormat(if (isHi) "dd MMM yyyy" else "dd MMM yyyy", Locale.getDefault()).apply { timeZone = istZone }
            val sdfDayName = SimpleDateFormat("EEEE", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).apply { timeZone = istZone }

            val eventsInMonth = allCalendarEvents.filter { it.eventDate.startsWith(selectedMonth) }
            val eventMap = eventsInMonth.groupBy { it.eventDate }

            for (day in 1..maxDays) {
                cal.set(Calendar.DAY_OF_MONTH, day)
                val dateString = sdfDate.format(cal.time)
                val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                val formattedDate = sdfDisplay.format(cal.time)
                val dayName = sdfDayName.format(cal.time)

                val dayEvents = eventMap[dateString] ?: emptyList()
                val holidayEvents = dayEvents.filter { it.eventType != "WORKING_DAY" }

                if (isSunday) {
                    if (holidayEvents.isNotEmpty()) {
                        val ev = holidayEvents.first()
                        items.add(
                            HolidayDisplayItem(
                                dateStr = dateString,
                                formattedDate = formattedDate,
                                dayOfWeekName = dayName,
                                title = "${ev.eventName} (${if (isHi) "रविवार" else "Sunday"})",
                                holidayType = ev.eventType,
                                orderNo = ev.orderNumber,
                                remarks = ev.remarks,
                                isSunday = true
                            )
                        )
                    } else {
                        items.add(
                            HolidayDisplayItem(
                                dateStr = dateString,
                                formattedDate = formattedDate,
                                dayOfWeekName = dayName,
                                title = if (isHi) "रविवार (साप्ताहिक अवकाश)" else "Sunday (Weekly Off)",
                                holidayType = "SUNDAY",
                                orderNo = "",
                                remarks = if (isHi) "साप्ताहिक अवकाश" else "Weekly Holiday",
                                isSunday = true
                            )
                        )
                    }
                } else if (holidayEvents.isNotEmpty()) {
                    holidayEvents.forEach { ev ->
                        items.add(
                            HolidayDisplayItem(
                                dateStr = dateString,
                                formattedDate = formattedDate,
                                dayOfWeekName = dayName,
                                title = ev.eventName,
                                holidayType = ev.eventType,
                                orderNo = ev.orderNumber,
                                remarks = ev.remarks,
                                isSunday = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // non-fatal fallback
        }
        items.sortedBy { it.dateStr }
    }

    val totalSundaysCount = monthHolidaysList.count { it.isSunday }
    val totalOtherHolidaysCount = monthHolidaysList.count { !it.isSunday }
    val totalHolidaysInMonth = monthHolidaysList.size

    // Formatted date and month for titles
    val formattedDateToday = try {
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d = sdfIn.parse(selectedDate) ?: Date()
        val sdfOut = SimpleDateFormat(if (isHi) "dd MMM yyyy" else "dd MMM yyyy", Locale.getDefault())
        sdfOut.format(d)
    } catch (e: Exception) { selectedDate }

    val monthOverviewTitle = try {
        val sdfIn = SimpleDateFormat("yyyy-MM", Locale.US)
        val d = sdfIn.parse(selectedMonth) ?: Date()
        val sdfOut = SimpleDateFormat(if (isHi) "MMM yyyy" else "MMM yyyy", Locale.getDefault())
        sdfOut.format(d)
    } catch (e: Exception) { selectedMonth }

    val totalActiveCooks = activeCooks.size.coerceAtLeast(1)
    val cookPresentCount = monthCookAttendances.count { it.isPresent }
    val cookTotalExpected = if (monthCookAttendances.isNotEmpty()) monthCookAttendances.size else (totalMealDays * totalActiveCooks)
    val cookPresentDisplay = if (cookPresentCount > 0) {
        "$cookPresentCount/$cookTotalExpected"
    } else if (totalMealDays > 0) {
        "${totalMealDays * totalActiveCooks}/${totalMealDays * totalActiveCooks}"
    } else {
        "0/0"
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isHi) "लॉगआउट / स्क्रीन लॉक करें?" else "Lock App / Logout?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = if (isHi)
                        "क्या आप ऐप को लॉक करके लॉगआउट करना चाहते हैं? पुनः उपयोग के लिए आपको अपना 4-अंकीय पिन दर्ज करना होगा।"
                    else
                        "Do you want to lock the app and logout? You will need to enter your 4-digit PIN to access again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onNavigateToLoginPin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHi) "हाँ, लॉगआउट करें" else "Yes, Logout",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    if (showHolidayPopup) {
        AlertDialog(
            onDismissRequest = { showHolidayPopup = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHi) "$monthOverviewTitle • अवकाश सूची" else "$monthOverviewTitle • Holiday List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isHi)
                                "कुल $totalHolidaysInMonth दिन ($totalSundaysCount रविवार • $totalOtherHolidaysCount अन्य अवकाश)"
                            else
                                "Total $totalHolidaysInMonth Days ($totalSundaysCount Sundays • $totalOtherHolidaysCount Other)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            },
            text = {
                if (monthHolidaysList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHi) "इस माह में कोई अवकाश दर्ज नहीं है।" else "No holidays recorded for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(monthHolidaysList) { holiday ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (holiday.isSunday) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (holiday.isSunday) Color(0xFFFECACA) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Date Box
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (holiday.isSunday) Color(0xFFDC2626) else Color(0xFF0284C7),
                                        modifier = Modifier.width(72.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 3.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val dayNum = holiday.dateStr.takeLast(2)
                                            Text(
                                                text = dayNum,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = holiday.dayOfWeekName,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                color = Color.White.copy(alpha = 0.95f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Holiday details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = holiday.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            val typeLabel = when (holiday.holidayType) {
                                                "SUNDAY" -> if (isHi) "साप्ताहिक अवकाश" else "Weekly Off"
                                                "NATIONAL" -> if (isHi) "राष्ट्रीय अवकाश" else "National Holiday"
                                                "STATE" -> if (isHi) "राज्य अवकाश" else "State Holiday"
                                                "VACATION" -> if (isHi) "दीर्घ अवकाश" else "Vacation"
                                                "LOCAL" -> if (isHi) "स्थानीय अवकाश" else "Local Holiday"
                                                "EMERGENCY" -> if (isHi) "आपातकालीन बंद" else "Emergency"
                                                else -> if (isHi) "अवकाश" else "Holiday"
                                            }
                                            Surface(
                                                color = if (holiday.isSunday) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = typeLabel,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (holiday.isSunday) Color(0xFF991B1B) else Color(0xFF0369A1),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }

                                            if (holiday.orderNo.isNotBlank()) {
                                                Text(
                                                    text = "आदेश: ${holiday.orderNo}",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHolidayPopup = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isHi) "ठीक है (बंद करें)" else "OK (Close)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showHolidayPopup = false
                        onNavigateToCalendar()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isHi) "कैलेंडर प्रबंधन" else "Manage Calendar")
                }
            }
        )
    }

    // -----------------------------------------------------------------
    // DATE-WISE BENEFICIARIES LIST DIALOG
    // -----------------------------------------------------------------
    if (showBeneficiariesDialog) {
        val sortedRecords = monthRecords.sortedBy { it.date }
        AlertDialog(
            onDismissRequest = { showBeneficiariesDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHi) "$monthOverviewTitle • लाभांवित छात्र विवरण" else "$monthOverviewTitle • Beneficiaries List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isHi)
                                "कुल लाभांवित: $totalMonthMealsServed (👦 $totalBoys बालक • 👧 $totalGirls बालिका)"
                            else
                                "Total: $totalMonthMealsServed (👦 $totalBoys Boys • 👧 $totalGirls Girls)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            },
            text = {
                if (sortedRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHi) "इस माह में कोई भोजन प्रविष्टि दर्ज नहीं है।" else "No meal records for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedRecords) { rec ->
                            val parsedDate = try {
                                val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone }
                                val outSdf = SimpleDateFormat("dd MMM, EEEE", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).apply { timeZone = istZone }
                                val d = inSdf.parse(rec.date)
                                if (d != null) outSdf.format(d) else rec.date
                            } catch (e: Exception) {
                                rec.date
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (rec.mealServed) Color(0xFFF8FAFC) else Color(0xFFFFF1F2),
                                border = BorderStroke(
                                    1.dp,
                                    if (rec.mealServed) Color(0xFFE2E8F0) else Color(0xFFFECDD3)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = parsedDate,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (rec.mealServed) {
                                            Text(
                                                text = "👦 ${if (isHi) "बालक" else "Boys"}: ${rec.boysServed}  •  👧 ${if (isHi) "बालिका" else "Girls"}: ${rec.girlsServed}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569),
                                                fontWeight = FontWeight.Medium
                                            )
                                        } else {
                                            Text(
                                                text = if (isHi) "भोजन नहीं परोसा गया" else "Meal Not Served",
                                                fontSize = 11.sp,
                                                color = Color(0xFFE11D48),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (rec.mealServed) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFFF7ED),
                                            border = BorderStroke(1.dp, Color(0xFFFFEDD5))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "${rec.studentsServed}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFFEA580C)
                                                )
                                                Text(
                                                    text = if (isHi) "कुल छात्र" else "Total",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF9A3412)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBeneficiariesDialog = false }) {
                    Text(if (isHi) "बंद करें" else "Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // -----------------------------------------------------------------
    // DATE-WISE COOKING COST LIST DIALOG
    // -----------------------------------------------------------------
    if (showCookingCostDialog) {
        val sortedRecords = monthRecords.sortedBy { it.date }
        AlertDialog(
            onDismissRequest = { showCookingCostDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHi) "$monthOverviewTitle • दैनिक कुकिंग लागत व्यय" else "$monthOverviewTitle • Cooking Cost Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isHi)
                                "कुल व्यय: ₹${String.format(Locale.US, "%.2f", totalCookingCost)} (दर: ₹${cookingCostRate}/छात्र)"
                            else
                                "Total: ₹${String.format(Locale.US, "%.2f", totalCookingCost)} (Rate: ₹${cookingCostRate}/student)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            },
            text = {
                if (sortedRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHi) "इस माह में कोई भोजन प्रविष्टि दर्ज नहीं है।" else "No meal records for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedRecords) { rec ->
                            val parsedDate = try {
                                val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone }
                                val outSdf = SimpleDateFormat("dd MMM, EEEE", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).apply { timeZone = istZone }
                                val d = inSdf.parse(rec.date)
                                if (d != null) outSdf.format(d) else rec.date
                            } catch (e: Exception) {
                                rec.date
                            }
                            val dayCost = if (rec.mealServed) rec.studentsServed * cookingCostRate else 0.0
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (rec.mealServed) Color(0xFFF8FAFC) else Color(0xFFFFF1F2),
                                border = BorderStroke(
                                    1.dp,
                                    if (rec.mealServed) Color(0xFFE2E8F0) else Color(0xFFFECDD3)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = parsedDate,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (rec.mealServed) {
                                            Text(
                                                text = "${rec.studentsServed} ${if (isHi) "छात्र" else "students"} × ₹$cookingCostRate",
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569)
                                            )
                                        } else {
                                            Text(
                                                text = if (isHi) "भोजन नहीं परोसा गया (व्यय: ₹0.00)" else "No meal served (Expense: ₹0.00)",
                                                fontSize = 11.sp,
                                                color = Color(0xFFE11D48)
                                            )
                                        }
                                    }
                                    if (rec.mealServed) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                        ) {
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", dayCost)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCookingCostDialog = false }) {
                    Text(if (isHi) "बंद करें" else "Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // -----------------------------------------------------------------
    // DATE-WISE RICE EXPENSE LIST DIALOG
    // -----------------------------------------------------------------
    if (showRiceExpenseDialog) {
        val sortedRecords = monthRecords.sortedBy { it.date }
        AlertDialog(
            onDismissRequest = { showRiceExpenseDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHi) "$monthOverviewTitle • दैनिक चावल व्यय" else "$monthOverviewTitle • Daily Rice Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isHi)
                                "कुल व्यय: $riceExpenseDisplay (मानक दर: ${riceNormGrams.toInt()}g/छात्र)"
                            else
                                "Total: $riceExpenseDisplay (Norm: ${riceNormGrams.toInt()}g/student)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            },
            text = {
                if (sortedRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHi) "इस माह में कोई भोजन प्रविष्टि दर्ज नहीं है।" else "No meal records for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedRecords) { rec ->
                            val parsedDate = try {
                                val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone }
                                val outSdf = SimpleDateFormat("dd MMM, EEEE", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).apply { timeZone = istZone }
                                val d = inSdf.parse(rec.date)
                                if (d != null) outSdf.format(d) else rec.date
                            } catch (e: Exception) {
                                rec.date
                            }
                            val dayRiceKg = if (rec.riceConsumedKg > 0.0) {
                                rec.riceConsumedKg
                            } else if (rec.mealServed) {
                                (rec.studentsServed * riceNormGrams) / 1000.0
                            } else 0.0

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (rec.mealServed) Color(0xFFF8FAFC) else Color(0xFFFFF1F2),
                                border = BorderStroke(
                                    1.dp,
                                    if (rec.mealServed) Color(0xFFE2E8F0) else Color(0xFFFECDD3)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = parsedDate,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (rec.mealServed) {
                                            Text(
                                                text = "${rec.studentsServed} ${if (isHi) "छात्र" else "students"} × ${riceNormGrams.toInt()}g",
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569)
                                            )
                                        } else {
                                            Text(
                                                text = if (isHi) "भोजन नहीं परोसा गया (व्यय: 0 कि.ग्रा.)" else "No meal served (0 Kg)",
                                                fontSize = 11.sp,
                                                color = Color(0xFFE11D48)
                                            )
                                        }
                                    }
                                    if (rec.mealServed) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF0FDF4),
                                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                        ) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.3f", dayRiceKg)} ${if (isHi) "कि.ग्रा." else "Kg"}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF166534),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRiceExpenseDialog = false }) {
                    Text(if (isHi) "बंद करें" else "Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        PoshanTopAppBar(
            title = AppStrings.appTitle(currentLanguage, school?.schoolName),
            subtitle = AppStrings.appSubtitle(
                currentLanguage,
                school?.udiseCode,
                school?.blockName,
                school?.districtName
            ),
            currentLanguage = currentLanguage,
            onLanguageToggle = { viewModel.toggleLanguage() },
            onCalendarClick = onNavigateToCalendar,
            onSyncClick = { viewModel.syncDataNow() },
            onLogoutClick = { showLogoutDialog = true }
        )

        // Month Selector with Language Support
        MonthSelectorRow(
            selectedMonth = selectedMonth,
            onMonthSelected = { viewModel.setSelectedMonth(it) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .swipeToNavigateMonth(
                    selectedMonth = selectedMonth,
                    onMonthSelected = { viewModel.setSelectedMonth(it) }
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ACTIVE STAFF USER BADGE
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BluePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (currentUserRole) {
                                    com.example.data.local.entity.UserRole.HEADMASTER -> Icons.Default.AdminPanelSettings
                                    com.example.data.local.entity.UserRole.MDM_INCHARGE -> Icons.Default.AssignmentInd
                                    com.example.data.local.entity.UserRole.SHG_REPRESENTATIVE -> Icons.Default.SoupKitchen
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentUser?.name ?: school?.headTeacherName ?: if (isHi) "प्रधान पाठक" else "Head Teacher",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isHi) currentUserRole.titleHi else currentUserRole.titleEn,
                                fontSize = 11.sp,
                                color = when (currentUserRole) {
                                    com.example.data.local.entity.UserRole.HEADMASTER -> BluePrimary
                                    com.example.data.local.entity.UserRole.MDM_INCHARGE -> Color(0xFF16A34A)
                                    com.example.data.local.entity.UserRole.SHG_REPRESENTATIVE -> Color(0xFFD97706)
                                    else -> Color(0xFF7C3AED)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHi) "लॉगिन" else "Active",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 1. Attention Required Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (attentionAlerts.any { it.severity == PoshanViewModel.AlertSeverity.CRITICAL })
                        Color(0xFFFEF2F2)
                    else if (attentionAlerts.isNotEmpty())
                        Color(0xFFFFFBEB)
                    else
                        Color(0xFFEFF6FF)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (attentionAlerts.any { it.severity == PoshanViewModel.AlertSeverity.CRITICAL })
                        Color(0xFFFECACA)
                    else if (attentionAlerts.isNotEmpty())
                        Color(0xFFFDE68A)
                    else
                        Color(0xFFBFDBFE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (attentionAlerts.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (attentionAlerts.isEmpty()) BluePrimary else StatusWarningOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (attentionAlerts.isEmpty()) AppStrings.allUpToDate(currentLanguage) else AppStrings.attentionRequired(currentLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (attentionAlerts.isEmpty()) BluePrimary else Color(0xFFC2410C)
                        )
                    }

                    if (attentionAlerts.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AppStrings.allGoodSubtitle(currentLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        attentionAlerts.forEach { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = if (alert.severity == PoshanViewModel.AlertSeverity.CRITICAL) "🔴" else "🟠",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Column {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = alert.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Today's Meal Quick Action Card (Students Attendance Entry Link Card)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BluePrimary),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.todayMealTitle(currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (mealRecord != null) AppStrings.recorded(currentLanguage) else AppStrings.pending(currentLanguage),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = AppStrings.studentsPresent(currentLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "${mealRecord?.studentsPresent ?: "-"} / ${enrollment?.totalEnrollment ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = AppStrings.mealBenefited(currentLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = if (mealRecord?.mealServed == true) AppStrings.studentsCount(currentLanguage, mealRecord?.studentsServed ?: 0) else (if (isHi) "नहीं" else "No"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047)
                            )
                        }

                        Column {
                            Text(
                                text = AppStrings.cookAttendance(currentLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            val presentCount = dateCookAttendances.count { it.isPresent }
                            Text(
                                text = AppStrings.presentCount(currentLanguage, presentCount, activeCooks.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onNavigateToMeal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.btnEnterMeal(currentLanguage, mealRecord != null),
                            color = BluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // 3. TODAY'S SUMMARY CARDS (MATCHING SCREENSHOTS)
            // -----------------------------------------------------------------
            Text(
                text = if (isHi) "आज ($formattedDateToday)" else "Today ($formattedDateToday)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(top = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today Present Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${mealRecord?.studentsPresent ?: 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isHi) "उपस्थित" else "Present",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Today Meal Served Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (mealRecord?.mealServed == true) (if (isHi) "हाँ" else "Yes") else (if (isHi) "नहीं" else "No"),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isHi) "भोजन परोसा गया" else "Meal Served",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // 4. MONTH OVERVIEW CARDS (MATCHING SCREENSHOTS)
            // -----------------------------------------------------------------
            Text(
                text = if (isHi) "$monthOverviewTitle विवरण" else "$monthOverviewTitle Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(top = 4.dp)
            )

            // Row 1: Meal Days & Total Beneficiaries
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    icon = Icons.Default.CalendarMonth,
                    iconTint = Color(0xFF9333EA),
                    value = "$totalMealDays",
                    label = if (isHi) "भोजन दिवस" else "Meal Days",
                    modifier = Modifier.weight(1f)
                )

                SummaryStatCard(
                    icon = Icons.Default.Face,
                    iconTint = Color(0xFFF97316),
                    value = "$totalMonthMealsServed",
                    label = if (isHi) "कुल लाभांवित" else "Total Beneficiaries",
                    subActionText = if (isHi) "दैनिक सूची देखें ›" else "Date-wise List ›",
                    onClick = { showBeneficiariesDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Boys (Total) & Girls (Total) with Colored Top Highlight Accents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    icon = Icons.Default.Male,
                    iconTint = Color(0xFF0284C7),
                    value = "$totalBoys",
                    label = if (isHi) "बालक (कुल)" else "Boys (Total)",
                    topAccentColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )

                SummaryStatCard(
                    icon = Icons.Default.Female,
                    iconTint = Color(0xFFE11D48),
                    value = "$totalGirls",
                    label = if (isHi) "बालिका (कुल)" else "Girls (Total)",
                    topAccentColor = Color(0xFFE11D48),
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Avg Attendance/Day & Cooking Cost
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    icon = Icons.Default.BarChart,
                    iconTint = Color(0xFF06B6D4),
                    value = "$avgAttendance",
                    label = if (isHi) "औसत उपस्थिति/दिन" else "Avg Attendance/Day",
                    modifier = Modifier.weight(1f)
                )

                SummaryStatCard(
                    icon = Icons.Default.Payments,
                    iconTint = Color(0xFF10B981),
                    value = "₹${String.format(Locale.US, "%.2f", totalCookingCost)}",
                    label = if (isHi) "कुकिंग लागत" else "Cooking Cost",
                    subActionText = if (isHi) "दैनिक व्यय देखें ›" else "Date-wise List ›",
                    onClick = { showCookingCostDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 4: Total Rice Expense & Total Holidays (Before Cook Attendance)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Total Rice Expense on that month
                SummaryStatCard(
                    icon = Icons.Default.Scale,
                    iconTint = Color(0xFF059669),
                    value = riceExpenseDisplay,
                    label = if (isHi) "कुल चावल व्यय" else "Total Rice Expense",
                    topAccentColor = Color(0xFF059669),
                    subActionText = if (isHi) "दैनिक खपत देखें ›" else "Date-wise List ›",
                    onClick = { showRiceExpenseDialog = true },
                    modifier = Modifier.weight(1f)
                )

                // 2. Total Holidays for the month (Sundays & Other Holidays in 2 lines, clickable for complete popup list)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .defaultMinSize(minHeight = 126.dp)
                        .clickable { showHolidayPopup = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color(0xFFE11D48))
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHi) "कुल अवकाश: $totalHolidaysInMonth" else "Holidays: $totalHolidaysInMonth",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF0F172A),
                                    maxLines = 1
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Line 1: Total Sundays
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isHi) "• कुल रविवार:" else "• Sundays:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = "$totalSundaysCount",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                }

                                // Line 2: Total Other Holidays
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isHi) "• अन्य अवकाश:" else "• Other Holidays:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "$totalOtherHolidaysCount",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color(0xFFE11D48).copy(alpha = 0.10f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isHi) "सूची देखें ›" else "View List ›",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE11D48),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Row 5: Cook Present Days (Full width card)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SoupKitchen,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cookPresentDisplay,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isHi) "रसोइया उपस्थिति दिवस" else "Cook Present Days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    topAccentColor: Color? = null,
    subActionText: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minHeight = 126.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            if (topAccentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(topAccentColor)
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val valueFontSize = when {
                        value.length > 11 -> 15.sp
                        value.length > 6 -> 17.sp
                        else -> 20.sp
                    }
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = valueFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onClick != null) {
                    Surface(
                        color = iconTint.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = subActionText ?: (if (label.contains("कुकिंग") || label.contains("Cost")) "दैनिक व्यय देखें ›" else "दैनिक सूची देखें ›"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionRowItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CategoryChip(text: String) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
