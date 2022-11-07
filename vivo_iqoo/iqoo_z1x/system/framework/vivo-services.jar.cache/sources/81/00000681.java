package com.vivo.services.popupcamera;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

/* loaded from: classes.dex */
final class FrontCameraPressedDialog extends BaseErrorDialog {
    private static final int MSG_CANCEL = 2;
    private static final int MSG_SURE = 1;
    private static final String TAG = "FrontCameraPressedDialog";
    private final Handler mHandler;
    private PopupCameraManagerService mService;

    public FrontCameraPressedDialog(Context context, boolean aboveSystem, String message, PopupCameraManagerService service) {
        super(context);
        this.mHandler = new Handler() { // from class: com.vivo.services.popupcamera.FrontCameraPressedDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == 1 && FrontCameraPressedDialog.this.mService != null && FrontCameraPressedDialog.this.mService.isCurrentFrontCameraOpened()) {
                    FrontCameraPressedDialog.this.mService.popupFrontCamera();
                }
                FrontCameraPressedDialog.this.dismiss();
            }
        };
        this.mService = service;
        Resources res = context.getResources();
        setCancelable(true);
        setMessage(message);
        setButton(-1, res.getText(17039370), this.mHandler.obtainMessage(1));
        setButton(-2, res.getText(17039360), this.mHandler.obtainMessage(2));
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = 272;
        getWindow().setAttributes(attrs);
    }

    @Override // android.app.AlertDialog, android.app.Dialog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}