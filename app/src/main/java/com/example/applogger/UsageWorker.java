package com.example.applogger;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class UsageWorker extends Worker {

    private static final String LOG_FILE = "AppUsageLog.txt";

    private static final String PREFS = "LoggerPrefs";
    private static final String PREF_LAST_APP = "last_app";
    public static final String EXTRA_PKG = "pkg";

    private static final String PREF_LAST_OPEN_TIME = "open_time";
    private static final String PREF_LAST_EVENT_TYPE = "last_event";
    private static final String PREF_LAST_LOG_TIME = "last_log_time";

    private static final String LOOP_NAME = "USAGE_MONITOR_LOOP";

    private static final long DEBOUNCE_WINDOW = 1200;

    private final PackageManager pm;

    private static final Set<String> ignored = new HashSet<>(Arrays.asList(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.android.launcher3",
            "com.example.applogger"
    ));

    public UsageWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        pm = context.getPackageManager();
    }

    @NonNull
    @Override
    public Result doWork() {

        Context ctx = getApplicationContext();
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (!hasUsagePermission()) {
            return scheduleNext();
        }

        String current = getCurrentForegroundApp();
        long now = System.currentTimeMillis();

        if (current == null || ignored.contains(current)) {
            handleAppClosedIfRequired(sp, now);
            return scheduleNext();
        }

        String lastApp = sp.getString(PREF_LAST_APP, "");
        String lastEvent = sp.getString(PREF_LAST_EVENT_TYPE, "");
        long lastLogTime = sp.getLong(PREF_LAST_LOG_TIME, 0);

        // ✅ NEW APP OPENED
        if (!current.equals(lastApp)) {

            if (now - lastLogTime < DEBOUNCE_WINDOW) {
                return scheduleNext();
            }

            String ts = timestamp();

            // ✅ CLOSE PREVIOUS APP PROPERLY
            if (!lastApp.isEmpty() && !ignored.contains(lastApp) && !"closed".equals(lastEvent)) {

                long openTime = sp.getLong(PREF_LAST_OPEN_TIME, now);
                long duration = now - openTime;

                write("[" + ts + "] " + getName(lastApp) +
                        " closed (" + durationFormat(duration) + ")\n");

                broadcast(lastApp, getName(lastApp),
                        "closed (" + durationFormat(duration) + ")", ts);
            }

            // ✅ OPEN NEW APP
            write("[" + ts + "] " + getName(current) + " opened\n");
            broadcast(current, getName(current), "opened", ts);

            sp.edit()
                    .putString(PREF_LAST_APP, current)
                    .putLong(PREF_LAST_OPEN_TIME, now)
                    .putString(PREF_LAST_EVENT_TYPE, "opened")
                    .putLong(PREF_LAST_LOG_TIME, now)
                    .apply();
        }

        return scheduleNext();
    }

    // ✅ ANDROID 15 SAFE RECURSIVE LOOP
    private Result scheduleNext() {

        OneTimeWorkRequest next = new OneTimeWorkRequest.Builder(UsageWorker.class)
                .setInitialDelay(2, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork(
                        LOOP_NAME,
                        ExistingWorkPolicy.REPLACE,
                        next
                );

        return Result.success();
    }

    // ✅ ANDROID 15 SAFE USAGE PERMISSION CHECK
    private boolean hasUsagePermission() {
        try {
            AppOpsManager ops = (AppOpsManager)
                    getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);

            int mode = ops.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    getApplicationContext().getPackageName()
            );

            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private String getCurrentForegroundApp() {

        UsageStatsManager usm = (UsageStatsManager)
                getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);

        long end = System.currentTimeMillis();
        long start = end - 6000;

        UsageEvents events = usm.queryEvents(start, end);
        UsageEvents.Event event = new UsageEvents.Event();

        String last = null;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                last = event.getPackageName();
            }
        }

        return last;
    }

    private void handleAppClosedIfRequired(SharedPreferences sp, long now) {

        String lastApp = sp.getString(PREF_LAST_APP, "");
        String lastEvent = sp.getString(PREF_LAST_EVENT_TYPE, "");

        if (!lastApp.isEmpty() && !"closed".equals(lastEvent)) {

            long openTime = sp.getLong(PREF_LAST_OPEN_TIME, now);
            long duration = now - openTime;
            String ts = timestamp();

            write("[" + ts + "] " + getName(lastApp) +
                    " closed (" + durationFormat(duration) + ")\n");

            broadcast(lastApp, getName(lastApp),
                    "closed (" + durationFormat(duration) + ")", ts);

            sp.edit()
                    .putString(PREF_LAST_EVENT_TYPE, "closed")
                    .apply();
        }
    }

    private void write(String line) {
        try (FileOutputStream fos = getApplicationContext()
                .openFileOutput(LOG_FILE, Context.MODE_APPEND)) {
            fos.write(line.getBytes());
        } catch (Exception ignored) {}
    }

    private String getName(String pkg) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    private String durationFormat(long ms) {
        long sec = ms / 1000;
        long min = sec / 60;
        long hr = min / 60;

        if (hr > 0) return hr + "h " + (min % 60) + "m " + (sec % 60) + "s";
        if (min > 0) return min + "m " + (sec % 60) + "s";
        return sec + "s";
    }

    private void broadcast(String pkg, String name, String status, String ts) {
        Intent i = new Intent(AppUsageActivity.ACTION_LOG_ADDED);
        i.putExtra(AppUsageActivity.EXTRA_PKG, pkg);
        i.putExtra(AppUsageActivity.EXTRA_APP_NAME, name);
        i.putExtra(AppUsageActivity.EXTRA_STATUS, status);
        i.putExtra(AppUsageActivity.EXTRA_TIMESTAMP, ts);
        getApplicationContext().sendBroadcast(i);
    }
}
