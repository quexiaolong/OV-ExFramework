package com.android.server.location;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.location.VivoLocConf;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class VivoMockLocationRecoveryNotify {
    private static final String ACTION_DEBUG_DUMP = "com.android.server.VivoMockLocationNotifyRecovery.debug.dump";
    private static final String ACTION_DEBUG_SET_MODE = "com.android.server.VivoMockLocationNotifyRecovery.debug.setMode";
    private static final int MODE_DO_NOTHING = 0;
    private static final int MODE_NOTIFY = 2;
    private static final int MODE_REMOVE_IMPLICITLY = 1;
    public static final int MSG_PKG_REMOVED = 2;
    public static final int MSG_POP_NOTIFICATION = 0;
    public static final int MSG_PROCESS_DIED = 1;
    public static final int MSG_SET_MODE = 3;
    private static final String NOTIFICATION_CHANNEL_ID = "vivo_mock_location_recovery_notify_channel_id";
    private static final int NOTIFY_ID = 51249836;
    private static final String SETTING_MOCK_LOCATION_RECOVERY_MODE = "setting_mock_location_recovery_mode";
    private static Context mContext;
    private LocationManagerServiceExtCallback mCallback;
    private MockLocationNotifyHandler mMockLocationNotifyHandler;
    private Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private Resources mResources;
    private BroadcastReceiver mVivoBroadcastReceiver;
    private IntentFilter mVivoIntentFilter;
    private HandlerThread vivoThread;
    private static boolean DBG = true;
    private static String TAG = VivoLocConf.MOCK_LOCATION_RECOVERY_NOTIFY;
    private Set<PackageProvider> runningLocationManagerApps = new HashSet();
    private ArrayList<Integer> mNotifications = new ArrayList<>();
    private volatile boolean mIsMocking = false;
    private boolean mNotifyEnabled = true;
    private int mMode = 0;
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.location.VivoMockLocationRecoveryNotify.1
        {
            VivoMockLocationRecoveryNotify.this = this;
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
            Message msg = VivoMockLocationRecoveryNotify.this.mMockLocationNotifyHandler.obtainMessage(1);
            msg.arg1 = pid;
            msg.arg2 = uid;
            VivoMockLocationRecoveryNotify.this.mMockLocationNotifyHandler.sendMessage(msg);
        }
    };
    private final PackageMonitor mPackageMonitor = new PackageMonitor() { // from class: com.android.server.location.VivoMockLocationRecoveryNotify.2
        {
            VivoMockLocationRecoveryNotify.this = this;
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
        }

        public void onPackageAdded(String packageName, int uid) {
        }

        public void onPackageRemoved(String packageName, int uid) {
            if (VivoMockLocationRecoveryNotify.DBG) {
                String str = VivoMockLocationRecoveryNotify.TAG;
                VLog.d(str, "onPackageRemoved " + packageName);
            }
            try {
                Message msg = VivoMockLocationRecoveryNotify.this.mMockLocationNotifyHandler.obtainMessage(2, packageName);
                VivoMockLocationRecoveryNotify.this.mMockLocationNotifyHandler.sendMessage(msg);
            } catch (Exception e) {
                VLog.e(VivoMockLocationRecoveryNotify.TAG, Log.getStackTraceString(e));
            }
        }

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            return super.onPackageChanged(packageName, uid, components);
        }
    };

    /* loaded from: classes.dex */
    public interface LocationManagerServiceExtCallback {
        List<String> getProviders();

        boolean isMock(String str);

        void removeTestProvider(String str, String str2);
    }

    public static /* synthetic */ void lambda$rW063mIp0AF5Lyee0aDxvKgVoUE(VivoMockLocationRecoveryNotify vivoMockLocationRecoveryNotify, ContentValuesList contentValuesList) {
        vivoMockLocationRecoveryNotify.updateVivoMockLocationRecoveryNotifyApp(contentValuesList);
    }

    public VivoMockLocationRecoveryNotify(Context context, LocationManagerServiceExtCallback callback, boolean dbg) {
        this.mCallback = callback;
        mContext = context;
        VivoLocConf conf = VivoLocConf.getInstance();
        conf.registerListener(VivoLocConf.MOCK_LOCATION_RECOVERY_NOTIFY, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoMockLocationRecoveryNotify$rW063mIp0AF5Lyee0aDxvKgVoUE
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoMockLocationRecoveryNotify.lambda$rW063mIp0AF5Lyee0aDxvKgVoUE(VivoMockLocationRecoveryNotify.this, contentValuesList);
            }
        });
        HandlerThread handlerThread = new HandlerThread(VivoLocConf.MOCK_LOCATION_RECOVERY_NOTIFY);
        this.vivoThread = handlerThread;
        handlerThread.start();
        this.mNotificationManager = (NotificationManager) mContext.getSystemService("notification");
        this.mResources = mContext.getResources();
        this.mMockLocationNotifyHandler = new MockLocationNotifyHandler(this.vivoThread.getLooper());
        setupVivoReceiver();
        registerProcessObserver();
        this.mPackageMonitor.register(mContext, (Looper) null, UserHandle.ALL, true);
    }

    public void updateVivoMockLocationRecoveryNotifyApp(ContentValuesList list) {
        if (DBG) {
            VLog.d(TAG, "updateVivoMockLocationRecoveryNotifyApp start");
        }
        if (list == null) {
            return;
        }
        int mode = Integer.valueOf(list.getValue("mode")).intValue();
        if (mode == -1) {
            mode = 0;
            if (DBG) {
                VLog.e(TAG, "failed to load mode from configuration (mode: -1)");
            }
        }
        this.mMode = mode;
        if (DBG) {
            dumpState();
            VLog.d(TAG, "updateVivoLocationNotifyApp end");
        }
    }

    private boolean isLocationEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(), "location_mode", 3) != 0;
    }

    public boolean isRunningMockLocationApp() {
        boolean result = !this.runningLocationManagerApps.isEmpty();
        if (result) {
            for (PackageProvider currPk : this.runningLocationManagerApps) {
                if (isAppDied(currPk.packageName)) {
                    this.runningLocationManagerApps.remove(currPk);
                }
            }
            return !this.runningLocationManagerApps.isEmpty();
        }
        return result;
    }

    public PackageProvider isContainPackageInfo(String pk) {
        if (pk != null && !pk.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && !this.runningLocationManagerApps.isEmpty()) {
            for (PackageProvider currPk : this.runningLocationManagerApps) {
                if (currPk.packageName.equals(pk)) {
                    return currPk;
                }
            }
        }
        return null;
    }

    public boolean hasMockProvider() {
        List<String> providers = this.mCallback.getProviders();
        for (String curr : providers) {
            if (this.mCallback.isMock(curr)) {
                return true;
            }
        }
        return false;
    }

    public void addTestProviderNotify(String pk, String provider) {
        if (DBG) {
            String str = TAG;
            VLog.d(str, "addTestProviderNotify  " + pk + "  " + provider);
        }
        PackageProvider packageProvider = isContainPackageInfo(pk);
        if (packageProvider != null) {
            packageProvider.addProvider(provider);
            return;
        }
        PackageProvider packageProvider2 = new PackageProvider(pk);
        packageProvider2.addProvider(provider);
        this.runningLocationManagerApps.add(packageProvider2);
        if (this.mMode == 2) {
            this.mMockLocationNotifyHandler.sendEmptyMessage(0);
        }
    }

    public void removeTestProviderNotify(String pk, String provider) {
        if (DBG) {
            String str = TAG;
            VLog.d(str, "removeTestProviderNotify  " + pk + "  " + provider);
        }
        PackageProvider packageProvider = isContainPackageInfo(pk);
        if (packageProvider != null) {
            packageProvider.removeProvider(provider);
            if (!packageProvider.hasProvider()) {
                this.runningLocationManagerApps.remove(packageProvider);
            }
        }
        this.mIsMocking = false;
    }

    public void setTestProviderNotify() {
        this.mIsMocking = true;
    }

    public boolean isEnableVivoMockLocationRecoveryNotify() {
        return this.mMode != 0;
    }

    public static String getAppName(int pid, int uid) {
        try {
            String pk = mContext.getPackageManager().getNameForUid(uid);
            return pk;
        } catch (Exception e) {
            VLog.e(TAG, Log.getStackTraceString(e));
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
    }

    public static boolean isAppDied(String pk) {
        boolean died = true;
        if (pk.equals("android.uid.system:1000")) {
            return false;
        }
        try {
            ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(pk, 1);
            ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
            List<ActivityManager.RunningAppProcessInfo> runTasks = mActivityManager.getRunningAppProcesses();
            Iterator<ActivityManager.RunningAppProcessInfo> it = runTasks.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ActivityManager.RunningAppProcessInfo runAppInfo = it.next();
                if (applicationInfo.uid == runAppInfo.uid) {
                    died = false;
                    break;
                }
            }
        } catch (Exception e) {
        }
        if (DBG) {
            String str = TAG;
            VLog.d(str, "isAppDied " + pk + ", died :" + died);
        }
        return died;
    }

    private void setupVivoReceiver() {
        this.mVivoBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoMockLocationRecoveryNotify.3
            {
                VivoMockLocationRecoveryNotify.this = this;
            }

            /* JADX WARN: Removed duplicated region for block: B:50:0x004c  */
            /* JADX WARN: Removed duplicated region for block: B:53:0x0056 A[Catch: Exception -> 0x005d, TRY_LEAVE, TryCatch #0 {Exception -> 0x005d, blocks: (B:33:0x0000, B:35:0x000a, B:38:0x0025, B:52:0x004f, B:53:0x0056, B:43:0x0037, B:46:0x0041), top: B:58:0x0000 }] */
            @Override // android.content.BroadcastReceiver
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct code enable 'Show inconsistent code' option in preferences
            */
            public void onReceive(android.content.Context r7, android.content.Intent r8) {
                /*
                    r6 = this;
                    java.lang.String r0 = r8.getAction()     // Catch: java.lang.Exception -> L5d
                    boolean r1 = com.android.server.location.VivoMockLocationRecoveryNotify.access$100()     // Catch: java.lang.Exception -> L5d
                    if (r1 == 0) goto L22
                    java.lang.String r1 = com.android.server.location.VivoMockLocationRecoveryNotify.access$200()     // Catch: java.lang.Exception -> L5d
                    java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> L5d
                    r2.<init>()     // Catch: java.lang.Exception -> L5d
                    java.lang.String r3 = "setupVivoReceiver action:"
                    r2.append(r3)     // Catch: java.lang.Exception -> L5d
                    r2.append(r0)     // Catch: java.lang.Exception -> L5d
                    java.lang.String r2 = r2.toString()     // Catch: java.lang.Exception -> L5d
                    com.vivo.common.utils.VLog.d(r1, r2)     // Catch: java.lang.Exception -> L5d
                L22:
                    if (r0 != 0) goto L25
                    return
                L25:
                    r1 = -1
                    int r2 = r0.hashCode()     // Catch: java.lang.Exception -> L5d
                    r3 = -357307292(0xffffffffeab3ec64, float:-1.0875702E26)
                    r4 = 0
                    r5 = 1
                    if (r2 == r3) goto L41
                    r3 = 1472108309(0x57be9715, float:4.19112203E14)
                    if (r2 == r3) goto L37
                L36:
                    goto L4a
                L37:
                    java.lang.String r2 = "com.android.server.VivoMockLocationNotifyRecovery.debug.dump"
                    boolean r2 = r0.equals(r2)     // Catch: java.lang.Exception -> L5d
                    if (r2 == 0) goto L36
                    r1 = r4
                    goto L4a
                L41:
                    java.lang.String r2 = "com.android.server.VivoMockLocationNotifyRecovery.debug.setMode"
                    boolean r2 = r0.equals(r2)     // Catch: java.lang.Exception -> L5d
                    if (r2 == 0) goto L36
                    r1 = r5
                L4a:
                    if (r1 == 0) goto L56
                    if (r1 == r5) goto L4f
                    goto L5c
                L4f:
                    r1 = 3
                    java.lang.String r2 = "setting_mock_location_recovery_mode"
                    r6.repostBroadcastMessage(r1, r8, r2, r4)     // Catch: java.lang.Exception -> L5d
                    goto L5c
                L56:
                    com.android.server.location.VivoMockLocationRecoveryNotify r1 = com.android.server.location.VivoMockLocationRecoveryNotify.this     // Catch: java.lang.Exception -> L5d
                    com.android.server.location.VivoMockLocationRecoveryNotify.access$400(r1)     // Catch: java.lang.Exception -> L5d
                L5c:
                    goto L69
                L5d:
                    r0 = move-exception
                    java.lang.String r1 = com.android.server.location.VivoMockLocationRecoveryNotify.access$200()
                    java.lang.String r2 = android.util.Log.getStackTraceString(r0)
                    com.vivo.common.utils.VLog.e(r1, r2)
                L69:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.VivoMockLocationRecoveryNotify.AnonymousClass3.onReceive(android.content.Context, android.content.Intent):void");
            }

            private void repostBroadcastMessage(int what, Intent intent, String key, int type) {
                Message msg = null;
                if (type == 0) {
                    int intVal = ((Integer) intent.getExtra(key, -1)).intValue();
                    msg = VivoMockLocationRecoveryNotify.this.mMockLocationNotifyHandler.obtainMessage(what, Integer.valueOf(intVal));
                } else if (type == 1) {
                    boolean boolVal = ((Boolean) intent.getExtra(key, true)).booleanValue();
                    msg = VivoMockLocationRecoveryNotify.this.mMockLocationNotifyHandler.obtainMessage(what, Boolean.valueOf(boolVal));
                }
                if (msg != null) {
                    VivoMockLocationRecoveryNotify.this.mMockLocationNotifyHandler.sendMessage(msg);
                }
            }
        };
        this.mMockLocationNotifyHandler.post(new Runnable() { // from class: com.android.server.location.VivoMockLocationRecoveryNotify.4
            {
                VivoMockLocationRecoveryNotify.this = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                VivoMockLocationRecoveryNotify.this.mVivoIntentFilter = new IntentFilter();
                VivoMockLocationRecoveryNotify.this.mVivoIntentFilter.addAction(VivoMockLocationRecoveryNotify.ACTION_DEBUG_DUMP);
                VivoMockLocationRecoveryNotify.this.mVivoIntentFilter.addAction(VivoMockLocationRecoveryNotify.ACTION_DEBUG_SET_MODE);
                VivoMockLocationRecoveryNotify.mContext.registerReceiver(VivoMockLocationRecoveryNotify.this.mVivoBroadcastReceiver, VivoMockLocationRecoveryNotify.this.mVivoIntentFilter);
            }
        });
    }

    public void registerProcessObserver() {
        try {
            if (DBG) {
                VLog.d(TAG, "registerProcessObserver");
            }
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (Exception e) {
            String str = TAG;
            VLog.e(str, "error registerProcessObserver " + e);
        }
    }

    public void dumpState() {
        try {
            VLog.d(TAG, "-------------------------dumpState begin-------------------------");
            String str = TAG;
            VLog.d(str, "mMode: " + this.mMode);
            String str2 = TAG;
            VLog.d(str2, "mNotifyEnabled: " + this.mNotifyEnabled);
            String str3 = TAG;
            VLog.d(str3, "mIsMocking: " + this.mIsMocking);
            VLog.d(TAG, "-------------------------dumpState end---------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void enableVerboseLogging(int verbose) {
        DBG = verbose > 0;
    }

    /* loaded from: classes.dex */
    public class PackageProvider {
        private String packageName;
        private Set<String> providers = new CopyOnWriteArraySet();

        public PackageProvider(String pk) {
            VivoMockLocationRecoveryNotify.this = r1;
            this.packageName = pk;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public boolean hasProvider() {
            return !this.providers.isEmpty();
        }

        public void addProvider(String provider) {
            this.providers.add(provider);
        }

        public void removeProvider(String provider) {
            this.providers.remove(provider);
        }
    }

    /* loaded from: classes.dex */
    public class MockLocationNotifyHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MockLocationNotifyHandler(Looper looper) {
            super(looper);
            VivoMockLocationRecoveryNotify.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (VivoMockLocationRecoveryNotify.DBG) {
                String str = VivoMockLocationRecoveryNotify.TAG;
                VLog.d(str, "handleMessage " + msg.what);
            }
            try {
                int i = msg.what;
                if (i == 0) {
                    handlePopNotification(msg);
                } else if (i == 1) {
                    handleProcessDied(msg);
                } else if (i == 2) {
                    handlePackageRemoved(msg);
                } else if (i == 3) {
                    handleSetMode(msg);
                }
            } catch (Exception e) {
                VLog.e(VivoMockLocationRecoveryNotify.TAG, Log.getStackTraceString(e));
            }
        }

        private void handlePopNotification(Message msg) {
            if (VivoMockLocationRecoveryNotify.DBG) {
                VivoMockLocationRecoveryNotify.this.dumpState();
            }
            if (VivoMockLocationRecoveryNotify.this.mNotifyEnabled && VivoMockLocationRecoveryNotify.this.mMode == 2) {
                setNotificationVisible();
            }
            VivoMockLocationRecoveryNotify.this.mNotifyEnabled = false;
        }

        private void handleProcessDied(Message msg) {
            String appName = VivoMockLocationRecoveryNotify.getAppName(msg.arg1, msg.arg2);
            if (appName == null || appName.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                return;
            }
            whenDiedOrRemoved(appName, true);
        }

        private void handlePackageRemoved(Message msg) {
            String appName = (String) msg.obj;
            if (appName == null || appName.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                return;
            }
            whenDiedOrRemoved(appName, false);
        }

        private void whenDiedOrRemoved(String appName, boolean isDied) {
            StringBuilder sb;
            String str;
            PackageProvider packageProvider = VivoMockLocationRecoveryNotify.this.isContainPackageInfo(appName);
            if ((!isDied || (packageProvider != null && VivoMockLocationRecoveryNotify.isAppDied(appName))) && packageProvider != null) {
                VivoMockLocationRecoveryNotify.this.runningLocationManagerApps.remove(packageProvider);
                if (VivoMockLocationRecoveryNotify.DBG) {
                    String str2 = VivoMockLocationRecoveryNotify.TAG;
                    if (isDied) {
                        sb = new StringBuilder();
                        str = "MSG_PROCESS_DIED ";
                    } else {
                        sb = new StringBuilder();
                        str = "MSG_PKG_REMOVED ";
                    }
                    sb.append(str);
                    sb.append(appName);
                    VLog.d(str2, sb.toString());
                }
                VivoMockLocationRecoveryNotify.this.mNotifyEnabled = true;
                VivoMockLocationRecoveryNotify.this.mIsMocking = false;
                if ((VivoMockLocationRecoveryNotify.this.mMode == 1 || VivoMockLocationRecoveryNotify.this.mMode == 2) && VivoMockLocationRecoveryNotify.this.hasMockProvider()) {
                    if (VivoMockLocationRecoveryNotify.this.isRunningMockLocationApp()) {
                        if (!VivoMockLocationRecoveryNotify.this.mIsMocking) {
                            removeTestProvider();
                            return;
                        }
                        return;
                    }
                    removeTestProvider();
                }
            }
        }

        private void handleSetMode(Message msg) {
            if (VivoMockLocationRecoveryNotify.DBG) {
                cancelNotification();
                int mode = ((Integer) msg.obj).intValue();
                if (mode == 0 || mode == 1 || mode == 2) {
                    VivoMockLocationRecoveryNotify.this.mMode = mode;
                    String str = VivoMockLocationRecoveryNotify.TAG;
                    VLog.d(str, "SETTING_MOCK_LOCATION_RECOVERY_MODE: " + VivoMockLocationRecoveryNotify.this.mMode);
                }
            }
        }

        private void removeTestProvider() {
            try {
                List<String> providers = VivoMockLocationRecoveryNotify.this.mCallback.getProviders();
                if (providers != null) {
                    for (String curr : providers) {
                        String str = VivoMockLocationRecoveryNotify.TAG;
                        VLog.d(str, "removeTestProvider: " + curr + "  isMock: " + VivoMockLocationRecoveryNotify.this.mCallback.isMock(curr));
                        if (VivoMockLocationRecoveryNotify.this.mCallback.isMock(curr)) {
                            VivoMockLocationRecoveryNotify.this.mCallback.removeTestProvider(curr, VivoMockLocationRecoveryNotify.TAG);
                        }
                    }
                    VivoMockLocationRecoveryNotify.this.runningLocationManagerApps.clear();
                }
            } catch (Exception e) {
                VLog.e(VivoMockLocationRecoveryNotify.TAG, Log.getStackTraceString(e));
            }
            VivoMockLocationRecoveryNotify.this.mNotifyEnabled = true;
            cancelNotification();
        }

        private void setNotificationVisible() {
            int offset = 0;
            while (offset < 500) {
                String tmpStr = String.valueOf(new Date().getTime());
                offset = Integer.parseInt(tmpStr.substring(tmpStr.length() - 5));
            }
            int currNotifyId = offset + VivoMockLocationRecoveryNotify.NOTIFY_ID;
            try {
                Bundle bundle = new Bundle();
                bundle.putInt("vivo.summaryIconRes", 50464099);
                NotificationChannel channel = new NotificationChannel(VivoMockLocationRecoveryNotify.NOTIFICATION_CHANNEL_ID, VivoLocConf.MOCK_LOCATION_RECOVERY_NOTIFY, 4);
                VivoMockLocationRecoveryNotify.this.mNotificationManager.createNotificationChannel(channel);
                VivoMockLocationRecoveryNotify.this.mNotificationBuilder = new Notification.Builder(VivoMockLocationRecoveryNotify.mContext, VivoMockLocationRecoveryNotify.NOTIFICATION_CHANNEL_ID).setWhen(0L).setSmallIcon(50464100).setExtras(bundle).setAutoCancel(true).setVisibility(0).setColor(VivoMockLocationRecoveryNotify.this.mResources.getColor(50856011));
                CharSequence title = VivoMockLocationRecoveryNotify.this.mResources.getText(VivoMockLocationRecoveryNotify.NOTIFY_ID);
                CharSequence details = VivoMockLocationRecoveryNotify.this.mResources.getText(51249835);
                VivoMockLocationRecoveryNotify.this.mNotificationBuilder.setTicker(title).setContentTitle(title).setStyle(new Notification.BigTextStyle().bigText(details));
                VivoMockLocationRecoveryNotify.this.mNotificationManager.notifyAsUser(null, currNotifyId, VivoMockLocationRecoveryNotify.this.mNotificationBuilder.build(), UserHandle.ALL);
                VivoMockLocationRecoveryNotify.this.mNotifications.add(Integer.valueOf(currNotifyId));
                if (VivoMockLocationRecoveryNotify.DBG) {
                    VLog.d(VivoMockLocationRecoveryNotify.TAG, "setNotificationVisible");
                }
            } catch (Exception e) {
                String str = VivoMockLocationRecoveryNotify.TAG;
                VLog.e(str, "setNotificationVisible: \n" + Log.getStackTraceString(e));
            }
        }

        private PendingIntent getPrivateBroadcast(String action) {
            Intent intent = new Intent(action).setPackage(VivoPermissionUtils.OS_PKG);
            return PendingIntent.getBroadcast(VivoMockLocationRecoveryNotify.mContext, 0, intent, Dataspace.RANGE_FULL);
        }

        private void cancelNotification() {
            if (VivoMockLocationRecoveryNotify.DBG) {
                VLog.d(VivoMockLocationRecoveryNotify.TAG, "cancelNotification");
            }
            Iterator it = VivoMockLocationRecoveryNotify.this.mNotifications.iterator();
            while (it.hasNext()) {
                int currNotifyId = ((Integer) it.next()).intValue();
                VivoMockLocationRecoveryNotify.this.mNotificationManager.cancelAsUser(null, currNotifyId, UserHandle.ALL);
            }
            VivoMockLocationRecoveryNotify.this.mNotifications.clear();
        }
    }
}