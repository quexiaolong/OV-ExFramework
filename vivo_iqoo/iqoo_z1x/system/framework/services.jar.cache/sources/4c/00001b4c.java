package com.android.server.utils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.utils.ManagedApplicationService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/* loaded from: classes2.dex */
public class ManagedApplicationService {
    private static final int MAX_RETRY_COUNT = 4;
    private static final long MAX_RETRY_DURATION_MS = 16000;
    private static final long MIN_RETRY_DURATION_MS = 2000;
    public static final int RETRY_BEST_EFFORT = 3;
    public static final int RETRY_FOREVER = 1;
    public static final int RETRY_NEVER = 2;
    private static final long RETRY_RESET_TIME_MS = 64000;
    private IInterface mBoundInterface;
    private final BinderChecker mChecker;
    private final int mClientLabel;
    private final ComponentName mComponent;
    private ServiceConnection mConnection;
    private final Context mContext;
    private final EventCallback mEventCb;
    private final Handler mHandler;
    private final boolean mIsImportant;
    private long mLastRetryTimeMs;
    private PendingEvent mPendingEvent;
    private int mRetryCount;
    private final int mRetryType;
    private boolean mRetrying;
    private final String mSettingsAction;
    private final int mUserId;
    private final String TAG = getClass().getSimpleName();
    private final Runnable mRetryRunnable = new Runnable() { // from class: com.android.server.utils.-$$Lambda$ManagedApplicationService$TUtdiUHqGW7Fae8jX7ATvPxzdeM
        @Override // java.lang.Runnable
        public final void run() {
            ManagedApplicationService.lambda$TUtdiUHqGW7Fae8jX7ATvPxzdeM(ManagedApplicationService.this);
        }
    };
    private final Object mLock = new Object();
    private long mNextRetryDurationMs = MIN_RETRY_DURATION_MS;

    /* loaded from: classes2.dex */
    public interface BinderChecker {
        IInterface asInterface(IBinder iBinder);

        boolean checkType(IInterface iInterface);
    }

    /* loaded from: classes2.dex */
    public interface EventCallback {
        void onServiceEvent(LogEvent logEvent);
    }

    /* loaded from: classes2.dex */
    public interface LogFormattable {
        String toLogString(SimpleDateFormat simpleDateFormat);
    }

    /* loaded from: classes2.dex */
    public interface PendingEvent {
        void runEvent(IInterface iInterface) throws RemoteException;
    }

    public static /* synthetic */ void lambda$TUtdiUHqGW7Fae8jX7ATvPxzdeM(ManagedApplicationService managedApplicationService) {
        managedApplicationService.doRetry();
    }

    /* loaded from: classes2.dex */
    public static class LogEvent implements LogFormattable {
        public static final int EVENT_BINDING_DIED = 3;
        public static final int EVENT_CONNECTED = 1;
        public static final int EVENT_DISCONNECTED = 2;
        public static final int EVENT_STOPPED_PERMANENTLY = 4;
        public final ComponentName component;
        public final int event;
        public final long timestamp;

        public LogEvent(long timestamp, ComponentName component, int event) {
            this.timestamp = timestamp;
            this.component = component;
            this.event = event;
        }

