# Walkthrough - Fix Offline Authentication for Deleted Supabase Accounts

We successfully investigated and resolved the offline authentication bug where a deleted Supabase account could still log in offline due to persistent stale local Room records.

## Changes

### Authentication & Stale Account Purging

#### [MODIFY] [AuthViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/Agrinovis/app/src/main/java/com/theis/agrinovis/ui/viewmodels/AuthViewModel.kt)
- Updated `login()` when the device is **online** (`isConnected == true`):
  - When Supabase rejects login with authentication errors (e.g. `invalid login credentials`, `user not found`, `unauthorized`, `401`, `404`), the app checks if a local `UserAccountEntity` exists for that email.
  - If rejected while online, the app automatically purges the stale local Room account (`userDao.deleteUserByEmail(email)`).
  - This guarantees that once an account is deleted remotely and the user reconnects online (or attempts online login), the stale local credentials are wiped and can never be used for offline authentication thereafter.

---

## Verification Results

### Automated Tests
- Ran `./gradlew app:assembleDebug` — **Build finished successfully.**

### Manual Verification Instructions
1. **Remote Deletion Test:**
   - Create User A in Supabase and log in online (caches credentials in Room).
   - Log out.
   - Delete User A from Supabase (remotely).
   - Turn Internet **ON** and attempt normal login with User A.
   - **Result:** Login fails ("Email or password is incorrect" or similar), and the app automatically purges the stale local Room account.
2. **Offline Login Prevention Test:**
   - Turn Internet **OFF**.
   - Attempt offline login with User A.
   - **Result:** Offline login **MUST FAIL** because the local authentication record has been invalidated and purged upon the previous online rejection.
