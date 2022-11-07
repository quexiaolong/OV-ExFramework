package com.android.server.notification;

import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.server.VivoSystemServiceFactory;
import com.android.server.notification.IVivoGroupHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class GroupHelper implements IVivoGroupHelper.IVivoGroupHelperExport {
    protected static final String AUTOGROUP_KEY = "ranker_group";
    private final int mAutoGroupAtCount;
    private final Callback mCallback;
    final ArrayMap<String, ArraySet<String>> mOngoingGroupCount = new ArrayMap<>();
    Map<Integer, Map<String, LinkedHashSet<String>>> mUngroupedNotifications = new HashMap();
    private IVivoGroupHelper mVivoGroupHelper;
    private static final String TAG = "GroupHelper";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public interface Callback {
        void addAutoGroup(String str);

        void addAutoGroup(String str, boolean z);

        void addAutoGroupSummary(int i, String str, String str2);

        void addAutoGroupSummary(int i, String str, String str2, boolean z);

        void addAutoGroupSummary(int i, String str, String str2, boolean z, boolean z2);

        void removeAutoGroup(String str);

        void removeAutoGroupSummary(int i, String str);

        void updateAutogroupSummary(StatusBarNotification statusBarNotification, boolean z);

        void updateAutogroupSummary(String str, boolean z);
    }

    public GroupHelper(int autoGroupAtCount, Callback callback) {
        this.mAutoGroupAtCount = autoGroupAtCount;
        this.mCallback = callback;
        VivoSystemServiceFactory vivoSystemServiceFactory = VivoSystemServiceFactory.getSystemServiceFactoryImpl();
        if (vivoSystemServiceFactory != null) {
            this.mVivoGroupHelper = vivoSystemServiceFactory.createVivoGroupHelper();
        }
    }

    private String generatePackageGroupKey(int userId, String pkg, String group) {
        return userId + "|" + pkg + "|" + group;
    }

    protected int getOngoingGroupCount(int userId, String pkg, String group) {
        String key = generatePackageGroupKey(userId, pkg, group);
        return this.mOngoingGroupCount.getOrDefault(key, new ArraySet<>(0)).size();
    }

    private void addToOngoingGroupCount(StatusBarNotification sbn, boolean add) {
        addToOngoingGroupCount(sbn, add, false);
    }

    private void addToOngoingGroupCount(StatusBarNotification sbn, boolean add, boolean summaryExists) {
        if (sbn.getNotification().isGroupSummary() && add) {
            IVivoGroupHelper iVivoGroupHelper = this.mVivoGroupHelper;
            if (iVivoGroupHelper != null) {
                iVivoGroupHelper.updateAutoGroupSummaryIfHasOngoing(sbn, this.mCallback, generatePackageGroupKey(sbn.getUser().getIdentifier(), sbn.getPackageName(), sbn.getGroup()), this.mOngoingGroupCount);
            }
        } else if (sbn.isOngoing() || !add) {
            int userId = sbn.getUser().getIdentifier();
            String key = generatePackageGroupKey(userId, sbn.getPackageName(), AUTOGROUP_KEY);
            ArraySet<String> notifications = this.mOngoingGroupCount.getOrDefault(key, new ArraySet<>(0));
            if (add) {
                notifications.add(sbn.getKey());
                this.mOngoingGroupCount.put(key, notifications);
            } else {
                notifications.remove(sbn.getKey());
            }
            generatePackageGroupKey(userId, sbn.getPackageName(), AUTOGROUP_KEY);
            boolean needsOngoingFlag = notifications.size() > 0;
            if (summaryExists) {
                if (needsOngoingFlag) {
                    this.mCallback.updateAutogroupSummary(sbn.getKey(), needsOngoingFlag);
                } else {
                    this.mCallback.updateAutogroupSummary(sbn, needsOngoingFlag);
                }
            }
        }
    }

    public void onNotificationUpdated(StatusBarNotification childSbn, boolean autogroupSummaryExists) {
        if (childSbn.getGroup() != AUTOGROUP_KEY || childSbn.getNotification().isGroupSummary()) {
            return;
        }
        if (childSbn.isOngoing()) {
            addToOngoingGroupCount(childSbn, true, autogroupSummaryExists);
        } else {
            addToOngoingGroupCount(childSbn, false, autogroupSummaryExists);
        }
    }

    public void onNotificationPosted(StatusBarNotification sbn, boolean autogroupSummaryExists) {
        onNotificationPosted(sbn, autogroupSummaryExists, false);
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x003f, code lost:
        if (r13.isAppGroup() == false) goto L13;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x00a6, code lost:
        if (r6.size() < r12.mAutoGroupAtCount) goto L53;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00b4, code lost:
        if (r12.mVivoGroupHelper.isCustomNotification(r13) == false) goto L34;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void onNotificationPosted(android.service.notification.StatusBarNotification r13, boolean r14, boolean r15) {
        /*
            Method dump skipped, instructions count: 295
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.GroupHelper.onNotificationPosted(android.service.notification.StatusBarNotification, boolean, boolean):void");
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        try {
            maybeUngroup(sbn, true, sbn.getUserId());
        } catch (Exception e) {
            Slog.e(TAG, "Error processing canceled notification", e);
        }
    }

    private void maybeUngroup(StatusBarNotification sbn, boolean notificationGone, int userId) {
        List<String> notificationsToUnAutogroup = new ArrayList<>();
        boolean removeSummary = false;
        synchronized (this.mUngroupedNotifications) {
            Map<String, LinkedHashSet<String>> ungroupedNotificationsByUser = this.mUngroupedNotifications.get(Integer.valueOf(sbn.getUserId()));
            if (ungroupedNotificationsByUser != null && ungroupedNotificationsByUser.size() != 0) {
                LinkedHashSet<String> notificationsForPackage = ungroupedNotificationsByUser.get(sbn.getPackageName());
                if (notificationsForPackage != null && notificationsForPackage.size() != 0) {
                    if (notificationsForPackage.remove(sbn.getKey()) && !notificationGone) {
                        notificationsToUnAutogroup.add(sbn.getKey());
                    }
                    if (notificationsForPackage.size() == 0) {
                        ungroupedNotificationsByUser.remove(sbn.getPackageName());
                        removeSummary = true;
                    }
                    boolean z = true;
                    if (notificationGone) {
                        if (notificationsForPackage.size() <= 0 || removeSummary) {
                            z = false;
                        }
                        addToOngoingGroupCount(sbn, false, z);
                    } else {
                        if (!AUTOGROUP_KEY.equals(sbn.getGroup()) || (sbn.getNotification().flags & 1024) != 1024) {
                            z = false;
                        }
                        addToOngoingGroupCount(sbn, z);
                    }
                    if (removeSummary) {
                        adjustAutogroupingSummary(userId, sbn.getPackageName(), null, false);
                    }
                    if (notificationsToUnAutogroup.size() > 0) {
                        adjustNotificationBundling(notificationsToUnAutogroup, false);
                    }
                }
            }
        }
    }

    private void adjustAutogroupingSummary(int userId, String packageName, String triggeringKey, boolean summaryNeeded) {
        adjustAutogroupingSummary(userId, packageName, triggeringKey, summaryNeeded, false);
    }

    private void adjustAutogroupingSummary(int userId, String packageName, String triggeringKey, boolean summaryNeeded, boolean modified) {
        adjustAutogroupingSummary(userId, packageName, triggeringKey, summaryNeeded, modified, false);
    }

    private void adjustAutogroupingSummary(int userId, String packageName, String triggeringKey, boolean summaryNeeded, boolean modified, boolean ongoing) {
        if (summaryNeeded) {
            this.mCallback.addAutoGroupSummary(userId, packageName, triggeringKey, modified, ongoing);
        } else {
            this.mCallback.removeAutoGroupSummary(userId, packageName);
        }
    }

    private void adjustNotificationBundling(List<String> keys, boolean group) {
        adjustNotificationBundling(keys, group, false);
    }

    private void adjustNotificationBundling(List<String> keys, boolean group, boolean modified) {
        for (String key : keys) {
            if (DEBUG) {
                Log.i(TAG, "Sending grouping adjustment for: " + key + " group? " + group);
            }
            if (group) {
                this.mCallback.addAutoGroup(key, modified);
            } else {
                this.mCallback.removeAutoGroup(key);
            }
        }
    }

    @Override // com.android.server.notification.IVivoGroupHelper.IVivoGroupHelperExport
    public IVivoGroupHelper getVivoInjectInstance() {
        return this.mVivoGroupHelper;
    }
}