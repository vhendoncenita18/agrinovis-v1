# Implementation Plan - Remote Account Deletion Detection & Local Room Account Purging

This implementation plan ensures that if a user account is deleted directly from Supabase, the application automatically detects the deletion upon online connection / session check, purges the corresponding local Room authentication record and user-owned data, clears the session, sets `AuthState.LOGGED_OUT`, and navigates to the Login screen.

## User Review Required

> [!IMPORTANT]
> 1. **Remote Account Validation**: Upon app start (when online) or during `OFFLINE → ONLINE` network transitions, the app will validate the current session against Supabase (`retrieveUser()`).
> 2. **Local Room & Data Purging**: If Supabase confirms the account no longer exists (unauthorized/user not found), the app will instantly delete the user's record from Room (`user_accounts`), purge the user's plants (`plants`), clear `MonitoringSession`, call `SupabaseClient.client.auth.clearSession()`, and transition to `AuthState.LoggedOut`.
> 3. **Prevention of Stale Re-authentication**: Deleted accounts can never re-authenticate locally or via offline fallback once purged.

## Proposed Changes

### Database & DAO Layer
#### [MODIFY] [UserDao.kt](file:///C:/Users/Admin/AndroidStudioProjects/Agrinovis/app/src/main/java/com/theis/agrinovis/data/local/dao/UserDao.kt)
- Add `deleteUserByEmail(email: String)` and `deleteUserBySupabaseId(supabaseUserId: String)`.

#### [MODIFY] [PlantDao.kt](file:///C:/Users/Admin/AndroidStudioProjects/Agrinovis/app/src/main/java/com/theis/agrinovis/data/local/dao/PlantDao.kt)
- Add `deletePlantsByUser(userId: String)`.

### Authentication & Network Monitor Layer
#### [MODIFY] [AuthViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/Agrinovis/app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt)
- Update `checkExistingSession()` to perform remote account validation against Supabase. If validation fails due to account deletion/not found, purge local Room account and transition to `AuthState.LoggedOut`.

#### [MODIFY] [NetworkMonitor.kt](file:///C:/Users/Admin/AndroidStudioProjects/Agrinovis/app/src/main/java/com/theis/agrinovis/data/remote/NetworkMonitor.kt)
- Upon `OFFLINE → ONLINE` transition, trigger remote account validation and synchronization.

## Verification Plan

### Automated Tests
- Build project successfully using Gradle (`app:assembleDebug`).

### Manual Verification
- **Test D (Remote Account Deleted)**: Create user, log in, add plant, delete user in Supabase, reconnect internet / reopen app -> app detects account deletion, purges local Room account and plants, sets `AuthState.LoggedOut`, and redirects to Login.
