package com.android.server.usb;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.multidisplay.MultiDisplayManager;
import android.os.Binder;
import android.os.Debug;
import com.android.server.usb.UsbDeviceManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUsbDeviceManagerImpl implements IVivoUsbDeviceManager {
    private boolean DEBUG = false;
    private String TAG = "VivoUsbDeviceManager";
    private ContentResolver mContentResolver;
    private Context mContext;
    private UsbDeviceManager mUsbDeviceManager;

    public VivoUsbDeviceManagerImpl(UsbDeviceManager usbDeviceManager, Context context) {
        this.mUsbDeviceManager = usbDeviceManager;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
    }

    public boolean onUnlockUser(int userHandle) {
        if (this.DEBUG) {
            String str = this.TAG;
            VSlog.v(str, "onUnlockUser userHandle=" + userHandle);
            return true;
        }
        return true;
    }

    public void setCurrentFunctions(long functions, Object usbHandler) {
        UsbDeviceManager.UsbHandler uh = (UsbDeviceManager.UsbHandler) usbHandler;
        String str = this.TAG;
        VSlog.d(str, "mCurrentFunctions=" + uh.mCurrentFunctions + " ,callingPid=" + Binder.getCallingPid() + " Callers=" + Debug.getCallers(10));
        if (functions == 4) {
            uh.mVivoUsbHandler.setDebugPort(false);
        }
    }

    public void updateAccessory4CarNetworking() {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return;
        }
        if (UsbManager.sDebugUsb) {
            VSlog.d("VivoAccessory", "updateAccessory4CarNetworking via manager");
        }
        this.mUsbDeviceManager.mHandler.obtainMessage(101).sendToTarget();
    }

    public void monitorEnabled4CarNetworking(boolean enable) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return;
        }
        if (UsbManager.sDebugUsb) {
            VSlog.d("VivoAccessory", "monitorEnabled4CarNetworking via manager enable = " + enable);
        }
        this.mUsbDeviceManager.mHandler.obtainMessage(100, enable ? 1 : 0, 0).sendToTarget();
    }

    public void dummy() {
    }
}