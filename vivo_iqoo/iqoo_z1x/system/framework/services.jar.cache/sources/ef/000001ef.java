package android.net;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.util.SharedLog;
import android.os.Environment;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Slog;
import java.io.File;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class ConnectivityModuleConnector {
    private static final String CONFIG_ALWAYS_RATELIMIT_NETWORKSTACK_CRASH = "always_ratelimit_networkstack_crash";
    private static final String CONFIG_MIN_CRASH_INTERVAL_MS = "min_crash_interval";
    private static final String CONFIG_MIN_UPTIME_BEFORE_CRASH_MS = "min_uptime_before_crash";
    private static final long DEFAULT_MIN_CRASH_INTERVAL_MS = 21600000;
    private static final long DEFAULT_MIN_UPTIME_BEFORE_CRASH_MS = 1800000;
    private static final String IN_PROCESS_SUFFIX = ".InProcess";
    private static final String PREFS_FILE = "ConnectivityModuleConnector.xml";
    private static final String PREF_KEY_LAST_CRASH_TIME = "lastcrash_time";
    private static final String TAG = ConnectivityModuleConnector.class.getSimpleName();
    private static ConnectivityModuleConnector sInstance;
    private Context mContext;
    private final Dependencies mDeps;
    private final ArraySet<ConnectivityModuleHealthListener> mHealthListeners;
    private final SharedLog mLog;

    /* loaded from: classes.dex */
    public interface ConnectivityModuleHealthListener {
        void onNetworkStackFailure(String str);
    }

    /* loaded from: classes.dex */
    protected interface Dependencies {
        Intent getModuleServiceIntent(PackageManager packageManager, String str, String str2, boolean z);
    }

    /* loaded from: classes.dex */
    public interface ModuleServiceCallback {
        void onModuleServiceConnected(IBinder iBinder);
    }

    private ConnectivityModuleConnector() {
        this(new DependenciesImpl());
    }

    ConnectivityModuleConnector(Dependencies deps) {
        this.mLog = new SharedLog(TAG);
        this.mHealthListeners = new ArraySet<>();
        this.mDeps = deps;
    }

    public static synchronized ConnectivityModuleConnector getInstance() {
        ConnectivityModuleConnector connectivityModuleConnector;
        synchronized (ConnectivityModuleConnector.class) {
            if (sInstance == null) {
                sInstance = new ConnectivityModuleConnector();
            }
            connectivityModuleConnector = sInstance;
        }
        return connectivityModuleConnector;
    }

    public void init(Context context) {
        log("Network stack init");
        this.mContext = context;
    }

    /* loaded from: classes.dex */
    private static class DependenciesImpl implements Dependencies {
        private DependenciesImpl() {
        }

        @Override // android.net.ConnectivityModuleConnector.Dependencies
        public Intent getModuleServiceIntent(PackageManager pm, String serviceIntentBaseAction, String servicePermissionName, boolean inSystemProcess) {
            String str;
            if (inSystemProcess) {
                str = serviceIntentBaseAction + ConnectivityModuleConnector.IN_PROCESS_SUFFIX;
            } else {
                str = serviceIntentBaseAction;
            }
            Intent intent = new Intent(str);
            ComponentName comp = intent.resolveSystemService(pm, 0);
            if (comp == null) {
                return null;
            }
            intent.setComponent(comp);
            try {
                int uid = pm.getPackageUidAsUser(comp.getPackageName(), 0);
                int expectedUid = inSystemProcess ? 1000 : 1073;
                if (uid != expectedUid) {
                    throw new SecurityException("Invalid network stack UID: " + uid);
                }
                if (!inSystemProcess) {
                    ConnectivityModuleConnector.checkModuleServicePermission(pm, comp, servicePermissionName);
                }
                return intent;
            } catch (PackageManager.NameNotFoundException e) {
                throw new SecurityException("Could not check network stack UID; package not found.", e);
            }
        }
    }

    public void registerHealthListener(ConnectivityModuleHealthListener listener) {
        synchronized (this.mHealthListeners) {
            this.mHealthListeners.add(listener);
        }
    }

    public void startModuleService(String serviceIntentBaseAction, String servicePermissionName, ModuleServiceCallback callback) {
        log("Starting networking module " + serviceIntentBaseAction);
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = this.mDeps.getModuleServiceIntent(pm, serviceIntentBaseAction, servicePermissionName, true);
        if (intent == null) {
            intent = this.mDeps.getModuleServiceIntent(pm, serviceIntentBaseAction, servicePermissionName, false);
            log("Starting networking module in network_stack process");
        } else {
            log("Starting networking module in system_server process");
        }
        if (intent == null) {
            maybeCrashWithTerribleFailure("Could not resolve the networking module", null);
            return;
        }
        String packageName = intent.getComponent().getPackageName();
        if (!this.mContext.bindServiceAsUser(intent, new ModuleServiceConnection(packageName, callback), 65, UserHandle.SYSTEM)) {
            maybeCrashWithTerribleFailure("Could not bind to networking module in-process, or in app with " + intent, packageName);
            return;
        }
        log("Networking module service start requested");
    }

    /* loaded from: classes.dex */
    private class ModuleServiceConnection implements ServiceConnection {
        private final ModuleServiceCallback mModuleServiceCallback;
        private final String mPackageName;

        private ModuleServiceConnection(String packageName, ModuleServiceCallback moduleCallback) {
            this.mPackageName = packageName;
            this.mModuleServiceCallback = moduleCallback;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            ConnectivityModuleConnector.this.logi("Networking module service connected");
            this.mModuleServiceCallback.onModuleServiceConnected(service);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            ConnectivityModuleConnector.this.maybeCrashWithTerribleFailure("Lost network stack", this.mPackageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void checkModuleServicePermission(PackageManager pm, ComponentName comp, String servicePermissionName) {
        int hasPermission = pm.checkPermission(servicePermissionName, comp.getPackageName());
        if (hasPermission != 0) {
            throw new SecurityException("Networking module does not have permission " + servicePermissionName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x007c, code lost:
        r4 = r23.mHealthListeners;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x0080, code lost:
        monitor-enter(r4);
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x0081, code lost:
        r0 = new android.util.ArraySet<>(r23.mHealthListeners);
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x0088, code lost:
        monitor-exit(r4);
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x0089, code lost:
        r4 = r0.iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x0091, code lost:
        if (r4.hasNext() == false) goto L39;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x0093, code lost:
        r5 = r4.next();
        r5.onNetworkStackFailure(r25);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public synchronized void maybeCrashWithTerribleFailure(java.lang.String r24, java.lang.String r25) {
        /*
            r23 = this;
            r1 = r23
            r2 = r24
            r3 = r25
            monitor-enter(r23)
            r0 = 0
            r1.logWtf(r2, r0)     // Catch: java.lang.Throwable -> Laf
            long r4 = android.os.SystemClock.elapsedRealtime()     // Catch: java.lang.Throwable -> Laf
            long r6 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Throwable -> Laf
            java.lang.String r0 = "connectivity"
            java.lang.String r8 = "min_crash_interval"
            r9 = 21600000(0x1499700, double:1.0671818E-316)
            long r8 = android.provider.DeviceConfig.getLong(r0, r8, r9)     // Catch: java.lang.Throwable -> Laf
            java.lang.String r0 = "connectivity"
            java.lang.String r10 = "min_uptime_before_crash"
            r11 = 1800000(0x1b7740, double:8.89318E-318)
            long r10 = android.provider.DeviceConfig.getLong(r0, r10, r11)     // Catch: java.lang.Throwable -> Laf
            java.lang.String r0 = "connectivity"
            java.lang.String r12 = "always_ratelimit_networkstack_crash"
            r13 = 0
            boolean r0 = android.provider.DeviceConfig.getBoolean(r0, r12, r13)     // Catch: java.lang.Throwable -> Laf
            r12 = r0
            android.content.SharedPreferences r0 = r23.getSharedPreferences()     // Catch: java.lang.Throwable -> Laf
            r14 = r0
            long r15 = r1.tryGetLastCrashTime(r14)     // Catch: java.lang.Throwable -> Laf
            boolean r0 = android.os.Build.IS_DEBUGGABLE     // Catch: java.lang.Throwable -> Laf
            r17 = 1
            if (r0 == 0) goto L49
            if (r12 != 0) goto L49
            r0 = r17
            goto L4a
        L49:
            r0 = r13
        L4a:
            r18 = r0
            int r0 = (r4 > r10 ? 1 : (r4 == r10 ? 0 : -1))
            if (r0 >= 0) goto L53
            r0 = r17
            goto L54
        L53:
            r0 = r13
        L54:
            r19 = r0
            r20 = 0
            int r0 = (r15 > r20 ? 1 : (r15 == r20 ? 0 : -1))
            if (r0 == 0) goto L63
            int r0 = (r15 > r6 ? 1 : (r15 == r6 ? 0 : -1))
            if (r0 >= 0) goto L63
            r0 = r17
            goto L64
        L63:
            r0 = r13
        L64:
            r20 = r0
            if (r20 == 0) goto L70
            long r21 = r15 + r8
            int r0 = (r6 > r21 ? 1 : (r6 == r21 ? 0 : -1))
            if (r0 >= 0) goto L70
            r13 = r17
        L70:
            if (r18 != 0) goto La4
            if (r19 != 0) goto L7a
            if (r13 == 0) goto L77
            goto L7a
        L77:
            r21 = r4
            goto La6
        L7a:
            if (r3 == 0) goto La0
            r21 = r4
            android.util.ArraySet<android.net.ConnectivityModuleConnector$ConnectivityModuleHealthListener> r4 = r1.mHealthListeners     // Catch: java.lang.Throwable -> Laf
            monitor-enter(r4)     // Catch: java.lang.Throwable -> Laf
            android.util.ArraySet r0 = new android.util.ArraySet     // Catch: java.lang.Throwable -> L9d
            android.util.ArraySet<android.net.ConnectivityModuleConnector$ConnectivityModuleHealthListener> r5 = r1.mHealthListeners     // Catch: java.lang.Throwable -> L9d
            r0.<init>(r5)     // Catch: java.lang.Throwable -> L9d
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L9d
            java.util.Iterator r4 = r0.iterator()     // Catch: java.lang.Throwable -> Laf
        L8d:
            boolean r5 = r4.hasNext()     // Catch: java.lang.Throwable -> Laf
            if (r5 == 0) goto La2
            java.lang.Object r5 = r4.next()     // Catch: java.lang.Throwable -> Laf
            android.net.ConnectivityModuleConnector$ConnectivityModuleHealthListener r5 = (android.net.ConnectivityModuleConnector.ConnectivityModuleHealthListener) r5     // Catch: java.lang.Throwable -> Laf
            r5.onNetworkStackFailure(r3)     // Catch: java.lang.Throwable -> Laf
            goto L8d
        L9d:
            r0 = move-exception
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L9d
            throw r0     // Catch: java.lang.Throwable -> Laf
        La0:
            r21 = r4
        La2:
            monitor-exit(r23)
            return
        La4:
            r21 = r4
        La6:
            r1.tryWriteLastCrashTime(r14, r6)     // Catch: java.lang.Throwable -> Laf
            java.lang.IllegalStateException r0 = new java.lang.IllegalStateException     // Catch: java.lang.Throwable -> Laf
            r0.<init>(r2)     // Catch: java.lang.Throwable -> Laf
            throw r0     // Catch: java.lang.Throwable -> Laf
        Laf:
            r0 = move-exception
            monitor-exit(r23)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: android.net.ConnectivityModuleConnector.maybeCrashWithTerribleFailure(java.lang.String, java.lang.String):void");
    }

    private SharedPreferences getSharedPreferences() {
        try {
            File prefsFile = new File(Environment.getDataSystemDeDirectory(0), PREFS_FILE);
            return this.mContext.createDeviceProtectedStorageContext().getSharedPreferences(prefsFile, 0);
        } catch (Throwable e) {
            logWtf("Error loading shared preferences", e);
            return null;
        }
    }

    private long tryGetLastCrashTime(SharedPreferences prefs) {
        if (prefs == null) {
            return 0L;
        }
        try {
            return prefs.getLong(PREF_KEY_LAST_CRASH_TIME, 0L);
        } catch (Throwable e) {
            logWtf("Error getting last crash time", e);
            return 0L;
        }
    }

    private void tryWriteLastCrashTime(SharedPreferences prefs, long value) {
        if (prefs == null) {
            return;
        }
        try {
            prefs.edit().putLong(PREF_KEY_LAST_CRASH_TIME, value).commit();
        } catch (Throwable e) {
            logWtf("Error writing last crash time", e);
        }
    }

    private void log(String message) {
        Slog.d(TAG, message);
        synchronized (this.mLog) {
            this.mLog.log(message);
        }
    }

    private void logWtf(String message, Throwable e) {
        Slog.wtf(TAG, message, e);
        synchronized (this.mLog) {
            this.mLog.e(message);
        }
    }

    private void loge(String message, Throwable e) {
        Slog.e(TAG, message, e);
        synchronized (this.mLog) {
            this.mLog.e(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logi(String message) {
        Slog.i(TAG, message);
        synchronized (this.mLog) {
            this.mLog.i(message);
        }
    }

    public void dump(PrintWriter pw) {
        this.mLog.dump(null, pw, null);
    }
}