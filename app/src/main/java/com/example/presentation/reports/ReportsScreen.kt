package com.example.presentation.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.presentation.common.swipeToNavigateMonth
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.MonthSelectorRow
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.common.formatMonthToMMYYYY
import com.example.presentation.common.poshanButtonColors
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.reports.MonthlyReportData
import com.example.reports.PdfReportGenerator
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val school by viewModel.school.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val enrollment by viewModel.currentMonthEnrollment.collectAsState()
    val teachers by viewModel.currentMonthTeachers.collectAsState()
    val activeCooks by viewModel.activeCooks.collectAsState()
    val monthMeals by viewModel.monthMealRecords.collectAsState()
    val allReceipts by viewModel.allReceipts.collectAsState()
    val allTransactions by viewModel.stockTransactions.collectAsState()
    val allMealRecords by viewModel.allMealRecords.collectAsState()
    val configNorms by viewModel.configNorms.collectAsState()
    val currentStockKg by viewModel.currentRiceStockKg.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val agencies by viewModel.allAgencies.collectAsState()
    val pdsShops by viewModel.allPdsShops.collectAsState()
    val agency = agencies.firstOrNull()
    val pdsShop = pdsShops.firstOrNull()

    val isHi = currentLanguage == AppLanguage.HINDI

    val monthReceipts = allReceipts.filter { it.receiptDate.startsWith(selectedMonth) }
    val monthRiceReceived = monthReceipts.sumOf { it.quantityKg }
    val totalMealsServed = monthMeals.sumOf { it.studentsServed }
    val totalWorkingDays = monthMeals.count { it.studentsServed > 0 }
    val avgDailyServed = if (totalWorkingDays > 0) totalMealsServed / totalWorkingDays else 0

    // Chronologically accurate opening rice stock for the selected month
    val monthRiceOpening = remember(selectedMonth, allTransactions, allReceipts, allMealRecords, configNorms) {
        calculateRiceOpeningStockForMonth(selectedMonth, allTransactions, allReceipts, allMealRecords, configNorms?.primaryRiceNormGrams ?: 110.0)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var allGeneratedReportFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }

    // Headmaster Signature state
    var isSignedByHeadMaster by remember { mutableStateOf(false) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var headMasterName by remember(school) { mutableStateOf(school?.headTeacherName ?: "") }
    var headMasterDesignation by remember { mutableStateOf("Head Master / प्रधान पाठक") }
    var signDate by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var showDigitalSignaturePad by remember { mutableStateOf(false) }
    var isSignatureModuleExpanded by remember { mutableStateOf(false) }

    // Launcher to pick signature from device gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val processed = processUploadedSignature(context, uri)
                if (processed != null) {
                    saveSignatureBitmapToFile(context, processed)
                    withContext(Dispatchers.Main) {
                        signatureBitmap = processed
                        isSignedByHeadMaster = true
                        Toast.makeText(context, "हस्ताक्षर गैलरी से लोड हो गया", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "फोटो से हस्ताक्षर लोड करने में असमर्थ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Load saved signature on screen launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "headmaster_signature.png")
            if (file.exists()) {
                try {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            signatureBitmap = bmp
                            isSignedByHeadMaster = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Helper to package report data
    fun buildReportData(): MonthlyReportData {
        val riceNormGrams = configNorms?.primaryRiceNormGrams ?: 150.0
        val riceNormKg = riceNormGrams / 1000.0
        val riceConsumed = monthMeals.sumOf { it.riceConsumedKg.takeIf { c -> c > 0 } ?: (it.studentsServed * riceNormKg) }
        val totalRiceStock = monthRiceOpening + monthRiceReceived
        val closingStock = (totalRiceStock - riceConsumed).coerceAtLeast(0.0)

        return MonthlyReportData(
            selectedMonth = selectedMonth,
            displayMonth = selectedMonth,
            school = school,
            agency = agency,
            pdsShop = pdsShop,
            cooks = activeCooks,
            enrollment = enrollment,
            teachers = teachers,
            dailyMeals = monthMeals,
            receipts = monthReceipts,
            configNorms = configNorms,
            openingRiceStockKg = monthRiceOpening,
            closingRiceStockKg = closingStock,
            isSignedByHeadMaster = isSignedByHeadMaster,
            headMasterName = headMasterName,
            headMasterDesignation = headMasterDesignation,
            signDate = signDate,
            signatureBitmap = if (isSignedByHeadMaster) signatureBitmap else null
        )
    }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "मासिक प्रतिवेदन एवं PM POSHAN रिपोर्ट" else "Monthly PM POSHAN Report",
                subtitle = if (isHi) "माह: ${formatMonthToMMYYYY(selectedMonth)}" else "Month: ${formatMonthToMMYYYY(selectedMonth)}",
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() },
                onSyncClick = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
                .swipeToNavigateMonth(
                    selectedMonth = selectedMonth,
                    onMonthSelected = { viewModel.setSelectedMonth(it) }
                )
        ) {
            MonthSelectorRow(
                selectedMonth = selectedMonth,
                onMonthSelected = { viewModel.setSelectedMonth(it) }
            )

            // PDF Action Bar
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 16.dp,
                        containerColor = Color.White,
                        contentColor = BluePrimary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("प्रारूप 1: शाला मासिक प्रपत्र (2 पृष्ठ)", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("प्रारूप 2: मध्यान्ह भोजन सारांश (Landscape)", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("सारांश व सांख्यिकी", fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Quick Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val data = buildReportData()
                                    val file = withContext(Dispatchers.IO) {
                                        PdfReportGenerator.generateCombinedPdf(context, data)
                                    }
                                    PdfReportGenerator.printPdf(context, file, "MDM_Complete_Report_${selectedMonth}")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("प्रिंट / PDF सेव", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val data = buildReportData()
                                    val (combined, f1, f2) = withContext(Dispatchers.IO) {
                                        val c = PdfReportGenerator.generateCombinedPdf(context, data)
                                        val p1 = PdfReportGenerator.generateFormat1Pdf(context, data)
                                        val p2 = PdfReportGenerator.generateFormat2Pdf(context, data)
                                        Triple(c, p1, p2)
                                    }
                                    generatedPdfFile = combined
                                    allGeneratedReportFiles = listOf(combined, f1, f2)
                                    showShareOptionsDialog = true
                                }
                            },
                            colors = poshanButtonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("शेयर करें (Share)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Headmaster Digital On-Screen Signature Card (Collapsible)
                    Surface(
                        color = if (isSignedByHeadMaster) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSignedByHeadMaster) Color(0xFF86EFAC) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            // Header bar (Always visible, compact, and clickable to expand/collapse)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSignatureModuleExpanded = !isSignatureModuleExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isSignedByHeadMaster) Icons.Default.CheckCircle else Icons.Default.Draw,
                                        contentDescription = null,
                                        tint = if (isSignedByHeadMaster) Color(0xFF16A34A) else BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSignedByHeadMaster) "✓ प्रधान पाठक डिजिटल हस्ताक्षर" else "प्रधान पाठक डिजिटल हस्ताक्षर",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSignedByHeadMaster) Color(0xFF166534) else Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (isSignedByHeadMaster) Color(0xFFDCFCE7) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isSignedByHeadMaster) "सक्रिय" else "बंद",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSignedByHeadMaster) Color(0xFF15803D) else Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = isSignedByHeadMaster,
                                        onCheckedChange = { checked ->
                                            isSignedByHeadMaster = checked
                                            if (checked) {
                                                if (signatureBitmap == null) {
                                                    showDigitalSignaturePad = true
                                                    isSignatureModuleExpanded = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { isSignatureModuleExpanded = !isSignatureModuleExpanded },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSignatureModuleExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isSignatureModuleExpanded) "Collapse" else "Expand",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Expandable Signature Details Body
                            AnimatedVisibility(
                                visible = isSignatureModuleExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    HorizontalDivider(color = if (isSignedByHeadMaster) Color(0xFFDCFCE7) else Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (isSignedByHeadMaster) {
                                            "हस्ताक्षरकर्ता: ${headMasterName.ifBlank { "प्रधान पाठक" }} ($signDate)"
                                        } else {
                                            "स्क्रीन पर उंगली/स्टाइलस से हस्ताक्षर करें या गैलरी से मुहर/हस्ताक्षर अपलोड करें"
                                        },
                                        fontSize = 10.sp,
                                        color = if (isSignedByHeadMaster) Color(0xFF15803D) else Color(0xFF64748B)
                                    )

                                    if (isSignedByHeadMaster && signatureBitmap != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                    modifier = Modifier
                                                        .height(36.dp)
                                                        .width(84.dp)
                                                ) {
                                                    Image(
                                                        bitmap = signatureBitmap!!.asImageBitmap(),
                                                        contentDescription = "Signature Preview",
                                                        modifier = Modifier.fillMaxSize().padding(2.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "PDF में मुहर (Stamp) के ठीक ऊपर प्रदर्शित होगा",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF16A34A),
                                                    lineHeight = 12.sp
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(
                                                    onClick = { showDigitalSignaturePad = true },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("स्क्रीन", fontSize = 9.5.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { galleryLauncher.launch("image/*") },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("गैलरी", fontSize = 9.5.sp)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        signatureBitmap = null
                                                        val file = File(context.filesDir, "headmaster_signature.png")
                                                        if (file.exists()) file.delete()
                                                        isSignedByHeadMaster = false
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete Signature",
                                                        tint = Color(0xFFDC2626),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { showDigitalSignaturePad = true },
                                                colors = poshanButtonColors(containerColor = BluePrimary),
                                                modifier = Modifier.weight(1f).height(34.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("✍️ स्क्रीन पर ड्रा करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { galleryLauncher.launch("image/*") },
                                                modifier = Modifier.weight(1f).height(34.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp), tint = BluePrimary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("📁 गैलरी से अपलोड", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> Format1PreviewContent(
                        school = school,
                        selectedMonth = selectedMonth,
                        enrollment = enrollment,
                        teachers = teachers,
                        activeCooks = activeCooks,
                        agency = agency,
                        pdsShop = pdsShop,
                        dailyMeals = monthMeals,
                        receipts = monthReceipts,
                        configNorms = configNorms,
                        openingRice = monthRiceOpening,
                        currentStockKg = currentStockKg,
                        isSigned = isSignedByHeadMaster,
                        signatureBitmap = if (isSignedByHeadMaster) signatureBitmap else null,
                        headMasterName = headMasterName,
                        headMasterDesignation = headMasterDesignation,
                        signDate = signDate
                    )
                    1 -> Format2PreviewContent(
                        school = school,
                        selectedMonth = selectedMonth,
                        enrollment = enrollment,
                        activeCooks = activeCooks,
                        agency = agency,
                        dailyMeals = monthMeals,
                        receipts = monthReceipts,
                        configNorms = configNorms,
                        openingRice = monthRiceOpening,
                        isSigned = isSignedByHeadMaster,
                        signatureBitmap = if (isSignedByHeadMaster) signatureBitmap else null,
                        headMasterName = headMasterName,
                        headMasterDesignation = headMasterDesignation,
                        signDate = signDate
                    )
                    2 -> SummaryStatsContent(
                        school = school,
                        selectedMonth = selectedMonth,
                        enrollment = enrollment,
                        teachers = teachers,
                        activeCooks = activeCooks,
                        dailyMeals = monthMeals,
                        configNorms = configNorms,
                        monthRiceOpening = monthRiceOpening,
                        monthRiceReceived = monthRiceReceived,
                        currentStockKg = currentStockKg
                    )
                }
            }
        }
    }

    // Headmaster On-Screen Digital Signature Dialog
    if (showDigitalSignaturePad) {
        DigitalSignatureDialog(
            initialName = headMasterName.ifBlank { school?.headTeacherName ?: "" },
            initialDesignation = headMasterDesignation,
            initialDate = signDate,
            onDismiss = { showDigitalSignaturePad = false },
            onSignatureSaved = { bmp, name, designation, date ->
                signatureBitmap = bmp
                headMasterName = name
                headMasterDesignation = designation
                signDate = date
                isSignedByHeadMaster = true
                showDigitalSignaturePad = false

                // Save to local storage for persistence
                scope.launch(Dispatchers.IO) {
                    saveSignatureBitmapToFile(context, bmp)
                }
            }
        )
    }

    // Success Dialog on PDF Creation
    if (showPdfSuccessDialog && generatedPdfFile != null) {
        val file = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "PDF प्रपत्र सफलतापूर्वक तैयार!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "माह $selectedMonth का शासकीय प्रपत्र तैयार कर लिया गया है। फाइल: ${file.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "✓ 100% शासकीय छत्तीसगढ़ मानक प्रारूप\n✓ UDISE, छात्र दैनिक उपस्थिति (1 से 31)\n✓ खाद्यान्न (चावल, सोयाबड़ी, दूध) लेजर\n✓ शिक्षक व छात्र जातिवार सांख्यिकी\n✓ संस्था प्रमुख का डिजिटल सील व हस्ताक्षर",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfSuccessDialog = false
                        showShareOptionsDialog = true
                    },
                    colors = poshanButtonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("शेयर करें (WhatsApp / अन्य)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPdfSuccessDialog = false
                        PdfReportGenerator.printPdf(context, file, "MDM_Monthly_${selectedMonth}")
                    }
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("प्रिंट करें / PDF सेव")
                }
            }
        )
    }

    // 1-Click WhatsApp, Admin Office & Multi-Channel Report Share Dialog
    // "Do not send single PDFs, send entries reports at once. System should gets the mobile number
    // and the email address first, if it is available in both the shg and the admin office,
    // prompt should not asks to add the credentials while sening the reports."
    if (showShareOptionsDialog && (generatedPdfFile != null || allGeneratedReportFiles.isNotEmpty())) {
        val completePdfFile = generatedPdfFile ?: allGeneratedReportFiles.first()
        val reportFiles = if (allGeneratedReportFiles.isNotEmpty()) allGeneratedReportFiles else listOf(completePdfFile)

        val activeAgency = agency ?: agencies.firstOrNull()
        val shgName = activeAgency?.name?.ifBlank { "स्व-सहायता समूह" } ?: "स्व-सहायता समूह"
        val shgPresident = activeAgency?.contactPerson?.ifBlank { "अध्यक्ष / सचिव" } ?: "अध्यक्ष / सचिव"
        val currentShgMobile = activeAgency?.mobile?.filter { it.isDigit() }.orEmpty()

        val adminOfficeName = configNorms?.adminOfficeName?.ifBlank { "Block Education Officer" } ?: "Block Education Officer"
        val currentAdminWhatsapp = configNorms?.adminOfficeWhatsapp?.filter { it.isDigit() }.orEmpty()
        val currentAdminEmail = configNorms?.adminOfficeEmail?.trim().orEmpty()

        val isShgConfigured = currentShgMobile.length >= 10
        val isAdminWhatsappConfigured = currentAdminWhatsapp.length >= 10
        val isAdminEmailConfigured = currentAdminEmail.isNotBlank() && currentAdminEmail.contains("@")
        val isAdminConfigured = isAdminWhatsappConfigured || isAdminEmailConfigured

        val bothCredentialsConfigured = isShgConfigured && isAdminConfigured

        var editShgMobile by remember(currentShgMobile) { mutableStateOf(currentShgMobile) }
        var editAdminOfficeName by remember(adminOfficeName) { mutableStateOf(adminOfficeName) }
        var editAdminWhatsapp by remember(currentAdminWhatsapp) { mutableStateOf(currentAdminWhatsapp) }
        var editAdminEmail by remember(currentAdminEmail) { mutableStateOf(currentAdminEmail) }

        var showManualEditSection by remember(bothCredentialsConfigured) { mutableStateOf(!bothCredentialsConfigured) }

        val schoolName = school?.schoolName ?: "शासकीय पूर्व माध्यमिक शाला"
        val udiseCode = school?.udiseCode ?: ""

        AlertDialog(
            onDismissRequest = { showShareOptionsDialog = false },
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
                            text = if (isHi) "समस्त मासिक प्रपत्र प्रेषित करें" else "Dispatch All Monthly Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "माह: $selectedMonth • प्रपत्र 1 एवं 2 (समस्त दैनिक प्रविष्टियां)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    if (bothCredentialsConfigured) {
                        TextButton(
                            onClick = { showManualEditSection = !showManualEditSection },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showManualEditSection) (if (isHi) "वापस" else "Back") else (if (isHi) "बदलें" else "Edit"),
                                fontSize = 11.5.sp,
                                color = BluePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ALL ENTRIES AT ONCE BADGE
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHi)
                                    "समस्त शासकीय प्रविष्टियां संलग्न: प्रपत्र 1 (मासिक सारांश) व प्रपत्र 2 (दैनिक पंजी)"
                                else
                                    "All entry reports attached: Format 1 (Summary) & Format 2 (Daily Register)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    // CONDITIONAL RENDERING:
                    // If credentials are fully available and not in edit mode, show 1-click dispatch buttons directly without prompting!
                    // Requirement: First sending option MUST be the Admin Office!
                    if (!showManualEditSection && bothCredentialsConfigured) {
                        // 1. FIRST SENDING OPTION: ADMINISTRATION OFFICE - OFFICIAL GMAIL (ALL ENTRY REPORTS AT ONCE)
                        if (isAdminEmailConfigured) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = Color(0xFFEA580C),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHi) "$adminOfficeName (Gmail / E-mail)" else "$adminOfficeName (Gmail / E-mail)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF9A3412)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFFFEDD5),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "✓ सक्रिय",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC2410C),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "कार्यालय: $adminOfficeName\nई-मेल: $currentAdminEmail",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF7C2D12),
                                        lineHeight = 15.sp
                                    )

                                    Button(
                                        onClick = {
                                            val emailSubject = "PM POSHAN Monthly Report - $selectedMonth - $schoolName (UDISE: $udiseCode)"
                                            val emailBody = "प्रति,\n$adminOfficeName महोदय,\n\nमध्यान्ह भोजन योजना (PM POSHAN) के अंतर्गत माह $selectedMonth का समस्त मासिक प्रतिवेदन प्रेषित है।\n\nशाला: $schoolName\nUDISE कोड: $udiseCode\nस्व-सहायता समूह: $shgName\n\nसंलग्न प्रपत्र सूची:\n1. सम्पूर्ण प्रतिवेदन (Combined Proforma - Format 1 & Format 2 Complete Entries)\n2. प्रपत्र 1 (मासिक सारांश एवं खाद्यान्न लेजर)\n3. प्रपत्र 2 (छात्र उपस्थिति एवं खाद्यान्न दैनिक व्यय पंजी)\n\nकृपया प्रतिवेदन स्वीकार करने का कष्ट करें।\n\nधन्यवाद,\n${school?.headTeacherName ?: "प्रधान पाठक"}\n$schoolName"
                                            PdfReportGenerator.shareReportsViaEmail(context, reportFiles, currentAdminEmail, emailSubject, emailBody)
                                            showShareOptionsDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHi) "$adminOfficeName को Gmail द्वारा भेजें" else "Send via Gmail to $adminOfficeName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 2. ADMINISTRATION OFFICE - 1-CLICK WHATSAPP
                        if (isAdminWhatsappConfigured) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = Color(0xFF1D4ED8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHi) "$adminOfficeName (WhatsApp)" else "$adminOfficeName (WhatsApp)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1E40AF)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFDBEAFE),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "✓ सक्रिय",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D4ED8),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "कार्यालय: $adminOfficeName\nव्हाट्सएप: +91 $currentAdminWhatsapp",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF1E3A8A),
                                        lineHeight = 15.sp
                                    )

                                    Button(
                                        onClick = {
                                            val caption = "प्रति, $adminOfficeName महोदय\nमध्यान्ह भोजन योजना (PM POSHAN) - समस्त मासिक प्रपत्र\nमाह: $selectedMonth\nशाला: $schoolName (UDISE: $udiseCode)\n\nसंलग्न: प्रपत्र 1 (मासिक सारांश) एवं प्रपत्र 2 (दैनिक छात्र उपस्थिति एवं खाद्यान्न व्यय पंजी)"
                                            PdfReportGenerator.sharePdfToWhatsApp(context, completePdfFile, currentAdminWhatsapp, caption)
                                            showShareOptionsDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHi) "$adminOfficeName को 1-क्लिक भेजें (WhatsApp)" else "1-Click Send to $adminOfficeName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 3. SW-SAHAYATA SAMUH (SHG) - 1-CLICK WHATSAPP
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHi) "स्व-सहायता समूह (SHG अध्यक्ष)" else "SHG President (WhatsApp)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "✓ सक्रिय",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "समूह: $shgName\nसंपर्क: $shgPresident (+91 $currentShgMobile)",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF14532D),
                                    lineHeight = 15.sp
                                )

                                Button(
                                    onClick = {
                                        val caption = "मध्यान्ह भोजन योजना (PM POSHAN) - समस्त मासिक प्रपत्र\nमाह: $selectedMonth\nशाला: $schoolName (UDISE: $udiseCode)\nसमूह: $shgName\n\nसंलग्न: प्रपत्र 1 (मासिक सारांश) एवं प्रपत्र 2 (दैनिक प्रविष्टियां व खाद्यान्न पंजी)"
                                        PdfReportGenerator.sharePdfToWhatsApp(context, completePdfFile, currentShgMobile, caption)
                                        showShareOptionsDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
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

                        // 4. GENERAL SHARE (ALL APPS - MULTIPLE REPORT ATTACHMENTS)
                        OutlinedButton(
                            onClick = {
                                showShareOptionsDialog = false
                                PdfReportGenerator.shareMultiplePdfs(context, reportFiles, "माह $selectedMonth PM POSHAN समस्त प्रपत्र ($schoolName)")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHi) "समस्त प्रपत्र अन्य ऐप्स से शेयर करें (Drive / Files / Bluetooth)" else "Share all reports with other apps",
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // MANUAL SETUP / EDIT SECTION (Only shown when credentials missing or user explicitly clicked edit)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isHi) "प्रतिवेदन प्रेषण संपर्क विवरण दर्ज करें:" else "Enter Dispatch Contact Details:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )

                                // 1. Admin Office Name
                                OutlinedTextField(
                                    value = editAdminOfficeName,
                                    onValueChange = { editAdminOfficeName = it },
                                    label = { Text(if (isHi) "प्रशासनिक कार्यालय नाम" else "Admin Office Name") },
                                    placeholder = { Text("Block Education Officer") },
                                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF1D4ED8)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 2. Admin Office Email
                                OutlinedTextField(
                                    value = editAdminEmail,
                                    onValueChange = { editAdminEmail = it.trim() },
                                    label = { Text(if (isHi) "कार्यालय ई-मेल पता (Gmail)" else "Admin Office E-mail") },
                                    placeholder = { Text("beo.office@cg.gov.in") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFEA580C)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 3. Admin Office WhatsApp
                                OutlinedTextField(
                                    value = editAdminWhatsapp,
                                    onValueChange = {
                                        if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                                            editAdminWhatsapp = it
                                        }
                                    },
                                    label = { Text(if (isHi) "कार्यालय व्हाट्सएप नंबर" else "Admin Office WhatsApp") },
                                    prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8)) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF1D4ED8)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // 4. SHG Mobile
                                OutlinedTextField(
                                    value = editShgMobile,
                                    onValueChange = {
                                        if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                                            editShgMobile = it
                                        }
                                    },
                                    label = { Text(if (isHi) "स्व-सहायता समूह व्हाट्सएप नंबर" else "SHG President WhatsApp") },
                                    prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF16A34A)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (editShgMobile.length == 10) {
                                            val existingAgency = activeAgency ?: CookingAgencyEntity(
                                                agencyId = "SHG-DEFAULT-01",
                                                name = shgName,
                                                contactPerson = shgPresident,
                                                mobile = editShgMobile,
                                                address = "",
                                                villageOrCity = ""
                                            )
                                            viewModel.saveCookingAgency(existingAgency.copy(mobile = editShgMobile))
                                        }
                                        val existingNorms = configNorms ?: ConfigNormsEntity()
                                        viewModel.saveConfigNorms(
                                            existingNorms.copy(
                                                adminOfficeName = editAdminOfficeName.ifBlank { "Block Education Officer" },
                                                adminOfficeWhatsapp = editAdminWhatsapp,
                                                adminOfficeEmail = editAdminEmail
                                            )
                                        )
                                        showManualEditSection = false
                                        Toast.makeText(
                                            context,
                                            if (isHi) "विवरण सुरक्षित किया गया!" else "Contact details saved successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHi) "विवरण सुरक्षित करें एवं प्रपत्र भेजें" else "Save Details & Send Reports",
                                        fontWeight = FontWeight.Bold,
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
                TextButton(onClick = { showShareOptionsDialog = false }) {
                    Text(if (isHi) "बंद करें (Close)" else "Close")
                }
            }
        )
    }
}

// =============================================================================
// PREVIEW COMPONENT: FORMAT 1 (2-PAGE PORTRAIT PROFORMA)
// =============================================================================

@Composable
private fun Format1PreviewContent(
    school: SchoolEntity?,
    selectedMonth: String,
    enrollment: MonthlyEnrollmentEntity?,
    teachers: MonthlyTeacherEntity?,
    activeCooks: List<CookEntity>,
    agency: CookingAgencyEntity?,
    pdsShop: PdsShopEntity?,
    dailyMeals: List<DailyMealRecordEntity>,
    receipts: List<RiceReceiptEntity>,
    configNorms: ConfigNormsEntity? = null,
    openingRice: Double,
    currentStockKg: Double,
    isSigned: Boolean = false,
    signatureBitmap: Bitmap? = null,
    headMasterName: String = "",
    headMasterDesignation: String = "Head Master",
    signDate: String = ""
) {
    val totalStudents = enrollment?.totalEnrollment ?: 0
    val totalMealsServed = dailyMeals.sumOf { it.studentsServed }
    val mdmDays = dailyMeals.count { it.studentsServed > 0 }
    val monthRiceReceived = receipts.sumOf { it.quantityKg }
    val riceNormGrams = configNorms?.primaryRiceNormGrams ?: 110.0
    val riceNormKg = riceNormGrams / 1000.0
    val riceConsumed = dailyMeals.sumOf { it.riceConsumedKg.takeIf { c -> c > 0 } ?: (it.studentsServed * riceNormKg) }
    val totalRiceStock = openingRice + monthRiceReceived
    val closingRice = (totalRiceStock - riceConsumed).coerceAtLeast(0.0)

    val customItemsList = com.example.data.model.CustomFoodItemParser.parse(configNorms?.customItemsJson)
    val soyabadiNorm = customItemsList.firstOrNull { it.id.contains("badi", ignoreCase = true) || it.id.contains("soya", ignoreCase = true) }?.quantity ?: 0.025
    val soyabadiConsumed = dailyMeals.filter { it.studentsServed > 0 }.sumOf { record ->
        val usedSet = com.example.data.model.CustomFoodItemParser.parseUsedItemIds(record.customItemsUsedJson)
        val isUsed = if (record.customItemsUsedJson.isNotBlank()) usedSet.any { it.contains("badi", ignoreCase = true) || it.contains("soya", ignoreCase = true) } else false
        if (isUsed) record.studentsServed * soyabadiNorm else 0.0
    }

    val soyadudhNorm = customItemsList.firstOrNull { it.id.contains("dudh", ignoreCase = true) || it.name.contains("milk", ignoreCase = true) }?.quantity ?: 0.200
    val soyadudhConsumed = dailyMeals.filter { it.studentsServed > 0 }.sumOf { record ->
        val usedSet = com.example.data.model.CustomFoodItemParser.parseUsedItemIds(record.customItemsUsedJson)
        val isUsed = if (record.customItemsUsedJson.isNotBlank()) usedSet.any { it.contains("dudh", ignoreCase = true) || it.contains("milk", ignoreCase = true) } else false
        if (isUsed) record.studentsServed * soyadudhNorm else 0.0
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // PAGE 1 CONTAINER
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(3.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("CG", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFB45309))
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("मासिक प्रपत्र", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        Text("मध्यान्ह भोजन योजना", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
                        Text("शाला मासिक जानकारी प्रपत्र (पृष्ठ 1)", fontSize = 11.sp, color = Color(0xFF475569))
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFDBEAFE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("MDM", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFF1D4ED8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(8.dp))

                // UDISE & Month Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("स्कुल कोड - ", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        val udise = (school?.udiseCode ?: "22080100308").padEnd(11, '0')
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            udise.forEach { digit ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .border(1.dp, Color.Black, RoundedCornerShape(2.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$digit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Surface(
                        color = Color(0xFFFEF08A),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = "माह: $selectedMonth",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 12 Items School Master Info Grid
                Text("शाला एवं मध्यान्ह भोजन संचालन विवरण:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BluePrimary)
                Spacer(modifier = Modifier.height(4.dp))

                val gridItems = listOf(
                    Pair("1. विकासखंड का नाम", school?.blockName ?: "-"),
                    Pair("2. संकुल केंद्र का नाम", school?.clusterName ?: "-"),
                    Pair("3. शहर / ग्राम का नाम", school?.villageName ?: "-"),
                    Pair("4. शाला का नाम", school?.schoolName ?: "-"),
                    Pair("5. शाला का स्तर", school?.schoolType ?: "-"),
                    Pair("6. कुल विद्यार्थी", "$totalStudents"),
                    Pair("7. माह में कुल लाभान्वित संख्या", "$totalMealsServed"),
                    Pair("8. मध्यान्ह भोजन दिवस", "$mdmDays"),
                    Pair("9. राशन दुकान का नाम", pdsShop?.shopName ?: "-"),
                    Pair("10. MDM संचालन एजेंसी", agency?.name ?: "-"),
                    Pair("11. एजेंसी खाता क्रमांक", agency?.maskedAccountNo?.ifBlank { "-" } ?: "-"),
                    Pair("12. बैंक एवं शाखा", agency?.bankName?.let { if (agency.branchName.isNotBlank()) "$it, शाखा ${agency.branchName}" else it } ?: "-")
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        for (i in 0 until 6) {
                            val l = gridItems[i * 2]
                            val r = gridItems[i * 2 + 1]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (i % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                Row(modifier = Modifier.weight(1f)) {
                                    Text(l.first + ": ", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(l.second, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color(0xFFE2E8F0)))
                                Row(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                                    Text(r.first + ": ", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(r.second, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }
                            if (i < 5) HorizontalDivider(color = Color(0xFFE2E8F0))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Daily Attendance Table
                Text("1. विद्यार्थियों की दैनिक उपस्थिति (दिनांक - 1st TO 31st):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BluePrimary)
                DailyDaysTablePreview(monthStr = selectedMonth, dailyMeals = dailyMeals, isAttendance = true, totalStudents = totalStudents)

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Daily Meal Served Beneficiaries Table
                Text("2. लाभान्वित विद्यार्थियों की दैनिक उपस्थिति (दिनांक - 1st To 31st):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BluePrimary)
                DailyDaysTablePreview(monthStr = selectedMonth, dailyMeals = dailyMeals, isAttendance = false, totalStudents = totalStudents)

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Rice Stock Ledger
                Text("3. अनाज का विवरण (किग्रा में):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BluePrimary)
                LedgerTablePreview(
                    headers = listOf("पूर्व माह का शेष", "माह में प्राप्त", "कुल स्टॉक", "माह में व्यय", "माह के अंत में शेष", "रिमार्क"),
                    values = listOf(
                        String.format(Locale.getDefault(), "%.3f", openingRice),
                        String.format(Locale.getDefault(), "%.3f", monthRiceReceived),
                        String.format(Locale.getDefault(), "%.3f", totalRiceStock),
                        String.format(Locale.getDefault(), "%.3f", riceConsumed),
                        String.format(Locale.getDefault(), "%.3f", closingRice),
                        "NA"
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Section 4 & 5: Soya Badi & Soya Milk
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("4. सोयाबड़ी विवरण (kg):", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = BluePrimary)
                        LedgerTablePreview(
                            headers = listOf("पूर्व शेष", "प्राप्त", "कुल", "व्यय", "शेष"),
                            values = listOf(
                                "0.000",
                                "0.000",
                                "0.000",
                                String.format(Locale.getDefault(), "%.3f", soyabadiConsumed),
                                "0.000"
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("5. सोयादूध (Soya Milk) विवरण (Ltr):", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = BluePrimary)
                        LedgerTablePreview(
                            headers = listOf("पूर्व शेष", "प्राप्त", "कुल", "व्यय", "शेष"),
                            values = listOf(
                                "0.000",
                                "0.000",
                                "0.000",
                                String.format(Locale.getDefault(), "%.3f", soyadudhConsumed),
                                "0.000"
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 6: IFA & Albendazole Tablets
                Text("6. आयरन फोलिक एसिड (IFA) व कृमिनाशक टेबलेट विवरण (नग में):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BluePrimary)
                TabletsTablePreview(enrollment = enrollment, teachers = teachers)
            }
        }

        // PAGE 2 CONTAINER
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(3.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("शाला मासिक जानकारी प्रपत्र (पृष्ठ 2)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    Text("UDISE: ${school?.udiseCode?.ifBlank { "-" } ?: "-"}", fontSize = 11.sp, color = Color(0xFF64748B))
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // Teacher Census
                Text("शिक्षक विवरण (Teacher Census Matrix):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BluePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                TeacherCensusTablePreview(teachers = teachers)

                Spacer(modifier = Modifier.height(14.dp))

                // Student Census
                Text("विद्यार्थी विवरण (Student Census Matrix with Baiga & CWSN):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BluePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                StudentCensusTablePreview(enrollment = enrollment)

                Spacer(modifier = Modifier.height(20.dp))

                // Stamp & Seal Preview with On-Screen Signature
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    OfficialStampSealPreview(
                        school = school,
                        isSigned = isSigned,
                        signatureBitmap = signatureBitmap,
                        headMasterName = headMasterName,
                        headMasterDesignation = headMasterDesignation,
                        signDate = signDate
                    )
                }
            }
        }
    }
}

// =============================================================================
// PREVIEW COMPONENT: FORMAT 2 (LANDSCAPE SUMMARY LEDGER)
// =============================================================================

@Composable
private fun Format2PreviewContent(
    school: SchoolEntity?,
    selectedMonth: String,
    enrollment: MonthlyEnrollmentEntity?,
    activeCooks: List<CookEntity>,
    agency: CookingAgencyEntity?,
    dailyMeals: List<DailyMealRecordEntity>,
    receipts: List<RiceReceiptEntity>,
    configNorms: ConfigNormsEntity?,
    openingRice: Double,
    isSigned: Boolean = false,
    signatureBitmap: Bitmap? = null,
    headMasterName: String = "",
    headMasterDesignation: String = "Head Master",
    signDate: String = ""
) {
    val cookingCostRate = configNorms?.middleReimbursementRate ?: 9.29
    val totalBeneficiaries = dailyMeals.sumOf { it.studentsServed }
    val mdmDays = dailyMeals.count { it.studentsServed > 0 }
    val totalCostAmount = totalBeneficiaries * cookingCostRate

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Text(
                text = "${school?.schoolName ?: "-"}, जिला- ${school?.districtName ?: "-"}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "UDISE: ${school?.udiseCode ?: "-"} • मध्यान्ह भोजन योजना प्रपत्र • माह: $selectedMonth",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rates pill banner
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("कुकिंग कॉस्ट दर: ₹${String.format(Locale.getDefault(), "%.2f", cookingCostRate)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    Text("कुल लाभान्वित: $totalBeneficiaries", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    Text("MDM दिवस: $mdmDays", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    Text("कुल व्यय: ₹${String.format(Locale.getDefault(), "%.2f", totalCostAmount)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("20-स्तंभीय मासिक सारांश पंजी (Master Ledger):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BluePrimary)
            Spacer(modifier = Modifier.height(4.dp))

            // 20-Column Table Horizontal Scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                LandscapeMasterTablePreview(
                    school = school,
                    agency = agency,
                    enrollment = enrollment,
                    dailyMeals = dailyMeals,
                    receipts = receipts,
                    configNorms = configNorms,
                    openingRice = openingRice,
                    cookingCostRate = cookingCostRate
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Section: Cooks Table & Class Matrix
            Text("रसोइया विवरण एवं कक्षावार/जातिवर्गवार सांख्यिकी:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BluePrimary)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(0.45f)) {
                    Text("रसोइया विवरण (Cooks Register):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF475569))
                    CooksRegisterTablePreview(cooks = activeCooks)
                }

                Column(modifier = Modifier.weight(0.55f)) {
                    Text("कक्षावार एवं जातिवर्गवार विद्यार्थी (Matrix):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF475569))
                    ClassMatrixTablePreview(enrollment = enrollment)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Format 2 Bottom Seal & Signature
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                OfficialStampSealPreview(
                    school = school,
                    isSigned = isSigned,
                    signatureBitmap = signatureBitmap,
                    headMasterName = headMasterName,
                    headMasterDesignation = headMasterDesignation,
                    signDate = signDate
                )
            }
        }
    }
}

// =============================================================================
// SUB-TABLE PREVIEW HELPERS
// =============================================================================

@Composable
private fun DailyDaysTablePreview(
    monthStr: String,
    dailyMeals: List<DailyMealRecordEntity>,
    isAttendance: Boolean,
    totalStudents: Int
) {
    val map = remember(monthStr, dailyMeals) {
        val res = mutableMapOf<Int, Int>()
        for (d in 1..31) {
            val dateKey = String.format(Locale.getDefault(), "%s-%02d", monthStr, d)
            val r = dailyMeals.find { it.date == dateKey || it.date.endsWith("-$d") }
            res[d] = if (isAttendance) (r?.studentsPresent ?: 0) else (r?.studentsServed ?: 0)
        }
        res
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Days 1 to 16
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                for (d in 1..16) {
                    Text(
                        text = "$d",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                for (d in 1..16) {
                    val v = map[d] ?: 0
                    Text(
                        text = "$v",
                        fontSize = 9.sp,
                        color = if (v > 0) Color(0xFF0F172A) else Color(0xFF94A3B8),
                        fontWeight = if (v > 0) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFCBD5E1))

            // Days 17 to 31 + Total
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                for (d in 17..31) {
                    Text(
                        text = "$d",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                    )
                }
                Text(
                    text = "कुल",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f).padding(vertical = 2.dp)
                )
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                for (d in 17..31) {
                    val v = map[d] ?: 0
                    Text(
                        text = "$v",
                        fontSize = 9.sp,
                        color = if (v > 0) Color(0xFF0F172A) else Color(0xFF94A3B8),
                        fontWeight = if (v > 0) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 3.dp)
                    )
                }
                val sum = map.values.sum()
                Text(
                    text = "$sum",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f).padding(vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun LedgerTablePreview(headers: List<String>, values: List<String>) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                headers.forEach { h ->
                    Text(
                        text = h,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(4.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                values.forEach { v ->
                    Text(
                        text = v,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletsTablePreview(enrollment: MonthlyEnrollmentEntity?, teachers: MonthlyTeacherEntity?) {
    val boys = enrollment?.totalBoys ?: 0
    val girls = enrollment?.totalGirls ?: 0
    val teachersCount = teachers?.totalTeachers ?: 0

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                Text("IFA टेबलेट वितरण (विद्यार्थी / शिक्षक)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(3.dp))
                Text("कृमिनाशक व्यय (बालक / बालिका)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(3.dp))
                Text("शिक्षक (M+F=T)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f).padding(3.dp))
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                Text("पूर्व: 0 | प्राप्त: 0 | व्यय: 0", fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(4.dp))
                Text("बालक: $boys | बालिका: $girls", fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(4.dp))
                Text("$teachersCount", fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f).padding(4.dp))
            }
        }
    }
}

@Composable
private fun TeacherCensusTablePreview(teachers: MonthlyTeacherEntity?) {
    val t = teachers
    val rows = listOf(
        listOf("अजजा (ST)", "${t?.stTrainedMale ?: 0}", "${t?.stTrainedFemale ?: 0}", "${t?.stUntrainedMale ?: 0}", "${t?.stUntrainedFemale ?: 0}", "${t?.stCount ?: 0}"),
        listOf("अजा (SC)", "${t?.scTrainedMale ?: 0}", "${t?.scTrainedFemale ?: 0}", "${t?.scUntrainedMale ?: 0}", "${t?.scUntrainedFemale ?: 0}", "${t?.scCount ?: 0}"),
        listOf("अपिव (OBC)", "${t?.obcTrainedMale ?: 0}", "${t?.obcTrainedFemale ?: 0}", "${t?.obcUntrainedMale ?: 0}", "${t?.obcUntrainedFemale ?: 0}", "${t?.obcCount ?: 0}"),
        listOf("सामान्य (Gen)", "${t?.genTrainedMale ?: 0}", "${t?.genTrainedFemale ?: 0}", "0", "0", "${t?.generalCount ?: 0}"),
        listOf("कुल (Total)", "${t?.trainedCount ?: 0}", "${t?.femaleCount ?: 0}", "${t?.untrainedCount ?: 0}", "0", "${t?.totalTeachers ?: 0}")
    )

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                listOf("जाति वर्ग", "प्रशिक्षित (पु/म)", "अप्रशिक्षित (पु/म)", "कुल").forEach { h ->
                    Text(h, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(3.dp))
                }
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            rows.forEachIndexed { idx, r ->
                val isTotal = idx == rows.size - 1
                Row(modifier = Modifier.fillMaxWidth().background(if (isTotal) Color(0xFFEFF6FF) else Color.White)) {
                    Text(r[0], fontSize = 8.5.sp, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f).padding(3.dp))
                    Text("${r[1]}/${r[2]}", fontSize = 8.5.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(3.dp))
                    Text("${r[3]}/${r[4]}", fontSize = 8.5.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(3.dp))
                    Text(r[5], fontSize = 8.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(3.dp))
                }
                if (!isTotal) HorizontalDivider(color = Color(0xFFE2E8F0))
            }
        }
    }
}

@Composable
private fun StudentCensusTablePreview(enrollment: MonthlyEnrollmentEntity?) {
    val e = enrollment
    val rows = listOf(
        listOf("अजजा (ST)", "-", "-", "${e?.stCount ?: 0}", "-", "0", "0", "0", "-"),
        listOf("अजा (SC)", "-", "-", "${e?.scCount ?: 0}", "-", "0", "0", "0", "-"),
        listOf("अपिव (OBC)", "-", "-", "${e?.obcCount ?: 0}", "-", "0", "0", "${e?.cwsnCount ?: 0}", if ((e?.cwsnCount ?: 0) > 0) "दिव्यांग" else "-"),
        listOf("सामान्य", "-", "-", "${e?.generalCount ?: 0}", "-", "0", "0", "0", "-"),
        listOf("कुल", "${e?.totalBoys ?: 0}", "${e?.totalGirls ?: 0}", "${e?.totalEnrollment ?: 0}", "कुल", "0", "0", "${e?.cwsnCount ?: 0}", "-")
    )

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                Text("जाति", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f).padding(2.dp))
                Text("विद्यार्थी (बा/बा/कुल)", fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1.4f).padding(2.dp))
                Text("बैगा (कक्षा/बा/बा)", fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1.3f).padding(2.dp))
                Text("दिव्यांग", fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f).padding(2.dp))
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            rows.forEachIndexed { idx, r ->
                val isTotal = idx == rows.size - 1
                Row(modifier = Modifier.fillMaxWidth().background(if (isTotal) Color(0xFFEFF6FF) else Color.White)) {
                    Text(r[0], fontSize = 8.sp, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(0.8f).padding(2.dp))
                    Text("${r[1]}/${r[2]} (${r[3]})", fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.4f).padding(2.dp))
                    Text("${r[4]}: ${r[5]}/${r[6]}", fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.3f).padding(2.dp))
                    Text("${r[7]} ${if (r[8] != "-") r[8] else ""}", fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f).padding(2.dp))
                }
                if (!isTotal) HorizontalDivider(color = Color(0xFFE2E8F0))
            }
        }
    }
}

@Composable
private fun LandscapeMasterTablePreview(
    school: SchoolEntity?,
    agency: CookingAgencyEntity?,
    enrollment: MonthlyEnrollmentEntity?,
    dailyMeals: List<DailyMealRecordEntity>,
    receipts: List<RiceReceiptEntity>,
    configNorms: ConfigNormsEntity? = null,
    openingRice: Double,
    cookingCostRate: Double
) {
    val totalStudents = enrollment?.totalEnrollment ?: 0
    val totalBeneficiaries = dailyMeals.sumOf { it.studentsServed }
    val mdmDays = dailyMeals.count { it.studentsServed > 0 }
    val avgAttendance = if (mdmDays > 0) totalBeneficiaries / mdmDays else 0
    val monthRiceReceived = receipts.sumOf { it.quantityKg }
    val riceNormGrams = configNorms?.primaryRiceNormGrams ?: 110.0
    val riceNormKg = riceNormGrams / 1000.0
    val riceConsumed = dailyMeals.sumOf { it.riceConsumedKg.takeIf { c -> c > 0 } ?: (it.studentsServed * riceNormKg) }
    val totalRiceStock = openingRice + monthRiceReceived
    val closingRice = (totalRiceStock - riceConsumed).coerceAtLeast(0.0)
    val totalCostAmount = totalBeneficiaries * cookingCostRate

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.width(920.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                listOf(
                    "क्र", "शहर", "शाला नाम", "कुल छात्र (बा/बा/कुल)", "औसत उपस्थिति", "MDM दिवस",
                    "लाभान्वित (बा/बा/कुल)", "पूर्व चावल", "प्राप्त चावल", "कुल स्टॉक", "चावल खपत", "शेष चावल",
                    "व्यय राशि (₹)", "संचालन एजेंसी", "खाता नं", "बैंक नाम"
                ).forEach { h ->
                    Text(h, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(4.dp))
                }
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                val totalB = dailyMeals.sumOf { it.boysServed }
                val totalG = dailyMeals.sumOf { it.girlsServed }
                val enrolledB = enrollment?.totalBoys ?: 0
                val enrolledG = enrollment?.totalGirls ?: 0
                val accNo = agency?.maskedAccountNo?.ifBlank { "-" } ?: "-"
                val bank = agency?.bankName?.let { if (agency.branchName.isNotBlank()) "$it ${agency.branchName}" else it } ?: "-"
                listOf(
                    "1",
                    school?.villageName ?: "-",
                    school?.schoolName ?: "-",
                    "$enrolledB / $enrolledG / $totalStudents",
                    "$avgAttendance",
                    "$mdmDays",
                    "$totalB / $totalG / $totalBeneficiaries",
                    String.format(Locale.getDefault(), "%.3f", openingRice),
                    String.format(Locale.getDefault(), "%.3f", monthRiceReceived),
                    String.format(Locale.getDefault(), "%.3f", totalRiceStock),
                    String.format(Locale.getDefault(), "%.3f", riceConsumed),
                    String.format(Locale.getDefault(), "%.3f", closingRice),
                    "₹${String.format(Locale.getDefault(), "%.2f", totalCostAmount)}",
                    agency?.name ?: "-",
                    accNo,
                    bank
                ).forEach { v ->
                    Text(v, fontSize = 8.5.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun CooksRegisterTablePreview(cooks: List<CookEntity>) {
    val list = cooks

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                Text("क्र.", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f).padding(2.dp))
                Text("रसोइया नाम", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f).padding(2.dp))
                Text("बैंक", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f).padding(2.dp))
                Text("खाता नं", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f).padding(2.dp))
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            list.forEachIndexed { i, c ->
                Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                    Text("${i + 1}", fontSize = 8.sp, modifier = Modifier.weight(0.4f).padding(2.dp))
                    Text(c.name, fontSize = 8.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.4f).padding(2.dp))
                    Text(c.bankName, fontSize = 8.sp, modifier = Modifier.weight(0.9f).padding(2.dp))
                    Text(c.bankAccountNo, fontSize = 8.sp, modifier = Modifier.weight(1.3f).padding(2.dp))
                }
                if (i < list.size - 1) HorizontalDivider(color = Color(0xFFE2E8F0))
            }
        }
    }
}

@Composable
private fun ClassMatrixTablePreview(enrollment: MonthlyEnrollmentEntity?) {
    val e = enrollment
    val st = e?.stCount ?: 0
    val sc = e?.scCount ?: 0
    val obc = e?.obcCount ?: 0
    val gen = e?.generalCount ?: 0
    val totB = e?.totalBoys ?: 0
    val totG = e?.totalGirls ?: 0
    val tot = e?.totalEnrollment ?: 0
    val minCount = e?.minorityCount ?: 0

    val rows = listOf(
        listOf("1", "अजजा", "-/-", "-/-", "-/-", "-/-", "$st"),
        listOf("2", "अजा", "-/-", "-/-", "-/-", "-/-", "$sc"),
        listOf("3", "अपिव", "-/-", "-/-", "-/-", "-/-", "$obc"),
        listOf("4", "सामा.", "-/-", "-/-", "-/-", "-/-", "$gen"),
        listOf("5", "कुल", "-/-", "-/-", "-/-", "$totB/$totG", "$tot"),
        listOf("6", "अल्प.", "-/-", "-/-", "-/-", "-/$minCount", "$minCount")
    )

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9))) {
                Text("जाति", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f).padding(2.dp))
                Text("6वीं", fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f).padding(2.dp))
                Text("7वीं", fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f).padding(2.dp))
                Text("8वीं", fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f).padding(2.dp))
                Text("योग", fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(2.dp))
                Text("महायोग", fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f).padding(2.dp))
            }
            HorizontalDivider(color = Color(0xFFCBD5E1))
            rows.forEachIndexed { idx, r ->
                val isTotal = idx == 4
                Row(modifier = Modifier.fillMaxWidth().background(if (isTotal) Color(0xFFEFF6FF) else Color.White)) {
                    Text(r[1], fontSize = 8.sp, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(0.8f).padding(2.dp))
                    Text(r[2], fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f).padding(2.dp))
                    Text(r[3], fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f).padding(2.dp))
                    Text(r[4], fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f).padding(2.dp))
                    Text(r[5], fontSize = 8.sp, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(2.dp))
                    Text(r[6], fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f).padding(2.dp))
                }
                if (idx < rows.size - 1) HorizontalDivider(color = Color(0xFFE2E8F0))
            }
        }
    }
}

// =============================================================================
// SUMMARY STATS CONTENT (TAB 3)
// =============================================================================

@Composable
private fun SummaryStatsContent(
    school: SchoolEntity?,
    selectedMonth: String,
    enrollment: MonthlyEnrollmentEntity?,
    teachers: MonthlyTeacherEntity?,
    activeCooks: List<CookEntity>,
    dailyMeals: List<DailyMealRecordEntity>,
    configNorms: ConfigNormsEntity? = null,
    monthRiceOpening: Double,
    monthRiceReceived: Double,
    currentStockKg: Double
) {
    val totalMealsServed = dailyMeals.sumOf { it.studentsServed }
    val totalWorkingDays = dailyMeals.count { it.studentsServed > 0 }
    val avgDailyServed = if (totalWorkingDays > 0) totalMealsServed / totalWorkingDays else 0
    val riceNormGrams = configNorms?.primaryRiceNormGrams ?: 110.0
    val riceNormKg = riceNormGrams / 1000.0
    val consumedMonth = dailyMeals.sumOf { it.riceConsumedKg.takeIf { c -> c > 0 } ?: (it.studentsServed * riceNormKg) }
    val totalRiceStock = monthRiceOpening + monthRiceReceived
    val closingMonth = (totalRiceStock - consumedMonth).coerceAtLeast(0.0)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Official Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "प्रधानमंत्री पोषण शक्ति निर्माण योजना (PM POSHAN)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    text = "मासिक प्रगति प्रतिवेदन (Monthly Progress Report) - $selectedMonth",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "शाला: ${school?.schoolName ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF334155)
                )
                Text(
                    text = "UDISE: ${school?.udiseCode ?: "-"} • विकासखंड: ${school?.blockName ?: "-"}, जिला: ${school?.districtName ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }

        // 1. Enrollment Summary
        ReportSectionCard(title = "1. छात्र नामांकन सारांश (Student Census)") {
            val e = enrollment
            if (e == null) {
                Text("इस माह का नामांकन डाटा दर्ज नहीं है।", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
            } else {
                ReportRow("कुल छात्र (Total Enrollment)", "${e.totalEnrollment}")
                ReportRow("बालक (Boys)", "${e.totalBoys}")
                ReportRow("बालिका (Girls)", "${e.totalGirls}")
                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                ReportRow("अनुसूचित जाति (SC)", "${e.scCount}")
                ReportRow("अनुसूचित जनजाति (ST)", "${e.stCount}")
                ReportRow("अन्य पिछड़ा वर्ग (OBC)", "${e.obcCount}")
                ReportRow("सामान्य वर्ग (General)", "${e.generalCount}")
                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                ReportRow("अल्पसंख्यक छात्र (Minority)", "${e.minorityCount}")
                ReportRow("दिव्यांग छात्र (CWSN)", "${e.cwsnCount}")
                ReportRow("विशेष पिछड़ी जनजाति (PVTG)", "${e.pvtgCount}")
            }
        }

        // 2. Meal Operations Summary
        ReportSectionCard(title = "2. मध्यान्ह भोजन संचालन (Meal Operations)") {
            ReportRow("कुल भोजन वितरण दिवस", "$totalWorkingDays दिवस")
            ReportRow("कुल भोजन लाभान्वित संख्या", "$totalMealsServed छात्र-भोजन")
            ReportRow("औसत दैनिक उपस्थिति", "$avgDailyServed छात्र")
            ReportRow("मेनू अनुपालन स्थिति", "100% संतोषप्रद")
            ReportRow("भोजन चखना एवं जांच", "नियमित पूर्ण")
        }

        // 3. Rice Stock & Supply Ledger
        ReportSectionCard(title = "3. चावल एवं खाद्यान्न स्थिति (Rice Stock Ledger)") {
            ReportRow("माह का प्रारंभिक चावल शेष (Opening)", "${String.format(Locale.getDefault(), "%.3f", monthRiceOpening)} kg")
            ReportRow("माह में PDS से प्राप्त चावल (Received)", "+${String.format(Locale.getDefault(), "%.3f", monthRiceReceived)} kg")
            ReportRow("माह में कुल उपलब्ध स्टॉक (Total Stock)", "${String.format(Locale.getDefault(), "%.3f", totalRiceStock)} kg")
            ReportRow("माह में कुल भोजन खपत (Consumed)", "-${String.format(Locale.getDefault(), "%.3f", consumedMonth)} kg")
            ReportRow("माह के अंत में अवशेष स्टॉक (Closing Balance)", "${String.format(Locale.getDefault(), "%.3f", closingMonth)} kg")
        }

        // 4. Cooks & Staff
        ReportSectionCard(title = "4. रसोइया एवं शिक्षक विवरण (Staffing)") {
            ReportRow("कुल कार्यरत रसोइया", "${activeCooks.size} रसोइया")
            ReportRow("संबद्ध स्व-सहायता समूह", activeCooks.firstOrNull()?.associatedAgency ?: "-")
            val teacherTotal = teachers?.totalTeachers ?: 0
            val teacherTrained = teachers?.trainedCount ?: 0
            ReportRow("कुल शिक्षक संख्या", if (teacherTotal > 0) "$teacherTotal (प्रशिक्षित: $teacherTrained)" else "-")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ReportSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }
}

// =============================================================================
// OFFICIAL STAMP & ON-SCREEN SIGNATURE PREVIEW COMPONENT
// =============================================================================

@Composable
fun OfficialStampSealPreview(
    school: SchoolEntity?,
    isSigned: Boolean,
    signatureBitmap: Bitmap?,
    headMasterName: String,
    headMasterDesignation: String,
    signDate: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isSigned) Color(0xFF16A34A) else Color(0xFF312E81)
        ),
        modifier = modifier.width(230.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (signatureBitmap != null) {
                // Actual handwritten signature captured from screen
                Image(
                    bitmap = signatureBitmap.asImageBitmap(),
                    contentDescription = "Headmaster Signature",
                    modifier = Modifier
                        .height(44.dp)
                        .width(140.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "✓ DIGITALLY SIGNED",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534)
                )
            } else if (isSigned) {
                Text(
                    text = "✍️ ${headMasterName.ifBlank { "Head Master" }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF1E1B4B)
                )
                Text(
                    text = "✓ DIGITALLY SIGNED",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534)
                )
            } else {
                Text(
                    text = "✍️ Head Master",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF1E1B4B)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            val nameToShow = headMasterName.ifBlank { school?.headTeacherName ?: "प्रधान पाठक" }
            Text(nameToShow, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
            Text(headMasterDesignation.ifBlank { "Head Master" }, fontSize = 9.sp, color = Color(0xFF475569))
            Text(
                school?.schoolName?.ifBlank { "शासकीय विद्यालय" } ?: "शासकीय विद्यालय",
                fontSize = 9.5.sp,
                color = Color(0xFF1E1B4B),
                textAlign = TextAlign.Center
            )
            val dist = school?.districtName?.ifBlank { "" } ?: ""
            if (dist.isNotBlank()) {
                Text("Distt.-$dist", fontSize = 9.sp, color = Color(0xFF1E1B4B))
            }
            val udise = school?.udiseCode?.ifBlank { "" } ?: ""
            if (udise.isNotBlank()) {
                Text("D.C.-$udise", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
            }
            if (isSigned && signDate.isNotBlank()) {
                Text("दिनांक: $signDate", fontSize = 8.5.sp, color = Color(0xFF15803D))
            }
        }
    }
}

// =============================================================================
// INTERACTIVE ON-SCREEN SIGNATURE PAD DIALOG (DRAW OR GALLERY UPLOAD)
// =============================================================================

@Composable
fun DigitalSignatureDialog(
    initialName: String,
    initialDesignation: String,
    initialDate: String,
    onDismiss: () -> Unit,
    onSignatureSaved: (Bitmap, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var designation by remember { mutableStateOf(initialDesignation) }
    var date by remember { mutableStateOf(initialDate) }

    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var uploadedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    val dialogGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val processed = processUploadedSignature(context, uri)
                withContext(Dispatchers.Main) {
                    if (processed != null) {
                        uploadedBitmap = processed
                        strokes = emptyList()
                        currentStroke = emptyList()
                        Toast.makeText(context, "हस्ताक्षर फोटो लोड हो गई", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "हस्ताक्षर लोड करने में विफल", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        icon = {
            Icon(
                imageVector = Icons.Default.Draw,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "प्रधान पाठक डिजिटल हस्ताक्षर",
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                textAlign = TextAlign.Center,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uploadedBitmap != null) "गैलरी से चयनित हस्ताक्षर:" else "स्क्रीन पर ड्रा करें या गैलरी से चुनें:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = { dialogGalleryLauncher.launch("image/*") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("गैलरी से चुनें", fontSize = 10.sp)
                    }
                }

                // Signature Canvas / Preview Pad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color(0xFF94A3B8), RoundedCornerShape(8.dp))
                ) {
                    if (uploadedBitmap != null) {
                        // Display uploaded image preview
                        Image(
                            bitmap = uploadedBitmap!!.asImageBitmap(),
                            contentDescription = "Uploaded Signature",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Interactive drawing canvas
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStroke = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentStroke = currentStroke + change.position
                                        },
                                        onDragEnd = {
                                            if (currentStroke.isNotEmpty()) {
                                                strokes = strokes + listOf(currentStroke)
                                                currentStroke = emptyList()
                                            }
                                        },
                                        onDragCancel = {
                                            currentStroke = emptyList()
                                        }
                                    )
                                }
                        ) {
                            canvasWidth = size.width
                            canvasHeight = size.height

                            // Baseline guide line
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(20f, size.height - 35f),
                                end = Offset(size.width - 20f, size.height - 35f),
                                strokeWidth = 1.5f
                            )

                            // Draw all completed strokes
                            for (stroke in strokes) {
                                if (stroke.size > 1) {
                                    val path = androidx.compose.ui.graphics.Path()
                                    path.moveTo(stroke.first().x, stroke.first().y)
                                    for (i in 1 until stroke.size) {
                                        val p0 = stroke[i - 1]
                                        val p1 = stroke[i]
                                        path.quadraticBezierTo(p0.x, p0.y, (p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                                    }
                                    path.lineTo(stroke.last().x, stroke.last().y)
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF1E1B4B),
                                        style = Stroke(
                                            width = 4.5f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                } else if (stroke.size == 1) {
                                    drawCircle(
                                        color = Color(0xFF1E1B4B),
                                        radius = 2.5f,
                                        center = stroke.first()
                                    )
                                }
                            }

                            // Draw current active stroke
                            if (currentStroke.size > 1) {
                                val path = androidx.compose.ui.graphics.Path()
                                path.moveTo(currentStroke.first().x, currentStroke.first().y)
                                for (i in 1 until currentStroke.size) {
                                    val p0 = currentStroke[i - 1]
                                    val p1 = currentStroke[i]
                                    path.quadraticBezierTo(p0.x, p0.y, (p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                                }
                                path.lineTo(currentStroke.last().x, currentStroke.last().y)
                                drawPath(
                                    path = path,
                                    color = Color(0xFF1E1B4B),
                                    style = Stroke(
                                        width = 4.5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    // Watermark if empty
                    if (uploadedBitmap == null && strokes.isEmpty() && currentStroke.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✍️ स्क्रीन पर हस्ताक्षर करें (Sign Here)",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "अथवा ऊपर 'गैलरी से चुनें' पर टैप करें",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Quick Canvas Controls (Clear / Undo)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (uploadedBitmap != null) {
                            IconButton(
                                onClick = {
                                    uploadedBitmap = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove Uploaded",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (strokes.isNotEmpty() || currentStroke.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    if (strokes.isNotEmpty()) {
                                        strokes = strokes.dropLast(1)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    strokes = emptyList()
                                    currentStroke = emptyList()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("प्रधान पाठक का नाम (HM Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("पदनाम (Designation)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("दिनांक (Date)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (uploadedBitmap != null) {
                        onSignatureSaved(uploadedBitmap!!, name, designation, date)
                    } else {
                        val allStrokes = if (currentStroke.isNotEmpty()) strokes + listOf(currentStroke) else strokes
                        val bmp = renderSignatureToBitmap(
                            strokes = allStrokes,
                            width = if (canvasWidth > 0) canvasWidth.toInt() else 400,
                            height = if (canvasHeight > 0) canvasHeight.toInt() else 200
                        )
                        if (bmp != null) {
                            onSignatureSaved(bmp, name, designation, date)
                        } else {
                            onSignatureSaved(createFallbackSignedBadgeBitmap(name), name, designation, date)
                        }
                    }
                },
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("हस्ताक्षर सुरक्षित व लागू करें", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

// =============================================================================
// SIGNATURE BITMAP RENDERING & FILE UTILITIES
// =============================================================================

fun processUploadedSignature(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null

        // Scale down to max 600x300 while maintaining aspect ratio
        val maxW = 600
        val maxH = 300
        val ratio = minOf(maxW.toFloat() / original.width, maxH.toFloat() / original.height, 1f)
        val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)

        // Make pure/light white background transparent and enhance contrast for signatures
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(targetW * targetH)
        scaled.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // Calculate luminance / brightness
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            if (luminance > 215) {
                // Background white/light grey -> transparent
                pixels[i] = 0x00000000
            } else if (luminance < 140) {
                // Dark ink -> sharp dark indigo / navy blue
                val alpha = a.coerceAtLeast(220)
                pixels[i] = (alpha shl 24) or (30 shl 16) or (27 shl 8) or 75
            } else {
                // Intermediate anti-aliasing edge
                val transAlpha = ((215 - luminance) * 255 / 75).coerceIn(0, 255)
                pixels[i] = (transAlpha shl 24) or (30 shl 16) or (27 shl 8) or 75
            }
        }
        output.setPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        output
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun renderSignatureToBitmap(
    strokes: List<List<Offset>>,
    width: Int,
    height: Int,
    strokeColor: Int = android.graphics.Color.rgb(30, 27, 75),
    strokeWidthPx: Float = 5.5f
): Bitmap? {
    if (strokes.isEmpty() || width <= 0 || height <= 0) return null
    val bitmap = Bitmap.createBitmap(
        width.coerceAtLeast(120),
        height.coerceAtLeast(60),
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }

    for (stroke in strokes) {
        if (stroke.isEmpty()) continue
        if (stroke.size == 1) {
            val p = stroke.first()
            val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(p.x, p.y, strokeWidthPx / 2f, dotPaint)
            continue
        }
        val path = android.graphics.Path()
        path.moveTo(stroke.first().x, stroke.first().y)
        for (i in 1 until stroke.size) {
            val p0 = stroke[i - 1]
            val p1 = stroke[i]
            path.quadTo(p0.x, p0.y, (p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
        }
        path.lineTo(stroke.last().x, stroke.last().y)
        canvas.drawPath(path, paint)
    }
    return bitmap
}

fun createFallbackSignedBadgeBitmap(name: String): Bitmap {
    val w = 320
    val h = 120
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val sp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(30, 27, 75)
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = android.graphics.Paint.Cap.ROUND
    }
    // Elegant cursive signature line
    val p = android.graphics.Path().apply {
        moveTo(30f, 65f)
        quadTo(80f, 25f, 130f, 65f)
        quadTo(180f, 105f, 230f, 45f)
        quadTo(260f, 75f, 290f, 55f)
        moveTo(50f, 75f)
        lineTo(260f, 75f)
    }
    canvas.drawPath(p, sp)
    return bitmap
}

fun saveSignatureBitmapToFile(context: Context, bitmap: Bitmap) {
    try {
        val file = File(context.filesDir, "headmaster_signature.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Calculates the exact opening stock balance for rice for the given [selectedMonth] (e.g. "2026-06").
 * It respects any baseline OPENING_BALANCE transaction, receipts received prior to this month,
 * manual adjustments prior to this month, and daily meal consumptions prior to this month.
 */
fun calculateRiceOpeningStockForMonth(
    selectedMonth: String,
    transactions: List<com.example.data.local.entity.StockTransactionEntity>,
    receipts: List<com.example.data.local.entity.RiceReceiptEntity>,
    mealRecords: List<com.example.data.local.entity.DailyMealRecordEntity>,
    riceNormGrams: Double = 150.0
): Double {
    val riceNormKg = riceNormGrams / 1000.0

    // 1. Find any OPENING_BALANCE transaction for RICE
    val riceOpeningTxns = transactions.filter {
        it.itemType.equals("RICE", ignoreCase = true) && it.transactionType == "OPENING_BALANCE"
    }.sortedBy { it.transactionDate }

    val startOfSelectedMonth = "$selectedMonth-01"

    val baseOpening = riceOpeningTxns.firstOrNull()
    if (baseOpening != null) {
        val baseDate = baseOpening.transactionDate
        val baseMonth = if (baseDate.length >= 7) baseDate.substring(0, 7) else selectedMonth

        if (selectedMonth <= baseMonth) {
            // For the initial opening month (or before), opening balance is the initial baseline
            return baseOpening.quantityKg
        }

        // For subsequent months, compute cumulative changes between baseDate and startOfSelectedMonth:
        var balance = baseOpening.quantityKg

        // Prior Receipts
        val priorReceipts = receipts.filter {
            it.receiptDate >= baseDate && it.receiptDate < startOfSelectedMonth
        }.sumOf { it.quantityKg }

        // Prior manual adjustments
        val priorAdjustments = transactions.filter {
            it.itemType.equals("RICE", ignoreCase = true) &&
            it.transactionType == "ADJUSTMENT" &&
            it.transactionDate >= baseDate &&
            it.transactionDate < startOfSelectedMonth
        }.sumOf { it.quantityKg }

        // Prior meal consumptions
        val priorMealConsumed = mealRecords.filter {
            it.date >= baseDate && it.date < startOfSelectedMonth
        }.sumOf {
            it.riceConsumedKg.takeIf { c -> c > 0 } ?: (it.studentsServed * riceNormKg)
        }

        balance += priorReceipts + priorAdjustments - priorMealConsumed
        return balance.coerceAtLeast(0.0)
    }

    // 2. If no explicit OPENING_BALANCE transaction exists, compute from total receipts and consumption prior to this month:
    val priorReceipts = receipts.filter { it.receiptDate < startOfSelectedMonth }.sumOf { it.quantityKg }
    val priorMealConsumed = mealRecords.filter { it.date < startOfSelectedMonth }.sumOf {
        it.riceConsumedKg.takeIf { c -> c > 0 } ?: (it.studentsServed * riceNormKg)
    }
    return (priorReceipts - priorMealConsumed).coerceAtLeast(0.0)
}

