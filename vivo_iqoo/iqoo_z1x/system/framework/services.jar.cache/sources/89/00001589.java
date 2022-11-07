package com.android.server.pm;

import android.content.pm.UserInfo;

/* loaded from: classes.dex */
public interface IVivoSettings {
    boolean isDoubleInstanceEnable();

    boolean isDoubleInstanceUser(int i);

    boolean isDoubleInstanceUserInfo(UserInfo userInfo);
}