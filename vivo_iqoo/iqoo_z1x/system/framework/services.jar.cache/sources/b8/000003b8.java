package com.android.server;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Slog;
import com.android.server.am.HostingRecord;

/* loaded from: classes.dex */
public class ActivityTriggerService extends SystemService {
    public static final int PROC_ADDED_NOTIFICATION = 1;
    public static final int PROC_REMOVED_NOTIFICATION = 0;
    private static String TAG = "ActivityTriggerService";
    private EventHandlerThread eventHandler;

    static native void notifyAction_native(String str, long j, String str2, int i, int i2);

    public ActivityTriggerService(Context context) {
        super(context);
        this.eventHandler = new EventHandlerThread("EventHandlerThread");
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        Slog.i(TAG, "Starting ActivityTriggerService");
        this.eventHandler.start();
        publishLocalService(ActivityTriggerService.class, this);
    }

    public void updateRecord(HostingRecord hr, ApplicationInfo info, int pid, int event) {
        if (hr != null) {
            this.eventHandler.getHandler().post(new LocalRunnable(info.packageName, info.longVersionCode, info.processName, pid, event));
        }
    }

    /* loaded from: classes.dex */
    public class EventHandlerThread extends HandlerThread {
        private Handler handler;

        public EventHandlerThread(String name) {
            super(name);
        }

        @Override // android.os.HandlerThread
        protected void onLooperPrepared() {
            this.handler = new Handler();
        }

        public Handler getHandler() {
            return this.handler;
        }
    }

    /* loaded from: classes.dex */
    static class LocalRunnable implements Runnable {
        private int event;
        private long lvCode;
        private String packageName;
        private int pid;
        private String procName;

        LocalRunnable(String packageName, long lvCode, String procName, int pid, int event) {
            this.packageName = packageName;
            this.lvCode = lvCode;
            this.procName = procName;
            this.pid = pid;
            this.event = event;
        }

        @Override // java.lang.Runnable
        public void run() {
            ActivityTriggerService.notifyAction_native(this.packageName, this.lvCode, this.procName, this.pid, this.event);
        }
    }
}