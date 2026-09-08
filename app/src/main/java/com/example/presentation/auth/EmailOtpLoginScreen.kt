package com.example.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.FirebaseAuthRepository
import com.example.auth.OtpErrorCode
import com.example.auth.OtpResult
import com.example.data.local.AppDatabase
import com.example.data.sync.FirestoreSyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OtpLoginStep {
    ENTER_EMAIL,
    ENTER_OTP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailOtpLoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegistration: () -> Unit,
    onQuickPinFallback: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val authRepo = remember { FirebaseAuthRepository(context, db) }
    val syncManager = remember { FirestoreSyncManager(context, db) }

    var currentStep by remember { mutableStateOf(OtpLoginStep.ENTER_EMAIL) }
    var emailInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Resend countdown timer
    var resendCooldownSeconds by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            delay(1000L)
            resendCooldownSeconds -= 1
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Hero Logo and App Title
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Auth",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "छत्तीसगढ़ शासन • शाला पोषण आहार",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "CG-MDM Cloud Login",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = if (currentStep == OtpLoginStep.ENTER_EMAIL)
                        "सुरक्षित 6-अंकीय ईमेल OTP द्वारा लॉगिन करें"
                    else
                        "आपके ईमेल पर भेजा गया 6-अंकों का OTP दर्ज करें",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Status Banner (Success / Error)
                AnimatedVisibility(
                    visible = errorMessage != null || successMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (errorMessage != null)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (errorMessage != null) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: successMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (errorMessage != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // STEP 1: Enter Email
                if (currentStep == OtpLoginStep.ENTER_EMAIL) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "पंजीकृत ईमेल पता (Registered Email) *",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    errorMessage = null
                                    successMessage = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input"),
                                placeholder = { Text("उदाहरण: teacher@cg.gov.in") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Email,
                                        contentDescription = "Email",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (emailInput.isNotBlank() && !isLoading) {
                                            focusManager.clearFocus()
                                            scope.launch {
                                                isLoading = true
                                                errorMessage = null
                                                val result = authRepo.requestEmailOtp(emailInput)
                                                isLoading = false
                                                when (result) {
                                                    is OtpResult.Success -> {
                                                        successMessage = result.data.message
                                                        resendCooldownSeconds = result.data.cooldownSeconds
                                                        currentStep = OtpLoginStep.ENTER_OTP
                                                    }
                                                    is OtpResult.Error -> {
                                                        errorMessage = result.message
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        val result = authRepo.requestEmailOtp(emailInput)
                                        isLoading = false
                                        when (result) {
                                            is OtpResult.Success -> {
                                                successMessage = result.data.message
                                                resendCooldownSeconds = result.data.cooldownSeconds
                                                currentStep = OtpLoginStep.ENTER_OTP
                                            }
                                            is OtpResult.Error -> {
                                                errorMessage = result.message
                                            }
                                            else -> {}
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("send_otp_button"),
                                enabled = emailInput.isNotBlank() && !isLoading,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("6-अंकीय OTP प्राप्त करें (Send OTP)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // STEP 2: Enter 6-Digit OTP
                if (currentStep == OtpLoginStep.ENTER_OTP) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Email chip with edit option
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = emailInput,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Email",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                currentStep = OtpLoginStep.ENTER_EMAIL
                                                otpInput = ""
                                                errorMessage = null
                                            },
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = "6-अंकों का OTP दर्ज करें (Enter 6-Digit OTP)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 6 Distinct OTP Boxes
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { focusRequester.requestFocus() },
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = otpInput,
                                    onValueChange = {
                                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                            otpInput = it
                                            errorMessage = null
                                            if (it.length == 6) {
                                                // Auto verify
                                                scope.launch {
                                                    isLoading = true
                                                    val result = authRepo.verifyOtpAndSignIn(emailInput, it)
                                                    isLoading = false
                                                    when (result) {
                                                        is OtpResult.Success -> {
                                                            successMessage = "सत्यापन सफल! विद्यालय डेटा लोड हो रहा है..."
                                                            // Trigger initial pull for authorized school
                                                            syncManager.initialPullFromCloud(result.data.schoolId)
                                                            delay(600)
                                                            onLoginSuccess()
                                                        }
                                                        is OtpResult.Error -> {
                                                            errorMessage = result.message
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .testTag("otp_input_hidden")
                                        .size(1.dp),
                                    singleLine = true
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 6) {
                                        val char = otpInput.getOrNull(i)?.toString() ?: ""
                                        val isFocused = otpInput.length == i
                                        Surface(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .border(
                                                    width = if (isFocused) 2.dp else 1.dp,
                                                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = char,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Verify Button
                            Button(
                                onClick = {
                                    if (otpInput.length == 6 && !isLoading) {
                                        focusManager.clearFocus()
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            val result = authRepo.verifyOtpAndSignIn(emailInput, otpInput)
                                            isLoading = false
                                            when (result) {
                                                is OtpResult.Success -> {
                                                    successMessage = "सत्यापन सफल! विद्यालय डेटा लोड हो रहा है..."
                                                    syncManager.initialPullFromCloud(result.data.schoolId)
                                                    delay(600)
                                                    onLoginSuccess()
                                                }
                                                is OtpResult.Error -> {
                                                    errorMessage = result.message
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("verify_otp_button"),
                                enabled = otpInput.length == 6 && !isLoading,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("सत्यापित करें एवं लॉगिन करें (Verify & Login)", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Resend OTP Button with countdown
                            TextButton(
                                onClick = {
                                    if (resendCooldownSeconds <= 0 && !isLoading) {
                                        scope.launch {
                                            isLoading = true
                                            val result = authRepo.requestEmailOtp(emailInput)
                                            isLoading = false
                                            when (result) {
                                                is OtpResult.Success -> {
                                                    successMessage = "नया OTP सफलतापूर्वक भेजा गया"
                                                    resendCooldownSeconds = result.data.cooldownSeconds
                                                    otpInput = ""
                                                }
                                                is OtpResult.Error -> {
                                                    errorMessage = result.message
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                },
                                enabled = resendCooldownSeconds <= 0 && !isLoading
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (resendCooldownSeconds > 0)
                                        "पुनः OTP भेजें (${resendCooldownSeconds}s)"
                                    else
                                        "पुनः OTP भेजें (Resend OTP)"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Alternative Options: School Setup or Quick PIN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateToRegistration) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नया विद्यालय सेटअप", style = MaterialTheme.typography.bodySmall)
                    }

                    TextButton(onClick = onQuickPinFallback) {
                        Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("त्वरित 4-अंकीय PIN", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
