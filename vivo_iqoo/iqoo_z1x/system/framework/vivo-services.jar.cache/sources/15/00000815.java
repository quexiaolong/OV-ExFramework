package com.vivo.services.security.server;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.vivo.framework.security.VivoPermissionManager;
import com.vivo.services.security.client.VivoPermissionType;
import java.util.Timer;
import java.util.TimerTask;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
public final class VivoPermissionDeniedDialogModeTwo {
    public static final long CONFIRM_PERIOD = 1000;
    public static final long CONFIRM_TIMEOUT = 2000000;
    public static final String PROP_SUPER_SAVER = "sys.super_power_save";
    private int mCallingUid;
    private Context mContext;
    private String mPackageName;
    private String mPermissionName;
    private Handler mUiHandler;
    private String mVPDKey;
    private VivoPermissionService mVPS;
    private Timer mConfirmTimer = null;
    private AlertDialog mAlertDialog = null;
    private boolean isDialogChecked = false;

    public VivoPermissionDeniedDialogModeTwo(VivoPermissionService vps, Context context, Handler uiHandler, String packageName, String permName, int uid, String key) {
        this.mVPS = null;
        this.mContext = null;
        this.mUiHandler = null;
        this.mCallingUid = 0;
        this.mVPDKey = null;
        this.mPackageName = null;
        this.mPermissionName = null;
        this.mVPS = vps;
        this.mContext = new ContextThemeWrapper(context, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
        this.mUiHandler = uiHandler;
        this.mPackageName = packageName;
        this.mPermissionName = permName;
        this.mCallingUid = uid;
        this.mVPDKey = key;
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
        ConfirmDialogListener listener = new ConfirmDialogListener();
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        View layout = inflater.inflate(50528363, (ViewGroup) null);
        TextView content = (TextView) layout.findViewById(51183726);
        String contentStr = getContentStr();
        content.setText(contentStr);
        String contentTitle = getContentTitle();
        AlertDialog create = new AlertDialog.Builder(this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setTitle(contentTitle).setView(layout).setPositiveButton(51249451, listener).setNegativeButton(51249432, listener).create();
        this.mAlertDialog = create;
        create.getWindow().setType(2003);
        this.mAlertDialog.getWindow().getAttributes().privateFlags |= 536870928;
        this.mAlertDialog.setCancelable(false);
        this.mAlertDialog.show();
        startConfirmTimer(2000000L, 1000L);
    }

    private String getAppName(String packageName) {
        try {
            PackageInfo pi = this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 64, UserHandle.getUserId(this.mCallingUid));
            return pi.applicationInfo.loadLabel(this.mContext.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            VivoPermissionService.printfInfo("vpftd Can't get calling app package info");
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
    /* renamed from: com.vivo.services.security.server.VivoPermissionDeniedDialogModeTwo$1  reason: invalid class name */
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
        VivoPermissionService.printfInfo("2 dismissing VivoPermissionDeniedDialogModeTwo...");
        cancelConfirmTimer();
        this.mVPS.removeVPD2(this.mVPDKey);
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
            VivoPermissionDeniedDialogModeTwo.this.cancelConfirmTimer();
            VivoPermissionDeniedDialogModeTwo.this.isDialogChecked = true;
            int vpTypeId = VivoPermissionType.getVPType(VivoPermissionDeniedDialogModeTwo.this.mPermissionName).getVPTypeId();
            if (which == -1) {
                VivoPermissionDeniedDialogModeTwo.this.mVPS.setAppPermission(VivoPermissionDeniedDialogModeTwo.this.mPackageName, vpTypeId, 1);
            }
            if (VivoPermissionDeniedDialogModeTwo.this.mVPS.checkConfigDeniedMode(VivoPermissionDeniedDialogModeTwo.this.mPackageName, VivoPermissionDeniedDialogModeTwo.this.mPermissionName) == 48) {
                VivoPermissionDeniedDialogModeTwo.this.mVPS.setConfigDeniedMode(VivoPermissionDeniedDialogModeTwo.this.mPackageName, VivoPermissionDeniedDialogModeTwo.this.mPermissionName, 80, VivoPermissionDeniedDialogModeTwo.this.mCallingUid);
            }
            VivoPermissionDeniedDialogModeTwo.this.mVPS.removeVPD2(VivoPermissionDeniedDialogModeTwo.this.mVPDKey);
        }
    }

    public int getCallingUid() {
        return this.mCallingUid;
    }

    boolean isDialogClicked() {
        return true == this.isDialogChecked;
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
            return superSaverOn || VivoPermissionService.isKeyguardLocked(VivoPermissionDeniedDialogModeTwo.this.mContext);
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            if (needDismiss()) {
                VivoPermissionDeniedDialogModeTwo.this.dismiss();
            }
        }
    }
}