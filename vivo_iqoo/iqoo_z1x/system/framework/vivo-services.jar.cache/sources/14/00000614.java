package com.vivo.services.autorecover;

import android.app.IActivityController;
import android.view.KeyEvent;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.WindowState;

/* loaded from: classes.dex */
public abstract class SystemAutoRecoverManagerInternal {
    public abstract boolean forceStopFreezingEnabled();

    public abstract void notifyAppDied(int i, int i2);

    public abstract void notifyPasswordMode(boolean z);

    public abstract void notifyShutDown();

    public abstract void onScreenTurnOff();

    public abstract void reportBackKey(KeyEvent keyEvent, KeyInterceptionInfo keyInterceptionInfo);

    public abstract void requestCheckForInputException(WindowState windowState, boolean z);

    public abstract void setActivityController(IActivityController iActivityController, boolean z);

    public abstract void setFocusedApps(int i, ActivityRecord activityRecord, ActivityRecord activityRecord2);

    public abstract void setFocusedWindow(int i, WindowState windowState, WindowState windowState2);

    public abstract void setUserIsMonkey(boolean z);
}