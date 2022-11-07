package com.android.server.usb;

import android.content.ContentResolver;
import android.content.Context;
import android.debug.AdbManagerInternal;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.server.LocalServices;
import com.android.server.usb.UsbDeviceManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUsbHandlerLegacyImpl implements IVivoUsbHandlerLegacy {
    private static final String MTK_DEBUG_PORT = "mtp,adb,acm";
    private static final String QCOM_DEBUG_PORT = "diag,serial_cdev,serial_tty,rmnet_ipa,mass_storage,adb";
    private boolean DEBUG = false;
    private String TAG = "VivoUsbDeviceManager";
    private ContentResolver mContentResolver;
    private Context mContext;
    protected boolean mIsDebugPort;
    private UsbDeviceManager.UsbHandlerLegacy mUsbHandlerLegacy;
    private IVivoUsbHandler mVivoUsbHandler;

    public VivoUsbHandlerLegacyImpl(UsbDeviceManager.UsbHandlerLegacy usbHandlerLegacy, Context context) {
        this.mUsbHandlerLegacy = usbHandlerLegacy;
        this.mVivoUsbHandler = usbHandlerLegacy.mVivoUsbHandler;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
    }

    public boolean initCurrentFunctions(String currentFunctionsStr, boolean currentFunctionsApplied, long currentFunctions) {
        String str = this.TAG;
        VSlog.d(str, "initCurrentFunctions mCurrentFunctions=" + currentFunctionsStr);
        return this.mVivoUsbHandler.initCurrentFunctions(this, currentFunctionsApplied, currentFunctions);
    }

    public void setEnabledFunctions(long usbFunctions, boolean forceRestart, boolean usbDataUnlocked) {
        String str = this.TAG;
        VSlog.d(str, "setEnabledFunctions(" + usbFunctions + ", " + forceRestart + ")");
        String str2 = this.TAG;
        VSlog.d(str2, "usbDataUnlocked=" + usbDataUnlocked + " ,mUsbDataUnlocked=" + usbDataUnlocked + " ,mCurrentFunctionsApplied=" + this.mUsbHandlerLegacy.mCurrentFunctionsApplied);
    }

    public void setToCharging() {
        if (TextUtils.isEmpty(this.mUsbHandlerLegacy.mCurrentFunctionsStr) || "none".equals(this.mUsbHandlerLegacy.mCurrentFunctionsStr) || "charging,adb".equals(this.mUsbHandlerLegacy.mCurrentFunctionsStr) || "charging".equals(this.mUsbHandlerLegacy.mCurrentFunctionsStr)) {
            return;
        }
        if (isAdbEnabled()) {
            this.mUsbHandlerLegacy.mCurrentFunctionsStr = "charging,adb";
        } else {
            this.mUsbHandlerLegacy.mCurrentFunctionsStr = "charging";
        }
        String str = this.TAG;
        VSlog.d(str, "setCurrentFunctionsStrToCharing mCurrentFunctionsStr=" + this.mUsbHandlerLegacy.mCurrentFunctionsStr);
    }

    protected String getSystemProperty(String prop, String def) {
        return SystemProperties.get(prop, def);
    }

    protected boolean isAdbEnabled() {
        return ((AdbManagerInternal) LocalServices.getService(AdbManagerInternal.class)).isAdbEnabled((byte) 0);
    }

    public void trySetEnabledFunctionsLog(long usbFunctions, boolean forceRestart) {
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("trySetEnabledFunctions(");
        sb.append(0 == usbFunctions ? "none" : UsbManager.usbFunctionsToString(usbFunctions));
        sb.append(", ");
        sb.append(forceRestart);
        sb.append(") isAdbEnabled()=");
        sb.append(isAdbEnabled());
        VSlog.d(str, sb.toString());
    }

    public boolean stopSetCurrentFunctions(long usbFunctions, boolean forceRestart, String functions) {
        boolean stop = this.mVivoUsbHandler.stopSetCurrentFunctions(usbFunctions, forceRestart, functions);
        String str = this.TAG;
        VSlog.w(str, "stopSetCurrentFunctions usbFunctions=" + usbFunctions + " ,forceRestart=" + forceRestart + " ,functions=" + functions);
        if (stop) {
            return true;
        }
        return false;
    }

    public String trySetEnabledFunctionsVivo(long usbFunctions, boolean forceRestart) {
        String func = this.mVivoUsbHandler.trySetEnabledFunctionsVivo(usbFunctions, forceRestart);
        String str = this.TAG;
        VSlog.d(str, "functions=" + func + " ,mCurrentFunctionsStr=" + this.mUsbHandlerLegacy.mCurrentFunctionsStr + " ,mCurrentFunctionsApplied=" + this.mUsbHandlerLegacy.mCurrentFunctionsApplied);
        return func;
    }

    public String applyAdbFunctionVivo(String functions) {
        return this.mVivoUsbHandler.applyAdbFunctionVivo(functions);
    }

    public void dummy() {
    }
}