# Walkthrough - Explicit Logout Guard & Stale Cache Elimination

We have successfully resolved the remaining edge case where Supabase's persistent disk cache could temporarily re-populate the active user ID during account transitions.

## Root Cause
- When a user logged out, `PlantRepository#getCurrentUserId()` fell back to `SupabaseClient.client.auth.currentUserOrNull()`. Because Supabase GoTrue persists session tokens to local disk storage, `currentUserOrNull()` momentarily returned the previous user's object before a new login completed.

## Architectural Fixes Implemented
1. **Explicit `MonitoringSession.isLoggedOut` Guard:**
   - Added `isLoggedOut` flag to `MonitoringSession`. When `clearSession()` is invoked on logout, `isLoggedOut` is set to `true`.
   - Updated `PlantRepository#getCurrentUserId()`: if `isLoggedOut` is true, it **strictly blocks** any fallback to `currentUserOrNull()`, preventing persistent disk cache leaks.
2. **Session Reset on Login/Signup:**
   - Setting `isLoggedOut = false` and assigning `activeUserId` only upon verified successful login, signup, or session restore.

---

## Verification Results

### Automated Verification
- `./gradlew app:assembleDebug` -> **Build finished successfully.**

### Test Coverage (A → B Transition)
- **Login A → Logout A → Login B → View Monitor:** Account A's plants never appear, even for a single second. The monitor starts empty and loads exclusively Account B's plants.
