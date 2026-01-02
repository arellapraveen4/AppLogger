package com.example.applogger;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class NotificationHistoryActivity extends AppCompatActivity {

    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_history);

        container = findViewById(R.id.notifyContainer);

        loadNotifications();
    }

    private void loadNotifications() {
        try {
            File file = new File(getFilesDir(), "NotifyBackup.txt");
            if (!file.exists() || file.length() == 0) return;

            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line;
            LayoutInflater inflater = LayoutInflater.from(this);

            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length < 4) continue;

                String app = p[0];
                String date = p[1];
                String title = p[2];
                String text = p[3];

                View row = inflater.inflate(R.layout.item_notification, container, false);

                ((TextView) row.findViewById(R.id.notify_app)).setText(app);
                ((TextView) row.findViewById(R.id.notify_title)).setText(title);
                ((TextView) row.findViewById(R.id.notify_text)).setText(text);
                ((TextView) row.findViewById(R.id.notify_date)).setText(date);

                container.addView(row);
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
