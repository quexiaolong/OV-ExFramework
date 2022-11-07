package com.android.server.usb;

/* loaded from: classes2.dex */
public interface IVivoUsbDeviceManager {
    public static final int MSG_CAR_NETWORKING_MONITOR_ENABLED = 100;
    public static final int MSG_CAR_NETWORKING_UPDATE_ACCESSORY = 101;

    void dummy();

    void monitorEnabled4CarNetworking(boolean z);

    boolean onUnlockUser(int i);

    void setCurrentFunctions(long j, Object obj);

    void updateAccessory4CarNetworking();

    /* loaded from: classes2.dex */
    public interface IVivoUsbDeviceManagerExport {
        IVivoUsbDeviceManager getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }

        default void updateAccessory4CarNetworking() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().updateAccessory4CarNetworking();
            }
        }

        default void monitorEnabled4CarNetworking(boolean enable) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().monitorEnabled4CarNetworking(enable);
            }
        }
    }
}