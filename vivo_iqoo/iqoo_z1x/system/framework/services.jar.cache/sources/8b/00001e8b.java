package com.android.server.wm;

import android.app.IApplicationThread;
import android.content.res.Configuration;

/* loaded from: classes2.dex */
public interface IVivoWindowProcessController {
    boolean handleTransaction(IApplicationThread iApplicationThread, Configuration configuration, int i);

    boolean hasVisibleActivitiesIgnoringKeyguard();

    boolean isPackageUpdating();

    void setHasShowUi();

    void setPackageUpdate(boolean z);

    /* loaded from: classes2.dex */
    public interface IVivoWindowProcessControllerExport {
        IVivoWindowProcessController getVivoInjectInstance();

        default boolean hasVisibleActivitiesIgnoringKeyguard() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().hasVisibleActivitiesIgnoringKeyguard();
            }
            return false;
        }

        default boolean isPackageUpdating() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isPackageUpdating();
            }
            return false;
        }

        default void setPackageUpdate(boolean isUpdate) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setPackageUpdate(isUpdate);
            }
        }
    }
}