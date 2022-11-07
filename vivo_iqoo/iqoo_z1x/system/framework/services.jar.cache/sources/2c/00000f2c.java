package com.android.server.input;

/* loaded from: classes.dex */
public interface IVivoWindowManagerCallbacks {
    void notifyInputFrozenTimeout();

    void setInputFreezeReason(String str);

    boolean shouldDisablePilferPointers(String str);

    /* loaded from: classes.dex */
    public interface IVivoWindowManagerCallbacksExport {
        IVivoWindowManagerCallbacks getVivoInjectInstance();

        default boolean shouldDisablePilferPointers(String opPackageName) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().shouldDisablePilferPointers(opPackageName);
            }
            return false;
        }

        default void notifyInputFrozenTimeout() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().notifyInputFrozenTimeout();
            }
        }
    }
}