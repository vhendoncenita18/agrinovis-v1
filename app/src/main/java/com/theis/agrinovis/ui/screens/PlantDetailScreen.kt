package com.theis.agrinovis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.ui.components.AgrinovisScaffold
import com.theis.agrinovis.ui.components.HolographicCard
import com.theis.agrinovis.ui.components.HolographicDetailItem
import com.theis.agrinovis.ui.navigation.Screen
import com.theis.agrinovis.ui.theme.DarkHologramBG
import com.theis.agrinovis.ui.theme.GrowthStageBadge
import com.theis.agrinovis.ui.theme.NeonGreen
import com.theis.agrinovis.ui.theme.getStageColorTheme
import com.theis.agrinovis.ui.utils.PlantUtils
import com.theis.agrinovis.ui.viewmodels.PlantsViewModel
import com.theis.agrinovis.visualization.threeD.LettuceScene

@Composable
fun PlantDetailScreen(
    navController: NavController, 
    plantId: String,
    viewModel: PlantsViewModel = viewModel()
) {
    var plant by remember { mutableStateOf<PlantEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHarvestDialog by remember { mutableStateOf(false) }

    LaunchedEffect(plantId) {
        plant = viewModel.getPlantById(plantId)
    }

    if (showDeleteDialog && plant != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Purge Data?", color = Color.White) },
            text = { Text("Permanently delete this plant's neural record?", color = Color.White.copy(alpha = 0.7f)) },
            containerColor = DarkHologramBG,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlant(plant!!)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) {
                    Text("DELETE", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }

    if (showHarvestDialog && plant != null) {
        AlertDialog(
            onDismissRequest = { showHarvestDialog = false },
            title = { Text("HARVEST LETTUCE", color = Color.White, fontWeight = FontWeight.Black) },
            text = { Text("Are you sure you want to harvest and finalize Plant ID ${plant!!.id} (${plant!!.variety}) at ${plant!!.dap} DAP?", color = Color.White.copy(alpha = 0.8f)) },
            containerColor = DarkHologramBG,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.harvestPlant(plant!!)
                        showHarvestDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("HARVEST NOW", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHarvestDialog = false }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    AgrinovisScaffold(
        navController = navController,
        title = "DATA CORE: ${plant?.id ?: "..."}",
        showBottomBar = false,
        actions = {
            if (plant != null) {
                IconButton(onClick = { navController.navigate(Screen.EditPlant.createRoute(plant!!.id)) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NeonGreen)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(DarkHologramBG)) {
            if (plant == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else {
                val recommendations = PlantUtils.getRecommendationsForStage(plant!!.stage)
                val stageTheme = getStageColorTheme(plant!!.stage)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        // 3D Header - Now with stage consistency
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color.Black.copy(alpha = 0.2f))
                        ) {
                            LettuceScene(
                                modifier = Modifier.fillMaxSize(),
                                modelPath = PlantUtils.getModelPathForVarietyAndStage(plant!!.variety, plant!!.stage),
                                scale = PlantUtils.getScaleForStage(plant!!.stage),
                                orbitRadius = PlantUtils.getCameraRadiusForStage(plant!!.stage),
                                showScanningText = false
                            )
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            
                            HolographicCard(title = "Plant Information") {
                                HolographicDetailItem("VARIETY", plant!!.variety)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CLOUD SYNC STATUS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Surface(
                                        color = if (plant!!.isSynced) NeonGreen.copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (plant!!.isSynced) "☁️ SYNCED ONLINE" else "⚡ LOCAL (PENDING SYNC)",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 9.sp,
                                            color = if (plant!!.isSynced) NeonGreen else Color(0xFFFFB300),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GROWTH STAGE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    GrowthStageBadge(stage = plant!!.stage)
                                }
                                HolographicDetailItem("PLANTED ON", plant!!.plantedDate)
                                HolographicDetailItem("GROWTH AGE", "${plant!!.dap} DAYS")
                                HolographicDetailItem("LEAF COUNT", plant!!.leafCount.toString())
                            }

                            HolographicCard(title = "Stage Recommendations & Guidance") {
                                StageRecommendationRow("PRIMARY FOCUS", recommendations.primaryFocus, stageTheme.accentColor)
                                StageRecommendationRow("IRRIGATION SCHEDULE", recommendations.irrigationAdvice, stageTheme.accentColor)
                                StageRecommendationRow("LIGHTING & CLIMATE", recommendations.lightingAdvice, stageTheme.accentColor)
                                StageRecommendationRow("NUTRIENT FORMULA", recommendations.nutrientAdvice, stageTheme.accentColor)
                                StageRecommendationRow("DAILY MONITORING TIP", recommendations.monitoringTip, stageTheme.accentColor)
                            }

                            HolographicCard(title = "Atmospheric conditions") {
                                HolographicDetailItem("TEMPERATURE", "${plant!!.temp}°C")
                                HolographicDetailItem("HUMIDITY", "${plant!!.humidity}%")
                                HolographicDetailItem("AIR PRESSURE", "${plant!!.pressure} hPa")
                                HolographicDetailItem("WEATHER", plant!!.condition)
                            }

                            HolographicCard(title = "Neural Prediction") {
                                HolographicDetailItem("EST. HARVEST", plant!!.estHarvestDate)
                                HolographicDetailItem("TIME REMAINING", "${plant!!.daysToHarvest} DAYS")
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { 1f - (plant!!.daysToHarvest.toFloat() / 30f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeonGreen,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                )

                                val isReadyToHarvest = plant!!.harvestStatus.equals("Ready to Harvest", ignoreCase = true) ||
                                        plant!!.stage.equals("Maturity", ignoreCase = true) ||
                                        plant!!.daysToHarvest <= 0 ||
                                        plant!!.dap >= 33

                                if (!plant!!.harvestStatus.equals("Harvested", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showHarvestDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isReadyToHarvest) NeonGreen else Color.White.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isReadyToHarvest) Color.Black else NeonGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isReadyToHarvest) "HARVEST PLANT (READY)" else "HARVEST PLANT NOW",
                                            color = if (isReadyToHarvest) Color.Black else NeonGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageRecommendationRow(
    label: String, 
    detail: String, 
    accentColor: Color = NeonGreen
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = detail,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f),
            lineHeight = 16.sp
        )
    }
}
