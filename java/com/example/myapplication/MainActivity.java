//package com.example.myapplication;
//
//import android.Manifest;
//import android.accessibilityservice.AccessibilityService;
//import android.accessibilityservice.AccessibilityServiceInfo;
//import android.app.usage.UsageStats;
//import android.app.usage.UsageStatsManager;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.provider.Settings;
//import android.view.accessibility.AccessibilityManager;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class MainActivity extends AppCompatActivity {
//    private static final int REQUEST_CODE_PERMISSIONS = 100;
//    private static final int REQUEST_ACCESSIBILITY = 101;
//    private static final int REQUEST_USAGE_STATS = 102;
//
//    // Permissions needed for the app
//    private static final String[] REQUIRED_PERMISSIONS = {
//            Manifest.permission.CAMERA,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//    };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // Check and request runtime permissions first
//        if (allPermissionsGranted()) {
//            initializeApp();
//        } else {
//            requestPermissions();
//        }
//    }
//
//    private void initializeApp() {
//        // Check additional special permissions after runtime permissions are granted
//        checkSpecialPermissions();
//
//        Button registerFaceButton = findViewById(R.id.registerFaceButton);
//        registerFaceButton.setOnClickListener(v -> {
//            if (isAccessibilityEnabled(this) && hasUsageStatsPermission()) {
////                startActivity(new Intent(this, FaceVerificationActivity.class)
////                        .putExtra("mode", "register"));
//                startActivity(new Intent(this, FaceVerificationActivity.class)
//                        .putExtra("mode", "register"));
//            } else {
//                Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void checkSpecialPermissions() {
//        if (!isAccessibilityServiceRunning(this, AppLockerAccessibilityService.class)) {
//            showAccessibilityDialog();
//        }
////        if (!isAccessibilityEnabled(this)) {
////            showAccessibilityDialog();
////    }
//         else if (!hasUsageStatsPermission()) {
//            showUsageStatsDialog();
//        }
//    }
//
//    private boolean allPermissionsGranted() {
//        for (String permission : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(this, permission)
//                    != PackageManager.PERMISSION_GRANTED) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    private void requestPermissions() {
//        // Only request permissions that haven't been granted yet
//        List<String> permissionsToRequest = new ArrayList<>();
//        for (String permission : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(this, permission)
//                    != PackageManager.PERMISSION_GRANTED) {
//                permissionsToRequest.add(permission);
//            }
//        }
//
//        if (!permissionsToRequest.isEmpty()) {
//            ActivityCompat.requestPermissions(
//                    this,
//                    permissionsToRequest.toArray(new String[0]),
//                    REQUEST_CODE_PERMISSIONS
//            );
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//            if (allPermissionsGranted()) {
//                initializeApp();
//            } else {
//                // Explain why permissions are needed
//                new AlertDialog.Builder(this)
//                        .setTitle("Permissions Required")
//                        .setMessage("Camera and storage permissions are required for face recognition")
//                        .setPositiveButton("Retry", (dialog, which) -> requestPermissions())
//                        .setNegativeButton("Exit", (dialog, which) -> finish())
//                        .show();
//            }
//        }
//    }
//
//    private boolean isAccessibilityEnabled(Context context) {
////        String service = getPackageName() + "/" + AppLockerAccessibilityService.class.getCanonicalName();
//        String service = getPackageName() + "/" + AppLockerAccessibilityService.class.getName();
//        try {
//            int enabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
//            if (enabled == 1) {
//                String services = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//                return services != null && services.contains(service);
//            }
//        } catch (Exception e) {
//            Toast.makeText(this, "Error checking accessibility", Toast.LENGTH_SHORT).show();
//        }
//        return false;
//    }
//
//    private boolean hasUsageStatsPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
//            long currentTime = System.currentTimeMillis();
//            List<UsageStats> stats = usageStatsManager.queryUsageStats(
//                    UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 60, currentTime);
//            return stats != null && !stats.isEmpty();
//        }
//        return true;
//    }
//
//    private void showAccessibilityDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Enable Accessibility")
//                .setMessage("Face Recognition App Locker needs Accessibility permission to lock apps.")
//                .setPositiveButton("Open Settings", (d, w) ->
//                        startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), REQUEST_ACCESSIBILITY))
//                .setCancelable(false)
//                .show();
//    }
//
//    private void showUsageStatsDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Enable Usage Access")
//                .setMessage("Allow Face Recognition App Locker to monitor app usage.")
//                .setPositiveButton("Open Settings", (d, w) ->
//                        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), REQUEST_USAGE_STATS))
//                .setCancelable(false)
//                .show();
//    }
//
//    public static boolean isAccessibilityServiceRunning(Context context, Class<? extends AccessibilityService> serviceClass) {
//        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
//        if (am != null) {
//            List<AccessibilityServiceInfo> runningServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
//            for (AccessibilityServiceInfo service : runningServices) {
//                if (service.getId().contains(context.getPackageName()) &&
//                        service.getId().contains(serviceClass.getSimpleName())) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        checkSpecialPermissions();
//    }
//}


