package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HardwareRepository
import com.example.data.HardwareSnapshot
import com.example.data.OldFileInfo
import com.example.data.OldFilePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HardwareRepository(application.applicationContext)

    private val _snapshot = MutableStateFlow(repository.getHardwareSnapshot())
    val snapshot: StateFlow<HardwareSnapshot> = _snapshot.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(OldFilePeriod.SIX_MONTHS)
    val selectedPeriod: StateFlow<OldFilePeriod> = _selectedPeriod.asStateFlow()

    private val _isScanningFiles = MutableStateFlow(false)
    val isScanningFiles: StateFlow<Boolean> = _isScanningFiles.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(repository.hasStoragePermission())
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryOnly()
            }
        }
    }

    init {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            getApplication<Application>().registerReceiver(batteryReceiver, filter)
        } catch (_: Exception) {}

        // Start automatic 2-second live updates for battery, RAM, and storage
        startPeriodicLiveUpdates()

        // Initial background scan for old files and running processes without blocking UI
        scanOldFiles(_selectedPeriod.value)
    }

    private fun startPeriodicLiveUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(2000L) // Exactly every 2 seconds
                try {
                    val freshBattery = repository.getBatteryInfo()
                    val freshRam = repository.getRamInfo()
                    val freshStorage = repository.getStorageInfo()
                    val current = _snapshot.value

                    _snapshot.value = current.copy(
                        battery = freshBattery,
                        ram = freshRam,
                        storage = freshStorage,
                        lastUpdatedMillis = System.currentTimeMillis()
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun createSampleTestFilesAndScan() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningFiles.value = true
            repository.createSampleTestFiles()
            val files = repository.getOldFiles(_selectedPeriod.value)
            _snapshot.value = _snapshot.value.copy(
                oldFiles = files,
                isScanningFiles = false
            )
            _isScanningFiles.value = false
        }
    }

    fun updatePermissionStatus() {
        val granted = repository.hasStoragePermission()
        _hasStoragePermission.value = granted
        if (granted) {
            scanOldFiles(_selectedPeriod.value)
        }
    }

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index.coerceIn(0, 4)
        if (index == 1) {
            // Check permissions & refresh background processes
            updatePermissionStatus()
            refreshProcessesOnly()
        }
    }

    fun goBack(): Boolean {
        if (_selectedTabIndex.value != 0) {
            _selectedTabIndex.value = 0
            return true
        }
        return false
    }

    fun setPeriodAndScan(period: OldFilePeriod) {
        _selectedPeriod.value = period
        scanOldFiles(period)
    }

    fun scanOldFiles(period: OldFilePeriod = _selectedPeriod.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningFiles.value = true
            val files = repository.getOldFiles(period)
            _snapshot.value = _snapshot.value.copy(
                oldFiles = files,
                isScanningFiles = false
            )
            _isScanningFiles.value = false
        }
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val fresh = repository.getHardwareSnapshot()
            val files = repository.getOldFiles(_selectedPeriod.value)
            _snapshot.value = fresh.copy(oldFiles = files)
            _isRefreshing.value = false
            _hasStoragePermission.value = repository.hasStoragePermission()
        }
    }

    fun refreshProcessesOnly() {
        viewModelScope.launch(Dispatchers.IO) {
            val freshProc = repository.getRunningProcesses()
            val freshRam = repository.getRamInfo()
            _snapshot.value = _snapshot.value.copy(
                runningProcesses = freshProc,
                ram = freshRam
            )
        }
    }

    fun cleanAndOptimize() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            System.gc()
            Runtime.getRuntime().gc()
            repository.killAllUserProcesses()
            kotlinx.coroutines.delay(200)
            val fresh = repository.getHardwareSnapshot()
            val files = repository.getOldFiles(_selectedPeriod.value)
            _snapshot.value = fresh.copy(oldFiles = files)
            _isRefreshing.value = false
        }
    }

    fun killProcess(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.killProcess(packageName)
            val updatedProcesses = repository.getRunningProcesses()
            val freshRam = repository.getRamInfo()
            _snapshot.value = _snapshot.value.copy(
                runningProcesses = updatedProcesses,
                ram = freshRam
            )
        }
    }

    fun killAllBackgroundProcesses(onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val count = repository.killAllUserProcesses()
            val updatedProcesses = repository.getRunningProcesses()
            val freshRam = repository.getRamInfo()
            _snapshot.value = _snapshot.value.copy(
                runningProcesses = updatedProcesses,
                ram = freshRam
            )
            _isRefreshing.value = false
            withContext(Dispatchers.Main) {
                onResult(count)
            }
        }
    }

    fun deleteSelectedOldFiles(files: List<OldFileInfo>, onResult: (Int, Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningFiles.value = true
            val result = repository.deleteOldFiles(files)
            val freshStorage = repository.getStorageInfo()
            val remainingFiles = repository.getOldFiles(_selectedPeriod.value)
            _snapshot.value = _snapshot.value.copy(
                storage = freshStorage,
                oldFiles = remainingFiles
            )
            _isScanningFiles.value = false
            withContext(Dispatchers.Main) {
                onResult(result.first, result.second)
            }
        }
    }

    private fun updateBatteryOnly() {
        viewModelScope.launch(Dispatchers.IO) {
            val freshBattery = repository.getBatteryInfo()
            val current = _snapshot.value
            _snapshot.value = current.copy(battery = freshBattery)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }
}
