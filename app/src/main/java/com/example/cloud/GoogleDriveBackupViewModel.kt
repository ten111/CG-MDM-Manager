package com.example.cloud

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GoogleDriveBackupViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val engine = GoogleDriveBackupEngine(context, database)

    private val _accountInfo = MutableStateFlow(engine.driveService.getAccountInfo())
    val accountInfo: StateFlow<GoogleDriveAccountInfo> = _accountInfo.asStateFlow()

    private val _autoBackupConfig = MutableStateFlow(engine.driveService.getAutoBackupConfig())
    val autoBackupConfig: StateFlow<AutoBackupConfig> = _autoBackupConfig.asStateFlow()

    private val _cloudBackups = MutableStateFlow(engine.driveService.getCloudBackupsList())
    val cloudBackups: StateFlow<List<PoshanBackupMetadata>> = _cloudBackups.asStateFlow()

    private val _monthFolderGroups = MutableStateFlow(engine.driveService.getGroupedByMonthFolders())
    val monthFolderGroups: StateFlow<List<DriveMonthFolderGroup>> = _monthFolderGroups.asStateFlow()

    private val _currentStatus = MutableStateFlow(BackupStatus.IDLE)
    val currentStatus: StateFlow<BackupStatus> = _currentStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isOperationInProgress = MutableStateFlow(false)
    val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _accountInfo.value = engine.driveService.getAccountInfo()
        _autoBackupConfig.value = engine.driveService.getAutoBackupConfig()
        _cloudBackups.value = engine.driveService.getCloudBackupsList()
        _monthFolderGroups.value = engine.driveService.getGroupedByMonthFolders()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun performBackupNow(createdBy: String = "Headmaster") {
        if (_isOperationInProgress.value) return
        _isOperationInProgress.value = true
        _currentStatus.value = BackupStatus.PREPARING
        _progress.value = 0.05f

        viewModelScope.launch {
            try {
                val result = engine.performCloudBackup(createdBy) { prog, msg ->
                    _progress.value = prog
                    _statusMessage.value = msg
                    _currentStatus.value = if (prog < 0.3f) BackupStatus.PREPARING else BackupStatus.UPLOADING
                }

                if (result.isSuccess) {
                    val meta = result.getOrNull()
                    _currentStatus.value = BackupStatus.SUCCESS
                    _statusMessage.value = "Google Drive बैकअप सफलतापूर्वक पूर्ण हुआ!"
                    _userMessage.value = "क्लाउड बैकअप सफल: ${meta?.fileName}"
                    refresh()
                } else {
                    _currentStatus.value = BackupStatus.FAILED
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown backup error"
                    _statusMessage.value = "त्रुटि: $errorMsg"
                    _userMessage.value = "बैकअप विफल: $errorMsg"
                }
            } catch (e: Exception) {
                _currentStatus.value = BackupStatus.FAILED
                _statusMessage.value = "त्रुटि: ${e.localizedMessage}"
                _userMessage.value = "त्रुटि: ${e.localizedMessage}"
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    fun restoreBackup(backup: PoshanBackupMetadata, restoredBy: String = "Headmaster", onComplete: (Boolean) -> Unit = {}) {
        if (_isOperationInProgress.value) return
        _isOperationInProgress.value = true
        _currentStatus.value = BackupStatus.DOWNLOADING
        _progress.value = 0.1f

        viewModelScope.launch {
            try {
                val result = engine.restoreCloudBackup(backup, restoredBy) { prog, msg ->
                    _progress.value = prog
                    _statusMessage.value = msg
                    _currentStatus.value = if (prog < 0.6f) BackupStatus.DOWNLOADING else BackupStatus.RESTORING
                }

                if (result.isSuccess) {
                    _currentStatus.value = BackupStatus.SUCCESS
                    _statusMessage.value = "डेटाबेस सफलतापूर्वक रीस्टोर कर लिया गया!"
                    _userMessage.value = "डेटाबेस सफलतापूर्वक रीस्टोर हुआ (${backup.backupId})"
                    refresh()
                    onComplete(true)
                } else {
                    _currentStatus.value = BackupStatus.FAILED
                    val err = result.exceptionOrNull()?.message ?: "Restore failed"
                    _statusMessage.value = "रीस्टोर विफल: $err"
                    _userMessage.value = "रीस्टोर विफल: $err"
                    onComplete(false)
                }
            } catch (e: Exception) {
                _currentStatus.value = BackupStatus.FAILED
                _statusMessage.value = "त्रुटि: ${e.localizedMessage}"
                _userMessage.value = "त्रुटि: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    fun restoreFromLocalJson(jsonString: String, onComplete: (Boolean) -> Unit = {}) {
        if (_isOperationInProgress.value) return
        _isOperationInProgress.value = true
        _currentStatus.value = BackupStatus.RESTORING

        viewModelScope.launch {
            try {
                val result = engine.restoreFromLocalJson(jsonString) { prog, msg ->
                    _progress.value = prog
                    _statusMessage.value = msg
                }
                if (result.isSuccess) {
                    _currentStatus.value = BackupStatus.SUCCESS
                    _statusMessage.value = "फ़ाइल से डेटाबेस सफलतापूर्वक रीस्टोर हुआ!"
                    _userMessage.value = "फ़ाइल से डेटाबेस रीस्टोर सफल!"
                    refresh()
                    onComplete(true)
                } else {
                    _currentStatus.value = BackupStatus.FAILED
                    val err = result.exceptionOrNull()?.message ?: "Restore failed"
                    _statusMessage.value = "रीस्टोर विफल: $err"
                    _userMessage.value = "रीस्टोर विफल: $err"
                    onComplete(false)
                }
            } catch (e: Exception) {
                _currentStatus.value = BackupStatus.FAILED
                _statusMessage.value = "त्रुटि: ${e.localizedMessage}"
                _userMessage.value = "त्रुटि: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    fun deleteBackup(backup: PoshanBackupMetadata) {
        viewModelScope.launch {
            engine.driveService.deleteBackup(backup.backupId, backup.fileId)
            _userMessage.value = "बैकअप हटाया गया: ${backup.fileName}"
            refresh()
        }
    }

    fun updateAutoBackupConfig(newConfig: AutoBackupConfig) {
        engine.driveService.saveAutoBackupConfig(newConfig)
        _autoBackupConfig.value = newConfig
        GoogleDriveBackupScheduler.scheduleAutoBackup(context, newConfig)
        _userMessage.value = if (newConfig.enabled) {
            "स्वचालित बैकअप शेड्यूल अपडेट किया गया (${newConfig.frequency.titleHi})"
        } else {
            "स्वचालित बैकअप बंद किया गया"
        }
    }

    fun connectGoogleAccount(email: String, name: String, token: String? = null, isPermissionGranted: Boolean = true) {
        engine.driveService.updateAccount(email, name, token, isPermissionGranted)
        refresh()
        _userMessage.value = "गूगल खाता लिंक किया गया: $email"
    }

    fun grantGoogleDrivePermission() {
        engine.driveService.grantDrivePermission()
        refresh()
        _userMessage.value = "Google Drive (drive.file) एक्सेस अनुमति सफलतापूर्वक सक्रिय की गई"
    }

    fun disconnectGoogleAccount() {
        engine.driveService.disconnectAccount()
        refresh()
        _userMessage.value = "गूगल खाता डिस्कनेक्ट किया गया"
    }

    fun getSuggestedFileName(): String {
        val sdfDay = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HHmm", Locale.getDefault())
        val now = Date()
        return "GMS_BODLA_${sdfDay.format(now)}_${sdfTime.format(now)}.json"
    }

    fun saveBackupToUri(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val payload = engine.createSnapshotPayload("Headmaster (Local File Manager Export)")
                val jsonString = GoogleDriveBackupSerializer.serialize(payload)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    } ?: throw Exception("फ़ाइल स्ट्रीम खोलने में असमर्थ")
                }
                _userMessage.value = "बैकअप फ़ाइल मैनेजर में सफलतापूर्वक सुरक्षित किया गया!"
                onResult(true, "बैकअप फ़ाइल मैनेजर में सफलतापूर्वक सुरक्षित किया गया!")
            } catch (e: Exception) {
                _userMessage.value = "सेव त्रुटि: ${e.localizedMessage}"
                onResult(false, e.localizedMessage)
            }
        }
    }

    suspend fun exportLocalBackup(): File {
        return engine.exportLocalBackupFile()
    }

    fun getShareIntent(file: File): Intent {
        return engine.getShareIntentForBackup(file)
    }
}
