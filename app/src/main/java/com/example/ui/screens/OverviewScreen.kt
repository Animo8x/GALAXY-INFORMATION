package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.HardwareRepository
import com.example.data.HardwareSnapshot
import com.example.ui.components.ClassicBatteryCanvas
import com.example.ui.components.ClassicCircularGauge
import com.example.ui.components.ClassicSectionHeader
import com.example.ui.components.ClassicShieldIcon
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

@Composable
fun OverviewScreen(
    snapshot: HardwareSnapshot,
    onNavigateToTab: (Int) -> Unit,
    onCleanAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

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
                .testTag("overview_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Device Identification Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "الهاتف",
                    tint = ClassicDarkCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${snapshot.system.manufacturer.uppercase()} ${snapshot.system.model}",
                        color = ClassicTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Android ${snapshot.system.androidVersion} (API ${snapshot.system.apiLevel}) • ${snapshot.cameras.totalCameras} كاميرات",
                        color = ClassicTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ClassicBorder)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4-Quadrant Card Container (Classic Manager Layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
                    .border(1.dp, ClassicBorder, RoundedCornerShape(2.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Half: Battery (Left) | Storage (Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        // Quadrant 1: Battery
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp, horizontal = 8.dp)
                                .clickable { onNavigateToTab(2) }
                                .testTag("overview_card_battery"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Battery",
                                color = ClassicTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ClassicBatteryCanvas(
                                percentage = snapshot.battery.levelPercentage,
                                isCharging = snapshot.battery.isCharging,
                                width = 64.dp,
                                height = 84.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val labelColor = if (snapshot.battery.levelPercentage > 20) ClassicGreen else ClassicRed
                            Text(
                                text = if (snapshot.battery.isCharging) "Charging" else "Remaining",
                                color = labelColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(ClassicBorderLight)
                        )

                        // Quadrant 2: Storage
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp, horizontal = 8.dp)
                                .clickable { onNavigateToTab(1) }
                                .testTag("overview_card_storage"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Storage",
                                color = ClassicTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ClassicCircularGauge(
                                percentage = snapshot.storage.usagePercentage,
                                label = "Used",
                                subLabel = "${HardwareRepository.formatBytes(snapshot.storage.freeBytes)} free",
                                color = ClassicCyan,
                                size = 84.dp,
                                strokeWidth = 10.dp
                            )
                        }
                    }

                    // Horizontal Middle Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ClassicBorderLight)
                    )

                    // Bottom Half: RAM (Left) | Device Security (Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        // Quadrant 3: RAM
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp, horizontal = 8.dp)
                                .clickable { onNavigateToTab(1) }
                                .testTag("overview_card_ram"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "RAM",
                                color = ClassicTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ClassicCircularGauge(
                                percentage = snapshot.ram.usagePercentage,
                                label = "Used",
                                subLabel = "${HardwareRepository.formatBytes(snapshot.ram.availBytes)} free",
                                color = ClassicCyan,
                                size = 84.dp,
                                strokeWidth = 10.dp
                            )
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(ClassicBorderLight)
                        )

                        // Quadrant 4: Device Security & Hardware
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp, horizontal = 8.dp)
                                .clickable { onNavigateToTab(4) }
                                .testTag("overview_card_security"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Device status",
                                color = ClassicTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ClassicShieldIcon(
                                size = 84.dp,
                                tint = ClassicCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Secure / Normal",
                                color = ClassicCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CLEAN ALL Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Button(
                    onClick = onCleanAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("clean_all_button"),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClassicCleanAllBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "CLEAN ALL / تحديث وفحص",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Navigation Tiles to Detailed Screens
            ClassicSectionHeader(title = "الأقسام التفصيلية لمعلومات الجهاز")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                QuickNavRow(
                    icon = Icons.Default.Memory,
                    title = "ذاكرة RAM ومساحة التخزين",
                    subtitle = "أشرطة الاستخدام الأفقية والتفاصيل الدقيقة",
                    onClick = { onNavigateToTab(1) },
                    testTag = "nav_to_ram_storage"
                )
                QuickNavRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "حالة البطارية وتفاصيل الطاقة",
                    subtitle = "أيقونة البطارية الكلاسيكية، الفولت والحرارة",
                    onClick = { onNavigateToTab(2) },
                    testTag = "nav_to_battery"
                )
                QuickNavRow(
                    icon = Icons.Default.CameraAlt,
                    title = "الكاميرات الخلفية والأمامية",
                    subtitle = "${snapshot.cameras.backCamerasCount} خلفية • ${snapshot.cameras.frontCamerasCount} أمامية",
                    onClick = { onNavigateToTab(3) },
                    testTag = "nav_to_cameras"
                )
                QuickNavRow(
                    icon = Icons.Default.Storage,
                    title = "المعالج والأداء والنظام",
                    subtitle = "${snapshot.system.processorName} • ${snapshot.system.coreCount} Cores",
                    onClick = { onNavigateToTab(4) },
                    testTag = "nav_to_hardware"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ClassicDarkCyan,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ClassicTextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = ClassicTextSecondary,
                fontSize = 12.sp
            )
        }
        Text(
            text = "›",
            color = ClassicTextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ClassicBorderLight)
    )
}
