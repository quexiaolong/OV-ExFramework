package com.android.server.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.multidisplay.MultiDisplayManager;
import com.android.server.VCarConfigManager;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUsbUserPermissionManagerImpl implements IVivoUsbUserPermissionManager {
    private String TAG = "VivoUsbUserPermissionManager";
    private UsbUserPermissionManager mUsbUserPermissionManager;

    public VivoUsbUserPermissionManagerImpl(Object manager) {
        this.mUsbUserPermissionManager = (UsbUserPermissionManager) manager;
    }

    public boolean ignoreRequestPermission(Context context, UsbAccessory accessory, String packageName, PendingIntent pi, int uid) {
        ArrayList<String> whitelist;
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && (whitelist = VCarConfigManager.getInstance(context).get("accessory")) != null && whitelist.size() > 0 && whitelist.contains(packageName)) {
            String str = this.TAG;
            VSlog.d(str, "ignoreRequestPermission and grantPermission pkg: " + packageName);
            this.mUsbUserPermissionManager.grantAccessoryPermission(accessory, uid);
            Intent intent = new Intent();
            intent.putExtra("accessory", accessory);
            intent.putExtra("permission", true);
            try {
                pi.send(context, 0, intent);
            } catch (PendingIntent.CanceledException e) {
                VSlog.d(this.TAG, "requestPermission PendingIntent was cancelled");
            }
            return true;
        }
        return false;
    }
}