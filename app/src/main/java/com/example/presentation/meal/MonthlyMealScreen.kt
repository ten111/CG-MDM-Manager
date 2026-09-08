package com.example.presentation.meal

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.FlowRow
import coil.compose.AsyncImage
import com.example.data.local.entity.CookingAgencyEntity
import com.example.data.local.entity.DailyMealRecordEntity
import com.example.data.local.entity.ConfigNormsEntity
import com.example.data.local.entity.SchoolEntity
import com.example.data.model.CustomFoodItemParser
import com.example.presentation.common.*
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.reports.PdfReportGenerator
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MonthlyMealScreen(
    viewModel: PoshanViewModel,
    onNavigateToDailyMeal: (String?) -> Unit
) {
    val context = LocalContext.current
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val monthMealRecords by viewModel.monthMealRecords.collectAsState()
    val school by viewModel.school.collectAsState()
    val configNorms by viewModel.configNorms.collectAsState()
    val agencies by viewModel.allAgencies.collectAsState()
    val activeAgency = agencies.firstOrNull()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    var recordToDelete by remember { mutableStateOf<DailyMealRecordEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteSuccessDate by remember { mutableStateOf<String?>(null) }
    var showFullPhotoPreview by remember { mutableStateOf(false) }
    var previewingPhotoUri by remember { mutableStateOf("") }

    // State for SHG Daily Attendance WhatsApp Dispatch
    var recordToShareOnWhatsApp by remember { mutableStateOf<DailyMealRecordEntity?>(null) }
    var showDailyShareDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val istZone = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone } }

    val sortedRecords = remember(monthMealRecords) {
        monthMealRecords.sortedByDescending { it.date }
    }

    val totalDaysLogged = monthMealRecords.size
    val totalMealDaysServed = monthMealRecords.count { it.mealServed }
    val totalMealsServedStudents = monthMealRecords.sumOf { it.studentsServed }
    val totalBoysServed = monthMealRecords.sumOf { it.boysServed }
    val totalGirlsServed = monthMealRecords.sumOf { it.girlsServed }
    val totalStudentsPresent = monthMealRecords.sumOf { it.studentsPresent }

    val schoolType = school?.schoolType ?: "PRIMARY"
    val ricePerStudentGrams = if (schoolType == "MIDDLE" || schoolType == "UPPER_PRIMARY") {
        configNorms?.upperPrimaryRiceNormGrams ?: 150.0
    } else {
        configNorms?.primaryRiceNormGrams ?: 150.0
    }
    val totalRiceUsedKg = (totalMealsServedStudents * ricePerStudentGrams) / 1000.0
    val cookingCostRate = configNorms?.cookingCostRate ?: 10.17
    val totalCookingCost = totalMealsServedStudents * cookingCostRate

    val monthName = remember(selectedMonth, isHi) {
        try {
            val cal = Calendar.getInstance(istZone).apply {
                val parts = selectedMonth.split("-")
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            SimpleDateFormat("MMMM yyyy", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).format(cal.time)
        } catch (e: Exception) {
            selectedMonth
        }
    }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "मासिक भोजन पंजी" else "Monthly Meal Register",
                subtitle = school?.let { s ->
                    if (s.udiseCode.isNotBlank()) {
                        "UDISE: ${s.udiseCode} • ${s.schoolName}"
                    } else {
                        s.schoolName
                    }
                } ?: "UDISE: 22080108901 • शासकीय प्राथमिक शाला",
                titleFontSize = 14.5.sp,
                subtitleFontSize = 11.sp,
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToDailyMeal(null) },
                containerColor = BluePrimary,
                contentColor = Color.White,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = if (isHi) "दैनिक भोजन दर्ज करें" else "Enter Daily Meal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .swipeToNavigateMonth(
                    selectedMonth = selectedMonth,
                    onMonthSelected = { viewModel.setSelectedMonth(it) }
                ),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. MONTH SELECTOR HORIZONTAL CHIPS
            item {
                Column {
                    Text(
                        text = if (isHi) "माह चुनें (Select Month)" else "Select Month",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    MonthSelectorRow(
                        selectedMonth = selectedMonth,
                        onMonthSelected = { viewModel.setSelectedMonth(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. MONTHLY AGGREGATE SUMMARY CARD
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = BluePrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$monthName ${if (isHi) "का कुल सारांश" else "Summary"}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${school?.schoolType ?: "Primary"} • ${if (isHi) "चावल दर" else "Rice Norm"}: ${ricePerStudentGrams.toInt()}g/छात्र",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                color = if (totalMealDaysServed > 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$totalMealDaysServed ${if (isHi) "भोजन दिवस" else "Meal Days"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalMealDaysServed > 0) Color(0xFF166534) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric Stat Badges Grid - All 4 Cards in 1 Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Days Logged
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$totalDaysLogged",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isHi) "दिन प्रविष्टि" else "Days Logged",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 2. Total Benefited
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$totalMealsServedStudents",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                    Text(
                                        text = if (isHi) "कुल लाभान्वित" else "Total Meals",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = BluePrimary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 3. Rice Consumption
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", totalRiceUsedKg),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = if (isHi) "चावल (Kg)" else "Rice (Kg)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = Color(0xFF166534),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 4. Cooking Cost Card
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.0f", totalCookingCost)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                    Text(
                                        text = if (isHi) "कुकिंग लागत" else "Cooking Cost",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = Color(0xFFB45309),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (totalMealDaysServed > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // First line: Boys and Girls count with perfect space
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👦 ${if (isHi) "बालक लाभान्वित" else "Boys Served"}: $totalBoysServed   •   👧 ${if (isHi) "बालिका लाभान्वित" else "Girls Served"}: $totalGirlsServed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Next line: Average attendance per day
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val avgAtt = if (totalMealDaysServed > 0) totalMealsServedStudents / totalMealDaysServed else 0
                                    Text(
                                        text = "📊 ${if (isHi) "औसत उपस्थिति" else "Average Attendance"}: $avgAtt / दिन (${if (isHi) "प्रतिदिन औसत" else "Daily Average"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. SECTION HEADER FOR DATE-WISE RECORDS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHi) "तिथि-वार उपस्थिति एवं भोजन अभिलेख" else "Date-wise Attendance & Meal Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${sortedRecords.size} ${if (isHi) "प्रविष्टियां" else "entries"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 4. DATE-WISE RECORDS LIST OR EMPTY STATE
            if (sortedRecords.isEmpty()) {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (isHi) "इस माह ($monthName) में कोई उपस्थिति दर्ज नहीं है" else "No attendance records saved for $monthName",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHi)
                                    "दैनिक भोजन एवं छात्र उपस्थिति दर्ज करने के लिए 'दैनिक भोजन' टैब का उपयोग करें।"
                                else
                                    "Use the 'Daily Meal' tab to record daily meals and student attendance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(sortedRecords, key = { it.date }) { record ->
                    val isCurrentSelection = record.date == selectedDate
                    val dayOfWeek = try {
                        val cal = Calendar.getInstance(istZone).apply {
                            time = sdf.parse(record.date) ?: Date()
                        }
                        SimpleDateFormat("EEEE", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).format(cal.time)
                    } catch (e: Exception) { "" }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentSelection) Color(0xFFF8FAFC) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(if (isCurrentSelection) 2.dp else 1.dp),
                        border = BorderStroke(
                            if (isCurrentSelection) 1.5.dp else 1.dp,
                            if (isCurrentSelection) BluePrimary else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSelectedDate(record.date)
                                onNavigateToDailyMeal(record.date)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Date & Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = if (isCurrentSelection) BluePrimary else Color(0xFF475569),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${record.date.toDisplayDate()} ($dayOfWeek)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentSelection) BluePrimary else Color(0xFF0F172A)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (record.photoUri.isNotEmpty()) {
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, Color(0xFF93C5FD)),
                                            modifier = Modifier.clickable {
                                                previewingPhotoUri = record.photoUri
                                                showFullPhotoPreview = true
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = null,
                                                    tint = BluePrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = if (isHi) "फोटो" else "Photo",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BluePrimary
                                                )
                                            }
                                        }
                                    }

                                    if (record.mealServed) {
                                        Surface(
                                            color = Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isHi) "✓ भोजन वितरित" else "✓ Meal Served",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF166534),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isHi) "✗ भोजन नहीं" else "✗ Not Served",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF991B1B),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Attendance Summary Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "👦 ${if (isHi) "बालक" else "Boys"}: ${record.boysPresent} • 👧 ${if (isHi) "बालिका" else "Girls"}: ${record.girlsPresent}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "${if (isHi) "कुल उपस्थित" else "Total"}: ${record.studentsPresent}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            if (record.mealServed) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🍽️ ${if (isHi) "लाभान्वित छात्र" else "Meals Served"}: ${record.studentsServed} (👦 ${record.boysServed} • 👧 ${record.girlsServed})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = "👩‍🍳 ${if (isHi) "रसोइया" else "Cooks"}: ${record.cooksPresentCount}/${record.totalCooksCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }

                            if (record.menuDetails.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🍲 ${record.menuDetails}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Actions Row: Photo View, Delete & SHG WhatsApp, View/Edit Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (record.photoUri.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                previewingPhotoUri = record.photoUri
                                                showFullPhotoPreview = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = BluePrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isHi) "फोटो" else "Photo",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = BluePrimary
                                            )
                                        }
                                    }

                                    // Delete Button
                                    TextButton(
                                        onClick = {
                                            recordToDelete = record
                                            showDeleteDialog = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Entry",
                                            modifier = Modifier.size(15.dp),
                                            tint = Color(0xFFDC2626)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isHi) "हटाएं" else "Delete",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // SHG President WhatsApp Share Button
                                    FilledTonalButton(
                                        onClick = {
                                            recordToShareOnWhatsApp = record
                                            showDailyShareDialog = true
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF25D366).copy(alpha = 0.15f),
                                            contentColor = Color(0xFF15803D)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share to SHG WhatsApp",
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFF15803D)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHi) "SHG व्हाट्सएप" else "SHG WhatsApp",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }

                                    // View / Edit Button -> Navigates to Daily Meal Tab with selected date
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.setSelectedDate(record.date)
                                            onNavigateToDailyMeal(record.date)
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = BluePrimary.copy(alpha = 0.12f),
                                            contentColor = BluePrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = BluePrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHi) "देखें / संपादित करें" else "View / Edit",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CONFIRMATION DIALOG FOR DELETING MEAL ENTRY
    if (showDeleteDialog && recordToDelete != null) {
        val target = recordToDelete!!
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                recordToDelete = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isHi) "प्रविष्टि हटाएं (Delete Entry)?" else "Delete Entry?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHi)
                            "क्या आप दिनांक ${target.date.toDisplayDate()} की दैनिक मध्यान्ह भोजन एवं छात्र उपस्थिति प्रविष्टि को हटाना चाहते हैं?"
                        else
                            "Are you sure you want to delete the daily mid-day meal and attendance entry for ${target.date.toDisplayDate()}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155)
                    )
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHi)
                                    "नोट: इस प्रविष्टि को हटाने पर उस दिन की रसोइया उपस्थिति एवं स्टॉक लेजर में दर्ज चावल खपत भी स्वतः निरस्त कर दी जाएगी।"
                                else
                                    "Note: Deleting this entry will also remove the cook attendance and linked rice consumption from the stock ledger.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deletedDate = target.date
                        viewModel.deleteDailyMeal(deletedDate) {
                            deleteSuccessDate = deletedDate
                        }
                        showDeleteDialog = false
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(
                        text = if (isHi) "हाँ, हटाएं (Delete)" else "Yes, Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                        recordToDelete = null
                    }
                ) {
                    Text(text = if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // FULL PHOTO PREVIEW DIALOG
    if (showFullPhotoPreview && previewingPhotoUri.isNotEmpty()) {
        Dialog(onDismissRequest = { showFullPhotoPreview = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHi) "मध्यान्ह भोजन फोटो" else "Mid-Day Meal Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showFullPhotoPreview = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AsyncImage(
                        model = previewingPhotoUri,
                        contentDescription = "Meal Photo Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showFullPhotoPreview = false },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (isHi) "बंद करें (Close)" else "Close")
                    }
                }
            }
        }
    }

    // DAILY ATTENDANCE WHATSAPP DISPATCH DIALOG (TO SHG PRESIDENT)
    if (showDailyShareDialog && recordToShareOnWhatsApp != null) {
        val record = recordToShareOnWhatsApp!!
        val shgName = activeAgency?.name?.ifBlank { "स्व-सहायता समूह" } ?: "स्व-सहायता समूह"
        val shgPresident = activeAgency?.contactPerson?.ifBlank { "अध्यक्ष / सचिव" } ?: "अध्यक्ष / सचिव"
        val currentShgMobile = activeAgency?.mobile?.filter { it.isDigit() }.orEmpty()
        val isShgConfigured = currentShgMobile.length >= 10

        var editShgMobile by remember(currentShgMobile) { mutableStateOf(currentShgMobile) }
        var showManualEditSection by remember(isShgConfigured) { mutableStateOf(!isShgConfigured) }

        val dateDisplay = try {
            val parsed = sdf.parse(record.date)
            if (parsed != null) SimpleDateFormat("dd-MM-yyyy (EEEE)", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).format(parsed) else record.date
        } catch (_: Exception) {
            record.date
        }

        val pulseNormG = configNorms?.pulseNormGrams ?: 30.0
        val vegNormG = configNorms?.vegetableNormGrams ?: 75.0
        val oilNormG = configNorms?.oilNormGrams ?: 7.5
        val saltNormG = configNorms?.saltNormGrams ?: 5.0

        val dailyRiceKg = if (record.riceConsumedKg > 0) record.riceConsumedKg else (record.studentsServed * ricePerStudentGrams / 1000.0)
        val dailyPulseKg = (record.studentsServed * pulseNormG) / 1000.0
        val dailyVegKg = (record.studentsServed * vegNormG) / 1000.0
        val dailyOilKg = (record.studentsServed * oilNormG) / 1000.0
        val dailySaltKg = (record.studentsServed * saltNormG) / 1000.0
        val dailyCookingCost = record.studentsServed * cookingCostRate

        val allCustomItems = remember(configNorms) {
            CustomFoodItemParser.parse(configNorms?.customItemsJson)
        }
        val usedCustomIds = remember(record) {
            CustomFoodItemParser.parseUsedItemIds(record.customItemsUsedJson)
        }
        val usedCustomItems = remember(allCustomItems, usedCustomIds) {
            allCustomItems.filter { usedCustomIds.contains(it.id) }
        }

        AlertDialog(
            onDismissRequest = {
                showDailyShareDialog = false
                recordToShareOnWhatsApp = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) "दैनिक उपस्थिति प्रेषित करें" else "Share Daily Attendance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isHi) "स्व-सहायता समूह (SHG अध्यक्ष) व्हाट्सएप" else "SHG President (WhatsApp)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isShgConfigured) {
                        TextButton(
                            onClick = { showManualEditSection = !showManualEditSection },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showManualEditSection) (if (isHi) "रद्द करें" else "Cancel") else (if (isHi) "बदलें" else "Change"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary Preview Card
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "📅 $dateDisplay",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "👦 बालक: ${record.boysPresent} • 👧 बालिका: ${record.girlsPresent}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "${if (isHi) "कुल उपस्थित" else "Present"}: ${record.studentsPresent}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            if (record.mealServed) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🍽️ ${if (isHi) "लाभान्वित" else "Served"}: ${record.studentsServed} छात्र",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = "💰 लागत: ₹${String.format(Locale.US, "%.2f", dailyCookingCost)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Entire Used Commodity Details Box
                                Surface(
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isHi) "📦 प्रयुक्त राशन एवं सामग्री आवश्यकता:" else "📦 Used Commodities & Ration:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )

                                        // 4 Key Commodities Grid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🌾 ${if (isHi) "चावल" else "Rice"}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF334155))
                                                    Text("${String.format(Locale.US, "%.3f", dailyRiceKg)} kg", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }
                                            }
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🥣 ${if (isHi) "दाल" else "Pulses"}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF334155))
                                                    Text("${String.format(Locale.US, "%.3f", dailyPulseKg)} kg", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }
                                            }
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🥬 ${if (isHi) "सब्जी" else "Veg"}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF334155))
                                                    Text("${String.format(Locale.US, "%.3f", dailyVegKg)} kg", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }
                                            }
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🛢️ ${if (isHi) "तेल" else "Oil"}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF334155))
                                                    Text("${String.format(Locale.US, "%.3f", dailyOilKg)} kg", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }
                                            }
                                        }

                                        // Special / Custom food commodities
                                        if (usedCustomItems.isNotEmpty()) {
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                usedCustomItems.forEach { item ->
                                                    val totalItemQty = record.studentsServed * item.quantity
                                                    val u = item.unit.lowercase()
                                                    val qtyStr = if (u == "kg" || u == "ml" || u == "l" || u == "ltr") {
                                                        String.format(Locale.US, "%.3f", totalItemQty)
                                                    } else if (totalItemQty == totalItemQty.toLong().toDouble()) {
                                                        totalItemQty.toLong().toString()
                                                    } else {
                                                        String.format(Locale.US, "%.3f", totalItemQty).trimEnd('0').trimEnd('.')
                                                    }
                                                    Surface(
                                                        color = Color(0xFFDCFCE7),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "✨ ${item.getDisplayName(isHi)}: $qtyStr ${item.getDisplayUnit(isHi)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF166534),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }


                                if (record.menuDetails.isNotBlank()) {
                                    Text(
                                        text = "🍲 मेनू: ${record.menuDetails}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (record.photoUri.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHi) "भोजन का फोटो भी साथ भेजा जाएगा" else "Meal photo will be attached",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BluePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (!showManualEditSection && isShgConfigured) {
                        // 1-Click Send to SHG President (Identical to Reports Screen format)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.2.dp, Color(0xFF86EFAC))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHi) "स्व-सहायता समूह (SHG अध्यक्ष)" else "SHG President (WhatsApp)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF14532D)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = "✓ सक्रिय",
                                            color = Color(0xFF15803D),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "समूह: $shgName\nसंपर्क: $shgPresident (+91 $currentShgMobile)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF166534),
                                    lineHeight = 16.sp
                                )

                                Button(
                                    onClick = {
                                        val msg = buildDailyAttendanceMessage(record, school, activeAgency, configNorms)
                                        PdfReportGenerator.shareDailyAttendanceToWhatsApp(
                                            context = context,
                                            rawMobileNumber = currentShgMobile,
                                            message = msg,
                                            photoUriString = record.photoUri
                                        )
                                        showDailyShareDialog = false
                                        recordToShareOnWhatsApp = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHi) "स्व-सहायता समूह को 1-क्लिक भेजें (WhatsApp)" else "1-Click Send to SHG (WhatsApp)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        // Manual Mobile Input Form
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isHi) "स्व-सहायता समूह (SHG) अध्यक्ष का मोबाइल नंबर दर्ज करें" else "Enter SHG President Mobile Number",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                OutlinedTextField(
                                    value = editShgMobile,
                                    onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) editShgMobile = it },
                                    label = { Text(if (isHi) "व्हाट्सएप मोबाइल नंबर" else "WhatsApp Mobile Number") },
                                    prefix = { Text("+91 ") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (editShgMobile.length >= 10) {
                                            val existingAgency = activeAgency ?: CookingAgencyEntity(
                                                agencyId = "SHG-DEFAULT-01",
                                                name = "स्व-सहायता समूह",
                                                contactPerson = "अध्यक्ष / सचिव",
                                                mobile = editShgMobile,
                                                address = "",
                                                villageOrCity = ""
                                            )
                                            viewModel.saveCookingAgency(existingAgency.copy(mobile = editShgMobile))

                                            val msg = buildDailyAttendanceMessage(record, school, existingAgency.copy(mobile = editShgMobile), configNorms)
                                            PdfReportGenerator.shareDailyAttendanceToWhatsApp(
                                                context = context,
                                                rawMobileNumber = editShgMobile,
                                                message = msg,
                                                photoUriString = record.photoUri
                                            )
                                            showDailyShareDialog = false
                                            recordToShareOnWhatsApp = null
                                        }
                                    },
                                    enabled = editShgMobile.length >= 10,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHi) "नंबर सुरक्षित करें एवं भेजें (WhatsApp)" else "Save Number & Send (WhatsApp)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showDailyShareDialog = false
                        recordToShareOnWhatsApp = null
                    }
                ) {
                    Text(
                        text = if (isHi) "बंद करें" else "Close",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        )
    }

    // SUCCESS SNACKBAR ON DELETE
    if (deleteSuccessDate != null) {
        val dateToNotify = deleteSuccessDate
        LaunchedEffect(dateToNotify) {
            if (dateToNotify != null) {
                snackbarHostState.showSnackbar(
                    if (isHi) "${dateToNotify.toDisplayDate()} की प्रविष्टि सफलतापूर्वक हटा दी गई।"
                    else "Entry for ${dateToNotify.toDisplayDate()} deleted successfully."
                )
                deleteSuccessDate = null
            }
        }
    }
}

