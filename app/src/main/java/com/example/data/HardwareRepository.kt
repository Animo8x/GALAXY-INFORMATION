package com.example.data

import android.Manifest
import android.app.ActivityManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.Range
import android.util.Size
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HardwareRepository(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    fun getHardwareSnapshot(): HardwareSnapshot {
        return HardwareSnapshot(
            ram = getRamInfo(),
            storage = getStorageInfo(),
            battery = getBatteryInfo(),
            cameras = getCamerasSummary(),
            system = getSystemInfo(),
            runningProcesses = getRunningProcesses(),
            oldFiles = emptyList(), // Load asynchronously on demand to avoid ANR!
            isScanningFiles = false,
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    // -------------------------------------------------------------
    // Permission Checks
    // -------------------------------------------------------------
    fun hasStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val img = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val vid = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val aud = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            return img || vid || aud
        }

        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    // -------------------------------------------------------------
    // 1. RAM Information
    // -------------------------------------------------------------
    fun getRamInfo(): RamInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMem = memoryInfo.totalMem
        val availMem = memoryInfo.availMem
        val usedMem = (totalMem - availMem).coerceAtLeast(0L)
        val usagePercentage = if (totalMem > 0) {
            ((usedMem.toDouble() / totalMem.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        val appMaxHeap = Runtime.getRuntime().maxMemory()

        var swapTotal: Long? = null
        var swapFree: Long? = null
        try {
            val file = File("/proc/meminfo")
            if (file.exists() && file.canRead()) {
                BufferedReader(FileReader(file)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("SwapTotal:")) {
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.size >= 2) swapTotal = parts[1].toLongOrNull()?.times(1024L)
                        } else if (trimmed.startsWith("SwapFree:")) {
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.size >= 2) swapFree = parts[1].toLongOrNull()?.times(1024L)
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (_: Exception) {}

        return RamInfo(
            totalBytes = totalMem,
            availBytes = availMem,
            usedBytes = usedMem,
            usagePercentage = usagePercentage,
            thresholdBytes = memoryInfo.threshold,
            isLowMemory = memoryInfo.lowMemory,
            appMaxHeapBytes = appMaxHeap,
            swapTotalBytes = swapTotal,
            swapFreeBytes = swapFree
        )
    }

    // -------------------------------------------------------------
    // Background Running Apps & Games (Optimized & Safe)
    // -------------------------------------------------------------
    fun getRunningProcesses(): List<RunningAppProcessInfoItem> {
        val pm = context.packageManager
        val result = mutableListOf<RunningAppProcessInfoItem>()
        val myPackageName = context.packageName
        val seenPackages = mutableSetOf<String>()

        // 1. Check running processes from ActivityManager
        try {
            val processes = activityManager.runningAppProcesses
            if (processes != null) {
                for (proc in processes) {
                    val pkgName = proc.processName.split(":")[0]
                    seenPackages.add(pkgName)
                    var appLabel = proc.processName
                    var isSystem = true
                    var categoryName = "تطبيق (App)"

                    try {
                        val appInfo = pm.getApplicationInfo(pkgName, 0)
                        appLabel = pm.getApplicationLabel(appInfo).toString()
                        isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            categoryName = when (appInfo.category) {
                                ApplicationInfo.CATEGORY_GAME -> "لعبة (Game)"
                                ApplicationInfo.CATEGORY_AUDIO -> "صوتيات (Audio)"
                                ApplicationInfo.CATEGORY_VIDEO -> "فيديو (Video)"
                                ApplicationInfo.CATEGORY_IMAGE -> "صور (Image)"
                                ApplicationInfo.CATEGORY_SOCIAL -> "تواصل اجتماعي (Social)"
                                ApplicationInfo.CATEGORY_NEWS -> "أخبار (News)"
                                ApplicationInfo.CATEGORY_MAPS -> "خرائط وملاحة"
                                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "إنتاجية وأعمال"
                                else -> if (isSystem) "خدمة نظام" else "تطبيق (App)"
                            }
                        } else {
                            val isGame = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                            categoryName = if (isGame) "لعبة (Game)" else if (isSystem) "خدمة نظام" else "تطبيق (App)"
                        }
                    } catch (_: Exception) {}

                    val importanceText = when (proc.importance) {
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "في الواجهة (نشط)"
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "مرئي في الخلفية"
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "خدمة تعمل في الخلفية"
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "مخزن مؤقتاً في RAM"
                        else -> "يعمل في الخلفية"
                    }

                    var pssBytes = 0L
                    if (!isSystem || pkgName == myPackageName) {
                        try {
                            val mem = activityManager.getProcessMemoryInfo(intArrayOf(proc.pid))
                            if (mem.isNotEmpty()) {
                                pssBytes = mem[0].totalPss * 1024L
                            }
                        } catch (_: Exception) {}
                    }

                    result.add(
                        RunningAppProcessInfoItem(
                            processName = proc.processName,
                            packageName = pkgName,
                            appName = appLabel,
                            pid = proc.pid,
                            ramUsageBytes = pssBytes,
                            isSystemApp = isSystem,
                            categoryName = categoryName,
                            importanceText = importanceText
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Also check user installed apps and games so user can always see and manage background apps
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                if (result.size >= 40) break
                val pkgName = appInfo.packageName
                if (pkgName in seenPackages || pkgName == myPackageName) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!isSystem) {
                    seenPackages.add(pkgName)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appInfo.category == ApplicationInfo.CATEGORY_GAME
                    } else {
                        (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                    }
                    val category = if (isGame) "لعبة (Game)" else "تطبيق مستخدم (App)"

                    result.add(
                        RunningAppProcessInfoItem(
                            processName = pkgName,
                            packageName = pkgName,
                            appName = label,
                            pid = 0,
                            ramUsageBytes = 0L,
                            isSystemApp = false,
                            categoryName = category,
                            importanceText = "مثبت / جاهز للإيقاف"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return result.sortedWith(
            compareBy<RunningAppProcessInfoItem> { it.isSystemApp }
                .thenByDescending { it.ramUsageBytes }
        )
    }

    fun killProcess(packageName: String) {
        try {
            if (packageName != context.packageName) {
                activityManager.killBackgroundProcesses(packageName)
                System.gc()
            }
        } catch (_: Exception) {}
    }

    fun killAllUserProcesses(): Int {
        val running = getRunningProcesses().filter { !it.isSystemApp && it.packageName != context.packageName }
        var count = 0
        for (item in running) {
            try {
                activityManager.killBackgroundProcesses(item.packageName)
                count++
            } catch (_: Exception) {}
        }
        System.gc()
        return count
    }

    // -------------------------------------------------------------
    // 2. Storage Information
    // -------------------------------------------------------------
    fun getStorageInfo(): StorageInfo {
        val dataDir = Environment.getDataDirectory()
        val stat = StatFs(dataDir.path)

        val totalBytes = stat.totalBytes
        val availableBytes = stat.availableBytes
        val freeBytes = stat.freeBytes
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val usagePercentage = if (totalBytes > 0) {
            ((usedBytes.toDouble() / totalBytes.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        var hasSdCard = false
        var sdCardTotal: Long? = null
        var sdCardFree: Long? = null
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            if (externalDirs.size > 1 && externalDirs[1] != null) {
                val sdDir = externalDirs[1]
                if (Environment.isExternalStorageRemovable(sdDir)) {
                    val sdStat = StatFs(sdDir.path)
                    if (sdStat.totalBytes > 0) {
                        hasSdCard = true
                        sdCardTotal = sdStat.totalBytes
                        sdCardFree = sdStat.freeBytes
                    }
                }
            }
        } catch (_: Exception) {}

        return StorageInfo(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            usagePercentage = usagePercentage,
            path = dataDir.path,
            hasSdCard = hasSdCard,
            sdCardTotalBytes = sdCardTotal,
            sdCardFreeBytes = sdCardFree
        )
    }

    // -------------------------------------------------------------
    // Dynamic Old Files Scanner (with customizable period: 2 days, 1 week, 1 month, 2 months, 3 months, 6 months, 1 year)
    // -------------------------------------------------------------
    fun getOldFiles(period: OldFilePeriod = OldFilePeriod.SIX_MONTHS): List<OldFileInfo> {
        val result = mutableListOf<OldFileInfo>()
        val cutOffMillis = System.currentTimeMillis() - (period.days * 24L * 60L * 60L * 1000L)
        val cutOffSeconds = cutOffMillis / 1000L

        // 1. Scan MediaStore Images
        queryMediaStoreTable(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cutOffSeconds, result, maxItems = 40)

        // 2. Scan MediaStore Videos
        queryMediaStoreTable(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cutOffSeconds, result, maxItems = 40)

        // 3. Scan MediaStore Audio
        queryMediaStoreTable(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cutOffSeconds, result, maxItems = 30)

        // 4. Scan MediaStore Downloads (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                queryMediaStoreTable(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cutOffSeconds, result, maxItems = 40)
            } catch (_: Exception) {}
        }

        // 5. Scan MediaStore Files
        try {
            queryMediaStoreTable(MediaStore.Files.getContentUri("external"), cutOffSeconds, result, maxItems = 40)
        } catch (_: Exception) {}

        // 6. Scan app's external files and cache
        val appDirs = listOfNotNull(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            context.getExternalFilesDir(null),
            context.externalCacheDir,
            context.cacheDir
        )
        for (dir in appDirs) {
            scanDirectoryRecursively(dir, cutOffMillis, result, maxDepth = 2)
        }

        // 7. If public storage is accessible, scan public folders safely
        try {
            val publicDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            )
            for (dir in publicDirs) {
                if (result.size >= 120) break
                scanDirectoryRecursively(dir, cutOffMillis, result, maxDepth = 2)
            }
        } catch (_: Exception) {}

        return result.distinctBy { it.uriString ?: it.path }
            .sortedByDescending { it.sizeBytes }
    }

    private fun queryMediaStoreTable(
        contentUri: Uri,
        cutOffSeconds: Long,
        result: MutableList<OldFileInfo>,
        maxItems: Int
    ) {
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATA
            )
            val selection = "${MediaStore.MediaColumns.DATE_MODIFIED} < ? AND ${MediaStore.MediaColumns.SIZE} > 0"
            val selectionArgs = arrayOf(cutOffSeconds.toString())
            val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

            context.contentResolver.query(
                contentUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                var count = 0
                while (cursor.moveToNext() && count < maxItems && result.size < 150) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "file_$id" else "file_$id"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val modSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val modMillis = modSec * 1000L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val uri = ContentUris.withAppendedId(contentUri, id).toString()

                    val diffMillis = (System.currentTimeMillis() - modMillis).coerceAtLeast(0L)
                    val ageDesc = formatAgeDescription(diffMillis)

                    val isImg = mime.startsWith("image/") || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true)
                    val isVid = mime.startsWith("video/") || name.endsWith(".mp4", true) || name.endsWith(".mkv", true)
                    val isDoc = mime.contains("pdf") || mime.contains("document") || name.endsWith(".pdf", true) || name.endsWith(".doc", true) || name.endsWith(".txt", true)

                    result.add(
                        OldFileInfo(
                            id = uri,
                            uriString = uri,
                            path = path,
                            name = name,
                            sizeBytes = size,
                            lastModifiedMillis = modMillis,
                            ageDescription = ageDesc,
                            isImage = isImg,
                            isVideo = isVid,
                            isDocument = isDoc
                        )
                    )
                    count++
                }
            }
        } catch (_: Exception) {}
    }

    private fun scanDirectoryRecursively(
        dir: File?,
        cutOffMillis: Long,
        result: MutableList<OldFileInfo>,
        maxDepth: Int
    ) {
        if (dir == null || !dir.exists() || !dir.canRead() || maxDepth <= 0 || result.size >= 150) return
        try {
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (result.size >= 150) break
                try {
                    if (f.isDirectory) {
                        if (!f.name.startsWith(".") && f.name != "Android") {
                            scanDirectoryRecursively(f, cutOffMillis, result, maxDepth - 1)
                        }
                    } else if (f.isFile && f.length() > 0) {
                        val mod = f.lastModified()
                        if (mod < cutOffMillis) {
                            val diff = (System.currentTimeMillis() - mod).coerceAtLeast(0L)
                            val ageDesc = formatAgeDescription(diff)
                            val name = f.name
                            val isImg = name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true)
                            val isVid = name.endsWith(".mp4", true) || name.endsWith(".mkv", true) || name.endsWith(".avi", true)
                            val isDoc = name.endsWith(".pdf", true) || name.endsWith(".txt", true) || name.endsWith(".doc", true) || name.endsWith(".zip", true)

                            result.add(
                                OldFileInfo(
                                    id = f.absolutePath,
                                    uriString = null,
                                    path = f.absolutePath,
                                    name = name,
                                    sizeBytes = f.length(),
                                    lastModifiedMillis = mod,
                                    ageDescription = ageDesc,
                                    isImage = isImg,
                                    isVideo = isVid,
                                    isDocument = isDoc
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun formatAgeDescription(diffMillis: Long): String {
        val days = diffMillis / (24L * 60L * 60L * 1000L)
        return when {
            days < 1 -> "اليوم"
            days < 7 -> "منذ $days أيام"
            days < 30 -> "منذ ${days / 7} أسابيع"
            days < 365 -> "منذ ${days / 30} أشهر"
            else -> "منذ ${days / 365} سنة"
        }
    }

    fun deleteOldFiles(filesToDelete: List<OldFileInfo>): Pair<Int, Long> {
        var count = 0
        var freedBytes = 0L

        for (file in filesToDelete) {
            var deleted = false
            if (!file.uriString.isNullOrBlank()) {
                try {
                    val rows = context.contentResolver.delete(Uri.parse(file.uriString), null, null)
                    if (rows > 0) deleted = true
                } catch (_: Exception) {}
            }
            if (!deleted && file.path.isNotBlank()) {
                try {
                    val f = File(file.path)
                    if (f.exists()) {
                        deleted = f.delete()
                    }
                } catch (_: Exception) {}
            }
            if (deleted) {
                count++
                freedBytes += file.sizeBytes
            }
        }
        return Pair(count, freedBytes)
    }

    fun createSampleTestFiles(): Int {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        var created = 0
        val sampleNames = listOf(
            Triple("temp_cache_report.log", 950_000L, 3L), // 3 days ago (matched by TWO_DAYS)
            Triple("weekly_download_cache.dat", 2_400_000L, 10L), // 10 days ago (matched by ONE_WEEK)
            Triple("camera_photo_old.jpg", 3_450_000L, 35L), // 35 days ago (matched by ONE_MONTH)
            Triple("project_archive_backup.zip", 8_800_000L, 65L), // 65 days ago (matched by TWO_MONTHS)
            Triple("quarterly_statement.pdf", 1_850_000L, 100L), // 100 days ago (matched by THREE_MONTHS)
            Triple("video_clip_vacation.mp4", 18_500_000L, 200L), // 200 days ago (matched by SIX_MONTHS)
            Triple("annual_system_archive.bin", 22_000_000L, 380L) // 380 days ago (matched by ONE_YEAR)
        )

        for ((name, size, daysAgo) in sampleNames) {
            try {
                val f = File(baseDir, name)
                if (!f.exists()) {
                    f.createNewFile()
                    val dummyBytes = ByteArray(1024)
                    f.outputStream().use { fos ->
                        val chunks = (size / 1024L).coerceIn(1L, 1024L)
                        for (i in 0 until chunks) {
                            fos.write(dummyBytes)
                        }
                    }
                    val backdatedTime = System.currentTimeMillis() - (daysAgo * 24L * 60L * 60L * 1000L)
                    f.setLastModified(backdatedTime)
                    created++
                }
            } catch (_: Exception) {}
        }
        return created
    }

    // -------------------------------------------------------------
    // 3. Battery Information
    // -------------------------------------------------------------
    fun getBatteryInfo(): BatteryInfo {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "جاري الشحن"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "يفرغ الشحن (قيد الاستخدام)"
            BatteryManager.BATTERY_STATUS_FULL -> "مكتمل الشحن (100%)"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "لا يشحن (متصل ولكن غير نشط)"
            else -> "غير معروف"
        }

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val powerSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "شاحن جداري (AC)"
            BatteryManager.BATTERY_PLUGGED_USB -> "منفذ USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "شاحن لاسلكي"
            0 -> "البطارية فقط (غير متصل)"
            else -> "غير معروف"
        }

        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthText = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "جيدة (Good)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "حرارة مرتفعة جداً (Overheat)"
            BatteryManager.BATTERY_HEALTH_DEAD -> "تالفة (Dead)"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "جهد زائد (Over Voltage)"
            BatteryManager.BATTERY_HEALTH_COLD -> "باردة جداً (Cold)"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "خلل غير محدد"
            else -> "غير معروف"
        }

        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatureCelsius = tempRaw / 10.0f

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageVolts = voltageMv / 1000.0f

        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            ?.takeIf { it.isNotBlank() } ?: "Li-ion (أيون الليثيوم)"

        var capacityText = "غير متاح على هذا الجهاز"
        try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfile = powerProfileClass.getConstructor(Context::class.java).newInstance(context)
            val getBatteryCapacityMethod = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = getBatteryCapacityMethod.invoke(powerProfile) as? Double
            if (capacity != null && capacity > 0) {
                capacityText = "${capacity.toInt()} mAh (السعة التصميمية المقدرة)"
            }
        } catch (_: Exception) {}

        return BatteryInfo(
            levelPercentage = percentage,
            statusText = statusText,
            isCharging = isCharging,
            powerSource = powerSource,
            healthText = healthText,
            temperatureCelsius = temperatureCelsius,
            voltageVolts = voltageVolts,
            voltageMv = voltageMv,
            technology = technology,
            capacityInfo = capacityText
        )
    }

    // -------------------------------------------------------------
    // 4. Cameras Information
    // -------------------------------------------------------------
    fun getCamerasSummary(): CamerasSummary {
        val manager = cameraManager ?: return CamerasSummary(0, 0, 0, emptyList())
        val cameraItems = mutableListOf<CameraItemInfo>()

        try {
            val cameraIds = manager.cameraIdList
            var backCount = 0
            var frontCount = 0

            for (id in cameraIds) {
                val characteristics = manager.getCameraCharacteristics(id)
                val facingInt = characteristics.get(CameraCharacteristics.LENS_FACING)

                val isBack = facingInt == CameraCharacteristics.LENS_FACING_BACK
                val isFront = facingInt == CameraCharacteristics.LENS_FACING_FRONT
                val facing = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        backCount++
                        "خلفية (Rear $backCount)"
                    }
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        frontCount++
                        "أمامية (Front $frontCount)"
                    }
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "خارجية (External)"
                    else -> "غير محدد"
                }

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
                var maxMp = 0.0
                var maxSizeStr = "غير متاح"

                if (jpegSizes.isNotEmpty()) {
                    var largestSize: Size = jpegSizes[0]
                    var maxPixels = largestSize.width.toLong() * largestSize.height.toLong()
                    for (size in jpegSizes) {
                        val pixels = size.width.toLong() * size.height.toLong()
                        if (pixels > maxPixels) {
                            maxPixels = pixels
                            largestSize = size
                        }
                    }
                    maxMp = maxPixels / 1_000_000.0
                    maxSizeStr = "${largestSize.width} × ${largestSize.height}"
                }

                val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val activeArrayStr = if (activeArray != null) {
                    "${activeArray.width()} × ${activeArray.height()}"
                } else {
                    "غير متاح"
                }

                val maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
                var minZoom = 1.0f
                var maxZoom = maxDigitalZoom
                var zoomDesc = "تقريب رقمي حتى ${String.format(Locale.US, "%.1f", maxDigitalZoom)}x"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                    if (zoomRange != null) {
                        minZoom = zoomRange.lower
                        maxZoom = zoomRange.upper
                        zoomDesc = "${String.format(Locale.US, "%.1f", minZoom)}x إلى ${String.format(Locale.US, "%.1f", maxZoom)}x"
                    }
                }

                val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
                val focusModesList: List<String> = afModes.toList().mapNotNull { mode: Int ->
                    when (mode) {
                        CameraMetadata.CONTROL_AF_MODE_AUTO -> "تركيز تلقائي (Auto)"
                        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "تركيز مستمر للصور"
                        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "تركيز مستمر للفيديو"
                        CameraMetadata.CONTROL_AF_MODE_MACRO -> "وضع ماكرو (Macro)"
                        CameraMetadata.CONTROL_AF_MODE_EDOF -> "عمق ممتد (EDOF)"
                        CameraMetadata.CONTROL_AF_MODE_OFF -> "تركيز يدوي (Manual / Off)"
                        else -> null
                    }
                }.distinct()

                val sceneModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES) ?: intArrayOf()
                val isHdrSupported = sceneModes.contains(CameraMetadata.CONTROL_SCENE_MODE_HDR)

                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                val isRawSupported = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)

                val videoSizes = map?.getOutputSizes(android.media.MediaRecorder::class.java)
                    ?: map?.getOutputSizes(android.graphics.SurfaceTexture::class.java)
                    ?: emptyArray()
                val videoResList = videoSizes.map { "${it.width} × ${it.height}" }
                    .distinct()
                    .take(4)

                val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                val maxFps = fpsRanges?.maxOfOrNull { it.upper } ?: 30

                val opticalStab = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val hasOis = opticalStab?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) == true

                val videoStab = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                val hasEis = videoStab?.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true

                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

                val physicalIds = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val pIds = characteristics.physicalCameraIds
                    physicalIds.addAll(pIds)
                }

                cameraItems.add(
                    CameraItemInfo(
                        id = id,
                        facing = facing,
                        isBack = isBack,
                        resolutionMegapixels = maxMp,
                        maxResolutionSize = maxSizeStr,
                        sensorActiveArraySize = activeArrayStr,
                        minZoom = minZoom,
                        maxZoom = maxZoom,
                        zoomDescription = zoomDesc,
                        focusModes = focusModesList.ifEmpty { listOf("عادي / ثابت") },
                        isHdrSupported = isHdrSupported,
                        isRawSupported = isRawSupported,
                        videoResolutions = videoResList.ifEmpty { listOf("1920 × 1080 (FHD)") },
                        maxFps = maxFps,
                        hasOis = hasOis,
                        hasEis = hasEis,
                        hasFlash = hasFlash,
                        physicalCameraIds = physicalIds
                    )
                )
            }

            return CamerasSummary(
                totalCameras = cameraItems.size,
                backCamerasCount = backCount,
                frontCamerasCount = frontCount,
                camerasList = cameraItems
            )
        } catch (_: Exception) {
            return CamerasSummary(0, 0, 0, emptyList())
        }
    }

    // -------------------------------------------------------------
    // 5. System & CPU Information
    // -------------------------------------------------------------
    fun getSystemInfo(): HardwareSystemInfo {
        val coreCount = Runtime.getRuntime().availableProcessors()
        val cpuCores = mutableListOf<CpuCoreInfo>()

        for (i in 0 until coreCount) {
            val curFreq = readFrequencyFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            val maxFreq = readFrequencyFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            cpuCores.add(CpuCoreInfo(coreIndex = i, currentFreqKhz = curFreq, maxFreqKhz = maxFreq))
        }

        var procName = "غير متاح"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL
            if (soc.isNotBlank() && soc != Build.UNKNOWN) {
                procName = soc
            }
        }

        if (procName == "غير متاح") {
            val hw = Build.HARDWARE
            if (hw.isNotBlank() && hw != Build.UNKNOWN) {
                procName = hw
            }
        }

        try {
            val file = File("/proc/cpuinfo")
            if (file.exists() && file.canRead()) {
                BufferedReader(FileReader(file)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("Hardware") || trimmed.startsWith("model name") || trimmed.startsWith("Processor")) {
                            val parts = trimmed.split(":")
                            if (parts.size >= 2) {
                                val valStr = parts[1].trim()
                                if (valStr.isNotBlank() && procName == "غير متاح") {
                                    procName = valStr
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (_: Exception) {}

        val glVersion = try {
            activityManager.deviceConfigurationInfo.glEsVersion ?: "OpenGL ES 3.0"
        } catch (_: Exception) {
            "غير متاح"
        }

        val arch = System.getProperty("os.arch") ?: "غير معروف"
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val kernel = System.getProperty("os.version") ?: "غير معروف"
        val secPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            "غير متاح"
        }

        return HardwareSystemInfo(
            processorName = procName,
            hardwareBoard = Build.BOARD,
            socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else "غير متاح",
            cpuArchitecture = arch,
            supportedAbis = abis,
            coreCount = coreCount,
            cpuCores = cpuCores,
            openGlVersion = glVersion,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            modelNumber = Build.PRODUCT,
            buildDevice = Build.DEVICE,
            buildBoard = Build.BOARD,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            kernelVersion = kernel,
            securityPatch = secPatch,
            buildId = Build.DISPLAY.ifBlank { Build.ID }
        )
    }

    private fun readFrequencyFile(path: String): Long? {
        return try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                RandomAccessFile(file, "r").use { raf ->
                    raf.readLine()?.trim()?.toLongOrNull()
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val mb = bytes / (1024.0 * 1024.0)
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) {
                String.format(Locale.US, "%.2f GB", gb)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }

        fun formatBytesGbOnly(bytes: Long): String {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return String.format(Locale.US, "%.2f GB", gb)
        }

        fun formatBytesMbOnly(bytes: Long): String {
            val mb = bytes / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f MB", mb)
        }

        fun formatDate(millis: Long): String {
            return try {
                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                sdf.format(Date(millis))
            } catch (_: Exception) {
                "قبل فترة"
            }
        }
    }
}
