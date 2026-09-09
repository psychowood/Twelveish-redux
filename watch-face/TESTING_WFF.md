# Twelveish Redux WFF - Comprehensive Test Suite

This suite defines the required functional tests for the **Watch Face Format (WFF)** version of Twelveish Redux. Use this as a checklist for manual or automated verification.

## 1. Environment Setup
- **Module:** `:watch-face`
- **Package:** `com.psychowood.twelveish.wff`
- **Target:** Wear OS device/emulator (API 34+ recommended for full WFF v2 support).

---

## 2. Core Fuzzy Logic (Language: English)
Verify the fuzzy time rendering logic against the following minute-to-string mapping.
**Rule:** Hour shifts to `Next Hour` when `minute >= 40`.

| Minute Range | Prefix | Hour | Suffix | Output Example (11:XX) |
| :--- | :--- | :--- | :--- | :--- |
| `00` | - | Current | - | Eleven |
| `01 - 04` | - | Current | `ish` | Eleven ish |
| `05 - 09` | - | Current | `or so` | Eleven or so |
| `10 - 14` | `a quarter past` | Current | - | A quarter past eleven |
| `15 - 19` | `a quarter past` | Current | `or so` | A quarter past eleven or so |
| `20 - 24` | `almost half past`| Current | - | Almost half past eleven |
| `25 - 29` | `around half past`| Current | - | Around half past eleven |
| `30 - 34` | `half past` | Current | `ish` | Half past eleven ish |
| `35 - 39` | `half past` | Current | `or so` | Half past eleven or so |
| `40 - 44` | `a quarter to` | Next | - | A quarter to twelve |
| `45 - 49` | `a quarter to` | Next | `or so` | A quarter to twelve or so |
| `50 - 54` | `approaching` | Next | - | Approaching twelve |
| `55 - 59` | `almost` | Next | - | Almost twelve |

---

## 3. Automated Comparison Testing
To ensure consistency between the Legacy (Java) and WFF (XML) versions, a dynamic comparison test is available. This test parses the real XML files and compares them with the original Java logic for all 1440 daily combinations across all locales.

### Run Comparison Test
Run the following command in the terminal:
```bash
./gradlew :app:testDebugUnitTest --tests "com.layoutxml.twelveish.FuzzyTimeComparisonTest"
```

The test generates a detailed report at:
`fuzzy_time_matrix.artifact.md` (or the project root)

---

## 4. System Information & Localization

### 3.1 Digital Clock & Sub-Info
- [ ] **Digital Time:** Verify `HH:MM` format at the top.
- [ ] **24h Mode:** Toggle "24h digital clock" in settings. Verify `14:00` vs `02:00`.
- [ ] **Sub-Info Line:** Verify `[DayOfWeek] [Day] • [Battery]%`.
- [ ] **Battery Toggle:** Disable "Show Battery Info" in settings. Verify the entire line or just the battery part disappears as per current XML logic.

### 3.2 System Language Integration
Switch system locale (Watch Settings -> General -> Language).
- [ ] **Italian (`it-IT`):** Verify fuzzy time (e.g., "quasi le due e un quarto").
- [ ] **German (`de-DE`):** Verify fuzzy time (e.g., "Viertel nach drei").
- [ ] **Lithuanian (`lt-LT`):** Verify fuzzy time (e.g., "beveik pirma su ketvirčiu").

---

## 4. Customization (User Configurations)

### 4.1 Visual Themes
- [ ] **Background Color:** Verify Black, Dark Gray, Deep Blue, Forest Green.
- [ ] **Main Text Color:** Verify White, Classic Orange, Twelveish Blue, Soft Gray.

### 4.2 Configuration UI
- [ ] **Labels:** Verify that settings in the watch editor have readable names (not technical IDs like `bg_color`).

---

## 5. Text Rendering Safety

### 5.1 Character Encoding & Spacing
- [ ] **Quotes:** Ensure no stray single (`'`) or double (`"`) quotes appear around words.
- [ ] **Logical Ops:** Verify that the use of `&&` and `<` in XML expressions doesn't cause blank screens.
- [ ] **Vertical Spacing:** Ensure "Prefix", "Hour", and "Suffix" have clear visual separation without overlapping.

### 5.2 Circular Screen Clipping
- [ ] **Long Strings:** Test 11:27 ("around half past eleven"). Ensure text stays within the circular boundary.
- [ ] **Safe Area:** Verify info doesn't touch extreme top/bottom edges.

---

## 6. Complications
- [ ] **Slot 1 (Left):** Default should be Watch Battery. Verify numerical updates.
- [ ] **Slot 2 (Right):** Default should be System Date. Verify it matches the system clock.

---

## 7. Performance & State
- [ ] **Ambient Mode:** Use "Power" button in emulator. Background must turn pitch black (`#000000`).
- [ ] **Update Frequency:** Ensure time updates promptly when minutes change.
