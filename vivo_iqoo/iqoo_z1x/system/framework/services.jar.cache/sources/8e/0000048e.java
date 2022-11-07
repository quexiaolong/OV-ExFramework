package com.android.server;

import android.os.IBinder;
import android.os.IInterface;
import java.util.ArrayList;

/* loaded from: classes.dex */
public interface IVivoProxy {
    public static final String DESCRIPTOR_IACCESSIBILITY = "IAccessibility";
    public static final String DESCRIPTOR_IAudioFocusDispatcher = "IAudioFocusDispatcher";
    public static final String DESCRIPTOR_IContentObserver = "IContentObserver";
    public static final String DESCRIPTOR_IDisplayManagerCallback = "IDisplayManagerCallback";
    public static final String DESCRIPTOR_IInputManager = "IInputManager";
    public static final String DESCRIPTOR_INetworkCallback = "INetworkCallback";
    public static final String DESCRIPTOR_IPhoneStateListener = "IPhoneStateListener";
    public static final String DESCRIPTOR_IWINDOW = "IWindow";

    void addBinderProxy(String str, IBinder iBinder, int i, int i2, boolean z);

    String getAidlDescriptor(IInterface iInterface);

    ArrayList<String> getWhiteList();

    void removeBinderProxy(IBinder iBinder);

    void setWhiteList(ArrayList<String> arrayList);

    default void addBinderProxy(String descriptor, IBinder bpBinder, int pid, int uid) {
        addBinderProxy(descriptor, bpBinder, pid, uid, false);
    }

    default void addBinderProxy(String descriptor, IBinder bpBinder, int pid) {
        addBinderProxy(descriptor, bpBinder, pid, -1, false);
    }

    default void addBinderProxy(String descriptor, IBinder bpBinder, int pid, boolean autoRemove) {
        addBinderProxy(descriptor, bpBinder, pid, -1, autoRemove);
    }
}