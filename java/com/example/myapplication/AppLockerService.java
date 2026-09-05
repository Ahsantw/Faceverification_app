package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class AppLockerService extends Service {
    private static final String CHANNEL_ID = "applocker_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForegroundService();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Locker",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Locker Running")
                .setContentText("Protecting your apps")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (SecurityException e) {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

//public class AppLockerService extends Service {
//    private static final String CHANNEL_ID = "applocker_channel";
//    private static final int NOTIFICATION_ID = 1;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        createNotificationChannel();
//        startForegroundService();
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return START_STICKY;
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "App Locker",
//                    NotificationManager.IMPORTANCE_LOW
//            );
//            NotificationManager manager = getSystemService(NotificationManager.class);
//            manager.createNotificationChannel(channel);
//        }
//    }
//
//    private void startForegroundService() {
//        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("App Locker Running")
//                .setContentText("Protecting your apps")
//                .setSmallIcon(android.R.drawable.ic_lock_lock)
//                .setPriority(NotificationCompat.PRIORITY_LOW)
//                .build();
//
//        startForeground(NOTIFICATION_ID, notification);
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//}

