package com.android.server.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLoggerImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.notification.NotificationChannelLogger;

/* loaded from: classes.dex */
public class NotificationChannelLoggerImpl implements NotificationChannelLogger {
    UiEventLogger mUiEventLogger = new UiEventLoggerImpl();

    @Override // com.android.server.notification.NotificationChannelLogger
    public void logNotificationChannel(NotificationChannelLogger.NotificationChannelEvent event, NotificationChannel channel, int uid, String pkg, int oldImportance, int newImportance) {
        FrameworkStatsLog.write(246, event.getId(), uid, pkg, NotificationChannelLogger.getIdHash(channel), oldImportance, newImportance);
    }

    @Override // com.android.server.notification.NotificationChannelLogger
    public void logNotificationChannelGroup(NotificationChannelLogger.NotificationChannelEvent event, NotificationChannelGroup channelGroup, int uid, String pkg, boolean wasBlocked) {
        FrameworkStatsLog.write(246, event.getId(), uid, pkg, NotificationChannelLogger.getIdHash(channelGroup), NotificationChannelLogger.getImportance(wasBlocked), NotificationChannelLogger.getImportance(channelGroup));
    }

    @Override // com.android.server.notification.NotificationChannelLogger
    public void logAppEvent(NotificationChannelLogger.NotificationChannelEvent event, int uid, String pkg) {
        this.mUiEventLogger.log(event, uid, pkg);
    }
}