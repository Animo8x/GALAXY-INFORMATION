package com.example.data

data class RamInfo(
    val totalBytes: Long,
    val availBytes: Long,
    val usedBytes: Long,
    val usagePercentage: Float,
    val thresholdBytes: Long,
    val isLowMemory: Boolean,
    val appMaxHeapBytes: Long,
    val swapTotalBytes: Long?,
    val swapFreeBytes: Long?
)

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usagePercentage: Float,
    val path: String,
    val hasSdCard: Boolean,
    val sdCardTotalBytes: Long?,
    val sdCardFreeBytes: Long?
)

data class BatteryInfo(
    val levelPercentage: Int,
    val statusText: String,
    val isCharging: Boolean,
    val powerSource: String,
    val healthText: String,
    val temperatureCelsius: Float,
    val voltageVolts: Float,
    val voltageMv: Int,
    val technology: String,
    val capacityInfo: String
)

data class CameraItemInfo(
    val id: String,
    val facing: String,
    val isBack: Boolean,
    val resolutionMegapixels: Double,
    val maxResolutionSize: String,
    val sensorActiveArraySize: String,
    val minZoom: Float,
    val maxZoom: Float,
    val zoomDescription: String,
    val focusModes: List<String>,
    val isHdrSupported: Boolean,
    val isRawSupported: Boolean,
    val videoResolutions: List<String>,
    val maxFps: Int,
    val hasOis: Boolean,
    val hasEis: Boolean,
    val hasFlash: Boolean,
    val physicalCameraIds: List<String>
)

data class CamerasSummary(
    val totalCameras: Int,
    val backCamerasCount: Int,
    val frontCamerasCount: Int,
    val camerasList: List<CameraItemInfo>
)

data class CpuCoreInfo(
    val coreIndex: Int,
    val currentFreqKhz: Long?,
    val maxFreqKhz: Long?
)

data class HardwareSystemInfo(
    val processorName: String,
    val hardwareBoard: String,
    val socModel: String,
    val cpuArchitecture: String,
    val supportedAbis: String,
    val coreCount: Int,
    val cpuCores: List<CpuCoreInfo>,
    val openGlVersion: String,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val modelNumber: String,
    val buildDevice: String,
    val buildBoard: String,
    val androidVersion: String,
    val apiLevel: Int,
    val kernelVersion: String,
    val securityPatch: String,
    val buildId: String
)

enum class OldFilePeriod(val label: String, val days: Long) {
    TWO_DAYS("يومان", 2L),
    ONE_WEEK("أسبوع", 7L),
    ONE_MONTH("شهر", 30L),
    TWO_MONTHS("شهران", 60L),
    THREE_MONTHS("3 أشهر", 90L),
    SIX_MONTHS("6 أشهر", 180L),
    ONE_YEAR("سنة", 365L)
}

data class OldFileInfo(
    val id: String,
    val uriString: String?,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val ageDescription: String,
    val isImage: Boolean,
    val isVideo: Boolean,
    val isDocument: Boolean
)

data class RunningAppProcessInfoItem(
    val processName: String,
    val packageName: String,
    val appName: String,
    val pid: Int,
    val ramUsageBytes: Long,
    val isSystemApp: Boolean,
    val categoryName: String,
    val importanceText: String
)

data class HardwareSnapshot(
    val ram: RamInfo,
    val storage: StorageInfo,
    val battery: BatteryInfo,
    val cameras: CamerasSummary,
    val system: HardwareSystemInfo,
    val runningProcesses: List<RunningAppProcessInfoItem> = emptyList(),
    val oldFiles: List<OldFileInfo> = emptyList(),
    val isScanningFiles: Boolean = false,
    val lastUpdatedMillis: Long
)
