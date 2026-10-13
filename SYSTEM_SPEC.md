# SYSTEM_SPEC.md

# LETTUCE HARVEST PREDICTION SYSTEM

## 1. System Overview

The Lettuce Harvest Prediction System is an Android-based thesis application that combines:

* Computer Vision
* Machine Learning
* Android mobile development
* Lettuce growth monitoring
* Harvest prediction
* 3D plant visualization
* Practical decision-support information

The primary purpose of the system is to estimate the **actual harvest date of lettuce**.

The Machine Learning model does this indirectly by predicting:

**Days to Harvest (Y)**

The application then converts the predicted number of days into an:

**Estimated Harvest Date**

---

# 2. Central System Workflow

The complete system follows this workflow:

```text
                         LETTUCE
                            │
                            ▼
                     CAMERA CAPTURE
                            │
                            ▼
                  COMPUTER VISION MODEL
                            │
                  ┌─────────┴─────────┐
                  ▼                   ▼
            Plant Diameter       Plant Height
                  │                   │
                  └─────────┬─────────┘
                            │
                            ▼
                 OTHER REQUIRED INPUTS
                            │
                            ▼
                HARVEST PREDICTION MODEL
                            │
                            ▼
                 Predicted Days to Harvest
                            │
                            ▼
                  Estimated Harvest Date
                            │
                            ▼
                     Growing Stage
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
        3D Lettuce Model         Practical Information
                │                       │
                └───────────┬───────────┘
                            ▼
                           USER
```

---

# 3. Prediction Target

## Primary Target

The prediction model predicts:

**Days to Harvest (Y)**

This is a numerical prediction problem.

The target represents the number of days remaining until the lettuce reaches its actual harvest point.

---

# 4. Actual Harvest Date

The dataset contains:

**Actual Harvest Date**

This value represents the observed date when the lettuce was actually harvested.

It is used to derive the target:

```text
Days to Harvest =
Actual Harvest Date - Record Date
```

Example:

```text
Record Date:
September 10, 2026

Actual Harvest Date:
September 18, 2026

Days to Harvest:
8 days
```

The example values above are illustrative only.

---

# 5. Data Leakage Prevention

`Actual Harvest Date` MUST NOT be used as an input feature during model training or inference.

It is the ground-truth information used to calculate the target.

The training relationship is:

```text
Input Features
      ↓
Prediction Model
      ↓
Days to Harvest (Y)
```

The actual harvest date is used only for:

* calculating the target
* evaluating the model
* validating predictions
* research analysis

It must not be supplied to the model as a predictive feature.

---

# 6. Dataset Structure

The current dataset specification is:

| Field                       | Description                   | Role                     |
| --------------------------- | ----------------------------- | ------------------------ |
| Record ID                   | Unique record identifier      | Identifier               |
| Date                        | Date of measurement/record    | Input                    |
| Plant ID                    | Unique plant identifier       | Identifier / grouping    |
| Planted Date                | Date lettuce was planted      | Input                    |
| DAP                         | Days after planting           | Input                    |
| Variety                     | Lettuce variety               | Input                    |
| Growth Stage                | Observed growth stage         | Input                    |
| Leaf Count                  | Number of leaves              | Input                    |
| Plant Height (cm)           | Height of lettuce             | CV-derived input         |
| Plant Diameter (cm)         | Diameter of lettuce           | CV-derived input         |
| Location                    | Growing location              | Input                    |
| Remarks                     | Additional observations       | Potential metadata       |
| Target: Days to Harvest (Y) | Number of days until harvest  | **Target**               |
| Harvest Status              | Harvest status                | Potential input / status |
| Actual Harvest Date         | Actual harvest date           | Ground truth             |
| Temp (°C)                   | Temperature                   | Input                    |
| Humidity (%)                | Relative humidity             | Input                    |
| Wind (m/s)                  | Wind speed                    | Input                    |
| Pressure (hPa)              | Atmospheric pressure          | Input                    |
| Condition                   | Weather/environment condition | Input                    |

The final feature list must be validated during the ML experimentation phase.

---

# 7. Computer Vision Role

