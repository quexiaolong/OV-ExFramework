package com.vivo.services.security.server;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.vivo.common.VivoCollectData;
import com.vivo.face.common.data.Constants;
import com.vivo.vcodetransbase.EventTransfer;
import java.io.File;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
public final class VivoDeleteDialog {
    public static final long CONFIRM_PERIOD = 1000;
    public static final long CONFIRM_TIMEOUT = 20000;
    public static final String PROP_SUPER_SAVER = "sys.super_power_save";
    private int mCallingUid;
    private Context mContext;
    private String mPackageName;
    private String mPathName;
    private String mPathPkg;
    private String mType;
    private Handler mUiHandler;
    private String mVPDKey;
    private VivoPermissionService mVPS;
    private long timeLeft;
    private Timer mConfirmTimer = null;
    private final Object mLock = new Object();
    private AlertDialog mAlertDialog = null;
    private int mPermissionResult = 0;
    private boolean isUserClicked = false;

    public VivoDeleteDialog(VivoPermissionService vps, Context context, Handler uiHandler, String packageName, String pathName, String pathPkg, String type, int uid, String key) {
        this.mVPS = null;
        this.mContext = null;
        this.mUiHandler = null;
        this.mCallingUid = -1;
        this.mVPDKey = null;
        this.mPackageName = null;
        this.mPathName = null;
        this.mPathPkg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mType = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mVPS = vps;
        this.mContext = new ContextThemeWrapper(context, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
        this.mUiHandler = uiHandler;
        this.mPackageName = packageName;
        this.mPathName = pathName;
        this.mCallingUid = uid;
        this.mVPDKey = key;
        this.mPathPkg = pathPkg;
        this.mType = type;
    }

    public void show() {
        String packageName = this.mPackageName;
        String pathName = this.mPathName;
        new File(this.mPathName);
        ConfirmDialogListener listener = new ConfirmDialogListener();
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        View layout = inflater.inflate(50528287, (ViewGroup) null);
        TextView content = (TextView) layout.findViewById(51183695);
        TextView contentHint = (TextView) layout.findViewById(51183726);
        String contentStr = String.format(this.mContext.getString(51249746), getAppName(packageName), pathName);
        content.setText(contentStr);
        contentHint.setText(getHintText(this.mPathPkg, this.mType));
        AlertDialog create = new AlertDialog.Builder(this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setTitle(51249543).setView(layout).setNegativeButton(getNegativeButtonText(20L), listener).setPositiveButton(51249415, listener).create();
        this.mAlertDialog = create;
        create.getWindow().setType(2003);
        this.mAlertDialog.setCancelable(false);
        this.mAlertDialog.show();
        startConfirmTimer(20000L, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getNegativeButtonText(long timeLeft) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        sb.append(timeLeft);
        sb.append("s");
        sb.append(")");
        sb.append(this.mContext.getString(51249411));
        return sb.toString();
    }

    private String getAppName(String packageName) {
        try {
            PackageInfo pi = this.mContext.getPackageManager().getPackageInfo(packageName, 64);
            return pi.applicationInfo.loadLabel(this.mContext.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            VivoPermissionService.printfInfo("Can't get calling app package info");
            e.printStackTrace();
            return packageName;
        }
    }

    private String getHintText(String pkg, String types) {
        StringBuffer result = new StringBuffer(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if (TextUtils.isEmpty(pkg)) {
            if (TextUtils.isEmpty(types)) {
                return result.toString();
            }
            String[] splitTypes = types.split(",");
            for (int i = 0; i < splitTypes.length; i++) {
                result.append(getTypeText(splitTypes[i]));
                if (i < splitTypes.length - 1) {
                    result.append((char) 12289);
                }
            }
            return String.format(this.mContext.getString(51249779), result.toString());
        }
        String[] splitTypes2 = types.split(",");
        for (int i2 = 0; i2 < splitTypes2.length; i2++) {
            result.append(getTypeText(splitTypes2[i2]));
            if (i2 < splitTypes2.length - 1) {
                result.append((char) 12289);
            }
        }
        return String.format(this.mContext.getString(51249780), getAppName(pkg), result.toString());
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private String getTypeText(String type) {
        char c;
        int hashCode = type.hashCode();
        switch (hashCode) {
            case 47665:
                if (type.equals("001")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 47666:
                if (type.equals("002")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 47667:
                if (type.equals("003")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 47668:
                if (type.equals("004")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 47669:
                if (type.equals("005")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 47670:
                if (type.equals("006")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 47671:
                if (type.equals("007")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 47672:
                if (type.equals("008")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 47673:
                if (type.equals("009")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            default:
                switch (hashCode) {
                    case 47695:
                        if (type.equals("010")) {
                            c = '\t';
                            break;
                        }
                        c = 65535;
                        break;
                    case 47696:
                        if (type.equals("011")) {
                            c = '\n';
                            break;
                        }
                        c = 65535;
                        break;
                    case 47697:
                        if (type.equals("012")) {
                            c = 11;
                            break;
                        }
                        c = 65535;
                        break;
                    case 47698:
                        if (type.equals("013")) {
                            c = '\f';
                            break;
                        }
                        c = 65535;
                        break;
                    case 47699:
                        if (type.equals("014")) {
                            c = '\r';
                            break;
                        }
                        c = 65535;
                        break;
                    case 47700:
                        if (type.equals("015")) {
                            c = 14;
                            break;
                        }
                        c = 65535;
                        break;
                    case 47701:
                        if (type.equals("016")) {
                            c = 15;
                            break;
                        }
                        c = 65535;
                        break;
                    case 47702:
                        if (type.equals("017")) {
                            c = 16;
                            break;
                        }
                        c = 65535;
                        break;
                    case 47703:
                        if (type.equals("018")) {
                            c = 17;
                            break;
                        }
                        c = 65535;
                        break;
                    case 47704:
                        if (type.equals("019")) {
                            c = 18;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        switch (hashCode) {
                            case 47726:
                                if (type.equals("020")) {
                                    c = 19;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47727:
                                if (type.equals("021")) {
                                    c = 20;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47728:
                                if (type.equals("022")) {
                                    c = 21;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47729:
                                if (type.equals("023")) {
                                    c = 22;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47730:
                                if (type.equals("024")) {
                                    c = 23;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47731:
                                if (type.equals("025")) {
                                    c = 24;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47732:
                                if (type.equals("026")) {
                                    c = 25;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47733:
                                if (type.equals("027")) {
                                    c = 26;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47734:
                                if (type.equals("028")) {
                                    c = 27;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 47735:
                                if (type.equals("029")) {
                                    c = 28;
                                    break;
                                }
                                c = 65535;
                                break;
                            default:
                                switch (hashCode) {
                                    case 47757:
                                        if (type.equals("030")) {
                                            c = 29;
                                            break;
                                        }
                                        c = 65535;
                                        break;
                                    case 47758:
                                        if (type.equals("031")) {
                                            c = 30;
                                            break;
                                        }
                                        c = 65535;
                                        break;
                                    case 47759:
                                        if (type.equals("032")) {
                                            c = 31;
                                            break;
                                        }
                                        c = 65535;
                                        break;
                                    default:
                                        c = 65535;
                                        break;
                                }
                        }
                }
        }
        switch (c) {
            case 0:
                return this.mContext.getString(51249747);
            case 1:
                return this.mContext.getString(51249748);
            case 2:
                return this.mContext.getString(51249749);
            case 3:
                return this.mContext.getString(51249750);
            case 4:
                return this.mContext.getString(51249751);
            case 5:
                return this.mContext.getString(51249752);
            case 6:
                return this.mContext.getString(51249753);
            case 7:
                return this.mContext.getString(51249754);
            case '\b':
                return this.mContext.getString(51249755);
            case '\t':
                return this.mContext.getString(51249756);
            case '\n':
                return this.mContext.getString(51249757);
            case 11:
                return this.mContext.getString(51249758);
            case '\f':
                return this.mContext.getString(51249759);
            case '\r':
                return this.mContext.getString(51249760);
            case 14:
                return this.mContext.getString(51249761);
            case 15:
                return this.mContext.getString(51249762);
            case 16:
                return this.mContext.getString(51249763);
            case 17:
                return this.mContext.getString(51249764);
            case 18:
                return this.mContext.getString(51249765);
            case 19:
                return this.mContext.getString(51249766);
            case 20:
                return this.mContext.getString(51249767);
            case 21:
                return this.mContext.getString(51249768);
            case 22:
                return this.mContext.getString(51249769);
            case 23:
                return this.mContext.getString(51249770);
            case 24:
                return this.mContext.getString(51249771);
            case 25:
                return this.mContext.getString(51249772);
            case 26:
                return this.mContext.getString(51249773);
            case 27:
                return this.mContext.getString(51249774);
            case 28:
                return this.mContext.getString(51249775);
            case KernelConfig.PT_ENABLE /* 29 */:
                return this.mContext.getString(51249776);
            case 30:
                return this.mContext.getString(51249777);
            case KernelConfig.LCE_SETTING /* 31 */:
                return this.mContext.getString(51249778);
            default:
                return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
    }

    public void dismiss() {
        VivoPermissionService.printfInfo("dismissing VivoDeleteDialog...");
        cancelConfirmTimer();
        setPermissionResultSync(2);
        synchronized (this.mLock) {
            if (this.mAlertDialog != null) {
                this.mAlertDialog.dismiss();
                this.mAlertDialog = null;
            }
        }
    }

    /* loaded from: classes.dex */
    private final class ConfirmDialogListener implements DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener {
        ConfirmDialogListener() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            VivoDeleteDialog.this.cancelConfirmTimer();
            VivoDeleteDialog.this.isUserClicked = true;
            if (which == -1) {
                VivoDeleteDialog.this.setPermissionResultSync(1);
            } else if (which == -2) {
                VivoDeleteDialog.this.setPermissionResultSync(2);
            }
        }

        @Override // android.widget.CompoundButton.OnCheckedChangeListener
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        }
    }

    public boolean isPermissionConfirmed() {
        return this.mPermissionResult != 0;
    }

    public boolean isUserClicked() {
        return this.isUserClicked;
    }

    public int getPermissionResult(String pathName) {
        return this.mPermissionResult;
    }

    public int getCallingUid() {
        return this.mCallingUid;
    }

    public void handleWaitTimeOut() {
        this.mPermissionResult = 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPermissionResultSync(int result) {
        synchronized (this) {
            this.mPermissionResult = result;
            this.mVPS.removeVDD(this.mVPDKey);
            int click = -1;
            if (this.isUserClicked) {
                if (result == 1) {
                    click = 1;
                } else if (result == 2) {
                    click = 2;
                }
            } else {
                click = 3;
            }
            VCD_VC(click, 20 - ((int) this.timeLeft));
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
            return superSaverOn || VivoPermissionService.isKeyguardLocked(VivoDeleteDialog.this.mContext);
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            synchronized (this) {
                long j = this.mTimeLeft - this.mPeriod;
                this.mTimeLeft = j;
                VivoDeleteDialog.this.setTimeLeft(j / this.mPeriod);
            }
            if (this.mTimeLeft <= 0 || needDismiss()) {
                VivoDeleteDialog.this.dismiss();
            } else {
                updateDialog();
            }
        }

        private void updateDialog() {
            VivoDeleteDialog.this.mUiHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoDeleteDialog.ConfirmTimerTask.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoPermissionService.printfInfo("mTimeLeft=" + ConfirmTimerTask.this.mTimeLeft);
                    if (VivoDeleteDialog.this.mAlertDialog != null) {
                        synchronized (VivoDeleteDialog.this.mLock) {
                            if (VivoDeleteDialog.this.mAlertDialog != null) {
                                Button button = VivoDeleteDialog.this.mAlertDialog.getButton(-2);
                                button.setText(VivoDeleteDialog.this.getNegativeButtonText(ConfirmTimerTask.this.mTimeLeft / ConfirmTimerTask.this.mPeriod));
                            }
                        }
                    }
                }
            });
        }
    }

    private void VCD_VC(int click, int delay) {
        VivoCollectData mVCD = VivoCollectData.getInstance(this.mContext);
        try {
            long curTime = System.currentTimeMillis();
            HashMap<String, String> params = new HashMap<>();
            params.put("click", click + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            params.put("delay", delay + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            if (mVCD != null && mVCD.getControlInfo("262")) {
                mVCD.writeData("262", "2622", curTime, curTime, 0L, 1, params);
            }
            EventTransfer mVcode = EventTransfer.getInstance();
            if (mVcode != null) {
                mVcode.singleEvent("262", "2622", curTime, 0L, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
    }
}