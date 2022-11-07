package com.android.server.display;

import android.os.IBinder;
import android.view.SurfaceControl;
import com.android.server.display.VirtualDisplayAdapter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoVirtualDisplayAdapterImpl implements IVivoVirtualDisplayAdapter {
    static final String TAG = "VivoVirtualDisplayAdapterImpl";
    private VirtualDisplayAdapter mVirtualDisplayAdapter;

    public VivoVirtualDisplayAdapterImpl(VirtualDisplayAdapter vda) {
        if (vda == null) {
            VSlog.i(TAG, "container is " + vda);
        }
        this.mVirtualDisplayAdapter = vda;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public boolean hasVirtualDisplayLocked(String packageName) {
        VirtualDisplayAdapter virtualDisplayAdapter;
        if (packageName == null || packageName.isEmpty() || (virtualDisplayAdapter = this.mVirtualDisplayAdapter) == null || virtualDisplayAdapter.mVirtualDisplayDevices == null || this.mVirtualDisplayAdapter.mVirtualDisplayDevices.isEmpty()) {
            return false;
        }
        for (VirtualDisplayAdapter.VirtualDisplayDevice device : this.mVirtualDisplayAdapter.mVirtualDisplayDevices.values()) {
            if (packageName.equals(device.mOwnerPackageName)) {
                return true;
            }
        }
        return false;
    }

    public boolean ifSpecialVirtualDisplay(String ownerPackage, String displayName) {
        return ownerPackage != null && ownerPackage.contains("com.vivo.upnpserver") && displayName != null && isEquals(displayName);
    }

    private boolean isEquals(String displayName) {
        VSlog.i(TAG, "isEquals, displayName=" + displayName);
        boolean flag = false;
        VirtualDisplayAdapter virtualDisplayAdapter = this.mVirtualDisplayAdapter;
        if (virtualDisplayAdapter != null) {
            flag = virtualDisplayAdapter.isContainsSupportVirtualDisplayName(displayName);
        }
        VSlog.i(TAG, "isContainsDisplayName, flag=" + flag);
        return flag;
    }

    public IBinder createSpecialVirtualDisplay(String displayName, boolean secure) {
        return SurfaceControl.createDisplay(displayName, secure, 2);
    }
}