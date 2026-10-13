package com.theis.agrinovis.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.theis.agrinovis.data.local.database.AgrinovisDatabase
import com.theis.agrinovis.data.repository.PlantRepository
import com.theis.agrinovis.data.session.MonitoringSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object NetworkMonitor {
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var isInitialized = false
    private var lastOnlineState = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            Log.w("NetworkMonitor", "ConnectivityManager is null.")
            return
        }

        // Check initial state
        val initialStatus = checkInitialConnectivity(connectivityManager)
        _isOnline.value = initialStatus
        lastOnlineState = initialStatus
        Log.d("NETWORK", "Initial Network State: ${if (initialStatus) "ONLINE" else "OFFLINE"}")

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    handleNetworkChange(appContext, true)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    handleNetworkChange(appContext, false)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    handleNetworkChange(appContext, hasInternet)
                }
            })
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Failed to register network callback: ${e.message}", e)
        }
    }

    private fun checkInitialConnectivity(cm: ConnectivityManager): Boolean {
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun handleNetworkChange(context: Context, isConnected: Boolean) {
        val previousState = lastOnlineState
        lastOnlineState = isConnected
        _isOnline.value = isConnected

        Log.d("NETWORK", "Network changed: ${if (isConnected) "ONLINE" else "OFFLINE"}")

        // Detect OFFLINE -> ONLINE transition
        if (!previousState && isConnected) {
            Log.d("SYNC", "Detected OFFLINE -> ONLINE transition. Resetting offline mode & triggering sync...")
            
            if (MonitoringSession.isOfflineMode) {
                MonitoringSession.isOfflineMode = false
                Log.d("AUTH", "Automatically transitioned from offline mode to online mode upon network reconnection.")
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AgrinovisDatabase.getDatabase(context)
                    val repo = PlantRepository(db.plantDao())
                    repo.forceSync(context)
                    repo.enqueueSyncWorker(context)
                } catch (e: Exception) {
                    Log.e("SYNC", "Auto-sync error on network reconnection: ${e.message}", e)
                }
            }
        }
    }
}
