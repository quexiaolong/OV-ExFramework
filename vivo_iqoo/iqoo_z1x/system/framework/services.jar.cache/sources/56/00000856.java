package com.android.server.attention;

import android.attention.AttentionManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.hardware.tv.cec.V1_0.CecMessageType;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.service.attention.IAttentionCallback;
import android.service.attention.IAttentionService;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.attention.AttentionManagerService;
import com.android.server.job.controllers.JobStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;

/* loaded from: classes.dex */
public class AttentionManagerService extends SystemService {
    protected static final int ATTENTION_CACHE_BUFFER_SIZE = 5;
    private static final long CONNECTION_TTL_MILLIS = 60000;
    private static final boolean DEBUG = false;
    private static final boolean DEFAULT_SERVICE_ENABLED = true;
    static final long DEFAULT_STALE_AFTER_MILLIS = 1000;
    private static final String KEY_SERVICE_ENABLED = "service_enabled";
    static final String KEY_STALE_AFTER_MILLIS = "stale_after_millis";
    private static final String LOG_TAG = "AttentionManagerService";
    private static String sTestAttentionServicePackage;
    private AttentionHandler mAttentionHandler;
    ComponentName mComponentName;
    private final Context mContext;
    private final Object mLock;
    private final PowerManager mPowerManager;
    private final SparseArray<UserState> mUserStates;

    public AttentionManagerService(Context context) {
        this(context, (PowerManager) context.getSystemService("power"), new Object(), null);
        this.mAttentionHandler = new AttentionHandler();
    }

    AttentionManagerService(Context context, PowerManager powerManager, Object lock, AttentionHandler handler) {
        super(context);
        this.mUserStates = new SparseArray<>();
        Objects.requireNonNull(context);
        this.mContext = context;
        this.mPowerManager = powerManager;
        this.mLock = lock;
        this.mAttentionHandler = handler;
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mContext.registerReceiver(new ScreenStateReceiver(), new IntentFilter("android.intent.action.SCREEN_OFF"));
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("attention", new BinderService());
        publishLocalService(AttentionManagerInternal.class, new LocalService());
    }

    @Override // com.android.server.SystemService
    public void onSwitchUser(int userId) {
        cancelAndUnbindLocked(peekUserStateLocked(userId));
    }

    public static boolean isServiceConfigured(Context context) {
        return !TextUtils.isEmpty(getServiceConfigPackage(context));
    }

