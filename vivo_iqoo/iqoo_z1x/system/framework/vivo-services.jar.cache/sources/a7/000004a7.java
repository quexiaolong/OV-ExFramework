package com.android.server.trust;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.SystemProperties;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoTrustManagerServiceImpl implements IVivoTrustManagerService {
    static boolean DEBUG = false;
    private static final boolean IS_ENG = Build.TYPE.equals("eng");
    private static final boolean IS_LOG_CTRL_OPEN;
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final long ONEDAY = 86400000;
    private static final String TAG = "VivoTrustManagerServiceImpl";
    private static final String VIVO_LOG_ACTION = "android.vivo.bbklog.action.CHANGED";
    private Context mContext;
    private SmartAppReceiver mSmartAppReceiver;
    private TrustManagerService mTrustManagerService;
    private long preTime = 0;

    static {
        boolean equals = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
        IS_LOG_CTRL_OPEN = equals;
        DEBUG = equals || IS_ENG;
    }

    public VivoTrustManagerServiceImpl(TrustManagerService trustManagerSerivce, Context context, Handler handler) {
        this.mContext = context;
        this.mTrustManagerService = trustManagerSerivce;
        sendRegisterSmartAppBroast();
    }

    public boolean debugStatus() {
        return DEBUG;
    }

    public void sendBroadcastToSmartunlockApp() {
        Intent mIntent = new Intent();
        mIntent.setAction("android.vivo.smartunlock.action.DATA");
        mIntent.setPackage("com.vivo.smartunlock");
        this.mContext.sendBroadcast(mIntent);
    }

    public ComponentName removeDefaultIfNotExist(ComponentName defaultAgent, List<ResolveInfo> resolveInfos) {
        if (defaultAgent == null) {
            VSlog.i(TAG, "no default agent");
            return null;
        }
        if (resolveInfos != null) {
            for (ResolveInfo resolveInfo : resolveInfos) {
                ComponentName componentName = getComponentName(resolveInfo);
                if (defaultAgent.equals(componentName)) {
                    VSlog.i(TAG, "found defined default agent");
                    return defaultAgent;
                }
            }
        }
        VSlog.i(TAG, "not found defined default agent, remove it");
        return null;
    }

    private ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    public void sendRegisterSmartAppBroast() {
        IntentFilter filter = new IntentFilter();
        this.mSmartAppReceiver = new SmartAppReceiver();
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.vivo.bbklog.action.CHANGED");
        this.mContext.registerReceiver(this.mSmartAppReceiver, filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSmartAppData() {
        long nowTime = System.currentTimeMillis();
        long diffTime = nowTime - this.preTime;
        if (diffTime >= 86400000) {
            this.preTime = nowTime;
            sendRegisterSmartAppBroast();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SmartAppReceiver extends BroadcastReceiver {
        private SmartAppReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VSlog.i(VivoTrustManagerServiceImpl.TAG, "onReceive action is:" + action);
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                VivoTrustManagerServiceImpl.this.updateSmartAppData();
            } else if ("android.vivo.bbklog.action.CHANGED".equals(action)) {
                boolean status = "on".equals(intent.getStringExtra("adblog_status"));
                VivoTrustManagerServiceImpl.DEBUG = status;
                VivoTrustManagerServiceImpl.this.mTrustManagerService.updateDebug(VivoTrustManagerServiceImpl.DEBUG);
            }
        }
    }
}