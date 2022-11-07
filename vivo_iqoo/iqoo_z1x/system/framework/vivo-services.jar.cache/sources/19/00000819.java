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
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import com.vivo.common.VivoCollectData;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.security.VivoPermissionManager;
import com.vivo.services.security.client.IVivoPermissionCallback;
import com.vivo.services.security.client.VivoPermissionInfo;
import com.vivo.services.security.client.VivoPermissionType;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
public final class VivoPermissionDialog {
    public static final long CONFIRM_PERIOD = 1000;
    public static final long CONFIRM_TIMEOUT = 20000;
    public static final String PROP_SUPER_SAVER = "sys.super_power_save";
    private boolean isVivoImeiPkg;
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
    private boolean mRememberChoice = false;
    private boolean isDialogChecked = false;
    private VivoPermissionInfo mVpi = null;
    private VivoPermissionType mVpt = null;

    public VivoPermissionDialog(VivoPermissionService vps, Context context, Handler uiHandler, String packageName, String permName, int uid, String key) {
        this.mVPS = null;
        this.mContext = null;
        this.mUiHandler = null;
        this.mCallingUid = 0;
        this.mVPDKey = null;
        this.mPackageName = null;
        this.mPermissionName = null;
        this.mCallbacks = null;
        this.isVivoImeiPkg = false;
        this.timeCountDownRight = true;
        this.mVPS = vps;
        this.mContext = new ContextThemeWrapper(context, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
        this.mUiHandler = uiHandler;
        this.mPackageName = packageName;
        this.mPermissionName = permName;
        this.mCallingUid = uid;
        this.mVPDKey = key;
        this.mCallbacks = new RemoteCallbackList<>();
        this.isVivoImeiPkg = this.mVPS.isVivoImeiPkg(packageName);
        if (VivoPermissionManager.getInstance().isOverSeas()) {
            this.timeCountDownRight = false;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:28:0x0168  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x0190  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void show() {
        /*
            Method dump skipped, instructions count: 641
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.security.server.VivoPermissionDialog.show():void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.vivo.services.security.server.VivoPermissionDialog$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$vivo$services$security$client$VivoPermissionType;

        static {
            int[] iArr = new int[VivoPermissionType.values().length];
            $SwitchMap$com$vivo$services$security$client$VivoPermissionType = iArr;
            try {
                iArr[VivoPermissionType.BLUETOOTH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CHANGE_WIFI_STATE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.SEND_SMS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.SEND_MMS.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CALL_PHONE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.MONITOR_CALL.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_SMS.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_SMS.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_MMS.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_MMS.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_CONTACTS.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_CONTACTS.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_CALL_LOG.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_CALL_LOG.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.ACCESS_LOCATION.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.READ_PHONE_STATE.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CAMERA_IMAGE.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CAMERA_VIDEO.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.RECORD_AUDIO.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.CHANGE_NETWORK_STATE.ordinal()] = 20;
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
            try {
                $SwitchMap$com$vivo$services$security$client$VivoPermissionType[VivoPermissionType.WRITE_CALENDAR.ordinal()] = 23;
            } catch (NoSuchFieldError e23) {
            }
        }
    }

    private boolean needRemeber(VivoPermissionType vpt) {
        int i = AnonymousClass1.$SwitchMap$com$vivo$services$security$client$VivoPermissionType[vpt.ordinal()];
        if (i != 1 && i != 2) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getNegativeButtonText(long timeLeft) {
        StringBuffer sb = new StringBuffer();
        if (this.timeCountDownRight) {
            sb.append(this.mContext.getString(51249411));
            sb.append("(");
            sb.append(timeLeft);
            sb.append("s");
            sb.append(")");
        } else {
            sb.append("(");
            sb.append(timeLeft);
            sb.append("s");
            sb.append(")");
            sb.append(this.mContext.getString(51249411));
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

    private String getPermissionString(VivoPermissionType vpt) {
        String result = vpt.toString();
        int stringId = -1;
        switch (AnonymousClass1.$SwitchMap$com$vivo$services$security$client$VivoPermissionType[vpt.ordinal()]) {
            case 1:
                stringId = 51249455;
                break;
            case 2:
                stringId = 51249458;
                break;
            case 3:
                stringId = 51249475;
                break;
            case 4:
                stringId = 51249474;
                break;
            case 5:
                stringId = 51249461;
                break;
            case 6:
                stringId = 51249462;
                break;
            case 7:
                stringId = 51249468;
                break;
            case 8:
                stringId = 51249490;
                break;
            case 9:
                stringId = 51249468;
                break;
            case 10:
                stringId = 51249490;
                break;
            case 11:
                stringId = 51249465;
                break;
            case 12:
                stringId = 51249487;
                break;
            case 13:
                stringId = 51249464;
                break;
            case 14:
                stringId = 51249486;
                break;
            case 15:
                stringId = 51249414;
                break;
            case 16:
                stringId = 51249413;
                break;
            case 17:
                stringId = 51249483;
                break;
            case 18:
                stringId = 51249483;
                break;
            case 19:
                stringId = 51249459;
                break;
            case 20:
                stringId = 51249456;
                break;
            case 21:
                stringId = 51249457;
                break;
            case 22:
                stringId = 51249463;
                break;
            case 23:
                stringId = 51249485;
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

    public void dismiss() {
        VivoPermissionService.printfInfo("dismissing VivoPermissionDialog...");
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
    public final class ConfirmDialogListener implements DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener {
        ConfirmDialogListener() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            VivoPermissionDialog.this.cancelConfirmTimer();
            VivoPermissionDialog.this.isDialogChecked = true;
            if (which == -1) {
                VivoPermissionDialog.this.mRememberChoice = true;
                if (VivoPermissionDialog.this.mVpt == VivoPermissionType.SET_WALLPAPER) {
                    VivoPermissionDialog.this.setPermissionResultSync(5);
                } else {
                    VivoPermissionDialog.this.setPermissionResultSync(1);
                }
            } else if (which == -2) {
                VivoPermissionDialog.this.setPermissionResultSync(2);
            } else if (which == -3) {
                VivoPermissionDialog.this.mRememberChoice = true;
                VivoPermissionDialog.this.setPermissionResultSync(5);
            }
        }

        @Override // android.widget.CompoundButton.OnCheckedChangeListener
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            VivoPermissionDialog.this.mRememberChoice = isChecked;
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            VivoPermissionDialog.this.cancelConfirmTimer();
            VivoPermissionDialog.this.isDialogChecked = true;
            switch (view.getId()) {
                case 51183629:
                    VivoPermissionDialog.this.mRememberChoice = true;
                    if (VivoPermissionDialog.this.mVpt == VivoPermissionType.SET_WALLPAPER) {
                        VivoPermissionDialog.this.setPermissionResultSync(5);
                        return;
                    } else {
                        VivoPermissionDialog.this.setPermissionResultSync(1);
                        return;
                    }
                case 51183630:
                    VivoPermissionDialog.this.mRememberChoice = true;
                    VivoPermissionDialog.this.setPermissionResultSync(5);
                    return;
                case 51183710:
                    VivoPermissionDialog.this.mRememberChoice = true;
                    VivoPermissionDialog.this.setPermissionResultSync(2);
                    return;
                case 51183959:
                    VivoPermissionDialog.this.mRememberChoice = true;
                    VivoPermissionDialog.this.setPermissionResultSync(3);
                    return;
                default:
                    return;
            }
        }
    }

    public boolean isPermissionConfirmed() {
        return this.mPermissionResult != 0;
    }

    public boolean isRememberChoice() {
        return this.mRememberChoice;
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
            int vpTypeId = this.mVpt.getVPTypeId();
            boolean z = true;
            if (this.mVpt == VivoPermissionType.READ_PHONE_STATE && this.isDialogChecked && this.isVivoImeiPkg) {
                if (result == 1) {
                    if (this.mVPS.needShowImeiTipsDialogOne(this.mVpi, vpTypeId)) {
                        String str = this.mPackageName;
                        VCD_VC_1(str, "0", (this.mVpi.getTipsDialogOneMode(vpTypeId) + 1) + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    } else if (this.mVPS.needShowImeiTipsDialogTwo(this.mVpi, vpTypeId)) {
                        String str2 = this.mPackageName;
                        VCD_VC_1(str2, "0", (this.mVpi.getTipsDialogOneMode(vpTypeId) + this.mVpi.getTipsDialogTwoMode(vpTypeId) + 1) + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    }
                } else if (this.mVPS.needShowImeiTipsDialogOne(this.mVpi, vpTypeId)) {
                    int count = this.mVpi.getTipsDialogOneMode(vpTypeId) + 1;
                    this.mVpi.setTipsDialogOneMode(vpTypeId, count);
                    String str3 = this.mPackageName;
                    VCD_VC_1(str3, "1", count + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    this.mVPS.setAppPermissionExt(this.mVpi);
                } else if (this.mVPS.needShowImeiTipsDialogTwo(this.mVpi, vpTypeId)) {
                    int count2 = this.mVpi.getTipsDialogOneMode(vpTypeId) + this.mVpi.getTipsDialogTwoMode(vpTypeId) + 1;
                    if (this.mRememberChoice) {
                        this.mVpi.setTipsDialogTwoMode(vpTypeId, this.mVpi.getTipsDialogTwoMode(vpTypeId) + 1);
                        String str4 = this.mPackageName;
                        VCD_VC_1(str4, "2", count2 + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    } else {
                        String str5 = this.mPackageName;
                        VCD_VC_1(str5, "1", count2 + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    }
                }
            }
            if (this.mRememberChoice && this.isDialogChecked) {
                this.mVPS.setAppPermission(this.mPackageName, vpTypeId, result, this.mCallingUid);
            }
            if (this.mVpt == VivoPermissionType.SET_WALLPAPER && this.isDialogChecked) {
                if (!this.mRememberChoice && result == 2) {
                    VCD_VC_2(this.mPackageName, 3);
                } else {
                    VCD_VC_2(this.mPackageName, result);
                }
            }
            if (result != 1) {
                z = false;
            }
            notifyCallbacks(z);
            this.mVPS.removeVPD(this.mVPDKey);
            notifyAll();
            if (this.mAlertDialog != null) {
                synchronized (this.mAlertDialog) {
                    this.mAlertDialog.dismiss();
                }
                this.mAlertDialog = null;
            }
        }
    }

    private void startConfirmTimer(long timeout, long period, Button button) {
        if (this.mConfirmTimer == null) {
            this.mConfirmTimer = new Timer();
        }
        this.mConfirmTimer.schedule(new ConfirmTimerTask(timeout, period, button), period, period);
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
        private Button mButton;
        private long mPeriod;
        private long mTimeLeft;

        ConfirmTimerTask(long timeout, long period, Button button) {
            this.mTimeLeft = 0L;
            this.mPeriod = 0L;
            this.mTimeLeft = timeout;
            this.mPeriod = period;
            this.mButton = button;
        }

        private boolean needDismiss() {
            boolean superSaverOn = SystemProperties.getBoolean("sys.super_power_save", false);
            return superSaverOn || VivoPermissionService.isKeyguardLocked(VivoPermissionDialog.this.mContext);
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            long j = this.mTimeLeft - this.mPeriod;
            this.mTimeLeft = j;
            if (j <= 0 || needDismiss()) {
                VivoPermissionDialog.this.dismiss();
            } else {
                updateDialog();
            }
        }

        private void updateDialog() {
            VivoPermissionDialog.this.mUiHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionDialog.ConfirmTimerTask.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoPermissionService.printfInfo("mTimeLeft=" + ConfirmTimerTask.this.mTimeLeft);
                    if (ConfirmTimerTask.this.mButton != null) {
                        ConfirmTimerTask.this.mButton.setText(VivoPermissionDialog.this.getNegativeButtonText(ConfirmTimerTask.this.mTimeLeft / ConfirmTimerTask.this.mPeriod));
                    }
                }
            });
        }
    }

    private void VCD_VC_1(String pkg, String mode, String type) {
        try {
            VivoCollectData mVCD = VivoCollectData.getInstance(this.mContext);
            long curTime = System.currentTimeMillis();
            HashMap<String, String> params = new HashMap<>();
            params.put("pkg", pkg);
            params.put("mode", mode);
            params.put("type", type);
            if (mVCD != null && mVCD.getControlInfo("243")) {
                mVCD.writeData("243", "2433", curTime, curTime, 0L, 1, params);
            }
            EventTransfer mVcode = EventTransfer.getInstance();
            if (mVcode != null) {
                mVcode.singleEvent("243", "2433", curTime, 1L, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void VCD_VC_2(String pkg, int type) {
        try {
            VivoCollectData mVCD = VivoCollectData.getInstance(this.mContext);
            long curTime = System.currentTimeMillis();
            HashMap<String, String> params = new HashMap<>();
            params.put("app", pkg);
            params.put("choose", type + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            if (mVCD != null && mVCD.getControlInfo("243")) {
                mVCD.writeData("243", "24331", curTime, curTime, 0L, 1, params);
            }
            EventTransfer mVcode = EventTransfer.getInstance();
            if (mVcode != null) {
                mVcode.singleEvent("243", "24331", curTime, 1L, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean onlyForgroundPerm(String perm) {
        char c;
        int hashCode = perm.hashCode();
        if (hashCode != 463403621) {
            if (hashCode == 1831139720 && perm.equals("android.permission.RECORD_AUDIO")) {
                c = 1;
            }
            c = 65535;
        } else {
            if (perm.equals("android.permission.CAMERA")) {
                c = 0;
            }
            c = 65535;
        }
        return c == 0 || c == 1;
    }
}