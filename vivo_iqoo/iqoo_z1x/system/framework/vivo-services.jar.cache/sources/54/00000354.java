package com.android.server.notification;

import android.os.FtBuild;
import java.util.Comparator;

/* loaded from: classes.dex */
public class VivoRankingHelperImpl implements IVivoRankingHelper {
    private static final int INTERNAL_NOTIFICATION_TYPE_PUSH = 1;
    private final boolean isTargetOS;
    private NotificationClassifyManager mClassifyManager;
    private final Comparator<NotificationRecord> mVivoNotificationClassifySort;
    private final Comparator<NotificationRecord> mVivoNotificationPrimarySort;

    public VivoRankingHelperImpl() {
        this.isTargetOS = FtBuild.getRomVersion() >= 12.0f;
        this.mVivoNotificationPrimarySort = $$Lambda$VivoRankingHelperImpl$E_efZTk0fXJNSUzlbCh_vtANumo.INSTANCE;
        this.mVivoNotificationClassifySort = new Comparator<NotificationRecord>() { // from class: com.android.server.notification.VivoRankingHelperImpl.1
            @Override // java.util.Comparator
            public int compare(NotificationRecord left, NotificationRecord right) {
                boolean isLeftClassifyEnabled = true;
                boolean isRightClassifyEnabled = true;
                if (VivoRankingHelperImpl.this.mClassifyManager != null) {
                    isLeftClassifyEnabled = VivoRankingHelperImpl.this.mClassifyManager.areVivoCustomNotificationEnabled() && VivoRankingHelperImpl.this.mClassifyManager.areVivoCustomNotificationEnabledForPackage(left.getSbn().getPackageName(), left.getUid()) && left.getChannel().isAcceptNotificationClassifyManage();
                    isRightClassifyEnabled = VivoRankingHelperImpl.this.mClassifyManager.areVivoCustomNotificationEnabled() && VivoRankingHelperImpl.this.mClassifyManager.areVivoCustomNotificationEnabledForPackage(right.getSbn().getPackageName(), right.getUid()) && right.getChannel().isAcceptNotificationClassifyManage();
                }
                boolean leftNotificationCollapse = left.getImportance() < 2;
                boolean leftNotificationLowPriorityByClassify = isLeftClassifyEnabled && !leftNotificationCollapse && left.getSbn().getClassifyImportance() == 1;
                boolean leftLowPriorityPush = left.getNotification().internalType == 1 && left.getNotification().internalPriority < 0 && !leftNotificationCollapse;
                boolean rightNotificationCollapse = right.getImportance() < 2;
                boolean rightNotificationLowPriorityByClassify = isRightClassifyEnabled && !rightNotificationCollapse && right.getSbn().getClassifyImportance() == 1;
                boolean rightLowPriorityPush = right.getNotification().internalType == 1 && right.getNotification().internalPriority < 0 && !rightNotificationCollapse;
                if (!leftNotificationLowPriorityByClassify && !leftLowPriorityPush) {
                    return leftNotificationCollapse ? rightNotificationCollapse ? 0 : 1 : (rightNotificationLowPriorityByClassify || rightLowPriorityPush || rightNotificationCollapse) ? -1 : 0;
                } else if (rightNotificationCollapse) {
                    return -1;
                } else {
                    return (rightNotificationLowPriorityByClassify || rightLowPriorityPush) ? 0 : 1;
                }
            }
        };
    }

    public void setClassifyManager(NotificationClassifyManager manager) {
        if (manager != null) {
            this.mClassifyManager = manager;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:46:0x0126  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void sort(java.util.ArrayList<com.android.server.notification.NotificationRecord> r22, java.util.Comparator<com.android.server.notification.NotificationRecord> r23, com.android.server.notification.GlobalSortKeyComparator r24) {
        /*
            Method dump skipped, instructions count: 426
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.VivoRankingHelperImpl.sort(java.util.ArrayList, java.util.Comparator, com.android.server.notification.GlobalSortKeyComparator):void");
    }
}