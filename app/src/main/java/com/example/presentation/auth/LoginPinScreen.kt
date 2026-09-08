package com.example.presentation.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.UserRole
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.LanguageToggleButton
import com.example.presentation.common.poshanButtonColors
import com.example.presentation.splash.WatermarkedSchoolWallpaper
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPinScreen(
    viewModel: PoshanViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    onNavigateToEmailOtp: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    val school by viewModel.school.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var selectedUser by remember { mutableStateOf<UserAccountEntity?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    var showSwitchUserSheet by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    // Auto-select headmaster or first active user
    LaunchedEffect(allUsers) {
        if (selectedUser == null && allUsers.isNotEmpty()) {
            selectedUser = allUsers.find { it.role == UserRole.HEADMASTER.code } ?: allUsers.firstOrNull()
        }
    }

    // Auto-submit when 4 digits are entered
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            val user = selectedUser
            if (user != null) {
                isChecking = true
                viewModel.switchCurrentUser(user, enteredPin) { success, err ->
                    isChecking = false
                    if (success) {
                        Toast.makeText(
                            context,
                            if (isHi) "स्वागत है, ${user.name}!" else "Welcome, ${user.name}!",
                            Toast.LENGTH_SHORT
                        ).show()
                        onLoginSuccess()
                    } else {
                        errorMessage = err ?: if (isHi) "गलत पिन दर्ज किया गया!" else "Incorrect PIN entered!"
                        enteredPin = ""
                    }
                }
            } else {
                viewModel.loginWithPin(enteredPin) { success, err ->
                    if (success) {
                        onLoginSuccess()
                    } else {
                        errorMessage = err ?: if (isHi) "गलत पिन दर्ज किया गया!" else "Incorrect PIN entered!"
                        enteredPin = ""
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        WatermarkedSchoolWallpaper(
            transparencyAlpha = 0.16f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    .padding(horizontal = 20.dp, vertical = 20.dp)
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(26.dp)
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
                                    text = if (isHi) "सुरक्षित त्वरित लॉगिन" else "Secure Quick Login",
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

                    // SCHOOL INFO BADGE
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val currentSchool = school
                            Column {
                                Text(
                                    text = currentSchool?.schoolName ?: "शासकीय प्राथमिक एवं पूर्व माध्यमिक शाला",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (currentSchool != null) "U-DISE: ${currentSchool.udiseCode} • ${currentSchool.blockName}, ${currentSchool.districtName}" else "PM-POSHAN (CG MDM)",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // USER PROFILE CARD (WHO IS LOGGING IN)
            val user = selectedUser
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(BluePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (user?.role) {
                                UserRole.HEADMASTER.code -> Icons.Default.AdminPanelSettings
                                UserRole.MDM_INCHARGE.code -> Icons.Default.AssignmentInd
                                UserRole.SHG_REPRESENTATIVE.code -> Icons.Default.SoupKitchen
                                else -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = user?.name ?: if (isHi) "प्रधानाध्यापक" else "Headmaster",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    val roleObj = UserRole.fromCode(user?.role ?: UserRole.HEADMASTER.code)
                    Surface(
                        color = when (roleObj) {
                            UserRole.HEADMASTER -> Color(0xFFEFF6FF)
                            UserRole.MDM_INCHARGE -> Color(0xFFF0FDF4)
                            UserRole.SHG_REPRESENTATIVE -> Color(0xFFFEF3C7)
                            UserRole.ASSISTANT_TEACHER -> Color(0xFFF5F3FF)
                            else -> Color(0xFFF1F5F9)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (isHi) roleObj.titleHi else roleObj.titleEn,
                            color = when (roleObj) {
                                UserRole.HEADMASTER -> BluePrimary
                                UserRole.MDM_INCHARGE -> Color(0xFF16A34A)
                                UserRole.SHG_REPRESENTATIVE -> Color(0xFFD97706)
                                UserRole.ASSISTANT_TEACHER -> Color(0xFF7C3AED)
                                else -> Color(0xFF475569)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showSwitchUserSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHi) "स्टाफ यूजर बदलें" else "Switch Staff User", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // PIN INPUT DOT INDICATORS
            Text(
                text = if (isHi) "4-अंकीय सुरक्षा पिन दर्ज करें" else "Enter 4-Digit Security PIN",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) BluePrimary else Color(0xFFCBD5E1)
                            )
                            .border(
                                width = if (isFilled) 2.dp else 1.dp,
                                color = if (isFilled) BluePrimary else Color(0xFF94A3B8),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // IN-APP NUMERIC KEYPAD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth(0.88f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "DEL")
                    )

                    keyRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { key ->
                                Surface(
                                    onClick = {
                                        errorMessage = null
                                        when (key) {
                                            "C" -> enteredPin = ""
                                            "DEL" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                            else -> if (enteredPin.length < 4) enteredPin += key
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when (key) {
                                        "C", "DEL" -> Color(0xFFF1F5F9)
                                        else -> Color(0xFFF8FAFC)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (key) {
                                            "DEL" -> Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Delete",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            "C" -> Text(
                                                text = "C",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                            else -> Text(
                                                text = key,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // HELPER ACTIONS: FORGOT PIN & RE-REGISTER
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showForgotPinDialog = true }) {
                    Text(
                        text = if (isHi) "🔑 पिन भूल गए? (Reset PIN)" else "🔑 Forgot PIN?",
                        fontSize = 13.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onNavigateToEmailOtp) {
                    Text(
                        text = if (isHi) "☁️ ईमेल OTP" else "☁️ Email OTP",
                        fontSize = 13.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onNavigateToRegistration) {
                    Text(
                        text = if (isHi) "पंजीकरण ➔" else "Register ➔",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

    // SWITCH USER SHEET
    if (showSwitchUserSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSwitchUserSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHi) "स्टाफ उपयोगकर्ता चुनें" else "Select Staff User",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = { showSwitchUserSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allUsers) { u ->
                        val isSelected = selectedUser?.userId == u.userId
                        val roleObj = UserRole.fromCode(u.role)
                        Surface(
                            onClick = {
                                selectedUser = u
                                enteredPin = ""
                                errorMessage = null
                                showSwitchUserSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BluePrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BluePrimary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BluePrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (u.role) {
                                            UserRole.HEADMASTER.code -> Icons.Default.AdminPanelSettings
                                            UserRole.MDM_INCHARGE.code -> Icons.Default.AssignmentInd
                                            UserRole.SHG_REPRESENTATIVE.code -> Icons.Default.SoupKitchen
                                            else -> Icons.Default.Person
                                        },
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = u.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (isHi) roleObj.titleHi else roleObj.titleEn,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // FORGOT PIN RECOVERY DIALOG
    if (showForgotPinDialog) {
        val u = selectedUser
        var enteredAnswer by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var resetError by remember { mutableStateOf<String?>(null) }
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
                showForgotPinDialog = false
            },
            title = {
                Text(
                    text = if (isHi) "पिन रिसेट करें (${u?.name})" else "Reset PIN (${u?.name})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHi) "सुरक्षा प्रश्न: ${u?.securityQuestion ?: "विद्यालय का नाम?"}"
                        else "Security Question: ${u?.securityQuestion ?: "School name?"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = enteredAnswer,
                        onValueChange = { enteredAnswer = it },
                        label = { Text(if (isHi) "सुरक्षा उत्तर दर्ज करें" else "Enter Security Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPin = it
                        },
                        label = { Text(if (isHi) "नया 4-अंकीय पिन" else "New 4-Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetError != null) {
                        Text(
                            text = resetError ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (u != null) {
                            if (newPin.length != 4) {
                                resetError = if (isHi) "नया पिन 4 अंकों का होना चाहिए" else "New PIN must be 4 digits"
                                return@Button
                            }
                            viewModel.resetPinForUser(u.userId, newPin, enteredAnswer) { success, err ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        if (isHi) "पिन सफलतापूर्वक बदल दिया गया!" else "PIN reset successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showForgotPinDialog = false
                                } else {
                                    resetError = err ?: if (isHi) "सुरक्षा उत्तर गलत है!" else "Incorrect security answer!"
                                }
                            }
                        }
                    },
                    colors = poshanButtonColors(containerColor = BluePrimary)
                ) {
                    Text(if (isHi) "पिन अपडेट करें" else "Update PIN", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        showForgotPinDialog = false
                    }
                ) {
                    Text(if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}
