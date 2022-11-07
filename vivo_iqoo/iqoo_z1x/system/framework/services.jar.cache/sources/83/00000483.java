package com.android.server;

import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.Context;
import android.os.Message;
import android.os.RemoteCallbackList;

/* loaded from: classes.dex */
public interface IVivoBluetoothManagerService {
    boolean checkCallingVivoPermission();

    boolean enableTwsApplication(Context context);

    void expandMsg(Message message);

    boolean isCustomSDKRestrict();

    boolean isThailandComercials();

    void iskeepFrozen(Object obj);

    void registerCallback(RemoteCallbackList<IBluetoothManagerCallback> remoteCallbackList, IBluetoothManagerCallback iBluetoothManagerCallback, Message message);

    void registerObserverForCBS(Context context);

    void registerObserverForGn(Context context);

    void registerStateCallback(RemoteCallbackList<IBluetoothStateChangeCallback> remoteCallbackList, IBluetoothStateChangeCallback iBluetoothStateChangeCallback, Message message);

    void setUserTurnOnOffBluetooth();
}