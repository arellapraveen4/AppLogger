package com.example.applogger;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.format.DateFormat;

import java.io.FileOutputStream;
import java.util.Date;

public class NotificationListener extends NotificationListenerService {

    private static final String FILE = "NotifyBackup.txt";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        try {

            String app = sbn.getPackageName();
            String title = sbn.getNotification().extras.getString("android.title", "No Title");
            String text = sbn.getNotification().extras.getString("android.text", "No Content");

            long time = sbn.getPostTime();
            String date = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(time)).toString();

            if (text != null) text = text.replace("\n", " ").replace("|", " ");
            if (title != null) title = title.replace("|", " ");

            String line = app + "|" + date + "|" + title + "|" + text + "\n";

            FileOutputStream fos = getApplicationContext().openFileOutput(FILE, MODE_APPEND);
            fos.write(line.getBytes());
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
