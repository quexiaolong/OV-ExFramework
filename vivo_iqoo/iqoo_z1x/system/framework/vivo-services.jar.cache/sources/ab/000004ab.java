package com.android.server.usb;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.VivoPolicyManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import com.android.server.LocalServices;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUsbCustomization {
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    private static final String TAG = "VivoUsbCustomization";
    private ContentResolver mContentResolver;
    private Context mContext;
    private DevicePolicyManager mDevicePolicyManager;
    private VivoUsbHandlerImpl mHandler;
    private int mUsbTransferMode = 0;
    private int mUsbAdbMode = 0;
    private int mUsbRndisMode = 0;
    private VivoPolicyManagerInternal mVivoPolicyManagerInternal = (VivoPolicyManagerInternal) LocalServices.getService(VivoPolicyManagerInternal.class);

    public VivoUsbCustomization(ContentResolver contentResolver, VivoUsbHandlerImpl usbHandler, Context context) {
        this.mContentResolver = contentResolver;
        this.mHandler = usbHandler;
        this.mContext = context;
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        initCustomUsbConfig();
    }

    private void initCustomUsbConfig() {
        updateUsbTransferMode();
        updateUsbRndisMode();
        this.mVivoPolicyManagerInternal.setVivoPolicyListener(new VivoPolicyManagerInternal.VivoPolicyListener() { // from class: com.android.server.usb.VivoUsbCustomization.1
            public void onVivoPolicyChanged(int poId) {
                if (poId == 0 || poId == 9) {
                    VivoUsbCustomization.this.updateUsbTransferMode();
                    VivoUsbCustomization.this.scheduleSetCurruntUsbTransferMode();
                }
                if (poId == 0 || poId == 11) {
                    VivoUsbCustomization.this.updateUsbRndisMode();
                }
            }
        });
    }

    public void updateUsbTransferMode() {
        this.mUsbTransferMode = this.mDevicePolicyManager.getRestrictionPolicy(null, 9, ActivityManager.getCurrentUser());
        VSlog.i(TAG, "updateUsbTransferMode mUsbTransferMode=" + this.mUsbTransferMode);
    }

    public void updateUsbRndisMode() {
        this.mUsbRndisMode = this.mDevicePolicyManager.getRestrictionPolicy(null, 11, ActivityManager.getCurrentUser());
        VSlog.i(TAG, "updateUsbRndisMode mUsbRndisMode=" + this.mUsbRndisMode);
    }

    public void scheduleSetCurruntUsbTransferMode() {
        long usbTransferfunction = 0;
        if (allowUsbTransfer()) {
            usbTransferfunction = 0;
        } else if (forceCharging()) {
            usbTransferfunction = 0;
        } else if (forceMtp()) {
            usbTransferfunction = 4;
        } else if (forcePtp()) {
            usbTransferfunction = 16;
        }
        this.mHandler.sendMessage(2, Long.valueOf(usbTransferfunction));
    }

    public boolean isVivoCustomized() {
        return this.mDevicePolicyManager.getCustomType() > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allowUsbTransfer() {
        return this.mUsbTransferMode == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allowSetRndis(long functions) {
        if (functions == 32 && this.mUsbRndisMode == 1) {
            VSlog.w(TAG, "allowSetRndis false");
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String applyAdbFunctionVivo(String functions) {
        if (this.mUsbAdbMode == 1) {
            if (functions != null && functions.contains("adb")) {
                String functions2 = removeFunction(functions, "adb");
                VSlog.w(TAG, "Device is not allow usb adb.  functions:" + functions2);
                return functions2;
            }
            return functions;
        }
        return functions;
    }

    private static String removeFunction(String functions, String function) {
        String[] split = functions.split(",");
        for (int i = 0; i < split.length; i++) {
            if (function.equals(split[i])) {
                split[i] = null;
            }
        }
        int i2 = split.length;
        if (i2 == 1 && split[0] == null) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (String s : split) {
            if (s != null) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(s);
            }
        }
        return builder.toString();
    }

    boolean forceCharging() {
        return this.mUsbTransferMode == 1;
    }

    boolean forceMtp() {
        return this.mUsbTransferMode == 10;
    }

    boolean forcePtp() {
        return this.mUsbTransferMode == 11;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getVivoUsbCusFunction(String functions) {
        if (forceCharging()) {
            return "charging";
        }
        if (forceMtp()) {
            return "mtp";
        }
        if (forcePtp()) {
            return "ptp";
        }
        return functions;
    }
}