package com.example.myapplication;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {
//    @Override
//    protected void onResume() {
//        super.onResume();
//        checkOverlayPermission(); // Check if "Draw over other apps" is enabled
//    }
//
//    private void checkOverlayPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                showOverlayPermissionDialog();
//            }
//        }
//    }
//
//    private void showOverlayPermissionDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Permission Needed")
//                .setMessage("Allow this app to display over other apps? Required for floating windows.")
//                .setPositiveButton("Open Settings", (dialog, which) -> {
//                    Intent intent = new Intent(
//                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                            Uri.parse("package:" + getPackageName())
//                    );
//                    startActivity(intent);
//                })
//                .setNegativeButton("Cancel", null)
//                .setCancelable(false)
//                .show();
//    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        checkOverlayPermission(); // Check overlay permission
//    }
//
//    private void checkOverlayPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                requestOverlayPermission();
//            }
//        }
//    }
//
//    private void requestOverlayPermission() {
//        new AlertDialog.Builder(this)
//                .setTitle("Display Over Other Apps")
//                .setMessage("This permission is required for the app to show floating windows.")
//                .setPositiveButton("Enable", (dialog, which) -> {
//                    Intent intent = new Intent(
//                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                            Uri.parse("package:" + getPackageName())
//                    );
//                    startActivity(intent);
//                })
//                .setNegativeButton("Cancel", null)
//                .show();
//    }

    private static final int REQUEST_CODE_PERMISSIONS = 100;

    private ActivityResultLauncher<Intent> accessibilitySettingsLauncher;
    private ActivityResultLauncher<Intent> usageStatsSettingsLauncher;

    // Android 13+ permission handling
//    private static final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
//            ? new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS}
//            : new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            return new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, AppLockerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Register activity result launchers
        accessibilitySettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkSpecialPermissions());

        usageStatsSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkSpecialPermissions());
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//        new PermissionDialogHelper(this).showPermissionFlowIfNeeded();
//        XiaomiPermissionHelper.checkBackgroundPopupPermission(this);
//
//        XiaomiPermissionHelper.openRestrictedSettings(this);
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            new PermissionDialogHelper(this).showPermissionFlowIfNeeded();
            XiaomiPermissionHelper.checkBackgroundPopupPermission(this);
            XiaomiPermissionHelper.openRestrictedSettings(this);
        }




        // Check runtime permissions
        if (allPermissionsGranted()) {
            initializeApp();
        } else {
            requestPermissions();
        }
    }

    private void initializeApp() {
        checkSpecialPermissions();
//        checkXiaomiAutoStart();

        Button registerFaceButton = findViewById(R.id.registerFaceButton);
        registerFaceButton.setOnClickListener(v -> {
            if (isAccessibilityEnabled(this) && hasUsageStatsPermission()) {
                startActivity(new Intent(this, FaceVerificationActivity.class)
                        .putExtra("mode", "register"));
            } else {
                Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    public void checkXiaomiAutoStart() {
//        if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {
//            try {
//                // Open MIUI's Autostart settings directly
//                Intent intent = new Intent();
//                intent.setComponent(new ComponentName(
//                        "com.miui.securitycenter",
//                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
//                ));
//                startActivity(intent);
//            } catch (Exception e) {
//                // Fallback for newer MIUI versions
//                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                intent.setData(Uri.parse("package:" + getPackageName()));
//                startActivity(intent);
//            }
//        }
//    }

    private void checkSpecialPermissions() {
        if (!isAccessibilityServiceRunning(this, AppLockerAccessibilityService.class)) {
            showAccessibilityDialog();
        } else if (!hasUsageStatsPermission()) {
            showUsageStatsDialog();
        }
    }

//    private boolean allPermissionsGranted() {
//        for (String permission : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(this, permission)
//                    != PackageManager.PERMISSION_GRANTED) {
//                return false;
//            }
//        }
//        return true;
//    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

//    private void requestPermissions() {
//        List<String> permissionsToRequest = new ArrayList<>();
//        for (String permission : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(this, permission)
//                    != PackageManager.PERMISSION_GRANTED) {
//                permissionsToRequest.add(permission);
//            }
//        }
//
//        if (!permissionsToRequest.isEmpty()) {
//            ActivityCompat.requestPermissions(
//                    this,
//                    permissionsToRequest.toArray(new String[0]),
//                    REQUEST_CODE_PERMISSIONS
//            );
//        }
//    }

    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeApp();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                        .setMessage("Camera and storage permissions are required for face recognition.")
                        .setPositiveButton("Retry", (dialog, which) -> requestPermissions())
                        .setNegativeButton("Exit", (dialog, which) -> finish())
                        .show();
            }
        }
    }

    private boolean isAccessibilityEnabled(Context context) {
        String service = getPackageName() + "/" + AppLockerAccessibilityService.class.getName();
        try {
            int enabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 1) {
                String services = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                return services != null && services.contains(service);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error checking accessibility", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private boolean hasUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long currentTime = System.currentTimeMillis();
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 60, currentTime);
            return stats != null && !stats.isEmpty();
        }
        return true;
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Accessibility")
                .setMessage("Face Recognition App Locker needs Accessibility permission to lock apps.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    accessibilitySettingsLauncher.launch(intent);
                })
                .setCancelable(false)
                .show();
    }

    private void showUsageStatsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Usage Access")
                .setMessage("Allow Face Recognition App Locker to monitor app usage.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    usageStatsSettingsLauncher.launch(intent);
                })
                .setCancelable(false)
                .show();
    }

    public static boolean isAccessibilityServiceRunning(Context context, Class<? extends AccessibilityService> serviceClass) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            List<AccessibilityServiceInfo> runningServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (AccessibilityServiceInfo service : runningServices) {
                if (service.getId().contains(context.getPackageName()) &&
                        service.getId().contains(serviceClass.getSimpleName())) {
                    return true;
                }
            }
        }
        return false;
    }
}




