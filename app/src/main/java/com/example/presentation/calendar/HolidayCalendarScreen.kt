package com.example.presentation.calendar

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CalendarEventEntity
import com.example.data.repository.GovtHolidaysMaster
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.viewmodel.PoshanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayCalendarScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val events by viewModel.allCalendarEvents.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    var selectedYear by remember { mutableIntStateOf(2026) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var showVacationDialog by remember { mutableStateOf(false) }
    var showForceSyncDialog by remember { mutableStateOf(false) }
    var showCsvTemplateDialog by remember { mutableStateOf(false) }
    var showCsvUploadDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<CalendarEventEntity?>(null) }

    // CSV Template Sample Data
    val sampleCsv = remember(selectedYear) {
        """
        Date,Holiday_Name,Holiday_Type,Order_No,Authority,Remarks
        $selectedYear-01-26,Republic Day (गणतंत्र दिवस),NATIONAL,CG-ED-01,Central Govt,National Festival - PM POSHAN mandatory
        $selectedYear-03-04,Holi Festival (होली),NATIONAL,CG-GA-04,Govt of Chhattisgarh,State Gazetted
        $selectedYear-08-15,Independence Day (स्वतंत्रता दिवस),NATIONAL,CG-ED-78,Central Govt,National Festival - Flag hoisting
        $selectedYear-08-27,Hareli Festival (हरेली तिहार),STATE,CG-GA-14,Govt of Chhattisgarh,Traditional CG Festival
        $selectedYear-09-04,Teeja Festival (तीजा),STATE,CG-GA-41,Govt of Chhattisgarh,State Holiday
        $selectedYear-10-02,Mahatma Gandhi Jayanti (गांधी जयंती),NATIONAL,CG-GA-50,Central Govt,National Holiday
        $selectedYear-10-20,Dussehra (दशहरा),NATIONAL,CG-GA-52,Govt of Chhattisgarh,Festive Holiday
        $selectedYear-11-01,CG Foundation Day (छत्तीसगढ़ स्थापना दिवस),STATE,CG-GA-55,Govt of Chhattisgarh,State Gazetted
        $selectedYear-11-08,Diwali Festival (दीपावली),NATIONAL,CG-GA-60,Govt of Chhattisgarh,Festival of Lights
        """.trimIndent()
    }

    // CSV Download Launcher (Saves real .csv file to device storage)
    val downloadCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(sampleCsv.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(
                    context,
                    if (isHi) "✅ CSV टेम्पलेट सफलतापूर्वक डाउनलोड हो गया" else "✅ CSV Template downloaded successfully",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // CSV File Picker Launcher (Uploads and imports .csv file from device)
    val uploadCsvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    viewModel.importHolidaysFromCsv(content) { count ->
                        Toast.makeText(
                            context,
                            if (isHi) "✅ $count अवकाश CSV से सफलतापूर्वक आयात किए गए" else "✅ Successfully imported $count holidays from CSV",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    showCsvUploadDialog = false
                } else {
                    Toast.makeText(context, if (isHi) "चयनित फ़ाइल खाली है" else "Selected file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading CSV file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Filter events by selected year
    val yearEvents = remember(events, selectedYear) {
        val prefix = selectedYear.toString()
        events.filter { it.eventDate.startsWith(prefix) }
    }

    val totalYearCount = yearEvents.size
    val nationalCount = remember(yearEvents) { yearEvents.count { it.eventType == "NATIONAL" } }
    val stateCount = remember(yearEvents) { yearEvents.count { it.eventType == "STATE" } }
    val schoolCount = remember(yearEvents) { yearEvents.count { it.eventType == "SCHOOL" || it.eventType == "SPECIAL_WORKING_DAY" } }
    val vacationCount = remember(yearEvents) { yearEvents.count { it.eventType == "VACATION" } }
    val localCount = remember(yearEvents) { yearEvents.count { it.eventType == "LOCAL" } }
    val emergencyCount = remember(yearEvents) { yearEvents.count { it.eventType == "EMERGENCY" } }

    val filteredEvents = remember(yearEvents, selectedFilter) {
        when (selectedFilter) {
            "NATIONAL" -> yearEvents.filter { it.eventType == "NATIONAL" }
            "STATE" -> yearEvents.filter { it.eventType == "STATE" }
            "SCHOOL" -> yearEvents.filter { it.eventType == "SCHOOL" || it.eventType == "SPECIAL_WORKING_DAY" }
            "VACATION" -> yearEvents.filter { it.eventType == "VACATION" }
            "LOCAL" -> yearEvents.filter { it.eventType == "LOCAL" }
            "EMERGENCY" -> yearEvents.filter { it.eventType == "EMERGENCY" }
            else -> yearEvents
        }
    }

    // Group filtered events by Month
    val groupedByMonth = remember(filteredEvents) {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfMonth = SimpleDateFormat("MMMM", Locale.US)

        val map = linkedMapOf<String, MutableList<CalendarEventEntity>>()
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        months.forEach { map[it] = mutableListOf() }

        filteredEvents.forEach { evt ->
            try {
                val date = sdfInput.parse(evt.eventDate)
                if (date != null) {
                    val monthName = sdfMonth.format(date)
                    map.getOrPut(monthName) { mutableListOf() }.add(evt)
                }
            } catch (_: Exception) {
                map.getOrPut("Other") { mutableListOf() }.add(evt)
            }
        }
        map.filter { it.value.isNotEmpty() }
    }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "अवकाश एवं शैक्षणिक कैलेंडर" else "Holiday & Academic Calendar",
                subtitle = if (isHi) "सत्र $selectedYear | स्कूल अवकाश मास्टर" else "Year $selectedYear | School Holiday Master",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("holiday_calendar_screen"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. TOP HEADER: Year Navigation & + Add Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { selectedYear -= 1 },
                            modifier = Modifier.size(36.dp).testTag("btn_prev_year")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Previous Year",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = selectedYear.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("text_current_year")
                        )

                        IconButton(
                            onClick = { selectedYear += 1 },
                            modifier = Modifier.size(36.dp).testTag("btn_next_year")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Next Year",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // + Add Green Button
                    Button(
                        onClick = {
                            editingEvent = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_add_holiday")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "+ जोड़ें" else "+ Add",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // 2. ACTION BUTTONS & FILTER PILLS
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // "All Official" green button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedFilter == "ALL") Color(0xFFE8F5E9) else Color.White)
                            .border(
                                BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedFilter = "ALL" }
                            .padding(vertical = 14.dp)
                            .testTag("btn_all_official"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHi) "✓ सभी शासकीय अवकाश ($totalYearCount)" else "✓ All Official Holidays ($totalYearCount)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2E7D32)
                                )
                            )
                        }
                    }

                    // "☀️ Summer Vacation / Long Breaks" amber button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF8E1))
                            .border(
                                BorderStroke(1.5.dp, Color(0xFFFFB300)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showVacationDialog = true }
                            .padding(vertical = 14.dp)
                            .testTag("btn_summer_vacation"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "☀️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHi) "ग्रीष्मकालीन अवकाश / लंबी छुट्टियां (कस्टम तिथियां)" else "Summer Vacation / Long Breaks (Custom Dates)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE65100)
                                )
                            )
                        }
                    }

                    // Filter Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChipCard(
                                title = if (isHi) "राष्ट्रीय ($nationalCount)" else "National ($nationalCount)",
                                icon = "🚩",
                                isSelected = selectedFilter == "NATIONAL",
                                borderColor = Color(0xFF4CAF50),
                                textColor = Color(0xFF2E7D32),
                                selectedBg = Color(0xFFE8F5E9),
                                onClick = { selectedFilter = if (selectedFilter == "NATIONAL") "ALL" else "NATIONAL" }
                            )
                        }
                        item {
                            FilterChipCard(
                                title = if (isHi) "राज्य ($stateCount)" else "State ($stateCount)",
                                icon = "📍",
                                isSelected = selectedFilter == "STATE",
                                borderColor = Color(0xFF2196F3),
                                textColor = Color(0xFF1565C0),
                                selectedBg = Color(0xFFE3F2FD),
                                onClick = { selectedFilter = if (selectedFilter == "STATE") "ALL" else "STATE" }
                            )
                        }
                        item {
                            FilterChipCard(
                                title = if (isHi) "विभाग/स्कूल ($schoolCount)" else "School ($schoolCount)",
                                icon = "🏫",
                                isSelected = selectedFilter == "SCHOOL",
                                borderColor = Color(0xFF9C27B0),
                                textColor = Color(0xFF6A1B9A),
                                selectedBg = Color(0xFFF3E5F5),
                                onClick = { selectedFilter = if (selectedFilter == "SCHOOL") "ALL" else "SCHOOL" }
                            )
                        }
                        item {
                            FilterChipCard(
                                title = if (isHi) "दशहरा/दीवाली अवकाश ($vacationCount)" else "Vacation ($vacationCount)",
                                icon = "🏖️",
                                isSelected = selectedFilter == "VACATION",
                                borderColor = Color(0xFFFF9800),
                                textColor = Color(0xFFE65100),
                                selectedBg = Color(0xFFFFF3E0),
                                onClick = { selectedFilter = if (selectedFilter == "VACATION") "ALL" else "VACATION" }
                            )
                        }
                        item {
                            FilterChipCard(
                                title = if (isHi) "स्थानीय/जिला ($localCount)" else "District ($localCount)",
                                icon = "🏛️",
                                isSelected = selectedFilter == "LOCAL",
                                borderColor = Color(0xFF009688),
                                textColor = Color(0xFF004D40),
                                selectedBg = Color(0xFFE0F2F1),
                                onClick = { selectedFilter = if (selectedFilter == "LOCAL") "ALL" else "LOCAL" }
                            )
                        }
                        item {
                            FilterChipCard(
                                title = if (isHi) "बाढ़/बारिश/हड़ताल ($emergencyCount)" else "Emergency ($emergencyCount)",
                                icon = "⚡",
                                isSelected = selectedFilter == "EMERGENCY",
                                borderColor = Color(0xFFF44336),
                                textColor = Color(0xFFC62828),
                                selectedBg = Color(0xFFFFEBEE),
                                onClick = { selectedFilter = if (selectedFilter == "EMERGENCY") "ALL" else "EMERGENCY" }
                            )
                        }
                    }

                    // CSV Template & Upload action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCsvTemplateDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_download_template"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            ),
                            border = BorderStroke(1.2.dp, Color(0xFF94A3B8)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = Color(0xFF0D5CD6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHi) "टेम्पलेट डाउनलोड" else "Download Template",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = { showCsvUploadDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_upload_csv"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            ),
                            border = BorderStroke(1.2.dp, Color(0xFF94A3B8)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFF0D5CD6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHi) "CSV अपलोड" else "Upload CSV",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            )
                        }
                    }

                    // Developer forced sync button
                    OutlinedButton(
                        onClick = { showForceSyncDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_force_govt_sync"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF1F8E9),
                            contentColor = Color(0xFF2E7D32)
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFF689F38)),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi) "🔄 शासन के आधिकारिक कैलेंडर से अपडेट करें (Forced Update)" else "🔄 Force Sync Official Govt Holidays ($selectedYear)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B5E20)
                            )
                        )
                    }

                    // Info Note: Sundays are weekly holidays
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE0F2FE),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHi) "सूचना: रविवार साप्ताहिक अवकाश हैं (यहाँ केवल विशेष/शासकीय अवकाश सूचीबद्ध हैं)" else "Note: Sundays are weekly holidays (only special & official holidays listed here)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF0369A1),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // 3. MONTHLY GROUPED HOLIDAY LIST
            if (groupedByMonth.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (isHi) "इस श्रेणी में कोई अवकाश नहीं मिला" else "No holidays found for this filter",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Button(
                                onClick = { showForceSyncDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHi) "शासकीय अवकाश सूची लोड करें" else "Load Official Govt List")
                            }
                        }
                    }
                }
            } else {
                groupedByMonth.forEach { (monthName, monthList) ->
                    item {
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                        )
                    }

                    itemsIndexed(
                        items = monthList,
                        key = { index, evt ->
                            if (evt.id != 0L) "evt_${evt.id}_${evt.eventDate}" else "evt_${index}_${evt.eventDate}_${evt.eventName}"
                        }
                    ) { _, holiday ->
                        HolidayCardItem(
                            event = holiday,
                            isHi = isHi,
                            onEdit = {
                                editingEvent = holiday
                                showAddEditDialog = true
                            },
                            onDelete = {
                                eventToDelete = holiday
                            }
                        )
                    }
                }
            }
        }
    }

    // DIALOG 1: ADD / EDIT HOLIDAY
    if (showAddEditDialog) {
        AddEditHolidayDialog(
            initialEvent = editingEvent,
            defaultYear = selectedYear,
            isHi = isHi,
            onDismiss = { showAddEditDialog = false },
            onSave = { savedEvent ->
                viewModel.saveCalendarEvent(savedEvent) {
                    Toast.makeText(context, if (isHi) "अवकाश सफलतापूर्वक सहेजा गया" else "Holiday saved successfully", Toast.LENGTH_SHORT).show()
                }
                showAddEditDialog = false
            }
        )
    }

    // DIALOG 2: SUMMER VACATION & CUSTOM DATES
    if (showVacationDialog) {
        VacationPeriodDialog(
            defaultYear = selectedYear,
            isHi = isHi,
            onDismiss = { showVacationDialog = false },
            onAddVacation = { start, end, name, type, auth, orderNo ->
                viewModel.addVacationPeriod(
                    startDate = start,
                    endDate = end,
                    vacationName = name,
                    vacationType = type,
                    authority = auth,
                    orderNumber = orderNo
                ) { count ->
                    Toast.makeText(context, if (isHi) "$count दिन का अवकाश सफलतापूर्वक जोड़ा गया" else "Added $count vacation days successfully", Toast.LENGTH_SHORT).show()
                }
                showVacationDialog = false
            }
        )
    }

    // DIALOG 3: FORCED UPDATE GOVT MASTER
    if (showForceSyncDialog) {
        AlertDialog(
            onDismissRequest = { showForceSyncDialog = false },
            containerColor = Color.White,
            icon = { Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = if (isHi) "शासकीय अवकाश सूची सिंक करें (Forced Update)" else "Force Sync Official Govt Holidays",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHi)
                            "यह छत्तीसगढ़ शासन एवं भारत सरकार द्वारा जारी आधिकारिक कैलेंडर (संस्करण ${GovtHolidaysMaster.GOVT_HOLIDAY_LIST_VERSION}) के सभी अवकाशों, दशहरा, दीपावली, शीतकालीन व ग्रीष्मकालीन छुट्टियों को आपके डेटाबेस में सिंक कर देगा।"
                        else
                            "This will sync and enforce the official Government of Chhattisgarh & Central Academic Holiday master list (Version ${GovtHolidaysMaster.GOVT_HOLIDAY_LIST_VERSION}) including state gazetted festivals, national days, and long vacations for $selectedYear.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155))
                    )
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isHi) "✓ 27+ आधिकारिक राज्य एवं केंद्रीय छुट्टियां शामिल हैं\n✓ स्वचालित रूप से मध्याह्न भोजन नियम लागू होंगे" else "✓ Includes 27+ official state & national holidays\n✓ PM POSHAN reporting rules preserved",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1B5E20), fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.forceSyncGovtHolidays(selectedYear) { count ->
                            Toast.makeText(context, if (isHi) "$count शासकीय अवकाश सिंक किए गए" else "Synced $count official holidays", Toast.LENGTH_LONG).show()
                        }
                        showForceSyncDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(if (isHi) "अभी सिंक करें (Sync Now)" else "Sync Official List Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForceSyncDialog = false }) { Text(if (isHi) "रद्द करें" else "Cancel") }
            }
        )
    }

    // DIALOG 4: DOWNLOADABLE CSV TEMPLATE (DOWNLOAD, SHARE & COPY)
    if (showCsvTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showCsvTemplateDialog = false },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = null,
                        tint = Color(0xFF0D5CD6)
                    )
                    Text(
                        text = if (isHi) "CSV अवकाश टेम्पलेट" else "CSV Holiday Template",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHi)
                            "आप नीचे दिए गए टेम्पलेट को डाउनलोड कर सकते हैं या कॉपी करके एक्सेल/नोटपैड में अपने अवकाश जोड़ सकते हैं:"
                        else
                            "Download this template to add holidays in Excel/Notepad, then upload it into the app:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                    )

                    // CSV Code Box Preview
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = sampleCsv,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }

                    // Download File Primary Button
                    Button(
                        onClick = {
                            downloadCsvLauncher.launch("holiday_template_${selectedYear}.csv")
                            showCsvTemplateDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5CD6)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHi) "📥 CSV फ़ाइल डाउनलोड करें (.csv)" else "📥 Download CSV File (.csv)")
                    }

                    // Secondary row: Share & Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, sampleCsv)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, if (isHi) "CSV टेम्पलेट शेयर करें" else "Share CSV Template")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHi) "शेयर करें" else "Share", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(sampleCsv))
                                Toast.makeText(context, if (isHi) "टेम्पलेट क्लिपबोर्ड पर कॉपी हो गया" else "Template copied to clipboard", Toast.LENGTH_SHORT).show()
                                showCsvTemplateDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHi) "कॉपी करें" else "Copy", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCsvTemplateDialog = false }) {
                    Text(if (isHi) "बंद करें" else "Close", color = Color(0xFF64748B))
                }
            }
        )
    }

    // DIALOG 5: CSV UPLOAD (FILE PICKER + MANUAL PASTE SYSTEM)
    if (showCsvUploadDialog) {
        var csvInputText by remember { mutableStateOf("") }
        var isManualMode by remember { mutableStateOf(false) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        DisposableEffect(Unit) {
            onDispose {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }

        AlertDialog(
            onDismissRequest = {
                keyboardController?.hide()
                focusManager.clearFocus()
                showCsvUploadDialog = false
            },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        tint = Color(0xFF0D5CD6)
                    )
                    Text(
                        text = if (isHi) "CSV अवकाश अपलोड / आयात" else "Upload & Import Holiday CSV",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Option 1: Direct File Picker from Device Storage
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (isHi) "डिवाइस से CSV फ़ाइल चुनें" else "Choose CSV File from Device",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D)
                                )
                            )
                            Text(
                                text = if (isHi)
                                    "अपने फोन के फ़ाइल मैनेजर / डाउनलोड्स से .csv फ़ाइल चुनें"
                                else
                                    "Select a .csv file from your phone storage / downloads folder",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF166534),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                            Button(
                                onClick = {
                                    uploadCsvFileLauncher.launch(
                                        arrayOf("text/*", "text/comma-separated-values", "text/csv", "text/plain", "*/*")
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHi) "फ़ाइल चुनें एवं अपलोड करें" else "Browse & Upload File")
                            }
                        }
                    }

                    // Divider or Option 2: Manual Text Paste
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = if (isHi) "या टेक्स्ट पेस्ट करें" else "OR Paste CSV Text",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    if (!isManualMode) {
                        OutlinedButton(
                            onClick = { isManualMode = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHi) "CSV डेटा मैन्युअल पेस्ट करें" else "Paste CSV Text Manually")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isHi) "CSV पंक्तियां पेस्ट करें (Date, Name, Type, OrderNo, Authority):" else "Paste CSV rows (Date, Name, Type, OrderNo, Authority):",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                            )
                            OutlinedTextField(
                                value = csvInputText,
                                onValueChange = { csvInputText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                placeholder = {
                                    Text(
                                        "2026-08-15,Independence Day,NATIONAL,CG-78,Govt of CG\n2026-08-27,Hareli Festival,STATE,CG-GA-14,Govt of CG",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 6
                            )
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    if (csvInputText.isNotBlank()) {
                                        viewModel.importHolidaysFromCsv(csvInputText) { count ->
                                            Toast.makeText(
                                                context,
                                                if (isHi) "✅ $count अवकाश सफलतापूर्वक आयात किए गए" else "✅ Successfully imported $count holidays",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        showCsvUploadDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                enabled = csvInputText.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isHi) "पेस्ट किया गया डेटा आयात करें" else "Import Pasted Data")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        showCsvUploadDialog = false
                    }
                ) {
                    Text(if (isHi) "रद्द करें" else "Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // DIALOG 6: DELETE CONFIRMATION
    if (eventToDelete != null) {
        val target = eventToDelete!!
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            containerColor = Color.White,
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp)) },
            title = { Text(if (isHi) "अवकाश हटाएं?" else "Delete Holiday?", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (isHi) "क्या आप निश्चित रूप से '${target.eventName}' (${target.eventDate}) को हटाना चाहते हैं?" else "Are you sure you want to delete '${target.eventName}' on ${target.eventDate}?",
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCalendarEvent(target) {
                            Toast.makeText(context, if (isHi) "अवकाश हटा दिया गया" else "Holiday deleted", Toast.LENGTH_SHORT).show()
                        }
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (isHi) "हटाएं" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) { Text(if (isHi) "रद्द करें" else "Cancel") }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// FILTER CHIP CARD
// ---------------------------------------------------------------------------
@Composable
private fun FilterChipCard(
    title: String,
    icon: String,
    isSelected: Boolean,
    borderColor: Color,
    textColor: Color,
    selectedBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) selectedBg else Color.White)
            .border(
                BorderStroke(1.2.dp, if (isSelected) borderColor else Color(0xFFCBD5E1)),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 14.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) textColor else Color(0xFF334155)
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// HOLIDAY CARD ITEM
// ---------------------------------------------------------------------------
@Composable
private fun HolidayCardItem(
    event: CalendarEventEntity,
    isHi: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(event.eventDate) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfOutput = SimpleDateFormat("EEE, dd MMM, yyyy", Locale.US)
            val d = sdfInput.parse(event.eventDate)
            if (d != null) sdfOutput.format(d) else event.eventDate
        } catch (_: Exception) {
            event.eventDate
        }
    }

    val (badgeText, badgeBgColor, badgeTextColor) = when (event.eventType.uppercase()) {
        "NATIONAL" -> Triple("National", Color(0xFFFF5722), Color.White)
        "STATE" -> Triple("State", Color(0xFF2196F3), Color.White)
        "SCHOOL", "SPECIAL_WORKING_DAY" -> Triple("School", Color(0xFF9C27B0), Color.White)
        "VACATION" -> Triple("Vacation", Color(0xFFFF9800), Color.White)
        "LOCAL" -> Triple("District", Color(0xFF009688), Color.White)
        "EMERGENCY" -> Triple("Emergency", Color(0xFFD32F2F), Color.White)
        else -> Triple(event.eventType, Color(0xFF64748B), Color.White)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("holiday_card_${event.eventDate}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left content: Date, Title, Remarks, Badge
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                )

                if (event.remarks.isNotBlank()) {
                    Text(
                        text = event.remarks,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF475569),
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    )
                }
            }

            // Right content: Edit & Delete action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp).testTag("btn_edit_${event.eventDate}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Holiday",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("btn_delete_${event.eventDate}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Holiday",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DIALOG: ADD / EDIT HOLIDAY (Screenshot 3)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditHolidayDialog(
    initialEvent: CalendarEventEntity?,
    defaultYear: Int,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (CalendarEventEntity) -> Unit
) {
    val isEdit = initialEvent != null
    var eventDate by remember { mutableStateOf(initialEvent?.eventDate ?: "$defaultYear-01-26") }
    var eventName by remember { mutableStateOf(initialEvent?.eventName ?: "") }
    var eventType by remember { mutableStateOf(initialEvent?.eventType ?: "NATIONAL") }
    var authority by remember { mutableStateOf(initialEvent?.authority ?: "Govt of Chhattisgarh") }
    var orderNumber by remember { mutableStateOf(initialEvent?.orderNumber ?: "") }
    var remarks by remember { mutableStateOf(initialEvent?.remarks ?: "") }
    var isMealReportingRequired by remember { mutableStateOf(initialEvent?.isMealReportingRequired ?: false) }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                        eventDate = sdf.format(cal.time)
                    }
                    showDatePicker = false
                }) {
                    Text(if (isHi) "चुनें" else "Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (isHi) "रद्द करें" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEdit) {
                        if (isHi) "अवकाश संपादित करें" else "Edit Holiday"
                    } else {
                        if (isHi) "अवकाश जोड़ें" else "Add Holiday"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Holiday Date * (Dropdown / Picker styling as in Screenshot 3)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isHi) "अवकाश दिनांक *" else "Holiday Date *",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .testTag("input_holiday_date"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = eventDate,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Holiday Name * (e.g., Republic Day) (Screenshot 3)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isHi) "अवकाश का नाम *" else "Holiday Name *",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_holiday_name"),
                        placeholder = { Text("e.g., Republic Day / गणतंत्र दिवस") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // 3. Holiday Type * (Pill selector as in Screenshot 3: National, State, School, Local, Emergency)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isHi) "अवकाश प्रकार *" else "Holiday Type *",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )

                    // Top Row: National, State, School
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypePillButton(
                            title = "National",
                            isSelected = eventType == "NATIONAL",
                            activeColor = Color(0xFFFF5722),
                            modifier = Modifier.weight(1f),
                            onClick = { eventType = "NATIONAL" }
                        )
                        TypePillButton(
                            title = "State",
                            isSelected = eventType == "STATE",
                            activeColor = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f),
                            onClick = { eventType = "STATE" }
                        )
                        TypePillButton(
                            title = "School",
                            isSelected = eventType == "SCHOOL",
                            activeColor = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f),
                            onClick = { eventType = "SCHOOL" }
                        )
                    }

                    // Bottom Row: Local (District), Emergency (Rain/Strike)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypePillButton(
                            title = if (isHi) "जिला/स्थानीय" else "District/Local",
                            isSelected = eventType == "LOCAL",
                            activeColor = Color(0xFF009688),
                            modifier = Modifier.weight(1f),
                            onClick = { eventType = "LOCAL" }
                        )
                        TypePillButton(
                            title = if (isHi) "बाढ़/बारिश/हड़ताल" else "Emergency/Strike",
                            isSelected = eventType == "EMERGENCY",
                            activeColor = Color(0xFFD32F2F),
                            modifier = Modifier.weight(1f),
                            onClick = { eventType = "EMERGENCY" }
                        )
                    }
                }

                // Authority / Order No
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = orderNumber,
                        onValueChange = { orderNumber = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(if (isHi) "आदेश क्रमांक" else "Order No") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = authority,
                        onValueChange = { authority = it },
                        modifier = Modifier.weight(1.2f),
                        label = { Text(if (isHi) "जारीकर्ता" else "Authority") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (eventName.isNotBlank()) {
                        val entity = (initialEvent ?: CalendarEventEntity(
                            eventDate = eventDate,
                            eventName = eventName.trim()
                        )).copy(
                            eventDate = eventDate,
                            eventName = eventName.trim(),
                            eventType = eventType,
                            authority = authority.trim(),
                            orderNumber = orderNumber.trim(),
                            remarks = remarks.trim(),
                            isMealReportingRequired = isMealReportingRequired
                        )
                        onSave(entity)
                    }
                },
                modifier = Modifier.testTag("btn_confirm_add_holiday"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = eventName.isNotBlank()
            ) {
                Text(
                    text = if (isEdit) {
                        if (isHi) "बदलाव सहेजें" else "Save Changes"
                    } else {
                        if (isHi) "अवकाश जोड़ें" else "Add Holiday"
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// TYPE PILL BUTTON (Screenshot 3)
// ---------------------------------------------------------------------------
@Composable
private fun TypePillButton(
    title: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.1f) else Color.Transparent)
            .border(
                BorderStroke(1.5.dp, if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// DIALOG: SUMMER VACATION / SPECIAL BREAK PERIOD (Screenshot 2)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacationPeriodDialog(
    defaultYear: Int,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onAddVacation: (startDate: String, endDate: String, name: String, type: String, authority: String, orderNo: String) -> Unit
) {
    var vacationTypeOption by remember { mutableStateOf("SUMMER") } // SUMMER, DASHEHRA, DIWALI, WINTER, CUSTOM
    var vacationName by remember { mutableStateOf("ग्रीष्मकालीन अवकाश (Summer Vacation)") }
    var startDate by remember { mutableStateOf("$defaultYear-05-01") }
    var endDate by remember { mutableStateOf("$defaultYear-06-15") }
    var authority by remember { mutableStateOf("School Education Dept, Govt of CG") }
    var orderNumber by remember { mutableStateOf("CG/DSE/VAC-$defaultYear/01") }

    // Dynamic day count calculation (Screenshot 2)
    val calculatedDays = remember(startDate, endDate) {
        GovtHolidaysMaster.calculateDaysBetween(startDate, endDate)
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                        startDate = sdf.format(cal.time)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                        endDate = sdf.format(cal.time)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHi) "ग्रीष्मकालीन / लंबी छुट्टियां" else "Summer Vacation Dates",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Subtitle info
                Text(
                    text = if (isHi)
                        "☀️ शासन / शिक्षा विभाग के आदेशानुसार ग्रीष्मकालीन, दशहरा, दीपावली या शीतकालीन अवकाश अवधि दर्ज करें"
                    else
                        "☀️ Customize vacation period based on government heat wave or seasonal orders",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1565C0))
                )

                // Quick preset pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetPill(
                        label = "☀️ Summer",
                        isSelected = vacationTypeOption == "SUMMER",
                        onClick = {
                            vacationTypeOption = "SUMMER"
                            vacationName = "ग्रीष्मकालीन अवकाश (Summer Vacation)"
                            startDate = "$defaultYear-05-01"
                            endDate = "$defaultYear-06-15"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetPill(
                        label = "🏹 Dussehra",
                        isSelected = vacationTypeOption == "DASHEHRA",
                        onClick = {
                            vacationTypeOption = "DASHEHRA"
                            vacationName = "दशहरा अवकाश (Dussehra Break)"
                            startDate = "$defaultYear-10-18"
                            endDate = "$defaultYear-10-23"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetPill(
                        label = "🪔 Diwali",
                        isSelected = vacationTypeOption == "DIWALI",
                        onClick = {
                            vacationTypeOption = "DIWALI"
                            vacationName = "दीपावली अवकाश (Diwali Break)"
                            startDate = "$defaultYear-11-07"
                            endDate = "$defaultYear-11-12"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetPill(
                        label = "❄️ Winter",
                        isSelected = vacationTypeOption == "WINTER",
                        onClick = {
                            vacationTypeOption = "WINTER"
                            vacationName = "शीतकालीन अवकाश (Winter Break)"
                            startDate = "$defaultYear-12-24"
                            endDate = "$defaultYear-12-31"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Vacation Title
                OutlinedTextField(
                    value = vacationName,
                    onValueChange = { vacationName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isHi) "अवकाश का नाम" else "Vacation Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Start Date * (Screenshot 2)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isHi) "प्रारंभ दिनांक *" else "Start Date *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = startDate, style = MaterialTheme.typography.bodyMedium)
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }

                // End Date * (Screenshot 2)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isHi) "अंतिम दिनांक *" else "End Date *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable { showEndDatePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = endDate, style = MaterialTheme.typography.bodyMedium)
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }

                // Amber Info Banner: "Will add X summer vacation days" (Screenshot 2)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi)
                                "कुल $calculatedDays दिन का अवकाश जोड़ा जाएगा"
                            else
                                "Will add $calculatedDays vacation days",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (vacationName.isNotBlank() && calculatedDays > 0) {
                        onAddVacation(startDate, endDate, vacationName, "VACATION", authority, orderNumber)
                    }
                },
                modifier = Modifier.testTag("btn_confirm_add_vacation"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = vacationName.isNotBlank() && calculatedDays > 0
            ) {
                Text(if (isHi) "अवकाश जोड़ें" else "Add Summer Holidays")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel")
            }
        }
    )
}

@Composable
private fun PresetPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                BorderStroke(1.dp, if (isSelected) Color(0xFF2E7D32) else Color.Transparent),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1
        )
    }
}
