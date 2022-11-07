package com.android.server;

import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
/* loaded from: classes.dex */
public abstract class SystemService {
    protected static final boolean DEBUG_USER = false;
    public static final int PHASE_ACTIVITY_MANAGER_READY = 550;
    public static final int PHASE_BOOT_COMPLETED = 1000;
    public static final int PHASE_DEVICE_SPECIFIC_SERVICES_READY = 520;
    public static final int PHASE_LOCK_SETTINGS_READY = 480;
    public static final int PHASE_SYSTEM_SERVICES_READY = 500;
    public static final int PHASE_THIRD_PARTY_APPS_CAN_START = 600;
    public static final int PHASE_WAIT_FOR_DEFAULT_DISPLAY = 100;
    private final Context mContext;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface BootPhase {
    }

    public abstract void onStart();

    @SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
    /* loaded from: classes.dex */
    public static final class TargetUser {
        private final UserInfo mUserInfo;

        public TargetUser(UserInfo userInfo) {
            this.mUserInfo = userInfo;
        }

        public UserInfo getUserInfo() {
            return this.mUserInfo;
        }

        public UserHandle getUserHandle() {
            return this.mUserInfo.getUserHandle();
        }

        public int getUserIdentifier() {
            return this.mUserInfo.id;
        }

        public String toString() {
            return Integer.toString(getUserIdentifier());
        }
    }

    public SystemService(Context context) {
        this.mContext = context;
    }

    public final Context getContext() {
        return this.mContext;
    }

    public final Context getUiContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    public final boolean isSafeMode() {
        return getManager().isSafeMode();
    }

    public void onBootPhase(int phase) {
    }

    public boolean isUserSupported(TargetUser user) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpSupportedUsers(PrintWriter pw, String prefix) {
        List<UserInfo> allUsers = UserManager.get(this.mContext).getUsers();
        List<Integer> supportedUsers = new ArrayList<>(allUsers.size());
        for (UserInfo user : allUsers) {
            supportedUsers.add(Integer.valueOf(user.id));
        }
        if (allUsers.isEmpty()) {
            pw.print(prefix);
            pw.println("No supported users");
            return;
        }
        int size = supportedUsers.size();
        pw.print(prefix);
        pw.print(size);
        pw.print(" supported user");
        if (size > 1) {
            pw.print("s");
        }
        pw.print(": ");
        pw.println(supportedUsers);
    }

    @Deprecated
    public void onStartUser(int userId) {
    }

    @Deprecated
    public void onStartUser(UserInfo userInfo) {
        onStartUser(userInfo.id);
    }

    public void onUserStarting(TargetUser user) {
        onStartUser(user.getUserInfo());
    }

    @Deprecated
    public void onUnlockUser(int userId) {
    }

    @Deprecated
    public void onUnlockUser(UserInfo userInfo) {
        onUnlockUser(userInfo.id);
    }

    public void onUserUnlocking(TargetUser user) {
        onUnlockUser(user.getUserInfo());
    }

    public void onUserUnlocked(TargetUser user) {
    }

    @Deprecated
    public void onSwitchUser(int toUserId) {
    }

    @Deprecated
    public void onSwitchUser(UserInfo from, UserInfo to) {
        onSwitchUser(to.id);
    }

    public void onUserSwitching(TargetUser from, TargetUser to) {
        onSwitchUser(from == null ? null : from.getUserInfo(), to.getUserInfo());
    }

    @Deprecated
    public void onStopUser(int userId) {
    }

    @Deprecated
    public void onStopUser(UserInfo user) {
        onStopUser(user.id);
    }

    public void onUserStopping(TargetUser user) {
        onStopUser(user.getUserInfo());
    }

    @Deprecated
    public void onCleanupUser(int userId) {
    }

    @Deprecated
    public void onCleanupUser(UserInfo user) {
        onCleanupUser(user.id);
    }

    public void onUserStopped(TargetUser user) {
        onCleanupUser(user.getUserInfo());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void publishBinderService(String name, IBinder service) {
        publishBinderService(name, service, false);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void publishBinderService(String name, IBinder service, boolean allowIsolated) {
        publishBinderService(name, service, allowIsolated, 8);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void publishBinderService(String name, IBinder service, boolean allowIsolated, int dumpPriority) {
        ServiceManager.addService(name, service, allowIsolated, dumpPriority);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final IBinder getBinderService(String name) {
        return ServiceManager.getService(name);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final <T> void publishLocalService(Class<T> type, T service) {
        LocalServices.addService(type, service);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final <T> T getLocalService(Class<T> type) {
        return (T) LocalServices.getService(type);
    }

    private SystemServiceManager getManager() {
        return (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
    }
}