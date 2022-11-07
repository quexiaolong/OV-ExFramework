package com.android.server.wm;

/* loaded from: classes2.dex */
public interface IVivoWindowAnimator {
    void dummy();

    Object getMultiWindowTransitionLocked(int i);

    void removeMultiWindowTransitionLocked(int i);

    void setMultiWindowTransitionLocked(int i, Object obj);

    /* loaded from: classes2.dex */
    public interface IVivoWindowAnimatorExport {
        IVivoWindowAnimator getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }

        default Object getMultiWindowTransitionLocked(int displayId) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getMultiWindowTransitionLocked(displayId);
            }
            return null;
        }

        default void setMultiWindowTransitionLocked(int displayId, Object multitrans) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setMultiWindowTransitionLocked(displayId, multitrans);
            }
        }
    }
}