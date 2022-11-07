package com.android.server.pm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import java.util.Timer;
import java.util.TimerTask;
import vivo.util.VSlog;
import vivo.util.VivoThemeUtil;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoADBInstallWarningDialog implements DialogInterface.OnCancelListener {
    private static final long CONFIRM_PERIOD = 1000;
    private static final long CONFIRM_TIMEOUT = 10000;
    private static final long FORBID_GET_APP_LIST_CONFIRM_TIMEOUT = 10000;
    private static final String TAG = "VivoADBInstallWarningDialog";
    public static final int WARNING_CONFIRM_ALLOW = 2;
    public static final int WARNING_CONFIRM_CANCEL = 1;
    public static final int WARNING_CONFIRM_TIME_OUT = 3;
    public static final int WARN_DIALOG_FLAG = 268435456;
    protected boolean isDialogChecked;
    private AlertDialog mAlertDialog;
    private int mAppInstallFlags;
    private String mAppName;
    private int mCallingPid;
    protected CheckBox mCheckBox;
    private Timer mConfirmTimer;
    private Context mContext;
    protected boolean mDialogIsChecked;
    private String mPackageName;
    private PackageManagerService mPms;
    protected boolean mRememberChoice;
    private Handler mUiHandler;
    public boolean mWarnDialogIsTimeOut;
    private int mWarningResult;

    public VivoADBInstallWarningDialog(PackageManagerService pms, Context context, Handler uiHandler, String packageName, String appName) {
        this.mPms = null;
        this.mContext = null;
        this.mPackageName = null;
        this.mAppName = null;
        this.mWarningResult = 1;
        this.mAlertDialog = null;
        this.mConfirmTimer = null;
        this.mUiHandler = null;
        this.mAppInstallFlags = 0;
        this.mRememberChoice = false;
        this.isDialogChecked = false;
        this.mDialogIsChecked = false;
        this.mWarnDialogIsTimeOut = false;
        this.mCallingPid = -1;
        this.mPms = pms;
        this.mContext = context;
        this.mUiHandler = uiHandler;
        this.mPackageName = packageName;
        this.mAppName = appName;
    }

    public void show() {
        ConfirmDialogListener listener = new ConfirmDialogListener();
        AlertDialog create = new AlertDialog.Builder(this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setTitle(51249708).setMessage(String.format(this.mContext.getString(51249707), this.mAppName)).setNegativeButton(getNegativeButtonText(10L), listener).setPositiveButton(51249705, listener).setOnCancelListener(this).create();
        this.mAlertDialog = create;
        create.getWindow().setType(2003);
        this.mAlertDialog.getWindow().getAttributes().flags |= 536870912;
        this.mAlertDialog.show();
        startConfirmTimer(10000L, 1000L);
    }

    public VivoADBInstallWarningDialog(PackageManagerService pms, Context context, Handler uiHandler, String packageName, String appName, int installFlags) {
        this.mPms = null;
        this.mContext = null;
        this.mPackageName = null;
        this.mAppName = null;
        this.mWarningResult = 1;
        this.mAlertDialog = null;
        this.mConfirmTimer = null;
        this.mUiHandler = null;
        this.mAppInstallFlags = 0;
        this.mRememberChoice = false;
        this.isDialogChecked = false;
        this.mDialogIsChecked = false;
        this.mWarnDialogIsTimeOut = false;
        this.mCallingPid = -1;
        this.mPms = pms;
        this.mContext = context;
        this.mUiHandler = uiHandler;
        this.mPackageName = packageName;
        this.mAppName = appName;
        this.mAppInstallFlags = installFlags;
    }

    public void showAppStoreWarnDialog() {
        ConfirmDialogListener listener = new ConfirmDialogListener();
        AlertDialog create = new AlertDialog.Builder(this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setTitle(51249974).setMessage(String.format(this.mContext.getString(51249973), new Object[0])).setNegativeButton(getNegativeButtonText(10L), listener).setPositiveButton(51249972, listener).setOnCancelListener(this).setCancelable(false).create();
        this.mAlertDialog = create;
        create.getWindow().setType(2003);
        this.mAlertDialog.getWindow().getAttributes().flags |= 536870912;
        this.mAlertDialog.show();
        startConfirmTimer(10000L, 1000L);
    }

    public VivoADBInstallWarningDialog(PackageManagerService pms, Context context, Handler uiHandler, String packageName, String appName, int installFlags, int callingPid) {
        this.mPms = null;
        this.mContext = null;
        this.mPackageName = null;
        this.mAppName = null;
        this.mWarningResult = 1;
        this.mAlertDialog = null;
        this.mConfirmTimer = null;
        this.mUiHandler = null;
        this.mAppInstallFlags = 0;
        this.mRememberChoice = false;
        this.isDialogChecked = false;
        this.mDialogIsChecked = false;
        this.mWarnDialogIsTimeOut = false;
        this.mCallingPid = -1;
        this.mPms = pms;
        this.mContext = context;
        this.mUiHandler = uiHandler;
        this.mPackageName = packageName;
        this.mAppName = appName;
        this.mAppInstallFlags = installFlags;
        this.mCallingPid = callingPid;
    }

    public void showGetInstallPackageWarnDialog() {
        String msg;
        ConfirmDialogListener listener = new ConfirmDialogListener();
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
        this.mContext = contextThemeWrapper;
        LayoutInflater inflater = (LayoutInflater) contextThemeWrapper.getSystemService("layout_inflater");
        View layout = inflater.inflate(50528361, (ViewGroup) null);
        TextView content = (TextView) layout.findViewById(51183695);
        String contentStr = String.format(this.mContext.getString(51249471), this.mAppName, this.mContext.getString(51249887));
        content.setText(contentStr);
        TextView contentHint = (TextView) layout.findViewById(51183726);
        if (VivoPKMSDatabaseUtils.isRomVersionIsLow_30()) {
            msg = this.mContext.getString(51249882);
        } else {
            msg = this.mContext.getString(51249470);
        }
        if (PackageManagerService.DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "showGetInstallPackageWarnDialog msg " + msg);
        }
        contentHint.setText(msg);
        CheckBox checkBox = (CheckBox) layout.findViewById(51183811);
        this.mCheckBox = checkBox;
        checkBox.setOnCheckedChangeListener(listener);
        this.mCheckBox.setChecked(false);
        AlertDialog create = new AlertDialog.Builder(this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setTitle(51249473).setView(layout).setNegativeButton(getNegativeButtonText(10L), listener).setPositiveButton(51249782, listener).setCancelable(false).create();
        this.mAlertDialog = create;
        create.getWindow().setType(2003);
        this.mAlertDialog.getWindow().getAttributes().flags |= 536870912;
        this.mAlertDialog.show();
        startConfirmTimer(10000L, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dismiss() {
        cancelConfirmTimer();
        AlertDialog alertDialog = this.mAlertDialog;
        if (alertDialog != null) {
            synchronized (alertDialog) {
                this.mAlertDialog.dismiss();
            }
            this.mAlertDialog = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setWarningConfirmResultSync(int result) {
        synchronized (this) {
            this.mWarningResult = result;
            notifyAll();
        }
    }

    public int getWarningConfirmResult() {
        CheckBox checkBox = this.mCheckBox;
        if (checkBox != null) {
            this.mDialogIsChecked = checkBox.isChecked();
        }
        if (PackageManagerService.DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "mDialogIsChecked " + this.mDialogIsChecked + " mRememberChoice:" + this.mRememberChoice + " isDialogChecked:" + this.isDialogChecked + " mWarningResult:" + this.mWarningResult);
        }
        return this.mWarningResult;
    }

    /* loaded from: classes.dex */
    private final class ConfirmDialogListener implements DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener {
        ConfirmDialogListener() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            VivoADBInstallWarningDialog.this.isDialogChecked = true;
            if (which == -1) {
                VivoADBInstallWarningDialog.this.setWarningConfirmResultSync(2);
            } else if (which == -2) {
                VivoADBInstallWarningDialog.this.setWarningConfirmResultSync(1);
            }
        }

        @Override // android.widget.CompoundButton.OnCheckedChangeListener
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            VivoADBInstallWarningDialog.this.mRememberChoice = isChecked;
            if (PackageManagerService.DEBUG_FOR_FB_APL) {
                VSlog.d(VivoADBInstallWarningDialog.TAG, "onCheckedChanged  isChecked:" + isChecked + " isDialogChecked:" + VivoADBInstallWarningDialog.this.isDialogChecked);
            }
        }
    }

    @Override // android.content.DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        setWarningConfirmResultSync(1);
    }

    private void startConfirmTimer(long timeout, long period) {
        if (this.mConfirmTimer == null) {
            this.mConfirmTimer = new Timer();
        }
        this.mConfirmTimer.schedule(new ConfirmTimerTask(timeout, period), period, period);
    }

    private void cancelConfirmTimer() {
        Timer timer = this.mConfirmTimer;
        if (timer != null) {
            timer.cancel();
            this.mConfirmTimer = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ConfirmTimerTask extends TimerTask {
        private long mPeriod;
        private long mTimeLeft;

        ConfirmTimerTask(long timeout, long period) {
            this.mTimeLeft = 0L;
            this.mPeriod = 0L;
            this.mTimeLeft = timeout;
            this.mPeriod = period;
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            long j = this.mTimeLeft - this.mPeriod;
            this.mTimeLeft = j;
            if (j <= 0) {
                if ((VivoADBInstallWarningDialog.this.mAppInstallFlags & 268435456) != 0) {
                    VivoADBInstallWarningDialog.this.setWarningConfirmResultSync(3);
                } else {
                    VivoADBInstallWarningDialog.this.setWarningConfirmResultSync(1);
                }
                VivoADBInstallWarningDialog.this.dismiss();
                return;
            }
            updateDialog();
        }

        private void updateDialog() {
            VivoADBInstallWarningDialog.this.mUiHandler.post(new Runnable() { // from class: com.android.server.pm.VivoADBInstallWarningDialog.ConfirmTimerTask.1
                @Override // java.lang.Runnable
                public void run() {
                    if (VivoADBInstallWarningDialog.this.mAlertDialog != null) {
                        synchronized (VivoADBInstallWarningDialog.this.mAlertDialog) {
                            if (VivoADBInstallWarningDialog.this.mAlertDialog != null) {
                                VivoADBInstallWarningDialog.this.mAlertDialog.getButton(-2).setText(VivoADBInstallWarningDialog.this.getNegativeButtonText(ConfirmTimerTask.this.mTimeLeft / ConfirmTimerTask.this.mPeriod));
                            }
                        }
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getNegativeButtonText(long timeLeft) {
        StringBuffer sb = new StringBuffer();
        int i = this.mAppInstallFlags;
        if ((4194304 & i) != 0) {
            sb.append(this.mContext.getString(51249971));
        } else if ((i & 268435456) != 0) {
            sb.append(this.mContext.getString(51249411));
        } else {
            sb.append(this.mContext.getString(51249706));
        }
        sb.append("(");
        sb.append(timeLeft);
        sb.append("s");
        sb.append(")");
        return sb.toString();
    }

    public int getCallingPid() {
        return this.mCallingPid;
    }
}