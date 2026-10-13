package com.theis.agrinovis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.theis.agrinovis.R
import com.theis.agrinovis.data.model.WeatherResponse
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.ui.theme.DarkHologramBG
import com.theis.agrinovis.ui.theme.GlassBackground
import com.theis.agrinovis.ui.theme.NeonGreen
import com.theis.agrinovis.ui.theme.getStageColorTheme
import com.theis.agrinovis.ui.utils.PlantUtils
import com.theis.agrinovis.visualization.threeD.LettuceScene

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHeader(
    farmerName: String, 
    date: String, 
    plantCount: Int = 0, 
    unreadCount: Int = 0,
    onNotificationClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A2E0C), DarkHologramBG)
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $farmerName",
                        color = NeonGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = date,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = Color(0xFFFF3D00),
                                contentColor = Color.White
                            )
                        }
                    }
                ) {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // System Quick Status (Replacing Search Bar)
            val isOnline by com.theis.agrinovis.data.remote.NetworkMonitor.isOnline.collectAsState()
            val isOffline = !isOnline || com.theis.agrinovis.data.session.MonitoringSession.isOfflineMode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        1.dp,
                        if (isOffline) Color(0xFFFFB300).copy(alpha = 0.4f) else NeonGreen.copy(alpha = 0.2f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = if (isOffline) Color(0xFFFFB300) else NeonGreen
                ) {}
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isOffline) "⚡ System Local (Offline Mode)" else "System Online",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$plantCount active assets",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherResponse?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBackground)
            .border(1.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = weather?.name ?: "SYNCING...", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text(text = "Environmental Node", fontSize = 11.sp, color = Color.White)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (weather?.weather?.firstOrNull()?.main?.contains("Rain") == true) Icons.Default.WaterDrop else Icons.Default.Cloud, 
                        contentDescription = null, 
                        tint = Color(0xFF00E5FF), 
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${weather?.main?.temp?.toInt() ?: "--"}°C", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem("Wind", "${weather?.wind?.speed ?: "--"} m/s", Icons.Default.Air)
                WeatherDetailItem("Pressure", "${weather?.main?.pressure ?: "--"} hPa", Icons.Default.Compress)
                WeatherDetailItem("Humidity", "${weather?.main?.humidity ?: "--"}%", Icons.Default.WaterDrop)
                WeatherDetailItem("Condition", weather?.weather?.firstOrNull()?.main ?: "---", Icons.Default.Opacity)
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        Text(text = label, fontSize = 9.sp, color = Color.White)
    }
}

@Composable
fun PlantCategorySection(
    title: String, 
    items: List<String>,
    onItemClick: (String) -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Text(
            text = title, 
            fontSize = 15.sp, 
            fontWeight = FontWeight.Bold, 
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items) { item ->
                PlantCircleItem(item) { onItemClick(item) }
            }
        }
    }
}

