package com.example.myapplication;

//import static XiaomiPermissionHelper.openAppSettings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

//public class XiaomiPermissionHelper {
//
//    private static final String MIUI_AUTOSTART_ACTIVITY = "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity";
//    private static final String MIUI_BATTERY_ACTIVITY = "com.miui.powerkeeper/com.miui.powerkeeper.ui.HiddenAppsConfigActivity";
//
//    public static boolean isXiaomiDevice() {
//        return Build.MANUFACTURER.equalsIgnoreCase("xiaomi");
//    }
//    public static void checkAutoStartPermission(Activity activity) {
//        if (!isXiaomiDevice()) return;
//
//        try {
//            Intent intent = new Intent();
//            intent.setComponent(new ComponentName(
//                    "com.miui.securitycenter",
//                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
//            ));
//            safeStartActivity(activity, intent);
//        } catch (Exception e) {
//            openAppSettings(activity);
//        }
//    }
//
//    public static void checkBatteryOptimization(Activity activity) {
//        if (!isXiaomiDevice()) return;
//
//        try {
//            Intent intent = new Intent();
//            intent.setComponent(new ComponentName(
//                    "com.miui.powerkeeper",
//                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
//            ));
//            intent.putExtra("package_name", activity.getPackageName());
//            intent.putExtra("package_label", activity.getString(R.string.app_name));
//            safeStartActivity(activity, intent);
//        } catch (Exception e) {
//            openAppSettings(activity);
//        }
//    }
////    public static void checkAutoStartPermission(Activity activity) {
////        if (!isXiaomiDevice()) return;
////
////        try {
////            Intent intent = new Intent();
////            intent.setComponent(new ComponentName(
////                    "com.miui.securitycenter",
////                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
////            ));
////            activity.startActivity(intent);
////        } catch (Exception e) {
////            openAppSettings(activity);
////        }
////    }
//
////    public static void checkBatteryOptimization(Activity activity) {
////        if (!isXiaomiDevice()) return;
////
////        try {
////            Intent intent = new Intent();
////            intent.setComponent(new ComponentName(
////                    "com.miui.powerkeeper",
////                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
////            ));
////            intent.putExtra("package_name", activity.getPackageName());
////            intent.putExtra("package_label", activity.getString(R.string.app_name));
////            activity.startActivity(intent);
////        } catch (Exception e) {
////            openAppSettings(activity);
////        }
////    }
//
//    private static void openAppSettings(Activity activity) {
//        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        intent.setData(Uri.parse("package:" + activity.getPackageName()));
//        activity.startActivity(intent);
//    }
//}
//
//    private static void safeStartActivity(Activity activity, Intent intent) {
//        if (intent.resolveActivity(activity.getPackageManager()) != null) {
//            activity.startActivity(intent);
//        } else {
//            Log.w("XiaomiHelper", "Target activity not found, falling back to app settings.");
//            XiaomiPermissionHelper.openAppSettings(activity);
//        }
//    }

public class XiaomiPermissionHelper {

    private static final String MIUI_AUTOSTART_ACTIVITY = "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity";
    private static final String MIUI_BATTERY_ACTIVITY = "com.miui.powerkeeper/com.miui.powerkeeper.ui.HiddenAppsConfigActivity";

//    public static boolean isXiaomiDevice() {
//        return Build.MANUFACTURER.equalsIgnoreCase("xiaomi");
//    }

    public static void checkAutoStartPermission(Activity activity) {
//        if (!isXiaomiDevice()) return;

        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ));
            safeStartActivity(activity, intent);
        } catch (Exception e) {
            openAppSettings(activity);
        }
    }

    public static void checkBatteryOptimization(Activity activity) {
//        if (!isXiaomiDevice()) return;

        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            ));
            intent.putExtra("package_name", activity.getPackageName());
            intent.putExtra("package_label", activity.getString(R.string.app_name));
            safeStartActivity(activity, intent);
        } catch (Exception e) {
            openAppSettings(activity);
        }
    }

    private static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    // ✅ THIS WAS OUTSIDE THE CLASS BEFORE — now it's inside
    private static void safeStartActivity(Activity activity, Intent intent) {
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            Log.w("XiaomiHelper", "Target activity not found, falling back to app settings.");
            openAppSettings(activity); // No need to use XiaomiPermissionHelper.openAppSettings here
        }
    }


//    public static void checkBackgroundPopupPermission(Activity activity) {
//        if (!isXiaomiDevice()) return;
//
//        try {
//            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
//            intent.setClassName("com.miui.securitycenter",
//                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
//            intent.putExtra("extra_pkgname", activity.getPackageName());
//            safeStartActivity(activity, intent);
//        } catch (Exception e) {
//            // Fallback to app settings
//            openAppSettings(activity);
//        }
//    }

    public static void checkBackgroundPopupPermission(Activity activity) {
//        if (!isXiaomiDevice()) return;

        SharedPreferences prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean alreadyPrompted = prefs.getBoolean("popup_permission_prompted", false);

        if (alreadyPrompted) return;

        try {
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", activity.getPackageName());
            safeStartActivity(activity, intent);

            Toast.makeText(activity, "Please allow 'Background pop-up' permission.", Toast.LENGTH_LONG).show();

            // Mark as prompted
            prefs.edit().putBoolean("popup_permission_prompted", true).apply();
        } catch (Exception e) {
            openAppSettings(activity);
        }
    }


//    public static void openRestrictedSettings(Activity activity) {
//        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        intent.setData(Uri.parse("package:" + activity.getPackageName()));
//        try {
//            activity.startActivity(intent);
//            Toast.makeText(activity, "Enable 'Allow restricted settings' manually", Toast.LENGTH_LONG).show();
//        } catch (ActivityNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
    public static void openRestrictedSettings(Activity activity) {
        SharedPreferences prefs2 = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean alreadyPrompted = prefs2.getBoolean("restricted_prompted", false);

        if (alreadyPrompted) return; // Don't open it again

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        try {
            safeStartActivity(activity, intent);

            Toast.makeText(activity, "Enable 'Allow restricted settings' manually", Toast.LENGTH_LONG).show();

            // Mark that we've prompted the user
            prefs2.edit().putBoolean("restricted_prompted", true).apply();

        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }
}

