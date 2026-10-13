package com.theis.agrinovis.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.ui.components.AgrinovisScaffold
import com.theis.agrinovis.ui.components.futuristicTextFieldColors
import com.theis.agrinovis.ui.theme.NeonGreen
import com.theis.agrinovis.ui.viewmodels.PlantsViewModel
import com.theis.agrinovis.ui.viewmodels.SavePlantState
import java.util.UUID

@Composable
fun AddPlantScreen(
    navController: NavController,
    viewModel: PlantsViewModel = viewModel()
) {
    var plantId by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("Olmetie") }
    var plantedDate by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val saveState by viewModel.savePlantState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetSaveState()
    }

    LaunchedEffect(saveState) {
        if (saveState is SavePlantState.Success) {
            val msg = (saveState as SavePlantState.Success).message
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.resetSaveState()
            navController.popBackStack()
        }
    }

    AgrinovisScaffold(
        navController = navController,
        title = "Register New Plant",
        showBottomBar = false
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ERROR ALERT BANNER
            AnimatedVisibility(
                visible = saveState is SavePlantState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = (saveState as? SavePlantState.Error)?.message ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = plantId,
                onValueChange = { plantId = it },
                label = { Text("Plant ID (e.g., L001)", color = Color.White) },
                placeholder = { Text("Auto-generated if blank", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is SavePlantState.Loading,
                textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                colors = futuristicTextFieldColors()
            )

            OutlinedTextField(
                value = variety,
                onValueChange = { variety = it },
                label = { Text("Variety", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is SavePlantState.Loading,
                textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                colors = futuristicTextFieldColors()
            )

            OutlinedTextField(
                value = plantedDate,
                onValueChange = { plantedDate = it },
                label = { Text("Planted Date (e.g. Sep 20, 2026)", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is SavePlantState.Loading,
                textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                colors = futuristicTextFieldColors()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is SavePlantState.Loading,
                textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                colors = futuristicTextFieldColors()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 40)
                    val formattedEstDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(cal.time)

                    val finalId = if (plantId.isBlank()) "L-${UUID.randomUUID().toString().take(6).uppercase()}" else plantId.trim()
                    val newPlant = PlantEntity(
                        id = finalId,
                        variety = if (variety.isBlank()) "Olmetie" else variety.trim(),
                        stage = "Seedling",
                        plantedDate = if (plantedDate.isBlank()) "Today" else plantedDate.trim(),
                        dap = 1,
                        leafCount = 4,
                        height = 5.0,
                        diameter = 8.0,
                        temp = 28.0,
                        humidity = 65,
                        pressure = 1012,
                        windSpeed = 2.0,
                        rainfall = 0.0,
                        condition = "Good",
                        harvestStatus = "Growing",
                        estHarvestDate = formattedEstDate,
                        daysToHarvest = 40,
                        timestamp = System.currentTimeMillis()
                    )
                    viewModel.savePlant(newPlant)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = saveState !is SavePlantState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    disabledContainerColor = NeonGreen.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (saveState is SavePlantState.Loading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving to Supabase...", fontWeight = FontWeight.Bold, color = Color.Black)
                } else {
                    Text("Save Plant", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
