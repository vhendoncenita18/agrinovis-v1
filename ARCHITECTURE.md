# ARCHITECTURE.md

# Lettuce Harvest Prediction System

## 1. System Overview

The system is an Android-based intelligent lettuce monitoring and harvest prediction application.

It combines three major technologies:

1. **Computer Vision**
2. **Machine Learning Prediction**
3. **Android Application + 3D Visualization**

The system is designed to minimize manual measurement by using Computer Vision to automatically obtain lettuce measurements.

---

# 2. High-Level Architecture

```text
                         LETTUCE
                            │
                            ▼
                  ┌──────────────────┐
                  │ Android Camera   │
                  └────────┬─────────┘
                           │
                           ▼
                 ┌────────────────────┐
                 │ Computer Vision    │
                 │      Model         │
                 └─────────┬──────────┘
                           │
                    ┌──────┴───────┐
                    │              │
                    ▼              ▼
               Diameter          Height
                    │              │
                    └──────┬───────┘
                           │
                           ▼
                 Other User/Measured
                    Parameters
                           │
                           ▼
                ┌────────────────────┐
                │ Prediction Model   │
                └─────────┬──────────┘
                          │
                          ▼
                 Harvest Prediction
                          │
                          ▼
                 Growing Stage Logic
                          │
              ┌───────────┴───────────┐
              │                       │
              ▼                       ▼
      Prediction Information     3D Lettuce
                                  Visualization
```

---

# 3. Major Components

## Component A — Android Application

Responsible for:

* user interface
* camera capture
* input collection
* CV result display
* prediction requests
* prediction result display
* history
* 3D visualization
* practical information

---

# 4. Component B — Computer Vision

Purpose:

Automatically obtain:

* lettuce diameter
* lettuce height

from captured images.

The actual CV architecture is:

**TBD**

The model must be trained using the project's actual dataset.

---

# 5. Component C — Prediction Model

Purpose:

Predict the thesis-defined harvest-related target using:

* Computer Vision measurements
* manually entered parameters
* other validated features

The exact prediction algorithm is:

**TBD**

The exact prediction target is:

**TBD**

---

# 6. Component D — 3D Visualization

The application includes a 3D representation of a lettuce plant.

The displayed model should correspond to the predicted/current growing stage.

Conceptual mapping:

```text
Stage 1
   ↓
3D Model / Visual State 1

Stage 2
   ↓
3D Model / Visual State 2

Stage 3
   ↓
3D Model / Visual State 3

Stage 4
   ↓
3D Model / Visual State 4
```

The exact 3D implementation technology is:

**TBD**

Possible technologies may include an Android-compatible 3D rendering solution or a game/3D engine integration if justified.

Do not select a technology until compatibility with Android Studio and the project requirements has been verified.

---

# 7. Android Technology

Primary development environment:

**Android Studio**

Programming language:

**Kotlin**

UI framework:

**Jetpack Compose**

The final Android SDK/Gradle versions must be based on the actual project configuration.

---

# 8. Android Application Architecture

Recommended:

**MVVM + layered architecture**

```text
UI
 │
 ▼
ViewModel
 │
 ▼
Use Case
 │
 ▼
Repository
 │
 ├──────────────┐
 ▼              ▼
Local         Remote
Data          Services
                │
                ├── CV Model
                └── Prediction Model
```

---

# 9. UI Layer

Responsibilities:

* display screens
* collect user actions
* display measurements
* display prediction results
* display 3D plant
* display errors
* display loading states

Potential screens:

```text
Splash
Login
Dashboard
Lettuce Monitoring
Camera Measurement
Measurement Result
Prediction Input
Prediction Result
3D Growth View
Prediction History
Profile
```

Final screen list:

**TBD according to thesis requirements.**

---

# 10. Computer Vision Flow

```text
User
 ↓
Open Camera
 ↓
Capture Image
 ↓
Image Validation
 ↓
Preprocessing
 ↓
CV Model
 ↓
Detection / Segmentation
 ↓
Measurement Calculation
 ↓
Diameter + Height
 ↓
Display Result
 ↓
User Confirmation
```

---

# 11. Measurement System

The CV system should produce:

```text
diameter
height
confidence
unit
```

However, actual units and measurement methodology are:

**TBD**

