package com.vivo.services.vivolight;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.view.Display;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class DisplayListenerUtil {
    private final Context mContext;
    private SensorUtil mSensorUtil;
    private VivoLightManagerService mService;
    private int lastState = 0;
    private DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.vivo.services.vivolight.DisplayListenerUtil.1
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            DisplayManager displayManager;
            if (displayId != 0 || (displayManager = (DisplayManager) DisplayListenerUtil.this.mContext.getSystemService("display")) == null) {
                return;
            }
            Display display = displayManager.getDisplay(displayId);
            int currentState = display.getState();
            VLog.d(VivoLightManagerService.TAG, "displayState change lastState=" + DisplayListenerUtil.this.lastState + " currentState=" + currentState);
            if (DisplayListenerUtil.this.lastState == 2 || currentState == 2) {
                DisplayListenerUtil.this.lastState = currentState;
                if (currentState == 2) {
                    if (DisplayListenerUtil.this.mSensorUtil != null) {
                        DisplayListenerUtil.this.mSensorUtil.unregisterProximitySensorListener();
                    }
                    DisplayListenerUtil.this.mService.setCurrentState(DisplayListenerUtil.this.mService.getCurrentState() & (-2));
                } else {
                    if (DisplayListenerUtil.this.mSensorUtil == null) {
                        DisplayListenerUtil displayListenerUtil = DisplayListenerUtil.this;
                        displayListenerUtil.mSensorUtil = new SensorUtil(displayListenerUtil.mContext, DisplayListenerUtil.this.mService);
                    }
                    DisplayListenerUtil.this.mSensorUtil.registerProximitySensorListener();
                    DisplayListenerUtil.this.mService.setCurrentState(DisplayListenerUtil.this.mService.getCurrentState() | 1);
                }
                VLog.d(VivoLightManagerService.TAG, "onDisplayChanged notifyUpdateLight");
                DisplayListenerUtil.this.mService.notifyUpdateLight();
                return;
            }
            DisplayListenerUtil.this.lastState = currentState;
        }
    };

    public DisplayListenerUtil(Context context, VivoLightManagerService service) {
        this.mContext = context;
        this.mService = service;
    }

    public void register(Handler handler) {
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        if (displayManager != null) {
            displayManager.registerDisplayListener(this.mDisplayListener, handler);
        } else {
            VLog.w(VivoLightManagerService.TAG, "vivoDisplayStateManager is null");
        }
    }
}