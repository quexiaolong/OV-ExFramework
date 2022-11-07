package com.android.server.usb;

/* loaded from: classes2.dex */
public interface IVivoUsbHandlerLegacy {
    String applyAdbFunctionVivo(String str);

    void dummy();

    boolean initCurrentFunctions(String str, boolean z, long j);

    void setEnabledFunctions(long j, boolean z, boolean z2);

    void setToCharging();

    boolean stopSetCurrentFunctions(long j, boolean z, String str);

    void trySetEnabledFunctionsLog(long j, boolean z);

    String trySetEnabledFunctionsVivo(long j, boolean z);

    /* loaded from: classes2.dex */
    public interface IVivoUsbHandlerLegacyExport {
        IVivoUsbHandlerLegacy getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }
    }
}