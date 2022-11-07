package com.android.server.wm;

import com.android.server.LocalServices;
import com.android.server.input.IVivoWindowManagerCallbacks;
import com.android.server.policy.InputExceptionReport;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoInputWindowCallbacksImpl implements IVivoWindowManagerCallbacks {
    private String mInputFreezeReason;
    private WindowManagerService mService;

    public VivoInputWindowCallbacksImpl(WindowManagerService service) {
        this.mService = service;
    }

    public boolean shouldDisablePilferPointers(String opPackageName) {
        return this.mService.mPolicy.shouldDisablePilferPointers(opPackageName);
    }

    public void notifyInputFrozenTimeout() {
        InputExceptionReport.getInstance().reportInputFrozenTimeout(this.mInputFreezeReason);
        if (((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).forceStopFreezingEnabled()) {
            this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoInputWindowCallbacksImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (VivoInputWindowCallbacksImpl.this.mService.mGlobalLock) {
                        try {
                            VivoInputWindowCallbacksImpl.this.mService.mWindowPlacerLocked.performSurfacePlacement();
                            VivoInputWindowCallbacksImpl.this.mService.stopFreezingDisplayLocked(true);
                        } catch (Exception e) {
                            VSlog.d("WindowManager", "notifyInputFrozenTimeout cause exception :" + e);
                        }
                    }
                }
            });
        }
    }

    public void setInputFreezeReason(String reason) {
        this.mInputFreezeReason = reason;
    }
}