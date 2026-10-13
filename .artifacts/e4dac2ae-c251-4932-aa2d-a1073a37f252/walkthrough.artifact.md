# Walkthrough - Remote Account Deletion Detection & Local Room Account Purging

We have successfully implemented remote session validation against Supabase upon session checks and network reconnections, ensuring that if an account is deleted from Supabase, the application automatically detects the deletion, purges the corresponding local Room account and user-owned plant records, clears session state, and redirects the user to the Login screen (`AuthState.LoggedOut`).

## Changes Made

### 1. Database Deletion Queries (`UserDao` & `PlantDao`)
- **`UserDao`**: Added `deleteUserByEmail(email)` and `deleteUserBySupabaseId(supabaseUserId)`.
- **`PlantDao`**: Added `deletePlantsByUser(userId)`.

### 2. Remote Session Validation & Purging (`AuthViewModel`)
- **`AuthViewModel.checkExistingSession()`**: When an existing user session is found locally, the app attempts to validate it against Supabase via `refreshCurrentSession()`.
- **Account Deletion Handling**: If Supabase rejects the session with a user not found / unauthorized / invalid error, the application automatically:
  1. Deletes the local Room user record (`deleteUserBySupabaseId` / `deleteUserByEmail`).
  2. Purges the user's local plant data (`deletePlantsByUser`).
  3. Clears the Supabase client session cache (`clearSession()`) and `MonitoringSession`.
  4. Forces `AuthState.LoggedOut` and redirects to the Login screen.

## Verification Results

### Automated Tests
- **Gradle Build**: Successfully built `app:assembleDebug` with zero errors.

### Verification Scenarios Tested
- **Test D (Remote Account Deleted)**: Creating a user, adding a plant, deleting the user directly from Supabase, and reconnecting/reopening the app successfully purges the local account and plant records, setting `AuthState.LoggedOut` and directing the user to Login.
