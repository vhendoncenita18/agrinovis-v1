package com.theis.agrinovis.ui.screens

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.theis.agrinovis.data.local.entities.PlantEntity
import com.theis.agrinovis.data.remote.RetrofitClient
import com.theis.agrinovis.data.remote.WeatherConfig
import com.theis.agrinovis.data.session.MonitoringSession
import com.theis.agrinovis.ml.prediction.PredictionInput
import com.theis.agrinovis.ml.prediction.TFLitePredictionEngine
import com.theis.agrinovis.ui.components.AgrinovisScaffold
import com.theis.agrinovis.ui.components.futuristicTextFieldColors
import com.theis.agrinovis.ui.navigation.Screen
import com.theis.agrinovis.ui.theme.DarkHologramBG
import com.theis.agrinovis.ui.theme.GlassBackground
import com.theis.agrinovis.ui.theme.NeonGreen
import com.theis.agrinovis.ui.utils.LocationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val diameterCv = MonitoringSession.diameterCvResult
    val heightCv = MonitoringSession.heightCvResult
    val isCvActive = (diameterCv?.isSuccessful == true) || (heightCv?.isSuccessful == true)

    // Form States
    var height by remember {
        mutableStateOf(
            if (heightCv?.isSuccessful == true) "%.1f".format(Locale.US, heightCv.heightCm) else "12.4"
        )
    }
    var diameter by remember {
        mutableStateOf(
            if (diameterCv?.isSuccessful == true) "%.1f".format(Locale.US, diameterCv.diameterCm) else "20.1"
        )
    }
    var leafCount by remember { mutableStateOf("") }
    
    // Variety Dropdown
    val varieties = listOf("Olmetie", "Limambo", "Orbital")
    var varietyExpanded by remember { mutableStateOf(false) }
    var variety by remember { mutableStateOf(varieties[0]) }
    
    // Growth Stage Dropdown
    val stages = listOf("Seedling", "Vegetative", "Heading", "Maturity")
    var stageExpanded by remember { mutableStateOf(false) }
    var growthStage by remember { mutableStateOf(stages[0]) }

    // Harvest Status Dropdown
    val statusOptions = listOf("Growing", "Ready to Harvest", "Harvested")
    var statusExpanded by remember { mutableStateOf(false) }
    var harvestStatus by remember { mutableStateOf(statusOptions[0]) }
    
    // Date and DAP
    val calendar = Calendar.getInstance()
    var plantedDate by remember { mutableStateOf("") }
    var dap by remember { mutableStateOf("0") }

    // Weather States
    val cachedWeather = com.theis.agrinovis.data.session.MonitoringSession.latestWeather
    var locationName by remember { mutableStateOf(cachedWeather?.name ?: "") }
    var temp by remember { mutableStateOf(cachedWeather?.main?.temp?.toString() ?: "") }
    var humidity by remember { mutableStateOf(cachedWeather?.main?.humidity?.toString() ?: "") }
    var rainfall by remember { mutableStateOf(cachedWeather?.rain?.oneHour?.toString() ?: "0.0") }
    var windSpeed by remember { mutableStateOf(cachedWeather?.wind?.speed?.toString() ?: "") }
    var pressure by remember { mutableStateOf(cachedWeather?.main?.pressure?.toString() ?: "") }
    var condition by remember { mutableStateOf(cachedWeather?.weather?.firstOrNull()?.main ?: "") }
    var isFetchingWeather by remember { mutableStateOf(false) }

    // Date Picker Dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            plantedDate = format.format(selectedDate.time)
            
            // Calculate DAP
            val diff = Date().time - selectedDate.timeInMillis
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            dap = days.coerceAtLeast(0).toString()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Function to fetch weather dynamically
    fun fetchWeatherData() {
        scope.launch {
            isFetchingWeather = true
            try {
                // Timeout for hardware GPS lock
                val location = withTimeoutOrNull(WeatherConfig.GPS_TIMEOUT_MS) {
                    LocationHelper.getLastLocation(context)
                }
                
                val response = if (location != null) {
                    // Fetch by coordinates if GPS locked
                    RetrofitClient.weatherService.getWeatherByCoords(
                        lat = location.latitude,
                        lon = location.longitude,
                        apiKey = WeatherConfig.API_KEY
                    )
                } else {
                    // Fallback to default search if no GPS
                    RetrofitClient.weatherService.getWeather(
                        if (locationName.isNotEmpty()) locationName else "Manila", 
                        WeatherConfig.API_KEY
                    )
                }

                // 4. AUTOMATIC PARAMETER INJECTION
                locationName = response.name
                temp = response.main.temp.toString()
                humidity = response.main.humidity.toString()
                pressure = response.main.pressure.toString()
                windSpeed = response.wind.speed.toString()
                rainfall = response.rain?.oneHour?.toString() ?: "0.0"
                condition = response.weather.firstOrNull()?.main ?: "Clear"
            } catch (e: Exception) {
                // Handling failure with baseline data
                if (locationName.isEmpty()) locationName = "Unknown Location"
                if (temp.isEmpty()) temp = "28.5"
                if (humidity.isEmpty()) humidity = "65"
                if (pressure.isEmpty()) pressure = "1012"
                if (windSpeed.isEmpty()) windSpeed = "3.2"
                if (condition.isEmpty()) condition = "Cloudy"
            } finally {
                isFetchingWeather = false
            }
        }
    }

    // Auto-fetch on entry only if no cached dashboard weather exists yet
    LaunchedEffect(Unit) {
        if (com.theis.agrinovis.data.session.MonitoringSession.latestWeather == null) {
            fetchWeatherData()
        }
    }

    // Validation
    val isDapInvalidForSeedling = growthStage == "Seedling" && (dap.toIntOrNull() ?: 0) > 10
    
    val isFormValid = height.isNotEmpty() && diameter.isNotEmpty() && 
                     variety.isNotEmpty() && plantedDate.isNotEmpty() && 
                     leafCount.isNotEmpty() && temp.isNotEmpty() &&
                     locationName.isNotEmpty() && humidity.isNotEmpty() &&
                     pressure.isNotEmpty() && windSpeed.isNotEmpty() &&
                     condition.isNotEmpty() && !isDapInvalidForSeedling

    AgrinovisScaffold(
        navController = navController,
        title = "VERIFY DATA",
        showBottomBar = false,
        showTopBar = true
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(DarkHologramBG)) {
            // Close Button in Upper Right
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 48.dp, bottom = 100.dp)
            ) {
                item {
                    Text(
                        text = "AI MEASUREMENT REVIEW",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen,
                        letterSpacing = 1.sp
                    )
                }

                // AI STATUS BANNER
                item {
                    if (isCvActive) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("AI EXTRACTION ACTIVE", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    Text("Computer Vision extracted diameter & height automatically.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFB300).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("AI MODEL PENDING OR SKIPPED", color = Color(0xFFFFB300), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    Text("CV model ('models/cv_model.tflite') was not triggered or confidence was low. Baseline values loaded for manual verification.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Measurement Fields
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { 
                                if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                    height = it 
                                }
                            },
                            label = { Text("Plant Height (cm)", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = futuristicTextFieldColors()
                        )
                        OutlinedTextField(
                            value = diameter,
                            onValueChange = { 
                                if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                    diameter = it 
                                }
                            },
                            label = { Text("Plant Diameter (cm)", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = futuristicTextFieldColors()
                        )
                    }
                }

                item { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }

                item {
                    Text(text = "PLANT IDENTITY", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = varietyExpanded,
                        onExpandedChange = { varietyExpanded = !varietyExpanded },
                    ) {
                        OutlinedTextField(
                            value = variety,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Lettuce Variety", color = Color.White) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = varietyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            colors = futuristicTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = varietyExpanded,
                            onDismissRequest = { varietyExpanded = false }
                        ) {
                            varieties.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(text = v, color = Color.White.copy(alpha = 0.7f)) },
                                    onClick = {
                                        variety = v
                                        varietyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = plantedDate,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Planted Date", color = Color.White) },
                            modifier = Modifier.weight(1.5f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = NeonGreen)
                                }
                            },
                            colors = futuristicTextFieldColors()
                        )
                        OutlinedTextField(
                            value = dap,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("DAP", color = Color.White) },
                            modifier = Modifier.weight(0.5f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            colors = futuristicTextFieldColors()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = leafCount,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                leafCount = it 
                            }
                        },
                        label = { Text("Leaf Count", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = futuristicTextFieldColors()
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = stageExpanded,
                        onExpandedChange = { stageExpanded = !stageExpanded },
                    ) {
                        OutlinedTextField(
                            value = growthStage,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Growth Stage", color = Color.White) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            colors = futuristicTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = stageExpanded,
                            onDismissRequest = { stageExpanded = false }
                        ) {
                            stages.forEach { stage ->
                                DropdownMenuItem(
                                    text = { Text(text = stage, color = Color.White.copy(alpha = 0.7f)) },
                                    onClick = {
                                        growthStage = stage
                                        stageExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (isDapInvalidForSeedling) {
                    item {
                        Text(
                            text = "Seedling stage cannot have a DAP greater than 10 days.",
                            color = Color(0xFFFF3D00),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                        )
                    }
                }

                // Harvest Status Dropdown removed as it is now automated by DAP threshold (>= 45 days)

                item { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ENVIRONMENTAL PARAMETERS", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
                        Spacer(modifier = Modifier.weight(1f))
                        if (isFetchingWeather) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonGreen)
                        } else {
                            IconButton(onClick = { fetchWeatherData() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Location Name / City", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                        colors = futuristicTextFieldColors()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = temp,
                            onValueChange = { 
                                if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".") || it == "-") {
                                    temp = it 
                                }
                            },
                            label = { Text("Temp (°C)", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = futuristicTextFieldColors()
                        )
                        OutlinedTextField(
                            value = humidity,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    humidity = it 
                                }
                            },
                            label = { Text("Humidity (%)", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = futuristicTextFieldColors()
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = windSpeed,
                            onValueChange = { 
                                if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                    windSpeed = it 
                                }
                            },
                            label = { Text("Wind (m/s)", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = futuristicTextFieldColors()
                        )
                        OutlinedTextField(
                            value = pressure,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    pressure = it 
                                }
                            },
                            label = { Text("hPa", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = futuristicTextFieldColors()
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = rainfall,
                            onValueChange = { 
                                if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                    rainfall = it 
                                }
                            },
                            label = { Text("Rainfall (mm)", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = futuristicTextFieldColors()
                        )
                        OutlinedTextField(
                            value = condition,
                            onValueChange = { condition = it },
                            label = { Text("Condition", color = Color.White) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold),
                            colors = futuristicTextFieldColors()
                        )
                    }
                }
            }

            // Bottom Confirm Button
            Button(
                onClick = {
                    try {
                        val input = PredictionInput(
                            dap = dap.toIntOrNull() ?: 0,
                            leafCount = leafCount.toIntOrNull() ?: 0,
                            diameterCm = diameter.toFloatOrNull() ?: 0f,
                            heightCm = height.toFloatOrNull() ?: 0f,
                            temperature = temp.toFloatOrNull() ?: 28.5f,
                            humidity = humidity.toFloatOrNull() ?: 65f,
                            windSpeed = windSpeed.toFloatOrNull() ?: 3.2f,
                            pressure = pressure.toFloatOrNull() ?: 1012f,
                            variety = variety,
                            growthStage = growthStage,
                            recordDateIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        )

                        scope.launch {
                            val predictionEngine = TFLitePredictionEngine(context)
                            val predictionResult = predictionEngine.predict(input)

                            val entity = PlantEntity(
                                id = "L-${UUID.randomUUID().toString().take(6).uppercase()}",
                                variety = variety,
                                stage = if (predictionResult.isSuccessful) predictionResult.predictedStage else growthStage,
                                plantedDate = plantedDate,
                                dap = dap.toIntOrNull() ?: 0,
                                leafCount = leafCount.toIntOrNull() ?: 0,
                                height = height.toDoubleOrNull() ?: 0.0,
                                diameter = diameter.toDoubleOrNull() ?: 0.0,
                                temp = temp.toDoubleOrNull() ?: 0.0,
                                humidity = humidity.toIntOrNull() ?: 0,
                                pressure = pressure.toIntOrNull() ?: 0,
                                windSpeed = windSpeed.toDoubleOrNull() ?: 0.0,
                                rainfall = rainfall.toDoubleOrNull() ?: 0.0,
                                condition = condition,
                                harvestStatus = if ((dap.toIntOrNull() ?: 0) >= 33 || predictionResult.daysToHarvest <= 0) "Ready to Harvest" else "Growing",
                                estHarvestDate = predictionResult.estimatedHarvestDate,
                                daysToHarvest = predictionResult.daysToHarvest
                            )
                            MonitoringSession.currentPlant = entity
                            navController.navigate(Screen.PredictionResult.route)
                        }
                    } catch (e: Exception) {
                        Log.e("VerificationScreen", "Error during prediction execution: ${e.message}")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    disabledContainerColor = NeonGreen.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    "PROCESS PREDICTION", 
                    fontWeight = FontWeight.Black, 
                    color = if (isFormValid) Color.Black else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
