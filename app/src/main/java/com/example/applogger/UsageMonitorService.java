package com.example.applogger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class UsageMonitorService extends Service {

    private static final String CHANNEL_ID = "LOGGER_SERVICE";
    private static final String USAGE_JOB = "USAGE_MONITOR_LOOP";
    private static final String CALL_JOB  = "CALL_MONITOR_LOOP";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotification();
        startUsageLoop();
        startCallLoop();   // ✅ START CALL LOGGER ALSO
    }

    private void startUsageLoop() {

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(UsageWorker.class)
                .setInitialDelay(2, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork(USAGE_JOB, ExistingWorkPolicy.REPLACE, req);
    }

    private void startCallLoop() {

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(CallLogWorker.class)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork(CALL_JOB, ExistingWorkPolicy.REPLACE, req);
    }

    private void createNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Smart Logger",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class)
                    .createNotificationChannel(ch);
        }

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_apps)
                .setContentTitle("Smart Logger Running")
                .setContentText("Tracking App Usage & Call Logs")
                .setOngoing(true)
                .build();

        startForeground(1101, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
