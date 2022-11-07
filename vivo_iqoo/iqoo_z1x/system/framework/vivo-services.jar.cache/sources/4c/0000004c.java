package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import com.android.internal.content.PackageMonitor;
import com.android.server.IVivoServiceWatcher;
import com.android.server.VivoServiceWatcherImpl;
import com.android.server.am.VivoAmsImpl;
import com.android.server.location.VivoLocThread;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;

/* loaded from: classes.dex */
public class VivoServiceWatcherImpl implements IVivoServiceWatcher {
    private static final String AOSP_FUSD_NAME = "com.android.location.fused";
    private static final String DEMESTIC_NLP_NAME = "com.vivo.multinlp";
    private static final String FUSED_LOCATION_SERVICE_ACTION = "com.android.location.service.FusedLocationProvider";
    private static final String GMS_NLP_NAME = "com.google.android.gms";
    public static final String TAG = "VivoServiceWatcher";
    Handler mHandler;
    BroadcastReceiver mVivoBroadcastReceiver;
    IntentFilter mVivoIntentFilter;
    private boolean isOversea = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
    int mFakeMcc = -1;
    boolean mShouldBindGms = false;
    boolean mCurrentBindGms = false;
    IVivoServiceWatcher.Callback mCallback = null;
    Context mContext = null;
    String mOriginalPackageName = null;
    boolean mOriginalPackageNameInited = false;
    private boolean mGTSTest = false;
    private PackageMonitor mPackageMonitor = new AnonymousClass1();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.VivoServiceWatcherImpl$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends PackageMonitor {
        AnonymousClass1() {
        }

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            return super.onPackageChanged(packageName, uid, components);
        }

        public void onPackageRemoved(String packageName, int uid) {
            super.onPackageRemoved(packageName, uid);
        }

        public void onPackageAdded(String packageName, int uid) {
            if (VivoServiceWatcherImpl.this.isOversea && packageName != null && (packageName.equals("com.android.preconditions.gts") || packageName.equals("com.google.android.permission.gts"))) {
                VLog.d(VivoServiceWatcherImpl.TAG, "GTS pkg name:" + packageName);
                VivoServiceWatcherImpl.this.mGTSTest = true;
                if (VivoServiceWatcherImpl.this.mCallback != null && VivoServiceWatcherImpl.this.mHandler != null) {
                    VivoServiceWatcherImpl.this.mHandler.post(new Runnable() { // from class: com.android.server.-$$Lambda$VivoServiceWatcherImpl$1$DndBPDAl437W0PZ5P7M6tTBVZnI
                        @Override // java.lang.Runnable
                        public final void run() {
                            VivoServiceWatcherImpl.AnonymousClass1.this.lambda$onPackageAdded$0$VivoServiceWatcherImpl$1();
                        }
                    });
                }
            }
            super.onPackageAdded(packageName, uid);
        }

