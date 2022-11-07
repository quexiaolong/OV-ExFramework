package com.android.server.pm;

import android.util.SparseArray;
import com.android.server.pm.UserManagerService;

/* loaded from: classes.dex */
public interface IVivoUms {
    int getDoubleAppFixedUserId();

    int getDoubleAppUserId(SparseArray<UserManagerService.UserData> sparseArray);

    int getFixModeUserId();

    boolean isDoubleAppUserExist(SparseArray<UserManagerService.UserData> sparseArray);

    boolean isDoubleAppUserFlag(int i);

    boolean isFixModeFromFlag(int i);

    boolean isFixModeFromUserId(int i);

    boolean judgeConditionsCreatDoubleInstance();
}