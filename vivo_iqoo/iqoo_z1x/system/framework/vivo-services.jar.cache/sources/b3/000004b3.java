package com.android.server.usb;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.hardware.usb.AccessoryFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.multidisplay.MultiDisplayManager;
import android.text.TextUtils;
import com.android.server.DisplayThread;
import com.android.server.VCarConfigManager;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUsbProfileGroupSettingsManagerImpl implements IVivoUsbProfileGroupSettingsManager {
    private static long sCarNetworkingVersion = 0;
    private String TAG = "VivoUsbProfileGroupSettingsManager";
    private boolean mCarNetworkingMonitorEnabled = true;
    private ArrayList<AccessoryFilter> mCarNetworkingDeviceFilters = null;
    private ArrayList<String> mAOAInterruptList = null;

    public VivoUsbProfileGroupSettingsManagerImpl() {
        getcarNetworkingVersion();
    }

    private void getcarNetworkingVersion() {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && sCarNetworkingVersion == 0) {
            DisplayThread.getHandler().post(new Runnable() { // from class: com.android.server.usb.VivoUsbProfileGroupSettingsManagerImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        PackageInfo packageInfo = AppGlobals.getPackageManager().getPackageInfo("com.vivo.car.networking", 0, 0);
                        long unused = VivoUsbProfileGroupSettingsManagerImpl.sCarNetworkingVersion = packageInfo != null ? packageInfo.getLongVersionCode() : 0L;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public boolean packageMatches(ArrayList<AccessoryFilter> accessoryFilters, String packageName) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && "com.vivo.car.networking".equals(packageName)) {
            this.mCarNetworkingDeviceFilters = accessoryFilters;
            return true;
        }
        return false;
    }

    public void monitorEnabled4CarNetworking(boolean enable) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return;
        }
        if (UsbManager.sDebugUsb) {
            VSlog.d("VivoAccessory", "profile monitorEnabled4CarNetworking enable = " + enable);
        }
        this.mCarNetworkingMonitorEnabled = enable;
    }

    public boolean accessoryMatches4CarNetworking(Context context, UsbAccessory accessory) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            VSlog.d("VivoAccessory", "carNetworkingMonitorEnabled enable = " + this.mCarNetworkingMonitorEnabled + " ,accessory: " + accessory);
            if (this.mCarNetworkingMonitorEnabled) {
                ArrayList<String> whitelist = VCarConfigManager.getInstance(context).get("AOA");
                if (whitelist != null && !whitelist.isEmpty()) {
                    this.mAOAInterruptList = whitelist;
                }
                boolean isEmpty = TextUtils.isEmpty(accessory.getManufacturer());
                String version = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                String manufaturer = isEmpty ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : accessory.getManufacturer();
                String model = TextUtils.isEmpty(accessory.getModel()) ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : accessory.getModel();
                if (!TextUtils.isEmpty(accessory.getVersion())) {
                    version = accessory.getVersion();
                }
                ArrayList<String> arrayList = this.mAOAInterruptList;
                if (arrayList != null && !arrayList.isEmpty()) {
                    String aoaString = manufaturer + "_" + model + "_" + version;
                    if (UsbManager.sDebugUsb) {
                        VSlog.d("VivoAccessory", "Aoa : " + aoaString);
                    }
                    Iterator<String> it = this.mAOAInterruptList.iterator();
                    while (it.hasNext()) {
                        String interruptString = it.next();
                        if (aoaString.contains(interruptString) && !TextUtils.isEmpty(interruptString)) {
                            return true;
                        }
                    }
                } else if (this.mCarNetworkingDeviceFilters != null) {
                    for (int i = 0; i < this.mCarNetworkingDeviceFilters.size(); i++) {
                        if (this.mCarNetworkingDeviceFilters.get(i).matches(accessory)) {
                            return true;
                        }
                    }
                } else if ("Baidu".equals(manufaturer) && "CarLife".equals(model) && sCarNetworkingVersion > 1200) {
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }
}