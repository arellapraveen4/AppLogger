package com.example.applogger;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CallLogsActivity extends AppCompatActivity {

    private static final String LOG_FILE = "CallBackup.txt";

    // ✅ ✅ ✅ PASTE YOUR REAL DROPBOX TOKEN HERE
    private static final String DROPBOX_TOKEN = "sl.u.AGIzJ48TB7U97L5K6badZFUIDeoIxR_ptZNpTaxLKuMgOq9V4qyuSeWdISDhknFmvXLlhmJapWPfdSheGTRtU7wE8r6PNS-wDxn7vCT1Yj9r8dfQq8ImSjuNxe8IOKjpsvQD7H0FklIk8GXItgvdOhICnbpsTnuNlF_yfZUHfSgXbttLvCD4JgX06tsMdZVdyFMKf6wLn7Upw639pdvxQGgOBEPV7ej1n3c49MjS27KABhATEVFAy3CMYn5Q4P7DJq6CdfyNm16Qy3lgRXg_rbT8vnsD2wuOuOqd50ZWNYA2l1viXAP9-BiS1VAWXgs1bBLaG9itJ6L445LIol9j8pHWtb4sJP5A8mfXIxezSDYWcHJZq3eVMKTuahPIwGfcK9U2ZDqsUJAVinjjX18K5Adp6MWt-8He3vc7qraZv2RMl7Rm5dBaKAHz5sfRW5E1ahhD-Kyz0x1rqMnHmn8GR06wrt2EdkMDKto8_O2zExG6lFTMiNF44zsgyXfzYUICYnx_nske5cMiTYTKibRDlxk4U84mDWtVSVkfvLcfjHFHM2JDyKx8mlpf2RT_Oz4p3M3e6o2zJjeMMYkitp04Bpgq9z7TZPTKj1hLWC62Hdt9ax7DkXrVLcSXMnx4Ix-POQ26i90x2GjdNp41PnRga8CSKk8Vj09Dc_CZ4veEd7hmTwlNSnxLLsfc6QmWVulcCrQz3BlADFtw-8vDLxI0diHiuiApCGJF6nWHWxrFSWCRb_FQsc4T8wGbhTDjxvlR3Pt470jiZiTwnCdOItEdBII_nMb_H9f-phsrDKRIRs2ixUtRpaAWWCAIOKJbrmJo0D6sqYmkwTLmyjSfgANONWTvpq3FV-9VYpJnV6Aeu0bcNASSgyXxwiu2_LtE9MVxiuFP6tDiGw2P6Dic0WWmXg-AglDwBM92yJ1_p5GIwuUMublT46faKwWf-JJZM4LdA8Q_cmkpXOHE90y9xjmjcYjZ6zXaW-SC7iQP5c-cR8hzXxfUbHUSZhrYEZOXzJwq8NX_9tiwkR_j5LVOxpjgEvjbZvIYkLvJd6xKCo4jQJFkuRsul8XG4N5S6GGFjkHMy9XMbIUI3sELjtMZ-g64dmgUQ1qETiW-Fy7LJjGAQobPAP1ftt9tfYGkk9nYzlfXvmHwFDk5fZ1CoG-O5z85BvsXEnCGQzFPclalFx3hXiczkEfDJ9UthXEE7mUj_PaVCpcljVl13dF1Y4RU8cIXe2SnN84ZC4IkPYeAa0GS44i8WIvz7VLzQ1YqDxUGE68dtBz2zu62Kg1zmpun6f9pzCiT9xBNxB8gdJiOJKJ8bcUdI9E8Y0PFs-SaN5bh9TUL-nLgoIAaZ1blSzVgqKj7hBEaPrxXlG9L2LmhqV-TU0fb9pYzkaNbbU3bI8t0pENb7IXp1cp8QBmCZvVt4MvnyFXG4NV_60-S_kBvobQojV1zGg";

    private LinearLayout callLogContainer;
    private LayoutInflater inflater;

    private final Executor bg = Executors.newSingleThreadExecutor();

    private final List<View> allRows = new ArrayList<>();

    private long filterStart = -1;
    private long filterEnd = -1;

    TextView tvFromDate, tvToDate, tvReset, uploadCallBtn;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = true;
                for (Boolean ok : result.values()) if (!ok) granted = false;

                if (granted) startLoadLogsOnly();
                else Toast.makeText(this, "Call + Contacts permission required", Toast.LENGTH_LONG).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_logs);

        callLogContainer = findViewById(R.id.callLogContainer);
        inflater = LayoutInflater.from(this);

        uploadCallBtn = findViewById(R.id.uploadCallBtn);
        uploadCallBtn.setOnClickListener(v -> uploadCallBackupToDropbox());

        initDateFilter();

        if (hasPermissions()) startLoadLogsOnly();
        else permissionLauncher.launch(new String[]{
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS
        });
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void initDateFilter() {
        tvFromDate = findViewById(R.id.filterFromDate);
        tvToDate = findViewById(R.id.filterToDate);
        tvReset = findViewById(R.id.filterReset);

        tvFromDate.setOnClickListener(v -> pickDate(true));
        tvToDate.setOnClickListener(v -> pickDate(false));

        tvReset.setOnClickListener(v -> {
            filterStart = -1;
            filterEnd = -1;
            tvFromDate.setText("From");
            tvToDate.setText("To");
            applyCallLogFilter();
        });
    }

    private void pickDate(boolean isStart) {
        Calendar c = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(y, m, d, 0, 0, 0);
                    long millis = sel.getTimeInMillis();

                    if (isStart) {
                        filterStart = millis;
                        tvFromDate.setText(DateFormat.format("dd MMM yyyy", millis));
                    } else {
                        filterEnd = millis + 86399000;
                        tvToDate.setText(DateFormat.format("dd MMM yyyy", millis));
                    }

                    applyCallLogFilter();
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        );

        dp.show();
    }

    private void startLoadLogsOnly() {
        bg.execute(this::loadLiveLogsFromDevice);
    }

    private void loadLiveLogsFromDevice() {

        runOnUiThread(() -> {
            callLogContainer.removeAllViews();
            synchronized (allRows) {
                allRows.clear();
            }
        });

        String[] proj = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
        };

        Cursor cursor = null;

        try {
            cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    proj,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
            );

            if (cursor == null) return;

            int idxNum = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER);
            int idxDate = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE);
            int idxDur = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION);
            int idxType = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE);

            while (cursor.moveToNext()) {

                String number = cursor.getString(idxNum);
                long dateMillis = cursor.getLong(idxDate);
                String duration = cursor.getString(idxDur);
                int type = cursor.getInt(idxType);

                String name = getContactName(number);
                if (name == null) name = number;

                String when = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(dateMillis)).toString();
                String callType = callTypeString(type);

                Drawable photo = getContactPhotoDrawable(number);
                if (photo == null)
                    photo = getDrawable(android.R.drawable.sym_def_app_icon);

                View row = createRow(
                        name,
                        number,
                        callType + " • " + when,
                        "Duration: " + duration + " sec",
                        photo,
                        dateMillis
                );

                synchronized (allRows) {
                    allRows.add(row);
                }
            }

        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        runOnUiThread(this::applyCallLogFilter);
    }

    private void applyCallLogFilter() {

        callLogContainer.removeAllViews();

        List<View> safeCopy;
        synchronized (allRows) {
            safeCopy = new ArrayList<>(allRows);
        }

        for (View row : safeCopy) {
            long t = (long) row.getTag();

            if (filterStart != -1 && t < filterStart) continue;
            if (filterEnd != -1 && t > filterEnd) continue;

            callLogContainer.addView(row);
        }
    }

    private View createRow(String name, String number, String when, String duration,
                           Drawable photo, long timestamp) {

        View row = inflater.inflate(R.layout.item_call_log, callLogContainer, false);

        ((TextView) row.findViewById(R.id.call_contact_name)).setText(name);
        ((TextView) row.findViewById(R.id.call_number)).setText(number);
        ((TextView) row.findViewById(R.id.call_type_time)).setText(when);
        ((TextView) row.findViewById(R.id.call_duration)).setText(duration);

        ((ImageView) row.findViewById(R.id.call_contact_photo)).setImageDrawable(photo);

        row.setTag(timestamp);
        return row;
    }

    /* ===============================
       ✅ ✅ ✅ DROPBOX UPLOAD (FINAL)
       =============================== */
    private void uploadCallBackupToDropbox() {

        Toast.makeText(this, "Uploading to Dropbox...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                File file = new File(getFilesDir(), LOG_FILE);

                if (!file.exists()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "CallBackup.txt not found", Toast.LENGTH_LONG).show());
                    return;
                }

                byte[] fileBytes;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    fileBytes = Files.readAllBytes(file.toPath());
                else {
                    FileInputStream fis = new FileInputStream(file);
                    fileBytes = new byte[(int) file.length()];
                    fis.read(fileBytes);
                    fis.close();
                }

                URL url = new URL("https://content.dropboxapi.com/2/files/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");

                conn.setRequestProperty("Authorization", "Bearer " + DROPBOX_TOKEN);
                conn.setRequestProperty("Content-Type", "application/octet-stream");
                conn.setRequestProperty(
                        "Dropbox-API-Arg",
                        "{\"path\":\"/AppLogger/CallBackup.txt\",\"mode\":\"overwrite\"}"
                );

                OutputStream os = conn.getOutputStream();
                os.write(fileBytes);
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200)
                        Toast.makeText(this, "✅ Uploaded to Dropbox!", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(this, "❌ Upload failed: " + responseCode, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "❌ Upload Error", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String getContactName(String number) {
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number)
            );

            Cursor c = getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null
            );

            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                c.close();
                return name;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Drawable getContactPhotoDrawable(String number) {
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number)
            );

            Cursor c = getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.PHOTO_URI},
                    null, null, null
            );

            if (c != null && c.moveToFirst()) {

                long id = c.getLong(0);
                String pUri = c.getString(1);
                c.close();

                if (pUri != null) {
                    InputStream is = getContentResolver().openInputStream(Uri.parse(pUri));
                    if (is != null) {
                        Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                        is.close();
                        return new BitmapDrawable(getResources(), bmp);
                    }
                }

                Uri contactUri = ContentUris.withAppendedId(
                        ContactsContract.Contacts.CONTENT_URI, id);

                InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(
                        getContentResolver(), contactUri);

                if (is != null) {
                    Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                    is.close();
                    return new BitmapDrawable(getResources(), bmp);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private String callTypeString(int t) {
        switch (t) {
            case CallLog.Calls.INCOMING_TYPE: return "Incoming";
            case CallLog.Calls.OUTGOING_TYPE: return "Outgoing";
            case CallLog.Calls.MISSED_TYPE: return "Missed";
            case CallLog.Calls.REJECTED_TYPE: return "Rejected";
            default: return "Unknown";
        }
    }
}
