package com.vivo.services.popupcamera;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

/* loaded from: classes.dex */
final class FrontCameraDropProtectDialog extends BaseErrorDialog {
    private static final int MSG_CANCEL = 2;
    private static final int MSG_SURE = 1;
    private static final String TAG = "FrontCameraDropProtectDialog";
    private final Handler mHandler;
    private PopupCameraManagerService mService;

    public FrontCameraDropProtectDialog(Context context, PopupCameraManagerService service) {
        super(context);
        this.mHandler = new Handler() { // from class: com.vivo.services.popupcamera.FrontCameraDropProtectDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i == 1) {
                    FrontCameraDropProtectDialog.this.dismiss();
                    if (FrontCameraDropProtectDialog.this.mService != null && FrontCameraDropProtectDialog.this.mService.isCurrentFrontCameraOpened()) {
                        FrontCameraDropProtectDialog.this.mService.popupFrontCamera();
                    }
                } else if (i == 2) {
                    FrontCameraDropProtectDialog.this.dismiss();
                    if (FrontCameraDropProtectDialog.this.mService != null) {
                        FrontCameraDropProtectDialog.this.mService.emulatePressHomeKey();
                    }
                }
            }
        };
        this.mService = service;
        String title = context.getString(51249508);
        String message = context.getString(51249506);
        String popupText = context.getString(51249507);
        String exitText = context.getString(51249278);
        setCancelable(true);
        setMessage(message);
        setTitle(title);
        setButton(-1, popupText, this.mHandler.obtainMessage(1));
        setButton(-2, exitText, this.mHandler.obtainMessage(2));
        getWindow().setType(2010);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = 272;
        getWindow().setAttributes(attrs);
    }

    @Override // android.app.AlertDialog, android.app.Dialog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}