package com.theis.agrinovis.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(context: Context): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return try {
            // Instantly return last known location cache for immediate speed
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return lastLocation
            }
            // Fallback to fast location provider if cache is empty
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        } catch (e: Exception) {
            null
        }
    }
}
