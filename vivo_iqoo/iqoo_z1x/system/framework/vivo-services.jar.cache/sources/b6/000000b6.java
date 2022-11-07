package com.android.server.am;

import android.hardware.display.DisplayManagerGlobal;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.view.DisplayInfo;
import com.vivo.common.utils.VLog;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.proxy.transact.TransactProxyManager;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;
import java.util.List;

/* loaded from: classes.dex */
public final class FrozenNotifier {
    private static final int ON_FROZEN_STATE_CHANGED = 20010;
    private static final String SURFACEFLINGER_DESCRIPTOR = "android.ui.ISurfaceComposer";
    private static final String SURFACEFLINGER_NAME = "SurfaceFlinger";
    private static final String TAG = "FrozenNotifier";
    private static boolean sDisplayConfigInited = false;
    private static boolean sDisplayMultiMode = false;

    public static void freeze(String pkgName, int uid, int userId) {
        BroadcastProxyManager.proxyPackage(pkgName, userId, true, ProxyConfigs.CTRL_MODULE_FROZEN);
        TransactProxyManager.proxyPackage(pkgName, userId, true, ProxyConfigs.CTRL_MODULE_FROZEN);
        notifySF("pem_frozen", uid, true);
    }

    public static void unfreeze(String pkgName, int uid, int userId) {
        BroadcastProxyManager.proxyPackage(pkgName, userId, false, ProxyConfigs.CTRL_MODULE_FROZEN);
        TransactProxyManager.proxyPackage(pkgName, userId, false, ProxyConfigs.CTRL_MODULE_FROZEN);
        notifySF("pem_frozen", uid, false);
    }

    private static void notifySF(String reason, int uid, boolean frozen) {
        if (!isSupportedMultiModes()) {
            return;
        }
        List<ProcessInfo> pidList = AppManager.getInstance().getProcessInfoList(uid);
        if (pidList.isEmpty()) {
            return;
        }
        int[] pids = new int[pidList.size()];
        for (int i = 0; i < pidList.size(); i++) {
            pids[i] = pidList.get(i).mPid;
        }
        IBinder service = ServiceManager.checkService(SURFACEFLINGER_NAME);
        if (service == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        try {
            try {
                data.writeInterfaceToken(SURFACEFLINGER_DESCRIPTOR);
                data.writeString(reason);
                data.writeIntArray(pids);
                data.writeInt(frozen ? 1 : 0);
                service.transact(ON_FROZEN_STATE_CHANGED, data, null, 0);
            } catch (Exception e) {
                VLog.e(TAG, String.format("notifySF reason=%s uid=%d frozen=%s e=%s", reason, Integer.valueOf(uid), String.valueOf(frozen), e.getMessage()));
            }
        } finally {
            data.recycle();
        }
    }

    private static boolean isSupportedMultiModes() {
        if (!sDisplayConfigInited) {
            boolean z = false;
            DisplayInfo displayInfo = DisplayManagerGlobal.getInstance().getDisplayInfo(0);
            sDisplayConfigInited = true;
            if (displayInfo != null && displayInfo.supportedModes != null && displayInfo.supportedModes.length > 1) {
                z = true;
            }
            sDisplayMultiMode = z;
        }
        return sDisplayMultiMode;
    }
}