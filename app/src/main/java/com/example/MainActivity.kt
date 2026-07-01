package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.WaterDatabase
import com.example.data.WaterRecord
import com.example.data.WaterRepository
import com.example.ui.AppScreen
import com.example.ui.WaterViewModel
import com.example.ui.WaterViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = WaterDatabase.getDatabase(this)
        val repository = WaterRepository(database.waterDao())
        val viewModelFactory = WaterViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: WaterViewModel = viewModel(factory = viewModelFactory)
                WaterTrackerApp(viewModel)
            }
        }
    }
}

@Composable
fun WaterTrackerApp(viewModel: WaterViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val historyRecords by viewModel.allHistory.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        record = todayRecord,
                        history = historyRecords,
                        onDrinkClick = { viewModel.drinkCup() },
                        onResetClick = { viewModel.resetToday() },
                        onGoalChange = { viewModel.adjustGoal(it) }
                    )
                }
                AppScreen.SUCCESS -> {
                    SuccessScreen(
                        record = todayRecord,
                        onBackToHome = { viewModel.navigateTo(AppScreen.HOME) },
                        onDrinkMore = { viewModel.drinkCup() },
                        onReset = { viewModel.resetToday() }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    record: WaterRecord,
    history: List<WaterRecord>,
    onDrinkClick: () -> Unit,
    onResetClick: () -> Unit,
    onGoalChange: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    // Encapsulate bounce state for drink button click
    var buttonScaleState by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = buttonScaleState,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = {
            if (buttonScaleState == 0.9f) {
                buttonScaleState = 1.0f
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "水滴时刻",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "喝水打卡 · 保持健康",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "日期",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Progress Arc Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WaterProgressIndicator(cups = record.cups, goal = record.goal)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "已完成 ${record.cups} / ${record.goal} 杯",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("progress_text")
                )

                Spacer(modifier = Modifier.height(6.dp))

                val encouragement = getEncouragementText(record.cups, record.goal)
                Text(
                    text = encouragement,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lit Cup Icons Grid Header
        Text(
            text = "今日进度面板",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Cup Icons List representing the goal
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // We show an adaptive cup grid
                val maxColumns = 4
                val totalCups = record.goal
                val rows = (totalCups + maxColumns - 1) / maxColumns

                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until maxColumns) {
                            val index = row * maxColumns + col
                            if (index < totalCups) {
                                val isLit = index < record.cups
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    WaterCupIcon(
                                        isLit = isLit,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .testTag("cup_item_$index")
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${index + 1} 杯",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            } else {
                                // Empty spacer to balance the grid row
                                Spacer(modifier = Modifier.size(54.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Huge Button for "喝了一杯"
        Button(
            onClick = {
                buttonScaleState = 0.9f
                onDrinkClick()
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(64.dp)
                .scale(animatedScale)
                .shadow(12.dp, CircleShape)
                .testTag("drink_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = CircleShape
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.img_water_hero),
                    contentDescription = "Drink icon",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "喝了一杯",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Target adjustment and reset panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "设置与操作",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Goal adjustment row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "今日目标",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onGoalChange(record.goal - 1) },
                            enabled = record.goal > 4,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = "—",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "${record.goal} 杯",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { onGoalChange(record.goal + 1) },
                            enabled = record.goal < 20,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "增加目标",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Reset Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "重置今日进度",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "清空今日记录并重新开始打卡",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Button(
                        onClick = onResetClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重置",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "清空",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History logs
        if (history.isNotEmpty()) {
            Text(
                text = "打卡历史记录",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    history.take(5).forEachIndexed { index, histRecord ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (histRecord.cups >= histRecord.goal) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = histRecord.date,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${histRecord.cups} / ${histRecord.goal} 杯",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (histRecord.cups >= histRecord.goal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (histRecord.cups >= histRecord.goal) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "达成",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (index < history.take(5).size - 1) {
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessScreen(
    record: WaterRecord,
    onBackToHome: () -> Unit,
    onDrinkMore: () -> Unit,
    onReset: () -> Unit
) {
    // Beautiful celebratory success screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Star badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "今日喝水目标达成！",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "你已经完成了今天的 ${record.goal} 杯水目标！\n身体因你的自律和好习惯而活力充沛！",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Success Illustration
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_water_success),
                contentDescription = "Success",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Interactive button options
        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
                .testTag("back_home_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "返回今日控制台",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onDrinkMore,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .padding(end = 6.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "+1杯",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .padding(start = 6.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "重新开始",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun WaterProgressIndicator(cups: Int, goal: Int, modifier: Modifier = Modifier) {
    val progress = (cups.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )

    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(190.dp)
    ) {
        // Outer circular track and progress
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            // Track background
            drawCircle(
                color = trackColor,
                radius = size.minDimension / 2 - strokeWidth,
                style = Stroke(width = strokeWidth)
            )
            // Progress Arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(strokeWidth, strokeWidth),
                size = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center Content with embedded circular hero avatar
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_water_hero),
                contentDescription = "Water drop",
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Transparent overlay with the text percentage
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun WaterCupIcon(isLit: Boolean, modifier: Modifier = Modifier) {
    val animateColor by animateColorAsState(
        targetValue = if (isLit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        animationSpec = tween(durationMillis = 350)
    )

    val shineColor = if (isLit) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Draw Glass Outline Path
        val glassPath = Path().apply {
            moveTo(width * 0.22f, height * 0.15f)
            lineTo(width * 0.28f, height * 0.82f)
            quadraticTo(width * 0.30f, height * 0.88f, width * 0.38f, height * 0.88f)
            lineTo(width * 0.62f, height * 0.88f)
            quadraticTo(width * 0.70f, height * 0.88f, width * 0.72f, height * 0.82f)
            lineTo(width * 0.78f, height * 0.15f)
        }

        // Draw Water Fill if Lit
        if (isLit) {
            val waterPath = Path().apply {
                moveTo(width * 0.24f, height * 0.36f)
                // Wavy water level
                quadraticTo(width * 0.37f, height * 0.32f, width * 0.50f, height * 0.36f)
                quadraticTo(width * 0.63f, height * 0.40f, width * 0.76f, height * 0.36f)
                lineTo(width * 0.71f, height * 0.81f)
                quadraticTo(width * 0.70f, height * 0.86f, width * 0.62f, height * 0.86f)
                lineTo(width * 0.38f, height * 0.86f)
                quadraticTo(width * 0.30f, height * 0.86f, 0.29f * width, height * 0.81f)
                close()
            }
            drawPath(path = waterPath, color = Color(0xFF29B6F6))
        }

        // Draw Glass Outline Stroke
        drawPath(
            path = glassPath,
            color = animateColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw dynamic water bubbles / sparkle inside the filled glass
        if (isLit) {
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2.dp.toPx(),
                center = Offset(width * 0.40f, height * 0.55f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 1.5.dp.toPx(),
                center = Offset(width * 0.62f, height * 0.65f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 1.dp.toPx(),
                center = Offset(width * 0.48f, height * 0.72f)
            )
        }

        // Draw elegant reflection line on left side
        val shinePath = Path().apply {
            moveTo(width * 0.28f, height * 0.24f)
            lineTo(width * 0.32f, height * 0.74f)
        }
        drawPath(
            path = shinePath,
            color = shineColor,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun getEncouragementText(cups: Int, goal: Int): String {
    if (cups == 0) return "开始你健康的一天，喝第一杯水吧！💧"
    val percent = cups.toFloat() / goal.toFloat()
    return when {
        percent >= 1.0f -> "太棒了！已达成今日饮水目标！🏆"
        percent >= 0.75f -> "完成大半啦，身体正在感谢你！🌟"
        percent >= 0.5f -> "半程已过！今天感觉神清气爽！⚡"
        percent >= 0.25f -> "走在健康的路上，继续保持！🍃"
        else -> "很好的开端！多喝水有益健康哦！🥤"
    }
}
