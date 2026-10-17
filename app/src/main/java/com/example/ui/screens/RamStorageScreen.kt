package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HardwareRepository
import com.example.data.HardwareSnapshot
import com.example.data.OldFileInfo
import com.example.data.RunningAppProcessInfoItem
import com.example.ui.components.ClassicHorizontalProgressBar
import com.example.ui.components.ClassicInfoRow
import com.example.ui.components.ClassicSectionHeader
import com.example.ui.theme.ClassicBarUsed
import com.example.ui.theme.ClassicBorder
import com.example.ui.theme.ClassicBorderLight
import com.example.ui.theme.ClassicCleanAllBlue
import com.example.ui.theme.ClassicCyan
import com.example.ui.theme.ClassicDarkCyan
import com.example.ui.theme.ClassicGrayBg
import com.example.ui.theme.ClassicGreen
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ClassicTextPrimary
import com.example.ui.theme.ClassicTextSecondary
import java.util.Locale

@Composable
fun RamStorageScreen(
    snapshot: HardwareSnapshot,
    onKillProcess: (String) -> Unit = {},
    onKillAllProcesses: () -> Unit = {},
    onDeleteFiles: (List<OldFileInfo>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // State for selected old files
    val selectedFileIds = remember { mutableStateListOf<String>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val allOldFiles = snapshot.oldFiles
    val selectedFilesList = remember(selectedFileIds.toList(), allOldFiles) {
        allOldFiles.filter { selectedFileIds.contains(it.id) }
    }
    val totalSelectedBytes = remember(selectedFilesList) {
        selectedFilesList.sumOf { it.sizeBytes }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClassicGrayBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(scrollState)
                .testTag("ram_storage_screen")
        ) {
            // =========================================================
            // 1. RAM Section (قسم ذاكرة RAM)
            // =========================================================
            ClassicSectionHeader(title = "1. ذاكرة الوصول العشوائي (RAM)")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "استخدام الذاكرة الحالية",
                        color = ClassicTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", snapshot.ram.usagePercentage),
                        color = ClassicBarUsed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Classic horizontal RAM progress bar (Blue = used, White = available)
                ClassicHorizontalProgressBar(
                    usagePercentage = snapshot.ram.usagePercentage,
                    barHeight = 22.dp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .padding(end = 4.dp)
                                .background(ClassicBarUsed)
                        )
                        Text(
                            text = "المستخدم: ${HardwareRepository.formatBytes(snapshot.ram.usedBytes)}",
                            color = ClassicTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "المتاح: ${HardwareRepository.formatBytes(snapshot.ram.availBytes)}",
                            color = ClassicTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ClassicBorder)
            )

            // Details in exact order required by prompt:
            ClassicInfoRow(
                label = "إجمالي الذاكرة",
                value = "${HardwareRepository.formatBytesGbOnly(snapshot.ram.totalBytes)} (${HardwareRepository.formatBytesMbOnly(snapshot.ram.totalBytes)})",
                isEmphasized = true
            )
            ClassicInfoRow(
                label = "المستخدم",
                value = HardwareRepository.formatBytes(snapshot.ram.usedBytes),
                valueColor = ClassicBarUsed
            )
            ClassicInfoRow(
                label = "المتاح",
                value = HardwareRepository.formatBytes(snapshot.ram.availBytes),
                valueColor = ClassicDarkCyan
            )
            ClassicInfoRow(
                label = "نسبة الاستخدام",
                value = String.format(Locale.US, "%.1f%%", snapshot.ram.usagePercentage),
                isEmphasized = true
            )
            ClassicInfoRow(
                label = "حد انخفاض الذاكرة (Threshold)",
                value = HardwareRepository.formatBytes(snapshot.ram.thresholdBytes)
            )
            ClassicInfoRow(
                label = "حالة الذاكرة المنخفضة (Low Memory)",
                value = if (snapshot.ram.isLowMemory) "نعم (تحذير)" else "لا (طبيعي مستقر)"
            )
            ClassicInfoRow(
                label = "الحد الأقصى لتخصيص التطبيقات (Max Heap)",
                value = HardwareRepository.formatBytes(snapshot.ram.appMaxHeapBytes)
            )

            // Virtual RAM / Swap
            if (snapshot.ram.swapTotalBytes != null && snapshot.ram.swapTotalBytes > 0) {
                ClassicInfoRow(
                    label = "ذاكرة المبادلة / Swap (افتراضية)",
                    value = "${HardwareRepository.formatBytes(snapshot.ram.swapTotalBytes)} (متاح: ${HardwareRepository.formatBytes(snapshot.ram.swapFreeBytes ?: 0)})"
                )
            } else {
                ClassicInfoRow(
                    label = "ذاكرة المبادلة / Swap (افتراضية)",
                    value = "غير نشطة أو غير متاحة"
                )
            }

            // =========================================================
            // NEW RAM FEATURE: Running Apps & Games in Background
            // =========================================================
            Spacer(modifier = Modifier.height(12.dp))
            ClassicSectionHeader(title = "تطبيقات وألعاب تعمل في الخلفية (Background Apps)")

            val runningApps = snapshot.runningProcesses
            val userAppsCount = runningApps.count { !it.isSystemApp }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "اكتشاف التطبيقات والألعاب النشطة",
                            color = ClassicTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تم العثور على $userAppsCount تطبيق مستخدم قيد التشغيل بالخلفية",
                            color = ClassicTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (userAppsCount > 0) {
                        Button(
                            onClick = {
                                onKillAllProcesses()
                                Toast.makeText(context, "تم إيقاف تطبيقات الخلفية وتحرير الذاكرة", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClassicCleanAllBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("إيقاف الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ClassicBorder)
            )

            if (runningApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد تطبيقات إضافية تعمل في الخلفية حالياً.",
                        color = ClassicTextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                runningApps.take(15).forEach { proc ->
                    RunningProcessRow(
                        item = proc,
                        onKill = {
                            onKillProcess(proc.packageName)
                            Toast.makeText(context, "تم إيقاف ${proc.appName}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================
            // 2. Storage Section (قسم مساحة التخزين)
            // =========================================================
            ClassicSectionHeader(title = "2. مساحة التخزين الداخلية (Internal Storage)")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "استخدام التخزين الداخلي",
                        color = ClassicTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", snapshot.storage.usagePercentage),
                        color = ClassicBarUsed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                ClassicHorizontalProgressBar(
                    usagePercentage = snapshot.storage.usagePercentage,
                    barHeight = 22.dp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المستخدم: ${HardwareRepository.formatBytesGbOnly(snapshot.storage.usedBytes)}",
                        color = ClassicTextSecondary,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = "المتاح: ${HardwareRepository.formatBytesGbOnly(snapshot.storage.availableBytes)}",
                        color = ClassicTextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ClassicBorder)
            )

            // Details in exact order required by prompt:
            ClassicInfoRow(
                label = "الإجمالي",
                value = HardwareRepository.formatBytesGbOnly(snapshot.storage.totalBytes),
                isEmphasized = true
            )
            ClassicInfoRow(
                label = "المستخدم",
                value = HardwareRepository.formatBytesGbOnly(snapshot.storage.usedBytes),
                valueColor = ClassicBarUsed
            )
            ClassicInfoRow(
                label = "المتاح",
                value = HardwareRepository.formatBytesGbOnly(snapshot.storage.availableBytes),
                valueColor = ClassicDarkCyan
            )
            ClassicInfoRow(
                label = "نسبة الاستخدام",
                value = String.format(Locale.US, "%.1f%%", snapshot.storage.usagePercentage),
                isEmphasized = true
            )
            ClassicInfoRow(
                label = "المساحة الحرة غير المحجوزة",
                value = HardwareRepository.formatBytesGbOnly(snapshot.storage.freeBytes)
            )
            ClassicInfoRow(
                label = "مسار التخزين الداخلي",
                value = snapshot.storage.path
            )

            // SD Card detection section
            if (snapshot.storage.hasSdCard && snapshot.storage.sdCardTotalBytes != null) {
                ClassicSectionHeader(title = "بطاقة الذاكرة الخارجية (SD Card)")
                ClassicInfoRow(
                    label = "حالة بطاقة SD",
                    value = "متصلة ومثبتة",
                    valueColor = ClassicDarkCyan
                )
                ClassicInfoRow(
                    label = "إجمالي مساحة SD",
                    value = HardwareRepository.formatBytesGbOnly(snapshot.storage.sdCardTotalBytes)
                )
                ClassicInfoRow(
                    label = "المساحة الحرة على SD",
                    value = HardwareRepository.formatBytesGbOnly(snapshot.storage.sdCardFreeBytes ?: 0)
                )
            } else {
                ClassicInfoRow(
                    label = "بطاقة الذاكرة الخارجية (SD Card)",
                    value = "لا توجد بطاقة SD مدخلة"
                )
            }

            // =========================================================
            // NEW STORAGE FEATURE: Clean Old Files (>= 6 Months)
            // =========================================================
            Spacer(modifier = Modifier.height(14.dp))
            ClassicSectionHeader(title = "تنظيف الملفات القديمة (من 6 أشهر فأكثر)")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(14.dp)
            ) {
                Text(
                    text = "فحص الملفات والصور والوسائط القديمة",
                    color = ClassicTextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "عرض الملفات التي لم تُعدّل منذ أكثر من 6 أشهر (180 يوماً) لإمكانية حذفها وتوفير المساحة.",
                    color = ClassicTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Select All + Delete Selected
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = allOldFiles.isNotEmpty() && selectedFileIds.size == allOldFiles.size

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (allSelected) {
                                    selectedFileIds.clear()
                                } else {
                                    selectedFileIds.clear()
                                    selectedFileIds.addAll(allOldFiles.map { it.id })
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedFileIds.clear()
                                    selectedFileIds.addAll(allOldFiles.map { it.id })
                                } else {
                                    selectedFileIds.clear()
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = ClassicCyan)
                        )
                        Text(
                            text = if (allSelected) "إلغاء التحديد" else "تحديد الكل (${allOldFiles.size})",
                            color = ClassicTextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (selectedFilesList.isNotEmpty()) {
                                showDeleteConfirmDialog = true
                            } else {
                                Toast.makeText(context, "الرجاء تحديد ملف واحد على الأقل", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = selectedFilesList.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClassicRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حذف المحدد (${HardwareRepository.formatBytes(totalSelectedBytes)})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ClassicBorder)
            )

            if (allOldFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ClassicGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لا توجد ملفات يزيد عمرها عن 6 أشهر حالياً في مساحة التخزين المتاحة.",
                            color = ClassicTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "هاتفك منظم ولا يحتاج إلى تفريغ ملفات قديمة.",
                            color = ClassicTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            } else {
                allOldFiles.forEach { file ->
                    val isChecked = selectedFileIds.contains(file.id)
                    OldFileRow(
                        file = file,
                        isChecked = isChecked,
                        onToggle = {
                            if (isChecked) {
                                selectedFileIds.remove(file.id)
                            } else {
                                selectedFileIds.add(file.id)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation Dialog before deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "تأكيد حذف الملفات القديمة",
                    color = ClassicTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف ${selectedFilesList.size} ملف (بحجم إجمالي ${HardwareRepository.formatBytes(totalSelectedBytes)})؟ لا يمكن التراجع عن هذا الإجراء.",
                    color = ClassicTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteFiles(selectedFilesList)
                        selectedFileIds.clear()
                    }
                ) {
                    Text("نعم، احذف الآن", color = ClassicRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء", color = ClassicTextPrimary)
                }
            }
        )
    }
}

@Composable
private fun RunningProcessRow(
    item: RunningAppProcessInfoItem,
    onKill: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("proc_${item.packageName}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.categoryName.contains("لعبة")) Icons.Default.Games else Icons.Default.Apps,
                contentDescription = null,
                tint = if (item.categoryName.contains("لعبة")) ClassicGreen else ClassicDarkCyan,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.appName,
                        color = ClassicTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEBF5FB), RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.categoryName,
                            color = ClassicDarkCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.packageName} • ${item.importanceText}",
                    color = ClassicTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (item.ramUsageBytes > 0) {
                    Text(
                        text = HardwareRepository.formatBytes(item.ramUsageBytes),
                        color = ClassicBarUsed,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!item.isSystemApp) {
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = onKill,
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("إيقاف", fontSize = 11.sp, color = ClassicRed)
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ClassicBorderLight)
    )
}

@Composable
private fun OldFileRow(
    file: OldFileInfo,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isChecked) Color(0xFFF0F9FF) else Color.White)
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("file_${file.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = ClassicCyan)
            )

            Spacer(modifier = Modifier.width(6.dp))

            val icon = when {
                file.isImage -> Icons.Default.Image
                file.isVideo -> Icons.Default.Movie
                file.isDocument -> Icons.Default.Description
                else -> Icons.Default.Description
            }
            val iconTint = when {
                file.isImage -> ClassicCyan
                file.isVideo -> ClassicRed
                file.isDocument -> ClassicDarkCyan
                else -> ClassicTextSecondary
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = ClassicTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "منذ ${file.ageInMonths} أشهر • ${HardwareRepository.formatDate(file.lastModifiedMillis)}",
                    color = ClassicTextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = HardwareRepository.formatBytes(file.sizeBytes),
                color = ClassicDarkCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ClassicBorderLight)
    )
}
