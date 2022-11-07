package com.android.server.power;

/* loaded from: classes2.dex */
public interface IVivoWakeLock {
    void dummy();

    int getDisplayId();

    boolean isShouldKeepScreenOn();

    void setShouldKeepScreenOn(boolean z);

    void toString(StringBuilder sb);

    /* loaded from: classes2.dex */
    public interface IVivoWakeLockExport {
        IVivoWakeLock getVivoInjectInstance();

        default void dummy() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }

        default void setShouldKeepScreenOn(boolean shouldKeepScreenOn) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setShouldKeepScreenOn(shouldKeepScreenOn);
            }
        }

        default boolean isShouldKeepScreenOn() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isShouldKeepScreenOn();
            }
            return false;
        }

        default int getDisplayId() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getDisplayId();
            }
            return 0;
        }
    }
}