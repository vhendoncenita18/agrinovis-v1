# Implementation Plan - Fix Stale Plant State During User Transition (Account A → Account B)

This plan addresses the critical bug where Account A's plants are temporarily visible when Account B logs in due to `stateIn` caching the last emitted value across user identity changes.

## Diagnostic Answers (Required)

1. **Exact source of Account A's stale Monitor data:** `PlantsViewModel`'s `stateIn` operator (with `SharingStarted.WhileSubscribed` and `initialValue = emptyList()`) retained the last emitted value from Account A's `getPlantsByUser` flow when `_currentUserId` switched to Account B, until the new Room query completed.
2. **Exact class/function where Account A's plants remain cached:** `PlantsViewModel` (`val allPlants` property using `stateIn`).
3. **Exact Room query currently used by Monitor:** `@Query("SELECT * FROM plants WHERE userId = :userId ORDER BY timestamp DESC") fun getPlantsByUser(userId: String): Flow<List<PlantEntity>>`.
4. **Whether PlantEntity has `userId`:** Yes (`val userId: String?`).
5. **Whether the Room query filters by `userId`:** Yes (`WHERE userId = :userId`).
6. **Whether MonitorViewModel survives the A → B user transition:** Yes, ViewModel instances retained across navigation or re-bound instances of `PlantsViewModel` held previous emission values via `stateIn`.
7. **Whether Repository caches plant data globally:** No, repository returns `Flow<List<PlantEntity>>` directly from Room DAO.
8. **Whether logout clears the current user ID:** Yes, but `PlantsViewModel` state caching caused stale emissions.
9. **Whether Account B creates a new Monitor data subscription:** Yes, but `stateIn` emitted the stale last value during the transition.
10. **Exact files that need modification:** `PlantsViewModel.kt`.

## Proposed Changes

### ViewModel Components

#### [MODIFY] [PlantsViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/Agrinovis/app/src/main/java/com/theis/agrinovis/ui/viewmodels/PlantsViewModel.kt)
- Replace `stateIn` with an explicit `MutableStateFlow<List<PlantEntity>>(emptyList())`.
- Use `collectLatest` on `_currentUserId`:
  - When `userId` changes (or on login/logout), immediately clear `_allPlants.value = emptyList()` to purge any stale state from Account A.
  - Cancel any active collection job and start collecting `repository.getPlantsForUser(userId)`.

## Verification Plan

### Automated Tests
- Build project using `./gradlew assembleDebug` to ensure compilation succeeds.

### Manual Verification
- **Test A → B Verification:**
  1. Log in as Account A, view Monitor (sees Account A's plants).
  2. Log out Account A, log in as Account B, immediately view Monitor.
  3. **Expected:** Account A's plants are **never** displayed; Account B's plants appear immediately (or empty if B has no plants), with zero stale data retention.
