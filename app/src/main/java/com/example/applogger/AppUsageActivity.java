package com.example.applogger;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppUsageActivity extends AppCompatActivity {

    public static final String ACTION_LOG_ADDED = "com.example.applogger.LOG_ADDED";
    public static final String EXTRA_APP_NAME = "appName";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_PKG = "pkg";

    private RecyclerView recyclerView;
    private RelativeLayout loadingOverlay;
    private LogAdapter adapter;
    private ArrayList<LogModel> logList;

    private TextView selectedDateTv;
    private ImageView calendarBtn;
    private Button uploadBtn;

    private static final String LOG_FILE = "AppUsageLog.txt";
    private String selectedDate;

    private Drawable defaultIcon;

    private static final String DROPBOX_TOKEN = "sl.u.AGLhpxeYLuh6q71tpiyPI5Ngo6iCMAqz2-K_nVBFPjojMhVoV13QcVO1XvmgQw0cQOPBgC447mbnei1KbQ6TfAovR7D9Hz36wHWGeqL_oQmMe2f3kiqbQNXj1GsuutaMhA5npCADQSq7qFz3CI3LXnZTLaOYAf8DIK6DANLRafcGlYNj_TX8OqEf8tORd15OJOa3fEqc3iMmvbPWXaiemmgLUfBP_ffYMEwW05oY-qWEX88tDUt6pzOuXgcROYmx7cgXZnFvc8NhPCjLf_9u3JgT2Y8jO30GG1TkamG7gNSezwMsRV7-ift25t4RBbIpmPqo3FxyQZ2HpjcKTnF-niOlid7mms8g3vaByPtqpSipCBzRhJb784mu2Zfb0NbBA4sr2MV2_LIzJlCBMVcqVU1BfYsGcAWDcTCADlam0zlqjyJvcGtC04d5VL6KNcT4UQQbV5HwLUewKCbMEl6QbtK81YDppdXmb8dE_U2uUXvGsHtSB9BCZcBN-4UHnwBw3JrfB-hynQiaDmi-_1cVAVoQyj7FWb_d8MdNVAFo-PNB96NA61rI1DLkPHb23vW9OewaO0w5_WygYXueaXD2pVexNj8CUBuXH9s9M0E6ktPTLn7Q7XZn0-_ZGbnDEWHSBwWAV__tqt2u5GFHCYr8vqe8MobegR2qoI8rV9CXPHa2__QvsgVY9ixPXHFTO7wdE5JpetftVgV69CAOBwOBcVtgIIbrQZY7syrRmvVT3wi3N3s86z82iDgMsPh4VFFxJZ2WhwvSPcHDVMSEAVL741slFLWGdK1QL_SzLrH_r51N_m6GJvuAq2YiznLdCpeYcmEdh6E12Wc6TcpmVmcZtwLiAGTDGn6Bo--5nkBynWuHrtcVg3gUo6D7rIMizKKwZ7DWahD0iQ3aBGe7BZ6ffCBd5YLNwrzmFy3Vk7pn_8sNjWb7TVGeey0YV_x5xWdlLrcgqz01lloN1tYzhoU456cq8wT2raQHHx6cfkqYh_1PYfHaX_dbGSQO3r_fnrwC57qXvUYUTsNhEh5TR3p0c1GRph7LIr6eZl2MAVdpaU1gnCcKpBxosk61mDftcflBmhCIHn6c9SwM0rYxecmrzjxp1xu0dgPA8Nw9dMD7ZJUtGa1URaoVhQz9ljY0DtROtuE85Qx1rVq85UIUclpBJz6_WAXI7gwVIGryPp_KDn1NeDOS6s-FzWFmSMgZ8L35FqHqK33GrwnviHAq_ArkFi5w6o5XqZzQJdKUsg6-BYFoZrCvf-HtZ7ajG3mC1ib4R-VnI9s1WeaR-23EOUUaUBz3j4OfY9vBupuhGXIfIufe0ehP1wWyAFatVNy6bnkgLynKaC8i7IdtyrMZLUYdckBjhxfxTcwfamjSWg7i5h8ZtbrB71VDZSoS1uTjUGSdebWQzuIiuSg91kMrPb7_xR6NpHptR5sqha1P8MRgg1iiZw";

    // ✅ REAL-TIME RECEIVER
    private final BroadcastReceiver liveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!ACTION_LOG_ADDED.equals(intent.getAction())) return;

            String rawName = intent.getStringExtra(EXTRA_APP_NAME);
            String status = intent.getStringExtra(EXTRA_STATUS);
            String timestamp = intent.getStringExtra(EXTRA_TIMESTAMP);

            if (rawName == null || timestamp == null) return;

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            if (!today.equals(selectedDate)) return;

            String time = timestamp.substring(timestamp.length() - 8);

            // ✅ CONTACT NAME RESOLUTION
            String finalName = resolveContactName(rawName);

            LogModel model = new LogModel(defaultIcon, finalName, status, time);

            runOnUiThread(() -> {
                logList.add(model);
                int index = logList.size() - 1;
                adapter.notifyItemInserted(index);
                recyclerView.smoothScrollToPosition(index);
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_usage);

        recyclerView   = findViewById(R.id.logRecycler);
        selectedDateTv = findViewById(R.id.selectedDateTv);
        calendarBtn    = findViewById(R.id.calendarBtn);
        uploadBtn      = findViewById(R.id.uploadBtn);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        defaultIcon = getDrawable(android.R.drawable.sym_def_app_icon);

        logList = new ArrayList<>();
        adapter = new LogAdapter(this, logList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        selectedDateTv.setText(selectedDate + " (Today)");

        calendarBtn.setOnClickListener(v -> openDatePicker());
        uploadBtn.setOnClickListener(v -> uploadToDropbox());

        loadLogsAsync(selectedDate);

        IntentFilter filter = new IntentFilter(ACTION_LOG_ADDED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(liveReceiver, filter, RECEIVER_NOT_EXPORTED);
        else
            registerReceiver(liveReceiver, filter);
    }

    private void fadeOverlay(boolean show) {
        if (show) {
            loadingOverlay.setVisibility(View.VISIBLE);
            AlphaAnimation fade = new AlphaAnimation(0, 1);
            fade.setDuration(250);
            loadingOverlay.startAnimation(fade);
        } else {
            AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(250);
            loadingOverlay.startAnimation(fadeOut);
            loadingOverlay.postDelayed(() -> loadingOverlay.setVisibility(View.GONE), 250);
        }
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> {
            m++;
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m, d);

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            selectedDateTv.setText(selectedDate.equals(today) ? selectedDate + " (Today)" : selectedDate);

            loadLogsAsync(selectedDate);

        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dp.show();
    }

    private void loadLogsAsync(String date) {

        fadeOverlay(true);

        new Thread(() -> {

            ArrayList<LogModel> temp = new ArrayList<>();

            try (FileInputStream fis = openFileInput(LOG_FILE);
                 BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

                String line;
                while ((line = br.readLine()) != null) {

                    if (!line.startsWith("[")) continue;

                    int bracket = line.indexOf("]");
                    String ts = line.substring(1, bracket);

                    if (!ts.startsWith(date)) continue;

                    String body = line.substring(bracket + 1).trim();
                    String[] part = body.split(" ", 2);

                    if (part.length < 2) continue;

                    String rawName = part[0];
                    String status  = part[1];
                    String time = ts.substring(ts.length() - 8);

                    // ✅ CONTACT NAME RESOLVE ON LOAD
                    String finalName = resolveContactName(rawName);

                    temp.add(new LogModel(defaultIcon, finalName, status, time));
                }

            } catch (Exception ignored) {}

            runOnUiThread(() -> {
                logList.clear();
                logList.addAll(temp);
                adapter.notifyDataSetChanged();
                if (!logList.isEmpty())
                    recyclerView.scrollToPosition(logList.size() - 1);
                fadeOverlay(false);
            });

        }).start();
    }

    private void uploadToDropbox() {

        Toast.makeText(this, "Uploading to Dropbox...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                FileInputStream fis = openFileInput(LOG_FILE);
                byte[] data = new byte[fis.available()];
                fis.read(data);
                fis.close();

                URL url = new URL("https://content.dropboxapi.com/2/files/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + DROPBOX_TOKEN);
                conn.setRequestProperty(
                        "Dropbox-API-Arg",
                        "{\"path\":\"/AppLogger/AppUsageLog.txt\",\"mode\":\"overwrite\"}"
                );
                conn.setRequestProperty("Content-Type", "application/octet-stream");

                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(this, "✅ Uploaded to Dropbox!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "❌ Upload failed: " + responseCode, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "❌ Dropbox upload error", Toast.LENGTH_LONG).show()
                );
            }

        }).start();
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(liveReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    // ================================
    // ✅ ✅ CONTACT NAME RESOLVER
    // ================================
    private String resolveContactName(String input) {
        if (!input.matches(".*\\d{5,}.*")) return input;

        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(input)
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

        return input;
    }

    // ================================
    // ✅ INNER MODEL CLASS
    // ================================
    private static class LogModel {

        private final Drawable icon;
        private final String appName;
        private final String status;
        private final String time;

        public LogModel(Drawable icon, String appName, String status, String time) {
            this.icon = icon;
            this.appName = appName;
            this.status = status;
            this.time = time;
        }

        public Drawable getIcon() { return icon; }
        public String getAppName() { return appName; }
        public String getStatus() { return status; }
        public String getTime() { return time; }
    }

    // ================================
    // ✅ INNER ADAPTER CLASS (COLOR FIX SAFE)
    // ================================
    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

        private final Context context;
        private final ArrayList<LogModel> items;
        private int lastPosition = -1;

        public LogAdapter(Context context, ArrayList<LogModel> items) {
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {

            LogModel item = items.get(position);

            holder.appIcon.setImageDrawable(item.getIcon());
            holder.appName.setText(item.getAppName());
            holder.appStatus.setText(item.getStatus());
            holder.appTime.setText(item.getTime());

            String status = item.getStatus().trim().toLowerCase();

            if (status.contains("opened")) {
                holder.appStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_green_dark)
                );
            } else if (status.contains("closed")) {
                holder.appStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_red_dark)
                );
            } else {
                holder.appStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.darker_gray)
                );
            }

            if (position > lastPosition) {
                Animation anim = AnimationUtils.loadAnimation(context, R.anim.item_anim);
                holder.itemView.startAnimation(anim);
                lastPosition = position;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class LogViewHolder extends RecyclerView.ViewHolder {

            ImageView appIcon;
            TextView appName, appStatus, appTime;

            public LogViewHolder(@NonNull View itemView) {
                super(itemView);
                appIcon   = itemView.findViewById(R.id.itemIcon);
                appName   = itemView.findViewById(R.id.itemName);
                appStatus = itemView.findViewById(R.id.itemStatus);
                appTime   = itemView.findViewById(R.id.itemTime);
            }
        }
    }
}
