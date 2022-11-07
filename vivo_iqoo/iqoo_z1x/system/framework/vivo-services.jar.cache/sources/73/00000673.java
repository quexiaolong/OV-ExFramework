package com.vivo.services.popupcamera;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.FtBuild;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.vivo.common.utils.VLog;
import com.vivo.services.popupcamera.PopupFrontCameraPermissionHelper;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class CameraPopupPermissionCheckDialog extends BaseErrorDialog {
    public static final int MAX_COUNTDOWN_TIMES = 20;
    private static final int MSG_DENY_PERMISSION = 1;
    private static final int MSG_GRANT_PERMISSION = 0;
    private static final int MSG_UPDATE_COUNTDOWN_TIMES = 2;
    private static final String TAG = "PopupCameraManagerService";
    private TextView confirmMessage;
    private Context context;
    private String denyPermTips;
    private String grantPermTips;
    private TextView hintMessage;
    private volatile boolean isCanceledByFrontCameraCloseTask;
    private volatile boolean isGrantedToUser;
    private volatile boolean isPermissionConfirmedByUser;
    private volatile boolean isRememberCheckBoxChecked;
    private volatile boolean isUserConfirmTimeout;
    private int mCurrentCountDown;
    private final Handler mHandler;
    private String mMessage;
    private PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState permissionState;
    private CheckBox rememberCheckBox;

    static /* synthetic */ int access$206(CameraPopupPermissionCheckDialog x0) {
        int i = x0.mCurrentCountDown - 1;
        x0.mCurrentCountDown = i;
        return i;
    }

    public CameraPopupPermissionCheckDialog(Context context, boolean aboveSystem, String message, String grantPermTips, String denyPermTips, boolean isShowRememberCheckbox, PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState state) {
        super(context);
        this.mCurrentCountDown = 20;
        this.isPermissionConfirmedByUser = false;
        this.isGrantedToUser = false;
        this.isUserConfirmTimeout = false;
        this.isCanceledByFrontCameraCloseTask = false;
        this.isRememberCheckBoxChecked = false;
        this.mHandler = new Handler() { // from class: com.vivo.services.popupcamera.CameraPopupPermissionCheckDialog.4
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i == 0) {
                    CameraPopupPermissionCheckDialog.this.isPermissionConfirmedByUser = true;
                    CameraPopupPermissionCheckDialog.this.isGrantedToUser = true;
                    CameraPopupPermissionCheckDialog.this.permissionState.currentState = 0;
                    CameraPopupPermissionCheckDialog.this.permissionState.alwaysDeny = CameraPopupPermissionCheckDialog.this.isRememberCheckBoxChecked ? 1 : 0;
                    PopupFrontCameraPermissionHelper.setFrontCameraPermissionStateToSettings(CameraPopupPermissionCheckDialog.this.context, CameraPopupPermissionCheckDialog.this.permissionState);
                    VLog.d(CameraPopupPermissionCheckDialog.TAG, "the user grant popupcamera permission, notify the caller to run continue");
                    CameraPopupPermissionCheckDialog.this.cancelCountDownTimesMessages();
                    CameraPopupPermissionCheckDialog.this.dismiss();
                } else if (i == 1) {
                    CameraPopupPermissionCheckDialog.this.isPermissionConfirmedByUser = true;
                    CameraPopupPermissionCheckDialog.this.isGrantedToUser = false;
                    CameraPopupPermissionCheckDialog.this.permissionState.currentState = 1;
                    CameraPopupPermissionCheckDialog.this.permissionState.alwaysDeny = CameraPopupPermissionCheckDialog.this.isRememberCheckBoxChecked ? 1 : 0;
                    PopupFrontCameraPermissionHelper.setFrontCameraPermissionStateToSettings(CameraPopupPermissionCheckDialog.this.context, CameraPopupPermissionCheckDialog.this.permissionState);
                    VLog.d(CameraPopupPermissionCheckDialog.TAG, "the user deny popupcamera permission, notify the caller to run continue");
                    CameraPopupPermissionCheckDialog.this.cancelCountDownTimesMessages();
                    CameraPopupPermissionCheckDialog.this.dismiss();
                } else if (i == 2) {
                    if (CameraPopupPermissionCheckDialog.this.mCurrentCountDown > 0) {
                        Button button = CameraPopupPermissionCheckDialog.this.getButton(-2);
                        button.setText(CameraPopupPermissionCheckDialog.this.denyPermTips + " (" + CameraPopupPermissionCheckDialog.this.mCurrentCountDown + CameraPopupPermissionCheckDialog.this.context.getString(51249505) + ")");
                    }
                    CameraPopupPermissionCheckDialog.access$206(CameraPopupPermissionCheckDialog.this);
                    if (CameraPopupPermissionCheckDialog.this.mCurrentCountDown >= 0) {
                        sendEmptyMessageDelayed(2, 1000L);
                        return;
                    }
                    CameraPopupPermissionCheckDialog.this.dismiss();
                    CameraPopupPermissionCheckDialog.this.cancelCountDownTimesMessages();
                    CameraPopupPermissionCheckDialog.this.isUserConfirmTimeout = true;
                    CameraPopupPermissionCheckDialog.this.isPermissionConfirmedByUser = true;
                    CameraPopupPermissionCheckDialog.this.isGrantedToUser = false;
                    VLog.d(CameraPopupPermissionCheckDialog.TAG, "the user doesn't confirm with 10 seconds, notify the caller to run continue");
                }
            }
        };
        this.context = context;
        this.permissionState = state;
        context.getResources();
        setCancelable(false);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        View layout = inflater.inflate(50528361, (ViewGroup) null);
        this.confirmMessage = (TextView) layout.findViewById(51183695);
        this.hintMessage = (TextView) layout.findViewById(51183726);
        if (FtBuild.isOverSeas()) {
            this.hintMessage.setText(51249515);
        } else {
            this.hintMessage.setText(51249470);
        }
        CheckBox checkBox = (CheckBox) layout.findViewById(51183811);
        this.rememberCheckBox = checkBox;
        checkBox.setChecked(false);
        this.confirmMessage.setText(message);
        this.rememberCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.vivo.services.popupcamera.CameraPopupPermissionCheckDialog.1
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                VLog.d(CameraPopupPermissionCheckDialog.TAG, "onCheckedChanged checked=" + b);
                CameraPopupPermissionCheckDialog.this.isRememberCheckBoxChecked = b;
            }
        });
        if (!isShowRememberCheckbox) {
            this.rememberCheckBox.setVisibility(8);
            this.hintMessage.setVisibility(8);
        }
        setTitle(51249473);
        setView(layout);
        this.mMessage = message;
        this.grantPermTips = grantPermTips;
        this.denyPermTips = denyPermTips;
        setButton(-2, denyPermTips, this.mHandler.obtainMessage(1));
        setButton(-1, grantPermTips, this.mHandler.obtainMessage(0));
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = 272;
        getWindow().setAttributes(attrs);
        setOnShowListener(new DialogInterface.OnShowListener() { // from class: com.vivo.services.popupcamera.CameraPopupPermissionCheckDialog.2
            @Override // android.content.DialogInterface.OnShowListener
            public void onShow(DialogInterface dialogInterface) {
                CameraPopupPermissionCheckDialog.this.mHandler.sendEmptyMessage(2);
            }
        });
        setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.vivo.services.popupcamera.CameraPopupPermissionCheckDialog.3
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                CameraPopupPermissionCheckDialog.this.mCurrentCountDown = 20;
                CameraPopupPermissionCheckDialog.this.cancelCountDownTimesMessages();
                CameraPopupPermissionCheckDialog.this.nofityCallersToRun();
            }
        });
    }

    @Override // android.app.AlertDialog, android.app.Dialog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public boolean isPermissionConfirmed() {
        return this.isPermissionConfirmedByUser;
    }

    public boolean isPermissionGranted() {
        return this.isGrantedToUser;
    }

    public boolean isConfirmTimeout() {
        return this.isUserConfirmTimeout;
    }

    public void cancelPermissionCheck() {
        this.isCanceledByFrontCameraCloseTask = true;
    }

    public boolean isPermissionCheckCanceled() {
        return this.isCanceledByFrontCameraCloseTask;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelCountDownTimesMessages() {
        if (this.mHandler.hasMessages(2)) {
            this.mHandler.removeMessages(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void nofityCallersToRun() {
        synchronized (this) {
            notifyAll();
        }
    }
}