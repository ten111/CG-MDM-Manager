package com.example.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MonthlyTeacherEntity
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.MonthSelectorRow
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.common.formatMonthFullName
import com.example.presentation.common.swipeToNavigateMonth
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CardBorderColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyTeacherDataScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val savedTeachers by viewModel.currentMonthTeachers.collectAsState()
    val allTeachers by viewModel.allTeachers.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCopyDialog by remember { mutableStateOf(false) }

    // Form states for all 4 categories (ST, SC, OBC, General)
    var stTrainedM by remember { mutableStateOf("1") }
    var stTrainedF by remember { mutableStateOf("1") }
    var stUntrainedM by remember { mutableStateOf("0") }
    var stUntrainedF by remember { mutableStateOf("0") }

    var scTrainedM by remember { mutableStateOf("2") }
    var scTrainedF by remember { mutableStateOf("0") }
    var scUntrainedM by remember { mutableStateOf("0") }
    var scUntrainedF by remember { mutableStateOf("0") }

    var obcTrainedM by remember { mutableStateOf("1") }
    var obcTrainedF by remember { mutableStateOf("0") }
    var obcUntrainedM by remember { mutableStateOf("0") }
    var obcUntrainedF by remember { mutableStateOf("0") }

    var genTrainedM by remember { mutableStateOf("0") }
    var genTrainedF by remember { mutableStateOf("0") }
    var genUntrainedM by remember { mutableStateOf("0") }
    var genUntrainedF by remember { mutableStateOf("0") }

    var isSavedState by remember { mutableStateOf(false) }

    fun populateFromTeacherEntity(t: MonthlyTeacherEntity) {
        stTrainedM = t.stTrainedMale.toString()
        stTrainedF = t.stTrainedFemale.toString()
        stUntrainedM = t.stUntrainedMale.toString()
        stUntrainedF = t.stUntrainedFemale.toString()

        scTrainedM = t.scTrainedMale.toString()
        scTrainedF = t.scTrainedFemale.toString()
        scUntrainedM = t.scUntrainedMale.toString()
        scUntrainedF = t.scUntrainedFemale.toString()

        obcTrainedM = t.obcTrainedMale.toString()
        obcTrainedF = t.obcTrainedFemale.toString()
        obcUntrainedM = t.obcUntrainedMale.toString()
        obcUntrainedF = t.obcUntrainedFemale.toString()

        genTrainedM = t.genTrainedMale.toString()
        genTrainedF = t.genTrainedFemale.toString()
        genUntrainedM = t.genUntrainedMale.toString()
        genUntrainedF = t.genUntrainedFemale.toString()
    }

    // Synchronize form when month or saved entity in DB changes
    LaunchedEffect(savedTeachers, selectedMonth) {
        if (savedTeachers != null) {
            populateFromTeacherEntity(savedTeachers!!)
            isSavedState = true
        } else {
            // Default blank state for unrecorded months
            stTrainedM = "0"
            stTrainedF = "0"
            stUntrainedM = "0"
            stUntrainedF = "0"

            scTrainedM = "0"
            scTrainedF = "0"
            scUntrainedM = "0"
            scUntrainedF = "0"

            obcTrainedM = "0"
            obcTrainedF = "0"
            obcUntrainedM = "0"
            obcUntrainedF = "0"

            genTrainedM = "0"
            genTrainedF = "0"
            genUntrainedM = "0"
            genUntrainedF = "0"

            isSavedState = false
        }
    }

    // Dynamic numeric calculations
    val stTM = stTrainedM.toIntOrNull() ?: 0
    val stTF = stTrainedF.toIntOrNull() ?: 0
    val stUM = stUntrainedM.toIntOrNull() ?: 0
    val stUF = stUntrainedF.toIntOrNull() ?: 0
    val stTotal = stTM + stTF + stUM + stUF

    val scTM = scTrainedM.toIntOrNull() ?: 0
    val scTF = scTrainedF.toIntOrNull() ?: 0
    val scUM = scUntrainedM.toIntOrNull() ?: 0
    val scUF = scUntrainedF.toIntOrNull() ?: 0
    val scTotal = scTM + scTF + scUM + scUF

    val obcTM = obcTrainedM.toIntOrNull() ?: 0
    val obcTF = obcTrainedF.toIntOrNull() ?: 0
    val obcUM = obcUntrainedM.toIntOrNull() ?: 0
    val obcUF = obcUntrainedF.toIntOrNull() ?: 0
    val obcTotal = obcTM + obcTF + obcUM + obcUF

    val genTM = genTrainedM.toIntOrNull() ?: 0
    val genTF = genTrainedF.toIntOrNull() ?: 0
    val genUM = genUntrainedM.toIntOrNull() ?: 0
    val genUF = genUntrainedF.toIntOrNull() ?: 0
    val genTotal = genTM + genTF + genUM + genUF

    val totalTrained = stTM + stTF + scTM + scTF + obcTM + obcTF + genTM + genTF
    val totalUntrained = stUM + stUF + scUM + scUF + obcUM + obcUF + genUM + genUF
    val grandTotal = totalTrained + totalUntrained
    val totalMale = stTM + stUM + scTM + scUM + obcTM + obcUM + genTM + genUM
    val totalFemale = stTF + stUF + scTF + scUF + obcTF + obcUF + genTF + genUF

    val prevMonthStr = getPreviousMonth(selectedMonth)
    val prevTeacher = remember(allTeachers, prevMonthStr) {
        allTeachers.firstOrNull { it.monthYear == prevMonthStr }
    }

    // Month Navigation helper
    fun shiftMonth(delta: Int) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(selectedMonth) ?: Date()
            cal.add(Calendar.MONTH, delta)
            viewModel.setSelectedMonth(sdf.format(cal.time))
        } catch (_: Exception) {}
    }

    val displayMonth = formatMonthFullName(selectedMonth, isHi)
    val displayPrevMonth = formatMonthFullName(prevMonthStr, isHi)

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "मासिक शिक्षक डेटा" else "Monthly Teacher Data",
                subtitle = if (isHi) "शिक्षक विवरण (प्रशिक्षित / अप्रशिक्षित)" else "Teacher Details (Trained / Untrained)",
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() },
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            val entity = MonthlyTeacherEntity(
                                monthYear = selectedMonth,
                                schoolId = "SCH-CG-RPR-001",
                                academicYear = "2026-27",
                                totalTeachers = grandTotal,
                                maleCount = totalMale,
                                femaleCount = totalFemale,
                                trainedCount = totalTrained,
                                untrainedCount = totalUntrained,
                                stCount = stTotal,
                                scCount = scTotal,
                                obcCount = obcTotal,
                                generalCount = genTotal,
                                stTrainedMale = stTM,
                                stTrainedFemale = stTF,
                                stUntrainedMale = stUM,
                                stUntrainedFemale = stUF,
                                scTrainedMale = scTM,
                                scTrainedFemale = scTF,
                                scUntrainedMale = scUM,
                                scUntrainedFemale = scUF,
                                obcTrainedMale = obcTM,
                                obcTrainedFemale = obcTF,
                                obcUntrainedMale = obcUM,
                                obcUntrainedFemale = obcUF,
                                genTrainedMale = genTM,
                                genTrainedFemale = genTF,
                                genUntrainedMale = genUM,
                                genUntrainedFemale = genUF,
                                isLocked = false,
                                createdBy = "Head Teacher",
                                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            )
                            viewModel.saveTeacherSummary(entity)
                            isSavedState = true
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = if (isHi)
                                        "$displayMonth का शिक्षक डेटा सुरक्षित कर लिया गया है।"
                                    else
                                        "Teacher data for $displayMonth saved successfully."
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi) "$displayMonth के लिए सुरक्षित करें" else "Save for $displayMonth",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .swipeToNavigateMonth(
                    selectedMonth = selectedMonth,
                    onMonthSelected = { viewModel.setSelectedMonth(it) }
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ACADEMIC YEAR & MONTH SELECTOR
            MonthSelectorRow(
                selectedMonth = selectedMonth,
                onMonthSelected = { viewModel.setSelectedMonth(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // MONTH SELECTOR AND STATUS PILL
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { shiftMonth(-1) }) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = BluePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayMonth,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isSavedState) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHi) "सुरक्षित (Saved)" else "Saved",
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "!",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFD97706)),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHi) "सुरक्षित नहीं (Not saved)" else "Not saved",
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = { shiftMonth(1) }) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = BluePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // COPY FROM PREVIOUS MONTH CALLOUT / BANNER
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHi) "पिछले माह से शिक्षक डेटा कॉपी करें" else "Copy Teachers from Previous Month",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = if (isHi)
                                    "$displayPrevMonth अथवा पूर्व माह का डेटा लें"
                                else
                                    "Replicate teacher census from $displayPrevMonth",
                                fontSize = 11.sp,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            if (prevTeacher != null) {
                                populateFromTeacherEntity(prevTeacher)
                                isSavedState = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isHi)
                                            "$displayPrevMonth का शिक्षक डेटा कॉपी किया गया। कृपया जांचकर सुरक्षित करें।"
                                        else
                                            "Teacher records copied from $displayPrevMonth! Review and click Save."
                                    )
                                }
                            } else {
                                showCopyDialog = true
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = BluePrimary)
                    ) {
                        Text(
                            text = if (isHi) "कॉपी करें" else "Copy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // SUMMARY KPI CARD (Trained, Untrained, Total)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$totalTrained",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isHi) "प्रशिक्षित (Trained)" else "Trained",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$totalUntrained",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isHi) "अप्रशिक्षित (Untrained)" else "Untrained",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$grandTotal",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isHi) "कुल शिक्षक (Total)" else "Total",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // 1. ST CATEGORY CARD
            CategoryTeacherCard(
                categoryName = if (isHi) "अनुसूचित जनजाति (ST)" else "ST",
                total = stTotal,
                isHi = isHi,
                trainedM = stTrainedM,
                trainedF = stTrainedF,
                untrainedM = stUntrainedM,
                untrainedF = stUntrainedF,
                onTrainedMChange = { stTrainedM = it; isSavedState = false },
                onTrainedFChange = { stTrainedF = it; isSavedState = false },
                onUntrainedMChange = { stUntrainedM = it; isSavedState = false },
                onUntrainedFChange = { stUntrainedF = it; isSavedState = false }
            )

            // 2. SC CATEGORY CARD
            CategoryTeacherCard(
                categoryName = if (isHi) "अनुसूचित जाति (SC)" else "SC",
                total = scTotal,
                isHi = isHi,
                trainedM = scTrainedM,
                trainedF = scTrainedF,
                untrainedM = scUntrainedM,
                untrainedF = scUntrainedF,
                onTrainedMChange = { scTrainedM = it; isSavedState = false },
                onTrainedFChange = { scTrainedF = it; isSavedState = false },
                onUntrainedMChange = { scUntrainedM = it; isSavedState = false },
                onUntrainedFChange = { scUntrainedF = it; isSavedState = false }
            )

            // 3. OBC CATEGORY CARD
            CategoryTeacherCard(
                categoryName = if (isHi) "अन्य पिछड़ा वर्ग (OBC)" else "OBC",
                total = obcTotal,
                isHi = isHi,
                trainedM = obcTrainedM,
                trainedF = obcTrainedF,
                untrainedM = obcUntrainedM,
                untrainedF = obcUntrainedF,
                onTrainedMChange = { obcTrainedM = it; isSavedState = false },
                onTrainedFChange = { obcTrainedF = it; isSavedState = false },
                onUntrainedMChange = { obcUntrainedM = it; isSavedState = false },
                onUntrainedFChange = { obcUntrainedF = it; isSavedState = false }
            )

            // 4. GENERAL CATEGORY CARD
            CategoryTeacherCard(
                categoryName = if (isHi) "सामान्य वर्ग (General)" else "General",
                total = genTotal,
                isHi = isHi,
                trainedM = genTrainedM,
                trainedF = genTrainedF,
                untrainedM = genUntrainedM,
                untrainedF = genUntrainedF,
                onTrainedMChange = { genTrainedM = it; isSavedState = false },
                onTrainedFChange = { genTrainedF = it; isSavedState = false },
                onUntrainedMChange = { genUntrainedM = it; isSavedState = false },
                onUntrainedFChange = { genUntrainedF = it; isSavedState = false }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // COPY FROM MONTH SELECTION DIALOG
    if (showCopyDialog) {
        val availableMonths = allTeachers.filter { it.monthYear != selectedMonth }
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (isHi) "माह से शिक्षक डेटा कॉपी करें" else "Copy Teacher Data from Month",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHi)
                            "जिस माह से शिक्षक डेटा $displayMonth में कॉपी करना चाहते हैं, उसका चयन करें:"
                        else
                            "Select the recorded month to copy into $displayMonth:",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )

                    if (availableMonths.isEmpty()) {
                        Text(
                            text = if (isHi)
                                "कॉपी करने हेतु अन्य कोई रिकॉर्ड उपलब्ध नहीं है।"
                            else
                                "No other recorded months available to copy from.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        availableMonths.forEach { item ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        populateFromTeacherEntity(item)
                                        isSavedState = false
                                        showCopyDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isHi)
                                                    "${formatMonthLabel(item.monthYear, isHi)} का शिक्षक डेटा कॉपी हो गया। कृपया सुरक्षित करें।"
                                                else
                                                    "Teacher data copied from ${formatMonthLabel(item.monthYear, isHi)}! Please click Save."
                                            )
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = formatMonthLabel(item.monthYear, isHi),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (isHi)
                                                "कुल शिक्षक: ${item.totalTeachers} (प्रशिक्षित: ${item.trainedCount}, अप्रशिक्षित: ${item.untrainedCount})"
                                            else
                                                "Total: ${item.totalTeachers} (Trained: ${item.trainedCount}, Untrained: ${item.untrainedCount})",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCopyDialog = false }) {
                    Text(if (isHi) "बंद करें" else "Close", fontWeight = FontWeight.Bold, color = BluePrimary)
                }
            }
        )
    }
}

