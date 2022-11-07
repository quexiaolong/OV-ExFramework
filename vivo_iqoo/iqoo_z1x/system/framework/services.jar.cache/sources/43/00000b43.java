package com.android.server.broadcastradio.hal2;

import android.os.RemoteException;

/* loaded from: classes.dex */
class Utils {
    private static final String TAG = "BcRadio2Srv.utils";

    /* loaded from: classes.dex */
    interface FuncThrowingRemoteException<T> {
        T exec() throws RemoteException;
    }

    /* loaded from: classes.dex */
    interface VoidFuncThrowingRemoteException {
        void exec() throws RemoteException;
    }

    Utils() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static FrequencyBand getBand(int freq) {
        return freq < 30 ? FrequencyBand.UNKNOWN : freq < 500 ? FrequencyBand.AM_LW : freq < 1705 ? FrequencyBand.AM_MW : freq < 30000 ? FrequencyBand.AM_SW : freq < 60000 ? FrequencyBand.UNKNOWN : freq < 110000 ? FrequencyBand.FM : FrequencyBand.UNKNOWN;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <T> T maybeRethrow(FuncThrowingRemoteException<T> r) {
        try {
            return r.exec();
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void maybeRethrow(VoidFuncThrowingRemoteException r) {
        try {
            r.exec();
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
    }
}