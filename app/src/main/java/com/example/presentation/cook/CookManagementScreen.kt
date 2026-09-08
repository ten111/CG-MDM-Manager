package com.example.presentation.cook

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CookEntity
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.MaskedAccountDisplay
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.common.StatusBadge
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookManagementScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val allCooks by viewModel.allCooks.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCookForEdit by remember { mutableStateOf<CookEntity?>(null) }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "रसोइया प्रबंधन" else "Cook-cum-Helper Management",
                subtitle = if (isHi) "रसोइया मास्टर, बैंक विवरण एवं स्थिति" else "Cook Master, Bank Details & Status",
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() },
                onSyncClick = {}
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "रसोइया जोड़ें (Add Cook)", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            // Header stats
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "कुल पंजीकृत रसोइया",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "${allCooks.size} रसोइया",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val activeCount = allCooks.count { it.status == "ACTIVE" }
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "🟢 सक्रिय: $activeCount",
                                color = StatusGoodGreen,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allCooks) { cook ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(BluePrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cook.name.ifEmpty { "रसोइया" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "पति/पिता: ${cook.guardianName.ifEmpty { "—" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Normal,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }

                                StatusBadge(status = cook.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📞 ${cook.mobile}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "समूह: ${cook.associatedAgency}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Protected / Masked Bank Details
                            MaskedAccountDisplay(
                                maskedAccount = cook.maskedAccountNo.ifEmpty { "-" },
                                ifsc = cook.ifscCode.ifEmpty { "-" },
                                bankName = cook.bankName.ifEmpty { "-" }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.toggleCookStatus(cook.cookId, cook.status)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (cook.status == "ACTIVE") "निष्क्रिय करें" else "सक्रिय करें",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { selectedCookForEdit = cook },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("संपादित करें", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditCookDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveCook(it)
                showAddDialog = false
            }
        )
    }

    if (selectedCookForEdit != null) {
        AddEditCookDialog(
            existing = selectedCookForEdit,
            onDismiss = { selectedCookForEdit = null },
            onSave = {
                viewModel.saveCook(it)
                selectedCookForEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCookDialog(
    existing: CookEntity?,
    onDismiss: () -> Unit,
    onSave: (CookEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var guardianName by remember { mutableStateOf(existing?.guardianName ?: "") }
    var mobile by remember { mutableStateOf(existing?.mobile ?: "") }
    var village by remember { mutableStateOf(existing?.village ?: "") }
    var bankAccountNo by remember { mutableStateOf(existing?.bankAccountNo ?: "") }
    var bankName by remember { mutableStateOf(existing?.bankName ?: "") }
    var branchName by remember { mutableStateOf(existing?.branchName ?: "") }
    var ifscCode by remember { mutableStateOf(existing?.ifscCode ?: "") }
    var agencyName by remember { mutableStateOf(existing?.associatedAgency ?: "") }
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
                text = if (existing == null) "नया रसोइया जोड़ें (Add Cook)" else "रसोइया विवरण संपादित करें",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
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
                    label = { Text("रसोइया का नाम (Cook Name)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = { Text("पति/पिता का नाम") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाइल नंबर") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("ग्राम / वार्ड") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider()
                Text("सुरक्षित बैंक खाता विवरण (Bank Details)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreenPrimary)

                OutlinedTextField(
                    value = bankAccountNo,
                    onValueChange = { bankAccountNo = it },
                    label = { Text("बैंक खाता नंबर (पूर्ण)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("बैंक का नाम") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text("शाखा") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ifscCode,
                        onValueChange = { ifscCode = it },
                        label = { Text("IFSC कोड") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = agencyName,
                    onValueChange = { agencyName = it },
                    label = { Text("संबद्ध स्व-सहायता समूह / एजेंसी") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (name.isNotEmpty()) {
                        val cook = CookEntity(
                            cookId = existing?.cookId ?: ("COOK-" + (100..999).random()),
                            name = name,
                            guardianName = guardianName,
                            mobile = mobile,
                            village = village,
                            bankAccountNo = bankAccountNo,
                            bankName = bankName,
                            branchName = branchName,
                            ifscCode = ifscCode,
                            associatedAgency = agencyName,
                            status = existing?.status ?: "ACTIVE"
                        )
                        onSave(cook)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("रद्द करें") }
        }
    )
}
