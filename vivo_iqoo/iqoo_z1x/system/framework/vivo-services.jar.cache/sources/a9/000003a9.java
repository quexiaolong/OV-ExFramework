package com.android.server.pm;

import android.content.pm.UserInfo;
import com.android.server.VivoDoubleInstanceServiceImpl;

/* loaded from: classes.dex */
public class VivoSettingsImpl implements IVivoSettings {
    static final String TAG = "VivoSettingsImpl";
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();

    public boolean isDoubleInstanceUser(int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && userId == 999) {
            return true;
        }
        return false;
    }

    public boolean isDoubleInstanceUserInfo(UserInfo user) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable()) {
            return this.mVivoDoubleInstanceService.isDoubleAppUser(user);
        }
        return false;
    }

    public boolean isDoubleInstanceEnable() {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null) {
            return vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable();
        }
        return false;
    }
}