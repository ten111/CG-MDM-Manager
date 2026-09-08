package com.example.presentation.meal

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import com.example.data.local.entity.CookAttendanceEntity
import com.example.data.local.entity.CookEntity
import com.example.data.local.entity.CookingAgencyEntity
import com.example.data.local.entity.DailyMealRecordEntity
import com.example.data.model.CustomFoodItemParser
import com.example.presentation.common.*
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.reports.PdfReportGenerator
import com.example.reminder.ReminderManager
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DailyMealScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val enrollment by viewModel.currentMonthEnrollment.collectAsState()
    val activeCooks by viewModel.activeCooks.collectAsState()
    val existingMealRecord by viewModel.dateMealRecord.collectAsState()
    val existingCookAttendances by viewModel.dateCookAttendances.collectAsState()
    val allCalendarEvents by viewModel.allCalendarEvents.collectAsState()
    val configNorms by viewModel.configNorms.collectAsState()
    val monthMealRecords by viewModel.monthMealRecords.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI
    val school by viewModel.school.collectAsState()
    val agencies by viewModel.allAgencies.collectAsState()
    val activeAgency = agencies.firstOrNull()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showAddCookDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<DailyMealRecordEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteSuccessDate by remember { mutableStateOf<String?>(null) }
    var showDailyShareDialog by remember { mutableStateOf(false) }

    val istZone = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone } }
    val displaySdf = remember(isHi) {
        SimpleDateFormat("dd MMMM yyyy (EEEE)", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).apply {
            timeZone = istZone
        }
    }

    val todayDateStr = remember { sdf.format(Date()) }
    val isPastDate = selectedDate < todayDateStr
    val isFutureDate = selectedDate > todayDateStr

    // Determine calendar status for the selected date
    val calendarEvent = allCalendarEvents.find { it.eventDate == selectedDate }
    val isSunday = try {
        val cal = Calendar.getInstance(istZone).apply { time = sdf.parse(selectedDate) ?: Date() }
        cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    } catch (e: Exception) { false }

    val isSpecialWorkingDay = calendarEvent?.eventType == "SPECIAL_WORKING_DAY"
    val isHoliday = !isSpecialWorkingDay && (
        (calendarEvent != null && calendarEvent.eventType != "WORKING_DAY") || isSunday
    )
    var isHolidayOverride by remember(selectedDate, existingMealRecord) {
        val record = existingMealRecord
        mutableStateOf(record != null && record.studentsServed > 0)
    }

    val totalEnrolled = enrollment?.totalEnrollment ?: 0
    val enrolledBoys = enrollment?.totalBoys ?: 0
    val enrolledGirls = enrollment?.totalGirls ?: 0

    // Local form state reactive to selectedDate / existingMealRecord - Defaults to empty for manual user entry
    var boysPresentText by remember(selectedDate, existingMealRecord) {
        val record = existingMealRecord
        mutableStateOf(if (record != null) record.boysPresent.toString() else "")
    }

    var girlsPresentText by remember(selectedDate, existingMealRecord) {
        val record = existingMealRecord
        mutableStateOf(if (record != null) record.girlsPresent.toString() else "")
    }

    var mealServed by remember(selectedDate, existingMealRecord) {
        mutableStateOf(existingMealRecord?.mealServed ?: true)
    }

    var boysServedText by remember(selectedDate, existingMealRecord) {
        val record = existingMealRecord
        mutableStateOf(if (record != null) record.boysServed.toString() else "")
    }

    var girlsServedText by remember(selectedDate, existingMealRecord) {
        val record = existingMealRecord
        mutableStateOf(if (record != null) record.girlsServed.toString() else "")
    }

    var menuDetails by remember(selectedDate, existingMealRecord) {
        mutableStateOf(existingMealRecord?.menuDetails ?: "चावल, दाल, मौसमी सब्जी (Rice, Dal, Mixed Veg Curry)")
    }
    var tastingDone by remember(selectedDate, existingMealRecord) {
        mutableStateOf(existingMealRecord?.tastingDone ?: true)
    }
    var tastedBy by remember(selectedDate, existingMealRecord, school) {
        mutableStateOf(existingMealRecord?.tastedBy ?: "${school?.headTeacherName ?: if (isHi) "प्रधान पाठक" else "Head Teacher"} एवं रसोइया")
    }
    var tasteQuality by remember(selectedDate, existingMealRecord) {
        mutableStateOf(existingMealRecord?.tasteQuality ?: "GOOD")
    }
    var hygieneChecked by remember(selectedDate, existingMealRecord) {
        mutableStateOf(existingMealRecord?.hygieneChecked ?: true)
    }
    var mealPhotoUri by remember(selectedDate, existingMealRecord) {
        mutableStateOf(existingMealRecord?.photoUri ?: "")
    }
    var showFullPhotoPreview by remember { mutableStateOf(false) }
    var previewingPhotoUri by remember { mutableStateOf("") }
    var showSamplePhotoPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Gallery Picker Launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            mealPhotoUri = uri.toString()
        }
    }

    // Camera Capture Launcher (returns Bitmap)
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "mdm_photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                mealPhotoUri = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraCaptureLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isHi) "कैमरा उपलब्ध नहीं है। कृपया गैलरी या नमूना फोटो चुनें।" else "Camera not available. Please choose from gallery.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                if (isHi) "कैमरा अनुमति अस्वीकृत। कृपया सेटिंग से अनुमति दें या गैलरी का उपयोग करें।" else "Camera permission denied. Please allow in settings or use gallery.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val safeLaunchCamera: () -> Unit = {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraCaptureLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isHi) "कैमरा उपलब्ध नहीं है। कृपया गैलरी या नमूना फोटो चुनें।" else "Camera not available. Please choose from gallery.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            try {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isHi) "कैमरा खोलने में असमर्थ" else "Unable to open camera",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val safeLaunchGallery: () -> Unit = {
        try {
            galleryPickerLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(
                context,
                if (isHi) "गैलरी खोलने में असमर्थ" else "Unable to open gallery",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Cook Attendance State map: cookId -> (isPresent, reason)
    var cookAttendanceMap by remember(selectedDate, activeCooks, existingCookAttendances) {
        val initialMap = mutableStateMapOf<String, Pair<Boolean, String>>()
        activeCooks.forEach { cook ->
            val existing = existingCookAttendances.find { it.cookId == cook.cookId }
            if (existing != null) {
                initialMap[cook.cookId] = Pair(existing.isPresent, existing.absenceReason)
            } else {
                initialMap[cook.cookId] = Pair(true, "") // Default present
            }
        }
        mutableStateOf(initialMap)
    }

    // Custom Food Items from Norms Module (Only active/enabled items)
    val customFoodItems = remember(configNorms) {
        val parsed = com.example.data.model.CustomFoodItemParser.parse(configNorms?.customItemsJson)
        val all = if (parsed.isNotEmpty()) parsed else com.example.data.model.CustomFoodItemParser.getDefaultCustomItems()
        all.filter { it.isEnabled }
    }

    // Checked custom item IDs for today's meal
    val customItemUsageMap = remember(selectedDate, existingMealRecord, customFoodItems) {
        val map = mutableStateMapOf<String, Boolean>()
        val existing = existingMealRecord
        if (existing != null && existing.customItemsUsedJson.isNotBlank()) {
            val usedSet = com.example.data.model.CustomFoodItemParser.parseUsedItemIds(existing.customItemsUsedJson)
            customFoodItems.forEach { item ->
                map[item.id] = usedSet.contains(item.id)
            }
        } else {
            // For new entries or legacy records, default to enabled items in norms
            customFoodItems.forEach { item ->
                map[item.id] = item.isEnabled
            }
        }
        map
    }

    var isCustomItemsSectionExpanded by remember { mutableStateOf(true) }

    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Derived numbers for display
    val parsedBoysPresent = boysPresentText.toIntOrNull() ?: 0
    val parsedGirlsPresent = girlsPresentText.toIntOrNull() ?: 0
    val totalPresentCount = parsedBoysPresent + parsedGirlsPresent

    val parsedBoysServed = boysServedText.toIntOrNull() ?: 0
    val parsedGirlsServed = girlsServedText.toIntOrNull() ?: 0
    val totalServedCount = if (mealServed) (parsedBoysServed + parsedGirlsServed) else 0

    fun navigateDateBy(days: Int) {
        try {
            val cal = Calendar.getInstance(istZone).apply { time = sdf.parse(selectedDate) ?: Date() }
            cal.add(Calendar.DAY_OF_YEAR, days)
            viewModel.setSelectedDate(sdf.format(cal.time))
        } catch (e: Exception) {
            // ignore
        }
    }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "दैनिक भोजन एवं उपस्थिति" else "Daily Meal & Attendance",
                subtitle = if (isHi) "दिनांक: ${selectedDate.toDisplayDate()}" else "Date: ${selectedDate.toDisplayDate()}",
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() },
                onSyncClick = {}
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BackgroundLight)
        ) {
            // 1. FROZEN DATE NAVIGATION & SELECTION HEADER CARD (Freezes at the top while scrolling)
            Surface(
                color = BackgroundLight,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Day button
                                IconButton(
                                    onClick = { navigateDateBy(-1) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BluePrimary.copy(alpha = 0.1f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Day",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Center Date Title & Calendar Pick Trigger
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { showDatePickerDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedDate.toDisplayDate(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Pick Date",
                                            tint = BluePrimary
                                        )
                                    }
                                    val formattedDateDesc = try {
                                        displaySdf.format(sdf.parse(selectedDate) ?: Date())
                                    } catch (e: Exception) { selectedDate }
                                    Text(
                                        text = formattedDateDesc,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )

                                    if (isHoliday) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                                        ) {
                                            val holidayTitle = calendarEvent?.eventName ?: (if (isHi) "रविवार (साप्ताहिक अवकाश)" else "Sunday (Weekly Off)")
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.EventBusy,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${if (isHi) "अवकाश:" else "Holiday:"} $holidayTitle",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF991B1B)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Next Day button
                                IconButton(
                                    onClick = { navigateDateBy(1) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BluePrimary.copy(alpha = 0.1f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Day",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Date Chips (Today, Yesterday, 2 Days ago, 3 Days ago)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val datesList = (0..3).map { offset ->
                                    val c = Calendar.getInstance(istZone).apply {
                                        time = Date()
                                        add(Calendar.DAY_OF_YEAR, -offset)
                                    }
                                    val dStr = sdf.format(c.time)
                                    val label = when (offset) {
                                        0 -> if (isHi) "आज (Today)" else "Today"
                                        1 -> if (isHi) "कल (Yest)" else "Yesterday"
                                        2 -> if (isHi) "2 दिन पूर्व" else "-2 Days"
                                        else -> if (isHi) "3 दिन पूर्व" else "-3 Days"
                                    }
                                    Pair(dStr, label)
                                }

                                datesList.forEach { (dStr, lbl) ->
                                    FilterChip(
                                        selected = selectedDate == dStr,
                                        onClick = { viewModel.setSelectedDate(dStr) },
                                        label = {
                                            Text(
                                                lbl,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = poshanFilterChipColors(),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = Color(0xFFCBD5E1),
                                            selectedBorderColor = BluePrimary,
                                            enabled = true,
                                            selected = selectedDate == dStr
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scrollable Content Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

            // PAST DATE NOTICE BANNER
            if (isPastDate) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color(0xFFB45309))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHi) "📅 पूर्व तिथि की प्रविष्टि (Backfilled Attendance / Meal)" else "📅 Previous Date Entry",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = if (isHi)
                                    "आप ${selectedDate.toDisplayDate()} की उपस्थिति एवं भोजन प्रविष्टि दर्ज कर रहे हैं। यह उस तिथि के स्टॉक लेजर में दर्ज होगी।"
                                else
                                    "You are backfilling attendance and meal entry for ${selectedDate.toDisplayDate()}. Stock will be updated accordingly.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }

            // STATUS BANNER (RECORDED OR NEW)
            if (existingMealRecord != null) {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF166534))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHi) "✓ इस तिथि का रिकॉर्ड पहले से दर्ज है (संपादित कर सकते हैं)" else "✓ Record already exists (Editable)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                        StatusBadge(status = "RECORDED")
                    }
                }
            }

            // HOLIDAY / SPECIAL WORKING DAY CHECK
            if (isHoliday && !isSpecialWorkingDay && !isHolidayOverride) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏫", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHi) "शासकीय शाला अवकाश / कार्यमुक्त (School Closed)" else "School Holiday / Working Off",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusCriticalRed
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Holiday Category Badge
                        val catLabel = when (calendarEvent?.eventType?.uppercase()) {
                            "NATIONAL" -> if (isHi) "राष्ट्रीय अवकाश (National Holiday)" else "National Holiday"
                            "STATE" -> if (isHi) "राज्य अवकाश (State Holiday)" else "State Holiday"
                            "VACATION" -> if (isHi) "दीर्घ अवकाश (Vacation Break)" else "Vacation Break"
                            "SCHOOL" -> if (isHi) "शाला स्तरीय अवकाश (School Off)" else "School Holiday"
                            "LOCAL" -> if (isHi) "स्थानीय अवकाश (Local Holiday)" else "Local Holiday"
                            "EMERGENCY", "EMERGENCY_CLOSURE" -> if (isHi) "आपातकालीन अवकाश (Emergency Off)" else "Emergency Closure"
                            else -> if (isSunday) (if (isHi) "साप्ताहिक अवकाश (Sunday Off)" else "Weekly Sunday Off") else (if (isHi) "सामान्य अवकाश" else "General Holiday")
                        }
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${if (isHi) "अवकाश विवरण" else "Holiday"}: ${calendarEvent?.eventName ?: (if (isHi) "रविवार (Sunday)" else "Sunday")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (calendarEvent?.orderNumber?.isNotEmpty() == true) {
                            Text(
                                text = "आदेश क्र.: ${calendarEvent.orderNumber} • ${calendarEvent.authority}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3))
                        ) {
                            Text(
                                text = if (isHi) "भोजन प्रविष्टि: आवश्यक नहीं (कार्यमुक्त)" else "Meal Entry: Not Required (Working Off)",
                                color = StatusCriticalRed,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { isHolidayOverride = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHi) "विशेष अवसर पर भोजन प्रविष्टि दर्ज करें" else "Enter Special Meal Entry (Override)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                if (isHoliday && isHolidayOverride) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHi) "⚠️ अवकाश दिवस पर विशेष भोजन प्रविष्टि" else "⚠️ Special Meal on Holiday / Off Day",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "${calendarEvent?.eventName ?: (if (isHi) "रविवार" else "Sunday")} - ${if (isHi) "विशेष भोजन वितरण चालू" else "Meal distribution enabled"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF78350F)
                                )
                            }
                            TextButton(onClick = { isHolidayOverride = false }) {
                                Text(
                                    text = if (isHi) "अवकाश मोड" else "Holiday Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = StatusCriticalRed
                                )
                            }
                        }
                    }
                }
                if (isSpecialWorkingDay) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = BluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHi) "विशेष कार्यदिवस (Special Working Day) - भोजन वितरण अनिवार्य" else "Special Working Day - Meal Serving Mandatory",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                    }
                }

                // STEP 1: ATTENDANCE & MEALS SERVED (BOYS & GIRLS BREAKDOWN)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHi) "1. छात्र उपस्थिति एवं भोजन वितरण" else "1. Student Attendance & Meal Distribution",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                            Surface(
                                color = BluePrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${if (isHi) "कुल दर्ज" else "Enrolled"}: $totalEnrolled",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHi) "दर्ज छात्र विवरण: 👦 बालक (Boys): $enrolledBoys • 👧 बालिका (Girls): $enrolledGirls"
                            else "Enrollment Breakdown: 👦 Boys: $enrolledBoys • 👧 Girls: $enrolledGirls",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Subsection A: ATTENDANCE (Boys & Girls)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isHi) "शाला में दैनिक उपस्थिति (Daily Attendance):" else "Daily Attendance:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Boys Present Input
                            OutlinedTextField(
                                value = boysPresentText,
                                onValueChange = {
                                    boysPresentText = it
                                    if (mealServed) boysServedText = it
                                },
                                label = {
                                    Text(
                                        text = if (isHi) "👦 उपस्थित बालक *" else "👦 Boys Present *",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                placeholder = { Text("अधिकतम $enrolledBoys", fontSize = 11.sp, maxLines = 1) },
                                supportingText = {
                                    Text(
                                        text = "${if (isHi) "दर्ज बालक" else "Enrolled"}: $enrolledBoys",
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = poshanTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            // Girls Present Input
                            OutlinedTextField(
                                value = girlsPresentText,
                                onValueChange = {
                                    girlsPresentText = it
                                    if (mealServed) girlsServedText = it
                                },
                                label = {
                                    Text(
                                        text = if (isHi) "👧 उपस्थित बालिका *" else "👧 Girls Present *",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                placeholder = { Text("अधिकतम $enrolledGirls", fontSize = 11.sp, maxLines = 1) },
                                supportingText = {
                                    Text(
                                        text = "${if (isHi) "दर्ज बालिका" else "Enrolled"}: $enrolledGirls",
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = poshanTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // Attendance Summary Box
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = if (totalPresentCount > 0) Color(0xFF166534) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHi) "कुल उपस्थित छात्र (Total Present):" else "Total Present:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                val pct = if (totalEnrolled > 0) (totalPresentCount * 100.0 / totalEnrolled) else 0.0
                                Text(
                                    text = "$totalPresentCount / $totalEnrolled (${String.format(Locale.US, "%.1f", pct)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalPresentCount > totalEnrolled) StatusCriticalRed else BluePrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Subsection B: MEAL SERVED SWITCH
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isHi) "क्या आज भोजन परोसा गया? *" else "Was Meal Served Today? *",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (mealServed) (if (isHi) "हाँ, बच्चों को गर्म ताजा भोजन वितरित किया गया" else "Yes, hot cooked meal served")
                                    else (if (isHi) "नहीं (शून्य राशन खपत)" else "No (Zero consumption)"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (mealServed) Color(0xFF166534) else StatusCriticalRed
                                )
                            }
                            Switch(
                                checked = mealServed,
                                onCheckedChange = {
                                    mealServed = it
                                    if (it) {
                                        boysServedText = boysPresentText
                                        girlsServedText = girlsPresentText
                                    } else {
                                        boysServedText = "0"
                                        girlsServedText = "0"
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BluePrimary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFF94A3B8)
                                )
                            )
                        }

                        // Subsection C: MEAL BENEFITED (BOYS & GIRLS)
                        if (mealServed) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHi) "भोजन लाभान्वित छात्र संख्या (Meals Served):" else "Students Benefited:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                // Quick sync button
                                TextButton(
                                    onClick = {
                                        boysServedText = boysPresentText
                                        girlsServedText = girlsPresentText
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = BluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHi) "उपस्थिति के अनुसार भरें" else "Fill from Attendance",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Boys Served Input
                                OutlinedTextField(
                                    value = boysServedText,
                                    onValueChange = { boysServedText = it },
                                    label = {
                                        Text(
                                            text = if (isHi) "👦 लाभान्वित बालक *" else "👦 Boys Served *",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    placeholder = { Text("अधिकतम $parsedBoysPresent", fontSize = 11.sp, maxLines = 1) },
                                    supportingText = {
                                        Text(
                                            text = "${if (isHi) "उपस्थित बालक" else "Present"}: $parsedBoysPresent",
                                            color = Color(0xFF334155),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = poshanTextFieldColors(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                // Girls Served Input
                                OutlinedTextField(
                                    value = girlsServedText,
                                    onValueChange = { girlsServedText = it },
                                    label = {
                                        Text(
                                            text = if (isHi) "👧 लाभान्वित बालिका *" else "👧 Girls Served *",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    placeholder = { Text("अधिकतम $parsedGirlsPresent", fontSize = 11.sp, maxLines = 1) },
                                    supportingText = {
                                        Text(
                                            text = "${if (isHi) "उपस्थित बालिका" else "Present"}: $parsedGirlsPresent",
                                            color = Color(0xFF334155),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = poshanTextFieldColors(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            // Consumption Preview Bar & Norms Breakdown
                            val riceNormG = configNorms?.upperPrimaryRiceNormGrams ?: 150.0
                            val pulseNormG = configNorms?.pulseNormGrams ?: 30.0
                            val vegNormG = configNorms?.vegetableNormGrams ?: 75.0
                            val oilNormG = configNorms?.oilNormGrams ?: 7.5
                            val saltNormG = configNorms?.saltNormGrams ?: 5.0
                            val cookingCostRate = configNorms?.cookingCostRate ?: 10.17

                            val estRiceKg = (totalServedCount * riceNormG) / 1000.0
                            val estPulseKg = (totalServedCount * pulseNormG) / 1000.0
                            val estVegKg = (totalServedCount * vegNormG) / 1000.0
                            val estOilKg = (totalServedCount * oilNormG) / 1000.0
                            val estSaltKg = (totalServedCount * saltNormG) / 1000.0
                            val estTotalCost = totalServedCount * cookingCostRate

                            val activeCustomItems = remember(configNorms) {
                                com.example.data.model.CustomFoodItemParser.parse(configNorms?.customItemsJson).filter { it.isEnabled }
                            }

                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isHi) "📊 अनुमानित राशन एवं सामग्री आवश्यकता:" else "📊 Estimated Material & Ration Requirement:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                        Text(
                                            text = "${if (isHi) "कुल" else "Total"}: $totalServedCount छात्र",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                    }

                                    // Material Grid / Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🌾 ${if (isHi) "चावल" else "Rice"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
                                                Text("${String.format(Locale.US, "%.2f", estRiceKg)} kg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            }
                                        }
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🥣 ${if (isHi) "दाल" else "Pulses"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
                                                Text("${String.format(Locale.US, "%.2f", estPulseKg)} kg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            }
                                        }
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🥬 ${if (isHi) "सब्जी" else "Veg"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
                                                Text("${String.format(Locale.US, "%.2f", estVegKg)} kg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            }
                                        }
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🛢️ ${if (isHi) "तेल" else "Oil"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
                                                Text("${String.format(Locale.US, "%.3f", estOilKg)} kg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            }
                                        }
                                    }

                                    // Second row: Cooking Cost and Custom Items
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💰 ${if (isHi) "अनुमानित कुकिंग लागत" else "Est. Cooking Cost"}: ₹${String.format(Locale.US, "%.2f", estTotalCost)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF334155)
                                        )
                                    }

                                    val checkedCustomItemsForEst = customFoodItems.filter { customItemUsageMap[it.id] == true }
                                    if (checkedCustomItemsForEst.isNotEmpty()) {
                                        androidx.compose.foundation.layout.FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            checkedCustomItemsForEst.take(4).forEach { item ->
                                                val totalItemQty = totalServedCount * item.quantity
                                                Surface(
                                                    color = Color(0xFFDCFCE7),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "✨ ${item.getDisplayName(isHi)}: ${if (totalItemQty == totalItemQty.toLong().toDouble()) totalItemQty.toLong().toString() else String.format(Locale.US, "%.2f", totalItemQty)} ${item.getDisplayUnit(isHi)}",
                                                        style = MaterialTheme.typography.labelSmall,
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
                        }
                    }
                }

                // STEP 2: CUSTOM / SPECIAL FOOD ITEMS SECTION (COLLAPSIBLE WITH CHECKBOXES)
                val usedCustomItemsCount = customFoodItems.count { customItemUsageMap[it.id] == true }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCustomItemsSectionExpanded = !isCustomItemsSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isHi) "2. अतिरिक्त/विशेष पोषण सामग्री (${usedCustomItemsCount}/${customFoodItems.size})"
                                               else "2. Special / Custom Food Items (${usedCustomItemsCount}/${customFoodItems.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                    Surface(
                                        color = if (usedCustomItemsCount > 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (usedCustomItemsCount > 0) {
                                                if (isHi) "✓ $usedCustomItemsCount चयनित" else "✓ $usedCustomItemsCount Selected"
                                            } else {
                                                if (isHi) "कोई नहीं" else "None"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (usedCustomItemsCount > 0) Color(0xFF166534) else Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isHi) "आज के भोजन में प्रयुक्त सामग्री चुनें (स्टॉक खपत गणना हेतु)"
                                           else "Select items used in today's meal for stock usage tracking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }

                            IconButton(
                                onClick = { isCustomItemsSectionExpanded = !isCustomItemsSectionExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCustomItemsSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isCustomItemsSectionExpanded) "Collapse" else "Expand",
                                    tint = BluePrimary
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isCustomItemsSectionExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                if (customFoodItems.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                customFoodItems.forEach { item ->
                                                    customItemUsageMap[item.id] = true
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isHi) "सभी चुनें ✓" else "Select All ✓",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BluePrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        TextButton(
                                            onClick = {
                                                customFoodItems.forEach { item ->
                                                    customItemUsageMap[item.id] = false
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isHi) "सभी हटाएं ✕" else "Clear All ✕",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusCriticalRed
                                            )
                                        }
                                    }

                                    customFoodItems.forEach { item ->
                                        val isChecked = customItemUsageMap[item.id] == true
                                        val totalCalculatedQty = totalServedCount * item.quantity
                                        val formattedCalcQty = if (totalCalculatedQty == totalCalculatedQty.toLong().toDouble()) {
                                            totalCalculatedQty.toLong().toString()
                                        } else {
                                            String.format(Locale.US, "%.3f", totalCalculatedQty)
                                        }

                                        val itemIcon = when {
                                            item.id.contains("ifa", ignoreCase = true) || item.name.contains("iron", ignoreCase = true) || item.name.contains("folic", ignoreCase = true) || item.name.contains("tablet", ignoreCase = true) || item.nameHi.contains("आयरन") || item.nameHi.contains("टेबलेट") -> Icons.Default.HealthAndSafety
                                            item.id.contains("dudh", ignoreCase = true) || item.name.contains("milk", ignoreCase = true) || item.nameHi.contains("दूध") -> Icons.Default.LocalCafe
                                            item.id.contains("badi", ignoreCase = true) || item.id.contains("soya", ignoreCase = true) || item.name.contains("soya", ignoreCase = true) || item.nameHi.contains("सोया") || item.nameHi.contains("बड़ी") || item.nameHi.contains("बड़ी") -> Icons.Default.Grain
                                            item.name.contains("Egg", ignoreCase = true) || item.nameHi.contains("अंडा") -> Icons.Default.Egg
                                            item.name.contains("Fruit", ignoreCase = true) || item.nameHi.contains("फल") -> Icons.Default.LocalFlorist
                                            else -> Icons.Default.Fastfood
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isChecked) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isChecked) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    customItemUsageMap[item.id] = !isChecked
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            customItemUsageMap[item.id] = checked
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = Color(0xFF166534),
                                                            checkmarkColor = Color.White
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isChecked) Color(0xFFDCFCE7) else Color(0xFFE2E8F0)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = itemIcon,
                                                            contentDescription = null,
                                                            tint = if (isChecked) Color(0xFF166534) else Color(0xFF64748B),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = item.getDisplayName(isHi),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isChecked) Color(0xFF0F172A) else Color(0xFF475569)
                                                        )
                                                        Text(
                                                            text = "${item.formattedQuantity()} ${item.getDisplayUnit(isHi)} " + if (isHi) "प्रति छात्र" else "per child",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }

                                                if (isChecked && totalServedCount > 0) {
                                                    Surface(
                                                        color = Color(0xFFDCFCE7),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            horizontalAlignment = Alignment.End
                                                        ) {
                                                            Text(
                                                                text = "$formattedCalcQty ${item.getDisplayUnit(isHi)}",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF166534)
                                                            )
                                                            Text(
                                                                text = if (isHi) "खपत ($totalServedCount छात्र)" else "Used ($totalServedCount std)",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                color = Color(0xFF166534)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = if (isHi) "कोई अतिरिक्त सामग्री सेटिंग्स में सक्रिय नहीं है।" else "No custom food items configured in Norms Settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // STEP 3: COOK ATTENDANCE
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val presentCooksCount = activeCooks.count { cookAttendanceMap[it.cookId]?.first == true }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = if (isHi) "3. रसोइया उपस्थिति (${presentCooksCount}/${activeCooks.size})" else "3. Cook Attendance (${presentCooksCount}/${activeCooks.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                                Text(
                                    text = if (isHi) "चेकबॉक्स पर क्लिक कर उपस्थिति दर्ज करें" else "Click checkbox to mark presence",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            if (activeCooks.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            activeCooks.forEach { cook ->
                                                cookAttendanceMap[cook.cookId] = Pair(true, "")
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isHi) "सभी उपस्थित ✓" else "All Present ✓",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(
                                        onClick = {
                                            activeCooks.forEach { cook ->
                                                cookAttendanceMap[cook.cookId] = Pair(false, "अवकाश (Leave)")
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isHi) "सभी हटाएं ✕" else "Clear All ✕",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusCriticalRed
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (activeCooks.isEmpty()) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isHi) "कोई सक्रिय रसोइया दर्ज नहीं है। रसोइया प्रबंधन मेनू से रसोइया जोड़ें।" else "No active cooks registered. Add cooks from Cook Management.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            activeCooks.forEach { cook ->
                                val currentPair = cookAttendanceMap[cook.cookId] ?: Pair(true, "")
                                val isPresent = currentPair.first
                                val reason = currentPair.second

                                Surface(
                                    color = if (isPresent) Color(0xFFF0FDF4) else Color(0xFFFFF1F2),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (isPresent) Color(0xFF86EFAC) else Color(0xFFFECDD3)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            val newPresent = !isPresent
                                            cookAttendanceMap[cook.cookId] = Pair(newPresent, if (newPresent) "" else "अवकाश (Leave)")
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Checkbox(
                                                    checked = isPresent,
                                                    onCheckedChange = { checked ->
                                                        cookAttendanceMap[cook.cookId] = Pair(checked, if (checked) "" else "अवकाश (Leave)")
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = GreenPrimary,
                                                        uncheckedColor = Color(0xFF94A3B8)
                                                    )
                                                )
                                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                                    Text(
                                                        text = cook.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "मो: ${cook.mobile} • समूह: ${cook.associatedAgency}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                            }

                                            Surface(
                                                color = if (isPresent) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isPresent) (if (isHi) "✓ उपस्थित (Present)" else "✓ Present") else (if (isHi) "✗ अनुपस्थित (Absent)" else "✗ Absent"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPresent) Color(0xFF15803D) else Color(0xFFB91C1C),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (!isPresent) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (isHi) "अनुपस्थिति का कारण चुनें:" else "Select Absence Reason:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF64748B)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(IntrinsicSize.Min),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val absenceOptions = listOf(
                                                    Triple(
                                                        "leave",
                                                        if (isHi) "अवकाश (Leave)" else "Leave (अवकाश)",
                                                        0.95f
                                                    ),
                                                    Triple(
                                                        "illness",
                                                        if (isHi) "बीमारी (Illness)" else "Illness (बीमारी)",
                                                        0.80f
                                                    ),
                                                    Triple(
                                                        "uninformed",
                                                        if (isHi) "बिना सूचना\n(Un-informed)" else "Un-informed\n(बिना सूचना)",
                                                        1.45f
                                                    )
                                                )
                                                absenceOptions.forEach { (optKey, displayLabel, optWeight) ->
                                                    val isOptSelected = when (optKey) {
                                                        "leave" -> reason.contains("Leave") || reason.contains("अवकाश") || (reason.isBlank() && cookAttendanceMap[cook.cookId]?.first == false)
                                                        "illness" -> reason.contains("Illness") || reason.contains("बीमारी")
                                                        "uninformed" -> reason.contains("Un-informed") || reason.contains("बिना सूचना") || reason.contains("Personal") || reason.contains("व्यक्तिगत")
                                                        else -> false
                                                    }
                                                    val saveVal = when (optKey) {
                                                        "leave" -> if (isHi) "अवकाश (Leave)" else "Leave (अवकाश)"
                                                        "illness" -> if (isHi) "बीमारी (Illness)" else "Illness (बीमारी)"
                                                        "uninformed" -> if (isHi) "बिना सूचना (Un-informed)" else "Un-informed (बिना सूचना)"
                                                        else -> ""
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(optWeight)
                                                            .fillMaxHeight()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isOptSelected) BluePrimary else Color(0xFFF1F5F9))
                                                            .border(
                                                                androidx.compose.foundation.BorderStroke(
                                                                    1.dp,
                                                                    if (isOptSelected) BluePrimary else Color(0xFFCBD5E1)
                                                                ),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable {
                                                                cookAttendanceMap[cook.cookId] = Pair(false, saveVal)
                                                            }
                                                            .padding(horizontal = 4.dp, vertical = 7.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = displayLabel,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 11.sp,
                                                                lineHeight = 14.sp
                                                            ),
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (isOptSelected) Color.White else Color(0xFF1E293B),
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 2
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
                }

                // STEP 4: TASTING & HYGIENE VERIFICATION
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHi) "4. भोजन चखना, स्वच्छता एवं फोटो" else "4. Meal Tasting, Hygiene & Photo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            if (mealPhotoUri.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF166534),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHi) "फोटो संलग्न" else "Photo Attached",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tastingDone,
                                onCheckedChange = { tastingDone = it },
                                colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                            )
                            Text(
                                text = if (isHi) "भोजन वितरण पूर्व चखा गया (Tasted prior to serving)" else "Tasted prior to distribution",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hygieneChecked,
                                onCheckedChange = { hygieneChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                            )
                            Text(
                                text = if (isHi) "रसोई स्वच्छता, स्वच्छ जल एवं बर्तनों की जांच पूर्ण (Hygiene Verified)" else "Kitchen hygiene, clean water & utensils verified",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = menuDetails,
                            onValueChange = { menuDetails = it },
                            label = {
                                Text(
                                    if (isHi) "आज का मेनू (Menu Details) *" else "Today's Menu Details *",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = poshanTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PHOTO UPLOAD & CAPTURE SECTION
                        Text(
                            text = if (isHi) "📸 भोजन वितरण / स्वच्छता लाइव फोटो (Photo Evidence)" else "📸 Meal Distribution / Hygiene Photo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (isHi) "भोजन चखने, वितरण या स्वच्छ किचन की फोटो अपलोड करें"
                            else "Upload photo of meal tasting, student dining, or clean kitchen",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (mealPhotoUri.isEmpty()) {
                            // Upload Card with Direct Action Options
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isHi) "दैनिक मध्यान्ह भोजन की फोटो जोड़ें" else "Attach Mid-Day Meal Photo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isHi) "कैमरा से खींचें या गैलरी से चुनें" else "Capture with camera or choose from gallery",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { safeLaunchCamera() },
                                            colors = poshanButtonColors(containerColor = BluePrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHi) "कैमरा (Camera)" else "Camera",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { safeLaunchGallery() },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Collections,
                                                contentDescription = null,
                                                tint = BluePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHi) "गैलरी (Gallery)" else "Gallery",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BluePrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(
                                        onClick = { showSamplePhotoPicker = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            tint = StatusWarningOrange,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHi) "नमूना/सैंपल फोटो चुनें (Sample Photo)" else "Select Sample Photo",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusWarningOrange
                                        )
                                    }
                                }
                            }
                        } else {
                            // Photo Preview Container
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                previewingPhotoUri = mealPhotoUri
                                                showFullPhotoPreview = true
                                            }
                                    ) {
                                        AsyncImage(
                                            model = mealPhotoUri,
                                            contentDescription = "Meal Photo Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Tap to preview badge
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ZoomIn,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isHi) "बड़ा देखें" else "Full View",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF166534),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isHi) "फोटो सत्यापित" else "Photo Attached",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF166534)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { safeLaunchCamera() },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = BluePrimary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    if (isHi) "कैमरा" else "Camera",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BluePrimary
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = { safeLaunchGallery() },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Collections,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = BluePrimary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    if (isHi) "गैलरी" else "Gallery",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BluePrimary
                                                )
                                            }

                                            TextButton(
                                                onClick = { mealPhotoUri = "" },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = StatusCriticalRed
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    if (isHi) "हटाएं" else "Remove",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusCriticalRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // VALIDATION ERROR DISPLAY
                if (validationError != null) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ $validationError",
                            color = StatusCriticalRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // SUBMIT BUTTON
                Button(
                    onClick = {
                        if (parsedBoysPresent > enrolledBoys) {
                            validationError = if (isHi) "उपस्थित बालक संख्या ($parsedBoysPresent) कुल दर्ज बालक ($enrolledBoys) से अधिक नहीं हो सकती।"
                            else "Boys present ($parsedBoysPresent) cannot exceed enrolled boys ($enrolledBoys)."
                            return@Button
                        }
                        if (parsedGirlsPresent > enrolledGirls) {
                            validationError = if (isHi) "उपस्थित बालिका संख्या ($parsedGirlsPresent) कुल दर्ज बालिका ($enrolledGirls) से अधिक नहीं हो सकती।"
                            else "Girls present ($parsedGirlsPresent) cannot exceed enrolled girls ($enrolledGirls)."
                            return@Button
                        }
                        if (parsedBoysServed > parsedBoysPresent) {
                            validationError = if (isHi) "लाभान्वित बालक संख्या ($parsedBoysServed) उपस्थित बालक संख्या ($parsedBoysPresent) से अधिक नहीं हो सकती।"
                            else "Served boys ($parsedBoysServed) cannot exceed present boys ($parsedBoysPresent)."
                            return@Button
                        }
                        if (parsedGirlsServed > parsedGirlsPresent) {
                            validationError = if (isHi) "लाभान्वित बालिका संख्या ($parsedGirlsServed) उपस्थित बालिका संख्या ($parsedGirlsPresent) से अधिक नहीं हो सकती।"
                            else "Served girls ($parsedGirlsServed) cannot exceed present girls ($parsedGirlsPresent)."
                            return@Button
                        }

                        validationError = null

                        val attendances = activeCooks.map { cook ->
                            val (isPresent, reason) = cookAttendanceMap[cook.cookId] ?: Pair(true, "")
                            CookAttendanceEntity(
                                date = selectedDate,
                                cookId = cook.cookId,
                                cookName = cook.name,
                                isPresent = isPresent,
                                absenceReason = reason
                            )
                        }

                        val checkedCustomIds = customFoodItems.filter { customItemUsageMap[it.id] == true }.map { it.id }
                        val customItemsJsonString = com.example.data.model.CustomFoodItemParser.usedItemIdsToJson(checkedCustomIds)

                        val currentSchoolType = school?.schoolType ?: "PRIMARY"
                        val riceNormG = if (currentSchoolType == "MIDDLE" || currentSchoolType == "UPPER_PRIMARY") {
                            configNorms?.upperPrimaryRiceNormGrams ?: 150.0
                        } else {
                            configNorms?.primaryRiceNormGrams ?: 150.0
                        }
                        val computedRiceKg = if (mealServed) (totalServedCount * riceNormG) / 1000.0 else 0.0

                        val record = DailyMealRecordEntity(
                            date = selectedDate,
                            enrolledStudents = totalEnrolled,
                            enrolledBoys = enrolledBoys,
                            enrolledGirls = enrolledGirls,
                            boysPresent = parsedBoysPresent,
                            girlsPresent = parsedGirlsPresent,
                            studentsPresent = totalPresentCount,
                            mealServed = mealServed,
                            boysServed = parsedBoysServed,
                            girlsServed = parsedGirlsServed,
                            studentsServed = totalServedCount,
                            menuDetails = menuDetails,
                            cooksPresentCount = attendances.count { it.isPresent },
                            totalCooksCount = activeCooks.size,
                            tastingDone = tastingDone,
                            tastedBy = tastedBy,
                            tasteQuality = tasteQuality,
                            hygieneChecked = hygieneChecked,
                            photoUri = mealPhotoUri,
                            riceConsumedKg = computedRiceKg,
                            customItemsUsedJson = customItemsJsonString,
                            syncStatus = "SYNCED"
                        )

                        viewModel.saveDailyMeal(record, attendances) {
                            showSuccessSnackbar = true
                            ReminderManager.cancelTodayNotification(context)
                        }
                    },
                    colors = poshanButtonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHi) "दैनिक रिपोर्ट सुरक्षित करें (${selectedDate.toDisplayDate()})" else "Submit Daily Report (${selectedDate.toDisplayDate()})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (existingMealRecord != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = {
                            showDailyShareDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF25D366).copy(alpha = 0.15f),
                            contentColor = Color(0xFF15803D)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share to SHG WhatsApp",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi) "स्व-सहायता समूह (SHG अध्यक्ष) को व्हाट्सएप भेजें" else "Send to SHG President (WhatsApp)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            if (showSuccessSnackbar) {
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar(
                        if (isHi) "${selectedDate.toDisplayDate()} की दैनिक मध्यान्ह भोजन रिपोर्ट सफलता से सुरक्षित की गई (स्टॉक अपडेटेड)"
                        else "Daily meal report for ${selectedDate.toDisplayDate()} saved successfully (Stock updated)"
                    )
                    showSuccessSnackbar = false
                }
            }

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
    }

    // INTERACTIVE DATE PICKER DIALOG FOR PREVIOUS DATES
    if (showDatePickerDialog) {
        DatePickerModal(
            currentDateStr = selectedDate,
            isHi = isHi,
            onDismiss = { showDatePickerDialog = false },
            onDateSelected = { newDate ->
                viewModel.setSelectedDate(newDate)
                showDatePickerDialog = false
            }
        )
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
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
                    Text(if (isHi) "रद्द करें (Cancel)" else "Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // FULL SCREEN PHOTO PREVIEW DIALOG
    if (showFullPhotoPreview && previewingPhotoUri.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullPhotoPreview = false }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
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
                            text = if (isHi) "📸 मध्यान्ह भोजन फोटो प्रमाण" else "📸 Mid-Day Meal Photo Evidence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        IconButton(onClick = { showFullPhotoPreview = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                    ) {
                        AsyncImage(
                            model = previewingPhotoUri,
                            contentDescription = "Full Photo Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showFullPhotoPreview = false },
                        colors = poshanButtonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isHi) "बंद करें (Close)" else "Close",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // SAMPLE PHOTO PICKER DIALOG (FOR EASY SELECTION)
    if (showSamplePhotoPicker) {
        val samplePhotos = listOf(
            Pair(
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80",
                if (isHi) "दाल, चावल एवं सब्जी (Served Meal)" else "Dal, Rice & Veg Curry"
            ),
            Pair(
                "https://images.unsplash.com/photo-1574484284002-952d92456975?w=800&q=80",
                if (isHi) "छात्र भोजन वितरण (Meal Distribution)" else "Students Dining"
            ),
            Pair(
                "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800&q=80",
                if (isHi) "रसोई स्वच्छता एवं बर्तन (Clean Kitchen & Utensils)" else "Clean Kitchen Setup"
            )
        )

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSamplePhotoPicker = false }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHi) "सैंपल फोटो चुनें" else "Choose Sample Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        IconButton(onClick = { showSamplePhotoPicker = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    samplePhotos.forEach { (url, label) ->
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    mealPhotoUri = url
                                    showSamplePhotoPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = label,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isHi) "चयन करने हेतु टैप करें" else "Tap to select",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = BluePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DAILY ATTENDANCE WHATSAPP DISPATCH DIALOG (TO SHG PRESIDENT)
    if (showDailyShareDialog && existingMealRecord != null) {
        val record = existingMealRecord!!
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

        val schoolType = school?.schoolType ?: "PRIMARY"
        val ricePerStudentGrams = if (schoolType == "MIDDLE" || schoolType == "UPPER_PRIMARY") {
            configNorms?.upperPrimaryRiceNormGrams ?: 150.0
        } else {
            configNorms?.primaryRiceNormGrams ?: 150.0
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
        val cookingCostRate = configNorms?.cookingCostRate ?: 10.17
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
                                                    Text("${String.format(Locale.US, "%.2f", dailyRiceKg)} kg", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
                                                    Text("${String.format(Locale.US, "%.2f", dailyPulseKg)} kg", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
                                                    Text("${String.format(Locale.US, "%.2f", dailyVegKg)} kg", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
                                                    val qtyStr = if (totalItemQty == totalItemQty.toLong().toDouble()) {
                                                        totalItemQty.toLong().toString()
                                                    } else {
                                                        String.format(Locale.US, "%.2f", totalItemQty).trimEnd('0').trimEnd('.')
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
                        // 1-Click Direct Send Card
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
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
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
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    currentDateStr: String,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialMillis = remember(currentDateStr) {
        try {
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdfUtc.parse(currentDateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayedMonthMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = selectedMillis
                        }
                        val yyyy = utcCal.get(Calendar.YEAR)
                        val mm = utcCal.get(Calendar.MONTH) + 1
                        val dd = utcCal.get(Calendar.DAY_OF_MONTH)
                        val formatted = String.format(Locale.US, "%04d-%02d-%02d", yyyy, mm, dd)
                        onDateSelected(formatted)
                    } else {
                        onDismiss()
                    }
                },
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Text(
                    text = if (isHi) "दिनांक चुनें (OK)" else "Select Date (OK)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isHi) "रद्द करें" else "Cancel",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = Color.White
        )
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = if (isHi) "कैलेंडर से दिनांक चुनें" else "Select Date from Calendar",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            },
            headline = {
                val selectedMillis = datePickerState.selectedDateMillis
                val headlineText = if (selectedMillis != null) {
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = selectedMillis
                    }
                    val sdf = SimpleDateFormat("dd MMM yyyy (EEEE)", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    sdf.format(utcCal.time)
                } else {
                    if (isHi) "दिनांक का चयन करें" else "Choose a date"
                }
                Text(
                    text = headlineText,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            showModeToggle = true,
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                titleContentColor = BluePrimary,
                headlineContentColor = Color(0xFF0F172A),
                weekdayContentColor = Color(0xFF334155),
                subheadContentColor = Color(0xFF0F172A),
                navigationContentColor = Color(0xFF0F172A),
                yearContentColor = Color(0xFF0F172A),
                disabledYearContentColor = Color(0xFF94A3B8),
                currentYearContentColor = BluePrimary,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = BluePrimary,
                dayContentColor = Color(0xFF0F172A),
                disabledDayContentColor = Color(0xFFCBD5E1),
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = BluePrimary,
                todayDateBorderColor = BluePrimary,
                todayContentColor = BluePrimary,
                dividerColor = Color(0xFFE2E8F0)
            )
        )
    }
}
