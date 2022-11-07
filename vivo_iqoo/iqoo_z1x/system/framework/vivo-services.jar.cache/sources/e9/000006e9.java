package com.vivo.services.rms;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import com.vivo.common.utils.VLog;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.services.rms.appmng.WorkingState;
import com.vivo.services.rms.cgrp.BinderGroupController;
import com.vivo.services.rms.cgrp.CgrpUtils;
import com.vivo.services.rms.display.RefreshRateAdjuster;
import com.vivo.services.rms.display.SfUtils;
import com.vivo.services.rms.sdk.RMNative;
import com.vivo.services.rms.sdk.args.Args;
import com.vivo.statistics.sdk.ArgPack;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class RMServer extends RMNative {
    public static final String TAG = "rms";
    private static RMServer sServer;
    private Context mContext;
    private EventNotifier mEventNotifier;
    private Looper mLooper;

    public static RMServer getInstance() {
        return sServer;
    }

    public static void publish(Context context) {
        if (sServer == null) {
            sServer = new RMServer(context);
        }
        try {
            ServiceManager.addService("rms", sServer);
        } catch (Exception e) {
            VLog.e("rms", "RMServer addService fail");
        }
    }

    private RMServer(Context context) {
        this.mContext = context;
        HandlerThread thread = new HandlerThread("rms");
        thread.start();
        this.mLooper = thread.getLooper();
        this.mEventNotifier = new EventNotifier(context, this.mLooper);
        SystemProperties.set("sys.rms.is_supported", "true");
    }

    public EventNotifier getEventNotifier() {
        return this.mEventNotifier;
    }

    public boolean isReady() {
        EventNotifier eventNotifier = this.mEventNotifier;
        if (eventNotifier != null) {
            return eventNotifier.isServiceConnected();
        }
        return false;
    }

    public Context getContext() {
        return this.mContext;
    }

    public void postProcessEvent(int event, Args args) {
        this.mEventNotifier.postProcessEvent(event, args);
    }

    public void postSystemEvent(int event, Args args) {
        this.mEventNotifier.postSystemEvent(event, args);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void setAppList(String typeName, ArrayList<String> appList) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void killProcess(int[] pids, int[] curAdjs, String reason, boolean secure) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        RMAms.getInstance().killProcess(pids, curAdjs, reason, secure);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void stopPackage(String pkg, int userId, String reason) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        RMAms.getInstance().stopPackage(pkg, userId, reason);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean writeSysFs(ArrayList<String> fileNames, ArrayList<String> values) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        } else if (!SysFsModifier.modify(fileNames, values)) {
            SysFsModifier.restore(fileNames);
            return false;
        } else {
            return true;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean restoreSysFs(ArrayList<String> fileNames) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        SysFsModifier.restore(fileNames);
        return true;
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public int getPss(int pid) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return (int) Debug.getPss(pid, null, null);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean setBundle(String name, Bundle bundle) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return Config.setBundle(name, bundle);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public Bundle getBundle(String name) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return Config.getBundle(name);
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args.length >= 1) {
            String[] args2 = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                args2[i - 1] = args[i];
            }
            if ("--native".equalsIgnoreCase(args[0])) {
                AppManager.getInstance().dump(pw);
                return;
            } else if ("--preloadn".equals(args[0])) {
                PreloadedAppRecordMgr.getInstance().dump(pw);
                return;
            } else if ("--proxy".equals(args[0])) {
                ProxyConfigs.dump(pw, args2);
                return;
            } else if ("--binder-group".equals(args[0])) {
                BinderGroupController.getInstance().dump(pw, args2);
                return;
            } else if ("--vspa-configs".equals(args[0])) {
                VspaConfigs.dump(pw, args2);
                return;
            } else if ("--cgrp-configs".equals(args[0])) {
                CgrpUtils.dump(pw, args2);
                return;
            } else if ("--display".equals(args[0])) {
                RefreshRateAdjuster.getInstance().dump(pw, args2);
                return;
            } else if (args.length >= 3 && "--frozen".equals(args[0])) {
                SfUtils.frozenUpdate("testFrozen", Integer.valueOf(args[1]).intValue(), Boolean.valueOf(args[2]).booleanValue());
                return;
            }
        }
        this.mEventNotifier.postDump(fd, pw, args);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean readProcFile(String file, int[] format, String[] outStrings, long[] outLongs, float[] outFloats) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        boolean ret = Process.readProcFile(file, format, outStrings, outLongs, outFloats);
        return ret;
    }

    /* JADX WARN: Code restructure failed: missing block: B:12:0x003a, code lost:
        if (r0 == null) goto L8;
     */
    @Override // com.vivo.services.rms.sdk.IRM
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean writePath(java.lang.String r9, java.lang.String r10) throws android.os.RemoteException {
        /*
            r8 = this;
            int r0 = android.os.Binder.getCallingUid()
            r1 = 1000(0x3e8, float:1.401E-42)
            if (r0 != r1) goto L44
            r0 = 0
            r1 = 0
            java.io.PrintWriter r2 = new java.io.PrintWriter     // Catch: java.lang.Throwable -> L1e java.lang.Exception -> L20
            java.io.FileOutputStream r3 = new java.io.FileOutputStream     // Catch: java.lang.Throwable -> L1e java.lang.Exception -> L20
            r3.<init>(r9)     // Catch: java.lang.Throwable -> L1e java.lang.Exception -> L20
            r2.<init>(r3)     // Catch: java.lang.Throwable -> L1e java.lang.Exception -> L20
            r0 = r2
            r0.write(r10)     // Catch: java.lang.Throwable -> L1e java.lang.Exception -> L20
            r1 = 1
        L1a:
            r0.close()
            goto L3d
        L1e:
            r2 = move-exception
            goto L3e
        L20:
            r2 = move-exception
            java.lang.String r3 = "rms"
            java.lang.String r4 = "writeFile fail %s %s"
            r5 = 2
            java.lang.Object[] r5 = new java.lang.Object[r5]     // Catch: java.lang.Throwable -> L1e
            r6 = 0
            r5[r6] = r9     // Catch: java.lang.Throwable -> L1e
            r6 = 1
            java.lang.String r7 = r2.toString()     // Catch: java.lang.Throwable -> L1e
            r5[r6] = r7     // Catch: java.lang.Throwable -> L1e
            java.lang.String r4 = java.lang.String.format(r4, r5)     // Catch: java.lang.Throwable -> L1e
            com.vivo.common.utils.VLog.e(r3, r4)     // Catch: java.lang.Throwable -> L1e
            if (r0 == 0) goto L3d
            goto L1a
        L3d:
            return r1
        L3e:
            if (r0 == 0) goto L43
            r0.close()
        L43:
            throw r2
        L44:
            java.lang.SecurityException r0 = new java.lang.SecurityException
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "Permission Denial callingUid="
            r1.append(r2)
            int r2 = android.os.Binder.getCallingUid()
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            r0.<init>(r1)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.rms.RMServer.writePath(java.lang.String, java.lang.String):boolean");
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public ArrayList<String> readPath(String path, int bufferSize) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        BufferedReader reader = null;
        ArrayList<String> result = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(path), bufferSize);
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    result.add(line);
                }
            } catch (Exception e) {
                VLog.d("rms", "readPath e = " + e.getMessage());
            }
            return result;
        } finally {
            closeQuietly(reader);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    public void startRms(boolean isSpsExist) {
        this.mEventNotifier.startRms(isSpsExist);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public int startProcess(int userId, String pkg, String proc, String reason, boolean keepQuiet, long delay) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        if (reason == null || reason.length() <= 0) {
            reason = "by " + Binder.getCallingPid();
        }
        return RMAms.getInstance().startProcess(userId, pkg, proc, reason, keepQuiet, delay);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean needKeepQuiet(int pid, int uid, int flag) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return PreloadedAppRecordMgr.getInstance().needKeepQuiet(pid, uid, flag);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean needKeepQuiet(String pkgName, int userId, int uid, int flag) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return PreloadedAppRecordMgr.getInstance().needKeepQuiet(pkgName, userId, uid, flag);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void setNeedKeepQuiet(int pid, int uid, boolean keepQuiet) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        PreloadedAppRecordMgr.getInstance().setNeedKeepQuiet(pid, uid, keepQuiet);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void setNeedKeepQuiet(String pkgName, int userId, int uid, boolean keepQuiet) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        PreloadedAppRecordMgr.getInstance().setNeedKeepQuiet(pkgName, userId, uid, keepQuiet);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void proxyApp(List<String> pkgList, int userId, int flags, boolean proxy, String module) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        ProxyConfigs.proxyApp(pkgList, userId, flags, proxy, module);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public ActivityManager.RunningAppProcessInfo getRunningAppProcesses(int pid) throws RemoteException {
        ProcessInfo app = AppManager.getInstance().getProcessInfo(pid);
        ActivityManager.RunningAppProcessInfo currApp = new ActivityManager.RunningAppProcessInfo();
        if (app != null && app.mPid != -1) {
            currApp.pid = app.mPid;
            currApp.processName = app.mProcName;
            currApp.pkgList = AppManager.getInstance().getPackageList(app);
            currApp.uid = app.mUid;
            currApp.flags = app.mPkgFlags;
        }
        return currApp;
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean isBroadcastRegistered(String pkgName, int userId, String action, int flags) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return RMAms.getInstance().isBroadcastRegistered(pkgName, userId, action, flags);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public ArgPack exeAppCmd(int pid, int cmd, ArgPack argPack) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        ArgPack result = AppCmdExecutor.exeAppCmd(pid, cmd, argPack);
        if (result == null) {
            return new ArgPack(new Object[]{false});
        }
        return result;
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids, boolean cached) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Permission Denial callingUid=" + Binder.getCallingUid());
        }
        return RMAms.getInstance().getProcessMemoryInfo(pids, cached);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void notifyWorkingState(String pkgName, int uid, int mask, boolean state) throws RemoteException {
        WorkingState.notifyWorkingState(pkgName, uid, mask, state);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void acquireRefreshRate(long handle, String sceneName, String reason, int fps, int priority, int duration, int caller, int client, int states, boolean dfps, int extra) throws RemoteException {
        RefreshRateAdjuster.getInstance().acquireRefreshRate(handle, sceneName, reason, fps, priority, duration, caller, client, states, dfps, extra);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void releaseRefreshRate(int caller, long handle) throws RemoteException {
        RefreshRateAdjuster.getInstance().releaseRefreshRate(caller, handle);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean createRefreshRateScene(String name, int priority, boolean powerFirst) throws RemoteException {
        return RefreshRateAdjuster.getInstance().createScene(name, priority, powerFirst);
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public float getActiveRefreshRate() throws RemoteException {
        return RefreshRateAdjuster.getInstance().getActiveRefreshRate();
    }
}