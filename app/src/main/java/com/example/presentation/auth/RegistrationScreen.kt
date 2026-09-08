package com.example.presentation.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.*
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: PoshanViewModel,
    onRegistrationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI
    val focusManager = LocalFocusManager.current

    var currentStep by remember { mutableStateOf(1) }

    // Step 1: School Identity State
    var udiseCode by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var schoolType by remember { mutableStateOf("Primary with Upper Primary (Class 1-8)") }
    var stateName by remember { mutableStateOf("छत्तीसगढ़ (Chhattisgarh)") }
    var districtName by remember { mutableStateOf("") }
    var blockName by remember { mutableStateOf("") }
    var clusterName by remember { mutableStateOf("") }
    var villageName by remember { mutableStateOf("") }

    // Step 2: Headmaster Identity State
    var headmasterName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var emailId by remember { mutableStateOf("") }

    // Step 3: Security & Quick PIN Setup
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var securityQuestion by remember { mutableStateOf(if (isHi) "विद्यालय का उपनाम या गांव क्या है?" else "What is the village or school nickname?") }
    var securityAnswer by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val schoolTypes = listOf(
        "Primary (Class 1-5)" to if (isHi) "प्राथमिक शाला (कक्षा 1-5)" else "Primary School (Class 1-5)",
        "Upper Primary (Class 6-8)" to if (isHi) "पूर्व माध्यमिक शाला (कक्षा 6-8)" else "Upper Primary School (Class 6-8)",
        "Primary with Upper Primary (Class 1-8)" to if (isHi) "संयुक्त प्राथमिक एवं पूर्व माध्यमिक (कक्षा 1-8)" else "Composite PS & MS (Class 1-8)"
    )

    Scaffold(
        containerColor = BackgroundLight,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // TOP HEADER BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BluePrimary, Color(0xFF0284C7))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isHi) "CG-MDM MANAGER • डिजिटल शाला पंजी" else "CG-MDM MANAGER • Digital MDM Register",
                                    color = SaffronPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (isHi) "शाला पंजीकरण (U-DISE)" else "School Registration",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LanguageToggleButton(
                            currentLanguage = currentLanguage,
                            onToggle = { viewModel.toggleLanguage() },
                            isDarkVariant = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step Progress Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepIndicatorPill(
                            stepNumber = 1,
                            title = if (isHi) "शाला U-DISE" else "School Details",
                            isActive = currentStep == 1,
                            isCompleted = currentStep > 1,
                            modifier = Modifier.weight(1f)
                        )
                        StepIndicatorPill(
                            stepNumber = 2,
                            title = if (isHi) "प्रधानाध्यापक" else "Principal Info",
                            isActive = currentStep == 2,
                            isCompleted = currentStep > 2,
                            modifier = Modifier.weight(1f)
                        )
                        StepIndicatorPill(
                            stepNumber = 3,
                            title = if (isHi) "सुरक्षा पिन" else "Quick PIN",
                            isActive = currentStep == 3,
                            isCompleted = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // MAIN SCROLLABLE FORM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFB91C1C),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                when (currentStep) {
                    1 -> {
                        // STEP 1: SCHOOL DETAILS & U-DISE
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHi) "चरण 1: विद्यालय पहचान एवं U-DISE कोड" else "Step 1: School Identity & U-DISE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                }

                                Divider(color = Color(0xFFF1F5F9))

                                // U-DISE CODE
                                OutlinedTextField(
                                    value = udiseCode,
                                    onValueChange = {
                                        if (it.length <= 11 && it.all { char -> char.isDigit() }) {
                                            udiseCode = it
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text(if (isHi) "11-अंकीय U-DISE कोड *" else "11-Digit U-DISE Code *") },
                                    placeholder = { Text("e.g. 22080100308") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Pin, contentDescription = null, tint = BluePrimary)
                                    },
                                    trailingIcon = {
                                        if (udiseCode.length == 11) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Valid",
                                                tint = GreenPrimary
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )

                                Text(
                                    text = if (isHi) "💡 U-DISE कोड आपके विद्यालय का 11 अंकों का विशिष्ट पहचान कोड है।"
                                    else "💡 U-DISE code is the unique 11-digit national identity for your school.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )

                                // SCHOOL NAME
                                OutlinedTextField(
                                    value = schoolName,
                                    onValueChange = {
                                        schoolName = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isHi) "विद्यालय का पूरा नाम *" else "Full School Name *") },
                                    placeholder = { Text(if (isHi) "शासकीय प्राथमिक/माध्यमिक शाला..." else "Govt. Primary / Middle School...") },
                                    leadingIcon = {
                                        Icon(Icons.Default.School, contentDescription = null, tint = BluePrimary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )

                                // SCHOOL CATEGORY
                                Text(
                                    text = if (isHi) "शाला की श्रेणी (Category) *" else "School Category *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    schoolTypes.forEach { (typeVal, typeLabel) ->
                                        Surface(
                                            onClick = { schoolType = typeVal },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (schoolType == typeVal) BluePrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (schoolType == typeVal) BluePrimary else Color(0xFFE2E8F0)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = schoolType == typeVal,
                                                    onClick = { schoolType = typeVal },
                                                    colors = RadioButtonDefaults.colors(selectedColor = BluePrimary)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = typeLabel,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (schoolType == typeVal) FontWeight.Bold else FontWeight.Normal,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                // DISTRICT & BLOCK
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = districtName,
                                        onValueChange = {
                                            districtName = it
                                            errorMessage = null
                                        },
                                        label = { Text(if (isHi) "जिला *" else "District *") },
                                        placeholder = { Text("कबीरधाम") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                        modifier = Modifier.weight(1f),
                                        colors = poshanOutlinedTextFieldColors()
                                    )
                                    OutlinedTextField(
                                        value = blockName,
                                        onValueChange = {
                                            blockName = it
                                            errorMessage = null
                                        },
                                        label = { Text(if (isHi) "विकासखंड (Block) *" else "Block *") },
                                        placeholder = { Text("बोड़ला") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                        modifier = Modifier.weight(1f),
                                        colors = poshanOutlinedTextFieldColors()
                                    )
                                }

                                // CLUSTER & VILLAGE
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = clusterName,
                                        onValueChange = { clusterName = it },
                                        label = { Text(if (isHi) "संकुल (Cluster)" else "Cluster") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                        modifier = Modifier.weight(1f),
                                        colors = poshanOutlinedTextFieldColors()
                                    )
                                    OutlinedTextField(
                                        value = villageName,
                                        onValueChange = { villageName = it },
                                        label = { Text(if (isHi) "ग्राम / शहर" else "Village / Town") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        modifier = Modifier.weight(1f),
                                        colors = poshanOutlinedTextFieldColors()
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (udiseCode.length != 11) {
                                    errorMessage = if (isHi) "कृपया 11 अंकों का मान्य U-DISE कोड दर्ज करें!" else "Please enter a valid 11-digit U-DISE code!"
                                    return@Button
                                }
                                if (schoolName.isBlank()) {
                                    errorMessage = if (isHi) "कृपया विद्यालय का नाम दर्ज करें!" else "Please enter school name!"
                                    return@Button
                                }
                                if (districtName.isBlank() || blockName.isBlank()) {
                                    errorMessage = if (isHi) "कृपया जिला एवं विकासखंड का नाम दर्ज करें!" else "Please enter District and Block name!"
                                    return@Button
                                }
                                errorMessage = null
                                currentStep = 2
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = poshanButtonColors(containerColor = BluePrimary)
                        ) {
                            Text(
                                text = if (isHi) "अगला: प्रधानाध्यापक विवरण ➔" else "Next: Principal Info ➔",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(120.dp))
                    }

                    2 -> {
                        // STEP 2: HEADMASTER / IN-CHARGE DETAILS
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHi) "चरण 2: संस्था प्रमुख / प्रधानाध्यापक विवरण" else "Step 2: Headmaster / Principal Details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                }

                                Divider(color = Color(0xFFF1F5F9))

                                OutlinedTextField(
                                    value = headmasterName,
                                    onValueChange = {
                                        headmasterName = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isHi) "प्रधानाध्यापक का पूरा नाम *" else "Principal / Headmaster Full Name *") },
                                    placeholder = { Text(if (isHi) "e.g. श्री राजेश कुमार शर्मा" else "e.g. Shri Rajesh Kumar Sharma") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = BluePrimary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )

                                OutlinedTextField(
                                    value = mobileNumber,
                                    onValueChange = {
                                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                            mobileNumber = it
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text(if (isHi) "आधिकारिक मोबाइल नंबर *" else "Official Mobile Number *") },
                                    placeholder = { Text("98260XXXXX") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = BluePrimary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )

                                OutlinedTextField(
                                    value = emailId,
                                    onValueChange = {
                                        emailId = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isHi) "ईमेल पता (गूगल ड्राइव बैकअप हेतु) *" else "Email ID (for Cloud Backup) *") },
                                    placeholder = { Text("hm.school@gov.in") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = BluePrimary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )

                                Surface(
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHi) "प्रधानाध्यापक के पास विद्यालय प्रोफाइल, स्टाफ रोल एवं Google Drive बैकअप का पूर्ण प्रशासनिक अधिकार रहेगा।"
                                            else "Headmaster will hold super-admin rights for school profile, staff roles & Google Drive backups.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    currentStep = 1
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isHi) "⬅ पीछे" else "⬅ Back")
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (headmasterName.isBlank()) {
                                        errorMessage = if (isHi) "कृपया प्रधानाध्यापक का नाम दर्ज करें!" else "Please enter principal name!"
                                        return@Button
                                    }
                                    if (mobileNumber.length != 10) {
                                        errorMessage = if (isHi) "कृपया 10 अंकों का मान्य मोबाइल नंबर दर्ज करें!" else "Please enter valid 10-digit mobile number!"
                                        return@Button
                                    }
                                    if (emailId.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailId.trim()).matches()) {
                                        errorMessage = if (isHi) "कृपया मान्य ईमेल आईडी दर्ज करें (गूगल बैकअप हेतु)!" else "Please enter a valid email address for Google backup!"
                                        return@Button
                                    }
                                    errorMessage = null
                                    currentStep = 3
                                },
                                modifier = Modifier.weight(2f).height(50.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = poshanButtonColors(containerColor = BluePrimary)
                            ) {
                                Text(
                                    text = if (isHi) "अगला: सुरक्षा पिन ➔" else "Next: Quick PIN ➔",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(120.dp))
                    }

                    3 -> {
                        // STEP 3: SECURITY & 4-DIGIT PIN SETUP
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHi) "चरण 3: 4-अंकीय त्वरित सुरक्षा पिन" else "Step 3: 4-Digit Quick Unlock PIN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                }

                                Divider(color = Color(0xFFF1F5F9))

                                Text(
                                    text = if (isHi) "दैनिक त्वरित लॉगिन के लिए 4 अंकों का सुरक्षा पिन निर्धारित करें:"
                                    else "Set a 4-digit PIN for quick daily one-tap school login:",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )

                                // PIN INPUT
                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                            pin = it
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text(if (isHi) "4-अंकीय मास्टर पिन दर्ज करें *" else "Enter 4-Digit Master PIN *") },
                                    placeholder = { Text("• • • •") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Password, contentDescription = null, tint = BluePrimary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showPin = !showPin }) {
                                            Icon(
                                                imageVector = if (showPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle PIN visibility"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )

                                // CONFIRM PIN INPUT
                                OutlinedTextField(
                                    value = confirmPin,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                            confirmPin = it
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text(if (isHi) "पिन की पुनः पुष्टि करें *" else "Confirm 4-Digit PIN *") },
                                    placeholder = { Text("• • • •") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = if (pin == confirmPin && confirmPin.length == 4) GreenPrimary else BluePrimary)
                                    },
                                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )

                                // SECURITY QUESTION
                                OutlinedTextField(
                                    value = securityQuestion,
                                    onValueChange = { securityQuestion = it },
                                    label = { Text(if (isHi) "सुरक्षा प्रश्न (पिन रिसेट हेतु) *" else "Security Question (For PIN Reset) *") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )

                                // SECURITY ANSWER
                                OutlinedTextField(
                                    value = securityAnswer,
                                    onValueChange = {
                                        securityAnswer = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isHi) "सुरक्षा उत्तर *" else "Security Answer *") },
                                    placeholder = { Text(if (isHi) "e.g. बोड़ला" else "e.g. Bodla") },
                                    leadingIcon = {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = BluePrimary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = poshanOutlinedTextFieldColors()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    currentStep = 2
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isHi) "⬅ पीछे" else "⬅ Back")
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (pin.length != 4) {
                                        errorMessage = if (isHi) "कृपया 4 अंकों का पिन दर्ज करें!" else "Please enter a 4-digit PIN!"
                                        return@Button
                                    }
                                    if (pin != confirmPin) {
                                        errorMessage = if (isHi) "दोनों पिन मेल नहीं खाते!" else "PINs do not match!"
                                        return@Button
                                    }
                                    if (securityAnswer.isBlank()) {
                                        errorMessage = if (isHi) "कृपया सुरक्षा उत्तर दर्ज करें!" else "Please enter security answer!"
                                        return@Button
                                    }

                                    isSubmitting = true
                                    viewModel.registerHeadmasterAndSchool(
                                        udiseCode = udiseCode,
                                        schoolName = schoolName,
                                        schoolType = schoolType,
                                        stateName = stateName,
                                        districtName = districtName,
                                        blockName = blockName,
                                        clusterName = clusterName,
                                        villageName = villageName,
                                        headmasterName = headmasterName,
                                        mobile = mobileNumber,
                                        email = emailId,
                                        pin = pin,
                                        securityQuestion = securityQuestion,
                                        securityAnswer = securityAnswer
                                    ) { success, err ->
                                        isSubmitting = false
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                if (isHi) "🎉 शाला एवं प्रधानाध्यापक पंजीकरण सफल!" else "🎉 School & Principal registration successful!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            onRegistrationSuccess()
                                        } else {
                                            errorMessage = err ?: "Registration failed"
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(2f).height(50.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = poshanButtonColors(containerColor = GreenPrimary)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHi) "पंजीकरण पूर्ण करें ✓" else "Complete Setup ✓",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicatorPill(
    stepNumber: Int,
    title: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            isActive -> Color.White
            isCompleted -> Color(0xFFDCFCE7)
            else -> Color.White.copy(alpha = 0.15f)
        },
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> BluePrimary
                            isCompleted -> GreenPrimary
                            else -> Color.White.copy(alpha = 0.4f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                } else {
                    Text(
                        text = "$stepNumber",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isActive -> BluePrimary
                    isCompleted -> Color(0xFF166534)
                    else -> Color.White.copy(alpha = 0.8f)
                },
                maxLines = 1
            )
        }
    }
}
