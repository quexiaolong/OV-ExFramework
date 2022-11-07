package com.android.server.display.color;

import android.os.Build;
import java.io.File;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class ExynosDisplayPanel {
    private static final String TAG = "ExynosDisplayPanel";
    private final boolean DEBUG = "eng".equals(Build.TYPE);
    private String PANEL_CABC_MODE_PATH = "/sys/class/panel/panel/cabc_mode";
    private String PANEL0_CABC_MODE_PATH = "/sys/devices/platform/panel_0/cabc_mode";
    private String CABC_MODE_VALUE = null;
    private String PANEL0_HBM_MODE_PATH = "/sys/devices/platform/panel_0/hbm_mode";
    private String HBM_MODE_VALUE = null;

    private boolean existPanelFile(String file_path) {
        File file = new File(file_path);
        return file.exists() && file.isFile();
    }

    public void setCABCMode(int value) {
        String cabc_path = null;
        try {
            if (existPanelFile(this.PANEL_CABC_MODE_PATH)) {
                this.CABC_MODE_VALUE = ExynosDisplayUtils.getStringFromFile(this.PANEL_CABC_MODE_PATH);
                cabc_path = this.PANEL_CABC_MODE_PATH;
            } else if (existPanelFile(this.PANEL0_CABC_MODE_PATH)) {
                this.CABC_MODE_VALUE = ExynosDisplayUtils.getStringFromFile(this.PANEL0_CABC_MODE_PATH);
                cabc_path = this.PANEL0_CABC_MODE_PATH;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String str = this.CABC_MODE_VALUE;
        if (str != null && !str.equals("0")) {
            VSlog.d(TAG, "setCABCMode(): value = " + value);
            ExynosDisplayUtils.sysfsWrite(cabc_path, value);
        }
    }

    public void setHBMMode(int value) {
        try {
            this.HBM_MODE_VALUE = ExynosDisplayUtils.getStringFromFile(this.PANEL0_HBM_MODE_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String str = this.HBM_MODE_VALUE;
        if (str != null && !str.equals("0")) {
            VSlog.d(TAG, "setHBMMode(): value = " + value);
            ExynosDisplayUtils.sysfsWrite(this.PANEL0_HBM_MODE_PATH, value);
        }
    }
}