        @Override // com.android.server.utils.ManagedApplicationService.LogFormattable
        public String toLogString(SimpleDateFormat dateFormat) {
            StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date(this.timestamp)));
            sb.append("   ");
            sb.append(eventToString(this.event));
            sb.append(" Managed Service: ");
            ComponentName componentName = this.component;
            sb.append(componentName == null ? "None" : componentName.flattenToString());
            return sb.toString();
        }

        public static String eventToString(int event) {
            if (event != 1) {
                if (event != 2) {
                    if (event != 3) {
                        if (event == 4) {
                            return "Permanently Stopped";
                        }
                        return "Unknown Event Occurred";
                    }
                    return "Binding Died For";
                }
                return "Disconnected";
            }
            return "Connected";
        }
    }

    private ManagedApplicationService(Context context, ComponentName component, int userId, int clientLabel, String settingsAction, BinderChecker binderChecker, boolean isImportant, int retryType, Handler handler, EventCallback eventCallback) {
        this.mContext = context;
        this.mComponent = component;
        this.mUserId = userId;
        this.mClientLabel = clientLabel;
        this.mSettingsAction = settingsAction;
        this.mChecker = binderChecker;
        this.mIsImportant = isImportant;
        this.mRetryType = retryType;
        this.mHandler = handler;
        this.mEventCb = eventCallback;
    }

    public static ManagedApplicationService build(Context context, ComponentName component, int userId, int clientLabel, String settingsAction, BinderChecker binderChecker, boolean isImportant, int retryType, Handler handler, EventCallback eventCallback) {
        return new ManagedApplicationService(context, component, userId, clientLabel, settingsAction, binderChecker, isImportant, retryType, handler, eventCallback);
    }

    public int getUserId() {
        return this.mUserId;
    }

    public ComponentName getComponent() {
        return this.mComponent;
    }

    public boolean disconnectIfNotMatching(ComponentName componentName, int userId) {
        if (matches(componentName, userId)) {
            return false;
        }
        disconnect();
        return true;
    }

    public void sendEvent(PendingEvent event) {
        IInterface iface;
        synchronized (this.mLock) {
            iface = this.mBoundInterface;
            if (iface == null) {
                this.mPendingEvent = event;
            }
        }
        if (iface != null) {
            try {
                event.runEvent(iface);
            } catch (RemoteException | RuntimeException ex) {
                Slog.e(this.TAG, "Received exception from user service: ", ex);
            }
        }
    }

    public void disconnect() {
        synchronized (this.mLock) {
            if (this.mConnection == null) {
                return;
            }
            this.mContext.unbindService(this.mConnection);
            this.mConnection = null;
            this.mBoundInterface = null;
        }
    }

    public void connect() {
        synchronized (this.mLock) {
            if (this.mConnection != null) {
                return;
            }
            Intent intent = new Intent().setComponent(this.mComponent);
            if (this.mClientLabel != 0) {
                intent.putExtra("android.intent.extra.client_label", this.mClientLabel);
            }
            if (this.mSettingsAction != null) {
                intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent(this.mSettingsAction), 0));
            }
            this.mConnection = new AnonymousClass1();
            int flags = AudioFormat.AAC_MAIN;
            if (this.mIsImportant) {
                flags = 67108865 | 64;
            }
            try {
                if (!this.mContext.bindServiceAsUser(intent, this.mConnection, flags, new UserHandle(this.mUserId))) {
                    String str = this.TAG;
                    Slog.w(str, "Unable to bind service: " + intent);
                    startRetriesLocked();
                }
            } catch (SecurityException e) {
                String str2 = this.TAG;
                Slog.w(str2, "Unable to bind service: " + intent, e);
                startRetriesLocked();
            }
        }
    }

    /* renamed from: com.android.server.utils.ManagedApplicationService$1 */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 implements ServiceConnection {
        AnonymousClass1() {
            ManagedApplicationService.this = this$0;
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName componentName) {
            final long timestamp = System.currentTimeMillis();
            String str = ManagedApplicationService.this.TAG;
            Slog.w(str, "Service binding died: " + componentName);
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() { // from class: com.android.server.utils.-$$Lambda$ManagedApplicationService$1$u8NdnzWjrb-KhRpDHf8fTyh3KVU
                    @Override // java.lang.Runnable
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1.this.lambda$onBindingDied$0$ManagedApplicationService$1(timestamp);
                    }
                });
                ManagedApplicationService.this.mBoundInterface = null;
                ManagedApplicationService.this.startRetriesLocked();
            }
        }

        public /* synthetic */ void lambda$onBindingDied$0$ManagedApplicationService$1(long timestamp) {
            ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 3));
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final long timestamp = System.currentTimeMillis();
            String str = ManagedApplicationService.this.TAG;
            Slog.i(str, "Service connected: " + componentName);
            IInterface iface = null;
            PendingEvent pendingEvent = null;
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() { // from class: com.android.server.utils.-$$Lambda$ManagedApplicationService$1$IyJ0KZQns9OXjnHsop6Gzx7uhvA
                    @Override // java.lang.Runnable
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1.this.lambda$onServiceConnected$1$ManagedApplicationService$1(timestamp);
                    }
                });
                ManagedApplicationService.this.stopRetriesLocked();
                ManagedApplicationService.this.mBoundInterface = null;
                if (ManagedApplicationService.this.mChecker != null) {
                    ManagedApplicationService.this.mBoundInterface = ManagedApplicationService.this.mChecker.asInterface(iBinder);
                    if (!ManagedApplicationService.this.mChecker.checkType(ManagedApplicationService.this.mBoundInterface)) {
                        ManagedApplicationService.this.mBoundInterface = null;
                        String str2 = ManagedApplicationService.this.TAG;
                        Slog.w(str2, "Invalid binder from " + componentName);
                        ManagedApplicationService.this.startRetriesLocked();
                        return;
                    }
                    iface = ManagedApplicationService.this.mBoundInterface;
                    pendingEvent = ManagedApplicationService.this.mPendingEvent;
                    ManagedApplicationService.this.mPendingEvent = null;
                }
                if (iface != null && pendingEvent != null) {
                    try {
                        pendingEvent.runEvent(iface);
                    } catch (RemoteException | RuntimeException ex) {
                        Slog.e(ManagedApplicationService.this.TAG, "Received exception from user service: ", ex);
                        ManagedApplicationService.this.startRetriesLocked();
                    }
                }
            }
        }

        public /* synthetic */ void lambda$onServiceConnected$1$ManagedApplicationService$1(long timestamp) {
            ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 1));
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            final long timestamp = System.currentTimeMillis();
            String str = ManagedApplicationService.this.TAG;
            Slog.w(str, "Service disconnected: " + componentName);
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() { // from class: com.android.server.utils.-$$Lambda$ManagedApplicationService$1$iBg5-L6PAieAfuWNXxIPqvSlAAg
                    @Override // java.lang.Runnable
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1.this.lambda$onServiceDisconnected$2$ManagedApplicationService$1(timestamp);
                    }
                });
                ManagedApplicationService.this.mBoundInterface = null;
                ManagedApplicationService.this.startRetriesLocked();
            }
        }

        public /* synthetic */ void lambda$onServiceDisconnected$2$ManagedApplicationService$1(long timestamp) {
            ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 2));
        }
    }

    private boolean matches(ComponentName component, int userId) {
        return Objects.equals(this.mComponent, component) && this.mUserId == userId;
    }

    public void startRetriesLocked() {
        if (checkAndDeliverServiceDiedCbLocked()) {
            disconnect();
        } else if (this.mRetrying) {
        } else {
            this.mRetrying = true;
            queueRetryLocked();
        }
    }

    public void stopRetriesLocked() {
        this.mRetrying = false;
        this.mHandler.removeCallbacks(this.mRetryRunnable);
    }

    private void queueRetryLocked() {
        long now = SystemClock.uptimeMillis();
        if (now - this.mLastRetryTimeMs > RETRY_RESET_TIME_MS) {
            this.mNextRetryDurationMs = MIN_RETRY_DURATION_MS;
            this.mRetryCount = 0;
        }
        this.mLastRetryTimeMs = now;
        this.mHandler.postDelayed(this.mRetryRunnable, this.mNextRetryDurationMs);
        this.mNextRetryDurationMs = Math.min(this.mNextRetryDurationMs * 2, (long) MAX_RETRY_DURATION_MS);
        this.mRetryCount++;
    }

    private boolean checkAndDeliverServiceDiedCbLocked() {
        int i = this.mRetryType;
        if (i == 2 || (i == 3 && this.mRetryCount >= 4)) {
            String str = this.TAG;
            Slog.e(str, "Service " + this.mComponent + " has died too much, not retrying.");
            if (this.mEventCb != null) {
                final long timestamp = System.currentTimeMillis();
                this.mHandler.post(new Runnable() { // from class: com.android.server.utils.-$$Lambda$ManagedApplicationService$7a-sAFwcUuC9yt8nXYlr0jScFcs
                    @Override // java.lang.Runnable
                    public final void run() {
                        ManagedApplicationService.this.lambda$checkAndDeliverServiceDiedCbLocked$0$ManagedApplicationService(timestamp);
                    }
                });
                return true;
            }
            return true;
        }
        return false;
    }

    public /* synthetic */ void lambda$checkAndDeliverServiceDiedCbLocked$0$ManagedApplicationService(long timestamp) {
        this.mEventCb.onServiceEvent(new LogEvent(timestamp, this.mComponent, 4));
    }

    public void doRetry() {
        synchronized (this.mLock) {
            if (this.mConnection == null) {
                return;
            }
            if (this.mRetrying) {
                String str = this.TAG;
                Slog.i(str, "Attempting to reconnect " + this.mComponent + "...");
                disconnect();
                if (checkAndDeliverServiceDiedCbLocked()) {
                    return;
                }
                queueRetryLocked();
                connect();
            }
        }
    }
}