        public /* synthetic */ void lambda$onPackageAdded$0$VivoServiceWatcherImpl$1() {
            VivoServiceWatcherImpl.this.mCallback.onMccChanged(false);
        }
    }

    public VivoServiceWatcherImpl() {
        this.mHandler = null;
        this.mHandler = new Handler(VivoLocThread.getInstance().getLocationServiceLooper());
    }

    public void registerMccChangedListener(IVivoServiceWatcher.Callback callback, Context context) {
        this.mCallback = callback;
        this.mContext = context;
        setupVivoReceiver();
    }

    public void updateNlpIntent(Intent intent) {
        String packageName = intent.getPackage();
        if (!this.mOriginalPackageNameInited) {
            this.mOriginalPackageName = packageName;
            this.mOriginalPackageNameInited = true;
        }
        checkShouldBindGms();
        if (this.mShouldBindGms && checkPackageExists(GMS_NLP_NAME)) {
            intent.setPackage(GMS_NLP_NAME);
        } else if (!this.mShouldBindGms && FUSED_LOCATION_SERVICE_ACTION.equals(intent.getAction())) {
            intent.setPackage(AOSP_FUSD_NAME);
        } else if (!this.mShouldBindGms && checkPackageExists(DEMESTIC_NLP_NAME)) {
            intent.setPackage(DEMESTIC_NLP_NAME);
        } else {
            intent.setPackage(this.mOriginalPackageName);
        }
    }

    public int updateCurrentUserIdForFixMode(int moldCurrentUserId, int mUserId) {
        if (moldCurrentUserId == 888) {
            VLog.d(TAG, "In Fix Mode");
            return VivoAmsImpl.FIX_MODE_USER_ID;
        }
        return mUserId;
    }

    public Handler getVivoHandler() {
        return this.mHandler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.VivoServiceWatcherImpl$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                VLog.d(VivoServiceWatcherImpl.TAG, "received action:" + action);
                boolean needUpdateNlp = false;
                if (action.equals("vivo.intent.action.USER_COUNTRY_CHANGE")) {
                    needUpdateNlp = true;
                } else if (action.equals("vivo.intent.action.VSNS_COUNTRY_CHANGE_DEBUG")) {
                    boolean isInChina = intent.getBooleanExtra("isInChina", true);
                    VLog.d(VivoServiceWatcherImpl.TAG, "VSNS_COUNTRY_CHANGE_DEBUG isInChina:" + isInChina);
                    if (isInChina) {
                        VivoServiceWatcherImpl.this.mFakeMcc = 1;
                    } else {
                        VivoServiceWatcherImpl.this.mFakeMcc = 0;
                    }
                    needUpdateNlp = true;
                } else if (action.equals("android.intent.action.SIM_STATE_CHANGED") || action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                    boolean oldShouldBindGms = VivoServiceWatcherImpl.this.mShouldBindGms;
                    VivoServiceWatcherImpl.this.checkShouldBindGms();
                    if (oldShouldBindGms != VivoServiceWatcherImpl.this.mShouldBindGms) {
                        needUpdateNlp = true;
                    }
                }
                if (needUpdateNlp && VivoServiceWatcherImpl.this.mCallback != null && VivoServiceWatcherImpl.this.mHandler != null) {
                    VivoServiceWatcherImpl.this.mHandler.post(new Runnable() { // from class: com.android.server.-$$Lambda$VivoServiceWatcherImpl$2$L730NQMM0rAX8Ao5GH9b30OyCew
                        @Override // java.lang.Runnable
                        public final void run() {
                            VivoServiceWatcherImpl.AnonymousClass2.this.lambda$onReceive$0$VivoServiceWatcherImpl$2();
                        }
                    });
                }
            } catch (Exception e) {
                VLog.e(VivoServiceWatcherImpl.TAG, e.toString());
            }
        }

        public /* synthetic */ void lambda$onReceive$0$VivoServiceWatcherImpl$2() {
            VivoServiceWatcherImpl.this.mCallback.onMccChanged(false);
        }
    }

    private void setupVivoReceiver() {
        this.mVivoBroadcastReceiver = new AnonymousClass2();
        IntentFilter intentFilter = new IntentFilter();
        this.mVivoIntentFilter = intentFilter;
        intentFilter.addAction("vivo.intent.action.USER_COUNTRY_CHANGE");
        this.mVivoIntentFilter.addAction("vivo.intent.action.VSNS_COUNTRY_CHANGE_DEBUG");
        this.mVivoIntentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mVivoIntentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        Handler handler = this.mHandler;
        if (handler != null && this.mContext != null) {
            handler.post(new Runnable() { // from class: com.android.server.-$$Lambda$VivoServiceWatcherImpl$AAbYYgb7s_h_WOb1CL6Kf95SpjI
                @Override // java.lang.Runnable
                public final void run() {
                    VivoServiceWatcherImpl.this.lambda$setupVivoReceiver$0$VivoServiceWatcherImpl();
                }
            });
        }
        this.mPackageMonitor.register(this.mContext, this.mHandler.getLooper(), UserHandle.ALL, true);
    }

    public /* synthetic */ void lambda$setupVivoReceiver$0$VivoServiceWatcherImpl() {
        this.mContext.registerReceiver(this.mVivoBroadcastReceiver, this.mVivoIntentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkShouldBindGms() {
        String mcc = SystemProperties.get("persist.radio.vivo.mcc", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        VLog.d(TAG, "checkShouldBindGms mcc:" + mcc + ", mFakeMcc:" + this.mFakeMcc + ", isOversea:" + this.isOversea);
        int i = this.mFakeMcc;
        if (i == 0) {
            mcc = "560";
        } else if (i == 1) {
            mcc = "460";
        }
        if (this.isOversea && mcc != null && !mcc.isEmpty()) {
            boolean is460 = mcc.startsWith("460");
            boolean is461 = mcc.startsWith("461");
            if (!is460 && !is461) {
                this.mShouldBindGms = true;
            } else {
                this.mShouldBindGms = false;
            }
        } else {
            this.mShouldBindGms = this.isOversea;
        }
        if (this.mGTSTest) {
            this.mShouldBindGms = true;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:12:0x0021, code lost:
        if (r3.isEmpty() == false) goto L15;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean checkPackageExists(java.lang.String r6) {
        /*
            r5 = this;
            r0 = 1
            r1 = 0
            if (r6 == 0) goto Lc
            boolean r2 = r6.isEmpty()
            if (r2 != 0) goto Lc
            r2 = r0
            goto Ld
        Lc:
            r2 = r1
        Ld:
            if (r2 == 0) goto L29
            android.content.Context r3 = r5.mContext     // Catch: java.lang.Exception -> L27
            android.content.pm.PackageManager r3 = r3.getPackageManager()     // Catch: java.lang.Exception -> L27
            android.content.pm.PackageInfo r3 = r3.getPackageInfo(r6, r1)     // Catch: java.lang.Exception -> L27
            java.lang.String r3 = r3.versionName     // Catch: java.lang.Exception -> L27
            if (r3 == 0) goto L24
            boolean r4 = r3.isEmpty()     // Catch: java.lang.Exception -> L27
            if (r4 != 0) goto L24
            goto L25
        L24:
            r0 = r1
        L25:
            r2 = r0
            goto L29
        L27:
            r0 = move-exception
            r2 = 0
        L29:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "checkPackageExists "
            r0.append(r1)
            r0.append(r6)
            java.lang.String r1 = " "
            r0.append(r1)
            r0.append(r2)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "VivoServiceWatcher"
            com.vivo.common.utils.VLog.d(r1, r0)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.VivoServiceWatcherImpl.checkPackageExists(java.lang.String):boolean");
    }
}