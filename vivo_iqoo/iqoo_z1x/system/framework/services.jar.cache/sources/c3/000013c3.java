package com.android.server.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.pm.ParceledListSlice;
import android.os.UserHandle;
import java.util.Collection;

/* loaded from: classes.dex */
public interface RankingConfig {
    boolean areMoreNotificationsEnabled(String str, int i);

    boolean badgingEnabled(UserHandle userHandle);

    boolean bubblesEnabled();

    boolean canShowBadge(String str, int i);

    boolean createNotificationChannel(String str, int i, NotificationChannel notificationChannel, boolean z, boolean z2);

    boolean createNotificationChannel(String str, int i, NotificationChannel notificationChannel, boolean z, boolean z2, boolean z3);

    boolean createNotificationChannel(String str, int i, NotificationChannel notificationChannel, boolean z, boolean z2, boolean z3, boolean z4);

    void createNotificationChannelGroup(String str, int i, NotificationChannelGroup notificationChannelGroup, boolean z);

    void deleteNotificationChannel(String str, int i, String str2);

    int getBubblePreference(String str, int i);

    NotificationChannel getConversationNotificationChannel(String str, int i, String str2, String str3, boolean z, boolean z2);

    int getImportance(String str, int i);

    NotificationChannel getNotificationChannel(String str, int i, String str2, boolean z);

    ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroups(String str, int i, boolean z, boolean z2, boolean z3);

    Collection<NotificationChannelGroup> getNotificationChannelGroups(String str, int i);

    ParceledListSlice<NotificationChannel> getNotificationChannels(String str, int i, boolean z);

    boolean isGroupBlocked(String str, int i, String str2);

    boolean isMediaNotificationFilteringEnabled();

    void permanentlyDeleteNotificationChannel(String str, int i, String str2);

    void permanentlyDeleteNotificationChannels(String str, int i);

    void setImportance(String str, int i, int i2);

    void setMoreNotificationEnabled(String str, int i, boolean z, boolean z2);

    void setShowBadge(String str, int i, boolean z);

    void updateNotificationChannel(String str, int i, NotificationChannel notificationChannel, boolean z);
}