package com.android.server.usb;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Message;

/* loaded from: classes2.dex */
public interface IVivoUsbHandler {
    boolean allowSetRndis(long j);

    boolean allowUsbTransfer();

    String applyAdbFunctionVivo(String str);

    void cancelVivoUsbNotification();

    void dummy();

    void finishBoot(boolean z, boolean z2, boolean z3, long j, boolean z4, boolean z5, long j2);

    Intent generateNotificationIntent();

    Notification.Builder generateUsbNotificationBuilder(String str, CharSequence charSequence, CharSequence charSequence2, PendingIntent pendingIntent);

    long getChargingFunctions();

    void handleMessage(Message message);

    boolean handleMessageUpdateState(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, long j, long j2, boolean z6);

    boolean initCurrentFunctions(IVivoUsbHandlerLegacy iVivoUsbHandlerLegacy, boolean z, long j);

    boolean isPhoneLocked();

    boolean isUsbTransferAllowedVivo();

    boolean notifyAccessoryDetached();

    void setDebugPort(boolean z);

    boolean stopSetCurrentFunctions(long j, boolean z, String str);

    String trySetEnabledFunctionsVivo(long j, boolean z);

    void useVivoAdbNotification(boolean z, boolean z2, boolean z3);

    boolean useVivoUsbNotification(int i, int i2, boolean z, boolean z2, long j, boolean z3);

    /* loaded from: classes2.dex */
    public interface IVivoUsbHandlerExport {
        IVivoUsbHandler getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }
    }
}