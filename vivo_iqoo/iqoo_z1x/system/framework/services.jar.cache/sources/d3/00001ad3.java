package com.android.server.usb;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.os.Bundle;
import android.os.UserHandle;
import com.android.internal.notification.SystemNotificationChannels;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class MtpNotificationManager {
    private static final String ACTION_OPEN_IN_APPS = "com.android.server.usb.ACTION_OPEN_IN_APPS";
    private static final int PROTOCOL_MTP = 0;
    private static final int PROTOCOL_PTP = 1;
    private static final int SUBCLASS_MTP = 255;
    private static final int SUBCLASS_STILL_IMAGE_CAPTURE = 1;
    private static final String TAG = "UsbMtpNotificationManager";
    private final Context mContext;
    private final OnOpenInAppListener mListener;

    /* loaded from: classes2.dex */
    interface OnOpenInAppListener {
        void onOpenInApp(UsbDevice usbDevice);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MtpNotificationManager(Context context, OnOpenInAppListener listener) {
        this.mContext = context;
        this.mListener = listener;
        Receiver receiver = new Receiver();
        context.registerReceiver(receiver, new IntentFilter(ACTION_OPEN_IN_APPS));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showNotification(UsbDevice device) {
        Resources resources = this.mContext.getResources();
        String title = resources.getString(17041942, device.getProductName());
        String description = resources.getString(17041941);
        Bundle bundle = new Bundle();
        bundle.putInt("vivo.summaryIconRes", 50463853);
        Notification.Builder builder = new Notification.Builder(this.mContext, SystemNotificationChannels.USB).setContentTitle(title).setContentText(description).setSmallIcon(50463852).setExtras(bundle).setCategory("sys");
        Intent intent = new Intent(ACTION_OPEN_IN_APPS);
        intent.putExtra("device", device);
        intent.addFlags(1342177280);
        PendingIntent openIntent = PendingIntent.getBroadcastAsUser(this.mContext, device.getDeviceId(), intent, AudioFormat.OPUS, UserHandle.SYSTEM);
        builder.setContentIntent(openIntent);
        Notification notification = builder.build();
        notification.flags |= 256;
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notify(Integer.toString(device.getDeviceId()), 25, notification);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideNotification(int deviceId) {
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancel(Integer.toString(deviceId), 25);
    }

    /* loaded from: classes2.dex */
    private class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            UsbDevice device = (UsbDevice) intent.getExtras().getParcelable("device");
            if (device == null) {
                return;
            }
            String action = intent.getAction();
            char c = 65535;
            if (action.hashCode() == 768361239 && action.equals(MtpNotificationManager.ACTION_OPEN_IN_APPS)) {
                c = 0;
            }
            if (c == 0) {
                MtpNotificationManager.this.mListener.onOpenInApp(device);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean shouldShowNotification(PackageManager packageManager, UsbDevice device) {
        return !packageManager.hasSystemFeature("android.hardware.type.automotive") && isMtpDevice(device);
    }

    private static boolean isMtpDevice(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (usbInterface.getInterfaceClass() == 6 && usbInterface.getInterfaceSubclass() == 1 && usbInterface.getInterfaceProtocol() == 1) {
                return true;
            }
            if (usbInterface.getInterfaceClass() == 255 && usbInterface.getInterfaceSubclass() == 255 && usbInterface.getInterfaceProtocol() == 0 && "MTP".equals(usbInterface.getName())) {
                return true;
            }
        }
        return false;
    }
}