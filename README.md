# AutoPay Alert — Smart Money Tracker & Fraud Shield

A native Android application built with **Jetpack Compose**, **Material 3**, and **Room Database** that automatically organizes Indian bank transaction SMS messages, detects recurring payment schedules, tracks spending budgets, provides upcoming payment reminders with regional language voice alerts, and identifies potentially fraudulent SMS messages — all with **100% on-device privacy**.

---

## 📱 Prebuilt APK Download

The compiled debug APK is prebuilt and committed directly to this repository:

- **Location:** [`apk/AutoPayAlert-debug.apk`](apk/AutoPayAlert-debug.apk)

### How to Install on your Android Phone:
1. Download `AutoPayAlert-debug.apk` onto your Android device.
2. Open the file from your phone's file manager or downloads.
3. If prompted, allow **"Install unknown apps"** for your file browser or chrome.
4. Tap **Install** and open **AutoPay Alert**!

---

## ⚙️ GitHub Actions Workflows

The repository includes three automated workflows under `.github/workflows/`:
1. **`build-apk.yml`**: Automatically builds and uploads `AutoPayAlert-debug.apk` as a downloadable artifact on every push and pull request.
2. **`release.yml`**: Automatically builds the APK and creates an official **GitHub Release** with the APK attached as a release asset whenever a version tag (e.g. `v1.0.0`) is pushed or manually triggered via "Run workflow".
3. **`ci.yml`**: Runs the full suite of unit and Robolectric tests and exports test results on every push.

---

## 🛠️ How to Build the APK from Source

### Prerequisites
- JDK 17 or higher
- Android SDK (API 34/36)
- Android Studio Ladybug / Meerkat (or Gradle CLI)

### 1. Build Debug APK
Run the following Gradle command in the root directory:
```bash
gradle assembleDebug
```
The newly compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Build Release APK / App Bundle (AAB)
To create a production-ready signed APK:
```bash
gradle assembleRelease
```
Or for Google Play Store publishing:
```bash
gradle bundleRelease
```
Output:
```
app/build/outputs/bundle/release/app-release.aab
```

---

## 🧪 Automated Testing

To run the local unit and Robolectric tests (covering bank SMS parsing, balance vs transaction amount disambiguation, refund detection, deduplication fingerprinting, and fraud detection):
```bash
gradle :app:testDebugUnitTest
```

---

## 🛡️ Privacy & Security Architecture

- **100% On-Device Processing:** All SMS messages, financial records, and balances are stored in a local SQLite database (`Room`). Zero telemetry, zero analytics, zero external API tracking, and zero advertising SDKs.
- **Exact Monetary Math:** All currency figures are stored as integer paise (`Long amountPaise`, where ₹1.00 = 100 paise) to prevent floating-point calculation inaccuracies.
- **Deduplication Engine:** Transactions are fingerprinted using a hash of bank, masked account, amount, reference number, and timestamp to avoid duplicate imports.
- **Fraud Shield:** An on-device scanner checks messages for urgent KYC freeze threats, malicious APK links, and remote access software (AnyDesk, TeamViewer) requests.
- **Data Portability:** Export your entire passbook at any time to a clean CSV file.

---

## 🚀 Key Features

1. **Financial Overview Dashboard:** Estimated balance hero card, monthly income and expense metrics, and spending charts.
2. **Smart Passbook:** Filterable timeline grouped by date with bank and category chips, search, and manual transaction entry.
3. **Recurring Payments & AutoPay Detection:** Schedule detection (monthly, weekly, quarterly, annual) for Netflix, Spotify, broadband, SIP investments, and EMIs.
4. **Upcoming Payment Reminders & Voice Alerts:** Text-to-Speech spoken reminders in English, Hindi, Tamil, Telugu, Kannada, and Marathi.
5. **Fraud Shield Security:** Transparent risk assessment (`INFORMATIONAL`, `CAUTION`, `HIGH_RISK`) with actionable safety checklists.
6. **SMS Parser Sandbox:** Test raw SMS strings against the parser and fraud engine in real-time.
