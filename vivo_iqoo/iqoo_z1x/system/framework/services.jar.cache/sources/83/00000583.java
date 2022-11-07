package com.android.server;

/* loaded from: classes.dex */
public class VivoActivityStateNotify {
    public static final int ACTIVITY_STATE_AUDIORECORD_START = 4;
    public static final int ACTIVITY_STATE_AUDIORECORD_STOP = 5;
    public static final int ACTIVITY_STATE_DESTORY = 3;
    public static final int ACTIVITY_STATE_MEDIARECORD_START = 6;
    public static final int ACTIVITY_STATE_MEDIARECORD_STOP = 7;
    public static final int ACTIVITY_STATE_NONE = 0;
    public static final int ACTIVITY_STATE_RESUME = 1;
    public static final int ACVTIVITY_STATE_STOP = 2;
    private static VivoActivityStateNotify mActivityDestoryInst = null;
    private static Object mLock = new Object();
    private VivoActivityStateEvent mActivityStateNotify;

    /* loaded from: classes.dex */
    public interface VivoActivityStateEvent {
        void onActivityStateEvent(String str, String str2, int i);
    }

    private VivoActivityStateNotify() {
    }

    public static VivoActivityStateNotify getInstance() {
        synchronized (mLock) {
            if (mActivityDestoryInst == null) {
                mActivityDestoryInst = new VivoActivityStateNotify();
            }
        }
        return mActivityDestoryInst;
    }

    public void setActivityStateListener(VivoActivityStateEvent listener) {
        this.mActivityStateNotify = listener;
    }

    public void notifyActivityDestory(String pkg, String shortname) {
        VivoActivityStateEvent vivoActivityStateEvent = this.mActivityStateNotify;
        if (vivoActivityStateEvent == null) {
            return;
        }
        vivoActivityStateEvent.onActivityStateEvent(pkg, shortname, 3);
    }

    public void notifyActivityResume(String pkg, String shortname) {
        VivoActivityStateEvent vivoActivityStateEvent = this.mActivityStateNotify;
        if (vivoActivityStateEvent == null) {
            return;
        }
        vivoActivityStateEvent.onActivityStateEvent(pkg, shortname, 1);
    }

    public void notifyActivityStopped(String pkg, String shortname) {
        if (this.mActivityStateNotify == null) {
        }
    }

    public String getActivityState(int state) {
        if (state != 1) {
            if (state != 2) {
                if (state == 3) {
                    return "Destory";
                }
                return "none";
            }
            return "Stop";
        }
        return "Resume";
    }
}