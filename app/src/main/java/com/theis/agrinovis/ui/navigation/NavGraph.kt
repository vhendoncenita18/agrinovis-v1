package com.theis.agrinovis.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.theis.agrinovis.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Signup.route) {
            SignupScreen(navController)
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController)
        }
        composable(
            route = Screen.VerifyOtp.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerifyOtpScreen(navController, email)
        }
        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("otp") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val otp = backStackEntry.arguments?.getString("otp") ?: ""
            ResetPasswordScreen(navController, email, otp)
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }
        composable(Screen.Plants.route) {
            PlantsScreen(navController)
        }
        composable(Screen.AddPlant.route) {
            AddPlantScreen(navController)
        }
        composable(Screen.CaptureDiameter.route) {
            CaptureScreen(navController, mode = "Diameter")
        }
        composable(Screen.CaptureHeight.route) {
            CaptureScreen(navController, mode = "Height")
        }
        composable(Screen.Verification.route) {
            VerificationScreen(navController)
        }
        composable(Screen.PredictionInput.route) {
            PredictionInputScreen(navController)
        }
        composable(Screen.PredictionResult.route) {
            PredictionResultScreen(navController)
        }
        composable(
            route = Screen.PlantDetail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
            PlantDetailScreen(navController, plantId)
        }
        composable(
            route = Screen.EditPlant.route,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
            EditPlantScreen(navController, plantId)
        }
        composable(Screen.HarvestedLog.route) {
            HarvestedLogScreen(navController)
        }
        composable(Screen.ActivityHistory.route) {
            ActivityHistoryScreen(navController)
        }
        composable(Screen.Information.route) {
            InformationScreen(navController)
        }
        composable(Screen.Insights.route) {
            InsightsScreen(navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        composable(Screen.Notifications.route) {
            val homeViewModel: com.theis.agrinovis.ui.viewmodels.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            NotificationsScreen(navController, homeViewModel)
        }
    }
}
