package com.android.server.pm;

import android.os.Process;
import android.os.SystemProperties;
import com.vivo.face.common.data.Constants;
import java.lang.Thread;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUserDataPreparerImpl implements IVivoUserDataPreparer {
    private static final String TAG = "VivoUserDataPreparerImpl";
    private static final String THREAD_NAME_SPEEDUP = "DexoptSpeedup";
    private static final int WARN_TIME_MS = 50;
    private Thread mRcvNotifyThread = null;
    private static final String PROPERTY_FEATURE_ENABLE = "persist.vivo.dex2oatopt.enable";
    private static boolean sIsEnable = SystemProperties.getBoolean(PROPERTY_FEATURE_ENABLE, true);
    private static final String[] PIDS_OF_INTREST = {"/system/bin/installd", "/apex/com.android.art/bin/dex2oat32", "/apex/com.android.art/bin/dex2oat64"};
    private static final int[] CMDLINE_OUT = {4096};
    private static VivoUserDataPreparerImpl sInstance = null;

    private VivoUserDataPreparerImpl() {
    }

    public static synchronized VivoUserDataPreparerImpl getInstance() {
        VivoUserDataPreparerImpl vivoUserDataPreparerImpl;
        synchronized (VivoUserDataPreparerImpl.class) {
            if (sInstance == null) {
                sInstance = new VivoUserDataPreparerImpl();
            }
            vivoUserDataPreparerImpl = sInstance;
        }
        return vivoUserDataPreparerImpl;
    }

    public void notifySpeedUp() {
        if (sIsEnable) {
            tryToStopDex2oat();
        }
    }

    public void notifySpeedUpNow() {
        long startTime = System.currentTimeMillis();
        if (sIsEnable) {
            Thread thread = this.mRcvNotifyThread;
            if (thread != null && thread.getState() != Thread.State.TERMINATED) {
                return;
            }
            tryToStopDex2oatInternal();
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 50) {
                VSlog.e(TAG, "notifySpeedUpNow took " + duration + " ms.");
            }
        }
    }

    private synchronized void tryToStopDex2oat() {
        if (this.mRcvNotifyThread == null || this.mRcvNotifyThread.getState() == Thread.State.TERMINATED) {
            Thread thread = new Thread(new Runnable() { // from class: com.android.server.pm.-$$Lambda$VivoUserDataPreparerImpl$IzakqVi2hA1SRbe0jQ8REReSu0E
                @Override // java.lang.Runnable
                public final void run() {
                    VivoUserDataPreparerImpl.this.lambda$tryToStopDex2oat$0$VivoUserDataPreparerImpl();
                }
            });
            this.mRcvNotifyThread = thread;
            thread.setPriority(10);
            this.mRcvNotifyThread.setName(THREAD_NAME_SPEEDUP);
            this.mRcvNotifyThread.start();
        }
    }

    public /* synthetic */ void lambda$tryToStopDex2oat$0$VivoUserDataPreparerImpl() {
        tryToStopDex2oatInternal();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
        }
        tryToStopDex2oatInternal();
    }

    private void tryToStopDex2oatInternal() {
        int[] pids = Process.getPidsForCommands(PIDS_OF_INTREST);
        if (pids == null) {
            VSlog.d(TAG, "can't get dex2oat pid!");
            return;
        }
        String[] cmdlines = new String[pids.length];
        int install_idx = -1;
        for (int i = 0; i < pids.length; i++) {
            cmdlines[i] = readCmdlineFromProcfs(pids[i]);
            if (cmdlines[i].equals(PIDS_OF_INTREST[0])) {
                install_idx = i;
            }
        }
        if (install_idx == -1) {
            return;
        }
        for (int i2 = 0; i2 < pids.length; i2++) {
            if (i2 != install_idx) {
                killIfIsDex2oat(pids[i2], cmdlines[i2], pids[install_idx]);
            }
        }
    }

    private void killIfIsDex2oat(int pid, String cmdline, int installPid) {
        if (pid == installPid) {
            return;
        }
        int ppid = Process.getParentPid(pid);
        if (ppid != installPid) {
            return;
        }
        VSlog.d(TAG, "kill dex2oat,pid " + pid + " cmdline is " + cmdline);
        Process.killProcess(pid);
    }

    private static String readCmdlineFromProcfs(int pid) {
        String[] cmdline = new String[1];
        if (!Process.readProcFile("/proc/" + pid + "/cmdline", CMDLINE_OUT, cmdline, null, null)) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        return cmdline[0];
    }
}