Computer Vision is responsible for automatically obtaining:

```text
Plant Height (cm)
Plant Diameter (cm)
```

The user should not normally need to manually type these measurements.

---

# 8. Computer Vision Workflow

```text
User
 ↓
Open Camera
 ↓
Capture Lettuce
 ↓
Image Processing
 ↓
Computer Vision Model
 ↓
Detect / Segment Lettuce
 ↓
Calculate Diameter
 ↓
Calculate Height
 ↓
Display Measurements
 ↓
User Verification
 ↓
Continue
```

---

# 9. Computer Vision Output

Conceptual output:

```text
ComputerVisionResult

diameterCm
heightCm
confidence
```

The exact model output depends on the selected CV architecture.

---

# 10. Physical Measurement Requirement

Computer Vision must produce physical measurements in centimeters.

The system must define a reliable image-to-real-world measurement methodology.

Potential requirements may include:

* reference object
* known scale
* controlled camera distance
* camera calibration
* perspective correction
* segmentation
* measurement conversion

The final methodology must be determined through experimentation.

The AI agent must not invent the measurement methodology.

---

# 11. User Input Workflow

The intended input process is:

## Step 1 — Capture Diameter / Lettuce Image

The user captures the lettuce using the Android camera.

The CV model processes the image.

The system obtains:

```text
Plant Diameter (cm)
```

---

## Step 2 — Capture / Determine Height

The user performs the required capture procedure for height.

The CV model obtains:

```text
Plant Height (cm)
```

The exact image setup for height measurement must follow the Computer Vision methodology.

---

## Step 3 — Additional Parameters

The application collects the remaining prediction features.

These may include:

```text
Date
Planted Date
DAP
Variety
Growth Stage
Leaf Count
Location
Temperature
Humidity
Wind
Pressure
Condition
```

The final feature list will be determined after model experimentation and feature analysis.

---

# 12. Prediction Input

The conceptual prediction input is:

```text
PredictionInput

Date
Planted Date
DAP
Variety
Growth Stage
Leaf Count
Plant Height
Plant Diameter
Location
Temperature
Humidity
Wind
Pressure
Condition
```

However, the actual deployed feature set MUST exactly match the final trained model.

---

# 13. Prediction Process

```text
CV Diameter
      +
CV Height
      +
Other Parameters
      ↓
Feature Preprocessing
      ↓
Prediction Model
      ↓
Predicted Days to Harvest
```

Example:

```text
Predicted Days to Harvest = 8
```

The value is an example only.

---

# 14. Estimated Harvest Date

The application calculates the estimated harvest date using:

```text
Estimated Harvest Date =
Record Date + Predicted Days to Harvest
```

Example:

```text
Record Date:
September 10

Predicted Days to Harvest:
8 days

Estimated Harvest Date:
September 18
```

The application must perform this calculation using date-aware operations rather than manually manipulating date strings.

---

# 15. Prediction Result

The primary prediction result screen should display:

```text
HARVEST PREDICTION

Predicted Days to Harvest:
[Model Output] days

Estimated Harvest Date:
[Calculated Date]

Current Growth Stage:
[Stage]

Plant Diameter:
[CV Output] cm

Plant Height:
[CV Output] cm
```

If supported by the model, additional information may include:

```text
Model Version
Prediction Confidence / Uncertainty
Prediction Timestamp
```

Only values actually produced by the system may be displayed.

---

# 16. Growing Stage

The system should represent the lettuce's growth stage.

The stage may be based on:

* observed growth information
* model-derived information
* approved research rules
* or a dedicated stage classification component

The exact stage methodology must be defined by the research.

The system must not invent biological thresholds.

---

# 17. 3D Lettuce Visualization

The application should contain a 3D representation of a lettuce plant.

The visual representation should correspond to the relevant growth stage.

Concept:

```text
Growth Stage
     ↓
Stage Mapping
     ↓
3D Lettuce Representation
```

For example:

```text
Early Growth
    ↓
Small 3D lettuce

Developing Growth
    ↓
Medium 3D lettuce

Mature Growth
    ↓
Large 3D lettuce

Harvest Ready
    ↓
Mature/harvest-ready 3D lettuce
```

