# Task List - Remote Account Deletion & Local Purging

- [x] **Step 1: Add Deletion Methods to DAOs**
  - [x] Add `deleteUserByEmail` and `deleteUserBySupabaseId` in `UserDao`.
  - [x] Add `deletePlantsByUser` in `PlantDao`.

- [x] **Step 2: Implement Remote Account Validation & Purging in AuthViewModel**
  - [x] Update `checkExistingSession()` to call `refreshCurrentSession()` and handle deleted account detection.
  - [x] Purge Room user account and plants upon deletion confirmation, clear session, and set `AuthState = LOGGED_OUT`.

- [x] **Step 3: NetworkMonitor Reconnection Validation**
  - [x] Reconnection triggers validation and sync.

- [x] **Step 4: Verification & Walkthrough**
  - [x] Build project with Gradle (`app:assembleDebug`).
  - [x] Create `walkthrough.artifact.md`.
