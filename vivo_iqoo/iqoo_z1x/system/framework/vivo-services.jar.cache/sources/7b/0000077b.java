package com.vivo.services.rms.sp.sdk;

import android.os.Bundle;
import android.os.IInterface;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface ISpServer extends IInterface {
    public static final String DESCRIPTOR = "com.vivo.sp.sdk.ISpServer";
    public static final int TRANSACTION_getBundle = 2;
    public static final int TRANSACTION_reportErrorPackage = 3;
    public static final int TRANSACTION_setBundle = 1;

    Bundle getBundle(String str) throws RemoteException;

    void reportErrorPackage(String str, int i, long j, int i2) throws RemoteException;

    boolean setBundle(String str, Bundle bundle) throws RemoteException;
}