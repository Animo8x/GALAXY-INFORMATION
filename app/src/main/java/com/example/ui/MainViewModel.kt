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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HardwareRepository(application.applicationContext)

    private val _snapshot = MutableStateFlow(repository.getHardwareSnapshot())
    val snapshot: StateFlow<HardwareSnapshot> = _snapshot.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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
    }

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index.coerceIn(0, 4)
    }

    fun goBack(): Boolean {
        if (_selectedTabIndex.value != 0) {
            _selectedTabIndex.value = 0
            return true
        }
        return false
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val fresh = repository.getHardwareSnapshot()
            _snapshot.value = fresh
            _isRefreshing.value = false
        }
    }

    fun cleanAndOptimize() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            System.gc()
            Runtime.getRuntime().gc()
            repository.killAllUserProcesses()
            kotlinx.coroutines.delay(300)
            val fresh = repository.getHardwareSnapshot()
            _snapshot.value = fresh
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
            val fresh = repository.getHardwareSnapshot()
            _snapshot.value = fresh
            _isRefreshing.value = false
            onResult(count)
        }
    }

    fun deleteSelectedOldFiles(files: List<OldFileInfo>, onResult: (Int, Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val result = repository.deleteOldFiles(files)
            val fresh = repository.getHardwareSnapshot()
            _snapshot.value = fresh
            _isRefreshing.value = false
            onResult(result.first, result.second)
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
