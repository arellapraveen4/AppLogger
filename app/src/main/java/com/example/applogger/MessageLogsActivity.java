package com.example.applogger;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

public class MessageLogsActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 200;
    private static final String SMS_FILE = "SmsBackup.txt";

    private LinearLayout smsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_logs);

        smsContainer = findViewById(R.id.smsContainer);

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST
            );
        } else {
            backupSmsLogs();
            loadSmsFromDevice();
        }
    }

    private boolean hasPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int rq, String[] perms, int[] res) {
        if (rq == PERMISSION_REQUEST && hasPermissions()) {
            backupSmsLogs();
            loadSmsFromDevice();
        } else {
            Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
        }
    }

    // --------------------------
    // LAST SAVED SMS ID STORAGE
    // --------------------------
    private void saveLastSmsId(long id) {
        getSharedPreferences("SmsPrefs", MODE_PRIVATE)
                .edit()
                .putLong("lastSmsId", id)
                .apply();
    }

    private long getLastSavedSmsId() {
        return getSharedPreferences("SmsPrefs", MODE_PRIVATE)
                .getLong("lastSmsId", -1);
    }

    private boolean isTxtEmpty() {
        File file = new File(getFilesDir(), SMS_FILE);
        return !file.exists() || file.length() == 0;
    }

    // --------------------------
    // BACKUP SMS LOGS
    // --------------------------
    private void backupSmsLogs() {

        boolean firstTime = isTxtEmpty();
        long lastSavedId = getLastSavedSmsId();
        long newestId = lastSavedId;

        Cursor cursor = getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                Telephony.Sms._ID + " DESC"
        );

        if (cursor == null) return;

        StringBuilder buffer = new StringBuilder();

        while (cursor.moveToNext()) {

            long id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));

            if (!firstTime && id <= lastSavedId) break;

            String number = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
            long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
            String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
            int typeCode = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));

            String name = getContactName(number);
            if (name == null) name = number;

            String type =
                    typeCode == Telephony.Sms.MESSAGE_TYPE_INBOX ? "Received" :
                            typeCode == Telephony.Sms.MESSAGE_TYPE_SENT ? "Sent" :
                                    "Unknown";

            String dateStr = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(dateMillis)).toString();

            body = body.replace("\n", " ").replace("|", " "); // clean message body

            String line = id + "|" + dateStr + "|" + type + "|" + name + "|" + number + "|" + body + "\n";

            buffer.append(line);

            if (id > newestId) newestId = id;
        }

        cursor.close();

        if (buffer.length() > 0) {
            writeToFile(buffer.toString());
            saveLastSmsId(newestId);
        }
    }

    private void writeToFile(String text) {
        try (FileOutputStream fos = openFileOutput(SMS_FILE, MODE_APPEND)) {
            fos.write(text.getBytes());
        } catch (Exception ignored) {}
    }

    // --------------------------
    // LOAD SMS TO UI (LIVE)
    // --------------------------
    private void loadSmsFromDevice() {

        smsContainer.removeAllViews();

        Cursor cursor = getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                Telephony.Sms.DATE + " DESC"
        );

        if (cursor == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);

        while (cursor.moveToNext()) {

            String number = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
            long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
            String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
            int typeCode = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));

            String name = getContactName(number);
            if (name == null) name = number;

            String date = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(dateMillis)).toString();

            String type =
                    typeCode == Telephony.Sms.MESSAGE_TYPE_INBOX ? "Received" :
                            typeCode == Telephony.Sms.MESSAGE_TYPE_SENT ? "Sent" :
                                    "Unknown";

            View row = inflater.inflate(R.layout.item_sms_log, smsContainer, false);

            ((TextView) row.findViewById(R.id.sms_name)).setText(name);
            ((TextView) row.findViewById(R.id.sms_number)).setText(number);
            ((TextView) row.findViewById(R.id.sms_body)).setText(body);
            ((TextView) row.findViewById(R.id.sms_time)).setText(type + " • " + date);

            Drawable photo = getContactPhotoDrawable(number);
            if (photo == null) photo = getDrawable(android.R.drawable.sym_def_app_icon);
            ((ImageView) row.findViewById(R.id.sms_photo)).setImageDrawable(photo);

            smsContainer.addView(row);
        }

        cursor.close();
    }

    // --------------------------
    // CONTACT NAME + PHOTO
    // --------------------------
    private String getContactName(String number) {
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor cursor = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                cursor.close();
                return name;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Drawable getContactPhotoDrawable(String number) {
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor cursor = getContentResolver().query(uri,
                    new String[]{
                            ContactsContract.PhoneLookup._ID,
                            ContactsContract.PhoneLookup.PHOTO_URI
                    },
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {

                long id = cursor.getLong(0);
                String photoUri = cursor.getString(1);
                cursor.close();

                if (photoUri != null) {
                    InputStream is = getContentResolver().openInputStream(Uri.parse(photoUri));
                    if (is != null) {
                        Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                        return new BitmapDrawable(getResources(), bmp);
                    }
                }

                Uri personUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
                InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), personUri);
                if (is != null) {
                    Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                    return new BitmapDrawable(getResources(), bmp);
                }
            }

        } catch (Exception ignored) {}
        return null;
    }
}
