package com.example.applogger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CallLog;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.FileOutputStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CallLogWorker extends Worker {

    private static final String LOG_FILE = "CallBackup.txt";
    private static final String PREFS = "CallPrefs";
    private static final String PREF_LAST_ID = "lastCallId";
    private static final String LOOP_NAME = "CALL_MONITOR_LOOP";

    public CallLogWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        try {
            backupNewCallsOnce();
        } catch (Exception ignored) {}

        // ✅ Repeat every 15 seconds safely
        OneTimeWorkRequest next = new OneTimeWorkRequest.Builder(CallLogWorker.class)
                .setInitialDelay(15, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork(
                        LOOP_NAME,
                        ExistingWorkPolicy.REPLACE,
                        next
                );

        return Result.success();
    }

    private void backupNewCallsOnce() {

        Context ctx = getApplicationContext();
        long lastSavedId = getLastSavedCallId(ctx);
        long newestId = lastSavedId;

        Cursor cursor = ctx.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{
                        CallLog.Calls._ID,
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION,
                        CallLog.Calls.TYPE
                },
                null,
                null,
                CallLog.Calls._ID + " DESC"
        );

        if (cursor == null) return;

        StringBuilder buffer = new StringBuilder();

        try {
            while (cursor.moveToNext()) {

                long id = cursor.getLong(0);
                if (id <= lastSavedId) break;

                String number = cursor.getString(1);
                long dateMillis = cursor.getLong(2);
                String duration = cursor.getString(3);
                int type = cursor.getInt(4);

                String dateStr = DateFormat.format(
                        "dd MMM yyyy, hh:mm a",
                        new Date(dateMillis)
                ).toString();

                buffer.append(id).append("|")
                        .append(dateStr).append("|")
                        .append(callType(type)).append("|")
                        .append(number).append("|")
                        .append(duration).append("\n");

                if (id > newestId) newestId = id;
            }

        } finally {
            cursor.close();
        }

        if (buffer.length() > 0) {
            writeToFile(ctx, buffer.toString());
            saveLastCallId(ctx, newestId);
        }
    }

    private void writeToFile(Context ctx, String text) {
        try (FileOutputStream fos = ctx.openFileOutput(LOG_FILE, Context.MODE_APPEND)) {
            fos.write(text.getBytes());
        } catch (Exception ignored) {}
    }

    private void saveLastCallId(Context ctx, long id) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putLong(PREF_LAST_ID, id).apply();
    }

    private long getLastSavedCallId(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getLong(PREF_LAST_ID, -1);
    }

    private String callType(int t) {
        switch (t) {
            case CallLog.Calls.INCOMING_TYPE: return "Incoming";
            case CallLog.Calls.OUTGOING_TYPE: return "Outgoing";
            case CallLog.Calls.MISSED_TYPE:   return "Missed";
            case CallLog.Calls.REJECTED_TYPE: return "Rejected";
            default: return "Unknown";
        }
    }
}
