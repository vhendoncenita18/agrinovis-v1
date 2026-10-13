package com.theis.agrinovis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.theis.agrinovis.data.remote.ConnectivityHelper
import com.theis.agrinovis.data.remote.NetworkMonitor
import com.theis.agrinovis.ui.navigation.NavGraph
import com.theis.agrinovis.ui.navigation.Screen
import com.theis.agrinovis.ui.theme.AgrinovisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkMonitor.init(applicationContext)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            AgrinovisTheme {
                val navController = rememberNavController()
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(navController = navController)
                }

                androidx.compose.runtime.LaunchedEffect(intent) {
                    val data = intent?.data
                    if (data != null && (data.host == "reset-password" || data.toString().contains("type=recovery") || data.toString().contains("access_token"))) {
                        Log.d("AUTH DEBUG", "Navigating to ResetPassword screen from deep link: $data")
                        try {
                            navController.navigate(Screen.ResetPassword.route)
                        } catch (e: Exception) {
                            Log.e("AUTH DEBUG", "Failed to navigate to ResetPassword: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null) {
            Log.d("AUTH DEBUG", "Received deep link intent: $data")
        }
    }

    override fun onResume() {
        super.onResume()
        val currentConnection = ConnectivityHelper.isConnected(applicationContext)
        Log.d("NETWORK", "App resumed. Current connectivity: $currentConnection")
    }
}
