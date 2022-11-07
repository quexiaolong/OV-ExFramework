package com.vivo.services.rms;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.services.rms.appmng.namelist.WidgetList;
import com.vivo.services.rms.sdk.IEventCallback;
import com.vivo.services.rms.sdk.IEventCallbackNative;
import com.vivo.services.rms.sdk.args.Args;
import com.vivo.services.rms.sdk.args.ArgsFactory;
import com.vivo.services.rms.sp.SpManagerImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class EventNotifier implements ServiceConnection {
    private static final int MAX_ACC_EVENT_COUNT = 1024;
    private static final int MAX_TRY_REBIND_COUNT = 40;
    private static final int MSG_BINDSERVICE = 1;
    private static final int MSG_EVENT = 0;
    private static final String PACKAGE_NAME_SPS = "com.vivo.sps";
    private static final int PROCESS_EVENT = 0;
    private static final String PROCESS_NAME_ABE = "com.vivo.rms";
    private static final String PROCESS_NAME_SPS = "com.vivo.sps:rms";
    private static final String SERVICE_NAME = "com.vivo.rms.dispatcher.EventReceiverService";
    private static final int SYSTEM_EVENT = 1;
    public static final String TAG = "rms";
    private int mConnectedTimes;
    private Context mContext;
    private String mDeathReason;
    private Handler mEventHanler;
    private IEventCallback mNotifier;
    private boolean mSuicide;
    private int mTid;
    private int mTryRebindTimes;
    private static final boolean FORCE_TO_RMS = SystemProperties.getBoolean("persist.debug.sps.use_rms", false);
    private static final String PACKAGE_NAME_ABE = "com.vivo.abe";
    public static String sUsingPackageName = PACKAGE_NAME_ABE;
    public static String sUsingProcessName = "com.vivo.rms";
    private AtomicInteger mAccEventCount = new AtomicInteger(0);
    private volatile int mPid = -1;

    public EventNotifier(Context context, Looper looper) {
        this.mContext = context;
        this.mEventHanler = new EventHandler(looper);
        VSlog.i("rms", "Set sys.sps.version as default.");
        SystemProperties.set("sys.sps.version", String.valueOf(SpManagerImpl.SUPER_PROCESS_FRAMEWORK_VERSION));
    }

    public void startRms(boolean isSpsExist) {
        boolean usingSps = !FORCE_TO_RMS && isSpsExist;
        sUsingPackageName = usingSps ? "com.vivo.sps" : PACKAGE_NAME_ABE;
        sUsingProcessName = usingSps ? PROCESS_NAME_SPS : "com.vivo.rms";
        if (!usingSps) {
            VLog.i("rms", "Clear sys.sps.version for some reasons.");
            SystemProperties.set("sys.sps.version", "0");
        }
        bindServiceIfNeeded(0L);
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName name, IBinder listener) {
        if (!isServiceConnected()) {
            IEventCallback l = IEventCallbackNative.asInterface(listener);
            try {
                synchronized (AppManager.getInstance()) {
                    Bundle data = new Bundle();
                    AppManager.getInstance().doInitLocked(data);
                    WidgetList.fillBundle(data);
                    fillSystemEvent(data);
                    fillDeathReason(data);
                    l.doInit(data);
                    this.mEventHanler.removeMessages(0);
                    this.mEventHanler.removeMessages(1);
                    this.mNotifier = l;
                    this.mPid = l.myPid();
                    this.mConnectedTimes++;
                    this.mSuicide = false;
                }
                this.mTryRebindTimes = 0;
                VLog.i("rms", String.format("EventReceiverService is connected pid=%d, times=%d", Integer.valueOf(this.mPid), Integer.valueOf(this.mConnectedTimes)));
            } catch (Exception e) {
                VLog.e("rms", "onServiceConnected exeption : " + e.getMessage());
            }
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName name) {
        if (isServiceConnected()) {
            this.mPid = -1;
            this.mNotifier = null;
            this.mAccEventCount.getAndSet(0);
            this.mEventHanler.removeMessages(0);
            this.mEventHanler.removeMessages(1);
            Config.setRmsEnable(false);
            bindServiceIfNeeded();
            VLog.d("rms", "EventReceiverService is disconnected");
        }
    }

    private void fillSystemEvent(Bundle data) {
        if (RmsInjectorImpl.self() != null && RmsInjectorImpl.self().isMonkey()) {
            data.putInt(EventDispatcher.NAME_MONKEY_STATE, 1);
        }
    }

    private void fillDeathReason(Bundle data) {
        data.putBoolean("fromReboot", this.mConnectedTimes == 0);
        data.putString("deathReason", this.mSuicide ? "suicide" : this.mDeathReason);
    }

    public void setDeathReason(String reason) {
        this.mDeathReason = reason;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindServiceIfNeeded() {
        bindServiceIfNeeded(1000L);
    }

    private void bindServiceIfNeeded(long delay) {
        if (isServiceConnected() || this.mEventHanler.hasMessages(1)) {
            return;
        }
        int i = this.mTryRebindTimes + 1;
        this.mTryRebindTimes = i;
        if (i < 40) {
            Handler handler = this.mEventHanler;
            handler.sendMessageDelayed(handler.obtainMessage(1), delay);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void realBindService() {
        synchronized (this) {
            if (!isServiceConnected()) {
                Intent intent = new Intent();
                intent.putExtra("version", "1.0");
                intent.putExtra("caller", this.mContext.getPackageName());
                intent.setComponent(new ComponentName(sUsingPackageName, SERVICE_NAME));
                try {
                    this.mContext.bindServiceAsUser(intent, this, 1, Process.myUserHandle());
                } catch (Exception e) {
                    VLog.e("rms", e.getMessage());
                }
            }
        }
    }

    public boolean isServiceConnected() {
        return this.mPid > 0;
    }

    public void postProcessEvent(int event, Args args) {
        if (isServiceConnected()) {
            this.mAccEventCount.addAndGet(1);
            this.mEventHanler.obtainMessage(0, 0, event, args).sendToTarget();
        }
    }

    public void postSystemEvent(int event, Args args) {
        if (isServiceConnected()) {
            this.mAccEventCount.addAndGet(1);
            this.mEventHanler.obtainMessage(0, 1, event, args).sendToTarget();
        }
    }

    public void postDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        try {
            if (this.mNotifier != null) {
                this.mNotifier.dumpData(fd, args);
            }
        } catch (Exception e) {
            pw.println("dumpData exeption : " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleEvent(int type, int event, Args args) {
        int accNum;
        StringBuilder sb;
        String format;
        if (args == null || this.mNotifier == null) {
            return;
        }
        if (this.mTid == 0) {
            int myTid = Process.myTid();
            this.mTid = myTid;
            Process.setThreadPriority(myTid, -8);
        }
        try {
            try {
                if (type == 0) {
                    this.mNotifier.onProcessEvent(event, args);
                } else if (type == 1) {
                    this.mNotifier.onSystemEvent(event, args);
                }
                ArgsFactory.recycle(args);
                accNum = this.mAccEventCount.addAndGet(-1);
            } catch (Exception e) {
                VLog.e("rms", "handle event exception : " + e.getMessage());
                accNum = this.mAccEventCount.addAndGet(-1);
                if (accNum > 1024) {
                    this.mSuicide = true;
                    Process.killProcess(this.mPid);
                    format = String.format("Maybe hang! accumulative %d kill %d.", Integer.valueOf(accNum), Integer.valueOf(this.mPid));
                } else if (accNum > 512) {
                    sb = new StringBuilder();
                } else {
                    return;
                }
            }
            if (accNum > 1024) {
                this.mSuicide = true;
                Process.killProcess(this.mPid);
                format = String.format("Maybe hang! accumulative %d kill %d.", Integer.valueOf(accNum), Integer.valueOf(this.mPid));
                VLog.e("rms", format);
            } else if (accNum > 512) {
                sb = new StringBuilder();
                sb.append("System is slowly, accNum=");
                sb.append(accNum);
                VLog.e("rms", sb.toString());
            }
        } catch (Throwable th) {
            int accNum2 = this.mAccEventCount.addAndGet(-1);
            if (accNum2 > 1024) {
                this.mSuicide = true;
                Process.killProcess(this.mPid);
                VLog.e("rms", String.format("Maybe hang! accumulative %d kill %d.", Integer.valueOf(accNum2), Integer.valueOf(this.mPid)));
            } else if (accNum2 > 512) {
                VLog.e("rms", "System is slowly, accNum=" + accNum2);
            }
            throw th;
        }
    }

    /* loaded from: classes.dex */
    private class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                EventNotifier.this.handleEvent(msg.arg1, msg.arg2, (Args) msg.obj);
            } else if (i == 1) {
                EventNotifier.this.realBindService();
                EventNotifier.this.bindServiceIfNeeded();
            }
        }
    }
}