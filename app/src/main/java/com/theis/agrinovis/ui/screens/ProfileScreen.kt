package com.theis.agrinovis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.theis.agrinovis.ui.components.AgrinovisScaffold
import com.theis.agrinovis.ui.navigation.Screen
import com.theis.agrinovis.ui.theme.DarkHologramBG
import com.theis.agrinovis.ui.theme.NeonGreen
import com.theis.agrinovis.ui.viewmodels.ProfileViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.userState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showResearchDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DarkHologramBG,
            title = { Text("TERMINATE SESSION?", color = Color.White, fontWeight = FontWeight.Black) },
            text = { Text("Are you sure you want to log out of the AgriNovis system?", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }) {
                    Text("LOGOUT", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }

    if (showResearchDialog) {
        AlertDialog(
            onDismissRequest = { showResearchDialog = false },
            containerColor = DarkHologramBG,
            title = { Text("SYSTEM RESEARCH & INFO", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Lettuce Research Overview:",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• About Lettuce Monitoring: Computer Vision and Machine Learning models process diameter and height to estimate remaining growth days until harvest.\n• Thesis Objectives: Scientific data collection and growth monitoring methodology.\n• Growing Stages: Identifies Seedling, Vegetative, Heading, and Maturity stages mapped to 3D plant models.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showResearchDialog = false }) {
                    Text("CLOSE", color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = DarkHologramBG,
            title = { Text("ABOUT AGRINOVIS", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AGRINOVIS v1.0.0",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "A Smart Lettuce Harvest Prediction & Computer Vision System developed for precision agricultural monitoring. Combines image-based dimension extraction and prediction models for growth tracking.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("CLOSE", color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = DarkHologramBG,
            title = { Text("HELP & SUPPORT", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "System Guide & Support:",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• Camera Capture: Center the lettuce in the frame under good lighting.\n• Data Syncing: Monitored plants automatically sync with Supabase when online.\n• Predictions: Growth stages and harvest timelines update dynamically with fresh measurements.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("CLOSE", color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = DarkHologramBG,
            title = { Text("PRIVACY PROTOCOL", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Data Policy & Protection:",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• Local-first storage ensures plant records remain accessible offline.\n• Cloud sync utilizes encrypted network calls to Supabase backend.\n• Plant images are stored strictly for dataset preparation and growth analysis.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("CLOSE", color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    AgrinovisScaffold(
        navController = navController,
        title = "USER PROFILE",
        showBottomBar = true
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkHologramBG),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // HEADER SECTION
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.1f))
                                .border(2.dp, NeonGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = NeonGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = user.name.uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = user.farmerId,
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // SECTIONS
            item { ProfileSectionTitle("PERSONAL ARCHIVES") }
            item { ProfileItem("NAME", user.name, Icons.Default.Badge) }
            item { ProfileItem("FARMER ID", user.farmerId, Icons.Default.Numbers) }
            item { ProfileItem("EMAIL", user.email, Icons.Default.Email) }
            item { 
                ProfileClickItem("HISTORY LOG", Icons.Default.History) {
                    navController.navigate(Screen.ActivityHistory.route)
                }
            }
            item { 
                ProfileClickItem("HARVESTED LOG", Icons.Default.Inventory2) {
                    navController.navigate(Screen.HarvestedLog.route)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { ProfileSectionTitle("SYSTEM PREFERENCES") }
            item {
                ProfileToggleItem(
                    label = "NOTIFICATIONS",
                    enabled = user.preferences.notificationsEnabled,
                    onToggle = { viewModel.toggleNotifications(it) },
                    icon = Icons.Default.Notifications
                )
            }
            item { ProfileItem("MEASUREMENT UNIT", user.preferences.measurementUnit, Icons.Default.Straighten) }
            item { ProfileItem("VISUAL THEME", user.preferences.themeMode, Icons.Default.Palette) }

            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { ProfileSectionTitle("DOCUMENTATION") }
            item {
                ProfileClickItem("SYSTEM RESEARCH & INFO", Icons.Default.Description) {
                    showResearchDialog = true
                }
            }
            item { ProfileClickItem("ABOUT AGRINOVIS", Icons.Default.Info) { showAboutDialog = true } }
            item { ProfileClickItem("HELP & SUPPORT", Icons.Default.Help) { showHelpDialog = true } }
            item { ProfileClickItem("PRIVACY PROTOCOL", Icons.Default.Shield) { showPrivacyDialog = true } }

            item { Spacer(modifier = Modifier.height(48.dp)) }
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("TERMINATE SESSION", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        color = NeonGreen.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp
    )
}

@Composable
fun ProfileItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProfileToggleItem(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, modifier = Modifier.weight(1f), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonGreen,
                checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun ProfileClickItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, modifier = Modifier.weight(1f), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
    }
}
