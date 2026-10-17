package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.ClassicTopBar
import com.example.ui.screens.BatteryScreen
import com.example.ui.screens.CamerasScreen
import com.example.ui.screens.HardwareScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.RamStorageScreen
import com.example.ui.theme.ClassicBorder
import com.example.ui.theme.ClassicCyan
import com.example.ui.theme.ClassicDarkCyan
import com.example.ui.theme.ClassicGrayBg
import com.example.ui.theme.ClassicTextSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                GalaxyInformationApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun GalaxyInformationApp(viewModel: MainViewModel) {
    val snapshot by viewModel.snapshot.collectAsState()
    val selectedTab by viewModel.selectedTabIndex.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val isScanningFiles by viewModel.isScanningFiles.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val context = LocalContext.current

    val tabs = listOf(
        "نظرة عامة",
        "RAM والتخزين",
        "البطارية",
        "الكاميرات",
        "المعالج والجهاز"
    )

    // Handle system back navigation to return to Overview tab first
    BackHandler(enabled = selectedTab != 0) {
        viewModel.goBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClassicCyan)
            ) {
                // Classic Top Bar with statusBarsPadding inside ClassicTopBar
                ClassicTopBar(
                    title = "Galaxy Info",
                    subtitle = "${snapshot.system.model} • Android ${snapshot.system.androidVersion}",
                    onRefresh = {
                        viewModel.refreshAll()
                        Toast.makeText(context, "تم تحديث البيانات بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    showBackButton = selectedTab != 0,
                    onBack = { viewModel.goBack() }
                )

                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color.White,
                        trackColor = ClassicDarkCyan
                    )
                }

                // Classic Navigation Tabs (Adaptive width on large screens)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = ClassicCyan,
                        edgePadding = 12.dp,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    height = 3.dp,
                                    color = ClassicCyan
                                )
                            }
                        },
                        divider = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ClassicBorder)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .testTag("classic_tabs")
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(index) },
                                text = {
                                    Text(
                                        text = title,
                                        color = if (isSelected) ClassicCyan else ClassicTextSecondary,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag("tab_$index")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClassicGrayBg)
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> OverviewScreen(
                        snapshot = snapshot,
                        onNavigateToTab = { viewModel.selectTab(it) },
                        onRefresh = {
                            viewModel.refreshAll()
                            Toast.makeText(
                                context,
                                "تم تحديث البيانات بنجاح",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    1 -> RamStorageScreen(
                        snapshot = snapshot,
                        selectedPeriod = selectedPeriod,
                        isScanningFiles = isScanningFiles,
                        hasStoragePermission = hasStoragePermission,
                        onSelectPeriod = { viewModel.setPeriodAndScan(it) },
                        onRescanFiles = { viewModel.scanOldFiles() },
                        onPermissionGranted = { viewModel.updatePermissionStatus() },
                        onKillProcess = { viewModel.killProcess(it) },
                        onKillAllProcesses = {
                            viewModel.killAllBackgroundProcesses { count ->
                                Toast.makeText(context, "تم إيقاف $count تطبيق من تطبيقات الخلفية وتحرير الذاكرة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteFiles = { files ->
                            viewModel.deleteSelectedOldFiles(files) { count, bytes ->
                                val freedStr = com.example.data.HardwareRepository.formatBytes(bytes)
                                Toast.makeText(context, "تم حذف $count ملف بنجاح وتحرير $freedStr من مساحة التخزين", Toast.LENGTH_LONG).show()
                            }
                        },
                        onCreateTestFiles = {
                            viewModel.createSampleTestFilesAndScan()
                            Toast.makeText(context, "تم إنشاء ملفات تجريبية للاختبار بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    )
                    2 -> BatteryScreen(snapshot = snapshot)
                    3 -> CamerasScreen(snapshot = snapshot)
                    4 -> HardwareScreen(snapshot = snapshot)
                }
            }
        }
    }
}