These labels are conceptual until the thesis defines the final stages.

---

# 18. 3D Visualization Principle

The 3D lettuce is a **visual representation**.

It should communicate:

* growth stage
* predicted condition
* progression toward harvest

It should not claim to be an exact 3D reconstruction of the photographed plant unless the system technically supports that capability.

---

# 19. Practical Information

The prediction result should be accompanied by practical information.

Potential information:

```text
Current Growth Stage
Predicted Days to Harvest
Estimated Harvest Date
Plant Diameter
Plant Height
Growth Information
Monitoring Status
Harvest Readiness
Next Monitoring Action
```

The final recommendation rules must be supported by:

* thesis literature
* research methodology
* validated agricultural references
* explicitly defined system rules

The AI agent must not invent agricultural recommendations.

---

# 20. Monitoring History

The application should preserve historical measurements.

Example:

```text
Plant ID: L001

Record 1
Date
Diameter
Height
Leaf Count
Environmental Data

Record 2
Date
Diameter
Height
Leaf Count
Environmental Data

Record 3
Date
Diameter
Height
Leaf Count
Environmental Data
```

This allows users to observe lettuce growth over time.

---

# 21. Prediction History

Each prediction should contain:

```text
Prediction ID
Plant ID
Prediction Date
Input Summary
Predicted Days to Harvest
Estimated Harvest Date
Growth Stage
CV Model Version
Prediction Model Version
```

Exact storage fields may be refined during implementation.

---

# 22. Harvest Status

The system manages the **Harvest Status** of each lettuce asset automatically.

## Automated Status Transition
The status is determined primarily by the lettuce age (Days After Planting - DAP):

- **Growing**: The lettuce is currently in its growth cycle (DAP < 45).
- **Ready to Harvest**: The lettuce has reached the harvest threshold (DAP >= 45).
- **Harvested**: The user has manually confirmed the harvest in the application.

## Harvest Workflow
1. When a plant reaches **45 DAP**, the system flags it as "Ready to Harvest".
2. The plant appears in the "Ready to Harvest" section of the **System Insights** dashboard.
3. The user can click the **HARVEST** button to finalize the cycle.
4. Upon harvest:
   - The application sends a confirmation notification.
   - The status is updated to "Harvested" in the database.

---

# 23. 3D Model Asset Matrix Specifications

The 3D visualization engine supports dedicated `.glb` 3D model assets for each supported lettuce variety across each growth stage:

## Variety-Stage 3D Model Matrix (12 Total `.glb` Assets)
Each of the 3 official varieties (`Olmetie`, `Limambo`, `Orbital`) has 4 dedicated 3D models (one per growth stage):

### Olmetie
- `models/olmetie_seedling.glb`
- `models/olmetie_vegetative.glb`
- `models/olmetie_heading.glb`
- `models/olmetie_maturity.glb`

### Limambo
- `models/limambo_seedling.glb`
- `models/limambo_vegetative.glb`
- `models/limambo_heading.glb`
- `models/limambo_maturity.glb`

### Orbital
- `models/orbital_seedling.glb`
- `models/orbital_vegetative.glb`
- `models/orbital_heading.glb`
- `models/orbital_maturity.glb`

## Asset Resolution Engine
`PlantUtils.getModelPathForVarietyAndStage(variety, stage)` resolves variety-stage paths with fallback to stage models (`models/seedling.glb`, `models/vegetative.glb`, etc.).

   - The record is preserved for historical growth and yield analysis.

Final statuses must be defined according to the actual research workflow. For testing purposes, the 45-day threshold is used as the standard maturity marker.

---

# 23. Training Dataset vs Application Data

The historical research dataset is used to train and evaluate the model.

The Android application will generate new observations.

Conceptually:

```text
Historical Dataset
       ↓
Model Training
       ↓
Validated Model
       ↓
Android Application
       ↓
New Lettuce Observation
       ↓
Prediction
```

---

# 24. Dataset Preparation

Before training:

1. Verify records.
2. Check missing values.
3. Check inconsistent measurements.
4. Check dates.
5. Calculate Days to Harvest.
6. Check outliers.
7. Encode categorical variables.
8. Perform required preprocessing.
9. Split data appropriately.
10. Train the model.
11. Evaluate the model.

---

# 25. Target Generation

For records containing actual harvest dates:

```text
Y = Actual Harvest Date - Record Date
```

The resulting number of days becomes:

**Target: Days to Harvest (Y)**

Records without a valid actual harvest date may require separate handling and must not automatically be assigned an invented target.

---

# 26. Model Evaluation

Because the target is numerical, regression evaluation metrics are likely appropriate.

Potential metrics:

```text
MAE
RMSE
R²
```

Final metrics must be determined based on the selected modeling methodology.

Current values:

```text
MAE: TBD
RMSE: TBD
R²: TBD
```

The AI agent must never fabricate these values.

---

# 27. Model Training

The prediction model should be trained outside the Android application environment.

Recommended conceptual workflow:

```text
Dataset
 ↓
EDA
 ↓
Cleaning
 ↓
Feature Engineering
 ↓
Preprocessing
 ↓
Train / Validation / Test
 ↓
Model Training
 ↓
Hyperparameter Tuning
 ↓
Evaluation
 ↓
Final Model
 ↓
Export
 ↓
Android Integration
```

---

# 28. Model Input Integrity

The preprocessing used during model training must be identical to the preprocessing used during Android inference.

For example, if the training pipeline uses:

```text
StandardScaler
```

the same fitted preprocessing parameters must be used during inference.

The Android application must not independently recreate preprocessing with different values.

---

# 29. Computer Vision + Prediction Integration

The final integration should follow:

```text
                 CAMERA
                   │
                   ▼
             CV MODEL
                   │
           ┌───────┴───────┐
           ▼               ▼
       DIAMETER          HEIGHT
           │               │
           └───────┬───────┘
                   │
                   ▼
            PREDICTION INPUT
                   │
                   ▼
            PREDICTION MODEL
                   │
                   ▼
       DAYS TO HARVEST (Y)
                   │
                   ▼
        ESTIMATED HARVEST DATE
                   │
                   ▼
             GROWTH STAGE
                   │
          ┌────────┴────────┐
          ▼                 ▼
       3D PLANT       PRACTICAL INFO
```

---

# 30. Important Data Distinction

The application must clearly distinguish:

### Computer Vision Output

```text
Plant Diameter
Plant Height
```

These are automatically obtained from images.

### User / Environmental Data

```text
Variety
Leaf Count
Location
Temperature
Humidity
Wind
Pressure
Condition
Other approved parameters
```

### Machine Learning Output

```text
Predicted Days to Harvest
```

### Derived Application Output

```text
Estimated Harvest Date
```

### Historical Ground Truth

```text
Actual Harvest Date
```

This separation is essential for preventing data leakage and maintaining research integrity.

---

# 31. Main Application Screens

Recommended initial screens:

```text
Splash
   ↓
Login
   ↓
Dashboard
   ↓
Lettuce Monitoring
   ├── Plant List
   ├── Add Plant
   ├── Capture Measurement
   ├── CV Measurement Result
   ├── Prediction Input
   ├── Prediction Result
   ├── 3D Growth View
   └── History
```

Final screens may be adjusted according to the actual thesis requirements.

---

# 32. Dashboard

The dashboard may display:

```text
Plants Being Monitored
Recent Predictions
Upcoming Estimated Harvests
Current Growth Stages
Recent Measurements
```

Exact dashboard metrics should be finalized during UI design.

---

# 33. Prediction Result UX

The prediction result should be easy to understand.

Example:

```text
🌱 LETTUCE PREDICTION

Growth Stage
[Mature]

Days to Harvest
[8 days]

Estimated Harvest Date
[September 18, 2026]

Diameter
[CV result] cm

Height
[CV result] cm

        [VIEW 3D GROWTH]

        [SAVE RECORD]
```

The values above are UI examples only.

---

# 34. Error Handling

## CV Failure

