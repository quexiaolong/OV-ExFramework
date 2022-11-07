package com.vivo.services.rms.cgrp;

import android.app.IApplicationThread;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.ServiceThread;
import com.android.server.am.firewall.VivoFirewall;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import com.vivo.services.rms.Platform;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.vspa.VspaManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class BinderGroupController implements Handler.Callback {
    private static final String KEY_BINDER_BG_WORK_DELAYED_TIME = "key_binder_bg_work_delayed_time";
    private static final String KEY_BINDER_GROUP_ENABLED = "key_binder_group_enabled";
    private static final String KEY_BINDER_PKG_DEFAULT_GROUP_LIST = "key_binder_pkg_default_list";
    private static final String KEY_BINDER_PROC_DEFAULT_GROUP_LIST = "key_binder_proc_default_list";
    private static final String KEY_BINDER_PROC_FG_GROUP_LIST = "key_binder_proc_fg_list";
    private static final String KEY_BINDER_SET_MIN_THREADS = "key_binder_set_min_threads";
    private static final String KEY_BINDER_THREADS_ADJUSTABLE = "key_binder_threads_adjustable";
    private static final String KEY_BINDER_THREADS_ADJUST_SIZE = "key_binder_threads_adjust_size";
    public static final byte PARAM_BG_DELAYED_PARAM = 7;
    public static final byte PARAM_DEBUG_ALL = 2;
    public static final byte PARAM_DEBUG_BG_THREADS = 5;
    public static final byte PARAM_DEBUG_BG_WORKS = 4;
    public static final byte PARAM_DEBUG_SET_GROUP = 3;
    public static final byte PARAM_DUMP = 0;
    public static final byte PARAM_SET_ADJUST_SIZE = 6;
    public static final byte PARAM_SET_BG_THREADS = 1;
    public static final byte PARAM_SLOW_TRANSACTION_PARAM = 8;
    public static final byte PROC_GROUP_BG = 1;
    public static final byte PROC_GROUP_DEFAULT = 0;
    public static final byte PROC_GROUP_FG = 2;
    private static final String TAG = "BinderGroup";
    private int mBgWorkDelayedTime;
    private boolean mEnabled;
    private Handler mHandler;
    private int mMinThreads;
    private boolean mSupported;
    private int mThreadsAdjustSize;
    private boolean mThreadsAdjustable;
    private static final int MY_PID = Process.myPid();
    private static final ArrayList<String> RESTRICTED_SERVICE_LIST = new ArrayList<>();
    private static final ArrayList<String> PROC_FG_GROUP_LIST = new ArrayList<>();
    private static final ArrayList<String> PROC_DEFAULT_GROUP_LIST = new ArrayList<>();
    private static final ArrayList<String> PKG_DEFAULT_GROUP_LIST = new ArrayList<>();

    static {
        PROC_FG_GROUP_LIST.add("system");
        PROC_FG_GROUP_LIST.add("com.vivo.systemblur.server");
        PROC_FG_GROUP_LIST.add(FaceUIState.PKG_SYSTEMUI);
        PROC_FG_GROUP_LIST.add("com.vivo.upslide");
        PROC_FG_GROUP_LIST.add(FaceUIState.PKG_FACEUI);
        PROC_FG_GROUP_LIST.add("com.vivo.fingerprintui");
        PROC_FG_GROUP_LIST.add("com.android.phone");
        PROC_DEFAULT_GROUP_LIST.add("android.process.media");
        PROC_DEFAULT_GROUP_LIST.add("com.google.android.providers.media.module");
        PROC_DEFAULT_GROUP_LIST.add("com.android.providers.media.module");
        PROC_DEFAULT_GROUP_LIST.add("com.vivo.globalanimation");
        RESTRICTED_SERVICE_LIST.add(VivoFirewall.TYPE_ACTIVITY);
        RESTRICTED_SERVICE_LIST.add("activity_task");
        RESTRICTED_SERVICE_LIST.add("window");
        RESTRICTED_SERVICE_LIST.add("package");
    }

    /* loaded from: classes.dex */
    public static class Instance {
        private static final BinderGroupController INSTANCE = new BinderGroupController();

        private Instance() {
        }
    }

    private BinderGroupController() {
        boolean z = true;
        this.mMinThreads = SystemProperties.getInt("persist.sys.binder.min_threads", Platform.isMiddlePerfDevice() ? 1 : 2);
        this.mThreadsAdjustSize = SystemProperties.getInt("persist.sys.binder.threads_adjust_size", Platform.isMiddlePerfDevice() ? 10 : 20);
        this.mThreadsAdjustable = SystemProperties.getBoolean("persist.sys.binder.threads_adjustable", true);
        this.mBgWorkDelayedTime = 50;
        this.mSupported = false;
        this.mEnabled = (Platform.isOverSeas() || !Platform.isMiddlePerfDevice()) ? false : z;
        final ServiceThread thread = new ServiceThread(TAG, -4, false);
        thread.start();
        Handler handler = new Handler(thread.getLooper(), this);
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.vivo.services.rms.cgrp.-$$Lambda$BinderGroupController$24FX4kvuNdcyVj7mLhU1i0Y0aUI
            @Override // java.lang.Runnable
            public final void run() {
                BinderGroupController.lambda$new$0(thread);
            }
        });
    }

    public static /* synthetic */ void lambda$new$0(ServiceThread thread) {
        Process.setThreadGroupAndCpuset(thread.getThreadId(), 5);
    }

    public static BinderGroupController getInstance() {
        return Instance.INSTANCE;
    }

    public void onSystemReady() {
        if (!SystemProperties.getBoolean("persist.sys.binder.enable_group", true)) {
            return;
        }
        synchronized (this) {
            Slog.d(TAG, "onSystemReady");
            int err = VspaManager.binderSetParam(1, this.mMinThreads, this.mThreadsAdjustable ? 1 : 0);
            if (err != 0) {
                Slog.d(TAG, "init error=" + err);
                return;
            }
            this.mSupported = true;
            VspaManager.binderSetParam(6, this.mThreadsAdjustSize, 0);
            VspaManager.binderSetParam(7, this.mBgWorkDelayedTime, 1);
            this.mHandler.postDelayed(new Runnable() { // from class: com.vivo.services.rms.cgrp.BinderGroupController.1
                {
                    BinderGroupController.this = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    int err2;
                    Iterator it = BinderGroupController.RESTRICTED_SERVICE_LIST.iterator();
                    while (it.hasNext()) {
                        String name = (String) it.next();
                        IBinder service = ServiceManager.checkService(name);
                        if (service != null && (err2 = VspaManager.binderAddRestrictedNode(service, name)) != 0) {
                            Slog.e(BinderGroupController.TAG, "restrictService fail for name=" + name + " err=" + err2);
                        }
                    }
                    Slog.d(BinderGroupController.TAG, "Restrict Service Finished");
                }
            }, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
            updateProcess(false);
        }
    }

    public void setConfigs(Bundle data) {
        CgrpUtils.initList(PROC_FG_GROUP_LIST, data.getStringArrayList(KEY_BINDER_PROC_FG_GROUP_LIST));
        CgrpUtils.initList(PROC_DEFAULT_GROUP_LIST, data.getStringArrayList(KEY_BINDER_PROC_DEFAULT_GROUP_LIST));
        CgrpUtils.initList(PKG_DEFAULT_GROUP_LIST, data.getStringArrayList(KEY_BINDER_PKG_DEFAULT_GROUP_LIST));
        int minThreads = data.getInt(KEY_BINDER_SET_MIN_THREADS, this.mMinThreads);
        int adjustSize = data.getInt(KEY_BINDER_THREADS_ADJUST_SIZE, this.mThreadsAdjustSize);
        boolean adjustable = data.getBoolean(KEY_BINDER_THREADS_ADJUSTABLE, this.mThreadsAdjustable);
        int bgWorkDelayedTime = data.getInt(KEY_BINDER_BG_WORK_DELAYED_TIME, this.mBgWorkDelayedTime);
        if (this.mMinThreads != minThreads || this.mThreadsAdjustable != adjustable) {
            this.mMinThreads = minThreads;
            this.mThreadsAdjustable = adjustable;
            VspaManager.binderSetParam(1, minThreads, adjustable ? 1 : 0);
        }
        int i = this.mBgWorkDelayedTime;
        if (bgWorkDelayedTime != i) {
            VspaManager.binderSetParam(7, i, 1);
        }
        if (this.mThreadsAdjustSize != adjustSize) {
            this.mThreadsAdjustSize = adjustSize;
            VspaManager.binderSetParam(6, adjustSize, 0);
        }
        setEnable(data.getBoolean(KEY_BINDER_GROUP_ENABLED, this.mEnabled));
    }

    private void setEnable(final boolean enable) {
        if (enable != this.mEnabled) {
            this.mHandler.post(new Runnable() { // from class: com.vivo.services.rms.cgrp.BinderGroupController.2
                {
                    BinderGroupController.this = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    boolean z = BinderGroupController.this.mEnabled;
                    boolean z2 = enable;
                    if (z != z2) {
                        BinderGroupController.this.mEnabled = z2;
                        Slog.d(BinderGroupController.TAG, "setEnable " + enable + ",isSupported=" + BinderGroupController.this.mSupported);
                        if (BinderGroupController.this.mSupported) {
                            BinderGroupController.this.updateProcess(true);
                        }
                    }
                }
            });
        }
    }

    public void setProcessGroup(ProcessInfo pi, int group) {
        setProcessGroup(pi, group, false);
    }

    private int setSfGroup(int group) {
        IBinder service = ServiceManager.checkService("SurfaceFlinger");
        if (service == null) {
            return 0;
        }
        int err = VspaManager.binderSetProcGroup(service, group);
        return err;
    }

    private void setProcessGroup(ProcessInfo pi, int group, boolean forceUpdate) {
        synchronized (this) {
            if (this.mSupported) {
                if (this.mEnabled || forceUpdate) {
                    if (pi.mBinderGroup != group) {
                        pi.mBinderGroup = group;
                        this.mHandler.removeMessages(pi.mPid);
                        this.mHandler.obtainMessage(pi.mPid, group, 0, pi).sendToTarget();
                    }
                }
            }
        }
    }

    public int computeGroupLocked(ProcessInfo pi) {
        if (this.mEnabled && this.mSupported) {
            if (pi.mSchedGroup == 3 || pi.isFgActivity() || pi.isRunningRemoteAnimation() || pi.mPid == AppManager.getInstance().getHomeProcessPid() || PROC_FG_GROUP_LIST.contains(pi.mProcName)) {
                return 2;
            }
            if (pi.isVisible() || pi.mOwner.isTopApp() || PROC_DEFAULT_GROUP_LIST.contains(pi.mProcName) || PKG_DEFAULT_GROUP_LIST.contains(pi.mPkgName)) {
                return 0;
            }
            ProcessInfo root = pi.getRootOom();
            if (root != null) {
                return (root.mSchedGroup == 3 || root.mBinderGroup == 2) ? 0 : 1;
            }
            return 1;
        }
        return 0;
    }

    public void updateProcess(boolean forceupdate) {
        AppManager mng = AppManager.getInstance();
        synchronized (mng) {
            SparseArray<ProcessInfo> procs = mng.getProcsLocked();
            for (int i = 0; i < procs.size(); i++) {
                setProcessGroup(procs.valueAt(i), computeGroupLocked(procs.valueAt(i)), forceupdate);
            }
        }
        if (this.mSupported && this.mEnabled) {
            setSfGroup(2);
        } else {
            setSfGroup(0);
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        ProcessInfo pi = (ProcessInfo) msg.obj;
        int pid = msg.what;
        int group = msg.arg1;
        IApplicationThread thread = pi.mParent.thread;
        if (pi.mPid == pid && pi.isAlive() && thread != null && pi.mBinderSetGroup != group) {
            IBinder binder = thread.asBinder();
            if ((pid != MY_PID || (binder = ServiceManager.checkService(VivoFirewall.TYPE_ACTIVITY)) != null) && binder.isBinderAlive()) {
                int err = VspaManager.binderSetProcGroup(binder, group);
                if (err != 0) {
                    Slog.d(TAG, "binderSetProcGroup fail for name=" + pi.mProcName + " err=" + err);
                } else {
                    pi.mBinderSetGroup = group;
                }
            }
        }
        return true;
    }

    public void dump(PrintWriter pw, String[] args) {
        if (!CgrpUtils.isAllowDump()) {
            return;
        }
        if (args.length >= 1) {
            if ("--restrict-node".equals(args[0]) && args.length >= 2) {
                IBinder service = ServiceManager.checkService(args[1]);
                if (service != null) {
                    int result = VspaManager.binderAddRestrictedNode(service, args[1]);
                    pw.println(String.format("binderAddGroupNode name=%s result=%d", args[1], Integer.valueOf(result)));
                    return;
                }
                return;
            } else if ("--set-proc".equals(args[0]) && args.length >= 3) {
                int pid = Integer.parseInt(args[1]);
                int group = Integer.parseInt(args[2]);
                ProcessInfo pi = AppManager.getInstance().getProcessInfo(pid);
                if (pi == null) {
                    pw.println("pid not exist");
                    return;
                }
                IApplicationThread thread = pi.mParent.thread;
                if (thread != null) {
                    IBinder binder = thread.asBinder();
                    if (pid == MY_PID) {
                        binder = ServiceManager.checkService(VivoFirewall.TYPE_ACTIVITY);
                    }
                    int result2 = VspaManager.binderSetProcGroup(binder, group);
                    pw.println(String.format("binderSetProcGroup %s %d to group=%d result=%d", pi.mProcName, Integer.valueOf(pid), Integer.valueOf(group), Integer.valueOf(result2)));
                    return;
                }
                return;
            } else if ("--enable".equals(args[0]) && args.length >= 2) {
                boolean enable = Boolean.parseBoolean(args[1]);
                setEnable(enable);
                pw.println("setEnable " + enable);
                return;
            } else if ("--set-param".equals(args[0]) && args.length >= 2) {
                int code = Integer.parseInt(args[1]);
                int arg1 = args.length >= 3 ? Integer.parseInt(args[2]) : 0;
                int arg2 = args.length >= 4 ? Integer.parseInt(args[3]) : 0;
                int result3 = VspaManager.binderSetParam(code, arg1, arg2);
                pw.println(String.format("binderSetParam code=%d arg1=%d arg2=%d result=%d", Integer.valueOf(code), Integer.valueOf(arg1), Integer.valueOf(arg2), Integer.valueOf(result3)));
                return;
            }
        }
        pw.println("Supported=" + this.mSupported);
        pw.println("Enabled=" + this.mEnabled);
        pw.println("MinThreads=" + this.mMinThreads);
        pw.println("ThreadsAdjustable=" + this.mThreadsAdjustable);
        pw.println("ThreadsAdjustSize=" + this.mThreadsAdjustSize);
        CgrpUtils.dumpList(pw, RESTRICTED_SERVICE_LIST, "RESTRICTED_SERVICE_LIST");
        CgrpUtils.dumpList(pw, PROC_FG_GROUP_LIST, "PROC_FG_GROUP_LIST");
        CgrpUtils.dumpList(pw, PROC_DEFAULT_GROUP_LIST, "PROC_DEFAULT_GROUP_LIST");
        CgrpUtils.dumpList(pw, PKG_DEFAULT_GROUP_LIST, "PKG_DEFAULT_GROUP_LIST");
    }
}