package com.android.server.display;

import android.content.Context;
import android.hardware.display.VivoDisplayStateInternal;
import android.os.Handler;
import android.view.SurfaceControl;
import com.android.server.LocalServices;
import com.android.server.policy.WindowManagerPolicy;
import com.vivo.face.common.notification.FaceSystemNotify;
import com.vivo.sensor.implement.SensorConfig;
import vivo.app.VivoFrameworkFactory;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLocalDisplayAdapterImpl implements IVivoLocalDisplayAdapter {
    private static final long BUILT_IN_DISPLAY_ID_QMAIN;
    private static final String TAG = "VivoLocalDisplayAdapter";
    private static final long[] physicalDisplayIds;
    private FaceSystemNotify mFaceSystemNotify;
    private LocalDisplayAdapter mLocalDisplayAdapter;
    private Handler mLocalDisplayHandler;
    private VivoDisplayStateInternal mVivoDisplayStateInternal;
    private WindowManagerPolicy mWindowManagerPolicy;

    static {
        long[] physicalDisplayIds2 = SurfaceControl.getPhysicalDisplayIds();
        physicalDisplayIds = physicalDisplayIds2;
        BUILT_IN_DISPLAY_ID_QMAIN = physicalDisplayIds2[0];
    }

    public VivoLocalDisplayAdapterImpl(Context context, Handler handler, LocalDisplayAdapter localDisplayAdapter) {
        this.mLocalDisplayHandler = handler;
        this.mLocalDisplayAdapter = localDisplayAdapter;
    }

    public void notifyDisplayStateBeginChange(int state, float backlight, long physicalDisplayId, boolean forceStateOn, boolean forceOnNoChangeVDSS) {
        int realState = forceStateOn ? 2 : state;
        int mappedBacklight = SensorConfig.float2LcmBrightnessAfterDPC(backlight);
        int displayId = BUILT_IN_DISPLAY_ID_QMAIN == physicalDisplayId ? 0 : 4096;
        if (this.mVivoDisplayStateInternal == null) {
            this.mVivoDisplayStateInternal = (VivoDisplayStateInternal) LocalServices.getService(VivoDisplayStateInternal.class);
        }
        VivoDisplayStateInternal vivoDisplayStateInternal = this.mVivoDisplayStateInternal;
        if (vivoDisplayStateInternal != null) {
            vivoDisplayStateInternal.onForceNoChangeVDSSChange(displayId, forceOnNoChangeVDSS);
            this.mVivoDisplayStateInternal.onBacklightStateBeginChange(displayId, realState, mappedBacklight);
        } else {
            VSlog.w(TAG, "notifyDisplayStateBeginChange failed/vivo display state internal is invalid");
        }
        if (this.mFaceSystemNotify == null) {
            this.mFaceSystemNotify = FaceSystemNotify.getInstance();
        }
        FaceSystemNotify faceSystemNotify = this.mFaceSystemNotify;
        if (faceSystemNotify != null) {
            faceSystemNotify.onDisplayStateChanged(displayId, realState, mappedBacklight);
        } else {
            VSlog.w(TAG, "notify face service proxy failed");
        }
    }

    public void notifyDisplayStateChanged(int state, float backlight, final long physicalDisplayId, boolean forceStateOn) {
        final int realState = forceStateOn ? 2 : state;
        final int mappedBacklight = SensorConfig.float2LcmBrightnessAfterDPC(backlight);
        int displayId = BUILT_IN_DISPLAY_ID_QMAIN == physicalDisplayId ? 0 : 4096;
        if (this.mVivoDisplayStateInternal == null) {
            this.mVivoDisplayStateInternal = (VivoDisplayStateInternal) LocalServices.getService(VivoDisplayStateInternal.class);
        }
        if (this.mWindowManagerPolicy == null) {
            this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        }
        VivoDisplayStateInternal vivoDisplayStateInternal = this.mVivoDisplayStateInternal;
        if (vivoDisplayStateInternal != null) {
            vivoDisplayStateInternal.onBacklightStateChanged(displayId, realState, mappedBacklight);
        } else {
            VSlog.w(TAG, "notifyDisplayStateChanged failed/vivo display state internal is invalid");
        }
        WindowManagerPolicy windowManagerPolicy = this.mWindowManagerPolicy;
        if (windowManagerPolicy != null) {
            windowManagerPolicy.onBacklightStateChanged(displayId, realState, mappedBacklight);
        } else {
            VSlog.w(TAG, "notifyDisplayStateChanged failed/WindowManagerPolicy is invalid");
        }
        Handler handler = this.mLocalDisplayHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.display.VivoLocalDisplayAdapterImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoLocalDisplayAdapterImpl.this.notifyDisplayStateChangedInternal(realState, mappedBacklight, physicalDisplayId);
                }
            });
        } else {
            VSlog.w(TAG, "notifyDisplayStateChanged failed/invalid local display handler");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDisplayStateChangedInternal(int state, int backlight, long physicalDisplayId) {
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            int displayId = BUILT_IN_DISPLAY_ID_QMAIN == physicalDisplayId ? 0 : 4096;
            VivoFrameworkFactory.getFrameworkFactoryImpl().getTouchScreenManager().TouchscreenLcdBacklightStateSet(displayId, state, backlight);
        }
    }
}