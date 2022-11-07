package com.vivo.face.internal.wrapper;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.internal.widget.LockPatternUtils;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public final class LockPatternUtilsWrapper {
    private static final int ALLOWING_FACE_CONVENIENCE = 4100;
    private static final int ALLOWING_FACE_STRONG = 4;
    private static final String FIELD_KEYGUARD_ENABLED = "face_unlock_keyguard_enabled";
    private static final String TAG = "LockPatternUtilsWrapper";
    private Context mContext;
    private boolean mIsFaceAllowed = true;
    private LockPatternUtils mLockPatternUtils;
    private int mStrongAuthReason;
    private StrongAuthRequiredChangedListener mStrongAuthRequiredChangedListener;
    private LockPatternUtils.StrongAuthTracker mStrongAuthTracker;
    private boolean mTrackerInitialized;
    private static final boolean IS_FACE_LEVEL_STRONG = SystemProperties.get("persist.system.vivo.face.unlock.strong", "1").equals("1");
    private static final boolean IS_FACE_CONVENIENCE_SCHEME_ONE = SystemProperties.get("persist.system.vivo.face.unlock.convenience.scheme", "0").equals("0");

    /* loaded from: classes.dex */
    public interface StrongAuthRequiredChangedListener {
        void onStrongAuthRequiredChanged(boolean z);
    }

    public LockPatternUtilsWrapper(Context context) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mStrongAuthTracker = new StrongAuthTracker(context);
    }

    public void setStrongAuthRequiredChangedListener(StrongAuthRequiredChangedListener listener) {
        this.mStrongAuthRequiredChangedListener = listener;
        if (listener != null) {
            VLog.i(TAG, "register StrongAuth tracker");
            this.mLockPatternUtils.registerStrongAuthTracker(this.mStrongAuthTracker);
            return;
        }
        VLog.i(TAG, "unregister StrongAuth tracker");
        this.mTrackerInitialized = false;
        this.mLockPatternUtils.unregisterStrongAuthTracker(this.mStrongAuthTracker);
    }

    public void requireStrongAuth() {
        this.mLockPatternUtils.requireStrongAuth(8, ActivityManager.getCurrentUser());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isFaceAllowed() {
        if (IS_FACE_LEVEL_STRONG) {
            return (getStrongAuthForUser() & (-5)) == 0;
        }
        int strongAuthReason = getStrongAuthForUser();
        if (strongAuthReason != 0) {
            int strongAuthReason2 = strongAuthReason & (~this.mStrongAuthReason);
            this.mIsFaceAllowed = (strongAuthReason2 & ALLOWING_FACE_CONVENIENCE) != 0;
            if (!IS_FACE_CONVENIENCE_SCHEME_ONE && strongAuthReason2 == 128 && isFaceUnlockEnabled()) {
                requireStrongAuth();
            }
        }
        this.mStrongAuthReason = getStrongAuthForUser();
        return this.mIsFaceAllowed;
    }

    private int getStrongAuthForUser() {
        if (this.mTrackerInitialized) {
            return this.mStrongAuthTracker.getStrongAuthForUser(ActivityManager.getCurrentUser());
        }
        return this.mLockPatternUtils.getStrongAuthForUser(ActivityManager.getCurrentUser());
    }

    public boolean isStrongAuthReboot() {
        return (getStrongAuthForUser() & 1) != 0;
    }

    public boolean isStrongAuthTimeOut() {
        return (getStrongAuthForUser() & 16) != 0;
    }

    public boolean isKeyguardSecure() {
        return this.mLockPatternUtils.isSecure(ActivityManager.getCurrentUser());
    }

    public boolean isFaceUnlockEnabled() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "face_unlock_keyguard_enabled", 0, ActivityManager.getCurrentUser()) == 1;
    }

    public boolean isUnlockByStrongAuth() {
        return (getStrongAuthForUser() & 4096) != 0;
    }

    /* loaded from: classes.dex */
    private class StrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        public StrongAuthTracker(Context context) {
            super(context);
        }

        public void onStrongAuthRequiredChanged(int userId) {
            VLog.i(LockPatternUtilsWrapper.TAG, "onStrongAuthRequiredChanged userId: " + userId);
            if (!LockPatternUtilsWrapper.this.mTrackerInitialized) {
                LockPatternUtilsWrapper.this.mTrackerInitialized = true;
            }
            if (userId == ActivityManager.getCurrentUser()) {
                if (LockPatternUtilsWrapper.this.mStrongAuthRequiredChangedListener != null) {
                    boolean faceAllowed = LockPatternUtilsWrapper.this.isFaceAllowed();
                    VLog.i(LockPatternUtilsWrapper.TAG, "face allowed: " + faceAllowed);
                    LockPatternUtilsWrapper.this.mStrongAuthRequiredChangedListener.onStrongAuthRequiredChanged(faceAllowed);
                    return;
                }
                return;
            }
            VLog.i(LockPatternUtilsWrapper.TAG, "Current UserId: " + ActivityManager.getCurrentUser() + " dont match UserId: " + userId);
        }
    }
}