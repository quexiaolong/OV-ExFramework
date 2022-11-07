package com.vivo.server.adapter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.android.server.display.WifiDisplayController;
import com.vivo.server.adapter.am.AbsAmsExtAdapter;
import com.vivo.server.adapter.display.AbsWifiDisplayControllerAdapter;
import com.vivo.server.adapter.hello.AbsHello;
import com.vivo.server.adapter.lcm.AbsVivoDisplayLcmControlImpl;
import com.vivo.server.adapter.location.AbsIZatDCControllerAdapter;
import com.vivo.server.adapter.power.AbsShutdownThreadAdapter;
import com.vivo.server.adapter.storage.AbsStorageManagerServiceAdapter;

/* loaded from: classes2.dex */
public abstract class ServiceAdapterFactory {
    private static final String TAG = "ServiceAdapterFactory";
    private static ServiceAdapterFactory factoryImpl;

    public abstract AbsAmsExtAdapter getAmsExtAdapter();

    public abstract AbsHello getHello();

    public abstract AbsIZatDCControllerAdapter getIZatDCControllerAdapter();

    public abstract AbsShutdownThreadAdapter getShutdownThreadAdapter();

    public abstract AbsStorageManagerServiceAdapter getStorageManagerServiceAdapter();

    public abstract AbsSystemServerAdapter getSystemServerAdapter();

    public abstract AbsVivoDisplayLcmControlImpl getVivoDisplayLcmControlImpl();

    public abstract AbsWifiDisplayControllerAdapter getWifiDisplayControllerAdapter(Context context, Handler handler, WifiDisplayController wifiDisplayController);

    public static ServiceAdapterFactory getServiceAdapterFactory() {
        synchronized (ServiceAdapterFactory.class) {
            if (factoryImpl != null) {
                return factoryImpl;
            }
            try {
                ServiceAdapterFactory serviceAdapterFactory = (ServiceAdapterFactory) Class.forName("com.vivo.server.adapter.ServiceAdapterFactoryImpl").newInstance();
                factoryImpl = serviceAdapterFactory;
                return serviceAdapterFactory;
            } catch (Exception e) {
                Log.d(TAG, "getServiceAdapterFactory get error!!!");
                factoryImpl = null;
                return null;
            }
        }
    }
}