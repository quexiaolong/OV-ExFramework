package com.android.server.usb;

import android.content.Context;
import android.hardware.usb.AccessoryFilter;
import android.hardware.usb.UsbAccessory;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public interface IVivoUsbProfileGroupSettingsManager {
    boolean accessoryMatches4CarNetworking(Context context, UsbAccessory usbAccessory);

    void monitorEnabled4CarNetworking(boolean z);

    boolean packageMatches(ArrayList<AccessoryFilter> arrayList, String str);

    /* loaded from: classes2.dex */
    public interface IVivoUsbProfileGroupSettingsManagerExport {
        IVivoUsbProfileGroupSettingsManager getVivoInjectInstance();

        default void monitorEnabled4CarNetworking(boolean enable) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().monitorEnabled4CarNetworking(enable);
            }
        }

        default boolean accessoryMatches4CarNetworking(Context context, UsbAccessory accessory) {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().accessoryMatches4CarNetworking(context, accessory);
            }
            return false;
        }
    }
}