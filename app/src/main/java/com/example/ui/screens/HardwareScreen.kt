package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HardwareSnapshot
import com.example.ui.components.ClassicInfoRow
import com.example.ui.components.ClassicSectionHeader
import com.example.ui.theme.ClassicDarkCyan
import com.example.ui.theme.ClassicGrayBg
import com.example.ui.theme.ClassicTextPrimary
import com.example.ui.theme.ClassicTextSecondary

@Composable
fun HardwareScreen(
    snapshot: HardwareSnapshot,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val sys = snapshot.system

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
                .testTag("hardware_screen")
        ) {
            // =========================================================
            // 1. Processor Group (المعالج)
            // =========================================================
            ClassicSectionHeader(title = "المعالج والأداء (Processor & Performance)")

            ClassicInfoRow(
                label = "اسم المعالج",
                value = sys.processorName,
                isEmphasized = true,
                valueColor = ClassicDarkCyan
            )
            ClassicInfoRow(
                label = "عدد الأنوية",
                value = "${sys.coreCount} أنوية فعلية",
                isEmphasized = true
            )
            ClassicInfoRow(
                label = "المعمارية",
                value = sys.cpuArchitecture
            )
            ClassicInfoRow(
                label = "المنصة / اللوحة الأم",
                value = "${sys.hardwareBoard} (${sys.buildBoard})"
            )
            if (sys.socModel != "غير متاح") {
                ClassicInfoRow(
                    label = "طراز شريحة SoC",
                    value = sys.socModel
                )
            }
            ClassicInfoRow(
                label = "معمارية البرامج المدعومة (ABIs)",
                value = sys.supportedAbis
            )
            ClassicInfoRow(
                label = "إصدار OpenGL ES (الرسوميات)",
                value = sys.openGlVersion,
                valueColor = ClassicDarkCyan
            )

            sys.cpuCores.forEach { core ->
                val freqText = when {
                    core.currentFreqKhz != null -> "${core.currentFreqKhz / 1000} MHz"
                    core.maxFreqKhz != null -> "حتى ${core.maxFreqKhz / 1000} MHz"
                    else -> "نشطة (يديرها نواة النظام)"
                }
                ClassicInfoRow(
                    label = "النواة CPU ${core.coreIndex}",
                    value = freqText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================
            // 2. System Group (النظام)
            // =========================================================
            ClassicSectionHeader(title = "النظام والبرمجيات (Android System)")

            ClassicInfoRow(
                label = "إصدار أندرويد",
                value = "Android ${sys.androidVersion}",
                isEmphasized = true,
                valueColor = ClassicDarkCyan
            )
            ClassicInfoRow(
                label = "مستوى API",
                value = "API ${sys.apiLevel}",
                isEmphasized = true
            )
            ClassicInfoRow(
                label = "إصدار النواة (Kernel)",
                value = sys.kernelVersion
            )
            ClassicInfoRow(
                label = "مستوى تصحيح الأمان",
                value = sys.securityPatch
            )
            ClassicInfoRow(
                label = "رقم البناء (Build ID)",
                value = sys.buildId
            )

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================
            // 3. Device Group (الجهاز)
            // =========================================================
            ClassicSectionHeader(title = "معلومات الجهاز والمصنّع (Device Info)")

            ClassicInfoRow(
                label = "الشركة المصنّعة",
                value = sys.manufacturer.replaceFirstChar { it.uppercase() },
                isEmphasized = true
            )
            ClassicInfoRow(
                label = "العلامة التجارية",
                value = sys.brand
            )
            ClassicInfoRow(
                label = "الطراز (Model)",
                value = sys.model,
                isEmphasized = true,
                valueColor = ClassicDarkCyan
            )
            ClassicInfoRow(
                label = "رقم الطراز / المنتج",
                value = sys.modelNumber
            )
            ClassicInfoRow(
                label = "اسم الجهاز الداخلي (Build Device)",
                value = sys.buildDevice
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FBFC))
                    .padding(14.dp)
            ) {
                Text(
                    text = "جميع مواصفات المعالج والنظام مستخرجة من واجهات Build وRuntime وقيم النواة الحقيقية للجهاز دون افتراض أي طراز محدد.",
                    color = ClassicTextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
