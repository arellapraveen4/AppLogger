package com.example.applogger;

import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PermissionSetupActivity extends AppCompatActivity {

    LinearLayout container;
    private static final String TAG = "PermissionSetup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_setup);

        container = findViewById(R.id.permissionContainer);

        Log.d(TAG, "onCreate: Loading permission UI...");
        loadPermissionList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume: Re-checking permissions...");
        loadPermissionList();

        // Check permissions with debug logs
        boolean usage = isUsageAccessGranted();
        boolean notif = isNotificationAllowed();

        Log.d(TAG, "Permission Check → Usage Access = " + usage);
        Log.d(TAG, "Permission Check → Notifications = " + notif);

        if (!usage) Toast.makeText(this, "Usage Access NOT allowed", Toast.LENGTH_SHORT).show();
        if (!notif) Toast.makeText(this, "Notification Permission NOT allowed", Toast.LENGTH_SHORT).show();

        // Now check if all required = OK
        if (usage && notif) {
            Log.d(TAG, "All required permissions granted → Opening MainActivity");
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Log.d(TAG, "Permissions missing → Staying on setup screen");
        }
    }

    // ==========================================
    // BUILD PERMISSION CARDS
    // ==========================================
    private void loadPermissionList() {
        container.removeAllViews();

        addPermissionCard("Usage Access",
                isUsageAccessGranted(),
                this::openUsageAccess);

        addPermissionCard("Notifications",
                isNotificationAllowed(),
                this::openNotificationSettings);

        addPermissionCard("Restricted Settings (Optional)",
                !isRestrictedSettingsBlocked(),
                this::openRestrictedSettings);
    }


    private void addPermissionCard(String title, boolean enabled, Runnable onAllow) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 40, 40, 40);
        card.setBackgroundColor(Color.parseColor("#EEEEEE"));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.setMargins(0, 20, 0, 0);
        card.setLayoutParams(params);

        TextView tv = new TextView(this);
        tv.setText(title + " → " + (enabled ? "Allowed" : "Not Allowed"));
        tv.setTextSize(18);
        tv.setTextColor(enabled ? Color.parseColor("#0F9D58") : Color.parseColor("#D32F2F"));

        Button btn = new Button(this);
        btn.setText(enabled ? "OK" : "Allow");
        btn.setEnabled(!enabled);
        btn.setOnClickListener(v -> onAllow.run());

        card.addView(tv);
        card.addView(btn);

        container.addView(card);
    }

    // ==========================================
    // PERMISSION CHECKERS WITH DEBUG LOGS
    // ==========================================

    private boolean isUsageAccessGranted() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            int mode = appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName()
            );

            Log.d(TAG, "Usage Access Mode = " + mode);

            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.e(TAG, "Usage Access check failed", e);
            return false;
        }
    }

    private boolean isNotificationAllowed() {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            boolean allowed = nm.areNotificationsEnabled();
            Log.d(TAG, "Notifications Allowed = " + allowed);
            return allowed;
        } catch (Exception e) {
            Log.e(TAG, "Notification check failed", e);
            return false;
        }
    }

    private boolean isRestrictedSettingsBlocked() {
        if (Build.VERSION.SDK_INT < 33) return false;

        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            int mode = appOps.unsafeCheckOpNoThrow(
                    "android:restricted_settings",
                    android.os.Process.myUid(),
                    getPackageName()
            );
            Log.d(TAG, "Restricted Settings Mode = " + mode);
            return mode != AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.e(TAG, "Restricted settings check failed", e);
            return false;
        }
    }

    // ==========================================
    // OPEN SETTINGS
    // ==========================================

    private void openUsageAccess() {
        Log.d(TAG, "Opening Usage Access screen...");
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private void openNotificationSettings() {
        Log.d(TAG, "Opening Notification Settings...");
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    private void openRestrictedSettings() {
        Log.d(TAG, "Opening Restricted Settings...");
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }
}
