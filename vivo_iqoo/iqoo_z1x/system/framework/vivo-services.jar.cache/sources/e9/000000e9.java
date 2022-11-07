package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.widget.Toast;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoPcbaCotrolImpl implements IVivoPcbaCotrol {
    private static final int MSG_SHOW_TOAST = 0;
    private static final String TAG = "VivoPcbaCotrolImpl";
    private static final String[] PCBA_PERMISSION = {"android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.READ_SMS", "android.permission.WRITE_SMS"};
    private static final String[] PCBA_PERMISSION_UID = {"android.permission.WRITE_CONTACTS", "android.permission.READ_SMS", "android.permission.WRITE_SMS"};
    private static final String[] PCBA_PACKAGE = {"com.android.mms", "com.android.contacts", "com.google.android.apps.messaging", "com.google.android.contacts"};
    public final boolean mIllegalPCBA = "0".equals(SystemProperties.get("ro.pcba.control", "1"));
    private Handler mToastHandler = null;
    private HandlerThread mHandlerThread = null;

    public boolean isIllegalPermisson(String permisson, String pkgName) {
        String[] strArr;
        if (this.mIllegalPCBA) {
            for (String name : PCBA_PERMISSION) {
                if (name.equals(permisson)) {
                    VLog.e(TAG, "Test Device, not allow " + permisson + " for pkg " + pkgName);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean isIllegalPermisson(String permisson, int userId) {
        String[] strArr;
        if (this.mIllegalPCBA) {
            for (String name : PCBA_PERMISSION_UID) {
                if (name.equals(permisson)) {
                    VLog.e(TAG, "Test Device, not allow " + permisson + " for userId " + userId);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean isIllegalPackage(Intent intent, Context iContext) {
        ComponentName comName;
        String[] strArr;
        if (!this.mIllegalPCBA || intent == null || iContext == null || (comName = intent.getComponent()) == null) {
            return false;
        }
        String pkgName = comName.getPackageName();
        for (String name : PCBA_PACKAGE) {
            if (name.equals(pkgName)) {
                VLog.e(TAG, "Test Device, not allow to start " + pkgName);
                showToast(iContext);
                return true;
            }
        }
        return false;
    }

    public void showToast(Context iContext) {
        if (this.mToastHandler == null) {
            VLog.i(TAG, "Test Device, new pcba_handler ");
            HandlerThread handlerThread = new HandlerThread("pcba_handler");
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            this.mToastHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.android.server.am.VivoPcbaCotrolImpl.1
                @Override // android.os.Handler
                public void handleMessage(Message msg) {
                    if (msg.what != 0 || msg.obj == null) {
                        return;
                    }
                    Toast.makeText((Context) msg.obj, 51249867, 0).show();
                }
            };
        }
        Handler handler = this.mToastHandler;
        if (handler != null) {
            Message msg = handler.obtainMessage(0);
            msg.obj = iContext;
            this.mToastHandler.sendMessage(msg);
        }
    }
}