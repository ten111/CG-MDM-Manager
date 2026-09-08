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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.UserRole
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.common.poshanButtonColors
import com.example.presentation.common.poshanOutlinedTextFieldColors
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffRoleManagementScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    val allUsers by viewModel.allUsers.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val school by viewModel.school.collectAsState()

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserAccountEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<UserAccountEntity?>(null) }
    var showPermissionMatrixSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "स्टाफ एवं यूजर रोल प्रबंधन" else "Staff & User Roles (RBAC)",
                subtitle = if (isHi) "प्रधानाध्यापक, एमडीएम प्रभारी एवं SHG अधिकार" else "Headmaster, MDM In-charge & SHG Access",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showPermissionMatrixSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Permissions",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUserRole == UserRole.HEADMASTER) {
                FloatingActionButton(
                    onClick = {
                        editingUser = null
                        showAddStaffDialog = true
                    },
                    containerColor = BluePrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHi) "नया स्टाफ जोड़ें" else "Add Staff", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ROLE SUMMARY BANNER
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) "सक्रिय स्टाफ एवं अधिकृत भूमिकाएं" else "Active Staff & Authorized Roles",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isHi) "कुल पंजीकृत उपयोगकर्ता: ${allUsers.size}" else "Total registered users: ${allUsers.size}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    OutlinedButton(
                        onClick = { showPermissionMatrixSheet = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHi) "अधिकार सूची" else "Permissions", fontSize = 12.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allUsers) { user ->
                    val roleObj = UserRole.fromCode(user.role)
                    val isHeadmaster = roleObj == UserRole.HEADMASTER

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHeadmaster) BluePrimary.copy(alpha = 0.4f) else CardBorderColor
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (roleObj) {
                                                    UserRole.HEADMASTER -> BluePrimary.copy(alpha = 0.12f)
                                                    UserRole.MDM_INCHARGE -> Color(0xFFDCFCE7)
                                                    UserRole.SHG_REPRESENTATIVE -> Color(0xFFFEF3C7)
                                                    UserRole.ASSISTANT_TEACHER -> Color(0xFFF3E8FF)
                                                    else -> Color(0xFFF1F5F9)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (roleObj) {
                                                UserRole.HEADMASTER -> Icons.Default.AdminPanelSettings
                                                UserRole.MDM_INCHARGE -> Icons.Default.AssignmentInd
                                                UserRole.SHG_REPRESENTATIVE -> Icons.Default.SoupKitchen
                                                else -> Icons.Default.Person
                                            },
                                            contentDescription = null,
                                            tint = when (roleObj) {
                                                UserRole.HEADMASTER -> BluePrimary
                                                UserRole.MDM_INCHARGE -> Color(0xFF16A34A)
                                                UserRole.SHG_REPRESENTATIVE -> Color(0xFFD97706)
                                                else -> Color(0xFF7C3AED)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = user.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Surface(
                                            color = when (roleObj) {
                                                UserRole.HEADMASTER -> BluePrimary.copy(alpha = 0.1f)
                                                UserRole.MDM_INCHARGE -> Color(0xFFDCFCE7)
                                                UserRole.SHG_REPRESENTATIVE -> Color(0xFFFEF3C7)
                                                else -> Color(0xFFF3E8FF)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isHi) roleObj.titleHi else roleObj.titleEn,
                                                color = when (roleObj) {
                                                    UserRole.HEADMASTER -> BluePrimary
                                                    UserRole.MDM_INCHARGE -> Color(0xFF16A34A)
                                                    UserRole.SHG_REPRESENTATIVE -> Color(0xFFD97706)
                                                    else -> Color(0xFF7C3AED)
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (currentUserRole == UserRole.HEADMASTER) {
                                    Row {
                                        IconButton(onClick = {
                                            editingUser = user
                                            showAddStaffDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BluePrimary)
                                        }
                                        if (!isHeadmaster) {
                                            IconButton(onClick = { showDeleteConfirmDialog = user }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(8.dp))

                            // USER DETAILS: MOBILE, QUICK PIN & PERMISSIONS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = user.mobile.ifEmpty { if (isHi) "नंबर अनुपलब्ध" else "No phone" },
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "PIN: ${user.pin}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHi) roleObj.descriptionHi else "Access rights granted according to assigned staff role.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // ADD / EDIT STAFF DIALOG
    if (showAddStaffDialog) {
        val editing = editingUser
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var mobile by remember { mutableStateOf(editing?.mobile ?: "") }
        var email by remember { mutableStateOf(editing?.email ?: "") }
        var selectedRole by remember { mutableStateOf(editing?.role ?: UserRole.MDM_INCHARGE.code) }
        var pin by remember { mutableStateOf(editing?.pin ?: "1234") }
        var securityQ by remember { mutableStateOf(editing?.securityQuestion ?: "विद्यालय का उपनाम या गांव क्या है?") }
        var securityA by remember { mutableStateOf(editing?.securityAnswer ?: "bodla") }
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
                showAddStaffDialog = false
            },
            title = {
                Text(
                    text = if (editing == null) {
                        if (isHi) "नया स्टाफ / यूजर रोल जोड़ें" else "Add New Staff / Role"
                    } else {
                        if (isHi) "स्टाफ रोल संपादित करें" else "Edit Staff Details"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isHi) "स्टाफ का नाम *" else "Staff Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = poshanOutlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) mobile = it },
                        label = { Text(if (isHi) "मोबाइल नंबर *" else "Mobile Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = poshanOutlinedTextFieldColors()
                    )

                    Text(
                        text = if (isHi) "भूमिका (User Role) *" else "Select Role *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    val roles = listOf(
                        UserRole.HEADMASTER,
                        UserRole.MDM_INCHARGE,
                        UserRole.SHG_REPRESENTATIVE,
                        UserRole.ASSISTANT_TEACHER
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        roles.forEach { r ->
                            Surface(
                                onClick = { selectedRole = r.code },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedRole == r.code) BluePrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedRole == r.code) BluePrimary else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedRole == r.code,
                                        onClick = { selectedRole = r.code },
                                        colors = RadioButtonDefaults.colors(selectedColor = BluePrimary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            text = if (isHi) r.titleHi else r.titleEn,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedRole == r.code) FontWeight.Bold else FontWeight.Normal,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pin = it },
                        label = { Text(if (isHi) "4-अंकीय लॉगिन पिन *" else "4-Digit Login PIN *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = poshanOutlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = securityA,
                        onValueChange = { securityA = it },
                        label = { Text(if (isHi) "सुरक्षा उत्तर (पिन रीसेट हेतु) *" else "Security Answer (For Reset) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = poshanOutlinedTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || mobile.length != 10 || pin.length != 4) {
                            Toast.makeText(
                                context,
                                if (isHi) "कृपया सभी आवश्यक जानकारी एवं 4 अंकों का पिन दर्ज करें!" else "Please fill all required fields and 4-digit PIN!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        val userToSave = UserAccountEntity(
                            userId = editing?.userId ?: "USER-${UUID.randomUUID().toString().take(8)}",
                            schoolId = school?.schoolId ?: "",
                            udiseCode = school?.udiseCode ?: "",
                            name = name.trim(),
                            role = selectedRole,
                            mobile = mobile.trim(),
                            email = email.trim(),
                            pin = pin.trim(),
                            securityQuestion = securityQ,
                            securityAnswer = securityA.trim().lowercase(),
                            isActive = true,
                            createdAt = editing?.createdAt ?: "2026-08-26",
                            lastLogin = editing?.lastLogin ?: ""
                        )

                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.saveUserStaff(userToSave) {
                            Toast.makeText(
                                context,
                                if (isHi) "स्टाफ विवरण सुरक्षित किया गया!" else "Staff details saved successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            showAddStaffDialog = false
                        }
                    },
                    colors = poshanButtonColors(containerColor = BluePrimary)
                ) {
                    Text(if (isHi) "सुरक्षित करें" else "Save Staff", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        showAddStaffDialog = false
                    }
                ) {
                    Text(if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (showDeleteConfirmDialog != null) {
        val userToDelete = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = if (isHi) "स्टाफ हटाएं?" else "Delete Staff User?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isHi) "क्या आप वाकई '${userToDelete.name}' का खाता हटाना चाहते हैं? यह उपयोगकर्ता अब इस ऐप में लॉगिन नहीं कर सकेगा।"
                    else "Are you sure you want to remove '${userToDelete.name}'? This user will no longer be able to log in."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUserStaff(userToDelete.userId) {
                            Toast.makeText(
                                context,
                                if (isHi) "स्टाफ हटा दिया गया!" else "Staff user removed!",
                                Toast.LENGTH_SHORT
                            ).show()
                            showDeleteConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (isHi) "हटाएं (Delete)" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // PERMISSION MATRIX SHEET
    if (showPermissionMatrixSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPermissionMatrixSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHi) "अधिकार एवं भूमिका निर्देशिका (RBAC)" else "Role & Permission Matrix",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = { showPermissionMatrixSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                PermissionRowCard(
                    role = UserRole.HEADMASTER,
                    isHi = isHi,
                    permissions = listOf(
                        "विद्यालय U-DISE एवं प्रोफाइल संपादन",
                        "स्टाफ जोड़ना/संपादित करना एवं पिन प्रबंधन",
                        "खाद्यान्न मान एवं सरकारी दरें संशोधन",
                        "दैनिक भोजन, उपस्थिति एवं स्टॉक स्वीकृति",
                        "मासिक PDF रिपोर्ट एवं ऑडिट लॉग देखना",
                        "Google Drive ऑटो बैकअप कॉन्फ़िगरेशन"
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                PermissionRowCard(
                    role = UserRole.MDM_INCHARGE,
                    isHi = isHi,
                    permissions = listOf(
                        "दैनिक भोजन एवं छात्र उपस्थिति प्रविष्टि",
                        "PDS खाद्यान्न पावती एवं स्टॉक प्रविष्टि",
                        "रसोइया उपस्थिति एवं दैनिक मेनू दर्ज करना",
                        "मासिक छात्र नामांकन एवं शिक्षक गणना प्रविष्टि",
                        "दैनिक/मासिक भोजन रिपोर्ट देखना"
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                PermissionRowCard(
                    role = UserRole.SHG_REPRESENTATIVE,
                    isHi = isHi,
                    permissions = listOf(
                        "दैनिक भोजन मेनू एवं आवश्यक सामग्री देखना",
                        "रसोइया उपस्थिति दर्ज करना",
                        "रसोईघर साफ-सफाई एवं चखना रिपोर्ट देखना"
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    role: UserRole,
    isHi: Boolean,
    permissions: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isHi) role.titleHi else role.titleEn,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BluePrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            permissions.forEach { perm ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = perm, fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}
