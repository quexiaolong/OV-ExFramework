package com.android.server.wm;

/* loaded from: classes.dex */
public class VivoResetTargetTaskHelperImpl implements IVivoResetTargetTaskHelper {
    public boolean checkUseridForDoubleInstance(int targetUserid, int uerid) {
        return targetUserid == uerid;
    }
}