# Implementation Plan - Fix Offline Authentication for Deleted Supabase Accounts

This plan addresses the bug where a deleted Supabase account can still successfully log in when the device is offline because stale local `UserAccountEntity` records persist indefinitely in Room without remote validation upon reconnection or online login rejection.

## Diagnosis

- **ROOM AUTHENTICATION SOURCE:** `com.theis.agrinovis.data.local.dao.UserDao#getUserByEmail` (`app/src/main/java/com/theis/agrinovis/data/local/dao/UserDao.kt`)
- **ROOM USER ENTITY:** `com.theis.agrinovis.data.local.entities.UserAccountEntity` (`app/src/main/java/com/theis/agrinovis/data/local/entities/UserAccountEntity.kt`)
- **ROOM DAO:** `com.theis.agrinovis.data.local.dao.UserDao` (`app/src/main/java/com/theis/agrinovis/data/local/dao/UserDao.kt`)
- **OFFLINE LOGIN FUNCTION:** `com.theis.agrinovis.ui.viewmodels.AuthViewModel#login` (offline catch block) (`app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt`)
- **AUTH REPOSITORY:** None (Authentication logic is implemented directly inside `AuthViewModel.kt`).
- **AUTH VIEWMODEL:** `com.theis.agrinovis.ui.viewmodels.AuthViewModel` (`app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt`)
- **WHERE LOCAL USER IS CREATED:** `com.theis.agrinovis.ui.viewmodels.AuthViewModel#signUp` and `AuthViewModel#login` via `userDao.insertUser` (`app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt`)
- **WHERE LOCAL USER IS DELETED:** `com.theis.agrinovis.ui.viewmodels.AuthViewModel#checkExistingSession` (`userDao.deleteUserBySupabaseId` / `deleteUserByEmail`) (`app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt`)
- **WHERE SUPABASE SESSION IS VALIDATED:** `com.theis.agrinovis.ui.viewmodels.AuthViewModel#checkExistingSession` (`app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt`)
- **WHERE OFFLINE → ONLINE IS DETECTED:** `com.theis.agrinovis.data.remote.NetworkMonitor#handleNetworkChange` (`app/src/main/java/com/theis/agrinovis/data/remote/NetworkMonitor.kt`)
- **WHO SETS AUTHENTICATED:** `com.theis.agrinovis.ui.viewmodels.AuthViewModel` (`_authState.value = AuthState.Success`, `AuthState.SessionRestored`, or `AuthState.OfflineMode`) (`app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt`)
- **WHO SETS LOGGED_OUT:** `com.theis.agrinovis.ui.viewmodels.AuthViewModel#logout` and `checkExistingSession` (`_authState.value = AuthState.LoggedOut`) (`app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt`)
- **WHO NAVIGATES TO DASHBOARD:** `com.theis.agrinovis.ui.screens.LoginScreen` (`LaunchedEffect(authState)` reacting to success/restored/offline states) (`app/src/main/java/com/theis/agrinovis/ui/screens/LoginScreen.kt`)
- **WHO NAVIGATES TO LOGIN:** `com.theis.agrinovis.ui.navigation.NavGraph` (startDestination) and state collection in screens / logout handlers.

### Root Cause
When a user successfully logs in online, `UserAccountEntity` is stored in Room (`user_accounts`). When that account is deleted from Supabase remotely, Room retains the local `UserAccountEntity`. When the device is offline, `AuthViewModel#login` catches the network error, checks `!isConnected`, queries Room (`userDao.getUserByEmail`), finds the cached entity, matches the password hash, and succeeds. There is no validation of cached local accounts against Supabase when coming online or when online login fails for a previously cached account.

---

## User Review Required

> [!IMPORTANT]
> We will adhere strictly to your instructions:
> 1. Do NOT add workarounds to login/logout buttons.
> 2. Do NOT create duplicate repositories, DBs, or classes.
> 3. Do NOT delete Room data when offline.
> 4. Validate cached local accounts upon reconnection (`OFFLINE → ONLINE`) or when online login fails for a locally cached user (indicating remote deletion/invalidation).

---

## Proposed Changes

### Authentication & Local User Management

#### [MODIFY] [AuthViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/Agrinovis/app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt)
- In `login()` when online, if Supabase throws an exception (such as `invalid login credentials`, `user not found`, `unauthorized`, `401`, `404`), check if a local `UserAccountEntity` exists in Room for this email. If a local user exists but online Supabase auth rejected it, it indicates the remote account was deleted or modified. Purge the stale local account from Room (`userDao.deleteUserByEmail(email)`) so offline login cannot be exploited using stale credentials.
- Add robust debug logging (`AUTH DEBUG`) around online login, offline login, session validation, and local account purging as requested.

#### [MODIFY] [NetworkMonitor.kt](file:///C:/Users/Admin/AndroidStudioProjects/Agrinovis/app/src/main/java/com/theis/agrinovis/data/remote/NetworkMonitor.kt)
- Upon `OFFLINE → ONLINE` transition, in addition to plant syncing, trigger a validation check (or invoke session validation) to verify cached accounts against Supabase if possible, ensuring any deleted accounts are purged from Room when connection is restored.

---

## Verification Plan

### Automated Tests
- Build project using `./gradlew assembleDebug` to verify no compilation issues.

### Manual Verification
1. Create user in Supabase, log in online (caches in Room).
2. Logout.
3. Delete user in Supabase.
4. Turn internet ON, attempt normal login -> fails ("Email or password is incorrect" or similar), and verify stale Room account is purged.
5. Turn internet OFF, attempt offline login with that email/password -> **Must FAIL**.
