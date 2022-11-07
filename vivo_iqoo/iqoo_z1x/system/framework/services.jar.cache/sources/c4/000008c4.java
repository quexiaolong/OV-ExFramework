package com.android.server.audio;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.AudioSystem;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RotationHelper {
    private static final String TAG = "AudioService.RotationHelper";
    private static Context sContext;
    private static AudioDisplayListener sDisplayListener;
    private static Handler sHandler;
    private static final Object sRotationLock = new Object();
    private static int sDeviceRotation = 0;

    RotationHelper() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void init(Context context, Handler handler) {
        if (context == null) {
            throw new IllegalArgumentException("Invalid null context");
        }
        sContext = context;
        sHandler = handler;
        sDisplayListener = new AudioDisplayListener();
        enable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enable() {
        ((DisplayManager) sContext.getSystemService("display")).registerDisplayListener(sDisplayListener, sHandler);
        updateOrientation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void disable() {
        ((DisplayManager) sContext.getSystemService("display")).unregisterDisplayListener(sDisplayListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void updateOrientation() {
        int newRotation = ((WindowManager) sContext.getSystemService("window")).getDefaultDisplay().getRotation();
        synchronized (sRotationLock) {
            if (newRotation != sDeviceRotation) {
                sDeviceRotation = newRotation;
                publishRotation(newRotation);
            }
        }
    }

    private static void publishRotation(int rotation) {
        Log.v(TAG, "publishing device rotation =" + rotation + " (x90deg)");
        if (rotation == 0) {
            AudioSystem.setParameters("rotation=0");
        } else if (rotation == 1) {
            AudioSystem.setParameters("rotation=90");
        } else if (rotation == 2) {
            AudioSystem.setParameters("rotation=180");
        } else if (rotation == 3) {
            AudioSystem.setParameters("rotation=270");
        } else {
            Log.e(TAG, "Unknown device rotation");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AudioDisplayListener implements DisplayManager.DisplayListener {
        AudioDisplayListener() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            RotationHelper.updateOrientation();
        }
    }
}