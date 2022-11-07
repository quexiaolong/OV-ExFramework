package com.vivo.services.security.server;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.vivo.framework.security.VivoPermissionManager;
import com.vivo.services.security.client.IVivoPermissionCallback;
import com.vivo.services.security.client.VivoPermissionType;
import java.util.Timer;
import java.util.TimerTask;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
public final class VivoPermissionDeniedDialogModeThree {
    public static final long CONFIRM_PERIOD = 1000;
    public static final long CONFIRM_TIMEOUT = 20000;
    public static final String PROP_SUPER_SAVER = "sys.super_power_save";
    private RemoteCallbackList<IVivoPermissionCallback> mCallbacks;
    private int mCallingUid;
    private Context mContext;
    private String mPackageName;
    private String mPermissionName;
    private Handler mUiHandler;
    private String mVPDKey;
    private VivoPermissionService mVPS;
    private boolean timeCountDownRight;
    private Timer mConfirmTimer = null;
    private AlertDialog mAlertDialog = null;
    private int mPermissionResult = 0;
    private boolean isDialogChecked = false;

    public VivoPermissionDeniedDialogModeThree(VivoPermissionService vps, Context context, Handler uiHandler, String packageName, String permName, int uid, String key) {
        this.mVPS = null;
        this.mContext = null;
        this.mUiHandler = null;
        this.mCallingUid = 0;
        this.mVPDKey = null;
        this.mPackageName = null;
        this.mPermissionName = null;
        this.mCallbacks = null;
        this.timeCountDownRight = true;
        this.mVPS = vps;
        this.mContext = new ContextThemeWrapper(context, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
        this.mUiHandler = uiHandler;
        this.mPackageName = packageName;
        this.mPermissionName = permName;
        this.mCallingUid = uid;
        this.mVPDKey = key;
        this.mCallbacks = new RemoteCallbackList<>();
        if (VivoPermissionManager.getInstance().isOverSeas()) {
            this.timeCountDownRight = false;
        }
    }

    private String getContentStr() {
        String packageName = this.mPackageName;
        String permName = this.mPermissionName;
        String PermStr = getPermissionString(permName);
        float osVer = VivoPermissionManager.getInstance().getOSVersion();
        if (osVer >= 3.0f) {
            return String.format(this.mContext.getString(51249435), getAppName(packageName), PermStr);
        }
        return String.format(this.mContext.getString(51249434), getAppName(packageName), PermStr);
    }

    private String getContentTitle() {
        String permName = this.mPermissionName;
        String PermStr = getPermissionString(permName);
        return String.format(this.mContext.getString(51249409), PermStr);
    }

    public void show() {
        String str = this.mPackageName;
        String str2 = this.mPermissionName;
        ConfirmDialogListener listener = new ConfirmDialogListener();
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        View layout = inflater.inflate(50528363, (ViewGroup) null);
        TextView content = (TextView) layout.findViewById(51183726);
        String contentStr = getContentStr();
        content.setText(contentStr);
        String contentTitle = getContentTitle();
        AlertDialog create = new AlertDialog.Builder(this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setTitle(contentTitle).setView(layout).setPositiveButton(51249451, listener).setNegativeButton(getNegativeButtonText(20L), listener).create();
        this.mAlertDialog = create;
        create.getWindow().setType(2003);
        this.mAlertDialog.getWindow().getAttributes().privateFlags |= 536870928;
        this.mAlertDialog.setCancelable(false);
        this.mAlertDialog.show();
        startConfirmTimer(20000L, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getNegativeButtonText(long timeLeft) {
        StringBuffer sb = new StringBuffer();
        if (this.timeCountDownRight) {
            sb.append(this.mContext.getString(51249432));
            sb.append("(");
            sb.append(timeLeft);
            sb.append("s");
            sb.append(")");
        } else {
            sb.append("(");
            sb.append(timeLeft);
            sb.append("s");
            sb.append(")");
            sb.append(this.mContext.getString(51249432));
        }
        return sb.toString();
    }

    private String getAppName(String packageName) {
        try {
            PackageInfo pi = this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 64, UserHandle.getUserId(this.mCallingUid));
            return pi.applicationInfo.loadLabel(this.mContext.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            VivoPermissionService.printfInfo("Can't get calling app package info");
            e.printStackTrace();
            return packageName;
        }
    }

    private String getPermissionString(String permName) {
        VivoPermissionType vpt = VivoPermissionType.getVPType(permName);
        String result = vpt.toString();
        int stringId = -1;
        switch (AnonymousClass1.$SwitchMap$com$vivo$services$security$client$VivoPermissionType[vpt.ordinal()]) {
            case 1:
                stringId = 51249475;
                break;
            case 2:
                stringId = 51249474;
                break;
            case 3:
                stringId = 51249461;
                break;
            case 4:
                stringId = 51249462;
                break;
            case 5:
                stringId = 51249468;
                break;
            case 6:
                stringId = 51249490;
                break;
            case 7:
                stringId = 51249468;
                break;
            case 8:
                stringId = 51249490;
                break;
            case 9:
                stringId = 51249465;
                break;
            case 10:
                stringId = 51249487;
                break;
            case 11:
                stringId = 51249464;
                break;
            case 12:
                stringId = 51249486;
                break;
            case 13:
                stringId = 51249414;
                break;
            case 14:
                stringId = 51249413;
                break;
            case 15:
                stringId = 51249483;
                break;
            case 16:
                stringId = 51249483;
                break;
            case 17:
                stringId = 51249459;
                break;
            case 18:
                stringId = 51249456;
                break;
            case 19:
                stringId = 51249458;
                break;
            case 20:
                stringId = 51249455;
                break;
            case 21:
                stringId = 51249457;
                break;
            case 22:
                stringId = 51249463;
                break;
        }
        if (stringId != -1) {
            result = this.mContext.getString(stringId).toLowerCase();
        }
        if (stringId == 51249475 || stringId == 51249489 || stringId == 51249467) {
            return result.replace("sms", "SMS");
        }
        if (stringId == 51249474 || stringId == 51249488 || stringId == 51249466) {
            return result.replace("mms", "MMS");
        }
        if (stringId == 51249468 || stringId == 51249490) {
            return result.replace("sms", "SMS").replace("mms", "MMS");
        }
        if (stringId == 51249413) {
            return result.replace("id", "ID");
        }
        if (stringId == 51249458) {
            if (VivoPermissionManager.getInstance().isOverSeas()) {
                return result.replace("wlan", "Wi-Fi").replace("wi-fi", "Wi-Fi");
            }
            return result.replace("wlan", "WLAN");
        } else if (stringId == 51249457) {
            return result.replace("nfc", "NFC");
        } else {
            if (stringId == 51249456) {
                return result.replace("mobile", "Mobile").replace("network", "Network");
            }
            if (stringId == 51249455) {
                return result.replace("bluetooth", "Bluetooth");
            }
            return result;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.vivo.services.security.server.VivoPermissionDeniedDialogModeThree$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$vivo$services$security$client$VivoPermissionType;

        static {
            int[] iArr = new int[VivoPermissionType.values().length];
            $SwitchMap$com$vivo$services$security$client$VivoPermissionType = iArr;
            try {
                iArr[VivoPermissionType.SEND_SMS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.SEND_MMS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CALL_PHONE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.MONITOR_CALL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_SMS.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_SMS.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_MMS.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_MMS.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_CONTACTS.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_CONTACTS.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_CALL_LOG.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_CALL_LOG.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.ACCESS_LOCATION.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_PHONE_STATE.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CAMERA_IMAGE.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CAMERA_VIDEO.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.RECORD_AUDIO.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CHANGE_NETWORK_STATE.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CHANGE_WIFI_STATE.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.BLUETOOTH.ordinal()] = 20;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.NFC.ordinal()] = 21;
            } catch (NoSuchFieldError e21) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_CALENDAR.ordinal()] = 22;
            } catch (NoSuchFieldError e22) {
            }
        }
    }

    public void dismiss() {
        VivoPermissionService.printfInfo("dismissing VivoPermissionDeniedDialogModeThree 3 ...");
        cancelConfirmTimer();
        setPermissionResultSync(2);
        AlertDialog alertDialog = this.mAlertDialog;
        if (alertDialog != null) {
            synchronized (alertDialog) {
                this.mAlertDialog.dismiss();
            }
            this.mAlertDialog = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ConfirmDialogListener implements DialogInterface.OnClickListener {
        ConfirmDialogListener() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            VivoPermissionDeniedDialogModeThree.this.cancelConfirmTimer();
            VivoPermissionDeniedDialogModeThree.this.isDialogChecked = true;
            if (which == -1) {
                VivoPermissionDeniedDialogModeThree.this.setPermissionResultSync(1);
            } else if (which == -2) {
                VivoPermissionDeniedDialogModeThree.this.setPermissionResultSync(2);
            }
            VivoPermissionType.getVPType(VivoPermissionDeniedDialogModeThree.this.mPermissionName).getVPTypeId();
            if (VivoPermissionDeniedDialogModeThree.this.mVPS.checkConfigDeniedMode(VivoPermissionDeniedDialogModeThree.this.mPackageName, VivoPermissionDeniedDialogModeThree.this.mPermissionName) == 48) {
                VivoPermissionDeniedDialogModeThree.this.mVPS.setConfigDeniedMode(VivoPermissionDeniedDialogModeThree.this.mPackageName, VivoPermissionDeniedDialogModeThree.this.mPermissionName, 80, VivoPermissionDeniedDialogModeThree.this.mCallingUid);
            }
        }
    }

    public boolean isPermissionConfirmed() {
        return this.mPermissionResult != 0;
    }

    public int getPermissionResult(String permName) {
        return this.mPermissionResult;
    }

    public int getCallingUid() {
        return this.mCallingUid;
    }

    public void handleWaitTimeOut() {
        this.mPermissionResult = 2;
    }

    public void registerCallback(IVivoPermissionCallback cb) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.register(cb);
        }
    }

    public void notifyCallbacks(boolean result) {
        synchronized (this.mCallbacks) {
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    this.mCallbacks.getBroadcastItem(i).onPermissionConfirmed(result);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            this.mCallbacks.finishBroadcast();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPermissionResultSync(int result) {
        synchronized (this) {
            this.mPermissionResult = result;
            int vpTypeId = VivoPermissionType.getVPType(this.mPermissionName).getVPTypeId();
            if (this.isDialogChecked) {
                this.mVPS.setAppPermission(this.mPackageName, vpTypeId, result);
            }
            boolean z = true;
            if (result != 1) {
                z = false;
            }
            notifyCallbacks(z);
            this.mVPS.removeVPD3(this.mVPDKey);
            notifyAll();
        }
    }

    private void startConfirmTimer(long timeout, long period) {
        if (this.mConfirmTimer == null) {
            this.mConfirmTimer = new Timer();
        }
        this.mConfirmTimer.schedule(new ConfirmTimerTask(timeout, period), period, period);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelConfirmTimer() {
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

        private boolean needDismiss() {
            boolean superSaverOn = SystemProperties.getBoolean("sys.super_power_save", false);
            return superSaverOn || VivoPermissionService.isKeyguardLocked(VivoPermissionDeniedDialogModeThree.this.mContext);
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            long j = this.mTimeLeft - this.mPeriod;
            this.mTimeLeft = j;
            if (j <= 0 || needDismiss()) {
                VivoPermissionDeniedDialogModeThree.this.dismiss();
            } else {
                updateDialog();
            }
        }

        private void updateDialog() {
            VivoPermissionDeniedDialogModeThree.this.mUiHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionDeniedDialogModeThree.ConfirmTimerTask.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoPermissionService.printfInfo("3 mTimeLeft=" + ConfirmTimerTask.this.mTimeLeft);
                    if (VivoPermissionDeniedDialogModeThree.this.mAlertDialog != null) {
                        synchronized (VivoPermissionDeniedDialogModeThree.this.mAlertDialog) {
                            if (VivoPermissionDeniedDialogModeThree.this.mAlertDialog != null) {
                                Button button = VivoPermissionDeniedDialogModeThree.this.mAlertDialog.getButton(-2);
                                button.setText(VivoPermissionDeniedDialogModeThree.this.getNegativeButtonText(ConfirmTimerTask.this.mTimeLeft / ConfirmTimerTask.this.mPeriod));
                            }
                        }
                    }
                }
            });
        }
    }
}