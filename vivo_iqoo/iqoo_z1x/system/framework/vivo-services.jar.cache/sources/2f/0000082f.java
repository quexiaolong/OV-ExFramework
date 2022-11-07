package com.vivo.services.sensorhub;

import android.content.Context;
import android.os.IBinder;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;
import com.vivo.sensor.implement.VivoSensorImpl;

/* loaded from: classes.dex */
public class NotifyTransitionToUnderLight {
    private WindowManagerInternal.AppTransitionListener mAppTransitionListener = new WindowManagerInternal.AppTransitionListener() { // from class: com.vivo.services.sensorhub.NotifyTransitionToUnderLight.1
        public void onAppTransitionPendingLocked() {
        }

        public void onAppTransitionCancelledLocked(int transit) {
            NotifyTransitionToUnderLight.this.notifyTransitionToSensorService(1);
        }

        public void onAppTransitionTimeoutLocked() {
            NotifyTransitionToUnderLight.this.notifyTransitionToSensorService(1);
        }

        public void onAppTransitionFinishedLocked(IBinder token) {
            NotifyTransitionToUnderLight.this.notifyTransitionToSensorService(1);
        }

        public int onAppTransitionStartingLocked(int transit, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
            if (duration > 0) {
                NotifyTransitionToUnderLight.this.notifyTransitionToSensorService(0);
            }
            return 0;
        }
    };
    private VivoSensorImpl mVivoSensorImpl;

    public NotifyTransitionToUnderLight(Context context) {
        this.mVivoSensorImpl = null;
        this.mVivoSensorImpl = VivoSensorImpl.getInstance(context);
    }

    public void registerAppTransitionListener() {
        WindowManagerInternal windowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        windowManagerInternal.registerAppTransitionListener(this.mAppTransitionListener);
    }

    public void notifyTransitionToSensorService(int status) {
        VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
        if (vivoSensorImpl != null) {
            vivoSensorImpl.notifyTransition(status);
        }
    }
}