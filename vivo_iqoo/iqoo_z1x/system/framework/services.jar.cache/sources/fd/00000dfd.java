package com.android.server.dreams;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.service.dreams.IDreamService;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.logging.MetricsLogger;
import com.android.server.dreams.DreamController;
import com.android.server.job.controllers.JobStatus;
import com.android.server.policy.PhoneWindowManager;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DreamController {
    private static final int DREAM_CONNECTION_TIMEOUT = 5000;
    private static final int DREAM_FINISH_TIMEOUT = 5000;
    private static final String TAG = "DreamController";
    private final Intent mCloseNotificationShadeIntent;
    private final Context mContext;
    private DreamRecord mCurrentDream;
    private long mDreamStartTime;
    private final Handler mHandler;
    private final Listener mListener;
    private String mSavedStopReason;
    private final Intent mDreamingStartedIntent = new Intent("android.intent.action.DREAMING_STARTED").addFlags(1073741824);
    private final Intent mDreamingStoppedIntent = new Intent("android.intent.action.DREAMING_STOPPED").addFlags(1073741824);
    private final Runnable mStopUnconnectedDreamRunnable = new Runnable() { // from class: com.android.server.dreams.DreamController.1
        @Override // java.lang.Runnable
        public void run() {
            if (DreamController.this.mCurrentDream != null && DreamController.this.mCurrentDream.mBound && !DreamController.this.mCurrentDream.mConnected) {
                Slog.w(DreamController.TAG, "Bound dream did not connect in the time allotted");
                DreamController.this.stopDream(true, "slow to connect");
            }
        }
    };
    private final Runnable mStopStubbornDreamRunnable = new Runnable() { // from class: com.android.server.dreams.-$$Lambda$DreamController$MzWLPaVogrekgPcs4ryibDvi1xA
        @Override // java.lang.Runnable
        public final void run() {
            DreamController.this.lambda$new$0$DreamController();
        }
    };
    private final IWindowManager mIWindowManager = WindowManagerGlobal.getWindowManagerService();

    /* loaded from: classes.dex */
    public interface Listener {
        void onDreamStopped(Binder binder);
    }

    public /* synthetic */ void lambda$new$0$DreamController() {
        Slog.w(TAG, "Stubborn dream did not finish itself in the time allotted");
        stopDream(true, "slow to finish");
        this.mSavedStopReason = null;
    }

    public DreamController(Context context, Handler handler, Listener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        Intent intent = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        this.mCloseNotificationShadeIntent = intent;
        intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, "dream");
    }

    public void dump(PrintWriter pw) {
        pw.println("Dreamland:");
        if (this.mCurrentDream != null) {
            pw.println("  mCurrentDream:");
            pw.println("    mToken=" + this.mCurrentDream.mToken);
            pw.println("    mName=" + this.mCurrentDream.mName);
            pw.println("    mIsTest=" + this.mCurrentDream.mIsTest);
            pw.println("    mCanDoze=" + this.mCurrentDream.mCanDoze);
            pw.println("    mUserId=" + this.mCurrentDream.mUserId);
            pw.println("    mBound=" + this.mCurrentDream.mBound);
            pw.println("    mService=" + this.mCurrentDream.mService);
            pw.println("    mSentStartBroadcast=" + this.mCurrentDream.mSentStartBroadcast);
            pw.println("    mWakingGently=" + this.mCurrentDream.mWakingGently);
            return;
        }
        pw.println("  mCurrentDream: null");
    }

    public void startDream(Binder token, ComponentName name, boolean isTest, boolean canDoze, int userId, PowerManager.WakeLock wakeLock) {
        long j;
        StringBuilder sb;
        stopDream(true, "starting new dream");
        Trace.traceBegin(131072L, "startDream");
        try {
            this.mContext.sendBroadcastAsUser(this.mCloseNotificationShadeIntent, UserHandle.ALL);
            sb = new StringBuilder();
            sb.append("Starting dream: name=");
            sb.append(name);
            sb.append(", isTest=");
        } catch (Throwable th) {
            ex = th;
        }
        try {
            sb.append(isTest);
            sb.append(", canDoze=");
            sb.append(canDoze);
            sb.append(", userId=");
            sb.append(userId);
            Slog.i(TAG, sb.toString());
            j = 131072;
            try {
                this.mCurrentDream = new DreamRecord(token, name, isTest, canDoze, userId, wakeLock);
                this.mDreamStartTime = SystemClock.elapsedRealtime();
                MetricsLogger.visible(this.mContext, this.mCurrentDream.mCanDoze ? 223 : 222);
                Intent intent = new Intent("android.service.dreams.DreamService");
                intent.setComponent(name);
                intent.addFlags(8388608);
                try {
                    if (!this.mContext.bindServiceAsUser(intent, this.mCurrentDream, AudioFormat.AAC_MAIN, new UserHandle(userId))) {
                        Slog.e(TAG, "Unable to bind dream service: " + intent);
                        stopDream(true, "bindService failed");
                        Trace.traceEnd(131072L);
                        return;
                    }
                    this.mCurrentDream.mBound = true;
                    this.mHandler.postDelayed(this.mStopUnconnectedDreamRunnable, 5000L);
                    Trace.traceEnd(131072L);
                } catch (SecurityException ex) {
                    Slog.e(TAG, "Unable to bind dream service: " + intent, ex);
                    stopDream(true, "unable to bind service: SecExp.");
                    Trace.traceEnd(131072L);
                }
            } catch (Throwable th2) {
                ex = th2;
                Trace.traceEnd(j);
                throw ex;
            }
        } catch (Throwable th3) {
            ex = th3;
            j = 131072;
            Trace.traceEnd(j);
            throw ex;
        }
    }

    public void stopDream(boolean immediate, String reason) {
        String str;
        if (this.mCurrentDream == null) {
            return;
        }
        Trace.traceBegin(131072L, "stopDream");
        if (!immediate) {
            try {
                if (this.mCurrentDream.mWakingGently) {
                    return;
                }
                if (this.mCurrentDream.mService != null) {
                    this.mCurrentDream.mWakingGently = true;
                    try {
                        this.mSavedStopReason = reason;
                        this.mCurrentDream.mService.wakeUp();
                        this.mHandler.postDelayed(this.mStopStubbornDreamRunnable, 5000L);
                        return;
                    } catch (RemoteException e) {
                    }
                }
            } finally {
                Trace.traceEnd(131072L);
            }
        }
        final DreamRecord oldDream = this.mCurrentDream;
        this.mCurrentDream = null;
        StringBuilder sb = new StringBuilder();
        sb.append("Stopping dream: name=");
        sb.append(oldDream.mName);
        sb.append(", isTest=");
        sb.append(oldDream.mIsTest);
        sb.append(", canDoze=");
        sb.append(oldDream.mCanDoze);
        sb.append(", userId=");
        sb.append(oldDream.mUserId);
        sb.append(", reason='");
        sb.append(reason);
        sb.append("'");
        if (this.mSavedStopReason == null) {
            str = "";
        } else {
            str = "(from '" + this.mSavedStopReason + "')";
        }
        sb.append(str);
        Slog.i(TAG, sb.toString());
        MetricsLogger.hidden(this.mContext, oldDream.mCanDoze ? 223 : 222);
        MetricsLogger.histogram(this.mContext, oldDream.mCanDoze ? "dozing_minutes" : "dreaming_minutes", (int) ((SystemClock.elapsedRealtime() - this.mDreamStartTime) / 60000));
        this.mHandler.removeCallbacks(this.mStopUnconnectedDreamRunnable);
        this.mHandler.removeCallbacks(this.mStopStubbornDreamRunnable);
        this.mSavedStopReason = null;
        if (oldDream.mSentStartBroadcast) {
            this.mContext.sendBroadcastAsUser(this.mDreamingStoppedIntent, UserHandle.ALL);
        }
        if (oldDream.mService != null) {
            try {
                oldDream.mService.detach();
            } catch (RemoteException e2) {
            }
            try {
                oldDream.mService.asBinder().unlinkToDeath(oldDream, 0);
            } catch (NoSuchElementException e3) {
            }
            oldDream.mService = null;
        }
        if (oldDream.mBound) {
            this.mContext.unbindService(oldDream);
        }
        oldDream.releaseWakeLockIfNeeded();
        this.mHandler.post(new Runnable() { // from class: com.android.server.dreams.-$$Lambda$DreamController$NsbIx0iECm45E_fdqE55LTS32LQ
            @Override // java.lang.Runnable
            public final void run() {
                DreamController.this.lambda$stopDream$1$DreamController(oldDream);
            }
        });
    }

    public /* synthetic */ void lambda$stopDream$1$DreamController(DreamRecord oldDream) {
        this.mListener.onDreamStopped(oldDream.mToken);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void attach(IDreamService service) {
        try {
            service.asBinder().linkToDeath(this.mCurrentDream, 0);
            service.attach(this.mCurrentDream.mToken, this.mCurrentDream.mCanDoze, this.mCurrentDream.mDreamingStartedCallback);
            this.mCurrentDream.mService = service;
            if (!this.mCurrentDream.mIsTest) {
                this.mContext.sendBroadcastAsUser(this.mDreamingStartedIntent, UserHandle.ALL);
                this.mCurrentDream.mSentStartBroadcast = true;
            }
        } catch (RemoteException ex) {
            Slog.e(TAG, "The dream service died unexpectedly.", ex);
            stopDream(true, "attach failed");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DreamRecord implements IBinder.DeathRecipient, ServiceConnection {
        public boolean mBound;
        public final boolean mCanDoze;
        public boolean mConnected;
        public final boolean mIsTest;
        public final ComponentName mName;
        public boolean mSentStartBroadcast;
        public IDreamService mService;
        public final Binder mToken;
        public final int mUserId;
        public PowerManager.WakeLock mWakeLock;
        public boolean mWakingGently;
        final Runnable mReleaseWakeLockIfNeeded = new Runnable() { // from class: com.android.server.dreams.-$$Lambda$gXC4nM2f5GMCBX0ED45DCQQjqv0
            @Override // java.lang.Runnable
            public final void run() {
                DreamController.DreamRecord.this.releaseWakeLockIfNeeded();
            }
        };
        final IRemoteCallback mDreamingStartedCallback = new IRemoteCallback.Stub() { // from class: com.android.server.dreams.DreamController.DreamRecord.1
            public void sendResult(Bundle data) throws RemoteException {
                DreamController.this.mHandler.post(DreamRecord.this.mReleaseWakeLockIfNeeded);
            }
        };

        public DreamRecord(Binder token, ComponentName name, boolean isTest, boolean canDoze, int userId, PowerManager.WakeLock wakeLock) {
            this.mToken = token;
            this.mName = name;
            this.mIsTest = isTest;
            this.mCanDoze = canDoze;
            this.mUserId = userId;
            this.mWakeLock = wakeLock;
            wakeLock.acquire();
            DreamController.this.mHandler.postDelayed(this.mReleaseWakeLockIfNeeded, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            DreamController.this.mHandler.post(new Runnable() { // from class: com.android.server.dreams.-$$Lambda$DreamController$DreamRecord$a6xKVQPRvHllqmi3b3aluvuTMEM
                @Override // java.lang.Runnable
                public final void run() {
                    DreamController.DreamRecord.this.lambda$binderDied$0$DreamController$DreamRecord();
                }
            });
        }

        public /* synthetic */ void lambda$binderDied$0$DreamController$DreamRecord() {
            this.mService = null;
            if (DreamController.this.mCurrentDream == this) {
                DreamController.this.stopDream(true, "binder died");
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, final IBinder service) {
            DreamController.this.mHandler.post(new Runnable() { // from class: com.android.server.dreams.-$$Lambda$DreamController$DreamRecord$RZAkATuUi2Nb2UNKTKrQg_Oz3Cw
                @Override // java.lang.Runnable
                public final void run() {
                    DreamController.DreamRecord.this.lambda$onServiceConnected$1$DreamController$DreamRecord(service);
                }
            });
        }

        public /* synthetic */ void lambda$onServiceConnected$1$DreamController$DreamRecord(IBinder service) {
            this.mConnected = true;
            if (DreamController.this.mCurrentDream == this && this.mService == null) {
                DreamController.this.attach(IDreamService.Stub.asInterface(service));
            } else {
                releaseWakeLockIfNeeded();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            DreamController.this.mHandler.post(new Runnable() { // from class: com.android.server.dreams.-$$Lambda$DreamController$DreamRecord$dxWbx4rgpHpZ1Hx0p_kP0KmKxQk
                @Override // java.lang.Runnable
                public final void run() {
                    DreamController.DreamRecord.this.lambda$onServiceDisconnected$2$DreamController$DreamRecord();
                }
            });
        }

        public /* synthetic */ void lambda$onServiceDisconnected$2$DreamController$DreamRecord() {
            this.mService = null;
            if (DreamController.this.mCurrentDream == this) {
                DreamController.this.stopDream(true, "service disconnected");
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void releaseWakeLockIfNeeded() {
            PowerManager.WakeLock wakeLock = this.mWakeLock;
            if (wakeLock != null) {
                wakeLock.release();
                this.mWakeLock = null;
                DreamController.this.mHandler.removeCallbacks(this.mReleaseWakeLockIfNeeded);
            }
        }
    }
}