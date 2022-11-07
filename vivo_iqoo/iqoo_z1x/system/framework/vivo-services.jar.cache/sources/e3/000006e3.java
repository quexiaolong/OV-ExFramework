package com.vivo.services.rms;

import android.content.Context;
import android.hardware.graphics.common.V1_0.BufferUsage;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import com.android.internal.util.MemInfoReader;
import com.android.server.ServiceThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.IVivoAms;
import com.android.server.am.VivoAmsImpl;
import com.android.server.am.VivoProcessListImpl;
import com.vivo.common.utils.VLog;
import com.vivo.statistics.sdk.ArgPack;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class RMAms {
    private static final int MSG_KILL_PROCESS = 1;
    private static final int MSG_START_PROCESS = 3;
    private static final int MSG_STOP_PACKAGE = 2;
    private ActivityManagerService mAms = null;
    private VivoAmsImpl mVivoAms = null;
    private Context mContext = null;
    private ServiceThread mRmAmsThread = null;
    private RmHandler mRmHanlder = null;
    private long mTotalMemMb = 0;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static RMAms INSTANCE = new RMAms();

        private Instance() {
        }
    }

    public static RMAms getInstance() {
        return Instance.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class RmHandler extends Handler {
        public RmHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
        }
    }

    public void initialize(ActivityManagerService ams, IVivoAms vivoAms, Context context) {
        if (this.mAms == null && ams != null && context != null && vivoAms != null && (vivoAms instanceof VivoAmsImpl)) {
            this.mAms = ams;
            this.mVivoAms = (VivoAmsImpl) vivoAms;
            this.mContext = context;
            ServiceThread serviceThread = new ServiceThread("rms_ams", 10, true);
            this.mRmAmsThread = serviceThread;
            serviceThread.start();
            this.mRmHanlder = new RmHandler(this.mRmAmsThread.getLooper());
            MemInfoReader minfo = new MemInfoReader();
            minfo.readMemInfo();
            this.mTotalMemMb = minfo.getTotalSize() / BufferUsage.RENDERSCRIPT;
            RmsInjectorImpl.getInstance().initialize(context);
            return;
        }
        VLog.e("rms", "Failed to initialize rms server!");
    }

    public boolean killProcess(final int[] pids, final int[] curAdjs, final String reason, final boolean secure) {
        RmHandler rmHandler;
        if (this.mVivoAms != null && (rmHandler = this.mRmHanlder) != null) {
            rmHandler.post(new Runnable() { // from class: com.vivo.services.rms.RMAms.1
                @Override // java.lang.Runnable
                public void run() {
                    RMAms.this.mVivoAms.killProcessByRms(pids, curAdjs, reason, secure);
                }
            });
            return true;
        }
        return true;
    }

    public void stopPackage(final String pkg, final int userId, final String reason) {
        RmHandler rmHandler;
        if (this.mVivoAms != null && (rmHandler = this.mRmHanlder) != null) {
            rmHandler.post(new Runnable() { // from class: com.vivo.services.rms.RMAms.2
                @Override // java.lang.Runnable
                public void run() {
                    RMAms.this.mVivoAms.forceStopPackageByRms(pkg, userId, reason);
                }
            });
        }
    }

    public int startProcess(final int userId, final String pkg, final String proc, final String reason, final boolean keepQuiet, long delay) {
        RmHandler rmHandler;
        if (this.mVivoAms != null && (rmHandler = this.mRmHanlder) != null) {
            rmHandler.post(new Runnable() { // from class: com.vivo.services.rms.RMAms.3
                @Override // java.lang.Runnable
                public void run() {
                    RMAms.this.mVivoAms.startProcessByRMS(userId, pkg, proc, reason, keepQuiet);
                }
            });
            return 0;
        }
        return 0;
    }

    public void setRmsEnable(boolean enable) {
    }

    public void updateOomLevels(int[] minFrees, int[] oomAdjs) {
        if (minFrees == null || oomAdjs == null || minFrees.length != oomAdjs.length || minFrees.length <= 0) {
            VLog.e("rms", "failed to updateOomLevels! fress:" + minFrees + " adjs:" + oomAdjs);
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(((oomAdjs.length * 2) + 1) * 4);
        buf.putInt(0);
        for (int i = 0; i < oomAdjs.length; i++) {
            buf.putInt(minFrees[i]);
            buf.putInt(oomAdjs[i]);
        }
        boolean ret = this.mVivoAms.writeLmkdByRms(buf);
        if (!ret) {
            VLog.e("rms", "failed to updateOomLevels!");
        }
    }

    public void restoreOomLevels() {
        updateOomLevels(VivoProcessListImpl.OOM_MINFREE_OVERRIDE[VivoProcessListImpl.myMinFreeIndex(this.mTotalMemMb)], VivoProcessListImpl.OOM_ADJ_OVERRIDE);
    }

    public boolean isBroadcastRegistered(String pkgName, int userId, String action, int flags) {
        return this.mVivoAms.isBroadcastRegistered(pkgName, userId, action, flags);
    }

    public ArgPack exeAppCmd(int pid, int cmd, ArgPack argPack) throws RemoteException {
        return this.mVivoAms.exeAppCmd(pid, cmd, argPack);
    }

    public Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids, boolean cached) {
        if (pids == null || pids.length <= 0) {
            return null;
        }
        if (cached) {
            return this.mVivoAms.getProcessMemoryInfo(pids);
        }
        Debug.MemoryInfo[] infos = new Debug.MemoryInfo[pids.length];
        for (int i = pids.length - 1; i >= 0; i--) {
            infos[i] = new Debug.MemoryInfo();
            Debug.getMemoryInfo(pids[i], infos[i]);
        }
        return infos;
    }

    public final int getWakefulness() {
        return this.mVivoAms.getWakefulness();
    }
}