package com.android.server.wm;

import android.multidisplay.MultiDisplayManager;

/* loaded from: classes.dex */
public class VivoEnsureActivitiesVisibleHelperImpl implements IVivoEnsureActivitiesVisibleHelper {
    public boolean shouldKeepAwakeForVirtualDisplay(ActivityRecord r) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && MultiDisplayManager.isVivoDisplay(r.getDisplayId())) {
            DisplayContent displayContent = r.getDisplayContent();
            if (!displayContent.shouldSleep()) {
                ActivityStack stack = displayContent != null ? displayContent.getTopStack() : null;
                if (r.getStack() == stack) {
                    return true;
                }
            }
        }
        return false;
    }
}