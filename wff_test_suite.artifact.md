# Twelveish Redux WFF - Comprehensive Test Suite

This document defines the test cases required to verify the functionality of the **Watch Face Format (WFF)** version of Twelveish Redux.

## 1. Prerequisites
- **Device:** Wear OS watch or emulator running API 34+ (Wear OS 5 or 6).
- **Package:** `com.psychowood.twelveish.wff`
- **Deployment:** Ensure the `:watch-face` module is deployed.

---

## 2. Core Functional Tests

### 2.1 Fuzzy Time Accuracy
Verify that the "Fuzzy Time" strings change correctly based on the system clock.

| Minute Range | Expected Prefix | Expected Suffix | Hour Shifting |
| :--- | :--- | :--- | :--- |
| `00 - 04` | (None) | `ish` | Current Hour |
| `05 - 09` | (None) | `or so` | Current Hour |
| `10 - 14` | `a quarter past` | (None) | Current Hour |
| `15 - 19` | `a quarter past` | `or so` | Current Hour |
| `20 - 24` | `almost half past` | (None) | Current Hour |
| `25 - 29` | `around half past` | (None) | Current Hour |
| `30 - 34` | `half past` | `ish` | Current Hour |
| `35 - 37` | `half past` | `or so` | Current Hour |
| `38 - 39` | `half past` | `or so` | **Next Hour** |
| `40 - 44` | `a quarter to` | (None) | Next Hour |
| `45 - 49` | `a quarter to` | `or so` | Next Hour |
| `50 - 54` | `approaching` | (None) | Next Hour |
| `55 - 59` | `almost` | (None) | Next Hour |

**Test Action:** Set system time using ADB and verify rendering.
```bash
adb shell date 090311022026.00  # Set to 11:02 -> Expected "eleven ish"
adb shell date 090311392026.00  # Set to 11:39 -> Expected "half past twelve or so"
```

### 2.2 Digital Clock & Sub-Info
- **Digital Time:** Should show `HH:MM` at the top.
- **Sub-Info:** Should show `DayOfWeek Day • Battery%` (e.g., "Thu 3 • 100%").

### 2.3 Internationalization (Language)
**Action:** Change system language in Watch Settings -> General -> Language.
- [ ] **Switch to Italian:** Digital clock and Date (Sub-Info) should localize (e.g., "Gio 3").
- [ ] **Fuzzy Time Localization:**
    > [!IMPORTANT]
    > Current WFF v1 uses English literals for fuzzy segments. Verify that if localized strings are implemented in `@string/wff_strings.xml`, they reflect the chosen language.

---

## 3. Configuration & Personalization

### 3.1 Background Color
**Action:** Long-press watch face -> Settings -> Background Color.
- [ ] Option 0: Black (`#000000`)
- [ ] Option 1: Dark Gray (`#222222`)
- [ ] Option 2: Deep Blue (`#000033`)
- [ ] Option 3: Forest Green (`#002200`)

### 3.2 Main Text Color
**Action:** Long-press watch face -> Settings -> Text Color.
- [ ] Option 0: White (`#FFFFFF`)
- [ ] Option 1: Classic Orange (`#FF9800`)
- [ ] Option 2: Twelveish Blue (`#2196F3`)
- [ ] Option 3: Soft Gray (`#AAAAAA`)

---

## 4. Text Rendering & Encoding

### 4.1 Special Characters & Quotes
- [ ] **Literal Check:** Ensure no single quotes (`'`) or double quotes (`"`) appear as text unless they are part of the fuzzy logic (e.g., ensure it doesn't render as `'eleven' ish`).
- [ ] **Logical Operators:** Ensure the watch face doesn't crash or fail to render due to `&&` or `<` symbols in the XML expressions.

### 4.2 Text Size & Fit
- [ ] **Center Alignment:** Ensure the fuzzy time is perfectly centered horizontally.
- [ ] **Circular Safety:** Ensure the text does not touch the edges of the circular screen. Longest string test: "around half past eleven or so".
- [ ] **Scaling:** Verify that the digital clock at the top doesn't overlap with the main fuzzy text.

### 4.3 Multi-line & New Lines
- [ ] **Static Wrapping:** Verify that the `FuzzyTime` `PartText` block handles the height correctly.
- [ ] **Line Spacing:** Ensure the "Prefix", "Hour", and "Suffix" segments (if rendered separately) have consistent vertical spacing.

---

## 5. Complications

### 5.1 Slot 1 (Left/Top-Left)
- **Position:** (50, 320)
- **Default:** Watch Battery.
- [ ] Verify data updates (change battery level via emulator controls).

### 5.2 Slot 2 (Right/Top-Right)
- **Position:** (320, 320)
- **Default:** System Date.
- [ ] Verify date matches system calendar.

---

## 6. Performance & System Behavior

### 6.1 Ambient Mode
**Action:** Use emulator "Power" button or let watch timeout.
- [ ] Background should force to true black (`#000000`).
- [ ] Fuzzy time should remain white/gray (anti-burn-in).

### 6.2 Power Consumption Warning
- [ ] Select watch face.
- [ ] Note if "Smart watchface" / "High battery usage" warning appears.

---

## 7. Automated Validation (Developer Only)
Run the following tools to ensure Play Store compliance:
1. **XML Schema:** `gradlew :watch-face:assembleDebug` should pass.
2. **Memory Footprint:** Use `wff-validator` to ensure < 10MB memory usage.