@Composable
fun PlantCircleItem(name: String, onClick: () -> Unit) {
    val stageTheme = getStageColorTheme(name)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp) // Increased size
                .clip(RoundedCornerShape(16.dp))
                .background(stageTheme.containerColor)
                .border(0.5.dp, stageTheme.borderColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stageTheme.icon, fontSize = 32.sp) // Dynamic stage icon
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun ThreeDLettucePreview(
    stage: String,
    onStageChange: (String) -> Unit = {}
) {
    var showStageMenu by remember { mutableStateOf(false) }
    var selectedVariety by remember { mutableStateOf("Olmetie") }
    val stagesList = remember { listOf("SEEDLING", "VEGETATIVE", "HEADING", "MATURITY") }

    val infiniteTransition = rememberInfiniteTransition(label = "hologramTransition")
    
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingAnimation"
    )

    val beamPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beamPulse"
    )

    val hudRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hudRotation"
    )

    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "AI GROWTH ANALYSIS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(DarkHologramBG)
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val cornerRadius = 32.dp.toPx()
                    drawRoundRect(
                        color = NeonGreen.copy(alpha = borderPulse),
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth),
                        style = Stroke(width = strokeWidth),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius - strokeWidth / 2, cornerRadius - strokeWidth / 2)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Perspective Grid (Floor)
            Canvas(modifier = Modifier.fillMaxSize()) {
                withTransform({
                    translate(top = size.height * 0.7f)
                    val perspectiveScale = 1.5f
                    scale(scaleX = perspectiveScale, scaleY = 0.5f, pivot = center)
                }) {
                    val gridSpacing = 40.dp.toPx()
                    val gridColor = NeonGreen.copy(alpha = 0.1f)
                    
                    for (x in -size.width.toInt()..size.width.toInt() step gridSpacing.toInt()) {
                        drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
                    }
                    for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                        drawLine(gridColor, Offset(-size.width, y.toFloat()), Offset(size.width * 2, y.toFloat()), 1f)
                    }
                }
            }

            // 2. Projector Base Ornaments
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Rotating HUD Rings
                Canvas(modifier = Modifier.size(240.dp).graphicsLayer { rotationZ = hudRotation }) {
                    val centerPt = Offset(size.width / 2, size.height / 2 + 10.dp.toPx())
                    drawCircle(
                        color = NeonGreen.copy(alpha = 0.15f),
                        radius = 80.dp.toPx(),
                        center = centerPt,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                    drawCircle(
                        color = NeonGreen.copy(alpha = 0.1f),
                        radius = 100.dp.toPx(),
                        center = centerPt,
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 20f), 0f))
                    )
                }

                // Vertical Light Cylinder
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val beamPath = Path().apply {
                        moveTo(size.width / 2 - 100.dp.toPx(), size.height)
                        lineTo(size.width / 2 + 100.dp.toPx(), size.height)
                        lineTo(size.width / 2 + 80.dp.toPx(), 0f)
                        lineTo(size.width / 2 - 80.dp.toPx(), 0f)
                        close()
                    }
                    drawPath(
                        path = beamPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonGreen.copy(alpha = 0f), NeonGreen.copy(alpha = 0.15f * beamPulse)),
                            startY = 0f,
                            endY = size.height
                        )
                    )
                }
            }

            // 3. Floating Particles (Bokeh)
            ParticleSystem(modifier = Modifier.fillMaxSize())

            // THE 3D LETTUCE SCENE (Increased size for enhanced visibility)
            Box(
                modifier = Modifier
                    .size(380.dp)
                    .graphicsLayer {
                        translationY = floatingOffset
                    },
                contentAlignment = Alignment.Center
            ) {
                val modelPath = PlantUtils.getModelPathForVarietyAndStage(selectedVariety, stage)
                val stageScale = PlantUtils.getScaleForStage(stage)
                val orbitRadius = PlantUtils.getCameraRadiusForStage(stage)

                LettuceScene(
                    modifier = Modifier.fillMaxSize(),
                    modelPath = modelPath,
                    initialStage = stage,
                    scale = stageScale,
                    orbitRadius = orbitRadius,
                    enableVarietySelector = true,
                    initialVariety = selectedVariety,
                    onVarietySelected = { selectedVariety = it },
                    enableControls = false
                )
                
                // Scan Line Matrix Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 6.dp.toPx()
                    for (i in 0 until (size.height / step).toInt()) {
                        val y = i * step
                        drawLine(
                            color = NeonGreen.copy(alpha = 0.1f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                }
            }

            val stageTheme = getStageColorTheme(stage)

            // Interactive Status Badge with Growth Stage Picker
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showStageMenu = true },
                    color = stageTheme.containerColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, stageTheme.borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${stageTheme.icon} STATUS: ${stage.uppercase()}",
                            color = stageTheme.accentColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Growth Stage",
                            tint = stageTheme.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showStageMenu,
                    onDismissRequest = { showStageMenu = false },
                    modifier = Modifier
                        .background(DarkHologramBG)
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    stagesList.forEach { stageOption ->
                        val isSelected = stageOption.equals(stage, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val optionTheme = getStageColorTheme(stageOption)
                                    Text(text = optionTheme.icon, fontSize = 14.sp)
                                    Text(
                                        text = stageOption,
                                        color = if (isSelected) NeonGreen else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            },
                            onClick = {
                                onStageChange(stageOption)
                                showStageMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParticleSystem(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "time"
    )

    Canvas(modifier = modifier) {
        val count = 15
        for (i in 0 until count) {
            val progress = (time + i.toFloat() / count) % 1f
            val x = (Math.sin(i.toDouble() * 1.5).toFloat() * 0.4f + 0.5f) * size.width
            val y = (1f - progress) * size.height
            val alpha = (1f - Math.abs(0.5f - progress) * 2) * 0.3f
            
            drawCircle(
                color = NeonGreen.copy(alpha = alpha),
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun HarvestPredictionsSummary(plants: List<PlantEntity>) {
    Column(modifier = Modifier.padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Dashboard, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Fleet Status Snapshot", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.15f), GlassBackground)
                    )
                )
                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            if (plants.isEmpty()) {
                Text(
                    text = "NO NEURAL DATA AVAILABLE",
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                val readyCount = plants.count { it.dap >= 33 || it.daysToHarvest <= 0 || it.stage.uppercase() == "MATURITY" }
                val growingCount = plants.size - readyCount

                Row(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(text = "UPCOMING SCHEDULE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonGreen, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        plants.filter { it.daysToHarvest > 0 && it.dap < 33 }
                            .sortedBy { it.daysToHarvest }
                            .take(3)
                            .forEach { plant ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(modifier = Modifier.size(4.dp), shape = CircleShape, color = NeonGreen) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "ID: ${plant.id} (${plant.daysToHarvest}d left)", fontSize = 12.sp, color = Color.White)
                                }
                            }
                    }
                    
                    Box(modifier = Modifier.width(1.dp).height(70.dp).background(Color.White.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                    
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(text = "FLEET STATUS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonGreen, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(text = "GROWING: $growingCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "READY: $readyCount", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = if (plants.isNotEmpty()) readyCount.toFloat() / plants.size else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = NeonGreen,
                            trackColor = Color.White.copy(alpha = 0.1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LettuceVarietyCategorySection(onItemClick: (String) -> Unit) {
    val varieties = listOf("OLMETIE", "LIMAMBO", "ORBITAL")
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Text(
            text = "Lettuce Varieties", 
            fontSize = 15.sp, 
            fontWeight = FontWeight.Bold, 
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(varieties) { variety ->
                PlantCircleItem(variety) { onItemClick(variety) }
            }
        }
    }
}

@Composable
fun DashboardInfoDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = title, 
                color = NeonGreen, 
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            ) 
        },
        text = {
            Column {
                // Placeholder for future PIC
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                    Text("IMAGE PREVIEW", color = Color.White.copy(alpha = 0.1f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DISMISS", color = NeonGreen, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkHologramBG,
        shape = RoundedCornerShape(28.dp)
    )
}
