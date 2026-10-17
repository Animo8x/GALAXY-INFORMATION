package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CameraItemInfo
import com.example.data.HardwareSnapshot
import com.example.ui.components.ClassicInfoRow
import com.example.ui.components.ClassicSectionHeader
import com.example.ui.theme.ClassicBorder
import com.example.ui.theme.ClassicDarkCyan
import com.example.ui.theme.ClassicGrayBg
import com.example.ui.theme.ClassicGreen
import com.example.ui.theme.ClassicTextPrimary
import com.example.ui.theme.ClassicTextSecondary
import java.util.Locale

@Composable
fun CamerasScreen(
    snapshot: HardwareSnapshot,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val cameras = snapshot.cameras

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
                .testTag("cameras_screen")
        ) {
            ClassicSectionHeader(title = "قسم الكاميرات ومستشعرات التصوير")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "الكاميرات",
                        tint = ClassicDarkCyan
                    )
                    Spacer(modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text(
                            text = "إجمالي الكاميرات المكتشفة: ${cameras.totalCameras}",
                            color = ClassicTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الكاميرات الخلفية: ${cameras.backCamerasCount} • الكاميرات الأمامية: ${cameras.frontCamerasCount}",
                            color = ClassicTextSecondary,
                            fontSize = 12.5.sp
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

            Spacer(modifier = Modifier.height(10.dp))

            if (cameras.camerasList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تعذر الوصول إلى واجهة الكاميرات أو لا توجد كاميرات مسجلة.",
                        color = ClassicTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                cameras.camerasList.forEachIndexed { index, cam ->
                    CameraDetailCard(index = index + 1, camera = cam)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Reliability / Accuracy Disclaimer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(Color(0xFFEBF5FB), RoundedCornerShape(2.dp))
                    .border(1.dp, Color(0xFFD4E6F1), RoundedCornerShape(2.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "شرط الدقة والموثوقية (Camera2 API):",
                        color = ClassicDarkCyan,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "جميع المعلومات أعلاه مستخرجة مباشرة من CameraCharacteristics الرسمية لنظام أندرويد. لم يتم تخمين أو وضع أي أرقام افتراضية. دقة الميجابكسل محسوبة من أكبر أبعاد فعلية مخصصة للصور (JPEG Output Sizes).",
                        color = ClassicTextPrimary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CameraDetailCard(index: Int, camera: CameraItemInfo) {
    val title = if (camera.isBack) "الكاميرا الخلفية ($index)" else "الكاميرا الأمامية ($index)"

    Column(modifier = Modifier.fillMaxWidth()) {
        ClassicSectionHeader(title = "$title • المعرف [ID: ${camera.id}]")

        ClassicInfoRow(
            label = "الموقع والاتجاه",
            value = camera.facing,
            isEmphasized = true
        )

        val mpText = if (camera.resolutionMegapixels > 0) {
            String.format(Locale.US, "%.1f ميجابكسل (MP)", camera.resolutionMegapixels)
        } else {
            "غير متاح"
        }
        ClassicInfoRow(
            label = "الدقة القصوى للصور",
            value = mpText,
            isEmphasized = true,
            valueColor = ClassicDarkCyan
        )
        ClassicInfoRow(
            label = "أبعاد الصورة القصوى",
            value = camera.maxResolutionSize
        )
        ClassicInfoRow(
            label = "مصفوفة المستشعر (Active Array)",
            value = camera.sensorActiveArraySize
        )
        ClassicInfoRow(
            label = "نطاق التقريب (Zoom Range)",
            value = camera.zoomDescription
        )
        ClassicInfoRow(
            label = "تقنية HDR",
            value = if (camera.isHdrSupported) "مدعومة (HDR Scene Available)" else "غير مدعومة عبر هذا المسار",
            valueColor = if (camera.isHdrSupported) ClassicGreen else ClassicTextPrimary
        )
        ClassicInfoRow(
            label = "دعم تصوير خام (RAW)",
            value = if (camera.isRawSupported) "مدعوم (RAW DNG Capture)" else "غير مدعوم",
            valueColor = if (camera.isRawSupported) ClassicGreen else ClassicTextPrimary
        )
        ClassicInfoRow(
            label = "أعلى معدل إطارات (Max FPS)",
            value = "${camera.maxFps} FPS"
        )
        ClassicInfoRow(
            label = "دقات الفيديو المدعومة",
            value = camera.videoResolutions.joinToString(", ")
        )
        ClassicInfoRow(
            label = "التثبيت البصري (OIS)",
            value = if (camera.hasOis) "مدعوم (Optical Stabilization)" else "غير مدعوم",
            valueColor = if (camera.hasOis) ClassicGreen else ClassicTextPrimary
        )
        ClassicInfoRow(
            label = "التثبيت الإلكتروني (EIS)",
            value = if (camera.hasEis) "مدعوم (Electronic Stabilization)" else "غير مدعوم"
        )
        ClassicInfoRow(
            label = "فلاش الكاميرا",
            value = if (camera.hasFlash) "متوفر" else "غير متوفر"
        )
        ClassicInfoRow(
            label = "أوضاع التركيز (Focus Modes)",
            value = camera.focusModes.joinToString("، ")
        )

        if (camera.physicalCameraIds.isNotEmpty()) {
            ClassicInfoRow(
                label = "الكاميرات الفيزيائية المرتبطة",
                value = camera.physicalCameraIds.joinToString(", ")
            )
        }
    }
}
