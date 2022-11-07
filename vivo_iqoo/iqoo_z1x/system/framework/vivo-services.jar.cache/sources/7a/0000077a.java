package com.vivo.services.rms.sp.sdk;

import android.os.Bundle;
import android.os.IInterface;
import android.os.RemoteException;
import java.io.FileDescriptor;

/* loaded from: classes.dex */
public interface ISpClient extends IInterface {
    public static final String DESCRIPTOR = "com.vivo.sp.sdk.ISpClient";
    public static final int TRANSACTION_doInit = 3;
    public static final int TRANSACTION_dumpData = 1;
    public static final int TRANSACTION_getBundle = 5;
    public static final int TRANSACTION_myPid = 2;
    public static final int TRANSACTION_notifyErrorPackage = 6;
    public static final int TRANSACTION_setBundle = 4;

    void doInit(Bundle bundle) throws RemoteException;

    void dumpData(FileDescriptor fileDescriptor, String[] strArr) throws RemoteException;

    Bundle getBundle(String str) throws RemoteException;

    int myPid() throws RemoteException;

    void notifyErrorPackage(String str, int i, long j, int i2) throws RemoteException;

    boolean setBundle(String str, Bundle bundle) throws RemoteException;
}