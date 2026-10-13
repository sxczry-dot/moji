package com.autobook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autobook.app.ui.list.TransactionListScreen
import com.autobook.app.ui.settings.SettingsScreen
import com.autobook.app.ui.stats.StatsScreen

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("明细", Icons.Filled.List),
    TabItem("统计", Icons.Filled.Assessment),
    TabItem("设置", Icons.Filled.Settings)
)

@Composable
fun MainScreen() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var scrollTopTick by remember { mutableIntStateOf(0) }
    var statsTick by remember { mutableIntStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.autobook.app.MainActivity.openTabRequests.collect { tab ->
            if (tab >= 0) {
                selected = tab
                // 消费后复位，避免每次进入组合（解锁/切页回来）都被拉回指定 tab
                com.autobook.app.MainActivity.openTabRequests.value = -1
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(selected) {
        if (selected == 1) statsTick++
    }
    val viewModel: com.autobook.app.viewmodel.MainViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * fontScale)
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f),
                            spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    NavigationBar(containerColor = androidx.compose.ui.graphics.Color.Transparent) {
                        androidx.compose.foundation.layout.BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            val tabWidth = maxWidth / 3
                            val sliderX by androidx.compose.animation.core.animateDpAsState(
                                targetValue = tabWidth * selected,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                ),
                                label = "nav_slider"
                            )
                            Box(
                                modifier = Modifier
                                    .offset(x = sliderX)
                                    .width(tabWidth)
                                    .height(64.dp)
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                            )
                                        )
                                    )
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                tabs.forEachIndexed { index, tab ->
                                    val itemSelected = selected == index
                                    val interactionSource = remember {
                                        androidx.compose.foundation.interaction.MutableInteractionSource()
                                    }
                                    val pressed by interactionSource.collectIsPressedAsState()
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(64.dp)
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                if (itemSelected) scrollTopTick++ else selected = index
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.graphicsLayer {
                                                val scale = if (pressed) 0.94f else 1f
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                        ) {
                                            val iconScale by androidx.compose.animation.core.animateFloatAsState(
                                                targetValue = if (itemSelected) 1.15f else 1f,
                                                animationSpec = androidx.compose.animation.core.spring(
                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                                ),
                                                label = "nav_icon_scale"
                                            )
                                            Icon(
                                                tab.icon,
                                                contentDescription = tab.label,
                                                tint = if (itemSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                }
                                            )
                                            Text(
                                                text = tab.label,
                                                fontSize = 11.sp,
                                                color = if (itemSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                pageCount = { tabs.size }
            )
            androidx.compose.runtime.LaunchedEffect(selected) {
                if (pagerState.currentPage != selected) {
                    pagerState.scrollToPage(selected)
                }
            }
            androidx.compose.runtime.LaunchedEffect(pagerState) {
                androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
                    if (page != selected) selected = page
                }
            }
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) { page ->
                when (page) {
                    0 -> TransactionListScreen(scrollTopTick = scrollTopTick)
                    1 -> StatsScreen(statsTick = statsTick)
                    else -> SettingsScreen()
                }
            }
        }
    }
}
