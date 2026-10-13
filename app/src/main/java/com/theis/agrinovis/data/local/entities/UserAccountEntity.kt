package com.theis.agrinovis.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
    val supabaseUserId: String? = null
)
