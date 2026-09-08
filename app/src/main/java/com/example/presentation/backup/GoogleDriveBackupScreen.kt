package com.example.presentation.backup

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloud.*
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveBackupScreen(
    onNavigateBack: () -> Unit = {},
    poshanViewModel: PoshanViewModel? = null,
    backupViewModel: GoogleDriveBackupViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val accountInfo by backupViewModel.accountInfo.collectAsState()
    val autoBackupConfig by backupViewModel.autoBackupConfig.collectAsState()
    val cloudBackups by backupViewModel.cloudBackups.collectAsState()
    val statusMessage by backupViewModel.statusMessage.collectAsState()
    val progress by backupViewModel.progress.collectAsState()
    val userMessage by backupViewModel.userMessage.collectAsState()
    val isOperationInProgress by backupViewModel.isOperationInProgress.collectAsState()

    var showRestoreDialogFor by remember { mutableStateOf<PoshanBackupMetadata?>(null) }
    var showDetailsDialogFor by remember { mutableStateOf<PoshanBackupMetadata?>(null) }
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var showPermissionRequestDialog by remember { mutableStateOf(false) }
    var pendingAccountEmail by remember { mutableStateOf("") }
    var pendingAccountName by remember { mutableStateOf("") }

    val currentLanguage = poshanViewModel?.currentLanguage?.collectAsState()?.value ?: AppLanguage.HINDI
    val isHi = currentLanguage == AppLanguage.HINDI

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            backupViewModel.clearUserMessage()
        }
    }

    // Save JSON Document directly
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            backupViewModel.saveBackupToUri(it) { success, msg ->
                if (success) {
                    Toast.makeText(context, msg ?: if (isHi) "बैकअप सहेजा गया!" else "Backup saved!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Android Google Account Picker
    val chooseAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                pendingAccountEmail = accountName
                pendingAccountName = accountName.substringBefore("@").replace(".", " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
                showSwitchAccountDialog = false
                showPermissionRequestDialog = true
            }
        }
    }

    // Restore from JSON File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonString = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (!jsonString.isNullOrBlank()) {
                    backupViewModel.restoreFromLocalJson(jsonString) { success ->
                        if (success) {
                            poshanViewModel?.refreshAllData()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "Google Drive बैकअप" else "Google Drive Backup",
                subtitle = if (isHi) "क्लाउड बैकअप एवं डेटा सुरक्षा" else "Cloud Sync & Restore",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { backupViewModel.refresh() },
                        modifier = Modifier.testTag("backup_refresh_button").size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isHi) "ताज़ा करें" else "Refresh",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            // 1. Google Account & Storage Status Card (Minimal)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CloudQueue,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = accountInfo.displayName.ifBlank { "Google Drive" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = accountInfo.email.ifBlank { "Not Connected" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            AssistChip(
                                onClick = {
                                    pendingAccountEmail = accountInfo.email
                                    pendingAccountName = accountInfo.displayName
                                    showSwitchAccountDialog = true
                                },
                                label = {
                                    Text(
                                        text = if (isHi) "बदलें" else "Switch",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Storage & Sync Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (accountInfo.isConnected) GreenPrimary else Color(0xFFE65100))
                                )
                                Text(
                                    text = if (accountInfo.isConnected) {
                                        if (isHi) "ड्राइव लिंक सक्रिय" else "Drive Active"
                                    } else {
                                        if (isHi) "कनेक्ट नहीं है" else "Disconnected"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (accountInfo.isConnected) GreenPrimary else Color(0xFFE65100)
                                )
                            }

                            Text(
                                text = if (accountInfo.lastSyncedAt.isNotBlank()) {
                                    if (isHi) "अंतिम सिंक: ${accountInfo.lastSyncedAt}" else "Last: ${accountInfo.lastSyncedAt}"
                                } else {
                                    if (isHi) "सिंक नहीं हुआ" else "Never synced"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Progress Indicator during backup/restore
            if (isOperationInProgress) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = statusMessage.ifEmpty { if (isHi) "प्रक्रिया जारी है..." else "Processing..." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }

            // 2. Main Primary Action: Backup Now Button
            item {
                Button(
                    onClick = {
                        if (!accountInfo.isPermissionGranted) {
                            pendingAccountEmail = accountInfo.email
                            pendingAccountName = accountInfo.displayName
                            showPermissionRequestDialog = true
                        } else {
                            backupViewModel.performBackupNow(
                                createdBy = poshanViewModel?.currentUser?.value?.name ?: poshanViewModel?.school?.value?.headTeacherName ?: "Headmaster"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("button_backup_now"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isOperationInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHi) "Google Drive में बैकअप लें (Backup Now)" else "Backup to Google Drive Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // 3. Secondary Actions Row (Save JSON & Restore JSON)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            createDocumentLauncher.launch(backupViewModel.getSuggestedFileName())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("button_save_file_manager"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isOperationInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "फ़ाइल सहेजें (Save)" else "Save JSON",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("application/json") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("button_restore_file"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isOperationInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "फ़ाइल से रीस्टोर" else "Restore JSON",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // 4. Automatic Backup & Smart Restore Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section Header & Master Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (autoBackupConfig.enabled) GreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (autoBackupConfig.enabled) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isHi) "स्वचालित बैकअप सेटिंग्स" else "Auto-Backup Settings",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isHi) "समयबद्ध बैकअप एवं त्वरित रीस्टोर" else "Scheduled sync & quick restore",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = autoBackupConfig.enabled,
                                onCheckedChange = { isEnabled ->
                                    backupViewModel.updateAutoBackupConfig(
                                        autoBackupConfig.copy(enabled = isEnabled)
                                    )
                                },
                                modifier = Modifier.testTag("switch_auto_backup")
                            )
                        }

                        if (autoBackupConfig.enabled) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // 1. Frequency Selection
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isHi) "बैकअप आवृत्ति (Frequency):" else "Backup Frequency:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BackupFrequency.values().forEach { freq ->
                                        Surface(
                                            onClick = {
                                                backupViewModel.updateAutoBackupConfig(
                                                    autoBackupConfig.copy(frequency = freq)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (autoBackupConfig.frequency == freq) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (autoBackupConfig.frequency == freq) MaterialTheme.colorScheme.primary else Color.Transparent
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    RadioButton(
                                                        selected = (autoBackupConfig.frequency == freq),
                                                        onClick = {
                                                            backupViewModel.updateAutoBackupConfig(
                                                                autoBackupConfig.copy(frequency = freq)
                                                            )
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = if (isHi) freq.titleHi else freq.titleEn,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = if (autoBackupConfig.frequency == freq) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Wi-Fi Only & Retention Count Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHi) "केवल Wi-Fi पर सिंक" else "Wi-Fi Only Sync",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isHi) "मोबाइल डेटा की बचत करें" else "Save mobile data",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoBackupConfig.wifiOnly,
                                    onCheckedChange = { isWifiOnly ->
                                        backupViewModel.updateAutoBackupConfig(
                                            autoBackupConfig.copy(wifiOnly = isWifiOnly)
                                        )
                                    }
                                )
                            }

                            // 3. Retention Policy (Number of backups to keep)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHi) "अधिकतम बैकअप प्रतियां रखें:" else "Keep Backups Count:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(5, 10, 30).forEach { count ->
                                        FilterChip(
                                            selected = autoBackupConfig.keepLastCount == count,
                                            onClick = {
                                                backupViewModel.updateAutoBackupConfig(
                                                    autoBackupConfig.copy(keepLastCount = count)
                                                )
                                            },
                                            label = { Text("$count") },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // One-Touch Quick Restore of Latest Auto-Backup
                        val latestBackup = cloudBackups.firstOrNull()
                        if (latestBackup != null) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            FilledTonalButton(
                                onClick = { showRestoreDialogFor = latestBackup },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("button_quick_restore_latest"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isOperationInProgress,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHi) "नवीनतम बैकअप तुरंत रीस्टोर करें (${latestBackup.timestamp})" else "Quick Restore Latest Backup (${latestBackup.timestamp})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 5. Cloud Backups List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHi) "उपलब्ध बैकअप प्रतियां" else "Available Cloud Backups",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${cloudBackups.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 6. Backups List Items
            if (cloudBackups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isHi) "कोई क्लाउड बैकअप उपलब्ध नहीं है" else "No cloud backups available yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                items(cloudBackups, key = { it.backupId }) { backup ->
                    MinimalBackupCard(
                        backup = backup,
                        isHindi = isHi,
                        isOperationInProgress = isOperationInProgress,
                        onShowDetails = { showDetailsDialogFor = it },
                        onDelete = { backupViewModel.deleteBackup(it) },
                        onRestore = { showRestoreDialogFor = it }
                    )
                }
            }
        }
    }

    // Confirm Restore Dialog
    showRestoreDialogFor?.let { backupToRestore ->
        AlertDialog(
            onDismissRequest = { showRestoreDialogFor = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isHi) "डेटा रीस्टोर करें?" else "Restore Database?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHi) {
                            "दिनांक ${backupToRestore.timestamp} की बैकअप प्रति रीस्टोर की जाएगी।"
                        } else {
                            "Backup from ${backupToRestore.timestamp} will be restored."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (isHi) "भोजन प्रविष्टियां: ${backupToRestore.recordCounts.mealsCount} | छात्र: ${backupToRestore.recordCounts.studentsCount}"
                        else "Meals: ${backupToRestore.recordCounts.mealsCount} | Students: ${backupToRestore.recordCounts.studentsCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backup = backupToRestore
                        showRestoreDialogFor = null
                        backupViewModel.restoreBackup(
                            backup = backup,
                            restoredBy = poshanViewModel?.currentUser?.value?.name ?: poshanViewModel?.school?.value?.headTeacherName ?: "Headmaster"
                        ) { success ->
                            if (success) {
                                poshanViewModel?.refreshAllData()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("dialog_confirm_restore")
                ) {
                    Text(text = if (isHi) "रीस्टोर करें" else "Restore Now")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreDialogFor = null }) {
                    Text(text = if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Details Dialog
    showDetailsDialogFor?.let { meta ->
        AlertDialog(
            onDismissRequest = { showDetailsDialogFor = null },
            title = {
                Text(
                    text = if (isHi) "बैकअप विवरण" else "Backup Details",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DetailRow(label = if (isHi) "फ़ाइल नाम:" else "File Name:", value = meta.fileName)
                    DetailRow(label = if (isHi) "दिनांक:" else "Timestamp:", value = meta.timestamp)
                    DetailRow(label = if (isHi) "साइज:" else "Size:", value = formatBytes(meta.sizeBytes))
                    DetailRow(label = if (isHi) "भोजन रिकॉर्ड्स:" else "Meal Records:", value = "${meta.recordCounts.mealsCount}")
                    DetailRow(label = if (isHi) "नामांकित छात्र:" else "Students:", value = "${meta.recordCounts.studentsCount}")
                    DetailRow(label = if (isHi) "स्टॉक लेनदेन:" else "Stock Logs:", value = "${meta.recordCounts.stockTransactionsCount}")
                }
            },
            confirmButton = {
                Button(onClick = { showDetailsDialogFor = null }) {
                    Text(text = if (isHi) "बंद करें" else "Close")
                }
            }
        )
    }

    // Switch Account Dialog
    if (showSwitchAccountDialog) {
        var emailInput by remember { mutableStateOf(accountInfo.email) }
        var nameInput by remember { mutableStateOf(accountInfo.displayName) }

        AlertDialog(
            onDismissRequest = {
                keyboardController?.hide()
                focusManager.clearFocus()
                showSwitchAccountDialog = false
            },
            title = {
                Text(
                    text = if (isHi) "Google खाता बदलें" else "Switch Google Account",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val intent = AccountManager.newChooseAccountIntent(
                                    null, null, arrayOf("com.google"), null, null, null, null
                                )
                                chooseAccountLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Please enter email below.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isHi) "डिवाइस से खाता चुनें" else "Choose Device Account")
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Account Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (emailInput.isNotBlank()) {
                            pendingAccountEmail = emailInput.trim()
                            pendingAccountName = nameInput.trim().ifBlank { "School Admin" }
                            showSwitchAccountDialog = false
                            showPermissionRequestDialog = true
                        }
                    }
                ) {
                    Text(text = if (isHi) "सहेजें" else "Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        showSwitchAccountDialog = false
                    }
                ) {
                    Text(text = if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Google Drive Permission Request Dialog
    if (showPermissionRequestDialog) {
        val targetEmail = pendingAccountEmail.ifBlank { accountInfo.email }
        val targetName = pendingAccountName.ifBlank { accountInfo.displayName }

        AlertDialog(
            onDismissRequest = { showPermissionRequestDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isHi) "Google Drive एक्सेस अनुमति" else "Google Drive Permission",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isHi) {
                        "$targetEmail पर बैकअप फाइलें सुरक्षित करने के लिए अनुमति दें।"
                    } else {
                        "Allow app to save backup files to $targetEmail."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        backupViewModel.connectGoogleAccount(
                            email = targetEmail,
                            name = targetName,
                            token = "ya29.poshan_drive_auth_verified_${System.currentTimeMillis()}",
                            isPermissionGranted = true
                        )
                        showPermissionRequestDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(text = if (isHi) "अनुमति दें (Grant)" else "Grant Access")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPermissionRequestDialog = false }) {
                    Text(text = if (isHi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun MinimalBackupCard(
    backup: PoshanBackupMetadata,
    isHindi: Boolean,
    isOperationInProgress: Boolean,
    onShowDetails: (PoshanBackupMetadata) -> Unit,
    onDelete: (PoshanBackupMetadata) -> Unit,
    onRestore: (PoshanBackupMetadata) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backup_card_${backup.backupId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = backup.timestamp,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatBytes(backup.sizeBytes)} • ${backup.recordCounts.mealsCount} meals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onShowDetails(backup) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onDelete(backup) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalButton(
                    onClick = { onRestore(backup) },
                    enabled = !isOperationInProgress,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isHindi) "रीस्टोर" else "Restore",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.2f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
