package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class PermissionDialogHelper {


    private final Activity activity;

    private final SharedPreferences prefs1;

    public PermissionDialogHelper(Activity activity) {
        this.activity = activity;
//        this.prefs1 = PreferenceManager.getDefaultSharedPreferences(activity);
//        this.prefs1 = getSharedPreferences("locked_apps_prefs", Context.MODE_PRIVATE);
        this.prefs1 = activity.getSharedPreferences("xiaomi_permission_prefs", Context.MODE_PRIVATE);
    }

    public void showPermissionFlowIfNeeded() {
//        XiaomiPermissionHelper.isXiaomiDevice() &&
        if (!prefs1.getBoolean("xiaomi_permissions_shown", false)) {

            new AlertDialog.Builder(activity)
                    .setTitle("Xiaomi Device Detected")
                    .setMessage("For best performance, please enable these settings:")
                    .setPositiveButton("Continue", (dialog, which) -> showStep1Dialog())
                    .setCancelable(false)
                    .show();
        }
    }

    private void showStep1Dialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Step 1/2: Enable Autostart")
                .setMessage("Please enable Autostart for this app in MIUI Security settings")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    XiaomiPermissionHelper.checkAutoStartPermission(activity);
                    new Handler().postDelayed(() -> showStep2Dialog(), 1000);
                })
                .setNegativeButton("Later", (dialog, which) -> showStep2Dialog())
                .show();
    }

    private void showStep2Dialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Step 2/2: Disable Battery Restrictions")
                .setMessage("Please set battery optimization to 'No restrictions'")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    XiaomiPermissionHelper.checkBatteryOptimization(activity);
                    markAsCompleted();
                })
                .setNegativeButton("Skip", (dialog, which) -> markAsCompleted())
                .show();
    }

    private void markAsCompleted() {
        prefs1.edit().putBoolean("xiaomi_permissions_shown", true).apply();
    }
}
