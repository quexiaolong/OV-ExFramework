package com.android.server.notification;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.server.LocalServices;
import com.android.server.notification.PreferencesHelper;
import java.util.Map;

/* loaded from: classes.dex */
public class VivoPreferencesHelperImpl implements IVivoPreferencesHelper {
    private static final String TAG = "NotificationPrefHelper";
    Context mContext;
    PreferencesHelper mPreferencesHelper;

    public VivoPreferencesHelperImpl(Context context, PreferencesHelper preferencesHelper) {
        this.mContext = context;
        this.mPreferencesHelper = preferencesHelper;
    }

    public void setMoreNotificationEnabled(String packageName, int uid, boolean enabled, boolean effectChannel) {
        boolean wasEnabled = areMoreNotificationsEnabled(packageName, uid);
        if (!effectChannel && wasEnabled == enabled) {
            return;
        }
        PreferencesHelper.PackagePreferences r = this.mPreferencesHelper.getOrCreatePackagePreferencesLocked(packageName, uid);
        r.moreNotificationsEnabled = enabled;
        if (effectChannel) {
            for (Map.Entry<String, NotificationChannel> entry : r.channels.entrySet()) {
                NotificationChannel channel = entry.getValue();
                if (!channel.isCreatedByPushService() && !"miscellaneous".equals(channel.getId()) && (channel.getUserLockedFields() & 4) == 0 && (!enabled || channel.getImportance() == 0)) {
                    if (!enabled) {
                        channel.setAppCreateImportance(channel.getImportance());
                    }
                    channel.setImportance(enabled ? channel.getAppCreateImportance() : 0);
                }
            }
        }
    }

    public boolean areMoreNotificationsEnabled(String packageName, int uid) {
        return this.mPreferencesHelper.getOrCreatePackagePreferencesLocked(packageName, uid).moreNotificationsEnabled;
    }

    public void createPushChannelIfNeeded(PreferencesHelper.PackagePreferences r, boolean isOverrideNotification) {
        if (!isOverrideNotification || r == null) {
            return;
        }
        if (r.channels.containsKey("vivo_push_channel")) {
            ((NotificationChannel) r.channels.get("vivo_push_channel")).setName(this.mContext.getString(51249895));
            return;
        }
        NotificationChannel channel = new NotificationChannel("vivo_push_channel", this.mContext.getString(51249895), 4);
        r.channels.put(channel.getId(), channel);
    }

    public boolean onlyHasDefaultChannels(PreferencesHelper.PackagePreferences r) {
        if (r.channels.size() == 2 && r.channels.containsKey("miscellaneous") && r.channels.containsKey("vivo_push_channel") && !r.hasChannelCreatedByPush) {
            return true;
        }
        if (r.channels.isEmpty() || !r.channels.containsKey("miscellaneous")) {
            return false;
        }
        for (NotificationChannel channel : r.channels.values()) {
            if (!channel.getId().equals("miscellaneous") && !channel.getId().equals("vivo_push_channel") && !channel.isCreatedByPushService()) {
                return false;
            }
        }
        return true;
    }

    public boolean shouldHaveDefaultChannel(String packageName) {
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        return pmi.getPackageTargetSdkVersion(packageName) < 26;
    }

    public boolean markApplicationCrashedByDeletingFgsChannel(String pkg, int crashedUid, String channelId) {
        PreferencesHelper.PackagePreferences preferences = this.mPreferencesHelper.getOrCreatePackagePreferencesLocked(pkg, crashedUid);
        NotificationChannel c = (NotificationChannel) preferences.channels.get(channelId);
        if (c != null) {
            c.setRequestDeleteByAppForFgsNotification(true);
            preferences.channels.put(channelId, c);
        }
        return preferences.crashFixed;
    }

    public boolean markApplicationCrashIgnored(String crashedPkg, ArrayMap<String, PreferencesHelper.PackagePreferences> preferences) {
        boolean updated = false;
        for (Map.Entry<String, PreferencesHelper.PackagePreferences> entry : preferences.entrySet()) {
            if (!TextUtils.isEmpty(entry.getKey())) {
                if (entry.getKey().startsWith(crashedPkg + "|")) {
                    PreferencesHelper.PackagePreferences target = entry.getValue();
                    target.crashFixed = true;
                    preferences.put(entry.getKey(), target);
                    updated = true;
                }
            }
        }
        return updated;
    }
}