package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClassicBarAvail
import com.example.ui.theme.ClassicBarBg
import com.example.ui.theme.ClassicBarUsed
import com.example.ui.theme.ClassicBorder
import com.example.ui.theme.ClassicBorderLight
import com.example.ui.theme.ClassicCyan
import com.example.ui.theme.ClassicDarkCyan
import com.example.ui.theme.ClassicGreen
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ClassicTextPrimary
import com.example.ui.theme.ClassicTextSecondary

/**
 * Top App Bar with safe status bar padding so system notifications, clock, and battery
 * never overlap the bar across any device screen size.
 */
@Composable
fun ClassicTopBar(
    title: String,
    subtitle: String? = null,
    onRefresh: () -> Unit,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClassicCyan)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("classic_top_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "تحديث البيانات",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Classic upright battery icon drawn with Canvas.
 * - Top terminal metallic cap
 * - Crisp outer rectangle
 * - Proportional fill: Green (>20%), Red (<=20%)
 * - Text percentage inside or alongside
 * - Charging bolt glyph when charging
 */
@Composable
fun ClassicBatteryCanvas(
    percentage: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    showPercentageInside: Boolean = true,
    width: Dp = 68.dp,
    height: Dp = 96.dp
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percentage.toFloat().coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 400),
        label = "battery_percent"
    )

    val fillColor = if (percentage > 20) ClassicGreen else ClassicRed
    val borderColor = Color(0xFF607D8B)
    val capColor = Color(0xFF78909C)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .testTag("classic_battery_icon"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Dimensions of top terminal cap
            val capW = canvasW * 0.38f
            val capH = canvasH * 0.08f
            val capLeft = (canvasW - capW) / 2f
            val capTop = 0f

            // Battery body dimensions
            val bodyLeft = canvasW * 0.08f
            val bodyRight = canvasW * 0.92f
            val bodyW = bodyRight - bodyLeft
            val bodyTop = capH
            val bodyBottom = canvasH - 2f
            val bodyH = bodyBottom - bodyTop
            val strokeWidth = 3f

            // 1. Draw top metallic cap
            drawRoundRect(
                color = capColor,
                topLeft = Offset(capLeft, capTop),
                size = Size(capW, capH),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // 2. Draw battery background (white)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // 3. Draw fill based on animatedPercent from bottom to top
            val innerPadding = 4f
            val fillMaxH = bodyH - (innerPadding * 2)
            val fillW = bodyW - (innerPadding * 2)
            val fillHeightActual = (fillMaxH * (animatedPercent / 100f)).coerceAtLeast(0f)
            val fillTop = (bodyTop + innerPadding) + (fillMaxH - fillHeightActual)

            if (fillHeightActual > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(bodyLeft + innerPadding, fillTop),
                    size = Size(fillW, fillHeightActual),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            // 4. Draw crisp outer border
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = strokeWidth)
            )

            // 5. Draw charging lightning bolt if charging
            if (isCharging) {
                drawChargingBolt(
                    centerX = canvasW / 2f,
                    centerY = bodyTop + (bodyH / 2f),
                    boltSize = bodyW * 0.45f
                )
            }
        }

        // Percentage text inside the battery body if space allows
        if (showPercentageInside && !isCharging) {
            Text(
                text = "$percentage%",
                color = if (percentage > 50) Color.White else ClassicTextPrimary,
                fontSize = (width.value * 0.26f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun DrawScope.drawChargingBolt(centerX: Float, centerY: Float, boltSize: Float) {
    val half = boltSize / 2f
    val boltPath = Path().apply {
        moveTo(centerX + half * 0.1f, centerY - half)
        lineTo(centerX - half * 0.7f, centerY + half * 0.05f)
        lineTo(centerX - half * 0.05f, centerY + half * 0.05f)
        lineTo(centerX - half * 0.2f, centerY + half)
        lineTo(centerX + half * 0.7f, centerY - half * 0.15f)
        lineTo(centerX + half * 0.05f, centerY - half * 0.15f)
        close()
    }
    drawPath(path = boltPath, color = Color(0x80000000), style = Stroke(width = 3f))
    drawPath(path = boltPath, color = Color(0xFFFFEB3B), style = Fill)
}

/**
 * Classic horizontal progress bar requested in Section 1 (RAM) & Section 2 (Storage).
 * - Blue used section
 * - White/light available section
 * - Rectangular with minimal 2dp border.
 */
@Composable
fun ClassicHorizontalProgressBar(
    usagePercentage: Float,
    modifier: Modifier = Modifier,
    barHeight: Dp = 16.dp,
    barColor: Color = ClassicBarUsed,
    backgroundColor: Color = ClassicBarAvail,
    borderColor: Color = ClassicBorder
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (usagePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "h_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .background(backgroundColor, RoundedCornerShape(2.dp))
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .testTag("horizontal_progress_bar")
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
            )
        }
    }
}

/**
 * Circular progress gauge styled in classic manager layout.
 * Features the circular arc, bold percentage text, and label.
 */
@Composable
fun ClassicCircularGauge(
    percentage: Float,
    label: String = "Used",
    subLabel: String = "",
    strokeWidth: Dp = 12.dp,
    size: Dp = 105.dp,
    color: Color = ClassicCyan,
    modifier: Modifier = Modifier
) {
    val animatedRatio by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450),
        label = "circular_ratio"
    )

    Column(
        modifier = modifier.testTag("classic_circular_gauge"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
                drawArc(
                    color = ClassicBarBg,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )

                val sweep = 360f * animatedRatio
                if (sweep > 0f) {
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${percentage.toInt()}%",
                    color = ClassicCyan,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = ClassicCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (subLabel.isNotBlank()) {
            Text(
                text = subLabel,
                color = ClassicTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Classic section header with thin line divider
 */
@Composable
fun ClassicSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEBF1F5))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("section_header_$title")
    ) {
        Text(
            text = title,
            color = ClassicDarkCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ClassicBorder)
    )
}

/**
 * Classic Information Row
 */
@Composable
fun ClassicInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isEmphasized: Boolean = false,
    valueColor: Color = ClassicTextPrimary
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag("info_row_${label.take(10)}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isEmphasized) ClassicTextPrimary else ClassicTextSecondary,
                fontSize = 13.5.sp,
                fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.End
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

/**
 * Classic Security / Device Shield Icon
 */
@Composable
fun ClassicShieldIcon(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    tint: Color = ClassicCyan
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "حالة أمان الجهاز",
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
    }
}
