package com.vivo.services.aivirus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.am.firewall.VivoAppIsolationController;
import com.vivo.framework.aivirus.VivoBehaviorEngListener;
import com.vivo.framework.aivirus.VivoBehaviorEngManager;
import java.util.ArrayList;
import vivo.app.aivirus.IVivoBehaviorEngService;

/* loaded from: classes.dex */
public class VivoBehaviorEngService extends IVivoBehaviorEngService.Stub {
    private static final String ACTION_AI_VIRUS_DETECT = "android.intent.action.AI_VIRUS_DETECT";
    private static final int READY_TO_DETECT = 1;
    private static final String TAG = "VBEservice";
    private Context mContext;
    private HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private VivoBehaviorEngManager mVBEManager;
    private VivoBehaviorEngListener vivoBehaviorEngListener;
    public static VivoBehaviorEngService sInstance = null;
    public static String mTargetPackage = VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE;

    private VivoBehaviorEngService(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.vivoBehaviorEngListener = new VivoBehaviorEngListener() { // from class: com.vivo.services.aivirus.VivoBehaviorEngService.1
            public void onNewAction(String packageName, ArrayList<Integer> data) {
                VivoBehaviorEngService.this.readyToDetect(packageName, data);
            }
        };
    }

    public VivoBehaviorEngService() {
    }

    public static synchronized VivoBehaviorEngService getInstance(Context context) {
        VivoBehaviorEngService vivoBehaviorEngService;
        synchronized (VivoBehaviorEngService.class) {
            Slog.d(TAG, "VBEservice start");
            if (sInstance == null) {
                sInstance = new VivoBehaviorEngService(context);
            }
            vivoBehaviorEngService = sInstance;
        }
        return vivoBehaviorEngService;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1 && msg.obj != null) {
                Intent intent = new Intent();
                intent.setAction(VivoBehaviorEngService.ACTION_AI_VIRUS_DETECT);
                intent.putExtras((Bundle) msg.obj);
                intent.setPackage(VivoBehaviorEngService.mTargetPackage);
                Slog.d(VivoBehaviorEngService.TAG, "mTargetPackage:" + VivoBehaviorEngService.mTargetPackage);
                VivoBehaviorEngService.this.mContext.sendBroadcast(intent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readyToDetect(String packageName, ArrayList<Integer> data) {
        Bundle bundle = new Bundle();
        bundle.putString("pkgname", packageName);
        bundle.putIntegerArrayList("actionIDs", data);
        Message msg = new Message();
        msg.what = 1;
        msg.obj = bundle;
        ServiceHandler serviceHandler = this.mServiceHandler;
        if (serviceHandler != null) {
            serviceHandler.sendMessage(msg);
        }
    }

    public boolean initBVE(Bundle bundle) throws RemoteException {
        try {
            String invokerPkg = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (!isSystemApp(this.mContext, invokerPkg)) {
                return false;
            }
            VivoBehaviorEngManager vivoBehaviorEngManager = VivoBehaviorEngManager.getInstance(this);
            this.mVBEManager = vivoBehaviorEngManager;
            vivoBehaviorEngManager.initVBEManager(bundle);
            this.mServiceHandler = new ServiceHandler(this.mHandlerThread.getLooper());
            this.mVBEManager.setOnBVEListener(this.vivoBehaviorEngListener);
            return true;
        } catch (Exception e) {
            Slog.d(TAG, "initBVE Exception:" + e.toString());
            return false;
        }
    }

    public boolean disableVBE() throws RemoteException {
        try {
            String invokerPkg = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (isSystemApp(this.mContext, invokerPkg)) {
                if (this.mVBEManager == null) {
                    this.mVBEManager = VivoBehaviorEngManager.getInstance(this);
                }
                this.mVBEManager.disableVBEManager();
                Slog.d(TAG, "mVBEManager disabled");
                return true;
            }
            return false;
        } catch (Exception e) {
            Slog.d(TAG, "disableVBE Exception:" + e.toString());
            return false;
        }
    }

    public void addFilterPkg(String pkgname) throws RemoteException, SecurityException {
        try {
            String invokerPkg = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (!isSystemApp(this.mContext, invokerPkg)) {
                throw new SecurityException("the user id has not the VBE permission");
            }
            if (pkgname == null || this.mVBEManager == null) {
                return;
            }
            this.mVBEManager.addFilterPkg(pkgname);
        } catch (Exception e) {
            Slog.d(TAG, "addFilterPkg Exception:" + e.toString());
        }
    }

    public void delFilterPkg(String pkgname) throws RemoteException, SecurityException {
        try {
            String invokerPkg = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (!isSystemApp(this.mContext, invokerPkg)) {
                throw new SecurityException("the user id has not the VBE permission");
            }
            if (pkgname == null || this.mVBEManager == null) {
                return;
            }
            this.mVBEManager.delFilterPkg(pkgname);
        } catch (Exception e) {
            Slog.d(TAG, "delFilterPkg Exception:" + e.toString());
        }
    }

    private boolean isSystemApp(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);
            if ((appInfo.flags & 1) == 0 && (appInfo.flags & 128) == 0) {
                if (appInfo.uid != 1000) {
                    return false;
                }
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "isSystemApp e" + e);
            return false;
        }
    }

    public String getFilterPkg() throws RemoteException, SecurityException {
        String invokerPkg = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (!isSystemApp(this.mContext, invokerPkg)) {
            throw new SecurityException("the user id has not the VBE permission");
        }
        VivoBehaviorEngManager vivoBehaviorEngManager = this.mVBEManager;
        if (vivoBehaviorEngManager == null) {
            return null;
        }
        return vivoBehaviorEngManager.getFilterPkg();
    }

    public void notifyAction(String CallingApp, int uid, int actionId) throws RemoteException {
        VivoBehaviorEngManager vivoBehaviorEngManager = this.mVBEManager;
        if (vivoBehaviorEngManager != null) {
            vivoBehaviorEngManager.notifyAction(CallingApp, uid, actionId);
        }
    }
}