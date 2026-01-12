

# 📱 Smart App Logger – Detailed Project Description

## 1. Project Overview

**Smart App Logger** is an advanced Android system-monitoring application that continuously tracks:

* 📊 **Foreground app usage** (open/close events with duration)
* 📞 **Call logs** (incoming, outgoing, missed, rejected)
* 🔔 (Planned/available) **Notifications & message history**
* ☁️ **Secure cloud backup** using Dropbox

The app runs **reliably in the background**, even on **Android 13–15**, using:

* Foreground Service
* WorkManager (recursive safe loops)
* Battery optimization whitelisting

All logs are stored **locally** in text files and can be **viewed, filtered, and uploaded** from the UI.

---

## 2. High-Level Architecture

```
MainActivity
   │
   ├── UsageMonitorService (Foreground)
   │       ├── UsageWorker (App usage tracking)
   │       └── CallLogWorker (Call log backup)
   │
   ├── AppUsageActivity (UI + Live updates)
   ├── CallLogsActivity (UI + filters)
   └── Dropbox Cloud Backup
```

---

## 3. MainActivity – Control Center

### Purpose

Acts as the **dashboard** and **control panel** of the app.

### Key Features

#### 🔘 Master Switch

* Starts/stops **UsageMonitorService**
* Saves state using `SharedPreferences`
* Automatically requests:

  * Usage Access permission
  * Notification permission
  * Battery optimization exemption

#### 🧩 Feature Cards

Each card opens a dedicated module:

* App Usage Logs
* Call Logs
* Message Logs
* Notification History

#### 🔋 Battery Optimization Handling

```java
ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
```

Ensures background logging is **not killed by the system**.

---

## 4. UsageMonitorService – Background Engine

### Why a Foreground Service?

Android aggressively kills background tasks.
Running as a **foreground service** ensures:

* Continuous logging
* Higher priority
* System visibility via notification

### What it does

On creation:

* Starts **UsageWorker**
* Starts **CallLogWorker**
* Shows persistent notification:

  > “Smart Logger Running – Tracking App Usage & Call Logs”

---

## 5. UsageWorker – App Usage Tracking (Core Logic)

### How app usage is detected

Uses:

```java
UsageStatsManager + UsageEvents
```

It detects:

* `MOVE_TO_FOREGROUND`
* `ACTIVITY_RESUMED`

### Logic Flow

1. Get **current foreground app**
2. Compare with **last saved app**
3. If changed:

   * Close previous app (calculate duration)
   * Log close event
   * Log new app open event

### Example Log Entry

```
[2026-01-12 10:41:02] Instagram opened
[2026-01-12 10:43:10] Instagram closed (2m 8s)
```

### Smart Features

* 🚫 Ignores system apps & itself
* ⏱️ Debounce window (prevents spam logs)
* 🔁 Recursive WorkManager loop (Android 15 safe)
* 📢 Sends live broadcast to UI

---

## 6. AppUsageActivity – Live App Usage UI

### What this screen does

* Reads `AppUsageLog.txt`
* Displays logs in a RecyclerView
* Updates **in real time** using BroadcastReceiver

### Key Features

#### 📅 Date Filter

* Pick any date
* Shows only logs from that day

#### 🔄 Live Updates

When `UsageWorker` logs an event:

```java
sendBroadcast(ACTION_LOG_ADDED)
```

UI instantly updates **without reload**

#### 👤 Contact Name Resolution

If the “app name” is actually a number:

* Resolves it using `ContactsContract`
* Displays contact name instead of raw number

#### ☁️ Dropbox Upload

Uploads logs securely using:

```http
POST https://content.dropboxapi.com/2/files/upload
```

---

## 7. CallLogWorker – Background Call Logger

### What it does

* Periodically checks device call logs
* Stores **only new calls**
* Prevents duplicate entries using last saved call ID

### Example Entry Format

```
12345|12 Jan 2026, 10:30 AM|Incoming|9876543210|45
```

### Why WorkManager?

* Safe execution on Android 12+
* Survives reboot & background limits
* Recursive scheduling every 15 seconds

---

## 8. CallLogsActivity – Call Log Viewer

### Features

#### 📞 Live Call Log Display

* Reads directly from device call logs
* Shows:

  * Contact name
  * Phone number
  * Call type
  * Duration
  * Contact photo

#### 📆 Date Range Filter

* From date
* To date
* Reset filter instantly

#### ☁️ Dropbox Backup

Uploads `CallBackup.txt` securely

---

## 9. Data Storage Strategy

| Data Type | Storage           |
| --------- | ----------------- |
| App usage | `AppUsageLog.txt` |
| Call logs | `CallBackup.txt`  |
| States    | SharedPreferences |
| Cloud     | Dropbox API       |

✔ Lightweight
✔ No database overhead
✔ Easy export & audit

---

## 10. Security & Permissions Used

* `PACKAGE_USAGE_STATS`
* `READ_CALL_LOG`
* `READ_CONTACTS`
* Foreground service permission
* Battery optimization exemption

> Permissions are **explicitly requested** and **user-controlled**

---

## 11. Why This Project Is Impressive

✅ Uses **system-level Android APIs**
✅ Handles **Android 15 background restrictions**
✅ Real-time UI sync with background workers
✅ Cloud backup integration
✅ Modular, scalable design

---

## 12. How You Can Describe This in Interviews

> “I built a Smart App Logger that tracks foreground app usage and call logs in real time using UsageStatsManager, WorkManager, and a foreground service. It safely runs on Android 15, logs app open/close durations, syncs data live to the UI, and supports secure cloud backups using Dropbox.”

---

## 13. Important Note (Very Honest)

This project is:

* ✔ **System monitoring**
* ✔ **Digital forensics**
* ✔ **Parental control / usage analytics**
* ✔ **Cybersecurity-adjacent**

⚠️ **NOT malware** if:

* User consent is taken
* Permissions are transparent
* No hidden exfiltration

---

