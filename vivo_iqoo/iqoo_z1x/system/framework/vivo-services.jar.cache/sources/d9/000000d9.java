package com.android.server.am;

import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.os.WorkSource;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.IVivoFrozenInjector;
import com.android.server.IoThread;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoBatteryStatsServiceImpl implements IVivoBatteryStatsService {
    private static final int DUMP_TIMEOUT = 30000;
    private static final int MSG_DUMP_TIMEOUT = 12;
    private static final int MSG_NOTE_START_SENSOR = 1;
    private static final String TAG = "VivoBatteryStatsServiceImpl";
    private BatteryStatsService mBatteryStatsService;
    private DumpTimeoutHandler mDumpTimeoutHandler;

    public VivoBatteryStatsServiceImpl(BatteryStatsService batteryStats) {
        this.mBatteryStatsService = batteryStats;
        if (this.mDumpTimeoutHandler == null) {
            this.mDumpTimeoutHandler = new DumpTimeoutHandler(IoThread.getHandler().getLooper());
        }
    }

    public void sendDumpTimeoutMsg() {
        Message message = this.mDumpTimeoutHandler.obtainMessage(12);
        message.arg1 = Binder.getCallingPid();
        message.arg2 = Binder.getCallingUid();
        this.mDumpTimeoutHandler.sendMessageDelayed(message, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
    }

    public void removeDumpTimeoutMsg() {
        this.mDumpTimeoutHandler.removeMessages(12);
    }

    public void noteStartSensor(int uid, int sensor) {
        Message msg = this.mDumpTimeoutHandler.obtainMessage(1);
        msg.arg1 = uid;
        msg.arg2 = sensor;
        this.mDumpTimeoutHandler.sendMessage(msg);
    }

    /* loaded from: classes.dex */
    private class DumpTimeoutHandler extends Handler {
        DumpTimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                VivoBatteryStatsServiceImpl.this.handleNoteStartSensor(msg.arg1, msg.arg2);
            } else if (i == 12) {
                int pid = msg.arg1;
                int uid = msg.arg2;
                if (pid != Process.myPid()) {
                    Process.killProcess(pid);
                    VSlog.d(VivoBatteryStatsServiceImpl.TAG, "BatteryStatsService kill pid:" + pid + " ,uid:" + uid + " ,reason: do dump take too long time.");
                    return;
                }
                VSlog.d(VivoBatteryStatsServiceImpl.TAG, "oops, system_server do dump take too long time");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNoteStartSensor(int uid, int sensor) {
        this.mBatteryStatsService.enforceCallingPermission();
        synchronized (this.mBatteryStatsService.mStats) {
            this.mBatteryStatsService.mStats.noteStartSensorLocked(uid, sensor);
            FrameworkStatsLog.write_non_chained(5, uid, (String) null, sensor, 1);
        }
    }

    public void noteGpsChanged(IVivoFrozenInjector injector, WorkSource oldWs, WorkSource newWs) {
        if (injector == null) {
            return;
        }
        ArrayList<Integer> oldUids = new ArrayList<>();
        ArrayList<Integer> newUids = new ArrayList<>();
        for (int i = 0; i < oldWs.size(); i++) {
            int uid = oldWs.getUid(i);
            if (UserHandle.isApp(uid)) {
                oldUids.add(Integer.valueOf(uid));
            }
        }
        for (int i2 = 0; i2 < newWs.size(); i2++) {
            int uid2 = newWs.getUid(i2);
            if (UserHandle.isApp(uid2)) {
                newUids.add(Integer.valueOf(uid2));
            }
        }
        for (int i3 = 0; i3 < oldUids.size(); i3++) {
            int uid3 = oldUids.get(i3).intValue();
            if (!newUids.contains(Integer.valueOf(uid3))) {
                injector.setWorkingState(32, 0, uid3);
            }
        }
        for (int i4 = 0; i4 < newUids.size(); i4++) {
            int uid4 = newUids.get(i4).intValue();
            if (!oldUids.contains(Integer.valueOf(uid4))) {
                injector.setWorkingState(32, 1, uid4);
            }
        }
    }
}