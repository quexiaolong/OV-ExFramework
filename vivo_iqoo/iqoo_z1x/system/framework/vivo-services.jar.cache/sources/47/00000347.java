package com.android.server.notification;

import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.notification.GroupHelper;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.security.server.VivoPermissionUtils;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoGroupHelperImpl implements IVivoGroupHelper {
    private static final int AUTOGROUP_AT_COUNT = 2;
    private static final int SLAD_NOTIFICATION_HEADER_ID = 50464259;
    private static final int SLAD_NOTIFICATION_ID = 50464261;
    private static final String TAG = "GroupHelper";

    public boolean isNeed2Group(boolean isNotAppGroup, StatusBarNotification sbn) {
        if (IS_VOS) {
            return isNotAppGroup;
        }
        return (isNotAppGroup || (isCustomNotification(sbn) && !sbn.getNotification().isGroupSummary())) && !isCountingToBundle(sbn);
    }

    public boolean isNeed2GroupOfCount(int notificationsForPackageSize) {
        return notificationsForPackageSize >= 2;
    }

    public boolean isCustomNotification(StatusBarNotification sbn) {
        VSlog.d(TAG, "sbn = " + sbn);
        return (IS_VOS || !sbn.getNotification().isCustomNotification() || isCountingToBundle(sbn) || isSlaNotification(sbn)) ? false : true;
    }

    public boolean isCountingToBundle(StatusBarNotification sbn) {
        return isMusicPlaying(sbn) || isCleanMasterSecurity(sbn) || isForeService(sbn) || isGameHanging(sbn) || isAssistant(sbn);
    }

    private boolean isAssistant(StatusBarNotification sbn) {
        return "com.vivo.assistant".equals(sbn.getPackageName()) && sbn.getId() == 4444;
    }

    private boolean isMusicPlaying(StatusBarNotification sbn) {
        return "com.android.bbkmusic".equals(sbn.getPackageName()) && (sbn.getId() == 1 || sbn.getId() == 10001);
    }

    private boolean isCleanMasterSecurity(StatusBarNotification sbn) {
        return "com.cleanmaster.security_cn".equals(sbn.getPackageName()) && sbn.getId() == 10001;
    }

    private boolean isForeService(StatusBarNotification sbn) {
        return VivoPermissionUtils.OS_PKG.equals(sbn.getPackageName()) && sbn.getId() == 40;
    }

    private boolean isGameHanging(StatusBarNotification sbn) {
        return "com.vivo.daemonService".equals(sbn.getPackageName()) && sbn.getId() == 987654321;
    }

    private boolean isSlaNotification(StatusBarNotification sbn) {
        return VivoPermissionUtils.OS_PKG.equals(sbn.getPackageName()) && (sbn.getId() == SLAD_NOTIFICATION_HEADER_ID || sbn.getId() == SLAD_NOTIFICATION_ID);
    }

    public void updateAutoGroupSummaryIfHasOngoing(StatusBarNotification sbn, GroupHelper.Callback callback, String key, ArrayMap<String, ArraySet<String>> ongoingGroupCount) {
        if ("ranker_group".equals(sbn.getGroup()) && (sbn.getNotification().flags & Consts.ProcessStates.FOCUS) == 1024) {
            ArraySet<String> notifications = ongoingGroupCount.getOrDefault(key, new ArraySet<>(0));
            if (notifications.size() > 0) {
                callback.updateAutogroupSummary(sbn.getKey(), true);
            }
        }
    }
}