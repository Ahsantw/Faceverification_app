package com.example.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;


public class AppLockerAccessibilityService extends AccessibilityService {
    private String currentPackage = null;
    private SharedPreferences prefs;
    private Set<String> lockedApps = new HashSet<>();
    private static final int REQUEST_VERIFY_FACE = 1001;
    private final Set<String> temporarilyUnlockedApps = new HashSet<>();
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        // Use named shared preferences instead of the deprecated method
        prefs = getSharedPreferences("locked_apps_prefs", Context.MODE_PRIVATE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            Context deviceProtectedContext = createDeviceProtectedStorageContext();
//            prefs = deviceProtectedContext.getSharedPreferences("locked_apps_prefs", MODE_PRIVATE);
//        } else {
//            prefs = getSharedPreferences("locked_apps_prefs", MODE_PRIVATE);
//        }

        // Add apps you want to lock here
        lockedApps.add("com.android.chrome");
         lockedApps.add("pk.com.telenor.phoenix");
         lockedApps.add("com.ofss.digx.mobile.android.allied");
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String newPackageName = String.valueOf(event.getPackageName());
            if (newPackageName == null) return;
            Log.d("AppLocker", "Window changed to: " + newPackageName);
            String className = String.valueOf(event.getClassName());
            Log.d("AppLocker", "Class: " + className);

            if (currentPackage != null && !newPackageName.equals(currentPackage)) {
                if (lockedApps.contains(currentPackage)) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("unlocked_" + currentPackage, false);
                    editor.apply();
                }
            }

            currentPackage = newPackageName;

            if (lockedApps.contains(newPackageName) && !newPackageName.equals(getPackageName())) {
                boolean isUnlocked = prefs.getBoolean("unlocked_" + newPackageName, false);
                if (!isUnlocked) {
                    verifyFace(newPackageName);
                }
            }
        }
    }
//    private void verifyFace(String packageName) {
//
//        Intent intent = new Intent(this, FaceVerificationActivity.class);
//        intent.putExtra("packageName", packageName);
//        Log.d("AppLocker_verify", "Window changed to: " + packageName);
////        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
////        face(intent);
//    }
    private void verifyFace(String packageName) {
        try {
//            startActivity(new Intent(this, FaceVerificationActivity.class)
//                    .putExtra("packageName", packageName));
            Intent intent = new Intent(this, FaceVerificationActivity.class);
            intent.putExtra("packageName", packageName);
//           intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  // Don't use CLEAR_TASK
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
//                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
//                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            if (intent!= null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

// Prevent showing in Recents & remove from back stack after exit
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
//                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
//                        | Intent.FLAG_ACTIVITY_NO_HISTORY);
//                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                Log.d("AppLocker_verify", "Window changed to: " + packageName);
                startActivity(intent);
//            }
        } catch (Exception e) {
            Log.e("AppLocker_verify", "Failed to start verification: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Could not start face verification", Toast.LENGTH_SHORT).show();
        }
    }
//
//    @Override
//    protected void onServiceConnected() {
//        super.onServiceConnected();
//        // Register receiver with proper flags
//        IntentFilter filter = new IntentFilter("FACE_VERIFICATION_RESULT");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            registerReceiver(verificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
//        } else {
//            registerReceiver(verificationReceiver, filter);
//        }
//    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        try {
//            unregisterReceiver(verificationReceiver);
//        } catch (IllegalArgumentException e) {
//            // Receiver was not registered
//        }
//    }
//    private void verifyFace(String packageName) {
//        Intent intent = new Intent(this, FaceVerificationActivity.class);
//        intent.putExtra("packageName", packageName);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivityForResult(intent, REQUEST_VERIFY_FACE);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_VERIFY_FACE && resultCode == RESULT_OK) {
//            String verifiedPackage = data.getStringExtra("verified_package");
//            // Temporarily unlock only this package
//            prefs.edit().putBoolean("unlocked_" + verifiedPackage, true).apply();
//
//            // Launch the app
//            try {
//                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(verifiedPackage);
//                if (launchIntent != null) {
//                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    startActivity(launchIntent);
//                }
//            } catch (Exception e) {
//                Toast.makeText(this, "Error opening app", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    @Override
    public void onInterrupt() {
        // Required but unused
    }

//    @Override
//    public void onTaskRemoved(Intent rootIntent) {
//        Intent restartServiceIntent = new Intent(getApplicationContext(), AppLockerAccessibilityService.class);
//        restartServiceIntent.setPackage(getPackageName());
//        startService(restartServiceIntent);
//        super.onTaskRemoved(rootIntent);
//    }
}
