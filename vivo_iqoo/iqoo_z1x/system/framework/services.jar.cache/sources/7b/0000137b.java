package com.android.server.notification;

import android.app.Notification;
import android.app.NotificationChannel;

/* loaded from: classes.dex */
public interface NotificationManagerInternal {
    void cancelNotification(String str, String str2, int i, int i2, String str3, int i3, int i4);

    void enqueueNotification(String str, String str2, int i, int i2, String str3, int i3, Notification notification, int i4);

    NotificationChannel getNotificationChannel(String str, int i, String str2);

    void notifyAppSharePackageChanged(String str, int i);

    void onConversationRemoved(String str, int i, String str2);

    void removeForegroundServiceFlagFromNotification(String str, int i, int i2);
}