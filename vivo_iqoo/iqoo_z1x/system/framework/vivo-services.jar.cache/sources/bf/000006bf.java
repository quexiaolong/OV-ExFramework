package com.vivo.services.proxy;

import android.os.IBinder;
import android.os.IInterface;
import com.android.server.IVivoProxy;
import com.vivo.services.proxy.transact.TransactProxyManager;
import java.util.ArrayList;
import java.util.HashMap;

/* loaded from: classes.dex */
public class VivoProxyImpl implements IVivoProxy {
    private static final HashMap<String, String> AIDL_DESCRIPTOR_CACHE = new HashMap<>();

    /* loaded from: classes.dex */
    private static class Instance {
        private static final VivoProxyImpl INSTANCE = new VivoProxyImpl();

        private Instance() {
        }
    }

    public static VivoProxyImpl getInstance() {
        return Instance.INSTANCE;
    }

    public void addBinderProxy(String descriptor, IBinder bpBinder, int pid, int uid, boolean autoRemove) {
        TransactProxyManager.addBinderProxy(descriptor, bpBinder, pid, uid, autoRemove);
    }

    public void removeBinderProxy(IBinder bpBinder) {
        TransactProxyManager.removeBinderProxy(bpBinder);
    }

    public String getAidlDescriptor(IInterface i) {
        synchronized (AIDL_DESCRIPTOR_CACHE) {
            String className = i.getClass().getName();
            String descriptor = AIDL_DESCRIPTOR_CACHE.get(className);
            if (descriptor != null) {
                return descriptor;
            }
            int endIndex = className.indexOf(36);
            if (endIndex == -1) {
                endIndex = className.length();
            }
            String className2 = className.substring(0, endIndex);
            int startIndex = className2.lastIndexOf(46);
            if (startIndex == -1) {
                startIndex = 0;
            }
            String descriptor2 = className2.substring(startIndex + 1, endIndex);
            AIDL_DESCRIPTOR_CACHE.put(className2, descriptor2);
            return descriptor2;
        }
    }

    public void setWhiteList(ArrayList<String> list) {
        TransactProxyManager.setWhiteList(list);
    }

    public ArrayList<String> getWhiteList() {
        return TransactProxyManager.getWhiteList();
    }
}