@Composable
private fun CategoryTeacherCard(
    categoryName: String,
    total: Int,
    isHi: Boolean,
    trainedM: String,
    trainedF: String,
    untrainedM: String,
    untrainedF: String,
    onTrainedMChange: (String) -> Unit,
    onTrainedFChange: (String) -> Unit,
    onUntrainedMChange: (String) -> Unit,
    onUntrainedFChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Title & Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = if (isHi) "कुल: $total" else "Total: $total",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Trained
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHi) "प्रशिक्षित" else "Trained",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF334155),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (isHi) "पुरुष" else "Male",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.padding(end = 5.dp)
                )
                CompactNumberInput(
                    value = trainedM,
                    onValueChange = onTrainedMChange
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (isHi) "महिला" else "Female",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFE11D48),
                    modifier = Modifier.padding(end = 5.dp)
                )
                CompactNumberInput(
                    value = trainedF,
                    onValueChange = onTrainedFChange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Untrained
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHi) "अप्रशिक्षित" else "Untrained",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF334155),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (isHi) "पुरुष" else "Male",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.padding(end = 5.dp)
                )
                CompactNumberInput(
                    value = untrainedM,
                    onValueChange = onUntrainedMChange
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (isHi) "महिला" else "Female",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFE11D48),
                    modifier = Modifier.padding(end = 5.dp)
                )
                CompactNumberInput(
                    value = untrainedF,
                    onValueChange = onUntrainedFChange
                )
            }
        }
    }
}

@Composable
private fun CompactNumberInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }.take(3)
                onValueChange(if (filtered.isEmpty()) "0" else filtered.trimStart('0').ifEmpty { "0" })
            },
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF16A34A)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun getPreviousMonth(monthYear: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(monthYear) ?: Date()
        cal.add(Calendar.MONTH, -1)
        sdf.format(cal.time)
    } catch (_: Exception) {
        monthYear
    }
}

private fun formatMonthLabel(monthYear: String, isHi: Boolean): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(monthYear) ?: Date()
        val outSdf = SimpleDateFormat("MMM yyyy", if (isHi) Locale("hi") else Locale.ENGLISH)
        outSdf.format(date)
    } catch (_: Exception) {
        monthYear
    }
}
