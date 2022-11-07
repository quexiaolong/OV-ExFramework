package com.android.server.policy.key;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.key.VivoAIKeyHandler;
import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class VivoAIKeyLongPress {
    private static final String ACTION = "vivo.intent.action.JOVI_KEY_LONG_PRESS";
    private static final String ACTION_VIVO_POLICY_MANAGER_STATE_CHANGED = "vivo.app.action.POLICY_MANAGER_STATE_CHANGED";
    private static final String ORDER = "vivo.settings.longpress_order";
    private static final String TAG = "AIKeyLongPress";
    public static final String USER_LONG_PRESS_SELECT = "jovi_longpress_key_function";
    private static final String VALUES = "vivo.settings.longpress_values";
    private static final int VIVO_TRANSACTION_OPERATION_AI_KEY = 3210;
    private static final String VOICE_ACTION_PRPARE = "vivo.intent.action.JOVI_KEY_LONG_PREPARE";
    private Context mContext;
    private String mCurrentLongPressChoose;
    private UserManager mUserManager;
    private VivoAIKeyHandler mVivoAiKey;
    private final List<VivoAIKeyHandler.Target> targets = new ArrayList();
    private boolean mIsFbeProject = false;
    private String mPackageForLongPressCustom = null;
    private BroadcastReceiver mDPMReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.key.VivoAIKeyLongPress.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (VivoAIKeyLongPress.ACTION_VIVO_POLICY_MANAGER_STATE_CHANGED.equals(action)) {
                int poId = intent.getIntExtra("poId", 0);
                if (poId == 0 || poId == VivoAIKeyLongPress.VIVO_TRANSACTION_OPERATION_AI_KEY) {
                    VivoAIKeyLongPress vivoAIKeyLongPress = VivoAIKeyLongPress.this;
                    vivoAIKeyLongPress.mPackageForLongPressCustom = vivoAIKeyLongPress.getPackageForCustom();
                }
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoAIKeyLongPress(VivoAIKeyHandler handler, Context context) {
        this.mVivoAiKey = handler;
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLongPressChoose(String value) {
        this.mCurrentLongPressChoose = value;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getLongPressChoose() {
        return this.mCurrentLongPressChoose;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int updateData(Context context) {
        long debugStart = System.currentTimeMillis();
        List<ResolveInfo> infos = null;
        try {
            try {
                HashMap<String, ResolveInfo> resolveInfoHashMap = new HashMap<>();
                PackageManager packageManager = context.getPackageManager();
                Intent intent = new Intent(ACTION);
                infos = packageManager.queryIntentServices(intent, 786624);
                if (infos == null) {
                    infos = new ArrayList<>();
                }
                infos.addAll(packageManager.queryIntentActivities(intent, 786624));
                String signature = context.getPackageName();
                this.targets.clear();
                VivoAIKeyHandler.handleComponents(TAG, this.targets, infos, resolveInfoHashMap, packageManager, ORDER, ACTION, signature);
                this.mCurrentLongPressChoose = VivoAIKeyHandler.checkChooseData(TAG, this.targets, VALUES, this.mCurrentLongPressChoose, USER_LONG_PRESS_SELECT, context);
                VLog.d(TAG, "infos size = " + infos.size());
                if (VivoAIKeyHandler.isDebug()) {
                    VLog.d(TAG, "updateData SPENT " + (System.currentTimeMillis() - debugStart) + " ms");
                }
                if (infos == null) {
                    return 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (VivoAIKeyHandler.isDebug()) {
                    VLog.d(TAG, "updateData SPENT " + (System.currentTimeMillis() - debugStart) + " ms");
                }
                if (infos == null) {
                    return 0;
                }
            }
        } catch (Throwable th) {
            if (VivoAIKeyHandler.isDebug()) {
                VLog.d(TAG, "updateData SPENT " + (System.currentTimeMillis() - debugStart) + " ms");
            }
            if (infos == null) {
                return 0;
            }
        }
        return infos.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<VivoAIKeyHandler.Target> dispatchLongPress(VivoAIKeyHandler handler) {
        if (isSupportCustom()) {
            triggerCustomService(true);
            return new HashSet();
        }
        Intent intent = new Intent(ACTION);
        intent.putExtra("keyCode", VivoPolicyConstant.KEYCODE_AI);
        intent.putExtra("keyEvent", "ai_long_press");
        if (VivoAIKeyHandler.isDebug()) {
            VLog.v(TAG, "LONG PRESS target size = " + this.targets.size());
        }
        Set<VivoAIKeyHandler.Target> needEvents = new HashSet<>();
        for (VivoAIKeyHandler.Target target : this.targets) {
            try {
                if (target.metaData != null && !TextUtils.isEmpty(target.pkgName) && !TextUtils.isEmpty(target.componentName) && TextUtils.equals(target.action, ACTION)) {
                    ComponentName componentName = new ComponentName(target.pkgName, target.componentName);
                    String longPressMode = target.metaData.getString(VALUES);
                    boolean isAlwaysLongpress = TextUtils.equals(target.metaData.getString(VivoAIKeyShortPress.ALWAYS_LONG_PRESS), "yes");
                    VLog.v(TAG, componentName + ", isAlwaysLongpress = " + isAlwaysLongpress + ", longPressMode = " + longPressMode + ", mCurrentLongPressChoose = " + this.mCurrentLongPressChoose);
                    if (longPressMode != null && !isAlwaysLongpress && TextUtils.equals(this.mCurrentLongPressChoose, longPressMode)) {
                        handler.sendToTarget(componentName, intent, target, 1002);
                        needEvents.add(target);
                        if (VivoAIKeyHandler.isDebug()) {
                            VLog.v(TAG, "=> send long press to Component  " + componentName);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return needEvents;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disaptchUp(Set<VivoAIKeyHandler.Target> needUpEvents, VivoAIKeyHandler handler) {
        if (isSupportCustom()) {
            triggerCustomService(false);
            return;
        }
        try {
            Intent intent = new Intent(ACTION);
            intent.putExtra("keyCode", VivoPolicyConstant.KEYCODE_AI);
            intent.putExtra("keyEvent", "ai_up");
            for (VivoAIKeyHandler.Target target : needUpEvents) {
                if (target.metaData != null && !TextUtils.isEmpty(target.pkgName) && !TextUtils.isEmpty(target.componentName) && TextUtils.equals(target.action, ACTION) && !TextUtils.equals(target.metaData.getString(VivoAIKeyShortPress.ALWAYS_LONG_PRESS), "yes")) {
                    ComponentName componentName = new ComponentName(target.pkgName, target.componentName);
                    handler.sendToTarget(componentName, intent, target, 1001);
                    if (VivoAIKeyHandler.isDebug()) {
                        VLog.v(TAG, "=> send up to Component  " + componentName);
                    }
                }
            }
        } catch (Exception e) {
            VLog.d(TAG, "error " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendVoicePreload(Context context) {
        if (isSupportCustom()) {
            return;
        }
        try {
            if ("voice".equals(this.mCurrentLongPressChoose) && context != null) {
                Intent intent = new Intent(VOICE_ACTION_PRPARE);
                intent.setPackage("com.vivo.agent");
                context.startService(intent);
                VLog.d("hsq", "start prpare action");
            }
        } catch (Exception e) {
        }
    }

    public void systemReady() {
        this.mIsFbeProject = "file".equals(SystemProperties.get("ro.crypto.type", "unknow"));
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        DevicePolicyManager mDpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        int type = mDpm.getCustomType();
        if (type > 0) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_VIVO_POLICY_MANAGER_STATE_CHANGED);
            this.mContext.registerReceiverAsUser(this.mDPMReceiver, UserHandle.ALL, filter, null, null);
            this.mPackageForLongPressCustom = getPackageForCustom();
        }
    }

    private boolean isSupportCustom() {
        boolean isSupport = false;
        String str = this.mPackageForLongPressCustom;
        if (str != null && !(isSupport = isPackageValid(str))) {
            VLog.d(TAG, "cannot find package or corresponding service:" + this.mPackageForLongPressCustom);
        }
        return isSupport;
    }

    private void triggerCustomService(boolean isDown) {
        boolean userKeyUnlocked = this.mUserManager.isUserUnlocked();
        if (this.mIsFbeProject && !userKeyUnlocked) {
            try {
                PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
                IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
                powerManager.wakeUp(SystemClock.uptimeMillis(), TAG);
                windowManager.dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
                VLog.d(TAG, "Fbe project and unlocked, request dismiss keyguard and return.");
                return;
            } catch (RemoteException e) {
                VLog.d(TAG, "Fbe project and unlocked, dismiss keyguard cause exception: " + e);
                return;
            }
        }
        try {
            Intent intent = new Intent();
            intent.setAction(ACTION);
            intent.setPackage(this.mPackageForLongPressCustom);
            intent.putExtra("action", isDown ? 0 : 1);
            this.mContext.startForegroundServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e2) {
            VLog.e(TAG, "Fail to start service " + e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getPackageForCustom() {
        DevicePolicyManager mDpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        Bundle bundle = mDpm.getInfoDeviceTransaction(null, VIVO_TRANSACTION_OPERATION_AI_KEY, null);
        if (bundle == null) {
            return null;
        }
        return bundle.getString("package_name");
    }

    private boolean isPackageValid(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return false;
        }
        Intent intent = new Intent(ACTION);
        PackageManager pms = this.mContext.getPackageManager();
        List<ResolveInfo> list = pms.queryIntentServices(intent, 786496);
        if (list == null) {
            return false;
        }
        for (ResolveInfo resolveInfo : list) {
            if (pkg.equals(resolveInfo.serviceInfo.packageName)) {
                return true;
            }
        }
        return false;
    }
}