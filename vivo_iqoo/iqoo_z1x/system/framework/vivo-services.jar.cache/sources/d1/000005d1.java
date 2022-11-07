package com.vivo.face.common.state;

import com.vivo.services.rms.ProcessList;

/* loaded from: classes.dex */
public final class KeyguardState {
    public static final int INTERACTIVE_STATE_FINISHED_SLEEP = 4;
    public static final int INTERACTIVE_STATE_FINISHED_WAKING = 2;
    public static final int INTERACTIVE_STATE_INIT = 0;
    public static final int INTERACTIVE_STATE_START_SLEEP = 3;
    public static final int INTERACTIVE_STATE_START_WAKING = 1;
    public static final int SCREEN_STATE_INIT = 0;
    public static final int SCREEN_STATE_TURNED_OFF = 2;
    public static final int SCREEN_STATE_TURNED_ON = 4;
    public static final int SCREEN_STATE_TURNING_OFF = 1;
    public static final int SCREEN_STATE_TURNING_ON = 3;
    public static final int WINDOW_HIDE = 1;
    public static final int WINDOW_HIDING = 3;
    public static final int WINDOW_NONE = 0;
    public static final int WINDOW_SHOW = 2;
    public boolean mBootCompleted;
    public int mCrashNum;
    public int mCurrentUser;
    public int mInteractiveState;
    public String mKeyguardMsg;
    public String mKeyguardMsgExtra;
    public String mKeyguardMsgType;
    public boolean mKeyguardOccluded;
    public boolean mKeyguardShown;
    public int mPrimaryDisplayBacklight;
    public int mPrimaryDisplayState;
    public int mScreenState;
    public int mSecondaryDisplayBacklight;
    public int mSecondaryDisplayState;
    public boolean mSystemReady;

    public KeyguardState() {
        reset();
    }

    private void reset() {
        this.mKeyguardShown = true;
        this.mCurrentUser = ProcessList.INVALID_ADJ;
        this.mScreenState = 0;
        this.mInteractiveState = 0;
        this.mPrimaryDisplayState = 0;
        this.mPrimaryDisplayBacklight = -1;
        this.mSecondaryDisplayState = 0;
        this.mSecondaryDisplayBacklight = -1;
    }

    private String toString(int state) {
        if (state != 0) {
            if (state != 1) {
                if (state != 2) {
                    if (state != 3) {
                        if (state != 4) {
                            return "unknown";
                        }
                        return "screen_turned_on";
                    }
                    return "screen_turning_on";
                }
                return "screen_turned_off";
            }
            return "screen_turning_off";
        }
        return "screen_init";
    }
}