The system must use a scientifically valid method for converting image information into physical measurements.

If a reference object, camera calibration, known scale, or controlled image setup is required, it must be documented in the research methodology.

---

# 12. Prediction Flow

```text
CV Diameter
      +
CV Height
      +
Other Required Parameters
      │
      ▼
Prediction Input
      │
      ▼
Preprocessing
      │
      ▼
Prediction Model
      │
      ▼
Prediction Output
      │
      ▼
Growing Stage
      │
      ├───────────────┐
      ▼               ▼
Prediction       3D Plant
Information      Visualization
```

---

# 13. Prediction Model Interface

The Android application should communicate with the prediction component through a defined interface.

Conceptual interface:

```text
PredictionInput
        ↓
PredictionService
        ↓
PredictionResult
```

Example:

```text
PredictionInput
├── diameter
├── height
├── parameter_1
├── parameter_2
└── parameter_n
```

Output:

```text
PredictionResult
├── prediction
├── unit
├── stage
├── confidence (if supported)
├── modelVersion
└── timestamp
```

The actual features are:

**TBD**

---

# 14. Model Deployment

Deployment approach:

**TBD**

Possible approaches:

### Option A — On-device

```text
Android
 ↓
ML Model
 ↓
Prediction
```

### Option B — Server/API

```text
Android
 ↓
API
 ↓
ML Model Server
 ↓
API Response
 ↓
Android
```

### Option C — Other validated deployment

The final choice must depend on the trained model, performance requirements, privacy requirements, and thesis scope.

The AI agent must not choose randomly.

---

# 15. Dataset Architecture

There are two major datasets.

## Computer Vision Dataset

```text
Images
   +
Annotations
   +
Measurement Ground Truth
```

Purpose:

Train the Computer Vision model.

---

## Prediction Dataset

```text
Growth Parameters
   +
CV/Measured Features
   +
Other Research Variables
   +
Target
```

Purpose:

Train the harvest prediction model.

---

# 16. Model Development Pipeline

## Computer Vision

```text
Image Collection
      ↓
Annotation
      ↓
Dataset Cleaning
      ↓
Train/Validation/Test
      ↓
Training
      ↓
Evaluation
      ↓
Measurement Validation
      ↓
Model Export
      ↓
Android Integration
```

## Prediction

```text
Data Collection
      ↓
Cleaning
      ↓
Feature Engineering
      ↓
Train/Validation/Test
      ↓
Model Training
      ↓
Evaluation
      ↓
Model Selection
      ↓
Model Export
      ↓
Android Integration
```

---

# 17. Data Integrity

The same measurement methodology must be used consistently between:

* dataset collection
* model training
* model validation
* Android inference

Otherwise, model performance may not represent actual application performance.

---

# 18. Growing Stage System

The growing-stage system connects prediction results to the user interface.

Conceptually:

```text
Prediction
    ↓
Stage Classification / Mapping
    ↓
Growing Stage
    ↓
3D Representation
    ↓
Practical Information
```

The exact stage definitions are:

**TBD**

The AI agent must not invent biological thresholds.

---

# 19. Practical Information Layer

The system should provide useful information based on the lettuce's state.

Possible information categories:

```text
Current Measurements
Growth Stage
Predicted Harvest
Monitoring Status
Historical Growth
Next Monitoring Action
Harvest Readiness
```

The exact recommendations must be supported by the research or explicitly defined rules.

---

# 20. Data Storage

Storage technology:

**TBD**

Potential options:

* Room
* Firebase
* REST API + database
* another approved storage solution

The final choice must consider:

* application requirements
* authentication
* history
* synchronization
* offline requirements
* thesis scope

---

# 21. Security

The system must not expose:

* passwords
* API keys
* database credentials
* private tokens

Secrets must not be committed to Git.

---

# 22. Error Handling

CV:

```text
Image Capture
 ↓
CV Failure
 ↓
Explain Failure
 ↓
Recapture
```

Prediction:

```text
Prediction Request
 ↓
Failure
 ↓
Display Error
 ↓
Retry
```

No fake results are allowed.

---

# 23. 3D Visualization Architecture

The 3D subsystem should receive a high-level state:

```text
GrowthStage
```

rather than directly modifying the ML model.

Example:

