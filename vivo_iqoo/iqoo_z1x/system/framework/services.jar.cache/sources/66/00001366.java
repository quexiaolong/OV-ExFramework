package com.android.server.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import com.android.internal.logging.UiEventLogger;
import com.android.server.usb.descriptors.UsbDescriptor;

/* loaded from: classes.dex */
public interface NotificationChannelLogger {
    void logAppEvent(NotificationChannelEvent notificationChannelEvent, int i, String str);

    void logNotificationChannel(NotificationChannelEvent notificationChannelEvent, NotificationChannel notificationChannel, int i, String str, int i2, int i3);

    void logNotificationChannelGroup(NotificationChannelEvent notificationChannelEvent, NotificationChannelGroup notificationChannelGroup, int i, String str, boolean z);

    default void logNotificationChannelCreated(NotificationChannel channel, int uid, String pkg) {
        logNotificationChannel(NotificationChannelEvent.getCreated(channel), channel, uid, pkg, 0, getLoggingImportance(channel));
    }

    default void logNotificationChannelDeleted(NotificationChannel channel, int uid, String pkg) {
        logNotificationChannel(NotificationChannelEvent.getDeleted(channel), channel, uid, pkg, getLoggingImportance(channel), 0);
    }

    default void logNotificationChannelModified(NotificationChannel channel, int uid, String pkg, int oldLoggingImportance, boolean byUser) {
        logNotificationChannel(NotificationChannelEvent.getUpdated(byUser), channel, uid, pkg, oldLoggingImportance, getLoggingImportance(channel));
    }

    default void logNotificationChannelGroup(NotificationChannelGroup channelGroup, int uid, String pkg, boolean isNew, boolean wasBlocked) {
        logNotificationChannelGroup(NotificationChannelEvent.getGroupUpdated(isNew), channelGroup, uid, pkg, wasBlocked);
    }

    default void logNotificationChannelGroupDeleted(NotificationChannelGroup channelGroup, int uid, String pkg) {
        logNotificationChannelGroup(NotificationChannelEvent.NOTIFICATION_CHANNEL_GROUP_DELETED, channelGroup, uid, pkg, false);
    }

    default void logAppNotificationsAllowed(int uid, String pkg, boolean enabled) {
        logAppEvent(NotificationChannelEvent.getBlocked(enabled), uid, pkg);
    }

    /* loaded from: classes.dex */
    public enum NotificationChannelEvent implements UiEventLogger.UiEventEnum {
        NOTIFICATION_CHANNEL_CREATED(219),
        NOTIFICATION_CHANNEL_UPDATED(UsbDescriptor.CLASSID_DIAGNOSTIC),
        NOTIFICATION_CHANNEL_UPDATED_BY_USER(221),
        NOTIFICATION_CHANNEL_DELETED(222),
        NOTIFICATION_CHANNEL_GROUP_CREATED(223),
        NOTIFICATION_CHANNEL_GROUP_UPDATED(UsbDescriptor.CLASSID_WIRELESS),
        NOTIFICATION_CHANNEL_GROUP_DELETED(226),
        NOTIFICATION_CHANNEL_CONVERSATION_CREATED(272),
        NOTIFICATION_CHANNEL_CONVERSATION_DELETED(274),
        APP_NOTIFICATIONS_BLOCKED(557),
        APP_NOTIFICATIONS_UNBLOCKED(558);
        
        private final int mId;

        NotificationChannelEvent(int id) {
            this.mId = id;
        }

        public int getId() {
            return this.mId;
        }

        public static NotificationChannelEvent getUpdated(boolean byUser) {
            if (byUser) {
                return NOTIFICATION_CHANNEL_UPDATED_BY_USER;
            }
            return NOTIFICATION_CHANNEL_UPDATED;
        }

        public static NotificationChannelEvent getCreated(NotificationChannel channel) {
            if (channel.getConversationId() != null) {
                return NOTIFICATION_CHANNEL_CONVERSATION_CREATED;
            }
            return NOTIFICATION_CHANNEL_CREATED;
        }

        public static NotificationChannelEvent getDeleted(NotificationChannel channel) {
            if (channel.getConversationId() != null) {
                return NOTIFICATION_CHANNEL_CONVERSATION_DELETED;
            }
            return NOTIFICATION_CHANNEL_DELETED;
        }

        public static NotificationChannelEvent getGroupUpdated(boolean isNew) {
            if (isNew) {
                return NOTIFICATION_CHANNEL_GROUP_CREATED;
            }
            return NOTIFICATION_CHANNEL_GROUP_DELETED;
        }

        public static NotificationChannelEvent getBlocked(boolean enabled) {
            return enabled ? APP_NOTIFICATIONS_UNBLOCKED : APP_NOTIFICATIONS_BLOCKED;
        }
    }

    static int getIdHash(NotificationChannel channel) {
        return SmallHash.hash(channel.getId());
    }

    static int getIdHash(NotificationChannelGroup group) {
        return SmallHash.hash(group.getId());
    }

    static int getLoggingImportance(NotificationChannel channel) {
        return getLoggingImportance(channel, channel.getImportance());
    }

    static int getLoggingImportance(NotificationChannel channel, int importance) {
        if (channel.getConversationId() == null || importance < 4) {
            return importance;
        }
        if (channel.isImportantConversation()) {
            return 5;
        }
        return importance;
    }

    static int getImportance(NotificationChannelGroup channelGroup) {
        return getImportance(channelGroup.isBlocked());
    }

    static int getImportance(boolean isBlocked) {
        if (isBlocked) {
            return 0;
        }
        return 3;
    }
}