```text
Lettuce not detected.

Please retake the image.
```

## Measurement Failure

```text
Unable to calculate the measurement.

Please follow the capture instructions and try again.
```

## Prediction Failure

```text
Unable to generate a prediction.

Please check the required information and try again.
```

The application must never substitute fake values.

---

# 35. Dataset-to-App Relationship

The complete research pipeline is:

```text
REAL LETTUCE
     ↓
DATA COLLECTION
     ↓
┌──────────────────────┐
│ CV DATASET           │
│ Images + Ground Truth│
└──────────┬───────────┘
           ↓
     CV MODEL TRAINING
           ↓
     CV MODEL VALIDATION
           ↓
      CV MODEL EXPORT
           │
           │
┌──────────▼───────────┐
│ PREDICTION DATASET   │
│ Growth + Environment │
│ + Harvest Records    │
└──────────┬───────────┘
           ↓
   PREDICTION TRAINING
           ↓
   MODEL EVALUATION
           ↓
    FINAL MODEL
           │
           ▼
      ANDROID APP
```

---

# 36. Final System Goal

The completed application should allow a user to perform a practical lettuce monitoring workflow:

```text
1. Select lettuce plant.

2. Capture lettuce image.

3. Computer Vision automatically measures:
   - Diameter
   - Height

4. User provides other required parameters.

5. Application sends the complete feature set to the trained prediction model.

6. Prediction model returns:
   - Days to Harvest

7. Application calculates:
   - Estimated Harvest Date

8. Application determines/displays:
   - Growing Stage

9. Application displays:
   - 3D lettuce representation

10. Application displays practical information.

11. Application saves the monitoring and prediction record.
```

---

# 37. Core Thesis Pipeline

The system can be summarized as:

```text
IMAGE
  ↓
COMPUTER VISION
  ↓
DIAMETER + HEIGHT
  ↓
GROWTH / ENVIRONMENTAL PARAMETERS
  ↓
HARVEST PREDICTION MODEL
  ↓
DAYS TO HARVEST
  ↓
ESTIMATED HARVEST DATE
  ↓
GROWTH STAGE
  ↓
3D LETTUCE VISUALIZATION
  ↓
PRACTICAL DECISION SUPPORT
```

This pipeline represents the central functional architecture of the thesis.

---

# 38. Automatic Daily Growth & Stage Evolution Specification

The application implements a dynamic time-aware evolution engine that automatically progresses monitored lettuce plants day-by-day based on real elapsed time.

## 38.1 Elapsed Time Math
For any recorded plant asset, the elapsed calendar days since initial measurement/prediction are calculated as:

$$\text{Elapsed Days} = \lfloor \frac{\text{System.currentTimeMillis()} - \text{Creation Timestamp}}{86,400,000 \text{ ms}} \rfloor$$

## 38.2 Dynamic Attribute Recalculation
- **Updated DAP**: $\text{DAP}_{\text{current}} = \text{DAP}_{\text{initial}} + \text{Elapsed Days}$
- **Updated Remaining Days**: $\text{DaysToHarvest}_{\text{current}} = \max(0, \text{DaysToHarvest}_{\text{initial}} - \text{Elapsed Days})$

## 38.3 Stage Progression Thresholds
Biological growth stages transition automatically as elapsed days accumulate:
- **Seedling**: $\text{DAP} < 10$
- **Vegetative**: $10 \le \text{DAP} \le 21$
- **Heading**: $22 \le \text{DAP} \le 35$
- **Maturity / Harvest Ready**: $\text{DAP} \ge 36$ or $\text{DaysToHarvest} \le 0$

## 38.4 UI & 3D Model Synchronization
- **3D Filament Renderer**: Dynamically swaps the GLTF asset (`models/{variety}_{stage}.glb`) and adjusts camera zoom/scale according to the updated stage.
- **Fleet Monitor & Dashboard**: Automatically updates stage section headers, stage badges, and countdown indicators (`X days left` -> `HARVEST READY`).
- **Disk Persistence**: Updated values are persisted back to Room DB so the app remains synchronized across sessions and offline restarts.

