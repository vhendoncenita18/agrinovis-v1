package com.theis.agrinovis.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.theis.agrinovis.data.local.database.AgrinovisDatabase
import com.theis.agrinovis.data.local.entities.UserAccountEntity
import com.theis.agrinovis.data.remote.ConnectivityHelper
import com.theis.agrinovis.data.remote.SupabaseClient
import com.theis.agrinovis.data.session.MonitoringSession
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object SessionRestored : AuthState()
    object OfflineMode : AuthState()
    object LoggedOut : AuthState()
    object OtpRequested : AuthState()
    object OtpVerified : AuthState()
    object PasswordResetEmailSent : AuthState()
    object PasswordUpdatedSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AgrinovisDatabase.getDatabase(application).userDao()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown

    private var cooldownJob: Job? = null
    var isPasswordRecoveryInProgress = false

    init {
        checkExistingSession()
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _resendCooldown.value = 60
            while (_resendCooldown.value > 0) {
                delay(1000L)
                _resendCooldown.value -= 1
            }
        }
    }

    fun checkExistingSession() {
        viewModelScope.launch {
            if (isPasswordRecoveryInProgress) {
                Log.d("AUTH DEBUG", "Session check skipped because password recovery is in progress")
                return@launch
            }
            if (_authState.value is AuthState.LoggedOut) {
                Log.d("AUTH DEBUG", "Session check skipped because state is LOGGED_OUT")
                return@launch
            }
            try {
                val isConnected = ConnectivityHelper.isConnected(getApplication())
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    if (!isConnected) {
                        Log.d("AUTH DEBUG", "Device is OFFLINE. Restoring session locally for user: ${currentUser.id}")
                        MonitoringSession.activeUserId = currentUser.id
                        MonitoringSession.offlineUserEmail = currentUser.email ?: MonitoringSession.offlineUserEmail
                        MonitoringSession.isOfflineMode = true
                        MonitoringSession.isLoggedOut = false
                        _authState.value = AuthState.SessionRestored
                        return@launch
                    }

                    try {
                        SupabaseClient.client.auth.refreshCurrentSession()
                        Log.d("AUTH DEBUG", "Remote account validated successfully for user: ${currentUser.id}")
                        MonitoringSession.activeUserId = currentUser.id
                        MonitoringSession.offlineUserEmail = currentUser.email ?: MonitoringSession.offlineUserEmail
                        MonitoringSession.isOfflineMode = false
                        MonitoringSession.isLoggedOut = false
                        _authState.value = AuthState.SessionRestored
                    } catch (remoteEx: Exception) {
                        val msg = remoteEx.message?.lowercase() ?: ""
                        val isNetworkError = remoteEx is UnknownHostException || remoteEx is SocketTimeoutException || remoteEx is IOException || remoteEx.javaClass.name.contains("UnresolvedAddressException") || msg.contains("host") || msg.contains("timeout") || !ConnectivityHelper.isConnected(getApplication())
                        val isAuthRejection = (msg.contains("invalid_grant") || msg.contains("user_not_found") || msg.contains("401") || msg.contains("404")) && !isNetworkError

                        if (isAuthRejection) {
                            Log.w("AUTH DEBUG", "Remote account deleted or unauthorized! Purging local Room account and session...")
                            val db = AgrinovisDatabase.getDatabase(getApplication())
                            db.userDao().deleteUserBySupabaseId(currentUser.id)
                            currentUser.email?.let { db.userDao().deleteUserByEmail(it) }
                            db.plantDao().deletePlantsByUser(currentUser.id)
                            SupabaseClient.client.auth.clearSession()
                            MonitoringSession.clearSession()
                            _authState.value = AuthState.LoggedOut
                        } else {
                            Log.w("AUTH DEBUG", "Remote account validation network error (offline/temporary): ${remoteEx.message}")
                            MonitoringSession.activeUserId = currentUser.id
                            MonitoringSession.offlineUserEmail = currentUser.email ?: MonitoringSession.offlineUserEmail
                            MonitoringSession.isLoggedOut = false
                            _authState.value = AuthState.SessionRestored
                        }
                    }
                } else {
                    Log.d("AUTH DEBUG", "No existing Supabase session found")
                    if (!isConnected && !MonitoringSession.offlineUserEmail.isNullOrBlank()) {
                        Log.d("AUTH DEBUG", "Offline mode active for: ${MonitoringSession.offlineUserEmail}")
                        MonitoringSession.activeUserId = MonitoringSession.offlineUserEmail
                        MonitoringSession.isOfflineMode = true
                        MonitoringSession.isLoggedOut = false
                        _authState.value = AuthState.SessionRestored
                    }
                }
            } catch (e: Exception) {
                Log.w("AUTH DEBUG", "Session check failed: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d("AUTH DEBUG", "Logout clicked / executed in AuthViewModel")
            MonitoringSession.clearSession()
            try {
                SupabaseClient.client.auth.clearSession()
                Log.d("AUTH DEBUG", "Supabase client session cache cleared successfully")
            } catch (e: Exception) {
                Log.w("AUTH DEBUG", "Supabase clearSession failed (expected when offline): ${e.message}")
            }
            _authState.value = AuthState.LoggedOut
        }
    }

    fun signUp(fullName: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                try { SupabaseClient.client.auth.clearSession() } catch (_: Exception) {}
                MonitoringSession.clearSession()

                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    password = pass
                    data = buildJsonObject {
                        put("full_name", fullName)
                    }
                }
                val remoteUid = SupabaseClient.client.auth.currentUserOrNull()?.id
                userDao.insertUser(
                    UserAccountEntity(
                        email = email,
                        passwordHash = pass,
                        fullName = fullName,
                        supabaseUserId = remoteUid,
                        pendingSync = false
                    )
                )
                MonitoringSession.activeUserId = remoteUid ?: email
                MonitoringSession.isLoggedOut = false
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(formatAuthError(e.message))
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                try { SupabaseClient.client.auth.clearSession() } catch (_: Exception) {}
                MonitoringSession.clearSession()

                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    password = pass
                }
                val remoteUid = SupabaseClient.client.auth.currentUserOrNull()?.id
                try {
                    userDao.insertUser(
                        UserAccountEntity(
                            email = email,
                            passwordHash = pass,
                            fullName = email.substringBefore("@"),
                            supabaseUserId = remoteUid,
                            pendingSync = false
                        )
                    )
                } catch (_: Exception) {}

                MonitoringSession.activeUserId = remoteUid ?: email
                MonitoringSession.isLoggedOut = false
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                val isConnected = ConnectivityHelper.isConnected(getApplication())
                if (!isConnected) {
                    try {
                        val localUser = userDao.getUserByEmail(email)
                        if (localUser == null) {
                            _authState.value = AuthState.Error("Offline login is unavailable. Please connect to the internet and sign in first.")
                        } else if (localUser.passwordHash == pass) {
                            MonitoringSession.isOfflineMode = true
                            MonitoringSession.offlineUserEmail = email
                            MonitoringSession.activeUserId = localUser.supabaseUserId ?: email
                            MonitoringSession.isLoggedOut = false
                            _authState.value = AuthState.Success
                        } else {
                            _authState.value = AuthState.Error("Email or password is incorrect.")
                        }
                    } catch (dbEx: Exception) {
                        _authState.value = AuthState.Error(formatAuthError(e.message))
                    }
                } else {
                    val msg = e.message?.lowercase() ?: ""
                    if (msg.contains("invalid") || msg.contains("not found") || msg.contains("unauthorized") || msg.contains("401") || msg.contains("404") || msg.contains("invalid login credentials")) {
                        Log.w("AUTH DEBUG", "Online login rejected by Supabase. Purging stale local Room account for email: $email")
                        try {
                            userDao.deleteUserByEmail(email)
                        } catch (dbEx: Exception) {
                            Log.e("AUTH DEBUG", "Failed to delete stale local Room user: ${dbEx.message}", dbEx)
                        }
                    }
                    _authState.value = AuthState.Error(formatAuthError(e.message))
                }
            }
        }
    }

    fun requestOtp(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Log.d("AUTH DEBUG", "Requesting password reset OTP for: $email")
                SupabaseClient.client.auth.resetPasswordForEmail(
                    email = email
                )
                Log.d("AUTH DEBUG", "Password reset OTP requested successfully for: $email")
                _authState.value = AuthState.OtpRequested
                startResendCooldown()
            } catch (e: Exception) {
                Log.e("AUTH DEBUG", "requestOtp error: ${e.message}", e)
                val msg = e.message?.lowercase() ?: ""
                val isRateLimit = msg.contains("over_email_send_rate_limit") || msg.contains("rate limit")
                val isNetworkError = e is UnknownHostException || 
                                     e is SocketTimeoutException || 
                                     e is IOException || 
                                     e.javaClass.name.contains("UnresolvedAddressException") ||
                                     msg.contains("unable to resolve host") || 
                                     msg.contains("timeout") || 
                                     msg.contains("sockettimeout")

                when {
                    isRateLimit -> {
                        _authState.value = AuthState.Error("Too many requests. Please wait a few minutes and try again.")
                    }
                    isNetworkError -> {
                        _authState.value = AuthState.Error("Network error: Unable to connect to server. Please check your internet connection.")
                    }
                    else -> {
                        _authState.value = AuthState.Error(formatAuthError(e.message))
                    }
                }
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                SupabaseClient.client.auth.verifyEmailOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email,
                    token = otp
                )
                isPasswordRecoveryInProgress = true
                _authState.value = AuthState.OtpVerified
            } catch (e: Exception) {
                Log.w("AUTH DEBUG", "verifyOtp error: ${e.message}", e)
                val msg = e.message?.lowercase() ?: ""
                val isNetworkError = e is UnknownHostException || 
                                     e is SocketTimeoutException || 
                                     e is IOException || 
                                     e.javaClass.name.contains("UnresolvedAddressException") ||
                                     msg.contains("unable to resolve host") || 
                                     msg.contains("timeout") || 
                                     msg.contains("sockettimeout")

                if (isNetworkError) {
                    _authState.value = AuthState.Error("Connection failed, please try again.")
                } else {
                    _authState.value = AuthState.Error("Invalid or expired verification code. Please check and try again.")
                }
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser == null) {
                    _authState.value = AuthState.Error("Your reset session expired. Please request a new code.")
                    return@launch
                }

                // Update password via Supabase Auth
                SupabaseClient.client.auth.updateUser {
                    password = newPassword
                }

                // Update local Room password hash if user exists
                try {
                    currentUser.email?.let { email ->
                        val localUser = userDao.getUserByEmail(email)
                        if (localUser != null) {
                            userDao.insertUser(localUser.copy(passwordHash = newPassword))
                        }
                    }
                } catch (e: Exception) {
                    Log.w("AUTH DEBUG", "Failed to update local user password hash: ${e.message}", e)
                }

                isPasswordRecoveryInProgress = false
                try {
                    SupabaseClient.client.auth.clearSession()
                } catch (e: Exception) {
                    Log.w("AUTH DEBUG", "Failed to clear session after password update: ${e.message}", e)
                }
                MonitoringSession.clearSession()

                _authState.value = AuthState.PasswordUpdatedSuccess
            } catch (e: Exception) {
                Log.e("AUTH DEBUG", "updatePassword error: ${e.message}", e)
                _authState.value = AuthState.Error(formatAuthError(e.message))
            }
        }
    }

    private fun formatAuthError(msg: String?): String {
        if (msg == null) return "Authentication failed. Please try again."
        val lowerMsg = msg.lowercase()
        return when {
            lowerMsg.contains("unable to resolve host") || lowerLogoMatch(lowerMsg) ->
                "Network error: Unable to connect to server. Please check your internet connection."
            lowerMsg.contains("over_email_send_rate_limit") || lowerMsg.contains("rate limit") -> 
                "Email rate limit exceeded. Please wait a few minutes before trying again."
            lowerMsg.contains("invalid login credentials") -> 
                "Email or password is incorrect."
            lowerMsg.contains("already registered") || lowerMsg.contains("user already exists") -> 
                "An account with this email already exists."
            lowerMsg.contains("email not confirmed") -> 
                "Please check your email to confirm your account."
            else -> {
                if (msg.contains("URL:") || msg.contains("Headers:")) {
                    msg.substringBefore("\nURL:").substringBefore("URL:").trim()
                } else {
                    msg
                }
            }
        }
    }

    private fun lowerLogoMatch(msg: String): Boolean {
        return msg.contains("connect") || msg.contains("network") || msg.contains("timeout") || msg.contains("socket")
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