/**
 * Builds a structured, official Hindi/English WhatsApp message for the SHG President
 * containing all daily attendance, meal served, grain consumption, cooking cost, and menu details.
 */
fun buildDailyAttendanceMessage(
    record: DailyMealRecordEntity,
    school: SchoolEntity?,
    agency: CookingAgencyEntity?,
    norms: ConfigNormsEntity?
): String {
    val dateDisplay = try {
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
        val sdfOut = SimpleDateFormat("dd-MM-yyyy (EEEE)", Locale("hi", "IN")).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
        val parsed = sdfIn.parse(record.date)
        if (parsed != null) sdfOut.format(parsed) else record.date
    } catch (_: Exception) {
        record.date
    }

    val schoolName = school?.schoolName ?: "शासकीय पूर्व माध्यमिक शाला"
    val udiseCode = school?.udiseCode ?: ""
    val shgName = agency?.name?.ifBlank { "स्व-सहायता समूह" } ?: "स्व-सहायता समूह"
    val shgPresident = agency?.contactPerson?.ifBlank { "अध्यक्ष / सचिव" } ?: "अध्यक्ष / सचिव"

    val schoolType = school?.schoolType ?: "PRIMARY"
    val isMiddle = schoolType == "MIDDLE" || schoolType == "UPPER_PRIMARY"
    val ricePerStudentGrams = if (isMiddle) {
        norms?.upperPrimaryRiceNormGrams ?: 150.0
    } else {
        norms?.primaryRiceNormGrams ?: 150.0
    }
    val pulseNormG = norms?.pulseNormGrams ?: 30.0
    val vegNormG = norms?.vegetableNormGrams ?: 75.0
    val oilNormG = norms?.oilNormGrams ?: 7.5
    val saltNormG = norms?.saltNormGrams ?: 5.0

    val cookingCostRate = if (isMiddle) {
        if ((norms?.middleReimbursementRate ?: 0.0) > 0.0) norms!!.middleReimbursementRate
        else (norms?.cookingCostRate ?: 10.17)
    } else {
        if ((norms?.primaryReimbursementRate ?: 0.0) > 0.0) norms!!.primaryReimbursementRate
        else (norms?.cookingCostRate ?: 10.17)
    }

    val served = record.studentsServed
    val riceKg = if (record.riceConsumedKg > 0) record.riceConsumedKg else (served * ricePerStudentGrams / 1000.0)
    val pulseKg = (served * pulseNormG) / 1000.0
    val vegKg = (served * vegNormG) / 1000.0
    val oilKg = (served * oilNormG) / 1000.0
    val saltKg = (served * saltNormG) / 1000.0
    val cookingCost = served * cookingCostRate

    val allCustomItems = CustomFoodItemParser.parse(norms?.customItemsJson)
    val usedCustomIds = CustomFoodItemParser.parseUsedItemIds(record.customItemsUsedJson)
    val usedCustomItems = allCustomItems.filter { usedCustomIds.contains(it.id) }

    val sb = StringBuilder()
    sb.appendLine("*$schoolName*")
    if (udiseCode.isNotBlank()) {
        sb.appendLine("UDISE कोड: $udiseCode")
    }
    sb.appendLine("*मध्यान्ह भोजन (PM POSHAN) - दैनिक उपस्थिति एवं सामग्री विवरण*")
    sb.appendLine("━━━━━━━━━━━━━━━━━━━")
    sb.appendLine("📅 *दिनांक:* $dateDisplay")
    sb.appendLine("👩‍🍳 *एजेंसी:* $shgName")
    sb.appendLine("👤 *अध्यक्ष:* $shgPresident")
    sb.appendLine("━━━━━━━━━━━━━━━━━━━")
    sb.appendLine("📊 *उपस्थिति एवं भोजन विवरण:*")
    sb.appendLine("• कुल दर्ज छात्र: ${record.enrolledStudents}")
    sb.appendLine("• कुल उपस्थित छात्र: ${record.studentsPresent} (बालक: ${record.boysPresent}, बालिका: ${record.girlsPresent})")
    if (record.mealServed) {
        sb.appendLine("• भोजन लाभान्वित: *${record.studentsServed}* छात्र (बालक: ${record.boysServed}, बालिका: ${record.girlsServed})")
        if (cookingCost > 0) {
            sb.appendLine("• 💰 कुकिंग लागत राशि: *₹${String.format(Locale.US, "%.2f", cookingCost)}*")
        }
        if (record.menuDetails.isNotBlank()) {
            sb.appendLine("• 🍲 मेनू: ${record.menuDetails}")
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📦 *प्रयुक्त राशन एवं सामग्री विवरण (Commodity Details):*")
        sb.appendLine("• 🌾 चावल (Rice): *${String.format(Locale.US, "%.3f", riceKg)} kg*")
        sb.appendLine("• 🥣 दाल (Pulses): *${String.format(Locale.US, "%.3f", pulseKg)} kg*")
        sb.appendLine("• 🥬 हरी सब्जी (Vegetables): *${String.format(Locale.US, "%.3f", vegKg)} kg*")
        sb.appendLine("• 🛢️ खाद्य तेल (Cooking Oil): *${String.format(Locale.US, "%.3f", oilKg)} kg*")
        if (saltKg > 0) {
            sb.appendLine("• 🧂 नमक (Salt): *${String.format(Locale.US, "%.3f", saltKg)} kg*")
        }

        if (usedCustomItems.isNotEmpty()) {
            sb.appendLine("━━━━━━━━━━━━━━━━━━━")
            sb.appendLine("✨ *विशेष / अनुपूरक खाद्य सामग्री (Special Commodities):*")
            usedCustomItems.forEach { item ->
                val totalItemQty = served * item.quantity
                val u = item.unit.lowercase()
                val qtyStr = if (u == "kg" || u == "ml" || u == "l" || u == "ltr") {
                    String.format(Locale.US, "%.3f", totalItemQty)
                } else if (totalItemQty == totalItemQty.toLong().toDouble()) {
                    totalItemQty.toLong().toString()
                } else {
                    String.format(Locale.US, "%.3f", totalItemQty).trimEnd('0').trimEnd('.')
                }
                sb.appendLine("• ✨ ${item.getDisplayName(true)}: *$qtyStr ${item.getDisplayUnit(true)}*")
            }
        }

        if (record.tastingDone) {
            sb.appendLine("━━━━━━━━━━━━━━━━━━━")
            sb.appendLine("✓ भोजन चखना: ${record.tastedBy} (गुणवत्ता: ${record.tasteQuality})")
        }
    } else {
        sb.appendLine("⚠️ आज भोजन वितरित नहीं हुआ")
        if (record.holidayReason.isNotBlank()) {
            sb.appendLine("कारण: ${record.holidayReason}")
        }
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━━")
    sb.appendLine("_यह संदेश PM POSHAN प्रबंधन पंजी से प्रेषित किया गया है।_")
    return sb.toString()
}
