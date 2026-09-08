package com.example.presentation.more

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.ConfigNormsEntity
import com.example.data.local.entity.MonthlyTeacherEntity
import com.example.data.local.entity.SchoolEntity
import com.example.data.util.LocationData
import com.example.presentation.common.*
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.reminder.ReminderManager
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuScreen(
    viewModel: PoshanViewModel,
    onNavigateToAgencyAndPds: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToEnrollment: () -> Unit,
    onNavigateToTeacherData: () -> Unit,
    onNavigateToCooks: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStaffRoles: () -> Unit = {},
    onNavigateToLoginPin: () -> Unit = {},
    onNavigateToGoogleDriveBackup: () -> Unit = {}
) {
    val school by viewModel.school.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val teachers by viewModel.currentMonthTeachers.collectAsState()
    val enrollment by viewModel.currentMonthEnrollment.collectAsState()
    val auditLogs by viewModel.recentAuditLogs.collectAsState()
    val configNorms by viewModel.configNorms.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI
    val context = LocalContext.current

    var showSchoolDialog by remember { mutableStateOf(false) }
    var showNormsDialog by remember { mutableStateOf(false) }
    var showReimbursementDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAdminOfficeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isHi) "लॉगआउट / स्क्रीन लॉक करें?" else "Lock App / Logout?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = if (isHi)
                        "क्या आप वर्तमान सत्र समाप्त कर ऐप लॉक करना चाहते हैं? दोबारा लॉगिन करने के लिए 4-अंकीय पिन आवश्यक होगा।"
                    else
                        "Do you want to end the current session and lock the app? You will need your 4-digit PIN to login again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onNavigateToLoginPin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHi) "हाँ, लॉगआउट करें" else "Yes, Logout",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "अधिक मेनू एवं सेटिंग्स" else "More Menu & Settings",
                subtitle = if (isHi) "विद्यालय विवरण, शिक्षक, ऑडिट लॉग्स एवं नियम" else "School Details, Teachers, Audit Logs & Norms",
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() },
                onSyncClick = {},
                onLogoutClick = { showLogoutDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SCHOOL MASTER SETTING CARD
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER || currentUserRole == com.example.data.local.entity.UserRole.MDM_INCHARGE) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(BluePrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isHi) "विद्यालय विवरण एवं सेटिंग्स" else "School Settings & Profile",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isHi) "UDISE, नाम, ग्राम, विकासखंड, जिला" else "UDISE, Name, Village, Block, District",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER) {
                                Button(
                                    onClick = { showSchoolDialog = true },
                                    colors = poshanButtonColors(containerColor = BluePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isHi) "संपादित करें" else "Edit", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = school?.schoolName ?: "शासकीय प्राथमिक एवं पूर्व माध्यमिक शाला",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SchoolInfoItem(if (isHi) "UDISE कोड" else "UDISE Code", school?.udiseCode ?: "22010100101")
                                    SchoolInfoItem(if (isHi) "शाला स्तर / प्रकार" else "School Level", school?.schoolType ?: "Middle with primary 1-8")
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SchoolInfoItem(if (isHi) "ग्राम / पता" else "Village / Address", school?.villageName ?: "Urla")
                                    SchoolInfoItem(if (isHi) "संकुल (Cluster)" else "Cluster", school?.clusterName ?: "Birgaon")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SchoolInfoItem(if (isHi) "विकासखंड (Block)" else "Block", school?.blockName ?: "Dharsiwa")
                                    SchoolInfoItem(if (isHi) "जिला (District)" else "District", school?.districtName ?: "Raipur")
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SchoolInfoItem(if (isHi) "राज्य (State)" else "State", school?.stateName ?: "Chhattisgarh")
                                    SchoolInfoItem(if (isHi) "प्रधान पाठक" else "Head Teacher", school?.headTeacherName ?: "Rajesh Sharma")
                                }
                            }
                        }
                    }
                }
            }

            // TEACHER DETAILS QUICK SUMMARY CARD (Only Headmaster & MDM Incharge)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER || currentUserRole == com.example.data.local.entity.UserRole.MDM_INCHARGE) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTeacherData() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = BluePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHi) "मासिक शिक्षक विवरण ($selectedMonth)" else "Monthly Teacher Census ($selectedMonth)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                text = AppStrings.viewDetails(currentLanguage),
                                style = MaterialTheme.typography.labelMedium,
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val t = teachers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatPill(if (isHi) "कुल शिक्षक" else "Total", "${t?.totalTeachers ?: 0}", BluePrimary)
                            StatPill(if (isHi) "पुरुष (Male)" else "Male", "${t?.maleCount ?: 0}", Color(0xFF0284C7))
                            StatPill(if (isHi) "महिला (Female)" else "Female", "${t?.femaleCount ?: 0}", Color(0xFFDB2777))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CategoryChip("ST: ${t?.stCount ?: 0}")
                            CategoryChip("SC: ${t?.scCount ?: 0}")
                            CategoryChip("OBC: ${t?.obcCount ?: 0}")
                            CategoryChip("Gen: ${t?.generalCount ?: 0}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = if (isHi) "प्रशिक्षित शिक्षक: ${t?.trainedCount ?: 0}" else "Trained Teachers: ${t?.trainedCount ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF059669)
                            )
                            Text(
                                text = if (isHi) "अप्रशिक्षित: ${t?.untrainedCount ?: 0}" else "Untrained: ${t?.untrainedCount ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if ((t?.untrainedCount ?: 0) > 0) Color(0xFFE11D48) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // MONTHLY STUDENT CENSUS SUMMARY CARD (Only Headmaster & MDM Incharge)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER || currentUserRole == com.example.data.local.entity.UserRole.MDM_INCHARGE) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToEnrollment() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = BluePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHi) "मासिक छात्र नामांकन ($selectedMonth)" else "Monthly Student Census ($selectedMonth)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                text = AppStrings.viewDetails(currentLanguage),
                                style = MaterialTheme.typography.labelMedium,
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatPill(AppStrings.totalStudents(currentLanguage), "${enrollment?.totalEnrollment ?: 0}", BluePrimary)
                            StatPill(AppStrings.boys(currentLanguage), "${enrollment?.totalBoys ?: 0}", Color(0xFF0284C7))
                            StatPill(AppStrings.girls(currentLanguage), "${enrollment?.totalGirls ?: 0}", Color(0xFFE11D48))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CategoryChip("SC: ${enrollment?.scCount ?: 0}")
                            CategoryChip("ST: ${enrollment?.stCount ?: 0}")
                            CategoryChip("OBC: ${enrollment?.obcCount ?: 0}")
                            CategoryChip("Gen: ${enrollment?.generalCount ?: 0}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = "${AppStrings.cwsn(currentLanguage)}: ${enrollment?.cwsnCount ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7C3AED)
                            )
                            Text(
                                text = "${AppStrings.minority(currentLanguage)}: ${enrollment?.minorityCount ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0D9488)
                            )
                            if ((enrollment?.pvtgCount ?: 0) > 0) {
                                Text(
                                    text = "PVTG: ${enrollment?.pvtgCount ?: 0}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEA580C)
                                )
                            }
                        }
                    }
                }
            }

            // NAVIGATION MENU ITEMS (ADMINISTRATION & MASTERS)
            Text(
                text = if (isHi) "प्रशासनिक एवं मास्टर प्रबंधन" else "Administration & Masters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // Cook Master (Headmaster, MDM Incharge, SHG Representative)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER ||
                currentUserRole == com.example.data.local.entity.UserRole.MDM_INCHARGE ||
                currentUserRole == com.example.data.local.entity.UserRole.SHG_REPRESENTATIVE) {
                MenuNavigationCard(
                    title = if (isHi) "रसोइया प्रबंधन (Cook Master)" else "Cook Management Master",
                    subtitle = if (isHi) "रसोइया प्रोफाइल, बैंक खाता एवं स्थिति" else "Cook profiles, bank details and status",
                    icon = Icons.Default.People,
                    onClick = onNavigateToCooks
                )
            }

            // Cooking Agency & PDS Shop (Headmaster, MDM Incharge)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER ||
                currentUserRole == com.example.data.local.entity.UserRole.MDM_INCHARGE) {
                MenuNavigationCard(
                    title = if (isHi) "रसोई एजेंसी (SHG) एवं PDS दुकान" else "Cooking Agency (SHG) & PDS FPS Shop",
                    subtitle = if (isHi) "स्व-सहायता समूह अनुबंध एवं उचित मूल्य दुकान" else "SHG agreement and Fair Price Shop details",
                    icon = Icons.Default.Storefront,
                    onClick = onNavigateToAgencyAndPds
                )
            }

            // Reimbursement Amount Rate (Only Headmaster)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER) {
                val currentNorms = configNorms
                val pRateDisplay = if (currentNorms != null && currentNorms.primaryReimbursementRate > 0.0) "₹${currentNorms.primaryReimbursementRate}" else "-"
                val mRateDisplay = if (currentNorms != null && currentNorms.middleReimbursementRate > 0.0) "₹${currentNorms.middleReimbursementRate}" else "-"
                MenuNavigationCard(
                    title = if (isHi) "प्रतिपूर्ति राशि दर (Reimbursement Amount)" else "Reimbursement Amount per Student",
                    subtitle = if (isHi) "प्राथमिक: $pRateDisplay • पूर्व माध्यमिक: $mRateDisplay (प्रति छात्र/दिवस)" else "Primary: $pRateDisplay • Middle: $mRateDisplay (per student/day)",
                    icon = Icons.Default.Payments,
                    onClick = { showReimbursementDialog = true }
                )
            }

            // Administration Office Setup (Block Education Officer / Admin Office)
            val currentOfficeName = configNorms?.adminOfficeName?.ifBlank { "Block Education Officer" } ?: "Block Education Officer"
            val officeSubtitle = buildString {
                append(currentOfficeName)
                if (!configNorms?.adminOfficeWhatsapp.isNullOrBlank()) {
                    append(" • +91 ${configNorms?.adminOfficeWhatsapp}")
                }
                if (!configNorms?.adminOfficeEmail.isNullOrBlank()) {
                    append(" • ${configNorms?.adminOfficeEmail}")
                }
            }
            MenuNavigationCard(
                title = if (isHi) "प्रशासनिक कार्यालय सेटअप (Administration Office Setup)" else "Administration Office Setup",
                subtitle = officeSubtitle,
                icon = Icons.Default.AccountBalance,
                onClick = { showAdminOfficeDialog = true }
            )

            // Academic Calendar & Leave Rules (Available to all roles)
            MenuNavigationCard(
                title = if (isHi) "शैक्षणिक कैलेंडर एवं अवकाश नियम" else "Academic Calendar & Leave Rules",
                subtitle = if (isHi) "शासकीय आदेश, अवकाश एवं विशेष कार्यदिवस" else "Govt orders, holidays and special meal days",
                icon = Icons.Default.CalendarMonth,
                onClick = onNavigateToCalendar
            )

            // Food Norms, Custom Items & Thresholds (Only Headmaster)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER) {
                MenuNavigationCard(
                    title = if (isHi) "खाद्यान्न मान, सामग्री एवं थ्रेशोल्ड सेटिंग्स" else "Food Norms, Custom Items & Thresholds",
                    subtitle = if (isHi) "चावल, दाल, सब्जी, तेल, अनुकूलित सामग्री, दरें व अलर्ट सीमा" else "Rice, Pulses, Veg, Oil, Custom Items, Rates & Alerts",
                    icon = Icons.Default.Tune,
                    onClick = { onNavigateToSettings() }
                )
            }

            // Daily Attendance Reminders (Headmaster & MDM Incharge)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER ||
                currentUserRole == com.example.data.local.entity.UserRole.MDM_INCHARGE) {
                MenuNavigationCard(
                    title = if (isHi) "दैनिक उपस्थिति रिमाइंडर एवं अलार्म" else "Daily Attendance Reminders & Alerts",
                    subtitle = if (isHi) "दैनिक भोजन उपस्थिति भरने हेतु कस्टम अलार्म, समय व पुश नोटिफिकेशन" else "Custom alarms, timing & push notifications for daily attendance",
                    icon = Icons.Default.NotificationsActive,
                    onClick = { showReminderDialog = true }
                )
            }

            // USER ROLES & ACCESS CONTROL SECTION
            Text(
                text = if (isHi) "उपयोगकर्ता भूमिकाएं एवं सुरक्षा (RBAC & Auth)" else "User Roles & Security (RBAC)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // Staff Role Management (Only Headmaster)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER) {
                MenuNavigationCard(
                    title = if (isHi) "स्टाफ एवं यूजर रोल प्रबंधन (Staff RBAC)" else "Staff Roles & Access Management",
                    subtitle = if (isHi) "प्रधानाध्यापक, एमडीएम प्रभारी एवं SHG प्रतिनिधि रोल तथा 4-अंकीय पिन" else "Headmaster, MDM Incharge & SHG roles & PIN setup",
                    icon = Icons.Default.ManageAccounts,
                    onClick = onNavigateToStaffRoles
                )
            }

            // Switch User (Available to all roles)
            MenuNavigationCard(
                title = if (isHi) "त्वरित यूजर बदलें / स्क्रीन लॉक (Switch User)" else "Switch Active User / Screen Lock",
                subtitle = if (isHi) "सक्रिय यूजर: ${currentUser?.name ?: "प्रधानाध्यापक"} (${currentUserRole.name})" else "Active User: ${currentUser?.name ?: "Headmaster"} (${currentUserRole.name})",
                icon = Icons.Default.Lock,
                onClick = {
                    viewModel.logout()
                    onNavigateToLoginPin()
                }
            )

            // GOOGLE DRIVE CLOUD BACKUP SECTION (Only Headmaster)
            // CLOUD SYNC & FIREBASE AUTHENTICATION
            Text(
                text = if (isHi) "क्लाउड डेटा सिंक एवं प्रमाणीकरण (Firebase)" else "Cloud Data Sync & Auth (Firebase)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            MenuNavigationCard(
                title = if (isHi) "क्लाउड डेटा सिंक (Firebase Firestore)" else "Cloud Data Sync (Firestore)",
                subtitle = if (isHi) "शाला डेटा को केंद्रीय क्लाउड में तुरंत सिंक करें (अंतिम सिंक: ${viewModel.syncState.value.lastSyncFormatted})"
                else "Real-time sync to central cloud database (Last: ${viewModel.syncState.value.lastSyncFormatted})",
                icon = Icons.Default.CloudSync,
                onClick = {
                    viewModel.syncDataNow()
                    Toast.makeText(
                        context,
                        if (isHi) "क्लाउड डेटा सिंक शुरू किया गया..." else "Starting cloud sync...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER) {
                Text(
                    text = if (isHi) "क्लाउड बैकअप एवं डेटा सुरक्षा (Cloud Backup)" else "Cloud Backup & Data Security",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                MenuNavigationCard(
                    title = if (isHi) "Google Drive क्लाउड बैकअप इंजन" else "Google Drive Cloud Backup Engine",
                    subtitle = if (isHi) "शाला डेटा का स्वचालित Google Drive बैकअप, रीस्टोर एवं लोकल एक्सपोर्ट" else "Automated Google Drive backup, atomic restore & local export",
                    icon = Icons.Default.CloudSync,
                    onClick = onNavigateToGoogleDriveBackup
                )
            }

            // AUDIT TRAIL SECTION (Only Headmaster)
            if (currentUserRole == com.example.data.local.entity.UserRole.HEADMASTER) {
                Text(
                    text = if (isHi) "ऑडिट एवं सुरक्षा लॉग्स" else "Audit Trail & Logs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                MenuNavigationCard(
                    title = if (isHi) "ऑडिट ट्रेल लॉग्स (Audit Trail)" else "Audit Trail Logs",
                    subtitle = if (isHi) "समस्त प्रविष्टियों एवं संपादन का सुरक्षित इतिहास (${auditLogs.size} लॉग्स)" else "Secure history of all changes (${auditLogs.size} logs)",
                    icon = Icons.Default.History,
                    onClick = { showAuditLogsDialog = true }
                )
            }

            // DEDICATED LOGOUT & LOCK ACTION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHi) "लॉगआउट एवं ऐप लॉक (Sign Out)" else "Logout & Lock Application",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = if (isHi) "सत्र समाप्त करें और 4-अंकीय पिन सुरक्षा सक्रिय करें" else "End current session and lock with 4-digit PIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // APP BRANDING & LOGO EMBLEM CARD
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, BluePrimary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cg_mdm_final_logo),
                            contentDescription = "CG MDM Manager Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CG MDM Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isHi) "“बच्चों का पोषण, डिजिटल प्रबंधन के साथ।”\nChild nutrition, powered by digital management." else "Child nutrition, powered by digital management.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Developed by - E Kosh Tech Solutions",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "E-Mail : krish.7m@gmail.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // SCHOOL SETTINGS DIALOG
    if (showSchoolDialog) {
        EditSchoolDialog(
            existing = school,
            isHi = isHi,
            onDismiss = { showSchoolDialog = false },
            onSave = { updatedSchool ->
                viewModel.saveSchool(updatedSchool)
                android.widget.Toast.makeText(
                    context,
                    if (isHi) "विद्यालय प्रोफाइल एवं प्रधान पाठक विवरण अपडेट किया गया!" else "School profile & Headmaster updated!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showSchoolDialog = false
            }
        )
    }

    // REIMBURSEMENT RATE DIALOG
    if (showReimbursementDialog) {
        EditReimbursementRateDialog(
            existing = configNorms,
            isHi = isHi,
            onDismiss = { showReimbursementDialog = false },
            onSave = { updatedNorms ->
                viewModel.saveConfigNorms(updatedNorms)
                showReimbursementDialog = false
            }
        )
    }

    // NORMS CONFIG DIALOG
    if (showNormsDialog) {
        EditNormsDialog(
            existing = configNorms,
            isHi = isHi,
            onDismiss = { showNormsDialog = false },
            onSave = {
                viewModel.saveConfigNorms(it)
                showNormsDialog = false
            }
        )
    }

    // AUDIT LOGS DIALOG
    if (showAuditLogsDialog) {
        AuditLogsDialog(
            logs = auditLogs,
            isHi = isHi,
            onDismiss = { showAuditLogsDialog = false }
        )
    }

    // DAILY REMINDER & ALERTS SETTINGS DIALOG
    if (showReminderDialog) {
        DailyReminderSettingsDialog(
            isHi = isHi,
            onDismiss = { showReminderDialog = false }
        )
    }

    // ADMINISTRATION OFFICE SETUP DIALOG
    if (showAdminOfficeDialog) {
        AdministrationOfficeSetupDialog(
            existing = configNorms,
            isHi = isHi,
            onDismiss = { showAdminOfficeDialog = false },
            onSave = { updated ->
                viewModel.saveConfigNorms(updated)
                showAdminOfficeDialog = false
            }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CategoryChip(text: String) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TeacherStatBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun MenuNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTeacherDialog(
    monthYear: String,
    existing: MonthlyTeacherEntity?,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (MonthlyTeacherEntity) -> Unit
) {
    var maleText by remember { mutableStateOf(existing?.maleCount?.toString() ?: "6") }
    var femaleText by remember { mutableStateOf(existing?.femaleCount?.toString() ?: "8") }
    var trainedText by remember { mutableStateOf(existing?.trainedCount?.toString() ?: "13") }
    var untrainedText by remember { mutableStateOf(existing?.untrainedCount?.toString() ?: "1") }

    val male = maleText.toIntOrNull() ?: 0
    val female = femaleText.toIntOrNull() ?: 0
    val total = male + female
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
            Text(
                text = if (isHi) "मासिक शिक्षक विवरण ($monthYear)" else "Monthly Teacher Census ($monthYear)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = maleText,
                    onValueChange = { maleText = it },
                    label = { Text(if (isHi) "पुरुष शिक्षक (Male)" else "Male Teachers") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = femaleText,
                    onValueChange = { femaleText = it },
                    label = { Text(if (isHi) "महिला शिक्षक (Female)" else "Female Teachers") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = trainedText,
                    onValueChange = { trainedText = it },
                    label = { Text(if (isHi) "प्रशिक्षित शिक्षक (Trained)" else "Trained Teachers") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = untrainedText,
                    onValueChange = { untrainedText = it },
                    label = { Text(if (isHi) "अप्रशिक्षित शिक्षक (Untrained)" else "Untrained Teachers") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHi) "कुल शिक्षक संख्या: $total" else "Total Teachers: $total",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val entity = MonthlyTeacherEntity(
                        monthYear = monthYear,
                        schoolId = existing?.schoolId ?: "SCH-CG-RPR-001",
                        academicYear = existing?.academicYear ?: "2026-27",
                        totalTeachers = total,
                        maleCount = male,
                        femaleCount = female,
                        trainedCount = trainedText.toIntOrNull() ?: 0,
                        untrainedCount = untrainedText.toIntOrNull() ?: 0,
                        scCount = existing?.scCount ?: 2,
                        stCount = existing?.stCount ?: 3,
                        obcCount = existing?.obcCount ?: 6,
                        generalCount = existing?.generalCount ?: 3,
                        updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    )
                    onSave(entity)
                },
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Text(if (isHi) "सुरक्षित करें" else "Save", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditNormsDialog(
    existing: ConfigNormsEntity?,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (ConfigNormsEntity) -> Unit
) {
    var primaryNorm by remember {
        val norm = existing?.primaryRiceNormGrams ?: 150.0
        val effective = if (norm == 100.0 || norm == 110.0) 150.0 else norm
        mutableStateOf(effective.toString())
    }
    var upperPrimaryNorm by remember { mutableStateOf((existing?.upperPrimaryRiceNormGrams ?: 150.0).toString()) }
    var bufferDays by remember { mutableStateOf((existing?.goodStockThresholdDays ?: 15).toString()) }
    var criticalDays by remember { mutableStateOf((existing?.lowStockThresholdDays ?: 5).toString()) }
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
            Text(
                text = if (isHi) "खाद्यान्न मान एवं थ्रेशोल्ड नियम" else "Food Grain Norms & Thresholds",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = primaryNorm,
                    onValueChange = { primaryNorm = it },
                    label = { Text(if (isHi) "प्राथमिक शाला चावल मान (ग्राम/छात्र)" else "Primary Rice Norm (g/student)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = upperPrimaryNorm,
                    onValueChange = { upperPrimaryNorm = it },
                    label = { Text(if (isHi) "पूर्व माध्यमिक चावल मान (ग्राम/छात्र)" else "Upper Primary Rice Norm (g/student)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bufferDays,
                    onValueChange = { bufferDays = it },
                    label = { Text(if (isHi) "पर्याप्त स्टॉक बफर थ्रेशोल्ड (दिवस)" else "Stock Buffer Days Threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = criticalDays,
                    onValueChange = { criticalDays = it },
                    label = { Text(if (isHi) "क्रिटिकल स्टॉक चेतावनी (दिवस)" else "Critical Stock Days Alert") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val updated = ConfigNormsEntity(
                        configId = existing?.configId ?: "DEFAULT_CG_CONFIG",
                        stateName = existing?.stateName ?: "Chhattisgarh",
                        primaryRiceNormGrams = primaryNorm.toDoubleOrNull() ?: 100.0,
                        upperPrimaryRiceNormGrams = upperPrimaryNorm.toDoubleOrNull() ?: 150.0,
                        pulseNormGrams = existing?.pulseNormGrams ?: 30.0,
                        oilNormGrams = existing?.oilNormGrams ?: 7.5,
                        vegetableNormGrams = existing?.vegetableNormGrams ?: 75.0,
                        saltNormGrams = existing?.saltNormGrams ?: 5.0,
                        reimbursementRate = existing?.reimbursementRate ?: 10.17,
                        cookingCostRate = existing?.cookingCostRate ?: 10.17,
                        lowStockThresholdKg = existing?.lowStockThresholdKg ?: 50.0,
                        goodStockThresholdDays = bufferDays.toIntOrNull() ?: 15,
                        lowStockThresholdDays = criticalDays.toIntOrNull() ?: 5,
                        customItemsJson = existing?.customItemsJson ?: ""
                    )
                    onSave(updated)
                },
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Text(if (isHi) "नियम लागू करें" else "Apply Norms", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel")
            }
        }
    )
}

@Composable
private fun AuditLogsDialog(
    logs: List<com.example.data.local.entity.AuditLogEntity>,
    isHi: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isHi) "ऑडिट ट्रेल लॉग्स (Audit History)" else "Audit Trail History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
        },
        text = {
            if (logs.isEmpty()) {
                Text(
                    text = if (isHi) "कोई ऑडिट लॉग उपलब्ध नहीं है।" else "No audit logs available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                    Text(
                                        text = log.moduleName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "द्वारा: ${log.userRole}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Text(if (isHi) "बंद करें" else "Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

@Composable
private fun SchoolInfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSchoolDialog(
    existing: SchoolEntity?,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (SchoolEntity) -> Unit
) {
    var schoolName by remember(existing) { mutableStateOf(existing?.schoolName ?: "") }
    var udiseCode by remember(existing) { mutableStateOf(existing?.udiseCode ?: "") }
    var villageName by remember(existing) { mutableStateOf(existing?.villageName ?: "") }
    var clusterName by remember(existing) { mutableStateOf(existing?.clusterName ?: "") }
    var districtName by remember(existing) { mutableStateOf(existing?.districtName ?: "") }
    var blockName by remember(existing) { mutableStateOf(existing?.blockName ?: "") }
    var stateName by remember(existing) { mutableStateOf(existing?.stateName ?: "छत्तीसगढ़") }
    var schoolType by remember(existing) { mutableStateOf(existing?.schoolType ?: "Primary only (1 to 5)") }
    var headTeacherName by remember(existing) { mutableStateOf(existing?.headTeacherName ?: "") }
    var headTeacherMobile by remember(existing) { mutableStateOf(existing?.headTeacherMobile ?: "") }

    var expandedDistrict by remember { mutableStateOf(false) }
    var expandedBlock by remember { mutableStateOf(false) }
    var expandedSchoolType by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Find current district info to get matching blocks
    val currentDistrictInfo = LocationData.CHHATTISGARH_DISTRICTS.find {
        it.nameHi.equals(districtName, ignoreCase = true) ||
        it.nameEn.equals(districtName, ignoreCase = true) ||
        districtName.contains(it.nameEn, ignoreCase = true) ||
        districtName.contains(it.nameHi, ignoreCase = true) ||
        it.nameEn.contains(districtName, ignoreCase = true) ||
        it.nameHi.contains(districtName, ignoreCase = true)
    } ?: LocationData.CHHATTISGARH_DISTRICTS.first()

    val availableBlocks = currentDistrictInfo.blocks

    val isUdiseValid = udiseCode.length == 11
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.School, contentDescription = null, tint = BluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHi) "विद्यालय सेटिंग्स एवं प्रोफाइल संपादित करें" else "Edit School Settings & Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. School Name
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text(if (isHi) "शाला का नाम (School Name) *" else "School Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. UDISE Code with 11-digit validation
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = udiseCode,
                        onValueChange = { input ->
                            // Filter only digits and max 11 digits
                            val digitsOnly = input.filter { it.isDigit() }.take(11)
                            udiseCode = digitsOnly
                            if (validationError != null && digitsOnly.length == 11) {
                                validationError = null
                            }
                        },
                        label = { Text(if (isHi) "UDISE कोड (11 अंक) *" else "UDISE Code (11 Digits) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            if (isUdiseValid) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid UDISE",
                                    tint = Color(0xFF166534)
                                )
                            } else {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "${udiseCode.length}/11",
                                        color = StatusCriticalRed,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        isError = !isUdiseValid && udiseCode.isNotEmpty(),
                        supportingText = {
                            if (isUdiseValid) {
                                Text(
                                    text = if (isHi) "✓ 11 अंकों का मान्य UDISE कोड दर्ज है" else "✓ Valid 11-digit UDISE code",
                                    color = Color(0xFF166534),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = if (isHi)
                                        "11 अंकों का UDISE कोड अनिवार्य है (दर्ज: ${udiseCode.length}/11, शेष: ${11 - udiseCode.length})"
                                    else
                                        "11 digits required (Entered: ${udiseCode.length}/11, Remaining: ${11 - udiseCode.length})",
                                    color = StatusCriticalRed,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. Village / Address
                OutlinedTextField(
                    value = villageName,
                    onValueChange = { villageName = it },
                    label = { Text(if (isHi) "ग्राम / पता (Village / Address) *" else "Village / Address *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Cluster Name (Placed BEFORE Block name as requested)
                OutlinedTextField(
                    value = clusterName,
                    onValueChange = { clusterName = it },
                    label = { Text(if (isHi) "संकुल (Cluster) *" else "Cluster Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. District Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDistrict,
                    onExpandedChange = { expandedDistrict = !expandedDistrict },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = districtName,
                        onValueChange = { districtName = it },
                        readOnly = true,
                        label = { Text(if (isHi) "जिला (District) *" else "District *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistrict) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDistrict,
                        onDismissRequest = { expandedDistrict = false },
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        LocationData.CHHATTISGARH_DISTRICTS.forEach { district ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${district.nameHi} (${district.nameEn})",
                                        fontWeight = if (districtName == district.nameHi || districtName == district.nameEn) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    districtName = if (isHi) district.nameHi else district.nameEn
                                    // Update block to the first block of the newly chosen district
                                    if (district.blocks.isNotEmpty()) {
                                        blockName = if (isHi) district.blocks.first().nameHi else district.blocks.first().nameEn
                                    }
                                    expandedDistrict = false
                                }
                            )
                        }
                    }
                }

                // 6. Block Dropdown (Respective blocks from selected district)
                ExposedDropdownMenuBox(
                    expanded = expandedBlock,
                    onExpandedChange = { expandedBlock = !expandedBlock },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = blockName,
                        onValueChange = { blockName = it },
                        readOnly = true,
                        label = { Text(if (isHi) "विकासखंड (Block) *" else "Block *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBlock) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBlock,
                        onDismissRequest = { expandedBlock = false },
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        availableBlocks.forEach { block ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${block.nameHi} (${block.nameEn})",
                                        fontWeight = if (blockName == block.nameHi || blockName == block.nameEn) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    blockName = if (isHi) block.nameHi else block.nameEn
                                    expandedBlock = false
                                }
                            )
                        }
                    }
                }

                // 7. State
                OutlinedTextField(
                    value = stateName,
                    onValueChange = { stateName = it },
                    label = { Text(if (isHi) "राज्य (State) *" else "State *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 8. School Type Dropdown (Primary 1-5, Middle school 6-8, Middle with primary 1-8)
                ExposedDropdownMenuBox(
                    expanded = expandedSchoolType,
                    onExpandedChange = { expandedSchoolType = !expandedSchoolType },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when {
                            schoolType.contains("Primary 1-5", ignoreCase = true) || schoolType.contains("प्राथमिक 1-5", ignoreCase = true) ->
                                if (isHi) "प्राथमिक (Primary 1-5)" else "Primary 1-5"
                            schoolType.contains("Middle school 6-8", ignoreCase = true) || schoolType.contains("पूर्व माध्यमिक 6-8", ignoreCase = true) ->
                                if (isHi) "पूर्व माध्यमिक (Middle school 6-8)" else "Middle school 6-8"
                            else ->
                                if (isHi) "प्राथमिक सह पूर्व माध्यमिक (Middle with primary 1-8)" else "Middle with primary 1-8"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isHi) "शाला का प्रकार / स्तर (School Type) *" else "School Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSchoolType) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSchoolType,
                        onDismissRequest = { expandedSchoolType = false }
                    ) {
                        LocationData.SCHOOL_TYPES.forEach { (typeKey, typeLabel) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = typeKey,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                        Text(
                                            text = typeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                },
                                onClick = {
                                    schoolType = typeKey
                                    expandedSchoolType = false
                                }
                            )
                        }
                    }
                }

                // 9. Head Teacher Name & Mobile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = headTeacherName,
                        onValueChange = { headTeacherName = it },
                        label = { Text(if (isHi) "प्रधान पाठक का नाम" else "Head Teacher Name") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = headTeacherMobile,
                        onValueChange = { headTeacherMobile = it },
                        label = { Text(if (isHi) "मोबाइल नंबर" else "Mobile") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Validation Error alert
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
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (udiseCode.length != 11) {
                        validationError = if (isHi)
                            "UDISE कोड अनिवार्य रूप से ठीक 11 अंकों का होना चाहिए (वर्तमान: ${udiseCode.length} अंक)।"
                        else
                            "UDISE code must be exactly 11 digits (Current: ${udiseCode.length} digits)."
                        return@Button
                    }
                    if (schoolName.isBlank()) {
                        validationError = if (isHi) "कृपया शाला का नाम दर्ज करें।" else "Please enter School Name."
                        return@Button
                    }

                    validationError = null

                    val updated = SchoolEntity(
                        schoolId = existing?.schoolId ?: "SCH-CG-KBD-001",
                        udiseCode = udiseCode.trim(),
                        schoolName = schoolName.trim(),
                        stateName = stateName.trim(),
                        districtName = districtName.trim(),
                        blockName = blockName.trim(),
                        clusterName = clusterName.trim(),
                        villageName = villageName.trim(),
                        schoolType = schoolType.trim(),
                        headTeacherName = headTeacherName.trim(),
                        headTeacherMobile = headTeacherMobile.trim(),
                        status = existing?.status ?: "ACTIVE"
                    )
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onSave(updated)
                },
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Text(if (isHi) "सुरक्षित करें" else "Save Settings", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReimbursementRateDialog(
    existing: ConfigNormsEntity?,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (ConfigNormsEntity) -> Unit
) {
    var primaryRateText by remember(existing) {
        mutableStateOf(
            if (existing != null && existing.primaryReimbursementRate > 0.0)
                existing.primaryReimbursementRate.toString()
            else ""
        )
    }
    var middleRateText by remember(existing) {
        mutableStateOf(
            if (existing != null && existing.middleReimbursementRate > 0.0)
                existing.middleReimbursementRate.toString()
            else ""
        )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BluePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isHi) "प्रतिपूर्ति राशि दर" else "Reimbursement Amount Rate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (isHi) "प्राथमिक एवं पूर्व माध्यमिक स्तर" else "Primary & Middle School Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Primary School Level (Class 1-5) Input Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isHi) "प्राथमिक स्तर (कक्षा 1 से 5) *" else "Primary Level (Class 1 to 5) *",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    OutlinedTextField(
                        value = primaryRateText,
                        onValueChange = { primaryRateText = it },
                        placeholder = { Text(if (isHi) "दर दर्ज करें" else "Enter rate") },
                        prefix = {
                            Text(
                                "₹ ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BluePrimary
                            )
                        },
                        suffix = {
                            Text(
                                if (isHi) "/ छात्र / दिन" else "/ student / day",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                // Middle School Level (Class 6-8) Input Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isHi) "पूर्व माध्यमिक / मिडिल स्तर (कक्षा 6 से 8) *" else "Middle / Upper Primary Level (Class 6 to 8) *",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    OutlinedTextField(
                        value = middleRateText,
                        onValueChange = { middleRateText = it },
                        placeholder = { Text(if (isHi) "दर दर्ज करें" else "Enter rate") },
                        prefix = {
                            Text(
                                "₹ ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BluePrimary
                            )
                        },
                        suffix = {
                            Text(
                                if (isHi) "/ छात्र / दिन" else "/ student / day",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHi) "💡 यह दर दैनिक भोजन लागत, सामग्री मान एवं मासिक प्रतिपूर्ति बिल की स्वतः गणना में उपयोग की जाएगी।" else "💡 These rates will be automatically used for calculating daily cooking costs, materials, and monthly reimbursement bills.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1E40AF),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val pRate = primaryRateText.toDoubleOrNull() ?: 0.0
                    val mRate = middleRateText.toDoubleOrNull() ?: 0.0
                    val updated = (existing ?: ConfigNormsEntity()).copy(
                        primaryReimbursementRate = pRate,
                        middleReimbursementRate = mRate,
                        reimbursementRate = mRate,
                        cookingCostRate = mRate
                    )
                    onSave(updated)
                },
                colors = poshanButtonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isHi) "दर सुरक्षित करें" else "Save Rates", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReminderSettingsDialog(
    isHi: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var enabled by remember { mutableStateOf(ReminderManager.isReminderEnabled(context)) }
    var selectedHour by remember { mutableStateOf(ReminderManager.getReminderHour(context)) }
    var selectedMinute by remember { mutableStateOf(ReminderManager.getReminderMinute(context)) }
    var soundEnabled by remember { mutableStateOf(ReminderManager.isSoundEnabled(context)) }
    var vibrateEnabled by remember { mutableStateOf(ReminderManager.isVibrateEnabled(context)) }
    var secondEnabled by remember { mutableStateOf(ReminderManager.isSecondReminderEnabled(context)) }
    var secondHour by remember { mutableStateOf(ReminderManager.getSecondReminderHour(context)) }
    var secondMinute by remember { mutableStateOf(ReminderManager.getSecondReminderMinute(context)) }

    // Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(
                context,
                if (isHi) "सूचना अनुमति प्राप्त हुई!" else "Notification permission granted!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isHi) "दैनिक उपस्थिति रिमाइंडर व अलार्म" else "Daily Attendance Reminders",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // MASTER REMINDER SWITCH CARD
                Surface(
                    color = if (enabled) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (enabled) BluePrimary.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                    ),
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
                                text = if (isHi) "दैनिक रिमाइंडर सक्रिय करें" else "Enable Daily Reminders",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isHi) "प्रत्येक कार्यदिवस स्वतः रिमाइंडर अलार्म बजेगा" else "Daily alert to log meal & attendance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BluePrimary
                            )
                        )
                    }
                }

                if (enabled) {
                    // PRIMARY REMINDER TIME SELECTOR
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHi) "प्राथमिक समय (Primary Time)" else "Primary Reminder Time",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Surface(
                                    color = BluePrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val amPm = if (selectedHour >= 12) "PM" else "AM"
                                    val displayHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                                    Text(
                                        text = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BluePrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isHi) "त्वरित समय चुनें (Quick Presets):" else "Quick Select Time:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Time presets chips
                            val presets = listOf(
                                Pair(11, 30) to "11:30 AM",
                                Pair(12, 0) to "12:00 PM",
                                Pair(12, 30) to "12:30 PM",
                                Pair(13, 0) to "01:00 PM",
                                Pair(13, 30) to "01:30 PM",
                                Pair(14, 0) to "02:00 PM"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    presets.take(3).forEach { (time, label) ->
                                        val isSel = selectedHour == time.first && selectedMinute == time.second
                                        FilterChip(
                                            selected = isSel,
                                            onClick = {
                                                selectedHour = time.first
                                                selectedMinute = time.second
                                            },
                                            label = { Text(label, fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            colors = poshanFilterChipColors()
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    presets.drop(3).forEach { (time, label) ->
                                        val isSel = selectedHour == time.first && selectedMinute == time.second
                                        FilterChip(
                                            selected = isSel,
                                            onClick = {
                                                selectedHour = time.first
                                                selectedMinute = time.second
                                            },
                                            label = { Text(label, fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            colors = poshanFilterChipColors()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECONDARY / AFTERNOON FOLLOW-UP REMINDER
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier.fillMaxWidth()
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
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = if (isHi) "दोपहर अंतिम चेतावनी (Follow-up)" else "Afternoon Follow-up",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (isHi) "यदि दोपहर तक उपस्थिति दर्ज न हुई हो" else "Alerts only if not logged yet",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Switch(
                                    checked = secondEnabled,
                                    onCheckedChange = { secondEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFEA580C)
                                    )
                                )
                            }

                            if (secondEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val secondPresets = listOf(
                                    Pair(14, 30) to "02:30 PM",
                                    Pair(15, 0) to "03:00 PM",
                                    Pair(15, 30) to "03:30 PM",
                                    Pair(16, 0) to "04:00 PM"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    secondPresets.forEach { (time, label) ->
                                        val isSel = secondHour == time.first && secondMinute == time.second
                                        FilterChip(
                                            selected = isSel,
                                            onClick = {
                                                secondHour = time.first
                                                secondMinute = time.second
                                            },
                                            label = { Text(label, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            colors = poshanFilterChipColors()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ALERT PREFERENCES: SOUND & VIBRATION
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isHi) "अलर्ट विकल्प (Alert Sound & Vibration)" else "Sound & Vibration",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF0F172A)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHi) "ध्वनि / अलार्म टोन" else "Sound / Alarm Tone", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHi) "कंपन (Vibration)" else "Vibration", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = vibrateEnabled, onCheckedChange = { vibrateEnabled = it })
                            }
                        }
                    }

                    // TEST NOTIFICATION BUTTON
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            ReminderManager.showTestNotification(context, isHi)
                            Toast.makeText(
                                context,
                                if (isHi) "🔔 टेस्ट नोटिफिकेशन भेजा गया!" else "🔔 Test notification sent!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "🔔 टेस्ट नोटिफिकेशन तुरंत भेजें" else "🔔 Send Test Notification Now",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // SMART LOGIC EXPLANATION
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
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHi) "💡 स्मार्ट अलार्म: यदि आज की भोजन एवं छात्र उपस्थिति पहले ही दर्ज हो चुकी है, तो रिमाइंडर स्वतः मौन रहेगा और नहीं बजेगा।"
                                else "💡 Smart Alert: If today's meal and attendance is already submitted, the reminder automatically stays silent.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    ReminderManager.saveSettings(
                        context = context,
                        enabled = enabled,
                        hour = selectedHour,
                        minute = selectedMinute,
                        sound = soundEnabled,
                        vibrate = vibrateEnabled,
                        secondEnabled = secondEnabled,
                        secondHour = secondHour,
                        secondMinute = secondMinute
                    )
                    Toast.makeText(
                        context,
                        if (isHi) "रिमाइंडर सेटिंग्स सुरक्षित की गईं!" else "Reminder settings saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                },
                colors = poshanButtonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isHi) "सेटिंग्स सेव करें" else "Save Settings",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isHi) "बंद करें" else "Close")
            }
        }
    )
}

@Composable
fun AdministrationOfficeSetupDialog(
    existing: ConfigNormsEntity?,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (ConfigNormsEntity) -> Unit
) {
    val context = LocalContext.current
    var officeName by remember(existing) {
        mutableStateOf(existing?.adminOfficeName?.ifBlank { "Block Education Officer" } ?: "Block Education Officer")
    }
    var whatsappNumber by remember(existing) {
        mutableStateOf(existing?.adminOfficeWhatsapp ?: "")
    }
    var emailAddress by remember(existing) {
        mutableStateOf(existing?.adminOfficeEmail ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = Color(0xFF1D4ED8),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isHi) "प्रशासनिक कार्यालय सेटअप" else "Administration Office Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isHi) "मासिक प्रतिवेदन व्हाट्सएप एवं ईमेल प्रेषण हेतु" else "For sending monthly reports via WhatsApp & Email",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi)
                                "यह मोबाइल नंबर एवं ईमेल मासिक प्रतिवेदन प्रेषित करने हेतु उपयोग किए जाएंगे। समस्त प्रविष्टियां (Format 1 व 2) एक साथ भेजी जाएंगी।"
                            else
                                "This mobile and email will be used to dispatch monthly reports. All entry reports will be sent at once.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }

                // Office Name
                OutlinedTextField(
                    value = officeName,
                    onValueChange = { officeName = it },
                    label = { Text(if (isHi) "कार्यालय का नाम" else "Office Name") },
                    placeholder = { Text("Block Education Officer") },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF1D4ED8))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // WhatsApp Mobile Number
                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = {
                        if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                            whatsappNumber = it
                        }
                    },
                    label = { Text(if (isHi) "व्हाट्सएप मोबाइल नंबर" else "WhatsApp Mobile Number") },
                    prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)) },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF16A34A))
                    },
                    placeholder = { Text("10-अंकीय मोबाइल नंबर") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Email Address
                OutlinedTextField(
                    value = emailAddress,
                    onValueChange = { emailAddress = it.trim() },
                    label = { Text(if (isHi) "ई-मेल पता (E-mail Address)" else "E-mail Address") },
                    placeholder = { Text("beo.office@cg.gov.in") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFEA580C))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val current = existing ?: ConfigNormsEntity()
                    val updated = current.copy(
                        adminOfficeName = officeName.ifBlank { "Block Education Officer" },
                        adminOfficeWhatsapp = whatsappNumber,
                        adminOfficeEmail = emailAddress
                    )
                    onSave(updated)
                    Toast.makeText(
                        context,
                        if (isHi) "प्रशासनिक कार्यालय विवरण सुरक्षित किया गया!" else "Administration office details saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHi) "विवरण सुरक्षित करें" else "Save Details",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isHi) "रद्द करें" else "Cancel")
            }
        }
    )
}



