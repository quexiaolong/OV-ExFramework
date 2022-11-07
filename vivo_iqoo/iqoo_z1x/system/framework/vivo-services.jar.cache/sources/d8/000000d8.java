package com.android.server.am;

import com.android.server.LocalServices;
import com.vivo.common.utils.VLog;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import com.vivo.services.autorecover.SystemAutoRecoverService;

/* loaded from: classes.dex */
public class VivoAppExitInfoTrackerImpl implements IVivoAppExitInfoTracker {
    public void notifyAppDied(int pid, int reason) {
        try {
            ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).notifyAppDied(pid, reason);
        } catch (Exception e) {
            VLog.d(SystemAutoRecoverService.TAG, "notifyAppDied cause exception:" + e);
        }
    }
}