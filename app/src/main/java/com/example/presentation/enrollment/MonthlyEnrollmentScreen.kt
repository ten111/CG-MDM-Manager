package com.example.presentation.enrollment

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
import com.example.data.local.entity.MonthlyEnrollmentEntity
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.MonthSelectorRow
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.common.formatMonthFullName
import com.example.presentation.common.swipeToNavigateMonth
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CardBorderColor
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class ClassInputState(
    var stB: String = "",
    var stG: String = "",
    var scB: String = "",
    var scG: String = "",
    var obcB: String = "",
    var obcG: String = "",
    var genB: String = "",
    var genG: String = "",
    // Inclusive Details (Out of Total Students)
    var minorityB: String = "",
    var minorityG: String = "",
    var cwsnB: String = "",
    var cwsnG: String = "",
    var pvtgB: String = "",
    var pvtgG: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyEnrollmentScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val savedEnrollment by viewModel.currentMonthEnrollment.collectAsState()
    val allEnrollments by viewModel.allEnrollments.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCopyDialog by remember { mutableStateOf(false) }

    val school by viewModel.school.collectAsState()
    val classList = remember(school?.schoolType) {
        getRelevantClasses(school?.schoolType)
    }
    val classInputs = remember {
        mutableStateMapOf<Int, ClassInputState>().apply {
            (1..8).forEach { num -> put(num, ClassInputState()) }
        }
    }

    var isSavedState by remember { mutableStateOf(false) }

    // Helper to populate inputs from an entity
    fun populateFromEntity(entity: MonthlyEnrollmentEntity) {
        val parsedMap = parseClassBreakupJson(entity.classBreakupJson)
        if (parsedMap.isNotEmpty()) {
            classList.forEach { classNum ->
                val c = parsedMap[classNum]
                if (c != null) {
                    classInputs[classNum] = ClassInputState(
                        stB = if (c.stB > 0) c.stB.toString() else "",
                        stG = if (c.stG > 0) c.stG.toString() else "",
                        scB = if (c.scB > 0) c.scB.toString() else "",
                        scG = if (c.scG > 0) c.scG.toString() else "",
                        obcB = if (c.obcB > 0) c.obcB.toString() else "",
                        obcG = if (c.obcG > 0) c.obcG.toString() else "",
                        genB = if (c.genB > 0) c.genB.toString() else "",
                        genG = if (c.genG > 0) c.genG.toString() else "",
                        minorityB = if (c.minorityB > 0) c.minorityB.toString() else "",
                        minorityG = if (c.minorityG > 0) c.minorityG.toString() else "",
                        cwsnB = if (c.cwsnB > 0) c.cwsnB.toString() else "",
                        cwsnG = if (c.cwsnG > 0) c.cwsnG.toString() else "",
                        pvtgB = if (c.pvtgB > 0) c.pvtgB.toString() else "",
                        pvtgG = if (c.pvtgG > 0) c.pvtgG.toString() else ""
                    )
                } else {
                    classInputs[classNum] = ClassInputState()
                }
            }
        } else {
            classList.forEach { classNum ->
                classInputs[classNum] = ClassInputState()
            }
        }
    }

    // Synchronize UI inputs whenever month or saved DB enrollment changes
    LaunchedEffect(savedEnrollment, selectedMonth, classList) {
        val existing = savedEnrollment
        if (existing != null) {
            populateFromEntity(existing)
            isSavedState = true
        } else {
            // Unrecorded month default: clear inputs for relevant classes
            classList.forEach { num ->
                classInputs[num] = ClassInputState()
            }
            isSavedState = false
        }
    }

    // Dynamic numeric calculations across all classes
    var grandTotalBoys = 0
    var grandTotalGirls = 0
    var grandTotalSt = 0
    var grandTotalSc = 0
    var grandTotalObc = 0
    var grandTotalGen = 0
    var grandTotalMinority = 0
    var grandTotalCwsn = 0
    var grandTotalPvtg = 0

    classList.forEach { num ->
        val input = classInputs[num] ?: ClassInputState()
        val stB = input.stB.toIntOrNull() ?: 0
        val stG = input.stG.toIntOrNull() ?: 0
        val scB = input.scB.toIntOrNull() ?: 0
        val scG = input.scG.toIntOrNull() ?: 0
        val obcB = input.obcB.toIntOrNull() ?: 0
        val obcG = input.obcG.toIntOrNull() ?: 0
        val genB = input.genB.toIntOrNull() ?: 0
        val genG = input.genG.toIntOrNull() ?: 0

        val minB = input.minorityB.toIntOrNull() ?: 0
        val minG = input.minorityG.toIntOrNull() ?: 0
        val cwB = input.cwsnB.toIntOrNull() ?: 0
        val cwG = input.cwsnG.toIntOrNull() ?: 0
        val pvB = input.pvtgB.toIntOrNull() ?: 0
        val pvG = input.pvtgG.toIntOrNull() ?: 0

        grandTotalBoys += (stB + scB + obcB + genB)
        grandTotalGirls += (stG + scG + obcG + genG)
        grandTotalSt += (stB + stG)
        grandTotalSc += (scB + scG)
        grandTotalObc += (obcB + obcG)
        grandTotalGen += (genB + genG)

        grandTotalMinority += (minB + minG)
        grandTotalCwsn += (cwB + cwG)
        grandTotalPvtg += (pvB + pvG)
    }

    val grandTotalStudents = grandTotalBoys + grandTotalGirls

    // Previous month string helper
    val prevMonthStr = getPreviousMonth(selectedMonth)
    val prevEnrollment = remember(allEnrollments, prevMonthStr) {
        allEnrollments.firstOrNull { it.monthYear == prevMonthStr }
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

    val classRangeSubtitle = when {
        classList.firstOrNull() == 1 && classList.lastOrNull() == 5 -> if (isHi) "छात्र पंजीयन (कक्षा 1 से 5)" else "Student Census (Class 1 to 5)"
        classList.firstOrNull() == 6 && classList.lastOrNull() == 8 -> if (isHi) "छात्र पंजीयन (कक्षा 6 से 8)" else "Student Census (Class 6 to 8)"
        else -> if (isHi) "छात्र पंजीयन (कक्षा 1 से 8)" else "Student Census (Class 1 to 8)"
    }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "मासिक छात्र नामांकन" else "Monthly Student Enrollment",
                subtitle = classRangeSubtitle,
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
                            val jsonPayload = serializeClassBreakup(classInputs, classList)
                            val entity = MonthlyEnrollmentEntity(
                                monthYear = selectedMonth,
                                schoolId = "SCH-CG-RPR-001",
                                academicYear = "2026-27",
                                totalEnrollment = grandTotalStudents,
                                totalBoys = grandTotalBoys,
                                totalGirls = grandTotalGirls,
                                scCount = grandTotalSc,
                                stCount = grandTotalSt,
                                obcCount = grandTotalObc,
                                generalCount = grandTotalGen,
                                cwsnCount = grandTotalCwsn,
                                minorityCount = grandTotalMinority,
                                pvtgCount = grandTotalPvtg,
                                classBreakupJson = jsonPayload,
                                isLocked = false,
                                createdBy = "Head Teacher",
                                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            )
                            viewModel.saveEnrollment(entity)
                            isSavedState = true
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = if (isHi)
                                        "$displayMonth का छात्र डेटा सुरक्षित कर लिया गया है।"
                                    else
                                        "Student data for $displayMonth saved successfully."
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
                                text = if (isHi) "पिछले माह से कॉपी करें" else "Copy from Previous Month",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = if (isHi)
                                    "$displayPrevMonth अथवा अन्य माह का डेटा लें"
                                else
                                    "Duplicate census from $displayPrevMonth",
                                fontSize = 11.sp,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            if (prevEnrollment != null) {
                                populateFromEntity(prevEnrollment)
                                isSavedState = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isHi)
                                            "$displayPrevMonth का छात्र डेटा कॉपी किया गया। कृपया जांचकर सुरक्षित करें।"
                                        else
                                            "Student census copied from $displayPrevMonth! Review and click Save."
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

            // SUMMARY KPI CARD (Boys, Girls, Total, Categories, Inclusive)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$grandTotalBoys",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHi) "बालक (Boys)" else "Boys",
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
                                text = "$grandTotalGirls",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHi) "बालिकाएं (Girls)" else "Girls",
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
                                text = "$grandTotalStudents",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHi) "कुल छात्र (Total)" else "Total",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Inclusive subcategory summary indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHi) "अल्पसंख्यक: $grandTotalMinority" else "Minority: $grandTotalMinority",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0D9488)
                        )
                        Text(
                            text = if (isHi) "दिव्यांग: $grandTotalCwsn" else "CWSN: $grandTotalCwsn",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7C3AED)
                        )
                        Text(
                            text = if (isHi) "PVTG: $grandTotalPvtg" else "PVTG: $grandTotalPvtg",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEA580C)
                        )
                    }
                }
            }

            // CLASS CARDS (Class 1 to Class 8)
            classList.forEach { classNum ->
                val input = classInputs[classNum] ?: ClassInputState()

                val stB = input.stB.toIntOrNull() ?: 0
                val stG = input.stG.toIntOrNull() ?: 0
                val scB = input.scB.toIntOrNull() ?: 0
                val scG = input.scG.toIntOrNull() ?: 0
                val obcB = input.obcB.toIntOrNull() ?: 0
                val obcG = input.obcG.toIntOrNull() ?: 0
                val genB = input.genB.toIntOrNull() ?: 0
                val genG = input.genG.toIntOrNull() ?: 0

                val classBoys = stB + scB + obcB + genB
                val classGirls = stG + scG + obcG + genG
                val classTotal = classBoys + classGirls

                ClassEnrollmentCard(
                    classNum = classNum,
                    isHi = isHi,
                    totalCount = classTotal,
                    totalBoys = classBoys,
                    totalGirls = classGirls,
                    input = input,
                    onInputChange = { updated ->
                        classInputs[classNum] = updated
                        isSavedState = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // COPY FROM MONTH SELECTION DIALOG
    if (showCopyDialog) {
        val availableMonths = allEnrollments.filter { it.monthYear != selectedMonth }
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
                    text = if (isHi) "माह से छात्र डेटा कॉपी करें" else "Copy Student Census from Month",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHi)
                            "जिस माह से डेटा $displayMonth में कॉपी करना चाहते हैं, उसका चयन करें:"
                        else
                            "Select the recorded month to duplicate into $displayMonth:",
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
                                        populateFromEntity(item)
                                        isSavedState = false
                                        showCopyDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isHi)
                                                    "${formatMonthLabel(item.monthYear, isHi)} का डेटा कॉपी हो गया। कृपया सुरक्षित करें।"
                                                else
                                                    "Data copied from ${formatMonthLabel(item.monthYear, isHi)}! Please click Save."
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
                                                "कुल छात्र: ${item.totalEnrollment} (बालक: ${item.totalBoys}, बालिका: ${item.totalGirls})"
                                            else
                                                "Total: ${item.totalEnrollment} (B: ${item.totalBoys}, G: ${item.totalGirls})",
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
private fun ClassEnrollmentCard(
    classNum: Int,
    isHi: Boolean,
    totalCount: Int,
    totalBoys: Int,
    totalGirls: Int,
    input: ClassInputState,
    onInputChange: (ClassInputState) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: "Class X" on left, "Total: X (Y Boys + Z Girls)" on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHi) "कक्षा $classNum" else "Class $classNum",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = if (isHi)
                        "कुल: $totalCount (${totalBoys} ${if (isHi) "बालक" else "Boys"} + ${totalGirls} ${if (isHi) "बालिका" else "Girls"})"
                    else
                        "Total: $totalCount (${totalBoys} Boys + ${totalGirls} Girls)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: ST
            CategoryStudentRow(
                label = "ST",
                isHi = isHi,
                boysValue = input.stB,
                onBoysChange = { onInputChange(input.copy(stB = it)) },
                girlsValue = input.stG,
                onGirlsChange = { onInputChange(input.copy(stG = it)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: SC
            CategoryStudentRow(
                label = "SC",
                isHi = isHi,
                boysValue = input.scB,
                onBoysChange = { onInputChange(input.copy(scB = it)) },
                girlsValue = input.scG,
                onGirlsChange = { onInputChange(input.copy(scG = it)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: OBC
            CategoryStudentRow(
                label = "OBC",
                isHi = isHi,
                boysValue = input.obcB,
                onBoysChange = { onInputChange(input.copy(obcB = it)) },
                girlsValue = input.obcG,
                onGirlsChange = { onInputChange(input.copy(obcG = it)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: General
            CategoryStudentRow(
                label = "General",
                isHi = isHi,
                boysValue = input.genB,
                onBoysChange = { onInputChange(input.copy(genB = it)) },
                girlsValue = input.genG,
                onGirlsChange = { onInputChange(input.copy(genG = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SEPARATOR LINE FOR INCLUSIVE DETAILS
            HorizontalDivider(
                color = Color(0xFFE2E8F0),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Header for inclusive details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHi) "समावेशी विवरण (कुल छात्रों में से)" else "Inclusive Details (Out of Total)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Row 5: Minority (अल्पसंख्यक)
            CategoryStudentRow(
                label = if (isHi) "अल्पसंख्यक (Minority)" else "Minority",
                isHi = isHi,
                boysValue = input.minorityB,
                onBoysChange = { onInputChange(input.copy(minorityB = it)) },
                girlsValue = input.minorityG,
                onGirlsChange = { onInputChange(input.copy(minorityG = it)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 6: CWSN (दिव्यांग)
            CategoryStudentRow(
                label = if (isHi) "दिव्यांग (CWSN)" else "CWSN (Special Needs)",
                isHi = isHi,
                boysValue = input.cwsnB,
                onBoysChange = { onInputChange(input.copy(cwsnB = it)) },
                girlsValue = input.cwsnG,
                onGirlsChange = { onInputChange(input.copy(cwsnG = it)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 7: PVTG (विशेष पिछड़ी जनजाति)
            CategoryStudentRow(
                label = if (isHi) "विशेष पिछड़ी जनजाति (PVTG)" else "PVTG (Special Tribal)",
                isHi = isHi,
                boysValue = input.pvtgB,
                onBoysChange = { onInputChange(input.copy(pvtgB = it)) },
                girlsValue = input.pvtgG,
                onGirlsChange = { onInputChange(input.copy(pvtgG = it)) }
            )
        }
    }
}

@Composable
private fun CategoryStudentRow(
    label: String,
    isHi: Boolean,
    boysValue: String,
    onBoysChange: (String) -> Unit,
    girlsValue: String,
    onGirlsChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF334155),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = if (isHi) "बालक" else "Boys",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color(0xFF0284C7),
            modifier = Modifier.padding(end = 5.dp)
        )
        CompactNumberInput(
            value = boysValue,
            onValueChange = onBoysChange
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = if (isHi) "बालिका" else "Girls",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color(0xFFE11D48),
            modifier = Modifier.padding(end = 5.dp)
        )
        CompactNumberInput(
            value = girlsValue,
            onValueChange = onGirlsChange
        )
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
                onValueChange(filtered)
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

private fun getRelevantClasses(schoolType: String?): List<Int> {
    val type = (schoolType ?: "").lowercase(Locale.getDefault())
    return when {
        type.contains("upper primary") || type.contains("middle") || type.contains("पूर्व माध्यमिक") || type.contains("6-8") || type.contains("6 to 8") -> {
            if (type.contains("primary with") || type.contains("with primary") || type.contains("1-8") || type.contains("1 to 8") || type.contains("प्राथमिक एवं")) {
                (1..8).toList()
            } else {
                (6..8).toList()
            }
        }
        type.contains("primary") || type.contains("प्राथमिक") || type.contains("1-5") || type.contains("1 to 5") -> (1..5).toList()
        else -> (1..8).toList()
    }
}

private fun serializeClassBreakup(inputs: Map<Int, ClassInputState>, activeClasses: List<Int> = (1..8).toList()): String {
    val array = JSONArray()
    for (classNum in activeClasses) {
        val input = inputs[classNum] ?: ClassInputState()
        val obj = JSONObject().apply {
            put("classNum", classNum)
            put("stB", input.stB.toIntOrNull() ?: 0)
            put("stG", input.stG.toIntOrNull() ?: 0)
            put("scB", input.scB.toIntOrNull() ?: 0)
            put("scG", input.scG.toIntOrNull() ?: 0)
            put("obcB", input.obcB.toIntOrNull() ?: 0)
            put("obcG", input.obcG.toIntOrNull() ?: 0)
            put("genB", input.genB.toIntOrNull() ?: 0)
            put("genG", input.genG.toIntOrNull() ?: 0)
            put("minorityB", input.minorityB.toIntOrNull() ?: 0)
            put("minorityG", input.minorityG.toIntOrNull() ?: 0)
            put("cwsnB", input.cwsnB.toIntOrNull() ?: 0)
            put("cwsnG", input.cwsnG.toIntOrNull() ?: 0)
            put("pvtgB", input.pvtgB.toIntOrNull() ?: 0)
            put("pvtgG", input.pvtgG.toIntOrNull() ?: 0)
        }
        array.put(obj)
    }
    return array.toString()
}

data class ParsedClassCounts(
    val classNum: Int,
    val stB: Int,
    val stG: Int,
    val scB: Int,
    val scG: Int,
    val obcB: Int,
    val obcG: Int,
    val genB: Int,
    val genG: Int,
    val minorityB: Int = 0,
    val minorityG: Int = 0,
    val cwsnB: Int = 0,
    val cwsnG: Int = 0,
    val pvtgB: Int = 0,
    val pvtgG: Int = 0
)

private fun parseClassBreakupJson(json: String?): Map<Int, ParsedClassCounts> {
    if (json.isNullOrBlank()) return emptyMap()
    val result = mutableMapOf<Int, ParsedClassCounts>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val classNum = obj.optInt("classNum", i + 1)
            result[classNum] = ParsedClassCounts(
                classNum = classNum,
                stB = obj.optInt("stB", 0),
                stG = obj.optInt("stG", 0),
                scB = obj.optInt("scB", 0),
                scG = obj.optInt("scG", 0),
                obcB = obj.optInt("obcB", 0),
                obcG = obj.optInt("obcG", 0),
                genB = obj.optInt("genB", 0),
                genG = obj.optInt("genG", 0),
                minorityB = obj.optInt("minorityB", 0),
                minorityG = obj.optInt("minorityG", 0),
                cwsnB = obj.optInt("cwsnB", 0),
                cwsnG = obj.optInt("cwsnG", 0),
                pvtgB = obj.optInt("pvtgB", 0),
                pvtgG = obj.optInt("pvtgG", 0)
            )
        }
    } catch (_: Exception) {}
    return result
}
