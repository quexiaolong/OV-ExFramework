package com.vivo.services.popupcamera;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import android.widget.Button;
import com.vivo.common.utils.VLog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class FrontCameraTemperatureProtectDialog extends BaseErrorDialog {
    private static final int MAX_COUNTDOWN_TIMES = 5;
    private static final int MSG_FORCE_CLOSE = 1;
    private static final int MSG_UPDATE_COUNTDOWN_TIMES = 2;
    private static final String TAG = "PopupCameraManagerService";
    private Context context;
    private int mCurrentCountDown;
    private final Handler mHandler;
    private String mMessage;
    private String positiveTips;

    static /* synthetic */ int access$106(FrontCameraTemperatureProtectDialog x0) {
        int i = x0.mCurrentCountDown - 1;
        x0.mCurrentCountDown = i;
        return i;
    }

    public FrontCameraTemperatureProtectDialog(Context context, boolean aboveSystem, String message, String positiveTips) {
        super(context);
        this.mCurrentCountDown = 5;
        this.mHandler = new Handler() { // from class: com.vivo.services.popupcamera.FrontCameraTemperatureProtectDialog.3
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i == 1) {
                    FrontCameraTemperatureProtectDialog.this.dismiss();
                } else if (i == 2) {
                    if (FrontCameraTemperatureProtectDialog.this.mCurrentCountDown > 0) {
                        Button button = FrontCameraTemperatureProtectDialog.this.getButton(-1);
                        button.setText(FrontCameraTemperatureProtectDialog.this.positiveTips + " (" + FrontCameraTemperatureProtectDialog.this.mCurrentCountDown + FrontCameraTemperatureProtectDialog.this.context.getString(51249505) + ")");
                    } else {
                        FrontCameraTemperatureProtectDialog.this.getButton(-1).setEnabled(true);
                        FrontCameraTemperatureProtectDialog.this.getButton(-1).setText(FrontCameraTemperatureProtectDialog.this.positiveTips);
                    }
                    FrontCameraTemperatureProtectDialog.access$106(FrontCameraTemperatureProtectDialog.this);
                    if (FrontCameraTemperatureProtectDialog.this.mCurrentCountDown >= 0) {
                        sendEmptyMessageDelayed(2, 1000L);
                    }
                }
            }
        };
        this.context = context;
        context.getResources();
        setCancelable(false);
        setMessage(message);
        this.mMessage = message;
        this.positiveTips = positiveTips;
        setButton(-1, positiveTips, this.mHandler.obtainMessage(1));
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = 272;
        getWindow().setAttributes(attrs);
        setOnShowListener(new DialogInterface.OnShowListener() { // from class: com.vivo.services.popupcamera.FrontCameraTemperatureProtectDialog.1
            @Override // android.content.DialogInterface.OnShowListener
            public void onShow(DialogInterface dialogInterface) {
                FrontCameraTemperatureProtectDialog.this.getButton(-1).setEnabled(false);
                if (FrontCameraTemperatureProtectDialog.this.mHandler.hasMessages(2)) {
                    FrontCameraTemperatureProtectDialog.this.mHandler.removeMessages(2);
                    VLog.d(FrontCameraTemperatureProtectDialog.TAG, "onShow ,first remove existed MSG_UPDATE_COUNTDOWN_TIMES");
                }
                FrontCameraTemperatureProtectDialog.this.mHandler.sendEmptyMessage(2);
            }
        });
        setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.vivo.services.popupcamera.FrontCameraTemperatureProtectDialog.2
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                FrontCameraTemperatureProtectDialog.this.mCurrentCountDown = 5;
                if (FrontCameraTemperatureProtectDialog.this.mHandler.hasMessages(2)) {
                    FrontCameraTemperatureProtectDialog.this.mHandler.removeMessages(2);
                    VLog.d(FrontCameraTemperatureProtectDialog.TAG, "onDismiss ,remove MSG_UPDATE_COUNTDOWN_TIMES");
                }
            }
        });
    }

    @Override // android.app.AlertDialog, android.app.Dialog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public boolean isAllowToDismiss() {
        return isShowing();
    }
}