package com.android.server.policy.keyguard;

import android.app.ActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.widget.LockPatternUtils;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
public class KeyguardStateMonitor extends IKeyguardStateCallback.Stub {
    private static final String TAG = "KeyguardStateMonitor";
    private final StateCallback mCallback;
    private int mCurrentUserId;
    private volatile boolean mHasLockscreenWallpaper;
    private volatile boolean mInputRestricted;
    private volatile boolean mIsShowing;
    private final LockPatternUtils mLockPatternUtils;
    private volatile boolean mSimSecure;
    private StateMonitorCallback mStateMonitorCallback;
    private volatile boolean mTrusted;

    /* loaded from: classes2.dex */
    public interface StateCallback {
        void onShowingChanged();

        void onTrustedChanged();
    }

    /* loaded from: classes2.dex */
    public interface StateMonitorCallback {
        void onKeyguardMsgChanged(String str, String str2, String str3);

        void onShowingChanged(boolean z);

        void onUnlockReason(int i);
    }

    public KeyguardStateMonitor(Context context, IKeyguardService service, StateCallback callback) {
        this(context, service, callback, null);
    }

    public KeyguardStateMonitor(Context context, IKeyguardService service, StateCallback callback, StateMonitorCallback fingerprintCallback) {
        this.mIsShowing = true;
        this.mSimSecure = true;
        this.mInputRestricted = true;
        this.mTrusted = false;
        this.mHasLockscreenWallpaper = false;
        this.mStateMonitorCallback = fingerprintCallback;
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mCallback = callback;
        try {
            service.addStateMonitorCallback(this);
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote Exception", e);
        }
    }

    public boolean isShowing() {
        return this.mIsShowing;
    }

    public boolean isSecure(int userId) {
        return this.mLockPatternUtils.isSecure(userId) || this.mSimSecure;
    }

    public boolean isInputRestricted() {
        return this.mInputRestricted;
    }

    public boolean isTrusted() {
        return this.mTrusted;
    }

    public boolean hasLockscreenWallpaper() {
        return this.mHasLockscreenWallpaper;
    }

    public void onShowingStateChanged(boolean showing) {
        Slog.w(TAG, " onShowingStateChanged showing = " + showing);
        this.mIsShowing = showing;
        this.mCallback.onShowingChanged();
        StateMonitorCallback stateMonitorCallback = this.mStateMonitorCallback;
        if (stateMonitorCallback != null) {
            stateMonitorCallback.onShowingChanged(showing);
        }
    }

    public void onSimSecureStateChanged(boolean simSecure) {
        this.mSimSecure = simSecure;
    }

    public synchronized void setCurrentUser(int userId) {
        this.mCurrentUserId = userId;
    }

    public void onInputRestrictedStateChanged(boolean inputRestricted) {
        this.mInputRestricted = inputRestricted;
    }

    public void onTrustedChanged(boolean trusted) {
        this.mTrusted = trusted;
        this.mCallback.onTrustedChanged();
    }

    public void onHasLockscreenWallpaperChanged(boolean hasLockscreenWallpaper) {
        this.mHasLockscreenWallpaper = hasLockscreenWallpaper;
    }

    public void onKeyguardMsgChanged(String msgType, String msg, String extra) {
        StateMonitorCallback stateMonitorCallback = this.mStateMonitorCallback;
        if (stateMonitorCallback != null) {
            stateMonitorCallback.onKeyguardMsgChanged(msgType, msg, extra);
        }
    }

    public void unlockReason(int reason) {
        StateMonitorCallback stateMonitorCallback = this.mStateMonitorCallback;
        if (stateMonitorCallback != null) {
            stateMonitorCallback.onUnlockReason(reason);
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "mIsShowing=" + this.mIsShowing);
        pw.println(prefix2 + "mSimSecure=" + this.mSimSecure);
        pw.println(prefix2 + "mInputRestricted=" + this.mInputRestricted);
        pw.println(prefix2 + "mTrusted=" + this.mTrusted);
        pw.println(prefix2 + "mCurrentUserId=" + this.mCurrentUserId);
    }
}