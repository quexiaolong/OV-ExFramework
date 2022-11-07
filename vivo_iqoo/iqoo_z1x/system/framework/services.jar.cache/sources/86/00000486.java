package com.android.server;

import android.content.pm.UserInfo;

/* loaded from: classes.dex */
public interface IVivoDoubleInstanceService {
    int getDoubleAppFixedUserId();

    int getDoubleAppUserId();

    boolean isDoubleAppUser(UserInfo userInfo);

    boolean isDoubleAppUserExist();

    boolean isDoubleInstanceDebugEnable();

    boolean isDoubleInstanceEnable();
}