package com.android.server;

import android.content.Context;
import android.net.INetd;
import android.net.INetdUnsolicitedEventListener;
import android.net.util.NetdService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import com.android.server.power.ShutdownThread;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoNetworkManagementServiceImpl implements IVivoNetworkManagementService {
    private static final long MAX_TIMEOUT_GET_NETD_SERVICE = 50000;
    private static final String REBOOT_REASON_NETD_HANGED = "fatal: getNetd hang";
    static final String TAG = "VivoNmsImpl";
    private Context mContext;
    private NetworkManagementService mNms;

    public VivoNetworkManagementServiceImpl(NetworkManagementService nms, Context c) {
        this.mNms = nms;
        this.mContext = c;
    }

    public INetd connectNativeNetdService(INetdUnsolicitedEventListener listener) {
        INetd netdService = NetdService.get((long) MAX_TIMEOUT_GET_NETD_SERVICE);
        if (netdService == null) {
            Runnable runnable = new Runnable() { // from class: com.android.server.VivoNetworkManagementServiceImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (this) {
                        VSlog.e(VivoNetworkManagementServiceImpl.TAG, "get netservice hang in 50000, try reboot device");
                        ShutdownThread.rebootOrShutdown((Context) null, true, VivoNetworkManagementServiceImpl.REBOOT_REASON_NETD_HANGED);
                    }
                }
            };
            Message msg = Message.obtain(UiThread.getHandler(), runnable);
            msg.setAsynchronous(true);
            UiThread.getHandler().sendMessage(msg);
        } else {
            try {
                netdService.registerUnsolicitedEventListener(listener);
                VSlog.d(TAG, "Register unsolicited event listener");
            } catch (RemoteException | ServiceSpecificException e) {
                VSlog.e(TAG, "Failed to set Netd unsolicited event listener " + e);
            }
        }
        return netdService;
    }

    public void dummy() {
    }
}