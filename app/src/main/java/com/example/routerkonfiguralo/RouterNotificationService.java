package com.example.routerkonfiguralo;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentChange;

import java.util.HashMap;
import java.util.Map;

public class RouterNotificationService extends Service {
    private static final String TAG = "RouterNotificationService";
    private static final String CHANNEL_ID = "router_status_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_CODE = 100;
    private static final long CHECK_INTERVAL = 15 * 60 * 1000; // 15 minutes

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration routerListener;
    private Map<String, DocumentSnapshot> previousStates;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            previousStates = new HashMap<>();
            createNotificationChannel();
            setupAlarm();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            stopSelf();
        }
    }

    private void setupAlarm() {
        try {
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }

            Intent intent = new Intent(this, RouterNotificationService.class);
            intent.setAction("CHECK_ROUTER_STATUS");
            alarmIntent = PendingIntent.getService(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Set the alarm to start after CHECK_INTERVAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + CHECK_INTERVAL,
                        alarmIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + CHECK_INTERVAL,
                        alarmIntent
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up alarm: " + e.getMessage(), e);
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent != null && "CHECK_ROUTER_STATUS".equals(intent.getAction())) {
                Log.d(TAG, "Alarm triggered - checking router status");
                checkRouterStatus();
                // Reschedule the alarm
                setupAlarm();
            } else {
                startForeground(NOTIFICATION_ID, createNotification("Router Status Service", "Monitoring router status..."));
                startRouterMonitoring();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: " + e.getMessage(), e);
            stopSelf();
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Router Status Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications about router status changes");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, RouterListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    private void startRouterMonitoring() {
        try {
            String userId = mAuth.getCurrentUser().getUid();
            if (userId == null) {
                Log.e(TAG, "User ID is null");
                stopSelf();
                return;
            }

            routerListener = db.collection("routers")
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error listening to router changes: " + error.getMessage());
                            return;
                        }

                        if (value != null) {
                            Log.d(TAG, "Received " + value.getDocumentChanges().size() + " document changes");
                            for (DocumentChange change : value.getDocumentChanges()) {
                                String routerId = change.getDocument().getId();
                                DocumentSnapshot currentDoc = change.getDocument();
                                DocumentSnapshot previousDoc = previousStates.get(routerId);

                                Log.d(TAG, "Processing change for router: " + routerId);
                                Log.d(TAG, "Change type: " + change.getType());

                                if (previousDoc != null) {
                                    Log.d(TAG, "Found previous state, checking for changes");
                                    checkAndNotifyChanges(previousDoc, currentDoc);
                                } else {
                                    Log.d(TAG, "No previous state found for router: " + routerId);
                                }

                                previousStates.put(routerId, currentDoc);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error starting router monitoring: " + e.getMessage(), e);
            stopSelf();
        }
    }

    private void checkAndNotifyChanges(DocumentSnapshot previous, DocumentSnapshot current) {
        String routerName = current.getString("name");
        if (routerName == null) {
            Log.d(TAG, "Router name is null, skipping notification");
            return;
        }

        StringBuilder changes = new StringBuilder();
        
        // Check name changes
        String previousName = previous.getString("name");
        if (!routerName.equals(previousName)) {
            Log.d(TAG, "Name change detected: " + previousName + " -> " + routerName);
            changes.append("Name changed from ").append(previousName).append(" to ").append(routerName).append("\n");
        }

        // Check model changes
        String previousModel = previous.getString("model");
        String currentModel = current.getString("model");
        if (currentModel != null && !currentModel.equals(previousModel)) {
            Log.d(TAG, "Model change detected: " + previousModel + " -> " + currentModel);
            changes.append("Model changed from ").append(previousModel).append(" to ").append(currentModel).append("\n");
        }

        // Check firmware changes
        String previousFirmware = previous.getString("firmwareVersion");
        String currentFirmware = current.getString("firmwareVersion");
        if (currentFirmware != null && !currentFirmware.equals(previousFirmware)) {
            Log.d(TAG, "Firmware change detected: " + previousFirmware + " -> " + currentFirmware);
            changes.append("Firmware changed from ").append(previousFirmware).append(" to ").append(currentFirmware).append("\n");
        }

        // Check IP changes
        String previousIp = previous.getString("ipAddress");
        String currentIp = current.getString("ipAddress");
        if (currentIp != null && !currentIp.equals(previousIp)) {
            Log.d(TAG, "IP change detected: " + previousIp + " -> " + currentIp);
            changes.append("IP changed from ").append(previousIp).append(" to ").append(currentIp).append("\n");
        }

        // Check online status changes
        Boolean previousOnline = previous.getBoolean("isOnline");
        Boolean currentOnline = current.getBoolean("isOnline");
        if (currentOnline != null && !currentOnline.equals(previousOnline)) {
            Log.d(TAG, "Online status change detected: " + previousOnline + " -> " + currentOnline);
            changes.append("Status changed to ").append(currentOnline ? "Online" : "Offline").append("\n");
        }

        if (changes.length() > 0) {
            Log.d(TAG, "Changes detected, showing notification");
            showChangesNotification(routerName, changes.toString());
        } else {
            Log.d(TAG, "No changes detected");
        }
    }

    private void showChangesNotification(String routerName, String changes) {
        Log.d(TAG, "Creating notification for router: " + routerName);
        Log.d(TAG, "Changes: " + changes);
        
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        Notification notification = createNotification(
                "Router Changes: " + routerName,
                changes.trim()
        );
        
        int notificationId = routerName.hashCode();
        Log.d(TAG, "Showing notification with ID: " + notificationId);
        notificationManager.notify(notificationId, notification);
    }

    private void checkRouterStatus() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("routers")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String routerId = doc.getId();
                        DocumentSnapshot previousDoc = previousStates.get(routerId);
                        
                        if (previousDoc != null) {
                            checkAndNotifyChanges(previousDoc, doc);
                        }
                        previousStates.put(routerId, doc);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking router status: " + e.getMessage());
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (routerListener != null) {
            routerListener.remove();
        }
        if (alarmManager != null && alarmIntent != null) {
            alarmManager.cancel(alarmIntent);
        }
        previousStates.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 