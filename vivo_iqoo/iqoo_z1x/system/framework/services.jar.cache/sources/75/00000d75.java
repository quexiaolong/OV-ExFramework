package com.android.server.display;

import android.os.IBinder;

/* loaded from: classes.dex */
public interface IVivoVirtualDisplayAdapter {
    IBinder createSpecialVirtualDisplay(String str, boolean z);

    void dummy();

    boolean hasVirtualDisplayLocked(String str);

    boolean ifSpecialVirtualDisplay(String str, String str2);

    /* loaded from: classes.dex */
    public interface IVivoVirtualDisplayAdapterExport {
        IVivoVirtualDisplayAdapter getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }

        default boolean hasVirtualDisplayLocked(String packageName) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().hasVirtualDisplayLocked(packageName);
            }
            return false;
        }
    }
}