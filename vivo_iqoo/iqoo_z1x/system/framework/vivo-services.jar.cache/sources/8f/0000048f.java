package com.android.server.power;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.multidisplay.MultiDisplayManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.view.Display;
import com.android.internal.telephony.ITelephony;
import com.android.server.policy.VivoPolicyConstant;

/* loaded from: classes.dex */
public class FaceWakeThread extends HandlerThread implements Handler.Callback {
    private boolean mBlocked;
    private Context mContext;
    private boolean mFaceWakeEnabled;
    private FaceWakeHandler mHandler;
    private int mScreenOffTimeout;

    public FaceWakeThread(String name, Context context) {
        super(name);
        this.mFaceWakeEnabled = false;
        this.mBlocked = false;
        this.mContext = context;
    }

    private boolean isScreenLocked() {
        KeyguardManager km = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (km != null) {
            return km.isKeyguardLocked();
        }
        return false;
    }

    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (pm != null) {
            return pm.isScreenOn();
        }
        return false;
    }

    private boolean isInCall() {
        getTelephonyService();
        return false;
    }

    static ITelephony getTelephonyService() {
        ITelephony telephonyService = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (telephonyService == null) {
            FaceWakeHandler.log("Unable to find ITelephony interface.");
        }
        return telephonyService;
    }

    private boolean isInSuperPowerSave() {
        return SystemProperties.getBoolean("sys.super_power_save", false) || SystemProperties.getBoolean(VivoPolicyConstant.KEY_DRIVING_MODE, false);
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        int i = msg.what;
        if (i == 1) {
            startFaceWakeInternal();
            return true;
        } else if (i == 2) {
            stopFaceWakeInternal();
            return true;
        } else if (i == 3) {
            resolveEnableSettings(msg);
            return true;
        } else if (i == 4) {
            resolveBlock(msg);
            return true;
        } else {
            return false;
        }
    }

    private void resolveEnableSettings(Message msg) {
        if (msg == null) {
            return;
        }
        int enable = msg.arg1;
        this.mFaceWakeEnabled = enable == 1;
        this.mScreenOffTimeout = msg.arg2;
        try {
            this.mHandler = (FaceWakeHandler) msg.getTarget();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FaceWakeHandler.log("resolveEnableSettings.... enabled: " + this.mFaceWakeEnabled + " timeout: " + this.mScreenOffTimeout);
    }

    private void resolveBlock(Message msg) {
        if (msg == null) {
            return;
        }
        int block = msg.arg1;
        this.mBlocked = block == 1;
        FaceWakeHandler.log("resolveBlock.... mBlocked: " + this.mBlocked);
    }

    private void startFaceWakeInternal() {
        FaceWakeHandler.log("startFaceWakeInternal....");
        if (!this.mFaceWakeEnabled) {
            FaceWakeHandler.log("face wake disabled, return....");
        } else if (this.mBlocked) {
            FaceWakeHandler.log("face wake blocked, return....");
        } else if (isInCall()) {
            FaceWakeHandler.log("is in call, return....");
        } else if (!isScreenOn()) {
            FaceWakeHandler.log("screen is off, return....");
        } else if (isScreenLocked()) {
            FaceWakeHandler.log("screen is locked, return");
        } else if (isInSuperPowerSave()) {
            FaceWakeHandler.log("in super power save mode, return");
        } else {
            DisplayManager mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
            Display display = mDisplayManager.getDisplay(4096);
            int mFocusedDisplayId = -1;
            if (display != null) {
                MultiDisplayManager mMultiDisplayManager = (MultiDisplayManager) this.mContext.getSystemService("multidisplay");
                mFocusedDisplayId = mMultiDisplayManager.getFocusedDisplayId();
            }
            if (display == null || (display != null && mFocusedDisplayId == 4096)) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.bbk.facewake", "com.bbk.facewake.FaceWakeService"));
                ComponentName cn = this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                if (cn == null) {
                    FaceWakeHandler.log("start face wake service faild.");
                }
            }
        }
    }

    private void stopFaceWakeInternal() {
        FaceWakeHandler.log("stopFaceWakeInternal....");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.bbk.facewake", "com.bbk.facewake.FaceWakeService"));
        this.mContext.stopServiceAsUser(intent, UserHandle.CURRENT);
    }
}