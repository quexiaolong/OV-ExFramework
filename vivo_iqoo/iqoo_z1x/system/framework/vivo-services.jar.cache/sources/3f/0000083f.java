package com.vivo.services.superresolution;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.wm.SuperResolutionWindowController;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.superresolution.SuperResolutionConfig;
import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import vivo.app.superresolution.IPackageSettingStateChangeListener;
import vivo.app.superresolution.ISuperResolutionManager;
import vivo.app.superresolution.ISuperResolutionStateCallback;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SuperResolutionManagerService extends ISuperResolutionManager.Stub {
    public static final String TAG = "SuperResolutionManagerService";
    private static volatile SuperResolutionManagerService mInstance;
    private static SuperResolutionManagerServiceHelper sHelper;
    private ActivityManager mActivityManager;
    private AlertDialog mAlertDialogTip;
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mLastScreenSRState;
    private Handler mMainHandler;
    private PackageManager mPackageManager;
    private PowerManager mPowerManager;
    private SRHandler mSRHandler;
    private HandlerThread mSRHandlerThread;
    private int mStopSceneType;
    private SuperResolutionReceiver mSuperResolutionReceiver;
    private static final ArrayList<String> mSuportActivityList = Constant.sSuportActivityList;
    private static final ArrayList<String> mSupportLauncherActivityList = Constant.mSuportLauncherActivityList;
    private static final ArrayList<String> mSplashActivity = Constant.mSplashActivity;
    private static Map<String, Integer> mAppSwitch = Constant.sAppSwitch;
    private static Map<String, Integer> mAppIsAlive = new HashMap();
    private String mLastAppName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private String mLastActivityName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private boolean isInit = false;
    private final Object mAppSwitchLock = new Object();
    private final Object mAppIsAliveLock = new Object();
    private Map<String, ISuperResolutionStateCallback> mCallbacks = new HashMap();
    private Map<String, IPackageSettingStateChangeListener> mListeners = new HashMap();
    private String NO_MORE_NOTICE = "_no_more_notice";
    private String ALL_NO_MORE_NOTICE = "all_no_more_notice";
    private SuperResolutionWindowController.WindowChangeListener mWindowChangeListener = new SuperResolutionWindowController.WindowChangeListener() { // from class: com.vivo.services.superresolution.SuperResolutionManagerService.1
        public void windowChange() {
            if (!SuperResolutionManagerService.this.isInit) {
                SuperResolutionManagerService.this.initService();
            }
            if (SuperResolutionManagerService.this.mSRHandler.hasMessages(4)) {
                VSlog.d(SuperResolutionManagerService.TAG, "windowChange: has MSG_WINDOW_CHANGE message");
            }
            SuperResolutionManagerService.this.mSRHandler.sendEmptyMessage(4);
        }
    };
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.vivo.services.superresolution.SuperResolutionManagerService.2
        public void onForegroundActivitiesChanged(int i, int i1, boolean b) throws RemoteException {
        }

        public void onProcessDied(int i, int i1) throws RemoteException {
            String app = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (SuperResolutionManagerService.this.mPackageManager != null) {
                app = SuperResolutionManagerService.this.mPackageManager.getNameForUid(i1);
            }
            if (SuperResolutionManagerService.this.isAppEnable(app)) {
                SuperResolutionManagerService.this.unRegisterSuperResolutionStateChange(null, 0, app);
            }
            if (SuperResolutionManagerService.mAppSwitch.containsKey(app)) {
                VSlog.e(SuperResolutionManagerService.TAG, "onProcessDied: app = " + app + "   pid = " + i);
                synchronized (SuperResolutionManagerService.this.mAppIsAliveLock) {
                    SuperResolutionManagerService.mAppIsAlive.put(app, 0);
                }
                VSlog.d(SuperResolutionManagerService.TAG, "show Dialog:mAppIsAlive.get(app)->" + SuperResolutionManagerService.mAppIsAlive.get(app));
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SRHandler extends Handler {
        SRHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            VSlog.d(SuperResolutionManagerService.TAG, "SRHandler: msg=" + msg.what + "  obj=" + msg.obj);
            int i = msg.what;
            if (i == 1) {
                SuperResolutionManagerService superResolutionManagerService = SuperResolutionManagerService.this;
                superResolutionManagerService.mStopSceneType = superResolutionManagerService.getStopSceneType();
                SuperResolutionManagerService.sHelper.initStopScene(SuperResolutionManagerService.this.mStopSceneType);
            } else if (i == 20) {
                SuperResolutionManagerService.sHelper.resetReportTimer();
                SuperResolutionManagerService.sHelper.resetUpdateAlgorithmTimer();
                SuperResolutionManagerService.sHelper.resetReportAccuTimer();
            } else if (i == 3) {
                String stopReason = String.valueOf(msg.obj);
                Map<String, String> map = new HashMap<>();
                map.put("close_high_quality", stopReason);
                DataReport.reportSwitchOff(map);
            } else if (i != 4) {
                switch (i) {
                    case 11:
                        SuperResolutionManagerService.sHelper.getUnifiedConfig();
                        return;
                    case 12:
                        synchronized (SuperResolutionManagerService.this.mAppSwitchLock) {
                            String packageName = String.valueOf(msg.obj);
                            if (SuperResolutionManagerService.this.isAppEnable(packageName) && (SuperResolutionManagerService.mAppSwitch.get(packageName) == null || ((Integer) SuperResolutionManagerService.mAppSwitch.get(packageName)).intValue() != 1)) {
                                SuperResolutionManagerService.mAppSwitch.put(packageName, 1);
                                VSlog.d(SuperResolutionManagerService.TAG, "handleMessage: mAppSwitch.put(packageName,1)");
                                SuperResolutionManagerService.sHelper.updateSettingAppSwitches();
                            }
                        }
                        return;
                    case 13:
                        String packageName2 = String.valueOf(msg.obj);
                        if (SuperResolutionManagerService.sHelper.mInstalledPackages != null) {
                            SuperResolutionManagerService.sHelper.mInstalledPackages.remove(packageName2);
                            return;
                        }
                        return;
                    case 14:
                        SuperResolutionManagerService.sHelper.registerReportTimer();
                        return;
                    default:
                        return;
                }
            } else {
                SuperResolutionManagerService.this.onWindowChange();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SRHandler getSRHandler() {
        if (this.mSRHandler == null) {
            this.mSRHandler = new SRHandler(this.mSRHandlerThread.getLooper());
        }
        return this.mSRHandler;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onWindowChange() {
        List<ActivityManager.RunningTaskInfo> runningTasks;
        VSlog.d(TAG, "onWindowChange: init = " + this.isInit);
        ComponentName cn = null;
        ActivityManager activityManager = this.mActivityManager;
        if (activityManager != null && (runningTasks = activityManager.getRunningTasks(1)) != null && runningTasks.size() > 0) {
            cn = runningTasks.get(0).topActivity;
        }
        if (cn != null) {
            String curPackage = cn.getPackageName();
            String curActivity = cn.getClassName();
            VSlog.d(TAG, " windowChange: window change " + curPackage + " - " + curActivity);
            if (!this.mLastActivityName.equals(curActivity)) {
                doActivityChange(curPackage, curActivity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getStopSceneType() {
        int stopSceneType = Settings.Global.getInt(this.mContentResolver, Constant.SETTING_STOP_TYPE, 0);
        VSlog.d(TAG, "getStopSceneType 0:" + stopSceneType);
        if (stopSceneType == 1 || stopSceneType == 3 || stopSceneType == 4 || stopSceneType == 0) {
            VSlog.d(TAG, "getStopSceneType 1:" + stopSceneType);
            return stopSceneType;
        }
        return 0;
    }

    public static SuperResolutionManagerService getInstance(Context context) {
        if (mInstance == null) {
            synchronized (SuperResolutionManagerService.class) {
                if (mInstance == null) {
                    mInstance = new SuperResolutionManagerService(context);
                }
            }
        }
        return mInstance;
    }

    private SuperResolutionManagerService(Context context) {
        this.mSRHandler = null;
        if (!isDeviceSupportSuperResolution()) {
            return;
        }
        this.mContext = context;
        this.mActivityManager = (ActivityManager) context.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        this.mPackageManager = this.mContext.getPackageManager();
        this.mContentResolver = this.mContext.getContentResolver();
        this.mMainHandler = new Handler(Looper.getMainLooper());
        HandlerThread handlerThread = new HandlerThread("SuperResolution");
        this.mSRHandlerThread = handlerThread;
        handlerThread.start();
        this.mSRHandler = new SRHandler(this.mSRHandlerThread.getLooper());
        SuperResolutionWindowController.getInstance().registerWindowChangeListener(this.mWindowChangeListener);
        VSlog.d(TAG, "SuperResolutionManagerService: service registerWindowChangeListener");
        sHelper = new SuperResolutionManagerServiceHelper();
        createDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initService() {
        String[] strArr;
        try {
            VSlog.d(TAG, "initService: ");
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            VSlog.e(TAG, " exception = " + e.getMessage());
        }
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        sHelper.initHelper(mInstance, this.mContext);
        registerReceiver();
        this.mSRHandler.sendEmptyMessage(1);
        if (SuperResolutionConfig.IS_SUPPORT_SUPER_RESOLUTION_NEW_DEVICE) {
            Constant.SUPPORT_APP = Constant.NEW_DEVICE_SUPPORT_APP;
        }
        this.isInit = true;
        if (Constant.PLATFORM.equals("SM8350")) {
            Constant.SUPPORT_APP = Constant.SM8350_SUPPORT_APP;
        }
        for (String app : Constant.SUPPORT_APP) {
            mAppIsAlive.put(app, 0);
        }
        mAppSwitch.put(Constant.APP_WEIXIN, 0);
        VSlog.d(TAG, "SuperResolutionManagerService: 屏蔽微信");
        sHelper.updateSettingAppSwitches();
    }

    private void doActivityChange(String curApp, String curActivity) {
        VSlog.d(TAG, "doActivityChange: curApp=" + curApp + "  curActivity=" + curActivity);
        int curState = getSRState(curApp, curActivity);
        StringBuilder sb = new StringBuilder();
        sb.append("doActivityChange: curState=");
        sb.append(curState);
        VSlog.d(TAG, sb.toString());
        dealSpecialCases(this.mLastActivityName, curActivity);
        if (curActivity.equals(Constant.ACTIVITY_LAUNCHER) || curActivity.equals("com.vivo.upslide.recents.RecentsActivity")) {
            hideDialog();
        }
        if ("com.android.camera.CameraActivity".equals(curActivity)) {
            sHelper.postClearAllHandlesRunnable();
        }
        if (this.mLastScreenSRState != 0 || curState != 0) {
            if (this.mLastScreenSRState == 1 && curState == 0) {
                notifyStateChanged(0, this.mLastAppName, this.mLastActivityName);
            } else if (this.mLastScreenSRState == 0 && curState == 1) {
                notifyStateChanged(1, curApp, curActivity);
            } else if (this.mLastScreenSRState == 1 && curState == 1) {
                notifyStateChanged(0, this.mLastAppName, this.mLastActivityName);
                notifyStateChanged(1, curApp, curActivity);
            }
        }
        boolean isHighTempureStop = isHighTempureStop(curApp, curActivity);
        VSlog.d(TAG, "doActivityChange: curState = " + curState + "  isHightempureStop = " + isHighTempureStop);
        if (curState == 0 && isHighTempureStop) {
            highTemperatureToast();
        }
        VSlog.d(TAG, "judge show Dialog start");
        boolean isCanShowDialog = isCanShowDialog(curApp, curActivity);
        VSlog.d(TAG, "show Dialog : isCanShowDialog->" + isCanShowDialog);
        if (isCanShowDialog) {
            this.mMainHandler.post(new Runnable() { // from class: com.vivo.services.superresolution.SuperResolutionManagerService.3
                @Override // java.lang.Runnable
                public void run() {
                    SuperResolutionManagerService.this.showDialog();
                }
            });
        }
        VSlog.d(TAG, "judge show Dialog end");
        this.mLastActivityName = curActivity;
        this.mLastAppName = curApp;
        this.mLastScreenSRState = curState;
        if (!mSplashActivity.contains(curActivity)) {
            synchronized (this.mAppIsAliveLock) {
                mAppIsAlive.put(this.mLastAppName, 1);
            }
        }
    }

    private void dealSpecialCases(String lastActivity, String curActivity) {
        if (Constant.ACTIVITY_WEIXIN_VIDEO.equals(lastActivity)) {
            sHelper.notifyNativeSwitchChange(0, lastActivity, 1);
        }
        if (Constant.ACTIVITY_WEIXIN_VIDEO.equals(curActivity)) {
            sHelper.notifyNativeSwitchChange(1, curActivity, 1);
        }
        if (Constant.ACTIVITY_WEIXIN_SHARE_VIDEO.equals(curActivity) || Constant.ACTIVITY_WEIXIN_FRIENDS_ZONE.equals(curActivity) || Constant.ACTIVITY_WEIXIN_SHARE_GALLERY.equals(curActivity)) {
            sHelper.notifyNativeSwitchChange(1, Constant.ACTIVITY_WEIXIN_NO_SR, 1);
            VSlog.d(TAG, "notifyNativeSwitchChange 进入不超分的页面:  " + curActivity);
        } else if (Constant.ACTIVITY_WEIXIN_SHARE_VIDEO.equals(lastActivity) || Constant.ACTIVITY_WEIXIN_FRIENDS_ZONE.equals(lastActivity) || Constant.ACTIVITY_WEIXIN_SHARE_GALLERY.equals(lastActivity)) {
            sHelper.notifyNativeSwitchChange(0, Constant.ACTIVITY_WEIXIN_NO_SR, 1);
            VSlog.d(TAG, "notifyNativeSwitchChange 退出不特殊处理的页面:  lastActivity=" + lastActivity + "  curActivity=" + curActivity);
        }
    }

    private void createDialog() {
        VSlog.d(TAG, "createDialog: start");
        View view = LayoutInflater.from(this.mContext).inflate(50528260, (ViewGroup) null);
        final CheckBox checkBox = (CheckBox) view.findViewById(51183780);
        if (checkBox == null) {
            return;
        }
        AlertDialog create = new AlertDialog.Builder(this.mContext, 51314692).setTitle(51249613).setView(view).setPositiveButton(51249610, new DialogInterface.OnClickListener() { // from class: com.vivo.services.superresolution.SuperResolutionManagerService.6
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                if (checkBox.isChecked()) {
                    Settings.Global.putInt(SuperResolutionManagerService.this.mContentResolver, SuperResolutionManagerService.this.ALL_NO_MORE_NOTICE, 1);
                } else {
                    Settings.Global.putInt(SuperResolutionManagerService.this.mContentResolver, SuperResolutionManagerService.this.ALL_NO_MORE_NOTICE, 0);
                }
                SuperResolutionManagerService.this.startSRSettingActivity();
            }
        }).setNegativeButton(51249608, new DialogInterface.OnClickListener() { // from class: com.vivo.services.superresolution.SuperResolutionManagerService.5
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                if (checkBox.isChecked()) {
                    Settings.Global.putInt(SuperResolutionManagerService.this.mContentResolver, SuperResolutionManagerService.this.ALL_NO_MORE_NOTICE, 1);
                } else {
                    Settings.Global.putInt(SuperResolutionManagerService.this.mContentResolver, SuperResolutionManagerService.this.ALL_NO_MORE_NOTICE, 0);
                }
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.vivo.services.superresolution.SuperResolutionManagerService.4
            @Override // android.content.DialogInterface.OnCancelListener
            public void onCancel(DialogInterface dialog) {
                if (checkBox.isChecked()) {
                    Settings.Global.putInt(SuperResolutionManagerService.this.mContentResolver, SuperResolutionManagerService.this.ALL_NO_MORE_NOTICE, 1);
                } else {
                    Settings.Global.putInt(SuperResolutionManagerService.this.mContentResolver, SuperResolutionManagerService.this.ALL_NO_MORE_NOTICE, 0);
                }
            }
        }).create();
        this.mAlertDialogTip = create;
        if (create.getWindow() != null) {
            this.mAlertDialogTip.getWindow().setType(2038);
        }
        this.mAlertDialogTip.setCanceledOnTouchOutside(false);
        this.mAlertDialogTip.setCancelable(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDialog() {
        VSlog.d(TAG, "showDialog: PLATFORM = " + Constant.PLATFORM + ", MODEL = " + Constant.MODEL);
        if (this.mAlertDialogTip != null && Constant.MODEL.equals("PD1981")) {
            this.mAlertDialogTip.show();
        }
    }

    private void hideDialog() {
        AlertDialog alertDialog = this.mAlertDialogTip;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startSRSettingActivity() {
        Intent intent = new Intent();
        intent.setPackage(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS);
        intent.setAction("com.vivo.settings.action.SUPER_RESOLUTION");
        intent.addFlags(268435456);
        try {
            this.mContext.startActivity(intent);
        } catch (Exception e) {
            VSlog.e(TAG, "startSRSettingActivity: not found settings");
        }
    }

    private void notifyStateChanged(int state, String app, String activity) {
        toastForTest(activity, state);
        ISuperResolutionStateCallback call = this.mCallbacks.get(app);
        if (call != null) {
            try {
                call.onChange(state, app, activity);
                VSlog.d(TAG, "notifyStateChanged  appName = " + app + "  activity = " + activity + "  state = " + state + "  wx call = " + this.mCallbacks.get(Constant.APP_WEIXIN));
            } catch (RemoteException e) {
                VSlog.e(TAG, "notifyStateChanged:  exception = " + e.getMessage());
            }
        }
    }

    private int getSRState(String app, String activity) {
        int i;
        synchronized (this.mAppSwitchLock) {
            VSlog.d(TAG, "getState: " + app + activity);
            i = 1;
            if (!sHelper.isMainSwitchOpen() || this.mStopSceneType != 0 || mAppSwitch.get(app) == null || mAppSwitch.get(app).intValue() != 1 || !mSuportActivityList.contains(activity)) {
                i = 0;
            }
        }
        return i;
    }

    private boolean isCanShowDialog(String app, String activity) {
        VSlog.d(TAG, "can show Dialog: app:" + app + ",activity:" + activity);
        synchronized (this.mAppSwitchLock) {
            if (mAppSwitch.get(app) == null) {
                VSlog.d(TAG, "mAppSwitch.get(app) == null");
                return false;
            }
            synchronized (this.mAppIsAliveLock) {
                if (mAppIsAlive.get(app) == null) {
                    VSlog.d(TAG, "mAppIsAlive.get(app) == null");
                    return false;
                }
                boolean isNoMoreNotice = Settings.Global.getInt(this.mContentResolver, this.ALL_NO_MORE_NOTICE, 0) == 0;
                boolean isMemcSwitchOpen = Settings.Global.getInt(this.mContentResolver, Constant.MEMC_SETTING_VALUE_MAIN, 0) == 1;
                boolean switchOn = sHelper.isMainSwitchOpen();
                StringBuilder sb = new StringBuilder();
                sb.append("show Dialog:!app.equals(Constant.APP_GALLERY)->");
                sb.append(!app.equals(Constant.APP_GALLERY));
                sb.append(",!switchOn->");
                sb.append(!switchOn);
                sb.append(",mSupportLauncherActivityList.contains(activity)->");
                sb.append(mSupportLauncherActivityList.contains(activity));
                sb.append(",mStopSceneType == 0->");
                sb.append(this.mStopSceneType == 0);
                sb.append(",mAppIsAlive.get(curApp) == 0->");
                sb.append(mAppIsAlive.get(app).intValue() == 0);
                sb.append(",isMemcSwitchClose->");
                sb.append(!isMemcSwitchOpen);
                sb.append(",isNoMoreNotice ->");
                sb.append(isNoMoreNotice);
                VSlog.d(TAG, sb.toString());
                return !switchOn && this.mStopSceneType == 0 && mSupportLauncherActivityList.contains(activity) && !app.equals(Constant.APP_GALLERY) && mAppIsAlive.get(app).intValue() == 0 && isNoMoreNotice && !isMemcSwitchOpen;
            }
        }
    }

    private boolean isHighTempureStop(String app, String activity) {
        boolean z;
        synchronized (this.mAppSwitchLock) {
            VSlog.d(TAG, "isHighTempureStop: mStopSceneType = " + this.mStopSceneType);
            z = true;
            if (!sHelper.isMainSwitchOpen() || this.mStopSceneType != 1 || ((mAppSwitch.get(app) == null || mAppSwitch.get(app).intValue() != 1 || !mSuportActivityList.contains(activity)) && !isBelongGallery(app, activity))) {
                z = false;
            }
        }
        return z;
    }

    private boolean isBelongGallery(String app, String activity) {
        return Constant.APP_GALLERY.equals(app) && (Constant.ACTIVITY_GALLERY.equals(activity) || Constant.ACTIVITY_GALLERY_TAB.equals(activity) || Constant.ACTIVITY_GALLERY_NEW.equals(activity));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appSwitchChangeCallBack(String value) {
        try {
            JSONObject object = new JSONObject(value);
            synchronized (this.mAppSwitchLock) {
                for (String name : mAppSwitch.keySet()) {
                    int res = ((Integer) object.get(name)).intValue();
                    if (mAppSwitch.get(name) != null && mAppSwitch.get(name).intValue() != res) {
                        mAppSwitch.put(name, Integer.valueOf(res));
                        VSlog.d(TAG, "doAppSwitchChange: app=" + name + "  value=" + res);
                        IPackageSettingStateChangeListener listener = this.mListeners.get(name);
                        if (listener != null) {
                            VSlog.d(TAG, "doAppSwitchChange: " + name + " listener change");
                            try {
                                listener.onChange(name, res, 0);
                            } catch (Exception e) {
                                VSlog.d(TAG, "Remote exception while listen call doSwitchChange message = " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (JSONException e2) {
            VSlog.e(TAG, "doAppSwitchChange: json parse exception ");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void mainSwitchChangeCallBack() {
        synchronized (this.mAppSwitchLock) {
            for (String name : mAppSwitch.keySet()) {
                IPackageSettingStateChangeListener listener = this.mListeners.get(name);
                if (listener != null) {
                    int i = 1;
                    if (mAppSwitch.get(name).intValue() == 1) {
                        VSlog.d(TAG, "doSwitchChange: " + name + " listener change");
                        try {
                            if (!sHelper.isMainSwitchOpen()) {
                                i = 0;
                            }
                            listener.onChange(name, i, 0);
                        } catch (RemoteException e) {
                            VSlog.d(TAG, "Remote exception while IPackageSettingStateChangeListener call doSwicthChange message = " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private void toastForTest(String activity, int state) {
        if (!"1".equals(SystemProperties.get("super_resolution_toast"))) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(activity);
        sb.append(state == 0 ? "关闭超分" : "开启超分");
        String toast = sb.toString();
        toastTips(toast);
    }

    private void highTemperatureToast() {
        String toastRes;
        boolean isSupportMemc = checkSupportMemc();
        if (isSupportMemc) {
            toastRes = this.mContext.getString(51249633);
        } else {
            toastRes = this.mContext.getString(51249632);
        }
        VSlog.d(TAG, "highTemperatureToast: isSupportMemc=" + isSupportMemc);
        toastTips(toastRes);
        Settings.Global.putInt(this.mContentResolver, Constant.SETTING_VALUE_MAIN, 0);
    }

    private void toastTips(final String toast) {
        this.mMainHandler.post(new Runnable() { // from class: com.vivo.services.superresolution.SuperResolutionManagerService.7
            @Override // java.lang.Runnable
            public void run() {
                Toast.makeText(SuperResolutionManagerService.this.mContext, toast, 0).show();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAppEnable(String appName) {
        for (int i = 0; i < Constant.SUPPORT_APP.length; i++) {
            if (Constant.SUPPORT_APP[i].equals(appName)) {
                return true;
            }
        }
        return false;
    }

    public void registerPackageSettingStateChangeListener(IPackageSettingStateChangeListener listener, String packageName) {
        VSlog.d(TAG, "registerPackageSettingStateChangeListener: packageName = " + packageName);
        if (!isAppEnable(packageName)) {
            return;
        }
        if (listener != null) {
            this.mListeners.put(packageName, listener);
            try {
                synchronized (this.mAppSwitchLock) {
                    listener.onChange(packageName, sHelper.isMainSwitchOpen() ? mAppSwitch.get(packageName).intValue() : 0, 0);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("registerPackageSettingStateChangeListener: packageName = ");
                sb.append(packageName);
                sb.append("  switch = ");
                sb.append(sHelper.isMainSwitchOpen() ? mAppSwitch.get(packageName).intValue() : 0);
                sb.append("  reason = ");
                sb.append(0);
                VSlog.d(TAG, sb.toString());
            } catch (RemoteException e) {
                VSlog.d(TAG, "Remote exception while registerPackageSettingStateChangeListener message = " + e.getMessage());
            }
        }
        VSlog.d(TAG, "registerPackageSettingStateChangeListener: success size = " + this.mListeners.size());
    }

    public void unRegisterPackageSettingStateChangeListener(IPackageSettingStateChangeListener listener, String packageName) {
        Map<String, IPackageSettingStateChangeListener> map = this.mListeners;
        if (map != null) {
            map.put(packageName, null);
            VSlog.d(TAG, "unRegisterPackageSettingStateChangeListener: success listener size = " + this.mListeners.size());
        }
    }

    public void setSuperResolutionStopState(int type, boolean isDisable) {
        long token = Binder.clearCallingIdentity();
        try {
            VSlog.d(TAG, "setSuperResolutionStopState type = " + type + "; isDisable = " + isDisable + " isOpenHightempureForbid = " + sHelper.isOpenHighTempureForbid());
            if (type == 1 && sHelper.isOpenHighTempureForbid() != 1 && isDisable) {
                return;
            }
            if (type == 5 || type == 10) {
                VSlog.d(TAG, "setSuperResolutionStopState: float ball/setting isDisable=" + isDisable);
                if (isDisable) {
                    Settings.Global.putInt(this.mContentResolver, Constant.SETTING_VALUE_MAIN, 0);
                    VSlog.d(TAG, "setSuperResolutionStopState: super resolution is closed by float ball/setting");
                } else {
                    boolean isMemcSwitchOpen = Settings.Global.getInt(this.mContentResolver, Constant.MEMC_SETTING_VALUE_MAIN, 0) == 1;
                    if (sHelper.checkIsHighTemperature()) {
                        if (type == 10 || isMemcSwitchOpen) {
                            VSlog.d(TAG, "setSuperResolutionStopState: in setting app, close memc if sr detected a high temperature");
                            Settings.Global.putInt(this.mContentResolver, Constant.MEMC_SETTING_VALUE_MAIN, 0);
                        }
                        highTemperatureToast();
                        VSlog.d(TAG, "setSuperResolutionStopState: float ball/setting open super resolution, but high temperature");
                    } else {
                        if (isMemcSwitchOpen) {
                            Settings.Global.putInt(this.mContentResolver, Constant.MEMC_SETTING_VALUE_MAIN, 0);
                        }
                        Settings.Global.putInt(this.mContentResolver, Constant.SETTING_VALUE_MAIN, 1);
                        Settings.Global.putInt(this.mContentResolver, Constant.SUPER_RESOLUTION_FIRST_APP, 1);
                    }
                    VSlog.d(TAG, "setSuperResolutionStopState: super resolution is opened by float ball/setting");
                }
                this.mLastScreenSRState = getSRState(this.mLastAppName, this.mLastActivityName);
                VSlog.d(TAG, "getState: mLastScreenSRState=" + this.mLastScreenSRState);
                return;
            }
            if (isDisable) {
                VSlog.d(TAG, "setSuperResolutionStopState: current mStopSceneType=" + this.mStopSceneType);
                if (type == 1 && (this.mStopSceneType == 3 || this.mStopSceneType == 0)) {
                    int sRState = getSRState(this.mLastAppName, this.mLastActivityName);
                    this.mLastScreenSRState = sRState;
                    if (sRState == 1) {
                        highTemperatureToast();
                        notifyStateChanged(0, this.mLastAppName, this.mLastActivityName);
                        Message msg = Message.obtain();
                        msg.what = 3;
                        msg.obj = "4";
                        this.mSRHandler.sendMessage(msg);
                        VSlog.d(TAG, "setSuperResolutionStopState: high temperature close super resolution");
                    }
                    this.mStopSceneType++;
                    VSlog.d(TAG, "setSuperResolutionStopState: high temperature caused, mStopSceneType=" + this.mStopSceneType);
                } else if (type == 3 && (this.mStopSceneType == 1 || this.mStopSceneType == 0)) {
                    if (this.mLastScreenSRState == 1) {
                        Settings.Global.putInt(this.mContentResolver, Constant.SETTING_VALUE_MAIN, 0);
                        notifyStateChanged(0, this.mLastAppName, this.mLastActivityName);
                        Message msg2 = Message.obtain();
                        msg2.what = 3;
                        msg2.obj = "2";
                        this.mSRHandler.sendMessage(msg2);
                        VSlog.d(TAG, "setSuperResolutionStopState: low power close super resolution");
                    }
                    this.mStopSceneType += 3;
                    VSlog.d(TAG, "setSuperResolutionStopState: low power caused, mStopSceneType=" + this.mStopSceneType);
                }
                Settings.Global.putInt(this.mContentResolver, Constant.SETTING_STOP_TYPE, this.mStopSceneType);
                VSlog.d(TAG, "setSuperResolutionStopState: put mStopSceneType to settings global");
            } else {
                if (type == 1 && (this.mStopSceneType == 1 || this.mStopSceneType == 4)) {
                    this.mStopSceneType--;
                    Settings.Global.putInt(this.mContentResolver, Constant.SETTING_VALUE_MAIN, 1);
                    VSlog.d(TAG, "setSuperResolutionStopState: high temperature removed, mStopSceneType=" + this.mStopSceneType);
                } else if (type == 3 && (this.mStopSceneType == 3 || this.mStopSceneType == 4)) {
                    this.mStopSceneType -= 3;
                    VSlog.d(TAG, "setSuperResolutionStopState: low power removed, mStopSceneType=" + this.mStopSceneType);
                }
                this.mLastScreenSRState = getSRState(this.mLastAppName, this.mLastActivityName);
                Settings.Global.putInt(this.mContentResolver, Constant.SETTING_STOP_TYPE, this.mStopSceneType);
                notifyStateChanged(this.mLastScreenSRState, this.mLastAppName, this.mLastActivityName);
            }
            VSlog.d(TAG, "setSuperResolutionStopState: mStopSceneType = " + this.mStopSceneType);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public int getSuperResolutionStopState() {
        VSlog.d(TAG, "getSuperResolutionStopState" + this.mStopSceneType);
        return this.mStopSceneType;
    }

    public int getCurrentState() {
        VSlog.d(TAG, "getCurrentState" + this.mLastScreenSRState);
        return this.mLastScreenSRState;
    }

    public int getPackageSettingState(String name) {
        if (!sHelper.isOpenAppShare()) {
            VSlog.d(TAG, "getPackageSettingState " + name + " state is " + mAppSwitch.get(name));
            return mAppSwitch.get(name).intValue();
        }
        VSlog.d(TAG, "getPackageSettingState  app share is running, stop SR. " + name);
        return 10;
    }

    public void putPackageSettingState(String name, int value) {
        synchronized (this.mAppSwitchLock) {
            Integer oldValueInteger = mAppSwitch.get(name);
            int oldValue = oldValueInteger != null ? oldValueInteger.intValue() : 1;
            VSlog.i(TAG, "putPackageSettingState: name=" + name + ", value=" + value + ", oldValue=" + oldValue);
            if (oldValue == value) {
                return;
            }
            if (oldValue == 0 && value == 1) {
                String superResolutionAppsAlphaAnimator = Settings.Global.getString(this.mContentResolver, Constant.SUPER_RESOLUTION_APPS_ALPHA_ANIMATOR);
                try {
                    JSONObject object = superResolutionAppsAlphaAnimator != null ? new JSONObject(superResolutionAppsAlphaAnimator) : new JSONObject();
                    object.put(name, value);
                    Settings.Global.putString(this.mContentResolver, Constant.SUPER_RESOLUTION_APPS_ALPHA_ANIMATOR, object.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            IPackageSettingStateChangeListener listener = this.mListeners.get(name);
            if (listener != null) {
                try {
                    listener.onChange(name, value, 0);
                } catch (RemoteException e2) {
                    VSlog.d(TAG, "Remote exception while listener call putPackageSettingState message = " + e2.getMessage());
                }
            }
            mAppSwitch.put(name, Integer.valueOf(value));
            sHelper.updateSettingAppSwitches();
            VSlog.d(TAG, "putPackageSettingState success 1");
        }
    }

    public void registerSuperResolutionStateChange(ISuperResolutionStateCallback call, int pid, String appName) {
        List<ActivityManager.RunningTaskInfo> runningTasks;
        VSlog.d(TAG, "registerSuperResolutionStateChange: appName = " + appName);
        if (!isAppEnable(appName)) {
            return;
        }
        if (call != null) {
            this.mCallbacks.put(appName, call);
            String activityName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            ComponentName cn = null;
            ActivityManager activityManager = this.mActivityManager;
            if (activityManager != null && (runningTasks = activityManager.getRunningTasks(1)) != null && runningTasks.size() != 0) {
                cn = runningTasks.get(0).topActivity;
            }
            if (cn != null) {
                activityName = cn.getClassName();
            }
            int state = getSRState(appName, activityName);
            try {
                call.onChange(state, appName, this.mLastActivityName);
                VSlog.d(TAG, "registerSuperResolutionStateChange : state = " + state + "  appName = " + appName + "  mLastActivity = " + this.mLastActivityName);
            } catch (RemoteException e) {
                VSlog.d(TAG, "Remote exception while registerSuperResolutionStateChange message = " + e.getMessage());
            }
        }
        VSlog.d(TAG, "registerSurperResolutionStateChange: success size = " + this.mCallbacks.size());
    }

    public void unRegisterSuperResolutionStateChange(ISuperResolutionStateCallback call, int pid, String appName) {
        if (this.mCallbacks != null && isAppEnable(appName)) {
            this.mCallbacks.put(appName, null);
        }
        VSlog.d(TAG, "unRegisterSurperResolutionStateChange: success callback size = " + this.mCallbacks.size());
    }

    public boolean isDeviceSupportSuperResolution() {
        return SuperResolutionConfig.SUPPORT_SUPER_RESOLUTION;
    }

    public String[] getSupportApp() {
        VSlog.d(TAG, "getSupportApp" + Constant.PLATFORM);
        VSlog.d(TAG, "NOT_SUPPORT_WECHAT_SR");
        List<String> list = Arrays.asList(Constant.SUPPORT_APP);
        if (list.contains(Constant.APP_WEIXIN)) {
            List<String> arrayList = new ArrayList<>(list);
            arrayList.remove(Constant.APP_WEIXIN);
            Constant.SUPPORT_APP = (String[]) arrayList.toArray(new String[arrayList.size()]);
        }
        return Constant.SUPPORT_APP;
    }

    public String[] getNonSystemApp() {
        return Constant.THIRD_PARTY_APP;
    }

    public void notifyAppShareStateChanged(String appName, int appShareState) {
        IPackageSettingStateChangeListener listener = this.mListeners.get(appName);
        if (listener != null) {
            try {
                listener.onChange(appName, appShareState, 10);
                VSlog.d(TAG, "notifyAppShareStateChanged: " + appName + "  appShareState:" + appShareState);
            } catch (RemoteException e) {
                VSlog.d(TAG, "Remote exception while listener call notifyAppShareStateChanged, message = " + e.getMessage());
            }
        }
    }

    public int[] getOutputSize(int inWidth, int inHeight) {
        SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper = sHelper;
        if (superResolutionManagerServiceHelper != null) {
            return superResolutionManagerServiceHelper.getOutputSize(inWidth, inHeight);
        }
        return new int[0];
    }

    public long initSuperResolution(int FrameRate, int BitPerSecond, int[] inputSize, int[] inStride, int[] outStride, int format, String appInfo) {
        SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper = sHelper;
        if (superResolutionManagerServiceHelper != null) {
            return superResolutionManagerServiceHelper.initSuperResolution(FrameRate, BitPerSecond, inputSize, inStride, outStride, format, appInfo);
        }
        return -1L;
    }

    public int runSuperResolution(long handler, FileDescriptor fd, long size, int dataPos, boolean isNewVideo) {
        SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper = sHelper;
        if (superResolutionManagerServiceHelper != null) {
            return superResolutionManagerServiceHelper.runSuperResolution(handler, fd, size, dataPos, isNewVideo);
        }
        return -1;
    }

    public int releaseSuperResolution(long handler) {
        SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper = sHelper;
        if (superResolutionManagerServiceHelper != null) {
            return superResolutionManagerServiceHelper.releaseSuperResolution(handler);
        }
        return -1;
    }

    public int resizeBilinearCommon(FileDescriptor fd, long size, int imageFormat, int imageLocation, int inWidth, int inHeight, int inWidthStride, int inHeightStride, int outWidth, int outHeight, int outWidthStride, int outHeightStride) {
        SuperResolutionManagerServiceHelper superResolutionManagerServiceHelper = sHelper;
        if (superResolutionManagerServiceHelper != null) {
            return superResolutionManagerServiceHelper.resizeBilinearCommon(fd, size, imageFormat, imageLocation, inWidth, inHeight, inWidthStride, inHeightStride, outWidth, outHeight, outWidthStride, outHeightStride);
        }
        return -1;
    }

    private boolean checkSupportMemc() {
        boolean isSupportMemc = false;
        try {
            Class<?> manager = Class.forName("com.vivo.framework.memc.MemcManager");
            if (manager != null) {
                Method getInstance = manager.getMethod("getInstance", new Class[0]);
                Method isDeviceSupportMemc = manager.getMethod("isDeviceSupportMemc", new Class[0]);
                if (getInstance != null) {
                    Object memcManager = getInstance.invoke(null, new Object[0]);
                    if (isDeviceSupportMemc != null) {
                        isSupportMemc = ((Boolean) isDeviceSupportMemc.invoke(memcManager, new Object[0])).booleanValue();
                    }
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            VSlog.d(TAG, "checkSupportMemc: error");
        }
        VSlog.d(TAG, "checkSupportMemc: isSupportMemc=" + isSupportMemc);
        return isSupportMemc;
    }

    private void registerReceiver() {
        if (Constant.IS_OVERSEAS) {
            this.mSuperResolutionReceiver = new SuperResolutionReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
            this.mContext.registerReceiver(this.mSuperResolutionReceiver, intentFilter);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SuperResolutionReceiver extends BroadcastReceiver {
        SuperResolutionReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                VSlog.d(SuperResolutionManagerService.TAG, "SuperResolutionReceiver intent is null");
                return;
            }
            if (intent.getAction() == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(intent.getAction())) {
                VSlog.d(SuperResolutionManagerService.TAG, "SuperResolutionReceiver action is null");
            }
            VSlog.d(SuperResolutionManagerService.TAG, "SuperResolutionReceiver onReceiver action:" + intent.getAction());
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(intent.getAction())) {
                try {
                    boolean isPowerSave = SuperResolutionManagerService.this.mPowerManager.isPowerSaveMode();
                    SuperResolutionManagerService.this.setSuperResolutionStopState(3, isPowerSave);
                    VSlog.d(SuperResolutionManagerService.TAG, "onReceive: now is in save mode " + isPowerSave);
                } catch (Exception e) {
                    VSlog.e(SuperResolutionManagerService.TAG, " exception = " + e.getMessage());
                }
            }
        }
    }
}