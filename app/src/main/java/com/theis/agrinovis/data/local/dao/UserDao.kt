package com.theis.agrinovis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.theis.agrinovis.data.local.entities.UserAccountEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE pendingSync = 1")
    suspend fun getUnsyncedUsers(): List<UserAccountEntity>

    @Query("UPDATE user_accounts SET pendingSync = 0, supabaseUserId = :supabaseUserId WHERE email = :email")
    suspend fun markUserSynced(email: String, supabaseUserId: String)

    @Query("DELETE FROM user_accounts WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)

    @Query("DELETE FROM user_accounts WHERE supabaseUserId = :supabaseUserId")
    suspend fun deleteUserBySupabaseId(supabaseUserId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccountEntity)
}
