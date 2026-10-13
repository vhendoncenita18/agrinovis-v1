package com.theis.agrinovis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.ui.components.AgrinovisScaffold
import com.theis.agrinovis.ui.theme.DarkHologramBG
import com.theis.agrinovis.ui.components.HolographicPlantPreview
import com.theis.agrinovis.ui.navigation.Screen
import com.theis.agrinovis.ui.theme.GlassBackground
import com.theis.agrinovis.ui.theme.GrowthStageBadge
import com.theis.agrinovis.ui.theme.NeonGreen
import com.theis.agrinovis.ui.theme.getStageColorTheme
import com.theis.agrinovis.ui.viewmodels.PlantsViewModel

@Composable
fun PlantsScreen(
    navController: NavController,
    viewModel: PlantsViewModel = viewModel()
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshUserAndPlants()
    }

    val plants by viewModel.allPlants.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    var plantToHarvest by remember { mutableStateOf<PlantEntity?>(null) }

    val activePlants = plants.filter { !it.harvestStatus.equals("Harvested", ignoreCase = true) }

    // Known growth stage order for logical chronological sorting
    val knownStageOrder = listOf(
        "SEEDLING",
        "VEGETATIVE",
        "DEVELOPING",
        "HEADING",
        "MATURITY",
        "READY TO HARVEST",
        "HARVEST READY"
    )

    // Dynamic grouping by growth stage from actual plant data
    val groupedPlants = activePlants
        .groupBy { it.stage.ifBlank { "UNSPECIFIED" }.uppercase() }
        .entries
        .sortedWith(compareBy { entry ->
            val index = knownStageOrder.indexOf(entry.key)
            if (index != -1) index else Int.MAX_VALUE
        })

    AgrinovisScaffold(
        navController = navController,
        title = "Monitoring Fleet",
        actions = {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 12.dp).size(20.dp),
                    color = NeonGreen,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = { viewModel.forceSyncAll() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Sync All",
                        tint = NeonGreen
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (activePlants.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NO MONITORED PLANTS",
                        color = Color.White.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use the center '+' button to begin monitoring.",
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedPlants.forEach { (stageName, plantList) ->
                        // Growth Stage Section Header (spans full width of 2 columns)
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "header_$stageName"
                        ) {
                            GrowthStageHeader(stage = stageName, count = plantList.size)
                        }

                        // Plant Cards for this growth stage section
                        items(
                            items = plantList,
                            key = { it.id }
                        ) { plant ->
                            PlantCard(
                                plant = plant,
                                onClick = {
                                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                                },
                                onHarvestClick = { targetPlant ->
                                    plantToHarvest = targetPlant
                                }
                            )
                        }
                    }
                }
            }

            if (plantToHarvest != null) {
                AlertDialog(
                    onDismissRequest = { plantToHarvest = null },
                    containerColor = DarkHologramBG,
                    title = { Text("HARVEST LETTUCE", color = Color.White, fontWeight = FontWeight.Black) },
                    text = {
                        Text(
                            text = "Are you sure you want to harvest and finalize Plant ID ${plantToHarvest?.id} (${plantToHarvest?.variety}) at ${plantToHarvest?.dap} DAP?",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                plantToHarvest?.let { viewModel.harvestPlant(it) }
                                plantToHarvest = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("HARVEST NOW", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { plantToHarvest = null }) {
                            Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GrowthStageHeader(stage: String, count: Int) {
    val stageTheme = getStageColorTheme(stage)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(stageTheme.accentColor)
                )
                Text(
                    text = "${stageTheme.icon} ${stage.uppercase()}",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Surface(
                color = stageTheme.containerColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, stageTheme.borderColor)
            ) {
                Text(
                    text = "$count ${if (count == 1) "PLANT" else "PLANTS"}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 9.sp,
                    color = stageTheme.accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = stageTheme.borderColor
        )
    }
}

@Composable
fun PlantCard(
    plant: PlantEntity,
    onClick: () -> Unit,
    onHarvestClick: (PlantEntity) -> Unit = {}
) {
    val isReadyToHarvest = plant.harvestStatus.equals("Ready to Harvest", ignoreCase = true) ||
            plant.stage.equals("Maturity", ignoreCase = true) ||
            plant.daysToHarvest <= 0 ||
            plant.dap >= 33

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBackground)
            .border(1.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f)),
                contentAlignment = Alignment.Center
            ) {
                HolographicPlantPreview(
                    modifier = Modifier.fillMaxSize(),
                    stage = plant.stage,
                    variety = plant.variety
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = plant.id, 
                    fontWeight = FontWeight.Black, 
                    color = NeonGreen, 
                    fontSize = 14.sp
                )
                Text(
                    text = plant.variety.uppercase(), 
                    fontSize = 11.sp, 
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GrowthStageBadge(stage = plant.stage)

                    // SYNC STATUS BADGE
                    Surface(
                        color = if (plant.isSynced) NeonGreen.copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (plant.isSynced) "☁️ SYNCED" else "🔄 PENDING",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 7.sp,
                            color = if (plant.isSynced) NeonGreen else Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Expected Harvest",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = plant.estHarvestDate.ifBlank { "N/A" },
                        fontSize = 11.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // LINEAR GROWTH PROGRESS INDICATOR (Growth Percentage)
                val totalDays = kotlin.math.max(33, plant.dap + plant.daysToHarvest)
                val progressFraction = (plant.dap.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
                val growthPercent = (progressFraction * 100).toInt()

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GROWTH PROGRESS",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$growthPercent%",
                            fontSize = 9.sp,
                            color = NeonGreen,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonGreen,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${plant.dap} DAP • ${if (plant.daysToHarvest <= 0) "Harvest Ready" else "${plant.daysToHarvest}d left"}",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )

                    if (isReadyToHarvest) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onHarvestClick(plant) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Harvest Plant",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "HARVEST PLANT",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