    private boolean isServiceAvailable() {
        if (this.mComponentName == null) {
            this.mComponentName = resolveAttentionService(this.mContext);
        }
        return this.mComponentName != null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAttentionServiceSupported() {
        return isServiceEnabled() && isServiceConfigured(this.mContext);
    }

    protected boolean isServiceEnabled() {
        return DeviceConfig.getBoolean("attention_manager_service", KEY_SERVICE_ENABLED, true);
    }

    protected long getStaleAfterMillis() {
        long millis = DeviceConfig.getLong("attention_manager_service", KEY_STALE_AFTER_MILLIS, 1000L);
        if (millis < 0 || millis > JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY) {
            Slog.w(LOG_TAG, "Bad flag value supplied for: stale_after_millis");
            return 1000L;
        }
        return millis;
    }

    boolean checkAttention(long timeout, AttentionManagerInternal.AttentionCallbackInternal callbackInternal) {
        Objects.requireNonNull(callbackInternal);
        if (!isAttentionServiceSupported()) {
            Slog.w(LOG_TAG, "Trying to call checkAttention() on an unsupported device.");
            return false;
        } else if (!isServiceAvailable()) {
            Slog.w(LOG_TAG, "Service is not available at this moment.");
            return false;
        } else if (this.mPowerManager.isInteractive()) {
            synchronized (this.mLock) {
                long now = SystemClock.uptimeMillis();
                freeIfInactiveLocked();
                UserState userState = getOrCreateCurrentUserStateLocked();
                userState.bindLocked();
                AttentionCheckCache cache = userState.mAttentionCheckCacheBuffer == null ? null : userState.mAttentionCheckCacheBuffer.getLast();
                if (cache == null || now >= cache.mLastComputed + getStaleAfterMillis()) {
                    if (userState.mCurrentAttentionCheck == null || (userState.mCurrentAttentionCheck.mIsDispatched && userState.mCurrentAttentionCheck.mIsFulfilled)) {
                        userState.mCurrentAttentionCheck = createAttentionCheck(callbackInternal, userState);
                        if (userState.mService != null) {
                            try {
                                cancelAfterTimeoutLocked(timeout);
                                userState.mService.checkAttention(userState.mCurrentAttentionCheck.mIAttentionCallback);
                                userState.mCurrentAttentionCheck.mIsDispatched = true;
                            } catch (RemoteException e) {
                                Slog.e(LOG_TAG, "Cannot call into the AttentionService");
                                return false;
                            }
                        }
                        return true;
                    }
                    return false;
                }
                callbackInternal.onSuccess(cache.mResult, cache.mTimestamp);
                return true;
            }
        } else {
            return false;
        }
    }

    private AttentionCheck createAttentionCheck(final AttentionManagerInternal.AttentionCallbackInternal callbackInternal, final UserState userState) {
        return new AttentionCheck(callbackInternal, new IAttentionCallback.Stub() { // from class: com.android.server.attention.AttentionManagerService.1
            public void onSuccess(int result, long timestamp) {
                if (userState.mCurrentAttentionCheck.mIsFulfilled) {
                    return;
                }
                userState.mCurrentAttentionCheck.mIsFulfilled = true;
                callbackInternal.onSuccess(result, timestamp);
                logStats(result);
                synchronized (AttentionManagerService.this.mLock) {
                    if (userState.mAttentionCheckCacheBuffer == null) {
                        userState.mAttentionCheckCacheBuffer = new AttentionCheckCacheBuffer();
                    }
                    userState.mAttentionCheckCacheBuffer.add(new AttentionCheckCache(SystemClock.uptimeMillis(), result, timestamp));
                }
            }

            public void onFailure(int error) {
                if (userState.mCurrentAttentionCheck.mIsFulfilled) {
                    return;
                }
                userState.mCurrentAttentionCheck.mIsFulfilled = true;
                callbackInternal.onFailure(error);
                logStats(error);
            }

            private void logStats(int result) {
                FrameworkStatsLog.write((int) CecMessageType.GIVE_DEVICE_POWER_STATUS, result);
            }
        });
    }

    void cancelAttentionCheck(AttentionManagerInternal.AttentionCallbackInternal callbackInternal) {
        synchronized (this.mLock) {
            UserState userState = peekCurrentUserStateLocked();
            if (userState == null) {
                return;
            }
            if (!userState.mCurrentAttentionCheck.mCallbackInternal.equals(callbackInternal)) {
                Slog.w(LOG_TAG, "Cannot cancel a non-current request");
            } else {
                cancel(userState);
            }
        }
    }

    protected void freeIfInactiveLocked() {
        this.mAttentionHandler.removeMessages(1);
        this.mAttentionHandler.sendEmptyMessageDelayed(1, 60000L);
    }

    private void cancelAfterTimeoutLocked(long timeout) {
        this.mAttentionHandler.sendEmptyMessageDelayed(2, timeout);
    }

    protected UserState getOrCreateCurrentUserStateLocked() {
        return getOrCreateUserStateLocked(0);
    }

    protected UserState getOrCreateUserStateLocked(int userId) {
        UserState result = this.mUserStates.get(userId);
        if (result == null) {
            UserState result2 = new UserState(userId, this.mContext, this.mLock, this.mAttentionHandler, this.mComponentName);
            this.mUserStates.put(userId, result2);
            return result2;
        }
        return result;
    }

    protected UserState peekCurrentUserStateLocked() {
        return peekUserStateLocked(0);
    }

    private UserState peekUserStateLocked(int userId) {
        return this.mUserStates.get(userId);
    }

    private static String getServiceConfigPackage(Context context) {
        return context.getPackageManager().getAttentionServicePackageName();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ComponentName resolveAttentionService(Context context) {
        String resolvedPackage;
        String serviceConfigPackage = getServiceConfigPackage(context);
        int flags = 1048576;
        if (!TextUtils.isEmpty(sTestAttentionServicePackage)) {
            resolvedPackage = sTestAttentionServicePackage;
            flags = 128;
        } else if (TextUtils.isEmpty(serviceConfigPackage)) {
            return null;
        } else {
            resolvedPackage = serviceConfigPackage;
        }
        Intent intent = new Intent("android.service.attention.AttentionService").setPackage(resolvedPackage);
        ResolveInfo resolveInfo = context.getPackageManager().resolveService(intent, flags);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Slog.wtf(LOG_TAG, String.format("Service %s not found in package %s", "android.service.attention.AttentionService", serviceConfigPackage));
            return null;
        }
        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        String permission = serviceInfo.permission;
        if ("android.permission.BIND_ATTENTION_SERVICE".equals(permission)) {
            return serviceInfo.getComponentName();
        }
        Slog.e(LOG_TAG, String.format("Service %s should require %s permission. Found %s permission", serviceInfo.getComponentName(), "android.permission.BIND_ATTENTION_SERVICE", serviceInfo.permission));
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(IndentingPrintWriter ipw) {
        ipw.println("Attention Manager Service (dumpsys attention) state:\n");
        ipw.println("isServiceEnabled=" + isServiceEnabled());
        ipw.println("AttentionServicePackageName=" + getServiceConfigPackage(this.mContext));
        ipw.println("Resolved component:");
        if (this.mComponentName != null) {
            ipw.increaseIndent();
            ipw.println("Component=" + this.mComponentName.getPackageName());
            ipw.println("Class=" + this.mComponentName.getClassName());
            ipw.decreaseIndent();
        }
        synchronized (this.mLock) {
            int size = this.mUserStates.size();
            ipw.print("Number user states: ");
            ipw.println(size);
            if (size > 0) {
                ipw.increaseIndent();
                for (int i = 0; i < size; i++) {
                    UserState userState = this.mUserStates.valueAt(i);
                    ipw.print(i);
                    ipw.print(":");
                    userState.dump(ipw);
                    ipw.println();
                }
                ipw.decreaseIndent();
            }
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends AttentionManagerInternal {
        private LocalService() {
        }

        public boolean isAttentionServiceSupported() {
            return AttentionManagerService.this.isAttentionServiceSupported();
        }

        public boolean checkAttention(long timeout, AttentionManagerInternal.AttentionCallbackInternal callbackInternal) {
            return AttentionManagerService.this.checkAttention(timeout, callbackInternal);
        }

        public void cancelAttentionCheck(AttentionManagerInternal.AttentionCallbackInternal callbackInternal) {
            AttentionManagerService.this.cancelAttentionCheck(callbackInternal);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static final class AttentionCheckCacheBuffer {
        private final AttentionCheckCache[] mQueue = new AttentionCheckCache[5];
        private int mStartIndex = 0;
        private int mSize = 0;

        AttentionCheckCacheBuffer() {
        }

        public AttentionCheckCache getLast() {
            int i = this.mStartIndex;
            int i2 = this.mSize;
            int lastIdx = ((i + i2) - 1) % 5;
            if (i2 == 0) {
                return null;
            }
            return this.mQueue[lastIdx];
        }

        public void add(AttentionCheckCache cache) {
            int i = this.mStartIndex;
            int i2 = this.mSize;
            int nextIndex = (i + i2) % 5;
            this.mQueue[nextIndex] = cache;
            if (i2 == 5) {
                this.mStartIndex = i + 1;
            } else {
                this.mSize = i2 + 1;
            }
        }

        public AttentionCheckCache get(int offset) {
            if (offset >= this.mSize) {
                return null;
            }
            return this.mQueue[(this.mStartIndex + offset) % 5];
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static final class AttentionCheckCache {
        private final long mLastComputed;
        private final int mResult;
        private final long mTimestamp;

        AttentionCheckCache(long lastComputed, int result, long timestamp) {
            this.mLastComputed = lastComputed;
            this.mResult = result;
            this.mTimestamp = timestamp;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AttentionCheck {
        private final AttentionManagerInternal.AttentionCallbackInternal mCallbackInternal;
        private final IAttentionCallback mIAttentionCallback;
        private boolean mIsDispatched;
        private boolean mIsFulfilled;

        AttentionCheck(AttentionManagerInternal.AttentionCallbackInternal callbackInternal, IAttentionCallback iAttentionCallback) {
            this.mCallbackInternal = callbackInternal;
            this.mIAttentionCallback = iAttentionCallback;
        }

        void cancelInternal() {
            this.mIsFulfilled = true;
            this.mCallbackInternal.onFailure(3);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static class UserState {
        AttentionCheckCacheBuffer mAttentionCheckCacheBuffer;
        private final Handler mAttentionHandler;
        private boolean mBinding;
        private final ComponentName mComponentName;
        private final AttentionServiceConnection mConnection = new AttentionServiceConnection();
        private final Context mContext;
        AttentionCheck mCurrentAttentionCheck;
        private final Object mLock;
        IAttentionService mService;
        private final int mUserId;

        UserState(int userId, Context context, Object lock, Handler handler, ComponentName componentName) {
            this.mUserId = userId;
            Objects.requireNonNull(context);
            this.mContext = context;
            Objects.requireNonNull(lock);
            this.mLock = lock;
            Objects.requireNonNull(componentName);
            this.mComponentName = componentName;
            this.mAttentionHandler = handler;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void handlePendingCallbackLocked() {
            if (!this.mCurrentAttentionCheck.mIsDispatched) {
                IAttentionService iAttentionService = this.mService;
                if (iAttentionService != null) {
                    try {
                        iAttentionService.checkAttention(this.mCurrentAttentionCheck.mIAttentionCallback);
                        this.mCurrentAttentionCheck.mIsDispatched = true;
                        return;
                    } catch (RemoteException e) {
                        Slog.e(AttentionManagerService.LOG_TAG, "Cannot call into the AttentionService");
                        return;
                    }
                }
                this.mCurrentAttentionCheck.mCallbackInternal.onFailure(2);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void bindLocked() {
            if (this.mBinding || this.mService != null) {
                return;
            }
            this.mBinding = true;
            this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.attention.-$$Lambda$AttentionManagerService$UserState$2cc0P7pJchsigKpbEq7IoxYFsSM
                @Override // java.lang.Runnable
                public final void run() {
                    AttentionManagerService.UserState.this.lambda$bindLocked$0$AttentionManagerService$UserState();
                }
            });
        }

        public /* synthetic */ void lambda$bindLocked$0$AttentionManagerService$UserState() {
            Intent serviceIntent = new Intent("android.service.attention.AttentionService").setComponent(this.mComponentName);
            this.mContext.bindServiceAsUser(serviceIntent, this.mConnection, 67112961, UserHandle.CURRENT);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(IndentingPrintWriter pw) {
            pw.println("userId=" + this.mUserId);
            synchronized (this.mLock) {
                pw.println("binding=" + this.mBinding);
                pw.println("current attention check:");
                if (this.mCurrentAttentionCheck != null) {
                    pw.increaseIndent();
                    pw.println("is dispatched=" + this.mCurrentAttentionCheck.mIsDispatched);
                    pw.println("is fulfilled:=" + this.mCurrentAttentionCheck.mIsFulfilled);
                    pw.decreaseIndent();
                }
                if (this.mAttentionCheckCacheBuffer != null) {
                    pw.println("attention check cache:");
                    for (int i = 0; i < this.mAttentionCheckCacheBuffer.mSize; i++) {
                        pw.increaseIndent();
                        pw.println("timestamp=" + this.mAttentionCheckCacheBuffer.get(i).mTimestamp);
                        pw.println("result=" + this.mAttentionCheckCacheBuffer.get(i).mResult);
                        pw.decreaseIndent();
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class AttentionServiceConnection implements ServiceConnection {
            private AttentionServiceConnection() {
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                init(IAttentionService.Stub.asInterface(service));
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                cleanupService();
            }

            @Override // android.content.ServiceConnection
            public void onBindingDied(ComponentName name) {
                cleanupService();
            }

            @Override // android.content.ServiceConnection
            public void onNullBinding(ComponentName name) {
                cleanupService();
            }

            void cleanupService() {
                init(null);
            }

            private void init(IAttentionService service) {
                synchronized (UserState.this.mLock) {
                    UserState.this.mService = service;
                    UserState.this.mBinding = false;
                    UserState.this.handlePendingCallbackLocked();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class AttentionHandler extends Handler {
        private static final int ATTENTION_CHECK_TIMEOUT = 2;
        private static final int CHECK_CONNECTION_EXPIRATION = 1;

        AttentionHandler() {
            super(Looper.myLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 1) {
                if (i == 2) {
                    synchronized (AttentionManagerService.this.mLock) {
                        AttentionManagerService.this.cancel(AttentionManagerService.this.peekCurrentUserStateLocked());
                    }
                    return;
                }
                return;
            }
            for (int i2 = 0; i2 < AttentionManagerService.this.mUserStates.size(); i2++) {
                AttentionManagerService attentionManagerService = AttentionManagerService.this;
                attentionManagerService.cancelAndUnbindLocked((UserState) attentionManagerService.mUserStates.valueAt(i2));
            }
        }
    }

    void cancel(UserState userState) {
        if (userState == null || userState.mCurrentAttentionCheck == null || userState.mCurrentAttentionCheck.mIsFulfilled) {
            return;
        }
        if (userState.mService == null) {
            userState.mCurrentAttentionCheck.cancelInternal();
            return;
        }
        try {
            userState.mService.cancelAttentionCheck(userState.mCurrentAttentionCheck.mIAttentionCallback);
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Unable to cancel attention check");
            userState.mCurrentAttentionCheck.cancelInternal();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAndUnbindLocked(final UserState userState) {
        synchronized (this.mLock) {
            if (userState == null) {
                return;
            }
            cancel(userState);
            if (userState.mService == null) {
                return;
            }
            this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.attention.-$$Lambda$AttentionManagerService$2UthIuCIdjigpPv1U5Dxw_fo4nY
                @Override // java.lang.Runnable
                public final void run() {
                    AttentionManagerService.this.lambda$cancelAndUnbindLocked$0$AttentionManagerService(userState);
                }
            });
            userState.mConnection.cleanupService();
            this.mUserStates.remove(userState.mUserId);
        }
    }

    public /* synthetic */ void lambda$cancelAndUnbindLocked$0$AttentionManagerService(UserState userState) {
        this.mContext.unbindService(userState.mConnection);
    }

    /* loaded from: classes.dex */
    private final class ScreenStateReceiver extends BroadcastReceiver {
        private ScreenStateReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                AttentionManagerService attentionManagerService = AttentionManagerService.this;
                attentionManagerService.cancelAndUnbindLocked(attentionManagerService.peekCurrentUserStateLocked());
            }
        }
    }

    /* loaded from: classes.dex */
    private final class AttentionManagerServiceShellCommand extends ShellCommand {
        final TestableAttentionCallbackInternal mTestableAttentionCallback;

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public class TestableAttentionCallbackInternal extends AttentionManagerInternal.AttentionCallbackInternal {
            private int mLastCallbackCode = -1;

            TestableAttentionCallbackInternal() {
            }

            public void onSuccess(int result, long timestamp) {
                this.mLastCallbackCode = result;
            }

            public void onFailure(int error) {
                this.mLastCallbackCode = error;
            }

            public void reset() {
                this.mLastCallbackCode = -1;
            }

            public int getLastCallbackCode() {
                return this.mLastCallbackCode;
            }
        }

        private AttentionManagerServiceShellCommand() {
            this.mTestableAttentionCallback = new TestableAttentionCallbackInternal();
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Removed duplicated region for block: B:51:0x0098  */
        /* JADX WARN: Removed duplicated region for block: B:56:0x00a7 A[Catch: IllegalArgumentException -> 0x00b1, TryCatch #0 {IllegalArgumentException -> 0x00b1, blocks: (B:6:0x000c, B:7:0x0015, B:30:0x0057, B:32:0x005c, B:34:0x0061, B:36:0x0066, B:38:0x006f, B:52:0x009a, B:54:0x009f, B:55:0x00a6, B:56:0x00a7, B:43:0x0082, B:46:0x008b, B:58:0x00ac, B:9:0x0019, B:12:0x0023, B:15:0x002d, B:18:0x0038, B:21:0x0042), top: B:63:0x000c }] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public int onCommand(java.lang.String r9) {
            /*
                r8 = this;
                if (r9 != 0) goto L7
                int r0 = r8.handleDefaultCommands(r9)
                return r0
            L7:
                java.io.PrintWriter r0 = r8.getErrPrintWriter()
                r1 = -1
                int r2 = r9.hashCode()     // Catch: java.lang.IllegalArgumentException -> Lb1
                r3 = 0
                r4 = 4
                r5 = 3
                r6 = 2
                r7 = 1
                switch(r2) {
                    case -1208709968: goto L42;
                    case -1002424240: goto L38;
                    case -415045819: goto L2d;
                    case 3045982: goto L23;
                    case 1193447472: goto L19;
                    default: goto L18;
                }     // Catch: java.lang.IllegalArgumentException -> Lb1
            L18:
                goto L4c
            L19:
                java.lang.String r2 = "clearTestableAttentionService"
                boolean r2 = r9.equals(r2)     // Catch: java.lang.IllegalArgumentException -> Lb1
                if (r2 == 0) goto L18
                r2 = r5
                goto L4d
            L23:
                java.lang.String r2 = "call"
                boolean r2 = r9.equals(r2)     // Catch: java.lang.IllegalArgumentException -> Lb1
                if (r2 == 0) goto L18
                r2 = r7
                goto L4d
            L2d:
                java.lang.String r2 = "setTestableAttentionService"
                boolean r2 = r9.equals(r2)     // Catch: java.lang.IllegalArgumentException -> Lb1
                if (r2 == 0) goto L18
                r2 = r6
                goto L4d
            L38:
                java.lang.String r2 = "getAttentionServiceComponent"
                boolean r2 = r9.equals(r2)     // Catch: java.lang.IllegalArgumentException -> Lb1
                if (r2 == 0) goto L18
                r2 = r3
                goto L4d
            L42:
                java.lang.String r2 = "getLastTestCallbackCode"
                boolean r2 = r9.equals(r2)     // Catch: java.lang.IllegalArgumentException -> Lb1
                if (r2 == 0) goto L18
                r2 = r4
                goto L4d
            L4c:
                r2 = r1
            L4d:
                if (r2 == 0) goto Lac
                if (r2 == r7) goto L6f
                if (r2 == r6) goto L66
                if (r2 == r5) goto L61
                if (r2 == r4) goto L5c
                int r1 = r8.handleDefaultCommands(r9)     // Catch: java.lang.IllegalArgumentException -> Lb1
                return r1
            L5c:
                int r1 = r8.cmdGetLastTestCallbackCode()     // Catch: java.lang.IllegalArgumentException -> Lb1
                return r1
            L61:
                int r1 = r8.cmdClearTestableAttentionService()     // Catch: java.lang.IllegalArgumentException -> Lb1
                return r1
            L66:
                java.lang.String r2 = r8.getNextArgRequired()     // Catch: java.lang.IllegalArgumentException -> Lb1
                int r1 = r8.cmdSetTestableAttentionService(r2)     // Catch: java.lang.IllegalArgumentException -> Lb1
                return r1
            L6f:
                java.lang.String r2 = r8.getNextArgRequired()     // Catch: java.lang.IllegalArgumentException -> Lb1
                int r4 = r2.hashCode()     // Catch: java.lang.IllegalArgumentException -> Lb1
                r5 = 763077136(0x2d7ba210, float:1.4303683E-11)
                if (r4 == r5) goto L8b
                r5 = 1485997302(0x589284f6, float:1.28879808E15)
                if (r4 == r5) goto L82
            L81:
                goto L95
            L82:
                java.lang.String r4 = "checkAttention"
                boolean r2 = r2.equals(r4)     // Catch: java.lang.IllegalArgumentException -> Lb1
                if (r2 == 0) goto L81
                goto L96
            L8b:
                java.lang.String r3 = "cancelCheckAttention"
                boolean r2 = r2.equals(r3)     // Catch: java.lang.IllegalArgumentException -> Lb1
                if (r2 == 0) goto L81
                r3 = r7
                goto L96
            L95:
                r3 = r1
            L96:
                if (r3 == 0) goto La7
                if (r3 != r7) goto L9f
                int r1 = r8.cmdCallCancelAttention()     // Catch: java.lang.IllegalArgumentException -> Lb1
                return r1
            L9f:
                java.lang.IllegalArgumentException r2 = new java.lang.IllegalArgumentException     // Catch: java.lang.IllegalArgumentException -> Lb1
                java.lang.String r3 = "Invalid argument"
                r2.<init>(r3)     // Catch: java.lang.IllegalArgumentException -> Lb1
                throw r2     // Catch: java.lang.IllegalArgumentException -> Lb1
            La7:
                int r1 = r8.cmdCallCheckAttention()     // Catch: java.lang.IllegalArgumentException -> Lb1
                return r1
            Lac:
                int r1 = r8.cmdResolveAttentionServiceComponent()     // Catch: java.lang.IllegalArgumentException -> Lb1
                return r1
            Lb1:
                r2 = move-exception
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "Error: "
                r3.append(r4)
                java.lang.String r4 = r2.getMessage()
                r3.append(r4)
                java.lang.String r3 = r3.toString()
                r0.println(r3)
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.attention.AttentionManagerService.AttentionManagerServiceShellCommand.onCommand(java.lang.String):int");
        }

        private int cmdSetTestableAttentionService(String testingServicePackage) {
            PrintWriter out = getOutPrintWriter();
            if (!TextUtils.isEmpty(testingServicePackage)) {
                String unused = AttentionManagerService.sTestAttentionServicePackage = testingServicePackage;
                resetStates();
                out.println(AttentionManagerService.this.mComponentName != null ? "true" : "false");
                return 0;
            }
            out.println("false");
            return 0;
        }

        private int cmdClearTestableAttentionService() {
            String unused = AttentionManagerService.sTestAttentionServicePackage = "";
            this.mTestableAttentionCallback.reset();
            resetStates();
            return 0;
        }

        private int cmdCallCheckAttention() {
            PrintWriter out = getOutPrintWriter();
            boolean calledSuccessfully = AttentionManagerService.this.checkAttention(2000L, this.mTestableAttentionCallback);
            out.println(calledSuccessfully ? "true" : "false");
            return 0;
        }

        private int cmdCallCancelAttention() {
            PrintWriter out = getOutPrintWriter();
            AttentionManagerService.this.cancelAttentionCheck(this.mTestableAttentionCallback);
            out.println("true");
            return 0;
        }

        private int cmdResolveAttentionServiceComponent() {
            PrintWriter out = getOutPrintWriter();
            ComponentName resolvedComponent = AttentionManagerService.resolveAttentionService(AttentionManagerService.this.mContext);
            out.println(resolvedComponent != null ? resolvedComponent.flattenToShortString() : "");
            return 0;
        }

        private int cmdGetLastTestCallbackCode() {
            PrintWriter out = getOutPrintWriter();
            out.println(this.mTestableAttentionCallback.getLastCallbackCode());
            return 0;
        }

        private void resetStates() {
            AttentionManagerService attentionManagerService = AttentionManagerService.this;
            attentionManagerService.mComponentName = AttentionManagerService.resolveAttentionService(attentionManagerService.mContext);
            AttentionManagerService.this.mUserStates.clear();
        }

        public void onHelp() {
            PrintWriter out = getOutPrintWriter();
            out.println("Attention commands: ");
            out.println("  setTestableAttentionService <service_package>: Bind to a custom implementation of attention service");
            out.println("  ---<service_package>:");
            out.println("       := Package containing the Attention Service implementation to bind to");
            out.println("  ---returns:");
            out.println("       := true, if was bound successfully");
            out.println("       := false, if was not bound successfully");
            out.println("  clearTestableAttentionService: Undo custom bindings. Revert to previous behavior");
            out.println("  getAttentionServiceComponent: Get the current service component string");
            out.println("  ---returns:");
            out.println("       := If valid, the component string (in shorten form) for the currently bound service.");
            out.println("       := else, empty string");
            out.println("  call checkAttention: Calls check attention");
            out.println("  ---returns:");
            out.println("       := true, if the call was successfully dispatched to the service implementation. (to see the result, call getLastTestCallbackCode)");
            out.println("       := false, otherwise");
            out.println("  call cancelCheckAttention: Cancels check attention");
            out.println("  getLastTestCallbackCode");
            out.println("  ---returns:");
            out.println("       := An integer, representing the last callback code received from the bounded implementation. If none, it will return -1");
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        AttentionManagerServiceShellCommand mAttentionManagerServiceShellCommand;

        private BinderService() {
            this.mAttentionManagerServiceShellCommand = new AttentionManagerServiceShellCommand();
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            this.mAttentionManagerServiceShellCommand.exec(this, in, out, err, args, callback, resultReceiver);
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(AttentionManagerService.this.mContext, AttentionManagerService.LOG_TAG, pw)) {
                AttentionManagerService.this.dumpInternal(new IndentingPrintWriter(pw, "  "));
            }
        }
    }
}