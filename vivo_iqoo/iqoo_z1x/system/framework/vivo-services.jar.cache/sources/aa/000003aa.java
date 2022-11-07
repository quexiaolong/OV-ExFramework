package com.android.server.pm;

import android.app.ActivityManager;
import android.content.pm.UserInfo;
import android.util.SparseArray;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.pm.UserManagerService;
import com.vivo.services.rms.ProcessList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUmsImpl implements IVivoUms {
    private static final int DOUBLE_APP_FIXED_USER_ID = 999;
    static final int FIX_MODE_USER_FLAG = 268435456;
    static final int FIX_MODE_USER_ID = 888;
    private static final int FLAG_DOUBLE_APP_PROFILE = 536870912;
    static final String TAG = "VivoUmsImpl";
    private UserManagerService mUms;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();

    public VivoUmsImpl(UserManagerService ums) {
        this.mUms = null;
        if (ums == null) {
            VSlog.i(TAG, "container is " + ums);
        }
        this.mUms = ums;
    }

    public int getDoubleAppFixedUserId() {
        return 999;
    }

    public boolean isDoubleAppUserFlag(int flags) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && (FLAG_DOUBLE_APP_PROFILE & flags) != 0 && (flags & 32) != 0) {
            return true;
        }
        return false;
    }

    public boolean judgeConditionsCreatDoubleInstance() {
        return ActivityManager.isLowRamDeviceStatic();
    }

    public int getDoubleAppUserId(SparseArray<UserManagerService.UserData> users) {
        int doubleAppUserId = ProcessList.INVALID_ADJ;
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable()) {
            for (int i = 0; i < users.size(); i++) {
                UserInfo userInfo = users.valueAt(i).info;
                if (userInfo.isManagedProfile() && this.mVivoDoubleInstanceService.isDoubleAppUser(userInfo)) {
                    doubleAppUserId = userInfo.id;
                }
            }
        }
        return doubleAppUserId;
    }

    public boolean isDoubleAppUserExist(SparseArray<UserManagerService.UserData> users) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl == null || !vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable()) {
            return false;
        }
        for (int i = 0; i < users.size(); i++) {
            UserInfo userInfo = users.valueAt(i).info;
            if (userInfo.isManagedProfile() && this.mVivoDoubleInstanceService.isDoubleAppUser(userInfo)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFixModeFromUserId(int userId) {
        if (userId == 888) {
            return true;
        }
        return false;
    }

    public boolean isFixModeFromFlag(int flags) {
        if ((268435456 & flags) != 0) {
            return true;
        }
        return false;
    }

    public int getFixModeUserId() {
        return 888;
    }
}