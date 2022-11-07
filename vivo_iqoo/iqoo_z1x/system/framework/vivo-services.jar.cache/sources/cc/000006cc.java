package com.vivo.services.proxy.transact;

import android.os.IBinder;

/* loaded from: classes.dex */
public final class TransactProxyFactory {
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static BpTransactProxy create(String descriptor, IBinder bpBinder, String pkg, String process, int uid, int pid) {
        char c;
        switch (descriptor.hashCode()) {
            case -1340625511:
                if (descriptor.equals("IWindow")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1134010650:
                if (descriptor.equals("IContentObserver")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1057698550:
                if (descriptor.equals("INetworkCallback")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 2117187993:
                if (descriptor.equals("IDisplayManagerCallback")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        if (c != 0) {
            if (c != 1) {
                if (c != 2) {
                    if (c == 3) {
                        return new IWindowProxy(descriptor, bpBinder, pkg, process, uid, pid);
                    }
                    return new BpTransactProxy(descriptor, bpBinder, pkg, process, uid, pid);
                }
                return new IMessengerProxy(descriptor, bpBinder, pkg, process, uid, pid);
            }
            return new IDisplayManagerCallbackProxy(descriptor, bpBinder, pkg, process, uid, pid);
        }
        return new IContentObserverProxy(descriptor, bpBinder, pkg, process, uid, pid);
    }
}