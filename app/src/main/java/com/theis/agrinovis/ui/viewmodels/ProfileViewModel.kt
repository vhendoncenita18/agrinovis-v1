package com.theis.agrinovis.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theis.agrinovis.data.model.User
import com.theis.agrinovis.data.remote.SupabaseClient
import com.theis.agrinovis.data.session.MonitoringSession
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _userState = MutableStateFlow(
        User(
            name = "Farmer",
            farmerId = "F-2026-GUEST",
            email = "offline@agrinovis.io",
            avatarUrl = null
        )
    )
    val userState: StateFlow<User> = _userState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser != null) {
                val email = currentUser.email ?: "offline@agrinovis.io"
                val nameFromEmail = email.substringBefore("@")
                    .split(".", "_", "-")
                    .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                val farmerId = "F-2026-" + currentUser.id.take(8).uppercase()

                _userState.update { current ->
                    current.copy(
                        name = "Farmer $nameFromEmail",
                        farmerId = farmerId,
                        email = email
                    )
                }
            } else if (!MonitoringSession.offlineUserEmail.isNullOrBlank()) {
                val email = MonitoringSession.offlineUserEmail!!
                val nameFromEmail = email.substringBefore("@")
                _userState.update { current ->
                    current.copy(
                        name = "Farmer $nameFromEmail",
                        farmerId = "F-2026-OFFLINE",
                        email = email
                    )
                }
            }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            Log.d("LOGOUT", "ProfileViewModel.signOut() started")
            // 1. Immediately clear local session state
            MonitoringSession.isOfflineMode = false
            MonitoringSession.offlineUserEmail = null
            MonitoringSession.clearSession()
            Log.d("LOGOUT", "Local session cleared in ProfileViewModel")

            // 2. Force clear Supabase client local session cache
            try {
                SupabaseClient.client.auth.clearSession()
                Log.d("LOGOUT", "Supabase clearSession() succeeded")
            } catch (e: Exception) {
                Log.w("LOGOUT", "Supabase clearSession failed (offline expected): ${e.message}")
            }

            // 3. Attempt remote sign out (non-blocking)
            try {
                SupabaseClient.client.auth.signOut()
                Log.d("LOGOUT", "Supabase remote sign out succeeded")
            } catch (e: Exception) {
                Log.w("LOGOUT", "Supabase remote sign out ignored (offline): ${e.message}")
            } finally {
                Log.d("LOGOUT", "Executing onComplete navigation callback")
                onComplete()
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _userState.update { 
            it.copy(preferences = it.preferences.copy(notificationsEnabled = enabled))
        }
    }

    fun updateMeasurementUnit(unit: String) {
        _userState.update {
            it.copy(preferences = it.preferences.copy(measurementUnit = unit))
        }
    }
}
