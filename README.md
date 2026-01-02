

# 📱 AppLogger – Android App & Call Usage Logger with Dropbox Backup

## 🔍 Overview
**AppLogger** is an Android application developed using **Java** in **Android Studio**, compatible with **Android 15 (API level 35)**.  
The app automatically monitors **application usage** and **call activity** in the background and stores logs locally as **text (.txt) files**.  
Users can control logging through a **toggle button**, and the collected logs can later be **uploaded to Dropbox** for secure cloud backup.

This project demonstrates background execution, permission handling, and cloud integration under modern Android constraints.

---

## 🎯 Project Objectives
- Automatically track app usage and call logs
- Run logging tasks in the background
- Allow user-controlled logging via toggle switch
- Append logs to local `.txt` files
- Upload logs to Dropbox cloud storage
- Ensure compatibility with Android 15 (API 35)

---

## ✨ Features

### 📊 App Usage Logging
- Automatically detects app launches and usage duration
- Records:
  - Application name
  - Package name
  - Start time
  - End time
  - Total usage duration
  - Date
- Logs are appended continuously to a local text file

**Sample App Usage Log (`app_usage_log.txt`):**
```

App Name: WhatsApp
Package: com.whatsapp
Start Time: 10:15 AM
End Time: 10:45 AM
Usage Duration: 30 minutes
Date: 02-01-2026
----------------

```

---

### 📞 Call Usage Logging
- Automatically logs phone call activity
- Captures:
  - Call type (Incoming / Outgoing / Missed)
  - Phone number (masked if required)
  - Call start time
  - Call end time
  - Call duration
  - Date
- Logs are appended to a separate text file

**Sample Call Log (`call_usage_log.txt`):**
```

Call Type: Incoming
Phone Number: +91XXXXXXXXXX
Start Time: 3:02 PM
Duration: 5 min 20 sec
Date: 02-01-2026
----------------

```

---

### 🔄 Background Logging (Automatic)
- Logging runs automatically in the background
- Uses:
  - Foreground/background services
  - System event listeners
- Continues even when the app is minimized
- Optimized for Android 15 background execution limits

---

### 🎚️ Toggle Button Control
- A toggle button allows the user to:
  - ▶️ Start logging
  - ⏹️ Stop logging
- When OFF:
  - No data is collected
- When ON:
  - App usage and call logs are recorded automatically

This ensures transparency and user control.

---

### 💾 Local Storage (TXT Files)
- Logs are stored as **plain `.txt` files**
- Data is appended (no overwriting)
- Files are saved in app’s internal storage
- Example files:
  - `app_usage_log.txt`
  - `call_usage_log.txt`

---

### ☁️ Dropbox Cloud Upload
- User authenticates Dropbox account
- Local log files are uploaded on demand
- Ensures safe backup and remote access
- Logs remain available even after app uninstall or device change

---

## 🔐 Permissions Used

| Permission | Purpose |
|----------|---------|
| Usage Access | Monitor app usage |
| READ_CALL_LOG | Read call history |
| READ_PHONE_STATE | Detect call state |
| FOREGROUND_SERVICE | Run background logging |
| INTERNET | Upload logs to Dropbox |

> All permissions are requested explicitly and used only when logging is enabled.

---

## 🏗️ Technology Stack
- **Language:** Java
- **IDE:** Android Studio
- **Target SDK:** Android 15 (API 35)
- **Components Used:**
  - Services (Foreground & Background)
  - Broadcast Receivers
  - UsageStatsManager
  - Call Log Content Provider
  - File I/O (Append Mode)
  - Dropbox REST API

---

## 🚀 Application Workflow
1. User installs and opens the app
2. Required permissions are granted
3. User enables logging using toggle button
4. Background service starts automatically
5. App and call usage data is logged to `.txt` files
6. User uploads logs to Dropbox when required

---

## 🧠 Key Learning Outcomes
- Android background execution in API 35
- Handling sensitive system permissions
- Efficient file handling using append mode
- Cloud integration using third-party APIs
- User-controlled background processes

---

## 🔮 Future Enhancements
- Encrypt log files before upload
- Dashboard with usage analytics
- Auto-scheduled cloud backups
- Export logs as CSV or PDF
- Google Drive support

---

## 📌 Disclaimer
This application is developed **strictly for educational purposes**.  
Users must follow privacy laws and obtain consent before monitoring usage data.

---

## 👤 Author
**Praveen Arella**
arellapraveen4@gmail.com
Engineering Student | Android Developer  
```

