package com.android.server.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.usb.UsbAccessory;

/* loaded from: classes2.dex */
public interface IVivoUsbUserPermissionManager {
    boolean ignoreRequestPermission(Context context, UsbAccessory usbAccessory, String str, PendingIntent pendingIntent, int i);
}