```text
Prediction Result
       ↓
GrowthStage = Stage 3
       ↓
3D Visualization Controller
       ↓
Stage 3 Lettuce Model
```

This keeps the ML and visualization components independent.

---

# 24. Suggested Android Project Structure

```text
app/
└── src/
    └── main/
        └── java/
            └── com.example.lettuceprediction/
                │
                ├── MainActivity.kt
                │
                ├── data/
                │   ├── model/
                │   ├── repository/
                │   ├── local/
                │   └── remote/
                │
                ├── domain/
                │   ├── model/
                │   └── usecase/
                │
                ├── ml/
                │   ├── cv/
                │   └── prediction/
                │
                ├── ui/
                │   ├── navigation/
                │   ├── screens/
                │   ├── components/
                │   └── theme/
                │
                └── visualization/
                    └── threeD/
```

---

# 25. Separation of AI Components

The project must keep these separate:

```text
Computer Vision
       │
       │ produces measurements
       ▼
Prediction System
       │
       │ produces prediction
       ▼
Growth Stage
       │
       ▼
3D Visualization
```

The CV model should not directly decide the harvest prediction.

The prediction model should not directly control the UI.

The UI should consume structured results from these components.

---

# 26. Architecture Principle

The complete system should follow:

```text
REAL WORLD LETTUCE
       ↓
IMAGE
       ↓
COMPUTER VISION
       ↓
MEASUREMENTS
       ↓
OTHER DATA
       ↓
PREDICTION MODEL
       ↓
PREDICTION
       ↓
GROWING STAGE
       ↓
3D VISUALIZATION
       ↓
USER DECISION SUPPORT
```

This is the central architecture of the thesis system.

---

# 27. Current Unknowns

The following must be finalized before their implementation:

* CV model architecture
* CV dataset structure
* CV annotation method
* physical measurement methodology
* diameter unit
* height unit
* prediction model
* prediction target
* prediction features
* dataset size
* data collection protocol
* growing-stage definitions
* model deployment method
* database
* authentication
* 3D technology
* exact practical recommendations

These must come from the thesis research and engineering decisions.

---

# 28. Architecture Change Rule

The AI coding agent must not change this architecture silently.

Any major change must include:

1. Reason for change
2. Advantages
3. Disadvantages
4. Files affected
5. Impact on thesis
6. Migration steps

The developer must understand the change before implementation.

---

# 29. Automatic Daily Growth Progression Architecture

The system incorporates a time-aware progression engine that connects real elapsed time to plant growth stages and 3D visualization.

```text
       ROOM DB (PlantEntity)
                 │
                 ▼
       Creation Timestamp (ms)
                 │
                 ▼
     Real-time Calendar Delta
   (System.currentTimeMillis - Timestamp)
                 │
                 ▼
          Elapsed Days
                 │
        ┌────────┴────────┐
        ▼                 ▼
   Updated DAP    Remaining Days
        │                 │
        └────────┬────────┘
                 │
                 ▼
       Stage Inference Engine
(Seedling → Vegetative → Heading → Maturity)
                 │
        ┌────────┴────────┐
        ▼                 ▼
  UI Badges &       3D Holographic
  Countdown        Plant Renderer
  (Monitor Fleet)   (Filament GLTF)
```

## Data Flow & Architecture
1. **Creation Anchor**: When a prediction is generated, the record is saved with its initial `dap`, `daysToHarvest`, `stage`, and `timestamp` (system milliseconds).
2. **Repository Transformation**: When `PlantRepository.getPlantsForUser()` queries Room DB, emitted entities pass through `PlantUtils.getUpdatedPlantForCurrentDate()`.
3. **Stage Re-Inference**: If $\text{Elapsed Days} > 0$, the engine automatically recalculates `dap`, `daysToHarvest`, and infers whether the plant has progressed to a new growth stage (`Seedling` $\to$ `Vegetative` $\to$ `Heading` $\to$ `Maturity`).
4. **UI & 3D Synchronization**:
   - The UI automatically re-groups plants under their updated stage headers on the Monitor screen.
   - The 3D viewer resolves the corresponding GLTF model asset (`models/{variety}_{stage}.glb`) and scale.
   - System Notifications trigger when plants transition to "Ready to Harvest".
5. **Disk Persistence**: Updated values are persisted back to Room DB so the app stays synchronized across restarts.

