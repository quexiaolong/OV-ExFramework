package com.vivo.services.popupcamera;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
final class FrontCameraJammedConfirmDialog extends BaseErrorDialog {
    private static final int ACTION_NO = 2;
    private static final int ACTION_YES = 1;
    private static final String TAG = "PopupCameraManagerService";
    private final Handler mHandler;

    public FrontCameraJammedConfirmDialog(Context context) {
        super(context);
        this.mHandler = new Handler() { // from class: com.vivo.services.popupcamera.FrontCameraJammedConfirmDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                FrontCameraJammedConfirmDialog.this.dismiss();
                int event = -1;
                int i = msg.what;
                if (i == 1) {
                    event = 1;
                } else if (i == 2) {
                    event = 2;
                }
                if (event != -1) {
                    int ret = VibHallWrapper.notifyEvent(event);
                    VLog.d(FrontCameraJammedConfirmDialog.TAG, "notifyEvent ret=" + ret);
                    if (event == 1 && ret == 0) {
                        PopupCameraManagerService.getInstance(null).notifyCalibrationResult(true);
                    }
                }
            }
        };
        context.getResources();
        String message = context.getString(51249512);
        setCancelable(true);
        setMessage(message);
        setButton(-1, context.getString(17040340), this.mHandler.obtainMessage(1));
        setButton(-2, context.getString(17040339), this.mHandler.obtainMessage(2));
        getWindow().setType(2010);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = 16;
        getWindow().setAttributes(attrs);
    }

    @Override // android.app.AlertDialog, android.app.Dialog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}