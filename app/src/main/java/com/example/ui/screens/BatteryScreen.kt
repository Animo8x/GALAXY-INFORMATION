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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HardwareSnapshot
import com.example.ui.components.ClassicBatteryCanvas
import com.example.ui.components.ClassicInfoRow
import com.example.ui.components.ClassicSectionHeader
import com.example.ui.theme.ClassicBorder
import com.example.ui.theme.ClassicGrayBg
import com.example.ui.theme.ClassicGreen
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ClassicTextPrimary
import com.example.ui.theme.ClassicTextSecondary
import java.util.Locale

@Composable
fun BatteryScreen(
    snapshot: HardwareSnapshot,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val battery = snapshot.battery
    val statusColor = if (battery.levelPercentage > 20) ClassicGreen else ClassicRed

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
                .testTag("battery_screen")
        ) {
            ClassicSectionHeader(title = "قسم البطارية وحالة الطاقة")

            // Top Battery Showcase Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ClassicBatteryCanvas(
                        percentage = battery.levelPercentage,
                        isCharging = battery.isCharging,
                        width = 82.dp,
                        height = 118.dp,
                        showPercentageInside = true
                    )

                    Spacer(modifier = Modifier.width(22.dp))

                    Column {
                        Text(
                            text = "${battery.levelPercentage}%",
                            color = statusColor,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = if (battery.isCharging) "جاري الشحن" else "مستوى الشحن الحالي",
                            color = ClassicTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (battery.levelPercentage <= 20) "تحذير: طاقة البطارية منخفضة" else "طاقة كافية للاستخدام",
                            color = if (battery.levelPercentage <= 20) ClassicRed else ClassicTextSecondary,
                            fontSize = 12.sp
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

            ClassicSectionHeader(title = "تفاصيل البطارية الحالية")

            ClassicInfoRow(
                label = "مستوى الشحن الحالي",
                value = "${battery.levelPercentage}%",
                isEmphasized = true,
                valueColor = statusColor
            )
            ClassicInfoRow(
                label = "حالة البطارية",
                value = battery.statusText,
                isEmphasized = true
            )
            ClassicInfoRow(
                label = "مصدر الطاقة",
                value = battery.powerSource
            )
            ClassicInfoRow(
                label = "حالة صحة البطارية",
                value = battery.healthText,
                valueColor = if (battery.healthText.contains("جيدة")) ClassicGreen else ClassicTextPrimary
            )
            ClassicInfoRow(
                label = "درجة الحرارة",
                value = String.format(Locale.US, "%.1f °C", battery.temperatureCelsius)
            )
            ClassicInfoRow(
                label = "الجهد الكهربائي",
                value = String.format(Locale.US, "%.3f V (%d mV)", battery.voltageVolts, battery.voltageMv)
            )
            ClassicInfoRow(
                label = "تقنية البطارية",
                value = battery.technology
            )
            ClassicInfoRow(
                label = "السعة المقدرة / المصنّعة",
                value = battery.capacityInfo
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Battery Health Notice Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(Color(0xFFFFFDE7), RoundedCornerShape(2.dp))
                    .border(1.dp, Color(0xFFFFF59D), RoundedCornerShape(2.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "تنبيه حول صحة البطارية (Battery Health):",
                        color = Color(0xFFF57F17),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يجب التمييز بوضوح بين نسبة الشحن الحالية (${battery.levelPercentage}%) وصحة البطارية العامة. نسبة الشحن تمثل كمية الطاقة الكهربائية المخزنة حالياً، ولا تعبر عن درجة التآكل أو السعة الكيميائية المتدهورة. تقرأ البيانات مباشرة من Intent.ACTION_BATTERY_CHANGED الرسمي.",
                        color = ClassicTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
