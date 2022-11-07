package com.android.server.notification;

import android.app.NotificationHistory;
import android.text.TextUtils;
import com.android.server.IVivoRmsInjector;

/* loaded from: classes.dex */
public final class NotificationHistoryFilter {
    private String mChannel;
    private int mNotificationCount;
    private String mPackage;

    private NotificationHistoryFilter() {
    }

    public String getPackage() {
        return this.mPackage;
    }

    public String getChannel() {
        return this.mChannel;
    }

    public int getMaxNotifications() {
        return this.mNotificationCount;
    }

    public boolean isFiltering() {
        return (getPackage() == null && getChannel() == null && this.mNotificationCount >= Integer.MAX_VALUE) ? false : true;
    }

    public boolean matchesPackageAndChannelFilter(NotificationHistory.HistoricalNotification notification) {
        if (!TextUtils.isEmpty(getPackage())) {
            if (getPackage().equals(notification.getPackage())) {
                return TextUtils.isEmpty(getChannel()) || getChannel().equals(notification.getChannelId());
            }
            return false;
        }
        return true;
    }

    public boolean matchesCountFilter(NotificationHistory notifications) {
        return notifications.getHistoryCount() < this.mNotificationCount;
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private String mPackage = null;
        private String mChannel = null;
        private int mNotificationCount = IVivoRmsInjector.QUIET_TYPE_ALL;

        public Builder setPackage(String aPackage) {
            this.mPackage = aPackage;
            return this;
        }

        public Builder setChannel(String pkg, String channel) {
            setPackage(pkg);
            this.mChannel = channel;
            return this;
        }

        public Builder setMaxNotifications(int notificationCount) {
            this.mNotificationCount = notificationCount;
            return this;
        }

        public NotificationHistoryFilter build() {
            NotificationHistoryFilter filter = new NotificationHistoryFilter();
            filter.mPackage = this.mPackage;
            filter.mChannel = this.mChannel;
            filter.mNotificationCount = this.mNotificationCount;
            return filter;
        }
    }
}