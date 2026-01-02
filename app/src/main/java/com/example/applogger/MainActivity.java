package com.example.applogger;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CardView cardAppUsage, cardCallLogs, cardMessageLogs, cardNotificationHistory;
    private SwitchCompat masterSwitch;

    private static final String PREFS = "LoggerPrefs";
    private static final String PREF_MASTER = "master_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional: if you still use a separate permission setup screen
        if (!allPermissionsGranted()) {
            Log.d(TAG, "Permissions missing → go to PermissionSetupActivity");
            startActivity(new Intent(this, PermissionSetupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        initViews();
        initCards();
        initMasterSwitch();
    }

    private void initViews() {
        masterSwitch = findViewById(R.id.masterSwitch);
        cardAppUsage = findViewById(R.id.cardAppUsage);
        cardCallLogs = findViewById(R.id.cardCallLogs);
        cardMessageLogs = findViewById(R.id.cardMessageLogs);
        cardNotificationHistory = findViewById(R.id.cardNotificationHistory);
    }

    private void initCards() {
        setCardListener(cardAppUsage, AppUsageActivity.class);
        setCardListener(cardCallLogs, CallLogsActivity.class);
        setCardListener(cardMessageLogs, MessageLogsActivity.class);
        setCardListener(cardNotificationHistory, NotificationHistoryActivity.class);
    }

    private void setCardListener(CardView card, Class<?> target) {
        if (card == null) return;

        card.setOnClickListener(v -> {
            animate(card);
            startActivity(new Intent(MainActivity.this, target));
        });
    }

    private void animate(CardView card) {
        card.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(90)
                .withEndAction(() -> card.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(90));
    }

    private void initMasterSwitch() {
        if (masterSwitch == null) {
            Log.e(TAG, "masterSwitch view is null – check activity_main.xml id");
            return;
        }

        boolean enabled = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_MASTER, false);

        masterSwitch.setChecked(isServiceRunning());

        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_MASTER, isChecked)
                    .apply();

            if (isChecked) {
                requestBatteryWhitelist();
                startLoggerService();
                Toast.makeText(this, "Logging enabled", Toast.LENGTH_SHORT).show();
            } else {
                stopLoggerService();
                Toast.makeText(this, "Logging disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startLoggerService() {
        Intent i = new Intent(this, UsageMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(i);
        else
            startService(i);
    }

    private void stopLoggerService() {
        stopService(new Intent(this, UsageMonitorService.class));

        // If you also use WorkManager loop inside service, you can cancel by tag/name there
        // or here using WorkManager if needed.
    }

    private boolean isServiceRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;

        for (ActivityManager.RunningServiceInfo svc :
                am.getRunningServices(Integer.MAX_VALUE)) {
            if (UsageMonitorService.class.getName().equals(svc.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void requestBatteryWhitelist() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName())
                    );
                    startActivity(intent);
                } catch (Exception ignored) {}
            }
        }
    }

    /* ---------------- Permission helpers (usage + notifications) ---------------- */

    private boolean allPermissionsGranted() {
        return isUsageAccessGranted() && isNotificationAllowed();
    }

    private boolean isUsageAccessGranted() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            if (appOps == null) return false;

            int mode = appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName()
            );
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNotificationAllowed() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        return nm != null && nm.areNotificationsEnabled();
    }
}
