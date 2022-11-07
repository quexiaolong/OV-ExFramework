package com.vivo.services.popupcamera;

import android.content.ContentValues;
import vivo.app.epm.ExceptionPolicyManager;

/* loaded from: classes.dex */
public class EPMReporter {
    private static final int EXCEPTION_TYPE_POPUP_FRONT_CAMERA = 9;
    private static final String KEY_PN = "pn";
    private static final String KEY_SUBT = "subt";
    public static final int TYPE_INVALID = 0;
    public static final int TYPE_PERMISSION_DENY = 3;
    public static final int TYPE_POPUP_JAMMED = 1;
    public static final int TYPE_PUSH_JAMMED = 2;
    private static EPMReporter sInstance;
    private ExceptionPolicyManager mEPM = ExceptionPolicyManager.getInstance();

    private EPMReporter() {
    }

    public static synchronized EPMReporter getInstance() {
        EPMReporter ePMReporter;
        synchronized (EPMReporter.class) {
            if (sInstance == null) {
                sInstance = new EPMReporter();
            }
            ePMReporter = sInstance;
        }
        return ePMReporter;
    }

    private void report2EPM(CameraStatus status, int type) {
        ContentValues content = new ContentValues();
        content.put(KEY_SUBT, Integer.valueOf(type));
        content.put(KEY_PN, status != null ? status.currentStatusPackageName : "uknow");
        this.mEPM.reportEvent(9, System.currentTimeMillis(), content);
    }

    public void reportPopupJammed(CameraStatus status) {
        report2EPM(status, 1);
    }

    public void reportPushJammed(CameraStatus status) {
        report2EPM(status, 2);
    }

    public void reportPermissionDeny(CameraStatus status) {
        report2EPM(status, 3);
    }
}