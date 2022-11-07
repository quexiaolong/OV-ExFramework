package com.vivo.services.rms.sp;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.vivo.services.rms.sp.sdk.SpServerStub;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SpServer extends SpServerStub {
    public static final String TAG = "SpManager";
    private Context mContext;

    public void initialize(Context context) {
        this.mContext = context;
        try {
            ServiceManager.addService(SpServerStub.SERVICE_NAME, this);
        } catch (Exception e) {
            VSlog.e("SpManager", "SpServer addService fail", e);
        }
    }

    public static SpServer getInstance() {
        return Instance.INSTANCE;
    }

    private SpServer() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static SpServer INSTANCE = new SpServer();

        private Instance() {
        }
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args.length >= 1) {
            if ("--native".equals(args[0])) {
                String[] args2 = new String[args.length - 1];
                for (int i = 1; i < args.length; i++) {
                    args2[i - 1] = args[i];
                }
                SpManagerImpl.getInstance().dump(pw, args2);
                return;
            }
            SpClientNotifier.getInstance().dump(fd, pw, args);
        }
    }

    @Override // com.vivo.services.rms.sp.sdk.ISpServer
    public boolean setBundle(String name, Bundle bundle) throws RemoteException {
        if (Binder.getCallingUid() != SpManagerImpl.getInstance().getSuperSystemProcessUid()) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return true;
    }

    @Override // com.vivo.services.rms.sp.sdk.ISpServer
    public Bundle getBundle(String name) throws RemoteException {
        if (Binder.getCallingUid() != SpManagerImpl.getInstance().getSuperSystemProcessUid()) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return new Bundle();
    }

    @Override // com.vivo.services.rms.sp.sdk.ISpServer
    public void reportErrorPackage(String pkgName, int uid, long versionCode, int flag) {
        if (Binder.getCallingUid() != SpManagerImpl.getInstance().getSuperSystemProcessUid()) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        SpManagerImpl.getInstance().reportErrorPackage(pkgName, uid, versionCode, flag);
    }
}