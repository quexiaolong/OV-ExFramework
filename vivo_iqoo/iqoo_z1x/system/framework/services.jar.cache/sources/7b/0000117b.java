package com.android.server.location.gnss;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.util.NtpTrustedTime;
import java.util.Date;

/* loaded from: classes.dex */
public class NtpTimeHelper {
    private static final boolean DEBUG = Log.isLoggable("NtpTimeHelper", 3);
    private static final long MAX_RETRY_INTERVAL = 14400000;
    static final long NTP_INTERVAL = 86400000;
    static final long RETRY_INTERVAL = 300000;
    private static final int STATE_IDLE = 2;
    private static final int STATE_PENDING_NETWORK = 0;
    private static final int STATE_RETRIEVING_AND_INJECTING = 1;
    private static final String TAG = "NtpTimeHelper";
    private static final String WAKELOCK_KEY = "NtpTimeHelper";
    private static final long WAKELOCK_TIMEOUT_MILLIS = 60000;
    private final InjectNtpTimeCallback mCallback;
    private final ConnectivityManager mConnMgr;
    private final Handler mHandler;
    private int mInjectNtpTimeState;
    private final ExponentialBackOff mNtpBackOff;
    private final NtpTrustedTime mNtpTime;
    private boolean mOnDemandTimeInjection;
    private final PowerManager.WakeLock mWakeLock;

    /* loaded from: classes.dex */
    public interface InjectNtpTimeCallback {
        void injectTime(long j, long j2, int i);
    }

    /* renamed from: lambda$0f3JRUuSYH-K-brPBZMOA96D-q4 */
    public static /* synthetic */ void m39lambda$0f3JRUuSYHKbrPBZMOA96Dq4(NtpTimeHelper ntpTimeHelper) {
        ntpTimeHelper.blockingGetNtpTimeAndInject();
    }

    NtpTimeHelper(Context context, Looper looper, InjectNtpTimeCallback callback, NtpTrustedTime ntpTime) {
        this.mNtpBackOff = new ExponentialBackOff(300000L, 14400000L);
        this.mInjectNtpTimeState = 0;
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mCallback = callback;
        this.mNtpTime = ntpTime;
        this.mHandler = new Handler(looper);
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, "NtpTimeHelper");
    }

    public NtpTimeHelper(Context context, Looper looper, InjectNtpTimeCallback callback) {
        this(context, looper, callback, NtpTrustedTime.getInstance(context));
    }

    public synchronized void enablePeriodicTimeInjection() {
        this.mOnDemandTimeInjection = true;
    }

    public synchronized void onNetworkAvailable() {
        if (this.mInjectNtpTimeState == 0) {
            retrieveAndInjectNtpTime();
        }
    }

    private boolean isNetworkConnected() {
        NetworkInfo activeNetworkInfo = this.mConnMgr.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public synchronized void retrieveAndInjectNtpTime() {
        if (this.mInjectNtpTimeState == 1) {
            return;
        }
        if (!isNetworkConnected()) {
            this.mInjectNtpTimeState = 0;
            return;
        }
        this.mInjectNtpTimeState = 1;
        this.mWakeLock.acquire(60000L);
        new Thread(new Runnable() { // from class: com.android.server.location.gnss.-$$Lambda$NtpTimeHelper$0f3JRUuSYH-K-brPBZMOA96D-q4
            @Override // java.lang.Runnable
            public final void run() {
                NtpTimeHelper.m39lambda$0f3JRUuSYHKbrPBZMOA96Dq4(NtpTimeHelper.this);
            }
        }).start();
    }

    public void blockingGetNtpTimeAndInject() {
        long delay;
        boolean refreshSuccess = true;
        NtpTrustedTime.TimeResult ntpResult = this.mNtpTime.getCachedTimeResult();
        if (ntpResult == null || ntpResult.getAgeMillis() >= 86400000) {
            refreshSuccess = this.mNtpTime.forceRefresh();
        }
        synchronized (this) {
            this.mInjectNtpTimeState = 2;
            NtpTrustedTime.TimeResult ntpResult2 = this.mNtpTime.getCachedTimeResult();
            if (ntpResult2 != null && ntpResult2.getAgeMillis() < 86400000) {
                final long time = ntpResult2.getTimeMillis();
                final long timeReference = ntpResult2.getElapsedRealtimeMillis();
                final long certainty = ntpResult2.getCertaintyMillis();
                if (DEBUG) {
                    long now = System.currentTimeMillis();
                    Log.d("NtpTimeHelper", "NTP server returned: " + time + " (" + new Date(time) + ") ntpResult: " + ntpResult2 + " system time offset: " + (time - now));
                }
                this.mHandler.post(new Runnable() { // from class: com.android.server.location.gnss.-$$Lambda$NtpTimeHelper$4YVWiahGRCeX2AoEdhSeek4fqhQ
                    @Override // java.lang.Runnable
                    public final void run() {
                        NtpTimeHelper.this.lambda$blockingGetNtpTimeAndInject$0$NtpTimeHelper(time, timeReference, certainty);
                    }
                });
                delay = 86400000;
                this.mNtpBackOff.reset();
            } else {
                Log.e("NtpTimeHelper", "requestTime failed");
                delay = this.mNtpBackOff.nextBackoffMillis();
            }
            if (DEBUG) {
                Log.d("NtpTimeHelper", String.format("onDemandTimeInjection=%s, refreshSuccess=%s, delay=%s", Boolean.valueOf(this.mOnDemandTimeInjection), Boolean.valueOf(refreshSuccess), Long.valueOf(delay)));
            }
            if (this.mOnDemandTimeInjection || !refreshSuccess) {
                this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.location.gnss.-$$Lambda$UoXKjYaYgPPHqNIgiLAobtz5XAU
                    @Override // java.lang.Runnable
                    public final void run() {
                        NtpTimeHelper.this.retrieveAndInjectNtpTime();
                    }
                }, delay);
            }
        }
        this.mWakeLock.release();
    }

    public /* synthetic */ void lambda$blockingGetNtpTimeAndInject$0$NtpTimeHelper(long time, long timeReference, long certainty) {
        this.mCallback.injectTime(time, timeReference, (int) certainty);
    }

    public void setNtpTimeStateIdle() {
        this.mInjectNtpTimeState = 2;
    }
}