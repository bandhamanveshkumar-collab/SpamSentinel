# SpamSentinel: Agentic AI SMS Spam Detection & Adaptive Learning (IEEE Implementation)

[![Android](https://img.shields.io/badge/Platform-Android_8.0%2B_(API_26%2B)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25_On--Device_&_Offline-success.svg)](#)

SpamSentinel is a production-grade, premium Android application implementing the research paper:
> **"An Agentic AI Framework for SMS Spam Detection with Explainable, Confidence-Aware and Feedback-Driven Adaptive Learning"**
> *Authors: E. Goutami, S. Sabir Hussain, B. Anvesh Kumar, S. Fahad (Santhiram Engineering College, Autonomous, Nandyal)*

---

## 🌟 Key Architecture & Highlights

- **100% On-Device Execution**: Zero internet permissions required (`INTERNET` permission is omitted from `AndroidManifest.xml`).
- **Linear SVM + Platt Calibration**: TF-IDF unigram + bigram vectorizer mapped through Platt sigmoid calibration for probabilistic confidence scoring.
- **Explainable Agentic Pipeline**: Computes feature contribution weights ($\phi_i = w_i x_i$) to provide positive (spam) and negative (ham) explanations for every classification.
- **Support Threat Signals Layer**: Offline URL shortener detection, raw IP detection, punycode/risky TLD flags, OTP Guard, and sender classification (Alphanumeric Headers vs 10-digit mobile).
- **Feedback-Driven Adaptive Learning**: Buffered user corrections fine-tune model weights on-device via online SGD. Automatically promoted only if validation F1 drop is $\le 1\%$, otherwise safely rolled back with audit logging.
- **Zero Third-Party Chart Dependencies**: Every chart (Donut, 7-day Line, Hourly Bar, Grouped Benchmark Bar, Diverging Explainer, Heatmap, Sparklines, Semi-Circle Gauge) is drawn using pure Jetpack Compose Canvas.

---

## 🚀 Setup & Setup Instructions

### Step 1: Run ML Training Script in Google Colab (Optional / Pre-Bundled)
The repository includes pre-trained bundled assets in `app/src/main/assets/`. To train from scratch or verify parity:
1. Open Google Colab and paste the contents of [`tools/train_model.py`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/tools/train_model.py).
2. Run the script. It automatically downloads the UCI SMS Spam Collection dataset, trains 4 models (Naive Bayes, Logistic Regression, Random Forest, Linear SVM), fits Platt calibration, and exports:
   - `assets/model.json`
   - `assets/benchmark.json`
   - `assets/validation.json`
   - `assets/parity.json`
3. Copy the generated JSON files into `app/src/main/assets/`.

### Step 2: Build & Install on Android Studio
1. Open the project in Android Studio Ladybug (2024.2.1+).
2. Sync Gradle files.
3. Build and run the `:app` target on an Android Emulator or physical device (Android 8.0+ / API 26+).

### Step 3: Grant Permissions & Test on Emulator
1. Launch SpamSentinel.
2. Complete the onboarding screen or tap **"Scan Inbox & Enable Protection"**.
3. Grant `RECEIVE_SMS` and `READ_SMS` permissions when prompted.
4. On Android Emulator, open **Extended Controls (`...`)** -> **Phone** -> **SMS**.
5. Send any test SMS to simulate incoming messages in real-time.

---

## 🧪 12 Demo Test SMS Messages

Test these exact SMS samples via the Android Emulator or the in-app **Live Checker** tab:

### 🚨 4 BLOCK (Spam / Scam)
1. **KYC Phishing**:
   - **Sender**: `AX-HDFCBK`
   - **Body**: `Urgent: Your HDFC Bank account will be suspended today. Update PAN immediately at http://hdfc-bank-kyc.xyz`
2. **Prize / Lottery Scam**:
   - **Sender**: `VM-PRIZE`
   - **Body**: `WINNER! You have won £900,000 cash reward in UK Lottery. Call 08000215888 now to claim your prize!`
3. **Electricity Bill Scam**:
   - **Sender**: `BZ-POWER`
   - **Body**: `Dear Customer, Electricity power disconnection notice for tonight 9:30 PM due to unpaid bill. Call officer immediately.`
4. **Part-time Job Scam**:
   - **Sender**: `+919876543210`
   - **Body**: `Part-time job offer! Earn Rs 5000/day by reviewing YouTube videos. Contact HR on WhatsApp now.`

### ⚠️ 4 REVIEW (Borderline / High Risk)
5. **Personal Number Promotional Spam**:
   - **Sender**: `+919812345678`
   - **Body**: `Flat 50% discount on instant personal loans up to 5 Lakhs! Click link to apply now.`
6. **Unrecognized Shortcode Offer**:
   - **Sender**: `56767`
   - **Body**: `You have an important unread message waiting in your portal inbox. Log in to check.`
7. **Delivery Address Update Suspicious**:
   - **Sender**: `IN-POST`
   - **Body**: `Your parcel delivery failed due to incomplete address. Update at http://cutt.ly/delivery-update`
8. **Crypto Investment Offer**:
   - **Sender**: `CRYPTO-VIP`
   - **Body**: `Guaranteed daily returns of 10% on Crypto trading signals! Join Telegram group.`

### 🛡️ 4 ALLOW (Legitimate / Safe)
9. **Legitimate Bank OTP (Protected by OTP Guard)**:
   - **Sender**: `AX-HDFCBK`
   - **Body**: `Your OTP for netbanking transaction of Rs 5,000 is 482910. Valid for 10 mins. Do not share with anyone.`
10. **Personal Conversation**:
    - **Sender**: `+919876543210`
    - **Body**: `Hey mate, are we still meeting for lunch today at 1 PM near office?`
11. **HR Meeting Confirmation**:
    - **Sender**: `HR-DEPT`
    - **Body**: `Your interview is scheduled for tomorrow at 11 AM at Bangalore HQ.`
12. **Courier Status Update**:
    - **Sender**: `IN-POST`
    - **Body**: `Your parcel #IN84920 has been dispatched and will be delivered by today 6 PM.`

---

## 🎓 Viva Cheat-Sheet (Research Mapping)

| Paper Section & Concept | App Implementation File / Class | Viva Defense Explanation |
| :--- | :--- | :--- |
| **Section 1: Agentic AI Overview** | [`DecisionAgent.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/domain/agents/DecisionAgent.kt) | Orchestrates multi-agent processing: Perceive (Preprocessing), Score (Linear SVM + Platt), Decide ($\tau$ thresholds), Explain ($\phi$ term weights), and Learn (SGD fine-tuning). |
| **Section 2: Preprocessing & Vectorization** | [`TextCleaner.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/domain/ml/TextCleaner.kt), [`TfIdfVectorizer.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/domain/ml/TfIdfVectorizer.kt) | Text cleaner applies lowercasing, regex token replacement (`urltoken`, `longnum`), and special char removal. TF-IDF extracts unigrams+bigrams with sublinear TF ($1+\ln tf$) and L2 norm. |
| **Section 3: Model & Calibration** | [`LinearSpamModel.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/domain/ml/LinearSpamModel.kt) | Linear SVM computes raw decision score $s = w \cdot x + b$. Platt calibration maps score to calibrated probability $p = \frac{1}{1 + e^{-(A s + B)}}$. |
| **Section 3: Symmetric Thresholding ($\tau$)** | [`DecisionAgent.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/domain/agents/DecisionAgent.kt) | Message blocked if $p \ge \tau_{high}$, allowed if $p \le \tau_{low}$, else routed to manual REVIEW queue. |
| **Section 4: Threat Signals & OTP Guard** | [`UrlAnalyzer.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/domain/signals/UrlAnalyzer.kt), [`OtpGuard.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/domain/signals/OtpGuard.kt) | Inspects URL shorteners, raw IP hosts, punycode, and risky TLDs. OTP Guard ensures legitimate bank OTPs without links are never falsely blocked. |
| **Section 5: Real-time Ingestion** | [`SmsReceiver.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/receiver/SmsReceiver.kt) | BroadcastReceiver listening on priority 999. Uses `goAsync()` to evaluate incoming multipart SMS asynchronously on background threads in <10ms. |
| **Section 6: Local Data Layer** | [`AppDatabase.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/data/db/AppDatabase.kt) | Room database holding messages, feedback buffer, model history, sender rules, and audit logs. |
| **Section 7: Adaptive Fine-Tuning** | [`FeedbackAgent.kt`](file:///C:/Users/bandh/AndroidStudioProjects/MyApplication2/app/src/main/java/com/spamsentinel/app/domain/agents/FeedbackAgent.kt) | User corrections trigger online SGD fine-tuning. Promotes new weights only if validation F1 drop $\le 1\%$, preventing catastrophic forgetting. |
